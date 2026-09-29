package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 다운로드 주소·고정 세 인자만 읽고 미리보기/JavaScript는 실행하지 않는다. */
final class ChungcheongThirdAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FORM=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site; private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    ChungcheongThirdAttachmentDiscoveryProfile(Site s){
        site=s;code="LOCAL_"+s+"_BOARD_V1";
        hash=AttachmentProfileFingerprint.selectHash("CHUNGCHEONG_THIRD:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|"+s.host+"|"+s.fileHost+"|"+s.download+"|https443|same-request|paired-preview|limit10|unknown-role|"
                +AttachmentProfileFingerprint.selectHash("PAGE:1",ChungcheongThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!site.parser.equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            return ChungcheongThirdNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    private boolean selectSafeName(String n){return n!=null&&!n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&!n.contains("%")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectSafeForm(Map<String,String> q){return q.keySet().equals(FORM)&&selectSafeName(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))&&q.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    private boolean selectSafeQuery(Map<String,String> q){return q.keySet().equals(Set.of("mode","fid","index","other"))&&"download".equals(q.get("mode"))&&q.getOrDefault("index","").matches("[0-9]{1,3}")&&q.getOrDefault("fid","").matches("#[a-f0-9]{64}")&&q.getOrDefault("other","").matches("#[a-f0-9]{96}");}
    @Override public boolean selectApprovedRequest(URI u){
        try{if(!selectSafeUri(u))return false;if(site.host.equals(u.getHost()))return u.equals(ChungcheongThirdNoticePage.selectDetailUri(site,u));
            return site==Site.CHUNGBUK&&site.fileHost.equals(u.getHost())&&site.download.equals(u.getPath())&&selectSafeQuery(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());
        return site==Site.GONGJU&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())&&site.download.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectSafeForm(r.form());
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try{cell=ChungcheongThirdNoticePage.selectAttachments(site,page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(site==Site.GONGJU){
            var forms=page.select("form#fileForm[name=fileForm][method=post]");
            if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var f=forms.getFirst();
            if(!("https://"+site.fileHost+site.download).equals(f.attr("action"))||f.childrenSize()!=3||f.select("input[type=hidden]").size()!=3||f.children().stream().anyMatch(e->!e.val().isEmpty())||!f.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FORM))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }else cell.select(":root > span.attach_tit").remove();
        cell.select("script,style").remove();var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        var items=site==Site.CHUNGBUK?cell.select(":root > div.attach_list > div.attach__item"):cell.select("a[href^=javascript:fn_egov_downFile(]");
        var remainder=cell.clone();
        for(var item:items)try{
            Request request;String name,id;List<String> args=List.of();Map<String,String> query=Map.of();
            if(site==Site.CHUNGBUK){
                var names=item.select(":root > span.text > em");var links=item.select("a.attach_btn.down[href]");
                if(names.size()!=1||links.size()!=1||links.getFirst().hasAttr("onclick"))throw new IllegalArgumentException();
                name=names.getFirst().text().strip();request=Request.selectGet(detail.resolve(links.getFirst().attr("href")));query=CapitalThirdNoticePage.selectParameters(request.uri().getRawQuery());
                id=normalizer.hash(query.get("fid")+"\n"+query.get("index")+"\n"+query.get("other"));
            }else{
                if(item.hasAttr("onclick"))throw new IllegalArgumentException();args=AttachmentDownloadInvocation.selectArguments(item.attr("href"),"fn_egov_downFile",false,false);
                if(args.size()!=3)throw new IllegalArgumentException();name=args.get(0);if(!name.equals(item.text().strip()))throw new IllegalArgumentException();
                request=new Request(URI.create("https://"+site.fileHost+site.download),"POST",Map.of("user_file_nm",name,"sys_file_nm",args.get(1),"file_path",args.get(2)));id=normalizer.hash(args.get(2)+"\n"+args.get(1));
            }
            if(!selectSafeName(name)||!selectApprovedRequest(request))throw new IllegalArgumentException();
            if(files.containsKey(id)){if(!files.get(id).selectRequest().equals(request)||!files.get(id).displayName().equals(name))unresolved=true;}
            else if(files.size()==10)exceeded=true;
            else{String format=selectFormat(name);boolean supported=format!=null&&(site==Site.CHUNGBUK||format.equals(selectFormat(args.get(1))));
                files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey))),name,supported?format:null,"UNKNOWN",supported,request.form()));}
            var residue=site==Site.CHUNGBUK?item.clone():remainder;
            if(site==Site.CHUNGBUK){residue.select(":root > span.text,a.attach_btn.down").remove();for(var preview:residue.select("a.attach_btn.preview"))if(selectChungbukPreview(preview,detail,query))preview.remove();if(selectResidue(residue))unresolved=true;}
            else{
                final String href=item.attr("href");remainder.select("a").stream().filter(e->href.equals(e.attr("href"))).forEach(Element::remove);
                for(var preview:remainder.select("a"))if(selectGongjuPreview(preview,args))preview.remove();
                if(!item.select("input,button,img,iframe,object,embed,[onclick]").isEmpty())unresolved=true;
            }
        }catch(IllegalArgumentException e){unresolved=true;}
        if(site==Site.CHUNGBUK)remainder.select(":root > div.attach_list > div.attach__item").remove();
        if(selectResidue(remainder))unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectChungbukPreview(Element e,URI detail,Map<String,String> file){
        try{URI u=detail.resolve(e.attr("href"));if(e.hasAttr("onclick")||!"미리보기".equals(e.text())||!selectSafeUri(u)||!site.host.equals(u.getHost())||!"/www/previewGosiPblancAtchmnfl.do".equals(u.getPath()))return false;
            var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());return q.equals(Map.of("no",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("no"),"fileIndex",file.get("index"),"fileFid",file.get("fid"),"fileOther",file.get("other")));
        }catch(IllegalArgumentException e1){return false;}
    }
    private boolean selectGongjuPreview(Element e,List<String> args){
        String raw=e.attr("href");String prefix="javascript:fn_egov_preview_File(";
        return !e.hasAttr("onclick")&&"파일 바로보기".equals(e.text())&&raw.startsWith(prefix)&&args.equals(AttachmentDownloadInvocation.selectArguments("javascript:fn_egov_downFile("+raw.substring(prefix.length()),"fn_egov_downFile",false,false));
    }
    private boolean selectResidue(Element e){return !e.text().isBlank()||!e.select("a,button,input,img,iframe,form,object,embed,[onclick],[href]").isEmpty();}
    private String selectFormat(String n){String ext=n.contains(".")?n.substring(n.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
