package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 대구광역시 고시공고의 명시된 제목·본문·첨부만 선택한다. */
public final class DaeguCityNoticePage {
    public static final String HOST = "www.daegu.go.kr";
    private static final String VIEW = "/front/daeguSidoGosi/daeguSidoGosiView.do";
    private DaeguCityNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && "/index.do".equals(uri.getPath())
                && uri.getRawQuery() != null && uri.getRawQuery().contains("daeguSidoGosiView.do");
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !"/index.do".equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("menu_id", "menu_link", "sno", "gosi_gbn").equals(query.keySet())
                || !"00940170".equals(query.get("menu_id")) || !VIEW.equals(query.get("menu_link"))
                || !query.getOrDefault("sno", "").matches("[1-9][0-9]{0,14}")
                || !Set.of("A", "N", "P", "E").contains(query.get("gosi_gbn"))) throw selectInvalid();
        return uri;
    }
    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "form#sidoGosiAPIVO > div#bbsView");
        if (selectTitle(root).text().isBlank()) throw selectInvalid();
        return root;
    }
    public static Element selectTitle(Element root) {
        return selectLabelCell(root, "title", "제목");
    }
    public static Element selectContent(Document page, URI uri) {
        selectDetailUri(uri); selectIdentity(page, uri);
        return selectLabelCell(selectRoot(page), "content", "내용");
    }
    public static Element selectAttachments(Document page, URI uri) {
        selectDetailUri(uri); selectIdentity(page, uri);
        return selectLabelCell(selectRoot(page), "attfile", "첨부파일");
    }
    private static void selectIdentity(Document page, URI uri) {
        var form = selectSingle(page, "form#sidoGosiAPIVO");
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        for (String name : Set.of("sno", "gosi_gbn")) {
            var input = selectSingle(form, ":root > input[type=hidden][name=" + name + "]");
            if (!query.get(name).equals(input.attr("value"))) throw selectInvalid();
        }
    }
    private static Element selectLabelCell(Element root, String style, String label) {
        var row = selectSingle(root, ":root > div.form_group > dl." + style);
        if (!label.equals(selectSingle(row, ":root > dt").text().strip())) throw selectInvalid();
        return selectSingle(row, ":root > dd");
    }
    private static Element selectSingle(Element root, String selector) {
        var values = root.select(selector);
        if (values.size() != 1) throw selectInvalid();
        return values.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("DAEGU_CITY_STRUCTURE_CHANGED"); }
}
