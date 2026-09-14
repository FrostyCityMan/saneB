import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url),P=require('../../src/main/resources/static/js/saneb-attachment-policy-core.js');
const Q=require('../../src/main/resources/static/js/saneb-attachment-provider-qa-core.js'),UI=require('../../src/main/resources/static/js/saneb-attachment-provider-qa.js');
const id=n=>`00000000-0000-0000-0000-${String(n).padStart(12,'0')}`,hash='a'.repeat(64),now='2026-09-15T00:00:00Z',later='2026-09-16T00:00:00Z';
const paged=(items=[],page=1,total=items.length)=>({items,page,size:20,totalCount:total,totalPages:Math.ceil(total/20)});
const segment=()=>({ordinal:1,caseCodes:['QA-ONE','QA-TWO'],maximumRequests:10,maximumBytes:100000,maximumSecondsIncludingMargin:500});
const plan=()=>({policyId:id(1),policyVersion:0,snapshotHash:hash,catalogHash:hash,planHash:hash,targetCount:225,catalogCaseCount:15,executableCaseCount:2,
    isExpectationCoverageComplete:false,isQaPassed:false,isReservationEnabled:true,segments:paged([segment()])});
const run=(extra={})=>({runId:id(3),policyId:id(1),policyVersion:0,snapshotHash:hash,catalogHash:hash,planHash:hash,statusCode:'READY',rowVersion:1,
    expectedCaseCount:2,maximumRequests:10,maximumBytes:100000,requestReservations:0,reservedBytes:0,segmentNo:1,segmentCount:1,catalogCaseCount:15,executableCaseCount:2,
    isExpectationCoverageComplete:false,maximumSecondsIncludingMargin:500,isInputVersionsCurrent:true,isQaPassed:false,createdAt:now,expiresAt:later,completedAt:null,...extra});
const item=(n=1,extra={})=>({caseId:id(n+3),ordinal:n,caseCode:n===1?'QA-ONE':'QA-TWO',inputHash:hash,profileHash:hash,expectedFileCount:2,statusCode:'PENDING',rowVersion:0,
    requestReservations:0,reservedBytes:0,startedAt:null,completedAt:null,errorCode:null,evidenceHash:null,...extra});
const input=()=>({acknowledgeScope:true,acknowledgeNetworkBudget:true,acknowledgeIncompleteCoverage:true,reason:'고정 분할 범위 확인'});
const command=()=>({kind:'reserve',path:`${P.base}/${id(1)}/provider-qa-runs`,policyId:id(1),payload:Q.reservation(plan(),segment(),input(),P),
    segmentCount:1,catalogCaseCount:15,executableCaseCount:2,isExpectationCoverageComplete:false});
const cancel=()=>({kind:'cancel',policyId:id(1),runId:id(3),path:`${P.base}/${id(1)}/provider-qa-runs/${id(3)}/cancellation`,payload:{expectedVersion:1,reason:'중지 영향 확인'}});
test('actual DTO contract accepts fixed plan and legacy history without claiming global QA',()=>{
    assert.ok(Q.preview(plan(),id(1),1,P));assert.ok(Q.run(run(),id(1),P));assert.ok(Q.item(item(),P));
    assert.ok(Q.run(run(Object.fromEntries(['planHash','segmentNo','segmentCount','catalogCaseCount','executableCaseCount','isExpectationCoverageComplete','maximumSecondsIncludingMargin'].map(k=>[k,null]))),id(1),P));
});
for(const [name,change]of Object.entries({wrongPolicy:d=>d.policyId=id(9),passed:d=>d.isQaPassed=true,missingPage:d=>d.segments.page=2,
    excessCases:d=>d.executableCaseCount=16,duplicateCases:d=>d.segments.items[0].caseCodes=['A','A'],wrongOrdinal:d=>d.segments.items[0].ordinal=2,
    excessiveBytes:d=>d.segments.items[0].maximumBytes=838860800001,tooLittleTime:d=>d.segments.items[0].maximumSecondsIncludingMargin=60,
    missingSegments:d=>d.segments=paged([]),invalidHash:d=>d.planHash='bad'})){
    test(`preview rejects ${name}`,()=>{const d=plan();change(d);assert.equal(Q.preview(d,id(1),1,P),false);});
}
for(const [name,patch]of Object.entries({wrongPolicy:{policyId:id(2)},passed:{isQaPassed:true},badState:{statusCode:'VERIFIED'},
    falseCompleted:{statusCode:'COMPLETED'},unfinishedWithTime:{completedAt:now},overBudget:{reservedBytes:100001},badPartialLegacy:{segmentNo:null},missingPolicy:{policyVersion:-1}})){
    test(`run rejects ${name}`,()=>assert.equal(Q.run(run(patch),id(1),P),false));
}
test('reservation requires all applicable independent confirmations and exact current page segment',()=>{
    for(const key of ['acknowledgeScope','acknowledgeNetworkBudget','acknowledgeIncompleteCoverage'])assert.throws(()=>Q.reservation(plan(),segment(),{...input(),[key]:false},P));
    assert.throws(()=>Q.reservation({...plan(),isReservationEnabled:false},segment(),input(),P));
    assert.throws(()=>Q.reservation(plan(),{...segment(),maximumRequests:1},input(),P));
    assert.throws(()=>Q.reservation(plan(),segment(),{...input(),reason:' '},P));
    const p={...plan(),isExpectationCoverageComplete:true};assert.equal(Q.reservation(p,segment(),{...input(),acknowledgeIncompleteCoverage:false},P).acknowledgeIncompleteCoverage,false);
});
test('reservation replay retains exact immutable key/body through 503, 401 and 409 then accepts later terminal receipt',async()=>{
    const sent=[];let sequence=0,keys=0;const m=Q.mutations(async(path,o)=>{sent.push({path,...o});sequence++;if(sequence<=3)throw new P.RequestError('오류',[503,401,409][sequence-1]);return run({statusCode:'COMPLETED',rowVersion:10,completedAt:now,isInputVersionsCurrent:false});},()=>{keys++;return id(8);},P);
    const c=command();await assert.rejects(m.execute(c));c.payload.maximumBytes=1;
    await assert.rejects(m.execute(command()),/이전 요청/);await assert.rejects(m.execute());await assert.rejects(m.execute());const result=await m.execute();
    assert.equal(keys,1);assert.equal(new Set(sent.map(s=>s.body)).size,1);assert.equal(new Set(sent.map(s=>s.headers['Idempotency-Key'])).size,1);assert.equal(result.data.statusCode,'COMPLETED');assert.equal(m.uncertain,false);
});
for(const [name,patch]of Object.entries({differentBudget:{maximumBytes:999},differentSegment:{segmentNo:2,segmentCount:2},wrongHash:{snapshotHash:'b'.repeat(64)},
    differentScope:{catalogCaseCount:16},wrongPolicy:{policyId:id(2)}})){
    test(`reservation wrong ${name} success is uncertain and does not permit new key`,async()=>{
        const m=Q.mutations(async()=>run(patch),()=>id(8),P);await assert.rejects(m.execute(command()));assert.ok(m.uncertain);await assert.rejects(m.execute(command()),/이전 요청/);
    });
}
test('known initial conflict leaves no uncertain reservation, but cancellation lost response requires current-state acknowledgment',async()=>{
    const known=Q.mutations(async()=>{throw new P.RequestError('충돌',409);},()=>id(8),P);await assert.rejects(known.execute(command()));assert.equal(known.sent,null);assert.equal(known.uncertain,false);
    const m=Q.mutations(async()=>{throw new P.RequestError('유실',503);},()=>id(8),P);await assert.rejects(m.execute(cancel()));assert.ok(m.uncertain);
    const current=run({statusCode:'CANCELLED',rowVersion:3,completedAt:now});assert.throws(()=>m.reconcile(current,false));assert.throws(()=>m.reconcile(run({runId:id(9)}),true));
    m.reconcile(current,true);assert.equal(m.sent,null);assert.equal(m.uncertain,false);
});
test('wrong operation path and malformed address never reach request',async()=>{
    let calls=0;const m=Q.mutations(async()=>{calls++;},()=>id(8),P);await assert.rejects(m.execute({...command(),path:'https://example.com/'}));assert.equal(calls,0);
    for(const query of ['',`?policyId=${id(1)}&policyId=${id(1)}`,`?policyId=${id(1)}&runId=bad`,`?policyId=${id(1)}&planPage=0`,`?policyId=${id(1)}&passed=true`])assert.throws(()=>Q.parameters(query,P));
});
class Element{constructor(tag='div'){this.tag=tag;this.children=[];this.events={};this.value='';this.checked=false;this.disabled=false;this.hidden=false;this.textContent='';}
    append(v){this.children.push(v);}replaceChildren(...v){this.children=v;this.textContent='';}setAttribute(k,v){this[k]=v;}focus(){this.focused=true;}
    addEventListener(k,v){this.events[k]=v;}fire(k){return this.events[k]?.({preventDefault(){}});}querySelectorAll(tag){return this.children.flatMap(c=>[...(c.tag===tag?[c]:[]),...c.querySelectorAll(tag)]);}}
const text=e=>[e.textContent,...e.children.map(text)].join(' ');
function harness({admin=true,search=`?policyId=${id(1)}&runId=${id(3)}`,request:custom}={}){
    const nodes=new Map(),q=s=>{if(!nodes.has(s))nodes.set(s,new Element());return nodes.get(s);},fields=new Map(['reason','acknowledgeScope','acknowledgeNetworkBudget','acknowledgeIncompleteCoverage','acknowledgeCancellation'].map(k=>[k,new Element()]));
    q('[data-approval-form]').elements={namedItem:n=>fields.get(n)};q('[data-approval-form]').reportValidity=()=>true;
    const fallback=async(url,o={})=>url.includes('/execution-plan?')?plan():url.includes('/cases?')?paged([item(),item(2)]):url.endsWith('/cancellation')?run({statusCode:'CANCELLED',rowVersion:3,completedAt:now}):o.method==='POST'?run():url.includes('?')?paged([run()]):run();
    const calls=[],nav=[];const app=UI.mount({page:{dataset:{isAdmin:String(admin)},querySelector:q,setAttribute(){}},P,Q,doc:{createElement:t=>new Element(t)},uuid:()=>id(8),navigation:{search:()=>search,replace:v=>nav.push({...v})},
        request:async(url,o={})=>{calls.push({url,...o});return custom?custom(url,o,fallback):fallback(url,o);}});
    const approve=()=>{for(const [k,e]of fields)k==='reason'?e.value='예산과 범위 확인':e.checked=true;};return{app,q,fields,calls,nav,approve};
}
test('workspace starts with only reads, exact plan/counts and no preselected consent',async()=>{
    const h=harness();await h.app.start();assert.equal(h.calls.length,4);assert.ok(h.calls.every(c=>!c.method));assert.ok(h.app.state.plan);assert.ok(h.app.state.run);
    assert.match(text(h.q('[data-plan]')),/225곳/);assert.match(text(h.q('[data-cases]')),/기대 파일 수 \(추출 성공 수 아님\)/);
    h.app.arm('reserve',1);assert.equal(h.fields.get('acknowledgeScope').checked,false);assert.equal(h.q('[data-approval-title]').focused,true);
});
test('reserve confirms exact segment and displays matching receipt without publishing or backfill',async()=>{
    const h=harness();await h.app.start();h.app.arm('reserve',1);h.approve();await h.app.submit();
    const writes=h.calls.filter(c=>c.method);assert.equal(writes.length,1);assert.equal(writes[0].method,'POST');assert.equal(JSON.parse(writes[0].body).maximumBytes,100000);
    assert.equal(writes[0].headers['Idempotency-Key'],id(8));assert.match(text(h.q('[data-receipt]')),/예약 응답을 확인/);assert.equal(h.q('[data-receipt]').focused,true);
    assert.equal(h.fields.get('reason').value,'');assert.equal(h.app.state.nav.runId,id(3));assert.equal(h.app.dirty,false);
});
test('operator and approver cannot forge reservation or cancellation from disabled controls',async()=>{
    const h=harness({admin:false});await h.app.start();h.app.arm('reserve',1);h.approve();await h.app.submit();h.app.arm('cancel');await h.app.submit();
    assert.equal(h.calls.filter(c=>c.method).length,0);assert.equal(h.q('[data-cancel]').disabled,true);assert.match(h.q('[data-reservation-condition]').textContent,/조회 전용/);
});
test('missing consent or blank reason makes no request and preserves entered reason',async()=>{
    const h=harness();await h.app.start();h.app.arm('reserve',1);h.fields.get('reason').value='확인 중';await h.app.submit();assert.equal(h.calls.filter(c=>c.method).length,0);
    assert.equal(h.fields.get('reason').value,'확인 중');assert.match(h.q('[data-error]').textContent,/전체 공고 코드/);
});
test('plan 409 or OFF does not prevent reading and cancelling an existing run',async()=>{
    for(const unavailable of [true,false]){const h=harness({request:async(url,o,next)=>{if(url.includes('/execution-plan?')){if(unavailable)throw new P.RequestError('설치 불가',409);return {...plan(),isReservationEnabled:false};}return next(url,o);}});
        await h.app.start();assert.ok(h.app.state.run);assert.equal(h.q('[data-cancel]').disabled,false);h.app.arm('reserve',1);assert.equal(h.app.state.action,null);
        h.app.arm('cancel');h.approve();await h.app.submit();assert.equal(h.calls.filter(c=>c.method).length,1);assert.equal(h.calls.find(c=>c.method).method,'PUT');assert.equal(h.app.state.run.statusCode,'CANCELLED');}
});
test('lost reservation locks edits and replays exact body/key even after 403; later success resolves',async()=>{
    let attempt=0;const h=harness({request:async(url,o,next)=>{if(o.method==='POST'){attempt++;if(attempt<3)throw new P.RequestError('유실',attempt===1?503:403);}return next(url,o);}});
    await h.app.start();h.app.arm('reserve',1);h.approve();await h.app.submit();assert.ok(h.app.mutation.uncertain);assert.ok(h.q('[data-uncertain]').hidden===false);
    h.fields.get('reason').value='변경 시도';h.app.arm('cancel');await h.app.submit(true);assert.ok(h.app.mutation.uncertain);await h.app.submit(true);
    const writes=h.calls.filter(c=>c.method);assert.equal(writes.length,3);assert.equal(new Set(writes.map(w=>w.body)).size,1);assert.equal(new Set(writes.map(w=>w.headers['Idempotency-Key'])).size,1);
    assert.equal(h.app.mutation.uncertain,false);
});
test('lost cancel current-state adoption is explicit and does not declare original success',async()=>{
    let lost=false;const h=harness({request:async(url,o,next)=>{if(o.method==='PUT'){lost=true;throw new P.RequestError('유실',503);}if(lost&&url.endsWith(id(3)))return run({statusCode:'CANCELLED',rowVersion:3,completedAt:now});return next(url,o);}});
    await h.app.start();h.app.arm('cancel');h.approve();await h.app.submit();await h.app.inspect();h.app.reconcile();assert.ok(h.app.mutation.uncertain);
    h.q('[data-reconcile-ack]').checked=true;h.app.reconcile();assert.equal(h.app.mutation.uncertain,false);assert.match(h.q('[data-status]').textContent,/성공을 확정한 것은 아닙니다/);
    assert.equal(h.calls.filter(c=>c.method).length,1);
});
test('CANCEL_REQUESTED remains pending cleanup and cannot cancel twice',async()=>{
    const h=harness({request:async(url,o,next)=>o.method==='PUT'?run({statusCode:'CANCEL_REQUESTED',rowVersion:2}):next(url,o)});
    await h.app.start();h.app.arm('cancel');h.approve();await h.app.submit();assert.equal(h.q('[data-cancel]').disabled,true);assert.match(text(h.q('[data-run]')),/소유자 정리 대기/);
});
test('invalid initial URI has no API calls; bad case denominator is not rendered as empty success',async()=>{
    const bad=harness({search:'?policyId=bad'});await bad.app.start();assert.equal(bad.calls.length,0);
    const h=harness({request:async(url,o,next)=>url.includes('/cases?')?paged([item()]):next(url,o)});await h.app.start();assert.match(h.q('[data-error]').textContent,/전체 공고 수/);assert.equal(h.q('[data-cases]').children.length,0);
});
test('busy duplicate submit and refresh never send parallel mutation or discard frozen approval',async()=>{
    let resolve;const h=harness({request:async(url,o,next)=>o.method==='POST'?new Promise(r=>resolve=r):next(url,o)});await h.app.start();h.app.arm('reserve',1);h.approve();
    const pending=h.app.submit();await h.app.submit();await h.q('[data-plan-refresh]').fire('click');assert.equal(h.calls.filter(c=>c.method).length,1);resolve(run());await pending;
});

test('plan pagination preserves reason, clears consent and binds selection to the newly loaded segment',async()=>{
    const h=harness({request:async(url,o,next)=>{if(!url.includes('/execution-plan?'))return next(url,o);
        const page=Number(new URL(url,'http://localhost').searchParams.get('page'));
        const segments=Array.from({length:page===1?20:1},(_,i)=>({...segment(),ordinal:(page-1)*20+i+1,caseCodes:[`QA-${(page-1)*20+i+1}`]}));
        return {...plan(),catalogCaseCount:21,executableCaseCount:21,segments:paged(segments,page,21)};}});
    await h.app.start();h.app.arm('reserve',1);h.approve();
    await h.q('[data-plan-pages]').querySelectorAll('button')[0].fire('click');
    assert.equal(h.app.state.nav.planPage,2);assert.equal(h.app.state.action,null);assert.equal(h.fields.get('reason').value,'예산과 범위 확인');
    assert.equal(h.fields.get('acknowledgeScope').checked,false);assert.match(text(h.q('[data-segments]')),/21번 QA 분할/);
    h.app.arm('reserve',1);assert.equal(h.app.state.action,null);h.app.arm('reserve',21);assert.equal(h.app.state.action.segment.ordinal,21);
    assert.equal(h.calls.filter(c=>c.method).length,0);
});

test('history and case pagination retain selected run and require exact full denominators',async()=>{
    const selected=run({expectedCaseCount:21,executableCaseCount:21,catalogCaseCount:21});const h=harness({request:async(url,o,next)=>{
        const u=new URL(url,'http://localhost'),page=Number(u.searchParams.get('page')||1);
        if(url.includes('/execution-plan?'))return next(url,o);
        if(url.includes('/cases?'))return paged(Array.from({length:page===1?20:1},(_,i)=>item((page-1)*20+i+1)),page,21);
        if(u.searchParams.has('page'))return paged(Array.from({length:page===1?20:1},(_,i)=>run({runId:id(100+(page-1)*20+i)})),page,21);
        return selected;}});
    await h.app.start();await h.q('[data-run-pages]').querySelectorAll('button')[0].fire('click');
    assert.equal(h.app.state.nav.runPage,2);assert.equal(h.app.state.nav.runId,id(3));assert.equal(h.app.state.run.runId,id(3));
    await h.q('[data-case-pages]').querySelectorAll('button')[0].fire('click');
    assert.equal(h.app.state.nav.casePage,2);assert.equal(h.app.state.nav.runId,id(3));assert.match(text(h.q('[data-cases]')),/21\./);
    assert.match(text(h.q('[data-case-pages]')),/현재 1건 \/ 전체 21건/);assert.equal(h.calls.filter(c=>c.method).length,0);
});

test('failed plan refresh invalidates previously selectable approval and retains history for cancellation',async()=>{
    let unavailable=false;const h=harness({request:async(url,o,next)=>{if(unavailable&&url.includes('/execution-plan?'))throw new P.RequestError('계획 변경',409);return next(url,o);}});
    await h.app.start();h.app.arm('reserve',1);h.approve();unavailable=true;await h.q('[data-plan-refresh]').fire('click');
    assert.equal(h.app.state.plan,null);assert.equal(h.app.state.action,null);assert.equal(h.fields.get('acknowledgeScope').checked,false);
    h.app.arm('reserve',1);assert.equal(h.app.state.action,null);assert.equal(h.q('[data-cancel]').disabled,false);
    assert.equal(h.calls.filter(c=>c.method).length,0);
});
