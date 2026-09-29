package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 울산시 공식 고시공고 표의 제목·본문·첨부 영역을 구분한다. */
public final class UlsanCityNoticePage {
    public static final String HOST = "www.ulsan.go.kr", PREFIX = "/u/rep/transfer/notice/";
    private UlsanCityNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && uri.getPath() != null
                && uri.getPath().matches("/u/rep/transfer/notice/[1-9][0-9]{0,14}\\.ulsan");
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !uri.getPath().equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("mId", "gosiGbn").equals(q.keySet()) || !"001004002000000000".equals(q.get("mId"))
                || !Set.of("A", "N", "P", "E").contains(q.get("gosiGbn"))) throw selectInvalid();
        return uri;
    }
    public static String selectNoticeId(URI uri) {
        selectDetailUri(uri); return uri.getPath().substring(PREFIX.length(), uri.getPath().length() - ".ulsan".length());
    }
    public static Element selectRoot(Document page) {
        var roots = page.select("#contents_inner table.tbl_bd_view");
        if (roots.size() != 1 || selectTitle(roots.getFirst()).text().isBlank()) throw selectInvalid();
        return roots.getFirst();
    }
    public static Element selectTitle(Element root) { return selectCell(root, "제목"); }
    public static Element selectAttachments(Document page, URI uri) {
        selectDetailUri(uri); return selectCell(selectRoot(page), "첨부파일");
    }
    public static Element selectContent(Document page, URI uri) {
        selectDetailUri(uri); var root = selectRoot(page);
        var labels = root.select("th[scope=row][colspan=4]").stream().filter(e -> e.closest("table") == root && "내용".equals(e.text().strip())).toList();
        var bodies = root.select("td[colspan=4]").stream().filter(e -> e.closest("table") == root).toList();
        if (labels.size() != 1 || bodies.size() != 1 || labels.getFirst().parent().nextElementSibling() != bodies.getFirst().parent()) throw selectInvalid();
        return bodies.getFirst();
    }
    private static Element selectCell(Element root, String label) {
        var labels = root.select("th[scope=row]").stream().filter(e -> e.closest("table") == root && label.equals(e.text().strip())).toList();
        if (labels.size() != 1) throw selectInvalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || !"3".equals(cell.attr("colspan")) || cell.nextElementSibling() != null) throw selectInvalid();
        return cell;
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("ULSAN_CITY_STRUCTURE_CHANGED"); }
}
