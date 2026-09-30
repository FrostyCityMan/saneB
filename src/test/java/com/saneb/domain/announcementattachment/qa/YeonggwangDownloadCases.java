package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.YeonggwangAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class YeonggwangDownloadCases {
    static ObservationCase selectCase() { return selectCase(false); }
    static ObservationCase selectCase(boolean titleStop) {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String notice = titleStop ? "29278" : "29330";
        String title = titleStop ? "「2026년 하반기 영광군 소상공인 소규모 경영환경 개선사업」신청자 모집 공고" : "2026년 전남광주 청년 문화복지카드 지원사업 대상자 3차 모집 공고";
        String url = normalizer.canonicalizeUrl("https://www.yeonggwang.go.kr/bbs/?b_id=gosigonggo&site=headquarter_new&mn=9059&type=view&bs_idx=" + notice);
        return new ObservationCase("YEONGGWANG-" + notice,title,
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(url),url,"LGS-000195","SPRING_BBS"),
                new YeonggwangAttachmentDiscoveryProfile(),"https://www.yeonggwang.go.kr/bbs/?b_id=gosigonggo&site=headquarter_new&mn=9059",1,TitleLayout.YEONGGWANG_BOARD);
    }
}
