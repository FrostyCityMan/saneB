package com.saneb.common.util;

/** 사람의 내부 식별자 대신 성함과 로그인 아이디를 표시합니다. */
public final class PersonDisplayName {
    private PersonDisplayName() { }

    public static String format(String name, String loginId) {
        String displayName = name == null ? "" : name.trim();
        String login = loginId == null ? "" : loginId.trim();
        if (!displayName.isEmpty() && !login.isEmpty()) return displayName + " (" + login + ")";
        if (!displayName.isEmpty()) return displayName;
        return login.isEmpty() ? "계정 정보 확인 필요" : login;
    }
}
