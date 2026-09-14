package com.saneb.domain.announcementattachment.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentFileTypeValidatorTest {
    @TempDir Path directory;
    private final AttachmentFileTypeValidator validator=new AttachmentFileTypeValidator();
    private Path selectBinary(byte[] bytes) throws Exception { return Files.write(directory.resolve("fixture.bin"),bytes); }
    private AttachmentPinnedDownloadClient.Download selectResponse(String mime,String disposition) {
        return new AttachmentPinnedDownloadClient.Download(8,"a".repeat(64),mime,disposition);
    }
    @Test void legacyBinaryMimeRequiresProfileOptInExpectedFormatAndMatchingAttachmentName() throws Exception {
        var file = selectBinary("%PDF-1.7".getBytes());
        for (String mime : java.util.List.of("application/x-msdownload", "application/octer-stream")) {
            var response = selectResponse(mime, "attachment; filename=notice.pdf");
            assertThatThrownBy(() -> validator.selectFormat(file, response, "PDF")).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
            assertThat(validator.selectFormat(file, response, "PDF", false, java.util.Set.of(mime))).isEqualTo("PDF");
            assertThatThrownBy(() -> validator.selectFormat(file, response, null, false, java.util.Set.of(mime))).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
            assertThatThrownBy(() -> validator.selectFormat(file, response, "HWPX", false, java.util.Set.of(mime))).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
            for (String badHeader : java.util.Arrays.asList(null, "inline; filename=notice.pdf", "attachment", "attachment; filename=noextension"))
                assertThatThrownBy(() -> validator.selectFormat(file, selectResponse(mime, badHeader), "PDF", false, java.util.Set.of(mime)))
                        .hasMessage("ATTACHMENT_DISPOSITION_INVALID");
            assertThatThrownBy(() -> validator.selectFormat(file, selectResponse(mime, "attachment; filename=payload.exe"), "PDF", false, java.util.Set.of(mime)))
                    .hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        }
    }
    @Test void legacyProfileCannotApproveHtmlOrExecutableSignatures() throws Exception {
        var file = selectBinary("%PDF-1.7".getBytes());
        assertThatThrownBy(() -> validator.selectFormat(file, selectResponse("text/html", "attachment; filename=notice.pdf"), "PDF", false, java.util.Set.of("text/html")))
                .hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        for (byte[] bytes : java.util.List.of("<html>error</html>".getBytes(), new byte[]{'M','Z',0,0,0,0,0,0})) {
            var invalid = selectBinary(bytes);
            assertThatThrownBy(() -> validator.selectFormat(invalid, selectResponse("application/x-msdownload", "attachment; filename=notice.pdf"), "PDF", false,
                    java.util.Set.of("application/x-msdownload"))).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
        }
    }
    @Test void signatureAndMimeAcceptSupportedFormatsWithoutUrlExtensions() throws Exception {
        assertThat(validator.selectFormat(selectBinary("%PDF-1.7".getBytes()),selectResponse("application/pdf",null),null)).isEqualTo("PDF");
        assertThat(validator.selectFormat(selectBinary(new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1}),
                selectResponse("application/x-hwp",null),"HWP")).isEqualTo("HWP");
        // ZIP 내부의 HWPX 여부는 이 validator가 확정하지 않고 격리 추출기에서 확인한다.
        assertThat(validator.selectFormat(selectBinary(new byte[]{'P','K',3,4,0,0,0,0}),selectResponse("application/zip",null),"HWPX")).isEqualTo("HWPX");
    }
    @Test void errorHtmlIsNeverAcceptedBecauseItsNameSaysPdf() throws Exception {
        var file=selectBinary("<html>error</html>".getBytes());
        assertThatThrownBy(() -> validator.selectFormat(file,selectResponse("application/pdf","attachment; filename=notice.pdf"),"PDF"))
                .hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
    }
    @Test void conflictingMimeExpectedFormatOrHeaderExtensionIsBlocked() throws Exception {
        var file=selectBinary("%PDF-1.7".getBytes());
        assertThatThrownBy(() -> validator.selectFormat(file,selectResponse("text/html",null),"PDF")).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        assertThatThrownBy(() -> validator.selectFormat(file,selectResponse("application/pdf",null),"HWP")).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
        assertThatThrownBy(() -> validator.selectFormat(file,selectResponse("application/pdf","attachment; filename=payload.exe"),"PDF"))
                .hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
    }
    @Test void rfcEncodedNameIsCheckedWithoutBeingUsedAsLocalPath() throws Exception {
        var file=selectBinary("%PDF-1.7".getBytes());
        assertThat(validator.selectFormat(file,selectResponse("application/octet-stream","attachment; filename*=UTF-8''%EA%B3%B5%EA%B3%A0.pdf"),"PDF"))
                .isEqualTo("PDF");
        assertThatThrownBy(() -> validator.selectFormat(file,selectResponse("application/pdf","attachment; filename=../notice.pdf"),"PDF"))
                .hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThatThrownBy(() -> validator.selectFormat(file,selectResponse("application/pdf","attachment\r\nX-Other: bad"),"PDF"))
                .hasMessage("ATTACHMENT_DISPOSITION_INVALID");
    }
    @Test void explicitlyApprovedUtf8OctetsRestoreKoreanWithoutRelaxingDefaultValidation() throws Exception {
        var file=selectBinary("%PDF-1.7".getBytes());
        String unicode="attachment; filename=\"지원공고.pdf\"";
        String octets=new String(unicode.getBytes(java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThatThrownBy(()->validator.selectFormat(file,selectResponse("application/pdf",octets),"PDF")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(file,selectResponse("application/pdf",octets),"PDF",true)).isEqualTo("PDF");
        String doubled=new String(octets.getBytes(java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThat(validator.selectFormat(file,selectResponse("application/pdf",doubled),"PDF",true)).isEqualTo("PDF");
        String tripled=new String(doubled.getBytes(java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThatThrownBy(()->validator.selectFormat(file,selectResponse("application/pdf",tripled),"PDF",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(file,selectResponse("application/pdf",unicode),"PDF",true)).isEqualTo("PDF");
        assertThat(validator.selectFormat(file,selectResponse("application/pdf","attachment; filename*=UTF-8''%EA%B3%B5%EA%B3%A0.pdf"),"PDF",true)).isEqualTo("PDF");
    }
    @Test void legacyUtf8ModeRejectsHeaderInjectionMalformedOctetsAndDecodedTraversal() throws Exception {
        var file=selectBinary("%PDF-1.7".getBytes());
        for(String name:java.util.List.of("../공고.pdf","경로/공고.pdf","경로\\공고.pdf","공고\u0085.pdf","공고\r\n.pdf")) {
            String header=new String(("attachment; filename=\""+name+"\"").getBytes(java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThatThrownBy(()->validator.selectFormat(file,selectResponse("application/pdf",header),"PDF",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        }
        for(String invalid:java.util.List.of("attachment; filename=\"\u00c0\u00af.pdf\"","attachment; filename=\"\u00ea\u00b3.pdf\"",
                "attachment; filename=\"\u0085.pdf\"","attachment; filename=\"a\u007f.pdf\"","attachment; filename=\"a\u0000.pdf\""))
            assertThatThrownBy(()->validator.selectFormat(file,selectResponse("application/pdf",invalid),"PDF",true)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        String mismatch=new String("attachment; filename=\"공고.exe\"".getBytes(java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThatThrownBy(()->validator.selectFormat(file,selectResponse("application/pdf",mismatch),"PDF",true)).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
    }
}
