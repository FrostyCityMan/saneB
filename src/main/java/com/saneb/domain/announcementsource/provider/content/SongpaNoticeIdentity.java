package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 제목이 비어 있는 송파 상세의 공고번호를 검증한다. 목록 제목을 상세 제목으로 주입하지 않는다. */
public final class SongpaNoticeIdentity {
    private SongpaNoticeIdentity() { }

    public static void validateDetail(Document page, Element root) {
        URI detail = SeoulFourthNoticePage.selectDetailUri(SeoulFourthNoticePage.Site.SONGPA, URI.create(page.location()));
        String id = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no");
        var forms = page.select("div.p-wrap.bbs.bbs__view > form[name=gosiFrm]");
        if (forms.size() != 1 || root.parent() != forms.getFirst()) throw invalid();
        var fields = forms.getFirst().select("input[name=not_ancmt_mgt_no]");
        if (fields.size() != 1 || !"hidden".equalsIgnoreCase(fields.getFirst().attr("type"))
                || !id.equals(fields.getFirst().val())) throw invalid();
    }

    public static URI selectListUri(String title) {
        if (title == null || title.isBlank() || title.length() > 500) throw invalid();
        return URI.create("https://www.songpa.go.kr/www/selectGosiList.do?key=2776&searchCnd=SJ&searchKrwd="
                + URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    public static void validateListTitle(Document list, URI detail, String expected) {
        if (!selectListUri(expected).toASCIIString().equals(list.location())) throw invalid();
        URI canonical = SeoulFourthNoticePage.selectDetailUri(SeoulFourthNoticePage.Site.SONGPA, detail);
        int matches = 0;
        for (Element link : list.select("td.p-subject > a[href]")) {
            URI candidate;
            try {
                candidate = SeoulFourthNoticePage.selectDetailUri(SeoulFourthNoticePage.Site.SONGPA,
                        URI.create(list.location()).resolve(link.attr("href").replace(" ", "%20")));
            } catch (IllegalArgumentException exception) { continue; }
            if (!canonical.equals(candidate)) continue;
            if (!normalize(expected).equals(normalize(link.text()))) throw invalid();
            matches++;
        }
        if (matches != 1) throw invalid();
    }

    private static String normalize(String value) { return value.replace('\u00a0', ' ').strip().replaceAll("\\s+", " "); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("SONGPA_NOTICE_IDENTITY_UNCONFIRMED"); }
}
