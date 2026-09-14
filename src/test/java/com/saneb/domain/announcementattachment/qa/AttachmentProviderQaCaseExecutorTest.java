package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.extraction.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase.*;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Locator;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** production flow/형식 검사/임시 저장을 쓰되 HTTP와 Linux 추출만 대역이다. 실제 사이트/격리 성공 증거가 아니다. */
class AttachmentProviderQaCaseExecutorTest {
    @TempDir Path directory;
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final String profileHash="a".repeat(64), runtimeHash="b".repeat(64);
    final byte[] binary="%PDF-1.7\nsynthetic-public-document".getBytes(StandardCharsets.UTF_8);
    final AtomicLong nano=new AtomicLong();
    AttachmentDiscoveryProfile profile;
    AttachmentPinnedDownloadClient client;
    AttachmentRuntimeIdentity runtime;
    IsolatedAttachmentExtractor extractor;
    AttachmentTemporaryStorage temporary;
    AttachmentProviderQaCaseExecutor executor;
    Control control;
    List<AttachmentDiscoveryProfile.Descriptor> descriptors;

    @BeforeEach void prepare() throws Exception {
        profile=mock(AttachmentDiscoveryProfile.class); client=mock(AttachmentPinnedDownloadClient.class);
        runtime=mock(AttachmentRuntimeIdentity.class); extractor=mock(IsolatedAttachmentExtractor.class);
        temporary=spy(new AttachmentTemporaryStorage(directory.toString())); control=new Control();
        when(profile.selectProviderCode()).thenReturn("BIZINFO"); when(profile.selectProfileCode()).thenReturn("QA_PROFILE");
        when(profile.selectProfileHash()).thenReturn(profileHash);
        when(profile.selectSourceBindings()).thenReturn(List.of(new AttachmentDiscoveryProfile.SourceBinding(null,null)));
        when(profile.selectApprovedHosts()).thenReturn(Set.of("www.example.go.kr"));
        when(profile.selectApprovedRequest(any(AttachmentPinnedDownloadClient.Request.class))).thenReturn(true);
        when(profile.selectLegacyBinaryContentTypes()).thenReturn(Set.of());
        when(profile.selectDetailUri(any(AttachmentDiscoveryProfile.Source.class))).thenReturn(URI.create("https://www.example.go.kr/detail"));
        when(runtime.selectIdentity()).thenReturn(new AttachmentRuntimeIdentity.Identity("1.0.0",runtimeHash,1,10));
        when(extractor.selectExtraction(any())).thenReturn(output("COMPLETE_TEXT","소상공인 지원금 합성문서"));
        descriptors=List.of(descriptor("1",true),descriptor("2",true));
        when(profile.selectDescriptors(any(AttachmentDiscoveryProfile.Source.class),anyString()))
                .thenAnswer(call->new AttachmentDiscoveryProfile.Result("FOUND",true,descriptors,List.of()));
        mockDownloads(); resetExecutor(profile);
    }
    void resetExecutor(AttachmentDiscoveryProfile selected) {
        executor=new AttachmentProviderQaCaseExecutor(new AttachmentDiscoveryProfileRegistry(List.of(selected)),client,temporary,runtime,extractor,
                new AttachmentFileTypeValidator(),mapper,Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"),ZoneOffset.UTC),nano::get);
    }
    void mockDownloads() throws Exception {
        when(client.selectDownload(any(AttachmentPinnedDownloadClient.Request.class),anySet(),any(),any(Path.class),anyLong(),any()))
                .thenAnswer(call->{
                    var request=call.getArgument(0,AttachmentPinnedDownloadClient.Request.class);
                    Predicate<AttachmentPinnedDownloadClient.Request> approved=call.getArgument(2);
                    if (!approved.test(request)) throw new IOException("UNAPPROVED");
                    Path target=call.getArgument(3);
                    var reservation=call.getArgument(5,AttachmentPinnedDownloadClient.ByteReservation.class);
                    boolean html=request.uri().getPath().equals("/detail");
                    byte[] bytes=html?"<html>합성 상세</html>".getBytes(StandardCharsets.UTF_8):binary;
                    if (!reservation.reserve(bytes.length)) throw new IOException("BYTE_LIMIT");
                    Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
                    return new AttachmentPinnedDownloadClient.Download(bytes.length,sha(bytes),html?"text/html":"application/pdf");
                });
    }
    AttachmentDiscoveryProfile.Descriptor descriptor(String id, boolean allowed) {
        return new AttachmentDiscoveryProfile.Descriptor(URI.create("https://www.example.go.kr/file/"+id),
                new Locator("QA_PROFILE","/file",Map.of("attachmentId",id)),"원문 파일명.pdf",allowed?"PDF":null,"UNKNOWN",allowed);
    }
    AttachmentProviderQaCase input(List<AttachmentDiscoveryProfile.Descriptor> expected) throws Exception {
        var files=new ArrayList<ExpectedFile>();
        for(var descriptor:expected) files.add(new ExpectedFile(executor.selectHash(descriptor.locator()),descriptor.downloadAllowed(),descriptor.expectedFormat(),
                descriptor.downloadAllowed()?sha(binary):null,descriptor.downloadAllowed()?"COMPLETE_TEXT":null,
                descriptor.downloadAllowed()?3:0,descriptor.downloadAllowed()?1:0,descriptor.downloadAllowed()?List.of("소상공인"):List.of()));
        return new AttachmentProviderQaCase("CASE-001","QA_PROFILE",profileHash,
                new AttachmentDiscoveryProfile.Source("BIZINFO","notice-id","https://www.example.go.kr/detail",null,null),
                "소상공인 지원금",rules(),runtimeHash,"FOUND",true,files,new Limits(420,44,80L*1024*1024));
    }
    AttachmentProviderQaCase change(AttachmentProviderQaCase input,String title,String discovery,boolean complete,List<ExpectedFile> files,Limits limits) {
        return new AttachmentProviderQaCase(input.caseId(),input.profileCode(),input.profileHash(),input.source(),title,input.rules(),input.runtimeHash(),
                discovery,complete,files,limits);
    }
    AnnouncementSourceClassificationRuleSet rules() {
        return new AnnouncementSourceClassificationRuleSet("QA-RULE",List.of(rule("TARGET",RuleGroupKindCode.TARGET,"소상공인",TargetCategoryCode.BUSINESS,null),
                rule("SUPPORT",RuleGroupKindCode.SUPPORT_TYPE,"지원금",null,SupportTypeCode.GRANT_SUBSIDY),
                rule("B",RuleGroupKindCode.AUTO_EXCLUDE_B,"수출",null,null)));
    }
    AnnouncementSourceClassificationRule rule(String code,RuleGroupKindCode kind,String word,TargetCategoryCode target,SupportTypeCode support) {
        return new AnnouncementSourceClassificationRule(code,code,kind,word,StrengthCode.STRONG,target,support,
                List.of(AnnouncementSourceClassificationTerm.canonical(word,MatchModeCode.NORMALIZED_PHRASE)),true);
    }
    JsonNode output(String quality,String text) {
        var output=mapper.createObjectNode().put("qualityCode",quality).put("format","PDF").put("text",text);
        var blocks=output.putArray("blocks");
        if(!text.isEmpty()) blocks.addObject().put("index",0).put("startOffset",0).put("endOffset",text.codePointCount(0,text.length()))
                .put("scopeReliable",true).put("evidenceScopeId","paragraph-1").put("locator","paragraph:1");
        return output;
    }
    String sha(byte[] value) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}
    void cleaned() throws Exception {
        try(var paths=Files.walk(directory)) {assertThat(paths.filter(path->Set.of("detail.html","attachment.bin").contains(path.getFileName().toString())).toList()).isEmpty();}
        assertThat(control.activeDownloads).isZero(); assertThat(control.activeExtractions).isZero();
    }
    RoleExpectation roleExpectation(String text) {
        var block=new AttachmentSetEvidence.Block(0,0,text.codePointCount(0,text.length()),"paragraph-1",true,"paragraph:1");
        var assessment=new AttachmentDocumentRoleClassifier().selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,List.of(block),null,0));
        return new RoleExpectation(assessment.ruleVersion(),assessment.rulesHash(),assessment.roleCode(),assessment.reasonCode(),
                assessment.textHash(),assessment.blocksHash(),executor.selectHash(assessment));
    }
    AttachmentProviderQaCase withRole(AttachmentProviderQaCase base,RoleExpectation role) {
        var files=base.files().stream().map(f->new ExpectedFile(f.locatorHash(),f.downloadAllowed(),f.format(),f.binaryHash(),f.quality(),
                f.minimumCharacters(),f.minimumBlocks(),f.requiredPhrases(),role)).toList();
        return change(base,base.title(),base.discoveryStatus(),base.discoveryComplete(),files,base.limits());
    }
    final String noticeText="소상공인 지원금 공고\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월";
    @Test void actualExtractionRoleAndEveryEvidencePositionMustMatchFrozenExpectation() throws Exception {
        when(extractor.selectExtraction(any())).thenReturn(output("COMPLETE_TEXT",noticeText));
        var role=roleExpectation(noticeText);assertThat(role.roleCode()).isEqualTo("NOTICE");
        var result=executor.selectResult(withRole(input(descriptors),role),control);
        assertThat(result.status()).isEqualTo("PASSED");
        assertThat(result.files()).allSatisfy(f->assertThat(f.roleAssessmentHash()).isEqualTo(role.assessmentHash()));
        assertThat(mapper.writeValueAsString(result)).doesNotContain("소상공인","신청기간","paragraph:1");cleaned();
    }
    @ParameterizedTest @ValueSource(strings={"role","reason","textHash","blocksHash","assessmentHash"})
    void completeTextAndPhraseMatchCannotOverrideChangedRoleProof(String field) throws Exception {
        when(extractor.selectExtraction(any())).thenReturn(output("COMPLETE_TEXT",noticeText));var role=roleExpectation(noticeText);
        var changed=new RoleExpectation(role.ruleVersion(),role.rulesHash(),field.equals("role")?"GUIDE":field.equals("reason")?"UNKNOWN":role.roleCode(),
                field.equals("reason")?"ROLE_STRUCTURE_INCOMPLETE":role.reasonCode(),field.equals("textHash")?"e".repeat(64):role.textHash(),
                field.equals("blocksHash")?"e".repeat(64):role.blocksHash(),field.equals("assessmentHash")?"e".repeat(64):role.assessmentHash());
        var result=executor.selectResult(withRole(input(descriptors),changed),control);
        assertThat(result.status()).isEqualTo("FAILED");assertThat(result.allTextComplete()).isFalse();
        assertThat(result.files()).allSatisfy(f->{assertThat(f.reasonCode()).isEqualTo("ROLE_EXPECTATION_CHANGED");assertThat(f.roleAssessmentHash()).isNull();});cleaned();
    }
    @ParameterizedTest @ValueSource(strings={"numericString","fraction","missing","scopeType","hiddenText","outOfRange","extra"})
    void malformedOrIncompleteEvidenceIsNotRoleProof(String field) throws Exception {
        var extracted=(com.fasterxml.jackson.databind.node.ObjectNode)output("COMPLETE_TEXT",noticeText);
        var block=(com.fasterxml.jackson.databind.node.ObjectNode)extracted.path("blocks").get(0);
        switch(field) {
            case "numericString" -> block.put("index","0");case "fraction" -> block.put("index",0.1);
            case "missing" -> block.remove("scopeReliable");case "scopeType" -> block.put("scopeReliable","true");
            case "hiddenText" -> block.put("startOffset",1);case "outOfRange" -> block.put("endOffset",999999);
            default -> block.put("override",true);
        }
        when(extractor.selectExtraction(any())).thenReturn(extracted);
        var result=executor.selectResult(withRole(input(descriptors),roleExpectation(noticeText)),control);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.files()).allSatisfy(f->assertThat(f.reasonCode()).isEqualTo("ROLE_EXTRACTION_STRUCTURE_INVALID"));cleaned();
    }
    @Test void unknownExpectationRemainsUnknownAndDoesNotClaimPolicySuccess() throws Exception {
        String text="소상공인 지원금 합성문서";var role=roleExpectation(text);assertThat(role.roleCode()).isEqualTo("UNKNOWN");
        var result=executor.selectResult(withRole(input(descriptors),role),control);
        assertThat(result.status()).isEqualTo("PASSED");assertThat(result.isPolicyQaPassed()).isFalse();
        assertThat(result.files()).allSatisfy(f->assertThat(f.roleAssessmentHash()).isEqualTo(role.assessmentHash()));cleaned();
    }
    @Test void staleRoleRulesAndNonCompleteRoleExpectationsAreRejectedBeforeAnyIo() throws Exception {
        var role=roleExpectation(noticeText);var base=input(descriptors);
        var stale=new RoleExpectation("document-role-0.0.0",role.rulesHash(),role.roleCode(),role.reasonCode(),role.textHash(),role.blocksHash(),role.assessmentHash());
        assertThatThrownBy(()->executor.selectResult(withRole(base,stale),control)).hasMessage("CASE_INPUT_INVALID");
        var f=base.files().getFirst();var partial=new ExpectedFile(f.locatorHash(),true,"PDF",f.binaryHash(),"PARTIAL_TEXT",3,1,f.requiredPhrases(),role);
        assertThatThrownBy(()->executor.selectResult(change(base,base.title(),"FOUND",true,List.of(partial),base.limits()),control)).hasMessage("CASE_INPUT_INVALID");
        verifyNoInteractions(client,extractor,temporary,runtime);
    }
    @Test void legacyCaseAndResultSerializationDoesNotAddNullRoleProofOrChangeFrozenHash() throws Exception {
        var base=input(descriptors);var before=executor.selectHash(base);var result=executor.selectResult(base,control);
        assertThat(mapper.writeValueAsString(base)).doesNotContain("roleExpectation");
        assertThat(mapper.writeValueAsString(result)).doesNotContain("roleAssessmentHash");
        assertThat(executor.selectHash(mapper.readTree(mapper.writeValueAsString(base)))).isEqualTo(before);cleaned();
    }
    @Test void everyFrozenFileIsExtractedAndOnlyMetadataReturnsAfterCleanup() throws Exception {
        var input=input(descriptors); var result=executor.selectResult(input,control);
        assertThat(result.status()).isEqualTo("PASSED"); assertThat(result.allTextComplete()).isTrue(); assertThat(result.isPolicyQaPassed()).isFalse();
        assertThat(result.files()).hasSize(2).allSatisfy(file->{assertThat(file.status()).isEqualTo("PASSED");assertThat(file.textHash()).hasSize(64);});
        assertThat(result.requestReservations()).isEqualTo(3); assertThat(result.discoveredFileCount()).isEqualTo(2);
        assertThat(result.originalFilesRemoved()).isTrue(); assertThat(result.reservedBytes()).isEqualTo(control.bytes);
        assertThat(result.inputHash()).isEqualTo(executor.selectHash(input));
        assertThat(mapper.writeValueAsString(result)).doesNotContain("www.example", "원문 파일명", "소상공인", "합성문서", directory.toString());
        assertThat(input.toString()).doesNotContain("www.example","소상공인");
        verify(extractor,times(2)).selectExtraction(any()); verify(runtime,times(2)).selectIdentity(); cleaned();
    }
    @ParameterizedTest @ValueSource(strings={"PDF","HWP","HWPX"})
    void eachDeclaredFormatUsesProductionSignatureAndRequiresSameExtractionFormat(String format) throws Exception {
        byte[] payload=switch(format) {
            case "HWP" -> new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};
            case "HWPX" -> new byte[]{'P','K',3,4,0,0,0,0};
            default -> binary;
        };
        // signature용 합성 bytes다. HWP/OLE나 HWPX/ZIP 실제 추출을 증명하지 않는다.
        var descriptor=new AttachmentDiscoveryProfile.Descriptor(URI.create("https://www.example.go.kr/file/1"),
                new Locator("QA_PROFILE","/file",Map.of("attachmentId","1")),"합성 문서",format,"UNKNOWN",true);
        descriptors=List.of(descriptor);var base=input(descriptors);
        var expected=new ExpectedFile(executor.selectHash(descriptor.locator()),true,format,sha(payload),"COMPLETE_TEXT",3,1,List.of("소상공인"));
        when(client.selectDownload(any(AttachmentPinnedDownloadClient.Request.class),anySet(),any(),any(Path.class),anyLong(),any()))
                .thenAnswer(call->{
                    var request=call.getArgument(0,AttachmentPinnedDownloadClient.Request.class);Predicate<AttachmentPinnedDownloadClient.Request> allowed=call.getArgument(2);
                    assertThat(allowed.test(request)).isTrue();boolean detail=request.uri().getPath().equals("/detail");
                    byte[] bytes=detail?"<html></html>".getBytes(StandardCharsets.UTF_8):payload;
                    call.getArgument(5,AttachmentPinnedDownloadClient.ByteReservation.class).reserve(bytes.length);
                    Files.write(call.getArgument(3,Path.class),bytes,StandardOpenOption.CREATE_NEW);
                    return new AttachmentPinnedDownloadClient.Download(bytes.length,sha(bytes),detail?"text/html":"application/octet-stream");
                });
        var extracted=(com.fasterxml.jackson.databind.node.ObjectNode)output("COMPLETE_TEXT","소상공인 지원금");extracted.put("format",format);
        when(extractor.selectExtraction(any())).thenReturn(extracted);
        var result=executor.selectResult(change(base,base.title(),"FOUND",true,List.of(expected),base.limits()),control);
        assertThat(result.status()).isEqualTo("PASSED");assertThat(result.files().getFirst().format()).isEqualTo(format);cleaned();
    }
    @ParameterizedTest @ValueSource(strings={"소상공인 수출 지원금","관련 없는 제목"})
    void excludedTitleMakesNoDetailFileRuntimeOrTemporaryCall(String title) throws Exception {
        var base=input(descriptors); var input=change(base,title,"TITLE_BLOCKED",false,List.of(),base.limits());
        var result=executor.selectResult(input,control);
        assertThat(result.status()).isEqualTo("PASSED"); assertThat(result.reasonCode()).isEqualTo("TITLE_BLOCKED_WITHOUT_REQUEST");
        assertThat(result.requestReservations()).isZero(); assertThat(result.allTextComplete()).isFalse();
        verifyNoInteractions(client,runtime,temporary,extractor); cleaned();
    }
    @Test void unexpectedTitleRejectionFailsInsteadOfClaimingFileCoverage() throws Exception {
        var base=input(descriptors); var result=executor.selectResult(change(base,"수출 소상공인 지원금","FOUND",true,base.files(),base.limits()),control);
        assertThat(result.reasonCode()).isEqualTo("TITLE_NOT_ELIGIBLE"); assertThat(result.files()).allSatisfy(f->assertThat(f.status()).isEqualTo("NOT_RUN"));
        verifyNoInteractions(client,extractor,temporary); cleaned();
    }
    @ParameterizedTest @ValueSource(strings={"ADD","MISSING","DUPLICATE","TYPE","INCOMPLETE"})
    void scopeChangeStopsAllFileDownloadsBeforeTheFirstFile(String kind) throws Exception {
        var input=input(descriptors);
        switch(kind) {
            case "ADD" -> descriptors=List.of(descriptor("1",true),descriptor("2",true),descriptor("3",true));
            case "MISSING" -> descriptors=List.of(descriptor("1",true));
            case "DUPLICATE" -> descriptors=List.of(descriptor("1",true),descriptor("1",true));
            case "TYPE" -> descriptors=List.of(descriptor("1",true),descriptor("2",false));
            default -> when(profile.selectDescriptors(any(AttachmentDiscoveryProfile.Source.class),anyString()))
                    .thenReturn(new AttachmentDiscoveryProfile.Result("FAILED",false,descriptors,List.of("DISCOVERY_INCOMPLETE")));
        }
        var result=executor.selectResult(input,control);
        assertThat(result.reasonCode()).isEqualTo("DISCOVERY_SCOPE_CHANGED"); assertThat(result.requestReservations()).isEqualTo(1);
        assertThat(result.files()).hasSize(2).allSatisfy(f->assertThat(f.status()).isEqualTo("NOT_RUN")); verifyNoInteractions(extractor); cleaned();
    }
    @Test void discoveryOrderDoesNotChangeFrozenFileResults() throws Exception {
        var input=input(descriptors); descriptors=List.of(descriptors.get(1),descriptors.get(0));
        var result=executor.selectResult(input,control);
        assertThat(result.status()).isEqualTo("PASSED");
        assertThat(result.files().stream().map(AttachmentProviderQaCaseExecutor.FileResult::locatorHash)).containsExactlyElementsOf(input.files().stream().map(ExpectedFile::locatorHash).toList()); cleaned();
    }
    @Test void downloadFlowUsesSameBudgetAndReleasesEachTransportPermit() throws Exception {
        var flowProfile=mock(AttachmentDiscoveryProfile.class,withSettings().extraInterfaces(AttachmentDownloadFlowProfile.class));
        when(flowProfile.selectProviderCode()).thenReturn("BIZINFO");when(flowProfile.selectProfileCode()).thenReturn("QA_PROFILE");
        when(flowProfile.selectProfileHash()).thenReturn(profileHash);when(flowProfile.selectSourceBindings()).thenReturn(List.of(new AttachmentDiscoveryProfile.SourceBinding(null,null)));
        when(flowProfile.selectDetailUri(any(AttachmentDiscoveryProfile.Source.class))).thenReturn(URI.create("https://www.example.go.kr/detail"));
        when(flowProfile.selectApprovedHosts()).thenReturn(Set.of("www.example.go.kr"));
        when(flowProfile.selectLegacyBinaryContentTypes()).thenReturn(Set.of());
        when(flowProfile.selectApprovedRequest(any(AttachmentPinnedDownloadClient.Request.class))).thenReturn(true);
        when(flowProfile.selectDescriptors(any(AttachmentDiscoveryProfile.Source.class),anyString()))
                .thenAnswer(call->new AttachmentDiscoveryProfile.Result("FOUND",true,descriptors,List.of()));
        when(((AttachmentDownloadFlowProfile)flowProfile).selectDownload(any(),any(),anyLong(),any())).thenAnswer(call->{
            var request=call.getArgument(0,AttachmentPinnedDownloadClient.Request.class);Path target=call.getArgument(1);
            long limit=call.getArgument(2);var operation=call.getArgument(3,AttachmentDownloadFlowProfile.Operation.class);
            if (!request.uri().getPath().equals("/detail")) {
                operation.selectDownload(AttachmentPinnedDownloadClient.Request.selectGet(URI.create("https://www.example.go.kr/detail")),Math.min(limit,1024));
                Files.delete(target);
            }
            return operation.selectDownload(request,limit);
        });
        resetExecutor(flowProfile);
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("PASSED");assertThat(result.requestReservations()).isEqualTo(5);cleaned();
    }
    @Test void forgedBinaryMimeFailsProductionSignatureValidation() throws Exception {
        var base=input(descriptors);
        when(client.selectDownload(any(AttachmentPinnedDownloadClient.Request.class),anySet(),any(),any(Path.class),anyLong(),any()))
                .thenAnswer(call->{
                    var request=call.getArgument(0,AttachmentPinnedDownloadClient.Request.class);
                    Predicate<AttachmentPinnedDownloadClient.Request> allowed=call.getArgument(2);assertThat(allowed.test(request)).isTrue();
                    boolean detail=request.uri().getPath().equals("/detail");
                    byte[] bytes=detail?"<html></html>".getBytes(StandardCharsets.UTF_8):binary;
                    call.getArgument(5,AttachmentPinnedDownloadClient.ByteReservation.class).reserve(bytes.length);
                    Files.write(call.getArgument(3,Path.class),bytes,StandardOpenOption.CREATE_NEW);
                    return new AttachmentPinnedDownloadClient.Download(bytes.length,sha(bytes),"text/html");
                });
        var result=executor.selectResult(base,control);
        assertThat(result.status()).isEqualTo("FAILED");assertThat(result.files()).allSatisfy(f->assertThat(f.reasonCode()).isEqualTo("FILE_EXECUTION_FAILED"));
        verifyNoInteractions(extractor);cleaned();
    }
    @Test void profileBindingMustMatchExactInstitutionBeforeAnyIo() throws Exception {
        when(profile.selectSourceBindings()).thenReturn(List.of(new AttachmentDiscoveryProfile.SourceBinding("LGS-123","SPRING_BBS")));
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.reasonCode()).isEqualTo("PROFILE_BINDING_CHANGED");verifyNoInteractions(client,runtime,temporary,extractor);cleaned();
    }
    @Test void binaryChangeIsNotExtractedAndOtherExpectedFileStillRuns() throws Exception {
        var base=input(descriptors); var changed=new ArrayList<>(base.files()); var file=changed.getFirst();
        changed.set(0,new ExpectedFile(file.locatorHash(),true,"PDF","c".repeat(64),file.quality(),3,1,file.requiredPhrases()));
        var result=executor.selectResult(change(base,base.title(),"FOUND",true,changed,base.limits()),control);
        assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.files().get(0).reasonCode()).isEqualTo("BINARY_CHANGED");
        assertThat(result.files().get(1).status()).isEqualTo("PASSED"); verify(extractor,times(1)).selectExtraction(any()); cleaned();
    }
    @Test void missingExpectedPhraseFailsEvenWhenExtractionSaysComplete() throws Exception {
        when(extractor.selectExtraction(any())).thenReturn(output("COMPLETE_TEXT","다른 문서입니다"));
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("FAILED"); assertThat(result.allTextComplete()).isFalse();
        assertThat(result.files()).allSatisfy(f->assertThat(f.reasonCode()).isEqualTo("EXTRACTION_TEXT_EXPECTATION_FAILED")); cleaned();
    }
    @Test void unexpectedOcrRequirementIsRecordedAsOcrNotCompleteOrUnknown() throws Exception {
        when(extractor.selectExtraction(any())).thenReturn(output("OCR_REQUIRED",""));
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("FAILED");assertThat(result.allTextComplete()).isFalse();
        assertThat(result.files()).allSatisfy(f->{assertThat(f.reasonCode()).isEqualTo("EXTRACTION_QUALITY_CHANGED");assertThat(f.quality()).isEqualTo("OCR_REQUIRED");});cleaned();
    }
    @Test void partialFailureKeepsSuccessfulOtherFileAndNeverLeaksExceptionContent() throws Exception {
        when(extractor.selectExtraction(any())).thenThrow(new IOException("https://secret.invalid/private 원문 문자열")).thenReturn(output("COMPLETE_TEXT","소상공인 지원금"));
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.files().get(0).status()).isEqualTo("FAILED"); assertThat(result.files().get(1).status()).isEqualTo("PASSED");
        assertThat(mapper.writeValueAsString(result)).doesNotContain("secret.invalid","원문 문자열"); cleaned();
    }
    @Test void unsupportedFilesRemainInDenominatorWithoutDownloadOrCompleteTextClaim() throws Exception {
        descriptors=List.of(descriptor("1",true),descriptor("2",false));
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("PASSED"); assertThat(result.expectedFileCount()).isEqualTo(2); assertThat(result.allTextComplete()).isFalse();
        assertThat(result.files().get(1).status()).isEqualTo("UNSUPPORTED_NOT_DOWNLOADED"); assertThat(result.requestReservations()).isEqualTo(2); cleaned();
    }
    @ParameterizedTest @ValueSource(strings={"OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED","PARTIAL_TEXT"})
    void expectedDegradedOutputIsSeparateFromFullTextSupport(String quality) throws Exception {
        descriptors=List.of(descriptor("1",true)); var base=input(descriptors); var file=base.files().getFirst();
        boolean text="PARTIAL_TEXT".equals(quality);
        var expected=new ExpectedFile(file.locatorHash(),true,"PDF",file.binaryHash(),quality,text?3:0,text?1:0,text?List.of("소상공인"):List.of());
        when(extractor.selectExtraction(any())).thenReturn(output(quality,text?"소상공인 일부": ""));
        var result=executor.selectResult(change(base,base.title(),"FOUND",true,List.of(expected),base.limits()),control);
        assertThat(result.status()).isEqualTo("PASSED"); assertThat(result.allTextComplete()).isFalse(); assertThat(result.isPolicyQaPassed()).isFalse(); cleaned();
    }
    @Test void actualEmptyAreaAndFailedDiscoveryHaveDistinctExpectedResults() throws Exception {
        var base=input(descriptors);
        for(String status:List.of("NO_FILES","FAILED")) {
            when(profile.selectDescriptors(any(AttachmentDiscoveryProfile.Source.class),anyString()))
                    .thenReturn(new AttachmentDiscoveryProfile.Result(status,status.equals("NO_FILES"),List.of(),List.of()));
            var result=executor.selectResult(change(base,base.title(),status,status.equals("NO_FILES"),List.of(),base.limits()),control);
            assertThat(result.status()).isEqualTo("PASSED"); assertThat(result.discoveryStatus()).isEqualTo(status); assertThat(result.allTextComplete()).isFalse();
        }
        verifyNoInteractions(extractor); cleaned();
    }
    @Test void cancellationBeforeStartTouchesNoRuntimeOrNetwork() throws Exception {
        control.allowed=false; var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("CANCELLED"); verifyNoInteractions(client,temporary,runtime,extractor); cleaned();
    }
    @Test void cancellationAfterExtractionDoesNotPublishTextAndKeepsUnrunRemainder() throws Exception {
        when(extractor.selectExtraction(any())).thenAnswer(call->{control.allowed=false;return output("COMPLETE_TEXT","소상공인 지원금");});
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("CANCELLED"); assertThat(result.files().get(0).reasonCode()).isEqualTo("EXECUTION_STOPPED");
        assertThat(result.files().get(0).textHash()).isNull(); assertThat(result.files().get(1).status()).isEqualTo("NOT_RUN"); cleaned();
    }
    @Test void perCaseRequestCapIsNotRefundedAndStopsNextFile() throws Exception {
        var base=input(descriptors); var result=executor.selectResult(change(base,base.title(),"FOUND",true,base.files(),new Limits(420,2,80L*1024*1024)),control);
        assertThat(result.reasonCode()).isEqualTo("REQUEST_LIMIT"); assertThat(result.requestReservations()).isEqualTo(2);
        assertThat(result.files().get(0).status()).isEqualTo("PASSED"); assertThat(result.files().get(1).reasonCode()).isEqualTo("REQUEST_LIMIT"); cleaned();
    }
    @Test void byteAndExternalLedgerCapsAreEnforcedBeforeBodyWrites() throws Exception {
        var base=input(descriptors); var result=executor.selectResult(change(base,base.title(),"FOUND",true,base.files(),new Limits(420,44,1)),control);
        assertThat(result.reasonCode()).isEqualTo("BYTE_LIMIT"); assertThat(result.reservedBytes()).isZero(); cleaned();
        control.rejectRequests=true; result=executor.selectResult(base,control);
        assertThat(result.reasonCode()).isEqualTo("REQUEST_LIMIT"); assertThat(result.requestReservations()).isZero(); cleaned();
    }
    @Test void enoughTimeForSingleOperationAndCleanupIsRequired() throws Exception {
        var base=input(descriptors); var result=executor.selectResult(change(base,base.title(),"FOUND",true,base.files(),new Limits(35,44,1024)),control);
        assertThat(result.reasonCode()).isEqualTo("CASE_DEADLINE"); verifyNoInteractions(client,temporary,extractor); cleaned();
    }
    @Test void elapsedDeadlineAfterFirstFileLeavesRestUnrun() throws Exception {
        when(extractor.selectExtraction(any())).thenAnswer(call->{nano.set(421_000_000_000L);return output("COMPLETE_TEXT","소상공인 지원금");});
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.reasonCode()).isEqualTo("CASE_DEADLINE"); assertThat(result.files().get(0).reasonCode()).isEqualTo("CASE_DEADLINE");
        assertThat(result.files().get(1).status()).isEqualTo("NOT_RUN"); cleaned();
    }
    @Test void runtimeChangeAtEndInvalidatesOtherwiseSuccessfulFiles() throws Exception {
        when(runtime.selectIdentity()).thenReturn(new AttachmentRuntimeIdentity.Identity("1.0.0",runtimeHash,1,10),
                new AttachmentRuntimeIdentity.Identity("1.0.0","c".repeat(64),1,10));
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.reasonCode()).isEqualTo("RUNTIME_CHANGED"); assertThat(result.files()).allSatisfy(f->assertThat(f.status()).isEqualTo("PASSED"));
        assertThat(result.allTextComplete()).isFalse(); cleaned();
    }
    @Test void missingLinuxIsolationDoesNotDownloadFirst() throws Exception {
        when(runtime.selectIdentity()).thenThrow(new IOException("ISOLATION_UNAVAILABLE"));
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("FAILED");assertThat(result.reasonCode()).isEqualTo("ISOLATION_UNAVAILABLE");
        verifyNoInteractions(client,temporary,extractor); cleaned();
    }
    @Test void cleanupFailureCannotReturnPassed() throws Exception {
        doAnswer(call->{var workspace=(AttachmentTemporaryStorage.Workspace)call.callRealMethod(); var wrapped=spy(workspace);
            doAnswer(close->{workspace.close();throw new IOException("private path");}).when(wrapped).close(); return wrapped;
        }).when(temporary).insertWorkspace(any(),any());
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.reasonCode()).isEqualTo("TEMPORARY_CLEANUP_FAILED"); assertThat(result.originalFilesRemoved()).isFalse();
        assertThat(result.allTextComplete()).isFalse(); cleaned();
    }
    @Test void leaseCancellationDuringCleanupCannotReturnPassed() throws Exception {
        doAnswer(call->{var workspace=(AttachmentTemporaryStorage.Workspace)call.callRealMethod();var wrapped=spy(workspace);
            doAnswer(close->{workspace.close();control.allowed=false;return null;}).when(wrapped).close();return wrapped;
        }).when(temporary).insertWorkspace(any(),any());
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.status()).isEqualTo("CANCELLED");assertThat(result.originalFilesRemoved()).isTrue();assertThat(result.allTextComplete()).isFalse();cleaned();
    }
    @Test void failedPermitReleaseStopsRemainingDownloads() throws Exception {
        control.failExtractionRelease=true;
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.reasonCode()).isEqualTo("RESOURCE_RELEASE_FAILED"); assertThat(result.files().get(1).status()).isEqualTo("NOT_RUN"); cleaned();
    }
    @Test void failedLedgerCheckStopsInsteadOfTryingOtherFiles() throws Exception {
        when(extractor.selectExtraction(any())).thenAnswer(call->{control.failOwnershipCheck=true;return output("COMPLETE_TEXT","소상공인 지원금");});
        var result=executor.selectResult(input(descriptors),control);
        assertThat(result.reasonCode()).isEqualTo("EXECUTION_CONTROL_FAILED");assertThat(result.files().get(0).reasonCode()).isEqualTo("EXECUTION_CONTROL_FAILED");
        assertThat(result.files().get(1).status()).isEqualTo("NOT_RUN");cleaned();
    }
    @Test void callerMustProvideResourceLeaseAndNoDbTransactionMayEncloseNetwork() throws Exception {
        control.noPermit=true; var result=executor.selectResult(input(descriptors),control);
        assertThat(result.reasonCode()).isEqualTo("RESOURCE_UNAVAILABLE"); assertThat(result.requestReservations()).isZero(); cleaned();
        var base=input(descriptors);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {assertThatThrownBy(()->executor.selectResult(base,control)).hasMessage("TRANSACTION_NOT_ALLOWED");}
        finally {TransactionSynchronizationManager.setActualTransactionActive(false);}
    }
    @Test void invalidBudgetDuplicateFilesAndMissingTextAssertionsAreRejectedBeforeIo() throws Exception {
        var base=input(descriptors);
        for(Limits limits:List.of(new Limits(421,44,1024),new Limits(420,45,1024),new Limits(420,44,80L*1024*1024+1)))
            assertThatThrownBy(()->executor.selectResult(change(base,base.title(),"FOUND",true,base.files(),limits),control)).hasMessage("CASE_INPUT_INVALID");
        assertThatThrownBy(()->executor.selectResult(change(base,base.title(),"FOUND",true,List.of(base.files().get(0),base.files().get(0)),base.limits()),control))
                .hasMessage("CASE_INPUT_INVALID");
        var file=base.files().get(0); var incomplete=new ExpectedFile(file.locatorHash(),true,"PDF",file.binaryHash(),"COMPLETE_TEXT",0,0,List.of());
        assertThatThrownBy(()->executor.selectResult(change(base,base.title(),"FOUND",true,List.of(incomplete),base.limits()),control)).hasMessage("CASE_INPUT_INVALID");
        verifyNoInteractions(client,temporary,runtime,extractor);
    }
    final class Control implements AttachmentProviderQaCaseExecutor.ExecutionControl {
        boolean allowed=true,rejectRequests,noPermit,failExtractionRelease,failOwnershipCheck;
        long bytes; int activeDownloads,activeExtractions;
        @Override public boolean selectExecutionAllowed() {if(failOwnershipCheck)throw new IllegalStateException("private database details");return allowed;}
        @Override public AutoCloseable selectDownloadPermit(String hostHash) {
            assertThat(hostHash).matches("[0-9a-f]{64}"); if(noPermit) return null;
            activeDownloads++;return ()->activeDownloads--;
        }
        @Override public AutoCloseable selectExtractionPermit() {
            if(noPermit) return null;
            activeExtractions++;return ()->{activeExtractions--;if(failExtractionRelease)throw new IOException("release private details");};
        }
        @Override public boolean saveRequestReservation() {return !rejectRequests;}
        @Override public boolean saveByteReservation(long amount) {bytes+=amount;return true;}
    }
}
