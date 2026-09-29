package com.saneb.domain.announcementattachment.qa;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;
final class CapitalEighthDownloadCases {
    static final Set<String> GROUPS=Set.of("PAJU","GWANGMYEONG");
    static ObservationCase selectCase(String group){if(!GROUPS.contains(group))throw new IllegalArgumentException("QA_GROUP_REQUIRED");boolean p=group.equals("PAJU");var config=new CapitalEighthAttachmentProfileConfiguration();var profile=p?config.selectPajuProfileDetails():config.selectGwangmyeongProfileDetails();String id=p?"20260119101158902":"65908",url=p?"https://www.paju.go.kr/user/board/BD_board.view.do?bbsCd=1022&seq=20260119101158902&q_ctgCd=4063":"https://www.gm.go.kr/pt/user/nftcBbs/BD_selectNftcBbsDetail.do?q_nftcBbsCode=1001&q_nftcBbsMgtno=65908&q_currPage=1",list=p?"https://www.paju.go.kr/user/board/BD_board.list.do?bbsCd=1022&q_ctgCd=4063":"https://www.gm.go.kr/pt/user/nftcBbs/BD_selectNftcBbsList.do?q_nftcBbsCode=1001";var n=new AnnouncementSourceIdentityNormalizer();var b=profile.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,p?"2026년 파주시 소상공인 운전자금 지원계획 공고":"2026년 소상공인 경영환경개선 지원사업 모집 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),profile,list,p?1:3,p?TitleLayout.PAJU_BOARD:TitleLayout.GWANGMYEONG_BOARD);}
}
