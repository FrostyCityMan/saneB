package com.saneb.domain.announcementattachment.qa;

import java.util.stream.Stream;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class YeonjeGuryeDownloadCases {
    static Stream<ObservationCase> selectCases(){return Stream.of(selectCase(true),selectCase(false));}
    static ObservationCase selectCase(boolean yeonje){
        String url=yeonje?"https://www.yeonje.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=43358&mId=0206030000":"https://www.gurye.go.kr/board/GosiView.do?pageIndex=1&menuNo=115004002001&not_ancmt_se_code=01,04,06,07&not_ancmt_mgt_no=25440";
        var n=new AnnouncementSourceIdentityNormalizer();var config=new YeonjeGuryeAttachmentProfileConfiguration();
        return new ObservationCase(yeonje?"YEONJE-43358":"GURYE-25440",yeonje?"2026년 하반기 연제구 다자녀가구 전세자금 대출이자 지원사업":"2026년 전남광주 청년 문화복지카드 지원사업 3차 모집 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,yeonje?"LGS-000040":"LGS-000185",yeonje?"SAEOL_GOSI":"SAFE_SAEOL_EMINWON"),
                yeonje?config.selectYeonjeProfileDetails():config.selectGuryeProfileDetails(),yeonje?"https://www.yeonje.go.kr/portal/contents.do?mId=0206030000":"https://www.gurye.go.kr/board/GosiList.do?not_ancmt_se_code=01,04,06,07&menuNo=115004002001&pageIndex=1",yeonje?1:2,yeonje?TitleLayout.YEONJE_BOARD:TitleLayout.GURYE_BOARD);
    }
}
