package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
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
import org.jsoup.nodes.Element;

/** 실측한 새올 GET 구조의 공유 구현. 기관·목록 코드·첨부 영역은 시스템 bean에서만 고정한다. */
public final class SaeolGetAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    static final String DETAIL = "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    static final String DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private static final Map<String, String> DETAIL_PARAMETERS = Map.of("context", "NTIS", "homepage_pbs_yn", "Y",
            "jndinm", "OfrNotAncmtEJB", "method", "selectOfrNotAncmt", "methodnm", "selectOfrNotAncmtRegst", "subCheck", "Y");
    private static final Set<String> FORMATS = Set.of("PDF", "HWP", "HWPX");
    private final String code;
    private final String sourceCode;
    private final String host;
    private final String listCode;
    private final String labelSelector;
    private final boolean upgradeStoredHttp;
    private final String hash;
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();

    SaeolGetAttachmentDiscoveryProfile(String code, String sourceCode, String host, String listCode,
                                      String labelSelector, boolean upgradeStoredHttp) {
        this.code = code; this.sourceCode = sourceCode; this.host = host; this.listCode = listCode;
        this.labelSelector = labelSelector; this.upgradeStoredHttp = upgradeStoredHttp;
        this.hash = AttachmentProfileFingerprint.selectHash(String.join("|", "SAEOL_GET:1", code, sourceCode, host,
                listCode, labelSelector, Boolean.toString(upgradeStoredHttp), "https443", DETAIL, DOWNLOAD,
                "form1-post|exact-label|exact-query|direct-get|all-roles-unknown|limit10"), getClass());
    }

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(sourceCode,listCode)); }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(host); }
    @Override public URI selectDetailUri(String noticeId) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String noticeId, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !sourceCode.equals(source.localSourceCode())
                    || !listCode.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))
                throw new IllegalArgumentException();
            URI uri = URI.create(source.sourceUrl());
            // 저장된 원문의 identity는 보존한다. 실측한 남구의 같은 경로만 HTTPS로 승격하며 HTTP는 요청하지 않는다.
            if (upgradeStoredHttp && "http".equals(uri.getScheme()) && host.equals(uri.getHost())
                    && (uri.getPort() == -1 || uri.getPort() == 80) && uri.getUserInfo() == null && uri.getFragment() == null) {
                uri = URI.create("https://" + host + uri.getRawPath() + "?" + uri.getRawQuery());
            }
            if (!DETAIL.equals(uri.getPath()) || !selectApprovedRequest(uri)) throw new IllegalArgumentException();
            return uri;
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || !host.equalsIgnoreCase(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                || !uri.equals(uri.normalize()) || !uri.getRawPath().equals(uri.getPath())) return false;
        Map<String, String> values = selectParameters(uri.getRawQuery());
        if (DETAIL.equals(uri.getPath())) return values.size() == 7 && values.entrySet().containsAll(DETAIL_PARAMETERS.entrySet())
                && values.getOrDefault("not_ancmt_mgt_no", "").matches("[0-9]{1,15}");
        return DOWNLOAD.equals(uri.getPath()) && values.keySet().equals(Set.of("user_file_nm", "sys_file_nm", "file_path"))
                && selectSafeName(values.get("user_file_nm")) && selectSafeName(values.get("sys_file_nm"))
                && values.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }

    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var document = Jsoup.parse(html, detail.toASCIIString());
        var forms = document.select("form[name=form1][method=post]");
        if (forms.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var labels = forms.getFirst().select(labelSelector).stream()
                .filter(e -> "첨부파일".equals(e.ownText().replaceAll("[\\s\\u00a0:：]+", ""))).toList();
        if (labels.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        Element label = labels.getFirst();
        Element container = label.tagName().equals("div") ? label : label.nextElementSibling();
        if (container == null || (!label.tagName().equals("div")
                && (!"td".equals(container.tagName()) || !"tr".equals(label.parent().tagName()) || container.nextElementSibling() != null)))
            return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var files = new LinkedHashMap<String, Descriptor>();
        boolean unresolved = !container.select("button,input,select,form,iframe,object,embed,script,[onclick]").isEmpty();
        boolean exceeded = false;
        var anchors = container.select("a");
        var residual = container.clone();
        residual.select("a").remove();
        String residualText = residual.text();
        if (label == container) residualText = residualText.replaceFirst("^첨부파일\\s*[:：]?\\s*", "");
        if (!residualText.isBlank() || !residual.select("[href],img").isEmpty()) unresolved = true;
        if (anchors.isEmpty()) {
            String remaining = label == container ? container.text().replaceFirst("^첨부파일\\s*[:：]?\\s*", "") : container.text();
            if (!remaining.isBlank() || !container.select("li,img,[href]").isEmpty()) unresolved = true;
        }
        for (Element anchor : anchors) {
            var arguments = AttachmentDownloadInvocation.selectArguments(anchor.attr("href"), "goDownLoad", false, false);
            if (arguments.size() != 3 || anchor.hasAttr("onclick")) { unresolved = true; continue; }
            String name = arguments.get(0);
            String systemName = arguments.get(1);
            String directory = arguments.get(2);
            URI fetch = URI.create("https://" + host + DOWNLOAD + "?user_file_nm=" + selectEncoded(name)
                    + "&sys_file_nm=" + selectEncoded(systemName) + "&file_path=" + selectEncoded(directory));
            if (!selectApprovedRequest(fetch)) { unresolved = true; continue; }
            String identity = normalizer.hash(directory + "\n" + systemName);
            if (files.containsKey(identity)) {
                if (!files.get(identity).displayName().equals(name)) unresolved = true;
                continue;
            }
            if (files.size() == 10) { exceeded = true; continue; }
            String format = selectFormat(name);
            boolean supported = format != null && format.equals(selectFormat(systemName));
            var locator = new AttachmentSetEvidence.Locator(code, DOWNLOAD, Map.of("attachmentId", identity,
                    "noticeId", selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no")));
            // 첨부 영역은 파일 역할을 명시하지 않는다. 파일명에 '공고문'이 있어도 자동 NOTICE로 만들지 않는다.
            files.put(identity, new Descriptor(fetch, locator, name, supported ? format : null, "UNKNOWN", supported));
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }

    private Map<String, String> selectParameters(String query) {
        if (query == null || query.length() > 8192) return Map.of();
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
    private boolean selectSafeName(String value) {
        return value != null && !value.isBlank() && value.length() <= 500
                && !value.contains("/") && !value.contains("\\") && !value.contains("..") && !value.contains("%")
                && value.indexOf('\ufffd') < 0 && value.codePoints().noneMatch(Character::isISOControl);
    }
    private String selectEncoded(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return FORMATS.contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
