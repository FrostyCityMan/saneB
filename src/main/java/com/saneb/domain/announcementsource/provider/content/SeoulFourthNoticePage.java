package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 성동·송파·광진의 공식 상세에서 제목·본문·첨부 경계만 읽는다. */
public final class SeoulFourthNoticePage {
    private SeoulFourthNoticePage() { }
    public enum Site {
        SEONGDONG("www.sd.go.kr","/main/selectBbsNttView.do","nttNo","LGS-000005","HEURISTIC_NOTICE","1473"),
        SONGPA("www.songpa.go.kr","/www/selectGosiData.do","not_ancmt_mgt_no","LGS-000025","SAEOL_GOSI","2776"),
        GWANGJIN("www.gwangjin.go.kr","/portal/bbs/B0000003/view.do","nttId","LGS-000006","SAEOL_GOSI","200192");
        public final String host,path,idKey,sourceCode,parser,menu;
        Site(String host,String path,String idKey,String sourceCode,String parser,String menu){this.host=host;this.path=path;this.idKey=idKey;this.sourceCode=sourceCode;this.parser=parser;this.menu=menu;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){
        if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        String raw=u.getRawQuery();if(raw==null||raw.length()>8192)throw invalid();
        // 성동의 공식 링크에 삽입된 빈 query 구분자만 제거한다. 중복된 실제 key는 거부한다.
        if(s==Site.SEONGDONG)raw=String.join("&",Arrays.stream(raw.split("&",-1)).filter(p->!p.isEmpty()).toList());
        var q=CapitalThirdNoticePage.selectParameters(raw);String menuKey=s==Site.GWANGJIN?"menuNo":"key";
        var allowed=new HashSet<>(Set.of(menuKey,s.idKey,"searchCnd","pageIndex"));
        if(s==Site.SEONGDONG){allowed.addAll(Set.of("bbsNo","pageUnit","searchKrwd","searchCtgry","integrDeptCode","nttShowPd"));if(!"184".equals(q.get("bbsNo")))throw invalid();}
        if(s==Site.SONGPA)allowed.addAll(Set.of("searchKrwd","not_ancmt_se_code"));
        if(s==Site.GWANGJIN){allowed.addAll(Set.of("searchWrd","pSiteId"));if(q.containsKey("pSiteId")&&!"portal".equals(q.get("pSiteId")))throw invalid();}
        if(!allowed.containsAll(q.keySet())||!s.menu.equals(q.get(menuKey))||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+s.host+s.path+"?"+menuKey+"="+s.menu+(s==Site.SEONGDONG?"&bbsNo=184":"")+"&"+s.idKey+"="+q.get(s.idKey));
    }
    public static Element selectRoot(Site s,Document p){Element root=selectSingle(p,s==Site.GWANGJIN?"div.view > div.t":s==Site.SONGPA?"div.p-wrap.bbs.bbs__view > form[name=gosiFrm] > table.p-table.block":"div.p-wrap.bbs.bbs__view > table.p-table.block");if(selectTitle(s,root).text().isBlank()){if(s!=Site.SONGPA)throw invalid();SongpaNoticeIdentity.validateDetail(p,root);}return root;}
    public static Element selectTitle(Site s,Element root){return s==Site.SEONGDONG?selectSingle(root,":root > tbody > tr.p-table__subject > td > span.p-table__subject_text"):selectCell(s,root,s==Site.GWANGJIN?"공고명":"제목");}
    public static Element selectContent(Site s,Document p){var root=selectRoot(s,p);return s==Site.SEONGDONG?selectSingle(root,":root > tbody > tr > td.p-table__content > div.ntt_cn_container"):selectCell(s,root,"내용");}
    public static Element selectAttachments(Site s,Document p){return selectCell(s,selectRoot(s,p),s==Site.SONGPA?"파일":"첨부파일");}
    private static Element selectCell(Site s,Element root,String label){var labels=root.select(s==Site.GWANGJIN?":root > dl > dt":":root > tbody > tr > th").stream().filter(e->label.equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!(s==Site.GWANGJIN?"dd":"td").equals(cell.tagName()))throw invalid();return cell;}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEOUL_FOURTH_STRUCTURE_CHANGED");}
}
