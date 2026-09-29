import test from 'node:test';
import assert from 'node:assert/strict';
import {importReceipt,selectLatestSamples,importLocalCollectionReport} from './attachment-collection-receipts.mjs';
import {assessSample} from './attachment-collection-stage.mjs';
const hash='a'.repeat(64);
const inventory={targets:[{providerCode:'LOCAL_GOV_NOTICE',localSourceCode:'LGS-000001',profiles:[{profileCode:'PROFILE'}]}]};
const observation=()=>({scope:'OFFICIAL_THREE_STAGE_OBSERVATION_V1',caseCode:'CASE-1',profileCode:'PROFILE',profileHash:hash,
  observedAt:'2026-09-28T09:00:00.123456789Z',productionWriteCount:0,titleStage:'COMBINATION_MATCHED',
  discoveryStatus:'FOUND',discoveryComplete:true,expectedListedFileCount:1,originalFilesRemoved:true,
  files:[{locatorHash:hash,status:'OBSERVED',downloadAllowed:true,format:'HWP',binaryHash:hash,bytes:1024,quality:'PARTIAL_TEXT'}]});
const envelope=r=>({reports:[{kind:'TEMPORARY_QA_RESULT',unitInactive:true,report:{productionDatabaseUsed:false,
  executionCodeHash:hash,probeCleanupSucceeded:true,transportTemporaryFilesRemoved:true,
  probe:{kind:'BBS_OBSERVATION_PROBE',productionDatabaseUsed:false,executionCodeHash:hash,reports:[r]}}}]});
const run=(r=observation(), mutate=()=>{})=>{const outer=envelope(r);mutate(outer);return importReceipt(Buffer.from(JSON.stringify(outer)),inventory);};
test('imports partial extraction binary metadata without copying source text',()=>{
  const r=observation();r.rawText='PRIVATE_CANARY';
  const imported=run(r);assert.equal(assessSample(imported.samples[0]).collectionVerified,true);
  assert.ok(!JSON.stringify(imported).includes('PRIVATE_CANARY'));assert.match(imported.receiptHash,/^[a-f0-9]{64}$/);
});
test('signature failure is not success even with byte count and hash',()=>{
  for(const stage of ['FILE_DOWNLOAD','FILE_SIGNATURE']){const r=observation();r.files[0].status='FAILED';r.files[0].failedStage=stage;
    assert.equal(assessSample(run(r).samples[0]).collectionVerified,false);}
  const r=observation();r.files[0].status='FAILED';r.files[0].failedStage='ISOLATED_EXTRACTION';
  assert.equal(assessSample(run(r).samples[0]).collectionVerified,true);
});
test('worker partial job does not negate successful file downloads',()=>{
  const r={...observation(),scope:'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1',status:'WORKER_DB_API_OBSERVED_NOT_APPROVED',
    workerStatus:'EVALUATED',jobStatus:'PARTIAL_FAILED',discoveredFileCount:1,processedFileCount:1};
  r.files[0].downloadStatus='SUCCEEDED';
  const asWorker=x=>{x.reports[0].report.probe.kind='OFFICIAL_WORKER_PROBE';};
  assert.equal(assessSample(run(r,asWorker).samples[0]).collectionVerified,true);
  r.processedFileCount=0;assert.equal(assessSample(run(r,asWorker).samples[0]).collectionVerified,false);
});
test('cleanup and isolated provenance are mandatory',()=>{
  const imported=run(observation(),x=>x.reports[0].report.transportTemporaryFilesRemoved=false);
  assert.equal(assessSample(imported.samples[0]).status,'CLEANUP_PENDING');
  for(const mutate of [x=>x.reports[0].report.productionDatabaseUsed=true,
    x=>x.reports[0].report.executionCodeHash='b'.repeat(64)])assert.throws(()=>run(observation(),mutate),/COLLECTION_RECEIPT_INVALID/);
  assert.throws(()=>run({...observation(),productionWriteCount:1}),/COLLECTION_RECEIPT_INVALID/);
});
test('unknown schemas and absent probes cannot silently become success',()=>{
  assert.equal(run({...observation(),scope:'DIAGNOSTIC_ONLY'}).status,'UNSUPPORTED_REPORT_SCHEMA');
  assert.equal(run(observation(),x=>delete x.reports[0].report.probe).status,'NO_PROBE_EVIDENCE');
  assert.throws(()=>run({...observation(),profileCode:'OTHER'}),/COLLECTION_RECEIPT_INVALID/);
});
test('latest failure wins over older success without duplicate sample inflation',()=>{
  const old=run();const r=observation();r.observedAt='2026-09-28T09:00:00.123456790Z';r.files[0].status='FAILED';
  const latest=run(r);assert.equal(selectLatestSamples([latest,old]).length,1);
  assert.equal(assessSample(selectLatestSamples([latest,old])[0]).collectionVerified,false);
  const conflict=structuredClone(old);conflict.samples[0].discoveryComplete=false;
  assert.throws(()=>selectLatestSamples([old,conflict]),/COLLECTION_RECEIPT_INVALID/);
});
test('verified title stop is kept as non-download evidence',()=>{
  const r={...observation(),status:'TITLE_NOT_ELIGIBLE_NOT_FETCHED',titleStage:'COMBINATION_NOT_MATCHED',files:[],discoveryComplete:false};
  delete r.discoveryStatus;
  assert.equal(run(r).samples[0].titleStatus,'NOT_ELIGIBLE');
  assert.equal(assessSample(run(r).samples[0]).collectionVerified,false);
});

test('local download report stays distinct from remote worker evidence',()=>{
  const r={...observation(),collectionOnly:true,isExtractionVerified:false,isWholeTextAnalysisComplete:false,
    isPolicyQaPassed:false,isExpectationApproved:false,collectionStageComplete:true,status:'COLLECTION_ONLY_OBSERVED_NOT_APPROVED'};
  r.files[0].status='DOWNLOADED';delete r.files[0].quality;
  const runLocal=()=>importLocalCollectionReport(Buffer.from(JSON.stringify(r)),inventory,hash);
  const result=runLocal();assert.equal(assessSample(result.samples[0]).collectionVerified,true);
  assert.equal(result.samples[0].evidenceScope,'LOCAL_COLLECTION_ONLY_REPORT');
  r.files[0].quality='COMPLETE_TEXT';assert.throws(runLocal,/COLLECTION_RECEIPT_INVALID/);delete r.files[0].quality;
  r.collectionOnly=false;assert.throws(runLocal,/COLLECTION_RECEIPT_INVALID/);
});

test('local title stop requires no requests or discovery and cannot count as download',()=>{
  const r={...observation(),collectionOnly:true,isExtractionVerified:false,isWholeTextAnalysisComplete:false,
    isPolicyQaPassed:false,isExpectationApproved:false,status:'TITLE_NOT_ELIGIBLE_NOT_FETCHED',
    titleStage:'COMBINATION_NOT_MATCHED',files:[],requestReservationsIncludingBodyUpperBound:0};
  delete r.discoveryStatus;delete r.discoveryComplete;
  const runLocal=()=>importLocalCollectionReport(Buffer.from(JSON.stringify(r)),inventory,hash);
  assert.equal(assessSample(runLocal().samples[0]).status,'TITLE_STOPPED_NOT_FETCHED');
  assert.equal(assessSample(runLocal().samples[0]).collectionVerified,false);
  r.requestReservationsIncludingBodyUpperBound=1;assert.throws(runLocal,/COLLECTION_RECEIPT_INVALID/);r.requestReservationsIncludingBodyUpperBound=0;
  r.titleStage='COMBINATION_MATCHED';assert.throws(runLocal,/COLLECTION_RECEIPT_INVALID/);
  r.titleStage='UNKNOWN';assert.throws(runLocal,/COLLECTION_RECEIPT_INVALID/);
});

test('partial discovery retains independently verified detail identity and successful binaries',()=>{
  const r={...observation(),collectionOnly:true,isExtractionVerified:false,isWholeTextAnalysisComplete:false,
    isPolicyQaPassed:false,isExpectationApproved:false,collectionStageComplete:false,
    status:'COLLECTION_ONLY_PARTIAL_NOT_APPROVED',detailIdentityVerified:true,discoveryStatus:'FAILED',discoveryComplete:false};
  r.files[0].status='DOWNLOADED';delete r.files[0].quality;
  const result=importLocalCollectionReport(Buffer.from(JSON.stringify(r)),inventory,hash);
  assert.equal(result.samples[0].detailIdentityVerified,true);
  assert.equal(assessSample(result.samples[0]).downloadedFileCount,1);
  assert.equal(assessSample(result.samples[0]).collectionVerified,false);
});
