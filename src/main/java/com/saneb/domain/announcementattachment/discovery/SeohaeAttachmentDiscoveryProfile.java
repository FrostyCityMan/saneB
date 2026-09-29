package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeohaeNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 실제 첨부 영역의 개별 다운로드만 사용한다. 같은 파일의 미리보기는 요청하지 않는다. */
@Component
public final class SeohaeAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_SEOHAE_BOARD_V1", FILE="/open_content/main/bbs/bbsMsgFileDown.do", PREVIEW="/open_content/main/bbs/bbsMsgFileView.do";
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("SEOHAE:1|LGS-000062|HEURISTIC_NOTICE|gosi|https443|direct-get|same-request|paired-preview|partial-preserved|limit10|unknown-role|"
            +AttachmentProfileFingerprint.selectHash("PAGE:1",SeohaeNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000062","HEURISTIC_NOTICE"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(SeohaeNoticePage.HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000062".equals(s.localSourceCode())||!"HEURISTIC_NOTICE".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return SeohaeNoticePage.selectDetailUri(URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean safe(URI u){return u!=null&&"https".equals(u.getScheme())&&SeohaeNoticePage.HOST.equals(u.getHost())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath())&&u.equals(u.normalize());}
    private boolean fields(Map<String,String> q){return q.keySet().equals(Set.of("bcd","msg_seq","fileno"))&&"gosi".equals(q.get("bcd"))&&q.get("msg_seq").matches("[1-9][0-9]{0,14}")&&q.get("fileno").matches("[1-9][0-9]{0,14}");}
    @Override public boolean selectApprovedRequest(URI u){try{if(!safe(u))return false;if(SeohaeNoticePage.PATH.equals(u.getPath()))return u.equals(SeohaeNoticePage.selectDetailUri(u));return FILE.equals(u.getPath())&&fields(CapitalThirdNoticePage.selectParameters(u.getRawQuery()));}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){return r!=null&&"GET".equals(r.method())&&r.form().isEmpty()&&selectApprovedRequest(r.uri());}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source s,String html){
        URI detail=selectDetailUri(s);if(html==null||html.length()>1048576)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=SeohaeNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("msg_seq");var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        for(var item:area.select(":root > ul > li.margin_b5"))try{
            var anchors=item.select(":root > a[href]").stream().filter(a->!a.hasClass("btn_preview")).toList();if(anchors.size()!=1)continue;var a=anchors.getFirst();if(a.hasAttr("onclick")||a.attr("href").length()>4096)continue;
            URI file=detail.resolve(a.attr("href"));if(!FILE.equals(file.getPath())||!selectApprovedRequest(file))continue;var q=CapitalThirdNoticePage.selectParameters(file.getRawQuery());if(!notice.equals(q.get("msg_seq")))continue;
            String name=a.text().strip();if(name.isBlank()||name.length()>500||name.contains("/")||name.contains("\\")||name.contains("..")||name.indexOf('\ufffd')>=0||name.codePoints().anyMatch(Character::isISOControl))continue;
            String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);String id=normalizer.hash(notice+"\n"+q.get("fileno"));
            var d=new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,FILE,Map.of("noticeId",notice,"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported);var old=files.get(id);if(old!=null){if(!old.equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(id,d);
            var residual=item.clone();residual.select(":root > a:not(.btn_preview)").remove();
            if(!a.select("button,input,iframe,object,embed,script,[onclick]").isEmpty())unresolved=true;
            for(var img:a.select("img"))if(!img.attr("src").matches("/open_content/share/images/filetype/[a-z0-9]+\\.gif")||img.hasAttr("onerror"))unresolved=true;
            for(var size:residual.select(":root > span.sfont.wfont"))if(size.children().isEmpty()&&size.text().matches("\\([0-9.,]+(?:KByte|MByte|Byte)\\)"))size.remove();
            for(var preview:residual.select(":root > a.btn_preview")){URI u=detail.resolve(preview.attr("href"));if(safe(u)&&PREVIEW.equals(u.getPath())&&CapitalThirdNoticePage.selectParameters(u.getRawQuery()).equals(q)&&!preview.hasAttr("onclick")&&"미리보기".equals(preview.text().strip())&&preview.children().select("a,button,input,script,iframe,object,embed,[onclick],[href]").isEmpty())preview.remove();}
            if(residual(residual))unresolved=true;item.remove();
        }catch(IllegalArgumentException e){unresolved=true;}
        if(residual(area))unresolved=true;if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean residual(Element e){return !e.text().isBlank()||!e.select("a,button,input,select,form,iframe,object,embed,script,img,[onclick],[href]").isEmpty();}
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
