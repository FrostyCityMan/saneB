package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.IcheonNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 이천 공식 파일의 고정 GET만 연결한다. 미리보기는 내려받기와 별개다. */
@Component
public final class IcheonAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_ICHEON_BOARD_V1", SOURCE = "LGS-000105", LIST = "SAEOL_GOSI";
    private static final String FILE_HOST = "eminwon.icheon.go.kr", DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile fileValidator = new SaeolGetAttachmentDiscoveryProfile(CODE,SOURCE,FILE_HOST,LIST,"td",false);
    private final String hash = AttachmentProfileFingerprint.selectHash(CODE + ":1|official-file-list|fixed-literal-get|five-argument-preview|preview-not-fetched|same-request|limit10|unknown-role|"
            + fileValidator.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1",IcheonNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE,LIST)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(IcheonNoticePage.HOST,FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !SOURCE.equals(source.localSourceCode())
                    || !LIST.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return IcheonNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || uri.toASCIIString().length() > 8192) return false;
            if (IcheonNoticePage.HOST.equals(uri.getHost())) return uri.equals(IcheonNoticePage.selectDetailUri(uri));
            return DOWNLOAD.equals(uri.getPath()) && fileValidator.selectApprovedRequest(uri);
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(Source source,String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element area;
        try { area = IcheonNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        if (area.children().isEmpty() && area.text().isBlank()) return new Result("NO_FILES",true,List.of(),List.of());
        var lists = area.select(":root > div > ul#updateFileList");
        if (lists.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var rest = area.clone(); rest.select(":root > div > ul#updateFileList").remove();
        boolean unresolved = selectResidual(rest), exceeded = false;
        var files = new LinkedHashMap<String,Descriptor>();
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo");
        for (var item : lists.getFirst().children()) {
            try {
                var links = item.select(":root > a");
                var downloads = links.stream().filter(a -> a.hasClass("download")).toList();
                if (!"li".equals(item.tagName()) || downloads.size() != 1) throw new IllegalArgumentException();
                var link = downloads.getFirst();
                if (!selectLink(link)) throw new IllegalArgumentException();
                var args = selectArguments(link.attr("onclick"),"goDownload");
                if (args.size() != 3 || !link.text().strip().equals(args.getFirst())) throw new IllegalArgumentException();
                URI file = URI.create("https://" + FILE_HOST + DOWNLOAD + "?user_file_nm=" + selectEncoded(args.get(0))
                        + "&sys_file_nm=" + selectEncoded(args.get(1)) + "&file_path=" + selectEncoded(args.get(2)));
                if (!selectApprovedRequest(file)) throw new IllegalArgumentException();
                String id = normalizer.hash(args.get(2) + "\n" + args.get(1)), format = selectFormat(args.getFirst());
                boolean supported = format != null && format.equals(selectFormat(args.get(1)));
                var descriptor = new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),args.getFirst(),supported ? format : null,"UNKNOWN",supported);
                if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id,descriptor);
                var remaining = item.clone();
                for (var anchor : remaining.select(":root > a")) {
                    if (anchor.hasClass("download") || selectPreview(anchor,notice,args)) anchor.remove();
                }
                if (selectResidual(remaining)) unresolved = true;
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND",true,List.copyOf(files.values()),List.of());
    }
    private List<String> selectArguments(String call,String function) {
        if (call == null || call.length() > 8192) return List.of();
        String trimmed = call.stripLeading();
        if (!trimmed.startsWith(function + "(")) return List.of();
        return AttachmentDownloadInvocation.selectArguments("goDownLoad" + trimmed.substring(function.length()),"goDownLoad",true,true);
    }
    private boolean selectPreview(Element anchor,String notice,List<String> fileArguments) {
        if (!anchor.classNames().equals(Set.of("btn","icon","small","view")) || !"#".equals(anchor.attr("href"))
                || !"바로 보기".equals(anchor.text().strip()) || !anchor.children().isEmpty()) return false;
        var clone = anchor.clone(); clone.removeAttr("onclick"); if (selectActive(clone)) return false;
        String call = anchor.attr("onclick").stripLeading(); if (call.length() > 8192) return false;
        var prefix = java.util.regex.Pattern.compile("^fn_egov_gosi_preview\\(\\s*'([1-9][0-9]{0,14})'\\s*,\\s*'([0-9]{1,2})'\\s*,").matcher(call);
        if (!prefix.find() || !notice.equals(prefix.group(1))) return false;
        return fileArguments.equals(AttachmentDownloadInvocation.selectArguments("goDownLoad(" + call.substring(prefix.end()),"goDownLoad",true,true));
    }
    private boolean selectLink(Element anchor) {
        if (!"#".equals(anchor.attr("href")) || anchor.childrenSize() != 1 || !"span".equals(anchor.child(0).tagName()) || !anchor.child(0).children().isEmpty()) return false;
        var clone = anchor.clone(); clone.removeAttr("onclick"); return !selectActive(clone);
    }
    private boolean selectActive(Element element) {
        return !element.select("input,select,form,iframe,object,embed,script,style,svg,button").isEmpty()
                || element.getAllElements().stream().anyMatch(e -> e.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));
    }
    private boolean selectResidual(Element element) { return !element.text().isBlank() || !element.select("a,img,[href]").isEmpty() || selectActive(element); }
    private String selectEncoded(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20"); }
    private String selectFormat(String name) { String suffix = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF","HWP","HWPX").contains(suffix) ? suffix : null; }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
