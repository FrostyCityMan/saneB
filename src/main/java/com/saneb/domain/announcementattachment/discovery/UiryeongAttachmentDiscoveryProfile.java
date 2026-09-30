package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.UiryeongNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 의령 공식 첨부 셀만 해석한다. query 시작에 관측된 탭 한 개만 전송 전에 정리한다. */
@Component
public final class UiryeongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_UIRYEONG_GET_V1",FILE_HOST="eminwon.uiryeong.go.kr",DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp",PREFIX="https://"+FILE_HOST+DOWNLOAD;
    private final SaeolGetAttachmentDiscoveryProfile validator=new SaeolGetAttachmentDiscoveryProfile(CODE,"LGS-000232",FILE_HOST,"HEURISTIC_NOTICE","td",false);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash(CODE+":1|gosiNo-and-dataSid|exact-leading-tab|paired-preview-no-fetch|same-request|unknown-role|limit10|"+validator.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",UiryeongNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000232","HEURISTIC_NOTICE"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(UiryeongNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000232".equals(s.localSourceCode())||!"HEURISTIC_NOTICE".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return UiryeongNoticePage.selectDetailUri(URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI uri){try{if(uri==null)return false;if(UiryeongNoticePage.HOST.equals(uri.getHost()))return uri.equals(UiryeongNoticePage.selectDetailUri(uri));return DOWNLOAD.equals(uri.getPath())&&validator.selectApprovedRequest(uri);}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private boolean selectActive(Element e){return !e.select("button,input,select,form,iframe,object,embed,script,style").isEmpty()||e.getAllElements().stream().anyMatch(n->n.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));}
    private boolean selectPreview(Element a,Map<String,String> file,URI detail){try{
        if(selectActive(a)||!a.ownText().isBlank()||a.childrenSize()!=1)return false;URI uri=detail.resolve(a.attr("href"));
        if(!"https".equals(uri.getScheme())||!UiryeongNoticePage.HOST.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!"/customuser/synap.uiryeong".equals(uri.getRawPath()))return false;
        var q=UiryeongNoticePage.selectParameters(uri.getRawQuery());var img=a.child(0);
        return q.keySet().equals(Set.of("p"))&&URLDecoder.decode(q.get("p"),StandardCharsets.UTF_8).equals(file.get("file_path")+":"+file.get("sys_file_nm")+":"+file.get("user_file_nm"))&&"img".equals(img.tagName())&&"/images/new/Potal/board/viwerIcon.png".equals(img.attr("src"))&&"바로보기".equals(img.attr("alt"));
    }catch(IllegalArgumentException e){return false;}}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element cell;
        try{cell=UiryeongNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()));}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        if(cell.children().isEmpty()&&cell.text().isBlank())return new Result("NO_FILES",true,List.of(),List.of());var lists=cell.select(":root > ul.fileBoxs");if(lists.size()!=1)return failed("ATTACHMENT_SELECTOR_CHANGED");
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;var residual=cell.clone();residual.select("ul.fileBoxs > li.gosiWrap").remove();if(!residual.text().isBlank()||!residual.select("a,img,[href]").isEmpty()||selectActive(residual))unresolved=true;
        for(var item:lists.getFirst().children())try{
            if(!"li".equals(item.tagName())||!item.hasClass("gosiWrap"))throw new IllegalArgumentException();var links=item.select(":root > a").stream().filter(a->a.attr("href").startsWith(PREFIX+"?")).toList();if(links.size()!=1)throw new IllegalArgumentException();var a=links.getFirst();if(selectActive(a)||!a.children().isEmpty())throw new IllegalArgumentException();
            String raw=a.attr("href");if(raw.startsWith(PREFIX+"?\tfile_path="))raw=PREFIX+"?file_path="+raw.substring((PREFIX+"?\tfile_path=").length());URI fetch=URI.create(raw.replace(" ","%20"));if(!selectApprovedRequest(fetch))throw new IllegalArgumentException();var q=UiryeongNoticePage.selectParameters(fetch.getRawQuery());String name=a.text().strip();if(!name.equals(q.get("user_file_nm")))throw new IllegalArgumentException();String id=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&q.get("sys_file_nm").toLowerCase(Locale.ROOT).endsWith("."+ext.toLowerCase(Locale.ROOT));
            var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",id,"noticeId",UiryeongNoticePage.selectParameters(detail.getRawQuery()).get("gosiNo"))),name,supported?ext:null,"UNKNOWN",supported);if(files.containsKey(id)){if(!files.get(id).equals(d))unresolved=true;}else if(files.size()==10)exceeded=true;else files.put(id,d);
            var rest=item.clone();rest.select(":root > a").stream().filter(n->n.attr("href").startsWith(PREFIX+"?")).toList().forEach(Element::remove);
            for(var p:rest.select("a"))if(selectPreview(p,q,detail))p.remove();for(var img:rest.select(":root > img"))if(!selectActive(img)&&"/images/new/Potal/board/fileIcon.png".equals(img.attr("src"))&&"다운로드".equals(img.attr("alt")))img.remove();
            if(!rest.text().replace('\u00a0',' ').isBlank()||!rest.select("a,img,[href]").isEmpty()||selectActive(rest))unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
