package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import java.util.Set;

/** 공식 목록에서 확인한 추가 표본. 이전 상세 실패·정책 기대값은 변경하지 않는다. */
final class RecoveredSupportDownloadCases {
    static final Set<String> GROUPS=Set.of("GANGNAM_SUPPORT", "GEUMSAN_SUPPORT", "GEOCHANG_SUPPORT", "MICHUHOL_SUPPORT", "ASAN_SUPPORT", "CHEONAN_SUPPORT");

    static ObservationCase selectCase(String group) {
        ObservationCase old;
        String id, title, url;
        int files;
        switch(group) {
            case "GANGNAM_SUPPORT" -> {
                old=SeoulEighthDownloadCases.selectCase("GANGNAM");
                id="61922";
                title="2026년 강남구 미취업 청년 어학・자격증 응시료 지원사업 시행 공고";
                url="https://www.gangnam.go.kr/notice/view.do?not_ancmt_mgt_no="+id+"&mid=ID05_040201";
                files=2;
            }
            case "GEUMSAN_SUPPORT" -> {
                old=ChungcheongFifthDownloadCases.selectCase("GEUMSAN");
                id="4df078b6fceb5d17b3162ce865ecbf3b";
                title="금산군 소상공인 사회보험료 2026년 1분기분 지원사업 공고(변경)";
                url="https://www.geumsan.go.kr/site/kr/html/sub03/030302.html?mode=V&site_dvs_cd=kr&mng_no="+id;
                files=2;
            }
            case "GEOCHANG_SUPPORT" -> {
                old=GeochangDownloadCases.selectCase();
                id="45659";
                title="2026년 거창군 청년 구직자 자격증 취득 응시료 지원사업";
                url="https://www.geochang.go.kr/00445/00451.web?amode=view&not_ancmt_mgt_no="+id;
                files=1;
            }
            case "MICHUHOL_SUPPORT" -> {
                old=IncheonSecondDownloadCases.selectCase("MICHUHOL");
                id="315063";
                title="2026년도 중소기업 육성 및 소상공인 지원사업 융자계획 공고";
                url="https://www.michuhol.go.kr/main/board/view.do?board_code=board_13&sq="+id;
                files=1;
            }
            case "ASAN_SUPPORT" -> {
                old=ChungcheongSixthDownloadCases.selectCase("ASAN");
                id="76469";
                title="2026년 친환경농산물 인증비 지원사업 신청 공고";
                url="https://www.asan.go.kr/main/cms/?no=257&m_mode=view&mgt_no="+id;
                files=2;
            }
            case "CHEONAN_SUPPORT" -> {
                old=CheonanDownloadCases.selectCase();
                id="113864";
                title="(재공고)청소년 인성교육 및 고3 수능이후 생활지도 사업 지방보조금 지원계획";
                url="https://eminwon.cheonan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
                files=1;
            }
            default -> throw new IllegalArgumentException("QA_GROUP_REQUIRED");
        }
        var n=new AnnouncementSourceIdentityNormalizer();
        var source=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,
                old.source().localSourceCode(),old.source().listParserProfileCode());
        return new ObservationCase(group.replace("_SUPPORT", "")+"-"+id.toUpperCase(java.util.Locale.ROOT),title,
                source,old.profile(),old.listUrl(),files,old.titleLayout());
    }
}
