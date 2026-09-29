package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 공식 제목·본문·첨부 컨테이너를 분리하며 메뉴와 담당자 정보는 읽지 않는다. */
public final class SeoulSixthNoticePage {
    private SeoulSixthNoticePage() { }
    public enum Site {
        YANGCHEON("www.yangcheon.go.kr","eminwon.yangcheon.go.kr","/site/yangcheon/ex/seol/seolContentDeailView.do","LGS-000016","SAFE_YANGCHEON_SEOL"),
        GWANAK("www.gwanak.go.kr","eminwon.gwanak.go.kr","/site/gwanak/ex/bbsNew/View.do","LGS-000022","SAFE_GWANAK_NOTICE");
        public final String host,fileHost,path,sourceCode,parser;
        Site(String host,String fileHost,String path,String sourceCode,String parser){this.host=host;this.fileHost=fileHost;this.path=path;this.sourceCode=sourceCode;this.parser=parser;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        Set<String> allowed=s==Site.YANGCHEON?Set.of("not_ancmt_mgt_no","pageIndex","not_ancmt_se_nm","searchCondition","searchKeyword"):Set.of("not_ancmt_mgt_no","typeCode","pageIndex","tgtTypeCd","searchKey","pageUnit","searchStartDate","searchEndDate");
        if(!allowed.containsAll(q.keySet())||!q.getOrDefault("not_ancmt_mgt_no","").matches("[1-9][0-9]{0,14}")||(s==Site.GWANAK&&!"1".equals(q.get("typeCode"))))throw invalid();
        return URI.create("https://"+s.host+s.path+"?not_ancmt_mgt_no="+q.get("not_ancmt_mgt_no")+(s==Site.GWANAK?"&typeCode=1":""));}
    public static Element selectRoot(Site s,Document p){var root=selectSingle(p,s==Site.YANGCHEON?"form#SeolCollectVo > div.new-basic-view.basic-view":"div.board > div.board-view");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.YANGCHEON?":root > div.view-subj > div#bbsTitle":":root > div.tit > strong");}
    public static Element selectContent(Site s,Document p){return selectSingle(selectRoot(s,p),s==Site.YANGCHEON?":root > div.view-content > div.txt-area":":root > div.view_contents > div.txt-area");}
    public static Element selectAttachments(Site s,Document p){var root=selectSingle(selectRoot(s,p),":root > div.view-attachment");if(s==Site.GWANAK)return root;var labels=root.select(":root > dl > dt").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!"dd".equals(cell.tagName()))throw invalid();return cell;}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEOUL_SIXTH_STRUCTURE_CHANGED");}
}
