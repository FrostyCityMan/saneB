package com.saneb.domain.announcementattachment.vo;

import java.util.List;
import java.util.Map;

/** worker 내부 결과입니다. 임시 URL·로컬 파일 경로·인증정보는 저장 계약에 포함하지 않습니다. */
public record AttachmentSetEvidence(String discoveryStatus, boolean discoveryComplete, List<File> files, List<String> warningCodes) {
    public AttachmentSetEvidence { files = List.copyOf(files); warningCodes = List.copyOf(warningCodes); }
    public AttachmentSetEvidence(String discoveryStatus, boolean discoveryComplete, List<File> files) {
        this(discoveryStatus,discoveryComplete,files,List.of());
    }

    public record Locator(String profileCode, String path, Map<String, String> identifiers) {
        public Locator { identifiers = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(identifiers)); }
    }
    public record File(Locator locator, String displayName, String detectedType, String role, String roleOrigin,
                       String downloadStatus, long downloadedBytes, String binaryHash,
                       AttachmentFailureCode failureCode, Extraction extraction,
                       @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                       com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment roleAssessment) {
        public File(Locator locator,String displayName,String detectedType,String role,String roleOrigin,String downloadStatus,
                long downloadedBytes,String binaryHash,AttachmentFailureCode failureCode,Extraction extraction) {
            this(locator,displayName,detectedType,role,roleOrigin,downloadStatus,downloadedBytes,binaryHash,failureCode,extraction,null);
        }
    }
    public record Extraction(String quality, String text, List<Block> blocks, Integer pageCount, int durationMs,
                             Long completedAtEpochMs) {
        public Extraction { blocks = List.copyOf(blocks); }
        public Extraction(String quality,String text,List<Block> blocks,Integer pageCount,int durationMs) {
            this(quality,text,blocks,pageCount,durationMs,null);
        }
    }
    public record Block(int index, int startOffset, int endOffset, String evidenceScopeId,
                        boolean scopeReliable, String locator) { }
}
