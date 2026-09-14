package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public record AttachmentBatchHistorySearch(UUID batchId,int throughVersion,int size,long offset) { }
