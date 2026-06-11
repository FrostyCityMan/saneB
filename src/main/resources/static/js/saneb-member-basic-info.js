(() => {
    const app = document.querySelector("[data-member-basic-info-app]");
    if (!app) {
        return;
    }

    const apiUrl = app.dataset.basicInfoUrl;
    const form = app.querySelector("[data-basic-info-form]");
    const familyList = app.querySelector("[data-family-list]");
    const addFamilyButton = app.querySelector("[data-family-add]");
    const submitButton = app.querySelector("[data-basic-info-submit]");
    const message = app.querySelector("[data-basic-info-message]");

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
            families: buildFamilyPayload()
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
