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
    let detailSequence = 0;
    let listSequence = 0;
    const attachmentClient = window.SanebAttachmentReview.client(window.fetch.bind(window));

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
        BODY_FETCH_FAILED: "본문 수집 오류입니다. 수집 원인을 확인하고 복구한 뒤 공고를 검수하세요.",
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
        BODY_ERRORS: "본문 수집 오류 공고",
        ALL: "전체 수집 공고(오류 포함)"
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
        detailSequence++;
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
            requestJson(`${sourceUrl}?semanticStatusCode=REVIEW_REQUIRED&bodyFetchFailed=false&page=1&size=1`),
            requestJson(`${sourceUrl}?semanticStatusCode=ACCEPTED&bodyFetchFailed=false&page=1&size=1`),
            requestJson(`${sourceUrl}?semanticStatusCode=EXCLUDED&bodyFetchFailed=false&page=1&size=1`)
        ]);
        page.querySelector("[data-collected-action-count]").textContent = `${actionData.totalCount || 0}건`;
        page.querySelector("[data-collected-accepted-count]").textContent = `${acceptedData.totalCount || 0}건`;
        page.querySelector("[data-collected-excluded-count]").textContent = `${excludedData.totalCount || 0}건`;
    };

    const applyView = (viewCode, resetOperationalStatus = true) => {
        currentView = viewCode;
        const semanticField = filterForm.elements.semanticStatusCode;
        const reviewField = filterForm.elements.reviewStatusCode;
        semanticField.value = viewCode === "ACTION_REQUIRED" ? "REVIEW_REQUIRED" : ["ALL", "BODY_ERRORS"].includes(viewCode) ? "" : viewCode;
        filterForm.elements.bodyFetchFailed.value = viewCode === "BODY_ERRORS" ? "true" : viewCode === "ALL" ? "" : "false";
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

    // 현재 방문 항목에 조회 조건만 보관한다. 원문·검수 입력은 저장하지 않는다.
    const navigationStateKey = "sanebCollectedList";
    const filterNames = ["providerCode", "reviewStatusCode", "targetCategoryCode", "supportTypeCode",
        "matchedGroupKindCode", "matchLocationCode", "ruleReleaseId", "keyword"];
    let appliedFilters = {};
    const saveNavigationState = () => {
        window.history.replaceState({...window.history.state, [navigationStateKey]: {
            version: 1, view: currentView, page: currentPage, sourceId: selectedSourceId,
            filters: {...appliedFilters}
        }}, "");
    };
    const restoreNavigationState = () => {
        const saved = window.history.state?.[navigationStateKey];
        filterForm.reset();
        const valid = saved?.version === 1 && Object.hasOwn(viewLabels, saved.view);
        applyView(valid ? saved.view : "ACTION_REQUIRED");
        if (valid) {
            filterNames.forEach((name) => {
                const value = saved.filters?.[name];
                if (typeof value === "string") filterForm.elements[name].value = value;
            });
        }
        currentPage = valid && Number.isSafeInteger(saved.page) && saved.page > 0 ? saved.page : 1;
        selectedSourceId = valid && typeof saved.sourceId === "string" ? saved.sourceId : null;
    };

    const selectSource = async (sourceId, selectedButton) => {
        selectedSourceId = sourceId;
        saveNavigationState();
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
        const sequence = ++listSequence;
        appliedFilters = Object.fromEntries(filterNames.map((name) => [name, filterForm.elements[name].value]));
        saveNavigationState();
        showDetailMessage("공고 목록을 조회하고 있습니다.");
        sourceList.replaceChildren(appendText(document.createDocumentFragment(), "p", "수집 공고를 조회하고 있습니다.", "collected-empty-state"));
        const params = new URLSearchParams(new FormData(filterForm));
        [...params.entries()].forEach(([key, value]) => { if (!value) params.delete(key); });
        params.set("page", String(currentPage));
        params.set("size", "15");
        let data;
        try {
            data = await requestJson(`${sourceUrl}?${params.toString()}`);
        } catch (error) {
            if (sequence !== listSequence) return;
            throw error;
        }
        if (sequence !== listSequence) return;

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
            saveNavigationState();
            appendText(sourceList, "p", "조건에 맞는 수집 공고가 없습니다.", "collected-empty-state");
            showDetailMessage("분류함 또는 검색 조건을 바꾸어 주세요.");
            return;
        }

        const selectedItem = items.find((item) => item.sourceId === selectedSourceId) || items[0];
        selectedSourceId = selectedItem.sourceId;
        saveNavigationState();
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
        appendText(section, "h4", "원문 첨부 링크 · 과거 수집 정보");
        appendText(section, "p", "아래 링크 유무는 첨부 다운로드·추출 상태와 다릅니다. 실제 처리 상태는 첨부 근거에서 확인하세요.", "classification-scope-note");
        const list = document.createElement("div");
        list.className = "collected-attachment-list";
        if (!attachments?.length) appendText(list, "p", "이전 수집 정보에 첨부 링크가 없습니다. 실제 첨부 없음이 확인된 것은 아닙니다.", "muted-copy");
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
        const sequence = detailSequence;
        let data;
        try {
            data = await requestJson(`${sourceUrl}/${encodeURIComponent(sourceId)}`);
        } catch (error) {
            if (sequence === detailSequence && sourceId === selectedSourceId) showDetailMessage(error.message, true);
            return;
        }
        try {
            data.classification = await requestJson(`${sourceUrl}/${encodeURIComponent(sourceId)}/classification`);
        } catch (error) {
            data.classificationLoadError = error.message;
        }
        if (sequence !== detailSequence || sourceId !== selectedSourceId) return;
        selectedSource = data;
        const classification = classificationOf(data);
        const semanticStatus = classification.semanticStatusCode || data.semanticStatusCode;
        sourceDetail.replaceChildren();
        appendText(sourceDetail, "h3", data.title);
        appendText(sourceDetail, "p", `${data.publicCode || "-"} · ${statusLabel(data.providerCode)} · 원문 등록 ${formatDateTime(data.postedAt)}`, "muted-copy");

        const actions = document.createElement("div");
        actions.className = "source-detail-actions collected-primary-actions";
        if (semanticStatus !== "EXCLUDED") {
            const attachmentLink = document.createElement("a");
            attachmentLink.className = "secondary-action small-action";
            attachmentLink.href = `/app/admin/collected-announcements/${encodeURIComponent(data.sourceId)}/attachments`;
            attachmentLink.textContent = "본문·첨부 근거와 검수";
            actions.append(attachmentLink);
        }
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
        if (semanticStatus !== "EXCLUDED") renderAttachmentOverview(data.sourceId, sequence);
        renderAttachments(data.attachments);

        const bodySection = document.createElement("section");
        bodySection.className = "collected-detail-section";
        appendText(bodySection, "h4", "원문 본문");
        appendText(bodySection, "pre", data.bodyText || "본문 정보가 없습니다.", "source-body");
        sourceDetail.append(bodySection);
        if (canManage && data.providerCode === "LOCAL_GOV_NOTICE" && semanticStatus !== "EXCLUDED") {
            renderBodyRefresh(bodySection, data.sourceId, sequence);
        }
    };

    const renderBodyRefresh = (parent, sourceId, sequence) => {
        const panel = document.createElement("details");
        appendText(panel, "summary", "기존 본문 복구 · 단건 미리보기");
        appendText(panel, "p", "등록된 공식 상세 페이지 1건을 제한된 재시도·리디렉션 범위에서 조회합니다. 첨부파일은 다시 받지 않습니다. 미리보기는 30분 동안 적용할 수 있으며, 적용 전에는 현재 본문과 판정을 바꾸지 않습니다.");
        const result = document.createElement("div");
        result.setAttribute("aria-live", "polite");
        const previewButton = appendText(panel, "button", "공식 본문 조회 후 변경 미리보기", "secondary-action small-action");
        previewButton.type = "button";
        panel.append(result);
        parent.append(panel);
        const path = `/api/v2/admin/announcement-sources/${encodeURIComponent(sourceId)}/body-refresh-previews`;
        previewButton.addEventListener("click", async () => {
            previewButton.disabled = true;
            result.replaceChildren();
            appendText(result, "p", "공식 본문을 조회·정제하고 있습니다.");
            try {
                const preview = await attachmentClient(path, {method: "POST"});
                if (sequence !== detailSequence || sourceId !== selectedSourceId) return;
                if (!preview || preview.sourceId !== sourceId || !preview.previewId || typeof preview.afterBody !== "string") {
                    throw new Error("공고 식별자 또는 정제 본문 응답이 올바르지 않습니다. 새 미리보기를 생성하세요.");
                }
                result.replaceChildren();
                appendText(result, "p", `판정 변경: ${statusLabel(preview.beforeStatusCode)} → ${statusLabel(preview.afterStatusCode)}`);
                appendText(result, "p", `새 판정 근거: ${reasonLabel(preview.reasonCode)}`);
                appendText(result, "p", `지원대상: ${(preview.targetCategoryCodes || []).map(statusLabel).join(", ") || "없음"} / 지원형태: ${(preview.supportTypeCodes || []).map(statusLabel).join(", ") || "없음"}`);
                appendText(result, "h5", "적용할 정제 본문");
                appendText(result, "pre", preview.afterBody, "source-body");
                appendText(result, "p", `적용 유효기간: ${formatDateTime(preview.expiresAt)}`);
                if (!preview.isChanged) {
                    appendText(result, "p", "기존 본문과 같습니다. 새 버전을 생성하지 않습니다.");
                    return;
                }
                const label = document.createElement("label"), acknowledged = document.createElement("input");
                acknowledged.type = "checkbox";
                label.append(acknowledged, document.createTextNode(" 이 공고 1건의 본문·기본 판정을 새 버전으로 저장하고 이전 첨부 근거는 재확인해야 함을 확인했습니다. 운영 공고·첨부 정책은 바꾸지 않습니다."));
                result.append(label);
                const apply = appendText(result, "button", "확인한 본문 1건 적용", "primary-action small-action");
                apply.type = "button"; apply.disabled = true;
                const receipt = appendText(result, "p", "적용 전입니다.");
                acknowledged.addEventListener("change", () => { apply.disabled = !acknowledged.checked; });
                apply.addEventListener("click", async () => {
                    apply.disabled = true; acknowledged.disabled = true; previewButton.disabled = true;
                    receipt.textContent = "고정한 본문을 적용하고 있습니다. 외부 파일을 요청하지 않습니다.";
                    try {
                        const applied = await attachmentClient(`${path}/${encodeURIComponent(preview.previewId)}/apply`, {method:"POST"});
                        if (sequence !== detailSequence || sourceId !== selectedSourceId) return;
                        if (!applied || applied.sourceId !== sourceId || applied.previewId !== preview.previewId || !applied.evaluationId || applied.statusCode !== "APPLIED") {
                            const mismatch = new Error("본문 적용 영수증을 확인하지 못했습니다. 같은 미리보기로 재시도하여 결과를 확인하세요.");
                            mismatch.uncertain = true;
                            throw mismatch;
                        }
                        receipt.textContent = "본문과 기본 판정의 새 버전이 저장됐습니다. 최신 첨부 근거를 준비한 뒤 최종 검수하세요.";
                        apply.hidden = true;
                        const refresh = appendText(result, "button", "최신 본문·첨부 상태 조회", "secondary-action small-action");
                        refresh.type = "button";
                        refresh.addEventListener("click", () => runAction(() => renderSourceDetail(sourceId)));
                    } catch (error) {
                        if (sequence !== detailSequence || sourceId !== selectedSourceId) return;
                        receipt.textContent = error.message;
                        apply.textContent = error.uncertain ? "적용 결과 미확정 · 같은 미리보기로 재시도" : "같은 미리보기 적용 재시도";
                        apply.disabled = false;
                        // 불확실한 적용 결과를 확인하기 전에는 새 본문으로 대체하지 않는다.
                        previewButton.disabled = !!error.uncertain;
                    }
                });
            } catch (error) {
                if (sequence !== detailSequence || sourceId !== selectedSourceId) return;
                result.replaceChildren();
                appendText(result, "p", `본문 미리보기를 준비하지 못했습니다. ${error.message}`, "is-error");
            } finally {
                previewButton.disabled = false;
            }
        });
    };

    const renderAttachmentOverview = (sourceId, sequence) => {
        const section = document.createElement("section");
        section.className = "collected-detail-section";
        section.setAttribute("aria-label", "현재 첨부 처리 상태");
        const content = document.createElement("div");
        content.setAttribute("role", "status");
        appendText(section, "h4", "현재 첨부 처리 상태");
        section.append(content);
        const link = document.createElement("a");
        link.href = `/app/admin/collected-announcements/${encodeURIComponent(sourceId)}/attachments`;
        link.textContent = "첨부 파일·추출 텍스트·검수 근거 확인";
        section.append(link);
        sourceDetail.append(section);
        const load = async () => {
            content.replaceChildren();
            appendText(content, "p", "첨부 처리 상태를 조회하고 있습니다.");
            try {
                const source = await attachmentClient(`/api/v2/admin/announcement-sources/${encodeURIComponent(sourceId)}/attachment-classification`);
                if (sequence !== detailSequence || sourceId !== selectedSourceId) return;
                if (source.sourceId !== sourceId) throw new Error("공고 식별자가 일치하지 않습니다. 다시 조회하세요.");
                const summary = window.SanebAttachmentReview.attachmentOverview(source);
                content.replaceChildren();
                appendText(content, "strong", summary.status);
                appendText(content, "p", summary.progress);
                appendText(content, "p", `작업: ${summary.job || "미확인"}`);
                if (summary.error) appendText(content, "p", summary.error, "is-error");
                if (summary.stale) appendText(content, "p", "이전 첨부 근거입니다. 현재 본문·판정과 연결된 최신 근거를 확인하세요.", "is-error");
                appendText(content, "p", summary.applied ? "첨부 판정 적용 대상 · 최종 검수 상태는 근거 화면에서 확인" : "첨부 판정 미적용 · 미리보기와 현재 분류는 별개");
                appendText(content, "p", summary.guidance);
            } catch (error) {
                if (sequence !== detailSequence || sourceId !== selectedSourceId) return;
                content.replaceChildren();
                appendText(content, "p", `첨부 상태를 불러오지 못했습니다. ${error.message}`, "is-error");
                const retry = document.createElement("button");
                retry.type = "button";
                retry.className = "secondary-action small-action";
                retry.textContent = "첨부 상태 다시 조회";
                retry.addEventListener("click", load);
                content.append(retry);
            }
        };
        void load();
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

    const restoreAndLoad = () => {
        restoreNavigationState();
        runAction(async () => { await Promise.all([renderSummary(), renderList()]); });
    };
    // pageshow 이후 복원해야 브라우저의 자동 폼 복원과 조회 조건이 엇갈리지 않는다.
    // BFCache 복귀도 같은 경로로 처리하고, 떠난 페이지의 늦은 응답은 무효화한다.
    window.addEventListener("pagehide", () => { listSequence++; detailSequence++; });
    window.addEventListener("pageshow", restoreAndLoad);
    if (document.readyState === "complete") restoreAndLoad();
})();
