package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PinnedProviderContentHttpTransportTest {

    @Test
    void selectResponseConnectsToPinnedAddressWithoutSystemDnsResolution() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<String> hostHeader = executor.submit(() -> selectRequestAndRespond(serverSocket));
                URI uri = URI.create("http://unresolvable.invalid:"
                        + serverSocket.getLocalPort()
                        + "/notices/42");
                ProviderContentRequestTarget target = new ProviderContentRequestTarget(
                        uri,
                        "unresolvable.invalid",
                        List.of(InetAddress.getLoopbackAddress())
                );
                PinnedProviderContentHttpTransport transport =
                        new PinnedProviderContentHttpTransport(Duration.ofSeconds(2));

                ProviderContentHttpResponse response = transport.selectResponse(
                        target,
                        Duration.ofSeconds(2),
                        1024,
                        "saneB-test-client/1.0"
                );

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("본문 수집 성공");
                assertThat(hostHeader.get(2, TimeUnit.SECONDS))
                        .isEqualTo("unresolvable.invalid:" + serverSocket.getLocalPort());
            } finally {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
            }
        }
    }

    @Test
    void pinnedDnsResolverReturnsOnlyPrevalidatedAddressesForAllowedHost() throws Exception {
        InetAddress first = address(203, 0, 113, 10);
        InetAddress second = address(203, 0, 113, 11);
        ProviderContentRequestTarget target = new ProviderContentRequestTarget(
                URI.create("https://city.example.go.kr/notices/42"),
                "city.example.go.kr",
                List.of(first, second)
        );
        PinnedProviderContentHttpTransport.PinnedDnsResolver resolver =
                new PinnedProviderContentHttpTransport.PinnedDnsResolver(target);

        InetAddress[] firstResolution = resolver.resolve("CITY.EXAMPLE.GO.KR");
        firstResolution[0] = address(10, 0, 0, 1);

        assertThat(resolver.resolve("city.example.go.kr")).containsExactly(first, second);
        assertThat(resolver.resolveCanonicalHostname("city.example.go.kr"))
                .isEqualTo("city.example.go.kr");
    }

    @Test
    void pinnedDnsResolverRejectsEveryOtherHost() {
        ProviderContentRequestTarget target = new ProviderContentRequestTarget(
                URI.create("https://city.example.go.kr/notices/42"),
                "city.example.go.kr",
                List.of(address(203, 0, 113, 10))
        );
        PinnedProviderContentHttpTransport.PinnedDnsResolver resolver =
                new PinnedProviderContentHttpTransport.PinnedDnsResolver(target);

        assertThatThrownBy(() -> resolver.resolve("outside.example"))
                .isInstanceOf(UnknownHostException.class);
    }

    private String selectRequestAndRespond(ServerSocket serverSocket) throws IOException {
        try (Socket socket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.US_ASCII
                ));
                OutputStream output = socket.getOutputStream()) {
            String hostHeader = null;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.regionMatches(true, 0, "Host:", 0, 5)) {
                    hostHeader = line.substring(5).trim();
                }
            }
            byte[] body = "본문 수집 성공".getBytes(StandardCharsets.UTF_8);
            String headers = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Content-Length: " + body.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            output.write(headers.getBytes(StandardCharsets.US_ASCII));
            output.write(body);
            output.flush();
            return hostHeader;
        }
    }

    private static InetAddress address(int first, int second, int third, int fourth) {
        try {
            return InetAddress.getByAddress(new byte[]{
                    (byte) first,
                    (byte) second,
                    (byte) third,
                    (byte) fourth
            });
        } catch (UnknownHostException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
