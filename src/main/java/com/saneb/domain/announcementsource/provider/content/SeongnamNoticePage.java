package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 성남 새올 상세의 제목·본문 경계를 메뉴·첨부·담당자와 분리한다. */
public final class SeongnamNoticePage {
    private SeongnamNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&"eminwon.seongnam.go.kr".equals(uri.getHost())&&"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do".equals(uri.getPath());}
    public static Element selectRoot(Document page){
        var tables=page.select("form[name=form1][method=post] div.boardWrap > table.bd00view");
        if(tables.size()!=1)throw invalid();var table=tables.getFirst();if(selectTitle(table).text().isBlank())throw invalid();return table;
    }
    public static Element selectTitle(Element table){
        var labels=table.select("th").stream().filter(e->e.closest("table")==table&&"제목".equals(e.text().strip())).toList();
        if(labels.size()!=1)throw invalid();var label=labels.getFirst();var cell=label.nextElementSibling();
        if(!"tr".equals(label.parent().tagName())||cell==null||!"td".equals(cell.tagName()))throw invalid();return cell;
    }
    public static Element selectContent(Document page){
        var table=selectRoot(page);var cells=table.select("td.bd01tdC[colspan=4]").stream().filter(e->e.closest("table")==table&&"tr".equals(e.parent().tagName())&&e.parent().childrenSize()==1).toList();
        if(cells.size()!=1)throw invalid();return cells.getFirst();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEONGNAM_NOTICE_STRUCTURE_CHANGED");}
}
