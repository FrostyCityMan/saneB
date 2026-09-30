package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationResponseMetadataTest {
    @TempDir Path temporary;
    private ObservationResponseMetadata.Snapshot select(byte[] bytes,String media,String disposition)throws Exception {
        Path binary=temporary.resolve("fixture.bin");Files.write(binary,bytes);
        return ObservationResponseMetadata.selectDetails(binary,new AttachmentPinnedDownloadClient.Download(bytes.length,"a".repeat(64),media,disposition));
    }
    @Test void observesContainerAndMimeWithoutAcceptingOrExtractingFile()throws Exception {
        var result=select(new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1},
                "APPLICATION/X-MSDOWNLOAD; charset=UTF-8","attachment; filename=private-name.hwp");
        assertThat(result).isEqualTo(new ObservationResponseMetadata.Snapshot(1,"application/x-msdownload","OLE_CONTAINER","ATTACHMENT","HWP"));
        assertThat(new ObjectMapper().writeValueAsString(result)).doesNotContain("private-name", "charset", "UTF-8");
    }
    @Test void classifiesOnlySmallSignaturesAndNeverTreatsZipAsValidatedHwpx()throws Exception {
        assertThat(select(new byte[]{'P','K',3,4},"application/zip",null).signatureKind()).isEqualTo("ZIP_CONTAINER");
        assertThat(select("%PDF-1.7".getBytes(),"application/pdf",null).signatureKind()).isEqualTo("PDF_PREFIX");
        assertThat(select("<html>".getBytes(),"text/html",null).signatureKind()).isEqualTo("OTHER");
        assertThat(select(new byte[0],null,null)).isEqualTo(new ObservationResponseMetadata.Snapshot(1,"MISSING","OTHER","MISSING","MISSING"));
    }
    @Test void removesMalformedHeadersUnknownExtensionsAndControlCharacters()throws Exception {
        var result=select(new byte[0],"invalid/private value", "attachment; filename=private-name.exe");
        assertThat(result.mediaType()).isEqualTo("UNRECOGNIZED");assertThat(result.extension()).isEqualTo("OTHER");
        assertThat(select(new byte[0],"text/html\r\nPrivate: value","attachment; filename=private\r\nname.hwp").dispositionType()).isEqualTo("INVALID");
        assertThat(select(new byte[0],"a/"+"b".repeat(128),"x".repeat(2001)).mediaType()).isEqualTo("UNRECOGNIZED");
    }
    @Test void isolatedProbeArchivesContainHelperAndNestedSnapshot()throws Exception {
        for(String path:java.util.List.of("build/official-worker-probe/official-worker-probe.jar","build/bbs-observation-probe/bbs-observation-probe.jar")) {
            try(var archive=new java.util.jar.JarFile(path)) {
                String prefix="com/saneb/domain/announcementattachment/qa/ObservationResponseMetadata";
                assertThat(archive.getJarEntry(prefix+".class")).as(path).isNotNull();
                assertThat(archive.getJarEntry(prefix+"$Snapshot.class")).as(path).isNotNull();
                assertThat(archive.getJarEntry(prefix+"Test.class")).as(path).isNull();
            }
        }
    }
}
