import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const core=require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const script=readFileSync(new URL('../../src/main/resources/static/js/saneb-announcement-attachment-review.js',import.meta.url),'utf8');
const html=readFileSync(new URL('../../src/main/resources/templates/app/announcement-attachment-review.html',import.meta.url),'utf8');
class Element {
    constructor(tag='div'){this.tag=tag;this.children=[];this.textContent='';this.hidden=false;this.disabled=false;this.events={};this.value='';this.checked=false;}
    append(...children){this.children.push(...children);}
    replaceChildren(...children){this.children=children;}
    setAttribute(){}
    addEventListener(type,fn){this.events[type]=fn;}
    querySelectorAll(){return [];}
    get options(){return this.children;}
    focus(){this.focused=true;}
}
async function harness({applied=false,saved=false,fail=false,linked=false,manage=true}={}) {
    const nodes=new Map(),calls=[];
    const q=selector=>{if(!nodes.has(selector))nodes.set(selector,new Element());return nodes.get(selector);};
    q('[data-review-form]').elements={confirmationAcknowledged:new Element()};
    q('[data-draft-form]').elements={draftAcknowledged:new Element(),primaryTargetCategoryCode:new Element()};
    const source={sourceId:'source',title:'공고 제목',publicCode:'SRC-TEST',providerCode:'LOCAL_GOV_NOTICE',
        effectiveClassification:applied?{setId:'effective',decisionId:'decision'}:{},previewClassification:{setId:'preview',decisionId:'preview-decision'}};
    const context={requiredAcknowledgementCodes:[],linkedAnnouncement:linked?{announcementCode:'ANN-TEST'}:null,
        confirmedClassification:saved?{confirmation:{confirmationId:'confirmation',confirmedAt:'2026-10-05'},targetCategoryCodes:['PERSONAL'],supportTypeCodes:['GENERAL_SUPPORT']}:null};
    const request=async url=>{calls.push(url);if(fail)throw new Error('최신 조회 실패');
        if(url.endsWith('/source'))return {source,content:{bodyText:'본문'}};
        if(url.endsWith('/review-context'))return context;
        return {items:[],page:1,totalPages:0,totalCount:0};};
    const C={...core,client:()=>request,canRequestFinalReview:()=>applied,matchesContext:(s,c)=>!!(applied&&s&&c),confirmedCurrent:c=>!!(saved&&c),mutation:()=>({uncertain:false,pending:false})};
    const page={dataset:{sourceId:'source',canManage:String(manage),canRollback:'false'},querySelector:q};
    const mount=()=>({load:async()=>{},gates(){},dirty:false,busy:false});
    runInNewContext(script,{window:{SanebAttachmentReview:C,fetch(){},SanebAttachmentSegments:{createPanel:()=>({reset(){}})},
        SanebAttachmentOperations:{mount},SanebAttachmentRecovery:{mount},addEventListener(){}},
        document:{querySelector:()=>page,createElement:tag=>new Element(tag),createTextNode:text=>text},Intl,URL});
    for(let i=0;i<12;i++)await Promise.resolve();
    return {q,calls};
}
test('unapplied preview opens files read-only and keeps review and draft hidden',async()=>{
    const h=await harness();assert.equal(h.q('[data-review-editor]').hidden,true);assert.equal(h.q('[data-draft-section]').hidden,true);
    assert.equal(h.q('[data-confirm]').disabled,true);assert.ok(h.calls.some(url=>url.includes('/attachment-sets/preview/files')));
    assert.ok(!h.calls.some(url=>url.endsWith('/review-context')));
});
test('effective evidence takes priority over a newer preview and opens review only',async()=>{
    const h=await harness({applied:true});assert.equal(h.q('[data-review-editor]').hidden,false);assert.equal(h.q('[data-review-editor]').open,true);
    assert.equal(h.q('[data-confirm]').disabled,false);assert.equal(h.q('[data-draft-section]').hidden,true);
    assert.ok(h.calls.some(url=>url.includes('/attachment-sets/effective/files')));assert.ok(!h.calls.some(url=>url.includes('/attachment-sets/preview/files')));
});
test('saved current confirmation collapses editing and reveals separate draft form',async()=>{
    const h=await harness({applied:true,saved:true});assert.equal(h.q('[data-review-editor]').open,false);assert.equal(h.q('[data-draft-section]').hidden,false);
    assert.equal(h.q('[data-create-draft]').disabled,false);
});
test('read-only role and linked announcements cannot write',async()=>{
    for(const opts of [{applied:true,saved:true,manage:false},{applied:true,saved:true,linked:true}]) {
        const h=await harness(opts);assert.equal(h.q('[data-confirm]').disabled,true);assert.equal(h.q('[data-create-draft]').disabled,true);
    }
});
test('lookup failure clears evidence and leaves actions locked',async()=>{
    const h=await harness({fail:true});assert.equal(h.q('[data-confirm]').disabled,true);assert.equal(h.q('[data-review-editor]').hidden,true);
    assert.match(h.q('[data-selected-evidence]').textContent,/조회 실패/);assert.match(h.q('[data-page-error]').textContent,/최신 조회 실패/);
});
test('template retains operation, recovery, acknowledgement and independent mutation controls',()=>{
    for(const selector of ['data-operation-form','data-recovery-form','data-acknowledgements','data-review-form','data-draft-form','data-retry-confirm','data-retry-draft'])assert.ok(html.includes(selector));
    assert.equal((html.match(/data-files\b/g)||[]).length,1);assert.equal((html.match(/data-source-content\b/g)||[]).length,1);
    assert.ok(html.indexOf('data-files')<html.indexOf('data-sets'));
    assert.doesNotMatch(script,/innerHTML|localStorage|sessionStorage/);
});
