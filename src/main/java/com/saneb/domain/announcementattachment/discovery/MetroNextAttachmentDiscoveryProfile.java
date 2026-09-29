package com.saneb.domain.announcementattachment.discovery;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.MetroNextNoticePage;
import com.saneb.domain.announcementsource.provider.content.MetroNextNoticePage.Site;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
/** 광주 남구는 기존 새올 GET 엔진을 재사용하고 대전 중구는 공식 고정 파일 폼만 사용한다. */
final class MetroNextAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;private final String code,hash;private final SaeolGetAttachmentDiscoveryProfile delegate;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    MetroNextAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_BOARD_V1";delegate=s==Site.GWANGJU_NAMGU?new SaeolGetAttachmentDiscoveryProfile(code,s.sourceCode,s.host,s.parser,"th",false):null;
        hash=AttachmentProfileFingerprint.selectHash("METRO_NEXT:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|"+(delegate==null?"POST":delegate.selectProfileHash())+"|https443|same-request|literal3|unknown-role|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",MetroNextNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return site.host.equals(site.fileHost)?Set.of(site.host):Set.of(site.host,site.fileHost);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!site.parser.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return MetroNextNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafeUri(URI u){return u!=null&&u.toASCIIString().length()<=8192&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath())&&u.equals(u.normalize());}
    @Override public boolean selectApprovedRequest(URI u){try{if(!selectSafeUri(u))return false;if(delegate!=null)return delegate.selectApprovedRequest(u);return site.host.equals(u.getHost())&&u.equals(MetroNextNoticePage.selectDetailUri(site,u));}catch(IllegalArgumentException e){return false;}}
    private boolean selectName(String v){return v!=null&&!v.isBlank()&&v.length()<=500&&!v.contains("/")&&!v.contains("\\")&&!v.contains("..")&&!v.contains("%")&&v.indexOf('\ufffd')<0&&v.codePoints().noneMatch(Character::isISOControl);}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());if(delegate!=null||!"POST".equals(r.method())||!selectSafeUri(r.uri())||!site.fileHost.equals(r.uri().getHost())||!DOWNLOAD.equals(r.uri().getPath())||r.uri().getRawQuery()!=null)return false;var f=r.form();return f.keySet().equals(FIELDS)&&selectName(f.get("user_file_nm"))&&selectName(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1048576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element area;try{area=MetroNextNoticePage.selectAttachments(site,page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(delegate!=null){var f=new Element("form").attr("name","form1").attr("method","post");var row=f.appendElement("table").appendElement("tr");row.appendElement("th").text("첨부파일");row.appendElement("td").html(area.html());return delegate.selectDescriptors(s,f.outerHtml());}
        var forms=page.select("form#fileForm[name=fileForm][method=post]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();if(!("https://"+site.fileHost+DOWNLOAD).equals(form.attr("action"))||form.childrenSize()!=3||form.select(":root > input[type=hidden]").size()!=3||form.children().stream().anyMatch(e->!e.val().isEmpty())||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FIELDS))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey);
        for(var a:area.select("a[href^=javascript:fn_egov_downFile(]"))try{var args=AttachmentDownloadInvocation.selectArguments(a.attr("href"),"fn_egov_downFile",false,false);if(args.size()!=3||a.hasAttr("onclick")||!args.get(0).equals(a.text().strip()))continue;var request=new Request(URI.create("https://"+site.fileHost+DOWNLOAD),"POST",Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));if(!selectApprovedRequest(request))continue;String id=normalizer.hash(args.get(2)+"\n"+args.get(1)),format=selectFormat(args.get(0));boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));var d=new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),args.get(0),supported?format:null,"UNKNOWN",supported,request.form());var old=files.get(id);if(old!=null){if(!old.equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(id,d);
            var residue=a.clone();for(var icon:residue.select(":root > i.ir.ir-bbs.ir-file.left"))if(icon.text().isBlank()&&icon.children().isEmpty()&&icon.attributes().size()==1)icon.remove();if(!residue.children().isEmpty())unresolved=true;a.remove();
            for(var preview:area.select("a[href^=javascript:fn_egov_preview_File(]")){String raw=preview.attr("href"),prefix="javascript:fn_egov_preview_File(";if(!preview.hasAttr("onclick")&&"미리보기".equals(preview.text().strip())&&args.equals(AttachmentDownloadInvocation.selectArguments("javascript:fn_egov_downFile("+raw.substring(prefix.length()),"fn_egov_downFile",false,false)))preview.remove();}
        }catch(IllegalArgumentException e){unresolved=true;}
        if(!area.text().isBlank()||!area.select("a,button,input,form,iframe,object,embed,script,img,[onclick],[href]").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());}
    private String selectFormat(String n){String e=n.contains(".")?n.substring(n.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(e)?e:null;}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
