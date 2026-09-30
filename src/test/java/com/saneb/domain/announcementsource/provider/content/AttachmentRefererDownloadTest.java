package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentRefererDownloadTest {
    @TempDir Path root;
    private final AttachmentDiscoveryProfile profile = new GyeongbukDirectAttachmentProfileConfiguration().selectYeongdeokProfileDetails();
    private final Request initial = Request.selectGet(URI.create("https://www.yd.go.kr/?action=kboard_file_download&uid=366709&file=file1"));
    private final URI detail = URI.create("https://www.yd.go.kr/?page_id=763&uid=366709&mod=document");
    private final URI binary = URI.create("https://www.yd.go.kr/wp-content/uploads/kboard_temp/6abd13adcc8a5/2026%EB%85%84-%EC%A7%80%EC%9B%90-%EA%B3%B5%EA%B3%A0.hwp");

    @Test void publicRefererSurvivesOneApprovedRedirectAndCannotLeakInToString() throws Exception {
        validatePublicRedirect(binary.toASCIIString());
    }

    @Test void rawUtf8LocationOctetsUseTheSameRestrictedOfficialRedirect() throws Exception {
        String unicode = "https://www.yd.go.kr" + binary.getPath();
        validatePublicRedirect(new String(unicode.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
    }

    private void validatePublicRedirect(String location) throws Exception {
        var requests = new ArrayList<Request>();
        try (var client = new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(
                host -> new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})}),
                (target, request, remaining, handler) -> {
                    requests.add(request);
                    assertThat(request.referer()).isEqualTo(detail);
                    if (requests.size() == 1) return handler.handle(new AttachmentPinnedDownloadClient.Response(
                            302, location, 0, "text/html", null, new ByteArrayInputStream(new byte[0])));
                    return handler.handle(new AttachmentPinnedDownloadClient.Response(200, null, 3,
                            "application/octet-stream", null, new ByteArrayInputStream(new byte[]{1,2,3})));
                }, Duration.ofSeconds(2))) {
            var result = AttachmentProfileDownloadFlow.selectDownload(profile, initial, root.resolve("file.bin"), 1024,
                    (request, limit, allowed) -> client.selectDownload(request, profile.selectApprovedHosts(), allowed,
                            root.resolve("file.bin"), limit, bytes -> true));
            assertThat(result.bytes()).isEqualTo(3);
            assertThat(requests).hasSize(2);
            assertThat(requests.getLast().uri().toASCIIString()).isEqualTo(binary.toASCIIString());
            assertThat(requests.getFirst().toString()).doesNotContain("366709", "www.yd.go.kr", "page_id");
        }
        // 전송 계약 테스트이며 합성 3byte를 실제 문서 형식 성공으로 취급하지 않는다.
        assertThat(Files.readAllBytes(root.resolve("file.bin"))).containsExactly((byte)1,(byte)2,(byte)3);
    }

    @Test void changedNoticeOrNonPublicRedirectStopsBeforeNextHttp() throws Exception {
        for (String redirect : List.of("https://www.yd.go.kr/?page_id=763&uid=366710&mod=document",
                "https://www.yd.go.kr/wp-login.php", "https://www.yd.go.kr/wp-content/uploads/private/a.hwp",
                "https://evil.example/notice.hwp", binary + "?token=synthetic", binary.resolve("../notice.hwp").toString(),
                binary.resolve("%2e%2e%2fnotice.hwp").toString(), binary.toString().replace(".hwp", ".exe"))) {
            var calls = new AtomicInteger();
            try (var client = new AttachmentPinnedDownloadClient(new ProviderContentUrlValidator(
                    host -> new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})}),
                    (target, request, remaining, handler) -> {
                        calls.incrementAndGet();
                        return handler.handle(new AttachmentPinnedDownloadClient.Response(302, redirect, 0,
                                "text/html", null, new ByteArrayInputStream(new byte[0])));
                    }, Duration.ofSeconds(2))) {
                assertThatThrownBy(() -> AttachmentProfileDownloadFlow.selectDownload(profile, initial, root.resolve("blocked.bin"), 1024,
                        (request, limit, allowed) -> client.selectDownload(request, profile.selectApprovedHosts(), allowed,
                                root.resolve("blocked.bin"), limit, bytes -> true))).isInstanceOf(java.io.IOException.class);
            }
            assertThat(calls.get()).isEqualTo(1);assertThat(root.resolve("blocked.bin")).doesNotExist();
        }
    }

    @Test void refererIsSameOriginGetOnlyAndNeverAFreeHeaderMap() {
        for (String bad : List.of("http://www.yd.go.kr/", "https://evil.example/", "https://user@www.yd.go.kr/",
                "https://www.yd.go.kr/#x", "https://www.yd.go.kr:444/", "https://www.yd.go.kr/../x", "https://www.yd.go.kr/?x=%0a"))
            assertThatThrownBy(() -> new Request(initial.uri(), "GET", Map.of(), URI.create(bad)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("ATTACHMENT_REFERER_INVALID");
        assertThatThrownBy(() -> new Request(initial.uri(), "POST", Map.of("x","y"), detail))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("ATTACHMENT_REFERER_INVALID");
        var other = URI.create(detail.toString().replace("366709","366710"));
        assertThat(profile.selectApprovedRequest(initial, new Request(initial.uri(),"GET",Map.of(),other,true))).isFalse();
        assertThat(profile.selectApprovedRequest(initial, new Request(binary,"GET",Map.of(),other,true))).isFalse();
        assertThat(profile.selectApprovedRequest(initial, Request.selectGet(binary))).isFalse();
        assertThat(profile.selectApprovedRequest(initial, new Request(binary,"GET",Map.of(),detail,true))).isTrue();
        assertThat(profile.selectApprovedRequest(initial, new Request(binary,"GET",Map.of(),detail))).isFalse();
        var seocheon = new SeocheonAttachmentDiscoveryProfile();
        var original = URI.create("https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=27490&subCheck=Y");
        assertThat(seocheon.selectApprovedRequest(new Request(original,"GET",Map.of(),original))).isFalse();
        assertThat(new Request(initial.uri(),"GET",Map.of()).referer()).isNull();
        assertThat(new Request(initial.uri(),"GET",Map.of()).utf8RedirectOctets()).isFalse();
    }

    @Test void redirectHeaderCompatibilityIsOptInStrictAndNeverDecodesPercentEscapes() throws Exception {
        String raw = new String("https://www.yd.go.kr/공고.hwp".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        assertThat(AttachmentPinnedDownloadClient.selectRedirectLocation(raw,false)).isEqualTo(raw);
        assertThat(AttachmentPinnedDownloadClient.selectRedirectLocation(raw,true)).isEqualTo("https://www.yd.go.kr/공고.hwp");
        assertThat(AttachmentPinnedDownloadClient.selectRedirectLocation(binary.toASCIIString(),true)).isEqualTo(binary.toASCIIString());
        for (String invalid : List.of("https://www.yd.go.kr/\u00ff.hwp", "https://www.yd.go.kr/공고.hwp",
                "https://www.yd.go.kr/\u00c2\u0085.hwp", "x".repeat(4097)+"\u00ff"))
            assertThatThrownBy(() -> AttachmentPinnedDownloadClient.selectRedirectLocation(invalid,true))
                    .isInstanceOf(java.io.IOException.class).hasMessage("ATTACHMENT_REDIRECT_ENCODING_INVALID");
        assertThatThrownBy(() -> new Request(initial.uri(),"GET",Map.of(),null,true))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("ATTACHMENT_REDIRECT_ENCODING_INVALID");
    }
}
