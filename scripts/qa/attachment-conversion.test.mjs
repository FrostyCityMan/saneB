import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createRequire} from 'node:module';
import {runInNewContext} from 'node:vm';
const require=createRequire(import.meta.url), core=require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const script=readFileSync(new URL('../../src/main/resources/static/js/saneb-announcement-conversion.js',import.meta.url),'utf8');
const html=readFileSync(new URL('../../src/main/resources/templates/app/announcement-conversion-review.html',import.meta.url),'utf8');
const id='11111111-1111-4111-8111-111111111111', base='22222222-2222-4222-8222-222222222222';
class El {
    constructor(tag='div'){this.tag=tag;this.children=[];this.value='';this.checked=false;this.hidden=false;this.disabled=false;this.textContent='';this.events={};}
    append(...items){this.children.push(...items);}
    replaceChildren(...items){this.children=items;}
    addEventListener(name,fn){this.events[name]=fn;}
    focus(){this.focused=true;}
    reportValidity(){return true;}
    querySelectorAll(selector){const name=/name="([^"]+)"/.exec(selector)?.[1];return this.all().filter(el=>el.name===name&&(!selector.endsWith(':checked')||el.checked));}
    all(){return this.children.flatMap(el=>typeof el==='object'?[el,...el.all()]:[]);}
}
async function harness(opts={}) {
    const nodes=new Map(),calls=[], pending=[];let confirmed=!!opts.saved,linked=!!opts.linked,confirmCount=0,draftCount=0;
    const q=s=>{if(!nodes.has(s))nodes.set(s,new El());return nodes.get(s);};
    const form=q('[data-form]');
    form.elements=Object.fromEntries(['primaryTargetCategoryCode','incomeJudgementCode','reviewNote','reviewMethodCode','acknowledged'].map(n=>[n,new El()]));
    form.elements.reviewMethodCode.value='EXTRACTED_TEXT';
    form.append(q('[data-targets]'),q('[data-supports]'),q('[data-acknowledgements]'));
    const version={expectedBaseDecisionId:base,expectedAttachmentDecisionId:id,expectedSourceVersion:1,expectedAttachmentVersion:2,expectedSetHash:'a'.repeat(64)};
    const source={sourceId:id,title:'검증 공고',publicCode:'SRC-TEST',providerCode:'LOCAL_GOV_NOTICE',isAttachmentReviewRequired:true,sourceVersion:1,attachmentVersion:2,
        processingFlow:{statusCode:opts.blocked?'RUNNING':'READY_FOR_FINAL_REVIEW',isFinalReviewAvailable:!opts.blocked},attachmentSummary:{jobStatusCode:opts.blocked?'RUNNING':'SUCCEEDED'},
        baseClassification:{decisionId:base},effectiveClassification:{decisionId:id,setId:id,setHash:'a'.repeat(64),targetCategoryCodes:['PERSONAL'],supportTypeCodes:['GENERAL_SUPPORT']}};
    const context=()=>({sourceId:id,version:{...version},requiredAcknowledgementCodes:opts.manual?['DISCOVERY_FAILED']:[],manualSourceCheckRequired:!!opts.manual,
        linkedAnnouncement:linked?{announcementId:id,announcementCode:'ANN-TEST'}:null,confirmedClassification:confirmed?{targetCategoryCodes:['PERSONAL'],supportTypeCodes:['GENERAL_SUPPORT'],confirmation:{confirmationId:id,sourceId:id,evaluationId:id,isCurrent:true,sourceVersion:1,attachmentVersion:2,setHash:'a'.repeat(64)}}:null});
    const request=async(url,options={})=>{
        calls.push({url,options});
        if(options.method==='POST') {
            if(url.endsWith('/confirmations')) {confirmCount++;if(opts.confirmFail&&confirmCount===1)throw new core.RequestError('검수 응답 유실');confirmed=true;return {confirmationId:id};}
            draftCount++;if(opts.draftFail&&draftCount===1)throw new core.RequestError('초안 응답 유실');
            if(opts.draftConflict&&draftCount===1)throw new core.RequestError('중복 확인 필요',409);
            linked=true;return {announcementId:id,announcementCode:'ANN-TEST'};
        }
        if(opts.lookupFail)throw new core.RequestError('자료 조회 실패');
        if(url.endsWith('/review-context')) {const c=context();if(opts.stale&&confirmed)c.version.expectedSourceVersion++;return c;}
        if(url.endsWith(`/${id}`))return {source,content:{bodyText:'확인할 본문',sourceUrl:'javascript:alert(1)'}};
        return {items:[],page:1,totalPages:0,totalCount:0};
    };
    const page={dataset:{sourceId:id,canManage:String(opts.manage!==false)},querySelector:q};
    runInNewContext(script,{window:{SanebAttachmentReview:{...core,client:()=>request,mutation:()=>core.mutation(()=>id)},fetch(){},addEventListener(){}},document:{querySelector:()=>page,createElement:tag=>new El(tag),createTextNode:t=>t}});
    const settle=async()=>{for(let i=0;i<35;i++)await Promise.resolve();};await settle();
    const fill=()=>{form.elements.primaryTargetCategoryCode.value='PERSONAL';form.elements.incomeJudgementCode.value='NO_LIMIT';form.elements.reviewNote.value='원문 확인';form.elements.acknowledged.checked=true;};
    const submit=async()=>{fill();form.events.submit({preventDefault(){}});await settle();};
    return {q,form,calls,submit,settle,fill,counts:()=>({confirmCount,draftCount})};
}
test('focused shell has one conversion action and no operational history controls',()=>{
    assert.doesNotMatch(html,/data-operation|data-recovery|data-load-history|data-segments/);
    assert.match(html,/data-acknowledgements/);assert.match(html,/자동 활성화하지 않습니다/);
    assert.doesNotMatch(script,/innerHTML|localStorage|sessionStorage|\/publication|\/attachment-jobs/);
});
test('confirmation then draft uses saved receipt and immutable version',async()=>{
    const h=await harness();await h.submit();assert.deepEqual(h.counts(),{confirmCount:1,draftCount:1});
    const writes=h.calls.filter(c=>c.options.method==='POST');assert.match(writes[0].url,/confirmations$/);assert.match(writes[1].url,/announcements$/);
    assert.equal(JSON.parse(writes[1].options.body).expectedConfirmationId,id);assert.ok(writes[0].options.headers['Idempotency-Key']);assert.equal(h.q('[data-convert]').disabled,true);
});
test('saved confirmation bypasses new confirmation and only creates draft',async()=>{const h=await harness({saved:true});await h.submit();assert.deepEqual(h.counts(),{confirmCount:0,draftCount:1});});
test('draft response loss retries only exact draft body',async()=>{
    const h=await harness({draftFail:true});await h.submit();assert.equal(h.q('[data-retry]').hidden,false);h.q('[data-retry]').events.click();await h.settle();
    assert.deepEqual(h.counts(),{confirmCount:1,draftCount:2});const writes=h.calls.filter(c=>c.url.endsWith('/announcements'));assert.equal(writes[0].options.body,writes[1].options.body);
});
test('confirmation response loss preserves original key and then continues once',async()=>{
    const h=await harness({confirmFail:true});await h.submit();assert.equal(h.counts().draftCount,0);h.q('[data-retry]').events.click();await h.settle();
    assert.deepEqual(h.counts(),{confirmCount:2,draftCount:1});const writes=h.calls.filter(c=>c.url.endsWith('/confirmations'));assert.deepEqual(writes[0].options,writes[1].options);
});
test('draft conflict preserves stored review and retries only draft after refresh',async()=>{
    const h=await harness({draftConflict:true});await h.submit();assert.equal(h.q('[data-convert]').disabled,true);
    h.q('[data-refresh]').events.click();await h.settle();assert.equal(h.form.elements.reviewNote.value,'원문 확인');await h.submit();assert.deepEqual(h.counts(),{confirmCount:1,draftCount:2});
});
test('version change after confirmation prevents draft',async()=>{const h=await harness({stale:true});await h.submit();assert.deepEqual(h.counts(),{confirmCount:1,draftCount:0});assert.match(h.q('[data-error]').textContent,/최신 기준이 변경/);});
test('running sources, read-only roles, linked records and failed lookup cannot write',async()=>{
    for(const options of [{blocked:true},{manage:false},{linked:true},{lookupFail:true}]){const h=await harness(options);await h.submit();assert.deepEqual(h.counts(),{confirmCount:0,draftCount:0});assert.equal(h.q('[data-convert]').disabled,true);}
});
test('manual requirements remain mandatory',async()=>{
    const h=await harness({manual:true});await h.submit();assert.match(h.q('[data-error]').textContent,/모두 확인/);assert.equal(h.counts().confirmCount,0);
    h.form.querySelectorAll('input[name="acknowledgedErrorCodes"]')[0].checked=true;h.form.elements.reviewMethodCode.value='EXTRACTED_TEXT';await h.submit();assert.match(h.q('[data-error]').textContent,/원문 전체/);
    h.form.elements.reviewMethodCode.value='MANUAL_SOURCE_CHECK';await h.submit();assert.equal(h.counts().draftCount,1);
});
test('editing saved categories invalidates saved shortcut',async()=>{
    const h=await harness({saved:true});h.form.events.input({target:{name:'targetCategoryCodes'}});await h.submit();assert.equal(h.counts().confirmCount,1);
});
test('source javascript URL never rendered as a link',async()=>{const h=await harness();assert.ok(!h.q('[data-body]').children.some(el=>el.tag==='a'));});
test('double submit is locked and result links to the exact draft',async()=>{
    const h=await harness();h.fill();h.form.events.submit({preventDefault(){}});h.form.events.submit({preventDefault(){}});await h.settle();
    assert.deepEqual(h.counts(),{confirmCount:1,draftCount:1});assert.equal(h.q('[data-result]').children.find(el=>el.tag==='a').href,`/app/announcements/input?announcementId=${id}`);
});
test('announcement input deep link validates UUID and only loads details',async()=>{
    const input=readFileSync(new URL('../../src/main/resources/static/js/saneb-announcement-input.js',import.meta.url),'utf8');
    const start=input.lastIndexOf('loadStandardDocumentFields().then');
    const end=input.indexOf('    loadAnnouncementList();',start);
    const snippet=input.slice(start,end);
    for(const [value,expected] of [[id,1],['javascript:alert(1)',0],['',0]]) {
        const loaded=[],messages=[];
        runInNewContext(snippet,{loadStandardDocumentFields:async()=>{},loadDetails:async v=>loaded.push(v),setMessage:m=>messages.push(m),window:{location:{href:`https://example.test/app/announcements/input?announcementId=${encodeURIComponent(value)}`}},URL});
        for(let i=0;i<5;i++)await Promise.resolve();
        assert.equal(loaded.length,expected);if(expected)assert.equal(loaded[0],id);if(value&& !expected)assert.equal(messages.length,1);
    }
});
