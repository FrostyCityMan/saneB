package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;

/** 기관별 경로를 시스템 설정으로 고정한 SCMS 새올 상세·첨부 처리기. */
final class ScmsSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String FILE = "/emwp/jsp/ofr/FileDown.jsp";
    private final String code, host, fileHost, sourceCode, parserCode, detailPath, downloadPath, hash;
    private final boolean directFile, legacyIdentity;
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile validator;

    ScmsSaeolAttachmentDiscoveryProfile(String region, String domain, String sourceCode, String parserCode,
            String detailPath, String downloadPath, boolean directFile, boolean legacyIdentity) {
        this.code = "LOCAL_" + region + "_SCMS_V1"; this.host = "www." + domain; this.fileHost = "eminwon." + domain;
        this.sourceCode = sourceCode; this.parserCode = parserCode; this.detailPath = detailPath; this.downloadPath = downloadPath;
        this.directFile = directFile; this.legacyIdentity = legacyIdentity;
        validator = new SaeolGetAttachmentDiscoveryProfile(code, sourceCode, fileHost, parserCode, "td", false);
        hash = AttachmentProfileFingerprint.selectHash(String.join("|", code, host, fileHost, sourceCode, parserCode, detailPath, downloadPath,
                Boolean.toString(directFile), Boolean.toString(legacyIdentity), validator.selectProfileHash(),
                "2|fixed-official-area|https443|same-request|nested-spaces|partial-preserved|unknown-role|limit10|hc-observed-x-msdownload-utf8-disposition"), getClass());
    }
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    // 합천의 실제 PK signature·HWPX disposition과 함께 관측한 구형 MIME만 기존 검증기에 허용한다.
    @Override public Set<String> selectLegacyBinaryContentTypes() { return "www.hc.go.kr".equals(host) ? Set.of("application/x-msdownload") : Set.of(); }
    @Override public boolean selectUtf8DispositionOctets() { return "www.hc.go.kr".equals(host); }
    @Override public Set<String> selectApprovedHosts() { return directFile ? Set.of(host, fileHost) : Set.of(host); }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(sourceCode, parserCode)); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !sourceCode.equals(source.localSourceCode())
                    || !parserCode.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            URI uri = URI.create(source.sourceUrl());
            if (legacyIdentity && fileHost.equals(uri.getHost()) && SaeolGetAttachmentDiscoveryProfile.DETAIL.equals(uri.getPath())) {
                // 통영에서 같은 공고 번호·제목·파일을 확인한 새올 identity만 보존해 공식 HTTPS 상세로 연결한다.
                URI checked = uri;
                if ("http".equals(uri.getScheme()) && (uri.getPort() == -1 || uri.getPort() == 80) && uri.getUserInfo() == null && uri.getFragment() == null)
                    checked = URI.create("https://" + fileHost + uri.getRawPath() + "?" + uri.getRawQuery());
                if (!validator.selectApprovedRequest(checked)) throw new IllegalArgumentException();
                return URI.create("https://" + host + detailPath + "?amode=view&not_ancmt_mgt_no=" + selectQuery(checked.getRawQuery()).get("not_ancmt_mgt_no"));
            }
            if (!detailPath.equals(uri.getPath()) || !selectApprovedRequest(uri)) throw new IllegalArgumentException();
            return uri;
        } catch (IllegalArgumentException e) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    private boolean selectSafe(URI uri) {
        return uri != null && "https".equals(uri.getScheme()) && selectApprovedHosts().contains(Objects.toString(uri.getHost(), ""))
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null && uri.getFragment() == null
                && uri.equals(uri.normalize()) && Objects.equals(uri.getRawPath(), uri.getPath());
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        if (!selectSafe(uri)) return false;
        var q = selectQuery(uri.getRawQuery());
        if (host.equals(uri.getHost()) && detailPath.equals(uri.getPath())) return "view".equals(q.get("amode"))
                && q.getOrDefault("not_ancmt_mgt_no", "").matches("[0-9]{1,15}")
                && Set.of("amode", "not_ancmt_mgt_no", "sstring", "stype", "cpage", "pbsDivision").containsAll(q.keySet());
        return selectDownload(uri);
    }
    private boolean selectDownload(URI uri) {
        if (!selectSafe(uri) || !downloadPath.equals(uri.getPath())) return false;
        if (directFile) return fileHost.equals(uri.getHost()) && validator.selectApprovedRequest(URI.create("https://" + fileHost + FILE + "?" + uri.getRawQuery()));
        var q = selectQuery(uri.getRawQuery());
        if (!host.equals(uri.getHost()) || !q.keySet().equals(Set.of("url", "name")) || !selectName(q.get("name"))) return false;
        try {
            URI inner = URI.create(q.get("url").replace(" ", "%20"));
            return Set.of("http", "https").contains(Objects.toString(inner.getScheme(), "")) && FILE.equals(inner.getPath())
                    && validator.selectApprovedRequest(URI.create(inner.toString().replaceFirst("^http:", "https:")))
                    && q.get("name").equals(selectQuery(inner.getRawQuery()).get("user_file_nm"));
        } catch (IllegalArgumentException e) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page = Jsoup.parse(html, detail.toASCIIString());
        var titles = page.select("form#saeolGosiVO > div.bbs1view1 > h1.h1");
        var areas = page.select("form#saeolGosiVO > div.bbs1view1 > div.attach1");
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || areas.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var area = areas.getFirst(); var files = new LinkedHashMap<String, Descriptor>();
        var recognized = new HashSet<org.jsoup.nodes.Element>(); boolean exceeded = false;
        String notice = selectQuery(detail.getRawQuery()).get("not_ancmt_mgt_no");
        for (var anchor : area.select("a")) try {
            if (anchor.hasAttr("onclick") || !selectName(anchor.text())) continue;
            URI fetch = detail.resolve(URI.create(anchor.attr("href").replace(" ", "%20")));
            if (!selectDownload(fetch)) continue;
            var outer = selectQuery(fetch.getRawQuery());
            var fields = directFile ? outer : selectQuery(URI.create(outer.get("url").replace(" ", "%20")).getRawQuery());
            if (!anchor.text().equals(fields.get("user_file_nm"))) continue;
            String id = normalizer.hash(fields.get("file_path") + "\n" + fields.get("sys_file_nm"));
            if (files.containsKey(id)) {
                if (files.get(id).fetchUri().equals(fetch) && files.get(id).displayName().equals(anchor.text())) recognized.add(anchor);
                continue;
            }
            if (files.size() == 10) { exceeded = true; continue; }
            String format = selectFormat(anchor.text()); boolean supported = format != null && format.equals(selectFormat(fields.get("sys_file_nm")));
            files.put(id, new Descriptor(fetch, new AttachmentSetEvidence.Locator(code, downloadPath, Map.of("noticeId", notice, "attachmentId", id)),
                    anchor.text(), supported ? format : null, "UNKNOWN", supported)); recognized.add(anchor);
        } catch (IllegalArgumentException ignored) { /* 잘못된 링크를 분리하고 정상 파일은 계속 처리한다. */ }
        boolean unresolved = area.select("a").stream().anyMatch(a -> !recognized.contains(a));
        var residual = area.clone(); residual.select("a").remove();
        if (!residual.text().isBlank() || !residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()
                || files.isEmpty() && !residual.select("li,p").isEmpty()) unresolved = true;
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectFormat(String name) { String ext = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF", "HWP", "HWPX").contains(ext) ? ext : null; }
    private boolean selectName(String value) {
        return value != null && !value.isBlank() && value.length() <= 500 && !value.contains("/") && !value.contains("\\")
                && !value.contains("..") && !value.contains("%") && value.indexOf('\ufffd') < 0 && value.codePoints().noneMatch(Character::isISOControl);
    }
    private Map<String, String> selectQuery(String query) {
        if (query == null || query.length() > 8192) return Map.of(); var values = new LinkedHashMap<String, String>();
        try { for (String pair : query.split("&", -1)) {
            String[] p = pair.split("=", -1); if (p.length != 2 || !p[0].matches("[A-Za-z_]+")) return Map.of();
            String value = URLDecoder.decode(p[1], StandardCharsets.UTF_8);
            if (value.length() > 4096 || value.codePoints().anyMatch(Character::isISOControl) || values.putIfAbsent(p[0], value) != null) return Map.of();
        } return values; } catch (IllegalArgumentException e) { return Map.of(); }
    }
    private Result selectFailed(String reason) { return new Result("FAILED", false, List.of(), List.of(reason)); }
}
