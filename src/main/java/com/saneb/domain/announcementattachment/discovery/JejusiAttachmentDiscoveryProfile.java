package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 제주시 공식 고시공고 첨부 영역. JS는 실행하지 않고 고정 함수의 파일3인자만 읽는다. */
final class JejusiAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_JEJUSI_GET_V1", HOST = "www.jejusi.go.kr", FILE_HOST = "eminwon.jejusi.go.kr";
    private static final String DETAIL = "/information/intro/notice.do", FILE = "/emwp/jsp/ofr/FileDown.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile validator = new SaeolGetAttachmentDiscoveryProfile(CODE, "LGS-000243", FILE_HOST, "SAEOL_GOSI", "td", false);
    private final String hash = AttachmentProfileFingerprint.selectHash(String.join("|", CODE, HOST, FILE_HOST, DETAIL, FILE,
            validator.selectProfileHash(), "1|board-view-default|fixed-3-args|paired-preview-and-icon|https443|same-request|partial-preserved|unknown-role|limit10"), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(HOST, FILE_HOST); }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000243", "SAEOL_GOSI")); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000243".equals(source.localSourceCode())
                    || !"SAEOL_GOSI".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            URI uri = URI.create(source.sourceUrl()); if (!DETAIL.equals(uri.getPath()) || !selectApprovedRequest(uri)) throw new IllegalArgumentException(); return uri;
        } catch (IllegalArgumentException e) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    private boolean selectSafe(URI uri) {
        return uri != null && "https".equals(uri.getScheme()) && selectApprovedHosts().contains(Objects.toString(uri.getHost(), ""))
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null && uri.getFragment() == null
                && uri.equals(uri.normalize()) && Objects.equals(uri.getRawPath(), uri.getPath());
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        if (!selectSafe(uri)) return false;
        if (FILE_HOST.equals(uri.getHost())) return FILE.equals(uri.getPath()) && validator.selectApprovedRequest(uri);
        var q = selectQuery(uri.getRawQuery());
        return DETAIL.equals(uri.getPath()) && "detail".equals(q.get("mode")) && q.getOrDefault("ancmnt_pbanc_mng_no", "").matches("[0-9]{1,15}")
                && Set.of("mode", "ancmnt_pbanc_mng_no", "currentPageNo", "search_se", "search_keyword", "recordCountPerPage").containsAll(q.keySet());
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source); if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page = Jsoup.parse(html, detail.toASCIIString());
        var titles = page.select("div.board-view-default > div.view-wrap > div.view-header > div.title > strong");
        var areas = page.select("div.board-view-default > div.view-wrap > div.view-header > div.file");
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || areas.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var area = areas.getFirst();
        if (area.select("dl > dt").size() != 1 || !"첨부파일".equals(area.select("dl > dt").text()) || area.select("dl > dd").size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var container = area.selectFirst("dl > dd"); String notice = selectQuery(detail.getRawQuery()).get("ancmnt_pbanc_mng_no");
        var files = new LinkedHashMap<String, Descriptor>(); var recognized = new HashSet<Element>(); boolean exceeded = false;
        for (var anchor : container.select("a")) {
            var args = AttachmentDownloadInvocation.selectArguments(anchor.attr("href"), "goDownLoad", false, false);
            if (anchor.hasAttr("onclick") || args.size() != 3 || !anchor.text().equals(args.getFirst())) continue;
            URI fetch;
            try { fetch = URI.create("https://" + FILE_HOST + FILE + "?user_file_nm=" + selectEncoded(args.get(0)) + "&sys_file_nm=" + selectEncoded(args.get(1)) + "&file_path=" + selectEncoded(args.get(2))); }
            catch (IllegalArgumentException e) { continue; }
            if (!selectApprovedRequest(fetch)) continue;
            String id = normalizer.hash(args.get(2) + "\n" + args.get(1));
            if (files.containsKey(id)) { if (files.get(id).fetchUri().equals(fetch) && files.get(id).displayName().equals(args.getFirst())) recognized.add(anchor); continue; }
            if (files.size() == 10) { exceeded = true; continue; }
            String format = selectFormat(args.getFirst()); boolean supported = format != null && format.equals(selectFormat(args.get(1)));
            files.put(id, new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, FILE, Map.of("noticeId", notice, "attachmentId", id)), args.getFirst(), supported ? format : null, "UNKNOWN", supported));
            recognized.add(anchor);
            var preview = anchor.nextElementSibling();
            if (preview != null && "a".equals(preview.tagName()) && !preview.hasAttr("onclick") && preview.hasClass("download") && "바로보기".equals(preview.text())) try {
                URI u = detail.resolve(URI.create(preview.attr("href").replace(" ", "%20"))); var q = selectQuery(u.getRawQuery());
                if (selectSafe(u) && HOST.equals(u.getHost()) && "/ancmntPbancFileDocViewer.ac".equals(u.getPath())
                        && q.keySet().equals(Set.of("ancmnt_pbanc_mng_no", "de_user_file_nm", "de_sys_file_nm", "de_file_path"))
                        && q.get("ancmnt_pbanc_mng_no").matches(notice + "_[1-9][0-9]?") && args.get(0).equals(q.get("de_user_file_nm"))
                        && args.get(1).equals(q.get("de_sys_file_nm")) && args.get(2).equals(q.get("de_file_path"))) recognized.add(preview);
            } catch (IllegalArgumentException ignored) { /* 미확인 미리보기는 실행하지 않는다. */ }
            var icon = anchor.previousElementSibling();
            if (icon != null && "img".equals(icon.tagName()) && "/images/jejusi/board/link_file.jpg".equals(icon.attr("src")) && "".equals(icon.attr("alt")) && !icon.hasAttr("onclick")) recognized.add(icon);
        }
        boolean unresolved = container.select("a").stream().anyMatch(a -> !recognized.contains(a));
        recognized.forEach(Element::remove);
        if (!container.text().isBlank() || !container.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()
                || files.isEmpty() && !container.select("li,p").isEmpty()) unresolved = true;
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectEncoded(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String selectFormat(String name) { String ext = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF", "HWP", "HWPX").contains(ext) ? ext : null; }
    private Map<String, String> selectQuery(String query) {
        if (query == null || query.length() > 8192) return Map.of(); var values = new LinkedHashMap<String, String>();
        try { for (String pair : query.split("&", -1)) {
            String[] p = pair.split("=", -1); if (p.length != 2 || !p[0].matches("[A-Za-z_]+")) return Map.of(); String v = URLDecoder.decode(p[1], StandardCharsets.UTF_8);
            if (v.length() > 500 || v.codePoints().anyMatch(Character::isISOControl) || values.putIfAbsent(p[0], v) != null) return Map.of();
        } return values; } catch (IllegalArgumentException e) { return Map.of(); }
    }
    private Result selectFailed(String reason) { return new Result("FAILED", false, List.of(), List.of(reason)); }
}
