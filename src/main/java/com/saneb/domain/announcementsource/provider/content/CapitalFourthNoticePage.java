package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 김포·동두천·평택 실측 경계. 만료 안내를 본문이나 첨부 없음으로 대체하지 않는다. */
public final class CapitalFourthNoticePage {
    private CapitalFourthNoticePage() { }
    public enum Site {
        GIMPO("www.gimpo.go.kr","eminwon.gimpo.go.kr","/portal/ntfcPblancView.do","key","1004","ntcn_no","LGS-000097","FileDown.jsp"),
        DONGDUCHEON("www.ddc.go.kr","eminwon.ddc.go.kr","/ddc/selectGosiData.do","key","340","not_ancmt_mgt_no","LGS-000112","FileDownNewPbs.jsp"),
        PYEONGTAEK("www.pyeongtaek.go.kr","eminwon.pyeongtaek.go.kr","/pyeongtaek/saeol/gosi/view.do","mid","0401020100","notAncmtMgtNo","LGS-000093","FileDown.jsp");
        public final String host,fileHost,path,menuKey,menu,idKey,sourceCode,download;
        Site(String h,String f,String p,String mk,String m,String id,String code,String down){host=h;fileHost=f;path=p;menuKey=mk;menu=m;idKey=id;sourceCode=code;download="/emwp/jsp/ofr/"+down;}
    }
    public static Site selectSite(URI uri){if(uri!=null)for(var s:Site.values())if(s.host.equals(uri.getHost())&&s.path.equals(uri.getPath()))return s;return null;}
    public static URI selectDetailUri(Site s,URI uri){
        if(s==null||uri==null||!"https".equals(uri.getScheme())||!s.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!s.path.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());if(!s.menu.equals(q.get(s.menuKey))||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}"))throw invalid();
        var allowed=new HashSet<>(Set.of(s.menuKey,s.idKey));String extra="";
        switch(s){case GIMPO->{allowed.addAll(Set.of("cate_cd","pageIndex","searchCnd","searchKrwd","search_term_se","search_bgnde","search_endde"));if(!"1".equals(q.get("cate_cd")))throw invalid();extra="&cate_cd=1";}
            case DONGDUCHEON->{allowed.addAll(Set.of("not_ancmt_se_code","pageIndex","searchCnd","searchKrwd"));if(!"04".equals(q.get("not_ancmt_se_code")))throw invalid();extra="&not_ancmt_se_code=04";}
            case PYEONGTAEK->allowed.addAll(Set.of("page","searchType","searchTxt","seCode","chargeDeptCode"));}
        if(!allowed.containsAll(q.keySet()))throw invalid();return URI.create("https://"+s.host+s.path+"?"+s.menuKey+"="+s.menu+"&"+s.idKey+"="+q.get(s.idKey)+extra);
    }
    public static Element selectRoot(Site s,Document page){var roots=page.select(s==Site.GIMPO?"div#contents > table.p-table.block":s==Site.DONGDUCHEON?"table.bbs_default.view":"form#detailForm[name=detailForm][method=post] > div.bod_wrap > div.bod_view");
        if(roots.size()!=1)throw invalid();var root=roots.getFirst();if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){if(s!=Site.PYEONGTAEK)return selectCell(root,s==Site.DONGDUCHEON?"제 목":"제목");return selectSingle(root,":root > h4");}
    public static Element selectContent(Site s,Document page){var root=selectRoot(s,page);if(s==Site.PYEONGTAEK)return selectSingle(root,":root > div.view_cont");if(s==Site.DONGDUCHEON)return selectCell(root,"내용");
        var row=selectCell(root,"제목").parent().nextElementSibling();if(row==null||!"tr".equals(row.tagName())||row.childrenSize()!=1||!"td".equals(row.child(0).tagName())||!"4".equals(row.child(0).attr("colspan")))throw invalid();
        var cell=row.child(0);if(cell.childrenSize()!=1||!"div".equals(cell.child(0).tagName()))throw invalid();return cell.child(0);}
    public static Element selectAttachments(Site s,Document page){var root=selectRoot(s,page);if(s!=Site.PYEONGTAEK)return selectCell(root,"첨부파일");
        var dl=selectSingle(root,":root > dl.view_file");if(dl.childrenSize()!=2||!"dt".equals(dl.child(0).tagName())||!"첨부 파일".equals(dl.child(0).text())||!"dd".equals(dl.child(1).tagName()))throw invalid();return dl.child(1);}
    private static Element selectSingle(Element root,String selector){var elements=root.select(selector);if(elements.size()!=1)throw invalid();return elements.getFirst();}
    private static Element selectCell(Element root,String label){var h=root.select("th").stream().filter(e->e.closest("table")==root&&label.equals(e.text().strip())).toList();if(h.size()!=1)throw invalid();var c=h.getFirst().nextElementSibling();if(c==null||!"td".equals(c.tagName())||c.nextElementSibling()!=null)throw invalid();return c;}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CAPITAL_FOURTH_STRUCTURE_CHANGED");}
}
