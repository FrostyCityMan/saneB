package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChungjuEminwonAttachmentDiscoveryProfileTest {
    private final ChungjuEminwonAttachmentDiscoveryProfile profile = new ChungjuEminwonAttachmentDiscoveryProfile();
    static final String DETAIL = "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=72039";
    static final String FILE = "https://eminwon.chungju.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=sample.hwpx&sys_file_nm=sample_01.hwpx&file_path=/ntishome/file/upload/ofr/ofr/20260916";
    static AttachmentDiscoveryProfile.Source selectSource(String url) {
        var n = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url, "LGS-000137", "SAEOL_GOSI");
    }
    static String selectLink(String uri, String name) {
        return "<a href='" + uri.replace("&", "&amp;") + "' title='파일 다운로드'><img src='/common/images/board/file/ico_hwpx.gif' alt='"
                + name + " 파일 다운로드'/>" + name + "</a><br/>";
    }
    static String selectHtml(String files) {
        return "<nav>외부 메뉴</nav><table class='bbs_default view'><tr><th>제목</th><td>중소기업 지원 공고</td></tr>"
                + "<tr><th>내용</th><td title='내용' class='bbs_content'>사업자 지원금</td></tr>"
                + "<tr><th>파일</th><td>" + files + "</td></tr></table><footer>관련자료</footer>";
    }
    @Test void exactSourceIdentityAndListBindingAreRequiredAndPresentationParametersAreNotRequested() {
        var source = selectSource(DETAIL + "&pageIndex=2&ancmt_sj=%EC%A7%80%EC%9B%90&method=&context=");
        assertThat(profile.selectDetailUri(source)).isEqualTo(URI.create(DETAIL));
        assertThat(profile.selectSourceBindings()).containsExactly(new AttachmentDiscoveryProfile.SourceBinding("LGS-000137", "SAEOL_GOSI"));
        for (var bad : List.of(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", "a".repeat(64), DETAIL, "LGS-000137", "SAEOL_GOSI"),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(), source.sourceUrl(), "LGS-000138", "SAEOL_GOSI"),
                new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(), source.sourceUrl(), "LGS-000137", "SPRING_BBS")))
            assertThatThrownBy(() -> profile.selectDetailUri(bad)).hasMessage("PROFILE_REQUIRED");
        assertThat(profile.selectProfileHash()).matches("[0-9a-f]{64}");
        assertThat(profile.selectApprovedHosts()).containsExactlyInAnyOrder("www.chungju.go.kr", "eminwon.chungju.go.kr");
        assertThat(profile.selectLegacyBinaryContentTypes()).isEmpty();
        assertThat(profile.selectUtf8DispositionOctets()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings = {"http://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=1",
            "https://www.chungju.go.kr:444/www/selectEminwonView.do?key=510&ancmt_mgt_no=1",
            "https://user@www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=1",
            "https://www.chungju.go.kr/www/selectEminwonView.do?key=509&ancmt_mgt_no=1",
            "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=0",
            "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=1&ancmt_mgt_no=2",
            "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=1#fragment",
            "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=1&redirect=http://localhost",
            "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=1&ancmt_sj=%0A",
            "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=1&key=510"})
    void rejectsUnboundOrAmbiguousSource(String uri) {
        assertThatThrownBy(() -> profile.selectDetailUri(selectSource(uri))).isInstanceOf(IllegalArgumentException.class);
        assertThat(profile.selectApprovedRequest(URI.create(uri))).isFalse();
    }
    @Test void allSupportedAndUnsupportedFilesStayInTheirOwnNoticeAndRoleRemainsUnknown() {
        String files = selectLink(FILE, "sample.hwpx") + selectLink(FILE.replace("sample.hwpx", "image.jpg").replace("sample_01.hwpx", "image_01.jpg"), "image.jpg");
        var result = profile.selectDescriptors(selectSource(DETAIL), selectHtml(files));
        assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(2);
        assertThat(result.descriptors()).allSatisfy(d -> {
            assertThat(d.documentRole()).isEqualTo("UNKNOWN");
            assertThat(d.locator().identifiers()).containsEntry("noticeId", "72039");
            assertThat(d.locator().identifiers().get("attachmentId")).matches("[0-9a-f]{64}");
        });
        assertThat(result.descriptors().getFirst().expectedFormat()).isEqualTo("HWPX");
        assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void koreanAndSpaceNamesAreDecodedExactlyOnceWithoutFollowingHtmlBase() {
        String uri = FILE.replace("sample.hwpx", "%EA%B3%B5%EA%B3%A0%EB%AC%B8%20%EC%96%91%EC%8B%9D.hwpx");
        var result = profile.selectDescriptors(selectSource(DETAIL), "<base href='https://evil.invalid/'>" + selectHtml(selectLink(uri, "공고문 양식.hwpx")));
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors().getFirst().displayName()).isEqualTo("공고문 양식.hwpx");
        assertThat(result.descriptors().getFirst().fetchUri().toASCIIString()).contains("%EA%B3%B5", "%20").doesNotContain("evil", "%25EA");
    }
    @Test void downloadCannotChangeHostMethodOrFileEvenOnRedirect() {
        var first = AttachmentPinnedDownloadClient.Request.selectGet(URI.create(FILE));
        assertThat(profile.selectApprovedRequest(first, first)).isTrue();
        assertThat(profile.selectApprovedRequest(first, AttachmentPinnedDownloadClient.Request.selectGet(URI.create(FILE.replace("sample_01", "other_02"))))).isFalse();
        assertThat(profile.selectApprovedRequest(first, new AttachmentPinnedDownloadClient.Request(first.uri(), "POST", Map.of("method", "download")))).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create("https:opaque"))).isFalse();
        for (String uri : List.of(FILE.replace("https:", "http:"), FILE.replace("eminwon.chungju", "evil.chungju"),
                FILE + "&file_path=/ntishome/file/upload/ofr/ofr/20260916", FILE.replace("sample_01.hwpx", "..%2Fsample.hwpx"),
                FILE.replace("sample_01.hwpx", "%252e%252e"), FILE.replace("20260916", "../20260916"), FILE + "#other"))
            assertThat(profile.selectApprovedRequest(URI.create(uri))).isFalse();
    }
    @Test void emptyOfficialCellIsDifferentFromMissingOrAmbiguousStructure() {
        assertThat(profile.selectDescriptors(selectSource(DETAIL), selectHtml("")).status()).isEqualTo("NO_FILES");
        String valid = selectHtml(selectLink(FILE, "sample.hwpx"));
        for (String html : List.of("<main>본문만 있음</main>", valid + valid, valid.replace("bbs_default view", "changed"),
                valid.replace("제목</th>", "변경</th>"), valid.replace("title='내용'", "title='다른 내용'"),
                valid.replace("<th>파일</th>", "<th>첨부파일</th>"),
                valid.replace("<th>제목</th><td>중소기업 지원 공고</td>", "<td><table><tr><th>제목</th><td>중첩 가짜 제목</td></tr></table></td>"),
                valid.replace("<th>파일</th>", "<th>파일</th><td></td></tr><tr><th>파일</th>"))) {
            var result = profile.selectDescriptors(selectSource(DETAIL), html);
            assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.complete()).isFalse();
        }
    }
    @ParameterizedTest @ValueSource(strings = {"<button>더 보기</button>", "<iframe></iframe>", "<script>run()</script>",
            "다운로드 장애", "<a href='/other'>참조</a>", "<img src='/other'>", "<form></form>"})
    void unresolvedElementsNeverDisappearFromTheWholeFileResult(String extra) {
        var result = profile.selectDescriptors(selectSource(DETAIL), selectHtml(selectLink(FILE, "sample.hwpx") + extra));
        assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.complete()).isFalse();
        assertThat(result.descriptors()).hasSize(1);
    }
    @Test void conflictingDuplicateNameHiddenHandlerAndLimitRemainFailures() {
        String link = selectLink(FILE, "sample.hwpx");
        assertThat(profile.selectDescriptors(selectSource(DETAIL), selectHtml(link + link)).descriptors()).hasSize(1);
        for (String changed : List.of(link.replace("sample.hwpx", "other.hwpx"), link.replace("<img ", "<img onerror='run()' "),
                link.replace("<a ", "<a onclick='run()' "), link.replace("sample.hwpx 파일 다운로드", "other.hwpx 파일 다운로드")))
            assertThat(profile.selectDescriptors(selectSource(DETAIL), selectHtml(link + changed)).complete()).isFalse();
        String many = IntStream.range(0, 11).mapToObj(i -> selectLink(FILE.replace("sample_01", "sample_" + i), "sample.hwpx")).reduce("", String::concat);
        var result = profile.selectDescriptors(selectSource(DETAIL), selectHtml(many));
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(result.descriptors()).hasSize(10);
    }
}
