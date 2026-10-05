import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {runInNewContext} from 'node:vm';

// 실제 페이지 스크립트를 DOM/HTTP 대역에서 실행한다. 브라우저 검증과 구분한다.
const script = await readFile(new URL('../../src/main/resources/static/js/saneb-collected-announcements.js', import.meta.url), 'utf8');
class Element {
    constructor() { this.children = []; this.events = {}; this.dataset = {}; this.value = ''; this.textContent = ''; this.classList = {toggle() {}, add() {}}; }
    appendChild(child) { this.children.push(child); return child; }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = children; }
    setAttribute(name, value) { this[name] = value; }
    addEventListener(name, callback) { this.events[name] = callback; }
    querySelectorAll() { return this.children; }
    fire(name) { this.events[name]?.({preventDefault() {}}); }
}
const settle = async () => { for (let n = 0; n < 5; n++) await new Promise(resolve => setImmediate(resolve)); };
const names = ['semanticStatusCode', 'providerCode', 'reviewStatusCode', 'targetCategoryCode', 'supportTypeCode', 'matchedGroupKindCode', 'matchLocationCode', 'ruleReleaseId', 'keyword'];
async function harness(state = null) {
    const nodes = new Map();
    const one = selector => { if (!nodes.has(selector)) nodes.set(selector, new Element()); return nodes.get(selector); };
    const form = one('[data-collected-filter-form]');
    form.elements = Object.fromEntries(names.map(name => [name, new Element()]));
    form.reset = () => { names.forEach(name => { form.elements[name].value = ''; }); form.elements.reviewStatusCode.value = 'REVIEW_PENDING'; };
    form.reset();
    const tabs = ['ACTION_REQUIRED', 'ACCEPTED', 'EXCLUDED', 'ALL'].map(view => {
        const node = new Element(); node.dataset.collectedView = view; return node;
    });
    const page = {dataset: {sourceUrl:'/api/v2/admin/announcement-sources'}, querySelector:one,
        querySelectorAll: selector => selector === '[data-collected-view]' ? tabs : []};
    const events = {}, calls = [];
    const win = {history: {state, replaceState(next) { this.state = structuredClone(next); }},
        addEventListener: (name, callback) => { events[name] = callback; },
        SanebAttachmentReview: {client: () => ({})}};
    win.fetch = async url => {
        calls.push(url);
        const parsed = new URL(url, 'https://saneb.invalid');
        // 목록 복원만 검증한다. 상세 조회 실패도 기존 오류 처리 경로로 종료된다.
        if (!parsed.search) return {ok:false, json:async () => ({message:'상세 테스트 대역'})};
        const number = Number(parsed.searchParams.get('page'));
        return {ok:true, json:async () => ({data:{page:number,totalPages:3,totalCount:33,
            items:[{sourceId:'source-a',title:'공고 A'},{sourceId:'source-b',title:'공고 B'}]}})};
    };
    class FormDataMock { constructor(f) { this.f = f; } *[Symbol.iterator]() { for (const name of names) yield [name, this.f.elements[name].value]; } }
    runInNewContext(script, {window:win, fetch:win.fetch, URL, URLSearchParams, FormData:FormDataMock,
        document:{readyState:'interactive',querySelector:()=>page,createElement:()=>new Element(),createDocumentFragment:()=>new Element()}});
    return {one, form, tabs, events, calls, win, show:async () => { events.pageshow({persisted:false}); await settle(); }};
}
const listCalls = h => h.calls.filter(url => url.includes('size=15')).map(url => new URL(url, 'https://saneb.invalid'));

test('initial request waits for pageshow and synchronizes browser-restored inputs', async () => {
    const h = await harness();
    assert.equal(h.calls.length, 0);
    h.form.elements.keyword.value = 'browser stale value';
    await h.show();
    assert.equal(h.form.elements.keyword.value, '');
    assert.equal(listCalls(h).length, 1);
    assert.equal(listCalls(h)[0].searchParams.get('keyword'), null);
});

test('back navigation restores submitted filters, tab, page and selected source together', async () => {
    const h = await harness(); await h.show();
    h.tabs[3].fire('click'); await settle();
    h.form.elements.keyword.value = 'SRC-017679';
    h.form.elements.providerCode.value = 'LOCAL_GOV_NOTICE';
    h.form.fire('submit'); await settle();
    h.one('[data-collected-page-next]').fire('click'); await settle();
    h.one('[data-collected-list]').children[1].fire('click'); await settle();
    // 미제출 입력은 조회 결과의 기준으로 저장하지 않는다.
    h.form.elements.keyword.value = 'not submitted';
    h.events.pagehide();
    const restored = await harness(h.win.history.state);
    restored.form.elements.keyword.value = 'browser stale value';
    await restored.show();
    const params = listCalls(restored).at(-1).searchParams;
    assert.equal(params.get('keyword'), 'SRC-017679');
    assert.equal(params.get('providerCode'), 'LOCAL_GOV_NOTICE');
    assert.equal(params.get('semanticStatusCode'), null);
    assert.equal(params.get('page'), '2');
    assert.equal(restored.form.elements.keyword.value, 'SRC-017679');
    assert.equal(restored.win.history.state.sanebCollectedList.sourceId, 'source-b');
    assert.equal(restored.tabs[3]['aria-selected'], 'true');
});

test('BFCache pageshow also restores applied filters before reloading', async () => {
    const h = await harness(); await h.show();
    h.form.elements.keyword.value = '양평'; h.form.fire('submit'); await settle();
    h.events.pagehide(); h.form.elements.keyword.value = '미제출';
    h.events.pageshow({persisted:true}); await settle();
    assert.equal(listCalls(h).at(-1).searchParams.get('keyword'), '양평');
    assert.equal(h.form.elements.keyword.value, '양평');
});

test('reset replaces saved search with defaults and leaves unrelated history intact', async () => {
    const h = await harness({otherFeature:'preserved'}); await h.show();
    h.form.elements.keyword.value = '양평'; h.form.fire('submit'); await settle();
    h.one('[data-collected-filter-reset]').fire('click'); await settle();
    const restored = await harness(h.win.history.state); await restored.show();
    assert.equal(listCalls(restored).at(-1).searchParams.get('keyword'), null);
    assert.equal(listCalls(restored).at(-1).searchParams.get('page'), '1');
    assert.equal(restored.win.history.state.otherFeature, 'preserved');
    assert.equal(restored.win.history.state.sanebCollectedList.view, 'ACTION_REQUIRED');
});

test('invalid saved view or page cannot override safe defaults', async () => {
    const h = await harness({sanebCollectedList:{version:1,view:'UNKNOWN',page:-2,filters:{keyword:'stale'}}}); await h.show();
    assert.equal(listCalls(h).at(-1).searchParams.get('page'), '1');
    assert.equal(listCalls(h).at(-1).searchParams.get('keyword'), null);
    assert.equal(h.win.history.state.sanebCollectedList.view, 'ACTION_REQUIRED');
});
