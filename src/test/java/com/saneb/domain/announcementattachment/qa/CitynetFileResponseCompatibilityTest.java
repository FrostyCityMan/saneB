package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentProfileDownloadFlow;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ContentDisposition;

class CitynetFileResponseCompatibilityTest {
    @TempDir Path directory;
    private static final byte[] ZIP={'P','K',3,4,0,0,0,0};
    private static final byte[] OLE={(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};
    private AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase selectCase(String group) {
        return group.equals("INCHEON_CITY")?IncheonCityDownloadCases.selectCase():UlsanCityDownloadCases.selectCase();
    }
    private Request selectRequest(String group) {
        return Request.selectGet(URI.create("https://"+(group.equals("INCHEON_CITY")?"announce.incheon.go.kr":"minwon.ulsan.go.kr")
                +"/citynet/jsp/cmm/attach/download.jsp?mode=download&fid=%23"+"a".repeat(64)+"&index=1&other=%23"+"b".repeat(96)));
    }
    private Download selectResponse(String group,Request request,byte[] bytes,String mime,String disposition,String expected) throws Exception {
        var profile=selectCase(group).profile();Path output=directory.resolve(UUID.randomUUID()+".bin");int[] calls={0};
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        var response=AttachmentProfileDownloadFlow.selectDownload(profile,request,output,1024*1024,(next,limit,approved)->{
            calls[0]++;assertThat(next).isEqualTo(request);assertThat(approved.test(next)).isTrue();assertThat(limit).isEqualTo(1024*1024);
            Files.write(output,bytes);return new Download(bytes.length,hash,mime,disposition);
        });
        assertThat(calls[0]).isEqualTo(1);assertThat(response.bytes()).isEqualTo(bytes.length);assertThat(response.sha256()).isEqualTo(hash);
        assertThat(new AttachmentFileTypeValidator().selectFormat(output,response,expected,profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes())).isEqualTo(expected);
        return response;
    }
    private String selectEucKrHeader(String name) {
        return new String(("attachment; filename=\""+name+"\"").getBytes(Charset.forName("EUC-KR")),StandardCharsets.ISO_8859_1);
    }
    @ParameterizedTest @ValueSource(strings={"INCHEON_CITY","ULSAN_CITY"})
    @EnabledIfEnvironmentVariable(named="SANEB_CITYNET_FILE_RESPONSE_FIXTURE",matches="true")
    void measuredHeaderMatchesOfficialAttachmentNameAndBinary(String group) throws Exception {
        var sample=selectCase(group);Path root=Path.of("build/qa-"+group.toLowerCase(Locale.ROOT).replace('_','-')+"-mime-20260930");
        String html=Files.readString(root.resolve("detail.html"),group.equals("INCHEON_CITY")?Charset.forName("EUC-KR"):StandardCharsets.UTF_8);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var discovery=sample.profile().selectDescriptors(sample.source(),html);assertThat(discovery.complete()).isTrue();assertThat(discovery.descriptors()).hasSize(1);
        String headers=Files.readString(root.resolve("file-head.txt"),StandardCharsets.ISO_8859_1);
        String cd=headers.lines().filter(l->l.toLowerCase(Locale.ROOT).startsWith("content-disposition:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        String mime=headers.lines().filter(l->l.toLowerCase(Locale.ROOT).startsWith("content-type:")).map(l->l.substring(l.indexOf(':')+1).strip()).findFirst().orElseThrow();
        assertThat(mime).isEqualTo("application/file");byte[] bytes=Files.readAllBytes(root.resolve("file.bin"));String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThat(hash).isEqualTo(group.equals("INCHEON_CITY")?"220ba26656d6626505bcb896efacb01d682330feda18627dc80202f5ce102fa5":"11739be61c8f6aebe974c25ab3ffd84b2409fa3e50adb762310846bba92baa68");
        assertThatThrownBy(()->new AttachmentFileTypeValidator().selectFormat(root.resolve("file.bin"),new Download(bytes.length,hash,mime,cd),"HWPX")).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        var descriptor=discovery.descriptors().getFirst();var response=selectResponse(group,descriptor.selectRequest(),bytes,mime,cd,"HWPX");
        assertThat(response.contentType()).isEqualTo("application/x-msdownload");
        assertThat(ContentDisposition.parse(response.contentDisposition()).getFilename().equals(descriptor.displayName())).isTrue();
        assertThatThrownBy(()->selectResponse(group,descriptor.selectRequest(),bytes,mime,cd,"PDF")).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
    }
    @ParameterizedTest @ValueSource(strings={"INCHEON_CITY","ULSAN_CITY"})
    void onlyMeasuredMimeUsesStrictEucKrAndLegacyAttachmentValidation(String group) throws Exception {
        var request=selectRequest(group);String mime="application/file",good=selectEucKrHeader("소상공인 지원.hwpx");
        var response=selectResponse(group,request,ZIP,mime,good,"HWPX");
        assertThat(ContentDisposition.parse(response.contentDisposition()).getFilename()).isEqualTo("소상공인 지원.hwpx");
        assertThat(selectResponse(group,request,ZIP,mime,"attachment; filename=notice.hwpx","HWPX").contentType()).isEqualTo("application/x-msdownload");
        selectResponse(group,request,OLE,mime,selectEucKrHeader("지원.hwp"),"HWP");
        selectResponse(group,request,"%PDF-1.7".getBytes(StandardCharsets.US_ASCII),mime,selectEucKrHeader("지원.pdf"),"PDF");
        selectResponse(group,request,ZIP,mime,"attachment; filename*=UTF-8''%EC%A7%80%EC%9B%90.hwpx","HWPX");
        for(String bad:Arrays.asList(null,"inline; filename=notice.hwpx","attachment","attachment; filename=noextension","attachment; filename=../notice.hwpx",selectEucKrHeader("../지원.hwpx"),"attachment; filename=\"a\u00b0.hwpx\"","attachment; filename=\"a\r.hwpx\"", "attachment; filename=\"한글\u0085.hwpx\""))
            assertThatThrownBy(()->selectResponse(group,request,ZIP,mime,bad,"HWPX")).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThatThrownBy(()->selectResponse(group,request,ZIP,mime,"attachment; filename=notice.exe","HWPX")).hasMessage("ATTACHMENT_DISPOSITION_MISMATCH");
        assertThatThrownBy(()->selectResponse(group,request,"<html>error</html>".getBytes(StandardCharsets.US_ASCII),mime,good,"HWPX")).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
        for(String other:List.of("text/html","application/x-msdownload","application/unknown","hwpx"))
            assertThatThrownBy(()->selectResponse(group,request,ZIP,other,"attachment; filename=notice.hwpx","HWPX")).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        var plain=selectResponse(group,request,ZIP,"application/hwp+zip","attachment; filename=notice.hwpx","HWPX");assertThat(plain.contentType()).isEqualTo("application/hwp+zip");
    }
}
