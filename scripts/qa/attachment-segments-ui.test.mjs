import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFile} from 'node:fs/promises';
const require = createRequire(import.meta.url);
const S = require('../../src/main/resources/static/js/saneb-attachment-segments.js');
const C = require('../../src/main/resources/static/js/saneb-attachment-review-core.js');
const uuid = n => `${String(n).padStart(8, '0')}-1111-4111-8111-111111111111`;
const sourceId = uuid(1), setId = uuid(2);
const file = {setId, fileId:uuid(3), extractionId:uuid(4), displayName:'공고문 및 신청서.hwp', characterCount:80, qualityCode:'COMPLETE_TEXT'};
const evidence = (codes, start) => codes.map((ruleCode, i) => ({ruleCode, blockIndex:i, startOffset:start+i*5, endOffset:start+i*5+4}));
const fixture = () => ({sourceId, setId, fileId:file.fileId, extractionId:file.extractionId,
    fileRoleCode:'UNKNOWN', fileRoleOriginCode:'TEXT_RULE', applicationMode:'SHADOW', analysisState:'ANALYZED',
    analysisId:uuid(5), analyzedAt:'2026-09-24T15:00:00+09:00', analysis:{analysisVersion:'segment-role-1.0.0',
        rulesHash:'fb807a5fcf11c102badcc35cc4b60c6abe7fa36672e2aa431e3b5f2dc16bcdde', textHash:'b'.repeat(64), blocksHash:'c'.repeat(64), textLength:80,
        statusCode:'RESOLVED', reasonCode:'SEGMENTS_RESOLVED', segments:[
            {index:0, startOffset:0, endOffset:40, roleCode:'NOTICE', reasonCode:'ROLE_TEXT_STRUCTURE_MATCHED',
                evidence:evidence(['NOTICE_HEADING','TARGET_SECTION','SUPPORT_SECTION','APPLICATION_SECTION'],0)},
            {index:1, startOffset:40, endOffset:80, roleCode:'FORM', reasonCode:'ROLE_TEXT_STRUCTURE_MATCHED',
                evidence:evidence(['FORM_HEADING','APPLICANT_FIELD','SIGNATURE_FIELD'],40)}]}});
const notAnalyzed = () => ({...fixture(), analysisState:'NOT_ANALYZED', analysis:null, analysisId:null, analyzedAt:null});
const boundFixture = () => {
    const segmentAnalysis=fixture();Object.assign(segmentAnalysis.analysis,{analysisVersion:'segment-role-1.0.2',
        rulesHash:'2f02f48368ce3f42557dd62094dec8e6b99d44e27d0f51f265a2fd737aabdd82'});
    return {evaluationId:uuid(6),policyId:uuid(7),evaluationCurrent:true,evaluatedFileRoleCode:'UNKNOWN',segmentAnalysis};
};
test('structural evaluation binding accepts exact known hash but never quarter fallback or default shadow',()=>{
    const d=boundFixture();d.segmentAnalysis.analysis.analysisVersion='segment-role-1.0.3';
    assert.equal(S.validBinding(d,uuid(6),sourceId,setId,file),false);
    d.segmentAnalysis.analysis.rulesHash='8b9fdd872f3eb9890146d6e360408204ff285f4b23977e07693834aceec66d43';
    assert.equal(S.validBinding(d,uuid(6),sourceId,setId,file),true);
    assert.equal(S.valid(d.segmentAnalysis,sourceId,setId,file),false);
    d.segmentAnalysis.analysis.analysisVersion='segment-role-1.0.2';
    assert.equal(S.validBinding(d,uuid(6),sourceId,setId,file),false);
});

test('bound analysis requires exact evaluation and known version/hash without legacy fallback', () => {
    const d=boundFixture();assert.equal(S.validBinding(d,uuid(6),sourceId,setId,file),true);
    assert.equal(S.valid(d.segmentAnalysis,sourceId,setId,file),false);
    for(const mutate of [v=>v.evaluationId=uuid(9),v=>v.policyId=null,v=>v.evaluationCurrent='true',
        v=>v.evaluatedFileRoleCode='APPROVED',v=>v.segmentAnalysis=notAnalyzed(),
        v=>v.segmentAnalysis.analysis.rulesHash=fixture().analysis.rulesHash,
        v=>v.segmentAnalysis.analysis.analysisVersion='segment-role-1.0.1']) {
        const invalid=boundFixture();mutate(invalid);assert.equal(S.validBinding(invalid,uuid(6),sourceId,setId,file),false);
    }
});

test('mixed notice/form segments preserve unknown file role and require exact source/set/file/extraction', () => {
    assert.equal(S.valid(fixture(),sourceId,setId,file),true);
    assert.equal(S.valid({...fixture(),fileRoleCode:'REFERENCE',fileRoleOriginCode:'MANUAL'},sourceId,setId,file),true);
    for(const key of ['sourceId','setId','fileId','extractionId']) assert.equal(S.valid({...fixture(),[key]:uuid(9)},sourceId,setId,file),false,key);
    for(const patch of [{setId:uuid(9)},{characterCount:79},{characterCount:'80'},{qualityCode:'PARTIAL_TEXT'}])
        assert.equal(S.valid(fixture(),sourceId,setId,{...file,...patch}),false);
});
test('not analyzed is separate from absent attachments and never accepts stored success fields', () => {
    assert.equal(S.valid(notAnalyzed(),sourceId,setId,file),true);
    for(const patch of [{analysis:fixture().analysis},{analysisId:uuid(5)},{analyzedAt:fixture().analyzedAt},
        {applicationMode:'APPLIED'},{analysisState:'UNKNOWN'},{fileRoleOriginCode:'future'}])
        assert.equal(S.valid({...notAnalyzed(),...patch},sourceId,setId,file),false);
});
test('segment coverage, bounds, data types, version and fingerprints fail closed', () => {
    const invalid = [d=>d.analysis.textLength=81,d=>d.analysis.segments.pop(),d=>d.analysis.segments.reverse(),
        d=>d.analysis.segments[1].startOffset=39,d=>d.analysis.segments[1].endOffset=81,
        d=>d.analysis.segments[0].index='0',d=>d.analysis.segments[0].roleCode='APPROVED',
        d=>d.analysis.segments[0].evidence[0].endOffset=41,d=>d.analysis.segments[1].evidence[0].startOffset=39,
        d=>d.analysis.segments[0].evidence[0].blockIndex=20000,d=>d.analysis.segments[0].evidence[0].startOffset='0',
        d=>d.analysis.segments[0].evidence[0].ruleCode='future',d=>d.analysis.segments[0].evidence=[],
        d=>d.analysis.segments[0].evidence=null,d=>d.analysis.segments[0]=null,d=>d.analysis.segments=Array(201).fill({}),
        d=>d.analysis.rulesHash='invalid',d=>d.analysis.blocksHash=null,d=>d.analysis.textHash='x'.repeat(64),
        d=>d.analysis.analysisVersion='segment-role-2.0.0',d=>d.analysis.reasonCode='SEGMENT_CONTEXT_REQUIRED',
        d=>d.analysis.statusCode='REVIEW_REQUIRED',d=>d.analyzedAt='invalid',d=>d.analysisId=null];
    for (const mutate of invalid) {const d=fixture();mutate(d);assert.equal(S.valid(d,sourceId,setId,file),false,mutate.toString());}
});
test('unknown or incomplete segments remain review-required even with useful headings', () => {
    const d=fixture();d.analysis.segments[1].roleCode='UNKNOWN';d.analysis.segments[1].reasonCode='ROLE_STRUCTURE_INCOMPLETE';
    d.analysis.statusCode='REVIEW_REQUIRED';d.analysis.reasonCode='SEGMENT_CONTEXT_REQUIRED';
    assert.equal(S.valid(d,sourceId,setId,file),true);
    d.analysis.statusCode='RESOLVED';assert.equal(S.valid(d,sourceId,setId,file),false);
    for(const reason of ['COMPLETE_TEXT_REQUIRED','STRUCTURE_UNCERTAIN','SEGMENT_ANALYSIS_LIMIT']){
        const a={...fixture().analysis,statusCode:'REVIEW_REQUIRED',reasonCode:reason,
            segments:[{index:0,startOffset:0,endOffset:80,roleCode:'UNKNOWN',reasonCode:reason,evidence:[]}]};
        assert.equal(S.valid({...fixture(),analysis:a},sourceId,setId,{...file,qualityCode:'PARTIAL_TEXT'}),true);
    }
});

// DOM 유사 객체와 순수 의존성으로 조회 상호작용을 검사한다. 실제 브라우저/레이아웃 검증은 아니다.
class Element {
    children=[];attrs={};focused=0;textContent='';listeners={};open=false;
    replaceChildren(){this.children=[];this.textContent='';}
    setAttribute(k,v){this.attrs[k]=v;}
    focus(){this.focused++;}
    addEventListener(name,callback){this.listeners[name]=callback;}
    expand(){this.open=true;this.listeners.toggle?.();}
}
const flatten = n => [n,...n.children.flatMap(flatten)];
function harness(fetcher) {
    const container=new Element(),requests=[],blocks=[];let epoch=1;
    const text=(parent,tag,value,className)=>{const el=new Element();Object.assign(el,{tag,textContent:value,className});parent.children.push(el);return el;};
    const panel=S.createPanel({container,sourceId,core:C,readEpoch:()=>epoch,
        request:async(...args)=>{requests.push(args);return fetcher(...args);},text,
        meta:(p,pairs)=>pairs.forEach(([k,v])=>{text(p,'dt',k);text(p,'dd',v);}),
        action:(p,title,callback)=>{const b=text(p,'button',title);b.click=callback;return b;},
        date:v=>v,showBlocks:(...args)=>blocks.push(args)});
    return {panel,container,requests,blocks,advance:()=>epoch++,all:()=>flatten(container),words:()=>flatten(container).map(x=>x.textContent).join('\n')};
}
test('read-only panel displays separate file and segment roles and exact extraction evidence navigation', async () => {
    const h=harness(()=>fixture());await h.panel.show(file,setId);
    assert.deepEqual(h.requests,[[`/api/v2/admin/announcement-sources/${sourceId}/attachment-extractions/${file.extractionId}/segment-analysis`]]);
    assert.equal(h.container.attrs['aria-busy'],'false');assert.equal(h.container.focused,1);
    assert.match(h.words(),/구간 역할 분석 완료 · 최종 검증 아님/);assert.match(h.words(),/파일 전체 역할/);
    assert.match(h.words(),/역할 미확정/);assert.match(h.words(),/1번 구간 · 공고문/);assert.match(h.words(),/2번 구간 · 신청 양식/);
    assert.match(h.words(),/지원대상 확정 근거로 단독 사용하지 않습니다/);
    assert.equal(h.all().filter(x=>x.tag==='button').length,0);
    const first=h.all().find(x=>x.tag==='details'&&x.className==='attachment-evidence-item');first.expand();first.expand();
    assert.equal(h.all().filter(x=>x.tag==='button').length,4);
    const button=h.all().find(x=>x.tag==='button');button.click();
    assert.deepEqual(h.blocks,[[file,{...fixture().analysis.segments[0].evidence[0],extractionId:file.extractionId},1]]);
    h.advance();button.click();assert.equal(h.blocks.length,1);
});
test('loading, unanalysed and unresolved states do not offer analysis writes or automatic success', async () => {
    let resolve;const h=harness(()=>new Promise(r=>resolve=r));const loading=h.panel.show(file,setId);
    assert.equal(h.container.attrs['aria-busy'],'true');assert.match(h.words(),/조회 중/);
    resolve(notAnalyzed());await loading;assert.match(h.words(),/분석 미완료/);
    assert.equal(h.all().filter(n=>n.tag==='button').length,0);assert.equal(h.container.attrs['aria-busy'],'false');
    const d=fixture();d.analysis.statusCode='REVIEW_REQUIRED';d.analysis.reasonCode='SEGMENT_CONTEXT_REQUIRED';
    Object.assign(d.analysis.segments[1],{roleCode:'UNKNOWN',reasonCode:'STRUCTURE_UNCERTAIN',evidence:[]});
    const u=harness(()=>d);await u.panel.show(file,setId);assert.match(u.words(),/역할 미확정 구간이 남아 있음/);
    assert.match(u.words(),/확정할 수 있는 구간 역할 근거가 없습니다/);assert.doesNotMatch(u.words(),/구간 역할 분석 완료/);
});
test('selected evaluation displays its bound version, snapshot role and history state using GET only', async () => {
    for(const current of [true,false]) {
        const d=boundFixture();d.evaluationCurrent=current;d.evaluatedFileRoleCode='FORM';
        const h=harness(()=>d);await h.panel.show(file,setId,{evaluationId:uuid(6)});
        assert.deepEqual(h.requests,[[`/api/v2/admin/announcement-sources/${sourceId}/attachment-extractions/${file.extractionId}/segment-analysis/evaluations/${uuid(6)}`]]);
        assert.match(h.words(),/선택한 판정의 입력에 연결/);assert.match(h.words(),/segment-role-1.0.2/);
        assert.match(h.words(),/판정 당시 파일 역할/);assert.match(h.words(),/파일 전체 역할/);
        assert.match(h.words(),current?/운영 적용 여부와 별개/:/과거 판정 이력 · 현재 검수 기준 아님/);
    }
});
test('bound retrieval failure never substitutes independent analysis and retries the same evaluation', async () => {
    let first=true;const h=harness(()=>{if(first){first=false;return fixture();}return boundFixture();});
    await h.panel.show(file,setId,{evaluationId:uuid(6)});assert.doesNotMatch(h.words(),/1번 구간/);
    await h.all().find(n=>n.tag==='button').click();assert.match(h.words(),/segment-role-1.0.2/);
    assert.equal(h.requests.length,2);assert.equal(h.requests[0][0],h.requests[1][0]);
    const invalid=harness(()=>{throw new Error('must not request');});await invalid.panel.show(file,setId,{evaluationId:'bad'});
    assert.equal(invalid.requests.length,0);assert.match(invalid.words(),/판정 ID/);
});
test('manual/profile role conflicts remain explicit without changing stored role or calling writes', async () => {
    for(const origin of ['MANUAL','PROFILE']){
        const d={...fixture(),fileRoleCode:'REFERENCE',fileRoleOriginCode:origin};
        const h=harness(()=>d);await h.panel.show(file,setId);
        assert.match(h.words(),/지정 파일 역할과 구간 제안이 다릅니다/);assert.match(h.words(),/파일 역할은 유지/);
        assert.equal(d.fileRoleCode,'REFERENCE');assert.equal(h.requests.length,1);
    }
});
test('bad binding and authorization/network errors offer GET-only retry without displaying evidence', async () => {
    for(const result of [{...fixture(),extractionId:uuid(9)},new Error('로그인이 만료되었습니다. 다시 로그인하세요.'),new Error('조회 권한이 없습니다.')]){
        let first=true;const h=harness(()=>{if(first){first=false;if(result instanceof Error)throw result;return result;}return fixture();});
        await h.panel.show(file,setId);assert.equal(h.container.attrs['aria-busy'],'false');
        assert.equal(h.all().filter(n=>n.attrs.role==='alert').length,1);assert.doesNotMatch(h.words(),/1번 구간/);
        await h.all().find(n=>n.tag==='button').click();assert.match(h.words(),/1번 구간/);
        assert.equal(h.requests.length,2);assert(h.requests.every(args=>args.length===1));
    }
});
test('late old-file response, reset and changed page epoch cannot overwrite newer evidence', async () => {
    const pending=[];const h=harness(()=>new Promise((resolve,reject)=>pending.push({resolve,reject})));
    const old=h.panel.show(file,setId);const newer=h.panel.show({...file,displayName:'두 번째 파일'},setId);
    pending[1].resolve(fixture());await newer;const saved=h.words();pending[0].reject(new Error('늦은 오류'));await old;
    assert.equal(h.words(),saved);assert.equal(h.container.focused,2);
    const stale=h.panel.show(file,setId);h.panel.reset();pending[2].resolve(fixture());await stale;
    assert.equal(h.container.children.length,0);assert.equal(h.container.attrs['aria-busy'],'false');
    const changed=h.panel.show(file,setId);h.advance();h.panel.reset();pending[3].resolve(fixture());await changed;
    assert.equal(h.container.children.length,0);
});
test('untrusted filename is text only, and page loads module before read-only navigation wiring', async () => {
    const name='<img src=x onerror=alert(1)>';const h=harness(()=>notAnalyzed());await h.panel.show({...file,displayName:name},setId);
    assert(h.all().some(n=>n.tag==='h3'&&n.textContent.includes(name)));assert(!h.all().some(n=>n.tag==='img'));
    const paths=['src/main/resources/static/js/saneb-attachment-segments.js','src/main/resources/static/js/saneb-announcement-attachment-review.js','src/main/resources/templates/app/announcement-attachment-review.html'];
    const [module,main,html]=await Promise.all(paths.map(p=>readFile(p,'utf8')));
    assert.doesNotMatch(module,/innerHTML|insertAdjacentHTML|localStorage|sessionStorage|method\s*:/);
    assert.match(main,/segments\.show\(file, setId\), !file\.extractionId/);
    assert.match(main,/const showFiles[^]*?segments\.reset\(\)/);
    assert.match(main,/const refresh[^]*?segments\.reset\(\)/);
    assert.match(html,/data-segments role="region"[^>]*tabindex="-1"/);
    assert(html.indexOf('saneb-attachment-segments.js')<html.indexOf('saneb-announcement-attachment-review.js'));
});
