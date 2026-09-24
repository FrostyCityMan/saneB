/* 정책 초안·QA 계약. 초안 저장, 검증, 게시와 운영 적용은 서로 다른 단계다. */
((root) => {
    "use strict";
    const base="/api/v2/admin/announcement-attachment-policies",rules="/api/v1/admin/announcement-source-rule-releases";
    const uuid=v=>typeof v==="string" && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v);
    const hash=v=>typeof v==="string" && /^[0-9a-f]{64}$/.test(v),integer=v=>Number.isSafeInteger(v)&&v>=0;
    const version=v=>integer(v)&&v<=2147483647,time=v=>typeof v==="string"&&Number.isFinite(Date.parse(v));
    const modes=["OFF","COLLECT_ONLY","ENFORCE"],statuses=["DRAFT","ACTIVE","RETIRED"];
    const segmentVersions={"segment-role-1.0.0":"fb807a5fcf11c102badcc35cc4b60c6abe7fa36672e2aa431e3b5f2dc16bcdde",
        "segment-role-1.0.2":"2f02f48368ce3f42557dd62094dec8e6b99d44e27d0f51f265a2fd737aabdd82",
        "segment-role-1.0.3":"8b9fdd872f3eb9890146d6e360408204ff285f4b23977e07693834aceec66d43"};
    const segmentEditable=d=>d?.configuration?.engineVersion==="attachment-segment-1.0.0"
        &&Object.hasOwn(segmentVersions,d.configuration.segmentRuleVersion)
        &&segmentVersions[d.configuration.segmentRuleVersion]===d.configuration.segmentRulesHash;
    const steps=["CLASSIFICATION_GOLDEN","INSTALLED_RUNTIME","PROVIDER_PROFILES","WORKER_DB_RECOVERY"];
    const runStates=["PENDING","RUNNING","CANCEL_REQUESTED","CANCELLED","INCOMPLETE","CONFLICT","FAILED","VERIFIED"];
    const labels={DRAFT:"초안 · 운영 미반영",ACTIVE:"게시 중",RETIRED:"퇴역 · 이력 보존",OFF:"새 첨부 수집 중지",
        COLLECT_ONLY:"수집·미리보기만",ENFORCE:"첨부 판정 적용 정책",PENDING:"QA 대기",RUNNING:"QA 실행 중",
        CANCEL_REQUESTED:"취소 요청됨 · 현재 파일 정리 대기",CANCELLED:"QA 취소됨",INCOMPLETE:"필수 QA 증거 부족 · 게시 불가",
        CONFLICT:"QA 입력 변경 · 재검증 필요",FAILED:"QA 실행 실패",VERIFIED:"QA 검증됨 · 게시 재검증·승인은 별도",
        PASSED:"단계 통과",MISSING:"필수 증거 없음",NOT_RUN:"아직 실행하지 않음",
        CLASSIFICATION_GOLDEN:"분류 정답 세트",INSTALLED_RUNTIME:"설치된 격리 추출기",PROVIDER_PROFILES:"전체 수집 방식·실파일",
        WORKER_DB_RECOVERY:"격리 DB 작업·복구",POLICY_NOT_DRAFT:"게시·퇴역 정책입니다. 새 개정 초안이 필요합니다.",
        KEYWORD_RULE_NOT_ACTIVE:"연결 키워드 규칙이 게시 상태가 아닙니다. 규칙을 자동 게시하지 않습니다.",
        QA_NOT_REQUESTED:"정책 QA 이력이 없습니다.",QA_NOT_VERIFIED:"최신 QA가 전체 검증을 통과하지 않았습니다.",
        QA_INPUT_VERSIONS_CHANGED:"QA 이후 정책 또는 규칙 버전이 바뀌었습니다.",QA_REQUIRED_STEPS_NOT_PASSED:"필수 QA 단계 중 미통과 항목이 있습니다.",
        PUBLICATION_REVALIDATION_REQUIRED:"게시 직전 전체 증거·대상·영향 재검증과 별도 승인이 필요합니다.",
        REQUIRED_QA_EVIDENCE_MISSING:"전체 실파일 또는 작업 DB 증거가 없습니다.",ALL_PROFILE_REAL_FILE_QA_REQUIRED:"모든 대상 수집 방식의 실파일 QA가 필요합니다.",
        ISOLATED_WORKER_DB_QA_REQUIRED:"격리 DB의 실제 작업·복구 검증이 필요합니다.",LEASE_EXPIRED:"실행 소유 시간이 만료됐습니다. 이력을 확인한 뒤 새 QA를 예약하세요.",
        VALIDATION_INPUT_OR_RESULT_CHANGED:"예약 입력과 현재 설정·실행 근거가 다릅니다. 새 QA가 필요합니다.",
        RUNTIME_QA_FAILED:"격리 추출기 검증에 실패했습니다. 설치·실행 근거를 확인하세요.",QA_CHECK_FAILED:"필수 QA 사례가 실패했습니다.",
        QA_EXECUTION_FAILED:"QA 실행에 실패했습니다. 서버의 비식별 오류 기록을 확인하세요.",BIZINFO:"기업마당",GOV24:"정부24",GOV24_PUBLIC_SERVICE:"정부24",LOCAL_GOV_NOTICE:"지자체"};
    const label=v=>v==null?"없음":labels[v]||`확인 필요 (${v})`;
    const requireValue=(ok,message="정책 응답의 대상·버전·상태가 일치하지 않습니다. 최신 상태를 다시 조회하세요.")=>{if(!ok)throw new Error(message);};
    const summary=p=>!!(p&&uuid(p.policyId)&&typeof p.policyCode==="string"&&p.policyCode.length<=120&&version(p.versionNo)&&p.versionNo>0
        &&version(p.rowVersion)&&statuses.includes(p.policyStatusCode)&&modes.includes(p.modeCode)&&uuid(p.ruleReleaseId)
        &&statuses.includes(p.ruleReleaseStatusCode)&&(p.policyHash===null||hash(p.policyHash))&&time(p.createdAt));
    const details=d=>!!(d&&summary(d.policy)&&integer(d.configuration?.maximumSourceBytes)&&d.configuration.maximumSourceBytes>=1
        &&d.configuration.maximumSourceBytes<=83886080&&typeof d.configuration.engineVersion==="string"&&typeof d.configuration.extractorVersion==="string"
        &&((d.configuration.roleRuleVersion==null&&d.configuration.roleRulesHash==null)
            ||typeof d.configuration.roleRuleVersion==="string"&&/^[A-Za-z0-9_.-]{1,40}$/.test(d.configuration.roleRuleVersion)&&hash(d.configuration.roleRulesHash))
        &&((d.configuration.segmentRuleVersion==null&&d.configuration.segmentRulesHash==null&&d.configuration.engineVersion!=="attachment-segment-1.0.0")
            ||typeof d.configuration.segmentRuleVersion==="string"&&/^[A-Za-z0-9_.-]{1,40}$/.test(d.configuration.segmentRuleVersion)&&hash(d.configuration.segmentRulesHash))
        &&typeof d.isEditable==="boolean"&&typeof d.isDraftValidationRequired==="boolean"&&(!d.isEditable||d.policy.policyStatusCode==="DRAFT")
        &&(d.copiedFromPolicyId===null||uuid(d.copiedFromPolicyId))&&time(d.updatedAt)&&Array.isArray(d.systemProfileBindings)&&d.systemProfileBindings.length<=1000
        &&d.systemProfileBindings.every(p=>["BIZINFO","GOV24","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE"].includes(p.providerCode)&&typeof p.profileCode==="string"&&hash(p.profileHash)));
    const validSteps=list=>Array.isArray(list)&&list.length===4&&new Set(list.map(s=>s.stepCode)).size===4
        &&list.every(s=>steps.includes(s.stepCode)&&["PASSED","FAILED","MISSING","NOT_RUN"].includes(s.statusCode)
            &&(s.statusCode==="NOT_RUN"?s.evidenceHash===null:hash(s.evidenceHash)));
    const run=(r,policyId)=>!!(r&&uuid(r.runId)&&r.policyId===policyId&&version(r.policyVersion)&&uuid(r.ruleReleaseId)&&version(r.ruleVersion)
        &&hash(r.snapshotHash)&&runStates.includes(r.statusCode)&&version(r.rowVersion)&&typeof r.inputVersionsCurrent==="boolean"
        &&time(r.createdAt)&&(r.completedAt===null||time(r.completedAt))&&validSteps(r.steps));
    const countFields={boundSourceCount:"정책 연결 원문",reviewRequiredSourceCount:"검수 필요 원문",effectiveAttachmentSourceCount:"첨부 판정 연결 원문",
        linkedSourceCount:"운영 공고 연결 원문",frozenCollectionJobCount:"고정된 미종료 수집 작업",runningCollectionJobCount:"실행 중 수집 작업",
        applicationPendingJobCount:"판정 적용 대기 작업",rollbackPendingJobCount:"원복 대기 작업",frozenCollectionPlanCount:"누적 고정 수집 계획"};
    const counts=c=>!!(c&&Object.keys(countFields).every(k=>integer(c[k]))&&c.reviewRequiredSourceCount<=c.boundSourceCount
        &&c.effectiveAttachmentSourceCount<=c.boundSourceCount&&c.linkedSourceCount<=c.boundSourceCount&&c.runningCollectionJobCount<=c.frozenCollectionJobCount);
    const impact=(i,d)=>!!(details(d)&&summary(i?.policy)&&i.policy.policyId===d.policy.policyId&&i.policy.rowVersion===d.policy.rowVersion
        &&i.policy.modeCode===d.policy.modeCode&&i.policy.ruleReleaseId===d.policy.ruleReleaseId&&i.policy.policyHash===d.policy.policyHash
        &&(i.activePolicyForRule===null||summary(i.activePolicyForRule)&&i.activePolicyForRule.policyStatusCode==="ACTIVE"&&i.activePolicyForRule.ruleReleaseId===d.policy.ruleReleaseId)
        &&counts(i.matchingRule)&&counts(i.allRules)&&Object.keys(countFields).every(k=>i.matchingRule[k]<=i.allRules[k])
        &&i.maximumSourceBytes===d.configuration.maximumSourceBytes&&typeof i.wouldStopNewExternalRequests==="boolean"&&typeof i.wouldLiftGlobalOffStop==="boolean"
        &&i.requiresPublicationRevalidation===true&&i.currentHttpRequests===0&&hash(i.observedImpactHash)&&time(i.observedAt)
        &&Array.isArray(i.blockingReasonCodes)&&i.blockingReasonCodes.includes("PUBLICATION_REVALIDATION_REQUIRED")
        &&i.blockingReasonCodes.every(c=>typeof c==="string"&&/^[A-Z_]{1,100}$/.test(c))
        &&(i.latestQa===null||uuid(i.latestQa.runId)&&runStates.includes(i.latestQa.statusCode)&&version(i.latestQa.policyVersion)
            &&version(i.latestQa.rowVersion)&&version(i.latestQa.ruleVersion)&&typeof i.latestQa.inputVersionsCurrent==="boolean"&&hash(i.latestQa.snapshotHash)&&validSteps(i.latestQa.steps)));
    const page=(p,n,size,validate)=>!!(p&&p.page===n&&p.size===size&&integer(p.totalCount)&&p.totalPages===Math.ceil(p.totalCount/size)
        &&Array.isArray(p.items)&&p.items.length===Math.min(size,Math.max(0,p.totalCount-(n-1)*size))&&p.items.every(validate));
    const rule=r=>!!(r&&uuid(r.releaseId)&&statuses.includes(r.releaseStatusCode)&&typeof r.releaseCode==="string"&&version(r.versionNo));
    const scopeSummary=(s,id)=>!!(s&&uuid(s.scopeId)&&s.policyId===id&&version(s.policyVersion)&&uuid(s.ruleReleaseId)&&version(s.ruleVersion)
        &&modes.includes(s.modeCode)&&integer(s.itemCount)&&s.itemCount>0&&hash(s.scopeHash)&&time(s.createdAt)&&time(s.expiresAt)
        &&Date.parse(s.expiresAt)>Date.parse(s.createdAt)&&Date.parse(s.expiresAt)-Date.parse(s.createdAt)<=900000
        &&(s.qaRunId===null?s.qaSnapshotHash===null:uuid(s.qaRunId)&&hash(s.qaSnapshotHash)));
    const scope=(s,id)=>!!(scopeSummary(s?.scope,id)&&typeof s.isExpired==="boolean"&&typeof s.isScopeCurrent==="boolean"
        &&!(s.isExpired&&s.isScopeCurrent)&&s.isApproval===false&&s.requiresPublicationRevalidation===true&&s.currentHttpRequests===0);
    const scopeItem=i=>!!(i&&["POLICY","SOURCE","JOB","COLLECTION_PLAN","COLLECTOR"].includes(i.entityTypeCode)&&uuid(i.entityId)&&hash(i.stateHash));
    const publication=(r,id)=>{const p=r?.publication;return !!(p&&uuid(p.publicationId)&&p.policyId===id&&version(p.publishedPolicyVersion)&&p.publishedPolicyVersion>0
        &&hash(p.policyHash)&&uuid(p.scopeId)&&hash(p.scopeHash)&&uuid(p.qaRunId)&&modes.includes(p.modeCode)&&time(p.publishedAt)
        &&(p.previousPolicyId===null?p.previousPolicyVersion===null:uuid(p.previousPolicyId)&&p.previousPolicyId!==id&&version(p.previousPolicyVersion))
        &&r.existingDataApplied===false&&r.workerEnabledByRequest===false&&r.currentHttpRequests===0);};
    // UI 준비 판정일 뿐 실제 QA 증거 검증·게시 승인은 서버가 최종 수행한다.
    function canPublish(state,now=Date.now()) {
        const d=state.detail,i=state.impact,s=state.scope?.scope,q=i?.latestQa;
        return !!(details(d)&&d.isEditable&&d.policy.policyStatusCode==="DRAFT"&&d.policy.ruleReleaseStatusCode==="ACTIVE"
            &&d.policy.rowVersion<2147483647&&impact(i,d)&&scope(state.scope,d.policy.policyId)&&!state.scope.isExpired&&state.scope.isScopeCurrent
            &&Date.parse(s.expiresAt)>now&&s.policyVersion===d.policy.rowVersion&&s.ruleReleaseId===d.policy.ruleReleaseId&&s.modeCode===d.policy.modeCode
            &&q&&q.statusCode==="VERIFIED"&&q.inputVersionsCurrent&&q.policyVersion===s.policyVersion&&q.ruleVersion===s.ruleVersion
            &&q.runId===s.qaRunId&&q.snapshotHash===s.qaSnapshotHash&&q.steps.every(step=>step.statusCode==="PASSED")
            &&i.blockingReasonCodes.length===1&&i.blockingReasonCodes[0]==="PUBLICATION_REVALIDATION_REQUIRED");
    }
    function command(kind,state,input,reason,acknowledged) {
        requireValue(acknowledged===true,"실행 대상·버전·영향을 확인하고 동의하세요.");
        requireValue(typeof reason==="string"&&reason.trim().length>0&&reason.length<=1000,"사유는 공백이 아닌 1~1000자로 입력하세요.");
        const d=state.detail,r=state.run;let path=base,method="POST",keyed=true,payload={reason};
        if(kind!=="create"){requireValue(details(d));path+=`/${d.policy.policyId}`;}
        if(kind==="create"||kind==="update") {
            requireValue(uuid(input.ruleReleaseId),"저장할 키워드 규칙을 선택하세요.");requireValue(modes.includes(input.modeCode),"첨부 정책 모드를 선택하세요.");
            const bytes=Number(input.maximumSourceBytes);requireValue(integer(bytes)&&bytes>=1&&bytes<=83886080,"공고별 한도는 1~83,886,080바이트의 정수로 입력하세요.");
            Object.assign(payload,{ruleReleaseId:input.ruleReleaseId,modeCode:input.modeCode,maximumSourceBytes:bytes});
            if(input.segmentRuleVersion!=null&&input.segmentRuleVersion!=="") {
                requireValue(Object.hasOwn(segmentVersions,input.segmentRuleVersion),"구간 규칙은 1.0.0, 1.0.2, 1.0.3 중 하나를 선택하세요.");
                requireValue(kind==="create"||segmentEditable(d),"기존 파일 단위 엔진은 구간 규칙을 선택할 수 없습니다. 새 정책 초안을 만드세요.");
                payload.segmentRuleVersion=input.segmentRuleVersion;
            }
            if(kind==="update"){requireValue(d.isEditable&&d.policy.rowVersion<2147483647,"수정 가능한 최신 초안을 조회하세요.");payload.expectedVersion=d.policy.rowVersion;method="PUT";keyed=false;}
        } else if(kind==="revision") {path+="/revisions";payload.expectedVersion=d.policy.rowVersion;
        } else if(kind==="qa") {requireValue(d.isEditable,"저장한 초안에서 QA를 예약하세요.");path+="/validation-runs";payload.expectedVersion=d.policy.rowVersion;
        } else if(kind==="cancel") {requireValue(run(r,d.policy.policyId)&&["PENDING","RUNNING"].includes(r.statusCode),"대기·실행 중인 QA를 다시 조회한 후 취소하세요.");
            path+=`/validation-runs/${r.runId}/cancellation`;payload.expectedVersion=r.rowVersion;method="PUT";keyed=false;
        } else if(kind==="prepare") {requireValue(d.isEditable&&impact(state.impact,d),"수정 가능한 초안과 게시 영향을 다시 조회하세요.");
            path+="/publication-scopes";payload.expectedVersion=d.policy.rowVersion;
        } else if(kind==="publish") {requireValue(canPublish(state),"최신 전체 QA와 유효한 게시 준비 범위가 필요합니다. 정책·QA·범위를 다시 조회하세요.");
            requireValue(input.acknowledgeNewCollectionBehavior===true&&input.acknowledgeExistingJobsUnchanged===true&&input.acknowledgeNoBackfill===true,
                "신규 수집 영향·기존 고정 작업 보존·기존 데이터 별도 처리의 세 항목을 각각 확인하세요.");
            path+="/publication";Object.assign(payload,{scopeId:state.scope.scope.scopeId,scopeHash:state.scope.scope.scopeHash,expectedVersion:d.policy.rowVersion,
                acknowledgeNewCollectionBehavior:true,acknowledgeExistingJobsUnchanged:true,acknowledgeNoBackfill:true});
        } else throw new Error("지원하지 않는 정책 작업입니다.");
        return {kind,path,method,keyed,payload,policyId:d?.policy.policyId||null,runId:r?.runId||null,
            ...(kind==="update"?{expectedSegmentVersion:payload.segmentRuleVersion??d.configuration.segmentRuleVersion??null,
                expectedSegmentHash:payload.segmentRuleVersion?segmentVersions[payload.segmentRuleVersion]:d.configuration.segmentRulesHash??null}:{}),
            ...(kind==="publish"?{qaRunId:state.scope.scope.qaRunId,policyHash:state.scope.scope.qaSnapshotHash,modeCode:d.policy.modeCode}:{})};
    }
    function receipt(d,s) {
        if(s.kind==="prepare")return scope(d,s.policyId)&&d.scope.policyVersion===s.payload.expectedVersion;
        if(s.kind==="publish")return publication(d,s.policyId)&&d.publication.scopeId===s.payload.scopeId&&d.publication.scopeHash===s.payload.scopeHash
            &&d.publication.publishedPolicyVersion===s.payload.expectedVersion+1&&d.publication.qaRunId===s.qaRunId
            &&d.publication.policyHash===s.policyHash&&d.publication.modeCode===s.modeCode;
        if(["qa","cancel"].includes(s.kind))return run(d,s.policyId)&&(s.kind==="qa"?d.policyVersion===s.payload.expectedVersion:d.runId===s.runId&&d.rowVersion>=s.payload.expectedVersion&&["CANCEL_REQUESTED","CANCELLED"].includes(d.statusCode));
        if(!details(d))return false;
        if(s.kind==="update")return d.policy.policyId===s.policyId&&d.policy.rowVersion===s.payload.expectedVersion+1
            &&(d.configuration.segmentRuleVersion??null)===s.expectedSegmentVersion&&(d.configuration.segmentRulesHash??null)===s.expectedSegmentHash
            &&d.policy.ruleReleaseId===s.payload.ruleReleaseId&&d.policy.modeCode===s.payload.modeCode&&d.configuration.maximumSourceBytes===s.payload.maximumSourceBytes;
        if(s.kind==="revision")return d.copiedFromPolicyId===s.policyId&&d.policy.policyId!==s.policyId;
        return s.kind==="create"&&d.copiedFromPolicyId===null;
    }
    class RequestError extends Error {constructor(message,status=0){super(message);this.status=status;this.uncertain=status===0||status>=500;}}
    function client(fetcher,timeout=20000) {
        return async(url,options={})=>{
            const method=options.method||"GET",parts=url.split("?"),path=parts[0];
            requireValue(typeof url==="string"&&parts.length<=2&&(!parts[1]||/^[a-zA-Z0-9=&-]+$/.test(parts[1]))
                &&(path===rules&&method==="GET"||new RegExp(`^${base}(?:/[a-zA-Z0-9-]+)*$`).test(path)),"허용되지 않은 정책 요청 경로입니다.");
            const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),timeout);
            try {
                const response=await fetcher(url,{...options,credentials:"same-origin",cache:"no-store",redirect:"error",signal:controller.signal,
                    headers:{Accept:"application/json",...(options.body?{"Content-Type":"application/json"}:{}),...options.headers}});
                let body;try{body=await response.json();}catch{throw new RequestError("응답을 읽지 못했습니다. 변경 결과는 미확정입니다.",response.ok?0:response.status);}
                if(!response.ok||body?.success!==true)throw new RequestError(response.status===401?"로그인이 만료됐습니다. 입력을 유지하고 다른 탭에서 로그인하세요.":response.status===403?"관리 권한 또는 보안 확인이 유효하지 않습니다. 로그인 상태·권한을 확인하세요.":response.status>=500?"서버 처리 결과가 미확정입니다. 원래 요청을 재확인하세요.":body?.message||"현재 상태와 충돌했습니다. 최신 상태를 다시 확인하세요.",response.ok?0:response.status);
                if(!body.data||typeof body.data!=="object")throw new RequestError("응답 계약이 다릅니다. 변경 결과는 미확정입니다.");return body.data;
            } catch(e){if(e instanceof RequestError)throw e;throw new RequestError("연결이 끊겼거나 시간이 초과됐습니다. 원래 요청의 결과를 재확인하세요.");}
            finally{clearTimeout(timer);}
        };
    }
    function mutations(request,randomUUID) {
        let sent=null,pending=false,uncertain=false;
        return {
            async execute(next){requireValue(!pending,"이미 요청 중입니다.");requireValue(!uncertain||!next,"이전 요청 결과를 먼저 확인하세요.");
                if(!sent){requireValue(next);sent={...next,body:JSON.stringify(next.payload),key:next.keyed?randomUUID():null};if(sent.keyed)requireValue(uuid(sent.key));}
                const previous=uncertain;pending=true;
                try {const data=await request(sent.path,{method:sent.method,body:sent.body,headers:sent.key?{"Idempotency-Key":sent.key}:{}});
                    if(!receipt(data,sent))throw new RequestError("응답의 대상·버전이 요청과 다릅니다. 원래 요청을 재확인하세요.");
                    const result={data,sent};sent=null;uncertain=false;return result;
                }catch(e){uncertain=previous||e.uncertain===true;if(!uncertain)sent=null;throw e;}finally{pending=false;}
            },
            reconcile(current,acknowledged){requireValue(uncertain&&!pending&&sent&&!sent.keyed&&acknowledged===true,"최신 상태 확인과 별도 동의가 필요합니다.");
                requireValue(sent.kind==="update"?details(current)&&current.policy.policyId===sent.policyId&&current.policy.rowVersion>=sent.payload.expectedVersion
                    :sent.kind==="cancel"&&run(current,sent.policyId)&&current.runId===sent.runId&&current.rowVersion>=sent.payload.expectedVersion);
                // 원래 요청의 성공을 추정하지 않는다. 현재 상태를 새 검토 기준으로만 채택한다.
                sent=null;uncertain=false;
            },
            get sent(){return sent;},get pending(){return pending;},get uncertain(){return uncertain;}
        };
    }
    const api={base,rules,uuid,hash,integer,version,time,label,requireValue,summary,details,run,impact,page,rule,steps,countFields,segmentVersions,segmentEditable,
        scopeSummary,scope,scopeItem,publication,canPublish,command,receipt,RequestError,client,mutations};
    if(typeof module!=="undefined"&&module.exports)module.exports=api;else root.SanebAttachmentPolicy=api;
})(globalThis);
