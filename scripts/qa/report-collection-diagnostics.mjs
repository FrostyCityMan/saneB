/** 읽기 전용 단계별 진단. --pending은 다운로드 미확인 수집원만, --details는 전체 상세를 표시한다. */
import fs from 'node:fs';
import {readCollectionDiagnostics} from './attachment-collection-diagnostics.mjs';
const index = JSON.parse(fs.readFileSync('docs/backend/attachment-collection-receipt-index-2026-09-28.json', 'utf8'));
const result = readCollectionDiagnostics(index, path => fs.readFileSync(path));
const {regions, ...summary} = result;
console.log(JSON.stringify(process.argv.includes('--pending') ? {...summary, regions: regions.filter(r => !r.hasObservedDownload)}
  : process.argv.includes('--details') ? result : summary, null, 2));
