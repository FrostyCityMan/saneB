/* 기본정보 간소화 UI의 v1 호환 변환. 생략한 기존 서류 값은 그대로 보존한다. */
((root) => {
    "use strict";
    const healthFlags = { WORKPLACE_INSURED_STATUS: "WORKPLACE", LOCAL_INSURED_STATUS: "LOCAL", DEPENDENT_STATUS: "DEPENDENT" };
    const nationalKeys = ["NATIONAL_TAX_DELINQUENT", "TAX_PAID_STATUS"];
    const healthKeys = ["HEALTH_INSURANCE_BASIS_CODE", ...Object.keys(healthFlags)];
    const emptyValue = (field) => ({ standardFieldId: field.standardFieldId, valueText: null, valueNumber: null, valueDate: null, valueBoolean: null });
    const copyValue = (field) => ({ ...emptyValue(field), valueText: field.valueText ?? null, valueNumber: field.valueNumber ?? null, valueDate: field.valueDate ?? null, valueBoolean: field.valueBoolean ?? null });
    const hasValue = (field) => [field.valueText, field.valueNumber, field.valueDate, field.valueBoolean].some(value => value != null && value !== "");
    const fieldsOf = (catalog, code) => catalog.find(doc => doc.documentTypeCode === code)?.fields || [];
    const selectTax = (fields) => {
        const candidates = fields.flatMap(field => {
            if (!nationalKeys.includes(field.fieldKey) || field.valueBoolean == null) return [];
            const paid = field.fieldKey === "TAX_PAID_STATUS" ? field.valueBoolean : !field.valueBoolean;
            return [paid ? "PAID" : "DELINQUENT"];
        });
        const values = [...new Set(candidates)];
        return { value: values.length === 1 ? values[0] : "", conflict: values.length > 1 };
    };
    const selectHealth = (fields, profileCode) => {
        const codes = [profileCode, ...fields.filter(f => f.fieldKey === "HEALTH_INSURANCE_BASIS_CODE").map(f => f.valueText)].filter(Boolean);
        const candidates = codes.filter(code => ["WORKPLACE", "LOCAL", "DEPENDENT"].includes(code));
        fields.forEach(field => { if (healthFlags[field.fieldKey] && field.valueBoolean === true) candidates.push(healthFlags[field.fieldKey]); });
        const values = [...new Set(candidates)];
        const negativeConflict = fields.some(f => f.valueBoolean === false && values.includes(healthFlags[f.fieldKey]));
        const unknownCode = codes.some(code => !["WORKPLACE", "LOCAL", "DEPENDENT", "UNKNOWN"].includes(code));
        const conflict = values.length > 1 || negativeConflict || unknownCode;
        return { value: !conflict && values.length === 1 ? values[0] : "", conflict, unknown: codes.includes("UNKNOWN") };
    };
    const isVisibleField = (code, key) => {
        if (["NATIONAL_TAX_PAID", "HEALTH_INSURANCE_QUALIFICATION", "FAMILY_RELATION"].includes(code)) return false;
        return !(code === "VAT_TAX_BASE" && ["TAX_PERIOD", "SUPPLY_AMOUNT"].includes(key));
    };
    const mergeDocuments = (catalog, renderedDocuments, choices) => {
        const edited = new Map(renderedDocuments.flatMap(doc => doc.fields.map(f => [f.standardFieldId, f])));
        return catalog.map(doc => ({
            documentTypeCode: doc.documentTypeCode,
            fields: (doc.fields || []).map(field => {
                const value = edited.has(field.standardFieldId) ? copyValue(edited.get(field.standardFieldId)) : copyValue(field);
                if (doc.documentTypeCode === "NATIONAL_TAX_PAID" && choices.tax?.dirty && nationalKeys.includes(field.fieldKey)) {
                    const next = emptyValue(field);
                    if (choices.tax.value) next.valueBoolean = field.fieldKey === "TAX_PAID_STATUS" ? choices.tax.value === "PAID" : choices.tax.value === "DELINQUENT";
                    return next;
                }
                if (doc.documentTypeCode === "HEALTH_INSURANCE_QUALIFICATION" && choices.health?.dirty && healthKeys.includes(field.fieldKey)) {
                    const next = emptyValue(field);
                    if (choices.health.value) {
                        if (field.fieldKey === "HEALTH_INSURANCE_BASIS_CODE") next.valueText = choices.health.value;
                        else next.valueBoolean = healthFlags[field.fieldKey] === choices.health.value;
                    }
                    return next;
                }
                return value;
            })
        }));
    };
    const api = { hasValue, copyValue, fieldsOf, selectTax, selectHealth, isVisibleField, mergeDocuments };
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.SanebMemberBasicInfo = api;
})(typeof window !== "undefined" ? window : globalThis);
