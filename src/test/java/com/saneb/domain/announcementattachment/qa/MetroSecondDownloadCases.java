package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class MetroSecondDownloadCases {
    static final Set<String> GROUPS = Set.of("GWANGJU_DONGGU", "GWANGJU_BUKGU", "DAEJEON_DONGGU");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        var c = new MetroSecondAttachmentProfileConfiguration();
        var p = switch(group) { case "GWANGJU_DONGGU" -> c.selectGwangjuDongguProfileDetails(); case "GWANGJU_BUKGU" -> c.selectGwangjuBukguProfileDetails(); default -> c.selectDaejeonDongguProfileDetails(); };
        String id = switch(group) { case "GWANGJU_DONGGU" -> "43109"; case "GWANGJU_BUKGU" -> "56519"; default -> "36677"; };
        String host = p.selectApprovedHosts().iterator().next(); String base = "https://" + host + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        String url = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + id + "&subCheck=Y";
        String list = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy=";
        String title = switch(group) { case "GWANGJU_DONGGU" -> "2025년 연매출액 기준 광주광역시 동구 임차 소상공인 카드수수료 지원사업 공고"; case "GWANGJU_BUKGU" -> "2025년 연매출기준 북구 임차 소상공인 카드수수료 지원사업 공고"; default -> "사회적경제기업 연계 언택트 청년일자리 지원사업 참여자 추가 모집 공고(3차)"; };
        var n = new AnnouncementSourceIdentityNormalizer(); var b = p.selectSourceBindings().getFirst();
        var layout = switch(group) { case "GWANGJU_DONGGU" -> TitleLayout.GWANGJU_DONGGU_BOARD; case "GWANGJU_BUKGU" -> TitleLayout.GWANGJU_BUKGU_BOARD; default -> TitleLayout.HWACHEON_LABEL; };
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                b.localSourceCode(), b.listParserProfileCode()), p, list, group.equals("DAEJEON_DONGGU") ? 1 : 2, layout);
    }
}
