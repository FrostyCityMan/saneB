package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 사하 공식 첨부 칸의 동일 onclick/onkeypress 호출만 읽으며 JavaScript는 실행하지 않는다. */
public final class SahaSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate=new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_SAHA_GET_V1","LGS-000037","eminwon.saha.go.kr","SAFE_SAEOL_EMINWON","th",true);
    private final String hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"
            +AttachmentProfileFingerprint.selectHash("SAHA:1|form1-post|board_read|th-scope-col|td-colspan3|equal-click-keypress-bare-call|whole-container|https-upgrade|same-request-redirect",getClass()));
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
        if(forms.size()!=1)return selectFailed();var tables=forms.getFirst().select("table.board_read");
        if(tables.size()!=1)return selectFailed();var table=tables.getFirst();
        var labels=table.select("th[scope=col]").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"첨부파일".equals(e.text().strip())).toList();
        if(labels.size()!=1)return selectFailed();var container=labels.getFirst().nextElementSibling();
        if(container==null||!"td".equals(container.tagName())||!"3".equals(container.attr("colspan"))||container.nextElementSibling()!=null)return selectFailed();
        var selected=container.clone();
        for(var anchor:selected.select("a")){
            String invocation=anchor.attr("onclick").strip();
            if(!"#".equals(anchor.attr("href"))||!invocation.equals(anchor.attr("onkeypress").strip())
                    ||!anchor.attributes().asList().stream().allMatch(a->Set.of("href","onclick","onkeypress").contains(a.getKey()))
                    ||AttachmentDownloadInvocation.selectArguments(invocation,"goDownLoad",true,false).size()!=3)return selectFailed();
            anchor.attr("href","javascript:"+invocation).removeAttr("onclick").removeAttr("onkeypress");
        }
        // 전체 칸을 보존하여 미지 링크/버튼/남은 텍스트/미지원 파일을 숨기지 않는다.
        var standard=new org.jsoup.nodes.Element("form").attr("name","form1").attr("method","post");
        var row=standard.appendElement("table").appendElement("tr");row.appendElement("th").text("첨부파일");row.appendChild(selected);
        return delegate.selectDescriptors(source,standard.outerHtml());
    }
    private Result selectFailed(){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
}
