package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 강동 공식 포털의 제목·본문·첨부 영역만 선택한다. 메뉴와 담당자 정보는 본문에 섞지 않는다. */
public final class GangdongNoticePage {
    public static final String HOST="www.gangdong.go.kr", PREFIX="/web/newportal/notice/01/";
    private GangdongNoticePage() { }
    public static boolean selectMatches(URI uri) { return uri!=null&&HOST.equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith(PREFIX); }
    public static URI selectDetailUri(URI uri) {
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getRawQuery()!=null||!uri.equals(uri.normalize())
                ||!uri.getPath().equals(uri.getRawPath())||!uri.getPath().substring(PREFIX.length()).matches("[1-9][0-9]{0,14}")) throw invalid();
        return uri;
    }
    public static Element selectRoot(Document page) {
        var form=selectOne(page,"form#frmNotice[method=get][action=/web/newportal/notice/01]");
        var table=selectOne(form,"table");if(selectTitle(table).text().isBlank())throw invalid();return table;
    }
    public static Element selectTitle(Element table) { return selectOne(table,":root > tbody > tr > th:matchesOwn(^제목$) + td[colspan=3]"); }
    public static Element selectContent(Document page) {
        var body=selectOne(selectRoot(page),":root > tbody > tr > td[colspan=4]");
        if(body.parent().childrenSize()!=1)throw invalid();return body;
    }
    public static Element selectAttachments(Document page) { return selectOne(selectRoot(page),":root > tbody > tr > th:matchesOwn(^첨부파일$) + td[colspan=3]"); }
    private static Element selectOne(Element root,String selector) { var elements=root.select(selector);if(elements.size()!=1)throw invalid();return elements.getFirst(); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("GANGDONG_STRUCTURE_CHANGED"); }
}
