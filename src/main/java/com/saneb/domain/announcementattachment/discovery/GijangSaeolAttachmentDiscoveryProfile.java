package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 기장 공식 form/tb_board_read/attach_list 전체 칸을 기존 새올 GET 엔진에 연결한다. */
public final class GijangSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate=new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_GIJANG_GET_V1","LGS-000043","eminwon.gijang.go.kr","SAFE_SAEOL_EMINWON","th",false);
    private final String hash=new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"
            +AttachmentProfileFingerprint.selectHash("GIJANG:1|unique-form-post|tb_board_read|whole-attach_list|single-leading-angle-text|same-request-redirect",getClass()));
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
        if(forms.size()!=1||!"form".equals(forms.getFirst().attr("name"))||!"post".equalsIgnoreCase(forms.getFirst().attr("method")))return selectFailed();
        var tables=forms.getFirst().select("table.tb_board_read");var cells=forms.getFirst().select("td.attach_list");
        if(tables.size()!=1||cells.size()!=1)return selectFailed();
        var cell=cells.getFirst();var parent=cell.parent();
        if(cell.closest("table")!=tables.getFirst()||!"tr".equals(parent.tagName())||parent.childrenSize()!=1
                ||selectHasEvent(cell)||selectHasEvent(parent))return selectFailed();
        var content=cell.clone();
        // 실측3공고에서 링크 앞에 표시된 단일 '<' 텍스트만 제거한다. 임의 문자/태그/다른 링크는 보존한다.
        if(!content.select("a").isEmpty()&&content.childNodeSize()>0&&content.childNode(0) instanceof TextNode first
                &&"<".equals(first.getWholeText().strip()))first.remove();
        var standard=new Element("form").attr("name","form1").attr("method","post");
        var row=standard.appendElement("table").appendElement("tr");row.appendElement("th").text("첨부파일");row.appendElement("td").html(content.html());
        return delegate.selectDescriptors(source,standard.outerHtml());
    }
    private boolean selectHasEvent(Element element){return element.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on"));}
    private Result selectFailed(){return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED"));}
}
