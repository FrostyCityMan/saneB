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

class PajuHeaderCompatibilityTest {
    @TempDir Path directory;
    private final AttachmentFileTypeValidator validator = new AttachmentFileTypeValidator();
    private String selectOctets(String value) {
        return new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }
    @Test void onlyMeasuredSourceOptsInAndGwangmyeongKeepsRevalidatedFingerprint() {
        var profile = CapitalEighthDownloadCases.selectCase("PAJU").profile();
        assertThat(profile.selectUtf8DispositionOctets()).isTrue();
        assertThat(profile.selectLegacyBinaryContentTypes()).isEmpty();
        assertThat(profile.selectProfileHash()).isNotEqualTo("1cd76e6edda78e6cf879fe81597a399bd0c29a0641e238f336eb7c6a1b635d1b");
        var other = CapitalEighthDownloadCases.selectCase("GWANGMYEONG").profile();
        assertThat(other.selectUtf8DispositionOctets()).isFalse();
        assertThat(other.selectProfileHash()).isEqualTo("6de87c99dc469bdf6edcdaa44ec482485bf54055d81b0533f141cfd3c777ca37");
    }
    @Test void headerRecoveryDoesNotApproveMismatchControlsPathsOrHtml() throws Exception {
        var profile = CapitalEighthDownloadCases.selectCase("PAJU").profile();
        var binary = Files.write(directory.resolve("fixture.bin"),"%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
        String good=selectOctets("attachment; filename=\"운전자금지원.pdf\"");
        var response=new Download(8,"a".repeat(64),"application/octet-stream;charset=UTF-8",good);
        assertThatThrownBy(()->validator.selectFormat(binary,response,"PDF")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(binary,response,"PDF",profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes())).isEqualTo("PDF");
        for(String bad:List.of("../지원.pdf","경로/지원.pdf","지원\u0085.pdf","지원\r\n.pdf")){
            var invalid=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\""+bad+"\""));
            assertThatThrownBy(()->validator.selectFormat(binary,invalid,"PDF",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        }
        var wrongExtension=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\"지원.exe\""));
        assertThatThrownBy(()->validator.selectFormat(binary,wrongExtension,"PDF",true)).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        var html=Files.writeString(directory.resolve("error.bin"),"<html>error</html>");
        assertThatThrownBy(()->validator.selectFormat(html,response,"PDF",true)).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_PAJU_HEADER_FIXTURE",matches="true")
    void measuredHeaderAndBinaryPassOnlyWithScopedRecovery() throws Exception {
        var sample=CapitalEighthDownloadCases.selectCase("PAJU");
        var root=Path.of("build/qa-paju-header-20260930");
        String html=Files.readString(root.resolve("detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var discovery=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(discovery.complete()).isTrue();assertThat(discovery.descriptors()).hasSize(1);
        String headers=Files.readString(root.resolve("file-head.txt"),StandardCharsets.ISO_8859_1);
        String disposition=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-disposition:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        String mime=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElse(null);
        var binary=root.resolve("file.bin");
        var response=new Download(Files.size(binary),"25c6b8002fc63f61c1b0d48e456317433aabfa4398adb905a1a006c1e0a5d7f5",mime,disposition);
        assertThatThrownBy(()->validator.selectFormat(binary,response,"PDF")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(binary,response,"PDF",sample.profile().selectUtf8DispositionOctets(),sample.profile().selectLegacyBinaryContentTypes())).isEqualTo("PDF");
    }
}
