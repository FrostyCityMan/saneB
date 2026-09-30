package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CheongjuNoticePage;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class CheongjuDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = CheongjuDownloadCases.selectCase();
    private String selectItem(int id,String extension) {
        String name = "지원 공고 " + id + "." + extension;
        return "<li class=p-attach__item><a class=p-attach__link href=\"javascript:goDownLoad('" + name + "','stored_" + id + "." + extension
                + "','/ntishome/file/upload/ofr/ofr/20260110');\"><span>" + name + "</span><i class='p-icon p-icon__arrow-circle-down'></i></a></li>";
    }
    private String selectPage(String files) {
        return "<div id=board class='p-wrap bbs bbs__view'><form name=form2 method=post>"
                + "<input type=hidden name=user_file_nm value=''><input type=hidden name=sys_file_nm value=''><input type=hidden name=file_path value=''></form>"
                + "<table class=bbs_basic><tr><th>제목</th><td>" + sample.title()
                + "</td></tr><tr><th>담당부서</th><td>수출 부서</td></tr><tr><th>내용</th><td>소상공인 융자금 지원</td></tr>"
                + "<tr><th>파일</th><td><ul class=p-attach>" + files + "</ul></td></tr></table></div>";
    }
    @Test void collectsSupportedFormatsUsingOnlyTheOfficialPost() {
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + selectItem(2,"hwpx") + selectItem(3,"pdf")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue(); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.fetchUri().toString()).isEqualTo("https://eminwon.cheongju.go.kr/emwp/jsp/ofr/FileDown.jsp");
            assertThat(file.selectRequest().method()).isEqualTo("POST"); assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
            assertThat(sample.profile().selectApprovedRequest(file.fetchUri())).isFalse();
            assertThat(file.locator().toString()).doesNotContain("지원", "stored_", "ntishome");
        });
    }
    @Test void changedFormsCannotSupplyRequestFields() {
        String page = selectPage(selectItem(1,"pdf"));
        for (String bad : List.of(page.replace("method=post","method=get"),page.replace("method=post","method=post action=https://evil.example"),
                page.replace("name=file_path","name=sys_file_nm"),page.replace("value=''","value='preset'"),
                page.replace("<form name=form2","<form onclick='evil()' name=form2"),page.replace("</form>","<input type=hidden name=extra value=''></form>"))) {
            var result = sample.profile().selectDescriptors(sample.source(),bad);
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).isEmpty(); assertThat(result.warnings()).contains("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
    }
    @Test void partialErrorsAndUnsupportedFilesKeepTheGoodFile() {
        for (String bad : List.of("<li><a href='/unknown'>미확인</a></li>",selectItem(2,"pdf").replace("/20260110","/../private"),
                selectItem(2,"pdf").replace("javascript:goDownLoad(","javascript:evil();goDownLoad("),selectItem(2,"pdf").replace("<span>","<span onmouseover='evil()'>"))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + selectItem(2,"xlsx")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void absentOrUnknownDecorationDoesNotDiscardTheValidFile() {
        String one = selectItem(1,"pdf");
        assertThat(sample.profile().selectDescriptors(sample.source(),selectPage(one.replace("<i class='p-icon p-icon__arrow-circle-down'></i>",""))).complete()).isTrue();
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(one.replace("</i>","새 안내</i>")));
        assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
    }
    @Test void emptyChangedDuplicateAndLimitRemainDistinct() {
        var profile = sample.profile(); assertThat(profile.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for (String bad : List.of("<p>에러</p>",selectPage("") + selectPage(""),selectPage("").replace("<th>파일</th>","<th>기타</th>"),selectPage("<li></li>")))
            assertThat(profile.selectDescriptors(sample.source(),bad).complete()).isFalse();
        String one = selectItem(1,"pdf"); assertThat(profile.selectDescriptors(sample.source(),selectPage(one + one)).descriptors()).hasSize(1);
        var conflict = profile.selectDescriptors(sample.source(),selectPage(one + one.replace("지원 공고","다른 공고")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limit = profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void sourceIdentityAndRequestBoundariesRemainCompatible() {
        var source = sample.source(); var profile = sample.profile(); var normalizer = new AnnouncementSourceIdentityNormalizer();
        assertThat(source.providerNoticeId()).isEqualTo(normalizer.hash(source.sourceUrl())); String raw = profile.selectDetailUri(source).toString();
        assertThat(normalizer.canonicalizeUrl(raw)).isEqualTo(source.sourceUrl());
        for (String bad : List.of(raw + "&extra=x",raw + "&notAncmtMgtNo=1",raw + "#x",raw.replace("https:","http:"),
                raw.replace("www.cheongju.go.kr","evil.example"),raw.replace("key=281","key=282"),raw.replace("nowDongGn=","nowDongGn=unapproved"),raw.replace("/www/","/%77ww/")))
            assertThatThrownBy(() -> CheongjuNoticePage.selectDetailUri(URI.create(bad))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"0".repeat(64),source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var files = profile.selectDescriptors(source,selectPage(selectItem(1,"pdf") + selectItem(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(URI.create(raw),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void titleBodyAndNestedTablesRemainIndependent() throws Exception {
        var page = Jsoup.parse(selectPage(selectItem(1,"pdf")) + "<a href='/unrelated.pdf'>무관한 파일</a>");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(CheongjuNoticePage.selectContent(page).text()).isEqualTo("소상공인 융자금 지원");
        var nested = Jsoup.parse(selectPage("").replace("소상공인 융자금 지원</td>","<table><tr><th>제목</th><td>본문 표</td></tr></table></td>"));
        assertThat(CheongjuNoticePage.selectTitle(CheongjuNoticePage.selectRoot(nested)).text()).isEqualTo(sample.title());
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @Test void catalogBudgetAndReferenceDoNotClaimOperatingApproval() throws Exception {
        var json = new ObjectMapper(); var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry = StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source())); assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"))).contains("'LGS-000136', '" + sample.listUrl() + "'");
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
