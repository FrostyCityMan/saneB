package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class CapitalSixthDownloadCases {
    static final Set<String> GROUPS=Set.of("SIHEUNG","ANSAN");
    static ObservationCase selectCase(String group) {
        var site=Site.valueOf(group);var config=new CapitalSixthAttachmentProfileConfiguration();
        var profile=site==Site.SIHEUNG?config.selectSiheungProfileDetails():config.selectAnsanProfileDetails();
        String id=site==Site.SIHEUNG?"82127":"1660335";
        String title=site==Site.SIHEUNG?"2026년 시흥시 일반 소상공인 특례보증 및 이차보전 지원사업 공고":"2026년도 안산시 소상공인 특례보증 지원 계획 공고";
        String url="https://"+site.host+site.path+"?"+site.menuKey+"="+site.menu+"&"+site.idKey+"="+id;
        String list="https://"+site.host+(site==Site.SIHEUNG?"/main/saeol/gosi/list.do?mId=0401040100":"/www/common/bbs/selectPageListBbs.do?bbs_code=WWW13");
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,site.sourceCode,site.parser),profile,list,1,TitleLayout.valueOf(group+"_PORTAL"));
    }
}
