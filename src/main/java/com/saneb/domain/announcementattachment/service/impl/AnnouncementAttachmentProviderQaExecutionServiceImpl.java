package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaDao;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCaseExecutor;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaExecutionService;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

/** 짧은 원장 transaction을 실행기의 소유권/공유 slot/예산 callback에 연결한다. */
@Service
public class AnnouncementAttachmentProviderQaExecutionServiceImpl implements AnnouncementAttachmentProviderQaExecutionService {
    private final AnnouncementAttachmentProviderQaDao dao;
    private final AttachmentProviderQaCaseExecutor executor;
    private final ObjectMapper mapper;
    private final Supplier<String> codeHash;
    private final boolean enabled;
    private final TransactionTemplate read,write;
    @org.springframework.beans.factory.annotation.Autowired
    public AnnouncementAttachmentProviderQaExecutionServiceImpl(AnnouncementAttachmentProviderQaDao dao,AttachmentProviderQaCaseExecutor executor,
            ObjectMapper mapper,PlatformTransactionManager transactions,
            @Value("${saneb.announcement-attachment.provider-qa.enabled:false}") boolean enabled) {
        this(dao,executor,mapper,transactions,enabled,()->{
            try {return new AttachmentApplicationCodeFingerprint(mapper).selectVerifiedHash();}
            catch(java.io.IOException failure) {throw new IllegalStateException("PROVIDER_QA_CODE_UNAVAILABLE");}
        });
    }
    AnnouncementAttachmentProviderQaExecutionServiceImpl(AnnouncementAttachmentProviderQaDao dao,AttachmentProviderQaCaseExecutor executor,
            ObjectMapper mapper,PlatformTransactionManager transactions,boolean enabled,Supplier<String> codeHash) {
        this.dao=dao;this.executor=executor;this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);this.enabled=enabled;this.codeHash=codeHash;
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setTimeout(10);
        write=new TransactionTemplate(transactions);write.setTimeout(10);write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Outcome saveCase(UUID caseId,AttachmentProviderQaCase input) {
        if (!enabled) return new Outcome(caseId,"DISABLED");
        if (caseId==null || input==null) return new Outcome(caseId,"CASE_INPUT_INVALID");
        var located=read.execute(tx->dao.selectCaseDetails(caseId));
        if (located==null) return new Outcome(caseId,"NOT_FOUND");
        if (!"PENDING".equals(located.statusCode())) return new Outcome(caseId,located.statusCode());
        String installed;
        try {installed=codeHash.get();} catch(RuntimeException failure) {return new Outcome(caseId,"CODE_UNAVAILABLE");}
        if (!matches(located,input,installed)) return new Outcome(caseId,"INPUT_CHANGED");
        var claimed=write.execute(tx->{
            lock(located.runId());dao.updateExpiredCases(located.runId());dao.updateUnstartedCancelled(located.runId());dao.updateRunFinished(located.runId());
            var row=dao.selectCaseDetails(caseId);
            if (row==null || !"PENDING".equals(row.statusCode()) || !matches(row,input,installed)) return null;
            if ("READY".equals(row.runStatusCode()) && dao.updateRunStarted(row.runId())!=1) return null;
            if (!Set.of("READY","RUNNING").contains(row.runStatusCode())) return null;
            UUID token=UUID.randomUUID();
            if (dao.updateClaim(caseId,token)!=1) return null;
            return dao.selectCaseDetails(caseId);
        });
        if (claimed==null) return new Outcome(caseId,"NOT_CLAIMED");
        try {
            var result=executor.selectResult(input,new Control(claimed));
            String after=codeHash.get();
            boolean bound=validResult(claimed,input,result) && Objects.equals(installed,after);
            String status=bound && "PASSED".equals(result.status())?"PASSED":bound && "CANCELLED".equals(result.status())?"CANCELLED":"FAILED";
            String error="PASSED".equals(status)?null:bound?result.reasonCode():"RESULT_BINDING_CHANGED";
            String json=result==null?null:mapper.writeValueAsString(mapper.convertValue(result,Object.class));
            if (json!=null && json.getBytes(StandardCharsets.UTF_8).length>32768) {json=null;status="FAILED";error="EVIDENCE_LIMIT";}
            return finish(claimed,status,error,json);
        } catch (Exception failure) {
            // 원문/환경/SQL 예외를 상태나 로그에 복사하지 않는다. 저장 불가 시 lease 만료로 회수한다.
            return finish(claimed,"FAILED","CASE_EXECUTION_FAILED",null);
        } finally {
            write.executeWithoutResult(tx->{lock(claimed.runId());dao.deleteCaseResources(claimed.caseId(),claimed.leaseToken());});
        }
    }
    private boolean matches(CaseRow row,AttachmentProviderQaCase input,String installed) {
        return installed!=null && installed.matches("[0-9a-f]{64}") && installed.equals(row.executionCodeHash())
                && Boolean.TRUE.equals(row.inputVersionsCurrent()) && input.limits()!=null
                && Objects.equals(row.caseCode(),input.caseId()) && Objects.equals(row.profileHash(),input.profileHash())
                && Objects.equals(row.runtimeHash(),input.runtimeHash()) && Objects.equals(row.inputHash(),executor.selectHash(input))
                && Objects.equals(row.expectedFileCount(),input.files().size()) && Objects.equals(row.maximumSeconds(),input.limits().maximumSeconds())
                && Objects.equals(row.maximumRequests(),input.limits().maximumRequestReservations()) && Objects.equals(row.maximumBytes(),input.limits().maximumReservedBytes());
    }
    private boolean validResult(CaseRow row,AttachmentProviderQaCase input,AttachmentProviderQaCaseExecutor.Result result) {
        return result!=null && "SINGLE_FIXED_NOTICE_PROVIDER_QA".equals(result.scope()) && !result.isPolicyQaPassed()
                && Objects.equals(row.caseCode(),result.caseId()) && Objects.equals(row.inputHash(),result.inputHash())
                && Objects.equals(row.profileHash(),result.profileHash()) && Objects.equals(row.runtimeHash(),result.runtimeHash())
                && result.expectedFileCount()==row.expectedFileCount() && result.files().size()==row.expectedFileCount()
                && result.files().stream().map(AttachmentProviderQaCaseExecutor.FileResult::locatorHash).toList()
                    .equals(input.files().stream().map(AttachmentProviderQaCase.ExpectedFile::locatorHash).toList())
                && (!"PASSED".equals(result.status()) || result.originalFilesRemoved())
                && result.requestReservations()>=0 && result.requestReservations()<=row.maximumRequests()
                && result.reservedBytes()>=0 && result.reservedBytes()<=row.maximumBytes()
                && result.startedAt()!=null && result.completedAt()!=null && !result.completedAt().isBefore(result.startedAt());
    }
    private Outcome finish(CaseRow owner,String status,String error,String json) {
        final String hash=json==null?null:hash(json);
        return write.execute(tx->{
            lock(owner.runId());
            var current=dao.selectCaseDetails(owner.caseId());
            String selectedStatus=status,selectedError=error;
            if (json!=null) try {
                var result=mapper.readTree(json);
                if (current==null || !result.path("requestReservations").canConvertToLong() || !result.path("reservedBytes").canConvertToLong()
                        || result.path("requestReservations").longValue()!=current.requestReservations()
                        || result.path("reservedBytes").longValue()!=current.reservedBytes()) {selectedStatus="FAILED";selectedError="RESULT_USAGE_CHANGED";}
            } catch(Exception ignored) {selectedStatus="FAILED";selectedError="EVIDENCE_INVALID";}
            if (dao.updateFinished(owner.caseId(),owner.leaseToken(),selectedStatus,selectedError,json,hash)!=1) return new Outcome(owner.caseId(),"LEASE_LOST");
            dao.updateUnstartedCancelled(owner.runId());dao.updateRunFinished(owner.runId());
            return new Outcome(owner.caseId(),dao.selectCaseDetails(owner.caseId()).statusCode());
        });
    }
    private void lock(UUID runId) {dao.selectQueueLock();if (dao.selectRunLock(runId)==null) throw new IllegalStateException("PROVIDER_QA_RUN_MISSING");}
    private String hash(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception exception) {throw new IllegalStateException("PROVIDER_QA_HASH_FAILED");}
    }
    private final class Control implements AttachmentProviderQaCaseExecutor.ExecutionControl {
        private final CaseRow owner;
        private Control(CaseRow owner) {this.owner=owner;}
        @Override public boolean selectExecutionAllowed() {
            return !Thread.currentThread().isInterrupted() && Boolean.TRUE.equals(read.execute(tx->dao.selectExecutionAllowed(owner.caseId(),owner.leaseToken())));
        }
        @Override public AutoCloseable selectDownloadPermit(String hostHash) {
            if (hostHash==null || !hostHash.matches("[0-9a-f]{64}")) return null;
            var leases=write.execute(tx->{
                lock(owner.runId());var acquired=new ArrayList<Resource>();
                for(int slot=1;slot<=2;slot++) {
                    var global=new Resource(owner.caseId(),owner.leaseToken(),"DOWNLOAD","GLOBAL",slot,UUID.randomUUID());
                    if (dao.insertResource(global)==1) {acquired.add(global);break;}
                }
                if (acquired.isEmpty()) return null;
                var host=new Resource(owner.caseId(),owner.leaseToken(),"HOST",hostHash,1,UUID.randomUUID());
                if (dao.insertResource(host)!=1) {tx.setRollbackOnly();return null;}
                acquired.add(host);return List.copyOf(acquired);
            });
            return leases==null?null:permit(leases);
        }
        @Override public AutoCloseable selectExtractionPermit() {
            var lease=write.execute(tx->{
                lock(owner.runId());var selected=new Resource(owner.caseId(),owner.leaseToken(),"EXTRACTION","GLOBAL",1,UUID.randomUUID());
                return dao.insertResource(selected)==1?selected:null;
            });
            return lease==null?null:permit(List.of(lease));
        }
        private AutoCloseable permit(List<Resource> leases) {
            var closed=new AtomicBoolean();
            return ()->{if(closed.compareAndSet(false,true)) write.executeWithoutResult(tx->{lock(owner.runId());leases.forEach(dao::deleteResource);});};
        }
        @Override public boolean saveRequestReservation() {return reserve(1,0);}
        @Override public boolean saveByteReservation(long bytes) {return bytes>0 && bytes<=83886080 && reserve(0,bytes);}
        private boolean reserve(int requests,long bytes) {
            return Boolean.TRUE.equals(write.execute(tx->{lock(owner.runId());return dao.updateUsage(owner.caseId(),owner.leaseToken(),requests,bytes)==1;}));
        }
    }
}
