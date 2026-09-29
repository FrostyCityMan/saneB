package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.DaejeonAggregatorNoticePage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 통합 목록 source identity를 유지하며 검증된 새올 GET 파일 엔진을 재사용한다. */
@Component
public final class DaejeonAggregatorAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_DAEJEON_AGGREGATOR_V1", SOURCE = "LGS-000071", PARSER = "DAEJEON_EMINWON_AGGREGATOR";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile delegate = new SaeolGetAttachmentDiscoveryProfile(CODE, SOURCE,
            DaejeonAggregatorNoticePage.HOST, PARSER, "th", false);
    private final String hash = AttachmentProfileFingerprint.selectHash("DAEJEON_AGGREGATOR:1|seogu-branch|six-key-source|parse-only-seven-key-adapter|same-request|"
            + delegate.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1", DaejeonAggregatorNoticePage.class), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE, PARSER)); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !SOURCE.equals(source.localSourceCode())
                    || !PARSER.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return DaejeonAggregatorNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        if (DaejeonAggregatorNoticePage.selectMatches(uri)) {
            try { DaejeonAggregatorNoticePage.selectDetailUri(uri); return true; }
            catch (IllegalArgumentException exception) { return false; }
        }
        return uri != null && "/emwp/jsp/ofr/FileDown.jsp".equals(uri.getPath()) && delegate.selectApprovedRequest(uri);
    }
    @Override public boolean selectApprovedRequest(Request request) {
        return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri());
    }
    @Override public boolean selectApprovedRequest(Request first, Request next) {
        return first != null && first.equals(next) && selectApprovedRequest(next);
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return new Result("FAILED", false, List.of(), List.of("ATTACHMENT_DETAIL_UNAVAILABLE"));
        try { DaejeonAggregatorNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return new Result("FAILED", false, List.of(), List.of("ATTACHMENT_SELECTOR_CHANGED")); }
        // 기존 엔진의 7-key 계약을 파싱 내부에서만 맞춘다. 이 합성 URL은 fetch/DB/identity에 사용하지 않는다.
        String parsingUrl = detail.toASCIIString() + "&homepage_pbs_yn=Y";
        return delegate.selectDescriptors(new Source(selectProviderCode(), normalizer.hash(normalizer.canonicalizeUrl(parsingUrl)),
                parsingUrl, SOURCE, PARSER), html);
    }
}
