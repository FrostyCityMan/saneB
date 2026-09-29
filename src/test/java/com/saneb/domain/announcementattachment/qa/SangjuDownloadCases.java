package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SangjuDownloadCases {
    static ObservationCase selectCase(){
        var normalizer=new AnnouncementSourceIdentityNormalizer();String url="https://www.sangju.go.kr/gosi/detail.tc?mn=10297&mgtNo=27590";
        return new ObservationCase("SANGJU-27590","2026년 상주시 소상공인 희망드림 특례보증 지원계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000208","SAFE_SANGJU_GOSI"),
                new SangjuAttachmentDiscoveryProfile(),"https://www.sangju.go.kr/page/10297/10606.tc",2,TitleLayout.SANGJU_BOARD);
    }
}
