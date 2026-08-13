package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TermTypeCode;
import java.util.Objects;

/**
 * 대표 키워드 또는 유의어 한 건입니다.
 */
public record AnnouncementSourceClassificationTerm(
        TermTypeCode termTypeCode,
        String termText,
        MatchModeCode matchModeCode,
        boolean classificationTerm,
        boolean enabled
) {

    public AnnouncementSourceClassificationTerm {
        Objects.requireNonNull(termTypeCode, "termTypeCode is required");
        Objects.requireNonNull(matchModeCode, "matchModeCode is required");
        if (termText == null || termText.isBlank()) {
            throw new IllegalArgumentException("termText is required");
        }
        termText = termText.trim();
    }

    public static AnnouncementSourceClassificationTerm canonical(
            String termText,
            MatchModeCode matchModeCode
    ) {
        return new AnnouncementSourceClassificationTerm(
                TermTypeCode.CANONICAL,
                termText,
                matchModeCode,
                true,
                true
        );
    }

    public static AnnouncementSourceClassificationTerm synonym(
            String termText,
            MatchModeCode matchModeCode
    ) {
        return new AnnouncementSourceClassificationTerm(
                TermTypeCode.SYNONYM,
                termText,
                matchModeCode,
                true,
                true
        );
    }
}
