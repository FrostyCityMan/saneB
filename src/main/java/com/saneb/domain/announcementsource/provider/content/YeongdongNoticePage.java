package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Element;

/** 영동 고시공고의 제목·본문·첨부 영역만 선택한다. */
public final class YeongdongNoticePage {
    public static final String HOST = "www.yd21.go.kr";
    public static final String PATH = "/kr/html/sub02/020103.html";
    private YeongdongNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static Element selectRoot(Element page) {
        var root = selectSingle(page, "div.program--contents > div.ui.bbs--view");
        if (selectTitle(root).text().isBlank()) throw new IllegalArgumentException("YEONGDONG_STRUCTURE_CHANGED");
        return root;
    }
    public static Element selectTitle(Element root) {
        return selectSingle(root, ":root > div.ui.bbs--view--header > h2.ui.bbs--view--tit");
    }
    public static Element selectContent(Element page) {
        return selectSingle(selectRoot(page), ":root > div.ui.bbs--view--cont > div.ui.bbs--detail--cont > div.ui.bbs--view--content");
    }
    public static Element selectFiles(Element page) {
        return selectSingle(selectRoot(page), ":root > div.ui.bbs--view--file");
    }
    private static Element selectSingle(Element parent, String selector) {
        var matches = parent.select(selector);
        if (matches.size() != 1) throw new IllegalArgumentException("YEONGDONG_STRUCTURE_CHANGED");
        return matches.getFirst();
    }
}
