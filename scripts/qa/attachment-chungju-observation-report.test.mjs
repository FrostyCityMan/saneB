import test from 'node:test';
import assert from 'node:assert/strict';
import { validateChungjuObservation } from './attachment-chungju-observation-report.mjs';
const startedAt = Date.now() - 1000;
const xml = '<testsuite name="com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest" tests="3" failures="0" errors="0" skipped="0"></testsuite>';
function reports() {
  return ['72625', '72039', '70852'].map((id, index) => {
    const r = { caseCode: 'CHUNGJU-' + id, scope: 'OFFICIAL_THREE_STAGE_OBSERVATION_V1', observedAt: new Date(startedAt + 1).toISOString(),
      profileCode: 'LOCAL_CHUNGJU_EMINWON_V1', productionWriteCount: 0, isPolicyQaPassed: false, isExpectationApproved: false,
      originalFilesRemoved: true, expectedListedFileCount: 1, maximumRequestReservations: 44, maximumReservedBytes: 83886080 };
    if (index < 2) return { ...r, status: 'TITLE_NOT_ELIGIBLE_NOT_FETCHED', titleStage: 'COMBINATION_NOT_MATCHED',
      titleReason: 'TITLE_COMBINATION_NOT_MATCHED', files: [], requestReservationsIncludingBodyUpperBound: 0, reservedBytesIncludingBodyUpperBound: 0,
      isWholeTextAnalysisComplete: false, requiresFinalAdminVerification: false };
    return { ...r, status: 'OBSERVED_NOT_VALIDATED', titleStage: 'COMBINATION_MATCHED', bodyStageComplete: true, bodyStatus: 'AVAILABLE',
      discoveryStatus: 'FOUND', discoveryComplete: true, discoveredFileCount: 1, requiresFinalAdminVerification: true,
      requestReservationsIncludingBodyUpperBound: 4, reservedBytesIncludingBodyUpperBound: 3000000, decisionStatus: 'REVIEW_REQUIRED',
      isWholeTextAnalysisComplete: false, files: [{ status: 'OBSERVED', format: 'HWP', bytes: 170496, quality: 'PARTIAL_TEXT' }] };
  });
}
test('음성 2건과 HWP 1개의 부분 추출 관측은 정책 승인과 구분한다', () => {
  const result = validateChungjuObservation(xml, reports(), startedAt);
  assert.equal(result.titleStoppedCount, 2); assert.equal(result.observedNoticeCount, 1);
  assert.equal(result.cases[2].isWholeTextAnalysisComplete, false); assert.equal(result.isPolicyQaPassed, false);
});
test('완전 추출은 실제 텍스트·블록·역할 근거를 요구한다', () => {
  const r = reports(); r[2].files[0] = { ...r[2].files[0], quality: 'COMPLETE_TEXT', characterCount: 1000, blockCount: 30, roleAssessment: { roleCode: 'FORM' } };
  r[2].isWholeTextAnalysisComplete = true;
  assert.equal(validateChungjuObservation(xml, r, startedAt).cases[2].isWholeTextAnalysisComplete, true);
});
for (const [name, change] of [
  ['음성 누락', r => r.shift()], ['다른 기관', r => r[2].profileCode = 'LOCAL_JECHEON_BBS_V1'],
  ['음성 뒤 파일 요청', r => r[1].requestReservationsIncludingBodyUpperBound = 1], ['음성 뒤 본문', r => r[0].bodyStatus = 'AVAILABLE'],
  ['양성 제목 중단', r => r[2] = { ...r[0], caseCode: 'CHUNGJU-70852' }], ['파일 형식 변경', r => r[2].files[0].format = 'HWPX'],
  ['부분 추출 완전성 위조', r => r[2].isWholeTextAnalysisComplete = true], ['다운로드 실패', r => r[2].files[0].status = 'FAILED'],
  ['원본 잔류', r => r[2].originalFilesRemoved = false], ['정책 승인', r => r[2].isPolicyQaPassed = true],
  ['운영 쓰기', r => r[2].productionWriteCount = 1], ['과거 관측', r => r[2].observedAt = new Date(startedAt - 1).toISOString()],
  ['파일 누락', r => r[2].files = []], ['관리자 검증 누락', r => r[2].requiresFinalAdminVerification = false],
]) test('거부: ' + name, () => { const r = reports(); change(r); assert.throws(() => validateChungjuObservation(xml, r, startedAt)); });
test('JUnit 미실행·실패·생략과 시각 누락을 거부한다', () => {
  for (const invalid of [xml.replace('tests="3"', 'tests="2"'), xml.replace('failures="0"', 'failures="1"'), xml.replace('skipped="0"', 'skipped="1"')])
    assert.throws(() => validateChungjuObservation(invalid, reports(), startedAt));
  assert.throws(() => validateChungjuObservation(xml, reports(), NaN));
});
