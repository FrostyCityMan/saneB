package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttachmentPublicSessionDownloadTest {
    @TempDir Path root;
    private static final URI DETAIL = URI.create("https://public.example/page.do?id=1");
    private static final URI FILE = URI.create("https://public.example/file.do?id=2");
    private static final byte[] HTML = "<html>official-link</html>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PDF = "%PDF-unit-test".getBytes(StandardCharsets.UTF_8);
    private AttachmentPinnedDownloadClient.Request selectRequest(boolean linkMatches) {
        var plan = new AttachmentPinnedDownloadClient.PublicSessionPlan(DETAIL, Set.of("JSESSIONID"),
                html -> linkMatches && html.equals(new String(HTML, StandardCharsets.UTF_8)));
        return new AttachmentPinnedDownloadClient.Request(FILE, "GET", Map.of(), null, false, plan);
    }
    private ProviderContentUrlValidator selectValidator() {
        return new ProviderContentUrlValidator(host -> new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})});
    }
    private AttachmentPinnedDownloadClient.Response selectResponse(boolean detail) {
        byte[] body = detail ? HTML : PDF;
        return new AttachmentPinnedDownloadClient.Response(200, null, body.length,
                detail ? "text/html; charset=UTF-8" : "application/pdf", null, new ByteArrayInputStream(body), null,
                detail ? List.of("JSESSIONID=unit-only; Path=/; Secure") : List.of());
    }
    @Test void bothRequestsUseApprovalPinnedDnsByteBudgetAndOnePrivateCookieContext() throws Exception {
        var calls = new AtomicInteger();var approvals = new AtomicInteger();var bytes = new AtomicLong();
        try (var client = new AttachmentPinnedDownloadClient(selectValidator(), (target, request, remaining, handler, cookies) -> {
            boolean detail = calls.incrementAndGet() == 1;
            assertThat(request.uri()).isEqualTo(detail ? DETAIL : FILE);
            assertThat(cookies).isEqualTo(detail ? null : "JSESSIONID=unit-only");
            assertThat(remaining).isLessThanOrEqualTo(Duration.ofSeconds(2));
            return handler.handle(selectResponse(detail));
        }, Duration.ofSeconds(2))) {
            var result = client.selectDownload(selectRequest(true), Set.of("public.example"), r -> {approvals.incrementAndGet();return true;},
                    root.resolve("file.bin"), 1024, amount -> {bytes.addAndGet(amount);return true;});
            assertThat(result.bytes()).isEqualTo(PDF.length);
            assertThat(Files.readAllBytes(root.resolve("file.bin"))).isEqualTo(PDF);
        }
        assertThat(calls.get()).isEqualTo(2);assertThat(approvals.get()).isEqualTo(2);
        assertThat(bytes.get()).isEqualTo(HTML.length + PDF.length);
        try (var paths=Files.list(root)) {assertThat(paths.toList()).hasSize(1);}
    }
    @Test void changedOfficialLinkStopsBeforeFileAndNeverSavesBootstrap() throws Exception {
        var calls=new AtomicInteger();
        try(var client=new AttachmentPinnedDownloadClient(selectValidator(),(target,request,remaining,handler,cookies)->{
            assertThat(calls.incrementAndGet()).isEqualTo(1);return handler.handle(selectResponse(true));
        },Duration.ofSeconds(2))){
            assertThatThrownBy(()->client.selectDownload(selectRequest(false),Set.of("public.example"),r->true,root.resolve("file"),1024,b->true))
                    .hasMessage("ATTACHMENT_PUBLIC_SESSION_LINK_CHANGED");
        }
        assertThat(root.resolve("file")).doesNotExist();
    }
    @ParameterizedTest @ValueSource(ints={1,2})
    void denialAtEitherRequestStopsBeforeHttpAndPreservesPartialFileCleanup(int denied) throws Exception {
        var calls=new AtomicInteger();var approval=new AtomicInteger();
        try(var client=new AttachmentPinnedDownloadClient(selectValidator(),(target,request,remaining,handler,cookies)->{
            calls.incrementAndGet();return handler.handle(selectResponse(true));
        },Duration.ofSeconds(2))){
            assertThatThrownBy(()->client.selectDownload(selectRequest(true),Set.of("public.example"),r->approval.incrementAndGet()!=denied,
                    root.resolve("file"),1024,b->true)).hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
        }
        assertThat(calls.get()).isEqualTo(denied-1);assertThat(root.resolve("file")).doesNotExist();
    }
    @ParameterizedTest @ValueSource(ints={1,2})
    void redirectsAreNotFollowedAtEitherStage(int redirectAt) throws Exception {
        var calls=new AtomicInteger();
        try(var client=new AttachmentPinnedDownloadClient(selectValidator(),(target,request,remaining,handler,cookies)->{
            return handler.handle(calls.incrementAndGet()==redirectAt
                    ?new AttachmentPinnedDownloadClient.Response(302,"https://evil.example/login",0,"text/html",null,new ByteArrayInputStream(new byte[0]))
                    :selectResponse(true));
        },Duration.ofSeconds(2))){
            assertThatThrownBy(()->client.selectDownload(selectRequest(true),Set.of("public.example"),r->true,root.resolve("file"),1024,b->true))
                    .hasMessage("ATTACHMENT_HTTP_302");
        }
        assertThat(calls.get()).isEqualTo(redirectAt);assertThat(root.resolve("file")).doesNotExist();
    }
    @Test void budgetFailureDoesNotOpenFileRequestOrKeepPartialOutput() throws Exception {
        for(int denyAt:List.of(1,2)){
            var calls=new AtomicInteger();var reservations=new AtomicInteger();
            try(var client=new AttachmentPinnedDownloadClient(selectValidator(),(target,request,remaining,handler,cookies)->
                    handler.handle(selectResponse(calls.incrementAndGet()==1)),Duration.ofSeconds(2))){
                assertThatThrownBy(()->client.selectDownload(selectRequest(true),Set.of("public.example"),r->true,root.resolve("file"),1024,
                        b->reservations.incrementAndGet()!=denyAt)).hasMessage("SOURCE_BYTE_LIMIT");
            }
            assertThat(calls.get()).isEqualTo(denyAt);assertThat(root.resolve("file")).doesNotExist();
        }
    }
    @Test void cumulativeLimitIncludesBootstrapAndPreservesExistingOutput() throws Exception {
        var calls=new AtomicInteger();
        try(var client=new AttachmentPinnedDownloadClient(selectValidator(),(target,request,remaining,handler,cookies)->
                handler.handle(selectResponse(calls.incrementAndGet()==1)),Duration.ofSeconds(2))){
            assertThatThrownBy(()->client.selectDownload(selectRequest(true),Set.of("public.example"),r->true,root.resolve("file"),HTML.length,b->true))
                    .hasMessage("SOURCE_BYTE_LIMIT");
            assertThat(calls.get()).isEqualTo(1);
            Files.writeString(root.resolve("file"),"existing");
            assertThatThrownBy(()->client.selectDownload(selectRequest(true),Set.of("public.example"),r->true,root.resolve("file"),1024,b->true))
                    .hasMessage("ATTACHMENT_OUTPUT_EXISTS");
            assertThat(calls.get()).isEqualTo(1);assertThat(Files.readString(root.resolve("file"))).isEqualTo("existing");
        }
    }
}
