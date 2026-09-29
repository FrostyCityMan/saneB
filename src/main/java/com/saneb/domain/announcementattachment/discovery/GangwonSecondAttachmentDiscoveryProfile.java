package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage;
import com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 영역의 파일 식별자만 GET에 결합한다. 원격 스크립트와 미리보기는 실행하지 않는다. */
final class GangwonSecondAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final Pattern YANGGU_DOWNLOAD=Pattern.compile("javascript:opendownload\\('announcement',\\s*([1-9][0-9]{0,14}),\\s*([1-9][0-9]?)\\)");
    private final Site site;private final String code,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    GangwonSecondAttachmentDiscoveryProfile(Site site) {
        this.site=site;code="LOCAL_"+site+"_BOARD_V1";
        hash=AttachmentProfileFingerprint.selectHash("GANGWON_SECOND:3|"+site+"|"+site.sourceCode+"|"+site.parser+"|https443|GET|same-request|limit10|unknown-role|yanggu-observed-octer-stream-utf8-disposition|"
                +AttachmentProfileFingerprint.selectHash("PAGE:1",GangwonSecondNoticePage.class)+"|"+AttachmentProfileFingerprint.selectHash("QUERY:1",CapitalThirdNoticePage.class),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(site.sourceCode,site.parser));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(site.host);}
    @Override public Set<String> selectLegacyBinaryContentTypes(){return site==Site.YANGGU?Set.of("application/octer-stream"):Set.of();}
    @Override public boolean selectUtf8DispositionOctets(){return site==Site.YANGGU;}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try {
            if(source==null||!selectProviderCode().equals(source.providerCode())||!site.sourceCode.equals(source.localSourceCode())||!site.parser.equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            return GangwonSecondNoticePage.selectDetailUri(site,URI.create(source.sourceUrl()));
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        try {
            if(uri==null||!"https".equals(uri.getScheme())||!site.host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null||!uri.equals(uri.normalize())||!Objects.equals(uri.getPath(),uri.getRawPath()))return false;
            if(site.path.equals(uri.getPath()))return uri.equals(GangwonSecondNoticePage.selectDetailUri(site,uri));
            if(!site.download.equals(uri.getPath()))return false;var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());
            return site==Site.INJE?q.keySet().equals(Set.of("fileSeq"))&&q.get("fileSeq").matches("[1-9][0-9]{0,14}")
                    :q.keySet().equals(Set.of("bcd","bn","num"))&&"announcement".equals(q.get("bcd"))&&q.get("bn").matches("[1-9][0-9]{0,14}")&&q.get("num").matches("[1-9][0-9]?");
        }catch(IllegalArgumentException e){return false;}
    }
    @Override public boolean selectApprovedRequest(Request r){return r!=null&&"GET".equals(r.method())&&r.form().isEmpty()&&selectApprovedRequest(r.uri());}
    @Override public boolean selectApprovedRequest(Request first,Request next){return first!=null&&first.equals(next)&&selectApprovedRequest(next);}
    @Override public Result selectDescriptors(Source source,String html){
        var detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        Element cell;try{cell=GangwonSecondNoticePage.selectAttachments(site,Jsoup.parse(html,detail.toASCIIString())).clone();}catch(IllegalArgumentException e){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        cell.select("script,style").remove();Element list=cell;
        if(site==Site.YANGGU){var lists=cell.select(":root > ul");if(lists.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");list=lists.getFirst();}
        var residueOutside=cell.clone();if(site==Site.YANGGU)residueOutside.select(":root > ul").remove();else residueOutside.empty();
        boolean unresolved=selectResidue(residueOutside)||!list.ownText().isBlank(),exceeded=false;var files=new LinkedHashMap<String,Descriptor>();
        String notice=CapitalThirdNoticePage.selectParameters(detail.getRawQuery()).get(site.idKey);
        for(var item:list.children())try{
            if(site==Site.YANGGU?!"li".equals(item.tagName()):!("div".equals(item.tagName())&&item.hasClass("attachFile")))throw new IllegalArgumentException();
            String linkSelector=site==Site.YANGGU?":root > p.filename > a":":root > a";var links=item.select(linkSelector);if(links.size()!=1)throw new IllegalArgumentException();var link=links.getFirst();if(link.hasAttr("onclick"))throw new IllegalArgumentException();
            var label=link.clone();label.select("span.icoFile").remove();String name=label.text().strip();
            if(name.isBlank()||name.length()>500||name.contains("/")||name.contains("\\")||name.contains("..")||name.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException();
            URI file;String bn=null;
            if(site==Site.YANGGU){var m=YANGGU_DOWNLOAD.matcher(link.attr("href"));if(!m.matches())throw new IllegalArgumentException();bn=m.group(1);file=URI.create("https://"+site.host+site.download+"?bcd=announcement&bn="+bn+"&num="+m.group(2));}
            else{String href=link.attr("href");if(!href.startsWith(site.download+"?"))throw new IllegalArgumentException();file=URI.create("https://"+site.host+href);}
            if(!selectApprovedRequest(file))throw new IllegalArgumentException();String id=normalizer.hash(file.toASCIIString());String format=selectFormat(name);
            if(files.containsKey(id)){if(!files.get(id).displayName().equals(name))unresolved=true;}
            else if(files.size()==10)exceeded=true;
            else files.put(id,new Descriptor(file,new AttachmentSetEvidence.Locator(code,site.download,Map.of("attachmentId",id,"noticeId",notice)),name,format,"UNKNOWN",format!=null));
            var residue=item.clone();residue.select(linkSelector).remove();
            if(site==Site.INJE){for(var counter:residue.select(":root > span.attachFile-txt"))if(counter.text().matches("\\(다운로드 수: [0-9,]+\\)")&&counter.children().isEmpty())counter.remove();}
            else {
                for(var duplicate:residue.select(":root > span.docdown > a"))if(link.attr("href").equals(duplicate.attr("href"))&&duplicate.text().equals("다운로드"))duplicate.remove();
                for(var counter:residue.select(":root > p.hit"))if(counter.text().matches("down : [0-9,]+"))counter.remove();
                selectRemovePreview(residue,bn,format);
            }
            if(selectResidue(residue)||!label.select("script,input,button,iframe,object,embed,img,[onclick]").isEmpty())unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private void selectRemovePreview(Element item,String bn,String format){
        var names=item.select("input[type=hidden][name=backboardfilename]");var boards=item.select("input[type=hidden][name=backbcd]");var ids=item.select("input[type=hidden][name=backboardkey]");
        if(names.size()!=1||boards.size()!=1||ids.size()!=1||!bn.equals(ids.getFirst().val())||!"board/announcement".equals(boards.getFirst().val()))return;
        String stored=names.getFirst().val();if(!stored.matches("[0-9]{10,20}_[0-9]{1,20}\\.[A-Za-z0-9]{1,8}")||!Objects.equals(format,selectFormat(stored)))return;
        String expected="/synap/docview?filePath=/data1/yanggu_uploadfiles/board/announcement/&filename="+stored;
        for(var p:item.select(":root > span.docview > a"))if(expected.equals(p.attr("href"))&&"미리보기".equals(p.text())&&!p.hasAttr("onclick"))p.remove();
        for(var p:item.select("input#SBPAPIBUN[type=button]"))if(("javascript:fn_sbapi_preview('"+stored+"','board/announcement','"+bn+"'); return false;").equals(p.attr("onclick")))p.remove();
        names.remove();boards.remove();ids.remove();
    }
    private boolean selectResidue(Element e){return !e.text().isBlank()||!e.select("a,input,button,img,iframe,form,object,embed,[href],[onclick]").isEmpty();}
    private String selectFormat(String n){String ext=n.contains(".")?n.substring(n.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Result selectFailed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
