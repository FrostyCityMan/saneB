/** 보관된 원본이 있는 작업 공간에서만 실행. 파일 읽기와 stdout 출력 외 부작용이 없다. */
import fs from 'node:fs';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import {importReceipt,selectLatestSamples,importLocalCollectionReport} from './attachment-collection-receipts.mjs';
import {buildCollectionPlan} from './attachment-collection-stage.mjs';
import {importGithubCollectionReport,validateGithubCollectionEntry} from './attachment-github-collection-receipts.mjs';
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const index=JSON.parse(fs.readFileSync('docs/backend/attachment-collection-receipt-index-2026-09-28.json','utf8'));
const inventoryBytes=fs.readFileSync(index.inventoryPath);
assert.equal(sha(inventoryBytes),index.inventorySha256,'INVENTORY_HASH_CHANGED');
const inventory=JSON.parse(inventoryBytes);
const receipts=index.receipts.map(entry=>{
  if(entry.kind==='GITHUB_COLLECTION_ONLY') validateGithubCollectionEntry(entry);
  else if(entry.kind==='LOCAL_COLLECTION_ONLY') assert.match(entry.path,/^build\/reports\/attachment-regional-collection\/[A-Z0-9_-]+\.json$/);
  else assert.match(entry.path,/^build\/temporary-bbs-qa-[a-f0-9]{32}\/result(?:-utf8)?\.json$/);
  const bytes=fs.readFileSync(entry.path);
  assert.equal(sha(bytes),entry.receiptHash,'RECEIPT_HASH_CHANGED');
  const imported=entry.kind==='GITHUB_COLLECTION_ONLY'?importGithubCollectionReport(bytes,inventory,entry):entry.kind==='LOCAL_COLLECTION_ONLY'?importLocalCollectionReport(bytes,inventory,entry.producerClassHash):importReceipt(bytes,inventory);
  assert.equal(imported.status,entry.status);assert.equal(imported.samples.length,entry.sampleCount);
  return imported;
});
const samples=selectLatestSamples(receipts);
assert.deepEqual(samples,index.samples,'IMPORTED_EVIDENCE_CHANGED');
const actual=buildCollectionPlan(inventory,samples);
const ledger=JSON.parse(fs.readFileSync('docs/backend/attachment-collection-regional-ledger-2026-09-28.json','utf8'));
assert.equal(ledger.inputInventorySha256,index.inventorySha256,'LEDGER_INVENTORY_HASH_CHANGED');
assert.equal(ledger.importedReceiptCount,receipts.length,'LEDGER_RECEIPT_COUNT_CHANGED');
assert.deepEqual(actual.summary,ledger.summary);
assert.deepEqual(actual.regions,ledger.regions);
console.log(JSON.stringify({status:'ARCHIVED_EVIDENCE_REPRODUCED',currentHttpRequests:0,
  productionWriteCount:0,receiptCount:receipts.length,sampleCount:samples.length,summary:actual.summary}));
