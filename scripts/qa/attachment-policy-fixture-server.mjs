// 로컬 합성 SSR/API. 실제 DB·인증·첨부 다운로드·운영 게시와 연결하지 않는다.
import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
const repo=fileURLToPath(new URL('../../',import.meta.url)),html=await readFile(path.join(repo,'build/attachment-policy-ui-qa/index.html'),'utf8');
if(!html.includes('합성 QA 전용 화면'))throw new Error('명시적으로 내보낸 합성 SSR이 필요합니다.');
const id=n=>`00000000-0000-0000-0000-${String(n).padStart(12,'0')}`,hash='a'.repeat(64),now='2026-09-12T00:00:00Z';
const base='/api/v2/admin/announcement-attachment-policies',rules='/api/v1/admin/announcement-source-rule-releases';
let scenario='normal',version=0,bytes=83886080,mode='OFF',qa=null,writes=0,lost=false,currentId=id(1),copied=null,scope=null,publication=null;
const saved=new Map(),scenarios=['normal','readonly','lost-update','partial-impact','conflict','publish-ready','lost-publication','expired-scope'];
const ready=()=>['publish-ready','lost-publication','expired-scope'].includes(scenario);
const summary=()=>({policyId:currentId,policyCode:'ATT-SYNTHETIC-QA',versionNo:1,rowVersion:version,policyStatusCode:publication?'ACTIVE':'DRAFT',modeCode:mode,ruleReleaseId:id(2),ruleReleaseStatusCode:'ACTIVE',policyHash:publication?hash:null,createdAt:now,publishedAt:publication?.publication.publishedAt||null});
const details=()=>({policy:summary(),configuration:{maximumSourceBytes:bytes,engineVersion:'SYNTHETIC',extractorVersion:'SYNTHETIC',extractorConfigHash:null},systemProfileBindings:[{providerCode:'BIZINFO',profileCode:'BIZINFO_SYNTHETIC',profileHash:hash}],copiedFromPolicyId:copied,isEditable:scenario!=='readonly'&&!publication,isDraftValidationRequired:true,updatedAt:now});
const steps=()=>['CLASSIFICATION_GOLDEN','INSTALLED_RUNTIME','PROVIDER_PROFILES','WORKER_DB_RECOVERY'].map((stepCode,i)=>({stepCode,statusCode:i<2?'PASSED':'MISSING',evidenceHash:hash,evidence:i<2?{}:{reasonCode:i===2?'ALL_PROFILE_REAL_FILE_QA_REQUIRED':'ISOLATED_WORKER_DB_QA_REQUIRED'}}));
const run=()=>qa||{runId:id(3),policyId:currentId,policyVersion:publication?version-1:version,ruleReleaseId:id(2),ruleVersion:0,snapshotHash:hash,statusCode:ready()?'VERIFIED':'INCOMPLETE',rowVersion:4,inputVersionsCurrent:!publication,errorCode:ready()?null:'REQUIRED_QA_EVIDENCE_MISSING',createdAt:now,startedAt:now,completedAt:now,steps:ready()?steps().map(s=>({...s,statusCode:'PASSED',evidence:{}})):steps()};
const counts=()=>({boundSourceCount:12,reviewRequiredSourceCount:3,effectiveAttachmentSourceCount:4,linkedSourceCount:2,frozenCollectionJobCount:8,runningCollectionJobCount:1,applicationPendingJobCount:2,rollbackPendingJobCount:1,frozenCollectionPlanCount:48});
const impact=()=>({policy:summary(),activePolicyForRule:null,matchingRule:counts(),allRules:Object.fromEntries(Object.entries(counts()).map(([k,n])=>[k,n*2])),maximumSourceBytes:bytes,wouldStopNewExternalRequests:mode==='OFF',wouldLiftGlobalOffStop:false,
    latestQa:run(),blockingReasonCodes:ready()&&!publication?['PUBLICATION_REVALIDATION_REQUIRED']:['QA_NOT_VERIFIED','QA_REQUIRED_STEPS_NOT_PASSED','PUBLICATION_REVALIDATION_REQUIRED'],requiresPublicationRevalidation:true,observedImpactHash:hash,observedAt:now,currentHttpRequests:0});
const scopeDetails=()=>({scope,isExpired:Date.parse(scope.expiresAt)<=Date.now(),isScopeCurrent:Date.parse(scope.expiresAt)>Date.now()&&version===scope.policyVersion,isApproval:false,requiresPublicationRevalidation:true,currentHttpRequests:0});
const paged=(items,url)=>{const page=Number(url.searchParams.get('page')||1),size=Number(url.searchParams.get('size')||10);return {items:items.slice((page-1)*size,page*size),page,size,totalCount:items.length,totalPages:Math.ceil(items.length/size)};};
const json=(res,status,data,message='')=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify({success:status<400,data,message}));};
const assets=new Set(['/css/saneb-dashboard.css','/css/saneb-announcement-attachment-review.css','/js/saneb-attachment-policy-core.js','/js/saneb-attachment-policies.js','/js/saneb-layout.js','/images/saneb-logo-mark.svg']);
const server=http.createServer(async(req,res)=>{
    try {
        const url=new URL(req.url,'http://127.0.0.1');
        if(url.pathname==='/qa-state')return json(res,200,{scenario,writes,version,bytes,mode,qaStatus:run().statusCode,scope:!!scope,published:!!publication,productionWrites:0});
        if(url.pathname==='/qa-scenario'&&scenarios.includes(url.searchParams.get('name'))){scenario=url.searchParams.get('name');version=0;bytes=83886080;mode='OFF';qa=null;writes=0;lost=false;currentId=id(1);copied=null;scope=null;publication=null;saved.clear();res.writeHead(303,{Location:`/app/admin/announcement-attachment-policies?policyId=${id(1)}`});return res.end();}
        if(url.pathname==='/app/admin/announcement-attachment-policies'){
            const nav=scenarios.map(n=>`<a href="/qa-scenario?name=${n}">합성 QA ${n}</a>`).join(' | ');
            const page=html.replace(/(<body[^>]*>)/,`$1<nav aria-label="합성 QA 시나리오">${nav}</nav>`).replace('data-is-admin="true"',`data-is-admin="${scenario!=='readonly'}"`);
            res.writeHead(200,{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store'});return res.end(page);
        }
        if(assets.has(url.pathname)){const body=await readFile(path.join(repo,'src/main/resources/static',url.pathname));res.writeHead(200,{'Content-Type':url.pathname.endsWith('.css')?'text/css':url.pathname.endsWith('.svg')?'image/svg+xml':'text/javascript'});return res.end(body);}
        if(req.method==='GET'){
            if(url.pathname===rules)return json(res,200,paged([{releaseId:id(2),releaseCode:'합성 키워드 규칙',versionNo:1,releaseStatusCode:'ACTIVE'}],url));
            if(url.pathname===base)return json(res,200,paged([summary()],url));
            if(url.pathname===`${base}/${currentId}`)return json(res,200,details());
            if(url.pathname.endsWith('/publication-impact'))return scenario==='partial-impact'?json(res,503,{},'합성 영향 조회 실패'):json(res,200,impact());
            if(url.pathname.endsWith('/publication-scopes'))return json(res,200,paged(scope?[scope]:[],url));
            if(scope&&url.pathname.endsWith('/publication-scopes/'+id(6)))return json(res,200,scopeDetails());
            if(scope&&url.pathname.endsWith('/publication-scopes/'+id(6)+'/items'))return json(res,200,paged([{entityTypeCode:'POLICY',entityId:currentId,stateHash:hash}],url));
            if(url.pathname.endsWith('/publication'))return publication?json(res,200,publication):json(res,404,{},'합성 영수증이 없습니다.');
            if(url.pathname.endsWith('/validation-runs'))return json(res,200,paged([run()],url));
            if(url.pathname.endsWith('/validation-runs/'+id(3)))return json(res,200,run());
        }
        if(['POST','PUT'].includes(req.method)&&url.pathname.startsWith(base)){
            if(scenario==='readonly')return json(res,403,{},'합성 조회 전용 역할입니다.');
            const chunks=[];let size=0;for await(const chunk of req){size+=chunk.length;if(size>8192)throw new Error();chunks.push(chunk);}const data=JSON.parse(Buffer.concat(chunks).toString('utf8'));
            const key=req.headers['idempotency-key'];if(key&&saved.has(key))return json(res,200,saved.get(key));
            if(scenario==='conflict')return json(res,409,{},'다른 관리자가 수정했습니다. 입력을 보존하고 최신 상태를 확인하세요.');
            let result;
            if(req.method==='PUT'&&url.pathname===`${base}/${currentId}`){if(data.expectedVersion!==version)return json(res,409,{},'정책 버전이 바뀌었습니다.');version++;bytes=data.maximumSourceBytes;mode=data.modeCode;result=details();}
            else if(req.method==='POST'&&(url.pathname===base||url.pathname.endsWith('/revisions'))){copied=url.pathname.endsWith('/revisions')?currentId:null;currentId=id(5);version=0;if(!copied){bytes=data.maximumSourceBytes;mode=data.modeCode;}result=details();}
            else if(req.method==='POST'&&url.pathname.endsWith('/validation-runs')){qa={...run(),policyVersion:version,statusCode:'PENDING',rowVersion:0,completedAt:null,errorCode:null};result=qa;}
            else if(req.method==='PUT'&&url.pathname.endsWith('/cancellation')&&qa){if(data.expectedVersion!==qa.rowVersion)return json(res,409,{},'QA 버전이 바뀌었습니다.');qa={...qa,statusCode:'CANCELLED',rowVersion:qa.rowVersion+1,completedAt:now};result=qa;}
            else if(req.method==='POST'&&url.pathname.endsWith('/publication-scopes')){if(data.expectedVersion!==version||publication)return json(res,409,{},'현재 초안을 확인하세요.');
                const start=Date.now()-(scenario==='expired-scope'?601000:0);scope={scopeId:id(6),policyId:currentId,policyVersion:version,ruleReleaseId:id(2),ruleVersion:0,modeCode:mode,qaRunId:id(3),qaSnapshotHash:hash,itemCount:1,scopeHash:'b'.repeat(64),createdAt:new Date(start).toISOString(),expiresAt:new Date(start+600000).toISOString()};result=scopeDetails();}
            else if(req.method==='POST'&&url.pathname.endsWith('/publication')){if(!ready()||!scope||!scopeDetails().isScopeCurrent||publication||data.scopeId!==scope.scopeId||data.scopeHash!==scope.scopeHash||data.expectedVersion!==version||!['acknowledgeNewCollectionBehavior','acknowledgeExistingJobsUnchanged','acknowledgeNoBackfill'].every(k=>data[k]===true))return json(res,409,{},'합성 게시 조건·개별 확인이 부족합니다.');
                version++;publication={publication:{publicationId:id(7),policyId:currentId,publishedPolicyVersion:version,policyHash:hash,previousPolicyId:null,previousPolicyVersion:null,scopeId:scope.scopeId,scopeHash:scope.scopeHash,qaRunId:id(3),modeCode:mode,publishedAt:new Date().toISOString()},existingDataApplied:false,workerEnabledByRequest:false,currentHttpRequests:0};result=publication;}
            else return json(res,404,{},'허용하지 않은 합성 작업입니다.');
            writes++;if(key)saved.set(key,result);if((scenario==='lost-update'||scenario==='lost-publication'&&url.pathname.endsWith('/publication'))&&!lost){lost=true;return json(res,503,{},'합성 응답 유실');}return json(res,200,result);
        }
        return json(res,404,{},'합성 QA 전용 경로입니다.');
    }catch{json(res,400,{},'합성 QA 입력 형식을 확인하세요.');}
});
server.listen(0,'127.0.0.1',()=>process.stdout.write(`SYNTHETIC_POLICY_QA http://127.0.0.1:${server.address().port}/app/admin/announcement-attachment-policies?policyId=${id(1)}\n`));
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>server.close(()=>process.exit(0)));
