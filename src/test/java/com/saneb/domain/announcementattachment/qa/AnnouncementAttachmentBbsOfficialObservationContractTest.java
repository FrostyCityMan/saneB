package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentBbsOfficialObservationContractTest {
    @Test void chungjuRetainsTwoTitleNegativesAndOneHwpCandidateWithCatalogIdentity() throws Exception {
        var cases=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("CHUNGJU").toList();
        assertThat(cases).extracting(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code)
                .containsExactly("CHUNGJU-72625","CHUNGJU-72039","CHUNGJU-70852");
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=mapper.readTree(java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine();
        for(var sample:cases) {
            assertThat(sample.listedFileCount()).isEqualTo(1);
            assertThat(sample.profile().selectProfileCode()).isEqualTo("LOCAL_CHUNGJU_EMINWON_V1");
            var reference=java.util.stream.StreamSupport.stream(catalog.spliterator(),false)
                    .filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(reference.path("source")).isEqualTo(mapper.valueToTree(sample.source()));
            assertThat(reference.hasNonNull("expectation")).isFalse();
            var title=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput(
                    "LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),
                    com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                    com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
            boolean stopped=!"CHUNGJU-70852".equals(sample.code());
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title)).isEqualTo(!stopped);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectPlannedTitleStop(sample,title)).isEqualTo(stopped);
            if (stopped) {
                // 초기 DRAFT의 포괄 범위 진단이다. 지원사업이 아니라는 판단이나 운영 규칙의 정답으로 사용하지 않는다.
                assertThat(title.groupACodes()).isEmpty();
                assertThat(title.groupBCodes()).isEmpty();
                assertThat(title.reasonCode().name()).isEqualTo("TITLE_COMBINATION_NOT_MATCHED");
                assertThat(title.supportTypeCodes()).extracting(Enum::name).containsExactly("GENERAL_SUPPORT");
                var tagged = title.matches().stream().filter(m -> m.appliedActionCode().name().equals("TAG")).toList();
                assertThat(tagged).extracting(m -> m.matchedRuleTerm()).containsExactlyElementsOf(
                        sample.code().equals("CHUNGJU-72625") ? List.of("지원") : List.of("기업", "지원"));
                assertThat(tagged).allSatisfy(match -> {
                    var rule = rules.rules().stream().filter(r -> r.ruleCode().equals(match.ruleCode())).findFirst().orElseThrow();
                    assertThat(rule.strengthCode().name()).isEqualTo("SUPPLEMENTARY");
                    assertThat(match.maskedByProtectedMetadata()).isFalse();
                });
                if (sample.code().equals("CHUNGJU-72625")) assertThat(title.targetCategoryCodes()).isEmpty();
                else assertThat(title.targetCategoryCodes()).extracting(Enum::name).containsExactly("BUSINESS");
            }
        }
    }
    @Test void observationBudgetUsesInitialRequestBindingBeforeCountingRedirect() {
        var profile=new com.saneb.domain.announcementattachment.discovery.ChungjuEminwonAttachmentDiscoveryProfile();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(profile);
        var first=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(
                java.net.URI.create("https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=70852"));
        var other=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(
                java.net.URI.create("https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=72039"));
        assertThat(budget.selectRequestAllowed(first,first)).isTrue();
        assertThat(budget.selectRequestAllowed(first,other)).isFalse();
        assertThat(budget.requests).isEqualTo(1);
    }
    @Test void jecheonFixedReferencesKeepTheirFullFileCountsAndTitleGate() throws Exception {
        var cases=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("JECHEON").toList();
        assertThat(cases).extracting(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code)
                .containsExactly("JECHEON-403587","JECHEON-403530","JECHEON-403490");
        assertThat(cases).extracting(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::listedFileCount).containsExactly(1,1,2);
        var json=new ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode notices;
        try(var input=getClass().getResourceAsStream("/announcement-attachment/provider-qa-catalog-v2.json")) {
            notices=json.readTree(input).path("notices");
        }
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine();
        for(var sample:cases) {
            assertThat(sample.profile().selectProfileCode()).isEqualTo("LOCAL_JECHEON_BBS_V1");
            assertThat(sample.profile().selectDetailUri(sample.source()).getHost()).isEqualTo("www.jecheon.go.kr");
            var reference=java.util.stream.StreamSupport.stream(notices.spliterator(),false)
                    .filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(reference.path("source")).isEqualTo(json.valueToTree(sample.source()));
            assertThat(reference.hasNonNull("expectation")).isFalse();
            var result=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                    sample.title(),null,null,List.of(),
                    com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                    com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
            System.out.println("FIXED_TITLE_PREFLIGHT "+sample.code()+" "+result.titleStageCode()+" "+result.reasonCode());
            boolean negative=sample.code().equals("JECHEON-403587");
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).isEqualTo(!negative);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectPlannedTitleStop(sample,result)).isEqualTo(negative);
            if(negative) {
                var eligible=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                        "청년 주택자금 대출이자 지원",null,null,List.of(),
                        com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                        com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
                assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectPlannedTitleStop(sample,eligible)).isInstanceOf(AssertionError.class);
            }
        }
    }
    @Test void compactLabelTitleDoesNotBorrowNestedOrNeighbouringCells() {
        var layout=AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout.COMPACT_LABEL;
        var page=org.jsoup.Jsoup.parse("<div class='p-wrap bbs bbs__view'><table class='p-table block'><tr><th>제목</th><td>공식 지원 공고</td></tr><tr><td><table><tr><th>제목</th><td>중첩 제목</td></tr></table></td></tr></table></div>");
        assertThatCode(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"공식 지원 공고",layout)).doesNotThrowAnyException();
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"중첩 제목",layout)).isInstanceOf(AssertionError.class);
        page.select("table.p-table > tbody").first().append("<tr><th>제목</th><td>공식 지원 공고</td></tr>");
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"공식 지원 공고",layout)).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse("<table class='p-table block'><tr><th>제목</th><td>공식 지원 공고</td></tr></table>"),"공식 지원 공고",layout)).isInstanceOf(AssertionError.class);
    }
    @Test void onlyFixedGroupsAndExactProfileSourcesAreSelectable() throws Exception {
        var observation=AnnouncementAttachmentBbsOfficialObservationTest.class.getDeclaredMethod(
                "observesTitleBodyAndWholeAttachmentSetWithoutPublication",AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase.class);
        String factory=observation.getAnnotation(org.junit.jupiter.params.provider.MethodSource.class).value()[0];
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.class.getDeclaredMethod(factory).getParameterCount()).isZero();
        assertThat(java.util.Arrays.stream(AnnouncementAttachmentBbsOfficialObservationTest.class.getDeclaredMethods())
                .filter(method->method.getName().equals(factory)).count()).isEqualTo(1);
        var cases=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YANGPYEONG").toList();
        assertThat(cases).extracting(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code)
                .containsExactly("YANGPYEONG-312241","YANGPYEONG-311846","YANGPYEONG-311507");
        assertThat(cases).extracting(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::listedFileCount).containsExactly(1,2,2);
        for(var sample:cases) {
            assertThat(sample.profile().selectProfileCode()).isEqualTo("LOCAL_YANGPYEONG_BBS_V1");
            assertThat(sample.profile().selectDetailUri(sample.source()).getHost()).isEqualTo("www.yp21.go.kr");
            assertThat(sample.toString()).doesNotContain("https:",sample.title());
        }
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("TAEBAEK")).hasSize(1);
        for(String invalid:List.of("ALL","YANGPYEONG,TAEBAEK","https://example.com","../YANGPYEONG",""))
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectCases(invalid)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void compactTitleMustBelongToSingleOfficialTableAndMatchObservedIdentity() {
        var page=org.jsoup.Jsoup.parse("<div class='p-wrap bbs bbs__view'><table class='p-table block'><tr><td><span class='p-table__subject_text'>공식 지원 공고</span></td></tr><tr><td><table><tr><td><span class='p-table__subject_text'>다른 중첩 제목</span></td></tr></table></td></tr></table></div>");
        assertThatCode(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"공식 지원 공고",true)).doesNotThrowAnyException();
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"변경된 제목",true)).isInstanceOf(AssertionError.class);
        page.select("table.p-table > tbody > tr > td").first().append("<span class='p-table__subject_text'>공식 지원 공고</span>");
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"공식 지원 공고",true)).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse("<span class='p-table__subject_text'>공식 지원 공고</span>"),"공식 지원 공고",true)).isInstanceOf(AssertionError.class);
    }
    @Test void unsupportedPartialOrUnrunFilesCannotBecomeCompleteTextAnalysis() {
        var complete=Map.<String,Object>of("status","OBSERVED","quality","COMPLETE_TEXT");
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectWholeTextAnalysisComplete(true,List.of(complete))).isTrue();
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectWholeTextAnalysisComplete(false,List.of(complete))).isFalse();
        for(var incomplete:List.of(Map.of("status","UNSUPPORTED_NOT_DOWNLOADED"),Map.of("status","NOT_RUN"),
                Map.of("status","FAILED"),Map.of("status","OBSERVED","quality","PARTIAL_TEXT"),Map.of("status","OBSERVED","quality","OCR_REQUIRED")))
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectWholeTextAnalysisComplete(true,List.of(complete,incomplete))).isFalse();
    }
    @Test void budgetCannotBorrowAnotherInstitutionOrFetchPreview() {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YANGPYEONG").findFirst().orElseThrow();
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile());b.reserveBody();
        var request=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(sample.profile().selectDetailUri(sample.source()));
        assertThat(b.selectRequestAllowed(request)).isTrue();
        for(String url:List.of("https://www.taebaek.go.kr/www/selectBbsNttView.do?key=352&bbsNo=25&nttNo=184816",
                "https://www.yp21.go.kr/common/program/synap.jsp?fileName=test.pdf"))
            assertThat(b.selectRequestAllowed(com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(java.net.URI.create(url)))).isFalse();
        assertThat(b.requests).isEqualTo(3);
    }
    @Test void yangpyeongTitlePreflightUsesCurrentDraftWithoutPromotingExcludedExamples() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine();
        for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YANGPYEONG").toList()) {
            var result=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                    sample.title(),null,null,List.of(),
                    com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                    com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
            System.out.println("FIXED_TITLE_PREFLIGHT "+sample.code()+" "+result.titleStageCode()+" "+result.reasonCode());
            assertThat(result.titleStageCode()).isNotNull();
            if(sample.code().equals("YANGPYEONG-312241")) assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).isTrue();
        }
    }
    @Test void failedOrEmptyBodyCannotPassWholeObservationEvenWhenFilesWereObserved() {
        var request=new com.saneb.domain.announcementsource.provider.content.ProviderContentRequest("LOCAL_GOV_NOTICE",java.util.UUID.randomUUID(),"https://example.go.kr/list","https://example.go.kr/detail");
        var uri=java.net.URI.create(request.officialDetailUrl());
        var complete=com.saneb.domain.announcementsource.provider.content.ProviderContentResult.available(request,"청년 지원",uri,200,1,0);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectBodyComplete(complete)).isTrue();
        for(var incomplete:List.of(com.saneb.domain.announcementsource.provider.content.ProviderContentResult.disabled(request),
                com.saneb.domain.announcementsource.provider.content.ProviderContentResult.failure(request,
                        com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode.TIMEOUT,uri,null,2,0),
                com.saneb.domain.announcementsource.provider.content.ProviderContentResult.available(request,"  ",uri,200,1,0))) {
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectBodyComplete(incomplete)).isFalse();
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateBodyComplete(incomplete)).isInstanceOf(AssertionError.class);
        }
    }
    @Test void currentDraftTitleMustPassBeforeAnyBodyOrAttachmentRequestAndBodyReviewDoesNotStopFiles() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine();
        var title=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                AnnouncementAttachmentBbsOfficialObservationTest.TITLE,null,null,List.of(),
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title)).isTrue();
        var body=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                AnnouncementAttachmentBbsOfficialObservationTest.TITLE,"청년 지원 수출 지원",null,List.of(),
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.DETAIL_PAGE_TEXT,
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.AVAILABLE),rules);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(body)).isTrue();
        assertThat(body.semanticStatusCode()).isEqualTo(com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SemanticStatusCode.REVIEW_REQUIRED);
        var blocked=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                "청년 수출 지원",null,null,List.of(),
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(blocked)).isFalse();
    }
    @Test void exactOfficialTitleIgnoresNestedLabelsButRejectsDifferentOrMissingIdentity() {
        var page=org.jsoup.Jsoup.parse("<table class='bbs_default view'><tr><th>제목</th><td>공식 지원 공고</td></tr><tr><td><table><tr><th>제목</th><td>중첩 표</td></tr></table></td></tr></table>");
        assertThatCode(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"공식 지원 공고")).doesNotThrowAnyException();
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고")).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse("<title>공식 지원 공고</title>"),"공식 지원 공고")).isInstanceOf(AssertionError.class);
    }
    @Test void bodyUpperBoundIsReservedBeforeFileBytesAndCannotBeReservedTwice() {
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget();b.reserveBody();assertThat(b.requests).isEqualTo(2);assertThat(b.bytes).isEqualTo(2*1024*1024);
        assertThatThrownBy(b::reserveBody).isInstanceOf(IllegalStateException.class);assertThat(b.saveBytes(78L*1024*1024)).isTrue();assertThat(b.saveBytes(1)).isFalse();assertThat(b.saveBytes(-1)).isFalse();
    }
    @Test void requestReservationsIncludeBodyAndNeverExceedWholeCaseLimit() {
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget();b.reserveBody();
        var request=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(java.net.URI.create(
                "https://www.taebaek.go.kr/www/selectBbsNttView.do?key=352&bbsNo=25&nttNo=184816"));
        for(int i=0;i<42;i++)assertThat(b.selectRequestAllowed(request)).isTrue();assertThat(b.selectRequestAllowed(request)).isFalse();assertThat(b.requests).isEqualTo(44);
        assertThat(b.selectRequestAllowed(com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(java.net.URI.create("https://example.com/")))).isFalse();
        assertThat(b.requests).isEqualTo(44);
    }
    @Test void fileInputUsesOnlyExtractedRoleAndBlocksAndUnknownRemainsUnknown() throws Exception {
        var json=new ObjectMapper();String text="일반자료";var actual=json.valueToTree(Map.of("qualityCode","COMPLETE_TEXT","text",text,"blocks",List.of(Map.of(
                "index",0,"startOffset",0,"endOffset",4,"evidenceScopeId","p1","scopeReliable",true,"locator","page:1"))));
        var observed=json.valueToTree(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(actual));
        var file=AnnouncementAttachmentBbsOfficialObservationTest.selectFileInput(actual,observed);assertThat(file.role()).isEqualTo("UNKNOWN");assertThat(file.text()).isEqualTo(text);assertThat(file.blocks()).hasSize(1);
        assertThat(file.blocks().getFirst().endOffset()).isEqualTo(4);
    }
}
