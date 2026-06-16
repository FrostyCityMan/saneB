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
    const conditionUsageLabels = {
        CONDITION_READY: "조건 가능",
        STANDARDIZATION_REQUIRED: "표준화 필요",
        INPUT_ONLY: "입력 전용"
    };
    const conditionKeyLabels = {
        BUSINESS_YEARS: "업력",
        ANNUAL_REVENUE: "매출",
        SUPPLY_AMOUNT: "공급가액",
        TOTAL_INCOME_AMOUNT: "총 소득금액",
        COMPREHENSIVE_INCOME_AMOUNT: "종합소득금액",
        BUSINESS_INCOME_AMOUNT: "사업소득",
        LABOR_INCOME_AMOUNT: "근로소득",
        EMPLOYEE_COUNT: "직원 수",
        REGULAR_EMPLOYEE_COUNT: "상시근로자 수",
        PLANNED_HIRE_COUNT: "신규 채용 예정 인원",
        NICE_CREDIT_SCORE: "NICE 신용 점수",
        KCB_CREDIT_SCORE: "KCB 신용 점수",
        AGE: "출생연도 기준 나이",
        INCOME_AMOUNT: "소득 기준",
        SPOUSE_INCOME_AMOUNT: "배우자 소득",
        HOUSEHOLD_INCOME_AMOUNT: "가구합산 소득",
        HOUSEHOLD_MEMBER_COUNT: "세대원 수",
        FAMILY_MEMBER_COUNT: "가구원 수",
        CHILD_COUNT: "자녀 수",
        FAMILY_CHILD_COUNT: "자녀 수",
        CHILD_BIRTH_YEAR: "자녀 출생연도",
        PARENT_COUNT: "부모 수",
        FAMILY_PARENT_COUNT: "부모 수",
        PARENT_AGE: "부모 연령",
        SPOUSE_COUNT: "배우자 수",
        MONTHLY_HEALTH_INSURANCE_PREMIUM: "월 건강보험료",
        RECENT_HEALTH_INSURANCE_PREMIUM: "최근 건강보험료",
        ANNUAL_HEALTH_INSURANCE_PREMIUM: "연 건강보험료",
        WORKPLACE_REGION_CODE: "사업장 지역",
        BUSINESS_TYPE_CODE: "사업자 유형",
        COMPANY_STAGE: "사업 상태",
        TAX_TYPE_CODE: "과세 유형",
        HAS_POLICY_FUND_USAGE: "중복 수혜 제한 - 정책자금 이용 이력",
        HAS_GUARANTEE_USAGE: "중복 수혜 제한 - 보증 이용 이력",
        NATIONAL_TAX_DELINQUENT: "국세 체납 여부",
        LOCAL_TAX_DELINQUENT: "지방세 체납 여부",
        TAX_PAID_STATUS: "세금 완납 여부",
        REGION_CODE: "주소지 지역",
        HAS_INCOME: "소득 여부",
        HEALTH_INSURANCE_BASIS_CODE: "건강보험 기준",
        INSURANCE_SUBSCRIBER_TYPE: "건강보험 가입 유형",
        IS_HOUSEHOLDER: "세대주 여부",
        DEPENDENT_STATUS: "피부양자 여부",
        WORKPLACE_INSURED_STATUS: "직장가입 여부",
        LOCAL_INSURED_STATUS: "지역가입 여부",
        HAS_SPOUSE: "배우자 여부",
        HAS_CHILD: "자녀 여부",
        HAS_PARENT: "부모 여부",
        CHILD_SCHOOL_AGE_STATUS_CODE: "자녀 학령 상태",
        CHILD_ENROLLMENT_STATUS_CODE: "자녀 재학 상태",
        PARENT_COHABITING: "부모 동거 여부",
        PARENT_SUPPORTED: "부모 부양 여부"
    };
    const booleanConditionKeys = new Set([
        "IS_HOUSEHOLDER",
        "HAS_SPOUSE",
        "HAS_CHILD",
        "HAS_PARENT",
        "NATIONAL_TAX_DELINQUENT",
        "LOCAL_TAX_DELINQUENT",
        "HAS_POLICY_FUND_USAGE",
        "HAS_GUARANTEE_USAGE",
        "DEPENDENT_STATUS",
        "WORKPLACE_INSURED_STATUS",
        "LOCAL_INSURED_STATUS",
        "PARENT_COHABITING",
        "PARENT_SUPPORTED"
    ]);
    const booleanOptionCandidates = [
        { code: "TRUE", label: "예" },
        { code: "FALSE", label: "아니오" }
    ];
    const regionOptionCandidates = [
        { code: "SEOUL", label: "서울" },
        { code: "BUSAN", label: "부산" },
        { code: "DAEGU", label: "대구" },
        { code: "INCHEON", label: "인천" },
        { code: "GWANGJU", label: "광주" },
        { code: "DAEJEON", label: "대전" },
        { code: "ULSAN", label: "울산" },
        { code: "SEJONG", label: "세종" },
        { code: "GYEONGGI", label: "경기" },
        { code: "GANGWON", label: "강원" },
        { code: "CHUNGBUK", label: "충북" },
        { code: "CHUNGNAM", label: "충남" },
        { code: "JEONBUK", label: "전북" },
        { code: "JEONNAM", label: "전남" },
        { code: "GYEONGBUK", label: "경북" },
        { code: "GYEONGNAM", label: "경남" },
        { code: "JEJU", label: "제주" }
    ];
    const optionCandidatesByConditionKey = {
        TAX_TYPE_CODE: [
            { code: "GENERAL_TAXPAYER", label: "일반과세자" },
            { code: "SIMPLIFIED_TAXPAYER", label: "간이과세자" },
            { code: "TAX_EXEMPT", label: "면세사업자" }
        ],
        BUSINESS_TYPE_CODE: [
            { code: "SOLE_PROPRIETOR", label: "개인사업자" },
            { code: "CORPORATION", label: "법인사업자" }
        ],
        COMPANY_STAGE: [
            { code: "PRE_STARTUP", label: "예비창업" },
            { code: "EARLY_STARTUP", label: "초기창업" },
            { code: "OPERATING", label: "운영 중" },
            { code: "SUSPENDED", label: "휴업" },
            { code: "CLOSURE_PLANNED", label: "폐업 예정" },
            { code: "CLOSED", label: "폐업" },
            { code: "RESTART_PREPARING", label: "재창업 준비" }
        ],
        WORKPLACE_REGION_CODE: regionOptionCandidates,
        REGION_CODE: regionOptionCandidates,
        HEALTH_INSURANCE_BASIS_CODE: [
            { code: "WORKPLACE", label: "직장가입자" },
            { code: "LOCAL", label: "지역가입자" },
            { code: "DEPENDENT", label: "피부양자" },
            { code: "UNKNOWN", label: "잘 모름" }
        ],
        INSURANCE_SUBSCRIBER_TYPE: [
            { code: "WORKPLACE", label: "직장가입자" },
            { code: "LOCAL", label: "지역가입자" },
            { code: "DEPENDENT", label: "피부양자" },
            { code: "UNKNOWN", label: "잘 모름" }
        ],
        TAX_PAID_STATUS: [
            { code: "PAID", label: "완납" },
            { code: "DELINQUENT", label: "체납" },
            { code: "UNKNOWN", label: "확인 필요" }
        ],
        HAS_INCOME: [
            { code: "UNKNOWN", label: "잘 모름" },
            { code: "NONE", label: "소득 없음" },
            { code: "HAS_INCOME", label: "소득 있음" }
        ],
        CHILD_SCHOOL_AGE_STATUS_CODE: [
            { code: "PRESCHOOL", label: "미취학" },
            { code: "ELEMENTARY", label: "초등" },
            { code: "MIDDLE_HIGH", label: "중·고등" },
            { code: "COLLEGE", label: "대학생" },
            { code: "NONE", label: "해당 없음" }
        ],
        CHILD_ENROLLMENT_STATUS_CODE: [
            { code: "ENROLLED", label: "재학" },
            { code: "NOT_ENROLLED", label: "비재학" },
            { code: "UNKNOWN", label: "확인 필요" }
        ]
    };
    const approvalStatusLabels = {
        DRAFT: "초안",
        REQUESTED: "승인 요청",
        APPROVED: "승인",
        REJECTED: "반려",
        CANCELED: "취소"
    };
    const manualStatusLabels = {
        NORMAL: "정상 노출",
        PAUSED: "일시중지",
        EARLY_CLOSED: "조기마감",
        SUSPENDED: "접수중단",
        BUDGET_EXHAUSTED: "예산소진",
        CLOSED: "종료",
        HIDDEN: "숨김처리"
    };
    const receptionTypeLabels = {
        BUDGET_EXHAUSTION: "예산 소진형",
        FIRST_COME: "선착순형",
        ALWAYS_OPEN: "상시접수형",
        PERIOD: "기간형",
        EARLY_CLOSE_POSSIBLE: "조기마감 가능형"
    };
    const receptionTypeShortLabels = {
        BUDGET_EXHAUSTION: "예산",
        FIRST_COME: "선착",
        ALWAYS_OPEN: "상시",
        PERIOD: "기간",
        EARLY_CLOSE_POSSIBLE: "조기"
    };
    const documentTypeLabels = {
        BUSINESS_REGISTRATION: "사업자등록증",
        VAT_TAX_BASE: "부가세 과세표준증명원",
        TAX_EXEMPT_INCOME: "면세사업자 수입금액증명원",
        INCOME_CERTIFICATE: "소득금액증명원",
        NATIONAL_TAX_PAID: "국세완납증명서",
        LOCAL_TAX_PAID: "지방세완납증명서",
        RESIDENT_REGISTRATION: "주민등록등본",
        FAMILY_RELATION: "가족관계증명서",
        HEALTH_INSURANCE_PAYMENT: "건강보험료 납부확인서",
        HEALTH_INSURANCE_QUALIFICATION: "건강보험 자격확인서"
    };
    const stepButtonActionLabels = {
        MOVE_NEXT: "다음 단계 이동",
        COMPLETE_STEP: "현재 단계 완료",
        STOP_PROGRESS: "진행 중단"
    };
    const completionConditionLabels = {
        BUTTON_CLICK: "버튼 선택",
        ALL_REQUIRED_DOCUMENTS_CHECKED: "필수 서류 전체 확인",
        REQUIRED_INPUTS_SAVED: "필수 입력값 저장",
        RECEIPT_SAVED: "접수 정보 저장",
        RESULT_SAVED: "최종 결과 저장",
        DOCUMENT_SUBMITTED: "필수 서류 전체 확인",
        STATUS_CONFIRMED: "최종 결과 저장"
    };
    const defaultProgressStepRequests = [
        {
            stepOrder: 1,
            stepName: "안내 발송",
            guideMessage: "현재 사업 정보 기준으로 진행 가능한 항목이 확인되었습니다.",
            actionGuide: "진행 의사를 선택하세요.",
            completionConditionCode: "BUTTON_CLICK",
            nextConditionCode: "진행 의사 확인",
            active: true,
            buttons: [
                { buttonCode: "WANT_TO_PROCEED", buttonLabel: "진행 원함", buttonActionCode: "MOVE_NEXT", sortOrder: 1 },
                { buttonCode: "ALREADY_RECEIVED", buttonLabel: "이미 지원받음", buttonActionCode: "STOP_PROGRESS", sortOrder: 2 },
                { buttonCode: "ALREADY_IN_PROGRESS", buttonLabel: "이미 진행중", buttonActionCode: "STOP_PROGRESS", sortOrder: 3 },
                { buttonCode: "NOT_INTERESTED", buttonLabel: "관심없음", buttonActionCode: "STOP_PROGRESS", sortOrder: 4 }
            ],
            documents: []
        },
        {
            stepOrder: 2,
            stepName: "서류 안내",
            guideMessage: "진행에 필요한 서류를 준비하고 체크리스트를 확인합니다.",
            actionGuide: "필수 서류가 모두 준비되면 서류 준비 완료를 선택하세요.",
            completionConditionCode: "ALL_REQUIRED_DOCUMENTS_CHECKED",
            nextConditionCode: "필수 서류 전체 확인",
            active: true,
            buttons: [
                { buttonCode: "DOCUMENTS_READY", buttonLabel: "서류 준비 완료", buttonActionCode: "MOVE_NEXT", sortOrder: 1 }
            ],
            documents: [
                { documentTypeCode: "BUSINESS_REGISTRATION", required: true, sortOrder: 1 },
                { documentTypeCode: "VAT_TAX_BASE", required: true, sortOrder: 2 },
                { documentTypeCode: "RESIDENT_REGISTRATION", required: true, sortOrder: 3 },
                { documentTypeCode: "FAMILY_RELATION", required: true, sortOrder: 4 }
            ]
        },
        {
            stepOrder: 3,
            stepName: "접수 단계",
            guideMessage: "실제 사업명, 기관명, 접수 방식 등 접수 전 확인이 필요한 정보를 안내합니다.",
            actionGuide: "접수 진행 여부를 선택하세요.",
            completionConditionCode: "BUTTON_CLICK",
            nextConditionCode: "접수 진행 의사 확인",
            active: true,
            buttons: [
                { buttonCode: "START_RECEIPT", buttonLabel: "접수 진행하기", buttonActionCode: "MOVE_NEXT", sortOrder: 1 },
                { buttonCode: "ALREADY_RECEIVED", buttonLabel: "이미 지원받음", buttonActionCode: "STOP_PROGRESS", sortOrder: 2 },
                { buttonCode: "STOP_APPLICATION", buttonLabel: "진행 중단", buttonActionCode: "STOP_PROGRESS", sortOrder: 3 }
            ],
            documents: []
        },
        {
            stepOrder: 4,
            stepName: "접수 진행",
            guideMessage: "실제 접수번호와 접수일을 저장합니다.",
            actionGuide: "접수 정보를 저장한 뒤 접수 완료를 선택하세요.",
            completionConditionCode: "RECEIPT_SAVED",
            nextConditionCode: "접수 정보 저장",
            active: true,
            buttons: [
                { buttonCode: "RECEIPT_DONE", buttonLabel: "접수 완료", buttonActionCode: "MOVE_NEXT", sortOrder: 1 }
            ],
            documents: []
        },
        {
            stepOrder: 5,
            stepName: "접수 완료",
            guideMessage: "접수 완료 후 결과를 기다리는 단계입니다.",
            actionGuide: "결과를 확인할 수 있으면 결과 입력하기를 선택하세요.",
            completionConditionCode: "BUTTON_CLICK",
            nextConditionCode: "결과 입력 가능",
            active: true,
            buttons: [
                { buttonCode: "OPEN_RESULT_INPUT", buttonLabel: "결과 입력하기", buttonActionCode: "MOVE_NEXT", sortOrder: 1 }
            ],
            documents: []
        },
        {
            stepOrder: 6,
            stepName: "결과 입력",
            guideMessage: "최종 결과와 실제 수령 금액을 저장합니다.",
            actionGuide: "결과 정보를 저장한 뒤 결과 저장을 선택하세요.",
            completionConditionCode: "RESULT_SAVED",
            nextConditionCode: "최종 결과 저장",
            active: true,
            buttons: [
                { buttonCode: "SAVE_RESULT", buttonLabel: "결과 저장", buttonActionCode: "MOVE_NEXT", sortOrder: 1 }
            ],
            documents: []
        }
    ];
    const optionFieldTypes = new Set(["SELECT", "RADIO", "MULTI_SELECT"]);
    const numericStandardFieldTypes = new Set(["NUMBER", "AMOUNT", "DATE"]);
    const optionStandardFieldTypes = new Set(["BOOLEAN", "SELECT", "RADIO", "MULTI_SELECT"]);

    const baseUrl = app.dataset.baseUrl;
    const listUrl = app.dataset.listUrl;
    const standardFieldsUrl = app.dataset.standardFieldsUrl;
    const standardCodesUrl = app.dataset.standardCodesUrl;
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
    const statusSummary = app.querySelector("[data-announcement-status-summary]");
    const effectiveStatusLabel = app.querySelector("[data-effective-status-label]");
    const automaticStatusLabel = app.querySelector("[data-automatic-status-label]");
    const manualStatusLabel = app.querySelector("[data-manual-status-label]");
    const receptionTypeLabel = app.querySelector("[data-reception-type-label]");
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
    const stepTemplates = {
        row: stepsList?.querySelector("[data-step-row]")?.cloneNode(true),
        button: stepsList?.querySelector("[data-step-button-row]")?.cloneNode(true),
        document: stepsList?.querySelector("[data-step-document-row]")?.cloneNode(true)
    };
    let defaultStepRequests = [];
    let standardDocumentFields = [];

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

    const targetClass = (targetTypeCode) => {
        const code = String(targetTypeCode || "").toLowerCase();
        return code ? `target-${code}` : "target-unknown";
    };

    const statusClass = (statusCode) => {
        const code = String(statusCode || "").toLowerCase().replaceAll("_", "-");
        return code ? `status-${code}` : "status-unknown";
    };

    const createBadge = (text, ...classes) => {
        const badge = document.createElement("span");
        badge.className = ["announcement-badge", ...classes.filter(Boolean)].join(" ");
        badge.textContent = text || "-";
        return badge;
    };

    const receptionTypeCodeFromOptions = (options) => {
        const item = (options || []).find((option) => option.optionGroupCode === "RECEPTION_TYPE");
        return item?.optionCode || "";
    };

    const receptionTypeText = (receptionTypeCode) => {
        if (!receptionTypeCode) {
            return "선택 안 함";
        }
        return receptionTypeLabels[receptionTypeCode] || receptionTypeCode;
    };

    const renderStatusSummary = (details) => {
        if (!statusSummary) {
            return;
        }
        const manualCode = details?.manualStatusCode || "NORMAL";
        const receptionCode = receptionTypeCodeFromOptions(details?.options);
        if (effectiveStatusLabel) {
            effectiveStatusLabel.textContent = details?.effectiveStatusLabel || manualStatusLabels[manualCode] || manualCode;
        }
        if (automaticStatusLabel) {
            automaticStatusLabel.textContent = details?.automaticStatusLabel || "-";
        }
        if (manualStatusLabel) {
            manualStatusLabel.textContent = manualStatusLabels[manualCode] || manualCode;
        }
        if (receptionTypeLabel) {
            receptionTypeLabel.textContent = receptionTypeText(receptionCode);
        }
        statusSummary.hidden = false;
    };

    const resetStatusSummary = () => {
        if (statusSummary) {
            statusSummary.hidden = true;
        }
        [effectiveStatusLabel, automaticStatusLabel, manualStatusLabel, receptionTypeLabel].forEach((node) => {
            if (node) {
                node.textContent = "-";
            }
        });
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

    const urlWithQuery = (url, params) => {
        const next = new URL(url, window.location.origin);
        Object.entries(params).forEach(([key, value]) => {
            if (value !== null && value !== undefined && String(value).trim() !== "") {
                next.searchParams.set(key, String(value));
            }
        });
        return next.toString();
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

    const buildNumericCondition = (scope, key, comparator, valueNumber, minNumber, maxNumber, unitCode, standardFieldId) => {
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
            standardFieldId: nullIfBlank(standardFieldId),
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

    const optionCandidatesForConditionKey = (conditionKey) => {
        if (!conditionKey) {
            return [];
        }
        if (booleanConditionKeys.has(conditionKey)) {
            return booleanOptionCandidates;
        }
        return optionCandidatesByConditionKey[conditionKey] || [];
    };

    const optionCandidateLabel = (conditionKey, optionCode) => {
        if (!conditionKey || !optionCode) {
            return "";
        }
        const candidate = optionCandidatesForConditionKey(conditionKey)
                .find((option) => option.code === optionCode);
        return candidate ? candidate.label : "";
    };

    const optionValueEmptyLabel = (conditionKey, candidateCount) => {
        if (!conditionKey) {
            return "조건 항목을 먼저 선택하세요";
        }
        if (candidateCount === 0) {
            return "선택 가능한 값이 없습니다";
        }
        return "선택값을 선택하세요";
    };

    const renderOptionValueSelect = (row, selectedValue = "") => {
        const select = row?.querySelector("[name='optionCode']");
        if (!select) {
            return;
        }
        const conditionKey = valueOf(row, "[name='conditionKey']");
        const candidates = optionCandidatesForConditionKey(conditionKey);
        select.replaceChildren();

        const empty = document.createElement("option");
        empty.value = "";
        empty.textContent = optionValueEmptyLabel(conditionKey, candidates.length);
        select.append(empty);

        candidates.forEach((candidate) => {
            const option = document.createElement("option");
            option.value = candidate.code;
            option.textContent = candidate.label;
            select.append(option);
        });

        if (selectedValue && !candidates.some((candidate) => candidate.code === selectedValue)) {
            const fallback = document.createElement("option");
            fallback.value = selectedValue;
            fallback.textContent = "등록되지 않은 선택값";
            fallback.dataset.fallbackOption = "true";
            select.append(fallback);
        }

        select.value = selectedValue || "";
    };

    const clearOptionMemoTouched = (row) => {
        const memo = row?.querySelector("[name='optionText']");
        if (memo) {
            delete memo.dataset.touched;
        }
    };

    const setOptionMemoFromSelection = (row, force = false) => {
        const select = row?.querySelector("[name='optionCode']");
        const memo = row?.querySelector("[name='optionText']");
        if (!select || !memo) {
            return;
        }
        if (!force && memo.dataset.touched === "true") {
            return;
        }
        const selected = select.selectedOptions && select.selectedOptions.length > 0 ? select.selectedOptions[0] : null;
        const isFallback = selected?.dataset.fallbackOption === "true";
        memo.value = select.value && selected && !isFallback ? selected.textContent : "";
    };

    const resetOptionValueForConditionKey = (row) => {
        renderOptionValueSelect(row, "");
        setFieldValue(row, "[name='optionText']", "");
        clearOptionMemoTouched(row);
    };

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
        row.querySelectorAll("[data-ksic-selected-label]").forEach((label) => {
            label.textContent = "업태/종목 텍스트를 그대로 비교하지 않고 KSIC 코드 기준으로 판단합니다.";
        });
        if (row.matches("[data-option-condition-row]")) {
            renderOptionValueSelect(row, "");
            clearOptionMemoTouched(row);
        }
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
        populateConditionStandardFieldSelects();
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
        populateConditionStandardFieldSelects();
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
                    valueOf(row, "[name='unitCode']"),
                    valueOf(row, "[name='standardFieldId']")
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
                standardFieldId: nullIfBlank(valueOf(row, "[name='standardFieldId']")),
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
                standardFieldId: nullIfBlank(valueOf(row, "[name='standardFieldId']")),
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
    const stepButtonRows = (row) => row ? Array.from(row.querySelectorAll("[data-step-button-row]")) : [];
    const stepDocumentRows = (row) => row ? Array.from(row.querySelectorAll("[data-step-document-row]")) : [];

    const buildStepRequestFromRow = (row, stepOrder) => {
        const stepName = valueOf(row, "[name='stepName']");
        if (!stepName) {
            return null;
        }

        const buttons = [];
        stepButtonRows(row).forEach((buttonRow) => {
            const buttonLabel = valueOf(buttonRow, "[name='buttonLabel']");
            const explicitButtonCode = valueOf(buttonRow, "[name='buttonCode']");
            if (!buttonLabel && !explicitButtonCode) {
                return;
            }
            if (!buttonLabel) {
                throw new Error("단계 버튼 이름을 입력해 주세요.");
            }
            buttons.push({
                buttonCode: explicitButtonCode || `STEP_${stepOrder}_BUTTON_${buttons.length + 1}`,
                buttonLabel,
                buttonActionCode: valueOf(buttonRow, "[name='buttonActionCode']") || "MOVE_NEXT",
                nextStepId: nullIfBlank(valueOf(buttonRow, "[name='nextStepId']")),
                sortOrder: buttons.length + 1
            });
        });

        const documents = [];
        stepDocumentRows(row).forEach((documentRow) => {
            const documentTypeCode = valueOf(documentRow, "[name='stepDocumentTypeCode']");
            if (!documentTypeCode) {
                return;
            }
            documents.push({
                documentTypeCode,
                required: Boolean(documentRow.querySelector("[name='stepDocumentRequired']")?.checked),
                sortOrder: documents.length + 1
            });
        });

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

    const fallbackOptionLabel = (field, value) => {
        if (field.name === "conditionKey") {
            return conditionKeyLabels[value] || "미등록 조건 항목";
        }
        if (field.name === "optionCode") {
            return "등록되지 않은 선택값";
        }
        if (field.name === "buttonActionCode") {
            return stepButtonActionLabels[value] || value;
        }
        if (field.name === "completionConditionCode") {
            return completionConditionLabels[value] || value;
        }
        if (field.name === "stepDocumentTypeCode") {
            return documentTypeLabels[value] || value;
        }
        if (field.name === "nextStepId") {
            return "저장된 이동 단계";
        }
        return value;
    };

    const setFieldValue = (row, selector, value) => {
        const field = row.querySelector(selector);
        if (field) {
            if (field.tagName === "SELECT") {
                Array.from(field.options)
                        .filter((option) => option.dataset.fallbackOption === "true" && option.value !== value)
                        .forEach((option) => option.remove());
            }
            if (field.tagName === "SELECT" && value && !Array.from(field.options).some((option) => option.value === value)) {
                const option = document.createElement("option");
                option.value = value;
                option.textContent = fallbackOptionLabel(field, value);
                option.dataset.fallbackOption = "true";
                field.append(option);
            }
            field.value = value ?? "";
        }
    };

    const selectStandardField = (standardFieldId) => {
        if (!standardFieldId) {
            return null;
        }
        return standardDocumentFields.find((field) => field.standardFieldId === standardFieldId) || null;
    };

    const standardFieldText = (field) => {
        const documentLabel = documentTypeLabels[field.documentTypeCode] || field.documentTypeCode || "서류";
        const conditionUsageCode = field.conditionUsageCode || (field.conditionEligible ? "CONDITION_READY" : "INPUT_ONLY");
        const conditionLabel = conditionUsageLabels[conditionUsageCode] || "확인 필요";
        return `${documentLabel} · ${field.fieldLabel || field.fieldKey} (${conditionLabel})`;
    };

    const populateStandardFieldSelect = (select, selectedValue, filter, emptyLabel) => {
        if (!select) {
            return;
        }
        const currentValue = selectedValue ?? select.value ?? "";
        select.replaceChildren();

        const empty = document.createElement("option");
        empty.value = "";
        empty.textContent = emptyLabel;
        select.append(empty);

        standardDocumentFields
                .filter((field) => field.selectable !== false)
                .filter(filter)
                .forEach((field) => {
                    const option = document.createElement("option");
                    option.value = field.standardFieldId;
                    option.textContent = standardFieldText(field);
                    select.append(option);
                });

        if (currentValue && !Array.from(select.options).some((option) => option.value === currentValue)) {
            const fallback = document.createElement("option");
            fallback.value = currentValue;
            fallback.textContent = "불러올 수 없는 표준 항목";
            select.append(fallback);
        }
        select.value = currentValue || "";
    };

    const populateConditionStandardFieldSelects = () => {
        numericRows().forEach((row) => {
            populateStandardFieldSelect(
                    row.querySelector("[data-numeric-standard-field]"),
                    valueOf(row, "[name='standardFieldId']"),
                    (field) => numericStandardFieldTypes.has(field.fieldTypeCode),
                    "기본정보 항목 직접 선택"
            );
        });
        optionRows().forEach((row) => {
            populateStandardFieldSelect(
                    row.querySelector("[data-option-standard-field]"),
                    valueOf(row, "[name='standardFieldId']"),
                    (field) => optionStandardFieldTypes.has(field.fieldTypeCode),
                    "기본정보 항목 직접 선택"
            );
        });
        documentRows().forEach((row) => {
            const documentTypeCode = valueOf(row, "[name='documentTypeCode']");
            populateStandardFieldSelect(
                    row.querySelector("[data-document-standard-field]"),
                    valueOf(row, "[name='standardFieldId']"),
                    (field) => !documentTypeCode || field.documentTypeCode === documentTypeCode,
                    "서류 전체 요청"
            );
        });
    };

    const populateDynamicStandardFieldSelects = () => {
        app.querySelectorAll("[data-dynamic-requirement-row]").forEach((row) => {
            populateStandardFieldSelect(
                    row.querySelector("[data-dynamic-standard-field]"),
                    valueOf(row, "[name='standardFieldId']"),
                    () => true,
                    "직접 입력"
            );
        });
    };

    const populateAllStandardFieldSelects = () => {
        populateConditionStandardFieldSelects();
        populateDynamicStandardFieldSelects();
    };

    const conditionKeyForStandardField = (field) => {
        if (field.fieldKey === "OPENING_DATE") {
            return "BUSINESS_YEARS";
        }
        return field.fieldKey || "";
    };

    const applyStandardFieldToConditionRow = (row, field, optionCondition = false) => {
        if (!row || !field) {
            return;
        }
        setFieldValue(row, "[name='conditionScopeCode']", field.scopeCode || selectedTargetCode());
        const conditionKey = conditionKeyForStandardField(field);
        setFieldValue(row, "[name='conditionKey']", conditionKey);
        if (optionCondition) {
            clearOptionMemoTouched(row);
            const defaultOptionCode = field.fieldTypeCode === "BOOLEAN" ? "TRUE" : "";
            renderOptionValueSelect(row, defaultOptionCode);
            setOptionMemoFromSelection(row, true);
        }
    };

    const applyStandardFieldToDocumentRow = (row, field) => {
        if (!row || !field) {
            return;
        }
        setFieldValue(row, "[name='documentTypeCode']", field.documentTypeCode || "");
        populateConditionStandardFieldSelects();
        setFieldValue(row, "[name='standardFieldId']", field.standardFieldId || "");
    };

    const applyStandardFieldToDynamicRequirementRow = (row, field) => {
        if (!row || !field) {
            return;
        }
        setFieldValue(row, "[name='fieldKey']", field.fieldKey || "");
        setFieldValue(row, "[name='fieldLabel']", field.fieldLabel || "");
        setFieldValue(row, "[name='fieldTypeCode']", field.fieldTypeCode || "TEXT");
        setFieldValue(row, "[name='scopeCode']", field.scopeCode || "APPLICATION");
        setFieldValue(row, "[name='helpText']", field.helpText || "");
        const requiredField = row.querySelector("[name='required']");
        if (requiredField) {
            requiredField.checked = Boolean(field.requiredDefault);
        }
        row.querySelector("[name='fieldTypeCode']")?.dispatchEvent(new Event("change", { bubbles: true }));
    };

    const loadStandardDocumentFields = async () => {
        if (!standardFieldsUrl) {
            return;
        }
        const data = await requestJson(standardFieldsUrl, { method: "GET" });
        standardDocumentFields = Array.isArray(data) ? data : [];
        populateAllStandardFieldSelects();
    };

    const updateKsicSelectedLabel = (row) => {
        if (!row) {
            return;
        }
        const select = row.querySelector("[data-ksic-select]");
        const label = row.querySelector("[data-ksic-selected-label]");
        if (!select || !label) {
            return;
        }
        const selected = select.selectedOptions && select.selectedOptions.length > 0 ? select.selectedOptions[0] : null;
        label.textContent = selected && selected.value
                ? `선택된 업종: ${selected.textContent}`
                : "업태/종목 텍스트를 그대로 비교하지 않고 KSIC 코드 기준으로 판단합니다.";
    };

    const ensureKsicOption = (row, code, labelText) => {
        if (!row || !code) {
            return;
        }
        const select = row.querySelector("[data-ksic-select]");
        if (!select) {
            return;
        }
        if (!Array.from(select.options).some((option) => option.value === code)) {
            const option = document.createElement("option");
            option.value = code;
            option.textContent = labelText || code;
            select.append(option);
        }
        select.value = code;
        updateKsicSelectedLabel(row);
    };

    const renderKsicOptions = (row, codes) => {
        const select = row?.querySelector("[data-ksic-select]");
        if (!select) {
            return;
        }
        const currentValue = select.value;
        select.replaceChildren();
        const empty = document.createElement("option");
        empty.value = "";
        empty.textContent = "KSIC 코드를 검색해 선택하세요";
        select.append(empty);
        codes.forEach((code) => {
            const option = document.createElement("option");
            option.value = code.code;
            option.textContent = `${code.code} · ${code.codeName}`;
            select.append(option);
        });
        if (currentValue && !Array.from(select.options).some((option) => option.value === currentValue)) {
            ensureKsicOption(row, currentValue, currentValue);
        } else {
            select.value = currentValue || "";
            updateKsicSelectedLabel(row);
        }
    };

    const searchKsicCodes = async (button) => {
        const row = button.closest("[data-industry-condition-row]");
        if (!row) {
            return;
        }
        const keyword = valueOf(row, "[name='ksicKeyword']");
        if (!keyword) {
            setMessage("검색할 업종명 또는 KSIC 코드를 입력해 주세요.", "error");
            return;
        }
        if (!standardCodesUrl) {
            setMessage("표준 코드 조회 주소가 설정되지 않았습니다.", "error");
            return;
        }
        try {
            setBusy(button, true, "검색 중");
            const data = await requestJson(urlWithQuery(standardCodesUrl, {
                groupCode: "KSIC_11",
                keyword,
                page: 1,
                size: 20
            }), { method: "GET" });
            const codes = Array.isArray(data?.items) ? data.items : [];
            renderKsicOptions(row, codes);
            setMessage(codes.length > 0 ? "KSIC 검색 결과를 불러왔습니다." : "검색 결과가 없습니다.", codes.length > 0 ? "success" : "info");
        } catch (error) {
            setMessage(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    };

    const createStepButtonRow = (button = {}) => {
        const source = stepTemplates.button;
        if (!source) {
            return null;
        }
        const row = source.cloneNode(true);
        setFieldValue(row, "[name='buttonLabel']", button.buttonLabel || "");
        setFieldValue(row, "[name='buttonCode']", button.buttonCode || "");
        setFieldValue(row, "[name='buttonActionCode']", button.buttonActionCode || "MOVE_NEXT");
        setFieldValue(row, "[name='nextStepId']", button.nextStepId || "");
        return row;
    };

    const createStepDocumentRow = (documentRequest = {}) => {
        const source = stepTemplates.document;
        if (!source) {
            return null;
        }
        const row = source.cloneNode(true);
        setFieldValue(row, "[name='stepDocumentTypeCode']", documentRequest.documentTypeCode || "");
        const requiredField = row.querySelector("[name='stepDocumentRequired']");
        if (requiredField) {
            requiredField.checked = Boolean(documentRequest.required);
        }
        return row;
    };

    const normalizeStepButtonRows = (row) => {
        const buttonRows = stepButtonRows(row);
        buttonRows.forEach((buttonRow) => {
            const removeButton = buttonRow.querySelector("[data-step-button-remove]");
            if (removeButton) {
                removeButton.disabled = buttonRows.length <= 1;
            }
        });
    };

    const normalizeStepDocumentRows = (row) => {
        const documentRowsForStep = stepDocumentRows(row);
        documentRowsForStep.forEach((documentRow) => {
            const removeButton = documentRow.querySelector("[data-step-document-remove]");
            if (removeButton) {
                removeButton.disabled = documentRowsForStep.length <= 1;
            }
        });
    };

    const populateStepNextOptions = () => {
        const rows = stepRows();
        rows.forEach((row) => {
            stepButtonRows(row).forEach((buttonRow) => {
                const select = buttonRow.querySelector("[name='nextStepId']");
                if (!select) {
                    return;
                }
                const currentValue = select.value;
                select.replaceChildren();

                const empty = document.createElement("option");
                empty.value = "";
                empty.textContent = "다음 순서 단계";
                select.append(empty);

                rows.forEach((targetRow, index) => {
                    const targetStepId = targetRow.dataset.stepId || "";
                    if (!targetStepId || targetRow === row) {
                        return;
                    }
                    const option = document.createElement("option");
                    option.value = targetStepId;
                    option.textContent = `${index + 1}단계 · ${valueOf(targetRow, "[name='stepName']") || "단계명 미입력"}`;
                    select.append(option);
                });

                if (currentValue && !Array.from(select.options).some((option) => option.value === currentValue)) {
                    const fallback = document.createElement("option");
                    fallback.value = currentValue;
                    fallback.textContent = "저장된 이동 단계";
                    fallback.dataset.fallbackOption = "true";
                    select.append(fallback);
                }
                select.value = currentValue || "";
            });
        });
    };

    const renderStepButtonRows = (row, buttons = []) => {
        const list = row.querySelector("[data-step-button-list]");
        if (!list) {
            return;
        }
        list.replaceChildren();
        const sourceButtons = buttons.length > 0 ? buttons : [{}];
        sourceButtons.forEach((button) => {
            const buttonRow = createStepButtonRow(button);
            if (buttonRow) {
                list.append(buttonRow);
            }
        });
        normalizeStepButtonRows(row);
    };

    const renderStepDocumentRows = (row, documents = []) => {
        const list = row.querySelector("[data-step-document-list]");
        if (!list) {
            return;
        }
        list.replaceChildren();
        const sourceDocuments = documents.length > 0 ? documents : [{}];
        sourceDocuments.forEach((documentRequest) => {
            const documentRow = createStepDocumentRow(documentRequest);
            if (documentRow) {
                list.append(documentRow);
            }
        });
        normalizeStepDocumentRows(row);
    };

    const clearStepRow = (row) => {
        row.dataset.stepId = "";
        setFieldValue(row, "[name='stepName']", "");
        setFieldValue(row, "[name='completionConditionCode']", "BUTTON_CLICK");
        setFieldValue(row, "[name='guideMessage']", "");
        setFieldValue(row, "[name='actionGuide']", "");
        setFieldValue(row, "[name='nextConditionCode']", "");
        const activeField = row.querySelector("[name='stepActive']");
        if (activeField) {
            activeField.checked = true;
        }
        renderStepButtonRows(row, [{}]);
        renderStepDocumentRows(row, [{}]);
    };

    const applyStepToRow = (row, step) => {
        row.dataset.stepId = step.stepId || "";
        setFieldValue(row, "[name='stepName']", step.stepName || "");
        setFieldValue(row, "[name='completionConditionCode']", step.completionConditionCode || "BUTTON_CLICK");
        setFieldValue(row, "[name='guideMessage']", step.guideMessage || "");
        setFieldValue(row, "[name='actionGuide']", step.actionGuide || "");
        setFieldValue(row, "[name='nextConditionCode']", step.nextConditionCode || "");
        const activeField = row.querySelector("[name='stepActive']");
        if (activeField) {
            activeField.checked = step.active !== false;
        }
        renderStepButtonRows(row, step.buttons || []);
        renderStepDocumentRows(row, step.documents || []);
    };

    const normalizeStepRows = () => {
        stepRows().forEach((row, index, rows) => {
            const order = row.querySelector(".step-order");
            if (order) {
                order.textContent = String(index + 1);
            }
            const removeButton = row.querySelector("[data-step-remove]");
            if (removeButton) {
                removeButton.disabled = rows.length <= 1;
            }
            const upButton = row.querySelector("[data-step-move-up]");
            if (upButton) {
                upButton.disabled = index === 0;
            }
            const downButton = row.querySelector("[data-step-move-down]");
            if (downButton) {
                downButton.disabled = index === rows.length - 1;
            }
            normalizeStepButtonRows(row);
            normalizeStepDocumentRows(row);
        });
        populateStepNextOptions();
    };

    const renderStepRows = (steps = []) => {
        if (!stepsList) {
            return;
        }
        const source = stepTemplates.row || stepRows()[0];
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
        const source = stepTemplates.row || stepRows()[0];
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
        button.className = `announcement-list-item ${targetClass(item.targetTypeCode)}`;
        button.dataset.announcementId = item.announcementId;

        const meta = document.createElement("span");
        meta.className = "list-meta";
        const approvalLabel = approvalStatusLabels[item.approvalStatusCode] || item.approvalStatusCode || "초안";
        meta.textContent = `${item.announcementCode || "공고 코드 없음"} · ${approvalLabel}`;

        const title = document.createElement("strong");
        title.textContent = item.title || "제목 없음";

        const sub = document.createElement("small");
        const dateText = item.applicationStartDate && item.applicationEndDate
                ? `${item.applicationStartDate} ~ ${item.applicationEndDate}`
                : "신청 기간 미입력";
        sub.textContent = `${item.agencyName || "기관 미입력"} · ${dateText}`;

        const badges = document.createElement("div");
        badges.className = "announcement-list-badges";
        badges.append(createBadge(targetLabels[item.targetTypeCode] || item.targetTypeCode || "대상 없음", `is-${targetClass(item.targetTypeCode)}`));
        badges.append(createBadge(
                item.effectiveStatusLabel || manualStatusLabels[item.manualStatusCode] || item.manualStatusCode || "상태 없음",
                `is-${statusClass(item.effectiveStatusCode || item.manualStatusCode)}`
        ));
        if (item.receptionTypeCode) {
            badges.append(createBadge(
                    receptionTypeShortLabels[item.receptionTypeCode] || receptionTypeLabels[item.receptionTypeCode] || item.receptionTypeCode,
                    "is-reception-type"
            ));
        }

        button.append(meta, title, sub, badges);
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

        const standardFieldBlock = document.createElement("div");
        standardFieldBlock.className = "field-block span-2";
        const standardFieldLabel = document.createElement("label");
        standardFieldLabel.textContent = "표준 서류 항목";
        const standardFieldSelect = document.createElement("select");
        standardFieldSelect.name = "standardFieldId";
        standardFieldSelect.dataset.dynamicStandardField = "true";
        const standardFieldHelp = document.createElement("small");
        standardFieldHelp.textContent = "서류 항목을 선택하면 아래 입력 항목이 자동으로 채워집니다. 조건으로 사용할 수 없는 항목도 요청 입력으로는 사용할 수 있습니다.";
        standardFieldBlock.append(standardFieldLabel, standardFieldSelect, standardFieldHelp);

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

        row.append(standardFieldBlock, fieldKeyBlock, labelBlock, fieldTypeBlock, scopeBlock, sortBlock, flags, optionBlock, helpBlock, actions);
        populateStandardFieldSelect(
                standardFieldSelect,
                requirement.standardFieldId || "",
                () => true,
                "직접 입력"
        );

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
                    standardFieldId: nullIfBlank(valueOf(row, "[name='standardFieldId']")),
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
                    ensureKsicOption(row, condition.ksicCode || "", condition.ksicCode || "");
                }
        );
        renderConditionRows(
                numericConditionList,
                conditionTemplates.numeric,
                "[data-numeric-condition-row]",
                "[data-numeric-condition-remove]",
                conditions?.numericConditions || [],
                (row, condition) => {
                    setFieldValue(row, "[name='standardFieldId']", condition.standardFieldId || "");
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
                    setFieldValue(row, "[name='standardFieldId']", condition.standardFieldId || "");
                    setFieldValue(row, "[name='conditionScopeCode']", condition.conditionScopeCode || targetCode);
                    const conditionKey = condition.conditionKey || "";
                    const optionCode = condition.optionCode || "";
                    setFieldValue(row, "[name='conditionKey']", conditionKey);
                    renderOptionValueSelect(row, optionCode);
                    setFieldValue(row, "[name='optionText']", condition.optionText || optionCandidateLabel(conditionKey, optionCode));
                    if (condition.optionText) {
                        const optionText = row.querySelector("[name='optionText']");
                        if (optionText) {
                            optionText.dataset.touched = "true";
                        }
                    }
                }
        );
        renderConditionRows(
                documentRequirementList,
                conditionTemplates.document,
                "[data-document-requirement-row]",
                "[data-document-requirement-remove]",
                conditions?.documentRequirements || [],
                (row, document) => {
                    setFieldValue(row, "[name='standardFieldId']", document.standardFieldId || "");
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
        renderStatusSummary(details);
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
        resetStatusSummary();
        setDynamicRequirementSummary("공고 저장 후 설정");
        updateTargetUi();
        setMessage("신규 공고 입력 상태입니다.");
    };

    app.addEventListener("change", (event) => {
        if (event.target.matches("input[name='targetTypeCode']")) {
            updateTargetUi();
            populateConditionStandardFieldSelects();
            return;
        }
        if (event.target.matches("[data-option-condition-row] [name='conditionKey']")) {
            resetOptionValueForConditionKey(event.target.closest("[data-option-condition-row]"));
            return;
        }
        if (event.target.matches("[data-option-condition-row] [name='optionCode']")) {
            setOptionMemoFromSelection(event.target.closest("[data-option-condition-row]"));
            return;
        }
        if (event.target.matches("[data-numeric-standard-field]")) {
            applyStandardFieldToConditionRow(
                    event.target.closest("[data-numeric-condition-row]"),
                    selectStandardField(event.target.value),
                    false
            );
            return;
        }
        if (event.target.matches("[data-option-standard-field]")) {
            applyStandardFieldToConditionRow(
                    event.target.closest("[data-option-condition-row]"),
                    selectStandardField(event.target.value),
                    true
            );
            return;
        }
        if (event.target.matches("[data-document-standard-field]")) {
            applyStandardFieldToDocumentRow(
                    event.target.closest("[data-document-requirement-row]"),
                    selectStandardField(event.target.value)
            );
            return;
        }
        if (event.target.matches("[data-document-requirement-row] [name='documentTypeCode']")) {
            const row = event.target.closest("[data-document-requirement-row]");
            if (row) {
                setFieldValue(row, "[name='standardFieldId']", "");
            }
            populateConditionStandardFieldSelects();
            return;
        }
        if (event.target.matches("[data-dynamic-standard-field]")) {
            applyStandardFieldToDynamicRequirementRow(
                    event.target.closest("[data-dynamic-requirement-row]"),
                    selectStandardField(event.target.value)
            );
            return;
        }
        if (event.target.matches("[data-ksic-select]")) {
            updateKsicSelectedLabel(event.target.closest("[data-industry-condition-row]"));
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

        if (event.target.matches("[data-step-move-up]")) {
            event.preventDefault();
            const row = event.target.closest("[data-step-row]");
            const previous = row?.previousElementSibling;
            if (row && previous) {
                stepsList.insertBefore(row, previous);
                normalizeStepRows();
            }
            return;
        }

        if (event.target.matches("[data-step-move-down]")) {
            event.preventDefault();
            const row = event.target.closest("[data-step-row]");
            const next = row?.nextElementSibling;
            if (row && next) {
                stepsList.insertBefore(next, row);
                normalizeStepRows();
            }
            return;
        }

        if (event.target.matches("[data-step-button-add]")) {
            event.preventDefault();
            const row = event.target.closest("[data-step-row]");
            const list = row?.querySelector("[data-step-button-list]");
            const buttonRow = createStepButtonRow({});
            if (list && buttonRow) {
                list.append(buttonRow);
                normalizeStepButtonRows(row);
                populateStepNextOptions();
                buttonRow.querySelector("[name='buttonLabel']")?.focus();
            }
            return;
        }

        if (event.target.matches("[data-step-button-remove]")) {
            event.preventDefault();
            const row = event.target.closest("[data-step-row]");
            const buttonRows = stepButtonRows(row);
            const buttonRow = event.target.closest("[data-step-button-row]");
            if (buttonRows.length <= 1) {
                setFieldValue(buttonRow, "[name='buttonLabel']", "");
                setFieldValue(buttonRow, "[name='buttonCode']", "");
                setFieldValue(buttonRow, "[name='buttonActionCode']", "MOVE_NEXT");
                setFieldValue(buttonRow, "[name='nextStepId']", "");
            } else {
                buttonRow?.remove();
            }
            normalizeStepButtonRows(row);
            populateStepNextOptions();
            return;
        }

        if (event.target.matches("[data-step-document-add]")) {
            event.preventDefault();
            const row = event.target.closest("[data-step-row]");
            const list = row?.querySelector("[data-step-document-list]");
            const documentRow = createStepDocumentRow({});
            if (list && documentRow) {
                list.append(documentRow);
                normalizeStepDocumentRows(row);
                documentRow.querySelector("[name='stepDocumentTypeCode']")?.focus();
            }
            return;
        }

        if (event.target.matches("[data-step-document-remove]")) {
            event.preventDefault();
            const row = event.target.closest("[data-step-row]");
            const documentRowsForStep = stepDocumentRows(row);
            const documentRow = event.target.closest("[data-step-document-row]");
            if (documentRowsForStep.length <= 1) {
                setFieldValue(documentRow, "[name='stepDocumentTypeCode']", "");
                const requiredField = documentRow?.querySelector("[name='stepDocumentRequired']");
                if (requiredField) {
                    requiredField.checked = false;
                }
            } else {
                documentRow?.remove();
            }
            normalizeStepDocumentRows(row);
            return;
        }

        if (event.target.matches("[data-industry-condition-add]")) {
            event.preventDefault();
            appendConditionRow(
                    industryConditionList,
                    conditionTemplates.industry,
                    "[data-industry-condition-row]",
                    "[data-industry-condition-remove]"
            )?.querySelector("[name='ksicKeyword']")?.focus();
            return;
        }

        if (event.target.matches("[data-ksic-search]")) {
            event.preventDefault();
            await searchKsicCodes(event.target);
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

    app.addEventListener("input", (event) => {
        if (event.target.matches("[data-option-condition-row] [name='optionText']")) {
            event.target.dataset.touched = "true";
        }
        if (event.target.matches("[data-step-row] [name='stepName']")) {
            populateStepNextOptions();
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
    optionRows().forEach((row) => renderOptionValueSelect(row, valueOf(row, "[name='optionCode']")));
    normalizeConditionRows(documentRequirementList, "[data-document-requirement-row]", "[data-document-requirement-remove]");
    defaultStepRequests = defaultProgressStepRequests;
    renderStepRows(defaultStepRequests);
    updateTargetUi();
    updateApprovalUi("");
    renderDynamicRequirements([]);
    loadStandardDocumentFields().catch((error) => {
        setMessage(error.message, "error");
    });
    loadAnnouncementList();
})();
