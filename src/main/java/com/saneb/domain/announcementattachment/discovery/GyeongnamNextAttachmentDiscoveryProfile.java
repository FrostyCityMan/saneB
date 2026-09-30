package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GyeongnamNextNoticePage;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;

/** 김해·창녕 공식 첨부의 같은 호스트 DownloadEx 프록시만 연결한다. 내부 HTTP URL은 직접 요청하지 않는다. */
final class GyeongnamNextAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String DOWNLOAD="/DownloadEx.do",FILE="/emwp/jsp/ofr/FileDown.jsp";
    private final String code,host,fileHost,sourceCode,parserCode,path,hash;
    private final SaeolGetAttachmentDiscoveryProfile validator;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    GyeongnamNextAttachmentDiscoveryProfile(String region,String domain,String sourceCode,String parserCode,String path){
        code="LOCAL_"+region+"_SCMS_GET_V1";host="www."+domain;fileHost="eminwon."+domain;this.sourceCode=sourceCode;this.parserCode=parserCode;this.path=path;
        validator=new SaeolGetAttachmentDiscoveryProfile(code,sourceCode,fileHost,parserCode,"td",false);
        hash=AttachmentProfileFingerprint.selectHash(String.join("|",code,host,fileHost,sourceCode,parserCode,path,"1|official-form-root|nested-saeol-get|section01|same-request|partial-preserved|limit10|unknown-role",validator.selectProfileHash(),AttachmentProfileFingerprint.selectHash("PAGE:1",GyeongnamNextNoticePage.class)),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(host);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parserCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){try{
        if(source==null||!selectProviderCode().equals(source.providerCode())||!sourceCode.equals(source.localSourceCode())||!parserCode.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
        URI uri=URI.create(source.sourceUrl());if(!path.equals(uri.getPath())||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
    }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafe(URI uri){return uri!=null&&"https".equals(uri.getScheme())&&host.equals(uri.getHost())&&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null&&uri.getFragment()==null&&uri.equals(uri.normalize())&&Objects.equals(uri.getRawPath(),uri.getPath());}
    @Override public boolean selectApprovedRequest(URI uri){
        if(!selectSafe(uri))return false;var q=selectQuery(uri.getRawQuery());
        if(path.equals(uri.getPath()))return "view".equals(q.get("amode"))&&q.getOrDefault("not_ancmt_mgt_no","").matches("[0-9]{1,15}")&&Set.of("amode","not_ancmt_mgt_no","sstring","stype","cpage","section","deptCode").containsAll(q.keySet())&&(!q.containsKey("section")||host.equals("www.gimhae.go.kr")&&"01".equals(q.get("section")));
        return selectDownloadFields(uri)!=null;
    }
    private Map<String,String> selectDownloadFields(URI uri){
        if(!selectSafe(uri)||!DOWNLOAD.equals(uri.getPath()))return null;var q=selectQuery(uri.getRawQuery());
        if(!q.keySet().equals(Set.of("url","name")))return null;
        try{URI inner=URI.create(q.get("url").replace(" ","%20"));if(!Set.of("http","https").contains(Objects.toString(inner.getScheme(),""))||!FILE.equals(inner.getPath())||!validator.selectApprovedRequest(URI.create(inner.toString().replaceFirst("^http:","https:"))))return null;
            var fields=selectQuery(inner.getRawQuery());return q.get("name").equals(fields.get("user_file_nm"))?fields:null;
        }catch(IllegalArgumentException e){return null;}
    }
    @Override public boolean selectApprovedRequest(Request request){return request!=null&&"GET".equals(request.method())&&selectApprovedRequest(request.uri());}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(initial);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());org.jsoup.nodes.Element area;
        try{var areas=GyeongnamNextNoticePage.selectRoot(page,host).select(":root > div.attach1");if(areas.size()!=1)throw new IllegalArgumentException();area=areas.getFirst();}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        var files=new LinkedHashMap<String,Descriptor>();var known=new HashSet<org.jsoup.nodes.Element>();boolean exceeded=false;
        String notice=selectQuery(detail.getRawQuery()).get("not_ancmt_mgt_no");
        for(var anchor:area.select("a"))try{
            if(anchor.hasAttr("onclick"))continue;URI uri=detail.resolve(URI.create(anchor.attr("href").replace(" ","%20")));var fields=selectDownloadFields(uri);
            if(fields==null||!anchor.text().equals(fields.get("user_file_nm")))continue;String id=normalizer.hash(fields.get("file_path")+"\n"+fields.get("sys_file_nm"));
            if(files.containsKey(id)){if(files.get(id).fetchUri().equals(uri)&&files.get(id).displayName().equals(anchor.text()))known.add(anchor);continue;}
            if(files.size()==10){exceeded=true;continue;}String format=selectFormat(anchor.text());boolean supported=format!=null&&format.equals(selectFormat(fields.get("sys_file_nm")));
            files.put(id,new Descriptor(uri,new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),anchor.text(),supported?format:null,"UNKNOWN",supported));known.add(anchor);
        }catch(IllegalArgumentException ignored){/* 개별 링크 실패와 정상 파일을 분리한다. */}
        boolean unresolved=area.select("a").stream().anyMatch(a->!known.contains(a));var residual=area.clone();residual.select("a").remove();
        if(!residual.text().isBlank()||!residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!area.select("li").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private String selectFormat(String name){String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Map<String,String> selectQuery(String raw){if(raw==null||raw.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>4096||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}}
    private Result failed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
