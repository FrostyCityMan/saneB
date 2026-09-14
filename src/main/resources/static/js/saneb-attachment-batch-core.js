/* 고정 배치 계약. 조회/승인/비동기 완료를 분리하며 URL·파서·성공값을 입력으로 만들지 않는다. */
((root) => {
    "use strict";
    const base = "/api/v2/admin/announcement-attachment-batches";
    const uuid = v => typeof v === "string" && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v);
    const hash = v => typeof v === "string" && /^[0-9a-f]{64}$/.test(v);
    const count = v => Number.isSafeInteger(v) && v >= 0;
    const time = v => typeof v === "string" && Number.isFinite(Date.parse(v));
    const requireValue = (ok, message = "응답의 대상·버전·건수가 일치하지 않습니다. 최신 배치를 다시 조회하세요.") => { if (!ok) throw new Error(message); };
    const statuses = {SCOPE_READY: "범위 예약 · 수집 전", COLLECTION_PENDING: "수집 대기", COLLECTING: "수집 중",
        COLLECTION_PAUSED: "수집 중지", COLLECTED: "수집 종료 · 적용 전", COLLECTION_PARTIAL_FAILED: "수집 종료 · 일부 실패/삭제",
        PREVIEW_RUNNING: "결과 미리보기 준비 중", PREVIEW_READY: "근거 전체 적격 · 적용 전", PREVIEW_PARTIAL_FAILED: "근거 일부 부적격 · 적용 전",
        APPLYING: "판정 적용 중", APPLY_PAUSED: "판정 적용 중지", APPLIED: "전체 판정 적용 · 검수는 별도",
        APPLY_PARTIAL_FAILED: "일부 적용 · 실패/미선택/삭제 확인", ROLLING_BACK: "원복 처리 중", ROLLED_BACK: "전체 이전 연결 복구",
        ROLLBACK_PARTIAL_FAILED: "일부 원복 · 미복구 항목 확인", CANCELLED: "예약 취소", READY: "근거 적격",
        ACTIVE: "게시 중", COLLECT_ONLY: "수집·미리보기만", ENFORCE: "첨부 판정 적용 정책", GOV24: "정부24", GOV24_PUBLIC_SERVICE: "정부24",
        TITLE_OR_BASE_NOT_ELIGIBLE: "제목·기본 판정 보호 제외", LINKED_PROTECTED: "운영 공고 연결 보호 제외", CANDIDATE: "범위 후보",
        COLLECTION_NOT_SUCCESSFUL: "수집 미완료/실패", EVIDENCE_INCOMPLETE: "근거 불완전", SOURCE_CHANGED: "원문 또는 기준 변경",
        ACTIVE_JOB: "다른 작업 진행 중", VERSION_LIMIT: "버전 증가 한도", NOT_REQUESTED: "요청 없음"};
    const label = (v, fallback) => statuses[v] || fallback(v);
    const validBatch = b => !!(b && uuid(b.batchId) && uuid(b.policyId) && hash(b.scopeHash) && count(b.rowVersion)
        && count(b.itemCount) && b.itemCount > 0 && b.itemCount <= 1000 && count(b.remainingItemCount) && count(b.deletedItemCount)
        && b.remainingItemCount + b.deletedItemCount === b.itemCount && typeof b.statusCode === "string"
        && b.jobCounts && Object.values(b.jobCounts).every(count) && Object.values(b.jobCounts).reduce((a,v)=>a+v,0) === b.remainingItemCount
        && b.frozenScope?.schemaVersion === 1 && time(b.createdAt));
    const validPage = (p, page, size, validator) => !!(p && p.page === page && p.size === size && count(p.totalCount)
        && p.totalPages === Math.ceil(p.totalCount / size) && Array.isArray(p.items)
        && p.items.length === Math.min(size, Math.max(0, p.totalCount - (page-1)*size)) && p.items.every(validator));
    const validPreview = (p, b) => !!(p && validBatch(b) && p.batchId === b.batchId && uuid(p.previewId)
        && p.scopeHash === b.scopeHash && hash(p.inputHash) && hash(p.previewHash) && p.currentBatchVersion === b.rowVersion
        && count(p.snapshotBatchVersion) && p.itemCount === b.itemCount && count(p.snapshotRemainingItemCount) && count(p.snapshotDeletedItemCount)
        && p.snapshotRemainingItemCount + p.snapshotDeletedItemCount === p.itemCount && p.availableItemCount === b.remainingItemCount
        && p.currentDeletedItemCount === b.deletedItemCount && count(p.eligibleItemCount) && p.eligibleItemCount <= p.snapshotRemainingItemCount
        && count(p.selectedItemCount) && p.selectedItemCount <= p.eligibleItemCount && typeof p.currentPreview === "boolean"
        && typeof p.inputsCurrent === "boolean" && p.currentHttpRequests === 0);
    const validItem = i => !!(i && uuid(i.jobId) && uuid(i.sourceId));
    const editable = (b,p) => validPreview(p,b) && p.currentPreview && p.inputsCurrent && p.snapshotBatchVersion === b.rowVersion
        && ["PREVIEW_READY","PREVIEW_PARTIAL_FAILED"].includes(b.statusCode);
    function scope(input) {
        const providers = [...new Set(input.providerCodes || [])].sort();
        requireValue(uuid(input.policyId), "게시 정책을 선택하세요.");
        requireValue(providers.length > 0 && providers.every(v=>["BIZINFO","GOV24","LOCAL_GOV_NOTICE"].includes(v)), "출처를 하나 이상 선택하세요.");
        const from = input.collectedFrom, before = input.collectedBefore;
        requireValue(time(from) && time(before) && Date.parse(from)<Date.parse(before), "수집 종료 시각은 시작 시각보다 뒤여야 합니다. 서울 시각으로 입력하세요.");
        const maximumCount = Number(input.maximumCount);
        requireValue(count(maximumCount) && maximumCount>=1 && maximumCount<=1000, "이번 배치 상한은 1~1000 사이의 정수입니다.");
        const d1 = input.deadlineFrom || null, d2 = input.deadlineThrough || null;
        requireValue([d1,d2].every(d=>d===null || /^\d{4}-\d{2}-\d{2}$/.test(d) && time(d)) && (!d1 || !d2 || d1<=d2), "마감일 범위의 시작과 끝을 확인하세요.");
        return {policyId:input.policyId,providerCodes:providers,collectedFrom:new Date(from).toISOString(),collectedBefore:new Date(before).toISOString(),deadlineFrom:d1,deadlineThrough:d2,maximumCount};
    }
    const sameScope = (a,b) => JSON.stringify(scope(a)) === JSON.stringify(scope(b));
    function validScope(p, s) {
        try { return !!(p && sameScope(p.scope,s) && hash(p.scopeHash) && hash(p.policyHash) && uuid(p.ruleReleaseId)
            && count(p.candidateCount) && count(p.selectedCount) && p.selectedCount === Math.min(s.maximumCount,p.candidateCount)
            && p.remainingCount === p.candidateCount-p.selectedCount && count(p.maximumDownloadBytes) && count(p.maximumHttpRequests)
            && p.currentHttpRequests===0 && typeof p.canReserve==="boolean" && Array.isArray(p.items) && p.items.length===p.selectedCount
            && p.items.every(i=>uuid(i.sourceId) && count(i.sourceVersion) && count(i.attachmentVersion) && typeof i.readinessCode==="string")
            && new Set(p.items.map(i=>i.sourceId)).size===p.items.length && Array.isArray(p.counts)
            && p.counts.every(c=>count(c.count) && s.providerCodes.includes(c.providerCode))
            && p.counts.filter(c=>c.reasonCode==="CANDIDATE").reduce((n,c)=>n+c.count,0)===p.candidateCount
            && (!p.canReserve || p.selectedCount>0 && p.items.every(i=>i.readinessCode==="READY") && p.maximumDownloadBytes>0 && p.maximumHttpRequests>0));
        } catch { return false; }
    }
    const validRollback = (r,b) => !!(r && validBatch(b) && r.batchId===b.batchId && r.version===b.rowVersion && r.statusCode===b.statusCode
        && hash(r.previewHash) && r.scopeCount===b.itemCount && r.remainingCount===b.remainingItemCount && r.deletedCount===b.deletedItemCount
        && [r.targetCount,r.eligibleCount,r.conflictCount,r.baseReopenCount,r.confirmationRestoreCount,r.staleConfirmationCount,r.cancelPendingCount].every(count)
        && r.targetCount<=r.remainingCount && r.eligibleCount+r.conflictCount===r.targetCount
        && r.baseReopenCount<=r.eligibleCount && r.confirmationRestoreCount<=r.eligibleCount && r.cancelPendingCount<=r.remainingCount-r.targetCount && r.currentHttpRequests===0);
    function command(kind, state, reason, acknowledged) {
        requireValue(acknowledged===true, "대상·건수·상한·검수/복구 영향을 확인하고 동의하세요.");
        requireValue(typeof reason==="string" && reason.trim().length>0 && reason.length<=1000, "작업 사유를 공백이 아닌 1~1000자로 입력하세요.");
        const b=state.batch, p=state.preview, r=state.rollback, s=state.scope;
        let payload, path, method="POST", keyed=true;
        if(kind==="reserve") {
            requireValue(!state.selectionDirty,"미저장 선택을 저장하거나 명시적으로 복원한 뒤 새 배치를 예약하세요.");
            requireValue(validScope(s,s?.scope) && s.canReserve, "예약 가능한 범위를 다시 조회하세요.");
            payload={scope:s.scope,expectedScopeHash:s.scopeHash,reason};path=base;
        } else {
            requireValue(validBatch(b) && b.rowVersion<2147483647);path=`${base}/${b.batchId}`;
            payload={expectedVersion:b.rowVersion,reason};
            if(["collection","collection-resume","collection-pause","scope-cancellation"].includes(kind)) {
                const allowed={collection:["SCOPE_READY"],"collection-resume":["COLLECTION_PAUSED"],"collection-pause":["COLLECTION_PENDING","COLLECTING"],"scope-cancellation":["SCOPE_READY"]};
                requireValue(allowed[kind].includes(b.statusCode), "현재 배치 단계에서는 이 수집 제어를 실행할 수 없습니다. 최신 상태를 조회하세요.");
                if(["collection","collection-resume"].includes(kind)) {
                    requireValue(count(b.frozenScope.maximumDownloadBytes) && b.frozenScope.maximumDownloadBytes>0 && count(b.frozenScope.maximumHttpRequests) && b.frozenScope.maximumHttpRequests>0);
                    requireValue(kind!=="collection" || b.deletedItemCount===0,"예약 뒤 삭제된 항목이 있습니다. 수집 전 예약을 취소하고 범위를 다시 고정하세요.");
                    Object.assign(payload,{expectedScopeHash:b.scopeHash,expectedItemCount:b.itemCount,expectedDeletedItemCount:b.deletedItemCount,
                        expectedMaximumDownloadBytes:b.frozenScope.maximumDownloadBytes,expectedMaximumHttpRequests:b.frozenScope.maximumHttpRequests});
                }
                path+=`/${kind}`;method="PUT";keyed=false;
            } else if(kind==="preview") {
                requireValue(["COLLECTED","COLLECTION_PARTIAL_FAILED","PREVIEW_READY","PREVIEW_PARTIAL_FAILED"].includes(b.statusCode),"수집이 종료된 배치에서 결과 미리보기를 생성하세요.");
                Object.assign(payload,{expectedScopeHash:b.scopeHash});path+="/classification-preview";
            } else if(kind==="selection") {
                requireValue(editable(b,p) && state.selectionComplete,"전체 미리보기 항목을 최신 기준으로 불러온 뒤 선택하세요.");
                const ids=[...state.selected].sort(); const items=state.previewItems;
                requireValue(items.length===p.availableItemCount && new Set(items.map(i=>i.jobId)).size===items.length
                    && ids.every(id=>items.some(i=>i.jobId===id && i.eligible===true && i.readinessCode==="READY")),"적격 항목만 선택할 수 있습니다. 다른 페이지의 선택을 포함해 다시 확인하세요.");
                Object.assign(payload,{expectedPreviewHash:p.previewHash,selectedJobIds:ids});path+="/classification-preview/selection";method="PUT";
            } else if(["apply","apply-pause","apply-resume"].includes(kind)) {
                requireValue(validPreview(p,b) && p.currentPreview && p.selectedItemCount>0,"현재 배치의 저장된 선택 미리보기가 필요합니다.");
                requireValue(kind==="apply"?editable(b,p) && !state.selectionDirty: b.statusCode===(kind==="apply-pause"?"APPLYING":"APPLY_PAUSED"),"저장된 최신 선택과 현재 적용 단계를 확인하세요.");
                Object.assign(payload,{expectedPreviewId:p.previewId,expectedPreviewHash:p.previewHash,expectedItemCount:b.itemCount,
                    expectedSelectedCount:p.selectedItemCount,expectedDeletedCount:b.deletedItemCount,acknowledgeReviewReset:true});
                path+="/application"+(kind==="apply"?"":kind==="apply-pause"?"/pause":"/resume");
            } else if(kind==="rollback") {
                requireValue(validRollback(r,b) && ["APPLIED","APPLY_PARTIAL_FAILED","APPLY_PAUSED"].includes(b.statusCode) && r.eligibleCount>0,"현재 적격 항목이 있는 원복 미리보기가 필요합니다.");
                Object.assign(payload,{expectedPreviewHash:r.previewHash,expectedScopeCount:r.scopeCount,expectedTargetCount:r.targetCount,
                    expectedDeletedCount:r.deletedCount,expectedBaseReopenCount:r.baseReopenCount,expectedConfirmationRestoreCount:r.confirmationRestoreCount,
                    expectedCancelPendingCount:r.cancelPendingCount,acknowledgeBindingRestoration:true});path+="/rollback";
            } else throw new Error("지원하지 않는 배치 작업입니다.");
        }
        return {kind,path,method,keyed,payload,batchId:b?.batchId || null,scopeHash:b?.scopeHash || null};
    }
    function validReceipt(data, sent) {
        const p=sent.payload,k=sent.kind;
        if(k==="reserve")return validBatch(data) && data.scopeHash===p.expectedScopeHash && data.policyId===p.scope.policyId && sameScope(data.frozenScope.filter,p.scope);
        if(["collection","collection-resume","collection-pause","scope-cancellation"].includes(k))return validBatch(data) && data.batchId===sent.batchId && data.scopeHash===sent.scopeHash && data.rowVersion===p.expectedVersion+1
            && data.statusCode===({collection:"COLLECTION_PENDING","collection-resume":"COLLECTING","collection-pause":"COLLECTION_PAUSED","scope-cancellation":"CANCELLED"})[k];
        if(["preview","selection"].includes(k))return data?.batchId===sent.batchId && uuid(data.previewId) && hash(data.previewHash)
            && data.snapshotBatchVersion===p.expectedVersion+2 && data.currentHttpRequests===0 && data.selectedItemCount===(k==="preview"?0:p.selectedJobIds.length);
        if(!validActionReceipt(data,sent.batchId,k==="rollback"?"rollback":"application") || data.acceptedFromVersion!==p.expectedVersion)return false;
        if(k==="rollback")return data.scopeCount===p.expectedScopeCount && data.approvedTargetCount===p.expectedTargetCount
            && data.approvedBaseReopenCount===p.expectedBaseReopenCount && data.approvedConfirmationRestoreCount===p.expectedConfirmationRestoreCount && data.cancelledPendingCount===p.expectedCancelPendingCount;
        return data.previewId===p.expectedPreviewId && data.scopeItemCount===p.expectedItemCount && data.approvedSelectedCount===p.expectedSelectedCount
            && data.actionCode===({apply:"START","apply-pause":"PAUSE","apply-resume":"RESUME"})[k];
    }
    function validActionReceipt(d,batchId,kind,actionId=null) {
        if(!d || d.batchId!==batchId || !uuid(d.actionId) || actionId!==null && d.actionId!==actionId || !count(d.acceptedFromVersion)
            || !count(d.currentVersion) || d.currentVersion<=d.acceptedFromVersion || !time(d.acceptedAt) || d.currentHttpRequests!==0)return false;
        if(kind==="rollback")return [d.scopeCount,d.approvedTargetCount,d.approvedEligibleCount,d.remainingTargetCount,d.deletedCount,d.pendingCount,d.rolledBackCount,d.conflictCount,d.failedCount].every(count)
            && d.approvedTargetCount<=d.scopeCount && d.approvedEligibleCount<=d.approvedTargetCount && d.remainingTargetCount<=d.approvedTargetCount
            && d.pendingCount+d.rolledBackCount+d.conflictCount+d.failedCount===d.remainingTargetCount && d.deletedCount<=d.scopeCount && typeof d.statusCode==="string";
        return kind==="application" && uuid(d.previewId) && ["START","PAUSE","RESUME"].includes(d.actionCode)
            && [d.scopeItemCount,d.approvedSelectedCount,d.remainingItemCount,d.deletedItemCount,d.selectedRemainingCount,d.pendingCount,d.appliedCount,d.conflictCount,d.failedCount].every(count)
            && d.remainingItemCount+d.deletedItemCount===d.scopeItemCount && d.approvedSelectedCount<=d.scopeItemCount && d.selectedRemainingCount<=d.approvedSelectedCount
            && d.selectedRemainingCount<=d.remainingItemCount && d.pendingCount+d.appliedCount+d.conflictCount+d.failedCount<=d.selectedRemainingCount && typeof d.currentStatusCode==="string";
    }
    function validHistoryEntry(e,b,bound) {
        if(!validBatch(b) || !e || e.batchId!==b.batchId || !uuid(e.actionId) || !count(e.acceptedFromVersion) || e.acceptedFromVersion>=bound
            || e.scopeItemCount!==b.itemCount || !count(e.approvedTargetCount) || e.approvedTargetCount<1 || e.approvedTargetCount>e.scopeItemCount
            || !count(e.deletedCountAtAcceptance) || e.deletedCountAtAcceptance>e.scopeItemCount || !time(e.acceptedAt))return false;
        if(e.actionKind==="APPLICATION")return ["START","PAUSE","RESUME"].includes(e.actionCode) && uuid(e.previewId)
            && [e.approvedEligibleCount,e.approvedBaseReopenCount,e.approvedConfirmationRestoreCount,e.cancelledPendingCount].every(v=>v===null);
        return e.actionKind==="ROLLBACK" && e.actionCode==="START" && e.previewId===null
            && [e.approvedEligibleCount,e.approvedBaseReopenCount,e.approvedConfirmationRestoreCount,e.cancelledPendingCount].every(count)
            && e.approvedEligibleCount>=1 && e.approvedEligibleCount<=e.approvedTargetCount
            && e.approvedBaseReopenCount+e.approvedConfirmationRestoreCount<=e.approvedEligibleCount
            && e.approvedTargetCount+e.deletedCountAtAcceptance+e.cancelledPendingCount<=e.scopeItemCount;
    }
    function validHistory(h,b,page,size,bound=null) {
        return !!(validBatch(b) && h?.batchId===b.batchId && count(h.throughVersion) && h.throughVersion<=2147483647
            && (bound===null || h.throughVersion===bound) && count(h.currentBatchVersion) && h.currentBatchVersion>=h.throughVersion
            && h.currentBatchVersion>=b.rowVersion && h.currentHttpRequests===0
            && validPage(h.history,page,size,e=>validHistoryEntry(e,b,h.throughVersion))
            && new Set(h.history.items.map(e=>`${e.actionKind}:${e.actionId}`)).size===h.history.items.length
            && h.history.items.every((e,i,items)=>i===0 || e.acceptedFromVersion<=items[i-1].acceptedFromVersion));
    }
    function validHistoricalReceipt(d,e) {
        if(!e || !validActionReceipt(d,e.batchId,e.actionKind==="APPLICATION"?"application":"rollback",e.actionId)
            || d.acceptedFromVersion!==e.acceptedFromVersion || Date.parse(d.acceptedAt)!==Date.parse(e.acceptedAt))return false;
        if(e.actionKind==="APPLICATION")return d.previewId===e.previewId && d.actionCode===e.actionCode && d.scopeItemCount===e.scopeItemCount && d.approvedSelectedCount===e.approvedTargetCount;
        return e.actionKind==="ROLLBACK" && d.scopeCount===e.scopeItemCount && d.approvedTargetCount===e.approvedTargetCount
            && d.approvedEligibleCount===e.approvedEligibleCount && d.approvedBaseReopenCount===e.approvedBaseReopenCount
            && d.approvedConfirmationRestoreCount===e.approvedConfirmationRestoreCount && d.cancelledPendingCount===e.cancelledPendingCount;
    }
    function client(fetcher,C,timeout=20000) {
        return async (url,options={}) => {
            requireValue(/^\/api\/v2\/admin\/announcement-attachment-(?:batches|policies|backfills)(?:\/[a-zA-Z0-9-]+)*(?:\?[a-zA-Z0-9=&-]+)?$/.test(url),"허용되지 않은 배치 요청 경로입니다.");
            const abort=new AbortController(), timer=setTimeout(()=>abort.abort(),timeout);
            try {
                const response=await fetcher(url,{...options,credentials:"same-origin",cache:"no-store",redirect:"error",signal:abort.signal,
                    headers:{Accept:"application/json",...(options.body?{"Content-Type":"application/json"}:{}),...options.headers}});
                let body;try{body=await response.json();}catch{throw new C.RequestError("서버 응답을 읽지 못했습니다. 변경 결과는 미확정입니다.",response.ok?0:response.status);}
                if(!response.ok || body?.success!==true)throw new C.RequestError(response.status===401?"로그인이 만료됐습니다. 이 탭을 유지하고 다른 탭에서 로그인하세요.":response.status===403?"관리 권한 또는 보안 확인이 유효하지 않습니다. 계정과 로그인 상태를 확인하세요.":response.status>=500?"서버 처리 결과가 미확정입니다. 원래 요청을 재확인하세요.":body?.message || "현재 기준과 충돌했습니다. 최신 상태를 확인하세요.",response.ok?0:response.status);
                if(!body.data || typeof body.data!=="object")throw new C.RequestError("서버 응답 계약이 일치하지 않아 처리 결과가 미확정입니다.");
                return body.data;
            } catch(e){if(e instanceof C.RequestError)throw e;throw new C.RequestError("응답이 유실되거나 연결이 끊겼습니다. 원래 요청을 먼저 재확인하세요.");}
            finally{clearTimeout(timer);}
        };
    }
    // 결과 유실 뒤 401/403/409도 최초 요청 실패의 증거가 아니다. 동일 요청을 유지한다.
    function mutations(request,C,randomUUID,receiptValidator=validReceipt) {
        let sent=null,pending=false,uncertain=false;
        return {
            async execute(next) {
                if(pending)throw new Error("이미 요청 중입니다.");
                if(uncertain && next)throw new Error("이전 요청 결과를 먼저 확인하세요.");
                if(!sent){requireValue(next);sent={...next,body:JSON.stringify(next.payload),key:next.keyed?randomUUID():null};}
                const wasUncertain=uncertain;pending=true;
                try{const data=await request(sent.path,{method:sent.method,body:sent.body,headers:sent.key?{"Idempotency-Key":sent.key}:{}});
                    if(!receiptValidator(data,sent))throw new C.RequestError("응답의 대상·승인 내용이 일치하지 않습니다. 원래 요청으로 재확인하세요.");
                    const receipt={data,sent};sent=null;uncertain=false;return receipt;
                }catch(e){uncertain=wasUncertain || e.uncertain===true;if(!uncertain)sent=null;throw e;}finally{pending=false;}
            },
            resolveCas(current,acknowledged){requireValue(uncertain && sent && !sent.keyed && acknowledged===true && validBatch(current) && current.batchId===sent.batchId && current.rowVersion>=sent.payload.expectedVersion,"최신 상태를 조회하고 별도 확인해야 수집 제어의 새 기준을 사용할 수 있습니다.");sent=null;uncertain=false;},
            get pending(){return pending;},get uncertain(){return uncertain;},get sent(){return sent;}
        };
    }
    async function loadPreviewItems(request,b,p) {
        requireValue(validPreview(p,b));let items=[];let pages=1;
        for(let page=1;page<=pages;page++) {
            const result=await request(`${base}/${b.batchId}/classification-preview/${p.previewId}/items?page=${page}&size=100`);
            requireValue(validPage(result,page,100,i=>validItem(i) && typeof i.eligible==="boolean" && typeof i.selected==="boolean") && result.totalCount===p.availableItemCount,"미리보기 항목 수가 바뀌었습니다. 선택을 보내지 말고 최신 기준을 다시 조회하세요.");
            pages=result.totalPages;requireValue(pages<=10);items.push(...result.items);
        }
        requireValue(new Set(items.map(i=>i.jobId)).size===items.length && items.length===p.availableItemCount);
        const after=await request(`${base}/${b.batchId}/classification-preview`);
        requireValue(validPreview(after,b) && after.previewId===p.previewId && after.previewHash===p.previewHash && after.inputsCurrent===p.inputsCurrent,"미리보기가 조회 중 바뀌었습니다. 새 미리보기 기준을 확인하세요.");
        if(p.inputsCurrent)requireValue(items.filter(i=>i.selected).length===p.selectedItemCount && items.every(i=>!i.selected || i.eligible && i.readinessCode==="READY"));
        return items;
    }
    const api={base,uuid,hash,count,requireValue,label,validBatch,validPage,validPreview,validItem,editable,scope,validScope,validRollback,command,validReceipt,validActionReceipt,validHistoryEntry,validHistory,validHistoricalReceipt,client,mutations,loadPreviewItems};
    if(typeof module!=="undefined" && module.exports)module.exports=api;else root.SanebAttachmentBatch=api;
})(globalThis);
