package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 삼척 공식 고시공고의 제목과 본문 영역. 첨부와 담당자 정보는 본문에 포함하지 않는다. */
public final class SamcheokNoticePage {
    public static final String HOST = "www.samcheok.go.kr", PATH = "/media/00084/00095.web";
    private SamcheokNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("amode", "mgtNo", "cd", "sstring", "stype", "cpage").containsAll(query.keySet())
                || !query.getOrDefault("mgtNo", "").matches("[1-9][0-9]{0,14}") || !"view".equals(query.get("amode"))
                || !"01".equals(query.get("cd"))) throw selectInvalid();
        return URI.create("https://" + HOST + PATH + "?amode=view&mgtNo=" + query.get("mgtNo") + "&cd=01");
    }
    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "form#saeolGosiVO[name=saeolGosiVO][method=get] > div.bbs1view1");
        if (selectTitle(root).text().isBlank()) throw selectInvalid();
        return root;
    }
    public static Element selectTitle(Element root) { return selectSingle(root, ":root > h1.h1"); }
    public static Element selectContent(Document page) { return selectSingle(selectRoot(page), ":root > div.substance"); }
    private static Element selectSingle(Element root, String selector) {
        var elements = root.select(selector);
        if (elements.size() != 1) throw selectInvalid();
        return elements.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("SAMCHEOK_STRUCTURE_CHANGED"); }
}
