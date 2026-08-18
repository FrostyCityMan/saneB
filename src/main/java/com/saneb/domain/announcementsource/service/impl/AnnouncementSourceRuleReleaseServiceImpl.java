package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TermTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRule;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationTerm;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceTextNormalizer;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceRuleReleaseDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleDeleteRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleSaveRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleStatusUpdateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePreviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePreviewResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleReleaseCreateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleReleaseSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceRuleGoldenGate.GoldenGateResult;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleUpdateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordTermInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleGroupRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleReleaseInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleReleaseRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleReleaseSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleTermRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 규칙 release의 편집·미리보기·게시 업무 규칙을 수행합니다. */
@Service
public class AnnouncementSourceRuleReleaseServiceImpl implements AnnouncementSourceRuleReleaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final char SYNONYM_SEPARATOR = 31;
    private static final String RELEASE_RESOURCE = "ANNOUNCEMENT_SOURCE_RULE_RELEASE";
    private static final String RULE_RESOURCE = "ANNOUNCEMENT_SOURCE_KEYWORD_RULE";
    private static final String CHANGE_REASON_PROVIDED_METADATA_KEY = "changeReasonProvided";
    private static final String CHANGE_REASON_LENGTH_METADATA_KEY = "changeReasonLength";
    private static final String CHANGE_REASON_SHA256_METADATA_KEY = "changeReasonSha256";
    private static final Set<String> RELEASE_STATUS_CODES = Set.of("DRAFT", "ACTIVE", "RETIRED");
    private static final Set<String> GROUP_KIND_CODES = Set.of(
            "TARGET", "SUPPORT_TYPE", "REVIEW_A", "AUTO_EXCLUDE_B", "CONTEXT", "PROTECTED_METADATA"
    );
    private static final Set<String> STRENGTH_CODES = Set.of("STRONG", "SUPPLEMENTARY");
    private static final Set<String> MATCH_MODE_CODES = Set.of("NORMALIZED_PHRASE", "TOKEN", "EXACT_TITLE");

    private final AnnouncementSourceRuleReleaseDao ruleReleaseDao;
    private final AnnouncementSourceRuleGoldenGate goldenGate;
    private final AnnouncementSourceClassificationEngine classificationEngine;
    private final AnnouncementSourceTextNormalizer textNormalizer;

    public AnnouncementSourceRuleReleaseServiceImpl(
            AnnouncementSourceRuleReleaseDao ruleReleaseDao,
            AnnouncementSourceRuleGoldenGate goldenGate
    ) {
        this.ruleReleaseDao = ruleReleaseDao;
        this.goldenGate = goldenGate;
        this.classificationEngine = new AnnouncementSourceClassificationEngine();
        this.textNormalizer = new AnnouncementSourceTextNormalizer();
    }

    @Override
    public PageResponse<AnnouncementSourceRuleReleaseSummaryResponse> selectRuleReleaseList(
            String releaseStatusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        String normalizedStatus = normalizeOptionalCode(releaseStatusCode);
        validateOptionalCode("releaseStatusCode", normalizedStatus, RELEASE_STATUS_CODES);
        AnnouncementSourceRuleReleaseSearchCondition condition = new AnnouncementSourceRuleReleaseSearchCondition(
                normalizedStatus,
                size,
                (page - 1) * size
        );
        long totalCount = ruleReleaseDao.selectRuleReleaseCount(condition);
        List<AnnouncementSourceRuleReleaseSummaryResponse> items = ruleReleaseDao
                .selectRuleReleaseList(condition)
                .stream()
                .map(this::toReleaseResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public AnnouncementSourceRuleReleaseSummaryResponse insertRuleReleaseDraft(
            Authentication authentication,
            AnnouncementSourceRuleReleaseCreateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AnnouncementSourceRuleReleaseRow active = ruleReleaseDao.selectActiveRuleReleaseDetailsForUpdate();
        if (active == null) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_ACTIVE,
                    HttpStatus.CONFLICT,
                    "복제할 ACTIVE 규칙 버전이 없습니다. 초기 DRAFT를 먼저 게시하세요."
            );
        }
        if (!active.rowVersion().equals(request.expectedVersion())) {
            throw versionConflict();
        }

        int versionNo = ruleReleaseDao.selectNextRuleReleaseVersionNo();
        UUID releaseId = UUID.randomUUID();
        AnnouncementSourceRuleReleaseInsertCommand command = new AnnouncementSourceRuleReleaseInsertCommand(
                releaseId,
                active.releaseId(),
                "ASCR-" + String.format(Locale.ROOT, "%06d", versionNo),
                versionNo,
                actorUserId,
                request.changeReason().trim()
        );
        ruleReleaseDao.insertClonedRuleRelease(command);
        ruleReleaseDao.insertClonedRuleGroupList(command);
        ruleReleaseDao.insertClonedKeywordRuleList(command);
        ruleReleaseDao.insertClonedKeywordTermList(command);
        insertAudit(actorUserId, "RULE_RELEASE_DRAFT_CREATED", RELEASE_RESOURCE, releaseId, metadataWithChangeReason(
                request.changeReason(),
                "sourceReleaseId", active.releaseId().toString(),
                "versionNo", String.valueOf(versionNo),
                "expectedVersion", request.expectedVersion().toString()
        ));
        return toReleaseResponse(selectReleaseDetails(releaseId));
    }

    @Override
    public PageResponse<AnnouncementSourceKeywordRuleSummaryResponse> selectKeywordRuleList(
            UUID releaseId,
            String groupKindCode,
            String groupCode,
            String strengthCode,
            String matchModeCode,
            Boolean enabled,
            String keyword,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        selectReleaseDetails(releaseId);
        String normalizedGroupKind = normalizeOptionalCode(groupKindCode);
        String normalizedGroupCode = normalizeOptionalCode(groupCode);
        String normalizedStrength = normalizeOptionalCode(strengthCode);
        String normalizedMatchMode = normalizeOptionalCode(matchModeCode);
        validateOptionalCode("groupKindCode", normalizedGroupKind, GROUP_KIND_CODES);
        validateOptionalCode("strengthCode", normalizedStrength, STRENGTH_CODES);
        validateOptionalCode("matchModeCode", normalizedMatchMode, MATCH_MODE_CODES);
        AnnouncementSourceKeywordRuleSearchCondition condition = new AnnouncementSourceKeywordRuleSearchCondition(
                releaseId,
                normalizedGroupKind,
                normalizedGroupCode,
                normalizedStrength,
                normalizedMatchMode,
                enabled,
                trimToNull(keyword),
                size,
                (page - 1) * size
        );
        long totalCount = ruleReleaseDao.selectKeywordRuleCount(condition);
        List<AnnouncementSourceKeywordRuleSummaryResponse> items = ruleReleaseDao
                .selectKeywordRuleList(condition)
                .stream()
                .map(this::toRuleResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public AnnouncementSourceKeywordRuleSummaryResponse insertKeywordRule(
            Authentication authentication,
            UUID releaseId,
            AnnouncementSourceKeywordRuleSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AnnouncementSourceRuleReleaseRow release = selectDraftReleaseForUpdate(releaseId);
        if (!release.rowVersion().equals(request.expectedVersion())) {
            throw versionConflict();
        }
        AnnouncementSourceRuleGroupRow group = selectEnabledGroup(releaseId, request.ruleGroupCode());
        NormalizedRuleInput input = normalizeRuleInput(request, group, null);
        validateNoDuplicateTerm(group.groupId(), input, null);
        if (ruleReleaseDao.updateRuleReleaseRowVersionExpected(
                releaseId,
                request.expectedVersion(),
                input.changeReason()
        ) == 0) {
            throw versionConflict();
        }

        UUID ruleId = UUID.randomUUID();
        String ruleCode = group.groupCode() + "_CUSTOM_"
                + ruleId.toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        ruleReleaseDao.insertKeywordRule(new AnnouncementSourceKeywordRuleInsertCommand(
                ruleId,
                group.groupId(),
                ruleCode,
                input.strengthCode(),
                input.sortOrder(),
                actorUserId
        ));
        insertTerms(ruleId, group, input, actorUserId);
        insertAudit(actorUserId, "KEYWORD_RULE_CREATED", RULE_RESOURCE, ruleId, metadataWithChangeReason(
                input.changeReason(),
                "releaseId", releaseId.toString(),
                "groupCode", group.groupCode(),
                "synonymCount", String.valueOf(input.synonyms().size())
        ));
        return toRuleResponse(selectRuleDetails(releaseId, ruleId));
    }

    @Override
    @Transactional
    public AnnouncementSourceKeywordRuleSummaryResponse updateKeywordRule(
            Authentication authentication,
            UUID releaseId,
            UUID ruleId,
            AnnouncementSourceKeywordRuleSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        selectDraftReleaseForUpdate(releaseId);
        AnnouncementSourceKeywordRuleRow before = selectRuleDetailsForUpdate(releaseId, ruleId);
        AnnouncementSourceRuleGroupRow group = selectEnabledGroup(releaseId, request.ruleGroupCode());
        NormalizedRuleInput input = normalizeRuleInput(request, group, before);
        validateNoDuplicateTerm(group.groupId(), input, ruleId);
        if (!before.ruleRowVersion().equals(request.expectedVersion())) {
            throw versionConflict();
        }

        ruleReleaseDao.deleteKeywordTermList(ruleId);
        int updated = ruleReleaseDao.updateKeywordRule(new AnnouncementSourceKeywordRuleUpdateCommand(
                ruleId,
                group.groupId(),
                input.strengthCode(),
                input.sortOrder(),
                request.expectedVersion(),
                actorUserId
        ));
        if (updated == 0) {
            throw versionConflict();
        }
        insertTerms(ruleId, group, input, actorUserId);
        if (ruleReleaseDao.updateRuleReleaseRowVersion(releaseId, input.changeReason()) == 0) {
            throw notDraft();
        }
        insertAudit(actorUserId, "KEYWORD_RULE_UPDATED", RULE_RESOURCE, ruleId, metadataWithChangeReason(
                input.changeReason(),
                "releaseId", releaseId.toString(),
                "beforeGroupCode", before.groupCode(),
                "afterGroupCode", group.groupCode()
        ));
        return toRuleResponse(selectRuleDetails(releaseId, ruleId));
    }

    @Override
    @Transactional
    public AnnouncementSourceKeywordRuleSummaryResponse updateKeywordRuleStatus(
            Authentication authentication,
            UUID releaseId,
            UUID ruleId,
            AnnouncementSourceKeywordRuleStatusUpdateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        selectDraftReleaseForUpdate(releaseId);
        AnnouncementSourceKeywordRuleRow before = selectRuleDetailsForUpdate(releaseId, ruleId);
        if (!before.ruleRowVersion().equals(request.expectedVersion())
                || ruleReleaseDao.updateKeywordRuleStatus(
                        ruleId,
                        request.enabled(),
                        request.expectedVersion(),
                        actorUserId
                ) == 0) {
            throw versionConflict();
        }
        if (ruleReleaseDao.updateRuleReleaseRowVersion(releaseId, request.changeReason().trim()) == 0) {
            throw notDraft();
        }
        insertAudit(actorUserId, "KEYWORD_RULE_STATUS_UPDATED", RULE_RESOURCE, ruleId, metadataWithChangeReason(
                request.changeReason(),
                "releaseId", releaseId.toString(),
                "beforeEnabled", String.valueOf(before.ruleEnabled()),
                "afterEnabled", String.valueOf(request.enabled())
        ));
        return toRuleResponse(selectRuleDetails(releaseId, ruleId));
    }

    @Override
    @Transactional
    public void deleteKeywordRule(
            Authentication authentication,
            UUID releaseId,
            UUID ruleId,
            AnnouncementSourceKeywordRuleDeleteRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        selectDraftReleaseForUpdate(releaseId);
        AnnouncementSourceKeywordRuleRow rule = selectRuleDetailsForUpdate(releaseId, ruleId);
        if (!rule.ruleRowVersion().equals(request.expectedVersion())
                || ruleReleaseDao.deleteKeywordRule(ruleId, request.expectedVersion()) == 0) {
            throw versionConflict();
        }
        if (ruleReleaseDao.updateRuleReleaseRowVersion(releaseId, request.changeReason().trim()) == 0) {
            throw notDraft();
        }
        insertAudit(actorUserId, "KEYWORD_RULE_DELETED", RULE_RESOURCE, ruleId, metadataWithChangeReason(
                request.changeReason(),
                "releaseId", releaseId.toString(),
                "groupCode", rule.groupCode(),
                "ruleCode", rule.ruleCode()
        ));
    }

    @Override
    public AnnouncementSourceRulePreviewResponse selectPreview(
            UUID releaseId,
            AnnouncementSourceRulePreviewRequest request
    ) {
        AnnouncementSourceRuleReleaseRow release = selectReleaseDetails(releaseId);
        if (!release.rowVersion().equals(request.expectedVersion())) {
            throw versionConflict();
        }
        AnnouncementSourceClassificationRuleSet ruleSet = selectRuleSet(
                releaseId,
                ruleReleaseDao.selectRuleTermList(releaseId)
        );
        boolean bodyAvailable = request.bodyText() != null && !request.bodyText().isBlank();
        AnnouncementSourceClassificationResult result = classificationEngine.selectDecision(
                new AnnouncementSourceClassificationInput(
                        request.providerCode() == null || request.providerCode().isBlank()
                                ? "PREVIEW" : request.providerCode().trim(),
                        request.title(),
                        request.bodyText(),
                        trimToNull(request.agencyName()),
                        request.agencyAliases(),
                        bodyAvailable ? BodySourceCode.PROVIDER_FULL_TEXT : BodySourceCode.NONE,
                        bodyAvailable ? BodyAvailabilityCode.AVAILABLE : BodyAvailabilityCode.UNAVAILABLE
                ),
                ruleSet
        );
        return toPreviewResponse(release, result);
    }

    @Override
    @Transactional
    public AnnouncementSourceRulePublicationResponse updateRuleReleasePublication(
            Authentication authentication,
            UUID releaseId,
            AnnouncementSourceRulePublicationRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AnnouncementSourceRuleReleaseRow candidate = selectDraftReleaseForUpdate(releaseId);
        if (!candidate.rowVersion().equals(request.expectedVersion())) {
            throw versionConflict();
        }
        List<AnnouncementSourceRuleTermRow> candidateRows = ruleReleaseDao.selectRuleTermList(releaseId);
        String snapshotHash = selectSnapshotHash(candidateRows);
        AnnouncementSourceClassificationRuleSet candidateRuleSet = selectRuleSet(releaseId, candidateRows);
        GoldenGateResult candidateGolden = goldenGate.selectValidatedResult(candidateRuleSet, snapshotHash);
        if (!candidateGolden.runId().equals(request.goldenSetRunId().trim())) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID,
                    HttpStatus.CONFLICT,
                    "현재 초안과 일치하는 최신 QA 정답 세트 실행 결과가 필요합니다. QA를 다시 실행해 주세요."
            );
        }

        AnnouncementSourceRuleReleaseRow previousActive = ruleReleaseDao.selectActiveRuleReleaseDetailsForUpdate();
        List<AnnouncementSourceRuleTermRow> previousRows = previousActive == null
                ? List.of() : ruleReleaseDao.selectRuleTermList(previousActive.releaseId());
        GoldenGateResult previousGolden = null;
        if (previousActive != null && !previousRows.isEmpty()) {
            previousGolden = goldenGate.selectValidatedResult(
                    selectRuleSet(previousActive.releaseId(), previousRows),
                    selectSnapshotHash(previousRows)
            );
        }
        RuleChangeCounts counts = selectRuleChangeCounts(previousRows, candidateRows);

        if (previousActive != null
                && ruleReleaseDao.updateActiveRuleReleaseRetired(releaseId, request.changeReason().trim()) != 1) {
            throw versionConflict();
        }
        if (ruleReleaseDao.updateRuleReleaseActive(
                releaseId,
                request.expectedVersion(),
                snapshotHash,
                request.changeReason().trim(),
                actorUserId
        ) != 1) {
            throw versionConflict();
        }

        insertAudit(actorUserId, "RULE_RELEASE_PUBLISHED", RELEASE_RESOURCE, releaseId, metadataWithChangeReason(
                request.changeReason(),
                "serverGoldenRunId", candidateGolden.runId(),
                "requestedGoldenRunId", request.goldenSetRunId().trim(),
                "goldenCaseCount", String.valueOf(candidateGolden.caseCount())
        ));
        AnnouncementSourceRuleReleaseSummaryResponse previousResponse = previousActive == null
                ? null : toReleaseResponse(selectReleaseDetails(previousActive.releaseId()));
        return new AnnouncementSourceRulePublicationResponse(
                previousResponse,
                toReleaseResponse(selectReleaseDetails(releaseId)),
                counts.added(),
                counts.modified(),
                counts.disabled(),
                candidateGolden.selectChangedCaseCount(previousGolden),
                candidateGolden.runId(),
                candidateGolden.caseCount()
        );
    }

    @Override
    public AnnouncementSourceClassificationRuleSet selectActiveRuleSet(UUID releaseId) {
        AnnouncementSourceRuleReleaseRow release = selectReleaseDetails(releaseId);
        if (!"ACTIVE".equals(release.releaseStatusCode())) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_ACTIVE,
                    HttpStatus.CONFLICT,
                    "적용 중인 규칙 버전만 재분류에 사용할 수 있습니다."
            );
        }
        return selectRuleSet(releaseId, ruleReleaseDao.selectRuleTermList(releaseId));
    }

    @Override
    public AnnouncementSourceClassificationRuleSet selectPublishedRuleSet(UUID releaseId) {
        AnnouncementSourceRuleReleaseRow release = selectReleaseDetails(releaseId);
        if (!Set.of("ACTIVE", "RETIRED").contains(release.releaseStatusCode())) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_ACTIVE,
                    HttpStatus.CONFLICT,
                    "게시된 규칙 버전만 재분류 실행에 사용할 수 있습니다."
            );
        }
        return selectRuleSet(releaseId, ruleReleaseDao.selectRuleTermList(releaseId));
    }

    @Override
    @Transactional
    public AnnouncementSourceRuleGoldenSetRunResponse insertGoldenSetRun(
            Authentication authentication,
            UUID releaseId,
            AnnouncementSourceRuleGoldenSetRunRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AnnouncementSourceRuleReleaseRow release = selectReleaseDetails(releaseId);
        if (!"DRAFT".equals(release.releaseStatusCode())) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_DRAFT,
                    HttpStatus.CONFLICT,
                    "초안 규칙 버전만 QA 정답 세트를 실행할 수 있습니다."
            );
        }
        if (!release.rowVersion().equals(request.expectedVersion())) {
            throw versionConflict();
        }
        List<AnnouncementSourceRuleTermRow> rows = ruleReleaseDao.selectRuleTermList(releaseId);
        String snapshotHash = selectSnapshotHash(rows);
        GoldenGateResult result = goldenGate.selectValidatedResult(
                selectRuleSet(releaseId, rows),
                snapshotHash
        );
        insertAudit(actorUserId, "RULE_RELEASE_GOLDEN_SET_EXECUTED", RELEASE_RESOURCE, releaseId, metadata(
                "serverGoldenRunId", result.runId(),
                "goldenCaseCount", String.valueOf(result.caseCount()),
                "ruleSnapshotHash", snapshotHash
        ));
        return new AnnouncementSourceRuleGoldenSetRunResponse(
                result.runId(), snapshotHash, result.caseCount()
        );
    }

    private AnnouncementSourceRuleReleaseRow selectReleaseDetails(UUID releaseId) {
        AnnouncementSourceRuleReleaseRow row = ruleReleaseDao.selectRuleReleaseDetails(releaseId);
        if (row == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "규칙 버전을 찾을 수 없습니다.");
        }
        return row;
    }

    private AnnouncementSourceRuleReleaseRow selectDraftReleaseForUpdate(UUID releaseId) {
        AnnouncementSourceRuleReleaseRow release = ruleReleaseDao.selectRuleReleaseDetailsForUpdate(releaseId);
        if (release == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "규칙 버전을 찾을 수 없습니다.");
        }
        if (!"DRAFT".equals(release.releaseStatusCode())) {
            throw notDraft();
        }
        return release;
    }

    private AnnouncementSourceKeywordRuleRow selectRuleDetails(UUID releaseId, UUID ruleId) {
        AnnouncementSourceKeywordRuleRow row = ruleReleaseDao.selectKeywordRuleDetails(releaseId, ruleId);
        if (row == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "키워드 규칙을 찾을 수 없습니다.");
        }
        return row;
    }

    private AnnouncementSourceKeywordRuleRow selectRuleDetailsForUpdate(UUID releaseId, UUID ruleId) {
        AnnouncementSourceKeywordRuleRow row = ruleReleaseDao.selectKeywordRuleDetailsForUpdate(releaseId, ruleId);
        if (row == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "키워드 규칙을 찾을 수 없습니다.");
        }
        return row;
    }

    private AnnouncementSourceRuleGroupRow selectEnabledGroup(UUID releaseId, String groupCode) {
        String normalizedCode = normalizeOptionalCode(groupCode);
        AnnouncementSourceRuleGroupRow group = normalizedCode == null
                ? null : ruleReleaseDao.selectRuleGroupDetails(releaseId, normalizedCode);
        if (group == null || !Boolean.TRUE.equals(group.enabled())) {
            throw invalidRule("사용할 수 있는 규칙 그룹을 선택하세요.");
        }
        return group;
    }

    private NormalizedRuleInput normalizeRuleInput(
            AnnouncementSourceKeywordRuleSaveRequest request,
            AnnouncementSourceRuleGroupRow group,
            AnnouncementSourceKeywordRuleRow existingRule
    ) {
        String strengthCode = normalizeRequiredCode("strengthCode", request.strengthCode(), STRENGTH_CODES);
        String matchModeCode = normalizeRequiredCode("matchModeCode", request.matchModeCode(), MATCH_MODE_CODES);
        TermValue canonical = normalizeTerm(request.canonicalKeyword());
        LinkedHashMap<String, TermValue> synonyms = new LinkedHashMap<>();
        if (request.synonyms() != null) {
            for (String synonym : request.synonyms()) {
                TermValue value = normalizeTerm(synonym);
                if (canonical.normalized().equals(value.normalized())
                        || synonyms.putIfAbsent(value.normalized(), value) != null) {
                    throw invalidRule("대표어와 유의어는 정규화 후 서로 달라야 합니다.");
                }
            }
        }
        boolean discoveryTerm = request.discoveryTerm() == null
                ? existingRule != null && Boolean.TRUE.equals(existingRule.discoveryTerm())
                : Boolean.TRUE.equals(request.discoveryTerm());
        Integer discoveryOrder = request.discoveryOrder() == null && request.discoveryTerm() == null
                && existingRule != null
                ? existingRule.discoveryOrder()
                : request.discoveryOrder();
        if (discoveryTerm && discoveryOrder == null) {
            throw invalidRule("발견 검색어에는 discoveryOrder가 필요합니다.");
        }
        if (!discoveryTerm && discoveryOrder != null) {
            throw invalidRule("발견 검색어가 아니면 discoveryOrder를 입력할 수 없습니다.");
        }
        if (discoveryTerm && !Set.of("TARGET", "SUPPORT_TYPE").contains(group.groupKindCode())) {
            throw invalidRule("발견 검색어는 지원대상 또는 지원유형 그룹에만 둘 수 있습니다.");
        }
        return new NormalizedRuleInput(
                canonical,
                List.copyOf(synonyms.values()),
                strengthCode,
                matchModeCode,
                request.sortOrder(),
                discoveryTerm,
                discoveryOrder,
                request.changeReason().trim()
        );
    }

    private TermValue normalizeTerm(String source) {
        String text = source == null ? "" : source.trim();
        String normalized = textNormalizer.selectNormalizedText(text).normalizedText();
        if (normalized.isBlank()) {
            throw invalidRule("키워드는 정규화 후 한 글자 이상이어야 합니다.");
        }
        return new TermValue(text, normalized);
    }

    private void validateNoDuplicateTerm(
            UUID groupId,
            NormalizedRuleInput input,
            UUID excludedRuleId
    ) {
        List<String> normalizedTerms = new ArrayList<>();
        normalizedTerms.add(input.canonical().normalized());
        input.synonyms().stream().map(TermValue::normalized).forEach(normalizedTerms::add);
        if (ruleReleaseDao.selectDuplicateTermCount(
                groupId,
                input.matchModeCode(),
                normalizedTerms,
                excludedRuleId
        ) > 0) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_DUPLICATE,
                    HttpStatus.CONFLICT,
                    "같은 그룹과 일치 방식에 이미 등록된 대표어 또는 유의어가 있습니다."
            );
        }
    }

    private void insertTerms(
            UUID ruleId,
            AnnouncementSourceRuleGroupRow group,
            NormalizedRuleInput input,
            UUID actorUserId
    ) {
        boolean classificationTerm = !"PROTECTED_METADATA".equals(group.groupKindCode());
        ruleReleaseDao.insertKeywordTerm(new AnnouncementSourceKeywordTermInsertCommand(
                UUID.randomUUID(),
                ruleId,
                group.groupId(),
                "CANONICAL",
                input.canonical().text(),
                input.canonical().normalized(),
                input.matchModeCode(),
                input.discoveryTerm(),
                input.discoveryOrder(),
                classificationTerm,
                actorUserId
        ));
        for (TermValue synonym : input.synonyms()) {
            ruleReleaseDao.insertKeywordTerm(new AnnouncementSourceKeywordTermInsertCommand(
                    UUID.randomUUID(),
                    ruleId,
                    group.groupId(),
                    "SYNONYM",
                    synonym.text(),
                    synonym.normalized(),
                    input.matchModeCode(),
                    false,
                    null,
                    classificationTerm,
                    actorUserId
            ));
        }
    }

    private AnnouncementSourceClassificationRuleSet selectRuleSet(
            UUID releaseId,
            List<AnnouncementSourceRuleTermRow> allRows
    ) {
        if (allRows.isEmpty()) {
            throw invalidRule("규칙 버전에 판정 가능한 키워드가 없습니다.");
        }
        List<AnnouncementSourceRuleTermRow> enabledRows = allRows.stream()
                .filter(row -> Boolean.TRUE.equals(row.groupEnabled()))
                .filter(row -> Boolean.TRUE.equals(row.ruleEnabled()))
                .filter(row -> Boolean.TRUE.equals(row.termEnabled()))
                .toList();
        if (enabledRows.isEmpty()) {
            throw invalidRule("규칙 버전에 사용 중인 키워드가 없습니다.");
        }
        LinkedHashMap<UUID, List<AnnouncementSourceRuleTermRow>> rowsByRule = enabledRows.stream()
                .collect(Collectors.groupingBy(
                        AnnouncementSourceRuleTermRow::ruleId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<AnnouncementSourceClassificationRule> rules = rowsByRule.values().stream()
                .map(this::toClassificationRule)
                .toList();
        AnnouncementSourceRuleTermRow first = enabledRows.getFirst();
        if (!releaseId.equals(first.releaseId())) {
            throw invalidRule("요청한 규칙 버전과 키워드 데이터가 일치하지 않습니다.");
        }
        return new AnnouncementSourceClassificationRuleSet(first.releaseCode(), rules);
    }

    private AnnouncementSourceClassificationRule toClassificationRule(
            List<AnnouncementSourceRuleTermRow> rows
    ) {
        AnnouncementSourceRuleTermRow first = rows.getFirst();
        String canonical = rows.stream()
                .filter(row -> "CANONICAL".equals(row.termTypeCode()))
                .map(AnnouncementSourceRuleTermRow::termText)
                .findFirst()
                .orElseThrow(() -> invalidRule("대표어가 없는 키워드 규칙이 있습니다."));
        List<AnnouncementSourceClassificationTerm> terms = rows.stream()
                .map(row -> new AnnouncementSourceClassificationTerm(
                        TermTypeCode.valueOf(row.termTypeCode()),
                        row.termText(),
                        MatchModeCode.valueOf(row.matchModeCode()),
                        Boolean.TRUE.equals(row.classificationTerm()),
                        Boolean.TRUE.equals(row.termEnabled())
                ))
                .toList();
        return new AnnouncementSourceClassificationRule(
                first.ruleCode(),
                first.groupCode(),
                RuleGroupKindCode.valueOf(first.groupKindCode()),
                canonical,
                StrengthCode.valueOf(first.strengthCode()),
                first.targetCategoryCode() == null
                        ? null : TargetCategoryCode.valueOf(first.targetCategoryCode()),
                first.supportTypeCode() == null
                        ? null : SupportTypeCode.valueOf(first.supportTypeCode()),
                terms,
                true
        );
    }

    private String selectSnapshotHash(List<AnnouncementSourceRuleTermRow> rows) {
        if (rows.isEmpty()) {
            throw invalidRule("게시할 키워드 규칙이 없습니다.");
        }
        StringBuilder canonical = new StringBuilder();
        for (AnnouncementSourceRuleTermRow row : rows) {
            appendHashField(canonical, row.groupCode());
            appendHashField(canonical, row.groupKindCode());
            appendHashField(canonical, row.titleActionCode());
            appendHashField(canonical, row.bodyActionCode());
            appendHashField(canonical, row.groupSortOrder());
            appendHashField(canonical, row.groupEnabled());
            appendHashField(canonical, row.targetCategoryCode());
            appendHashField(canonical, row.supportTypeCode());
            appendHashField(canonical, row.ruleCode());
            appendHashField(canonical, row.strengthCode());
            appendHashField(canonical, row.ruleSortOrder());
            appendHashField(canonical, row.ruleEnabled());
            appendHashField(canonical, row.termTypeCode());
            appendHashField(canonical, row.termText());
            appendHashField(canonical, row.normalizedTermText());
            appendHashField(canonical, row.matchModeCode());
            appendHashField(canonical, row.discoveryTerm());
            appendHashField(canonical, row.discoveryOrder());
            appendHashField(canonical, row.classificationTerm());
            appendHashField(canonical, row.termEnabled());
            canonical.append('\n');
        }
        return sha256(canonical.toString());
    }

    private void appendHashField(StringBuilder target, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        target.append(text.length()).append(':').append(text).append('|');
    }

    private RuleChangeCounts selectRuleChangeCounts(
            List<AnnouncementSourceRuleTermRow> previousRows,
            List<AnnouncementSourceRuleTermRow> candidateRows
    ) {
        Map<String, List<AnnouncementSourceRuleTermRow>> previous = groupRowsByRuleCode(previousRows);
        Map<String, List<AnnouncementSourceRuleTermRow>> candidate = groupRowsByRuleCode(candidateRows);
        int added = (int) candidate.keySet().stream().filter(code -> !previous.containsKey(code)).count();
        int modified = (int) candidate.entrySet().stream()
                .filter(entry -> previous.containsKey(entry.getKey()))
                .filter(entry -> !ruleFingerprint(entry.getValue()).equals(ruleFingerprint(previous.get(entry.getKey()))))
                .count();
        int disabled = (int) previous.entrySet().stream()
                .filter(entry -> isRuleEnabled(entry.getValue()))
                .filter(entry -> !candidate.containsKey(entry.getKey()) || !isRuleEnabled(candidate.get(entry.getKey())))
                .count();
        return new RuleChangeCounts(added, modified, disabled);
    }

    private Map<String, List<AnnouncementSourceRuleTermRow>> groupRowsByRuleCode(
            List<AnnouncementSourceRuleTermRow> rows
    ) {
        return rows.stream().collect(Collectors.groupingBy(
                AnnouncementSourceRuleTermRow::ruleCode,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    private boolean isRuleEnabled(List<AnnouncementSourceRuleTermRow> rows) {
        return !rows.isEmpty() && Boolean.TRUE.equals(rows.getFirst().ruleEnabled());
    }

    private String ruleFingerprint(List<AnnouncementSourceRuleTermRow> rows) {
        return rows.stream()
                .map(row -> String.join("|",
                        row.groupCode(),
                        row.strengthCode(),
                        String.valueOf(row.ruleSortOrder()),
                        String.valueOf(row.ruleEnabled()),
                        row.termTypeCode(),
                        row.normalizedTermText(),
                        row.matchModeCode(),
                        String.valueOf(row.discoveryTerm()),
                        String.valueOf(row.discoveryOrder()),
                        String.valueOf(row.termEnabled())
                ))
                .collect(Collectors.joining("\n"));
    }

    private AnnouncementSourceRulePreviewResponse toPreviewResponse(
            AnnouncementSourceRuleReleaseRow release,
            AnnouncementSourceClassificationResult result
    ) {
        return new AnnouncementSourceRulePreviewResponse(
                release.releaseId(),
                release.releaseCode(),
                release.rowVersion(),
                result.semanticStatusCode().name(),
                result.reasonCode().name(),
                result.titleStageCode().name(),
                result.bodyStageCode().name(),
                result.bodySourceCode().name(),
                result.bodyAvailabilityCode().name(),
                names(result.targetCategoryCodes()),
                names(result.supportTypeCodes()),
                result.groupACodes(),
                result.groupBCodes(),
                result.matches().stream()
                        .map(match -> new AnnouncementSourceRulePreviewResponse.MatchResponse(
                                match.ruleCode(),
                                match.groupCode(),
                                match.canonicalKeyword(),
                                match.matchedTerm(),
                                match.locationCode().name(),
                                match.startOffset(),
                                match.endOffset(),
                                match.appliedActionCode().name(),
                                match.maskedByProtectedMetadata()
                        ))
                        .toList()
        );
    }

    private AnnouncementSourceRuleReleaseSummaryResponse toReleaseResponse(AnnouncementSourceRuleReleaseRow row) {
        return new AnnouncementSourceRuleReleaseSummaryResponse(
                row.releaseId(),
                row.releaseCode(),
                row.versionNo(),
                row.rowVersion(),
                row.releaseStatusCode(),
                row.ruleSnapshotHash(),
                row.combinationOperatorCode(),
                row.bodyUnavailableActionCode(),
                Boolean.TRUE.equals(row.attachmentAnalysisEnabled()),
                Boolean.TRUE.equals(row.autoActivationEnabled()),
                row.changeNote(),
                row.ruleCount() == null ? 0 : row.ruleCount(),
                row.enabledRuleCount() == null ? 0 : row.enabledRuleCount(),
                row.createdAt(),
                row.activatedAt(),
                row.retiredAt()
        );
    }

    private AnnouncementSourceKeywordRuleSummaryResponse toRuleResponse(AnnouncementSourceKeywordRuleRow row) {
        return new AnnouncementSourceKeywordRuleSummaryResponse(
                row.ruleId(),
                row.releaseId(),
                row.releaseStatusCode(),
                row.ruleRowVersion(),
                row.ruleCode(),
                row.groupCode(),
                row.groupName(),
                row.groupKindCode(),
                row.targetCategoryCode(),
                row.supportTypeCode(),
                row.canonicalKeyword(),
                splitSynonyms(row.synonymsText()),
                row.strengthCode(),
                row.matchModeCode(),
                Boolean.TRUE.equals(row.discoveryTerm()),
                row.discoveryOrder(),
                Boolean.TRUE.equals(row.ruleEnabled()),
                row.sortOrder()
        );
    }

    private List<String> splitSynonyms(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(String.valueOf(SYNONYM_SEPARATOR), -1));
    }

    private List<String> names(List<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).toList();
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ErrorCode.INVALID_PAGE_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "페이지 요청 값이 올바르지 않습니다."
            );
        }
    }

    private String normalizeOptionalCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredCode(String field, String value, Set<String> allowed) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null || !allowed.contains(normalized)) {
            throw invalidRule(field + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private void validateOptionalCode(String field, String value, Set<String> allowed) {
        if (value != null && !allowed.contains(value)) {
            throw invalidRule(field + " 값이 올바르지 않습니다.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UUID selectRequiredActorUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return details.userId();
    }

    private void insertAudit(
            UUID actorUserId,
            String actionCode,
            String resourceType,
            UUID resourceId,
            String metadataJson
    ) {
        ruleReleaseDao.insertAuditLog(new AnnouncementSourceAuditLogCommand(
                actorUserId,
                actionCode,
                resourceType,
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    private String metadataWithChangeReason(String changeReason, String... keyValues) {
        String normalizedReason = trimToNull(changeReason);
        boolean reasonProvided = normalizedReason != null;
        String[] auditFields = new String[keyValues.length + 6];
        System.arraycopy(keyValues, 0, auditFields, 0, keyValues.length);
        int reasonFieldIndex = keyValues.length;
        auditFields[reasonFieldIndex] = CHANGE_REASON_PROVIDED_METADATA_KEY;
        auditFields[reasonFieldIndex + 1] = String.valueOf(reasonProvided);
        auditFields[reasonFieldIndex + 2] = CHANGE_REASON_LENGTH_METADATA_KEY;
        auditFields[reasonFieldIndex + 3] = String.valueOf(reasonProvided ? normalizedReason.length() : 0);
        auditFields[reasonFieldIndex + 4] = CHANGE_REASON_SHA256_METADATA_KEY;
        auditFields[reasonFieldIndex + 5] = reasonProvided ? sha256(normalizedReason) : "";
        return metadata(auditFields);
    }

    private String metadata(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("감사 metadata는 key와 value 쌍이어야 합니다.");
        }
        StringBuilder metadata = new StringBuilder("{");
        for (int index = 0; index < keyValues.length; index += 2) {
            if (index > 0) {
                metadata.append(',');
            }
            metadata.append('"').append(safeJson(keyValues[index])).append("\":\"")
                    .append(safeJson(keyValues[index + 1])).append('"');
        }
        return metadata.append('}').toString();
    }

    private String safeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private ApiException invalidRule(String message) {
        return new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID, HttpStatus.BAD_REQUEST, message);
    }

    private ApiException versionConflict() {
        return new ApiException(
                ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,
                HttpStatus.CONFLICT,
                "다른 관리자가 규칙을 변경했습니다. 최신 버전을 조회한 뒤 다시 시도하세요."
        );
    }

    private ApiException notDraft() {
        return new ApiException(
                ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_DRAFT,
                HttpStatus.CONFLICT,
                "DRAFT 규칙 버전만 수정할 수 있습니다."
        );
    }

    private record TermValue(String text, String normalized) {
    }

    private record NormalizedRuleInput(
            TermValue canonical,
            List<TermValue> synonyms,
            String strengthCode,
            String matchModeCode,
            int sortOrder,
            boolean discoveryTerm,
            Integer discoveryOrder,
            String changeReason
    ) {
    }

    private record RuleChangeCounts(int added, int modified, int disabled) {
    }
}
