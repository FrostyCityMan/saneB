package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 서해구 공식 고시 게시판. 검색 상태는 원문 식별에 보존하고 fetch URL에서는 제거한다. */
public final class SeohaeNoticePage {
    public static final String HOST="seohae.go.kr", PATH="/open_content/main/bbs/bbsMsgDetail.do";
    private SeohaeNoticePage() { }
    public static boolean selectMatches(URI u){return u!=null&&HOST.equals(u.getHost())&&PATH.equals(u.getPath());}
    public static URI selectDetailUri(URI u){
        if(!selectMatches(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!PATH.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        if(!Set.of("bcd","msg_seq","keyfield","keyword","symd","eymd","pgno","listsz").containsAll(q.keySet())||!"gosi".equals(q.get("bcd"))||!q.getOrDefault("msg_seq","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+HOST+PATH+"?bcd=gosi&msg_seq="+q.get("msg_seq"));
    }
    public static Element selectRoot(Document p){var root=single(p,"div.board_view");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){return single(root,":root > h4.title");}
    public static Element selectContent(Document p,URI u){selectDetailUri(u);return single(selectRoot(p),":root > div.con");}
    public static Element selectAttachments(Document p){
        var labels=selectRoot(p).select(":root > ul.datalist > li > dl > dt").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();
        var dd=labels.getFirst().nextElementSibling();if(dd==null||!"dd".equals(dd.tagName())||dd.nextElementSibling()!=null)throw invalid();return dd;
    }
    private static Element single(Element e,String s){var found=e.select(s);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEOHAE_STRUCTURE_CHANGED");}
}
