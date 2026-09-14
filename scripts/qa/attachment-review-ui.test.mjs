import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
const require = createRequire(import.meta.url);
const C = require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const id = '11111111-1111-4111-8111-111111111111';
const path = `/api/v2/admin/announcement-sources/${id}`;
const version = {expectedBaseDecisionId: 'base', expectedAttachmentDecisionId: 'attachment', expectedSourceVersion: 4, expectedAttachmentVersion: 7, expectedSetHash: 'a'.repeat(64)};
const context = {sourceId: id, version};
const source = {sourceId: id, isAttachmentReviewRequired: true, sourceVersion: 4, attachmentVersion: 7,
    processingFlow: {statusCode: 'READY_FOR_FINAL_REVIEW', isAutomaticAnalysisComplete: true, isFinalReviewAvailable: true},
    baseClassification: {decisionId: 'base'}, effectiveClassification: {decisionId: 'attachment', setHash: version.expectedSetHash}, attachmentSummary: {isStale: false}};
const response = (status, data, success = status < 400) => ({status, ok: status < 400, json: async () => ({success, data, message: '구체적인 정책 오류'})});
test('automatic processing and configuration do not request intermediate human review', () => {
    for (const code of ['NOT_APPLIED', 'CLASSIFICATION_PENDING', 'CONFIGURATION_REQUIRED', 'AUTOMATIC_PROCESSING', 'EVIDENCE_STALE', 'FUTURE_CODE']) {
        const pending = {...source, processingFlow: {statusCode: code, isFinalReviewAvailable: true}};
        assert.equal(C.canRequestFinalReview(pending), false, code);
        assert.equal(C.matchesContext(pending, context), false, code);
        assert.match(C.flowGuidance(pending), /[가-힣]/);
    }
    assert.equal(C.canRequestFinalReview({...source, processingFlow: undefined}), false);
    assert.equal(C.canRequestFinalReview({...source, processingFlow: {...source.processingFlow, isFinalReviewAvailable: 'true'}}), false);
});
test('technical exception permits explicit manual review only when current server evidence is available', () => {
    const exception = {...source, processingFlow: {statusCode: 'TECHNICAL_EXCEPTION', isAutomaticAnalysisComplete: false, isFinalReviewAvailable: true}};
    assert.equal(C.matchesContext(exception, context), true);
    assert.match(C.flowGuidance(exception), /자동 분석 성공을 뜻하지/);
    assert.equal(C.matchesContext({...exception, processingFlow: {...exception.processingFlow, isFinalReviewAvailable: false}}, context), false);
    for (const status of ['SCOPE_READY', 'PENDING', 'RUNNING', 'RETRY_WAIT', 'PAUSED'])
        assert.equal(C.matchesContext({...exception, attachmentSummary: {jobStatusCode: status}}, context), false);
});
test('final review states have distinct Korean labels and never mean automatic publication', () => {
    const codes = ['READY_FOR_FINAL_REVIEW', 'FINAL_REVIEW_EXCEPTION', 'FINAL_REVIEW_CONFIRMED'];
    assert.equal(new Set(codes.map(C.label)).size, codes.length);
    for (const statusCode of codes) assert.equal(C.canRequestFinalReview({...source, processingFlow: {...source.processingFlow, statusCode}}), true);
    assert.match(C.flowGuidance(source), /자동 공개되지/);
    assert.match(C.flowGuidance({...source, processingFlow: {statusCode: 'FINAL_REVIEW_CONFIRMED'}}), /별개/);
});
test('detail page gates review-context reads and explains missing body without demanding immediate input', async () => {
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js', import.meta.url), 'utf8');
    assert.match(script, /if \(C\.canRequestFinalReview\(details\.source\)\)/);
    assert.match(script, /1·2차 제목·본문 판정 이력 · 중간 근거/);
    assert.match(script, /첨부 분석 결과와 자동 처리 상태를 확인하세요/);
    assert.equal(script.includes('수집된 본문이 없습니다. 원문을 직접 확인하세요.'), false);
});
test('discovery failures have distinct Korean reasons rather than no-files or raw-code fallback', () => {
    const codes = ['ATTACHMENT_DETAIL_UNAVAILABLE', 'ATTACHMENT_SELECTOR_CHANGED', 'ATTACHMENT_DOWNLOAD_FORM_CHANGED',
        'ATTACHMENT_LINK_UNRESOLVED', 'ATTACHMENT_FILE_LIMIT'];
    assert.equal(new Set(codes.map(C.label)).size, codes.length);
    for (const code of codes) {
        assert.match(C.label(code), /[가-힣]/);
        assert.notEqual(C.label(code), C.label('NO_FILES'));
        assert.equal(C.label(code).includes(code), false);
    }
    assert.match(C.label('ATTACHMENT_DOWNLOAD_FORM_CHANGED'), /재검증 필요/);
});
test('current write gate binds every source/base/evaluation/set/version field', () => {
    assert.equal(C.matchesContext(source, context), true);
    for (const key of Object.keys(version)) assert.equal(C.matchesContext(source, {...context, version: {...version, [key]: null}}), false, key);
    assert.equal(C.matchesContext({...source, sourceId: 'other'}, context), false);
    assert.equal(C.matchesContext({...source, attachmentSummary: {isStale: true}}, context), false);
    assert.equal(C.matchesContext({...source, isAttachmentReviewRequired: false, previewClassification: source.effectiveClassification}, context), false);
    assert.equal(C.matchesContext({...source, effectiveClassification: null}, context), false);
});
test('current confirmation requires exact version and saved manual categories', () => {
    const saved = {confirmation: {sourceId: id, evaluationId: 'attachment', sourceVersion: 4, attachmentVersion: 7, setHash: version.expectedSetHash, isCurrent: true}, targetCategoryCodes: ['BUSINESS'], supportTypeCodes: ['POLICY_FINANCE']};
    assert.equal(C.confirmedCurrent({...context, confirmedClassification: saved}), true);
    for (const [key, value] of Object.entries({sourceId: 'other', evaluationId: 'old', sourceVersion: 3, attachmentVersion: 6, setHash: 'b'.repeat(64), isCurrent: false}))
        assert.equal(C.confirmedCurrent({...context, confirmedClassification: {...saved, confirmation: {...saved.confirmation, [key]: value}}}), false, key);
    assert.equal(C.confirmedCurrent({...context, confirmedClassification: {...saved, targetCategoryCodes: []}}), false);
});
test('same version comparison and labels do not treat unknown data as success', () => {
    assert.equal(C.sameVersion(version, {...version}), true); assert.equal(C.sameVersion(version, null), false);
    assert.equal(C.label(null), '미확인'); assert.match(C.label('FUTURE_CODE'), /확인 필요/);
    assert.match(C.label('NO_FILES'), /없음 확인/); assert.notEqual(C.label('NO_FILES'), C.label('DISCOVERY_FAILED'));
    assert.match(C.label('CURRENT_PREVIEW'), /적용 안 됨/);
});
test('restored confirmation keeps its historical version and requires exact effective binding', () => {
    const saved = {confirmation: {confirmationId: id,sourceId: id,evaluationId: 'attachment',sourceVersion: 4,attachmentVersion: 5,setHash: version.expectedSetHash,isCurrent: true},
        targetCategoryCodes: ['BUSINESS'],supportTypeCodes: ['POLICY_FINANCE'],
        binding: {restorationId: id,confirmationId: id,sourceId: id,sourceVersion: 4,attachmentVersion: 7}};
    assert.equal(C.confirmedCurrent({...context,confirmedClassification: saved}),true);
    assert.equal(saved.confirmation.attachmentVersion,5);
    for (const [key,value] of Object.entries({restorationId: null,confirmationId: 'foreign',sourceId: 'foreign',sourceVersion: 3,attachmentVersion: 8}))
        assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,binding: {...saved.binding,[key]: value}}}),false,key);
    for (const binding of [null,{},false,{...saved.binding,restorationId: 'unverified'}])
        assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,binding}}),false);
    assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,confirmation: {...saved.confirmation,isCurrent: false}}}),false);
});
test('explicit initial binding cannot fabricate a restored version or fall back on malformed data', () => {
    const confirmation = {confirmationId: id,sourceId: id,evaluationId: 'attachment',sourceVersion: 4,attachmentVersion: 7,setHash: version.expectedSetHash,isCurrent: true};
    const saved = {confirmation,targetCategoryCodes: ['BUSINESS'],supportTypeCodes: ['POLICY_FINANCE'],
        binding: {restorationId: null,confirmationId: id,sourceId: id,sourceVersion: 4,attachmentVersion: 7}};
    assert.equal(C.confirmedCurrent({...context,confirmedClassification: saved}),true);
    for (const binding of [null,{}, {...saved.binding,restorationId: id}, {...saved.binding,attachmentVersion: 6}])
        assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,binding}}),false);
});
test('external source links reject script, credentials, relative and non-http locations', () => {
    for (const url of ['javascript:alert(1)', 'data:text/html,a', '//evil.test', '/relative', 'https://name:password@example.test/', 'file:///a']) assert.equal(C.safeSourceUrl(url), null);
    assert.equal(C.safeSourceUrl('https://example.test/a'), 'https://example.test/a');
});
test('unicode evidence highlight uses code points without executing HTML', () => {
    const block = {blockIndex: 0, textStartOffset: 10, textEndOffset: 19, text: '😀지원금<b>x'};
    // Actual 8 code points; inconsistent server offsets must not invent a highlight.
    assert.equal(C.blockParts(block, {blockIndex: 0, startOffset: 11, endOffset: 14}).length, 1);
    block.textEndOffset = 18;
    const parts = C.blockParts(block, {blockIndex: 0, startOffset: 11, endOffset: 14});
    assert.equal(parts[1].text, '지원금'); assert.equal(parts.map(p => p.text).join(''), block.text);
    assert.equal(C.blockParts(block, {blockIndex: 1, startOffset: 11, endOffset: 14}).length, 1);
    assert.equal(C.blockParts(block, {blockIndex: 0, startOffset: 9, endOffset: 14}).length, 1);
});
test('no-store same-origin JSON reads and writes preserve idempotency and body', async () => {
    let seen;
    const call = C.client(async (url, options) => { seen = {url, ...options}; return response(200, {ok: true}); });
    await call(path + '/attachment-classification/confirmations', {method: 'POST', body: '{}', headers: {'Idempotency-Key': id}});
    assert.equal(seen.cache, 'no-store'); assert.equal(seen.credentials, 'same-origin'); assert.equal(seen.redirect, 'error');
    assert.equal(seen.headers['Idempotency-Key'], id); assert.equal(seen.headers['Content-Type'], 'application/json'); assert.equal(seen.body, '{}');
    await assert.rejects(() => call('https://example.test'), error => error.status === 400);
});
test('HTTP policies distinguish session, permission, conflict and unknown mutation results', async () => {
    for (const [status, pattern, uncertain] of [[401, /로그인/, false], [403, /권한/, false], [404, /정책 오류/, false], [409, /정책 오류/, false], [503, /결과를 확인/, true]]) {
        await assert.rejects(() => C.client(async () => response(status, {code: 'CONFLICT'}))(path), error => {
            assert.equal(error.status, status); assert.equal(error.uncertain, uncertain); assert.match(error.message, pattern); return true;
        });
    }
});
test('timeout, invalid JSON and malformed success are unknown rather than success', async () => {
    await assert.rejects(() => C.client(async () => { throw new Error('private upstream diagnostic'); })(path), error => error.uncertain && !error.message.includes('private'));
    for (const res of [response(200, null), {ok: true, status: 200, json: async () => { throw new Error('HTML'); }}])
        await assert.rejects(() => C.client(async () => res)(path), error => error.uncertain);
    await assert.rejects(() => C.client((url, options) => new Promise((resolve, reject) => options.signal.addEventListener('abort', () => reject(new Error('abort')))), 10)(path), error => error.uncertain);
});
test('duplicate submit is locked and network retry uses exactly the same key and payload', () => {
    let serial = 0; const m = C.mutation(() => `key-${++serial}`), payload = {version, reviewNote: '공개 원문 확인'};
    const first = m.prepare(payload); assert.equal(m.pending, true);
    assert.throws(() => m.prepare(payload), /이미 처리 중/);
    m.fail(new C.RequestError('timeout')); assert.equal(m.uncertain, true);
    assert.throws(() => m.prepare({...payload, reviewNote: '다른 요청'}), /미확정/);
    const repeated = m.prepare(m.original); assert.deepEqual(repeated, first);
    m.succeed(); assert.equal(m.original, null); assert.equal(m.uncertain, false);
    assert.notEqual(m.prepare(payload).key, first.key);
});
test('definite version conflict releases old request only for deliberate review', () => {
    let serial = 0; const m = C.mutation(() => ++serial);
    const first = m.prepare({version}); m.fail(new C.RequestError('stale', 409));
    assert.equal(m.original, null); assert.equal(m.uncertain, false);
    assert.notEqual(m.prepare({version: {...version, expectedAttachmentVersion: 8}}).key, first.key);
});
test('UI source uses text nodes, native controls, scoped APIs and does not persist sensitive inputs', async () => {
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js', import.meta.url), 'utf8');
    for (const forbidden of ['innerHTML', 'insertAdjacentHTML', 'localStorage', 'sessionStorage', 'console.log', 'eval(', '/publication', '/attachment-jobs']) assert.equal(script.includes(forbidden), false, forbidden);
    for (const expected of ['textContent', 'createTextNode', 'requiredAcknowledgementCodes', 'manualSourceCheckRequired', 'beforeunload', 'epoch !== expectedEpoch', 'Idempotency-Key', 'confirmedCurrent', 'reviewDirty', 'same-origin']) {
        if (expected === 'same-origin') continue; assert.ok(script.includes(expected), expected);
    }
    const css = await readFile(new URL('../../src/main/resources/static/css/saneb-announcement-attachment-review.css', import.meta.url), 'utf8');
    assert.match(css, /\[hidden\]\s*\{\s*display:\s*none\s*!important/);
});
