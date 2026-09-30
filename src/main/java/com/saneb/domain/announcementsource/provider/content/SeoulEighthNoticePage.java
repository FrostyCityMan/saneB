package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 강남·도봉 공식 고시공고 상세. 제목, 본문, 첨부 영역을 분리한다. */
public final class SeoulEighthNoticePage {
    public enum Site {
        GANGNAM("www.gangnam.go.kr","/notice/view.do","LGS-000024","SAEOL_GOSI","not_ancmt_mgt_no"),
        DOBONG("www.dobong.go.kr","/WDB_DEV/gosigong_go/detail.asp","LGS-000011","SPRING_BBS","idx");
        public final String host,path,source,parser,id;
        Site(String host,String path,String source,String parser,String id){this.host=host;this.path=path;this.source=source;this.parser=parser;this.id=id;}
    }
    private SeoulEighthNoticePage() { }
    public static Site selectSite(URI uri){if(uri!=null)for(var site:Site.values())if(site.host.equals(uri.getHost())&&site.path.equals(uri.getPath()))return site;return null;}
    public static URI selectDetailUri(Site site,URI uri){
        if(site==null||selectSite(uri)!=site||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!uri.getRawPath().equals(uri.getPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        var allowed=site==Site.GANGNAM?Set.of("not_ancmt_mgt_no","mid","keyfield","keyword","pgno","lists","gubunfield","deptField","deptId")
                :Set.of("idx","strSearchType","strSearchKeyword","intPage","intPageSize","sDEP_CODE");
        if(!allowed.containsAll(q.keySet())||!q.getOrDefault(site.id,"").matches("[1-9][0-9]{0,14}")
                ||site==Site.GANGNAM&&!"ID05_040201".equals(q.get("mid")))throw invalid();
        return URI.create("https://"+site.host+site.path+"?"+site.id+"="+q.get(site.id)+(site==Site.GANGNAM?"&mid=ID05_040201":""));
    }
    public static Element selectRoot(Document page,Site site){var root=single(page,site==Site.GANGNAM?"div.board.view div.bbs-view":"div.bbsView");if(selectTitle(root,site).isBlank())throw invalid();return root;}
    public static String selectTitle(Element root,Site site){
        if(site==Site.DOBONG)return single(root,":root > table.boardView > tbody > tr > td.title").text();
        var title=single(root,":root > div.post-title").clone();boolean after=false;
        for(var node:new java.util.ArrayList<>(title.childNodes())){if(node instanceof Element element&&"br".equals(element.tagName()))after=true;if(after)node.remove();}
        return title.text();
    }
    public static Element selectContent(Document page,URI uri){var site=selectSite(uri);selectDetailUri(site,uri);return single(selectRoot(page,site),site==Site.GANGNAM?":root > div.post-content":":root > div.bbsCont");}
    public static Element selectAttachments(Document page,Site site){
        var root=selectRoot(page,site);
        if(site==Site.GANGNAM){var area=single(root,":root > div.bbs-view-file");single(area,":root > ul#fileListCollap.view-file-list");return area;}
        var labels=single(root,":root > table.boardView").select(":root > tbody > tr > th").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();
        if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();
        if(cell==null||!"td".equals(cell.tagName())||!cell.hasClass("file")||cell.nextElementSibling()!=null)throw invalid();return cell;
    }
    private static Element single(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEOUL_EIGHTH_STRUCTURE_CHANGED");}
}
