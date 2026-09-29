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

/** 공식 첨부 영역의 직접 링크만 수집한다. 미확인 항목과 정상 파일을 분리한다. */
final class GyeongbukDirectAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { YEONGDEOK, ULJIN }
    private final Site site;
    private final String code,host,sourceCode,parserCode,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    GyeongbukDirectAttachmentDiscoveryProfile(Site site){
        this.site=site;code="LOCAL_"+site+"_DIRECT_V1";
        host=site==Site.YEONGDEOK?"www.yd.go.kr":"www.uljin.go.kr";
        sourceCode=site==Site.YEONGDEOK?"LGS-000214":"LGS-000221";
        parserCode=site==Site.YEONGDEOK?"SPRING_BBS":"HEURISTIC_NOTICE";
        hash=AttachmentProfileFingerprint.selectHash(String.join("|",code,host,sourceCode,parserCode,"1|https443|official-area|same-request|partial-preserved|unknown-role|limit10"),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return site==Site.YEONGDEOK?Set.of(host):Set.of(host,"eminwon.uljin.go.kr");}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parserCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){
        try{
            if(s==null||!selectProviderCode().equals(s.providerCode())||!sourceCode.equals(s.localSourceCode())||!parserCode.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();
            URI u=URI.create(s.sourceUrl());if(!selectDetail(u))throw new IllegalArgumentException();return u;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafe(URI u){return u!=null&&"https".equals(u.getScheme())&&selectApprovedHosts().contains(u.getHost()==null?"":u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    private boolean selectDetail(URI u){
        if(!selectSafe(u)||!host.equals(u.getHost()))return false;var q=selectQuery(u.getRawQuery());
        if(site==Site.YEONGDEOK)return "/".equals(u.getPath())&&"763".equals(q.get("page_id"))&&"document".equals(q.get("mod"))&&selectId(q.get("uid"))&&Set.of("page_id","uid","mod","pageid","target","keyword","tab").containsAll(q.keySet());
        return "/index.uljin".equals(u.getPath())&&"DOM_000000103002007001".equals(q.get("menuCd"))&&"view".equals(q.get("type"))&&selectId(q.get("ancmtMgtNo"))&&Set.of("menuCd","type","ancmtMgtNo","searchType","keyword","startPage").containsAll(q.keySet());
    }
    private boolean selectDownload(URI u){
        if(!selectSafe(u))return false;var q=selectQuery(u.getRawQuery());
        if(site==Site.YEONGDEOK)return host.equals(u.getHost())&&"/".equals(u.getPath())&&q.keySet().equals(Set.of("action","uid","file"))&&"kboard_file_download".equals(q.get("action"))&&selectId(q.get("uid"))&&q.get("file").matches("file[1-9][0-9]?");
        return "eminwon.uljin.go.kr".equals(u.getHost())&&"/emwp/jsp/ofr/FileDown.jsp".equals(u.getPath())&&q.keySet().equals(Set.of("file_path","sys_file_nm","user_file_nm"))&&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}")&&selectName(q.get("sys_file_nm"))&&selectName(q.get("user_file_nm"));
    }
    @Override public boolean selectApprovedRequest(URI u){return selectDetail(u)||selectDownload(u);}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());
        var titles=page.select(site==Site.YEONGDEOK?"div.kboard-document-wrap > div.kboard-title":"table.bbs_tablev > tbody > tr > th:matchesOwn(^제목$) + td");
        var areas=page.select(site==Site.YEONGDEOK?"div.kboard-document-wrap > div.kboard-attach":"table.bbs_tablev > tbody > tr > th:matchesOwn(^첨부파일$) + td");
        if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var area=areas.getFirst();if(site==Site.YEONGDEOK&&!"첨부파일 :".equals(area.ownText()))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean exceeded=false;
        String notice=selectQuery(detail.getRawQuery()).get(site==Site.YEONGDEOK?"uid":"ancmtMgtNo");
        for(var a:area.select("a"))try{
            if(a.hasAttr("onclick")||!selectName(a.text()))continue;URI u=detail.resolve(URI.create(a.attr("href").replace(" ","%20")));if(!selectDownload(u))continue;var q=selectQuery(u.getRawQuery());
            if(site==Site.YEONGDEOK?!notice.equals(q.get("uid")):!a.text().equals(q.get("user_file_nm")))continue;
            String key=site==Site.YEONGDEOK?q.get("file"):normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));
            if(files.containsKey(key)){if(files.get(key).fetchUri().equals(u)&&files.get(key).displayName().equals(a.text()))recognized.add(a);continue;}
            if(files.size()==10){exceeded=true;continue;}String ext=a.text().substring(a.text().lastIndexOf('.')+1).toUpperCase(Locale.ROOT);if(!Set.of("PDF","HWP","HWPX").contains(ext))ext=null;
            files.put(key,new Descriptor(u,new AttachmentSetEvidence.Locator(code,u.getPath(),Map.of("noticeId",notice,"attachmentId",key)),a.text(),ext,"UNKNOWN",ext!=null));recognized.add(a);
        }catch(IllegalArgumentException ignored){/* 하나의 잘못된 링크가 정상 첨부를 버리지 않게 한다. */}
        // 미리보기는 실행하지 않는다. 아직 검증하지 않은 변형은 발견 오류로 남긴다.
        boolean unresolved=area.select("a").stream().anyMatch(a->!recognized.contains(a));var residual=area.clone();residual.select("a").remove();
        String remainder=residual.text();if(site==Site.YEONGDEOK)remainder=remainder.replaceFirst("^첨부파일\\s*:","").strip();
        if(!remainder.isBlank()||!residual.select("button,input,form,iframe,object,embed,script,[onclick],[href],img").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectId(String s){return s!=null&&s.matches("[0-9]{1,15}");}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private Map<String,String> selectQuery(String raw){
        if(raw==null||raw.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();
        try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>500||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
