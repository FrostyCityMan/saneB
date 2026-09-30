package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.GeojeNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 공식 함수의 세 인자만 해석하며 JavaScript·미리보기 iframe은 실행하지 않는다. */
@Component
public final class GeojeAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_GEOJE_GET_V1", FILE_HOST="eminwon.geoje.go.kr", DOWNLOAD="/emwp/jsp/lga/homepage/FileDown.jsp";
    private final SaeolGetAttachmentDiscoveryProfile validator=new SaeolGetAttachmentDiscoveryProfile(CODE,"LGS-000230",FILE_HOST,"SAEOL_GOSI","td",false);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash(CODE+":1|fixed-menu-idx|official-three-arguments|lga-homepage-get|paired-preview-no-fetch|same-request|unknown-role|limit10|"+validator.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",GeojeNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000230","SAEOL_GOSI"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(GeojeNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){
        try {
            if(s==null || !selectProviderCode().equals(s.providerCode()) || !"LGS-000230".equals(s.localSourceCode()) || !"SAEOL_GOSI".equals(s.listParserProfileCode())
                    || s.sourceUrl()==null || s.sourceUrl().length()>4096 || !normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId())) throw new IllegalArgumentException();
            return GeojeNoticePage.selectDetailUri(URI.create(s.sourceUrl()));
        } catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        try {
            if(uri==null)return false;
            if(GeojeNoticePage.HOST.equals(uri.getHost()))return uri.equals(GeojeNoticePage.selectDetailUri(uri));
            if(!"https".equals(uri.getScheme()) || !FILE_HOST.equals(uri.getHost()) || (uri.getPort()!=-1&&uri.getPort()!=443)
                    || uri.getUserInfo()!=null || uri.getFragment()!=null || !DOWNLOAD.equals(uri.getRawPath()) || !uri.equals(uri.normalize()))return false;
            // 공유 검증기의 파일명/경로 규칙만 재사용한다. 이 가상 URI로 HTTP 요청하지 않는다.
            return validator.selectApprovedRequest(URI.create("https://"+FILE_HOST+"/emwp/jsp/ofr/FileDown.jsp?"+uri.getRawQuery()));
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private boolean selectActive(Element e){return !e.select("button,input,select,form,iframe,object,embed,script,style").isEmpty() || e.getAllElements().stream().anyMatch(n->n.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));}
    private String selectEncoded(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20");}
    private String selectFormat(String name){String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private boolean selectPreview(Element a,URI detail,String expectedName){
        try {
            if(expectedName==null || selectActive(a) || !a.ownText().isBlank() || a.childrenSize()!=1)return false;
            URI uri=detail.resolve(a.attr("href"));var img=a.child(0);
            if(!"https".equals(uri.getScheme()) || !GeojeNoticePage.HOST.equals(uri.getHost()) || (uri.getPort()!=-1&&uri.getPort()!=443)
                    || uri.getUserInfo()!=null || uri.getFragment()!=null || !"/synap/skin/doc.html".equals(uri.getRawPath()))return false;
            var q=GeojeNoticePage.selectParameters(uri.getRawQuery());
            return q.keySet().equals(Set.of("fn","rs")) && expectedName.equals(q.get("fn")) && "/upload_data/Synap/gosi_/".equals(q.get("rs"))
                    && "img".equals(img.tagName()) && "/images/ic_2.gif".equals(img.attr("src")) && "바로보기".equals(img.attr("alt"));
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element area;
        try{area=GeojeNoticePage.selectAttachments(Jsoup.parse(html,detail.toASCIIString()));}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=GeojeNoticePage.selectParameters(detail.getRawQuery()).get("idx"),previewName=null;
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=!area.ownText().isBlank(),exceeded=false;int ordinal=0;
        for(var a:area.children()) {
            if("br".equals(a.tagName())&&!selectActive(a))continue;
            if(!"a".equals(a.tagName())){unresolved=true;previewName=null;continue;}
            if(selectPreview(a,detail,previewName)){previewName=null;continue;}
            previewName=null;
            try {
                var args=AttachmentDownloadInvocation.selectArguments(a.attr("href"),"goDownLoad",false,false);
                if(!a.hasClass("atta") || args.size()!=3 || selectActive(a) || !a.text().equals(args.get(0)))throw new IllegalArgumentException();
                URI fetch=URI.create("https://"+FILE_HOST+DOWNLOAD+"?user_file_nm="+selectEncoded(args.get(0))+"&sys_file_nm="+selectEncoded(args.get(1))+"&file_path="+selectEncoded(args.get(2)));
                if(!selectApprovedRequest(fetch))throw new IllegalArgumentException();
                String id=normalizer.hash(args.get(2)+"\n"+args.get(1)),format=selectFormat(args.get(0));boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));
                var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),args.get(0),supported?format:null,"UNKNOWN",supported);
                if(files.containsKey(id)){if(!files.get(id).equals(d))unresolved=true;}else if(files.size()==10)exceeded=true;else files.put(id,d);
                String suffix=args.get(0).substring(args.get(0).lastIndexOf('.')+1);
                previewName=notice+(ordinal++)+"."+suffix;
            }catch(IllegalArgumentException e){unresolved=true;}
        }
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
