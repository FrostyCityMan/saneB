package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 군위의 고정 공고 메뉴에서 제목·본문·첨부 영역을 분리한다. */
public final class GunwiNoticePage {
    public static final String HOST = "www.gunwi.go.kr", PATH = "/ko/page.do";
    private GunwiNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("mnu_uid", "not_ancmt_mgt_no", "cmd", "board_code", "srchSDate", "srchKwd", "srchColumn", "srchDept", "srchEDate", "pageNo", "boardType").containsAll(q.keySet())
                || !"666".equals(q.get("mnu_uid")) || !"2".equals(q.get("cmd"))
                || !"notice".equals(q.getOrDefault("boardType", "notice"))
                || !q.getOrDefault("not_ancmt_mgt_no", "").matches("[1-9][0-9]{0,14}")) throw selectInvalid();
        return URI.create("https://" + HOST + PATH + "?mnu_uid=666&not_ancmt_mgt_no=" + q.get("not_ancmt_mgt_no") + "&cmd=2");
    }
    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "div.boardView");
        if (selectTitle(root).text().isBlank()) throw selectInvalid();
        return root;
    }
    public static Element selectTitle(Element root) { return selectSingle(root, ":root > div.title > h4"); }
    public static Element selectContent(Document page) { return selectSingle(selectRoot(page), ":root > div.cont > div.board_content"); }
    public static Element selectAttachments(Document page) { return selectSingle(selectRoot(page), ":root > div.title > div > ul"); }
    private static Element selectSingle(Element root, String selector) {
        var values = root.select(selector);
        if (values.size() != 1) throw selectInvalid();
        return values.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("GUNWI_STRUCTURE_CHANGED"); }
}
