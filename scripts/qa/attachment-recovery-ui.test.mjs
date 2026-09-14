import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
import {setImmediate as tick} from 'node:timers/promises';
const require = createRequire(import.meta.url);
const R = require('../../src/main/resources/static/js/saneb-attachment-recovery.js');
const C = require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const id = n => `33333333-3333-4333-8333-${String(n).padStart(12, '0')}`;
const source = {sourceId: id(1), sourceVersion: 2, attachmentVersion: 7};
const preview = {sourceId: id(1), jobId: id(2), sourceVersion: 2, attachmentVersion: 7, modeCode: 'APPLIED', jobStatusCode: 'SUCCEEDED',
    applicationStatusCode: 'APPLIED', rollbackStatusCode: 'NOT_REQUESTED', readinessCode: 'READY', previewHash: 'a'.repeat(64),
    baseReopens: false, confirmationRestores: true, staleConfirmationRemains: false, targetCount: 1, currentHttpRequests: 0};
const input = {reason: ' 이전 연결 복원 ', acknowledged: true};
const row = {sourceId: id(1), jobId: id(2), operationCode: 'COLLECT', jobStatusCode: 'SUCCEEDED', applicationStatusCode: 'APPLIED',
    rollbackStatusCode: 'NOT_REQUESTED', actionId: null, createdAt: '2026-09-11T12:00:00+09:00'};
const result = {actionId: id(3), sourceId: id(1), jobId: id(2), modeCode: 'APPLIED', statusCode: 'ROLLED_BACK', restoredSourceVersion: 2,
    restoredAttachmentVersion: 8, baseReopened: false, confirmationRestored: true, targetCount: 1, currentHttpRequests: 0, recordedAt: '2026-09-11T12:01:00+09:00'};
const pageOf = (items = [row], page = 1, totalCount = items.length) => ({items, page, size: 10, totalCount, totalPages: Math.ceil(totalCount / 10)});

test('approval uses exact current job, versions, hash and effects with an explicit confirmation', () => {
    const payload = R.prepare(source, id(2), preview, input);
    assert.deepEqual(payload, {expectedSourceVersion: 2, expectedAttachmentVersion: 7, expectedPreviewHash: 'a'.repeat(64),
        expectedBaseReopen: false, expectedConfirmationRestore: true, acknowledgeBindingRestoration: true, reason: '이전 연결 복원'});
    for (const patch of [{reason: ''}, {reason: ' '}, {reason: '가'.repeat(1001)}, {acknowledged: false}, {acknowledged: 'true'}])
        assert.throws(() => R.prepare(source, id(2), preview, {...input, ...patch}));
});
test('every refusal and mismatched effect or current version prevents approval', () => {
    for (const patch of [{sourceId: id(9)}, {jobId: id(9)}, {sourceVersion: 3}, {attachmentVersion: 8}, {previewHash: 'wrong'},
        {baseReopens: true}, {staleConfirmationRemains: true}, {confirmationRestores: 'true'}, {targetCount: 2}, {currentHttpRequests: 1},
        {readinessCode: 'NEW_UNKNOWN'}, {modeCode: 'ENFORCE'}, {jobStatusCode: 'RUNNING'}, {applicationStatusCode: 'PENDING'}, {rollbackStatusCode: 'ROLLED_BACK'}])
        assert.equal(R.ready(source, id(2), {...preview, ...patch}), false, JSON.stringify(patch));
    for (const readinessCode of ['CURRENT_BINDING_CHANGED', 'PREVIOUS_BINDING_INVALID', 'JOB_NOT_TERMINAL', 'RECOVERY_EVIDENCE_MISSING', 'ALREADY_RECOVERED'])
        assert.throws(() => R.prepare(source, id(2), {...preview, readinessCode}, input));
    assert.equal(R.ready({...source, attachmentVersion: 2147483647}, id(2), {...preview, attachmentVersion: 2147483647}), false);
});
test('failed reservations and partial applied jobs retain their original failure semantics', () => {
    for (const jobStatusCode of ['FAILED', 'CONFLICT', 'CANCELLED']) {
        const p = {...preview, modeCode: 'FAILED_RESERVATION', jobStatusCode, applicationStatusCode: 'PENDING', baseReopens: true, confirmationRestores: false};
        assert.equal(R.ready(source, id(2), p), true); assert.equal(R.prepare(source, id(2), p, input).expectedBaseReopen, true);
        assert.equal('jobStatusCode' in R.prepare(source, id(2), p, input), false);
    }
    assert.equal(R.ready(source, id(2), {...preview, jobStatusCode: 'PARTIAL_FAILED'}), true);
    assert.equal(R.ready(source, id(2), {...preview, modeCode: 'FAILED_RESERVATION', applicationStatusCode: 'PENDING', jobStatusCode: 'RETRY_WAIT'}), false);
});
test('receipt validation requires matching immutable outcome, not a future current-state assumption', () => {
    const sent = {modeCode: preview.modeCode, payload: R.prepare(source, id(2), preview, input)};
    assert.equal(R.validReceipt(result, id(1), id(2), sent), true);
    for (const patch of [{actionId: 'bad'}, {sourceId: id(9)}, {jobId: id(9)}, {modeCode: 'FAILED_RESERVATION'}, {statusCode: 'PENDING'},
        {restoredSourceVersion: 1}, {restoredAttachmentVersion: 9}, {baseReopened: true}, {confirmationRestored: false}, {targetCount: 2}, {currentHttpRequests: 1}, {recordedAt: null}])
        assert.equal(R.validReceipt({...result, ...patch}, id(1), id(2), sent), false);
    assert.equal(R.validReceipt(result, id(1), id(2), null, id(9)), false);
    assert.equal(R.validReceipt(result, id(1), id(2), null, id(3)), true);
});
test('list metadata binds source scope, complete page counts and distinct receipt linkage', () => {
    assert.equal(R.validJobs(pageOf(), id(1), 1), true); assert.equal(R.validJobs(pageOf([], 1), id(1), 1), true);
    for (const data of [{...pageOf(), totalCount: 2}, {...pageOf(), totalPages: 2}, {...pageOf(), size: 100}, pageOf([row, row]),
        pageOf([{...row, sourceId: id(9)}]), pageOf([{...row, actionId: id(3)}])])
        assert.equal(R.validJobs(data, id(1), 1), false);
    assert.equal(R.validJobs(pageOf([{...row, rollbackStatusCode: 'ROLLED_BACK', actionId: id(3)}]), id(1), 1), true);
    assert.equal(R.validJobs(pageOf([{...row, rollbackStatusCode: 'ROLLED_BACK', actionId: null}]), id(1), 1), true); // 과거 영수증 부재를 새 영수증으로 추정하지 않는다.
});

// Node-only interaction harness: real mount/state functions, minimal event/DOM ports. Not rendered-browser evidence.
class Element {
    constructor(tag = 'div') { this.tag = tag; this.children = []; this.events = {}; this.disabled = false; this.hidden = false; this.textContent = ''; }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = [...children]; }
    querySelectorAll(selector) { return this.children.flatMap(n => [...(n.tag === selector ? [n] : []), ...n.querySelectorAll(selector)]); }
    addEventListener(type, action) { this.events[type] = action; }
    async fire(type, event = {}) { const r = this.events[type]?.({preventDefault() {}, ...event}); await r; await tick(); }
    focus() { this.focused = true; }
}
function harness(options = {}) {
    const nodes = new Map(), calls = [], navigationWrites = [];
    const q = selector => { if (!nodes.has(selector)) nodes.set(selector, new Element()); return nodes.get(selector); };
    const form = q('[data-recovery-form]'); form.elements = {reason: {value: input.reason}, restorationAcknowledged: {checked: false}};
    form.reportValidity = () => true;
    let extraBlock = false, refreshCount = 0;
    const request = async (url, config = {}) => {
        calls.push({url, ...config});
        if (options.request) return options.request(url, config);
        if (url.includes('/attachment-recovery-jobs')) return pageOf();
        if (url.endsWith('/preview')) return preview;
        return result;
    };
    const api = R.mount({page: {querySelector: q}, C, request, apiRoot: `/api/v2/admin/announcement-sources/${id(1)}`,
        canRollback: options.canRollback ?? true, text: (parent, tag, value) => { const e = new Element(tag); e.textContent = value; parent.append(e); return e; },
        meta: (parent, pairs) => { parent.pairs = pairs; }, message: (selector, value, focus = false) => { const e = q(selector); e.textContent = value; e.hidden = !value; if (focus) e.focus(); },
        date: value => value, changed: () => {}, blocked: () => extraBlock, refresh: async () => { refreshCount++; },
        navigation: {read: () => options.navigation || {page: 1, jobId: null}, write: (...values) => navigationWrites.push(values)}});
    return {api, q, calls, form, navigationWrites, get refreshCount() { return refreshCount; }, block(value) { extraBlock = value; },
        async load() { await api.load(source); api.gates(); }, async choose() { await q('[data-recovery-jobs]').querySelectorAll('button')[0].fire('click'); },
        async submit() { form.elements.restorationAcknowledged.checked = true; await form.fire('submit'); }};
}
test('mount loads history then preview without writes; explicit approval returns receipt and refreshes current binding', async () => {
    const h = harness(); await h.load(); assert.equal(h.q('[data-recovery-submit]').disabled, true);
    await h.choose(); assert.equal(h.q('[data-recovery-submit]').disabled, false); assert.equal(h.q('[data-recovery-preview-title]').focused, true);
    assert.equal(h.calls.some(c => c.method), false); await h.submit();
    const post = h.calls.find(c => c.method === 'POST'); assert.ok(post.headers['Idempotency-Key']);
    assert.equal(JSON.parse(post.body).expectedAttachmentVersion, 7); assert.equal(h.refreshCount, 1);
    assert.match(h.q('[data-recovery-result-state]').textContent, /원복 완료 기록/); assert.equal(h.api.stale, true); assert.equal(h.api.dirty, false);
});
test('read-only roles can view history, preview and receipt but cannot submit even via a forged event', async () => {
    const h = harness({canRollback: false, request: async url => url.includes('recovery-jobs') ? pageOf([{...row, rollbackStatusCode: 'ROLLED_BACK', actionId: id(3)}]) : url.endsWith('/preview') ? preview : result});
    await h.load(); await h.choose(); await h.submit();
    assert.equal(h.q('[data-recovery-submit]').disabled, true); assert.equal(h.calls.some(c => c.method), false);
    await h.q('[data-recovery-jobs]').querySelectorAll('button')[1].fire('click'); assert.match(h.q('[data-recovery-result-state]').textContent, /원복 완료 기록/);
});
test('response loss then session/permission/conflict errors preserve identical key and body and block other actions', async () => {
    let writes = 0;
    const h = harness({request: async (url, config) => {
        if (url.includes('recovery-jobs')) return pageOf(); if (url.endsWith('/preview')) return preview;
        writes++; if (writes <= 4) throw new C.RequestError('미확정 또는 세션 확인', [0, 401, 403, 409][writes - 1]); return result;
    }});
    await h.load(); await h.choose(); await h.submit(); assert.equal(h.api.uncertain, true); assert.equal(h.q('[data-recovery-load]').disabled, true);
    const count = h.calls.length; await h.api.load(null); await h.q('[data-recovery-load]').fire('click'); assert.equal(h.calls.length, count);
    for (let n = 0; n < 3; n++) { await h.q('[data-recovery-retry]').fire('click'); assert.equal(h.api.uncertain, true); }
    await h.q('[data-recovery-retry]').fire('click');
    const posts = h.calls.filter(c => c.method === 'POST'); assert.equal(posts.length, 5);
    assert.equal(new Set(posts.map(c => c.headers['Idempotency-Key'])).size, 1); assert.equal(new Set(posts.map(c => c.body)).size, 1);
    assert.equal(h.api.uncertain, false); assert.equal(h.refreshCount, 1);
});
test('mismatched success receipt is uncertain and cannot unlock unrelated writes or reset approval', async () => {
    const h = harness({request: async url => url.includes('recovery-jobs') ? pageOf() : url.endsWith('/preview') ? preview : {...result, restoredAttachmentVersion: 1}});
    await h.load(); await h.choose(); await h.submit(); assert.equal(h.api.uncertain, true); assert.equal(h.api.stale, true);
    assert.equal(h.refreshCount, 0); assert.equal(h.q('[data-recovery-receipt]').pairs, undefined);
});
test('definite conflict preserves reason, locks approval and requires a new current-source refresh', async () => {
    const h = harness({request: async (url, options) => { if (options.method) throw new C.RequestError('새 검수가 있습니다.', 409); return url.includes('recovery-jobs') ? pageOf() : preview; }});
    await h.load(); await h.choose(); await h.submit(); assert.equal(h.api.uncertain, false); assert.equal(h.api.stale, true);
    assert.equal(h.form.elements.reason.value, input.reason); assert.equal(h.q('[data-recovery-submit]').disabled, true);
    await h.load(); assert.equal(h.form.elements.restorationAcknowledged.checked, false); assert.equal(h.form.elements.reason.value, input.reason);
});
test('preview failure or stale source version never creates an approval and is not a successful empty result', async () => {
    const h = harness({request: async url => url.includes('recovery-jobs') ? pageOf() : {...preview, attachmentVersion: 8}});
    await h.load(); await h.choose(); await h.submit(); assert.equal(h.calls.some(c => c.method), false);
    assert.match(h.q('[data-recovery-state]').textContent, /조회 미완료/); assert.equal(h.q('[data-recovery-submit]').disabled, true);
});
test('late read response cannot restore controls after source lookup was invalidated', async () => {
    let resolve; const h = harness({request: () => new Promise(r => { resolve = r; })});
    const loading = h.api.load(source); await h.api.load(null); resolve(pageOf()); await loading;
    assert.equal(h.q('[data-recovery-jobs]').children.length, 0); assert.equal(h.api.busy, false); assert.equal(h.q('[data-recovery-submit]').disabled, true);
});
test('pagination restores non-sensitive location and falls back from a deleted last page', async () => {
    const h = harness({navigation: {page: 2, jobId: id(2)}, request: async url => url.includes('page=2') ? pageOf([], 2, 1) : url.includes('recovery-jobs') ? pageOf() : preview});
    await h.load(); assert.equal(h.calls.length, 3); assert.match(h.q('[data-recovery-list-status]').textContent, /1\/1페이지/);
    assert.deepEqual(h.navigationWrites.at(-1), [1, id(2)]); assert.equal(h.form.elements.restorationAcknowledged.checked, false);
});
test('other mutations block recovery reads and writes; editing reason clears acknowledgement without losing input', async () => {
    const h = harness(); await h.load(); await h.choose(); h.block(true); h.api.gates(); const count = h.calls.length;
    await h.submit(); await h.q('[data-recovery-load]').fire('click'); assert.equal(h.calls.length, count);
    h.block(false); h.form.elements.restorationAcknowledged.checked = true; await h.form.fire('input', {target: {name: 'reason'}});
    assert.equal(h.form.elements.restorationAcknowledged.checked, false); assert.equal(h.api.dirty, true); assert.equal(h.form.elements.reason.value, input.reason);
});
test('list failure has a retry path and cannot leave a phantom running state', async () => {
    const h = harness({request: async () => { throw new C.RequestError('로그인 확인', 401); }}); await h.load();
    assert.match(h.q('[data-recovery-list-status]').textContent, /목록 조회 실패/); assert.equal(h.q('[data-recovery-load]').disabled, false);
    assert.equal(h.q('[data-recovery-submit]').disabled, true); assert.equal(h.api.busy, false);
});
test('recovery UI uses scoped APIs, text rendering, native controls and shared mutation locks', async () => {
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-attachment-recovery.js', import.meta.url), 'utf8');
    for (const forbidden of ['innerHTML', 'localStorage', 'sessionStorage', 'console.log', 'eval(', '/publication', 'setInterval', 'fetch(', 'window.confirm'])
        assert.equal(script.includes(forbidden), false, forbidden);
    const main = await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js', import.meta.url), 'utf8');
    for (const text of ['recovery?.busy', 'recovery?.uncertain', 'recovery?.stale', 'recovery.load(null)', 'recovery.dirty', 'recoveryPage', 'recoveryJob']) assert.ok(main.includes(text));
});
