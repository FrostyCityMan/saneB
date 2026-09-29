package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GangwonProvinceDownloadCases {
    static final Set<String> GROUPS = Set.of("GANGWON_PROVINCE");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("QA_GROUP_REQUIRED");
        var profile = new GangwonProvinceAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String list = "https://state.gwd.go.kr/portal/bulletin/notification", url = list + "?articleSeq=272329";
        return new ObservationCase(group + "-272329", "2026년도 강원특별자치도 소상공인 경영안정 특별자금 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000116", "SAFE_GWD_BULLETIN"), profile, list, 1, TitleLayout.GANGWON_PROVINCE_BOARD);
    }
}
