/* 관리자 배치 작업 화면. 모든 변경은 현재 영향 확인 뒤 단일 요청으로 전송한다. */
((root) => {
    "use strict";
    function mount({page,B,C,request,doc,uuid,navigation}) {
        const q=s=>page.querySelector(s), admin=page.dataset.isAdmin==="true", label=v=>B.label(v,C.label);
        const state={batch:null,preview:null,rollback:null,scope:null,previewItems:[],selected:new Set(),selectionComplete:false,selectionDirty:false};
        const mutation=B.mutations(request,C,uuid), policyOptions=new Map();
        let busy=false,stale=true,action=null,listPage=1,itemPage=1,previewPage=1,rollbackPage=1,receipt=null,casCurrent=null,historyPage=1,historyVersion=null,historyBatchId=null;
        const scopeForm=q("[data-scope-form]"),approval=q("[data-approval-form]"),field=n=>scopeForm.elements.namedItem(n);
        const el=(parent,tag,value,css)=>{const n=doc.createElement(tag);if(value!=null)n.textContent=String(value);if(css)n.className=css;parent.append(n);return n;};
        const clear=s=>{const n=q(s);n.replaceChildren();return n;};
        const meta=(parent,pairs)=>{const dl=el(parent,"dl",null,"attachment-meta");pairs.forEach(([k,v])=>{el(dl,"dt",k);el(dl,"dd",v==null?"미확인":v);});};
        const fmt=v=>B.count(v)?`${v.toLocaleString("ko-KR")}건`:"미확인";
        const date=v=>v?new Date(v).toLocaleString("ko-KR",{timeZone:"Asia/Seoul",hour12:false})+" (서울)":"미확인";
        const note=m=>{q("[data-status]").textContent=m;};
        const error=e=>{q("[data-error]").textContent=e?.message || "처리 결과를 확인하지 못했습니다. 최신 상태를 다시 조회하세요.";q("[data-error]").hidden=false;q("[data-error]").focus();};
        function button(parent,title,fn,disabled=false){const n=el(parent,"button",title,"secondary-action");n.type="button";n.disabled=disabled;n.addEventListener("click",()=>{if(!n.disabled)return fn();});return n;}
        const locked=()=>busy || mutation.pending || mutation.uncertain;
        function gates(){
            q("[data-list-refresh]").disabled=locked();q("[data-policy-refresh]").disabled=locked();
            q("[data-scope-fields]").disabled=locked();q("[data-scope-submit]").disabled=locked();
            q("[data-refresh]").disabled=locked() || !state.batch;
            q("[data-history-refresh]").disabled=locked() || !state.batch;
            q("[data-reserve]").disabled=locked() || !admin || !state.scope?.canReserve;
            q("[data-save-selection]").disabled=locked() || stale || !admin || !B.editable(state.batch,state.preview) || !state.selectionComplete || !state.selectionDirty;
            q("[data-reset-selection]").disabled=locked() || !state.selectionComplete || !state.selectionDirty;
            q("[data-approval-fields]").disabled=locked() || !admin || !action;
            q("[data-approve]").disabled=locked() || !admin || !action;
            q("[data-cancel-approval]").disabled=locked();
            q("[data-uncertain]").hidden=!mutation.uncertain;q("[data-retry]").disabled=busy || mutation.pending;
            const cas=mutation.uncertain && !mutation.sent?.keyed;
            q("[data-cas-read]").hidden=!cas;q("[data-cas-read]").disabled=busy;
            q("[data-cas-confirm]").hidden=!cas || !casCurrent;q("[data-cas-continue]").hidden=!cas || !casCurrent;
            q("[data-cas-continue]").disabled=busy || !q("[data-cas-ack]").checked;
            for(const s of ["[data-list]","[data-list-pages]","[data-policy-pages]","[data-actions]","[data-item-pages]","[data-preview-pages]","[data-rollback-pages]","[data-history]","[data-history-pages]"])
                q(s).querySelectorAll("button").forEach(n=>n.disabled=locked());
            q("[data-preview-items]").querySelectorAll("input").forEach(n=>n.disabled=locked() || stale || !admin || !B.editable(state.batch,state.preview) || n.dataset.eligible!=="true");
        }
        function disarm(){action=null;q("[data-approval]").hidden=true;approval.elements.namedItem("acknowledged").checked=false;}
        async function read(work){if(locked())return;busy=true;q("[data-error]").hidden=true;disarm();gates();
            try{await work();}catch(e){stale=true;error(e);}finally{busy=false;gates();}}
        function pages(selector,p,go){const box=clear(selector);el(box,"p",`전체 ${fmt(p.totalCount)} · ${p.totalPages?`${p.page}/${p.totalPages}페이지`:"0페이지"}`);
            if(p.page>1)button(box,"이전 페이지",()=>read(()=>go(p.page-1)));
            if(p.page<p.totalPages)button(box,"다음 페이지",()=>read(()=>go(p.page+1)));}
        function localPages(selector,pageNo,total,size,go){pages(selector,{page:pageNo,totalCount:total,totalPages:Math.ceil(total/size)},async n=>go(n));}
        async function loadList(p=1){
            const data=await request(`${B.base}?page=${p}&size=10`);B.requireValue(B.validPage(data,p,10,B.validBatch));
            if(!data.items.length && p>1){await loadList(Math.max(1,data.totalPages));return;}
            listPage=p;navigation.write({listPage});const box=clear("[data-list]");
            if(!data.items.length)el(box,"p","예약된 배치가 없습니다. 새 범위를 조회하면 보호 제외와 준비 상태를 확인할 수 있습니다.");
            for(const b of data.items){const row=el(box,"article",null,"attachment-evidence-item");el(row,"p",`${b.batchId} · ${label(b.statusCode)} · 전체 ${fmt(b.itemCount)} / 삭제 ${fmt(b.deletedItemCount)} · ${date(b.createdAt)}`);
                button(row,"이 배치의 단계·결과 확인",()=>{if(state.selectionDirty && state.batch?.batchId!==b.batchId){error(new Error("미저장 선택이 있습니다. 선택을 저장하거나 ‘미저장 선택을 버리고 저장된 선택 복원’ 후 다른 배치를 여세요."));return;}return read(async()=>{itemPage=previewPage=rollbackPage=1;receipt=null;navigation.write({batchId:b.batchId,itemPage:1,previewPage:1,rollbackPage:1,actionId:null,actionKind:null});await loadBatch(b.batchId);q("#batch-work").focus();});});}
            pages("[data-list-pages]",data,loadList);
        }
        async function loadPolicies(p=1){
            const data=await request(`/api/v2/admin/announcement-attachment-policies?status=ACTIVE&page=${p}&size=20`);
            B.requireValue(B.validPage(data,p,20,i=>B.uuid(i.policyId) && i.policyStatusCode==="ACTIVE"));
            const box=clear("[data-policies]");el(box,"p","게시 정책만 표시합니다. 선택은 게시/활성화가 아닙니다. 서버가 현재 규칙과 실행 환경을 다시 검증합니다.");
            if(!data.items.length)el(box,"p","게시 정책이 없습니다. 정책 QA·게시 절차가 필요하며 이 화면에서 자동 게시하지 않습니다.");
            for(const p of data.items){el(box,"p",`${p.policyCode} v${p.versionNo} · ${label(p.modeCode)} · 규칙 ${p.ruleReleaseId} (${label(p.ruleReleaseStatusCode)})`);
                if(p.ruleReleaseStatusCode==="ACTIVE" && ["COLLECT_ONLY","ENFORCE"].includes(p.modeCode))policyOptions.set(p.policyId,p);}
            const select=field("policyId"),old=select.value;select.replaceChildren();const placeholder=el(select,"option","게시 정책을 선택하세요");placeholder.value="";
            for(const p of policyOptions.values()){const o=el(select,"option",`${p.policyCode} v${p.versionNo} · ${label(p.modeCode)} · ${p.policyId}`);o.value=p.policyId;}select.value=old;
            pages("[data-policy-pages]",data,loadPolicies);
        }
        function scopeInput(){const koreanTime=n=>field(n).value?(field(n).value.length===16?field(n).value+":00":field(n).value)+"+09:00":"";
            return B.scope({policyId:field("policyId").value,providerCodes:[...scopeForm.querySelectorAll("input[name=provider]:checked")].map(n=>n.value),
                collectedFrom:koreanTime("collectedFrom"),collectedBefore:koreanTime("collectedBefore"),deadlineFrom:field("deadlineFrom").value,deadlineThrough:field("deadlineThrough").value,maximumCount:field("maximumCount").value});}
        function scopeView(p){const box=clear("[data-scope-result]");
            meta(box,[["전체 범위 후보",fmt(p.candidateCount)],["이번 고정 예정",fmt(p.selectedCount)],["이번 배치 밖 잔여 후보",fmt(p.remainingCount)],
                ["전체 수집 HTTP 상한",`${p.maximumHttpRequests.toLocaleString("ko-KR")}회`],["전체 다운로드 상한",`${p.maximumDownloadBytes.toLocaleString("ko-KR")}바이트`],["이번 조회의 외부 요청","0회"],["예약 준비",p.canReserve?"예약 가능 · 아직 실행하지 않음":"준비 불가 항목 확인"],["범위 지문",p.scopeHash]]);
            for(const c of p.counts)el(box,"p",`${label(c.providerCode)} · ${label(c.reasonCode)}: ${fmt(c.count)}`);
            const details=el(box,"details");el(details,"summary",`이번 예약 대상 ${fmt(p.items.length)}와 준비 사유`);
            for(const i of p.items)el(details,"p",`${i.sourceId} · ${label(i.providerCode)} · ${label(i.readinessCode)}`);
        }
        function batchView(b){const box=clear("[data-detail]");meta(box,[["배치",b.batchId],["상태",label(b.statusCode)],["버전",b.rowVersion],["정책",b.policyId],
            ["최초 전체 고정 범위",fmt(b.itemCount)],["현재 남은 작업",fmt(b.remainingItemCount)],["삭제된 작업",fmt(b.deletedItemCount)],
            ["예약 당시 범위 밖 잔여 후보",fmt(b.frozenScope.remainingCount)],["최초 전체 HTTP 상한",`${b.frozenScope.maximumHttpRequests}회`],
            ["최초 전체 다운로드 상한",`${b.frozenScope.maximumDownloadBytes}바이트`],["고정 범위 지문",b.scopeHash],["생성 시각",date(b.createdAt)]]);
            const f=b.frozenScope.filter; if(f)el(box,"p",`고정 필터: ${f.providerCodes.map(label).join(", ")} · ${date(f.collectedFrom)} 이상 ~ ${date(f.collectedBefore)} 미만 · 마감일 ${f.deadlineFrom||"제한 없음"} ~ ${f.deadlineThrough||"제한 없음"}`);
            if(B.uuid(b.frozenScope.backfillRunId) && B.count(b.frozenScope.backfillSegmentNo) && b.frozenScope.backfillSegmentNo>=1){
                const a=el(box,"a","이 배치의 전체 대상·고정 분할로 돌아가기","secondary-action");
                a.href=`/app/admin/announcement-attachment-backfills?runId=${b.frozenScope.backfillRunId}&segmentNo=${b.frozenScope.backfillSegmentNo}`;
            }
            el(box,"p","수집 상태 집계는 실제 작업 기준입니다. 배치 단계 표시는 작업자 집계 주기까지 지연될 수 있습니다.");
            for(const [k,v]of Object.entries(b.jobCounts))el(box,"p",`${label(k)}: ${fmt(v)}`);
        }
        const hasPreview=s=>["PREVIEW_READY","PREVIEW_PARTIAL_FAILED","APPLYING","APPLY_PAUSED","APPLIED","APPLY_PARTIAL_FAILED","ROLLING_BACK","ROLLED_BACK","ROLLBACK_PARTIAL_FAILED"].includes(s);
        const hasApplication=s=>["APPLYING","APPLY_PAUSED","APPLIED","APPLY_PARTIAL_FAILED","ROLLING_BACK","ROLLED_BACK","ROLLBACK_PARTIAL_FAILED"].includes(s);
        const hasRollback=s=>["APPLIED","APPLY_PARTIAL_FAILED","APPLY_PAUSED","ROLLING_BACK","ROLLED_BACK","ROLLBACK_PARTIAL_FAILED"].includes(s);
        async function loadBatch(id){
            B.requireValue(B.uuid(id));stale=true;const old=state.preview,oldSelection=state.selected,wasDirty=state.selectionDirty;
            if(state.batch?.batchId!==id && receipt?.batchId!==id){receipt=null;el(clear("[data-receipt]"),"p","선택한 배치의 접수 기록을 별도로 조회하세요. 이전 배치의 결과를 이어 표시하지 않습니다.");navigation.write({actionId:null,actionKind:null});}
            if(historyBatchId!==id){historyBatchId=null;historyVersion=null;historyPage=1;clear("[data-history]");clear("[data-history-pages]");navigation.write({historyPage:null,historyVersion:null});}
            state.preview=null;state.rollback=null;state.selectionComplete=false;state.previewItems=[];
            for(const s of ["[data-preview]","[data-preview-items]","[data-preview-pages]","[data-rollback]","[data-rollback-items]","[data-rollback-pages]","[data-actions]"])clear(s);
            const b=await request(`${B.base}/${id}`);B.requireValue(B.validBatch(b) && b.batchId===id);state.batch=b;batchView(b);
            await loadItems(itemPage);
            if(hasPreview(b.statusCode)){
                const p=await request(`${B.base}/${id}/classification-preview`);B.requireValue(B.validPreview(p,b));state.preview=p;
                const items=await B.loadPreviewItems(request,b,p);state.previewItems=items;state.selectionComplete=true;
                state.selected=old?.previewId===p.previewId && old.previewHash===p.previewHash && wasDirty?oldSelection:new Set(items.filter(i=>i.selected).map(i=>i.jobId));
                state.selectionDirty=old?.previewId===p.previewId && wasDirty;previewView();
            }else{state.selected=new Set();state.selectionDirty=false;el(q("[data-preview]"),"p","수집 종료 후 결과 미리보기를 직접 생성하세요. 아직 선택·적용 기준이 없습니다.");}
            if(["APPLIED","APPLY_PARTIAL_FAILED","APPLY_PAUSED"].includes(b.statusCode))await loadRollback();
            else if(hasRollback(b.statusCode)){el(q("[data-rollback]"),"p","원복 승인 이후에는 새 원복 미리보기를 만들지 않습니다. 항목별 결과와 기존 접수 기록을 확인하세요.");await loadRollbackItems(rollbackPage);}
            stale=false;actionsView();if(receipt?.batchId===id)await loadReceipt();
            if(historyBatchId===id)await loadHistory(historyPage,historyVersion);
            note("최신 배치 기준을 조회했습니다. 입력 사유는 유지하며 실행 동의는 다시 확인합니다.");
        }
        async function loadItems(p=1){const b=state.batch,app=hasApplication(b.statusCode),url=`${B.base}/${b.batchId}/${app?"application/items":"items"}`;
            const result=await request(`${url}?page=${p}&size=20`);B.requireValue(B.validPage(result,p,20,B.validItem));
            if(!result.items.length && p>1){await loadItems(Math.max(1,result.totalPages));return;}
            itemPage=p;navigation.write({itemPage});const box=clear("[data-items]");el(box,"h3",app?"항목별 수집·적용 결과":"고정 수집 작업");
            el(box,"p",`현재 조회 시점 작업 ${fmt(result.totalCount)}. 최초 전체/삭제 수는 상단 배치 조회 시점 기준이며 변동 시 최신 배치를 다시 조회하세요.`);
            for(const i of result.items){const row=el(box,"article",null,"attachment-evidence-item");sourceLink(row,i);
                meta(row,[["작업",i.jobId],["수집",label(i.collectionStatusCode||i.statusCode)],["적용",label(i.applicationStatusCode)],["원복",label(i.rollbackStatusCode)],
                    ["오류",i.applicationErrorCode||i.errorCode?label(i.applicationErrorCode||i.errorCode):"없음"],...(app?[["저장된 선택",i.selected?"선택":"미선택"],["적용 시도",i.applicationAttemptCount],["다음 시도",date(i.nextAttemptAt)]]:[])]);}
            pages("[data-item-pages]",result,loadItems);
        }
        function sourceLink(parent,i){const a=el(parent,"a",`공고 ${i.sourceId} 본문·첨부 근거 확인`);a.href=`/app/admin/collected-announcements/${encodeURIComponent(i.sourceId)}/attachments`;}
        function previewView(){const p=state.preview,box=clear("[data-preview]");meta(box,[["미리보기",p.previewId],["저장 당시 상태",label(p.statusCode)],["현재 연결 여부",p.currentPreview?"현재 미리보기":"과거 미리보기"],
            ["현재 입력 일치",p.inputsCurrent?"일치":"변경됨 · 신규 적용 근거로 사용 불가"],["저장 당시 전체/적격/선택",`${fmt(p.itemCount)} / ${fmt(p.eligibleItemCount)} / ${fmt(p.selectedItemCount)}`],
            ["저장 당시 삭제 / 현재 삭제",`${fmt(p.snapshotDeletedItemCount)} / ${fmt(p.currentDeletedItemCount)}`],["지문",p.previewHash],["추가 외부 요청","0회"]]);renderSelection();}
        function renderSelection(){const box=clear("[data-preview-items]"),all=state.previewItems;previewPage=Math.min(previewPage,Math.max(1,Math.ceil(all.length/10)));
            for(const i of all.slice((previewPage-1)*10,previewPage*10)){const row=el(box,"article",null,"attachment-evidence-item"),lab=el(row,"label",null,"attachment-check"),check=el(lab,"input");
                check.type="checkbox";check.dataset.eligible=String(i.eligible);check.checked=state.selected.has(i.jobId);check.disabled=!admin || stale || !B.editable(state.batch,state.preview) || !i.eligible;
                el(lab,"span",`${i.sourceId} · ${label(i.readinessCode)}`);check.addEventListener("change",()=>{if(locked() || stale || !admin || !B.editable(state.batch,state.preview) || !i.eligible)return;
                    if(check.checked)state.selected.add(i.jobId);else state.selected.delete(i.jobId);
                    state.selectionDirty=all.some(v=>state.selected.has(v.jobId)!==v.selected);disarm();selectionState();gates();});
                sourceLink(row,i);const e=i.evidence||{};meta(row,[["작업",i.jobId],["기본 판정",label(e.baseStatus)],["이전 첨부 판정",label(e.previousAttachmentStatus)],["제안 판정",label(e.proposedStatus)],
                    ["제안 사유",label(e.reasonCode)],["정책 모드",label(e.policyMode)],["발견 상태",label(e.discoveryStatus)],["발견/처리/파일 수",`${fmt(e.discoveredCount)} / ${fmt(e.processedCount)} / ${fmt(e.fileCount)}`],
                    ["다운로드 실패 / 추출 불완전 / 역할 미확정",`${fmt(e.downloadFailedCount)} / ${fmt(e.incompleteExtractionCount)} / ${fmt(e.unknownRoleCount)}`]]);
                for(const [prefix,title]of [["base","기본 자동"],["previous","이전 첨부 자동"],["confirmed","이전 관리자 확인"],["proposed","제안 자동"]]){
                    const codes=k=>Array.isArray(e[k])?e[k].map(label).join(", ")||"없음":"미확인";
                    el(row,"p",`${title}: 대상 ${codes(prefix+"TargetCodes")} / 형태 ${codes(prefix+"SupportCodes")}`);
                    if(prefix!=="proposed")for(const [kind,name]of [["Target","대상"],["Support","형태"]])el(row,"p",`${title} 대비 제안 ${name}: 추가 ${codes(prefix+kind+"Added")} / 제거 ${codes(prefix+kind+"Removed")}`);
                }
                const details=el(row,"details");el(details,"summary","규칙·파일 근거 확인");meta(details,[["규칙",e.ruleReleaseId],["규칙 버전",e.ruleVersion],["규칙 지문",e.ruleHash],["집합",e.setId],["평가",e.evaluationId]]);
                if(Array.isArray(e.files))for(const f of e.files)el(details,"p",`${f.fileId} · ${f.format||"형식 미확인"} · ${label(f.role)} · ${label(f.downloadStatus)} · ${label(f.quality)} · 추출 ${f.extractionId||"없음"} · ${label(f.downloadErrorCode||f.extractionErrorCode)}`);
            }
            localPages("[data-preview-pages]",previewPage,all.length,10,n=>{previewPage=n;navigation.write({previewPage});renderSelection();});selectionState();}
        function selectionState(){q("[data-selection-state]").textContent=`전체 페이지의 현재 선택 ${fmt(state.selected.size)} · ${state.selectionDirty?"아직 서버에 저장하지 않음":"저장된 선택과 일치"}`;}
        async function loadRollback(){const b=state.batch,r=await request(`${B.base}/${b.batchId}/rollback/preview`);B.requireValue(B.validRollback(r,b));state.rollback=r;
            const box=clear("[data-rollback]");meta(box,[["최초 전체",fmt(r.scopeCount)],["현재/삭제",`${fmt(r.remainingCount)} / ${fmt(r.deletedCount)}`],["원복 대상/적격/충돌",`${fmt(r.targetCount)} / ${fmt(r.eligibleCount)} / ${fmt(r.conflictCount)}`],
                ["기본 판정 경로 재개",fmt(r.baseReopenCount)],["이전 확인 복구",fmt(r.confirmationRestoreCount)],["오래된 확인 유지",fmt(r.staleConfirmationCount)],["남은 적용 대기 취소",fmt(r.cancelPendingCount)],["추가 외부 요청","0회"],["원복 지문",r.previewHash]]);await loadRollbackItems(rollbackPage);}
        async function loadRollbackItems(p){const data=await request(`${B.base}/${state.batch.batchId}/rollback/items?page=${p}&size=20`);B.requireValue(B.validPage(data,p,20,B.validItem));
            if(!data.items.length && p>1){await loadRollbackItems(Math.max(1,data.totalPages));return;}
            rollbackPage=p;navigation.write({rollbackPage});const box=clear("[data-rollback-items]");
            for(const i of data.items){const row=el(box,"article",null,"attachment-evidence-item");sourceLink(row,i);el(row,"p",`${i.jobId} · 대상 ${i.target?"포함":"아님"} · ${label(i.readinessCode)} · 적용 ${label(i.applicationStatusCode)} · 원복 ${label(i.rollbackStatusCode)} · ${i.errorCode?label(i.errorCode):"오류 없음"}`);}
            pages("[data-rollback-pages]",data,loadRollbackItems);
        }
        const names={reserve:"범위 고정 예약",collection:"첨부 수집 시작","collection-pause":"수집 중지","collection-resume":"수집 재개","scope-cancellation":"수집 전 예약 취소",preview:"봉인 결과 미리보기 생성",selection:"전체 선택 저장",apply:"선택한 판정 적용","apply-pause":"판정 적용 중지","apply-resume":"판정 적용 재개",rollback:"이전 연결 원복 승인"};
        function actionsView(){const box=clear("[data-actions]"),s=state.batch.statusCode;if(!admin){el(box,"p","조회 전용입니다. 실행은 관리자에게 요청하세요.");return;}
            const actions=[];if(s==="SCOPE_READY")actions.push("collection","scope-cancellation");if(["COLLECTING","COLLECTION_PENDING"].includes(s))actions.push("collection-pause");if(s==="COLLECTION_PAUSED")actions.push("collection-resume");
            if(["COLLECTED","COLLECTION_PARTIAL_FAILED","PREVIEW_READY","PREVIEW_PARTIAL_FAILED"].includes(s))actions.push("preview");
            if(B.editable(state.batch,state.preview) && state.preview.selectedItemCount>0)actions.push("apply");if(s==="APPLYING")actions.push("apply-pause");if(s==="APPLY_PAUSED")actions.push("apply-resume");
            if(["APPLIED","APPLY_PARTIAL_FAILED","APPLY_PAUSED"].includes(s) && state.rollback?.eligibleCount>0)actions.push("rollback");
            for(const kind of actions)button(box,`${names[kind]} 영향 확인`,()=>arm(kind));
            if(!actions.length)el(box,"p","현재 가능한 변경 작업이 없습니다. 처리 중이면 최신 배치를 조회하고, 실패·충돌은 항목별 근거를 확인하세요.");}
        function arm(kind){if(locked() || !admin || (stale && kind!=="reserve"))return;
            try{B.command(kind,state,"영향 확인",true);action=kind;approval.elements.namedItem("acknowledged").checked=false;q("[data-approval]").hidden=false;
                const box=clear("[data-impact]");el(box,"h3",names[kind]);
            if(kind==="reserve"){B.requireValue(!state.selectionDirty,"미저장 선택을 저장하거나 명시적으로 복원한 뒤 새 배치를 예약하세요.");scopeView(state.scope);meta(box,[["범위 지문",state.scope.scopeHash],["예약 대상",fmt(state.scope.selectedCount)],["이번 배치 밖 잔여",fmt(state.scope.remainingCount)],["외부 요청","0회"]]);}
                else{meta(box,[["배치",state.batch.batchId],["현재 버전",state.batch.rowVersion],["최초 전체/현재 삭제",`${fmt(state.batch.itemCount)} / ${fmt(state.batch.deletedItemCount)}`],["고정 범위 지문",state.batch.scopeHash]]);
                    if(kind.startsWith("collection"))meta(box,[["최초 전체 HTTP 상한",`${state.batch.frozenScope.maximumHttpRequests}회`],["최초 전체 다운로드 상한",`${state.batch.frozenScope.maximumDownloadBytes}바이트`]]);
                    if(kind==="selection" || kind.startsWith("apply"))meta(box,[["미리보기 지문",state.preview.previewHash],["선택",fmt(kind==="selection"?state.selected.size:state.preview.selectedItemCount)]]);
                    if(kind==="rollback")meta(box,[["대상/적격/충돌",`${fmt(state.rollback.targetCount)} / ${fmt(state.rollback.eligibleCount)} / ${fmt(state.rollback.conflictCount)}`],["기본 경로 재개 / 이전 확인 복구",`${fmt(state.rollback.baseReopenCount)} / ${fmt(state.rollback.confirmationRestoreCount)}`],["적용 대기 취소",fmt(state.rollback.cancelPendingCount)],["원복 지문",state.rollback.previewHash]]);}
                const warnings=kind==="reserve"?"고정 예약만 생성합니다. 아직 파일 수집·판정 적용을 하지 않습니다. 다른 수집과 충돌할 수 있으며 수집 전 예약 취소로 해제합니다.":kind.startsWith("collection")?"수집 시작·재개는 고정 범위의 외부 요청을 허용합니다. 새 예산을 추가하지 않습니다. 중지는 새 요청을 막지만 이미 전송 중인 요청의 즉시 중단을 보장하지 않습니다.":kind==="scope-cancellation"?"아직 수집하지 않은 예약을 취소합니다. 이력은 보존하며 원문을 삭제하지 않습니다.":kind==="preview"?"저장된 봉인 근거만 사용합니다. 새 미리보기의 선택은 0건이며 이전 선택을 자동 복사하지 않습니다. 외부 요청 0회입니다.":kind==="selection"?"전체 페이지의 선택 목록을 새 이력으로 저장합니다. 판정은 적용하지 않습니다. 외부 요청 0회입니다.":kind==="rollback"?"적용 완료분 전체의 조건부 원복을 접수합니다. 이전 판정과 유효했던 확인만 복구하며 남은 적용 대기는 취소됩니다. 실패·충돌은 남고 외부 요청은 0회입니다.":"적용/중지/재개 요청을 접수합니다. 적용은 고정 ACTIVE ENFORCE 정책에서만 가능하며 이전 첨부 확인은 STALE이 되어 재검수가 필요합니다. COLLECT_ONLY를 승격하지 않습니다. 외부 요청 0회이며 운영 공고를 자동 활성화하지 않습니다.";
                el(box,"p",warnings);q("[data-approve]").textContent=names[kind];q("#batch-approval-title").focus();gates();
            }catch(e){disarm();gates();error(e);}}
        async function submit(retry=false){if(busy || mutation.pending || !admin || (!retry && mutation.uncertain))return;
            let next;try{if(!retry){B.requireValue(action,"실행할 작업의 영향을 먼저 확인하세요.");if(!approval.reportValidity())return;next=B.command(action,state,approval.elements.namedItem("reason").value,approval.elements.namedItem("acknowledged").checked);}}
            catch(e){error(e);return;}busy=true;gates();q("[data-error]").hidden=true;
            try{const result=await mutation.execute(next);const sent=result.sent,data=result.data;receipt=null;
                if(data.actionId){receipt={batchId:sent.batchId,actionId:data.actionId,kind:sent.kind==="rollback"?"rollback":"application"};navigation.write({actionId:receipt.actionId,actionKind:receipt.kind});receiptView(data,receipt.kind);}
                else{navigation.write({actionId:null,actionKind:null});el(clear("[data-receipt]"),"p",`${names[sent.kind]} 응답 확인 · 배치 ${data.batchId}. 원문 처리 완료 여부는 최신 단계·항목 결과에서 확인하세요.`);}
                disarm();stale=true;state.scope=null;clear("[data-scope-result]");
                const id=sent.kind==="reserve"?data.batchId:sent.batchId;navigation.write({batchId:id});if(B.validBatch(data))state.batch=data;
                await loadBatch(id);await loadList(listPage);
                note("요청 응답과 최신 상태를 조회했습니다. 접수는 비동기 처리 완료를 뜻하지 않습니다.");
            }catch(e){stale=true;error(e);note(mutation.uncertain?"처리 결과 미확정 · 원래 요청 재확인 필요":"요청 또는 최신 조회 실패 · 입력을 유지하고 최신 배치를 다시 조회하세요.");}
            finally{busy=false;gates();}}
        function receiptView(data,kind){const box=clear("[data-receipt]");el(box,"p","접수 기록입니다. 아래 처리 건수는 조회 시점의 현재 상태이며 최초 접수의 완료 보장이 아닙니다.");
            meta(box,[["배치",data.batchId],["접수 식별자",data.actionId],["접수 시각",date(data.acceptedAt)],["현재 단계",label(data.currentStatusCode||data.statusCode)],
                ["최초 전체",fmt(data.scopeItemCount??data.scopeCount)],["승인된 선택/대상",fmt(data.approvedSelectedCount??data.approvedTargetCount)],
                ["대기",fmt(data.pendingCount)],[kind==="rollback"?"복구 완료":"적용 완료",fmt(data.rolledBackCount??data.appliedCount)],["충돌",fmt(data.conflictCount)],["실패",fmt(data.failedCount)],["현재 삭제",fmt(data.deletedCount??data.deletedItemCount)]]);}
        async function loadReceipt(){const r=receipt,data=await request(`${B.base}/${r.batchId}/${r.kind}/actions/${r.actionId}`);
            B.requireValue(B.validActionReceipt(data,r.batchId,r.kind,r.actionId));receiptView(data,r.kind);}
        async function loadHistory(p=1,bound=null){const b=state.batch;B.requireValue(B.validBatch(b));
            const data=await request(`${B.base}/${b.batchId}/action-history?page=${p}&size=10${bound===null?"":`&throughVersion=${bound}`}`);
            B.requireValue(B.validHistory(data,b,p,10,bound),"승인 목록의 배치·조회 기준·페이지·당시 건수가 일치하지 않습니다. 최신 승인 목록을 다시 조회하세요.");
            historyPage=p;historyVersion=data.throughVersion;historyBatchId=b.batchId;navigation.write({historyPage:p,historyVersion});
            const box=clear("[data-history]");el(box,"p",`고정 조회 기준: 배치 버전 ${historyVersion} · 현재 배치 버전 ${data.currentBatchVersion}. 페이지 이동은 이 기준을 유지합니다. 새 승인을 포함하려면 최신 승인 목록을 다시 조회하세요.`);
            if(!data.history.items.length)el(box,"p","이 조회 기준에 적용·원복 승인 기록이 없습니다. 수집/적용 완료를 뜻하지 않습니다.");
            for(const e of data.history.items){const row=el(box,"article",null,"attachment-evidence-item"),kind=e.actionKind==="APPLICATION"?"application":"rollback";
                const name=kind==="rollback"?"원복 승인":({START:"판정 적용 시작 승인",PAUSE:"판정 적용 중지 승인",RESUME:"판정 적용 재개 승인"})[e.actionCode];
                el(row,"h3",name);meta(row,[["승인 식별자",e.actionId],["승인 기록 시각",date(e.acceptedAt)],["승인 전 버전",e.acceptedFromVersion],
                    ["당시 최초 범위",fmt(e.scopeItemCount)],["당시 승인 선택/대상",fmt(e.approvedTargetCount)],["당시 삭제",fmt(e.deletedCountAtAcceptance)]]);
                if(e.actionKind==="ROLLBACK")meta(row,[["당시 원복 적격",fmt(e.approvedEligibleCount)],["당시 기본 경로 재개",fmt(e.approvedBaseReopenCount)],
                    ["당시 이전 확인 복구",fmt(e.approvedConfirmationRestoreCount)],["당시 적용 대기 취소",fmt(e.cancelledPendingCount)]]);
                el(row,"p","승인 당시의 불변 범위입니다. 현재 성공·실패·삭제 수는 아래 버튼으로 별도 확인하세요.");
                button(row,"이 승인 영수증·현재 결과 확인",()=>read(async()=>{const d=await request(`${B.base}/${b.batchId}/${kind}/actions/${e.actionId}`);
                    B.requireValue(B.validHistoricalReceipt(d,e),"영수증의 승인 식별자·당시 범위가 목록과 다릅니다. 최신 이력을 다시 조회하세요.");
                    receipt={batchId:b.batchId,actionId:e.actionId,kind};navigation.write({actionId:e.actionId,actionKind:kind});receiptView(d,kind);q("#batch-receipt-title").focus();}));}
            pages("[data-history-pages]",data.history,n=>loadHistory(n,historyVersion));
        }
        scopeForm.addEventListener("submit",event=>{event.preventDefault();return read(async()=>{if(!scopeForm.reportValidity())return;const s=scopeInput();state.scope=null;clear("[data-scope-result]");const p=await request(`${B.base}/scope-preview`,{method:"POST",body:JSON.stringify(s)});B.requireValue(B.validScope(p,s));state.scope=p;scopeView(p);note("범위 조회 완료 · 외부 요청 0회 · 예약/수집은 별도 승인입니다.");});});
        scopeForm.addEventListener("input",()=>{if(locked())return;state.scope=null;clear("[data-scope-result]");disarm();gates();});
        approval.addEventListener("input",e=>{if(e.target!==approval.elements.namedItem("acknowledged"))approval.elements.namedItem("acknowledged").checked=false;});
        approval.addEventListener("submit",e=>{e.preventDefault();return submit();});
        q("[data-reserve]").addEventListener("click",()=>arm("reserve"));q("[data-save-selection]").addEventListener("click",()=>arm("selection"));
        q("[data-cancel-approval]").addEventListener("click",()=>{if(!locked()){disarm();gates();}});
        q("[data-refresh]").addEventListener("click",()=>state.batch && read(()=>loadBatch(state.batch.batchId)));
        q("[data-list-refresh]").addEventListener("click",()=>read(()=>loadList(listPage)));
        q("[data-policy-refresh]").addEventListener("click",()=>read(()=>loadPolicies()));
        q("[data-history-refresh]").addEventListener("click",()=>state.batch && read(()=>loadHistory(1,null)));
        q("[data-reset-selection]").addEventListener("click",()=>{if(locked() || !state.selectionComplete)return;state.selected=new Set(state.previewItems.filter(i=>i.selected).map(i=>i.jobId));state.selectionDirty=false;disarm();renderSelection();gates();note("미저장 선택을 버리고 조회한 서버 선택을 복원했습니다. 서버 데이터는 변경하지 않았습니다.");});
        q("[data-retry]").addEventListener("click",()=>submit(true));
        q("[data-cas-read]").addEventListener("click",async()=>{if(busy || !mutation.uncertain || mutation.sent.keyed)return;busy=true;casCurrent=null;q("[data-cas-ack]").checked=false;gates();
            try{const b=await request(`${B.base}/${mutation.sent.batchId}`);B.requireValue(B.validBatch(b) && b.batchId===mutation.sent.batchId);casCurrent=b;batchView(b);note("최신 상태 조회만 완료했습니다. 최초 수집 제어 요청의 성공 여부는 미확정입니다.");}catch(e){error(e);}finally{busy=false;gates();}});
        q("[data-cas-ack]").addEventListener("change",gates);
        q("[data-cas-continue]").addEventListener("click",()=>{if(busy)return;try{mutation.resolveCas(casCurrent,q("[data-cas-ack]").checked);disarm();return read(()=>loadBatch(casCurrent.batchId));}catch(e){error(e);}});
        return {state,mutation,arm,submit,loadBatch:id=>read(()=>loadBatch(id)),get dirty(){return !!approval.elements.namedItem("reason").value || state.selectionDirty || mutation.uncertain || mutation.pending;},
            async start(){const nav=navigation.read();listPage=nav.listPage||1;itemPage=nav.itemPage||1;previewPage=nav.previewPage||1;rollbackPage=nav.rollbackPage||1;
                if(B.uuid(nav.batchId) && B.count(nav.historyVersion) && nav.historyVersion<=2147483647){historyBatchId=nav.batchId;historyVersion=nav.historyVersion;historyPage=nav.historyPage||1;}
                await read(async()=>{await loadList(listPage);await loadPolicies();if(B.uuid(nav.batchId)){if(B.uuid(nav.actionId) && ["application","rollback"].includes(nav.actionKind))receipt={batchId:nav.batchId,actionId:nav.actionId,kind:nav.actionKind};await loadBatch(nav.batchId);}else note("배치를 선택하거나 새 범위를 조회하세요. 변경 작업은 자동 실행되지 않습니다.");});},gates};
    }
    if(typeof module!=="undefined" && module.exports)module.exports={mount};
    else{const page=root.document?.querySelector("[data-attachment-batches]");if(!page)return;const B=root.SanebAttachmentBatch,C=root.SanebAttachmentReview;
        const navigation={read:()=>{const p=new URL(root.location.href).searchParams,out={};for(const k of ["listPage","itemPage","previewPage","rollbackPage","historyPage"]){const n=Number(p.get(k));out[k]=Number.isInteger(n)&&n>=1&&n<=(k==="historyPage"?2147483647:1000000)?n:1;}for(const k of ["batchId","actionId","actionKind"])out[k]=p.get(k);
            if(p.has("historyVersion")){const v=Number(p.get("historyVersion"));if(B.count(v)&&v<=2147483647)out.historyVersion=v;}return out;},
            write:patch=>{const u=new URL(root.location.href);for(const[k,v]of Object.entries(patch)){if(v==null)u.searchParams.delete(k);else u.searchParams.set(k,String(v));}root.history.replaceState(null,"",u.pathname+u.search);}};
        const app=mount({page,B,C,request:B.client(root.fetch.bind(root),C),doc:root.document,uuid:()=>root.crypto.randomUUID(),navigation});
        root.addEventListener("beforeunload",e=>{if(app.dirty){e.preventDefault();e.returnValue="";}});app.start();}
})(globalThis);
