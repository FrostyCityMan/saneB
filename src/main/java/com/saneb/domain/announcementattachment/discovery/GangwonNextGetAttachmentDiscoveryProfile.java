package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 동해·정선·강원 고성의 공식 첨부 경계를 공통 새올 GET 검증기에 연결한다. */
final class GangwonNextGetAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { DONGHAE, JEONGSEON, GW_GOSEONG }
    private final Site site;
    private final SaeolGetAttachmentDiscoveryProfile delegate;
    private final String hash;
    GangwonNextGetAttachmentDiscoveryProfile(Site site) {
        this.site = java.util.Objects.requireNonNull(site);
        String code = switch(site) { case DONGHAE -> "LGS-000120"; case JEONGSEON -> "LGS-000128"; default -> "LGS-000133"; };
        String host = switch(site) { case DONGHAE -> "eminwon.dh.go.kr"; case JEONGSEON -> "eminwon.jeongseon.go.kr"; default -> "eminwon.gwgs.go.kr"; };
        delegate = new SaeolGetAttachmentDiscoveryProfile("LOCAL_"+site+"_GET_V1",code,host,
                site==Site.DONGHAE ? "SAFE_SAEOL_EMINWON_CELL" : "SAFE_SAEOL_EMINWON","td",false);
        hash = AttachmentProfileFingerprint.selectHash(delegate.selectProfileHash()+"|1|"+site+"|official-boundary|numbered-rows|direct-text-marker|same-request",getClass());
    }
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public boolean selectApprovedRequest(URI uri) { return delegate.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(Request initial, Request next) { return initial!=null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(String id, String html) { return delegate.selectDescriptors(id,html); }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail=selectDetailUri(source);
        if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());var forms=page.select("form");
        if(forms.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");var form=forms.getFirst();
        if(!(site==Site.DONGHAE?"form":"form1").equals(form.attr("name"))||!"post".equalsIgnoreCase(form.attr("method")))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if(site==Site.DONGHAE){
            if(form.select("table[width=98%][cellspacing=1][cellpadding=0]").size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
            return delegate.selectDescriptors(source,form.clone().attr("name","form1").outerHtml());
        }
        var container=new Element("div");boolean incomplete=false;
        if(site==Site.JEONGSEON){
            var roots=form.select("div.skinTb.eminwon");var bodies=form.select("div.skinTb.eminwon > div.skinTb-tr > div.skinTb-conts");
            if(roots.size()!=1||bodies.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
            var body=bodies.getFirst();var markers=body.childNodes().stream().filter(n->n instanceof TextNode t&&"첨부파일".equals(t.getWholeText().strip())).toList();
            if(markers.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");var marker=markers.getFirst();
            if(!(marker.previousSibling() instanceof Element before)||!"br".equals(before.tagName())||!(marker.nextSibling() instanceof Element after)||!"br".equals(after.tagName()))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
            // 본문 전체 링크를 첨부로 보지 않는다. 공식 직접 텍스트 표식 이후의 모든 노드를 보존한다.
            for(int i=marker.siblingIndex()+1;i<body.childNodeSize();i++)container.appendChild(body.childNode(i).clone());
        }else{
            var tables=form.select("table.tb_style1");if(tables.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");var table=tables.getFirst();
            var labels=table.select("th").stream().filter(e->e.closest("table")==table&&e.text().strip().startsWith("첨부파일")).toList();
            if(labels.isEmpty())return selectFailed("ATTACHMENT_SELECTOR_CHANGED");int expected=1;
            for(var label:labels){
                var cell=label.nextElementSibling();
                if(!("첨부파일"+expected++).equals(label.text().strip())||!label.children().isEmpty())incomplete=true;
                if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null||label.parent().childrenSize()!=2){incomplete=true;continue;}
                container.appendChild(cell.clone().tagName("div"));
            }
        }
        var standard=new Element("form").attr("name","form1").attr("method","post");var row=standard.appendElement("table").appendElement("tr");row.appendElement("td").text("첨부파일");row.appendElement("td").html(container.html());
        var result=delegate.selectDescriptors(source,standard.outerHtml());
        return incomplete ? new Result(result.status().equals("LIMIT_EXCEEDED")?result.status():"FAILED",false,result.descriptors(),List.of("ATTACHMENT_SELECTOR_CHANGED")) : result;
    }
    private Result selectFailed(String code) { return new Result("FAILED",false,List.of(),List.of(code)); }
}
