package com.saneb.domain.announcementattachment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentSegmentServiceImpl;
import com.saneb.domain.announcementattachment.vo.AttachmentSegmentRows;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AnnouncementAttachmentSegmentServiceTest {
    private final UUID source = UUID.randomUUID(), set = UUID.randomUUID(), file = UUID.randomUUID(), extraction = UUID.randomUUID();
    private final AnnouncementAttachmentSegmentDao dao = mock(AnnouncementAttachmentSegmentDao.class);
    private final AnnouncementSourceDao audits = mock(AnnouncementSourceDao.class);
    private final ObjectMapper json = new ObjectMapper();
    private final AnnouncementAttachmentSegmentService service = new AnnouncementAttachmentSegmentServiceImpl(dao, audits, json);
    private static final String TEXT = "사업 지원 안내\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월\n지원 신청서\n성 명\n(서명 또는 인)";
    private AttachmentSegmentRows.Stored stored;

    @Test void browserVersionFingerprintsMatchServerContract() throws Exception {
        String script=new org.springframework.core.io.ClassPathResource("static/js/saneb-attachment-segments.js")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(script).contains("\""+AttachmentSegmentRoleAnalyzer.VERSION+"\": \""+AttachmentSegmentRoleAnalyzer.RULES_HASH+"\"",
                "\""+AttachmentSegmentRoleAnalyzer.QUARTER_VERSION+"\": \""+AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH+"\"");
    }

    @Test void getReturnsNotAnalyzedWithoutWriting() throws Exception {
        prepare();
        var result = service.selectAnalysisDetails(source, extraction);
        assertThat(result.analysisState()).isEqualTo("NOT_ANALYZED");
        assertThat(result.applicationMode()).isEqualTo("SHADOW");
        assertThat(result.analysis()).isNull();
        verify(dao, never()).insertAnalysis(any()); verifyNoInteractions(audits);
    }
    @Test void analyzesExactStoredExtractionAndAuditsOnlyMetadata() throws Exception {
        prepare();
        var result = service.insertAnalysis(auth("ADMIN"), source, extraction);
        assertThat(result.analysisState()).isEqualTo("ANALYZED");
        assertThat(result.analysis().statusCode()).isEqualTo("RESOLVED");
        assertThat(result.fileRoleCode()).isEqualTo("UNKNOWN");
        assertThat(result.fileRoleOriginCode()).isEqualTo("TEXT_RULE");
        assertThat(result.applicationMode()).isEqualTo("SHADOW");
        var command = ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(audits).insertAuditLog(command.capture());
        assertThat(command.getValue().metadataJson()).contains("SHADOW", extraction.toString()).doesNotContain("소상공인", "성 명", "신청서");
        var writer = ArgumentCaptor.forClass(AttachmentSegmentRows.Insert.class);
        verify(dao).insertAnalysis(writer.capture());
        assertThat(writer.getValue().sourceId()).isEqualTo(source);
        assertThat(writer.getValue().analysisJson()).doesNotContain("소상공인", "성 명");
    }
    @Test void repeatPostAndGetReuseSameAnalysisWithoutDuplicatingAudit() throws Exception {
        prepare();
        var first = service.insertAnalysis(auth("OPERATOR"), source, extraction);
        var second = service.insertAnalysis(auth("OPERATOR"), source, extraction);
        var read = service.selectAnalysisDetails(source, extraction);
        assertThat(second).isEqualTo(first); assertThat(read).isEqualTo(first);
        verify(dao, times(1)).insertAnalysis(any()); verify(audits, times(1)).insertAuditLog(any());
    }
    @ParameterizedTest @ValueSource(strings = {"APPROVER", "USER", "PARTNER"})
    void unauthorizedServiceCallDoesNotReadOrWrite(String role) {
        assertThatThrownBy(() -> service.insertAnalysis(auth(role), source, extraction)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao, audits);
    }
    @Test void anonymousServiceCallIsRejected() {
        assertThatThrownBy(() -> service.insertAnalysis(null, source, extraction)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao, audits);
    }
    @Test void wrongSourceAndMissingExtractionDoNotReadSavedAnalysis() throws Exception {
        prepare();
        assertThatThrownBy(() -> service.selectAnalysisDetails(UUID.randomUUID(), extraction)).isInstanceOf(ApiException.class);
        verify(dao, never()).selectAnalysisDetails(any(), any(), anyString(), anyString()); verifyNoInteractions(audits);
    }
    @Test void forgedSavedSegmentCannotBeDisplayedAsResolved() throws Exception {
        prepare();
        service.insertAnalysis(auth("ADMIN"), source, extraction);
        var tree = json.readTree(stored.analysisJson());
        ((com.fasterxml.jackson.databind.node.ObjectNode) tree.path("segments").get(1)).put("roleCode", "GUIDE");
        stored = new AttachmentSegmentRows.Stored(stored.id(), source, set, file, extraction, tree.toString(), stored.createdAt());
        assertThatThrownBy(() -> service.selectAnalysisDetails(source, extraction)).isInstanceOf(ApiException.class);
    }
    @Test void damagedBlocksCannotBeAnalyzedOrWritten() {
        when(dao.selectExtractionDetails(source, extraction)).thenReturn(new AttachmentSegmentRows.Input(source, set, file, extraction,
                "COMPLETE_TEXT", TEXT, "[]", 1, "UNKNOWN", "UNKNOWN"));
        assertThatThrownBy(() -> service.insertAnalysis(auth("ADMIN"), source, extraction)).isInstanceOf(ApiException.class);
        verify(dao, never()).insertAnalysis(any()); verifyNoInteractions(audits);
    }
    @Test void explicitVersionReadsOnlyItsStoredAnalysisWithoutCreatingOrFallingBack() throws Exception {
        prepare();
        var old=service.insertAnalysis(auth("ADMIN"),source,extraction);
        assertThat(service.selectAnalysisDetails(source,extraction,AttachmentSegmentRoleAnalyzer.QUARTER_VERSION).analysisState()).isEqualTo("NOT_ANALYZED");
        var analysis=new AttachmentSegmentRoleAnalyzer().selectAnalysis(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",TEXT,
                List.of(new AttachmentSetEvidence.Block(0,0,TEXT.length(),"p:0",true,"p:0")),1,0),
                AttachmentSegmentRoleAnalyzer.QUARTER_VERSION,AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH);
        var newer=new AttachmentSegmentRows.Stored(UUID.randomUUID(),source,set,file,extraction,json.writeValueAsString(analysis),OffsetDateTime.now());
        when(dao.selectAnalysisDetails(source,extraction,analysis.analysisVersion(),analysis.rulesHash())).thenReturn(newer);
        var read=service.selectAnalysisDetails(source,extraction,analysis.analysisVersion());
        assertThat(read.analysis()).isEqualTo(analysis);assertThat(read.analysisId()).isEqualTo(newer.id());
        assertThat(service.selectAnalysisDetails(source,extraction)).isEqualTo(old);
        assertThat(service.insertAnalysis(auth("ADMIN"),source,extraction)).isEqualTo(old);
        verify(dao,times(1)).insertAnalysis(any());verify(audits,times(1)).insertAuditLog(any());
        // 다른 버전의 정상 분석도 요청한 버전의 결과로 대체할 수 없다.
        when(dao.selectAnalysisDetails(source,extraction,analysis.analysisVersion(),analysis.rulesHash())).thenReturn(stored);
        assertThatThrownBy(()->service.selectAnalysisDetails(source,extraction,analysis.analysisVersion())).isInstanceOf(ApiException.class);
    }
    @ParameterizedTest @ValueSource(strings={"","segment-role-1.0.1","future","segment-role-1.0.2 "})
    void unknownOrDiagnosticVersionIsRejectedBeforeStorageAccess(String version) {
        assertThatThrownBy(()->service.selectAnalysisDetails(source,extraction,version)).isInstanceOf(ApiException.class)
                .hasMessageContaining("analysisVersion");verifyNoInteractions(dao,audits);
    }
    @Test void evaluationBindingReadsExactImmutableInputAndNeverFallsBackToShadow() throws Exception {
        prepare();
        var saved=service.insertAnalysis(auth("ADMIN"),source,extraction);
        UUID evaluation=UUID.randomUUID(), policy=UUID.randomUUID();
        var binding=new AttachmentSegmentRows.Binding(evaluation,source,set,file,extraction,policy,saved.analysisId(),
                AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH,"FORM",false);
        when(dao.selectEvaluationBindingDetails(source,extraction,evaluation)).thenReturn(binding);
        clearInvocations(dao,audits);
        var read=service.selectEvaluationAnalysisDetails(source,extraction,evaluation);
        assertThat(read.evaluationId()).isEqualTo(evaluation);assertThat(read.policyId()).isEqualTo(policy);
        assertThat(read.evaluationCurrent()).isFalse();assertThat(read.evaluatedFileRoleCode()).isEqualTo("FORM");
        assertThat(read.segmentAnalysis()).isEqualTo(saved);
        stored=new AttachmentSegmentRows.Stored(UUID.randomUUID(),source,set,file,extraction,stored.analysisJson(),stored.createdAt());
        assertThatThrownBy(()->service.selectEvaluationAnalysisDetails(source,extraction,evaluation)).isInstanceOf(ApiException.class);
        when(dao.selectEvaluationBindingDetails(source,extraction,evaluation)).thenReturn(null);
        assertThatThrownBy(()->service.selectEvaluationAnalysisDetails(source,extraction,evaluation)).isInstanceOf(ApiException.class)
                .hasMessageContaining("독립 분석으로 대체하지 않습니다");
        verify(dao,never()).insertAnalysis(any());verifyNoInteractions(audits);
    }
    @ParameterizedTest @ValueSource(strings={"evaluation","source","set","file","extraction","policy","analysis","version","hash","role","current"})
    void corruptEvaluationBindingCannotExposeAnalysis(String field) throws Exception {
        prepare();UUID evaluation=UUID.randomUUID();
        when(dao.selectEvaluationBindingDetails(source,extraction,evaluation)).thenReturn(new AttachmentSegmentRows.Binding(
                field.equals("evaluation")?UUID.randomUUID():evaluation,field.equals("source")?UUID.randomUUID():source,
                field.equals("set")?UUID.randomUUID():set,field.equals("file")?UUID.randomUUID():file,
                field.equals("extraction")?UUID.randomUUID():extraction,field.equals("policy")?null:UUID.randomUUID(),
                field.equals("analysis")?null:UUID.randomUUID(),field.equals("version")?"segment-role-1.0.1":AttachmentSegmentRoleAnalyzer.VERSION,
                field.equals("hash")?"a".repeat(64):AttachmentSegmentRoleAnalyzer.RULES_HASH,field.equals("role")?null:"UNKNOWN",field.equals("current")?null:true));
        assertThatThrownBy(()->service.selectEvaluationAnalysisDetails(source,extraction,evaluation)).isInstanceOf(ApiException.class);
        verify(dao,never()).selectAnalysisDetails(any(),any(),anyString(),anyString());
        verify(dao,never()).insertAnalysis(any());verifyNoInteractions(audits);
    }
    private void prepare() throws Exception {
        var block = new AttachmentSetEvidence.Block(0, 0, TEXT.length(), "p:0", true, "p:0");
        when(dao.selectExtractionDetails(source, extraction)).thenReturn(new AttachmentSegmentRows.Input(source, set, file, extraction,
                "COMPLETE_TEXT", TEXT, json.writeValueAsString(List.of(block)), 1, "UNKNOWN", "TEXT_RULE"));
        when(dao.selectAnalysisDetails(eq(source), eq(extraction), eq(AttachmentSegmentRoleAnalyzer.VERSION), eq(AttachmentSegmentRoleAnalyzer.RULES_HASH)))
                .thenAnswer(invocation -> stored);
        when(dao.insertAnalysis(any())).thenAnswer(invocation -> {
            var command = invocation.getArgument(0, AttachmentSegmentRows.Insert.class);
            stored = new AttachmentSegmentRows.Stored(command.id(), source, set, file, extraction, command.analysisJson(), OffsetDateTime.now());
            return 1;
        });
    }
    private Authentication auth(String role) {
        var principal = new AuthenticatedUserDetails(new AuthUserDetailsRow(UUID.randomUUID(), "segment-fixture", "unused-fixture", "합성 구간 QA", "ACTIVE", false, null, null, null), List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
}
