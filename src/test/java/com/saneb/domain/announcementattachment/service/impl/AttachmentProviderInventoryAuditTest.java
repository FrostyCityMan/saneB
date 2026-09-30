package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** 운영 연결/외부 요청 없이 읽기 전용 영수증과 현재 코드 등록을 대조한다. 실제 수집 성공 판정기가 아니다. */
class AttachmentProviderInventoryAuditTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private AnnotationConfigApplicationContext selectContext() {
        return new AnnotationConfigApplicationContext(BizInfoAttachmentDiscoveryProfile.class,SeoguSaeolAttachmentDiscoveryProfile.class,
                HwacheonPostAttachmentDiscoveryProfile.class,BusanBukguPostAttachmentDiscoveryProfile.class,SaeolGetAttachmentProfileConfiguration.class,LegalBoardAttachmentProfileConfiguration.class,
                StandardBbsAttachmentProfileConfiguration.class,ChungjuEminwonAttachmentDiscoveryProfile.class,YeongdoAttachmentDiscoveryProfile.class,DalseoPostAttachmentDiscoveryProfile.class,ChungbukLegacyAttachmentProfileConfiguration.class,ChungcheongAttachmentProfileConfiguration.class,JeonbukAttachmentProfileConfiguration.class,JeonnamAttachmentProfileConfiguration.class,GyeongbukPortalAttachmentProfileConfiguration.class,GyeongbukBoardAttachmentProfileConfiguration.class,GyeongbukThirdAttachmentProfileConfiguration.class,GyeongbukDirectAttachmentProfileConfiguration.class,GyeongbukCountyAttachmentProfileConfiguration.class,GoryeongJinjuAttachmentProfileConfiguration.class,GyeongnamBoardAttachmentProfileConfiguration.class,ScmsSaeolAttachmentProfileConfiguration.class,JejuAttachmentProfileConfiguration.class,SeoulSaeolAttachmentProfileConfiguration.class,MetroSaeolAttachmentProfileConfiguration.class,MetroSecondAttachmentProfileConfiguration.class,UlsanAttachmentProfileConfiguration.class,GangwonNextAttachmentProfileConfiguration.class,ChungcheongNextAttachmentProfileConfiguration.class,CapitalNextAttachmentProfileConfiguration.class,CapitalBoardAttachmentProfileConfiguration.class,CapitalThirdAttachmentProfileConfiguration.class,CapitalFourthAttachmentProfileConfiguration.class,CapitalFifthAttachmentProfileConfiguration.class,CapitalSixthAttachmentProfileConfiguration.class,CapitalSeventhAttachmentProfileConfiguration.class,GangwonSecondAttachmentProfileConfiguration.class,ChungcheongThirdAttachmentProfileConfiguration.class,ChungcheongFourthAttachmentProfileConfiguration.class,ChungcheongFifthAttachmentProfileConfiguration.class,ChungcheongSixthAttachmentProfileConfiguration.class,JeonbukThirdAttachmentProfileConfiguration.class,BoseongAttachmentDiscoveryProfile.class,HampyeongAttachmentDiscoveryProfile.class,SokchoAttachmentDiscoveryProfile.class,MapoAttachmentDiscoveryProfile.class,SeodaemunAttachmentDiscoveryProfile.class,SeoulFourthAttachmentProfileConfiguration.class,SeoulFifthAttachmentProfileConfiguration.class,SeoulSixthAttachmentProfileConfiguration.class,SeoulSeventhAttachmentProfileConfiguration.class,IncheonPortalAttachmentProfileConfiguration.class,IncheonSecondAttachmentProfileConfiguration.class,IncheonThirdAttachmentProfileConfiguration.class,MetroNextAttachmentProfileConfiguration.class,DaejeonNextAttachmentProfileConfiguration.class,SejongAttachmentDiscoveryProfile.class,CapitalEighthAttachmentProfileConfiguration.class,GangwonProvinceAttachmentDiscoveryProfile.class,ChuncheonAttachmentDiscoveryProfile.class,PyeongchangAttachmentDiscoveryProfile.class,SamcheokAttachmentDiscoveryProfile.class,HongcheonAttachmentDiscoveryProfile.class,DaeguPortalAttachmentProfileConfiguration.class,DaeguSeoguAttachmentDiscoveryProfile.class,GunwiAttachmentDiscoveryProfile.class,SangjuAttachmentDiscoveryProfile.class,AndongAttachmentDiscoveryProfile.class,PohangAttachmentProfileConfiguration.class,GeumcheonAttachmentDiscoveryProfile.class,DaeguCityAttachmentDiscoveryProfile.class,IncheonCityAttachmentDiscoveryProfile.class,DaejeonAggregatorAttachmentDiscoveryProfile.class,OsanAttachmentProfileConfiguration.class,UlsanCityAttachmentDiscoveryProfile.class,SeohaeAttachmentDiscoveryProfile.class,NowonAttachmentDiscoveryProfile.class,DamyangAttachmentDiscoveryProfile.class,SeoulEighthAttachmentProfileConfiguration.class,YeongamAttachmentDiscoveryProfile.class,GwangjuSeoguAttachmentDiscoveryProfile.class,YeonjeGuryeAttachmentProfileConfiguration.class,SeongnamAttachmentProfileConfiguration.class,YeongdongAttachmentDiscoveryProfile.class,GimjeAttachmentDiscoveryProfile.class,WandoAttachmentDiscoveryProfile.class,ShinanAttachmentDiscoveryProfile.class,JangheungAttachmentDiscoveryProfile.class,GyeongnamNextAttachmentProfileConfiguration.class,SacheonAttachmentDiscoveryProfile.class);
    }
    private List<Target> selectTargets(JsonNode input,boolean enabledOnly) {
        if(!"LOCAL_TARGET_INVENTORY".equals(input.path("kind").asText())||!"on".equals(input.path("readOnly").asText())
                ||!input.path("targets").isArray()||input.path("targets").size()>1000)throw new IllegalArgumentException("INVENTORY_INPUT_INVALID");
        var targets=new ArrayList<Target>();var codes=new HashSet<String>();int enabled=0;
        for(var row:input.path("targets")) {
            if(!row.isArray()||row.size()!=3||!row.get(0).isTextual()||!row.get(0).asText().matches("LGS-[0-9]{6}")
                    ||!row.get(1).isTextual()||!row.get(1).asText().matches("[A-Z0-9_]{1,100}")||!row.get(2).isBoolean()
                    ||!codes.add(row.get(0).asText()))throw new IllegalArgumentException("INVENTORY_INPUT_INVALID");
            if(row.get(2).booleanValue())enabled++;
            if(!enabledOnly||row.get(2).booleanValue()) {
                String code=row.get(0).asText();
                // 비교에만 쓰는 결정적 임시 UUID다. 실제 운영 PK/URL/설정은 수집하거나 결과에 기록하지 않는다.
                targets.add(new Target(UUID.nameUUIDFromBytes(code.getBytes(StandardCharsets.UTF_8)),code,row.get(1).asText(),null,null));
            }
        }
        if(!input.path("allUndeletedCount").isIntegralNumber()||input.path("allUndeletedCount").intValue()!=codes.size()
                ||!input.path("enabledCount").isIntegralNumber()||input.path("enabledCount").intValue()!=enabled)throw new IllegalArgumentException("INVENTORY_COUNT_MISMATCH");
        return targets;
    }
    private ObjectNode selectReport(JsonNode input,JsonNode catalog,List<AttachmentDiscoveryProfile> profiles) {
        var all=AttachmentProviderQaPlan.selectPlan(profiles,selectTargets(input,false));
        var active=AttachmentProviderQaPlan.selectPlan(profiles,selectTargets(input,true));
        var report=JSON.createObjectNode().put("schemaVersion",1).put("scope","OPERATING_TARGET_SNAPSHOT_VS_LOCAL_CODE_REGISTRY")
                .put("observedAt",input.path("observedAt").asText()).put("currentHttpRequests",0).put("productionWriteCount",0)
                .put("isCollectionVerified",false).put("isPolicyQaPassed",false).put("isExpectationApproved",false);
        report.set("allUndeletedSummary",JSON.valueToTree(all.summary()));report.set("enabledSummary",JSON.valueToTree(active.summary()));
        report.set("unboundProfiles",JSON.valueToTree(all.unboundProfiles()));
        var enabledCodes=new HashSet<String>();selectTargets(input,true).forEach(t->enabledCodes.add(t.publicCode()));
        var rows=report.putArray("targets");
        for(var item:all.items()) {
            var row=rows.addObject().put("providerCode",item.providerCode()).put("localSourceCode",item.localSourceCode())
                    .put("listParserProfileCode",item.listParserProfileCode()).put("bindingStatus",item.statusCode())
                    .put("includedInPolicyScope",item.localSourceCode()==null||enabledCodes.contains(item.localSourceCode()));
            // 국가 Provider는 정책 분모에 포함될 뿐 실제 운영 enabled/key/호출 성공을 관측한 것이 아니다.
            if(item.localSourceCode()==null)row.putNull("enabled");else row.put("enabled",enabledCodes.contains(item.localSourceCode()));
            row.set("profiles",JSON.valueToTree(item.profiles()));
            int references=0,expectations=0;
            for(var notice:catalog.path("notices")) {
                if(item.providerCode().equals(notice.path("source").path("providerCode").asText())
                        &&Objects.equals(item.localSourceCode(),notice.path("source").path("localSourceCode").isNull()?null:notice.path("source").path("localSourceCode").asText(null))) {
                    references++;if(notice.hasNonNull("expectation"))expectations++;
                }
            }
            row.put("catalogReferenceCount",references).put("storedExpectationCount",expectations);
        }
        return report;
    }
    private ObjectNode selectFixture() throws Exception {
        return (ObjectNode)JSON.readTree("""
                {"kind":"LOCAL_TARGET_INVENTORY","readOnly":"on","observedAt":"synthetic",
                 "allUndeletedCount":3,"enabledCount":2,"targets":[["LGS-000121","SPRING_BBS",true],
                 ["LGS-000126","OTHER_PARSER",false],["LGS-999999","SPRING_BBS",true]]}
                """);
    }
    @Test void disabledAndUnregisteredSourcesStayVisibleWithoutClaimingProviderWideSupport() throws Exception {
        try(var context=selectContext()) {
            var report=selectReport(selectFixture(),JSON.readTree("{\"notices\":[]}"),List.copyOf(context.getBeansOfType(AttachmentDiscoveryProfile.class).values()));
            assertThat(report.path("allUndeletedSummary").path("targetCount").asInt()).isEqualTo(5);
            assertThat(report.path("enabledSummary").path("targetCount").asInt()).isEqualTo(4);
            assertThat(report.path("allUndeletedSummary").path("registeredProfileCount").asInt()).isEqualTo(198);
            assertThat(report.at("/targets/3/bindingStatus").asText()).isEqualTo("LIST_PARSER_MISMATCH");
            assertThat(report.at("/targets/3/enabled").asBoolean()).isFalse();
            assertThat(report.at("/targets/4/bindingStatus").asText()).isEqualTo("PROFILE_MISSING");
            assertThat(report.at("/targets/1/bindingStatus").asText()).isEqualTo("PROFILE_MISSING");
            assertThat(report.at("/targets/0/enabled").isNull()).isTrue();
            assertThat(report.at("/targets/0/includedInPolicyScope").asBoolean()).isTrue();
            assertThat(report.path("isCollectionVerified").asBoolean()).isFalse();
            assertThat(report.toString()).doesNotContain("sourceId","sourceUrl","noticeUrl","https:");
        }
    }
    @Test void duplicateTruncatedAndCoercedReceiptsCannotProduceCoverage() throws Exception {
        var input=selectFixture();input.withArray("targets").add(input.at("/targets/0").deepCopy());
        assertThatThrownBy(()->selectTargets(input,false)).isInstanceOf(IllegalArgumentException.class);
        var count=selectFixture().put("enabledCount",3);assertThatThrownBy(()->selectTargets(count,false)).hasMessage("INVENTORY_COUNT_MISMATCH");
        var truncated=selectFixture();truncated.withArray("targets").remove(1);assertThatThrownBy(()->selectTargets(truncated,false)).isInstanceOf(IllegalArgumentException.class);
        var stringBoolean=selectFixture();((com.fasterxml.jackson.databind.node.ArrayNode)stringBoolean.at("/targets/0")).set(2,JSON.getNodeFactory().textNode("true"));
        assertThatThrownBy(()->selectTargets(stringBoolean,false)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT",matches="true")
    void exportCurrentReceiptAgainstActualRegistration() throws Exception {
        String receiptName=System.getenv("SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT");
        assertThat(receiptName).isNotBlank();
        Path root=Path.of("build/qa-results").toRealPath(),receipt=Path.of(receiptName).toRealPath();
        assertThat(receipt.startsWith(root)).as("수집한 비식별 QA 영수증만 읽는다").isTrue();
        assertThat(Files.size(receipt)).isLessThan(65536);
        var lines=Files.readAllLines(receipt);assertThat(lines).contains("SSM_STATUS=Success","OWNED_CA_BUNDLE_REMOVED");
        var packets=new ArrayList<JsonNode>();for(String line:lines)if(line.startsWith("{"))packets.add(JSON.readTree(line));
        var inputs=packets.stream().filter(p->"LOCAL_TARGET_INVENTORY".equals(p.path("kind").asText())).toList();assertThat(inputs).hasSize(1);
        assertThat(packets.stream().anyMatch(p->"TARGET_INVENTORY_TRANSACTION".equals(p.path("kind").asText())
                &&"ROLLED_BACK".equals(p.path("transaction").asText())&&p.path("writes").isIntegralNumber()&&p.path("writes").asInt()==0)).isTrue();
        try(var context=selectContext();var stream=getClass().getResourceAsStream("/announcement-attachment/provider-qa-catalog-v2.json")) {
            assertThat(stream).isNotNull();var catalog=JSON.readTree(stream);
            var report=selectReport(inputs.getFirst(),catalog,List.copyOf(context.getBeansOfType(AttachmentDiscoveryProfile.class).values()));
            report.put("receiptSha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(receipt))));
            report.put("catalogVersion",catalog.path("catalogVersion").asText());
            Path output=Path.of("build/reports/attachment-target-inventory/operating-targets-vs-local-code.json");Files.createDirectories(output.getParent());
            JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),report);
        }
    }
}
