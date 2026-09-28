package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 동래의 실측 form2만 내부 표준 구조로 연결한다. 기존 지역의 form1 계약은 변경하지 않는다. */
public final class DongnaeSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate=new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_DONGNAE_GET_V1","LGS-000033","eminwon.dongnae.go.kr","SAFE_SAEOL_EMINWON","td",false);
    private final String hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"
            +AttachmentProfileFingerprint.selectHash("DONGNAE:1|form2-post|unique-tb_t1|same-request-redirect",getClass()));
    @Override public String selectProviderCode(){return delegate.selectProviderCode();}
    @Override public String selectProfileCode(){return delegate.selectProfileCode();}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return delegate.selectSourceBindings();}
    @Override public Set<String> selectApprovedHosts(){return delegate.selectApprovedHosts();}
    @Override public URI selectDetailUri(String id){return delegate.selectDetailUri(id);}
    @Override public URI selectDetailUri(Source source){return delegate.selectDetailUri(source);}
    @Override public boolean selectApprovedRequest(URI uri){return delegate.selectApprovedRequest(uri);}
    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request next){
        return selectApprovedRequest(initial)&&selectApprovedRequest(next)&&initial.uri().equals(next.uri());
    }
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(Source source,String html){
        URI uri=selectDetailUri(source);
        if(html==null||html.length()>1_000_000)return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_DETAIL_UNAVAILABLE"));
        var page=Jsoup.parse(html,uri.toASCIIString());
        var forms=page.select("form");
        if(forms.size()!=1||!"form2".equals(forms.getFirst().attr("name"))||!"post".equalsIgnoreCase(forms.getFirst().attr("method"))
                ||forms.getFirst().select("table.tb_t1").size()!=1)
            return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));
        // 원문/저장 데이터를 바꾸지 않는다. 고정 구조를 확인한 임시 DOM만 기존 파서에 전달한다.
        forms.getFirst().attr("name","form1");
        return delegate.selectDescriptors(source,page.outerHtml());
    }
}
