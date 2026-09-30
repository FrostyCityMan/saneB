package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.LegalBoardAttachmentProfileConfiguration;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

/** 공식 검색 목록·상세에서 확인한 지원 공고. 기존 부산 프로필과 제목 정책을 유지한다. */
final class BusanCitySupportDownloadCases {
    static ObservationCase selectCase() {
        String url="https://www.busan.go.kr/nbgosi/view?sno=79622&gosiGbn=A&curPage=1";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("BUSAN_CITY-79622","2026년도 부산광역시 중소기업·소상공인 자금지원계획 10차 변경 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000027","SPRING_BBS"),
                new LegalBoardAttachmentProfileConfiguration().selectBusanLegalProfileDetails(),
                "https://www.busan.go.kr/nbgosi/list",1,TitleLayout.BUSAN_CITY_BOARD);
    }
}
