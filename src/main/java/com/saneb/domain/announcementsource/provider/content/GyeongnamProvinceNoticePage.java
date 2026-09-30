package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 경남도 공식 고시공고의 본문·첨부 레이블을 분리한다. */
public final class GyeongnamProvinceNoticePage {
    public static final String HOST="www.gyeongnam.go.kr",PATH="/index.gyeong";
    private GyeongnamProvinceNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static URI selectDetailUri(URI uri){
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!PATH.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(!Set.of("menuCd","mode","sno","gosiGbn","conTitle","conGosiGbn","conIfmStdt","conIfmEnddt","pageLine","page","searchCnt","conAnnounceNo","conDeptNm").containsAll(q.keySet())
                ||!"DOM_000000135003009001".equals(q.get("menuCd"))||!"view".equals(q.get("mode"))||!"A".equals(q.get("gosiGbn"))||!q.getOrDefault("sno","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+HOST+PATH+"?menuCd=DOM_000000135003009001&mode=view&sno="+q.get("sno")+"&gosiGbn=A");
    }
    public static Element selectRoot(Document page){var roots=page.select("div.basicView.view-v2");if(roots.size()!=1)throw invalid();var root=roots.getFirst();selectTitle(root);return root;}
    public static Element selectTitle(Element root){var titles=root.select(":root > div.titleField > h4");if(titles.size()!=1||titles.getFirst().text().isBlank())throw invalid();return titles.getFirst();}
    public static Element selectContent(Document page){var nodes=selectRoot(page).select(":root > div.conText");if(nodes.size()!=1)throw invalid();return nodes.getFirst();}
    public static Element selectAttachments(Document page){
        var rows=selectRoot(page).select(":root > div.conField > ul > li").stream().filter(li->li.select(":root > span").size()==1&&"첨부파일".equals(li.selectFirst(":root > span").text().strip())).toList();
        if(rows.size()!=1)throw invalid();var cells=rows.getFirst().select(":root > p");if(cells.size()!=1)throw invalid();return cells.getFirst();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("GYEONGNAM_PROVINCE_STRUCTURE_CHANGED");}
}
