package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

/** 공식 지원 검색에서 확인한 표본. 기존 제목 중단 표본과 처리기를 변경하지 않는다. */
final class YuseongSupportDownloadCases {
    static ObservationCase selectCase() {
        var profile = new DaejeonNextAttachmentProfileConfiguration().selectYuseongProfileDetails();
        String url = "https://www.yuseong.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do?notAncmtMgtNo=49380";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        var binding = profile.selectSourceBindings().getFirst();
        return new ObservationCase("YUSEONG-49380", "2025년 학생승마체험 지원사업 추가모집 안내",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, binding.localSourceCode(), binding.listParserProfileCode()), profile,
                "https://www.yuseong.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do", 2, TitleLayout.YUSEONG_BOARD);
    }
}
