package com.saneb.domain.announcementattachment.vo;

/** 서버가 검증한 프로필·산출물의 식별자만 저장한다. URL/헤더/인증값은 허용하지 않는다. */
public record AttachmentExecutionSnapshot(
        String profileCode, String profileHash, String engineVersion,
        String extractorVersion, String extractorConfigHash
) {
    public AttachmentExecutionSnapshot {
        if (profileCode == null || !profileCode.matches("[A-Z0-9_]{1,80}")
                || !isHash(profileHash) || !isHash(extractorConfigHash)
                || !isVersion(engineVersion) || !isVersion(extractorVersion)) {
            throw new IllegalArgumentException("첨부 실행에는 검증된 프로필 코드·SHA-256·엔진/추출기 버전이 필요합니다.");
        }
    }

    private static boolean isHash(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static boolean isVersion(String value) {
        return value != null && value.matches("[A-Za-z0-9_.-]{1,40}");
    }
}
