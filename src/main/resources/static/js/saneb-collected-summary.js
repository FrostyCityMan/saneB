(() => {
    'use strict';
    const page = document.querySelector('[data-collected-announcement-page]');
    if (!page) return;
    const q = selector => page.querySelector(selector);
    const providers = {BIZINFO:'기업마당', GOV24_PUBLIC_SERVICE:'정부24', LOCAL_GOV_NOTICE:'지자체'};
    const reasons = {TITLE_GROUP_B_MATCHED:'제목 제외 키워드 일치', TITLE_COMBINATION_NOT_MATCHED:'제목 대상·지원 조합 미충족', OTHER_LEGACY:'기타·과거 분류'};
    const specs = {
        action: ['[data-collected-action-count]', `${page.dataset.sourceUrl}?semanticStatusCode=REVIEW_REQUIRED&bodyFetchFailed=false&page=1&size=1`],
        accepted: ['[data-collected-accepted-count]', `${page.dataset.sourceUrl}?semanticStatusCode=ACCEPTED&bodyFetchFailed=false&page=1&size=1`],
        excluded: ['[data-collected-excluded-count]', '/api/v2/admin/announcement-source-exclusions/summary']
    };
    const sequences = {action:0, accepted:0, excluded:0};
    const controllers = {};
    const count = value => Number.isSafeInteger(value) && value >= 0;
    const invalid = () => { throw new Error('집계 응답의 기준 또는 건수가 올바르지 않습니다. 다시 집계하세요.'); };
    const validateGroups = (items, labels, total) => {
        if (!Array.isArray(items) || items.length !== Object.keys(labels).length || new Set(items.map(i => i?.code)).size !== items.length
            || items.some(i => !Object.hasOwn(labels, i?.code) || !count(i.count))
            || items.reduce((sum, i) => sum+i.count, 0) !== total) invalid();
    };
    const append = (parent, tag, text) => { const el=document.createElement(tag); el.textContent=text; parent.appendChild(el); return el; };
    const showExclusions = data => {
        if (!data || !count(data.uniqueExcludedCount) || !count(data.unknownPurposeCount)
            || data.countingBasis !== 'PROVIDER_IDENTITY_UNIQUE' || data.scope !== 'RETAINED_PRODUCTION_BY_LATEST_RUN'
            || typeof data.asOf !== 'string' || Number.isNaN(Date.parse(data.asOf))) invalid();
        validateGroups(data.byProvider, providers, data.uniqueExcludedCount);
        validateGroups(data.byLatestReason, reasons, data.uniqueExcludedCount);
        const box=q('[data-exclusion-breakdown]'); box.replaceChildren();
        for (const [heading,items,labels] of [['수집원별',data.byProvider,providers],['마지막 저장 사유별',data.byLatestReason,reasons]]) {
            append(box,'h3',heading);
            const list=append(box,'ul','');
            items.forEach(item=>append(list,'li',`${labels[item.code]}: ${item.count.toLocaleString('ko-KR')}건`));
        }
        q('[data-exclusion-asof]').textContent=`집계 기준: ${new Intl.DateTimeFormat('ko-KR',{dateStyle:'medium',timeStyle:'medium',timeZone:'Asia/Seoul'}).format(new Date(data.asOf))} (한국 시간)`;
        q('[data-exclusion-scope-note]').textContent=data.unknownPurposeCount
            ? `용도 미확인 ${data.unknownPurposeCount.toLocaleString('ko-KR')}건 별도 · 본 집계 제외` : '운영 용도 확인 기록 기준';
    };
    const load = async key => {
        const sequence=++sequences[key], [selector,url]=specs[key], value=q(selector), retry=q(`[data-summary-retry="${key}"]`);
        controllers[key]?.abort();
        const controller=new AbortController(); controllers[key]=controller;
        const timer=setTimeout(()=>controller.abort(),15000);
        value.textContent='집계 중'; value.removeAttribute('title'); retry.hidden=true;
        q(`[data-summary-message="${key}"]`).textContent='';
        if(key==='excluded') {
            q('[data-exclusion-breakdown]').replaceChildren(); q('[data-exclusion-asof]').textContent='집계 중'; q('[data-exclusion-scope-note]').textContent='';
        }
        try {
            const response=await fetch(url,{credentials:'same-origin',cache:'no-store',signal:controller.signal});
            if(response.status===401) throw new Error('로그인이 만료되었습니다. 다시 로그인한 뒤 집계하세요.');
            if(response.status===403) throw new Error('집계를 조회할 권한이 없습니다. 관리자에게 권한을 확인하세요.');
            if(!response.ok) throw new Error('집계를 조회하지 못했습니다. 다시 집계하세요.');
            const payload=await response.json();
            if(sequence!==sequences[key]) return;
            if(payload?.success!==true || !payload.data) invalid();
            const data=payload.data;
            if(key==='excluded') showExclusions(data);
            const total=key==='excluded'?data.uniqueExcludedCount:data.totalCount;
            if(!count(total)) invalid();
            value.textContent=`${total.toLocaleString('ko-KR')}건`;
        } catch(error) {
            if(sequence!==sequences[key]) return;
            const message=error.name==='AbortError'?'집계 응답 시간이 초과되었습니다. 다시 집계하세요.'
                : ['TypeError','SyntaxError'].includes(error.name)?'집계 응답을 확인하지 못했습니다. 네트워크 연결을 확인한 뒤 다시 집계하세요.':error.message;
            value.textContent='집계 불가'; value.setAttribute('title',message); retry.hidden=false;
            q(`[data-summary-message="${key}"]`).textContent=message;
            if(key==='excluded') q('[data-exclusion-asof]').textContent=message;
        } finally { clearTimeout(timer); }
    };
    const open = () => { q('[data-exclusion-summary]').open=true; q('[data-exclusion-heading]').focus(); };
    q('[data-exclusion-open]').addEventListener('click',open);
    Object.keys(specs).forEach(key=>q(`[data-summary-retry="${key}"]`).addEventListener('click',()=>load(key)));
    const restore = () => {
        // 이전 자동 제외 탭의 저장 상태/링크는 원문 목록 대신 이력 안내로 연결한다.
        if(window.history.state?.sanebCollectedList?.view==='EXCLUDED'
            || new URLSearchParams(window.location.search).get('view')==='EXCLUDED') open();
        Object.keys(specs).forEach(key=>load(key));
    };
    window.addEventListener('pagehide',()=>Object.keys(specs).forEach(key=>{ sequences[key]++; controllers[key]?.abort(); }));
    window.addEventListener('pageshow',restore);
    window.SanebCollectedSummary=Object.freeze({refresh:()=>Object.keys(specs).forEach(key=>load(key))});
    if(document.readyState==='complete') restore();
})();
