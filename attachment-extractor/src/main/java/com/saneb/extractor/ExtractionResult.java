package com.saneb.extractor;

import java.util.List;

/** 코드 포인트 offset을 사용하는 비HTML IPC 계약. 원문은 감사 로그로 전달하지 않는다. */
public record ExtractionResult(String format, String extractorVersion, String qualityCode,
        String text, List<Block> blocks, Integer pageCount, String errorCode,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        HwpStructure hwpStructure,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        HwpxStructure hwpxStructure) {
    public static final String VERSION = "1.0.5";
    public record Block(int index, int startOffset, int endOffset, String locator,
            String evidenceScopeId, boolean scopeReliable) { }
    /** 수치만 있는 격리 IPC 진단이다. 원문/파일명/컨트롤 payload를 포함하지 않는다. */
    public record HwpStructure(int sectionCount, int recordCount, int maximumLevel, List<RecordType> recordTypes) {
        public HwpStructure { recordTypes = List.copyOf(recordTypes); }
    }
    public record RecordType(int tagId, int count) { }
    /** 원문/속성/파일명 없이 부분 추출 원인을 구분하는 수치 진단이다. */
    public record HwpxStructure(int sectionCount, int paragraphCount, int pictureCount,
            int oleCount, int equationCount, int replacementCharacterCount) { }
    public ExtractionResult { blocks = List.copyOf(blocks); }
    public ExtractionResult(String format, String extractorVersion, String qualityCode,
            String text, List<Block> blocks, Integer pageCount, String errorCode) {
        this(format, extractorVersion, qualityCode, text, blocks, pageCount, errorCode, null, null);
    }
    public ExtractionResult(String format, String extractorVersion, String qualityCode,
            String text, List<Block> blocks, Integer pageCount, String errorCode, HwpStructure hwpStructure) {
        this(format, extractorVersion, qualityCode, text, blocks, pageCount, errorCode, hwpStructure, null);
    }
    public static ExtractionResult failure(String code) {
        return new ExtractionResult(null, VERSION, code, null, List.of(), null, code);
    }
}
