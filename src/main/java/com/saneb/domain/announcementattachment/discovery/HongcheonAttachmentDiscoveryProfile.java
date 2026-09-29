package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.HongcheonNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 공식 첨부 영역의 직접 링크만 수집한다. 미해결 링크와 정상 파일을 독립적으로 보존한다. */
@Component
public final class HongcheonAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_HONGCHEON_BOARD_V1", FILE_HOST = "eminwon.hongcheon.go.kr";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile validator = new SaeolGetAttachmentDiscoveryProfile(
            CODE, "LGS-000124", FILE_HOST, "SPRING_BBS", "td", false);
    private final String hash = AttachmentProfileFingerprint.selectHash("HONGCHEON:1|official-area|direct-get|partial-preserved|same-request|limit10|unknown-role|"
            + validator.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1", HongcheonNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000124", "SPRING_BBS")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(HongcheonNoticePage.HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000124".equals(source.localSourceCode())
                    || !"SPRING_BBS".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return HongcheonNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (HongcheonNoticePage.selectMatches(uri)) return uri.equals(HongcheonNoticePage.selectDetailUri(uri));
            return uri != null && uri.getRawPath() != null && SaeolGetAttachmentDiscoveryProfile.DOWNLOAD.equals(uri.getPath())
                    && validator.selectApprovedRequest(uri);
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
        if (html == null || html.length() > 1_048_576) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element area;
        try { area = HongcheonNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String items = ":root > ul.p-attach > li.p-attach__item";
        var residual = area.clone(); residual.select(items).remove();
        boolean unresolved = !residual.text().isBlank() || !residual.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty();
        boolean exceeded = false; var files = new LinkedHashMap<String, Descriptor>();
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no");
        for (var item : area.select(items)) try {
            var links = item.select(":root > a.p-attach__link[href]");
            if (links.size() != 1 || links.getFirst().hasAttr("onclick")) throw new IllegalArgumentException();
            var link = links.getFirst(); String raw = link.attr("href");
            if (raw.length() > 8192) throw new IllegalArgumentException();
            URI fetch = URI.create(raw.replace(" ", "%20"));
            if (!FILE_HOST.equals(fetch.getHost()) || !selectApprovedRequest(fetch)) throw new IllegalArgumentException();
            var query = CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());
            String name = query.get("user_file_nm"), systemName = query.get("sys_file_nm");
            var label = link.clone(); label.select("span.p-icon,i.p-icon").remove();
            if (!name.equals(label.text().strip())) throw new IllegalArgumentException();
            String id = normalizer.hash(query.get("file_path") + "\n" + systemName), format = selectFormat(name);
            boolean supported = format != null && format.equals(selectFormat(systemName));
            var descriptor = new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, fetch.getPath(),
                    Map.of("noticeId", notice, "attachmentId", id)), name, supported ? format : null, "UNKNOWN", supported);
            var old = files.get(id);
            if (old != null) { if (!old.equals(descriptor)) unresolved = true; }
            else if (files.size() == 10) exceeded = true;
            else files.put(id, descriptor);
            var remaining = item.clone(); remaining.select(":root > a.p-attach__link").remove();
            if (!remaining.text().isBlank() || !remaining.children().isEmpty()
                    || !link.select("script,input,button,img,iframe,object,embed,[onclick]").isEmpty()) unresolved = true;
        } catch (IllegalArgumentException exception) { unresolved = true; }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
