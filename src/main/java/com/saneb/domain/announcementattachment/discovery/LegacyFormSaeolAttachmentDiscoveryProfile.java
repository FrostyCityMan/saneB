package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 실측한 구형 새올 제목 표를 검증한 뒤 전체 첨부 칸을 공통 GET 파서로 전달한다. */
final class LegacyFormSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate;
    private final String formName, tableWidth, hash;
    LegacyFormSaeolAttachmentDiscoveryProfile(String code,String sourceCode,String host,String formName,String tableWidth) {
        this.delegate=new SaeolGetAttachmentDiscoveryProfile(code,sourceCode,host,"SAFE_SAEOL_EMINWON","td",false);
        this.formName=formName;this.tableWidth=tableWidth;
        this.hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash(
                "LEGACY_FORM_SAEOL:1|"+formName+"|"+tableWidth+"|title-td|whole-container|same-uri-redirect",getClass()));
    }
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
        var page=Jsoup.parse(html,uri.toASCIIString());var forms=page.select("form");
        if(forms.size()!=1)return selectFailed();var form=forms.getFirst();
        if(!formName.equals(form.attr("name"))||!"post".equalsIgnoreCase(form.attr("method")))return selectFailed();
        var tables=form.select("table[width='"+tableWidth+"'][border=0][cellspacing=1][cellpadding=0]");
        if(tables.size()!=1)return selectFailed();var table=tables.getFirst();
        var titles=table.select("td").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
        if(titles.size()!=1)return selectFailed();var title=titles.getFirst().nextElementSibling();
        if(title==null||!"td".equals(title.tagName())||!title.children().isEmpty()||title.text().isBlank())return selectFailed();
        form.attr("name","form1");
        return delegate.selectDescriptors(source,page.outerHtml());
    }
    private Result selectFailed(){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
}
