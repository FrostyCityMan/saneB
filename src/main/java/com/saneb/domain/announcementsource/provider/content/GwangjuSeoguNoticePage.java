package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 광주 서구 XML 연계 상세의 제목·본문·첨부 영역. 메뉴와 담당자 정보는 본문에서 제외한다. */
public final class GwangjuSeoguNoticePage {
    public static final String HOST="www.seogu.gwangju.kr",DETAIL="/api/eminwon/gosiXmlView.es";
    private GwangjuSeoguNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&DETAIL.equals(uri.getPath());}
    public static Element selectRoot(Document page){
        var roots=page.select("form[name=form1][method=post] > article.board_view");
        if(roots.size()!=1)throw invalid();var root=roots.getFirst();
        if(selectTitle(root).text().isBlank())throw invalid();return root;
    }
    public static Element selectTitle(Element root){return selectOne(root,"h2.title");}
    public static Element selectContent(Document page){return selectOne(selectRoot(page),"div.contents");}
    public static Element selectFiles(Element root){
        var area=selectOne(root,"div.file");
        if(!"첨부파일".equals(selectOne(area,"strong.title").text().strip()))throw invalid();
        return selectOne(area,"ul.list");
    }
    private static Element selectOne(Element root,String selector){var matches=root.select(":root > "+selector);if(matches.size()!=1)throw invalid();return matches.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("GWANGJU_SEOGU_STRUCTURE_CHANGED");}
}
