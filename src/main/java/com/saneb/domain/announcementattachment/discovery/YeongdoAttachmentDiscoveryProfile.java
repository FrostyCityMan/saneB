package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** 영도 SCMS 공식 상세의 attach1 전체와 명시된 새올 GET 링크만 처리한다. */
@Component
public final class YeongdoAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    public static final String CODE="LOCAL_YEONGDO_SCMS_GET_V1";
    private static final String HOST="www.yeongdo.go.kr", DOWNLOAD_HOST="eminwon.yeongdo.go.kr";
    private static final String DETAIL="/00000/00007/00013.web", DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("YEONGDO_SCMS:1|LGS-000031|SCMS_CARD_NOTICE|"+HOST+DETAIL+"|"+DOWNLOAD_HOST+DOWNLOAD+"|exact-query|whole-attach1|unknown-role|limit10|same-request-redirect",getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000031","SCMS_CARD_NOTICE"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(HOST,DOWNLOAD_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000031".equals(source.localSourceCode())
                    ||!"SCMS_CARD_NOTICE".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                    ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());
            if(!HOST.equals(uri.getHost())||!DETAIL.equals(uri.getPath())||!selectApprovedRequest(uri))throw new IllegalArgumentException();
            return uri;
        }catch(IllegalArgumentException exception){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        if(uri==null||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null
                ||uri.getFragment()!=null||uri.getRawPath()==null||!uri.equals(uri.normalize())||!uri.getRawPath().equals(uri.getPath()))return false;
        var values=selectParameters(uri.getRawQuery());
        if(HOST.equals(uri.getHost())&&DETAIL.equals(uri.getPath()))return values.keySet().equals(Set.of("amode","not_ancmt_mgt_no","type"))
                &&"view".equals(values.get("amode"))&&"A".equals(values.get("type"))&&values.get("not_ancmt_mgt_no").matches("[0-9]{1,15}");
        return DOWNLOAD_HOST.equals(uri.getHost())&&DOWNLOAD.equals(uri.getPath())&&values.keySet().equals(Set.of("user_file_nm","sys_file_nm","file_path"))
                &&selectSafeName(values.get("user_file_nm"))&&selectSafeName(values.get("sys_file_nm"))
                &&values.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }
    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request next){
        return selectApprovedRequest(initial)&&selectApprovedRequest(next)&&initial.uri().equals(next.uri());
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);
        if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());
        var forms=page.select("form#saeolGosiVO[name=saeolGosiVO][method=get]");
        var areas=page.select("div.attach1");
        if(forms.size()!=1||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var form=forms.getFirst();var views=form.select(":root > div.bbs1view1");var area=areas.getFirst();
        if(views.size()!=1||area.parent()!=views.getFirst()||selectHasEvent(form)||selectHasEvent(views.getFirst())
                ||area.childrenSize()!=1||!"ul".equals(area.child(0).tagName())||!area.ownText().isBlank())return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var files=new LinkedHashMap<String,Descriptor>();boolean exceeded=false;
        boolean unresolved=area.getAllElements().stream().anyMatch(this::selectHasEvent)
                ||!area.select("button,input,select,form,iframe,object,embed,script,img,[src],[style]").isEmpty();
        if(!area.child(0).ownText().isBlank()||area.child(0).children().stream().anyMatch(e->!"li".equals(e.tagName())
                ||e.childrenSize()!=1||!"a".equals(e.child(0).tagName())||!e.ownText().isBlank()))unresolved=true;
        var residual=area.clone();residual.select("a").remove();
        if(!residual.text().isBlank()||!residual.select("[href]").isEmpty())unresolved=true;
        var anchors=area.select("a");
        if(anchors.isEmpty()&&!area.select("li").isEmpty())unresolved=true;
        for(var anchor:anchors){
            try{
                if(!anchor.hasClass("filename")||!anchor.children().isEmpty()||!"li".equals(anchor.parent().tagName())
                        ||anchor.parent().parent()!=area.child(0)||anchor.parent().childrenSize()!=1||!anchor.parent().ownText().isBlank()
                        ||selectHasEvent(anchor))throw new IllegalArgumentException();
                String href=anchor.attr("href");
                if(href.isBlank()||href.length()>8192||href.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException();
                URI raw=detail.resolve(URI.create(href.replace(" ","%20")));
                if(!DOWNLOAD_HOST.equals(raw.getHost())||!DOWNLOAD.equals(raw.getPath())||!selectApprovedRequest(raw))throw new IllegalArgumentException();
                var values=selectParameters(raw.getRawQuery());String name=values.get("user_file_nm");
                if(!name.equals(anchor.text().strip()))throw new IllegalArgumentException();
                var query=new StringJoiner("&");values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
                URI fetch=URI.create("https://"+DOWNLOAD_HOST+DOWNLOAD+"?"+query);
                String identity=normalizer.hash(values.get("file_path")+"\n"+values.get("sys_file_nm"));
                var previous=files.get(identity);
                if(previous!=null){if(!previous.fetchUri().equals(fetch)||!previous.displayName().equals(name))unresolved=true;continue;}
                if(files.size()==10){exceeded=true;continue;}
                String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(values.get("sys_file_nm")));
                var locator=new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",identity,"noticeId",selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no")));
                files.put(identity,new Descriptor(fetch,locator,name,supported?format:null,"UNKNOWN",supported));
            }catch(IllegalArgumentException exception){unresolved=true;}
        }
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private Map<String,String> selectParameters(String query){
        if(query==null||query.length()>8192)return Map.of();var result=new LinkedHashMap<String,String>();
        try{for(String pair:query.split("&",-1)){String[] parts=pair.split("=",-1);
            if(parts.length!=2||!parts[0].matches("[A-Za-z_]+")||result.putIfAbsent(parts[0],URLDecoder.decode(parts[1],StandardCharsets.UTF_8))!=null)return Map.of();
        }return result;}catch(IllegalArgumentException exception){return Map.of();}
    }
    private boolean selectHasEvent(Element element){return element.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on"));}
    private boolean selectSafeName(String name){return name!=null&&!name.isBlank()&&name.length()<=500&&!name.contains("/")&&!name.contains("\\")&&!name.contains("..")&&!name.contains("%")&&name.indexOf('\ufffd')<0&&name.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String name){String suffix=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(suffix)?suffix:null;}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
