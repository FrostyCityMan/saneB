package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.util.List;

/** 서버 소유 catalog/원장에서만 구성할 실행 입력. HTTP 요청 DTO 또는 성공 결과 업로드 계약이 아니다. */
public record AttachmentProviderQaCase(String caseId, String profileCode, String profileHash, Source source,
        String title, AnnouncementSourceClassificationRuleSet rules, String runtimeHash,
        String discoveryStatus, boolean discoveryComplete, List<ExpectedFile> files, Limits limits) {
    public AttachmentProviderQaCase { files = List.copyOf(files); }
    @Override public String toString() { return "AttachmentProviderQaCase[input=REDACTED]"; }

    public record ExpectedFile(String locatorHash, boolean downloadAllowed, String format, String binaryHash,
            String quality, int minimumCharacters, int minimumBlocks, List<String> requiredPhrases,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            RoleExpectation roleExpectation) {
        public ExpectedFile { requiredPhrases = List.copyOf(requiredPhrases); }
        public ExpectedFile(String locatorHash,boolean downloadAllowed,String format,String binaryHash,String quality,
                int minimumCharacters,int minimumBlocks,List<String> requiredPhrases) {
            this(locatorHash,downloadAllowed,format,binaryHash,quality,minimumCharacters,minimumBlocks,requiredPhrases,null);
        }
        @Override public String toString() { return "ExpectedFile[expectation=REDACTED]"; }
    }
    /** 사전 검토한 실제 파일의 역할·텍스트·전체 위치 근거를 고정한다. 실행 결과로 기대값을 자동 작성하지 않는다. */
    public record RoleExpectation(String ruleVersion,String rulesHash,String roleCode,String reasonCode,
                                  String textHash,String blocksHash,String assessmentHash) { }
    public record Limits(int maximumSeconds, int maximumRequestReservations, long maximumReservedBytes) { }
}
