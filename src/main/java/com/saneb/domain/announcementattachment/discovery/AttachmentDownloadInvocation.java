package com.saneb.domain.announcementattachment.discovery;

import java.util.ArrayList;
import java.util.List;

/** 고정 다운로드 호출의 세 문자열만 선형으로 읽는다. JavaScript 실행·재귀 regex·임의 escape는 없다. */
public final class AttachmentDownloadInvocation {
    private AttachmentDownloadInvocation() { }

    public static List<String> selectArguments(String input, String function, boolean allowBareCall, boolean allowReturnFalse) {
        if (input == null || input.length() > 8192 || !("goDownLoad".equals(function) || "fn_egov_downFile".equals(function))) return List.of();
        int index = selectAfterWhitespace(input, 0);
        if (input.startsWith("javascript:", index)) index = selectAfterWhitespace(input, index + 11);
        else if (!allowBareCall) return List.of();
        if (!input.startsWith(function, index)) return List.of();
        index = selectAfterWhitespace(input, index + function.length());
        if (index >= input.length() || input.charAt(index++) != '(') return List.of();
        var arguments = new ArrayList<String>(3);
        for (int argument = 0; argument < 3; argument++) {
            index = selectAfterWhitespace(input, index);
            if (index >= input.length() || input.charAt(index++) != '\'') return List.of();
            var value = new StringBuilder(); boolean closed = false;
            while (index < input.length()) {
                char selected = input.charAt(index++);
                if (selected == '\'') { closed = true; break; }
                if (selected == '\\') {
                    if (index >= input.length()) return List.of();
                    selected = input.charAt(index++);
                    if (selected != '\\' && selected != '\'') return List.of();
                }
                if (Character.isISOControl(selected) || value.length() == 2048) return List.of();
                value.append(selected);
            }
            if (!closed) return List.of();
            arguments.add(value.toString()); index = selectAfterWhitespace(input, index);
            if (index >= input.length() || input.charAt(index++) != (argument == 2 ? ')' : ',')) return List.of();
        }
        index = selectAfterWhitespace(input, index);
        if (index < input.length() && input.charAt(index) == ';') index = selectAfterWhitespace(input, index + 1);
        if (index == input.length()) return List.copyOf(arguments);
        if (!allowReturnFalse || !input.startsWith("return", index)) return List.of();
        int afterReturn = index + 6; index = selectAfterWhitespace(input, afterReturn);
        if (index == afterReturn || !input.startsWith("false", index)) return List.of();
        index = selectAfterWhitespace(input, index + 5);
        if (index < input.length() && input.charAt(index) == ';') index = selectAfterWhitespace(input, index + 1);
        return index == input.length() ? List.copyOf(arguments) : List.of();
    }
    private static int selectAfterWhitespace(String input, int index) {
        while (index < input.length() && " \t\r\n".indexOf(input.charAt(index)) >= 0) index++;
        return index;
    }
}
