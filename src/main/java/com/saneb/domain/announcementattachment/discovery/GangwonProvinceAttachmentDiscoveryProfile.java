package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.GangwonProvinceNoticePage;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 공식 첨부 영역의 고정 파일 GET만 실행한다. 미리보기는 비교만 하고 호출하지 않는다. */
@Component
public final class GangwonProvinceAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_GANGWON_PROVINCE_BOARD_V1";
    private static final Pattern DOWNLOAD = Pattern.compile("/egf/bp/common/front/([1-9][0-9]{0,14})/download");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash(
            "GANGWON_PROVINCE:1|LGS-000116|SAFE_GWD_BULLETIN|https443|GET|same-request|unknown-role|limit10|"
                    + AttachmentProfileFingerprint.selectHash("PAGE:1", GangwonProvinceNoticePage.class) + "|"
                    + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() {
        return List.of(new SourceBinding("LGS-000116", "SAFE_GWD_BULLETIN"));
    }
    @Override public Set<String> selectApprovedHosts() { return Set.of(GangwonProvinceNoticePage.HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode())
                    || !selectSourceBindings().contains(new SourceBinding(source.localSourceCode(), source.listParserProfileCode()))
                    || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) {
                throw new IllegalArgumentException();
            }
            return GangwonProvinceNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || !"https".equals(uri.getScheme()) || !GangwonProvinceNoticePage.HOST.equals(uri.getHost())
                    || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null
                    || uri.getFragment() != null || uri.getPath() == null || !uri.getPath().equals(uri.getRawPath())
                    || !uri.equals(uri.normalize())) return false;
            if (GangwonProvinceNoticePage.PATH.equals(uri.getPath())) return uri.equals(GangwonProvinceNoticePage.selectDetailUri(uri));
            return uri.getRawQuery() == null && DOWNLOAD.matcher(uri.getPath()).matches();
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
        try { area = GangwonProvinceNoticePage.selectAttachments(Jsoup.parse(html, detail.toASCIIString())).clone(); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        var files = new LinkedHashMap<String, Descriptor>();
        boolean unresolved = !area.ownText().isBlank(), exceeded = false;
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("articleSeq");
        for (Element item : area.children()) {
            try {
                if (!"div".equals(item.tagName()) || !item.hasClass("attachFile")) throw new IllegalArgumentException();
                var links = item.select(":root > a[href]");
                if (links.size() != 1 || links.getFirst().hasAttr("onclick")) throw new IllegalArgumentException();
                var link = links.getFirst();
                URI fetch = detail.resolve(link.attr("href"));
                var idMatch = DOWNLOAD.matcher(fetch.getPath() == null ? "" : fetch.getPath());
                if (!selectApprovedRequest(fetch) || !idMatch.matches()) throw new IllegalArgumentException();
                var label = link.clone();
                for (var icon : label.select(":root > span.icoFile")) {
                    if (icon.text().isBlank() && icon.children().isEmpty() && !icon.hasAttr("onclick")) icon.remove();
                }
                String name = label.text().strip();
                if (name.isBlank() || name.length() > 500 || name.contains("/") || name.contains("\\")
                        || name.contains("..") || name.contains("%") || name.indexOf('\ufffd') >= 0
                        || name.codePoints().anyMatch(Character::isISOControl) || !label.children().isEmpty()) throw new IllegalArgumentException();
                String id = idMatch.group(1), format = selectFormat(name);
                var descriptor = new Descriptor(fetch, new AttachmentSetEvidence.Locator(CODE, fetch.getPath(),
                        Map.of("noticeId", notice, "attachmentId", id)), name, format, "UNKNOWN", format != null);
                var prior = files.get(id);
                if (prior != null) { if (!prior.equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id, descriptor);
                var residue = item.clone();
                residue.select(":root > a").remove();
                for (var counter : residue.select(":root > span.attachFile-txt")) {
                    if (counter.children().isEmpty() && !counter.hasAttr("onclick")
                            && counter.text().matches("\\(다운로드: [0-9,]+ \\| [0-9,.]+[kKmMgG]?[bB]\\)")) counter.remove();
                }
                for (var preview : residue.select(":root > div.contsBtn.contsBtnSmall.skinBtnBo.v2")) {
                    var labels = preview.select(":root > span");
                    var buttons = preview.select(":root > a.contsBtn-more");
                    if (preview.childrenSize() == 2 && preview.ownText().isBlank() && labels.size() == 1
                            && labels.getFirst().children().isEmpty() && "바로보기".equals(labels.getFirst().text())
                            && buttons.size() == 1 && buttons.getFirst().children().isEmpty()
                            && "바로보기".equals(buttons.getFirst().text())
                            && "javascript:void(0);".equals(buttons.getFirst().attr("href"))
                            && ("previewAjax('" + fetch.getPath() + "');").equals(buttons.getFirst().attr("onclick"))) preview.remove();
                }
                if (!residue.text().isBlank() || !residue.children().isEmpty()) unresolved = true;
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }

    private String selectFormat(String name) {
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(extension) ? extension : null;
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
