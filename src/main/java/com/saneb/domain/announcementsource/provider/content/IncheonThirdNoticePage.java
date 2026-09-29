package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 실측된 검단·영종 공식 게시판의 본문과 첨부 경계를 분리한다. */
public final class IncheonThirdNoticePage {
    private IncheonThirdNoticePage() { }
    public enum Site {
        GEOMDAN("www.geomdan.go.kr", "/main/bbs/bbsMsgDetail.do", "LGS-000063", "HEURISTIC_NOTICE", "msg_seq"),
        YEONGJONG("www.yeongjong.go.kr", "/main/pst/view.do", "LGS-000056", "SAFE_SAEOL_EMINWON", "pst_sn");
        public final String host,path,sourceCode,listCode,idKey;
        Site(String host,String path,String sourceCode,String listCode,String idKey){this.host=host;this.path=path;this.sourceCode=sourceCode;this.listCode=listCode;this.idKey=idKey;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){
        if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        var allowed=s==Site.GEOMDAN?Set.of("bcd","msg_seq","keyfield","keyword","symd","eymd","pgno","listsz"):Set.of("pst_id","pst_sn","search","page","srchKey","srchVal","pst_se");
        if(!allowed.containsAll(q.keySet())||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}")||!(s==Site.GEOMDAN?"notice".equals(q.get("bcd")):"mn_pub_ntc".equals(q.get("pst_id"))))throw invalid();
        return URI.create("https://"+s.host+s.path+"?"+(s==Site.GEOMDAN?"bcd=notice&":"pst_id=mn_pub_ntc&")+s.idKey+"="+q.get(s.idKey));
    }
    public static Element selectRoot(Site s,Document p){var root=selectSingle(p,s==Site.GEOMDAN?"div.board-view":"div.cm_board_detail1");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.GEOMDAN?":root > div.title":":root > div.board_header_wrap > div.board_header > div.board_title");}
    public static Element selectContent(Site s,Document p){return selectSingle(selectRoot(s,p),s==Site.GEOMDAN?":root > div.con-box > div.detail":":root > div.board_content > div.editor_content");}
    public static Element selectAttachments(Site s,Document p){var root=selectRoot(s,p);if(s==Site.YEONGJONG)return selectSingle(root,":root > ul.cm_file_list2");var dl=selectSingle(root,":root > ul.info-data > li.attach-file > dl");if(!"첨부파일".equals(selectSingle(dl,":root > dt").text().strip()))throw invalid();return selectSingle(dl,":root > dd");}
    private static Element selectSingle(Element e,String selector){var found=e.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INCHEON_THIRD_STRUCTURE_CHANGED");}
}
