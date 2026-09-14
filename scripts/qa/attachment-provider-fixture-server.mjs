/* 실제 SSR + 합성 API/CSRF만 사용하는 loopback QA. 운영 DB/실파일/외부 요청은 사용하지 않는다. */
import http from 'node:http';import fs from 'node:fs';import path from 'node:path';
const root=path.resolve(import.meta.dirname,'../..'),html=fs.readFileSync(path.join(root,'build/attachment-provider-ui-qa/index.html'),'utf8');
if(!html.includes('합성 QA 전용 화면입니다. 운영 DB·인증·정책·수집원과 연결하지 않습니다.'))throw new Error('합성 SSR 표식이 없습니다.');
const id=n=>`00000000-0000-0000-0000-${String(n).padStart(12,'0')}`,hash='a'.repeat(64),route='/app/admin/announcement-attachment-provider-qa';
const api=`/api/v2/admin/announcement-attachment-policies/${id(1)}/provider-qa-runs`;
const assets=new Set(['/css/saneb-dashboard.css','/css/saneb-announcement-attachment-review.css','/js/saneb-layout.js','/js/saneb-csrf.js',
    '/js/saneb-attachment-policy-core.js','/js/saneb-attachment-provider-qa-core.js','/js/saneb-attachment-provider-qa.js','/images/saneb-logo-mark.svg']);
let scenario='normal',current=null,writes=0,reserveAttempts=0,cancelAttempts=0,csrfRejected=0;const saved=new Map();
const paged=(items,u)=>{const n=Number(u.searchParams.get('page')||1),size=Number(u.searchParams.get('size')||20);return{items:items.slice((n-1)*size,n*size),page:n,size,totalCount:items.length,totalPages:Math.ceil(items.length/size)};};
const segment={ordinal:1,caseCodes:['QA-ONE','QA-TWO'],maximumRequests:10,maximumBytes:100000,maximumSecondsIncludingMargin:500};
const newRun=()=>({runId:id(3),policyId:id(1),policyVersion:0,snapshotHash:hash,catalogHash:hash,planHash:hash,statusCode:'READY',rowVersion:1,
    expectedCaseCount:2,maximumRequests:10,maximumBytes:100000,requestReservations:0,reservedBytes:0,segmentNo:1,segmentCount:1,catalogCaseCount:15,executableCaseCount:2,
    isExpectationCoverageComplete:false,maximumSecondsIncludingMargin:500,isInputVersionsCurrent:true,isQaPassed:false,
    createdAt:new Date().toISOString(),expiresAt:new Date(Date.now()+86400000).toISOString(),completedAt:null});
const item=n=>({caseId:id(n+3),ordinal:n,caseCode:n===1?'QA-ONE':'QA-TWO',inputHash:hash,profileHash:hash,expectedFileCount:2,
    statusCode:current?.statusCode==='CANCELLED'?'CANCELLED':'PENDING',rowVersion:0,requestReservations:0,reservedBytes:0,startedAt:null,completedAt:current?.completedAt||null,
    errorCode:current?.statusCode==='CANCELLED'?'EXECUTION_STOPPED':null,evidenceHash:null});
const json=(res,status,data)=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify({success:status>=200&&status<300,data,message:'합성 QA 응답'}));};
const server=http.createServer(async(req,res)=>{try{const u=new URL(req.url,'http://127.0.0.1');
    if(req.method==='GET'&&u.pathname==='/qa-state')return json(res,200,{scenario,writes,reserveAttempts,cancelAttempts,csrfRejected,productionWrites:0,externalRequests:0,status:current?.statusCode||null});
    if(req.method==='GET'&&u.pathname==='/qa-scenario'){const name=u.searchParams.get('name');if(!['normal','readonly','off','unavailable','empty','lost-reserve','lost-cancel','cancel-pending','conflict'].includes(name))return json(res,400,{});
        scenario=name;current=['readonly','off','unavailable','lost-cancel','cancel-pending'].includes(name)?newRun():null;writes=0;reserveAttempts=0;cancelAttempts=0;csrfRejected=0;saved.clear();
        res.writeHead(303,{Location:`${route}?policyId=${id(1)}${current?'&runId='+id(3):''}`});return res.end();}
    if(req.method==='GET'&&u.pathname===route){res.writeHead(200,{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store','Set-Cookie':'XSRF-TOKEN=synthetic-csrf-only; SameSite=Strict; Path=/'});
        return res.end(scenario==='readonly'?html.replace('data-is-admin="true"','data-is-admin="false"'):html);}
    if(req.method==='GET'&&assets.has(u.pathname)){res.writeHead(200,{'Content-Type':u.pathname.endsWith('.css')?'text/css':u.pathname.endsWith('.js')?'text/javascript':'image/svg+xml'});return fs.createReadStream(path.join(root,'src/main/resources/static',u.pathname)).pipe(res);}
    if(req.method==='GET'&&u.pathname===api+'/execution-plan'){
        if(scenario==='unavailable')return json(res,409,{});const empty=scenario==='empty';return json(res,200,{policyId:id(1),policyVersion:0,snapshotHash:hash,catalogHash:hash,planHash:hash,
            targetCount:225,catalogCaseCount:15,executableCaseCount:empty?0:2,isExpectationCoverageComplete:false,isQaPassed:false,isReservationEnabled:!empty&&scenario!=='off',segments:paged(empty?[]:[segment],u)});}
    if(req.method==='GET'&&u.pathname===api)return json(res,200,paged(current?[current]:[],u));
    if(req.method==='GET'&&u.pathname===`${api}/${id(3)}`)return json(res,current?200:404,current||{});
    if(req.method==='GET'&&u.pathname===`${api}/${id(3)}/cases`)return json(res,current?200:404,current?paged([item(1),item(2)],u):{});
    if(['POST','PUT'].includes(req.method)&&u.pathname.startsWith(api)){
        if(req.headers['x-xsrf-token']!=='synthetic-csrf-only'){csrfRejected++;return json(res,403,{});}if(scenario==='readonly')return json(res,403,{});
        let text='';for await(const c of req){text+=c.toString('utf8');if(Buffer.byteLength(text)>8192)return json(res,413,{});}const body=JSON.parse(text);
        if(req.method==='POST'&&u.pathname===api){reserveAttempts++;const key=req.headers['idempotency-key'];if(!key||typeof key!=='string'||!/^[a-f0-9-]{36}$/.test(key))return json(res,400,{});
            if(saved.has(key)){if(saved.get(key)!==text)return json(res,409,{});if(scenario==='lost-reserve'&&reserveAttempts===2)return json(res,403,{});return json(res,202,current);}
            if(scenario==='conflict'||scenario==='off'||scenario==='empty')return json(res,409,{});
            if(body.expectedVersion!==0||body.expectedSnapshotHash!==hash||body.expectedCatalogHash!==hash||body.expectedPlanHash!==hash||body.segmentNo!==1
                ||body.expectedCaseCount!==2||body.maximumRequests!==10||body.maximumBytes!==100000||body.maximumSecondsIncludingMargin!==500
                ||!body.acknowledgeScope||!body.acknowledgeNetworkBudget||!body.acknowledgeIncompleteCoverage||!body.reason?.trim())return json(res,400,{});
            saved.set(key,text);current=newRun();writes++;if(scenario==='lost-reserve')return json(res,503,{});return json(res,202,current);}
        if(req.method==='PUT'&&u.pathname===`${api}/${id(3)}/cancellation`){cancelAttempts++;if(!current||body.expectedVersion!==current.rowVersion||!body.reason?.trim()||!['READY','RUNNING'].includes(current.statusCode))return json(res,409,{});
            current={...current,statusCode:scenario==='cancel-pending'?'CANCEL_REQUESTED':'CANCELLED',rowVersion:current.rowVersion+1,completedAt:scenario==='cancel-pending'?null:new Date().toISOString()};writes++;
            return json(res,scenario==='lost-cancel'?503:200,current);}
    }
    if(u.pathname==='/favicon.ico'){res.writeHead(204);return res.end();}return json(res,404,{});
}catch{json(res,400,{});}});
server.listen(0,'127.0.0.1',()=>process.stdout.write(`SYNTHETIC_PROVIDER_QA http://127.0.0.1:${server.address().port}${route}?policyId=${id(1)}\n`));
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>server.close(()=>process.exit(0)));
