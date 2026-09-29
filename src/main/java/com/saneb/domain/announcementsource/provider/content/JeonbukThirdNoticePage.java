package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 전주·전북도에서 관측한 공고 제목·본문·첨부 영역만 선택한다. */
public final class JeonbukThirdNoticePage {
    private JeonbukThirdNoticePage() { }
    public enum Site {
        JEONJU("www.jeonju.go.kr","/planweb/board/view.9is","/planweb/board/download.9is","boardUid","9be517a7914528ce01930aa3ddc26cf0","contentUid","ff8080818990c349018b041a879f395a","dataUid","fileUid","LGS-000164"),
        JEONBUK("www.jeonbuk.go.kr","/board/view.jeonbuk","/board/download.jeonbuk","boardId","BBS_0000129","menuCd","DOM_000000102002005000","dataSid","fileSid","LGS-000163");
        public final String host,path,download,boardKey,board,menuKey,menu,idKey,fileKey,sourceCode;
        Site(String h,String p,String d,String bk,String b,String mk,String m,String ik,String fk,String sc){host=h;path=p;download=d;boardKey=bk;board=b;menuKey=mk;menu=m;idKey=ik;fileKey=fk;sourceCode=sc;}
        public boolean selectId(String id){return id!=null&&id.matches(this==JEONJU?"[a-f0-9]{31,32}":"[1-9][0-9]{0,14}");}
    }
    public static Site selectSite(URI u){if(u!=null)for(var s:Site.values())if(s.host.equals(u.getHost())&&s.path.equals(u.getPath()))return s;return null;}
    public static boolean selectSafeUri(Site s,URI u){return s!=null&&u!=null&&"https".equals(u.getScheme())&&s.host.equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    public static Map<String,String> selectParameters(Site s,URI u){
        // 전주 공식 링크의 빈 && 구간만 제거한다. 중복 이름은 공통 파서에서 계속 거부한다.
        String q=u.getRawQuery();if(s==Site.JEONJU&&q!=null)q=q.replaceAll("&{2,}","&");
        return CapitalThirdNoticePage.selectParameters(q);
    }
    public static URI selectDetailUri(Site s,URI u){
        if(!selectSafeUri(s,u)||!s.path.equals(u.getPath()))throw invalid();var q=selectParameters(s,u);
        if(!s.board.equals(q.get(s.boardKey))||!s.menu.equals(q.get(s.menuKey))||!s.selectId(q.get(s.idKey)))throw invalid();
        var allowed=s==Site.JEONJU?Set.of("boardUid","contentUid","dataUid","searchType","tmpField14","page","keyword","subPath","rowCount"):
                Set.of("boardId","menuCd","dataSid","orderBy","paging","startPage","searchType","keyword","categoryCode1","categoryCode2","listRow");
        if(!allowed.containsAll(q.keySet()))throw invalid();
        return URI.create("https://"+s.host+s.path+"?"+s.boardKey+"="+s.board+"&"+s.menuKey+"="+s.menu+"&"+s.idKey+"="+q.get(s.idKey));
    }
    public static Element selectRoot(Site s,Document page){var root=selectSingle(page,s==Site.JEONJU?"div#board_wrap > div.view-group":"div.bbs_skin > div.bbs_view");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return s==Site.JEONJU?selectSingle(selectLabel(root,"제목"),":root > span"):selectSingle(root,":root > div.bbs_vtop > h4");}
    public static Element selectContent(Site s,Document page){return selectSingle(selectRoot(s,page),s==Site.JEONJU?":root > div.view-list > div.view-con":":root > div.bbs_con");}
    public static Element selectAttachments(Site s,Document page){var root=selectRoot(s,page);return s==Site.JEONJU?selectSingle(selectLabel(root,"첨부파일"),":root > div"):selectSingle(root,":root > p.bbs_filedown");}
    private static Element selectLabel(Element root,String label){var rows=root.select(":root > div.view-table > ul > li").stream().filter(e->e.select(":root > strong").size()==1&&label.equals(e.select(":root > strong").text())).toList();if(rows.size()!=1)throw invalid();return rows.getFirst();}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("JEONBUK_THIRD_STRUCTURE_CHANGED");}
}
