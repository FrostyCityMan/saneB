package com.saneb.domain.announcementsource.provider.content;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 실측 상세 구조의 실제 본문 HTTP smoke. 정상 후보·첨부 판정·DB 저장 성공을 선언하지 않는다. */
@EnabledIfEnvironmentVariable(named = "SANEB_ATTACHMENT_PROFILE_QA", matches = "true")
class MeasuredBodyContentLiveQaTest {
    record Sample(String code, String detailUrl) { @Override public String toString() { return code; } }
    static Stream<Sample> selectCases() {
        String path = "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y"
                + "&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&subCheck=Y&not_ancmt_mgt_no=";
        return Stream.of(new Sample("NAMGU-46034", "https://eminwon.bsnamgu.go.kr" + path + "46034"),
                new Sample("DALSEONG-53932", "https://eminwon.dalseong.daegu.kr" + path + "53932"),
                new Sample("JUNGGU-34295", "https://eminwon.jung.daegu.kr" + path + "34295"),
                new Sample("HAMAN-43065", "https://eminwon.haman.go.kr" + path + "43065"),
                new Sample("SEOGU-51668", "https://www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do?notAncmtMgtNo=51668"),
                new Sample("BUSAN-79571", "https://www.busan.go.kr/nbgosi/view?sno=79571&gosiGbn=A&curPage=1"),
                new Sample("BUSAN-79570", "https://www.busan.go.kr/nbgosi/view?sno=79570&gosiGbn=A&curPage=1"),
                new Sample("GANGBUK-184761", "https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId=184761"),
                new Sample("GANGBUK-184744", "https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId=184744"),
                new Sample("HWACHEON-33897", "https://eminwon.ihc.go.kr" + path.replace("subCheck=Y", "subCheck=N") + "33897"),
                new Sample("HWACHEON-33895", "https://eminwon.ihc.go.kr" + path.replace("subCheck=Y", "subCheck=N") + "33895"));
    }
    @ParameterizedTest(name = "공식 본문 구조 {0}") @MethodSource("selectCases") @Timeout(30)
    void readsOnlyMeasuredBodyThroughProductionPinnedTransport(Sample sample) {
        var client = new LocalGovernmentNoticeProviderContentClient(true, 3000, 7000, 2 * 1024 * 1024, 0, 1, "saneB-notice-collector/1.0");
        var result = client.selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",
                UUID.fromString("77000000-0000-0000-0000-000000000001"), sample.detailUrl(), sample.detailUrl()));
        assertTrue(result.statusCode() == ProviderContentCodes.StatusCode.AVAILABLE,
                () -> "OFFICIAL_BODY_UNAVAILABLE:" + result.failureCode());
        assertTrue(result.bodyText() != null && !result.bodyText().isBlank(), "OFFICIAL_BODY_EMPTY");
        assertEquals(1, result.attemptCount(), "UNEXPECTED_RETRY");
        assertEquals(0, result.redirectCount(), "UNEXPECTED_DETAIL_REDIRECT");
        // 원문·URL·개인정보는 assertion 메시지와 산출물에 복사하지 않는다.
    }

    static Stream<Sample> selectDalseongSupportCases() {
        return com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("DALSEONG")
                .map(sample -> new Sample(sample.code(), sample.source().sourceUrl()));
    }
    @ParameterizedTest(name = "달성 지원사업 본문 {0}") @MethodSource("selectDalseongSupportCases") @Timeout(30)
    void readsDalseongSupportBodyThroughProductionPinnedTransport(Sample sample) {
        readsOnlyMeasuredBodyThroughProductionPinnedTransport(sample);
    }

    static Stream<Sample> selectHamanSupportCases() {
        return com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("HAMAN")
                .map(sample -> new Sample(sample.code(),sample.source().sourceUrl()));
    }
    @ParameterizedTest(name = "함안 지원사업 본문 {0}") @MethodSource("selectHamanSupportCases") @Timeout(30)
    void readsHamanSupportBodyThroughProductionPinnedTransport(Sample sample) {
        readsOnlyMeasuredBodyThroughProductionPinnedTransport(sample);
    }
}
