package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentCollectionServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AnnouncementAttachmentCollectionServiceTest {
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentIntakeDao intake=mock(AnnouncementAttachmentIntakeDao.class);
    private final AnnouncementAttachmentCollectionDao collections=mock(AnnouncementAttachmentCollectionDao.class);
    private final AnnouncementAttachmentRetryDao retries=mock(AnnouncementAttachmentRetryDao.class);
    private final AnnouncementSourceDao sources=mock(AnnouncementSourceDao.class);
    private final ObjectMapper mapper=new ObjectMapper();
    private final BizInfoAttachmentDiscoveryProfile profile=new BizInfoAttachmentDiscoveryProfile();
    private final AttachmentDiscoveryProfileRegistry profiles=new AttachmentDiscoveryProfileRegistry(List.of(profile));
    private final AnnouncementAttachmentCollectionService service=new AnnouncementAttachmentCollectionServiceImpl(jobs,intake,collections,retries,sources,profiles,mapper,true);
    private final UUID source=UUID.randomUUID(),actor=UUID.randomUUID(),base=UUID.randomUUID(),content=UUID.randomUUID(),release=UUID.randomUUID(),policy=UUID.randomUUID(),key=UUID.randomUUID();
    private final String hash="a".repeat(64);
    private AttachmentSourceContextRow sourceRow;
    private AttachmentPolicyRow policyRow;
    private AttachmentJobInsertCommand inserted;
    private AttachmentJobRow saved;
    private Authentication auth(String role) { return auth(actor,role,"ACTIVE",false); }
    private Authentication auth(UUID id,String role,String status,boolean reset) {
        var principal=new AuthenticatedUserDetails(new AuthUserDetailsRow(id,"attachment-collection-qa","unused","수집 QA",status,reset,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    private void source(boolean required,UUID current) {
        sourceRow=new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",base,content,release,"COMBINATION_MATCHED",4,3,required,required?policy:null,current);
        when(jobs.selectSourceContextDetails(source)).thenReturn(sourceRow);when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(sourceRow);
    }
    private void policy(String mode,String state,String settings,String manifest) {
        policyRow=new AttachmentPolicyRow(policy,state,mode,release,"ACTIVE",hash,settings,manifest,0);
        when(collections.selectActivePolicyDetails(eq(release),anyBoolean())).thenReturn(policyRow);
    }
    @BeforeEach void configure() throws Exception {
        source(false,null);
        policy("COLLECT_ONLY","ACTIVE",mapper.writeValueAsString(Map.of("engineVersion",AnnouncementAttachmentClassificationEngine.VERSION,
                "extractorVersion","1.0.0","extractorConfigHash",hash,"maximumSourceBytes",100)),mapper.writeValueAsString(List.of(Map.of(
                "providerCode","BIZINFO","profileCode",profile.selectProfileCode(),"profileHash",profile.selectProfileHash()))));
        when(intake.selectSourceLocatorDetails(source)).thenReturn(new AttachmentWorkerSourceRow("BIZINFO","PBLN_202600000000001",null,null,null));
        when(retries.selectRetryControlAllowed()).thenReturn(true);when(collections.selectManualRequestRateAllowed(source)).thenReturn(true);
        when(jobs.selectNextGeneration(source,content,policy)).thenReturn(2);when(jobs.updateAttachmentSourceVersion(source,3)).thenReturn(1);
        when(intake.updateSourceIntakeStatus(source,"QUEUED")).thenReturn(1);
        when(jobs.insertAttachmentJob(any())).thenAnswer(call->{inserted=call.getArgument(0);saved=new AttachmentJobRow(inserted.jobId(),source,content,base,release,policy,null,null,
                2,4,4,"PENDING",0,null,null,null,null,key,inserted.requestHash(),inserted.executionSnapshotJson(),inserted.downloadBudgetBytes(),0L,0,"COLLECT",null,inserted.requestedBy());return 1;});
        when(jobs.selectJobDetails(any())).thenAnswer(call->saved);
    }
    private AttachmentCollectionRequests.Request request() {
        var context=service.selectCollectionContextDetails(auth("ADMIN"),source);
        return new AttachmentCollectionRequests.Request(context.version(),context.policyId(),context.policyHash(),context.executionHash(),80L,"전체 첨부 수집 QA");
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readContextHasInitialNullDecisionAndFixedLimitsWithoutAnyWriteOrLock(String role) {
        var context=service.selectCollectionContextDetails(auth(role),source);
        assertThat(context.version().expectedAttachmentDecisionId()).isNull();assertThat(context.maximumHttpRequests()).isEqualTo(132);
        assertThat(context.maximumFileCount()).isEqualTo(10);assertThat(context.maximumAttempts()).isEqualTo(3);
        assertThat(context.maximumDownloadBytes()).isEqualTo(100);assertThat(context.effectCode()).isEqualTo("COLLECT_PREVIEW_ONLY");
        assertThat(context.executionHash()).matches("[0-9a-f]{64}");verify(collections).selectActivePolicyDetails(release,false);
        verify(jobs,never()).selectSourceContextDetailsForUpdate(any());verify(jobs,never()).insertAttachmentJob(any());verifyNoInteractions(sources);
    }
    @ParameterizedTest @CsvSource({"ADMIN,COLLECT_ONLY,false","OPERATOR,ENFORCE,false","ADMIN,ENFORCE,true"})
    void reservationUsesServerProfileAndNeverCreatesNewEnforceBinding(String role,String mode,boolean required) throws Exception {
        policy(mode,"ACTIVE",policyRow.settingsJson(),policyRow.profileManifestJson());UUID previous=UUID.randomUUID();source(required,previous);
        var request=request();var result=service.insertCollectionJob(auth(role),source,key,request);
        assertThat(result.jobStatusCode()).isEqualTo("PENDING");assertThat(result.setId()).isNull();assertThat(inserted.operationCode()).isEqualTo("COLLECT");
        assertThat(inserted.requestedBy()).isEqualTo(actor);assertThat(inserted.collectionRunId()).isNull();assertThat(inserted.referenceSetId()).isNull();
        assertThat(inserted.downloadBudgetBytes()).isEqualTo(80);assertThat(inserted.applyToSource()).isEqualTo(required);
        assertThat(inserted.previousReviewRequired()).isEqualTo(required);assertThat(inserted.previousEvaluationId()).isEqualTo(previous);
        var execution=mapper.readValue(inserted.executionSnapshotJson(),AttachmentExecutionSnapshot.class);
        assertThat(execution.profileCode()).isEqualTo(profile.selectProfileCode());assertThat(execution.profileHash()).isEqualTo(profile.selectProfileHash());
        verify(collections).selectActivePolicyDetails(release,true);verify(jobs).updateAttachmentEvaluationsStale(source);
        verify(jobs).updateAttachmentConfirmationsStale(source);verify(jobs,never()).updateNewSourceBinding(any(),any(),anyInt());
        verify(intake).updateSourceIntakeStatus(source,"QUEUED");
        var audit=org.mockito.ArgumentCaptor.forClass(com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand.class);
        verify(sources).insertAuditLog(audit.capture());String metadata=mapper.writeValueAsString(audit.getValue());
        assertThat(metadata).contains("132","maximumFileCount","reasonHash").doesNotContain(request.reason(),"PBLN_202600000000001");
    }
    @Test void sameKeyReturnsOriginalAfterVersionsOrWorkerAvailabilityChangeWithoutChargingAnotherRequest() {
        var request=request();service.insertCollectionJob(auth("ADMIN"),source,key,request);when(jobs.selectIdempotentJobDetails(key)).thenReturn(saved);
        var disabled=new AnnouncementAttachmentCollectionServiceImpl(jobs,intake,collections,retries,sources,profiles,mapper,false);
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",
                base,content,release,"COMBINATION_MATCHED",4,4,false,null,UUID.randomUUID()));
        clearInvocations(collections,intake,jobs,sources);
        assertThat(disabled.insertCollectionJob(auth("ADMIN"),source,key,request).jobId()).isEqualTo(saved.jobId());
        verifyNoInteractions(collections,intake,sources);verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void sameKeyCannotBeUsedByAnotherActorOrDifferentBody() {
        var request=request();service.insertCollectionJob(auth("ADMIN"),source,key,request);when(jobs.selectIdempotentJobDetails(key)).thenReturn(saved);
        assertThatThrownBy(()->service.insertCollectionJob(auth(UUID.randomUUID(),"ADMIN","ACTIVE",false),source,key,request)).isInstanceOf(ApiException.class);
        var changed=new AttachmentCollectionRequests.Request(request.version(),policy,hash,request.expectedExecutionHash(),81L,request.reason());
        assertThatThrownBy(()->service.insertCollectionJob(auth("ADMIN"),source,key,changed)).isInstanceOf(ApiException.class);
        verify(jobs,times(1)).insertAttachmentJob(any());
    }
    @Test void staleBaseAttachmentVersionAndPolicyFingerprintCannotReserve() {
        var request=request();var stale=new AttachmentCollectionRequests.Request(new AttachmentCollectionRequests.Version(base,UUID.randomUUID(),4,3),policy,hash,request.expectedExecutionHash(),80L,"QA");
        assertThatThrownBy(()->service.insertCollectionJob(auth("ADMIN"),source,key,stale)).isInstanceOf(ApiException.class).hasMessageContaining("버전");
        var changed=new AttachmentCollectionRequests.Request(request.version(),policy,"b".repeat(64),request.expectedExecutionHash(),80L,"QA");
        assertThatThrownBy(()->service.insertCollectionJob(auth("ADMIN"),source,key,changed)).isInstanceOf(ApiException.class).hasMessageContaining("정책");
        verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void unavailablePolicyProfileAndGlobalOffDoNotStartAnyCollection() throws Exception {
        when(collections.selectActivePolicyDetails(eq(release),anyBoolean())).thenReturn(null);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("ACTIVE");
        policy("OFF","ACTIVE",policyRow.settingsJson(),policyRow.profileManifestJson());
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class);
        policy("ENFORCE","RETIRED",policyRow.settingsJson(),policyRow.profileManifestJson());
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class);
        policy("COLLECT_ONLY","ACTIVE",policyRow.settingsJson(),"[]");
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("profile");
        when(retries.selectRetryControlAllowed()).thenReturn(false);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("OFF");
        verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void duplicateOrInvalidProfileAndExecutionDoNotAllowParserSelection() {
        String original=policyRow.profileManifestJson();policy("COLLECT_ONLY","ACTIVE",policyRow.settingsJson(),"["+original.substring(1,original.length()-1)+","+original.substring(1,original.length()-1)+"]");
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("profile");
        policy("COLLECT_ONLY","ACTIVE","{\"maximumSourceBytes\":100,\"engineVersion\":\"old\"}",original);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class);
        verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void linkedOrActiveSourceAndSharedManualLimitAreExplicitBlockers() {
        when(intake.selectProtectedLinkExists(source)).thenReturn(true);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("연결");
        when(intake.selectProtectedLinkExists(source)).thenReturn(false);when(intake.selectActiveJobId(source)).thenReturn(UUID.randomUUID());
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("진행 중");
        when(intake.selectActiveJobId(source)).thenReturn(null);when(collections.selectManualRequestRateAllowed(source)).thenReturn(false);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOfSatisfying(ApiException.class,
                e->assertThat(e.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_ATTACHMENT_COLLECTION_RATE_LIMITED));
    }
    @ParameterizedTest @ValueSource(strings={"EXCLUDED","QA","NO_TITLE"})
    void excludedQaAndUnclassifiedSourcesCannotResolveOrReserve(String state) {
        when(jobs.selectSourceContextDetails(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","QA".equals(state)?"QA":"PRODUCTION",
                state,base,content,release,"NO_TITLE".equals(state)?null:"COMBINATION_MATCHED",0,0,false,null,null));
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class);
        verifyNoInteractions(intake,collections,retries,sources);
    }
    @ParameterizedTest @ValueSource(longs={0,-1,83886081,Long.MAX_VALUE})
    void invalidBudgetsAreRejectedBeforeSourceLock(long budget) {
        var request=request();clearInvocations(jobs);
        var invalid=new AttachmentCollectionRequests.Request(request.version(),policy,hash,request.expectedExecutionHash(),budget,"QA");
        assertThatThrownBy(()->service.insertCollectionJob(auth("ADMIN"),source,key,invalid)).isInstanceOf(ApiException.class);
        verifyNoInteractions(jobs);
    }
    @Test void policyBudgetCannotBeRaisedAndExistingBindingCannotBeChanged() {
        var request=request();var high=new AttachmentCollectionRequests.Request(request.version(),policy,hash,request.expectedExecutionHash(),101L,"QA");
        assertThatThrownBy(()->service.insertCollectionJob(auth("ADMIN"),source,key,high)).isInstanceOf(ApiException.class).hasMessageContaining("상한");
        when(jobs.selectSourceContextDetails(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",base,content,release,"COMBINATION_MATCHED",4,3,true,UUID.randomUUID(),null));
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("검수 정책");
        verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void actorsNeedActiveAuthorizedAccountAndCompletedPasswordReset() {
        assertThatThrownBy(()->service.selectCollectionContextDetails(null,source)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth("USER"),source)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth(actor,"ADMIN","INACTIVE",false),source)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectCollectionContextDetails(auth(actor,"ADMIN","ACTIVE",true),source)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertCollectionJob(auth("APPROVER"),source,key,null)).isInstanceOf(ApiException.class).hasMessageContaining("ADMIN");
        verifyNoInteractions(jobs,intake,collections,retries,sources);
    }
    @Test void disabledWorkerDoesNotQueueNewRequests() {
        var disabled=new AnnouncementAttachmentCollectionServiceImpl(jobs,intake,collections,retries,sources,profiles,mapper,false);
        assertThatThrownBy(()->disabled.selectCollectionContextDetails(auth("ADMIN"),source)).isInstanceOf(ApiException.class).hasMessageContaining("비활성");
        verifyNoInteractions(intake,collections,retries,sources);
    }
    @Test void lostSourceCasCannotReportSuccessOrWriteAudit() {
        var request=request();when(jobs.updateAttachmentSourceVersion(source,3)).thenReturn(0);
        assertThatThrownBy(()->service.insertCollectionJob(auth("ADMIN"),source,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("버전");
        verify(jobs,never()).updateAttachmentEvaluationsStale(any());verifyNoInteractions(sources);
    }
    @Test void requestKeyCollisionFromAnotherSourceCannotMutateTheLoserSource() {
        var request=request();var collision=mock(AttachmentJobRow.class);when(collision.sourceId()).thenReturn(UUID.randomUUID());
        when(jobs.selectIdempotentJobDetails(key)).thenReturn(null,collision);doReturn(0).when(jobs).insertAttachmentJob(any());
        assertThatThrownBy(()->service.insertCollectionJob(auth("ADMIN"),source,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        verify(jobs,never()).updateAttachmentSourceVersion(any(),anyInt());verifyNoInteractions(sources);
    }
    @Test void coreReservationRejectsNullTitleStageWithPolicyErrorInsteadOfNullPointer() {
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",
                base,content,release,null,4,3,false,null,null));
        var core=new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentJobServiceImpl(jobs,mapper);
        var execution=new AttachmentExecutionSnapshot(profile.selectProfileCode(),profile.selectProfileHash(),AnnouncementAttachmentClassificationEngine.VERSION,"1.0.0",hash);
        assertThatThrownBy(()->core.insertAttachmentJob(new AttachmentJobReservation(source,policy,base,4,3,key,execution)))
                .isInstanceOf(ApiException.class).hasMessageContaining("제목 수집 기준");
        verify(jobs,never()).selectPolicyDetails(any());verify(jobs,never()).insertAttachmentJob(any());
    }
}
