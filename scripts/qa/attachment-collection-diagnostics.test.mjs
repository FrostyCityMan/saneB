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
