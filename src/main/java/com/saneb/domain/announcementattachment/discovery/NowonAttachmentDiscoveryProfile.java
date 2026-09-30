package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.NowonNoticePage;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 공식 첨부 셀의 개별 파일만 수집한다. ZIP 일괄 다운로드·본문 이미지·미리보기는 실행하지 않는다. */
@Component
public final class NowonAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    public static final String CODE="LOCAL_NOWON_BOARD_V1";
    private static final String FILE="/component/file/ND_fileDownload.do";
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("NOWON:1|LGS-000012|NOWON_NOTICE_TABLE|board1003|https443|same-request|individual-file|limit10|unknown-role|partial-preserved|"
            +AttachmentProfileFingerprint.selectHash("PAGE:1",NowonNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000012","NOWON_NOTICE_TABLE"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(NowonNoticePage.HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000012".equals(source.localSourceCode())||!"NOWON_NOTICE_TABLE".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();return NowonNoticePage.selectDetailUri(URI.create(source.sourceUrl()));}
        catch(IllegalArgumentException exception){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        try{
            if(uri==null||!"https".equals(uri.getScheme())||!NowonNoticePage.HOST.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getRawPath()==null||!uri.getRawPath().equals(uri.getPath())||!uri.equals(uri.normalize()))return false;
            if(NowonNoticePage.PATH.equals(uri.getPath()))return uri.equals(NowonNoticePage.selectDetailUri(uri));
            if(!FILE.equals(uri.getPath()))return false;var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return q.keySet().equals(Set.of("q_fileSn","q_fileId"))&&q.get("q_fileSn").matches("[1-9][0-9]{0,14}")&&q.get("q_fileId").matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
        }catch(IllegalArgumentException exception){return false;}
    }
    @Override public boolean selectApprovedRequest(Request request){return request!=null&&"GET".equals(request.method())&&request.form().isEmpty()&&selectApprovedRequest(request.uri());}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1048576)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=NowonNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException exception){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("q_bbscttSn");
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        for(var item:area.select(":root > ul.file-list > li"))try{
            var links=item.select(":root > a[href]");if(links.size()!=1)continue;var link=links.getFirst();URI file=detail.resolve(link.attr("href"));
            if(!FILE.equals(file.getPath())||!selectApprovedRequest(file)||link.hasAttr("onclick"))continue;
            var labels=link.select(":root > span");if(labels.size()!=1)continue;String name=labels.getFirst().text().strip();
            if(name.isBlank()||name.length()>500||name.contains("/")||name.contains("\\")||name.contains("..")||name.indexOf('\ufffd')>=0||name.codePoints().anyMatch(Character::isISOControl))continue;
            var q=CapitalThirdNoticePage.selectParameters(file.getRawQuery());String id=q.get("q_fileSn")+":"+q.get("q_fileId");String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);
            var descriptor=new Descriptor(file,new AttachmentSetEvidence.Locator(CODE,FILE,Map.of("noticeId",notice,"attachmentId",id)),name,supported?ext:null,"UNKNOWN",supported);
            var old=files.get(id);if(old!=null){if(!old.equals(descriptor))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(id,descriptor);
            if(!link.select("script,img,button,input,iframe,object,embed,[onclick]").isEmpty())unresolved=true;
            var residual=item.clone();residual.select(":root > a").remove();
            for(var size:residual.select(":root > span.file-size"))if(size.children().isEmpty()&&size.text().matches("[0-9.,]+\\s*(?:KB|MB|B)"))size.remove();
            for(var preview:residual.select(":root > button.btn-preveal")){
                String expected="opPreviewFile('"+q.get("q_fileId")+"','"+link.attr("href")+"');";
                if("button".equals(preview.attr("type"))&&"미리보기".equals(preview.text().strip())&&preview.children().isEmpty()&&expected.equals(preview.attr("onclick")))preview.remove();
            }
            if(hasResidual(residual))unresolved=true;item.remove();
        }catch(IllegalArgumentException exception){unresolved=true;}
        // 실측한 스크립트는 기존 미리보기 버튼의 aria-label만 바꾼다. 실행하지 않고 동일한 코드만 보조 UI로 제외한다.
        for(var script:area.select(":root > ul.file-list > script:not([src])"))
            if("b75733ffabc6ebb5310452b3ac3023ebbca8b76601d9fe5929247b89649f5dea".equals(normalizer.hash(script.data().replaceAll("\\s+",""))))script.remove();
        if(hasResidual(area))unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean hasResidual(Element element){return !element.text().isBlank()||!element.select("a,button,input,select,form,iframe,object,embed,script,img,[onclick],[href]").isEmpty();}
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
