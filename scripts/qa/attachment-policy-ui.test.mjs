import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
const require=createRequire(import.meta.url),P=require('../../src/main/resources/static/js/saneb-attachment-policy-core.js'),UI=require('../../src/main/resources/static/js/saneb-attachment-policies.js');
const id=n=>`00000000-0000-0000-0000-${String(n).padStart(12,'0')}`,hash='a'.repeat(64),now='2026-09-12T00:00:00Z';
const summary=(patch={})=>({policyId:id(1),policyCode:'ATT-QA',versionNo:1,rowVersion:0,policyStatusCode:'DRAFT',modeCode:'OFF',ruleReleaseId:id(2),ruleReleaseStatusCode:'ACTIVE',policyHash:null,createdAt:now,publishedAt:null,...patch});
const detail=(patch={})=>({policy:summary(),configuration:{maximumSourceBytes:83886080,engineVersion:'engine',extractorVersion:'extractor',extractorConfigHash:null},systemProfileBindings:[{providerCode:'BIZINFO',profileCode:'BIZINFO_V1',profileHash:hash}],copiedFromPolicyId:null,isEditable:true,isDraftValidationRequired:true,updatedAt:now,...patch});
const steps=()=>P.steps.map(stepCode=>({stepCode,statusCode:'NOT_RUN',evidenceHash:null,evidence:{}}));
const run=(patch={})=>({runId:id(3),policyId:id(1),policyVersion:0,ruleReleaseId:id(2),ruleVersion:0,snapshotHash:hash,statusCode:'INCOMPLETE',rowVersion:4,inputVersionsCurrent:true,errorCode:'REQUIRED_QA_EVIDENCE_MISSING',createdAt:now,startedAt:now,completedAt:now,steps:steps(),...patch});
const counts=()=>Object.fromEntries(Object.keys(P.countFields).map(k=>[k,0]));
const impact=(d=detail(),patch={})=>({policy:d.policy,activePolicyForRule:null,matchingRule:counts(),allRules:counts(),maximumSourceBytes:d.configuration.maximumSourceBytes,wouldStopNewExternalRequests:true,wouldLiftGlobalOffStop:false,latestQa:null,blockingReasonCodes:['QA_NOT_VERIFIED','PUBLICATION_REVALIDATION_REQUIRED'],requiresPublicationRevalidation:true,observedImpactHash:hash,observedAt:now,currentHttpRequests:0,...patch});
const paged=(items,n=1,size=10,total=items.length)=>({items,page:n,size,totalCount:total,totalPages:Math.ceil(total/size)});
const rules=()=>paged([{releaseId:id(2),releaseCode:'규칙1',versionNo:1,releaseStatusCode:'ACTIVE'}],1,20);
const input=()=>({ruleReleaseId:id(2),modeCode:'OFF',maximumSourceBytes:'83886080'});
const segmentDetail=(version='segment-role-1.0.0',rowVersion=0)=>detail({policy:summary({rowVersion}),configuration:{...detail().configuration,
    engineVersion:'attachment-segment-1.0.0',segmentRuleVersion:version,segmentRulesHash:P.segmentVersions[version]}});

test('explicit segment choices are allowlisted and omitted update preserves exact stored version in receipt',()=>{
    const d=segmentDetail(),s={detail:d};
    const command=P.command('update',s,{...input(),segmentRuleVersion:'segment-role-1.0.2'},'규칙 전환',true);
    assert.equal(command.payload.segmentRuleVersion,'segment-role-1.0.2');assert(!('segmentRulesHash' in command.payload));
    assert.equal(P.receipt(segmentDetail('segment-role-1.0.2',1),command),true);
    assert.equal(P.receipt(segmentDetail('segment-role-1.0.0',1),command),false);
    const omitted=P.command('update',{detail:segmentDetail('segment-role-1.0.2')},input(),'한도만 변경',true);
    assert.equal(omitted.payload.segmentRuleVersion,undefined);
    assert.equal(P.receipt(segmentDetail('segment-role-1.0.2',1),omitted),true);
    assert.equal(P.receipt(segmentDetail('segment-role-1.0.0',1),omitted),false);
    for(const v of ['segment-role-1.0.1','future','segment-role-1.0.2 '])assert.throws(()=>P.command('create',{}, {...input(),segmentRuleVersion:v},'규칙 선택',true));
    assert.throws(()=>P.command('update',{detail:detail()}, {...input(),segmentRuleVersion:'segment-role-1.0.2'},'전환 금지',true),/새 정책/);
    for(const v of Object.keys(P.segmentVersions))assert.equal(P.command('create',{}, {...input(),segmentRuleVersion:v},'새 초안',true).payload.segmentRuleVersion,v);
});
const cmd=(kind='qa')=>P.command(kind,{detail:detail(),run:run({statusCode:'RUNNING'})},input(),'업무 확인',true);

test('strict details and impact distinguish counts, zero and absent data',()=>{
    assert.equal(P.details(detail()),true);assert.equal(P.impact(impact(),detail()),true);
    for(const patch of [{matchingRule:{...counts(),boundSourceCount:-1}},{allRules:null},{matchingRule:{...counts(),boundSourceCount:1}},{currentHttpRequests:1},{requiresPublicationRevalidation:false},{blockingReasonCodes:[]},{maximumSourceBytes:1},{policy:summary({rowVersion:1})},{policy:summary({modeCode:'ENFORCE'})},{activePolicyForRule:summary({policyStatusCode:'ACTIVE',ruleReleaseId:id(7)})}])assert.equal(P.impact(impact(detail(),patch),detail()),false);
    assert.equal(P.details(detail({policy:summary({rowVersion:2147483648})})),false);assert.equal(P.details(detail({isEditable:true,policy:summary({policyStatusCode:'ACTIVE'})})),false);
});
test('role policy version and hash are paired while legacy policy remains valid without automatic upgrade',()=>{
    const d=detail();assert.equal(P.details(d),true);
    assert.equal(P.details({...d,configuration:{...d.configuration,roleRuleVersion:'document-role-1.0.1',roleRulesHash:hash}}),true);
    for(const config of [{roleRuleVersion:'v1'},{roleRulesHash:hash},{roleRuleVersion:'v1',roleRulesHash:'invalid'},{roleRuleVersion:'<script>',roleRulesHash:hash}])
        assert.equal(P.details({...d,configuration:{...d.configuration,...config}}),false);
    const script=readFileSync(new URL('../../src/main/resources/static/js/saneb-attachment-policies.js',import.meta.url),'utf8');
    assert.match(script,/기존 정책에는 새 역할 규칙을 자동 적용하지 않습니다/);
    assert.match(script,/정책 게시·기존 데이터 재처리는 별도 승인 대상/);
});
test('policy profiles accept actual Gov24 and historical alias but never unknown provider codes',()=>{
    for(const providerCode of ['GOV24_PUBLIC_SERVICE','GOV24']) {
        assert.equal(P.details(detail({systemProfileBindings:[{providerCode,profileCode:'GOV24_TEST_ONLY',profileHash:hash}]})),true);
        assert.equal(P.label(providerCode),'정부24');
    }
    assert.equal(P.details(detail({systemProfileBindings:[{providerCode:'UNKNOWN',profileCode:'UNKNOWN',profileHash:hash}]})),false);
});
test('QA history rejects missing/duplicate steps, unknown success and wrong policy',()=>{
    assert.equal(P.run(run(),id(1)),true);
    for(const patch of [{policyId:id(9)},{steps:[]},{steps:[...steps().slice(0,3),steps()[0]]},{statusCode:'SUCCEEDED'},{steps:steps().map(s=>({...s,statusCode:'PASSED'}))},{policyVersion:-1}])assert.equal(P.run(run(patch),id(1)),false);
    assert.match(P.label('VERIFIED'),/게시 재검증/);assert.match(P.label('INCOMPLETE'),/증거 부족/);assert.notEqual(P.label('MISSING'),P.label('FAILED'));
});
test('commands accept only contract fields and correct policy versus run version',()=>{
    const state={detail:detail({policy:summary({rowVersion:7})}),run:run({statusCode:'RUNNING',rowVersion:11})};
    assert.equal(P.command('qa',state,input(),'사유',true).payload.expectedVersion,7);
    assert.equal(P.command('cancel',state,input(),'사유',true).payload.expectedVersion,11);
    const create=P.command('create',{}, {...input(),url:'https://ignored.test',passed:true},'사유',true);
    assert.deepEqual(Object.keys(create.payload).sort(),['maximumSourceBytes','modeCode','reason','ruleReleaseId']);
    for(const n of ['0','1.5','83886081','',NaN])assert.throws(()=>P.command('create',{}, {...input(),maximumSourceBytes:n},'사유',true));
    for(const [reason,ack]of [['',true],[' ',true],['가'.repeat(1001),true],['이유',false]])assert.throws(()=>P.command('qa',state,input(),reason,ack));
    assert.throws(()=>P.command('publication',state,input(),'사유',true));
});
test('creation replay may show a later policy state; update receipt still matches exact CAS',()=>{
    const create=cmd('create');assert.equal(P.receipt(detail({policy:summary({rowVersion:4,policyStatusCode:'ACTIVE'}),isEditable:false}),create),true);
    const update=cmd('update');assert.equal(P.receipt(detail({policy:summary({rowVersion:1})}),update),true);
    assert.equal(P.receipt(detail({policy:summary({rowVersion:2})}),update),false);
    assert.equal(P.receipt(detail({configuration:{...detail().configuration,maximumSourceBytes:1},policy:summary({rowVersion:1})}),update),false);
    assert.equal(P.receipt(run({statusCode:'CANCEL_REQUESTED'}),cmd('cancel')),true);
    assert.equal(P.receipt(run(),cmd('cancel')),false);
});
test('lost keyed QA preserves body and key through 401/403/409; no new request substitutes',async()=>{
    let n=0;const calls=[],m=P.mutations(async(url,o)=>{calls.push({url,...o});if(n++<4)throw new P.RequestError('미확정',[0,401,403,409][n-1]);return run();},()=>id(9));
    await assert.rejects(m.execute(cmd()));for(let i=0;i<3;i++){await assert.rejects(m.execute());assert.equal(m.uncertain,true);}
    await assert.rejects(m.execute(cmd()));await m.execute();assert.equal(m.uncertain,false);
    assert.equal(new Set(calls.map(c=>c.body)).size,1);assert.equal(new Set(calls.map(c=>c.headers['Idempotency-Key'])).size,1);
});
test('uncertain unkeyed changes require separate valid current-state acknowledgment',async()=>{
    const m=P.mutations(async()=>{throw new P.RequestError('미확정');},()=>id(9));await assert.rejects(m.execute(cmd('update')));
    assert.throws(()=>m.reconcile(detail(),false));assert.throws(()=>m.reconcile(detail({policy:summary({policyId:id(9)})}),true));
    m.reconcile(detail({policy:summary({rowVersion:2})}),true);assert.equal(m.uncertain,false);
    const q=P.mutations(async()=>{throw new P.RequestError('미확정');},()=>id(9));await assert.rejects(q.execute(cmd()));assert.throws(()=>q.reconcile(run(),true));
});
test('transport only reads v1 rule list and blocks external/path traversal mutations',async()=>{
    const calls=[],api=P.client(async(url,o)=>{calls.push({url,...o});return {ok:true,status:200,json:async()=>({success:true,data:detail()})};});
    await api(P.rules+'?page=1&size=20');assert.equal(calls[0].cache,'no-store');assert.equal(calls[0].redirect,'error');
    for(const url of ['https://evil.test'+P.base,P.base+'/../secret',P.base+'/%2e%2e/secret','//evil.test',P.rules+'/x'])await assert.rejects(api(url));
    await assert.rejects(api(P.rules,{method:'POST'}));assert.equal(calls.length,1);
});
test('transport reports session loss, invalid response, network and service failure honestly',async()=>{
    for(const [status,success,expected]of [[401,false,false],[403,false,false],[409,false,false],[503,false,true],[200,false,true]]){
        const api=P.client(async()=>({ok:status===200,status,json:async()=>({success,message:'확인 필요'})}));
        await assert.rejects(api(P.base),e=>e.uncertain===expected);
    }
    const api=P.client(async()=>{throw new Error('private internal URL');});await assert.rejects(api(P.base),e=>!e.message.includes('private')&&e.uncertain);
});

// 실제 화면 이벤트 구현 + 제한 DOM/API 대역. 브라우저 렌더링 또는 PostgreSQL 증거는 아니다.
class Element {
    constructor(tag='div'){this.tag=tag;this.children=[];this.events={};this.dataset={};this.hidden=false;this.disabled=false;this.checked=false;this.value='';this.textContent='';}
    append(...items){this.children.push(...items);}replaceChildren(...items){this.children=items;}setAttribute(k,v){this[k]=v;}focus(){this.focused=true;}
    addEventListener(t,f){this.events[t]=f;}async fire(t,e={}){return this.events[t]?.({preventDefault(){},target:this,...e});}
    querySelectorAll(tag){return this.children.flatMap(c=>[...(c.tag===tag?[c]:[]),...c.querySelectorAll(tag)]);}
}
const text=e=>[e.textContent,...e.children.map(text)].join(' ');
function harness({admin=true,nav={policyId:id(1)},request:custom,confirm=true}={}){
    const nodes=new Map(),q=s=>{if(!nodes.has(s))nodes.set(s,new Element());return nodes.get(s);},fields=new Map(Object.entries(input()).map(([k,v])=>{const e=new Element();e.value=v;return[k,e];}));
    fields.set('segmentRuleVersion',new Element());
    q('[data-editor]').elements={namedItem:n=>fields.get(n)};q('[data-editor]').reportValidity=()=>true;
    const reason=new Element('textarea'),ack=new Element('input'),consents=new Map(['acknowledgeNewCollectionBehavior','acknowledgeExistingJobsUnchanged','acknowledgeNoBackfill'].map(n=>[n,new Element('input')]));reason.value='업무 확인';q('[data-approval-form]').elements={namedItem:n=>n==='reason'?reason:n==='acknowledged'?ack:consents.get(n)};q('[data-approval-form]').reportValidity=()=>true;
    const calls=[],params={...nav},navigation={read:()=>({...params}),url:patch=>'/app/admin/announcement-attachment-policies?'+new URLSearchParams(Object.entries({...params,...patch}).filter(([,v])=>v!=null)),replace:patch=>Object.assign(params,patch)};
    const fallback=async(url)=>{
        if(url.startsWith(P.rules))return rules();if(url.startsWith(P.base+'?'))return paged([summary()]);
        if(url.endsWith('/publication-impact'))return impact(detail({isEditable:admin}));
        if(url.includes('/publication-scopes?'))return paged([]);
        if(url.endsWith('/publication'))throw new P.RequestError('영수증 없음',404);
        if(url.includes('/validation-runs?'))return paged([run()]);if(url.includes('/validation-runs/'))return run();return detail({isEditable:admin});
    };
    const request=async(url,o={})=>{calls.push({url,...o});return custom?custom(url,o,fallback):fallback(url);};
    const app=UI.mount({page:{dataset:{isAdmin:String(admin)},querySelector:q,setAttribute(){}},P,request,doc:{createElement:t=>new Element(t)},uuid:()=>id(9),navigation,confirmDiscard:()=>confirm});
    return {app,q,calls,fields,reason,ack,consents,params};
}
test('policy workspace deep link shows QA incomplete and global impact without any writes',async()=>{
    const h=harness();await h.app.start();assert.equal(h.app.state.detail.policy.policyId,id(1));assert.equal(h.calls.some(c=>c.method),false);
    assert.match(text(h.q('[data-run]')),/필수 QA 증거 부족/);assert.match(text(h.q('[data-impact]')),/합산하지 않습니다/);
    assert.match(text(h.q('[data-impact]')),/정확한 대상 목록의 고정 지문이나 게시 승인 토큰이 아닙니다/);assert.equal(h.q('[data-save]').disabled,false);
    const coverage=h.q('[data-detail]').children.find(e=>e.tag==='a'&&e.textContent.includes('수집원별 첨부 검증 범위'));
    assert.equal(coverage.href,'/app/admin/announcement-attachment-provider-coverage?policyId='+id(1));
    assert.equal(h.q('[data-detail]').children.find(e=>e.tag==='a'&&e.textContent==='수집원 실파일 QA 예약·이력·취소').href,'/app/admin/announcement-attachment-provider-qa?policyId='+id(1));
});
test('operator or approver cannot forge write actions even by firing disabled controls',async()=>{
    const h=harness({admin:false});await h.app.start();for(const kind of ['save','qa','cancel','revision']){h.app.arm(kind);h.ack.checked=true;await h.app.submit();}
    h.app.reset(true);assert.equal(h.calls.some(c=>c.method),false);assert.equal(h.q('[data-editor-fields]').disabled,true);
});
test('input edit disarms approval, preserves reason and blocks QA of unsaved edits',async()=>{
    const h=harness();await h.app.start();h.app.arm('save');h.ack.checked=true;h.fields.get('maximumSourceBytes').value='1000';await h.q('[data-editor]').fire('input');
    assert.equal(h.ack.checked,false);assert.equal(h.q('[data-approval]').hidden,true);assert.equal(h.reason.value,'업무 확인');assert.equal(h.q('[data-qa]').disabled,true);
    h.app.arm('qa');assert.equal(h.q('[data-error]').hidden,false);assert.equal(h.app.dirty,true);
});
test('closing the confirmation returns keyboard focus to the invoking action',async()=>{
    const h=harness();await h.app.start();h.app.arm('qa');await h.q('[data-close-approval]').fire('click');
    assert.equal(h.q('[data-qa]').focused,true);assert.equal(h.q('[data-approval]').hidden,true);assert.equal(h.calls.some(c=>c.method),false);
});
test('rule refresh removes retired eligibility instead of retaining a cached active option',async()=>{
    let retired=false;const h=harness({request:async(url,o,next)=>url.startsWith(P.rules)&&retired?paged([{...rules().items[0],releaseStatusCode:'RETIRED'}],1,20):next(url)});
    await h.app.start();retired=true;await h.q('[data-rule-refresh]').fire('click');h.app.arm('save');
    assert.match(h.q('[data-error]').textContent,/퇴역/);assert.equal(h.calls.some(c=>c.method),false);
});
test('CAS update confirms exact saved fields and uses policy version',async()=>{
    let saved=false;const updated=detail({policy:summary({rowVersion:1})});const h=harness({request:async(url,o,next)=>{
        if(o.method==='PUT'){saved=true;return updated;}if(saved){if(url.endsWith('/publication-impact'))return impact(updated);if(url===P.base+'/'+id(1))return updated;}return next(url);
    }});await h.app.start();h.app.arm('save');assert.equal(h.calls.some(c=>c.method),false);h.ack.checked=true;await h.app.submit();
    const call=h.calls.find(c=>c.method==='PUT');assert.equal(JSON.parse(call.body).expectedVersion,0);assert.deepEqual(call.headers,{});
    assert.match(text(h.q('[data-receipt]')),/조회 버전 1/);assert.equal(h.app.mutation.uncertain,false);assert.equal(h.app.state.detail.policy.rowVersion,1);
    assert.equal(h.q('[data-receipt]').focused,true);
});
for(const version of ['segment-role-1.0.2','segment-role-1.0.3']) test(`segment selector ${version} requires reviewed change, clears consent and never publishes`,async()=>{
    let saved=false;const old=segmentDetail(),updated=segmentDetail(version,1);
    const h=harness({request:async(url,o,next)=>{
        if(o.method==='PUT'){saved=true;return updated;}
        if(url===P.base+'/'+id(1))return saved?updated:old;
        if(url.endsWith('/publication-impact'))return impact(saved?updated:old);
        return next(url);
    }});await h.app.start();assert.equal(h.fields.get('segmentRuleVersion').disabled,false);
    assert.match(text(h.q('[data-detail]')),/구간 규칙 버전/);
    h.app.arm('save');h.ack.checked=true;h.fields.get('segmentRuleVersion').value=version;
    await h.q('[data-editor]').fire('input');assert.equal(h.ack.checked,false);assert.equal(h.q('[data-qa]').disabled,true);
    h.app.arm('save');assert.match(text(h.q('[data-action-impact]')),/segment-role-1.0.0/);assert.ok(text(h.q('[data-action-impact]')).includes(version));
    assert.match(text(h.q('[data-action-impact]')),/전체 QA/);h.ack.checked=true;await h.app.submit();
    const writes=h.calls.filter(c=>c.method);assert.equal(writes.length,1);assert.equal(writes[0].method,'PUT');
    assert.equal(JSON.parse(writes[0].body).segmentRuleVersion,version);assert.equal(h.app.mutation.uncertain,false);
    assert.equal(h.fields.get('segmentRuleVersion').value,'');assert.ok(text(h.q('[data-detail]')).includes(version));
});
test('structural version receipt rejects quarter fallback and wrong hash; omitted updates keep structural version',()=>{
    const version='segment-role-1.0.3',d=segmentDetail(version);
    const command=P.command('update',{detail:d},input(),'한도 수정',true);
    assert.equal(command.payload.segmentRuleVersion,undefined);
    assert.equal(P.receipt(segmentDetail(version,1),command),true);
    assert.equal(P.receipt(segmentDetail('segment-role-1.0.2',1),command),false);
    const forged=segmentDetail(version,1);forged.configuration.segmentRulesHash=P.segmentVersions['segment-role-1.0.2'];
    assert.equal(P.receipt(forged,command),false);
    const template=readFileSync(new URL('../../src/main/resources/templates/app/announcement-attachment-policies.html',import.meta.url),'utf8');
    assert.match(template,/<option value="segment-role-1\.0\.3">1\.0\.3 · 내부 신청안내 절·연속 중복 표제 보완<\/option>/);
});
test('legacy selector is disabled and creation choice resets to explicit unchanged default',async()=>{
    const h=harness();await h.app.start();assert.equal(h.fields.get('segmentRuleVersion').disabled,true);
    h.fields.get('segmentRuleVersion').value='segment-role-1.0.2';h.app.arm('save');assert.equal(h.calls.some(c=>c.method),false);
    assert.match(h.q('[data-error]').textContent,/새 정책/);h.app.reset(true);
    assert.equal(h.fields.get('segmentRuleVersion').disabled,false);assert.equal(h.fields.get('segmentRuleVersion').value,'');
});
test('creation and revision use keyed POST without publication; returned policy becomes selection',async()=>{
    for(const kind of ['create','revision']){
        let saved=false;const d=detail({policy:summary({policyId:id(5),versionNo:kind==='revision'?2:1}),copiedFromPolicyId:kind==='revision'?id(1):null});
        const h=harness({nav:kind==='create'?{}:{policyId:id(1)},request:async(url,o,next)=>{
            if(o.method==='POST'){saved=true;return d;}if(saved&&url===P.base+'/'+id(5))return d;if(saved&&url.endsWith('/publication-impact'))return impact(d);
            if(saved&&url.includes('/validation-runs?'))return paged([]);return next(url);
        }});await h.app.start();h.fields.get('ruleReleaseId').value=id(2);h.app.arm(kind==='create'?'save':'revision');h.ack.checked=true;await h.app.submit();
        const calls=h.calls.filter(c=>c.method);assert.equal(calls.length,1);assert.equal(calls[0].headers['Idempotency-Key'],id(9));assert.equal(h.params.policyId,id(5));
        assert.equal(h.calls.some(c=>c.method&&c.url.endsWith('/publication')),false);assert.equal(h.app.state.detail.policy.policyId,id(5));
    }
});

const scopeSummary=(patch={})=>({scopeId:id(6),policyId:id(1),policyVersion:0,ruleReleaseId:id(2),ruleVersion:0,modeCode:'OFF',qaRunId:id(3),qaSnapshotHash:hash,itemCount:1,scopeHash:'b'.repeat(64),createdAt:new Date(Date.now()-1000).toISOString(),expiresAt:new Date(Date.now()+599000).toISOString(),...patch});
const scope=(patch={})=>({scope:scopeSummary(),isExpired:false,isScopeCurrent:true,isApproval:false,requiresPublicationRevalidation:true,currentHttpRequests:0,...patch});
const verified=()=>run({statusCode:'VERIFIED',errorCode:null,steps:steps().map(s=>({...s,statusCode:'PASSED',evidenceHash:hash}))});
const ready=()=>{const d=detail();return {detail:d,run:verified(),scope:scope(),impact:impact(d,{latestQa:verified(),blockingReasonCodes:['PUBLICATION_REVALIDATION_REQUIRED']})};};
const consentInput=()=>({acknowledgeNewCollectionBehavior:true,acknowledgeExistingJobsUnchanged:true,acknowledgeNoBackfill:true});
const publication=(patch={})=>({publication:{publicationId:id(7),policyId:id(1),publishedPolicyVersion:1,policyHash:hash,previousPolicyId:null,previousPolicyVersion:null,scopeId:id(6),scopeHash:'b'.repeat(64),qaRunId:id(3),modeCode:'OFF',publishedAt:now,...patch},existingDataApplied:false,workerEnabledByRequest:false,currentHttpRequests:0});
function publicationHarness({admin=true,write,scopeOverride,publicationRead,itemsFailure=false}={}){
    const r=ready();if(scopeOverride)r.scope=scopeOverride;
    return harness({admin,request:async(url,o,next)=>{
        if(o.method)return write?write(url,o):publication();
        if(url===P.base+'/'+id(1))return {...r.detail,isEditable:admin};
        if(url.endsWith('/publication-impact'))return {...r.impact,policy:{...r.impact.policy}};
        if(url.includes('/publication-scopes?'))return paged([r.scope.scope]);
        if(url.includes('/items?')){if(itemsFailure)throw new P.RequestError('항목 조회 실패',503);return paged([{entityTypeCode:'POLICY',entityId:id(1),stateHash:hash}],1,20);}
        if(url.endsWith('/publication-scopes/'+id(6)))return r.scope;
        if(url.endsWith('/publication')&&publicationRead)return publicationRead();
        return next(url);
    }});
}
test('scope details validate bounded lifetime, identities, totals and non-approval flags',()=>{
    assert.equal(P.scope(scope(),id(1)),true);
    for(const patch of [{scope:scopeSummary({policyId:id(99)})},{scope:scopeSummary({qaSnapshotHash:null})},{scope:scopeSummary({itemCount:0})},
        {scope:scopeSummary({expiresAt:new Date(Date.now()-60000).toISOString()})},{scope:scopeSummary({scopeHash:'x'})},{isApproval:true},{isScopeCurrent:true,isExpired:true},{currentHttpRequests:1}])assert.equal(P.scope(scope(patch),id(1)),false);
    assert.equal(P.scopeItem({entityTypeCode:'SOURCE',entityId:id(4),stateHash:hash}),true);
    assert.equal(P.scopeItem({entityTypeCode:'SOURCE',entityId:'raw source URL',stateHash:hash}),false);
});
test('publish requires all latest QA steps and exact live scope, not success metadata alone',()=>{
    assert.equal(P.canPublish(ready()),true);
    for(const mutate of [s=>s.scope.isExpired=true,s=>s.scope.isScopeCurrent=false,s=>s.scope.scope.policyVersion++,s=>s.scope.scope.ruleVersion++,
        s=>s.scope.scope.modeCode='ENFORCE',s=>s.scope.scope.qaRunId=id(8),s=>s.scope.scope.qaSnapshotHash='c'.repeat(64),s=>s.scope.scope.expiresAt=new Date(Date.now()-1).toISOString(),
        s=>s.impact.latestQa.statusCode='INCOMPLETE',s=>s.impact.latestQa.inputVersionsCurrent=false,s=>s.impact.latestQa.steps[3].statusCode='MISSING',
        s=>s.impact.blockingReasonCodes.push('QA_NOT_VERIFIED'),s=>s.detail.policy.ruleReleaseStatusCode='DRAFT',s=>s.detail.isEditable=false]){
        const s=ready();mutate(s);assert.equal(P.canPublish(s),false);assert.throws(()=>P.command('publish',s,consentInput(),'게시 사유',true));
    }
});
test('prepare does not send client QA evidence; publication needs three independent true consents',()=>{
    const s=ready(),prepared=P.command('prepare',s,{passed:true,scopeHash:'fake'},'범위 확인',true);assert.equal(prepared.path,P.base+'/'+id(1)+'/publication-scopes');
    assert.deepEqual(prepared.payload,{expectedVersion:0,reason:'범위 확인'});
    for(const key of Object.keys(consentInput()))assert.throws(()=>P.command('publish',s,{...consentInput(),[key]:false},'게시',true));
    const command=P.command('publish',s,{...consentInput(),modeCode:'ENFORCE',passed:true,scopeId:id(99)},'게시',true);
    assert.equal(command.payload.scopeId,id(6));assert.equal(command.modeCode,'OFF');assert.equal(command.keyed,true);
    assert.deepEqual(Object.keys(command.payload).sort(),['scopeId','scopeHash','expectedVersion','reason',...Object.keys(consentInput())].sort());
});
test('publication receipt binds exact requested version, scope, QA and mode, without old-data writes',()=>{
    const s=P.command('publish',ready(),consentInput(),'게시',true);assert.equal(P.receipt(publication(),s),true);
    for(const patch of [{policyId:id(9)},{scopeId:id(9)},{scopeHash:'c'.repeat(64)},{qaRunId:id(9)},{policyHash:'c'.repeat(64)},{publishedPolicyVersion:2},{modeCode:'ENFORCE'},{previousPolicyId:id(1)},{previousPolicyId:id(9),previousPolicyVersion:null}])assert.equal(P.receipt(publication(patch),s),false);
    for(const patch of [{existingDataApplied:true},{workerEnabledByRequest:true},{currentHttpRequests:1}])assert.equal(P.receipt({...publication(),...patch},s),false);
});
test('lost publication keeps original scope, consents and key even after expiry or 409',async()=>{
    const calls=[];let n=0;const m=P.mutations(async(url,o)=>{calls.push({url,...o});if(n++<2)throw new P.RequestError('미확정',n===1?503:409);return publication();},()=>id(99));
    await assert.rejects(m.execute(P.command('publish',ready(),consentInput(),'게시',true)));await assert.rejects(m.execute());
    assert.equal(m.uncertain,true);await assert.rejects(m.execute(P.command('prepare',ready(),{},'새 준비',true)));
    await m.execute();assert.equal(m.uncertain,false);assert.equal(new Set(calls.map(c=>c.body)).size,1);assert.equal(new Set(calls.map(c=>c.headers['Idempotency-Key'])).size,1);
});
test('UI shows paged scope IDs and keeps unverified or readonly publication disabled',async()=>{
    const incomplete=harness();await incomplete.app.start();assert.equal(incomplete.q('[data-publish]').disabled,true);incomplete.app.arm('publish');assert.equal(incomplete.calls.some(c=>c.method),false);
    const h=publicationHarness({admin:false});await h.app.start();assert.match(text(h.q('[data-scope]')),/고정 범위 지문/);assert.equal(h.params.scopeId,id(6));
    assert.equal(h.q('[data-prepare]').disabled,true);assert.equal(h.q('[data-publish]').disabled,true);h.app.arm('prepare');h.app.arm('publish');await h.app.submit();assert.equal(h.calls.some(c=>c.method),false);
});
test('UI preparation fixes scope without publishing; next publish has separate unchecked consents',async()=>{
    const h=publicationHarness({write:async()=>scope()});await h.app.start();h.app.arm('prepare');h.ack.checked=true;await h.app.submit();
    assert.equal(h.calls.filter(c=>c.method).length,1);assert.match(h.calls.find(c=>c.method).url,/publication-scopes$/);assert.match(text(h.q('[data-receipt]')),/실제 정책 게시·승인은 아닙니다/);
    h.app.arm('publish');assert.equal(h.ack.checked,false);assert.equal([...h.consents.values()].every(e=>!e.checked),true);
    h.ack.checked=true;h.reason.value='게시';await h.app.submit();assert.equal(h.calls.filter(c=>c.method).length,1);assert.match(h.q('[data-error]').textContent,/세 항목/);
});
test('UI publication only fires after all confirmations and preserves receipt on later read failure',async()=>{
    let posted=false;const h=publicationHarness({write:async()=>{posted=true;return publication();},publicationRead:()=>{throw new P.RequestError('후속 영수증 조회 실패',posted?503:404);}});
    await h.app.start();h.app.arm('publish');h.ack.checked=true;h.reason.value='정책 게시';for(const e of h.consents.values())e.checked=true;await h.app.submit();
    const sent=h.calls.filter(c=>c.method);assert.equal(sent.length,1);assert.equal(sent[0].url,P.base+'/'+id(1)+'/publication');
    assert.deepEqual(JSON.parse(sent[0].body),{reason:'정책 게시',scopeId:id(6),scopeHash:'b'.repeat(64),expectedVersion:0,...consentInput()});
    assert.match(text(h.q('[data-receipt]')),/게시 영수증/);assert.match(h.q('[data-error]').textContent,/후속 영수증 조회 실패/);assert.equal(h.app.mutation.uncertain,false);
});
test('UI expiry between review and submit blocks writes and incomplete scope item read locks actions',async()=>{
    const h=publicationHarness();await h.app.start();h.app.arm('publish');h.ack.checked=true;for(const e of h.consents.values())e.checked=true;
    h.app.state.scope.scope.expiresAt=new Date(Date.now()-1).toISOString();await h.app.submit();assert.equal(h.calls.some(c=>c.method),false);assert.equal(h.q('[data-publish]').disabled,true);
    const failed=publicationHarness({itemsFailure:true});await failed.app.start();assert.equal(failed.q('[data-publish]').disabled,true);assert.match(failed.q('[data-error]').textContent,/항목 조회 실패/);
});
test('historical receipt is readable even when policy is retired; missing and failed reads differ',async()=>{
    const retired=detail({policy:summary({policyStatusCode:'RETIRED',rowVersion:2,policyHash:hash,publishedAt:now}),isEditable:false});
    const h=harness({request:async(url,o,next)=>{if(url===P.base+'/'+id(1))return retired;if(url.endsWith('/publication-impact'))return impact(retired);
        if(url.endsWith('/publication'))return publication({previousPolicyId:id(10),previousPolicyVersion:7});return next(url);}});await h.app.start();
    assert.equal(h.app.state.detail.policy.policyStatusCode,'RETIRED');assert.equal(h.q('[data-publish]').disabled,true);
    assert.match(text(h.q('[data-publication-receipt]')),/퇴역 전 버전 7/);assert.match(text(h.q('[data-publication-receipt]')),/이후 퇴역/);
    const missing=harness();await missing.app.start();assert.match(text(missing.q('[data-publication-receipt]')),/영수증이 없습니다/);assert.equal(missing.q('[data-error]').hidden,true);
});
test('all three modes preserve the reviewed mode in publication requests and receipts',()=>{
    for(const modeCode of ['OFF','COLLECT_ONLY','ENFORCE']){const s=ready();s.detail.policy.modeCode=modeCode;s.impact.policy.modeCode=modeCode;s.scope.scope.modeCode=modeCode;
        const c=P.command('publish',s,consentInput(),'모드 확인',true);assert.equal(c.modeCode,modeCode);assert.equal(P.receipt(publication({modeCode}),c),true);assert.equal(Object.hasOwn(c.payload,'modeCode'),false);}
});
test('scope item pagination preserves the exact selected scope and rejects omitted total members',async()=>{
    const selected=scope({scope:scopeSummary({itemCount:21})});
    const h=harness({nav:{policyId:id(1),scopeId:id(6),scopeItemPage:2},request:async(url,o,next)=>{
        if(url.includes('/publication-scopes?'))return paged([selected.scope]);if(url.endsWith('/publication-scopes/'+id(6)))return selected;
        if(url.includes('/items?'))return paged([{entityTypeCode:'SOURCE',entityId:id(42),stateHash:hash}],2,20,21);return next(url);}});
    await h.app.start();assert.match(h.calls.find(c=>c.url.includes('/items?')).url,/publication-scopes\/00000000-0000-0000-0000-000000000006\/items\?page=2&size=20/);
    assert.match(text(h.q('[data-scope-item-pages]')),/21건 · 2\/2페이지/);assert.match(text(h.q('[data-scope-items]')),new RegExp(id(42)));
    const broken=publicationHarness({scopeOverride:selected});await broken.app.start();assert.equal(broken.q('[data-publish]').disabled,true);assert.equal(broken.q('[data-error]').hidden,false);
});
test('QA reserve selects actual run and cancel uses run version not policy version',async()=>{
    let reserved=false,cancelled=false;const h=harness({request:async(url,o,next)=>{
        if(o.method==='POST'){reserved=true;return run({statusCode:'PENDING',rowVersion:2});}
        if(o.method==='PUT'){cancelled=true;return run({statusCode:'CANCEL_REQUESTED',rowVersion:4});}
        if(url.includes('/validation-runs/')||url.includes('/validation-runs?')){const r=run({statusCode:cancelled?'CANCEL_REQUESTED':reserved?'RUNNING':'INCOMPLETE',rowVersion:cancelled?4:reserved?3:0});return url.includes('?')?paged([r]):r;}return next(url);
    }});await h.app.start();h.app.arm('qa');h.ack.checked=true;await h.app.submit();assert.equal(h.params.runId,id(3));
    h.reason.value='QA 중지';h.app.arm('cancel');h.ack.checked=true;await h.app.submit();assert.equal(JSON.parse(h.calls.find(c=>c.method==='PUT').body).expectedVersion,3);
    assert.match(text(h.q('[data-receipt]')),/현재 파일 정리 대기/);assert.equal(h.q('[data-cancel-qa]').disabled,true);
});
test('lost update and 409 replay remain uncertain; explicit current snapshot reconciliation preserves input',async()=>{
    let changed=false;const h=harness({request:async(url,o,next)=>{
        if(o.method==='PUT'){const status=changed?409:0;changed=true;throw new P.RequestError('미확정',status);}
        if(changed&&url===P.base+'/'+id(1))return detail({policy:summary({rowVersion:1})});return next(url);
    }});await h.app.start();h.fields.get('maximumSourceBytes').value='1000';await h.q('[data-editor]').fire('input');h.app.arm('save');h.ack.checked=true;await h.app.submit();await h.q('[data-retry]').fire('click');
    assert.equal(h.app.mutation.uncertain,true);assert.equal(h.q('[data-save]').disabled,true);assert.equal(h.fields.get('maximumSourceBytes').value,'1000');
    await h.app.inspect();h.q('[data-reconcile-ack]').checked=true;h.app.reconcile();assert.equal(h.app.mutation.uncertain,false);
    assert.equal(h.app.state.detail.policy.rowVersion,1);assert.equal(h.fields.get('maximumSourceBytes').value,'1000');assert.match(h.q('[data-status]').textContent,/성공 여부는 미확정/);
    assert.equal(h.q('[data-save]').disabled,true);assert.equal(h.app.dirty,true);
});
test('failed reread clears dependent QA and impact; old success cannot enable actions',async()=>{
    let fail=false;const h=harness({request:async(url,o,next)=>{if(fail&&url===P.base+'/'+id(1))throw new P.RequestError('정책 조회 실패',503);return next(url);}});
    await h.app.start();fail=true;await h.q('[data-refresh]').fire('click');assert.equal(h.app.state.run,null);assert.equal(h.app.state.impact,null);assert.equal(h.q('[data-save]').disabled,true);assert.equal(text(h.q('[data-impact]')).trim(),'');
});
test('partial impact failure does not hide readable QA or invent empty impact success',async()=>{
    const h=harness({request:async(url,o,next)=>{if(url.endsWith('/publication-impact'))throw new P.RequestError('영향 조회 실패',503);return next(url);}});
    await h.app.start();assert.match(text(h.q('[data-run]')),/증거 부족/);assert.match(h.q('[data-error]').textContent,/영향 조회 실패/);assert.equal(h.q('[data-qa]').disabled,true);
});
test('save receipt survives following detail failure; input remains on initial 409',async()=>{
    let saved=false;const h=harness({request:async(url,o,next)=>{if(o.method==='PUT'){saved=true;return detail({policy:summary({rowVersion:1})});}if(saved&&url===P.base+'/'+id(1))throw new P.RequestError('후속 조회 실패',503);return next(url);}});
    await h.app.start();h.app.arm('save');h.ack.checked=true;await h.app.submit();assert.match(text(h.q('[data-receipt]')),/조회 버전 1/);assert.equal(h.app.mutation.uncertain,false);
    const c=harness({request:async(url,o,next)=>{if(o.method)throw new P.RequestError('다른 관리자가 수정했습니다. 최신 버전을 확인하세요.',409);return next(url);}});await c.app.start();c.fields.get('maximumSourceBytes').value='100';await c.q('[data-editor]').fire('input');c.app.arm('save');c.ack.checked=true;await c.app.submit();
    assert.equal(c.fields.get('maximumSourceBytes').value,'100');assert.equal(c.reason.value,'업무 확인');assert.equal(c.app.mutation.uncertain,false);assert.equal(c.q('[data-save]').disabled,true);
});
test('dirty refresh keeps editor values while adopting current server version; discard is explicit',async()=>{
    const h=harness({confirm:false});await h.app.start();h.fields.get('maximumSourceBytes').value='100';await h.q('[data-editor]').fire('input');await h.q('[data-refresh]').fire('click');
    assert.equal(h.fields.get('maximumSourceBytes').value,'100');h.app.reset(true);assert.equal(h.app.state.detail.policy.policyId,id(1));assert.equal(h.reason.value,'업무 확인');
});
test('HTML-like labels and evidence are only text; unknown evidence metadata not displayed',async()=>{
    const h=harness({request:async(url,o,next)=>{if(url.includes('/validation-runs?'))return paged([run({steps:steps().map(s=>({...s,evidence:{reasonCode:'<img onerror=alert(1)>',url:'https://private.test',secret:'not-for-ui'}}))})]);return next(url);}});
    await h.app.start();assert.doesNotMatch(text(h.q('[data-run]')),/onerror|private\.test|not-for-ui/);
    for(const name of ['saneb-attachment-policy-core.js','saneb-attachment-policies.js']){const js=readFileSync(new URL('../../src/main/resources/static/js/'+name,import.meta.url),'utf8');assert.doesNotMatch(js,/innerHTML|outerHTML|insertAdjacentHTML|localStorage|sessionStorage|eval\(/);}
    const html=readFileSync(new URL('../../src/main/resources/templates/app/announcement-attachment-policies.html',import.meta.url),'utf8');assert.doesNotMatch(html,/th:utext|<script(?![^>]*src)/);assert.match(html,/name="acknowledged" type="checkbox" required/);assert.doesNotMatch(html,/acknowledged[^>]*checked/);
});
