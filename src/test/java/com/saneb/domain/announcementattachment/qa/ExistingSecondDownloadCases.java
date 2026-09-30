package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.stream.Stream;

/** 공식 목록에서 확인한 모집 공고. 기존 처리기를 재사용하며 기대값 승인을 추가하지 않는다. */
final class ExistingSecondDownloadCases {
    static Stream<ObservationCase> selectCases() {
        return Stream.of(selectCase(true), selectCase(false));
    }
    private static ObservationCase selectCase(boolean wonju) {
        AttachmentDiscoveryProfile profile=wonju
                ?new StandardBbsAttachmentProfileConfiguration().selectWonjuProfileDetails()
                :new SeoguSaeolAttachmentDiscoveryProfile();
        String url=wonju?"https://www.wonju.go.kr/www/selectBbsNttView.do?key=216&bbsNo=140&nttNo=482226"
                :"https://www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do?notAncmtMgtNo=49944";
        var n=new AnnouncementSourceIdentityNormalizer();var binding=profile.selectSourceBindings().getFirst();
        return new ObservationCase(wonju?"WONJU-482226":"DAEJEON_SEOGU-49944",
                wonju?"2026년 원주시 소상공인 경영안정자금 지원사업 공고 （변경）":"2026년 대전 서구 소상공인 경영환경개선 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,binding.localSourceCode(),binding.listParserProfileCode()),
                profile,wonju?"https://www.wonju.go.kr/www/selectBbsNttList.do?key=216&bbsNo=140"
                        :"https://www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do",
                1,wonju?TitleLayout.WONJU_BOARD:TitleLayout.DAEJEON_SEOGU_BOARD);
    }
}
