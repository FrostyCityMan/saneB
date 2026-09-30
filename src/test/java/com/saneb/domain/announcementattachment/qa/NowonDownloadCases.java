package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class NowonDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.nowon.kr/www/user/bbs/BD_selectBbs.do?q_bbsCode=1003&q_clCode=0&q_estnColumn1=11&q_ntceSiteCode=11&q_bbscttSn=20260915151630474";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("NOWON-20260915151630474","2026년 미취업청년 어학·자격증 응시료 지원사업 4분기 모집 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000012","NOWON_NOTICE_TABLE"),new NowonAttachmentDiscoveryProfile(),"https://www.nowon.kr/www/user/bbs/BD_selectBbsList.do?q_bbsCode=1003&q_clCode=0&q_estnColumn1=11&q_ntceSiteCode=11",2,TitleLayout.NOWON_BOARD);
    }
}
