package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class AndongDownloadCases {
    static ObservationCase selectCase(){
        var normalizer=new AnnouncementSourceIdentityNormalizer();String url="https://www.andong.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=63386&isLinkage=Y&mId=0401020100";
        return new ObservationCase("ANDONG-63386","2026년 안동시 소상공인 간판설치비용 지원사업 신청자 모집 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000204","SPRING_BBS"),
                new AndongAttachmentDiscoveryProfile(),"https://www.andong.go.kr/portal/saeol/gosi/list.do?mId=0401020100",3,TitleLayout.ANDONG_BOARD);
    }
}
