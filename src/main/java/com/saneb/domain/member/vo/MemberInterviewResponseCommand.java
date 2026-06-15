package com.saneb.domain.member.vo;

import java.util.UUID;

public record MemberInterviewResponseCommand(
        UUID memberUserId,
        String questionCode,
        String answerCode,
        String note,
        UUID actorUserId
) {
}
