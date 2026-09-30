package com.saneb.domain.announcementsource.provider.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 담양 공식 화면의 최신화 JSON 경로만 사용한다. 캐시 응답과 JavaScript는 실행하지 않는다. */
public final class DamyangNoticePage {
    public static final String HOST = "www.damyang.go.kr";
    public static final String PAGE = "/eminwon/searchDetail";
    public static final String API = "/eminwon/refreshSearchDetail";
    private static final Map<String,String> PAGE_FIELDS = Map.of("domainId","DOM_0000001",
            "menuCd","DOM_000000190001002001","contentsSid","2","listType","01");
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private DamyangNoticePage() { }
    public static boolean selectPageMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PAGE.equals(uri.getPath()); }
    public static boolean selectApiMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && API.equals(uri.getPath()); }
    public static URI selectApiUri(URI uri) {
        if ((!selectPageMatches(uri) && !selectApiMatches(uri)) || !"https".equals(uri.getScheme())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                || !uri.getRawPath().equals(uri.getPath()) || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        var allowed = selectPageMatches(uri) ? Set.of("notAncmtMgtNo","domainId","menuCd","contentsSid","listType") : Set.of("notAncmtMgtNo");
        if (!allowed.containsAll(query.keySet()) || !query.getOrDefault("notAncmtMgtNo", "").matches("[1-9][0-9]{0,14}")) throw selectInvalid();
        for (var field : PAGE_FIELDS.entrySet()) if (query.containsKey(field.getKey()) && !field.getValue().equals(query.get(field.getKey()))) throw selectInvalid();
        return URI.create("https://" + HOST + API + "?notAncmtMgtNo=" + query.get("notAncmtMgtNo"));
    }
    public static String selectNoticeId(URI uri) { return CapitalThirdNoticePage.selectParameters(selectApiUri(uri).getRawQuery()).get("notAncmtMgtNo"); }
    public static boolean selectJsonContentType(String type) {
        return type != null && type.strip().toLowerCase(Locale.ROOT).matches("application/json(?:\\s*;\\s*charset\\s*=\\s*(?:utf-8|\"utf-8\"))?");
    }
    public static String selectJsonPayload(InputStream stream, String type) throws IOException {
        if (!selectJsonContentType(type)) throw new IOException("ATTACHMENT_DETAIL_CONTENT_TYPE");
        byte[] bytes = stream.readNBytes(1_048_577);
        if (bytes.length == 0 || bytes.length > 1_048_576) throw new IOException("ATTACHMENT_DETAIL_SIZE");
        try { return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (java.nio.charset.CharacterCodingException exception) { throw new IOException("ATTACHMENT_DETAIL_ENCODING"); }
    }
    /** 응답에는 공고 ID가 없다. 요청 경로를 검증하며, 응답 자체의 ID 일치를 주장하지 않는다. */
    public static JsonNode selectDetail(String payload, URI uri) {
        selectApiUri(uri);
        try {
            if (payload == null || payload.length() > 1_048_576) throw selectInvalid();
            JsonNode root = JSON.readTree(payload), data = root == null ? null : root.get("RSLT_DATA");
            JsonNode detail = data == null ? null : data.get("searchDetail");
            if (root == null || !root.isObject() || !"0000".equals(root.path("RSLT_CD").asText())
                    || data == null || !data.isObject() || detail == null || !detail.isObject()
                    || data.path("needsRefresh").asBoolean(false) || root.path("needsRefresh").asBoolean(false)
                    || !detail.path("col4").isTextual() || detail.path("col4").textValue().isBlank()
                    || !detail.path("col8").isTextual()) throw selectInvalid();
            return detail;
        } catch (IOException exception) { throw selectInvalid(); }
    }
    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("DAMYANG_JSON_STRUCTURE_CHANGED"); }
}
