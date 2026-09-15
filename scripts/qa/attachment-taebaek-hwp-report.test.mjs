import test from 'node:test';
import assert from 'node:assert/strict';
import { validateHwpWorkerReport } from './attachment-taebaek-hwp-report.mjs';

const xml = '<testsuite name="com.saneb.db.AnnouncementAttachmentOfficialWorkerIntegrationTest" tests="1" failures="0" errors="0" skipped="0"></testsuite>';
const report = () => ({ caseCode: 'TAEBAEK-176153', scope: 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1', status: 'WORKER_DB_API_OBSERVED_NOT_APPROVED',
  productionWriteCount: 0, isPolicyQaPassed: false, isExpectationApproved: false, isAuthenticatedBrowserE2e: false,
  originalFilesRemoved: true, remainingResourceLeases: 0, requiresFinalAdminVerification: true, bodyStageComplete: true, bodyStatus: 'AVAILABLE',
  discoveryComplete: true, discoveredFileCount: 1, processedFileCount: 1, extractorCalls: 1, maximumRequestReservations: 44,
  maximumReservedBytes: 83886080, requestReservationsIncludingBodyUpperBound: 4, reservedBytesIncludingBodyUpperBound: 2354176,
  isWholeTextAnalysisComplete: false, files: [{ format: 'HWP', quality: 'PARTIAL_TEXT', characterCount: 9743, blockCount: 480,
    hwpStructure: { sectionCount: 1, recordCount: 1, maximumLevel: 1, recordTypes: [{tagId: 67, count: 1}] } }] });

test('부분 추출의 연결 성공을 완전 추출·정책 승인으로 표시하지 않는다', () => {
  const result = validateHwpWorkerReport(xml, report());
  assert.equal(result.quality, 'PARTIAL_TEXT'); assert.equal(result.isWholeTextAnalysisComplete, false); assert.equal(result.isPolicyQaPassed, false);
  assert.equal(result.requiresFinalAdminVerification, true);
});
test('완전 추출도 관리자 검증 요구를 유지한다', () => {
  const input = report(); input.files[0].quality = 'COMPLETE_TEXT'; input.isWholeTextAnalysisComplete = true;
  assert.equal(validateHwpWorkerReport(xml, input).requiresFinalAdminVerification, true);
});
for (const [key, value] of Object.entries({ caseCode: 'OTHER', productionWriteCount: 1, isPolicyQaPassed: true, isExpectationApproved: true,
  isAuthenticatedBrowserE2e: true, originalFilesRemoved: false, remainingResourceLeases: 1, bodyStageComplete: false,
  discoveredFileCount: 2, processedFileCount: 0, extractorCalls: 0, maximumRequestReservations: 45,
  maximumReservedBytes: 83886081, requestReservationsIncludingBodyUpperBound: 45, reservedBytesIncludingBodyUpperBound: 83886081,
  isWholeTextAnalysisComplete: true, requiresFinalAdminVerification: false })) {
  test(`범위·증거 불일치 거부: ${key}`, () => assert.throws(() => validateHwpWorkerReport(xml, { ...report(), [key]: value })));
}
test('누락·생략·다른 분모의 JUnit을 성공으로 계산하지 않는다', () => {
  for (const changed of ['', xml.replace('tests="1"', 'tests="0"'), xml.replace('tests="1"', 'tests="3"'),
    xml.replace('skipped="0"', 'skipped="1"'), xml.replace('failures="0"', 'failures="1"')]) assert.throws(() => validateHwpWorkerReport(changed, report()));
});
test('추출 구조·텍스트 없는 파일을 실제 HWP 처리로 표시하지 않는다', () => {
  for (const change of [{format:'PDF'}, {quality:'ENCRYPTED'}, {characterCount:0}, {blockCount:0}, {hwpStructure:null}, {hwpStructure:{}}]) {
    const input=report();Object.assign(input.files[0],change);assert.throws(() => validateHwpWorkerReport(xml,input));
  }
});
