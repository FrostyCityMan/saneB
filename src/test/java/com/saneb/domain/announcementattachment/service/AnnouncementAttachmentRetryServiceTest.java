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
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentRetryServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentRetryServiceTest {
    private final UUID source=UUID.randomUUID(),base=UUID.randomUUID(),content=UUID.randomUUID(),set=UUID.randomUUID(),evaluation=UUID.randomUUID(),
            policy=UUID.randomUUID(),release=UUID.randomUUID(),actor=UUID.randomUUID(),file=UUID.randomUUID(),key=UUID.randomUUID();
    private final String hash="a".repeat(64);
    private final ObjectMapper mapper=new ObjectMapper();
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentCurrentDao current=mock(AnnouncementAttachmentCurrentDao.class);
    private final AnnouncementAttachmentEvidenceDao evidence=mock(AnnouncementAttachmentEvidenceDao.class);
    private final AnnouncementAttachmentReviewDao reviews=mock(AnnouncementAttachmentReviewDao.class);
    private final AnnouncementAttachmentRoleDao roles=mock(AnnouncementAttachmentRoleDao.class);
    private final AnnouncementAttachmentRetryDao retries=mock(AnnouncementAttachmentRetryDao.class);
    private final AnnouncementSourceDao sources=mock(AnnouncementSourceDao.class);
    private final BizInfoAttachmentDiscoveryProfile profile=new BizInfoAttachmentDiscoveryProfile();
    private final AnnouncementAttachmentRetryService service=new AnnouncementAttachmentRetryServiceImpl(jobs,current,evidence,reviews,roles,retries,sources,
            new AttachmentDiscoveryProfileRegistry(List.of(profile)),mapper);
    private AttachmentJobInsertCommand inserted;
    private AttachmentJobRow saved;
    private AttachmentPolicyRow active;
    private AttachmentFileSummaryRow failed;
    private AttachmentCurrentSourceRow row;
    private AttachmentReviewRequests.Version version() { return new AttachmentReviewRequests.Version(base,evaluation,4,3,hash); }
    private AttachmentRetryRequest request() { return new AttachmentRetryRequest(version(),set,List.of(file),20971520L,"실패 첨부 원문 확인 후 재시도 QA"); }
    private Authentication auth(String role) {
        var principal=new AuthenticatedUserDetails(new AuthUserDetailsRow(actor,"attachment-retry-qa","unused","재시도 QA","ACTIVE",false,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    @BeforeEach void configure() throws Exception {
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",
                base,content,release,"COMBINATION_MATCHED",4,3,true,policy,evaluation));
        row=mock(AttachmentCurrentSourceRow.class);when(row.attachmentDecisionId()).thenReturn(evaluation);when(row.setId()).thenReturn(set);when(row.setHash()).thenReturn(hash);
        when(current.selectSourceDetails(source)).thenReturn(row);
        when(evidence.selectSetDetails(source,set)).thenReturn(new AttachmentSetRow(set,source,content,policy,"FOUND","SEALED",hash,profile.selectProfileHash(),1,1,true,null,null,null,List.of()));
        var execution=new AttachmentExecutionSnapshot(profile.selectProfileCode(),profile.selectProfileHash(),AnnouncementAttachmentClassificationEngine.VERSION,"1.0.0",hash);
        when(roles.selectSetJobDetails(source,set,evaluation)).thenReturn(new AttachmentJobRow(UUID.randomUUID(),source,content,base,release,policy,null,set,
                1,4,2,"PARTIAL_FAILED",3,null,null,null,null,UUID.randomUUID(),hash,mapper.writeValueAsString(execution),83886080L,20L,0));
        String settings=mapper.writeValueAsString(Map.of("engineVersion",execution.engineVersion(),"extractorVersion","1.0.0","extractorConfigHash",hash,"maximumSourceBytes",83886080));
        String manifest=mapper.writeValueAsString(List.of(Map.of("providerCode","BIZINFO","profileCode",profile.selectProfileCode(),"profileHash",profile.selectProfileHash())));
        active=new AttachmentPolicyRow(policy,"ACTIVE","ENFORCE",release,"ACTIVE",hash,settings,manifest,0);when(jobs.selectPolicyDetails(policy)).thenReturn(active);
        failed=mock(AttachmentFileSummaryRow.class);when(failed.fileId()).thenReturn(file);when(failed.downloadStatusCode()).thenReturn("FAILED");
        when(evidence.selectFileList(source,set,0,11)).thenReturn(List.of(failed));when(evidence.selectFileCount(source,set)).thenReturn(1L);
        when(roles.selectFileCopyList(source,set)).thenReturn(List.of(new AttachmentRoleRows.File(file,null,"NOTICE","MANUAL",hash,null,"FAILED",0,null,null)));
        when(retries.selectRetryControlAllowed()).thenReturn(true);when(retries.selectRetryRateAllowed(source)).thenReturn(true);
        when(retries.insertRetryFile(any(),eq(source),eq(set),eq(file))).thenReturn(1);when(jobs.selectNextGeneration(source,content,policy)).thenReturn(2);
        when(jobs.updateAttachmentSourceVersion(source,3)).thenReturn(1);
        when(jobs.insertAttachmentJob(any())).thenAnswer(call->{inserted=call.getArgument(0);saved=new AttachmentJobRow(inserted.jobId(),source,content,base,release,policy,null,null,
                2,4,4,"PENDING",0,null,null,null,null,key,inserted.requestHash(),inserted.executionSnapshotJson(),inserted.downloadBudgetBytes(),0L,0,"RETRY_FILES",set,actor);return 1;});
        when(jobs.selectJobDetails(any())).thenAnswer(call->saved);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR"})
    void reservesOnlySelectedScopeWithoutDownloadingOrActivating(String role) {
        var result=service.insertFileRetry(auth(role),source,key,request());assertThat(result.operationCode()).isEqualTo("RETRY_FILES");
        assertThat(result.jobStatusCode()).isEqualTo("PENDING");assertThat(result.setId()).isNull();
        assertThat(inserted.referenceSetId()).isEqualTo(set);assertThat(inserted.downloadBudgetBytes()).isEqualTo(20971520L);
        verify(retries).insertRetryFile(result.jobId(),source,set,file);verify(jobs).updateAttachmentConfirmationsStale(source);
        verify(evidence,never()).insertSet(any());verify(jobs,never()).updateNewSourceBinding(any(),any(),anyInt());
        var audit=ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);verify(sources).insertAuditLog(audit.capture());
        assertThat(audit.getValue().metadataJson()).contains("\"maximumHttpRequests\":24","reasonHash").doesNotContain(request().reason());
    }
    @Test void unboundRetryRemainsPreview() {
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",base,content,release,
                "COMBINATION_MATCHED",4,3,false,null,evaluation));service.insertFileRetry(auth("ADMIN"),source,key,request());assertThat(inserted.applyToSource()).isFalse();
    }
    @Test void sameKeyIsIdempotentAndDoesNotConsumeRateLimitAgain() {
        var first=service.insertFileRetry(auth("ADMIN"),source,key,request());when(jobs.selectIdempotentJobDetails(key)).thenReturn(saved);
        when(retries.selectRetryRateAllowed(source)).thenReturn(false);
        assertThat(service.insertFileRetry(auth("ADMIN"),source,key,request())).isEqualTo(first);
        verify(jobs,times(1)).insertAttachmentJob(any());verify(retries,times(1)).selectRetryRateAllowed(source);
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,new AttachmentRetryRequest(version(),set,List.of(file),1L,"다른 요청")),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
    }
    @ParameterizedTest @ValueSource(strings={"APPROVER","USER","PARTNER","REVIEWER"})
    void nonWriterServiceCallsCannotReserve(String role) { expect(()->service.insertFileRetry(auth(role),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN);verifyNoInteractions(sources); }
    @ParameterizedTest @ValueSource(strings={"COMPLETE_TEXT","OCR_REQUIRED","ENCRYPTED","UNSUPPORTED"})
    void successAndManualOnlyFormatsCannotBeSelectedAsFailed(String quality) {
        when(failed.downloadStatusCode()).thenReturn("SUCCEEDED");when(failed.qualityCode()).thenReturn(quality);
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_INVALID);verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void offControlAndRetiredPolicyPreventNewNetworkReservation() {
        when(retries.selectRetryControlAllowed()).thenReturn(false);expect(()->service.insertFileRetry(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        when(retries.selectRetryControlAllowed()).thenReturn(true);
        when(jobs.selectPolicyDetails(policy)).thenReturn(new AttachmentPolicyRow(policy,"RETIRED","ENFORCE",release,"ACTIVE",hash,active.settingsJson(),active.profileManifestJson(),1));
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void staleSetForeignFileAndDuplicateSelectionAreRejectedBeforeWrite() {
        when(row.setHash()).thenReturn("b".repeat(64));expect(()->service.insertFileRetry(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        when(row.setHash()).thenReturn(hash);
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,new AttachmentRetryRequest(version(),set,List.of(UUID.randomUUID()),1L,"QA")),ErrorCode.RESOURCE_NOT_FOUND);
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,new AttachmentRetryRequest(version(),set,List.of(file,file),1L,"QA")),ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_INVALID);
        verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void requestBudgetAndServerRateCapsAreEnforced() {
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,new AttachmentRetryRequest(version(),set,List.of(file),83886081L,"QA")),ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_INVALID);
        when(retries.selectRetryRateAllowed(source)).thenReturn(false);
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_RATE_LIMITED);verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void protectedLinkAndRunningWorkCannotBeOverwritten() {
        when(reviews.selectActiveNormalJobExists(source)).thenReturn(true);expect(()->service.insertFileRetry(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        when(reviews.selectActiveNormalJobExists(source)).thenReturn(false);when(reviews.selectConversionLinkDetails(source)).thenReturn(mock(AttachmentReviewRows.Link.class));
        expect(()->service.insertFileRetry(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);verify(jobs,never()).insertAttachmentJob(any());
    }
    private void expect(Runnable action,ErrorCode code) { assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.errorCode()).isEqualTo(code)); }
}
