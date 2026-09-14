import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const P=require('../../src/main/resources/static/js/saneb-attachment-policy-core.js');
const UI=require('../../src/main/resources/static/js/saneb-attachment-provider-coverage.js');
const id='00000000-0000-0000-0000-000000000001', hash='a'.repeat(64), formats=['HWP','HWPX','PDF'];
const target=(n=0,extra={})=>({targetKey:`LOCAL_GOV_NOTICE:LGS-${String(n).padStart(6,'0')}`,bindingStatusCode:'PROFILE_MISSING',
    referenceCount:0,executableCount:0,normalNoticeCount:0,requiredNormalNoticeCount:3,missingFormats:[],isExpectationCoverageComplete:false,
    formatApplicability:{statusCode:'EXPECTATIONS_UNKNOWN',expectedProvidedFormats:[],unobservedFormats:formats,normalMultiFileNoticeCount:0},...extra});
const ready=(extra={})=>target(1,{bindingStatusCode:'SYSTEM_BINDING_MATCHED',referenceCount:3,executableCount:3,normalNoticeCount:3,isExpectationCoverageComplete:true,
    formatApplicability:{statusCode:'FIXED_SAMPLE_EXPECTATIONS',expectedProvidedFormats:['HWPX'],unobservedFormats:['HWP','PDF'],normalMultiFileNoticeCount:1},...extra});
const coverage=(page=1,total=225)=>({policyId:id,policyVersion:0,snapshotHash:hash,catalogHash:hash,planHash:hash,
    isExpectationCoverageComplete:false,isQaPassed:false,formatCoverage:{modeCode:'FIXED_SAMPLE_FORMATS_V2',requiredFormats:formats,missingFormats:formats},
    targets:{page,size:20,totalCount:total,totalPages:Math.ceil(total/20),items:Array.from({length:Math.min(20,Math.max(0,total-(page-1)*20))},(_,i)=>target((page-1)*20+i))}});
class Element {
    constructor(tag='div'){this.tag=tag;this.children=[];this.events={};this.hidden=false;this.disabled=false;this.textContent='';}
    append(e){this.children.push(e);} replaceChildren(...e){this.children=e;this.textContent='';} setAttribute(k,v){this[k]=v;}
    focus(){this.focused=true;} addEventListener(k,v){this.events[k]=v;} fire(k){return this.events[k]?.();}
}
const text=e=>[e.textContent,...e.children.map(text)].join(' ');
function harness({search=`?policyId=${id}`,request:custom}={}){
    const nodes=new Map(),q=s=>{if(!nodes.has(s))nodes.set(s,new Element());return nodes.get(s);},calls=[],urls=[];
    const app=UI.mount({page:{querySelector:q,setAttribute(){}},P,doc:{createElement:t=>new Element(t)},
        navigation:{search:()=>search,replace:params=>urls.push({...params})},
        request:async(url,options)=>{calls.push({url,...options});return custom?custom(url,options):coverage(Number(new URL(url,'http://localhost').searchParams.get('page')));}});
    return {app,q,calls,urls};
}
test('all targets and unobserved formats remain visible without QA or write claims',async()=>{
    const h=harness();await h.app.start();assert.equal(h.calls.length,1);assert.equal(h.calls[0].method,'GET');
    assert.match(text(h.q('[data-summary]')),/225곳 · 모든 페이지 포함/);assert.match(text(h.q('[data-summary]')),/실제 QA 통과 여부 이 조회는 통과 근거가 아닙니다/);
    assert.match(text(h.q('[data-targets]')),/기관 미지원이나 검증 면제가 아님/);assert.match(text(h.q('[data-targets]')),/요구 형식 미확정/);
    assert.equal(h.q('[data-next]').disabled,false);assert.equal(h.q('[data-prev]').disabled,true);
    assert.equal(h.q('[data-policy-link]').href,`/app/admin/announcement-attachment-policies?policyId=${id}`);
});
test('valid fixed sample scope preserves per-provider missing failure format',()=>{
    assert.equal(UI.target(ready(),P),true);
    const partial=ready({normalNoticeCount:2,missingFormats:['PDF'],isExpectationCoverageComplete:false,
        formatApplicability:{statusCode:'FIXED_SAMPLE_EXPECTATIONS',expectedProvidedFormats:['HWPX','PDF'],unobservedFormats:['HWP'],normalMultiFileNoticeCount:1}});
    assert.equal(UI.target(partial,P),true);
});
for(const [name,change] of Object.entries({qaPassed:d=>d.isQaPassed=true,wrongPolicy:d=>d.policyId=id.replace(/1$/,'2'),wrongPage:d=>d.targets.page=2,
    count:d=>d.targets.totalCount=1,duplicates:d=>d.targets.items[1]=d.targets.items[0],falseGlobalReady:d=>d.isExpectationCoverageComplete=true,
    badHash:d=>d.planHash='bad',oldSchema:d=>d.formatCoverage=null,extraFormat:d=>d.formatCoverage.requiredFormats.push('DOCX')})){
    test(`reject ${name} response instead of displaying stale or false success`,async()=>{
        const d=structuredClone(coverage());change(d);const h=harness({request:async()=>d});await h.app.start();
        assert.equal(h.app.state.failed,true);assert.equal(h.app.state.data,null);assert.match(h.q('[data-error]').textContent,/현재 계약과 다릅니다/);
        assert.equal(h.q('[data-targets]').children.length,0);assert.equal(h.q('[data-next]').disabled,true);
    });
}
for(const [name,change] of Object.entries({falseReady:t=>t.isExpectationCoverageComplete=true,unknownBinding:t=>t.bindingStatusCode='READY',
    excessNormal:t=>t.normalNoticeCount=1,falseUnobserved:t=>t.formatApplicability.unobservedFormats=['PDF'],
    duplicateFormat:t=>t.formatApplicability.unobservedFormats=['PDF','PDF','HWP'],missingOutsideProvided:t=>t.missingFormats=['PDF'],
    unknownCounts:t=>t.formatApplicability.normalMultiFileNoticeCount=1,tooManyExecutable:t=>t.executableCount=1,
    wrongStatus:t=>t.formatApplicability.statusCode='FIXED_SAMPLE_EXPECTATIONS',badKey:t=>t.targetKey='<img src=x onerror=alert(1)>'})){
    test(`target contract rejects ${name}`,()=>{const t=structuredClone(target());change(t);assert.equal(UI.target(t,P),false);});
}
for(const search of ['', '?policyId=bad',`?policyId=${id}&policyId=${id}`,`?policyId=${id}&page=0`,`?policyId=${id}&page=-1`,
    `?policyId=${id}&page=1.5`,`?policyId=${id}&page=2147483647`,`?policyId=${id}&filePath=etc`,`?policyId=${id}&page=1&page=2`]){
    test(`invalid navigation makes no request: ${search}`,async()=>{const h=harness({search});await h.app.start();assert.equal(h.calls.length,0);assert.equal(h.q('[data-refresh]').disabled,true);});
}
test('paging preserves policy, exact denominator and focus; refresh resets to first page',async()=>{
    const h=harness();await h.app.start();await h.q('[data-next]').fire('click');assert.equal(h.app.state.page,2);
    assert.match(h.calls[1].url,/page=2&size=20$/);assert.equal(h.q('#coverage-targets').focused,true);
    await h.q('[data-prev]').fire('click');assert.equal(h.app.state.page,1);
    await h.q('[data-next]').fire('click');await h.q('[data-refresh]').fire('click');assert.equal(h.app.state.page,1);
    assert.ok(h.calls.every(c=>c.method==='GET'&&!c.body));
});
for(const [name,change] of Object.entries({plan:d=>d.planHash='b'.repeat(64),snapshot:d=>d.snapshotHash='b'.repeat(64),
    version:d=>d.policyVersion=1,catalog:d=>d.catalogHash='b'.repeat(64),scope:d=>{d.targets.totalCount=224;d.targets.totalPages=12;}})){
    test(`changed ${name} between pages requires explicit fresh plan`,async()=>{
        let changed=false;const h=harness({request:async url=>{const d=coverage(Number(new URL(url,'http://localhost').searchParams.get('page')));if(changed)change(d);return d;}});
        await h.app.start();changed=true;await h.q('[data-next]').fire('click');assert.equal(h.app.state.failed,true);
        assert.match(h.q('[data-error]').textContent,/계획이 변경/);assert.equal(h.q('[data-targets]').children.length,0);
        await h.q('[data-refresh]').fire('click');assert.equal(h.app.state.failed,false);assert.equal(h.app.state.page,1);
    });
}
for(const [status,message] of [[401,/로그인이 만료/],[403,/권한이 없습니다/],[409,/Linux 격리 추출기/],[404,/정책을 찾을 수 없습니다/],[500,/서버 연결/],[0,/서버 연결/]]){
    test(`read failure ${status} preserves policy address, no raw message and no automatic retry`,async()=>{
        let fail=false;const h=harness({request:async()=>{if(fail)throw new P.RequestError('UNTRUSTED_SENSITIVE_RESPONSE',status);return coverage();}});
        await h.app.start();fail=true;await h.q('[data-next]').fire('click');assert.equal(h.calls.length,2);
        assert.equal(h.app.state.policyId,id);assert.match(h.q('[data-error]').textContent,message);assert.doesNotMatch(h.q('[data-error]').textContent,/UNTRUSTED/);
        assert.equal(h.q('[data-error]').focused,true);assert.equal(h.q('[data-targets]').children.length,0);assert.equal(h.q('[data-refresh]').disabled,false);
    });
}
test('busy duplicate actions do not send parallel queries',async()=>{
    let release;const h=harness({request:()=>new Promise(r=>release=r)});const started=h.app.start();
    await h.q('[data-refresh]').fire('click');await h.q('[data-next]').fire('click');assert.equal(h.calls.length,1);
    release(coverage());await started;assert.equal(h.app.state.busy,false);
});
test('empty or out-of-range page is not interpreted as scope QA success',async()=>{
    const h=harness({search:`?policyId=${id}&page=13`,request:async()=>coverage(13)});await h.app.start();
    assert.match(text(h.q('[data-targets]')),/이 페이지에는 대상이 없습니다/);assert.equal(h.q('[data-prev]').disabled,false);
    assert.equal(h.q('[data-next]').disabled,true);assert.match(text(h.q('[data-summary]')),/통과 근거가 아닙니다/);
});
test('ready expectations still explicitly say actual QA is separate',async()=>{
    const d=coverage(1,1);d.targets.items=[ready()];d.formatCoverage.missingFormats=[];d.isExpectationCoverageComplete=true;
    const h=harness({request:async()=>d});await h.app.start();assert.equal(h.app.state.failed,false);
    assert.match(text(h.q('[data-summary]')),/기대값 준비됨 · 실제 검증은 별도/);assert.match(text(h.q('[data-targets]')),/기대값 준비됨 · 실제 QA 전/);
});
test('read client keeps same-origin no-store behavior and refuses external paths',async()=>{
    let call;const client=P.client(async(url,options)=>{call={url,options};return {ok:true,json:async()=>({success:true,data:coverage()})};});
    await client(`${P.base}/${id}/provider-qa-runs/execution-plan/targets?page=1&size=20`,{method:'GET'});
    assert.equal(call.options.credentials,'same-origin');assert.equal(call.options.cache,'no-store');assert.equal(call.options.redirect,'error');
    await assert.rejects(client('https://example.com/x'),/허용되지 않은/);
});
