package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class DaeguCityDownloadCases {
    static ObservationCase selectCase() {
        var profile = new DaeguCityAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.daegu.go.kr/index.do?menu_id=00940170&menu_link=/front/daeguSidoGosi/daeguSidoGosiView.do&sno=33505&gosi_gbn=A";
        return new ObservationCase("DAEGU_CITY-33505", "「소기업·소상공인 방역물품 지원금」 시행 연장 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000044", "SAFE_DAEGU_LEGAL_NOTICE"), profile,
                "https://www.daegu.go.kr/index.do?menu_id=00940170", 1, TitleLayout.DAEGU_CITY_BOARD);
    }
}
