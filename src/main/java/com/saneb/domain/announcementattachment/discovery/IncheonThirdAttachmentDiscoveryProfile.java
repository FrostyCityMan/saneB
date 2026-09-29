package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.IncheonThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.IncheonThirdNoticePage.Site;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 개별 파일 링크만 사용한다. 미리보기·버튼 스크립트는 실행하지 않는다. */
final class IncheonThirdAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String GEOMDAN_FILE="/main/bbs/bbsMsgFileDown.do",YEONGJONG_FILE="/other/attach/process.file.do";
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    IncheonThirdAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_BOARD_V1";hash=AttachmentProfileFingerprint.selectHash("INCHEON_THIRD:1|"+s+"|"+s.sourceCode+"|"+s.listCode+"|https443|direct-get|same-request|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",IncheonThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.listCode));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!site.listCode.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return IncheonThirdNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafeUri(URI u){return u!=null&&u.toASCIIString().length()<=4096&&"https".equals(u.getScheme())&&site.host.equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    @Override public boolean selectApprovedRequest(URI u){try{if(!selectSafeUri(u))return false;if(site.path.equals(u.getPath()))return u.equals(IncheonThirdNoticePage.selectDetailUri(site,u));var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(site==Site.GEOMDAN)return GEOMDAN_FILE.equals(u.getPath())&&q.keySet().equals(Set.of("bcd","msg_seq","fileno"))&&"notice".equals(q.get("bcd"))&&selectNumber(q.get("msg_seq"))&&selectNumber(q.get("fileno"));return YEONGJONG_FILE.equals(u.getPath())&&q.keySet().equals(Set.of("TP","sn","key"))&&"dn".equals(q.get("TP"))&&selectNumber(q.get("sn"))&&q.get("key").matches("[A-F0-9]{15,64}");}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){return r!=null&&"GET".equals(r.method())&&r.form().isEmpty()&&selectApprovedRequest(r.uri());}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private boolean selectNumber(String s){return s!=null&&s.matches("[1-9][0-9]{0,14}");}
    private boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectAuxiliary(Element e,URI file,URI detail){try{String call=e.attr("onclick").replaceAll("\\s+","");String path;boolean preview;if(call.startsWith("location.href='")&&call.endsWith("'")){path=call.substring(15,call.length()-1);preview=false;}else if(call.startsWith("window.open('")&&call.endsWith("','_blank')")){path=call.substring(13,call.length()-11);preview=true;}else return false;URI u=detail.resolve(path);return selectSafeUri(u)&&(preview?"/main/bbs/bbsMsgFileView.do":GEOMDAN_FILE).equals(u.getPath())&&CapitalThirdNoticePage.selectParameters(u.getRawQuery()).equals(CapitalThirdNoticePage.selectParameters(file.getRawQuery()));}catch(IllegalArgumentException e1){return false;}}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1048576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element area;try{area=IncheonThirdNoticePage.selectAttachments(site,page).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey);
        for(var item:area.select(site==Site.GEOMDAN?":root > ul > li":":root > li"))try{
            var links=item.select(site==Site.GEOMDAN?":root > p.tit > a":":root > div.file_box > div.file_btns > a[download]");if(links.size()!=1)continue;var a=links.getFirst();if(a.hasAttr("onclick"))continue;URI u=detail.resolve(a.attr("href"));if(!selectApprovedRequest(u)||!(site==Site.GEOMDAN?GEOMDAN_FILE:YEONGJONG_FILE).equals(u.getPath()))continue;
            String name;if(site==Site.GEOMDAN){if(!a.children().isEmpty())continue;name=a.text().strip();}else{var names=a.select("span.skip");if(names.size()!=1)continue;name=names.getFirst().text().strip();}if(!selectName(name))continue;var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(site==Site.GEOMDAN&&!notice.equals(q.get("msg_seq")))continue;String id=normalizer.hash(notice+"\n"+q.get(site==Site.GEOMDAN?"fileno":"sn"));String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);var d=new Descriptor(u,new AttachmentSetEvidence.Locator(code,u.getPath(),Map.of("noticeId",notice,"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported,Map.of());var old=files.get(id);if(old!=null){if(!old.equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(id,d);
            var residual=item.clone();residual.select(site==Site.GEOMDAN?":root > p.tit > a":":root > div.file_box > div.file_btns > a[download]").remove();
            if(site==Site.GEOMDAN){for(var span:residual.select(":root > p.tit > span.margin_l10"))if(span.children().isEmpty()&&span.text().matches("\\[[0-9.,]+(?:KByte|MByte|Byte)\\]"))span.remove();for(var button:residual.select(":root > div.btn-wrap > button"))if(selectAuxiliary(button,u,detail))button.remove();}
            else{for(var fn:residual.select(":root > div.file_box > div.file_name"))if(fn.text().matches(java.util.regex.Pattern.quote(name)+" \\[[A-Za-z0-9]+, [0-9.]+(?:MB|KB|B)\\]")&&fn.select("a,button,input,script,[onclick],[href]").isEmpty())fn.remove();for(var preview:residual.select(":root > div.file_box > div.file_btns > a")){URI v=detail.resolve(preview.attr("href"));if(!preview.hasAttr("onclick")&&selectSafeUri(v)&&"/other/synap_viewer/view.do".equals(v.getPath())&&CapitalThirdNoticePage.selectParameters(v.getRawQuery()).equals(Map.of("atch_file_sn",q.get("sn"))))preview.remove();}}
            if(selectResidual(residual))unresolved=true;item.remove();
        }catch(IllegalArgumentException e){unresolved=true;}
        if(selectResidual(area))unresolved=true;if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectResidual(Element e){return !e.text().isBlank()||!e.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty();}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
