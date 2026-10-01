package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.ReasonCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/** 공식 목록에서 발견한 링크가 있어도 DRAFT 제목 정책을 우회하여 파일을 받지 않는다. */
class EunpyeongFreshTitleContractTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "2026년 가정용 음식물류 폐기물 소형감량기 구매 지원사업 시행 공고",
            "1인가구 전입 생활 지원 '은빛SOL라이프' 사업 공고(2차)"
    })
    void attachmentLinksDoNotSubstituteForEligibleTargetAndSupportCombination(String title) throws Exception {
        var result = new AnnouncementSourceClassificationEngine().selectDecision(
                new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE", title, null, null,
                        List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE),
                AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());

        assertThat(result.titleStageCode()).isEqualTo(TitleStageCode.COMBINATION_NOT_MATCHED);
        assertThat(result.reasonCode()).isEqualTo(ReasonCode.TITLE_COMBINATION_NOT_MATCHED);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result))
                .as("현재 DRAFT seed의 제목 판정만 검증하며 운영 규칙으로 일반화하지 않는다")
                .isFalse();
    }
}
