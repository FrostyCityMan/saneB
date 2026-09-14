package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.*;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentPinnedDownloadClientTest {
    @TempDir Path root;
    @Test void unapprovedHostIsRejectedBeforeDnsOrHttpAndPreservesExistingFile() throws Exception {
        Path output=root.resolve("existing.bin"); Files.writeString(output,"사용자 기존 파일");
        try (var client=new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(host -> { throw new AssertionError("DNS must not run"); }))) {
        assertThatThrownBy(() -> client.selectDownload(URI.create("https://unapproved.example/file.pdf"),Set.of("approved.example"),output,1024))
                .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_HOST_NOT_APPROVED");
        assertThat(Files.readString(output)).isEqualTo("사용자 기존 파일");
        }
    }
    @Test void privateAddressIsRejectedEvenForAllowlistedHost() throws Exception {
        try (var client=new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(host -> new InetAddress[]{InetAddress.getByAddress(new byte[]{127,0,0,1})}))) {
        assertThatThrownBy(() -> client.selectDownload(URI.create("https://approved.example/file"),Set.of("approved.example"),root.resolve("file.bin"),1024))
                .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_URL_BLOCKED");
        try (var paths=Files.list(root)) { assertThat(paths.toList()).isEmpty(); }
        }
    }
    @Test void emptyBudgetAndHttpSchemeCannotOpenNetwork() {
        try (var client=new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(host -> { throw new AssertionError("DNS must not run"); }))) {
        assertThatThrownBy(() -> client.selectDownload(URI.create("https://approved.example/file"),Set.of("approved.example"),root.resolve("file.bin"),0))
                .isInstanceOf(java.io.IOException.class).hasMessage("SOURCE_BYTE_LIMIT");
        assertThatThrownBy(() -> client.selectDownload(URI.create("http://approved.example/file"),Set.of("approved.example"),root.resolve("file.bin"),1024))
                .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_HOST_NOT_APPROVED");
        }
    }
}
