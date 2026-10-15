package com.saneb.domain.announcementsource.provider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/** 원문 본문의 문단 경계를 보존하는 일반 텍스트 추출. HTML 자체는 저장하지 않는다. */
public final class AnnouncementBodyTextExtractor {
    private AnnouncementBodyTextExtractor() { }

    public static String selectText(String html) {
        return html == null ? null : selectText(Jsoup.parseBodyFragment(html).body());
    }

    public static String selectText(Element content) {
        Element copy = content.clone();
        copy.select("script, style, noscript, template, iframe, object, embed").remove();
        StringBuilder text = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                if (node instanceof TextNode value) {
                    text.append(value.getWholeText().replace("\r\n", "\n").replace('\r', '\n'));
                } else if (node instanceof Element element) {
                    if ("br".equals(element.normalName())) text.append('\n');
                    else if (!isTableCell(element) && element.isBlock()) appendBoundary(text, "p".equals(element.normalName()) ? 2 : 1);
                }
            }

            @Override public void tail(Node node, int depth) {
                if (node instanceof Element element) {
                    if (isTableCell(element)) text.append(' ');
                    else if (element.isBlock()) appendBoundary(text, "p".equals(element.normalName()) ? 2 : 1);
                }
            }
        }, copy);
        return text.toString().replace('\u00a0', ' ')
                .replaceAll("[\\h\\x0B\\f]+", " ")
                .replaceAll(" *\n *", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }

    private static boolean isTableCell(Element element) {
        return "td".equals(element.normalName()) || "th".equals(element.normalName());
    }

    private static void appendBoundary(StringBuilder text, int required) {
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\t')) end--;
        text.setLength(end);
        if (end == 0) return;
        int count = 0;
        while (end > 0 && text.charAt(--end) == '\n') count++;
        for (int i = count; i < required; i++) text.append('\n');
    }
}
