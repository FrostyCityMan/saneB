package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 사천 공고 상세의 제목·본문을 부서·첨부·메뉴와 분리한다. */
public final class SacheonNoticePage {
    public static final String HOST="www.sacheon.go.kr",PATH="/news/00009/00014.web";
    private SacheonNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static Element selectRoot(Document page){var roots=page.select("div.bbs1view1");if(roots.size()!=1)throw invalid();var root=roots.getFirst();selectTitle(root);return root;}
    public static Element selectTitle(Element root){var titles=root.select(":root > h1.h1#sns_bbs_title");if(titles.size()!=1||titles.getFirst().text().isBlank())throw invalid();return titles.getFirst();}
    public static Element selectContent(Document page){var bodies=selectRoot(page).select(":root > div.substance > div.substanceautolink");if(bodies.size()!=1)throw invalid();return bodies.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SACHEON_STRUCTURE_CHANGED");}
}
