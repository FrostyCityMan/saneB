package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentIntakeDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentIntakeServiceImpl;
import com.saneb.domain.announcementattachment.vo.AttachmentCollectionPlan;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyRow;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

class AnnouncementAttachmentIntakeServiceTest {
    private final AnnouncementAttachmentIntakeDao dao=mock(AnnouncementAttachmentIntakeDao.class);
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentJobService jobService=mock(AnnouncementAttachmentJobService.class);
    private final AttachmentDiscoveryProfileRegistry profiles=mock(AttachmentDiscoveryProfileRegistry.class);
    private final AnnouncementAttachmentIntakeService service=new AnnouncementAttachmentIntakeServiceImpl(dao,jobs,jobService,profiles,new ObjectMapper(),true);
    private final UUID source=UUID.randomUUID(),run=UUID.randomUUID(),policy=UUID.randomUUID(),release=UUID.randomUUID();
    private final AttachmentCollectionPlan plan=new AttachmentCollectionPlan(run,release,policy,"FROZEN");

    private void prepareSource() {
        when(dao.selectCollectionPlanDetails(run)).thenReturn(plan);
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION",
                "REVIEW_REQUIRED",UUID.randomUUID(),UUID.randomUUID(),release,"COMBINATION_MATCHED",0,2,false,null,UUID.randomUUID()));
    }
    @Test void recentCheckPreservesCurrentEvidenceAndDoesNotSelectProfileOrReserveAgain() {
        prepareSource();
        when(dao.selectRecentCheckExists(source)).thenReturn(true);
        service.saveCollectedSource(source,plan,false);
        verify(dao).updateSourceIntakeStatus(source,"RECHECK_NOT_DUE");
        verify(jobs,never()).updateAttachmentSourceVersion(any(),org.mockito.ArgumentMatchers.anyInt());
        verify(dao,never()).selectSourceLocatorDetails(any());
        verifyNoInteractions(jobService,profiles);
    }
    @Test void activeJobIsReusedBeforeTheRecheckCooldown() {
        prepareSource();
        when(dao.selectActiveJobId(source)).thenReturn(UUID.randomUUID());
        service.saveCollectedSource(source,plan,false);
        verify(dao).updateSourceIntakeStatus(source,"QUEUED");
        verify(dao,never()).selectRecentCheckExists(any());
        verifyNoInteractions(jobService,profiles);
    }
    @Test void offPlanDoesNotQueryOrReserveAnySource() {
        service.saveCollectedSource(source,new AttachmentCollectionPlan(run,release,policy,"OFF"),false);
        verifyNoInteractions(dao,jobs,jobService,profiles);
    }
    @Test void unmatchedActiveEnforceRejectsBeforeAnyPlanOrSourceWrite() {
        when(dao.selectUnmatchedEnforcePolicyId(release)).thenReturn(policy);
        assertMismatch(() -> service.saveCollectionPlan(run,release));
        verify(dao,never()).insertCollectionPlan(any());
        verifyNoInteractions(jobs,jobService,profiles);
    }
    @Test void missingBaseReleaseCannotBypassUnmatchedEnforce() {
        when(dao.selectUnmatchedEnforcePolicyId(null)).thenReturn(policy);
        assertMismatch(() -> service.saveCollectionPlan(run,null));
        verify(dao,never()).selectCollectionPlanDetails(any());
        verify(dao,never()).insertCollectionPlan(any());
        verifyNoInteractions(jobs,jobService,profiles);
    }
    @Test void noPolicyRunCannotContinueThroughAnUnmatchedEnforcePublishedLater() {
        when(dao.selectCollectionPlanDetails(run)).thenReturn(new AttachmentCollectionPlan(run,release,null,"NO_POLICY"));
        when(dao.selectUnmatchedEnforcePolicyId(release)).thenReturn(policy);
        assertMismatch(() -> service.saveCollectionPlan(run,release));
        verify(dao,never()).selectActivePolicyId(any());
        verify(dao,never()).insertCollectionPlan(any());
        verifyNoInteractions(jobs,jobService,profiles);
    }
    @ParameterizedTest @ValueSource(strings={"COLLECT_ONLY","ENFORCE","OFF"})
    void matchingPublishedPolicyDeterminesNewPlanWithoutAdoptingOlderPolicy(String mode) {
        var expected=new AttachmentCollectionPlan(run,release,policy,"OFF".equals(mode)?"OFF":"FROZEN");
        when(dao.selectCollectionPlanDetails(run)).thenReturn(null,expected);
        when(dao.selectActivePolicyId(release)).thenReturn(policy);
        when(jobs.selectPolicyDetails(policy)).thenReturn(new AttachmentPolicyRow(policy,"ACTIVE",mode,release,"ACTIVE",
                "a".repeat(64),"{}","[]",0));
        assertThat(service.saveCollectionPlan(run,release)).isEqualTo(expected);
        verify(dao).insertCollectionPlan(expected);
        verify(dao,never()).selectUnmatchedEnforcePolicyId(any());
        verifyNoInteractions(jobService,profiles);
    }
    @ParameterizedTest @ValueSource(strings={"FROZEN","OFF"})
    void existingFrozenModeCannotBeChangedDuringTheRun(String status) {
        var expected=new AttachmentCollectionPlan(run,release,policy,status);
        when(dao.selectCollectionPlanDetails(run)).thenReturn(expected);
        assertThat(service.saveCollectionPlan(run,release)).isSameAs(expected);
        verify(dao,never()).selectActivePolicyId(any());
        verify(dao,never()).selectUnmatchedEnforcePolicyId(any());
        verify(dao,never()).insertCollectionPlan(any());
        verifyNoInteractions(jobs,jobService,profiles);
    }
    @Test void initialInstallationWithoutAttachmentPolicyKeepsNoPolicySnapshot() {
        var expected=new AttachmentCollectionPlan(run,release,null,"NO_POLICY");
        when(dao.selectCollectionPlanDetails(run)).thenReturn(null,expected);
        assertThat(service.saveCollectionPlan(run,release)).isEqualTo(expected);
        verify(dao).selectUnmatchedEnforcePolicyId(release);
        verify(dao).insertCollectionPlan(expected);
        verifyNoInteractions(jobs,jobService,profiles);
    }
    @Test void missingBaseReleaseWithoutEnforceKeepsLegacyDisabledBehavior() {
        assertThat(service.saveCollectionPlan(run,null)).isNull();
        verify(dao).selectUnmatchedEnforcePolicyId(null);
        verify(dao,never()).insertCollectionPlan(any());
        verifyNoInteractions(jobs,jobService,profiles);
    }
    @Test void disabledWorkerDoesNotChangeCollectionOrPolicy() {
        var disabled=new AnnouncementAttachmentIntakeServiceImpl(dao,jobs,jobService,profiles,new ObjectMapper(),false);
        assertThat(disabled.saveCollectionPlan(run,release)).isNull();
        disabled.saveCollectedSource(source,plan,true);
        verifyNoInteractions(dao,jobs,jobService,profiles);
    }
    @Test void titleWithoutCompletedClassificationNeverReachesProfileOrJob() {
        when(dao.selectCollectionPlanDetails(run)).thenReturn(plan);
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION",
                "REVIEW_REQUIRED",UUID.randomUUID(),UUID.randomUUID(),release,null,0,0,false,null,null));
        service.saveCollectedSource(source,plan,true);
        verify(dao,never()).updateSourceIntakeStatus(any(),any());
        verifyNoInteractions(jobService,profiles);
    }
    private void assertMismatch(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ApiException.class,exception -> {
            assertThat(exception.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_ATTACHMENT_RULE_POLICY_MISMATCH);
            assertThat(exception.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getMessage()).contains("현재 키워드 규칙", "검증·게시", "새 수집");
        });
    }
}
