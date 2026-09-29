package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class CapitalFifthDownloadCases {
    static final Set<String> GROUPS=Set.of("ANSEONG","UIJEONGBU","GG_GWANGJU");
    static ObservationCase selectCase(String group) {
        var site=Site.valueOf(group); var config=new CapitalFifthAttachmentProfileConfiguration();
        var profile=switch(site) { case ANSEONG->config.selectAnseongProfileDetails(); case UIJEONGBU->config.selectUijeongbuProfileDetails(); case GG_GWANGJU->config.selectGgGwangjuProfileDetails(); };
        String id=switch(site) { case ANSEONG->"72476"; case UIJEONGBU->"66681"; case GG_GWANGJU->"75337"; };
        String title=switch(site) { case ANSEONG->"2026년 안성시 소상공인 카드수수료 지원 사업 실시 공고"; case UIJEONGBU->"「2026년 청년 AI소프트웨어 구독비용 지원」 공고"; case GG_GWANGJU->"2026년도 광주시 소상공인 특례보증 지원계획 공고"; };
        String url="https://"+site.host+site.path+"?mId="+site.menu+"&notAncmtMgtNo="+id;
        String list="https://"+site.host+site.path.replace("gosiView.do","gosiList.do").replace("/view.do","/list.do")+"?mId="+site.menu+"&seCode=01";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,site.sourceCode,site.parser),profile,list,site==Site.ANSEONG?3:1,TitleLayout.valueOf(group+"_PORTAL"));
    }
}
