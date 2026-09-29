package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 상주시 고시공고의 고정 메뉴와 표의 업무 영역만 선택한다. */
public final class SangjuNoticePage {
    public static final String HOST="www.sangju.go.kr",PATH="/gosi/detail.tc";
    private SangjuNoticePage(){ }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static URI selectDetailUri(URI uri){
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!PATH.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw selectInvalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(!Set.of("mn","mgtNo","pageIndex","notAncmtSeCode","searchKeyword","recordCountPerPage").containsAll(q.keySet())
                ||!"10297".equals(q.get("mn"))||!q.getOrDefault("mgtNo","").matches("[1-9][0-9]{0,14}")
                ||!"01,02,03,04,05,07".equals(q.getOrDefault("notAncmtSeCode","01,02,03,04,05,07")))throw selectInvalid();
        return URI.create("https://"+HOST+PATH+"?mn=10297&mgtNo="+q.get("mgtNo"));
    }
    public static Element selectTable(Document page){
        var tables=page.select("form#form1[name=form1][method=post] table.comp-tbl_datatype");
        if(tables.size()!=1)throw selectInvalid();var table=tables.getFirst();
        if(selectTitle(table).text().isBlank())throw selectInvalid();return table;
    }
    public static Element selectTitle(Element table){return selectCell(table,"제목");}
    public static Element selectContent(Document page){return selectCell(selectTable(page),"고시공고 내용");}
    public static Element selectAttachments(Document page){return selectCell(selectTable(page),"첨부파일");}
    private static Element selectCell(Element table,String name){
        var labels=table.select("th[scope=row]").stream().filter(e->e.closest("table")==table&&name.equals(e.text().strip())).toList();
        if(labels.size()!=1)throw selectInvalid();var label=labels.getFirst();var cell=label.nextElementSibling();
        if(!"tr".equals(label.parent().tagName())||cell==null||!"td".equals(cell.tagName())||!"3".equals(cell.attr("colspan"))||cell.nextElementSibling()!=null)throw selectInvalid();
        return cell;
    }
    private static IllegalArgumentException selectInvalid(){return new IllegalArgumentException("SANGJU_STRUCTURE_CHANGED");}
}
