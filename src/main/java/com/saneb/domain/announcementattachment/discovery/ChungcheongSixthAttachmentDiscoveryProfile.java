package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 세 문자열 호출만 읽으며 아산 POST와 서산 GET을 구분한다. */
final class ChungcheongSixthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FORM=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final String DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    ChungcheongSixthAttachmentDiscoveryProfile(Site s){
        site=s;code="LOCAL_"+s+"_BOARD_V1";
        hash=AttachmentProfileFingerprint.selectHash("CHUNGCHEONG_SIXTH:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|https443|same-request|plain-form-or-query|paired-preview|limit10|detail1MiB|"
                +AttachmentProfileFingerprint.selectHash("PAGE:1",ChungcheongSixthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return site==Site.ASAN?Set.of(site.host,"asan.go.kr",site.fileHost):Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!site.parser.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();return ChungcheongSixthNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    private boolean selectSafeName(String n){return n!=null&&!n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&!n.contains("%")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectSafeForm(Map<String,String> q){return q.keySet().equals(FORM)&&selectSafeName(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))&&q.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    @Override public boolean selectApprovedRequest(URI u){
        try{if(!selectSafeUri(u))return false;if(site.fileHost.equals(u.getHost()))return site==Site.SEOSAN&&DOWNLOAD.equals(u.getPath())&&selectSafeForm(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));return site.selectDetailHost(u.getHost())&&u.equals(ChungcheongSixthNoticePage.selectDetailUri(site,u));}catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());return site==Site.ASAN&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())&&DOWNLOAD.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectSafeForm(r.form());}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1048576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element cell;try{cell=ChungcheongSixthNoticePage.selectAttachments(site,page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(site==Site.ASAN){var forms=page.select("form[name=nnn][method=post]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
            if(!("https://"+site.fileHost+DOWNLOAD).equals(form.attr("action"))||form.childrenSize()!=3||form.select("input[type=hidden]").size()!=3||form.children().stream().anyMatch(e->!e.val().isEmpty())||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FORM))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");cell.select(":root > b").remove();}
        cell.select("script,style").remove();boolean unresolved=false,exceeded=false;var files=new LinkedHashMap<String,Descriptor>();String selector=":root > a[href^=javascript:goDownLoad(]";var remainder=cell.clone();
        for(var link:cell.select(selector))try{
            if(link.hasAttr("onclick"))throw new IllegalArgumentException();var args=AttachmentDownloadInvocation.selectArguments(link.attr("href"),"goDownLoad",false,false);if(args.size()!=3)throw new IllegalArgumentException();String name=args.get(0);var label=link.clone();
            if(site==Site.ASAN)for(var badge:label.select(":root > span")){if(!badge.text().strip().matches("\\([0-9]+(?:\\.[0-9]+)?(?:kb|mb|b)\\)"))throw new IllegalArgumentException();badge.remove();}
            if(!selectNormalized(name).equals(selectNormalized(label.text())))throw new IllegalArgumentException();var values=Map.of("user_file_nm",name,"sys_file_nm",args.get(1),"file_path",args.get(2));if(!selectSafeForm(values))throw new IllegalArgumentException();
            Request request;if(site==Site.ASAN)request=new Request(URI.create("https://"+site.fileHost+DOWNLOAD),"POST",values);else{var query=new StringJoiner("&");values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));request=Request.selectGet(URI.create("https://"+site.fileHost+DOWNLOAD+"?"+query));}
            if(!selectApprovedRequest(request))throw new IllegalArgumentException();String id=normalizer.hash(args.get(2)+"\n"+args.get(1));
            if(files.containsKey(id)){if(!files.get(id).selectRequest().equals(request)||!files.get(id).displayName().equals(name))unresolved=true;}
            else if(files.size()==10)exceeded=true;
            else{String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey))),name,supported?format:null,"UNKNOWN",supported,request.form()));}
            String raw=link.attr("href");remainder.select(selector).stream().filter(e->raw.equals(e.attr("href"))).forEach(Element::remove);
            if(site==Site.SEOSAN)for(var preview:remainder.select("a[onclick]"))if(selectPairedPreview(preview,args))preview.remove();
            if(link.select("input,button,img,iframe,object,embed,[onclick]").stream().anyMatch(e->e!=link))unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(!remainder.text().isBlank()||!remainder.select("a,button,input,img,iframe,form,object,embed,[onclick],[href]").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectPairedPreview(Element e,List<String> args){String raw=e.attr("onclick");return raw.startsWith("fn_previewDownload(")&&"#".equals(e.attr("href"))&&"새창으로 이동".equals(e.attr("title"))&&e.text().isBlank()&&e.childrenSize()==1&&e.select(":root > img[src='/common/images/board/btn_view.jpg'][alt=바로보기]").size()==1&&AttachmentDownloadInvocation.selectArguments("goDownLoad"+raw.substring("fn_previewDownload".length()),"goDownLoad",true,true).equals(args);}
    private String selectNormalized(String n){return n.replace('\u00a0',' ').strip().replaceAll("\\s+"," ");}
    private String selectFormat(String n){String ext=n.contains(".")?n.substring(n.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
