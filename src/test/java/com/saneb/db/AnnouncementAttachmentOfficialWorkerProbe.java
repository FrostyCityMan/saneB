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
            case "BOEUN", "BOEUN_SEGMENT" -> List.of("BOEUN-221499","BOEUN-221497","BOEUN-218812");
            default -> throw new IllegalArgumentException("OFFICIAL_WORKER_GROUP_INVALID");
        };
    }
    static boolean selectTitleStopExpected(String code) {
        boolean known=java.util.stream.Stream.of("YANGPYEONG","TAEBAEK","TAEBAEK_HWP","CHUNGJU","JECHEON","BOEUN")
                .flatMap(group->selectCaseCodes(group).stream()).anyMatch(code::equals);
        if(!known)throw new IllegalArgumentException("OFFICIAL_WORKER_CASE_INVALID");
        return Set.of("YANGPYEONG-311507","CHUNGJU-72625","CHUNGJU-72039","JECHEON-403587").contains(code);
    }
    static int selectExpectedExtractionCount(String code) {
        if(selectTitleStopExpected(code))return 0;
        // 양평의 미지원 이미지도 발견 분모에 남지만 추출 호출 수에는 넣지 않는다.
        return Set.of("TAEBAEK-184816","JECHEON-403490").contains(code)?2:1;
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
        return "BOEUN_SEGMENT".equals(group);
    }
    static String selectObservationGroup(String group) {
        return selectSegmentMode(group) ? "BOEUN" : group;
    }
    static long selectMaximumRequests(String group) { return selectSegmentMode(group) ? 5 : 44; }
    static long selectMaximumBytes(String group) { return (selectSegmentMode(group) ? 24L : 80L)*1024*1024; }
    /** 9/24 고정 실파일의 메모리 1.0.2 관측 지문이다. Provider catalog 승인이나 운영 정책 QA를 대신하지 않는다. */
    static String selectQuarterObservedHash(String caseCode) {
        return switch(caseCode) {
            case "BOEUN-221499" -> "7b5436c08db86b5ff9fe0648b1d8c8eed01e29cb996c35ee53cba26ab3b583e2";
            case "BOEUN-221497" -> "381f7db64f91cda2e44c5f3ee2a4d2ed5e89ecaf11a0225b6bf8bff7f9e71c7e";
            case "BOEUN-218812" -> "b87ff442be093bfc05c95739a5a48b70623c1bb1256174a5b8cbb084bebbbc7d";
            default -> throw new IllegalArgumentException("QUARTER_OBSERVATION_CASE_INVALID");
        };
    }
    static boolean selectSegmentReportComplete(com.fasterxml.jackson.databind.JsonNode report) {
        if (!selectCaseCodes("BOEUN").contains(report.path("caseCode").asText())
                || !"attachment-segment-1.0.0".equals(report.path("engineVersion").asText())
                || !"segment-role-1.0.2".equals(report.path("segmentRuleVersion").asText())
                || !com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH.equals(report.path("segmentRulesHash").asText())
                || !report.path("segmentDatabaseApiVerified").asBoolean(false)
                || !report.path("segmentReviewContextVerified").isBoolean() || !report.path("segmentReviewContextVerified").booleanValue()
                || !report.path("manualSourceCheckRequired").isBoolean()
                || report.path("maximumRequestReservations").asLong(-1)!=5
                || report.path("maximumReservedBytes").asLong(-1)!=25165824L
                || !selectBounded(report,"requestReservationsIncludingBodyUpperBound",3,5)
                || !selectBounded(report,"reservedBytesIncludingBodyUpperBound",1,25165824L)
                || !report.path("files").isArray() || report.path("files").size()!=1) return false;
        var file=report.path("files").get(0);
        return "COMPLETE_TEXT".equals(file.path("quality").asText())
                && selectQuarterObservedHash(report.path("caseCode").asText()).equals(file.path("segmentAnalysisHash").asText())
                && selectBounded(file,"segmentCount",1,200)
                && selectBounded(file,"unknownSegmentCount",0,file.path("segmentCount").asLong())
                && (file.path("unknownSegmentCount").longValue()==0 || report.path("manualSourceCheckRequired").booleanValue())
                && file.path("segmentEvaluationInputBound").asBoolean(false)
                && file.path("segmentApiProjectionMatched").asBoolean(false)
                && file.path("legacyDefaultReadOnlyVerified").isBoolean() && file.path("legacyDefaultReadOnlyVerified").booleanValue()
                && file.path("quarterObservedHashMatched").isBoolean() && file.path("quarterObservedHashMatched").booleanValue()
                && selectBounded(file,"noticeSegmentCount",0,file.path("segmentCount").asLong())
                && selectStructuralComparisonComplete(file.path("structuralCandidate"));
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
                if(selectSegmentMode(group))reportsComplete&=selectSegmentReportComplete(report);
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
