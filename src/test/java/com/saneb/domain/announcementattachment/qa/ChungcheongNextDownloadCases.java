package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class ChungcheongNextDownloadCases {
    static final Set<String> GROUPS=Set.of("JINCHEON","TAEAN");
    static ObservationCase selectCase(String group) {
        if(!GROUPS.contains(group))throw new IllegalArgumentException("GROUP_REQUIRED");boolean jin=group.equals("JINCHEON");
        var c=new ChungcheongNextAttachmentProfileConfiguration();var p=jin?c.selectJincheonProfileDetails():c.selectTaeanProfileDetails();
        String id=jin?"29107":"44242";
        String base="https://"+p.selectApprovedHosts().iterator().next()+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        String url=base+"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck="+(jin?"Y":"N");
        String list=base+"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code="+(jin?"01":"01%2C02%2C03%2C04%2C05")+"&pageIndex=1&subCheck="+(jin?"Y":"N")+"&yyyy=&list_gubun=A";
        String title=jin?"2021년 청년4-H회원 창업 성공모델 지원 공모사업 추진계획":"2026년 태안군 소상공인 사업장 시설개선 지원사업 공고";
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,1,jin?TitleLayout.JINCHEON_BOARD:TitleLayout.HWACHEON_LABEL);
    }
}
