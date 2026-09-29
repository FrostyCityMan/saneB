package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 포천·강릉 공식 상세의 제목, 본문, 첨부 영역을 분리한다. */
public final class CapitalSeventhNoticePage {
    private CapitalSeventhNoticePage() { }
    public enum Site {
        POCHEON("www.pocheon.go.kr","eminwon.pcs21.net","selectEminwonView.do","3712","notAncmtMgtNo","notAncmtSeCode","01","LGS-000108","FileDown.jsp"),
        GANGNEUNG("www.gn.go.kr","eminwon.gangneung.go.kr","selectGosiNttView.do","263","gosiNttNo","searchGosiSe","01,04,06","LGS-000119","FileDownNew.jsp");
        public final String host,fileHost,path,menu,idKey,typeKey,type,sourceCode,download;
        Site(String h,String fh,String p,String m,String id,String tk,String t,String sc,String down) {
            host=h;fileHost=fh;path="/www/"+p;menu=m;idKey=id;typeKey=tk;type=t;sourceCode=sc;download="/emwp/jsp/ofr/"+down;
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
        if(!Set.of("key",site.idKey,site.typeKey,"pageUnit","pageIndex","searchCnd","searchKrwd").containsAll(q.keySet())
                ||!site.menu.equals(q.get("key"))||!site.type.equals(q.get(site.typeKey))||!q.getOrDefault(site.idKey,"").matches("[1-9][0-9]{0,14}")) throw invalid();
        return URI.create("https://"+site.host+site.path+"?key="+site.menu+"&"+site.idKey+"="+q.get(site.idKey)+"&"+site.typeKey+"="+site.type);
    }
    public static Element selectRoot(Site site,Document page) {
        var root=selectSingle(page,site==Site.POCHEON?"div.p-wrap.bbs.bbs__view.uiux_type > div.bbs_viewbox":"table.bbs_default.view");
        if(selectTitle(site,root).text().isBlank()) throw invalid(); return root;
    }
    public static Element selectTitle(Site site,Element root) {
        return site==Site.POCHEON?selectSingle(root,":root > div.subjectbox > span.subject"):selectCell(root,"제목");
    }
    public static Element selectContent(Site site,Document page) {
        var root=selectRoot(site,page);
        return site==Site.POCHEON?selectSingle(root,":root > div.viewcontentbox > div.viewcontent > div.contenttext"):selectCell(root,"내용");
    }
    public static Element selectAttachments(Site site,Document page) {
        var root=selectRoot(site,page); if(site==Site.GANGNEUNG) return selectCell(root,"파일");
        var container=selectSingle(root,":root > div.viewcontentbox > div.viewcontent > div.attachedfile");
        if(!"첨부파일".equals(selectSingle(container,":root > span.attach_tit").text())) throw invalid();
        return container;
    }
    private static Element selectSingle(Element root,String selector) { var elements=root.select(selector); if(elements.size()!=1) throw invalid(); return elements.getFirst(); }
    private static Element selectCell(Element root,String label) {
        var headers=root.select("th").stream().filter(e->e.closest("table")==root&&label.equals(e.text().strip())).toList();
        if(headers.size()!=1) throw invalid(); var cell=headers.getFirst().nextElementSibling();
        if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null) throw invalid(); return cell;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("CAPITAL_SEVENTH_STRUCTURE_CHANGED"); }
}
