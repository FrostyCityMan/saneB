package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 보성 공식 고시공고의 본문과 첨부 영역을 분리한다. */
public final class BoseongNoticePage {
    public static final String HOST="www.boseong.go.kr",PATH="/www/open_administration/city_news/notification";
    private BoseongNoticePage() { }
    public static boolean selectSite(URI u){return u!=null&&HOST.equals(u.getHost())&&PATH.equals(u.getPath());}
    public static URI selectDetailUri(URI u){
        if(!selectSite(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!PATH.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!q.getOrDefault("idx","").matches("[1-9][0-9]{0,14}")||!"view".equals(q.get("mode"))||!Set.of("idx","mode","search_type","search_word","page","page_scale","start_date","finish_date").containsAll(q.keySet()))throw invalid();
        return URI.create("https://"+HOST+PATH+"?idx="+q.get("idx")+"&mode=view");
    }
    public static Element selectRoot(Document page){var root=selectSingle(page,"#content > #board_basic_view");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){return selectSingle(root,":root > .news_tit > h3");}
    public static Element selectContent(Document page){return selectSingle(selectRoot(page),":root > .board_cont");}
    public static Element selectAttachments(Document page){var cell=selectSingle(selectRoot(page),":root > .file_attach");selectDeclaredCount(cell);return cell;}
    public static int selectDeclaredCount(Element cell){String label=selectSingle(cell,":root > h5").text();if(!label.matches("첨부파일\\s*\\([0-9]{1,3}\\)"))throw invalid();return Integer.parseInt(label.replaceAll("[^0-9]",""));}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("BOSEONG_STRUCTURE_CHANGED");}
}
