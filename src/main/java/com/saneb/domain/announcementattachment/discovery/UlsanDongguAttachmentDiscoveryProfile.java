package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.UlsanDongguNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 공식 상세 조회 POST와 파일 GET을 분리하며 파일별 실패가 다른 정상 파일을 폐기하지 않는다. */
@Component
public final class UlsanDongguAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String CODE = "LOCAL_ULSAN_DONGGU_GET_V1";
    private static final String SOURCE = "LGS-000080";
    private static final String LIST = "DOBONG_NOTICE_TABLE";
    private static final String DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    // 기존 파일 경로·파일명 검증만 재사용한다. 이 사이트의 상세 query는 별도로 검증한다.
    private final SaeolGetAttachmentDiscoveryProfile fileValidator = new SaeolGetAttachmentDiscoveryProfile(
            CODE, SOURCE, UlsanDongguNoticePage.HOST, LIST, "div", false);
    private final String hash = AttachmentProfileFingerprint.selectHash(
            CODE + ":2|exact-seven-field-detail-post|official-download-div|three-literal-onclick|blank-onkeypress"
                    + "|same-request-only|limit10|unknown-role|" + fileValidator.selectProfileHash()
                    + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1", UlsanDongguNoticePage.class)
                    + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE, LIST)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(UlsanDongguNoticePage.HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode())
                    || !SOURCE.equals(source.localSourceCode()) || !LIST.equals(source.listParserProfileCode())
                    || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))
                throw new IllegalArgumentException();
            return UlsanDongguNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || uri.toASCIIString().length() > 8192) return false;
            if (UlsanDongguNoticePage.selectMatches(uri)) return uri.equals(UlsanDongguNoticePage.selectDetailUri(uri));
            return DOWNLOAD.equals(uri.getPath()) && fileValidator.selectApprovedRequest(uri);
        } catch (IllegalArgumentException exception) { return false; }
    }

    @Override public boolean selectApprovedRequest(Request request) {
        if (request == null) return false;
        if ("GET".equals(request.method())) return selectApprovedRequest(request.uri());
        try {
            if (!"POST".equals(request.method()) || !request.uri().equals(URI.create("https://" + UlsanDongguNoticePage.HOST + UlsanDongguNoticePage.DETAIL))) return false;
            String query = request.form().entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + selectEncoded(entry.getValue())).collect(java.util.stream.Collectors.joining("&"));
            return request.equals(UlsanDongguNoticePage.selectRequest(URI.create(request.uri() + "?" + query)));
        } catch (IllegalArgumentException exception) { return false; }
    }

    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        if (!selectApprovedRequest(initial) || !selectApprovedRequest(next)) return false;
        if ("GET".equals(initial.method()) && UlsanDongguNoticePage.selectMatches(initial.uri()))
            return next.equals(UlsanDongguNoticePage.selectRequest(initial.uri()));
        return initial.equals(next);
    }

    @Override public com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download selectDownload(
            Request initial, java.nio.file.Path output, long maximumBytes, Operation operation) throws java.io.IOException {
        if (!selectApprovedRequest(initial)) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        Request selected = "GET".equals(initial.method()) && UlsanDongguNoticePage.selectMatches(initial.uri())
                ? UlsanDongguNoticePage.selectRequest(initial.uri()) : initial;
        return operation.selectDownload(selected, maximumBytes);
    }

    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element area;
        try { area = UlsanDongguNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString())).clone(); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        var files = new LinkedHashMap<String, Descriptor>();
        boolean unresolved = false, exceeded = false;
        for (var anchor : area.select(":root > a")) {
            try {
                if (!"#download".equals(anchor.attr("href")) || !anchor.children().isEmpty()
                        || selectUnexpectedHandler(anchor)) continue;
                var arguments = AttachmentDownloadInvocation.selectArguments(anchor.attr("onclick"), "goDownLoad", true, true);
                if (arguments.size() != 3 || !anchor.text().strip().equals(arguments.getFirst())) continue;
                URI uri = URI.create("https://" + UlsanDongguNoticePage.HOST + DOWNLOAD
                        + "?user_file_nm=" + selectEncoded(arguments.get(0))
                        + "&sys_file_nm=" + selectEncoded(arguments.get(1))
                        + "&file_path=" + selectEncoded(arguments.get(2)));
                if (!selectApprovedRequest(uri)) continue;
                String identity = normalizer.hash(arguments.get(2) + "\n" + arguments.get(1));
                String format = selectFormat(arguments.get(0));
                boolean supported = format != null && format.equals(selectFormat(arguments.get(1)));
                var locator = new AttachmentSetEvidence.Locator(CODE, DOWNLOAD, Map.of(
                        "noticeId", CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no"),
                        "attachmentId", identity));
                var descriptor = new Descriptor(uri, locator, arguments.getFirst(), supported ? format : null, "UNKNOWN", supported);
                if (files.containsKey(identity)) {
                    if (!files.get(identity).equals(descriptor)) unresolved = true;
                } else if (files.size() == 10) { exceeded = true; continue; }
                else files.put(identity, descriptor);
                anchor.remove();
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        // 해석하지 못한 링크·활성 요소는 오류로 남기되 이미 확인한 descriptor는 유지한다.
        if (!area.text().isBlank() || !area.select("a,button,input,select,form,iframe,object,embed,script,style,svg,img,[href]").isEmpty()
                || area.getAllElements().stream().anyMatch(element -> element.attributes().asList().stream()
                    .anyMatch(attribute -> attribute.getKey().toLowerCase(Locale.ROOT).startsWith("on")))) unresolved = true;
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }

    private boolean selectUnexpectedHandler(Element anchor) {
        return anchor.attributes().asList().stream().anyMatch(attribute -> {
            String name = attribute.getKey().toLowerCase(Locale.ROOT);
            return name.startsWith("on") && !"onclick".equals(name)
                    && !("onkeypress".equals(name) && attribute.getValue().isBlank());
        });
    }

    private String selectEncoded(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String selectFormat(String name) {
        String suffix = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
