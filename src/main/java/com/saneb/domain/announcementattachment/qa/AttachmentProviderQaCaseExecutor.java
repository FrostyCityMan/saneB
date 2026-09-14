package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.extraction.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase.ExpectedFile;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.LongSupplier;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 고정 공고 전체의 실제 transport/격리 추출 실행 단위. DB 원장·전체 Provider QA 성공은 별도다. */
@Component
public final class AttachmentProviderQaCaseExecutor {
    private static final long MIB = 1024L * 1024;
    private static final Set<String> QUALITIES = AttachmentProviderQaCaseContract.QUALITIES;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final AttachmentPinnedDownloadClient client;
    private final AttachmentTemporaryStorage temporary;
    private final AttachmentRuntimeIdentity runtime;
    private final IsolatedAttachmentExtractor extractor;
    private final AttachmentFileTypeValidator types;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final LongSupplier nanoTime;

    /** 호출 원장이 소유권·공유 자원·누적 예산을 실제 저장해야 한다. 무제한/무인증 운영 기본 구현은 없다. */
    public interface ExecutionControl {
        boolean selectExecutionAllowed();
        AutoCloseable selectDownloadPermit(String hostHash);
        AutoCloseable selectExtractionPermit();
        boolean saveRequestReservation();
        boolean saveByteReservation(long bytes);
    }
    public record FileResult(String locatorHash, String status, String reasonCode, String format, String quality,
            long bytes, String binaryHash, String textHash, int characterCount, int blockCount,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            String roleAssessmentHash) {
        public FileResult(String locatorHash,String status,String reasonCode,String format,String quality,long bytes,
                String binaryHash,String textHash,int characterCount,int blockCount) {
            this(locatorHash,status,reasonCode,format,quality,bytes,binaryHash,textHash,characterCount,blockCount,null);
        }
    }
    public record Result(String scope, String caseId, String inputHash, String profileHash, String runtimeHash,
            String status, String reasonCode, String titleStage, String discoveryStatus, boolean discoveryComplete,
            int expectedFileCount, int discoveredFileCount, List<FileResult> files, long requestReservations,
            long reservedBytes, boolean originalFilesRemoved, boolean allTextComplete, boolean isPolicyQaPassed,
            Instant startedAt, Instant completedAt) {
        public Result { files = List.copyOf(files); }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AttachmentProviderQaCaseExecutor(AttachmentDiscoveryProfileRegistry profiles, AttachmentPinnedDownloadClient client,
            AttachmentTemporaryStorage temporary, AttachmentRuntimeIdentity runtime, IsolatedAttachmentExtractor extractor,
            AttachmentFileTypeValidator types, ObjectMapper mapper) {
        this(profiles, client, temporary, runtime, extractor, types, mapper, Clock.systemUTC(), System::nanoTime);
    }
    AttachmentProviderQaCaseExecutor(AttachmentDiscoveryProfileRegistry profiles, AttachmentPinnedDownloadClient client,
            AttachmentTemporaryStorage temporary, AttachmentRuntimeIdentity runtime, IsolatedAttachmentExtractor extractor,
            AttachmentFileTypeValidator types, ObjectMapper mapper, Clock clock, LongSupplier nanoTime) {
        this.profiles=profiles; this.client=client; this.temporary=temporary; this.runtime=runtime;
        this.extractor=extractor; this.types=types; this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.clock=clock; this.nanoTime=nanoTime;
    }

    public Result selectResult(AttachmentProviderQaCase input, ExecutionControl control) {
        validate(input, control);
        String inputHash=selectHash(input);
        var state=new State(input, control);
        AttachmentTemporaryStorage.Workspace workspace=null;
        boolean cleaned=true;
        try {
            state.check(0);
            var title=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    input.source().providerCode(), input.title(), null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE),input.rules());
            state.check(0);
            state.titleStage=title.titleStageCode().name();
            boolean eligible=title.semanticStatusCode()!=SemanticStatusCode.EXCLUDED
                    && Set.of(TitleStageCode.GROUP_A_MATCHED,TitleStageCode.COMBINATION_MATCHED).contains(title.titleStageCode());
            if (!eligible) {
                if (!"TITLE_BLOCKED".equals(input.discoveryStatus())) throw failure("TITLE_NOT_ELIGIBLE");
                state.discoveryStatus="NOT_REQUESTED"; state.status="PASSED"; state.reason="TITLE_BLOCKED_WITHOUT_REQUEST";
            } else {
                if ("TITLE_BLOCKED".equals(input.discoveryStatus())) throw failure("TITLE_GATE_EXPECTATION_CHANGED");
                var profile=profiles.selectProfileDetails(input.source().providerCode(),input.profileCode(),input.profileHash())
                        .orElseThrow(()->failure("PROFILE_CHANGED"));
                if (!profile.selectSourceBindings().contains(new AttachmentDiscoveryProfile.SourceBinding(
                        input.source().localSourceCode(),input.source().listParserProfileCode()))) throw failure("PROFILE_BINDING_CHANGED");
                var detail=profile.selectDetailUri(input.source());
                validateRuntime(input.runtimeHash());
                state.check(35);
                workspace=temporary.insertWorkspace(UUID.randomUUID(),UUID.randomUUID()); cleaned=false;
                AttachmentDiscoveryProfile.Result discovery;
                try {
                    var downloaded=selectDownload(profile,AttachmentPinnedDownloadClient.Request.selectGet(detail),workspace.selectDetailPath(),MIB,state);
                    String mime=downloaded.contentType()==null?"":downloaded.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT);
                    if (!Set.of("text/html","application/xhtml+xml").contains(mime)) throw failure("DETAIL_CONTENT_TYPE_CHANGED");
                    try (var stream=Files.newInputStream(workspace.selectDetailPath())) {
                        discovery=profile.selectDescriptors(input.source(),Jsoup.parse(stream,null,detail.toASCIIString()).outerHtml());
                    }
                } finally { Files.deleteIfExists(workspace.selectDetailPath()); }
                state.check(0);
                state.discoveryStatus=discovery.status(); state.discoveryComplete=discovery.complete();
                state.discovered=discovery.descriptors().size();
                if (!input.discoveryStatus().equals(discovery.status()) || input.discoveryComplete()!=discovery.complete()
                        || discovery.descriptors().size()!=input.files().size()) throw failure("DISCOVERY_SCOPE_CHANGED");
                var descriptors=new HashMap<String,AttachmentDiscoveryProfile.Descriptor>();
                for (var descriptor:discovery.descriptors()) {
                    if (descriptor==null || descriptor.locator()==null || !input.profileCode().equals(descriptor.locator().profileCode())
                            || descriptors.putIfAbsent(selectHash(descriptor.locator()),descriptor)!=null) throw failure("DISCOVERY_SCOPE_CHANGED");
                }
                // 새로 추가된 파일 또는 형식 변경은 승인한 공개 전체 표본과 다르다. 어떤 파일도 먼저 내려받지 않는다.
                for (var expected:input.files()) {
                    var descriptor=descriptors.get(expected.locatorHash());
                    if (descriptor==null || descriptor.downloadAllowed()!=expected.downloadAllowed()
                            || !Objects.equals(expected.format(),descriptor.expectedFormat())) throw failure("DISCOVERY_SCOPE_CHANGED");
                }
                for (int index=0;index<input.files().size();index++) {
                    state.check(0);
                    var expected=input.files().get(index);
                    state.files.set(index,selectFile(profile,descriptors.get(expected.locatorHash()),expected,workspace,state));
                }
                state.check(0);
                validateRuntime(input.runtimeHash());
                if (state.files.stream().anyMatch(f->"FAILED".equals(f.status()))) throw failure("FILE_EXPECTATION_FAILED");
                state.status="PASSED"; state.reason="FIXED_NOTICE_EXPECTATIONS_MATCHED";
            }
        } catch (Stopped stopped) {
            state.status="EXECUTION_STOPPED".equals(stopped.code)?"CANCELLED":"FAILED"; state.reason=stopped.code;
        } catch (IOException | RuntimeException exception) {
            state.status="FAILED"; state.reason="PROVIDER_CASE_EXECUTION_FAILED";
        } finally {
            if (workspace!=null) {
                try { workspace.close(); cleaned=true; }
                catch (IOException | RuntimeException exception) { cleaned=false; state.status="FAILED"; state.reason="TEMPORARY_CLEANUP_FAILED"; }
            }
        }
        if ("PASSED".equals(state.status)) {
            try {state.check(0);}
            catch (Stopped stopped) {state.status="EXECUTION_STOPPED".equals(stopped.code)?"CANCELLED":"FAILED";state.reason=stopped.code;}
        }
        boolean complete="PASSED".equals(state.status) && "FOUND".equals(state.discoveryStatus) && state.discoveryComplete
                && !state.files.isEmpty() && state.files.stream().allMatch(f->"COMPLETE_TEXT".equals(f.quality()) && "PASSED".equals(f.status()));
        return new Result("SINGLE_FIXED_NOTICE_PROVIDER_QA",input.caseId(),inputHash,input.profileHash(),input.runtimeHash(),
                state.status,state.reason,state.titleStage,state.discoveryStatus,state.discoveryComplete,input.files().size(),state.discovered,
                state.files,state.requests,state.bytes,cleaned,complete,false,state.started,clock.instant());
    }

    private FileResult selectFile(AttachmentDiscoveryProfile profile, AttachmentDiscoveryProfile.Descriptor descriptor,
            ExpectedFile expected, AttachmentTemporaryStorage.Workspace workspace, State state) throws IOException {
        if (!expected.downloadAllowed()) return new FileResult(expected.locatorHash(),"UNSUPPORTED_NOT_DOWNLOADED",null,
                expected.format(),null,0,null,null,0,0);
        AttachmentPinnedDownloadClient.Download downloaded=null;
        String actualQuality=null;
        try {
            downloaded=selectDownload(profile,descriptor.selectRequest(),workspace.selectBinaryPath(),20*MIB,state);
            if (!expected.binaryHash().equals(downloaded.sha256())) throw failure("BINARY_CHANGED");
            String format=types.selectFormat(workspace.selectBinaryPath(),downloaded,expected.format(),
                    profile.selectUtf8DispositionOctets(),profile.selectLegacyBinaryContentTypes());
            state.check(35);
            JsonNode output;
            var permit=invokeControl(state.control::selectExtractionPermit);
            if (permit==null) throw failure("RESOURCE_UNAVAILABLE");
            try {
                state.check(35);
                output=extractor.selectExtraction(workspace.selectBinaryPath());
            } catch (Stopped stopped) {throw stopped;}
            catch (Exception exception) {throw failure("EXTRACTION_EXECUTION_FAILED");}
            finally {closePermit(permit);}
            state.check(0);
            if (output!=null && (QUALITIES.contains(output.path("qualityCode").asText())
                    || Set.of("FAILED","TIMEOUT","ISOLATION_UNAVAILABLE","CANCELLED").contains(output.path("qualityCode").asText())))
                actualQuality=output.path("qualityCode").asText();
            if (output==null || !output.isObject() || !expected.quality().equals(output.path("qualityCode").asText()))
                throw failure("EXTRACTION_QUALITY_CHANGED");
            String quality=output.path("qualityCode").asText();
            boolean hasText=Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(quality);
            String text=output.path("text").asText("");
            int length=text.codePointCount(0,text.length());
            if (Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED").contains(quality) && !format.equals(output.path("format").asText()))
                throw failure("EXTRACTION_FORMAT_CHANGED");
            if (length>1_000_000 || text.indexOf('\0')>=0 || !output.path("blocks").isArray() || output.path("blocks").size()>20000
                    || (!hasText && (!text.isEmpty() || !output.path("blocks").isEmpty()))) throw failure("EXTRACTION_OUTPUT_INVALID");
            if (hasText && (length<expected.minimumCharacters() || output.path("blocks").size()<expected.minimumBlocks()
                    || expected.requiredPhrases().stream().anyMatch(phrase->!text.contains(phrase)))) throw failure("EXTRACTION_TEXT_EXPECTATION_FAILED");
            String roleHash=selectRoleAssessmentHash(expected,output,text);
            return new FileResult(expected.locatorHash(),"PASSED",null,format,quality,downloaded.bytes(),downloaded.sha256(),
                    hasText?selectTextHash(text):null,length,output.path("blocks").size(),roleHash);
        } catch (Stopped stopped) {
            if (Set.of("EXECUTION_STOPPED","CASE_DEADLINE","REQUEST_LIMIT","BYTE_LIMIT","RESOURCE_UNAVAILABLE","RESOURCE_RELEASE_FAILED","EXECUTION_CONTROL_FAILED").contains(stopped.code)) {
                state.files.set(state.input.files().indexOf(expected),failedFile(expected,downloaded,stopped.code,actualQuality));
                throw stopped;
            }
            return failedFile(expected,downloaded,stopped.code,actualQuality);
        } catch (IOException | RuntimeException exception) {
            return failedFile(expected,downloaded,"FILE_EXECUTION_FAILED",actualQuality);
        } finally {
            try {Files.deleteIfExists(workspace.selectBinaryPath());}
            catch (IOException exception) {
                state.files.set(state.input.files().indexOf(expected),failedFile(expected,downloaded,"TEMPORARY_CLEANUP_FAILED",actualQuality));
                throw failure("TEMPORARY_CLEANUP_FAILED");
            }
        }
    }
    private String selectRoleAssessmentHash(ExpectedFile file,JsonNode output,String text) {
        var expected=file.roleExpectation();if(expected==null)return null;
        // 실제 extractor block만 사용한다. 파일명·본문·다른 첨부에서 역할 근거를 가져오지 않는다.
        var blocks=new ArrayList<AttachmentSetEvidence.Block>();
        for(var block:output.path("blocks")) {
            if(!block.isObject() || block.size()!=6)throw failure("ROLE_EXTRACTION_STRUCTURE_INVALID");
            for(String field:List.of("index","startOffset","endOffset"))
                if(!block.path(field).isIntegralNumber() || !block.path(field).canConvertToInt())throw failure("ROLE_EXTRACTION_STRUCTURE_INVALID");
            if(!block.path("scopeReliable").isBoolean() || !block.path("evidenceScopeId").isTextual() || !block.path("locator").isTextual())
                throw failure("ROLE_EXTRACTION_STRUCTURE_INVALID");
            blocks.add(new AttachmentSetEvidence.Block(block.path("index").intValue(),block.path("startOffset").intValue(),
                    block.path("endOffset").intValue(),block.path("evidenceScopeId").textValue(),block.path("scopeReliable").booleanValue(),block.path("locator").textValue()));
        }
        AttachmentDocumentRoleClassifier.Assessment actual;
        try {actual=new AttachmentDocumentRoleClassifier().selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,blocks,null,0));}
        catch(IllegalArgumentException invalid){throw failure("ROLE_EXTRACTION_STRUCTURE_INVALID");}
        String hash=selectHash(actual);
        if(!expected.ruleVersion().equals(actual.ruleVersion()) || !expected.rulesHash().equals(actual.rulesHash())
                || !expected.roleCode().equals(actual.roleCode()) || !expected.reasonCode().equals(actual.reasonCode())
                || !expected.textHash().equals(actual.textHash()) || !expected.blocksHash().equals(actual.blocksHash())
                || !expected.assessmentHash().equals(hash))throw failure("ROLE_EXPECTATION_CHANGED");
        return hash;
    }
    private FileResult failedFile(ExpectedFile expected, AttachmentPinnedDownloadClient.Download download, String reason, String quality) {
        return new FileResult(expected.locatorHash(),"FAILED",reason,expected.format(),quality,
                download==null?0:download.bytes(),download==null?null:download.sha256(),null,0,0);
    }
    private AttachmentPinnedDownloadClient.Download selectDownload(AttachmentDiscoveryProfile profile,
            AttachmentPinnedDownloadClient.Request initial, Path output, long limit, State state) throws IOException {
        state.check(35);
        return AttachmentProfileDownloadFlow.selectDownload(profile,initial,output,limit,(request,maximum,approved)->{
            AutoCloseable[] permit={null}; String[] host={null};
            try {
                return client.selectDownload(request,profile.selectApprovedHosts(),next->{
                    if (!approved.test(next)) return false;
                    state.check(35);
                    String current=next.uri().getHost().toLowerCase(Locale.ROOT);
                    if (!current.equals(host[0])) {
                        closePermit(permit[0]); permit[0]=null;
                        permit[0]=invokeControl(()->state.control.selectDownloadPermit(selectTextHash(current)));
                        if (permit[0]==null) throw failure("RESOURCE_UNAVAILABLE");
                        host[0]=current;
                    }
                    if (state.requests>=state.input.limits().maximumRequestReservations() || !invokeControl(state.control::saveRequestReservation)) throw failure("REQUEST_LIMIT");
                    state.requests++; return true;
                },output,maximum,bytes->{
                    state.check(0);
                    if (bytes<=0 || bytes>state.input.limits().maximumReservedBytes()-state.bytes || !invokeControl(()->state.control.saveByteReservation(bytes)))
                        throw failure("BYTE_LIMIT");
                    state.bytes+=bytes; return true;
                });
            } finally { closePermit(permit[0]); }
        });
    }
    private void closePermit(AutoCloseable permit) {
        if (permit==null) return;
        try {permit.close();} catch (Exception exception) {throw failure("RESOURCE_RELEASE_FAILED");}
    }
    private <T> T invokeControl(java.util.function.Supplier<T> action) {
        try {return action.get();} catch (RuntimeException exception) {throw failure("EXECUTION_CONTROL_FAILED");}
    }
    private void validateRuntime(String expected) throws IOException {
        AttachmentRuntimeIdentity.Identity installed;
        try {installed=runtime.selectIdentity();}
        catch (IOException exception) {throw failure("ISOLATION_UNAVAILABLE".equals(exception.getMessage())?"ISOLATION_UNAVAILABLE":"RUNTIME_VERIFICATION_FAILED");}
        if (installed==null || !AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(installed.extractorVersion())
                || !expected.equals(installed.configHash())) throw failure("RUNTIME_CHANGED");
    }
    public String selectHash(Object value) {
        try {return selectTextHash(mapper.writeValueAsString(mapper.convertValue(value,Object.class)));}
        catch (Exception exception) {throw failure("CASE_INPUT_INVALID");}
    }
    private String selectTextHash(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch (Exception exception) {throw failure("CASE_HASH_FAILED");}
    }
    private void validate(AttachmentProviderQaCase input, ExecutionControl control) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw failure("TRANSACTION_NOT_ALLOWED");
        if (control==null) throw failure("CASE_INPUT_INVALID");
        try {AttachmentProviderQaCaseContract.validate(input);}catch(IllegalArgumentException invalid){throw failure("CASE_INPUT_INVALID");}
    }
    private static Stopped failure(String code) {return new Stopped(code);}
    private static final class Stopped extends RuntimeException {
        private final String code;
        private Stopped(String code) {super(code,null,false,false);this.code=code;}
    }
    private final class State {
        private final AttachmentProviderQaCase input;
        private final ExecutionControl control;
        private final Instant started=clock.instant();
        private final long deadline;
        private final List<FileResult> files=new ArrayList<>();
        private String status="FAILED",reason="NOT_EXECUTED",titleStage="NOT_RUN",discoveryStatus="NOT_RUN";
        private boolean discoveryComplete;
        private int discovered=-1;
        private long requests,bytes;
        private State(AttachmentProviderQaCase input, ExecutionControl control) {
            this.input=input; this.control=control; this.deadline=nanoTime.getAsLong()+input.limits().maximumSeconds()*1_000_000_000L;
            for(var file:input.files()) files.add(new FileResult(file.locatorHash(),"NOT_RUN",null,file.format(),null,0,null,null,0,0));
        }
        private void check(int reserveSeconds) {
            if (Thread.currentThread().isInterrupted() || !invokeControl(control::selectExecutionAllowed)) throw failure("EXECUTION_STOPPED");
            if (deadline-nanoTime.getAsLong()<=reserveSeconds*1_000_000_000L) throw failure("CASE_DEADLINE");
        }
    }
}
