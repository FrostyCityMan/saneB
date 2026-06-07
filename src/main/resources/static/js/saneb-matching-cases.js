(() => {
    const app = document.querySelector("[data-matching-app]");
    if (!app) {
        return;
    }

    const baseUrl = app.dataset.baseUrl;
    const announcementLookupUrl = app.dataset.announcementLookupUrl;
    const memberLookupUrl = app.dataset.memberLookupUrl;
    const createForm = app.querySelector("[data-matching-create-form]");
    const searchForm = app.querySelector("[data-matching-search-form]");
    const list = app.querySelector("[data-matching-list]");
    const message = app.querySelector("[data-matching-message]");

    const statusLabels = {
        MATCHED: "매칭",
        NOT_MATCHED: "미매칭",
        REVIEW_REQUIRED: "검토 필요",
        BLOCKED: "차단",
        PROGRESSED: "진행 전환"
    };

    const selectErrorMessage = (payload, fallback) => {
        const fieldErrors = payload && payload.data && Array.isArray(payload.data.fieldErrors)
                ? payload.data.fieldErrors
                : [];
        if (fieldErrors.length > 0 && fieldErrors[0].message) {
            return fieldErrors[0].message;
        }
        if (payload && typeof payload.message === "string" && payload.message.trim() !== "") {
            return payload.message;
        }
        return fallback;
    };

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.classList.toggle("is-success", status === "success");
        message.classList.toggle("is-error", status === "error");
    };

    const setBusy = (button, busy, busyText) => {
        if (!button) {
            return;
        }
        if (!button.dataset.defaultText) {
            button.dataset.defaultText = button.textContent;
        }
        button.disabled = busy;
        button.textContent = busy ? busyText : button.dataset.defaultText;
    };

    const valueOf = (root, name) => {
        const field = root.querySelector(`[name='${name}']`);
        return field ? String(field.value || "").trim() : "";
    };

    const nullIfBlank = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : text;
    };

    const requestJson = async (url, options = {}) => {
        const response = await fetch(url, {
            credentials: "same-origin",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json",
                ...(options.headers || {})
            },
            ...options
        });
        const payload = await response.json().catch(() => null);
        if (!response.ok || !payload || payload.success !== true) {
            throw new Error(selectErrorMessage(payload, "요청 처리에 실패했습니다."));
        }
        return payload.data;
    };

    const validateUuidText = (value, label) => {
        if (!value) {
            throw new Error(`${label}를 입력하세요.`);
        }
        return value;
    };

    const renderEmpty = (text) => {
        list.replaceChildren();
        const empty = document.createElement("p");
        empty.className = "empty-state";
        empty.textContent = text;
        list.append(empty);
    };

    const renderCase = (item) => {
        const card = document.createElement("article");
        card.className = "matching-case-card";
        card.dataset.matchingCaseId = item.matchingCaseId;

        const head = document.createElement("div");
        head.className = "matching-case-head";
        const title = document.createElement("strong");
        title.textContent = item.matchingCaseId || "매칭 ID 없음";
        const status = document.createElement("span");
        status.className = "soft-status";
        status.textContent = statusLabels[item.statusCode] || item.statusCode || "상태 없음";
        head.append(title, status);

        const meta = document.createElement("dl");
        meta.className = "matching-case-meta";
        [
            ["공고 ID", item.announcementId],
            ["회원 ID", item.memberUserId],
            ["차단 사유", item.blockedReasonCode || "없음"]
        ].forEach(([label, value]) => {
            const group = document.createElement("div");
            const dt = document.createElement("dt");
            dt.textContent = label;
            const dd = document.createElement("dd");
            dd.textContent = value || "-";
            group.append(dt, dd);
            meta.append(group);
        });

        const form = document.createElement("form");
        form.className = "matching-status-form";
        form.dataset.matchingStatusForm = "true";
        const select = document.createElement("select");
        select.name = "statusCode";
        Object.entries(statusLabels).forEach(([code, label]) => {
            const option = document.createElement("option");
            option.value = code;
            option.textContent = label;
            option.selected = code === item.statusCode;
            select.append(option);
        });
        const reason = document.createElement("input");
        reason.name = "blockedReasonCode";
        reason.type = "text";
        reason.maxLength = 80;
        reason.placeholder = "차단 사유(선택)";
        reason.value = item.blockedReasonCode || "";
        const button = document.createElement("button");
        button.className = "secondary-action";
        button.type = "submit";
        button.textContent = "상태 저장";
        form.append(select, reason, button);

        const resultButton = document.createElement("button");
        resultButton.className = "secondary-action";
        resultButton.type = "button";
        resultButton.dataset.matchingResultsButton = "true";
        resultButton.textContent = "결과 보기";

        const results = document.createElement("div");
        results.className = "matching-results";
        results.hidden = true;
        results.dataset.matchingResults = "true";

        card.append(head, meta, form, resultButton, results);
        return card;
    };

    const renderList = (items) => {
        if (!list) {
            return;
        }
        if (!items || items.length === 0) {
            renderEmpty("조회된 매칭 케이스가 없습니다.");
            return;
        }
        list.replaceChildren();
        items.forEach((item) => list.append(renderCase(item)));
    };

    const buildSearchParams = () => {
        const params = new URLSearchParams({ page: "1", size: "20" });
        ["announcementId", "memberUserId", "statusCode"].forEach((name) => {
            const value = nullIfBlank(valueOf(searchForm, name));
            if (value) {
                params.set(name, value);
            }
        });
        return params;
    };

    const loadMatchingList = async () => {
        const data = await requestJson(`${baseUrl}?${buildSearchParams().toString()}`, { method: "GET" });
        renderList(data ? data.items : []);
        setMessage("매칭 목록을 조회했습니다.", "success");
    };

    const lookupModal = (type) => app.querySelector(`[data-lookup-modal='${type}']`);
    const lookupResults = (type) => app.querySelector(`[data-lookup-results='${type}']`);

    const openLookupModal = async (type) => {
        const modal = lookupModal(type);
        if (!modal) {
            return;
        }
        modal.hidden = false;
        modal.querySelector("input[name='keyword']")?.focus();
        await loadLookupResults(type);
    };

    const closeLookupModal = (modal) => {
        if (modal) {
            modal.hidden = true;
        }
    };

    const renderLookupEmpty = (type, text) => {
        const results = lookupResults(type);
        if (!results) {
            return;
        }
        results.replaceChildren();
        const empty = document.createElement("p");
        empty.className = "empty-state";
        empty.textContent = text;
        results.append(empty);
    };

    const renderLookupButton = (type, title, meta, value) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "lookup-result-item";
        button.dataset.lookupSelect = type;
        button.dataset.lookupValue = value;
        const strong = document.createElement("strong");
        strong.textContent = title;
        const small = document.createElement("small");
        small.textContent = meta;
        button.append(strong, small);
        return button;
    };

    const loadAnnouncementLookupResults = async () => {
        const modal = lookupModal("announcement");
        const params = new URLSearchParams({
            page: "1",
            size: "10",
            approvalStatusCode: "APPROVED",
            manualStatusCode: "NORMAL"
        });
        const keyword = nullIfBlank(valueOf(modal, "keyword"));
        if (keyword) {
            params.set("keyword", keyword);
        }
        const data = await requestJson(`${announcementLookupUrl}?${params.toString()}`, { method: "GET" });
        const items = data ? data.items : [];
        if (items.length === 0) {
            renderLookupEmpty("announcement", "조회된 승인 공고가 없습니다.");
            return;
        }
        const results = lookupResults("announcement");
        results.replaceChildren();
        items.forEach((item) => {
            const dateText = item.applicationStartDate && item.applicationEndDate
                    ? `${item.applicationStartDate} ~ ${item.applicationEndDate}`
                    : "신청 기간 미입력";
            results.append(renderLookupButton(
                    "announcement",
                    item.title || "제목 없음",
                    `${item.agencyName || "기관 미입력"} · ${dateText} · ${item.announcementId}`,
                    item.announcementId
            ));
        });
    };

    const loadMemberLookupResults = async () => {
        const modal = lookupModal("member");
        const params = new URLSearchParams({ page: "1", size: "10" });
        const keyword = nullIfBlank(valueOf(modal, "keyword"));
        if (keyword) {
            params.set("keyword", keyword);
        }
        const data = await requestJson(`${memberLookupUrl}?${params.toString()}`, { method: "GET" });
        const items = data ? data.items : [];
        if (items.length === 0) {
            renderLookupEmpty("member", "조회된 회원이 없습니다.");
            return;
        }
        const results = lookupResults("member");
        results.replaceChildren();
        items.forEach((item) => {
            results.append(renderLookupButton(
                    "member",
                    item.name || item.loginId || "이름 없음",
                    `${item.loginId || "아이디 없음"} · ${item.statusCode || "상태 없음"} · ${item.userId}`,
                    item.userId
            ));
        });
    };

    const loadLookupResults = async (type) => {
        try {
            if (type === "announcement") {
                await loadAnnouncementLookupResults();
                return;
            }
            await loadMemberLookupResults();
        } catch (error) {
            renderLookupEmpty(type, "조회에 실패했습니다.");
            setMessage(error.message, "error");
        }
    };

    createForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = app.querySelector("[data-matching-create-submit]");
        try {
            setBusy(button, true, "생성 중");
            const body = {
                announcementId: validateUuidText(valueOf(createForm, "announcementId"), "공고 ID"),
                memberUserId: validateUuidText(valueOf(createForm, "memberUserId"), "회원 ID")
            };
            await requestJson(baseUrl, {
                method: "POST",
                body: JSON.stringify(body)
            });
            createForm.reset();
            await loadMatchingList();
            setMessage("매칭 케이스가 생성되었습니다.", "success");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });

    searchForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            await loadMatchingList();
        } catch (error) {
            renderEmpty("매칭 목록을 불러오지 못했습니다.");
            setMessage(error.message, "error");
        }
    });

    app.querySelectorAll("[data-lookup-search-form]").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            await loadLookupResults(form.dataset.lookupSearchForm);
        });
    });

    app.addEventListener("submit", async (event) => {
        const form = event.target.closest("[data-matching-status-form]");
        if (!form) {
            return;
        }
        event.preventDefault();
        const card = form.closest("[data-matching-case-id]");
        const matchingCaseId = card ? card.dataset.matchingCaseId : "";
        const button = form.querySelector("button[type='submit']");
        if (!matchingCaseId) {
            setMessage("매칭 케이스 ID를 확인할 수 없습니다.", "error");
            return;
        }
        try {
            setBusy(button, true, "저장 중");
            await requestJson(`${baseUrl}/${encodeURIComponent(matchingCaseId)}/status`, {
                method: "PATCH",
                body: JSON.stringify({
                    statusCode: valueOf(form, "statusCode"),
                    blockedReasonCode: nullIfBlank(valueOf(form, "blockedReasonCode"))
                })
            });
            await loadMatchingList();
            setMessage("매칭 상태가 저장되었습니다.", "success");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });

    app.addEventListener("click", async (event) => {
        const openButton = event.target.closest("[data-lookup-open]");
        if (openButton) {
            await openLookupModal(openButton.dataset.lookupOpen);
            return;
        }

        const closeButton = event.target.closest("[data-lookup-close]");
        if (closeButton) {
            closeLookupModal(closeButton.closest("[data-lookup-modal]"));
            return;
        }

        const lookupSelect = event.target.closest("[data-lookup-select]");
        if (lookupSelect) {
            const type = lookupSelect.dataset.lookupSelect;
            const value = lookupSelect.dataset.lookupValue || "";
            const fieldName = type === "announcement" ? "announcementId" : "memberUserId";
            const field = createForm.querySelector(`[name='${fieldName}']`);
            if (field) {
                field.value = value;
            }
            closeLookupModal(lookupSelect.closest("[data-lookup-modal]"));
            setMessage(type === "announcement" ? "공고 ID를 선택했습니다." : "회원 ID를 선택했습니다.", "success");
            return;
        }

        const button = event.target.closest("[data-matching-results-button]");
        if (!button) {
            return;
        }
        const card = button.closest("[data-matching-case-id]");
        const matchingCaseId = card ? card.dataset.matchingCaseId : "";
        const results = card ? card.querySelector("[data-matching-results]") : null;
        if (!matchingCaseId || !results) {
            return;
        }
        if (!results.hidden) {
            results.hidden = true;
            return;
        }
        try {
            setBusy(button, true, "조회 중");
            const data = await requestJson(`${baseUrl}/${encodeURIComponent(matchingCaseId)}/results`, { method: "GET" });
            results.replaceChildren();
            if (!data || data.length === 0) {
                const empty = document.createElement("p");
                empty.className = "empty-state";
                empty.textContent = "조건별 결과가 없습니다.";
                results.append(empty);
            } else {
                data.forEach((item) => {
                    const row = document.createElement("p");
                    row.textContent = `${item.conditionScopeCode || "-"} / ${item.conditionKey || "-"} / ${item.resultCode || "-"}`;
                    results.append(row);
                });
            }
            results.hidden = false;
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });

    loadMatchingList().catch((error) => {
        renderEmpty("매칭 목록을 불러오지 못했습니다.");
        setMessage(error.message, "error");
    });
})();
