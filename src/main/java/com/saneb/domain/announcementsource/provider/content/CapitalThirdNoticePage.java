package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 남양주·하남·구리 공식 상세의 본문·첨부 경계. 메뉴와 파일명은 본문 근거가 아니다. */
public final class CapitalThirdNoticePage {
    private CapitalThirdNoticePage() { }
    public enum Site {
        NAMYANGJU("www.nyj.go.kr","eminwon.nyj.go.kr","selectEminwonWebView.do","2492","pk","LGS-000091","FileDownNew.jsp"),
        HANAM("www.hanam.go.kr","eminwon.hanam.gyeonggi.kr","selectGosiData.do","171","not_ancmt_mgt_no","LGS-000100","FileDown.jsp"),
        GURI("www.guri.go.kr","eminwon.guri.go.kr","selectGosiNttView.do","387","gosiNttNo","LGS-000107","FileDownNew.jsp");
        public final String host,fileHost,path,menu,idKey,sourceCode,download;
        Site(String host,String fileHost,String path,String menu,String idKey,String sourceCode,String download){
            this.host=host;this.fileHost=fileHost;this.path="/www/"+path;this.menu=menu;this.idKey=idKey;this.sourceCode=sourceCode;this.download="/emwp/jsp/ofr/"+download;
        }
    }
    public static Site selectSite(URI uri){if(uri!=null)for(var s:Site.values())if(s.host.equals(uri.getHost())&&s.path.equals(uri.getPath()))return s;return null;}
    public static Map<String,String> selectParameters(String raw){
        if(raw==null||raw.length()>8192)throw invalid();var result=new LinkedHashMap<String,String>();
        for(String pair:raw.split("&",-1)){var parts=pair.split("=",2);if(parts.length!=2||!parts[0].matches("[A-Za-z_][A-Za-z0-9_]*"))throw invalid();
            String value=URLDecoder.decode(parts[1],StandardCharsets.UTF_8);
            if(value.length()>600||value.indexOf('\ufffd')>=0||value.codePoints().anyMatch(Character::isISOControl)||result.putIfAbsent(parts[0],value)!=null)throw invalid();}
        return result;
    }
    public static URI selectDetailUri(Site s,URI uri){
        if(s==null||uri==null||!"https".equals(uri.getScheme())||!s.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!s.path.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=selectParameters(uri.getRawQuery());if(!s.menu.equals(q.get("key"))||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}"))throw invalid();
        var allowed=new HashSet<>(Set.of("key",s.idKey,"pageIndex"));
        switch(s){case NAMYANGJU->{allowed.addAll(Set.of("sa1Join","sc4","sc1","sc2","rcpp","cp"));if(!"01;02;04;05".equals(q.get("sa1Join"))||!"2024".equals(q.get("sc4")))throw invalid();}
            case HANAM->{allowed.addAll(Set.of("not_ancmt_se_code","searchCnd","searchKrwd"));if(!"01,04".equals(q.get("not_ancmt_se_code")))throw invalid();}
            case GURI->{allowed.addAll(Set.of("searchGosiSe","pageUnit","searchCnd","searchKrwd","searchDeptNm"));if(!"01,04,06".equals(q.get("searchGosiSe")))throw invalid();}}
        if(!allowed.containsAll(q.keySet()))throw invalid();
        String fixed=s==Site.NAMYANGJU?"&sa1Join=01;02;04;05&sc4=2024":s==Site.HANAM?"&not_ancmt_se_code=01,04":"&searchGosiSe=01,04,06";
        return URI.create("https://"+s.host+s.path+"?key="+s.menu+"&"+s.idKey+"="+q.get(s.idKey)+fixed);
    }
    public static Element selectTable(Site s,Document page){
        var tables=page.select("div.p-wrap.bbs.bbs__view > table.p-table.block");if(tables.size()!=1)throw invalid();
        var t=tables.getFirst();if(selectTitle(s,t).text().isBlank())throw invalid();return t;
    }
    public static Element selectTitle(Site s,Element table){
        if(s==Site.GURI)return selectCell(table,"제목");
        var titles=(s==Site.NAMYANGJU?table.parent().select("div.card.board_bottom > div.card_title > div.bbs_view_title"):table.select("span.p-table__subject_text"));
        if(titles.size()!=1||!titles.getFirst().select("table,script,input").isEmpty())throw invalid();return titles.getFirst();
    }
    public static Element selectContent(Site s,Document page){
        var table=selectTable(s,page);if(s!=Site.HANAM)return selectCell(table,"내용");
        var cells=table.select("td.p-table__content").stream().filter(e->e.closest("table")==table).toList();
        if(cells.size()!=1||cells.getFirst().select("textarea[title=내용][disabled]").size()!=1)throw invalid();
        return cells.getFirst().selectFirst("textarea[title=내용][disabled]");
    }
    public static Element selectAttachments(Site s,Document page){return selectCell(selectTable(s,page),s==Site.GURI?"파일":"첨부파일");}
    private static Element selectCell(Element table,String label){
        var headings=table.select("th").stream().filter(e->e.closest("table")==table&&label.equals(e.text().strip())).toList();
        if(headings.size()!=1)throw invalid();var cell=headings.getFirst().nextElementSibling();
        if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null)throw invalid();return cell;
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CAPITAL_THIRD_STRUCTURE_CHANGED");}
}
