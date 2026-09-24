package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentOfficialWorkerProbeTest {
    @Test void structuralModeHasSeparatePinnedExecutionAndNeverReusesQuarterProof() throws Exception {
        String mode="BOEUN_STRUCTURAL";
        assertEquals(AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("BOEUN"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(mode));
        assertEquals("BOEUN",AnnouncementAttachmentOfficialWorkerProbe.selectObservationGroup(mode));
        assertEquals(5,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests(mode));
        assertEquals(25165824,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes(mode));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(mode,3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(mode,3,2,0,1,0,0));
        assertEquals("segment-role-1.0.2",AnnouncementAttachmentOfficialWorkerProbe.selectSegmentVersion("BOEUN_SEGMENT"));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectSegmentVersion("BOEUN"));
        var sample=com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("BOEUN").findFirst().orElseThrow();
        var execution=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(mode,sample,"1.0.5","b".repeat(64));
        assertEquals("segment-role-1.0.3",execution.segmentRuleVersion());assertTrue(execution.selectEngineCurrent());
        assertEquals(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH,execution.segmentRulesHash());
        var policy=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPolicyConfiguration(execution);
        assertEquals(execution.segmentRuleVersion(),policy.segmentRuleVersion());assertEquals(execution.segmentRulesHash(),policy.segmentRulesHash());
        assertEquals(25165824L,policy.maximumSourceBytes());
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        for(String code:AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(mode)) {
            boolean pdf=code.equals("BOEUN-218812"),first=code.equals("BOEUN-221499");
            var report=json.createObjectNode().put("caseCode",code).put("engineVersion",execution.engineVersion())
                    .put("segmentRuleVersion",execution.segmentRuleVersion()).put("segmentRulesHash",execution.segmentRulesHash())
                    .put("segmentDatabaseApiVerified",true).put("segmentReviewContextVerified",true).put("manualSourceCheckRequired",true)
                    .put("decisionStatus","REVIEW_REQUIRED").put("maximumRequestReservations",5).put("maximumReservedBytes",25165824)
                    .put("requestReservationsIncludingBodyUpperBound",4).put("reservedBytesIncludingBodyUpperBound",2400000);
            var file=report.putArray("files").addObject().put("quality","COMPLETE_TEXT")
                    .put("segmentAnalysisHash",AnnouncementAttachmentOfficialWorkerProbe.selectStructuralObservedHash(code))
                    .put("segmentCount",pdf?1:first?6:4).put("unknownSegmentCount",pdf?1:first?3:2).put("noticeSegmentCount",pdf?0:1);
            var flags=java.util.List.of("segmentEvaluationInputBound","segmentApiProjectionMatched","legacyDefaultReadOnlyVerified",
                    "structuralObservedHashMatched","evaluationBoundApiVerified","otherVersionReadOnlyVerified");
            flags.forEach(key->file.put(key,true));
            assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,report),code);
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report));
            for(String flag:flags) for(Object value:java.util.List.of(false,"true")) {
                var invalid=report.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)invalid.path("files").get(0)).set(flag,json.valueToTree(value));
                assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,invalid),code+":"+flag);
            }
            for(String flag:java.util.List.of("segmentDatabaseApiVerified","segmentReviewContextVerified","manualSourceCheckRequired"))
                assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,report.deepCopy().put(flag,"true")));
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,report.deepCopy().put("segmentRuleVersion","segment-role-1.0.2")));
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,report.deepCopy().put("decisionStatus","ACCEPTED")));
            for(String key:java.util.List.of("segmentCount","unknownSegmentCount","noticeSegmentCount")) {
                var invalid=report.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)invalid.path("files").get(0)).put(key,file.path(key).intValue()+1);
                assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,invalid),key);
            }
            file.put("segmentAnalysisHash",AnnouncementAttachmentOfficialWorkerProbe.selectQuarterObservedHash(code));
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,report));
            file.put("segmentAnalysisHash",AnnouncementAttachmentOfficialWorkerProbe.selectStructuralObservedHash(code));file.putObject("structuralCandidate");
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,report));
        }
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectStructuralObservedHash("UNKNOWN"));
    }
    @Test void structuralComparisonReportsOnlyCandidateMetadataAndRejectsMissingProof() throws Exception {
        String text="공고문\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월\n3. 신청안내\n신청기간: 9월";
        var input=new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,
                java.util.List.of(new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block(0,0,text.length(),"p:0",true,"p:0")),1,0);
        var analyzer=new com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer();
        var quarter=analyzer.selectAnalysis(input,com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.QUARTER_VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH);
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode report=json.valueToTree(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectStructuralComparison(input,quarter));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectStructuralComparisonComplete(report));
        assertEquals("RESOLVED",report.path("statusCode").asText());assertEquals("NOTICE",report.path("roles").get(0).asText());
        assertFalse(report.toString().contains("소상공인"));assertFalse(report.path("persistedOrApplied").asBoolean());
        for(String key:java.util.List.of("analysisVersion","rulesHash","analysisHash","sameInputAndCoverageVerified","persistedOrApplied","roles","reasons","statusCode")) {
            var invalid=report.deepCopy();invalid.remove(key);assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectStructuralComparisonComplete(invalid),key);
        }
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectStructuralComparisonComplete(report.deepCopy().put("persistedOrApplied",true)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectStructuralComparisonComplete(report.deepCopy().put("sameInputAndCoverageVerified","true")));
    }
    @Test void segmentModePreservesFixedBoeunDenominatorAndUsesSmallerBudget() {
        assertEquals(AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("BOEUN"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("BOEUN_SEGMENT"));
        assertEquals("BOEUN",AnnouncementAttachmentOfficialWorkerProbe.selectObservationGroup("BOEUN_SEGMENT"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentMode("BOEUN_SEGMENT"));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentMode("BOEUN"));
        assertEquals(5,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests("BOEUN_SEGMENT"));
        assertEquals(25165824,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes("BOEUN_SEGMENT"));
        assertEquals(44,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests("BOEUN"));
        assertEquals(83886080,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes("BOEUN"));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests("ALL_SEGMENT"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("BOEUN_SEGMENT",3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("BOEUN_SEGMENT",3,2,0,1,0,0));
    }
    @Test void segmentExecutionAndEphemeralPolicyPinBothRulesWithoutChangingLegacyMode() throws Exception {
        var sample=com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("BOEUN").findFirst().orElseThrow();
        var legacy=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution("BOEUN",sample,"1.0.5","b".repeat(64));
        var segment=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution("BOEUN_SEGMENT",sample,"1.0.5","b".repeat(64));
        assertEquals("attachment-1.0.0",legacy.engineVersion());assertNull(legacy.segmentRuleVersion());
        assertEquals("attachment-segment-1.0.0",segment.engineVersion());assertTrue(segment.selectEngineCurrent());
        assertEquals("segment-role-1.0.2",segment.segmentRuleVersion());
        var config=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPolicyConfiguration(segment);
        assertEquals(segment.segmentRuleVersion(),config.segmentRuleVersion());assertEquals(segment.segmentRulesHash(),config.segmentRulesHash());
        assertEquals(25165824L,config.maximumSourceBytes());assertTrue(config.selectEngineCurrent());
        var oldConfig=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPolicyConfiguration(legacy);
        assertEquals(83886080L,oldConfig.maximumSourceBytes());assertNull(oldConfig.segmentRuleVersion());
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(oldConfig).contains("segmentRule"));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution("TAEBAEK",sample,"1.0.5","b".repeat(64)));
    }
    @Test void segmentReportRequiresDbApiBindingAndExactBudgetNotJustOldWorkerSuccess() {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var report=json.createObjectNode().put("caseCode","BOEUN-221499").put("engineVersion","attachment-segment-1.0.0").put("segmentRuleVersion","segment-role-1.0.2")
                .put("segmentRulesHash",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH)
                .put("segmentDatabaseApiVerified",true).put("segmentReviewContextVerified",true).put("manualSourceCheckRequired",true)
                .put("maximumRequestReservations",5).put("maximumReservedBytes",25165824)
                .put("requestReservationsIncludingBodyUpperBound",4).put("reservedBytesIncludingBodyUpperBound",2400000);
        var file=report.putArray("files").addObject().put("quality","COMPLETE_TEXT")
                .put("segmentAnalysisHash",AnnouncementAttachmentOfficialWorkerProbe.selectQuarterObservedHash("BOEUN-221499"))
                .put("segmentCount",8).put("unknownSegmentCount",5).put("segmentEvaluationInputBound",true).put("segmentApiProjectionMatched",true)
                .put("legacyDefaultReadOnlyVerified",true).put("quarterObservedHashMatched",true).put("noticeSegmentCount",1);
        var comparison=file.putObject("structuralCandidate").put("analysisVersion","segment-role-1.0.3")
                .put("rulesHash",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH)
                .put("analysisHash","a".repeat(64)).put("sameInputAndCoverageVerified",true).put("persistedOrApplied",false).put("statusCode","REVIEW_REQUIRED");
        comparison.putArray("roles").add("UNKNOWN");comparison.putArray("reasons").add("INITIAL_HEADING_REQUIRED");
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report));
        for(String key:java.util.List.of("caseCode","engineVersion","segmentRuleVersion","segmentRulesHash","segmentDatabaseApiVerified","segmentReviewContextVerified","manualSourceCheckRequired","maximumRequestReservations","maximumReservedBytes","files")) {
            var invalid=report.deepCopy();invalid.remove(key);assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(invalid),key);
        }
        for(String key:java.util.List.of("segmentAnalysisHash","segmentCount","unknownSegmentCount","segmentEvaluationInputBound","segmentApiProjectionMatched","legacyDefaultReadOnlyVerified","quarterObservedHashMatched","noticeSegmentCount","structuralCandidate")) {
            var invalid=report.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)invalid.path("files").get(0)).remove(key);
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(invalid),key);
        }
        var old=report.deepCopy().put("maximumRequestReservations",44).put("maximumReservedBytes",83886080);
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(old));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("requestReservationsIncludingBodyUpperBound",6)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("segmentRuleVersion","segment-role-1.0.0")));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("caseCode","BOEUN-UNKNOWN")));
        for(String flag:java.util.List.of("legacyDefaultReadOnlyVerified","quarterObservedHashMatched")) {
            var invalid=report.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)invalid.path("files").get(0)).put(flag,"true");
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(invalid));
        }
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("reservedBytesIncludingBodyUpperBound",25165825)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("segmentRulesHash","b".repeat(64))));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("segmentReviewContextVerified",false)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("segmentReviewContextVerified","true")));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("manualSourceCheckRequired",false)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report.deepCopy().put("manualSourceCheckRequired","true")));
        file.put("unknownSegmentCount",9);assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report));
        file.put("unknownSegmentCount",5).put("quality","PARTIAL_TEXT");assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(report));
    }
    @Test void candidateComparisonKeepsUnknownAndNeverClaimsDatabaseApplication() throws Exception {
        String text="별도 서문\n사업 지원 안내\n❍(신청자격) 소상공인\n❍(지원내용) 지원금\n❍(신청기간) 9월";
        var input=new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,
                java.util.List.of(new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block(0,0,text.length(),"p:0",true,"p:0")),1,0);
        var legacy=new com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer().selectAnalysis(input);
        var result=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectCandidateSegmentComparison(input,legacy);
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode candidate=json.valueToTree(result);
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectCandidateComparisonComplete(candidate,2));
        assertEquals("REVIEW_REQUIRED",candidate.path("statusCode").asText());
        assertEquals("UNKNOWN",candidate.path("segments").get(0).path("roleCode").asText());
        assertEquals("GUIDE",candidate.path("segments").get(1).path("roleCode").asText());
        assertEquals(3,candidate.path("sectionLayout").size());
        for(var item:candidate.path("sectionLayout")) {
            assertEquals(1,item.path("segmentIndex").intValue());assertTrue(item.path("fullLineGrammarMatches").booleanValue());
            assertTrue(item.path("scopeReliable").booleanValue());
        }
        assertFalse(json.writeValueAsString(result).contains("소상공인"));
        for(String key:java.util.List.of("analysisVersion","rulesHash","analysisHash","statusCode","sameInputAndBoundariesVerified","persistedOrApplied","segments","sectionLayout")) {
            var missing=candidate.deepCopy();missing.remove(key);assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectCandidateComparisonComplete(missing,2),key);
        }
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectCandidateComparisonComplete(candidate.deepCopy().put("persistedOrApplied",true),2));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectCandidateComparisonComplete(candidate.deepCopy().put("sameInputAndBoundariesVerified","true"),2));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectCandidateComparisonComplete(candidate,1));
    }
    @Test void sectionDiagnosticSeparatesPrefixFromGuideAndDoesNotExposeValues() throws Exception {
        String text="❍(신청자격) 진단용비공개값\n사업 지원 안내\n❍(지원내용) 지원금\n❍(신청기간) 9월";
        var input=new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,
                java.util.List.of(new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block(0,0,text.length(),"p:0",true,"p:0")),1,0);
        var analysis=new com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer().selectAnalysis(input);
        var rows=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectSectionLayout(input,analysis);
        assertEquals(3,rows.size());assertEquals(0,rows.getFirst().get("segmentIndex"));assertEquals(1,rows.getLast().get("segmentIndex"));
        String encoded=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rows);
        assertFalse(encoded.contains("진단용비공개값"));assertFalse(encoded.contains("p:0"));
    }
    @Test void quarterComparisonChecksEntireCoverageButDoesNotRequireUnchangedBoundaries() throws Exception {
        String text="기관 공고 번호\n참여자 모집 공고(3분기)\n❍(신청자격) 소상공인\n❍(지원내용) 지원금\n❍(신청기간) 9월";
        var input=new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,
                java.util.List.of(new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block(0,0,text.length(),"p:0",true,"p:0")),1,0);
        var legacy=new com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer().selectAnalysis(input);
        var result=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectQuarterHeadingComparison(input,legacy);
        com.fasterxml.jackson.databind.node.ObjectNode encoded=new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(result);
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectQuarterComparisonComplete(encoded));
        assertEquals(java.util.List.of("UNKNOWN","NOTICE"),result.get("roles"));
        assertEquals(java.util.List.of(4),result.get("noticeEvidenceCounts"));
        for(String key:java.util.List.of("analysisVersion","rulesHash","analysisHash","statusCode","sameInputAndCoverageVerified","persistedOrApplied","roles","noticeEvidenceCounts")) {
            var missing=encoded.deepCopy();missing.remove(key);assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectQuarterComparisonComplete(missing),key);
        }
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectQuarterComparisonComplete(encoded.deepCopy().put("persistedOrApplied",true)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectQuarterComparisonComplete(encoded.deepCopy().put("sameInputAndCoverageVerified","true")));
    }
    @Test void boeunPreservesThreePositiveNoticesWithOneFileEach() {
        var expected=java.util.List.of("BOEUN-221499","BOEUN-221497","BOEUN-218812");
        assertEquals(expected,AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("BOEUN"));
        assertEquals(expected,com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest
                .selectCases("BOEUN").map(c->c.code()).toList());
        assertEquals(java.util.List.of(1,1,1),expected.stream().map(AnnouncementAttachmentOfficialWorkerProbe::selectExpectedExtractionCount).toList());
        for(var code:expected)assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(code));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("BOEUN",3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("BOEUN",2,2,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("BOEUN",3,2,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("BOEUN",3,3,0,0,0,1));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("BOEUN-UNKNOWN"));
    }
    @Test void jecheonKeepsTitleNegativeAndEveryFileInBothPositiveNotices() {
        var expected=java.util.List.of("JECHEON-403587","JECHEON-403530","JECHEON-403490");
        assertEquals(expected,AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("JECHEON"));
        assertEquals(expected,com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest
                .selectCases("JECHEON").map(c->c.code()).toList());
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(expected.get(0)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(expected.get(1)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(expected.get(2)));
        assertEquals(java.util.List.of(0,1,2),expected.stream().map(AnnouncementAttachmentOfficialWorkerProbe::selectExpectedExtractionCount).toList());
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("JECHEON",3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("JECHEON",2,2,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("JECHEON",3,2,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("JECHEON",3,3,0,0,0,1));
    }
    @Test void extractionDenominatorRejectsUnknownAndPreservesUnsupportedAndTitleStops() {
        assertEquals(2,AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("TAEBAEK-184816"));
        assertEquals(1,AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("YANGPYEONG-311846"));
        assertEquals(0,AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("CHUNGJU-72625"));
        assertEquals(1,AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("CHUNGJU-70852"));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("UNKNOWN"));
    }
    @Test void chungjuHasTwoFixedTitleStopsAndOneActualWorkerCandidate() {
        var expected=java.util.List.of("CHUNGJU-72625","CHUNGJU-72039","CHUNGJU-70852");
        assertEquals(expected,AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("CHUNGJU"));
        assertEquals(expected,com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest
                .selectCases("CHUNGJU").map(c->c.code()).toList());
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(expected.get(0)));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(expected.get(1)));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(expected.get(2)));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("CHUNGJU",3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("CHUNGJU",1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("CHUNGJU",3,2,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("CHUNGJU",3,3,0,0,0,1));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected("CHUNGJU-UNKNOWN"));
    }
    @Test void chungjuCaseDefinitionLoadsWithoutTheUnpackagedLiveQaClass() throws Exception {
        String definition="com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest";
        var location=getClass().getProtectionDomain().getCodeSource().getLocation();
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{location},getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                synchronized(getClassLoadingLock(name)) {
                    if(name.startsWith("com.saneb.domain.announcementattachment.qa.ChungjuEminwonProfileLiveQaTest"))throw new ClassNotFoundException("LIVE_QA_NOT_PACKAGED");
                    if(name.equals(definition)||name.startsWith(definition+"$")) {
                        Class<?> type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;
                    }
                    return super.loadClass(name,resolve);
                }
            }
        }) {
            var type=Class.forName(definition,true,loader);
            try(var samples=(java.util.stream.Stream<?>)type.getMethod("selectCases",String.class).invoke(null,"CHUNGJU")) {
                assertEquals(3,samples.count());
            }
        }
    }
    @Test void replacementCharacterDiagnosticCountsOnlyLossMarkersAndPreservesMissingText() throws Exception {
        assertNull(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount(null));
        assertEquals(0L,AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount(""));
        assertEquals(0L,AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount("한글 😀 정상"));
        var count=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount("private fixture \ufffd 😀 \ufffd");
        assertEquals(2L,count);
        var output=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("replacementCharacterCount",count));
        assertEquals("{\"replacementCharacterCount\":2}",output);
    }
    @Test void hwpNoticeUsesExistingTaebaekProfileAndAnIndependentSingleCaseDenominator() {
        var sample=com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("TAEBAEK_HWP").toList();
        assertEquals(1,sample.size());assertEquals("TAEBAEK-176153",sample.getFirst().code());assertEquals(1,sample.getFirst().listedFileCount());
        assertEquals("LOCAL_TAEBAEK_BBS_V1",sample.getFirst().profile().selectProfileCode());
        assertTrue(sample.getFirst().profile().selectApprovedRequest(sample.getFirst().profile().selectDetailUri(sample.getFirst().source())));
        assertEquals(java.util.List.of("TAEBAEK-176153"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("TAEBAEK_HWP"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK_HWP",1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK_HWP",1,0,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK_HWP",2,2,0,0,0,0));
    }
    @Test void fixedCaseSharesBoundedRequestsAndIdempotentResourceOwnership() throws Exception {
        var nano=new java.util.concurrent.atomic.AtomicLong();
        var control=new AnnouncementAttachmentOfficialWorkerIntegrationTest.FixedCaseControl(nano::get);
        assertNull(control.selectDownloadPermit("invalid"));var first=control.selectExtractionPermit();assertNotNull(first);
        assertNull(control.selectExtractionPermit());first.close();var second=control.selectDownloadPermit("a".repeat(64));assertNotNull(second);
        first.close();assertTrue(control.inUse.get());second.close();assertFalse(control.inUse.get());
        for(int i=0;i<44;i++)assertTrue(control.saveRequestReservation());assertFalse(control.saveRequestReservation());
        assertFalse(control.saveByteReservation(0));assertFalse(control.saveByteReservation(-1));assertFalse(control.saveByteReservation(Long.MAX_VALUE));
        assertTrue(control.saveByteReservation(83886080));assertFalse(control.saveByteReservation(1));
        nano.set(420_000_000_000L);assertFalse(control.selectExecutionAllowed());assertNull(control.selectExtractionPermit());
    }
    @Test void interruptedFixedCaseCannotReserveOrAcquireResources() {
        var control=new AnnouncementAttachmentOfficialWorkerIntegrationTest.FixedCaseControl(System::nanoTime);
        Thread.currentThread().interrupt();
        try {assertFalse(control.selectExecutionAllowed());assertFalse(control.saveRequestReservation());assertFalse(control.saveByteReservation(1));assertNull(control.selectExtractionPermit());}
        finally {Thread.interrupted();}
        assertEquals(0,control.requests);assertEquals(0,control.bytes);
    }
    @Test void reviewFingerprintUsesCanonicalRoleEvidenceWithoutCopyingOriginalText() throws Exception {
        var role=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.RULES_HASH,
                "a".repeat(64),"b".repeat(64),"UNKNOWN","INITIAL_HEADING_REQUIRED",java.util.List.of());
        var result=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleFingerprint(role);
        var executor=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCaseExecutor(
                null,null,null,null,null,null,new com.fasterxml.jackson.databind.ObjectMapper());
        assertEquals(executor.selectHash(role),result.get("assessmentHash"));
        var locator=new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"z\":\"private fixture\",\"a\":{\"z\":2,\"a\":1}}");
        assertEquals(executor.selectHash(locator),AnnouncementAttachmentOfficialWorkerIntegrationTest.selectCanonicalHash(locator));
        assertEquals(java.util.Set.of("ruleVersion","rulesHash","roleCode","reasonCode","textHash","blocksHash","assessmentHash"),result.keySet());
        var phrases=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReviewPhrasePresence("청년농업인 신청서 private fixture contact");
        assertEquals(java.util.Map.of("청년농업인",true,"취업농",false,"신청서",true,"서명",false),phrases);
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(phrases).contains("private fixture"));
        assertTrue(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReviewPhrasePresence(null).values().stream().noneMatch(Boolean::booleanValue));
    }
    @Test void rejectsUnverifiedFingerprintMetadataBeforeItCanLeaveTheProbe() {
        var role=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                "private fixture","private fixture","private fixture","private fixture","UNKNOWN","INITIAL_HEADING_REQUIRED",java.util.List.of());
        var failure=assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleFingerprint(role));
        assertEquals("ROLE_FINGERPRINT_INVALID",failure.getMessage());
    }
    @Test void taebaekIsAnExplicitSingleNoticeGroupAndCannotReplaceYangpyeongCoverage() {
        assertEquals(java.util.List.of("TAEBAEK-184816"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("TAEBAEK"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK",1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK",1,0,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK",3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("YANGPYEONG",1,1,0,0,0,0));
        assertEquals(java.util.List.of("TAEBAEK-184816"),
                com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("TAEBAEK").map(c->c.code()).toList());
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("ALL"));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("https://example.invalid"));
    }
    @Test void reportsOnlyAllowlistedRoleCodesAndCounts() throws Exception {
        var evidence=java.util.List.of(
                new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Evidence("NOTICE_HEADING",0,0,10),
                new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Evidence("FORM_HEADING",10,100,110));
        var assessment=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                "private fixture","private fixture","private fixture","private fixture","UNKNOWN","MIXED_DOCUMENT_ROLES",evidence);
        var result=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleDiagnostic(assessment);
        assertEquals(java.util.Set.of("assessmentPresent","reasonCode","matchedRuleCodes","evidenceCount","evidenceBlockCount"),result.keySet());
        assertEquals("MIXED_DOCUMENT_ROLES",result.get("reasonCode"));
        assertEquals(java.util.List.of("FORM_HEADING","NOTICE_HEADING"),result.get("matchedRuleCodes"));
        assertEquals(2,result.get("evidenceCount"));assertEquals(2L,result.get("evidenceBlockCount"));
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result).contains("private fixture"));
        assertEquals(java.util.Map.of("assessmentPresent",false),AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleDiagnostic(null));
    }
    @Test void rejectsUnexpectedRoleDiagnosticCodesWithoutEchoingThem() {
        var assessment=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                "v","h","t","b","UNKNOWN","private fixture",java.util.List.of());
        var failure=assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleDiagnostic(assessment));
        assertEquals("ROLE_DIAGNOSTIC_CODE_INVALID",failure.getMessage());
    }
    @Test void requiresAllThreeWithoutSkippedOrFailedContainers() {
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(2,2,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(4,4,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,2,1,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,2,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,2,0,0,1,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,3,0,0,0,1));
    }
    @Test void includesExcludedTitleInDenominator() {
        assertEquals(AnnouncementAttachmentOfficialWorkerProbe.CASES,
                AnnouncementAttachmentOfficialWorkerIntegrationTest.selectCases().map(c->c.code()).toList());
    }
    @Test void diagnosticTraceDoesNotExposeExceptionMessages() throws Exception {
        var failure=new ExceptionInInitializerError(new IllegalStateException("private fixture text"));
        String output=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                AnnouncementAttachmentOfficialWorkerProbe.selectFailureTrace(failure));
        assertFalse(output.contains("private fixture text"));
        assertTrue(output.contains("IllegalStateException"));
        assertTrue(output.contains("ExceptionInInitializerError"));
    }
    @Test void comparesWireNumbersWithoutDroppingFieldsOrValueChecks() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var dto=java.util.Map.of("downloadedBytes",116740L,"characterCount",11398);
        var wire=mapper.readTree("{\"downloadedBytes\":116740,\"characterCount\":11398}");
        assertNotEquals(mapper.valueToTree(dto),wire); // 기존 공식 시험의 false negative 재현
        assertEquals(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectWireTree(dto),wire);
        assertNotEquals(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectWireTree(dto),
                mapper.readTree("{\"downloadedBytes\":116741,\"characterCount\":11398}"));
        assertNotEquals(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectWireTree(dto),
                mapper.readTree("{\"downloadedBytes\":116740}"));
    }
}
