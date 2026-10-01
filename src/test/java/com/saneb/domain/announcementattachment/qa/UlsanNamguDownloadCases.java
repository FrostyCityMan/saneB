package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

/** 공식 목록과 서울 실측 제목으로 고정한 참조다. 실파일 성공/정책 QA 승인이 아니다. */
final class UlsanNamguDownloadCases {
    static ObservationCase selectCase() {
        String url="https://eminwon.ulsannamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
                +"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                +"&methodnm=selectOfrNotAncmtRegst&subCheck=Y&not_ancmt_mgt_no=53732";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("ULSAN_NAMGU-53732","2026년도 울산 남구 소상공인 경영안정자금 융자지원계획 2차 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,
                        "LGS-000079","SAFE_SAEOL_EMINWON_COMPACT"),
                new UlsanAttachmentProfileConfiguration().selectUlsanNamguProfileDetails(),
                "https://eminwon.ulsannamgu.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04&list_gubun=Y",
                1,TitleLayout.ULSAN_NAMGU_BOARD);
    }
}
