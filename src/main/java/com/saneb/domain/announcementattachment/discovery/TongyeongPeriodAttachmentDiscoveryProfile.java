package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 통영의 공식 게재기간 조회 GET→POST→POST 절차. 기간 종료·변경된 폼은 파일 요청 전에 중단한다. */
final class TongyeongPeriodAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String HOST="eminwon.tongyeong.go.kr",BRIDGE="/emwp/jsp/ofr/FileDownNewPbs.jsp";
    private static final String PERIOD="/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",FINAL="/emwp/jsp/ofr/FDSendNewPbs.jsp";
    private static final Set<String> FORM=Set.of("file_id","file_path","sys_file_nm","user_file_nm","pbs_end_ymd","method","methodnm","jndinm","context","isHome");
    private final AttachmentDiscoveryProfile delegate;
    private final String hash;
    TongyeongPeriodAttachmentDiscoveryProfile(AttachmentDiscoveryProfile delegate){
        if(!"LOCAL_TONGYEONG_SCMS_V1".equals(delegate.selectProfileCode()))throw new IllegalArgumentException("TONGYEONG_PROFILE_REQUIRED");
        this.delegate=delegate;hash=AttachmentProfileFingerprint.selectHash("TONGYEONG_PERIOD:1|three-stage|same-request|isHome-empty|period-observed-EmptyYMD-preserved|expired-blocked|"+delegate.selectProfileHash(),getClass());
    }
    @Override public String selectProviderCode(){return delegate.selectProviderCode();}
    @Override public String selectProfileCode(){return delegate.selectProfileCode();}
    @Override public String selectProfileHash(){return hash;}
    @Override public Set<String> selectApprovedHosts(){return delegate.selectApprovedHosts();}
    @Override public List<SourceBinding> selectSourceBindings(){return delegate.selectSourceBindings();}
    @Override public URI selectDetailUri(String id){return delegate.selectDetailUri(id);}
    @Override public URI selectDetailUri(Source source){return delegate.selectDetailUri(source);}
    @Override public Result selectDescriptors(String id,String html){return delegate.selectDescriptors(id,html);}
    @Override public Result selectDescriptors(Source source,String html){return delegate.selectDescriptors(source,html);}
    @Override public boolean selectApprovedRequest(URI uri){return delegate.selectApprovedRequest(uri);}
    @Override public boolean selectApprovedRequest(Request request){
        if(request==null)return false;if("GET".equals(request.method()))return delegate.selectApprovedRequest(request);
        URI uri=request.uri();return "POST".equals(request.method())&&"https".equals(uri.getScheme())&&HOST.equals(uri.getHost())
                &&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null&&uri.getFragment()==null&&uri.getRawQuery()==null
                &&uri.equals(uri.normalize())&&uri.getRawPath().equals(uri.getPath())&&Set.of(PERIOD,FINAL).contains(uri.getPath())&&selectValidForm(request.form());
    }
    @Override public boolean selectApprovedRequest(Request initial,Request next){
        if(initial==null||next==null||!"GET".equals(initial.method())||!selectApprovedRequest(initial))return false;
        if(initial.equals(next))return true;
        if(!BRIDGE.equals(initial.uri().getPath())||!"POST".equals(next.method())||!selectApprovedRequest(next))return false;
        String systemName=selectParameters(initial.uri().getRawQuery()).get("sys_file_nm");
        return systemName!=null&&systemName.matches(".+_"+java.util.regex.Pattern.quote(next.form().get("file_id"))+"_[0-9]{1,3}\\.[A-Za-z0-9]+")
                &&(!PERIOD.equals(next.uri().getPath())||next.form().get("pbs_end_ymd").isEmpty());
    }
    @Override public com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download selectDownload(
            com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request initial, java.nio.file.Path output,
            long maximumBytes, Operation operation) throws java.io.IOException {
        if(!"GET".equals(initial.method()) || !BRIDGE.equals(initial.uri().getPath())) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        var bridge=operation.selectDownload(initial,Math.min(maximumBytes,32768));
        if(bridge.contentType()==null || !bridge.contentType().toLowerCase(Locale.ROOT).startsWith("text/html")
                || java.nio.file.Files.size(output)>32768) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        Map<String,String> fields;
        try(var input=java.nio.file.Files.newInputStream(output)) {
            var document=Jsoup.parse(input,null,initial.uri().toASCIIString());
            var forms=document.select("form");
            if(forms.size()!=1) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
            var form=forms.getFirst();
            if(!"form".equals(form.attr("name")) || !"form".equals(form.id()) || !"post".equalsIgnoreCase(form.attr("method"))
                    || !"FDSendNewPbs.jsp".equals(form.attr("action")) || form.childrenSize()!=10) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
            fields=new LinkedHashMap<>();
            for(var element:form.children()) {
                if(!"input".equals(element.tagName()) || !"hidden".equals(element.attr("type"))
                        || !element.attributes().asList().stream().allMatch(a->Set.of("type","name","value").contains(a.getKey()))
                        || fields.putIfAbsent(element.attr("name"),element.val())!=null) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
            }
            String systemName=selectParameters(initial.uri().getRawQuery()).get("sys_file_nm");
            if(!selectValidForm(fields) || !fields.get("pbs_end_ymd").isEmpty() || systemName==null
                    || !systemName.matches(".+_"+java.util.regex.Pattern.quote(fields.get("file_id"))+"_[0-9]{1,3}\\.[A-Za-z0-9]+"))
                throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        } finally { java.nio.file.Files.deleteIfExists(output); }
        // 사이트가 수행하는 게재기간 조회를 생략하거나 isHome=Y로 우회하지 않는다.
        var periodResponse=operation.selectDownload(new com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request(
                URI.create("https://"+HOST+PERIOD),"POST",fields),Math.min(maximumBytes,256));
        String period;
        try {
            if(java.nio.file.Files.size(output)>256 || periodResponse.contentType()==null
                    || !periodResponse.contentType().toLowerCase(Locale.ROOT).startsWith("text/")) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
            period=java.nio.file.Files.readString(output,StandardCharsets.UTF_8).strip();
        } finally { java.nio.file.Files.deleteIfExists(output); }
        if("EmptyYmd".equals(period)) period="";
        if(!period.isEmpty() && !"EmptyYMD".equals(period)) {
            try {
                if(!period.matches("[0-9]{8}") || java.time.LocalDate.parse(period,java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                        .isBefore(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")))) throw new IllegalArgumentException();
            } catch(java.time.DateTimeException|IllegalArgumentException exception) { throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED"); }
        }
        fields.put("pbs_end_ymd",period);
        return operation.selectDownload(new com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request(
                URI.create("https://"+HOST+FINAL),"POST",fields),maximumBytes);
    }
    private boolean selectValidForm(Map<String,String> fields) {
        return fields.keySet().equals(FORM) && fields.values().stream().allMatch(v->v!=null && v.length()<=2048 && v.codePoints().noneMatch(Character::isISOControl))
                && "".equals(fields.get("isHome")) && "NTIS".equals(fields.get("context")) && "OfrNotAncmtEJB".equals(fields.get("jndinm"))
                && "selectOfrNotAncmtPbs".equals(fields.get("method")) && "selectOfrNotAncmtPbs".equals(fields.get("methodnm"))
                && fields.get("file_id").matches("[A-Za-z0-9_]{10,200}")
                && fields.get("file_path").matches("/ntisho[A-Za-z0-9+/]{30,}={0,2}")
                && selectOpaqueName(fields.get("sys_file_nm")) && selectOpaqueName(fields.get("user_file_nm"))
                && (fields.get("pbs_end_ymd").isEmpty() || "EmptyYMD".equals(fields.get("pbs_end_ymd")) || fields.get("pbs_end_ymd").matches("[0-9]{8}"));
    }
    private boolean selectOpaqueName(String value) {
        if(value==null || value.length()<16 || value.length()>2048) return false;
        int at=value.length();while(at>0 && (Character.isLetterOrDigit(value.charAt(at-1)) && value.charAt(at-1)<128 || "+/=".indexOf(value.charAt(at-1))>=0)) at--;
        return value.length()-at>=16 && value.substring(at).matches("[A-Za-z0-9+/]+={0,2}");
    }
    private Map<String, String> selectParameters(String query) {
        if (query == null || query.length() > 8192) return Map.of();
        var values = new LinkedHashMap<String, String>();
        try {
            for (String pair : query.split("&", -1)) {
                String[] parts = pair.split("=", -1);
                if (parts.length != 2 || !parts[0].matches("[A-Za-z_]+")
                        || values.putIfAbsent(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8)) != null) return Map.of();
            }
            return values;
        } catch (IllegalArgumentException exception) { return Map.of(); }
    }
}
