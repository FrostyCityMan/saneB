package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeGate;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** PASSED 문자열만 승인하지 않는다. 현재 입력/정답/fixture 및 실제 추가 QA 검증기의 결과를 요구한다. */
@Component
public final class AttachmentPolicyPublicationQaVerifier {
    private static final Set<String> REQUIRED=Set.of("CLASSIFICATION_GOLDEN","INSTALLED_RUNTIME","PROVIDER_PROFILES","WORKER_DB_RECOVERY");
    private final AttachmentPolicyValidationSnapshotFactory snapshots;
    private final AnnouncementAttachmentPolicyGoldenGate golden;
    private final AttachmentRuntimeGate runtime;
    private final Map<String,AttachmentPolicyAdditionalQaEvidenceVerifier> additional;
    private final ObjectMapper mapper;
    public AttachmentPolicyPublicationQaVerifier(AttachmentPolicyValidationSnapshotFactory snapshots,AnnouncementAttachmentPolicyGoldenGate golden,
            AttachmentRuntimeGate runtime,List<AttachmentPolicyAdditionalQaEvidenceVerifier> additional,ObjectMapper mapper){
        this.snapshots=snapshots;this.golden=golden;this.runtime=runtime;
        this.mapper=mapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var handlers=new HashMap<String,AttachmentPolicyAdditionalQaEvidenceVerifier>();
        for(var handler:additional){var code=handler.selectStepCode();
            if(code==null || !Set.of("PROVIDER_PROFILES","WORKER_DB_RECOVERY").contains(code) || handlers.putIfAbsent(code,handler)!=null)
                throw new IllegalStateException("추가 정책 QA 검증기의 코드가 중복되었거나 유효하지 않습니다.");
        }
        this.additional=Map.copyOf(handlers);
    }
    public String selectValidatedEvidenceHash(Run run,List<Step> steps,AttachmentPolicyValidationSnapshotFactory.Frozen frozen){
        if(run==null || !"VERIFIED".equals(run.statusCode()) || run.completedAt()==null || run.startedAt()==null
                || run.completedAt().isBefore(run.startedAt()) || !frozen.hash().equals(run.snapshotHash())
                || steps==null || steps.size()!=4)throw conflict("현재 입력의 전체 QA가 VERIFIED가 아닙니다. 누락·실패 단계를 실제 실행한 뒤 다시 게시하세요.");
        try {
            if(run.inputSnapshotJson()==null || run.inputSnapshotJson().length()>2097152
                    || !mapper.readTree(run.inputSnapshotJson()).equals(mapper.readTree(frozen.json())))throw conflict("QA의 고정 입력과 현재 게시 입력이 다릅니다.");
            var hashes=new TreeMap<String,String>();
            for(var step:steps){
                if(step==null || !run.runId().equals(step.runId()) || step.stepCode()==null || !REQUIRED.contains(step.stepCode())
                        || !"PASSED".equals(step.statusCode()) || hashes.containsKey(step.stepCode()) || step.evidenceJson()==null
                        || step.evidenceJson().length()>32768 || step.createdAt()==null || step.createdAt().isBefore(run.startedAt())
                        || step.createdAt().isAfter(run.completedAt()))throw conflict("QA 단계의 소속·상태·시각·증거가 유효하지 않습니다.");
                String calculated;
                if("CLASSIFICATION_GOLDEN".equals(step.stepCode())){
                    var saved=mapper.readValue(step.evidenceJson(),AnnouncementAttachmentPolicyGoldenGate.Result.class);
                    var expected=golden.selectValidatedResult(frozen.rule().ruleSet(),frozen.rule().calculatedSnapshotHash());
                    if(!expected.equals(saved))throw conflict("현재 규칙의 분류 정답 결과와 저장된 QA 근거가 다릅니다.");
                    calculated=snapshots.hash(saved);
                } else if("INSTALLED_RUNTIME".equals(step.stepCode())){
                    var saved=mapper.readValue(step.evidenceJson(),AttachmentRuntimeGate.Result.class);
                    runtime.validateStoredResult(saved,frozen.runtime().runtimeHash());
                    if(saved.startedAt().isBefore(run.startedAt().toInstant()) || saved.completedAt().isAfter(step.createdAt().toInstant()))
                        throw conflict("설치 런타임 증거가 해당 QA 실행의 시간 범위에 속하지 않습니다.");
                    calculated=snapshots.hash(saved);
                } else {
                    var validator=additional.get(step.stepCode());
                    if(validator==null)throw conflict(step.stepCode()+" 실제 실행 증거 검증기가 아직 연결되지 않았습니다. 전체 QA 구현·검증 후 게시하세요.");
                    calculated=validator.selectValidatedEvidenceHash(mapper.readTree(step.evidenceJson()),frozen,run,step.createdAt());
                }
                if(calculated==null || !calculated.matches("[0-9a-f]{64}") || !calculated.equals(step.evidenceHash()))throw conflict("저장된 QA 근거의 지문이 일치하지 않습니다.");
                hashes.put(step.stepCode(),calculated);
            }
            if(!hashes.keySet().equals(REQUIRED))throw conflict("필수 QA 네 단계가 모두 필요합니다.");
            return snapshots.hash(Map.of("schemaVersion",1,"snapshotHash",frozen.hash(),"runId",run.runId(),"steps",hashes));
        }catch(ApiException exception){throw exception;}
        catch(Exception exception){throw conflict("QA 근거 형식·정답·fixture를 재검증하지 못했습니다. 현재 코드에서 QA를 다시 실행하세요.");}
    }
    /** 잠금 밖에서 검증한 동일 steps에만 호출한다. 추가 실행 원장의 최신 시도 변화도 게시 전에 차단한다. */
    public void validateCurrentEvidence(Run run,List<Step> steps,AttachmentPolicyValidationSnapshotFactory.Frozen frozen){
        try {
            for(String code:List.of("PROVIDER_PROFILES","WORKER_DB_RECOVERY")) {
                var handler=additional.get(code);
                var matches=steps.stream().filter(s->code.equals(s.stepCode()) && "PASSED".equals(s.statusCode()) && run.runId().equals(s.runId())).toList();
                if(handler==null || matches.size()!=1)throw conflict("게시 직전 필수 QA 근거를 다시 확인할 수 없습니다.");
                handler.validateCurrentEvidence(mapper.readTree(matches.getFirst().evidenceJson()),frozen,run);
            }
        }catch(ApiException exception){throw exception;}
        catch(Exception exception){throw conflict("게시 직전 QA 근거의 현재성을 확인하지 못했습니다. 변경 없이 요청을 취소합니다.");}
    }
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
}
