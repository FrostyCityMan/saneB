package com.saneb.domain.announcementattachment.discovery;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 실제 사이트의 발견·다운로드·signature 증거. Windows host에서 공개 binary를 추출하지 않는다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_PROFILE_QA",matches="true")
class LegalBoardAttachmentProfileLiveQaTest {
    @TempDir Path directory;
    record Sample(boolean busan,String noticeId,int fileCount,int unsupportedCount) { }
    static Stream<Sample> selectCases() {
        return Stream.of(new Sample(true,"79571",3,0),new Sample(true,"79570",3,0),new Sample(true,"79567",3,0),
                new Sample(false,"184761",3,0),new Sample(false,"184744",2,0),new Sample(false,"184759",1,0),
                // 정상 파일만 고르지 않는다. 네 번째 파일은 비지원 형식이며 다운로드하지 않아야 한다.
                new Sample(false,"184760",4,1));
    }
    @ParameterizedTest(name="법정 게시판 고정 공개 표본 {index}") @MethodSource("selectCases") @Timeout(180)
    void discoversAllFilesAndDownloadsOnlySupportedDescriptorsThroughProductionTransport(Sample sample) throws Exception {
        var profile=LegalBoardAttachmentDiscoveryProfileTest.selectProfile(sample.busan());
        var source=LegalBoardAttachmentDiscoveryProfileTest.selectSource(sample.busan(),sample.noticeId());
        Path detail=directory.resolve("detail.html"),binary=directory.resolve("attachment.bin");
        var report=new LinkedHashMap<String,Object>();var files=new ArrayList<Map<String,Object>>();
        var requests=new AtomicLong();var reserved=new AtomicLong();
        report.put("profileCode",profile.selectProfileCode());report.put("profileHash",profile.selectProfileHash());
        report.put("caseId",(sample.busan()?"BUSAN-":"GANGBUK-")+sample.noticeId());report.put("startedAt",Instant.now().toString());
        report.put("scope","FIXED_PUBLIC_PAGE_DISCOVERY_DOWNLOAD_SIGNATURE_ONLY");report.put("extractionExecuted",false);
        report.put("databaseWrites",0);report.put("operatingActivation",false);report.put("status","FAILED");
        String stage="DETAIL";
        try(var client=new AttachmentPinnedDownloadClient()) {
            AttachmentPinnedDownloadClient.ByteReservation budget=bytes->reserved.addAndGet(bytes)<=80L*1024*1024;
            var downloaded=client.selectDownload(AttachmentPinnedDownloadClient.Request.selectGet(profile.selectDetailUri(source)),
                    profile.selectApprovedHosts(),r->{requests.incrementAndGet();return profile.selectApprovedRequest(r);},detail,1024L*1024,budget);
            assertTrue(downloaded.contentType()!=null && downloaded.contentType().toLowerCase(Locale.ROOT).startsWith("text/html"),"DETAIL_CONTENT_TYPE_CHANGED");
            report.put("detailHash",downloaded.sha256());AttachmentDiscoveryProfile.Result result;
            try(var input=Files.newInputStream(detail)) {
                result=profile.selectDescriptors(source,Jsoup.parse(input,null,profile.selectDetailUri(source).toASCIIString()).outerHtml());
            }
            Files.delete(detail);stage="DISCOVERY";
            report.put("discoveryStatus",result.status());report.put("discoveredCount",result.descriptors().size());report.put("discoveryWarnings",result.warnings());
            assertEquals("FOUND",result.status(),"DISCOVERY_STRUCTURE_CHANGED");assertTrue(result.complete(),"DISCOVERY_INCOMPLETE");
            assertEquals(sample.fileCount(),result.descriptors().size(),"FILE_LIST_CHANGED");
            assertEquals(sample.unsupportedCount(),result.descriptors().stream().filter(d->!d.downloadAllowed()).count(),"FORMAT_SUPPORT_CHANGED");
            stage="DOWNLOAD_SIGNATURE";
            for(var descriptor:result.descriptors()) {
                assertEquals("UNKNOWN",descriptor.documentRole(),"AUTOMATIC_DOCUMENT_ROLE_FORBIDDEN");
                var file=new LinkedHashMap<String,Object>();file.put("attachmentIdHash",descriptor.locator().identifiers().get("attachmentId"));
                if(!descriptor.downloadAllowed()) {
                    file.put("status","UNSUPPORTED_NOT_DOWNLOADED");files.add(file);continue;
                }
                try {
                    var bytes=AttachmentProfileDownloadFlow.selectDownload(profile,descriptor.selectRequest(),binary,20L*1024*1024,
                            (request,limit,approved)->{
                                String path=request.uri().getPath();
                                report.put("lastFileIndex",files.size()+1);
                                report.put("lastTransferStage",path.endsWith("FDSendNewPbs.jsp")?"FINAL_POST":path.endsWith("OfrAction.do")?"PERIOD_POST"
                                        :path.endsWith("FileDown.jsp")?"BRIDGE_GET":"DIRECT_GET");
                                return client.selectDownload(request,profile.selectApprovedHosts(),
                                        r->{requests.incrementAndGet();return approved.test(r);},binary,limit,budget);
                            });
                    String format=new AttachmentFileTypeValidator().selectFormat(binary,bytes,descriptor.expectedFormat(),profile.selectUtf8DispositionOctets());
                    assertTrue(bytes.bytes()>0 && bytes.bytes()<=20L*1024*1024,"DOWNLOAD_BYTE_LIMIT");
                    file.put("status","DOWNLOADED_SIGNATURE_VALID");file.put("binaryHash",bytes.sha256());file.put("bytes",bytes.bytes());file.put("signatureFormat",format);
                    files.add(file);
                } finally { Files.deleteIfExists(binary); }
            }
            report.put("status",sample.unsupportedCount()==0?"DISCOVERY_DOWNLOAD_SIGNATURE_PASSED":"DISCOVERY_WITH_UNSUPPORTED_FILES");
        } catch(Exception|AssertionError failure) {
            String safe=failure.getMessage()!=null && failure.getMessage().matches("[A-Z][A-Z0-9_]{1,79}")?failure.getMessage():failure.getClass().getSimpleName();
            report.put("failureCode",safe);report.put("failureStage",stage);
            throw new AssertionError(profile.selectProfileCode()+": "+stage+"/"+safe);
        } finally {
            Files.deleteIfExists(detail);Files.deleteIfExists(binary);boolean cleaned;
            try(var entries=Files.list(directory)) { cleaned=entries.findAny().isEmpty(); }
            report.put("temporaryCleaned",cleaned);report.put("files",files);report.put("requestCount",requests.get());
            report.put("reservedBytes",reserved.get());report.put("finishedAt",Instant.now().toString());
            Path reports=Path.of(System.getProperty("saneb.attachment-profile-qa.report","build/reports/attachment-profile-discovery-qa"));
            Files.createDirectories(reports);Files.writeString(reports.resolve(profile.selectProfileCode()+"-"+sample.noticeId()+".json"),
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
            assertTrue(cleaned,"TEMPORARY_ORIGINAL_NOT_REMOVED");
        }
    }
}
