package com.saneb.domain.announcementsource.classification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnnouncementSourceTextNormalizerTest {

    private final AnnouncementSourceTextNormalizer normalizer = new AnnouncementSourceTextNormalizer();

    @Test
    void selectNormalizedTextAppliesNfkcLowercaseAndWhitespaceRules() {
        AnnouncementSourceNormalizedText normalized = normalizer.selectNormalizedText(
                "  ＴＩＰＳ·PoC\n\t소상공인  "
        );

        assertThat(normalized.normalizedText()).isEqualTo("tips poc 소상공인");
    }

    @Test
    void selectNormalizedTextKeepsOriginalCodePointOffsets() {
        AnnouncementSourceNormalizedText normalized = normalizer.selectNormalizedText("🚀  소상공인 지원");
        int keywordStart = normalized.normalizedText().codePointCount(
                0,
                normalized.normalizedText().indexOf("소상공인")
        );

        AnnouncementSourceNormalizedText.OriginalRange range = normalized.selectOriginalRange(
                keywordStart,
                keywordStart + "소상공인".codePointCount(0, "소상공인".length())
        );

        assertThat(range.startOffset()).isEqualTo(3);
        assertThat(range.endOffset()).isEqualTo(7);
    }

    @Test
    void selectNormalizedTextPreservesWordBoundariesForPunctuation() {
        AnnouncementSourceNormalizedText normalized = normalizer.selectNormalizedText("R&D/IP,IR");

        assertThat(normalized.normalizedText()).isEqualTo("r d ip ir");
    }
}
