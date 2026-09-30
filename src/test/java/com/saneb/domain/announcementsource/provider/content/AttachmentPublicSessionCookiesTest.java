package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttachmentPublicSessionCookiesTest {
    private static final URI DETAIL = URI.create("https://public.example/page.do?notice=123");
    private static final URI FILE = URI.create("https://public.example/programs/download.do?file=456");
    private static final Set<String> NAMES = Set.of("JSESSIONID", "LENA-UID", "L-VISITOR");
    private static final String CANARY = "unit-test-only";

    private AttachmentPublicSessionCookies selectSession() {
        return new AttachmentPublicSessionCookies(DETAIL, FILE, NAMES);
    }

    @Test void onlyPublicAllowlistedCookiesArePassedOnceWithoutLoggingValues() throws Exception {
        try (var session = selectSession()) {
            session.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY + "; Path=/; Secure; HttpOnly",
                    "LENA-UID=unit-route; Path=/; SameSite=Lax", "unapproved=discarded; Path=/"));
            assertThat(session.toString()).doesNotContain(CANARY, "public.example", "unit-route");
            assertThat(session.selectDownloadCookieHeader(FILE)).isEqualTo("JSESSIONID=" + CANARY + "; LENA-UID=unit-route");
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE))
                    .isInstanceOf(IOException.class).hasMessage("ATTACHMENT_PUBLIC_SESSION_STATE_INVALID");
        }
    }

    @Test void sessionsNeverShareCookies() throws Exception {
        try (var first = selectSession(); var second = selectSession()) {
            first.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY));
            second.saveBootstrapCookies(DETAIL, List.of());
            assertThatThrownBy(() -> second.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_COOKIE_MISSING");
            assertThat(first.selectDownloadCookieHeader(FILE)).isEqualTo("JSESSIONID=" + CANARY);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://public.example/file", "https://other.example/file", "https://public.example:444/file",
            "https://name@public.example/file", "https://public.example/file#part", "https://public.example/a/../file",
            "https://public.example/%0dfile", "https://public.example/file?x=%0a"})
    void rejectsUnsafeOrDifferentOriginsBeforeAnySessionExists(String target) {
        assertThatThrownBy(() -> new AttachmentPublicSessionCookies(DETAIL, URI.create(target), NAMES))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("ATTACHMENT_PUBLIC_SESSION_INVALID");
    }

    @ParameterizedTest @ValueSource(strings = {"https://public.example/programs/download.do?file=999",
            "https://other.example/programs/download.do?file=456", "http://public.example/programs/download.do?file=456",
            "https://public.example/page.do?notice=123"})
    void cannotSendToAnotherFileOrRedirectAndClosesOnScopeViolation(String target) throws Exception {
        try (var session = selectSession()) {
            session.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY));
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(URI.create(target)))
                    .hasMessage("ATTACHMENT_PUBLIC_SESSION_TARGET_BLOCKED");
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE))
                    .hasMessage("ATTACHMENT_PUBLIC_SESSION_STATE_INVALID");
        }
    }

    @Test void rejectsCookiesFromAnotherDetailAndBeforeBootstrap() throws Exception {
        try (var session = selectSession()) {
            assertThatThrownBy(() -> session.saveBootstrapCookies(FILE, List.of("JSESSIONID=" + CANARY)))
                    .hasMessage("ATTACHMENT_PUBLIC_SESSION_HEADERS_INVALID");
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_STATE_INVALID");
        }
        try (var session = selectSession()) {
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_STATE_INVALID");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"Domain=example", "Domain=.example", "Domain=other.example"})
    void broaderDomainNeverReceivesOrSuppliesCookie(String attribute) throws Exception {
        try (var session = selectSession()) {
            assertThatThrownBy(() -> session.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY + "; " + attribute)))
                    .hasMessage("ATTACHMENT_PUBLIC_SESSION_DOMAIN_BLOCKED").hasNoCause();
        }
    }

    @Test void exactDomainAndMatchingPathAreAcceptedButPrefixIsNot() throws Exception {
        try (var session = selectSession()) {
            session.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY + "; Domain=.public.example; Path=/programs",
                    "LENA-UID=not-for-this-path; Path=/program"));
            assertThat(session.selectDownloadCookieHeader(FILE)).isEqualTo("JSESSIONID=" + CANARY);
        }
    }

    @Test void absentPathUsesDetailDirectoryRatherThanCookieRoot() throws Exception {
        URI nested = URI.create("https://public.example/board/detail");
        try (var session = new AttachmentPublicSessionCookies(nested, FILE, NAMES)) {
            session.saveBootstrapCookies(nested, List.of("JSESSIONID=" + CANARY));
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_COOKIE_MISSING");
        }
    }

    @Test void duplicateEvenExpiredNamesFailWithoutChoosingAnArbitraryCookie() throws Exception {
        try (var session = selectSession()) {
            assertThatThrownBy(() -> session.saveBootstrapCookies(DETAIL,
                    List.of("JSESSIONID=expired; Max-Age=0", "JSESSIONID=" + CANARY)))
                    .hasMessage("ATTACHMENT_PUBLIC_SESSION_COOKIE_CONFLICT");
        }
    }

    @Test void expiresAtThirtySecondsIncludingBootstrapTimeWithoutSleeping() throws Exception {
        var now = new AtomicLong();
        try (var session = new AttachmentPublicSessionCookies(DETAIL, FILE, NAMES, now::get)) {
            now.set(Duration.ofSeconds(29).toNanos());
            session.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY + "; Max-Age=600"));
            now.set(Duration.ofSeconds(30).toNanos());
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_EXPIRED");
        }
    }

    @Test void shorterCookieExpiryAndExplicitDeletionAreHonoured() throws Exception {
        var now = new AtomicLong();
        try (var session = new AttachmentPublicSessionCookies(DETAIL, FILE, NAMES, now::get)) {
            session.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY + "; Max-Age=1", "LENA-UID=gone; Max-Age=0"));
            now.set(Duration.ofSeconds(1).toNanos());
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_COOKIE_MISSING");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"JSESSIONID=invalid\r\nInjected: value", "JSESSIONID=bad\\value",
            "JSESSIONID=한글", "JSESSIONID=value; Path=relative", "JSESSIONID=value; Path=/../"})
    void untrustedHeaderFailuresNeverExposeRawValues(String header) throws Exception {
        try (var session = selectSession()) {
            assertThatThrownBy(() -> session.saveBootstrapCookies(DETAIL, List.of(header)))
                    .isInstanceOf(IOException.class).hasMessageStartingWith("ATTACHMENT_PUBLIC_SESSION_")
                    .hasMessageNotContaining(header).hasNoCause();
        }
    }

    @Test void headerCountSizeValueAndFinalHeaderAreBounded() throws Exception {
        for (var headers : List.of(java.util.Collections.nCopies(17, "ignored=v"),
                List.of("ignored=" + "x".repeat(4096)), List.of("JSESSIONID=" + "x".repeat(2049)),
                java.util.Collections.nCopies(5, "ignored=" + "x".repeat(3500)))) {
            try (var session = selectSession()) {
                assertThatThrownBy(() -> session.saveBootstrapCookies(DETAIL, headers)).isInstanceOf(IOException.class);
            }
        }
        try (var session = new AttachmentPublicSessionCookies(DETAIL, FILE, Set.of("A", "B", "C", "D"))) {
            session.saveBootstrapCookies(DETAIL, List.of("A=" + "x".repeat(2048), "B=" + "x".repeat(2048),
                    "C=" + "x".repeat(2048), "D=" + "x".repeat(2048)));
            assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_HEADER_LIMIT");
        }
    }

    @Test void closeAndSecondBootstrapAreTerminal() throws Exception {
        var session = selectSession();
        session.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY));
        session.close();
        assertThatThrownBy(() -> session.selectDownloadCookieHeader(FILE)).hasMessage("ATTACHMENT_PUBLIC_SESSION_STATE_INVALID");
        try (var second = selectSession()) {
            second.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=" + CANARY));
            assertThatThrownBy(() -> second.saveBootstrapCookies(DETAIL, List.of("JSESSIONID=other")))
                    .hasMessage("ATTACHMENT_PUBLIC_SESSION_STATE_INVALID");
        }
    }
}
