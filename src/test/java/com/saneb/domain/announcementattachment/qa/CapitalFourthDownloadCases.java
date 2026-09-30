package com.saneb.domain.announcementattachment.qa;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;
final class CapitalFourthDownloadCases {
    static final Set<String> GROUPS=Set.of("GIMPO","DONGDUCHEON","PYEONGTAEK");
    /** 기존44784 제목 중단 표본을 대체하지 않는 공식 청년 지원 공고다. */
    static ObservationCase selectDongducheonYouthCase(){
        String url="https://www.ddc.go.kr/ddc/selectGosiData.do?key=340&not_ancmt_mgt_no=45339&not_ancmt_se_code=04";
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("DONGDUCHEON-45339","2026년 동두천시 청년구직비용 패키지 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000112","SAEOL_GOSI"),
                new CapitalFourthAttachmentProfileConfiguration().selectDongducheonProfileDetails(),
                "https://www.ddc.go.kr/ddc/selectGosiList.do?key=340&not_ancmt_se_code=04",1,TitleLayout.DONGDUCHEON_PORTAL);
    }
    static ObservationCase selectCase(String group){var site=Site.valueOf(group);var config=new CapitalFourthAttachmentProfileConfiguration();
        var p=switch(site){case GIMPO->config.selectGimpoProfileDetails();case DONGDUCHEON->config.selectDongducheonProfileDetails();case PYEONGTAEK->config.selectPyeongtaekProfileDetails();};
        String id=switch(site){case GIMPO->"73069";case DONGDUCHEON->"44784";case PYEONGTAEK->"95902";};
        String title=switch(site){case GIMPO->"2026년 소상공인 운전자금 이자차액 지원계획 공고";case DONGDUCHEON->"2026년 동두천시 소상공인 경영환경개선사업 공고";case PYEONGTAEK->"2024년 소상공인 특례보증 및 이차보전(이자일부 지원) 계획 공고";};
        String url="https://"+site.host+site.path+"?"+site.menuKey+"="+site.menu+"&"+site.idKey+"="+id+(site==Site.GIMPO?"&cate_cd=1":site==Site.DONGDUCHEON?"&not_ancmt_se_code=04":"");
        String list="https://"+site.host+switch(site){case GIMPO->"/portal/ntfcPblancList.do?key=1004&cate_cd=1&searchCnd=40900000000";case DONGDUCHEON->"/ddc/selectGosiList.do?key=340&not_ancmt_se_code=04";case PYEONGTAEK->"/pyeongtaek/saeol/gosi/list.do?mid=0401020100&seCode=01";};
        var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,site.sourceCode,"SAEOL_GOSI"),p,list,site==Site.DONGDUCHEON?3:1,TitleLayout.valueOf(group+"_PORTAL"));
    }
}
