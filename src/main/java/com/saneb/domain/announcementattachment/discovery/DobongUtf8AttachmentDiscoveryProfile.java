package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 도봉의 실측 UTF-8 파일명 헤더만 복원하며 다운로드 사전 확인 절차를 보존한다. */
final class DobongUtf8AttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private final AttachmentDownloadFlowProfile delegate;
    private final String hash;

    DobongUtf8AttachmentDiscoveryProfile(AttachmentDownloadFlowProfile delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        if (!"LOCAL_DOBONG_BOARD_V1".equals(delegate.selectProfileCode())) throw new IllegalArgumentException("DOBONG_PROFILE_REQUIRED");
        hash = AttachmentProfileFingerprint.selectHash(
                "DOBONG_UTF8_DISPOSITION_FLOW:1|" + delegate.selectProfileHash(), getClass());
    }

    @Override public com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download selectDownload(
            Request initial, java.nio.file.Path output, long maximumBytes, Operation operation) throws java.io.IOException {
        return delegate.selectDownload(initial,output,maximumBytes,operation);
    }
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public boolean selectUtf8DispositionOctets() { return true; }
    @Override public Set<String> selectLegacyBinaryContentTypes() { return delegate.selectLegacyBinaryContentTypes(); }
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
