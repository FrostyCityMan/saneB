package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SeodaemunDownloadCases {
    static ObservationCase selectCase(){String url="https://www.sdm.go.kr/news/notice/notice.do?sdmBoardConfSeq=82&mode=view&sdmBoardSeq=313956";var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase("SEODAEMUN-313956","2026년 서대문구 소상공인 라이브커머스 지원사업 참여자 모집 재공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000014","SAFE_SEODAEMUN_NOTICE"),new SeodaemunAttachmentDiscoveryProfile(),"https://www.sdm.go.kr/news/notice/notice.do",4,TitleLayout.SEODAEMUN_BOARD);}
}
