package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.service.AttachmentLegacyPathGuard;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.*;
import com.saneb.domain.announcementsource.service.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기존 기본 검수·태그 확정·초안 생성 계약을 한 transaction으로 조합한다. */
@Service
public class AnnouncementSourceBaseReviewServiceImpl implements AnnouncementSourceBaseReviewService {
    private final AnnouncementSourceDao sources;
    private final AnnouncementSourceService sourceService;
    private final AnnouncementSourceClassificationManagementService classificationService;
    private final AnnouncementSourceV2ConversionService conversion;

    public AnnouncementSourceBaseReviewServiceImpl(AnnouncementSourceDao sources, AnnouncementSourceService sourceService,
            AnnouncementSourceClassificationManagementService classificationService, AnnouncementSourceV2ConversionService conversion) {
        this.sources = sources; this.sourceService = sourceService;
        this.classificationService = classificationService; this.conversion = conversion;
    }

    @Override @Transactional
    public AnnouncementSourceLinkResponse insertReviewedAnnouncement(Authentication authentication, UUID sourceId,
            AnnouncementSourceBaseReviewRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof com.saneb.domain.auth.vo.AuthenticatedUserDetails)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        var source = sources.selectSourceDetailsForUpdate(sourceId);
        if (source == null) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "수집 원문을 찾을 수 없습니다.");
        var linked = sources.selectLinkedAnnouncementDetails(sourceId);
        if (linked != null) return new AnnouncementSourceLinkResponse(sourceId, source.publicCode(), linked.announcementId(), linked.announcementCode());
        AttachmentLegacyPathGuard.validate(sources.selectAttachmentReviewRequiredDetailsForUpdate(sourceId));
        var context = conversion.selectConversionContextDetails(sourceId);
        if (!context.convertible()) throw new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE,
                HttpStatus.CONFLICT, context.blockedReason() == null ? "현재 자료는 기본 검수 경로로 전환할 수 없습니다." : context.blockedReason());
        var value = request.classification();
        if (!java.util.Objects.equals(context.decisionId(), value.expectedClassificationDecisionId())
                || !java.util.Objects.equals(context.version(), value.expectedVersion())) {
            throw new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT, HttpStatus.CONFLICT, "판정이 변경되었습니다. 최신 자료를 확인하세요.");
        }
        var confirmed = classificationService.saveConfirmedClassification(authentication, sourceId,
                new AnnouncementSourceConfirmedClassificationSaveRequest(value.expectedClassificationDecisionId(),
                        value.expectedVersion(), value.targetCategoryCodes(), value.supportTypeCodes(), request.reviewNote()));
        if ("REVIEW_REQUIRED".equals(confirmed.semanticStatusCode()) && !"REVIEW_COMPLETED".equals(source.reviewStatusCode())) {
            sourceService.updateSourceReviewStatus(authentication, sourceId,
                    new AnnouncementSourceReviewStatusUpdateRequest("REVIEW_COMPLETED", request.reviewNote()));
        }
        return conversion.insertOperationalAnnouncement(authentication, sourceId, new AnnouncementSourceV2ToAnnouncementRequest(
                value.primaryTargetCategoryCode(), value.targetCategoryCodes(), value.supportTypeCodes(), value.incomeJudgementCode(),
                confirmed.decisionId(), confirmed.version()));
    }
}
