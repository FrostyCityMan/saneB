package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 남해 공식 모듈의 고정 상세 주소. 일회성 요청 값은 저장 identity에 포함하지 않는다. */
public final class NamhaeNoticePage {
    public static final String HOST="www.namhae.go.kr",PATH="/modules/saeol/gosi.do";
    private static final Set<String> FIELDS=Set.of("amode","not_ancmt_mgt_no","scd","pageCd","siteGubun","cpage","stype","sstring");
    private static final Set<String> EPHEMERAL=Set.of("_csrf_parameterName","_csrf_token");
    private NamhaeNoticePage() { }
    public static boolean selectMatches(URI uri){return uri!=null&&HOST.equals(uri.getHost())&&PATH.equals(uri.getPath());}
    public static URI selectCanonicalDetail(URI uri,boolean stripEphemeral){
        if(!selectMatches(uri)||!"https".equals(uri.getScheme())||(uri.getPort()!=-1&&uri.getPort()!=443)
                ||uri.getUserInfo()!=null||uri.getFragment()!=null||!uri.equals(uri.normalize())||!uri.getRawPath().equals(uri.getPath())
                ||uri.getRawQuery()==null||uri.getRawQuery().length()>4096)throw invalid();
        var fields=new LinkedHashMap<String,String>();
        try{for(String pair:uri.getRawQuery().split("&",-1)){
            String[] p=pair.split("=",2);if(p.length!=2||!p[0].matches("[A-Za-z_]+"))throw invalid();
            String value=URLDecoder.decode(p[1],StandardCharsets.UTF_8);
            if(value.length()>1024||value.codePoints().anyMatch(Character::isISOControl)||fields.putIfAbsent(p[0],value)!=null
                    ||!FIELDS.contains(p[0])&&!(stripEphemeral&&EPHEMERAL.contains(p[0])))throw invalid();
        }}catch(IllegalArgumentException exception){throw invalid();}
        if(!"_view".equals(fields.get("amode"))||!fields.getOrDefault("not_ancmt_mgt_no","").matches("[0-9]{1,15}")
                ||!"01".equals(fields.get("scd"))||!"SM010110000".equals(fields.get("pageCd"))||!"socialm".equals(fields.get("siteGubun")))throw invalid();
        return URI.create("https://"+HOST+PATH+"?amode=_view&not_ancmt_mgt_no="+fields.get("not_ancmt_mgt_no")+"&scd=01&pageCd=SM010110000&siteGubun=socialm");
    }
    public static Element selectRoot(Document page){var roots=page.select("form#saeolGosiVO > div.bbs1view1");if(roots.size()!=1)throw invalid();var root=roots.getFirst();selectTitle(root);return root;}
    public static Element selectTitle(Element root){var titles=root.select(":root > h1.h1");if(titles.size()!=1||titles.getFirst().text().isBlank())throw invalid();return titles.getFirst();}
    public static Element selectContent(Document page){var bodies=selectRoot(page).select(":root > div.substance");if(bodies.size()!=1)throw invalid();return bodies.getFirst();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("NAMHAE_STRUCTURE_CHANGED");}
}
