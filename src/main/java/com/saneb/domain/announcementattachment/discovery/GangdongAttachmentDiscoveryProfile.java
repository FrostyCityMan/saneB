package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GangdongNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 강동 공식 goDownLoad의 공개 POST 4개 필드만 사용한다. 미리보기는 실행하지 않는다. */
@Component
public final class GangdongAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String CODE="LOCAL_GANGDONG_POST_V1", SOURCE="LGS-000026", PARSER="SAFE_SAEOL_EMINWON_HREF";
    private static final String HOST="eminwon.gangdong.go.kr", DOWNLOAD="/emwp/jsp/ofr/FileDownNewPbs.jsp";
    private static final URI FILE_URI=URI.create("https://"+HOST+DOWNLOAD);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("GANGDONG:2|LGS-000026|SAFE_SAEOL_EMINWON_HREF|https443|official-post4-isHome-N|period-before-file|same-file|unknown-role|limit10|partial-preserved|"+AttachmentProfileFingerprint.selectHash("PAGE:1",GangdongNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("FLOW:1",GangdongPeriodDownloadFlow.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(SOURCE,PARSER));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(GangdongNoticePage.HOST,HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!SOURCE.equals(source.localSourceCode())
                    ||!PARSER.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                    ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            return GangdongNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){try{return uri!=null&&uri.equals(GangdongNoticePage.selectDetailUri(uri));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request request){
        if(request==null)return false;
        if("GET".equals(request.method()))return request.form().isEmpty()&&selectApprovedRequest(request.uri());
        if(GangdongPeriodDownloadFlow.selectApprovedStep(request))return true;
        var form=request.form();return "POST".equals(request.method())&&FILE_URI.equals(request.uri())
                &&form.keySet().equals(Set.of("isHome","user_file_nm","sys_file_nm","file_path"))&&"N".equals(form.get("isHome"))
                &&selectName(form.get("user_file_nm"))&&selectName(form.get("sys_file_nm"))
                &&form.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){
        if(first==null||next==null||!selectApprovedRequest(first))return false;
        if(first.equals(next))return true;
        return FILE_URI.equals(first.uri())&&"POST".equals(first.method())&&GangdongPeriodDownloadFlow.selectSameFileStep(first,next);
    }
    @Override public com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download selectDownload(
            Request initial,java.nio.file.Path output,long maximumBytes,Operation operation) throws java.io.IOException {
        if(initial==null||!FILE_URI.equals(initial.uri())||!"POST".equals(initial.method())||!selectApprovedRequest(initial))throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        return GangdongPeriodDownloadFlow.selectDownload(initial,output,maximumBytes,operation);
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1048576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element area;try{area=GangdongNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var files=new LinkedHashMap<String,Descriptor>();var known=new HashSet<Element>();boolean unresolved=false,exceeded=false;
        for(var a:area.select("a")){
            var args=AttachmentDownloadInvocation.selectArguments(a.attr("onclick"),"goDownLoad",true,false);
            if(args.size()!=3||!"javascript:".equals(a.attr("href"))||!"다운로드".equals(a.attr("title"))
                    ||a.attributes().asList().stream().anyMatch(attr->attr.getKey().toLowerCase(Locale.ROOT).startsWith("on")&&!"onclick".equals(attr.getKey()))
                    ||!a.children().isEmpty()||!a.text().equals(args.get(0)))continue;
            var form=Map.of("isHome","N","user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));
            if(!selectApprovedRequest(new Request(FILE_URI,"POST",form)))continue;
            String id=normalizer.hash(args.get(2)+"\n"+args.get(1)),format=selectFormat(args.get(0));
            boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));
            var descriptor=new Descriptor(FILE_URI,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",detail.getPath().substring(GangdongNoticePage.PREFIX.length()),"attachmentId",id)),args.get(0),supported?format:null,"UNKNOWN",supported,form);
            var old=files.get(id);if(old!=null){if(!old.equals(descriptor)){unresolved=true;continue;}}
            else if(files.size()==10){exceeded=true;continue;}else files.put(id,descriptor);
            known.add(a);
        }
        known.forEach(Element::remove);
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()
                ||files.isEmpty()&&!area.select("li,p").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectName(String value){return value!=null&&!value.isBlank()&&value.length()<=500&&!value.contains("/")&&!value.contains("\\")&&!value.contains("..")&&!value.contains("%")&&value.indexOf('\ufffd')<0&&value.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String name){int dot=name.lastIndexOf('.');String ext=dot<0?"":name.substring(dot+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
