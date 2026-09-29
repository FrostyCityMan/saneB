package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage.Site;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 미리보기 오류·미지원 파일을 정상 공개 다운로드와 분리한다. 내부망 주소는 호출하지 않는다. */
final class SeoulFifthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern TWO_ARGS=Pattern.compile("^fn_goPreView\\('([^'\\\\\\r\\n]{1,6000})', ?'([^'\\\\\\r\\n]{1,2000})'\\);?$");
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    SeoulFifthAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_BOARD_V1";hash=AttachmentProfileFingerprint.selectHash("SEOUL_FIFTH:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|"+s.host+"|"+s.fileHost+"|"+s.download+"|https443|same-request|plain3|preview-no-fetch|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",SeoulFifthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!site.parser.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return SeoulFifthNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    private boolean selectSafeName(String n){return n!=null&&!n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&!n.contains("%")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectSafeValues(Map<String,String> q){return q.keySet().equals(FIELDS)&&selectSafeName(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))&&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    @Override public boolean selectApprovedRequest(URI u){try{if(!selectSafeUri(u))return false;if(site.host.equals(u.getHost()))return u.equals(SeoulFifthNoticePage.selectDetailUri(site,u));return site!=Site.DONGDAEMUN&&site.fileHost.equals(u.getHost())&&site.download.equals(u.getPath())&&selectSafeValues(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());return site==Site.DONGDAEMUN&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())&&site.download.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectSafeValues(r.form());}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private Map<String,String> selectFileValues(String raw){if(raw==null||raw.length()>8192)throw new IllegalArgumentException();URI u=URI.create(raw.replace(" ","%20"));if(!selectSafeUri(u)||!site.fileHost.equals(u.getHost())||!site.download.equals(u.getPath()))throw new IllegalArgumentException();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!selectSafeValues(q))throw new IllegalArgumentException();return q;}
    private Request selectRequest(Map<String,String> q){URI base=URI.create("https://"+site.fileHost+site.download);if(site==Site.DONGDAEMUN)return new Request(base,"POST",q);var out=new StringJoiner("&");q.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->out.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));return Request.selectGet(URI.create(base+"?"+out));}
    private boolean selectPreview(Element e,Map<String,String> q){try{String raw=e.attr(site==Site.YEONGDEUNGPO?"href":"onclick");if(raw.length()>8192)return false;
        if(site==Site.DONGDAEMUN){if(!e.is("button.p-attach__preview")||e.hasAttr("formaction"))return false;var m=TWO_ARGS.matcher(raw);return m.matches()&&selectFileValues(m.group(1)).equals(q)&&URLDecoder.decode(m.group(2),StandardCharsets.UTF_8).equals(q.get("user_file_nm"));}
        String prefix=site==Site.SEONGBUK?"fn_goPreView":"javascript:fn_PreView";if(!raw.startsWith(prefix+"("))return false;if(site==Site.SEONGBUK?!"#n".equals(e.attr("href")):e.hasAttr("onclick"))return false;
        var args=AttachmentDownloadInvocation.selectArguments("javascript:goDownLoad"+raw.substring(prefix.length()),"goDownLoad",false,false);if(args.size()!=3||!selectFileValues(args.get(0)).equals(q))return false;
        return site==Site.SEONGBUK?args.get(1).equals(q.get("user_file_nm"))&&args.get(2).equals(q.get("sys_file_nm")):URLDecoder.decode(args.get(1),StandardCharsets.UTF_8).equals(q.get("sys_file_nm"))&&URLDecoder.decode(args.get(2),StandardCharsets.UTF_8).equals(q.get("user_file_nm"));
    }catch(IllegalArgumentException ex){return false;}}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element cell;try{cell=SeoulFifthNoticePage.selectAttachments(site,page);}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(site==Site.DONGDAEMUN){var forms=page.select("form[name=downForm][method=post]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var f=forms.getFirst();if(!("https://"+site.fileHost+site.download).equals(f.attr("action"))||f.childrenSize()!=3||f.select("input[type=hidden]").size()!=3||!f.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FIELDS)||f.children().stream().anyMatch(e->!e.val().isEmpty()))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");}
        String items=site==Site.DONGDAEMUN?":root > ul > li":site==Site.SEONGBUK?":root > ul.upload_list > li":":root > ul.p-attach > li.p-attach__item";var residual=cell.clone();residual.select(items).remove();boolean unresolved=!residual.text().isBlank()||!residual.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty(),exceeded=false;var found=new LinkedHashMap<String,Descriptor>();
        for(var item:cell.select(items))try{String selector=site==Site.DONGDAEMUN?"a[href^=javascript:goDownLoad(]":site==Site.SEONGBUK?"a.btn_file":"a.p-attach__link";var anchors=item.select(selector);if(anchors.size()!=1)throw new IllegalArgumentException();var a=anchors.getFirst();if(a.hasAttr("onclick"))throw new IllegalArgumentException();Map<String,String> q;
            if(site==Site.DONGDAEMUN){var args=AttachmentDownloadInvocation.selectArguments(a.attr("href"),"goDownLoad",false,false);if(args.size()!=3)throw new IllegalArgumentException();q=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));}else q=selectFileValues(a.attr("href"));
            if(!selectSafeValues(q))throw new IllegalArgumentException();String name=q.get("user_file_nm");var label=a.clone();label.select("span.p-icon,i.p-icon").remove();if(!name.equals(label.text().strip()))throw new IllegalArgumentException();var request=selectRequest(q);String id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm")),ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&q.get("sys_file_nm").toLowerCase(Locale.ROOT).endsWith("."+ext.toLowerCase(Locale.ROOT));var d=new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo"))),name,supported?ext:null,"UNKNOWN",supported,request.form());
            if(found.containsKey(id)){if(!found.get(id).equals(d))unresolved=true;}else if(found.size()==10)exceeded=true;else found.put(id,d);
            var remainder=item.clone();remainder.select(selector).remove();for(var preview:remainder.select(site==Site.SEONGBUK?"a.preview":".p-attach__preview"))if(selectPreview(preview,q))preview.remove();
            if(!remainder.text().isBlank()||!remainder.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty()||!a.select("script,input,button,img,iframe,object,embed,[onclick]").isEmpty())unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(found.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(found.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(found.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(found.values()),List.of());}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
