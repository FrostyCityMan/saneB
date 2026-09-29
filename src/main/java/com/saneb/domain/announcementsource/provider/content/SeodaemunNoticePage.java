package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 서대문 고시공고 boardWrite의 제목·본문·첨부 셀을 분리한다. */
public final class SeodaemunNoticePage {
    public static final String HOST="www.sdm.go.kr",PATH="/news/notice/notice.do";
    private SeodaemunNoticePage() { }
    public static boolean selectSite(URI u){return u!=null&&HOST.equals(u.getHost())&&PATH.equals(u.getPath());}
    public static URI selectDetailUri(URI u){if(!selectSite(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!PATH.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!q.keySet().equals(Set.of("sdmBoardConfSeq","mode","sdmBoardSeq"))||!"82".equals(q.get("sdmBoardConfSeq"))||!"view".equals(q.get("mode"))||!q.get("sdmBoardSeq").matches("[1-9][0-9]{0,14}"))throw invalid();return u;}
    public static Element selectRoot(Document page){var root=selectSingle(page,"table.boardWrite");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){return selectSingle(root,":root > tbody > tr > td.subject");}
    public static Element selectContent(Document page){return selectSingle(selectRoot(page),":root > tbody > tr > td#viewCon.viewCon[colspan=4]");}
    public static Element selectAttachments(Document page){var root=selectRoot(page);var labels=root.select(":root > tbody > tr > th[scope=row]").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!"td".equals(cell.tagName())||!"3".equals(cell.attr("colspan"))||cell.nextElementSibling()!=null)throw invalid();return cell;}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SEODAEMUN_STRUCTURE_CHANGED");}
}
