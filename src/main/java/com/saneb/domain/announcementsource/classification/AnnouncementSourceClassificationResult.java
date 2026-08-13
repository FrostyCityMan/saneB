package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyStageCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.ReasonCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SemanticStatusCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode;
import java.util.List;

/**
 * Provider와 무관한 공통 공고 분류 결과입니다.
 */
public record AnnouncementSourceClassificationResult(
        String providerCode,
        String ruleReleaseCode,
        SemanticStatusCode semanticStatusCode,
        ReasonCode reasonCode,
        TitleStageCode titleStageCode,
        BodyStageCode bodyStageCode,
        BodySourceCode bodySourceCode,
        BodyAvailabilityCode bodyAvailabilityCode,
        List<TargetCategoryCode> targetCategoryCodes,
        List<SupportTypeCode> supportTypeCodes,
        List<String> groupACodes,
        List<String> groupBCodes,
        List<AnnouncementSourceClassificationMatch> matches
) {

    public AnnouncementSourceClassificationResult {
        targetCategoryCodes = List.copyOf(targetCategoryCodes);
        supportTypeCodes = List.copyOf(supportTypeCodes);
        groupACodes = List.copyOf(groupACodes);
        groupBCodes = List.copyOf(groupBCodes);
        matches = List.copyOf(matches);
    }
}
