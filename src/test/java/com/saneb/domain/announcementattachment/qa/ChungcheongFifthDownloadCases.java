package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class ChungcheongFifthDownloadCases {
    static final Set<String> GROUPS=Set.of("GEUMSAN","BUYEO");
    static ObservationCase selectCase(String group){
        var s=Site.valueOf(group);var c=new ChungcheongFifthAttachmentProfileConfiguration();boolean geum=s==Site.GEUMSAN;
        String id=geum?"ea6a4e53c07c7f05a9e9240dbb006d43":"2125452",title=geum?"2026년 금산군 소상공인 화재보험료 2차 지원사업 공고":"충남 소상공인 사회보험료 2026년 2분기 지원사업 공고";
        String url="https://"+s.host+s.path+"?mode=V"+(geum?"&site_dvs_cd=kr":"")+"&mng_no="+id;var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id.toUpperCase(java.util.Locale.ROOT),title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.sourceCode,s.parser),geum?c.selectGeumsanProfileDetails():c.selectBuyeoProfileDetails(),"https://"+s.host+s.listPath,2,TitleLayout.valueOf(group+"_BOARD"));
    }
}
