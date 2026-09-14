package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.util.List;

/** 서버 소유 catalog/원장에서만 구성할 실행 입력. HTTP 요청 DTO 또는 성공 결과 업로드 계약이 아니다. */
public record AttachmentProviderQaCase(String caseId, String profileCode, String profileHash, Source source,
        String title, AnnouncementSourceClassificationRuleSet rules, String runtimeHash,
        String discoveryStatus, boolean discoveryComplete, List<ExpectedFile> files, Limits limits) {
    public AttachmentProviderQaCase { files = List.copyOf(files); }
    @Override public String toString() { return "AttachmentProviderQaCase[input=REDACTED]"; }

    public record ExpectedFile(String locatorHash, boolean downloadAllowed, String format, String binaryHash,
            String quality, int minimumCharacters, int minimumBlocks, List<String> requiredPhrases) {
        public ExpectedFile { requiredPhrases = List.copyOf(requiredPhrases); }
        @Override public String toString() { return "ExpectedFile[expectation=REDACTED]"; }
    }
    public record Limits(int maximumSeconds, int maximumRequestReservations, long maximumReservedBytes) { }
}
