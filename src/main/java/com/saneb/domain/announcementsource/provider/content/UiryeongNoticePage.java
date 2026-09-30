package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 의령의 실제 공고번호 gosiNo와 공식 본문 영역을 보존한다. */
public final class UiryeongNoticePage {
    public static final String HOST="www.uiryeong.go.kr",PATH="/board/view.uiryeong";
    private UiryeongNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static Map<String,String> selectParameters(String raw){
        if(raw==null||raw.length()>8192)throw invalid();var q=new LinkedHashMap<String,String>();
        try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",2);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))throw invalid();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>4096||v.indexOf('\ufffd')>=0||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)throw invalid();}}catch(IllegalArgumentException e){throw invalid();}return q;
    }
    public static URI selectDetailUri(URI uri){
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!uri.equals(uri.normalize())||!uri.getRawPath().equals(uri.getPath()))throw invalid();var q=selectParameters(uri.getRawQuery());
        if(!Set.of("boardId","menuCd","startPage","dataSid","gosiNo").containsAll(q.keySet())||!"BBS_0000070".equals(q.get("boardId"))||!"DOM_000000203003001001".equals(q.get("menuCd"))||!q.getOrDefault("dataSid","").matches("[1-9][0-9]{0,14}")||!q.getOrDefault("gosiNo","").matches("[1-9][0-9]{0,14}")||!q.getOrDefault("startPage","1").matches("[1-9][0-9]{0,5}"))throw invalid();
        return URI.create("https://"+HOST+PATH+"?boardId=BBS_0000070&menuCd=DOM_000000203003001001&startPage=1&dataSid="+q.get("dataSid")+"&gosiNo="+q.get("gosiNo"));
    }
    public static Element selectRoot(Document page){var roots=page.select("div.boardViewWrap");if(roots.size()!=1)throw invalid();var root=roots.getFirst();selectTitle(root);return root;}
    public static Element selectTitle(Element root){var titles=root.select(":root > div.bdvTitWrap > p.bdvTit");if(titles.size()!=1||titles.getFirst().text().isBlank())throw invalid();return titles.getFirst();}
    public static Element selectContent(Document page){var bodies=selectRoot(page).select(":root > div.bdvCntWrap");if(bodies.size()!=1)throw invalid();return bodies.getFirst();}
    public static Element selectAttachments(Document page){var boxes=selectRoot(page).select(":root > div.bdvInfo > dl.fileBox");if(boxes.size()!=1)throw invalid();var box=boxes.getFirst();var labels=box.select(":root > dt");var cells=box.select(":root > dd");if(labels.size()!=1||!"첨부".equals(labels.getFirst().text().strip())||cells.size()!=1)throw invalid();return cells.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("UIRYEONG_STRUCTURE_CHANGED");}
}
