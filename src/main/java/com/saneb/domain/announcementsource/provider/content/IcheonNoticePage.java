package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 이천 공식 상세의 제목·본문·첨부 영역. 목록 범주를 다른 범주로 바꾸지 않는다. */
public final class IcheonNoticePage {
    public static final String HOST = "www.icheon.go.kr", PATH = "/portal/saeol/gosi/view.do";
    private IcheonNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!query.keySet().equals(Set.of("notAncmtMgtNo", "mid"))
                || !query.getOrDefault("notAncmtMgtNo", "").matches("[1-9][0-9]{0,14}")
                || !Set.of("0402010000", "0402020000").contains(query.get("mid"))) throw invalid();
        return URI.create("https://" + HOST + PATH + "?notAncmtMgtNo=" + query.get("notAncmtMgtNo") + "&mid=" + query.get("mid"));
    }
    public static Element selectRoot(Document page) {
        var roots = page.select("form#detailForm > div.bod_wrap > div.bod_view");
        if (roots.size() != 1 || selectTitle(roots.getFirst()).text().isBlank()) throw invalid();
        return roots.getFirst();
    }
    public static Element selectTitle(Element root) { return selectOne(root, ":root > div.subject"); }
    public static Element selectContent(Document page) { return selectOne(selectRoot(page), ":root > div.view_cont"); }
    public static Element selectAttachments(Document page) {
        var area = selectOne(selectRoot(page), ":root > dl.view_file");
        if (!"첨부 파일".equals(selectOne(area, ":root > dt").text().strip())) throw invalid();
        return selectOne(area, ":root > dd");
    }
    private static Element selectOne(Element root, String selector) {
        var elements = root.select(selector); if (elements.size() != 1) throw invalid(); return elements.getFirst();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("ICHEON_STRUCTURE_CHANGED"); }
}
