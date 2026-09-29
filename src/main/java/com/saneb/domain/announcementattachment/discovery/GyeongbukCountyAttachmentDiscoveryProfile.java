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

/** 공식 첨부의 GET·공개 POST·고정 파일 서버 이동을 연결하고 개별 실패를 분리한다. */
final class GyeongbukCountyAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { CHEONGSONG, YEONGYANG, ULLEUNG }
    private static final String GET="/emwp/jsp/ofr/FileDown.jsp",POST="/emwp/jsp/ofr/FileDownNew.jsp",BOARD="/programs/board/saeol/notice/download.do";
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;
    private final String code,sourceCode,parserCode,host,fileHost,path,idKey,hash;
    private final SaeolGetAttachmentDiscoveryProfile getValidator;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    GyeongbukCountyAttachmentDiscoveryProfile(Site site){
        this.site=site;code="LOCAL_"+site+"_COUNTY_V1";
        String domain=switch(site){case CHEONGSONG->"cs.go.kr";case YEONGYANG->"yyg.go.kr";case ULLEUNG->"ulleung.go.kr";};host="www."+domain;fileHost="eminwon."+domain;
        sourceCode=switch(site){case CHEONGSONG->"LGS-000212";case YEONGYANG->"LGS-000213";case ULLEUNG->"LGS-000222";};
        parserCode=switch(site){case CHEONGSONG->"HEURISTIC_NOTICE";case YEONGYANG->"SPRING_BBS";case ULLEUNG->"GUNWI_NOTICE_TABLE";};
        path=switch(site){case CHEONGSONG->"/news/00002679/00006203.web";case YEONGYANG->"/www/organization/yyg_news/notification";case ULLEUNG->"/ko/page.do";};idKey=site==Site.YEONGYANG?"idx":"not_ancmt_mgt_no";
        getValidator=new SaeolGetAttachmentDiscoveryProfile(code,sourceCode,fileHost,parserCode,"td",false);
        hash=AttachmentProfileFingerprint.selectHash(String.join("|",code,sourceCode,parserCode,host,fileHost,path,"1|https443|official-area|ephemeral-post4|ulleung-fixed-redirect|partial-preserved|unknown-role|limit10",getValidator.selectProfileHash()),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(host,fileHost);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parserCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){
        try{if(s==null||!selectProviderCode().equals(s.providerCode())||!sourceCode.equals(s.localSourceCode())||!parserCode.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();URI u=URI.create(s.sourceUrl());if(!host.equals(u.getHost())||!path.equals(u.getPath())||!selectApprovedRequest(u))throw new IllegalArgumentException();return u;}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafe(URI u){return u!=null&&"https".equals(u.getScheme())&&u.getHost()!=null&&selectApprovedHosts().contains(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    @Override public boolean selectApprovedRequest(URI u){
        if(!selectSafe(u))return false;var q=selectQuery(u.getRawQuery());
        if(fileHost.equals(u.getHost()))return site!=Site.YEONGYANG&&GET.equals(u.getPath())&&getValidator.selectApprovedRequest(u);
        if(site==Site.ULLEUNG&&BOARD.equals(u.getPath()))return q.keySet().equals(Set.of("file_seq",idKey))&&selectId(q.get("file_seq"))&&selectId(q.get(idKey));
        if(!path.equals(u.getPath())||!selectId(q.get(idKey)))return false;
        return switch(site){
            case CHEONGSONG->"view".equals(q.get("amode"))&&Set.of(idKey,"amode","withPast","cpage","sstring","stype","upunit").containsAll(q.keySet());
            case YEONGYANG->"view".equals(q.get("mode"))&&Set.of(idKey,"mode","page","search_type","search_word").containsAll(q.keySet());
            case ULLEUNG->"571".equals(q.get("mnu_uid"))&&"2".equals(q.get("cmd"))&&Set.of(idKey,"cmd","mnu_uid","boardType","board_code","srchSDate","srchKwd","srchColumn","srchDept","srchEDate","pageNo").containsAll(q.keySet())&&(!q.containsKey("boardType")||"notice".equals(q.get("boardType")));
        };
    }
    @Override public boolean selectApprovedRequest(Request r){
        if(r==null)return false;if("GET".equals(r.method()))return selectApprovedRequest(r.uri());var f=r.form();
        return site==Site.YEONGYANG&&"POST".equals(r.method())&&selectSafe(r.uri())&&fileHost.equals(r.uri().getHost())&&POST.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&f.keySet().equals(Set.of("csrf_token","user_file_nm","sys_file_nm","file_path"))&&f.get("csrf_token").matches("[a-f0-9]{64}")&&selectName(f.get("user_file_nm"))&&selectOpaque(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){
        if(first==null||next==null||!selectApprovedRequest(first)||!selectApprovedRequest(next))return false;if(first.equals(next))return true;
        return site==Site.ULLEUNG&&host.equals(first.uri().getHost())&&BOARD.equals(first.uri().getPath())&&fileHost.equals(next.uri().getHost())&&GET.equals(next.uri().getPath());
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());
        String title=switch(site){case CHEONGSONG->"form#saeolGosiVO div.board > div.view > div.title > h3";case YEONGYANG->"div.view_title > p.title";case ULLEUNG->"div.boardView > dl.title > dt";};
        String selector=switch(site){case CHEONGSONG->"form#saeolGosiVO div.view dl.attach > dd";case YEONGYANG->"div.view_box.file_area";case ULLEUNG->"div.boardView > dl.title > dd > ul";};
        var titles=page.select(title);var areas=page.select(selector);if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");var area=areas.getFirst();
        if(site==Site.CHEONGSONG&&(area.previousElementSibling()==null||!"첨부파일".equals(area.previousElementSibling().text())))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if(site==Site.YEONGYANG&&(area.select("div.file_tit > span.tit").size()!=1||!"첨부파일".equals(area.select("div.file_tit > span.tit").text())))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var blank=new LinkedHashMap<String,String>();
        if(site==Site.YEONGYANG){var forms=page.select("form[name=nnn]");if(forms.size()!=1)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();if(!"post".equalsIgnoreCase(form.attr("method"))||!("https://"+fileHost+POST).equals(form.attr("action")))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            for(var input:form.children())if(!"input".equals(input.tagName())||!"hidden".equals(input.attr("type"))||!input.attributes().asList().stream().allMatch(a->Set.of("type","name","value").contains(a.getKey()))||blank.putIfAbsent(input.attr("name"),input.val())!=null)return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            if(!blank.keySet().equals(Set.of("csrf_token","user_file_nm","sys_file_nm","file_path"))||!blank.get("csrf_token").matches("[a-f0-9]{64}")||FIELDS.stream().anyMatch(k->!blank.get(k).isEmpty()))return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
        String notice=selectQuery(detail.getRawQuery()).get(idKey);var files=new LinkedHashMap<String,Descriptor>();var known=new HashSet<Element>();var items=new HashSet<Element>();boolean exceeded=false;
        for(var link:area.select(site==Site.YEONGYANG?"button":"a"))try{
            String name,id;Request request;
            if(site==Site.YEONGYANG){if(!"button".equals(link.attr("type"))||!"다운로드".equals(link.text()))continue;var args=AttachmentDownloadInvocation.selectArguments(link.attr("onclick"),"goDownLoad",true,false);if(args.size()!=3)continue;var item=link.closest("li");if(item==null||item.select("span.file > span.name").size()!=1||item.select("span.file > span.info").size()!=1)continue;name=args.getFirst();String label=item.select("span.file > span.name").text()+"."+item.select("span.file > span.info").text().replaceAll("[\\[\\]]","");if(!selectNormalized(label).equals(selectNormalized(name)))continue;
                var form=new LinkedHashMap<>(blank);form.put("user_file_nm",name);form.put("sys_file_nm",args.get(1));form.put("file_path",args.get(2));request=new Request(URI.create("https://"+fileHost+POST),"POST",form);id=normalizer.hash(args.get(2)+"\n"+args.get(1));
            }else{if(link.hasAttr("onclick"))continue;name=link.text();URI u=detail.resolve(URI.create(link.attr("href").replace(" ","%20")));if(!selectApprovedRequest(u))continue;var q=selectQuery(u.getRawQuery());
                if(site==Site.ULLEUNG){if(!BOARD.equals(u.getPath())||!notice.equals(q.get(idKey)))continue;id=q.get("file_seq");}else{if(!GET.equals(u.getPath())||!name.equals(q.get("user_file_nm")))continue;id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));}request=Request.selectGet(u);
            }
            if(!selectName(name)||!selectApprovedRequest(request))continue;if(files.containsKey(id)){if(files.get(id).displayName().equals(name)&&files.get(id).selectRequest().equals(request))known.add(link);continue;}if(files.size()==10){exceeded=true;continue;}
            String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);if(!Set.of("PDF","HWP","HWPX").contains(ext))ext=null;
            // 폼의 일회성 값은 요청에만 사용하며 locator·감사 metadata에 저장하지 않는다.
            files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,request.uri().getPath(),Map.of("noticeId",notice,"attachmentId",id)),name,ext,"UNKNOWN",ext!=null,request.form()));known.add(link);if(link.closest("li")!=null)items.add(link.closest("li"));
        }catch(IllegalArgumentException ignored){/* 개별 실패를 남기고 다른 파일을 계속 수집한다. */}
        if(site==Site.ULLEUNG)for(var a:area.select("a"))if(!known.contains(a))try{var prev=a.previousElementSibling();if(prev==null||!known.contains(prev)||a.hasAttr("onclick")||!"[미리보기]".equals(a.text()))continue;URI u=detail.resolve(a.attr("href"));if(selectSafe(u)&&host.equals(u.getHost())&&"/programs/board/saeol/notice/fileView.do".equals(u.getPath())&&selectQuery(u.getRawQuery()).equals(selectQuery(detail.resolve(prev.attr("href")).getRawQuery())))known.add(a);}catch(IllegalArgumentException ignored){/* 뷰어는 실행하지 않는다. */}
        boolean unresolved=area.select("a,button").stream().anyMatch(a->!known.contains(a));known.forEach(Element::remove);
        if(site==Site.YEONGYANG){area.select("div.file_tit > span.tit").remove();items.forEach(i->i.select("span.file").remove());}
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!area.select("li,p.file").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectId(String s){return s!=null&&s.matches("[0-9]{1,15}");}
    private String selectNormalized(String s){return s.replaceAll("\\s+"," ").strip();}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectOpaque(String s){return s!=null&&s.length()<=2048&&java.util.regex.Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$").matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    private Map<String,String> selectQuery(String raw){if(raw==null||raw.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>2048||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}}
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
