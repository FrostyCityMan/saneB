package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SeohaeDownloadCases {
    static ObservationCase selectCase(){String url="https://seohae.go.kr/open_content/main/bbs/bbsMsgDetail.do?msg_seq=42495&bcd=gosi";var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase("SEOHAE-42495","2025년도 소기업, 소상공인 특례보증 추천 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000062","HEURISTIC_NOTICE"),new SeohaeAttachmentDiscoveryProfile(),"https://seohae.go.kr/open_content/main/community/news/gosi.jsp",1,TitleLayout.SEOHAE_BOARD);}
}
