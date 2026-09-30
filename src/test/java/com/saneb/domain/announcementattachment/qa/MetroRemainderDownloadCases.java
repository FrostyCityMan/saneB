package com.saneb.domain.announcementattachment.qa;

import java.util.List;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class MetroRemainderDownloadCases {
    static List<ObservationCase> selectCases(){return List.of(selectCase(true),selectCase(false));}
    static ObservationCase selectCase(boolean bupyeong){
        String url=bupyeong?"https://www.icbp.go.kr/main/eminwon/eminwonAnnounceDetail.do?mgt_no=50550":"https://dongjak.eminwon.seoul.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=29506&subCheck=Y";
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(bupyeong?"BUPYEONG-50550":"DONGJAK-29506",bupyeong?"2026년 부평구 무주택 신혼부부 전월세자금 대출이자 지원사업 공고(2차)":"2026년 동작구 청년창업자 안심금융(무이자 특별보증) 지원 계획 공고",
            new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,bupyeong?"LGS-000060":"LGS-000021",bupyeong?"HEURISTIC_NOTICE":"SAFE_SAEOL_EMINWON"),bupyeong?new BupyeongAttachmentDiscoveryProfile():new DongjakAttachmentDiscoveryProfile(),
            bupyeong?"https://www.icbp.go.kr/main/participation/news/announce.jsp":"https://dongjak.eminwon.seoul.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",1,bupyeong?TitleLayout.BUPYEONG_PORTAL:TitleLayout.DONGJAK_POST_BOARD);
    }
}
