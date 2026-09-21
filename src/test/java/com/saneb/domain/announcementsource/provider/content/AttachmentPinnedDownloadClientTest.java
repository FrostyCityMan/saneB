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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "/www/selectBbsNttView.do?key=236&bbsNo=40&nttNo=123,/www/selectBbsNttView.do?key=236&bbsNo=40&nttNo=124",
            "/www/downloadBbsFile.do?atchmnflNo=1,/www/downloadBbsFile.do?atchmnflNo=2",
            "/www/downloadBbsFile.do?atchmnflNo=1,/www/selectBbsNttView.do?key=236&bbsNo=40&nttNo=123"})
    void bbsRedirectIdentityChangeStopsBeforeSecondDnsHttpAndFileCreation(String initialPath,String redirectPath) throws Exception {
        var profile=new com.saneb.domain.announcementattachment.discovery.StandardBbsAttachmentProfileConfiguration().selectOkcheonProfileDetails();
        var initial=AttachmentPinnedDownloadClient.Request.selectGet(URI.create("https://www.oc.go.kr"+initialPath));
        var dns=new java.util.concurrent.atomic.AtomicInteger();var http=new java.util.concurrent.atomic.AtomicInteger();
        var dnsAtRedirect=new java.util.concurrent.atomic.AtomicInteger();
        var bytes=new java.util.concurrent.atomic.AtomicLong();var output=root.resolve("redirect.bin");
        try(var client=new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(host->{
            dns.incrementAndGet();return new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})};
        }), (target,remaining,handler)->{
            assertThat(http.incrementAndGet()).isEqualTo(1);
            dnsAtRedirect.set(dns.get());
            return handler.handle(new AttachmentPinnedDownloadClient.Response(302,redirectPath,0,"text/html",null,
                    new java.io.ByteArrayInputStream(new byte[0])));
        },java.time.Duration.ofSeconds(2))) {
            assertThatThrownBy(()->com.saneb.domain.announcementattachment.discovery.AttachmentProfileDownloadFlow.selectDownload(
                    profile,initial,output,1024,(request,limit,approved)->client.selectDownload(request,
                            profile.selectApprovedHosts(),approved,output,limit,count->{bytes.addAndGet(count);return true;})))
                    .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
        }
        // 최초 요청 검증의 DNS 호출 수를 가정하지 않고 redirect 이후 추가 조회가 없는지 확인한다.
        assertThat(dnsAtRedirect.get()).isPositive();assertThat(dns.get()).isEqualTo(dnsAtRedirect.get());
        assertThat(http.get()).isEqualTo(1);
        assertThat(bytes.get()).isZero();assertThat(output).doesNotExist();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"UNKNOWN_HOST", "EMPTY", "NULL"})
    void unresolvedPublicHostIsNetworkFailureWithoutHttpOrOriginalOutput(String mode) throws Exception {
        var resolver=new ProviderContentUrlValidator(host -> {
            if ("UNKNOWN_HOST".equals(mode)) throw new java.net.UnknownHostException("PRIVATE_DIAGNOSTIC_CANARY");
            return "EMPTY".equals(mode) ? new InetAddress[0] : null;
        });
        try (var client=new AttachmentPinnedDownloadClient(resolver, (target, remaining, handler) -> {
            throw new AssertionError("DNS failure must not open HTTP");
        }, java.time.Duration.ofSeconds(1))) {
            assertThatThrownBy(() -> client.selectDownload(URI.create("https://approved.example/file"),Set.of("approved.example"),root.resolve("file.bin"),1024))
                    .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_DNS_LOOKUP_FAILED").hasNoCause();
            try (var paths=Files.list(root)) { assertThat(paths.toList()).isEmpty(); }
        }
    }
    @Test void unexpectedResolverFailureRemainsBlockedWithoutLeakingItsMessage() throws Exception {
        var resolver=new ProviderContentUrlValidator(host -> { throw new IllegalStateException("PRIVATE_DIAGNOSTIC_CANARY"); });
        try (var client=new AttachmentPinnedDownloadClient(resolver, (target, remaining, handler) -> {
            throw new AssertionError("invalid resolver must not open HTTP");
        }, java.time.Duration.ofSeconds(1))) {
            assertThatThrownBy(() -> client.selectDownload(URI.create("https://approved.example/file"),Set.of("approved.example"),root.resolve("file.bin"),1024))
                    .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_URL_BLOCKED").hasNoCause();
            try (var paths=Files.list(root)) { assertThat(paths.toList()).isEmpty(); }
        }
    }
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
