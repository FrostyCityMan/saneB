package com.saneb.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** 동일 설치 코드에서 기존 고정 시험을 JUnit lifecycle/timeout 그대로 실행하는 일회성 도구. */
public final class AnnouncementAttachmentOfficialWorkerProbe {
    static final List<String> CASES=List.of("YANGPYEONG-312241","YANGPYEONG-311846","YANGPYEONG-311507");
    private static final Set<String> ENV=Set.of("PATH","LANG","HOME","TMPDIR","PWD","SANEB_ATTACHMENT_OFFICIAL_WORKER_QA");
    private AnnouncementAttachmentOfficialWorkerProbe() {}

    static List<String> selectCaseCodes(String group) {
        return switch(group) {
            case "YANGPYEONG" -> CASES;
            case "TAEBAEK" -> List.of("TAEBAEK-184816");
            case "TAEBAEK_HWP" -> List.of("TAEBAEK-176153");
            case "CHUNGJU" -> List.of("CHUNGJU-72625","CHUNGJU-72039","CHUNGJU-70852");
            case "JECHEON" -> List.of("JECHEON-403587","JECHEON-403530","JECHEON-403490");
            case "BOEUN", "BOEUN_SEGMENT", "BOEUN_STRUCTURAL" -> List.of("BOEUN-221499","BOEUN-221497","BOEUN-218812");
            case "BOEUN_LONG_FORM" -> List.of("BOEUN-221497");
            case "HAMAN_SEGMENT" -> List.of("HAMAN-41306");
            case "HAMAN_LAYOUT_SEGMENT" -> List.of("HAMAN-41306");
            case "JUNGGU_SEGMENT" -> List.of("JUNGGU-33626");
            default -> throw new IllegalArgumentException("OFFICIAL_WORKER_GROUP_INVALID");
        };
    }
    static boolean selectTitleStopExpected(String code) {
        boolean known=java.util.stream.Stream.of("YANGPYEONG","TAEBAEK","TAEBAEK_HWP","CHUNGJU","JECHEON","BOEUN","HAMAN_SEGMENT","JUNGGU_SEGMENT")
                .flatMap(group->selectCaseCodes(group).stream()).anyMatch(code::equals);
        if(!known)throw new IllegalArgumentException("OFFICIAL_WORKER_CASE_INVALID");
        return Set.of("YANGPYEONG-311507","CHUNGJU-72625","CHUNGJU-72039","JECHEON-403587").contains(code);
    }
    static int selectExpectedExtractionCount(String code) {
        if(selectTitleStopExpected(code))return 0;
        // 양평의 미지원 이미지도 발견 분모에 남지만 추출 호출 수에는 넣지 않는다.
        return Set.of("TAEBAEK-184816","JECHEON-403490","JUNGGU-33626").contains(code)?2:1;
    }
    static boolean selectComplete(long found,long succeeded,long failed,long skipped,long aborted,long containersFailed) {
        return selectComplete("YANGPYEONG",found,succeeded,failed,skipped,aborted,containersFailed);
    }
    static boolean selectComplete(String group,long found,long succeeded,long failed,long skipped,long aborted,long containersFailed) {
        int required=selectCaseCodes(group).size();
        return found==required&&succeeded==required&&failed==0&&skipped==0&&aborted==0&&containersFailed==0;
    }
    static boolean selectSegmentMode(String group) {
        selectCaseCodes(group);
        return Set.of("BOEUN_SEGMENT","BOEUN_STRUCTURAL","BOEUN_LONG_FORM","HAMAN_SEGMENT","HAMAN_LAYOUT_SEGMENT","JUNGGU_SEGMENT").contains(group);
    }
    static String selectSegmentVersion(String group) {
        if(!selectSegmentMode(group))throw new IllegalArgumentException("SEGMENT_MODE_REQUIRED");
        return Set.of("BOEUN_LONG_FORM","HAMAN_SEGMENT","HAMAN_LAYOUT_SEGMENT","JUNGGU_SEGMENT").contains(group)?"segment-role-1.0.4":"BOEUN_STRUCTURAL".equals(group)?"segment-role-1.0.3":"segment-role-1.0.2";
    }
    static String selectObservationGroup(String group) {
        if("JUNGGU_SEGMENT".equals(group))return "JUNGGU_PDF";
        if(Set.of("HAMAN_SEGMENT","HAMAN_LAYOUT_SEGMENT").contains(group))return "HAMAN";
        return selectSegmentMode(group) ? "BOEUN" : group;
    }
    static long selectMaximumRequests(String group) { return selectSegmentMode(group) ? 5 : 44; }
    static long selectMaximumBytes(String group) { return (selectSegmentMode(group) ? 24L : 80L)*1024*1024; }
    /** 명시한 새 검증에서 다른 추출기를 사용해 외부 요청을 소비하지 않는다. 기존 관측은 별도로 보존한다. */
    static void validatePinnedExtractor(String group,String version) {
        if("HAMAN_LAYOUT_SEGMENT".equals(group)&&!"1.0.15".equals(version))
            throw new IllegalArgumentException("HAMAN_LAYOUT_EXTRACTOR_VERSION_REQUIRED");
    }
    /** 9/24 고정 실파일의 메모리 1.0.2 관측 지문이다. Provider catalog 승인이나 운영 정책 QA를 대신하지 않는다. */
    static String selectQuarterObservedHash(String caseCode) {
        return switch(caseCode) {
            case "BOEUN-221499" -> "7b5436c08db86b5ff9fe0648b1d8c8eed01e29cb996c35ee53cba26ab3b583e2";
            case "BOEUN-221497" -> "381f7db64f91cda2e44c5f3ee2a4d2ed5e89ecaf11a0225b6bf8bff7f9e71c7e";
            case "BOEUN-218812" -> "b87ff442be093bfc05c95739a5a48b70623c1bb1256174a5b8cbb084bebbbc7d";
            default -> throw new IllegalArgumentException("QUARTER_OBSERVATION_CASE_INVALID");
        };
    }
    /** 실행5b81a5c5의 후보 메모리 대조 지문. 새 저장 실행의 성공 근거가 아니다. */
    static String selectStructuralObservedHash(String caseCode) {
        return switch(caseCode) {
            case "BOEUN-221499" -> "22f9d58ac8c6a8c76fe3508e58bf367358453b88684af5710437a5133e687827";
            case "BOEUN-221497" -> "3594adc138acae4211de819c3803295704410a8e8484a2070323042f9f3bda3a";
            case "BOEUN-218812" -> "f823ab78281f55cb16c634bde50bbc6fc84175c1058c90355c1845ea50aa3673";
            default -> throw new IllegalArgumentException("STRUCTURAL_OBSERVATION_CASE_INVALID");
        };
    }
    static boolean selectSegmentReportComplete(com.fasterxml.jackson.databind.JsonNode report) {
        return selectSegmentReportComplete("BOEUN_SEGMENT",report);
    }
    /** 실행4b3409d6의 메모리 비교 지문. 실제 worker 저장은 이 값과 별도로 검증한다. */
    static String selectLongFormObservedHash(String caseCode) {
        if(!"BOEUN-221497".equals(caseCode))throw new IllegalArgumentException("LONG_FORM_OBSERVATION_CASE_INVALID");
        return "9ba9e2ea3391599cb34de6b3dd8eeb394ef3a35d23954f52e16a6ac2061d1d9f";
    }
    static boolean selectSegmentReportComplete(String group,com.fasterxml.jackson.databind.JsonNode report) {
        if("JUNGGU_SEGMENT".equals(group))return selectJungguSegmentReportComplete(report);
        if("HAMAN_SEGMENT".equals(group))return selectHamanSegmentReportComplete(report);
        if("HAMAN_LAYOUT_SEGMENT".equals(group))return selectHamanSegmentReportComplete(report,true);
        String version=selectSegmentVersion(group);
        boolean structural="BOEUN_STRUCTURAL".equals(group);
        boolean longForm="BOEUN_LONG_FORM".equals(group);
        if (!selectCaseCodes(group).contains(report.path("caseCode").asText())
                || !"attachment-segment-1.0.0".equals(report.path("engineVersion").asText())
                || !version.equals(report.path("segmentRuleVersion").asText())
                || !com.saneb.domain.announcementattachment.classification.AttachmentEngineContract.selectSegmentRulesHash(version).equals(report.path("segmentRulesHash").asText())
                || !selectTrue(report,"segmentDatabaseApiVerified")
                || !report.path("segmentReviewContextVerified").isBoolean() || !report.path("segmentReviewContextVerified").booleanValue()
                || !report.path("manualSourceCheckRequired").isBoolean()
                || report.path("maximumRequestReservations").asLong(-1)!=5
                || report.path("maximumReservedBytes").asLong(-1)!=25165824L
                || !selectBounded(report,"requestReservationsIncludingBodyUpperBound",3,5)
                || !selectBounded(report,"reservedBytesIncludingBodyUpperBound",1,25165824L)
                || !report.path("files").isArray() || report.path("files").size()!=1) return false;
        var file=report.path("files").get(0);
        return "COMPLETE_TEXT".equals(file.path("quality").asText())
                && (longForm?selectLongFormObservedHash(report.path("caseCode").asText()):structural?selectStructuralObservedHash(report.path("caseCode").asText()):selectQuarterObservedHash(report.path("caseCode").asText())).equals(file.path("segmentAnalysisHash").asText())
                && selectBounded(file,"segmentCount",1,200)
                && selectBounded(file,"unknownSegmentCount",0,file.path("segmentCount").asLong())
                && (file.path("unknownSegmentCount").longValue()==0 || report.path("manualSourceCheckRequired").booleanValue())
                && selectTrue(file,"segmentEvaluationInputBound")
                && selectTrue(file,"segmentApiProjectionMatched")
                && file.path("legacyDefaultReadOnlyVerified").isBoolean() && file.path("legacyDefaultReadOnlyVerified").booleanValue()
                && selectBounded(file,"noticeSegmentCount",0,file.path("segmentCount").asLong())
                && (longForm?selectLongFormStorageComplete(report,file):structural
                    ? selectTrue(file,"structuralObservedHashMatched") && selectTrue(file,"evaluationBoundApiVerified")
                        && selectTrue(file,"otherVersionReadOnlyVerified") && !file.has("structuralCandidate")
                        && selectStructuralCounts(report,file)
                    : selectTrue(file,"quarterObservedHashMatched") && selectStructuralComparisonComplete(file.path("structuralCandidate")));
    }
    /** 부분 추출의 저장 경로 검증이다. 사전 승인된 정상 구간 기대값으로 표현하지 않는다. */
    static boolean selectHamanSegmentReportComplete(com.fasterxml.jackson.databind.JsonNode report) {
        return selectHamanSegmentReportComplete(report,false);
    }
    static boolean selectHamanSegmentReportComplete(com.fasterxml.jackson.databind.JsonNode report,boolean layout) {
        if(!"HAMAN-41306".equals(report.path("caseCode").asText())
                || !"attachment-segment-1.0.0".equals(report.path("engineVersion").asText())
                || !"segment-role-1.0.4".equals(report.path("segmentRuleVersion").asText())
                || !com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH.equals(report.path("segmentRulesHash").asText())
                || !(layout?"1.0.15":"1.0.12").equals(report.path("extractorVersion").asText())
                || !"REVIEW_REQUIRED".equals(report.path("decisionStatus").asText())
                || !"AVAILABLE".equals(report.path("bodyStatus").asText())
                || !selectTrue(report,"bodyStageComplete") || !selectTrue(report,"discoveryComplete")
                || !selectBounded(report,"discoveredFileCount",1,1) || !selectBounded(report,"processedFileCount",1,1)
                || !selectTrue(report,"segmentDatabaseApiVerified") || !selectTrue(report,"segmentReviewContextVerified")
                || !selectTrue(report,"manualSourceCheckRequired") || !selectTrue(report,"requiresFinalAdminVerification")
                || !selectFalse(report,"isWholeTextAnalysisComplete") || !selectFalse(report,"isPolicyQaPassed")
                || !selectFalse(report,"isExpectationApproved") || !selectFalse(report,"isAuthenticatedBrowserE2e")
                || !selectBounded(report,"productionWriteCount",0,0)
                || !selectBounded(report,"maximumRequestReservations",5,5) || !selectBounded(report,"maximumReservedBytes",25165824L,25165824L)
                || !selectBounded(report,"requestReservationsIncludingBodyUpperBound",3,5) || !selectBounded(report,"reservedBytesIncludingBodyUpperBound",1,25165824L)
                || !report.path("files").isArray() || report.path("files").size()!=1)return false;
        var file=report.path("files").get(0);
        if(layout&&(!"LOCAL_HAMAN_GET_V1".equals(report.path("profileCode").asText())
                ||!"c7cb4961b49e97449fe09afb77c5a4d009d3bf2df93d611c6c547046ad926513".equals(report.path("profileHash").asText())
                ||!"EVALUATED".equals(report.path("workerStatus").asText())||!"ATTACHMENT_INCOMPLETE".equals(report.path("decisionReason").asText())
                ||!selectBounded(report,"extractorCalls",1,1)||!"SUCCEEDED".equals(file.path("downloadStatus").asText())))return false;
        return "HWP".equals(file.path("format").asText()) && "PARTIAL_TEXT".equals(file.path("quality").asText())
                && "c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97".equals(file.path("binaryHash").asText())
                && (layout?"88bb6aebc74813186f8d9f3ac32b457e4a674c98bbf3649e3a1435077435542a":"ea24e32e9c049cf8a2a7d3ffae300ffdee864ac33939a78ef520cbfa334fb5f7").equals(file.path("textHash").asText())
                && selectBounded(file,"bytes",101888,101888) && selectBounded(file,"characterCount",layout?4647:4644,layout?4647:4644) && selectBounded(file,"blockCount",layout?214:213,layout?214:213)
                && (!layout || selectHamanLayoutDiagnosticsComplete(file))
                && file.path("segmentAnalysisHash").asText().matches("[a-f0-9]{64}")
                && selectBounded(file,"segmentCount",1,1) && selectBounded(file,"unknownSegmentCount",1,1) && selectBounded(file,"noticeSegmentCount",0,0)
                && "COMPLETE_TEXT_REQUIRED".equals(file.path("segmentReason").asText())
                && selectTrue(file,"segmentEvaluationInputBound") && selectTrue(file,"segmentApiProjectionMatched")
                && selectTrue(file,"legacyDefaultReadOnlyVerified") && selectTrue(file,"evaluationBoundApiVerified")
                && selectTrue(file,"otherVersionReadOnlyVerified") && selectTrue(file,"partialFullCoverageVerified")
                && !file.has("longFormObservedHashMatched") && !file.has("longFormCandidate") && !file.has("structuralCandidate");
    }
    static boolean selectHamanLayoutDiagnosticsComplete(com.fasterxml.jackson.databind.JsonNode file) {
        var causes=file.path("hwpPartialCauses");
        if(!causes.isArray()||causes.size()!=2)return false;
        return causes.get(0).size()==2&&"UNSUPPORTED_RECORD".equals(causes.get(0).path("code").asText())
                && selectBounded(causes.get(0),"count",2,2)
                && causes.get(1).size()==2&&"UNSUPPORTED_CONTROL".equals(causes.get(1).path("code").asText())
                && selectBounded(causes.get(1),"count",1,1);
    }
    /** 정상 기대값 승인이 아닌 두 형식의 실제 저장/조회 결합 검증이다. 입력 지문은1.0.14 관측에 고정한다. */
    static boolean selectJungguSegmentReportComplete(com.fasterxml.jackson.databind.JsonNode report) {
        if(!"JUNGGU-33626".equals(report.path("caseCode").asText())
                ||!"LOCAL_DAEGU_JUNGGU_GET_V1".equals(report.path("profileCode").asText())
                ||!"e648e332e85e22fd2a6818eaf48b1a5d7ae58d4cad50b9e9ee0da847d73541ef".equals(report.path("profileHash").asText())
                ||!"attachment-segment-1.0.0".equals(report.path("engineVersion").asText())
                ||!"segment-role-1.0.4".equals(report.path("segmentRuleVersion").asText())
                ||!com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH.equals(report.path("segmentRulesHash").asText())
                ||!"1.0.14".equals(report.path("extractorVersion").asText())
                ||!"EVALUATED".equals(report.path("workerStatus").asText())||!"AVAILABLE".equals(report.path("bodyStatus").asText())
                ||!"REVIEW_REQUIRED".equals(report.path("decisionStatus").asText())||!"ATTACHMENT_INCOMPLETE".equals(report.path("decisionReason").asText())
                ||!selectBounded(report,"discoveredFileCount",2,2)||!selectBounded(report,"processedFileCount",2,2)
                ||!selectBounded(report,"extractorCalls",2,2)||!selectBounded(report,"productionWriteCount",0,0)
                ||!selectBounded(report,"maximumRequestReservations",5,5)||!selectBounded(report,"maximumReservedBytes",25165824,25165824)
                ||!selectBounded(report,"requestReservationsIncludingBodyUpperBound",5,5)||!selectBounded(report,"reservedBytesIncludingBodyUpperBound",1,25165824)
                ||!report.path("files").isArray()||report.path("files").size()!=2)return false;
        for(String key:List.of("bodyStageComplete","discoveryComplete","segmentDatabaseApiVerified","segmentReviewContextVerified","manualSourceCheckRequired","requiresFinalAdminVerification"))
            if(!selectTrue(report,key))return false;
        for(String key:List.of("isWholeTextAnalysisComplete","isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e"))
            if(!selectFalse(report,key))return false;
        var seen=new java.util.HashSet<String>();
        for(var file:report.path("files")) {
            String format=file.path("format").asText();boolean pdf="PDF".equals(format);
            if(!Set.of("HWP","PDF").contains(format)||!seen.add(format)||!"SUCCEEDED".equals(file.path("downloadStatus").asText())
                    ||!(pdf?"PARTIAL_TEXT":"COMPLETE_TEXT").equals(file.path("quality").asText())
                    ||!(pdf?"6a57302609860d5332ccf95650cd754999270d6e6308acae19f06bd74104326c":"ae74fb4881522239bcb91a6f64dc137e1af55a207050ecfdc7e5a01f2a7026d6").equals(file.path("binaryHash").asText())
                    ||!(pdf?"9f6ed99df2287fe3e4b1e5546fcd44a045eb6c7434aaa3a65566e8b0aa2938cf":"76a66ee11c3a7e729f0777e19b7b17fef9de708ce893a199b381f603ca5a45bd").equals(file.path("textHash").asText())
                    ||!selectBounded(file,"bytes",pdf?209769:127488,pdf?209769:127488)
                    ||!selectBounded(file,"characterCount",pdf?4241:3120,pdf?4241:3120)||!selectBounded(file,"blockCount",pdf?5:178,pdf?5:178)
                    ||!file.path("segmentAnalysisHash").asText().matches("[a-f0-9]{64}")||!selectBounded(file,"segmentCount",1,200)
                    ||!selectBounded(file,"unknownSegmentCount",0,file.path("segmentCount").asInt())||!selectBounded(file,"noticeSegmentCount",0,file.path("segmentCount").asInt()))return false;
            for(String key:List.of("segmentEvaluationInputBound","segmentApiProjectionMatched","legacyDefaultReadOnlyVerified","evaluationBoundApiVerified","otherVersionReadOnlyVerified","pinnedInputAndCoverageVerified"))
                if(!selectTrue(file,key))return false;
            if(pdf&&(!selectTrue(file,"partialFullCoverageVerified")||!"COMPLETE_TEXT_REQUIRED".equals(file.path("segmentReason").asText())
                    ||!selectBounded(file,"segmentCount",1,1)||!selectBounded(file,"unknownSegmentCount",1,1)||!selectBounded(file,"noticeSegmentCount",0,0)))return false;
            if(file.has("longFormObservedHashMatched")||file.has("longFormCandidate")||file.has("structuralCandidate"))return false;
        }
        return true;
    }
    private static boolean selectFalse(com.fasterxml.jackson.databind.JsonNode node,String key) {
        return node.path(key).isBoolean() && !node.path(key).booleanValue();
    }
    private static boolean selectLongFormStorageComplete(com.fasterxml.jackson.databind.JsonNode report,com.fasterxml.jackson.databind.JsonNode file) {
        return selectTrue(file,"longFormObservedHashMatched") && selectTrue(file,"evaluationBoundApiVerified")
                && selectTrue(file,"otherVersionReadOnlyVerified") && !file.has("longFormCandidate") && !file.has("structuralCandidate")
                && selectBounded(file,"segmentCount",4,4) && selectBounded(file,"unknownSegmentCount",1,1)
                && selectBounded(file,"noticeSegmentCount",1,1) && "HWPX".equals(file.path("format").asText())
                && "6bf01402eaeedc655d89ef45cf4a3afb01dd3953e60685c7954a43a9faa9bf04".equals(file.path("binaryHash").asText())
                && "ff601753dc73037f6287d69b5fd261976b14f4e210377db81085bd5917fc2066".equals(file.path("textHash").asText())
                && "REVIEW_REQUIRED".equals(report.path("decisionStatus").asText()) && selectTrue(report,"manualSourceCheckRequired")
                && selectTrue(report,"requiresFinalAdminVerification");
    }
    private static boolean selectTrue(com.fasterxml.jackson.databind.JsonNode node,String key) {
        return node.path(key).isBoolean() && node.path(key).booleanValue();
    }
    private static boolean selectStructuralCounts(com.fasterxml.jackson.databind.JsonNode report,com.fasterxml.jackson.databind.JsonNode file) {
        String code=report.path("caseCode").asText();
        int total="BOEUN-221499".equals(code)?6:"BOEUN-221497".equals(code)?4:1;
        int unknown="BOEUN-221499".equals(code)?3:"BOEUN-221497".equals(code)?2:1;
        return file.path("segmentCount").longValue()==total && file.path("unknownSegmentCount").longValue()==unknown
                && file.path("noticeSegmentCount").longValue()==("BOEUN-218812".equals(code)?0:1)
                && "REVIEW_REQUIRED".equals(report.path("decisionStatus").asText()) && selectTrue(report,"manualSourceCheckRequired");
    }
    static boolean selectLongFormComparisonComplete(com.fasterxml.jackson.databind.JsonNode value) {
        if(!value.isObject())return false;
        var names=new java.util.HashSet<String>();value.fieldNames().forEachRemaining(names::add);
        if(!names.equals(Set.of("analysisVersion","rulesHash","analysisHash","sameInputAndCoverageVerified","sameBoundariesVerified","persistedOrApplied","statusCode","roles","reasons")))return false;
        if(!com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION.equals(value.path("analysisVersion").asText())
                || !com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH.equals(value.path("rulesHash").asText())
                || !value.path("analysisHash").asText().matches("[a-f0-9]{64}") || !selectTrue(value,"sameInputAndCoverageVerified")
                || !selectTrue(value,"sameBoundariesVerified") || !value.path("persistedOrApplied").isBoolean() || value.path("persistedOrApplied").booleanValue())return false;
        var roles=value.path("roles");var reasons=value.path("reasons");
        if(!roles.isArray() || roles.isEmpty() || roles.size()>200 || !reasons.isArray() || reasons.size()!=roles.size())return false;
        boolean unknown=false;
        for(int i=0;i<roles.size();i++) {
            String role=roles.get(i).asText(),reason=reasons.get(i).asText();
            if(!Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(role))return false;
            if("UNKNOWN".equals(role)) {
                unknown=true;
                if(!Set.of("INITIAL_HEADING_REQUIRED","ROLE_STRUCTURE_INCOMPLETE","STRUCTURE_UNCERTAIN","MIXED_DOCUMENT_ROLES",
                        "ROLE_ANALYSIS_LIMIT","COMPLETE_TEXT_REQUIRED","SEGMENT_ANALYSIS_LIMIT").contains(reason))return false;
            } else if(!"ROLE_TEXT_STRUCTURE_MATCHED".equals(reason))return false;
        }
        return (unknown?"REVIEW_REQUIRED":"RESOLVED").equals(value.path("statusCode").asText());
    }
    static boolean selectStructuralComparisonComplete(com.fasterxml.jackson.databind.JsonNode value) {
        var roles=value.path("roles");
        return com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.STRUCTURAL_VERSION.equals(value.path("analysisVersion").asText())
                && com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH.equals(value.path("rulesHash").asText())
                && value.path("analysisHash").asText().matches("[a-f0-9]{64}")
                && value.path("sameInputAndCoverageVerified").isBoolean() && value.path("sameInputAndCoverageVerified").booleanValue()
                && value.path("persistedOrApplied").isBoolean() && !value.path("persistedOrApplied").booleanValue()
                && roles.isArray() && roles.size()>0 && roles.size()<=200
                && java.util.stream.StreamSupport.stream(roles.spliterator(),false).allMatch(r->Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(r.asText()))
                && (java.util.stream.StreamSupport.stream(roles.spliterator(),false).anyMatch(r->"UNKNOWN".equals(r.asText()))?"REVIEW_REQUIRED":"RESOLVED").equals(value.path("statusCode").asText())
                && value.path("reasons").isArray() && value.path("reasons").size()==roles.size();
    }
    static boolean selectQuarterComparisonComplete(com.fasterxml.jackson.databind.JsonNode value) {
        return com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.QUARTER_VERSION.equals(value.path("analysisVersion").asText())
                && com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH.equals(value.path("rulesHash").asText())
                && value.path("analysisHash").asText().matches("[a-f0-9]{64}")
                && Set.of("RESOLVED","REVIEW_REQUIRED").contains(value.path("statusCode").asText())
                && value.path("sameInputAndCoverageVerified").isBoolean() && value.path("sameInputAndCoverageVerified").booleanValue()
                && value.path("persistedOrApplied").isBoolean() && !value.path("persistedOrApplied").booleanValue()
                && value.path("roles").isArray() && value.path("roles").size()>0 && value.path("roles").size()<=200
                && value.path("noticeEvidenceCounts").isArray();
    }
    static boolean selectCandidateComparisonComplete(com.fasterxml.jackson.databind.JsonNode candidate,int count) {
        return com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.PARENTHESIZED_VERSION.equals(candidate.path("analysisVersion").asText())
                && com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.PARENTHESIZED_RULES_HASH.equals(candidate.path("rulesHash").asText())
                && candidate.path("analysisHash").asText().matches("[a-f0-9]{64}")
                && Set.of("RESOLVED","REVIEW_REQUIRED").contains(candidate.path("statusCode").asText())
                && candidate.path("sameInputAndBoundariesVerified").isBoolean() && candidate.path("sameInputAndBoundariesVerified").booleanValue()
                && candidate.path("persistedOrApplied").isBoolean() && !candidate.path("persistedOrApplied").booleanValue()
                && candidate.path("sectionLayout").isArray() && candidate.path("sectionLayout").size()<=64
                && count>0 && count<=200 && candidate.path("segments").isArray() && candidate.path("segments").size()==count;
    }
    private static boolean selectBounded(com.fasterxml.jackson.databind.JsonNode report,String key,long min,long max) {
        var value=report.path(key);
        return value.isIntegralNumber() && value.canConvertToLong() && value.longValue()>=min && value.longValue()<=max;
    }
    static List<Object> selectFailureTrace(Throwable failure) {
        var trace=new java.util.ArrayList<Object>();
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        for(Throwable cause=failure;cause!=null&&trace.size()<8&&seen.add(cause);cause=cause.getCause()) {
            var frames=java.util.Arrays.stream(cause.getStackTrace()).limit(6)
                    .map(frame->frame.getClassName()+"#"+frame.getMethodName()+":"+frame.getLineNumber()).toList();
            trace.add(java.util.Map.of("type",cause.getClass().getName(),"frames",frames));
        }
        return trace;
    }
    public static void main(String[] args) {
        PrintStream output=System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        var result=new LinkedHashMap<String,Object>();
        result.put("kind","OFFICIAL_WORKER_PROBE");result.put("productionDatabaseUsed",false);
        result.put("isPolicyQaPassed",false);result.put("isAuthenticatedBrowserE2e",false);
        boolean passed=false;String stage="BOUNDARY";
        try {
            if((args.length!=1&&args.length!=2)||!args[0].matches("[0-9a-f]{64}")
                    ||!"Linux".equals(System.getProperty("os.name"))||"root".equals(System.getProperty("user.name"))
                    ||!"/work".equals(Path.of("").toAbsolutePath().toString())
                    ||!"/work/tmp".equals(System.getProperty("java.io.tmpdir"))
                    ||!"true".equals(System.getenv("SANEB_ATTACHMENT_OFFICIAL_WORKER_QA"))
                    ||!ENV.containsAll(System.getenv().keySet())) throw new IllegalStateException();
            String group=args.length==2?args[1]:"YANGPYEONG";
            List<String> caseCodes=selectCaseCodes(group);
            result.put("caseGroup",group);
            var json=new ObjectMapper();
            stage="CODE_IDENTITY";
            String codeHash=new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash();
            if(!args[0].equals(codeHash))throw new IllegalStateException();
            result.put("executionCodeHash",codeHash);
            stage="JCE_TLS_RUNTIME";
            // DB/외부 요청 전에 설치 JDK의 암호화 정책·기본 TLS 초기화를 확인한다.
            javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.net.ssl.SSLContext.getDefault();
            System.setProperty("saneb.attachment-qa.extractor-root","/qa/extractor");
            System.setProperty("saneb.attachment-official-worker.report","/work/reports");
            System.setProperty("saneb.attachment-official-worker.group",group);
            stage="JUNIT_EXECUTION";
            var listener=new SummaryGeneratingListener();
            var request=LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(AnnouncementAttachmentOfficialWorkerIntegrationTest.class))
                    .configurationParameter("junit.jupiter.execution.parallel.enabled","false")
                    .configurationParameter("junit.jupiter.tempdir.cleanup.mode.default","ALWAYS").build();
            LauncherFactory.create().execute(request,listener);
            var summary=listener.getSummary();
            result.put("found",summary.getTestsFoundCount());result.put("passed",summary.getTestsSucceededCount());
            result.put("failed",summary.getTestsFailedCount());result.put("skipped",summary.getTestsSkippedCount());
            result.put("aborted",summary.getTestsAbortedCount());result.put("failedContainers",summary.getContainersFailedCount());
            // 예외 메시지/본문/SQL을 제외하고 자료형과 제한된 클래스·메서드 위치만 진단한다.
            result.put("failureTypes",summary.getFailures().stream().map(f->f.getException().getClass().getSimpleName()).distinct().limit(8).toList());
            result.put("failureSites",summary.getFailures().stream().limit(3).map(f->selectFailureTrace(f.getException())).toList());
            stage="REPORTS";
            var reports=new java.util.ArrayList<Object>();boolean reportsComplete=true;
            for(String code:caseCodes) {
                Path path=Path.of("/work/reports",code+".json");
                if(!Files.isRegularFile(path)||Files.size(path)>65536){reportsComplete=false;continue;}
                var report=json.readTree(Files.readAllBytes(path));
                if(!code.equals(report.path("caseCode").asText())
                        ||!"OFFICIAL_WORKER_EPHEMERAL_DB_API_V1".equals(report.path("scope").asText()))throw new IllegalStateException();
                reports.add(report);
                String expected=selectTitleStopExpected(code)?"TITLE_EXCLUDED_NOT_FETCHED":"WORKER_DB_API_OBSERVED_NOT_APPROVED";
                reportsComplete&=expected.equals(report.path("status").asText())
                        &&report.path("originalFilesRemoved").asBoolean(false)&&report.path("remainingResourceLeases").asInt(-1)==0;
                if(selectSegmentMode(group))reportsComplete&=selectSegmentReportComplete(group,report);
            }
            result.put("cases",reports);
            stage="FINAL_IDENTITY";
            if(!codeHash.equals(new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash()))throw new IllegalStateException();
            passed=reportsComplete&&selectComplete(group,summary.getTestsFoundCount(),summary.getTestsSucceededCount(),
                    summary.getTestsFailedCount(),summary.getTestsSkippedCount(),summary.getTestsAbortedCount(),summary.getContainersFailedCount());
        } catch(Exception|LinkageError|AssertionError failure) {
            result.put("failedStage",stage);result.put("failureCode","OFFICIAL_WORKER_PROBE_INCOMPLETE");
            result.put("failureSites",selectFailureTrace(failure));
        }
        result.put("status",passed?"PASSED":"INCOMPLETE");
        try {output.println(new ObjectMapper().writeValueAsString(result));}
        catch(Exception ignored){output.println("{\"kind\":\"OFFICIAL_WORKER_PROBE\",\"status\":\"INCOMPLETE\"}");}
        System.exit(passed?0:1);
    }
}
