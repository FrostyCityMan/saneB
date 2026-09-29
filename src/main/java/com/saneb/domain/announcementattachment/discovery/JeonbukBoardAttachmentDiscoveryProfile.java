package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 실측한 공식 첨부 영역만 읽는다. 미리보기는 실행하지 않고 동일 파일의 보조 링크만 식별한다. */
final class JeonbukBoardAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { NAMWON, BUAN, GOCHANG }
    private final Site site;
    private final String code, host, sourceCode, listCode, board, menu, detailPath, downloadPath, idKey, fileKey, boardKey, menuKey, hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    JeonbukBoardAttachmentDiscoveryProfile(Site site){
        this.site=site;String city=site.name().toLowerCase(Locale.ROOT);code="LOCAL_"+site+"_GET_V1";host="www."+city+".go.kr";
        sourceCode=switch(site){case NAMWON->"LGS-000168";case BUAN->"LGS-000177";case GOCHANG->"LGS-000176";};
        listCode=switch(site){case NAMWON->"SPRING_BBS";case BUAN->"SAEOL_GOSI";case GOCHANG->"HEURISTIC_NOTICE";};
        board=switch(site){case NAMWON->"ff8080818ea1fec5018ea24137680031";case BUAN->"BBS_0000054";case GOCHANG->"BBS_0000180";};
        menu=switch(site){case NAMWON->"ff8080818e3beff0018e4077131b007a";case BUAN->"DOM_000000103001003000";case GOCHANG->"DOM_000000102003007000";};
        boolean n=site==Site.NAMWON;detailPath=n?"/board/post/view.do":"/board/view."+city;downloadPath=n?"/board/post/download.do":"/board/download."+city;
        idKey=n?"postUid":"dataSid";fileKey=n?"atchFileUid":"fileSid";boardKey=n?"boardUid":"boardId";menuKey=n?"menuUid":"menuCd";
        hash=AttachmentProfileFingerprint.selectHash(String.join("|","JEONBUK_BOARD:1",code,host,sourceCode,listCode,board,menu,detailPath,downloadPath,"https443|same-notice-files|unknown-role|limit10"),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return Set.of(host);}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,listCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try{
            if(source==null||!selectProviderCode().equals(source.providerCode())||!sourceCode.equals(source.localSourceCode())||!listCode.equals(source.listParserProfileCode())
                    ||source.sourceUrl()==null||source.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());if(!detailPath.equals(uri.getPath())||!selectApprovedRequest(uri))throw new IllegalArgumentException();return uri;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        if(uri==null||!"https".equals(uri.getScheme())||!host.equals(uri.getHost())||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getUserInfo()!=null||uri.getFragment()!=null
                ||!uri.equals(uri.normalize())||!Objects.equals(uri.getRawPath(),uri.getPath()))return false;
        boolean detail=detailPath.equals(uri.getPath());if(!detail&&!downloadPath.equals(uri.getPath()))return false;
        var q=selectParameters(uri.getRawQuery());if(!board.equals(q.get(boardKey))||!selectId(q.get(idKey)))return false;
        if(detail&&!menu.equals(q.get(menuKey))||!detail&&q.containsKey(menuKey)&&!menu.equals(q.get(menuKey)))return false;
        var allowed=new HashSet<>(Set.of(boardKey,menuKey,idKey));
        allowed.addAll(site==Site.NAMWON?Set.of("sort","searchType","keyword","page","size","codeUids"):Set.of("paging","startPage","searchType","keyword","orderBy"));
        if(!detail){allowed.add(fileKey);if(site!=Site.NAMWON)allowed.add("command");}
        if(!allowed.containsAll(q.keySet())||!detail&&!selectId(q.get(fileKey)))return false;
        return (!q.containsKey("command")||"update".equals(q.get("command")))&&(!q.containsKey("paging")||"ok".equals(q.get("paging")))
                &&(!q.containsKey("startPage")||q.get("startPage").matches("[1-9][0-9]{0,5}"));
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){
        return initial!=null&&next!=null&&initial.uri().equals(next.uri())&&selectApprovedRequest(initial)&&selectApprovedRequest(next);
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());
        var titles=page.select(site==Site.NAMWON?"table.view_table > thead > tr > td.title > strong":"div.bbs_view > div.bbs_vtop > h4");
        var areas=page.select(switch(site){case NAMWON->"table.view_table ul.file_list";case BUAN->"div.bbs_view > div.bbs_filedown";case GOCHANG->"p.bbs_filedown";});
        if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        Element area=areas.getFirst();
        if(site==Site.BUAN&&(area.select("dl > dt").size()!=1||!"첨부파일".equals(area.select("dl > dt").text().strip())))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean unresolved=false,exceeded=false;
        String noticeId=selectParameters(detail.getRawQuery()).get(idKey);
        for(var anchor:area.select("a")){
            try{
                URI fetch=detail.resolve(URI.create(anchor.attr("href").replace(" ","%20")));
                if(!downloadPath.equals(fetch.getPath())||!selectApprovedRequest(fetch)||!noticeId.equals(selectParameters(fetch.getRawQuery()).get(idKey))||anchor.hasAttr("onclick"))continue;
                if(site==Site.NAMWON&&anchor.hasClass("btn_down"))continue;
                String name=(site==Site.NAMWON?anchor.text():anchor.attr("title")).strip();if(!selectSafeName(name))continue;
                String fileId=selectParameters(fetch.getRawQuery()).get(fileKey);var previous=files.get(fileId);
                if(previous!=null){if(previous.fetchUri().equals(fetch)&&previous.displayName().equals(name))recognized.add(anchor);continue;}
                if(files.size()==10){exceeded=true;continue;}
                String ext=name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);boolean supported=Set.of("PDF","HWP","HWPX").contains(ext);
                var locator=new AttachmentSetEvidence.Locator(code,downloadPath,Map.of("noticeId",noticeId,"attachmentId",fileId));
                files.put(fileId,new Descriptor(fetch,locator,name,supported?ext:null,"UNKNOWN",supported));recognized.add(anchor);
            }catch(IllegalArgumentException ignored){/* 남은 링크는 아래에서 미해석 오류로 보존한다. */}
        }
        for(var anchor:area.select("a"))if(!recognized.contains(anchor)){
            boolean auxiliary=false;
            if(!anchor.hasAttr("onclick"))for(var entry:files.entrySet()){
                if(site==Site.NAMWON){
                    var li=anchor.closest("li");boolean sameItem=li!=null&&recognized.stream().anyMatch(a->a.closest("li")==li&&entry.getValue().displayName().equals(a.text().strip()));
                    if(sameItem&&anchor.hasClass("btn_down"))try{auxiliary=entry.getValue().fetchUri().equals(detail.resolve(URI.create(anchor.attr("href"))));}catch(IllegalArgumentException ignored){}
                    if(sameItem&&anchor.hasClass("btn_view")){
                        auxiliary=anchor.attr("href").matches("javascript:filePreView\\(\\s*''\\s*,\\s*'"+entry.getKey()+"'\\s*\\)\\s*;?");
                    }
                }else if(anchor.hasClass(site==Site.BUAN?"sbtn_file":"ico_viewer"))try{
                    URI preview=detail.resolve(URI.create(anchor.attr("href")));URI download=URI.create(preview.toString().replace("/board/SynapViewer.","/board/download."));
                    auxiliary=preview.getPath().equals("/board/SynapViewer."+site.name().toLowerCase(Locale.ROOT))&&entry.getValue().fetchUri().equals(download);
                }catch(IllegalArgumentException ignored){}
                if(auxiliary)break;
            }
            if(auxiliary)recognized.add(anchor);else unresolved=true;
        }
        Element residual=area.clone();residual.select("a").remove();if(site==Site.BUAN)residual.select("dl > dt").remove();
        String rest=residual.text().replaceAll("\\[[0-9]+Byte\\]","").strip();
        if(!rest.isEmpty()||!residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private boolean selectId(String value){return value!=null&&value.matches(site==Site.NAMWON?"[a-f0-9]{32}":"[0-9]{1,15}");}
    private boolean selectSafeName(String value){return !value.isBlank()&&value.length()<=500&&!value.contains("/")&&!value.contains("\\")&&!value.contains("..")&&!value.contains("%")&&value.indexOf('\ufffd')<0&&value.codePoints().noneMatch(Character::isISOControl);}
    private Map<String,String> selectParameters(String query){
        if(query==null||query.length()>8192)return Map.of();var values=new LinkedHashMap<String,String>();
        try{for(String pair:query.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);
            if(v.length()>500||v.codePoints().anyMatch(Character::isISOControl)||values.putIfAbsent(p[0],v)!=null)return Map.of();}return values;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
