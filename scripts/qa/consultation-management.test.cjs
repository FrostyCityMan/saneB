const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const base=path.join(__dirname,'../../src/main/resources');
const M=require(path.join(base,'static/js/saneb-consultation-management-model.js'));
const source=fs.readFileSync(path.join(base,'static/js/saneb-consultation-management.js'),'utf8');
const identity=require(path.join(base,'static/js/saneb-person-identity.js'));
class Element {
    constructor(tag='div',text='',value=''){this.tagName=tag;this.textContent=text;this.value=value;this.children=[];this.events={};this.dataset={};this.hidden=false;this.disabled=false;this.attributes={};}
    append(...nodes){this.children.push(...nodes);}
    replaceChildren(...nodes){this.children=nodes;this.value=nodes[0]?.value||'';}
    addEventListener(type,fn){this.events[type]=fn;}
    setAttribute(k,v){this.attributes[k]=v;}
    focus(){this.focused=true;}
    querySelectorAll(){return [];}
}
const record=(status='REQUESTED')=>({reservationId:'fixture-id',reservationCode:'CNS-TEST',memberName:'회원',memberLoginId:'member',partnerUserId:null,
    statusCode:status,requestNote:'<img src=x onerror=alert(1)>',statusNote:'보존 메모',createdAt:'2026-10-12T00:00:00Z',updatedAt:'2026-10-12T00:00:00.123456Z'});
const detail=(status='REQUESTED')=>({reservation:record(status),allowedNextStatusCodes:status==='COMPLETED'?[]:['ASSIGNED','CONFIRMED','CANCELED']});
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function harness(mode,handler,search='?page=2&size=20&statusCode=REQUESTED'){
    const nodes=new Map(), calls=[], assigned=[], confirms=[], scroll=[];
    const q=key=>{if(!nodes.has(key))nodes.set(key,new Element());return nodes.get(key);};
    const root=q('root');root.dataset={consultationManagement:mode,reservationId:'fixture-id'};root.querySelector=q;root.querySelectorAll=()=>[];
    const form=q('form');form.elements={note:new Element('textarea'),statusCode:new Element('select'),partnerUserId:new Element('input'),memberUserId:new Element('input'),requestNote:new Element('textarea')};
    form.elements.namedItem=name=>form.elements[name];
    q('[data-filter]').elements={statusCode:new Element('select'),size:new Element('select')};
    const history={state:null,replaceState(s,unused,url){this.state=s;this.url=url;}};
    const window={SanebConsultationManagement:M,SanebPersonIdentity:identity,location:{search,href:'/app/admin/consultations'+search,assign:url=>assigned.push(url)},scrollY:73,
        confirm:message=>{confirms.push(message);return true;},addEventListener(){},scrollTo:(x,y)=>scroll.push(y)};
    window.SanebPersonPicker=()=>({set(id,name,login){form.elements.partnerUserId.value=id||'';q('[data-person-selected]').textContent=identity.label(name,login);},unresolved:()=>false});
    const fetch=async(url,options)=>{calls.push({url,options});const result=await handler(url,options);return {ok:result.status?result.status<400:true,status:result.status||200,json:async()=>result.status?{success:false,message:result.message}:{success:true,data:result}};};
    vm.runInNewContext(source,{window,document:{querySelector:()=>root,createElement:tag=>new Element(tag)},history,fetch,URLSearchParams,AbortController,setTimeout,clearTimeout,
        Option:function(text,value){return new Element('option',text,value);}});
    return {q,form,calls,assigned,confirms,history,scroll,root};
}
test('목록 query는 허용 상태·페이지·크기만 보존하며 외부 복귀 URL을 무시한다',()=>{
    assert.deepEqual(M.state('?page=-1&size=999&statusCode=__proto__&returnUrl=https://example.com'),{page:1,size:20,statusCode:''});
    assert.deepEqual(M.state('?page=2&size=50&statusCode=COMPLETED'),{page:2,size:50,statusCode:'COMPLETED'});
    assert.equal(M.href('id',M.state('?page=2&size=50')), '/app/admin/consultations/id?statusCode=&page=2&size=50');
    assert.deepEqual(M.pages(100,100),[96,97,98,99,100]);
});
test('실제 목록 JS는 서버 페이지를 요청하고 표·상세 링크·페이지 이동을 만든다',async()=>{
    const h=harness('list',async()=>({items:[record()],totalCount:41,totalPages:3}));await tick();
    assert.match(h.calls[0].url,/page=2/);assert.equal(h.q('[data-table]').hidden,false);
    const row=h.q('[data-rows]').children[0];assert.equal(row.children[1].children[0].textContent,record().requestNote);
    assert.match(row.children[5].children[0].href,/fixture-id\?statusCode=REQUESTED&page=2&size=20/);
    assert.equal(h.q('[data-pagination]').children.length,5);
    assert.match(h.q('[data-count]').textContent,/총 41건/);
});
test('조회 실패는 0건으로 위장하지 않고 재조회 안내를 제공한다',async()=>{
    const h=harness('list',async()=>{throw new Error('offline');});await tick();
    assert.match(h.q('[data-count]').textContent,/조회 실패/);assert.equal(h.q('[data-table]').hidden,true);assert.equal(h.q('[data-error]').hidden,false);
});
test('줄어든 결과의 범위 초과 페이지는 마지막 유효 페이지로 보정한다',async()=>{
    const h=harness('list',async()=>({items:[],totalCount:0,totalPages:0}),'?page=9');await tick();
    assert.equal(h.calls.length,2);assert.match(h.calls[1].url,/page=1/);assert.match(h.history.url,/page=1/);
});
test('상세 직링크는 첫 페이지 목록을 검색하지 않고 단건 API를 호출한다',async()=>{
    const h=harness('detail',async()=>detail());await tick();
    assert.equal(h.calls[0].url,'/api/v2/admin/consultation-reservations/fixture-id');
    assert.equal(h.form.elements.statusCode.value,'');assert.equal(h.form.elements.statusCode.children.length,4);
    assert.equal(h.q('[data-request-note]').textContent,record().requestNote);assert.equal(h.form.elements.note.value,'보존 메모');
});
test('종료 건은 읽기 전용이고 실패한 최초 상세에서는 폼을 노출하지 않는다',async()=>{
    const h=harness('detail',async()=>detail('COMPLETED'));await tick();
    assert.equal(h.q('[data-fields]').disabled,true);assert.equal(h.q('[data-terminal]').hidden,false);
    const bad=harness('detail',async()=>({status:404}));bad.q('[data-detail]').hidden=true;await tick();
    assert.equal(bad.q('[data-detail]').hidden,true);assert.match(bad.q('[data-error]').textContent,/찾을 수 없습니다/);
});
test('조건부 저장은 원본 timestamp 정밀도를 유지하고 409에서 입력을 보존한다',async()=>{
    const h=harness('detail',async(url,options)=>options.method?{status:409,message:'다른 사용자가 수정했습니다.'}:detail());await tick();
    h.form.elements.statusCode.value='CANCELED';h.form.elements.note.value='작성 중인 메모';h.form.events.input();
    await h.form.events.submit({preventDefault(){}});
    const body=JSON.parse(h.calls[1].options.body);assert.equal(body.expectedUpdatedAt,record().updatedAt);
    assert.equal(h.form.elements.note.value,'작성 중인 메모');assert.equal(h.form.elements.statusCode.value,'CANCELED');assert.match(h.q('[data-error]').textContent,/다른 사용자/);
    await h.q('[data-reload]').events.click();assert.equal(h.form.elements.note.value,'작성 중인 메모');assert.equal(h.q('[data-conflict]').hidden,false);
    h.q('[data-use-latest]').events.click();assert.equal(h.form.elements.note.value,'보존 메모');
});
test('저장 성공 응답이 상태와 다음 선택지를 갱신하고 같은 상세 화면에 머문다',async()=>{
    const h=harness('detail',async(url,options)=>options.method?detail('COMPLETED'):detail());await tick();
    h.form.elements.statusCode.value='CANCELED';await h.form.events.submit({preventDefault(){}});
    assert.equal(h.q('[data-status]').textContent,'상담 완료');assert.equal(h.q('[data-fields]').disabled,true);assert.equal(h.assigned.length,0);
});
test('신규 접수 실패는 회원과 요청을 보존하며 성공 시 해당 상세로 이동한다',async()=>{
    let fail=true;const h=harness('new',async()=>fail?{status:400,message:'접수 확인 필요'}:record());
    h.form.elements.memberUserId.value='member-id';h.form.elements.requestNote.value='상담 요청';
    await h.form.events.submit({preventDefault(){}});assert.equal(h.form.elements.requestNote.value,'상담 요청');
    fail=false;await h.form.events.submit({preventDefault(){}});assert.match(h.assigned[0],/fixture-id\?/);
});
test('표·상세·접수 template는 분리하고 raw HTML 출력과 개인정보 로컬 저장을 사용하지 않는다',()=>{
    const list=fs.readFileSync(path.join(base,'templates/app/consultation-management-list.html'),'utf8');
    assert.match(list,/<table/);assert.doesNotMatch(list,/data-status-form|data-create-form/);
    assert.doesNotMatch(source,/innerHTML|localStorage|sessionStorage/);
});
