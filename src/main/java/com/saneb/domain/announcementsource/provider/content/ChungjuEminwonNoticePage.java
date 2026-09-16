package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 충주시 공식 고시공고의 URL·표 경계. 다른 게시판이나 페이지 전체를 본문으로 대체하지 않는다. */
public final class ChungjuEminwonNoticePage {
    private ChungjuEminwonNoticePage() { }
    public static final String HOST = "www.chungju.go.kr";
    public static final String DETAIL = "/www/selectEminwonView.do";
    private static final Set<String> OPTIONAL = Set.of("pageUnit", "pageIndex", "searchCnd", "jndinm", "context",
            "method", "methodnm", "wkly_event_mgt_no", "subCheck", "ofr_pageSize", "homepage_pbs_yn", "initValue",
            "wkly_se_code", "countYn", "event_sj", "ancmt_se_code", "title", "cha_dep_code_nm", "countYnAC",
            "list_gubun", "ancmt_sj", "ancmt_cn");

    public static URI selectDetailUri(URI uri) {
        if (uri == null || !"https".equals(uri.getScheme()) || !HOST.equals(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                || !DETAIL.equals(uri.getRawPath()) || !uri.equals(uri.normalize())) throw invalid();
        Map<String,String> query = selectParameters(uri.getRawQuery());
        if (!"510".equals(query.get("key")) || !query.getOrDefault("ancmt_mgt_no", "").matches("[1-9][0-9]{0,14}")
                || query.keySet().stream().anyMatch(k -> !Set.of("key", "ancmt_mgt_no").contains(k) && !OPTIONAL.contains(k))) throw invalid();
        // 목록의 검색/표시 인자는 저장 identity에만 남기며 첨부 상세 재요청에는 전달하지 않는다.
        return URI.create("https://" + HOST + DETAIL + "?key=510&ancmt_mgt_no=" + query.get("ancmt_mgt_no"));
    }

    public static Map<String,String> selectParameters(String query) {
        if (query == null || query.length() > 8192) throw invalid();
        var values = new LinkedHashMap<String,String>();
        try {
            for (String pair : query.split("&", -1)) {
                String[] parts = pair.split("=", 2);
                if (parts.length != 2 || !parts[0].matches("[A-Za-z_]+")) throw invalid();
                String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                if (value.length() > 600 || value.indexOf('\ufffd') >= 0 || value.codePoints().anyMatch(Character::isISOControl)
                        || values.putIfAbsent(parts[0], value) != null) throw invalid();
            }
        } catch (IllegalArgumentException exception) { throw invalid(); }
        return values;
    }

    public static Element selectTable(Document document) {
        var tables = document.select("table.bbs_default.view");
        if (tables.size() != 1) throw invalid();
        Element table = tables.getFirst();
        if (selectCell(table, "제목").text().isBlank()) throw invalid();
        Element content = selectCell(table, "내용");
        if (!"내용".equals(content.attr("title")) || !content.hasClass("bbs_content")) throw invalid();
        return table;
    }

    public static Element selectCell(Element table, String label) {
        var labels = table.select("th").stream().filter(e -> e.closest("table") == table && label.equals(e.text().strip())).toList();
        if (labels.size() != 1) throw invalid();
        Element heading = labels.getFirst(), cell = heading.nextElementSibling();
        if (!"tr".equals(heading.parent().tagName()) || cell == null || !"td".equals(cell.tagName())
                || cell.nextElementSibling() != null) throw invalid();
        return cell;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("CHUNGJU_NOTICE_STRUCTURE_CHANGED"); }
}
