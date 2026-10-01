package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Run;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttachmentCollectionSafetyGateTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final AttachmentPolicyValidationSnapshotFactory snapshots=mock(AttachmentPolicyValidationSnapshotFactory.class);
    private final AttachmentCollectionSafetyGate gate=new AttachmentCollectionSafetyGate(snapshots,mapper);
    private AttachmentProviderQaPlan.Plan selectPlan(boolean available) {
        if(!available)return AttachmentProviderQaPlan.selectPlan(List.of(),List.of());
        var profile=mock(AttachmentDiscoveryProfile.class);
        when(profile.selectProviderCode()).thenReturn("BIZINFO");when(profile.selectProfileCode()).thenReturn("BIZINFO_TEST");
        when(profile.selectProfileHash()).thenReturn("a".repeat(64));
        when(profile.selectSourceBindings()).thenReturn(List.of(new AttachmentDiscoveryProfile.SourceBinding(null,null)));
        return AttachmentProviderQaPlan.selectPlan(List.of(profile),List.of());
    }
    private AttachmentPolicyValidationSnapshotFactory.Frozen selectFrozen(AttachmentProviderQaPlan.Plan plan) throws Exception {
        return new AttachmentPolicyValidationSnapshotFactory.Frozen("a".repeat(64),mapper.writeValueAsString(Map.of(
                "validationContractCode","COLLECTION_SAFETY_V1","modeCode","COLLECT_ONLY","providerQaPlan",plan)),null,null);
    }
    private Run selectRun() {
        return new Run(UUID.randomUUID(),UUID.randomUUID(),0,UUID.randomUUID(),0,"a".repeat(64),null,"RUNNING",1,
                UUID.randomUUID(),UUID.randomUUID(),"b".repeat(64),null,null,null,null,null,null,true);
    }
    @Test void missingTargetsRemainInDenominatorWithoutClaimingDownloadSuccess() throws Exception {
        var plan=selectPlan(true);when(snapshots.selectProviderQaPlan()).thenReturn(plan);
        when(snapshots.hash(plan)).thenReturn("b".repeat(64));
        var evidence=gate.selectEvidence(selectFrozen(plan),selectRun());
        assertThat(evidence.path("targetCount").asInt()).isEqualTo(2);
        assertThat(evidence.path("bindingStatusCounts").path("PROFILE_MISSING").asInt()).isEqualTo(1);
        assertThat(evidence.path("bindingStatusCounts").path("SYSTEM_BINDING_MATCHED").asInt()).isEqualTo(1);
        assertThat(evidence.path("externalDownloadProven").asBoolean()).isFalse();
        assertThat(evidence.path("classificationEnforceProven").asBoolean()).isFalse();
    }
    @Test void noUsableBindingAndChangedFullInventoryFailClosed() throws Exception {
        var missing=selectPlan(false);when(snapshots.selectProviderQaPlan()).thenReturn(missing);
        var frozen=selectFrozen(missing);var run=selectRun();
        assertThatThrownBy(()->gate.selectEvidence(frozen,run)).isInstanceOf(IllegalArgumentException.class);
        var available=selectPlan(true);
        when(snapshots.selectProviderQaPlan()).thenReturn(available);
        assertThatThrownBy(()->gate.selectEvidence(frozen,run)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void evidenceCannotBeForgedOrTransferredAndIsRecheckedBeforePublication() throws Exception {
        var plan=selectPlan(true);when(snapshots.selectProviderQaPlan()).thenReturn(plan);
        when(snapshots.hash(any())).thenReturn("b".repeat(64));
        var frozen=selectFrozen(plan);var run=selectRun();
        var evidence=gate.selectEvidence(frozen,run);
        assertThat(gate.selectValidatedEvidenceHash(evidence,frozen,run)).isEqualTo("b".repeat(64));
        gate.validateCurrentEvidence(evidence,frozen,run);
        assertThatThrownBy(()->gate.validateCurrentEvidence(evidence,frozen,selectRun())).isInstanceOf(IllegalArgumentException.class);
        var forged=evidence.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)forged).put("externalDownloadProven",true);
        assertThatThrownBy(()->gate.selectValidatedEvidenceHash(forged,frozen,run)).isInstanceOf(IllegalArgumentException.class);
        when(snapshots.selectProviderQaPlan()).thenReturn(selectPlan(false));
        assertThatThrownBy(()->gate.validateCurrentEvidence(evidence,frozen,run)).isInstanceOf(IllegalArgumentException.class);
    }
}
