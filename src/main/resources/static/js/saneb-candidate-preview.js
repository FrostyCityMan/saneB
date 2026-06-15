(() => {
    const app = document.querySelector("[data-candidate-preview-app]");
    if (!app) {
        return;
    }

    const form = app.querySelector("[data-candidate-preview-form]");
    const submitButton = app.querySelector("[data-candidate-preview-submit]");
    const message = app.querySelector("[data-candidate-preview-message]");
    const result = app.querySelector("[data-candidate-preview-result]");
    const count = app.querySelector("[data-candidate-count]");
    const amount = app.querySelector("[data-candidate-amount]");
    const notice = app.querySelector("[data-candidate-notice]");
    const familyList = app.querySelector("[data-preview-family-list]");
    const addFamilyButton = app.querySelector("[data-preview-family-add]");
    const ageOutput = app.querySelector("[data-preview-age-output]");
    const businessYearsOutput = app.querySelector("[data-preview-business-years-output]");

    const setMessage = (text, status = "info") => {
        if (!message) {
            return;
        }
        message.textContent = text || "";
        message.classList.toggle("is-error", status === "error");
        message.classList.toggle("is-success", status === "success");
    };

    const setBusy = (busy) => {
        if (!submitButton) {
            return;
        }
        if (!submitButton.dataset.defaultText) {
            submitButton.dataset.defaultText = submitButton.textContent;
        }
        submitButton.disabled = busy;
        submitButton.textContent = busy ? "확인 중" : submitButton.dataset.defaultText;
    };

    const withAppLoading = (task, options) => {
        if (window.AppLoading) {
            return window.AppLoading.withLoading(task, options);
        }
        return task();
    };

    const valueOf = (name) => String(form.querySelector(`[name='${name}']`)?.value || "").trim();

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

    const textOrNull = (value) => {
        const text = String(value || "").trim();
        return text === "" ? null : text;
    };

    const rowValueOf = (row, name) => String(row.querySelector(`[name='${name}']`)?.value || "").trim();

    const renderFamilyRow = () => {
        if (!familyList) {
            return;
        }
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
                <label>자녀 학령 상태</label>
                <select name="familySchoolAgeStatusCode">
                    <option value="">선택 안 함</option>
                    <option value="PRESCHOOL">미취학</option>
                    <option value="ELEMENTARY">초등</option>
                    <option value="MIDDLE_HIGH">중고등</option>
                    <option value="COLLEGE">대학</option>
                    <option value="NONE">해당 없음</option>
                </select>
            </div>
            <div class="field-block">
                <label>부모 동거 여부</label>
                <select name="familyCohabiting">
                    <option value="">선택 안 함</option>
                    <option value="true">예</option>
                    <option value="false">아니오</option>
                </select>
            </div>
            <button class="secondary-action family-remove-button" type="button" data-preview-family-remove>삭제</button>
        `;
        familyList.append(row);
    };

    const buildFamilies = () => {
        return Array.from(familyList?.querySelectorAll(".family-row") || [])
            .map((row) => ({
                relationTypeCode: textOrNull(rowValueOf(row, "relationTypeCode")),
                birthYear: numberOrNull(rowValueOf(row, "familyBirthYear")),
                schoolAgeStatusCode: textOrNull(rowValueOf(row, "familySchoolAgeStatusCode")),
                cohabiting: booleanOrNull(rowValueOf(row, "familyCohabiting"))
            }))
            .filter((family) => Object.values(family).some((value) => value !== null));
    };

    const updateAgeOutput = () => {
        if (!ageOutput) {
            return;
        }
        const birthYear = numberOrNull(valueOf("birthYear"));
        if (birthYear == null || Number.isNaN(birthYear)) {
            ageOutput.textContent = "나이는 자동으로 계산됩니다.";
            return;
        }
        const age = new Date().getFullYear() - birthYear;
        ageOutput.textContent = age >= 0 ? `현재 기준 약 ${age.toLocaleString("ko-KR")}세입니다.` : "출생연도를 확인하세요.";
    };

    const updateBusinessYearsOutput = () => {
        if (!businessYearsOutput) {
            return;
        }
        const openingDateText = valueOf("openingDate");
        if (!openingDateText) {
            businessYearsOutput.textContent = "업력은 자동으로 계산됩니다.";
            return;
        }
        const openingDate = new Date(`${openingDateText}T00:00:00`);
        if (Number.isNaN(openingDate.getTime()) || openingDate > new Date()) {
            businessYearsOutput.textContent = "사업 시작일을 확인하세요.";
            return;
        }
        const now = new Date();
        let months = (now.getFullYear() - openingDate.getFullYear()) * 12 + (now.getMonth() - openingDate.getMonth());
        if (now.getDate() < openingDate.getDate()) {
            months -= 1;
        }
        const years = Math.max(0, Math.floor(months / 12));
        const remainMonths = Math.max(0, months % 12);
        businessYearsOutput.textContent = `현재 기준 약 ${years}년 ${remainMonths}개월입니다.`;
    };

    const formatCurrency = (value) => {
        if (value == null) {
            return null;
        }
        return `${Number(value).toLocaleString("ko-KR")}원`;
    };

    const renderResult = (data) => {
        if (count) {
            count.textContent = Number(data.possibleCandidateCount || 0).toLocaleString("ko-KR");
        }
        const minAmount = formatCurrency(data.minSupportAmount);
        const maxAmount = formatCurrency(data.maxSupportAmount);
        if (amount) {
            amount.textContent = minAmount && maxAmount ? `${minAmount} ~ ${maxAmount}` : "가입 후 상세 확인";
        }
        if (notice) {
            notice.textContent = data.criteriaNotice || "회원가입 전 임시 확인 결과입니다.";
        }
        if (result) {
            result.hidden = false;
        }
    };

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const annualRevenue = numberOrNull(valueOf("annualRevenue"));
        const birthYear = numberOrNull(valueOf("birthYear"));
        if (annualRevenue != null && Number.isNaN(annualRevenue)) {
            setMessage("연매출은 숫자로 입력하세요.", "error");
            return;
        }
        if (birthYear != null && Number.isNaN(birthYear)) {
            setMessage("출생연도는 숫자로 입력하세요.", "error");
            return;
        }
        setBusy(true);
        setMessage("");
        const families = buildFamilies();
        try {
            const data = await withAppLoading(
                async () => {
                    const response = await fetch("/api/v1/pre-signup/candidate-preview", {
                        method: "POST",
                        credentials: "same-origin",
                        headers: {
                            Accept: "application/json",
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            representativeName: textOrNull(valueOf("representativeName")),
                            birthYear,
                            regionCode: textOrNull(valueOf("regionCode")),
                            ksicCode: textOrNull(valueOf("ksicCode")),
                            annualRevenue,
                            openingDate: textOrNull(valueOf("openingDate")),
                            hasSpouse: families.some((family) => family.relationTypeCode === "SPOUSE") ? true : null,
                            hasChild: families.some((family) => family.relationTypeCode === "CHILD") ? true : null,
                            hasParent: families.some((family) => family.relationTypeCode === "PARENT") ? true : null,
                            families
                        })
                    });
                    const payload = await response.json().catch(() => null);
                    if (!response.ok || !payload || payload.success !== true) {
                        throw new Error(payload?.message || "간단 결과 확인에 실패했습니다.");
                    }
                    return payload.data || {};
                },
                {
                    preset: "search",
                    title: "간단 결과 확인 중",
                    message: "입력한 기본정보 기준으로 현재 확인 가능한 공고를 찾고 있습니다."
                }
            );
            renderResult(data);
            setMessage("간단 결과 확인이 완료되었습니다.", "success");
        } catch (error) {
            setMessage(error.message || "간단 결과 확인에 실패했습니다.", "error");
        } finally {
            setBusy(false);
        }
    });

    addFamilyButton?.addEventListener("click", renderFamilyRow);
    familyList?.addEventListener("click", (event) => {
        const removeButton = event.target.closest("[data-preview-family-remove]");
        if (!removeButton) {
            return;
        }
        removeButton.closest(".family-row")?.remove();
    });
    form?.querySelector("[name='birthYear']")?.addEventListener("input", updateAgeOutput);
    form?.querySelector("[name='openingDate']")?.addEventListener("input", updateBusinessYearsOutput);
})();
