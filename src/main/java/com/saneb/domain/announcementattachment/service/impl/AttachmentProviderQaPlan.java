package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaPlanResponse.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Target;
import java.util.*;

/** 전체 대상의 구조적 실행 요구 목록. 표본/URL/다운로드/추출 성공을 선언하거나 일부 출처를 분모에서 빼지 않는다. */
public final class AttachmentProviderQaPlan {
    private AttachmentProviderQaPlan() { }
    public record Plan(int schemaVersion,Summary summary,List<Item> items,List<Profile> unboundProfiles) {
        public Plan {items=List.copyOf(items);unboundProfiles=List.copyOf(unboundProfiles);}
    }

    public static Plan selectPlan(List<AttachmentDiscoveryProfile> profiles,List<Target> targets) {
        if(profiles==null || targets==null || profiles.size()>1000 || targets.size()>1000) throw invalid();
        var identities=new HashSet<String>();var sourceIds=new HashSet<UUID>();var sourceCodes=new HashSet<String>();
        for(var profile:profiles) {
            if(profile==null || !Set.of("BIZINFO","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE").contains(profile.selectProviderCode())
                    || !safe(profile.selectProfileCode()) || profile.selectProfileHash()==null || !profile.selectProfileHash().matches("[0-9a-f]{64}")
                    || !identities.add(profile.selectProviderCode()+":"+profile.selectProfileCode()) || profile.selectSourceBindings()==null
                    || profile.selectSourceBindings().size()>1000) throw invalid();
            var bindings=new HashSet<AttachmentDiscoveryProfile.SourceBinding>();
            for(var binding:profile.selectSourceBindings()) {
                if(binding==null || !bindings.add(binding)) throw invalid();
                if("LOCAL_GOV_NOTICE".equals(profile.selectProviderCode())) {
                    if(!safe(binding.localSourceCode()) || !safe(binding.listParserProfileCode())) throw invalid();
                } else if(binding.localSourceCode()!=null || binding.listParserProfileCode()!=null) throw invalid();
            }
        }
        for(var target:targets) {
            if(target==null || target.sourceId()==null || !safe(target.publicCode()) || !safe(target.parserProfileCode())
                    || !sourceIds.add(target.sourceId()) || !sourceCodes.add(target.publicCode())) throw invalid();
        }
        var items=new ArrayList<Item>();var used=new HashSet<Profile>();
        items.add(selectItem("BIZINFO",null,null,null,profiles,used));
        items.add(selectItem("GOV24_PUBLIC_SERVICE",null,null,null,profiles,used));
        targets.stream().sorted(Comparator.comparing(Target::publicCode)).forEach(target->
                items.add(selectItem("LOCAL_GOV_NOTICE",target.sourceId(),target.publicCode(),target.parserProfileCode(),profiles,used)));
        List<Profile> unbound=profiles.stream().map(AttachmentProviderQaPlan::selectProfile).filter(p->!used.contains(p))
                .sorted(Comparator.comparing(Profile::providerCode).thenComparing(Profile::profileCode)).toList();
        var counts=new HashMap<String,Integer>();items.forEach(i->counts.merge(i.statusCode(),1,Integer::sum));
        return new Plan(1,new Summary(items.size(),counts.getOrDefault("SYSTEM_BINDING_MATCHED",0),counts.getOrDefault("PROFILE_MISSING",0),
                counts.getOrDefault("LIST_PARSER_MISMATCH",0),counts.getOrDefault("PROFILE_AMBIGUOUS",0),profiles.size(),unbound.size(),
                Math.multiplyExact(items.size(),3),false,false,0,false),items,unbound);
    }
    private static Item selectItem(String provider,UUID id,String source,String parser,List<AttachmentDiscoveryProfile> profiles,Set<Profile> used) {
        var candidates=profiles.stream().filter(p->provider.equals(p.selectProviderCode()) && p.selectSourceBindings().stream()
                .anyMatch(b->Objects.equals(source,b.localSourceCode()))).toList();
        var matches=candidates.stream().filter(p->p.selectSourceBindings().stream().anyMatch(b->Objects.equals(source,b.localSourceCode())
                && Objects.equals(parser,b.listParserProfileCode()))).map(AttachmentProviderQaPlan::selectProfile)
                .sorted(Comparator.comparing(Profile::profileCode)).toList();
        String status=matches.size()>1?"PROFILE_AMBIGUOUS":matches.size()==1?"SYSTEM_BINDING_MATCHED":candidates.isEmpty()?"PROFILE_MISSING":"LIST_PARSER_MISMATCH";
        var visible=matches.isEmpty()?candidates.stream().map(AttachmentProviderQaPlan::selectProfile).sorted(Comparator.comparing(Profile::profileCode)).toList():matches;
        used.addAll(visible);
        String message=switch(status) {
            case "SYSTEM_BINDING_MATCHED" -> "시스템 기관·목록 파서 결합만 확인했습니다. 공식 상세 주소와 전체 표본·형식별 실파일 QA가 필요합니다.";
            case "PROFILE_AMBIGUOUS" -> "같은 기관·목록 파서에 첨부 프로필이 중복 연결됐습니다. 시스템 등록을 정리해야 합니다.";
            case "LIST_PARSER_MISMATCH" -> "등록된 첨부 프로필과 현재 목록 파서가 다릅니다. 출처 설정과 구현을 확인해야 합니다.";
            default -> "이 출처의 시스템 첨부 프로필이 없습니다. 프로필 구현과 출처별 실파일 QA가 필요합니다.";
        };
        return new Item(provider,id,source,parser,status,message,visible,3,List.of("PDF","HWP","HWPX"));
    }
    private static Profile selectProfile(AttachmentDiscoveryProfile value) {return new Profile(value.selectProviderCode(),value.selectProfileCode(),value.selectProfileHash());}
    private static boolean safe(String value) {return value!=null && value.matches("[A-Za-z0-9_-]{1,100}");}
    private static IllegalArgumentException invalid() {return new IllegalArgumentException("PROVIDER_QA_SYSTEM_SCOPE_INVALID");}
}
