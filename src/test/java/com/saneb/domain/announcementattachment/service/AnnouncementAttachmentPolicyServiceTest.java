package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentPolicyServiceImpl;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentPolicyServiceTest {
    @Test void systemProfileUsesActualGov24ProviderCodeNotBatchFilterAlias() {
        when(profile.selectProviderCode()).thenReturn("GOV24_PUBLIC_SERVICE");
        var result=create("COLLECT_ONLY");
        assertThat(result.systemProfileBindings()).singleElement().satisfies(binding->assertThat(binding.providerCode()).isEqualTo("GOV24_PUBLIC_SERVICE"));
        assertThat(result.policy().policyStatusCode()).isEqualTo("DRAFT");
        when(profile.selectProviderCode()).thenReturn("GOV24");
        assertThatThrownBy(()->service.updatePolicyDraft(auth("ADMIN"),result.policy().policyId(),new AttachmentPolicyRequests.Update(0,rule,"COLLECT_ONLY",100L,"별칭 거부")))
                .isInstanceOf(ApiException.class).hasMessageContaining("시스템 첨부 profile");
        verify(dao,never()).updatePolicyDraft(any());
    }
    private final AnnouncementAttachmentPolicyDao dao=mock(AnnouncementAttachmentPolicyDao.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final AttachmentDiscoveryProfileRegistry registry=mock(AttachmentDiscoveryProfileRegistry.class);
    private final AttachmentDiscoveryProfile profile=mock(AttachmentDiscoveryProfile.class);
    private final ObjectMapper mapper=new ObjectMapper();
    private final AnnouncementAttachmentPolicyService service=new AnnouncementAttachmentPolicyServiceImpl(dao,audit,registry,mapper);
    private final UUID actor=UUID.randomUUID(),rule=UUID.randomUUID(),key=UUID.randomUUID();
    private final Map<UUID,AttachmentPolicyManagementRows.Row> rows=new HashMap<>();
    private static final OffsetDateTime NOW=OffsetDateTime.parse("2026-09-11T11:00:00+09:00");
    private Authentication auth(String role) { return auth(actor,role,"ACTIVE",false); }
    private Authentication auth(UUID id,String role,String status,boolean reset) {
        var principal=new AuthenticatedUserDetails(new AuthUserDetailsRow(id,"policy-qa","unused","정책 QA",status,reset,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    @BeforeEach void configure() {
        when(profile.selectProviderCode()).thenReturn("BIZINFO");when(profile.selectProfileCode()).thenReturn("BIZINFO_DETAIL_V1");
        when(profile.selectProfileHash()).thenReturn("b".repeat(64));when(registry.selectProfileList()).thenReturn(List.of(profile));
        when(dao.selectRuleStatus(any())).thenReturn("ACTIVE");
        when(dao.selectPolicyDetails(any(),anyBoolean())).thenAnswer(call -> rows.get(call.getArgument(0)));
        when(dao.selectCreationDetails(any())).thenAnswer(call -> rows.values().stream().filter(row -> call.getArgument(0).equals(row.creationIdempotencyKey())).findFirst().orElse(null));
        when(dao.selectLatestVersion(any())).thenAnswer(call -> rows.values().stream().filter(row -> call.getArgument(0).equals(row.policyCode()))
                .mapToInt(AttachmentPolicyManagementRows.Row::versionNo).max().orElse(0));
        when(dao.insertPolicy(any())).thenAnswer(call -> {
            AttachmentPolicyManagementRows.Insert c=call.getArgument(0);
            rows.put(c.policyId(),new AttachmentPolicyManagementRows.Row(c.policyId(),c.policyCode(),c.versionNo(),0,"DRAFT",c.modeCode(),c.ruleReleaseId(),"ACTIVE",null,
                    c.settingsJson(),c.profileManifestJson(),c.actorId(),NOW,NOW,null,c.copiedFromPolicyId(),c.idempotencyKey(),c.requestHash(),c.operationCode()));return 1;
        });
        when(dao.updatePolicyDraft(any())).thenAnswer(call -> {
            AttachmentPolicyManagementRows.Update c=call.getArgument(0);var row=rows.get(c.policyId());
            rows.put(c.policyId(),new AttachmentPolicyManagementRows.Row(row.policyId(),row.policyCode(),row.versionNo(),row.rowVersion()+1,"DRAFT",c.modeCode(),c.ruleReleaseId(),"ACTIVE",null,
                    c.settingsJson(),c.profileManifestJson(),row.createdBy(),row.createdAt(),NOW,null,row.copiedFromPolicyId(),row.creationIdempotencyKey(),row.creationRequestHash(),row.creationOperationCode()));return 1;
        });
    }
    private AttachmentPolicyRequests.Create request(String mode) { return new AttachmentPolicyRequests.Create(rule,mode,83886080L,"관리 정책 QA"); }
    private AttachmentPolicyResponses.Details create(String mode) { return service.insertPolicy(auth("ADMIN"),key,request(mode)); }
    private void changeStatus(UUID id,String status) {
        var row=rows.get(id);rows.put(id,new AttachmentPolicyManagementRows.Row(row.policyId(),row.policyCode(),row.versionNo(),row.rowVersion(),status,row.modeCode(),row.ruleReleaseId(),
                row.ruleReleaseStatusCode(),"a".repeat(64),row.settingsJson(),row.profileManifestJson(),row.createdBy(),row.createdAt(),row.updatedAt(),NOW,
                row.copiedFromPolicyId(),row.creationIdempotencyKey(),row.creationRequestHash(),row.creationOperationCode()));
    }
    @ParameterizedTest @ValueSource(strings={"OFF","COLLECT_ONLY","ENFORCE"})
    void createAlwaysSavesUnvalidatedDraftAndSystemOwnedBindingsWithoutActivation(String mode) throws Exception {
        var result=create(mode);
        assertThat(result.policy().policyStatusCode()).isEqualTo("DRAFT");assertThat(result.policy().modeCode()).isEqualTo(mode);
        assertThat(result.policy().policyHash()).isNull();assertThat(result.policy().publishedAt()).isNull();
        assertThat(result.configuration().extractorConfigHash()).isNull();assertThat(result.configuration().maximumSourceBytes()).isEqualTo(83886080);
        assertThat(result.configuration().engineVersion()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        assertThat(result.configuration().segmentRuleVersion()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION);
        assertThat(result.configuration().segmentRulesHash()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH);
        assertThat(result.isDraftValidationRequired()).isTrue();assertThat(result.isEditable()).isTrue();
        assertThat(result.systemProfileBindings()).containsExactly(new AttachmentPolicyResponses.Profile("BIZINFO","BIZINFO_DETAIL_V1","b".repeat(64)));
        var saved=rows.get(result.policy().policyId());assertThat(saved.creationOperationCode()).isEqualTo("CREATE");
        assertThat(saved.creationRequestHash()).matches("[0-9a-f]{64}");
        var metadata=org.mockito.ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);verify(audit).insertAuditLog(metadata.capture());
        assertThat(metadata.getValue().metadataJson()).contains("reasonHash").doesNotContain("관리 정책 QA","http", "settingsJson");
    }
    @Test void duplicateCreateReturnsSamePolicyAfterEditingWithoutReapplyingOldInput() {
        var first=create("OFF");
        var modified=service.updatePolicyDraft(auth("ADMIN"),first.policy().policyId(),new AttachmentPolicyRequests.Update(0,rule,"COLLECT_ONLY",100L,"초안 변경"));
        var repeated=create("OFF");
        assertThat(repeated.policy().policyId()).isEqualTo(first.policy().policyId());assertThat(repeated.policy().rowVersion()).isEqualTo(modified.policy().rowVersion());
        assertThat(repeated.policy().modeCode()).isEqualTo("COLLECT_ONLY");verify(dao,times(1)).insertPolicy(any());
    }
    @Test void legacyPolicyEditAndRevisionDoNotSilentlyMigrateEngine() throws Exception {
        var created=create("OFF");var row=rows.get(created.policy().policyId());
        var settings=new AttachmentPolicyResponses.Configuration("attachment-1.0.0",created.configuration().extractorVersion(),null,83886080L,
                created.configuration().roleRuleVersion(),created.configuration().roleRulesHash());
        rows.put(row.policyId(),new AttachmentPolicyManagementRows.Row(row.policyId(),row.policyCode(),row.versionNo(),row.rowVersion(),row.policyStatusCode(),row.modeCode(),row.ruleReleaseId(),
                row.ruleReleaseStatusCode(),row.policyHash(),mapper.writeValueAsString(settings),row.profileManifestJson(),row.createdBy(),row.createdAt(),row.updatedAt(),row.publishedAt(),
                row.copiedFromPolicyId(),row.creationIdempotencyKey(),row.creationRequestHash(),row.creationOperationCode()));
        var updated=service.updatePolicyDraft(auth("ADMIN"),row.policyId(),new AttachmentPolicyRequests.Update(0,rule,"COLLECT_ONLY",100L,"기존 엔진 한도 변경"));
        assertThat(updated.configuration().engineVersion()).isEqualTo("attachment-1.0.0");assertThat(updated.configuration().segmentRuleVersion()).isNull();
        assertThat(rows.get(row.policyId()).settingsJson()).doesNotContain("segmentRule");
        var revision=service.insertPolicyRevision(auth("ADMIN"),row.policyId(),UUID.randomUUID(),new AttachmentPolicyRequests.Revision(1,"기존 정책 복사"));
        assertThat(revision.configuration()).isEqualTo(updated.configuration());assertThat(revision.policy().policyStatusCode()).isEqualTo("DRAFT");
        assertThat(create("OFF").configuration()).isEqualTo(updated.configuration());
    }
    @Test void reusedKeyCannotCrossActorBodyOrOperation() {
        var first=create("OFF");
        assertThatThrownBy(() -> create("ENFORCE")).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        assertThatThrownBy(() -> service.insertPolicy(auth(UUID.randomUUID(),"ADMIN","ACTIVE",false),key,request("OFF"))).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        assertThatThrownBy(() -> service.insertPolicyRevision(auth("ADMIN"),first.policy().policyId(),key,new AttachmentPolicyRequests.Revision(0,"개정")))
                .isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");verify(dao,times(1)).insertPolicy(any());
    }
    @ParameterizedTest @ValueSource(strings={"ACTIVE","RETIRED"})
    void publishedPolicyCannotBeEditedAndRevisionPreservesOldIdentityWithoutPublication(String state) {
        var first=create("ENFORCE");UUID id=first.policy().policyId();changeStatus(id,state);var old=rows.get(id);
        assertThatThrownBy(() -> service.updatePolicyDraft(auth("ADMIN"),id,new AttachmentPolicyRequests.Update(0,rule,"OFF",1L,"수정")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_DRAFT));
        var revision=service.insertPolicyRevision(auth("ADMIN"),id,UUID.randomUUID(),new AttachmentPolicyRequests.Revision(0,"개정"));
        assertThat(revision.policy().policyCode()).isEqualTo(first.policy().policyCode());assertThat(revision.policy().versionNo()).isEqualTo(2);
        assertThat(revision.policy().policyStatusCode()).isEqualTo("DRAFT");assertThat(revision.policy().policyHash()).isNull();
        assertThat(revision.policy().publishedAt()).isNull();assertThat(revision.copiedFromPolicyId()).isEqualTo(id);
        assertThat(rows.get(id)).isSameAs(old);verify(dao,never()).updatePolicyDraft(any());
    }
    @Test void revisionKeyIsIdempotentAndDifferentParentVersionsShareFamilyNumbering() {
        var first=create("OFF");UUID id=first.policy().policyId(),revisionKey=UUID.randomUUID();
        var request=new AttachmentPolicyRequests.Revision(0,"개정");
        var second=service.insertPolicyRevision(auth("ADMIN"),id,revisionKey,request);
        assertThat(service.insertPolicyRevision(auth("ADMIN"),id,revisionKey,request).policy().policyId()).isEqualTo(second.policy().policyId());
        var third=service.insertPolicyRevision(auth("ADMIN"),second.policy().policyId(),UUID.randomUUID(),request);
        var fourth=service.insertPolicyRevision(auth("ADMIN"),id,UUID.randomUUID(),request);
        assertThat(third.policy().versionNo()).isEqualTo(3);assertThat(fourth.policy().versionNo()).isEqualTo(4);
        verify(dao,times(3)).selectFamilyLock(first.policy().policyCode());
    }
    @Test void staleVersionAndLostCasKeepCurrentDraft() {
        var first=create("OFF");UUID id=first.policy().policyId();
        assertThatThrownBy(() -> service.updatePolicyDraft(auth("ADMIN"),id,new AttachmentPolicyRequests.Update(1,rule,"ENFORCE",1L,"수정")))
                .isInstanceOf(ApiException.class).hasMessageContaining("입력을 보존");
        assertThatThrownBy(() -> service.insertPolicyRevision(auth("ADMIN"),id,UUID.randomUUID(),new AttachmentPolicyRequests.Revision(1,"개정")))
                .isInstanceOf(ApiException.class).hasMessageContaining("현재 버전");
        doReturn(0).when(dao).updatePolicyDraft(any());
        assertThatThrownBy(() -> service.updatePolicyDraft(auth("ADMIN"),id,new AttachmentPolicyRequests.Update(0,rule,"ENFORCE",1L,"수정")))
                .isInstanceOf(ApiException.class).hasMessageContaining("다른 작업");assertThat(rows.get(id).rowVersion()).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesReceivePagedSummaryAndDetailsWithoutLocks(String role) {
        var first=create("OFF");clearInvocations(dao,audit);
        when(dao.selectPolicyCount(any())).thenReturn(1L);when(dao.selectPolicyList(any())).thenReturn(List.of(first.policy()));
        var list=service.selectPolicyList(auth(role)," DRAFT ",rule,2,20);
        assertThat(list.totalCount()).isEqualTo(1);assertThat(list.items()).containsExactly(first.policy());
        var details=service.selectPolicyDetails(auth(role),first.policy().policyId());
        assertThat(details.policy()).isEqualTo(first.policy());assertThat(details.isEditable()).isEqualTo("ADMIN".equals(role));
        assertThat(details.isDraftValidationRequired()).isTrue();
        verify(dao).selectPolicyCount(new AttachmentPolicyManagementRows.Search("DRAFT",rule,20,20));
        verify(dao,never()).selectPolicyDetails(any(),eq(true));verify(dao,never()).selectRuleStatus(any());verifyNoInteractions(audit);
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyActiveAdminCanMutateEvenWhenServiceCalledDirectly(String role) {
        assertThatThrownBy(() -> service.insertPolicy(auth(role),key,request("OFF"))).isInstanceOf(ApiException.class).hasMessageContaining("ADMIN");
        assertThatThrownBy(() -> service.updatePolicyDraft(auth(role),UUID.randomUUID(),new AttachmentPolicyRequests.Update(0,rule,"OFF",1L,"변경")))
                .isInstanceOf(ApiException.class).hasMessageContaining("ADMIN");
        assertThatThrownBy(() -> service.insertPolicyRevision(auth(role),UUID.randomUUID(),key,new AttachmentPolicyRequests.Revision(0,"개정")))
                .isInstanceOf(ApiException.class).hasMessageContaining("ADMIN");
        verifyNoInteractions(dao,audit,registry);
    }
    @Test void anonymousInactiveResetAndExternalReadAreRejected() {
        assertThatThrownBy(() -> service.selectPolicyList(null,null,null,1,20)).isInstanceOf(ApiException.class);
        for(var principal:List.of(auth(actor,"ADMIN","SUSPENDED",false),auth(actor,"ADMIN","ACTIVE",true),auth("USER")))
            assertThatThrownBy(() -> service.selectPolicyList(principal,null,null,1,20)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,audit,registry);
    }
    @ParameterizedTest @ValueSource(longs={0,-1,83886081,Long.MAX_VALUE})
    void budgetOutsideHardCapCannotReachDatabase(long bytes) {
        assertThatThrownBy(() -> service.insertPolicy(auth("ADMIN"),key,new AttachmentPolicyRequests.Create(rule,"OFF",bytes,"사유")))
                .isInstanceOf(ApiException.class).hasMessageContaining("80 MiB");verifyNoInteractions(dao,audit,registry);
    }
    @Test void invalidModeReasonAndPaginationCannotReachDatabase() {
        assertThatThrownBy(() -> service.insertPolicy(auth("ADMIN"),key,request("AUTO"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.insertPolicy(auth("ADMIN"),key,new AttachmentPolicyRequests.Create(rule,"OFF",1L," "))).isInstanceOf(ApiException.class);
        for(int page:List.of(0,-1,Integer.MAX_VALUE)) assertThatThrownBy(() -> service.selectPolicyList(auth("ADMIN"),null,null,page,100)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.selectPolicyList(auth("ADMIN"),"arbitrary",null,1,20)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,audit,registry);
    }
    @Test void missingAndRetiredRulesCannotCreateDraftAndUnknownPolicyIs404() {
        when(dao.selectRuleStatus(rule)).thenReturn(null,"RETIRED");
        assertThatThrownBy(() -> create("OFF")).isInstanceOf(ApiException.class).hasMessageContaining("규칙을 찾을 수");
        assertThatThrownBy(() -> create("OFF")).isInstanceOf(ApiException.class).hasMessageContaining("퇴역 키워드");
        assertThatThrownBy(() -> service.selectPolicyDetails(auth("ADMIN"),UUID.randomUUID())).isInstanceOf(ApiException.class).hasMessageContaining("정책을 찾을 수");
        verify(dao,never()).insertPolicy(any());verifyNoInteractions(audit);
    }
    @Test void duplicateSystemProfileCannotBeSilentlySelectedByAdmin() {
        when(registry.selectProfileList()).thenReturn(List.of(profile,profile));
        assertThatThrownBy(() -> create("OFF")).isInstanceOf(ApiException.class).hasMessageContaining("중복");verify(dao,never()).insertPolicy(any());
    }
    @Test void familyVersionOverflowDoesNotCreateAnotherPolicy() {
        var first=create("OFF");when(dao.selectLatestVersion(first.policy().policyCode())).thenReturn(Integer.MAX_VALUE);
        assertThatThrownBy(() -> service.insertPolicyRevision(auth("ADMIN"),first.policy().policyId(),UUID.randomUUID(),new AttachmentPolicyRequests.Revision(0,"개정")))
                .isInstanceOf(ApiException.class).hasMessageContaining("최대값");verify(dao,times(1)).insertPolicy(any());
    }
}
