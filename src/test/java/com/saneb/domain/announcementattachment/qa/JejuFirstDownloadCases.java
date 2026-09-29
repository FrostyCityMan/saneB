package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class JejuFirstDownloadCases {
    static final Set<String> GROUPS = Set.of("JEJUSI", "SEOGWIPO");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        boolean city = group.equals("JEJUSI"); var config = new JejuAttachmentProfileConfiguration();
        var profile = city ? config.selectJejusiProfileDetails() : config.selectSeogwipoProfileDetails();
        String id = city ? "108927" : "71884";
        String url = city ? "https://www.jejusi.go.kr/information/intro/notice.do?mode=detail&ancmnt_pbanc_mng_no=" + id + "&currentPageNo=1"
                : "https://eminwon.seogwipo.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + id + "&subCheck=Y";
        String list = city ? "https://www.jejusi.go.kr/information/intro/notice.do" : "https://eminwon.seogwipo.go.kr/emwp/jsp/ofr/OfrNotAncmtL.jsp?not_ancmt_se_code=01,04,07";
        String title = city ? "2026년도 소상공인 디지털 마케팅 지원사업 4차 공고" : "2026년 소상공인 고용보험료 지원사업 공고";
        var n = new AnnouncementSourceIdentityNormalizer(); var binding = profile.selectSourceBindings().getFirst();
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                binding.localSourceCode(), binding.listParserProfileCode()), profile, list, city ? 2 : 1, city ? TitleLayout.JEJUSI_BOARD : TitleLayout.HWACHEON_LABEL);
    }
}
