package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.DamyangNoticePage;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 공식 JSON의 파일명·고정 호출 인수만 결합한다. 잘못된 항목과 정상 첨부를 분리한다. */
@Component
public final class DamyangAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile, AttachmentJsonDetailProfile {
    private static final String CODE = "LOCAL_DAMYANG_JSON_V1", FILE_HOST = "eminwon.damyang.jeonnam.kr", FILE_PATH = "/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> FIELDS = Set.of("user_file_nm", "sys_file_nm", "file_path");
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();
    private final String hash = AttachmentProfileFingerprint.selectHash("DAMYANG_JSON:1|LGS-000183|DAMYANG_NOTICE_JSON|strict-json-mime|utf8|fresh-api|request-bound-no-response-id|paired-literal-manifest|GET|same-request|limit10|unknown-role|"
            + AttachmentProfileFingerprint.selectHash("PAGE:1", DamyangNoticePage.class) + "|"
            + AttachmentProfileFingerprint.selectHash("JSON:1", AttachmentJsonDetailProfile.class) + "|"
            + AttachmentProfileFingerprint.selectHash("QUERY:1", CapitalThirdNoticePage.class), getClass());

    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return CODE; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding("LGS-000183", "DAMYANG_NOTICE_JSON")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(DamyangNoticePage.HOST, FILE_HOST); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String payload) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }

    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || source.sourceUrl() == null
                    || source.sourceUrl().length() > 4096 || !selectSourceBindings().contains(new SourceBinding(source.localSourceCode(), source.listParserProfileCode()))
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            URI page = URI.create(source.sourceUrl());
            if (!DamyangNoticePage.selectPageMatches(page)) throw new IllegalArgumentException();
            return DamyangNoticePage.selectApiUri(page);
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }

    @Override public String selectJsonPayload(InputStream stream, String type) throws IOException { return DamyangNoticePage.selectJsonPayload(stream,type); }
    @Override public String selectJsonTitle(Source source, String payload) {
        return Jsoup.parseBodyFragment(DamyangNoticePage.selectDetail(payload,selectDetailUri(source)).path("col4").textValue()).text();
    }

    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if (uri == null || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getUserInfo() != null || uri.getFragment() != null || uri.getPath() == null
                    || !uri.getPath().equals(uri.getRawPath()) || !uri.equals(uri.normalize()) || uri.toASCIIString().length() > 8192) return false;
            if (DamyangNoticePage.selectApiMatches(uri)) return uri.equals(DamyangNoticePage.selectApiUri(uri));
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
        try { root = DamyangNoticePage.selectDetail(payload,detail); }
        catch (IllegalArgumentException exception) { return selectFailed("ATTACHMENT_MANIFEST_CHANGED"); }
        var names = root.get("fileNameArrList");
        var scripts = root.get("fileScriptArrList");
        if (names == null || !names.isArray() || scripts == null || !scripts.isArray()) return selectFailed("ATTACHMENT_MANIFEST_CHANGED");
        var files = new LinkedHashMap<String,Descriptor>();
        boolean unresolved = names.size() != scripts.size(), exceeded = false;
        for (int index = 0; index < Math.min(names.size(),scripts.size()); index++) {
            try {
                if (!names.get(index).isTextual() || !scripts.get(index).isTextual()) throw new IllegalArgumentException();
                var arguments = AttachmentDownloadInvocation.selectArguments(scripts.get(index).textValue(),"goDownLoad",false,true);
                if (arguments.size() != 3 || !names.get(index).textValue().equals(arguments.get(0))) throw new IllegalArgumentException();
                String name = arguments.get(0), stored = arguments.get(1), directory = arguments.get(2);
                URI file = URI.create("https://" + FILE_HOST + FILE_PATH + "?user_file_nm=" + selectEncoded(name)
                        + "&sys_file_nm=" + selectEncoded(stored) + "&file_path=" + selectEncoded(directory));
                if (!selectApprovedRequest(file)) throw new IllegalArgumentException();
                String id = normalizer.hash(directory + "\n" + stored), format = selectFormat(name);
                boolean supported = format != null && format.equals(selectFormat(stored));
                var descriptor = new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,FILE_PATH,
                        Map.of("noticeId",DamyangNoticePage.selectNoticeId(detail),"attachmentId",id)),name,supported ? format : null,"UNKNOWN",supported);
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
