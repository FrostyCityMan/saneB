(() => {
    const app = document.querySelector("[data-announcement-app]");
    if (!app) {
        return;
    }

    const targetLabels = {
        BUSINESS: "사업자 지원",
        PERSONAL: "개인 지원",
        SPOUSE: "배우자 지원",
        CHILD: "자녀 지원",
        PARENT: "부모 지원"
    };
    const targetSpecificTitles = {
        PERSONAL: "개인 조건",
        SPOUSE: "배우자 조건",
        CHILD: "자녀 조건",
        PARENT: "부모 조건"
    };

    const baseUrl = app.dataset.baseUrl;
    const listUrl = app.dataset.listUrl;
    const listContainer = app.querySelector("[data-announcement-list]");
    const message = app.querySelector("[data-announcement-message]");
    const currentIdLabel = app.querySelector("[data-current-announcement-id]");
    const basicForm = app.querySelector("[data-announcement-basic-form]");
    const conditionsForm = app.querySelector("[data-announcement-conditions-form]");
    const stepsForm = app.querySelector("[data-announcement-steps-form]");
    const statusForm = app.querySelector("[data-announcement-status-form]");
    const searchForm = app.querySelector("[data-announcement-search-form]");
    const businessPanel = app.querySelector("[data-business-panel]");
    const nonBusinessPanel = app.querySelector("[data-non-business-panel]");
    const activeTargetLabel = app.querySelector("[data-active-target-label]");
    const targetSpecificTitle = app.querySelector("[data-target-specific-title]");
    let currentAnnouncementId = "";

    const selectErrorMessage = (payload, fallback) => {
        if (payload && typeof payload.message === "string" && payload.message.trim() !== "") {
            return payload.message;
        }
        const fieldErrors = payload && payload.data && Array.isArray(payload.data.fieldErrors)
                ? payload.data.fieldErrors
                : [];
        if (fieldErrors.length > 0 && fieldErrors[0].message) {
            return fieldErrors[0].message;
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

    const valueOf = (root, selector) => {
        const field = root.querySelector(selector);
        return field ? String(field.value || "").trim() : "";
    };

    const numberOf = (root, selector) => {
        const value = valueOf(root, selector);
        return value === "" ? null : Number(value);
    };

    const nullIfBlank = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : text;
    };

    const selectedTargetCode = () => {
        const checked = app.querySelector("input[name='targetTypeCode']:checked");
        return checked ? checked.value : "BUSINESS";
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

    const togglePanel = (panel, active) => {
        if (!panel) {
            return;
        }
        panel.classList.toggle("is-active", active);
        panel.hidden = !active;
        panel.querySelectorAll("input, select, textarea").forEach((field) => {
            field.disabled = !active;
        });
    };

    const updateTargetUi = () => {
        const targetCode = selectedTargetCode();
        app.querySelectorAll("[data-target-card]").forEach((card) => {
            const input = card.querySelector("input[name='targetTypeCode']");
            const active = input && input.value === targetCode;
            card.classList.toggle("is-active", active);
            card.classList.toggle("is-muted", !active);
        });

        const businessActive = targetCode === "BUSINESS";
        togglePanel(businessPanel, businessActive);
        togglePanel(nonBusinessPanel, !businessActive);

        if (activeTargetLabel) {
            activeTargetLabel.textContent = targetLabels[targetCode] || targetCode;
        }
        if (targetSpecificTitle) {
            targetSpecificTitle.textContent = targetSpecificTitles[targetCode] || "개인/가족 조건";
        }
    };

    const updateCurrentAnnouncement = (announcementId) => {
        currentAnnouncementId = announcementId || "";
        if (currentIdLabel) {
            currentIdLabel.textContent = currentAnnouncementId || "신규 입력";
        }
    };

    const buildOptions = () => {
        const options = [];
        app.querySelectorAll("[data-option-group]:checked").forEach((field) => {
            options.push({
                optionGroupCode: field.dataset.optionGroup,
                optionCode: field.value
            });
        });
        app.querySelectorAll("[data-single-option-group]").forEach((field) => {
            if (field.value) {
                options.push({
                    optionGroupCode: field.dataset.singleOptionGroup,
                    optionCode: field.value
                });
            }
        });
        return options;
    };

    const buildSaveRequest = () => {
        const title = valueOf(basicForm, "[name='title']");
        const agencyName = valueOf(basicForm, "[name='agencyName']");
        const incomeJudgementCode = valueOf(basicForm, "[name='incomeJudgementCode']");

        if (!title || !agencyName || !incomeJudgementCode) {
            throw new Error("공고명, 기관명, 소득/매출 판단 기준을 입력해 주세요.");
        }

        return {
            targetTypeCode: selectedTargetCode(),
            title,
            agencyName,
            summary: nullIfBlank(valueOf(basicForm, "[name='summary']")),
            applicationStartDate: nullIfBlank(valueOf(basicForm, "[name='applicationStartDate']")),
            applicationEndDate: nullIfBlank(valueOf(basicForm, "[name='applicationEndDate']")),
            incomeJudgementCode,
            minAmount: numberOf(basicForm, "[name='minAmount']"),
            maxAmount: numberOf(basicForm, "[name='maxAmount']"),
            options: buildOptions()
        };
    };

    const buildNumericCondition = (scope, key, comparator, valueNumber, minNumber, maxNumber, unitCode) => {
        if (!key) {
            return null;
        }
        if (comparator === "BETWEEN") {
            if (minNumber === null || maxNumber === null) {
                throw new Error("구간 조건은 최소값과 최대값을 모두 입력해 주세요.");
            }
        } else if (valueNumber === null) {
            throw new Error("수치 조건의 값을 입력해 주세요.");
        }

        return {
            conditionScopeCode: scope,
            conditionKey: key,
            comparatorCode: comparator,
            valueNumber,
            minNumber,
            maxNumber,
            unitCode: nullIfBlank(unitCode)
        };
    };

    const buildConditionsRequest = () => {
        const targetCode = selectedTargetCode();
        const industryConditions = [];
        const numericConditions = [];
        const optionConditions = [];

        if (targetCode === "BUSINESS") {
            const includeCode = valueOf(conditionsForm, "[name='industryInclude']");
            const excludeCode = valueOf(conditionsForm, "[name='industryExclude']");
            if (includeCode) {
                industryConditions.push({ conditionTypeCode: "INCLUDE", ksicCode: includeCode });
            }
            if (excludeCode) {
                industryConditions.push({ conditionTypeCode: "EXCLUDE", ksicCode: excludeCode });
            }

            const businessType = valueOf(conditionsForm, "[name='businessType']");
            if (businessType) {
                optionConditions.push({
                    conditionScopeCode: "BUSINESS",
                    conditionKey: "BUSINESS_TYPE",
                    optionCode: businessType,
                    optionText: null
                });
            }
            const businessStage = valueOf(conditionsForm, "[name='businessStage']");
            if (businessStage) {
                optionConditions.push({
                    conditionScopeCode: "BUSINESS",
                    conditionKey: "BUSINESS_STAGE",
                    optionCode: businessStage,
                    optionText: null
                });
            }

            const numeric = buildNumericCondition(
                    "BUSINESS",
                    valueOf(conditionsForm, "[name='businessNumericKey']"),
                    valueOf(conditionsForm, "[name='businessComparator']") || "LTE",
                    numberOf(conditionsForm, "[name='businessValueNumber']"),
                    numberOf(conditionsForm, "[name='businessMinNumber']"),
                    numberOf(conditionsForm, "[name='businessMaxNumber']"),
                    valueOf(conditionsForm, "[name='businessUnitCode']")
            );
            if (numeric) {
                numericConditions.push(numeric);
            }
        } else {
            const numeric = buildNumericCondition(
                    targetCode,
                    valueOf(conditionsForm, "[name='targetNumericKey']"),
                    valueOf(conditionsForm, "[name='targetComparator']") || "LTE",
                    numberOf(conditionsForm, "[name='targetValueNumber']"),
                    numberOf(conditionsForm, "[name='targetMinNumber']"),
                    numberOf(conditionsForm, "[name='targetMaxNumber']"),
                    valueOf(conditionsForm, "[name='targetUnitCode']")
            );
            if (numeric) {
                numericConditions.push(numeric);
            }

            const optionKey = valueOf(conditionsForm, "[name='targetOptionKey']");
            const optionCode = valueOf(conditionsForm, "[name='targetOptionCode']");
            if (optionKey && optionCode) {
                optionConditions.push({
                    conditionScopeCode: targetCode,
                    conditionKey: optionKey,
                    optionCode,
                    optionText: null
                });
            }
        }

        const documentRequirements = [];
        app.querySelectorAll("[data-document-code]:checked").forEach((field, index) => {
            documentRequirements.push({
                documentTypeCode: field.dataset.documentCode,
                required: true,
                sortOrder: index + 1
            });
        });

        return {
            industryConditions,
            numericConditions,
            optionConditions,
            documentRequirements
        };
    };

    const buildStepsRequest = () => {
        const steps = [];
        app.querySelectorAll("[data-step-row]").forEach((row) => {
            const stepName = valueOf(row, "[name='stepName']");
            if (!stepName) {
                return;
            }

            const buttonLabel = valueOf(row, "[name='buttonLabel']");
            const buttonCode = valueOf(row, "[name='buttonCode']") || `STEP_${steps.length + 1}_BUTTON`;
            const documentTypeCode = valueOf(row, "[name='stepDocumentTypeCode']");
            const buttons = buttonLabel ? [{
                buttonCode,
                buttonLabel,
                buttonActionCode: "MOVE_NEXT",
                nextStepId: null,
                sortOrder: 1
            }] : [];
            const documents = documentTypeCode ? [{
                documentTypeCode,
                required: true,
                sortOrder: 1
            }] : [];

            steps.push({
                stepOrder: steps.length + 1,
                stepName,
                guideMessage: nullIfBlank(valueOf(row, "[name='guideMessage']")),
                actionGuide: nullIfBlank(valueOf(row, "[name='actionGuide']")),
                completionConditionCode: valueOf(row, "[name='completionConditionCode']") || "BUTTON_CLICK",
                nextConditionCode: nullIfBlank(valueOf(row, "[name='nextConditionCode']")),
                active: Boolean(row.querySelector("[name='stepActive']")?.checked),
                buttons,
                documents
            });
        });

        if (steps.length === 0) {
            throw new Error("진행 단계는 1개 이상 입력해 주세요.");
        }

        return { steps };
    };

    const renderListItem = (item) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "announcement-list-item";
        button.dataset.announcementId = item.announcementId;

        const meta = document.createElement("span");
        meta.className = "list-meta";
        meta.textContent = `${targetLabels[item.targetTypeCode] || item.targetTypeCode} · ${item.manualStatusCode || "NORMAL"}`;

        const title = document.createElement("strong");
        title.textContent = item.title || "제목 없음";

        const sub = document.createElement("small");
        const dateText = item.applicationStartDate && item.applicationEndDate
                ? `${item.applicationStartDate} ~ ${item.applicationEndDate}`
                : "신청 기간 미입력";
        sub.textContent = `${item.agencyName || "기관 미입력"} · ${dateText}`;

        button.append(meta, title, sub);
        return button;
    };

    const renderAnnouncementList = (items) => {
        if (!listContainer) {
            return;
        }
        listContainer.replaceChildren();
        if (!items || items.length === 0) {
            const empty = document.createElement("p");
            empty.className = "empty-state";
            empty.textContent = "조회된 공고가 없습니다.";
            listContainer.append(empty);
            return;
        }
        items.forEach((item) => {
            listContainer.append(renderListItem(item));
        });
    };

    const loadAnnouncementList = async () => {
        const params = new URLSearchParams({ page: "1", size: "8" });
        if (searchForm) {
            const formData = new FormData(searchForm);
            const keyword = nullIfBlank(formData.get("keyword"));
            const targetTypeCode = nullIfBlank(formData.get("targetTypeCode"));
            if (keyword) {
                params.set("keyword", keyword);
            }
            if (targetTypeCode) {
                params.set("targetTypeCode", targetTypeCode);
            }
        }

        try {
            const data = await requestJson(`${listUrl}?${params.toString()}`, { method: "GET" });
            renderAnnouncementList(data ? data.items : []);
        } catch (error) {
            renderAnnouncementList([]);
            setMessage(error.message, "error");
        }
    };

    const applyOptions = (options) => {
        app.querySelectorAll("[data-option-group]").forEach((field) => {
            field.checked = false;
        });
        app.querySelectorAll("[data-single-option-group]").forEach((field) => {
            field.value = "";
        });
        (options || []).forEach((option) => {
            const multi = app.querySelector(`[data-option-group='${option.optionGroupCode}'][value='${option.optionCode}']`);
            if (multi) {
                multi.checked = true;
                return;
            }
            const single = app.querySelector(`[data-single-option-group='${option.optionGroupCode}']`);
            if (single) {
                single.value = option.optionCode;
            }
        });
    };

    const applyConditions = (conditions, targetCode) => {
        conditionsForm.reset();
        app.querySelectorAll("[data-document-code]").forEach((field) => {
            field.checked = false;
        });

        (conditions?.industryConditions || []).forEach((condition) => {
            if (condition.conditionTypeCode === "INCLUDE") {
                conditionsForm.querySelector("[name='industryInclude']").value = condition.ksicCode || "";
            }
            if (condition.conditionTypeCode === "EXCLUDE") {
                conditionsForm.querySelector("[name='industryExclude']").value = condition.ksicCode || "";
            }
        });

        (conditions?.optionConditions || []).forEach((condition) => {
            if (condition.conditionScopeCode === "BUSINESS" && condition.conditionKey === "BUSINESS_TYPE") {
                conditionsForm.querySelector("[name='businessType']").value = condition.optionCode || "";
            } else if (condition.conditionScopeCode === "BUSINESS" && condition.conditionKey === "BUSINESS_STAGE") {
                conditionsForm.querySelector("[name='businessStage']").value = condition.optionCode || "";
            } else if (condition.conditionScopeCode === targetCode) {
                conditionsForm.querySelector("[name='targetOptionKey']").value = condition.conditionKey || "";
                conditionsForm.querySelector("[name='targetOptionCode']").value = condition.optionCode || "";
            }
        });

        const numeric = (conditions?.numericConditions || []).find((condition) => condition.conditionScopeCode === targetCode)
                || (conditions?.numericConditions || [])[0];
        if (numeric) {
            const prefix = numeric.conditionScopeCode === "BUSINESS" ? "business" : "target";
            conditionsForm.querySelector(`[name='${prefix}NumericKey']`).value = numeric.conditionKey || "";
            conditionsForm.querySelector(`[name='${prefix}Comparator']`).value = numeric.comparatorCode || "LTE";
            conditionsForm.querySelector(`[name='${prefix}ValueNumber']`).value = numeric.valueNumber ?? "";
            conditionsForm.querySelector(`[name='${prefix}MinNumber']`).value = numeric.minNumber ?? "";
            conditionsForm.querySelector(`[name='${prefix}MaxNumber']`).value = numeric.maxNumber ?? "";
            conditionsForm.querySelector(`[name='${prefix}UnitCode']`).value = numeric.unitCode || "";
        }

        (conditions?.documentRequirements || []).forEach((document) => {
            const field = app.querySelector(`[data-document-code='${document.documentTypeCode}']`);
            if (field) {
                field.checked = document.required !== false;
            }
        });
        updateTargetUi();
    };

    const applySteps = (steps) => {
        stepsForm.reset();
        const rows = Array.from(app.querySelectorAll("[data-step-row]"));
        (steps || []).slice(0, rows.length).forEach((step, index) => {
            const row = rows[index];
            row.querySelector("[name='stepName']").value = step.stepName || "";
            row.querySelector("[name='completionConditionCode']").value = step.completionConditionCode || "BUTTON_CLICK";
            row.querySelector("[name='guideMessage']").value = step.guideMessage || "";
            row.querySelector("[name='actionGuide']").value = step.actionGuide || "";
            row.querySelector("[name='nextConditionCode']").value = step.nextConditionCode || "";
            row.querySelector("[name='stepActive']").checked = step.active !== false;

            const firstButton = (step.buttons || [])[0];
            row.querySelector("[name='buttonLabel']").value = firstButton ? firstButton.buttonLabel || "" : "";
            row.querySelector("[name='buttonCode']").value = firstButton ? firstButton.buttonCode || "" : "";

            const firstDocument = (step.documents || [])[0];
            row.querySelector("[name='stepDocumentTypeCode']").value = firstDocument ? firstDocument.documentTypeCode || "" : "";
        });
    };

    const populateDetails = (details) => {
        updateCurrentAnnouncement(details.announcementId);
        const targetField = app.querySelector(`input[name='targetTypeCode'][value='${details.targetTypeCode || "BUSINESS"}']`);
        if (targetField) {
            targetField.checked = true;
        }
        updateTargetUi();

        basicForm.querySelector("[name='title']").value = details.title || "";
        basicForm.querySelector("[name='agencyName']").value = details.agencyName || "";
        basicForm.querySelector("[name='summary']").value = details.summary || "";
        basicForm.querySelector("[name='applicationStartDate']").value = details.applicationStartDate || "";
        basicForm.querySelector("[name='applicationEndDate']").value = details.applicationEndDate || "";
        basicForm.querySelector("[name='incomeJudgementCode']").value = details.incomeJudgementCode || "VAT_TAX_BASE_ONLY";
        basicForm.querySelector("[name='minAmount']").value = details.minAmount ?? "";
        basicForm.querySelector("[name='maxAmount']").value = details.maxAmount ?? "";
        applyOptions(details.options);
        applyConditions(details.conditions, details.targetTypeCode || "BUSINESS");
        applySteps(details.steps);
        statusForm.querySelector("[name='manualStatusCode']").value = details.manualStatusCode || "NORMAL";
        statusForm.querySelector("[name='reason']").value = "";
    };

    const loadDetails = async (announcementId) => {
        setMessage("공고 상세를 불러오는 중입니다.");
        const details = await requestJson(`${baseUrl}/${encodeURIComponent(announcementId)}`, { method: "GET" });
        populateDetails(details);
        setMessage("공고 상세를 입력 폼에 반영했습니다.", "success");
    };

    const resetForNewInput = () => {
        updateCurrentAnnouncement("");
        basicForm.reset();
        conditionsForm.reset();
        stepsForm.reset();
        statusForm.reset();
        const businessTarget = app.querySelector("input[name='targetTypeCode'][value='BUSINESS']");
        if (businessTarget) {
            businessTarget.checked = true;
        }
        const businessDocument = app.querySelector("[data-document-code='BUSINESS_REGISTRATION']");
        if (businessDocument) {
            businessDocument.checked = true;
        }
        updateTargetUi();
        setMessage("신규 공고 입력 상태입니다.");
    };

    app.addEventListener("change", (event) => {
        if (event.target.matches("input[name='targetTypeCode']")) {
            updateTargetUi();
        }
    });

    app.addEventListener("click", async (event) => {
        const item = event.target.closest("[data-announcement-id]");
        if (item && item.dataset.announcementId) {
            try {
                await loadDetails(item.dataset.announcementId);
            } catch (error) {
                setMessage(error.message, "error");
            }
            return;
        }

        if (event.target.matches("[data-reset-form]")) {
            resetForNewInput();
        }
    });

    basicForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = app.querySelector("[data-basic-submit]");
        try {
            setBusy(button, true, "저장 중");
            const body = buildSaveRequest();
            const url = currentAnnouncementId ? `${baseUrl}/${encodeURIComponent(currentAnnouncementId)}` : baseUrl;
            const method = currentAnnouncementId ? "PUT" : "POST";
            const details = await requestJson(url, {
                method,
                body: JSON.stringify(body)
            });
            populateDetails(details);
            await loadAnnouncementList();
            setMessage("기본 정보가 저장되었습니다. 조건과 진행 단계를 이어서 저장할 수 있습니다.", "success");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });

    conditionsForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = app.querySelector("[data-conditions-submit]");
        if (!currentAnnouncementId) {
            setMessage("기본 정보를 먼저 저장해 주세요.", "error");
            return;
        }
        try {
            setBusy(button, true, "저장 중");
            await requestJson(`${baseUrl}/${encodeURIComponent(currentAnnouncementId)}/conditions`, {
                method: "PUT",
                body: JSON.stringify(buildConditionsRequest())
            });
            setMessage("조건과 필요 서류가 저장되었습니다.", "success");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });

    stepsForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = app.querySelector("[data-steps-submit]");
        if (!currentAnnouncementId) {
            setMessage("기본 정보를 먼저 저장해 주세요.", "error");
            return;
        }
        try {
            setBusy(button, true, "저장 중");
            await requestJson(`${baseUrl}/${encodeURIComponent(currentAnnouncementId)}/steps`, {
                method: "PUT",
                body: JSON.stringify(buildStepsRequest())
            });
            setMessage("진행 단계가 저장되었습니다.", "success");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });

    statusForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = app.querySelector("[data-status-submit]");
        if (!currentAnnouncementId) {
            setMessage("상태를 변경할 공고를 먼저 저장하거나 선택해 주세요.", "error");
            return;
        }
        try {
            setBusy(button, true, "저장 중");
            await requestJson(`${baseUrl}/${encodeURIComponent(currentAnnouncementId)}/manual-status`, {
                method: "PATCH",
                body: JSON.stringify({
                    manualStatusCode: valueOf(statusForm, "[name='manualStatusCode']"),
                    reason: nullIfBlank(valueOf(statusForm, "[name='reason']"))
                })
            });
            await loadAnnouncementList();
            setMessage("수동 상태가 저장되었습니다.", "success");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    });

    if (searchForm) {
        searchForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await loadAnnouncementList();
        });
    }

    updateTargetUi();
    loadAnnouncementList();
})();
