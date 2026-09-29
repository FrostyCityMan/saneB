package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 시흥 새올 포털과 안산 공식 BBS의 검증된 상세 경계. */
public final class CapitalSixthNoticePage {
    private CapitalSixthNoticePage() { }
    public enum Site {
        SIHEUNG("www.siheung.go.kr","eminwon.siheung.go.kr","/main/saeol/gosi/view.do","mId","0401040100","notAncmtMgtNo","LGS-000095","SPRING_BBS","/emwp/jsp/ofr/FileDown.jsp"),
        ANSAN("www.ansan.go.kr","www.ansan.go.kr","/www/common/bbs/selectBbsDetail.do","bbs_code","WWW13","bbs_seq","LGS-000092","SAFE_ANSAN_BBS","/common/file/FileDown.do");
        public final String host,fileHost,path,menuKey,menu,idKey,sourceCode,parser,download;
        Site(String h,String fh,String p,String mk,String m,String id,String sc,String parser,String down) {
            host=h;fileHost=fh;path=p;menuKey=mk;menu=m;idKey=id;sourceCode=sc;this.parser=parser;download=down;
        }
    }
    public static Site selectSite(URI uri) {
        if(uri!=null) for(var site:Site.values()) if(site.host.equals(uri.getHost())&&site.path.equals(uri.getPath())) return site;
        return null;
    }
    public static URI selectDetailUri(Site site,URI uri) {
        if(site==null||uri==null||!"https".equals(uri.getScheme())||!site.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!site.path.equals(uri.getRawPath())||!uri.equals(uri.normalize())) throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        var allowed=site==Site.SIHEUNG?Set.of("mId","notAncmtMgtNo","page","searchType","searchTxt","seCode")
                :Set.of("bbs_code","bbs_seq","pageIndex","sch_type","sch_text");
        if(!allowed.containsAll(q.keySet())||!site.menu.equals(q.get(site.menuKey))||!q.getOrDefault(site.idKey,"").matches("[1-9][0-9]{0,14}")) throw invalid();
        return URI.create("https://"+site.host+site.path+"?"+site.menuKey+"="+site.menu+"&"+site.idKey+"="+q.get(site.idKey));
    }
    public static Element selectRoot(Site site,Document page) {
        var root=selectSingle(page,site==Site.SIHEUNG?"form#detailForm[name=detailForm][method=post] > div.bod_wrap > div.bod_view":"form#aform[method=get] div.p-wrap.bbs.bbs__view > table.p-table");
        if(selectTitle(site,root).text().isBlank()) throw invalid(); return root;
    }
    public static Element selectTitle(Site site,Element root) { return site==Site.SIHEUNG?selectSingle(root,":root > h4"):selectCell(root,"제목"); }
    public static Element selectContent(Site site,Document page) { var root=selectRoot(site,page); return site==Site.SIHEUNG?selectSingle(root,":root > div.view_cont"):selectCell(root,"내용"); }
    public static Element selectAttachments(Site site,Document page) {
        var root=selectRoot(site,page); if(site==Site.ANSAN) return selectCell(root,"파일");
        var dl=selectSingle(root,":root > dl.view_file");
        if(dl.childrenSize()!=2||!"dt".equals(dl.child(0).tagName())||!"첨부 파일".equals(dl.child(0).text())||!"dd".equals(dl.child(1).tagName())) throw invalid();
        return dl.child(1);
    }
    private static Element selectSingle(Element root,String selector) { var elements=root.select(selector); if(elements.size()!=1) throw invalid(); return elements.getFirst(); }
    private static Element selectCell(Element root,String label) {
        var headers=root.select("th").stream().filter(e->e.closest("table")==root&&label.equals(e.text().strip())).toList();
        if(headers.size()!=1) throw invalid(); var cell=headers.getFirst().nextElementSibling();
        if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null) throw invalid(); return cell;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("CAPITAL_SIXTH_STRUCTURE_CHANGED"); }
}
