package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 연제·구례 공식 상세에서 제목·본문·첨부 경계를 분리한다. */
public final class YeonjeGuryeNoticePage {
    public enum Site { YEONJE, GURYE }
    private YeonjeGuryeNoticePage() { }
    public static boolean selectMatches(URI uri,Site site){return uri!=null&&(site==Site.YEONJE?"www.yeonje.go.kr":"www.gurye.go.kr").equals(uri.getHost())
            &&(site==Site.YEONJE?"/portal/saeol/gosi/view.do":"/board/GosiView.do").equals(uri.getPath());}
    public static Element selectRoot(Document page,Site site){
        var roots=page.select(site==Site.YEONJE?"form#detailForm > div.bod_wrap > div.bod_view":"div.boardGroup > div.board_view");
        if(roots.size()!=1)throw invalid();var root=roots.getFirst();if(selectTitle(root,site).text().isBlank())throw invalid();return root;
    }
    public static Element selectTitle(Element root,Site site){return selectOne(root,site==Site.YEONJE?"h4":"h3");}
    public static Element selectContent(Document page,Site site){return selectOne(selectRoot(page,site),site==Site.YEONJE?"div.view_cont":"div.board_con");}
    public static Element selectFiles(Element root,Site site){return selectOne(root,site==Site.YEONJE?"dl.view_file":"ul.file_down");}
    private static Element selectOne(Element root,String selector){var nodes=root.select(":root > "+selector);if(nodes.size()!=1)throw invalid();return nodes.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("YEONJE_GURYE_STRUCTURE_CHANGED");}
}
