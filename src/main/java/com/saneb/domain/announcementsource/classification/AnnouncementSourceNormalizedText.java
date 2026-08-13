package com.saneb.domain.announcementsource.classification;

import java.util.Arrays;

/**
 * 정규화 문자열과 원문 Unicode code point 위치 대응표입니다.
 */
public final class AnnouncementSourceNormalizedText {

    private final String originalText;
    private final String normalizedText;
    private final int[] normalizedCodePoints;
    private final int[] originalStartOffsets;
    private final int[] originalEndOffsets;

    AnnouncementSourceNormalizedText(
            String originalText,
            String normalizedText,
            int[] normalizedCodePoints,
            int[] originalStartOffsets,
            int[] originalEndOffsets
    ) {
        this.originalText = originalText;
        this.normalizedText = normalizedText;
        this.normalizedCodePoints = normalizedCodePoints.clone();
        this.originalStartOffsets = originalStartOffsets.clone();
        this.originalEndOffsets = originalEndOffsets.clone();
        if (this.normalizedCodePoints.length != this.originalStartOffsets.length
                || this.normalizedCodePoints.length != this.originalEndOffsets.length) {
            throw new IllegalArgumentException("normalized text mapping lengths must be equal");
        }
    }

    public String originalText() {
        return originalText;
    }

    public String normalizedText() {
        return normalizedText;
    }

    public int[] normalizedCodePoints() {
        return normalizedCodePoints.clone();
    }

    public int length() {
        return normalizedCodePoints.length;
    }

    public OriginalRange selectOriginalRange(int normalizedStartOffset, int normalizedEndOffset) {
        if (normalizedStartOffset < 0
                || normalizedEndOffset <= normalizedStartOffset
                || normalizedEndOffset > normalizedCodePoints.length) {
            throw new IllegalArgumentException("invalid normalized offset range");
        }
        int originalStart = Arrays.stream(
                originalStartOffsets,
                normalizedStartOffset,
                normalizedEndOffset
        ).min().orElseThrow();
        int originalEnd = Arrays.stream(
                originalEndOffsets,
                normalizedStartOffset,
                normalizedEndOffset
        ).max().orElseThrow();
        return new OriginalRange(originalStart, originalEnd);
    }

    public record OriginalRange(int startOffset, int endOffset) {
    }
}
