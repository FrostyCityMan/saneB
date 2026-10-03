import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
import {runInNewContext} from 'node:vm';
const require = createRequire(import.meta.url);
const C = require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const id = '11111111-1111-4111-8111-111111111111';
const path = `/api/v2/admin/announcement-sources/${id}`;

test('linked evidence notices distinguish success partial failure and changed connections', () => {
    const notice={noticeId:id,jobId:id,batchId:id,setId:id,evaluationId:id,reasonCode:'EVIDENCE_READY',
        jobStatusCode:'SUCCEEDED',errorCode:null,connectionsUnchanged:true,createdAt:'2026-10-03T01:00:00+09:00'};
    assert.equal(C.validLinkedNotice(notice),true);
    assert.equal(C.linkedNoticeLabel(notice.reasonCode),'새 첨부 근거 확인 필요');
    assert.equal(C.validLinkedNotice({...notice,reasonCode:'EVIDENCE_PARTIAL',jobStatusCode:'PARTIAL_FAILED',connectionsUnchanged:false}),true);
    assert.equal(C.validLinkedNotice({...notice,reasonCode:'COLLECTION_FAILED',jobStatusCode:'FAILED',setId:null,evaluationId:null}),true);
    for(const changed of [{jobStatusCode:'FAILED'},{setId:null},{evaluationId:null},{noticeId:'javascript:alert(1)'},
        {connectionsUnchanged:'true'},{createdAt:'invalid'},{reasonCode:'UNKNOWN'}])
        assert.equal(C.validLinkedNotice({...notice,...changed}),false,JSON.stringify(changed));
    assert.equal(C.validLinkedNotice(null),false);
});

test('linked notice UI reads stored evidence without promoting current classification', async () => {
    const script=await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js',import.meta.url),'utf8');
    const html=await readFile(new URL('../../src/main/resources/templates/app/announcement-attachment-review.html',import.meta.url),'utf8');
    assert.match(html,/data-load-linked-notices disabled/);
    assert.match(html,/data-linked-notices aria-live="polite"/);
    const section=script.slice(script.indexOf('const showLinkedNotices'),script.indexOf('const showSets'));
    assert.match(section,/attachment-linked-review-notices/);
    assert.match(section,/C\.validLinkedNotice/);
    assert.match(section,/notice\.connectionsUnchanged/);
    assert.match(section,/showFiles\(notice\.setId/);
    assert.match(section,/현재 판정 아님/);
    assert.doesNotMatch(section,/method:|\.innerHTML|insertConfirmation|insertOperationalAnnouncement/);
    assert.match(script,/clear\(q\("\[data-linked-notices\]"\)\)/);
});

test('HTTP attachment requests use cryptographic UUIDs without requiring randomUUID', () => {
    assert.equal(C.requestUuid({randomUUID:()=>id}),id);
    let calls=0;
    const cryptoProvider={getRandomValues(bytes){calls++;bytes.fill(255);return bytes;}};
    assert.equal(C.requestUuid(cryptoProvider),'ffffffff-ffff-4fff-bfff-ffffffffffff');
    assert.equal(calls,1);
    assert.throws(()=>C.requestUuid({}),/안전한 요청 식별자.*요청은 전송하지 않았습니다/);
    assert.throws(()=>C.requestUuid({getRandomValues(){throw new Error('unavailable');}}),/요청은 전송하지 않았습니다/);
});

test('HTTP request keys preserve uncertain retries and recover from unavailable randomness', () => {
    let available=false,calls=0;
    const m=C.mutation(()=>C.requestUuid({getRandomValues(bytes){if(!available)throw new Error();bytes.fill(++calls);return bytes;}}));
    const payload={reason:'공개 첨부 수집 확인'};
    assert.throws(()=>m.prepare(payload),/안전한 요청 식별자/);
    assert.equal(m.pending,false);assert.equal(m.original,null);
    available=true;
    const first=m.prepare(payload);m.fail({uncertain:true});
    assert.deepEqual(m.prepare(payload),first);assert.equal(calls,1);
    m.succeed();assert.notEqual(m.prepare(payload).key,first.key);
});

test('browser core defaults work when HTTP exposes getRandomValues but no randomUUID', async () => {
    const code=await readFile(new URL('../../src/main/resources/static/js/saneb-attachment-review-core.js',import.meta.url),'utf8');
    const browser={crypto:{getRandomValues(bytes){bytes.fill(7);return bytes;}}};
    runInNewContext(code,browser);
    const request=browser.SanebAttachmentReview.mutation().prepare({reason:'단건 수집'});
    assert.match(request.key,/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    assert.equal(JSON.parse(request.body).reason,'단건 수집');
});

test('attachment collect, review and recovery entrypoints share the HTTP-compatible key factory', async () => {
    for(const name of ['saneb-announcement-attachment-review.js','saneb-attachment-operations.js','saneb-attachment-recovery.js']) {
        const script=await readFile(new URL(`../../src/main/resources/static/js/${name}`,import.meta.url),'utf8');
        assert.doesNotMatch(script,/crypto\.randomUUID/);
        assert.match(script,/C\.mutation\(\)/);
    }
});

for (const [entry,coreName,coreGlobal] of [
    ['batches','review','SanebAttachmentReview'],
    ['backfills','review','SanebAttachmentReview'],
    ['provider-qa','policy','SanebAttachmentPolicy']
]) {
    test(`${entry} browser entrypoint wires HTTP-safe keys and refuses missing secure randomness`, async () => {
        let factory,randomCalls=0;
        const mounted=new Error('request factory captured');
        const page={dataset:{isAdmin:'true'},querySelector:()=>({})};
        const browser={document:{querySelector:()=>page},fetch:()=>{throw new Error('network must not run');},
            crypto:{getRandomValues(bytes){bytes.fill(++randomCalls);return bytes;}}};
        const read=name=>readFile(new URL(`../../src/main/resources/static/js/saneb-attachment-${name}.js`,import.meta.url),'utf8');
        runInNewContext(await read(`${coreName}-core`),browser);
        const capture=uuid=>{factory=uuid;throw mounted;};
        browser.SanebAttachmentBatch={client:()=>()=>{},mutations:(_request,_core,uuid)=>capture(uuid)};
        browser.SanebAttachmentBackfill={};
        browser.SanebAttachmentProviderQa={mutations:(_request,uuid)=>capture(uuid)};
        const script=await read(entry);
        assert.doesNotMatch(script,/crypto\.randomUUID/);
        assert.throws(()=>runInNewContext(script,browser),error=>error===mounted);
        assert.equal(typeof factory,'function');
        assert.equal(factory(),'01010101-0101-4101-8101-010101010101');
        assert.equal(randomCalls,1);
        browser.crypto={randomUUID:()=>id};assert.equal(factory(),id);
        browser.crypto={};assert.throws(factory,/안전한 요청 식별자.*요청은 전송하지 않았습니다/);
        browser.crypto={getRandomValues(){throw new Error('private crypto diagnostic');}};
        assert.throws(factory,error=>/안전한 요청 식별자/.test(error.message)&&!error.message.includes('private'));
        const html=await readFile(new URL(`../../src/main/resources/templates/app/announcement-attachment-${entry}.html`,import.meta.url),'utf8');
        const dependency=html.indexOf(`saneb-attachment-${coreName}-core.js`);
        assert.ok(dependency>=0 && dependency<html.indexOf(`saneb-attachment-${entry}.js`));
        assert.equal(typeof browser[coreGlobal].requestUuid,'function');
    });
}

test('batch HTTP keys stop before transport on crypto failure and retain identical retries', async () => {
    const B=require('../../src/main/resources/static/js/saneb-attachment-batch-core.js');
    let available=false,randomCalls=0;const sent=[];
    const mutation=B.mutations(async(path,options)=>{sent.push({path,...options});if(sent.length===1)throw new C.RequestError('응답 유실');return {};},C,
        ()=>C.requestUuid({getRandomValues(bytes){if(!available)throw new Error();bytes.fill(++randomCalls);return bytes;}}),()=>true);
    const command={kind:'reserve',keyed:true,path:B.base,method:'POST',payload:{reason:'고정 범위 확인'}};
    await assert.rejects(mutation.execute(command),/안전한 요청 식별자/);
    assert.equal(sent.length,0);assert.equal(mutation.pending,false);assert.equal(mutation.sent,null);
    available=true;await assert.rejects(mutation.execute(command),/응답 유실/);
    assert.equal(mutation.uncertain,true);await mutation.execute();
    assert.equal(sent.length,2);assert.deepEqual(sent[0],sent[1]);assert.equal(randomCalls,1);
    assert.equal(mutation.uncertain,false);assert.equal(mutation.sent,null);
});
test('role evidence binds exact extraction and preserves manual overrides without claiming approval', async () => {
    const a={ruleVersion:'document-role-1.0.1',rulesHash:'a'.repeat(64),textHash:'b'.repeat(64),blocksHash:'c'.repeat(64),roleCode:'NOTICE',reasonCode:'ROLE_TEXT_STRUCTURE_MATCHED',
        evidence:['NOTICE_HEADING','TARGET_SECTION','SUPPORT_SECTION','APPLICATION_SECTION'].map((ruleCode,i)=>({ruleCode,blockIndex:i,startOffset:i*10,endOffset:i*10+5}))};
    const file={roleOriginCode:'TEXT_RULE',documentRoleCode:'NOTICE',roleExtractionId:id,extractionId:id,qualityCode:'COMPLETE_TEXT',roleAssessment:a};
    assert.equal(C.validRoleAssessment(file),true);
    assert.equal(C.validRoleAssessment({...file,roleOriginCode:'MANUAL',documentRoleCode:'REFERENCE'}),true);
    for(const patch of [{extractionId:'different'},{roleAssessment:null},{roleOriginCode:'PROFILE'},{documentRoleCode:'FORM'},{qualityCode:'PARTIAL_TEXT'},
        {roleAssessment:{...a,reasonCode:'ROLE_STRUCTURE_INCOMPLETE'}},{roleAssessment:{...a,rulesHash:'invalid'}},
        {roleAssessment:{...a,evidence:[{ruleCode:'NOTICE_HEADING',blockIndex:-1,startOffset:0,endOffset:1}]}},
        {roleAssessment:{...a,evidence:a.evidence.map(e=>({...e,startOffset:'0'}))}}])assert.equal(C.validRoleAssessment({...file,...patch}),false);
    assert.equal(C.validRoleAssessment({...file,documentRoleCode:'UNKNOWN',roleAssessment:{...a,roleCode:'UNKNOWN',reasonCode:'INITIAL_HEADING_REQUIRED',evidence:[]}}),true);
    assert.match(C.roleOrigin('TEXT_RULE'),/텍스트 규칙/);assert.match(C.roleOrigin('MANUAL'),/관리자 지정/);
    for(const code of [a.reasonCode,...a.evidence.map(e=>e.ruleCode)])assert.equal(C.label(code).includes(code),false);
    const script=await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js',import.meta.url),'utf8');
    assert.match(script,/C\.validRoleAssessment\(file\)/);assert.match(script,/extractionId:file\.roleExtractionId/);
    assert.match(script,/관리자 지정값을 덮어쓰지 않습니다/);assert.match(script,/최종 검증 완료를 의미하지 않습니다/);
});
const version = {expectedBaseDecisionId: 'base', expectedAttachmentDecisionId: 'attachment', expectedSourceVersion: 4, expectedAttachmentVersion: 7, expectedSetHash: 'a'.repeat(64)};
const context = {sourceId: id, version};
const source = {sourceId: id, isAttachmentReviewRequired: true, sourceVersion: 4, attachmentVersion: 7,
    processingFlow: {statusCode: 'READY_FOR_FINAL_REVIEW', isAutomaticAnalysisComplete: true, isFinalReviewAvailable: true},
    baseClassification: {decisionId: 'base'}, effectiveClassification: {decisionId: 'attachment', setHash: version.expectedSetHash}, attachmentSummary: {isStale: false}};
const response = (status, data, success = status < 400) => ({status, ok: status < 400, json: async () => ({success, data, message: '구체적인 정책 오류'})});
test('automatic processing and configuration do not request intermediate human review', () => {
    for (const code of ['NOT_APPLIED', 'CLASSIFICATION_PENDING', 'CONFIGURATION_REQUIRED', 'AUTOMATIC_PROCESSING', 'EVIDENCE_STALE', 'FUTURE_CODE']) {
        const pending = {...source, processingFlow: {statusCode: code, isFinalReviewAvailable: true}};
        assert.equal(C.canRequestFinalReview(pending), false, code);
        assert.equal(C.matchesContext(pending, context), false, code);
        assert.match(C.flowGuidance(pending), /[가-힣]/);
    }
    assert.equal(C.canRequestFinalReview({...source, processingFlow: undefined}), false);
    assert.equal(C.canRequestFinalReview({...source, processingFlow: {...source.processingFlow, isFinalReviewAvailable: 'true'}}), false);
});
test('technical exception permits explicit manual review only when current server evidence is available', () => {
    const exception = {...source, processingFlow: {statusCode: 'TECHNICAL_EXCEPTION', isAutomaticAnalysisComplete: false, isFinalReviewAvailable: true}};
    assert.equal(C.matchesContext(exception, context), true);
    assert.match(C.flowGuidance(exception), /자동 분석 성공을 뜻하지/);
    assert.equal(C.matchesContext({...exception, processingFlow: {...exception.processingFlow, isFinalReviewAvailable: false}}, context), false);
    for (const status of ['SCOPE_READY', 'PENDING', 'RUNNING', 'RETRY_WAIT', 'PAUSED'])
        assert.equal(C.matchesContext({...exception, attachmentSummary: {jobStatusCode: status}}, context), false);
});
test('final review states have distinct Korean labels and never mean automatic publication', () => {
    const codes = ['READY_FOR_FINAL_REVIEW', 'FINAL_REVIEW_EXCEPTION', 'FINAL_REVIEW_CONFIRMED'];
    assert.equal(new Set(codes.map(C.label)).size, codes.length);
    for (const statusCode of codes) assert.equal(C.canRequestFinalReview({...source, processingFlow: {...source.processingFlow, statusCode}}), true);
    assert.match(C.flowGuidance(source), /자동 공개되지/);
    assert.match(C.flowGuidance({...source, processingFlow: {statusCode: 'FINAL_REVIEW_CONFIRMED'}}), /별개/);
});
test('detail page gates review-context reads and explains missing body without demanding immediate input', async () => {
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js', import.meta.url), 'utf8');
    assert.match(script, /if \(C\.canRequestFinalReview\(details\.source\)\)/);
    assert.match(script, /1·2차 제목·본문 판정 이력 · 중간 근거/);
    assert.match(script, /첨부 분석 결과와 자동 처리 상태를 확인하세요/);
    assert.equal(script.includes('수집된 본문이 없습니다. 원문을 직접 확인하세요.'), false);
});
test('discovery failures have distinct Korean reasons rather than no-files or raw-code fallback', () => {
    const codes = ['ATTACHMENT_DETAIL_UNAVAILABLE', 'ATTACHMENT_SELECTOR_CHANGED', 'ATTACHMENT_DOWNLOAD_FORM_CHANGED',
        'ATTACHMENT_LINK_UNRESOLVED', 'ATTACHMENT_FILE_LIMIT', 'ATTACHMENT_DETAIL_TITLE_UNAVAILABLE'];
    assert.equal(new Set(codes.map(C.label)).size, codes.length);
    for (const code of codes) {
        assert.match(C.label(code), /[가-힣]/);
        assert.notEqual(C.label(code), C.label('NO_FILES'));
        assert.equal(C.label(code).includes(code), false);
    }
    assert.match(C.label('ATTACHMENT_DOWNLOAD_FORM_CHANGED'), /재검증 필요/);
});
test('current write gate binds every source/base/evaluation/set/version field', () => {
    assert.equal(C.matchesContext(source, context), true);
    for (const key of Object.keys(version)) assert.equal(C.matchesContext(source, {...context, version: {...version, [key]: null}}), false, key);
    assert.equal(C.matchesContext({...source, sourceId: 'other'}, context), false);
    assert.equal(C.matchesContext({...source, attachmentSummary: {isStale: true}}, context), false);
    assert.equal(C.matchesContext({...source, isAttachmentReviewRequired: false, previewClassification: source.effectiveClassification}, context), false);
    assert.equal(C.matchesContext({...source, effectiveClassification: null}, context), false);
});
test('current confirmation requires exact version and saved manual categories', () => {
    const saved = {confirmation: {sourceId: id, evaluationId: 'attachment', sourceVersion: 4, attachmentVersion: 7, setHash: version.expectedSetHash, isCurrent: true}, targetCategoryCodes: ['BUSINESS'], supportTypeCodes: ['POLICY_FINANCE']};
    assert.equal(C.confirmedCurrent({...context, confirmedClassification: saved}), true);
    for (const [key, value] of Object.entries({sourceId: 'other', evaluationId: 'old', sourceVersion: 3, attachmentVersion: 6, setHash: 'b'.repeat(64), isCurrent: false}))
        assert.equal(C.confirmedCurrent({...context, confirmedClassification: {...saved, confirmation: {...saved.confirmation, [key]: value}}}), false, key);
    assert.equal(C.confirmedCurrent({...context, confirmedClassification: {...saved, targetCategoryCodes: []}}), false);
});
test('same version comparison and labels do not treat unknown data as success', () => {
    assert.equal(C.sameVersion(version, {...version}), true); assert.equal(C.sameVersion(version, null), false);
    assert.equal(C.label(null), '미확인'); assert.match(C.label('FUTURE_CODE'), /확인 필요/);
    assert.match(C.label('NO_FILES'), /없음 확인/); assert.notEqual(C.label('NO_FILES'), C.label('DISCOVERY_FAILED'));
    assert.match(C.label('CURRENT_PREVIEW'), /적용 안 됨/);
});
test('restored confirmation keeps its historical version and requires exact effective binding', () => {
    const saved = {confirmation: {confirmationId: id,sourceId: id,evaluationId: 'attachment',sourceVersion: 4,attachmentVersion: 5,setHash: version.expectedSetHash,isCurrent: true},
        targetCategoryCodes: ['BUSINESS'],supportTypeCodes: ['POLICY_FINANCE'],
        binding: {restorationId: id,confirmationId: id,sourceId: id,sourceVersion: 4,attachmentVersion: 7}};
    assert.equal(C.confirmedCurrent({...context,confirmedClassification: saved}),true);
    assert.equal(saved.confirmation.attachmentVersion,5);
    for (const [key,value] of Object.entries({restorationId: null,confirmationId: 'foreign',sourceId: 'foreign',sourceVersion: 3,attachmentVersion: 8}))
        assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,binding: {...saved.binding,[key]: value}}}),false,key);
    for (const binding of [null,{},false,{...saved.binding,restorationId: 'unverified'}])
        assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,binding}}),false);
    assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,confirmation: {...saved.confirmation,isCurrent: false}}}),false);
});
test('explicit initial binding cannot fabricate a restored version or fall back on malformed data', () => {
    const confirmation = {confirmationId: id,sourceId: id,evaluationId: 'attachment',sourceVersion: 4,attachmentVersion: 7,setHash: version.expectedSetHash,isCurrent: true};
    const saved = {confirmation,targetCategoryCodes: ['BUSINESS'],supportTypeCodes: ['POLICY_FINANCE'],
        binding: {restorationId: null,confirmationId: id,sourceId: id,sourceVersion: 4,attachmentVersion: 7}};
    assert.equal(C.confirmedCurrent({...context,confirmedClassification: saved}),true);
    for (const binding of [null,{}, {...saved.binding,restorationId: id}, {...saved.binding,attachmentVersion: 6}])
        assert.equal(C.confirmedCurrent({...context,confirmedClassification: {...saved,binding}}),false);
});
test('external source links reject script, credentials, relative and non-http locations', () => {
    for (const url of ['javascript:alert(1)', 'data:text/html,a', '//evil.test', '/relative', 'https://name:password@example.test/', 'file:///a']) assert.equal(C.safeSourceUrl(url), null);
    assert.equal(C.safeSourceUrl('https://example.test/a'), 'https://example.test/a');
});
test('unicode evidence highlight uses code points without executing HTML', () => {
    const block = {blockIndex: 0, textStartOffset: 10, textEndOffset: 19, text: '😀지원금<b>x'};
    // Actual 8 code points; inconsistent server offsets must not invent a highlight.
    assert.equal(C.blockParts(block, {blockIndex: 0, startOffset: 11, endOffset: 14}).length, 1);
    block.textEndOffset = 18;
    const parts = C.blockParts(block, {blockIndex: 0, startOffset: 11, endOffset: 14});
    assert.equal(parts[1].text, '지원금'); assert.equal(parts.map(p => p.text).join(''), block.text);
    assert.equal(C.blockParts(block, {blockIndex: 1, startOffset: 11, endOffset: 14}).length, 1);
    assert.equal(C.blockParts(block, {blockIndex: 0, startOffset: 9, endOffset: 14}).length, 1);
});
test('no-store same-origin JSON reads and writes preserve idempotency and body', async () => {
    let seen;
    const call = C.client(async (url, options) => { seen = {url, ...options}; return response(200, {ok: true}); });
    await call(path + '/attachment-classification/confirmations', {method: 'POST', body: '{}', headers: {'Idempotency-Key': id}});
    assert.equal(seen.cache, 'no-store'); assert.equal(seen.credentials, 'same-origin'); assert.equal(seen.redirect, 'error');
    assert.equal(seen.headers['Idempotency-Key'], id); assert.equal(seen.headers['Content-Type'], 'application/json'); assert.equal(seen.body, '{}');
    await assert.rejects(() => call('https://example.test'), error => error.status === 400);
});
test('HTTP policies distinguish session, permission, conflict and unknown mutation results', async () => {
    for (const [status, pattern, uncertain] of [[401, /로그인/, false], [403, /권한/, false], [404, /정책 오류/, false], [409, /정책 오류/, false], [503, /결과를 확인/, true]]) {
        await assert.rejects(() => C.client(async () => response(status, {code: 'CONFLICT'}))(path), error => {
            assert.equal(error.status, status); assert.equal(error.uncertain, uncertain); assert.match(error.message, pattern); return true;
        });
    }
});
test('timeout, invalid JSON and malformed success are unknown rather than success', async () => {
    await assert.rejects(() => C.client(async () => { throw new Error('private upstream diagnostic'); })(path), error => error.uncertain && !error.message.includes('private'));
    for (const res of [response(200, null), {ok: true, status: 200, json: async () => { throw new Error('HTML'); }}])
        await assert.rejects(() => C.client(async () => res)(path), error => error.uncertain);
    await assert.rejects(() => C.client((url, options) => new Promise((resolve, reject) => options.signal.addEventListener('abort', () => reject(new Error('abort')))), 10)(path), error => error.uncertain);
});
test('duplicate submit is locked and network retry uses exactly the same key and payload', () => {
    let serial = 0; const m = C.mutation(() => `key-${++serial}`), payload = {version, reviewNote: '공개 원문 확인'};
    const first = m.prepare(payload); assert.equal(m.pending, true);
    assert.throws(() => m.prepare(payload), /이미 처리 중/);
    m.fail(new C.RequestError('timeout')); assert.equal(m.uncertain, true);
    assert.throws(() => m.prepare({...payload, reviewNote: '다른 요청'}), /미확정/);
    const repeated = m.prepare(m.original); assert.deepEqual(repeated, first);
    m.succeed(); assert.equal(m.original, null); assert.equal(m.uncertain, false);
    assert.notEqual(m.prepare(payload).key, first.key);
});
test('definite version conflict releases old request only for deliberate review', () => {
    let serial = 0; const m = C.mutation(() => ++serial);
    const first = m.prepare({version}); m.fail(new C.RequestError('stale', 409));
    assert.equal(m.original, null); assert.equal(m.uncertain, false);
    assert.notEqual(m.prepare({version: {...version, expectedAttachmentVersion: 8}}).key, first.key);
});
test('UI source uses text nodes, native controls, scoped APIs and does not persist sensitive inputs', async () => {
    const script = await readFile(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js', import.meta.url), 'utf8');
    for (const forbidden of ['innerHTML', 'insertAdjacentHTML', 'localStorage', 'sessionStorage', 'console.log', 'eval(', '/publication', '/attachment-jobs']) assert.equal(script.includes(forbidden), false, forbidden);
    for (const expected of ['textContent', 'createTextNode', 'requiredAcknowledgementCodes', 'manualSourceCheckRequired', 'beforeunload', 'epoch !== expectedEpoch', 'Idempotency-Key', 'confirmedCurrent', 'reviewDirty', 'same-origin']) {
        if (expected === 'same-origin') continue; assert.ok(script.includes(expected), expected);
    }
    const css = await readFile(new URL('../../src/main/resources/static/css/saneb-announcement-attachment-review.css', import.meta.url), 'utf8');
    assert.match(css, /\[hidden\]\s*\{\s*display:\s*none\s*!important/);
});
