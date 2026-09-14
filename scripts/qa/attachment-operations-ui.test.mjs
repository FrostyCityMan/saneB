import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
const O = createRequire(import.meta.url)('../../src/main/resources/static/js/saneb-attachment-operations.js');
const id = n => `22222222-2222-4222-8222-${String(n).padStart(12, '0')}`, hash = 'a'.repeat(64);
const source = {sourceId: id(0), baseClassification: {decisionId: id(1)}, isAttachmentReviewRequired: true,
    effectiveClassification: {decisionId: id(2), setId: id(3), setHash: hash}, sourceVersion: 4, attachmentVersion: 7,
    attachmentSummary: {isStale: false, jobStatusCode: 'SUCCEEDED', isDiscoveryComplete: true, discoveryStatusCode: 'FOUND', totalCount: 2, processedCount: 2}};
const context = {version: O.version(source), policyId: id(4), policyHash: hash, executionHash: hash, modeCode: 'ENFORCE',
    maximumDownloadBytes: 1048576, maximumFileCount: 10, maximumAttempts: 3, maximumHttpRequests: 132,
    isAttachmentReviewRequired: true, effectCode: 'PRESERVE_ENFORCE_AND_STALE_CONFIRMATION'};
const data = {page: 1, totalPages: 1, totalCount: 2, items: [
    {fileId: id(11), setId: id(3), documentRoleCode: 'NOTICE', downloadStatusCode: 'SUCCEEDED', qualityCode: 'COMPLETE_TEXT'},
    {fileId: id(12), setId: id(3), documentRoleCode: 'FORM', downloadStatusCode: 'FAILED', qualityCode: null}]};
const input = {operation: 'COLLECT', maximumDownloadBytes: 1000, reason: ' 공개 공고 복구 ', acknowledged: true};
test('collection binds source/base/attachment versions, policy, execution and exact published limits', () => {
    assert.equal(O.matchesCollection(source, context), true);
    for (const k of Object.keys(context.version)) assert.equal(O.matchesCollection(source, {...context, version: {...context.version, [k]: 'wrong'}}), false, k);
    for (const [k, v] of Object.entries({policyId: null, policyHash: 'x', executionHash: 'x', modeCode: 'OFF', maximumDownloadBytes: 83886081,
        maximumFileCount: 11, maximumAttempts: 4, maximumHttpRequests: 200, isAttachmentReviewRequired: false, effectCode: 'COLLECT_PREVIEW_ONLY'}))
        assert.equal(O.matchesCollection(source, {...context, [k]: v}), false, k);
});
test('first collection accepts null attachment decision and never introduces enforce binding', () => {
    const first = {...source, isAttachmentReviewRequired: false, effectiveClassification: source.baseClassification, previewClassification: null};
    const ctx = {...context, version: O.version(first), isAttachmentReviewRequired: false, effectCode: 'COLLECT_PREVIEW_ONLY'};
    const command = O.prepare(first, ctx, null, input);
    assert.equal(command.payload.version.expectedAttachmentDecisionId, null);
    assert.equal(command.path, '/attachment-jobs/collection'); assert.equal(command.maximumHttpRequests, 132);
    assert.equal(command.payload.reason, '공개 공고 복구'); assert.equal('enforce' in command.payload, false);
});
test('current files require exact set, all files and processed counts, no stale or active job', () => {
    assert.equal(O.currentFiles(source, data), true);
    for (const item of [{...data, totalCount: 3}, {...data, totalPages: 2}, {...data, page: 2}, {...data, items: [data.items[0], data.items[0]]},
        {...data, items: data.items.map(f => ({...f, setId: id(9)}))}, {...data, items: []}]) assert.equal(O.currentFiles(source, item), false);
    for (const patch of [{isStale: true}, {jobStatusCode: 'RUNNING'}, {jobStatusCode: 'PENDING'}, {jobStatusCode: 'RETRY_WAIT'}, {processedCount: 1}])
        assert.equal(O.currentFiles({...source, attachmentSummary: {...source.attachmentSummary, ...patch}}, data), false);
});
test('preview file role change remains preview, no requirement for active collection context or new HTTP', () => {
    const preview = {...source, isAttachmentReviewRequired: false, effectiveClassification: source.baseClassification, previewClassification: source.effectiveClassification};
    const command = O.prepare(preview, null, data, {...input, operation: 'ROLE_CHANGE', fileRoles: data.items.map(f => ({fileId: f.fileId, documentRoleCode: 'GUIDE'}))});
    assert.equal(command.payload.version.expectedAttachmentDecisionId, id(2)); assert.equal(command.method, 'PUT');
    assert.equal(command.maximumHttpRequests, 0); assert.equal('maximumDownloadBytes' in command.payload, false);
});
test('retryability matches service quality matrix without retrying OCR, encrypted or blocked files', () => {
    for (const qualityCode of ['PARTIAL_TEXT', 'CORRUPT', 'LIMIT_EXCEEDED', 'TIMEOUT', 'FAILED', 'ISOLATION_UNAVAILABLE'])
        assert.equal(O.retryable({...data.items[0], qualityCode}), true);
    for (const qualityCode of ['COMPLETE_TEXT', 'OCR_REQUIRED', 'ENCRYPTED', 'UNSUPPORTED_FORMAT', null])
        assert.equal(O.retryable({...data.items[0], qualityCode}), false);
    assert.equal(O.retryable({...data.items[0], downloadStatusCode: 'CANCELLED'}), true);
    assert.equal(O.retryable({...data.items[0], downloadStatusCode: 'BLOCKED', qualityCode: 'FAILED'}), false);
});
test('selected retry binds fixed set and uses selected-only HTTP budget', () => {
    const command = O.prepare(source, context, data, {...input, operation: 'RETRY_FILES', fileIds: [id(12)]});
    assert.equal(command.maximumHttpRequests, 24); assert.equal(command.path, '/attachment-jobs');
    assert.deepEqual(command.payload.fileIds, [id(12)]); assert.equal(command.payload.expectedSetId, id(3));
    assert.equal(command.payload.version.expectedSetHash, hash);
    for (const fileIds of [[], [id(11)], [id(99)], [id(12), id(12)]])
        assert.throws(() => O.prepare(source, context, data, {...input, operation: 'RETRY_FILES', fileIds}), /실패 파일/);
    assert.throws(() => O.prepare({...source, attachmentSummary: {...source.attachmentSummary, isDiscoveryComplete: false}}, context, data,
        {...input, operation: 'RETRY_FILES', fileIds: [id(12)]}), /전체 발견/);
});
test('role change requires complete unique current files and at least one actual change', () => {
    const fileRoles = data.items.map(f => ({fileId: f.fileId, documentRoleCode: f.documentRoleCode}));
    assert.throws(() => O.prepare(source, null, data, {...input, operation: 'ROLE_CHANGE', fileRoles}), /다른 문서 역할/);
    for (const values of [[fileRoles[0]], [fileRoles[0], fileRoles[0]], [fileRoles[0], {...fileRoles[1], documentRoleCode: 'PARSER'}], [fileRoles[0], {...fileRoles[1], fileId: id(99)}]])
        assert.throws(() => O.prepare(source, null, data, {...input, operation: 'ROLE_CHANGE', fileRoles: values}), /전체 파일/);
});
test('budget and explicit impact acknowledgement are required before any operation', () => {
    for (const patch of [{maximumDownloadBytes: 0}, {maximumDownloadBytes: 1048577}, {maximumDownloadBytes: 1.5}, {maximumDownloadBytes: NaN}, {acknowledged: false}, {reason: ' '}, {reason: '가'.repeat(1001)}, {operation: 'PUBLISH'}])
        assert.throws(() => O.prepare(source, context, data, {...input, ...patch}));
});
test('operation UI preserves input, locks other writes and uses scoped existing APIs only', async () => {
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-attachment-operations.js', import.meta.url), 'utf8');
    for (const forbidden of ['innerHTML', 'localStorage', 'sessionStorage', 'console.log', 'eval(', '/publication', 'setInterval', 'fetch(']) assert.equal(script.includes(forbidden), false, forbidden);
    for (const expected of ['Idempotency-Key', 'attempt.uncertain', 'data-operation-result', '현재 집합', '동일 요청', 'ticket !== generation']) assert.ok(script.includes(expected));
    const main = await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js', import.meta.url), 'utf8');
    assert.ok(main.includes('operations?.busy')); assert.ok(main.includes('operations?.uncertain')); assert.ok(main.includes('operations.load(null)'));
    assert.ok(main.includes('!operations?.stale'));
    const submitGuard = main.match(/if \(busy \|\| operations\?\.busy[^\n]+!canManage\) return;/)?.[0];
    assert.ok(submitGuard);
    for (const condition of ['operations?.uncertain', 'operations?.stale', 'recovery?.busy', 'recovery?.uncertain', 'recovery?.stale', '!canManage'])
        assert.ok(submitGuard.includes(condition), condition);
});
