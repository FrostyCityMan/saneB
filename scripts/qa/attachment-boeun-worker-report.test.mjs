import test from 'node:test';
import assert from 'node:assert/strict';
import { validateBoeunWorkerReport } from './attachment-boeun-worker-report.mjs';

const started = Date.now() - 2000;
const xml = '<testsuite name="com.saneb.db.AnnouncementAttachmentOfficialWorkerIntegrationTest" tests="3" failures="0" errors="0" skipped="0"></testsuite>';
const digest = 'a'.repeat(64);
function reports() {
  return ['BOEUN-221499', 'BOEUN-221497', 'BOEUN-218812'].map((caseCode, index) => ({
    scope: 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1', caseCode, observedAt: new Date(started + 1000).toISOString(),
    productionWriteCount: 0, isPolicyQaPassed: false, isExpectationApproved: false, isAuthenticatedBrowserE2e: false,
    originalFilesRemoved: true, remainingResourceLeases: 0, titleInputSource: 'FIXED_OFFICIAL_SAMPLE', policySource: 'EPHEMERAL_DB_FIXTURE',
    maximumRequestReservations: 44, maximumReservedBytes: 83886080, requestReservationsIncludingBodyUpperBound: 4,
    reservedBytesIncludingBodyUpperBound: 2200000, isWholeTextAnalysisComplete: true, status: 'WORKER_DB_API_OBSERVED_NOT_APPROVED',
    titleStage: 'COMBINATION_MATCHED', bodyStatus: 'AVAILABLE', bodyStageComplete: true, profileCode: 'LOCAL_BOEUN_BBS_V1',
    profileHash: digest, extractorConfigHash: digest, extractorVersion: '1.0.3', roleRuleVersion: 'document-role-1.0.2',
    workerStatus: 'EVALUATED', jobStatus: 'SUCCEEDED', requiresFinalAdminVerification: true, discoveryComplete: true,
    discoveredFileCount: 1, processedFileCount: 1, extractorCalls: 1, decisionStatus: 'REVIEW_REQUIRED', processingStatus: 'FINAL_REVIEW_EXCEPTION',
    files: [{ format: index === 2 ? 'PDF' : 'HWPX', downloadStatus: 'SUCCEEDED', downloadErrorCode: null, bytes: 4000,
      binaryHash: digest, locatorHash: String(index + 1).repeat(64), quality: 'COMPLETE_TEXT', characterCount: 100,
      blockCount: 10, textHash: digest, roleCode: 'UNKNOWN', roleOrigin: 'TEXT_RULE', roleFingerprint: {
        roleCode: 'UNKNOWN', ruleVersion: 'document-role-1.0.2', textHash: digest, blocksHash: digest, rulesHash: digest, assessmentHash: digest } }],
  }));
}
test('보은 전체 3공고의 HWPX2·PDF1 처리와 정책·브라우저 승인을 구분한다', () => {
  const result = validateBoeunWorkerReport(xml, reports(), started);
  assert.equal(result.tests, 3); assert.equal(result.titleStoppedCount, 0); assert.equal(result.observedNoticeCount, 3);
  assert.equal(result.processedFileCount, 3); assert.equal(result.isPolicyQaPassed, false);
  assert.equal(result.isExpectationApproved, false); assert.equal(result.isAuthenticatedBrowserE2e, false);
});
for (const [name, mutate] of [
  ['누락 공고', r => r.pop()], ['중복 공고', r => { r[2] = r[1]; }],
  ['다른 공고', r => { r[2].caseCode = 'BOEUN-UNKNOWN'; }], ['다른 기관', r => { r[1].profileCode = 'LOCAL_JECHEON_BBS_V1'; }],
  ['PDF 형식 변경', r => { r[2].files[0].format = 'HWPX'; }], ['HWPX 형식 변경', r => { r[0].files[0].format = 'PDF'; }],
  ['전체 파일 누락', r => { r[1].files = []; }], ['추출 호출 없음', r => { r[1].extractorCalls = 0; }],
  ['제목 제외로 대체', r => { r[1].titleStage = 'COMBINATION_NOT_MATCHED'; }], ['본문 실패', r => { r[1].bodyStatus = 'FAILED'; }],
  ['발견 불완전', r => { r[1].discoveryComplete = false; }], ['작업 미완료', r => { r[1].jobStatus = 'RUNNING'; }],
  ['이전 시각', r => { r[1].observedAt = new Date(started - 1).toISOString(); }],
  ['원본 잔류', r => { r[1].originalFilesRemoved = false; }], ['lease 잔류', r => { r[1].remainingResourceLeases = 1; }],
  ['운영 변경', r => { r[1].productionWriteCount = 1; }], ['정책 승인', r => { r[1].isPolicyQaPassed = true; }],
  ['브라우저 승인', r => { r[1].isAuthenticatedBrowserE2e = true; }], ['최종 검수 생략', r => { r[1].requiresFinalAdminVerification = false; }],
  ['UNKNOWN 자동 후보', r => { r[1].decisionStatus = 'ACCEPTED'; }], ['텍스트 지문 누락', r => { delete r[1].files[0].textHash; }],
  ['역할 근거 누락', r => { delete r[1].files[0].roleFingerprint; }], ['용량 초과', r => { r[1].reservedBytesIncludingBodyUpperBound = 83886081; }],
]) test(name + ': 잘못된 결과를 거부한다', () => { const input = reports(); mutate(input); assert.throws(() => validateBoeunWorkerReport(xml, input, started)); });
test('부분 PDF는 기술 예외·검수로 보존하며 정상 추출로 승격하지 않는다', () => {
  const input = reports(); input[2].files[0].quality = 'PARTIAL_TEXT'; input[2].isWholeTextAnalysisComplete = false;
  input[2].processingStatus = 'TECHNICAL_EXCEPTION';
  assert.equal(validateBoeunWorkerReport(xml, input, started).cases[2].isWholeTextAnalysisComplete, false);
  input[2].decisionStatus = 'ACCEPTED'; assert.throws(() => validateBoeunWorkerReport(xml, input, started));
});
test('근거가 완전한 NOTICE 후보도 최종 관리자 검증과 정책 승인을 분리한다', () => {
  const input = reports();
  for (const report of input) {
    report.files[0].roleCode = 'NOTICE'; report.files[0].roleFingerprint.roleCode = 'NOTICE';
    report.processingStatus = 'READY_FOR_FINAL_REVIEW'; report.decisionStatus = 'ACCEPTED';
  }
  const result = validateBoeunWorkerReport(xml, input, started);
  assert.equal(result.cases.every(r => r.processingStatus === 'READY_FOR_FINAL_REVIEW' && r.requiresFinalAdminVerification), true);
  assert.equal(result.isPolicyQaPassed, false); assert.equal(result.isExpectationApproved, false);
});
test('JUnit 실패·오류·생략·분모 축소 또는 시작 시각 누락을 거부한다', () => {
  for (const invalid of [xml.replace('failures="0"', 'failures="1"'), xml.replace('errors="0"', 'errors="1"'),
    xml.replace('skipped="0"', 'skipped="1"'), xml.replace('tests="3"', 'tests="2"')])
    assert.throws(() => validateBoeunWorkerReport(invalid, reports(), started));
  assert.throws(() => validateBoeunWorkerReport(xml, reports(), NaN));
});
