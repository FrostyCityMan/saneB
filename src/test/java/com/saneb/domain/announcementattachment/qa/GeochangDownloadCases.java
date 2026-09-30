package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GeochangDownloadCases {
    static ObservationCase selectCase() {
        String url="https://www.geochang.go.kr/00445/00451.web?amode=view&not_ancmt_mgt_no=47689";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("GEOCHANG-47689","「거창군 청년 지역활동 프로젝트 지원사업」 참여자 모집 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000240","SCMS_CARD_NOTICE"),
                new ScmsSaeolAttachmentProfileConfiguration().selectGeochangProfileDetails(),
                "https://www.geochang.go.kr/00445/00451.web",1,TitleLayout.GEOCHANG_BOARD);
    }
}
