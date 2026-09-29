package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LegacyThreeMimeCompatibilityTest {
    @TempDir Path directory;
    private static final byte[] OLE={(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};
    private static final byte[] ZIP={'P','K',3,4,0,0,0,0};
    private AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase selectCase(String group) {
        return switch(group) {
            case "YONGSAN" -> SeoulSeventhDownloadCases.selectCase(group);
            case "GEUMSAN" -> ChungcheongFifthDownloadCases.selectCase(group);
            case "CHANGWON" -> GyeongnamSecondDownloadCases.selectCase(group);
            default -> throw new IllegalArgumentException();
        };
    }
    private Request selectRequest(String group) {
        String inner="http://eminwon.changwon.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=notice.hwpx&sys_file_nm=stored.hwpx&file_path=/ntishome/file/upload/ofr/ofr/20260929";
        return Request.selectGet(URI.create(switch(group) {
            case "YONGSAN" -> "https://health.yongsan.go.kr/portal/cmmn/file/fileDown.do?menuNo=200233&atchFileId="+"a".repeat(32)+"&fileSn=1";
            case "GEUMSAN" -> "https://www.geumsan.go.kr/_prog/download/?func_gbn_cd=gosi&site_dvs_cd=kr&filename=20260908093000_"+"1".repeat(30)+".hwpx&file_realname=notice.hwpx";
            case "CHANGWON" -> "https://www.changwon.go.kr/cwportal/DownloadEx.do?url="+URLEncoder.encode(inner,StandardCharsets.UTF_8)+"&name=notice.hwpx";
            default -> throw new IllegalArgumentException();
        }));
    }
    private String selectFormat(String group,Request request,byte[] bytes,String mime,String disposition,String expected) throws Exception {
        var p=selectCase(group).profile();Path output=directory.resolve(UUID.randomUUID()+".bin");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));int[] calls={0};
        var normalized=AttachmentProfileDownloadFlow.selectDownload(p,request,output,1024*1024,(next,limit,approved)->{
            calls[0]++;assertThat(next).isEqualTo(request);assertThat(approved.test(next)).isTrue();
            assertThat(limit).isEqualTo(1024*1024);Files.write(output,bytes);return new Download(bytes.length,hash,mime,disposition);
        });
        assertThat(calls[0]).isEqualTo(1);assertThat(normalized.bytes()).isEqualTo(bytes.length);assertThat(normalized.sha256()).isEqualTo(hash);
        assertThat(normalized.contentDisposition()).isEqualTo(disposition);
        assertThat(normalized.contentType()).isEqualTo(group.equals("GEUMSAN")&&mime.equals("hwpx")?"application/x-msdownload":mime);
        return new AttachmentFileTypeValidator().selectFormat(output,normalized,expected,p.selectUtf8DispositionOctets(),p.selectLegacyBinaryContentTypes());
    }
    @Test void limitsAndNeighborProfilesRemainUnchanged() {
        for(String group:List.of("YONGSAN","GEUMSAN","CHANGWON")) {
            var p=selectCase(group).profile();assertThat(p.selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");
            assertThat(p.selectUtf8DispositionOctets()).isEqualTo(!group.equals("GEUMSAN"));
            assertThat(AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(p)).isEqualTo((group.equals("GEUMSAN")?2L:1L)*1024*1024);
        }
        assertThat(SeoulSeventhDownloadCases.selectCase("SEOUL").profile().selectProfileHash()).isEqualTo("9913bc326c21ccd1eb0335c1c333d5eac3c1b0106e95c9a507543e1e5a9b7258");
        assertThat(SeoulSeventhDownloadCases.selectCase("SEOUL_JUNGGU").profile().selectProfileHash()).isEqualTo("b58c862d6922fa5cddab006b0ba5f70ba76fc35907cd2e1141fcdc578a047ae6");
        assertThat(ChungcheongFifthDownloadCases.selectCase("BUYEO").profile().selectProfileHash()).isEqualTo("80091b38b74952c8a14acb3d5d6df68b92848f2b789da09c0eb6b0b1a22f0090");
        assertThat(GyeongnamSecondDownloadCases.selectCase("GOSEONG").profile().selectProfileHash()).isEqualTo("f6c332adf2309602116b0bb6383d98fc756790498532efbb0db98f8705f5beaf");
    }
    @ParameterizedTest @ValueSource(strings={"YONGSAN","GEUMSAN","CHANGWON"})
    @EnabledIfEnvironmentVariable(named="SANEB_LEGACY_THREE_MIME_FIXTURE",matches="true")
    void measuredDownloadRequiresSignatureAndMatchingAttachmentName(String group) throws Exception {
        var sample=selectCase(group);Path root=Path.of("build/qa-"+group.toLowerCase(Locale.ROOT)+"-mime-20260930");
        String html=Files.readString(root.resolve("detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var discovery=sample.profile().selectDescriptors(sample.source(),html);assertThat(discovery.complete()).isTrue();
        assertThat(discovery.descriptors()).hasSize(group.equals("YONGSAN")?3:group.equals("GEUMSAN")?2:1);
        String headers=Files.readString(root.resolve("file-head.txt"),StandardCharsets.ISO_8859_1);
        String cd=headers.lines().filter(l->l.toLowerCase(Locale.ROOT).startsWith("content-disposition:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        String mime=headers.lines().filter(l->l.toLowerCase(Locale.ROOT).startsWith("content-type:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        byte[] bytes=Files.readAllBytes(root.resolve("file.bin"));String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(hash).isEqualTo(switch(group){case "YONGSAN"->"867ecac352614598a3ba7b602c352bcb12a56c43ff4c2894697049c15d731d22";case "GEUMSAN"->"d9622e86b6f694757cd46bfcb635661c0be2d670a47a6a5b0cb090bdd1d13519";default->"1c10a6a20697b7ec0969bb310ee4cba391eef4cddef02e3c36c31752a8fbf9d5";});
        String expected=group.equals("YONGSAN")?"HWP":"HWPX";
        assertThatThrownBy(()->new AttachmentFileTypeValidator().selectFormat(root.resolve("file.bin"),new Download(bytes.length,hash,mime,cd),expected)).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        var request=discovery.descriptors().getFirst().selectRequest();
        assertThat(selectFormat(group,request,bytes,mime,cd,expected)).isEqualTo(expected);
        assertThatThrownBy(()->selectFormat(group,request,bytes,mime,cd,"PDF")).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
    }
    @ParameterizedTest @ValueSource(strings={"YONGSAN","GEUMSAN","CHANGWON"})
    void incompatibleResponsesStayErrors(String group) throws Exception {
        boolean bare=group.equals("GEUMSAN");byte[] bytes=bare?ZIP:OLE;String ext=bare?"hwpx":"hwp",expected=ext.toUpperCase(Locale.ROOT);
        String mime=bare?"hwpx":"application/x-msdownload",good="attachment; filename=notice."+ext;var request=selectRequest(group);
        assertThat(selectFormat(group,request,bytes,mime,good,expected)).isEqualTo(expected);
        for(String bad:Arrays.asList(null,"inline; filename=notice."+ext,"attachment","attachment; filename=noextension","attachment; filename=../notice."+ext,"attachment; filename=\"a\u0085."+ext+"\""))
            assertThatThrownBy(()->selectFormat(group,request,bytes,mime,bad,expected)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThatThrownBy(()->selectFormat(group,request,bytes,mime,"attachment; filename=notice.exe",expected)).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        assertThatThrownBy(()->selectFormat(group,request,"<html>error</html>".getBytes(StandardCharsets.US_ASCII),mime,good,expected)).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
        for(String unapproved:List.of("text/html","application/unknown","application/x-tika-msoffice",bare?"application/x-msdownload":"hwpx"))
            assertThatThrownBy(()->selectFormat(group,request,bytes,unapproved,good,expected)).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        if(bare)assertThatThrownBy(()->selectFormat(group,request,OLE,mime,"attachment; filename=notice.hwp","HWP")).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
        assertThat(selectFormat(group,request,bytes,bare?"application/hwp+zip":"application/x-hwp",good,expected)).isEqualTo(expected);
    }
}
