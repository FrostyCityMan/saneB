package com.saneb.extractor;

import java.util.List;

/** 코드 포인트 offset을 사용하는 비HTML IPC 계약. 원문은 감사 로그로 전달하지 않는다. */
public record ExtractionResult(String format, String extractorVersion, String qualityCode,
        String text, List<Block> blocks, Integer pageCount, String errorCode,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        HwpStructure hwpStructure,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        HwpxStructure hwpxStructure,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        List<PartialCause> hwpPartialCauses) {
    public static final String VERSION = "1.0.8";
    /** 고정된 검증 실패 종류다. 원본 문자열이나 컨트롤 ID를 코드로 사용하지 않는다. */
    public enum HwpPartialCause {
        UNATTACHED_PARAGRAPH, PARAGRAPH_LEVEL_GAP, UNATTACHED_TEXT, CONTROL_LEVEL_GAP,
        UNATTACHED_CONTROL, UNSUPPORTED_RECORD, LOOSE_STRUCTURE, MULTIPLE_TEXT_RECORDS,
        UNSUPPORTED_INLINE_CONTROL, TABLE_ANCHOR_TYPE, MISSING_CONTROL, UNANCHORED_CONTROL,
        UNSUPPORTED_CONTROL, REPLACEMENT_CHARACTER, TABLE_CONTROL_HEADER, TABLE_PARAGRAPH_LEVEL,
        TABLE_PARAGRAPH_WITHOUT_CELL, TABLE_METADATA_INVALID, TABLE_METADATA_MISSING, TABLE_LOOSE_STRUCTURE,
        CELL_HEADER_INVALID, CELL_GEOMETRY_INVALID, CELL_PARAGRAPH_COUNT, CELL_ORDER_INVALID,
        CELL_PARAGRAPH_HEADER, CELL_OVERLAP, TABLE_PARAGRAPH_COUNT, TABLE_COVERAGE, TABLE_ROW_COUNTS,
        FIELD_HEADER_INVALID, FIELD_RANGE_INVALID
    }
    public record PartialCause(HwpPartialCause code, int count) { }
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
    public ExtractionResult { blocks = List.copyOf(blocks); if (hwpPartialCauses != null) hwpPartialCauses = List.copyOf(hwpPartialCauses); }
    public ExtractionResult(String format, String extractorVersion, String qualityCode,
            String text, List<Block> blocks, Integer pageCount, String errorCode, HwpStructure hwpStructure, HwpxStructure hwpxStructure) {
        this(format, extractorVersion, qualityCode, text, blocks, pageCount, errorCode, hwpStructure, hwpxStructure, null);
    }
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
