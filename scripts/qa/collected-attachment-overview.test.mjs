import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
const C = createRequire(import.meta.url)('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const source = (state = {}, extra = {}) => ({attachmentSummary: state, processingFlow: {statusCode:'NOT_APPLIED'}, ...extra});

test('unknown and discovery failure are never reported as no files', () => {
    assert.equal(C.attachmentOverview(null).status, '첨부 상태 미확인');
    assert.equal(C.attachmentOverview(source()).status, '첨부 확인 전');
    assert.equal(C.attachmentOverview(source({discoveryStatusCode:'DISCOVERY_FAILED',totalCount:0})).status, '첨부 확인 실패');
    assert.notEqual(C.attachmentOverview(source({discoveryStatusCode:'NO_FILES',totalCount:0})).status, '원문에서 첨부 없음 확인');
    assert.equal(C.attachmentOverview(source({discoveryStatusCode:'NO_FILES',totalCount:0,isDiscoveryComplete:true})).status, '원문에서 첨부 없음 확인');
});
test('processed counts are not download/extraction success counts', () => {
    const result = C.attachmentOverview(source({discoveryStatusCode:'FOUND',totalCount:3,processedCount:3,jobStatusCode:'PARTIAL_FAILED',errorCode:'EXTRACTION_FAILED'}));
    assert.equal(result.status,'첨부 3개 발견');
    assert.equal(result.job,'일부 실패');
    assert.match(result.progress,/파일 처리 3\/3개/);
    assert.match(result.error,/추출 실패/);
    assert.equal(result.applied,false);
    assert.match(result.guidance,/검수 저장이나 초안을 생성할 수 없습니다/);
});
test('stale and invalid counts remain explicit', () => {
    const result = C.attachmentOverview(source({isStale:true,totalCount:1,processedCount:2}));
    assert.equal(result.stale,true);
    assert.equal(result.progress,'파일 처리 건수 미확인');
});
test('legacy screen consumes V2 state without changing V1 attachment contract', async () => {
    const js = await readFile(new URL('../../src/main/resources/static/js/saneb-collected-announcements.js',import.meta.url),'utf8');
    const html = await readFile(new URL('../../src/main/resources/templates/app/collected-announcements.html',import.meta.url),'utf8');
    assert.match(js,/attachment-classification/);
    assert.match(js,/sequence !== detailSequence \|\| sourceId !== selectedSourceId/);
    assert.match(js,/source.sourceId !== sourceId/);
    assert.doesNotMatch(js,/수집된 첨부파일이 없습니다|첨부파일은 분류 판정에 사용하지 않습니다/);
    assert.ok(html.indexOf('saneb-attachment-review-core.js') < html.indexOf('saneb-collected-announcements.js'));
});

test('body refresh is explicit, scoped and keeps uncertain apply retries on the same preview', async () => {
    const js = await readFile(new URL('../../src/main/resources/static/js/saneb-collected-announcements.js',import.meta.url),'utf8');
    assert.match(js,/canManage && data.providerCode === "LOCAL_GOV_NOTICE"/);
    assert.match(js,/previewButton.addEventListener\("click"/);
    assert.match(js,/body-refresh-previews/);
    assert.match(js,/acknowledged.type = "checkbox"/);
    assert.match(js,/encodeURIComponent\(preview.previewId\)/);
    assert.match(js,/previewButton.disabled = !!error.uncertain/);
    assert.doesNotMatch(js,/innerHTML\s*=/);
});
