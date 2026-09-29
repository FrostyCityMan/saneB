package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SejongNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 실측된 일반공고 첨부 칸의 직접 GET만 지원한다. 미리보기·스크립트는 실행하지 않는다. */
@Component
public final class SejongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_SEJONG_BOARD_V1", FILE_HOST = "eminwon.sejong.go.kr", DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> FIELDS = Set.of("user_file_nm", "sys_file_nm", "file_path");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash("SEJONG:1|LGS-000083|SAEOL_GOSI|https443|fixed-get|same-request|unknown-role|limit10|"
            + AttachmentProfileFingerprint.selectHash("PAGE:1", SejongNoticePage.class) + "|"
            + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000083", "SAEOL_GOSI")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(SejongNoticePage.HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000083".equals(source.localSourceCode())
                    || !"SAEOL_GOSI".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return SejongNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException e) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    private boolean selectSafeUri(URI uri) {
        return uri != null && uri.toASCIIString().length() <= 8192 && "https".equals(uri.getScheme()) && (uri.getPort() == -1 || uri.getPort() == 443)
                && uri.getUserInfo() == null && uri.getFragment() == null && uri.getRawPath() != null && uri.getRawPath().equals(uri.getPath()) && uri.equals(uri.normalize());
    }
    private boolean selectSafeName(String value) {
        return value != null && !value.isBlank() && value.length() <= 500 && !value.contains("/") && !value.contains("\\")
                && !value.contains("..") && !value.contains("%") && value.indexOf('\ufffd') < 0 && value.codePoints().noneMatch(Character::isISOControl);
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (!selectSafeUri(uri)) return false;
            if (SejongNoticePage.HOST.equals(uri.getHost())) return uri.equals(SejongNoticePage.selectDetailUri(uri));
            if (!FILE_HOST.equals(uri.getHost()) || !DOWNLOAD.equals(uri.getPath())) return false;
            var values = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return values.keySet().equals(FIELDS) && selectSafeName(values.get("user_file_nm")) && selectSafeName(values.get("sys_file_nm"))
                    && values.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        } catch (IllegalArgumentException e) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri());
    }
    @Override public boolean selectApprovedRequest(Request first, Request next) { return first != null && first.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_048_576) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        org.jsoup.nodes.Element area;
        try { area = SejongNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString())).clone(); }
        catch (IllegalArgumentException e) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        var files = new LinkedHashMap<String, Descriptor>();
        boolean unresolved = false, exceeded = false;
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no");
        for (var item : area.select(":root > div.download")) try {
            var anchors = item.select(":root > a[href]");
            if (anchors.size() != 1 || anchors.getFirst().hasAttr("onclick")) throw new IllegalArgumentException();
            var anchor = anchors.getFirst();
            URI raw = URI.create(anchor.attr("href").replace(" ", "%20"));
            if (!FILE_HOST.equals(raw.getHost()) || !selectApprovedRequest(raw)) throw new IllegalArgumentException();
            var values = CapitalThirdNoticePage.selectParameters(raw.getRawQuery());
            String name = values.get("user_file_nm"), format = selectFormat(name);
            if (!name.equals(anchor.text().strip())) throw new IllegalArgumentException();
            String identity = normalizer.hash(values.get("file_path") + "\n" + values.get("sys_file_nm"));
            URI fetch = URI.create("https://" + FILE_HOST + DOWNLOAD + "?user_file_nm=" + selectEncoded(name)
                    + "&sys_file_nm=" + selectEncoded(values.get("sys_file_nm")) + "&file_path=" + selectEncoded(values.get("file_path")));
            boolean supported = format != null && format.equals(selectFormat(values.get("sys_file_nm")));
            var descriptor = new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, DOWNLOAD, Map.of("noticeId", notice, "attachmentId", identity)),
                    name, supported ? format : null, "UNKNOWN", supported);
            var old = files.get(identity);
            if (old != null) { if (!old.equals(descriptor)) unresolved = true; }
            else if (files.size() == 10) { exceeded = true; continue; }
            else files.put(identity, descriptor);
            var residual = item.clone(); residual.select("a").remove();
            for (var icon : residual.select(":root > img"))
                if (icon.attr("src").matches("/images/mimetype/[A-Za-z0-9]+\\.gif") && icon.attributes().asList().stream().allMatch(a -> Set.of("src", "alt").contains(a.getKey()))) icon.remove();
            if (!residual.text().isBlank() || !residual.children().isEmpty() || !anchor.children().isEmpty()) unresolved = true;
            item.remove();
        } catch (IllegalArgumentException e) { unresolved = true; }
        if (!area.text().isBlank() || !area.select("a,img,button,input,select,form,iframe,object,embed,script,[onclick],[href]").isEmpty()) unresolved = true;
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectEncoded(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
