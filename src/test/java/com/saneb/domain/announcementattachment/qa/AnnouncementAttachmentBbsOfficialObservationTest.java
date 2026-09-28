package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.provider.content.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 고정 공식 표본의 세 단계 실제 관측. 기대값 자동 승인·원문 보관·DB/운영 쓰기는 없다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION", matches="true")
public class AnnouncementAttachmentBbsOfficialObservationTest {
    static final String CASE="TAEBAEK-184816";
    static final String TITLE="2026년 청년농업인 육성지원(취업농) 신청자 모집 공고";
    private static final long MIB=1024L*1024;
    private static final ObjectMapper JSON=new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private static final AttachmentDiscoveryProfile PROFILE=new StandardBbsAttachmentProfileConfiguration().selectTaebaekProfileDetails();
    private static final AttachmentDiscoveryProfile.Source SOURCE=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
            "4435df8486622964d09488f35efd579bac83f51eb07b104faf02e5b7bd486492",
            "https://www.taebaek.go.kr/www/selectBbsNttView.do?key=352&bbsNo=25&nttNo=184816","LGS-000121","SPRING_BBS");

    public enum TitleLayout { CLASSIC_LABEL, COMPACT_SUBJECT, COMPACT_LABEL, NAMGU_HEADER, DALSEONG_LABEL, HAMAN_LABEL, JUNGGU_LABEL, GANGBUK_SUBJECT, HWACHEON_LABEL, DONGNAE_LABEL, BUSANJIN_LABEL, GEUMJEONG_HEADER, SUYEONG_HEADING, SASANG_HEADER, HAEUNDAE_HEADING, GIJANG_HEADER, BUSAN_BUKGU_LABEL, BUSAN_GANGSEO_HEADING, SAHA_LABEL, BUSAN_SEOGU_LABEL, YEONGDO_HEADING }
    static final List<String> GANGBUK_LOCATORS=List.of(
            "abef5eff5d1f72128387e8bc15bc114a2a94bf2d22b1bd2ebd9cae04bc51bf4f",
            "20d878c543d793289cbf7845a07cf4bd9c60c20618a6df1a08d3c3e1e3d70672",
            "00cbd4c75b4c32b3a21a206cdfb39dd5b50928b6501d13faf0bcefee7af868f3",
            "894fb3c93a8a1eabfd630e31c1062875c3075a3895aa72e51f8411223394d40d");
    public record ObservationCase(String code,String title,AttachmentDiscoveryProfile.Source source,
                           AttachmentDiscoveryProfile profile,String listUrl,int listedFileCount,TitleLayout titleLayout,
                           TitleStageCode expectedTitleStopStage) {
        public ObservationCase(String code,String title,AttachmentDiscoveryProfile.Source source,
                               AttachmentDiscoveryProfile profile,String listUrl,int listedFileCount,TitleLayout titleLayout) {
            this(code,title,source,profile,listUrl,listedFileCount,titleLayout,null);
        }
        @Override public String toString(){return code;}
    }
    static Stream<ObservationCase> selectConfiguredCases() {
        return selectCases(System.getProperty("saneb.attachment-observation.group","TAEBAEK"));
    }
    public static Stream<ObservationCase> selectCases(String group) {
        if("YEONGDO".equals(group)) return Stream.of(
                selectYeongdoCase("36435","2026년 영도구 소상공인 보증료 지원사업 공고",3),
                selectYeongdoCase("36164","2026년 영도구 청년 면접수당 지원사업 공고(수정)",4),
                selectYeongdoCase("35633","2026년 영도구 청년 면접수당 지원사업 공고",4));
        if("BSJUNGGU".equals(group)) return Stream.of(
                selectBusanJungguSeoguCase(group,"29433","「2026년 중구 청년 1인가구 호신용품 지원사업」 신청 공고",1),
                selectBusanJungguSeoguCase(group,"29432","「2026년 중구 청년 자격증 등 시험 응시료 지원사업」 신청 공고",2),
                selectBusanJungguSeoguCase(group,"29138","중구「2026년 청년 프로그램 지원 사업」모집 공고",1));
        if("BSSEOGU".equals(group)) return Stream.of(
                selectBusanJungguSeoguCase(group,"36796","「2026년 서구 청년 창업자 임차료 지원사업」 참여 신청 수정 공고",3),
                selectBusanJungguSeoguCase(group,"36758","「2026년 서구 청년 창업자 임차료 지원사업」 참여 신청 재공고",3),
                selectBusanJungguSeoguCase(group,"36645","「2026년 서구 청년 창업자 임차료 지원사업」 참여 신청 공고",3));
        if("BSDONGGU".equals(group)) return Stream.of(
                selectBusanDongguSahaCase(group,"32674","「2026년 부산 동구 청년 자격증 시험 응시료 지원 사업」공고",1),
                selectBusanDongguSahaCase(group,"30868","「2025년 부산 동구 청년 자격증 시험 응시료 지원 사업」공고",1),
                selectBusanDongguSahaCase(group,"30095","「2024년 부산 동구 청년 자격증 시험 응시료 지원 사업」 대상자 모집 수정 공고",1));
        if("SAHA".equals(group)) return Stream.of(
                selectBusanDongguSahaCase(group,"43993","2026년 청년어촌정착지원사업 신청 공고",0),
                selectBusanDongguSahaCase(group,"46096","2026년도 여성어업인 특화건강검진 지원사업 신청안내 공고(2차)",1),
                selectBusanDongguSahaCase(group,"45847","2026년 다문화가족사업 민간단체 지방보조금 지원 계획 공고",2));
        if("BSBUKGU".equals(group)) return Stream.of(
                selectBusanNorthWestCase(group,"37638","「2026년 부산 북구 청년 자격시험 응시료 지원사업」공고",1),
                selectBusanNorthWestCase(group,"35578","2025년 부산 「북구 청년 자격시험 응시료 지원사업」공고",1),
                selectBusanNorthWestCase(group,"35116","2024년 부산 「북구 청년 자격시험 응시료 지원사업」 변경사항 공고",1));
        if("BSGANGSEO".equals(group)) return Stream.of(
                selectBusanNorthWestCase(group,"40497","2026년 2차 청년농업인 선발 및 영농정착 지원사업 시행 공고",2),
                selectBusanNorthWestCase(group,"39570","2026년 강서구 청년 자격시험 응시료 지원사업 대상자 모집 공고",1),
                selectBusanNorthWestCase(group,"39045","2026년 청년어촌정착지원사업 신청 공고",2));
        if("HAEUNDAE".equals(group)) return Stream.of(
                selectBusanEastCase(group,"53828","해운대구 청년 구직활동비 지원사업 참여자 모집 공고(3차)",1),
                selectBusanEastCase(group,"53342","해운대구 청년 구직활동비 지원사업 참여자 모집 공고",1),
                selectBusanEastCase(group,"53170","2026년『해운대 청년채움공간』가상오피스 지원사업 모집공고(3차)",1));
        if("GIJANG".equals(group)) return Stream.of(
                selectBusanEastCase(group,"51576","2026년 청년농업인영농정착지원사업 2차 선발 시행 공고",1),
                selectBusanEastCase(group,"50406","「2026년 기장군 청년 면접수당 지원사업」 대상자 모집 공고",4),
                selectBusanEastCase(group,"50405","「2026년 기장군 청년 자격시험 응시료 지원사업」 대상자 모집 공고",3));
        if("SUYEONG".equals(group)) return Stream.of(
                selectBusanStructuredCase(group,"40142","2026년 소상공인 영업용 전기차(전기이륜차포함) 구입비 지원 사업 변경 공고",1),
                selectBusanStructuredCase(group,"39601","2026년 소상공인 영업용 전기차(전기이륜차포함) 구입비 지원 사업 시행 공고",1),
                selectBusanStructuredCase(group,"38884","「2026년 수영구 청년 자격증 등 시험 응시료 지원 사업」지원대상자 접수 공고",2));
        if("SASANG".equals(group)) return Stream.of(
                selectBusanStructuredCase(group,"40870","2026년 동네 청년활동공간 활성화 지원사업(사상청년브릿지) 보조사업자 선정 공고",1),
                selectBusanStructuredCase(group,"40734","「동네 청년 활동공간 활성화 지원사업」 보조사업자 모집공고",2),
                selectBusanStructuredCase(group,"40426","「2026년 사상구 청년활동지원공모사업」참여단체 모집 공고",3));
        if("BUSANJIN".equals(group)) return Stream.of(
                selectBusanSupportCase(group,"51342","부산진구 청년 취업역량강화 자격시험 응시료 지원사업 공고",1),
                selectBusanSupportCase(group,"50698","2026년 부산진구 노인일자리 및 사회활동 지원사업 참여자 모집 공고",1),
                selectBusanSupportCase(group,"47592","2025년도 부산진구 노인일자리 및 사회활동 지원사업 참여자 모집 공고",1),
                selectBusanSupportCase(group,"47279","「부산진구 청년 취업역량강화 자격시험 응시료 지원사업」변경사항 공고",1),
                selectBusanSupportCase(group,"45554","부산진구 청년 취업역량강화 자격시험 응시료 지원사업 대상자 모집 공고",1));
        if("GEUMJEONG".equals(group)) return Stream.of(
                selectBusanSupportCase(group,"43289","2026 금정구 청년 전월세 중개수수료 지원사업 참여자 모집 공고",3));
        if("DONGNAE".equals(group)) return Stream.of(
                selectDongnaeCase("43807","2026년 동래구 청년 마이홈 부동산 중개수수료 지원사업"),
                selectDongnaeCase("43719","2026년 동래구 청년 성장+면접준비금 지원사업 신청 공고"),
                selectDongnaeCase("43686","『2026 동래구 청년 자격증 응시료 지원 사업』신청 공고"));
        if("HOENGSEONG".equals(group)) return Stream.of(
                selectStandardCollectionCase(group,"424679","2026년 신혼부부 주거자금 대출이자 지원 대상자 2차 모집 공고",1),
                selectStandardCollectionCase(group,"424078","2026년 횡성군 중소기업 특례보증 지원 공고",2),
                selectStandardCollectionCase(group,"424077","2026년 횡성군 중소기업육성자금 이차보전 지원 공고（3차）",2));
        if("YEONGWOL".equals(group)) return Stream.of(
                selectStandardCollectionCase(group,"157529","「2026 지역사랑 휴가지원 사업」 영월형 반값여행 추가 운영(5차) 공고",2),
                selectStandardCollectionCase(group,"157016","2026년 청년 창업육성 지원사업 하반기 공고문(제3차)(수정)",1),
                selectStandardCollectionCase(group,"156846","2026년 과수분야 지원사업 추가 공고",1),
                selectStandardCollectionCase(group,"150619","2026년 영월군 소상공인 경영환경개선 지원사업 공고",2),
                selectStandardCollectionCase(group,"147676","2025년 영월군 소상공인 시설개선 지원사업 추가 공고",2));
        if("GURO".equals(group)) return Stream.of(
                selectGuroCase("49626","2026년 구로구 중소기업・소상공인 1년 무이자 특별보증 융자지원 공고",1),
                selectGuroCase("39520","소상공인 버팀목 고용장려금 지원 시행계획 공고",5),
                selectGuroCase("35870","구로구 소기업 소상공인 방역물품지원 사업 변경 공고",1));
        if("HWACHEON".equals(group)) {
            String url="https://eminwon.ihc.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=32258&subCheck=N";
            return Stream.of(new ObservationCase("HWACHEON-32258","2026년 화천군 중소기업 및 소상공인 육성자금 융자추천 및 이차보전 지원계획 공고",
                    new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","adef30798671fe61ce7a68021b596a06e86d843e483807d7c2a16e6c269a8d40",url,"LGS-000130","SAFE_SAEOL_EMINWON_LEGACY"),
                    new HwacheonPostAttachmentDiscoveryProfile(),url,1,TitleLayout.HWACHEON_LABEL));
        }
        if("GANGBUK".equals(group)) {
            String url="https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId=179490";
            var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
            return Stream.of(new ObservationCase("GANGBUK-179490","2026년 청년 어학・자격시험 응시료 지원 사업 모집 공고",
                    new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000010","SPRING_BBS"),
                    new LegalBoardAttachmentProfileConfiguration().selectGangbukLegalProfileDetails(),url,4,TitleLayout.GANGBUK_SUBJECT));
        }
        if("JUNGGU_PDF".equals(group)) return selectCases("JUNGGU").filter(sample->"JUNGGU-33626".equals(sample.code()));
        if("JUNGGU".equals(group)) return Stream.of(
                selectJungguCase("34196","2026 다국어 QR메뉴판 지원사업 참여 사업체 모집",1,null),
                selectJungguCase("33626","「대구 중구 청년 부동산중개보수 및 이사비 지원사업」모집 공고",2,null),
                selectJungguCase("33315","2026년 음식점 위생등급제 컨설팅 지원 업소 모집 공고",1,TitleStageCode.COMBINATION_NOT_MATCHED));
        if("HAMAN".equals(group)) {
            String url="https://eminwon.haman.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=41306&subCheck=Y";
            var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
            return Stream.of(new ObservationCase("HAMAN-41306","2026년도 함안군 소상공인 육성자금(이자) 지원계획 공고",
                    new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000233","SAFE_SAEOL_EMINWON_CELL"),
                    new SaeolGetAttachmentProfileConfiguration().selectHamanProfileDetails(),url,1,TitleLayout.HAMAN_LABEL));
        }
        if("DALSEONG_HEADER".equals(group))return selectCases("DALSEONG").filter(sample->"DALSEONG-51022".equals(sample.code()));
        if("DALSEONG".equals(group)) return Stream.of(
                selectDalseongCase("51022","2026년 달성군 중소기업 경영안정자금(이차보전) 지원사업 공고",2),
                selectDalseongCase("52145","2026년 달성군 소상공인 카드수수료 지원사업 공고",1),
                selectDalseongCase("51075","「2026년 달성청년 자격증 응시료 지원사업」공고",1));
        if("NAMGU_STRUCTURE".equals(group)) return selectCases("NAMGU").filter(sample->"NAMGU-44381".equals(sample.code()));
        if("NAMGU".equals(group)) return Stream.of(
                selectNamguCase("44466","2026년 청년 사업자 임차료 지원사업 참여자 모집 공고"),
                selectNamguCase("44381","2026년 남구 청년 자격시험 응시료 지원사업 참가자 모집 공고"),
                selectNamguCase("42871","2025년 남구 청년 자기개발 도서구입비 지원사업 참여자 모집 변경공고"));
        if("OKCHEON".equals(group)) return Stream.of(
                selectOkcheonCase("193369","2026년 4차 옥천군 중소기업 환경개선 지원사업 모집 공고",null),
                selectOkcheonCase("193297","2026 충청북도 중소기업육성자금 융자(이차보전) 지원계획 변경(2차) 공고",null),
                selectOkcheonCase("193187","「2026년 일반음식점 주방환경 개선 지원 사업」(2차) 공고 게재",TitleStageCode.COMBINATION_NOT_MATCHED));
        if("BOEUN".equals(group)) return Stream.of(
                selectBoeunCase("221499","2026년 청년 월세 지원사업(취업자, 농업인 주거비 지원) 참여자 모집공고(3분기)"),
                selectBoeunCase("221497","2026년 청년 소상공인 점포 임차료 지원사업 참여자 모집 공고(3분기)"),
                selectBoeunCase("218812","2026년 소상공인 출산 지원사업 참여자 모집 공고"));
        if("CHUNGJU".equals(group)) return Stream.of(
                selectChungjuCase("72625","2026년 교통약자 차량용 보조기기 설치 추가지원 사업 공고(3차)"),
                selectChungjuCase("72039","2026년 충주시 중소기업육성기금 지원계획 변경 공고"),
                selectChungjuCase("70852","2026년 결혼·출산가정 대출이자 지원사업 공고"));
        if("TAEBAEK".equals(group)) return Stream.of(new ObservationCase(CASE,TITLE,SOURCE,PROFILE,
                "https://www.taebaek.go.kr/www/selectBbsNttList.do?bbsNo=25&key=352",2,TitleLayout.CLASSIC_LABEL));
        if("TAEBAEK_HWP".equals(group)) {
            String url="https://www.taebaek.go.kr/www/selectBbsNttView.do?key=352&bbsNo=25&nttNo=176153";
            var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
            return Stream.of(new ObservationCase("TAEBAEK-176153","2026년 태백시 소상공인 특례보증 및 이차보전 지원계획 공고",
                    new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000121","SPRING_BBS"),
                    PROFILE,"https://www.taebaek.go.kr/www/selectBbsNttList.do?bbsNo=25&key=352",1,TitleLayout.CLASSIC_LABEL));
        }
        if("JECHEON".equals(group)) return Stream.of(
                selectJecheonCase("403587","2026년 신백동 농지이용관리지원사업 농지전수조사 조사원 추가 모집 공고",1),
                selectJecheonCase("403530","2026년 제천시 청년 주택자금 대출이자 지원사업 신청자 모집 변경공고",1),
                selectJecheonCase("403490","- 2026년 제천 온(溫) 통합돌봄 특화사업 - 제천 온(溫) 방문운동 지원사업 제공기관 모집 재공고",2));
        if(!"YANGPYEONG".equals(group)) throw new IllegalArgumentException("UNKNOWN_OBSERVATION_GROUP");
        return Stream.of(
                selectYangpyeongCase("312241","9b6353577acabd579068a389244e774d40b5488eea86d1e15fb78e0eae9ccb9a",
                        "2026년 중장년 취업지원 프로그램 '산모·신생아 건강관리사' 교육생 모집공고",1),
                selectYangpyeongCase("311846","0fcd9b0bf381aebb5c18b80c5e5c1fd7355f9ce0d0ab130b64761de60cf56f96",
                        "2026년 중소기업 제품디자인개발 지원사업 참여기업 모집 공고",2),
                selectYangpyeongCase("311507","7d571058a83135abdb528156f40ef0e1c396dcd6f2a4731255ed960c5a7b65c5",
                        "『2026년 귀농인 정착지원 주택임대 사업』 대상자(빈집 소유자) 모집 3차 공고",2));
    }
    private static ObservationCase selectYeongdoCase(String id,String title,int count) {
        String url="https://www.yeongdo.go.kr/00000/00007/00013.web?amode=view&not_ancmt_mgt_no="+id+"&type=A";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("YEONGDO-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,
                "LGS-000031","SCMS_CARD_NOTICE"),new YeongdoAttachmentDiscoveryProfile(),"https://www.yeongdo.go.kr/00000/00007/00013.web",count,TitleLayout.YEONGDO_HEADING);
    }
    private static ObservationCase selectBusanJungguSeoguCase(String group,String id,String title,int count) {
        boolean junggu="BSJUNGGU".equals(group);
        if(!junggu&&!"BSSEOGU".equals(group))throw new IllegalArgumentException("UNKNOWN_BUSAN_JUNGGU_SEOGU_GROUP");
        String host=junggu?"eminwon.bsjunggu.go.kr":"eminwon.bsseogu.go.kr";
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();var c=new SaeolGetAttachmentProfileConfiguration();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,
                junggu?"LGS-000028":"LGS-000029","SAFE_SAEOL_EMINWON_COMPACT"),junggu?c.selectBusanJungguProfileDetails():c.selectBusanSeoguProfileDetails(),url,count,
                junggu?TitleLayout.GEUMJEONG_HEADER:TitleLayout.BUSAN_SEOGU_LABEL);
    }
    private static ObservationCase selectBusanDongguSahaCase(String group,String id,String title,int count) {
        boolean donggu="BSDONGGU".equals(group);
        if(!donggu&&!"SAHA".equals(group))throw new IllegalArgumentException("UNKNOWN_BUSAN_DONGGU_SAHA_GROUP");
        String host=donggu?"eminwon.bsdonggu.go.kr":"eminwon.saha.go.kr";
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        var config=new SaeolGetAttachmentProfileConfiguration();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,
                donggu?"LGS-000030":"LGS-000037","SAFE_SAEOL_EMINWON"),donggu?config.selectBusanDongguProfileDetails():config.selectSahaProfileDetails(),url,count,
                donggu?TitleLayout.HAMAN_LABEL:TitleLayout.SAHA_LABEL);
    }
    private static ObservationCase selectBusanNorthWestCase(String group,String id,String title,int count) {
        boolean bukgu="BSBUKGU".equals(group);
        if(!bukgu&&!"BSGANGSEO".equals(group))throw new IllegalArgumentException("UNKNOWN_BUSAN_NORTH_WEST_GROUP");
        String host=bukgu?"eminwon.bsbukgu.go.kr":"eminwon.bsgangseo.go.kr";
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,bukgu?"LGS-000035":"LGS-000039",bukgu?"SAFE_SAEOL_EMINWON":"SAFE_SAEOL_EMINWON_COMPACT"),
                bukgu?new BusanBukguPostAttachmentDiscoveryProfile():new BusanGangseoAttachmentDiscoveryProfile(),url,count,
                bukgu?TitleLayout.BUSAN_BUKGU_LABEL:TitleLayout.BUSAN_GANGSEO_HEADING);
    }
    private static ObservationCase selectBusanEastCase(String group,String id,String title,int count) {
        boolean haeundae="HAEUNDAE".equals(group);
        if(!haeundae&&!"GIJANG".equals(group))throw new IllegalArgumentException("UNKNOWN_BUSAN_EAST_GROUP");
        String host=haeundae?"eminwon.haeundae.go.kr":"eminwon.gijang.go.kr";
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        var config=new SaeolGetAttachmentProfileConfiguration();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,haeundae?"LGS-000036":"LGS-000043",haeundae?"SAFE_SAEOL_EMINWON_COMPACT":"SAFE_SAEOL_EMINWON"),
                haeundae?config.selectHaeundaeProfileDetails():config.selectGijangProfileDetails(),url,count,
                haeundae?TitleLayout.HAEUNDAE_HEADING:TitleLayout.GIJANG_HEADER);
    }
    private static ObservationCase selectBusanStructuredCase(String group,String id,String title,int count) {
        boolean suyeong="SUYEONG".equals(group);
        if(!suyeong&&!"SASANG".equals(group))throw new IllegalArgumentException("UNKNOWN_BUSAN_STRUCTURED_GROUP");
        String host=suyeong?"eminwon.suyeong.go.kr":"eminwon.sasang.go.kr";
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        var config=new SaeolGetAttachmentProfileConfiguration();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,suyeong?"LGS-000041":"LGS-000042",suyeong?"SAFE_SAEOL_EMINWON_COMPACT":"SAFE_SAEOL_EMINWON"),
                suyeong?config.selectSuyeongProfileDetails():config.selectSasangProfileDetails(),url,count,
                suyeong?TitleLayout.SUYEONG_HEADING:TitleLayout.SASANG_HEADER);
    }
    private static ObservationCase selectBusanSupportCase(String group,String id,String title,int count) {
        boolean busanjin="BUSANJIN".equals(group);
        if(!busanjin&&!"GEUMJEONG".equals(group))throw new IllegalArgumentException("UNKNOWN_BUSAN_SUPPORT_GROUP");
        String host=busanjin?"eminwon.busanjin.go.kr":"eminwon.geumjeong.go.kr";
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        var config=new SaeolGetAttachmentProfileConfiguration();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,busanjin?"LGS-000032":"LGS-000038","SAFE_SAEOL_EMINWON_COMPACT"),
                busanjin?config.selectBusanjinProfileDetails():config.selectGeumjeongProfileDetails(),url,count,
                busanjin?TitleLayout.BUSANJIN_LABEL:TitleLayout.GEUMJEONG_HEADER,
                busanjin&&Set.of("50698","47592").contains(id)?TitleStageCode.COMBINATION_NOT_MATCHED:null);
    }
    private static ObservationCase selectDongnaeCase(String id,String title) {
        String url="https://eminwon.dongnae.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("DONGNAE-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,"LGS-000033","SAFE_SAEOL_EMINWON"),
                new SaeolGetAttachmentProfileConfiguration().selectDongnaeProfileDetails(),url,1,TitleLayout.DONGNAE_LABEL);
    }
    private static ObservationCase selectJungguCase(String id,String title,int count,TitleStageCode expectedStop) {
        String url="https://eminwon.jung.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("JUNGGU-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000045","SAFE_SAEOL_EMINWON_LEGACY"),
                new SaeolGetAttachmentProfileConfiguration().selectDaeguJungguProfileDetails(),url,count,TitleLayout.JUNGGU_LABEL,expectedStop);
    }
    private static ObservationCase selectDalseongCase(String id,String title,int count) {
        String url="https://eminwon.dalseong.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("DALSEONG-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000052","SAFE_SAEOL_EMINWON"),
                new SaeolGetAttachmentProfileConfiguration().selectDaeguDalseongProfileDetails(),url,count,TitleLayout.DALSEONG_LABEL);
    }
    private static ObservationCase selectNamguCase(String id,String title) {
        String url="https://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("NAMGU-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000034","SAFE_SAEOL_EMINWON_LEGACY"),
                new SaeolGetAttachmentProfileConfiguration().selectBusanNamguProfileDetails(),url,1,TitleLayout.NAMGU_HEADER);
    }
    private static ObservationCase selectOkcheonCase(String id,String title,TitleStageCode expectedStop) {
        String url="https://www.oc.go.kr/www/selectBbsNttView.do?key=236&bbsNo=40&nttNo="+id;
        var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("OKCHEON-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000140","HEURISTIC_NOTICE"),
                new StandardBbsAttachmentProfileConfiguration().selectOkcheonProfileDetails(),
                "https://www.oc.go.kr/www/selectBbsNttList.do?bbsNo=40&key=236",1,TitleLayout.COMPACT_SUBJECT,expectedStop);
    }
    private static ObservationCase selectBoeunCase(String id,String title) {
        String url="https://www.boeun.go.kr/www/selectBbsNttView.do?key=194&bbsNo=66&nttNo="+id;
        var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("BOEUN-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000139","HEURISTIC_NOTICE"),
                new StandardBbsAttachmentProfileConfiguration().selectBoeunProfileDetails(),
                "https://www.boeun.go.kr/www/selectBbsNttList.do?bbsNo=66&key=194",1,TitleLayout.COMPACT_SUBJECT);
    }
    private static ObservationCase selectStandardCollectionCase(String group,String id,String title,int count) {
        boolean hoengseong="HOENGSEONG".equals(group);
        if(!hoengseong&&!"YEONGWOL".equals(group))throw new IllegalArgumentException("UNKNOWN_STANDARD_COLLECTION_GROUP");
        String host=hoengseong?"www.hsg.go.kr":"www.yw.go.kr";
        String query=hoengseong?"bbsNo=65&key=821":"bbsNo=17&key=273";
        String url="https://"+host+"/www/selectBbsNttView.do?"+query+"&nttNo="+id;
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        var configuration=new StandardBbsAttachmentProfileConfiguration();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,hoengseong?"LGS-000125":"LGS-000126","SPRING_BBS"),
                hoengseong?configuration.selectHoengseongProfileDetails():configuration.selectYeongwolProfileDetails(),
                "https://"+host+"/www/selectBbsNttList.do?"+query,count,
                hoengseong?TitleLayout.COMPACT_SUBJECT:TitleLayout.CLASSIC_LABEL,
                !hoengseong&&Set.of("157529","156846").contains(id)?TitleStageCode.COMBINATION_NOT_MATCHED:null);
    }
    private static ObservationCase selectGuroCase(String id,String title,int count) {
        String url="https://www.guro.go.kr/www/selectBbsNttGosiView.do?bbsNo=663&nttNo="+id+"&key=1791";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("GURO-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,"LGS-000018","SAEOL_GOSI"),new GuroGosiAttachmentDiscoveryProfile(),
                "https://www.guro.go.kr/www/selectBbsNttList.do?bbsNo=663&key=1791",count,TitleLayout.COMPACT_SUBJECT);
    }
    private static ObservationCase selectChungjuCase(String id,String title) {
        String url="https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no="+id;
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("CHUNGJU-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,"LGS-000137","SAEOL_GOSI"),new ChungjuEminwonAttachmentDiscoveryProfile(),
                "https://www.chungju.go.kr/www/selectEminwonList.do?key=510",1,TitleLayout.CLASSIC_LABEL,
                "70852".equals(id)?null:TitleStageCode.COMBINATION_NOT_MATCHED);
    }
    private static ObservationCase selectYangpyeongCase(String id,String identity,String title,int count) {
        return new ObservationCase("YANGPYEONG-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",identity,
                "https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo="+id,"LGS-000110","HEURISTIC_NOTICE"),
                new StandardBbsAttachmentProfileConfiguration().selectYangpyeongProfileDetails(),
                "https://www.yp21.go.kr/www/selectBbsNttList.do?bbsNo=5&key=1119",count,TitleLayout.COMPACT_SUBJECT);
    }
    private static ObservationCase selectJecheonCase(String id,String title,int count) {
        String url="https://www.jecheon.go.kr/www/selectBbsNttView.do?key=5233&bbsNo=18&nttNo="+id;
        var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("JECHEON-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000138","HEURISTIC_NOTICE"),
                new StandardBbsAttachmentProfileConfiguration().selectJecheonProfileDetails(),
                "https://www.jecheon.go.kr/www/selectBbsNttList.do?bbsNo=18&key=5233",count,TitleLayout.COMPACT_LABEL,
                "403587".equals(id)?TitleStageCode.COMBINATION_NOT_MATCHED:null);
    }

    @ParameterizedTest(name="{0}") @MethodSource("selectConfiguredCases") @Timeout(420)
    void observesTitleBodyAndWholeAttachmentSetWithoutPublication(ObservationCase sample) throws Exception {
        boolean collectionOnly=Boolean.getBoolean("saneb.attachment-observation.collection-only");
        var profile=sample.profile();var source=sample.source();
        var report=new LinkedHashMap<String,Object>();
        report.put("scope","OFFICIAL_THREE_STAGE_OBSERVATION_V1");report.put("caseCode",sample.code());report.put("observedAt",Instant.now().toString());
        report.put("titleInputSource","FIXED_OFFICIAL_SAMPLE");
        report.put("profileCode",profile.selectProfileCode());report.put("profileHash",profile.selectProfileHash());
        report.put("isPolicyQaPassed",false);report.put("isExpectationApproved",false);report.put("productionWriteCount",0);
        report.put("status","INCOMPLETE");report.put("expectedListedFileCount",sample.listedFileCount());
        report.put("collectionOnly",collectionOnly);report.put("isExtractionVerified",false);
        report.put("isWholeTextAnalysisComplete",false);
        var rows=new ArrayList<Map<String,Object>>();report.put("files",rows);
        var budget=new Budget(profile, Boolean.getBoolean("saneb.attachment-observation.diagnostic-budget"));Path temporary=null;String stage="RUNTIME";
        try(var client=new AttachmentPinnedDownloadClient()) {
            assertTrue(collectionOnly || System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")
                    &&Files.isExecutable(Path.of("/usr/bin/bwrap"))&&Files.isExecutable(Path.of("/usr/bin/prlimit")),"LINUX_ISOLATION_REQUIRED");
            var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
            report.put("rulesSource","EPHEMERAL_DB_DRAFT_SEED");report.put("rulesHash",AnnouncementAttachmentOfficialObservationTest.selectHash(rules));
            var engine=new AnnouncementSourceClassificationEngine();stage="TITLE_GATE";
            var title=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            report.put("titleStage",title.titleStageCode());
            report.put("titleReason",title.reasonCode());
            if(selectPlannedTitleStop(sample,title)) {
                // 사전 확인한 음성 표본도 보고서 분모에 남긴다. 현재 규칙의 판정이 바뀌면 실패한다.
                report.put("status","TITLE_NOT_ELIGIBLE_NOT_FETCHED");report.put("requiresFinalAdminVerification",false);return;
            }
            if(CASE.equals(sample.code())) assertTrue(selectTitleMayProceed(title),"TITLE_NOT_ELIGIBLE");
            if(title.semanticStatusCode()==SemanticStatusCode.EXCLUDED) {
                // 고정 표본이라도 현재 DRAFT 제목 규칙을 우회하여 본문/파일을 요청하지 않는다.
                report.put("status","TITLE_EXCLUDED_NOT_FETCHED");report.put("requiresFinalAdminVerification",false);return;
            }
            assertTrue(selectTitleMayProceed(title),"TITLE_NOT_ELIGIBLE");
            // 본문 클라이언트는 최대2시도, redirect0, 응답1MiB다. 그 상한을 전체 예산에서 먼저 확보한다.
            budget.reserveBody();stage="BODY_COLLECTION";
            var bodyClient=new LocalGovernmentNoticeProviderContentClient(true,3000,7000,(int)MIB,0,1,"saneB-notice-collector/1.0");
            var body=bodyClient.selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",UUID.fromString("77000000-0000-0000-0000-000000000001"),
                    sample.listUrl(),source.sourceUrl()));
            report.put("bodyStatus",body.statusCode());report.put("bodyFailureCode",body.failureCode());
            report.put("bodyAttempts",body.attemptCount());report.put("bodyRedirects",body.redirectCount());
            boolean bodyComplete=selectBodyComplete(body);report.put("bodyStageComplete",bodyComplete);
            stage="BODY_CLASSIFICATION";
            var base=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),bodyComplete?body.bodyText():null,null,List.of(),body.bodySourceCode(),body.bodyAvailabilityCode()),rules);
            report.put("bodyHash",bodyComplete?AnnouncementAttachmentOfficialObservationTest.selectHash(body.bodyText()):null);
            report.put("bodyCharacterCount",bodyComplete?body.bodyText().codePointCount(0,body.bodyText().length()):0);
            report.put("bodyDecision",base.semanticStatusCode());report.put("bodyReason",base.reasonCode());
            // 본문의 A/B/정보 부족은 여기서 첨부 요청을 끊는 조건이 아니다.
            assertTrue(selectTitleMayProceed(base),"TITLE_DECISION_CHANGED");
            temporary=Files.createTempDirectory("saneb-bbs-observation-");Path detail=temporary.resolve("detail.bin");
            stage="DETAIL_DISCOVERY";var uri=profile.selectDetailUri(source);
            var detailRequest=AttachmentPinnedDownloadClient.Request.selectGet(uri);
            var download=client.selectDownload(detailRequest,profile.selectApprovedHosts(),
                    r->budget.selectRequestAllowed(detailRequest,r),detail,MIB,budget::saveBytes);
            assertTrue(Set.of("text/html","application/xhtml+xml").contains(download.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT)),"DETAIL_CONTENT_TYPE_CHANGED");
            AttachmentDiscoveryProfile.Result discovered;
            try(var input=Files.newInputStream(detail)) {
                var page=Jsoup.parse(input,null,uri.toASCIIString());stage="TITLE_CONFIRMATION";validateTitle(page,sample.title(),sample.titleLayout());
                stage="DETAIL_DISCOVERY";discovered=profile.selectDescriptors(source,page.outerHtml());
            } finally {Files.deleteIfExists(detail);}
            report.put("discoveryStatus",discovered.status());report.put("discoveryComplete",discovered.complete());report.put("discoveredFileCount",discovered.descriptors().size());
            for(var d:discovered.descriptors()) {var row=new LinkedHashMap<String,Object>();rows.add(row);row.put("locatorHash",AnnouncementAttachmentOfficialObservationTest.selectHash(d.locator()));
                row.put("formatHint",d.expectedFormat());row.put("downloadAllowed",d.downloadAllowed());row.put("status","NOT_RUN");}
            assertTrue(discovered.complete()&&Set.of("FOUND","NO_FILES").contains(discovered.status()),"DISCOVERY_INCOMPLETE");
            if("GANGBUK-179490".equals(sample.code())) {
                assertEquals(GANGBUK_LOCATORS,rows.stream().map(row->row.get("locatorHash")).toList(),"OFFICIAL_FILE_LIST_CHANGED");
                assertEquals(List.of("HWPX","HWP","HWPX","HWPX"),rows.stream().map(row->row.get("formatHint")).toList(),"OFFICIAL_FILE_FORMAT_CHANGED");
            }
            var extractor=collectionOnly?null:new IsolatedAttachmentExtractor(JSON,System.getProperty("saneb.attachment-observation.extractor"));var files=new ArrayList<FileInput>();
            for(int i=0;i<discovered.descriptors().size();i++) {
                var descriptor=discovered.descriptors().get(i);var row=rows.get(i);Path binary=temporary.resolve(UUID.randomUUID()+".bin");
                if(!descriptor.downloadAllowed()) {row.put("status","UNSUPPORTED_NOT_DOWNLOADED");files.add(incompleteFile("UNSUPPORTED"));continue;}
                var transfer=new ObservationDownloadTrace();
                try {
                    stage="FILE_DOWNLOAD";var fileRequest=descriptor.selectRequest();
                    var bytes=selectFileDownload(profile,fileRequest,binary,budget,client,transfer);
                    row.put("bytes",bytes.bytes());row.put("binaryHash",bytes.sha256());stage="FILE_SIGNATURE";
                    String format=new AttachmentFileTypeValidator().selectFormat(binary,bytes,descriptor.expectedFormat(),profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes());
                    row.put("format",format);
                    if(collectionOnly) {row.put("status","DOWNLOADED");continue;}
                    stage="ISOLATED_EXTRACTION";var actual=extractor.selectExtraction(binary);
                    assertEquals(format,actual.path("format").asText(),"EXTRACTED_FORMAT_CHANGED");
                    row.put("quality",actual.path("qualityCode").asText());
                    row.put("extractorVersion",actual.path("extractorVersion").asText());
                    assertTrue(Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED").contains(actual.path("qualityCode").asText()),"ISOLATED_EXTRACTION_FAILED");
                    stage="TEXT_ROLE";var observation=AnnouncementAttachmentOfficialObservationTest.selectTextObservation(actual);row.putAll(observation);
                    files.add(selectFileInput(actual,JSON.valueToTree(observation)));row.put("status","OBSERVED");
                } catch(Exception|AssertionError failure) {row.put("status","FAILED");row.put("failedStage",stage);row.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));files.add(incompleteFile("EXTRACTION_FAILED"));}
                finally {row.put("downloadTrace",transfer.selectSnapshot());Files.deleteIfExists(binary);}
            }
            if(collectionOnly) {
                stage="COLLECTION_COMPLETENESS";
                saveCollectionOnlySummary(report,rows,sample.listedFileCount());
                return;
            }
            stage="COMBINED_CLASSIFICATION";
            var decision=new AnnouncementAttachmentClassificationEngine().selectDecision(new Input(base,rules,true,discovered.status(),discovered.complete(),files,null,List.of()));
            report.put("decisionStatus",decision.status());report.put("decisionReason",decision.reason());report.put("warningCodes",decision.warnings());
            report.put("targetCodes",decision.targetCodes());report.put("supportCodes",decision.supportCodes());report.put("matchCount",decision.matches().size());
            report.put("requiresFinalAdminVerification",true);assertNotEquals("EXCLUDED",decision.status(),"ATTACHMENT_MUST_NOT_DELETE_TITLE");
            assertEquals(sample.listedFileCount(),rows.size(),"OFFICIAL_FILE_LIST_CHANGED");
            assertTrue(rows.stream().noneMatch(r->Set.of("NOT_RUN","FAILED").contains(r.get("status"))),"WHOLE_SET_OBSERVATION_INCOMPLETE");
            // 본문 실패가 첨부 진단 결과를 숨기지 않게 하되 전체 관측 성공으로 승격하지 않는다.
            stage="BODY_COMPLETENESS";validateBodyComplete(body);
            report.put("isWholeTextAnalysisComplete",selectWholeTextAnalysisComplete(bodyComplete,rows));
            report.put("status","OBSERVED_NOT_VALIDATED");
        } catch(Exception|AssertionError failure) {report.put("failedStage",stage);report.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));throw new AssertionError(sample.code()+": "+stage+" / OBSERVATION_INCOMPLETE");}
        finally {
            if(temporary!=null)try(var paths=Files.walk(temporary)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
            report.put("originalFilesRemoved",temporary==null||!Files.exists(temporary));report.put("maximumRequestReservations",budget.maximumRequests);report.put("maximumReservedBytes",budget.maximumBytes);
            report.put("requestReservationsIncludingBodyUpperBound",budget.requests);report.put("reservedBytesIncludingBodyUpperBound",budget.bytes);
            Path output=Path.of(System.getProperty("saneb.attachment-observation.report")).toAbsolutePath().normalize();Files.createDirectories(output);
            JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve(sample.code()+".json").toFile(),report);
        }
    }
    static void saveCollectionOnlySummary(Map<String,Object> report,List<Map<String,Object>> rows,int expectedCount) {
        assertEquals(expectedCount,rows.size(),"OFFICIAL_FILE_LIST_CHANGED");
        assertTrue(rows.stream().allMatch(row->"DOWNLOADED".equals(row.get("status"))),"WHOLE_SET_DOWNLOAD_INCOMPLETE");
        report.put("status","COLLECTION_ONLY_OBSERVED_NOT_APPROVED");
        report.put("collectionStageComplete",true);
        report.put("isWholeTextAnalysisComplete",false);
        report.put("isExtractionVerified",false);
        report.put("isPolicyQaPassed",false);
        report.put("isExpectationApproved",false);
    }
    public static boolean selectTitleMayProceed(AnnouncementSourceClassificationResult result) {return result.semanticStatusCode()!=SemanticStatusCode.EXCLUDED
            &&Set.of(TitleStageCode.GROUP_A_MATCHED,TitleStageCode.COMBINATION_MATCHED).contains(result.titleStageCode());}
    static boolean selectPlannedTitleStop(ObservationCase sample,AnnouncementSourceClassificationResult result) {
        if(sample.expectedTitleStopStage()==null)return false;
        assertEquals(sample.expectedTitleStopStage(),result.titleStageCode(),"FIXED_TITLE_STOP_CHANGED");
        assertFalse(selectTitleMayProceed(result),"FIXED_TITLE_STOP_BECAME_ELIGIBLE");return true;
    }
    public static boolean selectBodyComplete(ProviderContentResult body) {return body!=null&&body.statusCode()==ProviderContentCodes.StatusCode.AVAILABLE
            &&body.bodyAvailabilityCode()==BodyAvailabilityCode.AVAILABLE&&body.bodySourceCode()==BodySourceCode.DETAIL_PAGE_TEXT
            &&body.bodyText()!=null&&!body.bodyText().isBlank();}
    static void validateBodyComplete(ProviderContentResult body) {assertTrue(selectBodyComplete(body),"BODY_OBSERVATION_INCOMPLETE");}
    static void validateTitle(org.jsoup.nodes.Document page,String expected) {
        validateTitle(page,expected,false);
    }
    public static void validateTitle(org.jsoup.nodes.Document page,String expected,boolean compact) {
        validateTitle(page,expected,compact?TitleLayout.COMPACT_SUBJECT:TitleLayout.CLASSIC_LABEL);
    }
    public static void validateTitle(org.jsoup.nodes.Document page,String expected,TitleLayout layout) {
        if(layout==TitleLayout.YEONGDO_HEADING){
            var titles=page.select("form#saeolGosiVO[name=saeolGosiVO][method=get] > div.bbs1view1 > h1.h1");
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertTrue(titles.getFirst().children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.BUSAN_SEOGU_LABEL){
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table[width=100%][border=0][cellspacing=1][cellpadding=0]");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("th.w_90").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(cell.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.SAHA_LABEL){
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table.board_read");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("thead th[scope=col]").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&"3".equals(cell.attr("colspan"))&&cell.childrenSize()==1&&cell.ownText().isBlank(),"TITLE_STRUCTURE_CHANGED");
            var title=cell.child(0);assertTrue("b".equals(title.tagName())&&title.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(title.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.BUSAN_BUKGU_LABEL||layout==TitleLayout.BUSAN_GANGSEO_HEADING) {
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");var form=forms.getFirst();
            if(layout==TitleLayout.BUSAN_BUKGU_LABEL){
                var tables=form.select("table.tbl.taC");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");
                var labels=tables.getFirst().select("th[scope=row]").stream().filter(e->e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
                assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
                assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.hasClass("taL")&&"3".equals(cell.attr("colspan"))&&cell.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
                assertEquals(normalized(expected),normalized(cell.text()),"TITLE_CHANGED");
            }else{
                var views=form.select("div.board > div.b_view");assertEquals(1,views.size(),"TITLE_STRUCTURE_CHANGED");
                var heads=views.getFirst().select(":root > div.view_head > h3");assertEquals(1,heads.size(),"TITLE_STRUCTURE_CHANGED");
                assertTrue(heads.getFirst().children().isEmpty(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(heads.getFirst().text()),"TITLE_CHANGED");
            }return;
        }
        if(layout==TitleLayout.HAEUNDAE_HEADING||layout==TitleLayout.GIJANG_HEADER) {
            boolean haeundae=layout==TitleLayout.HAEUNDAE_HEADING;
            var forms=page.select("form");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");var form=forms.getFirst();
            assertEquals(haeundae?"form1":"form",form.attr("name"),"TITLE_STRUCTURE_CHANGED");assertEquals("post",form.attr("method").toLowerCase(Locale.ROOT),"TITLE_STRUCTURE_CHANGED");
            var containers=form.select(haeundae?"article.news_view":"table.tb_board_read");assertEquals(1,containers.size(),"TITLE_STRUCTURE_CHANGED");
            var titles=containers.getFirst().select(haeundae?":root > h2.newsTitle":"thead > tr > th[scope=col]");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");
            var title=titles.getFirst();
            if(haeundae){assertEquals(1,title.childrenSize(),"TITLE_STRUCTURE_CHANGED");var date=title.child(0);
                assertTrue("p".equals(date.tagName())&&date.hasClass("small")&&date.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
                assertTrue(normalized(date.text()).matches("등록일\\s*"+normalized("ㅣ")+"\\s*[0-9]{4}-[0-9]{2}-[0-9]{2}"),"TITLE_STRUCTURE_CHANGED");
            }else assertTrue(title.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(title.ownText()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.SUYEONG_HEADING||layout==TitleLayout.SASANG_HEADER) {
            var forms=page.select("form");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");var form=forms.getFirst();
            assertEquals("form1",form.attr("name"),"TITLE_STRUCTURE_CHANGED");assertEquals("post",form.attr("method").toLowerCase(Locale.ROOT),"TITLE_STRUCTURE_CHANGED");
            var containers=form.select(layout==TitleLayout.SUYEONG_HEADING?"div.view01":"table.basic");assertEquals(1,containers.size(),"TITLE_STRUCTURE_CHANGED");
            var titles=containers.getFirst().select(layout==TitleLayout.SUYEONG_HEADING?":root > h3":"thead.tb > tr > th.fi_la[colspan=2]");
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertTrue(titles.getFirst().children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.BUSANJIN_LABEL) {
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table.jin_gosi_table");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");
            var labels=tables.getFirst().select("th.w_90").stream().filter(e->e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.hasClass("w_330")&&cell.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(cell.text())),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.DONGNAE_LABEL) {
            var forms=page.select("form[name=form2][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table.tb_t1");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");
            var table=tables.getFirst();
            var labels=table.select("th").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&"3".equals(cell.attr("colspan"))&&cell.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(cell.text())),"TITLE_CHANGED");return;
        }
        Objects.requireNonNull(layout);
        if(layout==TitleLayout.HWACHEON_LABEL) {
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table[width=100%][border=0][cellspacing=1][cellpadding=0]");
            assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("th").stream().filter(e->e.closest("table")==table&&e.select("table").isEmpty()&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(cell.text())),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.GANGBUK_SUBJECT) {
            var forms=page.select("form#board");assertEquals(1,forms.size(),"DETAIL_IDENTITY_CHANGED");var form=forms.getFirst();
            var ids=form.children().stream().filter(e->"input".equals(e.tagName())&&"nttId".equals(e.attr("name"))).toList();
            assertEquals(1,ids.size(),"DETAIL_IDENTITY_CHANGED");
            assertTrue("hidden".equalsIgnoreCase(ids.getFirst().attr("type"))&&"179490".equals(ids.getFirst().val()),"DETAIL_IDENTITY_CHANGED");
            var views=form.children().stream().filter(e->"div".equals(e.tagName())&&e.hasClass("bd-view")).toList();
            assertEquals(1,views.size(),"DETAIL_IDENTITY_CHANGED");
            var titles=views.getFirst().children().stream().filter(e->"h3".equals(e.tagName())&&e.hasClass("bd-view__subject")).toList();
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(titles.getFirst().select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(titles.getFirst().text())),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.JUNGGU_LABEL) {
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table.boardView");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");
            var table=tables.getFirst();
            var labels=table.select("th").stream().filter(e->e.closest("table")==table&&e.select("table").isEmpty()&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(expected.replaceAll("\\s+"," ").strip(),cell.text().replaceAll("\\s+"," ").strip(),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.HAMAN_LABEL) {
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table[width=100%][border=0][cellspacing=1][cellpadding=0]");
            assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("td").stream().filter(e->e.closest("table")==table&&e.select("table").isEmpty()&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(cell.text())),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.DALSEONG_LABEL) {
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select("table.bbsView");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");
            var table=tables.getFirst();
            var labels=table.select("th[scope=row]").stream().filter(e->e.closest("table")==table&&"제목".equals(e.text().strip())).toList();
            assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&"3".equals(cell.attr("colspan"))&&cell.select("table").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(cell.text())),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.NAMGU_HEADER||layout==TitleLayout.GEUMJEONG_HEADER) {
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var headers=forms.getFirst().select(layout==TitleLayout.NAMGU_HEADER?"table.table_03":"table.bbs_vtype");assertEquals(1,headers.size(),"TITLE_STRUCTURE_CHANGED");
            var table=headers.getFirst();
            var titles=table.select("th[colspan=4]").stream().filter(e->e.closest("table")==table).toList();
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertTrue(titles.getFirst().select("table").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(titles.getFirst().text())),"TITLE_CHANGED");return;
        }
        var tables=page.select(layout==TitleLayout.CLASSIC_LABEL?"table.bbs_default.view":"div.p-wrap.bbs.bbs__view > table.p-table.block");
        assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
        if(layout==TitleLayout.COMPACT_SUBJECT) {
            var subjects=table.select("span.p-table__subject_text").stream().filter(e->e.closest("table")==table).toList();
            assertEquals(1,subjects.size(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(normalized(expected).equals(normalized(subjects.getFirst().text())),"TITLE_CHANGED");return;
        }
        var labels=table.select("th").stream().filter(e->e.closest("table")==table&&"제목".equals(e.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");
        var cell=labels.getFirst().nextElementSibling();assertTrue(cell!=null&&"td".equals(cell.tagName()),"TITLE_STRUCTURE_CHANGED");
        assertTrue(normalized(expected).equals(normalized(cell.text())),"TITLE_CHANGED");
    }
    private static String normalized(String text){return Normalizer.normalize(text,Normalizer.Form.NFKC).replaceAll("\\s+"," ").strip();}
    static boolean selectWholeTextAnalysisComplete(boolean bodyComplete,List<? extends Map<String,?>> files) {
        return bodyComplete&&files.stream().allMatch(file->"OBSERVED".equals(file.get("status"))&&"COMPLETE_TEXT".equals(file.get("quality")));
    }
    static FileInput selectFileInput(JsonNode actual,JsonNode observation) {
        var blocks=new ArrayList<Block>();for(var b:actual.path("blocks"))blocks.add(new Block(b.path("index").asInt(),b.path("startOffset").asInt(),b.path("endOffset").asInt(),b.path("evidenceScopeId").asText(),b.path("scopeReliable").asBoolean()));
        return new FileInput(UUID.randomUUID(),UUID.randomUUID(),observation.path("roleAssessment").path("roleCode").asText("UNKNOWN"),actual.path("qualityCode").asText(),actual.path("text").asText(""),blocks,null);
    }
    private static FileInput incompleteFile(String error){return new FileInput(UUID.randomUUID(),UUID.randomUUID(),"UNKNOWN","FAILED",null,List.of(),error);}
    /** 관측도 worker와 같은 다단계 전달을 사용한다. bridge/기간 조회/redirect까지 같은 예산에 합산한다. */
    static AttachmentPinnedDownloadClient.Download selectFileDownload(AttachmentDiscoveryProfile profile,
            AttachmentPinnedDownloadClient.Request initial,Path binary,Budget budget,AttachmentPinnedDownloadClient client)
            throws java.io.IOException {
        return selectFileDownload(profile,initial,binary,budget,client,new ObservationDownloadTrace());
    }
    static AttachmentPinnedDownloadClient.Download selectFileDownload(AttachmentDiscoveryProfile profile,
            AttachmentPinnedDownloadClient.Request initial,Path binary,Budget budget,AttachmentPinnedDownloadClient client,
            ObservationDownloadTrace trace) throws java.io.IOException {
        var result=AttachmentProfileDownloadFlow.selectDownload(profile,initial,binary,20*MIB,
                (request,limit,approved)->{
                    trace.saveTransportStarted(profile,request);
                    var downloaded=client.selectDownload(request,profile.selectApprovedHosts(),
                        candidate->approved.test(candidate)&&budget.selectRequestAllowed(initial,candidate),
                        binary,limit,budget::saveBytes);
                    trace.saveTransportCompleted();
                    return downloaded;
                });
        trace.saveComplete();return result;
    }

    static final class Budget {
        private final AttachmentDiscoveryProfile profile;
        final int maximumRequests;
        final long maximumBytes;
        Budget(){this(PROFILE);}
        Budget(AttachmentDiscoveryProfile profile,int requests,long bytes){
            this.profile=Objects.requireNonNull(profile);
            if(requests<1||requests>44||bytes<1||bytes>80*MIB)throw new IllegalArgumentException("OBSERVATION_BUDGET_INVALID");
            maximumRequests=requests;maximumBytes=bytes;
        }
        Budget(AttachmentDiscoveryProfile profile){this(profile,false);}
        Budget(AttachmentDiscoveryProfile profile,boolean diagnostic){this.profile=Objects.requireNonNull(profile);
            boolean namgu=diagnostic && "LOCAL_BUSAN_NAMGU_GET_V1".equals(profile.selectProfileCode());
            boolean boundedSaeol=Set.of("LOCAL_BUSAN_JUNGGU_GET_V1","LOCAL_BUSAN_SEOGU_GET_V1","LOCAL_BUSAN_DONGGU_GET_V1","LOCAL_SAHA_GET_V1","LOCAL_DAEGU_DALSEONG_GET_V1","LOCAL_HAMAN_GET_V1","LOCAL_DAEGU_JUNGGU_GET_V1","LOCAL_DONGNAE_GET_V1","LOCAL_BUSANJIN_GET_V1","LOCAL_GEUMJEONG_GET_V1","LOCAL_SUYEONG_GET_V1","LOCAL_SASANG_GET_V1","LOCAL_HAEUNDAE_GET_V1","LOCAL_BUSAN_BUKGU_POST_V1","LOCAL_BUSAN_GANGSEO_GET_V1").contains(profile.selectProfileCode());
            boolean gijang=Set.of("LOCAL_GIJANG_GET_V1",YeongdoAttachmentDiscoveryProfile.CODE).contains(profile.selectProfileCode());
            boolean gangbuk="LOCAL_GANGBUK_LEGAL_GET_V1".equals(profile.selectProfileCode());
            boolean guro=GuroGosiAttachmentDiscoveryProfile.CODE.equals(profile.selectProfileCode());
            boolean standardCollection=Set.of("LOCAL_HOENGSEONG_BBS_V1","LOCAL_YEONGWOL_BBS_V1").contains(profile.selectProfileCode());
            maximumRequests=standardCollection?5:guro?8:gangbuk?20:gijang?7:boundedSaeol?6:namgu?5:diagnostic?20:44;maximumBytes=(standardCollection?43:guro?23:gangbuk?32:gijang?23:boundedSaeol?23:namgu?24:diagnostic?32:80)*MIB;}
        long requests,bytes;
        void reserveBody(){if(requests!=0||bytes!=0)throw new IllegalStateException("BODY_BUDGET_ALREADY_RESERVED");requests=2;bytes=2*MIB;}
        boolean selectRequestAllowed(AttachmentPinnedDownloadClient.Request r){if(!profile.selectApprovedRequest(r)||requests>=maximumRequests||Thread.currentThread().isInterrupted())return false;requests++;return true;}
        boolean selectRequestAllowed(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request r){
            return profile.selectApprovedRequest(initial,r)&&selectRequestAllowed(r);
        }
        boolean saveBytes(long count){if(count<0||bytes>maximumBytes-count||Thread.currentThread().isInterrupted())return false;bytes+=count;return true;}
    }
}
