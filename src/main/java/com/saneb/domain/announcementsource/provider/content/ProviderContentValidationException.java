package com.saneb.domain.announcementsource.provider.content;

import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;

final class ProviderContentValidationException extends RuntimeException {

    private final FailureCode failureCode;

    ProviderContentValidationException(FailureCode failureCode) {
        super(failureCode.name());
        this.failureCode = failureCode;
    }

    FailureCode selectFailureCode() {
        return failureCode;
    }
}
