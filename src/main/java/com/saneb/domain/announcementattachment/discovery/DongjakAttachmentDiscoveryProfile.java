package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 공개 상세 POST와 공식 파일 GET을 분리한다. 임의 URL·폼·스크립트를 실행하지 않는다. */
@Component
public final class DongjakAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String CODE="LOCAL_DONGJAK_POST_DETAIL_V1",DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private final SaeolGetAttachmentDiscoveryProfile validator=new SaeolGetAttachmentDiscoveryProfile(CODE,"LGS-000021",DongjakNoticePage.HOST,"SAFE_SAEOL_EMINWON","dt",false);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash(CODE+":1|fixed-seven-field-detail-post|file-get|onclick-three-literals|same-request|limit10|unknown-role|"+validator.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",DongjakNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return validator.selectSourceBindings();}
    @Override public Set<String> selectApprovedHosts(){return validator.selectApprovedHosts();}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{return DongjakNoticePage.selectDetailUri(validator.selectDetailUri(s));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI u){try{return u!=null&&u.toASCIIString().length()<=8192&&validator.selectApprovedRequest(u)&&(!DongjakNoticePage.selectMatches(u)||u.equals(DongjakNoticePage.selectDetailUri(u)));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return selectApprovedRequest(r.uri());try{if(!"POST".equals(r.method())||!r.uri().equals(URI.create("https://"+DongjakNoticePage.HOST+DongjakNoticePage.DETAIL)))return false;return r.equals(DongjakNoticePage.selectRequest(selectLogicalUri(r.form())));}catch(IllegalArgumentException e){return false;}}
    private URI selectLogicalUri(Map<String,String> f){return URI.create("https://"+DongjakNoticePage.HOST+DongjakNoticePage.DETAIL+"?"+f.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+"="+encode(e.getValue())).collect(java.util.stream.Collectors.joining("&")));}
    @Override public boolean selectApprovedRequest(Request initial,Request next){if(initial==null||next==null||!selectApprovedRequest(initial)||!selectApprovedRequest(next))return false;if(DongjakNoticePage.selectMatches(initial.uri())&&"GET".equals(initial.method()))return next.equals(DongjakNoticePage.selectRequest(initial.uri()));return initial.equals(next);}
    @Override public AttachmentPinnedDownloadClient.Download selectDownload(Request initial,java.nio.file.Path output,long maximumBytes,Operation operation)throws java.io.IOException{if(!selectApprovedRequest(initial))throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");return operation.selectDownload(DongjakNoticePage.selectMatches(initial.uri())&&"GET".equals(initial.method())?DongjakNoticePage.selectRequest(initial.uri()):initial,maximumBytes);}
    private String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8).replace("+","%20");}
    private boolean active(Element e){return !e.select("button,input,select,form,iframe,object,embed,script,style,svg,img,[href]").isEmpty()||e.getAllElements().stream().anyMatch(n->n.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=DongjakNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        for(var a:area.select(":root > a"))try{
            if(!"#n".equals(a.attr("href"))||!a.children().isEmpty()||a.attributes().asList().stream().anyMatch(x->x.getKey().toLowerCase(Locale.ROOT).startsWith("on")&&!"onclick".equals(x.getKey())))continue;var args=AttachmentDownloadInvocation.selectArguments(a.attr("onclick"),"goDownLoad",true,false);if(args.size()!=3||!a.text().strip().equals(args.getFirst()))continue;
            URI u=URI.create("https://"+DongjakNoticePage.HOST+DOWNLOAD+"?user_file_nm="+encode(args.get(0))+"&sys_file_nm="+encode(args.get(1))+"&file_path="+encode(args.get(2)));if(!selectApprovedRequest(u))continue;
            String id=normalizer.hash(args.get(2)+"\n"+args.get(1)),ext=args.getFirst().substring(args.getFirst().lastIndexOf('.')+1).toUpperCase(Locale.ROOT),systemExt=args.get(1).substring(args.get(1).lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&ext.equals(systemExt);
            var d=new Descriptor(u,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no"),"attachmentId",id)),args.getFirst(),supported?ext:null,"UNKNOWN",supported);
            if(files.containsKey(id)){if(!files.get(id).equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(id,d);a.remove();
        }catch(IllegalArgumentException e){unresolved=true;}
        for(var label:area.select(":root > strong"))if(label.children().isEmpty()&&"첨부파일".equals(label.text().strip())&&!active(label))label.remove();
        if(!area.text().isBlank()||active(area))unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
