package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase.*;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaPlanResponse.Item;
import com.saneb.domain.announcementattachment.service.impl.AttachmentProviderQaPlan;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** 서버 배포 catalog만 입력으로 사용한다. metadata 계획은 실행/추출/정책 성공 증거가 아니다. */
@Component
public final class AttachmentProviderQaCatalog {
    private final ObjectMapper mapper;
    private final AttachmentDiscoveryProfileRegistry registry;
    private final Definition definition;
    public record Definition(int schemaVersion,String catalogVersion,List<Notice> notices) {
        public Definition {notices=List.copyOf(notices);}
        @Override public String toString(){return "ProviderQaCatalog[definition=REDACTED]";}
    }
    public record Notice(String caseCode,String profileCode,AttachmentDiscoveryProfile.Source source,Expectation expectation) {
        @Override public String toString(){return "ProviderQaNotice[input=REDACTED]";}
    }
    public record Expectation(String profileHash,String title,Instant observedAt,String discoveryStatus,boolean discoveryComplete,List<ExpectedFile> files,Limits limits) {
        public Expectation {files=List.copyOf(files);}
        @Override public String toString(){return "ProviderQaExpectation[input=REDACTED]";}
    }
    public record CasePlan(String caseCode,String targetKey,String statusCode,String inputHash,Integer expectedFileCount,boolean normalNotice,List<String> formats) {
        public CasePlan {formats=List.copyOf(formats);}
    }
    public record TargetPlan(String targetKey,String bindingStatusCode,int referenceCount,int executableCount,int normalNoticeCount,int requiredNormalNoticeCount,
                             List<String> missingFormats,boolean isExpectationCoverageComplete) {
        public TargetPlan {missingFormats=List.copyOf(missingFormats);}
    }
    public record Segment(int ordinal,List<String> caseCodes,long maximumRequests,long maximumBytes,long maximumSecondsIncludingMargin) {
        public Segment {caseCodes=List.copyOf(caseCodes);}
    }
    public record Plan(String catalogVersion,String catalogHash,String scopeHash,List<TargetPlan> targets,List<CasePlan> cases,List<Segment> segments,
                       int executableCount,boolean isExpectationCoverageComplete,boolean isQaPassed) {
        public Plan {targets=List.copyOf(targets);cases=List.copyOf(cases);segments=List.copyOf(segments);}
    }
    public record Prepared(Plan plan,List<AttachmentProviderQaCase> inputs) {
        public Prepared {inputs=List.copyOf(inputs);}
        @Override public String toString(){return "ProviderQaPrepared[inputs=REDACTED]";}
    }
    @org.springframework.beans.factory.annotation.Autowired
    public AttachmentProviderQaCatalog(ObjectMapper mapper,AttachmentDiscoveryProfileRegistry registry) {
        this(mapper,registry,read(mapper));
    }
    AttachmentProviderQaCatalog(ObjectMapper mapper,AttachmentDiscoveryProfileRegistry registry,Definition definition) {
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.registry=registry;this.definition=definition;validateDefinition();
    }
    private static Definition read(ObjectMapper mapper) {
        try(var input=new ClassPathResource("announcement-attachment/provider-qa-catalog-v1.json").getInputStream()) {
            byte[] bytes=input.readNBytes(2097153);if(bytes.length>2097152)throw invalid("CATALOG_SIZE");
            return mapper.copy().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(bytes,Definition.class);
        }catch(Exception failure){throw invalid("CATALOG_RESOURCE_INVALID");}
    }
    private void validateDefinition() {
        if(definition==null || definition.schemaVersion()!=1 || definition.catalogVersion()==null || !definition.catalogVersion().matches("[A-Za-z0-9_.-]{1,60}")
                || definition.notices().size()>10000)throw invalid("CATALOG_DEFINITION_INVALID");
        var codes=new HashSet<String>();var identities=new HashSet<String>();
        for(var notice:definition.notices()) {
            if(notice==null || !code(notice.caseCode()) || !code(notice.profileCode()) || !codes.add(notice.caseCode()) || notice.source()==null)throw invalid("CATALOG_NOTICE_INVALID");
            var source=notice.source();
            if(source.providerCode()==null || !Set.of("BIZINFO","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE").contains(source.providerCode()) || source.providerNoticeId()==null
                    || source.providerNoticeId().isBlank() || source.providerNoticeId().length()>200 || source.providerNoticeId().codePoints().anyMatch(Character::isISOControl)
                    || source.sourceUrl()==null || source.sourceUrl().length()>2048
                    || source.sourceUrl().codePoints().anyMatch(Character::isISOControl))throw invalid("CATALOG_SOURCE_INVALID");
            try {
                var uri=java.net.URI.create(source.sourceUrl());
                if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null || !Set.of("https","http").contains(uri.getScheme()))throw invalid("CATALOG_SOURCE_INVALID");
            }catch(RuntimeException failure){throw invalid("CATALOG_SOURCE_INVALID");}
            if("LOCAL_GOV_NOTICE".equals(source.providerCode()) ? !code(source.localSourceCode()) || !code(source.listParserProfileCode())
                    : source.localSourceCode()!=null || source.listParserProfileCode()!=null)throw invalid("CATALOG_SOURCE_INVALID");
            if(!identities.add(target(source)+":"+source.providerNoticeId()))throw invalid("CATALOG_DUPLICATE_NOTICE");
        }
    }
    /** 규칙/runtime은 예약 coordinator가 현재 설치·DB snapshot에서 검증한 값을 전달해야 한다. 이 함수는 HTTP/DB를 호출하지 않는다. */
    public Prepared selectPrepared(AttachmentProviderQaPlan.Plan scope,AnnouncementSourceClassificationRuleSet rules,String runtimeHash,Instant observedNow) {
        if(scope==null || scope.schemaVersion()!=1 || scope.items().size()<2 || scope.items().size()>1002 || rules==null || rules.rules().isEmpty()
                || rules.rules().size()>2000 || !hash(runtimeHash) || observedNow==null)throw invalid("CATALOG_CONTEXT_INVALID");
        var required=new LinkedHashMap<String,Item>();
        if(scope.summary()==null || scope.summary().targetCount()!=scope.items().size())throw invalid("CATALOG_CONTEXT_INVALID");
        for(var item:scope.items()) {
            if(item==null || item.providerCode()==null || !Set.of("BIZINFO","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE").contains(item.providerCode())
                    || item.minimumNormalNoticeCount()!=3 || item.requiredFormats().size()!=3 || !new HashSet<>(item.requiredFormats()).equals(Set.of("PDF","HWP","HWPX")))throw invalid("CATALOG_SCOPE_INCOMPLETE");
            if(required.put(target(item),item)!=null)throw invalid("CATALOG_SCOPE_DUPLICATE");
        }
        if(!required.containsKey("BIZINFO") || !required.containsKey("GOV24_PUBLIC_SERVICE"))throw invalid("CATALOG_SCOPE_INCOMPLETE");
        var cases=new ArrayList<CasePlan>();var inputs=new ArrayList<AttachmentProviderQaCase>();var details=new HashSet<String>();
        for(var notice:definition.notices().stream().sorted(Comparator.comparing(Notice::caseCode)).toList()) {
            String key=target(notice.source());Item target=required.get(key);
            Integer expectedFiles=notice.expectation()==null?null:notice.expectation().files().size();
            if(target==null){cases.add(new CasePlan(notice.caseCode(),key,"TARGET_OUTSIDE_SCOPE",null,expectedFiles,false,List.of()));continue;}
            String state=selectState(notice,target,observedNow);AttachmentProviderQaCase input=null;
            if("EXPECTED_INPUT_READY".equals(state)) {
                var e=notice.expectation();input=new AttachmentProviderQaCase(notice.caseCode(),notice.profileCode(),e.profileHash(),notice.source(),e.title(),rules,runtimeHash,
                        e.discoveryStatus(),e.discoveryComplete(),e.files(),e.limits());
                try {AttachmentProviderQaCaseContract.validate(input);}catch(IllegalArgumentException failure){state="EXPECTATION_INVALID";input=null;}
                if(input!=null) {
                    var profile=registry.selectProfileDetails(input.source().providerCode(),input.profileCode(),input.profileHash()).orElseThrow(()->invalid("CATALOG_PROFILE_CHANGED"));
                    if(!details.add(key+":"+profile.selectDetailUri(input.source()).normalize().toASCIIString())){state="DUPLICATE_DETAIL";input=null;}
                }
                if(input!=null) {
                    var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                            input.source().providerCode(),input.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
                    boolean eligible=decision.semanticStatusCode()!=SemanticStatusCode.EXCLUDED
                            && Set.of(TitleStageCode.GROUP_A_MATCHED,TitleStageCode.COMBINATION_MATCHED).contains(decision.titleStageCode());
                    if(eligible=="TITLE_BLOCKED".equals(input.discoveryStatus())){state="TITLE_EXPECTATION_CHANGED";input=null;}
                }
            }
            boolean normal=input!=null && "FOUND".equals(input.discoveryStatus()) && input.discoveryComplete()
                    && input.files().stream().allMatch(f->f.downloadAllowed() && "COMPLETE_TEXT".equals(f.quality()));
            List<String> formats=input==null?List.of():input.files().stream().filter(f->f.downloadAllowed() && "COMPLETE_TEXT".equals(f.quality())).map(ExpectedFile::format).distinct().sorted().toList();
            cases.add(new CasePlan(notice.caseCode(),key,state,input==null?null:selectHash(input),expectedFiles,normal,formats));
            if(input!=null)inputs.add(input);
        }
        var targets=new ArrayList<TargetPlan>();
        for(var entry:required.entrySet()) {
            var found=cases.stream().filter(c->c.targetKey().equals(entry.getKey())).toList();var expected=found.stream().filter(c->"EXPECTED_INPUT_READY".equals(c.statusCode())).toList();
            var formats=expected.stream().flatMap(c->c.formats().stream()).collect(java.util.stream.Collectors.toSet());
            var missing=entry.getValue().requiredFormats().stream().filter(f->!formats.contains(f)).sorted().toList();
            int normal=(int)expected.stream().filter(CasePlan::normalNotice).count();
            targets.add(new TargetPlan(entry.getKey(),entry.getValue().statusCode(),found.size(),expected.size(),normal,entry.getValue().minimumNormalNoticeCount(),missing,
                    "SYSTEM_BINDING_MATCHED".equals(entry.getValue().statusCode()) && !found.isEmpty() && expected.size()==found.size()
                            && normal>=entry.getValue().minimumNormalNoticeCount() && missing.isEmpty()));
        }
        var plan=new Plan(definition.catalogVersion(),selectHash(definition),selectHash(scope),targets,cases,segments(inputs),inputs.size(),
                targets.stream().allMatch(TargetPlan::isExpectationCoverageComplete) && cases.stream().noneMatch(c->"TARGET_OUTSIDE_SCOPE".equals(c.statusCode())),false);
        return new Prepared(plan,inputs);
    }
    private String selectState(Notice notice,Item target,Instant now) {
        if(!"SYSTEM_BINDING_MATCHED".equals(target.statusCode()) || target.profiles().size()!=1
                || !Objects.equals(notice.source().listParserProfileCode(),target.listParserProfileCode()))return "TARGET_BINDING_UNAVAILABLE";
        var bound=target.profiles().getFirst();
        if(!bound.profileCode().equals(notice.profileCode()))return "PROFILE_CHANGED";
        var selected=registry.selectProfileDetails(bound.providerCode(),bound.profileCode(),bound.profileHash());
        if(selected.isEmpty())return "PROFILE_CHANGED";
        var profile=selected.get();
        try {if(!profile.selectApprovedRequest(profile.selectDetailUri(notice.source())))return "SOURCE_BINDING_INVALID";}
        catch(RuntimeException failure){return "SOURCE_BINDING_INVALID";}
        var expectation=notice.expectation();if(expectation==null)return "REFERENCE_ONLY";
        if(!bound.profileHash().equals(expectation.profileHash()))return "PROFILE_CHANGED";
        if(expectation.observedAt()==null || expectation.observedAt().isAfter(now) || expectation.observedAt().isBefore(now.minus(Duration.ofDays(7))))return "OBSERVATION_EXPIRED";
        return "EXPECTED_INPUT_READY";
    }
    private List<Segment> segments(List<AttachmentProviderQaCase> inputs) {
        var result=new ArrayList<Segment>();var codes=new ArrayList<String>();long requests=0,bytes=0,seconds=0;
        for(var input:inputs) {
            long next=input.limits().maximumSeconds()+60L;
            if(!codes.isEmpty() && seconds+next>82800) {
                result.add(new Segment(result.size()+1,codes,requests,bytes,seconds));codes=new ArrayList<>();requests=0;bytes=0;seconds=0;
            }
            codes.add(input.caseId());requests=Math.addExact(requests,input.limits().maximumRequestReservations());bytes=Math.addExact(bytes,input.limits().maximumReservedBytes());seconds=Math.addExact(seconds,next);
        }
        if(!codes.isEmpty())result.add(new Segment(result.size()+1,codes,requests,bytes,seconds));return List.copyOf(result);
    }
    private String selectHash(Object value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(mapper.convertValue(value,Object.class))));}
        catch(Exception failure){throw invalid("CATALOG_HASH_INVALID");}
    }
    private static String target(AttachmentDiscoveryProfile.Source s){return "LOCAL_GOV_NOTICE".equals(s.providerCode())?s.providerCode()+":"+s.localSourceCode():s.providerCode();}
    private static String target(Item i){return "LOCAL_GOV_NOTICE".equals(i.providerCode())?i.providerCode()+":"+i.localSourceCode():i.providerCode();}
    private static boolean code(String v){return v!=null && v.matches("[A-Z0-9_-]{1,100}");}
    private static boolean hash(String v){return v!=null && v.matches("[0-9a-f]{64}");}
    private static IllegalArgumentException invalid(String code){return new IllegalArgumentException(code);}
}
