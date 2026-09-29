package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 강원특별자치도 공식 고시공고의 제목·본문·첨부 영역을 분리한다. */
public final class GangwonProvinceNoticePage {
    public static final String HOST = "state.gwd.go.kr";
    public static final String PATH = "/portal/bulletin/notification";

    private GangwonProvinceNoticePage() { }

    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }

    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null
                || uri.getFragment() != null || !PATH.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) {
            throw selectInvalid();
        }
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("articleSeq", "pageIndex", "recordCountPerPage", "mode", "firstYN",
                "searchCondition", "searchKeyword", "searchFromDate", "searchToDate").containsAll(query.keySet())
                || !query.getOrDefault("articleSeq", "").matches("[1-9][0-9]{0,14}")
                || !query.getOrDefault("mode", "").isEmpty()) {
            throw selectInvalid();
        }
        return URI.create("https://" + HOST + PATH + "?articleSeq=" + query.get("articleSeq"));
    }

    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "div#content-bx > div.skinTb.skinTb-data-resList.skinTb-data-bgSbj");
        if (selectTitle(root).text().isBlank()) throw selectInvalid();
        return root;
    }

    public static Element selectTitle(Element root) { return selectCell(root, "제목"); }

    public static Element selectContent(Document page) {
        return selectSingle(selectRoot(page), ":root > div.skinTb-tr > div.skinTb-td.skinTb-conts");
    }

    public static Element selectAttachments(Document page) { return selectCell(selectRoot(page), "첨부파일"); }

    private static Element selectCell(Element root, String label) {
        var labels = root.select(":root > div.skinTb-tr > div.skinTb-th").stream()
                .filter(element -> label.equals(element.text().strip())).toList();
        if (labels.size() != 1) throw selectInvalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !cell.hasClass("skinTb-td") || cell.nextElementSibling() != null) throw selectInvalid();
        return cell;
    }

    private static Element selectSingle(Element root, String selector) {
        var matches = root.select(selector);
        if (matches.size() != 1) throw selectInvalid();
        return matches.getFirst();
    }

    private static IllegalArgumentException selectInvalid() {
        return new IllegalArgumentException("GANGWON_PROVINCE_STRUCTURE_CHANGED");
    }
}
