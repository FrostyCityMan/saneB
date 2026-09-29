package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.MapoNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 마포 공식 첨부 목록의 직접 링크만 수집한다. 파일명이나 미리보기로 문서 역할을 추측하지 않는다. */
@Component
public final class MapoAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_MAPO_PORTAL_V1",FILE_HOST="eminwon.mapo.go.kr",DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Pattern PREVIEW=Pattern.compile("^viewapplys\\(([1-9][0-9]{0,14}),'([^'\\r\\n]{1,8192})'\\);return false;$");
    private final SaeolGetAttachmentDiscoveryProfile files=new SaeolGetAttachmentDiscoveryProfile(CODE,"LGS-000015",FILE_HOST,"MAPO_LEGAL_NOTICE_TABLE","td",false);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("MAPO_PORTAL:1|www.mapo.go.kr|LGS-000015|MAPO_LEGAL_NOTICE_TABLE|bbs_view_file|paired-preview-no-fetch|limit10|unknown-role|"+files.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",MapoNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000015","MAPO_LEGAL_NOTICE_TABLE"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(MapoNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{
        if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000015".equals(s.localSourceCode())||!"MAPO_LEGAL_NOTICE_TABLE".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return MapoNoticePage.selectDetailUri(URI.create(s.sourceUrl()));
    }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI u){try{if(u!=null&&MapoNoticePage.HOST.equals(u.getHost()))return u.equals(MapoNoticePage.selectDetailUri(u));return u!=null&&DOWNLOAD.equals(u.getPath())&&files.selectApprovedRequest(u);}catch(IllegalArgumentException e){return false;}}
    private URI selectFileUri(String raw,boolean preview){String prefix=(preview?"http":"https")+"://"+FILE_HOST+DOWNLOAD+"?";
        if(raw==null||!raw.startsWith(prefix)||raw.length()>8192||raw.contains("#"))throw new IllegalArgumentException();var q=CapitalThirdNoticePage.selectParameters(raw.substring(prefix.length()));var encoded=new StringJoiner("&");q.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->encoded.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));URI result=URI.create("https://"+FILE_HOST+DOWNLOAD+"?"+encoded);if(!selectApprovedRequest(result))throw new IllegalArgumentException();return result;}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        org.jsoup.nodes.Element container;try{container=MapoNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()));}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var found=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;var residual=container.clone();residual.select(":root > li").remove();if(!residual.text().isBlank()||!residual.select("a,button,input,script,iframe,object,embed,img,form,[onclick],[href]").isEmpty())unresolved=true;
        String noticeId=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("bcId");
        for(var item:container.select(":root > li"))try{
            var anchors=item.select("a.file_name");if(anchors.size()!=1)throw new IllegalArgumentException();var anchor=anchors.getFirst();if(anchor.hasAttr("onclick"))throw new IllegalArgumentException();URI fetch=selectFileUri(anchor.attr("href"),false);var q=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());String name=q.get("user_file_nm");if(!name.equals(anchor.text().strip()))throw new IllegalArgumentException();
            String id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm")),ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&q.get("sys_file_nm").toLowerCase(Locale.ROOT).endsWith("."+ext.toLowerCase(Locale.ROOT));
            var descriptor=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",id,"noticeId",noticeId)),name,supported?ext:null,"UNKNOWN",supported);
            if(found.containsKey(id)){if(!found.get(id).equals(descriptor))unresolved=true;}else if(found.size()==10)exceeded=true;else found.put(id,descriptor);
            for(var preview:item.select("a.file_view_btn")){String call=preview.attr("onclick");if(call.length()>8192)throw new IllegalArgumentException();var m=PREVIEW.matcher(call);if(!"#none".equals(preview.attr("href"))||!m.matches()||!noticeId.equals(m.group(1))||!fetch.equals(selectFileUri(m.group(2),true)))unresolved=true;}
            var remainder=item.clone();remainder.select("a.file_name,a.file_view_btn").remove();if(!remainder.text().isBlank()||!remainder.select("a,button,input,script,iframe,object,embed,img,form,[onclick],[href]").isEmpty()||!item.select("a [onclick],a script,a input,a button,a img,a iframe").isEmpty())unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(found.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(found.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(found.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(found.values()),List.of());}
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
