package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.UlsanCityNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 시티넷 첨부의 고정 경로와 네 매개변수만 허용한다. 불투명 식별값은 해시로 기록한다. */
@Component
public final class UlsanCityAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_ULSAN_CITY_CITYNET_V1", DOWNLOAD = "/citynet/jsp/cmm/attach/download.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash("ULSAN_CITY:1|official-https-transfer-detail|citynet-fixed-get|partial-preserved|no-preview|limit10|unknown-role|"
            + AttachmentProfileFingerprint.selectHash("PAGE:1", UlsanCityNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000077", "SAEOL_GOSI")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(UlsanCityNoticePage.HOST, "minwon.ulsan.go.kr"); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000077".equals(source.localSourceCode())
                    || !"SAEOL_GOSI".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return UlsanCityNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || uri.getHost() == null || !"https".equals(uri.getScheme()) || !selectApprovedHosts().contains(uri.getHost())
                    || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                    || !uri.equals(uri.normalize())) return false;
            if (UlsanCityNoticePage.selectMatches(uri)) return uri.equals(UlsanCityNoticePage.selectDetailUri(uri));
            if (!"minwon.ulsan.go.kr".equals(uri.getHost()) || !DOWNLOAD.equals(uri.getRawPath())) return false;
            var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return Set.of("mode", "fid", "index", "other").equals(query.keySet()) && "download".equals(query.get("mode"))
                    && query.get("fid").matches("#[a-f0-9]{64}") && query.get("other").matches("#[a-f0-9]{96}")
                    && query.get("index").matches("[0-9]{1,3}");
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
        try { area = UlsanCityNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString()), detail).clone(); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        // 실제 화면의 대응 미리보기 버튼은 실행하지 않는다. 같은 파일 순번의 고정 호출만 장식으로 제외한다.
        for (var button : area.select(":root > button.btn_preview")) {
            var previous = button.previousElementSibling();
            var call = java.util.regex.Pattern.compile("^fn_fileNoticePreivew\\('F[0-9]{16}','([0-9]{1,3})'\\);$").matcher(button.attr("onclick").strip());
            if (previous == null || !"a".equals(previous.tagName()) || !"button".equals(button.attr("type"))
                    || !"미리보기".equals(button.text().strip()) || !call.matches()
                    || !button.children().select("a,button,input,script,iframe,object,embed,[onclick],[href]").isEmpty()) continue;
            try {
                URI file = detail.resolve(previous.attr("href"));
                if (DOWNLOAD.equals(file.getRawPath()) && selectApprovedRequest(file)
                        && call.group(1).equals(CapitalThirdNoticePage.selectParameters(file.getRawQuery()).get("index"))) button.remove();
            } catch (IllegalArgumentException ignored) { }
        }
        String items = ":root > a[href]";
        var residual = area.clone(); residual.select(items).remove();
        boolean unresolved = !residual.text().isBlank() || !residual.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty();
        boolean exceeded = false; var files = new LinkedHashMap<String, Descriptor>();
        String notice = UlsanCityNoticePage.selectNoticeId(detail);
        for (var link : area.select(items)) try {
            if (link.hasAttr("onclick") || link.attr("href").length() > 4096) throw new IllegalArgumentException();
            URI fetch = detail.resolve(link.attr("href"));
            if (!DOWNLOAD.equals(fetch.getRawPath()) || !selectApprovedRequest(fetch)) throw new IllegalArgumentException();
            var query = CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());
            String name = link.text().strip();
            if (name.isBlank() || name.length() > 500 || name.contains("/") || name.contains("\\") || name.contains("..")
                    || name.indexOf('\ufffd') >= 0 || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
            String id = normalizer.hash(query.get("fid") + "\n" + query.get("index") + "\n" + query.get("other")), format = selectFormat(name);
            var descriptor = new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, DOWNLOAD,
                    Map.of("noticeId", notice, "attachmentId", id)), name, format, "UNKNOWN", format != null);
            var old = files.get(id);
            if (old != null) { if (!old.equals(descriptor)) unresolved = true; }
            else if (files.size() == 10) exceeded = true;
            else files.put(id, descriptor);
            if (!link.select("script,input,button,img,iframe,object,embed,[onclick]").isEmpty()) unresolved = true;
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
