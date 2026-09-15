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

    public record ObservationCase(String code,String title,AttachmentDiscoveryProfile.Source source,
                           AttachmentDiscoveryProfile profile,String listUrl,int listedFileCount,boolean compactTitle) {
        @Override public String toString(){return code;}
    }
    static Stream<ObservationCase> selectConfiguredCases() {
        return selectCases(System.getProperty("saneb.attachment-observation.group","TAEBAEK"));
    }
    public static Stream<ObservationCase> selectCases(String group) {
        if("TAEBAEK".equals(group)) return Stream.of(new ObservationCase(CASE,TITLE,SOURCE,PROFILE,
                "https://www.taebaek.go.kr/www/selectBbsNttList.do?bbsNo=25&key=352",2,false));
        if(!"YANGPYEONG".equals(group)) throw new IllegalArgumentException("UNKNOWN_OBSERVATION_GROUP");
        return Stream.of(
                selectYangpyeongCase("312241","9b6353577acabd579068a389244e774d40b5488eea86d1e15fb78e0eae9ccb9a",
                        "2026년 중장년 취업지원 프로그램 '산모·신생아 건강관리사' 교육생 모집공고",1),
                selectYangpyeongCase("311846","0fcd9b0bf381aebb5c18b80c5e5c1fd7355f9ce0d0ab130b64761de60cf56f96",
                        "2026년 중소기업 제품디자인개발 지원사업 참여기업 모집 공고",2),
                selectYangpyeongCase("311507","7d571058a83135abdb528156f40ef0e1c396dcd6f2a4731255ed960c5a7b65c5",
                        "『2026년 귀농인 정착지원 주택임대 사업』 대상자(빈집 소유자) 모집 3차 공고",2));
    }
    private static ObservationCase selectYangpyeongCase(String id,String identity,String title,int count) {
        return new ObservationCase("YANGPYEONG-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",identity,
                "https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo="+id,"LGS-000110","HEURISTIC_NOTICE"),
                new StandardBbsAttachmentProfileConfiguration().selectYangpyeongProfileDetails(),
                "https://www.yp21.go.kr/www/selectBbsNttList.do?bbsNo=5&key=1119",count,true);
    }

    @ParameterizedTest(name="{0}") @MethodSource("selectConfiguredCases") @Timeout(420)
    void observesTitleBodyAndWholeAttachmentSetWithoutPublication(ObservationCase sample) throws Exception {
        var profile=sample.profile();var source=sample.source();
        var report=new LinkedHashMap<String,Object>();
        report.put("scope","OFFICIAL_THREE_STAGE_OBSERVATION_V1");report.put("caseCode",sample.code());report.put("observedAt",Instant.now().toString());
        report.put("titleInputSource","FIXED_OFFICIAL_SAMPLE");
        report.put("profileCode",profile.selectProfileCode());report.put("profileHash",profile.selectProfileHash());
        report.put("isPolicyQaPassed",false);report.put("isExpectationApproved",false);report.put("productionWriteCount",0);
        report.put("status","INCOMPLETE");report.put("expectedListedFileCount",sample.listedFileCount());
        report.put("isWholeTextAnalysisComplete",false);
        var rows=new ArrayList<Map<String,Object>>();report.put("files",rows);
        var budget=new Budget(profile);Path temporary=null;String stage="RUNTIME";
        try(var client=new AttachmentPinnedDownloadClient()) {
            assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")
                    &&Files.isExecutable(Path.of("/usr/bin/bwrap"))&&Files.isExecutable(Path.of("/usr/bin/prlimit")),"LINUX_ISOLATION_REQUIRED");
            var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
            report.put("rulesSource","EPHEMERAL_DB_DRAFT_SEED");report.put("rulesHash",AnnouncementAttachmentOfficialObservationTest.selectHash(rules));
            var engine=new AnnouncementSourceClassificationEngine();stage="TITLE_GATE";
            var title=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            report.put("titleStage",title.titleStageCode());
            report.put("titleReason",title.reasonCode());
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
            var download=client.selectDownload(AttachmentPinnedDownloadClient.Request.selectGet(uri),profile.selectApprovedHosts(),
                    budget::selectRequestAllowed,detail,MIB,budget::saveBytes);
            assertTrue(Set.of("text/html","application/xhtml+xml").contains(download.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT)),"DETAIL_CONTENT_TYPE_CHANGED");
            AttachmentDiscoveryProfile.Result discovered;
            try(var input=Files.newInputStream(detail)) {
                var page=Jsoup.parse(input,null,uri.toASCIIString());stage="TITLE_CONFIRMATION";validateTitle(page,sample.title(),sample.compactTitle());
                stage="DETAIL_DISCOVERY";discovered=profile.selectDescriptors(source,page.outerHtml());
            } finally {Files.deleteIfExists(detail);}
            report.put("discoveryStatus",discovered.status());report.put("discoveryComplete",discovered.complete());report.put("discoveredFileCount",discovered.descriptors().size());
            for(var d:discovered.descriptors()) {var row=new LinkedHashMap<String,Object>();rows.add(row);row.put("locatorHash",AnnouncementAttachmentOfficialObservationTest.selectHash(d.locator()));
                row.put("formatHint",d.expectedFormat());row.put("downloadAllowed",d.downloadAllowed());row.put("status","NOT_RUN");}
            assertTrue(discovered.complete()&&Set.of("FOUND","NO_FILES").contains(discovered.status()),"DISCOVERY_INCOMPLETE");
            var extractor=new IsolatedAttachmentExtractor(JSON,System.getProperty("saneb.attachment-observation.extractor"));var files=new ArrayList<FileInput>();
            for(int i=0;i<discovered.descriptors().size();i++) {
                var descriptor=discovered.descriptors().get(i);var row=rows.get(i);Path binary=temporary.resolve(UUID.randomUUID()+".bin");
                if(!descriptor.downloadAllowed()) {row.put("status","UNSUPPORTED_NOT_DOWNLOADED");files.add(incompleteFile("UNSUPPORTED"));continue;}
                try {
                    stage="FILE_DOWNLOAD";var bytes=client.selectDownload(descriptor.selectRequest(),profile.selectApprovedHosts(),budget::selectRequestAllowed,binary,20*MIB,budget::saveBytes);
                    row.put("bytes",bytes.bytes());row.put("binaryHash",bytes.sha256());stage="FILE_SIGNATURE";
                    String format=new AttachmentFileTypeValidator().selectFormat(binary,bytes,descriptor.expectedFormat(),profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes());
                    row.put("format",format);stage="ISOLATED_EXTRACTION";var actual=extractor.selectExtraction(binary);
                    assertEquals(format,actual.path("format").asText(),"EXTRACTED_FORMAT_CHANGED");
                    row.put("quality",actual.path("qualityCode").asText());
                    assertTrue(Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED").contains(actual.path("qualityCode").asText()),"ISOLATED_EXTRACTION_FAILED");
                    stage="TEXT_ROLE";var observation=AnnouncementAttachmentOfficialObservationTest.selectTextObservation(actual);row.putAll(observation);
                    files.add(selectFileInput(actual,JSON.valueToTree(observation)));row.put("status","OBSERVED");
                } catch(Exception|AssertionError failure) {row.put("status","FAILED");row.put("failedStage",stage);row.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));files.add(incompleteFile("EXTRACTION_FAILED"));}
                finally {Files.deleteIfExists(binary);}
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
            report.put("originalFilesRemoved",temporary==null||!Files.exists(temporary));report.put("maximumRequestReservations",44);report.put("maximumReservedBytes",80*MIB);
            report.put("requestReservationsIncludingBodyUpperBound",budget.requests);report.put("reservedBytesIncludingBodyUpperBound",budget.bytes);
            Path output=Path.of(System.getProperty("saneb.attachment-observation.report")).toAbsolutePath().normalize();Files.createDirectories(output);
            JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve(sample.code()+".json").toFile(),report);
        }
    }
    public static boolean selectTitleMayProceed(AnnouncementSourceClassificationResult result) {return result.semanticStatusCode()!=SemanticStatusCode.EXCLUDED
            &&Set.of(TitleStageCode.GROUP_A_MATCHED,TitleStageCode.COMBINATION_MATCHED).contains(result.titleStageCode());}
    public static boolean selectBodyComplete(ProviderContentResult body) {return body!=null&&body.statusCode()==ProviderContentCodes.StatusCode.AVAILABLE
            &&body.bodyAvailabilityCode()==BodyAvailabilityCode.AVAILABLE&&body.bodySourceCode()==BodySourceCode.DETAIL_PAGE_TEXT
            &&body.bodyText()!=null&&!body.bodyText().isBlank();}
    static void validateBodyComplete(ProviderContentResult body) {assertTrue(selectBodyComplete(body),"BODY_OBSERVATION_INCOMPLETE");}
    static void validateTitle(org.jsoup.nodes.Document page,String expected) {
        validateTitle(page,expected,false);
    }
    public static void validateTitle(org.jsoup.nodes.Document page,String expected,boolean compact) {
        var tables=page.select(compact?"div.p-wrap.bbs.bbs__view > table.p-table.block":"table.bbs_default.view");
        assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
        if(compact) {
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
    static final class Budget {
        private final AttachmentDiscoveryProfile profile;
        Budget(){this(PROFILE);}
        Budget(AttachmentDiscoveryProfile profile){this.profile=Objects.requireNonNull(profile);}
        long requests,bytes;
        void reserveBody(){if(requests!=0||bytes!=0)throw new IllegalStateException("BODY_BUDGET_ALREADY_RESERVED");requests=2;bytes=2*MIB;}
        boolean selectRequestAllowed(AttachmentPinnedDownloadClient.Request r){if(!profile.selectApprovedRequest(r)||requests>=44||Thread.currentThread().isInterrupted())return false;requests++;return true;}
        boolean saveBytes(long count){if(count<0||bytes>80*MIB-count||Thread.currentThread().isInterrupted())return false;bytes+=count;return true;}
    }
}
