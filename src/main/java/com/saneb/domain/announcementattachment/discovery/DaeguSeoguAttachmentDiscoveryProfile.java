package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.DaeguSeoguNoticePage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 현재·과거 두 공식 메뉴를 한 지역 프로필로 묶고 기존 포털 엔진에 위임한다. */
@Component
public final class DaeguSeoguAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final GyeongbukPortalAttachmentDiscoveryProfile current = selectDelegate("0601020100");
    private final GyeongbukPortalAttachmentDiscoveryProfile past = selectDelegate("0601020200");
    private final String hash = AttachmentProfileFingerprint.selectHash("DAEGU_SEOGU:1|two-official-menus|one-region|same-request|"
            + current.selectProfileHash() + "|" + past.selectProfileHash()
            + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1", DaeguSeoguNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    private GyeongbukPortalAttachmentDiscoveryProfile selectDelegate(String menu) {
        return new GyeongbukPortalAttachmentDiscoveryProfile("DAEGU_SEOGU", "LGS-000047", "SPRING_BBS",
                "dgs.go.kr", "mid", menu, false, true);
    }
    private GyeongbukPortalAttachmentDiscoveryProfile selectDelegate(Source source) {
        try {
            if (source == null || source.sourceUrl() == null || source.sourceUrl().length() > 4096) throw new IllegalArgumentException();
            var query = CapitalThirdNoticePage.selectParameters(URI.create(source.sourceUrl()).getRawQuery());
            return switch (query.getOrDefault("mid", "")) {
                case "0601020100" -> current;
                case "0601020200" -> past;
                default -> throw new IllegalArgumentException();
            };
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public String selectProviderCode() { return current.selectProviderCode(); }
    @Override public String selectProfileCode() { return current.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return current.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return current.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) { return selectDelegate(source).selectDetailUri(source); }
    @Override public Result selectDescriptors(Source source, String html) { return selectDelegate(source).selectDescriptors(source, html); }
    @Override public boolean selectApprovedRequest(URI uri) { return current.selectApprovedRequest(uri) || past.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(Request request) { return current.selectApprovedRequest(request) || past.selectApprovedRequest(request); }
    @Override public boolean selectApprovedRequest(Request initial, Request next) {
        return initial != null && initial.equals(next) && selectApprovedRequest(next);
    }
}
