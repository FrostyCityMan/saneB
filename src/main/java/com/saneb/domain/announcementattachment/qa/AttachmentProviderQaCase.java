package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.util.List;

/** 서버 소유 catalog/원장에서만 구성할 실행 입력. HTTP 요청 DTO 또는 성공 결과 업로드 계약이 아니다. */
public record AttachmentProviderQaCase(String caseId, String profileCode, String profileHash, Source source,
        String title, AnnouncementSourceClassificationRuleSet rules, String runtimeHash,
        String discoveryStatus, boolean discoveryComplete, List<ExpectedFile> files, Limits limits,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String engineVersion) {
    public AttachmentProviderQaCase { files = List.copyOf(files); }
    public AttachmentProviderQaCase(String caseId,String profileCode,String profileHash,Source source,String title,
            AnnouncementSourceClassificationRuleSet rules,String runtimeHash,String discoveryStatus,boolean discoveryComplete,List<ExpectedFile> files,Limits limits) {
        this(caseId,profileCode,profileHash,source,title,rules,runtimeHash,discoveryStatus,discoveryComplete,files,limits,null);
    }
    @Override public String toString() { return "AttachmentProviderQaCase[input=REDACTED]"; }

    public record ExpectedFile(String locatorHash, boolean downloadAllowed, String format, String binaryHash,
            String quality, int minimumCharacters, int minimumBlocks, List<String> requiredPhrases,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            RoleExpectation roleExpectation,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            SegmentExpectation segmentExpectation) {
        public ExpectedFile { requiredPhrases = List.copyOf(requiredPhrases); }
        public ExpectedFile(String locatorHash,boolean downloadAllowed,String format,String binaryHash,String quality,
                int minimumCharacters,int minimumBlocks,List<String> requiredPhrases,RoleExpectation roleExpectation) {
            this(locatorHash,downloadAllowed,format,binaryHash,quality,minimumCharacters,minimumBlocks,requiredPhrases,roleExpectation,null);
        }
        public ExpectedFile(String locatorHash,boolean downloadAllowed,String format,String binaryHash,String quality,
                int minimumCharacters,int minimumBlocks,List<String> requiredPhrases) {
            this(locatorHash,downloadAllowed,format,binaryHash,quality,minimumCharacters,minimumBlocks,requiredPhrases,null);
        }
        @Override public String toString() { return "ExpectedFile[expectation=REDACTED]"; }
    }
    /** 사전 검토한 실제 파일의 역할·텍스트·전체 위치 근거를 고정한다. 실행 결과로 기대값을 자동 작성하지 않는다. */
    public record RoleExpectation(String ruleVersion,String rulesHash,String roleCode,String reasonCode,
                                  String textHash,String blocksHash,String assessmentHash) { }
    /** 사전 검토된 전체 구간 분석 지문과 순서별 역할. 원문/구간 위치는 해시에 결합하며 결과 업로드로 만들지 않는다. */
    public record SegmentExpectation(String analysisVersion,String rulesHash,String textHash,String blocksHash,
                                     String analysisHash,String statusCode,List<String> roleCodes) {
        public SegmentExpectation {roleCodes=List.copyOf(roleCodes);}
    }
    public record Limits(int maximumSeconds, int maximumRequestReservations, long maximumReservedBytes) { }
}
