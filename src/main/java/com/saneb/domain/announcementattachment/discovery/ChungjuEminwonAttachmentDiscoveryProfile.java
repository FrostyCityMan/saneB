package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.provider.content.ChungjuEminwonNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 충주시 홈페이지 상세 → 공식 새올 파일 GET. 임의 host/selector/미리보기 실행은 허용하지 않는다. */
@Component
public final class ChungjuEminwonAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    public static final String CODE = "LOCAL_CHUNGJU_EMINWON_V1";
    private static final String FILE_HOST = "eminwon.chungju.go.kr";
    private static final String DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash("CHUNGJU_EMINWON:1|LGS-000137|SAEOL_GOSI|510|"
            + "https443|exact-file-cell|no-redirect|unknown-role|limit10|strict-headers|"
            + AttachmentProfileFingerprint.selectHash("CHUNGJU_PAGE:1", ChungjuEminwonNoticePage.class), getClass());

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000137", "SAEOL_GOSI")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(ChungjuEminwonNoticePage.HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !"LGS-000137".equals(source.localSourceCode())
                    || !"SAEOL_GOSI".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return ChungjuEminwonNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getUserInfo() != null || uri.getFragment() != null || !uri.equals(uri.normalize())
                    || uri.getRawPath() == null || !uri.getRawPath().equals(uri.getPath())) return false;
            if (ChungjuEminwonNoticePage.HOST.equals(uri.getHost())) return uri.equals(ChungjuEminwonNoticePage.selectDetailUri(uri));
            if (!FILE_HOST.equals(uri.getHost()) || !DOWNLOAD.equals(uri.getPath())) return false;
            var query = ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery());
            return query.keySet().equals(Set.of("user_file_nm", "sys_file_nm", "file_path"))
                    && selectSafeName(query.get("user_file_nm")) && selectSafeName(query.get("sys_file_nm"))
                    && query.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request initial, AttachmentPinnedDownloadClient.Request request) {
        // 다른 공고·다른 파일·다른 도메인으로의 redirect를 실제 발견 근거로 대체하지 않는다.
        return selectApprovedRequest(initial) && selectApprovedRequest(request) && initial.equals(request);
    }

    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element cell;
        try { cell = ChungjuEminwonNoticePage.selectCell(ChungjuEminwonNoticePage.selectTable(Jsoup.parse(html, detail.toASCIIString())), "파일"); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        boolean unresolved = cell.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on"));
        var files = new LinkedHashMap<String,Descriptor>();
        boolean exceeded = false;
        // 실측 영역은 다운로드 a와 줄바꿈만 있다. 다른 버튼·폼·링크를 무시해 부분 발견을 성공으로 만들지 않는다.
        if (!cell.ownText().isBlank()) unresolved = true;
        for (Element child : cell.children()) {
            if ("br".equals(child.tagName()) && child.attributes().size() == 0 && child.childNodeSize() == 0) continue;
            try {
                if (!"a".equals(child.tagName()) || !"파일 다운로드".equals(child.attr("title"))
                        || child.attributes().asList().stream().anyMatch(a -> !Set.of("href", "title").contains(a.getKey()))
                        || child.childrenSize() != 1 || !"img".equals(child.child(0).tagName())) throw new IllegalArgumentException();
                URI file = selectFileUri(detail, child.attr("href"));
                var values = ChungjuEminwonNoticePage.selectParameters(file.getRawQuery());
                String name = values.get("user_file_nm"), format = selectFormat(name);
                Element icon = child.child(0);
                if (icon.attributes().asList().stream().anyMatch(a -> !Set.of("src", "alt").contains(a.getKey()))
                        || !icon.attr("src").matches("/common/images/board/file/ico_[A-Za-z0-9]{1,10}\\.gif")
                        || !(name + " 파일 다운로드").equals(icon.attr("alt"))
                        || !child.ownText().strip().equals(name)) throw new IllegalArgumentException();
                String identity = normalizer.hash(values.get("file_path") + "\n" + values.get("sys_file_nm"));
                if (files.containsKey(identity)) {
                    if (!files.get(identity).fetchUri().equals(file) || !files.get(identity).displayName().equals(name)) unresolved = true;
                    continue;
                }
                if (files.size() == 10) { exceeded = true; continue; }
                boolean supported = format != null && format.equals(selectFormat(values.get("sys_file_nm")));
                var locator = new AttachmentSetEvidence.Locator(CODE, DOWNLOAD, Map.of("attachmentId", identity,
                        "noticeId", ChungjuEminwonNoticePage.selectParameters(detail.getRawQuery()).get("ancmt_mgt_no")));
                files.put(identity, new Descriptor(file, locator, name, supported ? format : null, "UNKNOWN", supported));
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private URI selectFileUri(URI detail, String href) {
        if (href == null || href.isBlank() || href.length() > 8192 || href.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
        URI parsed = detail.resolve(URI.create(href.replace(" ", "%20")));
        if (!FILE_HOST.equals(parsed.getHost()) || !selectApprovedRequest(parsed)) throw new IllegalArgumentException();
        var query = new StringJoiner("&");
        ChungjuEminwonNoticePage.selectParameters(parsed.getRawQuery()).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e ->
                query.add(e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8).replace("+", "%20")));
        return URI.create("https://" + FILE_HOST + DOWNLOAD + "?" + query);
    }
    private boolean selectSafeName(String value) {
        return value != null && !value.isBlank() && value.length() <= 500 && !value.contains("/") && !value.contains("\\")
                && !value.contains("..") && !value.contains("%") && value.codePoints().noneMatch(Character::isISOControl);
    }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
