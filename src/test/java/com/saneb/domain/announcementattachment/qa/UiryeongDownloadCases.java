package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class UiryeongDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.uiryeong.go.kr/board/view.uiryeong?boardId=BBS_0000070&menuCd=DOM_000000203003001001&startPage=1&dataSid=316445&gosiNo=35341";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("UIRYEONG-35341","2026년 의령군 소상공인 육성지원 사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000232","HEURISTIC_NOTICE"),new UiryeongAttachmentDiscoveryProfile(),
                "https://www.uiryeong.go.kr/index.uiryeong?menuCd=DOM_000000203003000000",1,TitleLayout.UIRYEONG_BOARD);
    }
}
