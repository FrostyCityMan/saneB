package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 기존 전체4첨부 실패 영수증의 네 번째 파일만 진단한다. 본문/전체 분석/정책 QA가 아니다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_GANGBUK_SELECTED_DOWNLOAD",matches="true")
public class GangbukSelectedDownloadDiagnosticTest {
    static final String MODE="GANGBUK_SELECTED_DOWNLOAD";
    static final String REPORT="GANGBUK-179490-selected-download";
    static final long MAX_BYTES=24L*1024*1024;
    static final String PRIOR_RECEIPT="bbe9144fb7462bb094104a093e4a0b94b60d07cd77a45cc7e661629354721a7e";

    static AttachmentDiscoveryProfile.Descriptor selectFixedFile(AttachmentDiscoveryProfile.Result result) throws Exception {
        assertTrue(result.complete()&&"FOUND".equals(result.status()),"DISCOVERY_INCOMPLETE");
        var locators=new ArrayList<String>();
        for(var descriptor:result.descriptors())locators.add(AnnouncementAttachmentOfficialObservationTest.selectHash(descriptor.locator()));
        assertEquals(AnnouncementAttachmentBbsOfficialObservationTest.GANGBUK_LOCATORS,locators,"OFFICIAL_FILE_LIST_CHANGED");
        assertEquals(List.of("HWPX","HWP","HWPX","HWPX"),result.descriptors().stream()
                .map(AttachmentDiscoveryProfile.Descriptor::expectedFormat).toList(),"OFFICIAL_FILE_FORMAT_CHANGED");
        var selected=result.descriptors().get(3);assertTrue(selected.downloadAllowed(),"SELECTED_DOWNLOAD_NOT_ALLOWED");return selected;
    }

    @Test @Timeout(420) void observesOnlyPreviouslyFailedFourthAttachment() throws Exception {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("GANGBUK").findFirst().orElseThrow();
        var profile=sample.profile();var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(profile,5,MAX_BYTES);
        var json=new ObjectMapper();var report=new LinkedHashMap<String,Object>();var rows=new ArrayList<Map<String,Object>>();
        report.put("scope","SELECTED_ATTACHMENT_DOWNLOAD_DIAGNOSTIC_V1");report.put("caseCode",sample.code());
        report.put("observedAt",Instant.now().toString());report.put("priorReceiptSha256",PRIOR_RECEIPT);
        report.put("profileCode",profile.selectProfileCode());report.put("profileHash",profile.selectProfileHash());
        report.put("expectedListedFileCount",4);report.put("selectedFileOrdinal",4);report.put("bodyRequests",0);
        report.put("isWholeTextAnalysisComplete",false);report.put("isPolicyQaPassed",false);report.put("isExpectationApproved",false);
        report.put("productionWriteCount",0);report.put("status","INCOMPLETE");report.put("files",rows);
        String stage="RUNTIME";Path temporary=null;
        try(var client=new AttachmentPinnedDownloadClient()) {
            assertEquals("Linux",System.getProperty("os.name"),"LINUX_ISOLATION_REQUIRED");
            assertTrue(Files.isExecutable(Path.of("/usr/bin/bwrap"))&&Files.isExecutable(Path.of("/usr/bin/prlimit")),"LINUX_ISOLATION_REQUIRED");
            temporary=Files.createTempDirectory("gangbuk-selected-");Path detail=temporary.resolve("detail.html"),binary=temporary.resolve("selected.bin");
            stage="DETAIL_DISCOVERY";var uri=profile.selectDetailUri(sample.source());
            var detailRequest=AttachmentPinnedDownloadClient.Request.selectGet(uri);
            var detailDownload=client.selectDownload(detailRequest,profile.selectApprovedHosts(),
                    request->budget.selectRequestAllowed(detailRequest,request),detail,1024*1024,budget::saveBytes);
            assertNotNull(detailDownload.contentType(),"DETAIL_CONTENT_TYPE_CHANGED");
            assertTrue(Set.of("text/html","application/xhtml+xml").contains(detailDownload.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT)),"DETAIL_CONTENT_TYPE_CHANGED");
            AttachmentDiscoveryProfile.Result found;
            try(var input=Files.newInputStream(detail)) {
                var page=Jsoup.parse(input,null,uri.toASCIIString());
                AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
                found=profile.selectDescriptors(sample.source(),page.outerHtml());
            } finally {Files.deleteIfExists(detail);}
            var selected=selectFixedFile(found);
            report.put("discoveredFileCount",found.descriptors().size());report.put("discoveryComplete",true);
            for(int i=0;i<4;i++) {
                var row=new LinkedHashMap<String,Object>();row.put("locatorHash",AnnouncementAttachmentBbsOfficialObservationTest.GANGBUK_LOCATORS.get(i));
                row.put("formatHint",found.descriptors().get(i).expectedFormat());row.put("status",i==3?"NOT_RUN":"NOT_SELECTED");rows.add(row);
            }
            var row=rows.get(3);var trace=new ObservationDownloadTrace();
            try {
                stage="FILE_DOWNLOAD";
                var downloaded=AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,selected.selectRequest(),binary,budget,client,trace);
                row.put("bytes",downloaded.bytes());row.put("binaryHash",downloaded.sha256());stage="FILE_SIGNATURE";
                assertEquals("HWPX",new AttachmentFileTypeValidator().selectFormat(binary,downloaded,selected.expectedFormat(),profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes()));
                stage="ISOLATED_EXTRACTION";var extracted=new IsolatedAttachmentExtractor(json,System.getProperty("saneb.attachment-observation.extractor")).selectExtraction(binary);
                assertEquals("HWPX",extracted.path("format").asText());
                row.put("format","HWPX");row.put("extractorVersion",extracted.path("extractorVersion").asText());row.put("quality",extracted.path("qualityCode").asText());
                row.putAll(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(extracted));row.put("status","OBSERVED");
                report.put("status","SELECTED_FILE_OBSERVED_NOT_WHOLE_NOTICE");
            } catch(Exception|AssertionError failure) {
                row.put("status","FAILED");row.put("failedStage",stage);row.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));throw failure;
            } finally {row.put("downloadTrace",trace.selectSnapshot());Files.deleteIfExists(binary);}
        } catch(Exception|AssertionError failure) {
            report.put("failedStage",stage);report.put("failureCode",AnnouncementAttachmentOfficialObservationTest.selectFailureCode(failure));
            throw new AssertionError("GANGBUK_SELECTED_DOWNLOAD_INCOMPLETE");
        } finally {
            if(temporary!=null)try(var paths=Files.walk(temporary)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
            report.put("originalFilesRemoved",temporary==null||!Files.exists(temporary));report.put("maximumRequestReservations",5);report.put("maximumReservedBytes",MAX_BYTES);
            report.put("requestReservationsIncludingBodyUpperBound",budget.requests);report.put("reservedBytesIncludingBodyUpperBound",budget.bytes);
            var output=Path.of(System.getProperty("saneb.attachment-observation.report")).toAbsolutePath().normalize();Files.createDirectories(output);
            json.writeValue(output.resolve(REPORT+".json").toFile(),report);
        }
    }
}
