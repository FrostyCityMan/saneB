package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 고성·창원의 공식 첨부 영역을 읽고 미해석 링크와 정상 파일을 분리한다. */
final class GyeongnamBoardAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { GOSEONG, CHANGWON }
    private final Site site;
    private final String code, host, fileHost, sourceCode, parserCode, detailPath, downloadPath, hash;
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile fileValidator;

    GyeongnamBoardAttachmentDiscoveryProfile(Site site) {
        this.site = site;
        String domain = site.name().toLowerCase(Locale.ROOT) + ".go.kr";
        code = "LOCAL_" + site + "_BOARD_V1"; host = "www." + domain; fileHost = "eminwon." + domain;
        sourceCode = site == Site.GOSEONG ? "LGS-000235" : "LGS-000224";
        parserCode = site == Site.GOSEONG ? "HEURISTIC_NOTICE" : "CHANGWON_GOSI_TABLE";
        detailPath = site == Site.GOSEONG ? "/board/view.goseong" : "/cwportal/10310/10438/10439.web";
        downloadPath = site == Site.GOSEONG ? "/emwp/jsp/ofr/FileDown.jsp" : "/cwportal/DownloadEx.do";
        fileValidator = new SaeolGetAttachmentDiscoveryProfile(code, sourceCode, fileHost, parserCode, "td", false);
        hash = AttachmentProfileFingerprint.selectHash(String.join("|", code, host, fileHost, sourceCode, parserCode,
                detailPath, downloadPath, fileValidator.selectProfileHash(), "1|https443|official-area|partial-preserved|same-request|unknown-role|limit10"), getClass());
    }
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    @Override public Set<String> selectApprovedHosts() { return site == Site.GOSEONG ? Set.of(host, fileHost) : Set.of(host); }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(sourceCode, parserCode)); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !sourceCode.equals(source.localSourceCode())
                    || !parserCode.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            URI uri = URI.create(source.sourceUrl());
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
        if (host.equals(uri.getHost()) && detailPath.equals(uri.getPath())) {
            if (site == Site.GOSEONG) return "BBS_0000015".equals(q.get("boardId")) && "DOM_000000103001014000".equals(q.get("menuCd"))
                    && selectId(q.get("dataSid")) && Set.of("boardId", "menuCd", "dataSid", "paging", "startPage", "searchType", "keyword").containsAll(q.keySet())
                    && (!q.containsKey("paging") || "ok".equals(q.get("paging"))) && (!q.containsKey("startPage") || selectId(q.get("startPage")));
            return "view".equals(q.get("amode")) && "gosi".equals(q.get("section")) && selectId(q.get("not_ancmt_mgt_no"))
                    && Set.of("amode", "section", "not_ancmt_mgt_no", "sstring", "stype", "cpage", "pbsDivision").containsAll(q.keySet());
        }
        return selectDownload(uri);
    }
    private boolean selectDownload(URI uri) {
        if (!selectSafe(uri) || !downloadPath.equals(uri.getPath())) return false;
        if (site == Site.GOSEONG) return fileHost.equals(uri.getHost()) && fileValidator.selectApprovedRequest(uri);
        var q = selectQuery(uri.getRawQuery());
        if (!host.equals(uri.getHost()) || !q.keySet().equals(Set.of("url", "name")) || !selectName(q.get("name"))) return false;
        try {
            URI inner = URI.create(q.get("url").replace(" ", "%20"));
            if (!Set.of("http", "https").contains(Objects.toString(inner.getScheme(), "")) || !"/emwp/jsp/ofr/FileDown.jsp".equals(inner.getPath())) return false;
            // 내부 HTTP 주소는 검증만 하며 직접 요청하지 않는다. 요청 대상은 공식 HTTPS 프록시다.
            return fileValidator.selectApprovedRequest(URI.create(inner.toString().replaceFirst("^http:", "https:")))
                    && q.get("name").equals(selectQuery(inner.getRawQuery()).get("user_file_nm"));
        } catch (IllegalArgumentException e) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        return initial != null && initial.equals(next) && selectApprovedRequest(next);
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page = Jsoup.parse(html, detail.toASCIIString());
        var titles = page.select(site == Site.GOSEONG ? "div.bdvTitWrap > p.bdvTit" : "form#saeolGosiVO > div.bbs1view1 > h1.h1");
        var areas = page.select(site == Site.GOSEONG ? "div.bdvFileWrap" : "form#saeolGosiVO > div.bbs1view1 > div.attach1");
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || areas.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        Element area = areas.getFirst();
        if (site == Site.GOSEONG) {
            if (area.select("p.bdvf_tit").size() != 1 || !"첨부파일".equals(area.select("p.bdvf_tit").text()) || area.select("ul.bdvFileBox").size() != 1)
                return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
            area = area.selectFirst("ul.bdvFileBox");
        }
        String notice = selectQuery(detail.getRawQuery()).get(site == Site.GOSEONG ? "dataSid" : "not_ancmt_mgt_no");
        var files = new LinkedHashMap<String, Descriptor>(); var recognized = new HashSet<Element>(); boolean exceeded = false;
        for (var anchor : area.select("a")) try {
            if (anchor.hasAttr("onclick") || !selectName(anchor.text())) continue;
            URI fetch = detail.resolve(URI.create(anchor.attr("href").replace(" ", "%20")));
            if (!selectDownload(fetch)) continue;
            var outer = selectQuery(fetch.getRawQuery());
            var fields = site == Site.GOSEONG ? outer : selectQuery(URI.create(outer.get("url").replace(" ", "%20")).getRawQuery());
            if (!anchor.text().equals(fields.get("user_file_nm"))) continue;
            String id = normalizer.hash(fields.get("file_path") + "\n" + fields.get("sys_file_nm"));
            if (files.containsKey(id)) {
                if (files.get(id).fetchUri().equals(fetch) && files.get(id).displayName().equals(anchor.text())) recognized.add(anchor);
                continue;
            }
            if (files.size() == 10) { exceeded = true; continue; }
            String format = selectFormat(anchor.text());
            boolean supported = format != null && format.equals(selectFormat(fields.get("sys_file_nm")));
            files.put(id, new Descriptor(fetch, new AttachmentSetEvidence.Locator(code, downloadPath, Map.of("noticeId", notice, "attachmentId", id)),
                    anchor.text(), supported ? format : null, "UNKNOWN", supported)); recognized.add(anchor);
        } catch (IllegalArgumentException ignored) { /* 다른 정상 파일의 처리는 계속한다. */ }
        // 미리보기 링크와 스크립트는 호출하지 않는다. 아직 검증하지 않은 변형은 별도 발견 오류다.
        boolean unresolved = area.select("a").stream().anyMatch(a -> !recognized.contains(a));
        var residual = area.clone(); residual.select("a").remove();
        if (!residual.text().isBlank() || !residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()
                || files.isEmpty() && !residual.select("li,p").isEmpty()) unresolved = true;
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectFormat(String name) {
        String suffix = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private boolean selectId(String value) { return value != null && value.matches("[0-9]{1,15}"); }
    private boolean selectName(String value) {
        return value != null && !value.isBlank() && value.length() <= 500 && !value.contains("/") && !value.contains("\\")
                && !value.contains("..") && !value.contains("%") && value.indexOf('\ufffd') < 0 && value.codePoints().noneMatch(Character::isISOControl);
    }
    private Map<String, String> selectQuery(String query) {
        if (query == null || query.length() > 8192) return Map.of();
        var values = new LinkedHashMap<String, String>();
        try {
            for (String pair : query.split("&", -1)) {
                String[] p = pair.split("=", -1); if (p.length != 2 || !p[0].matches("[A-Za-z_]+")) return Map.of();
                String value = URLDecoder.decode(p[1], StandardCharsets.UTF_8);
                if (value.length() > 4096 || value.codePoints().anyMatch(Character::isISOControl) || values.putIfAbsent(p[0], value) != null) return Map.of();
            }
            return values;
        } catch (IllegalArgumentException e) { return Map.of(); }
    }
    private Result selectFailed(String reason) { return new Result("FAILED", false, List.of(), List.of(reason)); }
}
