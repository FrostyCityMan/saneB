package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeodaemunNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 고정 게시판의 다운로드 endpoint만 허용한다. 이미지·뷰어·본문 링크는 요청하지 않는다. */
@Component
public final class SeodaemunAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_SEODAEMUN_BOARD_V1",DOWNLOAD="/downloadFile.do",PREVIEW="/htmlView/html.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("SEODAEMUN:1|LGS-000014|SAFE_SEODAEMUN_NOTICE|www.sdm.go.kr|82|boardWrite|paired-preview-no-fetch|uuid|underscores-as-spaces-label|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",SeodaemunNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000014","SAFE_SEODAEMUN_NOTICE"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(SeodaemunNoticePage.HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000014".equals(s.localSourceCode())||!"SAFE_SEODAEMUN_NOTICE".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return SeodaemunNoticePage.selectDetailUri(URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafeValues(Map<String,String> q){String name=q.getOrDefault("oriFileNm","");return q.keySet().equals(Set.of("path","oriFileNm","saveFileNm"))&&"/board/82".equals(q.get("path"))&&!name.isBlank()&&name.length()<=500&&!name.contains("/")&&!name.contains("\\")&&!name.contains("..")&&!name.contains("%")&&name.indexOf('\ufffd')<0&&name.codePoints().noneMatch(Character::isISOControl)&&q.get("saveFileNm").matches("[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}\\.[A-Za-z0-9]{1,8}");}
    @Override public boolean selectApprovedRequest(URI u){try{if(u==null||!"https".equals(u.getScheme())||!SeodaemunNoticePage.HOST.equals(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!u.equals(u.normalize())||!u.getRawPath().equals(u.getPath()))return false;if(SeodaemunNoticePage.PATH.equals(u.getPath()))return u.equals(SeodaemunNoticePage.selectDetailUri(u));return DOWNLOAD.equals(u.getPath())&&selectSafeValues(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));}catch(IllegalArgumentException e){return false;}}
    private URI selectFileUri(String raw,boolean preview){String prefix=(preview?PREVIEW:DOWNLOAD)+"?";if(raw==null||!raw.startsWith(prefix)||raw.length()>8192||raw.contains("#"))throw new IllegalArgumentException();var q=CapitalThirdNoticePage.selectParameters(raw.substring(prefix.length()));if(!selectSafeValues(q))throw new IllegalArgumentException();var encoded=new StringJoiner("&");q.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->encoded.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));return URI.create("https://"+SeodaemunNoticePage.HOST+DOWNLOAD+"?"+encoded);}
    private String selectLabel(String text){return text.replace('_',' ').replaceAll("\\s+"," ").strip();}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");org.jsoup.nodes.Element container;try{container=SeodaemunNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()));}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var found=new LinkedHashMap<String,Descriptor>();var requests=new HashSet<URI>();boolean unresolved=false,exceeded=false;var previews=new ArrayList<org.jsoup.nodes.Element>();var residual=container.clone();residual.select("a").remove();if(!residual.text().isBlank()||!residual.select("button,input,script,iframe,object,embed,img,form,[onclick],[href]").isEmpty())unresolved=true;
        for(var anchor:container.select("a"))try{if(anchor.hasAttr("onclick")||!anchor.select("script,button,input,img,iframe,[onclick]").isEmpty())throw new IllegalArgumentException();if(anchor.attr("href").startsWith(PREVIEW+"?")){previews.add(anchor);continue;}URI fetch=selectFileUri(anchor.attr("href"),false);var q=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());String name=q.get("oriFileNm");if(!"다운로드".equals(anchor.attr("title"))||!selectLabel(name).equals(selectLabel(anchor.text())))throw new IllegalArgumentException();String id=normalizer.hash(q.get("path")+"\n"+q.get("saveFileNm")),ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&q.get("saveFileNm").toLowerCase(Locale.ROOT).endsWith("."+ext.toLowerCase(Locale.ROOT));var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("sdmBoardSeq"))),anchor.text().strip(),supported?ext:null,"UNKNOWN",supported);if(found.containsKey(id)){if(!found.get(id).equals(d))unresolved=true;}else if(found.size()==10)exceeded=true;else{found.put(id,d);requests.add(fetch);}}catch(IllegalArgumentException e){unresolved=true;}
        for(var preview:previews)try{if(!requests.contains(selectFileUri(preview.attr("href"),true)))unresolved=true;}catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(found.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(found.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(found.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(found.values()),List.of());}
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
