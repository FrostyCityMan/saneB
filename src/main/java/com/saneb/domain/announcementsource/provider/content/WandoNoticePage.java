package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 완도 고시공고에서 본문·첨부·메타데이터를 분리한다. */
public final class WandoNoticePage {
    public static final String HOST = "www.wando.go.kr";
    public static final String DETAIL = "/wando/sub.cs";
    private WandoNoticePage() { }
    public static boolean selectMatches(URI uri) {
        if (uri == null || !HOST.equals(uri.getHost()) || !DETAIL.equals(uri.getPath())) return false;
        try { return "1031".equals(ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery()).get("m")); }
        catch (IllegalArgumentException failure) { return false; }
    }
    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "div#board_basic_view");
        if (selectTitle(root).text().isBlank()) throw invalid();
        return root;
    }
    public static Element selectTitle(Element root) { return selectSingle(root, ":root > div.news_tit > h3"); }
    public static Element selectContent(Document page) { return selectSingle(selectRoot(page), ":root > div.board_cont"); }
    public static Element selectFiles(Document page) {
        var area = selectSingle(selectRoot(page), ":root > div.file_attach");
        var heading = selectSingle(area, ":root > h5");
        if (!heading.text().replaceAll("\\s", "").matches("첨부파일\\([0-9]{1,3}\\)")) throw invalid();
        return selectSingle(area, ":root > div.attach_thum > ul");
    }
    public static int selectFileCount(Document page) {
        return Integer.parseInt(selectSingle(selectRoot(page), ":root > div.file_attach > h5 > span > strong").text().strip());
    }
    private static Element selectSingle(Element parent, String selector) {
        var matches = parent.select(selector);
        if (matches.size() != 1) throw invalid();
        return matches.getFirst();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("WANDO_STRUCTURE_CHANGED"); }
}
