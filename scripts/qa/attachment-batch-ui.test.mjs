import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
const require=createRequire(import.meta.url);
const B=require('../../src/main/resources/static/js/saneb-attachment-batch-core.js');
const C=require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const UI=require('../../src/main/resources/static/js/saneb-attachment-batches.js');
const id=n=>`00000000-0000-4000-8000-${String(n).padStart(12,'0')}`,hash='a'.repeat(64),now='2026-09-11T10:00:00Z';
const scope=B.scope({policyId:id(2),providerCodes:['BIZINFO'],collectedFrom:'2026-09-01T00:00:00+09:00',collectedBefore:'2026-09-10T00:00:00+09:00',maximumCount:100});
const batch=(statusCode='PREVIEW_READY',n=2)=>({batchId:id(1),policyId:id(2),statusCode,scopeHash:hash,rowVersion:4,itemCount:n,remainingItemCount:n,deletedItemCount:0,
    jobCounts:{SUCCEEDED:n},frozenScope:{schemaVersion:1,filter:scope,remainingCount:99,maximumDownloadBytes:10000,maximumHttpRequests:264},createdAt:now});
const preview=b=>({previewId:id(3),batchId:b.batchId,statusCode:'PREVIEW_READY',scopeHash:hash,inputHash:hash,previewHash:hash,snapshotBatchVersion:b.rowVersion,currentBatchVersion:b.rowVersion,
    itemCount:b.itemCount,snapshotRemainingItemCount:b.itemCount,snapshotDeletedItemCount:0,availableItemCount:b.remainingItemCount,currentDeletedItemCount:b.deletedItemCount,
    eligibleItemCount:b.itemCount,selectedItemCount:1,currentPreview:true,inputsCurrent:true,currentHttpRequests:0,createdAt:now});
const item=(n,selected=false)=>({jobId:id(10+n),sourceId:id(100+n),providerCode:'BIZINFO',readinessCode:'READY',eligible:true,selected,evidence:{proposedStatus:'REVIEW_REQUIRED',baseTargetCodes:['BUSINESS'],proposedTargetCodes:['BUSINESS']}});
const page=(items,p=1,size=20,total=items.length)=>({items,page:p,size,totalCount:total,totalPages:Math.ceil(total/size)});
function state(b=batch()){return {batch:b,preview:preview(b),previewItems:[item(1,true),item(2)],selected:new Set([id(11)]),selectionComplete:true,selectionDirty:false};}
const receipt=(sent)=>({actionId:id(9),batchId:id(1),previewId:id(3),actionCode:{apply:'START','apply-pause':'PAUSE','apply-resume':'RESUME'}[sent.kind],acceptedFromVersion:sent.payload.expectedVersion,
    currentStatusCode:'APPLYING',currentVersion:sent.payload.expectedVersion+1,scopeItemCount:2,approvedSelectedCount:1,remainingItemCount:2,deletedItemCount:0,selectedRemainingCount:1,
    pendingCount:1,appliedCount:0,conflictCount:0,failedCount:0,currentHttpRequests:0,acceptedAt:now});
const rollback=b=>({batchId:b.batchId,version:b.rowVersion,statusCode:b.statusCode,previewHash:hash,scopeCount:b.itemCount,remainingCount:b.remainingItemCount,deletedCount:b.deletedItemCount,
    targetCount:1,eligibleCount:1,conflictCount:0,baseReopenCount:1,confirmationRestoreCount:0,staleConfirmationCount:0,cancelPendingCount:1,currentHttpRequests:0});
test('scope uses server provider codes, explicit intervals and bounded maximum',()=>{
    assert.equal(scope.collectedFrom,'2026-08-31T15:00:00.000Z');assert.deepEqual(scope.providerCodes,['BIZINFO']);
    assert.equal(B.scope({...scope,providerCodes:['GOV24']}).providerCodes[0],'GOV24');
    assert.equal(B.label('GOV24_PUBLIC_SERVICE'),'정부24');assert.equal(B.label('GOV24'),'정부24');
    for(const patch of [{providerCodes:[]},{providerCodes:['GOV24_PUBLIC_SERVICE']},{policyId:'bad'},{maximumCount:1001},{maximumCount:0},{maximumCount:1.5},{collectedBefore:scope.collectedFrom},{deadlineFrom:'2026-09-12',deadlineThrough:'2026-09-11'}])assert.throws(()=>B.scope({...scope,...patch}));
});
test('scope never calls a selected subset the whole candidate scope',()=>{
    const s={scope,scopeHash:hash,ruleReleaseId:id(4),policyHash:hash,counts:[{providerCode:'BIZINFO',reasonCode:'CANDIDATE',count:102}],candidateCount:102,selectedCount:100,remainingCount:2,
        maximumDownloadBytes:10000,maximumHttpRequests:13200,currentHttpRequests:0,canReserve:true,items:Array.from({length:100},(_,n)=>({sourceId:id(n+100),sourceVersion:1,attachmentVersion:1,readinessCode:'READY'}))};
    assert.equal(B.validScope(s,scope),true);
    for(const patch of [{remainingCount:0},{selectedCount:102},{currentHttpRequests:1},{candidateCount:100},{items:s.items.slice(1)},{items:s.items.map((i,n)=>n?i:{...i,readinessCode:'PROFILE_REQUIRED'})}])assert.equal(B.validScope({...s,...patch},scope),false);
    const cmd=B.command('reserve',{scope:s},'범위 예약',true);assert.equal(cmd.payload.expectedScopeHash,hash);assert.equal(cmd.method,'POST');assert.equal('sourceIds' in cmd.payload,false);
});
test('batch and pagination retain original count and deleted denominator',()=>{
    assert.equal(B.validBatch(batch()),true);assert.equal(B.validBatch({...batch(),remainingItemCount:1,deletedItemCount:1,jobCounts:{FAILED:1}}),true);
    for(const patch of [{deletedItemCount:1},{jobCounts:{SUCCEEDED:1}},{itemCount:1001},{scopeHash:'bad'},{frozenScope:{}}])assert.equal(B.validBatch({...batch(),...patch}),false);
    assert.equal(B.validPage(page([batch()],1,10),1,10,B.validBatch),true);assert.equal(B.validPage(page([batch()],1,10,2),1,10,B.validBatch),false);
});
test('collection approvals bind original caps and refuse deleted initial scope',()=>{
    for(const [kind,s]of [['collection','SCOPE_READY'],['collection-resume','COLLECTION_PAUSED']]){
        const cmd=B.command(kind,state(batch(s)),'수집 실행',true);assert.equal(cmd.keyed,false);assert.equal(cmd.method,'PUT');assert.equal(cmd.payload.expectedMaximumDownloadBytes,10000);assert.equal(cmd.payload.expectedMaximumHttpRequests,264);assert.equal(cmd.payload.expectedDeletedItemCount,0);
        assert.equal(B.validReceipt({...batch('COLLECTING'),rowVersion:5},cmd),kind==='collection-resume');
    }
    const b={...batch('SCOPE_READY'),remainingItemCount:1,deletedItemCount:1,jobCounts:{SCOPE_READY:1}};assert.throws(()=>B.command('collection',state(b),'수집',true));
    assert.doesNotThrow(()=>B.command('scope-cancellation',state(b),'예약 취소',true));
    for(const kind of ['collection-pause','scope-cancellation','preview','apply-resume'])assert.throws(()=>B.command(kind,state(batch('ROLLED_BACK')),'금지',true));
});
test('selection preserves the whole snapshot rather than just visible page',()=>{
    const s=state(batch('PREVIEW_READY',102));s.previewItems=Array.from({length:102},(_,n)=>item(n,n===101));s.selected=new Set([id(10),id(111)]);s.preview.selectedItemCount=1;
    const cmd=B.command('selection',s,'선택 변경',true);assert.deepEqual(cmd.payload.selectedJobIds,[id(10),id(111)]);assert.equal(cmd.method,'PUT');assert.equal(cmd.keyed,true);
    for(const change of [{selectionComplete:false},{previewItems:s.previewItems.slice(1)},{selected:new Set([id(999)])},{preview:{...s.preview,inputsCurrent:false}}])assert.throws(()=>B.command('selection',{...s,...change},'선택',true));
});
test('application binds saved preview and review reset; pause permits expected version advance after start',()=>{
    const s=state();const cmd=B.command('apply',s,'선택 적용',true);assert.equal(cmd.payload.acknowledgeReviewReset,true);assert.equal(cmd.payload.expectedSelectedCount,1);
    for(const patch of [{selectionDirty:true},{preview:{...s.preview,inputsCurrent:false}},{preview:{...s.preview,currentPreview:false}}])assert.throws(()=>B.command('apply',{...s,...patch},'적용',true));
    const paused=state(batch('APPLYING'));paused.preview.inputsCurrent=false;paused.preview.snapshotBatchVersion=2;
    assert.equal(B.command('apply-pause',paused,'중지',true).path.endsWith('/application/pause'),true);
    for(const [reason,ack]of [['',true],[' ',true],['가'.repeat(1001),true],['사유',false],['사유','true']])assert.throws(()=>B.command('apply',s,reason,ack));
});
test('rollback approval uses all targets and exact restore/cancel effects without source IDs',()=>{
    const b=batch('APPLY_PAUSED'),s={...state(b),rollback:rollback(b)};const cmd=B.command('rollback',s,'원복',true);
    assert.equal(cmd.payload.expectedTargetCount,1);assert.equal(cmd.payload.expectedCancelPendingCount,1);assert.equal(cmd.payload.expectedBaseReopenCount,1);assert.equal(cmd.payload.acknowledgeBindingRestoration,true);assert.equal('jobIds' in cmd.payload,false);
    for(const patch of [{version:3},{eligibleCount:0},{targetCount:3},{currentHttpRequests:1},{previewHash:'bad'}])assert.throws(()=>B.command('rollback',{...s,rollback:{...s.rollback,...patch}},'복구',true));
});
test('receipts validate bindings, versions, actual counts and never invent completion',()=>{
    const cmd=B.command('apply',state(),'적용',true),r=receipt(cmd);assert.equal(B.validReceipt(r,cmd),true);
    for(const patch of [{actionId:'bad'},{batchId:id(88)},{previewId:id(88)},{actionCode:'PAUSE'},{approvedSelectedCount:2},{scopeItemCount:3},{currentVersion:4},{appliedCount:9},{currentHttpRequests:1},{acceptedAt:null}])assert.equal(B.validReceipt({...r,...patch},cmd),false);
    assert.equal(r.pendingCount,1);assert.equal(r.appliedCount,0);
});
test('idempotent retry remains uncertain across session, permission and conflict responses',async()=>{
    const calls=[],cmd=B.command('apply',state(),'적용',true);let n=0;
    const m=B.mutations(async(url,options)=>{calls.push({url,...options});if(n++<4)throw new C.RequestError('미확정',[0,401,403,409][n-1]);return receipt(cmd);},C,()=>id(99));
    await assert.rejects(m.execute(cmd));assert.equal(m.uncertain,true);await assert.rejects(m.execute(cmd));assert.equal(calls.length,1);
    for(let i=0;i<3;i++){await assert.rejects(m.execute());assert.equal(m.uncertain,true);}
    await m.execute();assert.equal(m.uncertain,false);assert.equal(new Set(calls.map(c=>c.body)).size,1);assert.equal(new Set(calls.map(c=>c.headers['Idempotency-Key'])).size,1);
});
test('mismatched success is uncertain; definite initial rejection is not',async()=>{
    const cmd=B.command('apply',state(),'적용',true),m=B.mutations(async()=>({...receipt(cmd),previewId:id(99)}),C,()=>id(9));await assert.rejects(m.execute(cmd));assert.equal(m.uncertain,true);
    const denied=B.mutations(async()=>{throw new C.RequestError('충돌',409);},C,()=>id(9));await assert.rejects(denied.execute(cmd));assert.equal(denied.uncertain,false);
});
test('CAS response loss can only clear after a same-target current read and explicit acknowledgement',async()=>{
    const cmd=B.command('collection',state(batch('SCOPE_READY')),'수집',true),m=B.mutations(async()=>{throw new C.RequestError('연결 유실');},C,()=>{throw new Error('CAS must not generate key');});
    await assert.rejects(m.execute(cmd));assert.equal(m.uncertain,true);
    assert.throws(()=>m.resolveCas(batch('SCOPE_READY'),false));assert.throws(()=>m.resolveCas({...batch(),batchId:id(99)},true));
    m.resolveCas({...batch('COLLECTION_PENDING'),rowVersion:5},true);assert.equal(m.uncertain,false);
});
test('preview fetch checks every page, exact count, duplicate ID and final preview binding',async()=>{
    const b=batch('PREVIEW_READY',102),p=preview(b),items=Array.from({length:102},(_,n)=>item(n,n===101)),urls=[];
    const fetch=async url=>{urls.push(url);return url.includes('/items?')?(url.includes('page=1&')?page(items.slice(0,100),1,100,102):page(items.slice(100),2,100,102)):p;};
    assert.equal((await B.loadPreviewItems(fetch,b,p)).length,102);assert.equal(urls.length,3);
    await assert.rejects(B.loadPreviewItems(async url=>url.includes('/items?')?fetch(url):({...p,previewHash:'b'.repeat(64)}),b,p));
    await assert.rejects(B.loadPreviewItems(async url=>url.includes('page=2&')?page([items[0],items[1]],2,100,102):fetch(url),b,p));
});
test('transport rejects external paths, preserves CSRF-compatible same-origin requests and hides raw server failures',async()=>{
    let options;const api=B.client(async(url,o)=>{options=o;return {ok:true,status:200,json:async()=>({success:true,data:batch()})};},C);
    await api(B.base,{method:'POST',body:'{}'});assert.equal(options.credentials,'same-origin');assert.equal(options.cache,'no-store');assert.equal(options.redirect,'error');
    for(const url of ['https://invalid.test',B.base+'/../secrets','/api/v1/users',B.base+'?url=https://invalid.test'])await assert.rejects(api(url));
    const failed=B.client(async()=>({ok:false,status:500,json:async()=>({message:'private server trace'})}),C);await assert.rejects(failed(B.base),e=>e.uncertain && !e.message.includes('private'));
});

// Minimal DOM ports run the real controller and event handlers, not rendered-browser QA.
class Element {
    constructor(tag='div'){this.tag=tag;this.children=[];this.events={};this.dataset={};this.disabled=false;this.hidden=false;this.value='';this.checked=false;this.textContent='';}
    append(...children){this.children.push(...children);}replaceChildren(...c){this.children=[...c];}addEventListener(t,f){this.events[t]=f;}
    querySelectorAll(tag){return this.children.flatMap(n=>[...(n.tag===tag?[n]:[]),...n.querySelectorAll(tag)]);}
    async fire(t,e={}){await this.events[t]?.({preventDefault(){},target:this,...e});}focus(){this.focused=true;}
}
function harness({admin=true,request:custom,nav={},batchValue=null,historyRequest=null}={}){
    const nodes=new Map(),calls=[],writes=[],q=s=>{if(!nodes.has(s))nodes.set(s,new Element());return nodes.get(s);};
    const fields=new Map();for(const n of ['policyId','collectedFrom','collectedBefore','deadlineFrom','deadlineThrough','maximumCount'])fields.set(n,new Element('input'));
    fields.get('policyId').value=id(2);fields.get('collectedFrom').value='2026-09-01T00:00';fields.get('collectedBefore').value='2026-09-10T00:00';fields.get('maximumCount').value='100';
    const sf=q('[data-scope-form]');sf.elements={namedItem:n=>fields.get(n)};sf.reportValidity=()=>true;sf.querySelectorAll=()=>[{value:'BIZINFO'}];
    const reason=new Element('textarea'),ack=new Element('input');reason.value='관리자 검토';q('[data-approval-form]').elements={namedItem:n=>n==='reason'?reason:ack};q('[data-approval-form]').reportValidity=()=>true;
    const b=batchValue||batch(),p=preview(b),items=[item(1,true),item(2)];
    const request=async(url,o={})=>{calls.push({url,...o});if(historyRequest && (url.includes('/action-history') || url.includes('/actions/')))return historyRequest(url,o);if(custom)return custom(url,o);
        if(url.includes('policies?'))return page([],1,20);if(url.includes('/classification-preview/'))return page(items,1,100);
        if(url.endsWith('/classification-preview'))return p;if(url.includes('/items?'))return page(items);if(url.includes('batches?page'))return page([b],1,10);return b;};
    const app=UI.mount({page:{dataset:{isAdmin:String(admin)},querySelector:q},B,C,request,doc:{createElement:t=>new Element(t)},uuid:()=>id(99),navigation:{read:()=>nav,write:p=>writes.push(p)}});
    return {app,q,calls,writes,reason,ack,fields};
}
test('workspace deep link loads exact batch and all preview items without writes',async()=>{
    const h=harness({nav:{batchId:id(1)}});await h.app.start();assert.equal(h.app.state.batch.batchId,id(1));assert.equal(h.app.state.selectionComplete,true);assert.equal(h.calls.some(c=>c.method),false);
    assert.equal(h.q('[data-save-selection]').disabled,true);assert.equal(h.q('[data-refresh]').disabled,false);assert.equal(h.q('[data-scope-submit]').disabled,false);
});
test('read-only roles cannot arm or forge mutation submit',async()=>{
    const h=harness({admin:false,nav:{batchId:id(1)}});await h.app.start();h.app.arm('apply');h.ack.checked=true;await h.app.submit();assert.equal(h.calls.some(c=>c.method),false);assert.equal(h.q('[data-approve]').disabled,true);
});

test('linked batch returns to its verified inventory segment without inventing a link for ordinary batches',async()=>{
    const b=batch('SCOPE_READY');b.frozenScope.backfillRunId=id(77);b.frozenScope.backfillSegmentNo=2;
    const h=harness({nav:{batchId:b.batchId},request:async url=>url.includes('policies?')?page([],1,20):url.includes('batches?page')?page([b],1,10):url.includes('/items?')?page([item(1),item(2)]):b});
    await h.app.start();assert.equal(h.q('[data-detail]').querySelectorAll('a')[0].href,`/app/admin/announcement-attachment-backfills?runId=${id(77)}&segmentNo=2`);
    b.frozenScope.backfillRunId='javascript:invalid';await h.app.loadBatch(b.batchId);assert.equal(h.q('[data-detail]').querySelectorAll('a').length,0);
});
test('selection changes reset consent, retain other selections, and block apply until saved',async()=>{
    const h=harness({nav:{batchId:id(1)}});await h.app.start();h.app.arm('apply');h.ack.checked=true;
    const checks=h.q('[data-preview-items]').querySelectorAll('input');checks[1].checked=true;await checks[1].fire('change');
    assert.equal(h.app.state.selected.size,2);assert.equal(h.ack.checked,false);assert.equal(h.app.state.selectionDirty,true);assert.equal(h.q('[data-save-selection]').disabled,false);
    h.app.arm('apply');assert.equal(h.q('[data-approval]').hidden,true);assert.equal(h.calls.some(c=>c.method),false);
});
test('workspace lost write locks other controls and same-key retry survives 401',async()=>{
    let n=0;const b=batch(),p=preview(b),h=harness({nav:{batchId:id(1)},request:async(url,o)=>{
        if(o.method){n++;throw new C.RequestError('결과 미확정',n===1?0:401);}if(url.includes('policies?'))return page([],1,20);if(url.includes('batches?page'))return page([b],1,10);
        if(url.includes('/classification-preview/'))return page([item(1,true),item(2)],1,100);if(url.endsWith('/classification-preview'))return p;if(url.includes('/items?'))return page([item(1,true),item(2)]);return b;
    }});await h.app.start();h.app.arm('apply');h.ack.checked=true;await h.app.submit();assert.equal(h.app.mutation.uncertain,true);assert.equal(h.q('[data-refresh]').disabled,true);
    await h.q('[data-retry]').fire('click');const posts=h.calls.filter(c=>c.method);assert.equal(posts.length,2);assert.equal(posts[0].body,posts[1].body);assert.deepEqual(posts[0].headers,posts[1].headers);assert.equal(h.reason.value,'관리자 검토');assert.equal(h.app.mutation.uncertain,true);
});
test('initial list failure remains error, can retry, and does not silently render empty success',async()=>{
    let fail=true;const h=harness({request:async url=>{if(fail)throw new C.RequestError('조회 실패',503);return page([],1,url.includes('policies')?20:10);}});
    await h.app.start();assert.equal(h.q('[data-error]').hidden,false);assert.equal(h.q('[data-list-refresh]').disabled,false);fail=false;await h.q('[data-list-refresh]').fire('click');assert.equal(h.q('[data-error]').hidden,true);
});
test('DOM text is not inserted as HTML; forms and public navigation have no secret storage',()=>{
    for(const f of ['saneb-attachment-batch-core.js','saneb-attachment-batches.js']){const js=readFileSync(new URL(`../../src/main/resources/static/js/${f}`,import.meta.url),'utf8');assert.doesNotMatch(js,/innerHTML|outerHTML|insertAdjacentHTML|localStorage|sessionStorage|\.eval\(/);}
    const html=readFileSync(new URL('../../src/main/resources/templates/app/announcement-attachment-batches.html',import.meta.url),'utf8');assert.doesNotMatch(html,/th:utext|<script(?![^>]*src)/);assert.match(html,/name="acknowledged" type="checkbox" required/);assert.doesNotMatch(html,/acknowledged[^>]*checked/);
});

test('workspace walks scope, reservation, collection controls, selection, application and rollback using existing endpoints',async()=>{
    let b=batch('SCOPE_READY'),p=null,existing=false,selection=[false,false],appReceipt=null,rollbackReceipt=null;
    const scopePreview={scope,scopeHash:hash,ruleReleaseId:id(4),policyHash:hash,counts:[{providerCode:'BIZINFO',reasonCode:'CANDIDATE',count:2}],candidateCount:2,selectedCount:2,remainingCount:0,
        maximumDownloadBytes:10000,maximumHttpRequests:264,currentHttpRequests:0,canReserve:true,items:[1,2].map(n=>({sourceId:id(100+n),sourceVersion:1,attachmentVersion:1,readinessCode:'READY'}))};
    const previewNow=()=>({...p,currentBatchVersion:b.rowVersion,inputsCurrent:p.snapshotBatchVersion===b.rowVersion});
    const h=harness({request:async(url,o)=>{
        const body=o.body?JSON.parse(o.body):null;
        if(url.includes('policies?'))return page([{policyId:id(2),policyCode:'QA',versionNo:1,policyStatusCode:'ACTIVE',modeCode:'ENFORCE',ruleReleaseId:id(4),ruleReleaseStatusCode:'ACTIVE'}],1,20);
        if(url.includes('batches?page'))return page(existing?[b]:[],1,10);
        if(url.endsWith('/scope-preview'))return scopePreview;
        if(o.method && url===B.base){existing=true;return b;}
        if(o.method && ['/collection','/collection-pause','/collection-resume'].some(v=>url.endsWith(v))){const status=url.endsWith('/collection')?'COLLECTION_PENDING':url.endsWith('/collection-pause')?'COLLECTION_PAUSED':'COLLECTING';b={...b,statusCode:status,rowVersion:b.rowVersion+1};return b;}
        if(o.method && url.includes('/classification-preview')){selection=url.endsWith('/selection')?[1,2].map(n=>body.selectedJobIds.includes(id(10+n))):[false,false];b={...b,statusCode:'PREVIEW_READY',rowVersion:b.rowVersion+2};p={...preview(b),previewId:id(300+b.rowVersion),selectedItemCount:selection.filter(Boolean).length};return p;}
        if(o.method && url.includes('/application')){const kind=url.endsWith('/pause')?'apply-pause':url.endsWith('/resume')?'apply-resume':'apply';const s={kind,payload:body};b={...b,statusCode:kind==='apply-pause'?'APPLY_PAUSED':'APPLYING',rowVersion:b.rowVersion+1};appReceipt={...receipt(s),previewId:p.previewId,currentStatusCode:b.statusCode,currentVersion:b.rowVersion};return appReceipt;}
        if(o.method && url.endsWith('/rollback')){b={...b,statusCode:'ROLLING_BACK',rowVersion:b.rowVersion+1};rollbackReceipt={actionId:id(900),batchId:b.batchId,acceptedFromVersion:body.expectedVersion,statusCode:b.statusCode,currentVersion:b.rowVersion,scopeCount:2,approvedTargetCount:1,approvedEligibleCount:1,approvedBaseReopenCount:1,approvedConfirmationRestoreCount:0,cancelledPendingCount:0,remainingTargetCount:1,deletedCount:0,pendingCount:1,rolledBackCount:0,conflictCount:0,failedCount:0,currentHttpRequests:0,acceptedAt:now};return rollbackReceipt;}
        if(url.includes('/application/actions/'))return {...appReceipt,currentStatusCode:b.statusCode,currentVersion:b.rowVersion};
        if(url.includes('/rollback/actions/'))return {...rollbackReceipt,statusCode:b.statusCode,currentVersion:b.rowVersion};
        if(url.endsWith('/rollback/preview')){assert.ok(['APPLIED','APPLY_PAUSED','APPLY_PARTIAL_FAILED'].includes(b.statusCode),'no new rollback preview after acceptance');return {...rollback(b),cancelPendingCount:0};}
        if(url.includes('/classification-preview/') && url.includes('/items?'))return page([item(1,selection[0]),item(2,selection[1])],1,100);
        if(url.endsWith('/classification-preview'))return previewNow();
        if(url.includes('/items?'))return page([item(1,selection[0]),item(2,selection[1])]);
        return b;
    }});
    await h.app.start();await h.q('[data-scope-form]').fire('submit');assert.equal(h.app.state.scope.canReserve,true);
    async function execute(kind){h.app.arm(kind);h.ack.checked=true;await h.app.submit();assert.equal(h.q('[data-error]').hidden,true,`${kind}: ${h.q('[data-error]').textContent}`);assert.equal(h.app.mutation.uncertain,false);}
    await execute('reserve');assert.equal(h.app.state.batch.statusCode,'SCOPE_READY');
    await execute('collection');await execute('collection-pause');await execute('collection-resume');
    // Simulated background collector finishes; neither UI nor receipt claims this transition itself.
    b={...b,statusCode:'COLLECTED',rowVersion:b.rowVersion+1};await h.app.loadBatch(id(1));await execute('preview');
    const check=h.q('[data-preview-items]').querySelectorAll('input')[0];check.checked=true;await check.fire('change');await execute('selection');assert.equal(h.app.state.preview.selectedItemCount,1);
    await execute('apply');assert.equal(h.app.state.batch.statusCode,'APPLYING');await execute('apply-pause');await execute('apply-resume');
    b={...b,statusCode:'APPLY_PARTIAL_FAILED',rowVersion:b.rowVersion+1};await h.app.loadBatch(id(1));await execute('rollback');assert.equal(h.app.state.batch.statusCode,'ROLLING_BACK');
    assert.equal(h.writes.at(-1).listPage,1);assert.ok(h.writes.some(w=>w.actionId===id(900)));
    const changes=h.calls.filter(c=>c.method && !c.url.endsWith('/scope-preview'));assert.equal(changes.length,10);
    assert.ok(changes.every(c=>!c.body.includes('force') && !c.body.includes('parser')));
});

test('accepted reservation keeps the new batch ID when the following list refresh fails',async()=>{
    let created=false,listReads=0;const b=batch('SCOPE_READY');
    const s={scope,scopeHash:hash,ruleReleaseId:id(4),policyHash:hash,counts:[{providerCode:'BIZINFO',reasonCode:'CANDIDATE',count:2}],candidateCount:2,selectedCount:2,remainingCount:0,maximumDownloadBytes:10000,maximumHttpRequests:264,currentHttpRequests:0,canReserve:true,items:[1,2].map(n=>({sourceId:id(n),sourceVersion:1,attachmentVersion:1,readinessCode:'READY'}))};
    const h=harness({request:async(url,o)=>{if(o.method){created=true;return b;}if(url.includes('policies?'))return page([],1,20);if(url.includes('batches?page')){if(++listReads>1)throw new C.RequestError('목록 조회 실패',503);return page([],1,10);}if(url.includes('/items?'))return page([item(1),item(2)]);return b;}});
    await h.app.start();h.app.state.scope=s;h.app.arm('reserve');h.ack.checked=true;await h.app.submit();assert.equal(created,true);assert.equal(h.app.mutation.uncertain,false);assert.equal(h.app.state.batch.batchId,id(1));assert.ok(h.writes.some(w=>w.batchId===id(1)));assert.equal(h.q('[data-error]').hidden,false);assert.equal(h.q('[data-refresh]').disabled,false);
});

const historyBatch=()=>({...batch('APPLYING'),rowVersion:20});
const historyEntry=(version=19)=>({actionId:id(700+version),batchId:id(1),actionKind:'APPLICATION',actionCode:version===9?'START':version%2?'RESUME':'PAUSE',acceptedFromVersion:version,
    previewId:id(3),scopeItemCount:2,approvedTargetCount:1,deletedCountAtAcceptance:0,approvedEligibleCount:null,approvedBaseReopenCount:null,approvedConfirmationRestoreCount:null,
    cancelledPendingCount:null,acceptedAt:'2026-09-12T00:00:00Z'});
const historyPage=(p=1,throughVersion=20,currentBatchVersion=20)=>({batchId:id(1),throughVersion,currentBatchVersion,history:page(Array.from({length:11},(_,i)=>historyEntry(19-i)).slice((p-1)*10,p*10),p,10,11),currentHttpRequests:0});
const historicalReceipt=e=>({actionId:e.actionId,batchId:e.batchId,previewId:e.previewId,actionCode:e.actionCode,acceptedFromVersion:e.acceptedFromVersion,currentStatusCode:'APPLIED',currentVersion:21,
    scopeItemCount:2,approvedSelectedCount:1,remainingItemCount:1,deletedItemCount:1,selectedRemainingCount:1,pendingCount:0,appliedCount:1,conflictCount:0,failedCount:0,currentHttpRequests:0,acceptedAt:e.acceptedAt});
test('approval history validates a stable upper bound and immutable counts independently from live receipt totals',()=>{
    const b=historyBatch(),h=historyPage();assert.equal(B.validHistory(h,b,1,10),true);
    for(const patch of [{batchId:id(4)},{throughVersion:18},{currentBatchVersion:19},{currentHttpRequests:1}])assert.equal(B.validHistory({...h,...patch},b,1,10,20),false);
    assert.equal(B.validHistory({...h,history:{...h.history,totalCount:10}},b,1,10),false);
    const e=historyEntry();assert.equal(B.validHistoricalReceipt(historicalReceipt(e),e),true);
    for(const patch of [{approvedSelectedCount:2},{actionCode:'PAUSE'},{previewId:id(99)},{acceptedAt:'2026-09-13T00:00:00Z'}])assert.equal(B.validHistoricalReceipt({...historicalReceipt(e),...patch},e),false);
    assert.equal(e.deletedCountAtAcceptance,0);assert.equal(historicalReceipt(e).deletedItemCount,1);
});
test('approval history rejects duplicate keys, ascending versions and unexpected rollback impact fields',()=>{
    const b=historyBatch(),h=historyPage();const invalid={...h,history:{...h.history,items:[h.history.items[1],h.history.items[0],...h.history.items.slice(2)]}};
    assert.equal(B.validHistory(invalid,b,1,10),false);invalid.history.items=[h.history.items[0],h.history.items[0],...h.history.items.slice(2)];assert.equal(B.validHistory(invalid,b,1,10),false);
    for(const patch of [{actionKind:'ADMIN_OVERRIDE'},{approvedEligibleCount:1},{previewId:null},{acceptedFromVersion:20},{scopeItemCount:1}])assert.equal(B.validHistoryEntry({...historyEntry(),...patch},b,20),false);
    const r={...historyEntry(),actionKind:'ROLLBACK',actionCode:'START',previewId:null,approvedEligibleCount:1,approvedBaseReopenCount:1,approvedConfirmationRestoreCount:0,cancelledPendingCount:1};
    assert.equal(B.validHistoryEntry(r,b,20),true);assert.equal(B.validHistoryEntry({...r,approvedConfirmationRestoreCount:1},b,20),false);
});
test('read-only actor can page old approvals and open exact receipt without losing input or calling mutations',async()=>{
    const b=historyBatch(),h=harness({admin:false,batchValue:b,nav:{batchId:b.batchId},historyRequest:async url=>{
        if(url.includes('/action-history')){const p=Number(new URL(url,'https://local.invalid').searchParams.get('page'));return historyPage(p,20,22);}
        const e=Array.from({length:11},(_,i)=>historyEntry(19-i)).find(e=>url.endsWith(e.actionId));return historicalReceipt(e);
    }});await h.app.start();const originalSelection=[...h.app.state.selected];await h.q('[data-history-refresh]').fire('click');
    assert.equal(h.q('[data-history]').querySelectorAll('button').length,10);await h.q('[data-history-pages]').querySelectorAll('button')[0].fire('click');
    assert.equal(h.q('[data-history]').querySelectorAll('button').length,1);assert.ok(h.calls.some(c=>c.url.includes('page=2&size=10&throughVersion=20')));
    await h.q('[data-history]').querySelectorAll('button')[0].fire('click');
    assert.ok(h.calls.some(c=>c.url===`${B.base}/${b.batchId}/application/actions/${historyEntry(9).actionId}`));
    assert.equal(h.q('#batch-receipt-title').focused,true);assert.equal(h.reason.value,'관리자 검토');assert.deepEqual([...h.app.state.selected],originalSelection);
    assert.equal(h.calls.some(c=>c.method),false);assert.ok(h.writes.some(w=>w.historyVersion===20 && w.historyPage===2));
});
test('history URL restores its original bound while explicit refresh starts a new bound',async()=>{
    const b=historyBatch(),h=harness({batchValue:b,nav:{batchId:b.batchId,historyVersion:20,historyPage:2},historyRequest:async url=>{
        const p=new URL(url,'https://local.invalid').searchParams;return historyPage(Number(p.get('page')),p.has('throughVersion')?20:22,22);
    }});await h.app.start();assert.ok(h.calls.some(c=>c.url.includes('page=2&size=10&throughVersion=20')));
    await h.q('[data-history-refresh]').fire('click');assert.ok(h.writes.some(w=>w.historyVersion===22 && w.historyPage===1));
});

test('switching batches clears old approval history and receipt even when the destination read fails',async()=>{
    for(const fail of [false,true]){
        const b=historyBatch(),other={...batch('SCOPE_READY'),batchId:id(88)};
        const h=harness({nav:{batchId:b.batchId},historyRequest:async url=>url.includes('/action-history')?historyPage():historicalReceipt(historyEntry()),request:async url=>{
            if(url.includes('policies?'))return page([],1,20);if(url.includes('batches?page'))return page([b,other],1,10);
            if(url.includes('/classification-preview/'))return page([item(1,true),item(2)],1,100);if(url.endsWith('/classification-preview'))return preview(b);
            if(url.includes('/items?'))return page([item(1,true),item(2)]);
            if(url.endsWith(other.batchId)){if(fail)throw new C.RequestError('다른 배치 조회 실패',503);return other;}return b;
        }});
        await h.app.start();await h.q('[data-history-refresh]').fire('click');await h.q('[data-history]').querySelectorAll('button')[0].fire('click');
        assert.ok(h.q('[data-receipt]').querySelectorAll('dd').some(n=>n.textContent===historyEntry().actionId));
        await h.q('[data-list]').querySelectorAll('button')[1].fire('click');
        assert.equal(h.q('[data-receipt]').querySelectorAll('dd').length,0);assert.equal(h.q('[data-history]').querySelectorAll('button').length,0);
        assert.equal(h.q('[data-history-pages]').children.length,0);assert.equal(h.reason.value,'관리자 검토');
        assert.ok(h.writes.some(w=>w.actionId===null && w.actionKind===null));assert.equal(h.calls.some(c=>c.method),false);
        assert.equal(h.q('[data-error]').hidden,!fail);
    }
});
test('failed history page keeps previous rows and exposes a retry without faking an empty result',async()=>{
    let failed=false;const b=historyBatch(),h=harness({batchValue:b,nav:{batchId:b.batchId},historyRequest:async()=>{if(failed)throw new C.RequestError('승인 목록 조회 실패',503);return historyPage();}});
    await h.app.start();await h.q('[data-history-refresh]').fire('click');failed=true;await h.q('[data-history-pages]').querySelectorAll('button')[0].fire('click');
    assert.equal(h.q('[data-history]').querySelectorAll('button').length,10);assert.equal(h.q('[data-error]').hidden,false);assert.equal(h.q('[data-history-refresh]').disabled,false);
});
test('history receipt mismatch is not accepted and an uncertain mutation blocks history reads',async()=>{
    const b=historyBatch(),h=harness({batchValue:b,nav:{batchId:b.batchId},historyRequest:async url=>url.includes('/action-history')?historyPage():{...historicalReceipt(historyEntry()),actionId:id(999)}});
    await h.app.start();await h.q('[data-history-refresh]').fire('click');await h.q('[data-history]').querySelectorAll('button')[0].fire('click');
    assert.equal(h.q('[data-error]').hidden,false);assert.equal(h.writes.some(w=>w.actionId===id(999)),false);
    const uncertain=harness({nav:{batchId:id(1)},request:async(url,o)=>{
        if(o.method)throw new C.RequestError('유실');if(url.includes('policies?'))return page([],1,20);if(url.includes('batches?page'))return page([batch()],1,10);
        if(url.includes('/classification-preview/'))return page([item(1,true),item(2)],1,100);if(url.endsWith('/classification-preview'))return preview(batch());if(url.includes('/items?'))return page([item(1),item(2)]);return batch();
    }});await uncertain.app.start();uncertain.app.arm('apply');uncertain.ack.checked=true;await uncertain.app.submit();const before=uncertain.calls.length;
    await uncertain.q('[data-history-refresh]').fire('click');assert.equal(uncertain.calls.length,before);assert.equal(uncertain.q('[data-history-refresh]').disabled,true);
});
