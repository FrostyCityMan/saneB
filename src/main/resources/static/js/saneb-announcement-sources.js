(() => {
    const page = document.querySelector("[data-announcement-source-page]");
    if (!page) {
        return;
    }

    const requestUrl = page.dataset.requestUrl;
    const runUrl = page.dataset.runUrl;
    const sourceUrl = page.dataset.sourceUrl;
    const localSourceUrl = page.dataset.localSourceUrl;
    const localParserUrl = page.dataset.localParserUrl;
    const localSummaryUrl = page.dataset.localSummaryUrl;
    const scheduleUrl = page.dataset.scheduleUrl;
    const roleCode = page.dataset.roleCode;
    const canApprove = roleCode === "APPROVER" || roleCode === "ADMIN";
    const requestForm = page.querySelector("[data-source-request-form]");
    const refreshButton = page.querySelector("[data-source-refresh]");
    const requestList = page.querySelector("[data-source-request-list]");
    const runList = page.querySelector("[data-source-run-list]");
    const sourceList = page.querySelector("[data-source-list]");
    const sourceDetail = page.querySelector("[data-source-detail]");
    const sourceFilterForm = page.querySelector("[data-source-filter-form]");
    const localSummary = page.querySelector("[data-local-summary]");
    const localSourceFilterForm = page.querySelector("[data-local-source-filter-form]");
    const localSourceForm = page.querySelector("[data-local-source-form]");
    const localSourceList = page.querySelector("[data-local-source-list]");
    const localParserSelect = page.querySelector("[data-local-parser-select]");
    const localFormReset = page.querySelector("[data-local-form-reset]");
    const localSaveButton = page.querySelector("[data-local-save-button]");
    const localPagePrev = page.querySelector("[data-local-page-prev]");
    const localPageNext = page.querySelector("[data-local-page-next]");
    const localPageInfo = page.querySelector("[data-local-page-info]");
    const scheduleForm = page.querySelector("[data-schedule-form]");
    const scheduleList = page.querySelector("[data-schedule-list]");
    let localSourcePage = 1;
    let localSourceTotalPages = 1;

    const label = {
        BIZINFO: "기업마당",
        GOV24_PUBLIC_SERVICE: "정부24 공공서비스",
        LOCAL_GOV_NOTICE: "전국 지자체 공고",
        MANUAL: "버튼 요청",
        BATCH: "배치 요청",
        APPROVAL_PENDING: "승인 대기",
        APPROVED: "승인됨",
        REJECTED: "반려",
        CANCELED: "취소",
        EXPIRED: "만료",
        RUNNING: "실행 중",
        COMPLETED: "완료",
        PARTIAL_FAILED: "일부 실패",
        FAILED: "실패",
        REVIEW_PENDING: "검수대기",
        CONDITION_INPUT_REQUIRED: "조건 입력 필요",
        REVIEW_COMPLETED: "검수완료",
        ACTIVATED: "활성 전환",
        ARCHIVED: "보관",
        COLLECTED: "수집완료",
        DUPLICATE: "중복",
        SKIPPED_ENDED: "종료 제외",
        EXACT_DUPLICATE: "동일 공고",
        SIMILAR: "유사 공고",
        PENDING: "검수 필요",
        CREATE_NEW_SELECTED: "신규 등록 선택",
        UPDATE_EXISTING_SELECTED: "기존 공고 업데이트 선택",
        IGNORED: "무시",
        AUTO_CONFIRMED: "자동 중복 확인",
        READY: "수집 준비",
        SUCCESS: "수집 성공",
        NO_CHANGE: "변경 없음",
        URL_ERROR: "URL 오류",
        ACCESS_BLOCKED: "접근 차단",
        PARSER_UNSUPPORTED: "파서 확인 필요",
        CHECK_REQUIRED: "확인 필요",
        DISABLED: "사용 안 함",
        PAUSED: "일시중지"
    };

    const statusLabel = (code) => label[code] || code || "-";

    const formatDateTime = (value) => {
        if (!value) {
            return "-";
        }
        try {
            return new Intl.DateTimeFormat("ko-KR", {
                dateStyle: "short",
                timeStyle: "short"
            }).format(new Date(value));
        } catch (error) {
            return value;
        }
    };

    const requestJson = async (url, options = {}) => {
        const response = await fetch(url, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            }
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "요청 처리에 실패했습니다.");
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

    const runAction = async (action) => {
        try {
            await action();
        } catch (error) {
            window.alert(error.message || "요청 처리에 실패했습니다.");
        }
    };

    const renderLocalSummary = async () => {
        const data = await requestJson(localSummaryUrl);
        localSummary.innerHTML = "";
        const card = document.createElement("article");
        card.className = `local-source-signal signal-${String(data.trafficLightCode || "YELLOW").toLowerCase()}`;
        const dot = document.createElement("span");
        dot.className = `signal-dot signal-${String(data.trafficLightCode || "YELLOW").toLowerCase()}`;
        card.appendChild(dot);
        const title = data.trafficLightCode === "RED" ? "수집 오류 확인 필요"
            : data.trafficLightCode === "YELLOW" ? "신규·미처리 항목 확인 필요" : "지자체 공고 수집 정상";
        appendText(card, "strong", title);
        appendText(card, "small", `전체 ${data.totalCount}곳 · 사용 ${data.enabledCount}곳 · 오류 ${data.failedCount}곳 · 검수대기 ${data.reviewPendingCount}건`);
        localSummary.appendChild(card);
    };

    const renderLocalParsers = async () => {
        const current = localParserSelect.value;
        const data = await requestJson(localParserUrl);
        localParserSelect.innerHTML = "";
        data.forEach((item) => {
            const option = document.createElement("option");
            option.value = item.profileCode;
            option.textContent = `${item.profileName} (${item.parserTypeCode})`;
            localParserSelect.appendChild(option);
        });
        localParserSelect.value = current || "MANUAL_ONLY";
    };

    const fillLocalSourceForm = (item) => {
        Object.entries(item).forEach(([key, value]) => {
            const field = localSourceForm.elements.namedItem(key);
            if (field && value != null) {
                field.value = value;
            }
        });
        localSaveButton.textContent = "URL 수정";
        localSourceForm.scrollIntoView({behavior: "smooth", block: "start"});
    };

    const resetLocalSourceForm = () => {
        localSourceForm.reset();
        localSourceForm.elements.namedItem("sourceId").value = "";
        localSaveButton.textContent = "URL 등록";
    };

    const toggleLocalSource = async (item) => {
        await requestJson(`${localSourceUrl}/${item.sourceId}/enabled`, {
            method: "PATCH",
            body: JSON.stringify({enabled: !item.enabled})
        });
        await Promise.all([renderLocalSources(), renderLocalSummary()]);
    };

    const requestLocalCollection = async (item) => {
        await requestJson(`${localSourceUrl}/${item.sourceId}/collection-requests`, {
            method: "POST",
            body: JSON.stringify({maxCount: 100, requestNote: `${item.institutionName} 수동 수집 요청`})
        });
        await renderRequests();
    };

    const deleteLocalSource = async (item) => {
        if (!window.confirm(`${item.institutionName} URL을 삭제하시겠습니까? 수집 이력은 보존됩니다.`)) {
            return;
        }
        await requestJson(`${localSourceUrl}/${item.sourceId}`, {method: "DELETE"});
        await Promise.all([renderLocalSources(), renderLocalSummary()]);
    };

    const renderLocalSources = async () => {
        localSourceList.innerHTML = "<tr><td colspan=\"7\">조회 중입니다.</td></tr>";
        const params = new URLSearchParams(new FormData(localSourceFilterForm));
        params.set("page", String(localSourcePage));
        params.set("size", "20");
        const data = await requestJson(`${localSourceUrl}?${params.toString()}`);
        localSourceTotalPages = Math.max(1, data.totalPages || 1);
        localPageInfo.textContent = `${localSourcePage} / ${localSourceTotalPages}`;
        localPagePrev.disabled = localSourcePage <= 1;
        localPageNext.disabled = localSourcePage >= localSourceTotalPages;
        localSourceList.innerHTML = "";
        if (!data.items.length) {
            localSourceList.innerHTML = "<tr><td colspan=\"7\">조건에 맞는 지자체 URL이 없습니다.</td></tr>";
            return;
        }
        data.items.forEach((item) => {
            const row = document.createElement("tr");
            const signalCell = document.createElement("td");
            const dot = document.createElement("span");
            dot.className = `signal-dot signal-${String(item.trafficLightCode).toLowerCase()}`;
            dot.title = item.trafficLightCode === "RED" ? "수집 오류" : item.trafficLightCode === "YELLOW" ? "확인 필요" : "정상";
            signalCell.appendChild(dot);
            row.appendChild(signalCell);
            appendText(row, "td", item.publicCode);
            appendText(row, "td", `${item.sidoName} ${item.sigunguName} · ${item.institutionName}`);
            appendText(row, "td", statusLabel(item.collectionStatusCode));
            appendText(row, "td", formatDateTime(item.lastCollectedAt));

            const enabledCell = document.createElement("td");
            const toggleButton = document.createElement("button");
            toggleButton.type = "button";
            toggleButton.className = item.enabled ? "status-toggle is-on" : "status-toggle";
            toggleButton.textContent = item.enabled ? "ON" : "OFF";
            toggleButton.setAttribute("aria-label", `${item.institutionName} 수집 ${item.enabled ? "끄기" : "켜기"}`);
            toggleButton.addEventListener("click", () => runAction(() => toggleLocalSource(item)));
            enabledCell.appendChild(toggleButton);
            row.appendChild(enabledCell);

            const actions = document.createElement("td");
            actions.className = "table-actions";
            const link = document.createElement("a");
            link.className = "secondary-action small-action";
            link.href = item.noticeUrl;
            link.target = "_blank";
            link.rel = "noopener noreferrer";
            link.textContent = "바로가기";
            actions.appendChild(link);
            [["수정", () => fillLocalSourceForm(item)], ["수집 요청", () => runAction(() => requestLocalCollection(item))], ["삭제", () => runAction(() => deleteLocalSource(item))]].forEach(([text, handler]) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "secondary-action small-action";
                button.textContent = text;
                button.addEventListener("click", handler);
                actions.appendChild(button);
            });
            row.appendChild(actions);
            localSourceList.appendChild(row);
        });
    };

    const updateScheduleStatus = async (scheduleId, scheduleStatusCode) => {
        await requestJson(`${scheduleUrl}/${scheduleId}/status`, {
            method: "PATCH",
            body: JSON.stringify({scheduleStatusCode, approvalNote: "관리자 화면 처리"})
        });
        await renderSchedules();
    };

    const renderSchedules = async () => {
        scheduleList.innerHTML = "<tr><td colspan=\"5\">조회 중입니다.</td></tr>";
        const data = await requestJson(scheduleUrl);
        scheduleList.innerHTML = "";
        if (!data.length) {
            scheduleList.innerHTML = "<tr><td colspan=\"5\">등록된 정기 수집 일정이 없습니다.</td></tr>";
            return;
        }
        data.forEach((item) => {
            const row = document.createElement("tr");
            appendText(row, "td", item.publicCode);
            appendText(row, "td", item.scheduleName);
            appendText(row, "td", statusLabel(item.scheduleStatusCode));
            appendText(row, "td", formatDateTime(item.nextRunAt));
            const actions = document.createElement("td");
            if (canApprove && (item.scheduleStatusCode === "APPROVAL_PENDING" || item.scheduleStatusCode === "PAUSED")) {
                const approve = document.createElement("button");
                approve.type = "button";
                approve.className = "primary-action small-action";
                approve.textContent = "승인";
                approve.addEventListener("click", () => runAction(() => updateScheduleStatus(item.scheduleId, "APPROVED")));
                actions.appendChild(approve);
            }
            if (canApprove && item.scheduleStatusCode === "APPROVED") {
                const pause = document.createElement("button");
                pause.type = "button";
                pause.className = "secondary-action small-action";
                pause.textContent = "중지";
                pause.addEventListener("click", () => runAction(() => updateScheduleStatus(item.scheduleId, "PAUSED")));
                actions.appendChild(pause);
            }
            row.appendChild(actions);
            scheduleList.appendChild(row);
        });
    };

    const renderRequests = async () => {
        requestList.innerHTML = "<tr><td colspan=\"6\">조회 중입니다.</td></tr>";
        const data = await requestJson(`${requestUrl}?page=1&size=20`);
        requestList.innerHTML = "";
        if (!data.items.length) {
            requestList.innerHTML = "<tr><td colspan=\"6\">수집 요청이 없습니다.</td></tr>";
            return;
        }
        data.items.forEach((item) => {
            const row = document.createElement("tr");
            appendText(row, "td", item.publicCode);
            appendText(row, "td", statusLabel(item.providerCode));
            appendText(row, "td", statusLabel(item.requestTypeCode));
            appendText(row, "td", statusLabel(item.requestStatusCode));
            appendText(row, "td", formatDateTime(item.requestedAt));

            const actions = document.createElement("td");
            if (item.requestStatusCode === "APPROVAL_PENDING" && canApprove) {
                const approveButton = document.createElement("button");
                approveButton.className = "secondary-action small-action";
                approveButton.type = "button";
                approveButton.textContent = "승인";
                approveButton.addEventListener("click", () => approveRequest(item.requestId, "APPROVED"));
                actions.appendChild(approveButton);
            }
            if (item.requestStatusCode === "APPROVED" && canApprove) {
                const runButton = document.createElement("button");
                runButton.className = "primary-action small-action";
                runButton.type = "button";
                runButton.textContent = "API 호출";
                runButton.addEventListener("click", () => runRequest(item.requestId));
                actions.appendChild(runButton);
            }
            row.appendChild(actions);
            requestList.appendChild(row);
        });
    };

    const renderRuns = async () => {
        runList.innerHTML = "<tr><td colspan=\"7\">조회 중입니다.</td></tr>";
        const data = await requestJson(`${runUrl}?page=1&size=20`);
        runList.innerHTML = "";
        if (!data.items.length) {
            runList.innerHTML = "<tr><td colspan=\"7\">수집 실행 이력이 없습니다.</td></tr>";
            return;
        }
        data.items.forEach((item) => {
            const row = document.createElement("tr");
            appendText(row, "td", item.publicCode);
            appendText(row, "td", item.requestPublicCode);
            appendText(row, "td", statusLabel(item.runStatusCode));
            appendText(row, "td", `${item.collectedCount}건`);
            appendText(row, "td", `${item.duplicateCount}건`);
            appendText(row, "td", `${item.skippedEndedCount}건`);
            appendText(row, "td", `${item.failedCount}건`);
            runList.appendChild(row);
        });
    };

    const renderSources = async () => {
        sourceList.textContent = "조회 중입니다.";
        const params = new URLSearchParams(new FormData(sourceFilterForm));
        params.set("page", "1");
        params.set("size", "20");
        const data = await requestJson(`${sourceUrl}?${params.toString()}`);
        sourceList.innerHTML = "";
        if (!data.items.length) {
            sourceList.textContent = "수집 원문이 없습니다.";
            return;
        }
        data.items.forEach((item) => {
            const button = document.createElement("button");
            button.className = "source-list-item";
            button.type = "button";
            appendText(button, "strong", item.title);
            appendText(button, "span", `${item.publicCode} · ${statusLabel(item.providerCode)} · ${statusLabel(item.reviewStatusCode)}`);
            button.addEventListener("click", () => renderSourceDetail(item.sourceId));
            sourceList.appendChild(button);
        });
    };

    const renderSourceDetail = async (sourceId) => {
        sourceDetail.textContent = "상세를 조회 중입니다.";
        const data = await requestJson(`${sourceUrl}/${sourceId}`);
        sourceDetail.innerHTML = "";
        appendText(sourceDetail, "h3", data.title);
        appendText(sourceDetail, "p", `${data.publicCode} · ${statusLabel(data.providerCode)} · ${statusLabel(data.reviewStatusCode)}`, "muted-copy");

        const actionRow = document.createElement("div");
        actionRow.className = "source-detail-actions";
        [
            ["CONDITION_INPUT_REQUIRED", "조건 입력 필요"],
            ["REVIEW_COMPLETED", "검수완료"],
            ["ARCHIVED", "보관"]
        ].forEach(([code, text]) => {
            const button = document.createElement("button");
            button.className = "secondary-action small-action";
            button.type = "button";
            button.textContent = text;
            button.addEventListener("click", () => updateSourceStatus(data.sourceId, code));
            actionRow.appendChild(button);
        });
        const convertButton = document.createElement("button");
        convertButton.className = "primary-action small-action";
        convertButton.type = "button";
        const pendingCandidates = (data.duplicateCandidates || []).filter((item) => item.decisionStatusCode === "PENDING");
        convertButton.textContent = pendingCandidates.length ? "후보 검수 후 DRAFT 생성" : "운영 공고 DRAFT 생성";
        convertButton.disabled = pendingCandidates.length > 0;
        convertButton.addEventListener("click", () => convertSource(data.sourceId));
        actionRow.appendChild(convertButton);
        sourceDetail.appendChild(actionRow);

        const meta = document.createElement("dl");
        meta.className = "source-meta";
        [
            ["기관", data.agencyName],
            ["신청 시작일", data.applicationStartDate],
            ["신청 마감일", data.applicationEndDate],
            ["원문 URL", data.sourceUrl],
            ["신청방법", data.applicationMethodText],
            ["문의처", data.inquiryText]
        ].forEach(([key, value]) => {
            appendText(meta, "dt", key);
            appendText(meta, "dd", value);
        });
        sourceDetail.appendChild(meta);

        renderDuplicateCandidates(data);
        renderSourceDuplicates(data);

        appendText(sourceDetail, "h4", "하이라이트");
        const highlights = document.createElement("div");
        highlights.className = "source-highlight-list";
        if (!data.highlights.length) {
            highlights.textContent = "하이라이트가 없습니다.";
        }
        data.highlights.forEach((item) => {
            const chip = document.createElement("div");
            chip.className = "source-highlight";
            appendText(chip, "strong", statusLabel(item.highlightTypeCode));
            appendText(chip, "span", item.matchedText);
            highlights.appendChild(chip);
        });
        sourceDetail.appendChild(highlights);

        appendText(sourceDetail, "h4", "원문 본문");
        appendText(sourceDetail, "pre", data.bodyText || "본문 정보가 없습니다.", "source-body");
    };

    const renderDuplicateCandidates = (data) => {
        appendText(sourceDetail, "h4", "중복/유사 공고 후보");
        const wrapper = document.createElement("div");
        wrapper.className = "source-duplicate-list";
        const candidates = data.duplicateCandidates || [];
        if (!candidates.length) {
            wrapper.textContent = "검출된 중복/유사 후보가 없습니다.";
            sourceDetail.appendChild(wrapper);
            return;
        }
        candidates.forEach((candidate) => {
            const card = document.createElement("div");
            card.className = "source-duplicate-card";
            appendText(card, "strong", `${candidate.announcementCode || "-"} · ${candidate.announcementTitle || "-"}`);
            appendText(card, "span", `${candidate.matchTypeLabel || statusLabel(candidate.matchTypeCode)} · ${candidate.decisionStatusLabel || statusLabel(candidate.decisionStatusCode)}`, "muted-copy");
            appendText(card, "p", candidate.similarityReason || "비교 기준 일부가 일치하거나 유사합니다.", "muted-copy");
            const flags = document.createElement("div");
            flags.className = "source-duplicate-flags";
            [
                ["사업명", candidate.titleMatched],
                ["주관기관", candidate.agencyMatched],
                ["공고번호", candidate.providerNoticeMatched],
                ["신청기간", candidate.periodMatched],
                ["원문 URL", candidate.sourceUrlMatched]
            ].forEach(([name, matched]) => {
                appendText(flags, "span", `${name}: ${matched ? "일치" : "미일치"}`);
            });
            card.appendChild(flags);
            if (candidate.decisionStatusCode === "PENDING") {
                const actions = document.createElement("div");
                actions.className = "source-detail-actions";
                [
                    ["CREATE_NEW", "신규 등록 선택", "secondary-action small-action"],
                    ["UPDATE_EXISTING", "기존 공고 업데이트", "primary-action small-action"],
                    ["IGNORE", "무시", "secondary-action small-action"]
                ].forEach(([actionCode, text, className]) => {
                    const button = document.createElement("button");
                    button.className = className;
                    button.type = "button";
                    button.textContent = text;
                    button.addEventListener("click", () => decideDuplicate(data.sourceId, candidate.candidateId, actionCode));
                    actions.appendChild(button);
                });
                card.appendChild(actions);
            }
            wrapper.appendChild(card);
        });
        sourceDetail.appendChild(wrapper);
    };

    const renderSourceDuplicates = (data) => {
        appendText(sourceDetail, "h4", "다른 수집처 중복/유사 원문");
        const wrapper = document.createElement("div");
        wrapper.className = "source-duplicate-list";
        const duplicates = data.sourceDuplicates || [];
        if (!duplicates.length) {
            wrapper.textContent = "다른 수집처에서 확인된 중복 원문이 없습니다.";
            sourceDetail.appendChild(wrapper);
            return;
        }
        duplicates.forEach((item) => {
            const card = document.createElement("div");
            card.className = "source-duplicate-card";
            appendText(card, "strong", `${item.candidatePublicCode} · ${item.candidateTitle}`);
            appendText(card, "span", `${statusLabel(item.candidateProviderCode)} · ${statusLabel(item.matchTypeCode)} · ${statusLabel(item.decisionStatusCode)}`, "muted-copy");
            appendText(card, "p", item.matchReason, "muted-copy");
            if (item.decisionStatusCode === "PENDING") {
                const actions = document.createElement("div");
                actions.className = "source-detail-actions";
                [["CREATE_NEW", "신규 등록"], ["UPDATE_EXISTING", "기존 공고 갱신"], ["IGNORE", "무시"]].forEach(([code, text]) => {
                    const button = document.createElement("button");
                    button.type = "button";
                    button.className = "secondary-action small-action";
                    button.textContent = text;
                    button.addEventListener("click", () => runAction(() => decideSourceDuplicate(data.sourceId, item.duplicateId, code)));
                    actions.appendChild(button);
                });
                card.appendChild(actions);
            }
            wrapper.appendChild(card);
        });
        sourceDetail.appendChild(wrapper);
    };

    const approveRequest = async (requestId, approvalStatusCode) => {
        await requestJson(`${requestUrl}/${requestId}/approval`, {
            method: "PATCH",
            body: JSON.stringify({approvalStatusCode, approvalNote: "관리자 화면 승인"})
        });
        await renderAll();
    };

    const runRequest = async (requestId) => {
        await requestJson(`${requestUrl}/${requestId}/runs`, {method: "POST", body: "{}"});
        await renderAll();
    };

    const updateSourceStatus = async (sourceId, reviewStatusCode) => {
        await requestJson(`${sourceUrl}/${sourceId}/review-status`, {
            method: "PATCH",
            body: JSON.stringify({reviewStatusCode, reason: "관리자 화면 상태 변경"})
        });
        await renderSources();
        await renderSourceDetail(sourceId);
    };

    const decideDuplicate = async (sourceId, candidateId, decisionActionCode) => {
        await requestJson(`${sourceUrl}/${sourceId}/duplicate-candidates/${candidateId}/decision`, {
            method: "PATCH",
            body: JSON.stringify({
                decisionActionCode,
                decisionNote: "관리자 화면 중복/유사 후보 검수"
            })
        });
        await renderSources();
        await renderSourceDetail(sourceId);
    };

    const decideSourceDuplicate = async (sourceId, duplicateId, decisionActionCode) => {
        await requestJson(`${sourceUrl}/${sourceId}/source-duplicates/${duplicateId}/decision`, {
            method: "PATCH",
            body: JSON.stringify({decisionActionCode, decisionNote: "관리자 화면 교차 수집처 중복 검수"})
        });
        await renderSources();
        await renderSourceDetail(sourceId);
    };

    const convertSource = async (sourceId) => {
        const result = await requestJson(`${sourceUrl}/${sourceId}/announcements`, {
            method: "POST",
            body: JSON.stringify({targetTypeCode: "BUSINESS", incomeJudgementCode: "VAT_TAX_BASE_ONLY"})
        });
        window.location.href = `/app/announcements/input?announcementCode=${encodeURIComponent(result.announcementCode || "")}`;
    };

    const renderAll = async () => {
        await Promise.all([
            renderRequests(),
            renderRuns(),
            renderSources(),
            renderLocalSummary(),
            renderLocalSources(),
            renderSchedules()
        ]);
    };

    localSourceForm.addEventListener("submit", (event) => {
        event.preventDefault();
        runAction(async () => {
            const formData = new FormData(localSourceForm);
            const sourceId = formData.get("sourceId");
            const payload = Object.fromEntries(formData.entries());
            delete payload.sourceId;
            await requestJson(sourceId ? `${localSourceUrl}/${sourceId}` : localSourceUrl, {
                method: sourceId ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            resetLocalSourceForm();
            await Promise.all([renderLocalSources(), renderLocalSummary()]);
        });
    });

    localSourceFilterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        localSourcePage = 1;
        runAction(renderLocalSources);
    });
    localFormReset.addEventListener("click", resetLocalSourceForm);
    localPagePrev.addEventListener("click", () => {
        if (localSourcePage > 1) {
            localSourcePage -= 1;
            runAction(renderLocalSources);
        }
    });
    localPageNext.addEventListener("click", () => {
        if (localSourcePage < localSourceTotalPages) {
            localSourcePage += 1;
            runAction(renderLocalSources);
        }
    });

    scheduleForm.addEventListener("submit", (event) => {
        event.preventDefault();
        runAction(async () => {
            const payload = Object.fromEntries(new FormData(scheduleForm).entries());
            payload.maxCount = payload.maxCount ? Number(payload.maxCount) : null;
            await requestJson(scheduleUrl, {method: "POST", body: JSON.stringify(payload)});
            scheduleForm.reset();
            scheduleForm.elements.namedItem("timezone").value = "Asia/Seoul";
            scheduleForm.elements.namedItem("cronExpression").value = "0 0 8 * * *";
            await renderSchedules();
        });
    });

    requestForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const formData = new FormData(requestForm);
        const payload = Object.fromEntries(formData.entries());
        payload.requestTypeCode = "MANUAL";
        payload.maxCount = payload.maxCount ? Number(payload.maxCount) : null;
        await requestJson(requestUrl, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        requestForm.reset();
        await renderAll();
    });

    refreshButton.addEventListener("click", renderAll);
    sourceFilterForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        await renderSources();
    });

    Promise.all([renderLocalParsers(), renderAll()]).catch((error) => {
        sourceDetail.textContent = error.message;
    });
})();
