package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulEighthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulEighthNoticePage.Site;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 첨부 셀만 해석한다. 도봉은 공개 다운로드 사전 확인이 OK일 때만 파일을 요청한다. */
public final class SeoulEighthAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String GANGNAM_HOST="gangnam.eminwon.seoul.kr",GANGNAM_FILE="/emwp/jsp/ofr/FileDown_gn.jsp";
    private static final String DOBONG_FILE="/WDB_common/include/download_unitsvc_gosing.asp",DOBONG_CHECK="/WDB_DEV/gosigong_go/ajax_user_attach.asp";
    private static final Pattern DOBONG_CALL=Pattern.compile("javascript:filedown\\(\\s*([1-9][0-9]{0,14})\\s*,\\s*([1-9][0-9]{0,14})\\s*\\);?");
    private final Site site;
    private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    public SeoulEighthAttachmentDiscoveryProfile(Site site){this.site=Objects.requireNonNull(site);code="LOCAL_"+site.name()+"_BOARD_V1";
        hash=AttachmentProfileFingerprint.selectHash("SEOUL_EIGHTH:1|"+site+"|"+site.source+"|"+site.parser+"|official-area|paired-preview|partial-preserved|limit10|unknown-role|exact-requests|dobong-check-before-file|"
                +AttachmentProfileFingerprint.selectHash("PAGE:1",SeoulEighthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("SITE:1",Site.class)+"|"
                +AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.source,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return site==Site.GANGNAM?Set.of(site.host,GANGNAM_HOST):Set.of(site.host);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){try{
        if(source==null||!selectProviderCode().equals(source.providerCode())||!site.source.equals(source.localSourceCode())||!site.parser.equals(source.listParserProfileCode())
                ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
        return SeoulEighthNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));
    }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean safe(URI uri){return uri!=null&&"https".equals(uri.getScheme())&&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null&&uri.getFragment()==null
            &&uri.getRawPath()!=null&&uri.getRawPath().equals(uri.getPath())&&uri.equals(uri.normalize())&&uri.toASCIIString().length()<=8192;}
    @Override public boolean selectApprovedRequest(URI uri){try{
        if(!safe(uri))return false;if(SeoulEighthNoticePage.selectSite(uri)==site)return uri.equals(SeoulEighthNoticePage.selectDetailUri(site,uri));
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(site==Site.DOBONG)return site.host.equals(uri.getHost())&&DOBONG_FILE.equals(uri.getPath())&&q.keySet().equals(Set.of("fcode","bcode"))&&numeric(q.get("fcode"))&&numeric(q.get("bcode"));
        return GANGNAM_HOST.equals(uri.getHost())&&GANGNAM_FILE.equals(uri.getPath())&&q.keySet().equals(Set.of("user_file_nm","sys_file_nm","file_path"))
                &&safeName(q.get("user_file_nm"))&&safeName(q.get("sys_file_nm"))&&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());
        return site==Site.DOBONG&&"POST".equals(r.method())&&safe(r.uri())&&site.host.equals(r.uri().getHost())&&DOBONG_CHECK.equals(r.uri().getPath())&&r.uri().getRawQuery()==null
                &&r.form().keySet().equals(Set.of("idx"))&&numeric(r.form().get("idx"));}
    @Override public boolean selectApprovedRequest(Request initial,Request next){try{
        if(initial==null||next==null||!selectApprovedRequest(initial)||!selectApprovedRequest(next))return false;
        if(initial.equals(next))return true;
        return site==Site.DOBONG&&"GET".equals(initial.method())&&DOBONG_FILE.equals(initial.uri().getPath())&&"POST".equals(next.method())
                &&CapitalThirdNoticePage.selectParameters(initial.uri().getRawQuery()).get("bcode").equals(next.form().get("idx"));
    }catch(IllegalArgumentException e){return false;}}
    @Override public AttachmentPinnedDownloadClient.Download selectDownload(Request initial,Path output,long maximumBytes,Operation operation)throws IOException{
        if(site!=Site.DOBONG||!DOBONG_FILE.equals(initial.uri().getPath()))return operation.selectDownload(initial,maximumBytes);
        String notice=CapitalThirdNoticePage.selectParameters(initial.uri().getRawQuery()).get("bcode");
        var check=new Request(URI.create("https://"+site.host+DOBONG_CHECK),"POST",Map.of("idx",notice));
        var response=operation.selectDownload(check,Math.min(maximumBytes,256));String status;
        try{
            if(Files.size(output)>256||response.contentType()==null||!response.contentType().toLowerCase(Locale.ROOT).matches("text/(?:html|plain)(?:;.*)?"))throw new IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
            status=Files.readString(output,StandardCharsets.UTF_8).strip();
        }finally{Files.deleteIfExists(output);}
        if(!"OK".equals(status))throw new IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        return operation.selectDownload(initial,maximumBytes);
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1048576)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=SeoulEighthNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()),site).clone();}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.id);var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        for(var item:area.select(site==Site.GANGNAM?":root > ul > li":":root > div.file_list"))try{
            var anchors=item.select(":root > a[href]").stream().filter(a->!a.hasClass(site==Site.GANGNAM?"btn-preview":"file_icon")).toList();if(anchors.size()!=1)continue;
            var a=anchors.getFirst();if(a.hasAttr("onclick"))continue;String name,identity;URI file;Map<String,String> q;
            if(site==Site.GANGNAM){
                if(a.attr("href").length()>8192)continue;URI raw=detail.resolve(a.attr("href").replace(" ","%20"));if(!GANGNAM_FILE.equals(raw.getPath())||!selectApprovedRequest(raw))continue;
                q=CapitalThirdNoticePage.selectParameters(raw.getRawQuery());name=q.get("user_file_nm");if(!name.equals(a.text().strip()))continue;
                var query=new StringJoiner("&");q.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
                file=URI.create("https://"+GANGNAM_HOST+GANGNAM_FILE+"?"+query);identity=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));
            }else{
                var call=DOBONG_CALL.matcher(a.attr("href"));if(!call.matches()||!notice.equals(call.group(2)))continue;
                name=a.text().replaceFirst("\\s+\\([0-9.,]+\\s*(?:KB|MB|Bytes?)\\)$","").strip();
                if(!a.attr("title").equals(name+" 다운로드"))continue;
                q=Map.of("fcode",call.group(1),"bcode",notice);file=URI.create("https://"+site.host+DOBONG_FILE+"?fcode="+call.group(1)+"&bcode="+notice);identity=normalizer.hash(notice+"\n"+call.group(1));
            }
            if(!safeName(name)||!selectApprovedRequest(file))continue;String format=format(name);boolean supported=format!=null&&(site==Site.DOBONG||format.equals(format(q.get("sys_file_nm"))));
            var d=new Descriptor(file,new AttachmentSetEvidence.Locator(code,file.getPath(),Map.of("noticeId",notice,"attachmentId",identity)),name,supported?format:null,"UNKNOWN",supported);
            var old=files.get(identity);if(old!=null){if(!old.equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(identity,d);
            var residual=item.clone();residual.select(":root > a:not(.btn-preview):not(.file_icon)").remove();
            if(!a.select("button,input,iframe,object,embed,script,[onclick],[onerror]").isEmpty())unresolved=true;
            for(var preview:residual.select(":root > a"))if(validPreview(preview,q,detail))preview.remove();
            if(residual(residual))unresolved=true;item.remove();
        }catch(IllegalArgumentException e){unresolved=true;}
        if(residual(area))unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean validPreview(Element a,Map<String,String> q,URI detail){
        if(site==Site.DOBONG)return a.hasClass("file_icon")&&!a.hasAttr("onclick")&&a.attr("href").equals("javascript:attach_docview_unitsvc('"+q.get("bcode")+"','"+q.get("fcode")+"','12');")
                &&a.text().isBlank()&&a.childrenSize()==1&&a.child(0).is("img")&&"미리보기".equals(a.child(0).attr("alt"))&&!a.child(0).hasAttr("onerror");
        var stored=Pattern.compile(".*_(ofr_ofr_[A-Za-z0-9]+_[0-9]{17})_([1-9][0-9]{0,3})\\.[A-Za-z0-9]+$").matcher(q.get("sys_file_nm"));
        if(!stored.matches())return false;URI preview=detail.resolve(a.attr("href"));
        return a.hasClass("btn-preview")&&safe(preview)&&site.host.equals(preview.getHost())&&preview.getRawQuery()==null
                &&preview.getPath().equals("/file/"+stored.group(2)+"/get/"+stored.group(1)+"/noticePreview.do")
                &&"미리보기".equals(a.text().strip())&&"window.open(this.href, '미리보기');return false;".equals(a.attr("onclick"))&&a.children().isEmpty();
    }
    private boolean numeric(String value){return value!=null&&value.matches("[1-9][0-9]{0,14}");}
    private boolean safeName(String value){return value!=null&&!value.isBlank()&&value.length()<=500&&!value.contains("/")&&!value.contains("\\")&&!value.contains("..")&&!value.contains("%")&&value.indexOf('\ufffd')<0&&value.codePoints().noneMatch(Character::isISOControl);}
    private String format(String name){String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private boolean residual(Element e){return !e.text().isBlank()||!e.select("a,button,input,select,form,iframe,object,embed,script,img,[onclick],[href]").isEmpty();}
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
