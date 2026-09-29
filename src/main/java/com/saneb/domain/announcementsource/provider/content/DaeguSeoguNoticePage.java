package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 대구 서구 현재·과거 게재 메뉴의 공식 본문 경계. 게재 상태를 공고 활성화로 해석하지 않는다. */
public final class DaeguSeoguNoticePage {
    public static final String HOST = "www.dgs.go.kr", PATH = "/portal/saeol/gosi/view.do";
    public static final Set<String> MENUS = Set.of("0601020100", "0601020200");
    private DaeguSeoguNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static Element selectContent(Document page, URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!query.keySet().equals(Set.of("mid", "notAncmtMgtNo")) || !MENUS.contains(query.getOrDefault("mid", ""))
                || !query.getOrDefault("notAncmtMgtNo", "").matches("[0-9]{1,15}")) throw selectInvalid();
        var roots = page.select("form#detailForm[name=detailForm][method=post] div.bod_view");
        if (roots.size() != 1) throw selectInvalid();
        var titles = roots.getFirst().select(":root > div.subject");
        var bodies = roots.getFirst().select(":root > div.view_cont");
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || bodies.size() != 1) throw selectInvalid();
        return bodies.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("DAEGU_SEOGU_STRUCTURE_CHANGED"); }
}
