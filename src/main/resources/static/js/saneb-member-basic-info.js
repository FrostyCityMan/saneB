(() => {
    const app = document.querySelector("[data-member-basic-info-app], [data-admin-member-basic-info-app]");
    if (!app) {
        return;
    }

    const isAdminApp = app.hasAttribute("data-admin-member-basic-info-app");
    const apiUrl = app.dataset.basicInfoUrl || "";
    const adminBaseUrl = app.dataset.baseUrl || "";
    const addressSearchUrl = app.dataset.addressSearchUrl || "";
    const form = app.querySelector("[data-basic-info-form]");
    const familyList = app.querySelector("[data-family-list]");
    const addFamilyButton = app.querySelector("[data-family-add]");
    const documentTypeSelect = app.querySelector("[data-document-type-select]");
    const addDocumentButton = app.querySelector("[data-document-add]");
    const documentList = app.querySelector("[data-document-list]");
    const submitButton = app.querySelector("[data-basic-info-submit]");
    const message = app.querySelector("[data-basic-info-message]");
    const placeholder = app.querySelector("[data-admin-member-placeholder]");
    const selectedMemberName = app.querySelector("[data-selected-member-name]");
    const selectedMemberLogin = app.querySelector("[data-selected-member-login]");
    const addressModal = app.querySelector("[data-address-modal]");
    const addressKeyword = app.querySelector("[data-address-search-keyword]");
    const addressSubmit = app.querySelector("[data-address-search-submit]");
    const addressMessage = app.querySelector("[data-address-search-message]");
    const addressResults = app.querySelector("[data-address-search-results]");
    const ageOutput = app.querySelector("[data-age-output]");
    const businessYearsOutput = app.querySelector("[data-business-years-output]");

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
    const sidoNameToRegionCode = new Map([
        ["서울특별시", "SEOUL"],
        ["부산광역시", "BUSAN"],
        ["대구광역시", "DAEGU"],
        ["인천광역시", "INCHEON"],
        ["광주광역시", "GWANGJU"],
        ["대전광역시", "DAEJEON"],
        ["울산광역시", "ULSAN"],
        ["세종특별자치시", "SEJONG"],
        ["경기도", "GYEONGGI"],
        ["강원특별자치도", "GANGWON"],
        ["강원도", "GANGWON"],
        ["충청북도", "CHUNGBUK"],
        ["충청남도", "CHUNGNAM"],
        ["전북특별자치도", "JEONBUK"],
        ["전라북도", "JEONBUK"],
        ["전라남도", "JEONNAM"],
        ["경상북도", "GYEONGBUK"],
        ["경상남도", "GYEONGNAM"],
        ["제주특별자치도", "JEJU"]
    ]);
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
    let selectedUserId = null;
    let activeAddressTarget = "member";
    let currentAddressResults = [];
    const otherRestrictionQuestionCode = "OTHER_RESTRICTION";

    const selectApiUrl = () => {
        if (!isAdminApp) {
            return apiUrl;
        }
        return selectedUserId ? `${adminBaseUrl}/${encodeURIComponent(selectedUserId)}` : "";
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
            if (response.status === 401) {
                throw new Error("로그인 상태가 만료되었습니다. 다시 로그인한 뒤 시도하세요.");
            }
            if (response.status === 403) {
                throw new Error("고객 정보를 입력할 권한이 없습니다. 관리자 또는 운영자 권한을 확인하세요.");
            }
            if (!payload) {
                throw new Error("서류 항목을 불러오지 못했습니다. 화면을 새로고침한 뒤 다시 시도하세요.");
            }
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

    const currentYear = () => new Date().getFullYear();

    const updateAgeOutput = () => {
        if (!ageOutput) {
            return;
        }
        const birthYear = numberOrNull(valueOf("birthYear"));
        if (birthYear == null || Number.isNaN(birthYear)) {
            ageOutput.textContent = "나이는 자동으로 계산됩니다.";
            return;
        }
        const age = currentYear() - birthYear;
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
            businessYearsOutput.textContent = "개업일을 확인하세요.";
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

    const setFieldValue = (name, value) => {
        const field = form.querySelector(`[name='${name}']`);
        if (!field) {
            return;
        }
        field.value = value == null ? "" : String(value);
    };

    const setAddressMessage = (text, status = "info") => {
        if (!addressMessage) {
            return;
        }
        addressMessage.textContent = text || "";
        addressMessage.classList.toggle("is-error", status === "error");
        addressMessage.classList.toggle("is-success", status === "success");
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

    const selectRegionCodeBySidoName = (sidoName) => {
        const text = String(sidoName || "").trim();
        return sidoNameToRegionCode.get(text) || null;
    };

    const showAddressModal = (target) => {
        if (!addressModal) {
            setMessage("주소 검색 화면을 찾을 수 없습니다.", "error");
            return;
        }
        if (!addressSearchUrl) {
            setMessage("주소 검색 주소가 설정되지 않았습니다.", "error");
            return;
        }
        activeAddressTarget = target === "business" ? "business" : "member";
        currentAddressResults = [];
        if (addressResults) {
            addressResults.replaceChildren();
        }
        if (addressKeyword) {
            addressKeyword.value = "";
        }
        setAddressMessage("");
        addressModal.hidden = false;
        addressKeyword?.focus();
    };

    const hideAddressModal = () => {
        if (!addressModal) {
            return;
        }
        addressModal.hidden = true;
        currentAddressResults = [];
    };

    const renderAddressResults = (items) => {
        if (!addressResults) {
            return;
        }
        addressResults.replaceChildren();
        if (items.length === 0) {
            const empty = document.createElement("p");
            empty.className = "field-help";
            empty.textContent = "검색 결과가 없습니다. 도로명, 건물명, 지번을 다시 입력하세요.";
            addressResults.append(empty);
            return;
        }
        items.forEach((item, index) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "address-result-button";
            button.dataset.addressResultIndex = String(index);

            const main = document.createElement("strong");
            main.textContent = item.roadAddress || item.roadAddressPart1 || "도로명주소 미제공";
            const meta = document.createElement("span");
            meta.textContent = [
                item.postalCode ? `우편번호 ${item.postalCode}` : "",
                item.jibunAddress || "",
                item.buildingName || ""
            ].filter(Boolean).join(" · ");
            button.append(main, meta);
            addressResults.append(button);
        });
    };

    const searchAddress = async () => {
        const keyword = String(addressKeyword?.value || "").trim();
        if (keyword.length < 2) {
            setAddressMessage("주소 검색어는 두 글자 이상 입력하세요.", "error");
            return;
        }
        const params = new URLSearchParams({
            keyword,
            page: "1",
            size: "10",
            firstSort: "road",
            includeHistory: "false"
        });
        if (addressSubmit) {
            addressSubmit.disabled = true;
            addressSubmit.textContent = "검색 중";
        }
        setAddressMessage("");
        try {
            const data = await requestJson(`${addressSearchUrl}?${params.toString()}`);
            currentAddressResults = Array.isArray(data.items) ? data.items : [];
            renderAddressResults(currentAddressResults);
            setAddressMessage(`${data.totalCount || currentAddressResults.length}건 중 ${currentAddressResults.length}건을 표시합니다.`, "success");
        } catch (error) {
            currentAddressResults = [];
            renderAddressResults(currentAddressResults);
            setAddressMessage(error.message || "주소 검색에 실패했습니다.", "error");
        } finally {
            if (addressSubmit) {
                addressSubmit.disabled = false;
                addressSubmit.textContent = "검색";
            }
        }
    };

    const selectAddressResult = (index) => {
        const item = currentAddressResults[index];
        if (!item) {
            setAddressMessage("선택한 주소를 확인할 수 없습니다.", "error");
            return;
        }
        const prefix = activeAddressTarget === "business" ? "workplace" : "";
        const fieldName = (suffix) => activeAddressTarget === "business"
            ? `${prefix}${suffix}`
            : `${suffix.charAt(0).toLowerCase()}${suffix.slice(1)}`;
        const regionField = activeAddressTarget === "business" ? "workplaceRegionCode" : "regionCode";
        const regionCode = selectRegionCodeBySidoName(item.sidoName);

        if (regionCode) {
            setSelectValue(regionField, regionCode);
        }
        setFieldValue(fieldName("PostalCode"), item.postalCode);
        setFieldValue(fieldName("RoadAddress"), item.roadAddress || item.roadAddressPart1);
        setFieldValue(fieldName("JibunAddress"), item.jibunAddress);
        setFieldValue(fieldName("SidoName"), item.sidoName);
        setFieldValue(fieldName("SigunguName"), item.sigunguName);
        setFieldValue(fieldName("EupmyeondongName"), item.eupmyeondongName);
        setFieldValue(fieldName("LegalDongCode"), item.legalDongCode);
        setFieldValue(fieldName("RoadNameCode"), item.roadNameCode);
        setFieldValue(fieldName("BuildingManagementNo"), item.buildingManagementNo);
        setFieldValue(fieldName("AddressSourceCode"), "JUSO_API");
        hideAddressModal();
        form.querySelector(`[name='${fieldName("DetailAddress")}']`)?.focus();
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
                <label>자녀 재학 상태</label>
                <select name="familyEnrollmentStatusCode">
                    <option value="">선택 안 함</option>
                    <option value="ENROLLED">재학</option>
                    <option value="NOT_ENROLLED">미재학</option>
                    <option value="UNKNOWN">잘 모름</option>
                </select>
            </div>
            <div class="field-block">
                <label>동거 여부</label>
                <select name="familyCohabiting">
                    <option value="">선택 안 함</option>
                    <option value="true">예</option>
                    <option value="false">아니오</option>
                </select>
            </div>
            <div class="field-block">
                <label>부양 여부</label>
                <select name="familySupported">
                    <option value="">선택 안 함</option>
                    <option value="true">예</option>
                    <option value="false">아니오</option>
                </select>
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
        row.querySelector("[name='familySchoolAgeStatusCode']").value = family.schoolAgeStatusCode || "";
        row.querySelector("[name='familyEnrollmentStatusCode']").value = family.enrollmentStatusCode || "";
        row.querySelector("[name='familyCohabiting']").value = family.cohabiting == null ? "" : String(family.cohabiting);
        row.querySelector("[name='familySupported']").value = family.supported == null ? "" : String(family.supported);
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
        setFieldValue("postalCode", data.postalCode);
        setFieldValue("roadAddress", data.roadAddress);
        setFieldValue("jibunAddress", data.jibunAddress);
        setFieldValue("detailAddress", data.detailAddress);
        setFieldValue("sidoName", data.sidoName);
        setFieldValue("sigunguName", data.sigunguName);
        setFieldValue("eupmyeondongName", data.eupmyeondongName);
        setFieldValue("legalDongCode", data.legalDongCode);
        setFieldValue("roadNameCode", data.roadNameCode);
        setFieldValue("buildingManagementNo", data.buildingManagementNo);
        setFieldValue("addressSourceCode", data.addressSourceCode);
        setSelectValue("incomePresenceCode", data.incomePresenceCode);
        setFieldValue("incomeAmount", data.incomeAmount);
        setSelectValue("healthInsuranceBasisCode", data.healthInsuranceBasisCode);

        const business = data.business || {};
        setFieldValue("representativeName", business.representativeName);
        setFieldValue("businessRegistrationNo", business.businessRegistrationNo);
        setFieldValue("businessName", business.businessName);
        setSelectValue("workplaceRegionCode", business.workplaceRegionCode);
        setFieldValue("workplacePostalCode", business.workplacePostalCode);
        setFieldValue("workplaceRoadAddress", business.workplaceRoadAddress);
        setFieldValue("workplaceJibunAddress", business.workplaceJibunAddress);
        setFieldValue("workplaceDetailAddress", business.workplaceDetailAddress);
        setFieldValue("workplaceSidoName", business.workplaceSidoName);
        setFieldValue("workplaceSigunguName", business.workplaceSigunguName);
        setFieldValue("workplaceEupmyeondongName", business.workplaceEupmyeondongName);
        setFieldValue("workplaceLegalDongCode", business.workplaceLegalDongCode);
        setFieldValue("workplaceRoadNameCode", business.workplaceRoadNameCode);
        setFieldValue("workplaceBuildingManagementNo", business.workplaceBuildingManagementNo);
        setFieldValue("workplaceAddressSourceCode", business.workplaceAddressSourceCode);
        setFieldValue("openingDate", business.openingDate);
        setFieldValue("annualRevenue", business.annualRevenue);
        setFieldValue("annualRevenueYear", business.annualRevenueYear);
        setFieldValue("employeeCount", business.employeeCount);
        setFieldValue("regularEmployeeCount", business.regularEmployeeCount);
        setFieldValue("plannedHireCount", business.plannedHireCount);
        setFieldValue("niceCreditScore", business.niceCreditScore);
        setFieldValue("kcbCreditScore", business.kcbCreditScore);
        setSelectValue("hasExistingLoan", business.hasExistingLoan);
        setSelectValue("businessTypeCode", business.businessTypeCode);
        setSelectValue("companyStageCode", business.companyStageCode);
        setFieldValue("ksicCode", business.ksicCode);
        setSelectValue("hasPolicyFundUsage", business.hasPolicyFundUsage);
        setSelectValue("hasGuaranteeUsage", business.hasGuaranteeUsage);
        renderInterviewResponses(data.interviewResponses || []);
        updateAgeOutput();
        updateBusinessYearsOutput();

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

    const renderInterviewResponses = (responses) => {
        const responseByQuestion = new Map(
            (responses || []).map((response) => [response.questionCode, response])
        );
        form.querySelectorAll("[data-interview-question-code]").forEach((field) => {
            const questionCode = field.dataset.interviewQuestionCode;
            const response = responseByQuestion.get(questionCode);
            field.value = response?.answerCode || "";
        });
        const otherRestriction = responseByQuestion.get(otherRestrictionQuestionCode);
        setFieldValue("interviewOtherRestrictionNote", otherRestriction?.note);
    };

    const buildBusinessPayload = () => {
        const payload = {
            businessRegistrationNo: textOrNull(valueOf("businessRegistrationNo")),
            representativeName: textOrNull(valueOf("representativeName")),
            businessName: textOrNull(valueOf("businessName")),
            workplaceRegionCode: textOrNull(valueOf("workplaceRegionCode")),
            workplacePostalCode: textOrNull(valueOf("workplacePostalCode")),
            workplaceRoadAddress: textOrNull(valueOf("workplaceRoadAddress")),
            workplaceJibunAddress: textOrNull(valueOf("workplaceJibunAddress")),
            workplaceDetailAddress: textOrNull(valueOf("workplaceDetailAddress")),
            workplaceSidoName: textOrNull(valueOf("workplaceSidoName")),
            workplaceSigunguName: textOrNull(valueOf("workplaceSigunguName")),
            workplaceEupmyeondongName: textOrNull(valueOf("workplaceEupmyeondongName")),
            workplaceLegalDongCode: textOrNull(valueOf("workplaceLegalDongCode")),
            workplaceRoadNameCode: textOrNull(valueOf("workplaceRoadNameCode")),
            workplaceBuildingManagementNo: textOrNull(valueOf("workplaceBuildingManagementNo")),
            workplaceAddressSourceCode: textOrNull(valueOf("workplaceAddressSourceCode")),
            openingDate: textOrNull(valueOf("openingDate")),
            ksicCode: textOrNull(valueOf("ksicCode")),
            businessTypeCode: textOrNull(valueOf("businessTypeCode")),
            companyStageCode: textOrNull(valueOf("companyStageCode")),
            annualRevenue: numberOrNull(valueOf("annualRevenue")),
            annualRevenueYear: numberOrNull(valueOf("annualRevenueYear")),
            employeeCount: numberOrNull(valueOf("employeeCount")),
            regularEmployeeCount: numberOrNull(valueOf("regularEmployeeCount")),
            plannedHireCount: numberOrNull(valueOf("plannedHireCount")),
            niceCreditScore: numberOrNull(valueOf("niceCreditScore")),
            kcbCreditScore: numberOrNull(valueOf("kcbCreditScore")),
            hasExistingLoan: booleanOrNull(valueOf("hasExistingLoan")),
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
                    schoolAgeStatusCode: textOrNull(rowValueOf(row, "familySchoolAgeStatusCode")),
                    enrollmentStatusCode: textOrNull(rowValueOf(row, "familyEnrollmentStatusCode")),
                    cohabiting: booleanOrNull(rowValueOf(row, "familyCohabiting")),
                    supported: booleanOrNull(rowValueOf(row, "familySupported")),
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

    const buildInterviewPayload = () => {
        const otherRestrictionNote = textOrNull(valueOf("interviewOtherRestrictionNote"));
        return Array.from(form.querySelectorAll("[data-interview-question-code]"))
            .map((field) => {
                const questionCode = field.dataset.interviewQuestionCode;
                const note = questionCode === otherRestrictionQuestionCode ? otherRestrictionNote : null;
                const selectedAnswer = textOrNull(field.value);
                const answerCode = selectedAnswer || (note ? "UNKNOWN" : null);
                if (!questionCode || answerCode == null) {
                    return null;
                }
                return {
                    questionCode,
                    answerCode,
                    note
                };
            })
            .filter(Boolean);
    };

    const buildPayload = () => {
        const incomePresenceCode = textOrNull(valueOf("incomePresenceCode"));
        return {
            birthYear: numberOrNull(valueOf("birthYear")),
            regionCode: textOrNull(valueOf("regionCode")),
            postalCode: textOrNull(valueOf("postalCode")),
            roadAddress: textOrNull(valueOf("roadAddress")),
            jibunAddress: textOrNull(valueOf("jibunAddress")),
            detailAddress: textOrNull(valueOf("detailAddress")),
            sidoName: textOrNull(valueOf("sidoName")),
            sigunguName: textOrNull(valueOf("sigunguName")),
            eupmyeondongName: textOrNull(valueOf("eupmyeondongName")),
            legalDongCode: textOrNull(valueOf("legalDongCode")),
            roadNameCode: textOrNull(valueOf("roadNameCode")),
            buildingManagementNo: textOrNull(valueOf("buildingManagementNo")),
            addressSourceCode: textOrNull(valueOf("addressSourceCode")),
            hasIncome: incomeFlag(incomePresenceCode),
            incomePresenceCode,
            incomeAmount: numberOrNull(valueOf("incomeAmount")),
            healthInsuranceBasisCode: textOrNull(valueOf("healthInsuranceBasisCode")),
            business: buildBusinessPayload(),
            families: buildFamilyPayload(),
            interviewResponses: buildInterviewPayload(),
            documentInputs: buildDocumentPayload()
        };
    };

    const load = async () => {
        const currentApiUrl = selectApiUrl();
        if (!currentApiUrl) {
            return;
        }
        try {
            const data = await requestJson(currentApiUrl);
            renderResponse(data);
            setMessage(isAdminApp ? "선택한 회원의 정보를 불러왔습니다." : "저장된 기본정보를 불러왔습니다.", "success");
        } catch (error) {
            setMessage(error.message || "기본정보를 불러오지 못했습니다.", "error");
        }
    };

    const selectMember = async (button) => {
        selectedUserId = button.dataset.userId || null;
        if (!selectedUserId) {
            setMessage("입력할 회원을 다시 선택하세요.", "error");
            return;
        }
        app.querySelectorAll("[data-member-select]").forEach((candidate) => {
            candidate.classList.toggle("is-selected", candidate === button);
        });
        if (selectedMemberName) {
            selectedMemberName.textContent = button.dataset.userName || "회원명 미입력";
        }
        if (selectedMemberLogin) {
            selectedMemberLogin.textContent = button.dataset.loginId || "아이디 미입력";
        }
        if (form) {
            form.hidden = false;
        }
        if (placeholder) {
            placeholder.hidden = true;
        }
        setMessage("");
        await load();
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

    app.querySelectorAll("[data-address-search-open]").forEach((button) => {
        button.addEventListener("click", () => {
            showAddressModal(button.dataset.addressTarget || "member");
        });
    });

    app.querySelectorAll("[data-address-search-close]").forEach((button) => {
        button.addEventListener("click", hideAddressModal);
    });

    addressModal?.addEventListener("click", (event) => {
        if (event.target === addressModal) {
            hideAddressModal();
        }
    });

    addressSubmit?.addEventListener("click", searchAddress);

    addressKeyword?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            searchAddress();
        }
    });

    form?.querySelector("[name='birthYear']")?.addEventListener("input", updateAgeOutput);
    form?.querySelector("[name='openingDate']")?.addEventListener("input", updateBusinessYearsOutput);

    addressResults?.addEventListener("click", (event) => {
        const button = event.target.closest("[data-address-result-index]");
        if (!button) {
            return;
        }
        selectAddressResult(Number(button.dataset.addressResultIndex));
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const currentApiUrl = selectApiUrl();
        if (!currentApiUrl) {
            setMessage("먼저 입력할 회원을 선택하세요.", "error");
            return;
        }
        setBusy(true);
        setMessage("");
        try {
            const data = await requestJson(currentApiUrl, {
                method: "PUT",
                body: JSON.stringify(buildPayload())
            });
            renderResponse(data);
            setMessage(
                isAdminApp
                    ? "고객 정보를 저장했습니다. 저장된 값은 매칭 후보 생성과 입증 확인에 사용됩니다."
                    : "기본정보를 저장했습니다. 현재 매칭 공고 화면에서 확인할 수 있습니다.",
                "success"
            );
        } catch (error) {
            setMessage(error.message || "기본정보 저장에 실패했습니다.", "error");
        } finally {
            setBusy(false);
        }
    });

    app.querySelectorAll("[data-member-select]").forEach((button) => {
        button.addEventListener("click", () => {
            selectMember(button);
        });
    });

    if (!isAdminApp) {
        load();
    }
})();
