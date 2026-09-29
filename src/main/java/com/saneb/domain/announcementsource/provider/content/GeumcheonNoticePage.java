package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 금천 공식 고시공고의 제목·본문·첨부 영역. 목록 및 담당부서 정보는 본문에 넣지 않는다. */
public final class GeumcheonNoticePage {
    public static final String HOST = "www.geumcheon.go.kr";
    public static final String PATH = "/portal/tblSeolGosiDetailView.do";
    private GeumcheonNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("key", "notAncmtMgtNo").equals(query.keySet())
                || !"294".equals(query.get("key")) || !query.getOrDefault("notAncmtMgtNo", "").matches("[1-9][0-9]{0,14}")) throw selectInvalid();
        return URI.create("https://" + HOST + PATH + "?key=294&notAncmtMgtNo=" + query.get("notAncmtMgtNo"));
    }
    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "div#contents.cts294 > div.program > div.veterinary_contract.view > div.p-wrap.bbs.bbs__view > table.p-table.block");
        if (selectTitle(root).text().isBlank()) throw selectInvalid();
        return root;
    }
    public static Element selectTitle(Element root) {
        return selectSingle(root, ":root > tbody > tr > td[colspan=4][data-brl-flag=1]");
    }
    public static Element selectContent(Document page) {
        return selectSingle(selectRoot(page), ":root > tbody > tr > td[colspan=4][data-brl-flag=7]");
    }
    public static Element selectAttachments(Document page) {
        var labels = selectRoot(page).select(":root > tbody > tr > th").stream()
                .filter(e -> "첨부파일".equals(e.text().strip())).toList();
        if (labels.size() != 1) throw selectInvalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || cell.nextElementSibling() != null) throw selectInvalid();
        return cell;
    }
    private static Element selectSingle(Element root, String selector) {
        var values = root.select(selector);
        if (values.size() != 1) throw selectInvalid();
        return values.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("GEUMCHEON_STRUCTURE_CHANGED"); }
}
