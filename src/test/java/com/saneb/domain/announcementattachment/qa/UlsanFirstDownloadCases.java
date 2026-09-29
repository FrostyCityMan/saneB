package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class UlsanFirstDownloadCases {
    static final Set<String> GROUPS = Set.of("ULSAN_JUNGGU", "ULSAN_BUKGU");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        boolean jung = group.equals("ULSAN_JUNGGU");
        var c = new UlsanAttachmentProfileConfiguration();
        var p = jung ? c.selectUlsanJungguProfileDetails() : c.selectUlsanBukguProfileDetails();
        String id = jung ? "21392" : "44394";
        String base = "https://" + p.selectApprovedHosts().iterator().next() + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        String url = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + id + "&subCheck=Y";
        String list = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=" + (jung ? "01%2C02%2C04" : "01%2C03%2C04") + "&pageIndex=1&subCheck=Y&yyyy=&list_gubun=A";
        String title = jung ? "2019년도 울산광역시 중구 소상공인 경영안정자금 융자계획 공고" : "「2026년도 하반기 소상공인 경영안정자금 지원계획」공고";
        var n = new AnnouncementSourceIdentityNormalizer(); var b = p.selectSourceBindings().getFirst();
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                b.localSourceCode(), b.listParserProfileCode()), p, list, 1, jung ? TitleLayout.ULSAN_JUNGGU_BOARD : TitleLayout.HWACHEON_LABEL);
    }
}
