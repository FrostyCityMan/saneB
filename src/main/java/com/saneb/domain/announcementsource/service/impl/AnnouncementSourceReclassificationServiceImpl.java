package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceClassificationDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRequest;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationManagementService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceReclassificationService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceContentVersionRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.operation.dao.OperationDao;
import com.saneb.domain.operation.vo.OperationTaskInsertCommand;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementSourceReclassificationServiceImpl
        implements AnnouncementSourceReclassificationService {

    private final AnnouncementSourceClassificationDao classificationDao;
    private final AnnouncementSourceDao announcementSourceDao;
    private final AnnouncementSourceRuleReleaseService ruleReleaseService;
    private final AnnouncementSourceClassificationPersistenceService persistenceService;
    private final AnnouncementSourceClassificationManagementService managementService;
    private final OperationDao operationDao;
    private final AnnouncementSourceClassificationEngine engine = new AnnouncementSourceClassificationEngine();

    public AnnouncementSourceReclassificationServiceImpl(
            AnnouncementSourceClassificationDao classificationDao,
            AnnouncementSourceDao announcementSourceDao,
            AnnouncementSourceRuleReleaseService ruleReleaseService,
            AnnouncementSourceClassificationPersistenceService persistenceService,
            AnnouncementSourceClassificationManagementService managementService,
            OperationDao operationDao
    ) {
        this.classificationDao = classificationDao;
        this.announcementSourceDao = announcementSourceDao;
        this.ruleReleaseService = ruleReleaseService;
        this.persistenceService = persistenceService;
        this.managementService = managementService;
        this.operationDao = operationDao;
    }

    @Override
    @Transactional
    public AnnouncementSourceClassificationDetailsResponse insertReclassification(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceReclassificationRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceClassificationStateRow state =
                classificationDao.selectClassificationStateDetails(sourceId);
        if (state == null || state.decisionId() == null) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED,
                    HttpStatus.CONFLICT,
                    "재분류할 현재 V2 판정이 없습니다. 원문을 신규 V2 판정 대상으로 먼저 등록해 주세요."
            );
        }
        if (!state.decisionId().equals(request.expectedClassificationDecisionId())) {
            throw versionConflict();
        }

        AnnouncementSourceContentVersionRow content =
                classificationDao.selectLatestContentVersionDetails(sourceId);
        AnnouncementSourceSnapshotRow source = announcementSourceDao.selectSourceDetails(sourceId);
        if (content == null || source == null) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED,
                    HttpStatus.CONFLICT,
                    "재분류할 불변 원문 버전을 찾을 수 없습니다."
            );
        }
        AnnouncementSourceClassificationRuleSet ruleSet =
                ruleReleaseService.selectActiveRuleSet(request.ruleReleaseId());
        AnnouncementSourceClassificationResult result = engine.selectDecision(
                new AnnouncementSourceClassificationInput(
                        source.providerCode(),
                        content.title(),
                        content.bodyText(),
                        source.agencyName(),
                        List.of(),
                        BodySourceCode.valueOf(content.bodySourceCode()),
                        BodyAvailabilityCode.valueOf(content.bodyAvailabilityCode())
                ),
                ruleSet
        );
        AnnouncementSourceProviderItem item = new AnnouncementSourceProviderItem(
                source.providerCode(),
                source.providerNoticeId(),
                content.title(),
                source.agencyName(),
                source.applicationStartDate(),
                source.applicationEndDate(),
                source.postedAt(),
                source.modifiedAt(),
                content.sourceUrl(),
                content.bodyText(),
                source.inquiryText(),
                source.applicationMethodText(),
                source.sourceCompletenessCode(),
                source.missingFieldsJson(),
                null,
                source.rawHash(),
                List.of(),
                null
        ).withSemanticDecision(
                result.semanticStatusCode().name(),
                result.reasonCode().name(),
                null
        );
        String nextReviewStatus = "EXCLUDED".equals(result.semanticStatusCode().name())
                ? "ARCHIVED"
                : "REVIEW_PENDING";
        UUID evaluationId = persistenceService.saveExistingContentEvaluation(
                sourceId,
                content.contentVersionId(),
                request.ruleReleaseId(),
                item,
                result,
                nextReviewStatus,
                request.expectedVersion()
        );
        insertOperationalReviewTaskWhenRequired(actorUserId, sourceId, result);
        announcementSourceDao.insertAuditLog(new AnnouncementSourceAuditLogCommand(
                actorUserId,
                "ANNOUNCEMENT_SOURCE_RECLASSIFIED",
                "ANNOUNCEMENT_SOURCE",
                sourceId,
                "SUCCESS",
                "{\"ruleReleaseId\":\"" + request.ruleReleaseId()
                        + "\",\"evaluationId\":\"" + evaluationId
                        + "\",\"previousDecisionStatusCode\":\"" + state.decisionStatusCode()
                        + "\",\"nextDecisionStatusCode\":\"" + result.semanticStatusCode().name()
                        + "\",\"changeReasonHash\":\"" + sha256(request.changeReason().trim()) + "\"}"
        ));
        return managementService.selectClassificationDetails(sourceId);
    }

    private void insertOperationalReviewTaskWhenRequired(
            UUID actorUserId,
            UUID sourceId,
            AnnouncementSourceClassificationResult result
    ) {
        if (!"EXCLUDED".equals(result.semanticStatusCode().name())
                || announcementSourceDao.selectApprovedLinkedAnnouncementCount(sourceId) == 0
                || operationDao.selectOpenOperationTaskCount(
                        "GENERAL", "ANNOUNCEMENT_SOURCE", sourceId
                ) > 0) {
            return;
        }
        operationDao.insertOperationTask(new OperationTaskInsertCommand(
                UUID.randomUUID(),
                "GENERAL",
                "HIGH",
                "재분류 제외 공고 운영 확인",
                "연결된 운영 공고의 원문이 새 규칙에서 자동 제외로 판정되었습니다. 운영 공고 상태를 수동 확인해 주세요.",
                "ANNOUNCEMENT_SOURCE",
                sourceId,
                OffsetDateTime.now().plusDays(1),
                actorUserId
        ));
    }

    private UUID selectActorUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return details.userId();
    }

    private ApiException versionConflict() {
        return new ApiException(
                ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,
                HttpStatus.CONFLICT,
                "다른 작업자가 판정을 변경했습니다. 최신 상태를 확인한 뒤 다시 시도해 주세요."
        );
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
