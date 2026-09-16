import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateReportTime } from './attachment-contract-report.mjs';
import { validateFixedWorkerReport } from './attachment-jecheon-worker-report.mjs';

const cases = [['BOEUN-221499', 1, 'HWPX'], ['BOEUN-221497', 1, 'HWPX'], ['BOEUN-218812', 1, 'PDF']];

// HWPX 두 파일과 PDF 한 파일을 서로 바꿔 세거나 일부 성공만으로 승인하지 않는다.
export function validateBoeunWorkerReport(xml, reports, startedAtMs) {
  return validateFixedWorkerReport(xml, reports, startedAtMs, cases, 'LOCAL_BOEUN_BBS_V1');
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const startedAt = Number(process.env.BOEUN_WORKER_QA_STARTED_AT);
    const paths = ['build/test-results/attachmentBoeunWorkerIntegrationTest/TEST-com.saneb.db.AnnouncementAttachmentOfficialWorkerIntegrationTest.xml',
      ...cases.map(([code]) => `build/reports/attachment-boeun-worker/${code}.json`)];
    const values = paths.map(path => {
      const full = resolve(path), stat = statSync(full);
      validateReportTime(stat.mtimeMs, startedAt);
      if (!stat.isFile() || stat.size > 262144) throw new Error();
      return readFileSync(full, 'utf8');
    });
    console.log(JSON.stringify(validateBoeunWorkerReport(values[0], values.slice(1).map(value => JSON.parse(value)), startedAt)));
  } catch {
    console.error('보은 worker 보고서가 없거나 현재 실행·고정 3공고·HWPX/PDF 전체 처리 근거가 일치하지 않습니다.');
    process.exitCode = 1;
  }
}
