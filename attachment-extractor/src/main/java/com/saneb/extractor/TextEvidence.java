package com.saneb.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class TextEvidence {
    static final int MAX_CHARACTERS = 1_000_000;
    private final StringBuilder text = new StringBuilder();
    private final List<ExtractionResult.Block> blocks = new ArrayList<>();
    private int length;
    private boolean partial;

    void insertBlock(String value, String locator, boolean reliable) throws IOException {
        // PostgreSQL text에 저장할 수 없는 NUL만 공백으로 바꾸고 문서 구조를 추정하지 않는다.
        String normalized = value.replace('\0', ' ').strip();
        if (normalized.isEmpty()) return;
        int size = normalized.codePointCount(0, normalized.length());
        if (length + size + 1 > MAX_CHARACTERS || blocks.size() >= 20000) throw new IOException("LIMIT_EXCEEDED");
        if (!text.isEmpty()) { text.append('\n'); length++; }
        blocks.add(new ExtractionResult.Block(blocks.size(), length, length + size, locator, locator, reliable));
        text.append(normalized);
        length += size;
        if (normalized.indexOf('\ufffd') >= 0) partial = true;
    }
    void updatePartial() { partial = true; }
    ExtractionResult selectResult(String format, Integer pages) {
        return new ExtractionResult(format, ExtractionResult.VERSION, text.isEmpty() ? "OCR_REQUIRED"
                : partial ? "PARTIAL_TEXT" : "COMPLETE_TEXT", text.toString(), blocks, pages, null);
    }
}
