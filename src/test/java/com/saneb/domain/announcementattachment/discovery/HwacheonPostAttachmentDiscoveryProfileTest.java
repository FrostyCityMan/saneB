package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class HwacheonPostAttachmentDiscoveryProfileTest {
    private final HwacheonPostAttachmentDiscoveryProfile profile = new HwacheonPostAttachmentDiscoveryProfile();
    static final String URL = "https://eminwon.ihc.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
            + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
            + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=33897&subCheck=N";
    private static final String DOWNLOAD = "https://eminwon.ihc.go.kr/emwp/jsp/ofr/FileDownNew.jsp";
    static final String CIPHER_USER = "합성표본" + "A".repeat(43) + "=";
    static final String CIPHER_SYSTEM = "합성표본" + "B".repeat(63) + "=";
    static final String CIPHER_PATH = "/ntisho" + "C".repeat(64);
    static AttachmentDiscoveryProfile.Source selectSource(String url) {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                url, "LGS-000130", "SAFE_SAEOL_EMINWON_LEGACY");
    }
    static String selectPage(String links) {
        return "<form name='nnn' method='post' action='/emwp/jsp/ofr/FileDownNew.jsp'>"
                + "<input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'>"
                + "<input type='hidden' name='file_path'><input type='hidden' name='isHome' value='Y'></form>"
                + "<form name='form1' method='post'><table><tr><td> 첨부파일 : </td><td>" + links + "</td></tr></table></form>";
    }
    static String selectLink(String displayName, String system) {
        return "<a href=\"javascript:goDownLoad('" + CIPHER_USER + "','" + system + "','" + CIPHER_PATH + "')\">" + displayName + "</a><br>";
    }
    private AttachmentDiscoveryProfile.Descriptor selectDescriptor() {
        return profile.selectDescriptors(selectSource(URL), selectPage(selectLink("합성 공고문.hwpx", CIPHER_SYSTEM))).descriptors().getFirst();
    }
    @Test void opaqueValuesArePassedOnceInFixedPostFormAndNeverInStoredLocator() throws Exception {
        var descriptor = selectDescriptor(); var request = descriptor.selectRequest();
        assertThat(profile.selectApprovedRequest(request)).isTrue();
        assertThat(request.method()).isEqualTo("POST"); assertThat(request.uri()).isEqualTo(URI.create(DOWNLOAD));
        assertThat(request.form()).isEqualTo(Map.of("user_file_nm", CIPHER_USER, "sys_file_nm", CIPHER_SYSTEM, "file_path", CIPHER_PATH, "isHome", "Y"));
        assertThat(request.uri().getRawQuery()).isNull();
        assertThat(descriptor.displayName()).isEqualTo("합성 공고문.hwpx");
        assertThat(descriptor.documentRole()).isEqualTo("UNKNOWN");
        assertThat(descriptor.expectedFormat()).isEqualTo("HWPX"); assertThat(descriptor.downloadAllowed()).isTrue();
        String stored = new ObjectMapper().writeValueAsString(descriptor.locator());
        assertThat(stored).doesNotContain(CIPHER_USER, CIPHER_SYSTEM, CIPHER_PATH, "합성 공고문", "isHome");
        assertThat(descriptor.locator().identifiers()).containsEntry("noticeId", "33897");
        assertThat(descriptor.locator().identifiers().get("attachmentId")).matches("[0-9a-f]{64}");
        assertThat(request.toString() + descriptor + profile.selectDescriptors(selectSource(URL), selectPage(selectLink("합성 공고문.hwpx", CIPHER_SYSTEM))))
                .doesNotContain(CIPHER_USER, CIPHER_SYSTEM, CIPHER_PATH, "합성 공고문", "user_file_nm");
    }
    @Test void sameOpaqueLocatorIsStableWhileRotationChangesIdentity() {
        var first = selectDescriptor(); var repeated = selectDescriptor();
        assertThat(first.locator()).isEqualTo(repeated.locator());
        var rotated = profile.selectDescriptors(selectSource(URL), selectPage(selectLink("합성 공고문.hwpx", "D".repeat(64)))).descriptors().getFirst();
        assertThat(rotated.locator()).isNotEqualTo(first.locator());
        var renamed = profile.selectDescriptors(selectSource(URL), selectPage(selectLink("다른 표시명.hwpx", CIPHER_SYSTEM))).descriptors().getFirst();
        assertThat(renamed.locator()).isEqualTo(first.locator());
    }
    @Test void shorterOpaqueUserNameObservedInMultiFilePagesIsPreservedVerbatim() {
        String shortValue = "합성" + "D".repeat(22) + "==";
        var result = profile.selectDescriptors(selectSource(URL), selectPage(selectLink("짧은 이름.pdf", CIPHER_SYSTEM).replace(CIPHER_USER, shortValue)));
        assertThat(result.status()).isEqualTo("FOUND");
        assertThat(result.descriptors().getFirst().postForm()).containsEntry("user_file_nm", shortValue);
    }
    @Test void emptyContainerRequiresValidPublicDownloadForm() {
        assertThat(profile.selectDescriptors(selectSource(URL), selectPage("")).status()).isEqualTo("NO_FILES");
        assertThat(profile.selectDescriptors(selectSource(URL), "<html>인증 필요</html>").status()).isEqualTo("FAILED");
        assertThat(profile.selectDescriptors(selectSource(URL), selectPage("").replace("name='nnn'", "name='other'")).status()).isEqualTo("FAILED");
    }
    @Test void changedFormFlagFieldTypeValueOrActionIsRejected() {
        String page = selectPage(selectLink("공고.pdf", CIPHER_SYSTEM));
        for (String changed : List.of(page.replace("value='Y'", "value='N'"), page.replace("type='hidden'", "type='text'"),
                page.replace("name='isHome'", "name='unknown'"), page.replace("name='user_file_nm'", "name='user_file_nm' value='unexpected'"),
                page.replace("action='/emwp/jsp/ofr/FileDownNew.jsp'", "action='https://other.go.kr/file'"),
                page.replace("<input type='hidden' name='user_file_nm'>", "<input type='hidden' name='user_file_nm' onfocus='arbitrary()'>"),
                page.replace("</form>", "<input type='hidden' name='unexpected'></form>"), page + page)) {
            var result = profile.selectDescriptors(selectSource(URL), changed);
            assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.descriptors()).isEmpty();
        }
    }
    @Test void unexpectedQuerySourceParserProviderHashAndHttpAreNeverApproved() {
        for (String url : List.of(URL.replace("https:", "http:"), URL.replace("subCheck=N", "subCheck=Y"), URL + "&subCheck=N",
                URL + "&unknown=value", URL + "#fragment", URL.replace("eminwon.ihc.go.kr", "127.0.0.1"),
                URL.replace("https://", "https://fixture@"), URL.replace("OfrAction", "%4FfrAction"))) {
            assertThat(profile.selectApprovedRequest(URI.create(url))).isFalse();
            assertThatThrownBy(() -> profile.selectDetailUri(selectSource(url))).hasMessage("PROFILE_REQUIRED");
        }
        var original = selectSource(URL);
        for (var source : List.of(new AttachmentDiscoveryProfile.Source("BIZINFO", original.providerNoticeId(), URL, "LGS-000130", original.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(original.providerCode(), "a".repeat(64), URL, "LGS-000130", original.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(original.providerCode(), original.providerNoticeId(), URL, "LGS-000034", original.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(original.providerCode(), original.providerNoticeId(), URL, "LGS-000130", "SAEOL_GOSI")))
            assertThatThrownBy(() -> profile.selectDetailUri(source)).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> profile.selectDetailUri(original.providerNoticeId())).hasMessage("PROFILE_REQUIRED");
    }
    @Test void postCannotRedirectChangeMethodTargetOrSendExtraFields() {
        var descriptor = selectDescriptor();
        assertThat(profile.selectApprovedRequest(AttachmentPinnedDownloadClient.Request.selectGet(descriptor.fetchUri()))).isFalse();
        for (String url : List.of(DOWNLOAD + "?isHome=Y", DOWNLOAD + "#fragment", DOWNLOAD.replace("New", ""),
                DOWNLOAD.replace("eminwon.ihc.go.kr", "other.go.kr"), DOWNLOAD.replace("https:", "http:"),
                DOWNLOAD.replace("eminwon.ihc.go.kr", "eminwon.ihc.go.kr:444"), DOWNLOAD.replace("/ofr/", "/ofr/../ofr/")))
            assertThat(profile.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(URI.create(url), "POST", descriptor.postForm()))).isFalse();
        for (String name : List.of("unknown", "isHome", "file_path", "sys_file_nm")) {
            var fields = new HashMap<>(descriptor.postForm()); fields.put(name, "invalid");
            assertThat(profile.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(descriptor.fetchUri(), "POST", fields))).isFalse();
        }
    }
    @Test void plainPathsEncodingsAndJavascriptDoNotBecomeDownloadRequests() {
        String link = selectLink("공고.hwpx", CIPHER_SYSTEM);
        for (String altered : List.of(link.replace(CIPHER_PATH, "/etc/private"), link.replace(CIPHER_SYSTEM, "../" + "A".repeat(64)),
                link.replace(CIPHER_PATH, "%2Fntisho" + "A".repeat(64)), link.replace(CIPHER_USER, "plaintext"),
                link.replace(")\">", ");arbitrary()\">"), link.replace("<a ", "<a onclick='arbitrary()' "))) {
            var result = profile.selectDescriptors(selectSource(URL), selectPage(altered));
            assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.descriptors()).isEmpty();
        }
    }
    @Test void baseTagAndPageJavascriptAreNotExecuted() {
        var result = profile.selectDescriptors(selectSource(URL), "<base href='https://other.go.kr'>"
                + "<script>function goDownLoad(){location.href='https://other.go.kr';}</script>"
                + selectPage(selectLink("공고.hwpx", CIPHER_SYSTEM)));
        assertThat(result.descriptors().getFirst().fetchUri()).isEqualTo(URI.create(DOWNLOAD));
    }
    @Test void validFilesSurviveUnresolvedAdditionalContentWithoutClaimingComplete() {
        for (String extra : List.of("<a href='/unknown'>다른 파일</a>", "<button>파일</button>", "<span>파일.pdf</span>",
                "<img src='/unknown'>", "<script>arbitrary()</script>")) {
            var result = profile.selectDescriptors(selectSource(URL), selectPage(selectLink("공고.hwpx", CIPHER_SYSTEM) + extra));
            assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
    }
    @Test void duplicatesAmbiguityLimitAndUnsupportedFilesAreExplicit() {
        String first = selectLink("공고.hwpx", CIPHER_SYSTEM);
        assertThat(profile.selectDescriptors(selectSource(URL), selectPage(first + first)).descriptors()).hasSize(1);
        assertThat(profile.selectDescriptors(selectSource(URL), selectPage(first + selectLink("다른 파일.pdf", CIPHER_SYSTEM))).complete()).isFalse();
        String many = IntStream.range(0, 11).mapToObj(i -> selectLink("공고.pdf", ((char)('D' + i) + "").repeat(64))).collect(java.util.stream.Collectors.joining());
        var limited = profile.selectDescriptors(selectSource(URL), selectPage(many));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limited.descriptors()).hasSize(10); assertThat(limited.complete()).isFalse();
        var unsupported = profile.selectDescriptors(selectSource(URL), selectPage(selectLink("자료.zip", CIPHER_SYSTEM))).descriptors().getFirst();
        assertThat(unsupported.downloadAllowed()).isFalse(); assertThat(unsupported.expectedFormat()).isNull();
    }
    @Test void byteLimitedOpaqueFormCannotBeExpandedBeyondTheSharedTransportCap() {
        String large = "가".repeat(1600) + "A".repeat(64);
        var result = profile.selectDescriptors(selectSource(URL), selectPage(selectLink("공고.pdf", large)));
        assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.descriptors()).isEmpty();
    }
    @Test void springRegistryResolvesSeventhProfileByExactHash() {
        try (var context = new AnnotationConfigApplicationContext(SaeolGetAttachmentProfileConfiguration.class,
                HwacheonPostAttachmentDiscoveryProfile.class, BizInfoAttachmentDiscoveryProfile.class,
                SeoguSaeolAttachmentDiscoveryProfile.class, AttachmentDiscoveryProfileRegistry.class)) {
            var registry = context.getBean(AttachmentDiscoveryProfileRegistry.class);
            assertThat(registry.selectProfileList()).hasSize(7);
            assertThat(registry.selectProfileDetails("LOCAL_GOV_NOTICE", profile.selectProfileCode(), profile.selectProfileHash())).isPresent();
            assertThat(registry.selectProfileDetails("LOCAL_GOV_NOTICE", profile.selectProfileCode(), "a".repeat(64))).isEmpty();
        }
    }
}
