(() => {
    "use strict";

    const page = document.querySelector("[data-collected-announcement-page]");
    if (!page) {
        return;
    }

    const sourceUrl = page.dataset.sourceUrl;
    const roleCode = page.dataset.roleCode;
    const canManage = roleCode === "OPERATOR" || roleCode === "ADMIN";
    const filterForm = page.querySelector("[data-collected-filter-form]");
    const filterReset = page.querySelector("[data-collected-filter-reset]");
    const sourceList = page.querySelector("[data-collected-list]");
    const sourceDetail = page.querySelector("[data-collected-detail]");
    const totalCount = page.querySelector("[data-collected-total-count]");
    const pendingCount = page.querySelector("[data-collected-pending-count]");
    const filteredCount = page.querySelector("[data-collected-filtered-count]");
    const listCount = page.querySelector("[data-collected-list-count]");
    const pagePrev = page.querySelector("[data-collected-page-prev]");
    const pageNext = page.querySelector("[data-collected-page-next]");
    const pageInfo = page.querySelector("[data-collected-page-info]");
    let currentPage = 1;
    let totalPages = 1;
    let selectedSourceId = null;

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
        EXACT_DUPLICATE: "동일 공고",
        SIMILAR: "유사 공고",
        PENDING: "검수 필요",
        CREATE_NEW_SELECTED: "신규 등록 선택",
        UPDATE_EXISTING_SELECTED: "기존 공고 갱신 선택",
        IGNORED: "무시",
        AUTO_CONFIRMED: "자동 중복 확인",
        ACCEPTED: "유효 후보",
        REVIEW_REQUIRED: "확인 필요",
        EXCLUDED: "제외됨",
        SOURCE_POLICY_COLLECT_ALL: "게시판 전체 수집 정책",
        SOURCE_POLICY_EXCLUDED: "수집 제외 출처",
        INCLUDE_KEYWORD_MATCHED: "지원사업 키워드 일치",
        EXCLUDE_KEYWORD_MATCHED: "제외 키워드 일치",
        NO_INCLUDE_KEYWORD: "지원사업 키워드 없음",
        INCLUDE_AND_EXCLUDE_KEYWORD: "포함·제외 키워드 동시 일치"
    };

    const statusLabel = (code) => labels[code] || code || "-";

    const requestJson = async (url, options = {}) => {
        const response = await fetch(url, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            }
        });
        const payload = await response.json().catch(() => null);
        if (!response.ok || !payload?.success) {
            throw new Error(payload?.message || "요청 처리에 실패했습니다.");
        }
        return payload.data;
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
        try {
            return new Intl.DateTimeFormat("ko-KR", {
                dateStyle: "medium",
                timeStyle: "short"
            }).format(new Date(value));
        } catch (error) {
            return value;
        }
    };

    const showDetailMessage = (message, isError = false) => {
        sourceDetail.innerHTML = "";
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
            const anchor = document.createElement("a");
            anchor.href = value;
            anchor.target = "_blank";
            anchor.rel = "noopener noreferrer";
            anchor.textContent = "원문 새 창에서 보기";
            description.appendChild(anchor);
        } else {
            description.textContent = value == null || value === "" ? "-" : String(value);
        }
        meta.appendChild(description);
    };

    const renderSummary = async () => {
        const [allData, pendingData] = await Promise.all([
            requestJson(`${sourceUrl}?page=1&size=1`),
            requestJson(`${sourceUrl}?reviewStatusCode=REVIEW_PENDING&page=1&size=1`)
        ]);
        totalCount.textContent = `${allData.totalCount}건`;
        pendingCount.textContent = `${pendingData.totalCount}건`;
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
        if (window.matchMedia("(max-width: 760px)").matches) {
            const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
            sourceDetail.scrollIntoView({behavior: reduceMotion ? "auto" : "smooth", block: "start"});
        }
    };

    const renderList = async () => {
        sourceList.innerHTML = '<p class="collected-empty-state">수집 공고를 조회하고 있습니다.</p>';
        const params = new URLSearchParams(new FormData(filterForm));
        params.set("page", String(currentPage));
        params.set("size", "15");
        const data = await requestJson(`${sourceUrl}?${params.toString()}`);

        currentPage = data.page || 1;
        totalPages = Math.max(1, data.totalPages || 1);
        pageInfo.textContent = `${currentPage} / ${totalPages}`;
        pagePrev.disabled = currentPage <= 1;
        pageNext.disabled = currentPage >= totalPages;
        filteredCount.textContent = `${data.totalCount}건`;
        listCount.textContent = `${data.totalCount}건`;
        sourceList.innerHTML = "";

        if (!data.items.length) {
            selectedSourceId = null;
            appendText(sourceList, "p", "조건에 맞는 수집 공고가 없습니다.", "collected-empty-state");
            showDetailMessage("검색 조건을 바꾸거나 수집 작업을 먼저 실행해 주세요.");
            return;
        }

        const selectedItem = data.items.find((item) => item.sourceId === selectedSourceId) || data.items[0];
        selectedSourceId = selectedItem.sourceId;
        let selectedButton = null;

        data.items.forEach((item) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "source-list-item collected-source-item";
            button.setAttribute("aria-pressed", String(item.sourceId === selectedSourceId));
            if (item.sourceId === selectedSourceId) {
                button.classList.add("is-selected");
                selectedButton = button;
            }

            appendText(button, "strong", item.title);
            const codeLine = document.createElement("span");
            codeLine.className = "collected-source-code";
            codeLine.textContent = `${item.publicCode} · ${statusLabel(item.providerCode)} · `
                + `${statusLabel(item.reviewStatusCode)} · ${statusLabel(item.semanticStatusCode)}`;
            button.appendChild(codeLine);

            const meta = document.createElement("span");
            meta.className = "collected-source-meta";
            appendText(meta, "span", item.agencyName || "기관 미확인");
            appendText(meta, "span", `원문 등록 ${formatDateTime(item.postedAt)}`);
            button.appendChild(meta);
            button.addEventListener("click", () => runAction(() => selectSource(item.sourceId, button)));
            sourceList.appendChild(button);
        });

        updateSelectedButton(selectedButton);
        await renderSourceDetail(selectedSourceId);
    };

    const appendActionButton = (parent, text, className, handler, disabled = false) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = className;
        button.textContent = text;
        button.disabled = disabled;
        button.addEventListener("click", () => runAction(handler));
        parent.appendChild(button);
    };

    const renderDuplicateCandidates = (data) => {
        appendText(sourceDetail, "h4", "운영 공고 중복 후보");
        const wrapper = document.createElement("div");
        wrapper.className = "source-duplicate-list";
        const candidates = data.duplicateCandidates || [];
        if (!candidates.length) {
            appendText(wrapper, "p", "검출된 중복 또는 유사 공고가 없습니다.", "muted-copy");
            sourceDetail.appendChild(wrapper);
            return;
        }

        candidates.forEach((candidate) => {
            const card = document.createElement("div");
            card.className = "source-duplicate-card";
            appendText(card, "strong", `${candidate.announcementCode || "-"} · ${candidate.announcementTitle || "-"}`);
            appendText(card, "span", `${candidate.matchTypeLabel || statusLabel(candidate.matchTypeCode)} · ${candidate.decisionStatusLabel || statusLabel(candidate.decisionStatusCode)}`, "muted-copy");
            appendText(card, "p", candidate.similarityReason || "비교 항목 일부가 일치하거나 유사합니다.", "muted-copy");
            const flags = document.createElement("div");
            flags.className = "source-duplicate-flags";
            [
                ["사업명", candidate.titleMatched],
                ["주관기관", candidate.agencyMatched],
                ["공고번호", candidate.providerNoticeMatched],
                ["신청기간", candidate.periodMatched],
                ["원문 URL", candidate.sourceUrlMatched]
            ].forEach(([name, matched]) => appendText(flags, "span", `${name}: ${matched ? "일치" : "미일치"}`));
            card.appendChild(flags);

            if (canManage && candidate.decisionStatusCode === "PENDING") {
                const actions = document.createElement("div");
                actions.className = "source-detail-actions";
                [
                    ["CREATE_NEW", "신규 공고로 검수"],
                    ["UPDATE_EXISTING", "기존 공고 갱신"],
                    ["IGNORE", "검수 제외"]
                ].forEach(([actionCode, text]) => appendActionButton(
                    actions,
                    text,
                    "secondary-action small-action",
                    () => decideDuplicate(data.sourceId, candidate.candidateId, actionCode)
                ));
                card.appendChild(actions);
            }
            wrapper.appendChild(card);
        });
        sourceDetail.appendChild(wrapper);
    };

    const renderSourceDuplicates = (data) => {
        appendText(sourceDetail, "h4", "다른 수집처의 중복 원문");
        const wrapper = document.createElement("div");
        wrapper.className = "source-duplicate-list";
        const duplicates = data.sourceDuplicates || [];
        if (!duplicates.length) {
            appendText(wrapper, "p", "다른 수집처에서 확인된 중복 원문이 없습니다.", "muted-copy");
            sourceDetail.appendChild(wrapper);
            return;
        }

        duplicates.forEach((item) => {
            const card = document.createElement("div");
            card.className = "source-duplicate-card";
            appendText(card, "strong", `${item.candidatePublicCode} · ${item.candidateTitle}`);
            appendText(card, "span", `${statusLabel(item.candidateProviderCode)} · ${statusLabel(item.matchTypeCode)} · ${statusLabel(item.decisionStatusCode)}`, "muted-copy");
            appendText(card, "p", item.matchReason, "muted-copy");
            if (canManage && item.decisionStatusCode === "PENDING") {
                const actions = document.createElement("div");
                actions.className = "source-detail-actions";
                [
                    ["CREATE_NEW", "신규 공고로 검수"],
                    ["UPDATE_EXISTING", "기존 공고 갱신"],
                    ["IGNORE", "검수 제외"]
                ].forEach(([actionCode, text]) => appendActionButton(
                    actions,
                    text,
                    "secondary-action small-action",
                    () => decideSourceDuplicate(data.sourceId, item.duplicateId, actionCode)
                ));
                card.appendChild(actions);
            }
            wrapper.appendChild(card);
        });
        sourceDetail.appendChild(wrapper);
    };

    const renderAttachments = (attachments) => {
        appendText(sourceDetail, "h4", "첨부파일");
        const list = document.createElement("div");
        list.className = "collected-attachment-list";
        if (!attachments?.length) {
            appendText(list, "p", "수집된 첨부파일이 없습니다.", "muted-copy");
            sourceDetail.appendChild(list);
            return;
        }
        attachments.forEach((attachment) => {
            const anchor = document.createElement("a");
            anchor.href = attachment.fileUrl;
            anchor.target = "_blank";
            anchor.rel = "noopener noreferrer";
            anchor.textContent = attachment.fileName || "첨부파일 열기";
            list.appendChild(anchor);
        });
        sourceDetail.appendChild(list);
    };

    const renderSourceDetail = async (sourceId) => {
        sourceDetail.innerHTML = '<div class="collected-detail-empty"><strong>상세 정보를 불러오고 있습니다.</strong></div>';
        const data = await requestJson(`${sourceUrl}/${sourceId}`);
        sourceDetail.innerHTML = "";
        appendText(sourceDetail, "h3", data.title);
        appendText(
            sourceDetail,
            "p",
            `${data.publicCode} · ${statusLabel(data.providerCode)} · ${statusLabel(data.reviewStatusCode)} · `
                + `${statusLabel(data.semanticStatusCode)}`,
            "muted-copy"
        );

        if (data.semanticStatusCode !== "ACCEPTED") {
            const semanticNotice = document.createElement("div");
            semanticNotice.className = `collected-semantic-notice is-${String(data.semanticStatusCode).toLowerCase()}`;
            appendText(
                semanticNotice,
                "strong",
                data.semanticStatusCode === "EXCLUDED" ? "검수 대상에서 제외된 게시물입니다." : "운영자 확인이 필요한 게시물입니다."
            );
            appendText(
                semanticNotice,
                "p",
                `${statusLabel(data.semanticReasonCode)}${data.semanticMatchedKeywords ? ` · 일치: ${data.semanticMatchedKeywords}` : ""}`
            );
            sourceDetail.appendChild(semanticNotice);
        }

        const actions = document.createElement("div");
        actions.className = "source-detail-actions";
        [
            ["CONDITION_INPUT_REQUIRED", "조건 입력 필요"],
            ["REVIEW_COMPLETED", "검수완료"],
            ["ARCHIVED", "보관"]
        ].forEach(([statusCode, text]) => {
            if (data.reviewStatusCode !== statusCode) {
                appendActionButton(
                    actions,
                    text,
                    "secondary-action small-action",
                    () => updateSourceStatus(data.sourceId, statusCode)
                );
            }
        });

        if (canManage) {
            const pendingCandidates = [
                ...(data.duplicateCandidates || []),
                ...(data.sourceDuplicates || [])
            ].filter((item) => item.decisionStatusCode === "PENDING");
            appendActionButton(
                actions,
                pendingCandidates.length ? "중복 검수 후 공고 입력" : "공고 입력 시작",
                "primary-action small-action",
                () => convertSource(data.sourceId),
                pendingCandidates.length > 0 || data.semanticStatusCode === "EXCLUDED"
            );
        }
        sourceDetail.appendChild(actions);

        const meta = document.createElement("dl");
        meta.className = "source-meta";
        createMetaValue(meta, "기관", data.agencyName);
        createMetaValue(meta, "원문 등록일", formatDateTime(data.postedAt));
        createMetaValue(meta, "수집일", formatDateTime(data.collectedAt));
        createMetaValue(meta, "신청 시작일", data.applicationStartDate);
        createMetaValue(meta, "신청 마감일", data.applicationEndDate);
        createMetaValue(meta, "수집 범위", statusLabel(data.sourceCompletenessCode));
        createMetaValue(meta, "수집 판정", statusLabel(data.semanticStatusCode));
        createMetaValue(meta, "판정 근거", statusLabel(data.semanticReasonCode));
        createMetaValue(meta, "일치 키워드", data.semanticMatchedKeywords);
        createMetaValue(meta, "원문", data.sourceUrl, true);
        createMetaValue(meta, "신청방법", data.applicationMethodText);
        createMetaValue(meta, "문의처", data.inquiryText);
        sourceDetail.appendChild(meta);

        renderDuplicateCandidates(data);
        renderSourceDuplicates(data);
        renderAttachments(data.attachments);

        appendText(sourceDetail, "h4", "하이라이트");
        const highlights = document.createElement("div");
        highlights.className = "source-highlight-list";
        if (!data.highlights?.length) {
            appendText(highlights, "p", "하이라이트가 없습니다.", "muted-copy");
        } else {
            data.highlights.forEach((item) => {
                const highlight = document.createElement("div");
                highlight.className = "source-highlight";
                appendText(highlight, "strong", statusLabel(item.highlightTypeCode));
                appendText(highlight, "span", item.matchedText);
                highlights.appendChild(highlight);
            });
        }
        sourceDetail.appendChild(highlights);

        appendText(sourceDetail, "h4", "원문 본문");
        appendText(sourceDetail, "pre", data.bodyText || "본문 정보가 없습니다.", "source-body");
    };

    const refreshAfterChange = async () => {
        await Promise.all([renderSummary(), renderList()]);
    };

    const updateSourceStatus = async (sourceId, reviewStatusCode) => {
        await requestJson(`${sourceUrl}/${sourceId}/review-status`, {
            method: "PATCH",
            body: JSON.stringify({reviewStatusCode, reason: "수집 공고 검수 화면 상태 변경"})
        });
        await refreshAfterChange();
    };

    const decideDuplicate = async (sourceId, candidateId, decisionActionCode) => {
        await requestJson(`${sourceUrl}/${sourceId}/duplicate-candidates/${candidateId}/decision`, {
            method: "PATCH",
            body: JSON.stringify({decisionActionCode, decisionNote: "수집 공고 검수 화면 중복 결정"})
        });
        await refreshAfterChange();
    };

    const decideSourceDuplicate = async (sourceId, duplicateId, decisionActionCode) => {
        await requestJson(`${sourceUrl}/${sourceId}/source-duplicates/${duplicateId}/decision`, {
            method: "PATCH",
            body: JSON.stringify({decisionActionCode, decisionNote: "수집 공고 검수 화면 교차 수집처 중복 결정"})
        });
        await refreshAfterChange();
    };

    const convertSource = async (sourceId) => {
        const result = await requestJson(`${sourceUrl}/${sourceId}/announcements`, {
            method: "POST",
            body: JSON.stringify({targetTypeCode: "BUSINESS", incomeJudgementCode: "VAT_TAX_BASE_ONLY"})
        });
        window.location.href = `/app/announcements/input?announcementCode=${encodeURIComponent(result.announcementCode || "")}`;
    };

    filterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        currentPage = 1;
        selectedSourceId = null;
        runAction(renderList);
    });

    filterReset.addEventListener("click", () => {
        filterForm.reset();
        currentPage = 1;
        selectedSourceId = null;
        runAction(renderList);
    });

    pagePrev.addEventListener("click", () => {
        if (currentPage > 1) {
            currentPage -= 1;
            selectedSourceId = null;
            runAction(renderList);
        }
    });

    pageNext.addEventListener("click", () => {
        if (currentPage < totalPages) {
            currentPage += 1;
            selectedSourceId = null;
            runAction(renderList);
        }
    });

    runAction(async () => {
        await Promise.all([renderSummary(), renderList()]);
    });
})();
