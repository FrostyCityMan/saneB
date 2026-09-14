package com.saneb.domain.announcementattachment.qa;

import java.util.HashSet;
import java.util.Set;

/** catalog 준비와 실행 직전이 같은 파일/기대값/한도 계약을 사용한다. 네트워크·DB 호출은 없다. */
public final class AttachmentProviderQaCaseContract {
    public static final Set<String> QUALITIES=Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED");
    private AttachmentProviderQaCaseContract() { }
    public static void validate(AttachmentProviderQaCase input) {
        if(input==null || !code(input.caseId()) || !code(input.profileCode()) || !hash(input.profileHash()) || !hash(input.runtimeHash())
                || input.source()==null || input.title()==null || input.title().isBlank() || input.title().length()>2000
                || input.rules()==null || input.rules().rules().isEmpty() || input.rules().rules().size()>2000 || input.files().size()>10
                || input.source().providerCode()==null || !Set.of("BIZINFO","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE").contains(input.source().providerCode())
                || input.source().providerNoticeId()==null || input.source().providerNoticeId().length()>200
                || input.source().sourceUrl()!=null && input.source().sourceUrl().length()>2048
                || input.discoveryStatus()==null || !Set.of("TITLE_BLOCKED","FOUND","NO_FILES","FAILED").contains(input.discoveryStatus()) || input.limits()==null
                || input.limits().maximumSeconds()<1 || input.limits().maximumSeconds()>420
                || input.limits().maximumRequestReservations()<1 || input.limits().maximumRequestReservations()>44
                || input.limits().maximumReservedBytes()<1 || input.limits().maximumReservedBytes()>83886080)throw invalid();
        if(Set.of("TITLE_BLOCKED","NO_FILES").contains(input.discoveryStatus()) && !input.files().isEmpty()
                || "FOUND".equals(input.discoveryStatus()) && input.files().isEmpty()
                || "NO_FILES".equals(input.discoveryStatus()) && !input.discoveryComplete()
                || Set.of("TITLE_BLOCKED","FAILED").contains(input.discoveryStatus()) && input.discoveryComplete())throw invalid();
        var identifiers=new HashSet<String>();
        for(var file:input.files()) {
            if(file==null || !hash(file.locatorHash()) || !identifiers.add(file.locatorHash()) || file.requiredPhrases().size()>20
                    || file.minimumCharacters()<0 || file.minimumCharacters()>1_000_000 || file.minimumBlocks()<0 || file.minimumBlocks()>20000
                    || file.requiredPhrases().stream().anyMatch(s->s==null || s.isBlank() || s.length()>200 || s.codePoints().anyMatch(Character::isISOControl)))throw invalid();
            if(file.downloadAllowed()) {
                if(file.format()==null || !Set.of("PDF","HWP","HWPX").contains(file.format()) || !hash(file.binaryHash()) || file.quality()==null || !QUALITIES.contains(file.quality()))throw invalid();
                if(Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(file.quality())) {
                    if(file.minimumCharacters()<1 || file.minimumBlocks()<1 || file.requiredPhrases().isEmpty())throw invalid();
                } else if(file.minimumCharacters()!=0 || file.minimumBlocks()!=0 || !file.requiredPhrases().isEmpty())throw invalid();
            } else if(file.binaryHash()!=null || file.quality()!=null || file.minimumCharacters()!=0 || file.minimumBlocks()!=0 || !file.requiredPhrases().isEmpty() || file.format()!=null)throw invalid();
        }
    }
    private static boolean hash(String value){return value!=null && value.matches("[0-9a-f]{64}");}
    private static boolean code(String value){return value!=null && value.matches("[A-Z0-9_-]{1,100}");}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CASE_INPUT_INVALID");}
}
