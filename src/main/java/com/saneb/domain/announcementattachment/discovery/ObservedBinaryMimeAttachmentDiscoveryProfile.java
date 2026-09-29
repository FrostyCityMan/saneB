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

/** 실측 기관의 generic/Office MIME만 검증 후 공통 legacy-binary 표현으로 정규화한다. */
final class ObservedBinaryMimeAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String CANONICAL = "application/x-msdownload";
    private final AttachmentDiscoveryProfile delegate;
    private final String observedMime;
    private final String hash;

    ObservedBinaryMimeAttachmentDiscoveryProfile(AttachmentDiscoveryProfile delegate, String observedMime) {
        this.delegate = Objects.requireNonNull(delegate);
        if (delegate instanceof AttachmentDownloadFlowProfile || observedMime == null
                || !Set.of("application/unknown", "application/x-tika-msoffice").contains(observedMime)) {
            throw new IllegalArgumentException("OBSERVED_BINARY_MIME_PROFILE_REQUIRED");
        }
        this.observedMime = observedMime;
        hash = AttachmentProfileFingerprint.selectHash(
                "OBSERVED_BINARY_MIME:1|" + delegate.selectProfileHash() + "|" + observedMime
                        + "|signature-attachment-filename-before-normalization|office-hwp-only|one-request", getClass());
    }

    @Override public Download selectDownload(Request initial, Path output, long maximumBytes, Operation operation) throws IOException {
        Download original = operation.selectDownload(initial, maximumBytes);
        String mime = original.contentType() == null ? "" : original.contentType().split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!observedMime.equals(mime)) {
            if (CANONICAL.equals(mime) && !delegate.selectLegacyBinaryContentTypes().contains(CANONICAL))
                throw new IOException("ATTACHMENT_CONTENT_TYPE_MISMATCH");
            return original;
        }
        var normalized = new Download(original.bytes(), original.sha256(), CANONICAL, original.contentDisposition());
        var types = new AttachmentFileTypeValidator();
        // unknown은 generic binary 의미다. 실제 서명으로 지원 형식을 판별한 뒤 legacy의 강화된 파일명 검증을 적용한다.
        String format = types.selectFormat(output,
                new Download(original.bytes(), original.sha256(), "application/octet-stream", original.contentDisposition()),
                null, delegate.selectUtf8DispositionOctets());
        if ("application/x-tika-msoffice".equals(observedMime) && !"HWP".equals(format))
            throw new IOException("ATTACHMENT_FORMAT_MISMATCH");
        // Office MIME은 OLE/HWP만 정규화한다. 호출자는 이후 descriptor의 예상 형식과 다시 대조한다.
        types.selectFormat(output, normalized, format,
                delegate.selectUtf8DispositionOctets(), Set.of(CANONICAL));
        return normalized;
    }
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public boolean selectUtf8DispositionOctets() { return delegate.selectUtf8DispositionOctets(); }
    @Override public Set<String> selectLegacyBinaryContentTypes() {
        var types = new java.util.HashSet<>(delegate.selectLegacyBinaryContentTypes());
        types.add(CANONICAL);
        return Set.copyOf(types);
    }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public boolean selectApprovedRequest(URI uri) { return delegate.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(Request request) { return delegate.selectApprovedRequest(request); }
    @Override public boolean selectApprovedRequest(Request initial, Request request) {
        return delegate.selectApprovedRequest(initial, request);
    }
    @Override public Result selectDescriptors(String id, String html) { return delegate.selectDescriptors(id, html); }
    @Override public Result selectDescriptors(Source source, String html) { return delegate.selectDescriptors(source, html); }
}
