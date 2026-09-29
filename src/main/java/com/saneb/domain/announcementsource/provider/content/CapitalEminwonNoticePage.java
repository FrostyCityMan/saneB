package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 양주·군포·여주의 실측 게시판 경계. 표시용 검색 인자를 다운로드 요청에 전파하지 않는다. */
public final class CapitalEminwonNoticePage {
    private CapitalEminwonNoticePage() { }
    public enum Site {
        YANGJU("www.yangju.go.kr", "eminwon.yangju.go.kr", "4075", "LGS-000101"),
        GUNPO("www.gunpo.go.kr", "eminwon.gunpo.go.kr", "3907", "LGS-000103"),
        YEOJU("www.yeoju.go.kr", "eminwon.yeoju.go.kr", "413", "LGS-000111");
        public final String host, fileHost, menu, sourceCode;
        Site(String host, String fileHost, String menu, String sourceCode) {
            this.host=host; this.fileHost=fileHost; this.menu=menu; this.sourceCode=sourceCode;
        }
    }
    public static final String DETAIL = "/www/selectEminwonView.do";
    private static final Set<String> OPTIONAL = Set.of("pageUnit", "pageIndex", "searchCnd", "searchKrwd", "ofr_pageSize");
    public static Site selectSite(URI uri) {
        if (uri != null) for (var site : Site.values()) if (site.host.equals(uri.getHost()) && DETAIL.equals(uri.getPath())) return site;
        return null;
    }
    public static URI selectDetailUri(Site site, URI uri) {
        if (site == null || uri == null || !"https".equals(uri.getScheme()) || !site.host.equals(uri.getHost())
                || (uri.getPort()!=-1 && uri.getPort()!=443) || uri.getUserInfo()!=null || uri.getFragment()!=null
                || !DETAIL.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var q=ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery());
        if (!site.menu.equals(q.get("key")) || !q.getOrDefault("not_ancmt_mgt_no", "").matches("[1-9][0-9]{0,14}")) throw invalid();
        for (String key:q.keySet()) if (!Set.of("key","not_ancmt_mgt_no").contains(key) && !OPTIONAL.contains(key)
                && !(site==Site.GUNPO && Set.of("Not_ancmt_se_code","notAncmtSeCd").contains(key))) throw invalid();
        if (site==Site.GUNPO && (!"01".equals(q.get("Not_ancmt_se_code")) || !"01".equals(q.get("notAncmtSeCd")))) throw invalid();
        return URI.create("https://"+site.host+DETAIL+"?key="+site.menu+"&not_ancmt_mgt_no="+q.get("not_ancmt_mgt_no")
                +(site==Site.GUNPO?"&Not_ancmt_se_code=01&notAncmtSeCd=01":""));
    }
    public static Element selectTable(Site site, Document page) {
        var tables=page.select(site==Site.YANGJU?"table.bbs_default.view":"div.p-wrap.bbs.bbs__view > table.p-table.block");
        if(tables.size()!=1)throw invalid();var table=tables.getFirst();
        if(selectTitle(site,table).text().isBlank())throw invalid();return table;
    }
    public static Element selectTitle(Site site, Element table) {
        if(site!=Site.YEOJU)return selectCell(table,"제목");
        var titles=table.select("span.p-table__subject_text").stream().filter(e->e.closest("table")==table).toList();
        if(titles.size()!=1||!titles.getFirst().select("table,script,input").isEmpty())throw invalid();return titles.getFirst();
    }
    public static Element selectContent(Site site, Document page) {
        var table=selectTable(site,page);
        var contents=table.select("td[title=내용]").stream().filter(e->e.closest("table")==table).toList();
        if(contents.size()!=1)throw invalid();var cell=contents.getFirst();
        if(!cell.hasClass(site==Site.YANGJU?"bbs_content":"p-table__content") || cell.nextElementSibling()!=null)throw invalid();
        if(site!=Site.YEOJU && selectCell(table,site==Site.YANGJU?"내용":"상세내용")!=cell)throw invalid();
        return cell;
    }
    public static Element selectAttachments(Site site, Document page) {
        var table=selectTable(site,page);
        if(site!=Site.YEOJU)return selectCell(table,site==Site.YANGJU?"파일":"첨부");
        var cells=table.select("td.p-table--attach").stream().filter(e->e.closest("table")==table).toList();
        if(cells.size()!=1||cells.getFirst().nextElementSibling()!=null)throw invalid();return cells.getFirst();
    }
    private static Element selectCell(Element table,String label) {
        var labels=table.select("th").stream().filter(e->e.closest("table")==table&&label.equals(e.text().strip())).toList();
        if(labels.size()!=1)throw invalid();var heading=labels.getFirst();var cell=heading.nextElementSibling();
        if(!"tr".equals(heading.parent().tagName())||!heading.children().isEmpty()||cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null)throw invalid();
        return cell;
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CAPITAL_EMINWON_STRUCTURE_CHANGED");}
}
