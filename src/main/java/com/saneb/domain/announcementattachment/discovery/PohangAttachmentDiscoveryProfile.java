package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.PohangNoticePage;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

/** 포항의 숫자 미리보기 순번만 정규화하고 다운로드는 검증된 포털 엔진에 위임한다. */
public final class PohangAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final GyeongbukPortalAttachmentDiscoveryProfile delegate=new GyeongbukPortalAttachmentDiscoveryProfile("POHANG","LGS-000201","SAEOL_GOSI","pohang.go.kr","mid","0202010000",false,true);
    private final String hash=AttachmentProfileFingerprint.selectHash("POHANG:1|paired-numeric-preview-only|no-preview-request|"+delegate.selectProfileHash()
            +"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",PohangNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)
            +"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    @Override public String selectProviderCode(){return delegate.selectProviderCode();}
    @Override public String selectProfileCode(){return delegate.selectProfileCode();}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return delegate.selectSourceBindings();}
    @Override public Set<String> selectApprovedHosts(){return delegate.selectApprovedHosts();}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){return delegate.selectDetailUri(source);}
    @Override public boolean selectApprovedRequest(URI uri){return delegate.selectApprovedRequest(uri);}
    @Override public boolean selectApprovedRequest(Request request){return delegate.selectApprovedRequest(request);}
    @Override public boolean selectApprovedRequest(Request first,Request next){return delegate.selectApprovedRequest(first,next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return delegate.selectDescriptors(source,html);
        var page=Jsoup.parse(html,detail.toASCIIString());String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo");
        for(var preview:page.select("form#detailForm div.bod_view > dl.view_file > dd a.btn_preview")){
            var previous=preview.previousElementSibling();
            if(previous==null||!"a".equals(previous.tagName())||!"#".equals(previous.attr("href"))||!"#".equals(preview.attr("href"))||!"바로 보기".equals(preview.text().strip()))continue;
            String call=previous.attr("onclick").strip();if(!call.startsWith("goDownload("))continue;
            var download=AttachmentDownloadInvocation.selectArguments("goDownLoad"+call.substring("goDownload".length()),"goDownLoad",true,true);
            var match=Pattern.compile("^fn_egov_gosi_preview\\('"+notice+"','([0-9]{1,3})',").matcher(preview.attr("onclick"));if(!match.find())continue;
            String tail=preview.attr("onclick").substring(match.end());var args=AttachmentDownloadInvocation.selectArguments("goDownLoad("+tail,"goDownLoad",true,true);
            if(args.size()!=3||!args.equals(download)||!previous.text().strip().equals(args.getFirst()))continue;
            String name=args.getFirst(),format=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);if(!Set.of("pdf","hwp","hwpx").contains(format))continue;
            preview.attr("onclick","fn_egov_gosi_preview('"+notice+"','"+notice+"-"+match.group(1)+"."+format+"',"+tail);
        }
        return delegate.selectDescriptors(source,page.outerHtml());
    }
}
