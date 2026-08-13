package com.saneb.domain.announcementsource.provider.content;

import java.io.IOException;

final class ProviderContentResponseTooLargeException extends IOException {

    ProviderContentResponseTooLargeException() {
        super("response size limit exceeded");
    }
}
