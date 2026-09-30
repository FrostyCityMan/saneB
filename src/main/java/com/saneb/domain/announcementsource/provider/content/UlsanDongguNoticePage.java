package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 울산 동구의 고정 공개 조회 폼과 제목·본문·공식 첨부 영역을 검증한다. */
public final class UlsanDongguNoticePage {
    public static final String HOST = "eminwon.donggu.ulsan.kr";
    public static final String DETAIL = "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    private static final Map<String, String> FIXED = Map.of(
            "context", "NTIS", "jndinm", "OfrNotAncmtEJB", "method", "selectOfrNotAncmt",
            "methodnm", "selectOfrNotAncmtRegst", "not_ancmt_se_code", "01,03,04,05", "subCheck", "Y");

    private UlsanDongguNoticePage() { }

    public static boolean selectMatches(URI uri) {
        return uri != null && HOST.equals(uri.getHost()) && DETAIL.equals(uri.getPath());
    }

    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null
                || uri.getFragment() != null || !DETAIL.equals(uri.getRawPath()) || !uri.equals(uri.normalize()))
            throw invalid();
        var values = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (values.size() != 7 || !values.entrySet().containsAll(FIXED.entrySet())
                || !values.getOrDefault("not_ancmt_mgt_no", "").matches("[1-9][0-9]{0,14}")) throw invalid();
        return uri;
    }

    public static Element selectRoot(Document page) {
        var root = selectSingle(page, "form[name=form1][method=post] > div#viewTable1vw > div.bbs_detail");
        if (selectTitle(root).text().isBlank()) throw invalid();
        return root;
    }

    public static AttachmentPinnedDownloadClient.Request selectRequest(URI uri) {
        selectDetailUri(uri);
        return new AttachmentPinnedDownloadClient.Request(URI.create("https://" + HOST + DETAIL),
                "POST", CapitalThirdNoticePage.selectParameters(uri.getRawQuery()));
    }

    public static Element selectTitle(Element root) {
        return selectSingle(root, ":root > div.bbs_detail_tit > h2");
    }

    public static Element selectContent(Document page) {
        return selectSingle(selectRoot(page), ":root > div.bbs_detail_content");
    }

    public static Element selectAttachments(Document page) {
        return selectSingle(selectRoot(page), ":root > div.bbs_detail_file#download");
    }

    private static Element selectSingle(Element parent, String selector) {
        var elements = parent.select(selector);
        if (elements.size() != 1) throw invalid();
        return elements.getFirst();
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("ULSAN_DONGGU_STRUCTURE_CHANGED");
    }
}
