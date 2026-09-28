/** 첨부 발견·다운로드 전용 대장. 추출·정상 기대값·정책 게시 Gate를 대체하지 않는다. */
const hash = value => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value);
const key = value => typeof value === 'string' && /^[A-Z0-9_-]{1,100}$/.test(value);
const formats = new Set(['PDF', 'HWP', 'HWPX']);
const invalid = () => { throw new Error('COLLECTION_EVIDENCE_INVALID'); };

export function assessSample(sample) {
  if (!sample || !key(sample.sourceCode) || !key(sample.caseCode) || !key(sample.profileCode)
      || !hash(sample.profileHash) || !hash(sample.receiptHash) || !Array.isArray(sample.files)
      || sample.files.length > 10 || !Number.isSafeInteger(sample.expectedFileCount)
      || sample.expectedFileCount < 0 || sample.expectedFileCount > 10
      || !['FOUND', 'NO_FILES', 'FAILED', 'LIMIT_EXCEEDED', 'NOT_RUN'].includes(sample.discoveryStatus)
      || !['ELIGIBLE', 'NOT_ELIGIBLE', 'NOT_CHECKED'].includes(sample.titleStatus)
      || typeof sample.detailIdentityVerified !== 'boolean' || typeof sample.discoveryComplete !== 'boolean'
      || typeof sample.originalFilesRemoved !== 'boolean' || sample.productionWriteCount !== 0) invalid();
  const locators = new Set();
  for (const file of sample.files) {
    if (!file || !hash(file.locatorHash) || locators.has(file.locatorHash)
        || !['SUCCEEDED', 'FAILED', 'NOT_RUN', 'UNSUPPORTED'].includes(file.status)
        || !Number.isSafeInteger(file.bytes) || file.bytes < 0 || file.bytes > 20 * 1024 * 1024) invalid();
    locators.add(file.locatorHash);
    if (file.status === 'SUCCEEDED' && (!formats.has(file.format) || !hash(file.binaryHash)
        || file.bytes === 0 || file.signatureVerified !== true)) invalid();
  }
  // 외부 문서 텍스트·URL·파일명·임의 오류 문자열은 결과에 복사하지 않는다.
  const base = {sourceCode: sample.sourceCode, caseCode: sample.caseCode, profileCode: sample.profileCode,
    profileHash: sample.profileHash, receiptHash: sample.receiptHash, fileCount: sample.files.length,
    downloadedFileCount: sample.files.filter(f => f.status === 'SUCCEEDED').length,
    status: 'INCOMPLETE', collectionVerified: false};
  if (!sample.originalFilesRemoved) return {...base, status: 'CLEANUP_PENDING'};
  if (sample.titleStatus !== 'ELIGIBLE') return {...base, status: 'TITLE_NOT_ELIGIBLE_OR_UNVERIFIED'};
  if (!sample.detailIdentityVerified || !sample.discoveryComplete
      || sample.files.length !== sample.expectedFileCount) return base;
  if (sample.discoveryStatus === 'NO_FILES' && sample.files.length === 0)
    return {...base, status: 'NO_FILES_VERIFIED', collectionVerified: true};
  if (sample.discoveryStatus === 'FOUND' && sample.files.length > 0
      && sample.files.every(f => f.status === 'SUCCEEDED'))
    return {...base, status: 'ALL_FILES_DOWNLOADED', collectionVerified: true};
  return base;
}

export function buildCollectionPlan(inventory, samples = []) {
  if (!inventory || inventory.scope !== 'OPERATING_TARGET_SNAPSHOT_VS_LOCAL_CODE_REGISTRY'
      || !Array.isArray(inventory.targets) || inventory.targets.length > 1000 || !Array.isArray(samples)
      || samples.length > 10000) invalid();
  const codes = new Set(), cases = new Set();
  const results = samples.map(assessSample);
  for (const item of results) {
    if (cases.has(item.caseCode)) invalid();
    cases.add(item.caseCode);
  }
  const regions = inventory.targets.filter(t => t.providerCode === 'LOCAL_GOV_NOTICE').map(target => {
    if (!/^LGS-[0-9]{6}$/.test(target.localSourceCode) || codes.has(target.localSourceCode)
        || !key(target.listParserProfileCode) || typeof target.enabled !== 'boolean'
        || !Array.isArray(target.profiles) || target.profiles.length > 100
        || !['SYSTEM_BINDING_MATCHED', 'PROFILE_MISSING', 'PROFILE_AMBIGUOUS', 'LIST_PARSER_MISMATCH'].includes(target.bindingStatus)) invalid();
    codes.add(target.localSourceCode);
    for (const profile of target.profiles) if (!key(profile.profileCode) || !hash(profile.profileHash)) invalid();
    const own = results.filter(s => s.sourceCode === target.localSourceCode);
    const matched = own.filter(s => target.bindingStatus === 'SYSTEM_BINDING_MATCHED' && target.profiles.length === 1
      && s.profileCode === target.profiles[0].profileCode && s.profileHash === target.profiles[0].profileHash);
    const passed = matched.filter(s => s.collectionVerified);
    const fileCases = passed.filter(s => s.status === 'ALL_FILES_DOWNLOADED');
    // 정상 추출3건과 다르다. 공식 표본3건·최소1건 실제 파일·미해결 관측0을 수집 Gate로만 사용한다.
    const verified = passed.length >= 3 && fileCases.length > 0 && matched.length === own.length
      && matched.every(s => s.collectionVerified);
    return {sourceCode: target.localSourceCode, listParserProfileCode: target.listParserProfileCode,
      enabled: target.enabled, bindingStatus: target.bindingStatus, collectionVerified: verified,
      status: !target.enabled ? 'INACTIVE_NOT_ACTIVATED' : verified ? 'COLLECTION_VERIFIED'
        : target.bindingStatus !== 'SYSTEM_BINDING_MATCHED' ? target.bindingStatus
        : own.length !== matched.length ? 'EVIDENCE_VERSION_MISMATCH'
        : matched.some(s => !s.collectionVerified) ? 'COLLECTION_INCOMPLETE' : 'SAMPLES_REQUIRED',
      verifiedSampleCount: passed.length, downloadedSampleCount: fileCases.length,
      observedSampleCount: own.length, minimumSampleCount: 3, samples: own};
  });
  if (results.some(s => !codes.has(s.sourceCode))) invalid();
  const active = regions.filter(r => r.enabled);
  return {schemaVersion: 1, scope: 'REGIONAL_ATTACHMENT_DISCOVERY_DOWNLOAD_ONLY',
    inventoryObservedAt: inventory.observedAt ?? null, currentHttpRequests: 0, productionWriteCount: 0,
    isExtractionVerified: false, isPolicyQaPassed: false, isOperatingE2eVerified: false,
    summary: {allRegionCount: regions.length, activeRegionCount: active.length,
      inactiveRegionCount: regions.length - active.length,
      registeredRegionCount: active.filter(r => r.bindingStatus === 'SYSTEM_BINDING_MATCHED').length,
      missingProfileRegionCount: active.filter(r => r.bindingStatus === 'PROFILE_MISSING').length,
      collectionVerifiedRegionCount: active.filter(r => r.collectionVerified).length,
      remainingRegionCount: active.filter(r => !r.collectionVerified).length}, regions};
}
