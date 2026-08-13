(() => {
    "use strict";

    const page = document.querySelector("[data-collected-announcement-page]");
    if (!page) {
        return;
    }

    const sourceUrl = page.dataset.sourceUrl;
    const conversionUrl = page.dataset.conversionUrl;
    const classificationV2Enabled = page.dataset.classificationV2Enabled === "true";
    const canManage = page.dataset.canManage === "true";
    const filterForm = page.querySelector("[data-collected-filter-form]");
    const filterReset = page.querySelector("[data-collected-filter-reset]");
    const sourceList = page.querySelector("[data-collected-list]");
    const sourceDetail = page.querySelector("[data-collected-detail]");
    const filteredCount = page.querySelector("[data-collected-filtered-count]");
    const listCount = page.querySelector("[data-collected-list-count]");
    const pagePrev = page.querySelector("[data-collected-page-prev]");
    const pageNext = page.querySelector("[data-collected-page-next]");
    const pageInfo = page.querySelector("[data-collected-page-info]");
    const conversionDialog = page.querySelector("[data-conversion-dialog]");
    const conversionForm = page.querySelector("[data-conversion-form]");
    let currentPage = 1;
    let totalPages = 1;
    let selectedSourceId = null;
    let selectedSource = null;
    let currentView = "ACTION_REQUIRED";

    const labels = {
        BIZINFO: "기업마당",
        GOV24_PUBLIC_SERVICE: "정부24 공공서비스",
        LOCAL_GOV_NOTICE: "전국 지자체 공고",
        REVIEW_PENDING: "검수대기",
        CONDITION_INPUT_REQUIRED: "조건 입력 필요",
        REVIEW_COMPLETED: "검수완료",
        ACTIVATED: "활성 전환",
        ARCHIVED: "보관",
        COMPLETE: "전체 원문",
        PARTIAL: "일부 원문",
        MINIMAL: "최소 정보",
        BODY_AVAILABLE: "본문 수집 완료",
        BODY_UNAVAILABLE: "본문 미수집",
        TITLE_ONLY: "제목만 수집",
        EXACT_DUPLICATE: "동일 공고",
        SIMILAR: "유사 공고",
        PENDING: "검수 필요",
        CREATE_NEW_SELECTED: "신규 등록 선택",
        UPDATE_EXISTING_SELECTED: "기존 공고 갱신 선택",
        IGNORED: "무시",
        AUTO_CONFIRMED: "자동 중복 확인",
        ACCEPTED: "유효 후보",
        REVIEW_REQUIRED: "관리자검수중",
        EXCLUDED: "자동 제외",
        SOURCE_POLICY_COLLECT_ALL: "게시판 전체 수집 정책",
        SOURCE_POLICY_EXCLUDED: "수집 제외 출처",
        INCLUDE_KEYWORD_MATCHED: "지원사업 키워드 일치",
        EXCLUDE_KEYWORD_MATCHED: "제외 키워드 일치",
        NO_INCLUDE_KEYWORD: "지원사업 키워드 없음",
        INCLUDE_AND_EXCLUDE_KEYWORD: "포함·제외 키워드 동시 일치",
        TITLE: "제목",
        BODY: "본문",
        ATTACHMENT: "첨부파일",
        TARGET_CATEGORY: "지원대상",
        SUPPORT_TYPE: "지원형태",
        GROUP_A_REVIEW: "그룹 A · 관리자 검수",
        GROUP_B_EXCLUDE: "그룹 B · 자동 제외",
        TAG: "태그 부여",
        REVIEW_REQUIRED_ACTION: "관리자 검수",
        EXCLUDED_ACTION: "자동 제외",
        MASK_ONLY: "보호 구간",
        CONTEXT_ONLY: "문맥 보조",
        BUSINESS: "사업자",
        PERSONAL: "본인(개인)",
        SPOUSE: "배우자",
        CHILD: "자녀",
        PARENT: "부모님",
        GENERAL_SUPPORT: "일반 지원",
        GRANT_SUBSIDY: "지원금·보조금",
        POLICY_FINANCE: "정책자금·융자",
        GUARANTEE: "보증",
        INTEREST_SUPPORT: "이차보전·이자지원",
        VOUCHER_BENEFIT: "바우처·혜택",
        REFUND_REDUCTION: "환급·감면"
    };

    const reasonLabels = {
        TITLE_GROUP_B_MATCHED: "제목에 자동 제외 문구가 있습니다.",
        TITLE_GROUP_A_MATCHED: "제목에 관리자 검수 문구가 있습니다.",
        TITLE_COMBINATION_NOT_MATCHED: "제목에서 지원대상과 지원형태 조합을 확인하지 못했습니다.",
        BODY_UNAVAILABLE: "확인할 본문이 없어 관리자가 검수해야 합니다.",
        BODY_FETCH_FAILED: "본문을 가져오지 못해 관리자가 검수해야 합니다.",
        BODY_GROUP_B_MATCHED: "본문에 자동 제외 검토 문구가 있어 관리자가 확인해야 합니다.",
        BODY_GROUP_A_MATCHED: "본문에 관리자 검수 문구가 있습니다.",
        BODY_COMBINATION_NOT_CONFIRMED: "본문에서 지원대상과 지원형태 조합을 다시 확인하지 못했습니다.",
        TARGET_SUPPORT_CONFIRMED: "제목과 본문에서 지원대상과 지원형태를 확인했습니다.",
        TARGET_SUPPORT_COMBINATION_MATCHED: "지원대상과 지원형태 조합을 확인했습니다.",
        REQUIRED_COMBINATION_NOT_MATCHED: "지원대상과 지원형태 조합을 확인하지 못했습니다."
    };

    const viewLabels = {
        ACTION_REQUIRED: "조치 필요 공고",
        ACCEPTED: "유효 후보 공고",
        EXCLUDED: "자동 제외 공고",
        ALL: "전체 수집 공고"
    };

    const statusLabel = (code) => labels[code] || code || "-";
    const reasonLabel = (code) => reasonLabels[code] || statusLabel(code);
    const safeHttpUrl = (value) => {
        if (!value) {
            return null;
        }
        try {
            const url = new URL(String(value), window.location.href);
            return ["http:", "https:"].includes(url.protocol) ? url.href : null;
        } catch (error) {
            return null;
        }
    };
    const classificationOf = (data) => data?.classification || {
        semanticStatusCode: data?.semanticStatusCode,
        reasonCode: data?.semanticReasonCode,
        targetCategoryCodes: data?.targetCategoryCodes || [],
        supportTypeCodes: data?.supportTypeCodes || [],
        ruleReleaseCode: data?.ruleReleaseCode,
        decisionId: data?.classificationDecisionId,
        version: data?.classificationVersion,
        matches: data?.semanticMatches || []
    };

    const requestJson = async (url, options = {}) => {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers: {"Content-Type": "application/json", ...(options.headers || {})}
        });
        const payload = await response.json().catch(() => null);
        if (!response.ok || payload?.success === false) {
            throw new Error(payload?.message || "요청 처리에 실패했습니다.");
        }
        return payload?.data ?? payload;
    };

    const appendText = (parent, tagName, text, className) => {
        const element = document.createElement(tagName);
        if (className) {
            element.className = className;
        }
        element.textContent = text == null || text === "" ? "-" : String(text);
        parent.appendChild(element);
        return element;
    };

    const formatDateTime = (value) => {
        if (!value) {
            return "-";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }
        return new Intl.DateTimeFormat("ko-KR", {dateStyle: "medium", timeStyle: "short"}).format(date);
    };

    const showDetailMessage = (message, isError = false) => {
        sourceDetail.replaceChildren();
        const wrapper = document.createElement("div");
        wrapper.className = `collected-detail-empty${isError ? " is-error" : ""}`;
        appendText(wrapper, "strong", isError ? "처리하지 못했습니다." : "공고를 선택하세요.");
        appendText(wrapper, "p", message);
        sourceDetail.appendChild(wrapper);
    };

    const runAction = async (action) => {
        try {
            await action();
        } catch (error) {
            showDetailMessage(error.message || "요청 처리에 실패했습니다.", true);
        }
    };

    const createMetaValue = (meta, label, value, link = false) => {
        appendText(meta, "dt", label);
        const description = document.createElement("dd");
        if (link && value) {
            const safeUrl = safeHttpUrl(value);
            if (safeUrl) {
                const anchor = document.createElement("a");
                anchor.href = safeUrl;
                anchor.target = "_blank";
                anchor.rel = "noopener noreferrer";
                anchor.textContent = "원문 새 창에서 보기";
                description.appendChild(anchor);
            } else {
                description.textContent = "안전한 원문 링크가 제공되지 않았습니다.";
            }
        } else {
            description.textContent = value == null || value === "" ? "-" : String(value);
        }
        meta.appendChild(description);
    };

    const createBadge = (code, kind = "") => {
        const badge = document.createElement("span");
        badge.className = `classification-badge ${kind}`.trim();
        badge.textContent = statusLabel(code);
        return badge;
    };

    const renderSummary = async () => {
        const [actionData, acceptedData, excludedData] = await Promise.all([
            requestJson(`${sourceUrl}?semanticStatusCode=REVIEW_REQUIRED&page=1&size=1`),
            requestJson(`${sourceUrl}?semanticStatusCode=ACCEPTED&page=1&size=1`),
            requestJson(`${sourceUrl}?semanticStatusCode=EXCLUDED&page=1&size=1`)
        ]);
        page.querySelector("[data-collected-action-count]").textContent = `${actionData.totalCount || 0}건`;
        page.querySelector("[data-collected-accepted-count]").textContent = `${acceptedData.totalCount || 0}건`;
        page.querySelector("[data-collected-excluded-count]").textContent = `${excludedData.totalCount || 0}건`;
    };

    const applyView = (viewCode, resetOperationalStatus = true) => {
        currentView = viewCode;
        const semanticField = filterForm.elements.semanticStatusCode;
        const reviewField = filterForm.elements.reviewStatusCode;
        semanticField.value = viewCode === "ACTION_REQUIRED" ? "REVIEW_REQUIRED" : viewCode === "ALL" ? "" : viewCode;
        if (resetOperationalStatus) {
            reviewField.value = viewCode === "ACTION_REQUIRED" ? "REVIEW_PENDING" : "";
        }
        page.querySelector("[data-collected-view-title]").textContent = viewLabels[viewCode];
        page.querySelectorAll("[data-collected-view]").forEach((button) => {
            const active = button.dataset.collectedView === viewCode;
            button.classList.toggle("is-active", active);
            button.setAttribute("aria-selected", String(active));
            button.tabIndex = active ? 0 : -1;
        });
    };

    const updateSelectedButton = (selectedButton) => {
        sourceList.querySelectorAll(".source-list-item").forEach((button) => {
            const selected = button === selectedButton;
            button.classList.toggle("is-selected", selected);
            button.setAttribute("aria-pressed", String(selected));
        });
    };

    const selectSource = async (sourceId, selectedButton) => {
        selectedSourceId = sourceId;
        updateSelectedButton(selectedButton);
        await renderSourceDetail(sourceId);
    };

    const appendClassificationTags = (parent, classification) => {
        const tagCodes = [...(classification.targetCategoryCodes || []), ...(classification.supportTypeCodes || [])];
        if (!tagCodes.length) {
            return;
        }
        const tags = document.createElement("span");
        tags.className = "classification-tag-row";
        tagCodes.forEach((code) => tags.append(createBadge(code, "is-tag")));
        parent.append(tags);
    };

    const renderList = async () => {
        sourceList.replaceChildren(appendText(document.createDocumentFragment(), "p", "수집 공고를 조회하고 있습니다.", "collected-empty-state"));
        const params = new URLSearchParams(new FormData(filterForm));
        [...params.entries()].forEach(([key, value]) => { if (!value) params.delete(key); });
        params.set("page", String(currentPage));
        params.set("size", "15");
        const data = await requestJson(`${sourceUrl}?${params.toString()}`);

        currentPage = data.page || 1;
        totalPages = Math.max(1, data.totalPages || 1);
        pageInfo.textContent = `${currentPage} / ${totalPages}`;
        pagePrev.disabled = currentPage <= 1;
        pageNext.disabled = currentPage >= totalPages;
        filteredCount.textContent = `${data.totalCount || 0}건`;
        listCount.textContent = `${data.totalCount || 0}건`;
        sourceList.replaceChildren();

        const items = data.items || data.content || [];
        if (!items.length) {
            selectedSourceId = null;
            appendText(sourceList, "p", "조건에 맞는 수집 공고가 없습니다.", "collected-empty-state");
            showDetailMessage("분류함 또는 검색 조건을 바꾸어 주세요.");
            return;
        }

        const selectedItem = items.find((item) => item.sourceId === selectedSourceId) || items[0];
        selectedSourceId = selectedItem.sourceId;
        let selectedButton = null;
        items.forEach((item) => {
            const classification = classificationOf(item);
            const semanticStatusCode = classification.semanticStatusCode || item.semanticStatusCode;
            const button = document.createElement("button");
            button.type = "button";
            button.className = "source-list-item collected-source-item";
            button.setAttribute("aria-pressed", String(item.sourceId === selectedSourceId));
            if (item.sourceId === selectedSourceId) {
                button.classList.add("is-selected");
                selectedButton = button;
            }
            appendText(button, "strong", item.title);
            const classificationLine = document.createElement("span");
            classificationLine.className = "collected-status-line is-classification";
            appendText(classificationLine, "span", "분류");
            classificationLine.append(createBadge(semanticStatusCode, `is-${String(semanticStatusCode || "unknown").toLowerCase()}`));
            button.append(classificationLine);
            const reviewLine = document.createElement("span");
            reviewLine.className = "collected-status-line is-operation";
            appendText(reviewLine, "span", "운영 검수");
            reviewLine.append(createBadge(item.reviewStatusCode, "is-review"));
            button.append(reviewLine);
            appendClassificationTags(button, classification);
            const meta = document.createElement("span");
            meta.className = "collected-source-meta";
            appendText(meta, "span", `${item.publicCode || "-"} · ${statusLabel(item.providerCode)}`);
            appendText(meta, "span", `${item.agencyName || "기관 미확인"} · 원문 등록 ${formatDateTime(item.postedAt)}`);
            button.append(meta);
            button.addEventListener("click", () => runAction(() => selectSource(item.sourceId, button)));
            sourceList.append(button);
        });
        updateSelectedButton(selectedButton);
        await renderSourceDetail(selectedSourceId);
    };

    const appendActionButton = (parent, text, className, handler, disabled = false, title = "") => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = className;
        button.textContent = text;
        button.disabled = disabled;
        if (title) button.title = title;
        button.addEventListener("click", () => runAction(handler));
        parent.appendChild(button);
    };

    const renderClassificationPanel = (data, classification) => {
        const panel = document.createElement("section");
        panel.className = "collected-detail-section classification-decision-panel";
        appendText(panel, "h4", "분류 판정");
        if (data.classificationLoadError) {
            appendText(panel, "p", `구조화 판정 근거를 불러오지 못했습니다. ${data.classificationLoadError}`, "classification-load-error");
        }
        const statusRow = document.createElement("div");
        statusRow.className = "classification-decision-status";
        statusRow.append(createBadge(classification.semanticStatusCode || data.semanticStatusCode, `is-${String(classification.semanticStatusCode || data.semanticStatusCode || "unknown").toLowerCase()}`));
        appendText(statusRow, "span", reasonLabel(classification.reasonCode || data.semanticReasonCode));
        panel.append(statusRow);
        const meta = document.createElement("dl");
        meta.className = "source-meta compact-source-meta";
        createMetaValue(meta, "규칙 버전", classification.ruleReleaseCode || classification.ruleReleaseId);
        createMetaValue(meta, "판정 시각", formatDateTime(classification.evaluatedAt || classification.createdAt));
        createMetaValue(meta, "제목 판정", statusLabel(classification.titleStageCode));
        createMetaValue(meta, "본문 판정", statusLabel(classification.bodyStageCode));
        createMetaValue(meta, "확정 상태", statusLabel(classification.confirmedClassificationStatusCode));
        panel.append(meta);
        const tags = document.createElement("div");
        tags.className = "classification-tag-groups";
        const targetGroup = document.createElement("div");
        appendText(targetGroup, "strong", "지원대상");
        (classification.targetCategoryCodes || []).forEach((code) => targetGroup.append(createBadge(code, "is-tag")));
        if (!(classification.targetCategoryCodes || []).length) appendText(targetGroup, "span", "판정된 대상 없음", "muted-copy");
        const supportGroup = document.createElement("div");
        appendText(supportGroup, "strong", "지원형태");
        (classification.supportTypeCodes || []).forEach((code) => supportGroup.append(createBadge(code, "is-tag")));
        if (!(classification.supportTypeCodes || []).length) appendText(supportGroup, "span", "판정된 형태 없음", "muted-copy");
        tags.append(targetGroup, supportGroup);
        panel.append(tags);

        const matches = classification.matches || [];
        if (matches.length) {
            const evidenceList = document.createElement("ul");
            evidenceList.className = "classification-evidence-list";
            matches.forEach((match) => {
                const item = document.createElement("li");
                const keyword = match.matchedTerm || match.canonicalKeyword || "일치어";
                const actionCode = match.appliedActionCode === "REVIEW_REQUIRED" ? "REVIEW_REQUIRED_ACTION"
                    : match.appliedActionCode === "EXCLUDED" ? "EXCLUDED_ACTION" : match.appliedActionCode;
                item.textContent = `${statusLabel(match.ruleGroupCode)} · ${statusLabel(match.locationCode)} · ${keyword} · ${statusLabel(actionCode)}`;
                evidenceList.append(item);
            });
            panel.append(evidenceList);
        }
        sourceDetail.append(panel);
    };

    const renderCollectionDiagnosticPanel = (data, classification) => {
        const panel = document.createElement("section");
        panel.className = "collected-detail-section collection-diagnostic-panel";
        appendText(panel, "h4", "수집 원문 상태");
        appendText(panel, "p", "분류 판정과 별개인 수집 진단입니다. 수집 실패나 본문 누락을 자동 제외로 해석하지 않습니다.", "muted-copy");
        const meta = document.createElement("dl");
        meta.className = "source-meta compact-source-meta";
        createMetaValue(meta, "수집처", statusLabel(data.providerCode));
        createMetaValue(meta, "수집 범위", statusLabel(data.sourceCompletenessCode));
        createMetaValue(meta, "본문 상태", statusLabel(classification.bodyAvailabilityCode || data.bodyAvailabilityCode));
        createMetaValue(meta, "본문 출처", statusLabel(classification.bodySourceCode || data.bodySourceCode));
        createMetaValue(meta, "수집일", formatDateTime(data.collectedAt));
        createMetaValue(meta, "원문", data.sourceUrl, true);
        panel.append(meta);
        sourceDetail.append(panel);
    };

    const renderDuplicateCandidates = (data) => {
        const section = document.createElement("section");
        section.className = "collected-detail-section";
        appendText(section, "h4", "운영 공고 중복 후보");
        const wrapper = document.createElement("div");
        wrapper.className = "source-duplicate-list";
        const candidates = data.duplicateCandidates || [];
        if (!candidates.length) {
            appendText(wrapper, "p", "검출된 중복 또는 유사 공고가 없습니다.", "muted-copy");
        }
        candidates.forEach((candidate) => {
            const card = document.createElement("div");
            card.className = "source-duplicate-card";
            appendText(card, "strong", `${candidate.announcementCode || "-"} · ${candidate.announcementTitle || "-"}`);
            appendText(card, "span", `${candidate.matchTypeLabel || statusLabel(candidate.matchTypeCode)} · ${candidate.decisionStatusLabel || statusLabel(candidate.decisionStatusCode)}`, "muted-copy");
            appendText(card, "p", candidate.similarityReason || "비교 항목 일부가 일치하거나 유사합니다.", "muted-copy");
            if (canManage && candidate.decisionStatusCode === "PENDING") {
                const actions = document.createElement("div");
                actions.className = "source-detail-actions";
                [["CREATE_NEW", "신규 공고로 검수"], ["UPDATE_EXISTING", "기존 공고 갱신"], ["IGNORE", "검수 제외"]].forEach(([actionCode, text]) => appendActionButton(actions, text, "secondary-action small-action", () => decideDuplicate(data.sourceId, candidate.candidateId, actionCode)));
                card.append(actions);
            }
            wrapper.append(card);
        });
        section.append(wrapper);
        sourceDetail.append(section);
    };

    const renderSourceDuplicates = (data) => {
        const section = document.createElement("section");
        section.className = "collected-detail-section";
        appendText(section, "h4", "다른 수집처의 중복 원문");
        const wrapper = document.createElement("div");
        wrapper.className = "source-duplicate-list";
        const duplicates = data.sourceDuplicates || [];
        if (!duplicates.length) appendText(wrapper, "p", "다른 수집처에서 확인된 중복 원문이 없습니다.", "muted-copy");
        duplicates.forEach((item) => {
            const card = document.createElement("div");
            card.className = "source-duplicate-card";
            appendText(card, "strong", `${item.candidatePublicCode || "-"} · ${item.candidateTitle || "-"}`);
            appendText(card, "span", `${statusLabel(item.candidateProviderCode)} · ${statusLabel(item.matchTypeCode)} · ${statusLabel(item.decisionStatusCode)}`, "muted-copy");
            appendText(card, "p", item.matchReason, "muted-copy");
            if (canManage && item.decisionStatusCode === "PENDING") {
                const actions = document.createElement("div");
                actions.className = "source-detail-actions";
                [["CREATE_NEW", "신규 공고로 검수"], ["UPDATE_EXISTING", "기존 공고 갱신"], ["IGNORE", "검수 제외"]].forEach(([actionCode, text]) => appendActionButton(actions, text, "secondary-action small-action", () => decideSourceDuplicate(data.sourceId, item.duplicateId, actionCode)));
                card.append(actions);
            }
            wrapper.append(card);
        });
        section.append(wrapper);
        sourceDetail.append(section);
    };

    const renderAttachments = (attachments) => {
        const section = document.createElement("section");
        section.className = "collected-detail-section attachment-reference-panel";
        appendText(section, "h4", "첨부파일 · 원문 확인용");
        appendText(section, "p", "첨부파일은 분류 판정에 사용하지 않습니다.", "classification-scope-note");
        const list = document.createElement("div");
        list.className = "collected-attachment-list";
        if (!attachments?.length) appendText(list, "p", "수집된 첨부파일이 없습니다.", "muted-copy");
        (attachments || []).forEach((attachment) => {
            const safeUrl = safeHttpUrl(attachment.fileUrl);
            if (safeUrl) {
                const anchor = document.createElement("a");
                anchor.href = safeUrl;
                anchor.target = "_blank";
                anchor.rel = "noopener noreferrer";
                anchor.textContent = attachment.fileName || "첨부파일 열기";
                list.append(anchor);
            } else {
                appendText(
                    list,
                    "span",
                    `${attachment.fileName || "첨부파일"} · 안전한 링크 형식이 아닙니다.`,
                    "muted-copy"
                );
            }
        });
        section.append(list);
        sourceDetail.append(section);
    };

    const renderSourceDetail = async (sourceId) => {
        showDetailMessage("상세 정보를 불러오고 있습니다.");
        const data = await requestJson(`${sourceUrl}/${encodeURIComponent(sourceId)}`);
        try {
            data.classification = await requestJson(`${sourceUrl}/${encodeURIComponent(sourceId)}/classification`);
        } catch (error) {
            data.classificationLoadError = error.message;
        }
        selectedSource = data;
        const classification = classificationOf(data);
        const semanticStatus = classification.semanticStatusCode || data.semanticStatusCode;
        sourceDetail.replaceChildren();
        appendText(sourceDetail, "h3", data.title);
        appendText(sourceDetail, "p", `${data.publicCode || "-"} · ${statusLabel(data.providerCode)} · 원문 등록 ${formatDateTime(data.postedAt)}`, "muted-copy");

        const actions = document.createElement("div");
        actions.className = "source-detail-actions collected-primary-actions";
        if (canManage) {
            [["CONDITION_INPUT_REQUIRED", "조건 입력 필요"], ["REVIEW_COMPLETED", "검수완료"], ["ARCHIVED", "보관"]].forEach(([statusCode, text]) => {
                if (data.reviewStatusCode !== statusCode) appendActionButton(actions, text, "secondary-action small-action", () => updateSourceStatus(data.sourceId, statusCode));
            });
            const pendingDuplicates = [...(data.duplicateCandidates || []), ...(data.sourceDuplicates || [])].some((item) => item.decisionStatusCode === "PENDING");
            const reviewRequired = semanticStatus === "REVIEW_REQUIRED" && data.reviewStatusCode !== "REVIEW_COMPLETED";
            const classificationUnavailable = !classification.decisionId || classification.version == null;
            const disabled = pendingDuplicates || semanticStatus === "EXCLUDED" || reviewRequired
                || (classificationV2Enabled && classificationUnavailable);
            const reason = pendingDuplicates
                ? "중복 후보를 먼저 처리하세요."
                : semanticStatus === "EXCLUDED"
                    ? "자동 제외 공고는 전환할 수 없습니다."
                    : reviewRequired
                        ? "관리자 검수를 완료한 뒤 전환하세요."
                        : classificationV2Enabled && classificationUnavailable
                            ? "분류 판정 식별자와 버전을 확인한 뒤 전환하세요."
                            : "";
            appendActionButton(actions, "운영 공고 전환", "primary-action small-action", () => openConversionDialog(data), disabled, reason);
        }
        sourceDetail.append(actions);

        const operational = document.createElement("section");
        operational.className = "collected-detail-section operational-review-panel";
        appendText(operational, "h4", "운영 검수 상태");
        const operationalStatus = document.createElement("div");
        operationalStatus.className = "classification-decision-status";
        operationalStatus.append(createBadge(data.reviewStatusCode, "is-review"));
        appendText(operationalStatus, "span", "운영자가 처리하는 업무 상태입니다.");
        operational.append(operationalStatus);
        sourceDetail.append(operational);

        renderClassificationPanel(data, classification);
        renderCollectionDiagnosticPanel(data, classification);
        renderDuplicateCandidates(data);
        renderSourceDuplicates(data);
        renderAttachments(data.attachments);

        const bodySection = document.createElement("section");
        bodySection.className = "collected-detail-section";
        appendText(bodySection, "h4", "원문 본문");
        appendText(bodySection, "pre", data.bodyText || "본문 정보가 없습니다.", "source-body");
        sourceDetail.append(bodySection);
    };

    const refreshAfterChange = async () => {
        await Promise.all([renderSummary(), renderList()]);
    };

    const updateSourceStatus = async (sourceId, reviewStatusCode) => {
        await requestJson(`${sourceUrl}/${encodeURIComponent(sourceId)}/review-status`, {method: "PATCH", body: JSON.stringify({reviewStatusCode, reason: "수집 공고 검수 화면 상태 변경"})});
        await refreshAfterChange();
    };

    const decideDuplicate = async (sourceId, candidateId, decisionActionCode) => {
        await requestJson(`${sourceUrl}/${encodeURIComponent(sourceId)}/duplicate-candidates/${encodeURIComponent(candidateId)}/decision`, {method: "PATCH", body: JSON.stringify({decisionActionCode, decisionNote: "수집 공고 검수 화면 중복 결정"})});
        await refreshAfterChange();
    };

    const decideSourceDuplicate = async (sourceId, duplicateId, decisionActionCode) => {
        await requestJson(`${sourceUrl}/${encodeURIComponent(sourceId)}/source-duplicates/${encodeURIComponent(duplicateId)}/decision`, {method: "PATCH", body: JSON.stringify({decisionActionCode, decisionNote: "수집 공고 검수 화면 교차 수집처 중복 결정"})});
        await refreshAfterChange();
    };

    const syncPrimaryTarget = () => {
        const primary = conversionForm.querySelector("input[name='primaryTargetCategoryCode']:checked")?.value;
        if (!primary) return;
        const matchingTag = conversionForm.querySelector(`input[name='targetCategoryCodes'][value='${primary}']`);
        if (matchingTag) matchingTag.checked = true;
    };

    const openConversionDialog = (data) => {
        selectedSource = data;
        const classification = classificationOf(data);
        conversionForm.reset();
        const confirmedCurrent = classification.confirmedClassificationStatusCode === "CURRENT";
        const targetCodes = confirmedCurrent && classification.confirmedTargetCategoryCodes?.length
            ? classification.confirmedTargetCategoryCodes
            : classification.targetCategoryCodes?.length ? classification.targetCategoryCodes : ["BUSINESS"];
        const supportCodes = confirmedCurrent && classification.confirmedSupportTypeCodes?.length
            ? classification.confirmedSupportTypeCodes
            : classification.supportTypeCodes?.length ? classification.supportTypeCodes : ["GENERAL_SUPPORT"];
        const primaryCode = classification.primaryTargetCategoryCode || targetCodes[0] || "BUSINESS";
        conversionForm.querySelectorAll("input[name='targetCategoryCodes']").forEach((input) => { input.checked = targetCodes.includes(input.value); });
        conversionForm.querySelectorAll("input[name='supportTypeCodes']").forEach((input) => { input.checked = supportCodes.includes(input.value); });
        const primaryInput = conversionForm.querySelector(`input[name='primaryTargetCategoryCode'][value='${primaryCode}']`);
        if (primaryInput) primaryInput.checked = true;
        conversionForm.elements.expectedClassificationDecisionId.value = classification.decisionId || "";
        conversionForm.elements.expectedVersion.value = classification.version ?? data.version ?? "";
        page.querySelector("[data-conversion-message]").textContent = "";
        page.querySelectorAll("[data-v2-only]").forEach((element) => {
            element.hidden = !classificationV2Enabled;
        });
        page.querySelector("[data-conversion-mode-note]").textContent = classificationV2Enabled
            ? "현재 분류 판정과 다중 태그를 확정한 뒤 운영 공고 DRAFT를 생성합니다."
            : "분류 V2 비활성 단계입니다. 기존 V1 계약으로 대표 지원대상만 저장하고 운영 공고 DRAFT를 생성합니다.";
        page.querySelector("[data-conversion-submit]").textContent = classificationV2Enabled
            ? "분류 확정 후 운영 공고 전환"
            : "기존 계약으로 운영 공고 전환";
        syncPrimaryTarget();
        conversionDialog.showModal();
    };

    const closeConversionDialog = () => {
        if (conversionDialog.open) conversionDialog.close();
    };

    conversionForm.addEventListener("change", (event) => {
        if (event.target.matches("input[name='primaryTargetCategoryCode']")) syncPrimaryTarget();
        if (event.target.matches("input[name='targetCategoryCodes']")) syncPrimaryTarget();
    });

    conversionForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!conversionForm.reportValidity() || !selectedSource) return;
        syncPrimaryTarget();
        const targetCategoryCodes = [...conversionForm.querySelectorAll("input[name='targetCategoryCodes']:checked")].map((input) => input.value);
        const supportTypeCodes = [...conversionForm.querySelectorAll("input[name='supportTypeCodes']:checked")].map((input) => input.value);
        const message = page.querySelector("[data-conversion-message]");
        if (classificationV2Enabled && (!targetCategoryCodes.length || !supportTypeCodes.length)) {
            message.textContent = "지원대상과 지원형태를 각각 하나 이상 선택하세요.";
            return;
        }
        const formData = new FormData(conversionForm);
        if (!classificationV2Enabled) {
            try {
                const result = await requestJson(
                    `${sourceUrl}/${encodeURIComponent(selectedSource.sourceId)}/announcements`,
                    {
                        method: "POST",
                        body: JSON.stringify({
                            targetTypeCode: formData.get("primaryTargetCategoryCode"),
                            incomeJudgementCode: formData.get("incomeJudgementCode")
                        })
                    }
                );
                closeConversionDialog();
                window.location.href = `/app/announcements/input?announcementCode=${encodeURIComponent(result.announcementCode || "")}`;
            } catch (error) {
                message.textContent = error.message;
            }
            return;
        }
        let classificationConfirmed = false;
        try {
            const confirmedClassification = await requestJson(`${sourceUrl}/${encodeURIComponent(selectedSource.sourceId)}/confirmed-classification`, {
                method: "PUT",
                body: JSON.stringify({
                    expectedClassificationDecisionId: formData.get("expectedClassificationDecisionId"),
                    expectedVersion: Number(formData.get("expectedVersion")),
                    targetCategoryCodes,
                    supportTypeCodes,
                    reviewNote: String(formData.get("reviewNote") || "").trim() || null
                })
            });
            classificationConfirmed = true;
            selectedSource.classification = confirmedClassification;
            const result = await requestJson(`${conversionUrl}/${encodeURIComponent(selectedSource.sourceId)}/announcements`, {
                method: "POST",
                body: JSON.stringify({
                    primaryTargetCategoryCode: formData.get("primaryTargetCategoryCode"),
                    targetCategoryCodes,
                    supportTypeCodes,
                    incomeJudgementCode: formData.get("incomeJudgementCode"),
                    expectedClassificationDecisionId: confirmedClassification.decisionId,
                    expectedVersion: confirmedClassification.version
                })
            });
            closeConversionDialog();
            window.location.href = `/app/announcements/input?announcementCode=${encodeURIComponent(result.announcementCode || "")}`;
        } catch (error) {
            message.textContent = classificationConfirmed
                ? `분류 태그는 확정했지만 운영 공고 전환에 실패했습니다. ${error.message}`
                : error.message;
        }
    });

    page.querySelectorAll("[data-conversion-close]").forEach((button) => button.addEventListener("click", closeConversionDialog));
    page.querySelectorAll("[data-collected-view]").forEach((button) => {
        button.addEventListener("click", () => {
            applyView(button.dataset.collectedView);
            currentPage = 1;
            selectedSourceId = null;
            runAction(renderList);
        });
        button.addEventListener("keydown", (event) => {
            if (!["ArrowLeft", "ArrowRight"].includes(event.key)) return;
            const tabs = [...page.querySelectorAll("[data-collected-view]")];
            const direction = event.key === "ArrowRight" ? 1 : -1;
            const next = tabs[(tabs.indexOf(button) + direction + tabs.length) % tabs.length];
            next.focus();
            next.click();
        });
    });

    filterForm.addEventListener("submit", (event) => { event.preventDefault(); currentPage = 1; selectedSourceId = null; runAction(renderList); });
    filterReset.addEventListener("click", () => { filterForm.reset(); applyView("ACTION_REQUIRED"); currentPage = 1; selectedSourceId = null; runAction(renderList); });
    pagePrev.addEventListener("click", () => { if (currentPage > 1) { currentPage -= 1; selectedSourceId = null; runAction(renderList); } });
    pageNext.addEventListener("click", () => { if (currentPage < totalPages) { currentPage += 1; selectedSourceId = null; runAction(renderList); } });

    applyView("ACTION_REQUIRED", false);
    runAction(async () => { await Promise.all([renderSummary(), renderList()]); });
})();
