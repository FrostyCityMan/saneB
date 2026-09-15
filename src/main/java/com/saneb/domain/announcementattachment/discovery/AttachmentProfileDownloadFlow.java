package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 내부 flow도 최대 4개 고정 요청·원래 파일 한도·같은 임시 파일을 벗어날 수 없다. */
public final class AttachmentProfileDownloadFlow {
    private AttachmentProfileDownloadFlow() { }
    @FunctionalInterface public interface Transport {
        AttachmentPinnedDownloadClient.Download selectDownload(AttachmentPinnedDownloadClient.Request request,long maximumBytes,
                java.util.function.Predicate<AttachmentPinnedDownloadClient.Request> approvedRequest) throws IOException;
    }
    public static AttachmentPinnedDownloadClient.Download selectDownload(AttachmentDiscoveryProfile profile,
            AttachmentPinnedDownloadClient.Request initial, Path output, long maximumBytes, Transport transport) throws IOException {
        if(Files.exists(output,java.nio.file.LinkOption.NOFOLLOW_LINKS)) throw new IOException("ATTACHMENT_OUTPUT_EXISTS");
        int[] count={0};
        int[] httpCount={0};
        java.util.function.Predicate<AttachmentPinnedDownloadClient.Request> approved=request->
                ++httpCount[0]<=4 && profile.selectApprovedRequest(initial,request);
        AttachmentDownloadFlowProfile.Operation bounded=(request,limit)->{
            if(++count[0]>4 || limit<1 || limit>maximumBytes || !profile.selectApprovedRequest(initial,request)) throw new IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
            return transport.selectDownload(request,limit,approved);
        };
        try {
            return profile instanceof AttachmentDownloadFlowProfile flow ? flow.selectDownload(initial,output,maximumBytes,bounded)
                    : bounded.selectDownload(initial,maximumBytes);
        } catch(IOException|RuntimeException failure) {
            Files.deleteIfExists(output);throw failure;
        }
    }
}
