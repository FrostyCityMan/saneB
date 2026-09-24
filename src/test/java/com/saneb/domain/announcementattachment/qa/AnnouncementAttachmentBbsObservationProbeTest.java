package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentBbsObservationProbeTest {
    private static final Instant START = Instant.parse("2026-09-22T01:00:00Z");
    private List<JsonNode> okcheonReports() {
        var reports = new ArrayList<JsonNode>();
        for (String code : AnnouncementAttachmentBbsObservationProbe.OKCHEON_CASES) {
            var node = report();
            node.put("caseCode", code).put("profileCode", "LOCAL_OKCHEON_BBS_V1")
                    .put("observedAt", START.plusSeconds(1).toString()).put("expectedListedFileCount", 1);
            node.remove("files");
            var files = node.putArray("files");
            if (code.equals("OKCHEON-193187")) {
                node.put("status", "TITLE_NOT_ELIGIBLE_NOT_FETCHED").put("titleStage", "COMBINATION_NOT_MATCHED")
                        .put("titleReason", "TITLE_COMBINATION_NOT_MATCHED").put("isWholeTextAnalysisComplete", false)
                        .put("requiresFinalAdminVerification", false).put("requestReservationsIncludingBodyUpperBound", 0)
                        .put("reservedBytesIncludingBodyUpperBound", 0);
                node.remove("bodyStageComplete");
            } else {
                node.put("titleStage", "COMBINATION_MATCHED").put("bodyStatus", "AVAILABLE")
                        .put("discoveryStatus", "FOUND").put("discoveryComplete", true).put("discoveredFileCount", 1)
                        .put("decisionStatus", "REVIEW_REQUIRED").put("isWholeTextAnalysisComplete", true);
                files.addObject().put("status", "OBSERVED").put("format", "HWPX").put("bytes", 1024)
                        .put("quality", "COMPLETE_TEXT").put("characterCount", 1000).put("blockCount", 30)
                        .putObject("roleAssessment").put("roleCode", "UNKNOWN");
            }
            reports.add(node);
        }
        return reports;
    }
    private boolean validOkcheon(List<JsonNode> reports) {
        return AnnouncementAttachmentBbsObservationProbe.selectOkcheonReportsComplete(reports, START, START.plusSeconds(60));
    }
    @Test void onlyExplicitModesCanChooseFixedScope() {
        String hash = "a".repeat(64);
        assertEquals("OBSERVATION", AnnouncementAttachmentBbsObservationProbe.selectMode(new String[]{hash}));
        for (String mode : List.of("FIXED", "OKCHEON", "BOEUN_OBSERVATION", "BOEUN_DIAGNOSTIC", "OKCHEON_DIAGNOSTIC", "NAMGU_OBSERVATION", "NAMGU_STRUCTURE", "DALSEONG_OBSERVATION"))
            assertEquals(mode, AnnouncementAttachmentBbsObservationProbe.selectMode(new String[]{hash, mode}));
        for (String[] args : new String[][]{{}, {"bad"}, {hash,"OTHER"}, {hash,"TAEBAEK_HWP"}, {hash,"OKCHEON","extra"}})
            assertThrows(IllegalArgumentException.class, () -> AnnouncementAttachmentBbsObservationProbe.selectMode(args));
    }
    @Test void okcheonRequiresAllThreeTestsIncludingNegativeSample() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectOkcheonComplete(3,3,0,0,0,0));
        for (long[] v : new long[][]{{0,0,0,0,0,0},{2,2,0,0,0,0},{3,2,1,0,0,0},{3,2,0,1,0,0},
                {3,2,0,0,1,0},{3,3,0,0,0,1},{4,4,0,0,0,0}})
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectOkcheonComplete(v[0],v[1],v[2],v[3],v[4],v[5]));
    }
    @Test void diagnosticModeUsesReducedBudgetAndRejectsThePriorLargerReceipt() throws Exception {
        for (boolean boeun : List.of(false,true)) {
            var reports=boeun?boeunReports():okcheonReports();
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(reports,START,START.plusSeconds(60),boeun,true));
            for(var report:reports)((ObjectNode)report).put("maximumRequestReservations",20).put("maximumReservedBytes",33554432);
            assertTrue(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(reports,START,START.plusSeconds(60),boeun,true));
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(reports,START,START.plusSeconds(60),boeun,false));
            ((ObjectNode)reports.getFirst()).put("requestReservationsIncludingBodyUpperBound",21);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(reports,START,START.plusSeconds(60),boeun,true));
            ((ObjectNode)reports.getFirst()).put("requestReservationsIncludingBodyUpperBound",4).put("reservedBytesIncludingBodyUpperBound",33554433);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(reports,START,START.plusSeconds(60),boeun,true));
        }
    }
    private List<JsonNode> dalseongReports() throws Exception {
        var reports=namguReports();
        for(int i=0;i<3;i++) {
            var row=(ObjectNode)reports.get(i);String code=AnnouncementAttachmentBbsObservationProbe.DALSEONG_CASES.get(i);
            var hashes=AnnouncementAttachmentBbsObservationProbe.selectDalseongBinaryHashes(code);int count=hashes.size();
            row.put("caseCode",code).put("profileCode","LOCAL_DAEGU_DALSEONG_GET_V1").put("maximumRequestReservations",6).put("maximumReservedBytes",24117248)
                    .put("expectedListedFileCount",count).put("discoveredFileCount",count).put("requestReservationsIncludingBodyUpperBound",count+3);
            var files=row.withArray("files");if(i==0)files.add(files.get(0).deepCopy());
            for(int j=0;j<count;j++)((ObjectNode)files.get(j)).put("binaryHash",hashes.get(j)).put("downloadAllowed",true).put("format",i==0&&j==0?"PDF":"HWP");
        }
        return reports;
    }
    private boolean validDalseong(List<JsonNode> reports) {
        return AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(reports,START,START.plusSeconds(60),"DALSEONG",false);
    }
    @Test void dalseongRequiresAllFourPinnedFilesAndDoesNotApproveExpectations() throws Exception {
        assertTrue(validDalseong(dalseongReports()));assertFalse(validDalseong(namguReports()));
        var rows=dalseongReports();rows.removeLast();assertFalse(validDalseong(rows));
        rows=dalseongReports();rows.set(1,rows.getFirst());assertFalse(validDalseong(rows));
        rows=dalseongReports();((ObjectNode)rows.getFirst()).withArray("files").remove(1);assertFalse(validDalseong(rows));
        for(String key:List.of("isPolicyQaPassed","isExpectationApproved")) {
            rows=dalseongReports();((ObjectNode)rows.getFirst()).put(key,true);assertFalse(validDalseong(rows));
        }
        for(String key:List.of("originalFilesRemoved","bodyStageComplete","discoveryComplete","requiresFinalAdminVerification")) {
            rows=dalseongReports();((ObjectNode)rows.getFirst()).put(key,false);assertFalse(validDalseong(rows));
        }
        for(String key:List.of("binaryHash","format","extractorVersion","quality","segmentAnalysisHash")) {
            rows=dalseongReports();((ObjectNode)rows.getFirst().at("/files/0")).put(key,"changed");assertFalse(validDalseong(rows));
        }
        for(String key:List.of("productionWriteCount","maximumRequestReservations","maximumReservedBytes","expectedListedFileCount","discoveredFileCount","requestReservationsIncludingBodyUpperBound")) {
            rows=dalseongReports();((ObjectNode)rows.getFirst()).put(key,"6");assertFalse(validDalseong(rows));
        }
        rows=dalseongReports();((ObjectNode)rows.getFirst()).put("observedAt",START.minusSeconds(1).toString());assertFalse(validDalseong(rows));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsObservationProbe.selectDalseongBinaryHashes("DALSEONG-53932"));
    }
    @Test void dalseongMixedPartialSetRemainsObservedButIncompleteAndReviewRequired() throws Exception {
        var rows=dalseongReports();var row=(ObjectNode)rows.getFirst();var file=(ObjectNode)row.at("/files/0");
        file.put("quality","PARTIAL_TEXT");file.remove(List.of("roleAssessment","segmentAnalysis","segmentAnalysisHash"));
        row.put("isWholeTextAnalysisComplete",false);assertTrue(validDalseong(rows));
        row.put("decisionStatus","ACCEPTED");assertFalse(validDalseong(rows));
        row.put("decisionStatus","REVIEW_REQUIRED").put("isWholeTextAnalysisComplete",true);assertFalse(validDalseong(rows));
    }
    private List<JsonNode> namguReports() throws Exception {
        var reports=boeunReports();
        for(int i=0;i<reports.size();i++) {
            var row=(ObjectNode)reports.get(i);
            row.put("caseCode",AnnouncementAttachmentBbsObservationProbe.NAMGU_CASES.get(i))
                    .put("profileCode","LOCAL_BUSAN_NAMGU_GET_V1").put("maximumRequestReservations",5).put("maximumReservedBytes",25165824);
            ((ObjectNode)row.at("/files/0")).put("format","HWP");
            ((ObjectNode)row.at("/files/0")).put("extractorVersion",com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity.EXTRACTOR_VERSION)
                    .putArray("hwpPartialCauses");
            ((ObjectNode)row.at("/files/0")).putObject("hwpStructure").put("sectionCount",1).put("recordCount",2)
                    .put("maximumLevel",1).putArray("recordTypes").addObject().put("tagId",67).put("count",2);
        }
        return reports;
    }
    private boolean validNamgu(List<JsonNode> reports) {
        return AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(reports,START,START.plusSeconds(60),"NAMGU",true);
    }
    private ObjectNode structureObservation() {
        var node=new ObjectMapper().createObjectNode().put("schemaVersion",1).put("scope","FIXED_TOKEN_STRUCTURE_ONLY")
                .put("inspectedNonblankLineCount",10).put("isLineLimitReached",false).put("signalMatchCount",1).put("isTruncated",false);
        node.putArray("signals").addObject().put("code","TARGET_LABEL").put("lineNumber",4)
                .put("startOffset",12).put("endOffset",22).put("blockIndex",3).put("isSingleReliableBlock",true)
                .put("isWithinInitialHeading",false).put("isWholeLineToken",false).put("isLineEndingToken",false)
                .put("hasColon",true).putArray("tokenIndexes").add(0).add(2);
        return node;
    }
    @Test void structureSummaryContainsOnlyFixedDictionaryAndNumericTuples() {
        var source=structureObservation();var result=AnnouncementAttachmentBbsObservationProbe.selectStructureSummary(source);
        assertEquals(14,result.path("ruleDictionary").size());
        assertEquals("[3,4,5,12,22,3,17]",result.path("signals").get(0).toString());
        assertTrue(source.has("scope"));
        assertFalse(result.toString().contains("startOffset"));
    }
    @Test void structureSummaryRejectsRawFieldsUnknownCodesMalformedValuesAndTruncation() {
        for(String key:List.of("raw","filename","url","locator")) {
            var input=structureObservation();((ObjectNode)input.path("signals").get(0)).put(key,"PRIVATE_CANARY");
            assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsObservationProbe.selectStructureSummary(input));
        }
        for(String key:List.of("isTruncated","isLineLimitReached")) {
            var input=structureObservation().put(key,true);
            assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsObservationProbe.selectStructureSummary(input));
        }
        for(String key:List.of("code","lineNumber","startOffset","endOffset","blockIndex","hasColon","isWholeLineToken")) {
            var input=structureObservation();((ObjectNode)input.path("signals").get(0)).put(key,"PRIVATE_CANARY");
            assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsObservationProbe.selectStructureSummary(input));
        }
        for(int index:new int[]{-1,5,999}) {
            var input=structureObservation();((ObjectNode)input.path("signals").get(0)).putArray("tokenIndexes").add(index);
            assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsObservationProbe.selectStructureSummary(input));
        }
    }
    @Test void structureModeIsOnePinnedCompleteFileAndCannotReplaceThreeCaseObservation() throws Exception {
        var row=(ObjectNode)namguReports().get(1);var file=(ObjectNode)row.at("/files/0");
        String textHash="7181d23cb9973622cf14e2a6edfbed42424b06ad13a91eb53ed55d158d77161a";
        file.put("binaryHash","69f7738308da99a68f528d2c08dae175e9880c0bb0764d6c5bcffd6e333548c8").put("textHash",textHash);
        ((ObjectNode)file.path("segmentAnalysis")).put("textHash",textHash);
        file.put("segmentAnalysisHash",AnnouncementAttachmentOfficialObservationTest.selectHash(file.path("segmentAnalysis")));
        file.set("roleStructureObservation",structureObservation());
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(List.of(row),START,START.plusSeconds(60),"NAMGU_STRUCTURE",true));
        assertFalse(validNamgu(List.of(row)));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(List.of(row,row),START,START.plusSeconds(60),"NAMGU_STRUCTURE",true));
        var transported=AnnouncementAttachmentBbsObservationProbe.selectStructureTransportReport(row);
        assertTrue(transported.at("/files/0/structureSummary").isObject());
        assertFalse(transported.at("/files/0").has("roleStructureObservation"));
        for(String key:List.of("binaryHash","textHash","quality")) {
            var invalid=row.deepCopy();((ObjectNode)invalid.at("/files/0")).put(key,"changed");
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(List.of(invalid),START,START.plusSeconds(60),"NAMGU_STRUCTURE",true));
        }
        assertEquals(List.of("NAMGU-44381"),AnnouncementAttachmentBbsOfficialObservationTest.selectCases("NAMGU_STRUCTURE").map(c->c.code()).toList());
    }
    @Test void namguRequiresAllThreeBoundedHwpReportsAndNeverApprovesPolicy() throws Exception {
        assertTrue(validNamgu(namguReports()));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectThreeReportsComplete(namguReports(),START,START.plusSeconds(60),"NAMGU",false));
        var missing=namguReports();missing.removeLast();assertFalse(validNamgu(missing));
        for(String key:List.of("isPolicyQaPassed","isExpectationApproved")) {
            var reports=namguReports();((ObjectNode)reports.getFirst()).put(key,true);assertFalse(validNamgu(reports));
        }
        var changed=namguReports();((ObjectNode)changed.getFirst().at("/files/0")).put("format","HWPX");assertFalse(validNamgu(changed));
        changed=namguReports();((ObjectNode)changed.getFirst()).put("caseCode","NAMGU-46034");assertFalse(validNamgu(changed));
        changed=namguReports();((ObjectNode)changed.getFirst()).put("requestReservationsIncludingBodyUpperBound",6);assertFalse(validNamgu(changed));
        changed=namguReports();((ObjectNode)changed.getFirst()).put("maximumRequestReservations",20).put("maximumReservedBytes",33554432);assertFalse(validNamgu(changed));
        changed=namguReports();((ObjectNode)changed.getFirst().at("/files/0")).remove("hwpStructure");assertFalse(validNamgu(changed));
        changed=namguReports();((ObjectNode)changed.getFirst().at("/files/0/hwpStructure")).put("recordCount",3);assertFalse(validNamgu(changed));
        changed=namguReports();((ObjectNode)changed.getFirst().at("/files/0")).remove("hwpPartialCauses");assertFalse(validNamgu(changed));
        changed=namguReports();((ObjectNode)changed.getFirst().at("/files/0")).put("extractorVersion","1.0.5");assertFalse(validNamgu(changed));
    }
    @Test void namguBudgetReservesBodyThenAllowsOnlyThreeMoreRequests() {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("NAMGU").findFirst().orElseThrow();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),true);
        budget.reserveBody(); assertEquals(2,budget.requests); assertEquals(2097152,budget.bytes);
        var request=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(sample.profile().selectDetailUri(sample.source()));
        for(int i=0;i<3;i++)assertTrue(budget.selectRequestAllowed(request));
        assertFalse(budget.selectRequestAllowed(request));assertEquals(5,budget.requests);
        assertTrue(budget.saveBytes(22L*1024*1024));assertFalse(budget.saveBytes(1));assertEquals(25165824,budget.bytes);
    }
    @Test void namguPartialExtractionRemainsIncompleteAndHasNoNormalExpectation() throws Exception {
        var reports=namguReports();((ObjectNode)reports.getFirst().at("/files/0")).put("quality","PARTIAL_TEXT");
        assertFalse(validNamgu(reports));
        ((ObjectNode)reports.getFirst()).put("isWholeTextAnalysisComplete",false);assertFalse(validNamgu(reports));
        ((ObjectNode)reports.getFirst().at("/files/0")).putArray("hwpPartialCauses").addObject().put("code","CELL_HEADER_INVALID").put("count",1);
        assertTrue(validNamgu(reports));
        var cases=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("NAMGU").toList();
        assertEquals(AnnouncementAttachmentBbsObservationProbe.NAMGU_CASES,cases.stream().map(c->c.code()).toList());
    }
    @Test void reducedBudgetReservesBodyFirstAndStopsBeforeAdditionalHttpOrBytes() {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("BOEUN").findFirst().orElseThrow();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),true);
        budget.reserveBody(); assertEquals(2,budget.requests); assertEquals(2097152,budget.bytes);
        var request=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(sample.profile().selectDetailUri(sample.source()));
        for(int i=0;i<18;i++)assertTrue(budget.selectRequestAllowed(request));
        assertFalse(budget.selectRequestAllowed(request)); assertEquals(20,budget.requests);
        assertTrue(budget.saveBytes(30L*1024*1024)); assertFalse(budget.saveBytes(1));
        assertEquals(33554432,budget.bytes);
    }
    @Test void temporaryScopeMatchesActualJUnitCaseSelection() {
        var cases = AnnouncementAttachmentBbsOfficialObservationTest.selectCases("OKCHEON").toList();
        assertEquals(AnnouncementAttachmentBbsObservationProbe.OKCHEON_CASES, cases.stream().map(c -> c.code()).toList());
        assertNull(cases.get(0).expectedTitleStopStage());
        assertNull(cases.get(1).expectedTitleStopStage());
        assertEquals("COMBINATION_NOT_MATCHED", cases.get(2).expectedTitleStopStage().name());
        assertTrue(cases.stream().allMatch(c -> c.listedFileCount() == 1
                && c.profile().selectProfileCode().equals("LOCAL_OKCHEON_BBS_V1")));
    }
    @Test void okcheonObservationsNeverApprovePolicyAndRetainIncompleteText() {
        assertTrue(validOkcheon(okcheonReports()));
        for (String quality : List.of("PARTIAL_TEXT", "OCR_REQUIRED", "ENCRYPTED", "CORRUPT", "UNSUPPORTED", "LIMIT_EXCEEDED")) {
            var reports = okcheonReports();
            ((ObjectNode)reports.getFirst().at("/files/0")).put("quality", quality);
            assertFalse(validOkcheon(reports));
            ((ObjectNode)reports.getFirst()).put("isWholeTextAnalysisComplete", false);
            assertTrue(validOkcheon(reports), quality);
        }
        for (String key : List.of("isPolicyQaPassed", "isExpectationApproved")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, true);
            assertFalse(validOkcheon(reports));
        }
    }
    @Test void okcheonRejectsMissingDuplicateAndChangedCaseOrProfile() {
        assertFalse(validOkcheon(null));
        var reports = okcheonReports(); reports.removeLast(); assertFalse(validOkcheon(reports));
        reports = okcheonReports(); reports.set(1, reports.getFirst()); assertFalse(validOkcheon(reports));
        reports = okcheonReports(); reports.set(1, null); assertFalse(validOkcheon(reports));
        for (String key : List.of("caseCode", "profileCode", "scope")) {
            reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, "OTHER"); assertFalse(validOkcheon(reports));
        }
    }
    @Test void okcheonRejectsStaleFutureAndMissingTimestamp() {
        for (String timestamp : List.of(START.minusSeconds(1).toString(), START.plusSeconds(61).toString(), "invalid", "")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put("observedAt", timestamp);
            assertFalse(validOkcheon(reports));
        }
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectOkcheonReportsComplete(okcheonReports(), null, START));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectOkcheonReportsComplete(okcheonReports(), START, START.minusSeconds(1)));
    }
    @Test void okcheonRejectsChangedBudgetsOperatingWritesAndCoercedTypes() {
        for (String key : List.of("expectedListedFileCount", "discoveredFileCount", "maximumRequestReservations",
                "maximumReservedBytes", "requestReservationsIncludingBodyUpperBound", "reservedBytesIncludingBodyUpperBound", "productionWriteCount")) {
            for (long value : new long[]{-1, 251658241}) {
                var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, value);
                assertFalse(validOkcheon(reports), key);
            }
            var reports = okcheonReports(); var node = (ObjectNode)reports.getFirst(); node.put(key, node.path(key).asText());
            assertFalse(validOkcheon(reports), key);
        }
        for (String key : List.of("originalFilesRemoved", "requiresFinalAdminVerification", "bodyStageComplete", "discoveryComplete")) {
            var reports = okcheonReports(); var node = (ObjectNode)reports.getFirst(); node.put(key, false);
            assertFalse(validOkcheon(reports)); node.put(key, "true"); assertFalse(validOkcheon(reports));
        }
    }
    @Test void negativeTitleMustNeverFetchBodyOrFiles() {
        for (String key : List.of("requestReservationsIncludingBodyUpperBound", "reservedBytesIncludingBodyUpperBound")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getLast()).put(key, 1); assertFalse(validOkcheon(reports));
        }
        for (String key : List.of("bodyStatus", "discoveryStatus", "titleReason", "status", "titleStage")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getLast()).put(key, "CHANGED"); assertFalse(validOkcheon(reports));
        }
        var reports = okcheonReports(); ((ObjectNode)reports.getLast()).withArray("files").addObject(); assertFalse(validOkcheon(reports));
        for (String key : List.of("isWholeTextAnalysisComplete", "requiresFinalAdminVerification")) {
            reports = okcheonReports(); ((ObjectNode)reports.getLast()).put(key, true); assertFalse(validOkcheon(reports));
        }
    }
    @Test void positiveTitlesRequireBodyWholeFileAndRoleEvidence() {
        for (String key : List.of("status", "titleStage", "bodyStatus", "discoveryStatus", "decisionStatus")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, "FAILED"); assertFalse(validOkcheon(reports));
        }
        for (String key : List.of("status", "format", "quality")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst().at("/files/0")).put(key, "FAILED"); assertFalse(validOkcheon(reports));
        }
        for (String key : List.of("bytes", "characterCount", "blockCount")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst().at("/files/0")).put(key, 0); assertFalse(validOkcheon(reports));
        }
        var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).withArray("files").removeAll(); assertFalse(validOkcheon(reports));
        reports = okcheonReports(); ((ObjectNode)reports.getFirst().at("/files/0")).remove("roleAssessment"); assertFalse(validOkcheon(reports));
    }
    private ObjectNode fixedReport() {
        var node=new ObjectMapper().createObjectNode();
        node.put("caseCode","TAEBAEK-184816").put("scope","FIXED_CASE_EXECUTOR_QA_EPHEMERAL_ONLY")
                .put("status","FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED").put("productionWriteCount",0)
                .put("originalFilesRemoved",true).put("isPolicyQaPassed",false)
                .put("isExpectationCoverageComplete",false).put("normalNotice",false);
        var result=node.putObject("result");
        result.put("scope","SINGLE_FIXED_NOTICE_PROVIDER_QA").put("caseId","TAEBAEK-184816")
                .put("status","PASSED").put("reasonCode","FIXED_NOTICE_EXPECTATIONS_MATCHED")
                .put("expectedFileCount",2).put("discoveredFileCount",2).put("discoveryComplete",true)
                .put("allTextComplete",true).put("originalFilesRemoved",true).put("isPolicyQaPassed",false)
                .put("requestReservations",3).put("reservedBytes",280787);
        var files=result.putArray("files");
        for(int i=0;i<2;i++)files.addObject().put("status","PASSED").put("quality","COMPLETE_TEXT").put("roleAssessmentHash","a".repeat(64));
        return node;
    }

    private List<JsonNode> boeunReports() throws Exception {
        var json = new ObjectMapper();
        var reports = new ArrayList<JsonNode>();
        String text = "지원 공고\n지원대상\n지원내용\n신청기간\n개인_CANARY\n신청서\n성명\n(인)";
        var observation = AnnouncementAttachmentOfficialObservationTest.selectTextObservation(json.valueToTree(java.util.Map.of(
                "qualityCode", "COMPLETE_TEXT", "text", text, "blocks", List.of(java.util.Map.of(
                "index", 0, "startOffset", 0, "endOffset", text.length(), "scopeReliable", true, "evidenceScopeId", "private-scope", "locator", "private-location")))));
        for (String code : AnnouncementAttachmentBbsObservationProbe.BOEUN_CASES) {
            var r = (ObjectNode) okcheonReports().getFirst();
            r.put("caseCode", code).put("profileCode", "LOCAL_BOEUN_BBS_V1");
            var file = (ObjectNode) r.at("/files/0");
            file.put("format", code.endsWith("218812") ? "PDF" : "HWPX");
            file.setAll((ObjectNode) json.valueToTree(observation)); reports.add(r);
        }
        return reports;
    }
    private boolean validBoeun(List<JsonNode> reports) {
        return AnnouncementAttachmentBbsObservationProbe.selectBoeunReportsComplete(reports, START, START.plusSeconds(60));
    }
    @Test void boeunObservationUsesFixedThreeCasesAndCurrentSegmentsWithoutChangingFileRoles() throws Exception {
        var cases = AnnouncementAttachmentBbsOfficialObservationTest.selectCases("BOEUN").toList();
        assertEquals(AnnouncementAttachmentBbsObservationProbe.BOEUN_CASES, cases.stream().map(c->c.code()).toList());
        assertTrue(cases.stream().allMatch(c->c.expectedTitleStopStage()==null && c.listedFileCount()==1
                && c.profile().selectProfileCode().equals("LOCAL_BOEUN_BBS_V1")));
        var reports = boeunReports(); assertTrue(validBoeun(reports)); assertFalse(validOkcheon(reports));
        assertFalse(validBoeun(okcheonReports()));
        assertEquals("UNKNOWN", reports.getFirst().at("/files/0/roleAssessment/roleCode").asText());
        assertEquals("RESOLVED", reports.getFirst().at("/files/0/segmentAnalysis/statusCode").asText());
        assertEquals("REVIEW_REQUIRED", reports.getFirst().path("decisionStatus").asText());
    }
    @Test void boeunCannotReuseWorkerModeOrOtherSourceOrIncompleteFileEvidence() throws Exception {
        for (String field : List.of("segmentAnalysis", "segmentAnalysisHash", "textHash")) {
            var reports = boeunReports(); ((ObjectNode) reports.getFirst().at("/files/0")).remove(field); assertFalse(validBoeun(reports));
        }
        for (String field : List.of("textHash", "rulesHash", "blocksHash", "analysisVersion")) {
            var reports = boeunReports(); ((ObjectNode) reports.getFirst().at("/files/0/segmentAnalysis")).put(field,"changed"); assertFalse(validBoeun(reports));
        }
        for (String field : List.of("bodyStatus", "caseCode", "profileCode", "status", "scope")) {
            var reports = boeunReports(); ((ObjectNode) reports.getFirst()).put(field,"changed"); assertFalse(validBoeun(reports));
        }
        var reports=boeunReports(); ((ObjectNode)reports.getLast().at("/files/0")).put("format","HWPX"); assertFalse(validBoeun(reports));
        reports=boeunReports(); reports.removeLast(); assertFalse(validBoeun(reports));
        reports=boeunReports(); reports.set(1,reports.getFirst()); assertFalse(validBoeun(reports));
    }
    @Test void boeunPartialQualityRemainsIncompleteAndNeverBecomesPolicyQaSuccess() throws Exception {
        var reports=boeunReports(); var file=(ObjectNode)reports.getFirst().at("/files/0");file.put("quality","PARTIAL_TEXT");
        file.remove(List.of("segmentAnalysis","segmentAnalysisHash","roleAssessment")); assertFalse(validBoeun(reports));
        ((ObjectNode)reports.getFirst()).put("isWholeTextAnalysisComplete",false);assertTrue(validBoeun(reports));
        ((ObjectNode)reports.getFirst()).put("isPolicyQaPassed",true);assertFalse(validBoeun(reports));
    }
    @Test void transportSummaryKeepsAllRolesAndFullAnalysisHashWithoutRawTextOrHugeCoordinates() throws Exception {
        var json=new ObjectMapper();var reports=boeunReports();
        for(var report:reports){
            var segments=(com.fasterxml.jackson.databind.node.ArrayNode)report.at("/files/0/segmentAnalysis/segments");
            var first=segments.get(0).deepCopy();segments.removeAll();for(int i=0;i<200;i++)segments.add(first.deepCopy());
        }
        var compact=reports.stream().map(AnnouncementAttachmentBbsObservationProbe::selectTransportReport).toList();
        for(int i=0;i<3;i++){
            var file=compact.get(i).at("/files/0");
            assertEquals(200,file.at("/segmentSummary/segmentCount").asInt());
            assertEquals(200,file.at("/segmentSummary/roleCodes").size());
            assertEquals(200,file.at("/segmentSummary/evidenceRuleMasks").size());
            assertEquals(11,file.at("/segmentSummary/evidenceRuleDictionary").size());
            assertEquals(reports.get(i).at("/files/0/segmentAnalysisHash"),file.path("segmentAnalysisHash"));
            assertFalse(file.has("roleStructureObservation"));assertFalse(file.has("segmentAnalysis"));
            assertTrue(reports.get(i).at("/files/0").has("segmentAnalysis"));
            assertEquals("METADATA_SUMMARY_FULL_ANALYSIS_HASH",compact.get(i).path("reportRepresentation").asText());
        }
        String output=json.writeValueAsString(compact);
        assertTrue(output.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<18000);
        for(String forbidden:List.of("CANARY","private-scope","private-location","startOffset","endOffset"))assertFalse(output.contains(forbidden));
    }
    @Test void compactRuleMasksPreserveMatchedPrefixWithoutInventingMissingConditions() throws Exception {
        var reports=boeunReports();
        var segment=(ObjectNode)reports.getFirst().at("/files/0/segmentAnalysis/segments/0");
        segment.put("roleCode","UNKNOWN").put("reasonCode","ROLE_STRUCTURE_INCOMPLETE");
        var evidence=segment.putArray("evidence");
        evidence.addObject().put("ruleCode","NOTICE_HEADING"); evidence.addObject().put("ruleCode","TARGET_SECTION");
        evidence.addObject().put("ruleCode","TARGET_SECTION");
        var output=AnnouncementAttachmentBbsObservationProbe.selectTransportReport(reports.getFirst());
        assertEquals(17,output.at("/files/0/segmentSummary/evidenceRuleMasks/0").asInt());
        assertEquals("UNKNOWN",output.at("/files/0/segmentSummary/roleCodes/0").asText());
        assertEquals("TARGET_SECTION",output.at("/files/0/segmentSummary/evidenceRuleDictionary/4").asText());
        evidence.addObject().put("ruleCode","PRIVATE_CANARY");
        var failure=assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsObservationProbe.selectTransportReport(reports.getFirst()));
        assertEquals("SEGMENT_DIAGNOSTIC_RULE_UNKNOWN",failure.getMessage());
    }
    @Test void compactReportKeepsPartialHwpxDiagnosticWithoutInventingSegments() throws Exception {
        var report=boeunReports().getFirst(); var file=(ObjectNode)report.at("/files/0");
        file.put("quality","PARTIAL_TEXT"); file.remove(List.of("segmentAnalysis","segmentAnalysisHash","roleAssessment","roleAssessmentHash"));
        file.putObject("hwpxStructure").put("sectionCount",1).put("paragraphCount",12).put("pictureCount",3)
                .put("oleCount",0).put("equationCount",0).put("replacementCharacterCount",0);
        var output=AnnouncementAttachmentBbsObservationProbe.selectTransportReport(report).at("/files/0");
        assertEquals(file.path("hwpxStructure"),output.path("hwpxStructure"));
        assertEquals("PARTIAL_TEXT",output.path("quality").asText());
        assertFalse(output.has("segmentSummary")); assertFalse(output.has("segmentAnalysisHash"));
    }
    @Test void fixedComparisonRequiresEveryFileAndNeverApprovesWholeCoverage() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(fixedReport()));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(null));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(report()));
        for(String key:new String[]{"isPolicyQaPassed","isExpectationCoverageComplete","normalNotice"}) {
            var changed=fixedReport();changed.put(key,true);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
            changed.remove(key);assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        }
        var changed=fixedReport();changed.withObject("/result").withArray("files").remove(0);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        changed=fixedReport();((ObjectNode)changed.at("/result/files/0")).put("quality","PARTIAL_TEXT");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        changed=fixedReport();((ObjectNode)changed.at("/result/files/0")).remove("roleAssessmentHash");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        changed=fixedReport();changed.withObject("/result").put("caseId","TAEBAEK-176153");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
    }
    @Test void fixedComparisonReservesOnlyRemainingApprovedBudget() {
        for(String key:new String[]{"requestReservations","reservedBytes"}) {
            var changed=fixedReport();changed.withObject("/result").put(key,key.equals("requestReservations")?40:81508142);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
            changed.withObject("/result").put(key,0);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        }
        for(String key:new String[]{"allTextComplete","originalFilesRemoved","discoveryComplete"}) {
            var changed=fixedReport();changed.withObject("/result").put(key,false);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        }
        var changed=fixedReport();changed.put("productionWriteCount",1);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
    }
    private ObjectNode report() {
        var node = new ObjectMapper().createObjectNode();
        node.put("caseCode", "TAEBAEK-184816").put("scope", "OFFICIAL_THREE_STAGE_OBSERVATION_V1")
                .put("status", "OBSERVED_NOT_VALIDATED").put("productionWriteCount", 0)
                .put("isPolicyQaPassed", false).put("isExpectationApproved", false)
                .put("originalFilesRemoved", true).put("requiresFinalAdminVerification", true)
                .put("bodyStageComplete", true).put("maximumRequestReservations", 44)
                .put("maximumReservedBytes", 83886080L).put("requestReservationsIncludingBodyUpperBound", 5)
                .put("reservedBytesIncludingBodyUpperBound", 2377939);
        node.putArray("files").addObject();
        node.withArray("files").addObject();
        return node;
    }
    @Test void requiresOneActualTestWithoutFailureOrSkip() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectComplete(1, 1, 0, 0, 0, 0));
        for (long[] values : new long[][]{{0,0,0,0,0,0},{2,2,0,0,0,0},{1,0,1,0,0,0},
                {1,0,0,1,0,0},{1,0,0,0,1,0},{1,1,0,0,0,1}})
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectComplete(values[0], values[1], values[2], values[3], values[4], values[5]));
    }
    @Test void observationDoesNotPublishPolicyOrApproveExpectations() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(report()));
        for (String key : new String[]{"isPolicyQaPassed", "isExpectationApproved"}) {
            var changed = report(); changed.put(key, true);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
            changed.remove(key);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        }
    }
    @Test void requiresBodyCleanupAndFinalAdministratorVerification() {
        for (String key : new String[]{"bodyStageComplete", "originalFilesRemoved", "requiresFinalAdminVerification"}) {
            var changed = report(); changed.put(key, false);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        }
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(null));
    }
    @Test void rejectsScopeDriftIncompleteAndOperatingWrites() {
        var changed = report(); changed.put("caseCode", "TAEBAEK-176153");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("scope", "OFFICIAL_WORKER_EPHEMERAL_DB_API_V1");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("status", "INCOMPLETE");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("productionWriteCount", 1);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
    }
    @Test void rejectsMissingFilesAndBudgetChanges() {
        var changed = report(); changed.withArray("files").remove(0);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        for (String key : new String[]{"maximumRequestReservations", "maximumReservedBytes",
                "requestReservationsIncludingBodyUpperBound", "reservedBytesIncludingBodyUpperBound"}) {
            changed = report(); changed.put(key, -1);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        }
        changed = report(); changed.put("reservedBytesIncludingBodyUpperBound", 83886081L);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("requestReservationsIncludingBodyUpperBound", 45);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
    }
}
