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
        assertThatThrownBy(()->new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles),new Definition(99,"TEST",List.of()))).hasMessage("CATALOG_DEFINITION_INVALID");
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
    private AttachmentProviderQaCatalog packagedCatalog() {
        var config=new StandardBbsAttachmentProfileConfiguration();profiles=List.of(config.selectTaebaekProfileDetails(),config.selectHoengseongProfileDetails(),config.selectYeongwolProfileDetails(),config.selectWonjuProfileDetails(),config.selectJecheonProfileDetails(),config.selectBoeunProfileDetails(),config.selectOkcheonProfileDetails(),config.selectYangpyeongProfileDetails(),config.selectCheorwonProfileDetails());
        var targets=profiles.stream().flatMap(p->p.selectSourceBindings().stream()).map(b->new Target(UUID.randomUUID(),b.localSourceCode(),b.listParserProfileCode(),"https://example.go.kr/list","{}")).toList();scope=AttachmentProviderQaPlan.selectPlan(profiles,targets);
        // 정적 계획 계약용 최소 규칙. 실제 seed/HTTP는 별도 fixed-case 시험에서 검증한다.
        rules=new AnnouncementSourceClassificationRuleSet("QA",List.of(rule("TARGET",RuleGroupKindCode.TARGET,"청년농업인",TargetCategoryCode.BUSINESS,null),
                rule("SUPPORT",RuleGroupKindCode.SUPPORT_TYPE,"육성지원",null,SupportTypeCode.GRANT_SUBSIDY)));
        return new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles));
    }
    private static final Instant TAEBAEK_OBSERVED=Instant.parse("2026-09-15T03:28:40.008199613Z");
    private Definition packagedDefinition() throws Exception {
        try(var input=new org.springframework.core.io.ClassPathResource("announcement-attachment/provider-qa-catalog-v2.json").getInputStream()) {
            return mapper.readValue(input,Definition.class);
        }
    }
    /** 계약 시험용 메모리 fixture만 지문을 맞춘다. 배포 catalog/공식 기대값 파일을 수정하거나 실행하지 않는다. */
    private AttachmentProviderQaCatalog matchingProfileFixture() throws Exception {
        packagedCatalog();var definition=packagedDefinition();
        var notices=definition.notices().stream().map(n->{
            if(n.expectation()==null)return n;
            var current=profiles.stream().filter(p->p.selectProfileCode().equals(n.profileCode())).findFirst().orElseThrow();var e=n.expectation();
            return new Notice(n.caseCode(),n.profileCode(),n.source(),new Expectation(current.selectProfileHash(),e.title(),e.observedAt(),e.discoveryStatus(),e.discoveryComplete(),e.files(),e.limits()));
        }).toList();
        return new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles),new Definition(2,"TEST-REVIEWED-FIXTURE",notices));
    }
    @Test void packagedHistoricalExpectationIsNotReboundToChangedBbsCode() throws Exception {
        var catalog=packagedCatalog();var result=catalog.selectPrepared(scope,rules,runtimeHash,TAEBAEK_OBSERVED);
        var historical=packagedDefinition().notices().stream().filter(n->"TAEBAEK-184816".equals(n.caseCode())).findFirst().orElseThrow();
        assertThat(historical.expectation().profileHash()).isEqualTo("9aea97d1281dd778ba6d6f331fd7dd147fef2a05f6de7132ccd90c28b28a58e5");
        assertThat(historical.expectation().profileHash()).isNotEqualTo(profiles.getFirst().selectProfileHash());
        assertThat(result.plan().cases().stream().filter(c->"TAEBAEK-184816".equals(c.caseCode()))).singleElement()
                .satisfies(c->assertThat(c.statusCode()).isEqualTo("PROFILE_CHANGED"));
        assertThat(result.plan().cases().stream().filter(c->c.caseCode().startsWith("CHEORWON-"))).hasSize(3)
                .allSatisfy(c->{assertThat(c.statusCode()).isEqualTo("REFERENCE_ONLY");assertThat(c.expectedFileCount()).isNull();assertThat(c.normalNotice()).isFalse();});
        assertThat(result.inputs()).isEmpty();assertThat(result.plan().segments()).isEmpty();
        assertThat(result.plan().executableCount()).isZero();assertThat(result.plan().isQaPassed()).isFalse();
        assertThat(result.plan().isExpectationCoverageComplete()).isFalse();
    }
    @Test void reviewedExceptionFixtureKeepsAllReferencesAndCannotFillNormalCoverage() throws Exception {
        var catalog=matchingProfileFixture();var result=catalog.selectPrepared(scope,rules,runtimeHash,TAEBAEK_OBSERVED);
        assertThat(result.plan().targets()).hasSize(11);assertThat(result.plan().cases()).hasSize(27);
        assertThat(result.plan().cases().stream().filter(c->!"TAEBAEK-184816".equals(c.caseCode())))
                .hasSize(26).allSatisfy(c->assertThat(c.statusCode()).isEqualTo("REFERENCE_ONLY"));
        assertThat(result.plan().cases().stream().filter(c->"TAEBAEK-184816".equals(c.caseCode()))).singleElement().satisfies(c->{
            assertThat(c.statusCode()).isEqualTo("EXPECTED_INPUT_READY");assertThat(c.normalNotice()).isFalse();assertThat(c.expectedFileCount()).isEqualTo(2);
        });
        assertThat(result.inputs()).hasSize(1);assertThat(result.plan().isQaPassed()).isFalse();assertThat(result.plan().isExpectationCoverageComplete()).isFalse();
        assertThat(result.plan().formatCoverage().missingFormats()).containsExactly("HWP","PDF");
        assertThat(result.plan().targets()).allSatisfy(t->{assertThat(t.normalNoticeCount()).isZero();assertThat(t.isExpectationCoverageComplete()).isFalse();});
        var target=result.plan().targets().stream().filter(t->t.targetKey().equals("LOCAL_GOV_NOTICE:LGS-000121")).findFirst().orElseThrow();
        assertThat(target.formatApplicability().expectedProvidedFormats()).containsExactly("HWPX");
        assertThat(target.formatApplicability().normalMultiFileNoticeCount()).isZero();
        var files=result.inputs().getFirst().files();assertThat(files).hasSize(2);
        assertThat(files).extracting(f->f.roleExpectation().roleCode()).containsExactly("UNKNOWN","FORM");
        assertThat(files).extracting(f->f.roleExpectation().reasonCode()).containsExactly("MIXED_DOCUMENT_ROLES","ROLE_TEXT_STRUCTURE_MATCHED");
        assertThat(files).extracting(ExpectedFile::minimumCharacters).containsExactly(2041,1994);
        assertThat(files).extracting(ExpectedFile::minimumBlocks).containsExactly(62,117);
        assertThat(files).extracting(ExpectedFile::binaryHash).containsExactly(
                "424bde05e7a87baaab4a7261bb2266e2ebde4d9d733c9986596507e79f14220a",
                "67dfc0af4cf21d0e9363132cc663154086aa5fc18625f443f43021704703247a");
        assertThat(result.inputs().getFirst().limits()).isEqualTo(new Limits(420,44,83886080));
    }
    @ParameterizedTest @ValueSource(longs={-1,604801})
    void reviewedFixtureCannotRunBeforeObservationOrAfterSevenDays(long elapsed) throws Exception {
        var catalog=matchingProfileFixture();var result=catalog.selectPrepared(scope,rules,runtimeHash,TAEBAEK_OBSERVED.plusSeconds(elapsed));
        assertThat(result.inputs()).isEmpty();assertThat(result.plan().segments()).isEmpty();
        assertThat(result.plan().cases().stream().filter(c->"TAEBAEK-184816".equals(c.caseCode()))).singleElement()
                .satisfies(c->assertThat(c.statusCode()).isEqualTo("OBSERVATION_EXPIRED"));
        assertThat(result.plan().isQaPassed()).isFalse();assertThat(result.plan().isExpectationCoverageComplete()).isFalse();
    }
    @Test void catalogOwnsInstantCodecWithoutMutatingTheCallerMapper() {
        var configured=packagedCatalog().selectPrepared(scope,rules,runtimeHash,TAEBAEK_OBSERVED);
        var caller=new ObjectMapper();var before=Set.copyOf(caller.getRegisteredModuleIds());
        var plain=new AttachmentProviderQaCatalog(caller,new AttachmentDiscoveryProfileRegistry(profiles))
                .selectPrepared(scope,rules,runtimeHash,TAEBAEK_OBSERVED);
        assertThat(plain.plan()).isEqualTo(configured.plan());assertThat(plain.inputs()).isEqualTo(configured.inputs());
        assertThat(caller.getRegisteredModuleIds()).isEqualTo(before);
    }
    @Test void reviewedFixtureDoesNotBypassChangedTitleRules() throws Exception {
        var catalog=matchingProfileFixture();var changed=new ArrayList<>(rules.rules());
        changed.add(rule("B",RuleGroupKindCode.AUTO_EXCLUDE_B,"취업농",null,null));
        var result=catalog.selectPrepared(scope,new AnnouncementSourceClassificationRuleSet("CHANGED",changed),runtimeHash,TAEBAEK_OBSERVED);
        assertThat(result.inputs()).isEmpty();assertThat(result.plan().segments()).isEmpty();
        assertThat(result.plan().cases().stream().filter(c->"TAEBAEK-184816".equals(c.caseCode()))).singleElement()
                .satisfies(c->assertThat(c.statusCode()).isEqualTo("TITLE_EXPECTATION_CHANGED"));
    }

    Prepared prepareV2(List<Notice> notices) {
        return new AttachmentProviderQaCatalog(mapper,new AttachmentDiscoveryProfileRegistry(profiles),new Definition(2,"TEST-V2",notices))
                .selectPrepared(scope,rules,runtimeHash,now);
    }
    Notice withFiles(Notice notice,List<ExpectedFile> files) {
        var e=notice.expectation();return alter(notice,new Expectation(e.profileHash(),e.title(),e.observedAt(),e.discoveryStatus(),e.discoveryComplete(),files,e.limits()));
    }
    ExpectedFile otherLocator(ExpectedFile file) {
        return new ExpectedFile("9".repeat(64),file.downloadAllowed(),file.format(),file.binaryHash(),file.quality(),file.minimumCharacters(),file.minimumBlocks(),file.requiredPhrases(),file.roleExpectation());
    }
    List<Notice> mixedProviderFormats() {
        var result=new ArrayList<Notice>();
        for(int i=1;i<=3;i++) {
            result.add(withFiles(notice("BIZINFO","BIZ",""+i),List.of(file("PDF","COMPLETE_TEXT"),otherLocator(file("PDF","COMPLETE_TEXT")))));
            result.add(withFiles(notice("GOV24_PUBLIC_SERVICE","GOV",""+i),List.of(file("HWP","COMPLETE_TEXT"),file("HWPX","COMPLETE_TEXT"))));
        }
        return result;
    }
    @Test void v2UsesEveryProvidedFormatPerTargetAndAllThreeAcrossFullScope() {
        var result=prepareV2(mixedProviderFormats());assertThat(result.plan().isExpectationCoverageComplete()).isTrue();assertThat(result.plan().isQaPassed()).isFalse();
        assertThat(result.inputs()).hasSize(6);assertThat(result.plan().targets()).hasSize(2);
        var biz=result.plan().targets().getFirst();assertThat(biz.missingFormats()).isEmpty();
        assertThat(biz.formatApplicability().expectedProvidedFormats()).containsExactly("PDF");
        assertThat(biz.formatApplicability().unobservedFormats()).containsExactly("HWP","HWPX");
        assertThat(biz.formatApplicability().normalMultiFileNoticeCount()).isEqualTo(3);
        assertThat(result.plan().formatCoverage().requiredFormats()).containsExactly("HWP","HWPX","PDF");
        assertThat(result.plan().formatCoverage().missingFormats()).isEmpty();
        assertThat(result.plan().targets().get(1).formatApplicability().unobservedFormats()).containsExactly("PDF");
    }
    @Test void v1KeepsPerTargetThreeFormatsAndOriginalMetadataShape() throws Exception {
        var v1=prepare(mixedProviderFormats());assertThat(v1.plan().isExpectationCoverageComplete()).isFalse();
        assertThat(v1.plan().targets().getFirst().missingFormats()).containsExactly("HWP","HWPX");
        assertThat(v1.plan().targets().get(1).missingFormats()).containsExactly("PDF");
        assertThat(mapper.writeValueAsString(v1.plan())).doesNotContain("formatCoverage","formatApplicability");
        assertThat(mapper.readValue(mapper.writeValueAsString(v1.plan()),Plan.class)).isEqualTo(v1.plan());
        assertThat(prepareV2(mixedProviderFormats()).plan().catalogHash()).isNotEqualTo(v1.plan().catalogHash());
    }
    @Test void v2UnobservedFormatsAreNotDeclaredUnsupportedOrSuccessful() {
        var result=prepareV2(List.of(alter(notice("BIZINFO","BIZ","1"),null)));
        assertThat(result.plan().targets()).allSatisfy(t->{
            assertThat(t.isExpectationCoverageComplete()).isFalse();assertThat(t.formatApplicability().statusCode()).isEqualTo("EXPECTATIONS_UNKNOWN");
            assertThat(t.formatApplicability().expectedProvidedFormats()).isEmpty();assertThat(t.formatApplicability().unobservedFormats()).containsExactly("HWP","HWPX","PDF");
        });
        assertThat(result.plan().isExpectationCoverageComplete()).isFalse();assertThat(result.inputs()).isEmpty();
    }
    @Test void v2RequiresAllThreeFormatsSomewhereEvenWhenEveryTargetSampleHasOnlyPdf() {
        var notices=mixedProviderFormats().stream().map(n->withFiles(n,List.of(file("PDF","COMPLETE_TEXT"),otherLocator(file("PDF","COMPLETE_TEXT"))))).toList();
        var plan=prepareV2(notices).plan();assertThat(plan.targets()).allSatisfy(t->assertThat(t.isExpectationCoverageComplete()).isTrue());
        assertThat(plan.formatCoverage().missingFormats()).containsExactly("HWP","HWPX");assertThat(plan.isExpectationCoverageComplete()).isFalse();
    }
    @Test void v2RequiresNormalMultiFileNoticeAndDoesNotConfuseSeveralSingleFileNotices() {
        var notices=mixedProviderFormats().stream().map(n->"BIZINFO".equals(n.source().providerCode())?withFiles(n,List.of(file("PDF","COMPLETE_TEXT"))):n).toList();
        var plan=prepareV2(notices).plan();assertThat(plan.targets().getFirst().normalNoticeCount()).isEqualTo(3);
        assertThat(plan.targets().getFirst().formatApplicability().normalMultiFileNoticeCount()).isZero();assertThat(plan.isExpectationCoverageComplete()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED"})
    void v2AnotherProvidersSuccessCannotEraseTheTargetsFailedFormat(String quality) {
        var notices=new ArrayList<>(mixedProviderFormats());notices.add(withFiles(notice("GOV24_PUBLIC_SERVICE","GOV","FAIL"),List.of(file("PDF",quality))));
        var plan=prepareV2(notices).plan();assertThat(plan.targets().get(1).normalNoticeCount()).isEqualTo(3);
        assertThat(plan.targets().get(1).formatApplicability().expectedProvidedFormats()).containsExactly("HWP","HWPX","PDF");
        assertThat(plan.targets().get(1).missingFormats()).containsExactly("PDF");assertThat(plan.formatCoverage().missingFormats()).isEmpty();
        assertThat(plan.cases()).hasSize(7);assertThat(plan.executableCount()).isEqualTo(7);assertThat(plan.isExpectationCoverageComplete()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"UNKNOWN","FORM","REFERENCE"})
    void v2NonNoticeRolesNeverSupplyNormalOrNormalMultiFileCoverage(String roleCode) {
        var notices=mixedProviderFormats().stream().map(n->{
            if(!"BIZINFO".equals(n.source().providerCode()))return n;
            return withFiles(n,n.expectation().files().stream().map(f->{var r=f.roleExpectation();
                return new ExpectedFile(f.locatorHash(),true,f.format(),f.binaryHash(),f.quality(),f.minimumCharacters(),f.minimumBlocks(),f.requiredPhrases(),
                        new RoleExpectation(r.ruleVersion(),r.rulesHash(),roleCode,"UNKNOWN".equals(roleCode)?"INITIAL_HEADING_REQUIRED":r.reasonCode(),r.textHash(),r.blocksHash(),r.assessmentHash()));}).toList());
        }).toList();
        var plan=prepareV2(notices).plan();assertThat(plan.targets().getFirst().normalNoticeCount()).isZero();
        assertThat(plan.targets().getFirst().formatApplicability().normalMultiFileNoticeCount()).isZero();assertThat(plan.isExpectationCoverageComplete()).isFalse();
    }
    @Test void v2UnsupportedExtraFileRemainsInDenominatorAndPreventsNormalNotice() {
        var notices=mixedProviderFormats().stream().map(n->{if(!"BIZINFO".equals(n.source().providerCode()))return n;
            var files=new ArrayList<>(n.expectation().files());files.add(new ExpectedFile("0".repeat(64),false,null,null,null,0,0,List.of()));return withFiles(n,files);}).toList();
        var result=prepareV2(notices);assertThat(result.inputs()).hasSize(6);assertThat(result.plan().cases().getFirst().expectedFileCount()).isEqualTo(3);
        assertThat(result.plan().targets().getFirst().normalNoticeCount()).isZero();assertThat(result.plan().isExpectationCoverageComplete()).isFalse();
    }
    @Test void v2CannotHideStaleReferenceIncompleteDiscoveryOrReduceScope() throws Exception {
        var notices=new ArrayList<>(mixedProviderFormats());var n=notices.getFirst();var e=n.expectation();
        for(var changed:Arrays.asList((Expectation)null,new Expectation(e.profileHash(),e.title(),now.minusSeconds(604801),"FOUND",true,e.files(),e.limits()),
                new Expectation(e.profileHash(),e.title(),e.observedAt(),"FOUND",false,e.files(),e.limits()))) {
            notices.set(0,alter(n,changed));assertThat(prepareV2(notices).plan().isExpectationCoverageComplete()).isFalse();
        }
        var item=scope.items().getFirst();var reduced=new com.saneb.domain.announcementattachment.dto.AttachmentProviderQaPlanResponse.Item(item.providerCode(),null,null,null,
                item.statusCode(),item.message(),item.profiles(),3,List.of("PDF"));
        scope=new AttachmentProviderQaPlan.Plan(1,scope.summary(),List.of(reduced,scope.items().get(1)),List.of());
        assertThatThrownBy(()->prepareV2(mixedProviderFormats())).hasMessage("CATALOG_SCOPE_INCOMPLETE");
    }
    @Test void v2ApplicabilityIsSafeFrozenMetadataAndChangesWithExpectedInventory() throws Exception {
        var result=prepareV2(mixedProviderFormats());String json=mapper.writeValueAsString(result.plan());
        assertThat(json).doesNotContain("https://","소상공인","sourceUrl","requiredPhrases","NOT_APPLICABLE","PASSED");
        assertThat(mapper.readValue(json,Plan.class)).isEqualTo(result.plan());
        var notices=new ArrayList<>(mixedProviderFormats());notices.set(0,withFiles(notices.getFirst(),List.of(file("HWPX","COMPLETE_TEXT"),file("PDF","COMPLETE_TEXT"))));
        assertThat(prepareV2(notices).plan().catalogHash()).isNotEqualTo(result.plan().catalogHash());
    }
    Target target(String code){return new Target(UUID.randomUUID(),code,"SPRING_BBS","https://example.go.kr/list","{}");}
}
