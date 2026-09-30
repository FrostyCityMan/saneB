package com.saneb.domain.announcementattachment.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/** 공식 파일 위젯의 다섯 JSON 문자열만 읽는다. 스크립트·계산식은 실행하지 않는다. */
final class GyeongbukFileManifest {
    private static final String CALL = "boardView.viewFile.putFileHtml";
    private static final ObjectMapper JSON = new ObjectMapper();
    record Result(List<List<String>> records,boolean incomplete) { }
    private record Parsed(List<String> values,int next) { }
    private GyeongbukFileManifest() { }
    static Result selectRecords(String script) {
        if (script == null || script.length() > 1_000_000) return new Result(List.of(),true);
        var records = new ArrayList<List<String>>(); boolean incomplete = false, statementStart = true;
        for (int index = 0; index < script.length();) {
            char ch = script.charAt(index);
            if (ch == '\'' || ch == '"' || ch == '`') { index = selectAfterQuoted(script,index,ch); statementStart = false; continue; }
            if (script.startsWith("//",index)) { int end = script.indexOf('\n',index + 2); index = end < 0 ? script.length() : end + 1; continue; }
            if (script.startsWith("/*",index)) { int end = script.indexOf("*/",index + 2); if (end < 0) { incomplete = true; break; } index = end + 2; continue; }
            if (script.startsWith(CALL,index) && statementStart) {
                Parsed parsed = selectCall(script,index + CALL.length());
                if (parsed.values() == null) incomplete = true; else records.add(parsed.values());
                index = Math.max(index + CALL.length(),parsed.next()); statementStart = parsed.values() != null; continue;
            }
            if (!Character.isWhitespace(ch)) statementStart = ";{}".indexOf(ch) >= 0;
            index++;
        }
        return new Result(List.copyOf(records),incomplete);
    }
    private static Parsed selectCall(String source,int index) {
        int start = index; index = selectAfterWhitespace(source,index);
        if (index >= source.length() || source.charAt(index++) != '(') return new Parsed(null,index);
        var values = new ArrayList<String>(5);
        for (int argument = 0; argument < 5; argument++) {
            index = selectAfterWhitespace(source,index);
            if (index >= source.length() || source.charAt(index) != '"') return new Parsed(null,index);
            int quoted = index++; boolean closed = false;
            while (index < source.length() && index - start <= 8192 && index - quoted <= 4096) {
                char ch = source.charAt(index++);
                if (Character.isISOControl(ch)) return new Parsed(null,index);
                if (ch == '\\') { if (index >= source.length()) return new Parsed(null,index); index++; }
                else if (ch == '"') { closed = true; break; }
            }
            if (!closed) return new Parsed(null,index);
            try { var value = JSON.readTree(source.substring(quoted,index)); if (!value.isTextual() || value.textValue().length() > 2048) return new Parsed(null,index); values.add(value.textValue()); }
            catch (java.io.IOException exception) { return new Parsed(null,index); }
            index = selectAfterWhitespace(source,index);
            if (index >= source.length() || source.charAt(index++) != (argument == 4 ? ')' : ',')) return new Parsed(null,index);
        }
        index = selectAfterWhitespace(source,index);
        if (index >= source.length() || source.charAt(index++) != ';') return new Parsed(null,index);
        return new Parsed(List.copyOf(values),index);
    }
    private static int selectAfterWhitespace(String value,int index) { while (index < value.length() && Character.isWhitespace(value.charAt(index))) index++; return index; }
    private static int selectAfterQuoted(String value,int index,char quote) {
        index++;
        while (index < value.length()) { char ch = value.charAt(index++); if (ch == '\\' && index < value.length()) index++; else if (ch == quote) break; }
        return index;
    }
}
