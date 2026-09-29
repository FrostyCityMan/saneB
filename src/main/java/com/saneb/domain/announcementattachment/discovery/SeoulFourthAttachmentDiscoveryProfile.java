package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage.Site;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 세 기관의 고정 첨부 셀만 읽는다. 미리보기·듣기·iframe은 요청하지 않는다. */
final class SeoulFourthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String SONGPA_HOST="songpa.eminwon.seoul.kr",SONGPA_DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Pattern GOURL=Pattern.compile("^javascript:gourl\\('([^'\\\\\\r\\n]{1,8192})'\\);?$");
    private static final Pattern PREVIEW=Pattern.compile("^javascript:(previewAjax|preListen)\\('([^'\\\\\\r\\n]{1,4096})', ?'([^'\\\\\\r\\n]{1,500})'\\);?$");
    private final Site site;private final String code,hash,download;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final SaeolGetAttachmentDiscoveryProfile saeol;
    SeoulFourthAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_BOARD_V1";download=s==Site.SEONGDONG?"/main/downloadBbsFile.do":s==Site.SONGPA?SONGPA_DOWNLOAD:"/portal/cmmn/file/fileDown.do";
        saeol=new SaeolGetAttachmentDiscoveryProfile(code,s.sourceCode,SONGPA_HOST,s.parser,"td",false);
        hash=AttachmentProfileFingerprint.selectHash("SEOUL_FOURTH:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|"+s.host+"|https443|same-request|paired-preview-no-fetch|limit10|unknown-role|"+saeol.selectProfileHash()+"|"+AttachmentProfileFingerprint.selectHash("PAGE:1",SeoulFourthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return site==Site.SONGPA?Set.of(site.host,SONGPA_HOST):Set.of(site.host);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!site.parser.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return SeoulFourthNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean selectSafeUri(URI u){return u!=null&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.equals(u.normalize())&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath());}
    private boolean selectNumber(String s){return s!=null&&s.matches("[1-9][0-9]{0,14}");}
    @Override public boolean selectApprovedRequest(URI u){try{if(!selectSafeUri(u))return false;if(site.host.equals(u.getHost())&&site.path.equals(u.getPath()))return u.equals(SeoulFourthNoticePage.selectDetailUri(site,u));if(!download.equals(u.getPath()))return false;
        if(site==Site.SONGPA)return saeol.selectApprovedRequest(u);if(!site.host.equals(u.getHost()))return false;var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());
        if(site==Site.SEONGDONG)return q.keySet().equals(Set.of("key","bbsNo","nttNo","atchmnflNo"))&&site.menu.equals(q.get("key"))&&"184".equals(q.get("bbsNo"))&&selectNumber(q.get("nttNo"))&&selectNumber(q.get("atchmnflNo"));
        return q.keySet().equals(Set.of("menuNo","atchFileId","fileSn"))&&site.menu.equals(q.get("menuNo"))&&q.get("atchFileId").matches("[a-f0-9]{64}")&&selectNumber(q.get("fileSn"));
    }catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private URI selectEncoded(URI base,Map<String,String> q){var out=new StringJoiner("&");q.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->out.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));return URI.create(base+"?"+out);}
    private URI selectFileUri(String raw,URI detail){if(raw==null||raw.length()>8192)throw new IllegalArgumentException();
        if(site==Site.SONGPA){var m=GOURL.matcher(raw);if(!m.matches())throw new IllegalArgumentException();String prefix="http://"+SONGPA_HOST+download+"?",link=m.group(1);if(!link.startsWith(prefix)||link.contains("#"))throw new IllegalArgumentException();var u=selectEncoded(URI.create("https://"+SONGPA_HOST+download),CapitalThirdNoticePage.selectParameters(link.substring(prefix.length())));if(!selectApprovedRequest(u))throw new IllegalArgumentException();return u;}
        // 성동 서버가 삽입한 경로 사이 ASCII 공백만 정규화한다. query 값의 공백은 변경하지 않는다.
        if(site==Site.SEONGDONG){int mark=raw.indexOf('?');if(mark<0)throw new IllegalArgumentException();raw=raw.substring(0,mark).replaceAll("[ \\t\\r\\n]","")+raw.substring(mark);}
        if(!raw.startsWith(download+"?"))throw new IllegalArgumentException();URI u=detail.resolve(raw);if(!selectApprovedRequest(u))throw new IllegalArgumentException();if(site==Site.SEONGDONG&&!CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey).equals(CapitalThirdNoticePage.selectParameters(u.getRawQuery()).get("nttNo")))throw new IllegalArgumentException();return u;
    }
    private boolean selectSafeName(String n){return n!=null&&!n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private String selectName(Element a,URI fetch){if(site==Site.SONGPA){String n=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery()).get("user_file_nm");if(!n.equals(a.text().strip()))throw new IllegalArgumentException();return n;}if(site==Site.GWANGJIN){String n=a.attr("title");if(!selectSafeName(n)||a.select("span.orignlFileNm").size()!=1||a.select("span.fileExtsnNm").size()!=1||!n.toLowerCase(Locale.ROOT).endsWith("."+a.selectFirst("span.fileExtsnNm").text().strip().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException();return n;}var clone=a.clone();clone.select("span.p-icon").remove();String n=clone.text().strip();if(!selectSafeName(n))throw new IllegalArgumentException();return n;}
    private boolean selectPairedPreview(Element a,URI file,String name,URI detail){try{if(a.hasAttr("onclick"))return false;String raw=a.attr("href");if(raw.length()>8192)return false;var fileQ=CapitalThirdNoticePage.selectParameters(file.getRawQuery());
        if(site==Site.GWANGJIN){var m=PREVIEW.matcher(raw);return m.matches()&&file.equals(URI.create(m.group(2)))&&name.equals(m.group(3));}
        String prefix=site==Site.SEONGDONG?"/previewBbs.do?":"/gosiPreview.do?";if(!a.hasClass("p-attach__preview")||!raw.startsWith(prefix)||raw.contains("#"))return false;var q=CapitalThirdNoticePage.selectParameters(raw.substring(prefix.length()));
        if(site==Site.SEONGDONG){var expected=new HashMap<>(fileQ);expected.remove("key");return expected.equals(q);}
        if(!q.keySet().equals(Set.of("atchmnflNo","user_file_nm","sys_file_nm","file_path"))||!selectNumber(q.remove("atchmnflNo")))return false;return q.equals(fileQ);
    }catch(IllegalArgumentException e){return false;}}
    @Override public Result selectDescriptors(Source s,String html){URI detail=selectDetailUri(s);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");Element cell;
        try{cell=SeoulFourthNoticePage.selectAttachments(site,Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        // 광진 첨부 셀의 뷰어용 script/style은 실행하지 않고 제거한다.
        if(site==Site.GWANGJIN)cell.select("script,style,link[rel=stylesheet]").remove();
        String itemSelector=site==Site.SEONGDONG?":root > ul.p-attach > li.p-attach__item":site==Site.SONGPA?":root > ul.view_attach > li":":root > div.fileList > div";
        var residual=cell.clone();residual.select(itemSelector).remove();boolean unresolved=!residual.text().isBlank()||!residual.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty(),exceeded=false;var found=new LinkedHashMap<String,Descriptor>();
        for(var item:cell.select(itemSelector))try{String downloadSelector=site==Site.SEONGDONG?"a.p-attach__link.download":site==Site.SONGPA?"a[href^=javascript:gourl(]":"a[href^=/portal/cmmn/file/fileDown.do?]";var links=item.select(downloadSelector);if(links.size()!=1)throw new IllegalArgumentException();var a=links.getFirst();if(a.hasAttr("onclick"))throw new IllegalArgumentException();URI fetch=selectFileUri(a.attr("href"),detail);String name=selectName(a,fetch),ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);var q=CapitalThirdNoticePage.selectParameters(fetch.getRawQuery());boolean supported=Set.of("PDF","HWP","HWPX").contains(ext)&&(site!=Site.SONGPA||q.get("sys_file_nm").toLowerCase(Locale.ROOT).endsWith("."+ext.toLowerCase(Locale.ROOT)));
            String id=site==Site.SEONGDONG?q.get("atchmnflNo"):normalizer.hash(site==Site.SONGPA?q.get("file_path")+"\n"+q.get("sys_file_nm"):q.get("atchFileId")+"\n"+q.get("fileSn"));
            var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(code,download,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey))),name,supported?ext:null,"UNKNOWN",supported);
            if(found.containsKey(id)){if(!found.get(id).equals(d))unresolved=true;}else if(found.size()==10)exceeded=true;else found.put(id,d);
            var remainder=item.clone();remainder.select(downloadSelector).remove();for(var preview:remainder.select("a"))if(selectPairedPreview(preview,fetch,name,detail))preview.remove();
            if(!remainder.text().isBlank()||!remainder.select("a,button,input,img,iframe,form,object,embed,script,[onclick],[href]").isEmpty()||!a.select("script,input,button,img,iframe,object,embed,[onclick]").isEmpty())unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(found.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(found.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(found.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(found.values()),List.of());
    }
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
