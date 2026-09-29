package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class ChungcheongSixthDownloadCases {
    static final Set<String> GROUPS=Set.of("ASAN","SEOSAN");
    static ObservationCase selectCase(String group){
        var s=Site.valueOf(group);var c=new ChungcheongSixthAttachmentProfileConfiguration();boolean asan=s==Site.ASAN;
        String id=asan?"80727":"65309",title=asan?"2026년 청년농업인 스마트팜 구축지원사업 공모 연장 알림":"2026년 서산시 소상공인 화재보험료 지원사업(2차)";
        String url="https://"+s.host+s.path+(asan?"?no=257&m_mode=view&mgt_no="+id:"?jndinm=OfrNotAncmtEJB&context=NTIS&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&homepage_pbs_yn=Y&subCheck=Y");var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.sourceCode,s.parser),asan?c.selectAsanProfileDetails():c.selectSeosanProfileDetails(),asan?"https://www.asan.go.kr/main/cms/?no=257":"https://www.seosan.go.kr/www/contents.do?key=1258",2,TitleLayout.valueOf(group+"_BOARD"));
    }
}
