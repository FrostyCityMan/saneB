/** 로컬 보관 영수증의 수집 근거만 이관한다. 네트워크/DB/추출기를 실행하지 않는다. */
import {createHash} from 'node:crypto';
import {assessSample} from './attachment-collection-stage.mjs';
const sha = value => createHash('sha256').update(value).digest('hex');
const hash = value => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value);
const fail = () => { throw new Error('COLLECTION_RECEIPT_INVALID'); };
const scopes = new Set(['OFFICIAL_THREE_STAGE_OBSERVATION_V1', 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1']);

export function importReceipt(bytes, inventory) {
  let outer;
  try { outer = JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/, '')); } catch { fail(); }
  const terminals = outer.reports?.filter(r => r.kind === 'TEMPORARY_QA_RESULT');
  if (!Array.isArray(terminals) || terminals.length !== 1) fail();
  const terminal = terminals[0], run = terminal.report, probe = run?.probe;
  if (!probe) return {receiptHash: sha(bytes), samples: [], status: 'NO_PROBE_EVIDENCE'};
  if (!['BBS_OBSERVATION_PROBE', 'OFFICIAL_WORKER_PROBE'].includes(probe.kind)
      || probe.productionDatabaseUsed !== false || !hash(probe.executionCodeHash)
      || run.productionDatabaseUsed !== false || run.executionCodeHash !== probe.executionCodeHash) fail();
  const reports = probe.cases ?? probe.reports;
  if (!Array.isArray(reports)) return {receiptHash: sha(bytes), samples: [], status: 'UNSUPPORTED_REPORT_SCHEMA'};
  if (reports.length > 100) fail();
  if (reports.some(r => !scopes.has(r.scope)))
    return {receiptHash: sha(bytes), samples: [], status: 'UNSUPPORTED_REPORT_SCHEMA'};
  const samples = reports.map(report => {
    const worker = report.scope === 'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1';
    if (worker !== (probe.kind === 'OFFICIAL_WORKER_PROBE')) fail();
    const targets = inventory.targets.filter(t => t.providerCode === 'LOCAL_GOV_NOTICE'
      && t.profiles.some(p => p.profileCode === report.profileCode));
    if (targets.length !== 1 || report.productionWriteCount !== 0 || !Array.isArray(report.files)
        || typeof report.observedAt !== 'string' || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,9})?Z$/.test(report.observedAt)
        || !Number.isFinite(Date.parse(report.observedAt))) fail();
    const eligible = ['COMBINATION_MATCHED', 'GROUP_A_MATCHED'].includes(report.titleStage);
    const stopped = ['TITLE_NOT_ELIGIBLE_NOT_FETCHED', 'TITLE_EXCLUDED_NOT_FETCHED'].includes(report.status);
    if (stopped && (report.files.length || report.discoveryComplete === true)) fail();
    // 기존 보고서는 상세 제목 확인 다음에만 discoveryStatus/count를 기록한다.
    // worker는 실제 worker+임시 DB/API 검증이 끝난 경우만 봉인된 전체 집합으로 인정한다.
    const workerCompleted = worker && report.status === 'WORKER_DB_API_OBSERVED_NOT_APPROVED'
      && report.workerStatus === 'EVALUATED' && ['SUCCEEDED', 'PARTIAL_FAILED'].includes(report.jobStatus)
      && report.discoveredFileCount === report.processedFileCount;
    const complete = report.discoveryComplete === true && (!worker || workerCompleted);
    const discovery = worker ? complete ? (report.discoveredFileCount > 0 ? 'FOUND' : 'NO_FILES') : 'NOT_RUN'
      : report.discoveryStatus ?? 'NOT_RUN';
    const files = report.files.map(file => {
      // OBSERVED의 format은 FileTypeValidator 통과 후에만 기록된다. 추출 실패는 별개다.
      const signature = ['PDF', 'HWP', 'HWPX'].includes(file.format) && hash(file.binaryHash)
        && Number.isSafeInteger(file.bytes) && file.bytes > 0
        && (worker ? file.downloadStatus === 'SUCCEEDED' : file.downloadAllowed === true
          && (file.status === 'OBSERVED' || file.status === 'DOWNLOADED'
            || file.status === 'FAILED' && ['ISOLATED_EXTRACTION', 'TEXT_ROLE'].includes(file.failedStage)));
      const unsupported = file.status === 'UNSUPPORTED_NOT_DOWNLOADED'
        || file.downloadStatus === 'BLOCKED' && file.downloadErrorCode === 'UNSUPPORTED_FORMAT';
      return {locatorHash: file.locatorHash, status: signature ? 'SUCCEEDED' : unsupported ? 'UNSUPPORTED'
        : file.status === 'NOT_RUN' ? 'NOT_RUN' : 'FAILED', bytes: file.bytes ?? 0,
        ...(signature ? {format: file.format, binaryHash: file.binaryHash, signatureVerified: true} : {})};
    });
    const sample = {sourceCode: targets[0].localSourceCode, caseCode: report.caseCode,
      profileCode: report.profileCode, profileHash: report.profileHash, receiptHash: sha(bytes),
      observedAt: report.observedAt, executionCodeHash: probe.executionCodeHash,
      evidenceScope: report.scope, expectedFileCount: worker ? report.discoveredFileCount ?? 0 : report.expectedListedFileCount,
      titleStatus: eligible ? 'ELIGIBLE' : stopped ? 'NOT_ELIGIBLE' : 'NOT_CHECKED',
      detailIdentityVerified: worker ? workerCompleted : ['FOUND', 'NO_FILES'].includes(discovery),
      discoveryStatus: discovery, discoveryComplete: complete,
      originalFilesRemoved: report.originalFilesRemoved === true && run.probeCleanupSucceeded === true
        && run.transportTemporaryFilesRemoved === true && terminal.unitInactive === true,
      productionWriteCount: 0, files};
    assessSample(sample);
    return sample;
  });
  return {receiptHash: sha(bytes), samples, status: 'IMPORTED'};
}

export function selectLatestSamples(receipts) {
  const time = value => value.slice(0,19) + (value.split('.')[1]?.slice(0,-1) ?? '').padEnd(9,'0');
  const selected = new Map();
  for (const receipt of receipts) for (const sample of receipt.samples) {
    const old = selected.get(sample.caseCode);
    if (old && old.sourceCode !== sample.sourceCode) fail();
    // 성공 여부와 무관하게 최신 관측을 선택한다. 동일 시각의 상충 근거는 중단한다.
    if (old && time(old.observedAt) === time(sample.observedAt) && JSON.stringify({...old, receiptHash: ''})
        !== JSON.stringify({...sample, receiptHash: ''})) fail();
    if (!old || time(sample.observedAt) > time(old.observedAt)) selected.set(sample.caseCode, sample);
  }
  return [...selected.values()].sort((a,b) => a.caseCode.localeCompare(b.caseCode));
}

/** 별도 수집 전용 Gradle 보고서. 원격 SSM/worker 검증으로 위장하지 않는다. */
export function importLocalCollectionReport(bytes, inventory, producerClassHash) {
  let report;
  try { report=JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/,'')); } catch { fail(); }
  if (!hash(producerClassHash) || report.scope !== 'OFFICIAL_THREE_STAGE_OBSERVATION_V1'
      || report.collectionOnly !== true || report.isExtractionVerified !== false
      || report.isWholeTextAnalysisComplete !== false || report.isPolicyQaPassed !== false
      || report.isExpectationApproved !== false || report.productionWriteCount !== 0
      || !Array.isArray(report.files) || typeof report.observedAt !== 'string'
      || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,9})?Z$/.test(report.observedAt)
      || !Number.isFinite(Date.parse(report.observedAt))) fail();
  const targets=inventory.targets.filter(t=>t.providerCode==='LOCAL_GOV_NOTICE'
    && t.profiles.some(p=>p.profileCode===report.profileCode));
  if (targets.length!==1) fail();
  const files=report.files.map(f=>{
    if (f.quality!==undefined || f.extractorVersion!==undefined || f.textHash!==undefined) fail();
    const success=f.status==='DOWNLOADED' && f.downloadAllowed===true;
    return {locatorHash:f.locatorHash, status:success?'SUCCEEDED':f.status==='UNSUPPORTED_NOT_DOWNLOADED'?'UNSUPPORTED'
      :f.status==='NOT_RUN'?'NOT_RUN':'FAILED',bytes:f.bytes??0,
      ...(success?{format:f.format,binaryHash:f.binaryHash,signatureVerified:true}:{})};
  });
  const stopped=['TITLE_NOT_ELIGIBLE_NOT_FETCHED','TITLE_EXCLUDED_NOT_FETCHED'].includes(report.status);
  const eligible=['COMBINATION_MATCHED','GROUP_A_MATCHED'].includes(report.titleStage);
  if(stopped&&(!['COMBINATION_NOT_MATCHED','GROUP_B_MATCHED'].includes(report.titleStage)
      ||eligible||report.files.length!==0||report.discoveryComplete===true
      || report.discoveryStatus!==undefined||report.requestReservationsIncludingBodyUpperBound!==0))fail();
  const sample={sourceCode:targets[0].localSourceCode,caseCode:report.caseCode,profileCode:report.profileCode,
    profileHash:report.profileHash,receiptHash:sha(bytes),producerClassHash,observedAt:report.observedAt,
    evidenceScope:'LOCAL_COLLECTION_ONLY_REPORT',expectedFileCount:report.expectedListedFileCount,
    titleStatus:eligible?'ELIGIBLE':stopped?'NOT_ELIGIBLE':'NOT_CHECKED',
    detailIdentityVerified:['FOUND','NO_FILES'].includes(report.discoveryStatus),discoveryStatus:report.discoveryStatus??'NOT_RUN',
    discoveryComplete:report.discoveryComplete===true,originalFilesRemoved:report.originalFilesRemoved===true,
    productionWriteCount:0,files};
  const assessed=assessSample(sample);
  if (assessed.collectionVerified && (report.status!=='COLLECTION_ONLY_OBSERVED_NOT_APPROVED' || report.collectionStageComplete!==true)) fail();
  return {receiptHash:sha(bytes),samples:[sample],status:'IMPORTED_LOCAL_COLLECTION_ONLY'};
}
