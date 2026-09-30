package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 공개 진도 응답에서 확인한 파일명 인코딩만 복원하며 파일 안전성 검증은 유지한다. */
class JindoHeaderCompatibilityTest {
    @TempDir Path root;
    private final AttachmentFileTypeValidator validator = new AttachmentFileTypeValidator();
    private String selectOctets(String value) {
        return new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }
    @Test void selectOnlyJindoHeaderCompatibility() {
        assertThat(JeonnamSecondDownloadCases.selectCase("JINDO").profile().selectUtf8DispositionOctets()).isTrue();
        assertThat(JeonnamSecondDownloadCases.selectCase("GOKSEONG").profile().selectUtf8DispositionOctets()).isFalse();
        assertThat(JeonnamSecondDownloadCases.selectCase("JINDO").profile().selectLegacyBinaryContentTypes()).isEmpty();
    }
    @Test void selectMeasuredHeaderRecoveryWithoutWeakeningFileChecks() throws Exception {
        Path file=Files.write(root.resolve("fixture.bin"),new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1});
        String header=selectOctets("attachment; filename=\"2026년 소상공인 융자금 이차보전 지원사업 공고.hwp\";");
        var response=new Download(8,"a".repeat(64),"application/octet-stream; charset=UTF-8",header);
        assertThatThrownBy(()->validator.selectFormat(file,response,"HWP")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        var profile=JeonnamSecondDownloadCases.selectCase("JINDO").profile();
        assertThat(validator.selectFormat(file,response,"HWP",profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes())).isEqualTo("HWP");
        for(String bad:List.of("../지원.hwp","경로/지원.hwp","지원\u0085.hwp","지원\r\n.hwp")) {
            var invalid=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\""+bad+"\""));
            assertThatThrownBy(()->validator.selectFormat(file,invalid,"HWP",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        }
        var mismatch=new Download(8,"a".repeat(64),"application/octet-stream",selectOctets("attachment; filename=\"지원.exe\""));
        assertThatThrownBy(()->validator.selectFormat(file,mismatch,"HWP",true)).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        var invalidUtf8=new Download(8,"a".repeat(64),"application/octet-stream","attachment; filename=\"\u00c3(.hwp\"");
        assertThatThrownBy(()->validator.selectFormat(file,invalidUtf8,"HWP",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        Path html=Files.writeString(root.resolve("error.bin"),"<html>오류</html>");
        assertThatThrownBy(()->validator.selectFormat(html,response,"HWP",true)).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
    }
}
