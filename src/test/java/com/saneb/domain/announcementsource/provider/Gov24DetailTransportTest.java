package com.saneb.domain.announcementsource.provider;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** loopback 합성 HTTP만 사용한다. 운영 URL/인증정보/외부 요청은 없다. */
class Gov24DetailTransportTest {
    private Gov24PublicServiceAnnouncementSourceProviderClient selectClient() {
        return new Gov24PublicServiceAnnouncementSourceProviderClient(new ObjectMapper(), "", "", 1000);
    }

    @Test void parsesJsonThroughBoundedTransport() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fixture", exchange -> {
            byte[] bytes = "{\"fixture\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            assertThat(selectClient().selectBoundedDetailJson(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/fixture"))
                    .path("fixture").booleanValue()).isTrue();
        } finally { server.stop(0); }
    }

    @Test void deadlineIncludesBodyAfterSuccessfulHeaders() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var executor = Executors.newSingleThreadExecutor();
        var release = new CountDownLatch(1);
        server.setExecutor(executor);
        server.createContext("/fixture", exchange -> {
            exchange.sendResponseHeaders(200, 100);
            try (var output = exchange.getResponseBody()) {
                output.write('{'); output.flush();
                try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            }
        });
        server.start();
        long started = System.nanoTime();
        try {
            assertThatThrownBy(() -> selectClient().selectBoundedDetailJson(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/fixture")))
                    .hasMessageContaining("전체 대기시간");
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(4500);
        } finally {
            release.countDown(); server.stop(0); executor.shutdownNow();
            assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void actualChunkedResponseAboveLimitFails() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fixture", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                byte[] chunk = new byte[8192];
                java.util.Arrays.fill(chunk, (byte) 'a');
                output.write("{\"value\":\"".getBytes(StandardCharsets.UTF_8));
                for (int i = 0; i < 257; i++) output.write(chunk);
                output.write("\"}".getBytes(StandardCharsets.UTF_8));
            } catch (java.io.IOException expectedCancellation) { /* 수신 상한 도달 후 연결 취소 허용 */ }
        });
        server.start();
        try {
            assertThatThrownBy(() -> selectClient().selectBoundedDetailJson(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/fixture")))
                    .hasMessageContaining("다운로드 상한 2MiB");
        } finally { server.stop(0); }
    }
}
