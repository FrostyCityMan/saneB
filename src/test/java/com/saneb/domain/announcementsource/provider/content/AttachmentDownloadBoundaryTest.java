package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentDownloadBoundaryTest {
    @TempDir Path root;
    private static final URI URL = URI.create("https://approved.example/download/file");
    private static final Set<String> HOSTS = Set.of("approved.example");

    @Test void reservesBytesBeforeReadingAndKeepsActualBytesSeparate() throws Exception {
        AtomicLong charged = new AtomicLong();
        InputStream body = new ByteArrayInputStream(new byte[]{1, 2, 3}) {
            @Override public synchronized int read(byte[] bytes, int offset, int length) {
                assertThat(charged.get()).isEqualTo(3);
                return super.read(bytes, offset, length);
            }
        };
        try (var client = selectClient((target, remaining, handler) -> handler.handle(
                new AttachmentPinnedDownloadClient.Response(200, null, 3, "application/pdf", null, body)))) {
            var result = client.selectDownload(URL, HOSTS, uri -> uri.getPath().startsWith("/download/"),
                    root.resolve("file.bin"), 3, bytes -> { charged.addAndGet(bytes); return true; });
            assertThat(result.bytes()).isEqualTo(3);
            assertThat(result.sha256()).hasSize(64);
            assertThat(Files.readAllBytes(root.resolve("file.bin"))).containsExactly(1, 2, 3);
        }
    }

    @Test void deniedBudgetReadsNoBytesAndDeletesOnlyNewPartialFile() throws Exception {
        InputStream body = new InputStream() {
            @Override public int read() { throw new AssertionError("예산 거부 후 읽기 금지"); }
        };
        try (var client = selectClient((target, remaining, handler) -> handler.handle(
                new AttachmentPinnedDownloadClient.Response(200, null, -1, "application/pdf", null, body)))) {
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, uri -> true,
                    root.resolve("file.bin"), 10, bytes -> false))
                    .isInstanceOf(IOException.class).hasMessage("SOURCE_BYTE_LIMIT");
            assertThat(root.resolve("file.bin")).doesNotExist();
        }
    }

    @Test void failedStreamKeepsChargedBudgetAndCleansPartialFile() throws Exception {
        AtomicLong charged = new AtomicLong();
        InputStream body = new InputStream() {
            int calls;
            @Override public int read() throws IOException { return 1; }
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                if (calls++ > 0) throw new IOException("FIXTURE_STREAM_FAILURE");
                bytes[offset] = 1;
                return 1;
            }
        };
        try (var client = selectClient((target, remaining, handler) -> handler.handle(
                new AttachmentPinnedDownloadClient.Response(200, null, -1, null, null, body)))) {
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, uri -> true,
                    root.resolve("file.bin"), 20, bytes -> { charged.addAndGet(bytes); return true; }))
                    .isInstanceOf(IOException.class).hasMessage("FIXTURE_STREAM_FAILURE");
            assertThat(charged.get()).isEqualTo(20);
            assertThat(root.resolve("file.bin")).doesNotExist();
        }
    }

    @Test void runtimeBudgetFailureAlsoCleansPartialFileAndDoesNotOverwriteExistingFile() throws Exception {
        try (var client = selectClient((target, remaining, handler) -> handler.handle(
                new AttachmentPinnedDownloadClient.Response(200, null, 3, null, null,
                        new ByteArrayInputStream(new byte[]{1, 2, 3}))))) {
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, uri -> true,
                    root.resolve("new.bin"), 3, bytes -> { throw new IllegalStateException("DB_FAILURE"); }))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(root.resolve("new.bin")).doesNotExist();
            Path existing = Files.writeString(root.resolve("existing.bin"), "기존 파일");
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, existing, 3)).isInstanceOf(IOException.class);
            assertThat(Files.readString(existing)).isEqualTo("기존 파일");
        }
    }

    @Test void redirectRevalidatesPathBeforeDnsOrSecondHttp() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        try (var client = selectClient((target, remaining, handler) -> {
            requests.incrementAndGet();
            return handler.handle(new AttachmentPinnedDownloadClient.Response(302, "/private/secret", -1, null, null, null));
        })) {
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, uri -> uri.getPath().startsWith("/download/"),
                    root.resolve("file.bin"), 1024, bytes -> true))
                    .isInstanceOf(IOException.class).hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
            assertThat(requests.get()).isEqualTo(1);
        }
    }

    @Test void redirectsShareOneDeadlineInsteadOfResettingTimeout() throws Exception {
        List<Duration> remainingTimes = new ArrayList<>();
        try (var client = selectClient((target, remaining, handler) -> {
            remainingTimes.add(remaining);
            if (remainingTimes.size() < 3) return handler.handle(new AttachmentPinnedDownloadClient.Response(
                    302, "/download/next", -1, null, null, null));
            return handler.handle(new AttachmentPinnedDownloadClient.Response(
                    200, null, 1, null, null, new ByteArrayInputStream(new byte[]{1})));
        })) {
            client.selectDownload(URL, HOSTS, root.resolve("file.bin"), 1024);
            assertThat(remainingTimes).hasSize(3);
            assertThat(remainingTimes.get(1)).isLessThan(remainingTimes.get(0));
            assertThat(remainingTimes.get(2)).isLessThan(remainingTimes.get(1));
        }
    }

    @Test void stalledDnsTimesOutWithoutStartingHttpAndResolverExits() throws Exception {
        CountDownLatch released = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        var validator = new ProviderContentUrlValidator(host -> {
            try { released.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new UnknownHostException(); }
            finally { stopped.countDown(); }
            return new InetAddress[]{InetAddress.getByAddress(new byte[]{8, 8, 8, 8})};
        });
        try (var client = new AttachmentPinnedDownloadClient(validator,
                (target, remaining, handler) -> { throw new AssertionError("DNS timeout 후 HTTP 금지"); },
                Duration.ofMillis(200))) {
            long start = System.nanoTime();
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, root.resolve("file.bin"), 1024))
                    .isInstanceOf(IOException.class).hasMessage("ATTACHMENT_DNS_TIMEOUT");
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
            assertThat(stopped.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(root.resolve("file.bin")).doesNotExist();
        } finally { released.countDown(); }
    }

    @Test void unknownLengthCannotReadPastItsBudgetAndTruncatedKnownLengthIsNotSuccess() throws Exception {
        AtomicInteger consumed = new AtomicInteger();
        InputStream infinite = new InputStream() {
            @Override public int read() { consumed.incrementAndGet(); return 1; }
        };
        try (var client = selectClient((target, remaining, handler) -> handler.handle(
                new AttachmentPinnedDownloadClient.Response(200, null, -1, null, null, infinite)))) {
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, root.resolve("limited.bin"), 5))
                    .isInstanceOf(IOException.class).hasMessage("SOURCE_BYTE_LIMIT");
            assertThat(consumed.get()).isEqualTo(5);
            assertThat(root.resolve("limited.bin")).doesNotExist();
        }
        try (var client = selectClient((target, remaining, handler) -> handler.handle(
                new AttachmentPinnedDownloadClient.Response(200, null, 5, null, null,
                        new ByteArrayInputStream(new byte[]{1}))))) {
            assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, root.resolve("truncated.bin"), 5))
                    .isInstanceOf(IOException.class).hasMessage("ATTACHMENT_TRUNCATED_FILE");
            assertThat(root.resolve("truncated.bin")).doesNotExist();
        }
    }

    @Test void compressedOrOverLimitResponsesAreRejectedBeforeReading() throws Exception {
        for (var response : List.of(
                new AttachmentPinnedDownloadClient.Response(200, null, 100, null, null, InputStream.nullInputStream()),
                new AttachmentPinnedDownloadClient.Response(200, null, 2, null, "gzip", InputStream.nullInputStream()))) {
            try (var client = selectClient((target, remaining, handler) -> handler.handle(response))) {
                assertThatThrownBy(() -> client.selectDownload(URL, HOSTS, root.resolve("file.bin"), 10))
                        .isInstanceOf(IOException.class);
                assertThat(root.resolve("file.bin")).doesNotExist();
            }
        }
    }

    private AttachmentPinnedDownloadClient selectClient(AttachmentPinnedDownloadClient.Transport transport) {
        return new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(host ->
                new InetAddress[]{InetAddress.getByAddress(new byte[]{8, 8, 8, 8})}), transport, Duration.ofSeconds(30));
    }

    @Test void dedicatedProfileMustApproveMethodAndFormBeforePostNetwork() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var request = new AttachmentPinnedDownloadClient.Request(URL, "POST", java.util.Map.of("file_id", "123"));
        var validator = new ProviderContentUrlValidator(host -> new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})});
        try (var client = new AttachmentPinnedDownloadClient(validator, (target, selected, remaining, handler) -> {
            calls.incrementAndGet();
            assertThat(selected.method()).isEqualTo("POST");
            assertThat(selected.form()).containsEntry("file_id", "123");
            return handler.handle(new AttachmentPinnedDownloadClient.Response(200,null,1,"application/pdf",null,
                    new ByteArrayInputStream(new byte[]{1}),"attachment; filename=test.pdf"));
        }, Duration.ofSeconds(30))) {
            assertThatThrownBy(() -> client.selectDownload(request, HOSTS, selected -> false, root.resolve("denied.bin"),10, bytes -> true))
                    .hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
            assertThat(calls.get()).isZero();
            var result = client.selectDownload(request,HOSTS,selected -> selected.equals(request),root.resolve("post.bin"),10,bytes -> true);
            assertThat(result.contentDisposition()).isEqualTo("attachment; filename=test.pdf");
            assertThat(calls.get()).isEqualTo(1);
            assertThat(result.toString()).doesNotContain("filename");
        }
    }

    @Test void postRedirectNeverForwardsFileNamesOrParametersToAnotherEndpoint() throws Exception {
        for (int status : List.of(301,302,303,307,308)) {
            AtomicInteger calls = new AtomicInteger();
            var validator = new ProviderContentUrlValidator(host -> new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})});
            try (var client = new AttachmentPinnedDownloadClient(validator, (target, selected, remaining, handler) -> {
                calls.incrementAndGet();
                return handler.handle(new AttachmentPinnedDownloadClient.Response(status,"/other",-1,null,null,null));
            }, Duration.ofSeconds(30))) {
                var request = new AttachmentPinnedDownloadClient.Request(URL,"POST",java.util.Map.of("file_id","123"));
                assertThatThrownBy(() -> client.selectDownload(request,HOSTS,selected -> true,root.resolve("post.bin"),10,bytes -> true))
                        .hasMessage("ATTACHMENT_POST_REDIRECT_BLOCKED");
                assertThat(calls.get()).isEqualTo(1);
                assertThat(root.resolve("post.bin")).doesNotExist();
            }
        }
    }

    @Test void formIsBoundedImmutableAndDoesNotAppearInDiagnosticStrings() {
        var form = new java.util.HashMap<String,String>();
        form.put("user_file_nm","표본 공고문.hwp");
        var request = new AttachmentPinnedDownloadClient.Request(URL,"POST",form);
        form.put("user_file_nm","changed");
        assertThat(request.form().get("user_file_nm")).isEqualTo("표본 공고문.hwp");
        assertThat(request.toString()).doesNotContain("표본","approved.example");
        assertThatThrownBy(() -> new AttachmentPinnedDownloadClient.Request(URL,"DELETE",java.util.Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttachmentPinnedDownloadClient.Request(URL,"GET",form)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttachmentPinnedDownloadClient.Request(URL,"POST",java.util.Map.of("file_path","x\r\ny"))).hasMessage("ATTACHMENT_FORM_INVALID");
        assertThatThrownBy(() -> new AttachmentPinnedDownloadClient.Request(URL,"POST",java.util.Map.of("file_name","가".repeat(2048)))).hasMessage("ATTACHMENT_FORM_LIMIT");
        var fields=new java.util.LinkedHashMap<String,String>();
        for(int i=0;i<10;i++) fields.put("field_"+i,"value");
        assertThat(new AttachmentPinnedDownloadClient.Request(URL,"POST",fields).form()).hasSize(10);
        fields.put("field_10","value");
        assertThatThrownBy(()->new AttachmentPinnedDownloadClient.Request(URL,"POST",fields)).hasMessage("ATTACHMENT_REQUEST_INVALID");
    }
}
