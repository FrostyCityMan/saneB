package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.IncheonPortalNoticePage;
import com.saneb.domain.announcementsource.provider.content.IncheonPortalNoticePage.Site;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 파일 링크의 암호화 query는 해석하지 않고 그대로 전송하며 감사 근거에는 hash만 남긴다. */
final class IncheonPortalAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String DOWNLOAD="/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Pattern OPAQUE=Pattern.compile("[A-Za-z0-9+/]{22,}={0,2}$");
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    IncheonPortalAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_PORTAL_V1";hash=AttachmentProfileFingerprint.selectHash("INCHEON_PORTAL:1|"+s+"|"+s.sourceCode+"|SPRING_BBS|"+s.host+"|"+s.fileHost+"|https443|opaque-three-field-get|same-request-only|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",IncheonPortalNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,"SPRING_BBS"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!"SPRING_BBS".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return IncheonPortalNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI u){try{if(u==null||u.toASCIIString().length()>8192||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!u.equals(u.normalize())||u.getRawPath()==null||!u.getRawPath().equals(u.getPath()))return false;if(site.host.equals(u.getHost())&&IncheonPortalNoticePage.DETAIL.equals(u.getPath()))return u.equals(IncheonPortalNoticePage.selectDetailUri(site,u));if(!site.fileHost.equals(u.getHost())||!DOWNLOAD.equals(u.getPath()))return false;var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());return q.keySet().equals(Set.of("user_file_nm","sys_file_nm","file_path"))&&selectOpaque(q.get("user_file_nm"))&&selectOpaque(q.get("sys_file_nm"))&&q.get("file_path").length()<=2048&&q.get("file_path").matches("/ntisho[A-Za-z0-9+/]{43,}={0,2}");}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private boolean selectOpaque(String s){return s!=null&&s.length()>=22&&s.length()<=2048&&OPAQUE.matcher(s).find()&&!s.contains("..")&&s.codePoints().noneMatch(Character::isISOControl)&&s.chars().noneMatch(c->"\\%<>:?#".indexOf(c)>=0);}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1048576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;try{area=IncheonPortalNoticePage.selectAttachments(site,Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean unresolved=false,exceeded=false;
        for(var item:area.select(":root > li.margin_b5"))try{var anchors=item.select(":root > a");if(anchors.size()!=1)continue;var a=anchors.getFirst();if(a.hasAttr("onclick"))continue;URI u=URI.create(a.attr("href"));if(!site.fileHost.equals(u.getHost())||!DOWNLOAD.equals(u.getPath())||!selectApprovedRequest(u))continue;var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());String name=a.text().strip();if(!selectName(name))continue;String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);var icons=a.select(":root > img");if(icons.size()!=1)continue;var icon=icons.getFirst();if(!ext.equalsIgnoreCase(icon.attr("alt"))||!selectIcon(icon.attr("src"),ext)||icon.hasAttr("onerror")||icon.hasAttr("onclick"))continue;
            String id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));var d=new Descriptor(u,new AttachmentSetEvidence.Locator(code,DOWNLOAD,Map.of("noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("seq"),"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported);var old=files.get(id);if(old!=null){if(!old.equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(id,d);
            var residual=item.clone();residual.select(":root > a").remove();var inner=a.clone();inner.select(":root > img").remove();if(selectResidual(residual)||!inner.children().isEmpty())unresolved=true;else recognized.add(item);
        }catch(IllegalArgumentException e){unresolved=true;}
        for(var item:recognized)item.remove();if(selectResidual(area))unresolved=true;if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());}
    private boolean selectIcon(String src,String ext){String prefix="/open_content/share/images/filetype/";return prefix.equals(src)||src.matches(Pattern.quote(prefix)+"(?:;jsessionid=[A-Fa-f0-9]{16,128})?"+Pattern.quote(ext.toLowerCase(Locale.ROOT))+"\\.gif");}
    private boolean selectResidual(Element e){return !e.text().isBlank()||!e.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty();}
    private boolean selectName(String s){return !s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
