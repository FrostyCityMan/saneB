package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 고정 세 기관의 공식 제목·본문·첨부 영역. 담당자 메타데이터는 본문과 분리한다. */
public final class SeoulFifthNoticePage {
    private SeoulFifthNoticePage() { }
    public enum Site {
        DONGDAEMUN("www.ddm.go.kr","eminwon.ddm.go.kr","selectEminwonWebView.do","3291","LGS-000007","SPRING_BBS","FileDownNew.jsp"),
        SEONGBUK("www.sb.go.kr","eminwon.sb.go.kr","selectEminwonView.do","6920","LGS-000009","SEONGBUK_EMINWON_TABLE","FileDownNew.jsp"),
        YEONGDEUNGPO("www.ydp.go.kr","eminwon.ydp.go.kr","selectEminwonView.do","2851","LGS-000020","SAEOL_GOSI","FileDown.jsp");
        public final String host,fileHost,path,menu,sourceCode,parser,download;
        Site(String host,String fileHost,String path,String menu,String sourceCode,String parser,String download){this.host=host;this.fileHost=fileHost;this.path="/www/"+path;this.menu=menu;this.sourceCode=sourceCode;this.parser=parser;this.download="/emwp/jsp/ofr/"+download;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        var allowed=new HashSet<>(Set.of("key","notAncmtMgtNo","pageIndex","pageUnit","searchCnd","searchKrwd"));
        if(s==Site.DONGDAEMUN){allowed.add("searchNotAncmtSeCode");if(!"01,02,04,05,06,07".equals(q.get("searchNotAncmtSeCode")))throw invalid();}
        if(s==Site.SEONGBUK)allowed.addAll(Set.of("searchCnd2","depNm","notAncmtSeCode","bgnde","endde"));
        if(s==Site.YEONGDEUNGPO){allowed.addAll(Set.of("menuFlag","not_ancmt_se_code"));if(!"01".equals(q.get("menuFlag")))throw invalid();}
        if(!allowed.containsAll(q.keySet())||!s.menu.equals(q.get("key"))||!q.getOrDefault("notAncmtMgtNo","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+s.host+s.path+"?key="+s.menu+"&notAncmtMgtNo="+q.get("notAncmtMgtNo")+(s==Site.DONGDAEMUN?"&searchNotAncmtSeCode=01%2C02%2C04%2C05%2C06%2C07":s==Site.YEONGDEUNGPO?"&menuFlag=01":""));}
    public static Element selectRoot(Site s,Document p){var root=selectSingle(p,s==Site.DONGDAEMUN?"div.p-wrap.bbs.bbs__view > div.table-responsive > table.p-table.scroll":s==Site.SEONGBUK?"table.p-table.block":"div.p-wrap.bbs.bbs__view > table.p-table.block");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return s==Site.YEONGDEUNGPO?selectSingle(root,":root > tbody > tr.p-table__subject > td > span.p-table__subject_text"):selectCell(root,s==Site.DONGDAEMUN?"고시공고명":"제목");}
    public static Element selectContent(Site s,Document p){var root=selectRoot(s,p);return s==Site.YEONGDEUNGPO?selectSingle(root,":root > tbody > tr > td.p-table__content[colspan=4]"):selectCell(root,"내용");}
    public static Element selectAttachments(Site s,Document p){return selectCell(selectRoot(s,p),s==Site.YEONGDEUNGPO?"파일":"첨부파일");}
    private static Element selectCell(Element root,String label){var found=root.select(":root > tbody > tr > th").stream().filter(e->label.equals(e.text().strip())).toList();if(found.size()!=1)throw invalid();var cell=found.getFirst().nextElementSibling();if(cell==null||!"td".equals(cell.tagName()))throw invalid();return cell;}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEOUL_FIFTH_STRUCTURE_CHANGED");}
}
