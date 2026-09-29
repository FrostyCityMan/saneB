package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class CapitalSeventhDownloadCases {
    static final Set<String> GROUPS=Set.of("POCHEON","GANGNEUNG");
    static ObservationCase selectCase(String group) {
        var site=Site.valueOf(group);var config=new CapitalSeventhAttachmentProfileConfiguration();
        var profile=site==Site.POCHEON?config.selectPocheonProfileDetails():config.selectGangneungProfileDetails();
        String id=site==Site.POCHEON?"64129":"60798";
        String title=site==Site.POCHEON?"2026년 포천시 소상공인 특례보증 및 이차보전 지원계획 공고":"2026년 소상공인 카드수수료 지원사업 추가접수 공고";
        String url="https://"+site.host+site.path+"?key="+site.menu+"&"+site.idKey+"="+id+"&"+site.typeKey+"="+site.type;
        String list="https://"+site.host+site.path.replace("View.do","List.do")+"?key="+site.menu+"&"+site.typeKey+"="+site.type;
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,site.sourceCode,"SAEOL_GOSI"),profile,list,1,TitleLayout.valueOf(group+"_PORTAL"));
    }
}
