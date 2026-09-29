package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.util.List;
import java.util.Set;

/** 울산 북구의 실측 평문 GET FileDownNew 경로를 기존 새올 검증기에 연결한다. */
public final class UlsanBukguGetAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String OLD = "/emwp/jsp/ofr/FileDown.jsp";
    private static final String DOWNLOAD = "/emwp/jsp/ofr/FileDownNew.jsp";
    private final SaeolGetAttachmentDiscoveryProfile delegate = new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_ULSAN_BUKGU_GET_V1", "LGS-000081", "eminwon.bukgu.ulsan.kr", "SAFE_SAEOL_EMINWON", "th", false);
    private final String hash = AttachmentProfileFingerprint.selectHash(delegate.selectProfileHash()
            + "|1|plain-get-filedownnew|same-request|no-script", getClass());
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public Result selectDescriptors(String id, String html) { return delegate.selectDescriptors(id, html); }
    @Override public boolean selectApprovedRequest(URI uri) {
        if (uri == null || OLD.equals(uri.getPath())) return false;
        // 경로만 매핑한다. 인코딩된 경로·다른 호스트·추가 인자는 원래 검증기에서 거부한다.
        return delegate.selectApprovedRequest(DOWNLOAD.equals(uri.getRawPath()) ? selectPath(uri, DOWNLOAD, OLD) : uri);
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        return initial != null && initial.equals(next) && selectApprovedRequest(next);
    }
    @Override public Result selectDescriptors(Source source, String html) {
        var result = delegate.selectDescriptors(source, html);
        var descriptors = result.descriptors().stream().map(d -> new Descriptor(selectPath(d.fetchUri(), OLD, DOWNLOAD),
                new AttachmentSetEvidence.Locator(selectProfileCode(), DOWNLOAD, d.locator().identifiers()),
                d.displayName(), d.expectedFormat(), d.documentRole(), d.downloadAllowed())).toList();
        return new Result(result.status(), result.complete(), descriptors, result.warnings());
    }
    private URI selectPath(URI uri, String from, String to) {
        String value = uri.toASCIIString(); int start = value.indexOf(from);
        return URI.create(value.substring(0, start) + to + value.substring(start + from.length()));
    }
}
