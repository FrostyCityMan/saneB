package com.saneb.domain.announcementattachment.classification;

/** 구간 설정 없는 기존 작업은 기존 엔진만 사용한다. 이름만 바꾼 작업 또는 불완전한 버전 결합은 거부한다. */
public final class AttachmentEngineContract {
    private AttachmentEngineContract() { }
    public static boolean selectCurrent(String engineVersion, String segmentVersion, String segmentHash) {
        return AnnouncementAttachmentClassificationEngine.VERSION.equals(engineVersion) && segmentVersion == null && segmentHash == null
                || AttachmentSegmentClassificationEngine.VERSION.equals(engineVersion)
                    && selectSegmentCurrent(segmentVersion, segmentHash);
    }
    /** 실행 가능한 명시적 버전만 허용한다. 진단용 1.0.1 및 알려지지 않은 지문은 승격하지 않는다. */
    public static boolean selectSegmentCurrent(String version, String hash) {
        String expected = selectSegmentRulesHash(version);
        return expected != null && expected.equals(hash);
    }
    public static String selectSegmentRulesHash(String version) {
        if (AttachmentSegmentRoleAnalyzer.VERSION.equals(version)) return AttachmentSegmentRoleAnalyzer.RULES_HASH;
        if (AttachmentSegmentRoleAnalyzer.QUARTER_VERSION.equals(version)) return AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH;
        if (AttachmentSegmentRoleAnalyzer.STRUCTURAL_VERSION.equals(version)) return AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH;
        return null;
    }
}
