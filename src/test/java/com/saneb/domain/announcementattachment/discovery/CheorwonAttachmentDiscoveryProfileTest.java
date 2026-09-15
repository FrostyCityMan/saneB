package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 관측한 다운로드 query와 엄격한 본문 구조 계약의 합성 시험. 실제 사이트 QA를 대체하지 않는다. */
class CheorwonAttachmentDiscoveryProfileTest {
    private final AttachmentDiscoveryProfile profile = new StandardBbsAttachmentProfileConfiguration().selectCheorwonProfileDetails();
    private static final String ORIGIN = "https://www.cwg.go.kr";
    private static final String DETAIL = ORIGIN + "/www/selectBbsNttView.do?key=1226&bbsNo=25&nttNo=288915";
    private static final String QUERY = "atchmnflNo=174651&bbsNo=25&nttNo=288915";
    @TempDir Path directory;

    static AttachmentDiscoveryProfile.Source selectSource(String noticeId) {
        return selectSourceUrl(DETAIL.replace("288915", noticeId));
    }
    private static AttachmentDiscoveryProfile.Source selectSourceUrl(String url) {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                url, "LGS-000129", "SAEOL_GOSI");
    }
    private static String selectPage(String items) {
        return "<div class='p-wrap bbs bbs__view'><table class='p-table block'><tbody>"
                + "<tr><td><span class='p-table__subject_text'>지원사업 공고</span></td></tr>"
                + "<tr><td title='내용'>소상공인 지원사업 안내</td></tr><tr><th scope='row'>파일</th><td>"
                + "<ul class='p-attach'>" + items + "</ul></td></tr></tbody></table></div>";
    }
    private static String selectItem(String query, String name) {
        return "<li class='p-attach__item'><a class='p-attach__link' href='./downloadBbsFile.do?" + query
                + "'><span class='p-icon p-icon__hwpx'>hwpx 문서</span><span>" + name + "</span></a></li>";
    }
    private static AttachmentPinnedDownloadClient.Request selectRequest(String query) {
        return AttachmentPinnedDownloadClient.Request.selectGet(URI.create(ORIGIN + "/www/downloadBbsFile.do?" + query));
    }

    @Test void sourceBindingDoesNotBorrowTaebaekBoardOrHeaderExceptions() {
        assertThat(profile.selectSourceBindings()).containsExactly(new AttachmentDiscoveryProfile.SourceBinding("LGS-000129", "SAEOL_GOSI"));
        assertThat(profile.selectApprovedHosts()).containsExactly("www.cwg.go.kr");
        assertThat(profile.selectLegacyBinaryContentTypes()).isEmpty();
        assertThat(profile.selectUtf8DispositionOctets()).isFalse();
        var source = selectSource("288915");
        assertThat(profile.selectDetailUri(source)).isEqualTo(URI.create(DETAIL));
        assertThat(profile.selectDetailUri(selectSourceUrl(DETAIL + "&pageIndex=1&searchKrwd=%EC%A7%80%EC%9B%90"))).isEqualTo(URI.create(DETAIL));
        for (var invalid : List.of(
                new AttachmentDiscoveryProfile.Source("BIZINFO", source.providerNoticeId(), DETAIL, "LGS-000129", "SAEOL_GOSI"),
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", "0".repeat(64), DETAIL, "LGS-000129", "SAEOL_GOSI"),
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", source.providerNoticeId(), DETAIL, "LGS-000121", "SAEOL_GOSI"),
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", source.providerNoticeId(), DETAIL, "LGS-000129", "SPRING_BBS")))
            assertThatThrownBy(() -> profile.selectDetailUri(invalid)).hasMessage("PROFILE_REQUIRED");
        for (String invalid : List.of(DETAIL.replace("1226", "352"), DETAIL.replace("bbsNo=25", "bbsNo=24"),
                DETAIL.replace("https:", "http:"), DETAIL.replace("www.cwg.go.kr", "www.taebaek.go.kr"),
                DETAIL.replace(".do?", ".do;CWG_JSESSIONID=SYNTHETIC?"), DETAIL + "&bbsNo=25", DETAIL + "&unknown=1"))
            assertThatThrownBy(() -> profile.selectDetailUri(selectSourceUrl(invalid))).hasMessage("PROFILE_REQUIRED");
    }

    @Test void discoversEachFileWithCurrentNoticeBindingWithoutGuessingDocumentRole() {
        var result = profile.selectDescriptors(selectSource("288915"), selectPage(selectItem(QUERY, "공고.hwpx")
                + selectItem(QUERY.replace("174651", "174652"), "신청.hwp")
                + selectItem(QUERY.replace("174651", "174653"), "안내.pdf")
                + selectItem(QUERY.replace("174651", "174654"), "포스터.jpg")));
        assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("HWPX", "HWP", "PDF", null);
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::downloadAllowed).containsExactly(true, true, true, false);
        assertThat(result.descriptors()).allSatisfy(file -> {
            assertThat(file.documentRole()).isEqualTo("UNKNOWN"); assertThat(file.postForm()).isEmpty();
            assertThat(file.locator().identifiers()).containsEntry("noticeId", "288915");
            assertThat(file.locator().identifiers().get("attachmentId")).matches("[0-9a-f]{64}");
            assertThat(profile.selectApprovedRequest(file.selectRequest())).isTrue();
            assertThat(file.toString()).doesNotContain("atchmnflNo", "공고.hwpx");
        });
    }

    @ParameterizedTest @ValueSource(strings = {
            "atchmnflNo=174651", "atchmnflNo=174651&bbsNo=25", "atchmnflNo=174651&nttNo=288915",
            "atchmnflNo=174651&bbsNo=24&nttNo=288915", "atchmnflNo=174651&bbsNo=25&nttNo=0",
            "atchmnflNo=0&bbsNo=25&nttNo=288915", "atchmnflNo=174651&bbsNo=25&nttNo=288915&bbsNo=25",
            "atchmnflNo=174651&bbsNo=25&nttNo=288915&key=1226", "atchmnflNo=174651&bbsNo=25&nttNo=288915&extra=1",
            "atchmnflNo=174651&bbsNo=25&nttNo=%0A", "atchmnflNo=174651&bbsNo=25&nttNo=288915%26extra%3D1"})
    void rejectsIncompleteOrAmbiguousDownloadParameters(String query) {
        assertThat(profile.selectApprovedRequest(selectRequest(query))).isFalse();
        var result = profile.selectDescriptors(selectSource("288915"), selectPage(selectItem(query, "공고.hwpx")));
        assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).isEmpty();
        assertThat(result.warnings()).containsExactly("ATTACHMENT_LINK_UNRESOLVED");
    }

    @Test void anotherNoticeLinkIsNotIncludedEvenWhenItsEndpointIsValid() {
        String another = QUERY.replace("288915", "288916");
        assertThat(profile.selectApprovedRequest(selectRequest(another))).isTrue(); // 기관 endpoint와 현재 공고 소속은 별도 검사다.
        var result = profile.selectDescriptors(selectSource("288915"), selectPage(selectItem(QUERY, "공고.hwpx")
                + selectItem(another, "다른공고.hwpx")));
        assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.complete()).isFalse();
        assertThat(result.descriptors()).hasSize(1);
        assertThat(result.descriptors().getFirst().fetchUri()).isEqualTo(selectRequest(QUERY).uri());
    }

    @Test void redirectCannotChangeNoticeFileOrEndpointAndRejectsPost() throws Exception {
        var initial = selectRequest(QUERY);
        var same = selectRequest("nttNo=288915&bbsNo=25&atchmnflNo=174651");
        assertThat(profile.selectApprovedRequest(initial, same)).isTrue();
        assertThat(profile.selectApprovedRequest(initial, new AttachmentPinnedDownloadClient.Request(initial.uri(), "POST", Map.of("test", "value")))).isFalse();
        assertThat(profile.selectApprovedRequest(null, initial)).isFalse();
        assertThat(profile.selectApprovedRequest(initial, null)).isFalse();
        var detail = AttachmentPinnedDownloadClient.Request.selectGet(URI.create(DETAIL));
        assertThat(profile.selectApprovedRequest(detail, detail)).isTrue();
        assertThat(profile.selectApprovedRequest(detail, AttachmentPinnedDownloadClient.Request.selectGet(URI.create(DETAIL.replace("288915", "288916"))))).isFalse();
        // 실제 worker/정책 QA가 사용하는 공통 흐름이 최초 요청을 redirect predicate까지 전달하는지 확인한다.
        AttachmentProfileDownloadFlow.selectDownload(profile, initial, directory.resolve("not-created.bin"), 1000, (request, limit, approved) -> {
            assertThat(approved.test(initial)).isTrue(); assertThat(approved.test(same)).isTrue();
            assertThat(approved.test(selectRequest(QUERY.replace("288915", "288916")))).isFalse();
            assertThat(approved.test(selectRequest(QUERY.replace("174651", "174652")))).isFalse();
            return null;
        });
        assertThat(profile.selectApprovedRequest(initial, detail)).isFalse();
        assertThat(profile.selectApprovedRequest(detail, initial)).isFalse();
    }

    @Test void unobservedPreviewAndResidualLinksRemainIncomplete() {
        String item = selectItem(QUERY, "공고.hwpx");
        for (String changed : List.of(item.replace("</li>", "<a class='p-attach__preview' href='./previewBbsFile.do?" + QUERY + "'>미리보기</a></li>"),
                item + "<li><a href='/other'>숨은 첨부</a></li>", item + "<li><button>첨부 다운로드</button></li>",
                item.replace("<span>공고", "<span onclick='invalid()'>공고"), item + "첨부 일부 생략"))
            assertThat(profile.selectDescriptors(selectSource("288915"), selectPage(changed)).complete()).isFalse();
        for (String changed : List.of(selectRequest(QUERY).uri().toString().replace("www.cwg.go.kr", "127.0.0.1"),
                selectRequest(QUERY).uri().toString().replace("https:", "http:"),
                selectRequest(QUERY).uri().toString().replace("downloadBbsFile", "previewBbsFile"),
                selectRequest(QUERY).uri() + "#other"))
            assertThat(profile.selectApprovedRequest(URI.create(changed))).isFalse();
    }

    @Test void onlyOwnedCompleteFileCellMayReportNoFiles() {
        String empty = selectPage("");
        assertThat(profile.selectDescriptors(selectSource("288915"), empty).status()).isEqualTo("NO_FILES");
        assertThat(profile.selectDescriptors(selectSource("288915"), empty.replace("<ul class='p-attach'></ul>", "")).status()).isEqualTo("NO_FILES");
        for (String changed : List.of("<main>페이지 오류</main>", empty + empty, empty.replace("p-table block", "p-table"),
                empty.replace("title='내용'", "title='변경'"), empty.replace("p-table__subject_text", "changed"),
                empty.replace("파일</th>", "파일이동</th>"), empty.replace("<ul class='p-attach'></ul>", "<a href='/download'>첨부</a>"),
                empty.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"),
                empty.replace("<span class='p-table__subject_text'>지원사업 공고</span>", "<table><tr><td><span class='p-table__subject_text'>중첩 제목</span></td></tr></table>")))
            assertThat(profile.selectDescriptors(selectSource("288915"), changed).complete()).isFalse();
    }

    @Test void duplicateConflictAndLimitCannotHideUnprocessedFiles() {
        String item = selectItem(QUERY, "공고.hwpx");
        assertThat(profile.selectDescriptors(selectSource("288915"), selectPage(item + item)).descriptors()).hasSize(1);
        assertThat(profile.selectDescriptors(selectSource("288915"), selectPage(item + item.replace("공고.hwpx", "다른이름.hwpx"))).complete()).isFalse();
        String items = IntStream.rangeClosed(1, 11).mapToObj(id -> selectItem(QUERY.replace("174651", Integer.toString(id)), "공고" + id + ".hwpx"))
                .reduce("", String::concat);
        var result = profile.selectDescriptors(selectSource("288915"), selectPage(items));
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(result.complete()).isFalse();
        assertThat(result.descriptors()).hasSize(10); assertThat(result.warnings()).containsExactly("ATTACHMENT_FILE_LIMIT");
    }
}
