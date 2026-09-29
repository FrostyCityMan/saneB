/** 부분 성공을 보존하는 수집 가능 현황. 기존 전체 집합 검증 대장과 별도이며 외부 요청이 없다. */
import {buildCollectionPlan} from './attachment-collection-stage.mjs';

export function buildCollectionAvailability(inventory, samples) {
  const strict = buildCollectionPlan(inventory, samples);
  const byCase = new Map(samples.map(s => [s.caseCode, s]));
  const regions = strict.regions.map(region => {
    const target = inventory.targets.find(t => t.localSourceCode === region.sourceCode);
    const current = region.samples.filter(s => target.bindingStatus === 'SYSTEM_BINDING_MATCHED'
      && target.profiles.length === 1 && s.profileCode === target.profiles[0].profileCode
      && s.profileHash === target.profiles[0].profileHash).map(s => byCase.get(s.caseCode));
    const eligible = current.filter(s => s.titleStatus === 'ELIGIBLE' && s.detailIdentityVerified && s.originalFilesRemoved);
    const downloaded = eligible.reduce((n,s) => n + s.files.filter(f => f.status === 'SUCCEEDED').length, 0);
    const errors = current.flatMap(s => {
      const issues=[];
      if (s.titleStatus !== 'ELIGIBLE') return issues;
      if (!s.discoveryComplete || !['FOUND','NO_FILES'].includes(s.discoveryStatus))
        issues.push({caseCode:s.caseCode, stage:'DISCOVERY', code:'DISCOVERY_INCOMPLETE', fileCount:0});
      for (const [status,code] of [['FAILED','FILE_COLLECTION_FAILED'],['UNSUPPORTED','UNSUPPORTED_FORMAT'],['NOT_RUN','FILE_NOT_RUN']]) {
        const count=s.files.filter(f => f.status === status).length;
        if (count) issues.push({caseCode:s.caseCode, stage:'FILE_COLLECTION', code, fileCount:count});
      }
      if (s.files.length !== s.expectedFileCount)
        issues.push({caseCode:s.caseCode,stage:'DISCOVERY',code:'FILE_COUNT_CHANGED',fileCount:0});
      if (!s.originalFilesRemoved) issues.push({caseCode:s.caseCode,stage:'CLEANUP',code:'CLEANUP_PENDING',fileCount:0});
      return issues;
    });
    return {sourceCode:region.sourceCode, enabled:region.enabled, listParserProfileCode:region.listParserProfileCode,
      bindingStatus:region.bindingStatus, hasObservedDownload:downloaded>0, downloadedFileObservations:downloaded,
      wholeSetVerified:region.collectionVerified, errors,
      nextAction:!region.enabled?'INACTIVE_NOT_ACTIVATED':region.bindingStatus!=='SYSTEM_BINDING_MATCHED'?'CONNECT_PROFILE'
        :downloaded===0?'FIRST_DOWNLOAD_SAMPLE':errors.length?'CONTINUE_COLLECTION_AND_TRACK_ERRORS':'CONTINUE_COLLECTION'};
  });
  const active=regions.filter(r=>r.enabled);
  return {schemaVersion:1, scope:'BEST_EFFORT_COLLECTION_AVAILABILITY', currentHttpRequests:0, productionWriteCount:0,
    inventoryObservedAt:strict.inventoryObservedAt, isOperatingE2eVerified:false, isExtractionVerified:false,
    summary:{activeRegionCount:active.length, observedDownloadRegionCount:active.filter(r=>r.hasObservedDownload).length,
      noObservedDownloadRegionCount:active.filter(r=>!r.hasObservedDownload).length,
      regionsWithErrors:active.filter(r=>r.errors.length).length, wholeSetVerifiedRegionCount:strict.summary.collectionVerifiedRegionCount,
      missingProfileRegionCount:strict.summary.missingProfileRegionCount}, regions};
}
