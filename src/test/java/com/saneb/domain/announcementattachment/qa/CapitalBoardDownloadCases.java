package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class CapitalBoardDownloadCases {
    static final Set<String> GROUPS=Set.of("YANGJU","GUNPO","YEOJU");
    static ObservationCase selectCase(String group){
        var site=Site.valueOf(group);var config=new CapitalBoardAttachmentProfileConfiguration();
        var p=switch(site){case YANGJU->config.selectYangjuProfileDetails();case GUNPO->config.selectGunpoProfileDetails();case YEOJU->config.selectYeojuProfileDetails();};
        String id=switch(site){case YANGJU->"65326";case GUNPO->"43659";case YEOJU->"51859";};
        String title=switch(site){case YANGJU->"2026년 양주시 중소기업 및 소상공인 운전자금 이차보전 지원 공고";case GUNPO->"2026년도 군포시 소상공인 특례보증(이차보전) 지원계획 공고";case YEOJU->"2026년 여주시 청년 주택 임차보증금 대출이자 지원사업 대상자 모집";};
        String url="https://"+site.host+"/www/selectEminwonView.do?key="+site.menu+"&not_ancmt_mgt_no="+id+(site==Site.GUNPO?"&Not_ancmt_se_code=01&notAncmtSeCd=01":"");
        String list="https://"+site.host+"/www/"+(site==Site.GUNPO?"selectEminwonNoticeList.do?key=3907&Not_ancmt_se_code=01&list_gubun=N&ofr_pageSize=10&notAncmtSeCd=01&pageUnit=10":"selectEminwonList.do?key="+site.menu);
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,site.sourceCode,"SPRING_BBS"),p,list,1,TitleLayout.valueOf(group+"_PORTAL"));
    }
}
