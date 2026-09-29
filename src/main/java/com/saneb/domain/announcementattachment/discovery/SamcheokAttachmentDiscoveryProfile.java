package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SamcheokNoticePage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 삼척의 mgtNo를 기존 SCMS 파서에 내부 전달한다. 내부 파싱용 URI는 외부 요청에 허용하지 않는다. */
@Component
public final class SamcheokAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final ScmsSaeolAttachmentDiscoveryProfile delegate = new ScmsSaeolAttachmentDiscoveryProfile(
            "SAMCHEOK", "samcheok.go.kr", "LGS-000123", "SCMS_CARD_NOTICE", SamcheokNoticePage.PATH, "/DownloadEx.do", false, false);
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash("SAMCHEOK:2|mgtNo|cd01|parse-only-id-adapter|GET|same-request|observed-x-msdownload-strict-utf8-disposition|"
            + delegate.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1", SamcheokNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    // 실제 OLE signature·HWP 확장자와 함께 확인한 삼척 중계 응답에만 적용한다.
    @Override public Set<String> selectLegacyBinaryContentTypes() { return Set.of("application/x-msdownload"); }
    @Override public boolean selectUtf8DispositionOctets() { return true; }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000123".equals(source.localSourceCode())
                    || !"SCMS_CARD_NOTICE".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return SamcheokNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (SamcheokNoticePage.selectMatches(uri)) return uri.equals(SamcheokNoticePage.selectDetailUri(uri));
            return uri != null && "/DownloadEx.do".equals(uri.getPath()) && delegate.selectApprovedRequest(uri);
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri());
    }
    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        return initial != null && initial.equals(next) && selectApprovedRequest(next);
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return new Result("FAILED", false, List.of(), List.of("ATTACHMENT_DETAIL_UNAVAILABLE"));
        try { SamcheokNoticePage.selectRoot(Jsoup.parse(html, detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return new Result("FAILED", false, List.of(), List.of("ATTACHMENT_SELECTOR_CHANGED")); }
        String id = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("mgtNo");
        // 공통 파서의 noticeId 전달 형식만 맞춘다. 이 URI는 네트워크/저장 identity로 사용하지 않는다.
        String internal = "https://" + SamcheokNoticePage.HOST + SamcheokNoticePage.PATH + "?amode=view&not_ancmt_mgt_no=" + id;
        var input = new Source(selectProviderCode(), normalizer.hash(normalizer.canonicalizeUrl(internal)), internal, "LGS-000123", "SCMS_CARD_NOTICE");
        return delegate.selectDescriptors(input, html);
    }
}
