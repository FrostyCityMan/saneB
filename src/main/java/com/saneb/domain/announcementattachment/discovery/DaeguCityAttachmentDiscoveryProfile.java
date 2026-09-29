package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.DaeguCityNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 대구시 첨부 그룹·순번의 고정 GET만 허용한다. 스크립트·미리보기는 실행하지 않는다. */
@Component
public final class DaeguCityAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_DAEGU_CITY_GOSI_V1", DOWNLOAD = "/icms/cmm/fms/FileDown.do";
    private static final Pattern CALL = Pattern.compile("^javascript:fn_egov_downFile\\('([A-Za-z0-9]{1,80} ?)',\\s*'([0-9]{1,3})'\\);?$");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash("DAEGU_CITY:1|fixed-get|group-count|partial-preserved|no-preview|limit10|unknown-role|"
            + AttachmentProfileFingerprint.selectHash("PAGE:1", DaeguCityNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    /** 실측한 구형 binary MIME와 UTF-8 파일명만 기존 검증기에 명시한다. */
    @Override public Set<String> selectLegacyBinaryContentTypes() { return Set.of("application/x-msdownload"); }
    @Override public boolean selectUtf8DispositionOctets() { return true; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000044", "SAFE_DAEGU_LEGAL_NOTICE")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(DaeguCityNoticePage.HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000044".equals(source.localSourceCode())
                    || !"SAFE_DAEGU_LEGAL_NOTICE".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return DaeguCityNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (DaeguCityNoticePage.selectMatches(uri)) return uri.equals(DaeguCityNoticePage.selectDetailUri(uri));
            if (uri == null || !"https".equals(uri.getScheme()) || !DaeguCityNoticePage.HOST.equals(uri.getHost())
                    || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                    || !DOWNLOAD.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) return false;
            var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return Set.of("atchFileId", "fileSn").equals(query.keySet())
                    && query.get("atchFileId").matches("[A-Za-z0-9]{1,80} ?") && query.get("fileSn").matches("[0-9]{1,3}");
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri());
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        return initial != null && initial.equals(next) && selectApprovedRequest(next);
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_048_576) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element area;
        try { area = DaeguCityNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString()), detail); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String items = ":root > span.attfile";
        var groups = area.select(":root > input[type=hidden][name=atchFileId]");
        var counts = area.select(":root > input[type=hidden][name=fileListCnt]");
        boolean metadataValid = groups.size() == 1 && groups.getFirst().attr("value").matches("[A-Za-z0-9]{1,80}")
                && counts.size() == 1 && counts.getFirst().attr("value").matches("[0-9]{1,3}");
        String group = metadataValid ? groups.getFirst().attr("value") : "";
        var residual = area.clone(); residual.select(items).remove();
        // 보조 함수 정의는 실행하지 않는다. 명시된 파일 수와 실제 항목 수는 따로 대조한다.
        residual.select(":root > script:not([src]), :root > input[type=hidden][name=atchFileId], :root > input[type=hidden][name=fileSn], :root > input[type=hidden][name=fileListCnt]").remove();
        boolean unresolved = !metadataValid || Integer.parseInt(metadataValid ? counts.getFirst().attr("value") : "0") != area.select(items).size()
                || !residual.text().isBlank() || !residual.children().isEmpty();
        boolean exceeded = false; var files = new LinkedHashMap<String, Descriptor>();
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("sno");
        for (var item : area.select(items)) try {
            var links = item.select(":root > a.download[href]");
            if (links.size() != 1 || links.getFirst().hasAttr("onclick")) throw new IllegalArgumentException();
            var link = links.getFirst(); String raw = link.attr("href");
            if (raw.length() > 200) throw new IllegalArgumentException();
            var match = CALL.matcher(raw); if (!match.matches()) throw new IllegalArgumentException();
            String id = match.group(1), sequence = match.group(2);
            if (metadataValid && !group.equals(id.strip())) throw new IllegalArgumentException();
            URI fetch = URI.create("https://" + DaeguCityNoticePage.HOST + DOWNLOAD + "?atchFileId="
                    + URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20") + "&fileSn=" + sequence);
            if (!selectApprovedRequest(fetch)) throw new IllegalArgumentException();
            String name = link.text().replace('\u00a0', ' ').replaceFirst("\\s*\\[[0-9]+\\s+byte\\]$", "").strip();
            if (name.isEmpty() || name.length() > 500 || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
            String fileId = normalizer.hash(id.strip() + "\n" + sequence), format = selectFormat(name);
            var descriptor = new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, DOWNLOAD,
                    Map.of("noticeId", notice, "attachmentId", fileId)), name, format, "UNKNOWN", format != null);
            var old = files.get(fileId);
            if (old != null) { if (!old.equals(descriptor)) unresolved = true; }
            else if (files.size() == 10) exceeded = true;
            else files.put(fileId, descriptor);
            var remaining = item.clone(); remaining.select(":root > a.download").remove();
            for (var preview : new ArrayList<>(remaining.select(":root > a.view"))) {
                String expected = "javascript:filePreview('" + id + "','" + sequence + "')";
                if (expected.equals(preview.attr("href")) && "미리보기".equals(preview.text().strip())
                        && !preview.hasAttr("onclick") && preview.children().isEmpty()) preview.remove();
            }
            if (!remaining.text().isBlank() || !remaining.children().isEmpty()
                    || !link.select("script,input,button,img,iframe,object,embed,[onclick]").isEmpty()) unresolved = true;
        } catch (IllegalArgumentException exception) { unresolved = true; }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
