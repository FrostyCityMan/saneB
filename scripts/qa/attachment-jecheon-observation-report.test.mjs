import test from 'node:test';
import assert from 'node:assert/strict';
import { validateJecheonObservation } from './attachment-jecheon-observation-report.mjs';

const startedAt = Date.now() - 1000;
const xml = '<testsuite name="com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest" tests="3" failures="0" errors="0" skipped="0"></testsuite>';
function reports() {
  return [['JECHEON-403587', 1], ['JECHEON-403530', 1], ['JECHEON-403490', 2]].map(([caseCode, count], index) => {
    const common = { caseCode, scope: 'OFFICIAL_THREE_STAGE_OBSERVATION_V1', observedAt: new Date(startedAt + 1).toISOString(),
      profileCode: 'LOCAL_JECHEON_BBS_V1', productionWriteCount: 0, isPolicyQaPassed: false, isExpectationApproved: false,
      originalFilesRemoved: true, expectedListedFileCount: count, maximumRequestReservations: 44, maximumReservedBytes: 83886080 };
    if (!index) return { ...common, status: 'TITLE_NOT_ELIGIBLE_NOT_FETCHED', titleStage: 'COMBINATION_NOT_MATCHED',
      titleReason: 'TITLE_COMBINATION_NOT_MATCHED', files: [], requestReservationsIncludingBodyUpperBound: 0,
      reservedBytesIncludingBodyUpperBound: 0, isWholeTextAnalysisComplete: false, requiresFinalAdminVerification: false };
    return { ...common, status: 'OBSERVED_NOT_VALIDATED', titleStage: 'COMBINATION_MATCHED', bodyStageComplete: true,
      bodyStatus: 'AVAILABLE', discoveryStatus: 'FOUND', discoveryComplete: true, discoveredFileCount: count,
      requiresFinalAdminVerification: true, requestReservationsIncludingBodyUpperBound: 4 + count, reservedBytesIncludingBodyUpperBound: 3000000,
      decisionStatus: 'REVIEW_REQUIRED', isWholeTextAnalysisComplete: true,
      files: Array.from({ length: count }, () => ({ status: 'OBSERVED', format: 'HWPX', bytes: 80000, quality: 'COMPLETE_TEXT',
        characterCount: 2000, blockCount: 100, roleAssessment: { roleCode: 'UNKNOWN' } })) };
  });
}
test('음성 1건과 전체 파일 3개의 관측을 정책 승인과 구분한다', () => {
  const r = validateJecheonObservation(xml, reports(), startedAt);
  assert.equal(r.tests, 3); assert.equal(r.titleStoppedCount, 1); assert.equal(r.observedNoticeCount, 2);
  assert.equal(r.isPolicyQaPassed, false); assert.equal(r.isExpectationApproved, false);
});
test('부분 추출은 완전성 false로만 관측할 수 있다', () => {
  const r = reports(); r[2].files[1].quality = 'PARTIAL_TEXT'; r[2].isWholeTextAnalysisComplete = false;
  assert.equal(validateJecheonObservation(xml, r, startedAt).cases[2].isWholeTextAnalysisComplete, false);
  r[2].isWholeTextAnalysisComplete = true;
  assert.throws(() => validateJecheonObservation(xml, r, startedAt));
});
for (const [name, mutate] of [
  ['전체 표본 누락', r => r.pop()], ['중복 표본', r => r[2] = r[1]], ['다른 기관', r => r[1].profileCode = 'OTHER'],
  ['과거 관측', r => r[1].observedAt = new Date(startedAt - 1).toISOString()], ['미래 관측', r => r[1].observedAt = '2099-01-01T00:00:00Z'],
  ['운영 쓰기', r => r[1].productionWriteCount = 1], ['정책 승인', r => r[1].isPolicyQaPassed = true],
  ['기대값 승인', r => r[1].isExpectationApproved = true], ['원본 잔류', r => r[1].originalFilesRemoved = false],
  ['제목 차단 뒤 요청', r => r[0].requestReservationsIncludingBodyUpperBound = 1],
  ['제목 차단 뒤 본문', r => r[0].bodyStatus = 'AVAILABLE'], ['제목 차단 뒤 파일', r => r[0].files = [{}]],
  ['제목 판정 변경', r => r[0].titleStage = 'COMBINATION_MATCHED'], ['본문 실패', r => r[1].bodyStageComplete = false],
  ['발견 불완전', r => r[2].discoveryComplete = false], ['전체 파일 누락', r => r[2].files.pop()],
  ['파일 미실행', r => r[2].files[1].status = 'NOT_RUN'], ['예상 형식 변경', r => r[1].files[0].format = 'PDF'],
  ['완전 추출 근거 누락', r => delete r[1].files[0].roleAssessment], ['추출 문자 없음', r => r[1].files[0].characterCount = 0],
  ['허용되지 않은 역할', r => r[1].files[0].roleAssessment.roleCode = 'UNTRUSTED_TEXT'],
  ['요청 상한 초과', r => r[1].requestReservationsIncludingBodyUpperBound = 45], ['용량 상한 초과', r => r[1].reservedBytesIncludingBodyUpperBound = 83886081],
  ['관리자 검증 누락', r => r[1].requiresFinalAdminVerification = false], ['첨부 자동 제외', r => r[1].decisionStatus = 'EXCLUDED'],
]) test('거부: ' + name, () => {
  const r = reports(); mutate(r); assert.throws(() => validateJecheonObservation(xml, r, startedAt));
});
for (const [name, invalid] of [['실행 누락', xml.replace('tests="3"', 'tests="2"')], ['생략', xml.replace('skipped="0"', 'skipped="1"')],
  ['실패', xml.replace('failures="0"', 'failures="1"')], ['오류', xml.replace('errors="0"', 'errors="1"')]])
  test('JUnit ' + name + '은 거부한다', () => assert.throws(() => validateJecheonObservation(invalid, reports(), startedAt)));
test('실행 시작 시각이 없으면 거부한다', () => assert.throws(() => validateJecheonObservation(xml, reports(), NaN)));
