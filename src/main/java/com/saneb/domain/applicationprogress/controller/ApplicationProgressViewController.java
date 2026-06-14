package com.saneb.domain.applicationprogress.controller;

import com.saneb.common.error.ApiException;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressDetailsResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressStartRequest;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressSummaryResponse;
import com.saneb.domain.applicationprogress.dto.ProgressActionRequest;
import com.saneb.domain.applicationprogress.dto.ProgressChecklistSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressReceiptSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressResultSaveRequest;
import com.saneb.domain.applicationprogress.service.ApplicationProgressService;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.service.MatchingService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ApplicationProgressViewController {

    private static final NumberFormat KOREAN_NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AuthService authService;
    private final ApplicationProgressService applicationProgressService;
    private final MatchingService matchingService;

    public ApplicationProgressViewController(
            AuthService authService,
            ApplicationProgressService applicationProgressService,
            MatchingService matchingService
    ) {
        this.authService = authService;
        this.applicationProgressService = applicationProgressService;
        this.matchingService = matchingService;
    }

    @GetMapping("/app/application-progresses")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public String selectApplicationProgressListPage(
            Authentication authentication,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PageResponse<ApplicationProgressSummaryResponse> progressPage =
                applicationProgressService.selectApplicationProgressList(
                        authentication,
                        null,
                        null,
                        null,
                        blankToNull(statusCode),
                        Math.max(page, 1),
                        20
                );

        model.addAttribute("page", ApplicationProgressPageModel.from(
                authMe,
                progressPage,
                null,
                blankToNull(statusCode),
                null,
                selectStartableMatchings(authMe)
        ));
        return "app/application-progress-detail";
    }

    @GetMapping("/app/application-progresses/{progressId}")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public String selectApplicationProgressDetailsPage(
            Authentication authentication,
            @PathVariable UUID progressId,
            Model model
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        ApplicationProgressDetailsResponse details =
                applicationProgressService.selectApplicationProgressDetails(progressId);
        ensureProgressAccessible(authMe, details);

        PageResponse<ApplicationProgressSummaryResponse> progressPage =
                applicationProgressService.selectApplicationProgressList(authentication, null, null, null, null, 1, 20);

        model.addAttribute("page", ApplicationProgressPageModel.from(
                authMe,
                progressPage,
                details,
                null,
                selectCurrentStep(details),
                selectStartableMatchings(authMe)
        ));
        return "app/application-progress-detail";
    }

    @PostMapping("/app/application-progresses/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public String insertApplicationProgress(
            Authentication authentication,
            @RequestParam UUID matchingCaseId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ApplicationProgressDetailsResponse details = applicationProgressService.insertApplicationProgress(
                    authentication,
                    new ApplicationProgressStartRequest(matchingCaseId)
            );
            addSuccess(redirectAttributes, "공고 신청 진행을 시작했습니다.");
            return "redirect:/app/application-progresses/" + details.progressId();
        } catch (RuntimeException exception) {
            addError(redirectAttributes, exception);
            return "redirect:/app/application-progresses";
        }
    }

    @PostMapping("/app/application-progresses/{progressId}/steps/{stepId}/documents")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public String saveProgressStepDocuments(
            Authentication authentication,
            @PathVariable UUID progressId,
            @PathVariable UUID stepId,
            @RequestParam(name = "stepDocumentId", required = false) List<UUID> stepDocumentIds,
            @RequestParam(name = "checkedDocumentId", required = false) List<UUID> checkedDocumentIds,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Set<UUID> checkedSet = new HashSet<>(checkedDocumentIds == null ? List.of() : checkedDocumentIds);
            List<ProgressChecklistSaveRequest.DocumentRequest> documents =
                    (stepDocumentIds == null ? List.<UUID>of() : stepDocumentIds).stream()
                            .map(stepDocumentId -> new ProgressChecklistSaveRequest.DocumentRequest(
                                    stepDocumentId,
                                    checkedSet.contains(stepDocumentId)
                            ))
                            .toList();
            applicationProgressService.saveProgressStepDocuments(
                    authentication,
                    progressId,
                    stepId,
                    new ProgressChecklistSaveRequest(documents)
            );
            addSuccess(redirectAttributes, "체크리스트를 저장했습니다.");
        } catch (RuntimeException exception) {
            addError(redirectAttributes, exception);
        }
        return "redirect:/app/application-progresses/" + progressId;
    }

    @PostMapping("/app/application-progresses/{progressId}/steps/{stepId}/action")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public String updateProgressStepAction(
            Authentication authentication,
            @PathVariable UUID progressId,
            @PathVariable UUID stepId,
            @RequestParam String buttonCode,
            RedirectAttributes redirectAttributes
    ) {
        try {
            applicationProgressService.updateProgressStepAction(
                    authentication,
                    progressId,
                    stepId,
                    new ProgressActionRequest(buttonCode, Map.of())
            );
            addSuccess(redirectAttributes, "단계 행동을 처리했습니다.");
        } catch (RuntimeException exception) {
            addError(redirectAttributes, exception);
        }
        return "redirect:/app/application-progresses/" + progressId;
    }

    @PostMapping("/app/application-progresses/{progressId}/receipt")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public String updateProgressReceipt(
            Authentication authentication,
            @PathVariable UUID progressId,
            @RequestParam String receiptNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiptDate,
            RedirectAttributes redirectAttributes
    ) {
        try {
            applicationProgressService.updateProgressReceipt(
                    authentication,
                    progressId,
                    new ProgressReceiptSaveRequest(receiptNo, receiptDate)
            );
            addSuccess(redirectAttributes, "접수 정보를 저장했습니다.");
        } catch (RuntimeException exception) {
            addError(redirectAttributes, exception);
        }
        return "redirect:/app/application-progresses/" + progressId;
    }

    @PostMapping("/app/application-progresses/{progressId}/result")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public String updateProgressResult(
            Authentication authentication,
            @PathVariable UUID progressId,
            @RequestParam String resultCode,
            @RequestParam(required = false) String resultNote,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate resultDate,
            @RequestParam(required = false) BigDecimal receivedAmount,
            RedirectAttributes redirectAttributes
    ) {
        try {
            applicationProgressService.updateProgressResult(
                    authentication,
                    progressId,
                    new ProgressResultSaveRequest(resultCode, resultNote, resultDate, receivedAmount)
            );
            addSuccess(redirectAttributes, "최종 결과를 저장했습니다.");
        } catch (RuntimeException exception) {
            addError(redirectAttributes, exception);
        }
        return "redirect:/app/application-progresses/" + progressId;
    }

    private static void ensureProgressAccessible(AuthMeResponse auth, ApplicationProgressDetailsResponse details) {
        if (hasAnyRole(auth, "PARTNER", "OPERATOR", "APPROVER", "REVIEWER", "ADMIN")) {
            return;
        }
        if (!auth.userId().equals(details.memberUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Application progress is not accessible.");
        }
    }

    private static StepStateModel selectCurrentStep(ApplicationProgressDetailsResponse details) {
        if (details == null || details.currentStepId() == null || details.stepStates() == null) {
            return null;
        }
        return details.stepStates().stream()
                .filter(step -> details.currentStepId().equals(step.stepId()))
                .findFirst()
                .map(StepStateModel::from)
                .orElse(null);
    }

    private static boolean hasAnyRole(AuthMeResponse auth, String... roles) {
        return auth.roles().stream().anyMatch(role -> Set.of(roles).contains(role));
    }

    private List<StartableMatchingModel> selectStartableMatchings(AuthMeResponse authMe) {
        return List.of();
    }

    private static void addSuccess(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("messageType", "success");
        redirectAttributes.addFlashAttribute("message", message);
    }

    private static void addError(RedirectAttributes redirectAttributes, RuntimeException exception) {
        redirectAttributes.addFlashAttribute("messageType", "error");
        redirectAttributes.addFlashAttribute("message", selectErrorMessage(exception));
    }

    private static String selectErrorMessage(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.getMessage();
        }
        return "요청 처리 중 오류가 발생했습니다.";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String roleLabel(String code) {
        return switch (code) {
            case "ADMIN" -> "관리자";
            case "APPROVER" -> "승인자";
            case "OPERATOR" -> "운영자";
            case "REVIEWER" -> "검수자";
            case "PARTNER" -> "파트너";
            default -> "일반 사용자";
        };
    }

    private static String progressStatusLabel(String code) {
        return switch (code) {
            case "READY" -> "준비";
            case "IN_PROGRESS" -> "진행 중";
            case "WAITING_RESULT" -> "결과 대기";
            case "APPROVED" -> "승인";
            case "REJECTED" -> "탈락";
            case "SUPPLEMENT_REQUESTED" -> "보완 요청";
            case "STOPPED" -> "중단";
            case "COMPLETED" -> "완료";
            default -> code;
        };
    }

    private static String matchingStatusLabel(String code) {
        return switch (code) {
            case "MATCHED" -> "신청 가능";
            case "REVIEW_REQUIRED" -> "확인 필요";
            case "PROGRESSED" -> "진행 중";
            case "BLOCKED" -> "진행 불가";
            case "NOT_MATCHED" -> "대상 아님";
            default -> code;
        };
    }

    private static String stepStatusLabel(String code) {
        return switch (code) {
            case "LOCKED" -> "잠김";
            case "READY" -> "준비";
            case "IN_PROGRESS" -> "진행 중";
            case "COMPLETED" -> "완료";
            case "SKIPPED" -> "건너뜀";
            case "BLOCKED" -> "차단";
            default -> code;
        };
    }

    private static String stepButtonLabel(ApplicationProgressDetailsResponse.StepButtonResponse response) {
        if (response.buttonLabel() != null && !response.buttonLabel().isBlank()) {
            return response.buttonLabel();
        }
        return switch (response.buttonActionCode()) {
            case "MOVE_NEXT" -> "다음 단계로 진행";
            case "COMPLETE_STEP" -> "현재 단계 완료";
            default -> "진행 처리";
        };
    }

    private static String stepButtonActionLabel(String code) {
        return switch (code) {
            case "MOVE_NEXT" -> "다음 단계 이동";
            case "COMPLETE_STEP" -> "단계 완료";
            default -> "진행 처리";
        };
    }

    private static String documentTypeLabel(String code) {
        return switch (code) {
            case "BUSINESS_REGISTRATION" -> "사업자등록증";
            case "VAT_TAX_BASE" -> "부가세 과세표준";
            case "TAX_EXEMPT_INCOME" -> "면세사업자 수입금액";
            case "INCOME_CERTIFICATE" -> "소득금액증명";
            case "NATIONAL_TAX_PAID" -> "국세 납세증명";
            case "LOCAL_TAX_PAID" -> "지방세 납세증명";
            case "RESIDENT_REGISTRATION" -> "주민등록";
            case "FAMILY_RELATION" -> "가족관계";
            case "HEALTH_INSURANCE_PAYMENT" -> "건강보험 납부확인";
            case "HEALTH_INSURANCE_QUALIFICATION" -> "건강보험 자격확인";
            default -> code;
        };
    }

    private static String amountText(BigDecimal amount) {
        if (amount == null) {
            return "미입력";
        }
        return KOREAN_NUMBER_FORMAT.format(amount) + "원";
    }

    private static String dateTimeText(OffsetDateTime value) {
        return value == null ? "기록 없음" : value.format(DATE_TIME_FORMAT);
    }

    private static List<StepButtonModel> selectCurrentStepButtons(
            ApplicationProgressDetailsResponse details,
            StepStateModel currentStep
    ) {
        if (details == null || currentStep == null || details.stepButtons() == null) {
            return List.of();
        }
        return details.stepButtons().stream()
                .filter(button -> currentStep.stepId().equals(button.stepId()))
                .map(StepButtonModel::from)
                .toList();
    }

    public record ApplicationProgressPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            String selectedStatusCode,
            boolean canOperate,
            List<StartableMatchingModel> startableMatchings,
            List<SummaryModel> progressItems,
            long totalCount,
            DetailsModel details,
            StepStateModel currentStep,
            List<StepButtonModel> currentStepButtons
    ) {

        private static ApplicationProgressPageModel from(
                AuthMeResponse auth,
                PageResponse<ApplicationProgressSummaryResponse> progressPage,
                ApplicationProgressDetailsResponse details,
                String selectedStatusCode,
                StepStateModel currentStep,
                List<StartableMatchingModel> startableMatchings
        ) {
            DetailsModel detailsModel = details == null ? null : DetailsModel.from(details);
            return new ApplicationProgressPageModel(
                    auth,
                    ApplicationProgressViewController.roleLabel(auth.primaryRole()),
                    "APPLICATION_PROGRESS",
                    selectedStatusCode,
                    hasAnyRole(auth, "PARTNER", "OPERATOR", "ADMIN"),
                    startableMatchings,
                    progressPage.items().stream().map(SummaryModel::from).toList(),
                    progressPage.totalCount(),
                    detailsModel,
                    currentStep,
                    selectCurrentStepButtons(details, currentStep)
            );
        }
    }

    public record StartableMatchingModel(
            UUID matchingCaseId,
            UUID announcementId,
            String matchingCaseCode,
            String announcementCode,
            String statusLabel,
            String matchedAtText
    ) {

        private static StartableMatchingModel from(MatchingCaseSummaryResponse response) {
            return new StartableMatchingModel(
                    response.matchingCaseId(),
                    response.announcementId(),
                    response.matchingCaseCode(),
                    response.announcementCode(),
                    matchingStatusLabel(response.statusCode()),
                    dateTimeText(response.matchedAt())
            );
        }
    }

    public record SummaryModel(
            UUID progressId,
            String progressCode,
            String statusCode,
            String statusLabel,
            String receiptNo,
            String receiptDateText,
            String resultCode,
            String resultDateText,
            String receivedAmountText,
            String updatedAtText
    ) {

        private static SummaryModel from(ApplicationProgressSummaryResponse response) {
            return new SummaryModel(
                    response.progressId(),
                    response.progressCode(),
                    response.statusCode(),
                    progressStatusLabel(response.statusCode()),
                    response.receiptNo() == null ? "접수 전" : response.receiptNo(),
                    response.receiptDate() == null ? "접수일 미입력" : response.receiptDate().toString(),
                    response.resultCode() == null ? "결과 대기" : progressStatusLabel(response.resultCode()),
                    response.resultDate() == null ? "결과일 미입력" : response.resultDate().toString(),
                    amountText(response.receivedAmount()),
                    dateTimeText(response.updatedAt())
            );
        }
    }

    public record DetailsModel(
            UUID progressId,
            UUID matchingCaseId,
            UUID announcementId,
            UUID memberUserId,
            String progressCode,
            String matchingCaseCode,
            String announcementCode,
            String memberUserCode,
            UUID currentStepId,
            String statusCode,
            String statusLabel,
            String receiptNo,
            String receiptDateText,
            String resultCode,
            String resultLabel,
            String resultNote,
            String resultDateText,
            String receivedAmountText,
            List<StepStateModel> stepStates,
            List<ChecklistModel> checklists,
            List<StepButtonModel> stepButtons
    ) {

        private static DetailsModel from(ApplicationProgressDetailsResponse response) {
            return new DetailsModel(
                    response.progressId(),
                    response.matchingCaseId(),
                    response.announcementId(),
                    response.memberUserId(),
                    response.progressCode(),
                    response.matchingCaseCode(),
                    response.announcementCode(),
                    response.memberUserCode(),
                    response.currentStepId(),
                    response.statusCode(),
                    progressStatusLabel(response.statusCode()),
                    response.receiptNo(),
                    response.receiptDate() == null ? "접수일 미입력" : response.receiptDate().toString(),
                    response.resultCode(),
                    response.resultCode() == null ? "결과 대기" : progressStatusLabel(response.resultCode()),
                    response.resultNote(),
                    response.resultDate() == null ? "결과일 미입력" : response.resultDate().toString(),
                    amountText(response.receivedAmount()),
                    response.stepStates().stream().map(StepStateModel::from).toList(),
                    response.checklists().stream().map(ChecklistModel::from).toList(),
                    response.stepButtons().stream().map(StepButtonModel::from).toList()
            );
        }
    }

    public record StepButtonModel(
            UUID stepId,
            String buttonCode,
            String buttonLabel,
            String actionLabel
    ) {

        private static StepButtonModel from(ApplicationProgressDetailsResponse.StepButtonResponse response) {
            return new StepButtonModel(
                    response.stepId(),
                    response.buttonCode(),
                    stepButtonLabel(response),
                    stepButtonActionLabel(response.buttonActionCode())
            );
        }
    }

    public record StepStateModel(
            UUID stepStateId,
            UUID stepId,
            int stepOrder,
            String stepName,
            String statusCode,
            String statusLabel,
            String startedAtText,
            String completedAtText
    ) {

        private static StepStateModel from(ApplicationProgressDetailsResponse.StepStateResponse response) {
            return new StepStateModel(
                    response.stepStateId(),
                    response.stepId(),
                    response.stepOrder(),
                    response.stepName(),
                    response.statusCode(),
                    stepStatusLabel(response.statusCode()),
                    dateTimeText(response.startedAt()),
                    dateTimeText(response.completedAt())
            );
        }
    }

    public record ChecklistModel(
            UUID checklistId,
            UUID stepDocumentId,
            UUID stepId,
            String documentTypeCode,
            String documentTypeLabel,
            boolean required,
            boolean checked,
            String checkedAtText,
            UUID checkedBy
    ) {

        private static ChecklistModel from(ApplicationProgressDetailsResponse.ChecklistResponse response) {
            return new ChecklistModel(
                    response.checklistId(),
                    response.stepDocumentId(),
                    response.stepId(),
                    response.documentTypeCode(),
                    ApplicationProgressViewController.documentTypeLabel(response.documentTypeCode()),
                    response.required(),
                    response.checked(),
                    dateTimeText(response.checkedAt()),
                    response.checkedBy()
            );
        }
    }
}
