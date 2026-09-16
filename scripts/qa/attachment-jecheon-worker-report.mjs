import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateSuite, validateReportTime } from './attachment-contract-report.mjs';

const suiteName = 'com.saneb.db.AnnouncementAttachmentOfficialWorkerIntegrationTest';
const cases = [['JECHEON-403587', 0], ['JECHEON-403530', 1], ['JECHEON-403490', 2]];
const bounded = (value, min, max) => Number.isSafeInteger(value) && value >= min && value <= max;
const hash = value => typeof value === 'string' && /^[0-9a-f]{64}$/.test(value);

// 연결 성공과 후보 판정을 분리한다. 원문·파일명·경로·예외 메시지는 결과에 복사하지 않는다.
export function validateJecheonWorkerReport(xml, reports, startedAtMs) {
  validateReportTime(startedAtMs, startedAtMs);
  const suite = validateSuite(xml, suiteName);
  if (suite.tests !== 3 || !Array.isArray(reports) || reports.length !== 3
      || new Set(reports.map(r => r?.caseCode)).size !== 3) throw new Error('제천 고정 3공고 전체 실행 증거가 필요합니다.');
  const summaries = [];
  for (const [code, count] of cases) {
    const r = reports.find(value => value?.caseCode === code);
    const observed = Date.parse(r?.observedAt);
    if (!r || r.scope !== 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1' || !Number.isFinite(observed)
        || observed < startedAtMs || observed > Date.now() || r.productionWriteCount !== 0
        || r.isPolicyQaPassed !== false || r.isExpectationApproved !== false || r.isAuthenticatedBrowserE2e !== false
        || r.originalFilesRemoved !== true || r.remainingResourceLeases !== 0
        || r.titleInputSource !== 'FIXED_OFFICIAL_SAMPLE' || r.policySource !== 'EPHEMERAL_DB_FIXTURE'
        || r.maximumRequestReservations !== 44 || r.maximumReservedBytes !== 83886080
        || !bounded(r.requestReservationsIncludingBodyUpperBound, 0, 44)
        || !bounded(r.reservedBytesIncludingBodyUpperBound, 0, 83886080) || !Array.isArray(r.files))
      throw new Error('제천 worker 시각·실행 범위·자원 정리·요청 상한 근거가 불완전합니다.');
    if (count === 0) {
      if (r.status !== 'TITLE_EXCLUDED_NOT_FETCHED' || r.titleStage !== 'COMBINATION_NOT_MATCHED'
          || r.titleReason !== 'TITLE_COMBINATION_NOT_MATCHED' || r.files.length !== 0
          || r.requestReservationsIncludingBodyUpperBound !== 0 || r.reservedBytesIncludingBodyUpperBound !== 0
          || r.isWholeTextAnalysisComplete !== false || r.bodyStatus != null || r.workerStatus != null
          || r.extractorCalls != null || r.discoveredFileCount != null)
        throw new Error('제목 제외 공고는 본문·첨부·worker 요청 없이 종료되어야 합니다.');
      summaries.push({ caseCode: code, status: r.status, processedFileCount: 0 });
      continue;
    }
    if (r.status !== 'WORKER_DB_API_OBSERVED_NOT_APPROVED' || r.titleStage !== 'COMBINATION_MATCHED'
        || r.bodyStatus !== 'AVAILABLE' || r.bodyStageComplete !== true || r.profileCode !== 'LOCAL_JECHEON_BBS_V1'
        || !hash(r.profileHash) || !hash(r.extractorConfigHash) || typeof r.extractorVersion !== 'string'
        || r.workerStatus !== 'EVALUATED' || r.jobStatus !== 'SUCCEEDED' || r.requiresFinalAdminVerification !== true
        || r.discoveryComplete !== true || r.discoveredFileCount !== count || r.processedFileCount !== count
        || r.extractorCalls !== count || r.files.length !== count || r.requestReservationsIncludingBodyUpperBound < count + 2
        || r.reservedBytesIncludingBodyUpperBound < 1 || !['ACCEPTED', 'REVIEW_REQUIRED'].includes(r.decisionStatus))
      throw new Error('본문·전체 첨부·실제 worker·DB/API 처리 증거가 일치하지 않습니다.');
    for (const f of r.files) {
      if (f?.format !== 'HWPX' || f.downloadStatus !== 'SUCCEEDED' || f.downloadErrorCode != null
          || !bounded(f.bytes, 1, 20971520) || !hash(f.binaryHash) || !hash(f.locatorHash)
          || !['COMPLETE_TEXT', 'PARTIAL_TEXT', 'OCR_REQUIRED', 'ENCRYPTED', 'CORRUPT', 'UNSUPPORTED', 'LIMIT_EXCEEDED'].includes(f.quality))
        throw new Error('지원 파일 전체의 다운로드·추출 결과가 필요합니다.');
      if (['COMPLETE_TEXT', 'PARTIAL_TEXT'].includes(f.quality)
          && (!bounded(f.characterCount, 1, 1000000) || !bounded(f.blockCount, 1, 20000) || !hash(f.textHash)))
        throw new Error('저장된 추출 텍스트와 블록 근거가 없습니다.');
      if (f.quality === 'COMPLETE_TEXT' && (!f.roleFingerprint || f.roleOrigin !== 'TEXT_RULE'
          || !['NOTICE', 'GUIDE', 'FORM', 'REFERENCE', 'UNKNOWN'].includes(f.roleCode)
          || f.roleFingerprint.roleCode !== f.roleCode || f.roleFingerprint.ruleVersion !== r.roleRuleVersion
          || f.roleFingerprint.textHash !== f.textHash || !hash(f.roleFingerprint.rulesHash)
          || !hash(f.roleFingerprint.blocksHash) || !hash(f.roleFingerprint.assessmentHash)))
        throw new Error('완전 추출 파일의 역할 판정과 저장 근거가 일치하지 않습니다.');
    }
    if (new Set(r.files.map(f => f.locatorHash)).size !== count) throw new Error('같은 파일을 중복 처리한 결과입니다.');
    const complete = r.files.every(f => f.quality === 'COMPLETE_TEXT');
    if (r.isWholeTextAnalysisComplete !== complete) throw new Error('부분 추출을 전체 완료로 표시할 수 없습니다.');
    if (!complete && (r.processingStatus !== 'TECHNICAL_EXCEPTION' || r.decisionStatus !== 'REVIEW_REQUIRED'))
      throw new Error('불완전 파일은 기술 예외와 검수 상태를 유지해야 합니다.');
    if (complete && (!['READY_FOR_FINAL_REVIEW', 'FINAL_REVIEW_EXCEPTION'].includes(r.processingStatus)
        || (r.files.some(f => f.roleCode === 'UNKNOWN')
          && (r.processingStatus !== 'FINAL_REVIEW_EXCEPTION' || r.decisionStatus !== 'REVIEW_REQUIRED'))))
      throw new Error('분석 완료·역할 미확정·최종 검수 상태를 구분해야 합니다.');
    summaries.push({ caseCode: code, status: r.status, processedFileCount: count,
      qualities: r.files.map(f => f.quality), roles: r.files.map(f => f.roleCode),
      isWholeTextAnalysisComplete: complete, processingStatus: r.processingStatus, requiresFinalAdminVerification: true });
  }
  return { tests: suite.tests, titleStoppedCount: 1, observedNoticeCount: 2, processedFileCount: 3,
    cases: summaries, productionWriteCount: 0, isPolicyQaPassed: false, isExpectationApproved: false, isAuthenticatedBrowserE2e: false };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const startedAt = Number(process.env.JECHEON_WORKER_QA_STARTED_AT);
    const paths = ['build/test-results/attachmentJecheonWorkerIntegrationTest/TEST-' + suiteName + '.xml',
      ...cases.map(([code]) => `build/reports/attachment-jecheon-worker/${code}.json`)];
    const values = paths.map(path => {
      const full = resolve(path), stat = statSync(full);
      validateReportTime(stat.mtimeMs, startedAt);
      if (!stat.isFile() || stat.size > 262144) throw new Error();
      return readFileSync(full, 'utf8');
    });
    console.log(JSON.stringify(validateJecheonWorkerReport(values[0], values.slice(1).map(value => JSON.parse(value)), startedAt)));
  } catch {
    console.error('제천 worker 보고서가 없거나 현재 실행·전체 표본·필수 검증 조건을 충족하지 못했습니다.');
    process.exitCode = 1;
  }
}
