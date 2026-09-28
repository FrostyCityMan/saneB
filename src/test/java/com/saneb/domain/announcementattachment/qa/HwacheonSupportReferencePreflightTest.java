package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** 고정 지원사업 한 건의 공개 상세·POST 파일 signature만 확인한다. 추출·본문 판정·운영 쓰기는 없다. */
class HwacheonSupportReferencePreflightTest {
    static final String ID="32258";
    static final String TITLE="2026년 화천군 중소기업 및 소상공인 육성자금 융자추천 및 이차보전 지원계획 공고";
    static final String URL="https://eminwon.ihc.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
            +"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
            +"&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no="+ID+"&subCheck=N";
    static final long MAXIMUM_BYTES=21L*1024*1024;
    @TempDir Path directory;

    static AttachmentDiscoveryProfile.Source selectSource() {
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(URL)),URL,
                "LGS-000130","SAFE_SAEOL_EMINWON_LEGACY");
    }
    static void validateTitle(Document page) {
        var forms=page.select("form[name=form1][method=post]");assertEquals(1,forms.size(),"TITLE_STRUCTURE_CHANGED");
        var tables=forms.getFirst().select("table[width=100%][border=0][cellspacing=1][cellpadding=0]");
        assertEquals(1,tables.size(),"TITLE_STRUCTURE_CHANGED");var table=tables.getFirst();
        var labels=table.select("th").stream().filter(e->e.closest("table")==table&&e.select("table").isEmpty()&&"제목".equals(e.text().strip())).toList();
        assertEquals(1,labels.size(),"TITLE_STRUCTURE_CHANGED");var cell=labels.getFirst().nextElementSibling();
        assertTrue(cell!=null&&"td".equals(cell.tagName())&&cell.select("table,script,input").isEmpty(),"TITLE_STRUCTURE_CHANGED");
        assertEquals(normalized(TITLE),normalized(cell.text()),"TITLE_CHANGED");
    }
    private static String normalized(String value){return Normalizer.normalize(value,Normalizer.Form.NFKC).replaceAll("\\s+"," ").strip();}
    private static String fixture(){return "<form name='form1' method='post'><table width='100%' border='0' cellspacing='1' cellpadding='0'><tr><th>제목</th><td>"+TITLE+"</td></tr></table></form>";}
    @Test void titleIsBoundToUniqueOfficialTableWithoutSurroundingTextFallback() {
        validateTitle(Jsoup.parse(fixture()));validateTitle(Jsoup.parse(fixture().replace("화천군 ","화천군  ")));
        for(String invalid:List.of(fixture()+fixture(),fixture().replace(TITLE,"다른 제목"),fixture().replace("method='post'","method='get'"),
                fixture().replace("<th>제목</th>","<td>제목</td>"),fixture().replace("</td>","<table><tr><td>메뉴</td></tr></table></td>"),
                fixture().replace("</tr>","<th>제목</th><td>"+TITLE+"</td></tr>")))assertThrows(AssertionError.class,()->validateTitle(Jsoup.parse(invalid)));
    }
    @Test void fixedSourceCannotExpandHostOrQuery() {
        var profile=new HwacheonPostAttachmentDiscoveryProfile();var source=selectSource();
        assertEquals(URL,profile.selectDetailUri(source).toString());
        assertFalse(profile.selectApprovedRequest(java.net.URI.create(URL+"&extra=1")));
        assertFalse(profile.selectApprovedRequest(java.net.URI.create(URL.replace("eminwon.ihc.go.kr","other.example"))));
        assertFalse(profile.selectApprovedRequest(java.net.URI.create(URL.replace("subCheck=N","subCheck=Y"))));
    }
    static final class Budget {
        long requests,bytes;
        boolean reserveRequest(){if(requests>=2||Thread.currentThread().isInterrupted())return false;requests++;return true;}
        boolean reserveBytes(long count){if(count<0||bytes>MAXIMUM_BYTES-count||Thread.currentThread().isInterrupted())return false;bytes+=count;return true;}
    }
    @Test void singleNoticeBudgetRejectsExtraRequestsAndByteOverflow() {
        var budget=new Budget();assertTrue(budget.reserveRequest());assertTrue(budget.reserveRequest());assertFalse(budget.reserveRequest());
        assertFalse(budget.reserveBytes(-1));assertTrue(budget.reserveBytes(MAXIMUM_BYTES));assertFalse(budget.reserveBytes(1));assertFalse(budget.reserveBytes(Long.MAX_VALUE));
        assertEquals(2,budget.requests);assertEquals(MAXIMUM_BYTES,budget.bytes);
    }

    @Test @Timeout(100) @EnabledIfEnvironmentVariable(named="SANEB_HWACHEON_SUPPORT_PREFLIGHT",matches="true")
    void observesFixedTitleAndEntireOneHwpWithoutExtractionOrOperatingChanges() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var title=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                "LOCAL_GOV_NOTICE",TITLE,null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
        assertTrue(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title),"TITLE_NOT_ELIGIBLE_NOT_FETCHED");
        var profile=new HwacheonPostAttachmentDiscoveryProfile();var source=selectSource();
        Path root=Path.of("build/qa-results");Files.createDirectories(root);
        var json=new ObjectMapper();
        Files.writeString(root.resolve("hwacheon-support-32258-reservation.json"),json.writeValueAsString(Map.of(
                "caseCode","HWACHEON-"+ID,"maximumRequests",2,"maximumBytes",MAXIMUM_BYTES,"maximumNetworkSeconds",60,
                "startedAt",Instant.now().toString())),StandardOpenOption.CREATE_NEW);
        var report=new LinkedHashMap<String,Object>();var files=new ArrayList<Map<String,Object>>();var budget=new Budget();
        report.put("caseCode","HWACHEON-"+ID);report.put("profileCode",profile.selectProfileCode());report.put("profileHash",profile.selectProfileHash());
        report.put("scope","FIXED_SUPPORT_DISCOVERY_DOWNLOAD_SIGNATURE_ONLY");report.put("titleStage",title.titleStageCode());
        report.put("rulesSource","EPHEMERAL_DB_DRAFT_SEED");report.put("rulesHash",AnnouncementAttachmentOfficialObservationTest.selectHash(rules));
        report.put("status","FAILED");report.put("productionWriteCount",0);report.put("isExpectationApproved",false);
        report.put("isPolicyQaPassed",false);report.put("bodyClassificationExecuted",false);report.put("extractionExecuted",false);
        Path detail=directory.resolve("detail.html"),binary=directory.resolve("attachment.bin");String stage="DETAIL_FETCH";
        try(var client=new AttachmentPinnedDownloadClient()) {
            var initial=AttachmentPinnedDownloadClient.Request.selectGet(profile.selectDetailUri(source));var detailOnce=new AtomicBoolean();
            var downloaded=client.selectDownload(initial,profile.selectApprovedHosts(),
                    request->initial.equals(request)&&profile.selectApprovedRequest(request)&&detailOnce.compareAndSet(false,true)&&budget.reserveRequest(),
                    detail,1024L*1024,budget::reserveBytes);
            assertTrue(downloaded.contentType()!=null&&downloaded.contentType().toLowerCase(Locale.ROOT).startsWith("text/html"),"DETAIL_CONTENT_TYPE_CHANGED");
            report.put("detailHash",downloaded.sha256());stage="TITLE_IDENTITY";Document page;
            try(var input=Files.newInputStream(detail)){page=Jsoup.parse(input,null,URL);}
            validateTitle(page);report.put("fixedTitleVerified",true);stage="ATTACHMENT_DISCOVERY";
            var discovered=profile.selectDescriptors(source,page.outerHtml());Files.delete(detail);
            report.put("discoveryStatus",discovered.status());report.put("discoveryComplete",discovered.complete());
            report.put("discoveredCount",discovered.descriptors().size());report.put("warnings",discovered.warnings());
            assertTrue(discovered.complete()&&"FOUND".equals(discovered.status()),"DISCOVERY_INCOMPLETE");
            assertEquals(1,discovered.descriptors().size(),"FILE_LIST_CHANGED");var descriptor=discovered.descriptors().getFirst();
            assertTrue(descriptor.downloadAllowed(),"FORMAT_NOT_SUPPORTED");assertEquals("HWP",descriptor.expectedFormat(),"FILE_FORMAT_HINT_CHANGED");
            assertEquals("UNKNOWN",descriptor.documentRole(),"AUTOMATIC_ROLE_FORBIDDEN");stage="DOWNLOAD_SIGNATURE";
            var fileRequest=descriptor.selectRequest();var fileOnce=new AtomicBoolean();assertEquals("POST",fileRequest.method());
            var file=client.selectDownload(fileRequest,profile.selectApprovedHosts(),
                    request->fileRequest.equals(request)&&profile.selectApprovedRequest(request)&&fileOnce.compareAndSet(false,true)&&budget.reserveRequest(),
                    binary,20L*1024*1024,budget::reserveBytes);
            assertEquals("HWP",new AttachmentFileTypeValidator().selectFormat(binary,file,"HWP"),"FILE_SIGNATURE_CHANGED");
            files.add(Map.of("format","HWP","binaryHash",file.sha256(),"bytes",file.bytes(),
                    "locatorHash",AnnouncementAttachmentOfficialObservationTest.selectHash(descriptor.locator()),"roleCode","UNKNOWN"));
            report.put("status","DISCOVERY_DOWNLOAD_SIGNATURE_PASSED");
        } catch(Exception|AssertionError failure) {
            report.put("failedStage",stage);report.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));
            throw new AssertionError("HWACHEON_SUPPORT_PREFLIGHT_FAILED:"+stage);
        } finally {
            Files.deleteIfExists(detail);Files.deleteIfExists(binary);
            report.put("temporaryOriginalRemoved",!Files.exists(detail)&&!Files.exists(binary));report.put("requestReservations",budget.requests);
            report.put("reservedBytes",budget.bytes);report.put("maximumRequests",2);report.put("maximumBytes",MAXIMUM_BYTES);
            report.put("files",files);report.put("finishedAt",Instant.now().toString());
            Files.writeString(root.resolve("hwacheon-support-32258-result.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(report),StandardOpenOption.CREATE_NEW);
        }
    }
}
