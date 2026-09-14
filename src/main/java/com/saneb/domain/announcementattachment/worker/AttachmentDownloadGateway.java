package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentJobService;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentResourceLease;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** 요청/redirect마다 DB 실행 허용과 목적지 host 임대를 먼저 확보한다. */
@Component
public final class AttachmentDownloadGateway {
    private final AnnouncementAttachmentJobService jobs;
    private final AttachmentPinnedDownloadClient client;
    public AttachmentDownloadGateway(AnnouncementAttachmentJobService jobs, AttachmentPinnedDownloadClient client) {
        this.jobs = jobs; this.client = client;
    }
    public static final class Deferred extends RuntimeException {
        public Deferred() { super("ATTACHMENT_EXECUTION_DEFERRED", null, false, false); }
    }
    public AttachmentPinnedDownloadClient.Download selectDownload(AttachmentJobRow job, AttachmentDiscoveryProfile profile,
            AttachmentPinnedDownloadClient.Request request, Path output, long maximumBytes) throws IOException {
        return selectDownload(job,profile,request,output,maximumBytes,() -> { });
    }
    public AttachmentPinnedDownloadClient.Download selectDownload(AttachmentJobRow job, AttachmentDiscoveryProfile profile,
            AttachmentPinnedDownloadClient.Request request, Path output, long maximumBytes, Runnable requestStarted) throws IOException {
        return com.saneb.domain.announcementattachment.discovery.AttachmentProfileDownloadFlow.selectDownload(profile,request,output,maximumBytes,
                (next,limit,approved)->selectSingleDownload(job,profile,next,output,limit,requestStarted,approved));
    }
    private AttachmentPinnedDownloadClient.Download selectSingleDownload(AttachmentJobRow job, AttachmentDiscoveryProfile profile,
            AttachmentPinnedDownloadClient.Request request, Path output, long maximumBytes, Runnable requestStarted,
            java.util.function.Predicate<AttachmentPinnedDownloadClient.Request> approved) throws IOException {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("첨부 외부 요청은 DB transaction 밖에서만 실행할 수 있습니다.");
        AttachmentResourceLease[] lease = {null};
        String[] leasedHost = {null};
        try {
            return client.selectDownload(request, profile.selectApprovedHosts(), selected -> {
                if (!approved.test(selected)) return false;
                if (!jobs.saveJobHeartbeat(job.jobId(), job.leaseToken())
                        || !jobs.selectExternalExecutionAllowed(job.jobId(), job.leaseToken())) throw new Deferred();
                String host = selected.uri().getHost().toLowerCase(Locale.ROOT);
                if (!host.equals(leasedHost[0])) {
                    if (lease[0] != null) { jobs.deleteResourceLease(lease[0]); lease[0] = null; }
                    lease[0] = jobs.saveDownloadLease(job.jobId(), job.leaseToken(), selectHash(host)).orElseThrow(Deferred::new);
                    leasedHost[0] = host;
                }
                requestStarted.run();
                return true;
            }, output, maximumBytes, bytes -> {
                if (!jobs.selectExternalExecutionAllowed(job.jobId(), job.leaseToken())) throw new Deferred();
                return jobs.saveDownloadBytes(job.jobId(), job.leaseToken(), bytes);
            });
        } finally { if (lease[0] != null) jobs.deleteResourceLease(lease[0]); }
    }
    private static String selectHash(String host) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(host.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
}
