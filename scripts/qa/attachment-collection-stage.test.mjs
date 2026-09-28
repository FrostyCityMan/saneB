import test from 'node:test';
import assert from 'node:assert/strict';
import {assessSample, buildCollectionPlan} from './attachment-collection-stage.mjs';
const hash='a'.repeat(64), other='b'.repeat(64);
const file=()=>({locatorHash:hash,status:'SUCCEEDED',format:'HWP',binaryHash:hash,bytes:512,signatureVerified:true});
const sample=(extra={})=>({sourceCode:'LGS-000001',caseCode:'CASE-1',profileCode:'PROFILE_1',profileHash:hash,
  receiptHash:hash,expectedFileCount:1,files:[file()],discoveryStatus:'FOUND',discoveryComplete:true,
  titleStatus:'ELIGIBLE',detailIdentityVerified:true,originalFilesRemoved:true,productionWriteCount:0,...extra});
const target=(extra={})=>({providerCode:'LOCAL_GOV_NOTICE',localSourceCode:'LGS-000001',listParserProfileCode:'SPRING_BBS',
  enabled:true,bindingStatus:'SYSTEM_BINDING_MATCHED',profiles:[{profileCode:'PROFILE_1',profileHash:hash}],...extra});
const inventory=(targets=[target()])=>({scope:'OPERATING_TARGET_SNAPSHOT_VS_LOCAL_CODE_REGISTRY',observedAt:'synthetic',targets});

test('HWP partial text does not invalidate a verified binary download or approve extraction',()=>{
  const s=sample({quality:'PARTIAL_TEXT',text:'PRIVATE_CANARY',sourceUrl:'PRIVATE_CANARY'});
  assert.equal(assessSample(s).status,'ALL_FILES_DOWNLOADED');
  const report=buildCollectionPlan(inventory(),[s]);
  assert.equal(report.isExtractionVerified,false);assert.equal(report.isPolicyQaPassed,false);
  assert.equal(report.summary.collectionVerifiedRegionCount,0);assert.ok(!JSON.stringify(report).includes('PRIVATE_CANARY'));
});
test('a region needs three observed samples including a real attachment',()=>{
  const samples=[1,2,3].map(i=>sample({caseCode:`CASE-${i}`}));
  assert.equal(buildCollectionPlan(inventory(),samples).summary.collectionVerifiedRegionCount,1);
  assert.equal(buildCollectionPlan(inventory(),samples.slice(0,2)).summary.remainingRegionCount,1);
});
test('confirmed absence is not discovery failure and cannot alone prove downloads',()=>{
  const none=sample({files:[],expectedFileCount:0,discoveryStatus:'NO_FILES'});
  assert.equal(assessSample(none).status,'NO_FILES_VERIFIED');
  assert.equal(assessSample({...none,discoveryComplete:false}).collectionVerified,false);
  assert.equal(assessSample({...none,discoveryStatus:'FAILED'}).collectionVerified,false);
  const samples=[1,2,3].map(i=>({...none,caseCode:`CASE-${i}`}));
  assert.equal(buildCollectionPlan(inventory(),samples).summary.remainingRegionCount,1);
  samples[0]=sample();assert.equal(buildCollectionPlan(inventory(),samples).summary.collectionVerifiedRegionCount,1);
});
test('one failed missing or unsupported attachment keeps the whole sample incomplete',()=>{
  for(const status of ['FAILED','NOT_RUN','UNSUPPORTED']) {
    const files=[file(),{...file(),locatorHash:other,status,bytes:0}];
    assert.equal(assessSample(sample({files,expectedFileCount:2})).collectionVerified,false);
  }
  assert.equal(assessSample(sample({expectedFileCount:2})).collectionVerified,false);
  assert.equal(assessSample(sample({discoveryStatus:'LIMIT_EXCEEDED'})).collectionVerified,false);
});
test('title gate identity and cleanup cannot be bypassed',()=>{
  for(const patch of [{titleStatus:'NOT_ELIGIBLE'},{titleStatus:'NOT_CHECKED'},{detailIdentityVerified:false},
    {discoveryComplete:false},{originalFilesRemoved:false}]) assert.equal(assessSample(sample(patch)).collectionVerified,false);
});
test('file identity signature counts and status types are validated',()=>{
  for(const patch of [{bytes:0},{bytes:'512'},{bytes:21*1024*1024},{binaryHash:'bad'},
    {format:'EXE'},{signatureVerified:false},{status:'ok'}])
    assert.throws(()=>assessSample(sample({files:[{...file(),...patch}]})),/COLLECTION_EVIDENCE_INVALID/);
  assert.throws(()=>assessSample(sample({files:[file(),file()],expectedFileCount:2})),/COLLECTION_EVIDENCE_INVALID/);
  for(const patch of [{productionWriteCount:1},{productionWriteCount:'0'},{discoveryComplete:'true'},
    {receiptHash:''},{files:null},{expectedFileCount:11}]) assert.throws(()=>assessSample(sample(patch)),/COLLECTION_EVIDENCE_INVALID/);
});
test('missing disabled and national targets remain separate',()=>{
  const report=buildCollectionPlan(inventory([target(),target({localSourceCode:'LGS-000002',profiles:[],bindingStatus:'PROFILE_MISSING'}),
    target({localSourceCode:'LGS-000003',enabled:false}),{providerCode:'BIZINFO'}]));
  assert.deepEqual(report.summary,{allRegionCount:3,activeRegionCount:2,inactiveRegionCount:1,registeredRegionCount:1,
    missingProfileRegionCount:1,collectionVerifiedRegionCount:0,remainingRegionCount:2});
  assert.equal(report.regions[2].status,'INACTIVE_NOT_ACTIVATED');assert.equal(report.currentHttpRequests,0);
});
test('stale profile evidence and any unresolved sample block regional completion',()=>{
  const samples=[1,2,3].map(i=>sample({caseCode:`CASE-${i}`}));
  assert.equal(buildCollectionPlan(inventory([target({profiles:[{profileCode:'PROFILE_1',profileHash:other}]})]),samples)
    .regions[0].status,'EVIDENCE_VERSION_MISMATCH');
  samples.push(sample({caseCode:'CASE-4',discoveryComplete:false}));
  assert.equal(buildCollectionPlan(inventory(),samples).regions[0].status,'COLLECTION_INCOMPLETE');
});
test('duplicate cases targets or out-of-scope evidence fail instead of inflating coverage',()=>{
  for(const run of [()=>buildCollectionPlan(inventory(),[sample(),sample()]),()=>buildCollectionPlan(inventory([target(),target()])),
    ()=>buildCollectionPlan(inventory(),[sample({sourceCode:'LGS-000099'})]),
    ()=>buildCollectionPlan(inventory([target({enabled:'true'})]))]) assert.throws(run,/COLLECTION_EVIDENCE_INVALID/);
});
