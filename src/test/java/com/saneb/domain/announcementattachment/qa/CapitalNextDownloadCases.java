package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class CapitalNextDownloadCases {
    static final Set<String> GROUPS = Set.of("YONGIN", "UIWANG");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        boolean yongin = group.equals("YONGIN");
        var c = new CapitalNextAttachmentProfileConfiguration();
        var p = yongin ? c.selectYonginProfileDetails() : c.selectUiwangProfileDetails();
        String id = yongin ? "145475" : "38883";
        String base = "https://" + p.selectApprovedHosts().iterator().next() + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        String sub = yongin ? "Y" : "N";
        String url = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + id + "&subCheck=" + sub;
        String list = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=" + (yongin ? "01%2C04" : "01%2C04%2C06") + "&pageIndex=1&subCheck=" + sub + "&yyyy=&list_gubun=A";
        String title = yongin ? "2026년 용인시 소상공인 온라인 플랫폼 비용 지원사업 추가모집 신청공고" : "2026년 의왕시 소상공인 특례보증 및 이차보전금 지원 계획 공고";
        var n = new AnnouncementSourceIdentityNormalizer(); var b = p.selectSourceBindings().getFirst();
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url, b.localSourceCode(), b.listParserProfileCode()), p, list, yongin ? 2 : 1, yongin ? TitleLayout.YONGIN_BOARD : TitleLayout.HWACHEON_LABEL);
    }
}
