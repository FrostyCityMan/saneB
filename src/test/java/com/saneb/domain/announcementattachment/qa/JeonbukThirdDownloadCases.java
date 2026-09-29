package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage.Site;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class JeonbukThirdDownloadCases {
    static final Set<String> GROUPS=Set.of("JEONJU","JEONBUK");
    static ObservationCase selectCase(String group){
        var s=Site.valueOf(group);var config=new JeonbukThirdAttachmentProfileConfiguration();boolean j=s==Site.JEONJU;
        String id=j?"7c37d618236741a28b133aa279d05c63":"670379",title=j?"2026년도 소상공인 카드수수료 지원사업 연장공고":"26년도 전북특별자치도 중소기업 육성자금 융자지원계획 수정 공고";
        String url="https://"+s.host+s.path+"?"+s.boardKey+"="+s.board+"&"+s.menuKey+"="+s.menu+"&"+s.idKey+"="+id;var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id.toUpperCase(java.util.Locale.ROOT),title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.sourceCode,"SPRING_BBS"),j?config.selectJeonjuProfileDetails():config.selectJeonbukProfileDetails(),j?"https://www.jeonju.go.kr/index.9is?contentUid=ff8080818990c349018b041a879f395a":"https://www.jeonbuk.go.kr/index.jeonbuk?menuCd=DOM_000000102002000000",1,TitleLayout.valueOf(group+"_THIRD_BOARD"));
    }
}
