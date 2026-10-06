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
    setAttribute(name,value){this[name]=value;}
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
        processingFlow:{statusCode:opts.statusCode||(opts.blocked?'RUNNING':'READY_FOR_FINAL_REVIEW'),isFinalReviewAvailable:!opts.blocked},attachmentSummary:{jobStatusCode:opts.blocked?'RUNNING':'SUCCEEDED'},
        baseClassification:{decisionId:base},effectiveClassification:{decisionId:id,setId:opts.noSet?null:id,setHash:'a'.repeat(64),targetCategoryCodes:['PERSONAL'],supportTypeCodes:['GENERAL_SUPPORT']}};
    const context=()=>({sourceId:id,version:{...version},requiredAcknowledgementCodes:opts.manual?['DISCOVERY_FAILED']:[],manualSourceCheckRequired:!!opts.manual,
        linkedAnnouncement:linked?{announcementId:id,announcementCode:'ANN-TEST'}:null,confirmedClassification:confirmed?{targetCategoryCodes:['PERSONAL'],supportTypeCodes:['GENERAL_SUPPORT'],confirmation:{confirmationId:id,sourceId:id,evaluationId:id,isCurrent:true,sourceVersion:1,attachmentVersion:2,setHash:'a'.repeat(64)}}:null});
    const request=async(url,options={})=>{
        calls.push({url,options});
        if(opts.read && options.method!=='POST') {const result=await opts.read(url);if(result!==undefined)return result;}
        if(options.method==='POST') {
            if(url.endsWith('/confirmations')) {confirmCount++;if(opts.confirmFail&&confirmCount===1)throw new core.RequestError('검수 응답 유실');confirmed=true;return {confirmationId:id};}
            draftCount++;if(opts.draftFail&&draftCount===1)throw new core.RequestError('초안 응답 유실');
            if(opts.draftConflict&&draftCount===1)throw new core.RequestError('중복 확인 필요',409);
            linked=true;return {announcementId:id,announcementCode:'ANN-TEST'};
        }
        if(opts.lookupFail)throw new core.RequestError('자료 조회 실패');
        if(url.endsWith('/conversion-context'))return {modeCode:opts.base?'BASE_REVIEW':'ATTACHMENT_REVIEW',convertible:!!opts.base,
            decisionId:base,version:1,linkedAnnouncement:linked?{announcementId:id,announcementCode:'ANN-TEST'}:null};
        if(url.endsWith('/review-context')) {const c=context();if(opts.stale&&confirmed)c.version.expectedSourceVersion++;return c;}
        if(url.endsWith(`/${id}`))return {source,content:{bodyText:'확인할 본문',sourceUrl:'javascript:alert(1)'}};
        if(url.endsWith('/attachment-sets?page=1&size=1'))return {items:[{setId:base,discoveryComplete:true,discoveryStatusCode:'FOUND'}]};
        return {items:[],page:1,totalPages:0,totalCount:0};
    };
    const page={dataset:{sourceId:id,canManage:String(opts.manage!==false)},querySelector:q};
    runInNewContext(script,{window:{SanebAttachmentReview:{...core,client:()=>request,mutation:()=>core.mutation(()=>id)},fetch(){},addEventListener(){}},document:{querySelector:()=>page,createElement:tag=>new El(tag),createTextNode:t=>t}});
    const settle=async()=>{for(let i=0;i<35;i++)await Promise.resolve();};await settle();
    const fill=()=>{form.elements.primaryTargetCategoryCode.value='PERSONAL';form.elements.incomeJudgementCode.value='NO_LIMIT';form.elements.reviewNote.value='원문 확인';form.elements.acknowledged.checked=true;};
    const submit=async()=>{fill();form.events.submit({preventDefault(){}});await settle();};
    return {q,form,calls,source,submit,settle,fill,counts:()=>({confirmCount,draftCount})};
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
test('unapplied source can read latest collected files without enabling conversion',async()=>{
    const h=await harness({blocked:true,noSet:true});assert.ok(h.calls.some(c=>c.url.includes(`/attachment-sets/${base}/files`)));assert.equal(h.q('[data-convert]').disabled,true);assert.match(h.q('[data-evidence]').textContent,/최근 수집/);
});
test('double submit is locked and result links to the exact draft',async()=>{
    const h=await harness();h.fill();h.form.events.submit({preventDefault(){}});h.form.events.submit({preventDefault(){}});await h.settle();
    assert.deepEqual(h.counts(),{confirmCount:1,draftCount:1});assert.equal(h.q('[data-result]').children.find(el=>el.tag==='a').href,`/app/announcements/input?announcementId=${id}`);
});

test('base review combines confirmation and draft without using attachment confirmation',async()=>{
    const h=await harness({base:true,statusCode:'NOT_APPLIED'});await h.submit();
    assert.deepEqual(h.counts(),{confirmCount:0,draftCount:1});
    const writes=h.calls.filter(c=>c.options.method==='POST');
    assert.equal(writes.length,1);assert.match(writes[0].url,/base-review\/announcements$/);
    const body=JSON.parse(writes[0].options.body);
    assert.equal(body.reviewNote,'원문 확인');assert.equal(body.classification.expectedClassificationDecisionId,base);
    assert.equal(body.classification.expectedVersion,1);assert.equal(body.classification.primaryTargetCategoryCode,'PERSONAL');
    assert.equal(h.q('[data-result]').children.find(el=>el.tag==='a').href,`/app/announcements/input?announcementId=${id}`);
});

test('base review response loss retries an identical atomic request',async()=>{
    const h=await harness({base:true,draftFail:true});await h.submit();
    h.q('[data-retry]').events.click();await h.settle();
    const writes=h.calls.filter(c=>c.options.method==='POST');
    assert.equal(writes.length,2);assert.equal(writes[0].options.body,writes[1].options.body);
    assert.ok(writes.every(c=>c.url.endsWith('/base-review/announcements')));
    assert.equal(h.counts().confirmCount,0);
});
test('announcement input deep link validates UUID and only loads details',async()=>{
    const input=readFileSync(new URL('../../src/main/resources/static/js/saneb-announcement-input.js',import.meta.url),'utf8');
    const start=input.lastIndexOf('loadStandardDocumentFields().then');
    const end=input.indexOf('    loadAnnouncementList();',start);
    const snippet=input.slice(start,end);
    for(const [value,expected] of [[id,1],['javascript:alert(1)',0],['',0]]) {
        const loaded=[],messages=[];
        runInNewContext(snippet,{lockDetailForms(){},loadStandardDocumentFields:async()=>{},loadDetails:async v=>loaded.push(v),setMessage:m=>messages.push(m),window:{location:{href:`https://example.test/app/announcements/input?announcementId=${encodeURIComponent(value)}`}},URL});
        for(let i=0;i<5;i++)await Promise.resolve();
        assert.equal(loaded.length,expected);if(expected)assert.equal(loaded[0],id);if(value&& !expected)assert.equal(messages.length,1);
    }
});

test('legacy code links resolve exact UUID and conflicting identifiers cannot load a form',async()=>{
    const input=readFileSync(new URL('../../src/main/resources/static/js/saneb-announcement-input.js',import.meta.url),'utf8');
    const start=input.lastIndexOf('loadStandardDocumentFields().then'),end=input.indexOf('    loadAnnouncementList();',start);
    for(const conflict of [false,true]){
        const loaded=[],messages=[],calls=[];
        runInNewContext(input.slice(start,end),{lockDetailForms(){},loadStandardDocumentFields:async()=>{},loadDetails:async v=>loaded.push(v),setMessage:m=>messages.push(m),
            baseUrl:'/api/v1/announcements',requestJson:async url=>{calls.push(url);return {announcementId:id};},
            window:{location:{href:`https://example.test/app/announcements/input?announcementCode=ANN-000070${conflict?`&announcementId=${base}`:''}`}},URL});
        for(let i=0;i<12;i++)await Promise.resolve();
        assert.deepEqual(calls,['/api/v1/announcements/by-code/ANN-000070']);
        assert.equal(loaded.length,conflict?0:1);if(conflict)assert.match(messages[0],/일치하지 않습니다/);else assert.equal(loaded[0],id);
    }
});

test('optional condition rows can be empty and the last row is removed rather than reset',()=>{
    const input=readFileSync(new URL('../../src/main/resources/static/js/saneb-announcement-input.js',import.meta.url),'utf8');
    const start=input.indexOf('    const removeConditionRow ='),end=input.indexOf('    const renderConditionRows =',start);
    let removed=0,cleared=0;
    const row={matches:()=>true,remove:()=>removed++};
    runInNewContext(input.slice(start,end)+"removeConditionRow({closest:()=>row},null,'[data-option-condition-row]',null);",{
        row,conditionRows:()=>[row],clearConditionRow:()=>cleared++,normalizeConditionRows(){}});
    assert.equal(removed,1);assert.equal(cleared,0);
    assert.match(input,/selector === "\[data-option-condition-row\]" \? \[\] : \[null\]/);
});

const button=(node,label)=>node.all().find(el=>el.tag==='button'&&el.textContent===label);
const contents=node=>[node,...node.all()].map(el=>el.textContent||'').join(' ');
const files=[{extractionId:id,displayName:'공고문.hwpx',qualityCode:'COMPLETE_TEXT'}, {extractionId:base,displayName:'신청서.hwpx',qualityCode:'COMPLETE_TEXT'}];
const block=(index,extra={})=>({blockIndex:index,startOffset:index*10000,endOffset:index*10000+10,text:`본문 ${index}`,textStartOffset:index*10000,textEndOffset:index*10000+10,hasMoreText:false,...extra});
const reader=callback=>async url=>{
    if(url.includes('/files?'))return {items:files,page:1,totalCount:2,totalPages:1};
    if(url.includes('/blocks?'))return callback(new URL(url,'https://example.test'));
};

test('unapplied evidence shows actionable blocker and read-only categories without enabling writes',async()=>{
    const h=await harness({blocked:true,statusCode:'NOT_APPLIED'});
    assert.match(h.q('[data-blocker]').textContent,/첨부 정책을 변경할 필요는 없습니다/);
    assert.match(h.q('[data-blocker]').textContent,/SRC-TEST/);
    assert.match(contents(h.q('[data-classification]')),/본인\(개인\).*일반 지원/);
    assert.equal(h.form.hidden,true);await h.submit();assert.equal(h.counts().confirmCount,0);
    h.source.effectiveClassification=null;h.source.previewClassification={targetCategoryCodes:['BUSINESS'],supportTypeCodes:['GRANT_SUBSIDY']};
    h.q('[data-refresh]').events.click();await h.settle();
    assert.match(contents(h.q('[data-classification]')),/미리보기 분류 · 적용 전.*사업자.*지원금/);
});

test('mismatched review context never displays ready guidance or clears an in-progress form',async()=>{
    const h=await harness();h.fill();h.form.events.input({target:{name:'reviewNote'}});h.source.sourceVersion=8;
    h.q('[data-refresh]').events.click();await h.settle();
    assert.match(h.q('[data-blocker]').textContent,/전환할 수 없습니다/);
    assert.equal(h.form.hidden,false);assert.equal(h.form.elements.reviewNote.value,'원문 확인');
    assert.equal(h.q('[data-convert]').disabled,true);
});

test('20-block reading, page boundaries and validated page jump use bounded read-only requests',async()=>{
    const h=await harness({read:reader(url=>{
        assert.equal(url.searchParams.get('size'),'20');assert.equal(url.searchParams.get('textLimit'),'4000');
        const p=Number(url.searchParams.get('page'));
        return {items:Array.from({length:p===24?9:20},(_,i)=>block((p-1)*20+i)),page:p,totalPages:24,totalCount:469};
    })});
    button(h.q('[data-files]'),'내용 보기').events.click();await h.settle();
    const node=h.q('[data-blocks]');assert.equal(node.all().filter(el=>el.tag==='pre').length,20);
    assert.match(contents(node),/문단 1~20 \/ 전체 469개/);assert.equal(button(node,'이전 20문단').disabled,true);
    button(node,'다음 20문단').events.click();await h.settle();assert.match(contents(node),/문단 21~40/);
    const jump=node.all().find(el=>el.type==='number');
    for(const invalid of ['0','25','1.5','']) {jump.value=invalid;const count=h.calls.length;button(node,'해당 쪽 보기').events.click();await h.settle();assert.equal(h.calls.length,count);assert.match(contents(node),/1~24 사이의 정수/);}
    jump.value='24';button(node,'해당 쪽 보기').events.click();await h.settle();
    assert.match(contents(node),/문단 461~469/);assert.equal(button(node,'다음 20문단').disabled,true);
    assert.ok(h.calls.every(c=>c.options.method!=='POST'));
});

test('long paragraphs continue independently with server offsets and retry preserves text',async()=>{
    let failures=0;
    const h=await harness({read:reader(url=>{
        if(url.searchParams.get('size')==='20')return {items:[block(0,{endOffset:6000,text:'앞부분 😀',textEndOffset:4000,hasMoreText:true}),block(1)],totalPages:1,totalCount:2};
        assert.equal(url.searchParams.get('page'),'1');assert.equal(url.searchParams.get('textOffset'),'4000');
        if(failures++===0)throw new Error('일시적 조회 실패');
        return {items:[block(0,{endOffset:6000,textStartOffset:4000,textEndOffset:6000,text:'나머지 내용'})]};
    })});
    button(h.q('[data-files]'),'내용 보기').events.click();await h.settle();const node=h.q('[data-blocks]');
    button(node,'문단 1 이어 읽기').events.click();await h.settle();
    assert.match(contents(node),/앞부분 😀/);assert.match(contents(node),/기존 내용은 유지/);assert.match(contents(node),/본문 1/);
    button(node,'문단 1 이어 읽기').events.click();await h.settle();
    assert.match(contents(node),/앞부분 😀.*나머지 내용/);assert.equal(button(node,'문단 1 이어 읽기').hidden,true);
});

test('changed block continuation is not appended and can be retried',async()=>{
    const h=await harness({read:reader(url=>url.searchParams.get('size')==='20'
        ? {items:[block(0,{endOffset:6000,textEndOffset:4000,hasMoreText:true})],totalPages:1,totalCount:1}
        : {items:[block(1,{text:'잘못된 문단',textStartOffset:4000,textEndOffset:6000})]})});
    button(h.q('[data-files]'),'내용 보기').events.click();await h.settle();const node=h.q('[data-blocks]');
    button(node,'문단 1 이어 읽기').events.click();await h.settle();
    assert.doesNotMatch(contents(node),/잘못된 문단/);assert.match(contents(node),/이어지는 내용을 확인하지 못했습니다/);
    assert.equal(button(node,'문단 1 이어 읽기').disabled,false);
});

test('late file response cannot replace a newer selected file',async()=>{
    let resolveOld;
    const h=await harness({read:reader(url=>url.pathname.includes(`/attachment-extractions/${id}/`)
        ? new Promise(resolve=>{resolveOld=resolve;}) : {items:[block(0,{text:'선택한 신청서'})],totalPages:1,totalCount:1})});
    const buttons=h.q('[data-files]').all().filter(el=>el.tag==='button');buttons[0].events.click();await h.settle();
    buttons[1].events.click();await h.settle();resolveOld({items:[block(0,{text:'늦은 이전 공고문'})],totalPages:1,totalCount:1});await h.settle();
    assert.match(contents(h.q('[data-blocks]')),/선택한 신청서/);assert.doesNotMatch(contents(h.q('[data-blocks]')),/늦은 이전 공고문/);
});

test('reader empty and error states offer honest guidance and retry',async()=>{
    let attempt=0;
    const h=await harness({read:reader(()=>{if(attempt++===0)throw new Error('조회 실패');return {items:[],totalPages:0,totalCount:0};})});
    button(h.q('[data-files]'),'내용 보기').events.click();await h.settle();const node=h.q('[data-blocks]');
    assert.match(contents(node),/조회 실패/);button(node,'내용 다시 조회').events.click();await h.settle();
    assert.match(contents(node),/추출한 내용이 없습니다.*외부 원문/);
});

test('refresh invalidates an in-flight attachment response',async()=>{
    let resolveOld;
    const h=await harness({read:reader(()=>new Promise(resolve=>{resolveOld=resolve;}))});
    button(h.q('[data-files]'),'내용 보기').events.click();await h.settle();
    h.q('[data-refresh]').events.click();await h.settle();
    resolveOld({items:[block(0,{text:'갱신 전 응답'})],totalPages:1,totalCount:1});await h.settle();
    assert.doesNotMatch(contents(h.q('[data-blocks]')),/갱신 전 응답/);
});

test('late continuation never contaminates the next file and repeated clicks send once',async()=>{
    let resolveMore;
    const h=await harness({read:reader(url=>{
        if(url.searchParams.get('size')==='1')return new Promise(resolve=>{resolveMore=resolve;});
        return {items:[block(0,{text:url.pathname.includes(`/attachment-extractions/${base}/`)?'다른 파일':'앞 내용',endOffset:6000,textEndOffset:4000,hasMoreText:true})],totalPages:1,totalCount:1};
    })});
    const buttons=h.q('[data-files]').all().filter(el=>el.tag==='button');buttons[0].events.click();await h.settle();
    const more=button(h.q('[data-blocks]'),'문단 1 이어 읽기');more.events.click();more.events.click();await h.settle();
    assert.equal(h.calls.filter(c=>c.url.includes('size=1&textOffset=4000')).length,1);
    buttons[1].events.click();await h.settle();resolveMore({items:[block(0,{text:'이전 파일 뒷부분',endOffset:6000,textStartOffset:4000,textEndOffset:6000})]});await h.settle();
    assert.match(contents(h.q('[data-blocks]')),/다른 파일/);assert.doesNotMatch(contents(h.q('[data-blocks]')),/이전 파일 뒷부분/);
});
