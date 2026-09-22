package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaEvidenceDao;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase.*;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration;
import com.saneb.domain.announcementattachment.service.impl.*;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.*;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.Run;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleValidationDetails;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 실제 catalog/파일 근거 검증기와 합성 DB 조회를 연결한다. 실제 Provider/PG 증거가 아니다. */
class AttachmentProviderQaEvidenceGateTest {
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    final AttachmentProviderQaStoredResultVerifier verifier=new AttachmentProviderQaStoredResultVerifier(mapper);
    final AnnouncementAttachmentProviderQaEvidenceDao dao=mock(AnnouncementAttachmentProviderQaEvidenceDao.class);
    final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    final AtomicInteger depth=new AtomicInteger();
    final UUID policyId=UUID.randomUUID(),ruleId=UUID.randomUUID(),policyRunId=UUID.randomUUID();
    final Instant now=Instant.now().minusSeconds(10);
    final List<Run> runs=new ArrayList<>();
    final Map<UUID,List<Item>> items=new LinkedHashMap<>();
    final String runtimeHash="b".repeat(64),profileHash="a".repeat(64),scopeJson="[{\"providerCode\":\"BIZINFO\"},{\"providerCode\":\"GOV24_PUBLIC_SERVICE\"}]";
    AttachmentPolicyValidationSnapshotFactory.Frozen frozen;
    AttachmentPolicyValidationRows.Run policyRun;
    AttachmentProviderQaEvidenceGate gate;
    Prepared prepared;
    @BeforeEach void setup() throws Exception {configure(6);}
    @AfterEach void clearPublicationMarkers(){TransactionSynchronizationManager.setActualTransactionActive(false);TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);}
    private void configure(int count) throws Exception {
        configure(count,1,false);
    }
    private void configure(int count,int catalogSchema,boolean mixedFormats) throws Exception {
        configure(count,catalogSchema,mixedFormats,false);
    }
    private void configure(int count,int catalogSchema,boolean mixedFormats,boolean segmentEngine) throws Exception {
        runs.clear();items.clear();reset(dao,transactions);depth.set(0);
        when(transactions.getTransaction(any())).thenAnswer(c->{var definition=c.getArgument(0,TransactionDefinition.class);assertThat(definition.isReadOnly()).isTrue();
            assertThat(definition.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);depth.incrementAndGet();return new SimpleTransactionStatus();});
        doAnswer(c->{depth.decrementAndGet();return null;}).when(transactions).commit(any());doAnswer(c->{depth.decrementAndGet();return null;}).when(transactions).rollback(any());
        var rules=new AnnouncementSourceClassificationRuleSet("TEST",List.of(rule("T",RuleGroupKindCode.TARGET,"소상공인",TargetCategoryCode.BUSINESS,null),rule("S",RuleGroupKindCode.SUPPORT_TYPE,"지원금",null,SupportTypeCode.GRANT_SUBSIDY)));
        var profiles=List.of(profile("BIZINFO","BIZ"),profile("GOV24_PUBLIC_SERVICE","GOV"));var scope=AttachmentProviderQaPlan.selectPlan(profiles,List.of());
        var role=new RoleExpectation(AttachmentDocumentRoleClassifier.VERSION,AttachmentDocumentRoleClassifier.RULES_HASH,
                segmentEngine?"UNKNOWN":"NOTICE",segmentEngine?"MIXED_DOCUMENT_ROLES":"ROLE_TEXT_STRUCTURE_MATCHED","8".repeat(64),"f".repeat(64),"9".repeat(64));
        var segmentExpectation=segmentEngine?new SegmentExpectation(AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH,
                role.textHash(),role.blocksHash(),"7".repeat(64),"RESOLVED",List.of("NOTICE","FORM")):null;
        var configuration=segmentEngine?new Configuration(AttachmentSegmentClassificationEngine.VERSION,null,null,null,null,null,
                AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH):new Configuration("attachment-1.0.0",null,null,null);
        var files=new ArrayList<ExpectedFile>();int f=1;for(String format:List.of("PDF","HWP","HWPX"))files.add(new ExpectedFile(Integer.toString(f++).repeat(64),true,format,"c".repeat(64),"COMPLETE_TEXT",10,1,List.of("지원"),role,segmentExpectation));
        var notices=new ArrayList<Notice>();
        for(int i=0;i<count;i++) {String provider=i%2==0?"BIZINFO":"GOV24_PUBLIC_SERVICE",code=i%2==0?"BIZ":"GOV";
            List<ExpectedFile> noticeFiles=files;
            if(mixedFormats) {
                var pdf=files.getFirst();
                noticeFiles=i%2==0?List.of(pdf,new ExpectedFile("4".repeat(64),true,"PDF",pdf.binaryHash(),pdf.quality(),pdf.minimumCharacters(),pdf.minimumBlocks(),pdf.requiredPhrases(),role,segmentExpectation))
                        :List.of(files.get(1),files.get(2));
            }
            notices.add(new Notice(code+"-"+String.format(Locale.ROOT,"%04d",i),code,new AttachmentDiscoveryProfile.Source(provider,Integer.toString(i),"https://example.go.kr/"+i,null,null),
                    new Expectation(profileHash,"소상공인 지원금",now.minusSeconds(3600),"FOUND",true,noticeFiles,new Limits(420,44,83886080))));}
        var catalog=new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles),new Definition(catalogSchema,"TEST",notices));
        prepared=catalog.selectPrepared(scope,rules,runtimeHash,now,configuration);
        var json=mapper.createObjectNode().put("schemaVersion",6).put("policyId",policyId.toString()).put("policyVersion",0);json.set("targets",mapper.readTree(scopeJson));
        json.set("settings",mapper.valueToTree(configuration));
        json.set("providerQaPlan",mapper.valueToTree(scope));json.set("providerQaCatalog",mapper.valueToTree(prepared.plan()));
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("d".repeat(64),mapper.writeValueAsString(json),new AnnouncementSourceRuleValidationDetails(ruleId,0,"DRAFT",null,"e".repeat(64),rules),
                new AttachmentPolicyValidationSnapshotFactory.Runtime(runtimeHash,"f".repeat(64),"0".repeat(64)));
        policyRun=new AttachmentPolicyValidationRows.Run(policyRunId,policyId,0,ruleId,0,frozen.hash(),frozen.json(),"RUNNING",1,UUID.randomUUID(),UUID.randomUUID(),"9".repeat(64),UUID.randomUUID(),
                now.plusSeconds(900).atOffset(ZoneOffset.UTC),null,now.minusSeconds(5).atOffset(ZoneOffset.UTC),now.atOffset(ZoneOffset.UTC),null,true);
        int all=0;
        for(var segment:prepared.plan().segments()) {
            UUID id=UUID.randomUUID();var rows=new ArrayList<Item>();Instant begin=now.minusSeconds(count*3L+300).plusSeconds(all*3L);
            for(String code:segment.caseCodes()) {
                var input=prepared.inputs().stream().filter(c->c.caseId().equals(code)).findFirst().orElseThrow();Instant start=now.minusSeconds(count*3L+300).plusSeconds(all++*3L);
                var fileResults=input.files().stream().map(e->new AttachmentProviderQaCaseExecutor.FileResult(e.locatorHash(),"PASSED",null,e.format(),e.quality(),100,e.binaryHash(),"8".repeat(64),20,1,e.roleExpectation().assessmentHash(),e.segmentExpectation()==null?null:e.segmentExpectation().analysisHash())).toList();
                var result=new AttachmentProviderQaCaseExecutor.Result("SINGLE_FIXED_NOTICE_PROVIDER_QA",code,verifier.hash(input),profileHash,runtimeHash,"PASSED","FIXED_NOTICE_EXPECTATIONS_MATCHED",
                        "COMBINATION_MATCHED","FOUND",true,input.files().size(),input.files().size(),fileResults,4,400,true,true,false,start.plusMillis(100),start.plusSeconds(1));
                rows.add(new Item(UUID.randomUUID(),id,rows.size()+1,code,verifier.hash(input),profileHash,input.files().size(),420,44,83886080L,4,400L,"PASSED",4,start.atOffset(ZoneOffset.UTC),
                        start.plusSeconds(2).atOffset(ZoneOffset.UTC),null,mapper.writeValueAsString(result),verifier.hash(result)));
            }
            items.put(id,rows);var last=rows.getLast().completedAt();
            runs.add(new Run(id,policyId,0,ruleId,0,frozen.hash(),prepared.plan().catalogHash(),frozen.runtime().executionCodeHash(),runtimeHash,rows.size(),segment.maximumRequests(),segment.maximumBytes(),4L*rows.size(),400L*rows.size(),
                    "COMPLETED",4,UUID.randomUUID(),UUID.randomUUID(),"7".repeat(64),begin.minusMillis(500).atOffset(ZoneOffset.UTC),begin.plusSeconds(86400).atOffset(ZoneOffset.UTC),last,
                    verifier.hash(prepared.plan()),segment.ordinal(),prepared.plan().segments().size(),count,count,prepared.plan().isExpectationCoverageComplete(),Math.toIntExact(segment.maximumSecondsIncludingMargin()),true));
        }
        when(dao.selectLatestRunList(any())).thenAnswer(c->{assertThat(depth.get()).isEqualTo(1);return List.copyOf(runs);});
        when(dao.selectRequiredScope(any())).thenReturn(scopeJson);
        when(dao.selectEvidenceCount(any())).thenAnswer(c->(long)items.get(c.getArgument(0)).size());
        when(dao.selectEvidenceList(any())).thenAnswer(c->{Page page=c.getArgument(0);var rows=items.get(page.runId());return List.copyOf(rows.subList(page.offset(),Math.min(rows.size(),page.offset()+page.size())));});
        gate=new AttachmentProviderQaEvidenceGate(catalog,verifier,dao,mapper,transactions);
    }
    private AnnouncementSourceClassificationRule rule(String code,RuleGroupKindCode group,String term,TargetCategoryCode target,SupportTypeCode support){return new AnnouncementSourceClassificationRule(code,code,group,term,StrengthCode.STRONG,target,support,List.of(AnnouncementSourceClassificationTerm.canonical(term,MatchModeCode.NORMALIZED_PHRASE)),true);}
    private AttachmentDiscoveryProfile profile(String provider,String code){var p=mock(AttachmentDiscoveryProfile.class);when(p.selectProviderCode()).thenReturn(provider);when(p.selectProfileCode()).thenReturn(code);when(p.selectProfileHash()).thenReturn(profileHash);
        when(p.selectSourceBindings()).thenReturn(List.of(new AttachmentDiscoveryProfile.SourceBinding(null,null)));when(p.selectDetailUri(any(AttachmentDiscoveryProfile.Source.class))).thenAnswer(c->URI.create(c.getArgument(0,AttachmentDiscoveryProfile.Source.class).sourceUrl()));when(p.selectApprovedRequest(any(URI.class))).thenReturn(true);return p;}
    private AttachmentProviderQaEvidenceGate.Assessment assess(){return gate.selectAssessment(frozen,policyRun,()->true);}
    @Test void segmentPolicyAggregatesMixedDocumentsWithoutRewritingWholeFileRoles() throws Exception {
        configure(6,2,false,true);
        var result=assess();assertThat(result.status()).isEqualTo("PASSED");
        assertThat(result.evidence().caseCount()).isEqualTo(6);assertThat(result.evidence().fileCount()).isEqualTo(18);
        assertThat(prepared.inputs()).allSatisfy(input->{
            assertThat(input.engineVersion()).isEqualTo(AttachmentSegmentClassificationEngine.VERSION);
            assertThat(input.files()).allSatisfy(file->{assertThat(file.roleExpectation().roleCode()).isEqualTo("UNKNOWN");
                assertThat(file.segmentExpectation().roleCodes()).containsExactly("NOTICE","FORM");});});
        var json=mapper.readTree(mapper.writeValueAsString(result.evidence()));
        assertThat(gate.selectValidatedEvidenceHash(json,frozen,policyRun)).isEqualTo(verifier.hash(result.evidence()));
        assertThat(json.toString()).doesNotContain("지원","roleCodes","textHash","startOffset");
    }
    @ParameterizedTest @ValueSource(strings={"missing","changed"})
    void segmentPolicyRejectsMissingOrRehashedChangedFileAnalysis(String mutation) throws Exception {
        configure(6,2,false,true);
        var row=items.get(runs.getFirst().runId()).getFirst();var json=(ObjectNode)mapper.readTree(row.evidenceJson());
        var file=(ObjectNode)json.path("files").get(0);
        if("missing".equals(mutation))file.remove("segmentAnalysisHash");else file.put("segmentAnalysisHash","6".repeat(64));
        changeCase("evidenceJson",mapper.writeValueAsString(json));changeCase("evidenceHash",verifier.hash(mapper.convertValue(json,Object.class)));
        assertThat(assess().status()).isEqualTo("FAILED");assertThat(assess().reasonCode()).isEqualTo("SEGMENT_EXPECTATION_CHANGED");
    }
    @Test void legacyProviderProofCannotBeReusedByChangingOnlyPolicyEngine() throws Exception {
        var json=(ObjectNode)mapper.readTree(frozen.json());
        json.set("settings",mapper.valueToTree(new Configuration(AttachmentSegmentClassificationEngine.VERSION,null,null,null,null,null,
                AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH)));
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen(frozen.hash(),mapper.writeValueAsString(json),frozen.rule(),frozen.runtime());
        assertThat(assess().reasonCode()).isEqualTo("CURRENT_CATALOG_CHANGED");verify(dao,never()).selectLatestRunList(any());
    }
    @Test void publicationHashRecomputesActualAggregateAndRejectsAlteredJson() throws Exception {
        var saved=assess().evidence();var json=mapper.readTree(mapper.writeValueAsString(saved));
        assertThat(gate.selectStepCode()).isEqualTo("PROVIDER_PROFILES");
        assertThat(gate.selectValidatedEvidenceHash(json,frozen,policyRun)).isEqualTo(verifier.hash(saved));
        ((ObjectNode)json).put("fileCount",17);
        assertThatThrownBy(()->gate.selectValidatedEvidenceHash(json,frozen,policyRun)).hasMessageContaining("수집원 QA");
        ((ObjectNode)json).put("fileCount",18).put("passedOverride",true);
        assertThatThrownBy(()->gate.selectValidatedEvidenceHash(json,frozen,policyRun)).hasMessageContaining("수집원 QA");
    }
    @Test void latestFailureInvalidatesPreviouslyValidatedPublicationEvidence() throws Exception {
        var json=mapper.readTree(mapper.writeValueAsString(assess().evidence()));changeRun("statusCode","FAILED");
        assertThatThrownBy(()->gate.selectValidatedEvidenceHash(json,frozen,policyRun)).hasMessageContaining("수집원 QA");
    }
    @Test void lockedCurrentnessUsesOnlyDatabaseAndRequiresWriteTransaction() throws Exception {
        var json=mapper.readTree(mapper.writeValueAsString(assess().evidence()));clearInvocations(dao,transactions);
        assertThatThrownBy(()->gate.validateCurrentEvidence(json,frozen,policyRun)).hasMessageContaining("수집원 QA");verifyNoInteractions(dao);
        TransactionSynchronizationManager.setActualTransactionActive(true);TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThatThrownBy(()->gate.validateCurrentEvidence(json,frozen,policyRun)).hasMessageContaining("수집원 QA");verifyNoInteractions(dao);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        doReturn(List.copyOf(runs)).when(dao).selectLatestRunList(any());
        gate.validateCurrentEvidence(json,frozen,policyRun);
        verify(dao).selectLatestRunList(any());verify(dao,never()).selectEvidenceList(any());verifyNoInteractions(transactions);
    }
    @ParameterizedTest @ValueSource(strings={"runId","statusCode","rowVersion","inputVersionsCurrent"})
    void lockedCurrentnessRejectsChangedLatestAttempt(String field) throws Exception {
        var json=mapper.readTree(mapper.writeValueAsString(assess().evidence()));
        changeRun(field,switch(field){case "runId"->UUID.randomUUID();case "statusCode"->"READY";case "rowVersion"->5;default->false;});
        doReturn(List.copyOf(runs)).when(dao).selectLatestRunList(any());TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThatThrownBy(()->gate.validateCurrentEvidence(json,frozen,policyRun)).hasMessageContaining("수집원 QA");
    }
    private void changeRun(String field,Object value)throws Exception{ObjectNode json=mapper.valueToTree(runs.getFirst());json.set(field,mapper.valueToTree(value));runs.set(0,mapper.treeToValue(json,Run.class));}
    private void changeCase(String field,Object value)throws Exception{var list=items.get(runs.getFirst().runId());ObjectNode json=mapper.valueToTree(list.getFirst());json.set(field,mapper.valueToTree(value));list.set(0,mapper.treeToValue(json,Item.class));}
    @Test void completeScopeRequiresEveryCaseAndFileAndEmitsOnlyDigestMetadata() throws Exception {
        var result=assess();assertThat(result.status()).isEqualTo("PASSED");assertThat(result.evidence().targetCount()).isEqualTo(2);assertThat(result.evidence().caseCount()).isEqualTo(6);assertThat(result.evidence().fileCount()).isEqualTo(18);
        assertThat(result.evidence().segments()).hasSize(1);assertThat(depth.get()).isZero();String json=mapper.writeValueAsString(result.evidence());assertThat(json).doesNotContain("소상공인","https://","requiredPhrases","textHash","idempotencyKey");
        assertThat(verifier.hash(result.evidence())).isEqualTo(verifier.hash(assess().evidence()));
    }
    @Test void v2ReverifiesAllFilesWithDifferentObservedFormatsForEachProvider() throws Exception {
        configure(6,2,true);var result=assess();assertThat(result.status()).isEqualTo("PASSED");
        assertThat(result.evidence().targetCount()).isEqualTo(2);assertThat(result.evidence().caseCount()).isEqualTo(6);assertThat(result.evidence().fileCount()).isEqualTo(12);
        assertThat(prepared.plan().targets().getFirst().formatApplicability().unobservedFormats()).containsExactly("HWP","HWPX");
        verify(dao).selectEvidenceList(any());
        changeCase("expectedFileCount",1);assertThat(assess().status()).isEqualTo("FAILED");
    }
    @ParameterizedTest @ValueSource(strings={"normalMultiFileNoticeCount","unobservedFormats","expectedProvidedFormats","formatCoverage"})
    void v2AlteredFormatMetadataCannotReuseFrozenPolicyEvidence(String field) throws Exception {
        configure(6,2,true);var json=(ObjectNode)mapper.readTree(frozen.json());var plan=(ObjectNode)json.path("providerQaCatalog");
        if("formatCoverage".equals(field))((ObjectNode)plan.path(field)).put("modeCode","DISABLED");
        else {var applicability=(ObjectNode)plan.path("targets").get(0).path("formatApplicability");
            if("normalMultiFileNoticeCount".equals(field))applicability.put(field,0);else applicability.putArray(field);}
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen(frozen.hash(),mapper.writeValueAsString(json),frozen.rule(),frozen.runtime());
        assertThat(assess().reasonCode()).isEqualTo("CURRENT_CATALOG_CHANGED");verify(dao,never()).selectLatestRunList(any());
    }
    @Test void allPagesAndSegmentsAreVerifiedWithoutReducingDenominator() throws Exception {
        configure(176);var result=assess();assertThat(result.status()).isEqualTo("PASSED");assertThat(result.evidence().segments()).hasSize(2);assertThat(result.evidence().caseCount()).isEqualTo(176);
        verify(dao).selectEvidenceList(new Page(runs.getFirst().runId(),100,100));verify(dao).selectEvidenceList(new Page(runs.getLast().runId(),100,0));
    }
    @Test void adminMayExecuteSegmentsOutOfOrdinalOrderWithoutOverlappingRuns()throws Exception {
        configure(176);var old=runs.getLast();var shifted=(ObjectNode)mapper.valueToTree(old);
        for(String field:List.of("createdAt","completedAt","expiresAt"))shifted.put(field,OffsetDateTime.parse(shifted.path(field).asText()).minusSeconds(1000).toString());
        runs.set(1,mapper.treeToValue(shifted,Run.class));var rows=items.get(old.runId());
        for(int i=0;i<rows.size();i++) {
            var row=rows.get(i);var json=(ObjectNode)mapper.valueToTree(row);var result=(ObjectNode)mapper.readTree(row.evidenceJson());
            for(String field:List.of("startedAt","completedAt")) {
                json.put(field,OffsetDateTime.parse(json.path(field).asText()).minusSeconds(1000).toString());
                result.put(field,Instant.parse(result.path(field).asText()).minusSeconds(1000).toString());
            }
            json.put("evidenceJson",mapper.writeValueAsString(result)).put("evidenceHash",verifier.hash(result));rows.set(i,mapper.treeToValue(json,Item.class));
        }
        assertThat(assess().status()).isEqualTo("PASSED");
    }
    @Test void partialExpectationsNeverReadSuccessFromDatabase() throws Exception {configure(2);assertThat(assess().status()).isEqualTo("MISSING");verify(dao,never()).selectLatestRunList(any());}
    @Test void missingOrAdditionalSegmentCannotPass() throws Exception {var first=runs.getFirst();runs.clear();assertThat(assess().reasonCode()).isEqualTo("ALL_SEGMENT_RUNS_REQUIRED");runs.add(first);runs.add(first);assertThat(assess().status()).isEqualTo("FAILED");}
    @ParameterizedTest @ValueSource(strings={"FAILED","CANCELLED","READY","RUNNING","CANCEL_REQUESTED"})
    void latestIncompleteAttemptNeverFallsBackToOldSuccess(String status)throws Exception {changeRun("statusCode",status);assertThat(assess().reasonCode()).isEqualTo("LATEST_SEGMENT_NOT_COMPLETED");verify(dao,never()).selectEvidenceList(any());}
    @ParameterizedTest @ValueSource(strings={"snapshotHash","catalogHash","executionCodeHash","runtimeHash","planHash"})
    void changedRunHashesCannotPass(String field)throws Exception {changeRun(field,"9".repeat(64));assertThat(assess().reasonCode()).isEqualTo("RUN_PLAN_CHANGED");}
    @Test void missingOrReorderedCaseAndWrongScopeCannotPass()throws Exception {
        when(dao.selectRequiredScope(any())).thenReturn("[]");assertThat(assess().reasonCode()).isEqualTo("RUN_REQUIRED_SCOPE_CHANGED");when(dao.selectRequiredScope(any())).thenReturn(scopeJson);
        var list=items.get(runs.getFirst().runId());Collections.swap(list,0,1);assertThat(assess().reasonCode()).isEqualTo("CASE_SEQUENCE_CHANGED");Collections.swap(list,0,1);
        list.removeLast();assertThat(assess().reasonCode()).isEqualTo("CASE_COUNT_CHANGED");
    }
    @Test void truncatedPageAndDuplicateCaseDoNotHideMissingEvidence()throws Exception {
        var original=List.copyOf(items.get(runs.getFirst().runId()));doReturn(original.subList(0,5)).when(dao).selectEvidenceList(any());assertThat(assess().reasonCode()).isEqualTo("CASE_PAGE_INCOMPLETE");
        changeCase("caseId",original.get(1).caseId());doAnswer(c->items.get(runs.getFirst().runId())).when(dao).selectEvidenceList(any());assertThat(assess().reasonCode()).isEqualTo("CASE_SEQUENCE_CHANGED");
    }
    @Test void failedCaseAndWrongUsageCannotProduceAggregatePass()throws Exception {
        changeCase("statusCode","FAILED");assertThat(assess().reasonCode()).isEqualTo("CASE_NOT_PASSED");changeCase("statusCode","PASSED");changeRun("reservedBytes",2401);assertThat(assess().reasonCode()).isEqualTo("RUN_USAGE_CHANGED");
    }
    @Test void completionAfterPolicyQaAndCancelledValidationCannotPass()throws Exception {
        changeRun("completedAt",now.plusSeconds(1).toString());assertThat(assess().reasonCode()).isEqualTo("RUN_TIME_INVALID");
        assertThat(gate.selectAssessment(frozen,policyRun,()->false).status()).isEqualTo("CANCELLED");
    }
    @Test void databaseFailureIsSafeFailureNotMissingOrPassed() {doThrow(new IllegalStateException("fixture database error")).when(dao).selectLatestRunList(any());var result=assess();assertThat(result.status()).isEqualTo("FAILED");assertThat(result.reasonCode()).doesNotContain("fixture");assertThat(depth.get()).isZero();}
}
