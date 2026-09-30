package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 천안 공식 구형 새올의 원문 제목·본문을 중첩 첨부 표와 분리한다. */
public final class CheonanNoticePage {
    public static final String HOST="eminwon.cheonan.go.kr", DETAIL="/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    private CheonanNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri!=null&&HOST.equals(uri.getHost())&&DETAIL.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!uri.equals(uri.normalize())||!uri.getPath().equals(uri.getRawPath()))throw invalid();
        var values=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(values.size()!=7||!values.entrySet().containsAll(Map.of("context","NTIS","homepage_pbs_yn","Y","jndinm","OfrNotAncmtEJB",
                "method","selectOfrNotAncmt","methodnm","selectOfrNotAncmtRegst","subCheck","Y").entrySet())
                ||!values.getOrDefault("not_ancmt_mgt_no","").matches("[0-9]{1,15}"))throw invalid();
        return uri;
    }
    public static Element selectRoot(Document document) {
        var forms=document.select("form");
        if(forms.size()!=1||!"form".equals(forms.getFirst().attr("name"))||!"post".equalsIgnoreCase(forms.getFirst().attr("method")))throw invalid();
        var tables=forms.getFirst().select("table[width='98%'][border=0][cellspacing=1][cellpadding=0]");
        if(tables.size()!=1)throw invalid();var table=tables.getFirst();selectTitle(table);return table;
    }
    public static Element selectTitle(Element table) {
        var labels=table.select("td").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
        if(labels.size()!=1)throw invalid();var title=labels.getFirst().nextElementSibling();
        if(title==null||!"td".equals(title.tagName())||!title.children().isEmpty()||title.text().isBlank())throw invalid();return title;
    }
    public static Element selectContent(Document document) {
        var table=selectRoot(document);
        var bodies=table.select("td[colspan=4][style]").stream().filter(e->e.closest("table")==table
                &&e.attr("style").matches("(?i)\\s*word-break\\s*:\\s*break-all\\s*;?\\s*")).toList();
        if(bodies.size()!=1||bodies.getFirst().parent().childrenSize()!=1)throw invalid();return bodies.getFirst();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CHEONAN_STRUCTURE_CHANGED");}
}
