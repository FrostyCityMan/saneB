package com.saneb.domain.announcementsource.provider.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;

/**
 * URL 검증 직후 확정한 공개 IP만 연결 계층에 제공하는 HTTP transport입니다.
 */
final class PinnedProviderContentHttpTransport implements ProviderContentHttpTransport {

    private static final int COPY_BUFFER_BYTES = 8192;

    private final Duration connectTimeout;

    PinnedProviderContentHttpTransport(Duration connectTimeout) {
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout is required");
    }

    @Override
    public ProviderContentHttpResponse selectResponse(
            ProviderContentRequestTarget requestTarget,
            Duration readTimeout,
            int maxResponseBytes,
            String userAgent
    ) throws IOException, TimeoutException {
        Objects.requireNonNull(requestTarget, "requestTarget is required");
        Objects.requireNonNull(readTimeout, "readTimeout is required");

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(connectTimeout))
                .setSocketTimeout(Timeout.of(readTimeout))
                .build();
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(new PinnedDnsResolver(requestTarget))
                        .setDefaultConnectionConfig(connectionConfig)
                        .setMaxConnTotal(1)
                        .setMaxConnPerRoute(1)
                        .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.of(readTimeout))
                .setConnectionRequestTimeout(Timeout.of(connectTimeout))
                .setRedirectsEnabled(false)
                .setContentCompressionEnabled(false)
                .build();
        HttpGet request = new HttpGet(requestTarget.uri());
        request.setConfig(requestConfig);
        request.setHeader("Accept", "text/html");
        request.setHeader("Accept-Encoding", "gzip");
        request.setHeader("User-Agent", userAgent);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .disableContentCompression()
                .disableCookieManagement()
                .build()) {
            return httpClient.execute(request, response -> new ProviderContentHttpResponse(
                    response.getCode(),
                    selectHeaders(response.getHeaders()),
                    selectLimitedBody(response.getEntity(), maxResponseBytes)
            ));
        } catch (SocketTimeoutException exception) {
            TimeoutException timeoutException = new TimeoutException("detail body request timed out");
            timeoutException.initCause(exception);
            throw timeoutException;
        }
    }

    private Map<String, List<String>> selectHeaders(Header[] responseHeaders) {
        Map<String, List<String>> valuesByName = new LinkedHashMap<>();
        for (Header header : responseHeaders) {
            String normalizedName = header.getName().toLowerCase(Locale.ROOT);
            valuesByName.computeIfAbsent(normalizedName, ignored -> new ArrayList<>())
                    .add(header.getValue());
        }
        Map<String, List<String>> immutableHeaders = new LinkedHashMap<>();
        valuesByName.forEach((name, values) -> immutableHeaders.put(name, List.copyOf(values)));
        return Map.copyOf(immutableHeaders);
    }

    private byte[] selectLimitedBody(HttpEntity entity, int maxResponseBytes) throws IOException {
        if (entity == null) {
            return new byte[0];
        }
        long contentLength = entity.getContentLength();
        if (contentLength > maxResponseBytes) {
            throw new ProviderContentResponseTooLargeException();
        }
        try (InputStream input = entity.getContent();
                ByteArrayOutputStream output = new ByteArrayOutputStream(selectInitialCapacity(
                        contentLength,
                        maxResponseBytes
                ))) {
            byte[] buffer = new byte[COPY_BUFFER_BYTES];
            int receivedBytes = 0;
            int readBytes;
            while ((readBytes = input.read(buffer)) != -1) {
                if ((long) receivedBytes + readBytes > maxResponseBytes) {
                    throw new ProviderContentResponseTooLargeException();
                }
                output.write(buffer, 0, readBytes);
                receivedBytes += readBytes;
            }
            return output.toByteArray();
        }
    }

    private int selectInitialCapacity(long contentLength, int maxResponseBytes) {
        if (contentLength <= 0) {
            return Math.min(COPY_BUFFER_BYTES, maxResponseBytes);
        }
        return (int) Math.min(contentLength, maxResponseBytes);
    }

    static final class PinnedDnsResolver implements DnsResolver {

        private final String allowedHost;
        private final InetAddress[] pinnedAddresses;

        PinnedDnsResolver(ProviderContentRequestTarget requestTarget) {
            allowedHost = requestTarget.allowedHost();
            pinnedAddresses = requestTarget.selectPinnedAddressArray();
        }

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            if (host == null || !allowedHost.equals(host.toLowerCase(Locale.ROOT))) {
                throw new UnknownHostException("host is outside the pinned request target");
            }
            return pinnedAddresses.clone();
        }

        @Override
        public String resolveCanonicalHostname(String host) throws UnknownHostException {
            resolve(host);
            return allowedHost;
        }
    }
}
