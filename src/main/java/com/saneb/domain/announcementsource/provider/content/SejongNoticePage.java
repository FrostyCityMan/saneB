package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 세종 일반공고의 제목·본문·첨부 경계. 다른 공고 종류와 메뉴는 포함하지 않는다. */
public final class SejongNoticePage {
    public static final String HOST = "www.sejong.go.kr";
    public static final String DETAIL = "/prog/publicNotice/kor/sub02_030301/C1_1/view.do";
    private SejongNoticePage() { }
    public static boolean selectSite(URI uri) { return uri != null && HOST.equals(uri.getHost()) && DETAIL.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectSite(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !DETAIL.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var values = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("not_ancmt_mgt_no", "pageIndex", "searchCondition", "searchKeyword", "searchStartDate", "searchEndDate", "pageSize", "gubun").containsAll(values.keySet())
                || !values.getOrDefault("not_ancmt_mgt_no", "").matches("[1-9][0-9]{0,14}")) throw invalid();
        return URI.create("https://" + HOST + DETAIL + "?not_ancmt_mgt_no=" + values.get("not_ancmt_mgt_no"));
    }
    public static Element selectTable(Document document) {
        var table = selectSingle(document, "div#txt > div.table-responsive > table.table.table-bordered");
        if (selectTitle(table).text().isBlank()) throw invalid();
        return table;
    }
    public static Element selectTitle(Element table) { return selectCell(table, "제목"); }
    public static Element selectContent(Document document) { return selectSingle(selectTable(document), ":root > tbody > tr > td.tbl_cnts.cell_left"); }
    public static Element selectAttachments(Document document) { return selectCell(selectTable(document), "파일첨부"); }
    private static Element selectCell(Element table, String label) {
        var labels = table.select(":root > tbody > tr > th").stream().filter(e -> label.equals(e.text().strip())).toList();
        if (labels.size() != 1) throw invalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || cell.nextElementSibling() != null) throw invalid();
        return cell;
    }
    private static Element selectSingle(Element parent, String selector) {
        var elements = parent.select(selector);
        if (elements.size() != 1) throw invalid();
        return elements.getFirst();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("SEJONG_STRUCTURE_CHANGED"); }
}
