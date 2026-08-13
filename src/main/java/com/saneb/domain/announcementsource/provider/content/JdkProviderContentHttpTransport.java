package com.saneb.domain.announcementsource.provider.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * redirect를 자동으로 따르지 않고 응답 본문을 streaming 크기 제한으로 읽는 JDK transport입니다.
 */
final class JdkProviderContentHttpTransport implements ProviderContentHttpTransport {

    private final HttpClient httpClient;

    JdkProviderContentHttpTransport(Duration connectTimeout) {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public ProviderContentHttpResponse selectResponse(
            URI uri,
            Duration readTimeout,
            int maxResponseBytes,
            String userAgent
    ) throws IOException, InterruptedException, TimeoutException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(readTimeout)
                .header("Accept", "text/html")
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", userAgent)
                .GET()
                .build();
        CompletableFuture<HttpResponse<byte[]>> future = httpClient.sendAsync(
                request,
                responseInfo -> new LimitedBodySubscriber(maxResponseBytes)
        );
        try {
            HttpResponse<byte[]> response = future.get(readTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return new ProviderContentHttpResponse(
                    response.statusCode(),
                    response.headers().map(),
                    response.body()
            );
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw exception;
        } catch (ExecutionException exception) {
            Throwable cause = selectRootCause(exception.getCause());
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("detail body request failed", cause);
        }
    }

    private Throwable selectRootCause(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {

        private final int maxResponseBytes;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;
        private int receivedBytes;

        private LimitedBodySubscriber(int maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) {
                return;
            }
            for (ByteBuffer buffer : buffers) {
                int length = buffer.remaining();
                if ((long) receivedBytes + length > maxResponseBytes) {
                    subscription.cancel();
                    body.completeExceptionally(new ProviderContentResponseTooLargeException());
                    return;
                }
                byte[] bytes = new byte[length];
                buffer.get(bytes);
                output.writeBytes(bytes);
                receivedBytes += length;
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            body.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            body.complete(output.toByteArray());
        }
    }
}
