package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongnamThirdDownloadCases {
    static final Set<String> GROUPS = Set.of("TONGYEONG", "HAPCHEON");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        boolean ty = group.equals("TONGYEONG"); var config = new ScmsSaeolAttachmentProfileConfiguration();
        var profile = ty ? config.selectTongyeongProfileDetails() : config.selectHapcheonProfileDetails();
        String id = ty ? "49251" : "44432";
        String list = ty ? "https://www.tongyeong.go.kr/00852/00853/00858.web" : "https://www.hc.go.kr/04923/04924/04948.web";
        String url = list + "?amode=view&not_ancmt_mgt_no=" + id;
        String title = ty ? "2026년 통영시 소상공인 육성자금 지원사업 공고" : "2026년 4분기 합천군 중소기업 및 소상공인 육성기금 지원계획 공고";
        var n = new AnnouncementSourceIdentityNormalizer(); var binding = profile.selectSourceBindings().getFirst();
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                binding.localSourceCode(), binding.listParserProfileCode()), profile, list, ty ? 1 : 2, TitleLayout.CHANGWON_BOARD);
    }
}
