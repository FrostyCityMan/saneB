package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GangdongDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.gangdong.go.kr/web/newportal/notice/01/37327";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("GANGDONG-37327","중소기업 및 소상공인 특별추천 신용대출 확대 및 1년간 무이자 융자지원",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000026","SAFE_SAEOL_EMINWON_HREF"),
                new GangdongAttachmentDiscoveryProfile(),"https://www.gangdong.go.kr/web/newportal/notice/01",2,TitleLayout.GANGDONG_PORTAL);
    }
}
