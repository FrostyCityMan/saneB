package com.saneb.domain.announcementsource.vo;

import java.time.OffsetDateTime;

public record AnnouncementSourceExclusionCountRow(String dimension, String code, long count, OffsetDateTime asOf) { }
