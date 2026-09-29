package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 용인 공식 상세의 첨부 dl만 공통 GET 검증기에 전달한다. 본문 링크는 수집하지 않는다. */
final class YonginGetAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate = new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_YONGIN_GET_V1", "LGS-000086", "eminwon.yongin.go.kr", "SAFE_SAEOL_EMINWON_CELL", "td", false);
    private final String hash = AttachmentProfileFingerprint.selectHash(delegate.selectProfileHash()
            + "|1|form1-post-contentDiv-boardDefalutView-dl-dt-dd|same-request|all-child-content", getClass());
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public boolean selectApprovedRequest(URI uri) { return delegate.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(Request initial, Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(String id, String html) { return delegate.selectDescriptors(id, html); }
    @Override public Result selectDescriptors(Source source, String html) {
        URI detail = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return selectFailed("ATTACHMENT_DETAIL_UNAVAILABLE");
        var page = Jsoup.parse(html, detail.toASCIIString());
        var forms = page.select("form[name=form1][method=post]");
        var roots = page.select("form[name=form1][method=post] div#contentDiv.boardGroup > div.boardDefalutView");
        if (forms.size() != 1 || roots.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var labels = roots.getFirst().select(":root > dl > dt").stream()
                .filter(e -> "첨부파일".equals(e.text().strip())).toList();
        if (labels.size() != 1) return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var label = labels.getFirst(); var cell = label.nextElementSibling();
        if (!label.children().isEmpty() || cell == null || !"dd".equals(cell.tagName()) || cell.nextElementSibling() != null
                || label.parent().childrenSize() != 2 || label.parent().child(0) != label || !label.parent().ownText().isBlank())
            return selectFailed("ATTACHMENT_SELECTOR_CHANGED");
        var standard = new Element("form").attr("name", "form1").attr("method", "post");
        var row = standard.appendElement("table").appendElement("tr");
        row.appendElement("td").text("첨부파일"); row.appendElement("td").html(cell.html());
        return delegate.selectDescriptors(source, standard.outerHtml());
    }
    private Result selectFailed(String code) { return new Result("FAILED", false, List.of(), List.of(code)); }
}
