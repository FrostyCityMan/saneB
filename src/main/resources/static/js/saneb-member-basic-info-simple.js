/* 일반 사용자 전용 서류 입력 뷰. 기존 관리자 입력 화면은 변경하지 않는다. */
(() => {
    "use strict";
    const core = window.SanebMemberBasicInfo;
    const node = (tag, className, text) => {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (text) element.textContent = text;
        return element;
    };
    const create = ({ app, form, list, familySection, renderField, onHealthChange, onDirty }) => {
        let catalog = [];
        let choices = {};
        let choiceGroups = [];
        const radioGroup = (kind, title, options, initial) => {
            choices[kind] = { ...initial, dirty: false };
            const group = node("fieldset", "basic-info-single-choice");
            group.append(node("legend", "", title));
            const controls = node("div", "basic-info-radio-options");
            const warning = node("p", "field-help basic-info-choice-warning");
            const conflictMessage = "기존에 저장된 값이 서로 다릅니다. 서류를 확인해 하나를 선택하거나, 모르면 선택을 해제하세요.";
            warning.textContent = initial.conflict ? conflictMessage : initial.unknown ? "기존 값은 ‘잘 모름’입니다. 확인된 경우에만 선택하세요." : "모르면 선택하지 않아도 됩니다.";
            warning.id = `basic-choice-${kind}-help`;
            group.setAttribute("aria-describedby", warning.id);
            const update = value => {
                choices[kind] = { value, conflict: false, dirty: true };
                warning.textContent = value ? "선택한 내용은 기본 정보 저장 시 반영됩니다." : "미입력으로 저장합니다. ‘없음’으로 판단하지 않습니다.";
                if (kind === "health") onHealthChange(value);
                refresh(); onDirty();
            };
            options.forEach(([value, text]) => {
                const label = node("label", "basic-info-radio-option");
                const input = node("input");
                input.type = "radio"; input.name = `basic-${kind}-choice`; input.value = value;
                input.checked = initial.value === value;
                input.addEventListener("change", () => update(value));
                label.append(input, document.createTextNode(text)); controls.append(label);
            });
            const clear = node("button", "basic-info-text-action", "선택 해제");
            clear.type = "button"; clear.setAttribute("aria-label", `${title} 선택 해제`);
            clear.addEventListener("click", () => { controls.querySelectorAll("input").forEach(input => { input.checked = false; }); update(""); });
            group.append(controls, warning, clear);
            choiceGroups.push({ kind, group });
            return group;
        };
        const render = (nextCatalog, profileHealth) => {
            catalog = nextCatalog; choices = {}; choiceGroups = [];
            const opened = new Set([...list.querySelectorAll("details[open][data-document-type-code]")].map(card => card.dataset.documentTypeCode));
            // 가족 입력 DOM과 이벤트를 재사용하며 목록 재생성에 같이 삭제되지 않게 이동한다.
            if (familySection) { familySection.hidden = true; list.before(familySection); }
            list.replaceChildren();
            catalog.forEach(doc => {
                const code = doc.documentTypeCode;
                const card = node("details", "member-document-card basic-info-document");
                card.dataset.documentTypeCode = code;
                card.open = opened.has(code) || ["NATIONAL_TAX_PAID", "HEALTH_INSURANCE_QUALIFICATION"].includes(code);
                const summary = node("summary", "basic-info-document-summary");
                summary.append(node("strong", "", doc.documentTypeLabel), node("span", "basic-info-document-status", "미입력"));
                const body = node("div", "basic-info-document-body");
                if (code === "NATIONAL_TAX_PAID") {
                    body.append(radioGroup("tax", "국세 납부 상태", [["DELINQUENT", "체납"], ["PAID", "완납"]], core.selectTax(doc.fields || [])));
                } else if (code === "HEALTH_INSURANCE_QUALIFICATION") {
                    body.append(radioGroup("health", "건강보험 자격", [["WORKPLACE", "직장가입자"], ["LOCAL", "지역가입자"], ["DEPENDENT", "피부양자"]], core.selectHealth(doc.fields || [], profileHealth)));
                } else if (code === "FAMILY_RELATION" && familySection) {
                    familySection.hidden = false; body.append(familySection);
                    // 과거 단일 필드 값을 가족 인원이나 개인 자격으로 추측해 변환하지 않는다.
                    const legacy = (doc.fields || []).filter(core.hasValue);
                    if (legacy.length) {
                        const previous = node("details", "basic-info-legacy-values");
                        previous.append(node("summary", "", "이전에 입력한 서류 요약 확인"));
                        previous.append(node("p", "field-help", "이전 요약값은 보존합니다. 위의 가족별 정보와 별도 자료이며 자동으로 인원수나 관계를 추정하지 않습니다. 내용이 다르면 운영자에게 정정을 요청하세요."));
                        legacy.forEach(field => {
                            const value = field.valueText ?? field.valueNumber ?? field.valueDate ?? (field.valueBoolean === true ? "예" : "아니오");
                            previous.append(node("p", "field-help", `${field.fieldLabel}: ${value}`));
                        });
                        body.append(previous);
                    }
                } else {
                    const grid = node("div", "document-field-grid");
                    (doc.fields || []).filter(field => core.isVisibleField(code, field.fieldKey)).forEach(field => grid.append(renderField(field)));
                    body.append(grid);
                }
                card.append(summary, body); list.append(card);
            });
            // 카탈로그에 가족 서류가 없더라도 기존 가족 정보 입력을 잃지 않는다.
            if (familySection && !catalog.some(doc => doc.documentTypeCode === "FAMILY_RELATION")) familySection.hidden = false;
            if (!catalog.length) list.append(node("p", "field-help", "현재 입력 가능한 서류 항목이 없습니다. 기본정보는 저장할 수 있습니다."));
            refresh();
        };
        const merge = (rendered) => core.mergeDocuments(catalog, rendered, choices);
        const refresh = () => {
            list.querySelectorAll(".basic-info-document").forEach(card => {
                const code = card.dataset.documentTypeCode;
                let entered = [...card.querySelectorAll("[data-document-input]")].some(input => input.value !== "");
                const kind = code === "NATIONAL_TAX_PAID" ? "tax" : code === "HEALTH_INSURANCE_QUALIFICATION" ? "health" : null;
                if (kind) entered = Boolean(choices[kind]?.value);
                if (code === "FAMILY_RELATION") entered = [...card.querySelectorAll(".family-row input,.family-row select")].some(input => input.value !== "");
                const retained = core.fieldsOf(catalog, code).some(core.hasValue);
                card.querySelector(".basic-info-document-status").textContent = kind && choices[kind]?.conflict ? "값 확인 필요" : entered ? "입력 있음" : retained && !choices[kind]?.dirty ? "이전 값 보존" : "미입력";
            });
        };
        const validate = () => {
            const problem = choiceGroups.find(({ kind }) => choices[kind]?.conflict);
            if (!problem) return null;
            problem.group.closest("details").open = true;
            problem.group.querySelector("input").focus();
            return "저장된 국세 또는 건강보험 값이 서로 다릅니다. 표시된 서류에서 하나를 다시 선택하거나 선택 해제로 미입력 처리하세요.";
        };
        form.addEventListener("input", refresh);
        form.addEventListener("change", refresh);
        app.querySelectorAll(".basic-info-section-links a").forEach(link => link.addEventListener("click", () => {
            const target = app.querySelector(link.getAttribute("href"));
            if (target?.tagName === "DETAILS") target.open = true;
        }));
        return { render, merge, refresh, validate };
    };
    window.SanebMemberBasicInfoUI = { create };
})();
