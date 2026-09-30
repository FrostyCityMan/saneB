package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import org.springframework.http.ContentDisposition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/** QA에서 파일 형식 불일치를 진단한다. URL·파일명·본문·원시 header는 보존하지 않는다. */
final class ObservationResponseMetadata {
    record Snapshot(int schemaVersion, String mediaType, String signatureKind,
                    String dispositionType, String extension) { }

    static Snapshot selectDetails(Path binary, AttachmentPinnedDownloadClient.Download download) throws IOException {
        String media = download.contentType() == null ? "MISSING" : download.contentType().split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!media.equals("MISSING") && (media.length() > 128 || !media.matches("[a-z0-9.+-]+/[a-z0-9.+-]+"))) media = "UNRECOGNIZED";
        byte[] prefix;
        try (var input = Files.newInputStream(binary)) { prefix = input.readNBytes(8); }
        String signature = Arrays.equals(prefix, new byte[]{(byte) 0xd0,(byte) 0xcf,0x11,(byte) 0xe0,(byte) 0xa1,(byte) 0xb1,0x1a,(byte) 0xe1})
                ? "OLE_CONTAINER" : prefix.length >= 5 && prefix[0]=='%' && prefix[1]=='P' && prefix[2]=='D' && prefix[3]=='F' && prefix[4]=='-'
                ? "PDF_PREFIX" : prefix.length >= 4 && prefix[0]=='P' && prefix[1]=='K' && prefix[2]==3 && prefix[3]==4 ? "ZIP_CONTAINER" : "OTHER";
        String type="MISSING",extension="MISSING";
        if (download.contentDisposition()!=null) {
            type="INVALID";
            if (download.contentDisposition().length()<=2000 && download.contentDisposition().codePoints().noneMatch(Character::isISOControl)) {
                try {
                    var parsed=ContentDisposition.parse(download.contentDisposition());
                    type=parsed.isAttachment()?"ATTACHMENT":parsed.isInline()?"INLINE":"OTHER";
                    var name=parsed.getFilename();
                    if(name!=null) {
                        String suffix=name.lastIndexOf('.')<0?"":name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);
                        extension=Set.of("PDF","HWP","HWPX").contains(suffix)?suffix:"OTHER";
                    }
                } catch (IllegalArgumentException ignored) { /* 원시 header를 기록하지 않는다. */ }
            }
        }
        return new Snapshot(1,media,signature,type,extension);
    }
}
