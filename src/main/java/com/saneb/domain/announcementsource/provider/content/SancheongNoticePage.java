package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 산청 공식 BBS의 제목·내용·파일 셀을 서로 분리한다. */
public final class SancheongNoticePage {
    public static final String HOST="www.sancheong.go.kr",PATH="/www/selectBbsNttView.do";
    private static final Set<String> FIELDS=Set.of("key","bbsNo","nttNo","searchCtgry","searchCnd","searchKrwd","pageIndex","pageUnit","integrDeptCode");
    private SancheongNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static boolean selectSafe(URI uri){return uri!=null&&"https".equals(uri.getScheme())&&HOST.equals(uri.getHost())&&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null&&uri.getFragment()==null&&uri.equals(uri.normalize())&&Objects.equals(uri.getRawPath(),uri.getPath());}
    public static Map<String,String> selectParameters(String raw){
        if(raw==null||raw.length()>4096)throw invalid();var values=new LinkedHashMap<String,String>();
        try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",2);if(p.length!=2||!p[0].matches("[A-Za-z]+"))throw invalid();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>1024||v.indexOf('\ufffd')>=0||v.codePoints().anyMatch(Character::isISOControl)||values.putIfAbsent(p[0],v)!=null)throw invalid();}}
        catch(IllegalArgumentException exception){throw invalid();}return values;
    }
    public static URI selectDetailUri(URI uri){
        if(!selectSafe(uri)||!selectMatches(uri))throw invalid();var q=selectParameters(uri.getRawQuery());
        if(!FIELDS.containsAll(q.keySet())||!"158".equals(q.get("key"))||!"118".equals(q.get("bbsNo"))||!q.getOrDefault("nttNo","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+HOST+PATH+"?key=158&bbsNo=118&nttNo="+q.get("nttNo"));
    }
    public static Element selectRoot(Document page){var roots=page.select("table.bbs_default_view");if(roots.size()!=1)throw invalid();var root=roots.getFirst();if(selectCell(root,"제목").text().isBlank())throw invalid();return root;}
    public static Element selectCell(Element table,String label){
        var labels=table.select("th").stream().filter(e->e.closest("table")==table&&label.equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var th=labels.getFirst();var cell=th.nextElementSibling();if(!"tr".equals(th.parent().tagName())||cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null)throw invalid();return cell;
    }
    public static Element selectContent(Document page){return selectCell(selectRoot(page),"내용");}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SANCHEONG_STRUCTURE_CHANGED");}
}
