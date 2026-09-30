package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 서울 강서 고시공고의 제목·본문·첨부를 공식 영역으로 분리한다. */
public final class SeoulGangseoNoticePage {
    public static final String HOST = "www.gangseo.seoul.kr", PATH = "/gs040301/view";
    private SeoulGangseoNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("mgtNo","srchPage","curPage","srchKey","srchText").containsAll(q.keySet())
                || !q.getOrDefault("mgtNo","").matches("[1-9][0-9]{0,14}")) throw invalid();
        for (var key : Set.of("srchPage","curPage")) if (q.containsKey(key) && !q.get(key).matches("(?:[1-9][0-9]{0,5})?")) throw invalid();
        for (var key : Set.of("srchKey","srchText")) if (q.containsKey(key) && (q.get(key).length() > 1024 || q.get(key).codePoints().anyMatch(Character::isISOControl))) throw invalid();
        return URI.create("https://" + HOST + PATH + "?mgtNo=" + q.get("mgtNo"));
    }
    public static Element selectRoot(Document page) { var root = selectOne(page,"div.board-view-wrap"); if (selectTitle(root).text().isBlank()) throw invalid(); return root; }
    public static Element selectTitle(Element root) { return selectOne(root,":root > div.board-view-head > div.top-element > div.subject"); }
    public static Element selectContent(Document page) { return selectOne(selectRoot(page),":root > div.board-view-body > div.view-content > div.gosi-con"); }
    public static Element selectAttachments(Document page) {
        var dl = selectOne(selectRoot(page),":root > div.board-view-body > div.file-element > dl");
        if (!"첨부파일".equals(selectOne(dl,":root > dt").text().strip())) throw invalid();
        return selectOne(dl,":root > dd > ul");
    }
    private static Element selectOne(Element root,String selector) { var found = root.select(selector); if (found.size() != 1) throw invalid(); return found.getFirst(); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("SEOUL_GANGSEO_STRUCTURE_CHANGED"); }
}
