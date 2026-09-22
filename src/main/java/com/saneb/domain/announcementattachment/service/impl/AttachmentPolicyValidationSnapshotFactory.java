package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeGate;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleValidationDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 짧은 DB snapshot과 transaction 밖의 설치 지문을 결합한다. 운영 URL/설정 원문은 저장하지 않는다. */
@Component
public final class AttachmentPolicyValidationSnapshotFactory {
    private final AnnouncementSourceRuleReleaseService rules;
    private final AnnouncementAttachmentPolicyValidationDao dao;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final AttachmentRuntimeIdentity runtime;
    private final AttachmentRuntimeGate gate;
    private final AttachmentWorkerDbQaGate workerDb;
    private final com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog catalog;
    private final ObjectMapper mapper;
    public record Runtime(String runtimeHash, String runtimeSuiteHash, String executionCodeHash, AttachmentWorkerDbQaGate.Identity workerDbQa) {
        public Runtime(String runtimeHash,String runtimeSuiteHash,String executionCodeHash) {this(runtimeHash,runtimeSuiteHash,executionCodeHash,null);}
    }
    public record Frozen(String hash, String json, AnnouncementSourceRuleValidationDetails rule, Runtime runtime) {
        public AttachmentPolicyResponses.Configuration selectConfiguration() {
            try {
                if(json==null || json.length()>2097152)throw new IllegalArgumentException();
                var parser=new ObjectMapper().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
                var configuration=parser.treeToValue(parser.readTree(json).path("settings"),AttachmentPolicyResponses.Configuration.class);
                if(configuration==null || !configuration.selectEngineCurrent())throw new IllegalArgumentException();
                return configuration;
            }catch(Exception exception){throw new IllegalArgumentException("고정된 정책 QA의 엔진·구간 규칙 설정이 유효하지 않습니다.");}
        }
    }
    public AttachmentPolicyValidationSnapshotFactory(AnnouncementSourceRuleReleaseService rules, AnnouncementAttachmentPolicyValidationDao dao,
            AttachmentDiscoveryProfileRegistry profiles, AttachmentRuntimeIdentity runtime, AttachmentRuntimeGate gate, AttachmentWorkerDbQaGate workerDb, ObjectMapper mapper,
            com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog catalog) {
        this.rules=rules; this.dao=dao; this.profiles=profiles; this.runtime=runtime; this.gate=gate;
        this.workerDb=workerDb;
        this.catalog=catalog;
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
    public Runtime selectRuntime() {
        try {
            var installed=runtime.selectIdentity();String suite=gate.selectSuiteHash();
            if(!AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(installed.extractorVersion()) || installed.configHash()==null
                    || !installed.configHash().matches("[0-9a-f]{64}") || suite==null || !suite.matches("[0-9a-f]{64}")) throw new IllegalStateException();
            String codeHash = new AttachmentApplicationCodeFingerprint(mapper).selectVerifiedHash();
            return new Runtime(installed.configHash(),suite,codeHash,workerDb.selectIdentity(codeHash,installed.configHash()));
        }
        catch (AttachmentApplicationCodeFingerprint.Failure exception) {
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED,HttpStatus.CONFLICT,
                    "애플리케이션 QA 빌드 목록과 설치 코드 검증에 실패했습니다(" + exception.selectCode()
                    + "). 현재 코드의 전체 빌드 산출물을 설치한 뒤 QA를 다시 예약하세요.");
        }
        catch (AttachmentWorkerDbQaProcess.Failure exception) {throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED,HttpStatus.CONFLICT,
                "독립 DB QA 설치·격리 검증에 실패했습니다("+exception.selectCode()+"). 현재 애플리케이션과 같은 QA 산출물을 설치한 뒤 다시 예약하세요.");}
        catch (Exception exception) { throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED,HttpStatus.CONFLICT,
                "Linux 격리 추출기의 설치 지문을 확인할 수 없습니다. 설치와 권한을 확인한 후 QA를 예약하세요."); }
    }
    public AttachmentProviderQaPlan.Plan selectProviderQaPlan() {
        try {return AttachmentProviderQaPlan.selectPlan(profiles.selectProfileList(),dao.selectTargetList());}
        catch(IllegalArgumentException exception) {throw conflict("시스템 수집원·첨부 프로필 등록의 ID/중복/형식이 유효하지 않습니다. 등록 정보를 확인하세요.");}
    }
    public boolean selectPolicyManifestCurrent(AttachmentPolicyManagementRows.Row policy) {
        try {
            var registered=profiles.selectProfileList().stream().map(p->new AttachmentPolicyResponses.Profile(p.selectProviderCode(),p.selectProfileCode(),p.selectProfileHash()))
                    .sorted(Comparator.comparing(AttachmentPolicyResponses.Profile::providerCode).thenComparing(AttachmentPolicyResponses.Profile::profileCode)).toList();
            return policy.profileManifestJson()!=null && policy.profileManifestJson().length()<=256000
                    && mapper.readTree(json(registered)).equals(mapper.readTree(policy.profileManifestJson()));
        } catch(Exception exception) {return false;}
    }
    public Frozen selectSnapshot(AttachmentPolicyManagementRows.Row policy, Runtime installed) {
        if(installed==null || installed.executionCodeHash()==null || !installed.executionCodeHash().matches("[0-9a-f]{64}"))
            throw conflict("DB 조회 전에 현재 애플리케이션 코드·리소스 지문을 검증해야 합니다. 설치 산출물을 확인한 뒤 QA를 다시 예약하세요.");
        try {AttachmentWorkerDbQaGate.validateIdentity(installed.workerDbQa(),installed.executionCodeHash(),installed.runtimeHash());}
        catch(AttachmentWorkerDbQaProcess.Failure failure) {throw conflict("독립 DB QA 산출물과 전체 suite/case 목록이 고정되지 않았습니다. 같은 코드의 QA 설치를 확인하세요.");}
        var rule=rules.selectRuleValidationDetails(policy.ruleReleaseId());
        if(rule==null || !policy.ruleReleaseId().equals(rule.releaseId()) || rule.rowVersion()==null || rule.rowVersion()<0
                || !Set.of("DRAFT","ACTIVE").contains(rule.releaseStatusCode()) || rule.calculatedSnapshotHash()==null
                || !rule.calculatedSnapshotHash().matches("[0-9a-f]{64}") || rule.ruleSet()==null || rule.ruleSet().rules().isEmpty() || rule.ruleSet().rules().size()>2000
                || ("ACTIVE".equals(rule.releaseStatusCode()) && !Objects.equals(rule.persistedSnapshotHash(),rule.calculatedSnapshotHash())))
            throw conflict("현재 규칙 상태·버전·게시 지문이 유효하지 않습니다. 규칙 무결성을 먼저 확인하세요.");
        try {
            if(policy.settingsJson()==null || policy.settingsJson().length()>8192 || policy.profileManifestJson()==null || policy.profileManifestJson().length()>256000)
                throw conflict("정책 설정 크기가 유효하지 않습니다.");
            var settings=mapper.readValue(policy.settingsJson(), AttachmentPolicyResponses.Configuration.class);
            if(settings==null || !settings.selectEngineCurrent() || !com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier
                    .selectRulesCurrent(settings.roleRuleVersion(),settings.roleRulesHash())
                    || !AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(settings.extractorVersion()) || settings.maximumSourceBytes()==null
                    || settings.maximumSourceBytes()<1 || settings.maximumSourceBytes()>83886080
                    || (settings.extractorConfigHash()!=null && !settings.extractorConfigHash().equals(installed.runtimeHash())))
                throw conflict("정책의 엔진·추출기 버전 또는 다운로드 한도가 현재 설치와 다릅니다. 초안을 갱신하세요.");
            List<AttachmentPolicyResponses.Profile> registered=profiles.selectProfileList().stream()
                    .map(p->new AttachmentPolicyResponses.Profile(p.selectProviderCode(),p.selectProfileCode(),p.selectProfileHash()))
                    .sorted(Comparator.comparing(AttachmentPolicyResponses.Profile::providerCode).thenComparing(AttachmentPolicyResponses.Profile::profileCode)).toList();
            if(registered.isEmpty() || registered.size()>1000 || registered.stream().map(p->p.providerCode()+":"+p.profileCode()).distinct().count()!=registered.size()
                    || registered.stream().anyMatch(p->p.profileHash()==null || !p.profileHash().matches("[0-9a-f]{64}"))
                    || !mapper.readTree(json(registered)).equals(mapper.readTree(policy.profileManifestJson())))
                throw conflict("시스템 첨부 profile과 정책의 고정 목록이 다릅니다. 초안에서 현재 시스템 구성을 다시 저장하세요.");
            var targets=dao.selectTargetList();
            if(targets.size()>1000) throw conflict("운영 대상 수집원 수가 QA snapshot 한도를 초과했습니다.");
            var scope=new ArrayList<Map<String,Object>>();
            scope.add(Map.of("providerCode","BIZINFO")); scope.add(Map.of("providerCode","GOV24_PUBLIC_SERVICE"));
            var targetIds = new HashSet<UUID>();
            var publicCodes = new HashSet<String>();
            for(var target:targets) {
                if(target==null || target.sourceId()==null || target.publicCode()==null || target.publicCode().isBlank()
                        || target.publicCode().length()>100 || target.publicCode().codePoints().anyMatch(Character::isISOControl)
                        || target.parserProfileCode()==null || target.noticeUrl()==null || target.profileConfigurationJson()==null)
                    throw conflict("활성 수집원에 시스템 profile 설정이 없습니다. 대상 목록을 먼저 확인하세요.");
                if(!targetIds.add(target.sourceId()) || !publicCodes.add(target.publicCode()))
                    throw conflict("활성 수집원의 ID 또는 시스템 기관 코드가 중복되었습니다. 중복 연결을 확인한 뒤 QA를 예약하세요.");
                // 첨부 profile은 publicCode에 결합된다. 같은 UUID/URL에서 기관 코드만 바뀌어도 이전 QA를 무효화한다.
                scope.add(Map.of("providerCode","LOCAL_GOV_NOTICE","sourceId",target.sourceId(),"publicCode",target.publicCode(),"parserProfileCode",target.parserProfileCode(),
                        "configurationHash",hash(List.of(target.noticeUrl(),mapper.readValue(target.profileConfigurationJson(),Object.class)))));
            }
            Map<String,Object> snapshot=new TreeMap<>();
            snapshot.put("schemaVersion",6); snapshot.put("policyId",policy.policyId()); snapshot.put("policyVersion",policy.rowVersion());
            snapshot.put("modeCode",policy.modeCode()); snapshot.put("settings",settings); snapshot.put("profiles",registered); snapshot.put("targets",scope);
            snapshot.put("rule",rule); snapshot.put("installed",installed); snapshot.put("executionCodeHash",installed.executionCodeHash());
            var providerPlan=AttachmentProviderQaPlan.selectPlan(profiles.selectProfileList(),targets);
            snapshot.put("providerQaPlan",providerPlan);
            snapshot.put("providerQaCatalog",catalog.selectPrepared(providerPlan,rule.ruleSet(),installed.runtimeHash(),java.time.Instant.now(),settings).plan());
            String value=json(snapshot);
            if(value.getBytes(StandardCharsets.UTF_8).length>1500000) throw conflict("QA 입력이 고정 저장 한도를 초과했습니다.");
            return new Frozen(hash(mapper.readValue(value,Object.class)),value,rule,installed);
        } catch(ApiException exception) { throw exception; }
        catch(Exception exception) { throw conflict("정책 QA 입력을 읽을 수 없습니다. 설정과 시스템 profile 형식을 확인하세요."); }
    }
    public String json(Object value) { try{return mapper.writeValueAsString(value);}catch(Exception exception){throw conflict("QA metadata를 직렬화할 수 없습니다.");} }
    public String hash(Object value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(value).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception exception){throw conflict("QA 입력 지문을 계산하지 못했습니다.");}
    }
    private ApiException conflict(String message) {return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
}
