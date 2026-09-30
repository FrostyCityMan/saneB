package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 영덕 공개 첨부 endpoint → 공식 임시 파일 redirect. 상세 Referer는 같은 공고 ID로만 생성한다. */
final class YeongdeokRefererAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private final AttachmentDiscoveryProfile delegate = new GyeongbukDirectAttachmentDiscoveryProfile(
            GyeongbukDirectAttachmentDiscoveryProfile.Site.YEONGDEOK);
    private final String hash = AttachmentProfileFingerprint.selectHash(
            "YEONGDEOK_REFERER:2|strict-utf8-location-octets|same-notice-public-detail|same-host-kboard-temp-13hex|no-query|no-cookie|unknown-role|"
                    + delegate.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("REQUEST:1", Request.class)
                    + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());

    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public Result selectDescriptors(String id, String html) { return delegate.selectDescriptors(id, html); }
    @Override public Result selectDescriptors(Source source, String html) { return delegate.selectDescriptors(source, html); }

    private boolean selectEndpoint(URI uri) {
        try { return delegate.selectApprovedRequest(uri) && "kboard_file_download".equals(
                CapitalThirdNoticePage.selectParameters(uri.getRawQuery()).get("action")); }
        catch (IllegalArgumentException exception) { return false; }
    }
    private URI selectReferer(URI uri) {
        return URI.create("https://www.yd.go.kr/?page_id=763&uid="
                + CapitalThirdNoticePage.selectParameters(uri.getRawQuery()).get("uid") + "&mod=document");
    }
    private boolean selectPublicReferer(URI uri) {
        try {
            if (!delegate.selectApprovedRequest(uri)) return false;
            var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return q.keySet().equals(Set.of("page_id", "uid", "mod")) && "763".equals(q.get("page_id"))
                    && "document".equals(q.get("mod")) && uri.equals(selectReferer(uri));
        } catch (IllegalArgumentException exception) { return false; }
    }
    private boolean selectTemporaryFile(URI uri) {
        if (uri == null || !"https".equals(uri.getScheme()) || !"www.yd.go.kr".equals(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null
                || uri.getFragment() != null || uri.getRawQuery() != null || !uri.equals(uri.normalize())) return false;
        String path = uri.getPath();
        return path != null && path.length() <= 700 && !path.contains("..") && !path.contains("%")
                && !path.contains("\\") && path.codePoints().noneMatch(Character::isISOControl)
                && path.matches("/wp-content/uploads/kboard_temp/[a-f0-9]{13}/[^/]+\\.(?i:hwp|hwpx|pdf)");
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        return delegate.selectApprovedRequest(uri) || selectTemporaryFile(uri);
    }
    @Override public boolean selectApprovedRequest(Request request) {
        if (request == null || !"GET".equals(request.method())) return false;
        if (request.referer() == null) return delegate.selectApprovedRequest(request);
        return request.utf8RedirectOctets() && selectPublicReferer(request.referer()) && (selectTemporaryFile(request.uri())
                || selectEndpoint(request.uri()) && request.referer().equals(selectReferer(request.uri())));
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        if (initial == null || initial.referer() != null || !delegate.selectApprovedRequest(initial)
                || next == null || !selectApprovedRequest(next)) return false;
        if (initial.equals(next)) return true;
        if (!selectEndpoint(initial.uri())) return initial.equals(next);
        return selectReferer(initial.uri()).equals(next.referer())
                && (initial.uri().equals(next.uri()) || selectTemporaryFile(next.uri()));
    }
    @Override public AttachmentPinnedDownloadClient.Download selectDownload(
            Request initial, Path output, long maximumBytes, Operation operation) throws IOException {
        if (initial == null || initial.referer() != null || !delegate.selectApprovedRequest(initial))
            throw new IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        Request selected = selectEndpoint(initial.uri())
                ? new Request(initial.uri(), "GET", Map.of(), selectReferer(initial.uri()), true) : initial;
        return operation.selectDownload(selected, maximumBytes);
    }
}
