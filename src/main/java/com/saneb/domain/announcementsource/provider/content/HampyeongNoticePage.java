package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 함평의 공식 제목 라벨·본문 단독 셀·첨부 셀을 선택한다. */
public final class HampyeongNoticePage {
    public static final String HOST="www.hampyeong.go.kr",PATH="/pg/GosiDetail.do";
    private HampyeongNoticePage() { }
    public static boolean selectSite(URI u){return u!=null&&HOST.equals(u.getHost())&&PATH.equals(u.getPath());}
    public static URI selectDetailUri(URI u){
        if(!selectSite(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!PATH.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!q.getOrDefault("SEQ","").matches("[1-9][0-9]{0,14}")||!"www273".equals(q.get("pageId"))||q.containsKey("notAncmtSeCode")&&!"01,02,03,04".equals(q.get("notAncmtSeCode"))||!Set.of("SEQ","pageId","notAncmtSeCode","listGubun","search_Type","search_Text","pageIndex").containsAll(q.keySet()))throw invalid();
        return URI.create("https://"+HOST+PATH+"?SEQ="+q.get("SEQ")+"&pageId=www273&notAncmtSeCode=01,02,03,04");
    }
    public static Element selectRoot(Document page){var root=selectSingle(page,"#board_view > table.basic_table");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){return selectLabelCell(root,"제목");}
    public static Element selectContent(Document page){return selectSingle(selectRoot(page),":root > tbody > tr > td[colspan=4]");}
    public static Element selectAttachments(Document page){return selectLabelCell(selectRoot(page),"첨부파일");}
    private static Element selectLabelCell(Element root,String label){var rows=root.select(":root > tbody > tr").stream().filter(e->e.select(":root > th").size()==1&&label.equals(e.select(":root > th").text())).toList();if(rows.size()!=1)throw invalid();return selectSingle(rows.getFirst(),":root > td");}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("HAMPYEONG_STRUCTURE_CHANGED");}
}
