package com.saneb.domain.announcementsource.provider.content;
import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
/** 광주 남구·대전 중구에서 실측한 제목·본문·첨부 경계. */
public final class MetroNextNoticePage {
    private MetroNextNoticePage() { }
    public enum Site {
        GWANGJU_NAMGU("eminwon.namgu.gwangju.kr","eminwon.namgu.gwangju.kr","/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do","LGS-000068","SAFE_GWANGJU_NAMGU_NOTICE","not_ancmt_mgt_no"),
        DAEJEON_JUNGGU("www.djjunggu.go.kr","eminwon.djjunggu.go.kr","/prog/saeolGosi/GOSI/sub03_06/view.do","LGS-000073","SAFE_EGOV_DETAIL_CELL","notAncmtMgtNo");
        public final String host,fileHost,path,sourceCode,parser,idKey;
        Site(String h,String f,String p,String s,String parser,String id){host=h;fileHost=f;path=p;sourceCode=s;this.parser=parser;idKey=id;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}"))throw invalid();
        if(s==Site.GWANGJU_NAMGU){var fixed=Map.of("context","NTIS","homepage_pbs_yn","Y","jndinm","OfrNotAncmtEJB","method","selectOfrNotAncmt","methodnm","selectOfrNotAncmtRegst","subCheck","Y");if(q.size()!=7||!q.entrySet().containsAll(fixed.entrySet()))throw invalid();return u;}
        if(!Set.of("notAncmtMgtNo","pageIndex","searchCondition","searchKeyword").containsAll(q.keySet()))throw invalid();return URI.create("https://"+s.host+s.path+"?notAncmtMgtNo="+q.get(s.idKey));}
    public static Element selectRoot(Site s,Document p){var r=selectSingle(p,s==Site.GWANGJU_NAMGU?"form[name=form1][method=post] > div.tstyle_view":"div.program--contents > div.ui.bbs--view");if(selectTitle(s,r).text().isBlank())throw invalid();return r;}
    public static Element selectTitle(Site s,Element r){return selectSingle(r,s==Site.GWANGJU_NAMGU?":root > div.title":":root > div.ui.bbs--view--header > h2.ui.bbs--view--tit");}
    public static Element selectContent(Site s,Document p){return selectSingle(selectRoot(s,p),s==Site.GWANGJU_NAMGU?":root > div.tb_contents":":root > div.ui.bbs--view--cont > div.ui.bbs--detail--cont > div.ui.bbs--view--content");}
    public static Element selectAttachments(Site s,Document p){var r=selectRoot(s,p);if(s==Site.DAEJEON_JUNGGU)return selectSingle(r,":root > div.ui.bbs--view--file");var area=selectSingle(r,":root > div.add_file");if(area.childrenSize()!=2||!"첨부파일".equals(selectSingle(area,":root > strong").text())||!area.ownText().isBlank())throw invalid();return selectSingle(area,":root > div");}
    private static Element selectSingle(Element e,String selector){var result=e.select(selector);if(result.size()!=1)throw invalid();return result.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("METRO_NEXT_STRUCTURE_CHANGED");}
}
