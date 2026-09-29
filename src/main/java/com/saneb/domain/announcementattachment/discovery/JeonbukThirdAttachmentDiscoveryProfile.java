package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage.Site;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 파일 링크만 내려받고 전체 ZIP·미리보기는 실행하지 않는다. */
final class JeonbukThirdAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    JeonbukThirdAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_BOARD_V1";hash=AttachmentProfileFingerprint.selectHash("JEONBUK_THIRD:1|"+s+"|"+s.sourceCode+"|SPRING_BBS|https443|same-request|same-board-notice|detail1MiB|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",JeonbukThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,"SPRING_BBS"));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){try{if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!"SPRING_BBS".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();return JeonbukThirdNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI u){
        try{if(!JeonbukThirdNoticePage.selectSafeUri(site,u))return false;if(site.path.equals(u.getPath()))return u.equals(JeonbukThirdNoticePage.selectDetailUri(site,u));if(!site.download.equals(u.getPath()))return false;
            var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!site.board.equals(q.get(site.boardKey))||!site.selectId(q.get(site.fileKey)))return false;
            if(site==Site.JEONJU)return q.keySet().equals(Set.of(site.boardKey,site.fileKey));
            return Set.of("boardId","menuCd","dataSid","fileSid","command","paging","startPage").containsAll(q.keySet())&&site.menu.equals(q.get(site.menuKey))&&site.selectId(q.get(site.idKey))&&(!q.containsKey("command")||"update".equals(q.get("command")))&&(!q.containsKey("paging")||"ok".equals(q.get("paging")))&&(!q.containsKey("startPage")||q.get("startPage").matches("[1-9][0-9]{0,5}"));
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1048576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=JeonbukThirdNoticePage.selectAttachments(site,Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=JeonbukThirdNoticePage.selectParameters(site,detail).get(site.idKey);var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean unresolved=false,exceeded=false;
        for(var a:area.select("a[href]"))try{
            if(a.hasAttr("onclick"))continue;URI fetch=detail.resolve(URI.create(a.attr("href")));if(!site.download.equals(fetch.getPath())||!selectApprovedRequest(fetch))continue;
            var q=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());if(site==Site.JEONBUK&&!notice.equals(q.get(site.idKey)))continue;
            String name=site==Site.JEONJU?a.text().replaceFirst("\\s*\\([0-9]+(?:\\.[0-9]+)?(?:KB|MB|B)\\)$","").strip():a.attr("title").strip();if(!selectSafeName(name))continue;
            String id=q.get(site.fileKey);var old=files.get(id);if(old!=null){if(old.fetchUri().equals(fetch)&&old.displayName().equals(name))recognized.add(a);continue;}
            if(files.size()==10){exceeded=true;continue;}String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);
            files.put(id,new Descriptor(fetch,new AttachmentSetEvidence.Locator(code,site.download,Map.of("noticeId",notice,"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported));recognized.add(a);
        }catch(IllegalArgumentException ignored){/* 실패한 링크는 정상 파일과 분리한다. */}
        for(var a:area.select("a"))if(!recognized.contains(a)){
            boolean auxiliary=false;try{URI u=detail.resolve(URI.create(a.attr("href")));if(!a.hasAttr("onclick")&&JeonbukThirdNoticePage.selectSafeUri(site,u)){
                var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
                if(site==Site.JEONJU)auxiliary="/synap/convert.jsp".equals(u.getPath())&&q.keySet().equals(Set.of("fileUid"))&&files.containsKey(q.get("fileUid"))&&"미리보기".equals(a.text());
                else if("/board/SynapViewer.jeonbuk".equals(u.getPath())&&a.hasClass("ico_viewer"))auxiliary=files.values().stream().anyMatch(d->d.fetchUri().equals(URI.create(u.toString().replace("/board/SynapViewer.jeonbuk","/board/download.jeonbuk"))));
                else if("/board/downloadAll.jeonbuk".equals(u.getPath())&&a.hasClass("ico_allfile"))auxiliary=q.equals(Map.of("boardId",site.board,"dataSid",notice))&&!files.isEmpty();
            }}catch(IllegalArgumentException ignored){}
            if(auxiliary)recognized.add(a);else unresolved=true;
        }
        for(var a:recognized)a.remove();String residual=area.text();if(site==Site.JEONBUK)for(String name:files.values().stream().map(Descriptor::displayName).sorted(Comparator.comparingInt(String::length).reversed()).toList())residual=residual.replace(name,"");residual=residual.replaceAll("\\[[0-9]+(?:\\.[0-9]+)?\\s*(?:kb|mb|Byte)\\]","").strip();
        if(!residual.isEmpty()||!area.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectSafeName(String n){return !n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&!n.contains("%")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
