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
import java.util.Set;

/** 공식 춘천 화면이 조회하는 JSON만 해석한다. HTML/스크립트 실행이나 임의 API 탐색은 없다. */
public final class ChuncheonNoticePage {
    public static final String HOST = "www.chuncheon.go.kr";
    public static final String PAGE = "/cityhall/administrative-info/notice-info/notice-announcement/view/";
    public static final String API = "/_chuncheon/noticeView.do";
    private static final Set<String> WITHHELD = Set.of("18207", "18265", "26243", "40349", "65304", "68021");
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private ChuncheonNoticePage() { }

    public static boolean selectPageMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && PAGE.equals(uri.getPath()); }
    public static boolean selectApiMatches(URI uri) { return uri != null && HOST.equals(uri.getHost()) && API.equals(uri.getPath()); }

    public static URI selectApiUri(URI uri) {
        if ((!selectPageMatches(uri) && !selectApiMatches(uri)) || !"https".equals(uri.getScheme())
                || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null || uri.getFragment() != null
                || !uri.getRawPath().equals(uri.getPath()) || !uri.equals(uri.normalize())) throw selectInvalid();
        var query = CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        var allowed = selectPageMatches(uri) ? Set.of("notAncmtMgtNo", "pageIndex", "searchCnd", "searchWrd") : Set.of("notAncmtMgtNo");
        if (!allowed.containsAll(query.keySet()) || !query.getOrDefault("notAncmtMgtNo", "").matches("[1-9][0-9]{0,14}")) throw selectInvalid();
        return URI.create("https://" + HOST + API + "?notAncmtMgtNo=" + query.get("notAncmtMgtNo"));
    }

    public static String selectNoticeId(URI uri) {
        return CapitalThirdNoticePage.selectParameters(selectApiUri(uri).getRawQuery()).get("notAncmtMgtNo");
    }

    public static boolean selectAttachmentsWithheld(URI uri) { return WITHHELD.contains(selectNoticeId(uri)); }

    /** 실제 API는 Content-Type이 없다. 이 고정 API의 UTF-8 JSON에만 빈 헤더 예외를 둔다. */
    public static boolean selectJsonContentType(String type) {
        if (type == null || type.isBlank()) return true;
        return type.strip().toLowerCase(Locale.ROOT).matches("application/json(?:\\s*;\\s*charset\\s*=\\s*(?:utf-8|\"utf-8\"))?");
    }

    public static String selectJsonPayload(InputStream stream, String type) throws IOException {
        if (!selectJsonContentType(type)) throw new IOException("ATTACHMENT_DETAIL_CONTENT_TYPE");
        byte[] bytes = stream.readNBytes(1_048_577);
        if (bytes.length == 0 || bytes.length > 1_048_576) throw new IOException("ATTACHMENT_DETAIL_SIZE");
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException exception) { throw new IOException("ATTACHMENT_DETAIL_ENCODING"); }
    }

    public static JsonNode selectEnvelope(String payload, URI uri) {
        try {
            if (payload == null || payload.length() > 1_048_576) throw selectInvalid();
            JsonNode root = JSON.readTree(payload), board = root == null ? null : root.get("board");
            if (root == null || !root.isObject() || board == null || !board.isObject()
                    || !board.path("not_ancmt_mgt_no").isTextual() || !selectNoticeId(uri).equals(board.path("not_ancmt_mgt_no").textValue())
                    || !board.path("not_ancmt_sj").isTextual() || board.path("not_ancmt_sj").textValue().isBlank()
                    || !board.path("not_ancmt_cn").isTextual()) throw selectInvalid();
            return root;
        } catch (IOException exception) { throw selectInvalid(); }
    }

    private static IllegalArgumentException selectInvalid() { return new IllegalArgumentException("CHUNCHEON_JSON_STRUCTURE_CHANGED"); }
}
