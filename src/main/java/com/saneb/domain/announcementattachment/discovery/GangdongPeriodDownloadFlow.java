package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.jsoup.Jsoup;

/** 강동 공개 POST→게시기간 조회→파일 POST. 폼 실행이나 게시기간 우회는 허용하지 않는다. */
final class GangdongPeriodDownloadFlow {
    static final String HOST="eminwon.gangdong.go.kr", BRIDGE="/emwp/jsp/ofr/FileDownNewPbs.jsp";
    static final String PERIOD="/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do", FINAL="/emwp/jsp/ofr/FDSendNewPbs.jsp";
    private static final Set<String> FIELDS=Set.of("file_id","file_path","sys_file_nm","user_file_nm","pbs_end_ymd","method","methodnm","jndinm","context","isHome");
    private GangdongPeriodDownloadFlow() { }
    static boolean selectApprovedStep(Request request){
        if(request==null||!"POST".equals(request.method()))return false;var uri=request.uri();
        return Set.of(URI.create("https://"+HOST+PERIOD),URI.create("https://"+HOST+FINAL)).contains(uri)&&selectValidForm(request.form());
    }
    static boolean selectSameFileStep(Request initial,Request next){
        if(!selectApprovedStep(next))return false;var id=next.form().get("file_id");var name=initial.form().get("sys_file_nm");
        return name!=null&&name.matches(".+_"+java.util.regex.Pattern.quote(id)+"_[0-9]{1,3}\\.[A-Za-z0-9]+")
                &&(!PERIOD.equals(next.uri().getPath())||next.form().get("pbs_end_ymd").isEmpty());
    }
    static AttachmentPinnedDownloadClient.Download selectDownload(Request initial,Path output,long maximumBytes,AttachmentDownloadFlowProfile.Operation operation) throws IOException {
        var bridge=operation.selectDownload(initial,Math.min(maximumBytes,32768));Map<String,String> fields;
        try(var input=Files.newInputStream(output)){
            if(bridge.contentType()==null||!bridge.contentType().toLowerCase(Locale.ROOT).startsWith("text/html")||Files.size(output)>32768)throw blocked();
            var forms=Jsoup.parse(input,null,initial.uri().toASCIIString()).select("form");if(forms.size()!=1)throw blocked();var form=forms.getFirst();
            if(!"form".equals(form.id())||!"form".equals(form.attr("name"))||!"post".equalsIgnoreCase(form.attr("method"))
                    ||!"FDSendNewPbs.jsp".equals(form.attr("action"))||form.childrenSize()!=10)throw blocked();
            fields=new LinkedHashMap<>();for(var element:form.children()){
                if(!"input".equals(element.tagName())||!"hidden".equals(element.attr("type"))
                        ||!element.attributes().asList().stream().allMatch(a->Set.of("type","name","value").contains(a.getKey()))
                        ||fields.putIfAbsent(element.attr("name"),element.val())!=null)throw blocked();
            }
            if(!selectSameFileStep(initial,new Request(URI.create("https://"+HOST+PERIOD),"POST",fields)))throw blocked();
        }finally{Files.deleteIfExists(output);}
        var periodResponse=operation.selectDownload(new Request(URI.create("https://"+HOST+PERIOD),"POST",fields),Math.min(maximumBytes,256));String period;
        try{
            if(periodResponse.contentType()==null||!periodResponse.contentType().toLowerCase(Locale.ROOT).startsWith("text/")||Files.size(output)>256)throw blocked();
            period=Files.readString(output,StandardCharsets.UTF_8).strip();
        }finally{Files.deleteIfExists(output);}
        if("EmptyYmd".equals(period))period="";
        if(!period.isEmpty()&&!"EmptyYMD".equals(period)){
            try{if(!period.matches("[0-9]{8}")||LocalDate.parse(period,DateTimeFormatter.BASIC_ISO_DATE).isBefore(LocalDate.now(ZoneId.of("Asia/Seoul"))))throw new IllegalArgumentException();}
            catch(DateTimeException|IllegalArgumentException e){throw blocked();}
        }
        fields.put("pbs_end_ymd",period);
        return operation.selectDownload(new Request(URI.create("https://"+HOST+FINAL),"POST",fields),maximumBytes);
    }
    private static boolean selectValidForm(Map<String,String> fields){
        return fields.keySet().equals(FIELDS)&&fields.values().stream().allMatch(v->v!=null&&v.length()<=2048&&v.codePoints().noneMatch(Character::isISOControl))
                &&"N".equals(fields.get("isHome"))&&"NTIS".equals(fields.get("context"))&&"OfrNotAncmtEJB".equals(fields.get("jndinm"))
                &&"selectOfrNotAncmtPbs".equals(fields.get("method"))&&"selectOfrNotAncmtPbs".equals(fields.get("methodnm"))
                &&fields.get("file_id").matches("[A-Za-z0-9_]{10,200}")&&fields.get("file_path").matches("/ntisho[A-Za-z0-9+/]{30,}={0,2}")
                &&selectOpaqueName(fields.get("sys_file_nm"))&&selectOpaqueName(fields.get("user_file_nm"))
                &&(fields.get("pbs_end_ymd").isEmpty()||"EmptyYMD".equals(fields.get("pbs_end_ymd"))||fields.get("pbs_end_ymd").matches("[0-9]{8}"));
    }
    private static boolean selectOpaqueName(String value){
        if(value==null||value.length()<16||value.length()>2048)return false;
        int at=value.length();while(at>0&&(Character.isLetterOrDigit(value.charAt(at-1))&&value.charAt(at-1)<128||"+/=".indexOf(value.charAt(at-1))>=0))at--;
        return value.length()-at>=16&&value.substring(at).matches("[A-Za-z0-9+/]+={0,2}");
    }
    private static IOException blocked(){return new IOException("ATTACHMENT_DOWNLOAD_BLOCKED");}
}
