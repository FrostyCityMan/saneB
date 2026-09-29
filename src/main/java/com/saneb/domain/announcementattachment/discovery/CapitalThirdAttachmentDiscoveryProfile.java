package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.Site;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 첨부 셀의 파일만 수집한다. 미확인 링크/미리보기 오류는 정상 파일과 분리한다. */
final class CapitalThirdAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    CapitalThirdAttachmentDiscoveryProfile(Site site){this.site=site;code="LOCAL_"+site+"_PORTAL_V1";
        hash=AttachmentProfileFingerprint.selectHash("CAPITAL_THIRD:1|"+site+"|"+site.sourceCode+"|SAEOL_GOSI|"+site.host+"|"+site.fileHost+"|"+site.download
                +"|https443|same-request|plain3|paired-preview|unknown-role|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,"SAEOL_GOSI"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{
        if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!"SAEOL_GOSI".equals(s.listParserProfileCode())
                ||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();
        return CapitalThirdNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));
    }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath().equals(u.getPath());}
    private boolean selectSafeName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectSafeValues(Map<String,String> q){return q.keySet().equals(FIELDS)&&selectSafeName(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))&&q.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    @Override public boolean selectApprovedRequest(URI u){try{
        if(!selectSafeUri(u))return false;if(site.host.equals(u.getHost()))return u.equals(CapitalThirdNoticePage.selectDetailUri(site,u));
        return site!=Site.NAMYANGJU&&site.fileHost.equals(u.getHost())&&site.download.equals(u.getPath())&&selectSafeValues(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));
    }catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return selectApprovedRequest(r.uri());
        return site==Site.NAMYANGJU&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())&&site.download.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectSafeValues(r.form());}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    // 검증된 함수명 접두부만 표준 리터럴 파서에 전달한다. 외부 스크립트는 실행하지 않는다.
    private List<String> selectArguments(String raw,String function,boolean bare){
        if(raw==null||raw.length()>8192)return List.of();String prefix=(bare?"":"javascript:")+function;
        if(!raw.startsWith(prefix+"("))return List.of();return AttachmentDownloadInvocation.selectArguments("javascript:goDownLoad"+raw.substring(prefix.length()),"goDownLoad",false,false);
    }
    @Override public Result selectDescriptors(Source source,String html){
        var detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try{cell=CapitalThirdNoticePage.selectAttachments(site,page);}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(site==Site.NAMYANGJU){var forms=page.select("form[name=nnn][method=post]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var f=forms.getFirst();
            if(!("https://"+site.fileHost+site.download).equals(f.attr("action"))||f.childrenSize()!=3||f.select("input[type=hidden]").size()!=3
                    ||!f.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FIELDS)||f.children().stream().anyMatch(e->!e.val().isEmpty()))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");}
        var remaining=cell.clone();remaining.select("li.p-attach__item").remove();boolean unresolved=!remaining.text().isBlank()||!remaining.select("a,button,script,img,input,iframe,form,[onclick]").isEmpty();
        var files=new LinkedHashMap<String,Descriptor>();boolean exceeded=false;
        for(var item:cell.select("li.p-attach__item"))try{
            var links=item.select(site==Site.GURI?"a.p-attach__link":"a.p-attach__down");if(links.size()!=1)throw new IllegalArgumentException();var a=links.getFirst();if(a.hasAttr("onclick"))throw new IllegalArgumentException();
            Map<String,String> values;
            if(site==Site.GURI){var u=detail.resolve(URI.create(a.attr("href").replace(" ","%20")));if(!site.fileHost.equals(u.getHost())||!selectApprovedRequest(u))throw new IllegalArgumentException();values=CapitalThirdNoticePage.selectParameters(u.getRawQuery());}
            else{var args=selectArguments(a.attr("href"),site==Site.HANAM?"gourl":"goDownLoad",false);if(args.size()!=3)throw new IllegalArgumentException();values=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));}
            if(!selectSafeValues(values))throw new IllegalArgumentException();String name=values.get("user_file_nm");
            if(site==Site.GURI){var label=a.clone();label.select("span.p-icon,i.p-icon").remove();if(!name.equals(label.text().strip()))throw new IllegalArgumentException();}
            else if(item.select("span.p-attach__text").size()!=1||!name.equals(item.selectFirst("span.p-attach__text").text().strip()))throw new IllegalArgumentException();
            var request=selectFileRequest(values);String id=normalizer.hash(values.get("file_path")+"\n"+values.get("sys_file_nm"));
            if(files.containsKey(id)){if(!files.get(id).selectRequest().equals(request)||!files.get(id).displayName().equals(name))unresolved=true;}
            else if(files.size()==10)exceeded=true;
            else{String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(values.get("sys_file_nm")));
                files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey))),name,supported?format:null,"UNKNOWN",supported,request.form()));}
            var previews=item.select(".p-attach__preview");for(var preview:previews){String raw=preview.attr(site==Site.HANAM?"onclick":"href");var args=selectArguments(raw,site==Site.HANAM?"fn_goPreView":"goPreviewFile",site==Site.HANAM);
                if(site==Site.GURI||!args.equals(List.of(values.get("user_file_nm"),values.get("sys_file_nm"),values.get("file_path"))))unresolved=true;}
            var residue=item.clone();residue.select("a.p-attach__link,a.p-attach__down,span.p-attach__text,.p-attach__preview").remove();
            if(!residue.text().isBlank()||!residue.select("a,button,input,script,iframe,object,embed,img,form,[onclick],[href]").isEmpty()||!a.select("script,input,button,iframe,object,embed,[onclick]").isEmpty())unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private Request selectFileRequest(Map<String,String> values){URI base=URI.create("https://"+site.fileHost+site.download);if(site==Site.NAMYANGJU)return new Request(base,"POST",values);
        var q=new StringJoiner("&");values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->q.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));return Request.selectGet(URI.create(base+"?"+q));}
    private String selectFormat(String name){String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
