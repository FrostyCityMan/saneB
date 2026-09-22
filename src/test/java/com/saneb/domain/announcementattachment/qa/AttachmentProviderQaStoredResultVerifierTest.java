package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase.*;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Item;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.Run;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 합성 원장/결과의 엄격한 재검증이다. 실제 파일 다운로드·추출 성공 증거가 아니다. */
class AttachmentProviderQaStoredResultVerifierTest {
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    final AttachmentProviderQaStoredResultVerifier verifier=new AttachmentProviderQaStoredResultVerifier(mapper);
    final Instant now=Instant.parse("2026-09-12T03:00:00Z");
    final UUID runId=UUID.randomUUID(),caseId=UUID.randomUUID();
    AttachmentProviderQaCase input;
    Run run;
    ObjectNode evidence;
    AnnouncementSourceClassificationRuleSet rules;
    @BeforeEach void setup() {
        rules=new AnnouncementSourceClassificationRuleSet("TEST",List.of(rule("T",RuleGroupKindCode.TARGET,"소상공인",TargetCategoryCode.BUSINESS,null),
                rule("S",RuleGroupKindCode.SUPPORT_TYPE,"지원금",null,SupportTypeCode.GRANT_SUBSIDY),rule("B",RuleGroupKindCode.AUTO_EXCLUDE_B,"수출",null,null)));
        prepare("FOUND",true,"소상공인 지원금",List.of(file("PDF","COMPLETE_TEXT")));
    }
    private AnnouncementSourceClassificationRule rule(String code,RuleGroupKindCode group,String term,TargetCategoryCode target,SupportTypeCode support) {
        return new AnnouncementSourceClassificationRule(code,code,group,term,StrengthCode.STRONG,target,support,List.of(AnnouncementSourceClassificationTerm.canonical(term,MatchModeCode.NORMALIZED_PHRASE)),true);
    }
    private ExpectedFile file(String format,String quality) {
        boolean text=Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(quality);
        return new ExpectedFile("a".repeat(64),true,format,"b".repeat(64),quality,text?10:0,text?1:0,text?List.of("지원"):List.of());
    }
    private void prepare(String discovery,boolean complete,String title,List<ExpectedFile> files) {
        input=new AttachmentProviderQaCase("CASE-1","TEST","c".repeat(64),new Source("BIZINFO","1","https://example.go.kr/1",null,null),title,rules,"d".repeat(64),
                discovery,complete,files,new Limits(420,44,83886080));
        boolean blocked="TITLE_BLOCKED".equals(discovery);
        var results=files.stream().map(f->{boolean text=Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(Objects.toString(f.quality(),""));return new AttachmentProviderQaCaseExecutor.FileResult(
                f.locatorHash(),f.downloadAllowed()?"PASSED":"UNSUPPORTED_NOT_DOWNLOADED",null,f.format(),f.quality(),f.downloadAllowed()?100:0,f.binaryHash(),text?"e".repeat(64):null,text?20:0,text?1:0,
                f.roleExpectation()==null?null:f.roleExpectation().assessmentHash(),f.segmentExpectation()==null?null:f.segmentExpectation().analysisHash());}).toList();
        boolean all="FOUND".equals(discovery) && complete && !files.isEmpty() && files.stream().allMatch(f->f.downloadAllowed() && "COMPLETE_TEXT".equals(f.quality()));
        String titleStage=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("BIZINFO",title,null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules).titleStageCode().name();
        var result=new AttachmentProviderQaCaseExecutor.Result("SINGLE_FIXED_NOTICE_PROVIDER_QA",input.caseId(),verifier.hash(input),input.profileHash(),input.runtimeHash(),"PASSED",
                blocked?"TITLE_BLOCKED_WITHOUT_REQUEST":"FIXED_NOTICE_EXPECTATIONS_MATCHED",titleStage,blocked?"NOT_REQUESTED":discovery,complete,files.size(),blocked?-1:files.size(),results,
                blocked?0:1+files.stream().filter(ExpectedFile::downloadAllowed).count(),blocked?0:100+results.stream().mapToLong(AttachmentProviderQaCaseExecutor.FileResult::bytes).sum(),true,all,false,now.plusSeconds(1),now.plusSeconds(4));
        evidence=mapper.valueToTree(result);
        run=new Run(runId,UUID.randomUUID(),0,UUID.randomUUID(),0,"f".repeat(64),"1".repeat(64),"2".repeat(64),input.runtimeHash(),1,44L,83886080L,result.requestReservations(),result.reservedBytes(),
                "COMPLETED",4,UUID.randomUUID(),UUID.randomUUID(),"3".repeat(64),now.minusSeconds(10).atOffset(ZoneOffset.UTC),now.plusSeconds(86000).atOffset(ZoneOffset.UTC),
                now.plusSeconds(10).atOffset(ZoneOffset.UTC),"4".repeat(64),1,1,1,1,false,480,true);
    }
    private Item row(String json,String hash) {
        return new Item(caseId,runId,1,input.caseId(),verifier.hash(input),input.profileHash(),input.files().size(),420,44,83886080L,
                run.requestReservations().intValue(),run.reservedBytes(),"PASSED",4,now.atOffset(ZoneOffset.UTC),now.plusSeconds(5).atOffset(ZoneOffset.UTC),null,json,hash);
    }
    private Item row() throws Exception{return row(mapper.writeValueAsString(evidence),verifier.hash(evidence));}
    private AttachmentProviderQaStoredResultVerifier.Verified verify() throws Exception{return verifier.selectVerifiedResult(row(),run,input,now.plusSeconds(20));}
    private void rejects(Consumer<ObjectNode> mutation) throws Exception {mutation.accept(evidence);assertThatThrownBy(this::verify).isInstanceOf(AttachmentProviderQaStoredResultVerifier.Failure.class);}
    private void prepareRole() {
        var f=file("PDF","COMPLETE_TEXT");var role=new RoleExpectation(AttachmentDocumentRoleClassifier.VERSION,AttachmentDocumentRoleClassifier.RULES_HASH,
                "NOTICE","ROLE_TEXT_STRUCTURE_MATCHED","e".repeat(64),"f".repeat(64),"9".repeat(64));
        prepare("FOUND",true,"소상공인 지원금",List.of(new ExpectedFile(f.locatorHash(),true,f.format(),f.binaryHash(),f.quality(),f.minimumCharacters(),f.minimumBlocks(),f.requiredPhrases(),role)));
    }
    private void prepareSegment() {
        var f=file("PDF","COMPLETE_TEXT");
        var s=new SegmentExpectation(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH,"e".repeat(64),"f".repeat(64),"9".repeat(64),"RESOLVED",List.of("NOTICE","FORM"));
        prepare("FOUND",true,"소상공인 지원금",List.of(new ExpectedFile(f.locatorHash(),true,f.format(),f.binaryHash(),f.quality(),f.minimumCharacters(),f.minimumBlocks(),f.requiredPhrases(),null,s)));
        input=new AttachmentProviderQaCase(input.caseId(),input.profileCode(),input.profileHash(),input.source(),input.title(),input.rules(),input.runtimeHash(),input.discoveryStatus(),input.discoveryComplete(),input.files(),input.limits(),
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        evidence.put("inputHash",verifier.hash(input));
    }
    @Test void storedSegmentProofBindsEngineAndAnalysisWithoutRawPositions() throws Exception {
        prepareSegment();assertThat(verify().allTextComplete()).isTrue();
        assertThat(evidence.path("files").get(0).path("segmentAnalysisHash").asText()).isEqualTo(input.files().getFirst().segmentExpectation().analysisHash());
        assertThat(evidence.toString()).doesNotContain("지원","startOffset","roleCodes","evidenceScopeId");
    }
    @ParameterizedTest @ValueSource(strings={"missing","null","wrong","coerced","textChanged"})
    void rehashingCannotHideSegmentProofMutation(String kind) throws Exception {
        prepareSegment();rejects(n->{var file=(ObjectNode)n.path("files").get(0);switch(kind){
            case "missing"->file.remove("segmentAnalysisHash");case "null"->file.putNull("segmentAnalysisHash");
            case "wrong"->file.put("segmentAnalysisHash","8".repeat(64));case "coerced"->file.put("segmentAnalysisHash",9);default->file.put("textHash","7".repeat(64));
        }});
    }
    @Test void legacyResultCannotInventSegmentProof() throws Exception {
        assertThat(verify().allTextComplete()).isTrue();rejects(n->((ObjectNode)n.path("files").get(0)).put("segmentAnalysisHash","9".repeat(64)));
    }
    @Test void storedRoleProofBindsFrozenInputAndActualTextHashWithoutRawEvidence() throws Exception {
        prepareRole();assertThat(verify().allTextComplete()).isTrue();
        assertThat(evidence.path("files").get(0).path("roleAssessmentHash").asText()).isEqualTo(input.files().getFirst().roleExpectation().assessmentHash());
        assertThat(evidence.toString()).doesNotContain("ROLE_TEXT_STRUCTURE_MATCHED","evidenceScopeId","지원");
    }
    @ParameterizedTest @ValueSource(strings={"missing","null","wrong","coerced","textChanged","unknownField"})
    void rehashingStoredRoleProofCannotHideMutation(String kind) throws Exception {
        prepareRole();rejects(n->{var file=(ObjectNode)n.path("files").get(0);switch(kind) {
            case "missing" -> file.remove("roleAssessmentHash");case "null" -> file.putNull("roleAssessmentHash");
            case "wrong" -> file.put("roleAssessmentHash","8".repeat(64));case "coerced" -> file.put("roleAssessmentHash",9);
            case "textChanged" -> file.put("textHash","7".repeat(64));default -> file.put("roleOverride","NOTICE");
        }});
    }
    @Test void legacyQualityOnlyResultCannotInventRoleProof() throws Exception {
        assertThat(verify().allTextComplete()).isTrue();rejects(n->((ObjectNode)n.path("files").get(0)).put("roleAssessmentHash","9".repeat(64)));
    }
    @ParameterizedTest @ValueSource(strings={"PDF","HWP","HWPX"})
    void validCompleteTextBindsAllFormatsAndMetadataWithoutOriginalText(String format) throws Exception {
        prepare("FOUND",true,"소상공인 지원금",List.of(file(format,"COMPLETE_TEXT")));var verified=verify();
        assertThat(verified.fileCount()).isEqualTo(1);assertThat(verified.allTextComplete()).isTrue();assertThat(verified.requestReservations()).isEqualTo(2);
        assertThat(mapper.writeValueAsString(verified)).doesNotContain("지원","sourceUrl","title","files");
    }
    @ParameterizedTest @ValueSource(strings={"PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED"})
    void expectedPartialAndNegativeQualityIsNotNormalCoverage(String quality) throws Exception {
        prepare("FOUND",true,"소상공인 지원금",List.of(file("HWP",quality)));assertThat(verify().allTextComplete()).isFalse();
    }
    @Test void unsupportedDescriptorIsPreservedWithoutDownload() throws Exception {
        prepare("FOUND",true,"소상공인 지원금",List.of(new ExpectedFile("a".repeat(64),false,null,null,null,0,0,List.of())));
        assertThat(verify().fileCount()).isEqualTo(1);assertThat(verify().allTextComplete()).isFalse();rejects(n->((ObjectNode)n.path("files").get(0)).put("bytes",1));
    }
    @Test void titleBlockedRequiresExactStageAndNoRequestOrBytes() throws Exception {
        prepare("TITLE_BLOCKED",false,"소상공인 수출 지원금",List.of());assertThat(verify().requestReservations()).isZero();assertThat(verify().allTextComplete()).isFalse();
        rejects(n->n.put("requestReservations",1));
    }
    @Test void noFilesAndFailedDiscoveryCanMatchNegativeExpectationsWithoutTextCoverage() throws Exception {
        prepare("NO_FILES",true,"소상공인 지원금",List.of());assertThat(verify().allTextComplete()).isFalse();
        prepare("FAILED",false,"소상공인 지원금",List.of());assertThat(verify().allTextComplete()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"originalFilesRemoved","allTextComplete"})
    void cannotOverrideCleanupOrCoverage(String field) throws Exception {rejects(n->n.put(field,false));}
    @Test void cannotClaimPolicyPassedOrSkipAFile() throws Exception {
        rejects(n->n.put("isPolicyQaPassed",true));evidence.put("isPolicyQaPassed",false);rejects(n->n.withArray("files").removeAll());
    }
    @ParameterizedTest @ValueSource(strings={"caseId","inputHash","profileHash","runtimeHash","scope","status","titleStage","discoveryStatus"})
    void changedRootIdentityOrStatusCannotBeRehashedIntoSuccess(String field) throws Exception {rejects(n->n.put(field,"CHANGED"));}
    @ParameterizedTest @ValueSource(strings={"locatorHash","binaryHash","format","quality","status","textHash"})
    void changedFileIdentityOrQualityCannotBeRehashedIntoSuccess(String field) throws Exception {rejects(n->((ObjectNode)n.path("files").get(0)).put(field,"CHANGED"));}
    @ParameterizedTest @ValueSource(strings={"characterCount","blockCount","bytes"})
    void missingTextOrBytesCannotPass(String field) throws Exception {rejects(n->((ObjectNode)n.path("files").get(0)).put(field,0));}
    @Test void unknownMissingDuplicateAndTrailingJsonFieldsAreRejected() throws Exception {
        String json=mapper.writeValueAsString(evidence);rejects(n->n.put("passedOverride",true));evidence.remove("passedOverride");
        rejects(n->n.remove("originalFilesRemoved"));evidence.put("originalFilesRemoved",true);
        for(String altered:List.of(json.replaceFirst("\\{","{\"status\":\"PASSED\","),json+" {}"))
            assertThatThrownBy(()->verifier.selectVerifiedResult(row(altered,"a".repeat(64)),run,input,now.plusSeconds(20))).isInstanceOf(AttachmentProviderQaStoredResultVerifier.Failure.class);
    }
    @Test void jsonbFieldOrderIsNotEvidenceMutationButHashMismatchIs() throws Exception {
        Map<String,Object> ordered=mapper.convertValue(evidence,new com.fasterxml.jackson.core.type.TypeReference<TreeMap<String,Object>>(){});String json=mapper.writeValueAsString(ordered);
        assertThat(verifier.selectVerifiedResult(row(json,verifier.hash(evidence)),run,input,now.plusSeconds(20)).allTextComplete()).isTrue();
        assertThatThrownBy(()->verifier.selectVerifiedResult(row(json,"9".repeat(64)),run,input,now.plusSeconds(20))).hasMessage("CASE_EVIDENCE_HASH_CHANGED");
    }
    @ParameterizedTest @ValueSource(strings={"requestReservations","reservedBytes","expectedFileCount","discoveredFileCount"})
    void coercedNumericStringsAreNotAllowed(String field) throws Exception {rejects(n->n.put(field,n.path(field).asText()));}
    @Test void numericBinaryHashAndFractionalFileCountsAreNotAllowed() throws Exception {
        rejects(n->((ObjectNode)n.path("files").get(0)).put("characterCount",20.0));
    }
    @Test void futureOutOfOwnerTimeOrUsageMismatchFails() throws Exception {
        rejects(n->n.put("completedAt",now.plusSeconds(100).toString()));evidence.put("completedAt",now.plusSeconds(4).toString());
        rejects(n->n.put("startedAt",now.minusSeconds(1).toString()));evidence.put("startedAt",now.plusSeconds(1).toString());
        rejects(n->n.put("reservedBytes",run.reservedBytes()+1));
    }
    @Test void originalEvidenceCannotBeModifiedAfterLedgerHashWasCalculated() throws Exception {
        String before=verifier.hash(evidence);evidence.put("allTextComplete",false);
        assertThatThrownBy(()->verifier.selectVerifiedResult(row(mapper.writeValueAsString(evidence),before),run,input,now.plusSeconds(20))).hasMessage("CASE_EVIDENCE_HASH_CHANGED");
    }
}
