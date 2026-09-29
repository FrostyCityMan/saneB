package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 파일 셀의 GET/고정 POST만 사용한다. 정상 파일과 미해결 링크를 별도로 보존한다. */
final class ChungcheongFifthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile, AttachmentDetailLimitProfile {
    private static final Set<String> FORM=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final String POST_PATH="/emwp/jsp/ofr/FileDown.jsp",GET_PATH="/_prog/download/";
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    ChungcheongFifthAttachmentDiscoveryProfile(Site s){
        site=s;code="LOCAL_"+s+"_BOARD_V1";
        hash=AttachmentProfileFingerprint.selectHash("CHUNGCHEONG_FIFTH:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|https443|same-request|plain-form|paired-preview|limit10|detail:"+selectDetailMaximumBytes()+"|"
                +AttachmentProfileFingerprint.selectHash("LIMIT:1",AttachmentDetailLimitProfile.class)+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",ChungcheongFifthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public long selectDetailMaximumBytes(){return (site==Site.GEUMSAN?2L:1L)*1024*1024;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return site==Site.GEUMSAN?Set.of(site.host):Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!site.parser.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();return ChungcheongFifthNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    private boolean selectSafeName(String n){return n!=null&&!n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&!n.contains("%")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectSafeForm(Map<String,String> q){return q.keySet().equals(FORM)&&selectSafeName(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))&&q.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    private boolean selectDownloadGet(URI u){
        if(site!=Site.GEUMSAN||!selectSafeUri(u)||!site.host.equals(u.getHost())||!GET_PATH.equals(u.getPath()))return false;
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());return q.keySet().equals(Set.of("func_gbn_cd","site_dvs_cd","filename","file_realname"))&&"gosi".equals(q.get("func_gbn_cd"))&&"kr".equals(q.get("site_dvs_cd"))&&q.getOrDefault("filename","").matches("[0-9]{14}_[a-z0-9]{30}\\.[A-Za-z0-9]{1,10}")&&selectSafeName(q.get("file_realname"));
    }
    @Override public boolean selectApprovedRequest(URI u){try{return selectDownloadGet(u)||(selectSafeUri(u)&&site.host.equals(u.getHost())&&u.equals(ChungcheongFifthNoticePage.selectDetailUri(site,u)));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());return site==Site.BUYEO&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())&&POST_PATH.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectSafeForm(r.form());}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>selectDetailMaximumBytes())return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try{cell=ChungcheongFifthNoticePage.selectAttachments(site,page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(site==Site.BUYEO){var forms=page.select("form[name=fileForm][method=post]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
            if(!form.attr("action").isEmpty()||form.childrenSize()!=3||form.select("input[type=hidden]").size()!=3||form.children().stream().anyMatch(e->!e.val().isEmpty())||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FORM))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");}
        cell.select("script,style").remove();boolean unresolved=false,exceeded=false;var files=new LinkedHashMap<String,Descriptor>();
        String selector=site==Site.GEUMSAN?":root > a.btn-file[href]":":root > a[onclick^=fn_saeol_downFile(]";var remainder=cell.clone();
        for(var link:cell.select(selector))try{
            Request request;String name,stored,identity;List<String> args=List.of();
            if(site==Site.GEUMSAN){if(link.hasAttr("onclick"))throw new IllegalArgumentException();URI u=detail.resolve(link.attr("href"));if(!selectDownloadGet(u))throw new IllegalArgumentException();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());name=q.get("file_realname");stored=q.get("filename");identity=stored;request=Request.selectGet(u);if(!selectNormalized(name).equals(selectNormalized(link.text())))throw new IllegalArgumentException();}
            else{if(!"javascript:void(0);".equals(link.attr("href")))throw new IllegalArgumentException();args=selectArguments(link.attr("onclick"),"fn_saeol_downFile");if(args.size()!=3)throw new IllegalArgumentException();name=args.get(0);stored=args.get(1);identity=args.get(2)+"\n"+stored;
                var label=link.clone();for(var badge:label.select(":root > strong")){if(!badge.text().replace('\u00a0',' ').strip().matches("\\([0-9]+(?:\\.[0-9]+)? (?:B|KB|MB)\\)"))throw new IllegalArgumentException();badge.remove();}
                if(!name.equals(link.attr("title"))||!selectNormalized(name).equals(selectNormalized(label.text())))throw new IllegalArgumentException();
                request=new Request(URI.create("https://"+site.fileHost+POST_PATH),"POST",Map.of("user_file_nm",name,"sys_file_nm",stored,"file_path",args.get(2)));}
            if(!selectSafeName(name)||!selectApprovedRequest(request))throw new IllegalArgumentException();String id=normalizer.hash(identity);
            if(files.containsKey(id)){if(!files.get(id).selectRequest().equals(request)||!files.get(id).displayName().equals(name))unresolved=true;}
            else if(files.size()==10)exceeded=true;
            else{String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(stored));files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,request.uri().getPath(),Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("mng_no"))),name,supported?format:null,"UNKNOWN",supported,request.form()));}
            String raw=link.attr(site==Site.GEUMSAN?"href":"onclick");remainder.select(selector).stream().filter(e->raw.equals(e.attr(site==Site.GEUMSAN?"href":"onclick"))).forEach(Element::remove);
            if(site==Site.BUYEO)for(var preview:remainder.select("a[onclick]"))if(selectPairedPreview(preview,args))preview.remove();
            if(link.select("input,button,img,iframe,object,embed,[onclick]").stream().anyMatch(e->e!=link))unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(!remainder.text().isBlank()||!remainder.select("a,button,input,img,iframe,form,object,embed,[onclick],[href]").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private List<String> selectArguments(String raw,String name){if(!raw.startsWith(name+"("))throw new IllegalArgumentException();return AttachmentDownloadInvocation.selectArguments("fn_egov_downFile"+raw.substring(name.length()),"fn_egov_downFile",true,false);}
    private boolean selectPairedPreview(Element e,List<String> args){try{return "javascript:void(0);".equals(e.attr("href"))&&"미리보기 새창열림".equals(e.attr("title"))&&e.text().isBlank()&&e.childrenSize()==1&&e.select(":root > img[src='/images/common/buyeo_see_btn.gif'][alt=바로보기]").size()==1&&selectArguments(e.attr("onclick"),"fn_egov_preview_File").equals(args);}catch(IllegalArgumentException ex){return false;}}
    private String selectNormalized(String n){return n.replace('\u00a0',' ').strip().replaceAll("\\s+"," ");}
    private String selectFormat(String n){String ext=n.contains(".")?n.substring(n.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
