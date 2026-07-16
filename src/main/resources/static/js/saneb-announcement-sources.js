(() => {
    const page = document.querySelector("[data-announcement-source-page]");
    if (!page) {
        return;
    }

    const requestUrl = page.dataset.requestUrl;
    const runUrl = page.dataset.runUrl;
    const localSourceUrl = page.dataset.localSourceUrl;
    const localParserUrl = page.dataset.localParserUrl;
    const localSummaryUrl = page.dataset.localSummaryUrl;
    const scheduleUrl = page.dataset.scheduleUrl;
    const scheduleTimezone = "Asia/Seoul";
    const roleCode = page.dataset.roleCode;
    const canApprove = roleCode === "APPROVER" || roleCode === "ADMIN";
    const requestForm = page.querySelector("[data-source-request-form]");
    const refreshButton = page.querySelector("[data-source-refresh]");
    const requestList = page.querySelector("[data-source-request-list]");
    const runList = page.querySelector("[data-source-run-list]");
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

    const scheduleTimeToCron = (executionTime) => {
        const match = /^(\d{2}):(\d{2})$/.exec(executionTime || "");
        if (!match) {
            throw new Error("실행 시각을 선택해 주세요.");
        }
        const hour = Number(match[1]);
        const minute = Number(match[2]);
        if (hour > 23 || minute > 59) {
            throw new Error("실행 시각을 올바르게 선택해 주세요.");
        }
        return `0 ${minute} ${hour} * * *`;
    };

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

    const renderAll = async () => {
        await Promise.all([
            renderRequests(),
            renderRuns(),
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
            payload.cronExpression = scheduleTimeToCron(payload.executionTime);
            payload.timezone = scheduleTimezone;
            delete payload.executionTime;
            payload.maxCount = payload.maxCount ? Number(payload.maxCount) : null;
            await requestJson(scheduleUrl, {method: "POST", body: JSON.stringify(payload)});
            scheduleForm.reset();
            scheduleForm.elements.namedItem("executionTime").value = "08:00";
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

    Promise.all([renderLocalParsers(), renderAll()]).catch((error) => {
        localSummary.textContent = error.message || "수집 관리 정보를 불러오지 못했습니다.";
    });
})();
