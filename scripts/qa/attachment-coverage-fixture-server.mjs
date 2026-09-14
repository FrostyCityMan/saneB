/* 실제 SSR을 합성 응답으로 검증하는 loopback 전용 서버. 외부 HTTP/운영 DB/인증 연동 없음. */
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'../..');
const html=fs.readFileSync(path.join(root,'build/attachment-coverage-ui-qa/index.html'),'utf8');
if(!html.includes('합성 QA 전용 화면입니다. 운영 DB·인증·정책·수집원과 연결하지 않습니다.'))throw new Error('합성 SSR 표식이 없습니다.');
const id='00000000-0000-0000-0000-000000000001', hash='a'.repeat(64), route='/app/admin/announcement-attachment-provider-coverage';
const api=`/api/v2/admin/announcement-attachment-policies/${id}/provider-qa-runs/execution-plan/targets`, formats=['HWP','HWPX','PDF'];
const assets=new Set(['/css/saneb-dashboard.css','/css/saneb-announcement-attachment-review.css','/js/saneb-layout.js',
    '/js/saneb-attachment-policy-core.js','/js/saneb-attachment-provider-coverage.js','/images/saneb-logo-mark.svg']);
const scenarios=['normal','missing','partial','changed','invalid','unauthorized','forbidden','unavailable','offline','empty'];
let scenario='normal', reads=0, blockedWrites=0;
const target=n=>({targetKey:`LOCAL_GOV_NOTICE:LGS-${String(n).padStart(6,'0')}`,bindingStatusCode:'PROFILE_MISSING',referenceCount:0,
    executableCount:0,normalNoticeCount:0,requiredNormalNoticeCount:3,missingFormats:[],isExpectationCoverageComplete:false,
    formatApplicability:{statusCode:'EXPECTATIONS_UNKNOWN',expectedProvidedFormats:[],unobservedFormats:formats,normalMultiFileNoticeCount:0}});
function data(n){
    const total=scenario==='empty'?0:225, items=Array.from({length:Math.min(20,Math.max(0,total-(n-1)*20))},(_,i)=>target((n-1)*20+i));
    if(scenario!=='missing'&&items.length>2){
        const ready=items[1];Object.assign(ready,{bindingStatusCode:'SYSTEM_BINDING_MATCHED',referenceCount:3,executableCount:3,normalNoticeCount:3,isExpectationCoverageComplete:true,
            formatApplicability:{statusCode:'FIXED_SAMPLE_EXPECTATIONS',expectedProvidedFormats:['HWPX'],unobservedFormats:['HWP','PDF'],normalMultiFileNoticeCount:1}});
        Object.assign(items[2],{...structuredClone(ready),targetKey:items[2].targetKey,normalNoticeCount:2,missingFormats:['PDF'],isExpectationCoverageComplete:false,
            formatApplicability:{statusCode:'FIXED_SAMPLE_EXPECTATIONS',expectedProvidedFormats:['HWPX','PDF'],unobservedFormats:['HWP'],normalMultiFileNoticeCount:1}});
    }
    return {policyId:id,policyVersion:0,snapshotHash:hash,catalogHash:hash,planHash:scenario==='changed'&&n>1?'b'.repeat(64):hash,
        isExpectationCoverageComplete:false,isQaPassed:scenario==='invalid',formatCoverage:{modeCode:'FIXED_SAMPLE_FORMATS_V2',requiredFormats:formats,missingFormats:formats},
        targets:{page:n,size:20,totalCount:total,totalPages:Math.ceil(total/20),items}};
}
function json(res,status,data){res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify({success:status===200,data,message:'합성 QA 응답'}));}
const server=http.createServer((req,res)=>{
    const u=new URL(req.url,'http://127.0.0.1');
    if(req.method!=='GET'){blockedWrites++;return json(res,405,{});}
    if(u.pathname==='/qa-state')return json(res,200,{scenario,reads,blockedWrites,productionWrites:0,externalRequests:0});
    if(u.pathname==='/qa-scenario'){
        const next=u.searchParams.get('name');if(!scenarios.includes(next))return json(res,400,{});
        scenario=next;reads=0;res.writeHead(303,{Location:`${route}?policyId=${id}`});return res.end();
    }
    if(u.pathname===route){res.writeHead(200,{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store'});return res.end(html);}
    if(assets.has(u.pathname)){
        const file=path.join(root,'src/main/resources/static',u.pathname);if(!fs.existsSync(file))return json(res,404,{});
        res.writeHead(200,{'Content-Type':u.pathname.endsWith('.css')?'text/css':u.pathname.endsWith('.js')?'text/javascript':'image/svg+xml'});return fs.createReadStream(file).pipe(res);
    }
    if(u.pathname===api){
        reads++;const status={unauthorized:401,forbidden:403,unavailable:409}[scenario];if(status)return json(res,status,{});
        if(scenario==='offline')return req.socket.destroy();
        const n=Number(u.searchParams.get('page'));if(!Number.isSafeInteger(n)||n<1||u.searchParams.get('size')!=='20')return json(res,400,{});
        return json(res,200,data(n));
    }
    if(u.pathname==='/favicon.ico'){res.writeHead(204);return res.end();}
    return json(res,404,{});
});
server.listen(0,'127.0.0.1',()=>process.stdout.write(`SYNTHETIC_COVERAGE_QA http://127.0.0.1:${server.address().port}${route}?policyId=${id}\n`));
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>server.close(()=>process.exit(0)));
