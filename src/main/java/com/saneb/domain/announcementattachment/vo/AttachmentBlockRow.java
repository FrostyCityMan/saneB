package com.saneb.domain.announcementattachment.vo;

public record AttachmentBlockRow(Integer blockIndex, Integer startOffset, Integer endOffset,
        String evidenceScopeId, Boolean scopeReliable, String locator, String text,
        Integer textStartOffset, Integer textEndOffset, Boolean hasMoreText) { }
