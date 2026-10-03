// 실제 SSR/정적 자산 + 메모리 API. 운영 인증·DB·외부 수집에는 연결하지 않는다.
import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const B=require('../../src/main/resources/static/js/saneb-attachment-batch-core.js');
const repo=fileURLToPath(new URL('../../',import.meta.url));
const html=await readFile(path.join(repo,'build/attachment-batch-ui-qa/index.html'),'utf8');
if(!html.includes('합성 QA 전용 화면'))throw new Error('명시적으로 내보낸 합성 SSR이 필요합니다.');
const id=n=>`00000000-0000-4000-8000-${String(n).padStart(12,'0')}`,hash='a'.repeat(64);
const base=B.base,linked=B.linkedBase,route='/app/admin/announcement-attachment-batches';
let batch=null,scope=null,writes=0,readOnly=false;
const receipts=new Map();
const assets=new Set(['/css/saneb-dashboard.css','/css/saneb-announcement-attachment-review.css',
    '/js/saneb-attachment-review-core.js','/js/saneb-attachment-batch-core.js',
    '/js/saneb-attachment-batches.js','/js/saneb-layout.js','/images/saneb-logo-mark.svg']);
const send=(res,status,data,message='')=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify({success:status<400,data,message}));};
const page=(items,u)=>{const p=Number(u.searchParams.get('page')||1),size=Number(u.searchParams.get('size')||10);return {items:items.slice((p-1)*size,p*size),page:p,size,totalCount:items.length,totalPages:Math.ceil(items.length/size)};};
const server=http.createServer(async(req,res)=>{
    try{
        const u=new URL(req.url,'http://127.0.0.1');
        // Loopback only, and no cross-origin mutations from another browser tab/site.
        if(req.headers.host!==`127.0.0.1:${server.address().port}`)return send(res,403,null,'합성 QA 호스트만 허용합니다.');
        if(req.headers.origin && req.headers.origin!==`http://${req.headers.host}`)return send(res,403,null,'다른 출처의 요청은 허용하지 않습니다.');
        if(req.method==='GET'){
            if(u.pathname==='/qa-state')return send(res,200,{writes,status:batch?.statusCode||null,productionWrites:0,externalRequests:0});
            if(u.pathname===route){readOnly=u.searchParams.get('role')==='readonly';res.writeHead(200,{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store','Content-Security-Policy':"default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; script-src 'self'"});return res.end(html.replace('data-is-admin="true"',`data-is-admin="${!readOnly}"`));}
            if(assets.has(u.pathname)){const body=await readFile(path.join(repo,'src/main/resources/static',u.pathname));res.writeHead(200,{'Content-Type':u.pathname.endsWith('.css')?'text/css':u.pathname.endsWith('.svg')?'image/svg+xml':'text/javascript'});return res.end(body);}
            if(u.pathname==='/api/v2/admin/announcement-attachment-policies')return send(res,200,page([{policyId:id(2),policyCode:'합성 수집 전용 정책',versionNo:1,policyStatusCode:'ACTIVE',modeCode:'COLLECT_ONLY',ruleReleaseId:id(3),ruleReleaseStatusCode:'ACTIVE'}],u));
            if(u.pathname===base)return send(res,200,page(batch?[batch]:[],u));
            if(batch && u.pathname===`${base}/${batch.batchId}`)return send(res,200,batch);
            if(batch && u.pathname===`${base}/${batch.batchId}/items`)return send(res,200,page(scope.sourceIds.map((sourceId,i)=>({sourceId,jobId:id(100+i),statusCode:'PENDING'})),u));
        }
        if(['POST','PUT'].includes(req.method) && u.pathname.startsWith(linked)){
            let length=0;const chunks=[];for await(const c of req){length+=c.length;if(length>65536)return send(res,413,null,'합성 요청은 64KiB 이하여야 합니다.');chunks.push(c);}
            const data=JSON.parse(Buffer.concat(chunks).toString('utf8'));
            if(req.method==='POST' && u.pathname===`${linked}/scope-preview`){const s=B.linkedScope(data);return send(res,200,{policyId:s.policyId,scopeHash:hash,requestedCount:s.sourceIds.length,maximumDownloadBytes:s.maximumSourceBytes*s.sourceIds.length,maximumHttpRequests:132*s.sourceIds.length,canReserve:true,candidates:s.sourceIds.map(sourceId=>({sourceId,readinessCode:'READY',connectionSnapshotHash:hash}))});}
            if(readOnly)return send(res,403,null,'합성 조회 전용 역할입니다.');
            const key=req.headers['idempotency-key'];
            if(key && receipts.has(key))return send(res,200,receipts.get(key));
            if(req.method==='POST' && u.pathname===linked){
                const s=B.linkedScope(data.scope);
                if(!B.uuid(key)||data.expectedScopeHash!==hash||data.evidenceOnlyAcknowledged!==true||!data.reason?.trim())return send(res,409,null,'합성 예약의 지문·사유·명시적 확인을 확인하세요.');
                scope=s;batch={batchId:id(1),policyId:s.policyId,statusCode:'SCOPE_READY',scopeHash:hash,rowVersion:0,itemCount:s.sourceIds.length,remainingItemCount:s.sourceIds.length,deletedItemCount:0,jobCounts:{PENDING:s.sourceIds.length},createdAt:new Date().toISOString(),frozenScope:{schemaVersion:1,purposeCode:'LINKED_EVIDENCE_ONLY',maximumSourceBytes:s.maximumSourceBytes,maximumDownloadBytes:s.maximumSourceBytes*s.sourceIds.length,maximumHttpRequests:132*s.sourceIds.length}};
                writes++;receipts.set(key,batch);return send(res,200,batch);
            }
            const transitions={collection:[['SCOPE_READY'],'COLLECTION_PENDING'],'collection-pause':[['COLLECTION_PENDING','COLLECTING'],'COLLECTION_PAUSED'],'collection-resume':[['COLLECTION_PAUSED'],'COLLECTING']};
            const action=u.pathname.split('/').at(-1),t=transitions[action];
            if(req.method==='PUT'&&batch&&t&&u.pathname===`${linked}/${batch.batchId}/${action}`&&t[0].includes(batch.statusCode)&&data.expectedVersion===batch.rowVersion){batch={...batch,statusCode:t[1],rowVersion:batch.rowVersion+1};writes++;return send(res,200,batch);}
            return send(res,409,null,'합성 작업의 현재 버전과 단계를 확인하세요.');
        }
        return send(res,404,null,'이 경로는 합성 QA에서 지원하지 않습니다.');
    }catch{return send(res,400,null,'합성 QA 입력 형식을 확인하세요.');}
});
server.listen(0,'127.0.0.1',()=>console.log(JSON.stringify({url:`http://127.0.0.1:${server.address().port}${route}`,pid:process.pid,syntheticOnly:true})));
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>server.close(()=>process.exit(0)));
