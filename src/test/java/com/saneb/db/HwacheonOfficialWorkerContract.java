package com.saneb.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/** 32258의 사전 다운로드 지문만 고정한다. 미관측 텍스트·역할을 정답으로 만들지 않는다. */
final class HwacheonOfficialWorkerContract {
    static final String CASE="HWACHEON-32258";
    static final String BINARY="dbeba265406d420c2a396e6f7d40368f499ab5ec25bc510004e4f27b964d3ea0";
    static final String LOCATOR="5ea8a8ffafeb7e36cd0a438eff350f6cfc6bc31b2ac1369ab0316ce0627baf88";
    static final String PROFILE="4e34a0852383aad7ab5c20635340c845acc0bc43683df3d7fdf6d44429271e84";
    static final List<String> FILE_FLAGS=List.of("pinnedInputAndCoverageVerified","segmentEvaluationInputBound",
            "segmentApiProjectionMatched","legacyDefaultReadOnlyVerified","evaluationBoundApiVerified","otherVersionReadOnlyVerified");
    private HwacheonOfficialWorkerContract() {}
    static void validateBinary(long bytes,String hash) throws IOException {
        if(bytes!=82944||!BINARY.equals(hash))throw new IOException("HWACHEON_FIXED_FILE_CHANGED");
    }
    static boolean selectComplete(JsonNode report) {
        if(!CASE.equals(report.path("caseCode").asText())
                ||!"OFFICIAL_WORKER_EPHEMERAL_DB_API_V1".equals(report.path("scope").asText())
                ||!"WORKER_DB_API_OBSERVED_NOT_APPROVED".equals(report.path("status").asText())
                ||!"LOCAL_HWACHEON_POST_V1".equals(report.path("profileCode").asText())
                ||!PROFILE.equals(report.path("profileHash").asText())
                ||!"attachment-segment-1.0.0".equals(report.path("engineVersion").asText())
                ||!AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION.equals(report.path("segmentRuleVersion").asText())
                ||!AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH.equals(report.path("segmentRulesHash").asText())
                ||!"1.0.15".equals(report.path("extractorVersion").asText())
                ||!"EVALUATED".equals(report.path("workerStatus").asText())
                ||!"COMBINATION_MATCHED".equals(report.path("titleStage").asText())
                ||!"AVAILABLE".equals(report.path("bodyStatus").asText())
                ||!Set.of("ACCEPTED","REVIEW_REQUIRED").contains(report.path("decisionStatus").asText()))return false;
        for(String key:List.of("isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e"))if(!bool(report,key,false))return false;
        for(String key:List.of("bodyStageComplete","discoveryComplete","originalFilesRemoved","segmentDatabaseApiVerified",
                "segmentReviewContextVerified","requiresFinalAdminVerification"))if(!bool(report,key,true))return false;
        if(!number(report,"productionWriteCount",0,0)||!number(report,"remainingResourceLeases",0,0)
                ||!number(report,"discoveredFileCount",1,1)||!number(report,"processedFileCount",1,1)||!number(report,"extractorCalls",1,1)
                ||!number(report,"maximumRequestReservations",5,5)||!number(report,"maximumReservedBytes",25165824,25165824)
                ||!number(report,"requestReservationsIncludingBodyUpperBound",4,5)||!number(report,"reservedBytesIncludingBodyUpperBound",1,25165824)
                ||!report.path("files").isArray()||report.path("files").size()!=1)return false;
        var file=report.path("files").get(0);String quality=file.path("quality").asText();
        if(!"HWP".equals(file.path("format").asText())||!"SUCCEEDED".equals(file.path("downloadStatus").asText())
                ||!BINARY.equals(file.path("binaryHash").asText())||!LOCATOR.equals(file.path("locatorHash").asText())
                ||!number(file,"bytes",82944,82944)||!Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(quality)
                ||!number(file,"characterCount",1,1000000)||!number(file,"blockCount",1,10000)
                ||!hash(file,"textHash")||!hash(file,"segmentAnalysisHash")
                ||!number(file,"segmentCount",1,200)||!number(file,"unknownSegmentCount",0,file.path("segmentCount").asLong())
                ||!number(file,"noticeSegmentCount",0,file.path("segmentCount").asLong())
                ||file.path("unknownSegmentCount").asLong()+file.path("noticeSegmentCount").asLong()>file.path("segmentCount").asLong())return false;
        for(String key:FILE_FLAGS)if(!bool(file,key,true))return false;
        for(String key:List.of("longFormObservedHashMatched","longFormCandidate","structuralCandidate"))if(file.has(key))return false;
        boolean partial="PARTIAL_TEXT".equals(quality);
        if(!bool(report,"isWholeTextAnalysisComplete",!partial)||!report.path("manualSourceCheckRequired").isBoolean())return false;
        if(partial&&(!number(file,"segmentCount",1,1)||!number(file,"unknownSegmentCount",1,1)||!number(file,"noticeSegmentCount",0,0)
                ||!bool(file,"partialFullCoverageVerified",true)||!"COMPLETE_TEXT_REQUIRED".equals(file.path("segmentReason").asText())))return false;
        if((partial||file.path("unknownSegmentCount").asLong()>0)
                &&(!bool(report,"manualSourceCheckRequired",true)||!"REVIEW_REQUIRED".equals(report.path("decisionStatus").asText())))return false;
        return true;
    }
    private static boolean hash(JsonNode node,String key){return node.path(key).isTextual()&&node.path(key).asText().matches("[a-f0-9]{64}");}
    private static boolean bool(JsonNode node,String key,boolean value){return node.path(key).isBoolean()&&node.path(key).booleanValue()==value;}
    private static boolean number(JsonNode node,String key,long min,long max){var n=node.path(key);return n.isIntegralNumber()&&n.canConvertToLong()&&n.longValue()>=min&&n.longValue()<=max;}
}
