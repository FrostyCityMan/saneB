package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

/** 내부 worker용 고정 범위. 다운로드 URL/원본 텍스트를 포함하지 않는다. */
public record AttachmentRetryFileRow(UUID fileId,String locatorJson,String locatorHash,String displayName,
        String role,String roleOrigin,String detectedType,Boolean selected) {
    @Override public String toString() { return "AttachmentRetryFileRow[fileId="+fileId+", selected="+selected+"]"; }
}
