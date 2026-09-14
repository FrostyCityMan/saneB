package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

/** 수집 run에 저장된 불변 정책 선택. NO_POLICY/OFF도 같은 run에서 임의로 교체하지 않는다. */
public record AttachmentCollectionPlan(UUID runId,UUID ruleReleaseId,UUID policyId,String statusCode) { }
