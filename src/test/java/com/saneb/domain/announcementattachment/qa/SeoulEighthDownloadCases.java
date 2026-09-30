package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class SeoulEighthDownloadCases {
    static final Set<String> GROUPS=Set.of("GANGNAM","DOBONG");
    static ObservationCase selectCase(String group){
        if(!GROUPS.contains(group))throw new IllegalArgumentException("QA_GROUP_REQUIRED");boolean gangnam="GANGNAM".equals(group);
        var config=new SeoulEighthAttachmentProfileConfiguration();var profile=gangnam?config.selectGangnamProfileDetails():config.selectDobongProfileDetails();
        String url=gangnam?"https://www.gangnam.go.kr/notice/view.do?not_ancmt_mgt_no=64668&mid=ID05_040201":"https://www.dobong.go.kr/WDB_DEV/gosigong_go/detail.asp?idx=4734";
        String list=gangnam?"https://www.gangnam.go.kr/notice/list.do?mid=ID05_040201":"https://www.dobong.go.kr/Contents.asp?code=10008772";
        var n=new AnnouncementSourceIdentityNormalizer();var b=profile.selectSourceBindings().getFirst();
        return new ObservationCase(group+(gangnam?"-64668":"-4734"),gangnam?"2026년 강남구 대학생 중소기업 인턴십 지원사업 참여기업 및 인턴모집 공고":"「2026년 도봉구 청년 사회첫출발 지원금 지원」사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),profile,list,4,
                gangnam?TitleLayout.GANGNAM_BOARD:TitleLayout.DOBONG_BOARD);
    }
}
