package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.*;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 현재 규칙으로 서버에서 분류 계약을 재실행한다. 파일/DB/운영 QA나 게시 승인을 대신하지 않는다. */
@Component
public final class AnnouncementAttachmentPolicyGoldenGate {
    public static final String SUITE_VERSION="attachment-golden-1.0.0";
    public static final int CASE_COUNT=30;
    private static final String TITLE="소상공인 지원금";
    private static final String COMBINATION="소상공인 지원금";
    private final AnnouncementSourceClassificationEngine baseEngine;
    private final AnnouncementAttachmentClassificationEngine engine;
    private final AttachmentSegmentPolicyGoldenGate segmentGate;
    private final JsonMapper mapper=JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    public AnnouncementAttachmentPolicyGoldenGate() { this(new AnnouncementSourceClassificationEngine(),new AnnouncementAttachmentClassificationEngine()); }
    AnnouncementAttachmentPolicyGoldenGate(AnnouncementSourceClassificationEngine baseEngine,AnnouncementAttachmentClassificationEngine engine) {
        this(baseEngine,engine,new AttachmentSegmentPolicyGoldenGate());
    }
    AnnouncementAttachmentPolicyGoldenGate(AnnouncementSourceClassificationEngine baseEngine,AnnouncementAttachmentClassificationEngine engine,AttachmentSegmentPolicyGoldenGate segmentGate) {
        this.baseEngine=baseEngine;this.engine=engine;this.segmentGate=segmentGate;
    }
    public record Result(String suiteVersion,String engineVersion,String ruleReleaseCode,String ruleSnapshotHash,
            String ruleContentHash,String resultHash,int caseCount,List<String> caseIds) {
        public Result { caseIds=List.copyOf(caseIds); }
    }
    public Result selectValidatedResult(AnnouncementSourceClassificationRuleSet rules,String ruleSnapshotHash,Configuration configuration) {
        if(configuration==null || !configuration.selectEngineCurrent()) throw failure("ENGINE","정책의 엔진·구간 규칙 버전과 지문을 확인하세요.");
        var legacy=selectValidatedResult(rules,ruleSnapshotHash);
        if(configuration.segmentRuleVersion()==null) return legacy;
        try {
            var signatures=segmentGate.selectValidatedSignatures(rules,configuration.segmentRuleVersion(),configuration.segmentRulesHash());
            var ids=new ArrayList<>(legacy.caseIds());ids.addAll(signatures.keySet());
            return new Result(AttachmentSegmentPolicyGoldenGate.SUITE_VERSION,AttachmentSegmentClassificationEngine.VERSION,
                    legacy.ruleReleaseCode(),legacy.ruleSnapshotHash(),legacy.ruleContentHash(),
                    hash(List.of(AttachmentSegmentPolicyGoldenGate.SUITE_VERSION,AttachmentSegmentClassificationEngine.VERSION,
                            configuration.segmentRuleVersion(),configuration.segmentRulesHash(),legacy.resultHash(),signatures)),ids.size(),ids);
        } catch(AttachmentSegmentPolicyGoldenGate.Failure exception) {
            throw failure("SEGMENT/"+exception.selectCaseId(),"구간 엔진의 고정 판정·위치·참고 근거 검증을 통과하지 못했습니다.");
        } catch(RuntimeException exception) {
            throw failure("SEGMENT","구간 엔진의 고정 판정·위치·참고 근거 검증을 통과하지 못했습니다.");
        }
    }
    public static List<String> selectCaseIds(String engineVersion) {
        var ids=new ArrayList<String>();
        java.util.stream.IntStream.rangeClosed(1,CASE_COUNT).mapToObj(n->String.format(Locale.ROOT,"AG-%03d",n)).forEach(ids::add);
        if(AttachmentSegmentClassificationEngine.VERSION.equals(engineVersion))
            java.util.stream.IntStream.rangeClosed(1,AttachmentSegmentPolicyGoldenGate.CASE_COUNT).mapToObj(n->String.format(Locale.ROOT,"SG-%03d",n)).forEach(ids::add);
        else if(!AnnouncementAttachmentClassificationEngine.VERSION.equals(engineVersion)) throw new IllegalArgumentException("지원하지 않는 정책 분류 엔진입니다.");
        return List.copyOf(ids);
    }
    public static boolean selectContractCurrent(Result result,Configuration configuration) {
        if(result==null || configuration==null || !configuration.selectEngineCurrent()) return false;
        var ids=selectCaseIds(configuration.engineVersion());
        return configuration.engineVersion().equals(result.engineVersion()) && ids.equals(result.caseIds()) && ids.size()==result.caseCount()
                && (configuration.segmentRuleVersion()==null?SUITE_VERSION:AttachmentSegmentPolicyGoldenGate.SUITE_VERSION).equals(result.suiteVersion());
    }
    public Result selectValidatedResult(AnnouncementSourceClassificationRuleSet rules,String ruleSnapshotHash) {
        if(rules==null || rules.rules().isEmpty() || rules.rules().size()>2000 || ruleSnapshotHash==null || !ruleSnapshotHash.matches("[0-9a-f]{64}"))
            throw failure("INPUT","현재 규칙과 유효한 규칙 snapshot이 필요합니다.");
        var signatures=new TreeMap<String,String>();
        String activeCase="SETUP";
        try {
            var missing=selectBase(rules,"BIZINFO",TITLE,null);
            require("SETUP",missing.reasonCode()==ReasonCode.BODY_UNAVAILABLE,"기본 표본이 제목 조합을 통과하고 본문 미확보 상태여야 합니다.");
            var body=selectBase(rules,"BIZINFO",TITLE,"소상공인에게 지원금을 제공하는 사업입니다.");
            require("SETUP",body.semanticStatusCode()==SemanticStatusCode.ACCEPTED,"본문 조합 표본이 후보로 판정되어야 합니다.");
            var cases=new ArrayList<GoldenCase>();
            cases.add(caseOf(1,missing,rules,"NOTICE",COMBINATION,"ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED"));
            cases.add(caseOf(2,missing,rules,"GUIDE",COMBINATION,"ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED"));
            cases.add(caseOf(3,missing,rules,"FORM","소상공인 수출 지원금","REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED"));
            cases.add(caseOf(4,missing,rules,"REFERENCE","소상공인 수출 지원금","REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED"));
            cases.add(caseOf(5,missing,rules,"UNKNOWN",COMBINATION,"REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW"));
            cases.add(caseOf(6,missing,rules,"NOTICE","소상공인 수출 지원금","REVIEW_REQUIRED","ATTACHMENT_GROUP_B_MATCHED"));
            cases.add(caseOf(7,missing,rules,"NOTICE","소상공인 수출 특허 지원금","REVIEW_REQUIRED","ATTACHMENT_GROUP_B_MATCHED"));
            cases.add(caseOf(8,missing,rules,"NOTICE","소상공인 특허 지원금","REVIEW_REQUIRED","ATTACHMENT_GROUP_A_MATCHED"));
            cases.add(new GoldenCase(9,input(missing,rules,List.of(file(0,"NOTICE",COMBINATION,"COMPLETE_TEXT",false))),"REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW"));
            cases.add(caseOf(10,missing,rules,"NOTICE","소상공인 지원금 지원 대상에서 제외","REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW"));
            cases.add(new GoldenCase(11,input(missing,rules,List.of(file(0,"NOTICE","소상공인","COMPLETE_TEXT",true),file(1,"NOTICE","지원금","COMPLETE_TEXT",true))),"REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED"));
            var paragraphs=new FileInput(fileId(0),extractionId(0),"NOTICE","COMPLETE_TEXT","소상공인\n지원금",
                    List.of(new Block(0,0,4,"paragraph:0",true),new Block(1,5,8,"paragraph:1",true)),null);
            cases.add(new GoldenCase(12,input(missing,rules,List.of(paragraphs)),"REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED"));
            cases.add(new GoldenCase(13,input(missing,rules,List.of(file(0,"NOTICE",COMBINATION,"COMPLETE_TEXT",true),file(1,"NOTICE",COMBINATION,"PARTIAL_TEXT",true))),"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE"));
            cases.add(new GoldenCase(14,input(missing,rules,List.of(file(0,"NOTICE","","OCR_REQUIRED",true))),"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE"));
            cases.add(new GoldenCase(15,input(missing,rules,List.of(new FileInput(fileId(0),extractionId(0),"NOTICE","FAILED",null,List.of(),"NETWORK_TIMEOUT"))),"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE"));
            cases.add(new GoldenCase(16,new Input(missing,rules,false,"PENDING",false,List.of(),null,List.of()),"REVIEW_REQUIRED","ATTACHMENT_PENDING"));
            cases.add(new GoldenCase(17,new Input(body,rules,true,"NO_FILES",true,List.of(),null,List.of()),"ACCEPTED","TARGET_SUPPORT_CONFIRMED"));
            cases.add(new GoldenCase(18,new Input(body,rules,true,"FAILED",false,List.of(),null,List.of()),"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE"));
            var titleA=selectBase(rules,"BIZINFO","소상공인 특허 지원금",null);
            require("AG-019",titleA.titleStageCode()==TitleStageCode.GROUP_A_MATCHED,"제목 A는 검수를 유지해야 합니다.");
            cases.add(caseOf(19,titleA,rules,"NOTICE",COMBINATION,"REVIEW_REQUIRED","TITLE_GROUP_A_MATCHED"));
            var bodyB=selectBase(rules,"BIZINFO",TITLE,"소상공인 수출 지원금");
            require("AG-020",bodyB.bodyStageCode()==BodyStageCode.GROUP_B_MATCHED,"본문 B 검수 표본이 누락됐습니다.");
            cases.add(caseOf(20,bodyB,rules,"NOTICE",COMBINATION,"REVIEW_REQUIRED",bodyB.reasonCode().name()));
            cases.add(new GoldenCase(29,new Input(missing,rules,true,"NO_FILES",true,List.of(),null,List.of()),"REVIEW_REQUIRED","BODY_UNAVAILABLE"));
            cases.add(caseOf(30,missing,rules,"NOTICE","😀 소상공인 지원금","ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED"));
            for(var sample:cases) {
                activeCase=sample.id();var result=engine.selectDecision(sample.input());
                validateDecision(sample,result);signatures.put(sample.id(),hash(result));
            }
            activeCase="AG-021";
            var excluded=selectBase(rules,"BIZINFO","소상공인 수출 지원금",null);
            require(activeCase,excluded.semanticStatusCode()==SemanticStatusCode.EXCLUDED,"제목 B 제외 표본이 누락됐습니다.");
            validateRejected(activeCase,input(excluded,rules,List.of(file(0,"NOTICE",COMBINATION,"COMPLETE_TEXT",true))),"제목 수집 기준",signatures);
            activeCase="AG-022";
            var unmatched=selectBase(rules,"BIZINFO","행사 안내",null);
            require(activeCase,unmatched.semanticStatusCode()==SemanticStatusCode.EXCLUDED,"제목 조합 미충족 표본이 누락됐습니다.");
            validateRejected(activeCase,input(unmatched,rules,List.of(file(0,"NOTICE",COMBINATION,"COMPLETE_TEXT",true))),"제목 수집 기준",signatures);
            activeCase="AG-023";
            validateRejected(activeCase,input(missing,new AnnouncementSourceClassificationRuleSet(rules.releaseCode()+"-OTHER",rules.rules()),List.of()),"BASE_RECLASSIFICATION_REQUIRED",signatures);
            activeCase="AG-024";
            var badOffset=new FileInput(fileId(0),extractionId(0),"NOTICE","COMPLETE_TEXT","😀지원금",List.of(new Block(0,0,5,"p",true)),null);
            validateRejected(activeCase,input(missing,rules,List.of(badOffset)),"근거 위치",signatures);
            activeCase="AG-025";
            var exact=new AnnouncementSourceClassificationRuleSet("EXACT-CONTROL",List.of(new AnnouncementSourceClassificationRule("EXACT-B","EXACT-B",RuleGroupKindCode.AUTO_EXCLUDE_B,
                    "수출",StrengthCode.STRONG,null,null,List.of(AnnouncementSourceClassificationTerm.canonical("수출",MatchModeCode.EXACT_TITLE)),true)));
            require(activeCase,baseEngine.selectAttachmentScope("수출",null,List.of(),exact).matches().isEmpty(),"제목 전용 규칙을 첨부에 적용하면 안 됩니다.");
            signatures.put(activeCase,hash("EXACT_TITLE_NOT_APPLIED"));
            activeCase="AG-026";
            for(String provider:List.of("BIZINFO","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE")) {
                var result=engine.selectDecision(input(selectBase(rules,provider,TITLE,null),rules,List.of(file(0,"NOTICE",COMBINATION,"COMPLETE_TEXT",true))));
                require(activeCase,signatures.get("AG-001").equals(hash(result)),"같은 근거의 판정이 Provider에 따라 바뀌면 안 됩니다.");
            }
            signatures.put(activeCase,hash("PROVIDER_INVARIANT"));
            activeCase="AG-027";
            validateRejected(activeCase,new Input(missing,rules,true,"NO_FILES",true,List.of(file(0,"NOTICE",COMBINATION,"COMPLETE_TEXT",true)),null,List.of()),"첨부 없음",signatures);
            activeCase="AG-028";
            validateRejected(activeCase,input(missing,rules,List.of()),"파일 근거",signatures);
            require("SUMMARY",signatures.size()==CASE_COUNT,"필수 분류 정답 세트가 모두 실행되어야 합니다.");
            String contentHash=hash(rules);
            return new Result(SUITE_VERSION,AnnouncementAttachmentClassificationEngine.VERSION,rules.releaseCode(),ruleSnapshotHash,contentHash,
                    hash(List.of(SUITE_VERSION,AnnouncementAttachmentClassificationEngine.VERSION,ruleSnapshotHash,contentHash,signatures)),CASE_COUNT,List.copyOf(signatures.keySet()));
        } catch(ApiException exception) { throw exception; }
        catch(RuntimeException exception) { throw failure(activeCase,"서버 분류 검증을 완료하지 못했습니다. 규칙과 엔진을 확인하세요."); }
    }
    private void validateDecision(GoldenCase sample,Decision result) {
        String id=sample.id();require(id,result!=null && sample.status().equals(result.status()) && sample.reason().equals(result.reason()),"필수 판정 또는 검수 사유가 일치하지 않습니다.");
        require(id,!"EXCLUDED".equals(result.status()),"첨부 판정은 제목 자동 제외가 아닙니다.");
        if("ACCEPTED".equals(result.status())) require(id,result.targetCodes().contains("BUSINESS") && !result.supportCodes().isEmpty(),"후보의 대상·지원형태가 누락됐습니다.");
        if(sample.number()==3 || sample.number()==4) require(id,result.matches().stream().allMatch(m->"CONTEXT_ONLY".equals(m.action())),"양식과 참고자료는 참고 근거여야 합니다.");
        if(sample.number()==7) require(id,result.matches().stream().anyMatch(m->m.keyword().groupKindCode()==RuleGroupKindCode.REVIEW_A)
                && result.matches().stream().anyMatch(m->m.keyword().groupKindCode()==RuleGroupKindCode.AUTO_EXCLUDE_B),"첨부 A/B 양쪽 근거를 보존해야 합니다.");
        for(var match:result.matches()) {
            var source=sample.input().files().stream().filter(file->file.fileId().equals(match.fileId()) && file.extractionId().equals(match.extractionId())).findFirst().orElseThrow();
            var block=source.blocks().stream().filter(value->value.index()==match.blockIndex()).findFirst().orElseThrow();
            require(id,match.startOffset()>=block.startOffset() && match.endOffset()>match.startOffset() && match.endOffset()<=block.endOffset(),"근거 code point 위치가 입력 범위를 벗어났습니다.");
            String term=source.text().substring(source.text().offsetByCodePoints(0,match.startOffset()),source.text().offsetByCodePoints(0,match.endOffset()));
            require(id,term.equals(match.keyword().matchedTerm()),"근거 위치의 실제 문구가 일치하지 않습니다.");
        }
    }
    private void validateRejected(String id,Input input,String expectedMessage,Map<String,String> signatures) {
        try { engine.selectDecision(input); }
        catch(IllegalArgumentException expected) {
            require(id,expected.getMessage()!=null && expected.getMessage().contains(expectedMessage),"잘못된 입력이 다른 사유로 실패했습니다.");
            signatures.put(id,hash(expectedMessage));return;
        }
        throw failure(id,"잘못된 제목·버전·근거 입력이 허용됐습니다.");
    }
    private AnnouncementSourceClassificationResult selectBase(AnnouncementSourceClassificationRuleSet rules,String provider,String title,String body) {
        return baseEngine.selectDecision(new AnnouncementSourceClassificationInput(provider,title,body,null,List.of(),
                body==null?BodySourceCode.NONE:BodySourceCode.PROVIDER_FULL_TEXT,body==null?BodyAvailabilityCode.UNAVAILABLE:BodyAvailabilityCode.AVAILABLE),rules);
    }
    private static GoldenCase caseOf(int number,AnnouncementSourceClassificationResult base,AnnouncementSourceClassificationRuleSet rules,String role,String text,String status,String reason) {
        return new GoldenCase(number,input(base,rules,List.of(file(0,role,text,"COMPLETE_TEXT",true))),status,reason);
    }
    private static Input input(AnnouncementSourceClassificationResult base,AnnouncementSourceClassificationRuleSet rules,List<FileInput> files) {
        return new Input(base,rules,true,"FOUND",true,files,null,List.of());
    }
    private static FileInput file(int number,String role,String text,String quality,boolean reliable) {
        return new FileInput(fileId(number),extractionId(number),role,quality,text,text.isEmpty()?List.of():List.of(new Block(0,0,text.codePointCount(0,text.length()),"paragraph:0",reliable)),null);
    }
    private static UUID fileId(int number) { return UUID.nameUUIDFromBytes(("golden-file-"+number).getBytes(StandardCharsets.UTF_8)); }
    private static UUID extractionId(int number) { return UUID.nameUUIDFromBytes(("golden-extraction-"+number).getBytes(StandardCharsets.UTF_8)); }
    private String hash(Object object) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(object))); }
        catch(Exception exception) { throw failure("HASH","분류 검증 지문을 생성하지 못했습니다."); }
    }
    private void require(String id,boolean condition,String message) { if(!condition) throw failure(id,message); }
    private ApiException failure(String id,String message) {
        return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED,HttpStatus.CONFLICT,"첨부 정책 분류 검증 "+id+": "+message);
    }
    private record GoldenCase(int number,Input input,String status,String reason) {
        String id() { return String.format(Locale.ROOT,"AG-%03d",number); }
    }
}
