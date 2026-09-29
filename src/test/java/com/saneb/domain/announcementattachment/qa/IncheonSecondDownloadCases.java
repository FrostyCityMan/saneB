package com.saneb.domain.announcementattachment.qa;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;
final class IncheonSecondDownloadCases {
    static final Set<String> GROUPS=Set.of("JEMULPO","MICHUHOL");
    static ObservationCase selectCase(String group){boolean je=group.equals("JEMULPO");if(!GROUPS.contains(group))throw new IllegalArgumentException("QA_GROUP_REQUIRED");var c=new IncheonSecondAttachmentProfileConfiguration();var p=je?c.selectJemulpoProfileDetails():c.selectMichuholProfileDetails();String id=je?"14094":"309943",url=je?"https://www.jemulpo.go.kr/main/bbs/bbsMsgDetail.do?msg_seq=14094&bcd=announce":"https://www.michuhol.go.kr/main/board/view.do?sq=309943&board_code=board_13",list=je?"https://www.jemulpo.go.kr/main/information/news/announce.jsp":"https://www.michuhol.go.kr/main/board/list.do?board_code=board_13";var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,je?"2026년 제물포구 청년 컬처페이 지원사업 참여자 모집공고":"2026년 청년커뮤니티 지원 사업 참여자 모집 공고(추가모집)",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,3,je?TitleLayout.JEMULPO_BOARD:TitleLayout.MICHUHOL_BOARD);}
}
