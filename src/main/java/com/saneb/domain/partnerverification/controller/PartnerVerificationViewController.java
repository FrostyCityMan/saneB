package com.saneb.domain.partnerverification.controller;

import com.saneb.common.error.ApiException;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.partnerverification.dto.PartnerVerificationDetailsResponse;
import com.saneb.domain.partnerverification.dto.PartnerVerificationSummaryResponse;
import com.saneb.domain.partnerverification.dto.VerificationDocumentsSaveRequest;
import com.saneb.domain.partnerverification.service.PartnerVerificationService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
public class PartnerVerificationViewController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AuthService authService;
    private final PartnerVerificationService partnerVerificationService;

    public PartnerVerificationViewController(
            AuthService authService,
            PartnerVerificationService partnerVerificationService
    ) {
        this.authService = authService;
        this.partnerVerificationService = partnerVerificationService;
    }

    @GetMapping("/app/partner/verifications")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public String selectPartnerVerificationListPage(
            Authentication authentication,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) Boolean current,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        UUID partnerUserId = isPartnerOnly(authMe) ? authMe.userId() : null;
        PageResponse<PartnerVerificationSummaryResponse> verificationPage =
                partnerVerificationService.selectPartnerVerificationList(
                        null,
                        partnerUserId,
                        blankToNull(statusCode),
                        current,
                        Math.max(page, 1),
                        20
                );

        model.addAttribute("page", VerificationListPageModel.from(
                authMe,
                verificationPage,
                blankToNull(statusCode),
                current
        ));
        return "app/partner-verification-list";
    }

    @GetMapping("/app/member/verifications/current")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public String selectCurrentVerificationProgressPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PartnerVerificationDetailsResponse details = selectCurrentVerification(authMe);

        model.addAttribute("page", VerificationPageModel.from(
                authMe,
                details,
                "VERIFICATION_PROGRESS",
                "사용자 검증 진행",
                false
        ));
        return "app/verification-progress";
    }

    @GetMapping("/app/member/verifications/{verificationId}")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public String selectVerificationProgressPage(
            Authentication authentication,
            @PathVariable UUID verificationId,
            Model model
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PartnerVerificationDetailsResponse details =
                partnerVerificationService.selectPartnerVerificationDetails(verificationId);
        ensureVerificationAccessible(authMe, details);

        model.addAttribute("page", VerificationPageModel.from(
                authMe,
                details,
                "VERIFICATION_PROGRESS",
                "사용자 검증 진행",
                false
        ));
        return "app/verification-progress";
    }

    @GetMapping("/app/partner-verifications/{verificationId}/input")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public String selectPartnerVerificationInputPage(
            Authentication authentication,
            @PathVariable UUID verificationId,
            Model model
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PartnerVerificationDetailsResponse details =
                partnerVerificationService.selectPartnerVerificationDetails(verificationId);

        model.addAttribute("page", VerificationPageModel.from(
                authMe,
                details,
                "PARTNER_VERIFICATION_INPUT",
                "파트너 검증 입력",
                true
        ));
        return "app/partner-verification-input";
    }

    @PostMapping("/app/member/verifications/{verificationId}/documents")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public String updateMemberVerificationDocuments(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @RequestParam(name = "documentTypeCode", required = false) List<String> documentTypeCodes,
            @RequestParam(name = "sourceTypeCode", required = false) List<String> sourceTypeCodes,
            @RequestParam(name = "note", required = false) List<String> notes,
            @RequestParam(name = "checkedIndex", required = false) List<Integer> checkedIndexes,
            RedirectAttributes redirectAttributes
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PartnerVerificationDetailsResponse details =
                partnerVerificationService.selectPartnerVerificationDetails(verificationId);
        ensureVerificationAccessible(authMe, details);
        return saveDocuments(authentication, verificationId, documentTypeCodes, sourceTypeCodes, notes, checkedIndexes,
                redirectAttributes, "redirect:/app/member/verifications/" + verificationId);
    }

    @PostMapping("/app/partner-verifications/{verificationId}/documents")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public String updatePartnerVerificationDocuments(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @RequestParam(name = "documentTypeCode", required = false) List<String> documentTypeCodes,
            @RequestParam(name = "sourceTypeCode", required = false) List<String> sourceTypeCodes,
            @RequestParam(name = "note", required = false) List<String> notes,
            @RequestParam(name = "checkedIndex", required = false) List<Integer> checkedIndexes,
            RedirectAttributes redirectAttributes
    ) {
        return saveDocuments(authentication, verificationId, documentTypeCodes, sourceTypeCodes, notes, checkedIndexes,
                redirectAttributes, "redirect:/app/partner-verifications/" + verificationId + "/input");
    }

    private String saveDocuments(
            Authentication authentication,
            UUID verificationId,
            List<String> documentTypeCodes,
            List<String> sourceTypeCodes,
            List<String> notes,
            List<Integer> checkedIndexes,
            RedirectAttributes redirectAttributes,
            String redirectPath
    ) {
        try {
            List<VerificationDocumentsSaveRequest.DocumentRequest> documents =
                    toDocumentRequests(documentTypeCodes, sourceTypeCodes, notes, checkedIndexes);
            partnerVerificationService.updateVerificationDocuments(
                    authentication,
                    verificationId,
                    new VerificationDocumentsSaveRequest(documents)
            );
            addSuccess(redirectAttributes, "검증 서류 확인 상태를 저장했습니다.");
        } catch (RuntimeException exception) {
            addError(redirectAttributes, exception);
        }
        return redirectPath;
    }

    private PartnerVerificationDetailsResponse selectCurrentVerification(AuthMeResponse authMe) {
        PageResponse<PartnerVerificationSummaryResponse> page =
                partnerVerificationService.selectPartnerVerificationList(authMe.userId(), null, null, true, 1, 1);
        if (page.items().isEmpty()) {
            return null;
        }
        return partnerVerificationService.selectPartnerVerificationDetails(page.items().getFirst().verificationId());
    }

    private static List<VerificationDocumentsSaveRequest.DocumentRequest> toDocumentRequests(
            List<String> documentTypeCodes,
            List<String> sourceTypeCodes,
            List<String> notes,
            List<Integer> checkedIndexes
    ) {
        List<String> safeDocumentTypeCodes = documentTypeCodes == null ? List.of() : documentTypeCodes;
        List<String> safeSourceTypeCodes = sourceTypeCodes == null ? List.of() : sourceTypeCodes;
        List<String> safeNotes = notes == null ? List.of() : notes;
        Set<Integer> checkedSet = new HashSet<>(checkedIndexes == null ? List.of() : checkedIndexes);

        return java.util.stream.IntStream.range(0, safeDocumentTypeCodes.size())
                .mapToObj(index -> new VerificationDocumentsSaveRequest.DocumentRequest(
                        safeDocumentTypeCodes.get(index),
                        index < safeSourceTypeCodes.size() ? safeSourceTypeCodes.get(index) : "PARTNER_CHECK",
                        checkedSet.contains(index),
                        index < safeNotes.size() ? safeNotes.get(index) : null
                ))
                .toList();
    }

    private static void ensureVerificationAccessible(AuthMeResponse auth, PartnerVerificationDetailsResponse details) {
        if (details == null) {
            return;
        }
        if (hasAnyRole(auth, "PARTNER", "OPERATOR", "APPROVER", "REVIEWER", "ADMIN")) {
            return;
        }
        if (!auth.userId().equals(details.memberUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verification is not accessible.");
        }
    }

    private static boolean hasAnyRole(AuthMeResponse auth, String... roles) {
        return auth.roles().stream().anyMatch(role -> Set.of(roles).contains(role));
    }

    private static boolean isPartnerOnly(AuthMeResponse auth) {
        return "PARTNER".equals(auth.primaryRole())
                && !hasAnyRole(auth, "OPERATOR", "APPROVER", "REVIEWER", "ADMIN");
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

    private static String verificationStatusLabel(String code) {
        return switch (code) {
            case "DRAFT" -> "작성 중";
            case "SUBMITTED" -> "제출 완료";
            case "REVIEWING" -> "검토 중";
            case "VERIFIED" -> "검증 완료";
            case "REJECTED" -> "반려";
            case "EXPIRED" -> "만료";
            default -> code;
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

    private static String sourceTypeLabel(String code) {
        return switch (code) {
            case "USER_UPLOAD" -> "사용자 제출";
            case "E_CERT" -> "전자증명";
            case "PARTNER_CHECK" -> "파트너 확인";
            case "OPERATOR_CHECK" -> "운영자 확인";
            default -> code;
        };
    }

    private static String dateTimeText(OffsetDateTime value) {
        return value == null ? "기록 없음" : value.format(DATE_TIME_FORMAT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record VerificationListPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            String selectedStatusCode,
            Boolean selectedCurrent,
            List<SummaryModel> items,
            long totalCount,
            int page,
            int size,
            boolean hasPreviousPage,
            boolean hasNextPage,
            int previousPage,
            int nextPage,
            boolean canOpenInput
    ) {

        private static VerificationListPageModel from(
                AuthMeResponse auth,
                PageResponse<PartnerVerificationSummaryResponse> verificationPage,
                String selectedStatusCode,
                Boolean selectedCurrent
        ) {
            return new VerificationListPageModel(
                    auth,
                    PartnerVerificationViewController.roleLabel(auth.primaryRole()),
                    "PARTNER_VERIFICATION_LIST",
                    selectedStatusCode,
                    selectedCurrent,
                    verificationPage.items().stream().map(SummaryModel::from).toList(),
                    verificationPage.totalCount(),
                    verificationPage.page(),
                    verificationPage.size(),
                    verificationPage.page() > 1,
                    verificationPage.page() * verificationPage.size() < verificationPage.totalCount(),
                    Math.max(verificationPage.page() - 1, 1),
                    verificationPage.page() + 1,
                    hasAnyRole(auth, "PARTNER", "OPERATOR", "ADMIN")
            );
        }
    }

    public record SummaryModel(
            UUID verificationId,
            UUID memberUserId,
            UUID partnerUserId,
            UUID businessProfileId,
            String statusCode,
            String statusLabel,
            boolean current,
            boolean matchingBlocked,
            String submittedAtText,
            String verifiedAtText,
            String updatedAtText
    ) {

        private static SummaryModel from(PartnerVerificationSummaryResponse response) {
            return new SummaryModel(
                    response.verificationId(),
                    response.memberUserId(),
                    response.partnerUserId(),
                    response.businessProfileId(),
                    response.statusCode(),
                    verificationStatusLabel(response.statusCode()),
                    response.current(),
                    response.matchingBlocked(),
                    dateTimeText(response.submittedAt()),
                    dateTimeText(response.verifiedAt()),
                    dateTimeText(response.updatedAt())
            );
        }
    }

    public record VerificationPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            String title,
            boolean operatorMode,
            boolean canEditDocuments,
            boolean canOpenPartnerInput,
            boolean empty,
            DetailsModel details
    ) {

        private static VerificationPageModel from(
                AuthMeResponse auth,
                PartnerVerificationDetailsResponse details,
                String activeNav,
                String title,
                boolean operatorMode
        ) {
            boolean canOperate = hasAnyRole(auth, "PARTNER", "OPERATOR", "ADMIN");
            return new VerificationPageModel(
                    auth,
                    PartnerVerificationViewController.roleLabel(auth.primaryRole()),
                    activeNav,
                    title,
                    operatorMode,
                    canOperate || hasAnyRole(auth, "USER"),
                    canOperate && details != null,
                    details == null,
                    details == null ? null : DetailsModel.from(details)
            );
        }
    }

    public record DetailsModel(
            UUID verificationId,
            UUID memberUserId,
            UUID partnerUserId,
            UUID businessProfileId,
            String statusCode,
            String statusLabel,
            boolean current,
            boolean matchingBlocked,
            String submittedAtText,
            String verifiedAtText,
            String reviewNote,
            List<DocumentModel> documents
    ) {

        private static DetailsModel from(PartnerVerificationDetailsResponse response) {
            return new DetailsModel(
                    response.verificationId(),
                    response.memberUserId(),
                    response.partnerUserId(),
                    response.businessProfileId(),
                    response.statusCode(),
                    verificationStatusLabel(response.statusCode()),
                    response.current(),
                    response.matchingBlocked(),
                    dateTimeText(response.submittedAt()),
                    dateTimeText(response.verifiedAt()),
                    response.reviewNote(),
                    response.documents().stream().map(DocumentModel::from).toList()
            );
        }
    }

    public record DocumentModel(
            String documentTypeCode,
            String documentTypeLabel,
            String sourceTypeCode,
            String sourceTypeLabel,
            boolean checked,
            String checkedAtText,
            UUID checkedBy,
            String note
    ) {

        private static DocumentModel from(PartnerVerificationDetailsResponse.DocumentResponse response) {
            return new DocumentModel(
                    response.documentTypeCode(),
                    PartnerVerificationViewController.documentTypeLabel(response.documentTypeCode()),
                    response.sourceTypeCode(),
                    PartnerVerificationViewController.sourceTypeLabel(response.sourceTypeCode()),
                    response.checked(),
                    dateTimeText(response.checkedAt()),
                    response.checkedBy(),
                    response.note()
            );
        }
    }
}
