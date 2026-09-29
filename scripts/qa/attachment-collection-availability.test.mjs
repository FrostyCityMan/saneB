import test from 'node:test';
import assert from 'node:assert/strict';
import {buildCollectionAvailability} from './attachment-collection-availability.mjs';
const hash='a'.repeat(64), other='b'.repeat(64);
const target={providerCode:'LOCAL_GOV_NOTICE',localSourceCode:'LGS-000001',listParserProfileCode:'SPRING_BBS',
  enabled:true,bindingStatus:'SYSTEM_BINDING_MATCHED',profiles:[{profileCode:'PROFILE_1',profileHash:hash}]};
const inventory={scope:'OPERATING_TARGET_SNAPSHOT_VS_LOCAL_CODE_REGISTRY',targets:[target]};
const sample={sourceCode:target.localSourceCode,caseCode:'CASE-1',profileCode:'PROFILE_1',profileHash:hash,
  receiptHash:hash,expectedFileCount:2,discoveryStatus:'FAILED',discoveryComplete:false,titleStatus:'ELIGIBLE',
  detailIdentityVerified:true,originalFilesRemoved:true,productionWriteCount:0,
  files:[{locatorHash:hash,status:'SUCCEEDED',format:'PDF',binaryHash:hash,bytes:512,signatureVerified:true},
    {locatorHash:other,status:'FAILED',bytes:0}]};
test('partial collection is useful while discovery and file errors remain separate',()=>{
  const result=buildCollectionAvailability(inventory,[{...sample,rawText:'PRIVATE_CANARY'}]);
  assert.equal(result.summary.observedDownloadRegionCount,1);assert.equal(result.summary.wholeSetVerifiedRegionCount,0);
  assert.equal(result.regions[0].downloadedFileObservations,1);
  assert.deepEqual(result.regions[0].errors.map(e=>e.stage),['DISCOVERY','FILE_COLLECTION']);
  assert.equal(result.regions[0].nextAction,'CONTINUE_COLLECTION_AND_TRACK_ERRORS');
  assert.ok(!JSON.stringify(result).includes('PRIVATE_CANARY'));
});
test('untrusted stale incomplete cleanup and title excluded evidence cannot prove download availability',()=>{
  for(const change of [{profileHash:other},{originalFilesRemoved:false},{detailIdentityVerified:false},{titleStatus:'NOT_ELIGIBLE'}])
    assert.equal(buildCollectionAvailability(inventory,[{...sample,...change}]).summary.observedDownloadRegionCount,0);
});
test('missing and inactive regions are not silently counted as collected',()=>{
  const result=buildCollectionAvailability({...inventory,targets:[target,{...target,localSourceCode:'LGS-000002',
    profiles:[],bindingStatus:'PROFILE_MISSING'},{...target,localSourceCode:'LGS-000003',enabled:false}]},[sample]);
  assert.equal(result.summary.activeRegionCount,2);assert.equal(result.summary.noObservedDownloadRegionCount,1);
  assert.equal(result.summary.missingProfileRegionCount,1);assert.equal(result.regions[1].nextAction,'CONNECT_PROFILE');
  assert.equal(result.regions[2].nextAction,'INACTIVE_NOT_ACTIVATED');
});
