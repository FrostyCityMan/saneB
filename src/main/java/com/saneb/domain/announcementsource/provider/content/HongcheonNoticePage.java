package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 홍천 공식 고시공고의 제목·본문·첨부 영역. 목록 및 담당부서 정보는 본문에 넣지 않는다. */
public final class HongcheonNoticePage {
    public static final String HOST = "www.hongcheon.go.kr";
    public static final String PATH = "/www/selectEminwonView.do";
    private HongcheonNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!Set.of("key", "not_ancmt_mgt_no", "pageIndex", "pageUnit", "searchCnd", "searchKrwd", "ofr_pageSize").containsAll(query.keySet())
                || !"278".equals(query.get("key")) || !query.getOrDefault("not_ancmt_mgt_no", "").matches("[1-9][0-9]{0,14}")) throw selectInvalid();
        return URI.create("https://" + HOST + PATH + "?key=278&not_ancmt_mgt_no=" + query.get("not_ancmt_mgt_no"));
    }
    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "div.p-wrap.bbs.bbs__view > table.p-table.block");
        if (selectTitle(root).text().isBlank()) throw selectInvalid();
        return root;
    }
    public static Element selectTitle(Element root) {
        return selectSingle(root, ":root > tbody > tr.p-table__subject > td > span.p-table__subject_text");
    }
    public static Element selectContent(Document page) {
        return selectSingle(selectRoot(page), ":root > tbody > tr > td[colspan=4]");
    }
    public static Element selectAttachments(Document page) {
        var labels = selectRoot(page).select(":root > tbody > tr > th").stream()
                .filter(e -> "첨부파일".equals(e.text().strip())).toList();
        if (labels.size() != 1) throw selectInvalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || cell.nextElementSibling() != null) throw selectInvalid();
        return cell;
    }
    private static Element selectSingle(Element root, String selector) {
        var values = root.select(selector);
        if (values.size() != 1) throw selectInvalid();
        return values.getFirst();
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("HONGCHEON_STRUCTURE_CHANGED"); }
}
