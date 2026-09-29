package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 홍성·예산 공식 상세의 본문과 파일 영역을 분리한다. */
public final class ChungcheongFourthNoticePage {
    private ChungcheongFourthNoticePage() { }
    public enum Site {
        HONGSEONG("hongseong","/prog/saeolGosi/kor/sub03_0204/GOSI_ALL/","LGS-000160","SAFE_EGOV_DETAIL_CELL"),
        YESAN("yesan","/prog/saeolGosi/GOSI/kor/sub04_03_01/","LGS-000161","SAFE_EGOV_DATA_LIST_NOTICE");
        public final String host,fileHost,path,listPath,sourceCode,parser;
        Site(String domain,String base,String sc,String p){host="www."+domain+".go.kr";fileHost="eminwon."+domain+".go.kr";path=base+"view.do";listPath=base+"list.do";sourceCode=sc;parser=p;}
    }
    public static Site selectSite(URI uri){if(uri!=null)for(var s:Site.values())if(s.host.equals(uri.getHost())&&s.path.equals(uri.getPath()))return s;return null;}
    public static URI selectDetailUri(Site s,URI uri){
        if(s==null||uri==null||!"https".equals(uri.getScheme())||!s.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!s.path.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(!Set.of("notAncmtMgtNo","pageIndex","searchCondition","searchKeyword").containsAll(q.keySet())||!q.getOrDefault("notAncmtMgtNo","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+s.host+s.path+"?notAncmtMgtNo="+q.get("notAncmtMgtNo"));
    }
    public static Element selectRoot(Site s,Document page){var root=selectSingle(page,s==Site.HONGSEONG?"div.program--contents > div.ui.bbs--view":"div.card.program--view > div.card-body.prog.bucket-form");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return s==Site.HONGSEONG?selectSingle(root,":root > div.ui.bbs--view--header > h2.ui.bbs--view--tit"):selectField(root,"notAncmtSj","제목");}
    public static Element selectContent(Site s,Document page){var root=selectRoot(s,page);return s==Site.HONGSEONG?selectSingle(root,":root > div.ui.bbs--view--cont > div.ui.bbs--detail--cont > div.ui.bbs--view--content"):selectField(root,"notAncmtCn","내용");}
    public static Element selectAttachments(Site s,Document page){
        var root=selectRoot(s,page);if(s==Site.HONGSEONG)return selectSingle(root,":root > div.ui.bbs--view--file");
        var groups=root.select(":root > div.form-group").stream().filter(e->e.select(":root > div.control-label > label").size()==1&&"파일".equals(e.select(":root > div.control-label > label").text())).toList();
        if(groups.size()!=1)throw invalid();return selectSingle(groups.getFirst(),":root > div.col-sm-9 > div.ui.bbs--view--file");
    }
    private static Element selectField(Element root,String id,String label){
        var labels=root.select(":root > div.form-group > div.control-label > label[for="+id+"]");if(labels.size()!=1||!label.equals(labels.getFirst().text()))throw invalid();
        return selectSingle(labels.getFirst().parent().parent(),":root > div > span#"+id);
    }
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CHUNGCHEONG_FOURTH_STRUCTURE_CHANGED");}
}
