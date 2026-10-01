/** 봉인된 영수증을 다시 검증하여 단계별 기술 오류만 반환한다. 원문·URL·파일명은 출력하지 않는다. */
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {importReceipt, importLocalCollectionReport, selectLatestSamples} from './attachment-collection-receipts.mjs';
import {buildCollectionAvailability} from './attachment-collection-availability.mjs';
import {importGithubCollectionReport, validateGithubCollectionEntry} from './attachment-github-collection-receipts.mjs';

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
// Java 관측의 나노초를 밀리초 Date로 잘라 마지막 성공 시각을 뒤바꾸지 않는다.
const observedOrder = value => value.slice(0,19) + (value.split('.')[1]?.slice(0,-1) ?? '').padEnd(9,'0');
const codes = new Set(['BODY_SELECTOR_CHANGED', 'NETWORK_ERROR', 'HTTP_STATUS_ERROR', 'DETAIL_HOST_NOT_ALLOWED',
  'BODY_TEXT_EMPTY', 'TRANSPORT_TIMEOUT', 'ATTACHMENT_SIGNATURE_UNSUPPORTED', 'ATTACHMENT_CONTENT_TYPE_MISMATCH', 'ATTACHMENT_PATH_NOT_APPROVED',
  'ATTACHMENT_FORMAT_MISMATCH', 'ATTACHMENT_DOWNLOAD_BLOCKED', 'TLS_FAILED', 'OBSERVATION_FAILED', 'TRANSPORT_FAILED',
  'OBSERVATION_ASSERTION_FAILED', 'ATTACHMENT_LINK_UNRESOLVED', 'ATTACHMENT_DETAIL_TITLE_UNAVAILABLE',
  'ATTACHMENT_SELECTOR_CHANGED', 'UNSUPPORTED_FORMAT']);
// ProviderContentCodes의 본문 TIMEOUT은 파일 전송 TRANSPORT_TIMEOUT과 별개다.
const safeCode = (value, stage) => (stage === 'BODY_FETCH' && value === 'TIMEOUT')
  || codes.has(value) || /^ATTACHMENT_HTTP_[1-5][0-9]{2}$/.test(value ?? '')
  ? value : 'UNCLASSIFIED_ERROR';
const stages = new Set(['TITLE_CONFIRMATION', 'DETAIL_DISCOVERY', 'FILE_DOWNLOAD', 'FILE_SIGNATURE',
  'ISOLATED_EXTRACTION', 'TEXT_ROLE']);

export function readCollectionDiagnostics(index, readBytes) {
  assert.equal(index.inventoryPath, 'build/reports/attachment-target-inventory/operating-targets-vs-local-code.json');
  assert.ok(Array.isArray(index.receipts) && index.receipts.length <= 10000);
  const inventoryBytes = readBytes(index.inventoryPath);
  assert.equal(sha(inventoryBytes), index.inventorySha256, 'INVENTORY_HASH_CHANGED');
  const inventory = JSON.parse(inventoryBytes);
  const rawByReceipt = new Map();
  const receipts = index.receipts.map(entry => {
    const local = entry.kind === 'LOCAL_COLLECTION_ONLY';
    const github = entry.kind === 'GITHUB_COLLECTION_ONLY';
    if (github) validateGithubCollectionEntry(entry);
    else assert.match(entry.path, local ? /^build\/reports\/attachment-regional-collection\/[A-Z0-9_-]+\.json$/
      : /^build\/temporary-bbs-qa-[a-f0-9]{32}\/result(?:-utf8)?\.json$/);
    const bytes = readBytes(entry.path);
    assert.equal(sha(bytes), entry.receiptHash, 'RECEIPT_HASH_CHANGED');
    const imported = github ? importGithubCollectionReport(bytes, inventory, entry)
      : local ? importLocalCollectionReport(bytes, inventory, entry.producerClassHash)
      : importReceipt(bytes, inventory);
    assert.equal(imported.status, entry.status);
    assert.equal(imported.samples.length, entry.sampleCount);
    const raw = JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/, ''));
    const probe = local || github ? null : raw.reports?.find(r => r.kind === 'TEMPORARY_QA_RESULT')?.report?.probe;
    rawByReceipt.set(entry.receiptHash, local || github ? [raw] : probe?.cases ?? probe?.reports ?? []);
    return imported;
  });
  const samples = selectLatestSamples(receipts);
  assert.deepEqual(samples, index.samples, 'IMPORTED_EVIDENCE_CHANGED');
  const availability = buildCollectionAvailability(inventory, samples);
  const allSamples = receipts.flatMap(receipt => receipt.samples);
  const hasDownload = sample => sample.titleStatus === 'ELIGIBLE' && sample.detailIdentityVerified
    && sample.originalFilesRemoved && sample.productionWriteCount === 0
    && sample.files.some(file => file.status === 'SUCCEEDED' && file.signatureVerified === true);
  const regions = availability.regions.filter(r => r.enabled).map(region => {
    const target = inventory.targets.find(t => t.providerCode === 'LOCAL_GOV_NOTICE' && t.localSourceCode === region.sourceCode);
    const own = samples.filter(s => s.sourceCode === region.sourceCode);
    const current = own.filter(s => target.bindingStatus === 'SYSTEM_BINDING_MATCHED'
      && target.profiles.length === 1 && s.profileCode === target.profiles[0].profileCode && s.profileHash === target.profiles[0].profileHash);
    const selectIssues = sample => {
      const issues = [];
      if (sample.titleStatus === 'NOT_ELIGIBLE') return issues;
      const reports = rawByReceipt.get(sample.receiptHash).filter(r => r.caseCode === sample.caseCode);
      assert.equal(reports.length, 1, 'EXACT_REPORT_REQUIRED');
      const report = reports[0];
      const add = (stage, code, locatorHash) => issues.push({caseCode: sample.caseCode, receiptHash: sample.receiptHash, observedAt: sample.observedAt,
        stage, code: safeCode(code, stage), ...(locatorHash ? {locatorHash} : {})});
      if (report.bodyStatus === 'FETCH_FAILED' || report.bodyFailureCode) add('BODY_FETCH', report.bodyFailureCode);
      if (report.failureCode || report.failedStage) add(stages.has(report.failedStage) ? report.failedStage : 'OBSERVATION', report.failureCode);
      for (const warning of report.discoveryWarningCodes ?? []) add('ATTACHMENT_DISCOVERY', warning);
      for (const file of sample.files) {
        if (file.status === 'SUCCEEDED') continue;
        const originals = report.files.filter(f => f.locatorHash === file.locatorHash);
        assert.equal(originals.length, 1, 'EXACT_FILE_REQUIRED');
        const original = originals[0];
        add(file.status === 'UNSUPPORTED' ? 'UNSUPPORTED_FILE' : file.status === 'NOT_RUN' ? 'FILE_NOT_RUN'
          : stages.has(original.failedStage) ? original.failedStage : 'FILE_COLLECTION',
        file.status === 'UNSUPPORTED' ? 'UNSUPPORTED_FORMAT' : original.failureCode ?? original.downloadErrorCode, file.locatorHash);
      }
      return issues;
    };
    const issues = current.flatMap(selectIssues);
    const bodyUnobservedSampleCount = current.filter(sample => {
      if (sample.titleStatus === 'NOT_ELIGIBLE') return false;
      const report = rawByReceipt.get(sample.receiptHash).find(r => r.caseCode === sample.caseCode);
      return report.bodyStatus !== 'AVAILABLE' && report.bodyStatus !== 'FETCH_FAILED' && !report.bodyFailureCode;
    }).length;
    const historicalSuccesses = allSamples.filter(sample => sample.sourceCode === region.sourceCode && hasDownload(sample));
    const latestSampleHasDownload = own.some(hasDownload);
    // 과거 지문을 현재 성공으로 승격하지 않는다. 원래 시점·지문을 가진 최신 오류만 별도 참고한다.
    const history = {hasHistoricalDownload: historicalSuccesses.length > 0, latestSampleHasDownload,
      status: latestSampleHasDownload ? 'LATEST_SAMPLE_DOWNLOAD_OBSERVED'
        : historicalSuccesses.length ? 'RECHECK_AFTER_PAST_DOWNLOAD' : own.length ? 'NEVER_DOWNLOADED' : 'NO_OBSERVATION',
      lastSuccessfulObservedAt: historicalSuccesses.sort((a,b) => observedOrder(b.observedAt).localeCompare(observedOrder(a.observedAt)))[0]?.observedAt ?? null,
      latestKnownIssues: own.flatMap(sample => selectIssues(sample).map(issue => ({...issue,
        profileHash: sample.profileHash, isCurrentProfile: current.includes(sample)})))};
    return {sourceCode: region.sourceCode, hasObservedDownload: region.hasObservedDownload,
      downloadedFileObservations: region.downloadedFileObservations, bindingStatus: region.bindingStatus,
      evidenceStatus: target.bindingStatus !== 'SYSTEM_BINDING_MATCHED' ? target.bindingStatus
        : current.length === 0 ? own.length ? 'STALE_EVIDENCE_ONLY' : 'NO_OBSERVATION' : 'CURRENT_PROFILE_OBSERVED',
      currentSampleCount: current.length, staleSampleCount: own.length - current.length,
      titleStoppedSampleCount: current.filter(s => s.titleStatus === 'NOT_ELIGIBLE').length,
      bodyUnobservedSampleCount, issues, history};
  });
  return {schemaVersion: 1, scope: 'VERIFIED_ARCHIVED_STAGE_DIAGNOSTICS', currentHttpRequests: 0, productionWriteCount: 0,
    inventoryObservedAt: availability.inventoryObservedAt, isOperatingE2eVerified: false,
    availabilitySummary: availability.summary,
    historySummary: {scope: 'ARCHIVED_HISTORY_NOT_CURRENT_CODE_VERIFICATION', activeRegionCount: regions.length,
      everDownloadedRegionCount: regions.filter(r => r.history.hasHistoricalDownload).length,
      neverDownloadedRegionCount: regions.filter(r => !r.history.hasHistoricalDownload).length,
      latestSampleDownloadedRegionCount: regions.filter(r => r.history.latestSampleHasDownload).length,
      latestSampleUnrecoveredRegionCount: regions.filter(r => !r.history.latestSampleHasDownload).length,
      recheckAfterPastDownloadRegionCount: regions.filter(r => r.history.status === 'RECHECK_AFTER_PAST_DOWNLOAD').length},
    diagnosticSummary: {
      regionsWithAnyRecordedIssue: regions.filter(r => r.issues.length).length,
      regionsWithStaleEvidenceOnly: regions.filter(r => r.evidenceStatus === 'STALE_EVIDENCE_ONLY').length,
      regionsWithNoObservation: regions.filter(r => r.evidenceStatus === 'NO_OBSERVATION').length,
      regionsWithBodyFetchFailure: regions.filter(r => r.issues.some(e => e.stage === 'BODY_FETCH')).length,
      successfulDownloadRegionsWithBodyFetchFailure: regions.filter(r => r.hasObservedDownload && r.issues.some(e => e.stage === 'BODY_FETCH')).length,
      bodyUnobservedSampleCount: regions.reduce((n, r) => n + r.bodyUnobservedSampleCount, 0)
    }, regions};
}
