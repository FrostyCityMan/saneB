package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 논산의 공식 첨부화일 칸만 공통 새올 GET 파서에 연결한다. */
final class NonsanAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate=new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_NONSAN_GET_V1","LGS-000153","eminwon.nonsan21.net","SAFE_SAEOL_EMINWON_COMPACT","th",false);
    private final String hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"+
            AttachmentProfileFingerprint.selectHash("NONSAN:1|form1-post|bbs_view|thead-title|첨부화일|whole-container|same-uri-redirect",getClass()));
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
        if(forms.size()!=1)return selectFailed();var tables=forms.getFirst().select("table.bbs_view");
        if(tables.size()!=1)return selectFailed();var table=tables.getFirst();
        var titles=table.select("thead > tr.head > th[scope=row]").stream()
                .filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().replaceAll("\\s+",""))).toList();
        if(titles.size()!=1)return selectFailed();var title=titles.getFirst().nextElementSibling();
        if(title==null||!"td".equals(title.tagName())||!"5".equals(title.attr("colspan"))||!title.children().isEmpty()||title.text().isBlank())return selectFailed();
        var labels=table.select("tbody > tr > th").stream()
                .filter(e->e.closest("table")==table&&e.children().isEmpty()&&"첨부화일".equals(e.text().strip())).toList();
        if(labels.size()!=1)return selectFailed();var label=labels.getFirst();var files=label.nextElementSibling();
        if(files==null||!"td".equals(files.tagName())||!"5".equals(files.attr("colspan")))return selectFailed();
        // 알려진 표기만 정규화한다. 링크·파일 수·내용은 삭제하거나 잘라내지 않는다.
        label.text("첨부파일");return delegate.selectDescriptors(source,page.outerHtml());
    }
    private Result selectFailed(){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
}
