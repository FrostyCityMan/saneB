package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class SeoulFifthDownloadCases {
    static final Set<String> GROUPS=Set.of("DONGDAEMUN","SEONGBUK","YEONGDEUNGPO");
    static ObservationCase selectCase(String group){var c=new SeoulFifthAttachmentProfileConfiguration();AttachmentDiscoveryProfile p;String id,title,url,list;int count;TitleLayout layout;
        switch(group){
            case "DONGDAEMUN"->{p=c.selectDongdaemunProfileDetails();id="22587";title="2026년 의류제조업체 간판 제작 지원 사업(공모)";url="https://www.ddm.go.kr/www/selectEminwonWebView.do?key=3291&searchNotAncmtSeCode=01%2C02%2C04%2C05%2C06%2C07&notAncmtMgtNo=22587";list="https://www.ddm.go.kr/www/selectEminwonWebList.do?key=3291&searchNotAncmtSeCode=01%2C02%2C04%2C05%2C06%2C07";count=2;layout=TitleLayout.DONGDAEMUN_BOARD;}
            case "SEONGBUK"->{p=c.selectSeongbukProfileDetails();id="43006";title="2026년 성북구 소상공인 사회보험료 지원 시행 공고";url="https://www.sb.go.kr/www/selectEminwonView.do?key=6920&notAncmtMgtNo=43006";list="https://www.sb.go.kr/www/selectEminwonList.do?bbsNo=41&key=6920";count=3;layout=TitleLayout.SEONGBUK_BOARD;}
            case "YEONGDEUNGPO"->{p=c.selectYeongdeungpoProfileDetails();id="38256";title="2026년 청년 국가자격시험 응시료 지원 사업 공고(수정)";url="https://www.ydp.go.kr/www/selectEminwonView.do?menuFlag=01&notAncmtMgtNo=38256&key=2851";list="https://www.ydp.go.kr/www/selectEminwonList.do?key=2851&menuFlag=01";count=2;layout=TitleLayout.YEONGDEUNGPO_BOARD;}
            default->throw new IllegalArgumentException("QA_GROUP_REQUIRED");
        }
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,count,layout);
    }
}
