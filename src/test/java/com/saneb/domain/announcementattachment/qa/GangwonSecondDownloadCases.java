package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GangwonSecondDownloadCases {
    static final Set<String> GROUPS=Set.of("YANGGU","INJE");
    static ObservationCase selectCase(String group){
        var s=Site.valueOf(group);var c=new GangwonSecondAttachmentProfileConfiguration();boolean yang=s==Site.YANGGU;
        String id=yang?"IHINR260828133751437":"245926",title=yang?"[양구군 공고 제2026–1082호] 2026년 영세소상공인 카드수수료 지원사업 2차 공고":"2026년 인제군 소상공인지원기금 융자지원 공고";
        String url="https://"+s.host+s.path+(yang?"?gfnc=www&bk="+id+"&mu_idx=226&bt=rd&bcd=announcement":"?articleSeq="+id);
        var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.sourceCode,s.parser),yang?c.selectYangguProfileDetails():c.selectInjeProfileDetails(),"https://"+s.host+s.path+(yang?"?gfnc=www&mu_idx=226":""),1,TitleLayout.valueOf(group+"_BOARD"));
    }
}
