package com.saneb.domain.announcementattachment.qa;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

/** 이미 등록된 고정 참조에서만 선택한다. 제목 규칙·기대값·운영 설정은 바꾸지 않는다. */
final class ExistingProfileDownloadCases {
    static final Set<String> CODES=Set.of("TAEBAEK-184816","JECHEON-403530","CHUNGJU-70852");
    static Stream<ObservationCase> selectCases(){
        var cases=List.of("TAEBAEK","JECHEON","CHUNGJU").stream()
                .flatMap(AnnouncementAttachmentBbsOfficialObservationTest::selectCases)
                .filter(c->CODES.contains(c.code())).toList();
        if(cases.size()!=CODES.size())throw new IllegalStateException("EXISTING_COLLECTION_CASE_CHANGED");
        return cases.stream();
    }
}
