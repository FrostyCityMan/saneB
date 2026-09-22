package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeGate;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleValidationDetails;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;

/** 근거 검증 조합 시험. 추가 QA 실행기는 대역이며 실제 전체 Provider/worker 성공 증거가 아니다. */
class AttachmentPolicyPublicationQaVerifierTest {
    private final AttachmentPolicyValidationSnapshotFactory snapshots=mock(AttachmentPolicyValidationSnapshotFactory.class);
    private final AnnouncementAttachmentPolicyGoldenGate golden=mock(AnnouncementAttachmentPolicyGoldenGate.class);
    private final AttachmentRuntimeGate runtime=mock(AttachmentRuntimeGate.class);
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final UUID runId=UUID.randomUUID(),policyId=UUID.randomUUID(),ruleId=UUID.randomUUID();
    private final OffsetDateTime now=OffsetDateTime.now();
    private AttachmentPolicyValidationSnapshotFactory.Frozen frozen;
    private Run run;
    private List<Step> steps;
    private AnnouncementAttachmentPolicyGoldenGate.Result classification;
    private AttachmentRuntimeGate.Result extraction;
    private AttachmentPolicyPublicationQaVerifier verifier;
    private String hash(Object value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(value)));}
    private AttachmentPolicyAdditionalQaEvidenceVerifier handler(String code){var handler=mock(AttachmentPolicyAdditionalQaEvidenceVerifier.class);when(handler.selectStepCode()).thenReturn(code);
        when(handler.selectValidatedEvidenceHash(any(),any(),any(),any())).thenAnswer(call->{assertThat(call.getArgument(3,OffsetDateTime.class)).isEqualTo(now.minusSeconds(1));return "e".repeat(64);});return handler;}
    private Step step(String code,Object evidence)throws Exception{return new Step(runId,code,"PASSED",mapper.writeValueAsString(evidence),hash(evidence),now.minusSeconds(1));}
    @BeforeEach void setup()throws Exception{
        when(snapshots.hash(any())).thenAnswer(c->hash(c.getArgument(0)));
        var rule=new AnnouncementSourceRuleValidationDetails(ruleId,1,"ACTIVE","a".repeat(64),"a".repeat(64),new AnnouncementSourceClassificationRuleSet("QA",List.of()));
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("b".repeat(64),"{\"schema\":1,\"settings\":{\"engineVersion\":\"attachment-1.0.0\"}}",rule,new AttachmentPolicyValidationSnapshotFactory.Runtime("c".repeat(64),"d".repeat(64),"e".repeat(64)));
        run=new Run(runId,policyId,0,ruleId,1,frozen.hash(),frozen.json(),"VERIFIED",2,UUID.randomUUID(),UUID.randomUUID(),"a".repeat(64),null,null,null,now.minusSeconds(90),now.minusSeconds(60),now,true);
        classification=new AnnouncementAttachmentPolicyGoldenGate.Result(AnnouncementAttachmentPolicyGoldenGate.SUITE_VERSION,"attachment-1.0.0","QA","a".repeat(64),"c".repeat(64),"d".repeat(64),30,java.util.stream.IntStream.rangeClosed(1,30).mapToObj(i->String.format("AG-%03d",i)).toList());
        when(golden.selectValidatedResult(any(),any(),any())).thenReturn(classification);
        extraction=new AttachmentRuntimeGate.Result(UUID.randomUUID(),AttachmentRuntimeGate.SCOPE,AttachmentRuntimeGate.SUITE_VERSION,"d".repeat(64),"c".repeat(64),"1.0.0","a".repeat(64),12,List.of(),now.minusSeconds(40).toInstant(),now.minusSeconds(5).toInstant());
        steps=new ArrayList<>(List.of(step("CLASSIFICATION_GOLDEN",classification),step("INSTALLED_RUNTIME",extraction),
                new Step(runId,"PROVIDER_PROFILES","PASSED","{}","e".repeat(64),now.minusSeconds(1)),new Step(runId,"WORKER_DB_RECOVERY","PASSED","{}","e".repeat(64),now.minusSeconds(1))));
        verifier=new AttachmentPolicyPublicationQaVerifier(snapshots,golden,runtime,List.of(handler("PROVIDER_PROFILES"),handler("WORKER_DB_RECOVERY")),mapper);
    }
    @Test void combinesOnlyRevalidatedCurrentBoundEvidence()throws Exception{
        assertThat(verifier.selectValidatedEvidenceHash(run,steps,frozen)).matches("[0-9a-f]{64}");
        verify(golden).selectValidatedResult(frozen.rule().ruleSet(),frozen.rule().calculatedSnapshotHash(),frozen.selectConfiguration());verify(runtime).validateStoredResult(extraction,"c".repeat(64));
    }
    @Test void fourPassedStringsCannotBypassMissingProductionVerifiers(){
        var unavailable=new AttachmentPolicyPublicationQaVerifier(snapshots,golden,runtime,List.of(),mapper);
        assertThatThrownBy(()->unavailable.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class).hasMessageContaining("PROVIDER_PROFILES");
    }
    @Test void lockedRevalidationCallsOnlyAdditionalDatabaseChecks() throws Exception {
        var provider=handler("PROVIDER_PROFILES");var worker=handler("WORKER_DB_RECOVERY");
        var current=new AttachmentPolicyPublicationQaVerifier(snapshots,golden,runtime,List.of(provider,worker),mapper);
        clearInvocations(golden,runtime,snapshots);
        current.validateCurrentEvidence(run,steps,frozen);
        verify(provider).validateCurrentEvidence(mapper.readTree("{}"),frozen,run);
        verify(worker).validateCurrentEvidence(mapper.readTree("{}"),frozen,run);
        verifyNoInteractions(golden,runtime,snapshots);
        steps.remove(2);
        assertThatThrownBy(()->current.validateCurrentEvidence(run,steps,frozen)).isInstanceOf(ApiException.class);
    }
    @Test void lockedRevalidationRejectsMissingHandlerOrChangedEvidence() {
        var unavailable=new AttachmentPolicyPublicationQaVerifier(snapshots,golden,runtime,List.of(),mapper);
        assertThatThrownBy(()->unavailable.validateCurrentEvidence(run,steps,frozen)).isInstanceOf(ApiException.class);
        var provider=handler("PROVIDER_PROFILES");
        doThrow(new IllegalStateException("private DB detail")).when(provider).validateCurrentEvidence(any(),any(),any());
        var current=new AttachmentPolicyPublicationQaVerifier(snapshots,golden,runtime,List.of(provider,handler("WORKER_DB_RECOVERY")),mapper);
        assertThatThrownBy(()->current.validateCurrentEvidence(run,steps,frozen)).isInstanceOf(ApiException.class).hasMessageNotContaining("private DB detail");
    }
    @Test void modifiedGoldenResultOrStoredHashIsRejected()throws Exception{
        var altered=new AnnouncementAttachmentPolicyGoldenGate.Result(classification.suiteVersion(),classification.engineVersion(),classification.ruleReleaseCode(),classification.ruleSnapshotHash(),classification.ruleContentHash(),"f".repeat(64),30,classification.caseIds());
        steps.set(0,step("CLASSIFICATION_GOLDEN",altered));assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class);
        var good=step("CLASSIFICATION_GOLDEN",classification);steps.set(0,new Step(runId,good.stepCode(),"PASSED",good.evidenceJson(),"f".repeat(64),good.createdAt()));
        assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class).hasMessageContaining("지문");
    }
    @Test void jsonbPropertyOrderIsNotMistakenForEvidenceTampering()throws Exception{
        var sorted=mapper.convertValue(classification,TreeMap.class);var original=steps.getFirst();
        steps.set(0,new Step(runId,original.stepCode(),"PASSED",mapper.writeValueAsString(sorted),original.evidenceHash(),original.createdAt()));
        assertThat(verifier.selectValidatedEvidenceHash(run,steps,frozen)).matches("[0-9a-f]{64}");
    }
    @Test void unknownEvidenceFieldsCannotBeIgnored(){
        var original=steps.getFirst();steps.set(0,new Step(runId,original.stepCode(),"PASSED",original.evidenceJson().replaceFirst("\\{","{\"passedOverride\":true,"),original.evidenceHash(),original.createdAt()));
        assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class);
    }
    @Test void missingDuplicateForeignAndOutOfTimeStepsAreRejected(){
        assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps.subList(0,3),frozen)).isInstanceOf(ApiException.class);
        var original=steps.get(3);steps.set(3,steps.getFirst());assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class);
        steps.set(3,new Step(UUID.randomUUID(),original.stepCode(),"PASSED",original.evidenceJson(),original.evidenceHash(),original.createdAt()));assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class);
        steps.set(3,new Step(runId,original.stepCode(),"PASSED",original.evidenceJson(),original.evidenceHash(),now.plusSeconds(1)));assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class);
    }
    @Test void segmentPublicationRechecksItsOwnEngineAndRejectsLegacyEvidence() throws Exception {
        var settings=Map.of("engineVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION,
                "segmentRuleVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION,
                "segmentRulesHash",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH);
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen(frozen.hash(),mapper.writeValueAsString(Map.of("settings",settings)),frozen.rule(),frozen.runtime());
        run=new Run(runId,policyId,0,ruleId,1,frozen.hash(),frozen.json(),"VERIFIED",2,UUID.randomUUID(),UUID.randomUUID(),"a".repeat(64),null,null,null,now.minusSeconds(90),now.minusSeconds(60),now,true);
        assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,frozen)).isInstanceOf(ApiException.class).hasMessageContaining("분류 정답");
        var result=new AnnouncementAttachmentPolicyGoldenGate.Result(AttachmentSegmentPolicyGoldenGate.SUITE_VERSION,settings.get("engineVersion"),"QA","a".repeat(64),"c".repeat(64),"d".repeat(64),52,
                AnnouncementAttachmentPolicyGoldenGate.selectCaseIds(settings.get("engineVersion")));
        when(golden.selectValidatedResult(any(),any(),any())).thenReturn(result);steps.set(0,step("CLASSIFICATION_GOLDEN",result));
        assertThat(verifier.selectValidatedEvidenceHash(run,steps,frozen)).matches("[0-9a-f]{64}");
        verify(golden,times(2)).selectValidatedResult(frozen.rule().ruleSet(),frozen.rule().calculatedSnapshotHash(),frozen.selectConfiguration());
    }
    @Test void changedFrozenSnapshotCannotReuseQa(){
        var changed=new AttachmentPolicyValidationSnapshotFactory.Frozen("f".repeat(64),frozen.json(),frozen.rule(),frozen.runtime());
        assertThatThrownBy(()->verifier.selectValidatedEvidenceHash(run,steps,changed)).isInstanceOf(ApiException.class);
    }
}
