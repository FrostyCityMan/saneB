(() => {
    const app = document.querySelector("[data-matching-app]");
    if (!app) {
        return;
    }

    const baseUrl = app.dataset.baseUrl;
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
            ["검증 ID", item.verificationId],
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
        reason.placeholder = "차단 사유 코드";
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
        ["announcementId", "memberUserId", "verificationId", "statusCode"].forEach((name) => {
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

    createForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = app.querySelector("[data-matching-create-submit]");
        try {
            setBusy(button, true, "생성 중");
            const body = {
                announcementId: validateUuidText(valueOf(createForm, "announcementId"), "공고 ID"),
                memberUserId: validateUuidText(valueOf(createForm, "memberUserId"), "회원 ID"),
                verificationId: validateUuidText(valueOf(createForm, "verificationId"), "검증 ID")
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
