package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 아산·서산의 공식 본문과 첨부 셀만 선택한다. */
public final class ChungcheongSixthNoticePage {
    private ChungcheongSixthNoticePage() { }
    public enum Site {
        ASAN("www.asan.go.kr","eminwon.asan.go.kr","/main/cms/","mgt_no","LGS-000151","SAEOL_GOSI"),
        SEOSAN("www.seosan.go.kr","eminwon.seosan.go.kr","/common/program/eminwonView.jsp","not_ancmt_mgt_no","LGS-000152","SAFE_SAEOL_EMINWON");
        public final String host,fileHost,path,idKey,sourceCode,parser;
        Site(String h,String f,String p,String id,String sc,String parser){host=h;fileHost=f;path=p;idKey=id;sourceCode=sc;this.parser=parser;}
        public boolean selectDetailHost(String h){return host.equals(h)||this==ASAN&&"asan.go.kr".equals(h);}
    }
    public static Site selectSite(URI uri){if(uri!=null)for(var s:Site.values())if(s.selectDetailHost(uri.getHost())&&s.path.equals(uri.getPath()))return s;return null;}
    public static URI selectDetailUri(Site s,URI uri){
        if(s==null||uri==null||!"https".equals(uri.getScheme())||!s.selectDetailHost(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!s.path.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());if(!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}"))throw invalid();
        if(s==Site.ASAN){if(!Set.of("no","m_mode","mgt_no","PageNo","yearOption","sltOption","txtKeyword").containsAll(q.keySet())||!"257".equals(q.get("no"))||!"view".equals(q.get("m_mode")))throw invalid();return URI.create("https://"+uri.getHost()+s.path+"?no=257&m_mode=view&mgt_no="+q.get(s.idKey));}
        if(!Set.of("jndinm","context","method","methodnm","not_ancmt_mgt_no","homepage_pbs_yn","subCheck","pageIndex","ofr_pageSize","not_ancmt_se_code","title","cha_dep_code_nm","initValue","countYn","list_gubun","not_ancmt_sj","not_ancmt_cn","dept_nm","mobile_code","Key","temp").containsAll(q.keySet()))throw invalid();
        for(var e:Map.of("jndinm","OfrNotAncmtEJB","context","NTIS","method","selectOfrNotAncmt","methodnm","selectOfrNotAncmtRegst","homepage_pbs_yn","Y","subCheck","Y").entrySet())if(!e.getValue().equals(q.get(e.getKey())))throw invalid();
        return URI.create("https://"+s.host+s.path+"?jndinm=OfrNotAncmtEJB&context=NTIS&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+q.get(s.idKey)+"&homepage_pbs_yn=Y&subCheck=Y");
    }
    public static Element selectRoot(Site s,Document page){var root=selectSingle(page,s==Site.ASAN?"div.customContents > div.viewForm":"form[name=form1] > table.bbs_default.view");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.ASAN?":root > dl.ct_th04 > dt":":root > tbody > tr.subject > td > span.subject_text");}
    public static Element selectContent(Site s,Document page){return selectSingle(selectRoot(s,page),s==Site.ASAN?":root > div.ct_tc14 > div.ct_tc14 > div.field-name-body > div.field-items":":root > tbody > tr > td.bbs_content[title=내용]");}
    public static Element selectAttachments(Site s,Document page){
        var root=selectRoot(s,page);if(s==Site.ASAN){var cell=selectSingle(root,":root > div.ct_tc14 > div.ct_btn04");if(!"첨부파일".equals(selectSingle(cell,":root > b").text()))throw invalid();return cell;}
        var rows=root.select(":root > tbody > tr").stream().filter(e->e.select(":root > th").size()==1&&"첨부파일".equals(e.select(":root > th").text())).toList();if(rows.size()!=1)throw invalid();return selectSingle(rows.getFirst(),":root > td");
    }
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CHUNGCHEONG_SIXTH_STRUCTURE_CHANGED");}
}
