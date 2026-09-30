package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.GwangyangNoticePage;
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

/** 광양의 공식 파일 셀과 세 공개 필드 POST 폼만 사용한다. */
@Component
public final class GwangyangAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_GWANGYANG_POST_V1", SOURCE = "LGS-000182", LIST = "SPRING_BBS";
    private static final String FILE_HOST = "eminwon.gwangyang.go.kr", DOWNLOAD = "/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> FIELDS = Set.of("user_file_nm","sys_file_nm","file_path");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile fileValidator = new SaeolGetAttachmentDiscoveryProfile(CODE,SOURCE,FILE_HOST,LIST,"td",false);
    private final String hash = AttachmentProfileFingerprint.selectHash(CODE + ":1|official-file-cell|nnn-empty-fields3|literal-post|persisted-canonical-identity|same-request|limit10|unknown-role|"
            + fileValidator.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1",GwangyangNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE,LIST)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(GwangyangNoticePage.HOST,FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !SOURCE.equals(source.localSourceCode())
                    || !LIST.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096) throw new IllegalArgumentException();
            URI detail = GwangyangNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
            String canonical = normalizer.canonicalizeUrl(detail.toASCIIString());
            boolean persisted = canonical.equals(source.sourceUrl()) && normalizer.hash(canonical).equals(source.providerNoticeId());
            if (!persisted && !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return detail;
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try { return uri != null && uri.toASCIIString().length() <= 8192 && uri.equals(GwangyangNoticePage.selectDetailUri(uri)); }
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
        try { area = GwangyangNoticePage.selectAttachments(page); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        var forms = page.select("form[name=nnn]");
        if (forms.size() != 1 || !selectForm(forms.getFirst())) return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var rest = area.clone(); rest.select(":root > a").remove();
        boolean unresolved = selectResidual(rest), exceeded = false;
        var files = new LinkedHashMap<String,Descriptor>();
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("seq");
        for (var link : area.select(":root > a")) {
            try {
                if (!link.hasClass("download") || selectActive(link) || link.childrenSize() != 1 || !"span".equals(link.child(0).tagName())
                        || !link.child(0).children().isEmpty()) throw new IllegalArgumentException();
                var args = AttachmentDownloadInvocation.selectArguments(link.attr("href"),"goDownLoad",false,false);
                if (args.size() != 3 || !link.text().strip().equals(args.getFirst())) throw new IllegalArgumentException();
                var request = new Request(URI.create("https://" + FILE_HOST + DOWNLOAD),"POST",Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));
                if (!selectApprovedRequest(request)) throw new IllegalArgumentException();
                String id = normalizer.hash(args.get(2) + "\n" + args.get(1)), format = selectFormat(args.getFirst());
                boolean supported = format != null && format.equals(selectFormat(args.get(1)));
                var descriptor = new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),args.getFirst(),supported ? format : null,"UNKNOWN",supported,request.form());
                if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id,descriptor);
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectForm(Element form) {
        if (!"post".equalsIgnoreCase(form.attr("method")) || !("https://" + FILE_HOST + DOWNLOAD).equals(form.attr("action"))
                || form.childrenSize() != 3 || !form.ownText().isBlank() || form.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on"))) return false;
        var fields = new HashSet<String>();
        for (var input : form.children()) if (!"input".equals(input.tagName()) || !"hidden".equals(input.attr("type")) || !input.val().isEmpty()
                || !fields.add(input.attr("name")) || !input.attributes().asList().stream().allMatch(a -> Set.of("type","name","value").contains(a.getKey()))) return false;
        return fields.equals(FIELDS);
    }
    private boolean selectActive(Element element) { return !element.select("input,select,form,iframe,object,embed,script,style,svg,button").isEmpty()
            || element.getAllElements().stream().anyMatch(e -> e.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on"))); }
    private boolean selectResidual(Element element) { return !element.text().isBlank() || !element.select("a,img,[href],li").isEmpty() || selectActive(element); }
    private String selectEncoded(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20"); }
    private String selectFormat(String name) { String suffix = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF","HWP","HWPX").contains(suffix) ? suffix : null; }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
