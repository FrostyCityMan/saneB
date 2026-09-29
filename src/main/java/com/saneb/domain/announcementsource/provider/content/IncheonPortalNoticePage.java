package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 실측된 계양·강화의 공식 공고 제목, 본문, 첨부 영역을 선택한다. */
public final class IncheonPortalNoticePage {
    private IncheonPortalNoticePage() { }
    public static final String DETAIL="/open_content/main/eminwon/announce/eminwonDetail.do";
    public enum Site {
        GYEYANG("www.gyeyang.go.kr","eminwon.gyeyang.go.kr","LGS-000061"),
        GANGHWA("www.ganghwa.go.kr","eminwon.ganghwa.go.kr","LGS-000064");
        public final String host,fileHost,sourceCode;
        Site(String host,String fileHost,String sourceCode){this.host=host;this.fileHost=fileHost;this.sourceCode=sourceCode;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&DETAIL.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!DETAIL.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!Set.of("seq","keyfield","keyword","symd","eymd","pgno","listsz","announce_div").containsAll(q.keySet())||!q.getOrDefault("seq","").matches("[1-9][0-9]{0,14}"))throw invalid();return URI.create("https://"+s.host+DETAIL+"?seq="+q.get("seq"));}
    public static Element selectRoot(Site s,Document p){var root=selectSingle(p,"div.board_view");if(s==Site.GYEYANG&&!root.hasClass("general_board"))throw invalid();if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.GYEYANG?":root > div.tit > p.title":":root > p.title");}
    public static Element selectContent(Site s,Document p){return selectSingle(selectRoot(s,p),":root > div.con");}
    public static Element selectAttachments(Site s,Document p){var root=selectRoot(s,p);var dl=selectSingle(root,s==Site.GYEYANG?":root > div.tit > dl.file":":root > dl.file");if(!"첨부파일".equals(selectSingle(dl,":root > dt").text().strip()))throw invalid();return selectSingle(dl,":root > dd > ul");}
    private static Element selectSingle(Element e,String selector){var found=e.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INCHEON_PORTAL_STRUCTURE_CHANGED");}
}
