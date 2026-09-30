package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SacheonDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.sacheon.go.kr/news/00009/00014.web?gcode=2017&idx=2106170&amode=view";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("SACHEON-2106170","2026년 하반기 사천시 소상공인 육성자금 지원 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000227","HEURISTIC_NOTICE"),new SacheonAttachmentDiscoveryProfile(),
                "https://www.sacheon.go.kr/news/00009/00014.web",1,TitleLayout.SACHEON_BOARD);
    }
}
