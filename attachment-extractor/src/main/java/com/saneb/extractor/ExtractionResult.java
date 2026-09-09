package com.saneb.extractor;

import java.util.List;

/** 코드 포인트 offset을 사용하는 비HTML IPC 계약. 원문은 감사 로그로 전달하지 않는다. */
public record ExtractionResult(String format, String extractorVersion, String qualityCode,
        String text, List<Block> blocks, Integer pageCount, String errorCode) {
    public record Block(int index, int startOffset, int endOffset, String locator,
            String evidenceScopeId, boolean scopeReliable) { }
    public ExtractionResult { blocks = List.copyOf(blocks); }
    public static ExtractionResult failure(String code) {
        return new ExtractionResult(null, "1.0.0", code, null, List.of(), null, code);
    }
}
