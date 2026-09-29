package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.HampyeongNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 함평 공식 세 문자열 POST 값은 요청에만 두고 감사 근거에는 해시만 남긴다. */
@Component
public final class HampyeongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_HAMPYEONG_NOTICE_V1",FILE_HOST="eminwon.hampyeong.go.kr",DOWNLOAD="/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> FIELDS=Set.of("seq","user_file_nm","sys_file_nm","file_path");
    private static final Pattern OPAQUE=Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$");
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("HAMPYEONG:1|LGS-000194|HEURISTIC_NOTICE|https443|opaque-three-field-empty-seq-post|limit10|detail1MiB|"+AttachmentProfileFingerprint.selectHash("PAGE:1",HampyeongNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000194","HEURISTIC_NOTICE"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(HampyeongNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){try{if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000194".equals(source.localSourceCode())||!"HEURISTIC_NOTICE".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();return HampyeongNoticePage.selectDetailUri(URI.create(source.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI u){try{return u!=null&&u.equals(HampyeongNoticePage.selectDetailUri(u));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());var u=r.uri();var f=r.form();
        return "POST".equals(r.method())&&"https".equals(u.getScheme())&&FILE_HOST.equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.getRawQuery()==null&&DOWNLOAD.equals(u.getRawPath())&&u.equals(u.normalize())&&f.keySet().equals(FIELDS)&&"".equals(f.get("seq"))&&selectOpaque(f.get("user_file_nm"))&&selectOpaque(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1048576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element area;
        try{area=HampyeongNoticePage.selectAttachments(page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var forms=page.select("form[name=ffile]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
        if(!"post".equalsIgnoreCase(form.attr("method"))||!("https://"+FILE_HOST+DOWNLOAD).equals(form.attr("action"))||form.childrenSize()!=4||form.select("input[type=hidden]").size()!=4||form.children().stream().anyMatch(e->!e.val().isEmpty())||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FIELDS))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean unresolved=false,exceeded=false;
        for(var a:area.select(":root > a"))try{
            if(!"#".equals(a.attr("href")))continue;String call=a.attr("onclick");if(!call.startsWith("goDown("))continue;var args=AttachmentDownloadInvocation.selectArguments("goDownLoad"+call.substring("goDown".length()),"goDownLoad",true,false);if(args.size()!=3)continue;
            String name=a.text().strip();if(!selectName(name))continue;var values=Map.of("seq","","user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));var request=new Request(URI.create("https://"+FILE_HOST+DOWNLOAD),"POST",values);if(!selectApprovedRequest(request))continue;
            String id=normalizer.hash(args.get(2)+"\n"+args.get(1));var old=files.get(id);if(old!=null){if(old.displayName().equals(name)&&old.selectRequest().equals(request)){recognized.add(a);}continue;}
            if(files.size()==10){exceeded=true;continue;}String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);
            files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("SEQ"),"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported,values));recognized.add(a);
        }catch(IllegalArgumentException ignored){/* 한 파일의 오류가 다른 정상 descriptor를 버리지 않도록 한다. */}
        for(var a:recognized)a.remove();
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectOpaque(String s){return s!=null&&s.length()>=22&&s.length()<=2048&&OPAQUE.matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private boolean selectName(String s){return !s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
