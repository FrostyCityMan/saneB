package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class MapoDownloadCases {
    static ObservationCase selectCase(){String url="https://www.mapo.go.kr/site/main/nPortal/detail?bcId=19775";var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase("MAPO-19775","2026년 마포구 소기업·소상공인 특별신용보증 지원 계획 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000015","MAPO_LEGAL_NOTICE_TABLE"),new MapoAttachmentDiscoveryProfile(),"https://www.mapo.go.kr/site/main/nPortal/list",1,TitleLayout.MAPO_BOARD);}
}
