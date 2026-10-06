// 브라우저를 실행하지 않는 간소화 서류 뷰 이벤트 단위 시험.
const test = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { runInNewContext } = require('node:vm');
const path = require('node:path');
const core = require('../../src/main/resources/static/js/saneb-member-basic-info-core.js');
const script = readFileSync(path.resolve(__dirname, '../../src/main/resources/static/js/saneb-member-basic-info-simple.js'), 'utf8');
class Element {
    constructor(tag = 'div') {
        this.tagName = tag.toUpperCase(); this.children = []; this.dataset = {}; this.events = {};
        this.className = ''; this.value = ''; this.open = false; this.checked = false; this.hidden = false;
        this.textContent = ''; this.attributes = {}; this.parentElement = null;
    }
    get parentNode() { return this.parentElement; }
    append(...nodes) { nodes.forEach(node => { node.remove(); node.parentElement = this; this.children.push(node); }); }
    remove() { if (this.parentElement) this.parentElement.children = this.parentElement.children.filter(child => child !== this); this.parentElement = null; }
    before(node) { const parent = this.parentElement; node.remove(); node.parentElement = parent; parent.children.splice(parent.children.indexOf(this), 0, node); }
    replaceChildren(...nodes) { this.children.forEach(child => { child.parentElement = null; }); this.children = []; this.append(...nodes); }
    setAttribute(key, value) { this.attributes[key] = value; }
    getAttribute(key) { return this.attributes[key]; }
    addEventListener(type, callback) { this.events[type] = callback; }
    focus() { this.focused = true; }
    matches(selector) {
        if (selector === '[data-document-input]') return this.dataset.documentInput !== undefined;
        if (selector === 'details[open][data-document-type-code]') return this.tagName === 'DETAILS' && this.open && this.dataset.documentTypeCode !== undefined;
        if (selector.startsWith('.')) return this.className.split(' ').includes(selector.slice(1));
        return this.tagName === selector.toUpperCase();
    }
    querySelectorAll(selector) {
        const results = [];
        const choices = selector.split(',');
        const match = (element, choice) => {
            const parts = choice.split(' ');
            return parts.length === 1 ? element.matches(choice) : element.matches(parts[1]) && Boolean(element.parentElement?.closest(parts[0]));
        };
        const visit = root => root.children.forEach(child => { if (choices.some(choice => match(child, choice))) results.push(child); visit(child); });
        visit(this); return results;
    }
    querySelector(selector) { return this.querySelectorAll(selector)[0] || null; }
    closest(selector) { return this.matches(selector) ? this : this.parentElement?.closest(selector); }
}
function harness() {
    const app = new Element(), form = new Element('form'), list = new Element(), family = new Element();
    app.append(form); form.append(family, list);
    let health = null, dirtyCount = 0;
    const window = { SanebMemberBasicInfo: core };
    const document = { createElement: tag => new Element(tag), createTextNode: text => Object.assign(new Element('#text'), { textContent: text }) };
    runInNewContext(script, { window, document });
    const view = window.SanebMemberBasicInfoUI.create({ app, form, list, familySection: family,
        renderField: field => {
            const block = new Element(); block.className = 'document-field-block';
            const input = new Element('input'); input.dataset.documentInput = 'true'; input.value = field.valueText || '';
            block.append(input); return block;
        }, onHealthChange: value => { health = value; }, onDirty: () => { dirtyCount += 1; }
    });
    return { app, form, list, family, view, health: () => health, dirtyCount: () => dirtyCount };
}
const field = (key, data = {}) => ({ standardFieldId: key, fieldKey: key, ...data });
const doc = (code, fields = []) => ({ documentTypeCode: code, documentTypeLabel: code, fields });
test('조회된 카탈로그는 selected=false여도 전부 렌더링', () => {
    const h = harness(); h.view.render([doc('VAT_TAX_BASE', [field('ANNUAL_REVENUE')]), doc('NATIONAL_TAX_PAID')], null);
    assert.equal(h.list.querySelectorAll('details').length, 2);
    assert.equal(h.list.querySelectorAll('input').length, 3);
});
test('국세 라디오 변경과 해제가 v1 저장값으로 전달', () => {
    const h = harness(); h.view.render([doc('NATIONAL_TAX_PAID', [field('NATIONAL_TAX_DELINQUENT'), field('TAX_PAID_STATUS')])], null);
    const inputs = h.list.querySelectorAll('input');
    inputs[1].events.change();
    let values = h.view.merge([])[0].fields;
    assert.equal(values[0].valueBoolean, false); assert.equal(values[1].valueBoolean, true);
    h.list.querySelector('button').events.click(); values = h.view.merge([])[0].fields;
    assert.ok(values.every(value => value.valueBoolean === null)); assert.equal(h.dirtyCount(), 2);
});
test('건강보험 변경은 본인 자격도 동기화하고 미입력 해제 지원', () => {
    const h = harness(); h.view.render([doc('HEALTH_INSURANCE_QUALIFICATION', [field('HEALTH_INSURANCE_BASIS_CODE')])], 'UNKNOWN');
    assert.ok(h.list.querySelectorAll('input').every(input => !input.checked));
    h.list.querySelectorAll('input')[2].events.change();
    assert.equal(h.health(), 'DEPENDENT'); assert.equal(h.view.merge([])[0].fields[0].valueText, 'DEPENDENT');
    h.list.querySelector('button').events.click(); assert.equal(h.health(), '');
});
test('상충 값 저장 검증은 카드 펼침·포커스 후 명시적 선택으로 해소', () => {
    const h = harness(); h.view.render([doc('NATIONAL_TAX_PAID', [field('NATIONAL_TAX_DELINQUENT', { valueBoolean: true }), field('TAX_PAID_STATUS', { valueBoolean: true })])], null);
    h.list.querySelector('details').open = false;
    assert.match(h.view.validate(), /서로 다릅니다/);
    assert.equal(h.list.querySelector('details').open, true);
    assert.equal(h.list.querySelector('input').focused, true);
    h.list.querySelector('input').events.change(); assert.equal(h.view.validate(), null);
});
test('가족 카드의 기존 DOM·입력값은 카탈로그 재렌더링에서도 유지', () => {
    const h = harness(), row = new Element(), input = new Element('input');
    row.className = 'family-row'; input.value = '2018'; row.append(input); h.family.append(row);
    const catalog = [doc('FAMILY_RELATION')]; h.view.render(catalog, null); h.view.render(catalog, null);
    assert.equal(h.family.querySelector('input'), input); assert.equal(input.value, '2018');
    assert.equal(h.list.querySelector('.basic-info-document-status').textContent, '입력 있음');
});
test('부가세 숨긴 필드는 표시하지 않으면서 저장에 보존', () => {
    const h = harness(); h.view.render([doc('VAT_TAX_BASE', [field('TAX_PERIOD', { valueText: '2025' }), field('SUPPLY_AMOUNT', { valueNumber: 33 }), field('TAX_TYPE_CODE', { valueText: 'GENERAL_TAXPAYER' })])], null);
    assert.equal(h.list.querySelectorAll('input').length, 1);
    assert.equal(h.view.merge([])[0].fields[1].valueNumber, 33);
});
test('카탈로그가 비어도 가족 입력 DOM을 숨기지 않음', () => {
    const h = harness(); h.view.render([], null);
    assert.equal(h.family.hidden, false); assert.match(h.list.querySelector('p').textContent, /서류 항목이 없습니다/);
});
