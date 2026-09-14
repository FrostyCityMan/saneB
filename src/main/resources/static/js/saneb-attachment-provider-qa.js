/* 관리자 실파일 QA 화면. 조회, 분할 승인, 실제 예약, 취소 요청과 종료를 구분한다. */
((root)=>{
    "use strict";
    function mount({page,P,Q,request,doc,uuid,navigation}) {
        const q=s=>page.querySelector(s),admin=page.dataset.isAdmin==="true",form=q("[data-approval-form]"),field=n=>form.elements.namedItem(n);
        const checks=["acknowledgeScope","acknowledgeNetworkBudget","acknowledgeIncompleteCoverage","acknowledgeCancellation"];
        const state={nav:null,plan:null,run:null,inspection:null,busy:false,action:null},mutation=Q.mutations(request,uuid,P);
        const path=()=>`${P.base}/${state.nav.policyId}/provider-qa-runs`;
        const el=(box,tag,text,css)=>{const e=doc.createElement(tag);if(text!=null)e.textContent=String(text);if(css)e.className=css;box.append(e);return e;};
        const clear=s=>{const box=q(s);box.replaceChildren();return box;};
        const meta=(box,pairs)=>{const dl=el(box,"dl",null,"attachment-meta");for(const[k,v]of pairs){el(dl,"dt",k);el(dl,"dd",v);}};
        const date=v=>v?new Date(v).toLocaleString("ko-KR",{timeZone:"Asia/Seoul",hour12:false})+" (서울)":"없음";
        const note=v=>q("[data-status]").textContent=v;
        const locked=()=>state.busy||mutation.pending||mutation.uncertain;
        function error(e){const message=e instanceof P.RequestError
            ?e.status===401?"로그인이 만료됐습니다. 이 탭을 유지하고 다른 탭에서 로그인한 뒤 원래 요청을 재확인하세요."
                :e.status===403?"예약·취소 권한 또는 CSRF 확인이 유효하지 않습니다. 활성 관리자 계정·비밀번호 변경·로그인 상태를 확인하세요."
                :e.status===409?"정책·실행 버전·계획·설치 또는 실행 제한이 현재 조건과 충돌합니다. 조회한 대상과 서버 상태를 확인하세요. 미확정 요청은 새 키로 바꾸지 마세요."
                :e.status===404?"선택한 정책 또는 실행을 찾을 수 없습니다. 정책 관리에서 대상과 이력을 확인하세요."
                :"서버 응답을 확인하지 못했습니다. 변경 요청이었다면 결과는 미확정입니다. 원래 요청을 재확인하세요."
            :e instanceof TypeError?"화면 응답 계약을 처리하지 못했습니다. 서버·화면 버전을 확인하세요.":e.message;
            q("[data-error]").textContent=message;q("[data-error]").hidden=false;q("[data-error]").focus();}
        function disarm(focus=false){const kind=state.action?.kind;state.action=null;for(const name of checks)field(name).checked=false;q("[data-approval]").hidden=true;
            if(focus)q(kind==="cancel"?"[data-cancel]":"#provider-qa-plan").focus();}
        function gates(){const frozen=locked(),reserve=state.action?.kind==="reserve",cancel=state.action?.kind==="cancel";
            page.setAttribute("aria-busy",String(state.busy||mutation.pending));
            for(const s of ["[data-plan-refresh]","[data-runs-refresh]"])q(s).disabled=frozen||!state.nav;
            q("[data-run-refresh]").disabled=frozen||!state.nav?.runId;
            q("[data-cancel]").disabled=frozen||!admin||!state.run||!["READY","RUNNING"].includes(state.run.statusCode);
            q("[data-cancel-condition]").textContent=!admin?"조회 전용 계정입니다.":!state.run?"최신 실행을 선택해야 취소할 수 있습니다.":["READY","RUNNING"].includes(state.run.statusCode)?"취소 직전 서버가 실행 버전을 다시 확인합니다. 이미 수행한 외부 요청은 되돌리지 않습니다.":"대기·실행 중인 QA만 취소할 수 있습니다. 취소 요청 중이면 소유자의 정리가 끝날 때까지 이력을 확인하세요.";
            q("[data-reservation-condition]").textContent=!admin?"조회 전용 계정은 예약할 수 없습니다.":!state.plan?"유효한 설치·DRAFT 정책의 실행 계획을 조회해야 예약할 수 있습니다.":!state.plan.isReservationEnabled?"실행 설정이 꺼져 있거나 실행 가능한 기대값이 없습니다. 예약할 수 없습니다.":"분할을 선택하고 범위·요청 예산을 확인하세요. 이 확인은 전체 QA/정책 게시 승인이 아닙니다.";
            for(const [selector,isPlan]of [["[data-plan-pages]",false],["[data-run-pages]",false],["[data-case-pages]",false],["[data-runs]",false],["[data-segments]",true]])
                for(const button of q(selector).querySelectorAll("button"))button.disabled=frozen||(isPlan&&(!admin||!state.plan?.isReservationEnabled));
            q("[data-approval-fields]").disabled=frozen||!admin||!state.action;q("[data-submit]").disabled=frozen||!admin||!state.action;q("[data-close]").disabled=frozen;
            q("[data-reserve-checks]").hidden=!reserve;q("[data-reserve-checks]").disabled=frozen||!reserve||!admin;
            q("[data-incomplete-label]").hidden=!reserve||state.action.plan.isExpectationCoverageComplete;
            field("acknowledgeIncompleteCoverage").required=reserve&&!state.action.plan.isExpectationCoverageComplete;
            q("[data-cancel-check]").hidden=!cancel;field("acknowledgeCancellation").required=cancel;field("acknowledgeCancellation").disabled=!cancel||frozen;
            q("[data-submit]").textContent=cancel?"확인한 실행의 취소 요청":"확인한 분할 QA 예약";
            q("[data-uncertain]").hidden=!mutation.uncertain;q("[data-retry]").disabled=state.busy||mutation.pending||!mutation.uncertain;
            const cancellable=mutation.uncertain&&mutation.sent?.kind==="cancel"&&!state.busy&&!mutation.pending;
            q("[data-inspect]").disabled=!cancellable;q("[data-reconcile-ack]").disabled=!cancellable||!state.inspection;q("[data-reconcile]").disabled=!cancellable||!state.inspection;
        }
        function pages(selector,p,key,loader){const box=clear(selector);el(box,"p",`${p.page} / ${p.totalPages}페이지 · 현재 ${p.items.length}건 / 전체 ${p.totalCount}건`);
            for(const[n,label]of [[p.page-1,"이전 페이지"],[p.page+1,"다음 페이지"]])if(n>=1&&n<=p.totalPages){const b=el(box,"button",label,"secondary-action");b.type="button";b.addEventListener("click",()=>read(async()=>{state.nav[key]=n;saveNav();await loader();}));}}
        function saveNav(){navigation.replace(state.nav);}
        function budget(box,s){meta(box,[["이번 분할",`${s.ordinal}번`],["전체 공고 수",`${s.caseCodes.length}건`],["최대 외부 요청",`${s.maximumRequests.toLocaleString("ko-KR")}회`],
            ["최대 다운로드",`${s.maximumBytes.toLocaleString("ko-KR")}바이트`],["여유 포함 최대 시간",`${s.maximumSecondsIncludingMargin.toLocaleString("ko-KR")}초`]]);
            const codes=el(box,"details");el(codes,"summary",`이번 분할의 전체 공고 코드 ${s.caseCodes.length}개`);const ul=el(codes,"ul");s.caseCodes.forEach(c=>el(ul,"li",c));}
        async function loadPlan(){state.plan=null;clear("[data-plan]");clear("[data-segments]");clear("[data-plan-pages]");
            const d=await request(`${path()}/execution-plan?page=${state.nav.planPage}&size=${Q.size}`);P.requireValue(Q.preview(d,state.nav.policyId,state.nav.planPage,P),"계획의 정책·지문·분할·예산·전체 건수가 일치하지 않습니다. 최신 계획 첫 페이지를 조회하세요.");
            state.plan=d;const box=q("[data-plan]");meta(box,[["정책 ID / 조회 버전",`${d.policyId} / ${d.policyVersion}`],["전체 수집원",`${d.targetCount}곳`],
                ["전체 표본 / 실행 기대값",`${d.catalogCaseCount}건 / ${d.executableCaseCount}건`],["전체 기대값 준비",d.isExpectationCoverageComplete?"준비됨 · 실제 QA는 별도":"미완료 · 일부 분할만 검증 가능"],
                ["전체 분할",`${d.segments.totalCount}개`]]);
            const hashes=el(box,"details");el(hashes,"summary","고정 입력·표본·계획 지문");meta(hashes,[["입력 지문",d.snapshotHash],["표본 지문",d.catalogHash],["계획 지문",d.planHash]]);
            for(const s of d.segments.items){const article=el(q("[data-segments]"),"article",null,"attachment-evidence-item");el(article,"h3",`${s.ordinal}번 QA 분할`);budget(article,s);
                const b=el(article,"button",`${s.ordinal}번 분할 예약 영향 확인`,"secondary-action");b.type="button";b.addEventListener("click",()=>arm("reserve",s.ordinal));}
            if(!d.segments.items.length)el(q("[data-segments]"),"p",d.segments.totalCount?"이 페이지에는 실행 분할이 없습니다. 최신 계획 첫 페이지를 조회하세요.":"실행 가능한 공고 기대값이 없습니다. 표본·역할·내용 기대값과 시스템 연결을 먼저 준비해야 합니다.");
            pages("[data-plan-pages]",d.segments,"planPage",loadPlan);
        }
        function showRun(box,r){meta(box,[["실행 ID",r.runId],["실행 상태",Q.label(r.statusCode)],["실행 조회 버전",r.rowVersion],["고정 정책 버전",r.policyVersion],
            ["분할",r.segmentNo===null?"과거 이력 · 승인 계획 metadata 없음":`${r.segmentNo} / ${r.segmentCount}`],["이번 공고 수",`${r.expectedCaseCount}건`],
            ["요청 예약량 / 상한",`${r.requestReservations} / ${r.maximumRequests}회`],["다운로드 예약량 / 상한",`${r.reservedBytes} / ${r.maximumBytes}바이트`],
            ["입력 DB 버전",r.isInputVersionsCurrent?"현재 조회와 일치 · 코드/실제 QA 통과 아님":"변경됨 · 과거 고정 실행 이력"],
            ["예약 / 만료",`${date(r.createdAt)} / ${date(r.expiresAt)}`],["종료 시각",date(r.completedAt)]]);
            el(box,"p","실행 완료와 정책 전체 검증·게시·기존 데이터 적용은 별도입니다.");}
        async function loadRuns(){clear("[data-runs]");clear("[data-run-pages]");const d=await request(`${path()}?page=${state.nav.runPage}&size=${Q.size}`);
            P.requireValue(P.page(d,state.nav.runPage,Q.size,r=>Q.run(r,state.nav.policyId,P))&&new Set(d.items.map(r=>r.runId)).size===d.items.length,"실행 이력의 정책·상태·페이지 응답이 일치하지 않습니다.");
            for(const r of d.items){const article=el(q("[data-runs]"),"article",null,"attachment-evidence-item");el(article,"p",`${Q.label(r.statusCode)} · ${r.runId} · ${date(r.createdAt)}`);
                const b=el(article,"button","이 실행·항목 조회","secondary-action");b.type="button";b.addEventListener("click",()=>read(async()=>{state.nav.runId=r.runId;state.nav.casePage=1;saveNav();await loadRun();q("#provider-qa-run").focus();}));}
            if(!d.items.length)el(q("[data-runs]"),"p","이 페이지의 실행 이력이 없습니다. 아직 예약하지 않은 상태는 실패나 통과가 아닙니다.");pages("[data-run-pages]",d,"runPage",loadRuns);}
        async function loadCases(){clear("[data-cases]");clear("[data-case-pages]");if(!state.run)return;
            const d=await request(`${path()}/${state.run.runId}/cases?page=${state.nav.casePage}&size=${Q.size}`);
            P.requireValue(P.page(d,state.nav.casePage,Q.size,i=>Q.item(i,P))&&d.totalCount===state.run.expectedCaseCount
                &&d.items.every((i,n)=>i.ordinal===(state.nav.casePage-1)*Q.size+n+1)&&new Set(d.items.map(i=>i.caseId)).size===d.items.length,"항목 페이지의 전체 공고 수·순번·상태가 선택한 실행과 다릅니다.");
            for(const i of d.items){const row=el(q("[data-cases]"),"article",null,"attachment-evidence-item");el(row,"h3",`${i.ordinal}. ${i.caseCode}`);
                meta(row,[["상태",Q.label(i.statusCode)],["기대 파일 수 (추출 성공 수 아님)",i.expectedFileCount],["요청 / 바이트 예약량",`${i.requestReservations}회 / ${i.reservedBytes}바이트`],
                    ["오류 코드",i.errorCode||"없음"],["시작 / 종료",`${date(i.startedAt)} / ${date(i.completedAt)}`]]);}
            pages("[data-case-pages]",d,"casePage",loadCases);}
        async function loadRun(){state.run=null;clear("[data-run]");clear("[data-cases]");clear("[data-case-pages]");
            const r=await request(`${path()}/${state.nav.runId}`);P.requireValue(Q.run(r,state.nav.policyId,P)&&r.runId===state.nav.runId,"선택한 실행의 정책·ID·상태가 응답과 다릅니다.");
            state.run=r;showRun(q("[data-run]"),r);await loadCases();}
        async function read(task){if(locked()||!state.nav)return;state.busy=true;disarm();q("[data-error]").hidden=true;note("조회 중입니다. 새 QA 실행을 예약하지 않습니다.");gates();
            try{await task();note("조회 완료. 실행 예약·취소는 별도 영향 확인 후 수행합니다.");}catch(e){error(e);note("일부 조회 미완료. 확인되지 않은 영역을 성공으로 판단하지 않습니다.");}finally{state.busy=false;gates();}}
        function arm(kind,ordinal){if(locked())return;try{P.requireValue(admin,"예약·취소는 활성 관리자만 실행할 수 있습니다.");disarm();
                const impact=clear("[data-impact]");if(kind==="reserve"){const s=state.plan?.segments.items.find(v=>v.ordinal===ordinal);
                    P.requireValue(state.plan?.isReservationEnabled&&s,"현재 실행 가능한 계획 분할을 조회하세요.");state.action={kind,plan:structuredClone(state.plan),segment:structuredClone(s)};
                    budget(impact,s);el(impact,"p",`전체 ${state.plan.targetCount}곳·${state.plan.catalogCaseCount}공고의 일부 분할입니다. 전체 기대값은 ${state.plan.isExpectationCoverageComplete?"준비됨":"미완료"}이며 실제 전체 QA는 별도입니다.`);
                }else{P.requireValue(kind==="cancel"&&state.run&&["READY","RUNNING"].includes(state.run.statusCode),"최신 대기·실행 중인 QA를 선택하세요.");state.action={kind,run:structuredClone(state.run)};showRun(impact,state.run);}
                q("[data-approval-title]").textContent=kind==="reserve"?"이번 분할의 외부 요청 예산 확인":"선택 실행의 취소 영향 확인";q("[data-approval]").hidden=false;gates();q("[data-approval-title]").focus();
            }catch(e){error(e);gates();}}
        async function submit(retry=false){if(state.busy||mutation.pending)return;let next;
            try{P.requireValue(admin,"예약·취소는 활성 관리자만 실행할 수 있습니다.");if(!retry){P.requireValue(!mutation.uncertain&&state.action,"작업 영향을 확인한 뒤 실행하세요.");if(!form.reportValidity())return;
                    const a=state.action;if(a.kind==="reserve"){const input=Object.fromEntries(checks.map(n=>[n,field(n).checked]));input.reason=field("reason").value;
                        next={kind:"reserve",path:path(),policyId:state.nav.policyId,payload:Q.reservation(a.plan,a.segment,input,P),segmentCount:a.plan.segments.totalCount,
                            catalogCaseCount:a.plan.catalogCaseCount,executableCaseCount:a.plan.executableCaseCount,isExpectationCoverageComplete:a.plan.isExpectationCoverageComplete};
                    }else{P.requireValue(field("acknowledgeCancellation").checked,"취소 범위와 실행 중 파일 정리의 영향을 확인하세요.");P.requireValue(field("reason").value.trim().length>0&&field("reason").value.length<=1000,"취소 사유를 공백이 아닌 1~1000자로 입력하세요.");
                        next={kind:"cancel",policyId:state.nav.policyId,runId:a.run.runId,path:`${path()}/${a.run.runId}/cancellation`,payload:{expectedVersion:a.run.rowVersion,reason:field("reason").value.trim()}};}}
                else P.requireValue(mutation.uncertain,"미확정인 원래 요청이 없습니다.");
                state.busy=true;gates();q("[data-error]").hidden=true;note("변경 요청 응답을 확인 중입니다. 중복 요청을 보내지 마세요.");
                const result=await mutation.execute(next);state.run=result.data;state.nav.runId=result.data.runId;state.nav.casePage=1;saveNav();
                disarm();field("reason").value="";showRun(clear("[data-run]"),result.data);clear("[data-cases]");clear("[data-case-pages]");
                el(q("[data-cases]"),"p","선택 실행·항목 다시 조회로 최신 전체 항목을 확인하세요.");const receipt=clear("[data-receipt]");
                el(receipt,"p",result.sent.kind==="reserve"?"고정 분할의 예약 응답을 확인했습니다. 실제 실행·전체 QA 통과 여부는 이력을 확인하세요.":"취소 요청 응답을 확인했습니다. 취소 요청 중이면 소유자의 정리 후 종료 여부를 확인하세요.");showRun(receipt,result.data);receipt.focus();
                note("변경 응답을 확인했습니다. 목록과 항목은 다시 조회하세요. 정책 게시·기존 데이터 적용은 실행하지 않았습니다.");
            }catch(e){error(e);note(mutation.uncertain?"요청 결과 미확정. 원래 요청만 재확인하세요.":"요청을 완료하지 못했습니다. 입력과 확인 내용을 보존했습니다.");}
            finally{state.busy=false;gates();}}
        async function inspect(){if(state.busy||mutation.pending||!mutation.uncertain||mutation.sent?.kind!=="cancel")return;
            state.busy=true;state.inspection=null;clear("[data-inspection]");q("[data-reconcile-ack]").checked=false;gates();
            try{const s=mutation.sent,r=await request(`${P.base}/${s.policyId}/provider-qa-runs/${s.runId}`);P.requireValue(Q.run(r,s.policyId,P)&&r.runId===s.runId&&r.rowVersion>=JSON.parse(s.body).expectedVersion,"최신 취소 대상과 조회 버전이 일치하지 않습니다.");state.inspection=r;showRun(q("[data-inspection]"),r);}
            catch(e){error(e);}finally{state.busy=false;gates();}}
        function reconcile(){try{mutation.reconcile(state.inspection,q("[data-reconcile-ack]").checked,P);state.run=state.inspection;state.inspection=null;disarm();showRun(clear("[data-run]"),state.run);
                note("조회한 현재 상태를 새 검토 기준으로 채택했습니다. 원래 취소 요청의 성공을 확정한 것은 아닙니다.");}catch(e){error(e);}gates();}
        q("[data-plan-refresh]").addEventListener("click",()=>read(async()=>{state.nav.planPage=1;saveNav();await loadPlan();}));
        q("[data-runs-refresh]").addEventListener("click",()=>read(loadRuns));q("[data-run-refresh]").addEventListener("click",()=>state.nav?.runId&&read(loadRun));
        q("[data-cancel]").addEventListener("click",()=>arm("cancel"));q("[data-close]").addEventListener("click",()=>{if(!locked()){disarm(true);gates();}});
        form.addEventListener("submit",e=>{e.preventDefault();return submit();});q("[data-retry]").addEventListener("click",()=>submit(true));
        q("[data-inspect]").addEventListener("click",inspect);q("[data-reconcile]").addEventListener("click",reconcile);
        return {state,mutation,arm,submit,inspect,reconcile,get dirty(){return !!field("reason").value||mutation.pending||mutation.uncertain;},
            async start(){try{state.nav=Q.parameters(navigation.search(),P);q("[data-policy-link]").href=`/app/admin/announcement-attachment-policies?policyId=${state.nav.policyId}`;
                    q("[data-coverage-link]").href=`/app/admin/announcement-attachment-provider-coverage?policyId=${state.nav.policyId}`;
                    await read(async()=>{const errors=[];for(const task of [loadPlan,loadRuns,...(state.nav.runId?[loadRun]:[])])try{await task();}catch(e){errors.push(e);}if(errors.length)throw errors[0];});
                }catch(e){error(e);gates();}}};
    }
    if(typeof module!=="undefined"&&module.exports)module.exports={mount};
    else{const page=root.document.querySelector("[data-provider-qa]");if(!page)return;const P=root.SanebAttachmentPolicy,Q=root.SanebAttachmentProviderQa;
        const app=mount({page,P,Q,request:P.client(root.fetch.bind(root)),doc:root.document,uuid:()=>root.crypto.randomUUID(),
            navigation:{search:()=>root.location.search,replace:nav=>root.history.replaceState(null,"",Q.route+"?"+new URLSearchParams(Object.entries(nav).filter(([,v])=>v!=null)))}});
        root.addEventListener("beforeunload",e=>{if(app.dirty){e.preventDefault();e.returnValue="";}});app.start();}
})(globalThis);
