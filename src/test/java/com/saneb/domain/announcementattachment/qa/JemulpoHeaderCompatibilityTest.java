package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

class JemulpoHeaderCompatibilityTest {
    @TempDir Path directory;
    private final AttachmentFileTypeValidator validator = new AttachmentFileTypeValidator();
    private String selectOctets(String value) {
        return new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }
    @Test void onlyMeasuredSourceOptsInAndMichuholFingerprintIsPreserved() {
        var profile = IncheonSecondDownloadCases.selectCase("JEMULPO").profile();
        assertThat(profile.selectUtf8DispositionOctets()).isTrue();
        assertThat(profile.selectLegacyBinaryContentTypes()).isEmpty();
        assertThat(profile.selectProfileHash()).isNotEqualTo("b0da239fac3f2020998cc25951d41e54737e3b717a56e2974ad238c9b173e7fb");
        var other = IncheonSecondDownloadCases.selectCase("MICHUHOL").profile();
        assertThat(other.selectUtf8DispositionOctets()).isFalse();
        assertThat(other.selectProfileHash()).isEqualTo("80d669d61bfcdc6d92ea1af53c73cde316a228cbb4317c97b17bf5f59d49006e");
    }
    @Test void headerRecoveryDoesNotApproveMismatchControlsPathsOrHtml() throws Exception {
        var profile = IncheonSecondDownloadCases.selectCase("JEMULPO").profile();
        var binary = Files.write(directory.resolve("fixture.bin"),new byte[]{'P','K',3,4,20,0,0,0});
        String good=selectOctets("attachment; filename=\"청년컬처페이.hwpx\"");
        var response=new Download(8,"a".repeat(64),"application/octet-stream;charset=UTF-8",good);
        assertThatThrownBy(()->validator.selectFormat(binary,response,"HWPX")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(binary,response,"HWPX",profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes())).isEqualTo("HWPX");
        for(String bad:List.of("../지원.hwpx","경로/지원.hwpx","지원\u0085.hwpx","지원\r\n.hwpx")){
            var invalid=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\""+bad+"\""));
            assertThatThrownBy(()->validator.selectFormat(binary,invalid,"HWPX",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        }
        var wrongExtension=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\"지원.exe\""));
        assertThatThrownBy(()->validator.selectFormat(binary,wrongExtension,"HWPX",true)).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        var html=Files.writeString(directory.resolve("error.bin"),"<html>error</html>");
        assertThatThrownBy(()->validator.selectFormat(html,response,"HWPX",true)).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_JEMULPO_HEADER_FIXTURE",matches="true")
    void measuredHeaderAndBinaryPassOnlyWithScopedRecovery() throws Exception {
        var sample=IncheonSecondDownloadCases.selectCase("JEMULPO");
        var root=Path.of("build/qa-jemulpo-header-20260930");
        String html=Files.readString(root.resolve("detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var discovery=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(discovery.complete()).isTrue();assertThat(discovery.descriptors()).hasSize(3);
        String headers=Files.readString(root.resolve("file-head.txt"),StandardCharsets.ISO_8859_1);
        String disposition=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-disposition:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        String mime=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        var binary=root.resolve("file.bin");
        var response=new Download(Files.size(binary),"e078acdada2c0b70bb0cd3448c858a0605208c01dc0c1fb86ac3ef0319c448c0",mime,disposition);
        assertThatThrownBy(()->validator.selectFormat(binary,response,"HWPX")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(binary,response,"HWPX",sample.profile().selectUtf8DispositionOctets(),sample.profile().selectLegacyBinaryContentTypes())).isEqualTo("HWPX");
    }
}
