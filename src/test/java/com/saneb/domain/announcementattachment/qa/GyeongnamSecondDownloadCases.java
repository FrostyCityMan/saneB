package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongnamSecondDownloadCases {
    static final Set<String> GROUPS = Set.of("GOSEONG", "CHANGWON");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        boolean go = group.equals("GOSEONG"); var config = new GyeongnamBoardAttachmentProfileConfiguration();
        var profile = go ? config.selectGoseongProfileDetails() : config.selectChangwonProfileDetails();
        String id = go ? "5733464" : "211423";
        String list = go ? "https://www.goseong.go.kr/board/list.goseong?boardId=BBS_0000015&menuCd=DOM_000000103001014000&contentsSid=29&cpath="
                : "https://www.changwon.go.kr/cwportal/10310/10438/10439.web?section=gosi";
        String url = go ? "https://www.goseong.go.kr/board/view.goseong?boardId=BBS_0000015&menuCd=DOM_000000103001014000&paging=ok&startPage=1&dataSid=" + id
                : "https://www.changwon.go.kr/cwportal/10310/10438/10439.web?amode=view&not_ancmt_mgt_no=" + id + "&section=gosi";
        String title = go ? "2026년도 고성군 소상공인 육성자금 지원 공고" : "2026년 하반기 창원시 소상공인 육성자금 지원사업 공고";
        var n = new AnnouncementSourceIdentityNormalizer(); var binding = profile.selectSourceBindings().getFirst();
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                binding.localSourceCode(), binding.listParserProfileCode()), profile, list, 1, go ? TitleLayout.GOSEONG_BOARD : TitleLayout.CHANGWON_BOARD);
    }
}
