package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 영암 공식 고시공고의 표 제목·본문·첨부 셀만 읽는다. 주변 메뉴는 근거가 아니다. */
public final class YeongamNoticePage {
    public static final String HOST="www.yeongam.go.kr";
    public static final String DETAIL="/home/www/open_information/yeongam_news/announcement/announcement_01/show/";
    private YeongamNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().matches(DETAIL+"[0-9]{1,15}");}
    public static Element selectRoot(Document page){var roots=page.select("table.show_form");if(roots.size()!=1)throw invalid();var root=roots.getFirst();if(selectCell(root,"제목").text().isBlank())throw invalid();return root;}
    public static Element selectCell(Element root,String label){
        var labels=root.select(":root > tbody > tr > th[scope=row]").stream().filter(e->label.equals(e.text().strip())).toList();
        if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();
        if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null)throw invalid();return cell;
    }
    public static Element selectContent(Document page){var cell=selectCell(selectRoot(page),"내용");if(!cell.hasClass("content"))throw invalid();return cell;}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("YEONGAM_STRUCTURE_CHANGED");}
}
