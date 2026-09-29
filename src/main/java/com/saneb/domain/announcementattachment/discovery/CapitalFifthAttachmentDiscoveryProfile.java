package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 고정 리터럴만 해석하며 실행 코드·미리보기는 요청하지 않는다. 정상 파일과 발견 오류를 분리한다. */
final class CapitalFifthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern LITERAL=Pattern.compile("\\s*'((?:[^'\\\\]|\\\\['\\\\])*)'\\s*(,|\\))");
    private final Site site;
    private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();

    CapitalFifthAttachmentDiscoveryProfile(Site site) {
        this.site=site; code="LOCAL_"+site+"_PORTAL_V1";
        hash=AttachmentProfileFingerprint.selectHash("CAPITAL_FIFTH:1|"+site+"|"+site.sourceCode+"|"+site.parser+"|"+site.host+"|"+site.fileHost
                +"|https443|same-request|plain3|paired-preview|unknown-role|limit10|"
                +AttachmentProfileFingerprint.selectHash("PAGE:1",CapitalFifthNoticePage.class)+"|"
                +AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    }
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(site.sourceCode,site.parser)); }
    @Override public Set<String> selectApprovedHosts() { return Set.of(site.host,site.fileHost); }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())
                    ||!site.parser.equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                    ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            return CapitalFifthNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));
        } catch(IllegalArgumentException e) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    private boolean selectSafeUri(URI uri) {
        return uri!=null&&"https".equals(uri.getScheme())&&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null
                &&uri.getFragment()==null&&uri.equals(uri.normalize())&&uri.getRawPath()!=null&&uri.getRawPath().equals(uri.getPath());
    }
    private boolean selectSafeName(String name) {
        return name!=null&&!name.isBlank()&&name.length()<=500&&!name.contains("/")&&!name.contains("\\")&&!name.contains("..")
                &&!name.contains("%")&&name.indexOf('\ufffd')<0&&name.codePoints().noneMatch(Character::isISOControl);
    }
    private boolean selectSafeValues(Map<String,String> values) {
        return values.keySet().equals(FIELDS)&&selectSafeName(values.get("user_file_nm"))&&selectSafeName(values.get("sys_file_nm"))
                &&values.getOrDefault("file_path","").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        try {
            if(!selectSafeUri(uri)) return false;
            if(site.host.equals(uri.getHost())) return uri.equals(CapitalFifthNoticePage.selectDetailUri(site,uri));
            return site!=Site.UIJEONGBU&&site.fileHost.equals(uri.getHost())&&site.download.equals(uri.getPath())
                    &&selectSafeValues(CapitalThirdNoticePage.selectParameters(uri.getRawQuery()));
        } catch(IllegalArgumentException e) { return false; }
    }
    @Override public boolean selectApprovedRequest(Request request) {
        if(request==null) return false;
        if("GET".equals(request.method())) return request.form().isEmpty()&&selectApprovedRequest(request.uri());
        return site==Site.UIJEONGBU&&"POST".equals(request.method())&&selectSafeUri(request.uri())
                &&site.fileHost.equals(request.uri().getHost())&&site.download.equals(request.uri().getPath())
                &&request.uri().getRawQuery()==null&&selectSafeValues(request.form());
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next) { return initial!=null&&initial.equals(next)&&selectApprovedRequest(next); }

    private List<String> selectArguments(String raw,String function,int count,boolean href) {
        String prefix=(href?"javascript:":"")+function+"(";
        if(raw==null||raw.length()>8192||!raw.startsWith(prefix)) return List.of();
        var result=new ArrayList<String>(); int cursor=prefix.length();
        for(int i=0;i<count;i++) {
            var match=LITERAL.matcher(raw).region(cursor,raw.length());
            if(!match.lookingAt()||!match.group(2).equals(i==count-1?")":",")) return List.of();
            result.add(match.group(1).replace("\\'","'").replace("\\\\","\\")); cursor=match.end();
        }
        String tail=raw.substring(cursor).strip();
        return (href?Set.of("",";"):Set.of("; return false;","return false;")).contains(tail)?result:List.of();
    }

    @Override public Result selectDescriptors(Source source,String html) {
        URI detail=selectDetailUri(source);
        if(html==null||html.length()>1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString()); Element cell;
        try { cell=CapitalFifthNoticePage.selectAttachments(site,page).clone(); }
        catch(IllegalArgumentException e) { return selectFailed("ATTACHMENT_SELECTOR_CHANGED"); }
        if(site==Site.UIJEONGBU) {
            var forms=page.select("form#gosiFiledownFrm[name=gosiFiledownFrm][method=post]");
            if(forms.size()!=1) return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            var form=forms.getFirst();
            if(!("https://"+site.fileHost+site.download).equals(form.attr("action"))||form.childrenSize()!=3
                    ||form.select("input[type=hidden]").size()!=3||form.children().stream().anyMatch(e->!e.val().isEmpty())
                    ||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FIELDS)) return selectFailed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            // 공식 빈 다운로드 프레임만 제외한다. URL이 붙은 iframe은 미확인 요소로 남는다.
            cell.select("iframe#hidden_frame[name=hidden_frame]").stream().filter(e->!e.hasAttr("src")&&!e.hasAttr("srcdoc")&&e.childrenSize()==0&&e.text().isBlank()).forEach(Element::remove);
        }
        cell.select("script,style").remove();
        String itemSelector=site==Site.ANSEONG?":root > p":site==Site.UIJEONGBU?":root > ul > li":":root > div > ul#updateFileList > li";
        var items=cell.select(itemSelector); var outside=cell.clone(); outside.select(itemSelector).remove();
        boolean unresolved=selectResidue(outside),exceeded=false;
        var files=new LinkedHashMap<String,Descriptor>();
        for(var item:items) try {
            var links=item.select(site==Site.GG_GWANGJU?"a[onclick^=goDownload(]":"a[href^=javascript:goDownload(]");
            if(links.size()!=1) throw new IllegalArgumentException();
            var link=links.getFirst(); boolean href=site!=Site.GG_GWANGJU;
            if(href?link.hasAttr("onclick"):!"#".equals(link.attr("href"))) throw new IllegalArgumentException();
            var args=selectArguments(link.attr(href?"href":"onclick"),"goDownload",3,href);
            if(args.size()!=3) throw new IllegalArgumentException();
            var values=Map.of("user_file_nm",args.get(0),"sys_file_nm",args.get(1),"file_path",args.get(2));
            if(!selectSafeValues(values)||!args.get(0).equals(link.text().strip())) throw new IllegalArgumentException();
            var request=selectFileRequest(values); if(!selectApprovedRequest(request)) throw new IllegalArgumentException();
            String id=normalizer.hash(args.get(2)+"\n"+args.get(1));
            if(files.containsKey(id)) {
                if(!files.get(id).selectRequest().equals(request)||!files.get(id).displayName().equals(args.get(0))) unresolved=true;
            } else if(files.size()==10) exceeded=true;
            else {
                String format=selectFormat(args.get(0)); boolean supported=format!=null&&format.equals(selectFormat(args.get(1)));
                files.put(id,new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo"))),args.get(0),supported?format:null,"UNKNOWN",supported,request.form()));
            }
            var residue=item.clone(); residue.select(href?"a[href^=javascript:goDownload(]":"a[onclick^=goDownload(]").remove();
            String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get("notAncmtMgtNo");
            for(var preview:residue.select("a")) if(selectPairedPreview(preview,args,notice)) preview.remove();
            if(selectResidue(residue)||link.select("script,input,button,iframe,object,embed,img,[onclick]").stream().anyMatch(e->e!=link)) unresolved=true;
        } catch(IllegalArgumentException e) { unresolved=true; }
        if(exceeded) return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved) return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }

    private boolean selectPairedPreview(Element preview,List<String> args,String notice) {
        if(!"#".equals(preview.attr("href"))) return false;
        var values=selectArguments(preview.attr("onclick"),site==Site.GG_GWANGJU?"previewAjax":"fn_egov_gosi_preview",site==Site.GG_GWANGJU?4:site==Site.UIJEONGBU?6:5,false);
        if(site==Site.GG_GWANGJU) return values.size()==4&&values.subList(0,3).equals(args)&&Set.of("N","Y").contains(values.get(3))&&Set.of("바로보기","바로듣기").contains(preview.text());
        if(values.size()!=(site==Site.UIJEONGBU?6:5)||!notice.equals(values.get(0))||!values.subList(2,5).equals(args)) return false;
        String ext=args.get(0).substring(args.get(0).lastIndexOf('.')+1);
        return values.get(1).matches(Pattern.quote(notice)+"-[0-9]{1,3}\\."+Pattern.quote(ext))
                &&(site!=Site.UIJEONGBU||Set.of("N","Y").contains(values.get(5)))&&Set.of("바로 보기","바로 듣기").contains(preview.text());
    }
    private boolean selectResidue(Element element) { return !element.text().isBlank()||!element.select("a,button,input,img,iframe,form,object,embed,[onclick],[href]").isEmpty(); }
    private Request selectFileRequest(Map<String,String> values) {
        URI base=URI.create("https://"+site.fileHost+site.download);
        if(site==Site.UIJEONGBU) {
            var form=new HashMap<>(values); form.put("user_file_nm",values.get("user_file_nm").replace(" ",""));
            return new Request(base,"POST",form);
        }
        var query=new StringJoiner("&"); values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
        return Request.selectGet(URI.create(base+"?"+query));
    }
    private String selectFormat(String name) { String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):""; return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null; }
    private Result selectFailed(String warning) { return new Result("FAILED",false,List.of(),List.of(warning)); }
}
