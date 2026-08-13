package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import java.util.List;
import java.util.Objects;

/**
 * Provider 공통 분류 입력입니다. 첨부파일은 의도적으로 포함하지 않습니다.
 */
public record AnnouncementSourceClassificationInput(
        String providerCode,
        String title,
        String bodyText,
        String agencyName,
        List<String> agencyAliases,
        BodySourceCode bodySourceCode,
        BodyAvailabilityCode bodyAvailabilityCode
) {

    public AnnouncementSourceClassificationInput {
        if (providerCode == null || providerCode.isBlank()) {
            throw new IllegalArgumentException("providerCode is required");
        }
        title = Objects.requireNonNullElse(title, "");
        agencyAliases = agencyAliases == null
                ? List.of()
                : agencyAliases.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
        bodySourceCode = Objects.requireNonNullElse(bodySourceCode, BodySourceCode.NONE);
        bodyAvailabilityCode = Objects.requireNonNullElse(bodyAvailabilityCode, BodyAvailabilityCode.UNAVAILABLE);
    }
}
