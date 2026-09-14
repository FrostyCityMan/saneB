package com.saneb.domain.announcementattachment.dto;

import com.saneb.common.response.PageResponse;
import java.util.List;
import java.util.UUID;

public record AttachmentProviderQaPlanResponse(String planHash,boolean isPolicyManifestCurrent,Summary summary,PageResponse<Item> targets) {
    public record Profile(String providerCode,String profileCode,String profileHash) { }
    public record Item(String providerCode,UUID sourceId,String localSourceCode,String listParserProfileCode,
            String statusCode,String message,List<Profile> profiles,int minimumNormalNoticeCount,List<String> requiredFormats) {
        public Item {profiles=List.copyOf(profiles);requiredFormats=List.copyOf(requiredFormats);}
    }
    public record Summary(int targetCount,int bindingMatchedCount,int missingProfileCount,int parserMismatchCount,int ambiguousProfileCount,
            int registeredProfileCount,int unboundProfileCount,int minimumNormalNoticeCount,boolean isExecutionPlanComplete,
            boolean isQaPassed,int currentHttpRequests,boolean isSingleLeaseCoverageGuaranteed) { }
}
