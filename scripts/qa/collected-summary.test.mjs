import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {runInNewContext} from 'node:vm';
const script=await readFile(new URL('../../src/main/resources/static/js/saneb-collected-summary.js',import.meta.url),'utf8');
const template=await readFile(new URL('../../src/main/resources/templates/app/collected-announcements.html',import.meta.url),'utf8');
class Element {
    constructor(){this.textContent='';this.children=[];this.events={};this.hidden=false;}
    appendChild(el){this.children.push(el);return el;}
    replaceChildren(...els){this.children=els;}
    addEventListener(name,fn){this.events[name]=fn;}
    setAttribute(name,value){this[name]=value;}
    removeAttribute(name){delete this[name];}
    focus(){this.focused=true;}
    click(){this.events.click?.();}
}
const fixture=()=>({uniqueExcludedCount:2,unknownPurposeCount:1,countingBasis:'PROVIDER_IDENTITY_UNIQUE',scope:'RETAINED_PRODUCTION_BY_LATEST_RUN',asOf:'2026-10-12T00:00:00Z',
    byProvider:[{code:'BIZINFO',count:2},{code:'GOV24_PUBLIC_SERVICE',count:0},{code:'LOCAL_GOV_NOTICE',count:0}],
    byLatestReason:[{code:'TITLE_GROUP_B_MATCHED',count:1},{code:'TITLE_COMBINATION_NOT_MATCHED',count:0},{code:'OTHER_LEGACY',count:1}]});
const ok=data=>({ok:true,status:200,json:async()=>({success:true,data})});
const settle=async()=>{for(let i=0;i<8;i++)await new Promise(r=>setImmediate(r));};
async function harness(handler, legacy=false){
    const nodes=new Map(),events={},calls=[],timers=[];
    const q=key=>{if(!nodes.has(key))nodes.set(key,new Element());return nodes.get(key);};
    const page={dataset:{sourceUrl:'/api/v1/admin/announcement-sources'},querySelector:q};
    const win={history:{state:legacy?{sanebCollectedList:{view:'EXCLUDED'}}:null},location:{search:''},addEventListener:(key,fn)=>events[key]=fn};
    const fetch=async(url,options)=>{calls.push(url);return handler?handler(url,options):ok(url.includes('exclusions')?fixture():{totalCount:5});};
    runInNewContext(script,{window:win,document:{readyState:'interactive',querySelector:()=>page,createElement:()=>new Element()},
        fetch,AbortController,URLSearchParams,Intl,setTimeout:fn=>{timers.push(fn);return timers.length;},clearTimeout:()=>{}});
    events.pageshow();await settle();
    return {q,events,calls,timers,win};
}
test('new card is distinct cumulative history, has no excluded list tab and uses shared page',()=>{
    assert.ok(template.includes('자동 제외 이력'));
    assert.ok(!template.includes('data-collected-view="EXCLUDED"'));
    assert.ok(template.includes('/js/saneb-collected-summary.js'));
    assert.ok(template.includes('누적 기록이므로 현재 공고 수와 합산하지 않습니다'));
});
test('production unique count, unknown purpose and finite breakdown are rendered',async()=>{
    const h=await harness();assert.equal(h.q('[data-collected-excluded-count]').textContent,'2건');
    assert.match(h.q('[data-exclusion-scope-note]').textContent,/미확인 1건/);
    assert.equal(h.q('[data-exclusion-breakdown]').children.length,4);
    assert.equal(h.calls.length,3);assert.ok(!h.calls.some(u=>u.includes('semanticStatusCode=EXCLUDED')));
    h.q('[data-exclusion-open]').click();assert.equal(h.q('[data-exclusion-summary]').open,true);
    assert.equal(h.q('[data-exclusion-heading]').focused,true);
});
test('real zero is shown only after valid response',async()=>{
    const data=fixture();data.uniqueExcludedCount=0;data.byProvider.forEach(i=>i.count=0);data.byLatestReason.forEach(i=>i.count=0);
    const h=await harness(url=>ok(url.includes('exclusions')?data:{totalCount:0}));
    assert.equal(h.q('[data-collected-excluded-count]').textContent,'0건');
});
for(const status of [401,403,500])test(`HTTP ${status} is not zero and does not break other cards`,async()=>{
    const h=await harness(url=>url.includes('exclusions')?{ok:false,status}:ok({totalCount:7}));
    assert.equal(h.q('[data-collected-excluded-count]').textContent,'집계 불가');
    assert.equal(h.q('[data-collected-action-count]').textContent,'7건');
    assert.equal(h.q('[data-summary-retry="excluded"]').hidden,false);
    assert.match(h.q('[data-exclusion-asof]').textContent,status===401?/로그인/:status===403?/권한/:/조회하지 못/);
});
for(const bad of ['sum','duplicate','missing','negative','scope','time'])test(`malformed ${bad} never becomes zero`,async()=>{
    const data=fixture();
    if(bad==='sum')data.uniqueExcludedCount=3;
    if(bad==='duplicate')data.byProvider[1].code='BIZINFO';
    if(bad==='missing')delete data.uniqueExcludedCount;
    if(bad==='negative')data.unknownPurposeCount=-1;
    if(bad==='scope')data.scope='ALL';
    if(bad==='time')data.asOf='invalid';
    const h=await harness(url=>ok(url.includes('exclusions')?data:{totalCount:1}));
    assert.equal(h.q('[data-collected-excluded-count]').textContent,'집계 불가');
    assert.equal(h.q('[data-exclusion-breakdown]').children.length,0);
});
test('retry can recover and old excluded state opens history without raw source query',async()=>{
    let failed=true;const h=await harness(url=>url.includes('exclusions')?(failed?{ok:false,status:500}:ok(fixture())):ok({totalCount:4}),true);
    assert.equal(h.q('[data-exclusion-summary]').open,true);failed=false;h.q('[data-summary-retry="excluded"]').click();await settle();
    assert.equal(h.q('[data-collected-excluded-count]').textContent,'2건');
});
test('late response after navigation cannot replace refreshed count',async()=>{
    let resolve;let first=true;const h=await harness(url=>url.includes('exclusions')&&first?(first=false,new Promise(r=>resolve=r)):ok(url.includes('exclusions')?fixture():{totalCount:1}));
    h.events.pagehide();h.events.pageshow();await settle();resolve({ok:false,status:500});await settle();
    assert.equal(h.q('[data-collected-excluded-count]').textContent,'2건');
});
test('timeout has retry and never displays false zero',async()=>{
    const h=await harness((url,options)=>url.includes('exclusions')?new Promise((_,reject)=>options.signal.addEventListener('abort',()=>reject(Object.assign(new Error(),{name:'AbortError'})))):ok({totalCount:1}));
    h.timers.at(-1)();await settle();assert.match(h.q('[data-exclusion-asof]').textContent,/초과/);
    assert.equal(h.q('[data-collected-excluded-count]').textContent,'집계 불가');
});
test('network failures offer Korean recovery guidance',async()=>{
    const h=await harness(url=>{if(url.includes('exclusions'))throw new TypeError('Failed to fetch');return ok({totalCount:1});});
    assert.match(h.q('[data-exclusion-asof]').textContent,/네트워크 연결/);
    assert.equal(h.q('[data-collected-excluded-count]').textContent,'집계 불가');
});
