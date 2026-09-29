package com.saneb.domain.announcementattachment.qa;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;
final class MetroNextDownloadCases {
    static final Set<String> GROUPS=Set.of("GWANGJU_NAMGU","DAEJEON_JUNGGU");
    static ObservationCase selectCase(String group){boolean g=group.equals("GWANGJU_NAMGU");if(!GROUPS.contains(group))throw new IllegalArgumentException("QA_GROUP_REQUIRED");var c=new MetroNextAttachmentProfileConfiguration();var p=g?c.selectGwangjuNamguProfileDetails():c.selectDaejeonJungguProfileDetails();String id=g?"45698":"46404",url=g?"https://eminwon.namgu.gwangju.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=45698&subCheck=Y":"https://www.djjunggu.go.kr/prog/saeolGosi/GOSI/sub03_06/view.do?notAncmtMgtNo=46404",list=g?"https://www.namgu.gwangju.kr/menu.es?mid=a10604020100":"https://www.djjunggu.go.kr/prog/saeolGosi/GOSI/sub03_06/list.do";var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,g?"2026년 「청년 1인가구 주거 안심물품 지원」대상자 모집 공고":"2026년 대전광역시 중구 소상공인 특례보증 지원사업 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,g?2:1,g?TitleLayout.GWANGJU_NAMGU_BOARD:TitleLayout.DAEJEON_JUNGGU_BOARD);}
}
