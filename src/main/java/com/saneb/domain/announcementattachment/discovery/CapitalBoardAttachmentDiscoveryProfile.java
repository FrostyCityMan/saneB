package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.ChungjuEminwonNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 포털 첨부 영역→해당 지자체 새올 파일. 실패한 파일과 정상 파일은 독립적으로 보존한다. */
final class CapitalBoardAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;
    private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    CapitalBoardAttachmentDiscoveryProfile(Site site){
        this.site=Objects.requireNonNull(site);code="LOCAL_"+site+"_PORTAL_V1";
        hash=AttachmentProfileFingerprint.selectHash("CAPITAL_BOARD:1|"+site+"|"+site.sourceCode+"|SPRING_BBS|"+site.host+"|"+site.fileHost
                +"|https443|same-request|plain3|paired-preview-only|unknown-role|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",CapitalEminwonNoticePage.class),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,"SPRING_BBS"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())
                ||!"SPRING_BBS".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            return CapitalEminwonNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)
            &&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath().equals(u.getPath());}
    private boolean selectSafeValues(Map<String,String> v){return v.keySet().equals(FIELDS)&&selectSafeName(v.get("user_file_nm"))&&selectSafeName(v.get("sys_file_nm"))
            &&v.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    @Override public boolean selectApprovedRequest(URI uri){
        try{if(!selectSafeUri(uri))return false;
            if(site.host.equals(uri.getHost()))return uri.equals(CapitalEminwonNoticePage.selectDetailUri(site,uri));
            return site!=Site.GUNPO&&site.fileHost.equals(uri.getHost())&&DOWNLOAD.equals(uri.getPath())&&selectSafeValues(ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery()));
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null)return false;if("GET".equals(r.method()))return selectApprovedRequest(r.uri());
        return site==Site.GUNPO&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())
                &&DOWNLOAD.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectSafeValues(r.form());
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try{cell=CapitalEminwonNoticePage.selectAttachments(site,page);}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(site==Site.GUNPO){var forms=page.select("form[name=form2][method=post]");
            if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
            if(form.childrenSize()!=3||form.select("input[type=hidden]").size()!=3||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FIELDS)
                    ||form.children().stream().anyMatch(e->!e.val().isEmpty())||form.hasAttr("action")&&!form.attr("action").isEmpty())return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");}
        var residue=cell.clone();residue.select("a.p-attach__link,a.p-attach__preview").remove();
        boolean unresolved=!residue.text().isBlank()||!residue.select("a,img,button,input,select,script,iframe,object,embed,form,[onclick],[href]").isEmpty();
        var files=new LinkedHashMap<String,Descriptor>();var validatedPreviews=new HashSet<Element>();boolean exceeded=false;
        for(var anchor:cell.select("a.p-attach__link"))try{
            if(anchor.hasAttr("onclick")||!anchor.select("script,input,button,iframe,object,embed,[onclick]").isEmpty())throw new IllegalArgumentException();
            Map<String,String> values;Request request;
            if(site==Site.GUNPO){var a=AttachmentDownloadInvocation.selectArguments(anchor.attr("href"),"goDownLoad",false,false);if(a.size()!=3)throw new IllegalArgumentException();
                values=Map.of("user_file_nm",a.get(0),"sys_file_nm",a.get(1),"file_path",a.get(2));request=new Request(URI.create("https://"+site.fileHost+DOWNLOAD),"POST",values);
            }else{String href=anchor.attr("href");if(href.length()>8192||href.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException();
                URI u=detail.resolve(URI.create(href.replace(" ","%20")));if(!selectApprovedRequest(u)||!site.fileHost.equals(u.getHost()))throw new IllegalArgumentException();
                values=ChungjuEminwonNoticePage.selectParameters(u.getRawQuery());var q=new StringJoiner("&");values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->q.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
                request=Request.selectGet(URI.create("https://"+site.fileHost+DOWNLOAD+"?"+q));}
            if(!selectApprovedRequest(request))throw new IllegalArgumentException();String name=values.get("user_file_nm");
            var label=anchor.clone();label.select("span.p-icon,img,i.p-icon").remove();if(!name.equals(label.text().strip()))throw new IllegalArgumentException();
            if(site==Site.GUNPO){var preview=anchor.nextElementSibling();if(preview!=null&&preview.hasClass("p-attach__preview")){
                if(selectPreview(preview,detail,values))validatedPreviews.add(preview);else unresolved=true;}}
            String id=normalizer.hash(values.get("file_path")+"\n"+values.get("sys_file_nm"));
            if(files.containsKey(id)){if(!files.get(id).selectRequest().equals(request)||!files.get(id).displayName().equals(name))unresolved=true;continue;}
            if(files.size()==10){exceeded=true;continue;}String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(values.get("sys_file_nm")));
            var locator=new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("attachmentId",id,"noticeId",ChungjuEminwonNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no")));
            files.put(id,new Descriptor(request.uri(),locator,name,supported?format:null,"UNKNOWN",supported,request.form()));
        }catch(IllegalArgumentException e){unresolved=true;}
        if(cell.select("a.p-attach__preview").stream().anyMatch(e->!validatedPreviews.contains(e)))unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectPreview(Element preview,URI detail,Map<String,String> file){
        try{var uri=detail.resolve(URI.create(preview.attr("href").replace(" ","%20")));if(!selectSafeUri(uri)||!site.host.equals(uri.getHost())||!"/www/previewEminwonFile.do".equals(uri.getPath())||preview.hasAttr("onclick")||!preview.children().isEmpty())return false;
            var q=ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery());return q.equals(Map.of("fileUrl",file.get("file_path"),"fileReNm",file.get("sys_file_nm"),"fileNm",file.get("user_file_nm"),"notAncmtMgtNo",ChungjuEminwonNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no")))&&"파일 미리보기".equals(preview.text().strip());
        }catch(IllegalArgumentException e){return false;}
    }
    private boolean selectSafeName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String s){String ext=s.contains(".")?s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
