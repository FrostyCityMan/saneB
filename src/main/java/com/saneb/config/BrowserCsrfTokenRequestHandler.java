package com.saneb.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/** 쿠키 기반 API 헤더와 Thymeleaf의 인코딩된 폼 토큰을 각각 검증하도록 해석한다. */
final class BrowserCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestHandler headerHandler = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler formHandler = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
        // HTML에 노출하는 값은 요청마다 마스킹하여 BREACH 보호를 유지한다.
        formHandler.handle(request, response, token);
        // 로그인·로그아웃 뒤 새 화면에서도 브라우저가 사용할 쿠키를 발급한다.
        token.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
        // 값의 일치 여부는 CsrfFilter가 검증한다. 예외 경로나 권한을 추가하지 않는다.
        CsrfTokenRequestHandler handler = StringUtils.hasText(request.getHeader(token.getHeaderName()))
                ? headerHandler : formHandler;
        return handler.resolveCsrfTokenValue(request, token);
    }
}
