package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 첨부 영역의 HTTPS GET만 허용한다. 미리보기 스크립트는 정적 비교만 한다. */
final class CapitalSeventhAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern PREVIEW=Pattern.compile("fn_goPreView\\('([^'\\\\\\r\\n]{1,6000})',\\s*'([^'\\\\\\r\\n]{1,1800})'\\);");
    private final Site site;
    private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    CapitalSeventhAttachmentDiscoveryProfile(Site site) {
        this.site=site;code="LOCAL_"+site+"_PORTAL_V1";
        hash=AttachmentProfileFingerprint.selectHash("CAPITAL_SEVENTH:1|"+site+"|"+site.sourceCode+"|SAEOL_GOSI|"+site.host+"|"+site.fileHost+"|"+site.download
                +"|https443|GET|same-request|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",CapitalSeventhNoticePage.class)
                +"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    }
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(site.sourceCode,"SAEOL_GOSI")); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(site.host,site.fileHost); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!"SAEOL_GOSI".equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return CapitalSeventhNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));
        } catch(IllegalArgumentException e) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    private boolean selectSafeName(String name) {
        return name!=null&&!name.isBlank()&&name.length()<=500&&!name.contains("/")&&!name.contains("\\")&&!name.contains("..")
                &&!name.contains("%")&&name.indexOf('\ufffd')<0&&name.codePoints().noneMatch(Character::isISOControl);
    }
    private boolean selectFileValues(Map<String,String> values) {
        return values.keySet().equals(FIELDS)&&selectSafeName(values.get("user_file_nm"))&&selectSafeName(values.get("sys_file_nm"))
                &&values.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if(uri==null||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null
                    ||!uri.equals(uri.normalize())||uri.getRawPath()==null||!uri.getRawPath().equals(uri.getPath())) return false;
            if(site.host.equals(uri.getHost())&&site.path.equals(uri.getPath())) return uri.equals(CapitalSeventhNoticePage.selectDetailUri(site,uri));
            return site.fileHost.equals(uri.getHost())&&site.download.equals(uri.getPath())&&selectFileValues(CapitalThirdNoticePage.selectParameters(uri.getRawQuery()));
        } catch(IllegalArgumentException e) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) { return request!=null&&"GET".equals(request.method())&&request.form().isEmpty()&&selectApprovedRequest(request.uri()); }
    @Override public boolean selectApprovedRequest(Request initial,Request next) { return initial!=null&&initial.equals(next)&&selectApprovedRequest(next); }
    private Map<String,String> selectFileLink(String raw) {
        if(raw==null||raw.length()>8192) throw new IllegalArgumentException();
        var uri=URI.create(raw.replace(" ","%20"));
        if(!site.fileHost.equals(uri.getHost())||!site.download.equals(uri.getPath())||!selectApprovedRequest(uri)) throw new IllegalArgumentException();
        return CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
    }
    private boolean selectPreview(Element preview,Map<String,String> values,String name) {
        if(preview.hasAttr("href")||preview.attr("onclick").length()>8192||!preview.text().endsWith("파일 미리보기")
                ||!preview.select("script,input,button,iframe,object,embed,img").isEmpty()) return false;
        var match=PREVIEW.matcher(preview.attr("onclick")); if(!match.matches()) return false;
        try {
            // 관측된 구형 HTTP 미리보기 인자는 HTTPS 다운로드와 동일한 파일인지 비교만 한다. 요청하지 않는다.
            String raw=match.group(1);if(raw.startsWith("http://")) raw="https://"+raw.substring(7);
            return values.equals(selectFileLink(raw))&&name.equals(URLDecoder.decode(match.group(2),StandardCharsets.UTF_8));
        } catch(IllegalArgumentException e) { return false; }
    }
    @Override public Result selectDescriptors(Source source,String html) {
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try { cell=CapitalSeventhNoticePage.selectAttachments(site,page).clone(); }
        catch(IllegalArgumentException e) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey);
        cell.select("script,style").remove();String selector=site==Site.POCHEON?":root > div.attach_list":":root > ul.view_attach";
        var lists=cell.select(selector);if(lists.size()!=1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var list=lists.getFirst();var outside=cell.clone();outside.select(selector).remove();
        if(site==Site.POCHEON) outside.select(":root > span.attach_tit").remove();
        boolean unresolved=selectResidue(outside)||!list.ownText().isBlank(),exceeded=false;
        var files=new LinkedHashMap<String,Descriptor>();
        for(var item:list.children()) try {
            if(site==Site.POCHEON?!("div".equals(item.tagName())&&item.hasClass("attach_item")):!"li".equals(item.tagName())) throw new IllegalArgumentException();
            String linkSelector=site==Site.POCHEON?":root > a.attach_btn.down":":root > div.down_view > a.file_down";
            String nameSelector=site==Site.POCHEON?":root > span.text":":root > div.down_view > span";
            var links=item.select(linkSelector);var labels=item.select(nameSelector);if(links.size()!=1||labels.size()!=1) throw new IllegalArgumentException();
            var link=links.getFirst();if(link.hasAttr("onclick")) throw new IllegalArgumentException();
            var label=labels.getFirst().clone();label.select("img").remove();String name=label.text().strip();
            var values=selectFileLink(link.attr("href"));if(!name.equals(values.get("user_file_nm"))) throw new IllegalArgumentException();
            var query=new StringJoiner("&");values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
            URI file=URI.create("https://"+site.fileHost+site.download+"?"+query);
            String id=normalizer.hash(values.get("file_path")+"\n"+values.get("sys_file_nm"));
            if(files.containsKey(id)) { if(!files.get(id).fetchUri().equals(file)||!files.get(id).displayName().equals(name)) unresolved=true; }
            else if(files.size()==10) exceeded=true;
            else {
                String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(values.get("sys_file_nm")));
                files.put(id,new Descriptor(file,new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",notice)),name,supported?format:null,"UNKNOWN",supported));
            }
            var residue=item.clone();residue.select(linkSelector).remove();residue.select(nameSelector).remove();
            if(site==Site.GANGNEUNG) for(var preview:residue.select(":root > div.down_view > a.file_view")) if(selectPreview(preview,values,name)) preview.remove();
            if(selectResidue(residue)||!label.select("a,button,input,iframe,object,embed,[onclick],[href]").isEmpty()
                    ||!link.select("script,input,button,iframe,object,embed,img,[onclick]").isEmpty()) unresolved=true;
        } catch(IllegalArgumentException e) { unresolved=true; }
        if(exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectResidue(Element element) { return !element.text().isBlank()||!element.select("a,button,input,img,iframe,form,object,embed,[onclick],[href]").isEmpty(); }
    private String selectFormat(String name) { String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null; }
    private Result selectFailed(String warning) { return new Result("FAILED",false,List.of(),List.of(warning)); }
}
