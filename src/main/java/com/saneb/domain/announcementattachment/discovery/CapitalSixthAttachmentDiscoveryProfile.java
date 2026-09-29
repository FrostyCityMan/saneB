package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 시흥 고정 3인자와 안산 공식 파일 식별자를 GET에 결합한다. 브라우저 스크립트는 실행하지 않는다. */
final class CapitalSixthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private final Site site;
    private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    CapitalSixthAttachmentDiscoveryProfile(Site site) {
        this.site=site;code="LOCAL_"+site+"_PORTAL_V1";
        hash=AttachmentProfileFingerprint.selectHash("CAPITAL_SIXTH:1|"+site+"|"+site.sourceCode+"|"+site.parser+"|"+site.host+"|"+site.fileHost+"|"+site.download
                +"|https443|GET|same-request|limit10|unknown-role|"+AttachmentProfileFingerprint.selectHash("PAGE:1",CapitalSixthNoticePage.class)
                +"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class)
                +"|"+AttachmentProfileFingerprint.selectHash("LITERAL:1",AttachmentDownloadInvocation.class),getClass());
    }
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(site.sourceCode,site.parser)); }
    @Override public Set<String> selectApprovedHosts() { return site.host.equals(site.fileHost)?Set.of(site.host):Set.of(site.host,site.fileHost); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!site.parser.equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return CapitalSixthNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));
        } catch(IllegalArgumentException e) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    private boolean selectSafeName(String name) {
        return name!=null&&!name.isBlank()&&name.length()<=500&&!name.contains("/")&&!name.contains("\\")&&!name.contains("..")
                &&!name.contains("%")&&name.indexOf('\ufffd')<0&&name.codePoints().noneMatch(Character::isISOControl);
    }
    private boolean selectFileValues(Map<String,String> values) {
        if(site==Site.ANSAN) return values.keySet().equals(Set.of("file_id"))&&values.get("file_id").matches("[A-Za-z0-9]{20,100}");
        return values.keySet().equals(FIELDS)&&selectSafeName(values.get("user_file_nm"))&&selectSafeName(values.get("sys_file_nm"))
                &&values.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if(uri==null||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null
                    ||!uri.equals(uri.normalize())||uri.getRawPath()==null||!uri.getRawPath().equals(uri.getPath())) return false;
            if(site.host.equals(uri.getHost())&&site.path.equals(uri.getPath())) return uri.equals(CapitalSixthNoticePage.selectDetailUri(site,uri));
            return site.fileHost.equals(uri.getHost())&&site.download.equals(uri.getPath())&&selectFileValues(CapitalThirdNoticePage.selectParameters(uri.getRawQuery()));
        } catch(IllegalArgumentException e) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) { return request!=null&&"GET".equals(request.method())&&request.form().isEmpty()&&selectApprovedRequest(request.uri()); }
    @Override public boolean selectApprovedRequest(Request initial,Request next) { return initial!=null&&initial.equals(next)&&selectApprovedRequest(next); }

    @Override public Result selectDescriptors(Source source,String html) {
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());Element cell;
        try { cell=CapitalSixthNoticePage.selectAttachments(site,page).clone(); }
        catch(IllegalArgumentException e) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey);
        if(site==Site.ANSAN) {
            var form=page.select("form#aform[method=get]");
            if(form.size()!=1||!selectHidden(form.getFirst(),"bbs_code","WWW13")||!selectHidden(form.getFirst(),"bbs_seq",notice)||!selectHidden(form.getFirst(),"file_id","")) return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        }
        cell.select("script,style").remove();String selector=site==Site.SIHEUNG?":root > div#updateFileList > ul":":root > ul.p-attach";
        var lists=cell.select(selector);if(lists.size()!=1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var list=lists.getFirst();var outside=cell.clone();outside.select(selector).remove();
        boolean unresolved=selectResidue(outside)||!list.ownText().isBlank(),exceeded=false;
        var files=new LinkedHashMap<String,Descriptor>();
        for(var item:list.children()) try {
            if(!"li".equals(item.tagName())) throw new IllegalArgumentException();
            String linkSelector=site==Site.ANSAN?"a.p-attach__link":"a[onclick^=goDownload(]";
            var links=item.select(linkSelector);if(links.size()!=1) throw new IllegalArgumentException();var link=links.getFirst();
            if(!"#".equals(link.attr("href"))) throw new IllegalArgumentException();
            Map<String,String> values;String name;
            if(site==Site.SIHEUNG) {
                String raw=link.attr("onclick");if(raw.length()>8192||!raw.startsWith("goDownload(")) throw new IllegalArgumentException();
                var args=AttachmentDownloadInvocation.selectArguments("goDownLoad"+raw.substring("goDownload".length()),"goDownLoad",true,true);
                if(args.size()!=3) throw new IllegalArgumentException();values=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));
                name=link.text().strip();if(!name.equals(args.get(0))) throw new IllegalArgumentException();
            } else {
                var match=Pattern.compile("fnFileDownLoad\\('([A-Za-z0-9]{20,100})'\\); return false;").matcher(link.attr("onclick"));
                if(!match.matches()) throw new IllegalArgumentException();values=Map.of("file_id",match.group(1));
                var label=link.clone();label.select("span.p-icon,i.p-icon").remove();name=label.text().strip();
            }
            if(!selectSafeName(name)||!selectFileValues(values)) throw new IllegalArgumentException();
            var query=new StringJoiner("&");values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
            URI file=URI.create("https://"+site.fileHost+site.download+"?"+query);if(!selectApprovedRequest(file)) throw new IllegalArgumentException();
            String identity=site==Site.ANSAN?values.get("file_id"):values.get("file_path")+"\n"+values.get("sys_file_nm");
            String id=normalizer.hash(identity);
            if(files.containsKey(id)) { if(!files.get(id).fetchUri().equals(file)||!files.get(id).displayName().equals(name)) unresolved=true; }
            else if(files.size()==10) exceeded=true;
            else {
                String format=selectFormat(name);boolean supported=format!=null&&(site==Site.ANSAN||format.equals(selectFormat(values.get("sys_file_nm"))));
                files.put(id,new Descriptor(file,new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",notice)),name,supported?format:null,"UNKNOWN",supported));
            }
            var residue=item.clone();residue.select(linkSelector).remove();
            if(site==Site.ANSAN) for(var preview:residue.select("a.p-attach__preview")) {
                if("#".equals(preview.attr("href"))&&("fnOpenPreview('"+values.get("file_id")+"'); return false;").equals(preview.attr("onclick"))&&"미리보기".equals(preview.text())) preview.remove();
            }
            if(selectResidue(residue)||link.select("script,input,button,iframe,object,embed,img,[onclick]").stream().anyMatch(e->e!=link)) unresolved=true;
        } catch(IllegalArgumentException e) { unresolved=true; }
        if(exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectHidden(Element form,String name,String value) { var fields=form.select("input[name="+name+"]");return fields.size()==1&&"hidden".equals(fields.getFirst().attr("type"))&&value.equals(fields.getFirst().val()); }
    private boolean selectResidue(Element element) { return !element.text().isBlank()||!element.select("a,button,input,img,iframe,form,object,embed,[onclick],[href]").isEmpty(); }
    private String selectFormat(String name) { String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null; }
    private Result selectFailed(String warning) { return new Result("FAILED",false,List.of(),List.of(warning)); }
}
