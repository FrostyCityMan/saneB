package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 포항 고시공고의 공식 본문만 선택한다. 담당부서·첨부·메뉴는 제외한다. */
public final class PohangNoticePage {
    public static final String HOST = "www.pohang.go.kr", PATH = "/portal/saeol/gosi/view.do";
    private PohangNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static Element selectContent(Document page, URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!query.keySet().equals(Set.of("mid", "notAncmtMgtNo")) || !"0202010000".equals(query.get("mid"))
                || !query.getOrDefault("notAncmtMgtNo", "").matches("[0-9]{1,15}")) throw selectInvalid();
        var roots = page.select("form#detailForm[name=detailForm][method=post] div.bod_view");
        if (roots.size() != 1) throw selectInvalid();
        var titles = roots.getFirst().select(":root > div.subject");
        var bodies = roots.getFirst().select(":root > div.view_cont");
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || bodies.size() != 1) throw selectInvalid();
        return bodies.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("POHANG_STRUCTURE_CHANGED"); }
}
