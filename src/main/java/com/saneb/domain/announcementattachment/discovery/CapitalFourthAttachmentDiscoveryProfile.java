package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 첨부의 데이터 속성/직접 링크/고정 리터럴 호출을 GET으로 연결한다. */
final class CapitalFourthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    CapitalFourthAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_PORTAL_V1";hash=AttachmentProfileFingerprint.selectHash("CAPITAL_FOURTH:1|"+s+"|"+s.sourceCode+"|SAEOL_GOSI|"+s.host+"|"+s.fileHost+"|"+s.download+"|GET|https443|same-request|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",CapitalFourthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,"SAEOL_GOSI"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!"SAEOL_GOSI".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return CapitalFourthNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafeName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectSafeValues(Map<String,String> q){return q.keySet().equals(FIELDS)&&selectSafeName(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))&&q.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    @Override public boolean selectApprovedRequest(URI u){try{if(u==null||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!u.equals(u.normalize())||u.getRawPath()==null||!u.getRawPath().equals(u.getPath()))return false;
        if(site.host.equals(u.getHost()))return u.equals(CapitalFourthNoticePage.selectDetailUri(site,u));return site.fileHost.equals(u.getHost())&&site.download.equals(u.getPath())&&selectSafeValues(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){var detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try{cell=CapitalFourthNoticePage.selectAttachments(site,page);}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        // 페이지의 실행 코드/장식은 실행하지 않는다. 파일 영역의 실제 링크·폼·버튼은 별도로 대조한다.
        cell=cell.clone();cell.select("script,style").remove();var lists=cell.select(site==Site.GIMPO?"ul.p-attach":site==Site.DONGDUCHEON?"ul.view_attach":"ul#updateFileList.file_list");if(lists.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");var list=lists.getFirst();
        var outside=cell.clone();outside.select("ul").remove();if(site==Site.DONGDUCHEON)outside.select("span").stream().filter(e->"※ 게재기간이 지난 첨부파일은 다운로드가 불가하오니 담당부서로 문의하시기 바랍니다.".equals(e.text())).forEach(Element::remove);
        boolean unresolved=!outside.text().isBlank()||!outside.select("a,button,input,img,iframe,form,object,embed,[onclick]").isEmpty();boolean exceeded=false;var files=new LinkedHashMap<String,Descriptor>();
        if(!list.ownText().isBlank()||list.children().stream().anyMatch(e->!"li".equals(e.tagName())))unresolved=true;
        for(var item:list.children())try{if(!"li".equals(item.tagName()))throw new IllegalArgumentException();String selector=site==Site.GIMPO?"a.p-attach__link.ntfc_file_down":site==Site.DONGDUCHEON?"div.down_view > a.file_down":"span.view_list_file > a";
            var anchors=item.select(selector);if(anchors.size()!=1)throw new IllegalArgumentException();var a=anchors.getFirst();Map<String,String> v;
            if(site==Site.GIMPO){if(!"#n".equals(a.attr("href"))||a.hasAttr("onclick"))throw new IllegalArgumentException();v=Map.of("user_file_nm",a.attr("data-user-file-nm"),"sys_file_nm",a.attr("data-sys-file-nm"),"file_path",a.attr("data-file-path"));}
            else if(site==Site.DONGDUCHEON){if(a.hasAttr("onclick"))throw new IllegalArgumentException();var u=detail.resolve(URI.create(a.attr("href").replace(" ","%20")));if(!site.fileHost.equals(u.getHost())||!selectApprovedRequest(u))throw new IllegalArgumentException();v=CapitalThirdNoticePage.selectParameters(u.getRawQuery());}
            else{String raw=a.attr("onclick");if(!"#".equals(a.attr("href"))||raw.length()>8192||!raw.startsWith("goDownload("))throw new IllegalArgumentException();var args=AttachmentDownloadInvocation.selectArguments("goDownLoad"+raw.substring("goDownload".length()),"goDownLoad",true,true);if(args.size()!=3)throw new IllegalArgumentException();v=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));}
            if(!selectSafeValues(v))throw new IllegalArgumentException();String name=v.get("user_file_nm");var label=site==Site.DONGDUCHEON?a.parent().select("span"):null;
            if(site==Site.DONGDUCHEON){if(label.size()!=1||!name.equals(label.getFirst().text()))throw new IllegalArgumentException();}
            else{var text=a.clone();text.select("span.p-icon,i.p-icon").remove();if(!name.equals(text.text().strip()))throw new IllegalArgumentException();}
            var q=new StringJoiner("&");v.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->q.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));URI uri=URI.create("https://"+site.fileHost+site.download+"?"+q);if(!selectApprovedRequest(uri))throw new IllegalArgumentException();
            String id=normalizer.hash(v.get("file_path")+"\n"+v.get("sys_file_nm"));if(files.containsKey(id)){if(!files.get(id).fetchUri().equals(uri)||!files.get(id).displayName().equals(name))unresolved=true;}
            else if(files.size()==10)exceeded=true;else{String ext=selectFormat(name);boolean supported=ext!=null&&ext.equals(selectFormat(v.get("sys_file_nm")));files.put(id,new Descriptor(uri,new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey))),name,supported?ext:null,"UNKNOWN",supported));}
            var residue=item.clone();residue.select(selector).remove();
            if(site==Site.DONGDUCHEON)residue.select("div.down_view > span").remove();
            if(site==Site.PYEONGTAEK){String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey);for(var preview:residue.select("a.btn_white.none")){
                String expected="fn_egov_gosi_preview('"+notice+"','"+notice+"-"+item.elementSiblingIndex()+"."+name.substring(name.lastIndexOf('.')+1)+"','"+name+"','"+v.get("sys_file_nm")+"','"+v.get("file_path")+"'); return false;";
                if("#".equals(preview.attr("href"))&&expected.equals(preview.attr("onclick"))&&"미리보기/음성듣기".equals(preview.text()))preview.remove();}}
            // 평택 링크 자체의 검증된 onclick은 허용하되 하위 요소의 실행 동작은 허용하지 않는다.
            if(!residue.text().isBlank()||!residue.select("a,button,input,img,iframe,form,object,embed,[onclick]").isEmpty()||a.select("script,input,button,iframe,object,embed,[onclick]").stream().anyMatch(e->e!=a))unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private String selectFormat(String name){String x=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(x)?x:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
