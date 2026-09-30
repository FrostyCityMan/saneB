package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.SancheongNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 공식 파일 셀의 직접 다운로드만 실행하고 같은 파일 미리보기는 요청하지 않는다. */
@Component
public final class SancheongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private static final String CODE="LOCAL_SANCHEONG_BBS_V1",DOWNLOAD="/downloadBbsFile.do",PREVIEW="/previewBbs.do";
    private static final String HASH=AttachmentProfileFingerprint.selectHash(CODE+":1|LGS-000238|SAEOL_GOSI|same-request|file-cell|paired-preview|limit10|unknown-role|octer-utf8|"+AttachmentProfileFingerprint.selectHash("PAGE:1",SancheongNoticePage.class),SancheongAttachmentDiscoveryProfile.class);
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return HASH;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(SancheongNoticePage.HOST);}
    @Override public Set<String> selectLegacyBinaryContentTypes(){return Set.of("application/octer-stream");}
    @Override public boolean selectUtf8DispositionOctets(){return true;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000238","SAEOL_GOSI"));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){try{if(s==null||!selectProviderCode().equals(s.providerCode())||!"LGS-000238".equals(s.localSourceCode())||!"SAEOL_GOSI".equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();return SancheongNoticePage.selectDetailUri(URI.create(s.sourceUrl()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}}
    @Override public boolean selectApprovedRequest(URI uri){try{if(!SancheongNoticePage.selectSafe(uri))return false;if(SancheongNoticePage.PATH.equals(uri.getPath()))return uri.equals(SancheongNoticePage.selectDetailUri(uri));return DOWNLOAD.equals(uri.getPath())&&selectFileId(uri)!=null;}catch(IllegalArgumentException e){return false;}}
    @Override public boolean selectApprovedRequest(Request initial,Request next){return initial!=null&&initial.equals(next)&&selectApprovedRequest(next);}
    private String selectFileId(URI uri){var q=SancheongNoticePage.selectParameters(uri.getRawQuery());if(!q.keySet().equals(Set.of("atchmnflNo"))||!q.get("atchmnflNo").matches("[1-9][0-9]{0,14}"))throw new IllegalArgumentException();return q.get("atchmnflNo");}
    private boolean selectSafeName(String name){return name!=null&&!name.isBlank()&&name.length()<=500&&!name.contains("/")&&!name.contains("\\")&&!name.contains("..")&&name.indexOf('\ufffd')<0&&name.codePoints().noneMatch(Character::isISOControl);}
    private boolean selectActive(Element element){return !element.select("button,input,select,form,iframe,object,embed,script,style").isEmpty()||element.getAllElements().stream().anyMatch(e->e.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on")));}
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return failed("ATTACHMENT_DETAIL_UNAVAILABLE");Element cell;
        try{var root=SancheongNoticePage.selectRoot(Jsoup.parse(html,detail.toASCIIString()));SancheongNoticePage.selectCell(root,"내용");cell=SancheongNoticePage.selectCell(root,"파일");}catch(IllegalArgumentException e){return failed("ATTACHMENT_SELECTOR_CHANGED");}
        if(cell.children().isEmpty()&&cell.text().isBlank())return new Result("NO_FILES",true,List.of(),List.of());
        var lists=cell.select(":root > ul.brd_file");if(lists.size()!=1)return failed("ATTACHMENT_SELECTOR_CHANGED");
        var found=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
        var residual=cell.clone();residual.select("ul.brd_file > li").remove();if(!residual.text().isBlank()||!residual.select("a,img,[href]").isEmpty()||selectActive(residual))unresolved=true;
        for(var item:lists.getFirst().children())try{
            if(!"li".equals(item.tagName()))throw new IllegalArgumentException();var links=item.select(":root > a[href^=/downloadBbsFile.do?]");if(links.size()!=1)throw new IllegalArgumentException();var a=links.getFirst();if(selectActive(a))throw new IllegalArgumentException();URI fetch=detail.resolve(a.attr("href"));if(!selectApprovedRequest(fetch))throw new IllegalArgumentException();String id=selectFileId(fetch);
            var names=a.select(":root > span");var icons=a.select(":root > img");if(names.size()!=1||!names.getFirst().children().isEmpty()||icons.size()!=1||a.childrenSize()!=2||!a.ownText().isBlank())throw new IllegalArgumentException();String name=names.getFirst().text().strip();if(!selectSafeName(name)||!name.equals(icons.getFirst().attr("alt"))||!icons.getFirst().attr("src").matches("/common/images/board/file/ico_[A-Za-z0-9]+\\.gif"))throw new IllegalArgumentException();
            String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);var d=new Descriptor(fetch,new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",id,"noticeId",SancheongNoticePage.selectParameters(detail.getRawQuery()).get("nttNo"))),name,supported?ext:null,"UNKNOWN",supported);
            if(found.containsKey(id)){if(!found.get(id).equals(d))unresolved=true;}else if(found.size()==10)exceeded=true;else found.put(id,d);
            var rest=item.clone();rest.select(":root > a[href^=/downloadBbsFile.do?]").remove();for(var p:rest.select("a"))try{URI preview=detail.resolve(p.attr("href"));var label=p.select(":root > span.skip");if(!selectActive(p)&&SancheongNoticePage.selectSafe(preview)&&PREVIEW.equals(preview.getPath())&&id.equals(selectFileId(preview))&&p.hasClass("cnt_btn_view")&&label.size()==1&&name.equals(label.getFirst().text())&&"미리보기".equals(p.ownText().strip()))p.remove();}catch(IllegalArgumentException ignored){/* 미해석 링크를 아래 오류로 분리한다. */}
            if(!rest.text().isBlank()||!rest.select("a,img,[href]").isEmpty()||selectActive(rest))unresolved=true;
        }catch(IllegalArgumentException e){unresolved=true;}
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(found.values()),List.of("ATTACHMENT_FILE_LIMIT"));if(unresolved)return new Result("FAILED",false,List.copyOf(found.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));return new Result(found.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(found.values()),List.of());
    }
    private Result failed(String code){return new Result("FAILED",false,List.of(),List.of(code));}
}
