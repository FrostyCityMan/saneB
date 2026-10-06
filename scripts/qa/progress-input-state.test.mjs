// 브라우저를 실행하지 않는 동적 입력 이벤트·요청 상태 회귀 테스트.
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
const script=readFileSync(new URL('../../src/main/resources/static/js/saneb-application-progress-input.js',import.meta.url),'utf8');
class Element {
    constructor(tag='div'){this.tag=tag;this.dataset={};this.children=[];this.events={};this.value='';this.textContent='';this.className='';this.classList={toggle(){}};}
    append(...nodes){this.children.push(...nodes);}
    replaceChildren(...nodes){this.children=nodes;}
    addEventListener(type,fn){this.events[type]=fn;}
    all(){return this.children.flatMap(e=>[e,...e.all()]);}
    querySelectorAll(selector){return this.all().filter(e=>{
        if(selector==='[data-requirement-id]')return !!e.dataset.requirementId;
        if(selector==='input,select,textarea')return ['input','select','textarea'].includes(e.tag);
        if(selector==='.dynamic-missing-message')return e.className==='dynamic-missing-message';
        const name=/name='([^']+)'/.exec(selector)?.[1];
        return name&&e.name===name&&(!selector.endsWith(':checked')||e.checked);
    });}
    querySelector(s){return this.querySelectorAll(s)[0]||null;}
}
async function harness({holdLoad=false,holdSave=false,failSave=false}={}){
    const fields=new Element(),form=new Element('form'),button=new Element('button'),summary=new Element(),message=new Element();
    const actionButton=new Element('button');actionButton.dataset.serverBlocked='false';
    const action={querySelector:()=>actionButton};const calls=[];let releaseLoad,releaseSave;
    const value={requirementId:'fixture',fieldTypeCode:'TEXT',fieldLabel:'합성 필수 입력',required:true,valueText:null,sortOrder:1};
    const saved=()=>({...value,valueText:fields.querySelector("[name='fixture']")?.value});
    const app={dataset:{inputValuesUrl:'/api/v1/progress/fixture/input-values'},querySelector:s=>({'[data-progress-dynamic-fields]':fields,'[data-progress-dynamic-form]':form,'[data-progress-dynamic-message]':message,'[data-progress-dynamic-summary]':summary,'[data-progress-dynamic-submit]':button}[s]),querySelectorAll:s=>s==='[data-progress-action-form]'?[action]:fields.querySelectorAll(s)};
    const response=data=>({ok:true,json:async()=>({success:true,data})});
    const fetch=async(url,options)=>{
        calls.push({url,options});
        if(options.method==='PUT'){
            if(holdSave)await new Promise(r=>{releaseSave=r;});
            if(failSave)return {ok:false,json:async()=>({success:false,message:'합성 필수 입력을 입력해야 합니다.'})};
            return response({values:[saved()]});
        }
        if(url.endsWith('/input-requirements'))return response({requirements:[]});
        if(holdLoad)await new Promise(r=>{releaseLoad=r;});
        return response({announcementId:'fixture',values:[value]});
    };
    runInNewContext(script,{document:{querySelector:()=>app,createElement:t=>new Element(t)},window:{},fetch});
    const settle=async()=>{for(let i=0;i<30;i++)await Promise.resolve();};await settle();
    return {fields,form,button,actionButton,message,summary,calls,settle,releaseLoad:()=>releaseLoad(),releaseSave:()=>releaseSave(),submit:()=>form.events.submit({preventDefault(){}})};
}
test('loading blocks premature save and actions, then first valid save succeeds',async()=>{
    const h=await harness({holdLoad:true});assert.equal(h.button.disabled,true);assert.equal(h.actionButton.disabled,true);
    await h.submit();assert.equal(h.calls.filter(c=>c.options.method==='PUT').length,0);
    h.releaseLoad();await h.settle();assert.equal(h.button.disabled,false);
    h.fields.querySelector("[name='fixture']").value='합성 첫 입력';h.form.events.input();assert.equal(h.actionButton.disabled,true);
    await h.submit();assert.equal(h.calls.filter(c=>c.options.method==='PUT').length,1);
    assert.equal(JSON.parse(h.calls.at(-1).options.body).values[0].valueText,'합성 첫 입력');
    assert.equal(h.actionButton.disabled,false);assert.match(h.message.textContent,/저장되었습니다/);
});
test('save locks duplicate submissions and captures input before awaiting response',async()=>{
    const h=await harness({holdSave:true});const input=h.fields.querySelector("[name='fixture']");input.value='원래 입력';h.form.events.input();
    const pending=h.submit();await h.settle();assert.equal(input.disabled,true);await h.submit();
    assert.equal(h.calls.filter(c=>c.options.method==='PUT').length,1);assert.equal(JSON.parse(h.calls.at(-1).options.body).values[0].valueText,'원래 입력');
    h.releaseSave();await pending;assert.equal(h.button.disabled,false);
});
test('failed save retains entered text and blocks next action until saved',async()=>{
    const h=await harness({failSave:true});const input=h.fields.querySelector("[name='fixture']");input.value='보존할 입력';h.form.events.input();await h.submit();
    assert.equal(input.value,'보존할 입력');assert.equal(input.disabled,false);assert.equal(h.actionButton.disabled,true);
    assert.match(h.message.textContent,/합성 필수 입력/);
});
