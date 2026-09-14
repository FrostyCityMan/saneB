package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentJobService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementattachment.worker.AttachmentDownloadGateway;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegalBoardAttachmentTransferTest {
    @TempDir Path directory;
    private final AttachmentDiscoveryProfile profile=new LegalBoardAttachmentProfileConfiguration().selectGangbukLegalProfileDetails();
    private final AttachmentPinnedDownloadClient.Request initial=AttachmentPinnedDownloadClient.Request.selectGet(URI.create(
            "https://eminwon.gangbuk.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=notice.pdf&sys_file_nm=notice_ofr_ofr_fixture_202609120001_1.pdf&file_path=/ntishome/file/upload/ofr/ofr/20260912"));
    private Path output() { return directory.resolve("attachment.bin"); }
    private Map<String,String> fields() {
        var values=new LinkedHashMap<String,String>();
        values.put("file_id","ofr_ofr_fixture_202609120001");values.put("file_path","/ntisho"+"A".repeat(64));
        values.put("sys_file_nm","합성"+"B".repeat(64));values.put("user_file_nm","합성"+"C".repeat(32));
        values.put("pbs_end_ymd","");values.put("method","selectOfrNotAncmtPbs");values.put("methodnm","selectOfrNotAncmtPbs");
        values.put("jndinm","OfrNotAncmtEJB");values.put("context","NTIS");values.put("isHome","");return values;
    }
    private String bridge(Map<String,String> values) {
        String inputs=values.entrySet().stream().map(e->"<input type='hidden' name='"+e.getKey()+"' value='"+e.getValue()+"'>")
                .collect(java.util.stream.Collectors.joining());
        return "<form id='form' name='form' method='post' action='FDSendNewPbs.jsp'>"+inputs+"</form>";
    }
    private AttachmentPinnedDownloadClient.Download writeResponse(Path output,String body,String type) throws IOException {
        assertThat(Files.exists(output)).isFalse();Files.writeString(output,body,StandardOpenOption.CREATE_NEW);
        return new AttachmentPinnedDownloadClient.Download(Files.size(output),"a".repeat(64),type);
    }
    private AttachmentPinnedDownloadClient.Download run(String page,String period,List<AttachmentPinnedDownloadClient.Request> requests,int firstRequestCount) throws IOException {
        return AttachmentProfileDownloadFlow.selectDownload(profile,initial,output(),1024*1024,(request,limit,approved)->{
            for(int n=0;n<(requests.isEmpty()?firstRequestCount:1);n++) if(!approved.test(request)) throw new IOException("ATTACHMENT_PATH_NOT_APPROVED");
            requests.add(request);
            if(requests.size()==1) { assertThat(limit).isEqualTo(32768);return writeResponse(output(),page,"text/html;charset=UTF-8"); }
            if(requests.size()==2) { assertThat(limit).isEqualTo(256);return writeResponse(output(),period,"text/html;charset=UTF-8"); }
            assertThat(limit).isEqualTo(1024*1024);return writeResponse(output(),"%PDF-1.7","application/pdf");
        });
    }
    @Test void preservesPeriodPolicyAndUsesExactThreeStageRequestsWithoutExecutingScripts() throws Exception {
        var requests=new ArrayList<AttachmentPinnedDownloadClient.Request>();
        var result=run("<script>throw new Error('never execute')</script>"+bridge(fields()),"20991231\n",requests,1);
        assertThat(result.contentType()).isEqualTo("application/pdf");assertThat(requests).hasSize(3);
        assertThat(requests).extracting(AttachmentPinnedDownloadClient.Request::method).containsExactly("GET","POST","POST");
        assertThat(requests.get(1).uri().getPath()).endsWith("/OfrAction.do");assertThat(requests.get(2).uri().getPath()).endsWith("/FDSendNewPbs.jsp");
        assertThat(requests.get(1).form()).containsEntry("isHome","").containsEntry("pbs_end_ymd","");
        assertThat(requests.get(2).form()).containsEntry("isHome","").containsEntry("pbs_end_ymd","20991231");
        assertThat(Files.readString(output())).isEqualTo("%PDF-1.7");assertThat(requests.toString()).doesNotContain("합성","fixture_2026");
    }
    @Test void exactEmptyPeriodVariantsFollowThePublishedPageBehavior() throws Exception {
        for(String period:List.of(""," \r\n ","EmptyYmd")) {
            var requests=new ArrayList<AttachmentPinnedDownloadClient.Request>();run(bridge(fields()),period,requests,1);
            assertThat(requests.getLast().form()).containsEntry("pbs_end_ymd","");Files.delete(output());
        }
    }
    @Test void expiredMalformedOrHtmlPeriodNeverReachesFilePost() {
        for(String period:List.of("20000101","20260230","209912310","<html>login</html>","2099-12-31","20991231\u0000")) {
            var requests=new ArrayList<AttachmentPinnedDownloadClient.Request>();
            assertThatThrownBy(()->run(bridge(fields()),period,requests,1)).hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");
            assertThat(requests).hasSize(2);assertThat(Files.exists(output())).isFalse();
        }
    }
    @Test void changedActionFieldsOwnershipAndPeriodBypassCannotBeSubmitted() {
        var pages=new ArrayList<String>();String correct=bridge(fields());
        pages.add(correct.replace("FDSendNewPbs.jsp","https://other.example/file"));pages.add(correct+correct);
        pages.add(correct.replace("type='hidden'","type='text'"));pages.add(correct.replace("<input ","<input onclick='run()' "));
        for(var changed:List.of(Map.entry("isHome","Y"),Map.entry("file_id","ofr_other_fixture_202609120001"),
                Map.entry("method","deleteSomething"),Map.entry("file_path","/etc/passwd"),Map.entry("user_file_nm","short"))) {
            var values=fields();values.put(changed.getKey(),changed.getValue());pages.add(bridge(values));
        }
        var missing=fields();missing.remove("context");pages.add(bridge(missing));
        for(String page:pages) {
            var requests=new ArrayList<AttachmentPinnedDownloadClient.Request>();
            assertThatThrownBy(()->run(page,"20991231",requests,1)).hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");
            assertThat(requests).hasSize(1);assertThat(Files.exists(output())).isFalse();
        }
    }
    @Test void redirectAndFlowStepsShareTheExistingFourHttpRequestBudget() throws Exception {
        var allowed=new ArrayList<AttachmentPinnedDownloadClient.Request>();run(bridge(fields()),"20991231",allowed,2);
        assertThat(allowed).hasSize(3);Files.delete(output());
        var blocked=new ArrayList<AttachmentPinnedDownloadClient.Request>();
        assertThatThrownBy(()->run(bridge(fields()),"20991231",blocked,3)).hasMessage("ATTACHMENT_PATH_NOT_APPROVED");
        assertThat(blocked).hasSize(2);assertThat(Files.exists(output())).isFalse();
    }
    @Test void preservesPreexistingOutputAndRejectsUnapprovedInitialRequestWithoutTransport() throws Exception {
        Files.writeString(output(),"기존 사용자 파일");var calls=new AtomicInteger();
        assertThatThrownBy(()->AttachmentProfileDownloadFlow.selectDownload(profile,initial,output(),1024,(r,l,a)->{calls.incrementAndGet();throw new AssertionError();}))
                .hasMessage("ATTACHMENT_OUTPUT_EXISTS");assertThat(Files.readString(output())).isEqualTo("기존 사용자 파일");Files.delete(output());
        var outside=AttachmentPinnedDownloadClient.Request.selectGet(URI.create("https://other.example/file"));
        assertThatThrownBy(()->AttachmentProfileDownloadFlow.selectDownload(profile,outside,output(),1024,(r,l,a)->{calls.incrementAndGet();throw new AssertionError();}))
                .hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");assertThat(calls).hasValue(0);
    }
    @Test void transportFailureRemovesItsPartialOutputAndDoesNotStartLaterSteps() {
        var count=new AtomicInteger();
        assertThatThrownBy(()->AttachmentProfileDownloadFlow.selectDownload(profile,initial,output(),1024,(r,l,a)->{
            count.incrementAndGet();Files.writeString(output(),"partial");throw new IOException("ATTACHMENT_DNS_TIMEOUT");
        })).hasMessage("ATTACHMENT_DNS_TIMEOUT");assertThat(count).hasValue(1);assertThat(Files.exists(output())).isFalse();
    }
    @Test void eachRealGatewayStageRechecksDbLeaseAndCumulativeByteReservation() throws Exception {
        var jobs=mock(AnnouncementAttachmentJobService.class);var client=mock(AttachmentPinnedDownloadClient.class);var job=mock(AttachmentJobRow.class);
        UUID id=UUID.randomUUID(),token=UUID.randomUUID();when(job.jobId()).thenReturn(id);when(job.leaseToken()).thenReturn(token);
        when(jobs.saveJobHeartbeat(id,token)).thenReturn(true);when(jobs.selectExternalExecutionAllowed(id,token)).thenReturn(true);
        when(jobs.saveDownloadBytes(eq(id),eq(token),anyLong())).thenReturn(true);
        when(jobs.saveDownloadLease(eq(id),eq(token),anyString())).thenAnswer(c->Optional.of(new AttachmentResourceLease(id,token,List.of(UUID.randomUUID()))));
        var step=new AtomicInteger();
        doAnswer(call->{
            var request=call.getArgument(0,AttachmentPinnedDownloadClient.Request.class);
            Predicate<AttachmentPinnedDownloadClient.Request> allowed=call.getArgument(2);assertThat(allowed.test(request)).isTrue();
            var budget=call.getArgument(5,AttachmentPinnedDownloadClient.ByteReservation.class);assertThat(budget.reserve(100)).isTrue();
            int index=step.incrementAndGet();Path target=call.getArgument(3);
            return writeResponse(target,index==1?bridge(fields()):index==2?"20991231":"%PDF-1.7",index==3?"application/pdf":"text/html");
        }).when(client).selectDownload(any(AttachmentPinnedDownloadClient.Request.class),anySet(),any(),any(),anyLong(),any());
        var result=new AttachmentDownloadGateway(jobs,client).selectDownload(job,profile,initial,output(),1024*1024);
        assertThat(result.contentType()).isEqualTo("application/pdf");assertThat(step).hasValue(3);
        verify(jobs,times(3)).saveJobHeartbeat(id,token);verify(jobs,times(6)).selectExternalExecutionAllowed(id,token);
        verify(jobs,times(3)).saveDownloadLease(eq(id),eq(token),anyString());verify(jobs,times(3)).deleteResourceLease(any());
        verify(jobs,times(3)).saveDownloadBytes(id,token,100L);
    }
    @Test void cancellingBetweenStagesStopsBeforeAnotherNetworkOrLease() throws Exception {
        var jobs=mock(AnnouncementAttachmentJobService.class);var client=mock(AttachmentPinnedDownloadClient.class);var job=mock(AttachmentJobRow.class);
        UUID id=UUID.randomUUID(),token=UUID.randomUUID();when(job.jobId()).thenReturn(id);when(job.leaseToken()).thenReturn(token);
        var allowed=new AtomicBoolean(true);when(jobs.saveJobHeartbeat(id,token)).thenReturn(true);
        when(jobs.selectExternalExecutionAllowed(id,token)).thenAnswer(c->allowed.get());
        when(jobs.saveDownloadLease(eq(id),eq(token),anyString())).thenReturn(Optional.of(new AttachmentResourceLease(id,token,List.of(UUID.randomUUID()))));
        var network=new AtomicInteger();
        doAnswer(call->{
            Predicate<AttachmentPinnedDownloadClient.Request> check=call.getArgument(2);assertThat(check.test(call.getArgument(0))).isTrue();
            network.incrementAndGet();allowed.set(false);return writeResponse(call.getArgument(3),bridge(fields()),"text/html");
        }).when(client).selectDownload(any(AttachmentPinnedDownloadClient.Request.class),anySet(),any(),any(),anyLong(),any());
        assertThatThrownBy(()->new AttachmentDownloadGateway(jobs,client).selectDownload(job,profile,initial,output(),1024*1024))
                .isInstanceOf(AttachmentDownloadGateway.Deferred.class);
        assertThat(network).hasValue(1);assertThat(Files.exists(output())).isFalse();verify(jobs,times(1)).deleteResourceLease(any());
    }
}
