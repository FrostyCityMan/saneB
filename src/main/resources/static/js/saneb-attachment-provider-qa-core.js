/* 수집원 QA의 분할 예약·이력·취소 계약. 성공 응답도 정책 게시/실제 공고 정상 판정이 아니다. */
((root) => {
    "use strict";
    const size=20, route="/app/admin/announcement-attachment-provider-qa";
    const states=["BUILDING","READY","RUNNING","CANCEL_REQUESTED","CANCELLED","COMPLETED","FAILED"];
    const labels={BUILDING:"예약 준비 중",READY:"실행 대기",RUNNING:"실행 중",CANCEL_REQUESTED:"취소 요청됨 · 소유자 정리 대기",CANCELLED:"취소 종료",
        COMPLETED:"분할 실행 종료 · 전체 QA 통과 아님",FAILED:"실행 실패",PENDING:"항목 대기",PASSED:"표본의 기대 동작 일치 · 정상 공고 판정 아님"};
    const code=v=>typeof v==="string"&&/^[A-Z0-9_-]{1,100}$/.test(v), count=(v,min,max)=>Number.isSafeInteger(v)&&v>=min&&v<=max;
    const budgets=v=>count(v.maximumRequests,1,440000)&&count(v.maximumBytes,1,838860800000)&&count(v.maximumSecondsIncludingMargin,61,82800);
    function segment(s){return !!(s&&count(s.ordinal,1,10000)&&Array.isArray(s.caseCodes)&&count(s.caseCodes.length,1,10000)
        &&s.caseCodes.every(code)&&new Set(s.caseCodes).size===s.caseCodes.length&&budgets(s));}
    function preview(v,id,n,P){return !!(v&&v.policyId===id&&P.version(v.policyVersion)&&[v.snapshotHash,v.catalogHash,v.planHash].every(P.hash)
        &&count(v.targetCount,2,1002)&&count(v.catalogCaseCount,0,10000)&&count(v.executableCaseCount,0,v.catalogCaseCount)
        &&typeof v.isExpectationCoverageComplete==="boolean"&&v.isQaPassed===false&&typeof v.isReservationEnabled==="boolean"
        &&P.page(v.segments,n,size,segment)&&v.segments.totalCount<=v.executableCaseCount
        &&((v.executableCaseCount===0)===(v.segments.totalCount===0))&&(!v.isReservationEnabled||v.segments.totalCount>0)
        &&v.segments.items.every((s,i)=>s.ordinal===(n-1)*size+i+1)
        &&v.segments.items.reduce((a,s)=>a+s.caseCodes.length,0)<=v.executableCaseCount
        &&new Set(v.segments.items.flatMap(s=>s.caseCodes)).size===v.segments.items.reduce((a,s)=>a+s.caseCodes.length,0));}
    const planFields=["planHash","segmentNo","segmentCount","catalogCaseCount","executableCaseCount","isExpectationCoverageComplete","maximumSecondsIncludingMargin"];
    function run(r,id,P){
        if(!r||!P.uuid(r.runId)||r.policyId!==id||!P.version(r.policyVersion)||!P.version(r.rowVersion)||!states.includes(r.statusCode)
            ||![r.snapshotHash,r.catalogHash].every(P.hash)||!count(r.expectedCaseCount,1,10000)||!count(r.maximumRequests,1,440000)
            ||!count(r.maximumBytes,1,838860800000)||!count(r.requestReservations,0,r.maximumRequests)||!count(r.reservedBytes,0,r.maximumBytes)
            ||typeof r.isInputVersionsCurrent!=="boolean"||r.isQaPassed!==false||!P.time(r.createdAt)||!P.time(r.expiresAt)
            ||Date.parse(r.expiresAt)<=Date.parse(r.createdAt)||!(r.completedAt===null||P.time(r.completedAt))
            ||(["COMPLETED","FAILED","CANCELLED"].includes(r.statusCode)!==(r.completedAt!==null)))return false;
        if(planFields.every(k=>r[k]===null))return true;
        return P.hash(r.planHash)&&count(r.segmentCount,1,10000)&&count(r.segmentNo,1,r.segmentCount)
            &&count(r.catalogCaseCount,1,10000)&&count(r.executableCaseCount,r.expectedCaseCount,r.catalogCaseCount)
            &&typeof r.isExpectationCoverageComplete==="boolean"&&budgets(r);
    }
    function item(i,P){return !!(i&&P.uuid(i.caseId)&&count(i.ordinal,1,10000)&&code(i.caseCode)&&[i.inputHash,i.profileHash].every(P.hash)
        &&count(i.expectedFileCount,0,10)&&["PENDING","RUNNING","PASSED","FAILED","CANCELLED"].includes(i.statusCode)&&P.version(i.rowVersion)
        &&count(i.requestReservations,0,44)&&count(i.reservedBytes,0,83886080)&&(i.startedAt===null||P.time(i.startedAt))
        &&(i.completedAt===null||P.time(i.completedAt))&&(i.errorCode===null||typeof i.errorCode==="string"&&/^[A-Z][A-Z0-9_]{0,79}$/.test(i.errorCode))
        &&(i.evidenceHash===null||P.hash(i.evidenceHash)));}
    function parameters(search,P){const p=new URLSearchParams(search),out={policyId:p.get("policyId"),runId:p.get("runId")};
        P.requireValue(P.uuid(out.policyId)&&(out.runId===null||P.uuid(out.runId))&&[...p.keys()].every(k=>["policyId","runId","planPage","runPage","casePage"].includes(k))
            &&[...new Set(p.keys())].every(k=>p.getAll(k).length===1),"정책·실행 주소가 올바르지 않습니다. 정책 관리에서 다시 진입하세요.");
        out.policyId=out.policyId.toLowerCase();if(out.runId)out.runId=out.runId.toLowerCase();
        for(const key of ["planPage","runPage","casePage"]){const n=p.get(key)||"1";P.requireValue(/^[1-9][0-9]*$/.test(n)&&count(Number(n),1,1000000),"조회 페이지는 1~1000000의 정수여야 합니다.");out[key]=Number(n);}return out;}
    function reservation(plan,s,input,P){
        P.requireValue(plan?.isReservationEnabled===true&&plan.isQaPassed===false&&segment(s)&&plan.segments.items.some(v=>JSON.stringify(v)===JSON.stringify(s)),"현재 조회한 실행 가능한 분할을 선택하세요.");
        P.requireValue(input.acknowledgeScope===true,"이번 분할의 전체 공고 코드와 건수를 확인하세요.");
        P.requireValue(input.acknowledgeNetworkBudget===true,"외부 요청 횟수·다운로드 용량·최대 시간을 확인하세요.");
        P.requireValue(plan.isExpectationCoverageComplete||input.acknowledgeIncompleteCoverage===true,"전체 기대값이 미완료인 일부 QA임을 확인하세요.");
        P.requireValue(typeof input.reason==="string"&&input.reason.trim().length>0&&input.reason.length<=1000,"예약 사유를 공백이 아닌 1~1000자로 입력하세요.");
        return {expectedVersion:plan.policyVersion,expectedSnapshotHash:plan.snapshotHash,expectedCatalogHash:plan.catalogHash,expectedPlanHash:plan.planHash,
            segmentNo:s.ordinal,expectedCaseCount:s.caseCodes.length,maximumRequests:s.maximumRequests,maximumBytes:s.maximumBytes,
            maximumSecondsIncludingMargin:s.maximumSecondsIncludingMargin,acknowledgeScope:true,acknowledgeNetworkBudget:true,
            acknowledgeIncompleteCoverage:input.acknowledgeIncompleteCoverage===true,reason:input.reason.trim()};
    }
    function receipt(r,s,P){if(!run(r,s.policyId,P))return false;
        const p=JSON.parse(s.body);
        if(s.kind==="cancel")return r.runId===s.runId&&r.rowVersion>p.expectedVersion&&["CANCEL_REQUESTED","CANCELLED"].includes(r.statusCode);
        return r.policyVersion===p.expectedVersion&&r.snapshotHash===p.expectedSnapshotHash&&r.catalogHash===p.expectedCatalogHash&&r.planHash===p.expectedPlanHash
            &&r.segmentNo===p.segmentNo&&r.expectedCaseCount===p.expectedCaseCount&&r.maximumRequests===p.maximumRequests&&r.maximumBytes===p.maximumBytes
            &&r.maximumSecondsIncludingMargin===p.maximumSecondsIncludingMargin&&r.segmentCount===s.segmentCount&&r.catalogCaseCount===s.catalogCaseCount
            &&r.executableCaseCount===s.executableCaseCount&&r.isExpectationCoverageComplete===s.isExpectationCoverageComplete;
    }
    function mutations(request,uuid,P){let sent=null,pending=false,uncertain=false;
        return {get sent(){return sent;},get pending(){return pending;},get uncertain(){return uncertain;},
            async execute(next){P.requireValue(!pending&&!(!sent&&!next)&&!(uncertain&&next),"이전 요청 결과를 먼저 확인하세요.");
                if(!sent){P.requireValue(P.uuid(next.policyId)&&["reserve","cancel"].includes(next.kind));
                    P.requireValue(next.kind!=="cancel"||P.uuid(next.runId));
                    const path=`${P.base}/${next.policyId}/provider-qa-runs`+(next.kind==="cancel"?`/${next.runId}/cancellation`:"");
                    P.requireValue(next.path===path,"QA 작업 경로가 선택한 정책·실행과 다릅니다.");
                    const key=next.kind==="reserve"?uuid():null;P.requireValue(key===null||P.uuid(key));
                    sent=Object.freeze({...next,body:JSON.stringify(next.payload),key});}
                const prior=uncertain;pending=true;
                try {const data=await request(sent.path,{method:sent.kind==="reserve"?"POST":"PUT",body:sent.body,headers:sent.key?{"Idempotency-Key":sent.key}:{}});
                    if(!receipt(data,sent,P))throw new P.RequestError("변경 응답의 대상·분할·예산이 요청과 다릅니다. 원래 요청을 재확인하세요.");
                    const result={data,sent};sent=null;uncertain=false;return result;
                }catch(e){uncertain=prior||e.uncertain===true;if(!uncertain)sent=null;throw e;}finally{pending=false;}
            },
            reconcile(current,ack){P.requireValue(uncertain&&!pending&&sent?.kind==="cancel"&&ack===true,"최신 취소 대상 조회와 별도 인지 확인이 필요합니다.");
                P.requireValue(run(current,sent.policyId,P)&&current.runId===sent.runId&&current.rowVersion>=JSON.parse(sent.body).expectedVersion);
                sent=null;uncertain=false;
            }
        };
    }
    const api={size,route,segment,preview,run,item,parameters,reservation,receipt,mutations,label:v=>labels[v]||`확인 필요 (${v})`};
    if(typeof module!=="undefined"&&module.exports)module.exports=api;else root.SanebAttachmentProviderQa=api;
})(globalThis);
