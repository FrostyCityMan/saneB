package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 포털 새올의 공식 첨부 영역을 공유한다. 하나의 미해석 링크가 정상 파일까지 버리지 않는다. */
final class GyeongbukPortalAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String DETAIL="/portal/saeol/gosi/view.do",DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final String code,sourceCode,parserCode,host,fileHost,menuKey,menuId,titleSelector,hash;
    private final boolean post;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();

    GyeongbukPortalAttachmentDiscoveryProfile(String region,String sourceCode,String parserCode,String domain,String menuKey,String menuId,boolean post,boolean subject){
        this.code="LOCAL_"+region+"_PORTAL_V1";this.sourceCode=sourceCode;this.parserCode=parserCode;
        this.host="www."+domain;this.fileHost="eminwon."+domain;this.menuKey=menuKey;this.menuId=menuId;this.post=post;
        this.titleSelector="form#detailForm div.bod_view > "+(subject?"div.subject":"h4");
        this.hash=AttachmentProfileFingerprint.selectHash(String.join("|",code,"1",sourceCode,parserCode,host,fileHost,menuKey,menuId,Boolean.toString(post),titleSelector,DETAIL,DOWNLOAD,"view-file-dt-dd|exact-source-query|https-only|fixed-3-args|paired-preview|partial-preserved|limit10"),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(host,fileHost);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parserCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!sourceCode.equals(source.localSourceCode())||!parserCode.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());if(!DETAIL.equals(uri.getPath())||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        if(!selectSafeUri(uri))return false;var q=selectQuery(uri.getRawQuery());
        if(host.equals(uri.getHost())&&DETAIL.equals(uri.getPath()))return q.keySet().equals(Set.of("notAncmtMgtNo",menuKey))&&menuId.equals(q.get(menuKey))&&q.get("notAncmtMgtNo").matches("[0-9]{1,15}");
        return !post&&fileHost.equals(uri.getHost())&&DOWNLOAD.equals(uri.getPath())&&selectFileFields(q);
    }
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null)return false;if("GET".equals(r.method()))return selectApprovedRequest(r.uri());
        return post&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&fileHost.equals(r.uri().getHost())&&DOWNLOAD.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&selectFileFields(r.form());
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(initial);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());var titles=page.select(titleSelector);var areas=page.select("form#detailForm div.bod_view > dl.view_file");
        if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var area=areas.getFirst();var labels=area.children().stream().filter(e->"dt".equals(e.tagName())).toList();var containers=area.children().stream().filter(e->"dd".equals(e.tagName())).toList();
        if(labels.size()!=1||containers.size()!=1||!"첨부파일".equals(labels.getFirst().text().replaceAll("[\\s\\u00a0:：]+","")))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if(post&&!selectPostForm(page))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var container=containers.getFirst();var residual=container.clone();residual.select("a").remove();
        boolean unresolved=!residual.text().isBlank()||!residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty();
        var files=new LinkedHashMap<String,Descriptor>();boolean exceeded=false;String notice=selectQuery(detail.getRawQuery()).get("notAncmtMgtNo");
        for(var anchor:container.select("a")){
            if(selectPairedPreview(anchor,notice))continue;
            var args=selectDownloadArguments(anchor);
            if(args.size()!=3||!anchor.text().strip().equals(args.getFirst())){unresolved=true;continue;}
            var fields=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));
            if(!selectFileFields(fields)){unresolved=true;continue;}
            Request request;
            try{
                String url="https://"+fileHost+DOWNLOAD;
                if(post)request=new Request(URI.create(url),"POST",fields);
                else request=new Request(URI.create(url+"?user_file_nm="+selectEncoded(args.get(0))+"&sys_file_nm="+selectEncoded(args.get(1))+"&file_path="+selectEncoded(args.get(2))),"GET",Map.of());
            }catch(IllegalArgumentException e){unresolved=true;continue;}
            if(!selectApprovedRequest(request)){unresolved=true;continue;}
            String identity=normalizer.hash(args.get(2)+"\n"+args.get(1));
            if(files.containsKey(identity)){if(!files.get(identity).displayName().equals(args.get(0))||!files.get(identity).selectRequest().equals(request))unresolved=true;continue;}
            if(files.size()==10){exceeded=true;continue;}
            String format=selectFormat(args.get(0));boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));
            var locator=new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",identity));
            files.put(identity,new Descriptor(request.uri(),locator,args.get(0),supported?format:null,"UNKNOWN",supported,request.form()));
        }
        if(files.isEmpty()&&!container.select("li").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private List<String> selectDownloadArguments(Element a){
        if(!"#".equals(a.attr("href")))return List.of();String call=a.attr("onclick").strip(),function=post?"goDownloadPost":"goDownload";
        if(!call.startsWith(function+"("))return List.of();
        return AttachmentDownloadInvocation.selectArguments("goDownLoad"+call.substring(function.length()),"goDownLoad",true,true);
    }
    private boolean selectPairedPreview(Element a,String notice){
        if(post||!"#".equals(a.attr("href"))||!"바로 보기".equals(a.text().strip()))return false;
        var match=Pattern.compile("^fn_egov_gosi_preview\\('"+notice+"','"+notice+"-[0-9]{1,3}\\.(?:pdf|hwp|hwpx)',").matcher(a.attr("onclick"));
        if(!match.find())return false;
        var args=AttachmentDownloadInvocation.selectArguments("goDownLoad("+a.attr("onclick").substring(match.end()),"goDownLoad",true,true);
        var previous=a.previousElementSibling();return args.size()==3&&previous!=null&&"a".equals(previous.tagName())&&args.equals(selectDownloadArguments(previous));
    }
    private boolean selectPostForm(Element page){
        var forms=page.select("form#fileDownFrm");if(forms.size()!=1)return false;var form=forms.getFirst();
        if(!"post".equalsIgnoreCase(form.attr("method"))||!("https://"+fileHost+DOWNLOAD).equals(form.attr("action"))||form.childrenSize()!=3)return false;
        var names=new HashSet<String>();for(var i:form.children())if(!"input".equals(i.tagName())||!"hidden".equals(i.attr("type"))||!i.val().isEmpty()||!names.add(i.attr("name"))||!i.attributes().asList().stream().allMatch(a->Set.of("type","name","value").contains(a.getKey())))return false;
        return names.equals(FIELDS);
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(host.equals(u.getHost())||fileHost.equals(u.getHost()))&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    private boolean selectFileFields(Map<String,String> f){return f.keySet().equals(FIELDS)&&selectName(f.get("user_file_nm"))&&selectName(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String s){String ext=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private String selectEncoded(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8).replace("+","%20");}
    private Map<String,String> selectQuery(String query){
        if(query==null||query.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();
        try{for(String pair:query.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+")||q.putIfAbsent(p[0],URLDecoder.decode(p[1],StandardCharsets.UTF_8))!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
