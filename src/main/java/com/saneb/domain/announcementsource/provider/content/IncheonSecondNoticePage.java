package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 제물포·미추홀의 실측된 공식 게시판 영역만 선택한다. */
public final class IncheonSecondNoticePage {
    private IncheonSecondNoticePage() { }
    public enum Site {
        JEMULPO("www.jemulpo.go.kr","/main/bbs/bbsMsgDetail.do","LGS-000055","msg_seq"),
        MICHUHOL("www.michuhol.go.kr","/main/board/view.do","LGS-000057","sq");
        public final String host,path,sourceCode,idKey;
        Site(String host,String path,String sourceCode,String idKey){this.host=host;this.path=path;this.sourceCode=sourceCode;this.idKey=idKey;}
    }
    public static Site selectSite(URI u){return u==null?null:Arrays.stream(Site.values()).filter(s->s.host.equals(u.getHost())&&s.path.equals(u.getPath())).findFirst().orElse(null);}
    public static URI selectDetailUri(Site s,URI u){if(s==null||u==null||!"https".equals(u.getScheme())||!s.host.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!s.path.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());var allowed=s==Site.JEMULPO?Set.of("bcd","msg_seq","keyfield","keyword","symd","eymd","pgno","listsz"):Set.of("board_code","sq","search","page","srchKey","srchValue","srchCate","maxRows");if(!allowed.containsAll(q.keySet())||!q.getOrDefault(s.idKey,"").matches("[1-9][0-9]{0,14}")||!(s==Site.JEMULPO?"announce".equals(q.get("bcd")):"board_13".equals(q.get("board_code"))))throw invalid();return URI.create("https://"+s.host+s.path+"?"+(s==Site.JEMULPO?"bcd=announce&":"board_code=board_13&")+s.idKey+"="+q.get(s.idKey));}
    public static Element selectRoot(Site s,Document p){var root=selectSingle(p,s==Site.JEMULPO?"div.board-view":"div.board-view-s1");if(selectTitle(s,root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Site s,Element root){return selectSingle(root,s==Site.JEMULPO?":root > div.title":":root > h3.board-title");}
    public static Element selectContent(Site s,Document p){return selectSingle(selectRoot(s,p),s==Site.JEMULPO?":root > div.con-box > div.detail":":root > div.content.editor_content");}
    public static Element selectAttachments(Site s,Document p){var root=selectRoot(s,p);if(s==Site.MICHUHOL)return selectSingle(root,":root > div.file-area.file-crawling");var dl=selectSingle(root,":root > ul.info-data > li.attach-file > dl");if(!"첨부파일".equals(selectSingle(dl,":root > dt").text().strip()))throw invalid();return selectSingle(dl,":root > dd");}
    private static Element selectSingle(Element e,String selector){var found=e.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INCHEON_SECOND_STRUCTURE_CHANGED");}
}
