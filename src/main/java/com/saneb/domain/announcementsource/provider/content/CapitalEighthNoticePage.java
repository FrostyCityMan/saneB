package com.saneb.domain.announcementsource.provider.content;
import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
/** 파주·광명 공식 고시공고의 제목/본문/첨부 경계. */
public final class CapitalEighthNoticePage {
    private CapitalEighthNoticePage() { }
    public enum Site {
        PAJU("www.paju.go.kr","www.paju.go.kr","/user/board/BD_board.view.do","LGS-000096","SAFE_PAJU_SUMMARY","seq"),
        GWANGMYEONG("www.gm.go.kr","eminwon.gm.go.kr","/pt/user/nftcBbs/BD_selectNftcBbsDetail.do","LGS-000102","SAFE_GWANGMYEONG_LEGAL_NOTICE","q_nftcBbsMgtno");
        public final String host,fileHost,path,sourceCode,parser,idKey;
        Site(String h,String f,String p,String s,String parser,String id){host=h;fileHost=f;path=p;sourceCode=s;this.parser=parser;idKey=id;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){
        if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        if(s==Site.PAJU){if(!"1022".equals(q.get("bbsCd"))||!q.getOrDefault(s.idKey,"").matches("[0-9]{17}")||!Set.of("bbsCd","seq","q_ctgCd","q_currPage","q_searchKeyType","q_searchVal").containsAll(q.keySet())||(q.containsKey("q_ctgCd")&&!"4063".equals(q.get("q_ctgCd"))))throw invalid();return URI.create("https://"+s.host+s.path+"?bbsCd=1022&seq="+q.get(s.idKey)+"&q_ctgCd=4063");}
        if(!"1001".equals(q.get("q_nftcBbsCode"))||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}")||!Set.of("q_nftcBbsCode",s.idKey,"q_currPage","q_searchKeyTy","q_searchVal").containsAll(q.keySet()))throw invalid();
        return URI.create("https://"+s.host+s.path+"?q_nftcBbsCode=1001&"+s.idKey+"="+q.get(s.idKey));
    }
    public static Element selectRoot(Site s,Document p){var r=single(p,s==Site.PAJU?"article#content div.content-body > div.container > div.article-view":"div.sub_content_cont_rt_cont > table.table_style2.bbsView");if(selectTitle(s,r).text().isBlank())throw invalid();return r;}
    public static Element selectTitle(Site s,Element r){if(s==Site.GWANGMYEONG)return cell(r,"제목");var title=single(r,":root > div.article-header > div.info-area > h1.article-subject").clone();var category=title.select(":root > span.category-label");if(category.size()!=1||!"고시공고".equals(category.getFirst().text().strip()))throw invalid();category.remove();if(!title.children().isEmpty())throw invalid();return title;}
    public static Element selectContent(Site s,Document p){var r=selectRoot(s,p);return s==Site.PAJU?single(r,":root > div.article-body > div.article-conetnt"):cell(r,"내용");}
    public static Element selectAttachments(Site s,Document p){var r=selectRoot(s,p);return s==Site.PAJU?single(r,":root > div.article-header > ul.file-list.file-shortcut"):cell(r,"첨부파일");}
    private static Element cell(Element r,String label){var labels=r.select(":root > tbody > tr > th").stream().filter(e->label.equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var c=labels.getFirst().nextElementSibling();if(c==null||!"td".equals(c.tagName())||c.nextElementSibling()!=null)throw invalid();return c;}
    private static Element single(Element p,String selector){var es=p.select(selector);if(es.size()!=1)throw invalid();return es.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CAPITAL_EIGHTH_STRUCTURE_CHANGED");}
}
