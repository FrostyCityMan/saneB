package com.saneb.domain.announcementsource.provider.content;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 부산 공식 목록의 검색 문맥만 허용한다. 원문 URL과 저장된 공고 식별자는 바꾸지 않는다. */
public final class BusanLegalNoticePage {
    private static final Set<String> KEYS=Set.of("sno","gosiGbn","curPage","conIfmStdt","conIfmEnddt","conGosiGbn","schKeyType","srchText");
    private BusanLegalNoticePage() { }

    public static Map<String,String> selectDetailParameters(URI uri) {
        if(uri==null || uri.toASCIIString().length()>4096 || !"https".equals(uri.getScheme())
                || !"www.busan.go.kr".equals(uri.getHost()) || uri.getPort()!=-1 && uri.getPort()!=443
                || uri.getUserInfo()!=null || uri.getFragment()!=null || !uri.equals(uri.normalize())
                || !"/nbgosi/view".equals(uri.getRawPath()) || uri.getRawQuery()==null)
            throw new IllegalArgumentException("BUSAN_DETAIL_INVALID");
        var values=new LinkedHashMap<String,String>();
        for(String pair:uri.getRawQuery().split("&",-1)) {
            String[] parts=pair.split("=",-1);
            if(parts.length!=2 || !KEYS.contains(parts[0])) throw new IllegalArgumentException("BUSAN_DETAIL_INVALID");
            String value=URLDecoder.decode(parts[1],StandardCharsets.UTF_8);
            if(value.indexOf('\ufffd')>=0 || value.codePoints().anyMatch(Character::isISOControl)
                    || values.putIfAbsent(parts[0],value)!=null) throw new IllegalArgumentException("BUSAN_DETAIL_INVALID");
        }
        if(!values.getOrDefault("sno","").matches("[0-9]{1,15}") || !values.getOrDefault("gosiGbn","").matches("[A-Z]")
                || values.containsKey("curPage") && !values.get("curPage").matches("[1-9][0-9]{0,6}")
                || values.containsKey("conGosiGbn") && !Set.of("","N","A","P","E").contains(values.get("conGosiGbn"))
                || values.containsKey("schKeyType") && !Set.of("A","B","C").contains(values.get("schKeyType"))
                || values.getOrDefault("srchText","").length()>256)
            throw new IllegalArgumentException("BUSAN_DETAIL_INVALID");
        for(String key:Set.of("conIfmStdt","conIfmEnddt")) {
            String value=values.get(key);
            if(value==null || value.isEmpty()) continue;
            try {
                if(!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new DateTimeParseException("INVALID_DATE",value,0);
                LocalDate.parse(value);
            } catch(DateTimeParseException invalid) { throw new IllegalArgumentException("BUSAN_DETAIL_INVALID"); }
        }
        return Map.copyOf(values);
    }

    public static boolean selectApprovedDetail(URI uri) {
        try { selectDetailParameters(uri);return true; }
        catch(IllegalArgumentException invalid) { return false; }
    }

    public static boolean selectSameNotice(URI initial,URI next) {
        try {
            var first=selectDetailParameters(initial);var second=selectDetailParameters(next);
            return first.get("sno").equals(second.get("sno")) && first.get("gosiGbn").equals(second.get("gosiGbn"));
        } catch(IllegalArgumentException invalid) { return false; }
    }
}
