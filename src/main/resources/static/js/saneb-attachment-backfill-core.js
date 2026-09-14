/* 전체 분모와 고정 분할 계약. 페이지 일부·예약·삭제를 처리 성공으로 계산하지 않는다. */
((root) => {
    "use strict";
    function create(B) {
        const base="/api/v2/admin/announcement-attachment-backfills", check=B.requireValue;
        const time=v=>typeof v==="string" && Number.isFinite(Date.parse(v));
        const labels={INVENTORIED:"전체 목록 고정 · 처리 완료 아님",UNRESERVED:"미예약",ALREADY_RESERVED:"이미 예약됨 · 기존 배치 확인",
            ALL_ITEMS_DELETED:"남은 대상 없음 · 삭제 이력 유지",INPUT_CHANGED:"고정 후 입력 변경 · 새 범위 검토 필요",
            POLICY_CHANGED:"고정 정책 변경 · 정책과 새 범위 검토 필요",BATCH_NOT_READY:"예약 준비 미충족 · 항목별 사유 확인",READY:"분할 예약 가능"};
        const label=(v,fallback)=>labels[v] || B.label(v,fallback);
        function scope(input) {
            const s=B.scope({...input,maximumCount:input.segmentSize});const {maximumCount,...filter}=s;
            return {...filter,segmentSize:maximumCount};
        }
        const sameScope=(a,b)=>JSON.stringify(scope(a))===JSON.stringify(scope(b));
        const safe=fn=>{try{return !!fn();}catch{return false;}};
        function validCounts(rows,s,candidateCount) {
            return Array.isArray(rows) && rows.every(r=>s.providerCodes.includes(r.providerCode) && B.count(r.count)
                && ["CANDIDATE","TITLE_OR_BASE_NOT_ELIGIBLE","LINKED_PROTECTED"].includes(r.reasonCode))
                && new Set(rows.map(r=>`${r.providerCode}:${r.reasonCode}`)).size===rows.length
                && rows.filter(r=>r.reasonCode==="CANDIDATE").reduce((a,r)=>a+r.count,0)===candidateCount;
        }
        const validPreview=(p,s)=>safe(()=>p && sameScope(p.scope,s) && B.hash(p.scopeHash) && B.hash(p.candidateHash)
            && B.uuid(p.ruleReleaseId) && B.hash(p.policyHash) && B.count(p.candidateCount)
            && p.segmentCount===Math.ceil(p.candidateCount/s.segmentSize) && p.previewHttpRequests===0
            && p.canInventory===(p.candidateCount>0) && validCounts(p.counts,s,p.candidateCount));
        const validRun=r=>safe(()=>r && B.uuid(r.runId) && B.uuid(r.policyId) && r.statusCode==="INVENTORIED"
            && B.hash(r.scopeHash) && B.hash(r.candidateHash) && B.count(r.rowVersion) && B.count(r.candidateCount) && r.candidateCount>0
            && B.count(r.remainingItemCount) && B.count(r.deletedItemCount) && r.remainingItemCount+r.deletedItemCount===r.candidateCount
            && B.count(r.segmentSize) && r.segmentSize>=1 && r.segmentSize<=1000 && r.segmentCount===Math.ceil(r.candidateCount/r.segmentSize)
            && r.frozenScope?.schemaVersion===1 && r.frozenScope.inventoryOnly===true && r.frozenScope.previewHttpRequests===0
            && scope(r.frozenScope.filter).policyId===r.policyId && r.frozenScope.filter.segmentSize===r.segmentSize
            && validCounts(r.frozenScope.counts,r.frozenScope.filter,r.candidateCount) && time(r.createdAt));
        const originalSize=(r,n)=>Math.min(r.segmentSize,r.candidateCount-(n-1)*r.segmentSize);
        const validSegment=(s,r)=>!!(validRun(r) && s?.runId===r.runId && B.count(s.segmentNo) && s.segmentNo>=1 && s.segmentNo<=r.segmentCount
            && s.itemCount===originalSize(r,s.segmentNo) && B.count(s.remainingItemCount) && B.count(s.deletedItemCount)
            && s.remainingItemCount+s.deletedItemCount===s.itemCount);
        const validItem=(i,r,n)=>!!(i && B.count(i.ordinal) && i.ordinal>(n-1)*r.segmentSize && i.ordinal<=Math.min(n*r.segmentSize,r.candidateCount)
            && [i.sourceId,i.contentVersionId,i.baseEvaluationId,i.ruleReleaseId].every(B.uuid) && B.hash(i.inputHash)
            && r.frozenScope.filter.providerCodes.includes(i.providerCode) && typeof i.currentInputMatches==="boolean");
        const validReservationPreview=(p,r,n)=>safe(()=>validRun(r) && p?.runId===r.runId && p.segmentNo===n
            && B.count(n) && n>=1 && n<=r.segmentCount && B.count(p.runVersion) && p.runVersion>=r.rowVersion && B.hash(p.segmentHash)
            && p.originalItemCount===originalSize(r,n) && B.count(p.remainingItemCount) && B.count(p.deletedItemCount)
            && p.remainingItemCount+p.deletedItemCount===p.originalItemCount
            && Object.hasOwn(labels,p.readinessCode) && !["INVENTORIED","UNRESERVED"].includes(p.readinessCode)
            && p.canReserve===(p.readinessCode==="READY")
            && (p.readinessCode==="ALREADY_RESERVED"?B.uuid(p.batchId) && p.batchPreview===null:p.batchId===null)
            && (p.readinessCode==="ALL_ITEMS_DELETED"?p.remainingItemCount===0:p.readinessCode==="ALREADY_RESERVED" || p.remainingItemCount>0)
            && (["READY","BATCH_NOT_READY"].includes(p.readinessCode)?
                B.validScope(p.batchPreview,p.batchPreview?.scope) && sameScope({...p.batchPreview.scope,segmentSize:p.batchPreview.scope.maximumCount},r.frozenScope.filter)
                && p.batchPreview.candidateCount===p.remainingItemCount && p.batchPreview.selectedCount===p.remainingItemCount
                && p.batchPreview.remainingCount===0 && p.batchPreview.canReserve===p.canReserve:p.batchPreview===null));
        function validSummary(s,r) {
            return safe(()=>validRun(r) && s?.runId===r.runId && s.candidateCount===r.candidateCount && s.segmentCount===r.segmentCount
                && [s.remainingItemCount,s.deletedItemCount,s.reservedSegmentCount,s.reservedRemainingItemCount,s.unreservedItemCount,s.unreservedInputChangedCount].every(B.count)
                && s.remainingItemCount+s.deletedItemCount===s.candidateCount && s.reservedSegmentCount<=s.segmentCount
                && s.reservedRemainingItemCount+s.unreservedItemCount===s.remainingItemCount && s.unreservedInputChangedCount<=s.unreservedItemCount
                && [s.collectionCounts,s.applicationCounts,s.rollbackCounts].every(d=>d && !Array.isArray(d) && Object.values(d).every(B.count)
                    && !Object.hasOwn(d,"MISSING_JOB") && Object.values(d).reduce((a,n)=>a+n,0)===s.remainingItemCount
                    && (d.UNRESERVED||0)===s.unreservedItemCount));
        }
        function command(kind,state,reason,acknowledged) {
            check(acknowledged===true,"전체/분할 대상·삭제 수와 예약만 수행되는 영향을 확인하고 동의하세요.");
            check(typeof reason==="string" && reason.trim().length>0 && reason.length<=1000,"작업 사유를 공백이 아닌 1~1000자로 입력하세요.");
            if(kind==="inventory") {
                const p=state.preview;check(validPreview(p,p?.scope) && p.canInventory,"전체 범위를 다시 조회하고 후보 수를 확인하세요.");
                return {kind,path:base,method:"POST",keyed:true,payload:{scope:p.scope,expectedScopeHash:p.scopeHash,expectedCandidateCount:p.candidateCount,reason}};
            }
            check(kind==="reserve","지원하지 않는 전체 분할 작업입니다.");
            const p=state.reservation;check(validReservationPreview(p,state.run,state.segmentNo) && p.canReserve,"현재 분할의 예약 준비 상태를 다시 조회하세요.");
            return {kind,path:`${base}/${p.runId}/segments/${p.segmentNo}/reservation`,method:"POST",keyed:true,runId:p.runId,segmentNo:p.segmentNo,
                originalItemCount:p.originalItemCount,payload:{expectedRunVersion:p.runVersion,expectedSegmentHash:p.segmentHash,
                    expectedRemainingItemCount:p.remainingItemCount,expectedDeletedItemCount:p.deletedItemCount,reason}};
        }
        function validReceipt(d,sent) {
            const p=sent.payload;
            if(sent.kind==="inventory")return safe(()=>validRun(d) && d.scopeHash===p.expectedScopeHash && d.candidateCount===p.expectedCandidateCount && sameScope(d.frozenScope.filter,p.scope));
            return !!(sent.kind==="reserve" && d?.runId===sent.runId && d.segmentNo===sent.segmentNo && B.uuid(d.batchId)
                && d.inventoryVersionAtReservation===p.expectedRunVersion && d.segmentHash===p.expectedSegmentHash
                && d.reservedItemCount===p.expectedRemainingItemCount && d.deletedBeforeReservation===p.expectedDeletedItemCount
                && d.originalItemCount===sent.originalItemCount && d.originalItemCount===d.reservedItemCount+d.deletedBeforeReservation
                && typeof d.currentBatchStatusCode==="string" && time(d.reservedAt));
        }
        const batchLink=id=>{check(B.uuid(id));return `/app/admin/announcement-attachment-batches?batchId=${id}`;};
        return {base,label,scope,sameScope,validPreview,validRun,validSegment,validItem,validReservationPreview,validSummary,command,validReceipt,batchLink};
    }
    if(typeof module!=="undefined" && module.exports)module.exports={create};else root.SanebAttachmentBackfill=create(root.SanebAttachmentBatch);
})(globalThis);
