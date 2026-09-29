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

/** 곡성·진도의 실측 공식 첨부 영역. 미해석 파일이 있어도 정상 파일은 보존한다. */
final class JeonnamCountyAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { GOKSEONG, JINDO }
    private static final String POST_PATH="/emwp/jsp/ofr/FileDownNew.jsp",GET_PATH="/doc/dataDown.jsp";
    private static final Set<String> FORM_FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;
    private final String code,sourceCode,listCode,host,fileHost,detailPath,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    JeonnamCountyAttachmentDiscoveryProfile(Site site){
        this.site=site;String city=site.name().toLowerCase(Locale.ROOT);
        code="LOCAL_"+site+"_NOTICE_V1";host="www."+city+".go.kr";
        fileHost=site==Site.GOKSEONG?"eminwon.gokseong.go.kr":host;
        sourceCode=site==Site.GOKSEONG?"LGS-000184":"LGS-000198";
        listCode=site==Site.GOKSEONG?"SAFE_SAEOL_EMINWON":"SPRING_BBS";
        detailPath=site==Site.GOKSEONG?"/board/GosiView.do":"/home/gosi/general.cs";
        hash=AttachmentProfileFingerprint.selectHash(String.join("|","JEONNAM_COUNTY:1",code,sourceCode,listCode,host,fileHost,detailPath,
                "fixed-official-source-alias|https443|official-file-area|ephemeral-post|same-request-only|unknown-role|limit10"),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return new HashSet<>(List.of(host,fileHost));}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,listCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!sourceCode.equals(source.localSourceCode())||!listCode.equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());
            // 기존 목록 파서가 만든 같은 기관의 고정 새올 경로만 공식 게시판으로 연결한다. 원문 identity는 유지한다.
            if(site==Site.GOKSEONG&&selectSafeUri(uri)&&host.equals(uri.getHost())&&SaeolGetAttachmentDiscoveryProfile.DETAIL.equals(uri.getPath())){
                var q=selectQuery(uri.getRawQuery());
                if(q.size()!=7||!q.entrySet().containsAll(Map.of("context","NTIS","homepage_pbs_yn","Y","jndinm","OfrNotAncmtEJB","method","selectOfrNotAncmt","methodnm","selectOfrNotAncmtRegst","subCheck","Y").entrySet())||!q.getOrDefault("not_ancmt_mgt_no","").matches("[0-9]{1,15}"))throw new IllegalArgumentException();
                uri=URI.create("https://"+host+detailPath+"?menuNo=102001003000&not_ancmt_mgt_no="+q.get("not_ancmt_mgt_no"));
            }
            if(!host.equals(uri.getHost())||!detailPath.equals(uri.getPath())||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        if(!selectSafeUri(uri)||!host.equals(uri.getHost()))return false;var q=selectQuery(uri.getRawQuery());
        if(detailPath.equals(uri.getPath())){
            if(site==Site.GOKSEONG)return "102001003000".equals(q.get("menuNo"))&&q.getOrDefault("not_ancmt_mgt_no","").matches("[0-9]{1,15}")
                    &&Set.of("menuNo","not_ancmt_mgt_no","not_ancmt_se_code","list_gubun","pageIndex","searchCnd","srhCate","searchWrd").containsAll(q.keySet());
            return "view".equals(q.get("act"))&&q.getOrDefault("notAncmtMgtNo","").matches("[0-9]{1,15}")
                    &&(!q.containsKey("m")||"878".equals(q.get("m")))&&Set.of("act","notAncmtMgtNo","m","searchCondition","searchKeyword","pageIndex").containsAll(q.keySet());
        }
        return site==Site.JINDO&&GET_PATH.equals(uri.getPath())&&selectFileQuery(q);
    }
    @Override public boolean selectApprovedRequest(Request request){
        if(request==null)return false;if("GET".equals(request.method()))return selectApprovedRequest(request.uri());
        if(site!=Site.GOKSEONG||!"POST".equals(request.method())||!selectSafeUri(request.uri())||!fileHost.equals(request.uri().getHost())||!POST_PATH.equals(request.uri().getPath())||request.uri().getRawQuery()!=null)return false;
        var f=request.form();return f.keySet().equals(FORM_FIELDS)&&selectOpaque(f.get("user_file_nm"))&&selectOpaque(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(initial);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());
        var titles=page.select(site==Site.GOKSEONG?"div.board_view > h3":"div.board_view > dl.view_head > dt > span.txt");
        var areas=page.select(site==Site.GOKSEONG?"div.board_view > ul.file_down":"div.board_view > div.view_foot > ul.board_file");
        if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if(site==Site.GOKSEONG){
            var forms=page.select("form[name=nnn]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var f=forms.getFirst();
            if(!"post".equalsIgnoreCase(f.attr("method"))||!("https://"+fileHost+POST_PATH).equals(f.attr("action"))||f.childrenSize()!=3)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            var fields=new HashSet<String>();for(var input:f.children())if(!input.tagName().equals("input")||!"hidden".equals(input.attr("type"))||!input.val().isEmpty()||!fields.add(input.attr("name"))||!input.attributes().asList().stream().allMatch(a->Set.of("type","name","value").contains(a.getKey())))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            if(!fields.equals(FORM_FIELDS))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
        var area=areas.getFirst();var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean exceeded=false;
        String noticeId=selectQuery(detail.getRawQuery()).get(site==Site.GOKSEONG?"not_ancmt_mgt_no":"notAncmtMgtNo");
        for(var item:area.children()){
            if(!item.tagName().equals("li"))continue;
            for(var link:item.select("a")){
                try{
                    if(link.hasAttr("onclick"))continue;Request request;String name,id,format;
                    if(site==Site.GOKSEONG){
                        var args=AttachmentDownloadInvocation.selectArguments(link.attr("href"),"goDownLoad",false,false);if(args.size()!=3)continue;
                        // 같은 li의 이름 링크와 다운로드 버튼을 한 파일로 처리한다.
                        var labels=item.select("a").stream().filter(a->a.attr("href").equals(link.attr("href"))&&!a.hasAttr("onclick")&&!"다운로드".equals(a.text().strip())&&selectName(a.text().strip())).toList();
                        name=labels.size()==1?labels.getFirst().text().strip():link.text().strip();
                        request=new Request(URI.create("https://"+fileHost+POST_PATH),"POST",Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)));
                        id=normalizer.hash(args.get(2)+"\n"+args.get(1));format=selectFormat(name);
                    }else{
                        URI uri=detail.resolve(link.attr("href"));if(!GET_PATH.equals(uri.getPath())||!selectApprovedRequest(uri))continue;
                        var q=selectQuery(uri.getRawQuery());if(!noticeId.equals(q.get("fileMgtNo")))continue;
                        var names=item.select("a > em");if(names.size()!=1)continue;name=names.getFirst().text().strip();format=selectFormat(name);
                        if(format!=null&&!format.equalsIgnoreCase(q.get("stype")))continue;
                        request=Request.selectGet(uri);id=normalizer.hash(noticeId+"\n"+q.get("fileNo"));
                    }
                    if(!selectName(name)||!selectApprovedRequest(request))continue;
                    if(files.containsKey(id)){if(files.get(id).displayName().equals(name)&&files.get(id).selectRequest().equals(request))recognized.add(link);continue;}
                    if(files.size()==10){exceeded=true;continue;}
                    var locator=new AttachmentSetEvidence.Locator(code,site==Site.GOKSEONG?POST_PATH:GET_PATH,Map.of("noticeId",noticeId,"attachmentId",id));
                    files.put(id,new Descriptor(request.uri(),locator,name,format,"UNKNOWN",format!=null,request.form()));recognized.add(link);
                    if(site==Site.JINDO)for(var preview:item.select("a")){
                        if(preview.hasAttr("onclick"))continue;URI u=detail.resolve(preview.attr("href"));
                        if(selectSafeUri(u)&&host.equals(u.getHost())&&"/doc/indexDown.jsp".equals(u.getPath())&&selectQuery(u.getRawQuery()).equals(selectQuery(request.uri().getRawQuery())))recognized.add(preview);
                    }
                }catch(IllegalArgumentException ignored){/* 아래에서 미해석 링크를 오류로 남기고 다른 파일을 계속 처리한다. */}
            }
        }
        boolean unresolved=files.isEmpty()&&!area.select("li").isEmpty();recognized.forEach(Element::remove);
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(host.equals(u.getHost())||fileHost.equals(u.getHost()))&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    private boolean selectFileQuery(Map<String,String> q){return q.keySet().equals(Set.of("fileNo","fileMgtNo","stype"))&&q.get("fileNo").matches("[1-9][0-9]?")&&q.get("fileMgtNo").matches("[0-9]{1,15}")&&q.get("stype").matches("[a-zA-Z0-9]{1,10}");}
    private boolean selectOpaque(String s){return s!=null&&s.length()<=2048&&java.util.regex.Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$").matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String s){if(s==null)return null;String ext=s.substring(s.lastIndexOf('.')+1).strip().toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Map<String,String> selectQuery(String query){
        if(query==null||query.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();
        try{for(String pair:query.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>2048||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
