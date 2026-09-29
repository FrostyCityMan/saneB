package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class ChungcheongThirdDownloadCases {
    static final Set<String> GROUPS=Set.of("CHUNGBUK","GONGJU");
    static ObservationCase selectCase(String group){
        var s=Site.valueOf(group);var c=new ChungcheongThirdAttachmentProfileConfiguration();boolean cb=s==Site.CHUNGBUK;
        String id=cb?"67302":"59971",title=cb?"2026년 충청북도 소상공인 육성자금 지원계획 공고":"2026년 소상공인 화재보험료 지원사업(2차) 공고";
        String url="https://"+s.host+s.path+"?"+(cb?"key=422&no=":"notAncmtMgtNo=")+id;
        var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.sourceCode,s.parser),cb?c.selectChungbukProfileDetails():c.selectGongjuProfileDetails(),cb?"https://www.chungbuk.go.kr/www/selectGosiPblancList.do?key=422":"https://www.gongju.go.kr/prog/saeolGosi/GOSI_03/sub04_03_03/list.do",cb?1:2,TitleLayout.valueOf(group+"_BOARD"));
    }
}
