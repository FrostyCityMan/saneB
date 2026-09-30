package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 장흥 공식 상세의 제목·본문·첨부 영역을 메뉴 및 만족도 폼과 분리한다. */
public final class JangheungNoticePage {
    public static final String HOST="www.jangheung.go.kr", PATH="/www/organization/news/notification";
    private JangheungNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static Element selectRoot(Document page){var roots=page.select("div#content");if(roots.size()!=1)throw invalid();var root=roots.getFirst();selectTitle(root);return root;}
    public static Element selectTitle(Element root){var titles=root.select(":root > div.view_title > p.title");if(titles.size()!=1||titles.getFirst().text().isBlank())throw invalid();return titles.getFirst();}
    public static Element selectContent(Document page){var bodies=selectRoot(page).select(":root > div.view_box:not(.file_area)");if(bodies.size()!=1)throw invalid();return bodies.getFirst();}
    public static Element selectFiles(Element root){var areas=root.select(":root > div.view_box.file_area");if(areas.size()!=1)throw invalid();var area=areas.getFirst();var labels=area.select(":root > div.file_tit > span.tit");if(labels.size()!=1||!"첨부파일".equals(labels.text()))throw invalid();return area;}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("JANGHEUNG_STRUCTURE_CHANGED");}
}
