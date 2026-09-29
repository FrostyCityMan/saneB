package com.saneb.domain.announcementattachment.qa;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;
final class DaejeonNextDownloadCases {
    static final Set<String> GROUPS=Set.of("YUSEONG","DAEDEOK");
    static ObservationCase selectCase(String group){boolean y=group.equals("YUSEONG");if(!GROUPS.contains(group))throw new IllegalArgumentException("QA_GROUP_REQUIRED");var c=new DaejeonNextAttachmentProfileConfiguration();var p=y?c.selectYuseongProfileDetails():c.selectDaedeokProfileDetails();String id=y?"35533":"1102472206",url=y?"https://www.yuseong.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do?notAncmtMgtNo=35533":"https://www.daedeok.go.kr/dpt/dpt04/DPT040204_cmmBoardView.do?boardId=DPT_000087&pageIndex=1&ntatcSeq=1102472206",list=y?"https://www.yuseong.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do":"https://www.daedeok.go.kr/dpt/dpt04/DPT040204_cmmBoardList.do";var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,y?"[2021년 청년, 신혼부부 전세임대 입주자 모집] 공고":"2026년 대덕뱅크(소상공인 대출지원사업) 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,y?4:3,y?TitleLayout.YUSEONG_BOARD:TitleLayout.DAEDEOK_BOARD,y?TitleStageCode.COMBINATION_NOT_MATCHED:null);}
}
