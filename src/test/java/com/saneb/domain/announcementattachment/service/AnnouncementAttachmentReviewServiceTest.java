package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentReviewServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentReviewServiceTest {
    private final UUID source=UUID.randomUUID(),base=UUID.randomUUID(),evaluation=UUID.randomUUID(),policy=UUID.randomUUID(),
            release=UUID.randomUUID(),content=UUID.randomUUID(),setId=UUID.randomUUID(),actor=UUID.randomUUID(),key=UUID.randomUUID(),confirmation=UUID.randomUUID();
    private final String hash="a".repeat(64);
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentCurrentDao current=mock(AnnouncementAttachmentCurrentDao.class);
    private final AnnouncementAttachmentEvaluationDao evaluations=mock(AnnouncementAttachmentEvaluationDao.class);
    private final AnnouncementAttachmentEvidenceDao evidence=mock(AnnouncementAttachmentEvidenceDao.class);
    private final AnnouncementAttachmentReviewDao reviews=mock(AnnouncementAttachmentReviewDao.class);
    private final AnnouncementSourceDao sources=mock(AnnouncementSourceDao.class);
    private final AnnouncementDao announcements=mock(AnnouncementDao.class);
    private final AnnouncementAttachmentReviewService service=new AnnouncementAttachmentReviewServiceImpl(jobs,current,evaluations,evidence,reviews,sources,announcements,new ObjectMapper());
    private AttachmentReviewRows.Confirmation saved;
    private AttachmentReviewRows.Link savedLink;
    private AttachmentReviewRequests.Version version(int attachmentVersion) { return new AttachmentReviewRequests.Version(base,evaluation,4,attachmentVersion,hash); }
    private AttachmentReviewRequests.Confirmation request(int attachmentVersion,String method,List<String> codes) {
        return new AttachmentReviewRequests.Confirmation(version(attachmentVersion),List.of("PERSONAL","BUSINESS"),List.of("POLICY_FINANCE"),method,codes,"공개 원문 전체를 직접 확인한 QA 검수 사유");
    }
    private Authentication auth(String role) {
        var details=new AuthenticatedUserDetails(new AuthUserDetailsRow(actor,"attachment-qa","unused","검수 QA","ACTIVE",false,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(details,null,details.getAuthorities());
    }
    private AttachmentCurrentSourceRow ready(boolean bound,int attachmentVersion) {
        var context=new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","ACCEPTED",base,content,release,"COMBINATION_MATCHED",4,attachmentVersion,bound,policy,evaluation);
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(context);
        when(jobs.selectSourceContextDetails(source)).thenReturn(context);
        var row=mock(AttachmentCurrentSourceRow.class);
        when(row.sourceId()).thenReturn(source); when(row.baseDecisionId()).thenReturn(base);
        when(row.attachmentDecisionId()).thenReturn(evaluation); when(row.sourceVersion()).thenReturn(4);
        when(row.attachmentVersion()).thenReturn(attachmentVersion); when(row.setId()).thenReturn(setId); when(row.setHash()).thenReturn(hash);
        when(current.selectSourceDetails(source)).thenReturn(row);
        when(evaluations.selectEvaluationDetails(source,evaluation)).thenReturn(decision("ACCEPTED","TARGET_SUPPORT_MATCH","[]"));
        when(evidence.selectSetDetails(source,setId)).thenReturn(set("NO_FILES","SEALED",true));
        when(evidence.selectFileList(source,setId,0,11)).thenReturn(List.of());
        when(reviews.selectEnabledTargetCodeList(any())).thenAnswer(i->i.getArgument(0));
        when(reviews.selectEnabledSupportCodeList(any())).thenAnswer(i->i.getArgument(0));
        when(reviews.updateSourceVersion(source,4,attachmentVersion,evaluation)).thenReturn(1);
        return row;
    }
    private AttachmentEvaluationRows.Evaluation decision(String status,String reason,String warnings) {
        return new AttachmentEvaluationRows.Evaluation(evaluation,source,base,setId,policy,release,"v1",hash,hash,status,reason,warnings,true,OffsetDateTime.now());
    }
    private AttachmentSetRow set(String discovery,String state,boolean complete) {
        return new AttachmentSetRow(setId,source,content,policy,discovery,state,hash,hash,0,0,complete,null,null,null,List.of());
    }
    private AttachmentReviewRows.Confirmation confirmed(boolean current,int version) {
        return new AttachmentReviewRows.Confirmation(confirmation,source,evaluation,hash,hash,"EXTRACTED_TEXT",current,4,version,OffsetDateTime.now());
    }
    private void confirmationPersistence() {
        when(reviews.insertConfirmation(any())).thenAnswer(i->{
            AttachmentReviewRows.ConfirmationInsert c=i.getArgument(0);
            saved=new AttachmentReviewRows.Confirmation(c.id(),c.sourceId(),c.evaluationId(),c.setHash(),c.requestHash(),c.method(),true,c.sourceVersion(),c.attachmentVersion(),OffsetDateTime.now());
            return 1;
        });
        when(reviews.selectConfirmationDetails(eq(source),any())).thenAnswer(i->saved);
        when(reviews.insertConfirmedTarget(any())).thenReturn(1); when(reviews.insertConfirmedSupport(any())).thenReturn(1);
    }
    private void draftReady() {
        ready(true,4);
        when(reviews.selectConfirmationDetails(source,confirmation)).thenReturn(confirmed(true,4));
        when(reviews.selectConfirmedTargetCodeList(confirmation)).thenReturn(List.of("BUSINESS","PERSONAL"));
        when(reviews.selectConfirmedSupportCodeList(confirmation)).thenReturn(List.of("POLICY_FINANCE"));
        var body=mock(AnnouncementSourceSnapshotRow.class); when(body.publicCode()).thenReturn("SRC-QA"); when(body.title()).thenReturn("QA 공고");
        when(sources.selectSourceDetails(source)).thenReturn(body);
        var draft=mock(AnnouncementDetailsRow.class); when(draft.approvalStatusCode()).thenReturn("DRAFT"); when(draft.announcementCode()).thenReturn("ANN-QA");
        when(announcements.selectAnnouncementDetails(any())).thenReturn(draft);
        when(reviews.insertConversionLink(any())).thenAnswer(i->{ AttachmentReviewRows.LinkInsert c=i.getArgument(0);
            savedLink=new AttachmentReviewRows.Link(c.announcementId(),"ANN-QA",c.confirmationId(),c.requestHash()); return 1; });
        when(sources.updateSourceReviewStatus(any())).thenReturn(1);
    }
    private AttachmentReviewRequests.Conversion conversion() { return new AttachmentReviewRequests.Conversion(version(4),confirmation,"BUSINESS",null); }
    private void expectError(Runnable action,ErrorCode error) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.errorCode()).isEqualTo(error));
    }
    @Test void confirmationStoresNormalizedTagsAndNewVersionWithoutChangingAutomaticDecision() {
        ready(true,3); confirmationPersistence();
        var response=service.insertConfirmation(auth("OPERATOR"),source,key,request(3,"EXTRACTED_TEXT",List.of()));
        assertThat(response.attachmentVersion()).isEqualTo(4); assertThat(response.sourceVersion()).isEqualTo(4);
        assertThat(response.evaluationId()).isEqualTo(evaluation); assertThat(response.isCurrent()).isTrue();
        verify(reviews).updateConfirmationsStale(source); verify(reviews,times(2)).insertConfirmedTarget(any());
        verify(evaluations,never()).updateEvaluationCurrent(any(),any()); verify(sources,never()).updateSourceReviewStatus(any());
        var audit=ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class); verify(sources).insertAuditLog(audit.capture());
        assertThat(audit.getValue().metadataJson()).contains("noteHash").doesNotContain("공개 원문 전체");
    }
    @Test void sameKeyAndRequestReturnsFirstConfirmationWithoutNewWritesEvenAfterVersionChanges() {
        ready(true,3); confirmationPersistence(); var request=request(3,"EXTRACTED_TEXT",List.of());
        var first=service.insertConfirmation(auth("ADMIN"),source,key,request);
        ready(true,4); when(reviews.selectIdempotentConfirmationDetails(key)).thenReturn(saved);
        var repeated=service.insertConfirmation(auth("ADMIN"),source,key,request);
        assertThat(repeated).isEqualTo(first); verify(reviews,times(1)).insertConfirmation(any());
    }
    @Test void keyReuseWithChangedNoteCannotReuseConfirmation() {
        ready(true,3); confirmationPersistence(); var request=request(3,"EXTRACTED_TEXT",List.of());
        service.insertConfirmation(auth("ADMIN"),source,key,request); when(reviews.selectIdempotentConfirmationDetails(key)).thenReturn(saved);
        var different=new AttachmentReviewRequests.Confirmation(request.version(),request.targetCategoryCodes(),request.supportTypeCodes(),request.reviewMethodCode(),List.of(),"다른 사유");
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,different),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(reviews,times(1)).insertConfirmation(any());
    }
    @Test void confirmationIdempotencyKeyIsBoundToActorNotOnlyPayload() {
        ready(true,3); confirmationPersistence(); var request=request(3,"EXTRACTED_TEXT",List.of());
        service.insertConfirmation(auth("ADMIN"),source,key,request); when(reviews.selectIdempotentConfirmationDetails(key)).thenReturn(saved);
        var other=new AuthenticatedUserDetails(new AuthUserDetailsRow(UUID.randomUUID(),"other-qa","unused","다른 검수 QA","ACTIVE",false,null,null,null),List.of("ADMIN"));
        var otherAuth=UsernamePasswordAuthenticationToken.authenticated(other,null,other.getAuthorities());
        expectError(()->service.insertConfirmation(otherAuth,source,key,request),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(reviews,times(1)).insertConfirmation(any());
    }
    @Test void lostIdempotentInsertRaisesConflictInsteadOfReturningSuccessAfterVersionWrite() {
        ready(true,3); when(reviews.insertConfirmation(any())).thenReturn(0);
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(reviews,never()).insertConfirmedTarget(any()); verify(sources,never()).insertAuditLog(any());
    }
    @Test void linkedSourceCannotReceiveNewConfirmationOrOverwriteDraftTags() {
        ready(true,3); when(reviews.selectConversionLinkDetails(source)).thenReturn(new AttachmentReviewRows.Link(UUID.randomUUID(),"ANN-QA",confirmation,hash));
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(reviews,never()).updateConfirmationsStale(any()); verifyNoInteractions(announcements);
    }
    @Test void staleRequestAndWrongSetHashCannotWrite() {
        ready(true,4);
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        var request=new AttachmentReviewRequests.Confirmation(new AttachmentReviewRequests.Version(base,evaluation,4,4,"b".repeat(64)),
                List.of("BUSINESS"),List.of("POLICY_FINANCE"),"EXTRACTED_TEXT",List.of(),"원문 확인");
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(reviews,never()).insertConfirmation(any());
    }
    @Test void crossSourceDecisionIs404NotReused() {
        ready(true,3); when(evaluations.selectEvaluationDetails(source,evaluation)).thenReturn(null);
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.RESOURCE_NOT_FOUND);
        verify(reviews,never()).insertConfirmation(any());
    }
    @Test void collectOnlyCannotBeImplicitlyEnforcedByConfirmation() {
        ready(false,3);
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_REVIEW_REQUIRED);
        verify(reviews,never()).updateSourceVersion(any(),anyInt(),anyInt(),any());
    }
    @Test void openSetAndRunningJobBlockConfirmation() {
        ready(true,3); when(evidence.selectSetDetails(source,setId)).thenReturn(set("NO_FILES","OPEN",true));
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        when(reviews.selectActiveNormalJobExists(source)).thenReturn(true);
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY);
        verify(reviews,never()).insertConfirmation(any());
    }
    @Test void failedDiscoveryRequiresManualMethodAndExactAcknowledgements() {
        ready(true,3); confirmationPersistence();
        when(evidence.selectSetDetails(source,setId)).thenReturn(set("FAILED","SEALED",false));
        when(evaluations.selectEvaluationDetails(source,evaluation)).thenReturn(decision("REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE","[]"));
        var codes=List.of("ATTACHMENT_DISCOVERY_INCOMPLETE","ATTACHMENT_INCOMPLETE");
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",codes)),ErrorCode.ANNOUNCEMENT_ATTACHMENT_REVIEW_REQUIRED);
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"MANUAL_SOURCE_CHECK",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_REVIEW_REQUIRED);
        var response=service.insertConfirmation(auth("ADMIN"),source,key,request(3,"MANUAL_SOURCE_CHECK",codes));
        assertThat(response.reviewMethodCode()).isEqualTo("MANUAL_SOURCE_CHECK"); verify(reviews,times(1)).insertConfirmation(any());
        verify(evaluations,never()).updateEvaluationCurrent(any(),any());
    }
    @Test void reviewContextReturnsExactVersionAndDoesNotLockOrMutate() {
        ready(true,3); var response=service.selectReviewContextDetails(source);
        assertThat(response.version()).isEqualTo(version(3)); assertThat(response.manualSourceCheckRequired()).isFalse();
        verify(jobs,never()).selectSourceContextDetailsForUpdate(any()); verify(reviews,never()).insertConfirmation(any());
    }
    @Test void reviewContextRestoresOnlyVersionBoundConfirmedTagsWithoutNoteOrWrites() {
        var row=ready(true,4); when(row.confirmationId()).thenReturn(confirmation);
        when(reviews.selectConfirmationDetails(source,confirmation)).thenReturn(confirmed(true,4));
        when(reviews.selectConfirmedTargetCodeList(confirmation)).thenReturn(List.of("PERSONAL","BUSINESS"));
        when(reviews.selectConfirmedSupportCodeList(confirmation)).thenReturn(List.of("POLICY_FINANCE"));
        var response=service.selectReviewContextDetails(source);
        assertThat(response.confirmedClassification().confirmation().confirmationId()).isEqualTo(confirmation);
        assertThat(response.confirmedClassification().targetCategoryCodes()).containsExactly("BUSINESS","PERSONAL");
        assertThat(response.confirmedClassification().supportTypeCodes()).containsExactly("POLICY_FINANCE");
        assertThat(response.linkedAnnouncement()).isNull(); verify(reviews,never()).updateConfirmationsStale(any());
    }
    @ParameterizedTest @ValueSource(strings={"stale","sourceVersion","attachmentVersion","evaluation","set","source","id"})
    void reviewContextDoesNotRestoreStaleOrForeignConfirmation(String mismatch) {
        var row=ready(true,4); when(row.confirmationId()).thenReturn(confirmation);
        var saved=new AttachmentReviewRows.Confirmation(mismatch.equals("id")?UUID.randomUUID():confirmation,
                mismatch.equals("source")?UUID.randomUUID():source,mismatch.equals("evaluation")?UUID.randomUUID():evaluation,
                mismatch.equals("set")?"b".repeat(64):hash,hash,"EXTRACTED_TEXT",!mismatch.equals("stale"),
                mismatch.equals("sourceVersion")?3:4,mismatch.equals("attachmentVersion")?3:4,OffsetDateTime.now());
        when(reviews.selectConfirmationDetails(source,confirmation)).thenReturn(saved);
        assertThat(service.selectReviewContextDetails(source).confirmedClassification()).isNull();
        verify(reviews,never()).selectConfirmedTargetCodeList(any()); verify(reviews,never()).selectConfirmedSupportCodeList(any());
    }
    @Test void restoredConfirmationUsesNewBindingButPreservesOriginalReviewReceipt() {
        var row=ready(true,6);when(row.confirmationId()).thenReturn(confirmation);var original=confirmed(true,4);UUID restoration=UUID.randomUUID();
        when(reviews.selectConfirmationDetails(source,confirmation)).thenReturn(original);
        when(reviews.selectRestoredBindingDetails(source,confirmation)).thenReturn(new AttachmentReviewRows.RestoredBinding(restoration,confirmation,source,4,6));
        when(reviews.selectConfirmedTargetCodeList(confirmation)).thenReturn(List.of("BUSINESS"));
        when(reviews.selectConfirmedSupportCodeList(confirmation)).thenReturn(List.of("POLICY_FINANCE"));
        var result=service.selectReviewContextDetails(source).confirmedClassification();
        assertThat(result.confirmation().attachmentVersion()).isEqualTo(4);
        assertThat(result.confirmation().confirmedAt()).isEqualTo(original.confirmedAt());
        assertThat(result.binding().restorationId()).isEqualTo(restoration);assertThat(result.binding().attachmentVersion()).isEqualTo(6);
        verify(reviews,never()).updateConfirmationsStale(any());verifyNoInteractions(announcements);
    }
    @ParameterizedTest @ValueSource(strings={"source","confirmation","restoration","base","equalVersion","futureVersion","nullVersion"})
    void malformedOrStaleRestorationDoesNotReopenReviewOrDraft(String mismatch) {
        draftReady();var row=ready(true,6);when(row.confirmationId()).thenReturn(confirmation);
        when(reviews.selectRestoredBindingDetails(source,confirmation)).thenReturn(new AttachmentReviewRows.RestoredBinding(
                mismatch.equals("restoration")?null:UUID.randomUUID(),mismatch.equals("confirmation")?UUID.randomUUID():confirmation,
                mismatch.equals("source")?UUID.randomUUID():source,mismatch.equals("base")?3:4,
                mismatch.equals("nullVersion")?null:mismatch.equals("equalVersion")?4:mismatch.equals("futureVersion")?7:6));
        assertThat(service.selectReviewContextDetails(source).confirmedClassification()).isNull();
        var request=new AttachmentReviewRequests.Conversion(version(6),confirmation,"BUSINESS",null);
        expectError(()->service.insertOperationalAnnouncement(auth("ADMIN"),source,request),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(announcements,never()).insertAnnouncement(any());
    }
    @Test void restoredConfirmationCanCreateOnlyDraftUsingCurrentRequestVersion() {
        draftReady();ready(true,6);
        when(reviews.selectRestoredBindingDetails(source,confirmation)).thenReturn(new AttachmentReviewRows.RestoredBinding(UUID.randomUUID(),confirmation,source,4,6));
        service.insertOperationalAnnouncement(auth("ADMIN"),source,new AttachmentReviewRequests.Conversion(version(6),confirmation,"BUSINESS",null));
        verify(reviews).updateSourceVersion(source,4,6,evaluation);verify(announcements,times(1)).insertAnnouncement(any());
        verify(reviews,never()).insertConfirmation(any());
    }
    @Test void originalVersionDoesNotBypassACompletedButNowStaleRestoration() {
        draftReady();when(reviews.selectRestoredBindingDetails(source,confirmation)).thenReturn(new AttachmentReviewRows.RestoredBinding(UUID.randomUUID(),confirmation,source,4,6));
        expectError(()->service.insertOperationalAnnouncement(auth("ADMIN"),source,conversion()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(announcements,never()).insertAnnouncement(any());
    }
    @Test void linkedSourceContextOnlyReturnsLinkNotFreshConversionPermission() {
        var row=ready(true,5); when(row.confirmationId()).thenReturn(confirmation);
        var announcement=UUID.randomUUID();
        when(reviews.selectConversionLinkDetails(source)).thenReturn(new AttachmentReviewRows.Link(announcement,"ANN-QA",confirmation,hash));
        var response=service.selectReviewContextDetails(source);
        assertThat(response.linkedAnnouncement().announcementId()).isEqualTo(announcement);
        assertThat(response.confirmedClassification()).isNull(); verify(reviews,never()).selectConfirmationDetails(any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"APPROVER","USER","PARTNER","REVIEWER"})
    void readOnlyOrExternalRoleCannotWriteEvenThroughDirectServiceCall(String role) {
        expectError(()->service.insertConfirmation(auth(role),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN);
        expectError(()->service.insertOperationalAnnouncement(auth(role),source,conversion()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN);
        verifyNoInteractions(jobs,reviews,announcements);
    }
    @Test void disabledCatalogAndLostVersionCasFailBeforeConfirmationInsert() {
        ready(true,3); when(reviews.selectEnabledTargetCodeList(any())).thenReturn(List.of("BUSINESS"));
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        when(reviews.selectEnabledTargetCodeList(any())).thenAnswer(i->i.getArgument(0)); when(reviews.updateSourceVersion(source,4,3,evaluation)).thenReturn(0);
        expectError(()->service.insertConfirmation(auth("ADMIN"),source,key,request(3,"EXTRACTED_TEXT",List.of())),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(reviews,never()).insertConfirmation(any());
    }
    @Test void sameDraftRequestReturnsOneLinkWithoutCreatingSecondAnnouncement() {
        draftReady(); var first=service.insertOperationalAnnouncement(auth("ADMIN"),source,conversion());
        when(reviews.selectConversionLinkDetails(source)).thenReturn(savedLink);
        var repeated=service.insertOperationalAnnouncement(auth("OPERATOR"),source,conversion());
        assertThat(repeated).isEqualTo(first); verify(announcements,times(1)).insertAnnouncement(any());
        verify(announcements,times(2)).insertAnnouncementTargetCategoryAssignment(any());
        verify(reviews,times(1)).insertConversionLink(any()); verify(reviews,never()).updateConfirmationsStale(any());
    }
    @Test void differentOrLegacyLinkNeverReturnsFakeConversionSuccess() {
        draftReady(); when(reviews.selectConversionLinkDetails(source)).thenReturn(new AttachmentReviewRows.Link(UUID.randomUUID(),"ANN-OLD",null,null));
        expectError(()->service.insertOperationalAnnouncement(auth("ADMIN"),source,conversion()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(announcements,never()).insertAnnouncement(any());
    }
    @Test void staleConfirmationCannotCreateDraft() {
        draftReady(); when(reviews.selectConfirmationDetails(source,confirmation)).thenReturn(confirmed(false,4));
        expectError(()->service.insertOperationalAnnouncement(auth("ADMIN"),source,conversion()),ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);
        verify(announcements,never()).insertAnnouncement(any());
    }
    @Test void duplicateCandidatesAndUnconfirmedPrimaryCannotCreateDraft() {
        draftReady(); when(sources.selectPendingDuplicateCandidateCount(source)).thenReturn(1L);
        expectError(()->service.insertOperationalAnnouncement(auth("ADMIN"),source,conversion()),ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE);
        var different=new AttachmentReviewRequests.Conversion(version(4),confirmation,"PARENT",null);
        expectError(()->service.insertOperationalAnnouncement(auth("ADMIN"),source,different),ErrorCode.VALIDATION_FAILED);
        verify(announcements,never()).insertAnnouncement(any());
    }
    @Test void unexpectedNonDraftPersistenceAbortsBeforeLinkAndTags() {
        draftReady(); var unexpected=mock(AnnouncementDetailsRow.class); when(unexpected.approvalStatusCode()).thenReturn("APPROVED");
        when(announcements.selectAnnouncementDetails(any())).thenReturn(unexpected);
        expectError(()->service.insertOperationalAnnouncement(auth("ADMIN"),source,conversion()),ErrorCode.INTERNAL_ERROR);
        verify(reviews,never()).insertConversionLink(any()); verify(announcements,never()).insertAnnouncementTargetCategoryAssignment(any());
    }
}
