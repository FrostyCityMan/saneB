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
    private static final Map<String,String> HOSTS=Map.of("NONSAN","eminwon.nonsan21.net","DANGJIN","eminwon.dangjin.go.kr","CHEONGYANG","eminwon.cheongyang.go.kr","HWASUN","eminwon.hwasun.go.kr");
    private static final Map<String,String> PORTALS=Map.of("NONSAN","https://www.nonsan.go.kr/kor/html/sub03/03010201.html","DANGJIN","https://www.dangjin.go.kr/kor/sub03_02_01_01.do","CHEONGYANG","https://www.cheongyang.go.kr/kor/sub04_02_03.do","HWASUN","https://www.hwasun.go.kr/contents.do?S=S01&M=020104000000");

    @ParameterizedTest @ValueSource(strings={"NONSAN","DANGJIN","CHEONGYANG","HWASUN"})
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

    @ParameterizedTest @ValueSource(strings={"NONSAN","DANGJIN","CHEONGYANG","HWASUN"})
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
}
