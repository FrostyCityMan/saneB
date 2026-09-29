package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 실측한 기관에만 기존 UTF-8 헤더 복원을 연결한다. URL·발견·MIME 계약은 위임한다. */
final class Utf8DispositionAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final AttachmentDiscoveryProfile delegate;
    private final String hash;

    Utf8DispositionAttachmentDiscoveryProfile(AttachmentDiscoveryProfile delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        hash = AttachmentProfileFingerprint.selectHash(
                "UTF8_DISPOSITION_OCTETS:1|" + delegate.selectProfileHash(), getClass());
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
