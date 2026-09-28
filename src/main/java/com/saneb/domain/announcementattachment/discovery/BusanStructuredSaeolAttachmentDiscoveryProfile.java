package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 실측한 수영 정의 목록과 사상 인라인 첨부 칸만 새올 GET 엔진에 연결한다. */
public final class BusanStructuredSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    public enum Layout { SUYEONG, SASANG }
    private final Layout layout;
    private final SaeolGetAttachmentDiscoveryProfile delegate;
    private final String hash;

    public BusanStructuredSaeolAttachmentDiscoveryProfile(Layout layout) {
        this.layout=java.util.Objects.requireNonNull(layout);
        boolean suyeong=layout==Layout.SUYEONG;
        delegate=new SaeolGetAttachmentDiscoveryProfile("LOCAL_"+layout+"_GET_V1",
                suyeong?"LGS-000041":"LGS-000042",suyeong?"eminwon.suyeong.go.kr":"eminwon.sasang.go.kr",
                suyeong?"SAFE_SAEOL_EMINWON_COMPACT":"SAFE_SAEOL_EMINWON","th",false);
        hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"
                +AttachmentProfileFingerprint.selectHash("BUSAN_STRUCTURED:1|"+layout+"|unique-form1-post|whole-container|same-request-redirect",getClass()));
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
        if(forms.size()!=1||!"form1".equals(forms.getFirst().attr("name"))||!"post".equalsIgnoreCase(forms.getFirst().attr("method")))return selectFailed();
        var form=forms.getFirst();Element container;
        if(layout==Layout.SUYEONG){
            var views=form.select("div.view01");
            var labels=form.select("dt").stream().filter(e->selectLabel(e)).toList();
            if(views.size()!=1||labels.size()!=1)return selectFailed();
            var label=labels.getFirst();var dl=label.parent();container=label.nextElementSibling();
            if(!label.children().isEmpty()||!"dl".equals(dl.tagName())||dl.parent()!=views.getFirst()
                    ||!dl.ownText().isBlank()||dl.childrenSize()!=2||dl.child(0)!=label||container==null||!"dd".equals(container.tagName())
                    ||selectHasEvent(container)||selectHasEvent(dl)||selectHasEvent(label))return selectFailed();
            container=container.clone();
        }else{
            var tables=form.select("table.basic");
            var labels=form.select("strong").stream().filter(e->selectLabel(e)).toList();
            if(tables.size()!=1||labels.size()!=1)return selectFailed();
            var label=labels.getFirst();container=label.parent();var row=container.parent();
            if(!label.children().isEmpty()||!"td".equals(container.tagName())||!"2".equals(container.attr("colspan"))
                    ||container.closest("table")!=tables.getFirst()||container.child(0)!=label
                    ||!"tr".equals(row.tagName())||row.childrenSize()!=1||selectHasEvent(container)||selectHasEvent(row)||selectHasEvent(label))return selectFailed();
            container=container.clone();container.child(0).remove();
        }
        // 공식 첨부 칸 전체를 유지한다. 미지원 링크/파일을 제거하거나 정상 파일로 바꾸지 않는다.
        var standard=new Element("form").attr("name","form1").attr("method","post");
        var row=standard.appendElement("table").appendElement("tr");row.appendElement("th").text("첨부파일");
        row.appendElement("td").html(container.html());
        return delegate.selectDescriptors(source,standard.outerHtml());
    }
    private boolean selectLabel(Element element){return "첨부파일".equals(element.text().replaceAll("[\\s\\u00a0:：]+",""));}
    private boolean selectHasEvent(Element element){return element.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on"));}
    private Result selectFailed(){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
}
