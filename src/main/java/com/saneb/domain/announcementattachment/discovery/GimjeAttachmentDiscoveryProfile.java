package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungjuEminwonNoticePage;
import com.saneb.domain.announcementsource.provider.content.GimjeNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 김제의 같은 공고 다운로드만 허용한다. 이름 표시용 JavaScript와 미리보기는 실행하지 않는다. */
@Component
public final class GimjeAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_GIMJE_BOARD_V1";
    private static final String HASH = AttachmentProfileFingerprint.selectHash(
            "GIMJE:1|LGS-000169|SUBJECT_NOTICE_TABLE|https443|same-notice|unknown-role|limit10|"
                    + AttachmentProfileFingerprint.selectHash("PAGE:1", GimjeNoticePage.class)
                    + AttachmentProfileFingerprint.selectHash("QUERY:1", ChungjuEminwonNoticePage.class), GimjeAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return HASH; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(GimjeNoticePage.HOST); }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000169", "SUBJECT_NOTICE_TABLE")); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    private Map<String, String> selectParameters(URI uri) { return ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery()); }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || uri.toASCIIString().length() > 4096 || !"https".equals(uri.getScheme())
                    || !GimjeNoticePage.HOST.equals(uri.getHost()) || uri.getPort() != -1 && uri.getPort() != 443
                    || uri.getUserInfo() != null || uri.getFragment() != null || !uri.equals(uri.normalize())
                    || !Objects.equals(uri.getRawPath(), uri.getPath())) return false;
            boolean detail = GimjeNoticePage.DETAIL.equals(uri.getPath());
            if (!detail && !GimjeNoticePage.DOWNLOAD.equals(uri.getPath())) return false;
            var q = selectParameters(uri);
            var allowed = new HashSet<>(Set.of("boardId", "menuCd", "dataSid", "paging", "startPage", "searchType", "searchOperation", "keyword"));
            if (!detail) allowed.addAll(Set.of("command", "fileSid"));
            return allowed.containsAll(q.keySet()) && "BBS_0000044".equals(q.get("boardId"))
                    && "DOM_000000104003000000".equals(q.get("menuCd")) && selectId(q.get("dataSid"))
                    && (!q.containsKey("paging") || "ok".equals(q.get("paging")))
                    && (!q.containsKey("startPage") || q.get("startPage").matches("[1-9][0-9]{0,5}"))
                    && (!q.containsKey("searchType") || Set.of("DATA_TITLE", "DATA_CONTENT", "USER_NICK").contains(q.get("searchType")))
                    && (!q.containsKey("searchOperation") || "AND".equals(q.get("searchOperation")))
                    && (detail || selectId(q.get("fileSid")) && "update".equals(q.get("command")));
        } catch (IllegalArgumentException failure) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri());
    }
    @Override public boolean selectApprovedRequest(Request first, Request next) { return first != null && first.equals(next) && selectApprovedRequest(next); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000169".equals(source.localSourceCode())
                    || !"SUBJECT_NOTICE_TABLE".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            URI uri = URI.create(source.sourceUrl());
            if (!GimjeNoticePage.DETAIL.equals(uri.getPath()) || !selectApprovedRequest(uri)) throw new IllegalArgumentException();
            return uri;
        } catch (IllegalArgumentException failure) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        org.jsoup.nodes.Element area;
        try { area = GimjeNoticePage.selectFiles(Jsoup.parse(html, detail.toASCIIString())).clone(); }
        catch (IllegalArgumentException failure) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String notice = selectParameters(detail).get("dataSid");
        var files = new LinkedHashMap<String, Descriptor>(); boolean unresolved = false, limited = false;
        area.select(":root > dt").remove();
        for (var row : area.select(":root > dd")) {
            for (var anchor : row.select("a.sbtn_down")) {
                try {
                    URI uri = detail.resolve(anchor.attr("href"));
                    if (anchor.hasAttr("onclick") || !GimjeNoticePage.DOWNLOAD.equals(uri.getPath()) || !selectApprovedRequest(uri)
                            || !notice.equals(selectParameters(uri).get("dataSid"))) throw new IllegalArgumentException();
                    String label = anchor.attr("title").strip();
                    if (!label.endsWith(" 다운로드")) throw new IllegalArgumentException();
                    String name = label.substring(0, label.length() - " 다운로드".length()).strip();
                    if (name.isBlank() || name.length() > 500 || name.contains("/") || name.contains("\\") || name.contains("..")
                            || name.indexOf('\ufffd') >= 0 || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
                    String id = selectParameters(uri).get("fileSid"), format = selectFormat(name);
                    var descriptor = new Descriptor(uri, new AttachmentSetEvidence.Locator(CODE, GimjeNoticePage.DOWNLOAD,
                            Map.of("noticeId", notice, "attachmentId", id)), name, format, "UNKNOWN", format != null);
                    if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                    else if (files.size() == 10) { limited = true; continue; }
                    else files.put(id, descriptor);
                    anchor.remove();
                    for (var auxiliary : row.select("a")) {
                        if (auxiliary.hasAttr("onclick")) continue;
                        if ("javascript:void('0')".equals(auxiliary.attr("href")) && name.equals(auxiliary.attr("title"))) auxiliary.remove();
                        else if (auxiliary.hasClass("sbtn_file2")) {
                            URI preview = detail.resolve(auxiliary.attr("href"));
                            if ("/board/SynapViewer.gimje".equals(preview.getPath())
                                    && uri.equals(URI.create(preview.toString().replace("/board/SynapViewer.gimje", GimjeNoticePage.DOWNLOAD)))) auxiliary.remove();
                        }
                    }
                } catch (IllegalArgumentException failure) { unresolved = true; }
            }
            if (!row.text().isBlank() || !row.select("a,button,input,select,form,iframe,object,embed,script,img,[onclick],[href]").isEmpty()) unresolved = true;
            row.remove();
        }
        if (!area.text().isBlank() || !area.children().isEmpty() || area.hasAttr("onclick")) unresolved = true;
        if (limited) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private boolean selectId(String value) { return value != null && value.matches("[1-9][0-9]{0,14}"); }
    private String selectFormat(String name) { String ext = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF", "HWP", "HWPX").contains(ext) ? ext : null; }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
