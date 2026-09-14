package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class LegalBoardAttachmentDiscoveryProfileTest {
    static AttachmentDiscoveryProfile selectProfile(boolean busan) { return new LegalBoardAttachmentDiscoveryProfile(busan); }
    static AttachmentDiscoveryProfile.Source selectSource(boolean busan, String id) {
        String url = busan ? "https://www.busan.go.kr/nbgosi/view?sno=" + id + "&gosiGbn=A&curPage=1"
                : "https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId=" + id;
        var n = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                busan ? "LGS-000027" : "LGS-000010", "SPRING_BBS");
    }
    static String selectPage(boolean busan, String links) {
        return busan ? "<dl class='form-data-info'><dt><span>첨부파일</span></dt><dd><ul class='attfiles'>" + links + "</ul></dd><dt>조회수</dt><dd>1</dd></dl>"
                : "<dl class='file-lists'><dt>첨부</dt><dd class='item'>" + links + "</dd></dl>";
    }
    static String selectLink(boolean busan, String name, int sequence) {
        if (busan) return "<li><a href='/nbgosi/download?fileId=F2609111527472043&amp;seq=" + sequence + "' title='" + name + "'>" + name + "</a>"
                + "<a href='/nbgosi/download?seq=" + sequence + "&amp;fileId=F2609111527472043' class='btnTypeS btnColorType5' title='새창'>다운로드</a></li>";
        String format = name.substring(name.lastIndexOf('.') + 1);
        return "<a class='file' href='https://eminwon.gangbuk.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=" + name
                + "&amp;sys_file_nm=" + sequence + "." + format + "&amp;file_path=/ntishome/file/upload/ofr/ofr/20260911'>" + name + "</a><br>";
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void collectsAllFormatsDeduplicatesAndDoesNotInferRole(boolean busan) {
        var profile = selectProfile(busan);
        var result = profile.selectDescriptors(selectSource(busan,"12345"), selectPage(busan,
                selectLink(busan,"공고문.pdf",0)+selectLink(busan,"신청서.hwp",1)+selectLink(busan,"안내.hwpx",2)));
        assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
        assertThat(result.descriptors()).allSatisfy(d -> {
            assertThat(d.documentRole()).isEqualTo("UNKNOWN"); assertThat(d.downloadAllowed()).isTrue();
            assertThat(d.postForm()).isEmpty(); assertThat(profile.selectApprovedRequest(d.selectRequest())).isTrue();
            assertThat(d.locator().identifiers().get("attachmentId")).matches("[0-9a-f]{64}");
            assertThat(d.locator().identifiers()).containsEntry("noticeId","12345");
            assertThat(d.toString()).doesNotContain("공고문","신청서","ntishome");
        });
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void missingEmptyAndPartialAreNotConfused(boolean busan) {
        var p=selectProfile(busan);var s=selectSource(busan,"12345");
        assertThat(p.selectDescriptors(s,selectPage(busan,"")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s,"<html>로그인</html>").status()).isEqualTo("FAILED");
        assertThat(p.selectDescriptors(s,null).status()).isEqualTo("FAILED");
        assertThat(p.selectDescriptors(s,"x".repeat(1_000_001)).status()).isEqualTo("FAILED");
        String valid=selectLink(busan,"공고.pdf",0);
        for(String unknown:List.of("<a href='/other'>다른 첨부</a>","<button>파일</button>","<script>unknown()</script>",
                "<img src='/other'>","<span>누락.pdf</span>","<input type='hidden' value='other'>")) {
            var result=p.selectDescriptors(s,selectPage(busan,valid+unknown));
            assertThat(result.status()).isEqualTo("FAILED");assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);
        }
        assertThat(p.selectDescriptors(s,selectPage(busan,valid)+selectPage(busan,valid)).status()).isEqualTo("FAILED");
        assertThat(p.selectDescriptors(s,selectPage(busan,valid).replaceFirst("</dd>","</dd><dd>다른 첨부</dd>")).status()).isEqualTo("FAILED");
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void sourceIdentityParserAndProviderAreMandatory(boolean busan) {
        var p=selectProfile(busan);var s=selectSource(busan,"12345");
        for(var invalid:List.of(new AttachmentDiscoveryProfile.Source("BIZINFO",s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(s.providerCode(),"a".repeat(64),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),"HEURISTIC_NOTICE")))
            assertThatThrownBy(()->p.selectDetailUri(invalid)).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri("12345")).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDescriptors("12345","")).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(selectSource(!busan,"12345"))).hasMessage("PROFILE_REQUIRED");
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void exactOriginPathQueryAndGetOnly(boolean busan) {
        var p=selectProfile(busan);var s=selectSource(busan,"12345");
        var d=p.selectDescriptors(s,selectPage(busan,selectLink(busan,"공고.pdf",0))).descriptors().getFirst();
        for(String base:List.of(s.sourceUrl(),d.fetchUri().toASCIIString())) {
            for(String invalid:List.of(base.replace("https://","http://"),base.replace("https://","https://fixture@"),base+"#part",
                    base+"&unknown=x",base.replace(URI.create(base).getHost(),"127.0.0.1"),base.replace(URI.create(base).getHost(),"example.com"),
                    base.replace(URI.create(base).getHost(),URI.create(base).getHost()+":444"),base.replace("/view","/%76iew"),
                    base.replace("/download","/%64ownload"),base.replace("/FileDown","/%46ileDown"))) {
                if(!invalid.equals(base)) assertThat(p.selectApprovedRequest(URI.create(invalid))).as("허용되지 않은 요청 경계").isFalse();
            }
        }
        assertThat(p.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(d.fetchUri(),"POST",Map.of("field","value")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl()+ (busan?"&sno=1":"&nttId=1")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace("/view","/../view")))).isFalse();
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void unsupportedAndOverLimitFilesRemainVisible(boolean busan) {
        var p=selectProfile(busan);var s=selectSource(busan,"12345");
        var unsupported=p.selectDescriptors(s,selectPage(busan,selectLink(busan,"공고.pdf",0)+selectLink(busan,"참고.xlsx",1)));
        assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(2);
        assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();assertThat(unsupported.descriptors().getLast().expectedFormat()).isNull();
        String many=IntStream.range(0,11).mapToObj(i->selectLink(busan,"공고.pdf",i)).collect(java.util.stream.Collectors.joining());
        var limit=p.selectDescriptors(s,selectPage(busan,many));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.complete()).isFalse();assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void alternateHostsScriptsBaseAndConflictingDuplicateCannotBroadenDiscovery(boolean busan) {
        var p=selectProfile(busan);var s=selectSource(busan,"12345");String link=selectLink(busan,"공고.pdf",0);
        var result=p.selectDescriptors(s,"<base href='https://example.com'>"+selectPage(busan,link));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors().getFirst().fetchUri().getHost()).isEqualTo(busan?"www.busan.go.kr":"eminwon.gangbuk.go.kr");
        for(String altered:List.of(link.replace("<a ","<a onclick='go()' "),link.replace("href='","href='javascript:go()#"),
                link.replace("/nbgosi/download","https://example.com/nbgosi/download").replace("eminwon.gangbuk.go.kr","example.com"))) {
            assertThat(p.selectDescriptors(s,selectPage(busan,altered)).complete()).isFalse();
        }
        assertThat(p.selectDescriptors(s,selectPage(busan,link+selectLink(busan,"다른.pdf",0))).complete()).isFalse();
    }
    @Test void gangbukEncodedQueryAndTraversalCannotEscapeApprovedFileDirectory() {
        var p=selectProfile(false);var s=selectSource(false,"12345");String link=selectLink(false,"지원 (1) 한글.pdf",0);
        var file=p.selectDescriptors(s,selectPage(false,link)).descriptors().getFirst();
        assertThat(file.fetchUri().toASCIIString()).doesNotContain(" ","한글");assertThat(p.selectApprovedRequest(file.fetchUri())).isTrue();
        for(String altered:List.of(link.replace("0.pdf","../0.pdf"),link.replace("0.pdf","%252f0.pdf"),link.replace("0.pdf","0.exe"),
                link.replace("/ntishome/file/upload/ofr/ofr/20260911","/etc/private"))) {
            var r=p.selectDescriptors(s,selectPage(false,altered));
            assertThat(r.complete() && r.descriptors().stream().anyMatch(AttachmentDiscoveryProfile.Descriptor::downloadAllowed)).isFalse();
        }
    }
    @Test void registersNineDistinctProfilesWithBoundHashes() {
        try(var c=new AnnotationConfigApplicationContext(LegalBoardAttachmentProfileConfiguration.class,SaeolGetAttachmentProfileConfiguration.class,
                HwacheonPostAttachmentDiscoveryProfile.class,BizInfoAttachmentDiscoveryProfile.class,SeoguSaeolAttachmentDiscoveryProfile.class,AttachmentDiscoveryProfileRegistry.class)) {
            var registry=c.getBean(AttachmentDiscoveryProfileRegistry.class);assertThat(registry.selectProfileList()).hasSize(9);
            assertThat(registry.selectProfileList().stream().map(AttachmentDiscoveryProfile::selectProfileCode).distinct().count()).isEqualTo(9);
            registry.selectProfileList().forEach(p->assertThat(registry.selectProfileDetails(p.selectProviderCode(),p.selectProfileCode(),p.selectProfileHash())).contains(p));
            assertThat(selectProfile(true).selectProfileHash()).matches("[0-9a-f]{64}").isNotEqualTo(selectProfile(false).selectProfileHash());
            assertThat(selectProfile(true).selectUtf8DispositionOctets()).isTrue();assertThat(selectProfile(false).selectUtf8DispositionOctets()).isFalse();
        }
    }
}
