package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;

/** 메인 프로세스에서는 8-byte signature만 읽는다. ZIP/OLE 내부 파싱은 격리 추출기만 수행한다. */
@Component
public final class AttachmentFileTypeValidator {
    private static final Set<String> GENERIC = Set.of("", "application/octet-stream", "application/download",
            "application/x-download", "application/force-download", "binary/octet-stream");
    public String selectFormat(Path binary, AttachmentPinnedDownloadClient.Download download, String expected) throws IOException {
        return selectFormat(binary,download,expected,false);
    }
    public String selectFormat(Path binary, AttachmentPinnedDownloadClient.Download download, String expected, boolean utf8DispositionOctets) throws IOException {
        return selectFormat(binary, download, expected, utf8DispositionOctets, Set.of());
    }
    public String selectFormat(Path binary, AttachmentPinnedDownloadClient.Download download, String expected,
                               boolean utf8DispositionOctets, Set<String> legacyBinaryContentTypes) throws IOException {
        byte[] prefix;
        try (var input = Files.newInputStream(binary)) { prefix = input.readNBytes(8); }
        String format;
        if (prefix.length >= 5 && "%PDF-".equals(new String(prefix, 0, 5, StandardCharsets.US_ASCII))) format = "PDF";
        else if (Arrays.equals(prefix, new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1})) format = "HWP";
        else if (prefix.length >= 4 && prefix[0] == 'P' && prefix[1] == 'K' && prefix[2] == 3 && prefix[3] == 4) format = "HWPX";
        else throw new IOException("ATTACHMENT_SIGNATURE_UNSUPPORTED");
        if (expected != null && !expected.equals(format)) throw new IOException("ATTACHMENT_FORMAT_MISMATCH");
        String mime = download.contentType() == null ? "" : download.contentType().split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        // 구형 MIME 예외는 프로필에서 고정한 두 값으로만 제한한다. 실제 signature와 명시적인 예상 형식이 필수다.
        boolean legacy = expected != null && expected.equals(format) && legacyBinaryContentTypes != null
                && legacyBinaryContentTypes.contains(mime) && Set.of("application/x-msdownload", "application/octer-stream").contains(mime);
        boolean accepted = legacy || GENERIC.contains(mime) || switch (format) {
            case "PDF" -> Set.of("application/pdf", "application/x-pdf").contains(mime);
            case "HWP" -> Set.of("application/x-hwp", "application/haansofthwp", "application/vnd.hancom.hwp").contains(mime);
            case "HWPX" -> Set.of("application/zip", "application/x-zip-compressed", "application/hwp+zip",
                    "application/vnd.hancom.hwpx", "application/haansofthwpx", "application/x-hwpx").contains(mime);
            default -> false;
        };
        if (!accepted) throw new IOException("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        String disposition = download.contentDisposition();
        if (legacy && disposition == null) throw new IOException("ATTACHMENT_DISPOSITION_INVALID");
        if (disposition != null) {
            if (utf8DispositionOctets) disposition=selectUtf8Disposition(disposition);
            if (disposition.length() > 2000 || disposition.codePoints().anyMatch(Character::isISOControl))
                throw new IOException("ATTACHMENT_DISPOSITION_INVALID");
            try {
                ContentDisposition parsed = ContentDisposition.parse(disposition);
                String name = parsed.getFilename();
                if (legacy && (!parsed.isAttachment() || name == null || name.lastIndexOf('.') < 0))
                    throw new IOException("ATTACHMENT_DISPOSITION_INVALID");
                if (name != null) {
                    if (name.length() > 500 || name.contains("/") || name.contains("\\") || name.codePoints().anyMatch(Character::isISOControl))
                        throw new IOException("ATTACHMENT_DISPOSITION_INVALID");
                    int dot = name.lastIndexOf('.');
                    if (dot >= 0 && !name.substring(dot + 1).toUpperCase(Locale.ROOT).equals(format))
                        throw new IOException("ATTACHMENT_DISPOSITION_MISMATCH");
                }
            } catch (IllegalArgumentException exception) { throw new IOException("ATTACHMENT_DISPOSITION_INVALID"); }
        }
        return format;
    }
    private String selectUtf8Disposition(String value) throws IOException {
        if (value.length()>2000 || value.codePoints().anyMatch(c->c<32 || c==127)) throw new IOException("ATTACHMENT_DISPOSITION_INVALID");
        // HttpComponents의 Latin-1 header 문자열에 들어온 UTF-8 octet만 엄격하게 복원한다.
        // 이미 Unicode인 값은 재인코딩하지 않으며, 불완전 UTF-8·C0/DEL·복원 후 제어문자는 모두 거부한다.
        // 부산의 실측 응답은 UTF-8 → Latin-1 문자열화가 두 번 적용되어 있다. 최대 두 번만 복원한다.
        for(int pass=0;pass<2 && value.codePoints().anyMatch(c->c>127) && value.codePoints().allMatch(c->c<=255);pass++) {
            try {
                value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(value.getBytes(StandardCharsets.ISO_8859_1))).toString();
            } catch (java.nio.charset.CharacterCodingException exception) { throw new IOException("ATTACHMENT_DISPOSITION_INVALID"); }
        }
        return value;
    }
}
