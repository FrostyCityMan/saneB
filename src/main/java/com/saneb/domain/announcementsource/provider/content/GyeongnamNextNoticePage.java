package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 김해·창녕의 공식 SCMS 고시 본문 경계. 첨부와 부서 정보는 본문이 아니다. */
public final class GyeongnamNextNoticePage {
    private GyeongnamNextNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&(("www.gimhae.go.kr".equals(uri.getHost())&&"/03360/00023/00029.web".equals(uri.getPath()))||("www.cng.go.kr".equals(uri.getHost())&&"/03517/01553.web".equals(uri.getPath())));}
    public static Element selectRoot(Document page,String host){String form=switch(host){case "www.gimhae.go.kr"->"saeolGosiVO";case "www.cng.go.kr"->"seolVO";default->throw invalid();};var roots=page.select("form#"+form+" > div.bbs1view1");if(roots.size()!=1)throw invalid();var root=roots.getFirst();selectTitle(root);return root;}
    public static Element selectTitle(Element root){var titles=root.select(":root > h1.h1");if(titles.size()!=1||titles.getFirst().text().isBlank())throw invalid();return titles.getFirst();}
    public static Element selectContent(Document page,URI uri){var bodies=selectRoot(page,uri.getHost()).select(":root > div.substance");if(bodies.size()!=1)throw invalid();return bodies.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("GYEONGNAM_SCMS_STRUCTURE_CHANGED");}
}
