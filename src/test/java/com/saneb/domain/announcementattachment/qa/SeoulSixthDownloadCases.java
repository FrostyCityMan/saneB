package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class SeoulSixthDownloadCases {
    static final Set<String> GROUPS=Set.of("YANGCHEON","GWANAK");
    static ObservationCase selectCase(String group){var c=new SeoulSixthAttachmentProfileConfiguration();AttachmentDiscoveryProfile p;String id,title,url,list;int count;TitleLayout layout;
        switch(group){
            case "YANGCHEON"->{p=c.selectYangcheonProfileDetails();id="46207";title="2026년 양천구 북한이탈주민 학생 학습지원사업 추가모집 공고";url="https://www.yangcheon.go.kr/site/yangcheon/ex/seol/seolContentDeailView.do?not_ancmt_mgt_no=46207";list="https://www.yangcheon.go.kr/site/yangcheon/ex/seol/seolCollectList.do";count=3;layout=TitleLayout.YANGCHEON_BOARD;}
            case "GWANAK"->{p=c.selectGwanakProfileDetails();id="41842";title="『관악구 소상공인 원스톱 지원사업』 소상공인 모집 공고";url="https://www.gwanak.go.kr/site/gwanak/ex/bbsNew/View.do?typeCode=1&not_ancmt_mgt_no=41842";list="https://www.gwanak.go.kr/site/gwanak/ex/bbsNew/List.do?typeCode=1";count=2;layout=TitleLayout.GWANAK_BOARD;}
            default->throw new IllegalArgumentException("QA_GROUP_REQUIRED");
        }
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,count,layout);
    }
}
