package com.saneb.domain.consultation.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.consultation.dao.ConsultationDao;
import com.saneb.domain.consultation.dto.ConsultationReservationCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationReservationResponse;
import com.saneb.domain.consultation.dto.ConsultationReservationStatusUpdateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotResponse;
import com.saneb.domain.consultation.dto.ConsultationSlotStatusUpdateRequest;
import com.saneb.domain.consultation.service.ConsultationService;
import com.saneb.domain.consultation.vo.ApplicationProgressOwnerRow;
import com.saneb.domain.consultation.vo.AuditLogCommand;
import com.saneb.domain.consultation.vo.ConsultationHistoryCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationRow;
import com.saneb.domain.consultation.vo.ConsultationReservationSearchCondition;
import com.saneb.domain.consultation.vo.ConsultationReservationStatusCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotRow;
import com.saneb.domain.consultation.vo.ConsultationSlotSearchCondition;
import com.saneb.domain.consultation.vo.ConsultationSlotStatusCommand;
import com.saneb.domain.consultation.vo.PartnerVerificationOwnerRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultationServiceImpl implements ConsultationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String RESOURCE_TYPE = "CONSULTATION_RESERVATION";
    private static final Set<String> OPERATING_ROLES = Set.of("OPERATOR", "ADMIN");
    private static final Set<String> PARTNER_OPERATING_ROLES = Set.of("PARTNER", "OPERATOR", "ADMIN");
    private static final Set<String> SLOT_STATUS_CODES = Set.of("OPEN", "HELD", "CLOSED", "CANCELED");
    private static final Set<String> RESERVATION_STATUS_CODES = Set.of(
            "REQUESTED", "CONFIRMED", "CANCELED", "COMPLETED", "NO_SHOW"
    );
    private static final Set<String> RESERVATION_UPDATE_STATUS_CODES = Set.of(
            "CONFIRMED", "CANCELED", "COMPLETED", "NO_SHOW"
    );

    private final ConsultationDao consultationDao;

    public ConsultationServiceImpl(ConsultationDao consultationDao) {
        this.consultationDao = consultationDao;
    }

    @Override
    public PageResponse<ConsultationSlotResponse> selectConsultationSlotList(
            Authentication authentication,
            UUID partnerUserId,
            String statusCode,
            OffsetDateTime startFrom,
            OffsetDateTime startTo,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, SLOT_STATUS_CODES);
        if (!hasPartnerOperatingRole(actor)) {
            normalizedStatusCode = "OPEN";
        }
        UUID effectivePartnerUserId = hasOperatingRole(actor)
                ? partnerUserId
                : actor.roles().contains("PARTNER") ? actor.userId() : partnerUserId;

        ConsultationSlotSearchCondition condition = new ConsultationSlotSearchCondition(
                effectivePartnerUserId,
                normalizedStatusCode,
                startFrom,
                startTo,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = consultationDao.selectConsultationSlotCount(condition);
        List<ConsultationSlotResponse> items = consultationDao.selectConsultationSlotList(condition).stream()
                .map(this::toSlotResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public ConsultationSlotResponse insertConsultationSlot(
            Authentication authentication,
            ConsultationSlotCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        UUID partnerUserId = selectSlotPartnerUserId(actor, request.partnerUserId());
        validateSlotTime(request.startAt(), request.endAt());

        UUID slotId = UUID.randomUUID();
        consultationDao.insertConsultationSlot(new ConsultationSlotInsertCommand(
                slotId,
                partnerUserId,
                request.startAt(),
                request.endAt(),
                trimToNull(request.note()),
                actor.userId()
        ));
        return toSlotResponse(selectSlotRow(slotId));
    }

    @Override
    @Transactional
    public ConsultationSlotResponse updateConsultationSlotStatus(
            Authentication authentication,
            UUID slotId,
            ConsultationSlotStatusUpdateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String statusCode = normalizeRequiredCode("statusCode", request.statusCode(), SLOT_STATUS_CODES);
        ConsultationSlotRow slot = selectSlotRow(slotId);
        validatePartnerSlotAccess(actor, slot.partnerUserId());

        int updatedCount = consultationDao.updateConsultationSlotStatus(new ConsultationSlotStatusCommand(
                slotId,
                statusCode,
                trimToNull(request.note()),
                actor.userId()
        ));
        if (updatedCount == 0) {
            throw notFound("상담 가능 시간을 찾을 수 없습니다.");
        }
        return toSlotResponse(selectSlotRow(slotId));
    }

    @Override
    public PageResponse<ConsultationReservationResponse> selectConsultationReservationList(
            Authentication authentication,
            UUID memberUserId,
            UUID partnerUserId,
            String statusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, RESERVATION_STATUS_CODES);

        UUID effectiveMemberUserId = memberUserId;
        UUID effectivePartnerUserId = partnerUserId;
        if (!hasPartnerOperatingRole(actor)) {
            effectiveMemberUserId = actor.userId();
        } else if (actor.roles().contains("PARTNER") && !hasOperatingRole(actor)) {
            effectivePartnerUserId = actor.userId();
        }

        ConsultationReservationSearchCondition condition = new ConsultationReservationSearchCondition(
                effectiveMemberUserId,
                effectivePartnerUserId,
                normalizedStatusCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = consultationDao.selectConsultationReservationCount(condition);
        List<ConsultationReservationResponse> items = consultationDao.selectConsultationReservationList(condition)
                .stream()
                .map(this::toReservationResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public ConsultationReservationResponse insertConsultationReservation(
            Authentication authentication,
            ConsultationReservationCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        ConsultationSlotRow slot = selectSlotRow(request.slotId());
        if (!"OPEN".equals(slot.statusCode())) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "예약 가능한 시간이 아닙니다."
            );
        }

        UUID memberUserId = selectReservationMemberUserId(actor, request.memberUserId());
        validateReservationReference(actor, memberUserId, request.progressId(), request.verificationId());

        UUID reservationId = UUID.randomUUID();
        consultationDao.insertConsultationReservation(new ConsultationReservationInsertCommand(
                reservationId,
                slot.slotId(),
                memberUserId,
                slot.partnerUserId(),
                request.progressId(),
                request.verificationId(),
                trimToNull(request.requestNote()),
                actor.userId()
        ));
        consultationDao.updateConsultationSlotStatus(new ConsultationSlotStatusCommand(
                slot.slotId(),
                "HELD",
                null,
                actor.userId()
        ));
        consultationDao.insertConsultationHistory(new ConsultationHistoryCommand(
                UUID.randomUUID(),
                reservationId,
                actor.userId(),
                null,
                "REQUESTED",
                null
        ));
        insertAudit(actor.userId(), "CONSULTATION_RESERVATION_CREATE", reservationId, metadata(
                "slotId", slot.slotId().toString(),
                "memberUserId", memberUserId.toString(),
                "partnerUserId", slot.partnerUserId().toString()
        ));
        return toReservationResponse(selectReservationRow(reservationId));
    }

    @Override
    @Transactional
    public ConsultationReservationResponse updateConsultationReservationStatus(
            Authentication authentication,
            UUID reservationId,
            ConsultationReservationStatusUpdateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String afterStatusCode = normalizeRequiredCode(
                "statusCode",
                request.statusCode(),
                RESERVATION_UPDATE_STATUS_CODES
        );
        ConsultationReservationRow reservation = selectReservationRow(reservationId);
        validateReservationAccess(actor, reservation, afterStatusCode);
        validateReservationTransition(reservation.statusCode(), afterStatusCode);

        int updatedCount = consultationDao.updateConsultationReservationStatus(new ConsultationReservationStatusCommand(
                reservationId,
                afterStatusCode,
                trimToNull(request.note()),
                actor.userId()
        ));
        if (updatedCount == 0) {
            throw notFound("상담 예약을 찾을 수 없습니다.");
        }
        consultationDao.updateConsultationSlotStatus(new ConsultationSlotStatusCommand(
                reservation.slotId(),
                selectSlotStatusForReservation(afterStatusCode),
                null,
                actor.userId()
        ));
        consultationDao.insertConsultationHistory(new ConsultationHistoryCommand(
                UUID.randomUUID(),
                reservationId,
                actor.userId(),
                reservation.statusCode(),
                afterStatusCode,
                trimToNull(request.note())
        ));
        insertAudit(actor.userId(), "CONSULTATION_RESERVATION_STATUS_UPDATE", reservationId, metadata(
                "beforeStatusCode", reservation.statusCode(),
                "afterStatusCode", afterStatusCode,
                "noteProvided", String.valueOf(trimToNull(request.note()) != null)
        ));
        return toReservationResponse(selectReservationRow(reservationId));
    }

    private UUID selectSlotPartnerUserId(AuthenticatedUserDetails actor, UUID requestedPartnerUserId) {
        if (hasOperatingRole(actor)) {
            return requestedPartnerUserId == null ? actor.userId() : requestedPartnerUserId;
        }
        if (actor.roles().contains("PARTNER")) {
            if (requestedPartnerUserId != null && !requestedPartnerUserId.equals(actor.userId())) {
                throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "본인 상담 시간만 등록할 수 있습니다.");
            }
            return actor.userId();
        }
        throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "상담 시간 등록 권한이 없습니다.");
    }

    private UUID selectReservationMemberUserId(AuthenticatedUserDetails actor, UUID requestedMemberUserId) {
        if (hasOperatingRole(actor)) {
            return requestedMemberUserId == null ? actor.userId() : requestedMemberUserId;
        }
        if (requestedMemberUserId != null && !requestedMemberUserId.equals(actor.userId())) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "본인 상담만 예약할 수 있습니다.");
        }
        return actor.userId();
    }

    private void validateSlotTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw validationFailed("상담 종료 시간은 시작 시간보다 뒤여야 합니다.");
        }
    }

    private void validateReservationReference(
            AuthenticatedUserDetails actor,
            UUID memberUserId,
            UUID progressId,
            UUID verificationId
    ) {
        if (progressId != null) {
            ApplicationProgressOwnerRow row = consultationDao.selectApplicationProgressOwner(progressId);
            if (row == null) {
                throw notFound("신청 진행 건을 찾을 수 없습니다.");
            }
            if (!hasOperatingRole(actor) && !row.memberUserId().equals(memberUserId)) {
                throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "본인 신청 진행 건만 예약할 수 있습니다.");
            }
        }
        if (verificationId != null) {
            PartnerVerificationOwnerRow row = consultationDao.selectPartnerVerificationOwner(verificationId);
            if (row == null) {
                throw notFound("검증 건을 찾을 수 없습니다.");
            }
            if (!hasOperatingRole(actor) && !row.memberUserId().equals(memberUserId)) {
                throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "본인 검증 건만 예약할 수 있습니다.");
            }
        }
    }

    private void validatePartnerSlotAccess(AuthenticatedUserDetails actor, UUID partnerUserId) {
        if (!hasOperatingRole(actor) && !partnerUserId.equals(actor.userId())) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "본인 상담 시간만 변경할 수 있습니다.");
        }
    }

    private void validateReservationAccess(
            AuthenticatedUserDetails actor,
            ConsultationReservationRow reservation,
            String afterStatusCode
    ) {
        if (hasOperatingRole(actor)) {
            return;
        }
        if (actor.roles().contains("PARTNER") && reservation.partnerUserId().equals(actor.userId())) {
            return;
        }
        if (reservation.memberUserId().equals(actor.userId()) && "CANCELED".equals(afterStatusCode)) {
            return;
        }
        throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "상담 예약 상태를 변경할 수 없습니다.");
    }

    private void validateReservationTransition(String beforeStatusCode, String afterStatusCode) {
        boolean allowed = switch (beforeStatusCode) {
            case "REQUESTED" -> Set.of("CONFIRMED", "CANCELED").contains(afterStatusCode);
            case "CONFIRMED" -> Set.of("COMPLETED", "NO_SHOW", "CANCELED").contains(afterStatusCode);
            default -> false;
        };
        if (!allowed) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "허용되지 않는 상담 예약 상태 변경입니다.");
        }
    }

    private String selectSlotStatusForReservation(String reservationStatusCode) {
        return switch (reservationStatusCode) {
            case "CANCELED" -> "OPEN";
            case "COMPLETED", "NO_SHOW" -> "CLOSED";
            default -> "HELD";
        };
    }

    private ConsultationSlotRow selectSlotRow(UUID slotId) {
        ConsultationSlotRow row = consultationDao.selectConsultationSlotDetails(slotId);
        if (row == null) {
            throw notFound("상담 가능 시간을 찾을 수 없습니다.");
        }
        return row;
    }

    private ConsultationReservationRow selectReservationRow(UUID reservationId) {
        ConsultationReservationRow row = consultationDao.selectConsultationReservationDetails(reservationId);
        if (row == null) {
            throw notFound("상담 예약을 찾을 수 없습니다.");
        }
        return row;
    }

    private ConsultationSlotResponse toSlotResponse(ConsultationSlotRow row) {
        return new ConsultationSlotResponse(
                row.slotId(),
                row.partnerUserId(),
                row.startAt(),
                row.endAt(),
                row.statusCode(),
                row.note(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private ConsultationReservationResponse toReservationResponse(ConsultationReservationRow row) {
        return new ConsultationReservationResponse(
                row.reservationId(),
                row.slotId(),
                row.memberUserId(),
                row.partnerUserId(),
                row.progressId(),
                row.verificationId(),
                row.startAt(),
                row.endAt(),
                row.statusCode(),
                row.requestNote(),
                row.statusNote(),
                row.confirmedAt(),
                row.canceledAt(),
                row.completedAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private AuthenticatedUserDetails selectRequiredPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal;
        }
        throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "DB 인증 사용자만 사용할 수 있습니다.");
    }

    private boolean hasOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(OPERATING_ROLES::contains);
    }

    private boolean hasPartnerOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(PARTNER_OPERATING_ROLES::contains);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "페이지 요청 값이 올바르지 않습니다.");
        }
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String code = normalizeOptionalCode(value);
        if (code == null || !allowedValues.contains(code)) {
            throw validationFailed(fieldName + " is invalid.");
        }
        return code;
    }

    private void validateOptionalCode(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(fieldName + " is invalid.");
        }
    }

    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void insertAudit(UUID actorUserId, String actionCode, UUID resourceId, String metadataJson) {
        consultationDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                RESOURCE_TYPE,
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
