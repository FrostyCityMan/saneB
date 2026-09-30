package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.HwaseongNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 화성의 공식 파일 링크만 다운로드하며 같은 파일에 결합된 미리보기·듣기 버튼은 요청하지 않는다. */
@Component
public final class HwaseongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_HWASEONG_BOARD_V1", SOURCE = "LGS-000088", LIST = "SAFE_HWASEONG_LEGAL_NOTICE";
    private static final String FILE_HOST = "eminwon.hscity.go.kr", DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private static final Pattern PREVIEW = Pattern.compile("\\A\\s*(call_viewer|call_viewer_tts)\\(\\s*'([1-9][0-9]{0,14})'\\s*,");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile fileValidator = new SaeolGetAttachmentDiscoveryProfile(CODE,SOURCE,FILE_HOST,LIST,"td",false);
    private final String hash = AttachmentProfileFingerprint.selectHash(CODE + ":1|notice-id-template|official-attachment-cell"
            + "|three-literal-file-get|same-file-preview-not-fetched|same-request|limit10|unknown-role|" + fileValidator.selectProfileHash()
            + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1",HwaseongNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE,LIST)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(HwaseongNoticePage.HOST,FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !SOURCE.equals(source.localSourceCode())
                    || !LIST.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return HwaseongNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || uri.toASCIIString().length() > 8192) return false;
            if (HwaseongNoticePage.HOST.equals(uri.getHost())) return uri.equals(HwaseongNoticePage.selectDetailUri(uri));
            return DOWNLOAD.equals(uri.getPath()) && fileValidator.selectApprovedRequest(uri);
        } catch (IllegalArgumentException exception) { return false; }
    }

    @Override public boolean selectApprovedRequest(Request initial,Request next) {
        return initial != null && initial.equals(next) && selectApprovedRequest(next);
    }

    @Override public Result selectDescriptors(Source source,String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element cell;
        try { cell = HwaseongNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        if (cell.children().isEmpty() && cell.text().isBlank()) return new Result("NO_FILES",true,List.of(),List.of());
        var lists = cell.select(":root > div.file_down > ul");
        if (lists.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var files = new LinkedHashMap<String,Descriptor>();
        var outer = cell.clone(); outer.select(":root > div.file_down > ul").remove();
        boolean unresolved = selectResidual(outer), exceeded = false;
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("q_notAncmtMgtNo");
        for (var item : lists.getFirst().children()) {
            try {
                var anchors = item.select(":root > a");
                if (!"li".equals(item.tagName()) || anchors.size() != 1) throw new IllegalArgumentException();
                var anchor = anchors.getFirst();
                if (selectActive(anchor) || !anchor.children().isEmpty()) throw new IllegalArgumentException();
                var args = AttachmentDownloadInvocation.selectArguments(anchor.attr("href"),"goDownLoad",false,false);
                if (args.size() != 3 || !anchor.text().strip().equals(args.getFirst())) throw new IllegalArgumentException();
                URI uri = URI.create("https://" + FILE_HOST + DOWNLOAD + "?user_file_nm=" + selectEncoded(args.get(0))
                        + "&sys_file_nm=" + selectEncoded(args.get(1)) + "&file_path=" + selectEncoded(args.get(2)));
                if (!selectApprovedRequest(uri)) throw new IllegalArgumentException();
                String id = normalizer.hash(args.get(2) + "\n" + args.get(1));
                String format = selectFormat(args.get(0));
                boolean supported = format != null && format.equals(selectFormat(args.get(1)));
                var descriptor = new Descriptor(uri,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),
                        args.getFirst(),supported ? format : null,"UNKNOWN",supported);
                if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id,descriptor);
                var rest = item.clone(); rest.select(":root > a").remove();
                for (var preview : rest.select(":root > button")) if (selectPreview(preview,notice,args)) preview.remove();
                for (var icon : rest.select(":root > img")) if (!selectActive(icon) && "/resources/health/img/sub/file_icon.png".equals(icon.attr("src")) && icon.attr("alt").isBlank()) icon.remove();
                if (selectResidual(rest)) unresolved = true;
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND",true,List.copyOf(files.values()),List.of());
    }

    private boolean selectPreview(Element button,String notice,List<String> file) {
        if (!button.hasClass("btn-view") || !button.children().isEmpty() || button.attributes().asList().stream()
                .anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on") && !"onclick".equals(a.getKey()))) return false;
        String call = button.attr("onclick");
        var prefix = PREVIEW.matcher(call);
        if (!prefix.find() || !notice.equals(prefix.group(2))) return false;
        String text = "call_viewer".equals(prefix.group(1)) ? "바로보기" : "바로듣기";
        return text.equals(button.text().strip()) && file.equals(AttachmentDownloadInvocation.selectArguments(
                "goDownLoad(" + call.substring(prefix.end()),"goDownLoad",true,false));
    }

    private boolean selectActive(Element element) {
        return !element.select("input,select,form,iframe,object,embed,script,style,svg").isEmpty()
                || element.getAllElements().stream().anyMatch(e -> e.attributes().asList().stream()
                    .anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));
    }
    private boolean selectResidual(Element element) {
        return !element.text().isBlank() || !element.select("a,button,img,[href]").isEmpty() || selectActive(element);
    }
    private String selectEncoded(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20"); }
    private String selectFormat(String name) {
        String suffix = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        return Set.of("PDF","HWP","HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
