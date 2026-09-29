package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SokchoNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 대표 홈페이지 첨부 영역의 직접 링크를 새올 GET 계약으로 검증한다. 미리보기는 호출하지 않는다. */
@Component
public final class SokchoAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_SOKCHO_PORTAL_V1",FILE_HOST="eminwon.sokcho.go.kr",DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private final SaeolGetAttachmentDiscoveryProfile files=new SaeolGetAttachmentDiscoveryProfile(CODE,"LGS-000122",FILE_HOST,"SAFE_SAEOL_EMINWON","td",false);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("SOKCHO_PORTAL:1|www.sokcho.go.kr|LGS-000122|SAFE_SAEOL_EMINWON|skinTb|attachFile|paired-preview-no-fetch|limit10|unknown-role|"+files.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",SokchoNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000122","SAFE_SAEOL_EMINWON"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(SokchoNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{
        if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000122".equals(s.localSourceCode())||!"SAFE_SAEOL_EMINWON".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();
        return SokchoNoticePage.selectDetailUri(URI.create(s.sourceUrl()));
    }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI u){try{
        if(u!=null&&SokchoNoticePage.HOST.equals(u.getHost()))return u.equals(SokchoNoticePage.selectDetailUri(u));
        return u!=null&&DOWNLOAD.equals(u.getPath())&&files.selectApprovedRequest(u);
    }catch(IllegalArgumentException e){return false;}}
    private URI selectFileUri(String raw,boolean preview){
        String prefix=(preview?"http":"https")+"://"+FILE_HOST+DOWNLOAD+"?";
        if(raw==null||!raw.startsWith(prefix)||raw.length()>8192||raw.contains("#"))throw new IllegalArgumentException();
        var q=CapitalThirdNoticePage.selectParameters(raw.substring(prefix.length()));var encoded=new StringJoiner("&");
        q.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->encoded.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
        URI result=URI.create("https://"+FILE_HOST+DOWNLOAD+"?"+encoded);if(!selectApprovedRequest(result))throw new IllegalArgumentException();return result;
    }
    @Override public Result selectDescriptors(Source s,String html){
        URI detail=selectDetailUri(s);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        org.jsoup.nodes.Element container;try{container=SokchoNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()));}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var found=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        var residual=container.clone();residual.select(":root > div.attachFile").remove();if(!residual.text().isBlank()||!residual.select("a,button,input,script,iframe,object,embed,img,form,[onclick],[href]").isEmpty())unresolved=true;
        for(var item:container.select(":root > div.attachFile"))try{
            var anchors=item.select(":root > a:not(.btn-file-ezview)");if(anchors.size()!=1)throw new IllegalArgumentException();var anchor=anchors.getFirst();if(anchor.hasAttr("onclick"))throw new IllegalArgumentException();
            URI fetch=selectFileUri(anchor.attr("href"),false);var q=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());String name=q.get("user_file_nm");if(!name.equals(anchor.text().strip()))throw new IllegalArgumentException();
            String id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT),stored=q.get("sys_file_nm");boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&stored.toLowerCase(Locale.ROOT).endsWith("."+ext.toLowerCase(Locale.ROOT));
            var descriptor=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo"))),name,supported?ext:null,"UNKNOWN",supported);
            if(found.containsKey(id)){if(!found.get(id).equals(descriptor))unresolved=true;}else if(found.size()==10)exceeded=true;else found.put(id,descriptor);
            for(var preview:item.select(":root > a.btn-file-ezview"))if(!"#nolink".equals(preview.attr("href"))||!fetch.equals(selectFileUri(preview.attr("data-url"),true))||preview.hasAttr("onclick"))unresolved=true;
            var remainder=item.clone();remainder.select(":root > a").remove();if(!remainder.text().isBlank()||!remainder.select("a,button,input,script,iframe,object,embed,img,form,[onclick],[href]").isEmpty()||!item.select("a [onclick],a script,a input,a button,a img,a iframe").isEmpty())unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(found.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(found.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(found.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(found.values()),List.of());
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
