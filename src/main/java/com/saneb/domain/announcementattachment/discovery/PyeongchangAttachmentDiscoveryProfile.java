package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.PyeongchangNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 평창 공식 첨부 영역의 세 인자만 읽어 고정 새올 GET으로 전달한다. 스크립트는 실행하지 않는다. */
@Component
public final class PyeongchangAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_PYEONGCHANG_BOARD_V1", FILE_HOST = "eminwon.pc.go.kr";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile validator = new SaeolGetAttachmentDiscoveryProfile(
            CODE, "LGS-000127", FILE_HOST, "SPRING_BBS", "td", false);
    private final String hash = AttachmentProfileFingerprint.selectHash("PYEONGCHANG:1|fixed-area|partial-preserved|GET|same-request|limit10|unknown-role|"
            + validator.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1", PyeongchangNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("INVOCATION:1", AttachmentDownloadInvocation.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000127", "SPRING_BBS")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(PyeongchangNoticePage.HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000127".equals(source.localSourceCode())
                    || !"SPRING_BBS".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return PyeongchangNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (PyeongchangNoticePage.selectMatches(uri)) return uri.equals(PyeongchangNoticePage.selectDetailUri(uri));
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
        try { area = PyeongchangNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        var files = new LinkedHashMap<String, Descriptor>(); boolean unresolved = !area.ownText().isBlank(), exceeded = false;
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("noticeMgrNo");
        for (var item : area.children()) try {
            if (!"div".equals(item.tagName()) || !item.hasClass("attachFile")) throw new IllegalArgumentException();
            var links = item.select(":root > a[href]");
            if (links.size() != 1 || links.getFirst().hasAttr("onclick")) throw new IllegalArgumentException();
            var link = links.getFirst();
            var args = AttachmentDownloadInvocation.selectArguments(link.attr("href"), "goDownLoad", false, false);
            if (args.size() != 3) throw new IllegalArgumentException();
            var label = link.clone();
            for (var icon : label.select(":root > span.icoFile")) {
                if (icon.children().isEmpty() && icon.text().isBlank() && !icon.hasAttr("onclick")) icon.remove();
            }
            if (!label.children().isEmpty() || !args.get(0).equals(label.text().strip())) throw new IllegalArgumentException();
            URI fetch = URI.create("https://" + FILE_HOST + SaeolGetAttachmentDiscoveryProfile.DOWNLOAD
                    + "?user_file_nm=" + selectEncoded(args.get(0)) + "&sys_file_nm=" + selectEncoded(args.get(1)) + "&file_path=" + selectEncoded(args.get(2)));
            if (!selectApprovedRequest(fetch)) throw new IllegalArgumentException();
            String id = normalizer.hash(args.get(2) + "\n" + args.get(1)), format = selectFormat(args.get(0));
            boolean supported = format != null && format.equals(selectFormat(args.get(1)));
            var descriptor = new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, fetch.getPath(),
                    Map.of("noticeId", notice, "attachmentId", id)), args.get(0), supported ? format : null, "UNKNOWN", supported);
            var old = files.get(id);
            if (old != null) { if (!old.equals(descriptor)) unresolved = true; }
            else if (files.size() == 10) exceeded = true;
            else files.put(id, descriptor);
            var residual = item.clone(); residual.select(":root > a").remove();
            if (!residual.text().isBlank() || !residual.children().isEmpty()) unresolved = true;
        } catch (IllegalArgumentException exception) { unresolved = true; }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private String selectEncoded(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
