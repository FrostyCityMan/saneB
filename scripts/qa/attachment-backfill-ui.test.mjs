import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
const require=createRequire(import.meta.url),B=require('../../src/main/resources/static/js/saneb-attachment-batch-core.js');
const C=require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const F=require('../../src/main/resources/static/js/saneb-attachment-backfill-core.js').create(B);
const UI=require('../../src/main/resources/static/js/saneb-attachment-backfills.js');
const id=n=>`00000000-0000-0000-0000-${String(n).padStart(12,'0')}`,hash='a'.repeat(64),stamp='2026-09-11T10:00:00Z';
const scope=()=>F.scope({policyId:id(2),providerCodes:['BIZINFO'],collectedFrom:'2026-09-01T00:00:00+09:00',collectedBefore:'2026-09-10T00:00:00+09:00',segmentSize:1000});
const counts=n=>[{providerCode:'BIZINFO',reasonCode:'CANDIDATE',count:n},{providerCode:'BIZINFO',reasonCode:'LINKED_PROTECTED',count:3}];
const preview=()=>({scope:scope(),scopeHash:hash,candidateHash:hash,ruleReleaseId:id(3),policyHash:hash,counts:counts(1001),candidateCount:1001,segmentCount:2,previewHttpRequests:0,canInventory:true});
const run=()=>({runId:id(1),policyId:id(2),statusCode:'INVENTORIED',scopeHash:hash,candidateHash:hash,rowVersion:0,candidateCount:1001,remainingItemCount:1001,deletedItemCount:0,segmentSize:1000,segmentCount:2,
    frozenScope:{schemaVersion:1,filter:scope(),counts:counts(1001),inventoryOnly:true,previewHttpRequests:0},createdAt:stamp});
const segment=n=>({runId:id(1),segmentNo:n,itemCount:n===1?1000:1,remainingItemCount:n===1?1000:1,deletedItemCount:0});
const item=n=>({ordinal:n,sourceId:id(n+100),contentVersionId:id(n+2000),baseEvaluationId:id(n+4000),ruleReleaseId:id(3),providerCode:'BIZINFO',inputHash:hash,currentInputMatches:true});
function reservation(){const {segmentSize,...filter}=scope(),bp={scope:{...filter,maximumCount:segmentSize},scopeHash:hash,policyHash:hash,ruleReleaseId:id(3),
    candidateCount:1,selectedCount:1,remainingCount:0,maximumDownloadBytes:1000,maximumHttpRequests:3,currentHttpRequests:0,canReserve:true,
    items:[{sourceId:id(1101),sourceVersion:1,attachmentVersion:0,readinessCode:'READY',providerCode:'BIZINFO'}],counts:[{providerCode:'BIZINFO',reasonCode:'CANDIDATE',count:1}]};
    return {runId:id(1),segmentNo:2,runVersion:0,originalItemCount:1,remainingItemCount:1,deletedItemCount:0,segmentHash:hash,readinessCode:'READY',canReserve:true,batchId:null,batchPreview:bp};}
const summary=()=>({runId:id(1),candidateCount:1001,remainingItemCount:1001,deletedItemCount:0,segmentCount:2,reservedSegmentCount:0,reservedRemainingItemCount:0,unreservedItemCount:1001,unreservedInputChangedCount:0,
    collectionCounts:{UNRESERVED:1001},applicationCounts:{UNRESERVED:1001},rollbackCounts:{UNRESERVED:1001}});
const page=(items,p=1,size=20,total=items.length)=>({items,page:p,size,totalCount:total,totalPages:Math.ceil(total/size)});
const sent=()=>F.command('reserve',{run:run(),segmentNo:2,reservation:reservation()},'분할 확인',true);
const receipt=()=>({runId:id(1),segmentNo:2,batchId:id(4),inventoryVersionAtReservation:0,originalItemCount:1,reservedItemCount:1,deletedBeforeReservation:0,segmentHash:hash,currentBatchStatusCode:'SCOPE_READY',reservedAt:stamp});

test('whole preview retains 1001 candidates and two segments without a maximumCount field',()=>{
    const p=preview();assert.equal(F.validPreview(p,p.scope),true);const c=F.command('inventory',{preview:p},'전체 고정',true);
    assert.equal(c.payload.expectedCandidateCount,1001);assert.equal(c.payload.scope.segmentSize,1000);assert.equal('maximumCount' in c.payload.scope,false);
    assert.equal(F.validReceipt(run(),c),true);
    for(const patch of [{segmentCount:1},{candidateCount:1000},{previewHttpRequests:1},{counts:[...p.counts,p.counts[0]]},{candidateHash:'bad'},{canInventory:false}])assert.equal(F.validPreview({...p,...patch},p.scope),false);
});
test('immutable denominators, deleted inventory replay and unsafe numeric counts are distinguished',()=>{
    assert.equal(F.validRun(run()),true);const r={...run(),remainingItemCount:999,deletedItemCount:2,rowVersion:2};assert.equal(F.validRun(r),true);
    assert.equal(F.validReceipt(r,F.command('inventory',{preview:preview()},'고정',true)),true);
    for(const patch of [{candidateCount:1000},{segmentCount:1},{remainingItemCount:1000},{candidateCount:Number.MAX_SAFE_INTEGER+1},{statusCode:'COMPLETED'},{frozenScope:{...r.frozenScope,inventoryOnly:false}}])assert.equal(F.validRun({...r,...patch}),false);
    assert.equal(F.validSegment(segment(2),r),true);assert.equal(F.validSegment({...segment(2),itemCount:1000},r),false);
});
test('fixed reservation sends exact version, segment hash and deletion count but no source IDs',()=>{
    assert.equal(F.validReservationPreview(reservation(),run(),2),true);const c=sent();assert.match(c.path,/\/segments\/2\/reservation$/);
    assert.deepEqual(Object.keys(c.payload).sort(),['expectedDeletedItemCount','expectedRemainingItemCount','expectedRunVersion','expectedSegmentHash','reason'].sort());
    for(const patch of [{runId:id(8)},{segmentNo:1},{originalItemCount:2},{deletedItemCount:1},{runVersion:-1},{batchId:id(4)},{canReserve:false}])assert.equal(F.validReservationPreview({...reservation(),...patch},run(),2),false);
    const p=reservation();p.batchPreview={...p.batchPreview,scope:{...p.batchPreview.scope,collectedBefore:'2026-09-20T00:00:00Z'}};assert.equal(F.validReservationPreview(p,run(),2),false);
});
test('all nonready cases prohibit reservation and existing batches remain navigable',()=>{
    for(const code of ['ALREADY_RESERVED','ALL_ITEMS_DELETED','INPUT_CHANGED','POLICY_CHANGED','BATCH_NOT_READY']){
        const p={...reservation(),readinessCode:code,canReserve:false,batchPreview:null};
        if(code==='ALREADY_RESERVED')p.batchId=id(4);
        if(code==='ALL_ITEMS_DELETED'){p.remainingItemCount=0;p.deletedItemCount=1;}
        if(code==='BATCH_NOT_READY'){p.batchPreview={...reservation().batchPreview,canReserve:false,items:[{...reservation().batchPreview.items[0],readinessCode:'PROFILE_REQUIRED'}]};}
        assert.equal(F.validReservationPreview(p,run(),2),true,code);assert.throws(()=>F.command('reserve',{run:run(),segmentNo:2,reservation:p},'예약',true));
    }
    assert.equal(F.batchLink(id(4)),`/app/admin/announcement-attachment-batches?batchId=${id(4)}`);assert.throws(()=>F.batchLink('https://bad.test'));
});
test('summary validates each dimension without adding applied and rolled back together',()=>{
    const s={...summary(),reservedSegmentCount:1,reservedRemainingItemCount:1,unreservedItemCount:1000,
        collectionCounts:{UNRESERVED:1000,SUCCEEDED:1},applicationCounts:{UNRESERVED:1000,APPLIED:1},rollbackCounts:{UNRESERVED:1000,ROLLED_BACK:1}};
    assert.equal(F.validSummary(s,run()),true);assert.equal('completed' in s,false);
    for(const patch of [{reservedRemainingItemCount:2},{unreservedInputChangedCount:1001},{deletedItemCount:1},{collectionCounts:{SUCCEEDED:1001}},
        {rollbackCounts:{UNRESERVED:1000,ROLLED_BACK:2}},{applicationCounts:{UNRESERVED:1000,MISSING_JOB:1}},{reservedSegmentCount:3}])assert.equal(F.validSummary({...s,...patch},run()),false);
    assert.equal(F.validSummary({...summary(),remainingItemCount:0,deletedItemCount:1001,unreservedItemCount:0,collectionCounts:{},applicationCounts:{},rollbackCounts:{}},run()),true);
});
test('fixed membership handles later pages without accepting foreign ordinals or unsafe identifiers',()=>{
    assert.equal(F.validItem(item(1001),run(),2),true);assert.equal(F.validItem(item(1),run(),2),false);
    assert.equal(F.validItem({...item(1001),sourceId:'../private'},run(),2),false);assert.equal(F.validItem({...item(1001),currentInputMatches:'true'},run(),2),false);
});
test('consent, reason and receipt identity are required; replay may return cancelled current batch status',()=>{
    for(const [reason,ack]of [['',true],[' ',true],['가'.repeat(1001),true],['이유',false]])assert.throws(()=>F.command('reserve',{run:run(),segmentNo:2,reservation:reservation()},reason,ack));
    assert.equal(F.validReceipt({...receipt(),currentBatchStatusCode:'CANCELLED'},sent()),true);
    for(const patch of [{runId:id(7)},{segmentNo:1},{segmentHash:'b'.repeat(64)},{reservedItemCount:2},{deletedBeforeReservation:1},{inventoryVersionAtReservation:1},{reservedAt:null}])assert.equal(F.validReceipt({...receipt(),...patch},sent()),false);
});
test('lost response followed by 401/403/409 retains original reservation key/body',async()=>{
    const calls=[];let n=0;const m=B.mutations(async(url,o)=>{calls.push({url,...o});if(n++<4)throw new C.RequestError('미확정',[0,401,403,409][n-1]);return receipt();},C,()=>id(9),F.validReceipt);
    await assert.rejects(m.execute(sent()));for(let i=0;i<3;i++){await assert.rejects(m.execute());assert.equal(m.uncertain,true);}
    await m.execute();assert.equal(m.uncertain,false);assert.equal(new Set(calls.map(c=>c.body)).size,1);assert.equal(new Set(calls.map(c=>c.headers['Idempotency-Key'])).size,1);
    const wrong=B.mutations(async()=>({...receipt(),runId:id(9)}),C,()=>id(9),F.validReceipt);await assert.rejects(wrong.execute(sent()));assert.equal(wrong.uncertain,true);
});

// Real controller/event code with DOM/API ports. This is not a rendered browser or PostgreSQL test.
class Element {
    constructor(tag='div'){this.tag=tag;this.children=[];this.events={};this.dataset={};this.disabled=false;this.hidden=false;this.value='';this.checked=false;this.textContent='';}
    append(...c){this.children.push(...c);}replaceChildren(...c){this.children=[...c];}addEventListener(t,f){this.events[t]=f;}setAttribute(k,v){this[k]=v;}
    querySelectorAll(tag){return this.children.flatMap(n=>[...(n.tag===tag?[n]:[]),...n.querySelectorAll(tag)]);}
    async fire(t,e={}){await this.events[t]?.({preventDefault(){},target:this,...e});}focus(){this.focused=true;}
}
function harness({admin=true,nav={runId:id(1),segmentNo:2},request:custom}={}){
    const nodes=new Map(),calls=[],q=s=>{if(!nodes.has(s))nodes.set(s,new Element());return nodes.get(s);};
    const fields=new Map(['policyId','collectedFrom','collectedBefore','deadlineFrom','deadlineThrough','segmentSize'].map(n=>[n,new Element('input')]));
    fields.get('policyId').value=id(2);fields.get('collectedFrom').value='2026-09-01T00:00';fields.get('collectedBefore').value='2026-09-10T00:00';fields.get('segmentSize').value='1000';
    const sf=q('[data-scope-form]');sf.elements={namedItem:n=>fields.get(n)};sf.reportValidity=()=>true;sf.querySelectorAll=()=>[{value:'BIZINFO'}];
    const reason=new Element('textarea'),ack=new Element('input');reason.value='운영 검토';q('[data-approval-form]').elements={namedItem:n=>n==='reason'?reason:ack};q('[data-approval-form]').reportValidity=()=>true;
    const fallback=async url=>{
        if(url.includes('policies?'))return page([],1,20);if(url.includes('backfills?page'))return page([run()],1,10);
        if(url.endsWith('/summary'))return summary();if(url.endsWith('/reservation-preview'))return reservation();
        if(url.includes('/segments?'))return page([segment(1),segment(2)]);if(url.includes('/items?'))return page([item(1001)]);return run();};
    const request=async(url,o={})=>{calls.push({url,...o});return custom?custom(url,o,fallback):fallback(url);};
    const app=UI.mount({page:{dataset:{isAdmin:String(admin)},querySelector:q,setAttribute(){}},B,C,F,request,doc:{createElement:t=>new Element(t)},uuid:()=>id(9),navigation:{read:()=>nav}});
    return {app,q,calls,fields,reason,ack};
}
const text=n=>[n.textContent,...n.children.map(text)].join(' ');
test('deep linked whole/segment screen reads full summary and exposes exact batch reservation without writes',async()=>{
    const h=harness();await h.app.start();assert.equal(h.app.state.run.candidateCount,1001);assert.equal(h.app.state.segmentNo,2);
    assert.equal(h.q('[data-reserve]').disabled,false);assert.equal(h.calls.some(c=>c.method),false);assert.match(text(h.q('[data-summary]')),/1,001/);
    assert.match(h.q('[data-segments]').querySelectorAll('a')[1].href,/segmentNo=2/);
});
test('operator and approver behavior cannot arm or forge a reservation',async()=>{
    const h=harness({admin:false});await h.app.start();h.app.arm('reserve');h.ack.checked=true;await h.app.submit();
    assert.equal(h.q('[data-reserve]').disabled,true);assert.equal(h.calls.some(c=>c.method),false);
});
test('scope edit clears prepared inventory consent and preserves reason with unload warning',async()=>{
    const h=harness({request:(url,o,next)=>url.endsWith('/scope-preview')?preview():next(url)});await h.app.start();
    // Selecting a policy happens after the initial policy list has loaded.
    h.fields.get('policyId').value=id(2);await h.q('[data-scope-form]').fire('submit');assert.equal(h.app.state.preview.candidateCount,1001);
    h.app.arm('inventory');h.ack.checked=true;await h.q('[data-scope-form]').fire('input');
    assert.equal(h.app.state.preview,null);assert.equal(h.ack.checked,false);assert.equal(h.reason.value,'운영 검토');assert.equal(h.app.dirty,true);
});
test('inventory creation and exact fixed segment reservation link into existing batch without collection request',async()=>{
    let reserved=false;const h=harness({request:async(url,o,next)=>{
        if(url.endsWith('/scope-preview'))return preview();if(o.method==='POST' && url===F.base)return run();
        if(o.method==='POST' && url.endsWith('/reservation')){reserved=true;return receipt();}
        if(reserved && url.endsWith('/reservation-preview'))return {...reservation(),canReserve:false,readinessCode:'ALREADY_RESERVED',batchId:id(4),batchPreview:null};return next(url);
    }});await h.app.start();h.fields.get('policyId').value=id(2);await h.q('[data-scope-form]').fire('submit');h.app.arm('inventory');h.ack.checked=true;await h.app.submit();
    assert.match(text(h.q('[data-receipt]')),/최초 1,001건/);h.reason.value='분할 예약';await h.app.loadRun(id(1),2);h.app.arm('reserve');h.ack.checked=true;await h.app.submit();
    const posts=h.calls.filter(c=>c.method==='POST');assert.equal(posts.length,3);assert.equal(JSON.parse(posts[2].body).expectedRemainingItemCount,1);
    assert.equal(h.app.state.reservation.readinessCode,'ALREADY_RESERVED');assert.equal(h.q('[data-reserve]').disabled,true);
    assert.equal(h.q('[data-receipt]').querySelectorAll('a')[0].href,F.batchLink(id(4)));assert.equal(h.calls.some(c=>c.url.endsWith('/collection')),false);
});
test('reservation receipt survives a following list failure',async()=>{
    let saved=false;const h=harness({request:async(url,o,next)=>{if(o.method==='POST'){saved=true;return receipt();}if(saved && url.includes('backfills?page'))throw new C.RequestError('후속 조회 실패',503);return next(url);}});
    await h.app.start();h.app.arm('reserve');h.ack.checked=true;await h.app.submit();
    assert.equal(h.app.mutation.uncertain,false);assert.match(text(h.q('[data-receipt]')),/예약 배치/);assert.equal(h.q('[data-error]').hidden,false);
    assert.equal(h.q('[data-receipt]').querySelectorAll('a')[0].href,F.batchLink(id(4)));assert.equal(h.q('[data-reserve]').disabled,true);
});
test('lost reservation blocks navigation and retries identical request even after a session failure',async()=>{
    let n=0;const h=harness({request:async(url,o,next)=>{if(o.method==='POST')throw new C.RequestError('미확정',n++===0?0:401);return next(url);}});
    await h.app.start();h.app.arm('reserve');h.ack.checked=true;await h.app.submit();await h.q('[data-retry]').fire('click');
    const posts=h.calls.filter(c=>c.method==='POST');assert.equal(posts.length,2);assert.equal(posts[0].body,posts[1].body);assert.deepEqual(posts[0].headers,posts[1].headers);
    let prevented=false;await h.q('[data-segments]').querySelectorAll('a')[0].fire('click',{preventDefault(){prevented=true;}});assert.equal(prevented,true);
    assert.equal(h.app.mutation.uncertain,true);assert.equal(h.q('[data-refresh]').disabled,true);assert.equal(h.reason.value,'운영 검토');
});
test('initial list error is not hidden by successful policy or detail reads',async()=>{
    const h=harness({request:async(url,o,next)=>{if(url.includes('backfills?page'))throw new C.RequestError('목록 실패',503);return next(url);}});
    await h.app.start();assert.equal(h.q('[data-error]').hidden,false);assert.match(h.q('[data-error]').textContent,/목록 실패/);assert.equal(h.q('[data-reserve]').disabled,true);
    await h.q('[data-refresh]').fire('click');assert.equal(h.q('[data-reserve]').disabled,false);
});
test('mismatched summary blocks reservation while retained total is not labeled complete',async()=>{
    const h=harness({request:(url,o,next)=>url.endsWith('/summary')?{...summary(),remainingItemCount:1}:next(url)});await h.app.start();
    assert.equal(h.q('[data-error]').hidden,false);assert.equal(h.q('[data-reserve]').disabled,true);assert.equal(h.app.state.reservation,null);
});
test('transport only expands to same-origin backfill namespace; UI uses text and no persistent sensitive draft',async()=>{
    const api=B.client(async()=>({ok:true,status:200,json:async()=>({success:true,data:run()})}),C);assert.equal((await api(F.base)).runId,id(1));
    await assert.rejects(api(F.base+'/../secrets'));await assert.rejects(api('https://bad.test'+F.base));
    for(const file of ['saneb-attachment-backfill-core.js','saneb-attachment-backfills.js']){const js=readFileSync(new URL(`../../src/main/resources/static/js/${file}`,import.meta.url),'utf8');assert.doesNotMatch(js,/innerHTML|outerHTML|insertAdjacentHTML|localStorage|sessionStorage|\.eval\(/);}
    const html=readFileSync(new URL('../../src/main/resources/templates/app/announcement-attachment-backfills.html',import.meta.url),'utf8');assert.doesNotMatch(html,/th:utext|<script(?![^>]*src)/);assert.match(html,/name="acknowledged" type="checkbox" required/);assert.doesNotMatch(html,/acknowledged[^>]*checked/);
});
