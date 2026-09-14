package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentRoleRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentRoleServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentRoleServiceTest {
    private final UUID source=UUID.randomUUID(),base=UUID.randomUUID(),content=UUID.randomUUID(),set=UUID.randomUUID(),evaluation=UUID.randomUUID(),
            policy=UUID.randomUUID(),release=UUID.randomUUID(),actor=UUID.randomUUID(),file=UUID.randomUUID(),extraction=UUID.randomUUID(),key=UUID.randomUUID();
    private final String hash="a".repeat(64);
    private final ObjectMapper mapper=new ObjectMapper();
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentCurrentDao current=mock(AnnouncementAttachmentCurrentDao.class);
    private final AnnouncementAttachmentEvidenceDao evidence=mock(AnnouncementAttachmentEvidenceDao.class);
    private final AnnouncementAttachmentReviewDao reviews=mock(AnnouncementAttachmentReviewDao.class);
    private final AnnouncementAttachmentRoleDao roles=mock(AnnouncementAttachmentRoleDao.class);
    private final AnnouncementSourceDao sources=mock(AnnouncementSourceDao.class);
    private final AnnouncementAttachmentRoleService service=new AnnouncementAttachmentRoleServiceImpl(jobs,current,evidence,reviews,roles,sources,mapper);
    private AttachmentJobInsertCommand inserted;
    private AttachmentJobRow saved;
    private AttachmentReviewRequests.Version version() { return new AttachmentReviewRequests.Version(base,evaluation,4,3,hash); }
    private AttachmentRoleRequest request() { return new AttachmentRoleRequest(version(),set,List.of(new AttachmentRoleRequest.FileRole(file,"GUIDE")),"공개 원문의 실제 문서 역할 확인 QA"); }
    private Authentication auth(String role) {
        var principal=new AuthenticatedUserDetails(new AuthUserDetailsRow(actor,"attachment-role-qa","unused","역할 QA","ACTIVE",false,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    private AttachmentSetRow setRow(String state) { return new AttachmentSetRow(set,source,content,policy,"FOUND",state,hash,hash,1,1,true,null,null,null,List.of()); }
    private AttachmentCurrentSourceRow ready(boolean bound) throws Exception {
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",
                base,content,release,"COMBINATION_MATCHED",4,3,bound,bound?policy:null,evaluation));
        var row=mock(AttachmentCurrentSourceRow.class); when(row.attachmentDecisionId()).thenReturn(evaluation); when(row.setId()).thenReturn(set); when(row.setHash()).thenReturn(hash);
        when(current.selectSourceDetails(source)).thenReturn(row); when(evidence.selectSetDetails(source,set)).thenReturn(setRow("SEALED"));
        var execution=new AttachmentExecutionSnapshot("BIZINFO",hash,AnnouncementAttachmentClassificationEngine.VERSION,"1.0.0",hash);
        var original=new AttachmentJobRow(UUID.randomUUID(),source,content,base,release,policy,null,set,1,4,2,"SUCCEEDED",1,null,null,null,null,
                UUID.randomUUID(),hash,mapper.writeValueAsString(execution),83886080L,20L,0);
        when(roles.selectSetJobDetails(source,set,evaluation)).thenReturn(original);
        when(jobs.selectPolicyDetails(policy)).thenReturn(new AttachmentPolicyRow(policy,"RETIRED","COLLECT_ONLY",release,"RETIRED",hash,"{}","[]",0));
        when(roles.selectFileCopyList(source,set)).thenReturn(List.of(new AttachmentRoleRows.File(file,extraction,"NOTICE","PROFILE",hash,hash,"SUCCEEDED",0,"1.0.0",hash)));
        when(evidence.selectFileCount(source,set)).thenReturn(1L); when(jobs.selectNextGeneration(source,content,policy)).thenReturn(2);
        when(evidence.insertSet(any())).thenReturn(1); when(roles.insertFileCopy(any())).thenReturn(1); when(roles.insertExtractionCopy(any())).thenReturn(1);
        when(evidence.updateSetSealed(any(),any(),any(),anyBoolean(),anyInt(),any())).thenReturn(1); when(jobs.updateAttachmentSourceVersion(source,3)).thenReturn(1);
        when(jobs.insertAttachmentJob(any())).thenAnswer(i->{
            inserted=i.getArgument(0);
            saved=new AttachmentJobRow(inserted.jobId(),source,content,base,release,policy,null,inserted.readySetId(),2,4,4,"PENDING",0,null,null,null,null,
                    key,inserted.requestHash(),inserted.executionSnapshotJson(),inserted.downloadBudgetBytes(),0L,0,"ROLE_CHANGE",set,actor);
            return 1;
        });
        when(jobs.selectJobDetails(any())).thenAnswer(i->saved);
        return row;
    }
    private void expect(Runnable action,ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.errorCode()).isEqualTo(code));
    }
    @Test void roleChangeCopiesEvidenceIntoNewSealedSetThenQueuesOnlyEvaluation() throws Exception {
        ready(true); var result=service.insertRoleChange(auth("ADMIN"),source,key,request());
        assertThat(result.operationCode()).isEqualTo("ROLE_CHANGE"); assertThat(result.jobStatusCode()).isEqualTo("PENDING");
        assertThat(result.setId()).isNotEqualTo(set); assertThat(result.attachmentVersionAtReservation()).isEqualTo(4);
        assertThat(inserted.referenceSetId()).isEqualTo(set); assertThat(inserted.applyToSource()).isTrue();
        var copy=ArgumentCaptor.forClass(AttachmentRoleRows.Copy.class); verify(roles).insertExtractionCopy(copy.capture());
        assertThat(copy.getValue().originalExtractionId()).isEqualTo(extraction); assertThat(copy.getValue().newExtractionId()).isNotEqualTo(extraction);
        assertThat(copy.getValue().role()).isEqualTo("GUIDE"); assertThat(copy.getValue().roleOrigin()).isEqualTo("MANUAL");
        verify(jobs).updateAttachmentEvaluationsStale(source); verify(jobs).updateAttachmentConfirmationsStale(source);
        verify(jobs,never()).updateJobDownloadBudget(any(),any(),org.mockito.ArgumentMatchers.anyLong());
        var audit=ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class); verify(sources).insertAuditLog(audit.capture());
        assertThat(audit.getValue().metadataJson()).contains("reasonHash").doesNotContain("공개 원문의 실제");
    }
    @Test void unboundCollectOnlyRoleChangeRemainsPreviewAndDoesNotEnforce() throws Exception {
        ready(false); service.insertRoleChange(auth("OPERATOR"),source,key,request());
        assertThat(inserted.applyToSource()).isFalse(); assertThat(inserted.previousReviewRequired()).isFalse();
        verify(jobs,never()).updateNewSourceBinding(any(),any(),anyInt());
    }
    @Test void exactIdempotentRequestReusesJobWhileChangedReasonConflicts() throws Exception {
        ready(true); var first=service.insertRoleChange(auth("ADMIN"),source,key,request());
        when(jobs.selectIdempotentJobDetails(key)).thenReturn(saved);
        assertThat(service.insertRoleChange(auth("ADMIN"),source,key,request())).isEqualTo(first);
        var changed=new AttachmentRoleRequest(version(),set,request().fileRoles(),"다른 요청 사유");
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,changed),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(jobs,org.mockito.Mockito.times(1)).insertAttachmentJob(any());
    }
    @Test void foreignIdempotencyActorCannotReuseResult() throws Exception {
        ready(true); service.insertRoleChange(auth("ADMIN"),source,key,request());
        when(jobs.selectIdempotentJobDetails(key)).thenReturn(new AttachmentJobRow(saved.jobId(),source,content,base,release,policy,null,saved.setId(),2,4,4,
                "PENDING",0,null,null,null,null,key,saved.requestHash(),saved.executionSnapshotJson(),83886080L,0L,0,"ROLE_CHANGE",set,UUID.randomUUID()));
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
    }
    @Test void wrongSourceSetOrFileIs404() throws Exception {
        ready(true); when(evidence.selectSetDetails(source,set)).thenReturn(null);
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,request()),ErrorCode.RESOURCE_NOT_FOUND);
        when(evidence.selectSetDetails(source,set)).thenReturn(setRow("SEALED"));
        var wrongFile=new AttachmentRoleRequest(version(),set,List.of(new AttachmentRoleRequest.FileRole(UUID.randomUUID(),"GUIDE")),"잘못된 파일 QA");
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,wrongFile),ErrorCode.RESOURCE_NOT_FOUND);
        verify(evidence,never()).insertSet(any());
    }
    @Test void staleVersionOrManifestCannotCloneEvidence() throws Exception {
        var row=ready(true); when(row.setHash()).thenReturn("b".repeat(64));
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(roles,never()).insertFileCopy(any());
    }
    @Test void openSetAndBusyNormalJobAreNotReady() throws Exception {
        ready(true); when(evidence.selectSetDetails(source,set)).thenReturn(setRow("OPEN"));
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY);
        when(evidence.selectSetDetails(source,set)).thenReturn(setRow("SEALED")); when(reviews.selectActiveNormalJobExists(source)).thenReturn(true);
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY);
        verify(evidence,never()).insertSet(any());
    }
    @Test void sameRolesAreNotANewGeneration() throws Exception {
        ready(true); var noChange=new AttachmentRoleRequest(version(),set,List.of(new AttachmentRoleRequest.FileRole(file,"NOTICE")),"동일 역할 QA");
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,noChange),ErrorCode.ANNOUNCEMENT_ATTACHMENT_ROLE_INVALID);
        verify(jobs,never()).insertAttachmentJob(any());
    }
    @Test void duplicatesAndInvalidRoleFailBeforeDatabase() {
        var duplicate=new AttachmentRoleRequest(version(),set,List.of(new AttachmentRoleRequest.FileRole(file,"GUIDE"),new AttachmentRoleRequest.FileRole(file,"NOTICE")),"중복 QA");
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,duplicate),ErrorCode.ANNOUNCEMENT_ATTACHMENT_ROLE_INVALID);
        var invalid=new AttachmentRoleRequest(version(),set,List.of(new AttachmentRoleRequest.FileRole(file,"EXECUTE_SCRIPT")),"잘못된 역할 QA");
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,invalid),ErrorCode.ANNOUNCEMENT_ATTACHMENT_ROLE_INVALID);
        verifyNoInteractions(jobs,roles,evidence);
    }
    @Test void linkedAnnouncementCannotBeAffectedByRoleChange() throws Exception {
        ready(true); when(reviews.selectConversionLinkDetails(source)).thenReturn(new AttachmentReviewRows.Link(UUID.randomUUID(),"ANN-QA",null,null));
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(evidence,never()).insertSet(any());
    }
    @ParameterizedTest @ValueSource(strings={"APPROVER","USER","PARTNER","REVIEWER"})
    void nonWritersCannotChangeRolesThroughDirectService(String role) {
        expect(()->service.insertRoleChange(auth(role),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN);
        verifyNoInteractions(jobs,roles,evidence);
    }
    @Test void changedExtractorContractCannotRelabelOldOutputAsNewExtraction() throws Exception {
        ready(true); when(roles.selectFileCopyList(source,set)).thenReturn(List.of(new AttachmentRoleRows.File(file,extraction,"NOTICE","PROFILE",hash,hash,"SUCCEEDED",0,"2.0.0",hash)));
        expect(()->service.insertRoleChange(auth("ADMIN"),source,key,request()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(roles,never()).insertExtractionCopy(any());
    }
    @Test void readonlyJobResultIsSourceScopedAndDoesNotExposeLeaseOrHash() throws Exception {
        ready(true); var roleJob=service.insertRoleChange(auth("ADMIN"),source,key,request());
        assertThat(service.selectJobDetails(source,roleJob.jobId())).isEqualTo(roleJob);
        assertThat(mapper.writeValueAsString(roleJob)).doesNotContain("leaseToken","requestHash","executionSnapshot");
        expect(()->service.selectJobDetails(UUID.randomUUID(),roleJob.jobId()),ErrorCode.RESOURCE_NOT_FOUND);
    }
}
