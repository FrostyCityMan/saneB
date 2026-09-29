package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 고정 POST 세 인자만 사용한다. 실행 코드/미리보기/다른 폼은 요청하지 않는다. */
final class ChungcheongFourthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile, AttachmentDetailLimitProfile {
    private static final Set<String> FORM=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final String DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    ChungcheongFourthAttachmentDiscoveryProfile(Site s){
        site=s;code="LOCAL_"+s+"_BOARD_V1";
        hash=AttachmentProfileFingerprint.selectHash("CHUNGCHEONG_FOURTH:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|"+s.host+"|"+s.fileHost+"|https443|same-request|double-encoded-name|paired-preview|limit10|detail:"+selectDetailMaximumBytes()+"|"
                +AttachmentProfileFingerprint.selectHash("LIMIT:1",AttachmentDetailLimitProfile.class)+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",ChungcheongFourthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public long selectDetailMaximumBytes(){return (site==Site.YESAN?2L:1L)*1024*1024;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!site.parser.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();return ChungcheongFourthNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    private boolean selectSafeName(String n){return n!=null&&!n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&!n.contains("%")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private String selectEncodedName(String n){return URLEncoder.encode(n,StandardCharsets.UTF_8).replace("+","%20").replace("%21","!").replace("%27","'").replace("%28","(").replace("%29",")").replace("%7E","~").replace("%","%25");}
    private boolean selectSafeForm(Map<String,String> q){
        try{if(!q.keySet().equals(FORM)||q.get("user_file_nm").length()>9000)return false;
            String name=URLDecoder.decode(URLDecoder.decode(q.get("user_file_nm"),StandardCharsets.UTF_8),StandardCharsets.UTF_8);
            return selectSafeName(name)&&selectEncodedName(name).equals(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))&&q.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(URI u){try{return selectSafeUri(u)&&site.host.equals(u.getHost())&&u.equals(ChungcheongFourthNoticePage.selectDetailUri(site,u));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());return "POST".equals(r.method())&&selectSafeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())&&DOWNLOAD.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectSafeForm(r.form());}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>selectDetailMaximumBytes())return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try{cell=ChungcheongFourthNoticePage.selectAttachments(site,page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var forms=page.select("form#fileForm[name=fileForm][method=post]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
        if(!(site==Site.YESAN?"":"https://"+site.fileHost+DOWNLOAD).equals(form.attr("action"))||form.childrenSize()!=3||form.select("input[type=hidden]").size()!=3||form.children().stream().anyMatch(e->!e.val().isEmpty())||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FORM))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        cell.select("script,style").remove();boolean unresolved=false,exceeded=false;var files=new LinkedHashMap<String,Descriptor>();
        String selector=site==Site.HONGSEONG?"a[href^=javascript:fn_egov_downFile(]":":root > div.bbs-file__download > a[onclick^=fn_egov_downFile(]";
        var remainder=cell.clone();
        for(var link:cell.select(selector))try{
            boolean yes=site==Site.YESAN;String raw=link.attr(yes?"onclick":"href");
            if(yes?!"#".equals(link.attr("href")):link.hasAttr("onclick"))throw new IllegalArgumentException();
            var args=AttachmentDownloadInvocation.selectArguments(raw,"fn_egov_downFile",yes,yes);if(args.size()!=3||!args.get(0).equals(link.text().strip())||!selectSafeName(args.get(0)))throw new IllegalArgumentException();
            var request=new Request(URI.create("https://"+site.fileHost+DOWNLOAD),"POST",Map.of("user_file_nm",selectEncodedName(args.get(0)),"sys_file_nm",args.get(1),"file_path",args.get(2)));
            if(!selectApprovedRequest(request))throw new IllegalArgumentException();String id=normalizer.hash(args.get(2)+"\n"+args.get(1));
            if(files.containsKey(id)){if(!files.get(id).selectRequest().equals(request)||!files.get(id).displayName().equals(args.get(0)))unresolved=true;}
            else if(files.size()==10)exceeded=true;
            else{String format=selectFormat(args.get(0));boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo"))),args.get(0),supported?format:null,"UNKNOWN",supported,request.form()));}
            remainder.select(selector).stream().filter(e->raw.equals(e.attr(yes?"onclick":"href"))).forEach(Element::remove);
            if(!yes)for(var preview:remainder.select("a[href]"))if(selectPairedPreview(preview,detail,args))preview.remove();
            if(link.select("input,button,img,iframe,object,embed,[onclick]").stream().anyMatch(e->e!=link))unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(!remainder.text().isBlank()||!remainder.select("a,button,input,img,iframe,form,object,embed,[onclick],[href]").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectPairedPreview(Element e,URI detail,List<String> args){
        try{URI u=detail.resolve(e.attr("href").replace(" ","%20"));return !e.hasAttr("onclick")&&"미리보기".equals(e.text())&&selectSafeUri(u)&&site.host.equals(u.getHost())&&"/synapsoft/SaeolFileViewer.do".equals(u.getPath())&&CapitalThirdNoticePage.selectParameters(u.getRawQuery()).equals(Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));}catch(IllegalArgumentException ex){return false;}
    }
    private String selectFormat(String n){String ext=n.contains(".")?n.substring(n.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
