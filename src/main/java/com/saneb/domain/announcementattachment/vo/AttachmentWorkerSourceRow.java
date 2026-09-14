package com.saneb.domain.announcementattachment.vo;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;

/** 내부 worker 조회용. URL은 저장된 원문에서만 읽으며 API 입력이나 로그로 전달하지 않는다. */
public record AttachmentWorkerSourceRow(String providerCode, String providerNoticeId, String sourceUrl,
                                        String localSourceCode, String listParserProfileCode) {
    public AttachmentDiscoveryProfile.Source selectDiscoverySource() {
        return new AttachmentDiscoveryProfile.Source(providerCode, providerNoticeId, sourceUrl, localSourceCode, listParserProfileCode);
    }
    @Override public String toString() { return "AttachmentWorkerSource[requestValues=REDACTED]"; }
}
