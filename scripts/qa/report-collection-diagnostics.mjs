/** 읽기 전용 진단. --pending은 현재 코드 미확인, --unrecovered는 과거 지문 포함 최신 표본 미확보다. */
import fs from 'node:fs';
import {readCollectionDiagnostics} from './attachment-collection-diagnostics.mjs';
const index = JSON.parse(fs.readFileSync('docs/backend/attachment-collection-receipt-index-2026-09-28.json', 'utf8'));
const result = readCollectionDiagnostics(index, path => fs.readFileSync(path));
const {regions, ...summary} = result;
console.log(JSON.stringify(process.argv.includes('--unrecovered') ? {...summary, regions: regions.filter(r => !r.history.latestSampleHasDownload)}
  : process.argv.includes('--pending') ? {...summary, regions: regions.filter(r => !r.hasObservedDownload)}
  : process.argv.includes('--details') ? result : summary, null, 2));
