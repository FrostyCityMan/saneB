package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongbukFifthDownloadCases {
    static final Set<String> GROUPS=Set.of("YEONGDEOK","ULJIN");
    static ObservationCase selectCase(String group){
        var config=new GyeongbukDirectAttachmentProfileConfiguration();boolean yd="YEONGDEOK".equals(group);if(!GROUPS.contains(group))throw new IllegalArgumentException("GROUP_REQUIRED");
        var p=yd?config.selectYeongdeokProfileDetails():config.selectUljinProfileDetails();String id=yd?"366709":"35041";
        String list=yd?"https://www.yd.go.kr/?page_id=763":"https://www.uljin.go.kr/index.uljin?menuCd=DOM_000000103002007001";
        String url=list+(yd?"&uid="+id+"&mod=document":"&type=view&ancmtMgtNo="+id);
        String title=yd?"2026년 소상공인 고효율기기지원사업 신청 공고":"「2026년 울진군 소상공인 카드수수료 지원사업」공고";
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,1,yd?TitleLayout.YEONGDEOK_BOARD:TitleLayout.ULJIN_BOARD);
    }
}
