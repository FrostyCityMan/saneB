package com.saneb.domain.announcementsource.provider.content;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * URL 정책 검증 직후 확정한 요청 URI와 연결 대상 IP를 함께 전달합니다.
 */
record ProviderContentRequestTarget(
        URI uri,
        String allowedHost,
        List<InetAddress> pinnedAddresses
) {

    ProviderContentRequestTarget {
        Objects.requireNonNull(uri, "uri is required");
        Objects.requireNonNull(allowedHost, "allowedHost is required");
        pinnedAddresses = List.copyOf(Objects.requireNonNull(
                pinnedAddresses,
                "pinnedAddresses is required"
        ));
        if (pinnedAddresses.isEmpty()) {
            throw new IllegalArgumentException("pinnedAddresses must not be empty");
        }
    }

    InetAddress[] selectPinnedAddressArray() {
        return pinnedAddresses.toArray(InetAddress[]::new);
    }
}
