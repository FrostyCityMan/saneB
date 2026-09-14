package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 실측한 두 법정 공고 게시판만 처리한다. 목록 SPRING_BBS를 전체 첨부 승인으로 해석하지 않는다. */
public final class LegalBoardAttachmentDiscoveryProfile implements AttachmentDownloadFlowProfile {
    private static final String GANGBUK_DETAIL = "/portal/bbs/B0000245/view.do";
    private static final String SAEOL_DOWNLOAD = "/emwp/jsp/ofr/FileDown.jsp";
    private static final String GANGBUK_PERIOD = "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    private static final String GANGBUK_FINAL = "/emwp/jsp/ofr/FDSendNewPbs.jsp";
    private static final Set<String> GANGBUK_FORM = Set.of("file_id","file_path","sys_file_nm","user_file_nm","pbs_end_ymd",
            "method","methodnm","jndinm","context","isHome");
    private final boolean busan;
    private final String code, sourceCode, detailHost, downloadHost, detailPath, downloadPath, hash;
    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();

    LegalBoardAttachmentDiscoveryProfile(boolean busan) {
        this.busan = busan;
        code = busan ? "LOCAL_BUSAN_LEGAL_GET_V1" : "LOCAL_GANGBUK_LEGAL_GET_V1";
        sourceCode = busan ? "LGS-000027" : "LGS-000010";
        detailHost = busan ? "www.busan.go.kr" : "child.gangbuk.go.kr";
        downloadHost = busan ? detailHost : "eminwon.gangbuk.go.kr";
        detailPath = busan ? "/nbgosi/view" : GANGBUK_DETAIL;
        downloadPath = busan ? "/nbgosi/download" : SAEOL_DOWNLOAD;
        hash = AttachmentProfileFingerprint.selectHash(String.join("|", "LEGAL_BOARD_GET:1", code, sourceCode,
                detailHost, downloadHost, detailPath, downloadPath, "SPRING_BBS|https443|exact-query|unknown-role|all-files|limit10"), getClass());
    }
    @Override public String selectProviderCode() { return "LOCAL_GOV_NOTICE"; }
    @Override public List<SourceBinding> selectSourceBindings() { return List.of(new SourceBinding(sourceCode,"SPRING_BBS")); }
    @Override public String selectProfileCode() { return code; }
    @Override public String selectProfileHash() { return hash; }
    @Override public Set<String> selectApprovedHosts() { return Set.copyOf(List.of(detailHost, downloadHost)); }
    @Override public boolean selectUtf8DispositionOctets() { return busan; }
    @Override public boolean selectApprovedRequest(com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request request) {
        if(request==null) return false;
        if("GET".equals(request.method())) return selectApprovedRequest(request.uri());
        URI uri=request.uri();
        return !busan && "POST".equals(request.method()) && "https".equals(uri.getScheme()) && downloadHost.equals(uri.getHost())
                && (uri.getPort()==-1 || uri.getPort()==443) && uri.getUserInfo()==null && uri.getFragment()==null && uri.getRawQuery()==null
                && uri.equals(uri.normalize()) && uri.getRawPath().equals(uri.getPath())
                && Set.of(GANGBUK_PERIOD,GANGBUK_FINAL).contains(uri.getPath()) && selectValidForm(request.form());
    }

    @Override public com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download selectDownload(
            com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request initial, java.nio.file.Path output,
            long maximumBytes, Operation operation) throws java.io.IOException {
        if(busan || !SAEOL_DOWNLOAD.equals(initial.uri().getPath())) return operation.selectDownload(initial,maximumBytes);
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
            if(!selectValidForm(fields) || systemName==null
                    || !systemName.matches(".+_"+java.util.regex.Pattern.quote(fields.get("file_id"))+"_[0-9]{1,3}\\.[A-Za-z0-9]+"))
                throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
        } finally { java.nio.file.Files.deleteIfExists(output); }
        // 사이트가 수행하는 게재기간 조회를 생략하거나 isHome=Y로 우회하지 않는다.
        var periodResponse=operation.selectDownload(new com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request(
                URI.create("https://"+downloadHost+GANGBUK_PERIOD),"POST",fields),Math.min(maximumBytes,256));
        String period;
        try {
            if(java.nio.file.Files.size(output)>256 || periodResponse.contentType()==null
                    || !periodResponse.contentType().toLowerCase(Locale.ROOT).startsWith("text/")) throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED");
            period=java.nio.file.Files.readString(output,StandardCharsets.UTF_8).strip();
        } finally { java.nio.file.Files.deleteIfExists(output); }
        if("EmptyYmd".equals(period)) period="";
        if(!period.isEmpty()) {
            try {
                if(!period.matches("[0-9]{8}") || java.time.LocalDate.parse(period,java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                        .isBefore(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")))) throw new IllegalArgumentException();
            } catch(java.time.DateTimeException|IllegalArgumentException exception) { throw new java.io.IOException("ATTACHMENT_DOWNLOAD_BLOCKED"); }
        }
        fields.put("pbs_end_ymd",period);
        return operation.selectDownload(new com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request(
                URI.create("https://"+downloadHost+GANGBUK_FINAL),"POST",fields),maximumBytes);
    }
    private boolean selectValidForm(Map<String,String> fields) {
        return fields.keySet().equals(GANGBUK_FORM) && fields.values().stream().allMatch(v->v!=null && v.length()<=2048 && v.codePoints().noneMatch(Character::isISOControl))
                && "".equals(fields.get("isHome")) && "NTIS".equals(fields.get("context")) && "OfrNotAncmtEJB".equals(fields.get("jndinm"))
                && "selectOfrNotAncmtPbs".equals(fields.get("method")) && "selectOfrNotAncmtPbs".equals(fields.get("methodnm"))
                && fields.get("file_id").matches("[A-Za-z0-9_]{10,200}")
                && fields.get("file_path").matches("/ntisho[A-Za-z0-9+/]{30,}={0,2}")
                && selectOpaqueName(fields.get("sys_file_nm")) && selectOpaqueName(fields.get("user_file_nm"))
                && (fields.get("pbs_end_ymd").isEmpty() || fields.get("pbs_end_ymd").matches("[0-9]{8}"));
    }
    private boolean selectOpaqueName(String value) {
        if(value==null || value.length()<16 || value.length()>2048) return false;
        int at=value.length();while(at>0 && (Character.isLetterOrDigit(value.charAt(at-1)) && value.charAt(at-1)<128 || "+/=".indexOf(value.charAt(at-1))>=0)) at--;
        return value.length()-at>=16 && value.substring(at).matches("[A-Za-z0-9+/]+={0,2}");
    }
    @Override public URI selectDetailUri(String id) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public URI selectDetailUri(Source source) {
        try {
            if (source == null || !selectProviderCode().equals(source.providerCode()) || !sourceCode.equals(source.localSourceCode())
                    || !"SPRING_BBS".equals(source.listParserProfileCode()) || source.sourceUrl() == null || source.sourceUrl().length() > 4096
                    || !normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl())).equals(source.providerNoticeId())) throw new IllegalArgumentException();
            URI uri = URI.create(source.sourceUrl());
            if (!detailHost.equals(uri.getHost()) || !detailPath.equals(uri.getPath()) || !selectApprovedRequest(uri)) throw new IllegalArgumentException();
            return uri;
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    }
    @Override public boolean selectApprovedRequest(URI uri) {
        if (uri == null || !"https".equals(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getUserInfo() != null || uri.getFragment() != null || uri.getRawPath() == null
                || !uri.equals(uri.normalize()) || !uri.getRawPath().equals(uri.getPath())) return false;
        Map<String, String> values = selectParameters(uri.getRawQuery());
        if (detailHost.equals(uri.getHost()) && detailPath.equals(uri.getPath())) {
            if (busan) return values.keySet().containsAll(Set.of("sno", "gosiGbn"))
                    && Set.of("sno", "gosiGbn", "curPage").containsAll(values.keySet())
                    && values.get("sno").matches("[0-9]{1,15}") && values.get("gosiGbn").matches("[A-Z]")
                    && (!values.containsKey("curPage") || values.get("curPage").matches("[1-9][0-9]{0,6}"));
            return values.keySet().equals(Set.of("menuNo", "nttId")) && "200082".equals(values.get("menuNo"))
                    && values.get("nttId").matches("[0-9]{1,15}");
        }
        if (!downloadHost.equals(uri.getHost()) || !downloadPath.equals(uri.getPath())) return false;
        if (busan) return values.keySet().equals(Set.of("fileId", "seq")) && values.get("fileId").matches("F[0-9]{10,30}")
                && values.get("seq").matches("[0-9]{1,3}");
        return values.keySet().equals(Set.of("user_file_nm", "sys_file_nm", "file_path"))
                && selectSafeName(values.get("user_file_nm")) && selectSafeName(values.get("sys_file_nm"))
                && values.get("file_path").matches("/ntishome/file/upload/ofr/ofr/[0-9]{8}");
    }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var document = Jsoup.parse(html, detail.toASCIIString());
        List<Element> labels = document.select(busan ? "dl.form-data-info > dt" : "dl.file-lists > dt").stream()
                .filter(e -> (busan ? "첨부파일" : "첨부").equals(e.text().replaceAll("[\\s\\u00a0:：]+", ""))).toList();
        if (labels.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        Element label = labels.getFirst(), container = label.nextElementSibling();
        if (container == null || !"dd".equals(container.tagName()) || (!busan && !container.hasClass("item")))
            return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if (container.nextElementSibling() != null && (!busan || !"dt".equals(container.nextElementSibling().tagName())))
            return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        if (busan && container.select("ul.attfiles").size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var files = new LinkedHashMap<String, Descriptor>();
        boolean unresolved = !container.select("button,input,select,form,iframe,object,embed,script,[onclick],[onload]").isEmpty();
        boolean exceeded = false;
        Element residual = container.clone(); residual.select("a").remove();
        if (!residual.text().isBlank() || !residual.select("img,[href]").isEmpty()) unresolved = true;
        for (Element anchor : container.select("a")) {
            try {
                if (anchor.hasAttr("onclick") || (busan && anchor.parents().stream().noneMatch(p -> p.hasClass("attfiles")))
                        || (!busan && !anchor.hasClass("file"))) throw new IllegalArgumentException();
                URI fetch = selectDownloadUri(detail, anchor.attr("href"));
                Map<String, String> values = selectParameters(fetch.getRawQuery());
                String identity = busan ? normalizer.hash(values.get("fileId") + "\n" + values.get("seq"))
                        : normalizer.hash(values.get("file_path") + "\n" + values.get("sys_file_nm"));
                String name = busan ? anchor.attr("title").strip() : values.get("user_file_nm");
                Descriptor previous = files.get(identity);
                // 부산의 같은 li에 있는 '다운로드' 보조 링크만 이름 없는 중복으로 허용한다.
                boolean duplicateButton = busan && "다운로드".equals(anchor.text().strip()) && anchor.hasClass("btnTypeS")
                        && anchor.parent() != null && "li".equals(anchor.parent().tagName())
                        && anchor.parent().select("a").stream().anyMatch(a -> a != anchor && previous != null
                            && previous.displayName().equals(a.attr("title").strip()));
                if (previous != null) {
                    if (!previous.fetchUri().equals(fetch) || (!previous.displayName().equals(name) && !duplicateButton)) unresolved = true;
                    continue;
                }
                if (duplicateButton || !selectSafeName(name)) throw new IllegalArgumentException();
                if (files.size() == 10) { exceeded = true; continue; }
                String format = selectFormat(name);
                boolean supported = format != null && (busan || format.equals(selectFormat(values.get("sys_file_nm"))));
                String noticeId = selectParameters(detail.getRawQuery()).get(busan ? "sno" : "nttId");
                var locator = new AttachmentSetEvidence.Locator(code, downloadPath, Map.of("attachmentId", identity, "noticeId", noticeId));
                files.put(identity, new Descriptor(fetch, locator, name, supported ? format : null, "UNKNOWN", supported));
            } catch (IllegalArgumentException exception) { unresolved = true; }
        }
        if (exceeded) return new Result("LIMIT_EXCEEDED", false, List.copyOf(files.values()), List.of("ATTACHMENT_FILE_LIMIT"));
        if (unresolved) return new Result("FAILED", false, List.copyOf(files.values()), List.of("ATTACHMENT_LINK_UNRESOLVED"));
        return new Result(files.isEmpty() ? "NO_FILES" : "FOUND", true, List.copyOf(files.values()), List.of());
    }
    private URI selectDownloadUri(URI detail, String href) {
        if (href == null || href.isBlank() || href.length() > 8192 || href.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
        // HTML의 공백 포함 한글 query를 단 한 번 해석·인코딩한다. DOM base 태그는 신뢰하지 않는다.
        URI parsed = detail.resolve(URI.create(href.replace(" ", "%20")));
        if (!selectApprovedRequest(parsed) || !downloadHost.equals(parsed.getHost()) || !downloadPath.equals(parsed.getPath())) throw new IllegalArgumentException();
        var query = new StringJoiner("&");
        selectParameters(parsed.getRawQuery()).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e ->
                query.add(e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8).replace("+", "%20")));
        return URI.create("https://" + downloadHost + downloadPath + "?" + query);
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
    private boolean selectSafeName(String name) {
        return name != null && !name.isBlank() && name.length() <= 500 && !name.contains("/") && !name.contains("\\")
                && !name.contains("..") && !name.contains("%") && name.indexOf('\ufffd') < 0 && name.codePoints().noneMatch(Character::isISOControl);
    }
    private String selectFormat(String name) {
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT) : "";
        return Set.of("PDF", "HWP", "HWPX").contains(suffix) ? suffix : null;
    }
    private Result selectFailed(String warning) { return new Result("FAILED", false, List.of(), List.of(warning)); }
}
