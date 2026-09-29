package com.saneb.domain.announcementattachment.qa;

import java.util.List;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

/** 실제 공식 목록에서 선택한 첫 표본. 운영 정책/기대값 승인이 아니다. */
final class JeonbukFirstDownloadCases {
    static final List<String> GROUPS=List.of("IKSAN","WANJU","JINAN","MUJU","JANGSU","IMSIL","SUNCHANG");
    static ObservationCase selectCase(String group){
        var config=new JeonbukAttachmentProfileConfiguration();
        var profile=switch(group){case "IKSAN"->config.selectIksanProfileDetails();case "WANJU"->config.selectWanjuProfileDetails();case "JINAN"->config.selectJinanProfileDetails();case "MUJU"->config.selectMujuProfileDetails();case "JANGSU"->config.selectJangsuProfileDetails();case "IMSIL"->config.selectImsilProfileDetails();case "SUNCHANG"->config.selectSunchangProfileDetails();default->throw new IllegalArgumentException("UNKNOWN_FIXED_GROUP");};
        String id=switch(group){case "IKSAN"->"74400";case "WANJU"->"43355";case "JINAN"->"33307";case "MUJU"->"34488";case "JANGSU"->"32491";case "IMSIL"->"33179";default->"31739";};
        String title=switch(group){case "IKSAN"->"2026년 영세소상공인 카드수수료 지원사업 공고";case "WANJU"->"2026년 완주군 소상공인 카드수수료 지원사업 공고";case "JINAN"->"2026년 소상공인 카드수수료 지원사업 수정 공고";case "MUJU"->"2026년 소상공인 카드형 무주사랑상품권 결제수수료 지원사업 공고";case "JANGSU"->"2026년 영세소상공인 카드수수료 지원사업 공고 알림";case "IMSIL"->"2026년 임실군 소상공인 희망더드림 특례보증 및 이차보전 지원 사업 공고";default->"2026년 소상공인 카드수수료 지원사업 공고";};
        String list=switch(group){case "IKSAN"->"https://eminwon.iksan.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,02,03,04,05&cpath=";case "WANJU"->"https://www.wanju.go.kr/index.9is?contentUid=ff8080818b024d8e018b274f41c32af7";case "JINAN"->"https://www.jinan.go.kr/index.jinan?menuCd=DOM_000000107001014000";case "MUJU"->"https://www.muju.go.kr/index.9is?contentUid=ff8080816c5f9d47016cbd3b2a4a006f";case "JANGSU"->"https://www.jangsu.go.kr/index.jangsu?menuCd=DOM_000000102001005000";case "IMSIL"->"https://www.imsil.go.kr/index.imsil?menuCd=DOM_000000103001005000";default->"https://eminwon.sunchang.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,02,03,04,05";};
        var layout=switch(group){case "IKSAN","IMSIL"->TitleLayout.HWACHEON_LABEL;case "JINAN"->TitleLayout.HAMAN_LABEL;case "JANGSU"->TitleLayout.DANYANG_LABEL;case "WANJU","MUJU"->TitleLayout.FILEBOX_LABEL;default->TitleLayout.SUNCHANG_LABEL;};
        String url="https://"+profile.selectApprovedHosts().iterator().next()+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var b=profile.selectSourceBindings().getFirst();var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),profile,list,group.equals("JANGSU")?2:1,layout);
    }
}
