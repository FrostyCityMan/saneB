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

class GeomdanHeaderCompatibilityTest {
    @TempDir Path directory;
    private final AttachmentFileTypeValidator validator = new AttachmentFileTypeValidator();
    private String selectOctets(String value) {
        return new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }
    @Test void onlyMeasuredSourceOptsInAndYeongjongKeepsRevalidatedFingerprint() {
        var profile = IncheonThirdDownloadCases.selectCase("GEOMDAN").profile();
        assertThat(profile.selectUtf8DispositionOctets()).isTrue();
        assertThat(profile.selectLegacyBinaryContentTypes()).isEmpty();
        assertThat(profile.selectProfileHash()).isNotEqualTo("811350257855cd256ef813ff10d10b64e2d24e6b42fc312dcde54d9bed8b3681");
        var other = IncheonThirdDownloadCases.selectCase("YEONGJONG").profile();
        assertThat(other.selectUtf8DispositionOctets()).isFalse();
        // cc44ebd 공개 세션 변경 후 PUBLIC-SESSION-PINS-01에서 실파일을 재검증한 영종 지문이다.
        // 검단의 헤더 복원 옵션을 영종에 적용하거나 과거 지문을 현재 지문으로 간주하지 않는다.
        assertThat(other.selectProfileHash()).isEqualTo("8f3f311bca5baf440e23c9110e9133cc43893ee0e6baee062bdf0e004395901a");
    }
    @Test void headerRecoveryDoesNotApproveMismatchControlsPathsOrHtml() throws Exception {
        var profile = IncheonThirdDownloadCases.selectCase("GEOMDAN").profile();
        var binary = Files.write(directory.resolve("fixture.bin"),new byte[]{'P','K',3,4,20,0,0,0});
        String good=selectOctets("attachment; filename=\"청년월세지원.hwpx\"");
        var response=new Download(8,"a".repeat(64),"application/octet-stream;charset=UTF-8",good);
        assertThatThrownBy(()->validator.selectFormat(binary,response,"HWPX")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(binary,response,"HWPX",profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes())).isEqualTo("HWPX");
        var other=IncheonThirdDownloadCases.selectCase("YEONGJONG").profile();
        assertThatThrownBy(()->validator.selectFormat(binary,response,"HWPX",other.selectUtf8DispositionOctets(),other.selectLegacyBinaryContentTypes()))
                .hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        for(String bad:List.of("../지원.hwpx","경로/지원.hwpx","지원\u0085.hwpx","지원\r\n.hwpx")){
            var invalid=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\""+bad+"\""));
            assertThatThrownBy(()->validator.selectFormat(binary,invalid,"HWPX",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        }
        var wrongExtension=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\"지원.exe\""));
        assertThatThrownBy(()->validator.selectFormat(binary,wrongExtension,"HWPX",true)).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        var html=Files.writeString(directory.resolve("error.bin"),"<html>error</html>");
        assertThatThrownBy(()->validator.selectFormat(html,response,"HWPX",true)).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_GEOMDAN_HEADER_FIXTURE",matches="true")
    void measuredHeaderAndBinaryPassOnlyWithScopedRecovery() throws Exception {
        var sample=IncheonThirdDownloadCases.selectCase("GEOMDAN");
        var root=Path.of("build/qa-geomdan-header-20260930");
        String html=Files.readString(root.resolve("detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var discovery=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(discovery.complete()).isTrue();assertThat(discovery.descriptors()).hasSize(1);
        String headers=Files.readString(root.resolve("file-head.txt"),StandardCharsets.ISO_8859_1);
        String disposition=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-disposition:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        String mime=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        var binary=root.resolve("file.bin");
        var response=new Download(Files.size(binary),"65dd3e7db2539988ff81734bffc8d8576c63501a3eedd3e67800b57fbdb0820b",mime,disposition);
        assertThatThrownBy(()->validator.selectFormat(binary,response,"HWPX")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(binary,response,"HWPX",sample.profile().selectUtf8DispositionOctets(),sample.profile().selectLegacyBinaryContentTypes())).isEqualTo("HWPX");
    }
}
