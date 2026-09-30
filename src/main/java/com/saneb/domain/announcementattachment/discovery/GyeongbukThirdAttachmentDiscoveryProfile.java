package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 첨부 영역만 읽고 정상 파일은 보존한다. 미확인 링크·형식은 성공으로 숨기지 않는다. */
final class GyeongbukThirdAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { YEONGJU, SEONGJU, YECHEON }
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;
    private final String code,host,sourceCode,parserCode,detailPath,downloadPath,idKey,fileKey,menu,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    GyeongbukThirdAttachmentDiscoveryProfile(Site site){
        this.site=site;code="LOCAL_"+site+"_BOARD_V1";
        host=switch(site){case YEONGJU->"www.yeongju.go.kr";case SEONGJU->"www.sj.go.kr";case YECHEON->"www.ycg.kr";};
        sourceCode=switch(site){case YEONGJU->"LGS-000206";case SEONGJU->"LGS-000217";case YECHEON->"LGS-000219";};
        parserCode=site==Site.YEONGJU?"SUBJECT_NOTICE_TABLE":"SPRING_BBS";
        detailPath=switch(site){case YEONGJU->"/open_content/main/page.do";case SEONGJU->"/page.do";case YECHEON->"/open.content/ko/administrative/news/announcement/";};
        downloadPath=switch(site){case YEONGJU->"/programs/board/saeol/notice/download.do";case SEONGJU->"/programs/board/board_download.do";case YECHEON->"/emwp/jsp/ofr/FileDownNew.jsp";};
        idKey=switch(site){case YEONGJU->"not_ancmt_mgt_no";case SEONGJU->"bod_uid";case YECHEON->"id";};
        fileKey=site==Site.YEONGJU?"file_seq":"file_uid";menu=site==Site.YEONGJU?"10619":"1044";
        hash=AttachmentProfileFingerprint.selectHash(String.join("|",code,"1",host,sourceCode,parserCode,detailPath,downloadPath,menu,"https443|official-area|same-request-or-yeongju-official-redirect|seongju-cmd2-to258|yecheon-post3|partial-preserved|unknown-role|limit10"),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return site==Site.YECHEON?Set.of(host,"eminwon.ycg.kr"):site==Site.YEONGJU?Set.of(host,"eminwon.yeongju.go.kr"):Set.of(host);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parserCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){
        try{
            if(s==null||!selectProviderCode().equals(s.providerCode())||!sourceCode.equals(s.localSourceCode())||!parserCode.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();
            URI u=URI.create(s.sourceUrl());if(!detailPath.equals(u.getPath())||!selectApprovedRequest(u))throw new IllegalArgumentException();
            // 성주 목록의 cmd=2는 공식 화면에서 cmd=258로만 이동한다. 임의 script는 실행하지 않는다.
            if(site==Site.SEONGJU&&"2".equals(selectQuery(u.getRawQuery()).get("cmd")))return URI.create("https://"+host+detailPath+"?mnu_uid="+menu+"&bod_uid="+selectQuery(u.getRawQuery()).get(idKey)+"&cmd=258");
            return u;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&u.getHost()!=null&&selectApprovedHosts().contains(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    @Override public boolean selectApprovedRequest(URI u){
        if(!selectSafeUri(u))return false;var q=selectQuery(u.getRawQuery());
        if(!host.equals(u.getHost()))return site==Site.YEONGJU&&"/emwp/jsp/ofr/FileDown.jsp".equals(u.getPath())&&q.keySet().equals(FIELDS)&&selectName(q.get("user_file_nm"))&&selectName(q.get("sys_file_nm"))&&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        if(detailPath.equals(u.getPath())){
            if(!selectId(q.get(idKey)))return false;
            if(site==Site.YECHEON)return Set.of("id","q","t","dn","p").containsAll(q.keySet());
            if(!menu.equals(q.get("mnu_uid"))||!(site==Site.SEONGJU?Set.of("2","258"):Set.of("2")).contains(q.getOrDefault("cmd","")))return false;
            return Set.of("mnu_uid",idKey,"cmd","board_code","boardType","srchSDate","srchKwd","srchColumn","srchDept","srchEDate","pageNo","srchBgpUid","srchEnable").containsAll(q.keySet())&&(!q.containsKey("boardType")||"notice".equals(q.get("boardType")));
        }
        if(site==Site.YECHEON||!downloadPath.equals(u.getPath())||!selectId(q.get(fileKey)))return false;
        return site==Site.YEONGJU?q.keySet().equals(Set.of(fileKey,idKey))&&selectId(q.get(idKey)):q.keySet().equals(Set.of(fileKey));
    }
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null||r.referer()!=null||r.utf8RedirectOctets())return false;
        if(r.publicSession()!=null){
            var plan=r.publicSession();
            if(site!=Site.SEONGJU||!"GET".equals(r.method())||!downloadPath.equals(r.uri().getPath())
                    ||!plan.cookieNames().equals(Set.of("JSESSIONID","LENA-UID","L-VISITOR"))
                    ||!selectApprovedRequest(plan.detailUri())||!detailPath.equals(plan.detailUri().getPath())
                    ||!"258".equals(selectQuery(plan.detailUri().getRawQuery()).get("cmd")))return false;
        }
        if("GET".equals(r.method()))return selectApprovedRequest(r.uri());
        var f=r.form();return site==Site.YECHEON&&"POST".equals(r.method())&&selectSafeUri(r.uri())&&"eminwon.ycg.kr".equals(r.uri().getHost())&&downloadPath.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&f.keySet().equals(FIELDS)&&selectName(f.get("user_file_nm"))&&selectOpaque(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){
        if(initial==null||next==null||!selectApprovedRequest(initial)||!selectApprovedRequest(next))return false;
        if(initial.equals(next))return true;
        if(site==Site.SEONGJU&&initial.publicSession()!=null)return next.publicSession()==null
                &&next.equals(Request.selectGet(initial.publicSession().detailUri()));
        return site==Site.YEONGJU&&host.equals(initial.uri().getHost())&&downloadPath.equals(initial.uri().getPath())&&"eminwon.yeongju.go.kr".equals(next.uri().getHost())&&"/emwp/jsp/ofr/FileDown.jsp".equals(next.uri().getPath());
    }
    @Override public Request selectDownloadRequest(Descriptor descriptor){
        if(site!=Site.SEONGJU)return descriptor.selectRequest();
        if(descriptor==null||!descriptor.downloadAllowed()||!code.equals(descriptor.locator().profileCode())
                ||!downloadPath.equals(descriptor.locator().path())||!downloadPath.equals(descriptor.fetchUri().getPath())
                ||!selectApprovedRequest(descriptor.selectRequest()))throw new IllegalArgumentException("PROFILE_REQUIRED");
        var ids=descriptor.locator().identifiers();
        if(!ids.keySet().equals(Set.of("noticeId","attachmentId"))||!selectId(ids.get("noticeId"))
                ||!Objects.equals(ids.get("attachmentId"),selectQuery(descriptor.fetchUri().getRawQuery()).get(fileKey)))
            throw new IllegalArgumentException("PROFILE_REQUIRED");
        URI detail=URI.create("https://"+host+detailPath+"?mnu_uid="+menu+"&bod_uid="+ids.get("noticeId")+"&cmd=258");
        var source=new Source(selectProviderCode(),normalizer.hash(normalizer.canonicalizeUrl(detail.toString())),detail.toString(),sourceCode,parserCode);
        var plan=new com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.PublicSessionPlan(
                detail,Set.of("JSESSIONID","LENA-UID","L-VISITOR"),html->selectDescriptors(source,html).descriptors().stream()
                    .anyMatch(found->found.downloadAllowed()&&found.fetchUri().equals(descriptor.fetchUri())
                            &&found.locator().equals(descriptor.locator())&&found.displayName().equals(descriptor.displayName())));
        return new Request(descriptor.fetchUri(),"GET",Map.of(),null,false,plan);
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());
        String titleSelector=switch(site){case YEONGJU->"div.news_view > div.data_top > h4";case SEONGJU->"form#frm div.bod_view > h4";case YECHEON->"div.km-view > div.km-view-title > span.content";};
        String areaSelector=switch(site){case YEONGJU->"div.news_view > div.data_add > span > span.span_r";case SEONGJU->"form#frm div.bod_view > dl.view_file > dd";case YECHEON->"div.km-view > div.km-view-file > ul.eminwon-files";};
        var titles=page.select(titleSelector);var areas=page.select(areaSelector);
        if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var area=areas.getFirst();var label=area.previousElementSibling();
        if(label==null||!(site==Site.SEONGJU?"첨부파일":"파일").equals(label.text().replaceAll("\\s+","")))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if(site==Site.YECHEON){
            var forms=page.select("div.km-view > form[name=form1]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();
            if(!"post".equalsIgnoreCase(form.attr("method"))||!("https://eminwon.ycg.kr"+downloadPath).equals(form.attr("action"))||form.childrenSize()!=3)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            var fields=new HashSet<String>();for(var input:form.children())if(!"input".equals(input.tagName())||!"hidden".equals(input.attr("type"))||!input.val().isEmpty()||!fields.add(input.attr("name"))||!input.attributes().asList().stream().allMatch(a->Set.of("type","name","id","value").contains(a.getKey())))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            if(!fields.equals(FIELDS))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
        String notice=selectQuery(detail.getRawQuery()).get(idKey);var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean exceeded=false;
        for(var a:area.select("a")){
            try{
                if(a.hasAttr("onclick"))continue;String name=a.text().strip();if(site==Site.SEONGJU)name=name.replaceFirst("\\s+\\[[0-9]+(?:\\.[0-9]+)? (?:B|KB|MB)\\]$","");
                if(!selectName(name))continue;Request request;String file;
                if(site==Site.YECHEON){var args=AttachmentDownloadInvocation.selectArguments(a.attr("href"),"goDownLoad",false,false);if(args.size()!=3||!name.equals(args.getFirst()))continue;
                    request=new Request(URI.create("https://eminwon.ycg.kr"+downloadPath),"POST",Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));file=normalizer.hash(args.get(2)+"\n"+args.get(1));
                }else{URI u=detail.resolve(URI.create(a.attr("href")));if(!downloadPath.equals(u.getPath())||site==Site.YEONGJU&&!notice.equals(selectQuery(u.getRawQuery()).get(idKey)))continue;request=Request.selectGet(u);file=selectQuery(u.getRawQuery()).get(fileKey);}
                if(!selectApprovedRequest(request))continue;
                if(files.containsKey(file)){if(files.get(file).displayName().equals(name)&&files.get(file).selectRequest().equals(request))recognized.add(a);continue;}
                if(files.size()==10){exceeded=true;continue;}String ext=selectFormat(name);
                files.put(file,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,downloadPath,Map.of("noticeId",notice,"attachmentId",file)),name,ext,"UNKNOWN",ext!=null,request.form()));recognized.add(a);
            }catch(IllegalArgumentException ignored){/* 실패한 링크만 분리하며 정상 첨부 처리를 계속한다. */}
        }
        if(site!=Site.YECHEON)for(var a:area.select("a"))if(!recognized.contains(a)){
            var previous=a.previousElementSibling();if(previous==null||!recognized.contains(previous))continue;
            try{String file=selectQuery(detail.resolve(previous.attr("href")).getRawQuery()).get(fileKey);boolean preview;
                if(site==Site.SEONGJU)preview="#self".equals(a.attr("href"))&&("openViewFiles("+file+")").equals(a.attr("onclick"))&&"[미리보기]".equals(a.text());
                else {URI u=detail.resolve(a.attr("href"));preview=!a.hasAttr("onclick")&&"[미리보기]".equals(a.text())&&selectSafeUri(u)&&host.equals(u.getHost())&&"/programs/board/saeol/notice/fileView.do".equals(u.getPath())&&selectQuery(u.getRawQuery()).equals(Map.of(fileKey,file,idKey,notice));}
                if(preview)recognized.add(a);
            }catch(IllegalArgumentException ignored){/* 미확인 뷰어는 호출하지 않는다. */}
        }
        boolean unresolved=area.select("a").stream().anyMatch(a->!recognized.contains(a));var residual=area.clone();residual.select("a").remove();
        if(!residual.text().isBlank()||!residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!residual.select("li,p.file").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectId(String s){return s!=null&&s.matches("[0-9]{1,15}");}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectOpaque(String s){return s!=null&&s.length()<=2048&&java.util.regex.Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$").matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private String selectFormat(String s){String ext=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Map<String,String> selectQuery(String query){
        if(query==null||query.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();
        try{for(String pair:query.split("&",-1)){if(pair.isEmpty())continue;String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>500||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
