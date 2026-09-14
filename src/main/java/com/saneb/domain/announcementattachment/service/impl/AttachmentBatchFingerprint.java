package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** 미리보기와 적용이 같은 metadata 직렬화/지문 계약을 사용한다. */
final class AttachmentBatchFingerprint {
    private final ObjectMapper mapper;
    AttachmentBatchFingerprint(ObjectMapper mapper) {this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);}
    String selectHash(Object value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(value)));}
        catch(Exception exception) {throw new IllegalStateException("배치 입력 지문을 계산할 수 없습니다.");}
    }
}
