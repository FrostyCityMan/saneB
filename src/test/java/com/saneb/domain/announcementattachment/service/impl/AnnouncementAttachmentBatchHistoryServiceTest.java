package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.*;

class AnnouncementAttachmentBatchHistoryServiceTest {
    private final AnnouncementAttachmentBatchHistoryDao dao=mock(AnnouncementAttachmentBatchHistoryDao.class);
    private final AnnouncementAttachmentBatchDao batches=mock(AnnouncementAttachmentBatchDao.class);
    private final AnnouncementAttachmentBatchHistoryServiceImpl service=new AnnouncementAttachmentBatchHistoryServiceImpl(dao,batches);
    private final UUID batchId=UUID.randomUUID(),previewId=UUID.randomUUID();
    private final OffsetDateTime at=OffsetDateTime.parse("2026-09-12T00:00:00Z");
    @BeforeEach void setup(){when(batches.selectBatchDetails(batchId,false)).thenReturn(row(10));when(dao.selectActionCount(any())).thenReturn(1L);when(dao.selectActionList(any())).thenReturn(List.of(entry(8)));}
    private AttachmentBatchRows.Row row(int version){return new AttachmentBatchRows.Row(batchId,UUID.randomUUID(),"APPLIED","a".repeat(64),2,2,1,version,"{}","{}",UUID.randomUUID(),UUID.randomUUID(),"b".repeat(64),at);}
    private Entry entry(int version){return new Entry(UUID.randomUUID(),batchId,"APPLICATION","START",version,previewId,2,2,0,null,null,null,null,at);}
    private Authentication auth(String role,String status,boolean reset){var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(UUID.randomUUID(),"fixture","unused","QA",status,reset,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    private Authentication actor(){return auth("ADMIN","ACTIVE",false);}
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesReceiveImmutableApprovalCountsNotCurrentDeletedCount(String role){var result=service.selectActionList(auth(role,"ACTIVE",false),batchId,null,1,20);
        assertThat(result.throughVersion()).isEqualTo(10);assertThat(result.history().items().getFirst().deletedCountAtAcceptance()).isZero();
        assertThat(result.history().items().getFirst().approvedTargetCount()).isEqualTo(2);assertThat(result.currentHttpRequests()).isZero();
        verify(batches).selectBatchDetails(batchId,false);verifyNoMoreInteractions(batches);
    }
    @Test void historicalBoundAndLongOffsetDoNotResetToCurrentVersion(){when(dao.selectActionCount(any())).thenReturn(0L);when(dao.selectActionList(any())).thenReturn(List.of());
        var result=service.selectActionList(actor(),batchId,5,Integer.MAX_VALUE,100);assertThat(result.throughVersion()).isEqualTo(5);assertThat(result.currentBatchVersion()).isEqualTo(10);
        verify(dao).selectActionList(new AttachmentBatchHistorySearch(batchId,5,100,214748364600L));}
    @Test void emptyAnchorZeroIsNotCompletion(){when(dao.selectActionCount(any())).thenReturn(0L);when(dao.selectActionList(any())).thenReturn(List.of());
        assertThat(service.selectActionList(actor(),batchId,0,1,10).history().totalCount()).isZero();}
    @Test void futureAnchorMissingBatchAndInvalidPaginationFailBeforeHistoryQueries(){
        assertThatThrownBy(()->service.selectActionList(actor(),batchId,11,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectActionList(actor(),UUID.randomUUID(),null,1,20)).isInstanceOf(ApiException.class);
        for(int[] args:new int[][]{{0,20},{1,0},{1,101}})assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,args[0],args[1])).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectActionList(actor(),batchId,-1,1,20)).isInstanceOf(ApiException.class);verifyNoInteractions(dao);
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotEnumerateHistory(String role){assertThatThrownBy(()->service.selectActionList(auth(role,"ACTIVE",false),batchId,null,1,20)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,batches);}
    @Test void disabledResetAndMissingActorsCannotRead(){
        for(var invalid:List.of(auth("ADMIN","INACTIVE",false),auth("ADMIN","ACTIVE",true),UsernamePasswordAuthenticationToken.unauthenticated("fixture",null)))
            assertThatThrownBy(()->service.selectActionList(invalid,batchId,null,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectActionList(null,batchId,null,1,20)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,batches);
    }
    @Test void wrongMembershipAnchorCountAndOrderCannotLookLikeACompletePage(){
        var foreign=new Entry(UUID.randomUUID(),UUID.randomUUID(),"APPLICATION","START",8,previewId,2,2,0,null,null,null,null,at);
        when(dao.selectActionList(any())).thenReturn(List.of(foreign));assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
        when(dao.selectActionList(any())).thenReturn(List.of(entry(10)));assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
        when(dao.selectActionCount(any())).thenReturn(2L);when(dao.selectActionList(any())).thenReturn(List.of(entry(1),entry(2)));assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
        var duplicate=entry(2);when(dao.selectActionList(any())).thenReturn(List.of(duplicate,duplicate));assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
        when(dao.selectActionList(any())).thenReturn(List.of(entry(8)));assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
        when(dao.selectActionCount(any())).thenReturn(-1L);assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
        when(dao.selectActionCount(any())).thenReturn((long)Integer.MAX_VALUE*20+1);assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
    }
    @Test void rollbackKeepsOriginalImpactsAndRejectsOverflowingCounts(){
        var good=new Entry(UUID.randomUUID(),batchId,"ROLLBACK","START",8,null,2,1,0,1,1,0,1,at);when(dao.selectActionList(any())).thenReturn(List.of(good));
        assertThat(service.selectActionList(actor(),batchId,null,1,20).history().items()).containsExactly(good);
        var invalid=new Entry(good.actionId(),batchId,"ROLLBACK","START",8,null,2,1,0,1,Integer.MAX_VALUE,Integer.MAX_VALUE,1,at);
        when(dao.selectActionList(any())).thenReturn(List.of(invalid));assertThatThrownBy(()->service.selectActionList(actor(),batchId,null,1,20)).isInstanceOf(ApiException.class);
    }
    @Test void serviceIsReadOnlyRepeatableReadWithBoundedTransaction() throws Exception {
        var annotation=service.getClass().getMethod("selectActionList",Authentication.class,UUID.class,Integer.class,int.class,int.class).getAnnotation(Transactional.class);
        assertThat(annotation.readOnly()).isTrue();assertThat(annotation.isolation()).isEqualTo(Isolation.REPEATABLE_READ);assertThat(annotation.timeout()).isEqualTo(15);
    }
}
