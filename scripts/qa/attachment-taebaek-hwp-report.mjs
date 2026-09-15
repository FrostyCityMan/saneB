import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateSuite, validateReportTime } from './attachment-contract-report.mjs';

const suiteName = 'com.saneb.db.AnnouncementAttachmentOfficialWorkerIntegrationTest';
const bounded = (value, min, max) => Number.isSafeInteger(value) && value >= min && value <= max;

// 성공한 연결 시험과 문서 완전성을 분리한다. 원문/예외/파일명은 출력하지 않는다.
export function validateHwpWorkerReport(xml, report) {
  const suite = validateSuite(xml, suiteName);
  if (suite.tests !== 1) throw new Error('태백 HWP 고정 1건 전체 실행이 필요합니다.');
  if (!report || report.caseCode !== 'TAEBAEK-176153' || report.scope !== 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1'
      || report.status !== 'WORKER_DB_API_OBSERVED_NOT_APPROVED'
      || report.productionWriteCount !== 0 || report.isPolicyQaPassed !== false || report.isExpectationApproved !== false
      || report.isAuthenticatedBrowserE2e !== false || report.originalFilesRemoved !== true || report.remainingResourceLeases !== 0
      || report.requiresFinalAdminVerification !== true || report.bodyStageComplete !== true || report.bodyStatus !== 'AVAILABLE'
      || report.discoveryComplete !== true || report.discoveredFileCount !== 1 || report.processedFileCount !== 1
      || report.extractorCalls !== 1 || report.maximumRequestReservations !== 44 || report.maximumReservedBytes !== 83886080
      || !bounded(report.requestReservationsIncludingBodyUpperBound, 1, 44)
      || !bounded(report.reservedBytesIncludingBodyUpperBound, 1, 83886080)
      || !Array.isArray(report.files) || report.files.length !== 1) throw new Error('태백 HWP 연결·범위·정리 증거가 불완전합니다.');
  const file = report.files[0];
  if (file?.format !== 'HWP' || !['COMPLETE_TEXT', 'PARTIAL_TEXT'].includes(file.quality)
      || !bounded(file.characterCount, 1, 1000000) || !bounded(file.blockCount, 1, 20000)
      || !file.hwpStructure || typeof file.hwpStructure !== 'object' || Array.isArray(file.hwpStructure)
      || !bounded(file.hwpStructure.sectionCount, 1, 33554432) || !bounded(file.hwpStructure.recordCount, 1, 33554432)
      || !bounded(file.hwpStructure.maximumLevel, 0, 1023) || !Array.isArray(file.hwpStructure.recordTypes)
      || file.hwpStructure.recordTypes.length < 1 || file.hwpStructure.recordTypes.length > 1024
      || report.isWholeTextAnalysisComplete !== (file.quality === 'COMPLETE_TEXT'))
    throw new Error('실제 HWP 추출 또는 문서 완전성 증거가 일치하지 않습니다.');
  return { caseCode: report.caseCode, tests: suite.tests, quality: file.quality, characterCount: file.characterCount,
    blockCount: file.blockCount, isWholeTextAnalysisComplete: report.isWholeTextAnalysisComplete,
    productionWriteCount: 0, isPolicyQaPassed: false, requiresFinalAdminVerification: true };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const paths = ['build/test-results/attachmentTaebaekHwpWorkerIntegrationTest/TEST-' + suiteName + '.xml',
      'build/reports/attachment-taebaek-hwp-worker/TAEBAEK-176153.json'];
    const values = paths.map(path => {
      const full = resolve(path), stat = statSync(full);
      validateReportTime(stat.mtimeMs, Number(process.env.TAEBAEK_HWP_QA_STARTED_AT));
      if (!stat.isFile() || stat.size > 262144) throw new Error();
      return readFileSync(full, 'utf8');
    });
    console.log(JSON.stringify(validateHwpWorkerReport(values[0], JSON.parse(values[1]))));
  } catch {
    console.error('태백 HWP 실행 보고서가 없거나 현재 실행·필수 검증 조건을 충족하지 못했습니다.');
    process.exitCode = 1;
  }
}
