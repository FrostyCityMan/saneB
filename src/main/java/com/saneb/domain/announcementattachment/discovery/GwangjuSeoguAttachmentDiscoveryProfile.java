package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChungjuEminwonNoticePage;
import com.saneb.domain.announcementsource.provider.content.GwangjuSeoguNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** 광주 서구의 공식 첨부 목록에서만 고정 새올 GET을 구성한다. 스크립트는 실행하지 않는다. */
@Component
public final class GwangjuSeoguAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_GWANGJU_SEOGU_GET_V1",FILE_HOST="eminwon.seogu.gwangju.kr",DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> FILE_FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final String HASH=AttachmentProfileFingerprint.selectHash(CODE+":1|LGS-000067|SPRING_BBS|board-view-file-list|fixed-get|unknown-role|limit10|strict-headers|"
            +AttachmentProfileFingerprint.selectHash("PAGE:1",GwangjuSeoguNoticePage.class)+AttachmentProfileFingerprint.selectHash("QUERY:1",ChungjuEminwonNoticePage.class),GwangjuSeoguAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return HASH;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000067","SPRING_BBS"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(GwangjuSeoguNoticePage.HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000067".equals(source.localSourceCode())||!"SPRING_BBS".equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());if(!GwangjuSeoguNoticePage.selectMatches(uri)||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        try{
            if(uri==null||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null
                    ||!uri.equals(uri.normalize())||!Objects.equals(uri.getRawPath(),uri.getPath())||uri.getRawQuery()==null||uri.getRawQuery().length()>8192)return false;
            var q=ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery());
            if(GwangjuSeoguNoticePage.selectMatches(uri))return q.keySet().equals(Set.of("mid","not_ancmt_mgt_no","method","methodnm"))&&"a10807010000".equals(q.get("mid"))
                    &&q.get("not_ancmt_mgt_no").matches("[1-9][0-9]{0,14}")&&"selectOfrNotAncmt".equals(q.get("method"))&&"selectOfrNotAncmtRegst".equals(q.get("methodnm"));
            return FILE_HOST.equals(uri.getHost())&&DOWNLOAD.equals(uri.getPath())&&q.keySet().equals(FILE_FIELDS)&&selectName(q.get("user_file_nm"))&&selectName(q.get("sys_file_nm"))
                    &&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        org.jsoup.nodes.Element list;
        try{list=GwangjuSeoguNoticePage.selectFiles(GwangjuSeoguNoticePage.selectRoot(Jsoup.parse(html,detail.toASCIIString())));}
        catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=!list.ownText().isBlank(),limited=false;
        for(var item:list.children()){
            try{
                if(!"li".equals(item.tagName())||!item.select("script,iframe,object,embed,button,input,form").isEmpty())throw new IllegalArgumentException();
                var links=item.select(":root > span.link > a.btn_line");if(links.size()!=1||item.select("a,[onclick]").size()!=1)throw new IllegalArgumentException();
                var anchor=links.getFirst();var args=AttachmentDownloadInvocation.selectArguments(anchor.attr("href"),"goDownLoad",false,false);
                if(args.size()!=3||anchor.hasAttr("onclick")||!selectName(args.get(0))||!selectName(args.get(1))||!item.ownText().strip().equals(args.get(0)))throw new IllegalArgumentException();
                var query=new StringJoiner("&");Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2)).entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
                URI file=URI.create("https://"+FILE_HOST+DOWNLOAD+"?"+query);if(!selectApprovedRequest(file))throw new IllegalArgumentException();
                String id=normalizer.hash(args.get(2)+"\n"+args.get(1));
                if(files.containsKey(id)){if(!files.get(id).fetchUri().equals(file))unresolved=true;continue;}
                if(files.size()==10){limited=true;continue;}String format=selectFormat(args.get(0));boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));
                var locator=new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",id,"noticeId",ChungjuEminwonNoticePage.selectParameters(detail.getRawQuery()).get("not_ancmt_mgt_no")));
                files.put(id,new Descriptor(file,locator,args.get(0),supported?format:null,"UNKNOWN",supported));
            }catch(IllegalArgumentException e){unresolved=true;}
        }
        if(limited)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private static boolean selectName(String s){return s!=null&&!s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private static String selectFormat(String s){String ext=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private static Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
