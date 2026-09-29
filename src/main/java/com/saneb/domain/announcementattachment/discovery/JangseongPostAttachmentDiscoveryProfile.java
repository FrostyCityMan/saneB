package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

/** 장성 공식 상세의 첨부 표와 공개 POST 폼만 처리한다. 인쇄·뷰어 링크는 대상이 아니다. */
final class JangseongPostAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_JANGSEONG_POST_V1",HOST="www.jangseong.go.kr",FILE_HOST="eminwon.jangseong.go.kr";
    private static final String DETAIL="/home/www/news/jangseong/announcement/show/",DOWNLOAD="/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern CIPHER=Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$");
    private static final String HASH=AttachmentProfileFingerprint.selectHash(CODE+":1|LGS-000196|SPRING_BBS|"+HOST+"|"+FILE_HOST+"|"+DETAIL+"|"+DOWNLOAD+"|show-info-file-table|nnn-hidden3|opaque-post|same-request-only|unknown-role|limit10",JangseongPostAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return HASH;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(HOST,FILE_HOST);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000196","SPRING_BBS"));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000196".equals(source.localSourceCode())||!"SPRING_BBS".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI detail=URI.create(source.sourceUrl());if(!selectApprovedRequest(detail))throw new IllegalArgumentException();return detail;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        if(!selectSafeUri(uri)||!HOST.equals(uri.getHost())||!uri.getPath().matches(DETAIL+"[0-9]{1,15}"))return false;
        if(uri.getRawQuery()==null)return true;var q=selectQuery(uri.getRawQuery());
        return !q.isEmpty()&&Set.of("page","search","not_ancmt_sj").containsAll(q.keySet())&&(!q.containsKey("page")||q.get("page").matches("[1-9][0-9]{0,5}"))&&(!q.containsKey("search")||"search_title".equals(q.get("search")));
    }
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null)return false;if("GET".equals(r.method()))return selectApprovedRequest(r.uri());
        var f=r.form();return "POST".equals(r.method())&&selectSafeUri(r.uri())&&FILE_HOST.equals(r.uri().getHost())&&DOWNLOAD.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&f.keySet().equals(FIELDS)&&selectOpaque(f.get("user_file_nm"))&&selectOpaque(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(initial);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());var titles=page.select("div.show_info > h3");var areas=page.select("div.show_info > div.file_down");var forms=page.select("form[name=nnn]");
        if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
        if(!"post".equalsIgnoreCase(form.attr("method"))||!("https://"+FILE_HOST+DOWNLOAD).equals(form.attr("action"))||form.childrenSize()!=3)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var fields=new HashSet<String>();for(var input:form.children())if(!input.tagName().equals("input")||!"hidden".equals(input.attr("type"))||!input.val().isEmpty()||!fields.add(input.attr("name"))||!input.attributes().asList().stream().allMatch(a->Set.of("type","name","value").contains(a.getKey())))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        if(!fields.equals(FIELDS))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var area=areas.getFirst();var labels=area.select("td").stream().filter(e->"첨부파일".equals(e.ownText().replaceAll("[\\s\\u00a0:：]+",""))).toList();
        if(labels.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");var label=labels.getFirst();var container=label.nextElementSibling();
        if(container==null||!"td".equals(container.tagName())||!"tr".equals(label.parent().tagName())||container.nextElementSibling()!=null)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var residual=area.clone();residual.select("a").remove();residual.select("td").stream().filter(e->"첨부파일".equals(e.ownText().replaceAll("[\\s\\u00a0:：]+",""))).forEach(org.jsoup.nodes.Element::remove);
        boolean unresolved=!residual.text().isBlank()||!area.select("button,input,select,form,iframe,object,embed,script,[onclick]").isEmpty()||!residual.select("[href],img").isEmpty();
        var files=new LinkedHashMap<String,Descriptor>();boolean exceeded=false;
        for(var anchor:area.select("a")){
            if(anchor.closest("td")!=container){unresolved=true;continue;}
            var args=AttachmentDownloadInvocation.selectArguments(anchor.attr("href"),"goDownLoad",false,false);String name=anchor.text().strip();
            if(args.size()!=3||anchor.hasAttr("onclick")||!selectName(name)){unresolved=true;continue;}
            Request request;
            try{request=new Request(URI.create("https://"+FILE_HOST+DOWNLOAD),"POST",Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));}
            catch(IllegalArgumentException e){unresolved=true;continue;}
            if(!selectApprovedRequest(request)){unresolved=true;continue;}String id=normalizer.hash(args.get(2)+"\n"+args.get(1));
            if(files.containsKey(id)){if(!files.get(id).displayName().equals(name)||!files.get(id).selectRequest().equals(request))unresolved=true;continue;}
            if(files.size()==10){exceeded=true;continue;}String ext=selectFormat(name);
            var locator=new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",detail.getPath().substring(DETAIL.length()),"attachmentId",id));
            files.put(id,new Descriptor(request.uri(),locator,name,ext,"UNKNOWN",ext!=null,request.form()));
        }
        if(files.isEmpty()&&!container.select("li").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(HOST.equals(u.getHost())||FILE_HOST.equals(u.getHost()))&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    private boolean selectOpaque(String s){return s!=null&&s.length()<=2048&&CIPHER.matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private boolean selectName(String s){return !s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String s){String ext=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Map<String,String> selectQuery(String query){
        if(query.length()>4096)return Map.of();var q=new LinkedHashMap<String,String>();try{for(String pair:query.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>500||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
