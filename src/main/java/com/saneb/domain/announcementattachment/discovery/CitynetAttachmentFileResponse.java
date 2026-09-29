package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/** 인천·울산 시티넷에서 실측한 application/file·EUC-KR 헤더만 정규화한다. */
final class CitynetAttachmentFileResponse {
    static final String LEGACY_MIME = "application/x-msdownload";
    private CitynetAttachmentFileResponse() { }

    static Download selectNormalized(Download response, Path binary) throws IOException {
        String mime = response.contentType() == null ? "" : response.contentType().split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!"application/file".equals(mime)) {
            // 내부 검증 표현을 허용했다고 서버의 다른 MIME까지 추가 허용하지 않는다.
            if (LEGACY_MIME.equals(mime)) throw new IOException("ATTACHMENT_CONTENT_TYPE_MISMATCH");
            return response;
        }
        String disposition = selectDisposition(response.contentDisposition());
        var types = new AttachmentFileTypeValidator();
        String format = types.selectFormat(binary,
                new Download(response.bytes(), response.sha256(), "application/octet-stream", disposition), null);
        var normalized = new Download(response.bytes(), response.sha256(), LEGACY_MIME, disposition);
        // 지원 서명과 attachment 파일명까지 검사한다. 내부 문서 구조 판정은 격리 추출기에 남긴다.
        types.selectFormat(binary, normalized, format, false, Set.of(LEGACY_MIME));
        return normalized;
    }

    private static String selectDisposition(String value) throws IOException {
        if (value == null || value.length() > 2000 || value.codePoints().anyMatch(c -> c < 32 || c == 127))
            throw new IOException("ATTACHMENT_DISPOSITION_INVALID");
        // ASCII/RFC5987 또는 이미 Unicode인 헤더는 재인코딩하지 않는다. 다른 문자셋으로 재시도하지 않는다.
        if (value.codePoints().anyMatch(c -> c > 127) && value.codePoints().allMatch(c -> c <= 255)) {
            try {
                value = Charset.forName("EUC-KR").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(value.getBytes(StandardCharsets.ISO_8859_1))).toString();
            } catch (CharacterCodingException exception) { throw new IOException("ATTACHMENT_DISPOSITION_INVALID"); }
        }
        if (value.codePoints().anyMatch(Character::isISOControl)) throw new IOException("ATTACHMENT_DISPOSITION_INVALID");
        return value;
    }
}
