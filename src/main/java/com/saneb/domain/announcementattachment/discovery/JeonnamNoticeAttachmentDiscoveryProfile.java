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

/** 대표 누리집의 공식 첨부만 처리한다. 인쇄용 PDF·미리보기·메뉴 링크는 다운로드하지 않는다. */
final class JeonnamNoticeAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { MOKPO, YEOSU, NAJU, GANGJIN, MUAN }
    private static final String GET_PATH="/emwp/jsp/ofr/FileDown.jsp",POST_PATH="/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> BASE_FORM=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern OPAQUE_SUFFIX=Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$");
    private final Site site;
    private final String code,sourceCode,detailHost,fileHost,detailPath,hash;
    private final boolean post,csrf;
    private final SaeolGetAttachmentDiscoveryProfile getValidator;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();

    JeonnamNoticeAttachmentDiscoveryProfile(Site site){
        this.site=site;String city=site.name().toLowerCase(Locale.ROOT);code="LOCAL_"+site+"_NOTICE_V1";
        detailHost="www."+city+".go.kr";fileHost="eminwon."+city+".go.kr";
        sourceCode=switch(site){case MOKPO->"LGS-000178";case YEOSU->"LGS-000179";case NAJU->"LGS-000181";case GANGJIN->"LGS-000190";case MUAN->"LGS-000193";};
        detailPath=switch(site){case MOKPO->"/www/mokpo_news/notification/public_notice";case YEOSU->"/www/govt/news/notify/new_notifys/new_notify";case NAJU->"/www/administration/notice/gosi_new";case GANGJIN->"/www/government/notice/gosi";case MUAN->"/www/openmuan/new/announcement";};
        post=site==Site.MOKPO||site==Site.YEOSU||site==Site.MUAN;csrf=site==Site.MOKPO||site==Site.MUAN;
        getValidator=new SaeolGetAttachmentDiscoveryProfile(code,sourceCode,fileHost,"SPRING_BBS","td",false);
        hash=AttachmentProfileFingerprint.selectHash(String.join("|","JEONNAM_NOTICE:1",code,sourceCode,detailHost,fileHost,detailPath,
                "SPRING_BBS|https443|official-file-area|same-request-no-redirect|ephemeral-form|unknown-role|limit10",getValidator.selectProfileHash()),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(detailHost,fileHost);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,"SPRING_BBS"));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!sourceCode.equals(source.localSourceCode())||!"SPRING_BBS".equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());if(!detailHost.equals(uri.getHost())||!detailPath.equals(uri.getPath())||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        if(!selectSafeUri(uri))return false;
        if(fileHost.equals(uri.getHost()))return !post&&GET_PATH.equals(uri.getPath())&&getValidator.selectApprovedRequest(uri);
        if(!detailHost.equals(uri.getHost())||!detailPath.equals(uri.getPath()))return false;
        var q=selectQuery(uri.getRawQuery());return q.getOrDefault("idx","").matches("[0-9]{1,15}")&&"view".equals(q.get("mode"))
                &&Set.of("idx","mode","page","search_type","search_word","page_scale","start_date","finish_date").containsAll(q.keySet());
    }
    @Override public boolean selectApprovedRequest(Request request){
        if(request==null)return false;if("GET".equals(request.method()))return selectApprovedRequest(request.uri());
        if(!post||!"POST".equals(request.method())||!selectSafeUri(request.uri())||!fileHost.equals(request.uri().getHost())||!POST_PATH.equals(request.uri().getPath())||request.uri().getRawQuery()!=null)return false;
        var fields=new HashSet<>(BASE_FORM);if(csrf)fields.add("csrf_token");var f=request.form();
        return f.keySet().equals(fields)&&(csrf?f.get("csrf_token").matches("[a-f0-9]{64}"):true)
                &&(site==Site.MUAN?selectOpaque(f.get("user_file_nm")):selectName(f.get("user_file_nm")))
                &&selectOpaque(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){
        return initial!=null&&initial.equals(next)&&selectApprovedRequest(initial);
    }

    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());
        String titleSelector=site==Site.NAJU?"div.view_title > p.title":site==Site.MUAN?"#board_basic_view > div.news_tit > h3":"div.view_titlebox > h3";
        var titles=page.select(titleSelector);if(titles.size()!=1||titles.getFirst().text().isBlank())return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        String areaSelector=switch(site){case MOKPO,YEOSU->"div.file_viewbox";case MUAN->"#board_basic_view > div.file_attach";case NAJU->"div.file_area";case GANGJIN->"div.file_body";};
        var areas=page.select(areaSelector);if(areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");Element area=areas.getFirst();
        if(site==Site.MOKPO||site==Site.YEOSU){if(area.select("div.left_box > strong").size()!=1||!"첨부파일".equals(area.select("div.left_box > strong").text()))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        else if(site==Site.NAJU){if(!"첨부파일".equals(area.select("div.file_tit > span.tit").text()))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        else if(site==Site.GANGJIN){var head=area.previousElementSibling();if(head==null||!head.hasClass("file_head")||!"첨부파일".equals(head.text()))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        else if(area.select("h5").size()!=1||!area.select("h5").text().matches("첨부파일\\s*\\([0-9]{1,2}\\)"))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        int declaredCount=site==Site.MUAN?Integer.parseInt(area.select("h5").text().replaceAll("[^0-9]","")):-1;
        Map<String,String> blankForm=Map.of();
        if(post){
            var forms=page.select("form[name=nnn]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
            if(!"post".equalsIgnoreCase(form.attr("method"))||!("https://"+fileHost+POST_PATH).equals(form.attr("action")))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            var f=new LinkedHashMap<String,String>();
            for(var input:form.children()){
                if(!input.tagName().equals("input")||!"hidden".equals(input.attr("type"))||!input.attributes().asList().stream().allMatch(a->Set.of("type","name","value").contains(a.getKey()))||f.putIfAbsent(input.attr("name"),input.val())!=null)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            }
            var expected=new HashSet<>(BASE_FORM);if(csrf)expected.add("csrf_token");
            if(!f.keySet().equals(expected)||BASE_FORM.stream().anyMatch(k->!f.get(k).isEmpty())||csrf&&!f.get("csrf_token").matches("[a-f0-9]{64}"))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");blankForm=f;
        }
        var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();var knownItems=new HashSet<Element>();boolean unresolved=false,exceeded=false;
        String noticeId=selectQuery(detail.getRawQuery()).get("idx");
        for(var link:area.select(site==Site.NAJU?"button":"a")){
            try{
                Request request;String name,id;
                if(post){
                    if(!"#none".equals(link.attr("href")))continue;
                    var args=AttachmentDownloadInvocation.selectArguments(link.attr("onclick"),"goDownLoad",true,false);if(args.size()!=3)continue;
                    var values=new LinkedHashMap<>(blankForm);values.put("user_file_nm",args.get(0));values.put("sys_file_nm",args.get(1));values.put("file_path",args.get(2));
                    request=new Request(URI.create("https://"+fileHost+POST_PATH),"POST",values);
                    var label=link.clone();label.select("span.file_icon").remove();name=label.text().strip();
                    if(site!=Site.MUAN&&!name.equals(args.get(0)))continue;id=normalizer.hash(args.get(2)+"\n"+args.get(1));
                }else{
                    if(site!=Site.NAJU&&link.hasAttr("onclick"))continue;
                    String href=site==Site.NAJU?selectWindowUri(link.attr("onclick")):link.attr("href");if(href==null)continue;
                    URI uri=detail.resolve(URI.create(href.replace(" ","%20")));if(!GET_PATH.equals(uri.getPath())||!selectApprovedRequest(uri))continue;
                    var q=selectQuery(uri.getRawQuery());name=q.get("user_file_nm");id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));
                    String query=q.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")).collect(java.util.stream.Collectors.joining("&"));
                    request=Request.selectGet(URI.create("https://"+fileHost+GET_PATH+"?"+query));
                }
                if(!selectName(name)||!selectApprovedRequest(request))continue;
                if(files.containsKey(id)){if(files.get(id).displayName().equals(name)&&files.get(id).selectRequest().equals(request))recognized.add(link);continue;}
                if(files.size()==10){exceeded=true;continue;}
                String format=selectFormat(name);if(!post&&!Objects.equals(format,selectFormat(selectQuery(request.uri().getRawQuery()).get("sys_file_nm"))))format=null;
                var locator=new AttachmentSetEvidence.Locator(code,post?POST_PATH:GET_PATH,Map.of("noticeId",noticeId,"attachmentId",id));
                // 공식 폼의 일회성 필드는 Request에만 둔다. locator·로그·감사 metadata에 복사하지 않는다.
                files.put(id,new Descriptor(request.uri(),locator,name,format,"UNKNOWN",format!=null,request.form()));recognized.add(link);
                if(link.closest("li")!=null)knownItems.add(link.closest("li"));
            }catch(IllegalArgumentException ignored){/* 인식하지 못한 링크는 아래에서 오류로 보존한다. */}
        }
        if(!post)for(var link:area.select("a,button"))if(!recognized.contains(link)&&knownItems.contains(link.closest("li"))){
            String href=site==Site.NAJU?selectWindowUri(link.attr("onclick")):link.hasAttr("onclick")?null:link.attr("href");
            if(href!=null&&href.matches("/Viewer_gosi/"+noticeId+"_[1-9][0-9]?"))recognized.add(link);
        }
        for(var link:area.select("a,button"))if(!recognized.contains(link))unresolved=true;
        if(declaredCount>=0&&declaredCount!=files.size()||files.isEmpty()&&!area.select("li").isEmpty())unresolved=true;
        recognized.forEach(Element::remove);
        for(var item:knownItems){
            if(site==Site.NAJU)item.select(":root > span.file").remove();
            if(site==Site.GANGJIN)item.select(":root > span.txt").remove();
            if(site==Site.MUAN)item.select("span.file_icon_box > img[src^=/images/common/ext_img/][alt=첨부파일]").remove();
        }
        switch(site){case MOKPO,YEOSU->area.select("div.left_box > strong").remove();case NAJU->area.select("div.file_tit > span.tit").remove();case MUAN->area.select("h5").remove();default->{}}
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private String selectWindowUri(String call){var m=Pattern.compile("window\\.open\\('([^'\\r\\n]{1,8192})',\\s*'_blank'\\)").matcher(call);return m.matches()?m.group(1):null;}
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(detailHost.equals(u.getHost())||fileHost.equals(u.getHost()))&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    private boolean selectOpaque(String s){return s!=null&&s.length()>=22&&s.length()<=2048&&OPAQUE_SUFFIX.matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String s){if(s==null)return null;String e=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("HWP","HWPX","PDF").contains(e)?e:null;}
    private Map<String,String> selectQuery(String query){
        if(query==null||query.length()>8192)return Map.of();var values=new LinkedHashMap<String,String>();
        try{for(String pair:query.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);
            if(v.length()>2048||v.codePoints().anyMatch(Character::isISOControl)||values.putIfAbsent(p[0],v)!=null)return Map.of();}return values;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
