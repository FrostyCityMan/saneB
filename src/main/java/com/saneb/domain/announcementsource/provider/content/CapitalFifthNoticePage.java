package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 안성·의정부·경기 광주의 공식 제목/본문/첨부 경계를 공유한다. */
public final class CapitalFifthNoticePage {
    private CapitalFifthNoticePage() { }

    public enum Site {
        ANSEONG("www.anseong.go.kr", "eminwon.anseong.go.kr", "/portal/saeol/gosiView.do", "0501040000", "LGS-000106", "SAFE_PORTAL_SAEOL_BOARD_VIEW"),
        UIJEONGBU("www.ui4u.go.kr", "eminwon.ui4u.go.kr", "/portal/saeol/gosiView.do", "0301040000", "LGS-000098", "SAFE_PORTAL_SAEOL_BOARD_VIEW"),
        GG_GWANGJU("www.gjcity.go.kr", "eminwon.gjcity.go.kr", "/portal/saeol/gosi/view.do", "0202010000", "LGS-000099", "SAEOL_GOSI");
        public final String host, fileHost, path, menu, sourceCode, parser;
        public final String download = "/emwp/jsp/ofr/FileDown.jsp";
        Site(String host, String fileHost, String path, String menu, String sourceCode, String parser) {
            this.host=host; this.fileHost=fileHost; this.path=path; this.menu=menu; this.sourceCode=sourceCode; this.parser=parser;
        }
    }

    public static Site selectSite(URI uri) {
        if (uri!=null) for (var site:Site.values()) if (site.host.equals(uri.getHost())&&site.path.equals(uri.getPath())) return site;
        return null;
    }

    public static URI selectDetailUri(Site site, URI uri) {
        if (site==null||uri==null||!"https".equals(uri.getScheme())||!site.host.equals(uri.getHost())
                ||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null
                ||!site.path.equals(uri.getRawPath())||!uri.equals(uri.normalize())) throw invalid();
        var query=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!site.menu.equals(query.get("mId"))||!query.getOrDefault("notAncmtMgtNo", "").matches("[1-9][0-9]{0,14}")
                ||!Set.of("mId","notAncmtMgtNo","page","searchType","searchTxt","seCode").containsAll(query.keySet())) throw invalid();
        return URI.create("https://"+site.host+site.path+"?mId="+site.menu+"&notAncmtMgtNo="+query.get("notAncmtMgtNo"));
    }

    public static Element selectRoot(Site site, Document page) {
        String selector="form#detailForm[name=detailForm][method=post] > "+(site==Site.UIJEONGBU?"":"div.bod_wrap > ")+"div.bod_view";
        var root=selectSingle(page,selector);
        if (selectTitle(root).text().isBlank()) throw invalid();
        return root;
    }

    public static Element selectTitle(Element root) { return selectSingle(root, ":root > h4"); }
    public static Element selectContent(Site site, Document page) { return selectSingle(selectRoot(site,page), ":root > div.view_cont"); }
    public static Element selectAttachments(Site site, Document page) {
        var dl=selectSingle(selectRoot(site,page), ":root > dl.view_file");
        if(dl.childrenSize()!=2||!"dt".equals(dl.child(0).tagName())||!"첨부파일".equals(dl.child(0).text().replace(" ",""))||!"dd".equals(dl.child(1).tagName())) throw invalid();
        return dl.child(1);
    }
    private static Element selectSingle(Element root,String selector) {
        var matches=root.select(selector); if(matches.size()!=1) throw invalid(); return matches.getFirst();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("CAPITAL_FIFTH_STRUCTURE_CHANGED"); }
}
