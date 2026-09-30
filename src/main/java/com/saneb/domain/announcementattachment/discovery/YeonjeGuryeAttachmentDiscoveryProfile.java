package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungjuEminwonNoticePage;
import com.saneb.domain.announcementsource.provider.content.YeonjeGuryeNoticePage;
import com.saneb.domain.announcementsource.provider.content.YeonjeGuryeNoticePage.Site;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

/** 연제 고정 GET과 구례 공개 POST만 지원한다. 검색 세션·CSRF 값은 파일 요청에 전달하지 않는다. */
final class YeonjeGuryeAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern CIPHER=Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$");
    private static final String PAGE_HASH=AttachmentProfileFingerprint.selectHash("PAGE:1",YeonjeGuryeNoticePage.class);
    private static final String QUERY_HASH=AttachmentProfileFingerprint.selectHash("QUERY:1",ChungjuEminwonNoticePage.class);
    private static final Map<Site,String> HASHES=Map.of(Site.YEONJE,selectHash(Site.YEONJE),Site.GURYE,selectHash(Site.GURYE));
    private final Site site;private final String code,sourceCode,parser,host,fileHost,filePath,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    YeonjeGuryeAttachmentDiscoveryProfile(Site site){
        this.site=Objects.requireNonNull(site);boolean y=site==Site.YEONJE;
        code=y?"LOCAL_YEONJE_GET_V1":"LOCAL_GURYE_POST_V1";sourceCode=y?"LGS-000040":"LGS-000185";parser=y?"SAEOL_GOSI":"SAFE_SAEOL_EMINWON";
        host=y?"www.yeonje.go.kr":"www.gurye.go.kr";fileHost=y?"eminwon.yeonje.go.kr":"eminwon.gurye.go.kr";filePath=y?"/emwp/jsp/ofr/FileDown.jsp":"/emwp/jsp/ofr/FileDownNew.jsp";
        hash=HASHES.get(site);
    }
    private static String selectHash(Site site){return AttachmentProfileFingerprint.selectHash("YEONJE_GURYE:1|"+site+"|fixed-official-area|same-request|unknown-role|limit10|strict-headers|"+PAGE_HASH+QUERY_HASH,YeonjeGuryeAttachmentDiscoveryProfile.class);}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parser));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(host,fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!sourceCode.equals(source.localSourceCode())||!parser.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                    ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());if(!YeonjeGuryeNoticePage.selectMatches(uri,site)||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    @Override public boolean selectApprovedRequest(URI uri){
        try{
            if(!selectSafeUri(uri)||uri.getRawQuery()==null||uri.getRawQuery().length()>8192)return false;var q=ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery());
            if(YeonjeGuryeNoticePage.selectMatches(uri,site)){
                if(site==Site.YEONJE)return q.keySet().equals(Set.of("notAncmtMgtNo","mId"))&&"0206030000".equals(q.get("mId"))&&q.get("notAncmtMgtNo").matches("[1-9][0-9]{0,14}");
                return q.keySet().equals(Set.of("pageIndex","menuNo","not_ancmt_se_code","not_ancmt_mgt_no"))&&q.get("pageIndex").matches("[1-9][0-9]{0,5}")&&"115004002001".equals(q.get("menuNo"))
                        &&"01,04,06,07".equals(q.get("not_ancmt_se_code"))&&q.get("not_ancmt_mgt_no").matches("[1-9][0-9]{0,14}");
            }
            return site==Site.YEONJE&&fileHost.equals(uri.getHost())&&filePath.equals(uri.getPath())&&q.keySet().equals(FIELDS)&&selectName(q.get("user_file_nm"))&&selectName(q.get("sys_file_nm"))&&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null)return false;if("GET".equals(r.method()))return selectApprovedRequest(r.uri());var u=r.uri();var f=r.form();
        return site==Site.GURYE&&"POST".equals(r.method())&&selectSafeUri(u)&&fileHost.equals(u.getHost())&&filePath.equals(u.getPath())&&u.getRawQuery()==null&&f.keySet().equals(FIELDS)
                &&selectOpaque(f.get("user_file_nm"))&&selectOpaque(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());org.jsoup.nodes.Element area;
        try{area=YeonjeGuryeNoticePage.selectFiles(YeonjeGuryeNoticePage.selectRoot(page,site),site);}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        if(site==Site.GURYE){
            var forms=page.select("form#command[name=nnn][method=post]");
            if(forms.size()!=1||!("https://"+fileHost+filePath).equals(forms.getFirst().attr("action")))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            var fields=new HashSet<String>();for(var input:forms.getFirst().select("input")){
                if(!"hidden".equals(input.attr("type")))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");String name=input.attr("name");
                if(!fields.add(name)||!Set.of("user_file_nm","sys_file_nm","file_path","CSRFToken").contains(name)||!"CSRFToken".equals(name)&&!input.val().isEmpty())return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            }
            if(!fields.containsAll(FIELDS)||!forms.getFirst().select("button,select,textarea,script,iframe").isEmpty())return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
        boolean unresolved=!area.select("script,iframe,object,embed,button,input,form").isEmpty();var files=new LinkedHashMap<String,Descriptor>();boolean limited=false;
        var allLinks=area.select("a");var handled=Collections.newSetFromMap(new IdentityHashMap<org.jsoup.nodes.Element,Boolean>());
        for(var anchor:allLinks){
            if(site==Site.YEONJE&&anchor.hasClass("bt_white_s"))continue;
            try{
                if(site==Site.YEONJE&&(!"dd".equals(anchor.parent().tagName())||anchor.parent().parent()!=area)||site==Site.GURYE&&(!"li".equals(anchor.parent().tagName())||anchor.parent().parent()!=area))throw new IllegalArgumentException();
                var args=site==Site.YEONJE?selectInvocation(anchor.attr("onclick"),"goDownload",3):AttachmentDownloadInvocation.selectArguments(anchor.attr("href"),"goDownLoad",false,false);
                String name=anchor.text().strip();if(args.size()!=3||!selectName(name))throw new IllegalArgumentException();Request request;
                var fields=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));
                if(site==Site.YEONJE){if(!name.equals(args.get(0)))throw new IllegalArgumentException();var q=new StringJoiner("&");fields.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->q.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));request=new Request(URI.create("https://"+fileHost+filePath+"?"+q),"GET",Map.of());}
                else request=new Request(URI.create("https://"+fileHost+filePath),"POST",fields);
                if(!selectApprovedRequest(request))throw new IllegalArgumentException();String id=normalizer.hash(args.get(2)+"\n"+args.get(1));handled.add(anchor);
                if(site==Site.YEONJE){var previews=anchor.parent().select("a.bt_white_s");for(var preview:previews){var pv=selectInvocation(preview.attr("onclick"),"fn_egov_gosi_preview",5);String notice=ChungjuEminwonNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo");
                    if(pv.size()==5&&notice.equals(pv.get(0))&&pv.get(1).matches(notice+"-[0-9]+\\.[A-Za-z0-9]+")&&pv.subList(2,5).equals(args))handled.add(preview);}}
                if(files.containsKey(id)){if(!files.get(id).displayName().equals(name)||!files.get(id).selectRequest().equals(request))unresolved=true;continue;}
                if(files.size()==10){limited=true;continue;}String format=selectFormat(name);boolean supported=format!=null&&(site==Site.GURYE||format.equals(selectFormat(args.get(1))));
                String notice=ChungjuEminwonNoticePage.selectParameters(detail.getRawQuery()).get(site==Site.YEONJE?"notAncmtMgtNo":"not_ancmt_mgt_no");
                files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,filePath,Map.of("noticeId",notice,"attachmentId",id)),name,supported?format:null,"UNKNOWN",supported,request.form()));
            }catch(IllegalArgumentException e){unresolved=true;}
        }
        if(handled.size()!=allLinks.size())unresolved=true;
        var residual=area.clone();residual.select("a,img,dt").remove();if(!residual.text().isBlank()||!residual.select("[href],[onclick]").isEmpty())unresolved=true;
        if(limited)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    /** 연제의 서로 다른 3/5 인자 호출을 선형 파싱한다. 스크립트는 실행하지 않는다. */
    private static List<String> selectInvocation(String input,String function,int count){
        if(input==null||input.length()>8192)return List.of();int at=selectWhitespace(input,0);
        if(input.startsWith("javascript:",at))at=selectWhitespace(input,at+11);
        if(!input.startsWith(function,at))return List.of();at=selectWhitespace(input,at+function.length());
        if(at>=input.length()||input.charAt(at++)!='(')return List.of();var args=new ArrayList<String>();
        for(int i=0;i<count;i++){
            at=selectWhitespace(input,at);if(at>=input.length()||input.charAt(at++)!='\'')return List.of();var value=new StringBuilder();boolean closed=false;
            while(at<input.length()){
                char c=input.charAt(at++);if(c=='\''){closed=true;break;}
                if(c=='\\'){if(at>=input.length())return List.of();c=input.charAt(at++);if(c!='\\'&&c!='\'')return List.of();}
                if(Character.isISOControl(c)||value.length()>=2048)return List.of();value.append(c);
            }
            if(!closed)return List.of();args.add(value.toString());at=selectWhitespace(input,at);
            if(at>=input.length()||input.charAt(at++)!=(i==count-1?')':','))return List.of();
        }
        at=selectWhitespace(input,at);if(at<input.length()&&input.charAt(at)==';')at=selectWhitespace(input,at+1);
        if(at==input.length())return List.copyOf(args);
        if(!input.startsWith("return",at))return List.of();int end=at+6;at=selectWhitespace(input,end);
        if(at==end||!input.startsWith("false",at))return List.of();at=selectWhitespace(input,at+5);
        if(at<input.length()&&input.charAt(at)==';')at=selectWhitespace(input,at+1);
        return at==input.length()?List.copyOf(args):List.of();
    }
    private static int selectWhitespace(String s,int i){while(i<s.length()&&" \t\r\n".indexOf(s.charAt(i))>=0)i++;return i;}
    private static boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private static boolean selectOpaque(String s){return s!=null&&s.length()<=2048&&CIPHER.matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private static String selectFormat(String s){String ext=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private static Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
