package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.dao.*;
import com.saneb.domain.announcementsource.provider.content.*;
import com.saneb.domain.announcementsource.service.*;
import com.saneb.domain.announcementsource.vo.*;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

class AnnouncementSourceBodyRefreshServiceImplTest {
    private final UUID source=UUID.randomUUID(),actor=UUID.randomUUID(),base=UUID.randomUUID(),content=UUID.randomUUID(),rule=UUID.randomUUID();
    private final AnnouncementSourceBodyRefreshDao dao=mock(AnnouncementSourceBodyRefreshDao.class);
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementSourceDao sources=mock(AnnouncementSourceDao.class);
    private final AnnouncementSourceRuleReleaseService rules=mock(AnnouncementSourceRuleReleaseService.class);
    private final AnnouncementSourceClassificationPersistenceService persistence=mock(AnnouncementSourceClassificationPersistenceService.class);
    private final ProviderContentClient client=mock(ProviderContentClient.class);
    private final Authentication auth=mock(Authentication.class);
    private final AnnouncementSourceBodyRefreshServiceImpl service=new AnnouncementSourceBodyRefreshServiceImpl(dao,jobs,sources,rules,persistence,List.of(client));
    private final String body="소상공인 지원금 신청기간 10월";
    private AttachmentSourceContextRow context(boolean required,int version) {
        return new AttachmentSourceContextRow(source,"LOCAL_GOV_NOTICE","PRODUCTION","ACCEPTED",base,content,rule,
                "COMBINATION_MATCHED",version,4,required,null,null);
    }
    private SourceBodyRefreshRows.Preview preview(OffsetDateTime expiry) {
        return new SourceBodyRefreshRows.Preview(UUID.randomUUID(),source,actor,base,content,rule,2,4,body,
                AnnouncementSourceBodyRefreshServiceImpl.selectHash(body),"NOTICE_BODY_2",expiry,null);
    }
    @BeforeEach void setup() {
        var principal=mock(AuthenticatedUserDetails.class);
        when(principal.userId()).thenReturn(actor);when(auth.getPrincipal()).thenReturn(principal);
        when(jobs.selectSourceContextDetails(source)).thenReturn(context(false,2));
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(context(false,2));
        when(dao.selectBoundaryDetails(source)).thenReturn(new SourceBodyRefreshRows.Boundary(UUID.randomUUID(),"https://example.go.kr/list",false));
        var now=OffsetDateTime.now();
        when(sources.selectSourceDetails(source)).thenReturn(new AnnouncementSourceSnapshotRow(source,"SRC-1","LOCAL_GOV_NOTICE","1",
                "소상공인 지원금","기관",null,null,now,now,"https://example.go.kr/notice", "사이트 메뉴 소상공인 지원금",null,null,
                "COMPLETE",null,"a".repeat(64),"REVIEW_PENDING","REVIEW_REQUIRED","BODY_GROUP_B_MATCHED",null,2,now,now,now));
        when(rules.selectActiveRuleSet(rule)).thenReturn(new AnnouncementSourceClassificationRuleSet("ACTIVE",List.of(
                new AnnouncementSourceClassificationRule("T","TARGET",RuleGroupKindCode.TARGET,"소상공인",StrengthCode.STRONG,TargetCategoryCode.BUSINESS,null,
                        List.of(AnnouncementSourceClassificationTerm.canonical("소상공인",MatchModeCode.NORMALIZED_PHRASE)),true),
                new AnnouncementSourceClassificationRule("S","SUPPORT",RuleGroupKindCode.SUPPORT_TYPE,"지원금",StrengthCode.STRONG,null,SupportTypeCode.GRANT_SUBSIDY,
                        List.of(AnnouncementSourceClassificationTerm.canonical("지원금",MatchModeCode.NORMALIZED_PHRASE)),true))));
    }
    @Test void previewFetchesOnlyRegisteredSourceAndDoesNotApply() {
        when(client.selectProviderCode()).thenReturn("LOCAL_GOV_NOTICE");when(client.isEnabled()).thenReturn(true);
        when(client.selectContent(any())).thenAnswer(a->ProviderContentResult.available(a.getArgument(0),body,URI.create("https://example.go.kr/notice"),200,1,0));
        when(dao.insertPreview(any())).thenReturn(1);
        var result=service.insertPreview(auth,source);
        assertThat(result.isChanged()).isTrue();assertThat(result.afterBody()).isEqualTo(body);
        assertThat(result.afterStatusCode()).isEqualTo("ACCEPTED");
        verifyNoInteractions(persistence);verify(dao,never()).updateBody(any(),any(),anyInt());
        var audit=ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(sources).insertAuditLog(audit.capture());
        assertThat(audit.getValue().metadataJson()).doesNotContain(body,"사이트 메뉴");
    }
    @Test void failureCannotReplaceOldBodyOrCreateReadyPreview() {
        when(client.selectProviderCode()).thenReturn("LOCAL_GOV_NOTICE");when(client.isEnabled()).thenReturn(true);
        when(client.selectContent(any())).thenAnswer(a->ProviderContentResult.failure(a.getArgument(0),ProviderContentCodes.FailureCode.BODY_SELECTOR_CHANGED,null,200,1,0));
        assertThatThrownBy(()->service.insertPreview(auth,source)).isInstanceOf(ApiException.class).hasMessageContaining("기존 본문과 판정은 변경하지 않았습니다");
        verify(dao,never()).insertPreview(any());verifyNoInteractions(persistence);
    }
    @Test void appliedOrLinkedSourceIsBlockedBeforeNetwork() {
        when(jobs.selectSourceContextDetails(source)).thenReturn(context(true,2));
        assertThatThrownBy(()->service.insertPreview(auth,source)).hasMessageContaining("원복");
        when(jobs.selectSourceContextDetails(source)).thenReturn(context(false,2));
        when(dao.selectBoundaryDetails(source)).thenReturn(new SourceBodyRefreshRows.Boundary(UUID.randomUUID(),"https://example.go.kr/list",true));
        assertThatThrownBy(()->service.insertPreview(auth,source)).hasMessageContaining("진행 중 첨부 작업");
        verifyNoInteractions(client,persistence);
    }
    @Test void applyUsesFrozenBodyAndPreservesHistoryWithoutNetwork() {
        var p=preview(OffsetDateTime.now().plusMinutes(20));UUID evaluation=UUID.randomUUID();
        when(dao.selectPreviewDetailsForUpdate(source,p.id())).thenReturn(p);
        when(dao.updateBody(source,body,2)).thenReturn(1);when(dao.updateApplied(p.id(),evaluation)).thenReturn(1);
        when(persistence.saveChangedContentEvaluation(eq(source),isNull(),eq(rule),any(),any(),eq("REVIEW_PENDING"),eq(2))).thenReturn(evaluation);
        assertThat(service.savePreview(auth,source,p.id()).evaluationId()).isEqualTo(evaluation);
        verifyNoInteractions(client);
        verify(dao).updateBody(source,body,2);
    }
    @Test void retryReturnsReceiptWithoutSecondWrite() {
        var p=preview(OffsetDateTime.now().minusMinutes(1));UUID evaluation=UUID.randomUUID();
        when(dao.selectPreviewDetailsForUpdate(source,p.id())).thenReturn(new SourceBodyRefreshRows.Preview(p.id(),source,actor,base,content,rule,2,4,body,p.bodyHash(),p.extractorVersion(),p.expiresAt(),evaluation));
        assertThat(service.savePreview(auth,source,p.id()).evaluationId()).isEqualTo(evaluation);
        verifyNoInteractions(client,persistence);verify(dao,never()).updateBody(any(),any(),anyInt());
    }
    @Test void changedVersionAndExpiredPreviewDoNotWrite() {
        var p=preview(OffsetDateTime.now().plusMinutes(20));when(dao.selectPreviewDetailsForUpdate(source,p.id())).thenReturn(p);
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(context(false,3));
        assertThatThrownBy(()->service.savePreview(auth,source,p.id())).hasMessageContaining("버전이 변경");
        when(jobs.selectSourceContextDetailsForUpdate(source)).thenReturn(context(false,2));
        var expired=preview(OffsetDateTime.now().minusMinutes(1));when(dao.selectPreviewDetailsForUpdate(source,expired.id())).thenReturn(expired);
        assertThatThrownBy(()->service.savePreview(auth,source,expired.id())).hasMessageContaining("유효기간");
        verifyNoInteractions(client,persistence);verify(dao,never()).updateBody(any(),any(),anyInt());
    }
    @Test void otherActorAndMissingAuthenticationDoNotWrite() {
        var p=preview(OffsetDateTime.now().plusMinutes(20));
        when(dao.selectPreviewDetailsForUpdate(source,p.id())).thenReturn(new SourceBodyRefreshRows.Preview(p.id(),source,UUID.randomUUID(),base,content,rule,2,4,body,p.bodyHash(),p.extractorVersion(),p.expiresAt(),null));
        assertThatThrownBy(()->service.savePreview(auth,source,p.id())).hasMessageContaining("이 계정");
        assertThatThrownBy(()->service.insertPreview(null,source)).isInstanceOf(ApiException.class);
        verifyNoInteractions(client,persistence);
    }
}
