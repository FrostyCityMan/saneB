package com.saneb.domain.announcementsource.provider.content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

record ProviderContentHttpResponse(
        int statusCode,
        Map<String, List<String>> headers,
        byte[] body
) {

    ProviderContentHttpResponse {
        Map<String, List<String>> copiedHeaders = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, values) -> copiedHeaders.put(
                    name.toLowerCase(Locale.ROOT),
                    values == null ? List.of() : List.copyOf(values)
            ));
        }
        headers = Map.copyOf(copiedHeaders);
        body = body == null ? new byte[0] : body.clone();
    }

    String selectFirstHeader(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
