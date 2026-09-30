package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.IcheonAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class IcheonDownloadCases {
    static ObservationCase selectCase() {
        String raw = "https://www.icheon.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=70639&mid=0402020000";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = normalizer.canonicalizeUrl(raw);
        return new ObservationCase("ICHEON-70639","2027년 지방보조금 지원계획 (소상공인 지원) 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000105","SAEOL_GOSI"),
                new IcheonAttachmentDiscoveryProfile(),"https://www.icheon.go.kr/portal/contents.do?mid=0402000000",1,TitleLayout.ICHEON_BOARD);
    }
}
