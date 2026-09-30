package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GwangjuSeoguDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.seogu.gwangju.kr/api/eminwon/gosiXmlView.es?mid=a10807010000&not_ancmt_mgt_no=53423&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst";
        var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase("GWANGJU_SEOGU-53423","2026년 서구 소상공인 카드 수수료 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000067","SPRING_BBS"),new GwangjuSeoguAttachmentDiscoveryProfile(),
                "https://www.seogu.gwangju.kr/menu.es?mid=a10807010000",1,TitleLayout.GWANGJU_SEOGU_BOARD);
    }
}
