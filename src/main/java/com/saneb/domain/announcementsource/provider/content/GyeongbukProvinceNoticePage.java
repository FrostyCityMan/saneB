package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 경북도 새 공식 게시판의 고정 고시공고 메뉴·본문·파일 영역. */
public final class GyeongbukProvinceNoticePage {
    public static final String HOST = "www.gb.go.kr", PATH = "/page/10109/67.do";
    private GyeongbukProvinceNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PATH.equals(uri.getPath()); }
    public static URI selectDetailUri(URI uri) {
        if (!selectMatches(uri) || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || !PATH.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if (!q.keySet().equals(Set.of("pageDtlOrdrNo","boardNo","boardMngNo","importUrl"))
                || !"1".equals(q.get("pageDtlOrdrNo")) || !"71".equals(q.get("boardMngNo"))
                || !q.get("boardNo").matches("[1-9][0-9]{0,14}")
                || !Set.of("/board/view.do","%2Fboard%2Fview.do").contains(q.get("importUrl"))) throw invalid();
        // 기존 canonicalizer의 추가 인코딩을 고정 importUrl 값에 한해서만 복원한다.
        return URI.create("https://" + HOST + PATH + "?pageDtlOrdrNo=1&boardNo=" + q.get("boardNo") + "&boardMngNo=71&importUrl=%2Fboard%2Fview.do");
    }
    public static Element selectRoot(Document page) {
        var root = selectOne(page,"form#boardViewForm[method=get]");
        if (selectTitle(root).text().isBlank()) throw invalid(); return root;
    }
    public static Element selectTitle(Element root) { return selectOne(root,":root > div.table_shape_tit > p"); }
    public static Element selectContent(Document page) { return selectOne(selectRoot(page),":root > div.board_view > div.text"); }
    public static Element selectFileArea(Document page) { return selectOne(selectRoot(page),":root > ul.table_shape.input_form.board_view > li > div.td_shape.file"); }
    public static Element selectFileSlot(Document page) { return selectOne(selectFileArea(page),":root > section > div.fileWrap.fileId[data-id=fileId]"); }
    private static Element selectOne(Element root,String selector) { var values = root.select(selector); if (values.size() != 1) throw invalid(); return values.getFirst(); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("GYEONGBUK_PROVINCE_STRUCTURE_CHANGED"); }
}
