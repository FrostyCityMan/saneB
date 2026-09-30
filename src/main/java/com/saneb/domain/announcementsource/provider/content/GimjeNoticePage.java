package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Element;

/** 김제 고시공고의 제목·본문·공식 첨부 영역을 분리한다. */
public final class GimjeNoticePage {
    public static final String HOST = "www.gimje.go.kr";
    public static final String DETAIL = "/board/view.gimje";
    public static final String DOWNLOAD = "/board/download.gimje";
    private GimjeNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && DETAIL.equals(uri.getPath());
    }
    public static Element selectRoot(Element page) {
        var root = selectSingle(page, "div.bbs_skin > div.bbs_view");
        if (selectTitle(root).text().isBlank()) throw new IllegalArgumentException("GIMJE_STRUCTURE_CHANGED");
        return root;
    }
    public static Element selectTitle(Element root) { return selectSingle(root, ":root > div.bbs_vtop > h4"); }
    public static Element selectContent(Element page) { return selectSingle(selectRoot(page), ":root > div.bbs_con"); }
    public static Element selectFiles(Element page) {
        var area = selectSingle(selectRoot(page), ":root > div.bbs_filedown > dl");
        if (!"첨부파일".equals(selectSingle(area, ":root > dt").text().strip())) throw new IllegalArgumentException("GIMJE_STRUCTURE_CHANGED");
        return area;
    }
    private static Element selectSingle(Element parent, String selector) {
        var matches = parent.select(selector);
        if (matches.size() != 1) throw new IllegalArgumentException("GIMJE_STRUCTURE_CHANGED");
        return matches.getFirst();
    }
}
