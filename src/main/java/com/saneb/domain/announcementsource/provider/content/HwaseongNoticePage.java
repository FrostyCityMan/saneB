package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 화성 공식 고시·공고 상세의 제목·내용·첨부 셀을 분리한다. */
public final class HwaseongNoticePage {
    public static final String HOST = "www.hscity.go.kr";
    public static final String DETAIL = "/www/gosi/BD_selectNoticeDetail.do";
    private HwaseongNoticePage() { }

    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && DETAIL.equals(uri.getPath());
    }

    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !DETAIL.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw invalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("q_notAncmtMgtNo", "q_notAncmtSeCode").containsAll(query.keySet())
                || !query.getOrDefault("q_notAncmtMgtNo", "").matches("[1-9][0-9]{0,14}")
                || (query.containsKey("q_notAncmtSeCode") && !Set.of("01", "04").contains(query.get("q_notAncmtSeCode")))) throw invalid();
        // 기존 DB 상세 템플릿의 공고번호만으로 같은 내용을 제공하는 것을 실측했다.
        return URI.create("https://" + HOST + DETAIL + "?q_notAncmtMgtNo=" + query.get("q_notAncmtMgtNo"));
    }

    public static Element selectRoot(Document page) {
        var tables = page.select("div.board_write > table");
        if (tables.size() != 1) throw invalid();
        var table = tables.getFirst();
        if (selectTitle(table).text().isBlank()) throw invalid();
        return table;
    }

    public static Element selectTitle(Element table) { return selectCell(table, "제목"); }
    public static Element selectContent(Document page) { return selectCell(selectRoot(page), "내용"); }
    public static Element selectAttachments(Document page) { return selectCell(selectRoot(page), "첨부파일"); }

    private static Element selectCell(Element table, String label) {
        var headers = table.select("tr > th").stream().filter(th -> th.closest("table") == table
                && label.equals(th.text().strip())).toList();
        if (headers.size() != 1) throw invalid();
        var cell = headers.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || cell.nextElementSibling() != null) throw invalid();
        return cell;
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("HWASEONG_STRUCTURE_CHANGED"); }
}
