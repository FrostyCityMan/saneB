package com.saneb.domain.announcementsource.provider.content;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 울산 남구 공식 새올의 실측 div/ul 상세 구조. 메뉴·담당 정보는 본문에 포함하지 않는다. */
public final class UlsanNamguNoticePage {
    private UlsanNamguNoticePage() { }

    public static Element selectRoot(Document page) {
        var forms = page.select("form");
        if (forms.size()!=1 || !"form1".equals(forms.getFirst().attr("name"))
                || !"post".equalsIgnoreCase(forms.getFirst().attr("method"))) throw invalid();
        var roots = forms.getFirst().select(":root > div.bbs_detail.bbs_detail_basic");
        if (roots.size()!=1) throw invalid();
        selectTitle(roots.getFirst());
        return roots.getFirst();
    }

    public static Element selectTitle(Element root) {
        var titles = root.select(":root > div.bbs_detail_tit > h2");
        if (titles.size()!=1 || titles.getFirst().text().isBlank() || !titles.getFirst().children().isEmpty()) throw invalid();
        return titles.getFirst();
    }

    public static Element selectContent(Element root) {
        var bodies = root.select(":root > div.bbs-view-content");
        if (bodies.size()!=1) throw invalid();
        return bodies.getFirst();
    }

    public static Element selectAttachments(Element root) {
        var lists = root.select(":root > ul.bbs_detail_content2");
        if (lists.size()!=2 || !lists.get(1).select("a,[href],[onclick],script,iframe,object,embed").isEmpty()) throw invalid();
        if (lists.getFirst().attributes().asList().stream().anyMatch(attribute ->
                attribute.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on"))) throw invalid();
        // 실측한 첫 목록 전체를 전달한다. 미해석 링크를 지워서 완전 발견으로 바꾸지 않는다.
        // 실제 '첨부 없음' 구조는 아직 관측하지 않았으므로 빈 목록도 정상 부재로 확정하지 않는다.
        if (lists.getFirst().select("a").isEmpty()) throw invalid();
        return lists.getFirst();
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("ATTACHMENT_SELECTOR_CHANGED"); }
}
