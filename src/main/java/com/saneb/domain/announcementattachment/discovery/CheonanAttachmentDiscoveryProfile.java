package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.CheonanNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 공개 구형 form을 검증한 후 기존 새올 GET 엔진에 전체 첨부 영역을 전달한다. */
@Component
public final class CheonanAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate=new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_CHEONAN_GET_V1","LGS-000148",CheonanNoticePage.HOST,"SPRING_BBS","td",false);
    private final String hash=AttachmentProfileFingerprint.selectHash("CHEONAN:1|official-form-post|98percent-table|whole-attachment-container|unknown-role|same-uri|"
            +delegate.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",CheonanNoticePage.class)
            +"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return delegate.selectProviderCode();}
    @Override public String selectProfileCode(){return delegate.selectProfileCode();}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return delegate.selectSourceBindings();}
    @Override public Set<String> selectApprovedHosts(){return delegate.selectApprovedHosts();}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){return CheonanNoticePage.selectDetailUri(delegate.selectDetailUri(source));}
    @Override public boolean selectApprovedRequest(URI uri){return delegate.selectApprovedRequest(uri);}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return selectApprovedRequest(initial)&&selectApprovedRequest(next)&&initial.equals(next);}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(Source source,String html){
        var uri=selectDetailUri(source);
        if(html==null||html.length()>1_000_000)return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_DETAIL_UNAVAILABLE"));
        var page=Jsoup.parse(html,uri.toASCIIString());
        try{CheonanNoticePage.selectRoot(page);}catch(IllegalArgumentException e){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
        // 메모리 내 검증된 form의 이름만 변환한다. 원문·URL 식별자와 전체 첨부 내용은 바꾸지 않는다.
        page.selectFirst("form").attr("name","form1");
        return delegate.selectDescriptors(source,page.outerHtml());
    }
}
