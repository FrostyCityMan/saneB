package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.AndongNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 안동 공식 첨부와 인접한 같은 파일 미리보기를 구분한다. 미리보기는 호출하지 않는다. */
@Component
public final class AndongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_ANDONG_TABLE_V1",FILE_HOST="eminwon.andong.go.kr",DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile validator=new SaeolGetAttachmentDiscoveryProfile(CODE,"LGS-000204",FILE_HOST,"SPRING_BBS","td",false);
    private final String hash=AttachmentProfileFingerprint.selectHash("ANDONG:1|table-official-area|three-string-get|paired-preview|partial-preserved|unknown-role|limit10|"
            +validator.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",AndongNoticePage.class)
            +"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("CALL:1",AttachmentDownloadInvocation.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000204","SPRING_BBS"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(AndongNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000204".equals(source.localSourceCode())||!"SPRING_BBS".equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            return AndongNoticePage.selectDetailUri(URI.create(source.sourceUrl()));
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        try{if(uri==null)return false;if(AndongNoticePage.selectMatches(uri))return uri.equals(AndongNoticePage.selectDetailUri(uri));
            return FILE_HOST.equals(uri.getHost())&&DOWNLOAD.equals(uri.getRawPath())&&validator.selectApprovedRequest(uri);
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request r){return r!=null&&"GET".equals(r.method())&&r.form().isEmpty()&&selectApprovedRequest(r.uri());}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_048_576)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=AndongNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo");
        var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean unresolved=false,exceeded=false;
        for(var a:area.select("a"))try{
            if(a.hasAttr("onclick"))continue;var args=selectArguments(a);if(args.size()!=3||!a.text().strip().equals(args.getFirst()))continue;
            URI fetch=URI.create("https://"+FILE_HOST+DOWNLOAD+"?user_file_nm="+selectEncoded(args.get(0))+"&sys_file_nm="+selectEncoded(args.get(1))+"&file_path="+selectEncoded(args.get(2)));
            if(!selectApprovedRequest(fetch))continue;
            String id=normalizer.hash(args.get(2)+"\n"+args.get(1)),format=selectFormat(args.get(0));boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));
            var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),args.get(0),supported?format:null,"UNKNOWN",supported);
            var old=files.get(id);if(old!=null){if(!old.equals(d))unresolved=true;}
            else if(files.size()==10)exceeded=true;else files.put(id,d);
            recognized.add(a);
        }catch(IllegalArgumentException e){unresolved=true;}
        for(var a:area.select("a"))if(!recognized.contains(a)&&selectPairedPreview(a,notice,recognized))recognized.add(a);
        for(var a:recognized)a.remove();
        if(!area.text().isBlank()||!area.select("a,button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!area.select("li").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private List<String> selectArguments(Element a){String href=a.attr("href").strip();String prefix="javascript:goDownload";if(!href.startsWith(prefix+"(")||!a.select("script,input,img,button,iframe").isEmpty())return List.of();return AttachmentDownloadInvocation.selectArguments("javascript:goDownLoad"+href.substring(prefix.length()),"goDownLoad",false,false);}
    private boolean selectPairedPreview(Element a,String notice,Set<Element> recognized){
        if(!"#".equals(a.attr("href"))||!a.hasClass("btn_fileview")||!a.text().isBlank()||a.childrenSize()!=1
                ||a.select(":root > img[src='/mayor/images/board/btn_fileview.png'][alt=바로보기]").size()!=1)return false;
        var previous=a.previousElementSibling();if(previous==null||!recognized.contains(previous))return false;
        var match=Pattern.compile("^fn_egov_gosi_preview\\('"+notice+"','"+notice+"-[0-9]{1,3}\\.(?:pdf|hwp|hwpx)',").matcher(a.attr("onclick"));if(!match.find())return false;
        var args=AttachmentDownloadInvocation.selectArguments("goDownLoad("+a.attr("onclick").substring(match.end()),"goDownLoad",true,true);
        return args.size()==3&&args.equals(selectArguments(previous));
    }
    private String selectEncoded(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20");}
    private String selectFormat(String name){String extension=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(extension)?extension:null;}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
