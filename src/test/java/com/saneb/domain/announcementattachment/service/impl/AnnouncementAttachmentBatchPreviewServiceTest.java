package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentBatchPreviewServiceTest {
    private final AnnouncementAttachmentBatchPreviewDao dao=mock(AnnouncementAttachmentBatchPreviewDao.class);
    private final AnnouncementAttachmentBatchDao batches=mock(AnnouncementAttachmentBatchDao.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final UUID batchId=UUID.randomUUID(),actor=UUID.randomUUID(),policyId=UUID.randomUUID(),ruleId=UUID.randomUUID(),job1=UUID.randomUUID(),job2=UUID.randomUUID();
    private final UUID source1=UUID.randomUUID(),source2=UUID.randomUUID();
    private final List<AttachmentBatchPreviewRows.LiveItem> live=new ArrayList<>();
    private final Map<UUID,AttachmentBatchPreviewRows.Preview> previews=new HashMap<>();
    private final Map<UUID,List<AttachmentBatchPreviewRows.Item>> items=new HashMap<>();
    private final Map<UUID,Boolean> jobSelections=new HashMap<>();
    private AttachmentBatchRows.Row batch;
    private AttachmentPolicyRow policy;
    private UUID current;
    private AnnouncementAttachmentBatchPreviewServiceImpl service;
    @BeforeEach void setup() throws Exception {
        policy=new AttachmentPolicyRow(policyId,"ACTIVE","COLLECT_ONLY",ruleId,"ACTIVE","a".repeat(64),"{}","[]",0);
        batch=new AttachmentBatchRows.Row(batchId,policyId,"COLLECTION_PARTIAL_FAILED","b".repeat(64),100,3,1,5,"{}",mapper.writeValueAsString(policy),actor,UUID.randomUUID(),"c".repeat(64),OffsetDateTime.now());
        String evidence=mapper.writeValueAsString(Map.of("baseStatus","ACCEPTED","proposedStatus","REVIEW_REQUIRED","unknownRoleCount",1,
                "baseTargetCodes",List.of("BUSINESS"),"previousTargetCodes",List.of("SPOUSE"),"proposedTargetCodes",List.of("BUSINESS","SELF"),
                "baseSupportCodes",List.of("CASH"),"proposedSupportCodes",List.of("CASH"),"confirmedTargetCodes",List.of("PARENT"),"files",List.of(Map.of("role","UNKNOWN","quality","COMPLETE_TEXT"))));
        live.add(new AttachmentBatchPreviewRows.LiveItem(job1,source1,"BIZINFO","SUCCEEDED",null,true,true,false,false,"{\"version\":0}",evidence));
        live.add(new AttachmentBatchPreviewRows.LiveItem(job2,source2,"BIZINFO","FAILED","ISOLATION_UNAVAILABLE",false,true,false,false,"{\"version\":0}","{\"files\":[],\"fileCount\":0}"));
        when(batches.selectBatchDetails(eq(batchId),anyBoolean())).thenAnswer(c->batch);
        when(batches.selectPolicyDetails(eq(policyId),anyBoolean())).thenAnswer(c->policy);
        when(batches.selectItemList(any())).thenAnswer(c->live.stream().map(i->new AttachmentBatchRows.Item(i.jobId(),i.sourceId(),i.providerCode(),i.jobStatusCode(),0,0,i.jobErrorCode(),"NOT_REQUESTED","NOT_REQUESTED")).toList());
        when(batches.selectSourceLocks(anyList())).thenAnswer(c->c.getArgument(0));
        when(dao.selectLiveItemList(batchId)).thenAnswer(c->List.copyOf(live));
        when(dao.selectRequestDetails(any())).thenAnswer(c->previews.values().stream().filter(p->p.idempotencyKey().equals(c.getArgument(0))).findFirst().orElse(null));
        when(dao.selectPreviewDetails(eq(batchId),any())).thenAnswer(c->previews.get(c.getArgument(1)));
        when(dao.selectCurrentPreviewDetails(batchId)).thenAnswer(c->previews.get(current));
        when(dao.updatePreviewRunning(eq(batchId),anyInt())).thenAnswer(c->{saveBatch("PREVIEW_RUNNING",batch.rowVersion()+1,batch.deletedItemCount());return 1;});
        when(dao.insertPreview(any())).thenAnswer(c->{var p=(AttachmentBatchPreviewRows.Preview)c.getArgument(0);previews.put(p.previewId(),p);items.put(p.previewId(),new ArrayList<>());return 1;});
        when(dao.insertItem(any())).thenAnswer(c->{var i=(AttachmentBatchPreviewRows.ItemInsert)c.getArgument(0);var original=live.stream().filter(l->l.jobId().equals(i.jobId())).findFirst().orElseThrow();
            items.get(i.previewId()).add(new AttachmentBatchPreviewRows.Item(i.jobId(),original.sourceId(),original.providerCode(),i.readinessCode(),i.eligible(),i.selected(),i.inputHash(),i.evidenceJson()));return 1;});
        when(dao.updateJobSelection(eq(batchId),anyList())).thenAnswer(c->{List<UUID> chosen=c.getArgument(1);live.forEach(l->jobSelections.put(l.jobId(),chosen.contains(l.jobId())));return live.size();});
        when(dao.updatePreviewReady(eq(batchId),anyInt(),anyString(),anyString(),any())).thenAnswer(c->{current=c.getArgument(4);saveBatch(c.getArgument(2),batch.rowVersion()+1,batch.deletedItemCount());return 1;});
        when(dao.selectItemCount(any())).thenAnswer(c->(long)items.get(c.getArgument(0)).size());
        when(dao.selectItemList(any())).thenAnswer(c->{var search=(AttachmentBatchPreviewRows.Search)c.getArgument(0);return items.get(search.previewId()).stream().skip(search.offset()).limit(search.size()).toList();});
        service=new AnnouncementAttachmentBatchPreviewServiceImpl(dao,batches,audit,mapper);
    }
    private Authentication auth(String role) {var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(actor,"fixture","unused","QA","ACTIVE",false,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    private AttachmentBatchPreviewRequests.Preparation preparation() {return new AttachmentBatchPreviewRequests.Preparation(batch.rowVersion(),batch.scopeHash(),"봉인 근거 미리보기");}
    private AttachmentBatchPreviewResponses.Preview prepare() {return service.insertPreview(auth("ADMIN"),batchId,UUID.randomUUID(),preparation());}
    private AttachmentBatchPreviewRequests.Selection selection(List<UUID> chosen) {return new AttachmentBatchPreviewRequests.Selection(batch.rowVersion(),previews.get(current).previewHash(),chosen,"명시적 선택");}
    private void saveBatch(String state,int version,int deleted) {batch=new AttachmentBatchRows.Row(batchId,policyId,state,batch.scopeHash(),100,3,deleted,version,"{}",batch.policySnapshotJson(),actor,batch.idempotencyKey(),batch.requestHash(),batch.createdAt());}
    private void changeSource() {var first=live.getFirst();live.set(0,new AttachmentBatchPreviewRows.LiveItem(first.jobId(),first.sourceId(),first.providerCode(),first.jobStatusCode(),first.jobErrorCode(),true,false,false,false,"{\"version\":1}",first.evidenceJson()));}
    @Test void previewKeepsWholeScopeAndFailureReasonsWithoutDefaultSelectionOrHttp() {
        var result=prepare();assertThat(result.statusCode()).isEqualTo("PREVIEW_PARTIAL_FAILED");assertThat(result.itemCount()).isEqualTo(3);
        assertThat(result.snapshotDeletedItemCount()).isEqualTo(1);assertThat(result.availableItemCount()).isEqualTo(2);
        assertThat(result.eligibleItemCount()).isEqualTo(1);assertThat(result.selectedItemCount()).isZero();assertThat(result.inputsCurrent()).isTrue();assertThat(result.currentHttpRequests()).isZero();
        var rows=service.selectItemList(auth("APPROVER"),batchId,result.previewId(),1,20).items();
        assertThat(rows).filteredOn(r->r.jobId().equals(job1)).singleElement().satisfies(r->{assertThat(r.eligible()).isTrue();assertThat(r.selected()).isFalse();
            assertThat(r.evidence()).containsEntry("proposedStatus","REVIEW_REQUIRED").containsEntry("baseTargetAdded",List.of("SELF")).containsEntry("previousTargetRemoved",List.of("SPOUSE"))
                .containsEntry("confirmedTargetCodes",List.of("PARENT")).containsEntry("confirmedTargetRemoved",List.of("PARENT"));});
        assertThat(rows).filteredOn(r->r.jobId().equals(job2)).singleElement().satisfies(r->{assertThat(r.readinessCode()).isEqualTo("COLLECTION_NOT_SUCCESSFUL");assertThat(r.evidence()).containsEntry("jobErrorCode","ISOLATION_UNAVAILABLE");});
        verify(batches,never()).selectScopeCounts(any());verify(batches,never()).selectCandidateList(any());verify(batches,never()).updateCollectionStart(any(),anyInt(),any(),anyString());
        assertThat(jobSelections).containsEntry(job1,false).containsEntry(job2,false);
    }
    @Test void selectionWritesNewHistoryAndHashWhilePreservingOldSnapshotAndInput() {
        var original=prepare();var chosen=service.updateSelection(auth("ADMIN"),batchId,UUID.randomUUID(),selection(List.of(job1)));
        assertThat(chosen.previewId()).isNotEqualTo(original.previewId());assertThat(chosen.previewHash()).isNotEqualTo(original.previewHash());
        assertThat(chosen.inputHash()).isEqualTo(original.inputHash());assertThat(chosen.selectedItemCount()).isEqualTo(1);assertThat(chosen.inputsCurrent()).isTrue();
        assertThat(chosen.currentBatchVersion()).isEqualTo(original.currentBatchVersion()+2);
        assertThat(service.selectPreviewDetails(auth("OPERATOR"),batchId,original.previewId()).currentPreview()).isFalse();
        assertThat(items.get(original.previewId())).allMatch(i->!i.selected());assertThat(jobSelections).containsEntry(job1,true).containsEntry(job2,false);
        var cleared=service.updateSelection(auth("ADMIN"),batchId,UUID.randomUUID(),selection(List.of()));assertThat(cleared.selectedItemCount()).isZero();
        assertThat(previews).hasSize(3);assertThat(items.get(chosen.previewId())).anyMatch(AttachmentBatchPreviewRows.Item::selected);
    }
    @Test void sameKeyReturnsOriginalHistoryWithoutRepeatingSelectionOrRevertingCurrent() {
        UUID key=UUID.randomUUID();var request=preparation();var original=service.insertPreview(auth("ADMIN"),batchId,key,request);
        var chosen=service.updateSelection(auth("ADMIN"),batchId,UUID.randomUUID(),selection(List.of(job1)));clearInvocations(dao,batches,audit);
        var repeated=service.insertPreview(auth("ADMIN"),batchId,key,request);
        assertThat(repeated.previewId()).isEqualTo(original.previewId());assertThat(repeated.currentPreview()).isFalse();assertThat(current).isEqualTo(chosen.previewId());
        verify(dao,never()).updateJobSelection(any(),any());verify(dao,never()).insertPreview(any());
        assertThatThrownBy(()->service.insertPreview(auth("ADMIN"),batchId,key,new AttachmentBatchPreviewRequests.Preparation(request.expectedVersion(),request.expectedScopeHash(),"다른 사유"))).hasMessageContaining("멱등 키");
    }
    @Test void selectionRejectsFailedForeignAndDuplicateIdsWithoutPartialWrites() {
        prepare();clearInvocations(dao);
        for(var ids:List.of(List.of(job2),List.of(UUID.randomUUID()),List.of(job1,job1)))
            assertThatThrownBy(()->service.updateSelection(auth("ADMIN"),batchId,UUID.randomUUID(),selection(ids))).isInstanceOf(ApiException.class);
        verify(dao,never()).updatePreviewRunning(any(),anyInt());verify(dao,never()).updateJobSelection(any(),any());
    }
    @Test void sourceChangeMarksReadStaleAndSelectionRequiresExplicitNewPreview() {
        var original=prepare();var oldSelection=selection(List.of(job1));changeSource();clearInvocations(dao);
        assertThat(service.selectCurrentPreviewDetails(auth("ADMIN"),batchId).inputsCurrent()).isFalse();
        assertThatThrownBy(()->service.updateSelection(auth("ADMIN"),batchId,UUID.randomUUID(),oldSelection)).hasMessageContaining("입력이 바뀌었습니다");
        verify(dao,never()).updatePreviewRunning(any(),anyInt());
        var changed=prepare();assertThat(changed.inputHash()).isNotEqualTo(original.inputHash());assertThat(changed.eligibleItemCount()).isZero();
        assertThat(changed.selectedItemCount()).isZero();assertThat(items.get(changed.previewId())).anyMatch(i->"SOURCE_CHANGED".equals(i.readinessCode()));
    }
    @Test void deletionDoesNotReconstructItemIdentityOrHideSnapshotCounts() {
        var preview=prepare();live.removeIf(i->i.jobId().equals(job2));items.values().forEach(list->list.removeIf(i->i.jobId().equals(job2)));saveBatch(batch.statusCode(),batch.rowVersion()+1,2);
        var history=service.selectPreviewDetails(auth("ADMIN"),batchId,preview.previewId());
        assertThat(history.snapshotRemainingItemCount()).isEqualTo(2);assertThat(history.availableItemCount()).isEqualTo(1);
        assertThat(history.snapshotDeletedItemCount()).isEqualTo(1);assertThat(history.currentDeletedItemCount()).isEqualTo(2);assertThat(history.inputsCurrent()).isFalse();
        assertThat(service.selectItemList(auth("ADMIN"),batchId,preview.previewId(),1,20).items()).noneMatch(i->i.sourceId().equals(source2));
    }
    @Test void incompleteCollectionAndPolicyChangesBlockPreparation() {
        saveBatch("COLLECTING",batch.rowVersion(),1);assertThatThrownBy(this::prepare).hasMessageContaining("배치 단계");
        saveBatch("COLLECTION_PARTIAL_FAILED",batch.rowVersion(),1);var old=policy;policy=new AttachmentPolicyRow(policyId,"RETIRED","COLLECT_ONLY",ruleId,"ACTIVE",old.policyHash(),old.settingsJson(),old.profileManifestJson(),1);
        assertThatThrownBy(this::prepare).hasMessageContaining("ACTIVE 정책");verify(dao,never()).insertPreview(any());
    }
    @Test void noItemsAfterDeletionStillCreatesPartialPreviewWithZeroEligibleSelection() {
        live.clear();saveBatch("COLLECTION_PARTIAL_FAILED",batch.rowVersion()+1,3);var preview=prepare();
        assertThat(preview.statusCode()).isEqualTo("PREVIEW_PARTIAL_FAILED");assertThat(preview.availableItemCount()).isZero();assertThat(preview.selectedItemCount()).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void writesRequireAdminAtServiceBoundary(String role) {
        assertThatThrownBy(()->service.insertPreview(auth(role),batchId,UUID.randomUUID(),preparation())).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.updateSelection(auth(role),batchId,UUID.randomUUID(),new AttachmentBatchPreviewRequests.Selection(5,"a".repeat(64),List.of(),"사유"))).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,batches,audit);
    }
    @Test void invalidHeaderScopeAndPageCannotBeAccepted() {
        assertThatThrownBy(()->service.insertPreview(auth("ADMIN"),batchId,null,preparation())).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertPreview(auth("ADMIN"),batchId,UUID.randomUUID(),new AttachmentBatchPreviewRequests.Preparation(Integer.MAX_VALUE,batch.scopeHash(),"사유"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertPreview(auth("ADMIN"),batchId,UUID.randomUUID(),new AttachmentBatchPreviewRequests.Preparation(5,"a".repeat(64),"사유"))).hasMessageContaining("범위 지문");
        var preview=prepare();assertThatThrownBy(()->service.selectItemList(auth("ADMIN"),batchId,preview.previewId(),1,101)).hasMessageContaining("1~100");
    }
    @Test void successfulJobWithoutCompleteSealedEvidenceCannotBeSelected() {
        var first=live.getFirst();live.set(0,new AttachmentBatchPreviewRows.LiveItem(first.jobId(),first.sourceId(),first.providerCode(),"SUCCEEDED",null,false,true,false,false,first.inputJson(),"{\"setId\":null,\"evaluationId\":null,\"files\":[]}"));
        var preview=prepare();assertThat(preview.eligibleItemCount()).isZero();
        assertThat(items.get(preview.previewId())).anyMatch(i->"EVIDENCE_INCOMPLETE".equals(i.readinessCode()));
        assertThatThrownBy(()->service.updateSelection(auth("ADMIN"),batchId,UUID.randomUUID(),selection(List.of(job1)))).isInstanceOf(ApiException.class);
    }
    @Test void auditDoesNotContainRawReasonSourceOrEvidence() {
        prepare();var captor=org.mockito.ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);verify(audit).insertAuditLog(captor.capture());
        assertThat(captor.getValue().metadataJson()).contains("previewHash","selectedCount","reasonHash").doesNotContain("봉인 근거 미리보기",source1.toString(),"proposedStatus","files");
    }
}
