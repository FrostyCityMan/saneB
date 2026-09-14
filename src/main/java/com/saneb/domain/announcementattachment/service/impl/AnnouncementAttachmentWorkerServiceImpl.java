package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.extraction.AttachmentTemporaryStorage;
import com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentEvidenceService;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentEvaluationService;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentJobService;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRetryService;
import com.saneb.domain.announcementattachment.vo.AttachmentRetryFileRow;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentWorkerService;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentFailureCode;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.worker.AttachmentDownloadGateway;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** DB service의 짧은 transaction 사이에서만 HTTP/격리 프로세스를 실행한다. */
@Service
public class AnnouncementAttachmentWorkerServiceImpl implements AnnouncementAttachmentWorkerService {
    private final AnnouncementAttachmentJobService jobs;
    private final AnnouncementAttachmentEvidenceService evidence;
    private final AnnouncementAttachmentEvaluationService evaluations;
    private final AnnouncementAttachmentRetryService retries;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final AttachmentRuntimeIdentity runtime;
    private final AttachmentTemporaryStorage temporary;
    private final AttachmentDownloadGateway downloads;
    private final AttachmentFileTypeValidator types;
    private final IsolatedAttachmentExtractor extractor;
    private final ObjectMapper mapper;

    public AnnouncementAttachmentWorkerServiceImpl(AnnouncementAttachmentJobService jobs, AnnouncementAttachmentEvidenceService evidence,
            AnnouncementAttachmentEvaluationService evaluations, AnnouncementAttachmentRetryService retries,AttachmentDiscoveryProfileRegistry profiles, AttachmentRuntimeIdentity runtime,
            AttachmentTemporaryStorage temporary, AttachmentDownloadGateway downloads, AttachmentFileTypeValidator types,
            IsolatedAttachmentExtractor extractor, ObjectMapper mapper) {
        this.jobs=jobs; this.evidence=evidence; this.evaluations=evaluations; this.profiles=profiles; this.runtime=runtime;
        this.retries=retries;
        this.temporary=temporary; this.downloads=downloads; this.types=types; this.extractor=extractor; this.mapper=mapper;
    }

    @Override @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Outcome saveNextAttachmentJob() {
        // 임시 저장 장애는 HTTP/추출만 막는다. 이미 봉인된 근거의 역할 재판정/복구는 DB에서 계속한다.
        boolean temporaryAvailable;
        try { temporary.deleteExpiredWorkspaces(); temporaryAvailable=temporary.selectWorkspaceAvailable(); }
        catch (IOException exception) { temporaryAvailable=false; }
        var claimed = jobs.saveNextJobClaim();
        if (claimed.isEmpty()) return new Outcome(null,temporaryAvailable?"IDLE":"TEMPORARY_UNAVAILABLE");
        AttachmentJobRow job = claimed.get();
        boolean[] requestStarted = {false};
        try {
            // 봉인 직후 재시작한 작업은 외부 재수집 없이 같은 근거에서 판정만 재개한다.
            if (job.setId() != null) return saveEvaluation(job);
            if (!temporaryAvailable) {
                jobs.saveJobDeferred(job.jobId(),job.leaseToken(),false);
                return new Outcome(job.jobId(),"TEMPORARY_UNAVAILABLE");
            }
            boolean fileRetry="RETRY_FILES".equals(job.operationCode());
            if (!fileRetry && !"COLLECT".equals(job.operationCode())) return saveFailure(job,AttachmentFailureCode.PROFILE_REQUIRED);
            AttachmentExecutionSnapshot execution = mapper.readValue(job.executionSnapshotJson(), AttachmentExecutionSnapshot.class);
            AttachmentRuntimeIdentity.Identity installed;
            try { installed=runtime.selectIdentity(); }
            catch (IOException exception) { return saveFailure(job,AttachmentFailureCode.ISOLATION_UNAVAILABLE); }
            if (!AnnouncementAttachmentClassificationEngine.VERSION.equals(execution.engineVersion())
                    || !installed.extractorVersion().equals(execution.extractorVersion())
                    || !installed.configHash().equals(execution.extractorConfigHash()))
                return saveFailure(job, AttachmentFailureCode.PROFILE_REQUIRED);
            var source = jobs.selectWorkerSourceDetails(job.jobId(), job.leaseToken()).orElseThrow(AttachmentDownloadGateway.Deferred::new);
            var profile = profiles.selectProfileDetails(source.providerCode(),execution.profileCode(),execution.profileHash()).orElse(null);
            if (profile == null) return saveFailure(job, AttachmentFailureCode.PROFILE_REQUIRED);
            var retryFiles=fileRetry?retries.selectRetryFileList(job.jobId(),job.leaseToken()):List.<AttachmentRetryFileRow>of();
            if(fileRetry && (retryFiles.isEmpty() || retryFiles.size()>10 || retryFiles.stream().noneMatch(f->Boolean.TRUE.equals(f.selected()))))
                return saveFailure(job,AttachmentFailureCode.PROFILE_REQUIRED);
            var detail = profile.selectDetailUri(source.selectDiscoverySource());
            AttachmentSetEvidence result;
            AttachmentFailureCode retry = null;
            try (var workspace = temporary.insertWorkspace(job.jobId(),job.leaseToken())) {
                AttachmentDiscoveryProfile.Result discovery;
                try {
                    validateExecution(job);
                    var response = downloads.selectDownload(job,profile,AttachmentPinnedDownloadClient.Request.selectGet(detail),
                            workspace.selectDetailPath(),1024L*1024,() -> requestStarted[0]=true);
                    String contentType = response.contentType() == null ? "" : response.contentType().toLowerCase(java.util.Locale.ROOT);
                    if (!contentType.isBlank() && !contentType.startsWith("text/html") && !contentType.startsWith("application/xhtml+xml"))
                        throw new IOException("ATTACHMENT_DETAIL_CONTENT_TYPE");
                    String html;
                    try (var stream = Files.newInputStream(workspace.selectDetailPath())) {
                        html = Jsoup.parse(stream,null,detail.toASCIIString()).outerHtml();
                    }
                    discovery = profile.selectDescriptors(source.selectDiscoverySource(),html);
                } catch (IOException exception) {
                    AttachmentFailureCode code = selectFailureCode(exception);
                    if (code.retryable() && job.attemptCount()<3) retry = code;
                    discovery = new AttachmentDiscoveryProfile.Result("FAILED",false,List.of(),List.of(code.name()));
                } finally { Files.deleteIfExists(workspace.selectDetailPath()); }
                var files = new ArrayList<AttachmentSetEvidence.File>();
                var descriptors=new java.util.LinkedHashMap<String,AttachmentDiscoveryProfile.Descriptor>();
                for(var descriptor:discovery.descriptors()) descriptors.put(selectLocatorHash(descriptor.locator()),descriptor);
                boolean retryScopeValid=!fileRetry || (discovery.complete() && "FOUND".equals(discovery.status())
                        && descriptors.size()==retryFiles.size() && descriptors.size()==discovery.descriptors().size()
                        && descriptors.keySet().equals(retryFiles.stream().map(AttachmentRetryFileRow::locatorHash).collect(java.util.stream.Collectors.toSet())));
                if(!retryScopeValid) {
                    var code="FOUND".equals(discovery.status()) || "NO_FILES".equals(discovery.status())
                            ?AttachmentFailureCode.DISCOVERY_CHANGED:AttachmentFailureCode.DISCOVERY_FAILED;
                    for(var fixed:retryFiles) if(Boolean.TRUE.equals(fixed.selected())) files.add(new AttachmentSetEvidence.File(
                            mapper.readValue(fixed.locatorJson(),AttachmentSetEvidence.Locator.class),fixed.displayName(),null,fixed.role(),fixed.roleOrigin(),
                            "FAILED",0,null,code,null));
                    var warnings=new ArrayList<>(discovery.warnings());if(!warnings.contains(code.name())) warnings.add(code.name());
                    discovery=new AttachmentDiscoveryProfile.Result("FAILED",false,List.of(),warnings.stream().limit(20).toList());
                }
                var toProcess=fileRetry?retryFiles.stream().filter(f->Boolean.TRUE.equals(f.selected())).filter(f->retryScopeValid)
                        .map(f->descriptors.get(f.locatorHash())).toList():discovery.descriptors();
                for (var descriptor : toProcess) {
                    validateExecution(job);
                    var fixed=fileRetry?retryFiles.stream().filter(f->f.locatorHash().equals(selectLocatorHash(descriptor.locator()))).findFirst().orElseThrow():null;
                    var checkpoint=descriptor.downloadAllowed()
                            ? evidence.selectFileCheckpoint(job.jobId(),job.leaseToken(),descriptor.locator())
                            : java.util.Optional.<AttachmentSetEvidence.File>empty();
                    if(checkpoint.isPresent()) {
                        var saved=checkpoint.get();
                        if(!(fixed==null?descriptor.documentRole():fixed.role()).equals(saved.role())
                                || (fixed!=null && !fixed.roleOrigin().equals(saved.roleOrigin()))
                                || (descriptor.expectedFormat()!=null && !descriptor.expectedFormat().equals(saved.detectedType())))
                            throw new IllegalArgumentException("ATTACHMENT_CHECKPOINT_DESCRIPTOR_CHANGED");
                        files.add(saved);
                        continue;
                    }
                    var file = selectFile(job,profile,descriptor,workspace,requestStarted);
                    if(fixed!=null) file=new AttachmentSetEvidence.File(file.locator(),file.displayName(),file.detectedType(),fixed.role(),fixed.roleOrigin(),
                            file.downloadStatus(),file.downloadedBytes(),file.binaryHash(),file.failureCode(),file.extraction());
                    // selectFile의 finally에서 원본 삭제 후 저장한다. 재시도/재시작에도 성공 근거와 최초 추출 시각은 보존한다.
                    if(file.extraction()!=null && "COMPLETE_TEXT".equals(file.extraction().quality())
                            && !evidence.saveFileCheckpoint(job.jobId(),job.leaseToken(),file))
                        throw new AttachmentDownloadGateway.Deferred();
                    files.add(file);
                    if (file.failureCode()!=null && file.failureCode().retryable() && job.attemptCount()<3) retry=file.failureCode();
                }
                result = new AttachmentSetEvidence(discovery.status(),discovery.complete(),files,discovery.warnings());
            }
            // 원본 정리 이후에만 DB 근거/최신 판정을 공개한다. 재시도 예산은 환급하지 않는다.
            if (retry != null) return saveFailure(job,retry);
            if (!jobs.saveJobHeartbeat(job.jobId(),job.leaseToken())) return new Outcome(job.jobId(),"LEASE_LOST");
            var stored=fileRetry?evidence.saveRetriedAttachmentSet(job.jobId(),job.leaseToken(),result):evidence.saveAttachmentSet(job.jobId(),job.leaseToken(),result);
            if (stored.isEmpty()) return new Outcome(job.jobId(),"CONFLICT");
            return saveEvaluation(job);
        } catch (AttachmentDownloadGateway.Deferred exception) {
            jobs.saveJobDeferred(job.jobId(),job.leaseToken(),requestStarted[0]);
            return new Outcome(job.jobId(),"DEFERRED");
        } catch (IOException exception) {
            if (exception.getMessage()!=null && exception.getMessage().startsWith("TEMPORARY_")) {
                jobs.saveJobDeferred(job.jobId(),job.leaseToken(),requestStarted[0]);
                return new Outcome(job.jobId(),"TEMPORARY_UNAVAILABLE");
            }
            return saveFailure(job,"ISOLATION_UNAVAILABLE".equals(exception.getMessage())
                    ? AttachmentFailureCode.ISOLATION_UNAVAILABLE : selectFailureCode(exception));
        } catch (IllegalArgumentException exception) { return saveFailure(job,AttachmentFailureCode.PROFILE_REQUIRED); }
        catch (RuntimeException exception) {
            // 예외 원문에는 URL·문서·환경값이 들어갈 수 있다. 고정 코드만 저장한다.
            return saveFailure(job,AttachmentFailureCode.WORKER_PROCESSING_FAILED);
        }
    }

    private AttachmentSetEvidence.File selectFile(AttachmentJobRow job, AttachmentDiscoveryProfile profile,
            AttachmentDiscoveryProfile.Descriptor descriptor, AttachmentTemporaryStorage.Workspace workspace, boolean[] requestStarted) throws IOException {
        String origin = "UNKNOWN".equals(descriptor.documentRole()) ? "UNKNOWN" : "PROFILE";
        if (!descriptor.downloadAllowed()) return new AttachmentSetEvidence.File(descriptor.locator(),descriptor.displayName(),null,
                descriptor.documentRole(),origin,"BLOCKED",0,null,AttachmentFailureCode.UNSUPPORTED_FORMAT,null);
        AttachmentPinnedDownloadClient.Download downloaded = null;
        try {
            downloaded = downloads.selectDownload(job,profile,descriptor.selectRequest(),workspace.selectBinaryPath(),20L*1024*1024,
                    () -> requestStarted[0]=true);
            String format = types.selectFormat(workspace.selectBinaryPath(),downloaded,descriptor.expectedFormat(),
                    profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes());
            validateExecution(job);
            var lease = jobs.saveExtractionLease(job.jobId(),job.leaseToken()).orElseThrow(AttachmentDownloadGateway.Deferred::new);
            AttachmentSetEvidence.Extraction extracted;
            long started = System.nanoTime();
            try {
                JsonNode output;
                try { output = extractor.selectExtraction(workspace.selectBinaryPath()); }
                catch (IOException exception) { output=mapper.createObjectNode().put("qualityCode","FAILED"); }
                long duration = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
                if (duration>35000) throw new IOException("ATTACHMENT_EXTRACTION_TIMEOUT");
                extracted = selectExtraction(output,format,(int)duration);
            } finally { jobs.deleteResourceLease(lease); }
            return new AttachmentSetEvidence.File(descriptor.locator(),descriptor.displayName(),"UNSUPPORTED".equals(extracted.quality()) ? null : format,descriptor.documentRole(),origin,
                    "SUCCEEDED",downloaded.bytes(),downloaded.sha256(),null,extracted);
        } catch (IOException exception) {
            var code = selectFailureCode(exception);
            return new AttachmentSetEvidence.File(descriptor.locator(),descriptor.displayName(),null,descriptor.documentRole(),origin,
                    code==AttachmentFailureCode.DOWNLOAD_BLOCKED || code==AttachmentFailureCode.UNSUPPORTED_FORMAT ? "BLOCKED" : "FAILED",
                    downloaded==null ? 0 : downloaded.bytes(),downloaded==null ? null : downloaded.sha256(),code,null);
        } finally { Files.deleteIfExists(workspace.selectBinaryPath()); }
    }

    private AttachmentSetEvidence.Extraction selectExtraction(JsonNode result,String expected,int duration) {
        String quality=result.path("qualityCode").asText("FAILED");
        if (!Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED",
                "LIMIT_EXCEEDED","TIMEOUT","FAILED","ISOLATION_UNAVAILABLE").contains(quality)) quality="FAILED";
        if (Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED").contains(quality)
                && !expected.equals(result.path("format").asText())) quality="UNSUPPORTED";
        boolean textAllowed=Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(quality);
        String text=textAllowed ? result.path("text").asText("") : null;
        var blocks=new ArrayList<AttachmentSetEvidence.Block>();
        if (textAllowed) for (JsonNode block:result.path("blocks")) blocks.add(new AttachmentSetEvidence.Block(
                block.path("index").asInt(),block.path("startOffset").asInt(),block.path("endOffset").asInt(),
                block.path("evidenceScopeId").asText(),block.path("scopeReliable").asBoolean(),block.path("locator").asText()));
        Integer pages=result.path("pageCount").isIntegralNumber() ? result.path("pageCount").intValue() : null;
        if (pages!=null && (pages<0 || pages>200)) return new AttachmentSetEvidence.Extraction("LIMIT_EXCEEDED",null,List.of(),null,duration);
        return new AttachmentSetEvidence.Extraction(quality,text,blocks,pages,duration,System.currentTimeMillis());
    }
    private void validateExecution(AttachmentJobRow job) {
        if (!jobs.saveJobHeartbeat(job.jobId(),job.leaseToken()) || !jobs.selectExternalExecutionAllowed(job.jobId(),job.leaseToken()))
            throw new AttachmentDownloadGateway.Deferred();
    }
    private String selectLocatorHash(AttachmentSetEvidence.Locator locator) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(mapper.writeValueAsBytes(locator))); }
        catch(java.security.NoSuchAlgorithmException|com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("ATTACHMENT_LOCATOR_INVALID");
        }
    }
    private Outcome saveEvaluation(AttachmentJobRow job) {
        return evaluations.saveJobEvaluation(job.jobId(),job.leaseToken())
                .map(result -> new Outcome(job.jobId(),"EVALUATED")).orElseGet(() -> new Outcome(job.jobId(),"CONFLICT"));
    }
    private Outcome saveFailure(AttachmentJobRow job,AttachmentFailureCode code) {
        return new Outcome(job.jobId(),jobs.saveJobFailure(job.jobId(),job.leaseToken(),code) ? code.name() : "LEASE_LOST");
    }
    private AttachmentFailureCode selectFailureCode(IOException exception) {
        String code=exception.getMessage()==null ? "" : exception.getMessage();
        if (code.equals("ATTACHMENT_EXTRACTION_TIMEOUT")) return AttachmentFailureCode.EXTRACTION_FAILED;
        if (code.startsWith("ATTACHMENT_DETAIL_")) return AttachmentFailureCode.DISCOVERY_FAILED;
        if (code.equals("ATTACHMENT_HTTP_429")) return AttachmentFailureCode.HTTP_RATE_LIMITED;
        if (code.matches("ATTACHMENT_HTTP_4[0-9]{2}")) return AttachmentFailureCode.DOWNLOAD_BLOCKED;
        if (code.matches("ATTACHMENT_HTTP_5[0-9]{2}")) return AttachmentFailureCode.HTTP_SERVER_ERROR;
        if (code.contains("TIMEOUT") || exception instanceof java.net.SocketTimeoutException) return AttachmentFailureCode.NETWORK_TIMEOUT;
        if (code.contains("LIMIT")) return AttachmentFailureCode.LIMIT_EXCEEDED;
        if (code.contains("SIGNATURE_UNSUPPORTED")) return AttachmentFailureCode.UNSUPPORTED_FORMAT;
        if (code.contains("MISMATCH") || code.contains("BLOCKED") || code.contains("NOT_APPROVED")
                || code.contains("DISPOSITION") || code.contains("REDIRECT")) return AttachmentFailureCode.DOWNLOAD_BLOCKED;
        return AttachmentFailureCode.NETWORK_UNAVAILABLE;
    }
}
