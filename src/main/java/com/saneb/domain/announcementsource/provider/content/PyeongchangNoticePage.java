package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 평창 공식 고시공고의 제목·본문·첨부만 분리한다. */
public final class PyeongchangNoticePage {
    public static final String HOST = "www.pc.go.kr";
    public static final String PATH = "/portal/government/government-notification";
    private PyeongchangNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("noticeMgrNo", "pageIndex", "searchCondition", "searchKeyword", "gubun", "mode").containsAll(query.keySet())
                || !query.getOrDefault("noticeMgrNo", "").matches("[1-9][0-9]{0,14}")
                || !query.getOrDefault("mode", "").isEmpty()) throw selectInvalid();
        return URI.create("https://" + HOST + PATH + "?noticeMgrNo=" + query.get("noticeMgrNo"));
    }
    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "div#contentsArea > div.skinTb.skinTb-data-resList.skinTb-data-bgSbj");
        if (selectTitle(root).text().isBlank()) throw selectInvalid();
        return root;
    }
    public static Element selectTitle(Element root) { return selectCell(root, "제목"); }
    public static Element selectContent(Document page) {
        var body = selectCell(selectRoot(page), "내용");
        if (!body.hasClass("skinTb-conts")) throw selectInvalid();
        return body;
    }
    public static Element selectAttachments(Document page) { return selectCell(selectRoot(page), "첨부파일"); }
    private static Element selectCell(Element root, String label) {
        var labels = root.select(":root > div.skinTb-tr > div.skinTb-th").stream()
                .filter(e -> label.equals(e.text().strip())).toList();
        if (labels.size() != 1) throw selectInvalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !cell.hasClass("skinTb-td") || cell.nextElementSibling() != null) throw selectInvalid();
        return cell;
    }
    private static Element selectSingle(Element root, String selector) {
        var rows = root.select(selector);
        if (rows.size() != 1) throw selectInvalid();
        return rows.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("PYEONGCHANG_STRUCTURE_CHANGED"); }
}
