package com.saneb.domain.announcementsource.provider.content;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

/** 검증된 IP에만 연결하며 DNS·redirect 전체에 하나의 30초 deadline을 적용합니다. */
@Component
public class AttachmentPinnedDownloadClient implements AutoCloseable {
    public record Download(long bytes, String sha256, String contentType, String contentDisposition) {
        public Download(long bytes, String sha256, String contentType) { this(bytes, sha256, contentType, null); }
        @Override public String toString() { return "Download[bytes=" + bytes + ",sha256=" + sha256 + "]"; }
    }
    /** 검증된 시스템 프로필만 생성하는 일회성 요청. POST 본문/URL은 저장하거나 로그로 출력하지 않는다. */
    public record Request(URI uri, String method, Map<String, String> form) {
        public Request {
            if (uri == null || method == null || !Set.of("GET", "POST").contains(method) || form == null || form.size() > 10
                    || ("GET".equals(method) && !form.isEmpty()) || ("POST".equals(method) && form.isEmpty()))
                throw new IllegalArgumentException("ATTACHMENT_REQUEST_INVALID");
            int bytes = 0;
            for (var entry : form.entrySet()) {
                if (entry.getKey() == null || !entry.getKey().matches("[A-Za-z_][A-Za-z0-9_]{0,63}")
                        || entry.getValue() == null || entry.getValue().length() > 2048
                        || entry.getValue().codePoints().anyMatch(Character::isISOControl))
                    throw new IllegalArgumentException("ATTACHMENT_FORM_INVALID");
                bytes += entry.getKey().length() + 2 + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8).length();
            }
            if (bytes > 8192) throw new IllegalArgumentException("ATTACHMENT_FORM_LIMIT");
            form = Collections.unmodifiableMap(new TreeMap<>(form));
        }
        public static Request selectGet(URI uri) { return new Request(uri, "GET", Map.of()); }
        @Override public String toString() { return "AttachmentRequest[method=" + method + ",requestValues=REDACTED]"; }
    }

    /** 읽기 전에 DB 누적 예산을 확보합니다. 미사용 예약량도 재시도에서 환급하지 않습니다. */
    @FunctionalInterface public interface ByteReservation { boolean reserve(long bytes); }

    @FunctionalInterface interface ResponseHandler { Reply handle(Response response) throws IOException; }
    @FunctionalInterface interface Transport {
        Reply selectResponse(ProviderContentRequestTarget target, Duration remaining, ResponseHandler handler) throws IOException;
    }
    @FunctionalInterface interface RequestTransport {
        Reply selectResponse(ProviderContentRequestTarget target, Request request, Duration remaining, ResponseHandler handler) throws IOException;
    }
    record Response(int status, String redirect, long contentLength, String contentType,
                    String encoding, InputStream body, String contentDisposition) {
        Response(int status, String redirect, long contentLength, String contentType, String encoding, InputStream body) {
            this(status, redirect, contentLength, contentType, encoding, body, null);
        }
    }
    record Reply(String redirect, Download download) { }

    private static final long FILE_BYTE_LIMIT = 20L * 1024 * 1024;
    private final ProviderContentUrlValidator validator;
    private final RequestTransport transport;
    private final Duration totalTimeout;
    // 지연된 OS DNS 호출은 HTTP로 이어지지 않으며 대기열/스레드를 무제한 생성하지 않습니다.
    private final ThreadPoolExecutor resolvers = new ThreadPoolExecutor(0, 2, 1, TimeUnit.SECONDS,
            new SynchronousQueue<>(), Thread.ofPlatform().daemon(true).name("saneb-attachment-dns-", 0).factory());

    public AttachmentPinnedDownloadClient() {
        this(new ProviderContentUrlValidator(InetAddress::getAllByName));
    }
    AttachmentPinnedDownloadClient(ProviderContentUrlValidator validator) {
        this(validator, AttachmentPinnedDownloadClient::selectHttpResponse, Duration.ofSeconds(30));
    }
    AttachmentPinnedDownloadClient(ProviderContentUrlValidator validator, Transport transport, Duration totalTimeout) {
        this(validator, (target, request, remaining, handler) -> {
            if (!"GET".equals(request.method())) throw new IOException("ATTACHMENT_METHOD_NOT_APPROVED");
            return transport.selectResponse(target, remaining, handler);
        }, totalTimeout);
    }
    AttachmentPinnedDownloadClient(ProviderContentUrlValidator validator, RequestTransport transport, Duration totalTimeout) {
        if (totalTimeout.isNegative() || totalTimeout.isZero() || totalTimeout.compareTo(Duration.ofSeconds(30)) > 0)
            throw new IllegalArgumentException("첨부 요청 제한시간은 0초 초과 30초 이하여야 합니다.");
        this.validator = validator;
        this.transport = transport;
        this.totalTimeout = totalTimeout;
    }

    /** 기존 격리 CLI 계약. 상시 worker는 path 검증과 DB 예산 callback이 있는 overload를 사용합니다. */
    public Download selectDownload(URI uri, Set<String> approvedHosts, Path output, long remainingSourceBytes) throws IOException {
        return selectDownload(uri, approvedHosts, candidate -> true, output, remainingSourceBytes, bytes -> true);
    }

    public Download selectDownload(URI uri, Set<String> approvedHosts, Predicate<URI> approvedPath,
                                   Path output, long remainingSourceBytes, ByteReservation budget) throws IOException {
        return selectDownload(Request.selectGet(uri), approvedHosts,
                request -> "GET".equals(request.method()) && approvedPath.test(request.uri()), output, remainingSourceBytes, budget);
    }

    public Download selectDownload(Request initial, Set<String> approvedHosts, Predicate<Request> approvedRequest,
                                   Path output, long remainingSourceBytes, ByteReservation budget) throws IOException {
        long byteLimit = Math.min(FILE_BYTE_LIMIT, remainingSourceBytes);
        if (byteLimit <= 0) throw new IOException("SOURCE_BYTE_LIMIT");
        long deadline = System.nanoTime() + totalTimeout.toNanos();
        URI current = initial.uri();
        boolean[] createdOutput = {false};
        boolean succeeded = false;
        try {
            for (int redirects = 0; redirects <= 3; redirects++) {
                if (current == null || !"https".equalsIgnoreCase(current.getScheme()) || current.getHost() == null
                        || (current.getPort() != -1 && current.getPort() != 443)
                        || !approvedHosts.contains(current.getHost().toLowerCase(Locale.ROOT)))
                    throw new IOException("ATTACHMENT_HOST_NOT_APPROVED");
                Request selected = new Request(current, initial.method(), initial.form());
                if (current.getUserInfo() != null || current.getFragment() != null || !approvedRequest.test(selected))
                    throw new IOException("ATTACHMENT_PATH_NOT_APPROVED");
                ProviderContentRequestTarget target = selectTarget(current, deadline);
                Reply reply = transport.selectResponse(target, selected, selectRemaining(deadline), response -> {
                    selectRemaining(deadline);
                    if (Set.of(301, 302, 303, 307, 308).contains(response.status())) {
                        // 검증한 직접 POST만 지원한다. 원문 이름/서버 파일 경로를 redirect 대상에 재전송하지 않는다.
                        if (!"GET".equals(initial.method())) throw new IOException("ATTACHMENT_POST_REDIRECT_BLOCKED");
                        if (response.redirect() == null || response.redirect().isBlank())
                            throw new IOException("ATTACHMENT_REDIRECT_INVALID");
                        return new Reply(response.redirect(), null);
                    }
                    if (response.status() != 200) throw new IOException("ATTACHMENT_HTTP_" + response.status());
                    if (response.body() == null || response.contentLength() > byteLimit)
                        throw new IOException("ATTACHMENT_BYTE_LIMIT");
                    if (response.encoding() != null && !"identity".equalsIgnoreCase(response.encoding()))
                        throw new IOException("ATTACHMENT_ENCODING_UNSUPPORTED");
                    if (response.contentDisposition() != null && response.contentDisposition().length() > 2000)
                        throw new IOException("ATTACHMENT_HEADER_LIMIT");
                    return new Reply(null, saveBody(response, output, byteLimit, budget, deadline, createdOutput));
                });
                selectRemaining(deadline);
                if (reply.download() != null) { succeeded = true; return reply.download(); }
                current = current.resolve(reply.redirect());
            }
            throw new IOException("ATTACHMENT_REDIRECT_LIMIT");
        } catch (ProviderContentValidationException | IllegalArgumentException exception) {
            throw new IOException("ATTACHMENT_URL_BLOCKED");
        } finally {
            // DB callback/검증 오류에서도 이 호출이 만든 부분파일만 정리합니다.
            if (!succeeded && createdOutput[0]) Files.deleteIfExists(output);
        }
    }

    private ProviderContentRequestTarget selectTarget(URI uri, long deadline) throws IOException {
        java.util.concurrent.Future<ProviderContentRequestTarget> task;
        try {
            task = resolvers.submit(() -> {
                var validated = validator.selectValidatedRequest("https://" + uri.getHost(), uri.toASCIIString());
                return validator.selectRequestTarget(validated.detailUri(), validated.allowedHost());
            });
        } catch (RejectedExecutionException exception) { throw new IOException("ATTACHMENT_DNS_BUSY"); }
        try {
            return task.get(Math.min(selectRemaining(deadline).toNanos(), Duration.ofSeconds(3).toNanos()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) { throw new IOException("ATTACHMENT_DNS_TIMEOUT"); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IOException("ATTACHMENT_CANCELLED"); }
        catch (ExecutionException exception) {
            // 조회 불능은 보안 차단과 다르다. 검증된 고정 코드만 기존 worker의 제한 재시도로 전달한다.
            // 사설 주소·미승인 URL·예상하지 못한 resolver 오류는 계속 차단하며 원인 메시지는 복사하지 않는다.
            if (exception.getCause() instanceof ProviderContentValidationException validation
                    && validation.selectFailureCode() == ProviderContentCodes.FailureCode.DNS_LOOKUP_FAILED)
                throw new IOException("ATTACHMENT_DNS_LOOKUP_FAILED");
            throw new IOException("ATTACHMENT_URL_BLOCKED");
        }
        finally { task.cancel(true); }
    }

    private Download saveBody(Response response, Path output, long byteLimit, ByteReservation budget,
                              long deadline, boolean[] createdOutput) throws IOException {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
        long total = 0;
        long reserved = 0;
        long credit = 0;
        try (var file = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            createdOutput[0] = true;
            byte[] buffer = new byte[8192];
            while (response.contentLength() < 0 || total < response.contentLength()) {
                selectRemaining(deadline);
                if (credit == 0) {
                    long allocation = Math.min(buffer.length, byteLimit - reserved);
                    if (response.contentLength() >= 0) allocation = Math.min(allocation, response.contentLength() - total);
                    if (allocation <= 0 || !budget.reserve(allocation)) throw new IOException("SOURCE_BYTE_LIMIT");
                    reserved += allocation;
                    credit = allocation;
                }
                int count = response.body().read(buffer, 0, (int) Math.min(buffer.length, credit));
                if (count == -1) {
                    if (response.contentLength() >= 0 && total != response.contentLength())
                        throw new IOException("ATTACHMENT_TRUNCATED_FILE");
                    break;
                }
                credit -= count;
                total += count;
                file.write(buffer, 0, count);
                digest.update(buffer, 0, count);
            }
        }
        if (total == 0) throw new IOException("ATTACHMENT_EMPTY_FILE");
        return new Download(total, HexFormat.of().formatHex(digest.digest()), response.contentType(), response.contentDisposition());
    }

    private static Duration selectRemaining(long deadline) throws IOException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0 || Thread.currentThread().isInterrupted()) throw new IOException("ATTACHMENT_TOTAL_TIMEOUT");
        return Duration.ofNanos(remaining);
    }

    private static Reply selectHttpResponse(ProviderContentRequestTarget target, Request selected, Duration remaining,
                                             ResponseHandler handler) throws IOException {
        long timeoutMillis = Math.max(1, remaining.toMillis());
        var manager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(new PinnedProviderContentHttpTransport.PinnedDnsResolver(target))
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(Math.min(3000, timeoutMillis)))
                        .setSocketTimeout(Timeout.ofMilliseconds(Math.min(10000, timeoutMillis))).build())
                .setMaxConnTotal(1).setMaxConnPerRoute(1).build();
        HttpUriRequestBase request;
        if ("POST".equals(selected.method())) {
            var post = new HttpPost(target.uri());
            post.setEntity(new UrlEncodedFormEntity(selected.form().entrySet().stream()
                    .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).toList(), StandardCharsets.UTF_8));
            request = post;
        } else request = new HttpGet(target.uri());
        request.setConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofMilliseconds(Math.min(10000, timeoutMillis)))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(Math.min(3000, timeoutMillis)))
                .setRedirectsEnabled(false).build());
        request.setHeader("Accept-Encoding", "identity");
        request.setHeader("User-Agent", "saneB-attachment-collector/1.0");
        try (var watchdog = Executors.newSingleThreadScheduledExecutor();
             var client = HttpClients.custom().setConnectionManager(manager).disableRedirectHandling()
                     .disableAutomaticRetries().disableCookieManagement().disableContentCompression().build()) {
            watchdog.schedule(request::cancel, remaining.toNanos(), TimeUnit.NANOSECONDS);
            try {
                try (var response = client.executeOpen(null, request, null)) {
                    try {
                        var entity = response.getEntity();
                        var redirect = response.getFirstHeader("Location");
                        var encoding = response.getFirstHeader("Content-Encoding");
                        var disposition = response.getFirstHeader("Content-Disposition");
                        return handler.handle(new Response(response.getCode(), redirect == null ? null : redirect.getValue(),
                                entity == null ? 0 : entity.getContentLength(), entity == null ? null : entity.getContentType(),
                                encoding == null ? null : encoding.getValue(), entity == null ? null : entity.getContent(),
                                disposition == null ? null : disposition.getValue()));
                    } finally {
                        // redirect/오류 body를 라이브러리의 연결 재사용 정리 단계에서 무제한 drain하지 않습니다.
                        request.cancel();
                    }
                }
            } finally { watchdog.shutdownNow(); }
        }
    }

    @Override @PreDestroy public void close() { resolvers.shutdownNow(); }
}
