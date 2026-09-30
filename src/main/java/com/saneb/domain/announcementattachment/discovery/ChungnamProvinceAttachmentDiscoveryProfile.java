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

/** 충남도 공식 파일 목록만 읽고 대응 미리보기/미리듣기는 실행하지 않는다. */
@Component
public final class ChungnamProvinceAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String CODE="LOCAL_CHUNGNAM_PROVINCE_V1",FILE_HOST="minwon.chungnam.go.kr",DOWNLOAD="/citynet/jsp/cmm/attach/download.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash(CODE+":1|fixed-menu-nttId|citynet-four-parameters|paired-preview-listen-no-fetch|same-request|unknown-role|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",ChungnamProvinceNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CITYNET_RESPONSE:1",CitynetAttachmentFileResponse.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000147","SPRING_BBS"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(ChungnamProvinceNoticePage.HOST,FILE_HOST);}
    @Override public Set<String> selectLegacyBinaryContentTypes(){return Set.of(CitynetAttachmentFileResponse.LEGACY_MIME);}
    @Override public AttachmentPinnedDownloadClient.Download selectDownload(Request initial,java.nio.file.Path output,long maximumBytes,Operation operation)throws java.io.IOException{return CitynetAttachmentFileResponse.selectNormalized(operation.selectDownload(initial,maximumBytes),output);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000147".equals(s.localSourceCode())||!"SPRING_BBS".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return ChungnamProvinceNoticePage.selectDetailUri(URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI uri){try{
        if(uri==null||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!uri.equals(uri.normalize()))return false;
        if(ChungnamProvinceNoticePage.selectMatches(uri))return uri.equals(ChungnamProvinceNoticePage.selectDetailUri(uri));
        if(!FILE_HOST.equals(uri.getHost())||!DOWNLOAD.equals(uri.getRawPath()))return false;var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
        return q.keySet().equals(Set.of("mode","fid","index","other"))&&"download".equals(q.get("mode"))&&q.get("fid").matches("#[a-f0-9]{64}")&&q.get("other").matches("#[a-f0-9]{96}")&&q.get("index").matches("[0-9]{1,3}");
    }catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private boolean selectActive(Element e){return !e.select("button,input,select,form,iframe,object,embed,script,style").isEmpty()||e.getAllElements().stream().anyMatch(n->n.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));}
    private boolean selectSafeName(String name){return !name.isBlank()&&name.length()<=500&&!name.contains("/")&&!name.contains("\\")&&!name.contains("..")&&!name.contains("%")&&name.indexOf('\ufffd')<0&&name.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectCompanion(Element a,URI file,String name){
        if(!"a".equals(a.tagName())||!a.hasClass("filelist_link")||!a.children().isEmpty()||!"javascript:void(0);".equals(a.attr("href")))return false;
        String function=switch(a.text().strip()){case "미리보기"->"previewEncodingUrlAjax";case "미리듣기"->"preListenEncodingUrlAjax";default->null;};if(function==null)return false;
        var clone=a.clone();clone.removeAttr("onclick");if(selectActive(clone))return false;
        String expected=function+"('"+file+"','"+name+"')",actual=a.attr("onclick").strip();return actual.equals(expected)||actual.equals(expected+";");
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=ChungnamProvinceNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()));}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        if(area.children().isEmpty()&&area.text().isBlank())return new Result("NO_FILES",true,List.of(),List.of());var lists=area.select(":root > ul.view-file-list");if(lists.size()!=1)return failed("ATTACHMENT_SELECTOR_CHANGED");
        var residual=area.clone();residual.select("ul.view-file-list").remove();boolean unresolved=!residual.text().isBlank()||!residual.select("a,img,[href]").isEmpty()||selectActive(residual),exceeded=false;
        var files=new LinkedHashMap<String,Descriptor>();String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("nttId");
        for(var li:lists.getFirst().children())try{
            if(!"li".equals(li.tagName()))throw new IllegalArgumentException();var links=li.select(":root > a.ico_file");if(links.size()!=1)throw new IllegalArgumentException();var a=links.getFirst();if(selectActive(a)||!a.children().isEmpty()||!"내려받기".equals(a.text().strip()))throw new IllegalArgumentException();
            URI file=detail.resolve(a.attr("href"));if(!DOWNLOAD.equals(file.getRawPath())||!selectApprovedRequest(file))throw new IllegalArgumentException();String name=li.ownText().strip();if(!selectSafeName(name))throw new IllegalArgumentException();var q=CapitalThirdNoticePage.selectParameters(file.getRawQuery());String id=normalizer.hash(q.get("fid")+"\n"+q.get("index")+"\n"+q.get("other")),ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);
            var d=new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported);
            if(files.containsKey(id)){if(!files.get(id).equals(d))unresolved=true;}else if(files.size()==10)exceeded=true;else files.put(id,d);
            for(var child:li.children())if(child!=a&&!selectCompanion(child,file,name))unresolved=true;
            if(li.attributes().asList().stream().anyMatch(attr->attr.getKey().toLowerCase(Locale.ROOT).startsWith("on")))unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
