package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GangwonNextDownloadCases {
    static final Set<String> GROUPS=Set.of("DONGHAE","JEONGSEON","GW_GOSEONG","YANGYANG");
    static ObservationCase selectCase(String group) {
        if(!GROUPS.contains(group))throw new IllegalArgumentException("GROUP_REQUIRED");
        var c=new GangwonNextAttachmentProfileConfiguration();
        var p=switch(group){case "DONGHAE"->c.selectDonghaeProfileDetails();case "JEONGSEON"->c.selectJeongseonProfileDetails();case "GW_GOSEONG"->c.selectGangwonGoseongProfileDetails();default->c.selectYangyangProfileDetails();};
        String id=switch(group){case "DONGHAE"->"36513";case "JEONGSEON"->"34952";case "GW_GOSEONG"->"32775";default->"37521";};
        String base="https://"+p.selectApprovedHosts().iterator().next()+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        String url=base+"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        String list=base+"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code="+(Set.of("DONGHAE","GW_GOSEONG").contains(group)?"01%2C04":"01%2C04%2C05%2C06")+"&pageIndex=1&subCheck=Y&yyyy=&list_gubun=A";
        String title=switch(group){case "DONGHAE"->"2025 소상공인 카드수수료 지원 사업 공고";case "JEONGSEON"->"2025년 정선군 소상공인 시설개선 지원사업 공고";case "GW_GOSEONG"->"고성군 소상공인 특례보증수수료 지원사업 (재)공고";default->"2026년 양양군 소상공인 특례보증수수료 지원사업(2차) 공고";};
        var layout=switch(group){case "DONGHAE"->TitleLayout.DONGHAE_BOARD;case "GW_GOSEONG"->TitleLayout.GW_GOSEONG_BOARD;default->TitleLayout.GANGWON_SKIN_BOARD;};
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,Set.of("GW_GOSEONG","JEONGSEON").contains(group)?2:1,layout);
    }
    static ObservationCase selectJeongseonTitleStopCase() {
        var eligible=selectCase("JEONGSEON");String url=eligible.source().sourceUrl().replace("34952","37876");var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("JEONGSEON-37876","2026년도 정선군 소상공인 경영안정 자금 시행 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000128","SAFE_SAEOL_EMINWON"),eligible.profile(),eligible.listUrl(),1,eligible.titleLayout(),
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode.COMBINATION_NOT_MATCHED);
    }
}
