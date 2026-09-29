package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SangjuNoticePage;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 공식 폼의 불투명 파일 값은 전송에만 쓰고 식별 근거에는 해시만 기록한다. */
@Component
public final class SangjuAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_SANGJU_GOSI_V1",FILE_HOST="eminwon.sangju.go.kr",DOWNLOAD="/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern OPAQUE=Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$");
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("SANGJU:1|fixed-form2-opaque-post|no-redirect|partial-preserved|unknown-role|limit10|"
            +AttachmentProfileFingerprint.selectHash("PAGE:1",SangjuNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)
            +"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000208","SAFE_SANGJU_GOSI"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(SangjuNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000208".equals(source.localSourceCode())
                    ||!"SAFE_SANGJU_GOSI".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                    ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            return SangjuNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){try{return uri!=null&&uri.equals(SangjuNoticePage.selectDetailUri(uri));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request request){
        if(request==null)return false;
        if("GET".equals(request.method()))return request.form().isEmpty()&&selectApprovedRequest(request.uri());
        var uri=request.uri();var values=request.form();
        return "POST".equals(request.method())&&"https".equals(uri.getScheme())&&FILE_HOST.equals(uri.getHost())
                &&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null&&uri.getFragment()==null&&uri.getRawQuery()==null
                &&DOWNLOAD.equals(uri.getRawPath())&&uri.equals(uri.normalize())&&values.keySet().equals(FIELDS)
                &&selectOpaque(values.get("user_file_nm"))&&selectOpaque(values.get("sys_file_nm"))
                &&values.get("file_path").length()<=2048&&values.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_048_576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element area;
        try{area=SangjuNoticePage.selectAttachments(page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var forms=page.select("form#form2[name=form2]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
        if(!"post".equalsIgnoreCase(form.attr("method"))||!("https://"+FILE_HOST+DOWNLOAD).equals(form.attr("action"))
                ||form.childrenSize()!=3||form.select("input[type=hidden]").size()!=3||form.children().stream().anyMatch(e->!e.val().isEmpty())
                ||!form.children().stream().map(e->e.attr("name")).collect(Collectors.toSet()).equals(FIELDS))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        for(var a:area.select("a"))try{
            String call=a.attr("onclick").strip();
            if(!"javascript:;".equals(a.attr("href"))||!call.startsWith("fnFileDown("))throw new IllegalArgumentException();
            // 호출명만 고정 치환하여 검증된 선형 세 문자열 파서를 재사용한다. JS는 실행하지 않는다.
            var args=AttachmentDownloadInvocation.selectArguments("goDownLoad"+call.substring("fnFileDown".length()),"goDownLoad",true,false);
            if(args.size()!=3||!a.select("script,input,button,img,iframe").isEmpty()||!a.children().select("[onclick]").isEmpty())throw new IllegalArgumentException();
            String name=a.text().strip();if(name.endsWith(","))name=name.substring(0,name.length()-1).strip();
            if(!selectName(name))throw new IllegalArgumentException();
            var values=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));
            var request=new Request(URI.create("https://"+FILE_HOST+DOWNLOAD),"POST",values);
            if(!selectApprovedRequest(request))throw new IllegalArgumentException();
            String id=normalizer.hash(args.get(2)+"\n"+args.get(1)),format=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);
            boolean supported=Set.of("PDF","HWP","HWPX").contains(format);
            var d=new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("mgtNo"),"attachmentId",id)),name,supported?format:null,"UNKNOWN",supported,values);
            var previous=files.get(id);
            if(previous!=null){if(!previous.equals(d))unresolved=true;}
            else if(files.size()==10)exceeded=true;else files.put(id,d);
            a.remove();
        }catch(IllegalArgumentException e){unresolved=true;}
        if(!area.text().isBlank()||!area.select("a,script,button,input,select,form,iframe,object,embed,[onclick],[href],img").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectOpaque(String value){return value!=null&&value.length()>=22&&value.length()<=2048&&OPAQUE.matcher(value).find()&&!value.contains("..")&&value.codePoints().noneMatch(Character::isISOControl)&&value.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private boolean selectName(String value){return !value.isBlank()&&value.length()<=500&&!value.contains("/")&&!value.contains("\\")&&!value.contains("..")&&!value.contains("%")&&value.indexOf('\ufffd')<0&&value.codePoints().noneMatch(Character::isISOControl);}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
