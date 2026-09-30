package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 충남도 도 및 산하기관 고시공고의 공식 본문·첨부 영역. */
public final class ChungnamProvinceNoticePage {
    public static final String HOST="www.chungnam.go.kr",PATH="/cnportal/province/province/view.do";
    private ChungnamProvinceNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static URI selectDetailUri(URI uri){
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!PATH.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(!Set.of("nttId","menuNo","pageIndex","cl1CdValue","sdate","edate","searchCnd","searchWrd","pageUnit").containsAll(q.keySet())||!"500487".equals(q.get("menuNo"))||!q.getOrDefault("nttId","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+HOST+PATH+"?nttId="+q.get("nttId")+"&menuNo=500487");
    }
    public static Element selectRoot(Document page){var roots=page.select("div.board-view");if(roots.size()!=1)throw invalid();var root=roots.getFirst();selectTitle(root);return root;}
    public static Element selectTitle(Element root){var titles=root.select(":root > div.board-view-title_wr > p.board-view-title");if(titles.size()!=1||titles.getFirst().text().isBlank())throw invalid();return titles.getFirst();}
    public static Element selectContent(Document page){var nodes=selectRoot(page).select(":root > ul.board-view-ul > li > div.board-view-inner > div.content");if(nodes.size()!=1)throw invalid();return nodes.getFirst();}
    public static Element selectAttachments(Document page){
        var rows=selectRoot(page).select(":root > ul.board-view-ul > li > div.board-view-inner").stream().filter(e->e.select(":root > div.k").size()==1&&"첨부파일".equals(e.selectFirst(":root > div.k").text().strip())).toList();
        if(rows.size()!=1)throw invalid();var cells=rows.getFirst().select(":root > div.v");if(cells.size()!=1)throw invalid();return cells.getFirst();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CHUNGNAM_PROVINCE_STRUCTURE_CHANGED");}
}
