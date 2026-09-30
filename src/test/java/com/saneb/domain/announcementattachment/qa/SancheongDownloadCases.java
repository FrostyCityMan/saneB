package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SancheongDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.sancheong.go.kr/www/selectBbsNttView.do?key=158&bbsNo=118&nttNo=164021";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("SANCHEONG-164021","2026년 소상공인 소규모 경영환경 개선지원 사업(3차) 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000238","SAEOL_GOSI"),new SancheongAttachmentDiscoveryProfile(),
                "https://www.sancheong.go.kr/www/selectBbsNttList.do?bbsNo=118&key=158",2,TitleLayout.SANCHEONG_BOARD);
    }
}
