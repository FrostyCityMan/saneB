package com.saneb.domain.announcementattachment.classification;

/** 구간 설정 없는 기존 작업은 기존 엔진만 사용한다. 이름만 바꾼 작업 또는 불완전한 버전 결합은 거부한다. */
public final class AttachmentEngineContract {
    private AttachmentEngineContract() { }
    public static boolean selectCurrent(String engineVersion, String segmentVersion, String segmentHash) {
        return AnnouncementAttachmentClassificationEngine.VERSION.equals(engineVersion) && segmentVersion == null && segmentHash == null
                || AttachmentSegmentClassificationEngine.VERSION.equals(engineVersion)
                    && AttachmentSegmentRoleAnalyzer.VERSION.equals(segmentVersion) && AttachmentSegmentRoleAnalyzer.RULES_HASH.equals(segmentHash);
    }
}
