package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.domain.announcementattachment.classification.*;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.*;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 구간 엔진의 고정 정답 계약. 구 엔진 30건과 함께 실행하며 실제 Provider/파일 QA를 대체하지 않는다. */
final class AttachmentSegmentPolicyGoldenGate {
    static final String SUITE_VERSION="attachment-segment-golden-1.0.0";
    static final int CASE_COUNT=22;
    private static final String GUIDE="사업 지원 안내\n지원대상: 소상공인 지원금\n지원내용: 경영지원\n신청기간: 9월";
    // '지원대상: 소상공인'에는 실제 seed의 보조 지원어 '지원'이 같은 문단에 있다.
    // 문단/파일/구간 분리 음성 표본은 지원어가 없는 자격 표제로 대상과 형태를 분리한다.
    private static final String TARGET_ONLY_GUIDE=GUIDE.replace("지원대상: 소상공인 지원금","신청자격: 소상공인");
    private static final String SUPPORT_ONLY_GUIDE=GUIDE.replace("지원대상: 소상공인 지원금","신청자격: 해당 조건 확인").replace("경영지원","지원금");
    private static final String FORM="지원 신청서\n성 명\n(서명 또는 인)\n수출 특허 지원금";
    private final AttachmentSegmentClassificationEngine engine;
    private final String segmentVersion, segmentHash;
    static final class Failure extends IllegalStateException {
        private final String caseId;
        Failure(String caseId) {super("구간 정답·위치·참고 근거 계약이 일치하지 않습니다.");this.caseId=caseId;}
        String selectCaseId(){return caseId;}
    }
    AttachmentSegmentPolicyGoldenGate() {this(new AttachmentSegmentClassificationEngine());}
    AttachmentSegmentPolicyGoldenGate(AttachmentSegmentClassificationEngine engine) {
        this(engine,AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH);
    }
    private AttachmentSegmentPolicyGoldenGate(AttachmentSegmentClassificationEngine engine,String version,String hash) {
        if(!AttachmentEngineContract.selectSegmentCurrent(version,hash))throw new Failure("VERSION");
        this.engine=engine;this.segmentVersion=version;this.segmentHash=hash;
    }
    record Fixture(FileInput input,AttachmentSegmentClassificationEngine.FileEvidence evidence) { }
    record Sample(int number,Input input,List<Fixture> files,String status,String reason) {
        String id(){return String.format(Locale.ROOT,"SG-%03d",number);}
    }
    Map<String,Object> selectValidatedSignatures(AnnouncementSourceClassificationRuleSet rules) {
        var missing=base(rules,"BIZINFO","소상공인 지원금",null);
        var samples=new ArrayList<Sample>();
        samples.add(sample(1,missing,rules,"ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED",file(0,"😀 "+GUIDE+"\n"+FORM)));
        String notice=GUIDE.replace("사업 지원 안내","지원사업 공고");
        boolean structural=AttachmentSegmentRoleAnalyzer.STRUCTURAL_VERSION.equals(segmentVersion);
        if(AttachmentSegmentRoleAnalyzer.QUARTER_VERSION.equals(segmentVersion) || structural)
            notice=notice.replace("지원사업 공고","지원사업 모집 공고(3분기)")
                    .replace("지원대상:","❍ (지원대상)").replace("지원내용:","❍ (지원내용)").replace("신청기간:","❍ (신청기간)");
        // 새 버전만 내부 절·중복 표제를 포함한다. 기존 버전의 정답 지문은 바꾸지 않는다.
        String noticeAndForm=structural?notice+"\n3. 신청안내\n접수 방법을 확인하세요.\n지원 신청서\n"+FORM:notice+"\n"+FORM;
        samples.add(sample(2,missing,rules,"ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED",file(0,noticeAndForm)));
        samples.add(sample(3,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW",file(0,"미확인 제한 조건\n"+GUIDE)));
        samples.add(sample(4,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW",file(0,GUIDE,"UNKNOWN","UNKNOWN","COMPLETE_TEXT",false)));
        samples.add(sample(5,missing,rules,"REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED",file(0,TARGET_ONLY_GUIDE.replace("경영지원","지원금"))));
        samples.add(sample(6,missing,rules,"REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED",file(0,TARGET_ONLY_GUIDE),file(1,SUPPORT_ONLY_GUIDE)));
        samples.add(sample(7,missing,rules,"REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED",file(0,TARGET_ONLY_GUIDE+"\n"+SUPPORT_ONLY_GUIDE)));
        samples.add(sample(8,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE",file(0,GUIDE,"UNKNOWN","UNKNOWN","PARTIAL_TEXT",true)));
        samples.add(sample(9,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE",file(0,GUIDE),failed(1)));
        samples.add(sample(10,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW",file(0,GUIDE+"\n"+FORM,"GUIDE","MANUAL","COMPLETE_TEXT",true)));
        samples.add(sample(11,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW",file(0,GUIDE+"\n"+FORM,"GUIDE","PROFILE","COMPLETE_TEXT",true)));
        samples.add(sample(12,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_GROUP_A_MATCHED",file(0,GUIDE+"\n특허")));
        samples.add(sample(13,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_GROUP_B_MATCHED",file(0,GUIDE+"\n수출")));
        samples.add(sample(14,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_GROUP_B_MATCHED",file(0,GUIDE+"\n특허 수출")));
        samples.add(sample(15,missing,rules,"REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW",file(0,GUIDE+"\n지원 대상에서 제외")));
        samples.add(sample(16,missing,rules,"REVIEW_REQUIRED","EXTENDED_COMBINATION_NOT_CONFIRMED",file(0,FORM)));
        var bodyB=base(rules,"BIZINFO","소상공인 지원금","소상공인 수출 지원금");
        require("SG-017",bodyB.bodyStageCode()==BodyStageCode.GROUP_B_MATCHED);
        samples.add(sample(17,bodyB,rules,"REVIEW_REQUIRED",bodyB.reasonCode().name(),file(0,GUIDE)));
        var titleA=base(rules,"BIZINFO","소상공인 특허 지원금",null);
        require("SG-018",titleA.titleStageCode()==TitleStageCode.GROUP_A_MATCHED);
        samples.add(sample(18,titleA,rules,"REVIEW_REQUIRED","TITLE_GROUP_A_MATCHED",file(0,GUIDE)));
        var signatures=new TreeMap<String,Object>();
        for(var sample:samples) {
            var evidence=sample.files().stream().map(Fixture::evidence).toList();
            var result=engine.selectDecision(sample.input(),evidence);
            validate(sample,result);
            // 원문은 결과에 반환하지 않는다. 호출자가 직렬화한 전체 정답 근거의 지문만 저장한다.
            signatures.put(sample.id(),List.of(sample.input(),evidence,result));
        }
        var excluded=base(rules,"BIZINFO","소상공인 수출 지원금",null);
        require("SG-019",excluded.semanticStatusCode()==SemanticStatusCode.EXCLUDED);
        var fixture=file(0,GUIDE);
        reject("SG-019",()->engine.selectDecision(input(excluded,rules,List.of(fixture)),List.of(fixture.evidence())));
        signatures.put("SG-019","TITLE_STOP_PRESERVED");
        var extraction=fixture.evidence().extraction();
        var altered=new AttachmentSetEvidence.Extraction(extraction.quality(),extraction.text()+"변조",extraction.blocks(),1,0);
        var forged=new AttachmentSegmentClassificationEngine.FileEvidence(fixture.input().fileId(),fixture.input().extractionId(),"UNKNOWN",altered,fixture.evidence().analysis());
        reject("SG-020",()->engine.selectDecision(input(missing,rules,List.of(fixture)),List.of(forged)));
        signatures.put("SG-020","ALTERED_TEXT_REJECTED");
        AttachmentSegmentClassificationEngine.Result prior=null;
        for(String provider:List.of("BIZINFO","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE")) {
            var result=engine.selectDecision(input(base(rules,provider,"소상공인 지원금",null),rules,List.of(fixture)),List.of(fixture.evidence()));
            require("SG-021",prior==null || prior.equals(result));prior=result;
        }
        signatures.put("SG-021",Objects.requireNonNull(prior));
        reject("SG-022",()->engine.selectDecision(input(missing,rules,List.of(fixture,fixture)),List.of(fixture.evidence(),fixture.evidence())));
        signatures.put("SG-022","DUPLICATE_FILE_REJECTED");
        require("SUMMARY",signatures.size()==CASE_COUNT);
        return signatures;
    }
    Map<String,Object> selectValidatedSignatures(AnnouncementSourceClassificationRuleSet rules,String version,String hash) {
        // 요청마다 불변 인스턴스를 사용하므로 서로 다른 정책의 병렬 검증이 섞이지 않는다.
        return new AttachmentSegmentPolicyGoldenGate(engine,version,hash).selectValidatedSignatures(rules);
    }
    private void validate(Sample sample,AttachmentSegmentClassificationEngine.Result result) {
        require(sample.id(),result!=null && AttachmentSegmentClassificationEngine.VERSION.equals(result.engineVersion())
                && sample.status().equals(result.decision().status()) && sample.reason().equals(result.decision().reason()));
        require(sample.id(),result.segmentMatches().size()==result.decision().matches().size());
        if("ACCEPTED".equals(result.decision().status())) require(sample.id(),result.decision().targetCodes().contains("BUSINESS") && !result.decision().supportCodes().isEmpty());
        if(sample.number()==10 || sample.number()==11) require(sample.id(),result.decision().warnings().contains("ATTACHMENT_SEGMENT_ROLE_CONFLICT"));
        if(sample.number()==1 || sample.number()==2 || sample.number()==16) require(sample.id(),result.segmentMatches().stream().anyMatch(m->"FORM".equals(m.segmentRole())));
        for(var match:result.segmentMatches()) {
            var file=sample.files().stream().filter(f->f.input().fileId().equals(match.match().fileId())).findFirst().orElseThrow();
            require(sample.id(),file.input().extractionId().equals(match.match().extractionId()) && match.segmentIndex()!=null);
            var segment=file.evidence().analysis().segments().get(match.segmentIndex());
            var block=file.input().blocks().stream().filter(b->b.index()==match.match().blockIndex()).findFirst().orElseThrow();
            require(sample.id(),segment.roleCode().equals(match.segmentRole()) && match.match().startOffset()>=Math.max(segment.startOffset(),block.startOffset())
                    && match.match().endOffset()<=Math.min(segment.endOffset(),block.endOffset()) && match.match().endOffset()>match.match().startOffset());
            String text=file.input().text();
            require(sample.id(),text.substring(text.offsetByCodePoints(0,match.match().startOffset()),text.offsetByCodePoints(0,match.match().endOffset())).equals(match.match().keyword().matchedTerm()));
            if(!Set.of("NOTICE","GUIDE").contains(match.segmentRole())) require(sample.id(),"CONTEXT_ONLY".equals(match.match().action()));
        }
    }
    private Fixture file(int n,String text) {return file(n,text,"UNKNOWN","UNKNOWN","COMPLETE_TEXT",true);}
    private Fixture file(int n,String text,String role,String origin,String quality,boolean reliable) {
        var blocks=new ArrayList<AttachmentSetEvidence.Block>();int offset=0;
        for(String line:text.split("\n")) {
            int end=offset+line.codePointCount(0,line.length());
            blocks.add(new AttachmentSetEvidence.Block(blocks.size(),offset,end,"p:"+blocks.size(),reliable,"p:"+blocks.size()));offset=end+1;
        }
        var extraction=new AttachmentSetEvidence.Extraction(quality,text,blocks,1,0);
        var file=new FileInput(id("file",n),id("extraction",n),role,quality,text,blocks.stream().map(b->new Block(b.index(),b.startOffset(),b.endOffset(),b.evidenceScopeId(),b.scopeReliable())).toList(),null);
        return new Fixture(file,new AttachmentSegmentClassificationEngine.FileEvidence(file.fileId(),file.extractionId(),origin,extraction,
                new AttachmentSegmentRoleAnalyzer().selectAnalysis(extraction,segmentVersion,segmentHash)));
    }
    private static Fixture failed(int n) {
        var file=new FileInput(id("file",n),null,"UNKNOWN","FAILED",null,List.of(),"NETWORK_TIMEOUT");
        return new Fixture(file,new AttachmentSegmentClassificationEngine.FileEvidence(file.fileId(),null,"UNKNOWN",null,null));
    }
    private static Sample sample(int n,AnnouncementSourceClassificationResult base,AnnouncementSourceClassificationRuleSet rules,String status,String reason,Fixture... files) {
        return new Sample(n,input(base,rules,List.of(files)),List.of(files),status,reason);
    }
    private static Input input(AnnouncementSourceClassificationResult base,AnnouncementSourceClassificationRuleSet rules,List<Fixture> files) {
        return new Input(base,rules,true,"FOUND",true,files.stream().map(Fixture::input).toList(),null,List.of());
    }
    private static AnnouncementSourceClassificationResult base(AnnouncementSourceClassificationRuleSet rules,String provider,String title,String body) {
        return new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(provider,title,body,null,List.of(),
                body==null?BodySourceCode.NONE:BodySourceCode.PROVIDER_FULL_TEXT,body==null?BodyAvailabilityCode.UNAVAILABLE:BodyAvailabilityCode.AVAILABLE),rules);
    }
    private static UUID id(String type,int number){return UUID.nameUUIDFromBytes(("segment-golden-"+type+number).getBytes(StandardCharsets.UTF_8));}
    private static void reject(String id,Runnable work){try{work.run();}catch(IllegalArgumentException expected){return;}throw new Failure(id);}
    private static void require(String id,boolean valid){if(!valid)throw new Failure(id);}
}
