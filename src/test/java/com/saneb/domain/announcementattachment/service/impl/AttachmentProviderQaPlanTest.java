package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Target;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class AttachmentProviderQaPlanTest {
    private AttachmentDiscoveryProfile profile(String code,String source,String parser) {
        var p=mock(AttachmentDiscoveryProfile.class);when(p.selectProviderCode()).thenReturn(source==null?"BIZINFO":"LOCAL_GOV_NOTICE");
        when(p.selectProfileCode()).thenReturn(code);when(p.selectProfileHash()).thenReturn("a".repeat(64));
        when(p.selectSourceBindings()).thenReturn(List.of(new AttachmentDiscoveryProfile.SourceBinding(source,parser)));return p;
    }
    private Target target(String code,String parser) {return new Target(UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)),code,parser,"https://not-read.invalid/?private=not-output","{\"secret\":\"not-output\"}");}
    @Test void everyTargetIncludingMissingGov24AndSameParserOtherInstitutionStaysInDenominator() throws Exception {
        var p=profile("TAEBAEK","LGS-000121","SPRING_BBS");
        var plan=AttachmentProviderQaPlan.selectPlan(List.of(new BizInfoAttachmentDiscoveryProfile(),p),List.of(target("LGS-000121","SPRING_BBS"),target("LGS-000126","SPRING_BBS")));
        assertThat(plan.summary().targetCount()).isEqualTo(4);assertThat(plan.summary().bindingMatchedCount()).isEqualTo(2);
        assertThat(plan.summary().missingProfileCount()).isEqualTo(2);assertThat(plan.summary().minimumNormalNoticeCount()).isEqualTo(12);
        assertThat(plan.summary().isQaPassed()).isFalse();assertThat(plan.summary().isExecutionPlanComplete()).isFalse();assertThat(plan.summary().currentHttpRequests()).isZero();
        assertThat(new ObjectMapper().writeValueAsString(plan)).doesNotContain("not-output","https:","sourceUrl","profileConfigurationJson");
        verify(p,never()).selectDetailUri(org.mockito.ArgumentMatchers.any(AttachmentDiscoveryProfile.Source.class));
        verify(p,never()).selectDescriptors(org.mockito.ArgumentMatchers.any(AttachmentDiscoveryProfile.Source.class),org.mockito.ArgumentMatchers.any());
    }
    @Test void mismatchAmbiguityAndUnboundRegistrationAreDifferentStates() {
        var p=profile("PROFILE_1","LGS-000001","OLD_PARSER");var q=profile("PROFILE_2","LGS-000002","PARSER");
        var q2=profile("PROFILE_3","LGS-000002","PARSER");var unused=profile("PROFILE_4","LGS-000099","PARSER");
        var plan=AttachmentProviderQaPlan.selectPlan(List.of(p,q,q2,unused),List.of(target("LGS-000001","CURRENT_PARSER"),target("LGS-000002","PARSER")));
        assertThat(plan.items()).extracting(i->i.statusCode()).containsExactly("PROFILE_MISSING","PROFILE_MISSING","LIST_PARSER_MISMATCH","PROFILE_AMBIGUOUS");
        assertThat(plan.summary().parserMismatchCount()).isEqualTo(1);assertThat(plan.summary().ambiguousProfileCount()).isEqualTo(1);
        assertThat(plan.unboundProfiles()).singleElement().satisfies(i->assertThat(i.profileCode()).isEqualTo("PROFILE_4"));
    }
    @Test void orderDoesNotChangePlanAndDataCannotBeModifiedAfterConstruction() {
        var p=profile("PROFILE_A","LGS-000001","PARSER");var q=profile("PROFILE_B","LGS-000002","PARSER");
        var first=target("LGS-000001","PARSER");var second=target("LGS-000002","PARSER");
        var plan=AttachmentProviderQaPlan.selectPlan(List.of(p,q),List.of(first,second));
        assertThat(plan).isEqualTo(AttachmentProviderQaPlan.selectPlan(List.of(q,p),List.of(second,first)));
        assertThatThrownBy(()->plan.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void malformedDuplicateOrOversizedScopeIsNotSilentlyTrimmed() {
        var p=profile("PROFILE_A","LGS-000001","PARSER");var first=target("LGS-000001","PARSER");
        assertThatThrownBy(()->AttachmentProviderQaPlan.selectPlan(List.of(p,p),List.of(first))).hasMessage("PROVIDER_QA_SYSTEM_SCOPE_INVALID");
        assertThatThrownBy(()->AttachmentProviderQaPlan.selectPlan(List.of(p),List.of(first,first))).hasMessage("PROVIDER_QA_SYSTEM_SCOPE_INVALID");
        assertThatThrownBy(()->AttachmentProviderQaPlan.selectPlan(List.of(p),Collections.nCopies(1001,first))).hasMessage("PROVIDER_QA_SYSTEM_SCOPE_INVALID");
        assertThatThrownBy(()->AttachmentProviderQaPlan.selectPlan(List.of(p),List.of(target("invalid\ncode","PARSER")))).hasMessage("PROVIDER_QA_SYSTEM_SCOPE_INVALID");
    }
    @Test void noDeclaredBindingNeverMeansProviderWideSupport() {
        var p=profile("PROFILE_A","LGS-000001","PARSER");when(p.selectSourceBindings()).thenReturn(List.of());
        var plan=AttachmentProviderQaPlan.selectPlan(List.of(p),List.of(target("LGS-000001","PARSER")));
        assertThat(plan.summary().bindingMatchedCount()).isZero();assertThat(plan.summary().unboundProfileCount()).isEqualTo(1);
    }
    @Test void allActualRegisteredProfilesDeclareTheirOwnInstitutionWithoutStartingApplicationOrNetwork() {
        try(var context=new AnnotationConfigApplicationContext()) {
            context.register(BizInfoAttachmentDiscoveryProfile.class,SeoguSaeolAttachmentDiscoveryProfile.class,HwacheonPostAttachmentDiscoveryProfile.class,
                    SaeolGetAttachmentProfileConfiguration.class,LegalBoardAttachmentProfileConfiguration.class,StandardBbsAttachmentProfileConfiguration.class);
            context.refresh();var profiles=new ArrayList<>(context.getBeansOfType(AttachmentDiscoveryProfile.class).values());
            assertThat(profiles).hasSize(18);
            var targets=profiles.stream().filter(p->"LOCAL_GOV_NOTICE".equals(p.selectProviderCode())).flatMap(p->p.selectSourceBindings().stream())
                    .map(b->target(b.localSourceCode(),b.listParserProfileCode())).toList();
            var plan=AttachmentProviderQaPlan.selectPlan(profiles,targets);
            assertThat(plan.summary().targetCount()).isEqualTo(19);assertThat(plan.summary().bindingMatchedCount()).isEqualTo(18);
            assertThat(plan.summary().missingProfileCount()).isEqualTo(1);assertThat(plan.unboundProfiles()).isEmpty();
            assertThat(plan.items()).filteredOn(i->i.providerCode().equals("GOV24_PUBLIC_SERVICE")).singleElement()
                    .satisfies(i->assertThat(i.statusCode()).isEqualTo("PROFILE_MISSING"));
        }
    }
}
