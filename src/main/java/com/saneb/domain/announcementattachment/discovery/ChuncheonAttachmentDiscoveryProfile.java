package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChuncheonNoticePage;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** JSON 파일 목록의 검증된 세 필드만 공식 GET에 결합한다. 제공자가 감춘 첨부는 요청하지 않는다. */
@Component
public final class ChuncheonAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile, AttachmentJsonDetailProfile {
    private static final String CODE = "LOCAL_CHUNCHEON_JSON_V1", FILE_HOST = "eminwon.chuncheon.go.kr", FILE_PATH = "/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> FIELDS = Set.of("user_file_nm", "sys_file_nm", "file_path");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash("CHUNCHEON_JSON:1|LGS-000117|CHUNCHEON_NOTICE_JSON|missing-mime-measured-json|utf8|fixed-api|same-id|withheld-six|GET|same-request|limit10|unknown-role|"
            + AttachmentProfileFingerprint.selectHash("PAGE:1", ChuncheonNoticePage.class) + "|"
            + AttachmentProfileFingerprint.selectHash("JSON:1", AttachmentJsonDetailProfile.class) + "|"
            + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000117", "CHUNCHEON_NOTICE_JSON")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(ChuncheonNoticePage.HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String payload) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || source.sourceUrl() == null
                    || source.sourceUrl().length() > 4096 || !selectSourceBindings().contains(new SourceBinding(source.localSourceCode(), source.listParserProfileCode()))
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            URI page = URI.create(source.sourceUrl());
            if (!ChuncheonNoticePage.selectPageMatches(page) || ChuncheonNoticePage.selectAttachmentsWithheld(page)) throw new IllegalArgumentException();
            return ChuncheonNoticePage.selectApiUri(page);
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public String selectJsonPayload(InputStream stream, String type) throws IOException { return ChuncheonNoticePage.selectJsonPayload(stream,type); }
    @Override public String selectJsonTitle(Source source, String payload) {
        return Jsoup.parseBodyFragment(ChuncheonNoticePage.selectEnvelope(payload,selectDetailUri(source)).path("board").path("not_ancmt_sj").textValue()).text();
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getUserInfo() != null || uri.getFragment() != null || uri.getPath() == null
                    || !uri.getPath().equals(uri.getRawPath()) || !uri.equals(uri.normalize()) || uri.toASCIIString().length() > 8192) return false;
            if (ChuncheonNoticePage.selectApiMatches(uri)) return uri.equals(ChuncheonNoticePage.selectApiUri(uri)) && !ChuncheonNoticePage.selectAttachmentsWithheld(uri);
            if (!FILE_HOST.equals(uri.getHost()) || !FILE_PATH.equals(uri.getPath())) return false;
            var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return query.keySet().equals(FIELDS) && selectSafeName(query.get("user_file_nm")) && selectSafeName(query.get("sys_file_nm"))
                    && query.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        } catch (IllegalArgumentException exception) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) { return request != null && "GET".equals(request.method()) && request.form().isEmpty() && selectApprovedRequest(request.uri()); }
    @Override public boolean selectApprovedRequest(Request initial, Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }

    @Override public Result selectDescriptors(Source source, String payload) {
        URI detail = selectDetailUri(source);
        com.fasterxml.jackson.databind.JsonNode root;
        try { root = ChuncheonNoticePage.selectEnvelope(payload,detail); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_MANIFEST_CHANGED"); }
        var rows = root.get("file");
        if (rows == null || !rows.isArray()) return selectFailed("ATTACHMENT_MANIFEST_CHANGED");
        var files = new LinkedHashMap<String,Descriptor>(); var sequences = new HashMap<String,String>();
        boolean unresolved = false, exceeded = false;
        for (var row : rows) {
            try {
                if (!row.isObject() || !row.path("file_nm").isTextual() || !row.path("sys_file_nm").isTextual()
                        || !row.path("file_path").isTextual() || !row.path("file_seq").isTextual()
                        || !row.path("file_seq").textValue().matches("[0-9]{1,4}")) throw new IllegalArgumentException();
                String name = row.path("file_nm").textValue(), stored = row.path("sys_file_nm").textValue(), directory = row.path("file_path").textValue();
                URI file = URI.create("https://" + FILE_HOST + FILE_PATH + "?user_file_nm=" + selectEncoded(name)
                        + "&sys_file_nm=" + selectEncoded(stored) + "&file_path=" + selectEncoded(directory));
                if (!selectApprovedRequest(file)) throw new IllegalArgumentException();
                String id = normalizer.hash(directory + "\n" + stored), format = selectFormat(name);
                boolean supported = format != null && format.equals(selectFormat(stored));
                var descriptor = new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,FILE_PATH,
                        Map.of("noticeId",ChuncheonNoticePage.selectNoticeId(detail),"attachmentId",id)),name,supported ? format : null,"UNKNOWN",supported);
                var oldSequence = sequences.putIfAbsent(row.path("file_seq").textValue(),id);
                if (oldSequence != null && !oldSequence.equals(id)) unresolved = true;
                var prior = files.get(id);
                if (prior != null) { if (!prior.equals(descriptor)) unresolved = true; }
                else if (files.size() == 10) exceeded = true;
                else files.put(id,descriptor);
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND",true,List.copyOf(files.values()),List.of());
    }
    private String selectEncoded(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20"); }
    private boolean selectSafeName(String value) { return value != null && !value.isBlank() && value.length() <= 500 && !value.contains("/") && !value.contains("\\") && !value.contains("..") && !value.contains("%") && value.indexOf('\ufffd') < 0 && value.codePoints().noneMatch(Character::isISOControl); }
    private String selectFormat(String name) { String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : ""; return Set.of("PDF","HWP","HWPX").contains(extension) ? extension : null; }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
