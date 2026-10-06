package com.saneb.domain.announcementsource.classification;

import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementSourceTargetContextTest {
    private AnnouncementSourceClassificationRule rule(String keyword, TargetCategoryCode target) {
        return new AnnouncementSourceClassificationRule("R-"+keyword,"G-TARGET",RuleGroupKindCode.TARGET,keyword,StrengthCode.STRONG,
                target,null,List.of(AnnouncementSourceClassificationTerm.canonical(keyword,MatchModeCode.NORMALIZED_PHRASE)),true);
    }
    private AnnouncementSourceClassificationRuleSet rules() {
        return new AnnouncementSourceClassificationRuleSet("CONTEXT-FIXTURE",List.of(rule("본인",TargetCategoryCode.PERSONAL),
                rule("육아",TargetCategoryCode.CHILD),rule("자녀",TargetCategoryCode.CHILD),rule("청년",TargetCategoryCode.PERSONAL)));
    }
    @Test void applicantAndEmployeeLeaveAreNotBeneficiaryTags() {
        var scope = new AnnouncementSourceClassificationEngine().selectAttachmentScope("소상공인 사회보험료 지원. 본인이 직접 신청합니다. 육아휴직 근로자는 사업주 확인.","기관",List.of(),rules());
        assertThat(scope.targets()).isEmpty();
        assertThat(scope.matches()).allMatch(m -> m.appliedActionCode() == AppliedActionCode.CONTEXT_ONLY && !m.maskedByProtectedMetadata());
    }
    @Test void genuinePersonalAndChildBeneficiariesRemainMultiTagged() {
        var scope = new AnnouncementSourceClassificationEngine().selectAttachmentScope("청년 본인과 자녀에게 지원합니다. 본인 신청 방법 안내.","기관",List.of(),rules());
        assertThat(scope.targets()).containsExactly(TargetCategoryCode.PERSONAL,TargetCategoryCode.CHILD);
    }
    @Test void leaveIsNotChildButOtherChildEvidenceRemains() {
        var scope = new AnnouncementSourceClassificationEngine().selectAttachmentScope("육아휴직 근로자 안내. 자녀에게 교육비 지원.","기관",List.of(),rules());
        assertThat(scope.targets()).containsExactly(TargetCategoryCode.CHILD);
    }
}
