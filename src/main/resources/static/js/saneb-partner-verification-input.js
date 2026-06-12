(() => {
    const app = document.querySelector("[data-partner-verification-values-app]");
    if (!app) {
        return;
    }

    const verificationId = app.dataset.verificationId;
    const form = app.querySelector("[data-verification-values-form]");
    const message = app.querySelector("[data-verification-values-message]");
    const submitButton = app.querySelector("[data-verification-values-submit]");
    const refreshButton = app.querySelector("[data-verification-values-refresh]");
    const familyList = app.querySelector("[data-verification-family-list]");
    const familyAddButton = app.querySelector("[data-verification-family-add]");
    const restrictionList = app.querySelector("[data-verification-restriction-list]");

    const regionOptions = [
        ["SEOUL", "서울"], ["BUSAN", "부산"], ["DAEGU", "대구"], ["INCHEON", "인천"],
        ["GWANGJU", "광주"], ["DAEJEON", "대전"], ["ULSAN", "울산"], ["SEJONG", "세종"],
        ["GYEONGGI", "경기"], ["GANGWON", "강원"], ["CHUNGBUK", "충북"], ["CHUNGNAM", "충남"],
        ["JEONBUK", "전북"], ["JEONNAM", "전남"], ["GYEONGBUK", "경북"], ["GYEONGNAM", "경남"],
        ["JEJU", "제주"]
    ];

    const restrictionOptions = [
        ["SAME_BUSINESS_SUSPECTED", "동일 사업 의심"],
        ["SPOUSE_TRANSFER_SUSPECTED", "배우자 명의 이전 의심"],
        ["FAMILY_BYPASS_SUSPECTED", "가족 우회 신청 의심"],
        ["CLOSED_REOPEN_SUSPECTED", "폐업 후 유사 재창업 의심"],
        ["POLICY_FUND_RESTRICTED", "정책자금 제한"],
        ["GUARANTEE_RESTRICTED", "보증 제한"],
        ["CREDIT_RECOVERY", "신용회복 이력"],
        ["PERSONAL_REHABILITATION", "개인회생 이력"],
        ["BANKRUPTCY_HISTORY", "파산 이력"],
        ["TAX_DELINQUENCY", "세금 체납"],
        ["OVERDUE_HISTORY", "연체 이력"],
        ["NEEDS_REVIEW", "운영자 추가 검토 필요"]
    ];

    const appendOption = (select, value, label) => {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        select.append(option);
    };

    app.querySelectorAll("[data-region-select]").forEach((select) => {
        regionOptions.forEach(([value, label]) => appendOption(select, value, label));
    });

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
            throw new Error(payload?.message || "요청 처리에 실패했습니다.");
        }
        return payload.data;
    };

    const field = (name) => form.querySelector(`[name='${name}']`);
    const valueOf = (name) => String(field(name)?.value || "").trim();
    const rowValueOf = (row, name) => String(row.querySelector(`[name='${name}']`)?.value || "").trim();
    const textOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : text;
    };
    const numberOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : Number(text);
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
    const setValue = (name, value) => {
        const target = field(name);
        if (target) {
            target.value = value == null ? "" : String(value);
        }
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
                <input name="birthYear" type="number" min="1900" max="2200" placeholder="예: 2018">
            </div>
            <div class="field-block">
                <label>주소</label>
                <input name="address" type="text" maxlength="500" placeholder="주소 또는 지역">
            </div>
            <div class="field-block">
                <label>학령 상태</label>
                <select name="schoolAgeStatusCode">
                    <option value="">선택 안 함</option>
                    <option value="PRESCHOOL">미취학</option>
                    <option value="ELEMENTARY">초등</option>
                    <option value="MIDDLE_HIGH">중고등</option>
                    <option value="COLLEGE">대학</option>
                    <option value="NONE">해당 없음</option>
                </select>
            </div>
            <div class="field-block">
                <label>재학 여부</label>
                <select name="enrollmentStatusCode">
                    <option value="">선택 안 함</option>
                    <option value="ENROLLED">재학</option>
                    <option value="NOT_ENROLLED">미재학</option>
                    <option value="UNKNOWN">확인 필요</option>
                </select>
            </div>
            <div class="field-block">
                <label>동거 여부</label>
                <select name="cohabiting">
                    <option value="">선택 안 함</option>
                    <option value="true">예</option>
                    <option value="false">아니오</option>
                </select>
            </div>
            <div class="field-block">
                <label>부양 여부</label>
                <select name="supported">
                    <option value="">선택 안 함</option>
                    <option value="true">예</option>
                    <option value="false">아니오</option>
                </select>
            </div>
            <div class="field-block">
                <label>소득 여부</label>
                <select name="hasIncome">
                    <option value="">선택 안 함</option>
                    <option value="true">소득 있음</option>
                    <option value="false">소득 없음</option>
                </select>
            </div>
            <button class="secondary-action family-remove-button" type="button" data-verification-family-remove>삭제</button>
        `;
        row.querySelector("[name='relationTypeCode']").value = family.relationTypeCode || "";
        row.querySelector("[name='birthYear']").value = family.birthYear == null ? "" : String(family.birthYear);
        row.querySelector("[name='address']").value = family.address || "";
        row.querySelector("[name='schoolAgeStatusCode']").value = family.schoolAgeStatusCode || "";
        row.querySelector("[name='enrollmentStatusCode']").value = family.enrollmentStatusCode || "";
        row.querySelector("[name='cohabiting']").value = family.cohabiting == null ? "" : String(family.cohabiting);
        row.querySelector("[name='supported']").value = family.supported == null ? "" : String(family.supported);
        row.querySelector("[name='hasIncome']").value = family.hasIncome == null ? "" : String(family.hasIncome);
        familyList.append(row);
    };

    const renderRestrictions = (flags = []) => {
        const existing = new Map((flags || []).map((flag) => [flag.restrictionCode, flag]));
        restrictionList.replaceChildren();
        restrictionOptions.forEach(([code, label]) => {
            const flag = existing.get(code) || {};
            const row = document.createElement("article");
            row.className = "restriction-flag-card";
            row.dataset.restrictionCode = code;
            row.innerHTML = `
                <label>
                    <input type="checkbox" data-restriction-checked ${flag.checked ? "checked" : ""}>
                    <span>${label}</span>
                </label>
                <textarea rows="2" maxlength="2000" data-restriction-note placeholder="검토 메모">${flag.note || ""}</textarea>
            `;
            restrictionList.append(row);
        });
    };

    const renderDetails = (data) => {
        const member = data.memberValues || {};
        setValue("birthYear", member.birthYear);
        setValue("address", member.address);
        setValue("regionCode", member.regionCode);
        setValue("householder", member.householder);
        setValue("householdMember", member.householdMember);
        setValue("healthInsuranceBasisCode", member.healthInsuranceBasisCode);
        setValue("hasIncome", member.hasIncome);

        const business = data.businessValues || {};
        setValue("annualRevenue", business.annualRevenue);
        setValue("employeeCount", business.employeeCount);
        setValue("regularEmployeeCount", business.regularEmployeeCount);
        setValue("taxStatusCode", business.taxStatusCode);
        setValue("niceCreditScore", business.niceCreditScore);
        setValue("kcbCreditScore", business.kcbCreditScore);
        setValue("hasExistingLoan", business.hasExistingLoan);
        setValue("hasPolicyFundUsage", business.hasPolicyFundUsage);
        setValue("hasGuaranteeUsage", business.hasGuaranteeUsage);
        setValue("financialCheckedOn", business.financialCheckedOn);

        familyList.replaceChildren();
        (data.familyValues || []).forEach(renderFamilyRow);
        renderRestrictions(data.restrictionFlags || []);
    };

    const load = async () => {
        try {
            const data = await requestJson(`/api/v1/partner-verifications/${encodeURIComponent(verificationId)}`);
            renderDetails(data);
            setMessage("검증 값을 불러왔습니다.", "success");
        } catch (error) {
            setMessage(error.message || "검증 값을 불러오지 못했습니다.", "error");
        }
    };

    const buildMemberPayload = () => ({
        birthYear: numberOrNull(valueOf("birthYear")),
        address: textOrNull(valueOf("address")),
        regionCode: textOrNull(valueOf("regionCode")),
        householder: booleanOrNull(valueOf("householder")),
        householdMember: booleanOrNull(valueOf("householdMember")),
        healthInsuranceBasisCode: textOrNull(valueOf("healthInsuranceBasisCode")),
        hasIncome: booleanOrNull(valueOf("hasIncome"))
    });

    const buildBusinessPayload = () => ({
        annualRevenue: numberOrNull(valueOf("annualRevenue")),
        employeeCount: numberOrNull(valueOf("employeeCount")),
        regularEmployeeCount: numberOrNull(valueOf("regularEmployeeCount")),
        taxStatusCode: textOrNull(valueOf("taxStatusCode")),
        niceCreditScore: numberOrNull(valueOf("niceCreditScore")),
        kcbCreditScore: numberOrNull(valueOf("kcbCreditScore")),
        hasExistingLoan: booleanOrNull(valueOf("hasExistingLoan")),
        hasPolicyFundUsage: booleanOrNull(valueOf("hasPolicyFundUsage")),
        hasGuaranteeUsage: booleanOrNull(valueOf("hasGuaranteeUsage")),
        financialCheckedOn: textOrNull(valueOf("financialCheckedOn"))
    });

    const buildFamilyPayload = () => ({
        familyValues: Array.from(familyList.querySelectorAll(".family-row"))
            .map((row) => ({
                relationTypeCode: textOrNull(rowValueOf(row, "relationTypeCode")),
                birthYear: numberOrNull(rowValueOf(row, "birthYear")),
                address: textOrNull(rowValueOf(row, "address")),
                schoolAgeStatusCode: textOrNull(rowValueOf(row, "schoolAgeStatusCode")),
                enrollmentStatusCode: textOrNull(rowValueOf(row, "enrollmentStatusCode")),
                cohabiting: booleanOrNull(rowValueOf(row, "cohabiting")),
                supported: booleanOrNull(rowValueOf(row, "supported")),
                hasIncome: booleanOrNull(rowValueOf(row, "hasIncome"))
            }))
            .filter((family) => family.relationTypeCode)
    });

    const buildRestrictionPayload = () => ({
        restrictionFlags: Array.from(restrictionList.querySelectorAll(".restriction-flag-card"))
            .map((row) => ({
                restrictionCode: row.dataset.restrictionCode,
                checked: row.querySelector("[data-restriction-checked]")?.checked === true,
                note: textOrNull(row.querySelector("[data-restriction-note]")?.value || "")
            }))
    });

    const save = async () => {
        setBusy(true);
        setMessage("");
        try {
            await requestJson(`/api/v1/partner-verifications/${encodeURIComponent(verificationId)}/member-values`, {
                method: "PUT",
                body: JSON.stringify(buildMemberPayload())
            });
            await requestJson(`/api/v1/partner-verifications/${encodeURIComponent(verificationId)}/business-values`, {
                method: "PUT",
                body: JSON.stringify(buildBusinessPayload())
            });
            await requestJson(`/api/v1/partner-verifications/${encodeURIComponent(verificationId)}/family-values`, {
                method: "PUT",
                body: JSON.stringify(buildFamilyPayload())
            });
            await requestJson(`/api/v1/partner-verifications/${encodeURIComponent(verificationId)}/restriction-flags`, {
                method: "PUT",
                body: JSON.stringify(buildRestrictionPayload())
            });
            setMessage("수동 검증 값을 저장했습니다.", "success");
            await load();
        } catch (error) {
            setMessage(error.message || "수동 검증 값 저장에 실패했습니다.", "error");
        } finally {
            setBusy(false);
        }
    };

    familyAddButton?.addEventListener("click", () => renderFamilyRow());
    familyList?.addEventListener("click", (event) => {
        const removeButton = event.target.closest("[data-verification-family-remove]");
        if (removeButton) {
            removeButton.closest(".family-row")?.remove();
        }
    });
    refreshButton?.addEventListener("click", load);
    form?.addEventListener("submit", (event) => {
        event.preventDefault();
        save();
    });
    load();
})();
