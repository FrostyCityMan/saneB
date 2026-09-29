package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 서울시·서울 중구·용산의 공식 제목/본문/첨부 영역만 선택한다. */
public final class SeoulSeventhNoticePage {
    private SeoulSeventhNoticePage() { }
    public enum Site {
        SEOUL("www.seoul.go.kr","seoulboard.seoul.go.kr","/news/news_notice.do","LGS-000001","SAFE_SEOUL_NOTICE","nttNo","/comm/getFile"),
        SEOUL_JUNGGU("www.junggu.seoul.kr","www.junggu.seoul.kr","/content.do","LGS-000003","JUNGGU_NOTICE_TABLE","cid","/cwsboard/board.do"),
        YONGSAN("health.yongsan.go.kr","health.yongsan.go.kr","/portal/bbs/B0000095/view.do","LGS-000004","SPRING_BBS","nttId","/portal/cmmn/file/fileDown.do");
        public final String host,fileHost,path,sourceCode,parser,idKey,download;
        Site(String host,String fileHost,String path,String sourceCode,String parser,String idKey,String download){this.host=host;this.fileHost=fileHost;this.path=path;this.sourceCode=sourceCode;this.parser=parser;this.idKey=idKey;this.download=download;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());Set<String> allowed;
        if(s==Site.SEOUL){allowed=Set.of("bbsId","bbsNo","nttNo","pageIndex","srchKey","srchText","srchBeginDt","srchEndDt","srchEtc1","srchEtc2","srchCtgryType");if(!"277".equals(q.get("bbsNo"))||(q.containsKey("bbsId")&&!"001".equals(q.get("bbsId"))))throw invalid();}
        else if(s==Site.SEOUL_JUNGGU){allowed=Set.of("cmsid","mode","cid","page","sf_dept","searchField","searchValue");if(!"14232".equals(q.get("cmsid"))||!"view".equals(q.get("mode")))throw invalid();}
        else {allowed=Set.of("menuNo","nttId","pageUnit","pageIndex","searchCnd","searchWrd","optn1","sdate","edate","deptId");if(!"200233".equals(q.get("menuNo")))throw invalid();}
        if(!allowed.containsAll(q.keySet())||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}"))throw invalid();String fixed=s==Site.SEOUL?"bbsNo=277&":s==Site.SEOUL_JUNGGU?"cmsid=14232&mode=view&":"menuNo=200233&";return URI.create("https://"+s.host+s.path+"?"+fixed+s.idKey+"="+q.get(s.idKey));}
    public static Element selectRoot(Site s,Document p){var root=selectSingle(p,s==Site.SEOUL?"div.sib-viw-type-basic":s==Site.SEOUL_JUNGGU?"div.board_view_02 > table":"div#content > div.bd-view");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.SEOUL?"h3":s==Site.SEOUL_JUNGGU?":root > tbody > tr > th.view_tit":":root > h2.subject");}
    public static Element selectContent(Site s,Document p){return selectSingle(selectRoot(s,p),s==Site.SEOUL?"div.sib-viw-type-basic-content > div#scrabArea":s==Site.SEOUL_JUNGGU?":root > tbody > tr > td.article_body":":root > div.dbdata");}
    public static Element selectAttachments(Site s,Document p){var root=selectRoot(s,p);if(s==Site.SEOUL)return selectSingle(root,"div.sib-viw-file-list");if(s==Site.YONGSAN)return selectSingle(root,":root > div.table-dl > dl.file-list > dd > div.file-list--set");var labels=root.select(":root > tbody > tr > th").stream().filter(e->"첨부".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!"td".equals(cell.tagName()))throw invalid();return cell;}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEOUL_SEVENTH_STRUCTURE_CHANGED");}
}
