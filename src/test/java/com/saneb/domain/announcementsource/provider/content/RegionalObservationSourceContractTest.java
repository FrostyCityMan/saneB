package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegionalObservationSourceContractTest {
    private static final String ENDPOINT_PATH="/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
    private static final Map<String,String> HOSTS=Map.ofEntries(
            Map.entry("NONSAN","eminwon.nonsan21.net"), Map.entry("DANGJIN","eminwon.dangjin.go.kr"),
            Map.entry("CHEONGYANG","eminwon.cheongyang.go.kr"), Map.entry("HWASUN","eminwon.hwasun.go.kr"),
            Map.entry("JEUNGPYEONG","eminwon.jp.go.kr"), Map.entry("DANYANG","eminwon.danyang.go.kr"),
            Map.entry("WANJU","eminwon.wanju.go.kr"), Map.entry("JINAN","eminwon.jinan.go.kr"),
            Map.entry("MUJU","eminwon.muju.go.kr"), Map.entry("JANGSU","eminwon.jangsu.go.kr"),
            Map.entry("IMSIL","eminwon.imsil.go.kr"));
    private static final Map<String,String> PORTALS=Map.ofEntries(
            Map.entry("NONSAN","https://www.nonsan.go.kr/kor/html/sub03/03010201.html"),
            Map.entry("DANGJIN","https://www.dangjin.go.kr/kor/sub03_02_01_01.do"),
            Map.entry("CHEONGYANG","https://www.cheongyang.go.kr/kor/sub04_02_03.do"),
            Map.entry("HWASUN","https://www.hwasun.go.kr/contents.do?S=S01&M=020104000000"),
            Map.entry("JEUNGPYEONG","https://www.jp.go.kr/kor/sub03_01_03.do"),
            Map.entry("DANYANG","https://www.danyang.go.kr/dy21/976"),
            Map.entry("WANJU","https://www.wanju.go.kr/index.9is?contentUid=ff8080818b024d8e018b274f41c32af7"),
            Map.entry("JINAN","https://www.jinan.go.kr/index.jinan?menuCd=DOM_000000107001014000"),
            Map.entry("MUJU","https://www.muju.go.kr/index.9is?contentUid=ff8080816c5f9d47016cbd3b2a4a006f"),
            Map.entry("JANGSU","https://www.jangsu.go.kr/index.jangsu?menuCd=DOM_000000102001005000"),
            Map.entry("IMSIL","https://www.imsil.go.kr/index.imsil?menuCd=DOM_000000103001005000"));

    @ParameterizedTest @ValueSource(strings={"NONSAN","DANGJIN","CHEONGYANG","HWASUN","JEUNGPYEONG","DANYANG","WANJU","JINAN","MUJU","JANGSU","IMSIL"})
    void usesMigrationEndpointWithoutChangingNoticeIdentityOrApproval(String group) throws Exception {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList().getFirst();
        String endpoint="https://"+HOSTS.get(group)+ENDPOINT_PATH;
        assertThat(sample.listUrl()).isEqualTo(endpoint);
        String migration=Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"));
        if("HWASUN".equals(group)) {
            // V61 후반의 공개 POST endpoint 보정이 앞부분의 NULL보다 우선한다.
            assertThat(migration).contains("('LGS-000188', '"+PORTALS.get(group)+"', NULL,");
            assertThat(migration).contains("('LGS-000188', '"+endpoint+"', '{");
        } else assertThat(migration).contains("('"+sample.source().localSourceCode()+"', '"+PORTALS.get(group)+"', '"+endpoint+"',");
        var json=new ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var matches=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).toList();
        assertThat(matches).hasSize(1);assertThat(matches.getFirst().path("source")).isEqualTo(json.valueToTree(sample.source()));
        assertThat(matches.getFirst().hasNonNull("expectation")).isFalse();
    }

    @ParameterizedTest @ValueSource(strings={"NONSAN","DANGJIN","CHEONGYANG","HWASUN","JEUNGPYEONG","DANYANG","WANJU","JINAN","MUJU","JANGSU","IMSIL"})
    void exactHostAndPublicAddressBoundariesRemainEnforced(String group) throws Exception {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList().getFirst();
        var validator=new ProviderContentUrlValidator(host->new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})});
        assertThat(validator.selectValidatedRequest(sample.listUrl(),sample.source().sourceUrl()).detailUri()).isEqualTo(URI.create(sample.source().sourceUrl()));
        assertThatThrownBy(()->validator.selectValidatedRequest(PORTALS.get(group),sample.source().sourceUrl()))
                .isInstanceOfSatisfying(ProviderContentValidationException.class,e->assertThat(e.selectFailureCode()).isEqualTo(ProviderContentCodes.FailureCode.DETAIL_HOST_NOT_ALLOWED));
        assertThatThrownBy(()->validator.selectRedirectUri(URI.create(sample.source().sourceUrl()),PORTALS.get(group),HOSTS.get(group)))
                .isInstanceOfSatisfying(ProviderContentValidationException.class,e->assertThat(e.selectFailureCode()).isEqualTo(ProviderContentCodes.FailureCode.DETAIL_HOST_NOT_ALLOWED));
        var privateDns=new ProviderContentUrlValidator(host->new InetAddress[]{InetAddress.getByAddress(new byte[]{10,0,0,1})});
        assertThatThrownBy(()->privateDns.selectValidatedRequest(sample.listUrl(),sample.source().sourceUrl()))
                .isInstanceOfSatisfying(ProviderContentValidationException.class,e->assertThat(e.selectFailureCode()).isEqualTo(ProviderContentCodes.FailureCode.ADDRESS_BLOCKED));
    }

    @Test void eumseongKeepsItsExistingRegisteredListQuery() {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("EUMSEONG").toList().getFirst();
        assertThat(sample.listUrl()).startsWith("https://eminwon.eumseong.go.kr/");
        assertThat(sample.listUrl()).contains("method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage");
    }

    @ParameterizedTest @ValueSource(strings={"IKSAN","SUNCHANG"})
    void unaffectedJeonbukSourcesKeepTheirRegisteredListQuery(String group) {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList().getFirst();
        assertThat(sample.listUrl()).startsWith("https://eminwon."+group.toLowerCase(java.util.Locale.ROOT)+".go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?");
        assertThat(sample.listUrl()).contains("not_ancmt_se_code=01,02,03,04,05");
    }

    @Test void gwangjuNamguPortalContractMustNotBeReplacedWithDirectHost() throws Exception {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("GWANGJU_NAMGU").toList().getFirst();
        String portal="https://www.namgu.gwangju.kr/menu.es?mid=a10604020100";
        assertThat(sample.listUrl()).isEqualTo(portal);
        String migration=Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"));
        assertThat(migration).contains("('LGS-000068', '"+portal+"', NULL,");
        assertThat(migration).doesNotContain("('LGS-000068', 'https://eminwon.namgu.gwangju.kr"+ENDPOINT_PATH+"', '{");
        assertThat(migration).contains("'/api/eminwon/gosiView.es?mid={query:mid}&method=selectOfrNotAncmt'");
        var validator=new ProviderContentUrlValidator(host->new InetAddress[]{InetAddress.getByAddress(new byte[]{8,8,8,8})});
        String portalDetail="https://www.namgu.gwangju.kr/api/eminwon/gosiView.es?mid=a10604020100&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=45698";
        assertThat(validator.selectValidatedRequest(portal,portalDetail).detailUri()).isEqualTo(URI.create(portalDetail));
        // 등록 계약 없이 테스트만 상세 호스트로 치환하여 성공시키지 않는다.
        assertThatThrownBy(()->validator.selectValidatedRequest(sample.listUrl(),sample.source().sourceUrl()))
                .isInstanceOfSatisfying(ProviderContentValidationException.class,e->assertThat(e.selectFailureCode()).isEqualTo(ProviderContentCodes.FailureCode.DETAIL_HOST_NOT_ALLOWED));
    }
}
