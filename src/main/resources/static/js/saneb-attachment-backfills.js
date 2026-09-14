/* 전체 목록 고정·분할 탐색. 실제 수집/적용/원복은 연결된 기존 배치 화면에서 별도 승인한다. */
((root) => {
    "use strict";
    function mount({page,B,C,F,request,doc,uuid,navigation}) {
        const q=s=>page.querySelector(s),admin=page.dataset.isAdmin==="true",label=v=>F.label(v,C.label);
        const state={preview:null,run:null,segmentNo:null,reservation:null};
        const mutation=B.mutations(request,C,uuid,F.validReceipt), form=q("[data-scope-form]"),approval=q("[data-approval-form]");
        const field=n=>form.elements.namedItem(n),ack=approval.elements.namedItem("acknowledged"),reason=approval.elements.namedItem("reason");
        const policies=new Map();let busy=false,action=null,stale=true,formDirty=false;
        const el=(parent,tag,text,css)=>{const n=doc.createElement(tag);if(text!=null)n.textContent=String(text);if(css)n.className=css;parent.append(n);return n;};
        const clear=s=>{const n=q(s);n.replaceChildren();return n;};
        const fmt=n=>B.count(n)?`${n.toLocaleString("ko-KR")}건`:"미확인";
        const date=v=>new Date(v).toLocaleString("ko-KR",{timeZone:"Asia/Seoul",hour12:false})+" (서울)";
        const meta=(box,pairs)=>{const dl=el(box,"dl",null,"attachment-meta");for(const[k,v]of pairs){el(dl,"dt",k);el(dl,"dd",v);}};
        const note=s=>{q("[data-status]").textContent=s;};
        const error=e=>{q("[data-error]").textContent=e?.message||"응답을 확인하지 못했습니다. 최신 상태를 다시 조회하세요.";q("[data-error]").hidden=false;q("[data-error]").focus();};
        const locked=()=>busy || mutation.pending || mutation.uncertain;
        function link(box,text,href){const a=el(box,"a",text,"secondary-action");a.href=href;a.addEventListener("click",e=>{if(locked()){e.preventDefault();error(new Error("요청 중이거나 결과가 미확정입니다. 원래 요청을 먼저 재확인하세요."));}});return a;}
        function url(patch){const params={...navigation.read(),...patch},query=new URLSearchParams();
            for(const[k,v]of Object.entries(params))if(v!=null)query.set(k,String(v));return `/app/admin/announcement-attachment-backfills?${query}`;}
        function gates(){
            for(const s of ["[data-list-refresh]","[data-policy-refresh]","[data-scope-submit]"])q(s).disabled=locked();
            q("[data-scope-fields]").disabled=locked();q("[data-refresh]").disabled=locked() || !state.run;
            q("[data-inventory]").disabled=locked() || !admin || !state.preview?.canInventory;
            q("[data-reserve]").disabled=locked() || !admin || stale || !state.reservation?.canReserve;
            q("[data-approval-fields]").disabled=locked() || !admin || !action;
            q("[data-approve]").disabled=locked() || !admin || !action;
            q("[data-cancel-approval]").disabled=locked();q("[data-uncertain]").hidden=!mutation.uncertain;
            q("[data-retry]").disabled=busy || mutation.pending;
            page.setAttribute("aria-busy",String(busy));
        }
        function disarm(){action=null;ack.checked=false;q("[data-approval]").hidden=true;}
        async function read(work){if(locked())return;busy=true;disarm();q("[data-error]").hidden=true;note("전체 목록·분할의 최신 상태를 조회합니다.");gates();
            try{await work();note("조회한 시점의 상태입니다. 목록 고정·예약은 처리 완료가 아니며 변경은 자동 실행되지 않습니다.");}
            catch(e){stale=true;error(e);note("조회 실패 · 입력과 확인된 이력은 유지합니다. 최신 상태를 다시 조회하세요.");}finally{busy=false;gates();}}
        function pages(selector,data,key){const box=clear(selector);el(box,"p",`현재 목록 ${fmt(data.totalCount)} · ${data.totalPages?`${data.page}/${data.totalPages}페이지`:"0페이지"}`);
            if(data.page>1)link(box,"이전 페이지",url({[key]:data.page-1}));if(data.page<data.totalPages)link(box,"다음 페이지",url({[key]:data.page+1}));}
        async function loadList(p=1){const data=await request(`${F.base}?page=${p}&size=10`);B.requireValue(B.validPage(data,p,10,F.validRun));
            const box=clear("[data-list]");if(!data.items.length)el(box,"p","고정한 전체 목록이 없습니다. 범위를 조회한 뒤 관리자가 전체 목록을 고정할 수 있습니다.");
            for(const r of data.items){const row=el(box,"article",null,"attachment-evidence-item");el(row,"p",`${r.runId} · ${label(r.statusCode)} · 최초 ${fmt(r.candidateCount)} / 현재 남음 ${fmt(r.remainingItemCount)} / 삭제 ${fmt(r.deletedItemCount)} · ${r.segmentCount}분할 · ${date(r.createdAt)}`);
                link(row,"이 전체 목록과 분할 확인",url({runId:r.runId,segmentNo:null,segmentPage:1,itemPage:1}));}
            pages("[data-list-pages]",data,"listPage");}
        async function loadPolicies(p=1){const data=await request(`/api/v2/admin/announcement-attachment-policies?status=ACTIVE&page=${p}&size=20`);
            B.requireValue(B.validPage(data,p,20,i=>B.uuid(i.policyId) && i.policyStatusCode==="ACTIVE"));
            const box=clear("[data-policies]");if(!data.items.length)el(box,"p","게시 정책이 없습니다. 정책 QA·게시 절차가 먼저 필요하며 이 화면에서 자동 게시하지 않습니다.");
            for(const r of data.items){el(box,"p",`${r.policyCode} v${r.versionNo} · ${label(r.modeCode)} · 규칙 ${label(r.ruleReleaseStatusCode)}`);
                if(r.ruleReleaseStatusCode==="ACTIVE" && ["COLLECT_ONLY","ENFORCE"].includes(r.modeCode))policies.set(r.policyId,r);}
            const select=field("policyId"),old=select.value;select.replaceChildren();el(select,"option","게시 정책 선택").value="";
            for(const r of policies.values())el(select,"option",`${r.policyCode} v${r.versionNo} · ${label(r.modeCode)} · ${r.policyId}`).value=r.policyId;
            select.value=old;pages("[data-policy-pages]",data,"policyPage");}
        function scopeInput(){return F.scope({policyId:field("policyId").value,providerCodes:[...form.querySelectorAll('input[name="provider"]:checked')].map(i=>i.value),
            collectedFrom:field("collectedFrom").value+":00+09:00",collectedBefore:field("collectedBefore").value+":00+09:00",
            deadlineFrom:field("deadlineFrom").value,deadlineThrough:field("deadlineThrough").value,segmentSize:field("segmentSize").value});}
        function filterView(box,s){el(box,"p",`범위: ${s.providerCodes.map(label).join(", ")} · ${date(s.collectedFrom)} 이상 ~ ${date(s.collectedBefore)} 미만 · 마감일 ${s.deadlineFrom||"제한 없음"} ~ ${s.deadlineThrough||"제한 없음"} · 분할당 최대 ${fmt(s.segmentSize)}`);}
        function countsView(box,counts){for(const c of counts)el(box,"p",`${label(c.providerCode)} · ${label(c.reasonCode)}: ${fmt(c.count)}`);}
        function scopeView(p){const box=clear("[data-scope-result]");filterView(box,p.scope);
            meta(box,[["최초 고정할 전체 후보",fmt(p.candidateCount)],["예상 분할 수",`${p.segmentCount}개`],["범위 지문",p.scopeHash],["후보 지문",p.candidateHash],["현재 외부 요청","0회"]]);countsView(box,p.counts);
            if(!p.canInventory)el(box,"p","고정할 후보가 없습니다. 보호 제외와 입력 범위를 확인하세요. 빈 목록은 저장하지 않습니다.");}
        function runView(r){const box=clear("[data-detail]");meta(box,[["전체 목록 ID",r.runId],["상태",label(r.statusCode)],["최초 전체 후보",fmt(r.candidateCount)],
            ["현재 남은 항목",fmt(r.remainingItemCount)],["삭제된 항목",fmt(r.deletedItemCount)],["분할 수",`${r.segmentCount}개`],["버전",r.rowVersion],["게시 정책 ID",r.policyId],["고정 시각",date(r.createdAt)],["범위 지문",r.scopeHash]]);
            filterView(box,r.frozenScope.filter);el(box,"p","아래 보호 제외는 최초 고정 시점입니다. 삭제된 대상을 다른 공고로 채우거나 이후 도착한 공고를 자동 편입하지 않습니다.");countsView(box,r.frozenScope.counts);}
        async function loadSummary(r){const s=await request(`${F.base}/${r.runId}/summary`);B.requireValue(F.validSummary(s,r),"전체 집계의 최초·잔여·삭제 또는 단계별 분모가 맞지 않습니다. 최신 전체 목록을 다시 조회하세요.");
            const box=clear("[data-summary]");el(box,"p",`집계 조회: ${date(new Date().toISOString())}. 각 단계의 합은 남은 ${fmt(s.remainingItemCount)}이며 단계끼리 더하지 않습니다.`);
            meta(box,[["최초 전체 후보",fmt(s.candidateCount)],["현재 남은 대상",fmt(s.remainingItemCount)],["현재 삭제",fmt(s.deletedItemCount)],
                ["예약된 분할 / 전체 분할",`${s.reservedSegmentCount} / ${s.segmentCount}`],["예약된 잔여 항목",fmt(s.reservedRemainingItemCount)],
                ["미예약 항목",fmt(s.unreservedItemCount)],["미예약 중 입력 변경",fmt(s.unreservedInputChangedCount)]]);
            for(const [title,key]of [["수집 상태","collectionCounts"],["판정 적용 상태","applicationCounts"],["원복 상태","rollbackCounts"]]){
                const row=el(box,"section");el(row,"h3",title);for(const [code,n]of Object.entries(s[key]))el(row,"p",`${label(code)}: ${fmt(n)}`);
                if(!Object.keys(s[key]).length)el(row,"p","남아 있는 항목 없음. 삭제는 성공이 아닙니다.");}}
        async function loadSegments(r,p=1){const data=await request(`${F.base}/${r.runId}/segments?page=${p}&size=20`);
            B.requireValue(B.validPage(data,p,20,s=>F.validSegment(s,r)) && data.totalCount===r.segmentCount
                && new Set(data.items.map(s=>s.segmentNo)).size===data.items.length);
            const box=clear("[data-segments]");for(const s of data.items){const row=el(box,"article",null,"attachment-evidence-item");
                el(row,"p",`${s.segmentNo}번 분할 · 최초 ${fmt(s.itemCount)} / 현재 남음 ${fmt(s.remainingItemCount)} / 삭제 ${fmt(s.deletedItemCount)}`);
                link(row,`${s.segmentNo}번 분할 준비·배치 확인`,url({runId:r.runId,segmentNo:s.segmentNo,itemPage:1}));}
            pages("[data-segment-pages]",data,"segmentPage");}
        async function loadItems(r,n,p=1){const data=await request(`${F.base}/${r.runId}/segments/${n}/items?page=${p}&size=20`);
            B.requireValue(B.validPage(data,p,20,i=>F.validItem(i,r,n)) && data.totalCount<=r.segmentSize
                && new Set(data.items.map(i=>i.sourceId)).size===data.items.length && new Set(data.items.map(i=>i.ordinal)).size===data.items.length);
            const box=clear("[data-items]");el(box,"p","현재 남아 있는 고정 소속만 표시합니다. 페이지의 일부를 전체 대상으로 해석하지 마세요. 입력 변경은 자동 제외가 아닙니다.");
            if(!data.items.length)el(box,"p","현재 남은 항목이 없습니다. 최초 대상과 삭제 건수는 보존됩니다.");
            for(const i of data.items){const row=el(box,"article",null,"attachment-evidence-item");el(row,"p",`최초 순번 ${i.ordinal} · ${label(i.providerCode)} · ${i.currentInputMatches?"고정 입력 일치":"고정 후 입력 변경"}`);
                link(row,`원문 ${i.sourceId} 검수 이력`, `/app/admin/collected-announcements/${i.sourceId}/attachments`);}
            pages("[data-item-pages]",data,"itemPage");}
        function reservationView(p){const box=clear("[data-reservation]");meta(box,[["선택 분할",`${p.segmentNo}번`],["예약 준비",label(p.readinessCode)],
            ["최초 대상",fmt(p.originalItemCount)],["현재 예약 대상",fmt(p.remainingItemCount)],["예약 전 삭제",fmt(p.deletedItemCount)],["확인한 버전",p.runVersion],["분할 지문",p.segmentHash]]);
            if(p.batchId){link(box,"연결된 배치에서 수집·적용·원복 확인",F.batchLink(p.batchId));el(box,"p","기존 연결을 유지합니다. 실패·취소한 배치를 이 분할에서 자동 교체하지 않습니다. 재처리는 남은 대상의 새 범위와 별도 승인이 필요합니다.");}
            if(p.batchPreview){const b=p.batchPreview;meta(box,[["예약 후 수집의 최대 HTTP",`${b.maximumHttpRequests}회`],["예약 후 수집의 최대 다운로드",`${b.maximumDownloadBytes}바이트`],["현재 외부 요청","0회"]]);
                const details=el(box,"details");el(details,"summary",`고정 대상 ${fmt(b.items.length)}의 예약 준비 사유`);
                for(const i of b.items)el(details,"p",`${i.sourceId} · ${label(i.providerCode)} · ${label(i.readinessCode)}`);}
            el(box,"p","예약은 수집 시작이 아닙니다. 예약 뒤 삭제나 입력 변경이 있으면 기존 배치의 수집 시작이 차단됩니다. 수집 전 예약 취소는 배치 화면에서 수행합니다.");}
        async function loadRun(id,n=null){B.requireValue(B.uuid(id));stale=true;state.reservation=null;state.run=null;state.segmentNo=null;
            for(const s of ["[data-detail]","[data-summary]","[data-segments]","[data-segment-pages]","[data-reservation]","[data-items]","[data-item-pages]"])clear(s);
            const r=await request(`${F.base}/${id}`);B.requireValue(F.validRun(r) && r.runId===id);state.run=r;runView(r);
            const nav=navigation.read();await loadSummary(r);await loadSegments(r,nav.segmentPage||1);
            if(n!=null){B.requireValue(B.count(n) && n>=1 && n<=r.segmentCount,"분할 번호가 전체 범위에 없습니다. 분할 목록에서 다시 선택하세요.");
                const p=await request(`${F.base}/${id}/segments/${n}/reservation-preview`);B.requireValue(F.validReservationPreview(p,r,n));
                state.segmentNo=n;state.reservation=p;reservationView(p);await loadItems(r,n,nav.itemPage||1);}
            else el(q("[data-reservation]"),"p","분할 목록에서 준비 상태를 확인할 분할을 선택하세요.");stale=false;}
        function arm(kind){if(locked() || !admin || kind==="reserve" && stale)return;try{F.command(kind,state,"영향 확인",true);action=kind;ack.checked=false;
            q("[data-approval]").hidden=false;const box=clear("[data-impact]");
            if(kind==="inventory")scopeViewInto(box,state.preview);else reservationImpact(box,state.reservation);
            q("[data-approve]").textContent=kind==="inventory"?"확인한 전체 목록 고정":"확인한 분할만 예약";q("#backfill-approval-title").focus();gates();
            }catch(e){disarm();gates();error(e);}}
        function scopeViewInto(box,p){filterView(box,p.scope);meta(box,[["전체 후보",fmt(p.candidateCount)],["분할 수",`${p.segmentCount}개`],["범위 지문",p.scopeHash]]);
            el(box,"p","전체 목록과 소속만 고정합니다. 수집 작업·파일 다운로드·판정 적용·운영 공고 활성화는 하지 않습니다. 목록 이력은 유지되며 수집은 분할 예약 후 별도 승인합니다.");}
        function reservationImpact(box,p){meta(box,[["전체 목록",p.runId],["분할",`${p.segmentNo}번`],["최초 / 남음 / 삭제",`${fmt(p.originalItemCount)} / ${fmt(p.remainingItemCount)} / ${fmt(p.deletedItemCount)}`],
            ["예약 후 수집 HTTP 상한",`${p.batchPreview.maximumHttpRequests}회`],["예약 후 다운로드 상한",`${p.batchPreview.maximumDownloadBytes}바이트`],["분할 지문",p.segmentHash]]);
            el(box,"p","현재 고정 소속만 예약합니다. 외부 요청 0회이며 수집·적용 승인이 아닙니다. 활성 작업을 예약하므로 다른 작업과 충돌할 수 있습니다. 수집 전 배치 취소가 가능하며 기존 연결과 취소 이력은 보존됩니다.");}
        function receiptView(d,kind){const box=clear("[data-receipt]");
            if(kind==="inventory"){el(box,"p",`전체 목록 ${d.runId} 고정 확인 · 최초 ${fmt(d.candidateCount)} · 수집/적용 완료 아님`);link(box,"고정한 전체 목록과 분할 열기",url({runId:d.runId,segmentNo:null,segmentPage:1,itemPage:1}));}
            else{meta(box,[["전체 목록",d.runId],["분할",`${d.segmentNo}번`],["예약 배치",d.batchId],["최초 / 예약 / 예약 전 삭제",`${fmt(d.originalItemCount)} / ${fmt(d.reservedItemCount)} / ${fmt(d.deletedBeforeReservation)}`],["조회 시 배치 상태",label(d.currentBatchStatusCode)],["예약 기록 시각",date(d.reservedAt)]]);
                link(box,"예약한 배치에서 수집 시작·중지·복구 검토",F.batchLink(d.batchId));el(box,"p","예약 영수증입니다. 파일 수집·판정 적용 완료를 의미하지 않습니다.");}}
        async function submit(retry=false){if(busy || mutation.pending || !admin || !retry && mutation.uncertain)return;let next;
            try{if(!retry){B.requireValue(action,"영향을 먼저 확인하세요.");if(!approval.reportValidity())return;next=F.command(action,state,reason.value,ack.checked);}}
            catch(e){error(e);return;}busy=true;gates();q("[data-error]").hidden=true;
            try{const r=await mutation.execute(next);disarm();receiptView(r.data,r.sent.kind);stale=true;state.reservation=null;state.preview=null;clear("[data-scope-result]");
                reason.value="";if(r.sent.kind==="inventory")formDirty=false;note("서버 영수증을 확인했습니다. 수집/적용은 자동 시작되지 않습니다.");
                // 조회 실패가 성공한 예약 영수증이나 이동 경로를 지우지 않는다.
                await loadList(navigation.read().listPage||1);
                if(r.sent.kind==="reserve")await loadRun(r.data.runId,r.data.segmentNo);
            }catch(e){stale=true;error(e);note(mutation.uncertain?"결과 미확정 · 원래 요청 그대로 재확인하세요.":"요청 또는 후속 조회 실패 · 확인된 영수증과 입력을 유지합니다.");}
            finally{busy=false;gates();}}
        form.addEventListener("submit",e=>{e.preventDefault();return read(async()=>{if(!form.reportValidity())return;state.preview=null;clear("[data-scope-result]");
            const s=scopeInput(),p=await request(`${F.base}/scope-preview`,{method:"POST",body:JSON.stringify(s)});B.requireValue(F.validPreview(p,s));state.preview=p;scopeView(p);});});
        form.addEventListener("input",()=>{if(locked())return;formDirty=true;state.preview=null;clear("[data-scope-result]");disarm();gates();});
        approval.addEventListener("input",e=>{if(e.target!==ack)ack.checked=false;});
        approval.addEventListener("submit",e=>{e.preventDefault();return submit();});
        q("[data-inventory]").addEventListener("click",()=>arm("inventory"));q("[data-reserve]").addEventListener("click",()=>arm("reserve"));
        q("[data-cancel-approval]").addEventListener("click",()=>{if(!locked()){disarm();gates();}});
        q("[data-retry]").addEventListener("click",()=>submit(true));
        q("[data-list-refresh]").addEventListener("click",()=>read(()=>loadList(navigation.read().listPage||1)));
        q("[data-policy-refresh]").addEventListener("click",()=>read(()=>loadPolicies(navigation.read().policyPage||1)));
        q("[data-refresh]").addEventListener("click",()=>state.run && read(()=>loadRun(state.run.runId,state.segmentNo)));
        return {state,mutation,arm,submit,loadRun:(id,n)=>read(()=>loadRun(id,n)),get dirty(){return formDirty || !!reason.value || mutation.pending || mutation.uncertain;},
            async start(){const nav=navigation.read();await read(async()=>{const errors=[];
                const work=[()=>loadList(nav.listPage||1),()=>loadPolicies(nav.policyPage||1)];
                if(nav.runId)work.push(()=>loadRun(nav.runId,nav.segmentNo));
                for(const load of work){try{await load();}catch(e){errors.push(e.message);}}
                if(errors.length)throw new Error(`일부 조회 실패: ${errors.join(" / ")} 확인된 결과만 표시하며 실패 영역은 다시 조회하세요.`);
            });},gates};
    }
    if(typeof module!=="undefined" && module.exports)module.exports={mount};
    else{const page=root.document?.querySelector("[data-attachment-backfills]");if(!page)return;const B=root.SanebAttachmentBatch,C=root.SanebAttachmentReview,F=root.SanebAttachmentBackfill;
        const navigation={read:()=>{const p=new URL(root.location.href).searchParams,out={};for(const k of ["listPage","policyPage","segmentPage","itemPage","segmentNo"]){const n=Number(p.get(k));if(Number.isSafeInteger(n)&&n>=1)out[k]=n;}
            if(p.has("runId"))out.runId=p.get("runId");return out;}};
        const app=mount({page,B,C,F,request:B.client(root.fetch.bind(root),C),doc:root.document,uuid:()=>root.crypto.randomUUID(),navigation});
        root.addEventListener("beforeunload",e=>{if(app.dirty){e.preventDefault();e.returnValue="";}});app.start();}
})(globalThis);
