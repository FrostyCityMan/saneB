package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 광양 공식 고시·공고 상세의 제목, 본문, 첨부 셀을 분리한다. */
public final class GwangyangNoticePage {
    public static final String HOST = "gwangyang.go.kr", PATH = "/saeol/gosi.es";
    private GwangyangNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("mid","act","type_code","seq","nPage").containsAll(q.keySet()) || !"view".equals(q.get("act"))
                || !q.getOrDefault("seq", "").matches("[1-9][0-9]{0,14}")
                || (q.containsKey("nPage") && !q.get("nPage").matches("(?:[1-9][0-9]{0,5})?"))) throw invalid();
        String category = q.getOrDefault("type_code", "");
        // 기존 목록 URL 정규화로 저장된 한 겹 추가 인코딩만 복원한다. 임의 재귀 디코딩은 하지 않는다.
        if ("02%2C04".equals(category)) category = "02,04";
        if (!("a10909010000".equals(q.get("mid")) && "01".equals(category))
                && !("a10909020000".equals(q.get("mid")) && "02,04".equals(category))) throw invalid();
        return URI.create("https://" + HOST + PATH + "?mid=" + q.get("mid") + "&act=view&type_code=" + category
                + "&seq=" + q.get("seq") + (q.containsKey("nPage") ? "&nPage=" + q.get("nPage") : ""));
    }
    public static Element selectRoot(Document page) {
        var tables = page.select("div.p-wrap.bbs.bbs_view > table.p-table.block");
        if (tables.size() != 1 || selectTitle(tables.getFirst()).text().isBlank()) throw invalid();
        return tables.getFirst();
    }
    public static Element selectTitle(Element table) { return selectCell(table, "th.bbs_tit"); }
    public static Element selectContent(Document page) { return selectCell(selectRoot(page), "td.view_content"); }
    public static Element selectAttachments(Document page) { return selectCell(selectRoot(page), "td.view_file"); }
    private static Element selectCell(Element table, String selector) {
        var cells = table.select(selector).stream().filter(e -> e.closest("table") == table).toList();
        if (cells.size() != 1) throw invalid(); return cells.getFirst();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("GWANGYANG_STRUCTURE_CHANGED"); }
}
