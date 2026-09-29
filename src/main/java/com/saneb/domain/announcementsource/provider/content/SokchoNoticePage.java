package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 속초 대표 홈페이지의 공식 제목·본문·첨부 셀만 선택한다. */
public final class SokchoNoticePage {
    public static final String HOST="www.sokcho.go.kr",PATH="/sc/portal/sokchonews/notification";
    private SokchoNoticePage() { }
    public static boolean selectSite(URI u){return u!=null&&HOST.equals(u.getHost())&&PATH.equals(u.getPath());}
    public static URI selectDetailUri(URI u){
        if(!selectSite(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!PATH.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!q.keySet().equals(Set.of("notAncmtMgtNo"))||!q.get("notAncmtMgtNo").matches("[1-9][0-9]{0,14}"))throw invalid();
        return u;
    }
    public static Element selectRoot(Document page){var root=selectSingle(page,"#content-bx .skinTb.skinTb-data-resList.skinTb-data-bgSbj");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    public static Element selectTitle(Element root){var title=selectSingle(root,":root > .skinTb-tr > .skinTb-sbj");var previous=title.previousElementSibling();if(previous==null||!previous.hasClass("skinTb-th")||!"제목".equals(previous.text().strip()))throw invalid();return title;}
    public static Element selectContent(Document page){return selectSingle(selectRoot(page),":root > .skinTb-tr > .skinTb-conts");}
    public static Element selectAttachments(Document page){var root=selectRoot(page);var labels=root.select(":root > .skinTb-tr > .skinTb-th").stream().filter(e->"첨부파일".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!cell.hasClass("skinTb-td")||cell.nextElementSibling()!=null)throw invalid();return cell;}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("SOKCHO_STRUCTURE_CHANGED");}
}
