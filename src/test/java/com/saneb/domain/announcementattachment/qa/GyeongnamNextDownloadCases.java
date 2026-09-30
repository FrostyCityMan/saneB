package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.List;
import java.util.stream.Stream;

final class GyeongnamNextDownloadCases {
    static final List<String> GROUPS=List.of("GIMHAE","CHANGNYEONG");
    static Stream<ObservationCase> selectCases(){return GROUPS.stream().map(GyeongnamNextDownloadCases::selectCase);}
    static ObservationCase selectCase(String group){
        var config=new GyeongnamNextAttachmentProfileConfiguration();
        boolean gimhae="GIMHAE".equals(group);if(!GROUPS.contains(group))throw new IllegalArgumentException();
        String list=gimhae?"https://www.gimhae.go.kr/03360/00023/00029.web":"https://www.cng.go.kr/03517/01553.web";
        String url=list+(gimhae?"?amode=view&not_ancmt_mgt_no=109947&section=01":"?amode=view&not_ancmt_mgt_no=46407");
        var profile=gimhae?config.selectGimhaeProfileDetails():config.selectChangnyeongProfileDetails();var binding=profile.selectSourceBindings().getFirst();var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+(gimhae?"-109947":"-46407"),gimhae?"2026년 하반기 김해시 소상공인 육성자금 지원 계획 공고":"2026년 창녕군 소상공인 육성자금 지원계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,binding.localSourceCode(),binding.listParserProfileCode()),profile,list,1,
                gimhae?TitleLayout.GIMHAE_BOARD:TitleLayout.CHANGNYEONG_BOARD);
    }
}
