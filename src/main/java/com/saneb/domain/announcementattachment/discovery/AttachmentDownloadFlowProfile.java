package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.nio.file.Path;

/** 시스템 코드에 고정된 비재귀 다운로드 절차. 각 호출은 공통 transport 경계를 다시 통과한다. */
public interface AttachmentDownloadFlowProfile extends AttachmentDiscoveryProfile {
    AttachmentPinnedDownloadClient.Download selectDownload(AttachmentPinnedDownloadClient.Request initial, Path output,
            long maximumBytes, Operation operation) throws IOException;
    @FunctionalInterface interface Operation {
        AttachmentPinnedDownloadClient.Download selectDownload(AttachmentPinnedDownloadClient.Request request, long maximumBytes) throws IOException;
    }
}
