package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SokchoDownloadCases {
    static ObservationCase selectCase(){String url="https://www.sokcho.go.kr/sc/portal/sokchonews/notification?notAncmtMgtNo=32983";var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("SOKCHO-32983","2026년 속초시 소상공인 특례보증 수수료 지원 사업 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000122","SAFE_SAEOL_EMINWON"),new SokchoAttachmentDiscoveryProfile(),"https://www.sokcho.go.kr/sc/portal/sokchonews/notification",2,TitleLayout.SOKCHO_BOARD);}
}
