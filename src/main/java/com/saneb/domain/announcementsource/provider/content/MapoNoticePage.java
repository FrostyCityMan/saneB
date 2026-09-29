package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 마포 공식 고시공고의 제목·본문·첨부 목록을 분리한다. 주석 처리된 과거 첨부 표는 대상이 아니다. */
public final class MapoNoticePage {
    public static final String HOST="www.mapo.go.kr",PATH="/site/main/nPortal/detail";
    private MapoNoticePage() { }
    public static boolean selectSite(URI u){return u!=null&&HOST.equals(u.getHost())&&PATH.equals(u.getPath());}
    public static URI selectDetailUri(URI u){
        if(!selectSite(u)||!"https".equals(u.getScheme())||(u.getPort()!=-1&&u.getPort()!=443)||u.getUserInfo()!=null||u.getFragment()!=null||!PATH.equals(u.getRawPath())||!u.equals(u.normalize()))throw invalid();
        var q=CapitalThirdNoticePage.selectParameters(u.getRawQuery());if(!q.keySet().equals(Set.of("bcId"))||!q.get("bcId").matches("[1-9][0-9]{0,14}"))throw invalid();return u;
    }
    public static Element selectRoot(Document page){var root=selectSingle(page,"div.bbs_view");if(selectTitle(root).text().isBlank())throw invalid();return root;}
    private static Element selectTable(Element root){return selectSingle(root,":root > .bbs_view_body > .tbl_wrap3 > table");}
    public static Element selectTitle(Element root){var table=selectTable(root);var labels=table.select(":root > tbody > tr > th").stream().filter(e->"제목".equals(e.text().strip())).toList();if(labels.size()!=1)throw invalid();var cell=labels.getFirst().nextElementSibling();if(cell==null||!"td".equals(cell.tagName())||cell.nextElementSibling()!=null)throw invalid();return cell;}
    public static Element selectContent(Document page){return selectSingle(selectTable(selectRoot(page)),":root > tbody > tr > td[colspan=4]");}
    public static Element selectAttachments(Document page){var files=selectSingle(selectRoot(page),":root > .bbs_view_file");if(!"첨부파일".equals(selectSingle(files,":root > strong.file_tit").text().strip()))throw invalid();return selectSingle(files,":root > ul.bbs_view_files_wrap");}
    private static Element selectSingle(Element root,String selector){var found=root.select(selector);if(found.size()!=1)throw invalid();return found.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("MAPO_STRUCTURE_CHANGED");}
}
