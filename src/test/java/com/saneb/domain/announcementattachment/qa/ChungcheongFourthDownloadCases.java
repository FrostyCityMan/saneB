package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class ChungcheongFourthDownloadCases {
    static final Set<String> GROUPS=Set.of("HONGSEONG","YESAN");
    static ObservationCase selectCase(String group){
        var s=Site.valueOf(group);var c=new ChungcheongFourthAttachmentProfileConfiguration();boolean hong=s==Site.HONGSEONG;
        String id=hong?"44858":"47075",title=hong?"2026년 2분기분 '소상공인 사회보험료' 지원사업 공고":"2026년 소상공인 화재보험료 지원사업 공고(2차)";
        String url="https://"+s.host+s.path+"?notAncmtMgtNo="+id;var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.sourceCode,s.parser),hong?c.selectHongseongProfileDetails():c.selectYesanProfileDetails(),"https://"+s.host+s.listPath,hong?1:2,TitleLayout.valueOf(group+"_BOARD"));
    }
}
