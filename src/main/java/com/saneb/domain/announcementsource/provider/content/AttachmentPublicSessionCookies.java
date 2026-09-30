package com.saneb.domain.announcementsource.provider.content;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * 공개 상세 한 번에서 받은 쿠키를 그 상세의 파일 한 번에만 전달하는 메모리 경계.
 * 로그인 쿠키를 입력받거나 전역 cookie jar를 공유하지 않는다.
 * 호출자는 상세 응답 상태·본문·공식 첨부 링크를 검증한 뒤에만 쿠키를 등록해야 한다.
 * DNS·본문·파일 서명·예산 검증은 공통 pinned 다운로드 경로에서 별도로 수행한다.
 */
final class AttachmentPublicSessionCookies implements AutoCloseable {
    private enum State { BOOTSTRAP, READY, CONSUMED, CLOSED }
    private record Entry(String name, String value, long expiresAt) {
        @Override public String toString() { return "PublicCookie[value=REDACTED]"; }
    }
    private static final long LIFETIME_NANOS = Duration.ofSeconds(30).toNanos();
    private final URI detail;
    private final URI file;
    private final Set<String> allowedNames;
    private final LongSupplier clock;
    private final long startedAt;
    private final List<Entry> entries = new ArrayList<>();
    private State state = State.BOOTSTRAP;

    AttachmentPublicSessionCookies(URI detail, URI file, Set<String> allowedNames) {
        this(detail, file, allowedNames, System::nanoTime);
    }

    AttachmentPublicSessionCookies(URI detail, URI file, Set<String> allowedNames, LongSupplier clock) {
        if (!selectSafeUri(detail) || !selectSafeUri(file)
                || !detail.getHost().equalsIgnoreCase(file.getHost()) || detail.equals(file)
                || allowedNames == null || allowedNames.isEmpty() || allowedNames.size() > 8
                || allowedNames.stream().anyMatch(name -> name == null || !name.matches("[A-Za-z][A-Za-z0-9_-]{0,63}"))
                || clock == null) throw new IllegalArgumentException("ATTACHMENT_PUBLIC_SESSION_INVALID");
        this.detail = detail;
        this.file = file;
        this.allowedNames = Set.copyOf(allowedNames);
        this.clock = clock;
        this.startedAt = clock.getAsLong();
    }

    /** 원시 헤더는 저장하지 않으며 값이 포함된 예외를 밖으로 전달하지 않는다. */
    void saveBootstrapCookies(URI actualDetail, List<String> headers) throws IOException {
        try {
            validateState(State.BOOTSTRAP);
            if (!detail.equals(actualDetail) || headers == null || headers.size() > 16)
                throw new IOException("ATTACHMENT_PUBLIC_SESSION_HEADERS_INVALID");
            int total = 0;
            var seen = new HashSet<String>();
            for (String header : headers) {
                if (header == null || header.length() > 4096 || header.codePoints().anyMatch(Character::isISOControl))
                    throw new IOException("ATTACHMENT_PUBLIC_SESSION_HEADERS_INVALID");
                total += header.length();
                if (total > 16384) throw new IOException("ATTACHMENT_PUBLIC_SESSION_HEADER_LIMIT");
                for (HttpCookie cookie : HttpCookie.parse(header)) {
                    if (!allowedNames.contains(cookie.getName())) continue;
                    // 같은 이름의 모호한 헤더나 다른 호스트까지 허용하는 Domain은 거부한다.
                    if (!seen.add(cookie.getName()))
                        throw new IOException("ATTACHMENT_PUBLIC_SESSION_COOKIE_CONFLICT");
                    String domain = cookie.getDomain();
                    if (domain != null && !domain.replaceFirst("^\\.", "").equalsIgnoreCase(detail.getHost()))
                        throw new IOException("ATTACHMENT_PUBLIC_SESSION_DOMAIN_BLOCKED");
                    String path = cookie.getPath();
                    if (path == null || path.isEmpty()) {
                        int slash = detail.getPath().lastIndexOf('/');
                        path = slash <= 0 ? "/" : detail.getPath().substring(0, slash);
                    }
                    if (!path.startsWith("/") || path.contains("\\") || path.contains("..")
                            || path.length() > 1024 || path.codePoints().anyMatch(Character::isISOControl))
                        throw new IOException("ATTACHMENT_PUBLIC_SESSION_PATH_BLOCKED");
                    // RFC 경로 경계: /program은 /programs에 해당하지 않는다.
                    String filePath = file.getPath();
                    if (!(filePath.equals(path) || filePath.startsWith(path.endsWith("/") ? path : path + "/"))) continue;
                    String value = cookie.getValue();
                    if (value == null || value.length() > 2048 || !value.chars().allMatch(c ->
                            c == 0x21 || c >= 0x23 && c <= 0x2b || c >= 0x2d && c <= 0x3a
                                    || c >= 0x3c && c <= 0x5b || c >= 0x5d && c <= 0x7e))
                        throw new IOException("ATTACHMENT_PUBLIC_SESSION_VALUE_INVALID");
                    if (cookie.getMaxAge() == 0 || value.isEmpty()) continue;
                    long age = cookie.getMaxAge() < 0 ? 30 : Math.min(30, cookie.getMaxAge());
                    entries.add(new Entry(cookie.getName(), value, clock.getAsLong() + Duration.ofSeconds(age).toNanos()));
                }
            }
            state = State.READY;
        } catch (IOException failure) {
            close();
            throw failure;
        } catch (IllegalArgumentException failure) {
            close();
            throw new IOException("ATTACHMENT_PUBLIC_SESSION_HEADERS_INVALID");
        }
    }

    /** 정확히 승인된 파일 URI에서 한 번만 사용할 수 있다. redirect·재시도·다른 파일에 재사용하지 않는다. */
    String selectDownloadCookieHeader(URI target) throws IOException {
        try {
            validateState(State.READY);
            if (!file.equals(target)) throw new IOException("ATTACHMENT_PUBLIC_SESSION_TARGET_BLOCKED");
            long now = clock.getAsLong();
            String value = entries.stream().filter(entry -> entry.expiresAt() - now > 0)
                    .map(entry -> entry.name() + "=" + entry.value()).collect(java.util.stream.Collectors.joining("; "));
            if (value.isEmpty()) throw new IOException("ATTACHMENT_PUBLIC_SESSION_COOKIE_MISSING");
            if (value.length() > 8192) throw new IOException("ATTACHMENT_PUBLIC_SESSION_HEADER_LIMIT");
            entries.clear();
            state = State.CONSUMED;
            return value;
        } catch (IOException failure) {
            close();
            throw failure;
        }
    }

    private void validateState(State expected) throws IOException {
        if (state != expected) throw new IOException("ATTACHMENT_PUBLIC_SESSION_STATE_INVALID");
        long elapsed = clock.getAsLong() - startedAt;
        if (elapsed < 0 || elapsed >= LIFETIME_NANOS) throw new IOException("ATTACHMENT_PUBLIC_SESSION_EXPIRED");
    }

    private static boolean selectSafeUri(URI uri) {
        return uri != null && "https".equals(uri.getScheme()) && uri.getHost() != null
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null
                && uri.getFragment() == null && uri.getPath() != null && uri.getPath().startsWith("/")
                && uri.equals(uri.normalize()) && !uri.getPath().contains("..") && !uri.getPath().contains("\\")
                && uri.toASCIIString().length() <= 4096
                && uri.toString().codePoints().noneMatch(Character::isISOControl)
                && uri.getPath().codePoints().noneMatch(Character::isISOControl)
                && (uri.getQuery() == null || uri.getQuery().codePoints().noneMatch(Character::isISOControl));
    }

    @Override public void close() { entries.clear(); state = State.CLOSED; }
    @Override public String toString() { return "AttachmentPublicSessionCookies[requestValues=REDACTED]"; }
}
