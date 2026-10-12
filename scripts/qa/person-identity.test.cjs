const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const base=path.join(__dirname,'../../src/main/resources');
const identity=require(path.join(base,'static/js/saneb-person-identity.js'));
const picker=fs.readFileSync(path.join(base,'static/js/saneb-person-picker.js'),'utf8');
class Element {
    constructor(){this.value='';this.textContent='';this.disabled=false;this.events={};this.children=[];}
    replaceChildren(...nodes){this.children=nodes;this.value='';}
    append(node){this.children.push(node);}
    addEventListener(type,fn){this.events[type]=fn;}
}
function harness(handler){
    const nodes=new Map();const q=key=>{if(!nodes.has(key))nodes.set(key,new Element());return nodes.get(key);};
    const root={dataset:{personPicker:'member',lookupUrl:'/api/test'},querySelector:q};
    const window={SanebPersonIdentity:identity};
    vm.runInNewContext(picker,{window,URLSearchParams,Option:function(text,value){this.textContent=text;this.value=value;}});
    return {q,api:window.SanebPersonPicker(root,handler)};
}
const page=()=>({items:[{userId:'one',name:'동명이인',loginId:'member.one'},{userId:'two',name:'동명이인',loginId:'member.two'}],totalCount:12,totalPages:2});
test('성함과 아이디를 함께 표시하며 누락 정보와 동명이인을 구분한다',()=>{
    assert.equal(identity.label(' 이름 ',' account '),'이름 (account)');
    assert.equal(identity.label(null,'id'),'id');assert.equal(identity.label('이름',null),'이름');
    assert.equal(identity.label(null,null),'계정 정보 확인 필요');
    assert.notEqual(identity.label('이름','one'),identity.label('이름','two'));
});
test('검색은 자동 선택하지 않으며 명시 선택 후 내부 ID만 저장한다',async()=>{
    const h=harness(async()=>page());await h.q('[data-person-search]').events.click();
    assert.equal(h.q('input[type="hidden"]').value,'');assert.equal(h.q('[data-person-results]').children[1].textContent,'동명이인 (member.one)');
    h.q('[data-person-results]').value='two';h.q('[data-person-results]').events.change();
    assert.equal(h.q('input[type="hidden"]').value,'two');assert.match(h.q('[data-person-selected]').textContent,/member.two/);
});
test('검색어 변경은 이전 선택·결과를 무효화하고 페이지를 초기화한다',async()=>{
    const h=harness(async()=>page());h.api.set('one','이름','member.one');
    h.q('[data-person-query]').value='새 검색';h.q('[data-person-query]').events.input();
    assert.equal(h.q('input[type="hidden"]').value,'');assert.equal(h.api.unresolved(),true);
    assert.equal(h.q('[data-person-next]').disabled,true);
});
test('페이지 이동과 빈 결과를 처리한다',async()=>{
    const urls=[];const h=harness(async url=>{urls.push(url);return urls.length===1?page():{items:[],totalCount:0,totalPages:0};});
    await h.q('[data-person-search]').events.click();assert.equal(h.q('[data-person-next]').disabled,false);
    await h.q('[data-person-next]').events.click();assert.match(urls[1],/page=2/);assert.match(h.q('[data-person-message]').textContent,/검색 결과가 없습니다/);
});
test('늦은 응답이 변경된 검색어와 현재 선택을 덮어쓰지 않는다',async()=>{
    let resolve;const h=harness(()=>new Promise(r=>resolve=r));const pending=h.q('[data-person-search]').events.click();
    h.api.set('current','현재 담당자','staff');resolve(page());await pending;
    assert.equal(h.q('input[type="hidden"]').value,'current');assert.equal(h.q('[data-person-results]').children.length,1);
});
test('조회 실패 시 선택은 보존하고 검색 재시도를 허용한다',async()=>{
    const h=harness(async()=>{throw new Error('검색 권한이 없습니다.');});h.api.set('current','현재','staff');
    await h.q('[data-person-search]').events.click();assert.equal(h.q('input[type="hidden"]').value,'current');
    assert.equal(h.q('[data-person-search]').disabled,false);assert.match(h.q('[data-person-message]').textContent,/권한/);
});
test('코드 대신 이름 선택과 기존 UUID API 필드를 사용한다',()=>{
    const html=fs.readFileSync(path.join(base,'templates/app/consultations.html'),'utf8');
    const js=fs.readFileSync(path.join(base,'static/js/saneb-consultations.js'),'utf8');
    assert.ok(!/USR-|회원 코드|담당자 코드|partnerUserCode|memberUserCode/.test(html+js));
    assert.ok(!/innerHTML|insertAdjacentHTML/.test(js+picker));
    assert.match(js,/memberUserId,/);assert.match(js,/partnerUserId,/);assert.match(js,/partnerPicker.unresolved/);
    assert.ok(html.includes('th:if="${page.operating}"'));
    for(const file of ['saneb-consultations.js','saneb-matching-cases.js','saneb-person-picker.js','saneb-person-identity.js'])new vm.Script(fs.readFileSync(path.join(base,'static/js',file),'utf8'));
});
test('매칭 선택 결과가 코드를 우선 표시하지 않는다',()=>{
    const js=fs.readFileSync(path.join(base,'static/js/saneb-matching-cases.js'),'utf8');
    const html=fs.readFileSync(path.join(base,'templates/app/matching-cases.html'),'utf8');
    assert.ok(!/회원 코드|회원 코드를|item.userCode \|\||item.memberUserCode/.test(js));
    assert.match(js,/SanebPersonIdentity.label/);assert.match(html,/saneb-person-identity.js/);
    assert.ok(js.includes('성함 또는 아이디로 검색한 뒤 회원을 선택하세요.'));
});
test('검증 3개 화면은 표시 이름을 참조하며 코드 필드는 렌더링하지 않는다',()=>{
    for(const file of ['partner-verification-list','partner-verification-input','verification-progress']){
        const html=fs.readFileSync(path.join(base,'templates/app',file+'.html'),'utf8');
        assert.ok(!/memberUserCode|partnerUserCode|회원 번호|파트너 번호/.test(html),file);
        assert.match(html,/memberDisplayName/);
    }
});

// 실제 상담 스크립트의 선택/저장 이벤트를 브라우저 없이 회귀 검증한다.
async function consultationHarness(rows, patch = async body => ({...rows[0], statusCode: body.statusCode})) {
    const node = () => Object.assign(new Element(), {dataset:{},classList:{toggle(){}}});
    const fields = new Map();
    const statusForm = node();
    statusForm.querySelector = key => {if(!fields.has(key))fields.set(key,node());return fields.get(key);};
    const list = node(), statusButton = node(), message = node(), currentPartner = node();
    const controls = new Map([
        ['[data-consultation-status-form]',statusForm],['[data-consultation-list]',list],
        ['[data-consultation-status-submit]',statusButton],['[data-consultation-status-message]',message],
        ['[data-consultation-current-partner]',currentPartner]
    ]);
    const app = {dataset:{operating:'true'},querySelector:key=>controls.get(key)||null};
    const requests=[];
    const fetch = async (url,options) => {
        requests.push({url,options});
        if(options.method==='PATCH') {
            try {const data=await patch(JSON.parse(options.body));return {ok:true,json:async()=>({success:true,data})};}
            catch(error){return {ok:false,json:async()=>({success:false,message:error.message})};}
        }
        return {ok:true,json:async()=>({success:true,data:{items:rows}})};
    };
    const window={SanebPersonIdentity:identity,SanebPersonPicker:()=>({set(){},unresolved:()=>false}),confirm:()=>true};
    vm.runInNewContext(fs.readFileSync(path.join(base,'static/js/saneb-consultations.js'),'utf8'),{
        document:{querySelector:()=>app,createElement:()=>node()},window,fetch
    });
    await new Promise(resolve=>setImmediate(resolve));
    return {field:name=>statusForm.querySelector(`[name='${name}']`),
        select:index=>list.children[index].children.at(-1).events.click(),
        submit:()=>statusForm.events.submit({preventDefault(){}}),requests,message,statusButton};
}
const reservation = statusCode => ({reservationId:'case-'+statusCode,reservationCode:'CNS-QA',statusCode,
    partnerUserId:'staff',partnerName:'담당자',partnerLoginId:'staff',statusNote:'기존 메모'});
test('상담 완료 포함 모든 상태와 다른 상담 재선택 시 서버 현재 상태를 반영한다',async()=>{
    const codes=['COMPLETED','REQUESTED','ASSIGNED','CONFIRMED','CANCELED','NO_SHOW'];
    const h=await consultationHarness(codes.map(reservation));
    codes.forEach((code,index)=>{h.field('statusCode').value='ASSIGNED';h.select(index);assert.equal(h.field('statusCode').value,code);});
    assert.equal(h.requests.filter(r=>r.options.method==='PATCH').length,0);
});
test('미선택·알 수 없는 상태는 담당자 배정으로 대체하지 않는다',async()=>{
    const h=await consultationHarness([reservation('UNKNOWN')]);h.select(0);
    assert.equal(h.field('statusCode').value,'');await h.submit();
    assert.equal(h.requests.filter(r=>r.options.method==='PATCH').length,0);
    const html=fs.readFileSync(path.join(base,'templates/app/consultations.html'),'utf8');
    assert.match(html,/<option value="" disabled selected>상담 요청을 먼저 선택하세요/);
    assert.match(html,/<option value="REQUESTED" disabled>요청 접수/);
});
test('완료·요청접수·동일 상태 저장은 전송 없이 구체적으로 안내한다',async()=>{
    for(const code of ['COMPLETED','NO_SHOW','CANCELED','REQUESTED','ASSIGNED','CONFIRMED']){
        const h=await consultationHarness([reservation(code)]);h.select(0);await h.submit();
        assert.equal(h.requests.filter(r=>r.options.method==='PATCH').length,0,code);
        assert.match(h.message.textContent,/상태를 변경할 수 없습니다|변경할 상태를 선택/);
    }
});
test('상태 저장 성공 응답을 폼에 반영하고 목록도 다시 조회한다',async()=>{
    const h=await consultationHarness([reservation('CONFIRMED')]);h.select(0);h.field('statusCode').value='COMPLETED';
    await h.submit();assert.equal(h.field('statusCode').value,'COMPLETED');
    assert.equal(JSON.parse(h.requests.find(r=>r.options.method==='PATCH').options.body).statusCode,'COMPLETED');
    assert.equal(h.requests.length,3);assert.equal(h.statusButton.disabled,false);
});
test('서버 저장 실패 시 선택한 값과 메모를 보존하고 재시도를 허용한다',async()=>{
    const h=await consultationHarness([reservation('CONFIRMED')],async()=>{throw new Error('상태 변경을 허용하지 않습니다.');});
    h.select(0);h.field('statusCode').value='COMPLETED';await h.submit();
    assert.equal(h.field('statusCode').value,'COMPLETED');assert.equal(h.field('note').value,'기존 메모');
    assert.match(h.message.textContent,/상태 변경을 허용하지 않습니다/);assert.equal(h.statusButton.disabled,false);
});
