package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SeoguSaeolAttachmentDiscoveryProfileTest {
    private final SeoguSaeolAttachmentDiscoveryProfile profile = new SeoguSaeolAttachmentDiscoveryProfile();
    private static final String URL = "https://www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do?notAncmtMgtNo=51668";
    private AttachmentDiscoveryProfile.Source selectSource() {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(URL)),URL,
                "LGS-000074","SAFE_DAEJEON_DATA_KEY_NOTICE");
    }
    private String selectPage(String content) {
        return "<form id='fileForm' method='post'></form><div class='bbs--view--file'>"+content+"</div>";
    }
    private String selectButton(String name, String systemName, String directory) {
        return "<a href='#' class='btn btn-file btn-on-ico' onclick=\"fn_egov_downFile('"+name+"','"+systemName+"','"+directory+"');\">다운로드</a>";
    }
    @Test void directPostUsesOnlyFixedHostPathAndEphemeralForm() {
        var result = profile.selectDescriptors(selectSource(),selectPage(selectButton("표본 (공고문).hwpx","123_표본공고문.hwpx","/ntishome/file/upload/ofr/ofr/20260820")));
        assertThat(result.status()).isEqualTo("FOUND");
        assertThat(result.complete()).isTrue();
        var descriptor = result.descriptors().getFirst();
        assertThat(descriptor.documentRole()).isEqualTo("UNKNOWN");
        assertThat(descriptor.expectedFormat()).isEqualTo("HWPX");
        assertThat(descriptor.downloadAllowed()).isTrue();
        assertThat(profile.selectApprovedRequest(descriptor.selectRequest())).isTrue();
        assertThat(descriptor.selectRequest().method()).isEqualTo("POST");
        String encoded = descriptor.postForm().get("user_file_nm");
        assertThat(URLDecoder.decode(URLDecoder.decode(encoded, StandardCharsets.UTF_8),StandardCharsets.UTF_8)).isEqualTo("표본 (공고문).hwpx");
        assertThat(descriptor.locator().identifiers()).containsEntry("noticeId","51668");
        assertThat(descriptor.locator().identifiers().get("attachmentId")).matches("[0-9a-f]{64}");
        assertThat(descriptor.locator().toString()).doesNotContain("표본", "ntishome");
        assertThat(descriptor.toString()).doesNotContain("표본", "file_path");
    }
    @Test void changedLayoutAndUnresolvedAdditionalDownloadAreNotNoFiles() {
        assertThat(profile.selectDescriptors(selectSource(),"<html>오류</html>").status()).isEqualTo("FAILED");
        assertThat(profile.selectDescriptors(selectSource(),selectPage("<span class='ir-file'>파일</span>")).status()).isEqualTo("FAILED");
        var mixed = selectButton("공고문.pdf","123.pdf","/ntishome/file/upload/ofr/ofr/20260820")+"<a href='/unknown-download'>다른 첨부</a>";
        var result = profile.selectDescriptors(selectSource(),selectPage(mixed));
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.descriptors()).hasSize(1);
        assertThat(result.complete()).isFalse();
        assertThat(profile.selectDescriptors(selectSource(),selectPage("")).status()).isEqualTo("NO_FILES");
    }
    @Test void unsupportedDocumentsRemainVisibleAndFormsDoNotBecomeNotices() {
        var result = profile.selectDescriptors(selectSource(),selectPage(
                selectButton("공고 신청서.hwp","123.hwp","/ntishome/file/upload/ofr/ofr/20260820")
                +selectButton("첨부자료.zip","124.zip","/ntishome/file/upload/ofr/ofr/20260820")));
        assertThat(result.descriptors()).hasSize(2);
        assertThat(result.descriptors().getFirst().documentRole()).isEqualTo("UNKNOWN");
        assertThat(result.descriptors().get(1).downloadAllowed()).isFalse();
    }
    @Test void arbitraryJavascriptAndTraversalAreNeverExecutedOrRequested() {
        for (String button : new String[]{
                selectButton("공고문.pdf","../../private.pdf","/ntishome/file/upload/ofr/ofr/20260820"),
                selectButton("공고문.pdf","123.pdf","/etc/private"),
                selectButton("공고문.pdf","%252fprivate.pdf","/ntishome/file/upload/ofr/ofr/20260820"),
                selectButton("공고문.pdf","123.pdf","/ntishome/file/upload/ofr/ofr/20260820").replace(";\">",";evil();\">")}) {
            var result = profile.selectDescriptors(selectSource(),selectPage(button));
            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(result.descriptors()).isEmpty();
        }
    }
    @Test void anotherLocalSourceOrUrlHashCannotChooseThisSystemProfile() {
        var source = selectSource();
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),
                source.sourceUrl(),"LGS-000075",source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"a".repeat(64),
                source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThat(profile.selectApprovedRequest(URI.create(URL+"&unknown=value"))).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create(URL.replace("https:","http:")))).isFalse();
    }
    @Test void postFormCannotChangeEndpointMethodKeysOrDirectory() {
        var descriptor = profile.selectDescriptors(selectSource(),selectPage(selectButton("공고.pdf","123.pdf","/ntishome/file/upload/ofr/ofr/20260820"))).descriptors().getFirst();
        assertThat(profile.selectApprovedRequest(AttachmentPinnedDownloadClient.Request.selectGet(descriptor.fetchUri()))).isFalse();
        assertThat(profile.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(URI.create("https://www.seogu.go.kr/other"),"POST",descriptor.postForm()))).isFalse();
        assertThat(profile.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(descriptor.fetchUri(),"POST",Map.of("secret","fixture")))).isFalse();
    }
    @Test void duplicatesAreDeduplicatedAndEleventhFileMakesWholeSetIncomplete() {
        String first = selectButton("공고문.pdf","123.pdf","/ntishome/file/upload/ofr/ofr/20260820");
        assertThat(profile.selectDescriptors(selectSource(),selectPage(first+first)).descriptors()).hasSize(1);
        String files = IntStream.range(0,11).mapToObj(i -> selectButton("공고문.pdf",i+".pdf","/ntishome/file/upload/ofr/ofr/20260820"))
                .collect(java.util.stream.Collectors.joining());
        var result = profile.selectDescriptors(selectSource(),selectPage(files));
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(result.descriptors()).hasSize(10);
        assertThat(result.complete()).isFalse();
    }
}
