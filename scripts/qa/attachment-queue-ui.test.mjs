import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
import vm from 'node:vm';
const require = createRequire(import.meta.url);
const Q = require('../../src/main/resources/static/js/saneb-attachment-queue-core.js');
const C = require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const state = Q.validatedState(Q.stateFromSearch(''));
const source = (index = 1, code = state.processingFlowStatusCode) => ({sourceId: `11111111-1111-4111-8111-${String(index).padStart(12, '0')}`,
    providerCode: 'BIZINFO', title: '소상공인 지원금', processingFlow: {statusCode: code, isAutomaticAnalysisComplete: true, isFinalReviewAvailable: true}});
const page = (total = 1, selected = state, code = selected.processingFlowStatusCode) => ({page: selected.page, size: 20, totalCount: total,
    totalPages: Math.ceil(total / 20), items: Array.from({length: Math.min(20, Math.max(0, total - (selected.page - 1) * 20))}, (_, index) => source(index + 1, code))});
const response = (data, status = 200) => ({ok: status === 200, status, json: async () => ({success: status === 200, data})});

test('default final review queue and explicit all-state survive refresh and Korean search round trip', () => {
    assert.equal(state.processingFlowStatusCode, 'READY_FOR_FINAL_REVIEW');
    const all = {processingFlowStatusCode: '', providerCode: 'BIZINFO', keyword: '소상공인 & 지원', page: 3};
    assert.deepEqual(Q.validatedState(Q.stateFromSearch(Q.searchFromState(all))), all);
});
test('all nine states have precise Korean labels and preserve filter value', () => {
    assert.equal(Q.flowCodes.length, 9);
    for (const code of Q.flowCodes) {
        assert.equal(Q.validatedState({...state, processingFlowStatusCode: ` ${code} `}).processingFlowStatusCode, code);
        assert.doesNotMatch(C.label(code), /확인 필요 \(/);
    }
});
test('unknown URL values are rejected instead of silently widening filter', () => {
    assert.equal(Q.stateFromSearch('?processingFlowStatusCode=BOGUS').processingFlowStatusCode, 'BOGUS');
    assert.throws(() => Q.validatedState({...state, processingFlowStatusCode: 'BOGUS'}), /처리 단계/);
    assert.throws(() => Q.validatedState({...state, providerCode: 'OTHER'}), /수집처/);
    for (const value of ['0', '-1', '2.1', '1e2', '1000001', 'Infinity'])
        assert.throws(() => Q.validatedState({...state, page: value}), /페이지/);
    assert.throws(() => Q.validatedState({...state, keyword: 'a'.repeat(101)}), /100자/);
    assert.throws(() => Q.validatedState({...state, keyword: 'a\u0085b'}), /제어문자/);
});
test('paging uses complete server count including second page and out-of-range pages', () => {
    const second = {...state, page: 2};
    const data = Q.validatePage(page(21, second), second);
    assert.equal(data.items.length, 1); assert.equal(data.totalCount, 21); assert.equal(data.totalPages, 2);
    assert.equal(Q.validatePage(page(21, {...state, page: 3}), {...state, page: 3}).items.length, 0);
    assert.equal(Q.validatePage(page(0), state).totalCount, 0);
});
test('page and count contract mismatches cannot be displayed as valid empty result', () => {
    for (const change of [{page: 2}, {size: 10}, {totalCount: null}, {totalPages: 7}, {items: []}, {totalCount: Number.MAX_SAFE_INTEGER + 1}])
        assert.throws(() => Q.validatePage({...page(), ...change}, state), /응답이 일치하지/);
});
test('final-ready filter rejects processing and technical rows without client-side filtering', () => {
    for (const code of ['AUTOMATIC_PROCESSING', 'TECHNICAL_EXCEPTION', 'NOT_APPLIED', 'FINAL_REVIEW_EXCEPTION'])
        assert.throws(() => Q.validatePage({...page(), items: [source(1, code)]}, state), /처리 상태/);
    const all = {...state, processingFlowStatusCode: ''};
    assert.equal(Q.validatePage({ ...page(2, all), items: [source(1, 'AUTOMATIC_PROCESSING'), source(2, 'TECHNICAL_EXCEPTION')]}, all).items.length, 2);
});
test('invalid identifiers, duplicate rows, provider mismatch and malformed booleans fail closed', () => {
    assert.throws(() => Q.validatePage({...page(), items: [{...source(), sourceId: 'javascript:alert(1)'}]}, state), /처리 상태/);
    assert.throws(() => Q.validatePage({...page(2), items: [source(), source()]}, state), /처리 상태/);
    assert.throws(() => Q.validatePage(page(), {...state, providerCode: 'LOCAL_GOV_NOTICE'}), /처리 상태/);
    const malformed = source(); malformed.processingFlow.isAutomaticAnalysisComplete = 'true';
    assert.throws(() => Q.validatePage({...page(), items: [malformed]}, state), /처리 상태/);
});
test('manual confirmation does not fabricate complete automatic analysis', () => {
    const selected = {...state, processingFlowStatusCode: 'FINAL_REVIEW_CONFIRMED'};
    const item = source(1, selected.processingFlowStatusCode); item.processingFlow.isAutomaticAnalysisComplete = false;
    assert.equal(Q.validatePage({...page(1, selected), items: [item]}, selected).items[0].processingFlow.isAutomaticAnalysisComplete, false);
});
test('read-only client sends exact v2 filter and no-store without body or mutation', async () => {
    const fetcher = async (url, options) => {
        const parsed = new URL(url, 'https://saneb.invalid');
        assert.equal(parsed.pathname, '/api/v2/admin/announcement-sources');
        assert.equal(parsed.searchParams.get('processingFlowStatusCode'), 'READY_FOR_FINAL_REVIEW');
        assert.equal(parsed.searchParams.get('size'), '20');
        assert.equal(options.method, 'GET'); assert.equal(options.body, undefined);
        assert.equal(options.cache, 'no-store'); assert.equal(options.credentials, 'same-origin');
        assert.equal(options.redirect, 'error'); return response(page());
    };
    assert.equal((await Q.client(fetcher)(state)).totalCount, 1);
});
test('invalid filters cannot issue HTTP requests', async () => {
    let calls = 0;
    await assert.rejects(Q.client(async () => { calls++; })({...state, processingFlowStatusCode: 'BAD'}), /처리 단계/);
    assert.equal(calls, 0);
});
test('authentication and server failures do not become zero counts', async () => {
    for (const [status, message] of [[401, /로그인이 만료/], [403, /조회 권한/], [500, /서버에서 목록/]])
        await assert.rejects(Q.client(async () => response(null, status))(state), message);
    await assert.rejects(Q.client(async () => ({ok: true, status: 200, json: async () => { throw Error(); }}))(state), /서버 응답/);
});
test('network timeout has recoverable read-only message and clears timer', async () => {
    await assert.rejects(Q.client(async () => { throw new TypeError('offline'); })(state), /네트워크를 확인/);
    const fetcher = async (_url, options) => new Promise((_resolve, reject) => {
        options.signal.addEventListener('abort', () => reject(new DOMException('timeout', 'AbortError')));
    });
    await assert.rejects(Q.client(fetcher, 5)(state), /조회 시간이 초과/);
});
test('late result and late failure are ignored after newer filter request', () => {
    const gate = Q.latestRequest(); const old = gate.begin(); const latest = gate.begin();
    assert.equal(gate.isCurrent(old), false); assert.equal(gate.isCurrent(latest), true);
});
test('DOM implementation preserves URL navigation and renders text without raw HTML or automatic writes', async () => {
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-attachment-queue.js', import.meta.url), 'utf8');
    assert.doesNotMatch(script, /innerHTML|outerHTML|insertAdjacentHTML|localStorage|sessionStorage|\.filter\(/);
    assert.match(script, /textContent/); assert.match(script, /popstate/); assert.match(script, /pushState/);
    assert.match(script, /isCurrent\(revision\)/); assert.match(script, /0건이 아니라 미확인/);
    assert.match(script, /page: 1/); assert.match(script, /aria-busy/); assert.match(script, /isAutomaticAnalysisComplete/);
});

// 실제 화면 이벤트 코드를 DOM/HTTP 대역으로 실행한다. 실제 브라우저·PostgreSQL 검증은 아니다.
class Element {
    constructor(tag = 'div') { this.tagName = tag.toUpperCase(); this.children = []; this.events = {}; this.dataset = {}; this.textContent = ''; this.value = ''; this.disabled = false; this.hidden = false; }
    appendChild(child) { child.parent = this; this.children.push(child); return child; }
    replaceChildren() { this.children = []; }
    addEventListener(type, action) { this.events[type] = action; }
    setAttribute(name, value) { this[name] = value; }
    querySelectorAll() { return this.children.filter(child => child.dataset.unsupported); }
    get options() { return this.children; }
    remove() { this.parent.children = this.parent.children.filter(child => child !== this); }
    fire(type) { this.events[type]?.({preventDefault() {}}); }
}
const settle = async () => { for (let index = 0; index < 4; index++) await new Promise(resolve => setImmediate(resolve)); };
const allText = node => [node.textContent, ...node.children.map(allText)].join(' ');
async function harness({search = '', fetcher} = {}) {
    const nodes = new Map(), calls = [], events = {};
    const one = selector => { if (!nodes.has(selector)) nodes.set(selector, new Element()); return nodes.get(selector); };
    const fields = new Map([['processingFlowStatusCode', new Element('select')], ['providerCode', new Element('select')], ['keyword', new Element('input')]]);
    for (const code of ['', ...Q.flowCodes]) { const option = new Element('option'); option.value = code; fields.get('processingFlowStatusCode').appendChild(option); }
    for (const code of ['', 'BIZINFO', 'GOV24_PUBLIC_SERVICE', 'LOCAL_GOV_NOTICE']) { const option = new Element('option'); option.value = code; fields.get('providerCode').appendChild(option); }
    one('[data-queue-filter]').elements = {namedItem: name => fields.get(name)};
    const win = {SanebAttachmentQueue: Q, SanebAttachmentReview: C,
        location: {pathname: '/app/admin/announcement-attachment-queue', search}, addEventListener: (type, handler) => { events[type] = handler; }};
    win.history = {pushState: (_state, _title, url) => { win.location.search = url.slice(url.indexOf('?')); },
        replaceState: (_state, _title, url) => { win.location.search = url.slice(url.indexOf('?')); }};
    win.fetch = async (url, options) => {
        calls.push({url, options});
        const selected = Q.validatedState(Q.stateFromSearch(url.slice(url.indexOf('?'))));
        return fetcher ? fetcher(url, options, selected) : response(page(21, selected, selected.processingFlowStatusCode || 'READY_FOR_FINAL_REVIEW'));
    };
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-attachment-queue.js', import.meta.url), 'utf8');
    vm.runInNewContext(script, {window: win, document: {querySelector: () => ({querySelector: one}), createElement: tag => new Element(tag)}});
    await settle(); return {one, fields, win, events, calls};
}
test('screen defaults to ready rows, preserves count across page navigation and resets filter to first page', async () => {
    const h = await harness();
    assert.match(h.one('[data-queue-count]').textContent, /21건 · 현재 페이지 20건/);
    assert.equal(h.one('[data-queue-next]').disabled, false);
    h.one('[data-queue-next]').fire('click'); await settle();
    assert.match(h.win.location.search, /page=2/); assert.match(h.one('[data-queue-count]').textContent, /현재 페이지 1건/);
    h.fields.get('processingFlowStatusCode').value = 'AUTOMATIC_PROCESSING';
    h.one('[data-queue-filter]').fire('submit'); await settle();
    assert.match(h.win.location.search, /page=1/); assert.match(allText(h.one('[data-queue-items]')), /자동 분석 대기·진행 중/);
    assert.equal(h.calls.every(call => call.options.method === 'GET'), true);
});
test('screen restores explicit all-filter and page on popstate without losing Korean keyword', async () => {
    const h = await harness();
    h.win.location.search = '?processingFlowStatusCode=&page=2&keyword=소상공인'; h.events.popstate(); await settle();
    assert.equal(h.fields.get('processingFlowStatusCode').value, ''); assert.equal(h.fields.get('keyword').value, '소상공인');
    assert.match(h.one('[data-queue-page]').textContent, /2페이지/);
    assert.match(h.one('[data-queue-filter-summary]').textContent, /전체 처리 단계/);
});
test('screen rejects stale response after changing filter and retains latest failure with unknown count', async () => {
    let resolveOld; let first = true;
    const h = await harness({fetcher: async (_url, _options, selected) => {
        if (first) { first = false; return new Promise(resolve => { resolveOld = () => resolve(response(page(1, selected))); }); }
        return response(null, 503);
    }});
    h.fields.get('processingFlowStatusCode').value = 'TECHNICAL_EXCEPTION'; h.one('[data-queue-filter]').fire('submit'); await settle();
    resolveOld(); await settle();
    assert.equal(h.one('[data-queue-error]').hidden, false);
    assert.match(h.one('[data-queue-status]').textContent, /0건이 아니라 미확인/);
    assert.equal(h.one('[data-queue-items]').children.length, 0); assert.equal(h.one('[data-queue-next]').disabled, true);
    assert.equal(h.fields.get('processingFlowStatusCode').value, 'TECHNICAL_EXCEPTION');
    assert.equal(h.one('#attachment-queue-results')['aria-busy'], 'false');
});
test('screen does not execute malformed URL filter and offers deliberate form recovery', async () => {
    const h = await harness({search: '?processingFlowStatusCode=BAD&page=9'});
    assert.equal(h.calls.length, 0); assert.equal(h.fields.get('processingFlowStatusCode').value, 'BAD');
    assert.match(h.one('[data-queue-error]').textContent, /처리 단계/);
    h.one('[data-queue-clear]').fire('click'); await settle();
    assert.equal(h.calls.length, 1); assert.equal(h.one('[data-queue-error]').hidden, true);
    assert.equal(h.fields.get('processingFlowStatusCode').value, ''); assert.match(h.win.location.search, /page=1/);
});
