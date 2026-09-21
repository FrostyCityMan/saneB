package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.extraction.*;
import com.saneb.domain.announcementattachment.service.impl.AttachmentProviderQaPlan;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** 실제 production 단건 실행기 검증. 운영 원장 저장/전체 정책 QA/worker E2E를 주장하지 않는다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_BBS_FIXED_CASE_QA", matches="true")
class AnnouncementAttachmentBbsFixedCaseQaTest {
    private static final String CASE="TAEBAEK-184816";
    private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    @TempDir Path directory;

    @Test @Timeout(420)
    void verifiesReviewedWholeFileExpectationsUsingProductionExecutor() throws Exception {
        var report=new LinkedHashMap<String,Object>();
        report.put("scope","FIXED_CASE_EXECUTOR_QA_EPHEMERAL_ONLY");report.put("caseCode",CASE);
        report.put("isPolicyQaPassed",false);report.put("productionWriteCount",0);report.put("status","INCOMPLETE");
        try(var context=new AnnotationConfigApplicationContext(StandardBbsAttachmentProfileConfiguration.class,
                SaeolGetAttachmentProfileConfiguration.class,LegalBoardAttachmentProfileConfiguration.class,
                BizInfoAttachmentDiscoveryProfile.class,SeoguSaeolAttachmentDiscoveryProfile.class,HwacheonPostAttachmentDiscoveryProfile.class);
            var client=new AttachmentPinnedDownloadClient()) {
            String distribution=System.getProperty("saneb.attachment-observation.extractor");
            var runtime=new AttachmentRuntimeIdentity(distribution);
            String runtimeHash=runtime.selectIdentity().configHash();
            var seed=AnnouncementAttachmentRealFileQaTest.selectDraftQaContext();
            var profiles=List.copyOf(context.getBeansOfType(AttachmentDiscoveryProfile.class).values());
            var registry=new AttachmentDiscoveryProfileRegistry(profiles);
            var scope=AttachmentProviderQaPlan.selectPlan(profiles,seed.targets());
            var prepared=new AttachmentProviderQaCatalog(JSON,registry).selectPrepared(scope,seed.rules(),runtimeHash,Instant.now());
            report.put("scopeSource","EPHEMERAL_REPOSITORY_ALL_UNDELETED_SEEDS_AND_NATIONAL_PROVIDERS");
            report.put("targetCount",scope.items().size());report.put("catalogHash",prepared.plan().catalogHash());
            report.put("referenceCount",prepared.plan().cases().size());report.put("executableCount",prepared.plan().executableCount());
            report.put("isExpectationCoverageComplete",prepared.plan().isExpectationCoverageComplete());
            assertEquals(seed.targets().size()+2,prepared.plan().targets().size(),"SEED_SCOPE_REDUCED");
            assertFalse(prepared.plan().isExpectationCoverageComplete(),"WHOLE_COVERAGE_NOT_APPROVED");
            assertFalse(prepared.plan().isQaPassed());
            var plan=prepared.plan().cases().stream().filter(c->CASE.equals(c.caseCode())).findFirst().orElseThrow();
            report.put("caseState",plan.statusCode());report.put("normalNotice",plan.normalNotice());
            assertEquals("EXPECTED_INPUT_READY",plan.statusCode(),"FROZEN_EXPECTATIONS_NOT_READY");
            assertFalse(plan.normalNotice(),"MIXED_DOCUMENT_IS_NOT_NORMAL_NOTICE");
            var input=prepared.inputs().stream().filter(c->CASE.equals(c.caseId())).findFirst().orElseThrow();
            assertEquals(2,input.files().size(),"WHOLE_FILE_SET_REQUIRED");
            assertEquals(List.of("UNKNOWN","FORM"),input.files().stream().map(f->f.roleExpectation().roleCode()).toList());
            var control=new BoundedControl(selectBoundedLimits(input.limits(),
                    System.getProperty("saneb.attachment-fixed.maximum-requests"),
                    System.getProperty("saneb.attachment-fixed.maximum-bytes")),System::nanoTime);
            var executor=new AttachmentProviderQaCaseExecutor(registry,client,new AttachmentTemporaryStorage(directory.toString()),runtime,
                    new IsolatedAttachmentExtractor(JSON,distribution),new AttachmentFileTypeValidator(),JSON);
            var result=executor.selectResult(input,control);report.put("result",result);
            assertEquals("PASSED",result.status(),"FIXED_CASE_EXECUTION_FAILED");
            assertEquals("FIXED_NOTICE_EXPECTATIONS_MATCHED",result.reasonCode());
            assertEquals(2,result.expectedFileCount());assertEquals(2,result.discoveredFileCount());
            assertTrue(result.discoveryComplete());assertTrue(result.allTextComplete());assertTrue(result.originalFilesRemoved());
            assertFalse(result.isPolicyQaPassed());assertFalse(control.inUse.get(),"LOCAL_RESOURCE_NOT_RELEASED");
            assertEquals(control.requests,result.requestReservations());assertEquals(control.bytes,result.reservedBytes());
            assertTrue(result.files().stream().allMatch(f->"PASSED".equals(f.status())&&f.roleAssessmentHash()!=null));
            report.put("status","FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED");
        } catch(Exception|AssertionError failure) {
            report.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));
            throw new AssertionError(CASE+": FIXED_CASE_QA_INCOMPLETE");
        } finally {
            try(var files=Files.walk(directory)) {
                report.put("originalFilesRemoved",files.noneMatch(p->Set.of("detail.html","attachment.bin").contains(p.getFileName().toString())));
            }
            Path output=Path.of(System.getProperty("saneb.attachment-observation.report")).toAbsolutePath().normalize();
            Files.createDirectories(output);JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve(CASE+"-fixed-case.json").toFile(),report);
        }
    }

    /** 이전 관측에서 사용한 예산을 빼고 실행할 때 상한 축소만 허용한다. */
    static AttachmentProviderQaCase.Limits selectBoundedLimits(AttachmentProviderQaCase.Limits maximum,
            String requestsValue,String bytesValue) {
        if((requestsValue==null)!=(bytesValue==null))throw new IllegalArgumentException("FIXED_BUDGET_PAIR_REQUIRED");
        if(requestsValue==null)return maximum;
        if(!requestsValue.matches("[1-9][0-9]{0,2}")||!bytesValue.matches("[1-9][0-9]{0,8}"))
            throw new IllegalArgumentException("FIXED_BUDGET_INVALID");
        int requests=Integer.parseInt(requestsValue);long bytes=Long.parseLong(bytesValue);
        if(requests>maximum.maximumRequestReservations()||bytes>maximum.maximumReservedBytes())
            throw new IllegalArgumentException("FIXED_BUDGET_CANNOT_INCREASE");
        return new AttachmentProviderQaCase.Limits(maximum.maximumSeconds(),requests,bytes);
    }

    /** 시험 프로세스 한 개의 제한된 자원. 운영 callback/원장 대신 사용할 수 없는 test 전용 구현이다. */
    static final class BoundedControl implements AttachmentProviderQaCaseExecutor.ExecutionControl {
        final AttachmentProviderQaCase.Limits limits;final LongSupplier nanoTime;final long started;
        final AtomicBoolean inUse=new AtomicBoolean();long requests,bytes;
        BoundedControl(AttachmentProviderQaCase.Limits limits,LongSupplier nanoTime) {
            this.limits=limits;this.nanoTime=nanoTime;this.started=nanoTime.getAsLong();
        }
        @Override public boolean selectExecutionAllowed() {
            return !Thread.currentThread().isInterrupted()&&nanoTime.getAsLong()-started<limits.maximumSeconds()*1_000_000_000L;
        }
        @Override public AutoCloseable selectDownloadPermit(String hostHash) {
            return hostHash!=null&&hostHash.matches("[0-9a-f]{64}")?permit():null;
        }
        @Override public AutoCloseable selectExtractionPermit() {return permit();}
        private AutoCloseable permit() {
            if(!selectExecutionAllowed()||!inUse.compareAndSet(false,true))return null;
            var closed=new AtomicBoolean();return ()->{if(closed.compareAndSet(false,true))inUse.set(false);};
        }
        @Override public boolean saveRequestReservation() {
            if(!selectExecutionAllowed()||requests>=limits.maximumRequestReservations())return false;requests++;return true;
        }
        @Override public boolean saveByteReservation(long count) {
            if(!selectExecutionAllowed()||count<=0||count>limits.maximumReservedBytes()-bytes)return false;bytes+=count;return true;
        }
    }
}
