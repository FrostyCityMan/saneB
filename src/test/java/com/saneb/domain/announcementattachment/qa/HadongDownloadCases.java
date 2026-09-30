package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class HadongDownloadCases {
    static ObservationCase selectCase() {
        String url="https://www.hadong.go.kr/media/00012.web?amode=view&not_ancmt_mgt_no=45193";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("HADONG-45193","2026년 하동군 소상공인 디지털 인프라 지원사업 추가공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000237","SCMS_CARD_NOTICE"),
                new ScmsSaeolAttachmentProfileConfiguration().selectHadongProfileDetails(),
                "https://www.hadong.go.kr/media/00012.web",1,TitleLayout.HADONG_BOARD);
    }
}
