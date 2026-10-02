package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.dto.AttachmentLinkedBatchRequests;
import jakarta.validation.Validation;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttachmentLinkedBatchRequestsTest {
    private AttachmentLinkedBatchRequests.Scope scope(List<UUID> ids,Long bytes) {
        return new AttachmentLinkedBatchRequests.Scope(UUID.randomUUID(),ids,bytes);
    }
    private boolean valid(Object request) {
        try(var factory=Validation.buildDefaultValidatorFactory()) {return factory.getValidator().validate(request).isEmpty();}
    }
    @Test void exactScopeIsBoundedUniqueAndImmutable() {
        var ids=new ArrayList<>(List.of(UUID.randomUUID()));var request=scope(ids,83886080L);
        ids.clear();assertThat(request.sourceIds()).hasSize(1);
        assertThatThrownBy(()->request.sourceIds().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(valid(request)).isTrue();
        UUID duplicate=UUID.randomUUID();
        assertThat(valid(scope(List.of(duplicate,duplicate),1L))).isFalse();
        assertThat(valid(scope(List.of(),1L))).isFalse();
        assertThat(valid(scope(Arrays.asList((UUID)null),1L))).isFalse();
        assertThat(valid(scope(List.of(duplicate),0L))).isFalse();
        assertThat(valid(scope(List.of(duplicate),83886081L))).isFalse();
        assertThat(valid(scope(java.util.stream.IntStream.range(0,1001).mapToObj(i->UUID.randomUUID()).toList(),1L))).isFalse();
    }
    @Test void reservationRequiresAcknowledgmentHashAndReason() {
        var scope=scope(List.of(UUID.randomUUID()),1L);
        assertThat(valid(new AttachmentLinkedBatchRequests.Reservation(scope,"a".repeat(64),true,"연결 근거 확인"))).isTrue();
        assertThat(valid(new AttachmentLinkedBatchRequests.Reservation(scope,"a".repeat(64),false,"확인"))).isFalse();
        assertThat(valid(new AttachmentLinkedBatchRequests.Reservation(scope,"a".repeat(64),null,"확인"))).isFalse();
        assertThat(valid(new AttachmentLinkedBatchRequests.Reservation(scope,"A".repeat(64),true,"확인"))).isFalse();
        assertThat(valid(new AttachmentLinkedBatchRequests.Reservation(scope,"a".repeat(64),true," "))).isFalse();
    }
    @Test void jsonRejectsExecutionOverridesAndDoesNotLogReason() throws Exception {
        var mapper=new ObjectMapper();var scope=scope(List.of(UUID.randomUUID()),1L);
        String json=mapper.writeValueAsString(scope);
        assertThat(mapper.readValue(json,AttachmentLinkedBatchRequests.Scope.class)).isEqualTo(scope);
        assertThatThrownBy(()->mapper.readValue(json.substring(0,json.length()-1)+",\"includeLinked\":true}",AttachmentLinkedBatchRequests.Scope.class))
                .hasMessageContaining("정책 ID");
        assertThat(new AttachmentLinkedBatchRequests.Reservation(scope,"a".repeat(64),true,"별도 사유").toString()).doesNotContain("별도 사유");
        assertThat(scope.toString()).doesNotContain(scope.sourceIds().getFirst().toString());
    }
}
