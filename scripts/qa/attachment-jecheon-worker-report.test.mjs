import test from 'node:test';
import assert from 'node:assert/strict';
import { validateJecheonWorkerReport } from './attachment-jecheon-worker-report.mjs';

const started = Date.now() - 2000;
const xml = '<testsuite name="com.saneb.db.AnnouncementAttachmentOfficialWorkerIntegrationTest" tests="3" failures="0" errors="0" skipped="0"><testcase name="a"/><testcase name="b"/><testcase name="c"/></testsuite>';
const digest = 'a'.repeat(64);
function reports() {
  return [0, 1, 2].map((count, i) => {
    const r = { scope: 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1', caseCode: ['JECHEON-403587', 'JECHEON-403530', 'JECHEON-403490'][i],
      observedAt: new Date(started + 1000).toISOString(), productionWriteCount: 0, isPolicyQaPassed: false,
      isExpectationApproved: false, isAuthenticatedBrowserE2e: false, originalFilesRemoved: true, remainingResourceLeases: 0,
      titleInputSource: 'FIXED_OFFICIAL_SAMPLE', policySource: 'EPHEMERAL_DB_FIXTURE', maximumRequestReservations: 44,
      maximumReservedBytes: 83886080, requestReservationsIncludingBodyUpperBound: count ? count + 2 : 0,
      reservedBytesIncludingBodyUpperBound: count ? 10000 : 0, isWholeTextAnalysisComplete: count > 0, files: [],
      status: count ? 'WORKER_DB_API_OBSERVED_NOT_APPROVED' : 'TITLE_EXCLUDED_NOT_FETCHED',
      titleStage: count ? 'COMBINATION_MATCHED' : 'COMBINATION_NOT_MATCHED', titleReason: 'TITLE_COMBINATION_NOT_MATCHED' };
    if (count) Object.assign(r, { bodyStatus: 'AVAILABLE', bodyStageComplete: true, profileCode: 'LOCAL_JECHEON_BBS_V1',
      profileHash: digest, extractorConfigHash: digest, extractorVersion: '1.0.3', roleRuleVersion: 'document-role-1.0.2',
      workerStatus: 'EVALUATED', jobStatus: 'SUCCEEDED', requiresFinalAdminVerification: true, discoveryComplete: true,
      discoveredFileCount: count, processedFileCount: count, extractorCalls: count, decisionStatus: 'REVIEW_REQUIRED',
      processingStatus: 'FINAL_REVIEW_EXCEPTION', files: Array.from({ length: count }, (_, file) => ({
        format: 'HWPX', downloadStatus: 'SUCCEEDED', downloadErrorCode: null, bytes: 4000, binaryHash: digest,
        locatorHash: String(file + 1).repeat(64), quality: 'COMPLETE_TEXT', characterCount: 100, blockCount: 10,
        textHash: digest, roleCode: 'UNKNOWN', roleOrigin: 'TEXT_RULE', roleFingerprint: { roleCode: 'UNKNOWN',
          ruleVersion: 'document-role-1.0.2', textHash: digest, blocksHash: digest, rulesHash: digest, assessmentHash: digest } })) });
    return r;
  });
}
test('제목 음성 1건과 두 공고 전체 3파일의 worker 결과를 승인과 구분한다', () => {
  const result = validateJecheonWorkerReport(xml, reports(), started);
  assert.equal(result.processedFileCount, 3); assert.equal(result.titleStoppedCount, 1);
  assert.equal(result.isPolicyQaPassed, false); assert.equal(result.isAuthenticatedBrowserE2e, false);
  assert.deepEqual(result.cases[2].roles, ['UNKNOWN', 'UNKNOWN']);
});
for (const [name, mutate] of [
  ['누락 공고', r => r.pop()], ['중복 공고', r => { r[2] = r[1]; }],
  ['오래된 관측', r => { r[1].observedAt = new Date(started - 1).toISOString(); }],
  ['미래 관측', r => { r[1].observedAt = new Date(Date.now() + 60000).toISOString(); }],
  ['원본 미정리', r => { r[1].originalFilesRemoved = false; }],
  ['잔여 lease', r => { r[1].remainingResourceLeases = 1; }],
  ['운영 쓰기', r => { r[1].productionWriteCount = 1; }],
  ['정책 승인 주장', r => { r[1].isPolicyQaPassed = true; }],
  ['브라우저 성공 주장', r => { r[1].isAuthenticatedBrowserE2e = true; }],
  ['음성의 후속 요청', r => { r[0].requestReservationsIncludingBodyUpperBound = 1; }],
  ['음성의 본문 확보', r => { r[0].bodyStatus = 'AVAILABLE'; }],
  ['음성 사유 변화', r => { r[0].titleReason = 'OTHER'; }],
  ['첫 파일만 처리', r => { r[2].files.pop(); }],
  ['중복 파일', r => { r[2].files[1].locatorHash = r[2].files[0].locatorHash; }],
  ['추출 호출 부족', r => { r[2].extractorCalls = 1; }],
  ['작업 미완료', r => { r[1].jobStatus = 'RUNNING'; }],
  ['기관 변화', r => { r[1].profileCode = 'LOCAL_OTHER'; }],
  ['요청 예산 초과', r => { r[1].requestReservationsIncludingBodyUpperBound = 45; }],
  ['파일 상한 초과', r => { r[1].files[0].bytes = 20971521; }],
  ['텍스트 지문 불일치', r => { r[1].files[0].textHash = 'b'.repeat(64); }],
  ['역할 근거 누락', r => { delete r[1].files[0].roleFingerprint; }],
  ['UNKNOWN 정상 후보 승격', r => { r[1].processingStatus = 'READY_FOR_FINAL_REVIEW'; }],
  ['부분 추출 완전성 위장', r => { r[1].files[0].quality = 'PARTIAL_TEXT'; }],
]) test(name + ': 잘못된 결과를 거부한다', () => {
  const input = reports(); mutate(input);
  assert.throws(() => validateJecheonWorkerReport(xml, input, started));
});
test('부분 추출은 실패를 숨기지 않는 기술 예외 검수 결과로만 인정한다', () => {
  const input = reports(); input[1].files[0].quality = 'PARTIAL_TEXT';
  input[1].isWholeTextAnalysisComplete = false; input[1].processingStatus = 'TECHNICAL_EXCEPTION';
  assert.equal(validateJecheonWorkerReport(xml, input, started).cases[1].isWholeTextAnalysisComplete, false);
  input[1].decisionStatus = 'ACCEPTED'; assert.throws(() => validateJecheonWorkerReport(xml, input, started));
});
test('JUnit 실패·생략·분모 축소를 허용하지 않는다', () => {
  for (const changed of [xml.replace('failures="0"', 'failures="1"'), xml.replace('skipped="0"', 'skipped="1"'), xml.replace('tests="3"', 'tests="2"')])
    assert.throws(() => validateJecheonWorkerReport(changed, reports(), started));
});
