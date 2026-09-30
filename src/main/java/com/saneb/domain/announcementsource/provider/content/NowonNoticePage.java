package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 노원 고시공고 상세의 제목·본문·공식 첨부 영역. 메뉴 및 이미지 링크를 첨부로 확장하지 않는다. */
public final class NowonNoticePage {
    public static final String HOST="www.nowon.kr", PATH="/www/user/bbs/BD_selectBbs.do";
    private static final Map<String,String> FIXED=Map.of("q_bbsCode","1003","q_clCode","0","q_estnColumn1","11","q_ntceSiteCode","11");
    private NowonNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static URI selectDetailUri(URI uri){
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!PATH.equals(uri.getRawPath())||!uri.equals(uri.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(!Set.of("q_bbsCode","q_clCode","q_estnColumn1","q_ntceSiteCode","q_bbscttSn","q_searchKeyTy","q_searchVal","q_currPage","q_rowPerPage","q_pagingStartNum","q_sortName","q_sortOrder","q_estnColumn7").containsAll(q.keySet())
                ||!FIXED.entrySet().stream().allMatch(e->e.getValue().equals(q.get(e.getKey())))||!q.getOrDefault("q_bbscttSn","").matches("[0-9]{17}"))throw invalid();
        return URI.create("https://"+HOST+PATH+"?q_bbsCode=1003&q_clCode=0&q_estnColumn1=11&q_ntceSiteCode=11&q_bbscttSn="+q.get("q_bbscttSn"));
    }
    public static Element selectRoot(Document page){var root=single(page,"div.article-view");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){return single(root,":root > h1.article-subject");}
    public static Element selectContent(Document page,URI uri){selectDetailUri(uri);return single(selectRoot(page),":root > div.article-body > div.txt");}
    public static Element selectAttachments(Document page){
        var table=single(selectRoot(page),":root > table.table-article");
        var labels=table.select(":root > tbody > tr > th[scope=row]").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();
        if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();
        if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null)throw invalid();
        // 전체 셀을 반환하여 첨부 목록 밖의 미확인 제어도 잔여 검사에 남긴다.
        single(cell,":root > ul.file-list");return cell;
    }
    private static Element single(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("NOWON_STRUCTURE_CHANGED");}
}
