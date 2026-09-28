package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 부산 강서 공식 첨부 목록만 GET 엔진에 연결한다. 별도 문서 뷰어 폼은 실행하지 않는다. */
public final class BusanGangseoAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate=new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_BUSAN_GANGSEO_GET_V1","LGS-000039","eminwon.bsgangseo.go.kr","SAFE_SAEOL_EMINWON_COMPACT","th",false);
    private final String hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"
            +AttachmentProfileFingerprint.selectHash("BUSAN_GANGSEO:1|form1-post|unique-board-b_view-view_file|viewer-not-executed|whole-list|same-request-redirect",getClass()));
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
        var page=Jsoup.parse(html,uri.toASCIIString());var forms=page.select("form[name=form1][method=post]");
        if(forms.size()!=1)return selectFailed();
        var views=forms.getFirst().select("div.board > div.b_view");var lists=forms.getFirst().select("ul.view_file");
        if(views.size()!=1||lists.size()!=1||lists.getFirst().parent()!=views.getFirst())return selectFailed();
        var list=lists.getFirst();
        if(list.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on")))return selectFailed();
        var standard=new Element("form").attr("name","form1").attr("method","post");
        var row=standard.appendElement("table").appendElement("tr");row.appendElement("th").text("첨부파일");row.appendElement("td").appendChild(list.clone());
        return delegate.selectDescriptors(source,standard.outerHtml());
    }
    private Result selectFailed(){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
}
