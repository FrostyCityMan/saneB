package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.GunwiNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 군위의 숫자 파일 링크와 실측한 공식 전자민원 서버 이동만 허용한다. */
@Component
public final class GunwiAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_GUNWI_BOARD_V1", FILE_HOST = "eminwon.gunwi.go.kr";
    private static final String DOWNLOAD = "/programs/board/saeol/notice/download.do";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile validator = new SaeolGetAttachmentDiscoveryProfile(
            CODE, "LGS-000053", FILE_HOST, "GUNWI_NOTICE_TABLE", "td", false);
    private final String hash = AttachmentProfileFingerprint.selectHash("GUNWI:1|numeric-file-same-notice|official-file-redirect|partial-preserved|unknown-role|limit10|"
            + validator.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1", GunwiNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000053", "GUNWI_NOTICE_TABLE")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(GunwiNoticePage.HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000053".equals(source.localSourceCode())
                    || !"GUNWI_NOTICE_TABLE".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return GunwiNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || uri.getRawPath() == null || uri.getHost() == null || !"https".equals(uri.getScheme()) || !selectApprovedHosts().contains(uri.getHost())
                    || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                    || !uri.equals(uri.normalize()) || !uri.getRawPath().equals(uri.getPath())) return false;
            if (GunwiNoticePage.selectMatches(uri)) return uri.equals(GunwiNoticePage.selectDetailUri(uri));
            if (FILE_HOST.equals(uri.getHost())) return SaeolGetAttachmentDiscoveryProfile.DOWNLOAD.equals(uri.getPath()) && validator.selectApprovedRequest(uri);
            var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return DOWNLOAD.equals(uri.getPath()) && q.keySet().equals(Set.of("file_seq", "not_ancmt_mgt_no"))
                    && q.get("file_seq").matches("[1-9][0-9]{0,14}") && q.get("not_ancmt_mgt_no").matches("[1-9][0-9]{0,14}");
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri());
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        if (!selectApprovedRequest(initial) || !selectApprovedRequest(next)) return false;
        if (initial.equals(next)) return true;
        return GunwiNoticePage.HOST.equals(initial.uri().getHost()) && DOWNLOAD.equals(initial.uri().getPath())
                && FILE_HOST.equals(next.uri().getHost()) && SaeolGetAttachmentDiscoveryProfile.DOWNLOAD.equals(next.uri().getPath());
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_048_576) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element area;
        try { area = GunwiNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no");
        var files = new LinkedHashMap<String, Descriptor>(); boolean unresolved = false, exceeded = false;
        for (var a : area.select("a")) try {
            if (a.hasAttr("onclick") || !a.select("script,button,input,iframe,img,object,embed,[onclick]").isEmpty()
                    || !"파일 다운로드".equals(a.attr("title")) || a.attr("href").length() > 4096) throw new IllegalArgumentException();
            URI fetch = detail.resolve(URI.create(a.attr("href")));
            if (!GunwiNoticePage.HOST.equals(fetch.getHost()) || !DOWNLOAD.equals(fetch.getPath()) || !selectApprovedRequest(fetch)) throw new IllegalArgumentException();
            var q = CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());
            if (!notice.equals(q.get("not_ancmt_mgt_no"))) throw new IllegalArgumentException();
            String name = a.text().strip(), id = q.get("file_seq");
            if (name.isBlank() || name.length() > 500 || name.contains("/") || name.contains("\\") || name.contains("..") || name.contains("%")
                    || name.indexOf('\ufffd') >= 0 || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
            String format = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
            boolean supported = Set.of("PDF", "HWP", "HWPX").contains(format);
            var d = new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, DOWNLOAD, Map.of("noticeId", notice, "attachmentId", id)), name, supported ? format : null, "UNKNOWN", supported);
            var old = files.get(id);
            if (old != null) { if (!old.equals(d)) unresolved = true; }
            else if (files.size() == 10) exceeded = true;
            else files.put(id, d);
        } catch (IllegalArgumentException exception) { unresolved = true; }
        var residual = area.clone(); residual.select("a").remove();
        if (!residual.text().isBlank() || !residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()
                || files.isEmpty() && !residual.select("li").isEmpty()) unresolved = true;
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
