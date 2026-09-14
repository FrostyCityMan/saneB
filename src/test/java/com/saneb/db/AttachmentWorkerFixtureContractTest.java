package com.saneb.db;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.BizInfoAttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 합성 HTTP fixture의 형식/실패 계약만 검증한다. DB·Linux worker 실행 증거가 아니다. */
class AttachmentWorkerFixtureContractTest {
    @TempDir Path directory;
    @Test void fixtureUsesRealProfileAndThreeGeneratedBinarySignaturesWithoutNetwork() throws Exception {
        var profile = new BizInfoAttachmentDiscoveryProfile();
        var detail = Request.selectGet(profile.selectDetailUri("PBLN_000000000124628"));
        try (var client = new AnnouncementAttachmentWorkerIntegrationTest.FixtureClient()) {
            Path html = directory.resolve("detail.html");
            client.selectDownload(detail, profile.selectApprovedHosts(), profile::selectApprovedRequest, html, 1048576, bytes -> true);
            var found = profile.selectDescriptors("PBLN_000000000124628", Files.readString(html));
            assertThat(found.complete()).isTrue();assertThat(found.descriptors()).hasSize(4);
            assertThat(found.descriptors().getLast().downloadAllowed()).isFalse();
            var validator = new AttachmentFileTypeValidator();
            for (var descriptor : found.descriptors()) if (descriptor.downloadAllowed()) {
                Path file = directory.resolve("attachment.bin");
                var downloaded = client.selectDownload(descriptor.selectRequest(), profile.selectApprovedHosts(), profile::selectApprovedRequest, file, 20971520, bytes -> true);
                assertThat(validator.selectFormat(file, downloaded, descriptor.expectedFormat())).isEqualTo(descriptor.expectedFormat());
                Files.delete(file);
            }
            assertThat(client.requests).isEqualTo(4);
        }
    }
    @Test void retryFixtureFailsBeforeCreatingFileAndKeepsRequestCounter() throws Exception {
        var profile = new BizInfoAttachmentDiscoveryProfile();
        try (var client = new AnnouncementAttachmentWorkerIntegrationTest.FixtureClient()) {
            client.failureIndex = 1;
            var request = Request.selectGet(java.net.URI.create("https://www.bizinfo.go.kr/cmm/fms/fileDown.do?atchFileId=FILE_000000000000001&fileSn=1"));
            Path file = directory.resolve("attachment.bin");
            assertThatThrownBy(() -> client.selectDownload(request, profile.selectApprovedHosts(), profile::selectApprovedRequest, file, 20971520, bytes -> true))
                    .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_HTTP_503");
            assertThat(file).doesNotExist();assertThat(client.fileRequests.get(1)).isEqualTo(1);
        }
    }
}
