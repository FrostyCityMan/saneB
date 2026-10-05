package com.saneb.domain.announcementsource.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** 수신 도중 상한을 검사한다. 초과 응답을 모두 메모리에 읽은 뒤 자르지 않는다. */
final class BoundedJsonBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final int maximumBytes;
    private final ByteArrayOutputStream buffer;
    private final CompletableFuture<byte[]> result = new CompletableFuture<>();
    private Flow.Subscription subscription;

    BoundedJsonBodySubscriber(int maximumBytes) {
        if (maximumBytes < 1) throw new IllegalArgumentException("응답 바이트 상한은 1 이상이어야 합니다.");
        this.maximumBytes = maximumBytes;
        this.buffer = new ByteArrayOutputStream(Math.min(maximumBytes, 8192));
    }

    @Override public CompletionStage<byte[]> getBody() { return result; }

    @Override public void onSubscribe(Flow.Subscription next) {
        if (subscription != null || result.isDone()) { next.cancel(); return; }
        subscription = next;
        next.request(1);
    }

    @Override public void onNext(List<ByteBuffer> chunks) {
        if (result.isDone()) return;
        long incoming = chunks.stream().mapToLong(ByteBuffer::remaining).sum();
        if (incoming > maximumBytes - buffer.size()) {
            subscription.cancel();
            buffer.reset();
            result.completeExceptionally(new IOException("상세 응답의 다운로드 바이트 상한을 초과했습니다."));
            return;
        }
        byte[] scratch = new byte[Math.min(maximumBytes, 8192)];
        for (ByteBuffer chunk : chunks) {
            while (chunk.hasRemaining()) {
                int length = Math.min(chunk.remaining(), scratch.length);
                chunk.get(scratch, 0, length);
                buffer.write(scratch, 0, length);
            }
        }
        subscription.request(1);
    }

    @Override public void onError(Throwable ignored) {
        buffer.reset();
        result.completeExceptionally(new IOException("상세 응답 수신에 실패했습니다."));
    }

    @Override public void onComplete() {
        if (result.isDone()) return;
        result.complete(buffer.toByteArray());
        buffer.reset();
    }
}
