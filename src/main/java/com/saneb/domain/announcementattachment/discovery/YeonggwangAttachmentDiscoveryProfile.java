package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.YeonggwangNoticePage;
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

/** 영광 공식 첨부 셀의 파일 GET만 연결하며 개별 오류는 정상 파일과 분리한다. */
@Component
public final class YeonggwangAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_YEONGGWANG_GET_V1", SOURCE = "LGS-000195", LIST = "SPRING_BBS";
    private static final String FILE_HOST = "eminwon.yeonggwang.jeonnam.kr", DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile fileValidator = new SaeolGetAttachmentDiscoveryProfile(CODE,SOURCE,FILE_HOST,LIST,"td",false);
    private final String hash = AttachmentProfileFingerprint.selectHash(CODE + ":1|official-table-cell|literal-get|stored-and-raw-identity|search-not-forwarded|same-request|limit10|unknown-role|"
            + fileValidator.selectProfileHash() + "|" + AttachmentProfileFingerprint.selectHash("PAGE:1",YeonggwangNoticePage.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE,LIST)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(YeonggwangNoticePage.HOST,FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !SOURCE.equals(source.localSourceCode())
                    || !LIST.equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096) throw new IllegalArgumentException();
            URI detail = YeonggwangNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
            // collector 저장 문자열의 hash와 기존 raw URL 정규화 hash를 모두 보존한다.
            if (!normalizer.hash(source.sourceUrl()).equals(source.providerNoticeId())
                    && !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return detail;
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || uri.toASCIIString().length() > 8192) return false;
            if (YeonggwangNoticePage.HOST.equals(uri.getHost())) return uri.equals(YeonggwangNoticePage.selectDetailUri(uri));
            return DOWNLOAD.equals(uri.getPath()) && fileValidator.selectApprovedRequest(uri);
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(Source source,String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element area;
        try { area = YeonggwangNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        var rest = area.clone(); rest.select(":root > a").remove();
        boolean unresolved = selectResidual(rest), exceeded = false;
        var files = new LinkedHashMap<String,Descriptor>();
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("bs_idx");
        for (var link : area.select(":root > a")) {
            try {
                var args = AttachmentDownloadInvocation.selectArguments(link.attr("href"),"goDownLoad",false,false);
                if (selectActive(link) || !link.children().isEmpty() || args.size() != 3 || !link.text().strip().equals(args.getFirst())) throw new IllegalArgumentException();
                URI file = URI.create("https://" + FILE_HOST + DOWNLOAD + "?user_file_nm=" + selectEncoded(args.get(0))
                        + "&sys_file_nm=" + selectEncoded(args.get(1)) + "&file_path=" + selectEncoded(args.get(2)));
                if (!selectApprovedRequest(file)) throw new IllegalArgumentException();
                String id = normalizer.hash(args.get(2) + "\n" + args.get(1)), format = selectFormat(args.getFirst());
                boolean supported = format != null && format.equals(selectFormat(args.get(1)));
                var descriptor = new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),args.getFirst(),supported ? format : null,"UNKNOWN",supported);
                if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id,descriptor);
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectActive(Element element) { return !element.select("input,select,form,iframe,object,embed,script,style,svg,button").isEmpty()
            || element.getAllElements().stream().anyMatch(e -> e.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on"))); }
    private boolean selectResidual(Element element) { return !element.text().isBlank() || !element.select("a,img,[href],li").isEmpty() || selectActive(element); }
    private String selectEncoded(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20"); }
    private String selectFormat(String name) { String suffix = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT); return Set.of("PDF","HWP","HWPX").contains(suffix) ? suffix : null; }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
