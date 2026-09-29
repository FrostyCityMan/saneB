package com.saneb.domain.announcementattachment.discovery;

/** 실측한 시스템 프로필의 상세 용량 예외. 기존 프로필 계약/지문과 1MiB 기본값은 보존한다. */
public interface AttachmentDetailLimitProfile {
    long selectDetailMaximumBytes();
    static long selectBoundedDetailMaximumBytes(AttachmentDiscoveryProfile profile) {
        if (profile == null) throw new IllegalArgumentException("PROFILE_REQUIRED");
        long maximum = profile instanceof AttachmentDetailLimitProfile selected
                ? selected.selectDetailMaximumBytes() : 1024L * 1024;
        if (maximum != 1024L * 1024 && maximum != 2L * 1024 * 1024)
            throw new IllegalArgumentException("PROFILE_REQUIRED");
        return maximum;
    }
}
