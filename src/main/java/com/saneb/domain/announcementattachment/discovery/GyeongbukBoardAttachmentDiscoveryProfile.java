package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 첨부 영역의 숫자 파일 ID만 읽는다. 본문 script와 문서 뷰어는 실행하지 않는다. */
final class GyeongbukBoardAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Site { GYEONGJU, GYEONGSAN, UISEONG }
    private final Site site;
    private final String code,host,sourceCode,parserCode,menu,detailPath,downloadPath,idKey,fileKey,hash;
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    GyeongbukBoardAttachmentDiscoveryProfile(Site site){
        this.site=site;code="LOCAL_"+site+"_BOARD_V1";
        host=switch(site){case GYEONGJU->"www.gyeongju.go.kr";case GYEONGSAN->"www.gbgs.go.kr";case UISEONG->"www.usc.go.kr";};
        sourceCode=switch(site){case GYEONGJU->"LGS-000202";case GYEONGSAN->"LGS-000210";case UISEONG->"LGS-000211";};
        parserCode=site==Site.UISEONG?"GUNWI_NOTICE_TABLE":"SPRING_BBS";menu=switch(site){case GYEONGJU->"423";case GYEONGSAN->"2160";case UISEONG->"157";};
        detailPath=site==Site.UISEONG?"/ko/page.do":"/open_content/ko/page.do";
        downloadPath=site==Site.UISEONG?"/programs/board/saeol/notice/download.do":"/programs/board/download.do";
        idKey=site==Site.UISEONG?"not_ancmt_mgt_no":"parm_bod_uid";fileKey=site==Site.UISEONG?"file_seq":"parm_file_uid";
        hash=AttachmentProfileFingerprint.selectHash(String.join("|",code,"1",host,sourceCode,parserCode,menu,detailPath,downloadPath,"https443|numeric-function-or-direct-link|paired-preview-only|same-request-or-uiseong-official-file-redirect|gyeongju-gyeongsan-utf8-disposition|partial-preserved|unknown-role|limit10"),getClass());
    }
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return code;}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return site==Site.UISEONG?Set.of(host,"eminwon.uiseong.go.kr"):Set.of(host);}
    @Override public boolean selectUtf8DispositionOctets(){return site!=Site.UISEONG;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding(sourceCode,parserCode));}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source s){
        try{
            if(s==null||!selectProviderCode().equals(s.providerCode())||!sourceCode.equals(s.localSourceCode())||!parserCode.equals(s.listParserProfileCode())||s.sourceUrl()==null||s.sourceUrl().length()>4096||!normalizer.hash(normalizer.canonicalizeUrl(s.sourceUrl())).equals(s.providerNoticeId()))throw new IllegalArgumentException();
            URI u=URI.create(s.sourceUrl());if(!detailPath.equals(u.getPath())||!selectApprovedRequest(u))throw new IllegalArgumentException();return u;
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI u){
        if(u==null||!"https".equals(u.getScheme())||u.getHost()==null||!selectApprovedHosts().contains(u.getHost())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!u.equals(u.normalize())||!Objects.equals(u.getRawPath(),u.getPath()))return false;
        var q=selectQuery(u.getRawQuery());
        if(!host.equals(u.getHost()))return site==Site.UISEONG&&"/emwp/jsp/ofr/FileDown.jsp".equals(u.getPath())&&q.keySet().equals(Set.of("user_file_nm","sys_file_nm","file_path"))&&selectName(q.get("user_file_nm"))&&selectName(q.get("sys_file_nm"))&&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        if(detailPath.equals(u.getPath())){
            if(!menu.equals(q.get("mnu_uid"))||!selectId(q.get(idKey)))return false;
            if(site==Site.UISEONG)return "2".equals(q.get("cmd"))&&Set.of("mnu_uid",idKey,"cmd","board_code","srchSDate","srchKwd","srchColumn","srchDept","srchEDate","pageNo","boardType").containsAll(q.keySet())&&(!q.containsKey("boardType")||"notice".equals(q.get("boardType")));
            return "258".equals(q.get("step"))&&Set.of("mnu_uid",idKey,"step","pageNo","pagePrvNxt","pageRef","pageOrder","srchVoteType","parm_mnu_uid","srchEnable","srchBgpUid","srchKeyword","srchSDate","srchColumn","srchEDate","pageSize").containsAll(q.keySet());
        }
        if(!downloadPath.equals(u.getPath())||!selectId(q.get(fileKey)))return false;
        return switch(site){case GYEONGJU->q.keySet().equals(Set.of(fileKey));case GYEONGSAN->q.keySet().equals(Set.of(fileKey,"bod_uid","mnu_uid"))&&selectId(q.get("bod_uid"))&&menu.equals(q.get("mnu_uid"));case UISEONG->q.keySet().equals(Set.of(fileKey,idKey))&&selectId(q.get(idKey));};
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){
        if(initial==null||next==null||!selectApprovedRequest(initial)||!selectApprovedRequest(next))return false;
        if(initial.equals(next))return true;
        // 의성 대표 누리집의 검증된 다운로드 응답만 같은 기관 전자민원 파일 서버로 연결한다.
        return site==Site.UISEONG&&host.equals(initial.uri().getHost())&&downloadPath.equals(initial.uri().getPath())&&"eminwon.uiseong.go.kr".equals(next.uri().getHost())&&"/emwp/jsp/ofr/FileDown.jsp".equals(next.uri().getPath());
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page=Jsoup.parse(html,detail.toASCIIString());boolean uiseong=site==Site.UISEONG;
        var titles=page.select(uiseong?"div.boardView > dl.title > dt":"#viewBoardContent > h4.view_tle");
        var areas=page.select(uiseong?"div.boardView > dl.title > dd > ul":"#viewBoardContent > dl.view_file > dd");
        if(titles.size()!=1||titles.getFirst().text().isBlank()||areas.size()!=1)return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var area=areas.getFirst();if(!uiseong){var label=area.previousElementSibling();if(label==null||!"dt".equals(label.tagName())||!"파일".equals(label.text().strip()))return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
        String notice=selectQuery(detail.getRawQuery()).get(idKey);var files=new LinkedHashMap<String,Descriptor>();var recognized=new HashSet<Element>();boolean exceeded=false;
        for(var a:area.select("a")){
            try{
                URI fetch=selectDownload(detail,a,notice);if(fetch==null)continue;String name=a.text().strip();if(!selectName(name))continue;
                String file=selectQuery(fetch.getRawQuery()).get(fileKey);
                if(files.containsKey(file)){if(files.get(file).displayName().equals(name)&&files.get(file).fetchUri().equals(fetch))recognized.add(a);continue;}
                if(files.size()==10){exceeded=true;continue;}String ext=selectFormat(name);
                files.put(file,new Descriptor(fetch,new AttachmentSetEvidence.Locator(code,downloadPath,Map.of("noticeId",notice,"attachmentId",file)),name,ext,"UNKNOWN",ext!=null));recognized.add(a);
            }catch(IllegalArgumentException ignored){/* 이 링크만 오류로 남기고 다른 파일을 계속 처리한다. */}
        }
        for(var a:area.select("a"))if(!recognized.contains(a)){
            var previous=a.previousElementSibling();if(previous==null||!recognized.contains(previous))continue;
            URI download=selectDownload(detail,previous,notice);if(download==null)continue;String file=selectQuery(download.getRawQuery()).get(fileKey);
            String call=a.attr("onclick");boolean preview;
            if(uiseong)preview="#".equals(a.attr("href"))&&"[미리보기]".equals(a.text().strip())&&call.matches("return\\s+openSynap\\(this,\\s*'"+file+"',\\s*'"+notice+"'\\);?");
            else preview=("viewFiles_"+file).equals(a.id())&&("#viewFiles_"+file).equals(a.attr("href"))&&call.matches("openViewFiles\\("+file+",\\s*"+notice+"\\);\\s*return false;");
            if(preview)recognized.add(a);
        }
        boolean unresolved=area.select("a").stream().anyMatch(a->!recognized.contains(a));
        var residual=area.clone();residual.select("a").remove();if(!residual.text().isBlank()||!residual.select("button,input,select,form,iframe,object,embed,script,[onclick],[href],img").isEmpty()||files.isEmpty()&&!residual.select("li").isEmpty())unresolved=true;
        if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
        if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
    }
    private URI selectDownload(URI detail,Element a,String notice){
        URI result;
        if(site==Site.UISEONG){
            if(a.hasAttr("onclick"))return null;result=detail.resolve(URI.create(a.attr("href")));if(!notice.equals(selectQuery(result.getRawQuery()).get(idKey)))return null;
        }else{
            String call=a.attr("onclick");if(call.length()>512)return null;
            String args=site==Site.GYEONGJU?"([0-9]{1,15})":"([0-9]{1,15}),\\s*"+notice+",\\s*"+menu;
            var m=Pattern.compile("openDownloadFiles\\("+args+"\\);\\s*return false;").matcher(call);
            if(!m.matches())return null;String file=m.group(1);
            if(!("downFiles_"+file).equals(a.id())||!("#downFiles_"+file).equals(a.attr("href"))||!a.hasClass("clsFileDownload"))return null;
            result=URI.create("https://"+host+downloadPath+"?"+fileKey+"="+file+(site==Site.GYEONGSAN?"&bod_uid="+notice+"&mnu_uid="+menu:""));
        }
        return downloadPath.equals(result.getPath())&&selectApprovedRequest(result)?result:null;
    }
    private boolean selectId(String s){return s!=null&&s.matches("[0-9]{1,15}");}
    private boolean selectName(String s){return !s.isBlank()&&s.length()<=500&&!s.contains("/")&&!s.contains("\\")&&!s.contains("..")&&!s.contains("%")&&s.indexOf('\ufffd')<0&&s.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String s){String ext=s.substring(s.lastIndexOf('.')+1).toUpperCase(Locale.ROOT);return Set.of("PDF","HWP","HWPX").contains(ext)?ext:null;}
    private Map<String,String> selectQuery(String query){
        if(query==null||query.length()>8192)return Map.of();var q=new LinkedHashMap<String,String>();String input=query.endsWith("&")?query.substring(0,query.length()-1):query;
        try{for(String pair:input.split("&",-1)){String[] p=pair.split("=",-1);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))return Map.of();String v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);if(v.length()>500||v.codePoints().anyMatch(Character::isISOControl)||q.putIfAbsent(p[0],v)!=null)return Map.of();}return q;}catch(IllegalArgumentException e){return Map.of();}
    }
    private Result selectFailed(String reason){return new Result("FAILED",false,List.of(),List.of(reason));}
}
