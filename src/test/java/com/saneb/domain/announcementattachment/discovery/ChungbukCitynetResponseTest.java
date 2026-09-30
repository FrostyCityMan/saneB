package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.Site;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class ChungbukCitynetResponseTest {
    @TempDir Path temporary;
    private final AttachmentDiscoveryProfile profile=new ChungcheongThirdAttachmentProfileConfiguration().selectChungbukProfileDetails();
    private Request selectRequest(){return Request.selectGet(URI.create("https://sido.chungbuk.go.kr/citynet/jsp/cmm/attach/download.jsp?mode=download&fid=%23"+"a".repeat(64)+"&index=1&other=%23"+"b".repeat(96)));}
    private Path selectBinary(byte[] bytes)throws Exception {return Files.write(temporary.resolve("file.bin"),bytes);}
    private byte[] selectHwp(){return new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};}
    @Test void measuredFileMimeRequiresSignatureAndAttachmentNameInOneTransport()throws Exception {
        var file=selectBinary(selectHwp());var original=new Download(8,"a".repeat(64),"application/file","attachment; filename=notice.hwp");
        var validator=new AttachmentFileTypeValidator();
        assertThatThrownBy(()->validator.selectFormat(file,original,"HWP")).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        var count=new AtomicInteger();
        var normalized=((AttachmentDownloadFlowProfile)profile).selectDownload(selectRequest(),file,1024,(request,limit)->{
            assertThat(request).isEqualTo(selectRequest());assertThat(limit).isEqualTo(1024);count.incrementAndGet();return original;
        });
        assertThat(count).hasValue(1);assertThat(normalized.bytes()).isEqualTo(original.bytes());assertThat(normalized.sha256()).isEqualTo(original.sha256());
        assertThat(validator.selectFormat(file,normalized,"HWP",false,profile.selectLegacyBinaryContentTypes())).isEqualTo("HWP");
    }
    @Test void invalidBodiesMimeHeadersAndDestinationsRemainBlocked()throws Exception {
        var flow=(AttachmentDownloadFlowProfile)profile;var file=selectBinary(selectHwp());
        for(String header:java.util.Arrays.asList(null,"inline; filename=notice.hwp","attachment; filename=payload.exe")) {
            assertThatThrownBy(()->flow.selectDownload(selectRequest(),file,1024,(r,l)->new Download(8,"a".repeat(64),"application/file",header))).isInstanceOf(java.io.IOException.class);
        }
        assertThatThrownBy(()->flow.selectDownload(selectRequest(),file,1024,(r,l)->new Download(8,"a".repeat(64),"application/x-msdownload","attachment; filename=notice.hwp"))).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
        for(byte[] bytes:java.util.List.of("<html>error</html>".getBytes(),new byte[]{'M','Z',0,0,0,0,0,0})) {
            var bad=selectBinary(bytes);
            assertThatThrownBy(()->flow.selectDownload(selectRequest(),bad,1024,(r,l)->new Download(bytes.length,"a".repeat(64),"application/file","attachment; filename=notice.hwp"))).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
        }
        var calls=new AtomicInteger();
        assertThatThrownBy(()->flow.selectDownload(Request.selectGet(URI.create("https://other.invalid/file")),file,1024,(r,l)->{calls.incrementAndGet();return null;})).hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
        assertThat(calls).hasValue(0);
    }
    @Test void gongjuAndGlobalValidatorAreNotOptedIn() {
        var gongju=new ChungcheongThirdAttachmentProfileConfiguration().selectGongjuProfileDetails();
        assertThat(gongju.selectProfileHash()).isEqualTo("5dba7cc4fb710cf9070db27442cb558c41dde92459689a44654e99357ad496fd");
        assertThat(gongju.selectLegacyBinaryContentTypes()).isEmpty();assertThat(gongju).isNotInstanceOf(AttachmentDownloadFlowProfile.class);
        assertThatThrownBy(()->new ChungbukCitynetResponseAttachmentProfile(new ChungcheongThirdAttachmentDiscoveryProfile(Site.GONGJU))).hasMessage("CHUNGBUK_PROFILE_REQUIRED");
        assertThat(profile.selectProfileHash()).isNotEqualTo("a03ff4f3c7c294842bf51e948ba8a632dcc3159678f1820d68cccf4684e00683");
    }
}
