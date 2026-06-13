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

    const fieldTypeLabels = {
        TEXT: "짧은 글",
        TEXTAREA: "긴 글",
        NUMBER: "숫자",
        AMOUNT: "금액",
        DATE: "날짜",
        BOOLEAN: "예/아니오",
        SELECT: "목록에서 선택",
        RADIO: "하나만 선택",
        MULTI_SELECT: "여러 개 선택"
    };
    const scopeLabels = {
        BUSINESS: "사업자 조건",
        PERSONAL: "개인 조건",
        SPOUSE: "배우자 조건",
        CHILD: "자녀 조건",
        PARENT: "부모 조건",
        APPLICATION: "신청 조건",
        SUPPORT: "지원 내용"
    };
    const approvalStatusLabels = {
        DRAFT: "초안",
        REQUESTED: "승인 요청",
        APPROVED: "승인",
        REJECTED: "반려",
        CANCELED: "취소"
    };
    const optionFieldTypes = new Set(["SELECT", "RADIO", "MULTI_SELECT"]);

    const baseUrl = app.dataset.baseUrl;
    const listUrl = app.dataset.listUrl;
    const listContainer = app.querySelector("[data-announcement-list]");
    const message = app.querySelector("[data-announcement-message]");
    const currentIdLabel = app.querySelector("[data-current-announcement-id]");
    const basicForm = app.querySelector("[data-announcement-basic-form]");
    const conditionsForm = app.querySelector("[data-announcement-conditions-form]");
    const industryConditionList = conditionsForm ? conditionsForm.querySelector("[data-industry-condition-list]") : null;
    const numericConditionList = conditionsForm ? conditionsForm.querySelector("[data-numeric-condition-list]") : null;
    const optionConditionList = conditionsForm ? conditionsForm.querySelector("[data-option-condition-list]") : null;
    const documentRequirementList = conditionsForm ? conditionsForm.querySelector("[data-document-requirement-list]") : null;
    const stepsForm = app.querySelector("[data-announcement-steps-form]");
    const stepsList = stepsForm ? stepsForm.querySelector("[data-step-list]") : null;
    const statusForm = app.querySelector("[data-announcement-status-form]");
    const approvalForm = app.querySelector("[data-announcement-approval-form]");
    const approvalStatusLabel = app.querySelector("[data-approval-status-label]");
    const searchForm = app.querySelector("[data-announcement-search-form]");
    const businessPanel = app.querySelector("[data-business-panel]");
    const nonBusinessPanel = app.querySelector("[data-non-business-panel]");
    const activeTargetLabel = app.querySelector("[data-active-target-label]");
    const targetSpecificTitle = app.querySelector("[data-target-specific-title]");
    const dynamicRequirementsForm = app.querySelector("[data-dynamic-requirements-form]");
    const dynamicRequirementsList = app.querySelector("[data-dynamic-requirements-list]");
    const dynamicRequirementsSummary = app.querySelector("[data-dynamic-requirements-summary]");
    let currentAnnouncementId = "";
    const conditionTemplates = {
        industry: industryConditionList?.querySelector("[data-industry-condition-row]")?.cloneNode(true),
        numeric: numericConditionList?.querySelector("[data-numeric-condition-row]")?.cloneNode(true),
        option: optionConditionList?.querySelector("[data-option-condition-row]")?.cloneNode(true),
        document: documentRequirementList?.querySelector("[data-document-requirement-row]")?.cloneNode(true)
    };
    let defaultStepRequests = [];

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

    const updateCurrentAnnouncement = (announcementId, announcementCode) => {
        currentAnnouncementId = announcementId || "";
        if (currentIdLabel) {
            currentIdLabel.textContent = announcementCode || currentAnnouncementId || "신규 입력";
        }
    };

    const updateApprovalUi = (approvalStatusCode) => {
        const statusCode = approvalStatusCode || "DRAFT";
        if (approvalStatusLabel) {
            approvalStatusLabel.textContent = currentAnnouncementId
                    ? (approvalStatusLabels[statusCode] || statusCode)
                    : "신규 입력";
        }
        if (approvalForm) {
            approvalForm.dataset.approvalStatusCode = currentAnnouncementId ? statusCode : "";
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

    const conditionRows = (list, selector) => list ? Array.from(list.querySelectorAll(selector)) : [];
    const industryRows = () => conditionRows(industryConditionList, "[data-industry-condition-row]");
    const numericRows = () => conditionRows(numericConditionList, "[data-numeric-condition-row]");
    const optionRows = () => conditionRows(optionConditionList, "[data-option-condition-row]");
    const documentRows = () => conditionRows(documentRequirementList, "[data-document-requirement-row]");

    const clearConditionRow = (row) => {
        row.querySelectorAll("input, textarea").forEach((field) => {
            if (field.type === "checkbox") {
                field.checked = false;
            } else {
                field.value = "";
            }
        });
        row.querySelectorAll("select").forEach((field) => {
            field.selectedIndex = 0;
        });
    };

    const normalizeConditionRows = (list, selector, removeSelector) => {
        conditionRows(list, selector).forEach((row, index, rows) => {
            const removeButton = row.querySelector(removeSelector);
            if (removeButton) {
                removeButton.disabled = rows.length <= 1;
            }
            const sortOrder = row.querySelector("[name='sortOrder']");
            if (sortOrder) {
                sortOrder.value = String(index + 1);
            }
        });
    };

    const appendConditionRow = (list, template, selector, removeSelector, defaults = {}) => {
        if (!list || !template) {
            return null;
        }
        const row = template.cloneNode(true);
        clearConditionRow(row);
        Object.entries(defaults).forEach(([name, value]) => {
            const field = row.querySelector(`[name='${name}']`);
            if (field) {
                if (field.type === "checkbox") {
                    field.checked = Boolean(value);
                } else {
                    field.value = value ?? "";
                }
            }
        });
        list.append(row);
        normalizeConditionRows(list, selector, removeSelector);
        return row;
    };

    const removeConditionRow = (button, list, selector, removeSelector) => {
        const row = button.closest(selector);
        if (conditionRows(list, selector).length <= 1) {
            clearConditionRow(row);
        } else {
            row?.remove();
        }
        normalizeConditionRows(list, selector, removeSelector);
    };

    const renderConditionRows = (list, template, selector, removeSelector, values, applyValue) => {
        if (!list || !template) {
            return;
        }
        list.replaceChildren();
        const rows = values && values.length > 0 ? values : [null];
        rows.forEach((value) => {
            const row = template.cloneNode(true);
            clearConditionRow(row);
            if (value) {
                applyValue(row, value);
            }
            list.append(row);
        });
        normalizeConditionRows(list, selector, removeSelector);
    };

    const buildConditionsRequest = () => {
        const industryConditions = [];
        industryRows().forEach((row) => {
            const ksicCode = valueOf(row, "[name='ksicCode']");
            if (!ksicCode) {
                return;
            }
            industryConditions.push({
                conditionTypeCode: valueOf(row, "[name='conditionTypeCode']") || "INCLUDE",
                ksicCode
            });
        });

        const numericConditions = [];
        numericRows().forEach((row) => {
            const conditionKey = valueOf(row, "[name='conditionKey']");
            if (!conditionKey) {
                return;
            }
            const numeric = buildNumericCondition(
                    valueOf(row, "[name='conditionScopeCode']") || selectedTargetCode(),
                    conditionKey,
                    valueOf(row, "[name='comparatorCode']") || "LTE",
                    numberOf(row, "[name='valueNumber']"),
                    numberOf(row, "[name='minNumber']"),
                    numberOf(row, "[name='maxNumber']"),
                    valueOf(row, "[name='unitCode']")
            );
            if (numeric) {
                numericConditions.push(numeric);
            }
        });

        const optionConditions = [];
        optionRows().forEach((row) => {
            const conditionKey = valueOf(row, "[name='conditionKey']");
            const optionCode = valueOf(row, "[name='optionCode']");
            if (!conditionKey && !optionCode) {
                return;
            }
            if (!conditionKey || !optionCode) {
                throw new Error("선택/상태 조건은 조건 항목과 선택값을 모두 입력해 주세요.");
            }
            optionConditions.push({
                conditionScopeCode: valueOf(row, "[name='conditionScopeCode']") || selectedTargetCode(),
                conditionKey,
                optionCode,
                optionText: nullIfBlank(valueOf(row, "[name='optionText']"))
            });
        });

        const documentRequirements = [];
        documentRows().forEach((row) => {
            const documentTypeCode = valueOf(row, "[name='documentTypeCode']");
            if (!documentTypeCode) {
                return;
            }
            documentRequirements.push({
                documentTypeCode,
                required: Boolean(row.querySelector("[name='required']")?.checked),
                sortOrder: documentRequirements.length + 1
            });
        });

        return {
            industryConditions,
            numericConditions,
            optionConditions,
            documentRequirements
        };
    };

    const stepRows = () => stepsList ? Array.from(stepsList.querySelectorAll("[data-step-row]")) : [];

    const buildStepRequestFromRow = (row, stepOrder) => {
        const stepName = valueOf(row, "[name='stepName']");
        if (!stepName) {
            return null;
        }

        const buttonLabel = valueOf(row, "[name='buttonLabel']");
        const buttonCode = valueOf(row, "[name='buttonCode']") || `STEP_${stepOrder}_BUTTON`;
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
            required: false,
            sortOrder: 1
        }] : [];

        return {
            stepOrder,
            stepName,
            guideMessage: nullIfBlank(valueOf(row, "[name='guideMessage']")),
            actionGuide: nullIfBlank(valueOf(row, "[name='actionGuide']")),
            completionConditionCode: valueOf(row, "[name='completionConditionCode']") || "BUTTON_CLICK",
            nextConditionCode: nullIfBlank(valueOf(row, "[name='nextConditionCode']")),
            active: Boolean(row.querySelector("[name='stepActive']")?.checked),
            buttons,
            documents
        };
    };

    const setFieldValue = (row, selector, value) => {
        const field = row.querySelector(selector);
        if (field) {
            if (field.tagName === "SELECT" && value && !Array.from(field.options).some((option) => option.value === value)) {
                const option = document.createElement("option");
                option.value = value;
                option.textContent = value;
                field.append(option);
            }
            field.value = value ?? "";
        }
    };

    const clearStepRow = (row) => {
        setFieldValue(row, "[name='stepName']", "");
        setFieldValue(row, "[name='completionConditionCode']", "BUTTON_CLICK");
        setFieldValue(row, "[name='guideMessage']", "");
        setFieldValue(row, "[name='actionGuide']", "");
        setFieldValue(row, "[name='nextConditionCode']", "");
        setFieldValue(row, "[name='buttonLabel']", "");
        setFieldValue(row, "[name='buttonCode']", "");
        setFieldValue(row, "[name='stepDocumentTypeCode']", "");
        const activeField = row.querySelector("[name='stepActive']");
        if (activeField) {
            activeField.checked = true;
        }
    };

    const applyStepToRow = (row, step) => {
        setFieldValue(row, "[name='stepName']", step.stepName || "");
        setFieldValue(row, "[name='completionConditionCode']", step.completionConditionCode || "BUTTON_CLICK");
        setFieldValue(row, "[name='guideMessage']", step.guideMessage || "");
        setFieldValue(row, "[name='actionGuide']", step.actionGuide || "");
        setFieldValue(row, "[name='nextConditionCode']", step.nextConditionCode || "");
        const activeField = row.querySelector("[name='stepActive']");
        if (activeField) {
            activeField.checked = step.active !== false;
        }

        const firstButton = (step.buttons || [])[0];
        setFieldValue(row, "[name='buttonLabel']", firstButton ? firstButton.buttonLabel || "" : "");
        setFieldValue(row, "[name='buttonCode']", firstButton ? firstButton.buttonCode || "" : "");

        const firstDocument = (step.documents || [])[0];
        setFieldValue(row, "[name='stepDocumentTypeCode']", firstDocument ? firstDocument.documentTypeCode || "" : "");
    };

    const normalizeStepRows = () => {
        stepRows().forEach((row, index, rows) => {
            const order = row.querySelector(".step-order");
            if (order) {
                order.textContent = String(index + 1);
            }
            let removeButton = row.querySelector("[data-step-remove]");
            if (!removeButton) {
                const actionBlock = document.createElement("div");
                actionBlock.className = "step-row-actions span-2";
                removeButton = document.createElement("button");
                removeButton.type = "button";
                removeButton.className = "secondary-action";
                removeButton.dataset.stepRemove = "true";
                removeButton.textContent = "단계 삭제";
                actionBlock.append(removeButton);
                row.querySelector(".form-grid")?.append(actionBlock);
            }
            removeButton.disabled = rows.length <= 1;
        });
    };

    const renderStepRows = (steps = []) => {
        if (!stepsList) {
            return;
        }
        const source = stepRows()[0];
        if (!source) {
            return;
        }
        stepsList.replaceChildren();
        const nextSteps = steps.length > 0 ? steps : defaultStepRequests;
        if (nextSteps.length === 0) {
            const row = source.cloneNode(true);
            clearStepRow(row);
            stepsList.append(row);
            normalizeStepRows();
            return;
        }
        nextSteps.forEach((step) => {
            const row = source.cloneNode(true);
            clearStepRow(row);
            applyStepToRow(row, step);
            stepsList.append(row);
        });
        normalizeStepRows();
    };

    const appendBlankStepRow = () => {
        const source = stepRows()[0];
        if (!source || !stepsList) {
            return;
        }
        const row = source.cloneNode(true);
        clearStepRow(row);
        stepsList.append(row);
        normalizeStepRows();
        row.querySelector("[name='stepName']")?.focus();
    };

    const buildStepsRequest = () => {
        const steps = [];
        stepRows().forEach((row) => {
            const stepName = valueOf(row, "[name='stepName']");
            if (!stepName) {
                return;
            }

            steps.push(buildStepRequestFromRow(row, steps.length + 1));
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
        const approvalLabel = approvalStatusLabels[item.approvalStatusCode] || item.approvalStatusCode || "초안";
        meta.textContent = `${item.announcementCode || "공고 코드 없음"} · ${targetLabels[item.targetTypeCode] || item.targetTypeCode} · ${item.manualStatusCode || "NORMAL"} · ${approvalLabel}`;

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

    const optionListText = (options) => (options || [])
            .map((option) => `${option.optionCode || ""}=${option.optionLabel || ""}`)
            .join("\n");

    const setDynamicRequirementSummary = (text) => {
        if (dynamicRequirementsSummary) {
            dynamicRequirementsSummary.textContent = text;
        }
    };

    const createSelect = (name, values, selectedValue) => {
        const select = document.createElement("select");
        select.name = name;
        Object.entries(values).forEach(([value, label]) => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = label;
            option.selected = value === selectedValue;
            select.append(option);
        });
        return select;
    };

    const createDynamicRequirementRow = (requirement = {}) => {
        const row = document.createElement("article");
        row.className = "dynamic-requirement-row";
        row.dataset.dynamicRequirementRow = "true";

        const fieldKeyBlock = document.createElement("div");
        fieldKeyBlock.className = "field-block";
        const fieldKeyLabel = document.createElement("label");
        fieldKeyLabel.textContent = "항목 식별값";
        const fieldKeyInput = document.createElement("input");
        fieldKeyInput.name = "fieldKey";
        fieldKeyInput.type = "text";
        fieldKeyInput.maxLength = 80;
        fieldKeyInput.required = true;
        fieldKeyInput.placeholder = "예: 연매출";
        fieldKeyInput.value = requirement.fieldKey || "";
        fieldKeyBlock.append(fieldKeyLabel, fieldKeyInput);

        const labelBlock = document.createElement("div");
        labelBlock.className = "field-block";
        const labelLabel = document.createElement("label");
        labelLabel.textContent = "화면 표시 이름";
        const labelInput = document.createElement("input");
        labelInput.name = "fieldLabel";
        labelInput.type = "text";
        labelInput.maxLength = 200;
        labelInput.required = true;
        labelInput.placeholder = "예: 연 매출";
        labelInput.value = requirement.fieldLabel || "";
        labelBlock.append(labelLabel, labelInput);

        const fieldTypeBlock = document.createElement("div");
        fieldTypeBlock.className = "field-block";
        const fieldTypeLabel = document.createElement("label");
        fieldTypeLabel.textContent = "입력 유형";
        const fieldTypeSelect = createSelect("fieldTypeCode", fieldTypeLabels, requirement.fieldTypeCode || "TEXT");
        fieldTypeBlock.append(fieldTypeLabel, fieldTypeSelect);

        const scopeBlock = document.createElement("div");
        scopeBlock.className = "field-block";
        const scopeLabel = document.createElement("label");
        scopeLabel.textContent = "적용 범위";
        const scopeSelect = createSelect("scopeCode", scopeLabels, requirement.scopeCode || "APPLICATION");
        scopeBlock.append(scopeLabel, scopeSelect);

        const optionBlock = document.createElement("div");
        optionBlock.className = "field-block span-2 dynamic-options-block";
        const optionLabel = document.createElement("label");
        optionLabel.textContent = "선택 항목";
        const optionTextarea = document.createElement("textarea");
        optionTextarea.name = "options";
        optionTextarea.rows = 3;
        optionTextarea.placeholder = "선택값=화면에 보일 이름";
        optionTextarea.value = optionListText(requirement.options);
        const optionHelp = document.createElement("small");
        optionHelp.textContent = "목록 선택, 하나만 선택, 여러 개 선택 유형에서만 사용합니다. 한 줄에 하나씩 입력합니다.";
        optionBlock.append(optionLabel, optionTextarea, optionHelp);

        const helpBlock = document.createElement("div");
        helpBlock.className = "field-block span-2";
        const helpLabel = document.createElement("label");
        helpLabel.textContent = "도움말";
        const helpInput = document.createElement("textarea");
        helpInput.name = "helpText";
        helpInput.rows = 2;
        helpInput.placeholder = "사용자가 입력할 때 볼 안내 문구";
        helpInput.value = requirement.helpText || "";
        helpBlock.append(helpLabel, helpInput);

        const flags = document.createElement("div");
        flags.className = "dynamic-requirement-flags";
        flags.innerHTML = `
            <label><input type="checkbox" name="required"> 필수 입력으로 요청</label>
            <label><input type="checkbox" name="sensitive"> 민감정보</label>
        `;
        flags.querySelector("[name='required']").checked = Boolean(requirement.required);
        flags.querySelector("[name='sensitive']").checked = Boolean(requirement.sensitive);

        const sortBlock = document.createElement("div");
        sortBlock.className = "field-block";
        const sortLabel = document.createElement("label");
        sortLabel.textContent = "표시 순서";
        const sortInput = document.createElement("input");
        sortInput.name = "sortOrder";
        sortInput.type = "number";
        sortInput.min = "0";
        sortInput.step = "1";
        sortInput.value = Number.isFinite(Number(requirement.sortOrder)) ? requirement.sortOrder : 0;
        sortBlock.append(sortLabel, sortInput);

        const actions = document.createElement("div");
        actions.className = "dynamic-requirement-actions";
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "secondary-action";
        remove.dataset.dynamicRequirementRemove = "true";
        remove.textContent = "삭제";
        actions.append(remove);

        row.append(fieldKeyBlock, labelBlock, fieldTypeBlock, scopeBlock, sortBlock, flags, optionBlock, helpBlock, actions);

        const updateOptionVisibility = () => {
            optionBlock.hidden = !optionFieldTypes.has(fieldTypeSelect.value);
        };
        fieldTypeSelect.addEventListener("change", updateOptionVisibility);
        updateOptionVisibility();
        return row;
    };

    const renderDynamicRequirements = (requirements = []) => {
        if (!dynamicRequirementsList) {
            return;
        }
        dynamicRequirementsList.replaceChildren();
        if (!requirements.length) {
            const empty = document.createElement("p");
            empty.className = "empty-state";
            empty.textContent = currentAnnouncementId
                    ? "등록된 동적 입력 항목이 없습니다."
                    : "공고를 저장하거나 선택하면 동적 입력 항목을 설정할 수 있습니다.";
            dynamicRequirementsList.append(empty);
        } else {
            requirements.forEach((requirement) => {
                dynamicRequirementsList.append(createDynamicRequirementRow(requirement));
            });
        }
        setDynamicRequirementSummary(`${requirements.length}개 항목`);
    };

    const parseDynamicOptions = (textarea) => {
        const lines = String(textarea.value || "")
                .split(/\r?\n/)
                .map((line) => line.trim())
                .filter(Boolean);
        return lines.map((line, index) => {
            const separatorIndex = line.indexOf("=");
            const optionCode = separatorIndex >= 0 ? line.slice(0, separatorIndex).trim() : line;
            const optionLabel = separatorIndex >= 0 ? line.slice(separatorIndex + 1).trim() : line;
            if (!optionCode || !optionLabel) {
                throw new Error("Option requires optionCode and optionLabel.");
            }
            return {
                optionCode,
                optionLabel,
                sortOrder: index + 1
            };
        });
    };

    const buildDynamicRequirementsRequest = () => {
        const rows = Array.from(app.querySelectorAll("[data-dynamic-requirement-row]"));
        return {
            requirements: rows.map((row, index) => {
                const fieldTypeCode = valueOf(row, "[name='fieldTypeCode']");
                const options = optionFieldTypes.has(fieldTypeCode)
                        ? parseDynamicOptions(row.querySelector("[name='options']"))
                        : [];
                if (optionFieldTypes.has(fieldTypeCode) && options.length === 0) {
                    throw new Error("선택형 입력에는 선택 항목을 하나 이상 입력해 주세요.");
                }
                return {
                    fieldKey: valueOf(row, "[name='fieldKey']"),
                    fieldLabel: valueOf(row, "[name='fieldLabel']"),
                    fieldTypeCode,
                    scopeCode: valueOf(row, "[name='scopeCode']"),
                    required: Boolean(row.querySelector("[name='required']")?.checked),
                    sensitive: Boolean(row.querySelector("[name='sensitive']")?.checked),
                    sortOrder: numberOf(row, "[name='sortOrder']") ?? index,
                    helpText: nullIfBlank(valueOf(row, "[name='helpText']")),
                    options
                };
            })
        };
    };

    const loadDynamicRequirements = async (announcementId) => {
        if (!announcementId || !dynamicRequirementsList) {
            renderDynamicRequirements([]);
            setDynamicRequirementSummary("공고 저장 후 설정");
            return;
        }
        setDynamicRequirementSummary("불러오는 중");
        const data = await requestJson(`${baseUrl}/${encodeURIComponent(announcementId)}/input-requirements`, { method: "GET" });
        renderDynamicRequirements(data ? data.requirements : []);
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
        renderConditionRows(
                industryConditionList,
                conditionTemplates.industry,
                "[data-industry-condition-row]",
                "[data-industry-condition-remove]",
                conditions?.industryConditions || [],
                (row, condition) => {
                    setFieldValue(row, "[name='conditionTypeCode']", condition.conditionTypeCode || "INCLUDE");
                    setFieldValue(row, "[name='ksicCode']", condition.ksicCode || "");
                }
        );
        renderConditionRows(
                numericConditionList,
                conditionTemplates.numeric,
                "[data-numeric-condition-row]",
                "[data-numeric-condition-remove]",
                conditions?.numericConditions || [],
                (row, condition) => {
                    setFieldValue(row, "[name='conditionScopeCode']", condition.conditionScopeCode || targetCode);
                    setFieldValue(row, "[name='conditionKey']", condition.conditionKey || "");
                    setFieldValue(row, "[name='comparatorCode']", condition.comparatorCode || "LTE");
                    setFieldValue(row, "[name='valueNumber']", condition.valueNumber ?? "");
                    setFieldValue(row, "[name='minNumber']", condition.minNumber ?? "");
                    setFieldValue(row, "[name='maxNumber']", condition.maxNumber ?? "");
                    setFieldValue(row, "[name='unitCode']", condition.unitCode || "");
                }
        );
        renderConditionRows(
                optionConditionList,
                conditionTemplates.option,
                "[data-option-condition-row]",
                "[data-option-condition-remove]",
                conditions?.optionConditions || [],
                (row, condition) => {
                    setFieldValue(row, "[name='conditionScopeCode']", condition.conditionScopeCode || targetCode);
                    setFieldValue(row, "[name='conditionKey']", condition.conditionKey || "");
                    setFieldValue(row, "[name='optionCode']", condition.optionCode || "");
                    setFieldValue(row, "[name='optionText']", condition.optionText || "");
                }
        );
        renderConditionRows(
                documentRequirementList,
                conditionTemplates.document,
                "[data-document-requirement-row]",
                "[data-document-requirement-remove]",
                conditions?.documentRequirements || [],
                (row, document) => {
                    setFieldValue(row, "[name='documentTypeCode']", document.documentTypeCode || "");
                    const requiredField = row.querySelector("[name='required']");
                    if (requiredField) {
                        requiredField.checked = document.required === true;
                    }
                }
        );
        updateTargetUi();
    };

    const applySteps = (steps) => {
        renderStepRows(steps || []);
    };

    const populateDetails = (details) => {
        updateCurrentAnnouncement(details.announcementId, details.announcementCode);
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
        updateApprovalUi(details.approvalStatusCode || "DRAFT");
        if (approvalForm) {
            approvalForm.querySelector("[name='requestNote']").value = "";
            approvalForm.querySelector("[name='approvalStatusCode']").value = "APPROVED";
            approvalForm.querySelector("[name='decisionNote']").value = "";
        }
        loadDynamicRequirements(details.announcementId).catch((error) => {
            setMessage(error.message, "error");
        });
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
        applyConditions({
            industryConditions: [],
            numericConditions: [],
            optionConditions: [],
            documentRequirements: [{
                documentTypeCode: "BUSINESS_REGISTRATION",
                required: false,
                sortOrder: 1
            }]
        }, selectedTargetCode());
        renderStepRows(defaultStepRequests);
        statusForm.reset();
        approvalForm?.reset();
        updateApprovalUi("");
        const businessTarget = app.querySelector("input[name='targetTypeCode'][value='BUSINESS']");
        if (businessTarget) {
            businessTarget.checked = true;
        }
        renderDynamicRequirements([]);
        setDynamicRequirementSummary("공고 저장 후 설정");
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
            return;
        }

        if (event.target.matches("[data-step-add]")) {
            event.preventDefault();
            appendBlankStepRow();
            return;
        }

        if (event.target.matches("[data-step-remove]")) {
            event.preventDefault();
            const rows = stepRows();
            const row = event.target.closest("[data-step-row]");
            if (rows.length <= 1) {
                clearStepRow(row);
            } else {
                row?.remove();
            }
            normalizeStepRows();
            return;
        }

        if (event.target.matches("[data-industry-condition-add]")) {
            event.preventDefault();
            appendConditionRow(
                    industryConditionList,
                    conditionTemplates.industry,
                    "[data-industry-condition-row]",
                    "[data-industry-condition-remove]"
            )?.querySelector("[name='ksicCode']")?.focus();
            return;
        }

        if (event.target.matches("[data-numeric-condition-add]")) {
            event.preventDefault();
            appendConditionRow(
                    numericConditionList,
                    conditionTemplates.numeric,
                    "[data-numeric-condition-row]",
                    "[data-numeric-condition-remove]",
                    { conditionScopeCode: selectedTargetCode() }
            )?.querySelector("[name='conditionKey']")?.focus();
            return;
        }

        if (event.target.matches("[data-option-condition-add]")) {
            event.preventDefault();
            appendConditionRow(
                    optionConditionList,
                    conditionTemplates.option,
                    "[data-option-condition-row]",
                    "[data-option-condition-remove]",
                    { conditionScopeCode: selectedTargetCode() }
            )?.querySelector("[name='conditionKey']")?.focus();
            return;
        }

        if (event.target.matches("[data-document-requirement-add]")) {
            event.preventDefault();
            appendConditionRow(
                    documentRequirementList,
                    conditionTemplates.document,
                    "[data-document-requirement-row]",
                    "[data-document-requirement-remove]",
                    { required: false }
            )?.querySelector("[name='documentTypeCode']")?.focus();
            return;
        }

        if (event.target.matches("[data-industry-condition-remove]")) {
            event.preventDefault();
            removeConditionRow(
                    event.target,
                    industryConditionList,
                    "[data-industry-condition-row]",
                    "[data-industry-condition-remove]"
            );
            return;
        }

        if (event.target.matches("[data-numeric-condition-remove]")) {
            event.preventDefault();
            removeConditionRow(
                    event.target,
                    numericConditionList,
                    "[data-numeric-condition-row]",
                    "[data-numeric-condition-remove]"
            );
            return;
        }

        if (event.target.matches("[data-option-condition-remove]")) {
            event.preventDefault();
            removeConditionRow(
                    event.target,
                    optionConditionList,
                    "[data-option-condition-row]",
                    "[data-option-condition-remove]"
            );
            return;
        }

        if (event.target.matches("[data-document-requirement-remove]")) {
            event.preventDefault();
            removeConditionRow(
                    event.target,
                    documentRequirementList,
                    "[data-document-requirement-row]",
                    "[data-document-requirement-remove]"
            );
            return;
        }

        if (event.target.matches("[data-dynamic-requirement-add]")) {
            event.preventDefault();
            if (!currentAnnouncementId) {
                setMessage("공고를 먼저 저장하거나 선택하세요.", "error");
                return;
            }
            if (dynamicRequirementsList && dynamicRequirementsList.querySelector(".empty-state")) {
                dynamicRequirementsList.replaceChildren();
            }
            dynamicRequirementsList?.append(createDynamicRequirementRow({
                fieldTypeCode: "TEXT",
                scopeCode: selectedTargetCode(),
                required: false,
                sensitive: false,
                sortOrder: app.querySelectorAll("[data-dynamic-requirement-row]").length + 1,
                options: []
            }));
            setDynamicRequirementSummary(`${app.querySelectorAll("[data-dynamic-requirement-row]").length}개 항목`);
            return;
        }

        if (event.target.matches("[data-dynamic-requirement-reload]")) {
            event.preventDefault();
            if (!currentAnnouncementId) {
                setMessage("공고를 먼저 저장하거나 선택하세요.", "error");
                return;
            }
            try {
                await loadDynamicRequirements(currentAnnouncementId);
                setMessage("동적 입력 항목을 다시 불러왔습니다.", "success");
            } catch (error) {
                setMessage(error.message, "error");
            }
            return;
        }

        if (event.target.matches("[data-dynamic-requirement-remove]")) {
            event.preventDefault();
            event.target.closest("[data-dynamic-requirement-row]")?.remove();
            setDynamicRequirementSummary(`${app.querySelectorAll("[data-dynamic-requirement-row]").length}개 항목`);
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

    if (approvalForm) {
        approvalForm.addEventListener("click", async (event) => {
            const requestButton = event.target.closest("[data-approval-request-submit]");
            const decisionButton = event.target.closest("[data-approval-decision-submit]");
            if (!requestButton && !decisionButton) {
                return;
            }
            event.preventDefault();
            if (!currentAnnouncementId) {
                setMessage("승인 처리할 공고를 먼저 저장하거나 선택해 주세요.", "error");
                return;
            }

            try {
                if (requestButton) {
                    setBusy(requestButton, true, "요청 중");
                    const details = await requestJson(`${baseUrl}/${encodeURIComponent(currentAnnouncementId)}/approval-requests`, {
                        method: "POST",
                        body: JSON.stringify({
                            requestNote: nullIfBlank(valueOf(approvalForm, "[name='requestNote']"))
                        })
                    });
                    populateDetails(details);
                    await loadAnnouncementList();
                    setMessage("승인 요청 상태로 변경되었습니다.", "success");
                    return;
                }

                setBusy(decisionButton, true, "처리 중");
                const details = await requestJson(`${baseUrl}/${encodeURIComponent(currentAnnouncementId)}/approval`, {
                    method: "PATCH",
                    body: JSON.stringify({
                        approvalStatusCode: valueOf(approvalForm, "[name='approvalStatusCode']") || "APPROVED",
                        decisionNote: nullIfBlank(valueOf(approvalForm, "[name='decisionNote']"))
                    })
                });
                populateDetails(details);
                await loadAnnouncementList();
                setMessage("승인 상태가 저장되었습니다.", "success");
            } catch (error) {
                setMessage(error.message, "error");
            } finally {
                setBusy(requestButton || decisionButton, false);
            }
        });
    }

    if (dynamicRequirementsForm) {
        dynamicRequirementsForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const button = app.querySelector("[data-dynamic-requirements-submit]");
            if (!currentAnnouncementId) {
                setMessage("공고를 먼저 저장하거나 선택하세요.", "error");
                return;
            }
            try {
                setBusy(button, true, "저장 중");
                const data = await requestJson(`${baseUrl}/${encodeURIComponent(currentAnnouncementId)}/input-requirements`, {
                    method: "PUT",
                    body: JSON.stringify(buildDynamicRequirementsRequest())
                });
                renderDynamicRequirements(data ? data.requirements : []);
                setMessage("동적 입력 항목이 저장되었습니다.", "success");
            } catch (error) {
                setMessage(error.message, "error");
            } finally {
                setBusy(button, false);
            }
        });
    }

    if (searchForm) {
        searchForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await loadAnnouncementList();
        });
    }

    normalizeConditionRows(industryConditionList, "[data-industry-condition-row]", "[data-industry-condition-remove]");
    normalizeConditionRows(numericConditionList, "[data-numeric-condition-row]", "[data-numeric-condition-remove]");
    normalizeConditionRows(optionConditionList, "[data-option-condition-row]", "[data-option-condition-remove]");
    normalizeConditionRows(documentRequirementList, "[data-document-requirement-row]", "[data-document-requirement-remove]");
    defaultStepRequests = stepRows()
            .map((row, index) => buildStepRequestFromRow(row, index + 1))
            .filter(Boolean);
    normalizeStepRows();
    updateTargetUi();
    updateApprovalUi("");
    renderDynamicRequirements([]);
    loadAnnouncementList();
})();
