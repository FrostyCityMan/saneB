package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.JangheungNoticePage;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 장흥 공식 첨부 영역의 고정 window.open 문자열만 읽는다. JavaScript는 실행하지 않는다. */
@org.springframework.stereotype.Component
public final class JangheungAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_JANGHEUNG_GET_V1", HOST=JangheungNoticePage.HOST, PATH=JangheungNoticePage.PATH;
    private static final String FILE_HOST="eminwon.jangheung.go.kr", DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Pattern OPEN=Pattern.compile("^window\\.open\\('([^'\\\\\\p{Cntrl}]{1,8192})',\\s*'_blank'\\);?$");
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile validator=new SaeolGetAttachmentDiscoveryProfile(CODE,"LGS-000189",FILE_HOST,"SPRING_BBS","td",false);
    private final String hash=AttachmentProfileFingerprint.selectHash(CODE+":1|LGS-000189|SPRING_BBS|"+HOST+"|"+FILE_HOST+"|"+PATH+"|"+DOWNLOAD+"|labelled-file-area|fixed-window-open|same-request|unknown-role|limit10|"+validator.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",JangheungNoticePage.class),JangheungAttachmentDiscoveryProfile.class);
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(HOST,FILE_HOST);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000189","SPRING_BBS"));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000189".equals(source.localSourceCode())||!"SPRING_BBS".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());if(!HOST.equals(uri.getHost())||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    private boolean selectSafe(URI u){return u!=null&&"https".equals(u.getScheme())&&selectApprovedHosts().contains(u.getHost()==null?"":u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    @Override public boolean selectApprovedRequest(URI uri){
        if(!selectSafe(uri))return false;
        if(FILE_HOST.equals(uri.getHost()))return DOWNLOAD.equals(uri.getPath())&&validator.selectApprovedRequest(uri);
        var q=selectQuery(uri.getRawQuery());return PATH.equals(uri.getPath())&&q.getOrDefault("idx","").matches("[0-9]{1,15}")&&"view".equals(q.get("mode"))&&Set.of("idx","mode","page","search_type","search_word","page_scale","start_date","finish_date").containsAll(q.keySet());
    }
    @Override public boolean selectApprovedRequest(Request request){return request!=null&&"GET".equals(request.method())&&selectApprovedRequest(request.uri());}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(first);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element area;
        try{area=JangheungNoticePage.selectFiles(JangheungNoticePage.selectRoot(page));}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=selectQuery(detail.getRawQuery()).get("idx");var files=new LinkedHashMap<String,Descriptor>();var known=new HashSet<Element>();var items=new HashSet<Element>();boolean exceeded=false;
        for(var button:area.select("div.file_cnt > ul.file_list > li button"))try{
            if(!"button".equals(button.attr("type"))||!"다운로드".equals(button.text()))continue;
            var item=button.closest("li");var names=item.select("span.file > span.name");var extensions=item.select("span.file > span.info");
            if(names.size()!=1||extensions.size()!=1||!extensions.text().matches("\\[[A-Za-z0-9]+\\]"))continue;
            URI uri=selectOpenUri(button.attr("onclick"));if(!selectApprovedRequest(uri)||!FILE_HOST.equals(uri.getHost()))continue;
            var q=selectQuery(uri.getRawQuery());String name=q.get("user_file_nm");String label=names.text()+"."+extensions.text().replaceAll("[\\[\\]]","");
            if(!selectNormalized(label).equals(selectNormalized(name)))continue;
            String id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));
            if(files.containsKey(id)){if(files.get(id).displayName().equals(name)&&files.get(id).fetchUri().equals(uri)){known.add(button);items.add(item);}continue;}
            if(files.size()==10){exceeded=true;continue;}
            String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(q.get("sys_file_nm")));
            files.put(id,new Descriptor(uri,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),name,supported?format:null,"UNKNOWN",supported));known.add(button);items.add(item);
        }catch(IllegalArgumentException ignored){/* 개별 오류가 다른 정상 첨부를 막지 않는다. */}
        for(var item:items)for(var button:item.select("button"))if(!known.contains(button))try{
            if(!"button".equals(button.attr("type"))||!"바로가기".equals(button.text()))continue;
            URI preview=detail.resolve(selectOpenUri(button.attr("onclick")));if(selectSafe(preview)&&HOST.equals(preview.getHost())&&preview.getRawQuery()==null&&preview.getPath().matches("/Viewer_gosi/"+notice+"_[1-9][0-9]*"))known.add(button);
        }catch(IllegalArgumentException ignored){/* 미리보기는 요청하지 않는다. */}
        boolean unresolved=area.select("a,button").stream().anyMatch(e->!known.contains(e));known.forEach(Element::remove);
        area.select("div.file_tit > span.tit").remove();items.forEach(i->i.select("span.file").remove());
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!area.select("li").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private URI selectOpenUri(String invocation){if(invocation==null||invocation.length()>8300)throw new IllegalArgumentException();var match=OPEN.matcher(invocation.strip());if(!match.matches())throw new IllegalArgumentException();return URI.create(match.group(1).replace(" ","%20"));}
    private String selectNormalized(String value){return value.replaceAll("\\s+"," ").strip();}
    private String selectFormat(String name){String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Map<String,String> selectQuery(String raw){if(raw==null||raw.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>2048||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}}
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
