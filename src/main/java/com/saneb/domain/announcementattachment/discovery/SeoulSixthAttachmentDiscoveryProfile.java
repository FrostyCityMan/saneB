package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage.Site;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 양천의 고정 다운로드 호출과 관악의 파일 링크만 읽고 미리보기·점자·음성은 요청하지 않는다. */
final class SeoulSixthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Pattern ACCESSIBLE=Pattern.compile("^viewAttachFileBraille(?:4Speech)?\\('([^'\\\\\\r\\n]{1,2048})'[ \\t]*,[ \\t]*'([^'\\\\\\r\\n]{1,1000})'\\);[ \\t]*return false;?$");
    private final Site site;private final String code,hash;
    private final SaeolGetAttachmentDiscoveryProfile fileValidator;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    SeoulSixthAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_BOARD_V1";fileValidator=new SaeolGetAttachmentDiscoveryProfile(code,s.sourceCode,s.fileHost,s.parser,"td",false);hash=AttachmentProfileFingerprint.selectHash("SEOUL_SIXTH:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|"+s.host+"|"+fileValidator.selectProfileHash()+"|https443|same-request|preview-no-fetch|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",SeoulSixthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!site.parser.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return SeoulSixthNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI u){try{if(u==null)return false;if(site.host.equals(u.getHost()))return u.equals(SeoulSixthNoticePage.selectDetailUri(site,u));return DOWNLOAD.equals(u.getPath())&&fileValidator.selectApprovedRequest(u);}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private URI selectEncoded(Map<String,String> q){var out=new StringJoiner("&");q.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->out.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));URI u=URI.create("https://"+site.fileHost+DOWNLOAD+"?"+out);if(!selectApprovedRequest(u))throw new IllegalArgumentException();return u;}
    private URI selectFile(Element a){if(site==Site.YANGCHEON){if(!"#".equals(a.attr("href")))throw new IllegalArgumentException();String call=a.attr("onclick");if(!call.startsWith("doUrlDownload("))throw new IllegalArgumentException();var args=AttachmentDownloadInvocation.selectArguments("javascript:goDownLoad"+call.substring("doUrlDownload".length()),"goDownLoad",false,false);if(args.size()!=3)throw new IllegalArgumentException();return selectEncoded(Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));}
        if(a.hasAttr("onclick"))throw new IllegalArgumentException();String raw=a.attr("href");if(raw.length()>8192)throw new IllegalArgumentException();URI u=URI.create(raw.replace(" ","%20"));if(!selectApprovedRequest(u)||!DOWNLOAD.equals(u.getPath()))throw new IllegalArgumentException();return selectEncoded(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));}
    private boolean selectPreview(Element a,Map<String,String> file,String id){try{if(!a.hasClass("viewbtns"))return false;String system=file.get("sys_file_nm"),ext=system.substring(system.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);var suffix=Pattern.compile("_([1-9][0-9]?)\\.[a-zA-Z0-9]+$").matcher(system);if(!suffix.find())return false;String stored=id+"-"+suffix.group(1)+"."+ext;Map<String,String> expected=Map.of("not_ancmt_mgt_no",id,"streFileNm",stored);
        if(!a.hasAttr("onclick")){String raw=a.attr("href");if(!raw.startsWith("/synapGosiView.do?")||raw.length()>4096||raw.contains("#"))return false;return CapitalThirdNoticePage.selectParameters(raw.substring(raw.indexOf('?')+1)).equals(expected);}
        if(!"#".equals(a.attr("href")))return false;String call=a.attr("onclick");if(call.length()>4096)return false;var match=ACCESSIBLE.matcher(call);if(!match.matches())return false;var q=CapitalThirdNoticePage.selectParameters(match.group(1));return q.size()==3&&q.entrySet().containsAll(expected.entrySet())&&"gosi".equals(q.get("Downtype"))&&file.get("user_file_nm").strip().equals(match.group(2));
    }catch(IllegalArgumentException e){return false;}}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element cell;try{cell=SeoulSixthNoticePage.selectAttachments(site,page);}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        String items=site==Site.YANGCHEON?":root > ul.attached-files > li":":root > ul > li";var residual=cell.clone();residual.select(items).remove();boolean unresolved=selectResidual(residual),exceeded=false;var found=new LinkedHashMap<String,Descriptor>();String noticeId=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no");
        for(var item:cell.select(items))try{String selector=site==Site.YANGCHEON?"span > a.file":"a.filedown";var anchors=item.select(selector);if(anchors.size()!=1)throw new IllegalArgumentException();var a=anchors.getFirst();URI fetch=selectFile(a);var q=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());String name=q.get("user_file_nm").strip();if(!name.equals(a.text().strip()))throw new IllegalArgumentException();String id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm")),ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&q.get("sys_file_nm").toLowerCase(Locale.ROOT).endsWith("."+ext.toLowerCase(Locale.ROOT));var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("attachmentId",id,"noticeId",noticeId)),name,supported?ext:null,"UNKNOWN",supported);
            if(found.containsKey(id)){if(!found.get(id).equals(d))unresolved=true;}else if(found.size()==10)exceeded=true;else found.put(id,d);
            var remainder=item.clone();remainder.select(selector).remove();if(site==Site.GWANAK)for(var preview:remainder.select("a.viewbtns"))if(selectPreview(preview,q,noticeId))preview.remove();if(selectResidual(remainder)||!a.clone().removeAttr("onclick").select("script,input,button,img,iframe,object,embed,[onclick]").isEmpty())unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(found.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(found.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(found.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(found.values()),List.of());}
    private boolean selectResidual(Element e){return !e.text().isBlank()||!e.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty();}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
