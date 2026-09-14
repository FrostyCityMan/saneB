package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase.*;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.service.impl.AttachmentProviderQaPlan;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Target;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 계획/실행 입력의 계약 검증. 합성 기대값은 실제 문서 추출 또는 운영 QA 통과가 아니다. */
class AttachmentProviderQaCatalogTest {
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final Instant now=Instant.parse("2026-09-12T01:00:00Z");
    final String profileHash="a".repeat(64),runtimeHash="b".repeat(64);
    List<AttachmentDiscoveryProfile> profiles;
    AttachmentProviderQaPlan.Plan scope;
    AnnouncementSourceClassificationRuleSet rules;
    @BeforeEach void setup() {
        profiles=List.of(profile("BIZINFO","BIZ"),profile("GOV24_PUBLIC_SERVICE","GOV"));scope=AttachmentProviderQaPlan.selectPlan(profiles,List.of());
        rules=new AnnouncementSourceClassificationRuleSet("QA",List.of(rule("TARGET",RuleGroupKindCode.TARGET,"소상공인",TargetCategoryCode.BUSINESS,null),
                rule("SUPPORT",RuleGroupKindCode.SUPPORT_TYPE,"지원금",null,SupportTypeCode.GRANT_SUBSIDY),rule("B",RuleGroupKindCode.AUTO_EXCLUDE_B,"수출",null,null)));
    }
    AttachmentDiscoveryProfile profile(String provider,String code) {
        var p=mock(AttachmentDiscoveryProfile.class);when(p.selectProviderCode()).thenReturn(provider);when(p.selectProfileCode()).thenReturn(code);when(p.selectProfileHash()).thenReturn(profileHash);
        when(p.selectSourceBindings()).thenReturn(List.of(new AttachmentDiscoveryProfile.SourceBinding(null,null)));
        when(p.selectDetailUri(any(AttachmentDiscoveryProfile.Source.class))).thenAnswer(c->URI.create("https://example.go.kr/detail/"+c.getArgument(0,AttachmentDiscoveryProfile.Source.class).providerNoticeId()));
        when(p.selectApprovedRequest(any(URI.class))).thenReturn(true);return p;
    }
    AnnouncementSourceClassificationRule rule(String code,RuleGroupKindCode group,String term,TargetCategoryCode target,SupportTypeCode support) {
        return new AnnouncementSourceClassificationRule(code,code,group,term,StrengthCode.STRONG,target,support,List.of(AnnouncementSourceClassificationTerm.canonical(term,MatchModeCode.NORMALIZED_PHRASE)),true);
    }
    ExpectedFile file(String format,String quality) {
        boolean text=Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(quality);
        return new ExpectedFile(Integer.toHexString(format.hashCode()).repeat(64).substring(0,64),true,format,"d".repeat(64),quality,text?1:0,text?1:0,text?List.of("지원"):List.of(),
                "COMPLETE_TEXT".equals(quality)?new RoleExpectation(AttachmentDocumentRoleClassifier.VERSION,AttachmentDocumentRoleClassifier.RULES_HASH,
                        "NOTICE","ROLE_TEXT_STRUCTURE_MATCHED","e".repeat(64),"f".repeat(64),"a".repeat(64)):null);
    }
    Notice notice(String provider,String code,String id) {
        return new Notice(code+"-"+id,code,new AttachmentDiscoveryProfile.Source(provider,id,"https://example.go.kr/detail/"+id,null,null),
                new Expectation(profileHash,"소상공인 지원금",now.minusSeconds(60),"FOUND",true,List.of(file("PDF","COMPLETE_TEXT"),file("HWP","COMPLETE_TEXT"),file("HWPX","COMPLETE_TEXT")),new Limits(420,44,83886080L)));
    }
    Notice alter(Notice n,Expectation e){return new Notice(n.caseCode(),n.profileCode(),n.source(),e);}
    AttachmentProviderQaCatalog catalog(List<Notice> notices){return new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles),new Definition(1,"TEST-1",notices));}
    Prepared prepare(List<Notice> notices){return catalog(notices).selectPrepared(scope,rules,runtimeHash,now);}
    @Test void entireScopeRemainsWhenCatalogHasNoReadyEntries() {
        var result=prepare(List.of());assertThat(result.inputs()).isEmpty();assertThat(result.plan().targets()).hasSize(2);
        assertThat(result.plan().targets()).allSatisfy(t->{assertThat(t.normalNoticeCount()).isZero();assertThat(t.missingFormats()).containsExactly("HWP","HWPX","PDF");});
        assertThat(result.plan().segments()).isEmpty();assertThat(result.plan().isExpectationCoverageComplete()).isFalse();assertThat(result.plan().isQaPassed()).isFalse();
    }
    @Test void threeDistinctNormalNoticesForEveryTargetAndAllFormatsOnlyCompleteExpectations() {
        var notices=new ArrayList<Notice>();for(int i=1;i<=3;i++){notices.add(notice("BIZINFO","BIZ",""+i));notices.add(notice("GOV24_PUBLIC_SERVICE","GOV",""+i));}
        var result=prepare(notices);assertThat(result.inputs()).hasSize(6);assertThat(result.plan().isExpectationCoverageComplete()).isTrue();assertThat(result.plan().isQaPassed()).isFalse();
        assertThat(result.plan().segments()).singleElement().satisfies(s->{assertThat(s.maximumRequests()).isEqualTo(264);assertThat(s.maximumBytes()).isEqualTo(503316480);assertThat(s.maximumSecondsIncludingMargin()).isEqualTo(2880);});
        assertThat(result.plan().cases()).allSatisfy(c->{assertThat(c.inputHash()).matches("[0-9a-f]{64}");assertThat(c.normalNotice()).isTrue();});
    }
    @Test void referenceIsNeverConvertedToAnExecutionInput() {var n=notice("BIZINFO","BIZ","1");var result=prepare(List.of(alter(n,null)));assertThat(result.inputs()).isEmpty();assertThat(result.plan().cases()).singleElement().satisfies(c->{assertThat(c.statusCode()).isEqualTo("REFERENCE_ONLY");assertThat(c.expectedFileCount()).isNull();});}
    @Test void legacyCompleteTextWithoutRoleProofCannotEnterNewCatalogExecution() {
        var n=notice("BIZINFO","BIZ","1");var e=n.expectation();var f=e.files().getFirst();
        var legacy=new ExpectedFile(f.locatorHash(),true,f.format(),f.binaryHash(),f.quality(),f.minimumCharacters(),f.minimumBlocks(),f.requiredPhrases());
        var result=prepare(List.of(alter(n,new Expectation(e.profileHash(),e.title(),e.observedAt(),"FOUND",true,List.of(legacy),e.limits()))));
        assertThat(result.inputs()).isEmpty();assertThat(result.plan().cases().getFirst().statusCode()).isEqualTo("EXPECTATION_INVALID");
        assertThat(result.plan().isExpectationCoverageComplete()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"UNKNOWN","FORM","REFERENCE"})
    void roleNegativeCaseOrFormsAloneCannotInflateNormalNoticeCoverage(String roleCode) {
        var n=notice("BIZINFO","BIZ","1");var e=n.expectation();var f=e.files().getFirst();var r=f.roleExpectation();
        var role=new RoleExpectation(r.ruleVersion(),r.rulesHash(),roleCode,"UNKNOWN".equals(roleCode)?"INITIAL_HEADING_REQUIRED":r.reasonCode(),r.textHash(),r.blocksHash(),r.assessmentHash());
        var altered=new ExpectedFile(f.locatorHash(),true,f.format(),f.binaryHash(),f.quality(),f.minimumCharacters(),f.minimumBlocks(),f.requiredPhrases(),role);
        var result=prepare(List.of(alter(n,new Expectation(e.profileHash(),e.title(),e.observedAt(),"FOUND",true,List.of(altered),e.limits()))));
        assertThat(result.inputs()).hasSize(1);assertThat(result.plan().cases().getFirst().normalNotice()).isFalse();
        assertThat(result.plan().targets().getFirst().normalNoticeCount()).isZero();assertThat(result.plan().isQaPassed()).isFalse();
    }
    @Test void duplicateNoticeCannotCountAsThreeNormalNotices() {var n=notice("BIZINFO","BIZ","1");assertThatThrownBy(()->catalog(List.of(n,new Notice("OTHER",n.profileCode(),n.source(),n.expectation())))).hasMessage("CATALOG_DUPLICATE_NOTICE");}
    @Test void twoSourceIdsResolvingToTheSameDetailCannotInflateCoverage() {
        doReturn(URI.create("https://example.go.kr/same-detail")).when(profiles.getFirst()).selectDetailUri(any(AttachmentDiscoveryProfile.Source.class));
        var result=prepare(List.of(notice("BIZINFO","BIZ","1"),notice("BIZINFO","BIZ","2")));
        assertThat(result.inputs()).hasSize(1);assertThat(result.plan().cases()).extracting(CasePlan::statusCode).containsExactly("EXPECTED_INPUT_READY","DUPLICATE_DETAIL");
    }
    @ParameterizedTest @ValueSource(strings={"https://user:password@example.go.kr/detail","https://example.go.kr/detail#fragment","javascript:alert(1)","/relative"})
    void unsafeReferenceUrlsAreRejectedBeforeProfileResolution(String url) {
        var n=notice("BIZINFO","BIZ","1");var source=new AttachmentDiscoveryProfile.Source("BIZINFO","1",url,null,null);
        assertThatThrownBy(()->catalog(List.of(new Notice(n.caseCode(),n.profileCode(),source,null)))).hasMessage("CATALOG_SOURCE_INVALID");
    }
    @Test void callerCannotReduceNormalNoticeOrRequiredFormatsInScope() {
        var original=scope.items().getFirst();var reduced=new com.saneb.domain.announcementattachment.dto.AttachmentProviderQaPlanResponse.Item(original.providerCode(),null,null,null,
                original.statusCode(),original.message(),original.profiles(),1,List.of("PDF"));
        var changed=new AttachmentProviderQaPlan.Plan(1,scope.summary(),List.of(reduced,scope.items().get(1)),List.of());
        assertThatThrownBy(()->catalog(List.of()).selectPrepared(changed,rules,runtimeHash,now)).hasMessage("CATALOG_SCOPE_INCOMPLETE");
    }
    @Test void duplicateCaseCodeAndUnsupportedSchemaFailClosed() {
        var n=notice("BIZINFO","BIZ","1");assertThatThrownBy(()->catalog(List.of(n,n))).hasMessage("CATALOG_NOTICE_INVALID");
        assertThatThrownBy(()->new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles),new Definition(2,"TEST",List.of()))).hasMessage("CATALOG_DEFINITION_INVALID");
    }
    @Test void outOfScopeReferencesStayVisibleAndCannotBeExecuted() {
        var n=notice("BIZINFO","BIZ","1");var source=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","id","https://example.go.kr/detail","LGS-000001","SPRING_BBS");
        var result=prepare(List.of(new Notice(n.caseCode(),n.profileCode(),source,n.expectation())));assertThat(result.inputs()).isEmpty();assertThat(result.plan().cases().getFirst().statusCode()).isEqualTo("TARGET_OUTSIDE_SCOPE");
    }
    @ParameterizedTest @ValueSource(longs={-1,604801}) void futureOrStaleObservationDoesNotEnterBudget(long age) {
        var n=notice("BIZINFO","BIZ","1");var e=n.expectation();var result=prepare(List.of(alter(n,new Expectation(profileHash,e.title(),now.minusSeconds(age),e.discoveryStatus(),true,e.files(),e.limits()))));
        assertThat(result.inputs()).isEmpty();assertThat(result.plan().cases().getFirst().statusCode()).isEqualTo("OBSERVATION_EXPIRED");
    }
    @Test void profileHashAndSourceBindingChangesStayUnready() {
        var n=notice("BIZINFO","BIZ","1");var e=n.expectation();var changed=alter(n,new Expectation("f".repeat(64),e.title(),e.observedAt(),e.discoveryStatus(),true,e.files(),e.limits()));
        assertThat(prepare(List.of(changed)).plan().cases().getFirst().statusCode()).isEqualTo("PROFILE_CHANGED");
        when(profiles.getFirst().selectApprovedRequest(any(URI.class))).thenReturn(false);assertThat(prepare(List.of(n)).plan().cases().getFirst().statusCode()).isEqualTo("SOURCE_BINDING_INVALID");
    }
    @ParameterizedTest @ValueSource(strings={"PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED"})
    void expectedNegativeQualityDoesNotFillNormalNoticeOrCompleteFormatCoverage(String quality) {
        var n=notice("BIZINFO","BIZ","1");var e=n.expectation();var result=prepare(List.of(alter(n,new Expectation(profileHash,e.title(),e.observedAt(),"FOUND",true,List.of(file("PDF",quality)),e.limits()))));
        assertThat(result.inputs()).hasSize(1);assertThat(result.plan().cases().getFirst().normalNotice()).isFalse();assertThat(result.plan().targets().getFirst().missingFormats()).contains("PDF");
    }
    @Test void titleExclusionCannotInflateNormalCoverageAndExplicitNegativeCaseHasNoFiles() {
        var n=notice("BIZINFO","BIZ","1");var e=n.expectation();var wrong=alter(n,new Expectation(profileHash,"소상공인 수출 지원금",e.observedAt(),"FOUND",true,e.files(),e.limits()));
        assertThat(prepare(List.of(wrong)).plan().cases().getFirst().statusCode()).isEqualTo("TITLE_EXPECTATION_CHANGED");
        var expected=alter(n,new Expectation(profileHash,"소상공인 수출 지원금",e.observedAt(),"TITLE_BLOCKED",false,List.of(),e.limits()));
        var result=prepare(List.of(expected));assertThat(result.inputs()).hasSize(1);assertThat(result.plan().cases().getFirst().normalNotice()).isFalse();
    }
    @Test void everyFileMustBeValidAndOverLimitCasesAreNotSilentlyTruncated() {
        var n=notice("BIZINFO","BIZ","1");var e=n.expectation();
        var wrong=alter(n,new Expectation(profileHash,e.title(),e.observedAt(),"FOUND",true,e.files(),new Limits(421,44,83886080L)));
        assertThat(prepare(List.of(wrong)).plan().cases().getFirst().statusCode()).isEqualTo("EXPECTATION_INVALID");
        var duplicate=alter(n,new Expectation(profileHash,e.title(),e.observedAt(),"FOUND",true,Collections.nCopies(11,file("PDF","COMPLETE_TEXT")),e.limits()));
        var rejected=prepare(List.of(duplicate));assertThat(rejected.inputs()).isEmpty();assertThat(rejected.plan().cases().getFirst().expectedFileCount()).isEqualTo(11);
    }
    @Test void segmentationRetainsEveryCaseAndStaysWithinTwentyThreeHoursIncludingMargin() {
        var notices=new ArrayList<Notice>();for(int i=1;i<=350;i++)notices.add(notice("BIZINFO","BIZ",""+i));var result=prepare(notices);
        assertThat(result.plan().segments()).hasSize(3).allSatisfy(s->assertThat(s.maximumSecondsIncludingMargin()).isLessThanOrEqualTo(82800));
        assertThat(result.plan().segments().stream().flatMap(s->s.caseCodes().stream()).toList()).containsExactlyElementsOf(result.inputs().stream().map(AttachmentProviderQaCase::caseId).toList());
        assertThat(result.plan().targets()).hasSize(2);assertThat(result.plan().isExpectationCoverageComplete()).isFalse();
    }
    @Test void planContainsOnlySafeMetadataAndInputHashesMatchExecutor() throws Exception {
        var result=prepare(List.of(notice("BIZINFO","BIZ","1")));var executor=new AttachmentProviderQaCaseExecutor(null,null,null,null,null,null,mapper);
        assertThat(result.plan().cases().getFirst().inputHash()).isEqualTo(executor.selectHash(result.inputs().getFirst()));
        String json=mapper.writeValueAsString(result.plan());assertThat(json).doesNotContain("https://","소상공인","requiredPhrases","sourceUrl","title");
        assertThat(result.toString()).doesNotContain("https://","소상공인");
    }
    @Test void runtimeOrRuleChangesProduceDifferentFrozenInputs() {
        var cat=catalog(List.of(notice("BIZINFO","BIZ","1")));var first=cat.selectPrepared(scope,rules,runtimeHash,now);
        assertThat(cat.selectPrepared(scope,rules,"c".repeat(64),now).plan().cases().getFirst().inputHash()).isNotEqualTo(first.plan().cases().getFirst().inputHash());
        var second=new AnnouncementSourceClassificationRuleSet("SECOND",rules.rules());assertThat(cat.selectPrepared(scope,second,runtimeHash,now).plan().cases().getFirst().inputHash()).isNotEqualTo(first.plan().cases().getFirst().inputHash());
    }
    @Test void packagedPublicReferencesHaveValidProductionBindingsButNoExecutionExpectations() {
        var config=new StandardBbsAttachmentProfileConfiguration();profiles=List.of(config.selectTaebaekProfileDetails(),config.selectHoengseongProfileDetails(),config.selectYeongwolProfileDetails());
        var targets=List.of(target("LGS-000121"),target("LGS-000125"),target("LGS-000126"));scope=AttachmentProviderQaPlan.selectPlan(profiles,targets);
        var result=new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles)).selectPrepared(scope,rules,runtimeHash,now);
        assertThat(result.plan().targets()).hasSize(5);assertThat(result.plan().cases()).hasSize(9).allSatisfy(c->assertThat(c.statusCode()).isEqualTo("REFERENCE_ONLY"));
        assertThat(result.inputs()).isEmpty();assertThat(result.plan().isQaPassed()).isFalse();assertThat(result.plan().isExpectationCoverageComplete()).isFalse();
    }
    Target target(String code){return new Target(UUID.randomUUID(),code,"SPRING_BBS","https://example.go.kr/list","{}");}
}
