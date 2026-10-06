const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const C = require('../../src/main/resources/static/js/saneb-member-basic-info-core.js');
const source = name => fs.readFileSync(path.resolve(__dirname, '../../src/main/resources/', name), 'utf8');
const js = source('static/js/saneb-member-basic-info.js');
const ui = source('static/js/saneb-member-basic-info-simple.js');
const html = source('templates/app/member-basic-info.html');
const field = (key, data = {}) => ({ standardFieldId: key, fieldKey: key, ...data });
const doc = (code, fields) => ({ documentTypeCode: code, fields });
test('국세 단일 선택은 기존 체납/완납 boolean 양방향과 동일', () => {
    assert.deepEqual(C.selectTax([field('NATIONAL_TAX_DELINQUENT', { valueBoolean: false })]), { value: 'PAID', conflict: false });
    assert.equal(C.selectTax([field('TAX_PAID_STATUS', { valueBoolean: false })]).value, 'DELINQUENT');
    assert.equal(C.selectTax([]).value, '');
});
test('상충하는 국세 값은 완납으로 추정하지 않음', () => {
    const tax = C.selectTax([field('NATIONAL_TAX_DELINQUENT', { valueBoolean: true }), field('TAX_PAID_STATUS', { valueBoolean: true })]);
    assert.equal(tax.conflict, true); assert.equal(tax.value, '');
});
test('건강보험 코드와 true 플래그를 읽되 false만으로 자격을 추측하지 않음', () => {
    for (const [key, value] of [['WORKPLACE_INSURED_STATUS', 'WORKPLACE'], ['LOCAL_INSURED_STATUS', 'LOCAL'], ['DEPENDENT_STATUS', 'DEPENDENT']]) {
        assert.equal(C.selectHealth([field(key, { valueBoolean: true })], null).value, value);
        assert.equal(C.selectHealth([field(key, { valueBoolean: false })], null).value, '');
    }
    assert.equal(C.selectHealth([], 'UNKNOWN').value, '');
    assert.equal(C.selectHealth([], 'UNKNOWN').unknown, true);
});
test('건강보험 본인정보/서류/boolean 충돌 감지', () => {
    assert.equal(C.selectHealth([field('HEALTH_INSURANCE_BASIS_CODE', { valueText: 'LOCAL' })], 'WORKPLACE').conflict, true);
    assert.equal(C.selectHealth([field('DEPENDENT_STATUS', { valueBoolean: false })], 'DEPENDENT').conflict, true);
    assert.equal(C.selectHealth([], 'LEGACY_UNKNOWN').conflict, true);
});
test('서류 조회 원본을 변경하지 않고 숨긴 과세기간·공급가액·false·0 보존', () => {
    const catalog = [doc('VAT_TAX_BASE', [field('TAX_PERIOD', { valueText: '2025-01~06' }), field('SUPPLY_AMOUNT', { valueNumber: 0 }), field('ANNUAL_REVENUE', { valueNumber: 100 })]), doc('FAMILY_RELATION', [field('HAS_SPOUSE', { valueBoolean: false })])];
    const before = JSON.stringify(catalog);
    const merged = C.mergeDocuments(catalog, [doc('VAT_TAX_BASE', [field('ANNUAL_REVENUE', { valueNumber: 200 })])], {});
    assert.equal(merged[0].fields[0].valueText, '2025-01~06');
    assert.equal(merged[0].fields[1].valueNumber, 0);
    assert.equal(merged[0].fields[2].valueNumber, 200);
    assert.equal(merged[1].fields[0].valueBoolean, false);
    assert.equal(JSON.stringify(catalog), before);
});
test('조회만 한 단일 선택은 기존 증빙 값을 새로 만들거나 변경하지 않음', () => {
    const catalog = [doc('HEALTH_INSURANCE_QUALIFICATION', [field('HEALTH_INSURANCE_BASIS_CODE')])];
    const merged = C.mergeDocuments(catalog, [], { health: { value: 'WORKPLACE', dirty: false } });
    assert.equal(merged[0].fields[0].valueText, null);
});
test('국세 선택 변경은 기존 두 필드만 일관되게 갱신, 발급일 보존', () => {
    const catalog = [doc('NATIONAL_TAX_PAID', [field('NATIONAL_TAX_DELINQUENT'), field('TAX_PAID_STATUS'), field('ISSUE_DATE', { valueDate: '2026-01-01' })])];
    for (const choice of ['PAID', 'DELINQUENT']) {
        const values = C.mergeDocuments(catalog, [], { tax: { value: choice, dirty: true } })[0].fields;
        assert.equal(values[0].valueBoolean, choice === 'DELINQUENT');
        assert.equal(values[1].valueBoolean, choice === 'PAID');
        assert.equal(values[2].valueDate, '2026-01-01');
    }
});
test('건강보험은 한 코드와 배타적인 세 flag로 변환', () => {
    const catalog = [doc('HEALTH_INSURANCE_QUALIFICATION', ['HEALTH_INSURANCE_BASIS_CODE', 'WORKPLACE_INSURED_STATUS', 'LOCAL_INSURED_STATUS', 'DEPENDENT_STATUS'].map(key => field(key)))];
    for (const choice of ['WORKPLACE', 'LOCAL', 'DEPENDENT']) {
        const values = C.mergeDocuments(catalog, [], { health: { value: choice, dirty: true } })[0].fields;
        assert.equal(values[0].valueText, choice);
        assert.equal(values.filter(value => value.valueBoolean === true).length, 1);
        assert.equal(C.selectHealth(values.map((value, i) => ({ ...value, fieldKey: catalog[0].fields[i].fieldKey })), choice).value, choice);
    }
});
test('선택 해제는 false가 아니라 모든 대응 필드 null', () => {
    const catalog = [doc('NATIONAL_TAX_PAID', [field('NATIONAL_TAX_DELINQUENT', { valueBoolean: false }), field('TAX_PAID_STATUS', { valueBoolean: true })])];
    const values = C.mergeDocuments(catalog, [], { tax: { value: '', dirty: true } })[0].fields;
    assert.ok(values.every(value => value.valueBoolean === null));
});
test('일반 입력값 명시적 지우기는 null로 전달', () => {
    const catalog = [doc('VAT_TAX_BASE', [field('ANNUAL_REVENUE', { valueNumber: 500 })])];
    assert.equal(C.mergeDocuments(catalog, [doc('VAT_TAX_BASE', [field('ANNUAL_REVENUE')])], {})[0].fields[0].valueNumber, null);
});
test('부가세 두 항목과 복합 UI로 대체한 서류 필드만 숨김', () => {
    assert.equal(C.isVisibleField('VAT_TAX_BASE', 'TAX_PERIOD'), false);
    assert.equal(C.isVisibleField('VAT_TAX_BASE', 'SUPPLY_AMOUNT'), false);
    assert.equal(C.isVisibleField('VAT_TAX_BASE', 'ANNUAL_REVENUE'), true);
    assert.equal(C.isVisibleField('BUSINESS_REGISTRATION', 'OPENING_DATE'), true);
    assert.equal(C.isVisibleField('FAMILY_RELATION', 'CHILD_COUNT'), false);
});
test('일반 사용자만 간소화, 관리자 경로/기존 v1 API 유지', () => {
    assert.match(html, /data-basic-info-simple/);
    assert.doesNotMatch(source('templates/app/admin-member-basic-info.html'), /data-basic-info-simple|saneb-member-basic-info-simple/);
    assert.match(html, /\/api\/v1\/member\/basic-info/);
    assert.match(js, /return isSimpleApp \? simpleUi\.merge\(rendered\) : rendered/);
});
test('조회 실패 시 저장 금지 및 재시도, 저장 중 수정 잠금', () => {
    assert.match(js, /busy \|\| \(isSimpleApp && !loaded\)/);
    assert.match(js, /disabledBeforeBusy/);
    assert.match(js, /retryButton\.hidden = false/);
    assert.match(html, /data-basic-info-submit disabled/);
});
test('가족 인원별 ID·삭제 취소·이탈 경고, 원문 HTML 삽입 없음', () => {
    assert.match(js, /basic-family-\$\{familySequence\}/);
    assert.match(js, /familyList\.insertBefore\(entry\.row/);
    assert.match(js, /beforeunload/);
    assert.doesNotMatch(ui, /innerHTML|localStorage|sessionStorage/);
});
test('카탈로그 전체 사전 표시, 실제 자동매칭을 과장하지 않음', () => {
    assert.match(ui, /catalog\.forEach/);
    assert.doesNotMatch(html, /data-document-add|data-document-type-select|공고가.*자동.*매칭/);
    assert.match(html, /후보는 선정 결과가 아니며/);
});
