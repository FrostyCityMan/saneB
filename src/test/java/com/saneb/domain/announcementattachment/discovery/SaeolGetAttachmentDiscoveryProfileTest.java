package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class SaeolGetAttachmentDiscoveryProfileTest {
    record Case(AttachmentDiscoveryProfile profile, String sourceCode, String parser, String layout, String noticeId, int fileCount, String subCheck) {
        Case(AttachmentDiscoveryProfile profile, String sourceCode, String parser, String layout, String noticeId, int fileCount) {
            this(profile, sourceCode, parser, layout, noticeId, fileCount, "Y");
        }
    }
    static Stream<Case> selectCases() {
        var configuration = new SaeolGetAttachmentProfileConfiguration();
        return Stream.of(new Case(configuration.selectBusanNamguProfileDetails(), "LGS-000034", "SAFE_SAEOL_EMINWON_LEGACY", "th", "46034", 4),
                new Case(configuration.selectDaeguDalseongProfileDetails(), "LGS-000052", "SAFE_SAEOL_EMINWON", "th", "53932", 1),
                new Case(configuration.selectDaeguJungguProfileDetails(), "LGS-000045", "SAFE_SAEOL_EMINWON_LEGACY", "div", "34295", 1),
                new Case(configuration.selectHamanProfileDetails(), "LGS-000233", "SAFE_SAEOL_EMINWON_CELL", "td", "43065", 1));
    }
    static AttachmentDiscoveryProfile.Source selectSource(Case sample, String scheme) {
        String url = scheme + "://" + sample.profile().selectApprovedHosts().iterator().next() + SaeolGetAttachmentDiscoveryProfile.DETAIL
                + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + sample.noticeId() + "&subCheck=" + sample.subCheck();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)), url,
                sample.sourceCode(), sample.parser());
    }
    static String selectPage(Case sample, String links) {
        String body = sample.layout().equals("div") ? "<div class='tal'>첨부파일 : " + links + "</div>"
                : "<table><tr><" + sample.layout() + ">첨부파일 :</" + sample.layout() + "><td>" + links + "</td></tr></table>";
        return "<form name='form1' method='post'>" + body + "</form>";
    }
    static String selectLink(String name, String system, String directory) {
        return "<a href=\"javascript:goDownLoad('" + name + "','" + system + "','" + directory + "')\">첨부</a><br>";
    }
    static String selectLink(String name, String system) { return selectLink(name, system, "/ntishome/file/upload/ofr/ofr/20260911"); }

    @ParameterizedTest @MethodSource("selectCases") void discoversEveryFormatWithUnknownRoleAndHashedLocator(Case sample) {
        String links = selectLink("공고문.pdf", "1.pdf") + selectLink("신청서.hwp", "2.hwp") + selectLink("안내.hwpx", "3.hwpx");
        var result = sample.profile().selectDescriptors(selectSource(sample, "https"), selectPage(sample, links));
        assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF", "HWP", "HWPX");
        assertThat(result.descriptors()).allSatisfy(file -> {
            assertThat(file.documentRole()).isEqualTo("UNKNOWN"); assertThat(file.downloadAllowed()).isTrue();
            assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
            assertThat(file.selectRequest().method()).isEqualTo("GET"); assertThat(file.postForm()).isEmpty();
            assertThat(file.locator().identifiers()).containsEntry("noticeId", sample.noticeId());
            assertThat(file.locator().identifiers().get("attachmentId")).matches("[0-9a-f]{64}");
            assertThat(file.locator().toString()).doesNotContain("공고문", "ntishome", "user_file_nm");
            assertThat(file.toString()).doesNotContain("공고문", "ntishome", "user_file_nm");
        });
    }
    @ParameterizedTest @MethodSource("selectCases") void emptyMissingMixedAndHiddenFilesHaveDifferentStates(Case sample) {
        var source = selectSource(sample, "https"); var profile = sample.profile();
        assertThat(profile.selectDescriptors(source, selectPage(sample, "")).status()).isEqualTo("NO_FILES");
        assertThat(profile.selectDescriptors(source, "<html>로그인</html>").status()).isEqualTo("FAILED");
        String valid = selectLink("공고.pdf", "1.pdf");
        for (String extra : List.of("<a href='/other'>미지원 링크</a>", "<button>다른 첨부</button>",
                "<span>다른 첨부.pdf</span>", "<img src='/unknown'>", "<script>arbitrary()</script>")) {
            var result = profile.selectDescriptors(source, selectPage(sample, valid + extra));
            assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.complete()).isFalse();
            assertThat(result.descriptors()).hasSize(1);
        }
        assertThat(profile.selectDescriptors(source, selectPage(sample, valid) + selectPage(sample, valid)).status()).isEqualTo("FAILED");
    }
    @ParameterizedTest @MethodSource("selectCases") void noCrossSourceHashParserHostOrHttpFallback(Case sample) {
        var source = selectSource(sample, "https"); var profile = sample.profile();
        for (var altered : List.of(new AttachmentDiscoveryProfile.Source("BIZINFO", source.providerNoticeId(), source.sourceUrl(), sample.sourceCode(), sample.parser()),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), "a".repeat(64), source.sourceUrl(), sample.sourceCode(), sample.parser()),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(), source.sourceUrl(), "LGS-000130", sample.parser()),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(), source.sourceUrl(), sample.sourceCode(), "SAEOL_GOSI")))
            assertThatThrownBy(() -> profile.selectDetailUri(altered)).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> profile.selectDetailUri(source.providerNoticeId())).hasMessage("PROFILE_REQUIRED");
        if (sample.sourceCode().equals("LGS-000034")) {
            assertThat(profile.selectDetailUri(selectSource(sample, "http"))).isEqualTo(profile.selectDetailUri(source));
        } else assertThatThrownBy(() -> profile.selectDetailUri(selectSource(sample, "http"))).hasMessage("PROFILE_REQUIRED");
        assertThat(profile.selectApprovedRequest(URI.create(selectSource(sample, "http").sourceUrl()))).isFalse();
    }
    @Test void rejectsMalformedJavascriptEncodedTraversalAndUnexpectedForm() {
        Case sample = selectCases().findFirst().orElseThrow(); var source = selectSource(sample, "https"); var profile = sample.profile();
        for (String link : List.of(selectLink("공고.pdf", "../private.pdf"), selectLink("공고.pdf", "%252fprivate.pdf"),
                selectLink("공고.pdf", "1.pdf", "/etc/private"), selectLink("공고.pdf", "1.pdf").replace(")\">", ");evil()\">"),
                selectLink("공고.pdf", "1.pdf").replace("goDownLoad", "goDownLoadNew"),
                selectLink("공고.pdf", "1.pdf").replace("<a ", "<a onclick='evil()' "))) {
            var result = profile.selectDescriptors(source, selectPage(sample, link));
            assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.descriptors()).isEmpty();
        }
        var file = profile.selectDescriptors(source, selectPage(sample, selectLink("공고.pdf", "1.pdf"))).descriptors().getFirst();
        assertThat(profile.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(file.fetchUri(), "POST", Map.of("data", "fixture")))).isFalse();
    }
    @Test void encodedNamesCannotInjectQueryOrRequestAnotherPath() {
        Case sample = selectCases().findFirst().orElseThrow(); var profile = sample.profile();
        var file = profile.selectDescriptors(selectSource(sample, "https"), selectPage(sample,
                selectLink("지원 & 신청 + 자료(1).pdf", "지원 & 신청 + 자료_1.pdf"))).descriptors().getFirst();
        assertThat(file.fetchUri().getRawQuery().split("&")).hasSize(3);
        assertThat(URLDecoder.decode(file.fetchUri().getRawQuery(), StandardCharsets.UTF_8)).contains("지원 & 신청 + 자료(1).pdf");
        for (String uri : List.of(file.fetchUri() + "&file_path=other", file.fetchUri() + "&unknown=data", file.fetchUri() + "#fragment",
                file.fetchUri().toString().replace("FileDown.jsp", "%46ileDown.jsp"), file.fetchUri().toString().replace("/jsp/", "/jsp/../jsp/"),
                file.fetchUri().toString().replace("eminwon.bsnamgu.go.kr", "127.0.0.1"),
                file.fetchUri().toString().replace("eminwon.bsnamgu.go.kr", "eminwon.bsnamgu.go.kr:444"),
                file.fetchUri().toString().replace("https://", "https://fixture@")))
            assertThat(profile.selectApprovedRequest(URI.create(uri))).isFalse();
    }
    @Test void duplicatesLimitsAndUnsupportedFormatsAreNotHidden() {
        Case sample = selectCases().findFirst().orElseThrow(); var source = selectSource(sample, "https"); var profile = sample.profile();
        String first = selectLink("공고.pdf", "1.pdf");
        assertThat(profile.selectDescriptors(source, selectPage(sample, first + first)).descriptors()).hasSize(1);
        assertThat(profile.selectDescriptors(source, selectPage(sample, first + selectLink("다른 이름.pdf", "1.pdf"))).complete()).isFalse();
        String many = IntStream.range(0, 11).mapToObj(i -> selectLink("공고.pdf", i + ".pdf")).collect(java.util.stream.Collectors.joining());
        var limited = profile.selectDescriptors(source, selectPage(sample, many));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limited.descriptors()).hasSize(10); assertThat(limited.complete()).isFalse();
        var unsupported = profile.selectDescriptors(source, selectPage(sample, selectLink("참고.zip", "1.zip") + selectLink("공고.pdf", "2.exe")));
        assertThat(unsupported.descriptors()).hasSize(2);
        assertThat(unsupported.descriptors()).allSatisfy(d -> { assertThat(d.downloadAllowed()).isFalse(); assertThat(d.expectedFormat()).isNull(); });
    }
    @Test void maliciousBaseAndAlternateRequestImplementationDoNotChangeEndpoint() {
        Case sample = selectCases().findFirst().orElseThrow();
        var result = sample.profile().selectDescriptors(selectSource(sample, "https"), "<base href='https://other.go.kr'>"
                + "<script>function goDownLoad(){ location.href='https://other.go.kr'; }</script>"
                + selectPage(sample, selectLink("공고.pdf", "1.pdf")));
        assertThat(result.descriptors().getFirst().fetchUri().getHost()).isEqualTo("eminwon.bsnamgu.go.kr");
    }
    @Test void springRegistryContainsFourSeparateImmutableSystemProfiles() {
        try (var context = new AnnotationConfigApplicationContext(SaeolGetAttachmentProfileConfiguration.class, AttachmentDiscoveryProfileRegistry.class)) {
            var registry = context.getBean(AttachmentDiscoveryProfileRegistry.class);
            assertThat(registry.selectProfileList()).hasSize(4);
            assertThat(registry.selectProfileList()).extracting(AttachmentDiscoveryProfile::selectProfileHash).doesNotHaveDuplicates();
            registry.selectProfileList().forEach(profile -> {
                assertThat(profile.selectProfileHash()).matches("[0-9a-f]{64}");
                assertThat(registry.selectProfileDetails("LOCAL_GOV_NOTICE", profile.selectProfileCode(), profile.selectProfileHash())).contains(profile);
                assertThat(registry.selectProfileDetails("LOCAL_GOV_NOTICE", profile.selectProfileCode(), "a".repeat(64))).isEmpty();
            });
        }
    }
}
