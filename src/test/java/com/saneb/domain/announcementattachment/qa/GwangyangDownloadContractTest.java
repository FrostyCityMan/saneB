package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GwangyangNoticePage;
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

class GwangyangDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = GwangyangDownloadCases.selectCase();
    private String selectItem(int id,String extension) {
        String name = "지원 공고 " + id + "." + extension;
        return "<a class='btn_type download' href=\"javascript:goDownLoad('" + name + "','stored_" + id + "." + extension
                + "','/ntishome/file/upload/ofr/ofr/20260821')\"><span>" + name + "</span></a><br/>";
    }
    private String selectPage(String files) {
        return "<div class='p-wrap bbs bbs_view'><table class='p-table block'><thead><tr><th class=bbs_tit>" + sample.title()
                + "</th></tr></thead><tbody><tr><td>수출 부서</td></tr><tr><td class=view_content>소상공인 융자금 지원</td></tr>"
                + "<tr><th>첨부파일</th><td class=view_file>" + files + "</td></tr></tbody></table></div>"
                + "<form name=nnn method=post action='https://eminwon.gwangyang.go.kr/emwp/jsp/ofr/FileDownNew.jsp'>"
                + "<input type=hidden name=user_file_nm value=''><input type=hidden name=sys_file_nm value=''><input type=hidden name=file_path value=''></form>";
    }
    @Test void collectsSupportedFormatsUsingOnlyTheOfficialPublicPost() {
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + selectItem(2,"hwpx") + selectItem(3,"pdf")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue(); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.fetchUri().toString()).isEqualTo("https://eminwon.gwangyang.go.kr/emwp/jsp/ofr/FileDownNew.jsp");
            assertThat(file.selectRequest().method()).isEqualTo("POST");
            assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
            assertThat(sample.profile().selectApprovedRequest(file.fetchUri())).isFalse();
            assertThat(file.locator().toString()).doesNotContain("지원", "stored_", "ntishome");
        });
    }
    @Test void changedOrActiveDownloadFormsCannotSupplyRequestFields() {
        String page = selectPage(selectItem(1,"hwpx"));
        for (String bad : List.of(page.replace("method=post","method=get"),page.replace("eminwon.gwangyang.go.kr","evil.example"),
                page.replace("name=file_path","name=sys_file_nm"),page.replace("value=''","value='preset'"),
                page.replace("<form name=nnn","<form onclick='evil()' name=nnn"),page.replace("</form>","<input type=hidden name=extra value=''></form>"))) {
            var result = sample.profile().selectDescriptors(sample.source(),bad);
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).isEmpty();
            assertThat(result.warnings()).contains("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
    }
    @Test void badLinksAndUnsupportedFilesAreSeparateFromGoodFiles() {
        for (String bad : List.of("<a href='/unknown'>미확인</a>",selectItem(2,"pdf").replace("/20260821","/../private"),
                selectItem(2,"pdf").replace("javascript:goDownLoad(","javascript:evil();goDownLoad("),
                selectItem(2,"pdf").replace("<span>","<span onmouseover='evil()'>"))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + selectItem(2,"xlsx")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void emptyChangedDuplicateAndLimitRemainDistinct() {
        var profile = sample.profile();
        assertThat(profile.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for (String bad : List.of("<p>에러</p>",selectPage("") + selectPage(""),selectPage("").replace("class=view_file","class=other"),selectPage("<li></li>")))
            assertThat(profile.selectDescriptors(sample.source(),bad).complete()).isFalse();
        String one = selectItem(1,"hwp");
        assertThat(profile.selectDescriptors(sample.source(),selectPage(one + one)).descriptors()).hasSize(1);
        var conflict = profile.selectDescriptors(sample.source(),selectPage(one + one.replace("지원 공고","다른 공고")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limit = profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void persistedCollectorIdentityAndBoundedCategoryEncodingRemainCompatible() {
        var source = sample.source(); var profile = sample.profile(); var normalizer = new AnnouncementSourceIdentityNormalizer();
        assertThat(source.sourceUrl()).contains("type_code=02%252C04");
        assertThat(source.providerNoticeId()).isEqualTo(normalizer.hash(source.sourceUrl()));
        String raw = profile.selectDetailUri(source).toString();
        assertThat(raw).contains("type_code=02,04");
        assertThat(normalizer.canonicalizeUrl(raw)).isEqualTo(source.sourceUrl());
        assertThat(profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),normalizer.hash(normalizer.canonicalizeUrl(raw)),raw,source.localSourceCode(),source.listParserProfileCode())).toString()).isEqualTo(raw);
        for (String bad : List.of(raw + "&extra=x",raw + "&seq=1",raw + "#x",raw.replace("https:","http:"),
                raw.replace("gwangyang.go.kr","evil.example"),raw.replace("type_code=02,04","type_code=02%25252C04"),
                raw.replace("a10909020000","a10909010000"),raw.replace("/saeol/","/%73aeol/")))
            assertThatThrownBy(() -> GwangyangNoticePage.selectDetailUri(URI.create(bad))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"0".repeat(64),source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var files = profile.selectDescriptors(source,selectPage(selectItem(1,"pdf") + selectItem(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(URI.create(raw),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void bodyTitleAndNestedTablesRemainIndependent() throws Exception {
        var page = Jsoup.parse(selectPage(selectItem(1,"hwpx")) + "<a href='/unrelated.pdf'>무관한 파일</a>");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(GwangyangNoticePage.selectContent(page).text()).isEqualTo("소상공인 융자금 지원");
        var nested = Jsoup.parse(selectPage("").replace("소상공인 융자금 지원</td>","<table><tr><th class=bbs_tit>본문 표</th></tr></table></td>"));
        assertThat(GwangyangNoticePage.selectTitle(GwangyangNoticePage.selectRoot(nested)).text()).isEqualTo(sample.title());
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @Test void catalogBudgetAndReferenceDoNotClaimOperatingApproval() throws Exception {
        var json = new ObjectMapper(); var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry = StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source())); assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"))).contains("'LGS-000182', '" + sample.listUrl() + "'");
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
