package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 청주 공식 상세의 제목·내용·파일 행을 분리한다. */
public final class CheongjuNoticePage {
    public static final String HOST = "www.cheongju.go.kr", PATH = "/www/selectEminwonNoticeView.do";
    private CheongjuNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!q.keySet().equals(Set.of("key","nowDongGn","notAncmtSeCd","notAncmtMgtNo")) || !"281".equals(q.get("key"))
                || !q.get("nowDongGn").isEmpty() || !q.get("notAncmtSeCd").isEmpty() || !q.get("notAncmtMgtNo").matches("[1-9][0-9]{0,14}")) throw invalid();
        return URI.create("https://" + HOST + PATH + "?key=281&nowDongGn=&notAncmtSeCd=&notAncmtMgtNo=" + q.get("notAncmtMgtNo"));
    }
    public static Element selectRoot(Document page) {
        var tables = page.select("div#board.p-wrap.bbs.bbs__view > table.bbs_basic");
        if (tables.size() != 1 || selectTitle(tables.getFirst()).text().isBlank()) throw invalid();
        return tables.getFirst();
    }
    public static Element selectTitle(Element table) { return selectCell(table, "제목"); }
    public static Element selectContent(Document page) { return selectCell(selectRoot(page), "내용"); }
    public static Element selectAttachments(Document page) { return selectCell(selectRoot(page), "파일"); }
    private static Element selectCell(Element table, String label) {
        var headers = table.select("tr > th").stream().filter(e -> e.closest("table") == table && label.equals(e.text().strip())).toList();
        if (headers.size() != 1) throw invalid();
        var cell = headers.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || cell.nextElementSibling() != null) throw invalid(); return cell;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("CHEONGJU_STRUCTURE_CHANGED"); }
}
