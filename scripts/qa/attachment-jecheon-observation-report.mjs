import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateSuite, validateReportTime } from './attachment-contract-report.mjs';

const suiteName = 'com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest';
const cases = [['JECHEON-403587', 1, true, 'HWPX'], ['JECHEON-403530', 1, false, 'HWPX'], ['JECHEON-403490', 2, false, 'HWPX']];
const chungjuCases = [['CHUNGJU-72625', 1, true, 'HWPX'], ['CHUNGJU-72039', 1, true, 'HWPX'], ['CHUNGJU-70852', 1, false, 'HWP']];
const okcheonCases = [['OKCHEON-193369', 1, false, 'HWPX'], ['OKCHEON-193297', 1, false, 'HWPX'], ['OKCHEON-193187', 1, true, 'HWPX']];
const bounded = (n, min, max) => Number.isSafeInteger(n) && n >= min && n <= max;

// 연결·음성 표본·문서 완전성을 분리하며 정상 공고 또는 정책 승인으로 승격하지 않는다.
export function validateJecheonObservation(xml, reports, startedAtMs) {
  return validateObservation(xml, reports, startedAtMs, cases, 'LOCAL_JECHEON_BBS_V1');
}

export function validateChungjuObservation(xml, reports, startedAtMs) {
  return validateObservation(xml, reports, startedAtMs, chungjuCases, 'LOCAL_CHUNGJU_EMINWON_V1');
}

export function validateOkcheonObservation(xml, reports, startedAtMs) {
  return validateObservation(xml, reports, startedAtMs, okcheonCases, 'LOCAL_OKCHEON_BBS_V1');
}

function validateObservation(xml, reports, startedAtMs, fixedCases, profileCode) {
  validateReportTime(startedAtMs, startedAtMs);
  const suite = validateSuite(xml, suiteName);
  if (suite.tests !== 3 || !Array.isArray(reports) || reports.length !== 3
      || new Set(reports.map(r => r?.caseCode)).size !== 3) throw new Error('고정 3공고의 전체 실행 증거가 필요합니다.');
  const summaries = [];
  for (const [code, listed, titleStopped, format] of fixedCases) {
    const r = reports.find(report => report?.caseCode === code);
    const observed = Date.parse(r?.observedAt);
    if (!r || r.scope !== 'OFFICIAL_THREE_STAGE_OBSERVATION_V1' || !Number.isFinite(observed)
        || observed < startedAtMs || observed > Date.now() || r.productionWriteCount !== 0
        || r.isPolicyQaPassed !== false || r.isExpectationApproved !== false || r.originalFilesRemoved !== true
        || r.profileCode !== profileCode || r.expectedListedFileCount !== listed
        || r.maximumRequestReservations !== 44 || r.maximumReservedBytes !== 83886080
        || !bounded(r.requestReservationsIncludingBodyUpperBound, 0, 44)
        || !bounded(r.reservedBytesIncludingBodyUpperBound, 0, 83886080)
        || !Array.isArray(r.files)) throw new Error('고정 공고 관측의 시각·범위·정리·예산 증거가 불완전합니다.');
    if (titleStopped) {
      if (r.status !== 'TITLE_NOT_ELIGIBLE_NOT_FETCHED' || r.titleStage !== 'COMBINATION_NOT_MATCHED'
          || r.titleReason !== 'TITLE_COMBINATION_NOT_MATCHED' || r.files.length !== 0
          || r.requestReservationsIncludingBodyUpperBound !== 0 || r.reservedBytesIncludingBodyUpperBound !== 0
          || r.isWholeTextAnalysisComplete !== false || r.requiresFinalAdminVerification !== false
          || r.bodyStatus != null || r.discoveryStatus != null) throw new Error('제목 미충족 표본이 본문·파일 요청 없이 종료되지 않았습니다.');
      summaries.push({ caseCode: code, status: r.status, observedFileCount: 0, isWholeTextAnalysisComplete: false });
      continue;
    }
    if (r.status !== 'OBSERVED_NOT_VALIDATED' || r.titleStage !== 'COMBINATION_MATCHED'
        || r.bodyStageComplete !== true || r.bodyStatus !== 'AVAILABLE' || r.discoveryStatus !== 'FOUND'
        || r.discoveryComplete !== true || r.discoveredFileCount !== listed || r.files.length !== listed
        || r.requiresFinalAdminVerification !== true || r.requestReservationsIncludingBodyUpperBound < 3
        || r.reservedBytesIncludingBodyUpperBound < 1 || !['ACCEPTED', 'REVIEW_REQUIRED'].includes(r.decisionStatus))
      throw new Error('제목 통과 공고의 본문·전체 첨부·종합 판정 증거가 불완전합니다.');
    for (const f of r.files) {
      if (f?.status !== 'OBSERVED' || f.format !== format || !bounded(f.bytes, 1, 20971520)
          || !['COMPLETE_TEXT', 'PARTIAL_TEXT', 'OCR_REQUIRED', 'ENCRYPTED', 'CORRUPT', 'UNSUPPORTED', 'LIMIT_EXCEEDED'].includes(f.quality))
        throw new Error('전체 파일 중 미실행·실패·형식 변경이 있습니다.');
      if (f.quality === 'COMPLETE_TEXT' && (!bounded(f.characterCount, 1, 1000000)
          || !bounded(f.blockCount, 1, 20000) || !f.roleAssessment
          || !['NOTICE', 'GUIDE', 'FORM', 'REFERENCE', 'UNKNOWN'].includes(f.roleAssessment.roleCode)))
        throw new Error('완전 추출 파일의 텍스트·블록·역할 근거가 없습니다.');
    }
    const complete = r.files.every(f => f.quality === 'COMPLETE_TEXT');
    if (r.isWholeTextAnalysisComplete !== complete) throw new Error('문서 완전성 표시와 실제 파일 품질이 다릅니다.');
    summaries.push({ caseCode: code, status: r.status, observedFileCount: r.files.length,
      qualities: r.files.map(f => f.quality), roles: r.files.map(f => f.roleAssessment?.roleCode ?? 'UNKNOWN'),
      isWholeTextAnalysisComplete: complete, requiresFinalAdminVerification: true });
  }
  const stoppedCount = fixedCases.filter(c => c[2]).length;
  return { referenceCount: fixedCases.length, tests: suite.tests, titleStoppedCount: stoppedCount, observedNoticeCount: fixedCases.length - stoppedCount,
    cases: summaries, productionWriteCount: 0, isPolicyQaPassed: false, isExpectationApproved: false };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const startedAt = Number(process.env.JECHEON_OBSERVATION_STARTED_AT);
    const paths = ['build/test-results/attachmentBbsOfficialFileObservation/TEST-' + suiteName + '.xml',
      ...cases.map(([code]) => `build/reports/attachment-bbs-official-observation/${code}.json`)];
    const values = paths.map(path => {
      const full = resolve(path), stat = statSync(full);
      validateReportTime(stat.mtimeMs, startedAt);
      if (!stat.isFile() || stat.size > 262144) throw new Error();
      return readFileSync(full, 'utf8');
    });
    console.log(JSON.stringify(validateJecheonObservation(values[0], values.slice(1).map(v => JSON.parse(v)), startedAt)));
  } catch {
    console.error('제천 관측 보고서가 없거나 현재 실행·전체 표본·필수 검증 조건을 충족하지 못했습니다.');
    process.exitCode = 1;
  }
}
