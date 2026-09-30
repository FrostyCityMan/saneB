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

/** 고령의 파일 쌍과 진주의 공식 다운로드 프록시를 고정한다. 정상 파일과 미확인 항목을 분리한다. */
final class GoryeongJinjuAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { GORYEONG, JINJU }
    private final Site site;
    private final String code,host,sourceCode,parserCode,path,downloadPath,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile innerValidator;
    GoryeongJinjuAttachmentDiscoveryProfile(Site site){
        this.site=site;code="LOCAL_"+site+"_GET_V1";host=site==Site.GORYEONG?"www.goryeong.go.kr":"www.jinju.go.kr";sourceCode=site==Site.GORYEONG?"LGS-000216":"LGS-000225";parserCode=site==Site.GORYEONG?"SAFE_GORYEONG_BOARD":"SCMS_CARD_NOTICE";path=site==Site.GORYEONG?"/kor/boardView.do":"/00130/02730/05586.web";downloadPath=site==Site.GORYEONG?"/front/downFile.do":"/DownloadEx.do";
        innerValidator=new SaeolGetAttachmentDiscoveryProfile(code,sourceCode,"eminwon.jinju.go.kr",parserCode,"td",false);
        hash=AttachmentProfileFingerprint.selectHash(String.join("|",code,host,sourceCode,parserCode,path,downloadPath,"1|https443|goryeong-stored-http-upgrade|jinju-nested-official-url-only|same-request|partial-preserved|unknown-role|limit10",innerValidator.selectProfileHash()),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(host);}
    // 진주 공식 프록시가 HWPX에 반환하는 구형 MIME만 허용한다. 형식·첨부 filename 검증은 필수다.
    @Override public Set<String> selectLegacyBinaryContentTypes(){return site==Site.JINJU?Set.of("application/x-msdownload"):Set.of();}
    @Override public boolean selectUtf8DispositionOctets(){return site==Site.JINJU;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parserCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){
        try{if(s==null||!selectProviderCode().equals(s.providerCode())||!sourceCode.equals(s.localSourceCode())||!parserCode.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();URI u=URI.create(s.sourceUrl());
            // 운영 seed의 HTTP identity는 유지하고 확인된 동일 HTTPS 상세만 요청한다.
            if(site==Site.GORYEONG&&"http".equals(u.getScheme())&&host.equals(u.getHost())&&(u.getPort()==-1||u.getPort()==80)&&u.getUserInfo()==null&&u.getFragment()==null)u=URI.create("https://"+host+u.getRawPath()+"?"+u.getRawQuery());
            if(!path.equals(u.getPath())||!selectApprovedRequest(u))throw new IllegalArgumentException();return u;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafe(URI u){return u!=null&&"https".equals(u.getScheme())&&host.equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    @Override public boolean selectApprovedRequest(URI u){
        if(!selectSafe(u))return false;var q=selectQuery(u.getRawQuery());
        if(path.equals(u.getPath()))return site==Site.GORYEONG?"154".equals(q.get("IDX"))&&"1023".equals(q.get("BRD_ID"))&&selectId(q.get("BOARD_IDX"))&&Set.of("IDX","BRD_ID","BOARD_IDX","searchType","searchValue","page").containsAll(q.keySet()):"view".equals(q.get("amode"))&&selectId(q.get("not_ancmt_mgt_no"))&&Set.of("amode","not_ancmt_mgt_no","cpage","sstring","stype").containsAll(q.keySet());
        if(!downloadPath.equals(u.getPath()))return false;
        if(site==Site.GORYEONG)return q.keySet().equals(Set.of("IDX_FI","BRD_ID"))&&"1023".equals(q.get("BRD_ID"))&&selectId(q.get("IDX_FI"));
        if(!q.keySet().equals(Set.of("url","name"))||!selectName(q.get("name")))return false;
        try{URI inner=selectInnerUri(q.get("url"));if(!"http".equals(inner.getScheme())&&!"https".equals(inner.getScheme()))return false;
            // 프록시의 내부 URL도 공식 FileDown.jsp와 공개 파일3query로 제한한다. 내부 HTTP는 직접 호출하지 않는다.
            URI checked=URI.create(inner.toString().replaceFirst("^http:","https:"));return "/emwp/jsp/ofr/FileDown.jsp".equals(inner.getPath())&&innerValidator.selectApprovedRequest(checked)&&q.get("name").equals(selectQuery(inner.getRawQuery()).get("user_file_nm"));
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());var titles=page.select(site==Site.GORYEONG?"table.boardView_table > tbody > tr > th[scope=col][colspan=4]":"div.bbs1view1 > h1.h1");var areas=page.select(site==Site.GORYEONG?"table.boardView_table > tbody > tr > th:matchesOwn(^첨부$) + td":"div.bbs1view1 > div.attach1");if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");var area=areas.getFirst();
        String notice=selectQuery(detail.getRawQuery()).get(site==Site.GORYEONG?"BOARD_IDX":"not_ancmt_mgt_no");var files=new LinkedHashMap<String,Descriptor>();var known=new HashSet<Element>();boolean exceeded=false;
        for(var a:area.select("a"))try{
            if(a.hasAttr("onclick"))continue;URI u=detail.resolve(URI.create(a.attr("href")));if(!downloadPath.equals(u.getPath())||!selectApprovedRequest(u))continue;var q=selectQuery(u.getRawQuery());String name,id;Element label=null;
            if(site==Site.GORYEONG){label=a.previousElementSibling();if(!"다운로드".equals(a.text())||label==null||!label.hasClass("bV_file")||label.hasAttr("onclick"))continue;URI view=detail.resolve(label.attr("href"));if(!selectSafe(view)||!"/front/viewFile.do".equals(view.getPath())||!selectQuery(view.getRawQuery()).equals(q))continue;name=label.text().replaceFirst("\\s+\\[[A-Za-z0-9]+,\\s*[0-9]+(?:\\.[0-9]+)?(?:B|KB|MB)\\]$","");id=q.get("IDX_FI");
            }else{name=a.text();if(!name.equals(q.get("name")))continue;var inner=selectQuery(selectInnerUri(q.get("url")).getRawQuery());id=normalizer.hash(inner.get("file_path")+"\n"+inner.get("sys_file_nm"));}
            if(!selectName(name))continue;if(files.containsKey(id)){if(files.get(id).displayName().equals(name)&&files.get(id).fetchUri().equals(u)){known.add(a);if(label!=null)known.add(label);}continue;}if(files.size()==10){exceeded=true;continue;}
            String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);if(!Set.of("PDF","HWP","HWPX").contains(ext))ext=null;files.put(id,new Descriptor(u,new AttachmentSetEvidence.Locator(code,downloadPath,Map.of("noticeId",notice,"attachmentId",id)),name,ext,"UNKNOWN",ext!=null));known.add(a);if(label!=null)known.add(label);
        }catch(IllegalArgumentException ignored){/* 실패 링크를 분리하고 정상 파일은 유지한다. */}
        boolean unresolved=area.select("a").stream().anyMatch(a->!known.contains(a));known.forEach(Element::remove);if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!area.select("li,p").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private URI selectInnerUri(String raw){
        // 공식 프록시의 바깥 form 인코딩을 해제하면 파일명 공백이 나타난다.
        // query의 공백만 복원하고 호스트·경로·3개 파일 parameter 검증은 그대로 적용한다.
        int query=raw.indexOf('?');if(query<0)throw new IllegalArgumentException("ATTACHMENT_LINK_UNRESOLVED");
        return URI.create(raw.substring(0,query)+raw.substring(query).replace(" ","%20"));
    }
    private boolean selectId(String s){return s!=null&&s.matches("[0-9]{1,15}");}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private Map<String,String> selectQuery(String raw){if(raw==null||raw.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>4096||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}}
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
