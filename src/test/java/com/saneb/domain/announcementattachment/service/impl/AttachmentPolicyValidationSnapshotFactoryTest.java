package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses;
import com.saneb.domain.announcementattachment.extraction.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleValidationDetails;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttachmentPolicyValidationSnapshotFactoryTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final AnnouncementSourceRuleReleaseService rules=mock(AnnouncementSourceRuleReleaseService.class);
    private final AnnouncementAttachmentPolicyValidationDao dao=mock(AnnouncementAttachmentPolicyValidationDao.class);
    private final AttachmentRuntimeIdentity runtime=mock(AttachmentRuntimeIdentity.class);
    private final AttachmentRuntimeGate gate=mock(AttachmentRuntimeGate.class);
    private final AttachmentWorkerDbQaGate workerDb=mock(AttachmentWorkerDbQaGate.class);
    private final BizInfoAttachmentDiscoveryProfile profile=new BizInfoAttachmentDiscoveryProfile();
    private final UUID policyId=UUID.randomUUID(),ruleId=UUID.randomUUID(),sourceId=UUID.randomUUID();
    private final AttachmentPolicyValidationSnapshotFactory.Runtime installed=installed("a".repeat(64),"e".repeat(64));
    private static AttachmentPolicyValidationSnapshotFactory.Runtime installed(String runtime,String code) {
        return new AttachmentPolicyValidationSnapshotFactory.Runtime(runtime,"b".repeat(64),code,AttachmentWorkerDbQaGateTest.identity(code,runtime));
    }
    private AttachmentPolicyValidationSnapshotFactory factory;
    private AnnouncementSourceRuleValidationDetails rule;
    @BeforeEach void setup() throws Exception {
        var keyword=new AnnouncementSourceClassificationRule("T","T",RuleGroupKindCode.TARGET,"소상공인",StrengthCode.STRONG,TargetCategoryCode.BUSINESS,null,
                List.of(AnnouncementSourceClassificationTerm.canonical("소상공인",MatchModeCode.NORMALIZED_PHRASE)),true);
        rule=new AnnouncementSourceRuleValidationDetails(ruleId,0,"DRAFT",null,"c".repeat(64),new AnnouncementSourceClassificationRuleSet("QA",List.of(keyword)));
        when(rules.selectRuleValidationDetails(ruleId)).thenAnswer(c->rule);
        when(dao.selectTargetList()).thenReturn(List.of(target("https://public.example/notice","{\"parser\":\"fixture\"}")));
        when(runtime.selectIdentity()).thenReturn(new AttachmentRuntimeIdentity.Identity(AttachmentRuntimeIdentity.EXTRACTOR_VERSION,"a".repeat(64),1,10));when(gate.selectSuiteHash()).thenReturn("b".repeat(64));
        when(workerDb.selectIdentity(anyString(),anyString())).thenAnswer(c->AttachmentWorkerDbQaGateTest.identity(c.getArgument(0),c.getArgument(1)));
        var registry=new AttachmentDiscoveryProfileRegistry(List.of(profile));
        factory=new AttachmentPolicyValidationSnapshotFactory(rules,dao,registry,runtime,gate,workerDb,mapper,
                new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog(mapper,registry));
    }
    private AttachmentPolicyValidationRows.Target target(String url,String settings) {return new AttachmentPolicyValidationRows.Target(sourceId,"LGS-QA","QA_LIST",url,settings);}
    private AttachmentPolicyManagementRows.Row policy(String settings,String manifest) throws Exception {
        var now=OffsetDateTime.parse("2026-09-11T14:00:00+09:00");
        return new AttachmentPolicyManagementRows.Row(policyId,"ATT-QA",1,0,"DRAFT","ENFORCE",ruleId,"DRAFT",null,settings,manifest,UUID.randomUUID(),now,now,null,null,null,null,null);
    }
    private String settings() throws Exception {return mapper.writeValueAsString(new AttachmentPolicyResponses.Configuration("attachment-1.0.0",AttachmentRuntimeIdentity.EXTRACTOR_VERSION,null,83886080L));}
    private String manifest() throws Exception {return mapper.writeValueAsString(List.of(new AttachmentPolicyResponses.Profile("BIZINFO",profile.selectProfileCode(),profile.selectProfileHash())));}
    private AttachmentPolicyManagementRows.Row policy() throws Exception {return policy(settings(),manifest());}
    @Test void freezesEveryTargetAndRuleWithoutSavingPublicUrlOrCallerSuccess() throws Exception {
        var frozen=factory.selectSnapshot(policy(),installed);
        var value=mapper.readTree(frozen.json());
        assertThat(value.path("targets").size()).isEqualTo(3);assertThat(frozen.hash()).matches("[0-9a-f]{64}");
        assertThat(value.path("targets").get(1).path("providerCode").asText()).isEqualTo("GOV24_PUBLIC_SERVICE");
        assertThat(value.path("schemaVersion").asInt()).isEqualTo(6);
        assertThat(value.path("providerQaPlan").path("summary").path("targetCount").asInt()).isEqualTo(3);
        assertThat(value.path("providerQaCatalog").path("catalogHash").asText()).matches("[0-9a-f]{64}");
        assertThat(value.path("providerQaCatalog").path("cases").size()).isEqualTo(37);
        assertThat(value.path("providerQaCatalog").path("executableCount").asInt()).isZero();
        assertThat(value.path("providerQaCatalog").path("isQaPassed").asBoolean()).isFalse();
        assertThat(value.path("installed").path("workerDbQa").path("caseIds").size()).isEqualTo(4);
        assertThat(value.path("targets").get(2).path("publicCode").asText()).isEqualTo("LGS-QA");
        assertThat(frozen.json()).contains(sourceId.toString(),"QA_LIST","configurationHash","executionCodeHash").doesNotContain("https://public.example", "noticeUrl", "passed");
        assertThat(frozen.hash()).isEqualTo(factory.selectSnapshot(policy(),installed).hash());verifyNoInteractions(runtime,gate);
    }
    @Test void frozenConfigurationRejectsMissingDuplicateOrUnboundSegmentRules() throws Exception {
        assertThat(factory.selectSnapshot(policy(),installed).selectConfiguration().engineVersion()).isEqualTo("attachment-1.0.0");
        String segmentVersion=com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION;
        String segmentHash=com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH;
        String segmentEngine=com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION;
        String valid="{\"settings\":{\"engineVersion\":\""+segmentEngine+"\",\"segmentRuleVersion\":\""+segmentVersion+"\",\"segmentRulesHash\":\""+segmentHash+"\"}}";
        assertThat(new AttachmentPolicyValidationSnapshotFactory.Frozen("a".repeat(64),valid,rule,installed)
                .selectConfiguration().segmentRulesHash()).isEqualTo(segmentHash);
        for(String invalid:List.of("{}","{\"settings\":null}",
                "{\"settings\":{\"engineVersion\":\"attachment-1.0.0\",\"engineVersion\":\"attachment-1.0.0\"}}",
                valid.replace(segmentEngine,"attachment-1.0.0"),valid.replace(segmentHash,"0".repeat(64)))) {
            assertThatThrownBy(()->new AttachmentPolicyValidationSnapshotFactory.Frozen("a".repeat(64),invalid,rule,installed).selectConfiguration())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("고정된 정책 QA");
        }
    }
    @Test void targetOrSystemConfigurationChangesInvalidateFrozenInput() throws Exception {
        String first=factory.selectSnapshot(policy(),installed).hash();
        when(dao.selectTargetList()).thenReturn(List.of(target("https://public.example/changed","{\"parser\":\"fixture\"}")));
        assertThat(factory.selectSnapshot(policy(),installed).hash()).isNotEqualTo(first);
        when(dao.selectTargetList()).thenReturn(List.of());assertThat(factory.selectSnapshot(policy(),installed).hash()).isNotEqualTo(first);
    }
    @Test void publicSourceCodeChangesInvalidateQaEvenWhenUuidUrlAndParserStayTheSame() throws Exception {
        String first=factory.selectSnapshot(policy(),installed).hash();
        when(dao.selectTargetList()).thenReturn(List.of(new AttachmentPolicyValidationRows.Target(sourceId,"LGS-QB","QA_LIST",
                "https://public.example/notice","{\"parser\":\"fixture\"}")));
        assertThat(factory.selectSnapshot(policy(),installed).hash()).isNotEqualTo(first);
    }
    @Test void duplicateOrMissingSystemSourceIdentityCannotCollapseProviderCoverage() throws Exception {
        var policy=policy();var first=target("https://public.example/notice","{}");
        for(var second:List.of(first,new AttachmentPolicyValidationRows.Target(UUID.randomUUID(),first.publicCode(),"QA_LIST","https://public.example/notice","{}"),
                new AttachmentPolicyValidationRows.Target(sourceId,"LGS-QB","QA_LIST","https://public.example/notice","{}"))) {
            when(dao.selectTargetList()).thenReturn(List.of(first,second));
            assertThatThrownBy(()->factory.selectSnapshot(policy,installed)).hasMessageContaining("중복");
        }
        for(String code:Arrays.asList(null,""," ","LGS\nQA","x".repeat(101))) {
            when(dao.selectTargetList()).thenReturn(List.of(new AttachmentPolicyValidationRows.Target(sourceId,code,"QA_LIST","https://public.example/notice","{}")));
            assertThatThrownBy(()->factory.selectSnapshot(policy,installed)).hasMessageContaining("설정이 없습니다");
        }
    }
    @Test void runtimeAndRuleChangesInvalidateFrozenInput() throws Exception {
        String first=factory.selectSnapshot(policy(),installed).hash();
        assertThat(factory.selectSnapshot(policy(),installed("f".repeat(64),installed.executionCodeHash())).hash()).isNotEqualTo(first);
        rule=new AnnouncementSourceRuleValidationDetails(ruleId,1,"DRAFT",null,rule.calculatedSnapshotHash(),rule.ruleSet());
        assertThat(factory.selectSnapshot(policy(),installed).hash()).isNotEqualTo(first);
    }
    @Test void activeRuleIntegrityMismatchAndOldProfileBindingsAreRejected() throws Exception {
        var policy=policy();
        assertThatThrownBy(()->factory.selectSnapshot(policy(settings(),"[]"),installed)).hasMessageContaining("profile");
        rule=new AnnouncementSourceRuleValidationDetails(ruleId,0,"ACTIVE","f".repeat(64),"c".repeat(64),rule.ruleSet());
        assertThatThrownBy(()->factory.selectSnapshot(policy,installed)).hasMessageContaining("규칙");
    }
    @Test void unknownInstalledRuntimeAndInvalidConfigurationCannotBeGuessed() throws Exception {
        when(runtime.selectIdentity()).thenThrow(new java.io.IOException("internal-path-must-not-leak"));
        assertThatThrownBy(()->factory.selectRuntime()).hasMessageContaining("Linux").hasMessageNotContaining("internal-path");
        assertThatThrownBy(()->factory.selectSnapshot(policy("{}",manifest()),installed)).hasMessageContaining("버전");
    }
    @Test void segmentSnapshotRetainsMissingProviderEvidenceInsteadOfReusingLegacyProof() throws Exception {
        var settings=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(settings());
        settings.put("engineVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        settings.put("segmentRuleVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION);
        settings.put("segmentRulesHash",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH);
        var frozen=factory.selectSnapshot(policy(settings.toString(),manifest()),installed);
        assertThat(frozen.selectConfiguration().segmentRulesHash()).isEqualTo(settings.path("segmentRulesHash").asText());
        var catalog=mapper.readTree(frozen.json()).path("providerQaCatalog");
        assertThat(catalog.path("executableCount").asInt()).isZero();
        assertThat(catalog.path("isExpectationCoverageComplete").asBoolean()).isFalse();
        assertThat(catalog.path("isQaPassed").asBoolean()).isFalse();
        assertThat(frozen.hash()).isNotEqualTo(factory.selectSnapshot(policy(),installed).hash());
    }
    @Test void oversizedAndMissingTargetConfigurationAreRejected() throws Exception {
        var policy=policy();when(dao.selectTargetList()).thenReturn(Collections.nCopies(1001,target("https://example.com","{}")));
        assertThatThrownBy(()->factory.selectSnapshot(policy,installed)).hasMessageContaining("한도");
        when(dao.selectTargetList()).thenReturn(List.of(target("https://example.com",null)));
        assertThatThrownBy(()->factory.selectSnapshot(policy,installed)).hasMessageContaining("설정이 없습니다");
    }
    @Test void applicationCodeChangeInvalidatesSnapshotWithoutReadingFilesInsideSnapshotTransaction() throws Exception {
        var first = factory.selectSnapshot(policy(), installed);
        var changed = installed(installed.runtimeHash(), "f".repeat(64));
        assertThat(factory.selectSnapshot(policy(), changed).hash()).isNotEqualTo(first.hash());
        assertThat(mapper.readTree(first.json()).path("executionCodeHash").asText()).isEqualTo(installed.executionCodeHash());
        verifyNoInteractions(runtime, gate);
    }
    @Test void runtimeReadsVerifiedCurrentApplicationCatalogAndMissingCodeHashIsRejected() throws Exception {
        assertThat(factory.selectRuntime().executionCodeHash()).matches("[0-9a-f]{64}");
        var policy = policy();
        assertThatThrownBy(() -> factory.selectSnapshot(policy, new AttachmentPolicyValidationSnapshotFactory.Runtime("a".repeat(64), "b".repeat(64), null)))
                .hasMessageContaining("코드·리소스 지문");
    }
    @Test void absentQaInstallationAndChangedCaseCatalogCannotReuseSnapshot() throws Exception {
        var policy=policy();
        assertThatThrownBy(()->factory.selectSnapshot(policy,new AttachmentPolicyValidationSnapshotFactory.Runtime(installed.runtimeHash(),installed.runtimeSuiteHash(),installed.executionCodeHash())))
                .hasMessageContaining("suite/case");
        var qa=installed.workerDbQa();
        var changed=new AttachmentWorkerDbQaGate.Identity("9".repeat(64),qa.executionCodeHash(),qa.extractorRuntimeHash(),qa.suites(),qa.caseIds());
        assertThat(factory.selectSnapshot(policy,new AttachmentPolicyValidationSnapshotFactory.Runtime(installed.runtimeHash(),installed.runtimeSuiteHash(),installed.executionCodeHash(),changed)).hash())
                .isNotEqualTo(factory.selectSnapshot(policy,installed).hash());
    }
    @Test void providerScopeCanBeInspectedWithoutLinuxOrExecutingRulesAndKeepsEveryTarget() throws Exception {
        var plan=factory.selectProviderQaPlan();assertThat(plan.summary().targetCount()).isEqualTo(3);
        assertThat(plan.summary().bindingMatchedCount()).isEqualTo(1);assertThat(plan.summary().missingProfileCount()).isEqualTo(2);
        assertThat(plan.summary().isQaPassed()).isFalse();assertThat(factory.selectPolicyManifestCurrent(policy())).isTrue();
        verifyNoInteractions(runtime,gate,workerDb,rules);
    }
}
