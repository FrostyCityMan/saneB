import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateReportTime } from './attachment-contract-report.mjs';
import { validateChungjuObservation } from './attachment-jecheon-observation-report.mjs';
export { validateChungjuObservation };

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const startedAt = Number(process.env.CHUNGJU_OBSERVATION_STARTED_AT);
    const paths = ['build/test-results/attachmentBbsOfficialFileObservation/TEST-com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.xml',
      ...['72625', '72039', '70852'].map(id => `build/reports/attachment-bbs-official-observation/CHUNGJU-${id}.json`)];
    const values = paths.map(path => {
      const stat = statSync(path); validateReportTime(stat.mtimeMs, startedAt);
      if (!stat.isFile() || stat.size > 262144) throw new Error();
      return readFileSync(path, 'utf8');
    });
    console.log(JSON.stringify(validateChungjuObservation(values[0], values.slice(1).map(v => JSON.parse(v)), startedAt)));
  } catch {
    console.error('충주 관측 보고서가 없거나 현재 실행·전체 표본·필수 검증 조건을 충족하지 못했습니다.');
    process.exitCode = 1;
  }
}
