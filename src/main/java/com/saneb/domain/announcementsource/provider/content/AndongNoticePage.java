package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 안동 공식 표형 새올의 제목·본문·첨부 경계. */
public final class AndongNoticePage {
    public static final String HOST="www.andong.go.kr",PATH="/portal/saeol/gosi/view.do";
    private AndongNoticePage(){ }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static URI selectDetailUri(URI uri){
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null
                ||uri.getFragment()!=null||!PATH.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw selectInvalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(!Set.of("notAncmtMgtNo","isLinkage","mId","page","searchType","searchTxt","seCode").containsAll(q.keySet())
                ||!"0401020100".equals(q.get("mId"))||!"Y".equals(q.get("isLinkage"))||!q.getOrDefault("notAncmtMgtNo","").matches("[1-9][0-9]{0,14}"))throw selectInvalid();
        return URI.create("https://"+HOST+PATH+"?notAncmtMgtNo="+q.get("notAncmtMgtNo")+"&isLinkage=Y&mId=0401020100");
    }
    public static Element selectTable(Document page){
        var forms=page.select("form#detailForm[name=detailForm][method=post]");if(forms.size()!=1)throw selectInvalid();
        var heading=forms.getFirst().nextElementSibling();var table=heading==null?null:heading.nextElementSibling();
        if(heading==null||!"h4".equals(heading.tagName())||!heading.hasClass("hidden")||table==null||!"table".equals(table.tagName())||!table.hasClass("bod_view")||selectTitle(table).text().isBlank())throw selectInvalid();
        return table;
    }
    public static Element selectTitle(Element table){return selectSingle(table,"th.title[scope=col][colspan=4]");}
    public static Element selectContent(Document page){var table=selectTable(page);var cell=selectSingle(table,"td.cont[colspan=4]");var contents=cell.select(":root > div.cont_box");if(contents.size()!=1)throw selectInvalid();return contents.getFirst();}
    public static Element selectAttachments(Document page){
        var table=selectTable(page);var label=selectSingle(table,"th.list_file[scope=row]");var cell=label.nextElementSibling();
        if(!"첨부파일".equals(label.text().strip())||cell==null||!"td".equals(cell.tagName())||!cell.hasClass("box_file")||!"3".equals(cell.attr("colspan"))||cell.nextElementSibling()!=null)throw selectInvalid();
        var lists=cell.select(":root > ul.list_file");if(lists.size()!=1)throw selectInvalid();return lists.getFirst();
    }
    private static Element selectSingle(Element table,String selector){var elements=table.select(selector).stream().filter(e->e.closest("table")==table).toList();if(elements.size()!=1)throw selectInvalid();return elements.getFirst();}
    private static IllegalArgumentException selectInvalid(){return new IllegalArgumentException("ANDONG_STRUCTURE_CHANGED");}
}
