package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 공식 첨부 셀의 고정 contentsSid 다운로드만 연결한다. */
@Component
public final class GyeongnamProvinceAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_GYEONGNAM_PROVINCE_V1";
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash(CODE+":1|fixed-menu-sno-group-A|contentsSid3651|official-file-label|same-request|unknown-role|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",GyeongnamProvinceNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000223","SAEOL_GOSI"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(GyeongnamProvinceNoticePage.HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000223".equals(s.localSourceCode())||!"SAEOL_GOSI".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return GyeongnamProvinceNoticePage.selectDetailUri(URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI uri){try{
        if(uri==null||!"https".equals(uri.getScheme())||!GyeongnamProvinceNoticePage.HOST.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!GyeongnamProvinceNoticePage.PATH.equals(uri.getRawPath())||!uri.equals(uri.normalize()))return false;
        var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        if(q.containsKey("contentsSid"))return q.keySet().equals(Set.of("contentsSid","fn"))&&"3651".equals(q.get("contentsSid"))&&selectSafeName(q.get("fn"));
        return uri.equals(GyeongnamProvinceNoticePage.selectDetailUri(uri));
    }catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private boolean selectSafeName(String value){return value!=null&&!value.isBlank()&&value.length()<=500&&!value.contains("/")&&!value.contains("\\")&&!value.contains("..")&&!value.contains("%")&&value.indexOf('\ufffd')<0&&value.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectActive(Element e){return !e.select("button,input,select,form,iframe,object,embed,script,style,[href]:not(a)").isEmpty()||e.getAllElements().stream().anyMatch(n->n.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=GyeongnamProvinceNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()));}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=!area.ownText().isBlank(),exceeded=false;String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("sno");
        for(var span:area.children())try{
            if("br".equals(span.tagName())&&!selectActive(span))continue;
            if(!"span".equals(span.tagName())||!span.className().matches("attach_[1-9][0-9]*")||!span.ownText().isBlank()||span.childrenSize()!=1)throw new IllegalArgumentException();
            var a=span.child(0);if(!"a".equals(a.tagName())||!a.hasClass("file")||!a.children().isEmpty()||selectActive(span))throw new IllegalArgumentException();
            URI fetch=detail.resolve(a.attr("href"));if(!selectApprovedRequest(fetch))throw new IllegalArgumentException();var q=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());if(!"3651".equals(q.get("contentsSid")))throw new IllegalArgumentException();
            String name=a.text().strip();if(!name.equals(q.get("fn")))throw new IllegalArgumentException();String id=normalizer.hash(name),ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);
            var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,GyeongnamProvinceNoticePage.PATH,Map.of("noticeId",notice,"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported);
            if(files.containsKey(id)){if(!files.get(id).equals(d))unresolved=true;}else if(files.size()==10)exceeded=true;else files.put(id,d);
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
