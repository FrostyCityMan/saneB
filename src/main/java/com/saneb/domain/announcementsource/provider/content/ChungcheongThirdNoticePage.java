package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 충북·공주의 공식 상세에서 본문과 첨부 영역을 분리한다. */
public final class ChungcheongThirdNoticePage {
    private ChungcheongThirdNoticePage() { }
    public enum Site {
        CHUNGBUK("www.chungbuk.go.kr","sido.chungbuk.go.kr","/www/selectGosiPblancView.do","/citynet/jsp/cmm/attach/download.jsp","no","LGS-000135","SAEOL_GOSI"),
        GONGJU("www.gongju.go.kr","eminwon.gongju.go.kr","/prog/saeolGosi/GOSI_03/sub04_03_03/view.do","/emwp/jsp/ofr/FileDown.jsp","notAncmtMgtNo","LGS-000149","SAFE_EGOV_DETAIL_CELL");
        public final String host,fileHost,path,download,idKey,sourceCode,parser;
        Site(String h,String f,String p,String d,String id,String sc,String parser){host=h;fileHost=f;path=p;download=d;idKey=id;sourceCode=sc;this.parser=parser;}
    }
    public static Site selectSite(URI uri){if(uri!=null)for(var s:Site.values())if(s.host.equals(uri.getHost())&&s.path.equals(uri.getPath()))return s;return null;}
    public static URI selectDetailUri(Site s,URI uri){
        if(s==null||uri==null||!"https".equals(uri.getScheme())||!s.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!s.path.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        var allowed=s==Site.CHUNGBUK?Set.of("key","no","pageUnit","pageIndex","searchCnd","searchKrwd"):Set.of("notAncmtMgtNo","pageIndex","searchCondition","searchKeyword");
        if(!allowed.containsAll(q.keySet())||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}")||(s==Site.CHUNGBUK&&!"422".equals(q.get("key"))))throw invalid();
        return URI.create("https://"+s.host+s.path+"?"+(s==Site.CHUNGBUK?"key=422&":"")+s.idKey+"="+q.get(s.idKey));
    }
    public static Element selectRoot(Site s,Document page){
        var root=selectSingle(page,s==Site.CHUNGBUK?"div.p-wrap.bbs.bbs__view.uiux_type > div.bbs_viewbox":"div.program--contents > div.ui.bbs--view");
        if(selectTitle(s,root).text().isBlank())throw invalid();return root;
    }
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.CHUNGBUK?":root > div.subjectbox > span.subject":":root > div.ui.bbs--view--header > h2.ui.bbs--view--tit");}
    public static Element selectContent(Site s,Document page){return selectSingle(selectRoot(s,page),s==Site.CHUNGBUK?":root > div.viewcontentbox > div.viewcontent > div.contenttext":":root > div.ui.bbs--view--cont > div.ui.bbs--detail--cont > div.ui.bbs--view--content");}
    public static Element selectAttachments(Site s,Document page){
        var cell=selectSingle(selectRoot(s,page),s==Site.CHUNGBUK?":root > div.viewcontentbox > div.viewcontent > div.attachedfile":":root > div.ui.bbs--view--file");
        if(s==Site.CHUNGBUK&&!"첨부파일".equals(selectSingle(cell,":root > span.attach_tit").text()))throw invalid();return cell;
    }
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CHUNGCHEONG_THIRD_STRUCTURE_CHANGED");}
}
