package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentProfileDownloadFlow;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnsanInjeMimeCompatibilityTest {
    @TempDir Path directory;
    private static final byte[] HWP={(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};
    private AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase selectCase(String group) {
        return group.equals("ANSAN")?CapitalSixthDownloadCases.selectCase(group):GangwonSecondDownloadCases.selectCase(group);
    }
    private Request selectRequest(String group) {
        return Request.selectGet(URI.create(group.equals("ANSAN")?"https://www.ansan.go.kr/common/file/FileDown.do?file_id=1768494252902O3IP51944NE3RIE4D2BIBLEQW":"https://www.inje.go.kr/egf/bp/board/article/download?fileSeq=122177"));
    }
    private String selectFormat(String group,byte[] bytes,String mime,String disposition,String expected) throws Exception {
        var p=selectCase(group).profile();var request=selectRequest(group);var output=directory.resolve(UUID.randomUUID()+".bin");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));int[] calls={0};
        var normalized=AttachmentProfileDownloadFlow.selectDownload(p,request,output,1024*1024,(next,limit,approved)->{
            calls[0]++;assertThat(next).isEqualTo(request);assertThat(approved.test(next)).isTrue();
            assertThat(limit).isEqualTo(1024*1024);Files.write(output,bytes);return new Download(bytes.length,hash,mime,disposition);
        });
        assertThat(calls[0]).isEqualTo(1);assertThat(normalized.bytes()).isEqualTo(bytes.length);assertThat(normalized.sha256()).isEqualTo(hash);
        assertThat(normalized.contentDisposition()).isEqualTo(disposition);
        return new AttachmentFileTypeValidator().selectFormat(output,normalized,expected,p.selectUtf8DispositionOctets(),p.selectLegacyBinaryContentTypes());
    }
    @Test void optInIsLimitedToMeasuredSourcesAndOtherProfileHashesStayUnchanged() {
        var ansan=CapitalSixthDownloadCases.selectCase("ANSAN").profile();
        var inje=GangwonSecondDownloadCases.selectCase("INJE").profile();
        assertThat(ansan.selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");
        assertThat(ansan.selectUtf8DispositionOctets()).isTrue();
        assertThat(inje.selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");
        assertThat(inje.selectUtf8DispositionOctets()).isFalse();
        assertThat(CapitalSixthDownloadCases.selectCase("SIHEUNG").profile().selectProfileHash()).isEqualTo("322e1bcba46d62483cecbbaab6a7f6102602cb173d28f22bf5227d5929fb320f");
        assertThat(GangwonSecondDownloadCases.selectCase("YANGGU").profile().selectProfileHash()).isEqualTo("ad236849cd3a20da25b4dc4c32e66a94927478258b1fb0e205a392dccd0e3a37");
    }
    @ParameterizedTest @ValueSource(strings={"ANSAN","INJE"})
    @EnabledIfEnvironmentVariable(named="SANEB_ANSAN_INJE_MIME_FIXTURE",matches="true")
    void measuredFileStillRequiresSignatureExpectedFormatAndDisposition(String group) throws Exception {
        var sample=group.equals("ANSAN")?CapitalSixthDownloadCases.selectCase(group):GangwonSecondDownloadCases.selectCase(group);
        var root=Path.of("build/qa-"+group.toLowerCase(java.util.Locale.ROOT)+"-mime-20260930");
        String html=Files.readString(root.resolve("detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var discovery=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(discovery.complete()).isTrue();assertThat(discovery.descriptors()).hasSize(1);
        String headers=Files.readString(root.resolve("file-head.txt"),StandardCharsets.ISO_8859_1);
        String disposition=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-disposition:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        String mime=headers.lines().filter(l->l.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        var binary=root.resolve("file.bin");String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(binary)));
        assertThat(hash).isEqualTo(group.equals("ANSAN")?"039bf09bedc5e08627b65db14d83a2155c54b9db518520db9c1b98e5f03aa08e":"3e7feb24bb08de48abe8d485af783e83cd8d94eede3ad7cc4b4673d44c1087cf");
        var response=new Download(Files.size(binary),hash,mime,disposition);var validator=new AttachmentFileTypeValidator();
        assertThatThrownBy(()->validator.selectFormat(binary,response,"HWP")).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        assertThat(discovery.descriptors().getFirst().selectRequest()).isEqualTo(selectRequest(group));
        assertThat(selectFormat(group,Files.readAllBytes(binary),mime,disposition,"HWP")).isEqualTo("HWP");
        assertThatThrownBy(()->selectFormat(group,Files.readAllBytes(binary),mime,disposition,"PDF")).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
    }
    @ParameterizedTest @ValueSource(strings={"ANSAN","INJE"})
    void scopedMimeStillRequiresOleAndHwpAttachmentName(String group) throws Exception {
        String mime=group.equals("ANSAN")?"application/unknown":"application/x-tika-msoffice",good="attachment; filename=notice.hwp";
        assertThat(selectFormat(group,HWP,mime,good,"HWP")).isEqualTo("HWP");
        assertThat(selectFormat(group,HWP,"application/x-hwp",good,"HWP")).isEqualTo("HWP");
        for(String bad:Arrays.asList(null,"inline; filename=notice.hwp","attachment","attachment; filename=noextension","attachment; filename=../notice.hwp","attachment; filename=\"a\u0085.hwp\""))
            assertThatThrownBy(()->selectFormat(group,HWP,mime,bad,"HWP")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        for(String name:List.of("notice.xls","notice.doc","notice.exe"))
            assertThatThrownBy(()->selectFormat(group,HWP,mime,"attachment; filename="+name,"HWP")).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        for(byte[] invalid:List.of("<html>error</html>".getBytes(StandardCharsets.US_ASCII),new byte[]{'M','Z',0,0,0,0,0,0}))
            assertThatThrownBy(()->selectFormat(group,invalid,mime,good,"HWP")).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
        byte[] pdf="%PDF-1.7".getBytes(StandardCharsets.US_ASCII),zip={'P','K',3,4,0,0,0,0};
        if(group.equals("ANSAN")){
            assertThat(selectFormat(group,pdf,mime,"attachment; filename=notice.pdf","PDF")).isEqualTo("PDF");
            assertThat(selectFormat(group,zip,mime,"attachment; filename=notice.hwpx","HWPX")).isEqualTo("HWPX");
        }else{
            assertThatThrownBy(()->selectFormat(group,pdf,mime,"attachment; filename=notice.pdf","PDF")).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
            assertThatThrownBy(()->selectFormat(group,zip,mime,"attachment; filename=notice.hwpx","HWPX")).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
        }
        for(String unapproved:List.of("text/html","application/x-msdownload",group.equals("ANSAN")?"application/x-tika-msoffice":"application/unknown"))
            assertThatThrownBy(()->selectFormat(group,HWP,unapproved,good,"HWP")).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
    }
}
