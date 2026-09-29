package com.saneb.domain.announcementattachment.qa;

import java.util.List;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

/** 공식 목록에서 고정한 표본. 원문 수집과 최종 정책 승인은 구분한다. */
final class JeonnamSecondDownloadCases {
    static final List<String> GROUPS=List.of("GOKSEONG","JINDO");
    static ObservationCase selectCase(String group){
        var c=new JeonnamAttachmentProfileConfiguration();var p=switch(group){case "GOKSEONG"->c.selectGokseongProfileDetails();case "JINDO"->c.selectJindoProfileDetails();default->throw new IllegalArgumentException("UNKNOWN_FIXED_GROUP");};
        boolean g=group.equals("GOKSEONG");String id=g?"34854":"25159";
        String url=g?"https://www.gokseong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y":"https://www.jindo.go.kr/home/gosi/general.cs?act=view&notAncmtMgtNo="+id;
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,g?"2026년 소상공인 온라인 마케팅 비용 지원 사업 알림":"2026년 소상공인 융자금 이차보전 지원사업 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,g?"https://www.gokseong.go.kr/board/GosiList.do?menuNo=102001003000":"https://www.jindo.go.kr/home/gosi/general.cs?m=878",1,g?TitleLayout.GOKSEONG_BOARD:TitleLayout.JINDO_BOARD);
    }
}
