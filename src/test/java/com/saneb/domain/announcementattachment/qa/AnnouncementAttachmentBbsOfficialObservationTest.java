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

    public enum TitleLayout { CLASSIC_LABEL, COMPACT_SUBJECT, COMPACT_LABEL, NAMGU_HEADER, DALSEONG_LABEL, HAMAN_LABEL, JUNGGU_LABEL, GANGBUK_SUBJECT, HWACHEON_LABEL, DONGNAE_LABEL, BUSANJIN_LABEL, GEUMJEONG_HEADER, SUYEONG_HEADING, SASANG_HEADER, HAEUNDAE_HEADING, GIJANG_HEADER, BUSAN_BUKGU_LABEL, BUSAN_GANGSEO_HEADING, SAHA_LABEL, BUSAN_SEOGU_LABEL, YEONGDO_HEADING, SUSEONG_LABEL, DALSEO_LABEL, DANYANG_LABEL, EUMSEONG_LABEL, NONSAN_LABEL, FILEBOX_LABEL, SUNCHANG_LABEL, NAMWON_BOARD, JEONBUK_BOARD, JEONNAM_VIEW_TITLE, JEONNAM_NAJU_TITLE, JEONNAM_MUAN_TITLE, GOKSEONG_BOARD, JINDO_BOARD, JANGSEONG_BOARD, GYEONGBUK_HEADING, GYEONGBUK_SUBJECT, GYEONGBUK_BOARD, UISEONG_BOARD, YEONGJU_BOARD, SEONGJU_BOARD, YECHEON_BOARD, YEONGDEOK_BOARD, ULJIN_BOARD, CHEONGSONG_BOARD, GORYEONG_BOARD, JINJU_BOARD, GOSEONG_BOARD, CHANGWON_BOARD, JEJUSI_BOARD, EUNPYEONG_BOARD, SEOCHO_BOARD, DAEGU_NAMGU_BOARD, GWANGJU_DONGGU_BOARD, GWANGJU_BUKGU_BOARD, ULSAN_JUNGGU_BOARD, DONGHAE_BOARD, GW_GOSEONG_BOARD, GANGWON_SKIN_BOARD, JINCHEON_BOARD, YONGIN_BOARD, YANGJU_PORTAL, GUNPO_PORTAL, YEOJU_PORTAL, NAMYANGJU_PORTAL, HANAM_PORTAL, GURI_PORTAL, GIMPO_PORTAL, DONGDUCHEON_PORTAL, PYEONGTAEK_PORTAL, ANSEONG_PORTAL, UIJEONGBU_PORTAL, GG_GWANGJU_PORTAL, SIHEUNG_PORTAL, ANSAN_PORTAL, POCHEON_PORTAL, GANGNEUNG_PORTAL, YANGGU_BOARD, INJE_BOARD, CHUNGBUK_BOARD, GONGJU_BOARD, HONGSEONG_BOARD, YESAN_BOARD, GEUMSAN_BOARD, BUYEO_BOARD, ASAN_BOARD, SEOSAN_BOARD, JEONJU_THIRD_BOARD, JEONBUK_THIRD_BOARD, BOSEONG_BOARD, HAMPYEONG_BOARD, SOKCHO_BOARD, MAPO_BOARD, SEODAEMUN_BOARD, SEONGDONG_BOARD, SONGPA_BOARD, GWANGJIN_BOARD, DONGDAEMUN_BOARD, SEONGBUK_BOARD, YEONGDEUNGPO_BOARD, YANGCHEON_BOARD, GWANAK_BOARD, SEOUL_BOARD, SEOUL_JUNGGU_BOARD, YONGSAN_BOARD, GYEYANG_PORTAL, GANGHWA_PORTAL, JEMULPO_BOARD, MICHUHOL_BOARD, GEOMDAN_BOARD, YEONGJONG_BOARD, GWANGJU_NAMGU_BOARD, DAEJEON_JUNGGU_BOARD, YUSEONG_BOARD, DAEDEOK_BOARD, SEJONG_BOARD, PAJU_BOARD, GWANGMYEONG_BOARD, GANGWON_PROVINCE_BOARD, PYEONGCHANG_BOARD, HONGCHEON_BOARD, GUNWI_BOARD, SANGJU_BOARD, ANDONG_BOARD, GEUMCHEON_BOARD, DAEGU_CITY_BOARD, INCHEON_CITY_BOARD, DAEJEON_AGGREGATOR_BOARD, ULSAN_CITY_BOARD, SEOHAE_BOARD, NOWON_BOARD, GANGNAM_BOARD, DOBONG_BOARD, YEONGAM_BOARD, WONJU_BOARD, DAEJEON_SEOGU_BOARD, GWANGJU_SEOGU_BOARD, YEONJE_BOARD, GURYE_BOARD, SEONGNAM_BOARD, YEONGDONG_BOARD, GIMJE_BOARD, WANDO_BOARD, SHINAN_BOARD, JANGHEUNG_BOARD, GIMHAE_BOARD, CHANGNYEONG_BOARD, SACHEON_BOARD, HADONG_BOARD, GEOCHANG_BOARD, NAMHAE_BOARD, SANCHEONG_BOARD, UIRYEONG_BOARD, GEOJE_BOARD, GYEONGNAM_PROVINCE_BOARD, CHUNGNAM_PROVINCE_BOARD, ONGJIN_PORTAL, BUPYEONG_PORTAL, DONGJAK_POST_BOARD, ULSAN_DONGGU_BOARD, HWASEONG_BOARD, ULJU_BOARD, GWANGYANG_BOARD, CHEONGJU_BOARD, ICHEON_BOARD, YEONGGWANG_BOARD, GYEONGBUK_PROVINCE_BOARD, NAMDONG_PORTAL, SEOUL_GANGSEO_BOARD, GANGDONG_PORTAL, GOESAN_BOARD, SEOCHEON_BOARD, CHEONAN_BOARD, BUSAN_CITY_BOARD }
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
        return selectBatchCases(System.getProperty("saneb.attachment-observation.group","TAEBAEK"));
    }
    public static Stream<ObservationCase> selectBatchCases(String groups) {
        // 명시한 지역만 한 JVM에서 순차 실행한다. 각 표본의 기존 요청/용량 상한은 그대로다.
        String[] names=groups.split(",",-1);
        if(names.length>8)throw new IllegalArgumentException("COLLECTION_BATCH_GROUP_LIMIT");
        var selected=new LinkedHashMap<String,ObservationCase>();
        for(String name:names) {
            if(!name.strip().matches("[A-Z0-9_]+"))throw new IllegalArgumentException("COLLECTION_BATCH_GROUP_INVALID");
            selectCases(name.strip()).forEach(sample->selected.putIfAbsent(sample.code(),sample));
        }
        return selected.values().stream();
    }
    public static Stream<ObservationCase> selectCases(String group) {
        if(RecoveredSupportDownloadCases.GROUPS.contains(group))return Stream.of(RecoveredSupportDownloadCases.selectCase(group));
        if("BUSAN_CITY_SUPPORT".equals(group))return Stream.of(BusanCitySupportDownloadCases.selectCase());
        if("EUNPYEONG_SUPPORT".equals(group))return Stream.of(SeoulFirstDownloadCases.selectEunpyeongSupportCase());
        if("NOWON_SUPPORT".equals(group))return Stream.of(NowonDownloadCases.selectSupportCase());
        if("DONGDUCHEON_YOUTH".equals(group))return Stream.of(CapitalFourthDownloadCases.selectDongducheonYouthCase());
        if("CHEONAN".equals(group))return Stream.of(CheonanDownloadCases.selectCase());if("SEOCHEON".equals(group))return Stream.of(SeocheonDownloadCases.selectCase());if("GOESAN".equals(group))return Stream.of(GoesanDownloadCases.selectCase());if("GANGDONG".equals(group))return Stream.of(GangdongDownloadCases.selectCase());if("SEOUL_GANGSEO".equals(group))return Stream.of(SeoulGangseoDownloadCases.selectCase());
        if("NAMDONG".equals(group))return Stream.of(NamdongDownloadCases.selectCase());
        if("GYEONGBUK_PROVINCE".equals(group))return Stream.of(GyeongbukProvinceDownloadCases.selectCase());
        if("YEONGGWANG".equals(group))return Stream.of(YeonggwangDownloadCases.selectCase(true),YeonggwangDownloadCases.selectCase());
        if("ICHEON".equals(group))return Stream.of(IcheonDownloadCases.selectCase());
        if("CHEONGJU".equals(group))return Stream.of(CheongjuDownloadCases.selectCase());
        if("GWANGYANG".equals(group))return Stream.of(GwangyangDownloadCases.selectCase());
        if("ULJU".equals(group))return Stream.of(UljuDownloadCases.selectCase());
        if("HWASEONG".equals(group))return Stream.of(HwaseongDownloadCases.selectCase());
        if("ULSAN_DONGGU".equals(group))return Stream.of(UlsanDongguDownloadCases.selectCase());
        if("ULSAN_DONGGU_CARD".equals(group))return Stream.of(UlsanDongguDownloadCases.selectCase(true));
        if("METRO_REMAINDER".equals(group))return MetroRemainderDownloadCases.selectCases().stream();
        if("METRO_REMAINDER_DONGJAK".equals(group))return Stream.of(MetroRemainderDownloadCases.selectCase(false));
        if("ONGJIN".equals(group))return Stream.of(OngjinDownloadCases.selectCase());
        if("PROVINCE_NEXT".equals(group))return ProvinceNextDownloadCases.selectCases().stream();
        if("GEOJE".equals(group))return Stream.of(GeojeDownloadCases.selectCase());
        if("UIRYEONG".equals(group))return Stream.of(UiryeongDownloadCases.selectCase());
        if("SANCHEONG".equals(group))return Stream.of(SancheongDownloadCases.selectCase());
        if("NAMHAE".equals(group))return Stream.of(NamhaeDownloadCases.selectCase());
        if("GEOCHANG".equals(group))return Stream.of(GeochangDownloadCases.selectCase());
        if("HADONG".equals(group))return Stream.of(HadongDownloadCases.selectCase());
        if("SACHEON".equals(group))return Stream.of(SacheonDownloadCases.selectCase());
        if("GYEONGNAM_NEXT".equals(group))return GyeongnamNextDownloadCases.selectCases();
        if(GyeongnamNextDownloadCases.GROUPS.contains(group))return Stream.of(GyeongnamNextDownloadCases.selectCase(group));
        if("JANGHEUNG".equals(group))return Stream.of(JangheungDownloadCases.selectCase()); if("SHINAN".equals(group))return Stream.of(ShinanDownloadCases.selectCase()); if("WANDO".equals(group))return Stream.of(WandoDownloadCases.selectCase()); if("GIMJE".equals(group))return Stream.of(GimjeDownloadCases.selectCase()); if("YEONGDONG".equals(group))return Stream.of(YeongdongDownloadCases.selectCase()); if("YUSEONG_SUPPORT".equals(group))return Stream.of(YuseongSupportDownloadCases.selectCase());
        if("EXISTING_FIRST".equals(group))return ExistingProfileDownloadCases.selectCases();
        if("EXISTING_SECOND".equals(group))return ExistingSecondDownloadCases.selectCases();
        if("SEONGNAM".equals(group))return Stream.of(SeongnamDownloadCases.selectCase());
        if("YEONJE_GURYE".equals(group))return YeonjeGuryeDownloadCases.selectCases();
        if("YEONJE_FIRST_HALF".equals(group))return Stream.of(YeonjeGuryeDownloadCases.selectYeonjeFirstHalfCase());
        if("GWANGJU_SEOGU".equals(group))return Stream.of(GwangjuSeoguDownloadCases.selectCase());
        if(JeonbukFirstDownloadCases.GROUPS.contains(group))return Stream.of(JeonbukFirstDownloadCases.selectCase(group));
        if(JeonbukSecondDownloadCases.GROUPS.contains(group))return Stream.of(JeonbukSecondDownloadCases.selectCase(group));
        if(JeonnamFirstDownloadCases.GROUPS.contains(group))return Stream.of(JeonnamFirstDownloadCases.selectCase(group));
        if(JeonnamSecondDownloadCases.GROUPS.contains(group))return Stream.of(JeonnamSecondDownloadCases.selectCase(group));
        if("JANGSEONG".equals(group))return Stream.of(JeonnamThirdDownloadCases.selectJangseongCase());
        if(GyeongbukFirstDownloadCases.GROUPS.contains(group))return Stream.of(GyeongbukFirstDownloadCases.selectCase(group));
        if(CapitalBoardDownloadCases.GROUPS.contains(group))return Stream.of(CapitalBoardDownloadCases.selectCase(group));
        if(CapitalThirdDownloadCases.GROUPS.contains(group))return Stream.of(CapitalThirdDownloadCases.selectCase(group));
        if("HWASUN".equals(group))return Stream.of(HwasunDownloadCases.selectCase());
        if("SOKCHO".equals(group))return Stream.of(SokchoDownloadCases.selectCase());
        if("SEOUL_EIGHTH".equals(group))return SeoulEighthDownloadCases.GROUPS.stream().sorted().map(SeoulEighthDownloadCases::selectCase);
        if(SeoulEighthDownloadCases.GROUPS.contains(group))return Stream.of(SeoulEighthDownloadCases.selectCase(group));
        if("YEONGAM".equals(group))return Stream.of(YeongamDownloadCases.selectCase());
        if("DAMYANG".equals(group))return Stream.of(DamyangDownloadCases.selectCase());
        if("NOWON".equals(group))return Stream.of(NowonDownloadCases.selectCase());
        if("SEOHAE".equals(group))return Stream.of(SeohaeDownloadCases.selectCase()); if("ULSAN_CITY".equals(group))return Stream.of(UlsanCityDownloadCases.selectCase());
        if("OSAN".equals(group))return Stream.of(OsanDownloadCases.selectCase());
        if("DAEJEON_AGGREGATOR".equals(group))return Stream.of(DaejeonAggregatorDownloadCases.selectCase());
        if("INCHEON_CITY".equals(group))return Stream.of(IncheonCityDownloadCases.selectCase());
        if("DAEGU_CITY".equals(group))return Stream.of(DaeguCityDownloadCases.selectCase());
        if("GEUMCHEON".equals(group))return Stream.of(GeumcheonDownloadCases.selectCase());
        if("POHANG".equals(group))return Stream.of(PohangDownloadCases.selectCase());
        if("ANDONG".equals(group))return Stream.of(AndongDownloadCases.selectCase());
        if("SANGJU".equals(group))return Stream.of(SangjuDownloadCases.selectCase());
        if("GUNWI".equals(group))return Stream.of(GunwiDownloadCases.selectCase());
        if("DAEGU_SEOGU".equals(group))return Stream.of(DaeguSeoguDownloadCases.selectCase());
        if("DAEGU_DONGGU".equals(group))return Stream.of(DaeguDongguDownloadCases.selectCase());
        if("HONGCHEON".equals(group))return Stream.of(HongcheonDownloadCases.selectCase());
        if("SAMCHEOK".equals(group))return Stream.of(SamcheokDownloadCases.selectCase());
        if("PYEONGCHANG".equals(group))return Stream.of(PyeongchangDownloadCases.selectCase());
        if("CHUNCHEON".equals(group))return Stream.of(ChuncheonDownloadCases.selectCase());
        if(GangwonProvinceDownloadCases.GROUPS.contains(group))return Stream.of(GangwonProvinceDownloadCases.selectCase(group));
        if(CapitalEighthDownloadCases.GROUPS.contains(group))return Stream.of(CapitalEighthDownloadCases.selectCase(group));
        if("SEJONG".equals(group))return Stream.of(SejongDownloadCases.selectCase());
        if(DaejeonNextDownloadCases.GROUPS.contains(group))return Stream.of(DaejeonNextDownloadCases.selectCase(group));
        if(MetroNextDownloadCases.GROUPS.contains(group))return Stream.of(MetroNextDownloadCases.selectCase(group));
        if(IncheonThirdDownloadCases.GROUPS.contains(group))return Stream.of(IncheonThirdDownloadCases.selectCase(group));
        if(IncheonSecondDownloadCases.GROUPS.contains(group))return Stream.of(IncheonSecondDownloadCases.selectCase(group));
        if(IncheonFirstDownloadCases.GROUPS.contains(group))return Stream.of(IncheonFirstDownloadCases.selectCase(group));
        if(SeoulSeventhDownloadCases.GROUPS.contains(group))return Stream.of(SeoulSeventhDownloadCases.selectCase(group));
        if(SeoulSixthDownloadCases.GROUPS.contains(group))return Stream.of(SeoulSixthDownloadCases.selectCase(group));
        if(SeoulFifthDownloadCases.GROUPS.contains(group))return Stream.of(SeoulFifthDownloadCases.selectCase(group));
        if(SeoulFourthDownloadCases.GROUPS.contains(group))return Stream.of(SeoulFourthDownloadCases.selectCase(group));
        if("SEODAEMUN".equals(group))return Stream.of(SeodaemunDownloadCases.selectCase());
        if("MAPO".equals(group))return Stream.of(MapoDownloadCases.selectCase());
        if(JeonnamFifthDownloadCases.GROUPS.contains(group))return Stream.of(JeonnamFifthDownloadCases.selectCase(group));
        if(JeonnamFourthDownloadCases.GROUPS.contains(group))return Stream.of(JeonnamFourthDownloadCases.selectCase(group));
        if(JeonbukThirdDownloadCases.GROUPS.contains(group))return Stream.of(JeonbukThirdDownloadCases.selectCase(group));
        if(ChungcheongSixthDownloadCases.GROUPS.contains(group))return Stream.of(ChungcheongSixthDownloadCases.selectCase(group));
        if(ChungcheongFifthDownloadCases.GROUPS.contains(group))return Stream.of(ChungcheongFifthDownloadCases.selectCase(group));
        if(ChungcheongFourthDownloadCases.GROUPS.contains(group))return Stream.of(ChungcheongFourthDownloadCases.selectCase(group));
        if(ChungcheongThirdDownloadCases.GROUPS.contains(group))return Stream.of(ChungcheongThirdDownloadCases.selectCase(group));
        if(GangwonSecondDownloadCases.GROUPS.contains(group))return Stream.of(GangwonSecondDownloadCases.selectCase(group));
        if(CapitalSeventhDownloadCases.GROUPS.contains(group))return Stream.of(CapitalSeventhDownloadCases.selectCase(group));
        if(CapitalSixthDownloadCases.GROUPS.contains(group))return Stream.of(CapitalSixthDownloadCases.selectCase(group));
        if(CapitalFifthDownloadCases.GROUPS.contains(group))return Stream.of(CapitalFifthDownloadCases.selectCase(group));
        if(CapitalFourthDownloadCases.GROUPS.contains(group))return Stream.of(CapitalFourthDownloadCases.selectCase(group));
        if(CapitalNextDownloadCases.GROUPS.contains(group))return Stream.of(CapitalNextDownloadCases.selectCase(group));
        if(ChungcheongNextDownloadCases.GROUPS.contains(group))return Stream.of(ChungcheongNextDownloadCases.selectCase(group));
        if(GangwonNextDownloadCases.GROUPS.contains(group))return group.equals("JEONGSEON")?Stream.of(GangwonNextDownloadCases.selectCase(group),GangwonNextDownloadCases.selectJeongseonTitleStopCase()):Stream.of(GangwonNextDownloadCases.selectCase(group));
        if(UlsanFirstDownloadCases.GROUPS.contains(group))return Stream.of(UlsanFirstDownloadCases.selectCase(group));
        if(MetroSecondDownloadCases.GROUPS.contains(group))return Stream.of(MetroSecondDownloadCases.selectCase(group));
        if(MetroSaeolFirstDownloadCases.GROUPS.contains(group))return Stream.of(MetroSaeolFirstDownloadCases.selectCase(group));
        if(SeoulFirstDownloadCases.GROUPS.contains(group))return Stream.of(SeoulFirstDownloadCases.selectCase(group));
        if(JejuFirstDownloadCases.GROUPS.contains(group))return Stream.of(JejuFirstDownloadCases.selectCase(group));
        if(GyeongnamThirdDownloadCases.GROUPS.contains(group))return Stream.of(GyeongnamThirdDownloadCases.selectCase(group));
        if(GyeongnamSecondDownloadCases.GROUPS.contains(group))return Stream.of(GyeongnamSecondDownloadCases.selectCase(group));
        if(GyeongnamFirstDownloadCases.GROUPS.contains(group))return Stream.of(GyeongnamFirstDownloadCases.selectCase(group));
        if(GyeongbukSixthDownloadCases.GROUPS.contains(group))return Stream.of(GyeongbukSixthDownloadCases.selectCase(group));
        if(GyeongbukFifthDownloadCases.GROUPS.contains(group))return Stream.of(GyeongbukFifthDownloadCases.selectCase(group));
        if(GyeongbukFourthDownloadCases.GROUPS.contains(group))return Stream.of(GyeongbukFourthDownloadCases.selectCase(group));
        if(GyeongbukThirdDownloadCases.GROUPS.contains(group))return Stream.of(GyeongbukThirdDownloadCases.selectCase(group));
        if(GyeongbukSecondDownloadCases.GROUPS.contains(group))return Stream.of(GyeongbukSecondDownloadCases.selectCase(group));
        if(Set.of("EUMSEONG","NONSAN","DANGJIN","CHEONGYANG").contains(group))return Stream.of(selectChungcheongCase(group));
        if("JEUNGPYEONG".equals(group)||"DANYANG".equals(group)) {
            boolean jp="JEUNGPYEONG".equals(group);String id=jp?"31159":"32263";
            var config=new ChungbukLegacyAttachmentProfileConfiguration();var profile=jp?config.selectJeungpyeongProfileDetails():config.selectDanyangProfileDetails();
            String url="https://"+(jp?"eminwon.jp.go.kr":"eminwon.danyang.go.kr")+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
                    +"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
            var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
            return Stream.of(new ObservationCase(group+"-"+id,jp?"증평군 소상공인 지원자금 이차보전금 신청 공고":"2026년 단양군 소상공인 이차보전금 지원 사업 공고",
                    new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,jp?"LGS-000142":"LGS-000146","SAFE_SAEOL_EMINWON"),
                    // 실제 서비스와 같이 V61의 collection_endpoint_url을 본문 요청의 기준으로 사용한다.
                    profile,"https://"+(jp?"eminwon.jp.go.kr":"eminwon.danyang.go.kr")+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",1,jp?TitleLayout.HAMAN_LABEL:TitleLayout.DANYANG_LABEL));
        }
        if("SUSEONG".equals(group)) return Stream.of(
                selectSuseongDalseoCase(group,"52705","2026년 수성구 소상공인 정책자금 이차보전 지원 사업 공고",1));
        if("DALSEO".equals(group)) return Stream.of(
                selectSuseongDalseoCase(group,"54290","2026년 소상공인 경영안정자금 지원사업 변경 공고",1),
                selectSuseongDalseoCase(group,"53625","「2026년 달서청년 자격증 응시료 지원사업」공고",1),
                selectSuseongDalseoCase(group,"42293","2023년 하반기 소상공인 경영안정자금 지원사업 변경 공고",1));
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
    private static ObservationCase selectSuseongDalseoCase(String group,String id,String title,int count) {
        boolean suseong="SUSEONG".equals(group);
        if(!suseong&&!"DALSEO".equals(group))throw new IllegalArgumentException("UNKNOWN_SUSEONG_DALSEO_GROUP");
        String host=suseong?"eminwon.suseong.kr":"eminwon.dalseo.daegu.kr";
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,
                suseong?"LGS-000050":"LGS-000051","SAFE_SAEOL_EMINWON"),suseong?new SaeolGetAttachmentProfileConfiguration().selectSuseongProfileDetails():new DalseoPostAttachmentDiscoveryProfile(),url,count,
                suseong?TitleLayout.SUSEONG_LABEL:TitleLayout.DALSEO_LABEL);
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
    private static ObservationCase selectChungcheongCase(String group) {
        var config=new ChungcheongAttachmentProfileConfiguration();
        var profile=switch(group){case "EUMSEONG"->config.selectEumseongProfileDetails();case "NONSAN"->config.selectNonsanProfileDetails();case "DANGJIN"->config.selectDangjinProfileDetails();default->config.selectCheongyangProfileDetails();};
        String id=switch(group){case "EUMSEONG"->"52473";case "NONSAN"->"50928";case "DANGJIN"->"57265";default->"37758";};
        String title=switch(group){case "EUMSEONG"->"2026년도 음성형 소상공인 지원자금 지원계획 공고(3차)";case "NONSAN"->"2026년 소상공인 노란우산 공제가입 장려금 지원사업 공고";case "DANGJIN"->"2026년 소상공인 화재보험료 지원사업(2차) 공고";default->"2026년 청양군 하반기 소상공인 화재보험료 지원사업 공고";};
        String host=profile.selectApprovedHosts().iterator().next();
        String url="https://"+host+"/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+id+"&subCheck=Y";
        // 본문 요청은 운영 서비스와 같이 V61의 수집 endpoint를 기준으로 검증한다. 대표 포털은 상세 host와 다르다.
        String list=switch(group){case "EUMSEONG"->"https://eminwon.eumseong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy=";case "NONSAN"->"https://eminwon.nonsan21.net/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";case "DANGJIN"->"https://eminwon.dangjin.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";default->"https://eminwon.cheongyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";};
        var binding=profile.selectSourceBindings().getFirst();var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,binding.localSourceCode(),binding.listParserProfileCode()),profile,list,
                Set.of("DANGJIN","CHEONGYANG").contains(group)?2:1,group.equals("EUMSEONG")?TitleLayout.EUMSEONG_LABEL:group.equals("NONSAN")?TitleLayout.NONSAN_LABEL:TitleLayout.DANYANG_LABEL);
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
        Path output=Path.of(System.getProperty("saneb.attachment-observation.report")).toAbsolutePath().normalize();
        // HTTP 요청 전에 중복 경로를 거부한다. 공고 ID는 유지하고 보고서 파일에만 실행 식별자를 붙인다.
        Path reportFile=collectionOnly?selectCollectionReportFile(output,sample.code(),System.getProperty("saneb.attachment-observation.report-label","")):output.resolve(sample.code()+".json");
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
        var budget=selectBudget(profile,collectionOnly,Boolean.getBoolean("saneb.attachment-observation.diagnostic-budget"));Path temporary=null;String stage="RUNTIME";
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
            // 본문 최대2시도·redirect0. 시스템 프로필의 1/2MiB 한도를 전체 예산에 먼저 반영한다.
            budget.reserveBody();stage="BODY_COLLECTION";
            var bodyClient=new LocalGovernmentNoticeProviderContentClient(true,3000,7000,(int)com.saneb.domain.announcementattachment.discovery.AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(profile),0,1,"saneB-notice-collector/1.0");
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
            var download=profile instanceof DongjakAttachmentDiscoveryProfile || profile instanceof UlsanDongguAttachmentDiscoveryProfile
                    ? AttachmentProfileDownloadFlow.selectDownload(profile,detailRequest,detail,com.saneb.domain.announcementattachment.discovery.AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(profile),
                        (next,limit,approved)->client.selectDownload(next,profile.selectApprovedHosts(),r->approved.test(r)&&budget.selectRequestAllowed(r),detail,limit,budget::saveBytes))
                    : client.selectDownload(detailRequest,profile.selectApprovedHosts(),
                    r->budget.selectRequestAllowed(detailRequest,r),detail,com.saneb.domain.announcementattachment.discovery.AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(profile),budget::saveBytes);
            if(!(profile instanceof AttachmentJsonDetailProfile))assertTrue(Set.of("text/html","application/xhtml+xml").contains(download.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT)),"DETAIL_CONTENT_TYPE_CHANGED");
            AttachmentDiscoveryProfile.Result discovered;
            try(var input=Files.newInputStream(detail)) {
                String payload;
                if(profile instanceof AttachmentJsonDetailProfile json){payload=json.selectJsonPayload(input,download.contentType());stage="TITLE_CONFIRMATION";assertEquals(normalized(sample.title()),normalized(json.selectJsonTitle(source,payload)),"TITLE_CHANGED");}
                else {var page=Jsoup.parse(input,null,uri.toASCIIString());payload=page.outerHtml();stage="TITLE_CONFIRMATION";
                    if(sample.titleLayout()==TitleLayout.SONGPA_BOARD && SeoulFourthNoticePage.selectTitle(SeoulFourthNoticePage.Site.SONGPA,SeoulFourthNoticePage.selectRoot(SeoulFourthNoticePage.Site.SONGPA,page)).text().isBlank()) {
                        // 상세 제목을 만들어 넣지 않는다. 현재 공식 목록의 제목/ID와 상세 hidden ID를 독립적으로 대조한다.
                        var listUri=SongpaNoticeIdentity.selectListUri(sample.title());var listRequest=AttachmentPinnedDownloadClient.Request.selectGet(listUri);
                        Path listFile=temporary.resolve("identity-list.bin");
                        try {
                            var listDownload=client.selectDownload(listRequest,Set.of("www.songpa.go.kr"),
                                    next->budget.selectSongpaIdentityListRequestAllowed(listRequest,next),listFile,MIB,budget::saveBytes);
                            assertTrue(Set.of("text/html","application/xhtml+xml").contains(listDownload.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT)),"IDENTITY_LIST_CONTENT_TYPE_CHANGED");
                            try(var listInput=Files.newInputStream(listFile)){SongpaNoticeIdentity.validateListTitle(Jsoup.parse(listInput,null,listUri.toASCIIString()),uri,sample.title());}
                            report.put("detailIdentityMethod","OFFICIAL_LIST_TITLE_AND_DETAIL_ID");report.put("detailTitleAvailable",false);report.put("identityListHash",listDownload.sha256());
                        } finally {Files.deleteIfExists(listFile);}
                    } else validateTitle(page,sample.title(),sample.titleLayout());
                }
                report.put("detailIdentityVerified",true);
                stage="DETAIL_DISCOVERY";discovered=profile.selectDescriptors(source,payload);
            } finally {Files.deleteIfExists(detail);}
            report.put("discoveryStatus",discovered.status());report.put("discoveryComplete",discovered.complete());report.put("discoveredFileCount",discovered.descriptors().size());
            report.put("discoveryWarningCodes",discovered.warnings());
            for(var d:discovered.descriptors()) {var row=new LinkedHashMap<String,Object>();rows.add(row);row.put("locatorHash",AnnouncementAttachmentOfficialObservationTest.selectHash(d.locator()));
                row.put("formatHint",d.expectedFormat());row.put("downloadAllowed",d.downloadAllowed());row.put("status","NOT_RUN");}
            // 부분 파싱 결과에도 프로필이 검증한 descriptor는 수집한다. 미확인 링크는 실행하지 않는다.
            report.put("discoveryError",!discovered.complete()||!Set.of("FOUND","NO_FILES").contains(discovered.status()));
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
                    stage="FILE_DOWNLOAD";var fileRequest=profile.selectDownloadRequest(descriptor);
                    var bytes=selectFileDownload(profile,fileRequest,binary,budget,client,transfer);
                    row.put("bytes",bytes.bytes());row.put("binaryHash",bytes.sha256());stage="FILE_SIGNATURE";
                    row.put("responseMetadata",ObservationResponseMetadata.selectDetails(binary,bytes));
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
            assertTrue(discovered.complete()&&Set.of("FOUND","NO_FILES").contains(discovered.status()),"DISCOVERY_INCOMPLETE");
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
            Files.createDirectories(output);
            if(collectionOnly)saveCollectionReport(reportFile,report);
            else JSON.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(),report);
        }
    }
    static Path selectCollectionReportFile(Path output,String caseCode,String label) {
        if(caseCode==null||!caseCode.matches("[A-Z0-9][A-Z0-9_-]{0,99}")
                ||label==null||!label.isEmpty()&&!label.matches("[A-Z0-9][A-Z0-9_-]{0,63}"))
            throw new IllegalArgumentException("COLLECTION_REPORT_IDENTIFIER_INVALID");
        Path report=output.resolve(caseCode+(label.isEmpty()?"":"-"+label)+".json");
        if(Files.exists(report,LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException("COLLECTION_REPORT_ALREADY_EXISTS_USE_NEW_LABEL");
        return report;
    }
    static void saveCollectionReport(Path path,Map<String,Object> report) throws java.io.IOException {
        // 사전 검사 이후 다른 실행이 파일을 만들더라도 원자적 CREATE_NEW로 기존 근거를 보존한다.
        try(var stream=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            JSON.writerWithDefaultPrettyPrinter().writeValue(stream,report);
        }
    }
    static void saveCollectionOnlySummary(Map<String,Object> report,List<Map<String,Object>> rows,int expectedCount) {
        boolean complete=Boolean.TRUE.equals(report.get("discoveryComplete"))
                && Set.of("FOUND","NO_FILES").contains(report.getOrDefault("discoveryStatus","NOT_RUN"))
                && expectedCount==rows.size() && rows.stream().allMatch(row->"DOWNLOADED".equals(row.get("status")));
        report.put("status",complete?"COLLECTION_ONLY_OBSERVED_NOT_APPROVED":"COLLECTION_ONLY_PARTIAL_NOT_APPROVED");
        report.put("collectionStageComplete",complete);
        report.put("downloadedFileCount",rows.stream().filter(row->"DOWNLOADED".equals(row.get("status"))).count());
        report.put("failedFileCount",rows.stream().filter(row->"FAILED".equals(row.get("status"))).count());
        report.put("unsupportedFileCount",rows.stream().filter(row->"UNSUPPORTED_NOT_DOWNLOADED".equals(row.get("status"))).count());
        report.put("notRunFileCount",rows.stream().filter(row->"NOT_RUN".equals(row.get("status"))).count());
        report.put("fileListChanged",expectedCount!=rows.size());
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
        if(layout==TitleLayout.BUSAN_CITY_BOARD) {
            var views=page.select("div.boardView");assertEquals(1,views.size(),"TITLE_STRUCTURE_CHANGED");
            var titles=views.getFirst().select("div.form-group > h4.form-data-subject");
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.GIMHAE_BOARD||layout==TitleLayout.CHANGNYEONG_BOARD){String host=layout==TitleLayout.GIMHAE_BOARD?"www.gimhae.go.kr":"www.cng.go.kr";assertEquals(normalized(expected),normalized(GyeongnamNextNoticePage.selectTitle(GyeongnamNextNoticePage.selectRoot(page,host)).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.BUPYEONG_PORTAL){var root=BupyeongNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(BupyeongNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.CHEONAN_BOARD){assertEquals(normalized(expected),normalized(CheonanNoticePage.selectTitle(CheonanNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;}if(layout==TitleLayout.SEOCHEON_BOARD){assertEquals(normalized(expected),normalized(SeocheonNoticePage.selectTitle(SeocheonNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;}if(layout==TitleLayout.GOESAN_BOARD){var titles=page.select("form[name=form1][method=post] table.table_view > tbody > tr > th:matchesOwn(^제목$) + td");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}if(layout==TitleLayout.GANGDONG_PORTAL){var root=GangdongNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GangdongNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}if(layout==TitleLayout.SEOUL_GANGSEO_BOARD){var root=SeoulGangseoNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(SeoulGangseoNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.NAMDONG_PORTAL){var root=NamdongNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(NamdongNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GYEONGBUK_PROVINCE_BOARD){var root=GyeongbukProvinceNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GyeongbukProvinceNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.YEONGGWANG_BOARD){var root=YeonggwangNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(YeonggwangNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.ICHEON_BOARD){var root=IcheonNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(IcheonNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.CHEONGJU_BOARD){var root=CheongjuNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(CheongjuNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GWANGYANG_BOARD){var root=GwangyangNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GwangyangNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.ULJU_BOARD){var root=UljuNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(UljuNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.HWASEONG_BOARD){var root=HwaseongNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(HwaseongNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.ULSAN_DONGGU_BOARD){var root=UlsanDongguNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(UlsanDongguNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.DONGJAK_POST_BOARD){var root=DongjakNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(DongjakNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.ONGJIN_PORTAL){var root=OngjinNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(OngjinNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GYEONGNAM_PROVINCE_BOARD){var root=GyeongnamProvinceNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GyeongnamProvinceNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.CHUNGNAM_PROVINCE_BOARD){var root=ChungnamProvinceNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(ChungnamProvinceNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GEOJE_BOARD){var root=GeojeNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GeojeNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.UIRYEONG_BOARD){var root=UiryeongNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(UiryeongNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SANCHEONG_BOARD){var root=SancheongNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(SancheongNoticePage.selectCell(root,"제목").text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.NAMHAE_BOARD){var titles=page.select("form#saeolGosiVO > div.bbs1view1 > h1.h1");assertEquals(1,titles.size(),"TITLE_SELECTOR_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GEOCHANG_BOARD){var titles=page.select("form#saeolGosiVO > div.bbs1view1 > h1.h1");assertEquals(1,titles.size(),"TITLE_SELECTOR_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.HADONG_BOARD){var titles=page.select("form#saeolGosiVO > div.bbs1view1 > h1.h1");assertEquals(1,titles.size(),"TITLE_SELECTOR_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SACHEON_BOARD){assertEquals(normalized(expected),normalized(SacheonNoticePage.selectTitle(SacheonNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.JANGHEUNG_BOARD){assertEquals(normalized(expected),normalized(JangheungNoticePage.selectTitle(JangheungNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SHINAN_BOARD){assertEquals(normalized(expected),normalized(ShinanNoticePage.selectCell(ShinanNoticePage.selectRoot(page),"제목").text()),"TITLE_CHANGED");return;} if(layout==TitleLayout.WANDO_BOARD){assertEquals(normalized(expected),normalized(WandoNoticePage.selectTitle(WandoNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;} if(layout==TitleLayout.GIMJE_BOARD){assertEquals(normalized(expected),normalized(GimjeNoticePage.selectTitle(GimjeNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;} if(layout==TitleLayout.YEONGDONG_BOARD){assertEquals(normalized(expected),normalized(YeongdongNoticePage.selectTitle(YeongdongNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;} if(layout==TitleLayout.SEONGNAM_BOARD){assertEquals(normalized(expected),normalized(SeongnamNoticePage.selectTitle(SeongnamNoticePage.selectRoot(page)).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.YEONJE_BOARD||layout==TitleLayout.GURYE_BOARD){var site=layout==TitleLayout.YEONJE_BOARD?YeonjeGuryeNoticePage.Site.YEONJE:YeonjeGuryeNoticePage.Site.GURYE;assertEquals(normalized(expected),normalized(YeonjeGuryeNoticePage.selectTitle(YeonjeGuryeNoticePage.selectRoot(page,site),site).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GWANGJU_SEOGU_BOARD){var root=com.saneb.domain.announcementsource.provider.content.GwangjuSeoguNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.GwangjuSeoguNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.WONJU_BOARD) {
            var tables=page.select("div.bbs_wrap > div.p-wrap.bbs.bbs__view > table.p-table");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("th").stream().filter(x->x.closest("table")==table&&"제목".equals(x.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");
            var cell=labels.getFirst().nextElementSibling();assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(cell.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.DAEJEON_SEOGU_BOARD) {
            var cards=page.select("div.card.program--view");assertEquals(1,cards.size(),"TITLE_STRUCTURE_CHANGED");
            var titles=cards.getFirst().select("span#notAncmtSj");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");
            assertTrue(titles.getFirst().select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.YEONGAM_BOARD){assertEquals(normalized(expected),normalized(YeongamNoticePage.selectCell(YeongamNoticePage.selectRoot(page),"제목").text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GANGNAM_BOARD||layout==TitleLayout.DOBONG_BOARD){var site=layout==TitleLayout.GANGNAM_BOARD?SeoulEighthNoticePage.Site.GANGNAM:SeoulEighthNoticePage.Site.DOBONG;assertEquals(normalized(expected),normalized(SeoulEighthNoticePage.selectTitle(SeoulEighthNoticePage.selectRoot(page,site),site)),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.NOWON_BOARD){var root=NowonNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(NowonNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SEOHAE_BOARD){var root=SeohaeNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(SeohaeNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;} if(layout==TitleLayout.ULSAN_CITY_BOARD){var root=UlsanCityNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(UlsanCityNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.DAEJEON_AGGREGATOR_BOARD){var root=DaejeonAggregatorNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(DaejeonAggregatorNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.INCHEON_CITY_BOARD){var root=IncheonCityNoticePage.selectTable(page);assertEquals(normalized(expected),normalized(IncheonCityNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.DAEGU_CITY_BOARD){var root=DaeguCityNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(DaeguCityNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GEUMCHEON_BOARD){var root=GeumcheonNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GeumcheonNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.ANDONG_BOARD){var table=AndongNoticePage.selectTable(page);assertEquals(normalized(expected),normalized(AndongNoticePage.selectTitle(table).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SANGJU_BOARD){var table=SangjuNoticePage.selectTable(page);assertEquals(normalized(expected),normalized(SangjuNoticePage.selectTitle(table).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GUNWI_BOARD){var root=GunwiNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GunwiNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.HONGCHEON_BOARD){var root=HongcheonNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(HongcheonNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.PYEONGCHANG_BOARD){var root=PyeongchangNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(PyeongchangNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GANGWON_PROVINCE_BOARD){var root=GangwonProvinceNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(GangwonProvinceNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.PAJU_BOARD,TitleLayout.GWANGMYEONG_BOARD).contains(layout)){var site=com.saneb.domain.announcementsource.provider.content.CapitalEighthNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.CapitalEighthNoticePage.selectRoot(site,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.CapitalEighthNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SEJONG_BOARD){var table=com.saneb.domain.announcementsource.provider.content.SejongNoticePage.selectTable(page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.SejongNoticePage.selectTitle(table).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.YUSEONG_BOARD,TitleLayout.DAEDEOK_BOARD).contains(layout)){var site=com.saneb.domain.announcementsource.provider.content.DaejeonNextNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.DaejeonNextNoticePage.selectRoot(site,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.DaejeonNextNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.GWANGJU_NAMGU_BOARD,TitleLayout.DAEJEON_JUNGGU_BOARD).contains(layout)){var site=com.saneb.domain.announcementsource.provider.content.MetroNextNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.MetroNextNoticePage.selectRoot(site,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.MetroNextNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.GEOMDAN_BOARD,TitleLayout.YEONGJONG_BOARD).contains(layout)){var site=com.saneb.domain.announcementsource.provider.content.IncheonThirdNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.IncheonThirdNoticePage.selectRoot(site,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.IncheonThirdNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.JEMULPO_BOARD,TitleLayout.MICHUHOL_BOARD).contains(layout)){var site=com.saneb.domain.announcementsource.provider.content.IncheonSecondNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.IncheonSecondNoticePage.selectRoot(site,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.IncheonSecondNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.GYEYANG_PORTAL,TitleLayout.GANGHWA_PORTAL).contains(layout)){var site=com.saneb.domain.announcementsource.provider.content.IncheonPortalNoticePage.Site.valueOf(layout.name().replace("_PORTAL",""));var root=com.saneb.domain.announcementsource.provider.content.IncheonPortalNoticePage.selectRoot(site,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.IncheonPortalNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.SEOUL_BOARD,TitleLayout.SEOUL_JUNGGU_BOARD,TitleLayout.YONGSAN_BOARD).contains(layout)){var s=com.saneb.domain.announcementsource.provider.content.SeoulSeventhNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.SeoulSeventhNoticePage.selectRoot(s,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.SeoulSeventhNoticePage.selectTitle(s,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.YANGCHEON_BOARD,TitleLayout.GWANAK_BOARD).contains(layout)){var s=com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage.selectRoot(s,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage.selectTitle(s,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.DONGDAEMUN_BOARD,TitleLayout.SEONGBUK_BOARD,TitleLayout.YEONGDEUNGPO_BOARD).contains(layout)){var s=com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage.selectRoot(s,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage.selectTitle(s,root).text()),"TITLE_CHANGED");return;}
        if(Set.of(TitleLayout.SEONGDONG_BOARD,TitleLayout.SONGPA_BOARD,TitleLayout.GWANGJIN_BOARD).contains(layout)){var s=com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));var root=com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage.selectRoot(s,page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage.selectTitle(s,root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SEODAEMUN_BOARD){var root=com.saneb.domain.announcementsource.provider.content.SeodaemunNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.SeodaemunNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.MAPO_BOARD){var root=com.saneb.domain.announcementsource.provider.content.MapoNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.MapoNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.SOKCHO_BOARD){var root=com.saneb.domain.announcementsource.provider.content.SokchoNoticePage.selectRoot(page);assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.SokchoNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.FILEBOX_LABEL||layout==TitleLayout.SUNCHANG_LABEL){
            var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select(layout==TitleLayout.FILEBOX_LABEL?"table.tstyle":"table.bbs_view");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("th[scope=row]").stream().filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");
            var title=labels.getFirst().nextElementSibling();assertTrue(title!=null&&"td".equals(title.tagName())&&title.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(title.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.EUMSEONG_LABEL||layout==TitleLayout.NONSAN_LABEL){
            boolean nonsan=layout==TitleLayout.NONSAN_LABEL;var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select(nonsan?"table.bbs_view":"table.board_view");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select(nonsan?"thead > tr.head > th[scope=row]":"th.first[scope=row]").stream()
                    .filter(e->e.closest("table")==table&&e.children().isEmpty()&&"제목".equals(e.text().replaceAll("\\s+",""))).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");
            var title=labels.getFirst().nextElementSibling();assertTrue(title!=null&&"td".equals(title.tagName())&&title.children().isEmpty()&&(nonsan?"5":"3").equals(title.attr("colspan")),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(title.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.SUSEONG_LABEL||layout==TitleLayout.DALSEO_LABEL){
            boolean suseong=layout==TitleLayout.SUSEONG_LABEL;var forms=page.select("form[name=form1][method=post]");
            assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");var containers=forms.getFirst().select(suseong?"table.readtype2":"div#bbsView");
            assertEquals(1,containers.size(),"TITLE_STRUCTURE_CHANGED");var container=containers.getFirst();
            var labels=container.select(suseong?"th[scope=row]":":root > div.form_group > dl.title > dt").stream()
                    .filter(e->e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");
            var title=labels.getFirst().nextElementSibling();assertTrue(title!=null&&(suseong?"td":"dd").equals(title.tagName())&&title.children().isEmpty(),"TITLE_STRUCTURE_CHANGED");
            assertEquals(normalized(expected),normalized(title.text()),"TITLE_CHANGED");return;
        }
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
        if(Set.of(TitleLayout.YEONGJU_BOARD,TitleLayout.SEONGJU_BOARD,TitleLayout.YECHEON_BOARD).contains(layout)){var titles=page.select(layout==TitleLayout.YEONGJU_BOARD?"div.news_view > div.data_top > h4":layout==TitleLayout.SEONGJU_BOARD?"form#frm div.bod_view > h4":"div.km-view > div.km-view-title > span.content");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.JEJUSI_BOARD){var titles=page.select("div.board-view-default > div.view-wrap > div.view-header > div.title > strong");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GOSEONG_BOARD||layout==TitleLayout.CHANGWON_BOARD){var titles=page.select(layout==TitleLayout.GOSEONG_BOARD?"div.bdvTitWrap > p.bdvTit":"form#saeolGosiVO > div.bbs1view1 > h1.h1");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GORYEONG_BOARD||layout==TitleLayout.JINJU_BOARD){var titles=page.select(layout==TitleLayout.GORYEONG_BOARD?"table.boardView_table > tbody > tr > th[scope=col][colspan=4]":"div.bbs1view1 > h1.h1");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");var title=titles.getFirst().clone();if(layout==TitleLayout.GORYEONG_BOARD)title.select("span.bV_date").remove();assertEquals(normalized(expected),normalized(title.text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.CHEONGSONG_BOARD){var titles=page.select("form#saeolGosiVO div.board > div.view > div.title > h3");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.YEONGDEOK_BOARD||layout==TitleLayout.ULJIN_BOARD){var titles=page.select(layout==TitleLayout.YEONGDEOK_BOARD?"div.kboard-document-wrap > div.kboard-title":"table.bbs_tablev > tbody > tr > th:matchesOwn(^제목$) + td");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GYEONGBUK_HEADING||layout==TitleLayout.GYEONGBUK_SUBJECT){var titles=page.select("form#detailForm div.bod_view > "+(layout==TitleLayout.GYEONGBUK_SUBJECT?"div.subject":"h4"));assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GYEONGBUK_BOARD||layout==TitleLayout.UISEONG_BOARD){var titles=page.select(layout==TitleLayout.UISEONG_BOARD?"div.boardView > dl.title > dt":"#viewBoardContent > h4.view_tle");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.JANGSEONG_BOARD){var titles=page.select("div.show_info > h3");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;}
        if(layout==TitleLayout.GOKSEONG_BOARD||layout==TitleLayout.JINDO_BOARD){
            var titles=page.select(layout==TitleLayout.GOKSEONG_BOARD?"div.board_view > h3":"div.board_view > dl.view_head > dt > span.txt");
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.JEONNAM_VIEW_TITLE,TitleLayout.JEONNAM_NAJU_TITLE,TitleLayout.JEONNAM_MUAN_TITLE).contains(layout)){
            var titles=page.select(layout==TitleLayout.JEONNAM_NAJU_TITLE?"div.view_title > p.title":layout==TitleLayout.JEONNAM_MUAN_TITLE?"#board_basic_view > div.news_tit > h3":"div.view_titlebox > h3");
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.NAMWON_BOARD||layout==TitleLayout.JEONBUK_BOARD){
            var titles=page.select(layout==TitleLayout.NAMWON_BOARD?"table.view_table > thead > tr > td.title > strong":"div.bbs_view > div.bbs_vtop > h4");
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.EUNPYEONG_BOARD||layout==TitleLayout.SEOCHO_BOARD||layout==TitleLayout.DAEGU_NAMGU_BOARD){
            var tables=page.select("form[name=form1][method=post] table."+(layout==TitleLayout.EUNPYEONG_BOARD?"board2":layout==TitleLayout.DAEGU_NAMGU_BOARD?"boardw.wps_100":"view"));assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("th").stream().filter(e->e.closest("table")==table&&"제목".equals(e.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(cell.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.GWANGJU_DONGGU_BOARD||layout==TitleLayout.GWANGJU_BUKGU_BOARD){
            var titles=page.select(layout==TitleLayout.GWANGJU_DONGGU_BOARD?"form[name=form1][method=post] div.tstyle_view > div.title":"form[name=form][method=post] div.board_read_wrap > div.board_read > h3.title");assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertTrue(titles.getFirst().select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.DONGHAE_BOARD||layout==TitleLayout.GW_GOSEONG_BOARD||layout==TitleLayout.GANGWON_SKIN_BOARD) {
            boolean dong=layout==TitleLayout.DONGHAE_BOARD,skin=layout==TitleLayout.GANGWON_SKIN_BOARD;
            var roots=page.select(dong?"form[name=form][method=post] table[width=98%][cellspacing=1][cellpadding=0]":skin?"form[name=form1][method=post] div.skinTb":"form[name=form1][method=post] table.tb_style1");assertEquals(1,roots.size(),"TITLE_STRUCTURE_CHANGED");var root=roots.getFirst();
            var labels=root.select(dong?"td":skin?"div.skinTb-th":"th").stream().filter(e->(skin||e.closest("table")==root)&&e.children().isEmpty()&&"제목".equals(e.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&(skin?cell.hasClass("skinTb-td"):"td".equals(cell.tagName()))&&cell.select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(cell.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.HAMPYEONG_BOARD) {
            var root=com.saneb.domain.announcementsource.provider.content.HampyeongNoticePage.selectRoot(page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.HampyeongNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.BOSEONG_BOARD) {
            var root=com.saneb.domain.announcementsource.provider.content.BoseongNoticePage.selectRoot(page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.BoseongNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.JEONJU_THIRD_BOARD,TitleLayout.JEONBUK_THIRD_BOARD).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage.Site.valueOf(layout.name().replace("_THIRD_BOARD",""));
            var root=com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.ASAN_BOARD,TitleLayout.SEOSAN_BOARD).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));
            var root=com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.GEUMSAN_BOARD,TitleLayout.BUYEO_BOARD).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));
            var root=com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.HONGSEONG_BOARD,TitleLayout.YESAN_BOARD).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));
            var root=com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.CHUNGBUK_BOARD,TitleLayout.GONGJU_BOARD).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));
            var root=com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.YANGGU_BOARD,TitleLayout.INJE_BOARD).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage.Site.valueOf(layout.name().replace("_BOARD",""));
            var root=com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.POCHEON_PORTAL,TitleLayout.GANGNEUNG_PORTAL).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage.Site.valueOf(layout.name().replace("_PORTAL",""));
            var root=com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.SIHEUNG_PORTAL,TitleLayout.ANSAN_PORTAL).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage.Site.valueOf(layout.name().replace("_PORTAL",""));
            var root=com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.ANSEONG_PORTAL,TitleLayout.UIJEONGBU_PORTAL,TitleLayout.GG_GWANGJU_PORTAL).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage.Site.valueOf(layout.name().replace("_PORTAL",""));
            var root=com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage.selectTitle(root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.GIMPO_PORTAL,TitleLayout.DONGDUCHEON_PORTAL,TitleLayout.PYEONGTAEK_PORTAL).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage.Site.valueOf(layout.name().replace("_PORTAL",""));
            var root=com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage.selectRoot(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage.selectTitle(site,root).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.NAMYANGJU_PORTAL,TitleLayout.HANAM_PORTAL,TitleLayout.GURI_PORTAL).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.Site.valueOf(layout.name().replace("_PORTAL",""));
            var table=com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.selectTable(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.selectTitle(site,table).text()),"TITLE_CHANGED");return;
        }
        if(Set.of(TitleLayout.YANGJU_PORTAL,TitleLayout.GUNPO_PORTAL,TitleLayout.YEOJU_PORTAL).contains(layout)) {
            var site=com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage.Site.valueOf(layout.name().replace("_PORTAL",""));
            var table=com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage.selectTable(site,page);
            assertEquals(normalized(expected),normalized(com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage.selectTitle(site,table).text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.YONGIN_BOARD) {
            var titles=page.select("form[name=form1][method=post] div#contentDiv.boardGroup > div.boardDefalutView > dl > h3.viewOtherWidth");
            assertEquals(1,titles.size(),"TITLE_STRUCTURE_CHANGED");assertTrue(titles.getFirst().select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(titles.getFirst().text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.JINCHEON_BOARD) {
            var tables=page.select("form[name=form1][method=post] table.contTable");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("th").stream().filter(e->e.closest("table")==table&&"제목".equals(e.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(cell.text()),"TITLE_CHANGED");return;
        }
        if(layout==TitleLayout.ULSAN_JUNGGU_BOARD) {
            var tables=page.select("form[name=form1][method=post] table.public_view");assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
            var labels=table.select("th").stream().filter(e->e.closest("table")==table&&"제목".equals(e.text().strip())).toList();assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
            assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");assertEquals(normalized(expected),normalized(cell.text()),"TITLE_CHANGED");return;
        }
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
        if(layout==TitleLayout.HAMAN_LABEL||layout==TitleLayout.DANYANG_LABEL) {
            boolean danyang=layout==TitleLayout.DANYANG_LABEL;
            var forms=page.select(danyang?"form[name=form][method=post]":"form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
            var tables=forms.getFirst().select(danyang?"table[width=98%][border=0][cellspacing=1][cellpadding=0]":"table[width=100%][border=0][cellspacing=1][cellpadding=0]");
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

    static Budget selectBudget(AttachmentDiscoveryProfile profile,boolean collectionOnly,boolean diagnostic){
        // 본문 예약2 + 상세1 + 공식 첨부2개 각각의 익명 상세/파일2요청. 파일·시간·바이트 한도는 유지한다.
        if(collectionOnly&&"LOCAL_SEONGJU_BOARD_V1".equals(profile.selectProfileCode()))return new Budget(profile,7,23*MIB);
        if("LOCAL_SONGPA_BOARD_V1".equals(profile.selectProfileCode()))return new Budget(profile,7,23*MIB);
        if(collectionOnly&&"LOCAL_GANGDONG_POST_V1".equals(profile.selectProfileCode()))return new Budget(profile,9,23*MIB);
        if(collectionOnly&&Set.of("LOCAL_TAEBAEK_BBS_V1","LOCAL_JECHEON_BBS_V1","LOCAL_CHUNGJU_EMINWON_V1","LOCAL_WONJU_BBS_V1","LOCAL_DAEJEON_SEOGU_V1","LOCAL_GWANGJU_SEOGU_GET_V1","LOCAL_YEONJE_GET_V1","LOCAL_GURYE_POST_V1","LOCAL_SEONGNAM_GET_V1","LOCAL_YEONGDONG_BOARD_V1","LOCAL_GIMJE_BOARD_V1","LOCAL_WANDO_POST_V1","LOCAL_SHINAN_POST_V1","LOCAL_JANGHEUNG_GET_V1","LOCAL_GIMHAE_SCMS_GET_V1","LOCAL_CHANGNYEONG_SCMS_GET_V1","LOCAL_SACHEON_BOARD_V1","LOCAL_HADONG_SCMS_V1","LOCAL_GEOCHANG_SCMS_V1","LOCAL_NAMHAE_SCMS_V1","LOCAL_SANCHEONG_BBS_V1","LOCAL_UIRYEONG_GET_V1","LOCAL_GEOJE_GET_V1","LOCAL_GYEONGNAM_PROVINCE_V1","LOCAL_CHUNGNAM_PROVINCE_V1","LOCAL_ONGJIN_PORTAL_V1","LOCAL_BUPYEONG_PORTAL_V1","LOCAL_DONGJAK_POST_DETAIL_V1","LOCAL_ULSAN_DONGGU_GET_V1","LOCAL_HWASEONG_BOARD_V1","LOCAL_ULJU_BOARD_V1","LOCAL_GWANGYANG_POST_V1","LOCAL_CHEONGJU_POST_V1","LOCAL_ICHEON_BOARD_V1","LOCAL_YEONGGWANG_GET_V1","LOCAL_GYEONGBUK_PROVINCE_V1","LOCAL_NAMDONG_PORTAL_V1","LOCAL_SEOUL_GANGSEO_GET_V1","LOCAL_GANGDONG_POST_V1").contains(profile.selectProfileCode()))return new Budget(profile,6,23*MIB);
        return new Budget(profile,diagnostic);
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
            boolean boundedSaeol=Set.of("LOCAL_SUSEONG_GET_V1","LOCAL_DALSEO_POST_V1","LOCAL_BUSAN_JUNGGU_GET_V1","LOCAL_BUSAN_SEOGU_GET_V1","LOCAL_BUSAN_DONGGU_GET_V1","LOCAL_SAHA_GET_V1","LOCAL_DAEGU_DALSEONG_GET_V1","LOCAL_HAMAN_GET_V1","LOCAL_DAEGU_JUNGGU_GET_V1","LOCAL_DONGNAE_GET_V1","LOCAL_BUSANJIN_GET_V1","LOCAL_GEUMJEONG_GET_V1","LOCAL_SUYEONG_GET_V1","LOCAL_SASANG_GET_V1","LOCAL_HAEUNDAE_GET_V1","LOCAL_BUSAN_BUKGU_POST_V1","LOCAL_BUSAN_GANGSEO_GET_V1").contains(profile.selectProfileCode());
            boolean gijang=Set.of("LOCAL_GIJANG_GET_V1",YeongdoAttachmentDiscoveryProfile.CODE).contains(profile.selectProfileCode());
            boolean gangbuk="LOCAL_GANGBUK_LEGAL_GET_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_BUSAN_LEGAL_GET_V1".equals(profile.selectProfileCode());
            boolean guro=GuroGosiAttachmentDiscoveryProfile.CODE.equals(profile.selectProfileCode());
            boolean standardCollection=Set.of("LOCAL_HOENGSEONG_BBS_V1","LOCAL_YEONGWOL_BBS_V1").contains(profile.selectProfileCode());
            boundedSaeol|=Set.of("LOCAL_JEUNGPYEONG_GET_V1","LOCAL_DANYANG_GET_V1").contains(profile.selectProfileCode());
            boundedSaeol|=Set.of("LOCAL_EUMSEONG_GET_V1","LOCAL_NONSAN_GET_V1","LOCAL_DANGJIN_GET_V1","LOCAL_CHEONGYANG_GET_V1").contains(profile.selectProfileCode());
            boundedSaeol|=JeonbukFirstDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_GET_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=JeonbukSecondDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_GET_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=JeonnamFirstDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_NOTICE_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=JeonnamSecondDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_NOTICE_V1").equals(profile.selectProfileCode()));
            boundedSaeol|="LOCAL_JANGSEONG_POST_V1".equals(profile.selectProfileCode());
            boundedSaeol|=GyeongbukThirdDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongbukSecondDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=Set.of("LOCAL_DAEGU_NAMGU_GET_V1","LOCAL_DAEGU_BUKGU_POST_V1").contains(profile.selectProfileCode());
            boundedSaeol|=CapitalBoardDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=CapitalThirdDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=ChungcheongSixthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|="LOCAL_HAMPYEONG_NOTICE_V1".equals(profile.selectProfileCode());
            boundedSaeol|=Set.of("LOCAL_HWASUN_GET_V1","LOCAL_GOESAN_GET_V1","LOCAL_SEOCHEON_GET_V1","LOCAL_CHEONAN_GET_V1").contains(profile.selectProfileCode());
            boundedSaeol|="LOCAL_SOKCHO_PORTAL_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_MAPO_PORTAL_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_SEODAEMUN_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_DAEGU_SEOGU_PORTAL_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_DAEGU_DONGGU_PORTAL_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_NOWON_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_YEONGAM_POST_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_DAMYANG_JSON_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_SEOHAE_BOARD_V1".equals(profile.selectProfileCode()); boundedSaeol|="LOCAL_ULSAN_CITY_CITYNET_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_OSAN_PORTAL_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_DAEJEON_AGGREGATOR_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_INCHEON_CITY_CITYNET_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_DAEGU_CITY_GOSI_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_GEUMCHEON_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_POHANG_PORTAL_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_ANDONG_TABLE_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_SANGJU_GOSI_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_GUNWI_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_HONGCHEON_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_SAMCHEOK_SCMS_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_PYEONGCHANG_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_CHUNCHEON_JSON_V1".equals(profile.selectProfileCode());
            boundedSaeol|="LOCAL_GANGWON_PROVINCE_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|=CapitalEighthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|="LOCAL_SEJONG_BOARD_V1".equals(profile.selectProfileCode());
            boundedSaeol|=DaejeonNextDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=MetroNextDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=IncheonThirdDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=IncheonSecondDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=IncheonFirstDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=SeoulSeventhDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=SeoulSixthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=SeoulFifthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=SeoulFourthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|="LOCAL_BOSEONG_NOTICE_V1".equals(profile.selectProfileCode());
            boundedSaeol|=JeonbukThirdDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=ChungcheongFifthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=ChungcheongFourthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=ChungcheongThirdDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GangwonSecondDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=CapitalSeventhDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=CapitalSixthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=CapitalFifthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=CapitalFourthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=Set.of("LOCAL_YONGIN_GET_V1","LOCAL_UIWANG_POST_V1").contains(profile.selectProfileCode());
            boundedSaeol|=Set.of("LOCAL_JINCHEON_GET_V1","LOCAL_TAEAN_POST_V1").contains(profile.selectProfileCode());
            boundedSaeol|=GangwonNextDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+(group.equals("YANGYANG")?"_POST_V1":"_GET_V1")).equals(profile.selectProfileCode()));
            boundedSaeol|=Set.of("LOCAL_ULSAN_JUNGGU_POST_V1","LOCAL_ULSAN_BUKGU_GET_V1").contains(profile.selectProfileCode());
            boundedSaeol|=MetroSecondDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_GET_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=SeoulFirstDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_GET_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=JejuFirstDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_GET_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongnamThirdDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_SCMS_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongnamSecondDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_BOARD_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongnamFirstDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_GET_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongbukSixthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_COUNTY_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongbukFifthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_DIRECT_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongbukFourthDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            boundedSaeol|=GyeongbukFirstDownloadCases.GROUPS.stream().anyMatch(group->("LOCAL_"+group+"_PORTAL_V1").equals(profile.selectProfileCode()));
            maximumRequests="LOCAL_DOBONG_BOARD_V1".equals(profile.selectProfileCode())?12:"LOCAL_GANGNAM_BOARD_V1".equals(profile.selectProfileCode())?8:Set.of("LOCAL_GYEYANG_PORTAL_V1","LOCAL_YEONGJU_BOARD_V1","LOCAL_EUNPYEONG_GET_V1","LOCAL_SEODAEMUN_BOARD_V1","LOCAL_YUSEONG_BOARD_V1").contains(profile.selectProfileCode())?7:standardCollection?5:guro?8:gangbuk?20:gijang?7:boundedSaeol?6:namgu?5:diagnostic?20:44;maximumBytes=(Set.of("LOCAL_GANGNAM_BOARD_V1","LOCAL_DOBONG_BOARD_V1").contains(profile.selectProfileCode())?43:standardCollection?43:guro?23:gangbuk?32:gijang?23:boundedSaeol?23:namgu?24:diagnostic?32:80)*MIB+3*(com.saneb.domain.announcementattachment.discovery.AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(profile)-MIB);}
        long requests,bytes;
        boolean songpaIdentityListRequested;
        void reserveBody(){if(requests!=0||bytes!=0)throw new IllegalStateException("BODY_BUDGET_ALREADY_RESERVED");requests=2;bytes=2*com.saneb.domain.announcementattachment.discovery.AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(profile);}
        boolean selectRequestAllowed(AttachmentPinnedDownloadClient.Request r){if(!profile.selectApprovedRequest(r)||requests>=maximumRequests||Thread.currentThread().isInterrupted())return false;requests++;return true;}
        boolean selectRequestAllowed(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request r){
            return profile.selectApprovedRequest(initial,r)&&selectRequestAllowed(r);
        }
        boolean selectSongpaIdentityListRequestAllowed(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request next){
            if(!"LOCAL_SONGPA_BOARD_V1".equals(profile.selectProfileCode()) || initial==null || !initial.equals(next)
                    || !"GET".equals(initial.method()) || initial.referer()!=null || initial.utf8RedirectOctets() || !initial.form().isEmpty())return false;
            var uri=initial.uri();
            try {var q=CapitalThirdNoticePage.selectParameters(uri.getRawQuery());if(!SongpaNoticeIdentity.selectListUri(q.get("searchKrwd")).equals(uri))return false;}
            catch(IllegalArgumentException exception){return false;}
            if(songpaIdentityListRequested || requests>=maximumRequests || Thread.currentThread().isInterrupted())return false;songpaIdentityListRequested=true;requests++;return true;
        }
        boolean saveBytes(long count){if(count<0||bytes>maximumBytes-count||Thread.currentThread().isInterrupted())return false;bytes+=count;return true;}
    }
}
