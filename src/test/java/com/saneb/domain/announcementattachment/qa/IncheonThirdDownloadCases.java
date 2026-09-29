package com.saneb.domain.announcementattachment.qa;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;
final class IncheonThirdDownloadCases {
    static final Set<String> GROUPS=Set.of("GEOMDAN","YEONGJONG");
    static ObservationCase selectCase(String group){boolean g=group.equals("GEOMDAN");if(!GROUPS.contains(group))throw new IllegalArgumentException("QA_GROUP_REQUIRED");var c=new IncheonThirdAttachmentProfileConfiguration();var p=g?c.selectGeomdanProfileDetails():c.selectYeongjongProfileDetails();String id=g?"235":"332418",url=g?"https://www.geomdan.go.kr/main/bbs/bbsMsgDetail.do?msg_seq=235&bcd=notice":"https://www.yeongjong.go.kr/main/pst/view.do?pst_id=mn_pub_ntc&pst_sn=332418",list=g?"https://www.geomdan.go.kr/main/community/news/notice.jsp":"https://www.yeongjong.go.kr/main/pst/list.do?pst_id=mn_pub_ntc";var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,g?"청년월세 지원사업 이의신청서":"「2026년 영종구 청년 이사비 지원사업」 지원 대상자 모집 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,1,g?TitleLayout.GEOMDAN_BOARD:TitleLayout.YEONGJONG_BOARD);}
}
