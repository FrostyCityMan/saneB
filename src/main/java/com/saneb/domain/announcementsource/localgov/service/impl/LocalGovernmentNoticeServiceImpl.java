/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.dao.LocalGovernmentNoticeDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunResponse;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleCreateRequest;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleResponse;
import com.saneb.domain.announcementsource.localgov.dto.AnnouncementSourceScheduleStatusRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionSummaryResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeParserProfileResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceEnabledRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceSaveRequest;
import com.saneb.domain.announcementsource.localgov.service.LocalGovernmentNoticeService;
import com.saneb.domain.announcementsource.localgov.support.LocalGovernmentNoticeUrlValidator;
import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleCommand;
import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleExecutionCommand;
import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleRow;
import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleStatusCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeParserProfileRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceEnabledCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceSearchCondition;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalGovernmentNoticeServiceImpl implements LocalGovernmentNoticeService {

    private static final Set<String> INSTITUTION_TYPES = Set.of(
            "SIDO", "BASIC_LOCAL_GOVERNMENT", "ADMINISTRATIVE_CITY", "CHECK_REQUIRED"
    );
    private static final Set<String> CONFIDENCE_CODES = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> VALIDATION_STATUS_CODES = Set.of("VERIFIED", "CHECK_REQUIRED", "FAILED");
    private static final Set<String> SCHEDULE_STATUS_CODES = Set.of("APPROVED", "PAUSED", "REJECTED", "EXPIRED");

    private final LocalGovernmentNoticeDao localGovernmentNoticeDao;
    private final AnnouncementSourceService announcementSourceService;
    private final LocalGovernmentNoticeUrlValidator urlValidator;

    /**
     * 지자체 공고 URL 관리 서비스를 생성합니다.
     */
    public LocalGovernmentNoticeServiceImpl(
            LocalGovernmentNoticeDao localGovernmentNoticeDao,
            AnnouncementSourceService announcementSourceService,
            LocalGovernmentNoticeUrlValidator urlValidator
    ) {
        this.localGovernmentNoticeDao = localGovernmentNoticeDao;
        this.announcementSourceService = announcementSourceService;
        this.urlValidator = urlValidator;
    }

    /**
     * 지자체 공고 URL 목록을 조회합니다.
     */
    @Override
    public PageResponse<LocalGovernmentNoticeSourceResponse> selectSourceList(
            String sidoName,
            String validationStatusCode,
            String collectionStatusCode,
            Boolean enabled,
            String keyword,
            int page,
            int size
    ) {
        LocalGovernmentNoticeSourceSearchCondition condition = new LocalGovernmentNoticeSourceSearchCondition(
                blankToNull(sidoName), normalizeOptional(validationStatusCode), normalizeOptional(collectionStatusCode),
                enabled, blankToNull(keyword), size, (page - 1) * size
        );
        return PageResponse.of(
                localGovernmentNoticeDao.selectSourceList(condition).stream()
                        .map(LocalGovernmentNoticeSourceResponse::from)
                        .toList(),
                page, size, localGovernmentNoticeDao.selectSourceCount(condition)
        );
    }

    /**
     * 지자체 공고 URL 상세를 조회합니다.
     */
    @Override
    public LocalGovernmentNoticeSourceResponse selectSourceDetails(UUID sourceId) {
        return LocalGovernmentNoticeSourceResponse.from(selectSourceRow(sourceId));
    }

    /**
     * 지자체 공고 URL을 OFF 상태로 등록합니다.
     */
    @Override
    @Transactional
    public LocalGovernmentNoticeSourceResponse insertSource(
            Authentication authentication,
            LocalGovernmentNoticeSourceSaveRequest request
    ) {
        validateSaveRequest(request);
        UUID sourceId = UUID.randomUUID();
        UUID actorUserId = selectActorUserId(authentication);
        localGovernmentNoticeDao.insertSource(toCommand(sourceId, actorUserId, request));
        insertAudit(actorUserId, "LOCAL_GOV_NOTICE_SOURCE_CREATE", "LOCAL_GOV_NOTICE_SOURCE", sourceId,
                "{\"validationStatusCode\":\"" + normalizeOptional(request.validationStatusCode()) + "\"}");
        return selectSourceDetails(sourceId);
    }

    /**
     * 지자체 공고 URL 관리 정보를 수정합니다.
     */
    @Override
    @Transactional
    public LocalGovernmentNoticeSourceResponse updateSource(
            Authentication authentication,
            UUID sourceId,
            LocalGovernmentNoticeSourceSaveRequest request
    ) {
        selectSourceRow(sourceId);
        validateSaveRequest(request);
        UUID actorUserId = selectActorUserId(authentication);
        int updated = localGovernmentNoticeDao.updateSource(toCommand(sourceId, actorUserId, request));
        if (updated == 0) {
            throw notFound();
        }
        insertAudit(actorUserId, "LOCAL_GOV_NOTICE_SOURCE_UPDATE", "LOCAL_GOV_NOTICE_SOURCE", sourceId,
                "{\"validationStatusCode\":\"" + normalizeOptional(request.validationStatusCode()) + "\"}");
        return selectSourceDetails(sourceId);
    }

    /**
     * 검증·파서 조건을 확인한 뒤 지자체 URL ON/OFF를 변경합니다.
     */
    @Override
    @Transactional
    public LocalGovernmentNoticeSourceResponse updateSourceEnabled(
            Authentication authentication,
            UUID sourceId,
            LocalGovernmentNoticeSourceEnabledRequest request
    ) {
        LocalGovernmentNoticeSourceRow row = selectSourceRow(sourceId);
        if (Boolean.TRUE.equals(request.enabled())) {
            if (!"VERIFIED".equals(row.validationStatusCode())) {
                throw invalid("URL 검증 상태가 '검증완료'인 경우에만 수집을 켤 수 있습니다.");
            }
            LocalGovernmentNoticeParserProfileRow profile = row.parserProfileCode() == null
                    ? null : localGovernmentNoticeDao.selectParserProfileDetails(row.parserProfileCode());
            if (profile == null || !profile.enabled() || "MANUAL_ONLY".equals(profile.parserTypeCode())) {
                throw invalid("실행 가능한 수집 파서를 지정한 뒤 수집을 켜세요.");
            }
            urlValidator.validate(row.noticeUrl());
        }
        UUID actorUserId = selectActorUserId(authentication);
        int updated = localGovernmentNoticeDao.updateSourceEnabled(new LocalGovernmentNoticeSourceEnabledCommand(
                sourceId, Boolean.TRUE.equals(request.enabled()), actorUserId
        ));
        if (updated == 0) {
            throw invalid("URL 검증 상태와 수집 파서 설정을 확인하세요.");
        }
        insertAudit(actorUserId, "LOCAL_GOV_NOTICE_SOURCE_ENABLED_UPDATE", "LOCAL_GOV_NOTICE_SOURCE", sourceId,
                "{\"enabled\":" + Boolean.TRUE.equals(request.enabled()) + "}");
        return selectSourceDetails(sourceId);
    }

    /**
     * 지자체 공고 URL을 소프트 삭제합니다.
     */
    @Override
    @Transactional
    public void deleteSource(Authentication authentication, UUID sourceId) {
        selectSourceRow(sourceId);
        UUID actorUserId = selectActorUserId(authentication);
        if (localGovernmentNoticeDao.deleteSource(sourceId, actorUserId) == 0) {
            throw notFound();
        }
        insertAudit(actorUserId, "LOCAL_GOV_NOTICE_SOURCE_DELETE", "LOCAL_GOV_NOTICE_SOURCE", sourceId, "{}");
    }

    /**
     * 단일 지자체 URL 수동 수집 승인 요청을 생성합니다.
     */
    @Override
    public AnnouncementSourceCollectionRequestResponse insertCollectionRequest(
            Authentication authentication,
            UUID sourceId,
            LocalGovernmentNoticeCollectionRequest request
    ) {
        LocalGovernmentNoticeSourceRow row = selectSourceRow(sourceId);
        if (!row.enabled()) {
            throw invalid("수집이 켜진 지자체 URL만 수동 수집 요청을 만들 수 있습니다.");
        }
        int maxCount = request == null || request.maxCount() == null ? 100 : request.maxCount();
        String note = request == null ? null : blankToNull(request.requestNote());
        return announcementSourceService.insertLocalGovernmentCollectionRequest(
                authentication, sourceId, maxCount, note
        );
    }

    /**
     * 파서 프로필 목록을 조회합니다.
     */
    @Override
    public List<LocalGovernmentNoticeParserProfileResponse> selectParserProfileList() {
        return localGovernmentNoticeDao.selectParserProfileList().stream()
                .map(LocalGovernmentNoticeParserProfileResponse::from)
                .toList();
    }

    /**
     * 지자체 수집 신호등 현황을 조회합니다.
     */
    @Override
    public LocalGovernmentNoticeCollectionSummaryResponse selectCollectionSummary() {
        return LocalGovernmentNoticeCollectionSummaryResponse.from(localGovernmentNoticeDao.selectCollectionSummary());
    }

    /**
     * 최초 승인이 필요한 정기 수집 스케줄을 등록합니다.
     */
    @Override
    @Transactional
    public AnnouncementSourceScheduleResponse insertSchedule(
            Authentication authentication,
            AnnouncementSourceScheduleCreateRequest request
    ) {
        CronExpression cron = parseCron(request.cronExpression());
        ZoneId zone = parseZone(request.timezone());
        UUID scheduleId = UUID.randomUUID();
        UUID actorUserId = selectActorUserId(authentication);
        localGovernmentNoticeDao.insertSchedule(new AnnouncementSourceScheduleCommand(
                scheduleId, "LOCAL_GOV_NOTICE", request.scheduleName().trim(), request.cronExpression().trim(),
                zone.getId(), request.maxCount(), actorUserId, selectNextRun(cron, zone)
        ));
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_SCHEDULE_CREATE", "ANNOUNCEMENT_SOURCE_SCHEDULE", scheduleId,
                "{\"providerCode\":\"LOCAL_GOV_NOTICE\"}");
        return selectScheduleResponse(scheduleId);
    }

    /**
     * 정기 수집 스케줄 목록을 조회합니다.
     */
    @Override
    public List<AnnouncementSourceScheduleResponse> selectScheduleList() {
        return localGovernmentNoticeDao.selectScheduleList().stream()
                .map(AnnouncementSourceScheduleResponse::from)
                .toList();
    }

    /**
     * 서버에서 상태 전이를 검증하고 정기 수집 스케줄 상태를 변경합니다.
     */
    @Override
    @Transactional
    public AnnouncementSourceScheduleResponse updateScheduleStatus(
            Authentication authentication,
            UUID scheduleId,
            AnnouncementSourceScheduleStatusRequest request
    ) {
        AnnouncementSourceScheduleRow row = selectScheduleRow(scheduleId);
        String nextStatus = normalizeRequired(request.scheduleStatusCode(), SCHEDULE_STATUS_CODES, "지원하지 않는 스케줄 상태입니다.");
        validateScheduleTransition(row.scheduleStatusCode(), nextStatus);
        OffsetDateTime nextRunAt = "APPROVED".equals(nextStatus)
                ? selectNextRun(parseCron(row.cronExpression()), parseZone(row.timezone())) : null;
        UUID actorUserId = selectActorUserId(authentication);
        int updated = localGovernmentNoticeDao.updateScheduleStatus(new AnnouncementSourceScheduleStatusCommand(
                scheduleId, nextStatus, actorUserId, blankToNull(request.approvalNote()), nextRunAt
        ));
        if (updated == 0) {
            throw notFound("수집 스케줄을 찾을 수 없습니다.");
        }
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_SCHEDULE_STATUS_UPDATE", "ANNOUNCEMENT_SOURCE_SCHEDULE", scheduleId,
                "{\"statusCode\":\"" + nextStatus + "\"}");
        return selectScheduleResponse(scheduleId);
    }

    /**
     * 승인된 실행 예정 스케줄을 DB 실행 슬롯 기준으로 한 번씩 처리합니다.
     */
    @Override
    public void executeDueSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        for (AnnouncementSourceScheduleRow schedule : localGovernmentNoticeDao.selectDueScheduleList(now)) {
            executeSchedule(schedule);
        }
    }

    /**
     * 단일 승인 스케줄의 실행 슬롯을 선점하고 수집을 실행합니다.
     */
    private void executeSchedule(AnnouncementSourceScheduleRow schedule) {
        UUID executionId = UUID.randomUUID();
        OffsetDateTime scheduledFor = schedule.nextRunAt();
        int claimed = localGovernmentNoticeDao.insertScheduleExecution(new AnnouncementSourceScheduleExecutionCommand(
                executionId, schedule.scheduleId(), scheduledFor, null, null, "RUNNING", null
        ));
        if (claimed == 0) {
            return;
        }
        UUID requestId = null;
        UUID runId = null;
        String status = "FAILED";
        String errorMessage = null;
        try {
            AnnouncementSourceCollectionRequestResponse request = announcementSourceService
                    .insertApprovedScheduledCollectionRequest(schedule.scheduleId(), schedule.maxCount());
            requestId = request.requestId();
            AnnouncementSourceCollectionRunResponse run = announcementSourceService.insertCollectionRun(requestId);
            runId = run.runId();
            status = run.runStatusCode();
        } catch (RuntimeException exception) {
            errorMessage = "승인된 정기 수집 실행에 실패했습니다.";
        } finally {
            localGovernmentNoticeDao.updateScheduleExecution(new AnnouncementSourceScheduleExecutionCommand(
                    executionId, schedule.scheduleId(), scheduledFor, requestId, runId, status, errorMessage
            ));
            insertAudit(null, "ANNOUNCEMENT_SOURCE_SCHEDULE_EXECUTE", "ANNOUNCEMENT_SOURCE_SCHEDULE", schedule.scheduleId(),
                    "{\"executionStatusCode\":\"" + status + "\"}");
            ZoneId zone = parseZone(schedule.timezone());
            OffsetDateTime nextRun = selectNextRun(parseCron(schedule.cronExpression()), zone);
            localGovernmentNoticeDao.updateScheduleNextRun(schedule.scheduleId(), OffsetDateTime.now(), nextRun);
        }
    }

    /**
     * 저장 요청의 코드와 외부 URL을 검증합니다.
     */
    private void validateSaveRequest(LocalGovernmentNoticeSourceSaveRequest request) {
        normalizeRequired(request.institutionTypeCode(), INSTITUTION_TYPES, "기관 유형을 확인하세요.");
        normalizeRequired(request.confidenceCode(), CONFIDENCE_CODES, "URL 신뢰도 값을 확인하세요.");
        normalizeRequired(request.validationStatusCode(), VALIDATION_STATUS_CODES, "URL 검증 상태를 확인하세요.");
        urlValidator.validate(request.noticeUrl());
        if (request.homepageUrl() != null && !request.homepageUrl().isBlank()) {
            urlValidator.validate(request.homepageUrl());
        }
        if (request.parserProfileCode() != null && !request.parserProfileCode().isBlank()
                && localGovernmentNoticeDao.selectParserProfileDetails(request.parserProfileCode().trim()) == null) {
            throw invalid("선택한 수집 파서를 찾을 수 없습니다.");
        }
    }

    /**
     * 저장 요청을 DAO 명령으로 변환합니다.
     */
    private LocalGovernmentNoticeSourceCommand toCommand(
            UUID sourceId,
            UUID actorUserId,
            LocalGovernmentNoticeSourceSaveRequest request
    ) {
        return new LocalGovernmentNoticeSourceCommand(
                sourceId, blankToNull(request.sidoCode()), request.sidoName().trim(), request.sigunguCode().trim(),
                request.sigunguName().trim(), normalizeOptional(request.institutionTypeCode()), request.institutionName().trim(),
                blankToNull(request.homepageUrl()), request.noticeUrl().trim(), blankToNull(request.pageTypeCode()),
                normalizeOptional(request.parserProfileCode()), blankToNull(request.collectionHint()),
                normalizeOptional(request.confidenceCode()), normalizeOptional(request.validationStatusCode()), actorUserId
        );
    }

    /**
     * 지자체 URL 조회 결과를 반환하거나 404 예외를 발생시킵니다.
     */
    private LocalGovernmentNoticeSourceRow selectSourceRow(UUID sourceId) {
        LocalGovernmentNoticeSourceRow row = localGovernmentNoticeDao.selectSourceDetails(sourceId);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    /**
     * 스케줄 조회 결과를 반환하거나 404 예외를 발생시킵니다.
     */
    private AnnouncementSourceScheduleRow selectScheduleRow(UUID scheduleId) {
        AnnouncementSourceScheduleRow row = localGovernmentNoticeDao.selectScheduleDetails(scheduleId);
        if (row == null) {
            throw notFound("수집 스케줄을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * 스케줄 응답을 조회합니다.
     */
    private AnnouncementSourceScheduleResponse selectScheduleResponse(UUID scheduleId) {
        return AnnouncementSourceScheduleResponse.from(selectScheduleRow(scheduleId));
    }

    /**
     * 스케줄 상태 전이를 검증합니다.
     */
    private void validateScheduleTransition(String currentStatus, String nextStatus) {
        boolean valid = switch (currentStatus) {
            case "APPROVAL_PENDING" -> Set.of("APPROVED", "REJECTED").contains(nextStatus);
            case "APPROVED" -> Set.of("PAUSED", "EXPIRED").contains(nextStatus);
            case "PAUSED" -> Set.of("APPROVED", "EXPIRED").contains(nextStatus);
            default -> false;
        };
        if (!valid) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "현재 상태에서 선택한 스케줄 상태로 변경할 수 없습니다.");
        }
    }

    /**
     * Spring cron 표현식을 검증합니다.
     */
    private CronExpression parseCron(String expression) {
        try {
            return CronExpression.parse(expression.trim());
        } catch (RuntimeException exception) {
            throw invalid("수집 일정 형식이 올바르지 않습니다. 초 분 시 일 월 요일 순서로 입력하세요.");
        }
    }

    /**
     * 시간대 식별자를 검증합니다.
     */
    private ZoneId parseZone(String timezone) {
        try {
            return ZoneId.of(timezone.trim());
        } catch (RuntimeException exception) {
            throw invalid("수집 일정의 시간대를 확인하세요.");
        }
    }

    /**
     * cron의 다음 실행 시각을 계산합니다.
     */
    private OffsetDateTime selectNextRun(CronExpression cron, ZoneId zone) {
        ZonedDateTime next = cron.next(ZonedDateTime.now(zone));
        if (next == null) {
            throw invalid("다음 실행 시각을 계산할 수 없는 수집 일정입니다.");
        }
        return next.toOffsetDateTime();
    }

    /**
     * 인증 사용자 식별자를 조회합니다.
     */
    private UUID selectActorUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails user)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return user.userId();
    }

    /**
     * URL·스케줄 변경의 비식별 감사 로그를 저장합니다.
     *
     * @param actorUserId 처리 사용자 식별자
     * @param actionCode 작업 코드
     * @param resourceType 대상 유형
     * @param resourceId 대상 식별자
     * @param metadataJson 상태 메타데이터
     */
    private void insertAudit(
            UUID actorUserId,
            String actionCode,
            String resourceType,
            UUID resourceId,
            String metadataJson
    ) {
        localGovernmentNoticeDao.insertAuditLog(new AnnouncementSourceAuditLogCommand(
                actorUserId, actionCode, resourceType, resourceId, "SUCCESS", metadataJson
        ));
    }

    /**
     * 필수 코드 값을 대문자로 정규화하고 허용 범위를 검증합니다.
     */
    private String normalizeRequired(String value, Set<String> allowed, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null || !allowed.contains(normalized)) {
            throw invalid(message);
        }
        return normalized;
    }

    /**
     * 선택 코드 값을 대문자로 정규화합니다.
     */
    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 공백 문자열을 null로 변환합니다.
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 사용자 입력 검증 예외를 생성합니다.
     */
    private ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 기본 지자체 URL 404 예외를 생성합니다.
     */
    private ApiException notFound() {
        return notFound("지자체 공고 URL을 찾을 수 없습니다.");
    }

    /**
     * 지정 메시지의 404 예외를 생성합니다.
     */
    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
