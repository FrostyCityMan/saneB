package com.saneb.domain.announcementsource.provider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

class BoundedJsonBodySubscriberTest {
    @Test void exactLimitAcrossChunksCompletes() {
        var subscriber = new BoundedJsonBodySubscriber(4);
        var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2})));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{3, 4}).asReadOnlyBuffer()));
        subscriber.onComplete();
        assertThat(subscriber.getBody().toCompletableFuture().join()).containsExactly(1, 2, 3, 4);
        verify(subscription, never()).cancel();
    }

    @Test void overflowCancelsBeforeReadingOversizedChunkAndCannotBecomeSuccess() {
        var subscriber = new BoundedJsonBodySubscriber(3);
        var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2})));
        var oversized = ByteBuffer.wrap(new byte[]{3, 4});
        subscriber.onNext(List.of(oversized));
        subscriber.onComplete();
        verify(subscription).cancel();
        assertThat(oversized.position()).isZero();
        assertThatThrownBy(() -> subscriber.getBody().toCompletableFuture().join())
                .hasRootCauseMessage("상세 응답의 다운로드 바이트 상한을 초과했습니다.");
    }

    @Test void failureDoesNotLeakTransportMessage() {
        var subscriber = new BoundedJsonBodySubscriber(4);
        subscriber.onError(new RuntimeException("DO-NOT-LOG"));
        assertThatThrownBy(() -> subscriber.getBody().toCompletableFuture().join())
                .hasRootCauseMessage("상세 응답 수신에 실패했습니다.");
    }

    @Test void duplicateSubscriptionIsCancelled() {
        var subscriber = new BoundedJsonBodySubscriber(4);
        var first = mock(Flow.Subscription.class);
        var duplicate = mock(Flow.Subscription.class);
        subscriber.onSubscribe(first);
        subscriber.onSubscribe(duplicate);
        verify(duplicate).cancel();
        verify(first).request(1);
    }
}
