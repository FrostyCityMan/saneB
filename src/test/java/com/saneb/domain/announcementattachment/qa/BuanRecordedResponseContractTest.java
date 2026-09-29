package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 고정 공개 표본을 로컬에서 재현한다. 네트워크 요청과 운영 변경은 수행하지 않는다. */
class BuanRecordedResponseContractTest {
    @Test
    void fixedDownloadKeepsSameNoticeAndOriginalRequestBudget() {
        var sample=JeonbukSecondDownloadCases.selectCase("BUAN");
        var profile=sample.profile();
        var detail=Request.selectGet(profile.selectDetailUri(sample.source()));
        var file=Request.selectGet(URI.create("https://www.buan.go.kr/board/download.buan?boardId=BBS_0000054&menuCd=DOM_000000103001003000&paging=ok&startPage=1&dataSid=363827&command=update&fileSid=260981"));
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(profile);
        assertThat(budget.maximumRequests).isEqualTo(6);
        assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        budget.reserveBody();
        assertThat(budget.selectRequestAllowed(detail,detail)).isTrue();
        assertThat(budget.selectRequestAllowed(file,file)).isTrue();
        assertThat(budget.requests).isEqualTo(4);
        for(String changed:java.util.List.of(file.uri().toString().replace("fileSid=260981","fileSid=260982"),
                file.uri().toString().replace("dataSid=363827","dataSid=363828"),
                file.uri()+"&unexpected=1",file.uri().toString().replace("https://","http://")))
            assertThat(budget.selectRequestAllowed(file,Request.selectGet(URI.create(changed)))).isFalse();
        assertThat(budget.requests).isEqualTo(4);
        assertThat(budget.selectRequestAllowed(file,file)).isTrue();
        assertThat(budget.selectRequestAllowed(file,file)).isTrue();
        assertThat(budget.selectRequestAllowed(file,file)).isFalse();
        assertThat(budget.requests).isEqualTo(6);
    }

    @Test
    @EnabledIfEnvironmentVariable(named="SANEB_BUAN_RESPONSE_FIXTURE", matches="true")
    void recordedOfficialLinkAndBinaryRemainAcceptedWithoutExpandingPaths() throws Exception {
        var sample=JeonbukSecondDownloadCases.selectCase("BUAN");
        var root=Path.of("build/qa-buan-path-20260930");
        String html=Files.readString(root.resolve("detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse(html),sample.title(),sample.titleLayout());
        var result=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(1);
        var descriptor=result.descriptors().getFirst();
        assertThat(descriptor.expectedFormat()).isEqualTo("HWPX");
        assertThat(sample.profile().selectApprovedRequest(descriptor.selectRequest(),descriptor.selectRequest())).isTrue();
        var binary=root.resolve("file.bin");
        var headers=Files.readAllLines(root.resolve("file-head.txt"),StandardCharsets.ISO_8859_1);
        String mime=headers.stream().filter(s->s.regionMatches(true,0,"Content-Type:",0,13)).map(s->s.substring(13).strip()).findFirst().orElseThrow();
        String disposition=headers.stream().filter(s->s.regionMatches(true,0,"Content-Disposition:",0,20)).map(s->s.substring(20).strip()).findFirst().orElseThrow();
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(binary)));
        assertThat(hash).isEqualTo("2dec4ab835470c4097bb7f55897c55b2d4f6e4b1644f4f54c8c5f8ead807e461");
        assertThat(Files.size(binary)).isEqualTo(82795);
        assertThat(new AttachmentFileTypeValidator().selectFormat(binary,new Download(Files.size(binary),hash,mime,disposition),descriptor.expectedFormat())).isEqualTo("HWPX");
    }
}
