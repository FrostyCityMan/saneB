/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeUrlValidator.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.support;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LocalGovernmentNoticeUrlValidator {

    /**
     * 운영자가 입력한 URL의 형식과 DNS 해석 결과를 검증합니다.
     *
     * @param value 검증할 URL
     * @return 정규 URI
     */
    public URI validate(String value) {
        URI uri = parse(value);
        validateUriParts(uri);
        validateResolvedAddresses(uri.getHost());
        return uri;
    }

    /**
     * 문자열 URL을 URI로 변환합니다.
     *
     * @param value URL 문자열
     * @return URI
     */
    private URI parse(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("공고 URL을 입력하세요.");
        }
        try {
            return new URI(value.trim());
        } catch (URISyntaxException exception) {
            throw invalid("공고 URL 형식이 올바르지 않습니다.");
        }
    }

    /**
     * URI 스킴, 호스트, 인증정보와 포트를 검증합니다.
     *
     * @param uri 검증할 URI
     */
    private void validateUriParts(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw invalid("공고 URL은 http 또는 https 주소만 사용할 수 있습니다.");
        }
        if (uri.getUserInfo() != null) {
            throw invalid("아이디나 비밀번호가 포함된 URL은 사용할 수 없습니다.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw invalid("공고 URL의 인터넷 주소를 확인하세요.");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if ("localhost".equals(host) || host.endsWith(".localhost") || host.endsWith(".local")) {
            throw invalid("내부 네트워크 주소는 공고 URL로 사용할 수 없습니다.");
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw invalid("공고 URL은 기본 웹 포트(80, 443)만 사용할 수 있습니다.");
        }
    }

    /**
     * DNS가 반환한 모든 IPv4·IPv6 주소가 공용 인터넷 주소인지 검증합니다.
     *
     * @param host 검증할 호스트명
     */
    private void validateResolvedAddresses(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw invalid("공고 URL의 인터넷 주소를 확인할 수 없습니다.");
            }
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw invalid("내부 네트워크 또는 보호된 주소는 공고 URL로 사용할 수 없습니다.");
                }
            }
        } catch (UnknownHostException exception) {
            throw invalid("공고 URL의 인터넷 주소를 확인할 수 없습니다.");
        }
    }

    /**
     * SSRF 차단 대상 주소인지 확인합니다.
     *
     * @param address DNS 해석 주소
     * @return 차단 대상이면 true
     */
    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first == 0 || first == 10 || first == 127 || first >= 224
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 198 && (second == 18 || second == 19));
        }
        if (address instanceof Inet6Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            return (first & 0xfe) == 0xfc;
        }
        return true;
    }

    /**
     * 사용자에게 표시 가능한 URL 검증 예외를 생성합니다.
     *
     * @param message 한글 오류 메시지
     * @return API 예외
     */
    private ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }
}
