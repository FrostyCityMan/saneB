package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 금산·부여 공식 상세의 제목, 본문, 파일 영역을 분리한다. */
public final class ChungcheongFifthNoticePage {
    private ChungcheongFifthNoticePage() { }
    public enum Site {
        GEUMSAN("geumsan","/site/kr/html/sub03/030302.html","/kr/html/sub03/030302.html","LGS-000156"),
        BUYEO("buyeo","/html/kr/news/news_040202.html","/html/kr/news/news_040202.html","LGS-000157");
        public final String host,fileHost,path,listPath,sourceCode,parser="SPRING_BBS";
        Site(String domain,String detail,String list,String sc){host="www."+domain+".go.kr";fileHost="eminwon."+domain+".go.kr";path=detail;listPath=list;sourceCode=sc;}
    }
    public static Site selectSite(URI uri){if(uri!=null)for(var s:Site.values())if(s.host.equals(uri.getHost())&&s.path.equals(uri.getPath()))return s;return null;}
    public static URI selectDetailUri(Site s,URI uri){
        if(s==null||uri==null||!"https".equals(uri.getScheme())||!s.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!s.path.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(!Set.of("mode","mng_no","site_dvs_cd","skey","sval","GotoPage").containsAll(q.keySet())||!"V".equals(q.get("mode"))||!q.getOrDefault("mng_no","").matches(s==Site.GEUMSAN?"[a-f0-9]{30,32}":"[1-9][0-9]{0,14}")||(s==Site.GEUMSAN?!"kr".equals(q.get("site_dvs_cd")):q.containsKey("site_dvs_cd")))throw invalid();
        return URI.create("https://"+s.host+s.path+"?mode=V"+(s==Site.GEUMSAN?"&site_dvs_cd=kr":"")+"&mng_no="+q.get("mng_no"));
    }
    public static Element selectRoot(Site s,Document page){var root=selectSingle(page,s==Site.GEUMSAN?"div.program--contents > div.ui.bbs--view":"section#con_body > div#txt");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.GEUMSAN?":root > div.ui.bbs--view--header > h2.ui.bbs--view--tit":":root > div.board_viewTit > h4");}
    public static Element selectContent(Site s,Document page){return selectSingle(selectRoot(s,page),s==Site.GEUMSAN?":root > div.ui.bbs--view--cont > div.ui.bbs--detail--cont > div.ui.bbs--view--content":":root > div.board_viewDetail");}
    public static Element selectAttachments(Site s,Document page){
        var root=selectRoot(s,page);if(s==Site.GEUMSAN)return selectSingle(root,":root > div.ui.bbs--view--file");
        var file=selectSingle(root,":root > ul.board_viewInfo > li.file");if(!"파일".equals(selectSingle(file,":root > span").text()))throw invalid();return selectSingle(file,":root > div");
    }
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CHUNGCHEONG_FIFTH_STRUCTURE_CHANGED");}
}
