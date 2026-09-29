package com.saneb.domain.announcementattachment.discovery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.CapitalEighthNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalEighthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
/** 고정 JSON 리터럴만 읽는다. 페이지 스크립트·미리보기·외부 함수는 실행하지 않는다. */
final class CapitalEighthAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String PAJU_DOWNLOAD="/component/file/ND_fileDownload.do", PAJU_PREVIEW="/component/file/ND_fileViewer.do", GM_DOWNLOAD="/emwp/jsp/ofr/FileDownNew.jsp";
    private static final Set<String> FIELDS=Set.of("user_file_nm","sys_file_nm","file_path");
    private static final Pattern PUSH=Pattern.compile("fileList\\.push\\(\\s*(\\{[^{}]{1,6000}\\})\\s*\\);"), PUSH_START=Pattern.compile("fileList\\.push\\s*\\(");
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final ObjectMapper mapper=new ObjectMapper().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    CapitalEighthAttachmentDiscoveryProfile(Site s){site=s;code="LOCAL_"+s+"_BOARD_V1";hash=AttachmentProfileFingerprint.selectHash("CAPITAL_EIGHTH:1|"+s+"|"+s.sourceCode+"|"+s.parser+"|fixed-host|https443|literal-manifest|same-request|unknown-role|limit10|"+AttachmentProfileFingerprint.selectHash("PAGE:1",CapitalEighthNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());}
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return site.host.equals(site.fileHost)?Set.of(site.host):Set.of(site.host,site.fileHost);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!site.sourceCode.equals(s.localSourceCode())||!site.parser.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return CapitalEighthNoticePage.selectDetailUri(site,URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    private boolean safeUri(URI u){return u!=null&&u.toASCIIString().length()<=8192&&"https".equals(u.getScheme())&&(u.getPort()==-1||u.getPort()==443)&&u.getUserInfo()==null&&u.getFragment()==null&&u.getRawPath()!=null&&u.getRawPath().equals(u.getPath())&&u.equals(u.normalize());}
    private boolean safeName(String n){return n!=null&&!n.isBlank()&&n.length()<=500&&!n.contains("/")&&!n.contains("\\")&&!n.contains("..")&&!n.contains("%")&&n.indexOf('\ufffd')<0&&n.codePoints().noneMatch(Character::isISOControl);}
    private boolean fileValues(Map<String,String> f){return f.keySet().equals(FIELDS)&&safeName(f.get("user_file_nm"))&&safeName(f.get("sys_file_nm"))&&f.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");}
    private String selectPajuId(URI u,String path){if(!safeUri(u)||!site.host.equals(u.getHost())||!path.equals(u.getPath()))throw new IllegalArgumentException();var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!q.keySet().equals(Set.of("id"))||!q.get("id").matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))throw new IllegalArgumentException();return q.get("id");}
    @Override public boolean selectApprovedRequest(URI u){try{if(!safeUri(u))return false;if(site.host.equals(u.getHost())&&site.path.equals(u.getPath()))return u.equals(CapitalEighthNoticePage.selectDetailUri(site,u));return site==Site.PAJU&&selectPajuId(u,PAJU_DOWNLOAD)!=null;}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request r){if(r==null)return false;if("GET".equals(r.method()))return r.form().isEmpty()&&selectApprovedRequest(r.uri());return site==Site.GWANGMYEONG&&"POST".equals(r.method())&&safeUri(r.uri())&&site.fileHost.equals(r.uri().getHost())&&GM_DOWNLOAD.equals(r.uri().getPath())&&r.uri().getRawQuery()==null&&fileValues(r.form());}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source s,String html){
        URI detail=selectDetailUri(s);if(html==null||html.length()>1_048_576)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");var page=Jsoup.parse(html,detail.toASCIIString());Element area;
        try{area=CapitalEighthNoticePage.selectAttachments(site,page).clone();}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey);
        if(site==Site.PAJU){
            for(var item:area.select(":root > li"))try{
                var links=item.select(":root > a[href]");if(links.isEmpty()||links.size()>2)throw new IllegalArgumentException();var a=links.getFirst();if(a.hasAttr("onclick")||!a.children().isEmpty())throw new IllegalArgumentException();URI fetch=detail.resolve(a.attr("href"));String id=selectPajuId(fetch,PAJU_DOWNLOAD),name=a.text().strip();if(!safeName(name))throw new IllegalArgumentException();
                String format=format(name);var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(code,PAJU_DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),name,format,"UNKNOWN",format!=null);
                var old=files.get(id);if(old!=null){if(!old.equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;continue;}else files.put(id,d);
                if(links.size()==2){var preview=links.getLast();if(preview.hasAttr("onclick")||!"바로보기".equals(preview.text().strip())||!id.equals(selectPajuId(detail.resolve(preview.attr("href")),PAJU_PREVIEW)))unresolved=true;}
                var residual=item.clone();residual.select("a").remove();for(var icon:residual.select(":root > i.ico"))if(icon.attributes().size()==1&&icon.text().isBlank()&&icon.children().isEmpty()&&icon.className().matches("ico ico-file-[A-Za-z0-9]+"))icon.remove();for(var label:residual.select(":root > span.sr-only"))if(label.attributes().size()==1&&label.children().isEmpty()&&label.text().matches("[A-Za-z0-9]+"))label.remove();if(!residual.text().isBlank()||!residual.children().isEmpty())unresolved=true;item.remove();
            }catch(IllegalArgumentException e){unresolved=true;}
        }else{
            var forms=page.select("form#fileDown[name=fileDown][method=post]");if(forms.size()!=1)return failed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");var form=forms.getFirst();if(!("https://"+site.fileHost+GM_DOWNLOAD).equals(form.attr("action"))||form.childrenSize()!=3||form.select(":root > input[type=hidden]").size()!=3||!form.children().stream().map(e->e.attr("name")).collect(java.util.stream.Collectors.toSet()).equals(FIELDS)||form.children().stream().anyMatch(e->!e.val().isEmpty()))return failed("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            var slots=area.select(":root > ul#otherList");if(slots.isEmpty()||slots.size()>10||slots.stream().anyMatch(e->!e.text().isBlank()||!e.children().isEmpty()))return failed("ATTACHMENT_SELECTOR_CHANGED");slots.remove();
            var scripts=page.select("script:not([src])").stream().map(Element::data).filter(v->v.contains("var fileList = [];" )&&v.contains("$(\"#otherList\")")).toList();if(scripts.size()!=1)return failed("ATTACHMENT_MANIFEST_CHANGED");String script=scripts.getFirst();if(script.length()>65000)return failed("ATTACHMENT_MANIFEST_CHANGED");var matches=PUSH.matcher(script);int matched=0;var ids=new HashMap<String,String>();
            while(matches.find()){matched++;try{var n=mapper.readTree(matches.group(1));var names=new HashSet<String>();n.fieldNames().forEachRemaining(names::add);if(!names.equals(Set.of("sysFileNm","fileNm","filePath","fileId"))||selectAnyNonText(n)||!n.path("fileId").asText().matches("[0-9]{1,4}"))throw new IllegalArgumentException();var values=Map.of("user_file_nm",n.path("fileNm").asText(),"sys_file_nm",n.path("sysFileNm").asText(),"file_path",n.path("filePath").asText());var request=new Request(URI.create("https://"+site.fileHost+GM_DOWNLOAD),"POST",values);if(!selectApprovedRequest(request))throw new IllegalArgumentException();String id=normalizer.hash(values.get("file_path")+"\n"+values.get("sys_file_nm")),name=values.get("user_file_nm"),format=format(name);var prior=ids.putIfAbsent(n.path("fileId").asText(),id);if(prior!=null&&!prior.equals(id))unresolved=true;boolean supported=format!=null&&format.equals(format(values.get("sys_file_nm")));var d=new Descriptor(request.uri(),new AttachmentSetEvidence.Locator(code,GM_DOWNLOAD,Map.of("noticeId",notice,"attachmentId",id)),name,supported?format:null,"UNKNOWN",supported,values);var old=files.get(id);if(old!=null){if(!old.equals(d))unresolved=true;}else if(files.size()==10){exceeded=true;}else files.put(id,d);}catch(java.io.IOException|IllegalArgumentException e){unresolved=true;}}
            int start=script.indexOf("var fileList = [];")+"var fileList = [];".length(),end=script.indexOf("fileList.sort(",start);
            if(end<start||!PUSH.matcher(script.substring(start,end<start?start:end)).replaceAll("").isBlank()||matched!=PUSH_START.matcher(script).results().count())unresolved=true;
        }
        if(!area.text().isBlank()||!area.select("a,img,li,button,input,select,form,iframe,object,embed,script,[onclick],[href]").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private String format(String name){String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
    private boolean selectAnyNonText(com.fasterxml.jackson.databind.JsonNode n){for(var value:n)if(!value.isTextual())return true;return false;}
}
