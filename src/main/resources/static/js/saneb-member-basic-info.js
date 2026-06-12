(() => {
    const app = document.querySelector("[data-member-basic-info-app]");
    if (!app) {
        return;
    }

    const apiUrl = app.dataset.basicInfoUrl;
    const form = app.querySelector("[data-basic-info-form]");
    const familyList = app.querySelector("[data-family-list]");
    const addFamilyButton = app.querySelector("[data-family-add]");
    const documentTypeSelect = app.querySelector("[data-document-type-select]");
    const addDocumentButton = app.querySelector("[data-document-add]");
    const documentList = app.querySelector("[data-document-list]");
    const submitButton = app.querySelector("[data-basic-info-submit]");
    const message = app.querySelector("[data-basic-info-message]");

    const regionOptions = [
        ["SEOUL", "서울"],
        ["BUSAN", "부산"],
        ["DAEGU", "대구"],
        ["INCHEON", "인천"],
        ["GWANGJU", "광주"],
        ["DAEJEON", "대전"],
        ["ULSAN", "울산"],
        ["SEJONG", "세종"],
        ["GYEONGGI", "경기"],
        ["GANGWON", "강원"],
        ["CHUNGBUK", "충북"],
        ["CHUNGNAM", "충남"],
        ["JEONBUK", "전북"],
        ["JEONNAM", "전남"],
        ["GYEONGBUK", "경북"],
        ["GYEONGNAM", "경남"],
        ["JEJU", "제주"]
    ];
    const businessTypeOptions = [
        ["SOLE_PROPRIETOR", "개인사업자"],
        ["CORPORATION", "법인사업자"],
        ["SIMPLIFIED_TAXPAYER", "간이과세자"],
        ["GENERAL_TAXPAYER", "일반과세자"],
        ["TAX_EXEMPT", "면세사업자"]
    ];
    const taxTypeOptions = [
        ["GENERAL_TAXPAYER", "일반과세"],
        ["SIMPLIFIED_TAXPAYER", "간이과세"],
        ["TAX_EXEMPT", "면세"]
    ];
    const healthInsuranceOptions = [
        ["WORKPLACE", "직장가입자"],
        ["LOCAL", "지역가입자"],
        ["DEPENDENT", "피부양자"],
        ["UNKNOWN", "잘 모름"]
    ];
    const selectOptionsByFieldKey = {
        REGION_CODE: regionOptions,
        WORKPLACE_REGION_CODE: regionOptions,
        BUSINESS_TYPE_CODE: businessTypeOptions,
        TAX_TYPE_CODE: taxTypeOptions,
        HEALTH_INSURANCE_BASIS_CODE: healthInsuranceOptions,
        INSURANCE_SUBSCRIBER_TYPE: healthInsuranceOptions
    };

    let documentCatalog = [];
    let selectedDocumentTypes = new Set();

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

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.classList.toggle("is-success", status === "success");
        message.classList.toggle("is-error", status === "error");
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

    const valueOf = (name) => {
        const field = form.querySelector(`[name='${name}']`);
        return field ? String(field.value || "").trim() : "";
    };

    const rowValueOf = (row, name) => {
        const field = row.querySelector(`[name='${name}']`);
        return field ? String(field.value || "").trim() : "";
    };

    const numberOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : Number(text);
    };

    const textOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : text;
    };

    const booleanOrNull = (value) => {
        if (value === "true") {
            return true;
        }
        if (value === "false") {
            return false;
        }
        return null;
    };

    const incomeFlag = (incomePresenceCode) => {
        if (incomePresenceCode === "HAS_INCOME") {
            return true;
        }
        if (incomePresenceCode === "NONE") {
            return false;
        }
        return null;
    };

    const setFieldValue = (name, value) => {
        const field = form.querySelector(`[name='${name}']`);
        if (!field) {
            return;
        }
        field.value = value == null ? "" : String(value);
    };

    const setSelectValue = (name, value) => {
        const field = form.querySelector(`[name='${name}']`);
        if (!field) {
            return;
        }
        const nextValue = value == null ? "" : String(value);
        if (nextValue !== "" && !Array.from(field.options).some((option) => option.value === nextValue)) {
            const option = document.createElement("option");
            option.value = nextValue;
            option.textContent = nextValue;
            field.append(option);
        }
        field.value = nextValue;
    };

    const appendOption = (select, value, label) => {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        select.append(option);
    };

    const selectOptionsForField = (field) => {
        return selectOptionsByFieldKey[field.fieldKey] || [];
    };

    const currentDocumentValue = (field) => {
        if (field.valueText != null && field.valueText !== "") {
            return String(field.valueText);
        }
        if (field.valueNumber != null) {
            return String(field.valueNumber);
        }
        if (field.valueDate != null) {
            return String(field.valueDate);
        }
        if (field.valueBoolean != null) {
            return String(field.valueBoolean);
        }
        return "";
    };

    const renderFamilyRow = (family = {}) => {
        const row = document.createElement("div");
        row.className = "family-row";
        row.innerHTML = `
            <div class="field-block">
                <label>관계</label>
                <select name="relationTypeCode">
                    <option value="">선택</option>
                    <option value="SPOUSE">배우자</option>
                    <option value="CHILD">자녀</option>
                    <option value="PARENT">부모</option>
                </select>
            </div>
            <div class="field-block">
                <label>출생연도</label>
                <input name="familyBirthYear" type="number" min="1900" max="2200" placeholder="예: 2018">
            </div>
            <div class="field-block">
                <label>소득 여부</label>
                <select name="familyIncomePresenceCode">
                    <option value="">선택 안 함</option>
                    <option value="UNKNOWN">잘 모름</option>
                    <option value="NONE">소득 없음</option>
                    <option value="HAS_INCOME">소득 있음</option>
                </select>
            </div>
            <div class="field-block">
                <label>연 소득 금액</label>
                <input name="familyIncomeAmount" type="number" min="0" step="10000" placeholder="예: 12000000">
            </div>
            <button class="secondary-action family-remove-button" type="button" data-family-remove>삭제</button>
        `;
        row.querySelector("[name='relationTypeCode']").value = family.relationTypeCode || "";
        row.querySelector("[name='familyBirthYear']").value = family.birthYear == null ? "" : String(family.birthYear);
        row.querySelector("[name='familyIncomePresenceCode']").value = family.incomePresenceCode || "";
        row.querySelector("[name='familyIncomeAmount']").value = family.incomeAmount == null ? "" : String(family.incomeAmount);
        familyList.append(row);
    };

    const renderDocumentSelector = () => {
        if (!documentTypeSelect) {
            return;
        }
        const previousValue = documentTypeSelect.value;
        documentTypeSelect.replaceChildren();
        appendOption(documentTypeSelect, "", "서류 선택");

        documentCatalog
            .filter((documentInput) => !selectedDocumentTypes.has(documentInput.documentTypeCode))
            .forEach((documentInput) => {
                appendOption(documentTypeSelect, documentInput.documentTypeCode, documentInput.documentTypeLabel);
            });

        if (previousValue && !selectedDocumentTypes.has(previousValue)) {
            documentTypeSelect.value = previousValue;
        }
        if (addDocumentButton) {
            addDocumentButton.disabled = documentTypeSelect.options.length <= 1;
        }
    };

    const renderDocumentField = (field) => {
        const block = document.createElement("div");
        block.className = "field-block document-field-block";
        block.dataset.standardFieldId = field.standardFieldId;
        block.dataset.fieldTypeCode = field.fieldTypeCode;

        const label = document.createElement("label");
        label.textContent = `${field.fieldLabel} (선택)`;
        block.append(label);

        const fieldTypeCode = field.fieldTypeCode;
        let input;
        if (fieldTypeCode === "TEXTAREA") {
            input = document.createElement("textarea");
            input.rows = 3;
            input.dataset.documentValueType = "text";
            input.placeholder = "필요한 경우만 입력";
            input.value = field.valueText || "";
        } else if (fieldTypeCode === "BOOLEAN") {
            input = document.createElement("select");
            input.dataset.documentValueType = "boolean";
            appendOption(input, "", "선택 안 함");
            appendOption(input, "true", "예");
            appendOption(input, "false", "아니오");
            input.value = field.valueBoolean == null ? "" : String(field.valueBoolean);
        } else if (fieldTypeCode === "DATE") {
            input = document.createElement("input");
            input.type = "date";
            input.dataset.documentValueType = "date";
            input.value = field.valueDate || "";
        } else if (fieldTypeCode === "NUMBER" || fieldTypeCode === "AMOUNT") {
            input = document.createElement("input");
            input.type = "number";
            input.min = "0";
            input.step = fieldTypeCode === "AMOUNT" ? "10000" : "1";
            input.dataset.documentValueType = "number";
            input.placeholder = fieldTypeCode === "AMOUNT" ? "예: 30000000" : "숫자 입력";
            input.value = field.valueNumber == null ? "" : String(field.valueNumber);
        } else if (fieldTypeCode === "SELECT" || fieldTypeCode === "RADIO" || fieldTypeCode === "MULTI_SELECT") {
            const options = selectOptionsForField(field);
            if (options.length > 0) {
                input = document.createElement("select");
                input.dataset.documentValueType = "text";
                appendOption(input, "", "선택 안 함");
                options.forEach(([value, labelText]) => appendOption(input, value, labelText));
                input.value = currentDocumentValue(field);
            } else {
                input = document.createElement("input");
                input.type = "text";
                input.dataset.documentValueType = "text";
                input.placeholder = "값 입력";
                input.value = currentDocumentValue(field);
            }
        } else {
            input = document.createElement("input");
            input.type = "text";
            input.dataset.documentValueType = "text";
            input.placeholder = "값 입력";
            input.value = currentDocumentValue(field);
        }
        input.dataset.documentInput = "true";
        block.append(input);

        if (field.helpText) {
            const help = document.createElement("small");
            help.textContent = field.helpText;
            block.append(help);
        }
        return block;
    };

    const renderDocumentCard = (documentInput) => {
        const card = document.createElement("article");
        card.className = "member-document-card";
        card.dataset.documentTypeCode = documentInput.documentTypeCode;

        const header = document.createElement("div");
        header.className = "member-document-card-head";

        const titleWrap = document.createElement("div");
        const eyebrow = document.createElement("span");
        eyebrow.className = "eyebrow";
        eyebrow.textContent = "선택 서류";
        const title = document.createElement("h3");
        title.textContent = documentInput.documentTypeLabel;
        titleWrap.append(eyebrow, title);

        const removeButton = document.createElement("button");
        removeButton.className = "secondary-action";
        removeButton.type = "button";
        removeButton.dataset.documentRemove = "true";
        removeButton.textContent = "서류 삭제";

        header.append(titleWrap, removeButton);
        card.append(header);

        const fieldGrid = document.createElement("div");
        fieldGrid.className = "document-field-grid";
        (documentInput.fields || []).forEach((field) => fieldGrid.append(renderDocumentField(field)));
        card.append(fieldGrid);
        return card;
    };

    const renderDocumentList = () => {
        if (!documentList) {
            return;
        }
        documentList.replaceChildren();
        const selectedDocuments = documentCatalog.filter((documentInput) => selectedDocumentTypes.has(documentInput.documentTypeCode));
        if (selectedDocuments.length === 0) {
            const empty = document.createElement("p");
            empty.className = "field-help empty-document-note";
            empty.textContent = "서류를 선택해 추가하면 입력 항목이 표시됩니다.";
            documentList.append(empty);
            return;
        }
        selectedDocuments.forEach((documentInput) => documentList.append(renderDocumentCard(documentInput)));
    };

    const renderDocuments = () => {
        renderDocumentSelector();
        renderDocumentList();
    };

    const renderResponse = (data) => {
        setFieldValue("birthYear", data.birthYear);
        setSelectValue("regionCode", data.regionCode);
        setSelectValue("incomePresenceCode", data.incomePresenceCode);
        setFieldValue("incomeAmount", data.incomeAmount);
        setSelectValue("healthInsuranceBasisCode", data.healthInsuranceBasisCode);

        const business = data.business || {};
        setFieldValue("businessRegistrationNo", business.businessRegistrationNo);
        setFieldValue("businessName", business.businessName);
        setSelectValue("workplaceRegionCode", business.workplaceRegionCode);
        setFieldValue("openingDate", business.openingDate);
        setFieldValue("annualRevenue", business.annualRevenue);
        setFieldValue("annualRevenueYear", business.annualRevenueYear);
        setSelectValue("businessTypeCode", business.businessTypeCode);
        setSelectValue("companyStageCode", business.companyStageCode);
        setFieldValue("ksicCode", business.ksicCode);
        setSelectValue("hasPolicyFundUsage", business.hasPolicyFundUsage);
        setSelectValue("hasGuaranteeUsage", business.hasGuaranteeUsage);

        familyList.replaceChildren();
        (data.families || []).forEach(renderFamilyRow);

        documentCatalog = Array.isArray(data.documentInputs) ? data.documentInputs : [];
        selectedDocumentTypes = new Set(
            documentCatalog
                .filter((documentInput) => documentInput.selected === true)
                .map((documentInput) => documentInput.documentTypeCode)
        );
        renderDocuments();
    };

    const buildBusinessPayload = () => {
        const payload = {
            businessRegistrationNo: textOrNull(valueOf("businessRegistrationNo")),
            businessName: textOrNull(valueOf("businessName")),
            workplaceRegionCode: textOrNull(valueOf("workplaceRegionCode")),
            openingDate: textOrNull(valueOf("openingDate")),
            ksicCode: textOrNull(valueOf("ksicCode")),
            businessTypeCode: textOrNull(valueOf("businessTypeCode")),
            companyStageCode: textOrNull(valueOf("companyStageCode")),
            annualRevenue: numberOrNull(valueOf("annualRevenue")),
            annualRevenueYear: numberOrNull(valueOf("annualRevenueYear")),
            hasPolicyFundUsage: booleanOrNull(valueOf("hasPolicyFundUsage")),
            hasGuaranteeUsage: booleanOrNull(valueOf("hasGuaranteeUsage"))
        };
        return Object.values(payload).some((value) => value !== null) ? payload : null;
    };

    const buildFamilyPayload = () => {
        return Array.from(familyList.querySelectorAll(".family-row"))
            .map((row) => {
                const incomePresenceCode = textOrNull(rowValueOf(row, "familyIncomePresenceCode"));
                return {
                    relationTypeCode: textOrNull(rowValueOf(row, "relationTypeCode")),
                    birthYear: numberOrNull(rowValueOf(row, "familyBirthYear")),
                    hasIncome: incomeFlag(incomePresenceCode),
                    incomePresenceCode,
                    incomeAmount: numberOrNull(rowValueOf(row, "familyIncomeAmount"))
                };
            })
            .filter((family) => Object.values(family).some((value) => value !== null));
    };

    const buildDocumentFieldPayload = (fieldBlock) => {
        const input = fieldBlock.querySelector("[data-document-input]");
        const valueType = input?.dataset.documentValueType || "text";
        const value = String(input?.value || "").trim();
        const payload = {
            standardFieldId: fieldBlock.dataset.standardFieldId,
            valueText: null,
            valueNumber: null,
            valueDate: null,
            valueBoolean: null
        };
        if (value === "") {
            return payload;
        }
        if (valueType === "number") {
            payload.valueNumber = Number(value);
        } else if (valueType === "date") {
            payload.valueDate = value;
        } else if (valueType === "boolean") {
            payload.valueBoolean = booleanOrNull(value);
        } else {
            payload.valueText = value;
        }
        return payload;
    };

    const buildDocumentPayload = () => {
        return Array.from(documentList?.querySelectorAll(".member-document-card") || [])
            .map((card) => ({
                documentTypeCode: card.dataset.documentTypeCode,
                fields: Array.from(card.querySelectorAll(".document-field-block")).map(buildDocumentFieldPayload)
            }));
    };

    const buildPayload = () => {
        const incomePresenceCode = textOrNull(valueOf("incomePresenceCode"));
        return {
            birthYear: numberOrNull(valueOf("birthYear")),
            regionCode: textOrNull(valueOf("regionCode")),
            hasIncome: incomeFlag(incomePresenceCode),
            incomePresenceCode,
            incomeAmount: numberOrNull(valueOf("incomeAmount")),
            healthInsuranceBasisCode: textOrNull(valueOf("healthInsuranceBasisCode")),
            business: buildBusinessPayload(),
            families: buildFamilyPayload(),
            documentInputs: buildDocumentPayload()
        };
    };

    const load = async () => {
        try {
            const data = await requestJson(apiUrl);
            renderResponse(data);
            setMessage("저장된 기본정보를 불러왔습니다.", "success");
        } catch (error) {
            setMessage(error.message || "기본정보를 불러오지 못했습니다.", "error");
        }
    };

    addFamilyButton?.addEventListener("click", () => {
        renderFamilyRow();
    });

    familyList?.addEventListener("click", (event) => {
        const removeButton = event.target.closest("[data-family-remove]");
        if (!removeButton) {
            return;
        }
        removeButton.closest(".family-row")?.remove();
    });

    addDocumentButton?.addEventListener("click", () => {
        const documentTypeCode = documentTypeSelect?.value || "";
        if (!documentTypeCode) {
            return;
        }
        selectedDocumentTypes.add(documentTypeCode);
        if (documentTypeSelect) {
            documentTypeSelect.value = "";
        }
        renderDocuments();
    });

    documentList?.addEventListener("click", (event) => {
        const removeButton = event.target.closest("[data-document-remove]");
        if (!removeButton) {
            return;
        }
        const card = removeButton.closest(".member-document-card");
        const documentTypeCode = card?.dataset.documentTypeCode;
        if (documentTypeCode) {
            selectedDocumentTypes.delete(documentTypeCode);
        }
        renderDocuments();
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        setBusy(true);
        setMessage("");
        try {
            const data = await requestJson(apiUrl, {
                method: "PUT",
                body: JSON.stringify(buildPayload())
            });
            renderResponse(data);
            setMessage("기본정보를 저장했습니다. 대시보드에서 진행 가능 현황을 확인할 수 있습니다.", "success");
        } catch (error) {
            setMessage(error.message || "기본정보 저장에 실패했습니다.", "error");
        } finally {
            setBusy(false);
        }
    });

    load();
})();
