package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** 실측한 구형 파일 응답만 보정하며 기관별 URL·상세 한도·발견 계약을 보존한다. */
final class LegacyFileResponseAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile, AttachmentDetailLimitProfile {
    private static final String LEGACY_MIME = "application/x-msdownload";
    private final AttachmentDiscoveryProfile delegate;
    private final boolean bareHwpx;
    private final String hash;

    LegacyFileResponseAttachmentDiscoveryProfile(AttachmentDiscoveryProfile delegate, String mode) {
        this.delegate = Objects.requireNonNull(delegate);
        if (delegate instanceof AttachmentDownloadFlowProfile || mode == null
                || !Set.of("LEGACY_BINARY_UTF8", "BARE_HWPX").contains(mode))
            throw new IllegalArgumentException("MEASURED_FILE_RESPONSE_MODE_REQUIRED");
        bareHwpx = "BARE_HWPX".equals(mode);
        hash = AttachmentProfileFingerprint.selectHash(
                "LEGACY_FILE_RESPONSE:1|" + delegate.selectProfileHash() + "|" + mode
                        + "|one-request|preserve-detail-limit:" + selectDetailMaximumBytes()
                        + "|bare-hwpx-signature-and-attachment-name-required|"
                        + AttachmentProfileFingerprint.selectHash("LIMIT:1", AttachmentDetailLimitProfile.class), getClass());
    }

    @Override public Download selectDownload(Request initial, Path output, long maximumBytes, Operation operation) throws IOException {
        Download response = operation.selectDownload(initial, maximumBytes);
        if (!bareHwpx) return response;
        String mime = response.contentType() == null ? "" : response.contentType().split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!"hwpx".equals(mime)) {
            if (LEGACY_MIME.equals(mime) && !delegate.selectLegacyBinaryContentTypes().contains(mime))
                throw new IOException("ATTACHMENT_CONTENT_TYPE_MISMATCH");
            return response;
        }
        // 내부 legacy 검증 표현이다. 서버 원래 MIME을 x-msdownload라고 주장하지 않는다.
        Download normalized = new Download(response.bytes(), response.sha256(), LEGACY_MIME, response.contentDisposition());
        new AttachmentFileTypeValidator().selectFormat(output, normalized, "HWPX",
                delegate.selectUtf8DispositionOctets(), Set.of(LEGACY_MIME));
        return normalized;
    }

    @Override public long selectDetailMaximumBytes() { return AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(delegate); }
    @Override public boolean selectUtf8DispositionOctets() { return !bareHwpx || delegate.selectUtf8DispositionOctets(); }
    @Override public Set<String> selectLegacyBinaryContentTypes() {
        var types = new java.util.HashSet<>(delegate.selectLegacyBinaryContentTypes());
        types.add(LEGACY_MIME);
        return Set.copyOf(types);
    }
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public boolean selectApprovedRequest(URI uri) { return delegate.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(Request request) { return delegate.selectApprovedRequest(request); }
    @Override public boolean selectApprovedRequest(Request initial, Request request) { return delegate.selectApprovedRequest(initial, request); }
    @Override public Result selectDescriptors(String id, String html) { return delegate.selectDescriptors(id, html); }
    @Override public Result selectDescriptors(Source source, String html) { return delegate.selectDescriptors(source, html); }
}
