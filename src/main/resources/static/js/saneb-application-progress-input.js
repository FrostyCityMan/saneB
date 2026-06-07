(() => {
    const app = document.querySelector("[data-progress-input-app]");
    if (!app || !app.dataset.inputValuesUrl) {
        return;
    }

    const fieldContainer = app.querySelector("[data-progress-dynamic-fields]");
    const form = app.querySelector("[data-progress-dynamic-form]");
    const message = app.querySelector("[data-progress-dynamic-message]");
    const summary = app.querySelector("[data-progress-dynamic-summary]");
    const actionForms = Array.from(app.querySelectorAll("[data-progress-action-form]"));
    const submitButton = app.querySelector("[data-progress-dynamic-submit]");
    let currentValues = [];
    let currentRequirements = new Map();

    const optionFieldTypes = new Set(["SELECT", "RADIO", "MULTI_SELECT"]);

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.hidden = !text;
        message.textContent = text || "";
        message.classList.toggle("is-success", status === "success");
        message.classList.toggle("is-error", status === "error");
    };

    const setSummary = (text) => {
        if (summary) {
            summary.textContent = text;
        }
    };

    const setBusy = (busy) => {
        if (!submitButton) {
            return;
        }
        if (!submitButton.dataset.defaultText) {
            submitButton.dataset.defaultText = submitButton.textContent;
        }
        submitButton.disabled = busy;
        submitButton.textContent = busy ? "저장 중" : submitButton.dataset.defaultText;
    };

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

    const valueForRequirement = (requirementId) => currentValues.find((value) => value.requirementId === requirementId);

    const optionsForRequirement = (requirementId) => {
        const requirement = currentRequirements.get(requirementId);
        return requirement ? requirement.options || [] : [];
    };

    const appendMetaBadges = (header, value) => {
        if (value.required) {
            const required = document.createElement("span");
            required.className = "dynamic-badge is-required";
            required.textContent = "필수";
            header.append(required);
        }
        if (value.sensitive) {
            const sensitive = document.createElement("span");
            sensitive.className = "dynamic-badge is-sensitive";
            sensitive.textContent = "민감";
            header.append(sensitive);
        }
    };

    const createFieldHeader = (value) => {
        const header = document.createElement("div");
        header.className = "dynamic-field-header";

        const label = document.createElement("label");
        label.textContent = value.fieldLabel || value.fieldKey;
        label.htmlFor = `input-${value.requirementId}`;

        const badges = document.createElement("div");
        badges.className = "dynamic-field-badges";
        appendMetaBadges(badges, value);

        header.append(label, badges);
        return header;
    };

    const textInput = (value, type, inputValue) => {
        const input = document.createElement("input");
        input.id = `input-${value.requirementId}`;
        input.name = String(value.requirementId);
        input.type = type;
        input.value = inputValue ?? "";
        input.required = value.required;
        input.dataset.fieldTypeCode = value.fieldTypeCode;
        if (value.sensitive && type === "text") {
            input.type = "password";
            input.autocomplete = "off";
        }
        return input;
    };

    const createScalarControl = (value) => {
        switch (value.fieldTypeCode) {
            case "TEXT":
                return textInput(value, "text", value.valueText);
            case "TEXTAREA": {
                const textarea = document.createElement("textarea");
                textarea.id = `input-${value.requirementId}`;
                textarea.name = String(value.requirementId);
                textarea.rows = 3;
                textarea.required = value.required;
                textarea.dataset.fieldTypeCode = value.fieldTypeCode;
                textarea.value = value.valueText || "";
                return textarea;
            }
            case "NUMBER":
                return textInput(value, "number", value.valueNumber);
            case "AMOUNT": {
                const input = textInput(value, "number", value.valueNumber);
                input.step = "10000";
                input.min = "0";
                return input;
            }
            case "DATE":
                return textInput(value, "date", value.valueDate);
            case "BOOLEAN": {
                const label = document.createElement("label");
                label.className = "dynamic-boolean-control";
                const input = document.createElement("input");
                input.id = `input-${value.requirementId}`;
                input.name = String(value.requirementId);
                input.type = "checkbox";
                input.dataset.fieldTypeCode = value.fieldTypeCode;
                input.checked = value.valueBoolean === true;
                const span = document.createElement("span");
                span.textContent = "해당";
                label.append(input, span);
                return label;
            }
            default:
                return null;
        }
    };

    const createOptionControl = (value) => {
        const options = optionsForRequirement(value.requirementId);
        if (value.fieldTypeCode === "SELECT") {
            const select = document.createElement("select");
            select.id = `input-${value.requirementId}`;
            select.name = String(value.requirementId);
            select.required = value.required;
            select.dataset.fieldTypeCode = value.fieldTypeCode;
            const empty = document.createElement("option");
            empty.value = "";
            empty.textContent = "선택";
            select.append(empty);
            options.forEach((option) => {
                const item = document.createElement("option");
                item.value = option.optionCode;
                item.textContent = option.optionLabel;
                item.selected = option.optionCode === value.optionCode;
                select.append(item);
            });
            return select;
        }

        const group = document.createElement("div");
        group.className = "dynamic-option-group";
        options.forEach((option) => {
            const label = document.createElement("label");
            const input = document.createElement("input");
            input.name = String(value.requirementId);
            input.type = value.fieldTypeCode === "MULTI_SELECT" ? "checkbox" : "radio";
            input.value = option.optionCode;
            input.dataset.fieldTypeCode = value.fieldTypeCode;
            input.checked = value.fieldTypeCode === "MULTI_SELECT"
                    ? (value.optionCodes || []).includes(option.optionCode)
                    : value.optionCode === option.optionCode;
            const span = document.createElement("span");
            span.textContent = option.optionLabel;
            label.append(input, span);
            group.append(label);
        });
        return group;
    };

    const createDynamicField = (value) => {
        const field = document.createElement("article");
        field.className = "dynamic-progress-field";
        field.dataset.requirementId = value.requirementId;
        field.dataset.fieldTypeCode = value.fieldTypeCode;
        field.dataset.required = String(value.required);
        field.dataset.sensitive = String(value.sensitive);

        field.append(createFieldHeader(value));

        if (value.helpText) {
            const help = document.createElement("p");
            help.className = "dynamic-field-help";
            help.textContent = value.helpText;
            field.append(help);
        }

        const control = optionFieldTypes.has(value.fieldTypeCode)
                ? createOptionControl(value)
                : createScalarControl(value);
        if (control) {
            field.append(control);
        }

        const missing = document.createElement("small");
        missing.className = "dynamic-missing-message";
        missing.textContent = "필수 입력이 필요합니다.";
        missing.hidden = true;
        field.append(missing);
        return field;
    };

    const hasSavedValue = (value) => {
        if (value.fieldTypeCode === "TEXT" || value.fieldTypeCode === "TEXTAREA") {
            return typeof value.valueText === "string" && value.valueText.trim() !== "";
        }
        if (value.fieldTypeCode === "NUMBER" || value.fieldTypeCode === "AMOUNT") {
            return value.valueNumber !== null && value.valueNumber !== undefined;
        }
        if (value.fieldTypeCode === "DATE") {
            return Boolean(value.valueDate);
        }
        if (value.fieldTypeCode === "BOOLEAN") {
            return value.valueBoolean !== null && value.valueBoolean !== undefined;
        }
        if (value.fieldTypeCode === "SELECT" || value.fieldTypeCode === "RADIO") {
            return Boolean(value.optionCode);
        }
        if (value.fieldTypeCode === "MULTI_SELECT") {
            return Array.isArray(value.optionCodes) && value.optionCodes.length > 0;
        }
        return false;
    };

    const updateRequiredState = () => {
        const missingRequired = currentValues.filter((value) => value.required && !hasSavedValue(value));
        setSummary(missingRequired.length > 0 ? `필수 ${missingRequired.length}건 필요` : `${currentValues.length}개 항목 완료`);
        app.querySelectorAll("[data-requirement-id]").forEach((field) => {
            const value = valueForRequirement(field.dataset.requirementId);
            const missing = Boolean(value?.required && !hasSavedValue(value));
            field.classList.toggle("is-missing", missing);
            const messageNode = field.querySelector(".dynamic-missing-message");
            if (messageNode) {
                messageNode.hidden = !missing;
            }
        });
        actionForms.forEach((actionForm) => {
            const actionButton = actionForm.querySelector("button[type='submit']");
            if (!actionButton) {
                return;
            }
            actionButton.disabled = missingRequired.length > 0;
            actionButton.title = missingRequired.length > 0 ? "필수 입력값 저장 후 진행할 수 있습니다." : "";
        });
    };

    const renderFields = () => {
        if (!fieldContainer) {
            return;
        }
        fieldContainer.replaceChildren();
        if (currentValues.length === 0) {
            const empty = document.createElement("p");
            empty.className = "empty-state";
            empty.textContent = "공고에 설정된 동적 입력 항목이 없습니다.";
            fieldContainer.append(empty);
            setSummary("0개 항목");
            return;
        }
        currentValues
                .slice()
                .sort((a, b) => a.sortOrder - b.sortOrder)
                .forEach((value) => {
                    fieldContainer.append(createDynamicField(value));
                });
        updateRequiredState();
    };

    const selectFieldValue = (field) => {
        const requirementId = field.dataset.requirementId;
        const fieldTypeCode = field.dataset.fieldTypeCode;
        const request = { requirementId };
        if (fieldTypeCode === "TEXT" || fieldTypeCode === "TEXTAREA") {
            const value = field.querySelector(`[name='${requirementId}']`)?.value?.trim();
            return value ? { ...request, valueText: value } : null;
        }
        if (fieldTypeCode === "NUMBER" || fieldTypeCode === "AMOUNT") {
            const value = field.querySelector(`[name='${requirementId}']`)?.value;
            return value === "" ? null : { ...request, valueNumber: Number(value) };
        }
        if (fieldTypeCode === "DATE") {
            const value = field.querySelector(`[name='${requirementId}']`)?.value;
            return value ? { ...request, valueDate: value } : null;
        }
        if (fieldTypeCode === "BOOLEAN") {
            return { ...request, valueBoolean: Boolean(field.querySelector(`[name='${requirementId}']`)?.checked) };
        }
        if (fieldTypeCode === "SELECT" || fieldTypeCode === "RADIO") {
            const selected = field.querySelector(`[name='${requirementId}']:checked`) || field.querySelector(`select[name='${requirementId}']`);
            const optionCode = selected ? selected.value : "";
            return optionCode ? { ...request, optionCode } : null;
        }
        if (fieldTypeCode === "MULTI_SELECT") {
            const optionCodes = Array.from(field.querySelectorAll(`[name='${requirementId}']:checked`))
                    .map((input) => input.value);
            return optionCodes.length > 0 ? { ...request, optionCodes } : null;
        }
        return null;
    };

    const buildSaveRequest = () => ({
        values: Array.from(app.querySelectorAll("[data-requirement-id]"))
                .map(selectFieldValue)
                .filter(Boolean)
    });

    const loadDynamicInputs = async () => {
        setSummary("불러오는 중");
        const values = await requestJson(app.dataset.inputValuesUrl, { method: "GET" });
        const requirements = await requestJson(
                `/api/v1/announcements/${encodeURIComponent(values.announcementId)}/input-requirements`,
                { method: "GET" }
        );
        currentRequirements = new Map((requirements.requirements || [])
                .map((requirement) => [requirement.requirementId, requirement]));
        currentValues = values.values || [];
        renderFields();
    };

    if (form) {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            setMessage("");
            try {
                setBusy(true);
                const response = await requestJson(app.dataset.inputValuesUrl, {
                    method: "PUT",
                    body: JSON.stringify(buildSaveRequest())
                });
                currentValues = response.values || [];
                renderFields();
                setMessage("입력값이 저장되었습니다.", "success");
            } catch (error) {
                setMessage(error.message, "error");
            } finally {
                setBusy(false);
            }
        });
    }

    loadDynamicInputs().catch((error) => {
        setSummary("불러오기 실패");
        setMessage(error.message, "error");
    });
})();
