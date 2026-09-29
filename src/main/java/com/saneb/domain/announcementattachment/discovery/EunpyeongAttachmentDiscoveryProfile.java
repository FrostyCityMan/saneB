package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 은평 공식 board2의 중첩 첨부 칸 전체를 공통 새올 GET 엔진에 전달한다. */
final class EunpyeongAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate = new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_EUNPYEONG_GET_V1", "LGS-000013", "eminwon.ep.go.kr", "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    private final String hash = AttachmentProfileFingerprint.selectHash(delegate.selectProfileHash()
            + "|1|form1-post|board2-title|nested-font-label|whole-container|same-request", getClass());
    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public boolean selectApprovedRequest(URI uri) { return delegate.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(Request initial, Request next) { return initial != null && initial.equals(next) && selectApprovedRequest(next); }
    @Override public Result selectDescriptors(String id, String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(Source source, String html) {
        URI uri = selectDetailUri(source);
        if (html == null || html.length() > 1_000_000) return new Result("FAILED", false, List.of(), List.of("ATTACHMENT_DETAIL_UNAVAILABLE"));
        var page = Jsoup.parse(html, uri.toASCIIString()); var forms = page.select("form[name=form1][method=post]");
        if (forms.size() != 1) return selectFailed();
        var tables = forms.getFirst().select("table.board2"); if (tables.size() != 1) return selectFailed(); var table = tables.getFirst();
        var titles = table.select("th").stream().filter(e -> e.closest("table") == table && "제목".equals(e.text().strip())).toList();
        if (titles.size() != 1) return selectFailed(); var title = titles.getFirst().nextElementSibling();
        if (title == null || !"td".equals(title.tagName()) || title.text().isBlank()) return selectFailed();
        var labels = table.select("td > font").stream().filter(e -> "첨부파일".equals(e.text().replaceAll("[\\s\\u00a0:：]+", ""))).toList();
        if (labels.size() != 1) return selectFailed(); var label = labels.getFirst(); var cell = label.parent(); var row = cell.parent();
        var nested = cell.closest("table"); var files = cell.nextElementSibling();
        if (!label.children().isEmpty() || cell.childrenSize() != 1 || !cell.ownText().isBlank()
                || nested == table || nested == null || nested.parent().closest("table") != table
                || !"tr".equals(row.tagName()) || row.childrenSize() != 2 || row.child(0) != cell
                || files == null || !"td".equals(files.tagName()) || selectHasEvent(label) || selectHasEvent(cell)
                || selectHasEvent(row) || selectHasEvent(files)) return selectFailed();
        // 사용자 제공 JS는 실행하지 않으며 첨부 칸의 미확인 요소도 제거하지 않는다.
        var standard = new Element("form").attr("name", "form1").attr("method", "post");
        var standardRow = standard.appendElement("table").appendElement("tr");
        standardRow.appendElement("th").text("첨부파일"); standardRow.appendElement("td").html(files.html());
        return delegate.selectDescriptors(source, standard.outerHtml());
    }
    private boolean selectHasEvent(Element e) { return e.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on")); }
    private Result selectFailed() { return new Result("FAILED", false, List.of(), List.of("ATTACHMENT_SELECTOR_CHANGED")); }
}
