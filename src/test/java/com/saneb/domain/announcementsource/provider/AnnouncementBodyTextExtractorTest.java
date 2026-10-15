package com.saneb.domain.announcementsource.provider;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AnnouncementBodyTextExtractorTest {
    @Test void classificationNormalizationRetainsEquivalentTermsAndOriginalText() {
        String body = AnnouncementBodyTextExtractor.selectText("<p>소상공인 지원금</p><div>신청 기간</div>");
        var normalizer = new com.saneb.domain.announcementsource.classification.AnnouncementSourceTextNormalizer();
        assertThat(normalizer.selectNormalizedText(body).normalizedText())
                .isEqualTo(normalizer.selectNormalizedText("소상공인 지원금 신청 기간").normalizedText());
    }
    @Test void preservesParagraphsExplicitBreaksAndInlineWords() {
        assertThat(AnnouncementBodyTextExtractor.selectText(
                "<p>1. 관련: <strong>지원</strong>사업</p><p>2. 모집내용<br>가. 대상: 소상공인<br/>나. 기간: 10월</p>"))
                .isEqualTo("1. 관련: 지원사업\n\n2. 모집내용\n가. 대상: 소상공인\n나. 기간: 10월");
    }
    @Test void preservesPlainTextAndNormalizesNewlineEncodings() {
        assertThat(AnnouncementBodyTextExtractor.selectText("첫 줄\r\n둘째 줄\r셋째 줄\n\n마지막 줄"))
                .isEqualTo("첫 줄\n둘째 줄\n셋째 줄\n\n마지막 줄");
    }
    @Test void separatesListsDivisionsAndTableRows() {
        assertThat(AnnouncementBodyTextExtractor.selectText(
                "<div>지원 안내</div><ul><li>대상</li><li>기간</li></ul><table><tr><td>지역</td><td>금액</td></tr><tr><td>서울</td><td>100만원</td></tr></table>"))
                .isEqualTo("지원 안내\n대상\n기간\n지역 금액\n서울 100만원");
    }
    @Test void removesExecutableContentDecodesEntitiesAndBoundsEmptyLines() {
        assertThat(AnnouncementBodyTextExtractor.selectText(
                "<script>수출</script><style>투자</style><p>사업자&nbsp; &amp; 지원</p><br><br><br><div>다음 &lt;안내&gt;</div>"))
                .isEqualTo("사업자 & 지원\n\n다음 <안내>");
    }
    @Test void leavesMissingOrEmptyBodyEmpty() {
        assertThat(AnnouncementBodyTextExtractor.selectText((String) null)).isNull();
        assertThat(AnnouncementBodyTextExtractor.selectText("<div><br> </div>")).isEmpty();
    }
}
