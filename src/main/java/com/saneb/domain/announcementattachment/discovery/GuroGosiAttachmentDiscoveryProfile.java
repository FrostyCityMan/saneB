package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.provider.content.ChungjuEminwonNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 구로 고시공고의 공식 첨부 셀만 사용한다. 목록 아이콘·미리보기는 다운로드 근거가 아니다. */
public final class GuroGosiAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    public static final String CODE="LOCAL_GURO_GOSI_V1";
    private static final String HOST="www.guro.go.kr", FILE_HOST="eminwon.guro.go.kr";
    private static final String DETAIL="/www/selectBbsNttGosiView.do", DOWNLOAD="/emwp/jsp/ofr/FileDown.jsp";
    private static final Set<String> SOURCE_KEYS=Set.of("bbsNo","nttNo","key","rowNum","pageUnit","searchCnd","searchKrwd","pageIndex");
    private final AnnouncementSourceIdentityNormalizer normalizer=new AnnouncementSourceIdentityNormalizer();
    private final String hash=AttachmentProfileFingerprint.selectHash("GURO_GOSI:1|LGS-000018|SAEOL_GOSI|663|1791|https443|exact-cell|no-redirect|unknown-role|limit10|strict-headers|"
            +AttachmentProfileFingerprint.selectHash("QUERY_CELL:1",ChungjuEminwonNoticePage.class),getClass());
    @Override public String selectProviderCode(){return "LOCAL_GOV_NOTICE";}
    @Override public String selectProfileCode(){return CODE;}
    @Override public String selectProfileHash(){return hash;}
    @Override public List<SourceBinding> selectSourceBindings(){return List.of(new SourceBinding("LGS-000018","SAEOL_GOSI"));}
    @Override public Set<String> selectApprovedHosts(){return Set.of(HOST,FILE_HOST);}
    @Override public URI selectDetailUri(String id){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public Result selectDescriptors(String id,String html){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    @Override public URI selectDetailUri(Source source){
        try {
            if(source==null||!selectProviderCode().equals(source.providerCode())||!"LGS-000018".equals(source.localSourceCode())
                    ||!"SAEOL_GOSI".equals(source.listParserProfileCode())||source.sourceUrl()==null||source.sourceUrl().length()>4096
                    ||!normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId()))throw new IllegalArgumentException();
            URI uri=URI.create(source.sourceUrl());
            if(!selectOrigin(uri,HOST)||!DETAIL.equals(uri.getPath()))throw new IllegalArgumentException();
            // 공식 목록의 &&/말미 &는 저장 identity에만 남기고 outbound 요청은 식별자3개로 고정한다.
            String query=String.join("&",Arrays.stream(Objects.requireNonNull(uri.getRawQuery()).split("&")).filter(s->!s.isEmpty()).toList());
            var values=ChungjuEminwonNoticePage.selectParameters(query);
            if(!SOURCE_KEYS.containsAll(values.keySet())||!"663".equals(values.get("bbsNo"))||!"1791".equals(values.get("key"))
                    ||!values.getOrDefault("nttNo","").matches("[1-9][0-9]{0,14}"))throw new IllegalArgumentException();
            return URI.create("https://"+HOST+DETAIL+"?bbsNo=663&nttNo="+values.get("nttNo")+"&key=1791");
        }catch(IllegalArgumentException|NullPointerException failure){throw new IllegalArgumentException("PROFILE_REQUIRED");}
    }
    @Override public boolean selectApprovedRequest(URI uri){
        try {
            if(selectOrigin(uri,HOST)&&DETAIL.equals(uri.getPath())) {
                var q=ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery());
                return q.keySet().equals(Set.of("bbsNo","nttNo","key"))&&"663".equals(q.get("bbsNo"))
                        &&"1791".equals(q.get("key"))&&q.get("nttNo").matches("[1-9][0-9]{0,14}");
            }
            if(!selectOrigin(uri,FILE_HOST)||!DOWNLOAD.equals(uri.getPath()))return false;
            var q=ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery());
            return q.keySet().equals(Set.of("user_file_nm","sys_file_nm","file_path"))
                    &&selectSafeName(q.get("user_file_nm"))&&selectSafeName(q.get("sys_file_nm"))
                    &&q.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
        }catch(IllegalArgumentException failure){return false;}
    }
    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request request){
        return initial.equals(request)&&selectApprovedRequest(initial)&&selectApprovedRequest(request);
    }
    @Override public Result selectDescriptors(Source source,String html){
        URI detail=selectDetailUri(source);
        if(html==null||html.length()>1_000_000)return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        try {
            var tables=Jsoup.parse(html,detail.toASCIIString()).select("div.p-wrap.bbs.bbs__view > table.p-table.block");
            if(tables.size()!=1)throw new IllegalArgumentException();
            Element table=tables.getFirst();
            var subjects=table.select("span.p-table__subject_text").stream().filter(e->e.closest("table")==table).toList();
            var content=table.select("td.p-table__content[title=내용]").stream().filter(e->e.closest("table")==table).toList();
            if(subjects.size()!=1||subjects.getFirst().text().isBlank()||content.size()!=1)throw new IllegalArgumentException();
            Element cell=ChungjuEminwonNoticePage.selectCell(table,"파일");
            if(cell.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on")))throw new IllegalArgumentException();
            if(cell.children().isEmpty()&&cell.text().isBlank())return new Result("NO_FILES",true,List.of(),List.of());
            if(cell.childrenSize()!=1||!cell.child(0).is("ul.p-attach")||!cell.ownText().isBlank()||!cell.child(0).ownText().isBlank())throw new IllegalArgumentException();
            var files=new LinkedHashMap<String,Descriptor>();boolean unresolved=false,exceeded=false;
            for(Element item:cell.child(0).children()) {
                try {
                    if(!item.is("li.p-attach__item")||!item.ownText().isBlank()
                            ||!item.select("script,iframe,form,input,button,object,embed").isEmpty()
                            ||item.getAllElements().stream().anyMatch(e->e.attributes().asList().stream().anyMatch(a->a.getKey().toLowerCase(Locale.ROOT).startsWith("on"))))throw new IllegalArgumentException();
                    var links=item.children().stream().filter(e->e.is("a.p-attach__link")).toList();
                    if(links.size()!=1)throw new IllegalArgumentException();
                    Element anchor=links.getFirst();
                    if(anchor.childrenSize()!=3||!anchor.child(0).is("span.p-icon")||!anchor.child(1).is("span")
                            ||anchor.child(1).childrenSize()!=0||!anchor.child(2).is("svg")||!anchor.ownText().isBlank())throw new IllegalArgumentException();
                    URI file=selectFileUri(anchor.attr("href"));var q=ChungjuEminwonNoticePage.selectParameters(file.getRawQuery());
                    String name=q.get("user_file_nm");if(!name.equals(anchor.child(1).text()))throw new IllegalArgumentException();
                    for(Element child:item.children())if(child!=anchor) {
                        if(!child.is("a.p-attach__preview")||!selectPreview(detail,child.attr("href"),q))throw new IllegalArgumentException();
                    }
                    if(item.childrenSize()>2||anchor.select("a,[href]").stream().anyMatch(e->e!=anchor))throw new IllegalArgumentException();
                    String identity=normalizer.hash(q.get("file_path")+"\n"+q.get("sys_file_nm"));
                    if(files.containsKey(identity)){if(!files.get(identity).fetchUri().equals(file))unresolved=true;continue;}
                    if(files.size()==10){exceeded=true;continue;}
                    String format=selectFormat(name);boolean supported=format!=null&&format.equals(selectFormat(q.get("sys_file_nm")));
                    var locator=new AttachmentSetEvidence.Locator(CODE,DOWNLOAD,Map.of("attachmentId",identity,"noticeId",
                            ChungjuEminwonNoticePage.selectParameters(detail.getRawQuery()).get("nttNo")));
                    files.put(identity,new Descriptor(file,locator,name,supported?format:null,"UNKNOWN",supported));
                }catch(IllegalArgumentException failure){unresolved=true;}
            }
            if(exceeded)return new Result("LIMIT_EXCEEDED",false,List.copyOf(files.values()),List.of("ATTACHMENT_FILE_LIMIT"));
            if(unresolved)return new Result("FAILED",false,List.copyOf(files.values()),List.of("ATTACHMENT_LINK_UNRESOLVED"));
            return new Result(files.isEmpty()?"NO_FILES":"FOUND",true,List.copyOf(files.values()),List.of());
        }catch(IllegalArgumentException failure){return selectFailed("ATTACHMENT_SELECTOR_CHANGED");}
    }
    private URI selectFileUri(String href){
        if(href==null||href.length()>8192||href.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException();
        URI uri=URI.create(href.replace(" ","%20"));if(!selectOrigin(uri,FILE_HOST)||!selectApprovedRequest(uri))throw new IllegalArgumentException();
        var query=new StringJoiner("&");ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery()).entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e->query.add(e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8).replace("+","%20")));
        return URI.create("https://"+FILE_HOST+DOWNLOAD+"?"+query);
    }
    private boolean selectPreview(URI detail,String href,Map<String,String> file){
        try{URI uri=detail.resolve(URI.create(href.replace(" ","%20")));return selectOrigin(uri,HOST)&&"/previewBbs.do".equals(uri.getPath())
                &&file.equals(ChungjuEminwonNoticePage.selectParameters(uri.getRawQuery()));}catch(IllegalArgumentException failure){return false;}
    }
    private boolean selectOrigin(URI uri,String host){return uri!=null&&"https".equals(uri.getScheme())&&host.equals(uri.getHost())
            &&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null&&uri.getFragment()==null&&uri.equals(uri.normalize())
            &&uri.getRawPath()!=null&&uri.getRawPath().equals(uri.getPath());}
    private boolean selectSafeName(String value){return value!=null&&!value.isBlank()&&value.length()<=500&&!value.contains("/")
            &&!value.contains("\\")&&!value.contains("..")&&!value.contains("%")&&value.codePoints().noneMatch(Character::isISOControl);}
    private String selectFormat(String value){String suffix=value.contains(".")?value.substring(value.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";
        return Set.of("PDF","HWP","HWPX").contains(suffix)?suffix:null;}
    private Result selectFailed(String warning){return new Result("FAILED",false,List.of(),List.of(warning));}
}
