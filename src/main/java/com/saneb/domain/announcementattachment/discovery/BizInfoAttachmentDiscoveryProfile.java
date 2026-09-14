package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 기업마당 공식 첨부 영역의 직접 GET 링크만 발견합니다. 파일명으로 문서 역할을 확정하지 않습니다. */
@Component
public final class BizInfoAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String HOST = "www.bizinfo.go.kr";
    private static final String DETAIL = "/sii/siia/selectSIIA200Detail.do";
    private static final String DOWNLOAD = "/cmm/fms/fileDown.do";
    private static final String CODE = "BIZINFO_DETAIL_V1";
    private static final Set<String> UNSUPPORTED_FORMATS = Set.of("ZIP", "RAR", "7Z", "ALZ", "EXE", "MSI", "BAT", "SH", "JS",
            "JPG", "JPEG", "PNG", "GIF", "TIF", "TIFF", "WEBP", "DOC", "DOCX", "XLS", "XLSX", "PPT", "PPTX", "TXT", "CSV", "HTML", "HTM", "XML");
    private static final String DEFINITION = "BIZINFO_DETAIL_V1:1|https:443|www.bizinfo.go.kr|"
            + "detail:/sii/siia/selectSIIA200Detail.do:pblancId=PBLN_[0-9]{15}|"
            + "download:/cmm/fms/fileDown.do:atchFileId=FILE_[0-9]{15},fileSn=[0-9]{1,3}|"
            + "meta[property=og:title]|.attached_file_list:exactly1>ul|li>.file_name|a[href]|"
            + "roles:unknown-requires-evidence|formats:pdf-hwp-hwpx-unknown|limit10|no-script-no-base";
    private static final String HASH = selectImplementationHash();

    @Override public String selectProviderCode() { return "BIZINFO"; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(null,null)); }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return HASH; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(HOST); }
    @Override public URI selectDetailUri(String providerNoticeId) {
        if (providerNoticeId == null || !providerNoticeId.matches("PBLN_[0-9]{15}"))
            throw new IllegalArgumentException("PROFILE_REQUIRED");
        return URI.create("https://" + HOST + DETAIL + "?pblancId=" + providerNoticeId);
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || !HOST.equalsIgnoreCase(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                || !uri.equals(uri.normalize()) || !uri.getRawPath().equals(uri.getPath())) return false;
        Map<String, String> parameters = selectParameters(uri.getRawQuery());
        if (DETAIL.equals(uri.getPath())) return parameters.size() == 1
                && parameters.containsKey("pblancId") && parameters.get("pblancId").matches("PBLN_[0-9]{15}");
        return DOWNLOAD.equals(uri.getPath()) && parameters.size() == 2
                && parameters.containsKey("atchFileId") && parameters.get("atchFileId").matches("FILE_[0-9]{15}")
                && parameters.containsKey("fileSn") && parameters.get("fileSn").matches("[0-9]{1,3}");
    }

    @Override public Result selectDescriptors(String providerNoticeId, String html) {
        URI detail = selectDetailUri(providerNoticeId);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var document = Jsoup.parse(html, detail.toASCIIString());
        var title = document.selectFirst("meta[property=og:title]");
        var containers = document.select(".attached_file_list");
        if (title == null || title.attr("content").isBlank() || containers.size() != 1
                || containers.getFirst().selectFirst("ul") == null) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var files = new LinkedHashMap<String, Descriptor>();
        boolean unresolved = containers.getFirst().select("li").isEmpty()
                && (!containers.getFirst().select("a[href]").isEmpty() || !containers.getFirst().select(".file_name").isEmpty());
        boolean exceeded = false;
        for (var row : containers.getFirst().select("li")) {
            var nameElement = row.selectFirst(".file_name");
            if (nameElement == null || nameElement.text().isBlank() || nameElement.text().length() > 500) {
                unresolved = true;
                continue;
            }
            String name = nameElement.text().strip();
            var rowLinks = new LinkedHashMap<String, URI>();
            for (var anchor : row.select("a[href]")) {
                try {
                    // <base>, onclick/JavaScript, 미리보기 renderer URL은 사용하지 않는다.
                    URI candidate = detail.resolve(anchor.attr("href"));
                    if (DOWNLOAD.equals(candidate.getPath()) && selectApprovedRequest(candidate)) {
                        var parameters = selectParameters(candidate.getRawQuery());
                        rowLinks.putIfAbsent(parameters.get("atchFileId") + ":" + parameters.get("fileSn"), candidate);
                    }
                } catch (IllegalArgumentException ignored) { /* 외부 URL 원문을 오류/로그로 전달하지 않는다. */ }
            }
            if (rowLinks.isEmpty()) { unresolved = true; continue; }
            // 예상과 달리 한 행에 서로 다른 파일이 있으면 모두 수집하되 역할/완전성은 검수가 필요하다.
            if (rowLinks.size() > 1) unresolved = true;
            for (URI link : rowLinks.values()) {
            var params = selectParameters(link.getRawQuery());
            String key = params.get("atchFileId") + ":" + params.get("fileSn");
            if (files.containsKey(key)) continue;
            if (files.size() == 10) { exceeded = true; continue; }
            var locator = new AttachmentSetEvidence.Locator(CODE, DOWNLOAD,
                    Map.of("fileId", params.get("atchFileId"), "fileSn", params.get("fileSn"), "noticeId", providerNoticeId));
            String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
            boolean knownSupported = Set.of("PDF", "HWP", "HWPX").contains(extension);
            // 명시적 비지원 형식도 발견 목록에는 남긴다. 해당 binary는 다운로드하지 않는다.
            files.put(key, new Descriptor(link, locator, name, knownSupported && rowLinks.size() == 1 ? extension : null,
                    "UNKNOWN",
                    knownSupported || !UNSUPPORTED_FORMATS.contains(extension)));
            }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }

    private Map<String, String> selectParameters(String query) {
        if (query == null || query.length() > 200) return Map.of();
        var values = new LinkedHashMap<String, String>();
        for (String part : query.split("&", -1)) {
            String[] entry = part.split("=", -1);
            if (entry.length != 2 || values.putIfAbsent(entry[0], entry[1]) != null) return Map.of();
        }
        return values;
    }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
    private static String selectImplementationHash() {
        return AttachmentProfileFingerprint.selectHash(DEFINITION, BizInfoAttachmentDiscoveryProfile.class);
    }
}
