package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class IncheonFirstDownloadCases {
    static final Set<String> GROUPS=Set.of("GYEYANG","GANGHWA");
    static ObservationCase selectCase(String group){boolean gy=group.equals("GYEYANG");if(!GROUPS.contains(group))throw new IllegalArgumentException("QA_GROUP_REQUIRED");var c=new IncheonPortalAttachmentProfileConfiguration();var p=gy?c.selectGyeyangProfileDetails():c.selectGanghwaProfileDetails();String id=gy?"53151":"52786",host=gy?"www.gyeyang.go.kr":"www.ganghwa.go.kr",url="https://"+host+"/open_content/main/eminwon/announce/eminwonDetail.do?seq="+id,list="https://"+host+(gy?"/open_content/main/open_info/admin/gosi.jsp":"/open_content/main/ganghwa/news/announce.jsp");var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,gy?"2026년도 중소기업육성기금 융자지원 계획 공고":"소상공인 경영안정지원금 지급사업 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,gy?4:1,gy?TitleLayout.GYEYANG_PORTAL:TitleLayout.GANGHWA_PORTAL);}
}
