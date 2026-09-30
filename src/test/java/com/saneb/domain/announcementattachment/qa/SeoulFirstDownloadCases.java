package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class SeoulFirstDownloadCases {
    static final Set<String> GROUPS = Set.of("EUNPYEONG", "SEOCHO");
    static ObservationCase selectEunpyeongSupportCase() {
        var old=selectCase("EUNPYEONG");
        String url=old.source().sourceUrl().replace("not_ancmt_mgt_no=48267","not_ancmt_mgt_no=50607");
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("EUNPYEONG-50607","2026년 4분기 중소기업육성기금 융자지원 계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,
                        old.source().localSourceCode(),old.source().listParserProfileCode()),
                old.profile(),old.listUrl(),4,old.titleLayout());
    }
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        boolean ep = group.equals("EUNPYEONG"); var c = new SeoulSaeolAttachmentProfileConfiguration();
        var p = ep ? c.selectEunpyeongProfileDetails() : c.selectSeochoProfileDetails();
        String id = ep ? "48267" : "43962", host = ep ? "eminwon.ep.go.kr" : "eminwon.seocho.go.kr";
        String base = "https://" + host + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        String url = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + id + "&subCheck=Y";
        String list = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C02%2C04&pageIndex=1&subCheck=Y&yyyy=";
        String title = ep ? "2026년 소규모 자영업자 LED간판 설치 지원사업 공고" : "2026년 서초구 중소상공인 초스피드 대출 지원 계획";
        var n = new AnnouncementSourceIdentityNormalizer(); var b = p.selectSourceBindings().getFirst();
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                b.localSourceCode(), b.listParserProfileCode()), p, list, ep ? 4 : 1, ep ? TitleLayout.EUNPYEONG_BOARD : TitleLayout.SEOCHO_BOARD);
    }
}
