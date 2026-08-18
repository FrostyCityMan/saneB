(() => {
    "use strict";

    const app = document.querySelector("[data-keyword-rule-page]");
    if (!app) {
        return;
    }

    const releaseUrl = app.dataset.releaseUrl;
    const reclassificationUrl = app.dataset.reclassificationUrl;
    const isAdmin = app.dataset.isAdmin === "true";
    const PAGE_SIZE = 100;
    const MAX_RELEASE_PAGE_COUNT = 20;
    const MAX_RULE_PAGE_COUNT = 100;
    const releaseSelect = app.querySelector("[data-release-select]");
    const releaseStatus = app.querySelector("[data-release-status]");
    const ruleList = app.querySelector("[data-rule-list]");
    const filterForm = app.querySelector("[data-rule-filter-form]");
    const pageMessage = app.querySelector("[data-keyword-page-message]");
    const ruleDialog = app.querySelector("[data-rule-dialog]");
    const ruleForm = app.querySelector("[data-rule-form]");
    const createReleaseDialog = app.querySelector("[data-release-create-dialog]");
    const createReleaseForm = app.querySelector("[data-release-create-form]");
    const previewDialog = app.querySelector("[data-preview-dialog]");
    const previewForm = app.querySelector("[data-preview-form]");
    const publicationDialog = app.querySelector("[data-publication-dialog]");
    const publicationForm = app.querySelector("[data-publication-form]");
    const reclassificationPreviewForm = app.querySelector("[data-reclassification-preview-form]");
    const reclassificationRunList = app.querySelector("[data-reclassification-run-list]");
    const reclassificationMessage = app.querySelector("[data-reclassification-message]");

    let releases = [];
    let selectedRelease = null;
    let rules = [];
    let latestGoldenSetResult = null;
    let latestPublicationResult = null;
    let reclassificationRuns = [];
    let reclassificationPollId = null;

    const groupLabels = {
        TARGET_BUSINESS: "지원대상 · 사업자",
        TARGET_PERSONAL: "지원대상 · 본인(개인)",
        TARGET_SPOUSE: "지원대상 · 배우자",
        TARGET_CHILD: "지원대상 · 자녀",
        TARGET_PARENT: "지원대상 · 부모님",
        SUPPORT_GENERAL_SUPPORT: "지원형태 · 일반 지원",
        SUPPORT_GRANT_SUBSIDY: "지원형태 · 지원금·보조금",
        SUPPORT_POLICY_FINANCE: "지원형태 · 정책자금·융자",
        SUPPORT_GUARANTEE: "지원형태 · 보증",
        SUPPORT_INTEREST: "지원형태 · 이차보전·이자지원",
        SUPPORT_VOUCHER_BENEFIT: "지원형태 · 바우처·혜택",
        SUPPORT_REFUND_REDUCTION: "지원형태 · 환급·감면",
        REVIEW_A_MANUFACTURING_TECH_PRODUCT: "그룹 A1 · 제조·기술·제품",
        REVIEW_A_RESEARCH_DEVELOPMENT: "그룹 A2 · R&D·연구개발",
        REVIEW_A_IP_CERTIFICATION: "그룹 A3 · 특허·인증",
        AUTO_EXCLUDE_B_EXPORT: "그룹 B1 · 수출·해외진출",
        AUTO_EXCLUDE_B_INVESTMENT_STARTUP: "그룹 B2 · 투자·스타트업",
        AUTO_EXCLUDE_B_PROCUREMENT: "그룹 B3 · 조달·혁신제품",
        AUTO_EXCLUDE_B_ESG: "그룹 B4 · ESG·친환경 기술",
        AUTO_EXCLUDE_B_ADMINISTRATIVE: "그룹 B6 · 행정 잡공고",
        PROTECTED_METADATA_AGENCY: "보호 기관명",
        CONTEXT_ANNOUNCEMENT: "공고 문맥",
        CONTEXT_HOUSEHOLD: "가구 문맥"
    };

    const statusLabels = {
        DRAFT: "초안",
        PUBLISHED: "공개",
        ACTIVE: "사용 중",
        RETIRED: "종료",
        ENABLED: "사용 중",
        DISABLED: "사용 중지"
    };

    const strengthLabels = {
        STRONG: "강함",
        SUPPLEMENTARY: "약함"
    };

    const matchModeLabels = {
        NORMALIZED_PHRASE: "정규화 문구",
        TOKEN: "단어 경계",
        EXACT_TITLE: "제목 전체 일치"
    };

    const semanticLabels = {
        ACCEPTED: "유효 후보",
        REVIEW_REQUIRED: "관리자검수중",
        EXCLUDED: "자동 제외",
        TAG: "태그 부여",
        CONTEXT_ONLY: "문맥 보조",
        MASK_ONLY: "보호 구간"
    };

    const reclassificationStatusLabels = {
        PREVIEW_PENDING: "미리보기 대기",
        PREVIEW_RUNNING: "미리보기 진행",
        PREVIEW_COMPLETED: "미리보기 완료",
        PREVIEW_PARTIAL_FAILED: "미리보기 일부 실패",
        APPLY_PENDING: "적용 대기",
        APPLY_RUNNING: "적용 진행",
        APPLY_PAUSED: "적용 일시중지",
        APPLY_COMPLETED: "적용 완료",
        APPLY_PARTIAL_FAILED: "적용 일부 실패",
        ROLLBACK_PENDING: "원복 대기",
        ROLLBACK_RUNNING: "원복 진행",
        ROLLBACK_COMPLETED: "원복 완료",
        ROLLBACK_PARTIAL_FAILED: "원복 일부 실패"
    };

    const unwrap = (payload) => payload && Object.prototype.hasOwnProperty.call(payload, "data")
        ? payload.data
        : payload;

    const asList = (payload) => {
        const data = unwrap(payload);
        if (Array.isArray(data)) {
            return data;
        }
        return data?.items || data?.content || data?.list || [];
    };

    const requestJson = async (url, options = {}) => {
        const response = await fetch(url, {
            credentials: "same-origin",
            headers: {"Content-Type": "application/json", ...(options.headers || {})},
            ...options
        });
        let payload = null;
        try {
            payload = await response.json();
        } catch (error) {
            payload = null;
        }
        if (!response.ok || payload?.success === false) {
            throw new Error(payload?.message || "요청을 처리하지 못했습니다.");
        }
        return payload;
    };

    const requestAllPages = async (url, label, maxPageCount) => {
        const items = [];
        let totalPages = 1;
        let totalCount = null;
        for (let pageNumber = 1; pageNumber <= totalPages; pageNumber += 1) {
            if (pageNumber > maxPageCount) {
                throw new Error(`${label}가 안전 조회 상한 ${maxPageCount * PAGE_SIZE}건을 초과했습니다. 서버 페이지네이션 화면이 필요합니다.`);
            }
            const requestUrl = new URL(url, window.location.href);
            requestUrl.searchParams.set("page", String(pageNumber));
            requestUrl.searchParams.set("size", String(PAGE_SIZE));
            const payload = await requestJson(requestUrl.toString());
            const data = unwrap(payload);
            if (Array.isArray(data)) {
                return pageNumber === 1 ? data : [...items, ...data];
            }

            const pageItems = asList(payload);
            items.push(...pageItems);
            const reportedTotalPages = Number(data?.totalPages);
            if (Number.isInteger(reportedTotalPages) && reportedTotalPages >= 0) {
                totalPages = Math.max(totalPages, Math.max(1, reportedTotalPages));
            }
            if (totalPages > maxPageCount) {
                throw new Error(`${label}가 안전 조회 상한 ${maxPageCount * PAGE_SIZE}건을 초과했습니다. 서버 페이지네이션 화면이 필요합니다.`);
            }
            if (pageNumber === 1 && data?.totalCount != null && Number.isFinite(Number(data.totalCount))) {
                totalCount = Number(data.totalCount);
            }
        }
        if (totalCount != null && items.length < totalCount) {
            throw new Error(`${label} 전체를 불러오지 못했습니다. 화면을 새로고침해 다시 시도하세요.`);
        }
        return items;
    };

    const releaseIdOf = (release) => release?.releaseId || release?.id || "";
    const ruleIdOf = (rule) => rule?.ruleId || rule?.id || "";
    const releaseVersionOf = (release) => release?.rowVersion ?? release?.version;
    const ruleVersionOf = (rule) => rule?.rowVersion ?? rule?.version;
    const requireVersion = (value, label) => {
        const version = Number(value);
        if (!Number.isInteger(version) || version < 0) {
            throw new Error(`${label}의 최신 버전을 확인하지 못했습니다. 목록을 새로고침한 뒤 다시 시도하세요.`);
        }
        return version;
    };
    const isDraft = () => selectedRelease?.releaseStatusCode === "DRAFT"
        || selectedRelease?.statusCode === "DRAFT";
    const canEdit = () => isAdmin && isDraft();
    const isDiscoveryEligibleGroup = (groupCode) => String(groupCode || "").startsWith("TARGET_")
        || String(groupCode || "").startsWith("SUPPORT_");

    const syncDiscoveryFields = () => {
        const groupCode = ruleForm.elements.ruleGroupCode.value;
        const discoveryTerm = ruleForm.elements.discoveryTerm;
        const discoveryOrder = ruleForm.elements.discoveryOrder;
        const eligible = isDiscoveryEligibleGroup(groupCode);

        discoveryTerm.disabled = !eligible;
        if (!eligible) {
            discoveryTerm.checked = false;
        }
        discoveryOrder.disabled = !eligible || !discoveryTerm.checked;
        discoveryOrder.required = eligible && discoveryTerm.checked;
        if (!discoveryTerm.checked) {
            discoveryOrder.value = "";
        }

        const help = app.querySelector("[data-discovery-term-help]");
        if (help) {
            help.textContent = eligible
                ? "이 그룹의 대표 키워드를 수집 발견 검색어로 지정할 수 있습니다."
                : "지원대상·지원형태 그룹에서만 수집 발견 검색어를 지정할 수 있습니다.";
        }
    };
    const currentGoldenSetResult = () => {
        if (!latestGoldenSetResult || !selectedRelease) {
            return null;
        }
        const sameRelease = String(latestGoldenSetResult.releaseId) === String(releaseIdOf(selectedRelease));
        const sameVersion = Number(latestGoldenSetResult.releaseVersion) === Number(releaseVersionOf(selectedRelease));
        return sameRelease && sameVersion ? latestGoldenSetResult : null;
    };

    const setMessage = (message, isError = false) => {
        pageMessage.textContent = message;
        pageMessage.classList.toggle("is-error", isError);
    };

    const formatDateTime = (value) => {
        if (!value) {
            return "-";
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? String(value)
            : new Intl.DateTimeFormat("ko-KR", {dateStyle: "medium", timeStyle: "short"}).format(date);
    };

    const textCell = (text) => {
        const cell = document.createElement("td");
        cell.textContent = text || "-";
        return cell;
    };

    const createBadge = (label, className = "") => {
        const badge = document.createElement("span");
        badge.className = `keyword-rule-badge ${className}`.trim();
        badge.textContent = label;
        return badge;
    };

    const renderReleaseSummary = () => {
        const statusCode = selectedRelease?.releaseStatusCode || selectedRelease?.statusCode || "";
        const ruleCount = selectedRelease?.ruleCount ?? rules.length;
        app.querySelector("[data-release-code]").textContent = selectedRelease?.releaseCode || selectedRelease?.versionCode || "-";
        app.querySelector("[data-release-status-label]").textContent = statusLabels[statusCode] || statusCode || "-";
        app.querySelector("[data-release-rule-count]").textContent = `${ruleCount}개`;
        app.querySelector("[data-release-created-at]").textContent = formatDateTime(selectedRelease?.createdAt);
        releaseStatus.textContent = selectedRelease ? (statusLabels[statusCode] || statusCode) : "선택된 버전 없음";

        app.querySelectorAll("[data-rule-create-open], [data-preview-open]").forEach((button) => {
            button.disabled = !canEdit();
        });
        const publicationButton = app.querySelector("[data-publication-open]");
        if (publicationButton) {
            publicationButton.disabled = !canEdit() || !currentGoldenSetResult();
        }
        const goldenSetRunButton = app.querySelector("[data-golden-set-run]");
        if (goldenSetRunButton) {
            goldenSetRunButton.disabled = !canEdit();
        }
        const reclassificationPreviewButton = app.querySelector("[data-reclassification-preview]");
        if (reclassificationPreviewButton) {
            reclassificationPreviewButton.disabled = !isAdmin || statusCode !== "ACTIVE";
        }
        renderQaSummary();
        renderHistory();
    };

    const isReclassificationRunning = (statusCode) => [
        "PREVIEW_PENDING", "PREVIEW_RUNNING", "APPLY_PENDING", "APPLY_RUNNING",
        "ROLLBACK_PENDING", "ROLLBACK_RUNNING"
    ].includes(statusCode);

    const reclassificationScopeLabel = (run) => {
        const providerLabels = {
            BIZINFO: "기업마당",
            GOV24_PUBLIC_SERVICE: "정부24",
            LOCAL_GOV_NOTICE: "지자체"
        };
        const period = run.collectedFrom || run.collectedTo
            ? `${run.collectedFrom || "처음"}~${run.collectedTo || "현재"}`
            : "전체 기간";
        return `${providerLabels[run.providerCode] || run.providerCode || "전체 채널"} · ${period} · 최대 ${run.maximumCount}건`;
    };

    const appendReclassificationAction = (cell, label, action, danger = false) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `text-action${danger ? " is-danger" : ""}`;
        button.dataset.reclassificationAction = action;
        button.textContent = label;
        cell.append(button);
    };

    const renderReclassificationRuns = () => {
        if (!reclassificationRunList) {
            return;
        }
        reclassificationRunList.replaceChildren();
        if (!reclassificationRuns.length) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 6;
            cell.className = "keyword-rule-empty";
            cell.textContent = "아직 생성된 기존 원문 영향도 실행이 없습니다.";
            row.append(cell);
            reclassificationRunList.append(row);
            return;
        }
        reclassificationRuns.forEach((run) => {
            const row = document.createElement("tr");
            row.dataset.reclassificationRunId = run.runId;
            const statusCell = document.createElement("td");
            statusCell.append(createBadge(
                reclassificationStatusLabels[run.runStatusCode] || run.runStatusCode,
                run.failedCount || run.conflictCount ? "is-disabled" : "is-enabled"
            ));
            row.append(statusCell);
            row.append(textCell(reclassificationScopeLabel(run)));
            row.append(textCell(`유효 ${run.acceptedCount} · 검수 ${run.reviewRequiredCount} · 제외 ${run.excludedCount}`));
            row.append(textCell(`적용 ${run.appliedCount}/${run.totalCount} · 충돌 ${run.conflictCount} · 실패 ${run.failedCount} · 원복 ${run.rolledBackCount}`));
            row.append(textCell(formatDateTime(run.updatedAt)));
            const actionCell = document.createElement("td");
            actionCell.className = "keyword-rule-row-actions";
            if (!isAdmin) {
                actionCell.textContent = "조회 전용";
            } else if (run.runStatusCode === "PREVIEW_COMPLETED") {
                appendReclassificationAction(actionCell, "적용", "apply");
            } else if (["APPLY_PENDING", "APPLY_RUNNING"].includes(run.runStatusCode)) {
                appendReclassificationAction(actionCell, "일시중지", "pause");
            } else if (run.runStatusCode === "APPLY_PAUSED") {
                appendReclassificationAction(actionCell, "재개", "resume");
                if (run.appliedCount > 0) {
                    appendReclassificationAction(actionCell, "원복", "rollback", true);
                }
            } else if (["APPLY_COMPLETED", "APPLY_PARTIAL_FAILED"].includes(run.runStatusCode)
                    && run.appliedCount > run.rolledBackCount) {
                appendReclassificationAction(actionCell, "원복", "rollback", true);
            } else {
                actionCell.textContent = isReclassificationRunning(run.runStatusCode) ? "처리 중" : "완료";
            }
            row.append(actionCell);
            reclassificationRunList.append(row);
        });
    };

    const loadReclassificationRuns = async (announce = false) => {
        if (!reclassificationUrl) {
            return;
        }
        try {
            const payload = await requestJson(reclassificationUrl);
            reclassificationRuns = asList(payload);
            renderReclassificationRuns();
            if (announce && reclassificationMessage) {
                reclassificationMessage.textContent = `최근 실행 ${reclassificationRuns.length}건을 갱신했습니다.`;
            }
        } catch (error) {
            if (reclassificationMessage) {
                reclassificationMessage.textContent = `실행 이력을 불러오지 못했습니다. ${error.message}`;
            }
        }
    };

    const updateReclassificationAction = async (run, action) => {
        const actionLabels = {apply: "적용", pause: "일시중지", resume: "재개", rollback: "원복"};
        const reason = window.prompt(`${actionLabels[action]} 사유를 입력하세요.`);
        if (!reason?.trim()) {
            return;
        }
        const request = {expectedVersion: requireVersion(run.rowVersion, "재분류 실행"), changeReason: reason.trim()};
        let endpoint = action;
        if (action === "apply") {
            endpoint = "application";
            request.confirmationText = window.prompt("적용하려면 '기존 원문 재분류 적용'을 정확히 입력하세요.") || "";
            if (request.confirmationText !== "기존 원문 재분류 적용") {
                throw new Error("확인 문구가 일치하지 않아 적용을 시작하지 않았습니다.");
            }
        } else if (action === "rollback") {
            request.confirmationText = window.prompt("원복하려면 '기존 원문 재분류 원복'을 정확히 입력하세요.") || "";
            if (request.confirmationText !== "기존 원문 재분류 원복") {
                throw new Error("확인 문구가 일치하지 않아 원복을 시작하지 않았습니다.");
            }
        }
        await requestJson(`${reclassificationUrl}/${encodeURIComponent(run.runId)}/${endpoint}`, {
            method: "POST",
            body: JSON.stringify(request)
        });
        if (reclassificationMessage) {
            reclassificationMessage.textContent = `${actionLabels[action]} 요청을 접수했습니다. 진행 상태를 자동으로 갱신합니다.`;
        }
        await loadReclassificationRuns();
    };

    const renderQaSummary = () => {
        const goldenResult = currentGoldenSetResult();
        const publishedRelease = latestPublicationResult?.activeRelease;
        const isCurrentPublication = publishedRelease
            && String(releaseIdOf(publishedRelease)) === String(releaseIdOf(selectedRelease));
        const result = goldenResult || (isCurrentPublication ? latestPublicationResult : null);
        app.querySelector("[data-qa-gate-status]").textContent = goldenResult
            ? "게시 전 QA 통과"
            : isCurrentPublication ? "공개 검증 통과" : "실행 전";
        app.querySelector("[data-qa-execution-id]").textContent = result?.goldenSetRunId || "-";
        app.querySelector("[data-qa-case-count]").textContent = result?.goldenCaseCount != null
            && Number.isInteger(Number(result.goldenCaseCount))
            ? `${Number(result.goldenCaseCount)}건`
            : "미제공";
        app.querySelector("[data-qa-impact-count]").textContent = isCurrentPublication
            && latestPublicationResult.expectedDecisionChangeCount != null
            && Number.isInteger(Number(latestPublicationResult.expectedDecisionChangeCount))
            ? `${Number(latestPublicationResult.expectedDecisionChangeCount)}건`
            : goldenResult ? "게시 시 산출" : "미확인";
        const goldenRunMessage = app.querySelector("[data-golden-run-message]");
        if (goldenRunMessage) {
            goldenRunMessage.textContent = goldenResult
                ? `QA ${goldenResult.goldenCaseCount}건 통과 · 공개 가능`
                : canEdit() ? "QA 실행 후 공개할 수 있습니다." : "초안 버전에서만 실행할 수 있습니다.";
        }
    };

    const renderHistory = () => {
        const historyList = app.querySelector("[data-release-history]");
        historyList.replaceChildren();
        if (!selectedRelease) {
            const item = document.createElement("li");
            item.textContent = "버전을 선택하면 변경 사유와 처리 시각을 확인할 수 있습니다.";
            historyList.append(item);
            return;
        }
        const entries = [
            {actionLabel: "초안 생성", actedAt: selectedRelease.createdAt},
            {actionLabel: "버전 공개", actedAt: selectedRelease.activatedAt},
            {actionLabel: "버전 종료", actedAt: selectedRelease.retiredAt}
        ].filter((entry) => entry.actedAt);
        entries.forEach((entry) => {
            const item = document.createElement("li");
            const title = document.createElement("strong");
            title.textContent = entry.actionLabel;
            const description = document.createElement("span");
            description.textContent = "서버가 제공한 처리 시각";
            const meta = document.createElement("small");
            meta.textContent = formatDateTime(entry.actedAt);
            item.append(title, description, meta);
            historyList.append(item);
        });
        if (!entries.length) {
            const item = document.createElement("li");
            item.textContent = "서버가 제공한 처리 이력이 없습니다.";
            historyList.append(item);
        }
        if (selectedRelease.changeNote) {
            const item = document.createElement("li");
            const title = document.createElement("strong");
            title.textContent = "현재 버전 메모";
            const description = document.createElement("span");
            description.textContent = selectedRelease.changeNote;
            const meta = document.createElement("small");
            meta.textContent = "변경자와 변경별 시각은 현재 API에서 제공하지 않습니다.";
            item.append(title, description, meta);
            historyList.append(item);
        }
    };

    const filteredRules = () => {
        const formData = new FormData(filterForm);
        const group = String(formData.get("ruleGroupCode") || "");
        const strength = String(formData.get("strengthCode") || "");
        const enabled = String(formData.get("enabled") || "");
        const keyword = String(formData.get("keyword") || "").trim().toLocaleLowerCase("ko-KR");
        return rules.filter((rule) => {
            const synonyms = Array.isArray(rule.synonyms) ? rule.synonyms : String(rule.synonyms || "").split(",");
            const haystack = [rule.canonicalKeyword, ...synonyms].join(" ").toLocaleLowerCase("ko-KR");
            const ruleEnabled = rule.enabled ?? rule.isEnabled ?? rule.statusCode !== "DISABLED";
            return (!group || rule.ruleGroupCode === group)
                && (!strength || rule.strengthCode === strength)
                && (!enabled || String(Boolean(ruleEnabled)) === enabled)
                && (!keyword || haystack.includes(keyword));
        });
    };

    const renderRules = () => {
        const visibleRules = filteredRules();
        app.querySelector("[data-rule-result-count]").textContent = `${visibleRules.length}개`;
        ruleList.replaceChildren();
        if (!visibleRules.length) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 6;
            cell.className = "keyword-rule-empty";
            cell.textContent = selectedRelease ? "조건에 맞는 키워드 규칙이 없습니다." : "규칙 버전을 선택하세요.";
            row.append(cell);
            ruleList.append(row);
            return;
        }

        visibleRules.forEach((rule) => {
            const row = document.createElement("tr");
            row.append(textCell(groupLabels[rule.ruleGroupCode] || rule.ruleGroupCode));

            const keywordCell = document.createElement("td");
            const keyword = document.createElement("strong");
            keyword.textContent = rule.canonicalKeyword || "-";
            keywordCell.append(keyword);
            row.append(keywordCell);

            const synonyms = Array.isArray(rule.synonyms) ? rule.synonyms : String(rule.synonyms || "").split(/[\n,]/);
            row.append(textCell(synonyms.map((item) => item.trim()).filter(Boolean).join(", ") || "없음"));

            const policyCell = document.createElement("td");
            policyCell.append(
                createBadge(strengthLabels[rule.strengthCode] || rule.strengthCode || "-", `is-${String(rule.strengthCode || "").toLowerCase()}`),
                createBadge(matchModeLabels[rule.matchModeCode] || rule.matchModeCode || "-")
            );
            if (rule.discoveryTerm) {
                const discoveryOrder = Number(rule.discoveryOrder);
                const orderLabel = Number.isInteger(discoveryOrder) && discoveryOrder > 0
                    ? `${discoveryOrder}순위`
                    : "순서 미확인";
                policyCell.append(createBadge(`수집 발견 검색어 · ${orderLabel}`, "is-enabled"));
            }
            row.append(policyCell);

            const enabled = rule.enabled ?? rule.isEnabled ?? rule.statusCode !== "DISABLED";
            const statusCell = document.createElement("td");
            statusCell.append(createBadge(enabled ? "사용 중" : "사용 중지", enabled ? "is-enabled" : "is-disabled"));
            row.append(statusCell);

            const managementCell = document.createElement("td");
            managementCell.className = "keyword-rule-row-actions";
            if (canEdit()) {
                const editButton = document.createElement("button");
                editButton.type = "button";
                editButton.className = "text-action";
                editButton.dataset.ruleEdit = ruleIdOf(rule);
                editButton.textContent = "수정";
                const statusButton = document.createElement("button");
                statusButton.type = "button";
                statusButton.className = "text-action";
                statusButton.dataset.ruleStatus = ruleIdOf(rule);
                statusButton.textContent = enabled ? "사용중지" : "사용재개";
                const deleteButton = document.createElement("button");
                deleteButton.type = "button";
                deleteButton.className = "text-action is-danger";
                deleteButton.dataset.ruleDelete = ruleIdOf(rule);
                deleteButton.textContent = "삭제";
                managementCell.append(editButton, statusButton, deleteButton);
            } else {
                managementCell.textContent = "조회 전용";
            }
            row.append(managementCell);
            ruleList.append(row);
        });
    };

    const loadRules = async () => {
        if (!selectedRelease) {
            rules = [];
            renderRules();
            renderReleaseSummary();
            return;
        }
        const releaseId = releaseIdOf(selectedRelease);
        setMessage("선택한 버전의 키워드 규칙을 불러오고 있습니다.");
        try {
            rules = await requestAllPages(
                `${releaseUrl}/${encodeURIComponent(releaseId)}/keyword-rules`,
                "키워드 규칙",
                MAX_RULE_PAGE_COUNT
            );
            renderRules();
            renderReleaseSummary();
            setMessage(`${selectedRelease.releaseCode || "선택 버전"} 규칙 ${rules.length}개를 확인했습니다.`);
        } catch (error) {
            rules = [];
            renderRules();
            renderReleaseSummary();
            setMessage(`키워드 규칙 API를 불러오지 못했습니다. ${error.message}`, true);
        }
    };

    const loadReleases = async (preferredReleaseId = "") => {
        try {
            releases = await requestAllPages(releaseUrl, "규칙 버전", MAX_RELEASE_PAGE_COUNT);
            releaseSelect.replaceChildren();
            if (!releases.length) {
                const option = new Option("등록된 규칙 버전이 없습니다", "");
                releaseSelect.append(option);
                selectedRelease = null;
                await loadRules();
                setMessage("등록된 규칙 버전이 없습니다. 관리자는 새 초안 버전을 생성할 수 있습니다.");
                return;
            }
            releases.forEach((release) => {
                const statusCode = release.releaseStatusCode || release.statusCode;
                const label = `${release.releaseCode || release.versionCode || "규칙 버전"} · ${statusLabels[statusCode] || statusCode}`;
                releaseSelect.append(new Option(label, releaseIdOf(release)));
            });
            const defaultRelease = releases.find((release) => String(releaseIdOf(release)) === String(preferredReleaseId))
                || releases.find((release) => ["ACTIVE", "DRAFT"].includes(release.releaseStatusCode || release.statusCode))
                || releases[0];
            selectedRelease = defaultRelease;
            releaseSelect.value = String(releaseIdOf(defaultRelease));
            await loadRules();
        } catch (error) {
            releases = [];
            selectedRelease = null;
            releaseSelect.replaceChildren(new Option("규칙 버전 API 확인 필요", ""));
            rules = [];
            renderRules();
            renderReleaseSummary();
            setMessage(`규칙 버전 API를 불러오지 못했습니다. ${error.message}`, true);
        }
    };

    const openDialog = (dialog) => {
        if (dialog && typeof dialog.showModal === "function") {
            dialog.showModal();
        }
    };

    const closeDialog = (dialog) => {
        if (dialog?.open) {
            dialog.close();
        }
    };

    const openRuleEditor = (rule = null) => {
        if (!canEdit()) {
            return;
        }
        ruleForm.reset();
        ruleForm.elements.ruleId.value = ruleIdOf(rule);
        ruleForm.elements.expectedVersion.value = rule
            ? requireVersion(ruleVersionOf(rule), "키워드 규칙")
            : requireVersion(releaseVersionOf(selectedRelease), "선택한 규칙 버전");
        ruleForm.elements.ruleGroupCode.value = rule?.ruleGroupCode || "TARGET_BUSINESS";
        ruleForm.elements.canonicalKeyword.value = rule?.canonicalKeyword || "";
        ruleForm.elements.synonyms.value = Array.isArray(rule?.synonyms) ? rule.synonyms.join(", ") : rule?.synonyms || "";
        ruleForm.elements.strengthCode.value = rule?.strengthCode || "STRONG";
        ruleForm.elements.matchModeCode.value = rule?.matchModeCode || "NORMALIZED_PHRASE";
        ruleForm.elements.discoveryTerm.checked = Boolean(rule?.discoveryTerm);
        ruleForm.elements.discoveryOrder.value = rule?.discoveryOrder ?? "";
        ruleForm.elements.sortOrder.value = rule?.sortOrder ?? 100;
        syncDiscoveryFields();
        app.querySelector("[data-rule-dialog-title]").textContent = rule ? "키워드 수정" : "키워드 추가";
        app.querySelector("[data-rule-form-message]").textContent = "";
        openDialog(ruleDialog);
    };

    const ruleRequestBody = () => {
        const formData = new FormData(ruleForm);
        const ruleGroupCode = String(formData.get("ruleGroupCode") || "");
        const discoveryTerm = isDiscoveryEligibleGroup(ruleGroupCode)
            && ruleForm.elements.discoveryTerm.checked;
        const discoveryOrder = discoveryTerm
            ? Number(ruleForm.elements.discoveryOrder.value)
            : null;
        if (discoveryTerm && (!Number.isInteger(discoveryOrder) || discoveryOrder < 1)) {
            throw new Error("수집 발견 검색어의 순서는 1 이상의 정수로 입력하세요.");
        }
        return {
            ruleGroupCode,
            canonicalKeyword: String(formData.get("canonicalKeyword") || "").trim(),
            synonyms: String(formData.get("synonyms") || "").split(/[\n,]/).map((item) => item.trim()).filter(Boolean),
            strengthCode: formData.get("strengthCode"),
            matchModeCode: formData.get("matchModeCode"),
            sortOrder: Number(formData.get("sortOrder")),
            discoveryTerm,
            discoveryOrder,
            expectedVersion: requireVersion(formData.get("expectedVersion"), "저장 대상"),
            changeReason: String(formData.get("changeReason") || "").trim()
        };
    };

    ruleForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!ruleForm.reportValidity() || !canEdit()) {
            return;
        }
        const ruleId = ruleForm.elements.ruleId.value;
        const url = `${releaseUrl}/${encodeURIComponent(releaseIdOf(selectedRelease))}/keyword-rules${ruleId ? `/${encodeURIComponent(ruleId)}` : ""}`;
        const message = app.querySelector("[data-rule-form-message]");
        message.textContent = "저장 중입니다.";
        try {
            await requestJson(url, {method: ruleId ? "PUT" : "POST", body: JSON.stringify(ruleRequestBody())});
            closeDialog(ruleDialog);
            await loadReleases(releaseIdOf(selectedRelease));
            setMessage(ruleId ? "키워드 규칙을 수정했습니다." : "키워드 규칙을 추가했습니다.");
        } catch (error) {
            message.textContent = error.message;
        }
    });

    ruleForm?.elements.ruleGroupCode.addEventListener("change", syncDiscoveryFields);
    ruleForm?.elements.discoveryTerm.addEventListener("change", syncDiscoveryFields);

    ruleList?.addEventListener("click", async (event) => {
        const editButton = event.target.closest("[data-rule-edit]");
        if (editButton) {
            openRuleEditor(rules.find((rule) => String(ruleIdOf(rule)) === editButton.dataset.ruleEdit));
            return;
        }
        const statusButton = event.target.closest("[data-rule-status]");
        if (statusButton && canEdit()) {
            const rule = rules.find((item) => String(ruleIdOf(item)) === statusButton.dataset.ruleStatus);
            const enabled = rule.enabled ?? rule.isEnabled ?? rule.statusCode !== "DISABLED";
            const reason = window.prompt(`${enabled ? "사용중지" : "사용재개"} 사유를 입력하세요.`);
            if (!reason?.trim()) {
                return;
            }
            try {
                await requestJson(`${releaseUrl}/${encodeURIComponent(releaseIdOf(selectedRelease))}/keyword-rules/${encodeURIComponent(ruleIdOf(rule))}/status`, {
                    method: "PATCH",
                    body: JSON.stringify({
                        enabled: !enabled,
                        expectedVersion: requireVersion(ruleVersionOf(rule), "키워드 규칙"),
                        changeReason: reason.trim()
                    })
                });
                await loadReleases(releaseIdOf(selectedRelease));
            } catch (error) {
                setMessage(error.message, true);
            }
            return;
        }
        const deleteButton = event.target.closest("[data-rule-delete]");
        if (deleteButton && canEdit()) {
            const rule = rules.find((item) => String(ruleIdOf(item)) === deleteButton.dataset.ruleDelete);
            if (!window.confirm(`'${rule.canonicalKeyword}' 규칙을 초안에서 삭제하시겠습니까?`)) {
                return;
            }
            try {
                await requestJson(`${releaseUrl}/${encodeURIComponent(releaseIdOf(selectedRelease))}/keyword-rules/${encodeURIComponent(ruleIdOf(rule))}`, {
                    method: "DELETE",
                    body: JSON.stringify({
                        expectedVersion: requireVersion(ruleVersionOf(rule), "키워드 규칙"),
                        changeReason: "초안 규칙 삭제"
                    })
                });
                await loadReleases(releaseIdOf(selectedRelease));
            } catch (error) {
                setMessage(error.message, true);
            }
        }
    });

    createReleaseForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!createReleaseForm.reportValidity() || !isAdmin) {
            return;
        }
        const message = app.querySelector("[data-release-create-message]");
        try {
            const activeRelease = releases.find((release) => (release.releaseStatusCode || release.statusCode) === "ACTIVE");
            if (!activeRelease) {
                throw new Error("복제할 사용 중 규칙 버전을 찾지 못했습니다. 목록을 새로고침한 뒤 다시 시도하세요.");
            }
            const payload = await requestJson(releaseUrl, {
                method: "POST",
                body: JSON.stringify({
                    expectedVersion: requireVersion(releaseVersionOf(activeRelease), "사용 중 규칙 버전"),
                    changeReason: new FormData(createReleaseForm).get("changeReason")
                })
            });
            const created = unwrap(payload);
            closeDialog(createReleaseDialog);
            createReleaseForm.reset();
            await loadReleases(releaseIdOf(created));
        } catch (error) {
            message.textContent = error.message;
        }
    });

    previewForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!previewForm.reportValidity() || !canEdit()) {
            return;
        }
        const result = app.querySelector("[data-preview-result]");
        result.textContent = "판정 중입니다.";
        try {
            const formData = new FormData(previewForm);
            const payload = await requestJson(`${releaseUrl}/${encodeURIComponent(releaseIdOf(selectedRelease))}/preview`, {
                method: "POST",
                body: JSON.stringify({
                    title: formData.get("title"),
                    bodyText: formData.get("bodyText"),
                    expectedVersion: requireVersion(releaseVersionOf(selectedRelease), "선택한 규칙 버전")
                })
            });
            const preview = unwrap(payload) || {};
            result.replaceChildren();
            const heading = document.createElement("strong");
            heading.textContent = semanticLabels[preview.semanticStatusCode] || preview.semanticStatusCode || "판정 결과";
            const reason = document.createElement("p");
            reason.textContent = preview.reasonLabel || preview.reasonCode || "일치 근거를 확인하세요.";
            const tags = document.createElement("div");
            tags.className = "keyword-preview-tags";
            [...(preview.targetCategoryCodes || []), ...(preview.supportTypeCodes || [])].forEach((code) => tags.append(createBadge(code)));
            result.append(heading, reason, tags);
        } catch (error) {
            result.textContent = error.message;
        }
    });

    publicationForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!publicationForm.reportValidity() || !canEdit()) {
            return;
        }
        const formData = new FormData(publicationForm);
        const message = app.querySelector("[data-publication-message]");
        try {
            const payload = await requestJson(`${releaseUrl}/${encodeURIComponent(releaseIdOf(selectedRelease))}/publication`, {
                method: "POST",
                body: JSON.stringify({
                    goldenSetRunId: formData.get("goldenSetRunId"),
                    expectedVersion: requireVersion(releaseVersionOf(selectedRelease), "선택한 규칙 버전"),
                    changeReason: formData.get("changeReason")
                })
            });
            latestPublicationResult = unwrap(payload);
            closeDialog(publicationDialog);
            publicationForm.reset();
            await loadReleases(releaseIdOf(latestPublicationResult?.activeRelease) || releaseIdOf(selectedRelease));
            setMessage("QA 정답 세트 검증을 통과한 규칙 버전을 공개했습니다.");
        } catch (error) {
            message.textContent = error.message;
        }
    });

    app.querySelector("[data-golden-set-run]")?.addEventListener("click", async () => {
        if (!canEdit()) {
            return;
        }
        const message = app.querySelector("[data-golden-run-message]");
        message.textContent = "QA-01~20을 서버에서 검증하고 있습니다.";
        try {
            const releaseId = releaseIdOf(selectedRelease);
            const releaseVersion = requireVersion(releaseVersionOf(selectedRelease), "선택한 규칙 버전");
            const payload = await requestJson(`${releaseUrl}/${encodeURIComponent(releaseId)}/golden-set-runs`, {
                method: "POST",
                body: JSON.stringify({expectedVersion: releaseVersion})
            });
            latestGoldenSetResult = {
                ...(unwrap(payload) || {}),
                releaseId,
                releaseVersion
            };
            renderReleaseSummary();
            setMessage("QA 정답 세트 20건을 통과했습니다. 같은 초안 버전을 공개할 수 있습니다.");
        } catch (error) {
            latestGoldenSetResult = null;
            renderReleaseSummary();
            message.textContent = error.message;
            setMessage(`QA 정답 세트 검증에 실패했습니다. ${error.message}`, true);
        }
    });

    reclassificationPreviewForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!reclassificationPreviewForm.reportValidity() || !isAdmin
                || (selectedRelease?.releaseStatusCode || selectedRelease?.statusCode) !== "ACTIVE") {
            return;
        }
        const formData = new FormData(reclassificationPreviewForm);
        const collectedFrom = String(formData.get("collectedFrom") || "");
        const collectedTo = String(formData.get("collectedTo") || "");
        if (collectedFrom && collectedTo && collectedFrom > collectedTo) {
            reclassificationMessage.textContent = "수집 종료일은 수집 시작일보다 빠를 수 없습니다.";
            return;
        }
        try {
            const payload = {
                ruleReleaseId: releaseIdOf(selectedRelease),
                providerCode: String(formData.get("providerCode") || "") || null,
                collectedFrom: collectedFrom || null,
                collectedTo: collectedTo || null,
                includeLinkedAnnouncements: formData.get("includeLinkedAnnouncements") === "on",
                maximumCount: Number(formData.get("maximumCount")),
                batchSize: Number(formData.get("batchSize")),
                changeReason: String(formData.get("changeReason") || "").trim()
            };
            await requestJson(`${reclassificationUrl}/previews`, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            reclassificationMessage.textContent = "운영 데이터 변경 없는 영향도 미리보기를 생성했습니다. 결과를 자동으로 갱신합니다.";
            await loadReclassificationRuns();
        } catch (error) {
            reclassificationMessage.textContent = error.message;
        }
    });

    reclassificationRunList?.addEventListener("click", async (event) => {
        const button = event.target.closest("[data-reclassification-action]");
        const row = button?.closest("[data-reclassification-run-id]");
        if (!button || !row || !isAdmin) {
            return;
        }
        const run = reclassificationRuns.find((item) => item.runId === row.dataset.reclassificationRunId);
        if (!run) {
            return;
        }
        try {
            await updateReclassificationAction(run, button.dataset.reclassificationAction);
        } catch (error) {
            reclassificationMessage.textContent = error.message;
        }
    });

    app.querySelector("[data-reclassification-refresh]")?.addEventListener("click", () => loadReclassificationRuns(true));

    releaseSelect.addEventListener("change", async () => {
        selectedRelease = releases.find((release) => String(releaseIdOf(release)) === releaseSelect.value) || null;
        latestGoldenSetResult = null;
        await loadRules();
    });
    filterForm.addEventListener("submit", (event) => { event.preventDefault(); renderRules(); });
    app.querySelector("[data-rule-filter-reset]")?.addEventListener("click", () => { filterForm.reset(); renderRules(); });
    app.querySelector("[data-rule-create-open]")?.addEventListener("click", () => openRuleEditor());
    app.querySelector("[data-release-create-open]")?.addEventListener("click", () => openDialog(createReleaseDialog));
    app.querySelector("[data-preview-open]")?.addEventListener("click", () => openDialog(previewDialog));
    app.querySelector("[data-publication-open]")?.addEventListener("click", () => {
        const goldenResult = currentGoldenSetResult();
        if (!goldenResult) {
            setMessage("현재 초안 버전으로 QA 정답 세트를 먼저 실행하세요.", true);
            return;
        }
        publicationForm.elements.goldenSetRunId.value = goldenResult.goldenSetRunId;
        app.querySelector("[data-publication-message]").textContent = "";
        openDialog(publicationDialog);
    });
    app.querySelectorAll("[data-rule-dialog-close]").forEach((button) => button.addEventListener("click", () => closeDialog(ruleDialog)));
    app.querySelector("[data-release-create-close]")?.addEventListener("click", () => closeDialog(createReleaseDialog));
    app.querySelectorAll("[data-preview-close]").forEach((button) => button.addEventListener("click", () => closeDialog(previewDialog)));
    app.querySelector("[data-publication-close]")?.addEventListener("click", () => closeDialog(publicationDialog));

    loadReleases();
    loadReclassificationRuns();
    reclassificationPollId = window.setInterval(() => {
        if (reclassificationRuns.some((run) => isReclassificationRunning(run.runStatusCode))) {
            loadReclassificationRuns();
        }
    }, 3000);
    window.addEventListener("pagehide", () => window.clearInterval(reclassificationPollId), {once: true});
})();
