import test from 'node:test';
import assert from 'node:assert/strict';
import { validateOkcheonObservation } from './attachment-okcheon-observation-report.mjs';

const startedAt = Date.now() - 1000;
const xml = '<testsuite name="com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest" tests="3" failures="0" errors="0" skipped="0"></testsuite>';
function reports() {
  return ['193369', '193297', '193187'].map((id, index) => {
    const base = { caseCode: 'OKCHEON-' + id, scope: 'OFFICIAL_THREE_STAGE_OBSERVATION_V1', observedAt: new Date(startedAt + 1).toISOString(),
      profileCode: 'LOCAL_OKCHEON_BBS_V1', productionWriteCount: 0, isPolicyQaPassed: false, isExpectationApproved: false,
      originalFilesRemoved: true, expectedListedFileCount: 1, maximumRequestReservations: 44, maximumReservedBytes: 83886080 };
    if (index === 2) return { ...base, status: 'TITLE_NOT_ELIGIBLE_NOT_FETCHED', titleStage: 'COMBINATION_NOT_MATCHED',
      titleReason: 'TITLE_COMBINATION_NOT_MATCHED', files: [], requestReservationsIncludingBodyUpperBound: 0, reservedBytesIncludingBodyUpperBound: 0,
      isWholeTextAnalysisComplete: false, requiresFinalAdminVerification: false };
    return { ...base, status: 'OBSERVED_NOT_VALIDATED', titleStage: 'COMBINATION_MATCHED', bodyStageComplete: true, bodyStatus: 'AVAILABLE',
      discoveryStatus: 'FOUND', discoveryComplete: true, discoveredFileCount: 1, requiresFinalAdminVerification: true,
      requestReservationsIncludingBodyUpperBound: 4, reservedBytesIncludingBodyUpperBound: 3000000, decisionStatus: 'REVIEW_REQUIRED',
      isWholeTextAnalysisComplete: true, files: [{ status: 'OBSERVED', format: 'HWPX', bytes: 1024, quality: 'COMPLETE_TEXT',
        characterCount: 1000, blockCount: 30, roleAssessment: { roleCode: 'UNKNOWN' } }] };
  });
}

test('전체3건 중 제목 음성1건·완전 추출2건을 정책 승인과 구분한다', () => {
  const result = validateOkcheonObservation(xml, reports(), startedAt);
  assert.equal(result.referenceCount, 3); assert.equal(result.titleStoppedCount, 1); assert.equal(result.observedNoticeCount, 2);
  assert.equal(result.cases[0].isWholeTextAnalysisComplete, true); assert.deepEqual(result.cases[0].roles, ['UNKNOWN']);
  assert.equal(result.isPolicyQaPassed, false); assert.equal(result.isExpectationApproved, false); assert.equal(result.productionWriteCount, 0);
});
test('부분 추출도 실제 관측 사실로 보존하되 완전 추출로 표시하지 않는다', () => {
  const r = reports(); r[0].files[0].quality = 'PARTIAL_TEXT'; r[0].isWholeTextAnalysisComplete = false;
  assert.equal(validateOkcheonObservation(xml, r, startedAt).cases[0].isWholeTextAnalysisComplete, false);
});
for (const [name, change] of [
  ['표본 누락', r => r.pop()], ['표본 중복', r => r[1] = r[0]], ['다른 기관', r => r[0].profileCode = 'LOCAL_JECHEON_BBS_V1'],
  ['다른 공고', r => r[0].caseCode = 'OKCHEON-1'], ['제목 중단 후 요청', r => r[2].requestReservationsIncludingBodyUpperBound = 1],
  ['제목 중단 후 본문', r => r[2].bodyStatus = 'AVAILABLE'], ['제목 중단 후 첨부', r => r[2].files = r[0].files],
  ['본문 실패', r => r[0].bodyStatus = 'FETCH_FAILED'], ['미완료 발견', r => r[0].discoveryComplete = false],
  ['파일 누락', r => r[0].files = []], ['예상 목록 변경', r => r[0].expectedListedFileCount = 2],
  ['형식 변경', r => r[0].files[0].format = 'HWP'], ['추출 실패', r => r[0].files[0].status = 'FAILED'],
  ['부분 추출 위장', r => r[0].files[0].quality = 'PARTIAL_TEXT'], ['역할 근거 없음', r => delete r[0].files[0].roleAssessment],
  ['원본 잔류', r => r[1].originalFilesRemoved = false], ['정책 승인', r => r[0].isPolicyQaPassed = true],
  ['기대값 승인', r => r[0].isExpectationApproved = true], ['운영 쓰기', r => r[0].productionWriteCount = 1],
  ['과거 관측', r => r[0].observedAt = new Date(startedAt - 1).toISOString()], ['예산 초과', r => r[0].reservedBytesIncludingBodyUpperBound = 83886081],
  ['관리자 검증 누락', r => r[0].requiresFinalAdminVerification = false],
]) test('거부: ' + name, () => { const r = reports(); change(r); assert.throws(() => validateOkcheonObservation(xml, r, startedAt)); });
test('JUnit 미실행·실패·생략과 시각 누락을 거부한다', () => {
  for (const invalid of [xml.replace('tests="3"', 'tests="2"'), xml.replace('failures="0"', 'failures="1"'), xml.replace('skipped="0"', 'skipped="1"')])
    assert.throws(() => validateOkcheonObservation(invalid, reports(), startedAt));
  assert.throws(() => validateOkcheonObservation(xml, reports(), NaN));
});
