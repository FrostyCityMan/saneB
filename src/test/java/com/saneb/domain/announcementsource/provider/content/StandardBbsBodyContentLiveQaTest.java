package com.saneb.domain.announcementsource.provider.content;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 기존 공개 지원사업 표본의 본문만 확인한다. 파일 요청·DB 저장·운영 변경은 없다. */
@EnabledIfEnvironmentVariable(named = "SANEB_ATTACHMENT_PROFILE_QA", matches = "true")
class StandardBbsBodyContentLiveQaTest {
    record Sample(String host, String board, String menu, String notice) { }
    static Stream<Sample> selectCases() {
        return Stream.of(new Sample("www.taebaek.go.kr", "25", "352", "184816"),
                new Sample("www.hsg.go.kr", "65", "821", "424078"),
                new Sample("www.yw.go.kr", "17", "273", "157016"));
    }
    static Stream<Sample> selectWonjuCases() {
        return Stream.of(new Sample("www.wonju.go.kr", "140", "216", "491704"),
                new Sample("www.wonju.go.kr", "140", "216", "491507"), new Sample("www.wonju.go.kr", "140", "216", "491340"));
    }
    @ParameterizedTest(name = "원주 본문 표본 {index}") @MethodSource("selectWonjuCases") @Timeout(30)
    void readsWonjuBodyWithoutRequestingFiles(Sample site) {
        readsMeasuredOfficialBodyWithoutRequestingFiles(site);
    }
    static Stream<Sample> selectJecheonCases() {
        return Stream.of(new Sample("www.jecheon.go.kr", "18", "5233", "403587"),
                new Sample("www.jecheon.go.kr", "18", "5233", "403530"), new Sample("www.jecheon.go.kr", "18", "5233", "403490"));
    }
    @ParameterizedTest(name = "제천 본문 표본 {index}") @MethodSource("selectJecheonCases") @Timeout(30)
    void readsJecheonBodyWithoutRequestingFiles(Sample site) {
        readsMeasuredOfficialBodyWithoutRequestingFiles(site);
    }
    @ParameterizedTest(name = "공식 BBS 본문 표본 {index}") @MethodSource("selectCases") @Timeout(30)
    void readsMeasuredOfficialBodyWithoutRequestingFiles(Sample site) {
        var client = new LocalGovernmentNoticeProviderContentClient(true, 3000, 7000, 2 * 1024 * 1024, 3, 1, "saneB-notice-collector/1.0");
        String base = "https://" + site.host() + "/www/";
        var result = client.selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE", UUID.fromString("77000000-0000-0000-0000-000000000001"),
                base + "selectBbsNttList.do?bbsNo=" + site.board() + "&key=" + site.menu(),
                base + "selectBbsNttView.do?bbsNo=" + site.board() + "&key=" + site.menu() + "&nttNo=" + site.notice()
                        + (site.host().equals("www.jecheon.go.kr") ? "&id=" : "")));
        // 실패 시 원문·URL을 assertion 보고서에 복사하지 않는다.
        assertTrue(result.statusCode() == ProviderContentCodes.StatusCode.AVAILABLE,
                () -> "OFFICIAL_BODY_UNAVAILABLE:" + result.failureCode());
        assertTrue(result.bodyText() != null && !result.bodyText().isBlank(), "OFFICIAL_BODY_EMPTY");
        assertTrue(result.bodyText().contains("지원"), "OFFICIAL_SUPPORT_BODY_CHANGED");
        assertEquals(1, result.attemptCount(), "UNEXPECTED_RETRY");
        assertEquals(0, result.redirectCount(), "UNEXPECTED_DETAIL_REDIRECT");
    }
}
