package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 양구·인제 게시판의 고정 상세 경계. */
public final class GangwonSecondNoticePage {
    private GangwonSecondNoticePage() { }
    public enum Site {
        YANGGU("www.yanggu.go.kr","/user_sub","bk","LGS-000131","SAEOL_GOSI","/fnc_bbs/user_bbs_download"),
        INJE("www.inje.go.kr","/portal/adm/bulletin","articleSeq","LGS-000132","SPRING_BBS","/egf/bp/board/article/download");
        public final String host,path,idKey,sourceCode,parser,download;
        Site(String h,String p,String id,String sc,String parser,String down) {host=h;path=p;idKey=id;sourceCode=sc;this.parser=parser;download=down;}
    }
    public static Site selectSite(URI uri) { if(uri!=null)for(var s:Site.values())if(s.host.equals(uri.getHost())&&s.path.equals(uri.getPath()))return s;return null; }
    public static URI selectDetailUri(Site s,URI uri) {
        if(s==null||uri==null||!"https".equals(uri.getScheme())||!s.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!s.path.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(s==Site.YANGGU) {
            if(!Set.of("gfnc","mu_idx","bt","bcd","bk","pg","pgsize","sbfd","sbt").containsAll(q.keySet())||!"www".equals(q.get("gfnc"))||!"226".equals(q.get("mu_idx"))||!"rd".equals(q.get("bt"))||!"announcement".equals(q.get("bcd"))||!q.getOrDefault("bk","").matches("[A-Z]{5}[0-9]{15}"))throw invalid();
            return URI.create("https://"+s.host+s.path+"?gfnc=www&bk="+q.get("bk")+"&mu_idx=226&bt=rd&bcd=announcement");
        }
        if(!Set.of("articleSeq","pageIndex","mode","firstYN","searchCondition","searchKeyword").containsAll(q.keySet())||!q.getOrDefault("articleSeq","").matches("[1-9][0-9]{0,14}"))throw invalid();
        return URI.create("https://"+s.host+s.path+"?articleSeq="+q.get("articleSeq"));
    }
    public static Element selectRoot(Site s,Document page) {
        var root=selectSingle(page,s==Site.YANGGU?"div#user_board_whole > form#registform[name=registform][method=post] > fieldset":"div.skinTb.skinTb-data-resList.skinTb-data-bgSbj");
        if(selectTitle(s,root).text().isBlank())throw invalid();return root;
    }
    public static Element selectTitle(Site s,Element root) {return s==Site.YANGGU?selectSingle(root,":root > div#user_board_read_title"):selectCell(root,"제목");}
    public static Element selectContent(Site s,Document page) {
        var root=selectRoot(s,page);return selectSingle(root,s==Site.YANGGU?":root > div#user_board_read_view > div.user_board_read_view_pre":":root > div.skinTb-tr > div.skinTb-conts");
    }
    public static Element selectAttachments(Site s,Document page) {
        var root=selectRoot(s,page);if(s==Site.INJE)return selectCell(root,"첨부파일");
        var table=selectSingle(root,":root > div#user_board_read_file > table");var headers=table.select("th");
        if(headers.size()!=1||!"파일".equals(headers.getFirst().text()))throw invalid();
        var cell=headers.getFirst().nextElementSibling();if(cell==null||!cell.hasClass("file_list")||cell.nextElementSibling()!=null)throw invalid();return cell;
    }
    private static Element selectCell(Element root,String label) {
        var labels=root.select(":root > div.skinTb-tr > div.skinTb-th").stream().filter(e->label.equals(e.text().strip())).toList();
        if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!cell.hasClass("skinTb-td")||cell.nextElementSibling()!=null)throw invalid();return cell;
    }
    private static Element selectSingle(Element root,String selector) {var e=root.select(selector);if(e.size()!=1)throw invalid();return e.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("GANGWON_SECOND_STRUCTURE_CHANGED");}
}
