package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRoleDao;
import com.saneb.domain.announcementattachment.classification.AttachmentFileRoleRules;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentEvidenceService;
import com.saneb.domain.announcementattachment.vo.AttachmentEvidenceCommands;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 네트워크/추출을 실행하지 않고 완료된 근거만 짧은 source→job transaction에서 봉인합니다. */
@Service
public class AnnouncementAttachmentEvidenceServiceImpl implements AnnouncementAttachmentEvidenceService {
    private static final Set<String> QUALITIES = Set.of("COMPLETE_TEXT", "PARTIAL_TEXT", "OCR_REQUIRED", "ENCRYPTED",
            "CORRUPT", "UNSUPPORTED", "LIMIT_EXCEEDED", "TIMEOUT", "FAILED", "ISOLATION_UNAVAILABLE");
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentEvidenceDao evidence;
    private final AnnouncementAttachmentRetryDao retries;
    private final AnnouncementAttachmentRoleDao roles;
    private final ObjectMapper mapper;

    public AnnouncementAttachmentEvidenceServiceImpl(AnnouncementAttachmentJobDao jobs,
            AnnouncementAttachmentEvidenceDao evidence, AnnouncementAttachmentRetryDao retries,AnnouncementAttachmentRoleDao roles,ObjectMapper mapper) {
        this.jobs = jobs;
        this.evidence = evidence;
        this.retries=retries;this.roles=roles;
        this.mapper = mapper;
    }

    @Override @Transactional(readOnly=true,timeout=10)
    public Optional<AttachmentSetEvidence.File> selectFileCheckpoint(UUID jobId,UUID leaseToken,AttachmentSetEvidence.Locator locator) {
        if(jobId==null || leaseToken==null || locator==null) return Optional.empty();
        String json=evidence.selectFileCheckpoint(jobId,leaseToken,selectHash(selectJson(locator)));
        if(json==null) return Optional.empty();
        try {
            var file=mapper.readValue(json,AttachmentSetEvidence.File.class);
            if(file==null || !locator.equals(file.locator()) || !selectCheckpointEligible(file))
                throw conflict("저장된 재시도 근거가 현재 첨부 locator 또는 완전 추출 조건과 다릅니다.");
            return Optional.of(file);
        } catch(JsonProcessingException exception) { throw conflict("저장된 재시도 근거를 해석할 수 없습니다."); }
    }

    @Override @Transactional(timeout=10)
    public boolean saveFileCheckpoint(UUID jobId,UUID leaseToken,AttachmentSetEvidence.File file) {
        var hint=jobs.selectJobDetails(jobId);
        if(hint==null) return false;
        var source=jobs.selectSourceContextDetailsForUpdate(hint.sourceId());
        var job=jobs.selectOwnedJobDetailsForUpdate(jobId,leaseToken);
        if(job==null) return false;
        if(!selectCurrentInput(source,job)) { jobs.updateJobConflict(jobId,leaseToken); return false; }
        require(Set.of("COLLECT","RETRY_FILES").contains(job.operationCode()) && job.setId()==null,"봉인되지 않은 수집 작업만 성공 파일을 중간 저장할 수 있습니다.");
        require(selectCheckpointEligible(file),"재시도 중간 저장에는 완료 시각이 있는 완전 추출 성공 파일만 사용할 수 있습니다.");
        try {
            var execution=mapper.readValue(job.executionSnapshotJson(),AttachmentExecutionSnapshot.class);
            boolean retry="RETRY_FILES".equals(job.operationCode());
            validateResult(new AttachmentSetEvidence("FOUND",true,List.of(file)),execution,job.reservedDownloadBytes(),retry);
            if(retry) validateSelectedRetryFile(retries.selectRetryFileList(jobId,leaseToken),file);
        } catch(JsonProcessingException exception) { throw invalid("작업의 고정 실행 버전을 확인할 수 없습니다."); }
        String json=selectJson(file),locatorHash=selectHash(selectJson(file.locator()));
        require(json.getBytes(StandardCharsets.UTF_8).length<=16*1024*1024,"중간 추출 근거는 파일당 16 MiB 이하여야 합니다.");
        if(evidence.insertFileCheckpoint(jobId,leaseToken,locatorHash,json)==1) return true;
        String previous=evidence.selectFileCheckpoint(jobId,leaseToken,locatorHash);
        if(previous==null) return false;
        try {
            if(!mapper.readTree(previous).equals(mapper.readTree(json)))
                throw conflict("같은 작업의 성공 파일 근거가 이미 저장되어 있습니다. 기존 결과를 덮어쓸 수 없습니다.");
        } catch(JsonProcessingException exception) { throw conflict("저장된 성공 파일 근거를 확인할 수 없습니다."); }
        return true;
    }

    private boolean selectCheckpointEligible(AttachmentSetEvidence.File file) {
        return file!=null && "SUCCEEDED".equals(file.downloadStatus()) && file.failureCode()==null && file.extraction()!=null
                && "COMPLETE_TEXT".equals(file.extraction().quality()) && file.extraction().completedAtEpochMs()!=null;
    }

    @Override @Transactional
    public Optional<AttachmentSetRow> saveAttachmentSet(UUID jobId, UUID leaseToken, AttachmentSetEvidence result) {
        AttachmentJobRow hint = jobs.selectJobDetails(jobId);
        if (hint == null) return Optional.empty();
        AttachmentSourceContextRow source = jobs.selectSourceContextDetailsForUpdate(hint.sourceId());
        AttachmentJobRow job = jobs.selectOwnedJobDetailsForUpdate(jobId, leaseToken);
        if (job == null) return Optional.empty();
        require("COLLECT".equals(job.operationCode()),"새 전체 수집 저장 경로로 부분 재시도 또는 역할 변경 근거를 덮어쓸 수 없습니다.");
        if (!selectCurrentInput(source, job)) {
            jobs.updateJobConflict(jobId, leaseToken);
            return Optional.empty();
        }
        AttachmentExecutionSnapshot execution;
        try { execution = mapper.readValue(job.executionSnapshotJson(), AttachmentExecutionSnapshot.class); }
        catch (JsonProcessingException exception) { throw invalid("작업의 고정 실행 버전을 확인할 수 없습니다."); }
        validateResult(result, execution, job.reservedDownloadBytes());
        String manifest = selectManifest(result, execution, jobId);
        if (job.setId() != null) {
            AttachmentSetRow previous = evidence.selectSetDetails(job.sourceId(), job.setId());
            if (previous == null || !"SEALED".equals(previous.setStatus()) || !manifest.equals(previous.manifestHash()))
                throw conflict("이미 저장된 첨부 묶음과 결과가 다릅니다. 새 작업 세대로 수집해 주세요.");
            return Optional.of(previous);
        }
        UUID setId = selectEvidenceId(jobId, 0, "set");
        requireInserted(evidence.insertSet(new AttachmentEvidenceCommands.SetInsert(setId, job.sourceId(),
                job.contentVersionId(), job.policyId(), source.dataPurposeCode(), execution.profileHash())));
        for (int index = 0; index < result.files().size(); index++) {
            var file = result.files().get(index);
            UUID fileId = selectEvidenceId(jobId, index, "file");
            String locatorJson = selectJson(file.locator());
            requireInserted(evidence.insertFile(new AttachmentEvidenceCommands.FileInsert(fileId, setId, job.sourceId(),
                    selectHash(locatorJson), locatorJson, file.displayName(), file.detectedType(), file.role(), file.roleOrigin(),
                    file.downloadStatus(), file.downloadedBytes(), file.binaryHash(), index,
                    file.failureCode() == null ? null : file.failureCode().name(),
                    file.roleAssessment()==null?null:selectEvidenceId(jobId,index,"extraction"),
                    file.roleAssessment()==null?null:selectJson(file.roleAssessment()))));
            if (file.extraction() == null) continue;
            var extraction = file.extraction();
            String text = extraction.text() == null || extraction.text().isEmpty() ? null : extraction.text();
            requireInserted(evidence.insertExtraction(new AttachmentEvidenceCommands.ExtractionInsert(selectEvidenceId(jobId, index, "extraction"),
                    fileId, setId, job.sourceId(), job.attemptCount(), "ISOLATED_JAVA", execution.extractorVersion(),
                    execution.extractorConfigHash(), extraction.quality(), text, text == null ? null : selectHash(text),
                    selectJson(extraction.blocks()), text == null ? 0 : text.codePointCount(0, text.length()),
                    extraction.pageCount(), extraction.durationMs(),
                    "COMPLETE_TEXT".equals(extraction.quality()) ? null : extraction.quality(),
                    extraction.completedAtEpochMs()==null?null:java.time.OffsetDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(extraction.completedAtEpochMs()),java.time.ZoneOffset.UTC))));
        }
        requireInserted(evidence.updateSetSealed(setId, manifest, result.discoveryStatus(), result.discoveryComplete(), result.files().size(),
                selectJson(result.warningCodes())));
        // 마지막 fence가 만료되면 위 근거 쓰기까지 rollback한다. 현재 판정과 base는 여기서 변경하지 않는다.
        if (evidence.updateJobSet(jobId, leaseToken, setId) != 1) throw conflict("첨부 작업의 실행 권한이 만료되었습니다.");
        evidence.deleteFileCheckpoints(jobId);
        return Optional.of(evidence.selectSetDetails(job.sourceId(), setId));
    }

    @Override @Transactional(timeout=20)
    public Optional<AttachmentSetRow> saveRetriedAttachmentSet(UUID jobId,UUID leaseToken,AttachmentSetEvidence result) {
        var hint=jobs.selectJobDetails(jobId);if(hint==null) return Optional.empty();
        var source=jobs.selectSourceContextDetailsForUpdate(hint.sourceId());
        var job=jobs.selectOwnedJobDetailsForUpdate(jobId,leaseToken);if(job==null) return Optional.empty();
        require("RETRY_FILES".equals(job.operationCode()),"실패 파일 재시도 작업만 이 근거 저장 경로를 사용할 수 있습니다.");
        if(!selectCurrentInput(source,job)) { jobs.updateJobConflict(jobId,leaseToken);return Optional.empty(); }
        if(job.setId()!=null) throw conflict("재시도 근거는 이미 봉인됐습니다. 같은 작업의 저장된 판정을 재개하세요.");
        var original=evidence.selectSetDetails(job.sourceId(),job.referenceSetId());
        require(original!=null && "SEALED".equals(original.setStatus()),"재시도 원래 근거가 봉인되지 않았습니다.");
        var plan=retries.selectRetryFileList(jobId,leaseToken);
        require(plan!=null && !plan.isEmpty() && plan.size()<=10 && plan.size()==evidence.selectFileCount(job.sourceId(),job.referenceSetId()),
                "재시도의 고정 전체 파일 목록이 일치하지 않습니다.");
        require(result!=null && result.files().size()==plan.stream().filter(f->Boolean.TRUE.equals(f.selected())).count(),
                "선택한 실패 파일 전체의 결과를 저장해야 합니다.");
        AttachmentExecutionSnapshot execution;
        try { execution=mapper.readValue(job.executionSnapshotJson(),AttachmentExecutionSnapshot.class); }
        catch(JsonProcessingException exception) { throw invalid("재시도 고정 실행 버전을 해석할 수 없습니다."); }
        validateResult(result,execution,job.reservedDownloadBytes(),true);
        for(var file:result.files()) validateSelectedRetryFile(plan,file);
        var originals=roles.selectFileCopyList(job.sourceId(),job.referenceSetId());
        require(originals!=null && originals.size()==plan.size(),"보존할 원래 파일 전체의 근거가 필요합니다.");
        UUID setId=selectEvidenceId(jobId,0,"set");
        var selectedIndexes=result.files().stream().map(file->java.util.stream.IntStream.range(0,plan.size())
                .filter(index->plan.get(index).locatorHash().equals(selectHash(selectJson(file.locator())))).findFirst().orElseThrow()).toList();
        String manifest=selectHash(selectJson(List.of("attachment-file-retry-manifest-v1",job.referenceSetId(),original.manifestHash(),
                selectManifest(result,execution,jobId,selectedIndexes),plan,originals)));
        requireInserted(evidence.insertSet(new AttachmentEvidenceCommands.SetInsert(setId,job.sourceId(),job.contentVersionId(),job.policyId(),
                source.dataPurposeCode(),execution.profileHash())));
        for(int index=0;index<plan.size();index++) {
            var item=plan.get(index); UUID fileId=selectEvidenceId(jobId,index,"file"),extractionId=selectEvidenceId(jobId,index,"extraction");
            if(!Boolean.TRUE.equals(item.selected())) {
                var originalFile=originals.stream().filter(f->f.fileId().equals(item.fileId())).findFirst().orElseThrow(()->conflict("원래 파일 근거를 찾을 수 없습니다."));
                var copy=new com.saneb.domain.announcementattachment.vo.AttachmentRoleRows.Copy(job.sourceId(),job.referenceSetId(),item.fileId(),originalFile.extractionId(),
                        setId,fileId,originalFile.extractionId()==null?null:extractionId,item.role(),item.roleOrigin());
                requireInserted(roles.insertFileCopy(copy));
                if(copy.originalExtractionId()!=null) requireInserted(roles.insertExtractionCopy(copy));
                continue;
            }
            var file=result.files().stream().filter(f->item.locatorHash().equals(selectHash(selectJson(f.locator())))).findFirst()
                    .orElseThrow(()->conflict("선택한 실패 파일 결과가 누락됐습니다."));
            String locatorJson=selectJson(file.locator());
            requireInserted(evidence.insertFile(new AttachmentEvidenceCommands.FileInsert(fileId,setId,job.sourceId(),selectHash(locatorJson),locatorJson,
                    file.displayName(),file.detectedType(),file.role(),file.roleOrigin(),file.downloadStatus(),file.downloadedBytes(),file.binaryHash(),index,
                    file.failureCode()==null?null:file.failureCode().name(),file.roleAssessment()==null?null:extractionId,
                    file.roleAssessment()==null?null:selectJson(file.roleAssessment()))));
            if(file.extraction()!=null) {
                var x=file.extraction();String text=x.text()==null || x.text().isEmpty()?null:x.text();
                requireInserted(evidence.insertExtraction(new AttachmentEvidenceCommands.ExtractionInsert(extractionId,fileId,setId,job.sourceId(),job.attemptCount(),
                        "ISOLATED_JAVA",execution.extractorVersion(),execution.extractorConfigHash(),x.quality(),text,text==null?null:selectHash(text),
                        selectJson(x.blocks()),text==null?0:text.codePointCount(0,text.length()),x.pageCount(),x.durationMs(),
                        "COMPLETE_TEXT".equals(x.quality())?null:x.quality(),x.completedAtEpochMs()==null?null:java.time.OffsetDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(x.completedAtEpochMs()),java.time.ZoneOffset.UTC))));
            }
        }
        requireInserted(evidence.updateSetSealed(setId,manifest,result.discoveryStatus(),result.discoveryComplete(),plan.size(),selectJson(result.warningCodes())));
        if(evidence.updateJobSet(jobId,leaseToken,setId)!=1) throw conflict("재시도 작업의 lease가 만료됐습니다.");
        evidence.deleteFileCheckpoints(jobId);
        return Optional.of(evidence.selectSetDetails(job.sourceId(),setId));
    }
    private void validateSelectedRetryFile(List<com.saneb.domain.announcementattachment.vo.AttachmentRetryFileRow> plan,AttachmentSetEvidence.File file) {
        require(plan!=null && file!=null,"재시도의 고정 파일 범위가 필요합니다.");
        String hash=selectHash(selectJson(file.locator()));
        var selected=plan.stream().filter(f->Boolean.TRUE.equals(f.selected()) && hash.equals(f.locatorHash())).findFirst()
                .orElseThrow(()->conflict("승인된 재시도 파일 범위 밖의 근거는 저장할 수 없습니다."));
        boolean automaticUnknown="UNKNOWN".equals(selected.role()) && "UNKNOWN".equals(selected.roleOrigin())
                && "TEXT_RULE".equals(file.roleOrigin()) && file.roleAssessment()!=null;
        require(automaticUnknown || (selected.role().equals(file.role()) && selected.roleOrigin().equals(file.roleOrigin())),
                "재시도는 수동·프로필 지정 역할을 바꾸지 않습니다. 미확인 역할만 고정한 텍스트 규칙으로 판정할 수 있습니다.");
    }

    private boolean selectCurrentInput(AttachmentSourceContextRow source, AttachmentJobRow job) {
        return source != null && "PRODUCTION".equals(source.dataPurposeCode())
                && !"EXCLUDED".equals(source.semanticStatusCode())
                && Objects.equals(source.baseEvaluationId(), job.baseEvaluationId())
                && Objects.equals(source.contentVersionId(), job.contentVersionId())
                && Objects.equals(source.ruleReleaseId(), job.ruleReleaseId())
                && Objects.equals(source.sourceVersion(), job.expectedSourceVersion())
                && Objects.equals(source.attachmentVersion(), job.expectedAttachmentVersion());
    }

    private void validateResult(AttachmentSetEvidence result, AttachmentExecutionSnapshot execution, long chargedBytes) {
        validateResult(result,execution,chargedBytes,false);
    }
    private void validateResult(AttachmentSetEvidence result, AttachmentExecutionSnapshot execution, long chargedBytes,boolean preserveManualRole) {
        require(result != null && selectAllowed(result.discoveryStatus(), "FOUND", "NO_FILES", "FAILED", "PROFILE_REQUIRED", "LIMIT_EXCEEDED"),
                "첨부 발견 결과에 최종 상태가 필요합니다.");
        require(result.files().size() <= 10, "공고별 첨부 근거는 최대 10개입니다. 초과분은 제한 상태로 표시해야 합니다.");
        require(result.warningCodes().size()<=20 && result.warningCodes().stream().allMatch(this::selectAllowedWarning),
                "발견 경고에는 외부 원문 없이 정의된 코드만 최대 20개 저장할 수 있습니다.");
        if ("NO_FILES".equals(result.discoveryStatus())) require(result.discoveryComplete() && result.files().isEmpty(),
                "첨부 없음은 첨부 영역을 정상 확인하고 파일이 0개일 때만 사용할 수 있습니다.");
        if ("FOUND".equals(result.discoveryStatus())) require(!result.files().isEmpty(), "첨부 발견 상태에는 파일 근거가 필요합니다.");
        if (selectAllowed(result.discoveryStatus(), "FAILED", "PROFILE_REQUIRED", "LIMIT_EXCEEDED"))
            require(!result.discoveryComplete(), "발견 실패 또는 제한 상태를 발견 완료로 표시할 수 없습니다.");
        Set<String> locators = new HashSet<>();
        long received = 0;
        for (var file : result.files()) {
            require(file != null && file.locator() != null, "파일의 안전한 locator가 필요합니다.");
            validateLocator(file.locator(), execution.profileCode());
            require(locators.add(selectHash(selectJson(file.locator()))), "같은 첨부 locator가 중복되었습니다.");
            require(file.displayName() == null || (file.displayName().length() <= 500 && file.displayName().codePoints().noneMatch(Character::isISOControl)),
                    "파일 표시명은 제어문자 없이 500자 이하여야 합니다.");
            require(file.detectedType() == null || selectAllowed(file.detectedType(), "PDF", "HWP", "HWPX"), "지원하지 않는 검출 파일 형식입니다.");
            require(selectAllowed(file.role(), "NOTICE", "GUIDE", "FORM", "REFERENCE", "UNKNOWN"), "지원하지 않는 문서 역할입니다.");
            require(selectAllowed(file.roleOrigin(), "UNKNOWN", "PROFILE", "TEXT_RULE") || (preserveManualRole && "MANUAL".equals(file.roleOrigin())),
                    "최초 수집 결과에 관리자 수동 역할을 삽입할 수 없습니다.");
            require(selectAllowed(file.downloadStatus(), "SUCCEEDED", "FAILED", "BLOCKED", "CANCELLED"), "처리 중인 파일을 봉인할 수 없습니다.");
            require(file.downloadedBytes() >= 0 && file.downloadedBytes() <= 20L * 1024 * 1024, "파일 수신량은 20 MiB 이하여야 합니다.");
            require(file.binaryHash() == null || file.binaryHash().matches("[0-9a-f]{64}"), "파일 SHA-256 형식이 올바르지 않습니다.");
            received += file.downloadedBytes();
            if ("SUCCEEDED".equals(file.downloadStatus())) {
                require(file.downloadedBytes() > 0 && file.binaryHash() != null && file.extraction() != null && file.failureCode() == null,
                        "다운로드 성공에는 실제 수신량·파일 hash·추출 성공 또는 실패 근거가 필요합니다.");
            } else require(file.extraction() == null && file.failureCode() != null, "다운로드 실패에는 고정 실패 코드가 필요하며 추출 성공 근거를 연결할 수 없습니다.");
            if (file.extraction() != null) validateExtraction(file.extraction());
            require(AttachmentFileRoleRules.selectAssessmentValid(file,execution),
                    "문서 역할에는 현재 고정 규칙과 같은 완전 추출의 실제 위치 근거가 필요합니다. 이전 규칙이나 다른 파일의 결과를 재사용할 수 없습니다.");
        }
        require(received <= chargedBytes, "수신량이 작업에 예약된 다운로드 예산을 초과했습니다.");
    }

    private void validateLocator(AttachmentSetEvidence.Locator locator, String profileCode) {
        require(profileCode.equals(locator.profileCode()), "파일 locator의 시스템 프로필이 작업과 다릅니다.");
        String path = locator.path();
        require(path != null && path.length() <= 1000 && path.matches("/[A-Za-z0-9_./~-]*")
                && !path.startsWith("//") && !path.contains("/../") && !path.endsWith("/..")
                && URI.create(path).normalize().getPath().equals(path), "파일 locator에는 검증된 상대 경로만 저장할 수 있습니다.");
        require(locator.identifiers().size() <= 5, "파일 locator 식별자 수가 한도를 초과했습니다.");
        for (var entry : locator.identifiers().entrySet()) require(
                selectAllowed(entry.getKey(), "fileId", "fileSn", "noticeId", "attachmentId", "boardId")
                        && entry.getValue() != null && entry.getValue().matches("[A-Za-z0-9_.-]{1,200}"),
                "파일 locator에는 허용된 비기밀 식별자만 저장할 수 있습니다.");
    }

    private void validateExtraction(AttachmentSetEvidence.Extraction extraction) {
        require(extraction.completedAtEpochMs()==null || (extraction.completedAtEpochMs()>0
                && extraction.completedAtEpochMs()<=System.currentTimeMillis()+5000),"추출 완료 시각을 미래 시각으로 저장할 수 없습니다.");
        require(extraction.quality() != null && QUALITIES.contains(extraction.quality()), "추출 결과의 품질 코드가 올바르지 않습니다.");
        require(extraction.durationMs() >= 0 && extraction.durationMs() <= 35000, "추출 소요시간은 정리 시간을 포함해 35초 이하여야 합니다.");
        require(extraction.pageCount() == null || (extraction.pageCount() >= 0 && extraction.pageCount() <= 200), "실제 확인한 페이지 수는 최대 200입니다.");
        String text = extraction.text();
        int count = text == null ? 0 : text.codePointCount(0, text.length());
        require(count <= 1_000_000 && (text == null || text.indexOf('\0') < 0), "추출 텍스트는 NUL 없이 100만자 이하여야 합니다.");
        require(extraction.blocks().size() <= 20000, "첨부 근거 block 수가 한도를 초과했습니다.");
        int previousEnd = 0, index = 0;
        Set<String> scopes = new HashSet<>();
        for (var block : extraction.blocks()) {
            require(block != null && block.index() == index++ && block.startOffset() >= previousEnd
                    && block.endOffset() > block.startOffset() && block.endOffset() <= count,
                    "추출 근거의 순서 또는 code point 위치가 올바르지 않습니다.");
            require(block.evidenceScopeId() != null && block.evidenceScopeId().length() <= 300
                    && !block.evidenceScopeId().isBlank() && scopes.add(block.evidenceScopeId())
                    && block.locator() != null && !block.locator().isBlank() && block.locator().length() <= 300,
                    "추출 근거에는 중복되지 않은 범위와 확인 가능한 위치가 필요합니다.");
            previousEnd = block.endOffset();
        }
        if ("COMPLETE_TEXT".equals(extraction.quality())) require(text != null && !text.isBlank() && !extraction.blocks().isEmpty(),
                "완전 추출에는 비어 있지 않은 본문과 위치 근거가 필요합니다.");
    }

    private String selectManifest(AttachmentSetEvidence result, AttachmentExecutionSnapshot execution, UUID jobId) {
        return selectManifest(result,execution,jobId,null);
    }
    private String selectManifest(AttachmentSetEvidence result, AttachmentExecutionSnapshot execution, UUID jobId,List<Integer> selectedIndexes) {
        List<Object> files = new ArrayList<>();
        for (int index = 0; index < result.files().size(); index++) {
            var file = result.files().get(index);
            var extraction = file.extraction();
            // 같은 작업 재저장은 같은 선택 근거 ID를 사용한다. 새 세대는 과거 추출 입력과 분리한다.
            // lease·소요시간·임시 URL·표시명은 입력 hash에 포함하지 않는다.
            int actualIndex=selectedIndexes==null?index:selectedIndexes.get(index);
            files.add(new ManifestFile(selectEvidenceId(jobId, actualIndex, "file"),
                    extraction == null ? null : selectEvidenceId(jobId, actualIndex, "extraction"),
                    selectHash(selectJson(file.locator())), file.role(), file.roleOrigin(), file.downloadStatus(),
                    file.downloadedBytes(), file.binaryHash(), file.detectedType(), file.failureCode() == null ? null : file.failureCode().name(),
                    extraction == null ? null : extraction.quality(),
                    extraction == null || extraction.text() == null || extraction.text().isEmpty() ? null : selectHash(extraction.text()),
                    extraction == null ? null : selectHash(selectJson(extraction.blocks())), extraction == null ? null : extraction.pageCount(),file.roleAssessment()));
        }
        return selectHash(selectJson(new Manifest(execution.roleRuleVersion()==null?1:2, selectEvidenceId(jobId, 0, "set"), execution,
                result.discoveryStatus(), result.discoveryComplete(), result.warningCodes(), files)));
    }

    private UUID selectEvidenceId(UUID jobId, int index, String kind) {
        return UUID.nameUUIDFromBytes(("saneb-attachment-evidence-v1:" + jobId + ":" + index + ":" + kind)
                .getBytes(StandardCharsets.UTF_8));
    }
    private record Manifest(int schemaVersion, UUID selectedSetId, AttachmentExecutionSnapshot execution,
                            String discovery, boolean complete, List<String> warnings, List<Object> files) { }
    private record ManifestFile(UUID fileId, UUID extractionId, String locatorHash, String role, String roleOrigin, String download, long bytes,
            String binaryHash, String format, String error, String quality, String textHash, String blocksHash, Integer pages,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment roleAssessment) { }
    private boolean selectAllowed(String value, String... allowed) { return value != null && List.of(allowed).contains(value); }
    private boolean selectAllowedWarning(String code) {
        if (Set.of("ATTACHMENT_DETAIL_UNAVAILABLE", "ATTACHMENT_SELECTOR_CHANGED", "ATTACHMENT_DOWNLOAD_FORM_CHANGED",
                "ATTACHMENT_LINK_UNRESOLVED", "ATTACHMENT_FILE_LIMIT").contains(code)) return true;
        try { com.saneb.domain.announcementattachment.vo.AttachmentFailureCode.valueOf(code); return true; }
        catch (IllegalArgumentException exception) { return false; }
    }
    private void require(boolean condition, String message) { if (!condition) throw invalid(message); }
    private void requireInserted(int count) { if (count != 1) throw conflict("첨부 근거를 정확히 한 건 저장하지 못했습니다."); }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message); }
    private ApiException conflict(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT, HttpStatus.CONFLICT, message); }
    private String selectJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw invalid("첨부 근거를 직렬화할 수 없습니다."); }
    }
    private String selectHash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
}
