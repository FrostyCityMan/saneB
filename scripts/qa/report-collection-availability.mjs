/** 읽기 전용: 보관된 수집 근거 대장을 입력으로 부분 수집 현황을 표시한다. */
import fs from 'node:fs';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import {buildCollectionAvailability} from './attachment-collection-availability.mjs';
const index=JSON.parse(fs.readFileSync('docs/backend/attachment-collection-receipt-index-2026-09-28.json','utf8'));
const bytes=fs.readFileSync(index.inventoryPath);
assert.equal(createHash('sha256').update(bytes).digest('hex'),index.inventorySha256,'INVENTORY_HASH_CHANGED');
const result=buildCollectionAvailability(JSON.parse(bytes),index.samples);
console.log(JSON.stringify(process.argv.includes('--details')?result:{scope:result.scope,
  inventoryObservedAt:result.inventoryObservedAt,currentHttpRequests:0,productionWriteCount:0,summary:result.summary},null,2));
