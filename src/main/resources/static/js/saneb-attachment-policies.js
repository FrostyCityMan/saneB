/* 정책 초안·QA·범위 고정·게시 화면. 실제 게시만 명시적 3항 동의로 실행하며 기존 데이터 배치는 호출하지 않는다. */
((root)=>{
    "use strict";
    function mount({page,P,request,doc,uuid,navigation,confirmDiscard}) {
        const q=s=>page.querySelector(s),admin=page.dataset.isAdmin==="true",form=q("[data-editor]"),approval=q("[data-approval-form]");
        const field=n=>form.elements.namedItem(n),reason=approval.elements.namedItem("reason"),ack=approval.elements.namedItem("acknowledged");
        const consentNames=["acknowledgeNewCollectionBehavior","acknowledgeExistingJobsUnchanged","acknowledgeNoBackfill"];
        const consents=()=>Object.fromEntries(consentNames.map(n=>[n,approval.elements.namedItem(n).checked]));
        const state={detail:null,run:null,impact:null,scope:null,publication:null,reconciliation:null},mutation=P.mutations(request,uuid);
        let busy=false,stale=true,dirty=false,action=null,creating=false;const rules=new Map(),retiredRules=new Set();
        const el=(parent,tag,text,css)=>{const e=doc.createElement(tag);if(text!=null)e.textContent=String(text);if(css)e.className=css;parent.append(e);return e;};
        const clear=s=>{const e=q(s);e.replaceChildren();return e;},date=t=>t?new Date(t).toLocaleString("ko-KR",{timeZone:"Asia/Seoul",hour12:false})+" (서울)":"없음";
        const meta=(box,pairs)=>{const dl=el(box,"dl",null,"attachment-meta");for(const[k,v]of pairs){el(dl,"dt",k);el(dl,"dd",v);}};
        const note=s=>{q("[data-status]").textContent=s;},error=e=>{q("[data-error]").textContent=e?.message||"현재 상태를 확인하지 못했습니다. 다시 조회하세요.";q("[data-error]").hidden=false;q("[data-error]").focus();};
        const locked=()=>busy||mutation.pending||mutation.uncertain;
        const values=()=>({ruleReleaseId:field("ruleReleaseId").value,modeCode:field("modeCode").value,maximumSourceBytes:field("maximumSourceBytes").value});
        function disarm(restoreFocus=false){const kind=action?.kind;action=null;ack.checked=false;for(const n of consentNames)approval.elements.namedItem(n).checked=false;q("[data-approval]").hidden=true;
            if(restoreFocus&&kind)q(({create:"[data-save]",update:"[data-save]",revision:"[data-revision]",qa:"[data-qa]",cancel:"[data-cancel-qa]",prepare:"[data-prepare]",publish:"[data-publish]"})[kind]).focus();}
        function gates(){
            const editable=admin&&!stale&&(creating||state.detail?.isEditable),frozen=locked();
            q("[data-editor-fields]").disabled=frozen||!editable;q("[data-save]").disabled=frozen||!editable;
            for(const s of ["[data-list-refresh]","[data-filter-submit]","[data-rule-refresh]","[data-new]","[data-reset]"])q(s).disabled=frozen;
            q("[data-new]").disabled=frozen||!admin;q("[data-refresh]").disabled=frozen||!state.detail;
            q("[data-revision]").disabled=frozen||!admin||stale||!state.detail;
            q("[data-qa]").disabled=frozen||!admin||stale||dirty||!state.detail?.isEditable;
            q("[data-cancel-qa]").disabled=frozen||!admin||stale||!state.run||!["PENDING","RUNNING"].includes(state.run.statusCode);
            q("[data-prepare]").disabled=frozen||!admin||stale||dirty||!state.detail?.isEditable;
            q("[data-publish]").disabled=frozen||!admin||stale||dirty||!P.canPublish(state);
            q("[data-publication-condition]").textContent=!admin?"조회 전용 역할입니다. 게시 준비·실행은 활성 관리자만 가능합니다.":dirty?"미저장 초안 입력을 저장하거나 복원하세요.":stale?"최신 정책·QA·영향·범위 조회가 필요합니다.":P.canPublish(state)?"현재 조회 조건은 충족합니다. 세 가지 영향 확인 후에도 서버가 전체 증거·설치·범위를 최종 재검증합니다.":"최신 전체 QA 4단계 통과와 현재 정책에 연결된 유효한 준비 범위가 필요합니다. 준비 이력만으로 게시할 수 없습니다.";
            q("[data-approval-fields]").disabled=frozen||!admin||!action;q("[data-approve]").disabled=frozen||!admin||!action;
            q("[data-publication-consents]").hidden=action?.kind!=="publish";q("[data-publication-consents]").disabled=frozen||action?.kind!=="publish";
            q("[data-approve]").textContent=action?.kind==="publish"?"확인한 정책을 실제 게시":action?.kind==="prepare"?"게시 준비 범위만 고정":"확인한 초안·QA 작업 실행";
            q("[data-close-approval]").disabled=frozen;q("[data-uncertain]").hidden=!mutation.uncertain;q("[data-retry]").disabled=busy||mutation.pending;
            q("[data-inspect]").disabled=busy||mutation.pending||!mutation.uncertain||mutation.sent?.keyed!==false;
            q("[data-reconcile]").disabled=busy||mutation.pending||!state.reconciliation;q("[data-reconcile-ack]").disabled=busy||!state.reconciliation;
            page.setAttribute("aria-busy",String(busy));
            q("[data-input-state]").textContent=dirty?"저장하지 않은 입력이 있습니다. QA는 저장된 초안으로만 실행됩니다.":"현재 표시한 저장 기준입니다. 편집·저장만으로 운영 모드를 바꾸지 않습니다.";
        }
        function link(box,text,patch){const a=el(box,"a",text,"secondary-action");a.href=navigation.url(patch);a.addEventListener("click",e=>{if(locked()){e.preventDefault();error(new Error("요청 결과가 미확정이거나 처리 중입니다. 현재 요청을 먼저 확인하세요."));}});return a;}
        function pages(selector,data,key){const box=clear(selector);el(box,"p",`${data.totalCount.toLocaleString("ko-KR")}건 · ${data.totalPages?`${data.page}/${data.totalPages}`:"0"}페이지`);
            if(data.page>1)link(box,"이전 페이지",{[key]:data.page-1});if(data.page<data.totalPages)link(box,"다음 페이지",{[key]:data.page+1});}
        async function read(work){if(locked())return;busy=true;disarm();q("[data-error]").hidden=true;note("정책·QA의 현재 상태를 조회합니다.");gates();
            try{await work();note("조회 시점의 상태입니다. QA 통과와 운영 게시·기존 데이터 적용은 별도입니다.");}
            catch(e){stale=true;error(e);note("일부 조회에 실패했습니다. 확인된 이력과 입력은 보존하며 변경 전 최신 상태 확인이 필요합니다.");}
            finally{busy=false;gates();}}
        function policyMeta(box,p){meta(box,[["정책",`${p.policyCode} / 개정 ${p.versionNo}`],["정책 ID",p.policyId],["정책 상태",P.label(p.policyStatusCode)],
            ["초안/게시 모드",P.label(p.modeCode)],["조회 버전",p.rowVersion],["키워드 규칙",`${p.ruleReleaseId} · ${P.label(p.ruleReleaseStatusCode)}`],["게시 시각",date(p.publishedAt)]]);}
        async function loadList(){const nav=navigation.read(),n=nav.listPage||1,status=nav.status||"";
            const data=await request(`${P.base}?page=${n}&size=10${status?`&status=${status}`:""}`);P.requireValue(P.page(data,n,10,P.summary)&&new Set(data.items.map(p=>p.policyId)).size===data.items.length);
            const box=clear("[data-list]");if(!data.items.length)el(box,"p",status?"선택한 상태의 정책이 없습니다. 상태 필터를 변경하세요.":"등록된 첨부 정책이 없습니다. 관리자는 키워드 규칙을 선택해 새 초안을 만들 수 있습니다.");
            for(const p of data.items){const row=el(box,"article",null,"attachment-evidence-item");el(row,"p",`${p.policyCode} / 개정 ${p.versionNo} · ${P.label(p.policyStatusCode)} · ${P.label(p.modeCode)}`);link(row,`개정 ${p.versionNo} 정책 상세·QA 확인`,{policyId:p.policyId,runId:null,qaPage:1,scopeId:null,scopePage:1,scopeItemPage:1});}
            pages("[data-list-pages]",data,"listPage");}
        async function loadRules(){const n=navigation.read().rulePage||1,data=await request(`${P.rules}?page=${n}&size=20`);P.requireValue(P.page(data,n,20,P.rule));
            const box=clear("[data-rules]");el(box,"p","현재 페이지에 있는 키워드 규칙만 선택 목록에 추가됩니다. 퇴역 규칙은 새 초안 저장에 사용할 수 없습니다.");
            for(const r of data.items){el(box,"p",`${r.releaseCode} / 개정 ${r.versionNo} · ${P.label(r.releaseStatusCode)}`);
                if(["DRAFT","ACTIVE"].includes(r.releaseStatusCode)){rules.set(r.releaseId,r);retiredRules.delete(r.releaseId);}else{rules.delete(r.releaseId);retiredRules.add(r.releaseId);}}
            if(!data.items.length)el(box,"p","키워드 규칙이 없습니다. 공고 키워드 관리에서 규칙을 먼저 준비하세요.");
            fillRules(field("ruleReleaseId").value);pages("[data-rule-pages]",data,"rulePage");}
        function fillRules(selected){const select=field("ruleReleaseId");select.replaceChildren();el(select,"option","키워드 규칙 선택").value="";
            for(const r of rules.values())el(select,"option",`${r.releaseCode} / 개정 ${r.versionNo} · ${P.label(r.releaseStatusCode)}`).value=r.releaseId;
            if(selected&&!rules.has(selected)){const option=el(select,"option",`${selected} · ${retiredRules.has(selected)?"퇴역 규칙 · 저장 불가":"연결 규칙(저장 가능 여부는 서버 확인)"}`);option.value=selected;option.disabled=retiredRules.has(selected);}select.value=selected||"";}
        function showDetail(d,fill=true){P.requireValue(P.details(d));state.detail=d;creating=false;const box=clear("[data-detail]");policyMeta(box,d.policy);
            const coverageLink=el(box,"a","수집원별 첨부 검증 범위 조회 (읽기 전용)","secondary-action");
            coverageLink.href="/app/admin/announcement-attachment-provider-coverage?policyId="+d.policy.policyId;
            meta(box,[["공고별 누적 다운로드 한도",`${d.configuration.maximumSourceBytes.toLocaleString("ko-KR")}바이트`],["저장 시각",date(d.updatedAt)],
                ["분류 엔진",d.configuration.engineVersion],["추출기",d.configuration.extractorVersion],["개정 원본",d.copiedFromPolicyId||"없음"],
                ["텍스트 역할 규칙",d.configuration.roleRuleVersion||"연결 없음 · 자동 역할 판정 미적용"],
                ["역할 규칙 지문",d.configuration.roleRulesHash||"연결 없음"]]);
            el(box,"p",d.configuration.roleRuleVersion
                ?"이 정책은 완전 추출된 역할 미확정 파일에 텍스트 규칙을 사용합니다. 관리자·시스템 지정 역할은 유지하며, 정책 게시·기존 데이터 재처리는 별도 승인 대상입니다."
                :"기존 정책에는 새 역할 규칙을 자동 적용하지 않습니다. 새 초안 저장 후 QA와 게시 절차가 필요합니다.");
            el(box,"p",d.isEditable?"이 초안은 편집 가능합니다. 수정 후 이전 QA는 다시 검증해야 합니다.":"현재 계정·정책 상태에서는 직접 편집할 수 없습니다. 관리자는 개정 초안을 만들 수 있습니다.");
            const bindings=el(box,"details");el(bindings,"summary",`시스템 수집 방식 ${d.systemProfileBindings.length}개 · 관리자가 선택하지 않음`);
            for(const p of d.systemProfileBindings)el(bindings,"p",`${P.label(p.providerCode)} / ${p.profileCode} · 지문 ${p.profileHash}`);
            if(fill){fillRules(d.policy.ruleReleaseId);field("modeCode").value=d.policy.modeCode;field("maximumSourceBytes").value=String(d.configuration.maximumSourceBytes);dirty=false;}
        }
        function stepView(box,list){for(const s of list){const row=el(box,"article",null,"attachment-evidence-item");el(row,"p",`${P.label(s.stepCode)}: ${P.label(s.statusCode)}`);
            if(s.evidenceHash)el(row,"p",`단계 증거 지문: ${s.evidenceHash}`);
            // 임의 evidence JSON·원문·URL은 화면에 출력하지 않는다.
            const code=s.evidence?.reasonCode;if(typeof code==="string"&&/^[A-Z_]{1,100}$/.test(code))el(row,"p",P.label(code));}}
        function showRun(r){P.requireValue(P.run(r,state.detail.policy.policyId));state.run=r;const box=clear("[data-run]");
            meta(box,[["QA 실행 ID",r.runId],["상태",P.label(r.statusCode)],["QA 실행 조회 버전",r.rowVersion],["고정 정책/규칙 버전",`${r.policyVersion} / ${r.ruleVersion}`],
                ["현재 정책·규칙 DB 버전 일치",r.inputVersionsCurrent?"일치 · 설치·전체 QA 최신성을 보장하지 않음":"불일치 · 현재 초안 재검증 필요"],
                ["예약 시각",date(r.createdAt)],["종료 시각",date(r.completedAt)],["고정 입력 지문",r.snapshotHash]]);
            if(r.errorCode)el(box,"p",P.label(r.errorCode));stepView(box,r.steps);el(box,"p","과거 실행의 통과를 현재 게시 승인으로 사용하지 않습니다.");}
        async function loadQa(id){const n=navigation.read().qaPage||1,data=await request(`${P.base}/${id}/validation-runs?page=${n}&size=10`);
            P.requireValue(P.page(data,n,10,r=>P.run(r,id))&&new Set(data.items.map(r=>r.runId)).size===data.items.length);const box=clear("[data-qa-list]");
            if(!data.items.length)el(box,"p","이 정책의 QA 이력이 없습니다. 초안을 저장하고 QA 준비 상태를 확인하세요.");
            for(const r of data.items){const row=el(box,"article",null,"attachment-evidence-item");el(row,"p",`${P.label(r.statusCode)} · ${date(r.createdAt)} · 정책 버전 ${r.policyVersion}`);link(row,"이 QA 실행의 단계·취소 상태 확인",{policyId:id,runId:r.runId});}
            pages("[data-qa-pages]",data,"qaPage");let runId=navigation.read().runId;
            if(runId){P.requireValue(P.uuid(runId),"QA 실행 ID 형식이 올바르지 않습니다. 정책 목록에서 다시 선택하세요.");const r=await request(`${P.base}/${id}/validation-runs/${runId}`);P.requireValue(r.runId===runId);showRun(r);}
            else if(data.items.length)showRun(data.items[0]);}
        async function loadImpact(d){const i=await request(`${P.base}/${d.policy.policyId}/publication-impact`);P.requireValue(P.impact(i,d));state.impact=i;
            const box=clear("[data-impact]");el(box,"p",`관측 시각: ${date(i.observedAt)} · 현재 외부 요청: 0회 · 공고별 상한: ${i.maximumSourceBytes.toLocaleString("ko-KR")}바이트`);
            if(i.activePolicyForRule){el(box,"h3","이 키워드 규칙의 현재 게시 정책");policyMeta(box,i.activePolicyForRule);}else el(box,"p","이 키워드 규칙에는 현재 게시된 첨부 정책이 없습니다.");
            for(const [title,c]of [["선택한 키워드 규칙 범위",i.matchingRule],["전체 키워드 규칙 범위",i.allRules]]){el(box,"h3",title);meta(box,Object.entries(P.countFields).map(([k,v])=>[v,`${c[k].toLocaleString("ko-KR")}건`]));}
            el(box,"p","두 범위는 겹치므로 합산하지 않습니다. 누적 고정 계획은 현재 실행 수가 아니며 원문·작업·계획도 서로 다른 집계입니다.");
            if(i.wouldStopNewExternalRequests)el(box,"p","게시할 경우 다른 규칙의 고정 작업까지 새 외부 요청이 중지될 수 있습니다. 현재 중지 실행 결과가 아닙니다.");
            if(i.wouldLiftGlobalOffStop)el(box,"p","게시할 경우 현재 OFF의 전역 중지 조건이 해제될 수 있습니다. 작업자 설정·다른 제한은 별도로 적용됩니다.");
            el(box,"h3","게시 차단·재검증 사유");for(const code of i.blockingReasonCodes)el(box,"p",P.label(code));
            if(i.latestQa)el(box,"p",`관측 시점 최신 QA: ${P.label(i.latestQa.statusCode)} / ${i.latestQa.runId}`);
            el(box,"p",`관측 지문: ${i.observedImpactHash}. 정확한 대상 목록의 고정 지문이나 게시 승인 토큰이 아닙니다.`);
        }
        function showScope(s){P.requireValue(P.scope(s,state.detail.policy.policyId));state.scope=s;const x=s.scope,box=clear("[data-scope]");
            meta(box,[["준비 범위 ID",x.scopeId],["고정 정책/규칙 버전",`${x.policyVersion} / ${x.ruleVersion}`],["고정 모드",P.label(x.modeCode)],
                ["혼합 대상 항목",`${x.itemCount.toLocaleString("ko-KR")}개 · 공고 건수와 다름`],["고정 QA 실행",x.qaRunId||"없음 · 게시 불가"],
                ["고정 범위 지문",x.scopeHash],["고정 시각",date(x.createdAt)],["만료 시각",date(x.expiresAt)],
                ["조회 시점 현재성",s.isExpired?"만료 · 새 준비 필요":s.isScopeCurrent?"일치 · 실제 게시 직전 다시 검증":"불일치 · 새 준비 필요"]]);
            el(box,"p","준비는 승인이 아니며 외부 요청은 0회입니다. 준비를 생성한 관리자만 실제 게시할 수 있습니다. 만료·대상 변경은 새 준비가 필요합니다.");}
        async function loadScopes(id){const nav=navigation.read(),n=nav.scopePage||1,data=await request(`${P.base}/${id}/publication-scopes?page=${n}&size=10`);
            P.requireValue(P.page(data,n,10,s=>P.scopeSummary(s,id))&&new Set(data.items.map(s=>s.scopeId)).size===data.items.length);
            const box=clear("[data-scope-list]");if(!data.items.length)el(box,"p","게시 준비 이력이 없습니다. 영향 확인 후 관리자가 범위를 고정할 수 있습니다.");
            for(const s of data.items){const row=el(box,"article",null,"attachment-evidence-item");el(row,"p",`${date(s.createdAt)} · 정책 버전 ${s.policyVersion} · ${s.itemCount.toLocaleString("ko-KR")}개 항목`);
                link(row,"이 준비 범위의 현재성·고정 항목 확인",{scopeId:s.scopeId,scopeItemPage:1});}
            pages("[data-scope-pages]",data,"scopePage");const scopeId=nav.scopeId||data.items[0]?.scopeId;if(!scopeId)return;
            P.requireValue(P.uuid(scopeId),"준비 범위 ID 형식이 올바르지 않습니다. 이력에서 다시 선택하세요.");
            if(!nav.scopeId)navigation.replace({scopeId,scopeItemPage:1});
            const s=await request(`${P.base}/${id}/publication-scopes/${scopeId}`);P.requireValue(P.scope(s,id)&&s.scope.scopeId===scopeId);showScope(s);
            const itemPage=nav.scopeItemPage||1,items=await request(`${P.base}/${id}/publication-scopes/${scopeId}/items?page=${itemPage}&size=20`);
            P.requireValue(P.page(items,itemPage,20,P.scopeItem)&&items.totalCount===s.scope.itemCount&&new Set(items.items.map(i=>i.entityTypeCode+":"+i.entityId)).size===items.items.length);
            const target=clear("[data-scope-items]"),types={POLICY:"정책",SOURCE:"공고 원문",JOB:"작업",COLLECTION_PLAN:"누적 수집 계획",COLLECTOR:"수집원"};
            for(const i of items.items)el(target,"p",`${types[i.entityTypeCode]} / ${i.entityId} / 상태 지문 ${i.stateHash}`);
            pages("[data-scope-item-pages]",items,"scopeItemPage");}
        function showPublication(result){P.requireValue(P.publication(result,state.detail.policy.policyId));state.publication=result;
            const p=result.publication,box=clear("[data-publication-receipt]");meta(box,[["게시 영수증",p.publicationId],["게시 정책 ID / 당시 버전",`${p.policyId} / ${p.publishedPolicyVersion}`],
                ["게시한 모드",P.label(p.modeCode)],["교체한 이전 정책",p.previousPolicyId?`${p.previousPolicyId} / 퇴역 전 버전 ${p.previousPolicyVersion}`:"없음"],
                ["준비 범위 ID / 지문",`${p.scopeId} / ${p.scopeHash}`],["검증 QA 실행",p.qaRunId],["게시 정책 지문",p.policyHash],["게시 시각",date(p.publishedAt)]]);
            el(box,"p","이 영수증은 당시 게시 성공의 불변 이력입니다. 현재 정책은 이후 퇴역했을 수 있습니다. 기존 데이터 적용·작업자 활성 설정·운영 공고 자동 활성화는 수행하지 않았습니다.");
            el(box,"p","게시 API 자체의 외부 요청은 0회지만, 별도 상시 작업자는 게시된 모드에 따라 새 수집을 수행할 수 있습니다. 정책 변경이 필요하면 새 개정·QA·게시 절차를 사용하고, 기존 데이터 복구는 해당 작업의 별도 복구 절차를 따릅니다.");}
        async function loadPublication(id){try{const result=await request(`${P.base}/${id}/publication`);showPublication(result);}
            catch(e){if(e.status!==404)throw e;el(clear("[data-publication-receipt]"),"p","이 정책의 V77 게시 영수증이 없습니다. 과거 게시 또는 미게시 여부는 정책 상태와 별도로 확인하세요.");}}
        async function loadDetail(id,{fill=true}={}){P.requireValue(P.uuid(id),"정책 ID 형식이 올바르지 않습니다. 목록에서 다시 선택하세요.");stale=true;state.run=null;state.impact=null;state.scope=null;state.publication=null;
            clear("[data-run]");clear("[data-qa-list]");clear("[data-qa-pages]");clear("[data-impact]");
            for(const selector of ["[data-scope-list]","[data-scope-pages]","[data-scope]","[data-scope-items]","[data-scope-item-pages]","[data-publication-receipt]"])clear(selector);
            const d=await request(`${P.base}/${id}`);P.requireValue(P.details(d)&&d.policy.policyId===id);showDetail(d,fill);
            const errors=[];for(const task of [()=>loadQa(id),()=>loadImpact(d),()=>loadScopes(id),()=>loadPublication(id)])try{await task();}catch(e){errors.push(e.message);}
            if(errors.length)throw new Error(`정책 상세는 조회했으나 일부 자료는 미확인입니다: ${errors.join(" / ")}`);stale=false;
        }
        function arm(kind){if(locked()||!admin||stale)return;try {
            if(kind==="save")kind=creating?"create":"update";
            if(["create","update"].includes(kind))P.requireValue(!retiredRules.has(values().ruleReleaseId),"선택한 키워드 규칙이 퇴역했습니다. 현재 초안 또는 게시 중인 규칙으로 변경하세요.");
            if(kind!=="create"&&kind!=="update")P.requireValue(!dirty,"초안 입력을 저장하거나 명시적으로 복원한 뒤 이 작업을 준비하세요.");
            const planned=P.command(kind,state,{...values(),...Object.fromEntries(consentNames.map(n=>[n,true]))},"영향 확인용",true);action={kind,input:values(),planned};ack.checked=false;
            for(const n of consentNames)approval.elements.namedItem(n).checked=false;
            const box=clear("[data-action-impact]");el(box,"p",`대상: ${planned.policyId||"새 정책 초안"}${planned.runId&&kind==="cancel"?` / QA ${planned.runId}`:""}`);
            const descriptions={create:"새 정책 초안만 만듭니다. 운영 정책·원문·기존 데이터는 바꾸지 않습니다.",update:"조회 버전의 초안만 수정합니다. 저장한 시스템 수집 방식·설치 기준과 이전 QA의 일치 여부를 다시 확인해야 합니다.",
                revision:"원본을 보존하고 새 개정 초안을 만듭니다. 게시 상태·QA 성공은 복사하지 않습니다.",qa:"현재 저장된 초안으로 QA를 예약합니다. 현재 구현은 분류·합성 격리 실행이며 외부 공고 요청은 0회입니다. 전역 1개, 같은 정책은 60초 간격·24시간 최대 3회이고 실패·취소도 포함됩니다. 필수 증거가 없으면 게시할 수 없습니다.",
                cancel:"선택한 QA만 취소 요청합니다. 실행 중이면 현재 파일의 제한된 처리·정리가 끝날 때까지 기다립니다. 정책·수집 배치·운영 데이터는 취소하지 않습니다.",
                prepare:"현재 정책·규칙과 전체 보호 대상 ID/상태를 10분 유효한 준비 원장으로 고정합니다. 승인·정책 게시·외부 수집·기존 데이터 적용은 하지 않습니다.",
                publish:"실제 게시 요청입니다. 서버가 전체 QA와 설치·현재 범위를 재검증한 뒤 같은 규칙의 이전 ACTIVE 정책을 퇴역시키고 이 초안을 게시합니다. 기존 고정 작업의 정책은 보존하고 기존 데이터는 일괄 적용하지 않습니다."};
            el(box,"p",descriptions[kind]);if(planned.payload.expectedVersion!=null)el(box,"p",`확인한 ${kind==="cancel"?"QA 실행":"정책"} 버전: ${planned.payload.expectedVersion}`);
            if(["create","update"].includes(kind))meta(box,[["저장할 키워드 규칙",planned.payload.ruleReleaseId],["저장할 초안 모드",P.label(planned.payload.modeCode)],["공고별 상한",`${planned.payload.maximumSourceBytes.toLocaleString("ko-KR")}바이트`]]);
            if(kind==="publish"){const s=state.scope.scope,i=state.impact;meta(box,[["실제로 게시할 모드",P.label(planned.modeCode)],["고정 범위 ID",s.scopeId],["고정 범위 지문",s.scopeHash],["고정 항목 수",`${s.itemCount}개 · 공고/작업/정책/계획/수집원 혼합`],["유효기간",date(s.expiresAt)],["연결 QA",s.qaRunId],
                ["현재 동일 규칙 게시 정책",i.activePolicyForRule?.policyId||"없음"],["공고별 누적 다운로드 상한",`${i.maximumSourceBytes.toLocaleString("ko-KR")}바이트`]]);
                el(box,"p",i.wouldStopNewExternalRequests?"OFF 게시로 다른 규칙의 고정 작업을 포함한 새 외부 요청이 중지될 수 있습니다.":i.wouldLiftGlobalOffStop?"현재 OFF의 전역 중지가 해제될 수 있습니다. 별도 작업자 설정·제한은 유지됩니다.":"새 수집에는 게시한 모드가 적용됩니다. 기존 고정 작업·다른 제한은 유지됩니다.");
                el(box,"p","게시 요청 자체의 외부 요청은 0회이며 향후 상시 수집 총량을 뜻하지 않습니다. 게시 취소 버튼으로 되돌릴 수 없으며 새 개정·QA·게시로 정책을 변경합니다. 기존 데이터 처리·복구는 별도 승인 작업입니다.");}
            q("[data-approval]").hidden=false;q("[data-approval-title]").focus();gates();
        }catch(e){error(e);}}
        async function submit(retry=false){if(busy||mutation.pending||!admin)return;if(!retry&&(!action||mutation.uncertain))return;
            let cmd;try{if(!retry){if(!approval.reportValidity())return;cmd=P.command(action.kind,state,{...action.input,...consents()},reason.value,ack.checked);}}catch(e){error(e);gates();return;}
            busy=true;state.reconciliation=null;q("[data-reconcile-ack]").checked=false;q("[data-error]").hidden=true;note("요청 중입니다. 응답을 확인하기 전에는 성공으로 처리하지 않습니다.");gates();
            try {const result=await mutation.execute(cmd);disarm();reason.value="";dirty=false;const box=clear("[data-receipt]");
                if(result.sent.kind==="prepare"){el(box,"p",`준비 범위 ${result.data.scope.scopeId} / ${result.data.scope.itemCount}개 항목을 고정했습니다. 실제 정책 게시·승인은 아닙니다.`);showScope(result.data);navigation.replace({scopeId:result.data.scope.scopeId,scopePage:1,scopeItemPage:1});}
                else if(result.sent.kind==="publish"){showPublication(result.data);el(box,"p",`게시 영수증 ${result.data.publication.publicationId} / 정책 ${result.data.publication.policyId} / 당시 버전 ${result.data.publication.publishedPolicyVersion}을 확인했습니다. 기존 데이터는 적용하지 않았습니다.`);}
                else if(["qa","cancel"].includes(result.sent.kind)){el(box,"p",`확인된 QA ${result.data.runId}: ${P.label(result.data.statusCode)}. 예약·취소 요청은 전체 QA 또는 운영 게시 완료가 아닙니다.`);showRun(result.data);navigation.replace({runId:result.data.runId});}
                else{el(box,"p",`응답에서 확인한 정책: ${result.data.policy.policyId} / 조회 버전 ${result.data.policy.rowVersion}. 초안 요청은 운영 게시·활성화가 아닙니다. 재시도 응답은 현재 정책 상태일 수 있습니다.`);showDetail(result.data);navigation.replace({policyId:result.data.policy.policyId,runId:null,qaPage:1,scopeId:null,scopePage:1,scopeItemPage:1});}
                q("[data-receipt]").focus();note("서버 응답을 확인했습니다. 후속 최신 조회는 별도로 진행합니다.");
                await loadDetail(state.detail.policy.policyId);await loadList();note(result.sent.kind==="publish"?"게시 영수증과 현재 정책을 각각 확인했습니다. 기존 데이터 일괄 적용은 별도입니다.":"요청 응답과 현재 상태를 확인했습니다. 이번 요청으로 정책 게시·기존 데이터 적용은 실행하지 않았습니다.");
            }catch(e){stale=true;error(e);note(mutation.uncertain?"변경 결과 미확정 · 같은 요청 재확인 또는 버전 기반 요청의 현재 상태 확인이 필요합니다.":"요청 또는 후속 조회 실패 · 확인된 응답과 입력을 보존합니다. 최신 상태를 확인하세요.");}
            finally{busy=false;gates();}}
        async function inspect(){if(busy||mutation.pending||!mutation.uncertain||mutation.sent.keyed)return;busy=true;state.reconciliation=null;q("[data-reconcile-ack]").checked=false;gates();
            try{const s=mutation.sent,current=await request(s.kind==="update"?`${P.base}/${s.policyId}`:`${P.base}/${s.policyId}/validation-runs/${s.runId}`);
                P.requireValue(s.kind==="update"?P.details(current)&&current.policy.policyId===s.policyId&&current.policy.rowVersion>=s.payload.expectedVersion:P.run(current,s.policyId)&&current.runId===s.runId&&current.rowVersion>=s.payload.expectedVersion);
                state.reconciliation=current;const box=clear("[data-reconcile-result]");el(box,"p","원래 요청이 성공했는지는 확정하지 않습니다. 아래는 현재 조회한 상태이며, 별도 확인 후 새로운 검토 기준으로만 채택할 수 있습니다.");
                if(s.kind==="update"){policyMeta(box,current.policy);el(box,"p",`현재 공고별 한도: ${current.configuration.maximumSourceBytes.toLocaleString("ko-KR")}바이트`);}else{el(box,"p",`QA ${current.runId} · 버전 ${current.rowVersion} · ${P.label(current.statusCode)}`);}
            }catch(e){error(e);}finally{busy=false;gates();}}
        function reconcile(){if(busy||!admin)return;try{const c=state.reconciliation,kind=mutation.sent?.kind;mutation.reconcile(c,q("[data-reconcile-ack]").checked);
            if(kind==="update"){showDetail(c,false);dirty=true;}else showRun(c);state.reconciliation=null;disarm();stale=true;clear("[data-reconcile-result]");
            note("이전 요청의 성공 여부는 미확정으로 남깁니다. 입력은 보존했습니다. 선택 정책을 다시 조회한 후 현재 기준으로 재검토하세요.");gates();q("[data-refresh]").focus();
        }catch(e){error(e);}}
        function reset(newDraft=false){if(locked()||newDraft&&!admin)return;if((dirty||reason.value)&&!confirmDiscard("저장하지 않은 초안 입력과 사유를 버리시겠습니까? 서버에 저장된 정책과 QA 이력은 바뀌지 않습니다."))return;
            disarm();reason.value="";dirty=false;
            if(newDraft){state.detail=null;state.run=null;state.impact=null;state.scope=null;state.publication=null;creating=true;stale=false;fillRules("");field("modeCode").value="OFF";field("maximumSourceBytes").value="83886080";
                for(const s of ["[data-detail]","[data-run]","[data-qa-list]","[data-qa-pages]","[data-impact]","[data-scope-list]","[data-scope-pages]","[data-scope]","[data-scope-items]","[data-scope-item-pages]","[data-publication-receipt]"])clear(s);navigation.replace({policyId:null,runId:null,scopeId:null,scopePage:1,scopeItemPage:1});}
            else if(state.detail)showDetail(state.detail);gates();}
        form.addEventListener("submit",e=>{e.preventDefault();if(form.reportValidity())arm("save");});form.addEventListener("input",()=>{if(locked())return;dirty=true;disarm();gates();});
        approval.addEventListener("submit",e=>{e.preventDefault();return submit();});q("[data-retry]").addEventListener("click",()=>submit(true));
        q("[data-inspect]").addEventListener("click",inspect);q("[data-reconcile]").addEventListener("click",reconcile);
        for(const [selector,kind]of [["[data-revision]","revision"],["[data-qa]","qa"],["[data-cancel-qa]","cancel"],["[data-prepare]","prepare"],["[data-publish]","publish"]])q(selector).addEventListener("click",()=>arm(kind));
        q("[data-new]").addEventListener("click",()=>reset(true));q("[data-reset]").addEventListener("click",()=>reset());q("[data-close-approval]").addEventListener("click",()=>{if(!locked()){disarm(true);gates();}});
        q("[data-list-refresh]").addEventListener("click",()=>read(loadList));q("[data-rule-refresh]").addEventListener("click",()=>read(loadRules));
        q("[data-refresh]").addEventListener("click",()=>state.detail&&read(()=>loadDetail(state.detail.policy.policyId,{fill:!dirty})));
        q("[data-filter]").addEventListener("submit",e=>{e.preventDefault();if(locked())return;navigation.replace({status:q("[data-filter-status]").value||null,listPage:1});return read(loadList);});
        return {state,mutation,arm,submit,inspect,reconcile,reset,get dirty(){return dirty||!!reason.value||mutation.pending||mutation.uncertain;},
            async start(){q("[data-filter-status]").value=navigation.read().status||"";await read(async()=>{const errors=[];for(const task of [loadList,loadRules])try{await task();}catch(e){errors.push(e.message);}
                if(navigation.read().policyId)try{await loadDetail(navigation.read().policyId);}catch(e){errors.push(e.message);}else{creating=admin;stale=false;}
                if(errors.length)throw new Error(errors.join(" / "));});},gates};
    }
    if(typeof module!=="undefined"&&module.exports)module.exports={mount};
    else{const page=root.document?.querySelector("[data-attachment-policies]");if(!page)return;const P=root.SanebAttachmentPolicy;
        const navigation={read:()=>{const p=new URL(root.location.href).searchParams,out={};for(const k of ["listPage","rulePage","qaPage","scopePage","scopeItemPage"]){const n=Number(p.get(k));if(Number.isSafeInteger(n)&&n>=1&&n<=1000000)out[k]=n;}
            for(const k of ["policyId","runId","scopeId"])if(p.has(k))out[k]=p.get(k);if(["DRAFT","ACTIVE","RETIRED"].includes(p.get("status")))out.status=p.get("status");return out;},
            url:patch=>{const p=new URLSearchParams();for(const[k,v]of Object.entries({...navigation.read(),...patch}))if(v!=null)p.set(k,String(v));return `/app/admin/announcement-attachment-policies?${p}`;},
            replace:patch=>root.history.replaceState(null,"",navigation.url(patch))};
        const app=mount({page,P,request:P.client(root.fetch.bind(root)),doc:root.document,uuid:()=>root.crypto.randomUUID(),navigation,confirmDiscard:message=>root.confirm(message)});
        root.addEventListener("beforeunload",e=>{if(app.dirty){e.preventDefault();e.returnValue="";}});app.start();}
})(globalThis);
