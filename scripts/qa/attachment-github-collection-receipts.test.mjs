import test from 'node:test';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {importGithubCollectionReport} from './attachment-github-collection-receipts.mjs';
import {readCollectionDiagnostics} from './attachment-collection-diagnostics.mjs';

const hash = 'a'.repeat(64);
const inventory = {scope: 'OPERATING_TARGET_SNAPSHOT_VS_LOCAL_CODE_REGISTRY', targets: [{providerCode: 'LOCAL_GOV_NOTICE',
  localSourceCode: 'LGS-000001', listParserProfileCode: 'SPRING_BBS', enabled: true, bindingStatus: 'SYSTEM_BINDING_MATCHED',
  profiles: [{profileCode: 'PROFILE_1', profileHash: hash}]}]};
const base = {scope: 'OFFICIAL_THREE_STAGE_OBSERVATION_V1', collectionOnly: true, isExtractionVerified: false,
  isWholeTextAnalysisComplete: false, isPolicyQaPassed: false, isExpectationApproved: false, productionWriteCount: 0,
  observedAt: '2026-10-01T01:00:00Z', profileCode: 'PROFILE_1', profileHash: hash, caseCode: 'CASE-1',
  titleStage: 'COMBINATION_MATCHED', expectedListedFileCount: 1, detailIdentityVerified: true,
  discoveryStatus: 'FOUND', discoveryComplete: true, originalFilesRemoved: true, bodyStatus: 'AVAILABLE',
  status: 'COLLECTION_ONLY_OBSERVED_NOT_APPROVED', collectionStageComplete: true,
  files: [{locatorHash: hash, status: 'DOWNLOADED', downloadAllowed: true, format: 'PDF', binaryHash: hash, bytes: 512}]};
const entry = {kind: 'GITHUB_COLLECTION_ONLY', path: 'build/qa-github-runs/1/reports/attachment-regional-collection/CASE-1.json',
  producerClassHash: hash, github: {repository: 'FrostyCityMan/saneB', runId: 1, runAttempt: 1, jobId: 2, artifactId: 3, headSha: 'b'.repeat(40)}};
const bytes = object => Buffer.from(JSON.stringify(object));
const sha = data => createHash('sha256').update(data).digest('hex');

test('Linux runner 결과는 로컬 또는 운영 worker 결과로 표시하지 않는다', () => {
  const result = importGithubCollectionReport(bytes(base), inventory, entry);
  assert.equal(result.status, 'IMPORTED_GITHUB_COLLECTION_ONLY');
  assert.equal(result.samples[0].evidenceScope, 'GITHUB_COLLECTION_ONLY_REPORT');
  assert.deepEqual(result.samples[0].github, entry.github);
  assert.equal(result.samples[0].files[0].status, 'SUCCEEDED');
});
test('실행 ID·저장 경로·저장소·SHA와 최초 실행 제약을 검증한다', () => {
  for (const patch of [{runId: 0}, {runId: 4}, {runAttempt: 2}, {repository: 'other/repo'},
    {headSha: 'invalid'}, {artifactId: -1}, {jobId: null}]) {
    assert.throws(() => importGithubCollectionReport(bytes(base), inventory, {...entry, github: {...entry.github, ...patch}}));
  }
  assert.throws(() => importGithubCollectionReport(bytes(base), inventory, {...entry, path: '../outside.json'}));
});
test('운영 쓰기·추출·잘못된 파일 서명은 GitHub 결과에서도 거부한다', () => {
  for (const patch of [{productionWriteCount: 1}, {isExtractionVerified: true}, {files: [{...base.files[0], format: 'HTML'}]}]) {
    assert.throws(() => importGithubCollectionReport(bytes({...base, ...patch}), inventory, entry));
  }
});
test('Linux 결과도 봉인 대장의 단계별 오류 진단에 포함한다', () => {
  const report = bytes({...base, bodyStatus: 'FETCH_FAILED', bodyFailureCode: 'NETWORK_ERROR'});
  const imported = importGithubCollectionReport(report, inventory, entry);
  const inventoryPath = 'build/reports/attachment-target-inventory/operating-targets-vs-local-code.json';
  const inventoryBytes = bytes(inventory);
  const index = {inventoryPath, inventorySha256: sha(inventoryBytes), samples: imported.samples,
    receipts: [{...entry, receiptHash: sha(report), status: imported.status, sampleCount: 1}]};
  const result = readCollectionDiagnostics(index, path => path === inventoryPath ? inventoryBytes : report);
  assert.equal(result.availabilitySummary.observedDownloadRegionCount, 1);
  assert.equal(result.diagnosticSummary.regionsWithBodyFetchFailure, 1);
});
