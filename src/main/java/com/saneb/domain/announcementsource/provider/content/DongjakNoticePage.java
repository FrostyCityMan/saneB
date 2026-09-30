package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 동작 공식 조회 폼의 고정 일곱 필드와 상세 본문·첨부 경계를 검증한다. */
public final class DongjakNoticePage {
    private DongjakNoticePage() { }
    public static final String HOST="dongjak.eminwon.seoul.kr", DETAIL="/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    private static final Map<String,String> FIXED=Map.of("context","NTIS","homepage_pbs_yn","Y","jndinm","OfrNotAncmtEJB","method","selectOfrNotAncmt","methodnm","selectOfrNotAncmtRegst","subCheck","Y");
    public static boolean selectMatches(URI u){return u!=null&&HOST.equals(u.getHost())&&DETAIL.equals(u.getPath());}
    public static URI selectDetailUri(URI u){
        if(!selectMatches(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!DETAIL.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        if(q.size()!=7||!q.entrySet().containsAll(FIXED.entrySet())||!q.getOrDefault("not_ancmt_mgt_no","").matches("[1-9][0-9]{0,14}"))throw invalid();return u;
    }
    public static AttachmentPinnedDownloadClient.Request selectRequest(URI u){selectDetailUri(u);return new AttachmentPinnedDownloadClient.Request(URI.create("https://"+HOST+DETAIL),"POST",CapitalThirdNoticePage.selectParameters(u.getRawQuery()));}
    public static Element selectRoot(Document p){var root=selectSingle(p,"form[name=form][method=post] > div.view");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){return selectSingle(root,":root > p.subject");}
    public static Element selectContent(Document p){return selectSingle(selectRoot(p),":root > div.dbData");}
    public static Element selectAttachments(Document p){var labels=selectRoot(p).select(":root > dl > dt").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!"dd".equals(cell.tagName()))throw invalid();return cell;}
    private static Element selectSingle(Element e,String selector){var found=e.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("DONGJAK_STRUCTURE_CHANGED");}
}
