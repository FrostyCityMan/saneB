package com.saneb.domain.announcementsource.classification;

import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 원문을 보존하면서 NFKC·소문자·공백 규칙을 적용합니다.
 */
public final class AnnouncementSourceTextNormalizer {

    public AnnouncementSourceNormalizedText selectNormalizedText(String sourceText) {
        String original = sourceText == null ? "" : sourceText;
        List<Integer> codePoints = new ArrayList<>();
        List<Integer> originalStarts = new ArrayList<>();
        List<Integer> originalEnds = new ArrayList<>();

        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(original);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String segment = original.substring(start, end);
            String normalizedSegment = Normalizer.normalize(segment, Normalizer.Form.NFKC)
                    .toLowerCase(Locale.ROOT);
            int sourceStart = original.codePointCount(0, start);
            int sourceEnd = original.codePointCount(0, end);
            normalizedSegment.codePoints().forEach(codePoint -> appendNormalizedCodePoint(
                    codePoint,
                    sourceStart,
                    sourceEnd,
                    codePoints,
                    originalStarts,
                    originalEnds
            ));
        }

        removeTrailingSpace(codePoints, originalStarts, originalEnds);
        int[] normalizedCodePoints = codePoints.stream().mapToInt(Integer::intValue).toArray();
        String normalizedText = new String(normalizedCodePoints, 0, normalizedCodePoints.length);
        return new AnnouncementSourceNormalizedText(
                original,
                normalizedText,
                normalizedCodePoints,
                originalStarts.stream().mapToInt(Integer::intValue).toArray(),
                originalEnds.stream().mapToInt(Integer::intValue).toArray()
        );
    }

    private void appendNormalizedCodePoint(
            int codePoint,
            int sourceStart,
            int sourceEnd,
            List<Integer> codePoints,
            List<Integer> originalStarts,
            List<Integer> originalEnds
    ) {
        int normalizedCodePoint = isWordCodePoint(codePoint) ? codePoint : ' ';
        if (normalizedCodePoint == ' ') {
            if (codePoints.isEmpty()) {
                return;
            }
            if (codePoints.get(codePoints.size() - 1) == ' ') {
                int lastIndex = originalEnds.size() - 1;
                originalEnds.set(lastIndex, Math.max(originalEnds.get(lastIndex), sourceEnd));
                return;
            }
        }
        codePoints.add(normalizedCodePoint);
        originalStarts.add(sourceStart);
        originalEnds.add(sourceEnd);
    }

    private boolean isWordCodePoint(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || Character.getType(codePoint) == Character.NON_SPACING_MARK;
    }

    private void removeTrailingSpace(
            List<Integer> codePoints,
            List<Integer> originalStarts,
            List<Integer> originalEnds
    ) {
        if (!codePoints.isEmpty() && codePoints.get(codePoints.size() - 1) == ' ') {
            int lastIndex = codePoints.size() - 1;
            codePoints.remove(lastIndex);
            originalStarts.remove(lastIndex);
            originalEnds.remove(lastIndex);
        }
    }
}
