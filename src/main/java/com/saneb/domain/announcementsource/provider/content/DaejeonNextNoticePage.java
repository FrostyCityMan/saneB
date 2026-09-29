package com.saneb.domain.announcementsource.provider.content;
import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
/** 유성·대덕 공식 상세 영역을 분리하며 목록/관리 화면은 허용하지 않는다. */
public final class DaejeonNextNoticePage {
    private DaejeonNextNoticePage() { }
    public enum Site {
        YUSEONG("www.yuseong.go.kr","eminwon.yuseong.go.kr","/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do","LGS-000075","SAFE_YUSEONG_LEGAL_NOTICE","notAncmtMgtNo"),
        DAEDEOK("www.daedeok.go.kr","eminwon.daedeok.go.kr","/dpt/dpt04/DPT040204_cmmBoardView.do","LGS-000076","SPRING_BBS","ntatcSeq");
        public final String host,fileHost,path,sourceCode,parser,idKey;
        Site(String h,String f,String p,String s,String parser,String id){host=h;fileHost=f;path=p;sourceCode=s;this.parser=parser;idKey=id;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());var allowed=s==Site.YUSEONG?Set.of("notAncmtMgtNo","pageIndex","searchCondition","searchKeyword","status"):Set.of("boardId","ntatcSeq","pageIndex","searchCondition","searchKeyword","categorySeq");if(!allowed.containsAll(q.keySet())||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}")||(s==Site.DAEDEOK&&!"DPT_000087".equals(q.get("boardId"))))throw invalid();return URI.create("https://"+s.host+s.path+"?"+(s==Site.DAEDEOK?"boardId=DPT_000087&":"")+s.idKey+"="+q.get(s.idKey));}
    public static Element selectRoot(Site s,Document p){var r=selectSingle(p,s==Site.YUSEONG?"div.program--contents > div.ui.bbs--view":"table.table2023:has(th.tit00)");if(selectTitle(s,r).text().isBlank())throw invalid();return r;}
    public static Element selectTitle(Site s,Element r){return selectSingle(r,s==Site.YUSEONG?":root > div.ui.bbs--view--header > h2.ui.bbs--view--tit":":root > tbody > tr > th.tit00");}
    public static Element selectContent(Site s,Document p){return selectSingle(selectRoot(s,p),s==Site.YUSEONG?":root > div.ui.bbs--view--cont > div.ui.bbs--detail--cont > div.ui.bbs--view--content":":root > tbody > tr > td.cont_area");}
    public static Element selectAttachments(Site s,Document p){var r=selectRoot(s,p);if(s==Site.YUSEONG)return selectSingle(r,":root > div.ui.bbs--view--file");var labels=r.select(":root > tbody > tr > th").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null)throw invalid();return cell;}
    private static Element selectSingle(Element e,String selector){var result=e.select(selector);if(result.size()!=1)throw invalid();return result.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("DAEJEON_NEXT_STRUCTURE_CHANGED");}
}
