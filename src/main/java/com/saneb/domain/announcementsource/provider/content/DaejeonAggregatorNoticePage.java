package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 대전 통합 목록에서 확인한 서구 새올 상세. 다른 구청 구조는 별도 연결한다. */
public final class DaejeonAggregatorNoticePage {
    public static final String HOST = "eminwon.seogu.go.kr";
    public static final String PATH = "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    private DaejeonAggregatorNoticePage() { }
    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath());
    }
    public static boolean selectRegisteredBridge(String registeredUrl, String detailUrl) {
        try {
            var source = URI.create(registeredUrl);
            selectDetailUri(URI.create(detailUrl));
            return "https".equals(source.getScheme()) && "www.daejeon.go.kr".equals(source.getHost())
                    && (source.getPort() == -1 || source.getPort() == 443) && source.getUserInfo() == null && source.getFragment() == null
                    && "/drh/MediaList.do".equals(source.getRawPath()) && source.equals(source.normalize())
                    && java.util.Map.of("notiType", "NOTI_06", "menuSeq", "2564").equals(CapitalThirdNoticePage.selectParameters(source.getRawQuery()));
        } catch (IllegalArgumentException | NullPointerException exception) { return false; }
    }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath())
                || !uri.equals(uri.normalize())) throw selectInvalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!q.keySet().equals(Set.of("subCheck", "jndinm", "context", "method", "methodnm", "not_ancmt_mgt_no"))
                || !"Y".equals(q.get("subCheck")) || !"OfrNotAncmtEJB".equals(q.get("jndinm"))
                || !"NTIS".equals(q.get("context")) || !"selectOfrNotAncmt".equals(q.get("method"))
                || !"selectOfrNotAncmtRegst".equals(q.get("methodnm"))
                || !q.getOrDefault("not_ancmt_mgt_no", "").matches("[0-9]{1,15}")) throw selectInvalid();
        return uri;
    }
    public static Element selectRoot(Document page) {
        var roots = page.select("form[name=form1][method=post] > table.tbl_board");
        if (roots.size() != 1 || selectTitle(roots.getFirst()).text().isBlank()) throw selectInvalid();
        return roots.getFirst();
    }
    public static Element selectTitle(Element root) { return selectCell(root, "제목"); }
    public static Element selectAttachments(Document page) { return selectCell(selectRoot(page), "첨부파일"); }
    public static Element selectContent(Document page, URI uri) {
        selectDetailUri(uri); var root = selectRoot(page);
        var bodies = root.select("td.aleft.end[colspan=4]").stream().filter(e -> e.closest("table") == root).toList();
        if (bodies.size() != 1 || selectTitle(root).parent().nextElementSibling() != bodies.getFirst().parent()) throw selectInvalid();
        return bodies.getFirst();
    }
    private static Element selectCell(Element root, String label) {
        var labels = root.select("th").stream().filter(e -> e.closest("table") == root && label.equals(e.text().strip())).toList();
        if (labels.size() != 1) throw selectInvalid();
        var cell = labels.getFirst().nextElementSibling();
        if (cell == null || !"td".equals(cell.tagName()) || !"3".equals(cell.attr("colspan")) || cell.nextElementSibling() != null) throw selectInvalid();
        return cell;
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("DAEJEON_AGGREGATOR_STRUCTURE_CHANGED"); }
}
