package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class CapitalThirdDownloadCases {
    static final Set<String> GROUPS=Set.of("NAMYANGJU","HANAM","GURI");
    static ObservationCase selectCase(String group){
        var site=Site.valueOf(group);var config=new CapitalThirdAttachmentProfileConfiguration();
        var p=switch(site){case NAMYANGJU->config.selectNamyangjuProfileDetails();case HANAM->config.selectHanamProfileDetails();case GURI->config.selectGuriProfileDetails();};
        String id=switch(site){case NAMYANGJU->"84824";case HANAM->"51521";case GURI->"46706";};
        String title=switch(site){case NAMYANGJU->"2026년 남양주시 소상공인 특례보증 및 이차보전 지원계획 공고";case HANAM->"2026년도 하남시 소상공인 특례보증 지원 확대 계획 공고";case GURI->"2026년 구리시 저신용·저소득 소상공인 이자지원 계획 공고";};
        String fixed=switch(site){case NAMYANGJU->"&sa1Join=01;02;04;05&sc4=2024";case HANAM->"&not_ancmt_se_code=01,04";case GURI->"&searchGosiSe=01,04,06";};
        String url="https://"+site.host+site.path+"?key="+site.menu+"&"+site.idKey+"="+id+fixed;
        String list="https://"+site.host+"/www/"+switch(site){case NAMYANGJU->"selectEminwonWebList.do?key=2492&sa1=01&sa1=02&sa1=04&sa1=05&sc4=2024";case HANAM->"selectGosiList.do?key=171&not_ancmt_se_code=01,04";case GURI->"selectGosiNttList.do?key=387&searchGosiSe=01,04,06";};
        var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,site.sourceCode,"SAEOL_GOSI"),p,list,site==Site.HANAM?2:1,TitleLayout.valueOf(group+"_PORTAL"));
    }
}
