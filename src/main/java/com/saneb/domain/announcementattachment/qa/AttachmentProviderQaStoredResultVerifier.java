package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Item;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.Run;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 서버 고정 입력과 DB 불변 원장을 결합한다. 관리자 제공 JSON/원문을 판정 API로 받지 않는다. */
@Component
public final class AttachmentProviderQaStoredResultVerifier {
    private final ObjectMapper mapper;
    private static final Set<String> ROOT=Set.of("scope","caseId","inputHash","profileHash","runtimeHash","status","reasonCode","titleStage","discoveryStatus",
            "discoveryComplete","expectedFileCount","discoveredFileCount","files","requestReservations","reservedBytes","originalFilesRemoved","allTextComplete","isPolicyQaPassed","startedAt","completedAt");
    private static final Set<String> FILE=Set.of("locatorHash","status","reasonCode","format","quality","bytes","binaryHash","textHash","characterCount","blockCount");
    public record Verified(String evidenceHash,int fileCount,long requestReservations,long reservedBytes,boolean allTextComplete){ }
    public static final class Failure extends RuntimeException {
        private final String code;
        public Failure(String code){super(code,null,false,false);this.code=code;}
        public String selectCode(){return code;}
    }
    public AttachmentProviderQaStoredResultVerifier(ObjectMapper mapper){
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    public Verified selectVerifiedResult(Item row,Run run,AttachmentProviderQaCase input,Instant cutoff) {
        try {
            AttachmentProviderQaCaseContract.validate(input);
            require(row!=null && run!=null && cutoff!=null && row.caseId()!=null && run.runId().equals(row.runId()) && "COMPLETED".equals(run.statusCode()) && "PASSED".equals(row.statusCode())
                    && row.errorCode()==null && row.rowVersion()!=null && row.rowVersion()>=2,"CASE_NOT_PASSED");
            require(Objects.equals(row.caseCode(),input.caseId()) && Objects.equals(row.profileHash(),input.profileHash())
                    && Objects.equals(row.inputHash(),hash(input)) && Objects.equals(run.runtimeHash(),input.runtimeHash())
                    && Objects.equals(row.expectedFileCount(),input.files().size()) && Objects.equals(row.maximumSeconds(),input.limits().maximumSeconds())
                    && Objects.equals(row.maximumRequests(),input.limits().maximumRequestReservations()) && Objects.equals(row.maximumBytes(),input.limits().maximumReservedBytes()),"CASE_INPUT_CHANGED");
            require(row.evidenceJson()!=null && row.evidenceJson().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=32768 && isHash(row.evidenceHash()),"CASE_EVIDENCE_MISSING");
            JsonNode tree=mapper.readTree(row.evidenceJson());fields(tree,ROOT);
            for(var file:tree.path("files")) {
                var expectedFields=new HashSet<>(FILE);
                if(file.has("roleAssessmentHash"))expectedFields.add("roleAssessmentHash");
                fields(file,expectedFields);
                if(file.has("roleAssessmentHash"))require(file.path("roleAssessmentHash").isTextual(),"EVIDENCE_TYPE_INVALID");
            }
            for(String key:List.of("expectedFileCount","discoveredFileCount","requestReservations","reservedBytes"))integer(tree,key);
            for(String key:List.of("discoveryComplete","originalFilesRemoved","allTextComplete","isPolicyQaPassed"))require(tree.path(key).isBoolean(),"EVIDENCE_TYPE_INVALID");
            for(String key:List.of("scope","caseId","inputHash","profileHash","runtimeHash","status","reasonCode","titleStage","discoveryStatus","startedAt","completedAt"))
                require(tree.path(key).isTextual(),"EVIDENCE_TYPE_INVALID");
            require(tree.path("files").isArray(),"EVIDENCE_TYPE_INVALID");
            require(row.evidenceHash().equals(hash(mapper.convertValue(tree,Object.class))),"CASE_EVIDENCE_HASH_CHANGED");
            var result=mapper.treeToValue(tree,AttachmentProviderQaCaseExecutor.Result.class);
            require("SINGLE_FIXED_NOTICE_PROVIDER_QA".equals(result.scope()) && "PASSED".equals(result.status()) && result.originalFilesRemoved() && !result.isPolicyQaPassed()
                    && Objects.equals(row.caseCode(),result.caseId()) && Objects.equals(row.inputHash(),result.inputHash()) && Objects.equals(row.profileHash(),result.profileHash())
                    && Objects.equals(run.runtimeHash(),result.runtimeHash()) && result.expectedFileCount()==input.files().size() && result.files().size()==input.files().size(),"CASE_RESULT_BINDING_CHANGED");
            require(run.createdAt()!=null && run.expiresAt()!=null && run.completedAt()!=null && row.startedAt()!=null && row.completedAt()!=null
                    && !row.startedAt().isBefore(run.createdAt()) && !row.completedAt().isBefore(row.startedAt()) && !row.completedAt().isAfter(run.completedAt())
                    && !row.completedAt().isAfter(run.expiresAt()) && !row.completedAt().toInstant().isAfter(cutoff)
                    && result.startedAt()!=null && result.completedAt()!=null && !result.startedAt().isBefore(row.startedAt().toInstant())
                    && !result.completedAt().isBefore(result.startedAt()) && !result.completedAt().isAfter(row.completedAt().toInstant())
                    && Duration.between(result.startedAt(),result.completedAt()).compareTo(Duration.ofSeconds(input.limits().maximumSeconds()))<=0
                    && Duration.between(row.startedAt(),row.completedAt()).compareTo(Duration.ofMinutes(8))<=0,"CASE_EXECUTION_TIME_INVALID");
            require(row.requestReservations()!=null && row.reservedBytes()!=null && result.requestReservations()==row.requestReservations() && result.reservedBytes()==row.reservedBytes()
                    && result.requestReservations()>=0 && result.requestReservations()<=input.limits().maximumRequestReservations()
                    && result.reservedBytes()>=0 && result.reservedBytes()<=input.limits().maximumReservedBytes(),"CASE_USAGE_CHANGED");
            boolean titleBlocked="TITLE_BLOCKED".equals(input.discoveryStatus());
            var title=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(input.source().providerCode(),input.title(),
                    null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),input.rules());
            boolean eligible=title.semanticStatusCode()!=SemanticStatusCode.EXCLUDED && Set.of(TitleStageCode.GROUP_A_MATCHED,TitleStageCode.COMBINATION_MATCHED).contains(title.titleStageCode());
            require(Objects.equals(result.titleStage(),title.titleStageCode().name()) && titleBlocked!=eligible,"TITLE_EXPECTATION_CHANGED");
            if(titleBlocked) {
                require("TITLE_BLOCKED_WITHOUT_REQUEST".equals(result.reasonCode()) && "NOT_REQUESTED".equals(result.discoveryStatus()) && !result.discoveryComplete()
                        && result.discoveredFileCount()==-1 && result.requestReservations()==0 && result.reservedBytes()==0
                        && !Set.of("NOT_RUN","GROUP_A_MATCHED","COMBINATION_MATCHED").contains(result.titleStage()),"TITLE_BLOCKED_REQUESTED");
            } else {
                require("FIXED_NOTICE_EXPECTATIONS_MATCHED".equals(result.reasonCode()) && Objects.equals(input.discoveryStatus(),result.discoveryStatus())
                        && input.discoveryComplete()==result.discoveryComplete() && result.discoveredFileCount()==input.files().size()
                        && Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(result.titleStage()) && result.requestReservations()>=1 && result.reservedBytes()>0,"DISCOVERY_EXPECTATION_CHANGED");
            }
            long downloaded=0;int downloadable=0;
            for(int index=0;index<input.files().size();index++) {
                var expected=input.files().get(index);var actual=result.files().get(index);var node=tree.path("files").get(index);
                for(String key:List.of("bytes","characterCount","blockCount"))integer(node,key);
                for(String key:List.of("locatorHash","status"))require(node.path(key).isTextual(),"EVIDENCE_TYPE_INVALID");
                for(String key:List.of("reasonCode","format","quality","binaryHash","textHash"))require(node.path(key).isTextual() || node.path(key).isNull(),"EVIDENCE_TYPE_INVALID");
                require(Objects.equals(expected.locatorHash(),actual.locatorHash()) && Objects.equals(expected.format(),actual.format()) && actual.reasonCode()==null,"FILE_BINDING_CHANGED");
                var role=expected.roleExpectation();
                require(role==null ? actual.roleAssessmentHash()==null
                        : "COMPLETE_TEXT".equals(actual.quality()) && Objects.equals(role.textHash(),actual.textHash())
                            && Objects.equals(role.assessmentHash(),actual.roleAssessmentHash()),"ROLE_EXPECTATION_CHANGED");
                if(!expected.downloadAllowed()) {
                    require("UNSUPPORTED_NOT_DOWNLOADED".equals(actual.status()) && actual.bytes()==0 && actual.binaryHash()==null && actual.quality()==null
                            && actual.textHash()==null && actual.characterCount()==0 && actual.blockCount()==0,"UNSUPPORTED_FILE_DOWNLOADED");
                } else {
                    downloadable++;downloaded=Math.addExact(downloaded,actual.bytes());
                    require("PASSED".equals(actual.status()) && Objects.equals(expected.binaryHash(),actual.binaryHash()) && Objects.equals(expected.quality(),actual.quality())
                            && actual.bytes()>0 && actual.bytes()<=20L*1024*1024,"FILE_EXPECTATION_CHANGED");
                    if(Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(actual.quality())) {
                        require(isHash(actual.textHash()) && actual.characterCount()>=expected.minimumCharacters() && actual.characterCount()<=1_000_000
                                && actual.blockCount()>=expected.minimumBlocks() && actual.blockCount()<=20000,"TEXT_EXPECTATION_CHANGED");
                    } else require(actual.textHash()==null && actual.characterCount()==0 && actual.blockCount()==0,"NON_TEXT_RESULT_CHANGED");
                }
            }
            boolean complete="FOUND".equals(result.discoveryStatus()) && result.discoveryComplete() && !result.files().isEmpty()
                    && result.files().stream().allMatch(f->"PASSED".equals(f.status()) && "COMPLETE_TEXT".equals(f.quality()));
            require(result.allTextComplete()==complete && downloaded<=result.reservedBytes() && (titleBlocked || result.requestReservations()>=1L+downloadable),"RESULT_COVERAGE_OR_USAGE_CHANGED");
            return new Verified(row.evidenceHash(),input.files().size(),result.requestReservations(),result.reservedBytes(),complete);
        }catch(Failure failure){throw failure;}
        catch(Exception exception){throw new Failure("CASE_EVIDENCE_INVALID");}
    }
    public String hash(Object value) {
        try {
            byte[] canonical=mapper.writeValueAsBytes(mapper.convertValue(value,Object.class));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        }
        catch(Exception exception){throw new Failure("CASE_EVIDENCE_HASH_FAILED");}
    }
    private static void fields(JsonNode value,Set<String> expected){
        require(value.isObject(),"EVIDENCE_FIELDS_INVALID");var actual=new HashSet<String>();value.fieldNames().forEachRemaining(actual::add);
        require(actual.equals(expected),"EVIDENCE_FIELDS_INVALID");
    }
    private static void integer(JsonNode node,String key){require(node.path(key).isIntegralNumber() && node.path(key).canConvertToLong(),"EVIDENCE_TYPE_INVALID");}
    private static boolean isHash(String value){return value!=null && value.matches("[0-9a-f]{64}");}
    private static void require(boolean accepted,String code){if(!accepted)throw new Failure(code);}
}
