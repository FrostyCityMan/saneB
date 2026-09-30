package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 남동 공식 상세의 제목·본문·첨부 경계를 분리한다. */
public final class NamdongNoticePage {
    private NamdongNoticePage() { }
    public static final String DETAIL="/main/eminwon/eminwonAnnounceDetail.do";
    public static boolean selectMatches(URI u){return u!=null&&"www.namdong.go.kr".equals(u.getHost())&&DETAIL.equals(u.getPath());}
    public static URI selectDetailUri(URI u){
        if(!selectMatches(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!DETAIL.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        if(!Set.of("mgt_no","keyfield","keyword","symd","eymd","pgno","listsz").containsAll(q.keySet())||!q.getOrDefault("mgt_no","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://www.namdong.go.kr"+DETAIL+"?mgt_no="+q.get("mgt_no"));
    }
    public static Element selectRoot(Document p){var root=selectSingle(p,"div.board_view");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){return selectSingle(root,":root > div.title > p");}
    public static Element selectContent(Document p){return selectSingle(selectRoot(p),":root > div.con > div.detail");}
    public static Element selectAttachments(Document p){var dl=selectSingle(selectRoot(p),":root > div.add_file > dl");if(!"첨부파일".equals(selectSingle(dl,":root > dt").text().strip()))throw invalid();return selectSingle(dl,":root > dd > ul");}
    private static Element selectSingle(Element e,String selector){var found=e.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("NAMDONG_STRUCTURE_CHANGED");}
}
