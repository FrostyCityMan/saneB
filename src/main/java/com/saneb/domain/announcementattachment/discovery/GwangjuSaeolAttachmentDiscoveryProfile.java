package com.saneb.domain.announcementattachment.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

/** 광주 동구/북구의 실측 공식 첨부 칸을 공통 새올 GET 엔진에 연결한다. */
final class GwangjuSaeolAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    enum Layout { DONGGU, BUKGU }
    private final Layout layout;
    private final SaeolGetAttachmentDiscoveryProfile delegate;
    private final String hash;
    GwangjuSaeolAttachmentDiscoveryProfile(Layout layout) {
        this.layout = java.util.Objects.requireNonNull(layout); boolean dong = layout == Layout.DONGGU;
        delegate = new SaeolGetAttachmentDiscoveryProfile("LOCAL_GWANGJU_" + layout + "_GET_V1",
                dong ? "LGS-000066" : "LGS-000069", dong ? "eminwon.donggu.gwangju.kr" : "eminwon.bukgu.gwangju.kr",
                dong ? "SAFE_SAEOL_EMINWON_LIST" : "SAFE_SAEOL_EMINWON", "th", false);
        hash = AttachmentProfileFingerprint.selectHash(delegate.selectProfileHash() + "|1|" + layout
                + "|whole-official-container|paired-static-icon|same-request|no-script", getClass());
    }
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
        var page = Jsoup.parse(html, uri.toASCIIString()); var forms = page.select("form"); boolean dong = layout == Layout.DONGGU;
        if (forms.size() != 1) return selectFailed(); var form = forms.getFirst();
        if (!(dong ? "form1" : "form").equals(form.attr("name")) || !"post".equalsIgnoreCase(form.attr("method"))) return selectFailed();
        Element container;
        if (dong) {
            var views = form.select("div.tstyle_view"); var titles = form.select("div.tstyle_view > div.title");
            var areas = form.select("div.tstyle_view > div.add_file");
            if (views.size() != 1 || titles.size() != 1 || titles.getFirst().text().isBlank() || areas.size() != 1) return selectFailed();
            var area = areas.getFirst();
            if (area.childrenSize() != 2 || !"strong".equals(area.child(0).tagName()) || !"첨부파일".equals(area.child(0).text().strip())
                    || !"div".equals(area.child(1).tagName()) || !area.ownText().isBlank() || selectHasEvent(area) || selectHasEvent(area.child(0)) || selectHasEvent(area.child(1))) return selectFailed();
            container = area.child(1).clone();
        } else {
            var views = form.select("div.board_read_wrap > div.board_read"); var titles = form.select("div.board_read > h3.title");
            var labels = form.select("div.board_read > dl.info_basic > dt").stream().filter(e -> "첨부파일".equals(e.text().replaceAll("[\\s\\u00a0:：]+", ""))).toList();
            if (views.size() != 1 || titles.size() != 1 || titles.getFirst().text().isBlank() || labels.size() != 1) return selectFailed();
            var label = labels.getFirst(); var dl = label.parent();
            if (dl.child(0) != label || !label.children().isEmpty() || !dl.ownText().isBlank() || selectHasEvent(dl) || selectHasEvent(label)) return selectFailed();
            container = new Element("div");
            for (var cell : dl.children()) {
                if (cell == label) continue;
                if (!"dd".equals(cell.tagName()) || selectHasEvent(cell)) return selectFailed();
                var copy = cell.clone();
                for (var icon : copy.select("img")) {
                    var anchor = icon.nextElementSibling();
                    if ("http://bukgu.gwangju.kr/upload/skin/board/basic/ico_file.gif".equals(icon.attr("src")) && !selectHasEvent(icon)
                            && anchor != null && "a".equals(anchor.tagName()) && ("첨부파일 " + anchor.text() + " 다운로드").equals(icon.attr("alt"))) icon.remove();
                }
                container.appendChild(copy);
            }
        }
        // 첨부 칸 전체를 전달한다. 미확인 링크/요소는 오류로 남기고 확인한 파일은 보존한다.
        var standard = new Element("form").attr("name", "form1").attr("method", "post"); var row = standard.appendElement("table").appendElement("tr");
        row.appendElement("th").text("첨부파일"); row.appendElement("td").html(container.html());
        return delegate.selectDescriptors(source, standard.outerHtml());
    }
    private boolean selectHasEvent(Element e) { return e.attributes().asList().stream().anyMatch(a -> a.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on")); }
    private Result selectFailed() { return new Result("FAILED", false, List.of(), List.of("ATTACHMENT_SELECTOR_CHANGED")); }
}
