package com.saneb.domain.announcementsource.provider.content;

import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;

/**
 * 등록 source host를 경계로 상세 URL과 redirect의 SSRF 위험을 검증합니다.
 */
final class ProviderContentUrlValidator {

    private final ProviderContentHostResolver hostResolver;

    ProviderContentUrlValidator(ProviderContentHostResolver hostResolver) {
        this.hostResolver = Objects.requireNonNull(hostResolver, "hostResolver is required");
    }

    ValidatedRequest selectValidatedRequest(String registeredSourceUrl, String officialDetailUrl) {
        URI registeredSourceUri = selectHttpUri(registeredSourceUrl, FailureCode.SOURCE_URL_INVALID);
        String allowedHost = selectNormalizedHost(registeredSourceUri, FailureCode.SOURCE_URL_INVALID);
        selectPublicAddress(allowedHost);

        URI detailUri = selectHttpUri(officialDetailUrl, FailureCode.DETAIL_URL_INVALID);
        selectAllowedHost(detailUri, allowedHost);
        selectPublicAddress(allowedHost);
        return new ValidatedRequest(registeredSourceUri, detailUri, allowedHost);
    }

    URI selectRedirectUri(URI currentUri, String location, String allowedHost) {
        if (location == null || location.isBlank()) {
            throw invalid(FailureCode.REDIRECT_LOCATION_MISSING);
        }
        URI redirectUri;
        try {
            redirectUri = currentUri.resolve(new URI(location.trim()));
        } catch (IllegalArgumentException | URISyntaxException exception) {
            throw invalid(FailureCode.DETAIL_URL_INVALID);
        }
        redirectUri = selectHttpUri(redirectUri.toASCIIString(), FailureCode.DETAIL_URL_INVALID);
        selectAllowedHost(redirectUri, allowedHost);
        return redirectUri;
    }

    void validateBeforeRequest(URI requestUri, String allowedHost) {
        selectAllowedHost(requestUri, allowedHost);
        selectPublicAddress(allowedHost);
    }

    private URI selectHttpUri(String value, FailureCode invalidCode) {
        if (value == null || value.isBlank()) {
            throw invalid(invalidCode);
        }
        URI uri;
        try {
            uri = new URI(value.trim()).normalize();
        } catch (URISyntaxException exception) {
            throw invalid(invalidCode);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw invalid(invalidCode);
        }
        if (!uri.isAbsolute() || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw invalid(invalidCode);
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw invalid(invalidCode);
        }
        selectNormalizedHost(uri, invalidCode);
        return uri;
    }

    private void selectAllowedHost(URI uri, String allowedHost) {
        String requestHost = selectNormalizedHost(uri, FailureCode.DETAIL_URL_INVALID);
        if (!requestHost.equals(allowedHost)) {
            throw invalid(FailureCode.DETAIL_HOST_NOT_ALLOWED);
        }
    }

    private String selectNormalizedHost(URI uri, FailureCode invalidCode) {
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw invalid(invalidCode);
        }
        String host;
        try {
            host = IDN.toASCII(uri.getHost(), IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw invalid(invalidCode);
        }
        if ("localhost".equals(host)
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")) {
            throw invalid(FailureCode.ADDRESS_BLOCKED);
        }
        return host;
    }

    private void selectPublicAddress(String host) {
        InetAddress[] addresses;
        try {
            addresses = hostResolver.selectAddressList(host);
        } catch (UnknownHostException exception) {
            throw invalid(FailureCode.DNS_LOOKUP_FAILED);
        }
        if (addresses == null || addresses.length == 0) {
            throw invalid(FailureCode.DNS_LOOKUP_FAILED);
        }
        for (InetAddress address : addresses) {
            if (address == null || selectBlockedAddress(address)) {
                throw invalid(FailureCode.ADDRESS_BLOCKED);
            }
        }
    }

    private boolean selectBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first == 0
                    || first == 10
                    || first == 127
                    || first >= 224
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

    private ProviderContentValidationException invalid(FailureCode failureCode) {
        return new ProviderContentValidationException(failureCode);
    }

    record ValidatedRequest(URI registeredSourceUri, URI detailUri, String allowedHost) {
    }
}
