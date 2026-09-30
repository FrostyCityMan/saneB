package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SacheonNoticePage;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 사천 공개 첨부의 일회성 세션 경로는 전송에만 사용하고 locator에는 저장하지 않는다. */
@org.springframework.stereotype.Component
public final class SacheonAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_SACHEON_BOARD_V1",HOST=SacheonNoticePage.HOST,DETAIL=SacheonNoticePage.PATH,DOWNLOAD="/board/download.do",VIEWER="/sn3hcv_convert.jsp";
    private static final String HASH=AttachmentProfileFingerprint.selectHash(CODE+":1|LGS-000227|HEURISTIC_NOTICE|"+HOST+"|"+DETAIL+"|gcode2017|official-attach|session32-transport-only|same-request|preview-tts-not-fetched|unknown-role|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",SacheonNoticePage.class),SacheonAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return HASH;}
    // 실제 ZIP 서명과 함께 관측한 구형 MIME·UTF-8 파일명 응답만 호환한다. 서명 검증은 유지한다.
    @Override public Set<String> selectLegacyBinaryContentTypes(){return Set.of("application/x-msdownload");}
    @Override public boolean selectUtf8DispositionOctets(){return true;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(HOST);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000227","HEURISTIC_NOTICE"));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000227".equals(s.localSourceCode())||!"HEURISTIC_NOTICE".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();URI u=URI.create(s.sourceUrl());if(!DETAIL.equals(u.getPath())||!selectApprovedRequest(u))throw new IllegalArgumentException();return u;}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafe(URI u){return u!=null&&"https".equals(u.getScheme())&&HOST.equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&Objects.equals(u.getRawPath(),u.getPath());}
    @Override public boolean selectApprovedRequest(URI u){
        if(!selectSafe(u))return false;var q=selectQuery(u.getRawQuery());
        if(DETAIL.equals(u.getPath()))return "2017".equals(q.get("gcode"))&&"view".equals(q.get("amode"))&&q.getOrDefault("idx","").matches("[0-9]{1,15}")&&Set.of("gcode","idx","amode","cpage","stype","sstring","wmode").containsAll(q.keySet());
        return selectDownload(u);
    }
    private boolean selectDownload(URI u){if(!selectSafe(u)||!u.getPath().matches("/board/download\\.do(?:;jsessionid=[A-Fa-f0-9]{32})?"))return false;var q=selectQuery(u.getRawQuery());return q.keySet().equals(Set.of("gcode","name"))&&"2017".equals(q.get("gcode"))&&selectName(q.get("name"));}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(first);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element area;
        try{var areas=SacheonNoticePage.selectRoot(page).select(":root > div.attach1");if(areas.size()!=1)throw new IllegalArgumentException();area=areas.getFirst();}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        var files=new LinkedHashMap<String,Descriptor>();var known=new HashSet<Element>();var items=new LinkedHashMap<Element,Descriptor>();boolean exceeded=false;
        String notice=selectQuery(detail.getRawQuery()).get("idx");
        for(var a:area.select("a.filename"))try{
            if(a.hasAttr("onclick")||!selectName(a.text()))continue;URI u=detail.resolve(URI.create(a.attr("href").replace(" ","%20")));if(!selectDownload(u))continue;var q=selectQuery(u.getRawQuery());if(!a.text().equals(q.get("name")))continue;
            String id=normalizer.hash("2017\n"+q.get("name"));if(files.containsKey(id)){var old=files.get(id);if(old.fetchUri().equals(u)&&old.displayName().equals(a.text())){known.add(a);if(a.closest("li")!=null)items.put(a.closest("li"),old);}continue;}
            if(files.size()==10){exceeded=true;continue;}String format=selectFormat(a.text());var locator=new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id));
            var descriptor=new Descriptor(u,locator,a.text(),format,"UNKNOWN",format!=null);files.put(id,descriptor);known.add(a);if(a.closest("li")!=null)items.put(a.closest("li"),descriptor);
        }catch(IllegalArgumentException ignored){/* 잘못된 개별 첨부와 정상 파일을 분리한다. */}
        for(var entry:items.entrySet())for(var a:entry.getKey().select("a"))if(!known.contains(a))try{
            if(a.hasAttr("onclick"))continue;URI u=detail.resolve(URI.create(a.attr("href").replace(" ","%20")));var d=entry.getValue();
            if(a.hasClass("download")&&"다운로드".equals(a.text())&&(d.displayName()+" 다운로드").equals(a.attr("title"))&&d.fetchUri().equals(u)){known.add(a);continue;}
            if(!selectSafe(u)||!VIEWER.equals(u.getPath()))continue;var q=selectQuery(u.getRawQuery());if(!"2017".equals(q.get("gcode"))||!d.displayName().equals(q.get("name")))continue;
            boolean view="바로보기".equals(a.text())&&q.keySet().equals(Set.of("gcode","name"));boolean audio="바로듣기".equals(a.text())&&"1".equals(q.get("tts"))&&q.keySet().equals(Set.of("gcode","name","tts"));if(view||audio)known.add(a);
        }catch(IllegalArgumentException ignored){/* 미리보기와 음성 변환은 요청하지 않는다. */}
        boolean unresolved=area.select("a").stream().anyMatch(a->!known.contains(a));known.forEach(Element::remove);
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!area.select("li").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String s){String ext=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Map<String,String> selectQuery(String raw){if(raw==null||raw.length()>4096)return Map.of();var q=new LinkedHashMap<String,String>();try{for(String pair:raw.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>500||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}}
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
