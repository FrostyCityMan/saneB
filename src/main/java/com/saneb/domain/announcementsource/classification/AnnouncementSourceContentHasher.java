package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 첨부파일을 제외한 provider 원문과 분류 본문 상태의 canonical SHA-256을 계산합니다.
 */
public final class AnnouncementSourceContentHasher {

    private static final AnnouncementSourceIdentityNormalizer IDENTITY_NORMALIZER =
            new AnnouncementSourceIdentityNormalizer();

    private AnnouncementSourceContentHasher() {
    }

    public static String selectHash(
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result
    ) {
        String canonicalContent = selectValue(item.providerCode()) + '\u0000'
                + selectValue(item.rawHash()) + '\u0000'
                + selectValue(item.title()) + '\u0000'
                + result.bodySourceCode().name() + '\u0000'
                + result.bodyAvailabilityCode().name() + '\u0000'
                + selectValue(item.bodyText()) + '\u0000'
                + selectValue(IDENTITY_NORMALIZER.canonicalizeUrl(item.sourceUrl()));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonicalContent.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private static String selectValue(String value) {
        return value == null ? "" : value;
    }
}
