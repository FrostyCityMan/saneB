package com.saneb.domain.announcementsource.provider.content;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
interface ProviderContentHttpTransport {

    ProviderContentHttpResponse selectResponse(
            URI uri,
            Duration readTimeout,
            int maxResponseBytes,
            String userAgent
    ) throws IOException, InterruptedException, TimeoutException;
}
