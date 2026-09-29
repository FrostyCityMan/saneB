package com.saneb.domain.announcementattachment.qa;

import java.util.List;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

/** 공식 목록에서 확인한 고정 표본. 파일 수집만 관측하며 기대값 승인을 대체하지 않는다. */
final class JeonbukSecondDownloadCases {
    static final List<String> GROUPS=List.of("GUNSAN","JEONGEUP","NAMWON","BUAN","GOCHANG");
    static ObservationCase selectCase(String group){
        var c=new JeonbukAttachmentProfileConfiguration();
        var p=switch(group){case "GUNSAN"->c.selectGunsanProfileDetails();case "JEONGEUP"->c.selectJeongeupProfileDetails();case "NAMWON"->c.selectNamwonProfileDetails();case "BUAN"->c.selectBuanProfileDetails();case "GOCHANG"->c.selectGochangProfileDetails();default->throw new IllegalArgumentException("UNKNOWN_FIXED_GROUP");};
        String id=switch(group){case "GUNSAN"->"71315";case "JEONGEUP"->"50232";case "NAMWON"->"33037092911b4bd8be3b8c15657d8aef";case "BUAN"->"363827";default->"807493";};
        String title=switch(group){case "GUNSAN"->"2026년 군산시 영세 소상공인 카드수수료 지원사업 공고";case "JEONGEUP"->"2026년 정읍시 소상공인 안정지원금 미신청자 지원사업 공고";case "NAMWON"->"2026년 남원시 소상공인 희망더드림 특례보증 지원사업 재공고";case "BUAN"->"2026년 소상공인 카드수수료 지원사업 공고(추가)";default->"2026년 소상공인 카드수수료 지원사업 시행 공고";};
        String url=switch(group){case "NAMWON"->"https://www.namwon.go.kr/board/post/view.do?boardUid=ff8080818ea1fec5018ea24137680031&menuUid=ff8080818e3beff0018e4077131b007a&postUid="+id;case "BUAN"->"https://www.buan.go.kr/board/view.buan?boardId=BBS_0000054&menuCd=DOM_000000103001003000&dataSid="+id;case "GOCHANG"->"https://www.gochang.go.kr/board/view.gochang?boardId=BBS_0000180&menuCd=DOM_000000102003007000&dataSid="+id;default->"http://eminwon."+group.toLowerCase(java.util.Locale.ROOT)+".go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";};
        String list=switch(group){case "GUNSAN"->"http://eminwon.gunsan.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,02,03,04,05";case "JEONGEUP"->"http://eminwon.jeongeup.go.kr/emwp/jsp/ofr/OfrNotAncmtL.jsp?not_ancmt_se_code=01,02,03,04,05";case "NAMWON"->"https://www.namwon.go.kr/index.do?menuUid=ff8080818e3beff0018e4077131b007a";case "BUAN"->"https://www.buan.go.kr/index.buan?menuCd=DOM_000000103001003000";default->"http://www.gochang.go.kr/index.gochang?menuCd=DOM_000000102003000000";};
        var layout=switch(group){case "GUNSAN"->TitleLayout.HWACHEON_LABEL;case "JEONGEUP"->TitleLayout.HAMAN_LABEL;case "NAMWON"->TitleLayout.NAMWON_BOARD;default->TitleLayout.JEONBUK_BOARD;};
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id.toUpperCase(java.util.Locale.ROOT),title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,1,layout);
    }
}
