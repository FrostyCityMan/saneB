package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 실측한 기관별 BBS만 지원한다. 목록 parser가 같다는 이유로 다른 기관을 지원하지 않는다. */
public final class StandardBbsAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Layout { CLASSIC, COMPACT, COMPACT_MENU_KEY, COMPACT_SVG, COMPACT_BOARD_PREVIEW, COMPACT_LABELLED_CONTENT }
    enum FileHeaders { EXISTING_PROFILE, MS_DOWNLOAD_STANDARD_DISPOSITION }
    static final String DETAIL = "/www/selectBbsNttView.do";
    static final String DOWNLOAD = "/www/downloadBbsFile.do";
    private static final Set<String> SOURCE_PARAMETERS = Set.of("key", "bbsNo", "nttNo", "searchCtgry",
            "searchCnd", "searchKrwd", "pageIndex", "pageUnit", "integrDeptCode");
    private final String code;
    private final String sourceCode;
    private final String host;
    private final String board;
    private final String menu;
    private final boolean compactLayout;
    private final Layout layout;
    private final String listParser;
    private final boolean upgradeStoredHttp;
    private final FileHeaders fileHeaders;
    private final String hash;
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();

    StandardBbsAttachmentDiscoveryProfile(String code, String sourceCode, String host, String board, String menu,
                                          Layout layout, boolean upgradeStoredHttp, String listParser) {
        this(code, sourceCode, host, board, menu, layout, upgradeStoredHttp, listParser, FileHeaders.EXISTING_PROFILE);
    }

    StandardBbsAttachmentDiscoveryProfile(String code, String sourceCode, String host, String board, String menu,
                                          Layout layout, boolean upgradeStoredHttp, String listParser, FileHeaders fileHeaders) {
        this.code = code; this.sourceCode = sourceCode; this.host = host; this.board = board; this.menu = menu;
        this.layout = layout; this.listParser = listParser;
        this.fileHeaders = java.util.Objects.requireNonNull(fileHeaders);
        this.compactLayout = layout != Layout.CLASSIC; this.upgradeStoredHttp = upgradeStoredHttp;
        this.hash = AttachmentProfileFingerprint.selectHash(String.join("|", "STANDARD_BBS:2", code, sourceCode,
                host, board, menu, layout.name(), Boolean.toString(upgradeStoredHttp), listParser, fileHeaders.name(),
                "https443|session-free|exact-file-cell|unknown-role|limit10|no-preview-fetch"), getClass());
    }

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(sourceCode, listParser)); }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    @Override public Set<String> selectApprovedHosts() { return Set.of(host); }
    // 기관별로 실측한 header octet만 복원한다. 제천은 실제 기본 검사 통과를 확인해 복원하지 않는다.
    @Override public boolean selectUtf8DispositionOctets() { return fileHeaders == FileHeaders.EXISTING_PROFILE && !upgradeStoredHttp && layout != Layout.COMPACT_SVG && layout != Layout.COMPACT_BOARD_PREVIEW; }
    @Override public Set<String> selectLegacyBinaryContentTypes() {
        if (fileHeaders == FileHeaders.MS_DOWNLOAD_STANDARD_DISPOSITION) return Set.of("application/x-msdownload");
        if (layout == Layout.COMPACT_MENU_KEY) return Set.of();
        if (layout == Layout.COMPACT_SVG || layout == Layout.COMPACT_BOARD_PREVIEW) return Set.of("application/x-msdownload");
        return Set.of(upgradeStoredHttp ? "application/x-msdownload" : "application/octer-stream");
    }
    @Override public URI selectDetailUri(String noticeId) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String noticeId, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !sourceCode.equals(source.localSourceCode())
                    || !listParser.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))
                throw new IllegalArgumentException();
            URI original = URI.create(source.sourceUrl());
            if (!selectOrigin(original, upgradeStoredHttp)) throw new IllegalArgumentException();
            String path = original.getRawPath();
            // 횡성 목록의 익명 세션 경로는 원문 identity 검증에만 사용한다. 네트워크/locator에 전달하지 않는다.
            if (layout == Layout.COMPACT && "www.hsg.go.kr".equals(host)
                    && path.matches(DETAIL.replace(".", "\\.") + ";jsessionid=[A-Za-z0-9.-]{1,128}")) path = DETAIL;
            Map<String, String> query = selectParameters(original.getRawQuery());
            // 제천 목록의 빈 id는 collector가 정규화한 저장 URL에서만 제거한다. 새 요청에는 전송하지 않는다.
            if (layout == Layout.COMPACT_SVG && "".equals(query.get("id"))) {
                query = new LinkedHashMap<>(query);
                query.remove("id");
            }
            if (!DETAIL.equals(path) || !SOURCE_PARAMETERS.containsAll(query.keySet()) || !selectDetailParameters(query))
                throw new IllegalArgumentException();
            return URI.create("https://" + host + DETAIL + "?key=" + menu + "&bbsNo=" + board + "&nttNo=" + query.get("nttNo"));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        if (!selectOrigin(uri, false)) return false;
        Map<String, String> query = selectParameters(uri.getRawQuery());
        if (DETAIL.equals(uri.getPath())) return query.keySet().equals(Set.of("key", "bbsNo", "nttNo")) && selectDetailParameters(query);
        return DOWNLOAD.equals(uri.getPath()) && selectDownloadParameters(query);
    }

    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var document = Jsoup.parse(html, detail.toASCIIString());
        var tables = document.select(layout == Layout.COMPACT_MENU_KEY ? "div.bbs_wrap > div.p-wrap.bbs.bbs__view > table.p-table"
                : compactLayout ? "div.p-wrap.bbs.bbs__view > table.p-table.block" : "table.bbs_default.view");
        if (tables.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        Element table = tables.getFirst();
        var ownSubjects = table.select("span.p-table__subject_text").stream().filter(e -> e.closest("table") == table).toList();
        boolean subject = layout == Layout.COMPACT_BOARD_PREVIEW || layout == Layout.COMPACT || layout == Layout.COMPACT_LABELLED_CONTENT
                ? ownSubjects.size() == 1 && !ownSubjects.getFirst().text().isBlank()
                : table.select("th").stream().filter(e -> e.closest("table") == table && "제목".equals(e.text().trim()) && e.nextElementSibling() != null
                        && "td".equals(e.nextElementSibling().tagName()) && !e.nextElementSibling().text().isBlank()).count() == 1;
        if (!subject || !selectContentMarker(table)) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var labels = table.select("th").stream().filter(e -> e.closest("table") == table
                && (layout == Layout.COMPACT_SVG ? "첨부파일" : "파일").equals(e.text().trim())).toList();
        if (labels.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        Element label = labels.getFirst(), cell = label.nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || !"tr".equals(label.parent().tagName()) || cell.nextElementSibling() != null)
            return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        String listSelector = compactLayout ? "ul.p-attach" : "ul.view_attach";
        var lists = cell.select(listSelector);
        if (lists.isEmpty()) return cell.text().isBlank() && cell.children().isEmpty()
                ? new Result("NO_FILES", true, List.of(), List.of()) : selectFailed("ATTACHMENT_LINK_UNRESOLVED");
        if (lists.size() != 1 || lists.getFirst().parent() != cell) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        Element list = lists.getFirst(), outside = cell.clone();
        outside.select(listSelector).remove();
        boolean unresolved = !outside.text().isBlank() || !outside.children().isEmpty() || selectActiveContent(cell)
                || !list.ownText().isBlank() || list.children().stream().anyMatch(e -> !"li".equals(e.tagName()));
        var files = new LinkedHashMap<String, Descriptor>();
        boolean exceeded = false;
        String noticeId = selectParameters(detail.getRawQuery()).get("nttNo");
        for (Element item : list.children()) {
            if (!"li".equals(item.tagName()) || selectActiveContent(item)) { unresolved = true; continue; }
            var downloads = item.select(compactLayout ? "a.p-attach__link" : "a.file_down, a.file_down2");
            if (downloads.size() != 1) { unresolved = true; continue; }
            Element anchor = downloads.getFirst();
            URI fetch = selectResolved(detail, anchor.attr("href"));
            if (fetch == null || !DOWNLOAD.equals(fetch.getPath()) || !selectApprovedRequest(fetch)) { unresolved = true; continue; }
            var names = compactLayout ? anchor.select("span:not(.p-icon)") : item.select("div.down_view > span");
            if (names.size() != 1) { unresolved = true; continue; }
            Element nameElement = names.getFirst();
            String name = nameElement.text().trim();
            if (!selectSafeName(name) || !selectFileStructure(item, anchor, nameElement)) { unresolved = true; continue; }
            String attachmentId = selectParameters(fetch.getRawQuery()).get("atchmnflNo");
            Element residue = item.clone();
            residue.select(compactLayout ? "a.p-attach__link" : "div.down_view > span, a.file_down, a.file_down2").remove();
            var previews = item.select(compactLayout ? "a.p-attach__preview" : "a.file_view");
            if (previews.size() > 1 || previews.stream().anyMatch(a -> !selectPreview(detail, a.attr("href"), attachmentId, noticeId, name)
                    || (layout == Layout.COMPACT_SVG && (a.parent() != item || a.childrenSize() != 1 || !selectSvgIcon(a.child(0), false))))) unresolved = true;
            else residue.select(compactLayout ? "a.p-attach__preview" : "a.file_view").remove();
            // 이름/다운로드/검증한 미리보기 외 요소를 숨겨 부분 발견을 완료로 만들지 않는다.
            if (!residue.text().isBlank() || !residue.select("*:not(li):not(div)").isEmpty()
                    || residue.select("div").stream().anyMatch(e -> !e.hasClass("down_view"))) unresolved = true;
            String identity = normalizer.hash(host + "\n" + board + "\n" + attachmentId);
            if (files.containsKey(identity)) {
                if (!files.get(identity).displayName().equals(name)) unresolved = true;
                continue;
            }
            if (files.size() == 10) { exceeded = true; continue; }
            String format = selectFormat(name);
            var locator = new AttachmentSetEvidence.Locator(code, DOWNLOAD, Map.of("attachmentId", identity, "noticeId", noticeId));
            files.put(identity, new Descriptor(fetch, locator, name, format, "UNKNOWN", format != null));
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }

    private boolean selectOrigin(URI uri, boolean allowHttp) {
        if (uri == null || !host.equalsIgnoreCase(uri.getHost()) || uri.getUserInfo() != null || uri.getFragment() != null
                || !uri.equals(uri.normalize()) || uri.getRawPath() == null || !uri.getRawPath().equals(uri.getPath())) return false;
        return ("https".equalsIgnoreCase(uri.getScheme()) && (uri.getPort() == -1 || uri.getPort() == 443))
                || (allowHttp && "http".equalsIgnoreCase(uri.getScheme()) && (uri.getPort() == -1 || uri.getPort() == 80));
    }
    private boolean selectDetailParameters(Map<String, String> query) {
        return menu.equals(query.get("key")) && board.equals(query.get("bbsNo")) && selectId(query.get("nttNo"));
    }
    private boolean selectDownloadParameters(Map<String, String> query) {
        if (!selectId(query.get("atchmnflNo"))) return false;
        if (layout == Layout.COMPACT || layout == Layout.COMPACT_SVG || layout == Layout.COMPACT_BOARD_PREVIEW
                || layout == Layout.COMPACT_LABELLED_CONTENT) return query.keySet().equals(Set.of("atchmnflNo"));
        return upgradeStoredHttp || layout == Layout.COMPACT_MENU_KEY ? query.keySet().equals(Set.of("key", "atchmnflNo")) && menu.equals(query.get("key"))
                : query.keySet().equals(Set.of("bbsNo", "atchmnflNo")) && board.equals(query.get("bbsNo"));
    }
    private boolean selectPreview(URI detail, String href, String attachmentId, String noticeId, String displayName) {
        URI uri = selectResolved(detail, href);
        if (!selectOrigin(uri, false)) return false;
        Map<String, String> query = selectParameters(uri.getRawQuery());
        if (layout == Layout.COMPACT_LABELLED_CONTENT) {
            String suffix = displayName.substring(displayName.lastIndexOf('.') + 1);
            // 미리보기는 다운로드/파일 소유의 근거가 아니다. 표시 구조만 확인하며 요청은 승인하지 않는다.
            return suffix.matches("[A-Za-z0-9]{1,10}") && "/common/program/synap.jsp".equals(uri.getPath())
                    && query.keySet().equals(Set.of("fileName"))
                    && query.get("fileName").matches("/DATA/bbs/" + board
                        + "/[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}\\."
                        + java.util.regex.Pattern.quote(suffix));
        }
        if (layout == Layout.COMPACT_SVG) return "/previewBbs.do".equals(uri.getPath())
                && query.equals(Map.of("atchmnflNo", attachmentId));
        if (layout == Layout.COMPACT_BOARD_PREVIEW) return "/www/previewBbsFile.do".equals(uri.getPath())
                && query.equals(Map.of("key", menu, "bbsNo", board, "atchmnflNo", attachmentId));
        if (layout == Layout.COMPACT) return "/www/previewBbsFile.do".equals(uri.getPath())
                && query.equals(Map.of("atchmnflNo", attachmentId));
        if (upgradeStoredHttp || layout == Layout.COMPACT_MENU_KEY) return "/www/previewUrl.do".equals(uri.getPath())
                && query.equals(Map.of("key", menu, "atchmnflNo", attachmentId));
        return "/common/program/synap.jsp".equals(uri.getPath()) && query.keySet().equals(Set.of("fileName", "nttNo", "FileIndex"))
                && noticeId.equals(query.get("nttNo")) && query.get("FileIndex").matches("[0-9]{1,3}")
                && query.get("fileName").matches("/DATA/bbs/" + board + "/[A-Za-z0-9-]{1,100}\\.[A-Za-z0-9]{1,10}");
    }
    private URI selectResolved(URI detail, String href) {
        try {
            if (href == null || href.isBlank() || href.length() > 4096 || href.codePoints().anyMatch(Character::isISOControl)) return null;
            URI relative = URI.create(href);
            // 관측한 ./만 허용하고 ../로 다른 경로를 거쳐 정규화되는 링크는 거부한다.
            if (relative.getRawPath() != null && relative.getRawPath().contains("..")) return null;
            return detail.resolve(relative);
        } catch (IllegalArgumentException exception) { return null; }
    }
    private Map<String, String> selectParameters(String query) {
        if (query == null || query.length() > 8192) return Map.of();
        var values = new LinkedHashMap<String, String>();
        try {
            for (String pair : query.split("&", -1)) {
                String[] parts = pair.split("=", -1);
                if (parts.length != 2 || !parts[0].matches("[A-Za-z]+")) return Map.of();
                String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                if (value.length() > 1024 || value.codePoints().anyMatch(Character::isISOControl) || value.indexOf('\ufffd') >= 0
                        || values.putIfAbsent(parts[0], value) != null) return Map.of();
            }
            return values;
        } catch (IllegalArgumentException exception) { return Map.of(); }
    }
    private boolean selectActiveContent(Element element) {
        return !element.select("button,input,select,form,iframe,object,embed,script,style").isEmpty()
                || element.getAllElements().stream().anyMatch(e -> e.attributes().asList().stream()
                        .anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));
    }
    private boolean selectFileStructure(Element item, Element anchor, Element name) {
        if (!name.select("[href],[srcset]").isEmpty()) return false;
        if (layout == Layout.COMPACT_SVG) return item.hasClass("p-attch__item") && anchor.parent() == item
                && anchor.childrenSize() == 3 && anchor.child(0).hasClass("p-icon")
                && "span".equals(anchor.child(0).tagName()) && anchor.child(0).children().isEmpty()
                && anchor.child(1) == name && name.children().isEmpty() && selectSvgIcon(anchor.child(2), true);
        if (compactLayout) return item.hasClass("p-attach__item") && anchor.parent() == item
                && anchor.children().size() == 2 && anchor.child(0).hasClass("p-icon")
                && "span".equals(anchor.child(0).tagName()) && anchor.child(0).children().isEmpty()
                && anchor.child(1) == name && name.children().isEmpty();
        return item.children().size() == 1 && item.child(0).hasClass("down_view")
                && name.parent() == item.child(0) && anchor.parent() == item.child(0)
                && name.children().stream().allMatch(e -> "img".equals(e.tagName())
                    && e.attr("src").matches("/common/images/board/file/ico_[A-Za-z0-9]+\\.gif"))
                && name.children().size() <= 1 && anchor.children().stream().allMatch(e -> "i".equals(e.tagName())
                    && e.attributes().size() == 0 && e.children().isEmpty() && e.text().isBlank());
    }
    private boolean selectSvgIcon(Element svg, boolean download) {
        if (!"svg".equals(svg.tagName()) || svg.childrenSize() != 1 || !svg.ownText().isBlank()
                || !Set.of("width", "height", "fill", "focusable", "class").containsAll(svg.attributes().asList().stream().map(a -> a.getKey()).toList())
                || !svg.attr("width").matches("[1-9][0-9]{0,2}") || !svg.attr("height").matches("[1-9][0-9]{0,2}")
                || !svg.attr("fill").matches("#[0-9A-Fa-f]{6}") || !"false".equals(svg.attr("focusable"))
                || !(download ? "margin_l_5" : "").equals(svg.className())) return false;
        Element use = svg.child(0);
        return "use".equals(use.tagName()) && use.children().isEmpty() && use.text().isBlank()
                && (download ? Set.of("xlink:href", "y") : Set.of("xlink:href"))
                    .equals(Set.copyOf(use.attributes().asList().stream().map(a -> a.getKey()).toList()))
                && ("/common/images/program/p-icon.svg#" + (download ? "arrow-circle-down" : "search")).equals(use.attr("xlink:href"))
                && (!download || "2".equals(use.attr("y")));
    }
    private boolean selectId(String value) { return value != null && value.matches("[1-9][0-9]{0,14}"); }
    private boolean selectContentMarker(Element table) {
        if (layout != Layout.COMPACT_LABELLED_CONTENT)
            return table.select("td[title=내용]").stream().filter(e -> e.closest("table") == table).count() == 1;
        var labels = table.select("th").stream().filter(e -> e.closest("table") == table && "내용".equals(e.text().trim())).toList();
        if (labels.size() != 1) return false;
        var label = labels.getFirst(); var cell = label.nextElementSibling();
        return "tr".equals(label.parent().tagName()) && cell != null && "td".equals(cell.tagName())
                && cell.hasClass("p-table__content") && cell.nextElementSibling() == null
                && table.select("td.p-table__content").stream().filter(e -> e.closest("table") == table).count() == 1;
    }
    private boolean selectSafeName(String name) {
        return !name.isBlank() && name.length() <= 500 && !name.contains("/") && !name.contains("\\")
                && !name.contains("..") && name.indexOf('\ufffd') < 0 && name.codePoints().noneMatch(Character::isISOControl);
    }
    private String selectFormat(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0) return null;
        String suffix = name.substring(dot + 1).toUpperCase(Locale.ROOT);
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
