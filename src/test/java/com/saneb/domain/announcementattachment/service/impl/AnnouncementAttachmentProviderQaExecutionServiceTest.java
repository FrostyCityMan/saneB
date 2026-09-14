package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaDao;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCaseExecutor;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCaseExecutor.*;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** 원장 callback/transaction 경계 검증. DAO와 HTTP 실행기는 대역이며 실제 PG/실파일 성공 증거가 아니다. */
class AnnouncementAttachmentProviderQaExecutionServiceTest {
    final AnnouncementAttachmentProviderQaDao dao=mock(AnnouncementAttachmentProviderQaDao.class);
    final AttachmentProviderQaCaseExecutor executor=mock(AttachmentProviderQaCaseExecutor.class);
    final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final UUID caseId=UUID.randomUUID(),runId=UUID.randomUUID();
    final String codeHash="a".repeat(64),inputHash="b".repeat(64),profileHash="c".repeat(64),runtimeHash="d".repeat(64),locatorHash="e".repeat(64);
    final AtomicInteger active=new AtomicInteger();
    final List<Resource> resources=new ArrayList<>(),deleted=new ArrayList<>();
    final List<Boolean> rollbacks=new ArrayList<>();
    final Instant now=Instant.parse("2026-09-12T00:00:00Z");
    AttachmentProviderQaCase input;
    AnnouncementAttachmentProviderQaExecutionServiceImpl service;
    String status="PENDING",runStatus="READY",savedError,savedJson;
    UUID token;
    int requests;
    long bytes;
    boolean current=true,allowed=true;

    @BeforeEach void setup() {
        input=new AttachmentProviderQaCase("CASE-001","TEST_PROFILE",profileHash,
                new Source("BIZINFO","source-id","https://example.go.kr/detail",null,null),"원문 제목은 저장하지 않음",
                new AnnouncementSourceClassificationRuleSet("FIXTURE",List.of()),runtimeHash,"FOUND",true,
                List.of(new ExpectedFile(locatorHash,true,"PDF","f".repeat(64),"COMPLETE_TEXT",1,1,List.of("원문 기대 문구"))),new Limits(420,44,83886080L));
        when(transactions.getTransaction(any())).thenAnswer(c->{assertThat(active.incrementAndGet()).isEqualTo(1);return new SimpleTransactionStatus();});
        doAnswer(c->{rollbacks.add(c.getArgument(0,SimpleTransactionStatus.class).isRollbackOnly());assertThat(active.decrementAndGet()).isZero();return null;}).when(transactions).commit(any());
        doAnswer(c->{assertThat(active.decrementAndGet()).isZero();return null;}).when(transactions).rollback(any());
        when(dao.selectRunLock(runId)).thenAnswer(c->{assertThat(active.get()).isEqualTo(1);return runId.toString();});
        when(dao.selectCaseDetails(caseId)).thenAnswer(c->{assertThat(active.get()).isEqualTo(1);return row();});
        when(executor.selectHash(input)).thenReturn(inputHash);
        when(dao.updateRunStarted(runId)).thenAnswer(c->{runStatus="RUNNING";return 1;});
        when(dao.updateClaim(eq(caseId),any())).thenAnswer(c->{status="RUNNING";token=c.getArgument(1);return 1;});
        when(dao.selectExecutionAllowed(eq(caseId),any())).thenAnswer(c->{assertThat(active.get()).isEqualTo(1);return allowed;});
        when(dao.insertResource(any())).thenAnswer(c->{assertThat(active.get()).isEqualTo(1);resources.add(c.getArgument(0));return 1;});
        when(dao.deleteResource(any())).thenAnswer(c->{assertThat(active.get()).isEqualTo(1);deleted.add(c.getArgument(0));return 1;});
        when(dao.updateUsage(eq(caseId),any(),anyInt(),anyLong())).thenAnswer(c->{assertThat(active.get()).isEqualTo(1);requests+=(int)c.getArgument(2);bytes+=(long)c.getArgument(3);return 1;});
        when(dao.updateFinished(eq(caseId),any(),anyString(),nullable(String.class),nullable(String.class),nullable(String.class))).thenAnswer(c->{
            assertThat(active.get()).isEqualTo(1);assertThat(c.getArgument(1,UUID.class)).isEqualTo(token);
            status="CANCEL_REQUESTED".equals(runStatus)?"CANCELLED":c.getArgument(2);savedError=c.getArgument(3);savedJson=c.getArgument(4);return 1;
        });
        when(executor.selectResult(eq(input),any())).thenAnswer(c->{assertThat(active.get()).isZero();return result("PASSED",null);});
        service=create(true,()->{assertThat(active.get()).isZero();return codeHash;});
    }
    AnnouncementAttachmentProviderQaExecutionServiceImpl create(boolean enabled,Supplier<String> code) {
        return new AnnouncementAttachmentProviderQaExecutionServiceImpl(dao,executor,mapper,transactions,enabled,code);
    }
    CaseRow row() {
        return new CaseRow(caseId,runId,input.caseId(),inputHash,profileHash,1,420,44,83886080L,requests,bytes,status,0,token,
                token==null?null:OffsetDateTime.now().plusMinutes(8),runStatus,codeHash,runtimeHash,current,savedJson);
    }
    Result result(String selected,String reason) {
        return new Result("SINGLE_FIXED_NOTICE_PROVIDER_QA",input.caseId(),inputHash,profileHash,runtimeHash,selected,reason,"CANDIDATE","FOUND",true,
                1,1,List.of(new FileResult(locatorHash,"PASSED",null,"PDF","COMPLETE_TEXT",12,"f".repeat(64),"0".repeat(64),12,1)),
                requests,bytes,true,true,false,now,now.plusSeconds(1));
    }
    void execute(String expected) {assertThat(service.saveCase(caseId,input).statusCode()).isEqualTo(expected);assertThat(active.get()).isZero();}
    void notExecuted() {verify(executor,never()).selectResult(any(),any());verify(dao,never()).updateClaim(any(),any());}
    @Test void disabledDoesNotReadOrHashOrExecute() {service=create(false,()->{throw new AssertionError();});execute("DISABLED");verifyNoInteractions(dao,executor,transactions);}
    @Test void invalidAndMissingInputsDoNotClaim() {
        assertThat(service.saveCase(null,input).statusCode()).isEqualTo("CASE_INPUT_INVALID");
        assertThat(service.saveCase(caseId,null).statusCode()).isEqualTo("CASE_INPUT_INVALID");
        doReturn(null).when(dao).selectCaseDetails(caseId);execute("NOT_FOUND");notExecuted();
    }
    @ParameterizedTest @ValueSource(strings={"RUNNING","PASSED","FAILED","CANCELLED"})
    void ownedAndTerminalCasesAreNotReplayed(String state) {status=state;execute(state);notExecuted();}
    @Test void codeUnavailableDoesNotStartNetwork() {service=create(true,()->{throw new IllegalStateException("unexposed path");});execute("CODE_UNAVAILABLE");notExecuted();}
    @Test void installedCodeMismatchDoesNotClaim() {service=create(true,()->"f".repeat(64));execute("INPUT_CHANGED");notExecuted();}
    @Test void changedPolicyOrFixedInputDoesNotClaim() {current=false;execute("INPUT_CHANGED");current=true;when(executor.selectHash(input)).thenReturn("f".repeat(64));execute("INPUT_CHANGED");notExecuted();}
    @Test void claimFailureDoesNotExecuteOrCleanAnUnownedCase() {when(dao.updateClaim(any(),any())).thenReturn(0);execute("NOT_CLAIMED");verify(executor,never()).selectResult(any(),any());verify(dao,never()).deleteCaseResources(any(),any());}
    @Test void startFailureDoesNotClaim() {when(dao.updateRunStarted(any())).thenReturn(0);execute("NOT_CLAIMED");notExecuted();}
    @Test void changeAfterFirstReadIsRecheckedUnderParentLock() {
        doReturn(row()).doAnswer(c->{current=false;return row();}).when(dao).selectCaseDetails(caseId);execute("NOT_CLAIMED");notExecuted();
    }
    @Test void successfulResultStoresOnlyMetadataAndCleansOnlyOwnedPermits() {
        execute("PASSED");assertThat(savedError).isNull();assertThat(savedJson).contains("SINGLE_FIXED_NOTICE_PROVIDER_QA","\"isPolicyQaPassed\":false")
                .doesNotContain(input.title(),input.source().sourceUrl(),"원문 기대 문구");
        verify(dao).deleteCaseResources(caseId,token);verify(dao).updateFinished(eq(caseId),eq(token),eq("PASSED"),isNull(),eq(savedJson),matches("[0-9a-f]{64}"));
    }
    @Test void resourceAndBudgetCallbacksUseShortTransactionsAndIdempotentOwnedRelease() {
        when(executor.selectResult(eq(input),any())).thenAnswer(c->{
            assertThat(active.get()).isZero();var control=c.getArgument(1,ExecutionControl.class);assertThat(control.selectExecutionAllowed()).isTrue();
            var download=control.selectDownloadPermit("f".repeat(64));assertThat(download).isNotNull();assertThat(active.get()).isZero();
            assertThat(control.saveRequestReservation()).isTrue();assertThat(control.saveByteReservation(31)).isTrue();download.close();download.close();
            var extraction=control.selectExtractionPermit();assertThat(extraction).isNotNull();extraction.close();extraction.close();
            return result("PASSED",null);
        });
        execute("PASSED");assertThat(resources).extracting(Resource::resourceCode).containsExactly("DOWNLOAD","HOST","EXTRACTION");
        assertThat(resources).allSatisfy(r->{assertThat(r.caseId()).isEqualTo(caseId);assertThat(r.leaseToken()).isEqualTo(token);assertThat(r.slotNo()).isEqualTo(1);});
        assertThat(deleted).containsExactlyElementsOf(resources);assertThat(requests).isEqualTo(1);assertThat(bytes).isEqualTo(31);
    }
    @Test void secondDownloadSlotIsTriedButNoThirdSlotExists() {
        doAnswer(c->{var r=c.getArgument(0,Resource.class);resources.add(r);return r.resourceCode().equals("DOWNLOAD")&&r.slotNo()==1?0:1;}).when(dao).insertResource(any());
        when(executor.selectResult(eq(input),any())).thenAnswer(c->{try(var permit=c.getArgument(1,ExecutionControl.class).selectDownloadPermit("f".repeat(64))) {assertThat(permit).isNotNull();}return result("PASSED",null);});
        execute("PASSED");assertThat(resources).extracting(Resource::slotNo).containsExactly(1,2,1);assertThat(deleted).hasSize(2);
    }
    @Test void failedHostRollsBackAcquiredGlobalSlot() {
        doAnswer(c->{var r=c.getArgument(0,Resource.class);resources.add(r);return r.resourceCode().equals("HOST")?0:1;}).when(dao).insertResource(any());
        when(executor.selectResult(eq(input),any())).thenAnswer(c->{assertThat(c.getArgument(1,ExecutionControl.class).selectDownloadPermit("f".repeat(64))).isNull();return result("FAILED","RESOURCE_BUSY");});
        execute("FAILED");assertThat(rollbacks).contains(true);assertThat(resources).extracting(Resource::resourceCode).containsExactly("DOWNLOAD","HOST");
    }
    @Test void busySlotsAndBadHostFailWithoutAllocationOrUnboundedRetry() {
        doReturn(0).when(dao).insertResource(any());
        when(executor.selectResult(eq(input),any())).thenAnswer(c->{var control=c.getArgument(1,ExecutionControl.class);
            assertThat(control.selectDownloadPermit(null)).isNull();assertThat(control.selectDownloadPermit("host-name")).isNull();
            assertThat(control.selectDownloadPermit("f".repeat(64))).isNull();assertThat(control.selectExtractionPermit()).isNull();return result("FAILED","RESOURCE_BUSY");});
        execute("FAILED");verify(dao,times(3)).insertResource(any());verify(dao,never()).deleteResource(any());
    }
    @Test void requestAndByteDenialsStayDeniedAndInvalidBytesNeverReachDao() {
        when(dao.updateUsage(any(),any(),anyInt(),anyLong())).thenReturn(0);
        when(executor.selectResult(eq(input),any())).thenAnswer(c->{var control=c.getArgument(1,ExecutionControl.class);
            allowed=false;assertThat(control.selectExecutionAllowed()).isFalse();assertThat(control.saveRequestReservation()).isFalse();assertThat(control.saveByteReservation(1)).isFalse();
            for(long n:List.of(0L,-1L,83886081L,Long.MAX_VALUE))assertThat(control.saveByteReservation(n)).isFalse();return result("FAILED","EXECUTION_STOPPED");});
        execute("FAILED");verify(dao,times(2)).updateUsage(any(),any(),anyInt(),anyLong());
    }
    @Test void changedCodeAfterExecutionCannotPass() {var calls=new AtomicInteger();service=create(true,()->calls.getAndIncrement()==0?codeHash:"f".repeat(64));execute("FAILED");assertThat(savedError).isEqualTo("RESULT_BINDING_CHANGED");}
    @ParameterizedTest @ValueSource(strings={"scope","caseId","inputHash","profileHash","runtimeHash","expectedFileCount","files","originalFilesRemoved","isPolicyQaPassed","completedAt"})
    void resultMustMatchExactCaseAndCleanupBeforePassing(String field) throws Exception {
        ObjectNode tree=mapper.valueToTree(result("PASSED",null));
        switch(field) {
            case "expectedFileCount"->tree.put(field,2);
            case "files"->((ObjectNode)tree.path("files").get(0)).put("locatorHash","f".repeat(64));
            case "originalFilesRemoved"->tree.put(field,false);
            case "isPolicyQaPassed"->tree.put(field,true);
            case "completedAt"->tree.putNull(field);
            default->tree.put(field,"wrong");
        }
        when(executor.selectResult(any(),any())).thenReturn(mapper.treeToValue(tree,Result.class));execute("FAILED");assertThat(savedError).isEqualTo("RESULT_BINDING_CHANGED");
    }
    @Test void actualReservedUsageMustEqualResultNotJustStayUnderLimit() {
        var result=result("PASSED",null);when(executor.selectResult(eq(input),any())).thenAnswer(c->{requests=1;bytes=15;return result;});
        execute("FAILED");assertThat(savedError).isEqualTo("RESULT_USAGE_CHANGED");
    }
    @Test void cancellationWinsOverLateSuccessfulWork() {
        when(executor.selectResult(eq(input),any())).thenAnswer(c->{runStatus="CANCEL_REQUESTED";return result("PASSED",null);});execute("CANCELLED");verify(dao).deleteCaseResources(caseId,token);
    }
    @Test void expiredOwnershipDoesNotPretendResultWasSaved() {
        when(dao.updateFinished(any(),any(),anyString(),nullable(String.class),nullable(String.class),nullable(String.class))).thenReturn(0);
        execute("LEASE_LOST");assertThat(savedJson).isNull();verify(dao).deleteCaseResources(caseId,token);
    }
    @Test void executorFailureStoresFixedCodeAndReleasesOwnedResources() {
        when(executor.selectResult(any(),any())).thenThrow(new IllegalStateException("private input must not be copied"));execute("FAILED");
        assertThat(savedError).isEqualTo("CASE_EXECUTION_FAILED");assertThat(savedJson).isNull();verify(dao).deleteCaseResources(caseId,token);
    }
    @Test void missingResultFailsRatherThanPublishingSuccess() {when(executor.selectResult(any(),any())).thenReturn(null);execute("FAILED");assertThat(savedError).isEqualTo("RESULT_BINDING_CHANGED");}
    @Test void missingParentNeverExecutes() {
        doReturn(null).when(dao).selectRunLock(runId);assertThatThrownBy(()->service.saveCase(caseId,input)).hasMessage("PROVIDER_QA_RUN_MISSING");verify(executor,never()).selectResult(any(),any());assertThat(active.get()).isZero();
    }
    @AfterEach void noLeakedTransactions() {assertThat(active.get()).isZero();}
}
