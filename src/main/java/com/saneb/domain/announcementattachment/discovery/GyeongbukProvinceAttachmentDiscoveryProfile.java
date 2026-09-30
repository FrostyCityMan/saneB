package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.GyeongbukProvinceNoticePage;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 경북도 공식 파일 영역과 정적 파일 위젯 데이터를 결합한다. */
@Component
public final class GyeongbukProvinceAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE = "LOCAL_GYEONGBUK_PROVINCE_V1", SOURCE = "LGS-000200", LIST = "SAEOL_GOSI", DOWNLOAD = "/file/readFile.do";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash(CODE + ":1|official-file-slot|five-json-strings|no-script-execution|same-request|limit10|unknown-role|"
            + AttachmentProfileFingerprint.selectHash("PAGE:1",GyeongbukProvinceNoticePage.class) + "|" + AttachmentProfileFingerprint.selectHash("MANIFEST:1",GyeongbukFileManifest.class)
            + "|" + AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(SOURCE,LIST)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(GyeongbukProvinceNoticePage.HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !SOURCE.equals(source.localSourceCode()) || !LIST.equals(source.listParserProfileCode())
                    || source.sourceUrl() == null || source.sourceUrl().length() > 4096) throw new IllegalArgumentException();
            URI detail = GyeongbukProvinceNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
            if (!normalizer.hash(source.sourceUrl()).equals(source.providerNoticeId()) && !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return detail;
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || !"https".equals(uri.getScheme()) || !GyeongbukProvinceNoticePage.HOST.equals(uri.getHost()) || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getUserInfo() != null || uri.getFragment() != null || !uri.getPath().equals(uri.getRawPath()) || !uri.equals(uri.normalize())) return false;
            if (GyeongbukProvinceNoticePage.PATH.equals(uri.getPath())) return uri.equals(GyeongbukProvinceNoticePage.selectDetailUri(uri));
            var q = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return DOWNLOAD.equals(uri.getPath()) && q.keySet().equals(Set.of("fileId","fileNo")) && selectFileId(q.get("fileId")) && q.get("fileNo").matches("[1-9][0-9]{0,5}");
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(Source source,String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page = Jsoup.parse(html,detail.toASCIIString()); Element slot,area;
        try { area = GyeongbukProvinceNoticePage.selectFileArea(page); slot = GyeongbukProvinceNoticePage.selectFileSlot(page); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String fileId = slot.attr("data-vl");
        if ((!fileId.isEmpty() && !selectFileId(fileId)) || !slot.children().isEmpty() || !slot.text().isBlank()) return selectFailed("ATTACHMENT_MANIFEST_CHANGED");
        var scripts = page.select("script:not([src])").stream().map(Element::data).filter(s -> s.contains("boardView.form = $(\"#boardViewForm\");")
                && s.contains("boardView.viewFile = boardView.form.find(\"div.fileWrap.fileId\").viewFile({template:\"front\", previewYn:\"Y\"});")).toList();
        if (scripts.size() != 1) return selectFailed("ATTACHMENT_MANIFEST_CHANGED");
        var manifest = GyeongbukFileManifest.selectRecords(scripts.getFirst());
        var rest = area.clone(); rest.select("div.fileWrap.fileId").remove();
        boolean unresolved = manifest.incomplete() || !rest.text().isBlank() || !rest.select("a,img,button,input,script,[href],[onclick]").isEmpty(), exceeded = false;
        var files = new LinkedHashMap<String,Descriptor>(); var metadata = new LinkedHashMap<String,List<String>>();
        String notice = CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("boardNo");
        for (var values : manifest.records()) {
            try {
                if (!fileId.equals(values.get(0)) || !selectFileId(fileId) || !values.get(1).matches("[1-9][0-9]{0,5}") || !selectSafeName(values.get(2))
                        || !values.get(3).matches("[0-9]{1,12}") || !values.get(4).matches("[A-Za-z0-9]{1,10}")) throw new IllegalArgumentException();
                String name = values.get(2), extension = name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
                if (!extension.equals(values.get(4).toUpperCase(Locale.ROOT))) throw new IllegalArgumentException();
                boolean supported = Set.of("PDF","HWP","HWPX").contains(extension); String id = fileId + ":" + values.get(1);
                var previous = metadata.putIfAbsent(id,values); if (previous != null && !previous.equals(values)) unresolved = true;
                URI file = URI.create("https://" + GyeongbukProvinceNoticePage.HOST + DOWNLOAD + "?fileId=" + fileId + "&fileNo=" + values.get(1));
                var descriptor = new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",normalizer.hash(id))),name,supported ? extension : null,"UNKNOWN",supported);
                if (files.containsKey(id)) { if (!files.get(id).equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id,descriptor);
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (!fileId.isEmpty() && manifest.records().isEmpty()) unresolved = true;
        if (exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_MANIFEST_CHANGED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectFileId(String value) { return value != null && value.matches("FL[0-9]{11}"); }
    private boolean selectSafeName(String value) { return value != null && !value.isBlank() && value.length() <= 500 && !value.contains("/") && !value.contains("\\") && !value.contains("..")
            && !value.contains("%") && value.indexOf('\ufffd') < 0 && value.codePoints().noneMatch(Character::isISOControl); }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
