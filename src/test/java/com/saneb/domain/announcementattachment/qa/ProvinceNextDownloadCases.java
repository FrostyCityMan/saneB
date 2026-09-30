package com.saneb.domain.announcementattachment.qa;

import java.util.List;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class ProvinceNextDownloadCases {
    static List<ObservationCase> selectCases(){return List.of(selectCase(true),selectCase(false));}
    static ObservationCase selectCase(boolean gyeongnam){
        String url=gyeongnam?"https://www.gyeongnam.go.kr/index.gyeong?menuCd=DOM_000000135003009001&mode=view&sno=54749&gosiGbn=A":"https://www.chungnam.go.kr/cnportal/province/province/view.do?nttId=2182547&menuNo=500487";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(gyeongnam?"GYEONGNAM_PROVINCE-54749":"CHUNGNAM_PROVINCE-2182547",gyeongnam?"2026년도 경상남도 소상공인 정책자금 운용계획 변경공고":"2026년 소상공인 화재보험료 지원사업 수정 공고(2차)",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,gyeongnam?"LGS-000223":"LGS-000147",gyeongnam?"SAEOL_GOSI":"SPRING_BBS"),gyeongnam?new GyeongnamProvinceAttachmentDiscoveryProfile():new ChungnamProvinceAttachmentDiscoveryProfile(),
                gyeongnam?"https://www.gyeongnam.go.kr/index.gyeong?menuCd=DOM_000000135003009000":"https://www.chungnam.go.kr/cnportal/province/province/list.do?menuNo=500487",gyeongnam?1:2,gyeongnam?TitleLayout.GYEONGNAM_PROVINCE_BOARD:TitleLayout.CHUNGNAM_PROVINCE_BOARD);
    }
}
