(() => {
    const app = document.querySelector("[data-matching-app]");
    if (!app) {
        return;
    }

    const baseUrl = app.dataset.baseUrl;
    const finalUrl = app.dataset.finalUrl || `${baseUrl}/final`;
    const finalRecalculateUrl = app.dataset.finalRecalculateUrl || `${baseUrl}/final-recalculate`;
    const progressUrl = app.dataset.progressUrl;
    const announcementLookupUrl = app.dataset.announcementLookupUrl;
    const memberLookupUrl = app.dataset.memberLookupUrl;
    const canOperate = app.dataset.canOperate === "true";
    const createForm = app.querySelector("[data-matching-create-form]");
    const candidateForm = app.querySelector("[data-matching-candidate-form]");
    const searchForm = app.querySelector("[data-matching-search-form]");
    const list = app.querySelector("[data-matching-list]");
    const message = app.querySelector("[data-matching-message]");
    let activeLookupForm = null;

    const statusLabels = {
        MATCHED: "매칭",
        NOT_MATCHED: "미매칭",
        REVIEW_REQUIRED: "검토 필요",
        BLOCKED: "차단",
        PROGRESSED: "진행 전환"
    };

    const stageLabels = {
        BASIC: "기본 후보",
        FINAL: "최종 매칭"
    };

    const basisLabels = {
        BASIC_INFO: "기본정보",
        PARTNER_INPUT: "상담 입력",
        DOCUMENT_INPUT: "서류 입력"
    };

    const targetLabels = {
        BUSINESS: "사업자",
        PERSONAL: "개인",
        SPOUSE: "가족",
        CHILD: "가족",
        PARENT: "가족",
        FAMILY: "가족"
    };

    const targetClass = (targetTypeCode) => {
        const code = String(targetTypeCode || "").toLowerCase();
        return code ? `target-${code}` : "target-unknown";
    };

    const conditionScopeLabels = {
        BUSINESS: "사업자 조건",
        PERSONAL: "개인 조건",
        SPOUSE: "배우자 조건",
        CHILD: "자녀 조건",
        PARENT: "부모 조건",
        APPLICATION: "신청 조건",
        SUPPORT: "지원 내용",
        FAMILY: "가족 조건"
    };

    const conditionKeyLabels = {
        ANNUAL_REVENUE: "매출",
        SUPPLY_AMOUNT: "공급가액",
        TOTAL_INCOME_AMOUNT: "총 소득금액",
        INCOME_AMOUNT: "소득 기준",
        EMPLOYEE_COUNT: "직원 수",
        REGULAR_EMPLOYEE_COUNT: "상시근로자 수",
        PLANNED_HIRE_COUNT: "신규 채용 예정 인원",
        NICE_CREDIT_SCORE: "NICE 신용 점수",
        KCB_CREDIT_SCORE: "KCB 신용 점수",
        BUSINESS_YEARS: "업력",
        BUSINESS_AGE: "업력",
        BUSINESS_AGE_YEARS: "업력",
        OPENING_DATE: "개업일",
        AGE: "출생연도 기준 나이",
        REPRESENTATIVE_AGE: "대표자 나이",
        HOUSEHOLD_MEMBER_COUNT: "세대원 수",
        FAMILY_MEMBER_COUNT: "가구원 수",
        HOUSEHOLD_INCOME_AMOUNT: "가구합산 소득",
        CHILD_COUNT: "자녀 수",
        FAMILY_CHILD_COUNT: "자녀 수",
        CHILD_BIRTH_YEAR: "자녀 출생연도",
        PARENT_COUNT: "부모 수",
        FAMILY_PARENT_COUNT: "부모 수",
        PARENT_AGE: "부모 연령",
        SPOUSE_COUNT: "배우자 수",
        SPOUSE_INCOME_AMOUNT: "배우자 소득",
        HAS_CHILD: "자녀 여부",
        HAS_SPOUSE: "배우자 여부",
        HAS_PARENT: "부모 여부",
        CHILD_SCHOOL_AGE_STATUS_CODE: "자녀 학령 상태",
        CHILD_ENROLLMENT_STATUS_CODE: "자녀 재학 상태",
        PARENT_COHABITING: "부모 동거 여부",
        PARENT_SUPPORTED: "부모 부양 여부",
        HAS_INCOME: "소득 여부",
        WORKPLACE_REGION_CODE: "사업장 지역",
        REGION_CODE: "주소지 지역",
        BUSINESS_TYPE_CODE: "사업자 유형",
        COMPANY_STAGE: "사업 상태",
        BUSINESS_STAGE: "사업 상태",
        TAX_TYPE_CODE: "과세 유형",
        HAS_POLICY_FUND_USAGE: "정책자금 이용 이력",
        HAS_GUARANTEE_USAGE: "보증 이용 이력",
        HEALTH_INSURANCE_BASIS_CODE: "건강보험 자격",
        INSURANCE_SUBSCRIBER_TYPE: "건강보험 자격",
        MONTHLY_HEALTH_INSURANCE_PREMIUM: "월 건강보험료",
        RECENT_HEALTH_INSURANCE_PREMIUM: "최근 건강보험료",
        ANNUAL_HEALTH_INSURANCE_PREMIUM: "연 건강보험료",
        NATIONAL_TAX_DELINQUENT: "국세 체납 여부",
        LOCAL_TAX_DELINQUENT: "지방세 체납 여부",
        TAX_PAID_STATUS: "세금 완납 여부",
        IS_HOUSEHOLDER: "세대주 여부"
    };

    const conditionResultLabels = {
        PASS: "충족",
        FAIL: "미충족",
        SKIPPED: "확인 제외",
        REVIEW_REQUIRED: "검토 필요"
    };

    const conditionValueLabels = {
        TRUE: "예",
        FALSE: "아니오",
        YES: "예",
        NO: "아니오",
        UNKNOWN: "잘 모름",
        NONE: "없음",
        HAS_INCOME: "소득 있음",
        SEOUL: "서울",
        BUSAN: "부산",
        DAEGU: "대구",
        INCHEON: "인천",
        GWANGJU: "광주",
        DAEJEON: "대전",
        ULSAN: "울산",
        SEJONG: "세종",
        GYEONGGI: "경기",
        GANGWON: "강원",
        CHUNGBUK: "충북",
        CHUNGNAM: "충남",
        JEONBUK: "전북",
        JEONNAM: "전남",
        GYEONGBUK: "경북",
        GYEONGNAM: "경남",
        JEJU: "제주",
        SOLE_PROPRIETOR: "개인사업자",
        CORPORATION: "법인사업자",
        SIMPLIFIED_TAXPAYER: "간이과세자",
        GENERAL_TAXPAYER: "일반과세자",
        TAX_EXEMPT: "면세사업자",
        PRE_STARTUP: "예비창업",
        EARLY_STARTUP: "창업초기",
        OPERATING: "운영 중",
        SUSPENDED: "휴업",
        CLOSURE_PLANNED: "폐업 예정",
        CLOSED: "폐업",
        RESTART_PREPARING: "재창업 준비",
        WORKPLACE: "직장가입자",
        LOCAL: "지역가입자",
        DEPENDENT: "피부양자"
    };

    const koreanLabel = (labels, value) => {
        if (value == null || String(value).trim() === "") {
            return "-";
        }
        const text = String(value).trim();
        return labels[text] || labels[text.toUpperCase()] || text;
    };

    const conditionValueText = (value) => koreanLabel(conditionValueLabels, value);

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
        card.className = `matching-case-card ${targetClass(item.targetTypeCode)}`;
        card.dataset.matchingCaseId = item.matchingCaseId;

        const head = document.createElement("div");
        head.className = "matching-case-head";
        const title = document.createElement("strong");
        title.textContent = item.announcementTitle || item.matchingCaseCode || "매칭 코드 없음";
        const status = document.createElement("span");
        status.className = "soft-status";
        status.textContent = statusLabels[item.statusCode] || item.statusCode || "상태 없음";
        head.append(title, status);

        const meta = document.createElement("dl");
        meta.className = "matching-case-meta";
        [
            ["기관명", item.agencyName || "기관 미입력"],
            ["지원 주체", targetLabels[item.targetTypeCode] || item.targetTypeCode || "-"],
            ["예상 금액", amountRangeText(item.minAmount, item.maxAmount)],
            ["접수 기간", periodText(item.applicationStartDate, item.applicationEndDate)],
            ["회원", item.memberName || item.memberLoginId || item.memberUserCode],
            ["매칭 단계", stageLabels[item.matchingStageCode] || item.matchingStageCode || "-"],
            ["판단 기준", basisLabels[item.matchingBasisCode] || item.matchingBasisCode || "-"],
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

        const ids = document.createElement("p");
        ids.className = "matching-id-line";
        ids.textContent = `공고 코드: ${item.announcementCode || "-"} · 회원 코드: ${item.memberUserCode || "-"} · 매칭 코드: ${item.matchingCaseCode || "-"}`;

        const resultButton = document.createElement("button");
        resultButton.className = "secondary-action";
        resultButton.type = "button";
        resultButton.dataset.matchingResultsButton = "true";
        resultButton.textContent = "결과 보기";

        const results = document.createElement("div");
        results.className = "matching-results";
        results.hidden = true;
        results.dataset.matchingResults = "true";

        card.append(head, meta, ids);
        if (canOperate) {
            const operateActions = document.createElement("div");
            operateActions.className = "matching-operate-actions";
            if (item.progressCreated || item.statusCode === "PROGRESSED") {
                const progressed = document.createElement("span");
                progressed.className = "passive-action";
                progressed.textContent = "진행 생성됨";
                operateActions.append(progressed);
            } else if (item.statusCode === "MATCHED") {
                const startButton = document.createElement("button");
                startButton.className = "primary-action button-action";
                startButton.type = "button";
                startButton.dataset.progressStartButton = "true";
                startButton.textContent = "이 공고로 진행 시작";
                operateActions.append(startButton);
            }
            if (operateActions.childElementCount > 0) {
                card.append(operateActions);
            }

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
            card.append(form);
        }
        card.append(resultButton, results);
        return card;
    };

    const amountRangeText = (minAmount, maxAmount) => {
        if (minAmount == null && maxAmount == null) {
            return "금액 미입력";
        }
        const formatter = new Intl.NumberFormat("ko-KR");
        const minText = minAmount == null ? "하한 없음" : `${formatter.format(Number(minAmount))}원`;
        const maxText = maxAmount == null ? "상한 없음" : `${formatter.format(Number(maxAmount))}원`;
        return `${minText} ~ ${maxText}`;
    };

    const periodText = (startDate, endDate) => {
        if (!startDate && !endDate) {
            return "기간 미입력";
        }
        return `${startDate || "시작일 미입력"} ~ ${endDate || "마감일 미입력"}`;
    };

    const renderList = (items) => {
        if (!list) {
            return;
        }
        if (!items || items.length === 0) {
            renderEmpty("조회된 최종 매칭 케이스가 없습니다.");
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
        const data = await requestJson(`${finalUrl}?${buildSearchParams().toString()}`, { method: "GET" });
        renderList(data ? data.items : []);
        setMessage("최종 매칭 목록을 조회했습니다.", "success");
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

    const renderLookupButton = (type, title, meta, value, displayValue) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "lookup-result-item";
        button.dataset.lookupSelect = type;
        button.dataset.lookupValue = value;
        button.dataset.lookupDisplay = displayValue || value;
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
                    `${item.announcementCode || "공고 코드 없음"} · ${item.agencyName || "기관 미입력"} · ${dateText}`,
                    item.announcementId,
                    item.announcementCode || item.title || item.announcementId
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
                    `${item.userCode || "회원 코드 없음"} · ${item.loginId || "아이디 없음"} · ${item.statusCode || "상태 없음"}`,
                    item.userId,
                    item.userCode || item.loginId || item.userId
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

    if (createForm) {
        createForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const button = app.querySelector("[data-matching-create-submit]");
            try {
                setBusy(button, true, "생성 중");
                const body = {
                    announcementId: validateUuidText(valueOf(createForm, "announcementId"), "공고 코드"),
                    memberUserId: validateUuidText(valueOf(createForm, "memberUserId"), "회원 코드")
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
    }

    if (candidateForm) {
        candidateForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const button = app.querySelector("[data-matching-candidate-submit]");
            try {
                setBusy(button, true, "생성 중");
                const memberUserId = validateUuidText(valueOf(candidateForm, "memberUserId"), "회원 코드");
                const data = await requestJson(finalRecalculateUrl, {
                    method: "POST",
                    body: JSON.stringify({ memberUserId })
                });
                const searchMemberField = searchForm.querySelector("[name='memberUserId']");
                if (searchMemberField) {
                    searchMemberField.value = memberUserId;
                }
                renderList(data ? data.candidates : []);
                setMessage(`최종 매칭 ${data.createdCount || 0}건을 새로 반영했습니다.`, "success");
            } catch (error) {
                setMessage(error.message, "error");
            } finally {
                setBusy(button, false);
            }
        });
    }

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
            activeLookupForm = openButton.closest("form");
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
            const displayValue = lookupSelect.dataset.lookupDisplay || "";
            const fieldName = type === "announcement" ? "announcementId" : "memberUserId";
            const displayName = type === "announcement" ? "announcementCodeDisplay" : "memberUserCodeDisplay";
            const targetForm = activeLookupForm || createForm || searchForm;
            const idField = targetForm ? targetForm.querySelector(`[name='${fieldName}']`) : null;
            const displayField = targetForm ? targetForm.querySelector(`[name='${displayName}']`) : null;
            if (idField) {
                idField.value = value;
            }
            if (displayField) {
                displayField.value = displayValue;
            }
            closeLookupModal(lookupSelect.closest("[data-lookup-modal]"));
            activeLookupForm = null;
            setMessage(type === "announcement" ? "공고 코드를 선택했습니다." : "회원 코드를 선택했습니다.", "success");
            return;
        }

        const startButton = event.target.closest("[data-progress-start-button]");
        if (startButton) {
            const card = startButton.closest("[data-matching-case-id]");
            const matchingCaseId = card ? card.dataset.matchingCaseId : "";
            if (!matchingCaseId) {
                setMessage("매칭 케이스 ID를 확인할 수 없습니다.", "error");
                return;
            }
            try {
                setBusy(startButton, true, "생성 중");
                await requestJson(progressUrl, {
                    method: "POST",
                    body: JSON.stringify({ matchingCaseId })
                });
                await loadMatchingList();
                setMessage("신청 진행건이 생성되었습니다.", "success");
            } catch (error) {
                setMessage(error.message, "error");
            } finally {
                setBusy(startButton, false);
            }
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
                    const scope = koreanLabel(conditionScopeLabels, item.conditionScopeCode);
                    const key = koreanLabel(conditionKeyLabels, item.conditionKey);
                    const result = koreanLabel(conditionResultLabels, item.resultCode);
                    const basis = conditionValueText(item.basisValue);
                    const required = conditionValueText(item.requiredValue);
                    row.textContent = `${scope} / ${key} / 결과: ${result} / 입력값: ${basis} / 기준값: ${required}`;
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
