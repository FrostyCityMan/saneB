package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class BizInfoAttachmentDiscoveryProfileTest {
    private static final String NOTICE = "PBLN_000000000124628";
    private static final String FILE = "FILE_000000000765684";
    private final BizInfoAttachmentDiscoveryProfile profile = new BizInfoAttachmentDiscoveryProfile();

    @Test void discoversAllFiveFilesAndKeepsFilenameOutOfClassificationInputs() {
        var result = profile.selectDescriptors(NOTICE, page(
                row("수출 공고문.pdf", 0) + row("지원 안내.hwpx", 1) + row("참여 신청서.hwp", 2)
                        + row("업종 기준.hwpx", 3) + row("포스터.pdf", 4)));
        assertThat(result.status()).isEqualTo("FOUND");
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(5);
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::documentRole)
                .containsExactly("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN");
        assertThat(result.descriptors()).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue();
            assertThat(file.locator().profileCode()).isEqualTo(profile.selectProfileCode());
            assertThat(file.locator().path()).doesNotContain("?", "https:");
        });
    }

    @Test void verifiedEmptyContainerIsDifferentFromMissingOrBrokenSelector() {
        assertThat(profile.selectDescriptors(NOTICE, page("")).status()).isEqualTo("NO_FILES");
        assertThat(profile.selectDescriptors(NOTICE, "<html><body>로그인 필요</body></html>").status()).isEqualTo("FAILED");
        var broken = profile.selectDescriptors(NOTICE, page("<li><div class='file_name'>공고문.pdf</div><a onclick='downloadSomething()'>보기</a></li>"));
        assertThat(broken.status()).isEqualTo("FAILED");
        assertThat(broken.complete()).isFalse();
        assertThat(broken.descriptors()).isEmpty();
        assertThat(profile.selectDescriptors(NOTICE, page(row("공고문.pdf", 0).replace("<li>", "").replace("</li>", ""))).status())
                .isEqualTo("FAILED");
    }

    @Test void duplicateLinksAreDeduplicatedAndEleventhFileIsExplicitlyLimited() {
        StringBuilder rows = new StringBuilder(row("공고문.pdf", 0));
        for (int i = 0; i < 11; i++) rows.append(row("공고문.pdf", i));
        var result = profile.selectDescriptors(NOTICE, page(rows.toString()));
        assertThat(result.descriptors()).hasSize(10);
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(result.complete()).isFalse();
    }

    @Test void keepsUnsupportedFilesVisibleButDoesNotDownloadThem() {
        var result = profile.selectDescriptors(NOTICE, page(row("양식.xlsx", 0) + row("공고.download", 1)));
        assertThat(result.status()).isEqualTo("FOUND");
        assertThat(result.descriptors().getFirst().downloadAllowed()).isFalse();
        assertThat(result.descriptors().get(1).expectedFormat()).isNull();
        assertThat(result.descriptors().get(1).downloadAllowed()).isTrue();
    }

    @Test void rejectsUnexpectedHostPathPortQueryAndCredentials() {
        for (String path : List.of(
                "http://www.bizinfo.go.kr/cmm/fms/fileDown.do?atchFileId=" + FILE + "&fileSn=0",
                "https://www.bizinfo.go.kr:80/cmm/fms/fileDown.do?atchFileId=" + FILE + "&fileSn=0",
                "https://other.go.kr/cmm/fms/fileDown.do?atchFileId=" + FILE + "&fileSn=0",
                "https://www.bizinfo.go.kr/private/file?atchFileId=" + FILE + "&fileSn=0",
                "https://www.bizinfo.go.kr/cmm/fms/fileDown.do?atchFileId=" + FILE + "&fileSn=0&token=not-a-real-token",
                "https://www.bizinfo.go.kr/cmm/fms/fileDown.do?atchFileId=" + FILE + "&fileSn=0&fileSn=1",
                "https://not-a-real-user@www.bizinfo.go.kr/cmm/fms/fileDown.do?atchFileId=" + FILE + "&fileSn=0",
                "https://www.bizinfo.go.kr/cmm/fms/../fileDown.do?atchFileId=" + FILE + "&fileSn=0")) {
            assertThat(profile.selectApprovedRequest(URI.create(path))).isFalse();
        }
        assertThatThrownBy(() -> profile.selectDetailUri("https://other.go.kr/detail"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("PROFILE_REQUIRED");
    }

    @Test void maliciousBaseTagCannotRedirectDiscoveryAndPartialFailureRemainsVisible() {
        String html = "<base href='https://other.go.kr/'>" + page(row("공고문.pdf", 0)
                + "<li><div class='file_name'>확인 필요.pdf</div><a href='javascript:run()'>다운로드</a></li>");
        var result = profile.selectDescriptors(NOTICE, html);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.descriptors()).hasSize(1);
        assertThat(result.descriptors().getFirst().fetchUri().getHost()).isEqualTo("www.bizinfo.go.kr");
    }

    @Test void registryRequiresExactDeployedProfileHashAndDoesNotGuessOtherProviders() {
        var registry = new AttachmentDiscoveryProfileRegistry(List.of(profile));
        assertThat(profile.selectProfileHash()).matches("[0-9a-f]{64}");
        assertThat(registry.selectProfileDetails("BIZINFO", profile.selectProfileCode(), profile.selectProfileHash())).contains(profile);
        assertThat(registry.selectProfileDetails("BIZINFO", profile.selectProfileCode(), "a".repeat(64))).isEmpty();
        assertThat(registry.selectProfileDetails("GOV24_PUBLIC_SERVICE", profile.selectProfileCode(), profile.selectProfileHash())).isEmpty();
    }

    @Test void multipleDifferentFilesInOneRowAreNotSilentlyDropped() {
        String row = row("공고문.pdf", 0).replace("</li>",
                "<a href='/cmm/fms/fileDown.do?atchFileId=" + FILE + "&amp;fileSn=1'>추가 다운로드</a></li>");
        var result = profile.selectDescriptors(NOTICE, page(row));
        assertThat(result.descriptors()).hasSize(2);
        assertThat(result.complete()).isFalse();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::documentRole).containsOnly("UNKNOWN");
    }

    private String page(String rows) {
        return "<html><head><meta property='og:title' content='합성 소상공인 지원금 공고'></head>"
                + "<body><div class='attached_file_list'><ul>" + rows + "</ul></div></body></html>";
    }
    private String row(String name, int fileSn) {
        return "<li><div class='file_name'>" + name + "</div><a href='/cmm/fms/fileDown.do?atchFileId="
                + FILE + "&amp;fileSn=" + fileSn + "'>다운로드</a></li>";
    }
}
