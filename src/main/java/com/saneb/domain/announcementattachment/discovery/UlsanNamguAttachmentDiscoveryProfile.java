package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.provider.content.UlsanNamguNoticePage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 고시공고의 div/ul 영역만 기존 새올 GET 엔진에 연결한다. 보조 게시판으로 대체하지 않는다. */
public final class UlsanNamguAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate = new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_ULSAN_NAMGU_GET_V1", "LGS-000079", "eminwon.ulsannamgu.go.kr", "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    private final String hash = new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"
            +AttachmentProfileFingerprint.selectHash("ULSAN_NAMGU:1|form1-post|bbs_detail_basic|first-of-two-bbs_detail_content2|whole-list|no-empty-proof|same-uri-redirect",
                    getClass())+"|"+AttachmentProfileFingerprint.selectHash("ULSAN_NAMGU_DOM:1",UlsanNamguNoticePage.class));

    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public boolean selectApprovedRequest(URI uri) { return delegate.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request next) {
        return selectApprovedRequest(initial)&&selectApprovedRequest(next)&&initial.uri().equals(next.uri());
    }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(Source source,String html) {
        URI detail = selectDetailUri(source);
        if (html==null || html.length()>1_000_000) return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_DETAIL_UNAVAILABLE"));
        Element container;
        try { container = UlsanNamguNoticePage.selectAttachments(UlsanNamguNoticePage.selectRoot(Jsoup.parse(html,detail.toASCIIString()))); }
        catch (IllegalArgumentException failure) { return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED")); }
        // 공식 li/a는 href 대신 onclick을 사용한다. 고정 세 문자열 호출만 정적으로 변환한다.
        // 다른 href는 따라가지 않는다. 고정 함수의 안전한 후보와 미해석 href 경고를 분리한다.
        container = container.clone();
        boolean unresolvedHref = false;
        for (Element anchor : container.select("a[onclick]")) {
            var arguments = AttachmentDownloadInvocation.selectArguments(anchor.attr("onclick"), "goDownLoad", true, true);
            if (arguments.size() != 3) continue;
            String inertHref = anchor.attr("href").replaceAll("[ \\t\\r\\n]", "");
            if (!Set.of("", "#", "#none", "javascript:;", "javascript:", "javascript:void(0);", "javascript:void(0)").contains(inertHref)) unresolvedHref = true;
            String call = arguments.stream().map(value -> "'"+value.replace("\\", "\\\\").replace("'", "\\'")+"'")
                    .collect(java.util.stream.Collectors.joining(","));
            anchor.attr("href", "javascript:goDownLoad("+call+")").removeAttr("onclick");
        }
        var standard = new Element("form").attr("name","form1").attr("method","post");
        var row = standard.appendElement("table").appendElement("tr");
        row.appendElement("th").text("첨부파일");
        row.appendElement("td").html(container.html());
        var result = delegate.selectDescriptors(source,standard.outerHtml());
        if (!unresolvedHref) return result;
        var warnings = new java.util.LinkedHashSet<>(result.warnings());
        warnings.add("ATTACHMENT_LINK_UNRESOLVED");
        return new Result("LIMIT_EXCEEDED".equals(result.status()) ? result.status() : "FAILED",false,result.descriptors(),List.copyOf(warnings));
    }
}
