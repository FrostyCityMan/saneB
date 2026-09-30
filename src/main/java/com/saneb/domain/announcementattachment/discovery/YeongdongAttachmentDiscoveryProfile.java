package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungjuEminwonNoticePage;
import com.saneb.domain.announcementsource.provider.content.YeongdongNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 영동의 공식 고시공고 mode=D 다운로드만 지원하며 미리보기 스크립트는 실행하지 않는다. */
@Component
public final class YeongdongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_YEONGDONG_BOARD_V1";
    private static final String HASH = AttachmentProfileFingerprint.selectHash(
            "YEONGDONG:1|LGS-000141|SAEOL_GOSI|https443|same-notice-file|unknown-role|limit10|"
                    + AttachmentProfileFingerprint.selectHash("PAGE:1", YeongdongNoticePage.class)
                    + AttachmentProfileFingerprint.selectHash("QUERY:1", ChungjuEminwonNoticePage.class),
            YeongdongAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return HASH; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(YeongdongNoticePage.HOST); }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000141", "SAEOL_GOSI")); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    private Map<String, String> selectParameters(URI uri) { return ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery()); }
    private boolean selectSafeUri(URI uri) {
        return uri != null && uri.toASCIIString().length() <= 4096 && "https".equals(uri.getScheme())
                && YeongdongNoticePage.HOST.equals(uri.getHost()) && (uri.getPort() == -1 || uri.getPort() == 443)
                && uri.getUserInfo() == null && uri.getFragment() == null && uri.equals(uri.normalize())
                && Objects.equals(uri.getRawPath(), uri.getPath());
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (!selectSafeUri(uri) || !YeongdongNoticePage.PATH.equals(uri.getPath())) return false;
            var query = selectParameters(uri);
            if (!query.getOrDefault("no", "").matches("[a-f0-9]{32}")) return false;
            if ("V".equals(query.get("mode"))) return query.keySet().equals(Set.of("mode", "no", "GotoPage")) && "1".equals(query.get("GotoPage"));
            return query.keySet().equals(Set.of("mode", "no", "file_id")) && "D".equals(query.get("mode"))
                    && query.get("file_id").matches("[1-9][0-9]{0,14}");
        } catch (IllegalArgumentException failure) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri());
    }
    @Override public boolean selectApprovedRequest(Request first, Request next) { return first != null && first.equals(next) && selectApprovedRequest(next); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000141".equals(source.localSourceCode())
                    || !"SAEOL_GOSI".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            var uri = URI.create(source.sourceUrl());
            if (!selectApprovedRequest(uri) || !"V".equals(selectParameters(uri).get("mode"))) throw new IllegalArgumentException();
            return uri;
        } catch (IllegalArgumentException failure) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    private boolean selectPreview(URI uri, String format) {
        try {
            if (!selectSafeUri(uri) || !"/_prog/bbs/pre_viewer.php".equals(uri.getPath())) return false;
            var query = selectParameters(uri);
            return query.keySet().equals(Set.of("site_dvs_cd", "bbs_cd", "nm")) && "kr".equals(query.get("site_dvs_cd"))
                    && "kor_sub020103".equals(query.get("bbs_cd")) && query.get("nm").matches("[A-Za-z0-9_]{1,160}\\.[A-Za-z0-9]{1,8}")
                    && Objects.equals(format, selectFormat(query.get("nm")));
        } catch (IllegalArgumentException failure) { return false; }
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        org.jsoup.nodes.Element area;
        try { area = YeongdongNoticePage.selectFiles(Jsoup.parse(html, detail.toASCIIString())).clone(); }
        catch (IllegalArgumentException failure) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String notice = selectParameters(detail).get("no");
        var files = new LinkedHashMap<String, Descriptor>(); boolean unresolved = false, limited = false;
        for (var row : area.select(":root > li")) {
            var anchors = row.select(":root > a");
            if (anchors.isEmpty()) { unresolved = true; continue; }
            var anchor = anchors.getFirst();
            try {
                if (anchor.hasAttr("onclick")) throw new IllegalArgumentException();
                String href = anchor.attr("href"), name = anchor.text().strip();
                // URI.resolve("?query")는 마지막 경로를 버릴 수 있으므로 현재 문서 경로를 명시한다.
                URI file = href.startsWith("?") ? URI.create("https://" + YeongdongNoticePage.HOST + YeongdongNoticePage.PATH + href) : detail.resolve(href);
                if (!selectApprovedRequest(file) || !"D".equals(selectParameters(file).get("mode")) || !notice.equals(selectParameters(file).get("no"))
                        || name.isBlank() || name.length() > 500 || name.contains("/") || name.contains("\\") || name.codePoints().anyMatch(Character::isISOControl)
                        || !name.equals(anchor.attr("title"))) throw new IllegalArgumentException();
                String id = selectParameters(file).get("file_id"), format = selectFormat(name);
                var descriptor = new Descriptor(file, new AttachmentSetEvidence.Locator(CODE, YeongdongNoticePage.PATH,
                        Map.of("noticeId", notice, "attachmentId", id)), name, format, "UNKNOWN", format != null);
                if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) { limited = true; continue; }
                else files.put(id, descriptor);
                for (var preview : anchors.subList(1, anchors.size())) {
                    if (preview.hasAttr("onclick") || !"파일 미리보기".equals(preview.text().strip()) || !selectPreview(detail.resolve(preview.attr("href")), format)) unresolved = true;
                    else preview.remove();
                }
                var nested = anchor.clone(); nested.select("span:empty").remove();
                if (!nested.children().isEmpty()) unresolved = true;
                anchor.remove();
                if (!row.text().isBlank() || !row.children().isEmpty() || row.hasAttr("onclick")) unresolved = true;
                row.remove();
            } catch (IllegalArgumentException failure) { unresolved = true; }
        }
        if (!area.text().isBlank() || !area.children().isEmpty() || area.hasAttr("onclick")) unresolved = true;
        if (limited) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectFormat(String name) { String ext = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF", "HWP", "HWPX").contains(ext) ? ext : null; }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
