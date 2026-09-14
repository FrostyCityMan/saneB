package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 대전 서구에서 확인한 고정 다운로드 폼만 해석한다. JS를 실행하거나 임의 form/action을 전송하지 않는다. */
@Component
public final class SeoguSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_DAEJEON_SEOGU_V1";
    private static final String HOST = "www.seogu.go.kr";
    private static final String FILE_HOST = "eminwon.seogu.go.kr";
    private static final String DETAIL_PATH = "/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do";
    private static final String FILE_PATH = "/emwp/jsp/ofr/FileDown.jsp";
    private static final String HASH = AttachmentProfileFingerprint.selectHash(CODE + ":1|LGS-000074|direct-post-no-redirect|fileForm|bbs--view--file|double-uri-name|all-roles-unknown", SeoguSaeolAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000074","SAFE_DAEJEON_DATA_KEY_NOTICE")); }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return HASH; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String providerNoticeId) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String providerNoticeId, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000074".equals(source.localSourceCode())
                || !"SAFE_DAEJEON_DATA_KEY_NOTICE".equals(source.listParserProfileCode()) || source.sourceUrl() == null
                || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))
            throw new IllegalArgumentException("PROFILE_REQUIRED");
        URI uri = URI.create(source.sourceUrl());
        if (!selectApprovedRequest(uri)) throw new IllegalArgumentException("PROFILE_REQUIRED");
        return uri;
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        return selectSafeUri(uri) && HOST.equalsIgnoreCase(uri.getHost()) && DETAIL_PATH.equals(uri.getPath())
                && uri.getRawQuery() != null && uri.getRawQuery().matches("notAncmtMgtNo=[0-9]{1,15}");
    }
    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request request) {
        if (request == null) return false;
        if ("GET".equals(request.method())) return selectApprovedRequest(request.uri());
        URI uri = request.uri();
        if (!"POST".equals(request.method()) || !selectSafeUri(uri) || !FILE_HOST.equalsIgnoreCase(uri.getHost())
                || !FILE_PATH.equals(uri.getPath()) || uri.getRawQuery() != null
                || !request.form().keySet().equals(Set.of("user_file_nm", "sys_file_nm", "file_path"))) return false;
        try {
            String name = selectDecodedName(request.form().get("user_file_nm"));
            return selectSafeName(name, 500) && selectSafeName(request.form().get("sys_file_nm"), 200)
                    && request.form().get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        } catch (IllegalArgumentException exception) { return false; }
    }

    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var document = Jsoup.parse(html, detail.toASCIIString());
        var forms = document.select("form#fileForm[method=post]");
        var containers = document.select(".bbs--view--file");
        if (forms.size() != 1 || containers.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var container = containers.getFirst();
        var buttons = container.select(".btn-on-ico[onclick]");
        boolean unresolved = buttons.isEmpty() && !container.select("a,button,[onclick]").isEmpty();
        if (buttons.isEmpty() && !container.select(".ir-file,li").isEmpty()) unresolved = true;
        for (var action : container.select("a,button,[onclick]")) {
            if (action.hasClass("btn-on-ico") && action.hasAttr("onclick")) continue;
            if (action.hasClass("ir-preview") && action.attr("onclick").startsWith("fn_egov_previewFile(")) continue;
            unresolved = true;
        }
        boolean limited = false;
        var descriptors = new LinkedHashMap<String, Descriptor>();
        for (var button : buttons) {
            var arguments = AttachmentDownloadInvocation.selectArguments(button.attr("onclick"), "fn_egov_downFile", true, true);
            if (arguments.size() != 3) { unresolved = true; continue; }
            String name = arguments.get(0);
            String systemName = arguments.get(1);
            String path = arguments.get(2);
            Map<String, String> form = Map.of("user_file_nm", selectEncodedName(name), "sys_file_nm", systemName, "file_path", path);
            AttachmentPinnedDownloadClient.Request request;
            try { request = new AttachmentPinnedDownloadClient.Request(URI.create("https://" + FILE_HOST + FILE_PATH), "POST", form); }
            catch (IllegalArgumentException exception) { unresolved = true; continue; }
            if (!selectApprovedRequest(request)) { unresolved = true; continue; }
            String identity = normalizer.hash(path + "\n" + systemName);
            if (descriptors.containsKey(identity)) continue;
            if (descriptors.size() == 10) { limited = true; continue; }
            String format = selectFormat(name);
            String systemFormat = selectFormat(systemName);
            boolean supported = format != null && format.equals(systemFormat);
            var locator = new AttachmentSetEvidence.Locator(CODE, FILE_PATH,
                    Map.of("attachmentId", identity, "noticeId", detail.getRawQuery().substring("notAncmtMgtNo=".length())));
            descriptors.put(identity, new Descriptor(request.uri(), locator, name, supported ? format : null,
                    "UNKNOWN", supported, form));
        }
        if (limited) return new Result("LIMIT_EXCEEDED", false, List.copyOf(descriptors.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(descriptors.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(descriptors.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(descriptors.values()), List.of());
    }

    private boolean selectSafeUri(URI uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null && uri.getFragment() == null
                && uri.equals(uri.normalize()) && uri.getRawPath().equals(uri.getPath());
    }
    private boolean selectSafeName(String name, int maximum) {
        if (name == null || name.isBlank() || name.length() > maximum || name.codePoints().anyMatch(Character::isISOControl)
                || name.contains("/") || name.contains("\\") || name.contains("..")) return false;
        // 퍼센트 인코딩된 경로 구분자도 filename으로 허용하지 않는다.
        String decoded = name;
        for (int count = 0; count < 2 && decoded.contains("%"); count++) {
            try { decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8); }
            catch (IllegalArgumentException exception) { return false; }
            if (decoded.contains("/") || decoded.contains("\\") || decoded.contains("..")
                    || decoded.codePoints().anyMatch(Character::isISOControl)) return false;
        }
        return true;
    }
    private String selectEncodedName(String name) { return selectUriEncoding(selectUriEncoding(name)); }
    private String selectUriEncoding(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("%21", "!")
                .replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%7E", "~");
    }
    private String selectDecodedName(String encoded) {
        return URLDecoder.decode(URLDecoder.decode(encoded, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
