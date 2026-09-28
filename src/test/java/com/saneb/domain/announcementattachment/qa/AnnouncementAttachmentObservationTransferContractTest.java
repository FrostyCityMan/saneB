package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 실제 관측 진입점과 실제 전달 프로필을 검증한다. HTTP와 binary는 합성이며 외부 성공 증거가 아니다. */
class AnnouncementAttachmentObservationTransferContractTest {
    @Test void flowFailuresKeepOnlyKnownCodesWithoutCopyingExternalMessages() {
        for(String code:List.of("ATTACHMENT_DOWNLOAD_BLOCKED","ATTACHMENT_OUTPUT_EXISTS"))
            assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new IOException(code))).isEqualTo(code);
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new IOException("external PRIVATE_CANARY"))).isEqualTo("TRANSPORT_FAILED");
    }
    @TempDir Path directory;
    private final AttachmentDiscoveryProfile profile=new LegalBoardAttachmentProfileConfiguration().selectGangbukLegalProfileDetails();
    private final AttachmentPinnedDownloadClient.Request initial=AttachmentPinnedDownloadClient.Request.selectGet(URI.create(
            "https://eminwon.gangbuk.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=notice.pdf&sys_file_nm=notice_ofr_ofr_fixture_202609120001_1.pdf&file_path=/ntishome/file/upload/ofr/ofr/20260912"));
    private Path output(){return directory.resolve("attachment.bin");}
    private String bridge() {
        var fields=new LinkedHashMap<String,String>();
        fields.put("file_id","ofr_ofr_fixture_202609120001");fields.put("file_path","/ntisho"+"A".repeat(64));
        fields.put("sys_file_nm","합성"+"B".repeat(64));fields.put("user_file_nm","합성"+"C".repeat(32));
        fields.put("pbs_end_ymd","");fields.put("method","selectOfrNotAncmtPbs");fields.put("methodnm","selectOfrNotAncmtPbs");
        fields.put("jndinm","OfrNotAncmtEJB");fields.put("context","NTIS");fields.put("isHome","");
        return "<form id='form' name='form' method='post' action='FDSendNewPbs.jsp'>"
                +fields.entrySet().stream().map(e->"<input type='hidden' name='"+e.getKey()+"' value='"+e.getValue()+"'>")
                .collect(java.util.stream.Collectors.joining())+"</form>";
    }
    private AttachmentPinnedDownloadClient client(String period,List<AttachmentPinnedDownloadClient.Request> sent,
            int firstRequestChecks,int failStage) throws IOException {
        var client=mock(AttachmentPinnedDownloadClient.class);var steps=new AtomicInteger();
        doAnswer(call->{
            var request=call.getArgument(0,AttachmentPinnedDownloadClient.Request.class);
            Predicate<AttachmentPinnedDownloadClient.Request> approved=call.getArgument(2);
            int stage=steps.incrementAndGet();
            for(int i=0;i<(stage==1?firstRequestChecks:1);i++)
                if(!approved.test(request))throw new IOException("ATTACHMENT_PATH_NOT_APPROVED");
            sent.add(request);
            long limit=call.getArgument(4);assertThat(limit).isEqualTo(stage==1?32768L:stage==2?256L:20L*1024*1024);
            AttachmentPinnedDownloadClient.ByteReservation bytes=call.getArgument(5);
            if(!bytes.reserve(4096))throw new IOException("SOURCE_BYTE_LIMIT");
            Path path=call.getArgument(3);assertThat(path).isEqualTo(output());assertThat(Files.exists(path)).isFalse();
            Files.writeString(path,stage==1?bridge():stage==2?period:"%PDF-1.7",StandardOpenOption.CREATE_NEW);
            if(stage==Math.abs(failStage))throw new IOException(failStage<0?"ATTACHMENT_HTTP_400":"ATTACHMENT_TIMEOUT");
            return new AttachmentPinnedDownloadClient.Download(Files.size(path),"a".repeat(64),stage==3?"application/pdf":"text/html");
        }).when(client).selectDownload(any(AttachmentPinnedDownloadClient.Request.class),anySet(),any(),any(),anyLong(),any());
        return client;
    }
    private AnnouncementAttachmentBbsOfficialObservationTest.Budget budget(){
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(profile);budget.reserveBody();return budget;
    }
    @Test void actualObservationUsesThreeStagesAndChargesEveryStepToOneBudget() throws Exception {
        var budget=budget();var sent=new ArrayList<AttachmentPinnedDownloadClient.Request>();
        var trace=new ObservationDownloadTrace();
        var result=AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client("20991231",sent,1,0),trace);
        assertThat(trace.selectSnapshot()).isEqualTo(new ObservationDownloadTrace.Snapshot(1,
                ObservationDownloadTrace.Step.FINAL_POST,ObservationDownloadTrace.Phase.COMPLETE,3,3));
        assertThat(result.contentType()).isEqualTo("application/pdf");assertThat(Files.readString(output())).isEqualTo("%PDF-1.7");
        assertThat(sent).extracting(AttachmentPinnedDownloadClient.Request::method).containsExactly("GET","POST","POST");
        assertThat(sent.getLast().form()).containsEntry("isHome","").containsEntry("pbs_end_ymd","20991231");
        assertThat(budget.requests).isEqualTo(5);assertThat(budget.bytes).isEqualTo(2L*1024*1024+3*4096);
        Files.delete(output());
    }
    @Test void http400IsLocatedAtEachActualTransportWithoutLeakingRequestOrResponse() throws Exception {
        var steps=List.of(ObservationDownloadTrace.Step.BRIDGE_GET,ObservationDownloadTrace.Step.PERIOD_POST,ObservationDownloadTrace.Step.FINAL_POST);
        for(int stage=1;stage<=3;stage++) {
            var trace=new ObservationDownloadTrace();var sent=new ArrayList<AttachmentPinnedDownloadClient.Request>();
            var client=client("20991231",sent,1,-stage);var budget=budget();
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client,trace))
                    .hasMessage("ATTACHMENT_HTTP_400");
            assertThat(trace.selectSnapshot()).isEqualTo(new ObservationDownloadTrace.Snapshot(1,steps.get(stage-1),
                    ObservationDownloadTrace.Phase.TRANSPORT,stage,stage-1));
            assertThat(budget.requests).isEqualTo(2+stage);assertThat(Files.exists(output())).isFalse();
            var json=new com.fasterxml.jackson.databind.ObjectMapper();var row=json.createObjectNode();
            row.putArray("files").addObject().set("downloadTrace",json.valueToTree(trace.selectSnapshot()));
            var transported=AnnouncementAttachmentBbsObservationProbe.selectTransportReport(row).at("/files/0/downloadTrace");
            assertThat(transported).isEqualTo(json.valueToTree(trace.selectSnapshot()));
            assertThat(transported.size()).isEqualTo(5);
            assertThat(transported.toString()).doesNotContain("https", "notice", "file_path", "form", "20991231");
        }
    }
    @Test void expiredPeriodIsProfileProcessingNotTransportFailure() throws Exception {
        var trace=new ObservationDownloadTrace();var client=client("20000101",new ArrayList<>(),1,0);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget(),client,trace))
                .hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");
        assertThat(trace.selectSnapshot()).isEqualTo(new ObservationDownloadTrace.Snapshot(1,
                ObservationDownloadTrace.Step.PERIOD_POST,ObservationDownloadTrace.Phase.PROFILE_PROCESSING,2,2));
        assertThat(Files.exists(output())).isFalse();
    }
    @Test void rejectedInitialRequestDoesNotInventATransportAttempt() {
        var trace=new ObservationDownloadTrace();var client=mock(AttachmentPinnedDownloadClient.class);
        var unrelated=AttachmentPinnedDownloadClient.Request.selectGet(URI.create("https://example.com/private-canary"));
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,unrelated,output(),budget(),client,trace))
                .hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");
        assertThat(trace.selectSnapshot()).isEqualTo(new ObservationDownloadTrace.Snapshot(1,
                ObservationDownloadTrace.Step.NOT_STARTED,ObservationDownloadTrace.Phase.BEFORE_TRANSPORT,0,0));
        verifyNoInteractions(client);
    }
    @Test void redirectsRemainHttpReservationsRatherThanInventedFlowSteps() throws Exception {
        var trace=new ObservationDownloadTrace();var budget=budget();var client=client("20991231",new ArrayList<>(),3,0);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client,trace))
                .hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
        assertThat(trace.selectSnapshot()).isEqualTo(new ObservationDownloadTrace.Snapshot(1,
                ObservationDownloadTrace.Step.FINAL_POST,ObservationDownloadTrace.Phase.TRANSPORT,3,2));
        assertThat(budget.requests).isEqualTo(6);assertThat(Files.exists(output())).isFalse();
    }
    @Test void expiredPeriodNeverReachesFinalFileAndIsNotTreatedAsNoAttachments() throws Exception {
        var sent=new ArrayList<AttachmentPinnedDownloadClient.Request>();var budget=budget();var client=client("20000101",sent,1,0);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client))
                .hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");
        assertThat(sent).hasSize(2);assertThat(budget.requests).isEqualTo(4);assertThat(Files.exists(output())).isFalse();
    }
    @Test void redirectsAndStagesCannotExceedFourHttpRequestsPerFile() throws Exception {
        var sent=new ArrayList<AttachmentPinnedDownloadClient.Request>();var budget=budget();var client=client("20991231",sent,3,0);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client))
                .hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
        assertThat(sent).hasSize(2);assertThat(budget.requests).isEqualTo(6);assertThat(Files.exists(output())).isFalse();
    }
    @Test void exhaustedWholeCaseRequestBudgetStopsBeforeNextStage() throws Exception {
        var sent=new ArrayList<AttachmentPinnedDownloadClient.Request>();var budget=budget();budget.requests=budget.maximumRequests-1;
        var client=client("20991231",sent,1,0);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client))
                .hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
        assertThat(sent).hasSize(1);assertThat(budget.requests).isEqualTo(budget.maximumRequests);assertThat(Files.exists(output())).isFalse();
    }
    @Test void byteBudgetIsSharedAndDoesNotResetBetweenBridgeAndPeriod() throws Exception {
        var sent=new ArrayList<AttachmentPinnedDownloadClient.Request>();var budget=budget();budget.bytes=budget.maximumBytes-4096;
        var client=client("20991231",sent,1,0);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client))
                .hasMessage("SOURCE_BYTE_LIMIT");
        assertThat(sent).hasSize(2);assertThat(budget.bytes).isEqualTo(budget.maximumBytes);assertThat(Files.exists(output())).isFalse();
    }
    @Test void partialFinalDownloadIsRemovedAndFailureIsPreserved() throws Exception {
        var sent=new ArrayList<AttachmentPinnedDownloadClient.Request>();var budget=budget();var client=client("20991231",sent,1,3);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,initial,output(),budget,client))
                .hasMessage("ATTACHMENT_TIMEOUT");
        assertThat(sent).hasSize(3);assertThat(Files.exists(output())).isFalse();
    }
    @Test void unrelatedRequestIsRejectedBeforeTransport() {
        var client=mock(AttachmentPinnedDownloadClient.class);var budget=budget();
        var unrelated=AttachmentPinnedDownloadClient.Request.selectGet(URI.create("https://example.com/file.pdf"));
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(profile,unrelated,output(),budget,client))
                .hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");
        verifyNoInteractions(client);assertThat(budget.requests).isEqualTo(2);
    }
    @Test void existingDirectGetProfileStillUsesSingleBoundedRequest() throws Exception {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("TAEBAEK").findFirst().orElseThrow();
        var direct=sample.profile();var request=AttachmentPinnedDownloadClient.Request.selectGet(direct.selectDetailUri(sample.source()));
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(direct);budget.reserveBody();
        var client=mock(AttachmentPinnedDownloadClient.class);
        doAnswer(call->{
            Predicate<AttachmentPinnedDownloadClient.Request> approved=call.getArgument(2);assertThat(approved.test(request)).isTrue();
            AttachmentPinnedDownloadClient.ByteReservation bytes=call.getArgument(5);assertThat(bytes.reserve(4096)).isTrue();
            assertThat((Long)call.getArgument(4)).isEqualTo(20L*1024*1024);
            Files.writeString(output(),"fixture",StandardOpenOption.CREATE_NEW);
            return new AttachmentPinnedDownloadClient.Download(7,"a".repeat(64),"application/octet-stream");
        }).when(client).selectDownload(eq(request),eq(direct.selectApprovedHosts()),any(),eq(output()),anyLong(),any());
        var trace=new ObservationDownloadTrace();
        AnnouncementAttachmentBbsOfficialObservationTest.selectFileDownload(direct,request,output(),budget,client,trace);
        assertThat(trace.selectSnapshot()).isEqualTo(new ObservationDownloadTrace.Snapshot(1,
                ObservationDownloadTrace.Step.DIRECT,ObservationDownloadTrace.Phase.COMPLETE,1,1));
        verify(client,times(1)).selectDownload(eq(request),anySet(),any(),any(),anyLong(),any());
        assertThat(budget.requests).isEqualTo(3);Files.delete(output());
    }
}
