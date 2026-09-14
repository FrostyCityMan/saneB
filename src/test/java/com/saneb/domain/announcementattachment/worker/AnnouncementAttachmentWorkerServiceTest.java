package com.saneb.domain.announcementattachment.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.discovery.BizInfoAttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.SaeolGetAttachmentProfileConfiguration;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.extraction.AttachmentTemporaryStorage;
import com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor;
import com.saneb.domain.announcementattachment.service.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentWorkerServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/** 연결/순서/정리 회귀. HTTP와 Linux 추출은 mock이며 실제 파일 운영 QA와 구분한다. */
class AnnouncementAttachmentWorkerServiceTest {
    @TempDir Path directory;
    private final ObjectMapper mapper=new ObjectMapper();
    private final AnnouncementAttachmentJobService jobs=mock(AnnouncementAttachmentJobService.class);
    private final AnnouncementAttachmentEvidenceService evidence=mock(AnnouncementAttachmentEvidenceService.class);
    private final AnnouncementAttachmentEvaluationService evaluations=mock(AnnouncementAttachmentEvaluationService.class);
    private final AnnouncementAttachmentRetryService retries=mock(AnnouncementAttachmentRetryService.class);
    private final AttachmentDownloadGateway downloads=mock(AttachmentDownloadGateway.class);
    private final AttachmentRuntimeIdentity runtime=mock(AttachmentRuntimeIdentity.class);
    private final IsolatedAttachmentExtractor extractor=mock(IsolatedAttachmentExtractor.class);
    private final BizInfoAttachmentDiscoveryProfile profile=new BizInfoAttachmentDiscoveryProfile();
    private AttachmentJobRow job;
    private AnnouncementAttachmentWorkerService worker;
    private String page;

    @BeforeEach void configure() throws Exception {
        job=selectJob(1,null,"b".repeat(64));
        page=selectPage("공고문.pdf","안내.hwp","신청서.hwpx");
        worker=new AnnouncementAttachmentWorkerServiceImpl(jobs,evidence,evaluations,retries,new AttachmentDiscoveryProfileRegistry(List.of(profile)),
                runtime,new AttachmentTemporaryStorage(directory.toString()),downloads,new AttachmentFileTypeValidator(),extractor,mapper);
        when(jobs.saveNextJobClaim()).thenAnswer(call -> Optional.of(job));
        when(jobs.saveJobHeartbeat(any(),any())).thenReturn(true);
        when(jobs.selectExternalExecutionAllowed(any(),any())).thenReturn(true);
        when(jobs.selectWorkerSourceDetails(any(),any())).thenReturn(Optional.of(new AttachmentWorkerSourceRow("BIZINFO",
                "PBLN_000000000124628",null,null,null)));
        when(jobs.saveExtractionLease(any(),any())).thenAnswer(call -> Optional.of(new AttachmentResourceLease(job.jobId(),job.leaseToken(),List.of(UUID.randomUUID()))));
        when(jobs.saveJobFailure(any(),any(),any())).thenReturn(true);
        when(jobs.saveJobDeferred(any(),any(),anyBoolean())).thenReturn(true);
        when(evidence.saveFileCheckpoint(any(),any(),any())).thenAnswer(call -> {
            try(var paths=Files.walk(directory)) {
                assertThat(paths.filter(Files::isRegularFile).map(p -> p.getFileName().toString()))
                        .noneMatch(name -> name.equals("attachment.bin"));
            }
            return true;
        });
        when(runtime.selectIdentity()).thenReturn(new AttachmentRuntimeIdentity.Identity("1.0.0","b".repeat(64),1,100));
        when(evidence.saveAttachmentSet(any(),any(),any())).thenAnswer(call -> {
            assertTemporaryEmpty();
            return Optional.of(mock(AttachmentSetRow.class));
        });
        when(evaluations.saveJobEvaluation(any(),any())).thenReturn(Optional.of(mock(AttachmentEvaluationRows.Evaluation.class)));
        when(downloads.selectDownload(any(),any(),any(),any(),anyLong(),any())).thenAnswer(call -> {
            ((Runnable)call.getArgument(5)).run();
            Path path=call.getArgument(3);
            var request=(AttachmentPinnedDownloadClient.Request)call.getArgument(2);
            if (request.uri().getPath().contains("selectSIIA")) {
                Files.writeString(path,page);
                return new AttachmentPinnedDownloadClient.Download(Files.size(path),"a".repeat(64),"text/html;charset=UTF-8");
            }
            int index=Integer.parseInt(request.uri().getQuery().substring(request.uri().getQuery().lastIndexOf('=')+1));
            byte[] bytes=switch(index) {
                case 1 -> new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};
                case 2 -> new byte[]{'P','K',3,4,0,0,0,0};
                default -> "%PDF-1.7".getBytes();
            };
            Files.write(path,bytes);
            return new AttachmentPinnedDownloadClient.Download(bytes.length,"a".repeat(64),"application/octet-stream");
        });
        when(extractor.selectExtraction(any())).thenAnswer(call -> {
            Path path=call.getArgument(0);
            byte first=Files.readAllBytes(path)[0];
            String format=first=='%' ? "PDF" : first=='P' ? "HWPX" : "HWP";
            return mapper.readTree("""
                {"format":"%s","qualityCode":"COMPLETE_TEXT","text":"소상공인 지원",
                 "blocks":[{"index":0,"startOffset":0,"endOffset":7,"evidenceScopeId":"p:1","scopeReliable":true,"locator":"p:1"}]}
                """.formatted(format));
        });
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void workerUsesOnlySelectedProfilesLegacyMimeAndKeepsUnknownRole(boolean approvedMime) throws Exception {
        var localProfile = new com.saneb.domain.announcementattachment.discovery.StandardBbsAttachmentProfileConfiguration().selectTaebaekProfileDetails();
        var execution = new AttachmentExecutionSnapshot(localProfile.selectProfileCode(), localProfile.selectProfileHash(),
                AnnouncementAttachmentClassificationEngine.VERSION, "1.0.0", "b".repeat(64));
        job = new AttachmentJobRow(job.jobId(), job.sourceId(), job.contentVersionId(), job.baseEvaluationId(), job.ruleReleaseId(), job.policyId(),
                null, null, 1, 0, 1, "RUNNING", 1, null, job.leaseToken(), job.leaseExpiresAt(), null, job.idempotencyKey(), job.requestHash(),
                mapper.writeValueAsString(execution), 80L*1024*1024, 0L, 0);
        String url = "https://www.taebaek.go.kr/www/selectBbsNttView.do?key=352&bbsNo=25&nttNo=123";
        var normalizer = new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        when(jobs.selectWorkerSourceDetails(any(), any())).thenReturn(Optional.of(new AttachmentWorkerSourceRow("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)), url, "LGS-000121", "SPRING_BBS")));
        worker = new AnnouncementAttachmentWorkerServiceImpl(jobs, evidence, evaluations, retries,
                new AttachmentDiscoveryProfileRegistry(List.of(profile, localProfile)), runtime, new AttachmentTemporaryStorage(directory.toString()),
                downloads, new AttachmentFileTypeValidator(), extractor, mapper);
        doAnswer(call -> {
            var request = call.getArgument(2, AttachmentPinnedDownloadClient.Request.class);
            assertThat(localProfile.selectApprovedRequest(request)).isTrue();
            ((Runnable) call.getArgument(5)).run(); Path output = call.getArgument(3);
            if (request.uri().getPath().endsWith("selectBbsNttView.do")) {
                Files.writeString(output, """
                    <table class='bbs_default view'><tr><th>제목</th><td>소상공인 지원</td></tr>
                    <tr><td title='내용'>지원사업</td></tr><tr><th>파일</th><td><ul class='view_attach'>
                    <li><div class='down_view'><span>공고문.pdf</span><a class='file_down'
                    href='./downloadBbsFile.do?key=352&atchmnflNo=1'>다운로드</a></div></li></ul></td></tr></table>
                    """);
                return new AttachmentPinnedDownloadClient.Download(Files.size(output), "a".repeat(64), "text/html;charset=UTF-8");
            }
            Files.writeString(output, "%PDF-1.7");
            return new AttachmentPinnedDownloadClient.Download(Files.size(output), "a".repeat(64),
                    approvedMime ? "application/x-msdownload" : "application/octer-stream", "attachment; filename=notice.pdf");
        }).when(downloads).selectDownload(any(), any(), any(), any(), anyLong(), any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var saved = selectSaved(); assertThat(saved.discoveryComplete()).isTrue(); assertThat(saved.files()).hasSize(1);
        var file = saved.files().getFirst(); assertThat(file.role()).isEqualTo("UNKNOWN");
        assertThat(file.downloadStatus()).isEqualTo(approvedMime ? "SUCCEEDED" : "BLOCKED");
        if (approvedMime) verify(extractor).selectExtraction(any());
        else { verifyNoInteractions(extractor); assertThat(file.failureCode()).isEqualTo(AttachmentFailureCode.DOWNLOAD_BLOCKED); }
        assertTemporaryEmpty();
    }
    @Test void idleDoesNotStartRuntimeOrNetwork() throws Exception {
        when(jobs.saveNextJobClaim()).thenReturn(Optional.empty());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("IDLE");
        verifyNoInteractions(runtime,downloads,extractor,evidence,evaluations);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,false", "true,false", "true,true"})
    void registeredSaeolProfileProcessesAllFilesWithoutHttpOrAutomaticRoleAssignment(boolean opaquePost, boolean changedForm) throws Exception {
        var localProfile = opaquePost ? new com.saneb.domain.announcementattachment.discovery.HwacheonPostAttachmentDiscoveryProfile()
                : new SaeolGetAttachmentProfileConfiguration().selectBusanNamguProfileDetails();
        var execution = new AttachmentExecutionSnapshot(localProfile.selectProfileCode(),localProfile.selectProfileHash(),
                AnnouncementAttachmentClassificationEngine.VERSION,"1.0.0","b".repeat(64));
        job = new AttachmentJobRow(job.jobId(),job.sourceId(),job.contentVersionId(),job.baseEvaluationId(),job.ruleReleaseId(),job.policyId(),
                null,null,1,0,1,"RUNNING",1,null,job.leaseToken(),job.leaseExpiresAt(),null,job.idempotencyKey(),job.requestHash(),
                mapper.writeValueAsString(execution),80L*1024*1024,0L,0);
        String sourceUrl = (opaquePost ? "https://eminwon.ihc.go.kr" : "http://eminwon.bsnamgu.go.kr")
                + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
                + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=46034&subCheck=" + (opaquePost ? "N" : "Y");
        var normalizer = new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        when(jobs.selectWorkerSourceDetails(any(),any())).thenReturn(Optional.of(new AttachmentWorkerSourceRow("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(sourceUrl)),sourceUrl,opaquePost ? "LGS-000130" : "LGS-000034","SAFE_SAEOL_EMINWON_LEGACY")));
        worker = new AnnouncementAttachmentWorkerServiceImpl(jobs,evidence,evaluations,retries,
                new AttachmentDiscoveryProfileRegistry(List.of(profile,localProfile)),runtime,new AttachmentTemporaryStorage(directory.toString()),
                downloads,new AttachmentFileTypeValidator(),extractor,mapper);
        doAnswer(call -> {
            var request = (AttachmentPinnedDownloadClient.Request)call.getArgument(2);
            assertThat(request.uri().getScheme()).isEqualTo("https");
            assertThat(localProfile.selectApprovedRequest(request)).isTrue();
            assertThat(call.getArgument(1, com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.class)).isSameAs(localProfile);
            ((Runnable)call.getArgument(5)).run(); Path output = call.getArgument(3);
            if(request.uri().getPath().endsWith("OfrAction.do")) {
                String links = java.util.stream.IntStream.range(0,3).mapToObj(i -> opaquePost
                        ? "<a href=\"javascript:goDownLoad('합성"+"A".repeat(64)+"','합성"+Character.toString('B'+i).repeat(64)
                          +"','/ntisho"+"C".repeat(64)+"')\">공고"+i+".pdf</a>"
                        : "<a href=\"javascript:goDownLoad('공고"+i+".pdf','"+i+".pdf','/ntishome/file/upload/ofr/ofr/20260911')\">첨부</a>")
                        .collect(java.util.stream.Collectors.joining());
                String downloadForm = opaquePost ? "<form name='nnn' method='post' action='/emwp/jsp/ofr/FileDownNew.jsp'>"
                        +"<input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'>"
                        +"<input type='hidden' name='file_path'><input type='hidden' name='isHome' value='Y'></form>" : "";
                String label = opaquePost ? "td" : "th";
                if (changedForm) downloadForm = downloadForm.replace("value='Y'", "value='N'");
                Files.writeString(output,downloadForm+"<form name='form1' method='post'><table><tr><"+label+">첨부파일</"+label+"><td>"+links+"</td></tr></table></form>");
                return new AttachmentPinnedDownloadClient.Download(Files.size(output),"a".repeat(64),"text/html;charset=UTF-8");
            }
            if(opaquePost) {
                assertThat(request.method()).isEqualTo("POST"); assertThat(request.uri().getRawQuery()).isNull();
                assertThat(request.form().get("user_file_nm")).isEqualTo("합성"+"A".repeat(64));
                assertThat(request.form()).containsEntry("isHome","Y").hasSize(4);
            } else { assertThat(request.method()).isEqualTo("GET"); assertThat(request.form()).isEmpty(); }
            Files.writeString(output,"%PDF-1.7");
            return new AttachmentPinnedDownloadClient.Download(Files.size(output),"a".repeat(64),"application/pdf");
        }).when(downloads).selectDownload(any(),any(),any(),any(),anyLong(),any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var saved = selectSaved();
        if (changedForm) {
            assertThat(saved.discoveryStatus()).isEqualTo("FAILED");
            assertThat(saved.discoveryComplete()).isFalse(); assertThat(saved.files()).isEmpty();
            assertThat(saved.warningCodes()).containsExactly("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
            verify(downloads,times(1)).selectDownload(any(),any(),any(),any(),anyLong(),any());
            verifyNoInteractions(extractor); assertTemporaryEmpty(); return;
        }
        assertThat(saved.discoveryStatus()).isEqualTo("FOUND");
        assertThat(saved.files()).hasSize(3).allSatisfy(file -> {
            assertThat(file.role()).isEqualTo("UNKNOWN"); assertThat(file.roleOrigin()).isEqualTo("UNKNOWN");
            assertThat(file.downloadStatus()).isEqualTo("SUCCEEDED");
        });
        verify(downloads,times(4)).selectDownload(any(),any(),any(),any(),anyLong(),any());
        verify(extractor,times(3)).selectExtraction(any()); assertTemporaryEmpty();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void legalBoardProfilesReachWorkerAndPreserveUnsupportedAttachmentsWithoutDownloadingThem(boolean busan) throws Exception {
        var configuration=new com.saneb.domain.announcementattachment.discovery.LegalBoardAttachmentProfileConfiguration();
        var local=busan?configuration.selectBusanLegalProfileDetails():configuration.selectGangbukLegalProfileDetails();
        var execution=new AttachmentExecutionSnapshot(local.selectProfileCode(),local.selectProfileHash(),
                AnnouncementAttachmentClassificationEngine.VERSION,"1.0.0","b".repeat(64));
        job=new AttachmentJobRow(job.jobId(),job.sourceId(),job.contentVersionId(),job.baseEvaluationId(),job.ruleReleaseId(),job.policyId(),
                null,null,1,0,1,"RUNNING",1,null,job.leaseToken(),job.leaseExpiresAt(),null,job.idempotencyKey(),job.requestHash(),
                mapper.writeValueAsString(execution),80L*1024*1024,0L,0);
        String url=busan?"https://www.busan.go.kr/nbgosi/view?sno=79571&gosiGbn=A&curPage=1"
                :"https://child.gangbuk.go.kr/portal/bbs/B0000245/view.do?menuNo=200082&nttId=184761";
        var normalizer=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        when(jobs.selectWorkerSourceDetails(any(),any())).thenReturn(Optional.of(new AttachmentWorkerSourceRow("LOCAL_GOV_NOTICE",
                normalizer.hash(normalizer.canonicalizeUrl(url)),url,busan?"LGS-000027":"LGS-000010","SPRING_BBS")));
        worker=new AnnouncementAttachmentWorkerServiceImpl(jobs,evidence,evaluations,retries,new AttachmentDiscoveryProfileRegistry(List.of(profile,local)),
                runtime,new AttachmentTemporaryStorage(directory.toString()),downloads,new AttachmentFileTypeValidator(),extractor,mapper);
        doAnswer(call->{
            var request=(AttachmentPinnedDownloadClient.Request)call.getArgument(2);Path path=call.getArgument(3);
            assertThat(local.selectApprovedRequest(request)).isTrue();assertThat(request.method()).isEqualTo("GET");
            assertThat(call.getArgument(1,com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.class)).isSameAs(local);
            ((Runnable)call.getArgument(5)).run();
            if(request.uri().getPath().contains("view")) {
                String links=java.util.stream.IntStream.range(0,3).mapToObj(i->{
                    String extension=i==2?"xlsx":"pdf";String name="첨부"+i+"."+extension;
                    return busan?"<li><a title='"+name+"' href='/nbgosi/download?fileId=F2609111527472043&seq="+i+"'>"+name+"</a></li>"
                            :"<a class='file' href='https://eminwon.gangbuk.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm="+name
                              +"&sys_file_nm="+i+"."+extension+"&file_path=/ntishome/file/upload/ofr/ofr/20260911'>"+name+"</a>";
                }).collect(java.util.stream.Collectors.joining());
                Files.writeString(path,busan?"<dl class='form-data-info'><dt>첨부파일</dt><dd><ul class='attfiles'>"+links+"</ul></dd></dl>"
                        :"<dl class='file-lists'><dt>첨부</dt><dd class='item'>"+links+"</dd></dl>");
                return new AttachmentPinnedDownloadClient.Download(Files.size(path),"a".repeat(64),"text/html;charset=UTF-8");
            }
            assertThat(request.uri().getQuery()).doesNotContain("xlsx","seq=2");
            Files.writeString(path,"%PDF-1.7");return new AttachmentPinnedDownloadClient.Download(Files.size(path),"a".repeat(64),"application/pdf");
        }).when(downloads).selectDownload(any(),any(),any(),any(),anyLong(),any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var saved=selectSaved();assertThat(saved.discoveryStatus()).isEqualTo("FOUND");assertThat(saved.discoveryComplete()).isTrue();
        assertThat(saved.files()).hasSize(3).allSatisfy(f->{assertThat(f.role()).isEqualTo("UNKNOWN");assertThat(f.roleOrigin()).isEqualTo("UNKNOWN");});
        assertThat(saved.files().subList(0,2)).allSatisfy(f->assertThat(f.downloadStatus()).isEqualTo("SUCCEEDED"));
        assertThat(saved.files().getLast().downloadStatus()).isEqualTo("BLOCKED");
        assertThat(saved.files().getLast().failureCode()).isEqualTo(AttachmentFailureCode.UNSUPPORTED_FORMAT);
        assertThat(saved.files().getLast().extraction()).isNull();assertThat(saved.files().getLast().downloadedBytes()).isZero();
        verify(downloads,times(3)).selectDownload(any(),any(),any(),any(),anyLong(),any());verify(extractor,times(2)).selectExtraction(any());
        assertTemporaryEmpty();
    }
    @Test void sealedRoleReevaluationDoesNotRequireTemporaryStorageOrExternalRuntime() throws Exception {
        var original=selectJob(1,UUID.randomUUID(),"b".repeat(64));
        job=new AttachmentJobRow(original.jobId(),original.sourceId(),original.contentVersionId(),original.baseEvaluationId(),original.ruleReleaseId(),
                original.policyId(),null,original.setId(),original.generation(),original.expectedSourceVersion(),original.expectedAttachmentVersion(),
                "RUNNING",1,null,original.leaseToken(),original.leaseExpiresAt(),null,original.idempotencyKey(),original.requestHash(),original.executionSnapshotJson(),
                83886080L,0L,0,"ROLE_CHANGE",UUID.randomUUID(),UUID.randomUUID());
        var unavailable=mock(AttachmentTemporaryStorage.class);
        when(unavailable.selectWorkspaceAvailable()).thenReturn(false);
        worker=new AnnouncementAttachmentWorkerServiceImpl(jobs,evidence,evaluations,retries,new AttachmentDiscoveryProfileRegistry(List.of(profile)),
                runtime,unavailable,downloads,new AttachmentFileTypeValidator(),extractor,mapper);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        verify(evaluations).saveJobEvaluation(job.jobId(),job.leaseToken());
        verifyNoInteractions(runtime,downloads,extractor,evidence);
        verify(unavailable,never()).insertWorkspace(any(),any());
        verify(jobs,never()).saveJobDeferred(any(),any(),anyBoolean());
    }
    @Test void missingTemporaryStorageDefersUnsealedFetchWithoutConsumingNetworkAttempt() throws Exception {
        var unavailable=mock(AttachmentTemporaryStorage.class);
        when(unavailable.selectWorkspaceAvailable()).thenReturn(false);
        worker=new AnnouncementAttachmentWorkerServiceImpl(jobs,evidence,evaluations,retries,new AttachmentDiscoveryProfileRegistry(List.of(profile)),
                runtime,unavailable,downloads,new AttachmentFileTypeValidator(),extractor,mapper);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("TEMPORARY_UNAVAILABLE");
        verify(jobs).saveJobDeferred(job.jobId(),job.leaseToken(),false);
        verifyNoInteractions(runtime,downloads,extractor,evidence,evaluations);
    }
    @Test void att014EveryFileReachesEvidenceAfterOriginalCleanupThenEvaluation() throws Exception {
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var result=selectSaved();
        assertThat(result.files()).hasSize(3);
        assertThat(result.files()).extracting(AttachmentSetEvidence.File::detectedType).containsExactly("PDF","HWP","HWPX");
        assertThat(result.files()).extracting(AttachmentSetEvidence.File::role).containsExactly("UNKNOWN","UNKNOWN","UNKNOWN");
        verify(downloads,times(4)).selectDownload(any(),any(),any(),any(),anyLong(),any());
        verify(extractor,times(3)).selectExtraction(any());
        var order=inOrder(evidence,evaluations);
        order.verify(evidence).saveAttachmentSet(any(),any(),any());
        order.verify(evaluations).saveJobEvaluation(any(),any());
        assertTemporaryEmpty();
    }
    @Test void att001MissingEligibleSourceNeverRequestsDetail() throws Exception {
        when(jobs.selectWorkerSourceDetails(any(),any())).thenReturn(Optional.empty());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("DEFERRED");
        verify(jobs).saveJobDeferred(job.jobId(),job.leaseToken(),false);
        verifyNoInteractions(downloads,extractor,evidence);
    }
    @Test void att040OffBeforeDetailDoesNotConsumeNetworkAttempt() throws Exception {
        when(jobs.selectExternalExecutionAllowed(any(),any())).thenReturn(false);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("DEFERRED");
        verify(jobs).saveJobDeferred(job.jobId(),job.leaseToken(),false);
        verifyNoInteractions(downloads,extractor,evidence);
        assertTemporaryEmpty();
    }
    @Test void att061ChangedRuntimeStopsBeforeNetwork() throws Exception {
        job=selectJob(1,null,"c".repeat(64));
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("PROFILE_REQUIRED");
        verifyNoInteractions(downloads,extractor,evidence);
    }
    @Test void unavailableRuntimeIsNotMisreportedAsProviderNetworkFailure() throws Exception {
        doThrow(new IOException("EXTRACTOR_ARTIFACT_INVALID")).when(runtime).selectIdentity();
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("ISOLATION_UNAVAILABLE");
        verifyNoInteractions(downloads,extractor,evidence);
    }
    @Test void att012VerifiedNoFilesIsSealedWithoutInventingExtraction() throws Exception {
        page=selectPage();
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(selectSaved().discoveryStatus()).isEqualTo("NO_FILES");
        assertThat(selectSaved().files()).isEmpty();
        verifyNoInteractions(extractor);
    }
    @Test void att013LayoutFailureStoresExactFixedReasonInsteadOfNoFiles() throws Exception {
        page="<html><body>기관 오류 화면</body></html>";
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(selectSaved().discoveryStatus()).isEqualTo("FAILED");
        assertThat(selectSaved().warningCodes()).containsExactly("ATTACHMENT_SELECTOR_CHANGED");
        verifyNoInteractions(extractor);
    }
    @Test void att023UnsupportedFileRemainsVisibleWithoutBinaryRequest() throws Exception {
        page=selectPage("자료.zip");
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(selectSaved().files().getFirst().failureCode()).isEqualTo(AttachmentFailureCode.UNSUPPORTED_FORMAT);
        verify(downloads,times(1)).selectDownload(any(),any(),any(),any(),anyLong(),any());
        verifyNoInteractions(extractor);
    }
    @Test void att020OcrAndFormatMismatchStayDistinctAndDoNotHideOtherFiles() throws Exception {
        doReturn(mapper.readTree("{\"format\":\"PDF\",\"qualityCode\":\"OCR_REQUIRED\",\"text\":\"\",\"blocks\":[]}"))
                .when(extractor).selectExtraction(any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(selectSaved().files()).hasSize(3);
        assertThat(selectSaved().files().getFirst().extraction().quality()).isEqualTo("OCR_REQUIRED");
        assertThat(selectSaved().files().get(1).extraction().quality()).isEqualTo("UNSUPPORTED");
    }
    @Test void att031SealedCrashRecoveryDoesNotRepeatHttpOrExtraction() throws Exception {
        job=selectJob(2,UUID.randomUUID(),"b".repeat(64));
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        verifyNoInteractions(runtime,downloads,extractor,evidence);
        verify(evaluations).saveJobEvaluation(job.jobId(),job.leaseToken());
    }
    @Test void transientDetailFailureRetriesWithNoFalseSet() throws Exception {
        doThrow(new IOException("ATTACHMENT_HTTP_503")).when(downloads).selectDownload(any(),any(),any(),any(),anyLong(),any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("HTTP_SERVER_ERROR");
        verify(jobs).saveJobFailure(job.jobId(),job.leaseToken(),AttachmentFailureCode.HTTP_SERVER_ERROR);
        verifyNoInteractions(evidence,evaluations,extractor);
        assertTemporaryEmpty();
    }
    @Test void lastDetailFailureSealsFailureEvidenceAndCanBeReviewed() throws Exception {
        job=selectJob(3,null,"b".repeat(64));
        doThrow(new IOException("ATTACHMENT_HTTP_503")).when(downloads).selectDownload(any(),any(),any(),any(),anyLong(),any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(selectSaved().discoveryStatus()).isEqualTo("FAILED");
        assertThat(selectSaved().warningCodes()).containsExactly("HTTP_SERVER_ERROR");
        assertTemporaryEmpty();
    }
    @Test void failedSaveNeverLeavesOriginalFiles() throws Exception {
        doThrow(new IllegalStateException("FIXTURE_FAILURE")).when(evidence).saveAttachmentSet(any(),any(),any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("WORKER_PROCESSING_FAILED");
        assertTemporaryEmpty();
        verifyNoInteractions(evaluations);
    }
    @Test void restartRetriesFailedFileOnlyAndPreservesOriginalSuccessfulExtractionTime() throws Exception {
        var cache=insertCheckpointStore();
        var failedOnce=new java.util.concurrent.atomic.AtomicBoolean(false);
        // 두 번째 파일만 첫 회차 503. 기존 mock의 정상 응답은 delegate로 유지한다.
        var successfulDownloads=downloads;
        var gateway=mock(AttachmentDownloadGateway.class);
        when(gateway.selectDownload(any(),any(),any(),any(),anyLong(),any())).thenAnswer(call -> {
            var request=(AttachmentPinnedDownloadClient.Request)call.getArgument(2);
            if(request.uri().getRawQuery()!=null && request.uri().getRawQuery().endsWith("fileSn=1") && !failedOnce.getAndSet(true)) {
                ((Runnable)call.getArgument(5)).run(); throw new IOException("ATTACHMENT_HTTP_503");
            }
            return successfulDownloads.selectDownload(call.getArgument(0),call.getArgument(1),call.getArgument(2),call.getArgument(3),call.getArgument(4),call.getArgument(5));
        });
        worker=selectWorker(gateway);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("HTTP_SERVER_ERROR");
        assertThat(cache).hasSize(2);
        var originalTimes=cache.values().stream().map(f -> f.extraction().completedAtEpochMs()).toList();
        verify(evidence,never()).saveAttachmentSet(any(),any(),any());
        job=selectRetriedJob();
        worker=selectWorker(gateway);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(selectSaved().files()).hasSize(3);
        assertThat(selectSaved().files().stream().map(f -> f.extraction().completedAtEpochMs()).toList()).containsAll(originalTimes);
        verify(gateway,times(6)).selectDownload(any(),any(),any(),any(),anyLong(),any());
        verify(extractor,times(3)).selectExtraction(any());
        assertTemporaryEmpty();
    }
    @Test void newJobGenerationDoesNotReusePriorJobEvenWithSameLocator() throws Exception {
        insertCheckpointStore();
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        job=selectJob(1,null,"b".repeat(64));
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        verify(downloads,times(8)).selectDownload(any(),any(),any(),any(),anyLong(),any());
        verify(extractor,times(6)).selectExtraction(any());
    }
    @Test void manualRetryRequestsOnlySelectedFileAndKeepsManualRole() throws Exception {
        insertManualRetryPlan(1);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var result=ArgumentCaptor.forClass(AttachmentSetEvidence.class);verify(evidence).saveRetriedAttachmentSet(any(),any(),result.capture());
        assertThat(result.getValue().files()).hasSize(1);assertThat(result.getValue().files().getFirst().roleOrigin()).isEqualTo("MANUAL");
        assertThat(result.getValue().files().getFirst().role()).isEqualTo("GUIDE");
        verify(downloads,times(2)).selectDownload(any(),any(),any(),any(),anyLong(),any());verify(extractor,times(1)).selectExtraction(any());
        verify(evidence,never()).saveAttachmentSet(any(),any(),any());assertTemporaryEmpty();
    }
    @Test void manualRetryChangedManifestDoesNotExpandDownloadScopeOrClaimComplete() throws Exception {
        insertManualRetryPlan(1);page=selectPage("공고문.pdf","안내.hwp","신청서.hwpx","새 첨부.pdf");
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var result=ArgumentCaptor.forClass(AttachmentSetEvidence.class);verify(evidence).saveRetriedAttachmentSet(any(),any(),result.capture());
        assertThat(result.getValue().discoveryComplete()).isFalse();assertThat(result.getValue().warningCodes()).contains("DISCOVERY_CHANGED");
        assertThat(result.getValue().files()).hasSize(1);assertThat(result.getValue().files().getFirst().failureCode()).isEqualTo(AttachmentFailureCode.DISCOVERY_CHANGED);
        verify(downloads,times(1)).selectDownload(any(),any(),any(),any(),anyLong(),any());verifyNoInteractions(extractor);
    }
    @Test void manualRetryMultipleFailuresReusesNewSuccessOnItsNextLease() throws Exception {
        insertManualRetryPlan(1);var originalPlan=retries.selectRetryFileList(job.jobId(),job.leaseToken());
        var plan=java.util.stream.IntStream.range(0,originalPlan.size()).mapToObj(index->{var f=originalPlan.get(index);
            return new AttachmentRetryFileRow(f.fileId(),f.locatorJson(),f.locatorHash(),f.displayName(),f.role(),f.roleOrigin(),f.detectedType(),index<2);}).toList();
        when(retries.selectRetryFileList(eq(job.jobId()),any())).thenReturn(plan);insertCheckpointStore();
        var failedOnce=new java.util.concurrent.atomic.AtomicBoolean();var gateway=mock(AttachmentDownloadGateway.class);
        when(gateway.selectDownload(any(),any(),any(),any(),anyLong(),any())).thenAnswer(call->{
            var request=(AttachmentPinnedDownloadClient.Request)call.getArgument(2);
            if(request.uri().getRawQuery()!=null && request.uri().getRawQuery().endsWith("fileSn=1") && !failedOnce.getAndSet(true)) {
                ((Runnable)call.getArgument(5)).run();throw new IOException("ATTACHMENT_HTTP_503");}
            return downloads.selectDownload(call.getArgument(0),call.getArgument(1),call.getArgument(2),call.getArgument(3),call.getArgument(4),call.getArgument(5));});
        worker=selectWorker(gateway);assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("HTTP_SERVER_ERROR");
        job=selectRetriedJob();worker=selectWorker(gateway);assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        verify(gateway,times(5)).selectDownload(any(),any(),any(),any(),anyLong(),any());verify(extractor,times(2)).selectExtraction(any());
        var result=ArgumentCaptor.forClass(AttachmentSetEvidence.class);verify(evidence).saveRetriedAttachmentSet(any(),any(),result.capture());
        assertThat(result.getValue().files()).hasSize(2);assertTemporaryEmpty();
    }
    @Test void manualRetryFinalDiscoveryFailurePreservesFailedSelectionForReview() throws Exception {
        insertManualRetryPlan(3);
        doThrow(new IOException("ATTACHMENT_HTTP_503")).when(downloads).selectDownload(any(),any(),any(),any(),anyLong(),any());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var result=ArgumentCaptor.forClass(AttachmentSetEvidence.class);verify(evidence).saveRetriedAttachmentSet(any(),any(),result.capture());
        assertThat(result.getValue().discoveryStatus()).isEqualTo("FAILED");assertThat(result.getValue().files()).hasSize(1);
        assertThat(result.getValue().warningCodes()).contains("HTTP_SERVER_ERROR","DISCOVERY_FAILED");verifyNoInteractions(extractor);
    }
    private void insertManualRetryPlan(int attempt) throws Exception {
        var original=job;
        job=new AttachmentJobRow(original.jobId(),original.sourceId(),original.contentVersionId(),original.baseEvaluationId(),original.ruleReleaseId(),original.policyId(),
                null,null,2,0,2,"RUNNING",attempt,null,original.leaseToken(),original.leaseExpiresAt(),null,original.idempotencyKey(),original.requestHash(),
                original.executionSnapshotJson(),original.downloadBudgetBytes(),0L,0,"RETRY_FILES",UUID.randomUUID(),UUID.randomUUID());
        var discovered=profile.selectDescriptors("PBLN_000000000124628",page);var plan=new java.util.ArrayList<AttachmentRetryFileRow>();
        for(int i=0;i<discovered.descriptors().size();i++) {
            var d=discovered.descriptors().get(i);String locator=mapper.writeValueAsString(d.locator());
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(locator.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            // 선택 파일의 GUIDE는 관리자가 확정한 값이다. 새 발견의 UNKNOWN으로 덮어쓰지 않아야 한다.
            plan.add(new AttachmentRetryFileRow(UUID.randomUUID(),locator,hash,d.displayName(),i==1?"GUIDE":d.documentRole(),"MANUAL",d.expectedFormat(),i==1));
        }
        when(retries.selectRetryFileList(job.jobId(),job.leaseToken())).thenReturn(plan);
        when(evidence.saveRetriedAttachmentSet(any(),any(),any())).thenAnswer(call->{assertTemporaryEmpty();return Optional.of(mock(AttachmentSetRow.class));});
    }
    @Test void lostCheckpointFenceDefersBeforeProcessingRemainingFiles() throws Exception {
        when(evidence.saveFileCheckpoint(any(),any(),any())).thenReturn(false);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("DEFERRED");
        verify(downloads,times(2)).selectDownload(any(),any(),any(),any(),anyLong(),any());
        verify(evidence,never()).saveAttachmentSet(any(),any(),any());
        verifyNoInteractions(evaluations);
        assertTemporaryEmpty();
    }
    private java.util.Map<String,AttachmentSetEvidence.File> insertCheckpointStore() {
        var cache=new java.util.HashMap<String,AttachmentSetEvidence.File>();
        when(evidence.selectFileCheckpoint(any(),any(),any())).thenAnswer(call -> Optional.ofNullable(cache.get(call.getArgument(0)+":"+call.getArgument(2))));
        when(evidence.saveFileCheckpoint(any(),any(),any())).thenAnswer(call -> {
            var file=(AttachmentSetEvidence.File)call.getArgument(2);
            try(var paths=Files.walk(directory)) { assertThat(paths.filter(p -> p.getFileName().toString().equals("attachment.bin"))).isEmpty(); }
            cache.put(call.getArgument(0)+":"+file.locator(),file); return true;
        });
        return cache;
    }
    private AnnouncementAttachmentWorkerService selectWorker(AttachmentDownloadGateway gateway) {
        return new AnnouncementAttachmentWorkerServiceImpl(jobs,evidence,evaluations,retries,new AttachmentDiscoveryProfileRegistry(List.of(profile)),
                runtime,new AttachmentTemporaryStorage(directory.toString()),gateway,new AttachmentFileTypeValidator(),extractor,mapper);
    }
    private AttachmentJobRow selectRetriedJob() {
        return new AttachmentJobRow(job.jobId(),job.sourceId(),job.contentVersionId(),job.baseEvaluationId(),job.ruleReleaseId(),job.policyId(),
                null,null,job.generation(),job.expectedSourceVersion(),job.expectedAttachmentVersion(),"RUNNING",2,null,UUID.randomUUID(),
                OffsetDateTime.now().plusSeconds(120),null,job.idempotencyKey(),job.requestHash(),job.executionSnapshotJson(),job.downloadBudgetBytes(),
                job.reservedDownloadBytes(),job.rowVersion()+1,job.operationCode(),job.referenceSetId(),job.requestedBy());
    }
    private AttachmentSetEvidence selectSaved() {
        var argument=ArgumentCaptor.forClass(AttachmentSetEvidence.class);
        verify(evidence).saveAttachmentSet(any(),any(),argument.capture());
        return argument.getValue();
    }
    private void assertTemporaryEmpty() throws IOException {
        Path root=directory.resolve("announcement-attachment-tmp");
        if (!Files.exists(root)) return;
        try (var files=Files.list(root)) { assertThat(files.filter(p -> p.getFileName().toString().startsWith("job-")).count()).isZero(); }
    }
    private AttachmentJobRow selectJob(int attempt,UUID set,String configHash) throws Exception {
        var execution=new AttachmentExecutionSnapshot(profile.selectProfileCode(),profile.selectProfileHash(),
                AnnouncementAttachmentClassificationEngine.VERSION,"1.0.0",configHash);
        return new AttachmentJobRow(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),null,set,1,0,1,"RUNNING",attempt,null,UUID.randomUUID(),OffsetDateTime.now().plusSeconds(120),
                null,UUID.randomUUID(),"a".repeat(64),mapper.writeValueAsString(execution),80L*1024*1024,0L,0);
    }
    private String selectPage(String... names) {
        StringBuilder html=new StringBuilder("<meta property='og:title' content='표본'><div class='attached_file_list'><ul>");
        for (int i=0;i<names.length;i++) html.append("<li><span class='file_name'>").append(names[i]).append("</span><a href='/cmm/fms/fileDown.do?atchFileId=FILE_000000000765683&amp;fileSn=")
                .append(i).append("'>다운로드</a></li>");
        return html.append("</ul></div>").toString();
    }
}
