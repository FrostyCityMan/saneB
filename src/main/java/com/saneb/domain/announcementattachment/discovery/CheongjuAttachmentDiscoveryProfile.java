package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.CheongjuNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 청주의 공식 파일 셀과 세 공개 필드 POST 폼만 사용한다. */
@Component
public final class CheongjuAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_CHEONGJU_POST_V1", SOURCE = "LGS-000136", LIST = "SAEOL_GOSI";
    private static final String FILE_HOST = "eminwon.cheongju.go.kr", DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> FIELDS = Set.of("user_file_nm","sys_file_nm","file_path");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile fileValidator = new SaeolGetAttachmentDiscoveryProfile(CODE,SOURCE,FILE_HOST,LIST,"td",false);
    private final String hash = AttachmentProfileFingerprint.selectHash(CODE + ":1|official-attachment-list|form2-empty-fields3-fixed-script-action|literal-post|persisted-canonical-identity|same-request|limit10|unknown-role|"
            + fileValidator.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1",CheongjuNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE,LIST)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(CheongjuNoticePage.HOST,FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !SOURCE.equals(source.localSourceCode())
                    || !LIST.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096) throw new IllegalArgumentException();
            URI detail = CheongjuNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
            String canonical = normalizer.canonicalizeUrl(detail.toASCIIString());
            boolean persisted = canonical.equals(source.sourceUrl()) && normalizer.hash(canonical).equals(source.providerNoticeId());
            if (!persisted && !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return detail;
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try { return uri != null && uri.toASCIIString().length() <= 8192 && uri.equals(CheongjuNoticePage.selectDetailUri(uri)); }
        catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        if (request == null) return false;
        if ("GET".equals(request.method())) return request.form().isEmpty() && selectApprovedRequest(request.uri());
        URI uri = request.uri(); var fields = request.form();
        if (!"POST".equals(request.method()) || !URI.create("https://" + FILE_HOST + DOWNLOAD).equals(uri) || !FIELDS.equals(fields.keySet())) return false;
        // 경로·파일명 검증만 기존 GET 검증기에 위임한다. GET 요청을 실행하지 않는다.
        try { return fileValidator.selectApprovedRequest(URI.create("https://" + FILE_HOST + "/emwp/jsp/ofr/FileDown.jsp?user_file_nm="
                + selectEncoded(fields.get("user_file_nm")) + "&sys_file_nm=" + selectEncoded(fields.get("sys_file_nm")) + "&file_path=" + selectEncoded(fields.get("file_path")))); }
        catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(Source source,String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page = Jsoup.parse(html,detail.toASCIIString()); Element area;
        try { area = CheongjuNoticePage.selectAttachments(page); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        var forms = CheongjuNoticePage.selectRoot(page).parent().select(":root > form[name=form2]");
        if (forms.size() != 1 || !selectForm(forms.getFirst())) return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var lists = area.select(":root > ul.p-attach");
        if (area.children().isEmpty() && area.text().isBlank()) return new Result("NO_FILES",true,List.of(),List.of());
        if (lists.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var rest = area.clone(); rest.select(":root > ul.p-attach").remove();
        boolean unresolved = selectResidual(rest), exceeded = false;
        var files = new LinkedHashMap<String,Descriptor>();
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo");
        for (var item : lists.getFirst().children()) {
            try {
                var links = item.select(":root > a.p-attach__link");
                if (!"li".equals(item.tagName()) || !item.hasClass("p-attach__item") || links.size() != 1) throw new IllegalArgumentException();
                var link = links.getFirst();
                var names = link.select(":root > span");
                if (selectActive(link) || names.size() != 1 || !names.getFirst().children().isEmpty()) throw new IllegalArgumentException();
                var args = AttachmentDownloadInvocation.selectArguments(link.attr("href"),"goDownLoad",false,false);
                if (args.size() != 3 || !names.getFirst().text().strip().equals(args.getFirst())) throw new IllegalArgumentException();
                var request = new Request(URI.create("https://" + FILE_HOST + DOWNLOAD),"POST",Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));
                if (!selectApprovedRequest(request)) throw new IllegalArgumentException();
                String id = normalizer.hash(args.get(2) + "\n" + args.get(1)), format = selectFormat(args.getFirst());
                boolean supported = format != null && format.equals(selectFormat(args.get(1)));
                var descriptor = new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),args.getFirst(),supported ? format : null,"UNKNOWN",supported,request.form());
                if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id,descriptor);
                var decoration = link.clone(); decoration.select(":root > span").remove(); decoration.removeAttr("href");
                for (var icon : decoration.select(":root > i")) if (icon.classNames().equals(Set.of("p-icon","p-icon__arrow-circle-down"))
                        && icon.children().isEmpty() && icon.text().isBlank()) icon.remove();
                if (selectResidual(decoration)) unresolved = true;
                var remaining = item.clone(); remaining.select(":root > a.p-attach__link").remove();
                if (selectResidual(remaining)) unresolved = true;
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectForm(Element form) {
        if (!"post".equalsIgnoreCase(form.attr("method")) || !form.attr("action").isEmpty()
                || form.childrenSize() != 3 || !form.ownText().isBlank() || form.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on"))) return false;
        var fields = new HashSet<String>();
        for (var input : form.children()) if (!"input".equals(input.tagName()) || !"hidden".equals(input.attr("type")) || !input.val().isEmpty()
                || !fields.add(input.attr("name")) || !input.attributes().asList().stream().allMatch(a -> Set.of("type","name","value").contains(a.getKey()))) return false;
        return fields.equals(FIELDS);
    }
    private boolean selectActive(Element element) { return !element.select("input,select,form,iframe,object,embed,script,style,svg,button").isEmpty()
            || element.getAllElements().stream().anyMatch(e -> e.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on"))); }
    private boolean selectResidual(Element element) { return !element.text().isBlank()
            || element.select("a,img,[href],li").stream().anyMatch(child -> child != element) || selectActive(element); }
    private String selectEncoded(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20"); }
    private String selectFormat(String name) { String suffix = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF","HWP","HWPX").contains(suffix) ? suffix : null; }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
