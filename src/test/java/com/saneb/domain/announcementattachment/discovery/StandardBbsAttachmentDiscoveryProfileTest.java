package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class StandardBbsAttachmentDiscoveryProfileTest {
    record Case(AttachmentDiscoveryProfile profile, String sourceCode, String host, String board, String menu) { }
    static Stream<Case> selectCases() {
        var config = new StandardBbsAttachmentProfileConfiguration();
        return Stream.of(new Case(config.selectTaebaekProfileDetails(), "LGS-000121", "www.taebaek.go.kr", "25", "352"),
                new Case(config.selectHoengseongProfileDetails(), "LGS-000125", "www.hsg.go.kr", "65", "821"),
                new Case(config.selectYeongwolProfileDetails(), "LGS-000126", "www.yw.go.kr", "17", "273"),
                new Case(config.selectWonjuProfileDetails(), "LGS-000118", "www.wonju.go.kr", "140", "216"),
                new Case(config.selectJecheonProfileDetails(), "LGS-000138", "www.jecheon.go.kr", "18", "5233"),
                new Case(config.selectBoeunProfileDetails(), "LGS-000139", "www.boeun.go.kr", "66", "194"));
    }
    static AttachmentDiscoveryProfile.Source selectSource(Case sample, String noticeId) {
        return selectSourceUrl(sample, "https://" + sample.host() + StandardBbsAttachmentDiscoveryProfile.DETAIL
                + "?key=" + sample.menu() + "&bbsNo=" + sample.board() + "&nttNo=" + noticeId);
    }
    static AttachmentDiscoveryProfile.Source selectSourceUrl(Case sample, String url) {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                url, sample.sourceCode(), List.of("140", "18", "66").contains(sample.board()) ? "HEURISTIC_NOTICE" : "SPRING_BBS");
    }
    static boolean selectCompact(Case sample) { return List.of("65", "140", "18", "66").contains(sample.board()); }
    static String selectPage(Case sample, String items) {
        boolean compact = selectCompact(sample);
        boolean wonju = sample.board().equals("140");
        return (wonju ? "<div class='bbs_wrap'><div class='p-wrap bbs bbs__view'><table class='p-table'>"
                : compact ? "<div class='p-wrap bbs bbs__view'><table class='p-table block'>" : "<table class='bbs_default view'>")
                + "<tr><th>제목</th><td>" + (List.of("65", "66").contains(sample.board()) ? "<span class='p-table__subject_text'>소상공인 지원 공고</span>" : "소상공인 지원 공고")
                + "</td></tr><tr><td title='내용'>지원사업 안내</td></tr><tr><th scope='row'>" + (sample.board().equals("18") ? "첨부파일" : "파일") + "</th><td>"
                + "<ul class='" + (compact ? "p-attach" : "view_attach") + "'>" + items + "</ul></td></tr></table>" + (wonju ? "</div></div>" : compact ? "</div>" : "");
    }
    static String selectQuery(Case sample, String id) {
        return (sample.board().equals("140") ? "key=216&" : selectCompact(sample) ? "" : sample.board().equals("25") ? "key=352&" : "bbsNo=17&") + "atchmnflNo=" + id;
    }
    static String selectItem(Case sample, String id, String name, boolean preview) {
        String download = "./downloadBbsFile.do?" + selectQuery(sample, id);
        if (sample.board().equals("18")) return "<li class='p-attch__item'><a class='p-attach__link' href='" + download
                + "'><span class='p-icon'>문서</span><span>" + name + "</span>" + selectSvg(true) + "</a>"
                + (preview ? "<a class='p-attach__preview p-button' href='/previewBbs.do?atchmnflNo=" + id + "'>미리보기" + selectSvg(false) + "</a>" : "") + "</li>";
        if (selectCompact(sample)) return "<li class='p-attach__item'><a class='p-attach__link' href='" + download
                + "'><span class='p-icon'>문서</span><span>" + name + "</span></a>"
                + (preview ? "<a class='p-attach__preview' href='" + (sample.board().equals("140") ? "./previewUrl.do?key=216&" : sample.board().equals("66") ? "./previewBbsFile.do?key=194&bbsNo=66&" : "./previewBbsFile.do?")
                    + "atchmnflNo=" + id + "'>미리보기</a>" : "") + "</li>";
        String href = sample.board().equals("25") ? "/www/previewUrl.do?key=352&atchmnflNo=" + id
                : "/common/program/synap.jsp?fileName=/DATA/bbs/17/FIXTURE-UUID.pdf&nttNo=123&FileIndex=0";
        return "<li><div class='down_view'><span><img src='/common/images/board/file/ico_pdf.gif' alt='문서'/>" + name
                + "</span><a class='file_down' href='" + download + "'>다운로드<i></i></a>"
                + (preview ? "<a class='file_view' href='" + href + "'>미리보기<i></i></a>" : "") + "</div></li>";
    }
    static String selectSvg(boolean download) {
        return "<svg width='20' height='23' fill='#3b3e42' focusable='false'" + (download ? " class='margin_l_5'" : "")
                + "><use xlink:href='/common/images/program/p-icon.svg#" + (download ? "arrow-circle-down' y='2'" : "search'") + "></use></svg>";
    }
    @ParameterizedTest @MethodSource("selectCases") void discoversAllSupportedAndUnsupportedWithoutInferringRole(Case sample) {
        String items = selectItem(sample, "1", "공고문.pdf", true) + selectItem(sample, "2", "신청.hwp", false)
                + selectItem(sample, "3", "자료.hwpx", false) + selectItem(sample, "4", "참고.xlsx", false);
        var result = sample.profile().selectDescriptors(selectSource(sample, "123"), selectPage(sample, items));
        assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF", "HWP", "HWPX", null);
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::downloadAllowed).containsExactly(true, true, true, false);
        assertThat(result.descriptors()).allSatisfy(d -> {
            assertThat(d.documentRole()).isEqualTo("UNKNOWN"); assertThat(d.postForm()).isEmpty();
            assertThat(d.locator().identifiers()).containsEntry("noticeId", "123");
            assertThat(d.locator().identifiers().get("attachmentId")).matches("[0-9a-f]{64}");
            assertThat(sample.profile().selectApprovedRequest(d.selectRequest())).isTrue();
            assertThat(d.toString()).doesNotContain("공고문", "atchmnflNo");
        });
    }
    @ParameterizedTest @MethodSource("selectCases") void sourceIdentityAndExactBoardAreMandatory(Case sample) {
        var profile = sample.profile(); var source = selectSource(sample, "123");
        for (var altered : List.of(new AttachmentDiscoveryProfile.Source("BIZINFO", source.providerNoticeId(), source.sourceUrl(), sample.sourceCode(), "SPRING_BBS"),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), "a".repeat(64), source.sourceUrl(), sample.sourceCode(), "SPRING_BBS"),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(), source.sourceUrl(), "LGS-000000", "SPRING_BBS"),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(), source.sourceUrl(), sample.sourceCode(), "OTHER")))
            assertThatThrownBy(() -> profile.selectDetailUri(altered)).hasMessage("PROFILE_REQUIRED");
        for (String url : List.of(source.sourceUrl().replace("bbsNo=" + sample.board(), "bbsNo=999"),
                source.sourceUrl().replace("key=" + sample.menu(), "key=999"), source.sourceUrl() + "&bbsNo=" + sample.board(),
                source.sourceUrl() + "&redirect=other", source.sourceUrl() + "#fragment", source.sourceUrl() + "&searchKrwd=%0A",
                source.sourceUrl().replace("https://", "https://fixture@"), source.sourceUrl().replace("/www/", "/www/../www/"),
                source.sourceUrl().replace("selectBbs", "%73electBbs"), source.sourceUrl().replace("nttNo=123", "nttNo=0")))
            assertThatThrownBy(() -> profile.selectDetailUri(selectSourceUrl(sample, url))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> profile.selectDetailUri("123")).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> profile.selectDescriptors("123", "")).hasMessage("PROFILE_REQUIRED");
    }
    @ParameterizedTest @MethodSource("selectCases") void stripsOnlyKnownPresentationParametersAndNeverTransmitsHttp(Case sample) {
        var profile = sample.profile(); var source = selectSource(sample, "123");
        assertThat(profile.selectDetailUri(selectSourceUrl(sample, source.sourceUrl()
                + "&searchCtgry=&searchCnd=all&searchKrwd=%EC%A7%80%EC%9B%90&pageIndex=1&pageUnit=10&integrDeptCode=")))
                .isEqualTo(profile.selectDetailUri(source));
        var http = selectSourceUrl(sample, source.sourceUrl().replace("https:", "http:"));
        if (sample.board().equals("25")) assertThat(profile.selectDetailUri(http)).isEqualTo(profile.selectDetailUri(source));
        else assertThatThrownBy(() -> profile.selectDetailUri(http)).hasMessage("PROFILE_REQUIRED");
        assertThat(profile.selectApprovedRequest(URI.create(http.sourceUrl()))).isFalse();
    }
    @ParameterizedTest @MethodSource("selectCases") void missingAndPartialStructureCannotBecomeNoFiles(Case sample) {
        var profile = sample.profile(); var source = selectSource(sample, "123");
        assertThat(profile.selectDescriptors(source, selectPage(sample, "")).status()).isEqualTo("NO_FILES");
        for (String html : List.of("<html>로그인</html>", selectPage(sample, "").replace("파일", "자료"),
                selectPage(sample, "").replace("title='내용'", "title='변경'"), selectPage(sample, "") + selectPage(sample, "")))
            assertThat(profile.selectDescriptors(source, html).status()).isEqualTo("FAILED");
        String first = selectItem(sample, "1", "공고.pdf", false);
        for (String extra : List.of("<li>찾지 못한 파일.pdf</li>", "<li><a href='/other'>첨부</a></li>", "<li><button>첨부</button></li>",
                "<li><img src='/unknown'></li>", "<li><script>unknown()</script></li>", "첨부 목록 일부 생략")) {
            var result = profile.selectDescriptors(source, selectPage(sample, first + extra));
            assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
    }
    @ParameterizedTest @MethodSource("selectCases") void strictNetworkBoundaryAndPreviewIsNeverRequested(Case sample) {
        var profile = sample.profile();
        var d = profile.selectDescriptors(selectSource(sample, "123"), selectPage(sample, selectItem(sample, "1", "공고.pdf", true))).descriptors().getFirst();
        String uri = d.fetchUri().toASCIIString();
        for (String invalid : List.of(uri + "&atchmnflNo=2", uri + "&unknown=value", uri + "#other", uri.replace("https:", "http:"),
                uri.replace(sample.host(), "127.0.0.1"), uri.replace(sample.host(), sample.host() + ":444"),
                uri.replace("downloadBbsFile", "%64ownloadBbsFile"), uri.replace("/www/", "/www/../www/"),
                uri.replace("downloadBbsFile", "previewBbsFile")))
            assertThat(profile.selectApprovedRequest(URI.create(invalid))).as(invalid).isFalse();
        assertThat(profile.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(d.fetchUri(), "POST", Map.of("test", "value")))).isFalse();
        String item = selectItem(sample, "1", "공고.pdf", true);
        for (String bad : List.of(item.replace("./downloadBbsFile", "https://unapproved.go.kr/www/downloadBbsFile"),
                item.replace("./downloadBbsFile", "../www/downloadBbsFile"), item.replace("<a ", "<a onclick='other()' ")))
            assertThat(profile.selectDescriptors(selectSource(sample, "123"), selectPage(sample, bad)).complete()).isFalse();
        assertThat(profile.selectDescriptors(selectSource(sample, "123"), "<base href='https://unapproved.go.kr'>" + selectPage(sample, item))
                .descriptors().getFirst().fetchUri().getHost()).isEqualTo(sample.host());
        String invalidPreview = item.replace("previewBbsFile.do?atchmnflNo=1", "previewBbsFile.do?atchmnflNo=2")
                .replace("previewUrl.do?key=352&atchmnflNo=1", "previewUrl.do?key=352&atchmnflNo=2").replace("&nttNo=123&", "&nttNo=999&");
        invalidPreview = invalidPreview.replace("previewUrl.do?key=216&atchmnflNo=1", "previewUrl.do?key=216&atchmnflNo=2");
        invalidPreview = invalidPreview.replace("previewBbs.do?atchmnflNo=1", "previewBbs.do?atchmnflNo=2");
        invalidPreview = invalidPreview.replace("previewBbsFile.do?key=194&bbsNo=66&atchmnflNo=1", "previewBbsFile.do?key=194&bbsNo=66&atchmnflNo=2");
        var partial = profile.selectDescriptors(selectSource(sample, "123"), selectPage(sample, invalidPreview));
        assertThat(partial.complete()).isFalse(); assertThat(partial.descriptors()).hasSize(1);
    }
    @ParameterizedTest @MethodSource("selectCases") void duplicateAndOverflowCannotHideIncompleteFiles(Case sample) {
        var profile = sample.profile(); var source = selectSource(sample, "123"); String first = selectItem(sample, "1", "공고.pdf", false);
        assertThat(profile.selectDescriptors(source, selectPage(sample, first + first)).descriptors()).hasSize(1);
        assertThat(profile.selectDescriptors(source, selectPage(sample, first + selectItem(sample, "1", "다른.pdf", false))).complete()).isFalse();
        String many = IntStream.rangeClosed(1, 11).mapToObj(i -> selectItem(sample, "" + i, "공고.pdf", false)).collect(java.util.stream.Collectors.joining());
        var limit = profile.selectDescriptors(source, selectPage(sample, many));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.complete()).isFalse(); assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectCases") void doesNotSwallowHiddenLinksInDisplayNameOrOutsideList(Case sample) {
        var profile = sample.profile(); var source = selectSource(sample, "123");
        String valid = selectItem(sample, "1", "공고.pdf", false);
        for (String name : List.of("<span href='/other'>추가</span>공고.pdf", "<img src='/other'>공고.pdf", "<button>파일</button>공고.pdf")) {
            var result = profile.selectDescriptors(source, selectPage(sample, selectItem(sample, "1", name, false)));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).isEmpty();
        }
        assertThat(profile.selectDescriptors(source, selectPage(sample, valid).replace("</ul>", "</ul><a href='/other'>첨부</a>")).complete()).isFalse();
        assertThat(profile.selectDescriptors(source, selectPage(sample, valid).replace("</ul>", "</ul><img src='/other'>")).complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectCases") void formatWordWithoutFilenameExtensionIsNotDownloadApproval(Case sample) {
        for (String name : List.of("PDF", "HWP", "HWPX", ".pdf", "공고.exe")) {
            var result = sample.profile().selectDescriptors(selectSource(sample, "123"), selectPage(sample, selectItem(sample, "1", name, false)));
            assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.descriptors()).hasSize(1);
            assertThat(result.descriptors().getFirst().expectedFormat()).isNull();
            assertThat(result.descriptors().getFirst().downloadAllowed()).isFalse();
        }
    }
    @Test void sessionPathIsOnlyStrippedForMeasuredHoengseongWithoutChangingStoredIdentity() {
        for (Case sample : selectCases().toList()) {
            var source = selectSource(sample, "123");
            // 합성 경로 매개변수이며 실제 세션이 아니다.
            var session = selectSourceUrl(sample, source.sourceUrl().replace(".do?", ".do;jsessionid=FIXTURE-ONLY?"));
            if (sample.board().equals("65")) {
                assertThat(sample.profile().selectDetailUri(session)).isEqualTo(sample.profile().selectDetailUri(source));
                assertThat(session.providerNoticeId()).isNotEqualTo(source.providerNoticeId());
            } else assertThatThrownBy(() -> sample.profile().selectDetailUri(session)).hasMessage("PROFILE_REQUIRED");
            assertThat(sample.profile().selectApprovedRequest(URI.create(session.sourceUrl()))).isFalse();
        }
    }
    @Test void wonjuDoesNotBorrowOtherBoardsTitleOrDownloadRules() {
        var sample = selectCases().toList().get(3); var profile = sample.profile(); var source = selectSource(sample, "123");
        assertThat(profile.selectSourceBindings()).containsExactly(new AttachmentDiscoveryProfile.SourceBinding("LGS-000118", "HEURISTIC_NOTICE"));
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(),
                source.sourceUrl(), source.localSourceCode(), "SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        String item = selectItem(sample, "1", "공고.hwpx", true), page = selectPage(sample, item);
        for (String altered : List.of(page.replace("bbs_wrap", "different"),
                page.replace("<th>제목</th><td>소상공인 지원 공고</td>", "<td><table><tr><th>제목</th><td>중첩 제목</td></tr></table></td>"),
                page.replace("<th scope='row'>파일</th>", "<th>변경</th>"),
                page.replace("downloadBbsFile.do?key=216&", "downloadBbsFile.do?key=999&"),
                page.replace("downloadBbsFile.do?key=216&", "downloadBbsFile.do?"),
                page.replace("previewUrl.do?key=216&", "previewBbsFile.do?")))
            assertThat(profile.selectDescriptors(source, altered).complete()).isFalse();
    }

    @Test void jecheonAcceptsOnlyBlankPresentationIdAfterActualCollectorCanonicalization() {
        var sample = selectCases().toList().get(4); var profile = sample.profile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String raw = "https://www.jecheon.go.kr/www/selectBbsNttView.do?key=5233&id=&&bbsNo=18&nttNo=123&searchCtgry=&searchCnd=&searchKrwd=&pageIndex=1&integrDeptCode=";
        String stored = normalizer.canonicalizeUrl(raw);
        var source = new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(stored), stored, "LGS-000138", "HEURISTIC_NOTICE");
        assertThat(profile.selectDetailUri(source)).isEqualTo(profile.selectDetailUri(selectSource(sample, "123")));
        assertThat(profile.selectApprovedRequest(URI.create(stored))).isFalse();
        for (String suffix : List.of("&id=other", "&id=%20", "&id=&id=", "&id=&&", "&unknown="))
            assertThatThrownBy(() -> profile.selectDetailUri(selectSourceUrl(sample, selectSource(sample, "123").sourceUrl() + suffix))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> selectCases().toList().get(1).profile().selectDetailUri(selectSourceUrl(selectCases().toList().get(1),
                selectSource(selectCases().toList().get(1), "123").sourceUrl() + "&id="))).hasMessage("PROFILE_REQUIRED");
    }

    @Test void jecheonUnknownIconsAndNestedLinksRemainIncomplete() {
        var sample = selectCases().toList().get(4); var profile = sample.profile(); var source = selectSource(sample, "123");
        String valid = selectItem(sample, "1", "공고.hwpx", true);
        for (String altered : List.of(valid.replace("p-attch__item", "p-attach__item"), valid.replace("xlink:href=", "href="),
                valid.replace("#search", "#other"), valid.replace("#arrow-circle-down", "#other"),
                valid.replace("/common/images/program/p-icon.svg", "https://unapproved.go.kr/icon.svg"),
                valid.replace("<use ", "<use data-extra='1' "), valid.replace("</use>", "<a href='/other'>누락 파일</a></use>"),
                valid.replace("<svg ", "<svg onclick='unknown()' "), valid.replace("<svg ", "<svg style='url(/unknown)' "),
                valid.replace("</svg>", "</svg><svg></svg>"), valid.replace("focusable='false'", "focusable='true'"),
                valid.replace("y='2'", "y='3'"), valid.replace("<span>공고.hwpx</span>", "<span><a href='/other'>공고.hwpx</a></span>")))
            assertThat(profile.selectDescriptors(source, selectPage(sample, altered)).complete()).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create("https://www.jecheon.go.kr/previewBbs.do?atchmnflNo=1"))).isFalse();
    }

    @Test void boeunRequiresItsOwnTitleBoardAndPreviewIdentity() {
        var sample = selectCases().toList().get(5); var profile = sample.profile(); var source = selectSource(sample, "123");
        assertThat(profile.selectSourceBindings()).containsExactly(new AttachmentDiscoveryProfile.SourceBinding("LGS-000139", "HEURISTIC_NOTICE"));
        String page = selectPage(sample, selectItem(sample, "1", "공고.hwpx", true));
        for (String altered : List.of(page.replace("p-table block", "p-table"),
                page.replace("<span class='p-table__subject_text'>소상공인 지원 공고</span>",
                        "<table><tr><td><span class='p-table__subject_text'>중첩 제목</span></td></tr></table>"),
                page.replace("previewBbsFile.do?key=194&bbsNo=66&", "previewBbsFile.do?key=999&bbsNo=66&"),
                page.replace("previewBbsFile.do?key=194&bbsNo=66&", "previewBbsFile.do?key=194&bbsNo=999&"),
                page.replace("previewBbsFile.do?key=194&bbsNo=66&", "previewBbsFile.do?"),
                page.replace("downloadBbsFile.do?atchmnflNo=1", "downloadBbsFile.do?key=194&atchmnflNo=1")))
            assertThat(profile.selectDescriptors(source, altered).complete()).isFalse();
    }

    @Test void registryHasSixDistinctImmutableProfiles() {
        try (var context = new AnnotationConfigApplicationContext(StandardBbsAttachmentProfileConfiguration.class, AttachmentDiscoveryProfileRegistry.class)) {
            var registry = context.getBean(AttachmentDiscoveryProfileRegistry.class);
            assertThat(registry.selectProfileList()).hasSize(6);
            assertThat(registry.selectProfileList()).extracting(AttachmentDiscoveryProfile::selectProfileHash).doesNotHaveDuplicates();
            registry.selectProfileList().forEach(profile -> {
                assertThat(profile.selectProfileHash()).matches("[0-9a-f]{64}");
                assertThat(registry.selectProfileDetails("LOCAL_GOV_NOTICE", profile.selectProfileCode(), profile.selectProfileHash())).contains(profile);
                assertThat(registry.selectProfileDetails("LOCAL_GOV_NOTICE", profile.selectProfileCode(), "a".repeat(64))).isEmpty();
            });
        }
    }
    @Test void allFifteenProfilesCoexistAndLegacyMimeDoesNotLeakToExistingProviders() {
        try (var context = new AnnotationConfigApplicationContext(StandardBbsAttachmentProfileConfiguration.class,
                LegalBoardAttachmentProfileConfiguration.class, SaeolGetAttachmentProfileConfiguration.class,
                HwacheonPostAttachmentDiscoveryProfile.class, BizInfoAttachmentDiscoveryProfile.class,
                SeoguSaeolAttachmentDiscoveryProfile.class, AttachmentDiscoveryProfileRegistry.class)) {
            var profiles = context.getBean(AttachmentDiscoveryProfileRegistry.class).selectProfileList();
            assertThat(profiles).hasSize(15);
            assertThat(profiles).extracting(AttachmentDiscoveryProfile::selectProfileCode).doesNotHaveDuplicates();
            assertThat(profiles).extracting(AttachmentDiscoveryProfile::selectProfileHash).doesNotHaveDuplicates();
            assertThat(profiles.stream().filter(p -> !(p instanceof StandardBbsAttachmentDiscoveryProfile)))
                    .allSatisfy(p -> assertThat(p.selectLegacyBinaryContentTypes()).isEmpty());
            selectCases().forEach(sample -> {
                if (List.of("140", "18", "66").contains(sample.board())) {
                    if (sample.board().equals("140")) assertThat(sample.profile().selectLegacyBinaryContentTypes()).isEmpty();
                    else assertThat(sample.profile().selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");
                    assertThat(sample.profile().selectUtf8DispositionOctets()).isEqualTo(sample.board().equals("140"));
                    return;
                }
                assertThat(sample.profile().selectLegacyBinaryContentTypes())
                        .containsExactly(sample.board().equals("25") ? "application/x-msdownload" : "application/octer-stream");
                assertThat(sample.profile().selectUtf8DispositionOctets()).isEqualTo(!sample.board().equals("25"));
            });
        }
    }
}
