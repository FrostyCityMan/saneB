package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusanLegalQueryContractTest {
    private final AttachmentDiscoveryProfile profile=new LegalBoardAttachmentProfileConfiguration().selectBusanLegalProfileDetails();
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private static final String DETAIL="https://www.busan.go.kr/nbgosi/view?sno=79622&gosiGbn=A&curPage=1";
    private static final String SEARCH="&conIfmStdt=2026-04-01&conIfmEnddt=2026-10-01&conGosiGbn=A&schKeyType=A&srchText=%EC%86%8C%EC%83%81%EA%B3%B5%EC%9D%B8";

    @Test void officialDefaultAndSearchContextAreAcceptedWithoutChangingSourceIdentity() {
        for(String url:List.of(DETAIL+"&conGosiGbn=",DETAIL+SEARCH)) {
            var source=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000027","SPRING_BBS");
            assertThat(profile.selectDetailUri(source)).isEqualTo(URI.create(url));
            assertThat(profile.selectApprovedRequest(Request.selectGet(URI.create(url)))).isTrue();
            // 실제 목록 수집기는 canonical URL 자체와 그 바이트의 hash를 저장한다. 재해싱으로 ID를 바꾸지 않는다.
            String storedUrl=normalizer.canonicalizeUrl(url),storedId=normalizer.hash(storedUrl);
            var stored=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",storedId,storedUrl,"LGS-000027","SPRING_BBS");
            assertThat(profile.selectDetailUri(stored)).isEqualTo(URI.create(storedUrl));
            assertThat(stored.providerNoticeId()).isEqualTo(storedId);
            assertThatThrownBy(()->profile.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","a".repeat(64),storedUrl,"LGS-000027","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        }
    }

    @Test void contextCannotBroadenPathsMethodsOrInjectUnknownOrInvalidParameters() {
        for(String suffix:List.of("&unknown=x","&sno=1","&conGosiGbn=A&conGosiGbn=N","&conGosiGbn=X","&schKeyType=Q",
                "&conIfmStdt=2026-02-30","&conIfmEnddt=2026-13-01","&srchText=%00","&srchText=%0d%0aInjected",
                "&srchText=%C3%28","&srchText=a=b","&srchText="+"x".repeat(257),"&srchText=x&srchText=y","&redirect=https://example.com"))
            assertThat(profile.selectApprovedRequest(URI.create(DETAIL+suffix))).as(suffix).isFalse();
        for(String url:List.of(DETAIL.replace("https:","http:"),DETAIL.replace("www.busan.go.kr","evil.example"),DETAIL.replace("/view","/%76iew"),DETAIL+"#x"))
            assertThat(profile.selectApprovedRequest(URI.create(url+SEARCH))).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(URI.create(DETAIL+SEARCH),"POST",java.util.Map.of("sno","79622")))).isFalse();
    }

    @Test void redirectsKeepNoticeOrFileIdentityInsteadOfOnlyMatchingHost() {
        var initial=Request.selectGet(URI.create(DETAIL+SEARCH));
        assertThat(profile.selectApprovedRequest(initial,Request.selectGet(URI.create(DETAIL)))).isTrue();
        assertThat(profile.selectApprovedRequest(initial,Request.selectGet(URI.create(DETAIL.replace("79622","79644"))))).isFalse();
        assertThat(profile.selectApprovedRequest(initial,Request.selectGet(URI.create(DETAIL.replace("gosiGbn=A","gosiGbn=N"))))).isFalse();
        var file=Request.selectGet(URI.create("https://www.busan.go.kr/nbgosi/download?fileId=F26091517382020184&seq=0"));
        assertThat(profile.selectApprovedRequest(file,Request.selectGet(URI.create("https://www.busan.go.kr/nbgosi/download?seq=0&fileId=F26091517382020184")))).isTrue();
        assertThat(profile.selectApprovedRequest(file,Request.selectGet(URI.create(file.uri().toString().replace("seq=0","seq=1"))))).isFalse();
        assertThat(profile.selectApprovedRequest(initial,file)).isFalse();
        assertThat(profile.selectApprovedRequest(file,initial)).isFalse();
        assertThat(profile.selectApprovedRequest(file,Request.selectGet(URI.create(file.uri()+"&conGosiGbn=A")))).isFalse();
    }
}
