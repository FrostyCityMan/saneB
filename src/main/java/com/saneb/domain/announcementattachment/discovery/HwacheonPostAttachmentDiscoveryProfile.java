package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 화천 공식 공개 다운로드 폼의 불투명 인자를 해독·영구 저장하지 않고 같은 출처로만 전달한다. */
@Component
public final class HwacheonPostAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_HWACHEON_POST_V1";
    private static final String HOST = "eminwon.ihc.go.kr";
    private static final String DETAIL = "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    private static final String DOWNLOAD = "/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> FORM_FIELDS = Set.of("user_file_nm", "sys_file_nm", "file_path", "isHome");
    private static final Pattern CIPHER_SUFFIX = Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$");
    private static final Map<String, String> DETAIL_PARAMETERS = Map.of("context", "NTIS", "homepage_pbs_yn", "Y",
            "jndinm", "OfrNotAncmtEJB", "method", "selectOfrNotAncmt", "methodnm", "selectOfrNotAncmtRegst", "subCheck", "N");
    private static final String HASH = AttachmentProfileFingerprint.selectHash(CODE + ":1|LGS-000130|SAFE_SAEOL_EMINWON_LEGACY|"
            + HOST + "|https443|" + DETAIL + "|" + DOWNLOAD
            + "|nnn-hidden4-isHomeY|form1-td-label|opaque-direct-post-no-redirect|hash-sys-path|roles-unknown|limit10", HwacheonPostAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000130","SAFE_SAEOL_EMINWON_LEGACY")); }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return HASH; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(HOST); }
    @Override public URI selectDetailUri(String noticeId) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String noticeId, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000130".equals(source.localSourceCode())
                    || !"SAFE_SAEOL_EMINWON_LEGACY".equals(source.listParserProfileCode()) || source.sourceUrl() == null
                    || source.sourceUrl().length() > 4096 || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))
                throw new IllegalArgumentException();
            URI detail = URI.create(source.sourceUrl());
            if (!selectApprovedRequest(detail)) throw new IllegalArgumentException();
            return detail;
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        if (!selectSafeUri(uri) || !DETAIL.equals(uri.getPath())) return false;
        Map<String, String> query = selectQuery(uri.getRawQuery());
        return query.size() == 7 && query.entrySet().containsAll(DETAIL_PARAMETERS.entrySet())
                && query.getOrDefault("not_ancmt_mgt_no", "").matches("[0-9]{1,15}");
    }

    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request request) {
        if (request == null) return false;
        if ("GET".equals(request.method())) return selectApprovedRequest(request.uri());
        return "POST".equals(request.method()) && selectSafeUri(request.uri()) && DOWNLOAD.equals(request.uri().getPath())
                && request.uri().getRawQuery() == null && request.form().keySet().equals(FORM_FIELDS)
                && "Y".equals(request.form().get("isHome")) && selectOpaqueName(request.form().get("user_file_nm"))
                && selectOpaqueName(request.form().get("sys_file_nm"))
                && request.form().get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }

    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var document = Jsoup.parse(html, detail.toASCIIString());
        var downloadForms = document.select("form[name=nnn]");
        var contentForms = document.select("form[name=form1][method=post]");
        if (downloadForms.size() != 1 || contentForms.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var form = downloadForms.getFirst();
        if (!"post".equalsIgnoreCase(form.attr("method")) || !DOWNLOAD.equals(form.attr("action"))
                || form.childrenSize() != 4 || form.select("input[type=hidden]").size() != 4
                || !form.children().stream().map(e -> e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FORM_FIELDS))
            return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        for (var input : form.children()) {
            if (!input.attributes().asList().stream().allMatch(a -> Set.of("type", "name", "value").contains(a.getKey()))
                    || ("isHome".equals(input.attr("name")) ? !"Y".equals(input.val()) : !input.val().isEmpty()))
                return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
        var labels = contentForms.getFirst().select("td").stream()
                .filter(e -> "첨부파일".equals(e.ownText().replaceAll("[\\s\\u00a0:：]+", ""))).toList();
        if (labels.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var label = labels.getFirst(); var container = label.nextElementSibling();
        if (container == null || !"td".equals(container.tagName()) || !"tr".equals(label.parent().tagName()) || container.nextElementSibling() != null)
            return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        boolean unresolved = !container.select("button,input,select,form,iframe,object,embed,script,[onclick]").isEmpty();
        var residual = container.clone(); residual.select("a").remove();
        if (!residual.text().isBlank() || !residual.select("[href],img").isEmpty()) unresolved = true;
        var anchors = container.select("a");
        if (anchors.isEmpty() && !container.select("li").isEmpty()) unresolved = true;
        boolean exceeded = false;
        var files = new LinkedHashMap<String, Descriptor>();
        for (var anchor : anchors) {
            var arguments = AttachmentDownloadInvocation.selectArguments(anchor.attr("href"), "goDownLoad", false, false);
            String displayName = anchor.text().strip();
            if (arguments.size() != 3 || anchor.hasAttr("onclick") || displayName.isBlank() || displayName.length() > 500
                    || displayName.codePoints().anyMatch(Character::isISOControl)) { unresolved = true; continue; }
            Map<String, String> values = Map.of("user_file_nm", arguments.get(0), "sys_file_nm", arguments.get(1),
                    "file_path", arguments.get(2), "isHome", "Y");
            AttachmentPinnedDownloadClient.Request request;
            try { request = new AttachmentPinnedDownloadClient.Request(URI.create("https://" + HOST + DOWNLOAD), "POST", values); }
            catch (IllegalArgumentException exception) { unresolved = true; continue; }
            if (!selectApprovedRequest(request)) { unresolved = true; continue; }
            // 불투명 locator가 교체되면 hash도 달라져 기존 checkpoint/수동 재시도 범위를 임의 재사용하지 않는다.
            String id = normalizer.hash(values.get("file_path") + "\n" + values.get("sys_file_nm"));
            if (files.containsKey(id)) {
                if (!files.get(id).displayName().equals(displayName)) unresolved = true;
                continue;
            }
            if (files.size() == 10) { exceeded = true; continue; }
            String format = selectFormat(displayName);
            var locator = new AttachmentSetEvidence.Locator(CODE, DOWNLOAD,
                    Map.of("attachmentId", id, "noticeId", selectQuery(detail.getRawQuery()).get("not_ancmt_mgt_no")));
            files.put(id, new Descriptor(request.uri(), locator, displayName, format, "UNKNOWN", format != null, values));
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }

    private boolean selectSafeUri(URI uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null && uri.getFragment() == null
                && uri.equals(uri.normalize()) && uri.getRawPath().equals(uri.getPath());
    }
    private Map<String, String> selectQuery(String query) {
        if (query == null || query.length() > 1024) return Map.of();
        var values = new LinkedHashMap<String, String>();
        try {
            for (String pair : query.split("&", -1)) {
                String[] parts = pair.split("=", -1);
                if (parts.length != 2 || !parts[0].matches("[A-Za-z_]+")
                        || values.putIfAbsent(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8)) != null) return Map.of();
            }
            return values;
        } catch (IllegalArgumentException exception) { return Map.of(); }
    }
    private boolean selectOpaqueName(String value) {
        return value != null && value.length() <= 2048 && value.length() >= 22 && CIPHER_SUFFIX.matcher(value).find()
                && value.codePoints().noneMatch(Character::isISOControl) && !value.contains("..")
                && value.chars().noneMatch(c -> "\\%<>:?#".indexOf(c) >= 0);
    }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
