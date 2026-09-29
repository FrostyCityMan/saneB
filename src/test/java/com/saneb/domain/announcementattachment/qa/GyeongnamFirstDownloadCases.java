package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongnamFirstDownloadCases {
    static final Set<String> GROUPS=Set.of("GORYEONG","JINJU");
    static ObservationCase selectCase(String group){var c=new GoryeongJinjuAttachmentProfileConfiguration();if(!GROUPS.contains(group))throw new IllegalArgumentException("GROUP_REQUIRED");boolean gr=group.equals("GORYEONG");var p=gr?c.selectGoryeongProfileDetails():c.selectJinjuProfileDetails();String id=gr?"42015":"64420",url=gr?"http://www.goryeong.go.kr/kor/boardView.do?IDX=154&BRD_ID=1023&BOARD_IDX="+id:"https://www.jinju.go.kr/00130/02730/05586.web?amode=view&not_ancmt_mgt_no="+id;
        String list=gr?"https://www.goryeong.go.kr/kor/boardList.do?IDX=154&BRD_ID=1023":"https://www.jinju.go.kr/00130/02730/05586.web",title=gr?"2026년 소상공인 새바람 체인지업 지원사업 추가모집 공고(안내)":"2026년 하반기 진주시 소상공인 육성자금 지원 공고";var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,gr?3:1,gr?TitleLayout.GORYEONG_BOARD:TitleLayout.JINJU_BOARD);}
}
