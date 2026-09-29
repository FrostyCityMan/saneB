package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 완주·무주 공식 filebox의 고정 다운로드 호출을 읽는다. JavaScript를 실행하지 않는다. */
final class FileboxSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate;
    private final String hash;
    FileboxSaeolAttachmentDiscoveryProfile(String code,String sourceCode,String host){
        delegate=new SaeolGetAttachmentDiscoveryProfile(code,sourceCode,host,"SAFE_SAEOL_EMINWON","th",false);
        hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"+
                AttachmentProfileFingerprint.selectHash("FILEBOX_SAEOL:1|form1-post|tstyle|p.btn.btn-file|allfile|onclick-fixed-call|whole-box|same-uri-redirect",getClass()));
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
        if(!"form1".equals(form.attr("name"))||!"post".equalsIgnoreCase(form.attr("method")))return selectFailed();
        var tables=form.select("table.tstyle");var boxes=form.select("div.filebox");
        if(tables.size()!=1||boxes.size()!=1)return selectFailed();var table=tables.getFirst();
        var titles=table.select("th[scope=row]").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
        if(titles.size()!=1)return selectFailed();var title=titles.getFirst().nextElementSibling();
        if(title==null||!"td".equals(title.tagName())||!"3".equals(title.attr("colspan"))||!title.children().isEmpty()||title.text().isBlank())return selectFailed();
        var box=boxes.getFirst().clone();var labels=box.select(":root > p.btn.btn-file");var contents=box.select(":root > div.allfile");
        if(labels.size()!=1||contents.size()!=1||!labels.getFirst().children().isEmpty()||!"첨부파일".equals(labels.getFirst().text().strip()))return selectFailed();
        labels.getFirst().remove();
        for(var anchor:contents.getFirst().select("a")){
            if(!"#".equals(anchor.attr("href")))continue;
            var args=AttachmentDownloadInvocation.selectArguments(anchor.attr("onclick"),"goDownLoad",true,true);
            if(args.size()!=3)continue;
            String call="javascript:goDownLoad("+args.stream().map(s->"'"+s.replace("\\","\\\\").replace("'","\\'")+"'").collect(Collectors.joining(","))+")";
            anchor.attr("href",call).removeAttr("onclick");
        }
        // 공식 첨부 영역 전체를 보존한다. 미해석 링크·스크립트는 공통 파서의 부분 실패 근거로 남는다.
        var standard=new Element("form").attr("name","form1").attr("method","post");var row=standard.appendElement("table").appendElement("tr");
        row.appendElement("th").text("첨부파일");row.appendElement("td").html(box.html());
        return delegate.selectDescriptors(source,standard.outerHtml());
    }
    private Result selectFailed(){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
}
