package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 영광 고시공고의 제목·본문·첨부 행만 선택한다. 검색 조건은 파일 요청에 전달하지 않는다. */
public final class YeonggwangNoticePage {
    public static final String HOST = "www.yeonggwang.go.kr";
    private YeonggwangNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && Set.of("/bbs","/bbs/").contains(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !uri.getPath().equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("b_id","site","mn","type","bs_idx","sc_key","sc_word").containsAll(q.keySet())
                || !"gosigonggo".equals(q.get("b_id")) || !"headquarter_new".equals(q.get("site")) || !"9059".equals(q.get("mn"))
                || !"view".equals(q.get("type")) || !q.getOrDefault("bs_idx","").matches("[1-9][0-9]{0,14}")) throw invalid();
        if (q.containsKey("sc_key") != q.containsKey("sc_word") || (q.containsKey("sc_key")
                && (!"subject".equals(q.get("sc_key")) || q.get("sc_word").length() > 1024 || q.get("sc_word").codePoints().anyMatch(Character::isISOControl)))) throw invalid();
        return URI.create("https://" + HOST + "/bbs/?b_id=gosigonggo&site=headquarter_new&mn=9059&type=view&bs_idx=" + q.get("bs_idx"));
    }
    public static Element selectRoot(Document page) {
        var tables = page.select("div#board_view > table");
        if (tables.size() != 1 || selectTitle(tables.getFirst()).text().isBlank()) throw invalid(); return tables.getFirst();
    }
    public static Element selectTitle(Element table) { return selectCell(table,"제목"); }
    public static Element selectContent(Document page) {
        var table = selectRoot(page);
        var areas = table.select("tr > td.leftcell.rightcell > div.board_view_contents").stream().filter(e -> e.closest("table") == table).toList();
        if (areas.size() != 1) throw invalid(); return areas.getFirst();
    }
    public static Element selectAttachments(Document page) { return selectCell(selectRoot(page),"첨부"); }
    private static Element selectCell(Element table,String label) {
        var headers = table.select("tr > th").stream().filter(e -> e.closest("table") == table && label.equals(e.text().strip())).toList();
        if (headers.size() != 1) throw invalid(); var cell = headers.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || cell.nextElementSibling() != null) throw invalid(); return cell;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("YEONGGWANG_STRUCTURE_CHANGED"); }
}
