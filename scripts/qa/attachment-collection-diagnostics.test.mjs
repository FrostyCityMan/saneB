import test from 'node:test';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {importReceipt, importLocalCollectionReport, selectLatestSamples} from './attachment-collection-receipts.mjs';
import {readCollectionDiagnostics} from './attachment-collection-diagnostics.mjs';
const hash = 'a'.repeat(64), other = 'b'.repeat(64);
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const inventoryPath = 'build/reports/attachment-target-inventory/operating-targets-vs-local-code.json';
const target = {providerCode: 'LOCAL_GOV_NOTICE', localSourceCode: 'LGS-000001', listParserProfileCode: 'SPRING_BBS',
  enabled: true, bindingStatus: 'SYSTEM_BINDING_MATCHED', profiles: [{profileCode: 'PROFILE_1', profileHash: hash}]};
const base = {scope: 'OFFICIAL_THREE_STAGE_OBSERVATION_V1', collectionOnly: true, isExtractionVerified: false,
  isWholeTextAnalysisComplete: false, isPolicyQaPassed: false, isExpectationApproved: false, productionWriteCount: 0,
  observedAt: '2026-10-01T01:00:00Z', profileCode: 'PROFILE_1', profileHash: hash, caseCode: 'CASE-1',
  titleStage: 'COMBINATION_MATCHED', expectedListedFileCount: 1, detailIdentityVerified: true,
  discoveryStatus: 'FOUND', discoveryComplete: true, originalFilesRemoved: true,
  status: 'COLLECTION_ONLY_OBSERVED_NOT_APPROVED', collectionStageComplete: true, bodyStatus: 'AVAILABLE',
  files: [{locatorHash: hash, status: 'DOWNLOADED', downloadAllowed: true, format: 'PDF', binaryHash: hash, bytes: 512}]};
function fixture(reports = [base], targets = [target]) {
  const inventory = {scope: 'OPERATING_TARGET_SNAPSHOT_VS_LOCAL_CODE_REGISTRY', targets};
  const inventoryBytes = Buffer.from(JSON.stringify(inventory));
  const data = new Map([[inventoryPath, inventoryBytes]]);
  const imported = [];
  const receipts = reports.map((report, i) => {
    const path = `build/reports/attachment-regional-collection/CASE-${i}.json`, bytes = Buffer.from(JSON.stringify(report));
    data.set(path, bytes);
    const receipt = importLocalCollectionReport(bytes, inventory, hash);
    imported.push(receipt);
    return {kind: 'LOCAL_COLLECTION_ONLY', path, receiptHash: sha(bytes), producerClassHash: hash,
      status: receipt.status, sampleCount: receipt.samples.length};
  });
  const index = {inventoryPath, inventorySha256: sha(inventoryBytes), receipts, samples: selectLatestSamples(imported)};
  return {index, data, run: () => readCollectionDiagnostics(index, path => data.get(path))};
}
test('body failure remains visible without discarding a successfully downloaded file', () => {
  const result = fixture([{...base, bodyStatus: 'FETCH_FAILED', bodyFailureCode: 'BODY_TEXT_EMPTY'}]).run();
  assert.equal(result.availabilitySummary.observedDownloadRegionCount, 1);
  assert.equal(result.availabilitySummary.regionsWithErrors, 0);
  assert.equal(result.diagnosticSummary.successfulDownloadRegionsWithBodyFetchFailure, 1);
  assert.equal(result.regions[0].downloadedFileObservations, 1);
  assert.equal(result.regions[0].issues[0].stage, 'BODY_FETCH');
  assert.equal(result.regions[0].issues[0].code, 'BODY_TEXT_EMPTY');
});
test('discovery warnings and file transport failures remain distinct', () => {
  const result = fixture([{...base, collectionStageComplete: false, status: 'COLLECTION_ONLY_PARTIAL_NOT_APPROVED',
    discoveryComplete: false, discoveryWarningCodes: ['ATTACHMENT_LINK_UNRESOLVED'],
    files: [{locatorHash: hash, status: 'FAILED', bytes: 0, failedStage: 'FILE_DOWNLOAD', failureCode: 'TRANSPORT_TIMEOUT'}]}]).run();
  assert.deepEqual(result.regions[0].issues.map(i => [i.stage, i.code]),
    [['ATTACHMENT_DISCOVERY', 'ATTACHMENT_LINK_UNRESOLVED'], ['FILE_DOWNLOAD', 'TRANSPORT_TIMEOUT']]);
  assert.equal(result.availabilitySummary.observedDownloadRegionCount, 0);
});
test('본문 TIMEOUT과 상세 TRANSPORT_TIMEOUT을 원래 단계별 코드로 보존한다', () => {
  const result = fixture([{...base, bodyStatus: 'FETCH_FAILED', bodyFailureCode: 'TIMEOUT',
    failedStage: 'DETAIL_DISCOVERY', failureCode: 'TRANSPORT_TIMEOUT'}]).run();
  assert.deepEqual(result.regions[0].issues.map(i => [i.stage, i.code]),
    [['BODY_FETCH', 'TIMEOUT'], ['DETAIL_DISCOVERY', 'TRANSPORT_TIMEOUT']]);
  assert.equal(result.availabilitySummary.observedDownloadRegionCount, 1);
  assert.equal(result.diagnosticSummary.successfulDownloadRegionsWithBodyFetchFailure, 1);
});
test('본문 전용 TIMEOUT을 다른 단계의 임의 오류 코드로 허용하지 않는다', () => {
  const result = fixture([{...base, failedStage: 'FILE_DOWNLOAD', failureCode: 'TIMEOUT'}]).run();
  assert.deepEqual(result.regions[0].issues.map(i => [i.stage, i.code]),
    [['FILE_DOWNLOAD', 'UNCLASSIFIED_ERROR']]);
});
test('Content-Type mismatch remains a signature-stage error, not a verified download', () => {
  const result = fixture([{...base, status: 'COLLECTION_ONLY_PARTIAL_NOT_APPROVED', collectionStageComplete: false,
    files: [{locatorHash: hash, status: 'FAILED', bytes: 512, failedStage: 'FILE_SIGNATURE', failureCode: 'ATTACHMENT_CONTENT_TYPE_MISMATCH'}]}]).run();
  assert.equal(result.availabilitySummary.observedDownloadRegionCount, 0);
  assert.deepEqual(result.regions[0].issues.map(i => [i.stage, i.code]), [['FILE_SIGNATURE', 'ATTACHMENT_CONTENT_TYPE_MISMATCH']]);
});
test('raw text, filenames, URLs and arbitrary error strings are not copied', () => {
  const result = fixture([{...base, bodyStatus: 'FETCH_FAILED', bodyFailureCode: 'PRIVATE_CANARY',
    rawText: 'PRIVATE_CANARY', url: 'https://private.invalid/PRIVATE_CANARY', filename: 'PRIVATE_CANARY',
    failureCode: 'PRIVATE_CANARY', failedStage: 'PRIVATE_CANARY', discoveryWarningCodes: ['PRIVATE_CANARY']}]).run();
  assert.ok(!JSON.stringify(result).includes('PRIVATE_CANARY'));
  assert.ok(result.regions[0].issues.every(i => i.code === 'UNCLASSIFIED_ERROR'));
});
test('latest successful observation supersedes an earlier failure, not vice versa', () => {
  const old = {...base, observedAt: '2026-09-30T01:00:00Z', bodyStatus: 'FETCH_FAILED', bodyFailureCode: 'NETWORK_ERROR'};
  assert.equal(fixture([old, base]).run().diagnosticSummary.regionsWithBodyFetchFailure, 0);
  assert.equal(fixture([base, {...old, observedAt: '2026-10-01T02:00:00Z'}]).run().diagnosticSummary.regionsWithBodyFetchFailure, 1);
});
test('title policy stops and review decisions are not technical failures', () => {
  const stopped = {...base, titleStage: 'GROUP_B_MATCHED', status: 'TITLE_EXCLUDED_NOT_FETCHED',
    files: [], discoveryStatus: undefined, discoveryComplete: false, collectionStageComplete: false,
    detailIdentityVerified: false, requestReservationsIncludingBodyUpperBound: 0, bodyStatus: undefined};
  assert.equal(fixture([stopped]).run().diagnosticSummary.bodyUnobservedSampleCount, 0);
  assert.equal(fixture([{...base, bodyDecision: 'REVIEW_REQUIRED', bodyReason: 'BODY_GROUP_A_MATCHED'}]).run().regions[0].issues.length, 0);
});
test('unobserved body and missing profile are explicit, not invented failures', () => {
  const result = fixture([{...base, bodyStatus: undefined}], [target,
    {...target, localSourceCode: 'LGS-000002', profiles: [], bindingStatus: 'PROFILE_MISSING'}]).run();
  assert.equal(result.diagnosticSummary.bodyUnobservedSampleCount, 1);
  assert.equal(result.diagnosticSummary.regionsWithAnyRecordedIssue, 0);
  assert.equal(result.regions[1].bindingStatus, 'PROFILE_MISSING');
  assert.equal(result.regions[1].hasObservedDownload, false);
});
test('stale profiles cannot generate current success or current error claims', () => {
  const result = fixture([{...base, bodyStatus: 'FETCH_FAILED', bodyFailureCode: 'NETWORK_ERROR'}],
    [{...target, profiles: [{profileCode: 'PROFILE_1', profileHash: other}]}]).run();
  assert.equal(result.availabilitySummary.observedDownloadRegionCount, 0);
  assert.equal(result.diagnosticSummary.regionsWithAnyRecordedIssue, 0);
  assert.equal(result.regions[0].evidenceStatus, 'STALE_EVIDENCE_ONLY');
  assert.equal(result.regions[0].staleSampleCount, 1);
});
test('remote worker envelopes preserve body failures while retaining successful files', () => {
  const f = fixture();
  const r = {...base, scope: 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1', status: 'WORKER_DB_API_OBSERVED_NOT_APPROVED',
    workerStatus: 'EVALUATED', jobStatus: 'SUCCEEDED', discoveredFileCount: 1, processedFileCount: 1,
    bodyStatus: 'FETCH_FAILED', bodyFailureCode: 'NETWORK_ERROR',
    files: [{...base.files[0], downloadStatus: 'SUCCEEDED'}]};
  const envelope = {reports: [{kind: 'TEMPORARY_QA_RESULT', unitInactive: true, report: {productionDatabaseUsed: false,
    executionCodeHash: hash, probeCleanupSucceeded: true, transportTemporaryFilesRemoved: true,
    probe: {kind: 'OFFICIAL_WORKER_PROBE', productionDatabaseUsed: false, executionCodeHash: hash, cases: [r]}}}]};
  const bytes = Buffer.from(JSON.stringify(envelope)), path = `build/temporary-bbs-qa-${'a'.repeat(32)}/result.json`;
  const imported = importReceipt(bytes, JSON.parse(f.data.get(inventoryPath)));
  f.data.set(path, bytes);
  f.index.receipts = [{path, receiptHash: sha(bytes), status: imported.status, sampleCount: 1}];
  f.index.samples = imported.samples;
  const result = f.run();
  assert.equal(result.diagnosticSummary.successfulDownloadRegionsWithBodyFetchFailure, 1);
  assert.equal(result.currentHttpRequests, 0);
  assert.equal(result.isOperatingE2eVerified, false);
});
test('inventory, receipts and imported samples must still match their sealed evidence', () => {
  for (const mutate of [
    f => f.data.set(inventoryPath, Buffer.from('{}')),
    f => f.data.set(f.index.receipts[0].path, Buffer.from('{}')),
    f => f.index.samples[0].originalFilesRemoved = false,
    f => f.index.receipts[0].path = '../PRIVATE_CANARY',
    f => f.index.inventoryPath = '../PRIVATE_CANARY'
  ]) {
    const f = fixture(); mutate(f); assert.throws(f.run);
  }
});

test('과거 성공 후 최신 실패는 최초 미확인과 분리하고 지문 변경으로 숨기지 않는다', () => {
  const old={...base,observedAt:'2026-09-30T01:00:00Z'};
  const failed={...base,observedAt:'2026-10-01T02:00:00Z',profileHash:other,
    collectionStageComplete:false,status:'COLLECTION_ONLY_PARTIAL_NOT_APPROVED',
    files:[{locatorHash:hash,status:'FAILED',bytes:0,failedStage:'FILE_DOWNLOAD',failureCode:'TRANSPORT_TIMEOUT'}]};
  const result=fixture([old,failed],[{...target,profiles:[{profileCode:'PROFILE_1',profileHash:'c'.repeat(64)}]}]).run();
  assert.equal(result.availabilitySummary.observedDownloadRegionCount,0);
  assert.equal(result.diagnosticSummary.regionsWithAnyRecordedIssue,0);
  assert.equal(result.historySummary.everDownloadedRegionCount,1);
  assert.equal(result.historySummary.neverDownloadedRegionCount,0);
  assert.equal(result.historySummary.latestSampleUnrecoveredRegionCount,1);
  assert.equal(result.historySummary.recheckAfterPastDownloadRegionCount,1);
  const history=result.regions[0].history;
  assert.equal(history.lastSuccessfulObservedAt,old.observedAt);
  assert.equal(history.status,'RECHECK_AFTER_PAST_DOWNLOAD');
  assert.equal(history.latestKnownIssues[0].code,'TRANSPORT_TIMEOUT');
  assert.equal(history.latestKnownIssues[0].profileHash,other);
  assert.equal(history.latestKnownIssues[0].isCurrentProfile,false);
});

test('새 표본 성공이 있으면 다른 과거 표본 실패 때문에 수집원 전체를 미확보로 세지 않는다', () => {
  const failed={...base,caseCode:'CASE-2',collectionStageComplete:false,status:'COLLECTION_ONLY_PARTIAL_NOT_APPROVED',
    bodyStatus:'FETCH_FAILED',bodyFailureCode:'TIMEOUT',failedStage:'DETAIL_DISCOVERY',failureCode:'TRANSPORT_TIMEOUT',
    files:[],discoveryStatus:undefined,discoveryComplete:false,detailIdentityVerified:false};
  const result=fixture([failed,base]).run();
  assert.equal(result.historySummary.latestSampleDownloadedRegionCount,1);
  assert.equal(result.historySummary.latestSampleUnrecoveredRegionCount,0);
  assert.equal(result.historySummary.recheckAfterPastDownloadRegionCount,0);
  assert.equal(result.regions[0].history.latestKnownIssues.length,2);
  assert(result.regions[0].history.latestKnownIssues.every(i=>i.isCurrentProfile));
});

test('원본 미정리·식별 미확인 자료는 과거 다운로드 성공에도 포함하지 않는다', () => {
  for(const extra of [{originalFilesRemoved:false},{detailIdentityVerified:false,discoveryComplete:false,discoveryStatus:'FAILED'}]) {
    const result=fixture([{...base,...extra,status:'COLLECTION_ONLY_PARTIAL_NOT_APPROVED',collectionStageComplete:false}]).run();
    assert.equal(result.historySummary.everDownloadedRegionCount,0);
    assert.equal(result.historySummary.neverDownloadedRegionCount,1);
    assert.equal(result.regions[0].history.lastSuccessfulObservedAt,null);
  }
});

test('이력 수집원 수는 영수증 반복·비활성 수집원으로 증가하지 않는다', () => {
  const result=fixture([base,{...base,observedAt:'2026-10-01T02:00:00Z'}],
    [target,{...target,localSourceCode:'LGS-000002',enabled:false,profiles:[],bindingStatus:'PROFILE_MISSING'}]).run();
  assert.equal(result.historySummary.activeRegionCount,1);
  assert.equal(result.historySummary.everDownloadedRegionCount,1);
  assert.equal(result.regions[0].history.lastSuccessfulObservedAt,'2026-10-01T02:00:00Z');
});

test('제목 중단과 최신 미관측은 오류를 만들지 않고 과거 성공을 현재 성공으로 바꾸지 않는다', () => {
  const stopped={...base,observedAt:'2026-10-01T02:00:00Z',titleStage:'GROUP_B_MATCHED',status:'TITLE_EXCLUDED_NOT_FETCHED',
    files:[],discoveryStatus:undefined,discoveryComplete:false,collectionStageComplete:false,
    detailIdentityVerified:false,requestReservationsIncludingBodyUpperBound:0,bodyStatus:undefined};
  const result=fixture([base,stopped]).run();
  assert.equal(result.regions[0].history.status,'RECHECK_AFTER_PAST_DOWNLOAD');
  assert.deepEqual(result.regions[0].history.latestKnownIssues,[]);
  assert.equal(result.availabilitySummary.observedDownloadRegionCount,0);
  const absent=fixture([]).run();
  assert.equal(absent.regions[0].history.status,'NO_OBSERVATION');
  assert.deepEqual(absent.regions[0].history.latestKnownIssues,[]);
});

test('과거 지문의 오류 metadata도 원문·파일명·알 수 없는 오류 문자열을 배제한다', () => {
  const result=fixture([{...base,bodyFailureCode:'PRIVATE_CANARY',filename:'PRIVATE_CANARY'}],
    [{...target,profiles:[{profileCode:'PROFILE_1',profileHash:other}]}]).run();
  assert(!JSON.stringify(result).includes('PRIVATE_CANARY'));
  assert.equal(result.regions[0].history.latestKnownIssues[0].code,'UNCLASSIFIED_ERROR');
  assert.equal(result.regions[0].history.latestKnownIssues[0].isCurrentProfile,false);
  assert.equal(result.historySummary.latestSampleDownloadedRegionCount,1);
  assert.equal(result.availabilitySummary.observedDownloadRegionCount,0);
});

test('마지막 성공 시각은 입력 순서와 무관하게 나노초까지 보존한다', () => {
  const early={...base,observedAt:'2026-10-01T01:00:00.000000001Z'};
  const later={...base,observedAt:'2026-10-01T01:00:00.000000002Z'};
  for(const reports of [[early,later],[later,early]])
    assert.equal(fixture(reports).run().regions[0].history.lastSuccessfulObservedAt,later.observedAt);
});
