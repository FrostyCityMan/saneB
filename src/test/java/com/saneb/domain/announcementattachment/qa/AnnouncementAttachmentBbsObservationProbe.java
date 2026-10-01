package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** 별도 승인 범위의 고정 표본만 실행한다. 남구44381 단일 구조 진단은3건 QA와 구분한다. 게시/기대값 변경은 없다. */
public final class AnnouncementAttachmentBbsObservationProbe {
    static final List<String> OKCHEON_CASES = List.of("OKCHEON-193369", "OKCHEON-193297", "OKCHEON-193187");
    static final List<String> BOEUN_CASES = List.of("BOEUN-221499", "BOEUN-221497", "BOEUN-218812");
    static final List<String> NAMGU_CASES = List.of("NAMGU-44466", "NAMGU-44381", "NAMGU-42871");
    static final List<String> DALSEONG_CASES = List.of("DALSEONG-51022", "DALSEONG-52145", "DALSEONG-51075");
    static final List<String> JUNGGU_CASES = List.of("JUNGGU-34196", "JUNGGU-33626", "JUNGGU-33315");
    // bit i는 아래 사전의 i번째 규칙이 실제 해당 구간 evidence에 존재한다는 뜻이다. 역할 추정이 아니다.
    private static final List<String> SEGMENT_EVIDENCE_RULES = List.of("NOTICE_HEADING", "GUIDE_HEADING", "FORM_HEADING",
            "REFERENCE_HEADING", "TARGET_SECTION", "SUPPORT_SECTION", "APPLICATION_SECTION", "APPLICANT_FIELD",
            "SIGNATURE_FIELD", "QUESTION_ITEM", "ANSWER_ITEM");
    private static final Set<String> ENV = Set.of("PATH", "LANG", "HOME", "TMPDIR", "PWD",
            "SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION", "SANEB_ATTACHMENT_BBS_FIXED_CASE_QA", "SANEB_ATTACHMENT_GANGBUK_SELECTED_DOWNLOAD");
    private AnnouncementAttachmentBbsObservationProbe() { }

    static boolean selectComplete(long found, long succeeded, long failed, long skipped, long aborted, long containersFailed) {
        return found == 1 && succeeded == 1 && failed == 0 && skipped == 0 && aborted == 0 && containersFailed == 0;
    }

    static String selectMode(String[] args) {
        if (args.length < 1 || args.length > 2 || !args[0].matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("PROBE_ARGUMENTS_INVALID");
        if (args.length == 1) return "OBSERVATION";
        if (!Set.of("FIXED", "OKCHEON", "BOEUN_OBSERVATION", "BOEUN_DIAGNOSTIC", "OKCHEON_DIAGNOSTIC", "NAMGU_OBSERVATION", "NAMGU_STRUCTURE", "DALSEONG_OBSERVATION", "DALSEONG_HEADER", "HAMAN_OBSERVATION", "JUNGGU_OBSERVATION", "JUNGGU_PDF", "GANGBUK_OBSERVATION", "GANGBUK_SELECTED_DOWNLOAD").contains(args[1])) throw new IllegalArgumentException("PROBE_MODE_INVALID");
        return args[1];
    }

    static boolean selectOkcheonComplete(long found, long succeeded, long failed, long skipped, long aborted, long containersFailed) {
        return found == 3 && succeeded == 3 && failed == 0 && skipped == 0 && aborted == 0 && containersFailed == 0;
    }

    private static boolean selectBoolean(JsonNode node, String field, boolean expected) {
        return node.path(field).isBoolean() && node.path(field).booleanValue() == expected;
    }

    private static boolean selectBounded(JsonNode node, String field, long min, long max) {
        var value = node.path(field);
        return value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= min && value.longValue() <= max;
    }

    static boolean selectGangbukSelectedComplete(List<JsonNode> rows,Instant start,Instant end) {
        if(rows.size()!=1)return false;var row=rows.getFirst();
        try {var at=Instant.parse(row.path("observedAt").asText());if(at.isBefore(start)||at.isAfter(end))return false;}
        catch(Exception ignored){return false;}
        if(!"SELECTED_ATTACHMENT_DOWNLOAD_DIAGNOSTIC_V1".equals(row.path("scope").asText())
                ||!"GANGBUK-179490".equals(row.path("caseCode").asText())
                ||!GangbukSelectedDownloadDiagnosticTest.PRIOR_RECEIPT.equals(row.path("priorReceiptSha256").asText())
                ||!"LOCAL_GANGBUK_LEGAL_GET_V1".equals(row.path("profileCode").asText())
                ||!"6aa8ef570fdaf1dcc94e6e66b85d326a197de1057b6564368e97dc87ae781158".equals(row.path("profileHash").asText())
                ||!"SELECTED_FILE_OBSERVED_NOT_WHOLE_NOTICE".equals(row.path("status").asText()))return false;
        for(String key:List.of("isWholeTextAnalysisComplete","isPolicyQaPassed","isExpectationApproved"))if(!selectBoolean(row,key,false))return false;
        for(String key:List.of("discoveryComplete","originalFilesRemoved"))if(!selectBoolean(row,key,true))return false;
        for(String key:List.of("expectedListedFileCount","discoveredFileCount","selectedFileOrdinal"))if(!selectBounded(row,key,4,4))return false;
        if(!selectBounded(row,"productionWriteCount",0,0)||!selectBounded(row,"bodyRequests",0,0)
                ||!selectBounded(row,"maximumRequestReservations",5,5)||!selectBounded(row,"maximumReservedBytes",25165824,25165824)
                ||!selectBounded(row,"requestReservationsIncludingBodyUpperBound",4,5)||!selectBounded(row,"reservedBytesIncludingBodyUpperBound",1,25165824)
                ||!row.path("files").isArray()||row.path("files").size()!=4)return false;
        for(int i=0;i<4;i++) {
            var file=row.path("files").get(i);
            if(!AnnouncementAttachmentBbsOfficialObservationTest.GANGBUK_LOCATORS.get(i).equals(file.path("locatorHash").asText())
                    ||!(i==1?"HWP":"HWPX").equals(file.path("formatHint").asText()))return false;
            if(i<3){if(file.size()!=3||!"NOT_SELECTED".equals(file.path("status").asText()))return false;continue;}
            var trace=file.path("downloadTrace");
            if(!"OBSERVED".equals(file.path("status").asText())||!"HWPX".equals(file.path("format").asText())
                    ||!"1.0.14".equals(file.path("extractorVersion").asText())||!selectBounded(file,"bytes",1,20971520)
                    ||!file.path("binaryHash").asText().matches("[a-f0-9]{64}")
                    ||!Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED").contains(file.path("quality").asText())
                    ||trace.size()!=5||!selectBounded(trace,"schemaVersion",1,1)||!selectBounded(trace,"transportInvocations",3,3)
                    ||!selectBounded(trace,"completedTransports",3,3)||!"FINAL_POST".equals(trace.path("step").asText())
                    ||!"COMPLETE".equals(trace.path("phase").asText()))return false;
            if("COMPLETE_TEXT".equals(file.path("quality").asText())&&!selectSegmentMetadataComplete(file))return false;
        }
        return true;
    }

    // Node 관측 보고서 검증과 동일하게 관측 성공과 완전 추출·정책 승인을 분리한다.
    static boolean selectOkcheonReportsComplete(List<JsonNode> reports, Instant startedAt, Instant endedAt) {
        return selectThreeReportsComplete(reports, startedAt, endedAt, false);
    }
    static boolean selectBoeunReportsComplete(List<JsonNode> reports, Instant startedAt, Instant endedAt) {
        return selectThreeReportsComplete(reports, startedAt, endedAt, true);
    }
    private static boolean selectThreeReportsComplete(List<JsonNode> reports, Instant startedAt, Instant endedAt, boolean boeun) {
        return selectThreeReportsComplete(reports, startedAt, endedAt, boeun, false);
    }
    static boolean selectThreeReportsComplete(List<JsonNode> reports, Instant startedAt, Instant endedAt, boolean boeun, boolean diagnostic) {
        return selectThreeReportsComplete(reports, startedAt, endedAt, boeun ? "BOEUN" : "OKCHEON", diagnostic);
    }
    static boolean selectThreeReportsComplete(List<JsonNode> reports, Instant startedAt, Instant endedAt, String group, boolean diagnostic) {
        if ("GANGBUK".equals(group)) return selectGangbukReportsComplete(reports,startedAt,endedAt);
        if ("JUNGGU_PDF".equals(group)) return selectSaeolReportsComplete(reports,startedAt,endedAt,false,false,true,true);
        if ("JUNGGU".equals(group)) return selectSaeolReportsComplete(reports,startedAt,endedAt,false,false,true);
        if ("HAMAN".equals(group)) return selectSaeolReportsComplete(reports,startedAt,endedAt,false,true);
        if ("DALSEONG".equals(group)) return selectDalseongReportsComplete(reports,startedAt,endedAt);
        if ("DALSEONG_HEADER".equals(group)) return selectDalseongReportsComplete(reports,startedAt,endedAt,true);
        if (!Set.of("BOEUN", "OKCHEON", "NAMGU", "NAMGU_STRUCTURE").contains(group)) return false;
        boolean structure="NAMGU_STRUCTURE".equals(group);
        boolean boeun = "BOEUN".equals(group), namgu = "NAMGU".equals(group) || structure;
        if (namgu && !diagnostic) return false;
        long maximumRequests = namgu ? 5 : diagnostic ? 20 : 44, maximumBytes = namgu ? 25165824L : diagnostic ? 33554432L : 83886080L;
        if (reports == null || reports.size() != (structure?1:3) || startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) return false;
        var caseCodes = structure?List.of("NAMGU-44381"):namgu ? NAMGU_CASES : boeun ? BOEUN_CASES : OKCHEON_CASES;
        var seen = new HashSet<String>();
        for (var report : reports) {
            if (report == null || !caseCodes.contains(report.path("caseCode").asText())
                    || !seen.add(report.path("caseCode").asText())) return false;
            try {
                var observedAt = Instant.parse(report.path("observedAt").asText());
                if (observedAt.isBefore(startedAt) || observedAt.isAfter(endedAt)) return false;
            } catch (java.time.format.DateTimeParseException failure) { return false; }
            if (!"OFFICIAL_THREE_STAGE_OBSERVATION_V1".equals(report.path("scope").asText())
                    || !(namgu ? "LOCAL_BUSAN_NAMGU_GET_V1" : boeun ? "LOCAL_BOEUN_BBS_V1" : "LOCAL_OKCHEON_BBS_V1").equals(report.path("profileCode").asText())
                    || !selectBounded(report, "productionWriteCount", 0, 0)
                    || !selectBoolean(report, "isPolicyQaPassed", false) || !selectBoolean(report, "isExpectationApproved", false)
                    || !selectBoolean(report, "originalFilesRemoved", true) || !selectBounded(report, "expectedListedFileCount", 1, 1)
                    || !selectBounded(report, "maximumRequestReservations", maximumRequests, maximumRequests)
                    || !selectBounded(report, "maximumReservedBytes", maximumBytes, maximumBytes)
                    || !selectBounded(report, "requestReservationsIncludingBodyUpperBound", 0, maximumRequests)
                    || !selectBounded(report, "reservedBytesIncludingBodyUpperBound", 0, maximumBytes)
                    || !report.path("files").isArray()) return false;
            if (!boeun && "OKCHEON-193187".equals(report.path("caseCode").asText())) {
                if (!"TITLE_NOT_ELIGIBLE_NOT_FETCHED".equals(report.path("status").asText())
                        || !"COMBINATION_NOT_MATCHED".equals(report.path("titleStage").asText())
                        || !"TITLE_COMBINATION_NOT_MATCHED".equals(report.path("titleReason").asText())
                        || !report.path("files").isEmpty() || report.hasNonNull("bodyStatus") || report.hasNonNull("discoveryStatus")
                        || !selectBounded(report, "requestReservationsIncludingBodyUpperBound", 0, 0)
                        || !selectBounded(report, "reservedBytesIncludingBodyUpperBound", 0, 0)
                        || !selectBoolean(report, "isWholeTextAnalysisComplete", false)
                        || !selectBoolean(report, "requiresFinalAdminVerification", false)) return false;
                continue;
            }
            if (!"OBSERVED_NOT_VALIDATED".equals(report.path("status").asText())
                    || !"COMBINATION_MATCHED".equals(report.path("titleStage").asText())
                    || !selectBoolean(report, "bodyStageComplete", true) || !"AVAILABLE".equals(report.path("bodyStatus").asText())
                    || !"FOUND".equals(report.path("discoveryStatus").asText()) || !selectBoolean(report, "discoveryComplete", true)
                    || !selectBounded(report, "discoveredFileCount", 1, 1) || report.path("files").size() != 1
                    || !selectBoolean(report, "requiresFinalAdminVerification", true)
                    || !selectBounded(report, "requestReservationsIncludingBodyUpperBound", 3, maximumRequests)
                    || !selectBounded(report, "reservedBytesIncludingBodyUpperBound", 1, maximumBytes)
                    || !Set.of("ACCEPTED", "REVIEW_REQUIRED").contains(report.path("decisionStatus").asText())) return false;
            var file = report.path("files").get(0);
            String format = namgu ? "HWP" : boeun && "BOEUN-218812".equals(report.path("caseCode").asText()) ? "PDF" : "HWPX";
            if (!"OBSERVED".equals(file.path("status").asText()) || !format.equals(file.path("format").asText())
                    || !selectBounded(file, "bytes", 1, 20971520)
                    || !Set.of("COMPLETE_TEXT", "PARTIAL_TEXT", "OCR_REQUIRED", "ENCRYPTED", "CORRUPT", "UNSUPPORTED", "LIMIT_EXCEEDED")
                            .contains(file.path("quality").asText())) return false;
            boolean complete = "COMPLETE_TEXT".equals(file.path("quality").asText());
            if(structure) {
                if(!complete || !"69f7738308da99a68f528d2c08dae175e9880c0bb0764d6c5bcffd6e333548c8".equals(file.path("binaryHash").asText())
                        || !"7181d23cb9973622cf14e2a6edfbed42424b06ad13a91eb53ed55d158d77161a".equals(file.path("textHash").asText()))return false;
                try { selectStructureSummary(file.path("roleStructureObservation")); }
                catch(IllegalArgumentException invalid){return false;}
            }
            if (namgu && Set.of("COMPLETE_TEXT", "PARTIAL_TEXT", "OCR_REQUIRED").contains(file.path("quality").asText())) {
                try {
                    if (com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor.selectHwpStructureDetails(file) == null) return false;
                    if (!com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(file.path("extractorVersion").asText())) return false;
                    var partialDiagnostic=(com.fasterxml.jackson.databind.node.ObjectNode)file.deepCopy();
                    partialDiagnostic.set("qualityCode",file.path("quality"));
                    if (com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor.selectHwpPartialCauseList(partialDiagnostic) == null) return false;
                } catch (java.io.IOException invalid) { return false; }
            }
            if (complete && (!selectBounded(file, "characterCount", 1, 1000000) || !selectBounded(file, "blockCount", 1, 20000)
                    || !Set.of("NOTICE", "GUIDE", "FORM", "REFERENCE", "UNKNOWN").contains(file.path("roleAssessment").path("roleCode").asText()))) return false;
            if (!selectBoolean(report, "isWholeTextAnalysisComplete", complete)) return false;
            if ((boeun || namgu) && complete && !selectSegmentMetadataComplete(file)) return false;
        }
        return true;
    }

    /** 첫 실파일 관측은 고정 locator 전체를 결합하되 아직 없는 binary 기대값을 만들어 승인하지 않는다. */
    static boolean selectGangbukReportsComplete(List<JsonNode> reports,Instant startedAt,Instant endedAt) {
        if(reports==null||reports.size()!=1||reports.getFirst()==null||startedAt==null||endedAt==null||endedAt.isBefore(startedAt))return false;
        var row=reports.getFirst();
        try {var at=Instant.parse(row.path("observedAt").asText());if(at.isBefore(startedAt)||at.isAfter(endedAt))return false;}
        catch(java.time.format.DateTimeParseException invalid){return false;}
        var expected=java.util.Map.of("caseCode","GANGBUK-179490","scope","OFFICIAL_THREE_STAGE_OBSERVATION_V1",
                "profileCode","LOCAL_GANGBUK_LEGAL_GET_V1","profileHash","6aa8ef570fdaf1dcc94e6e66b85d326a197de1057b6564368e97dc87ae781158",
                "status","OBSERVED_NOT_VALIDATED","titleStage","COMBINATION_MATCHED","titleInputSource","FIXED_OFFICIAL_SAMPLE",
                "rulesSource","EPHEMERAL_DB_DRAFT_SEED","bodyStatus","AVAILABLE","discoveryStatus","FOUND");
        if(expected.entrySet().stream().anyMatch(e->!e.getValue().equals(row.path(e.getKey()).asText()))
                ||!row.path("rulesHash").asText().matches("[a-f0-9]{64}")||!row.path("bodyHash").asText().matches("[a-f0-9]{64}"))return false;
        for(String key:List.of("bodyStageComplete","discoveryComplete","originalFilesRemoved","requiresFinalAdminVerification"))
            if(!selectBoolean(row,key,true))return false;
        for(String key:List.of("isPolicyQaPassed","isExpectationApproved"))if(!selectBoolean(row,key,false))return false;
        if(!selectBounded(row,"productionWriteCount",0,0)||!selectBounded(row,"expectedListedFileCount",4,4)
                ||!selectBounded(row,"discoveredFileCount",4,4)||!selectBounded(row,"bodyCharacterCount",1,1000000)
                ||!selectBounded(row,"maximumRequestReservations",20,20)||!selectBounded(row,"maximumReservedBytes",33554432,33554432)
                ||!selectBounded(row,"requestReservationsIncludingBodyUpperBound",15,20)||!selectBounded(row,"reservedBytesIncludingBodyUpperBound",1,33554432)
                ||!row.path("files").isArray()||row.path("files").size()!=4
                ||!Set.of("ACCEPTED","REVIEW_REQUIRED").contains(row.path("decisionStatus").asText()))return false;
        boolean whole=true;var formats=List.of("HWPX","HWP","HWPX","HWPX");
        for(int i=0;i<4;i++) {
            var file=row.path("files").get(i);String quality=file.path("quality").asText();
            if(!AnnouncementAttachmentBbsOfficialObservationTest.GANGBUK_LOCATORS.get(i).equals(file.path("locatorHash").asText())
                    ||!formats.get(i).equals(file.path("format").asText())||!formats.get(i).equals(file.path("formatHint").asText())
                    ||!"OBSERVED".equals(file.path("status").asText())||!selectBoolean(file,"downloadAllowed",true)
                    ||!selectBounded(file,"bytes",1,20971520)||!file.path("binaryHash").asText().matches("[a-f0-9]{64}")
                    ||!com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(file.path("extractorVersion").asText())
                    ||!Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED").contains(quality))return false;
            boolean complete="COMPLETE_TEXT".equals(quality);whole&=complete;
            if(complete&&(!selectBounded(file,"characterCount",1,1000000)||!selectBounded(file,"blockCount",1,20000)
                    ||!Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(file.path("roleAssessment").path("roleCode").asText())
                    ||!selectSegmentMetadataComplete(file)))return false;
        }
        return selectBoolean(row,"isWholeTextAnalysisComplete",whole)&&(whole||"REVIEW_REQUIRED".equals(row.path("decisionStatus").asText()));
    }

    static List<String> selectDalseongBinaryHashes(String code) {
        return switch(code) {
            case "DALSEONG-51022" -> List.of("1dc0fd0deeb1d892bb125ec567c71bc3111fb9ecf861f52e7107c6877ec88fdb","6dd8d582a0c8d6cbe2f22376002d737eb15aab98a28ac0e8f612bdc1e1837fb4");
            case "DALSEONG-52145" -> List.of("c3488feba7f7b3addafb0e6037be47f1e34193e8137769b514ed2453bd27e98c");
            case "DALSEONG-51075" -> List.of("f400d97b469c0473a78d10a91ec0d9bc2956d0ecbba2df6546a8191564b729ad");
            default -> throw new IllegalArgumentException("DALSEONG_CASE_INVALID");
        };
    }
    static boolean selectDalseongReportsComplete(List<JsonNode> reports,Instant startedAt,Instant endedAt) {
        return selectDalseongReportsComplete(reports,startedAt,endedAt,false);
    }
    static boolean selectDalseongReportsComplete(List<JsonNode> reports,Instant startedAt,Instant endedAt,boolean header) {
        return selectSaeolReportsComplete(reports,startedAt,endedAt,header,false);
    }
    private static boolean selectSaeolReportsComplete(List<JsonNode> reports,Instant startedAt,Instant endedAt,boolean header,boolean haman) {
        return selectSaeolReportsComplete(reports,startedAt,endedAt,header,haman,false);
    }
    static List<String> selectJungguBinaryHashes(String code) {
        return switch(code) {
            case "JUNGGU-34196" -> List.of("4a544f3c98451eb7c002c626157c2b92468962c14756f7b38e3e218954b1b554");
            case "JUNGGU-33626" -> List.of("ae74fb4881522239bcb91a6f64dc137e1af55a207050ecfdc7e5a01f2a7026d6","6a57302609860d5332ccf95650cd754999270d6e6308acae19f06bd74104326c");
            case "JUNGGU-33315" -> List.of("0d192cd2ad69fde6271fb2f8fc6018a98aada490b35ab1a4c66901b8f7e3bc77");
            default -> throw new IllegalArgumentException("JUNGGU_CASE_INVALID");
        };
    }
    private static boolean selectSaeolReportsComplete(List<JsonNode> reports,Instant startedAt,Instant endedAt,boolean header,boolean haman,boolean junggu) {
        return selectSaeolReportsComplete(reports,startedAt,endedAt,header,haman,junggu,false);
    }
    private static boolean selectSaeolReportsComplete(List<JsonNode> reports,Instant startedAt,Instant endedAt,boolean header,boolean haman,boolean junggu,boolean jungguPdf) {
        int caseCount=header||haman||jungguPdf?1:3;
        if(reports==null||reports.size()!=caseCount||startedAt==null||endedAt==null||endedAt.isBefore(startedAt))return false;
        for(int index=0;index<caseCount;index++) {
            var row=reports.get(index);String code=jungguPdf?"JUNGGU-33626":junggu?JUNGGU_CASES.get(index):haman?"HAMAN-41306":DALSEONG_CASES.get(index);
            var hashes=junggu?selectJungguBinaryHashes(code):haman?List.of("c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97"):selectDalseongBinaryHashes(code);int count=hashes.size();
            if(row==null||!code.equals(row.path("caseCode").asText()))return false;
            try {var time=Instant.parse(row.path("observedAt").asText());if(time.isBefore(startedAt)||time.isAfter(endedAt))return false;}
            catch(java.time.format.DateTimeParseException invalid){return false;}
            if(junggu) {
                if(!"e648e332e85e22fd2a6818eaf48b1a5d7ae58d4cad50b9e9ee0da847d73541ef".equals(row.path("profileHash").asText())
                        ||!"FIXED_OFFICIAL_SAMPLE".equals(row.path("titleInputSource").asText())
                        ||!"EPHEMERAL_DB_DRAFT_SEED".equals(row.path("rulesSource").asText())
                        ||!row.path("rulesHash").asText().matches("[a-f0-9]{64}"))return false;
                if(index==2) {
                    if(!selectJungguTitleStopComplete(row))return false;
                    continue;
                }
            }
            if(!"OFFICIAL_THREE_STAGE_OBSERVATION_V1".equals(row.path("scope").asText())
                    ||!(junggu?"LOCAL_DAEGU_JUNGGU_GET_V1":haman?"LOCAL_HAMAN_GET_V1":"LOCAL_DAEGU_DALSEONG_GET_V1").equals(row.path("profileCode").asText())
                    ||!"OBSERVED_NOT_VALIDATED".equals(row.path("status").asText())
                    ||!"COMBINATION_MATCHED".equals(row.path("titleStage").asText())
                    ||!"AVAILABLE".equals(row.path("bodyStatus").asText())||!selectBoolean(row,"bodyStageComplete",true)
                    ||!"FOUND".equals(row.path("discoveryStatus").asText())||!selectBoolean(row,"discoveryComplete",true)
                    ||!selectBounded(row,"expectedListedFileCount",count,count)||!selectBounded(row,"discoveredFileCount",count,count)
                    ||!selectBounded(row,"maximumRequestReservations",6,6)||!selectBounded(row,"maximumReservedBytes",24117248,24117248)
                    ||!selectBounded(row,"requestReservationsIncludingBodyUpperBound",3+count,6)||!selectBounded(row,"reservedBytesIncludingBodyUpperBound",1,24117248)
                    ||!selectBounded(row,"productionWriteCount",0,0)||!selectBoolean(row,"isPolicyQaPassed",false)
                    ||!selectBoolean(row,"isExpectationApproved",false)||!selectBoolean(row,"originalFilesRemoved",true)
                    ||!selectBoolean(row,"requiresFinalAdminVerification",true)||!row.path("files").isArray()||row.path("files").size()!=count
                    ||!Set.of("ACCEPTED","REVIEW_REQUIRED").contains(row.path("decisionStatus").asText()))return false;
            boolean whole=true;
            for(int fileIndex=0;fileIndex<count;fileIndex++) {
                var file=row.path("files").get(fileIndex);String format=junggu?("JUNGGU-34196".equals(code)?"HWPX":fileIndex==0?"HWP":"PDF"):!haman&&index==0&&fileIndex==0?"PDF":"HWP";
                if(!"OBSERVED".equals(file.path("status").asText())||!format.equals(file.path("format").asText())
                        ||!hashes.get(fileIndex).equals(file.path("binaryHash").asText())||!selectBoolean(file,"downloadAllowed",true)
                        ||!selectBounded(file,"bytes",1,20971520)
                        ||!com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(file.path("extractorVersion").asText())
                        ||!Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED").contains(file.path("quality").asText()))return false;
                boolean complete="COMPLETE_TEXT".equals(file.path("quality").asText());whole&=complete;
                if(jungguPdf&&"PDF".equals(format)&&!selectPdfDiagnosticComplete(file))return false;
                if(complete&&(!selectBounded(file,"characterCount",1,1000000)||!selectBounded(file,"blockCount",1,20000)
                        ||!Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(file.path("roleAssessment").path("roleCode").asText())
                        ||!selectSegmentMetadataComplete(file)))return false;
                if("HWP".equals(format)&&Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED").contains(file.path("quality").asText())) {
                    try {
                        if(com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor.selectHwpStructureDetails(file)==null)return false;
                        if(header&&(!file.at("/hwpStructure/controlHeaders").isArray()||file.at("/hwpStructure/controlHeaders").isEmpty()))return false;
                        var diagnostic=(com.fasterxml.jackson.databind.node.ObjectNode)file.deepCopy();diagnostic.set("qualityCode",file.path("quality"));
                        if(com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor.selectHwpPartialCauseList(diagnostic)==null)return false;
                    } catch(java.io.IOException invalid){return false;}
                }
            }
            if(!selectBoolean(row,"isWholeTextAnalysisComplete",whole)||(!whole&&!"REVIEW_REQUIRED".equals(row.path("decisionStatus").asText())))return false;
        }
        return true;
    }
    /** 원문 없는 전송용 진단 검사. 실제 텍스트 정합성은 격리 IPC 검증에서 이미 확인한다. */
    static boolean selectPdfDiagnosticComplete(JsonNode file) {
        var value=file.path("pdfStructure");
        if(!value.isObject()||value.size()!=6||!"PDF".equals(file.path("format").asText())
                ||!selectBounded(file,"characterCount",0,1000000)||!selectBounded(file,"blockCount",0,20000)
                ||!file.path("textHash").asText().matches("[a-f0-9]{64}"))return false;
        for(String key:List.of("pageCount","reliablePageCount","externalObjectInvocationCount","inlineImageInvocationCount","blankPageCount","replacementCharacterCount"))
            if(!selectBounded(value,key,0,key.endsWith("InvocationCount")?40000000:"replacementCharacterCount".equals(key)?1000000:200))return false;
        int pages=value.path("pageCount").asInt(),reliable=value.path("reliablePageCount").asInt(),blank=value.path("blankPageCount").asInt();
        long objects=value.path("externalObjectInvocationCount").asLong()+value.path("inlineImageInvocationCount").asLong();
        int replacements=value.path("replacementCharacterCount").asInt(),characters=file.path("characterCount").asInt();
        if(reliable>pages||blank>pages||objects>200000L*pages||(objects>0&&reliable==pages)||replacements>characters||(pages==0&&characters>0))return false;
        String quality=characters==0?"OCR_REQUIRED":objects>0||blank>0||replacements>0?"PARTIAL_TEXT":"COMPLETE_TEXT";
        return quality.equals(file.path("quality").asText());
    }
    private static boolean selectJungguTitleStopComplete(JsonNode row) {
        return "OFFICIAL_THREE_STAGE_OBSERVATION_V1".equals(row.path("scope").asText())
                &&"LOCAL_DAEGU_JUNGGU_GET_V1".equals(row.path("profileCode").asText())
                &&"TITLE_NOT_ELIGIBLE_NOT_FETCHED".equals(row.path("status").asText())
                &&"COMBINATION_NOT_MATCHED".equals(row.path("titleStage").asText())
                &&"TITLE_COMBINATION_NOT_MATCHED".equals(row.path("titleReason").asText())
                &&selectBounded(row,"expectedListedFileCount",1,1)&&row.path("files").isArray()&&row.path("files").isEmpty()
                &&List.of("bodyStatus","bodyHash","bodyStageComplete","discoveryStatus","discoveredFileCount","decisionStatus").stream().noneMatch(row::has)
                &&selectBounded(row,"maximumRequestReservations",6,6)&&selectBounded(row,"maximumReservedBytes",24117248,24117248)
                &&selectBounded(row,"requestReservationsIncludingBodyUpperBound",0,0)&&selectBounded(row,"reservedBytesIncludingBodyUpperBound",0,0)
                &&selectBounded(row,"productionWriteCount",0,0)&&selectBoolean(row,"isPolicyQaPassed",false)
                &&selectBoolean(row,"isExpectationApproved",false)&&selectBoolean(row,"originalFilesRemoved",true)
                &&selectBoolean(row,"isWholeTextAnalysisComplete",false)&&selectBoolean(row,"requiresFinalAdminVerification",false);
    }
    static boolean selectSegmentMetadataComplete(JsonNode file) {
        try {
            var analysis = file.path("segmentAnalysis");
            return analysis.isObject() && analysis.path("analysisVersion").asText().equals(
                    com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION)
                    && analysis.path("rulesHash").asText().equals(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH)
                    && analysis.path("textHash").asText().matches("[a-f0-9]{64}")
                    && analysis.path("textHash").equals(file.path("textHash"))
                    && analysis.path("blocksHash").asText().matches("[a-f0-9]{64}")
                    && analysis.path("blocksHash").equals(file.path("roleAssessment").path("blocksHash"))
                    && analysis.path("textLength").equals(file.path("characterCount"))
                    && analysis.path("segments").isArray() && analysis.path("segments").size() > 0 && analysis.path("segments").size() <= 200
                    && Set.of("RESOLVED", "REVIEW_REQUIRED").contains(analysis.path("statusCode").asText())
                    && AnnouncementAttachmentOfficialObservationTest.selectHash(analysis).equals(file.path("segmentAnalysisHash").asText());
        } catch (Exception failure) { return false; }
    }

    // SSM stdout 한도 안에서 모든 파일/역할 분모와 전체 분석 지문을 보존한다. 원문·좌표를 전송하지 않는다.
    static JsonNode selectTransportReport(JsonNode report) {
        var copy = (com.fasterxml.jackson.databind.node.ObjectNode) report.deepCopy();
        copy.put("reportRepresentation", "METADATA_SUMMARY_FULL_ANALYSIS_HASH");
        for (var file : copy.path("files")) {
            if (!(file instanceof com.fasterxml.jackson.databind.node.ObjectNode row)) continue;
            row.remove("roleStructureObservation");
            if (row.path("roleAssessment") instanceof com.fasterxml.jackson.databind.node.ObjectNode role) {
                role.put("evidenceCount", role.path("evidence").size()); role.remove("evidence");
            }
            if (row.path("segmentAnalysis") instanceof com.fasterxml.jackson.databind.node.ObjectNode analysis) {
                var segments = analysis.remove("segments");
                analysis.put("segmentCount", segments.size());
                var roles = analysis.putArray("roleCodes");
                var ruleDictionary = analysis.putArray("evidenceRuleDictionary");
                SEGMENT_EVIDENCE_RULES.forEach(ruleDictionary::add);
                var ruleMasks = analysis.putArray("evidenceRuleMasks");
                var reasons = analysis.putObject("reasonCounts");
                for (var segment : segments) {
                    roles.add(segment.path("roleCode").asText());
                    int mask = 0;
                    for (var evidence : segment.path("evidence")) {
                        int index = SEGMENT_EVIDENCE_RULES.indexOf(evidence.path("ruleCode").asText());
                        if (index < 0) throw new IllegalArgumentException("SEGMENT_DIAGNOSTIC_RULE_UNKNOWN");
                        mask |= 1 << index;
                    }
                    ruleMasks.add(mask);
                    String reason = segment.path("reasonCode").asText();
                    reasons.put(reason, reasons.path(reason).asInt() + 1);
                }
                row.set("segmentSummary", analysis); row.remove("segmentAnalysis");
            }
        }
        return copy;
    }

    private static final List<String> STRUCTURE_CODES=List.of("NOTICE_TERM","GUIDE_TERM","FORM_TERM","TARGET_LABEL",
            "SUPPORT_LABEL","PERIOD_LABEL","METHOD_LABEL","APPLICANT_LABEL","BUSINESS_ID_LABEL","BUSINESS_NAME_LABEL",
            "SIGNATURE_MARKER","STANDARD_REFERENCE","EXCLUSION_REFERENCE","SUBMISSION_SECTION");
    private static final int[] STRUCTURE_TOKEN_COUNTS={3,4,5,5,7,4,3,3,2,3,5,5,3,3};
    /** 고정 사전 번호/원문 위치/불리언만 전송한다. 임의 문자열과 잘린 관측은 거부한다. */
    static JsonNode selectStructureSummary(JsonNode input) {
        final String error="STRUCTURE_DIAGNOSTIC_INVALID";
        if(!input.isObject() || input.size()!=7 || !selectBounded(input,"schemaVersion",1,1)
                || !"FIXED_TOKEN_STRUCTURE_ONLY".equals(input.path("scope").asText())
                || !selectBounded(input,"inspectedNonblankLineCount",1,20000)
                || !selectBoolean(input,"isLineLimitReached",false) || !selectBoolean(input,"isTruncated",false)
                || !input.path("signals").isArray() || !selectBounded(input,"signalMatchCount",0,128)
                || input.path("signals").size()!=input.path("signalMatchCount").intValue())throw new IllegalArgumentException(error);
        var result=new ObjectMapper().createObjectNode();result.put("schemaVersion",1);
        result.put("lineCount",input.path("inspectedNonblankLineCount").intValue());
        var dictionary=result.putArray("ruleDictionary");STRUCTURE_CODES.forEach(dictionary::add);
        var rows=result.putArray("signals");
        int previousLine=0,previousRule=-1;
        for(var signal:input.path("signals")) {
            int rule=STRUCTURE_CODES.indexOf(signal.path("code").asText());
            if(!signal.isObject() || signal.size()!=11 || rule<0
                    || !selectBounded(signal,"lineNumber",1,input.path("inspectedNonblankLineCount").intValue())
                    || !selectBounded(signal,"startOffset",0,1000000) || !selectBounded(signal,"endOffset",1,1000000)
                    || signal.path("endOffset").intValue()<=signal.path("startOffset").intValue()
                    || !selectBounded(signal,"blockIndex",-1,19999) || !signal.path("tokenIndexes").isArray()
                    || signal.path("tokenIndexes").isEmpty())throw new IllegalArgumentException(error);
            int line=signal.path("lineNumber").intValue();
            if(line<previousLine || (line==previousLine && rule<=previousRule))throw new IllegalArgumentException(error);
            previousLine=line;previousRule=rule;
            int mask=0,last=-1;
            for(var index:signal.path("tokenIndexes")) {
                if(!index.isIntegralNumber() || !index.canConvertToInt() || index.intValue()<=last || index.intValue()>=STRUCTURE_TOKEN_COUNTS[rule])throw new IllegalArgumentException(error);
                last=index.intValue();mask|=1<<last;
            }
            int flags=0,bit=0;
            for(String key:List.of("isSingleReliableBlock","isWithinInitialHeading","isWholeLineToken","isLineEndingToken","hasColon")) {
                if(!signal.path(key).isBoolean())throw new IllegalArgumentException(error);
                if(signal.path(key).booleanValue())flags|=1<<bit;bit++;
            }
            rows.addArray().add(rule).add(line).add(mask).add(signal.path("startOffset").intValue())
                    .add(signal.path("endOffset").intValue()).add(signal.path("blockIndex").intValue()).add(flags);
        }
        return result;
    }
    static JsonNode selectStructureTransportReport(JsonNode report) {
        var output=selectTransportReport(report);
        for(int index=0;index<report.path("files").size();index++) {
            var file=(com.fasterxml.jackson.databind.node.ObjectNode)output.path("files").get(index);
            file.set("structureSummary",selectStructureSummary(report.path("files").get(index).path("roleStructureObservation")));
        }
        return output;
    }

    static boolean selectReportComplete(JsonNode report) {
        return report != null && "TAEBAEK-184816".equals(report.path("caseCode").asText())
                && "OFFICIAL_THREE_STAGE_OBSERVATION_V1".equals(report.path("scope").asText())
                && "OBSERVED_NOT_VALIDATED".equals(report.path("status").asText())
                && report.path("productionWriteCount").asInt(-1) == 0
                && report.path("isPolicyQaPassed").isBoolean() && !report.path("isPolicyQaPassed").asBoolean()
                && report.path("isExpectationApproved").isBoolean() && !report.path("isExpectationApproved").asBoolean()
                && report.path("originalFilesRemoved").asBoolean(false)
                && report.path("requiresFinalAdminVerification").asBoolean(false)
                && report.path("bodyStageComplete").asBoolean(false)
                && report.path("files").isArray() && report.path("files").size() == 2
                && report.path("maximumRequestReservations").asInt(-1) == 44
                && report.path("maximumReservedBytes").asLong(-1) == 83886080L
                && report.path("requestReservationsIncludingBodyUpperBound").asLong(-1) >= 0
                && report.path("requestReservationsIncludingBodyUpperBound").asLong(-1) <= 44
                && report.path("reservedBytesIncludingBodyUpperBound").asLong(-1) >= 0
                && report.path("reservedBytesIncludingBodyUpperBound").asLong(-1) <= 83886080L;
    }

    static boolean selectFixedReportComplete(JsonNode report) {
        if(report==null||!"TAEBAEK-184816".equals(report.path("caseCode").asText())
                ||!"FIXED_CASE_EXECUTOR_QA_EPHEMERAL_ONLY".equals(report.path("scope").asText())
                ||!"FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED".equals(report.path("status").asText())
                ||report.path("productionWriteCount").asInt(-1)!=0
                ||!report.path("originalFilesRemoved").asBoolean(false))return false;
        for(String key:Set.of("isPolicyQaPassed","isExpectationCoverageComplete","normalNotice"))
            if(!report.path(key).isBoolean()||report.path(key).asBoolean())return false;
        var result=report.path("result");
        if(!"SINGLE_FIXED_NOTICE_PROVIDER_QA".equals(result.path("scope").asText())
                ||!"TAEBAEK-184816".equals(result.path("caseId").asText())
                ||!"PASSED".equals(result.path("status").asText())
                ||!"FIXED_NOTICE_EXPECTATIONS_MATCHED".equals(result.path("reasonCode").asText())
                ||result.path("expectedFileCount").asInt(-1)!=2||result.path("discoveredFileCount").asInt(-1)!=2
                ||!result.path("discoveryComplete").asBoolean(false)||!result.path("allTextComplete").asBoolean(false)
                ||!result.path("originalFilesRemoved").asBoolean(false)||!result.path("isPolicyQaPassed").isBoolean()
                ||result.path("isPolicyQaPassed").asBoolean()||!result.path("files").isArray()||result.path("files").size()!=2
                ||result.path("requestReservations").asLong(-1)<1||result.path("requestReservations").asLong()>39
                ||result.path("reservedBytes").asLong(-1)<1||result.path("reservedBytes").asLong()>81508141L)return false;
        for(var file:result.path("files"))if(!"PASSED".equals(file.path("status").asText())
                ||!"COMPLETE_TEXT".equals(file.path("quality").asText())||!file.path("roleAssessmentHash").asText().matches("[a-f0-9]{64}"))return false;
        return true;
    }

    public static void main(String[] args) {
        if (args.length == 2 && RegionalCollectionObservationProbe.selectSupportedMode(args[1])) {
            RegionalCollectionObservationProbe.main(args);
            return;
        }
        PrintStream output = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        var result = new LinkedHashMap<String, Object>();
        result.put("kind", "BBS_OBSERVATION_PROBE");
        result.put("productionDatabaseUsed", false);
        result.put("isPolicyQaPassed", false);
        result.put("isExpectationApproved", false);
        boolean passed = false;
        String stage = "BOUNDARY";
        try {
            String mode = selectMode(args);
            boolean fixed = "FIXED".equals(mode);
            boolean selected = GangbukSelectedDownloadDiagnosticTest.MODE.equals(mode);
            boolean okcheon = Set.of("OKCHEON", "OKCHEON_DIAGNOSTIC").contains(mode);
            boolean boeun = Set.of("BOEUN_OBSERVATION", "BOEUN_DIAGNOSTIC").contains(mode);
            boolean structure="NAMGU_STRUCTURE".equals(mode);
            boolean namgu = "NAMGU_OBSERVATION".equals(mode) || structure;
            boolean header="DALSEONG_HEADER".equals(mode);
            boolean dalseong = "DALSEONG_OBSERVATION".equals(mode)||header;
            boolean haman = "HAMAN_OBSERVATION".equals(mode);
            boolean jungguPdf = "JUNGGU_PDF".equals(mode);
            boolean junggu = "JUNGGU_OBSERVATION".equals(mode)||jungguPdf;
            boolean gangbuk = "GANGBUK_OBSERVATION".equals(mode);
            boolean diagnostic = mode.endsWith("_DIAGNOSTIC") || namgu;
            boolean three = okcheon || boeun || namgu || dalseong || haman || junggu || gangbuk || selected;
            String group = gangbuk?"GANGBUK":jungguPdf?"JUNGGU_PDF":junggu?"JUNGGU":haman?"HAMAN":header?"DALSEONG_HEADER":dalseong?"DALSEONG":structure?"NAMGU_STRUCTURE":namgu ? "NAMGU" : boeun ? "BOEUN" : okcheon ? "OKCHEON" : "TAEBAEK";
            result.put("structureDiagnostic",structure);
            if (!"Linux".equals(System.getProperty("os.name")) || "root".equals(System.getProperty("user.name"))
                    || !"/work".equals(Path.of("").toAbsolutePath().toString())
                    || !"/work/tmp".equals(System.getProperty("java.io.tmpdir"))
                    || !"true".equals(System.getenv(selected?"SANEB_ATTACHMENT_GANGBUK_SELECTED_DOWNLOAD":fixed?"SANEB_ATTACHMENT_BBS_FIXED_CASE_QA":"SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION"))
                    || System.getenv().keySet().stream().filter(key->key.startsWith("SANEB_ATTACHMENT_")).count()!=1
                    || System.getenv(fixed?"SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION":"SANEB_ATTACHMENT_BBS_FIXED_CASE_QA")!=null
                    || !ENV.containsAll(System.getenv().keySet())) throw new IllegalStateException();
            result.put("verificationMode", mode);
            var json = new ObjectMapper();
            stage = "CODE_IDENTITY";
            String codeHash = new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash();
            if (!args[0].equals(codeHash)) throw new IllegalStateException();
            result.put("executionCodeHash", codeHash);
            stage = "JCE_TLS_RUNTIME";
            javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.net.ssl.SSLContext.getDefault();
            System.setProperty("saneb.attachment-observation.extractor", "/qa/extractor");
            System.setProperty("saneb.attachment-observation.report", "/work/reports");
            System.setProperty("saneb.attachment-observation.group", group);
            System.setProperty("saneb.attachment-observation.diagnostic-budget", Boolean.toString(diagnostic));
            if(fixed) {
                // 동일 승인44요청/80MiB에서 앞선 관측5요청/2,377,939bytes를 공제한다.
                System.setProperty("saneb.attachment-fixed.maximum-requests","39");
                System.setProperty("saneb.attachment-fixed.maximum-bytes","81508141");
            }
            stage = "JUNIT_EXECUTION";
            Instant startedAt = Instant.now();
            var listener = new SummaryGeneratingListener();
            var selector=selected?DiscoverySelectors.selectClass(GangbukSelectedDownloadDiagnosticTest.class)
                    :DiscoverySelectors.selectClass(fixed?AnnouncementAttachmentBbsFixedCaseQaTest.class:AnnouncementAttachmentBbsOfficialObservationTest.class);
            var request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selector)
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                    .configurationParameter("junit.jupiter.tempdir.cleanup.mode.default", "ALWAYS").build();
            LauncherFactory.create().execute(request, listener);
            var summary = listener.getSummary();
            result.put("found", summary.getTestsFoundCount());
            result.put("passed", summary.getTestsSucceededCount());
            result.put("failed", summary.getTestsFailedCount());
            result.put("skipped", summary.getTestsSkippedCount());
            result.put("aborted", summary.getTestsAbortedCount());
            result.put("failedContainers", summary.getContainersFailedCount());
            result.put("failureTypes", summary.getFailures().stream().map(f -> f.getException().getClass().getSimpleName()).distinct().limit(8).toList());
            stage = "REPORTS";
            var reports = new ArrayList<JsonNode>();
            if (three) result.put("reports", reports);
            for (String name : selected?List.of(GangbukSelectedDownloadDiagnosticTest.REPORT):gangbuk?List.of("GANGBUK-179490"):jungguPdf?List.of("JUNGGU-33626"):junggu?JUNGGU_CASES:haman?List.of("HAMAN-41306"):header?List.of("DALSEONG-51022"):dalseong?DALSEONG_CASES:structure?List.of("NAMGU-44381"):namgu ? NAMGU_CASES : boeun ? BOEUN_CASES : okcheon ? OKCHEON_CASES : List.of(fixed ? "TAEBAEK-184816-fixed-case" : "TAEBAEK-184816")) {
                Path path = Path.of("/work/reports", name + ".json");
                if (!Files.isRegularFile(path) || Files.size(path) > (gangbuk?262144:65536)) throw new IllegalStateException();
                reports.add(json.readTree(Files.readAllBytes(path)));
            }
            if (!three) result.put("report", reports.getFirst());
            stage = "FINAL_IDENTITY";
            if (!codeHash.equals(new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash())) throw new IllegalStateException();
            passed = selected ? selectGangbukSelectedComplete(reports,startedAt,Instant.now())
                    &&selectComplete(summary.getTestsFoundCount(),summary.getTestsSucceededCount(),summary.getTestsFailedCount(),summary.getTestsSkippedCount(),summary.getTestsAbortedCount(),summary.getContainersFailedCount())
                    : three ? selectThreeReportsComplete(reports, startedAt, Instant.now(), group, diagnostic)
                    && (structure||header||haman||jungguPdf||gangbuk?selectComplete(summary.getTestsFoundCount(), summary.getTestsSucceededCount(), summary.getTestsFailedCount(),
                            summary.getTestsSkippedCount(), summary.getTestsAbortedCount(), summary.getContainersFailedCount()):selectOkcheonComplete(summary.getTestsFoundCount(), summary.getTestsSucceededCount(), summary.getTestsFailedCount(),
                            summary.getTestsSkippedCount(), summary.getTestsAbortedCount(), summary.getContainersFailedCount()))
                    : (fixed ? selectFixedReportComplete(reports.getFirst()) : selectReportComplete(reports.getFirst()))
                    && selectComplete(summary.getTestsFoundCount(), summary.getTestsSucceededCount(), summary.getTestsFailedCount(),
                            summary.getTestsSkippedCount(), summary.getTestsAbortedCount(), summary.getContainersFailedCount());
        } catch (Exception | LinkageError | AssertionError failure) {
            result.put("failedStage", stage);
            result.put("failureCode", "BBS_OBSERVATION_INCOMPLETE");
            result.put("failureType", failure.getClass().getSimpleName());
        }
        if (result.get("reports") instanceof List<?> reports) {
            try { result.put("reports", reports.stream().map(report -> Boolean.TRUE.equals(result.get("structureDiagnostic"))
                    ?selectStructureTransportReport((JsonNode)report):selectTransportReport((JsonNode) report)).toList()); }
            catch(IllegalArgumentException invalid){result.remove("reports");result.put("failureCode","STRUCTURE_DIAGNOSTIC_INVALID");passed=false;}
        }
        result.put("status", passed ? "PASSED" : "INCOMPLETE");
        try { output.println(new ObjectMapper().writeValueAsString(result)); }
        catch (Exception ignored) { output.println("{\"kind\":\"BBS_OBSERVATION_PROBE\",\"status\":\"INCOMPLETE\"}"); }
        System.exit(passed ? 0 : 1);
    }
}
