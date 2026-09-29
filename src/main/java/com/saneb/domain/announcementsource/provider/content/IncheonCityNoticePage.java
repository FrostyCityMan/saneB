package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 인천 시티넷 공식 상세. 기존 HTTP 식별자는 보존하고 실제 요청만 확인된 HTTPS로 만든다. */
public final class IncheonCityNoticePage {
    public static final String HOST = "announce.incheon.go.kr";
    public static final String PATH = "/citynet/jsp/sap/SAPGosiBizProcess.do";
    private IncheonCityNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !Set.of("http", "https").contains(uri.getScheme())
                || (uri.getPort() != -1 && uri.getPort() != ("https".equals(uri.getScheme()) ? 443 : 80))
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("command", "flag", "svp", "sido", "sno", "gosiGbn").equals(query.keySet())
                || !"searchDetail".equals(query.get("command")) || !"gosiGL".equals(query.get("flag"))
                || !"Y".equals(query.get("svp")) || !"ic".equals(query.get("sido"))
                || !query.getOrDefault("sno", "").matches("[1-9][0-9]{0,14}")
                || !Set.of("A", "N", "P", "E").contains(query.get("gosiGbn"))) throw selectInvalid();
        return URI.create("https://" + HOST + PATH + "?command=searchDetail&flag=gosiGL&svp=Y&sido=ic&sno="
                + query.get("sno") + "&gosiGbn=" + query.get("gosiGbn"));
    }
    public static Element selectTable(Document page) {
        var forms = page.select("form[name=myform]");
        if (forms.size() != 1) throw selectInvalid();
        var tables = forms.getFirst().select("table").stream().filter(table -> table.select("th.tb_tit_center").stream()
                .anyMatch(th -> th.closest("table") == table && "제목".equals(th.text().strip()))).toList();
        if (tables.size() != 1 || selectTitle(tables.getFirst()).text().isBlank()) throw selectInvalid();
        return tables.getFirst();
    }
    public static Element selectTitle(Element table) { return selectCell(table, "제목"); }
    public static Element selectContent(Document page, URI uri) {
        selectIdentity(page, uri); var table = selectTable(page);
        var labels = table.select("th.tb_tit_center[colspan=4]").stream().filter(th -> th.closest("table") == table
                && "내용".equals(th.text().replaceAll("\\s+", ""))).toList();
        var cells = table.select("td.tb_left[colspan=4][wrap=VIRTUAL]").stream().filter(td -> td.closest("table") == table).toList();
        if (labels.size() != 1 || cells.size() != 1) throw selectInvalid();
        var line = labels.getFirst().parent().nextElementSibling();
        if (line == null || line.childrenSize() != 1 || !line.child(0).hasClass("board_line")
                || line.nextElementSibling() != cells.getFirst().parent()) throw selectInvalid();
        return cells.getFirst();
    }
    public static Element selectAttachments(Document page, URI uri) {
        selectIdentity(page, uri); return selectCell(selectTable(page), "첨부파일");
    }
    private static void selectIdentity(Document page, URI uri) {
        selectDetailUri(uri); var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        var forms = page.select("form[name=myform]"); if (forms.size() != 1) throw selectInvalid();
        for (String name : Set.of("sno", "gosiGbn", "flag")) {
            var inputs = forms.getFirst().select(":root > input[type=hidden][name=" + name + "]");
            if (inputs.size() != 1 || !query.get(name).equals(inputs.getFirst().attr("value"))) throw selectInvalid();
        }
    }
    private static Element selectCell(Element table, String label) {
        var labels = table.select("th.tb_tit_center").stream().filter(th -> th.closest("table") == table && label.equals(th.text().strip())).toList();
        if (labels.size() != 1) throw selectInvalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || !cell.hasClass("tb_left") || cell.nextElementSibling() != null) throw selectInvalid();
        return cell;
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("INCHEON_CITY_STRUCTURE_CHANGED"); }
}
