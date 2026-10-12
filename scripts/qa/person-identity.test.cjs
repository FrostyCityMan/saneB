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
