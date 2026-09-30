package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 거제 고시공고의 고정 메뉴·공고번호와 공식 콘텐츠 영역을 검증한다. */
public final class GeojeNoticePage {
    public static final String HOST = "www.geoje.go.kr", PATH = "/index.geoje";
    private GeojeNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath()); }
    public static Map<String,String> selectParameters(String raw) {
        if (raw == null || raw.length() > 8192) throw invalid();
        var values = new LinkedHashMap<String,String>();
        try {
            for (String pair : raw.split("&", -1)) {
                String[] p = pair.split("=", 2);
                if (p.length != 2 || !p[0].matches("[A-Za-z_]+")) throw invalid();
                String value = URLDecoder.decode(p[1], StandardCharsets.UTF_8);
                if (value.length() > 4096 || value.indexOf('\ufffd') >= 0 || value.codePoints().anyMatch(Character::isISOControl)
                        || values.putIfAbsent(p[0], value) != null) throw invalid();
            }
        } catch (IllegalArgumentException e) { throw invalid(); }
        return values;
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !uri.equals(uri.normalize()) || !uri.getRawPath().equals(uri.getPath())) throw invalid();
        var q = selectParameters(uri.getRawQuery());
        if (!Set.of("menuCd","m","idx","startPage","searchType","keyword","DEP_NM","NOT_ANCMT_SJ","NOT_ANCMT_CN","listRow").containsAll(q.keySet())
                || !"DOM_000008902001002001".equals(q.get("menuCd")) || !"D".equals(q.get("m"))
                || !q.getOrDefault("idx", "").matches("[1-9][0-9]{0,14}")) throw invalid();
        return URI.create("https://" + HOST + PATH + "?menuCd=DOM_000008902001002001&m=D&idx=" + q.get("idx"));
    }
    public static Element selectRoot(Document page) {
        var roots = page.select("div.board-view > div.view01");
        if (roots.size() != 1) throw invalid();
        var root = roots.getFirst(); selectTitle(root); return root;
    }
    public static Element selectTitle(Element root) { return selectUnique(root, "div.title", true); }
    public static Element selectContent(Document page) { return selectUnique(selectRoot(page), "div.substan", false); }
    public static Element selectAttachments(Document page) { return selectUnique(selectRoot(page), "div.attach", false); }
    private static Element selectUnique(Element root, String selector, boolean requireText) {
        var nodes = root.select(":root > " + selector);
        if (nodes.size() != 1 || requireText && nodes.getFirst().text().isBlank()) throw invalid();
        return nodes.getFirst();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("GEOJE_STRUCTURE_CHANGED"); }
}
