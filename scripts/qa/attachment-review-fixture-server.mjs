// 로컬 UI 회귀 전용. synthetic SSR/API만 제공하며 DB·인증·외부 사이트에 연결하지 않는다.
import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
const repo = fileURLToPath(new URL('../../', import.meta.url));
const html = await readFile(path.join(repo, 'build/attachment-ui-qa/index.html'), 'utf8');
const sourceId = html.match(/data-source-id="([0-9a-f-]{36})"/i)?.[1];
if (!sourceId) throw new Error('명시적 SSR QA 산출물이 필요합니다.');
const id = n => `22222222-2222-4222-8222-${String(n).padStart(12, '0')}`;
const hash = 'a'.repeat(64), now = '2026-09-11T04:00:00Z';
const scenarios = ['normal', 'partial', 'preview', 'conflict', 'unknown-result', 'readonly', 'not-found', 'retry-files', 'policy-off'];
let scenario = 'normal', version = 7, confirmation = null, link = null, handled = false, confirms = 0, drafts = 0;
const requests = new Map();
let job = null, jobs = 0;
const reset = name => { scenario = name; version = 7; confirmation = null; link = null; handled = false; confirms = 0; drafts = 0; requests.clear(); job = null; jobs = 0; };
const currentVersion = () => ({expectedBaseDecisionId: id(1), expectedAttachmentDecisionId: id(2), expectedSourceVersion: 4, expectedAttachmentVersion: version, expectedSetHash: hash});
const classification = () => ({decisionId: id(2), semanticStatusCode: scenario === 'partial' ? 'REVIEW_REQUIRED' : 'ACCEPTED', reasonCode: scenario === 'partial' ? 'OCR_REQUIRED' : 'TARGET_SUPPORT_MATCH',
    setId: id(3), setHash: hash, targetCategoryCodes: ['BUSINESS'], supportTypeCodes: ['POLICY_FINANCE']});
const summary = () => ({sourceId, publicCode: 'SRC-SYNTHETIC-QA', providerCode: 'BIZINFO',
    title: '합성 QA 공고 <img src=x onerror=alert(1)>', sourceVersion: 4, attachmentVersion: version,
    baseClassification: {...classification(), decisionId: id(1), setId: null}, effectiveClassification: scenario === 'preview' ? {...classification(), decisionId: id(1)} : classification(),
    previewClassification: scenario === 'preview' ? classification() : null, isAttachmentReviewRequired: scenario !== 'preview',
    confirmationStatusCode: confirmation ? 'CURRENT' : 'NONE',
    attachmentSummary: {jobId: job?.jobId, jobStatusCode: job ? job.jobStatusCode : scenario === 'partial' || scenario === 'retry-files' ? 'PARTIAL_FAILED' : 'SUCCEEDED', isStale: !!job,
        discoveryStatusCode: 'FOUND', isDiscoveryComplete: true, totalCount: 2, processedCount: 2}});
const file = n => ({fileId: id(10 + n), setId: id(3), displayName: `QA-${n}.pdf <b>그대로 표시</b>`, detectedTypeCode: 'PDF', documentRoleCode: n === 1 ? 'NOTICE' : 'FORM',
    downloadStatusCode: scenario === 'retry-files' && n === 2 ? 'FAILED' : 'SUCCEEDED', qualityCode: scenario === 'partial' && n === 2 ? 'OCR_REQUIRED' : 'COMPLETE_TEXT', characterCount: 30,
    extractionId: id(20 + n), extractedAt: now});
const pageOf = (items, url) => { const page = Number(url.searchParams.get('page') || 1), size = Number(url.searchParams.get('size') || 10);
    return {items: items.slice((page - 1) * size, page * size), page, size, totalCount: items.length, totalPages: Math.ceil(items.length / size)}; };
const json = (res, status, data, message = '') => { res.writeHead(status, {'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store'}); res.end(JSON.stringify({success: status < 400, data, message})); };
const server = http.createServer(async (req, res) => {
    try {
        const url = new URL(req.url, 'http://localhost');
        if (url.pathname === '/qa-state') return json(res, 200, {scenario, confirms, drafts, jobs, version});
        if (url.pathname === '/qa-scenario' && scenarios.includes(url.searchParams.get('name'))) {
            reset(url.searchParams.get('name')); res.writeHead(303, {Location: '/', 'Cache-Control': 'no-store'}); return res.end();
        }
        if (url.pathname === '/') {
            const links = scenarios.map(name => `<a href="/qa-scenario?name=${name}">QA ${name}</a>`).join(' | ');
            const body = html.replace(/(<body[^>]*>)/, `$1<nav aria-label="합성 QA 시나리오">${links}</nav>`)
                .replace('data-can-manage="true"', `data-can-manage="${scenario !== 'readonly'}"`)
                .replace('data-can-rollback="true"', `data-can-rollback="${scenario !== 'readonly'}"`);
            res.writeHead(200, {'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store'}); return res.end(body);
        }
        const assets = new Set(['/css/saneb-dashboard.css', '/css/saneb-announcement-attachment-review.css', '/js/saneb-attachment-review-core.js',
            '/js/saneb-announcement-attachment-review.js', '/js/saneb-attachment-operations.js', '/js/saneb-attachment-recovery.js', '/js/saneb-layout.js', '/images/saneb-logo-mark.svg']);
        if (assets.has(url.pathname)) {
            const body = await readFile(path.join(repo, 'src/main/resources/static', url.pathname));
            res.writeHead(200, {'Content-Type': url.pathname.endsWith('.css') ? 'text/css' : url.pathname.endsWith('.svg') ? 'image/svg+xml' : 'text/javascript', 'Cache-Control': 'no-store'}); return res.end(body);
        }
        const prefix = `/api/v2/admin/announcement-sources/${sourceId}`;
        if (!url.pathname.startsWith(prefix)) return json(res, 404, {}, '합성 QA 전용 경로입니다.');
        if (scenario === 'not-found') return json(res, 404, {}, '제목 제외 또는 조회할 수 없는 원문입니다.');
        const suffix = url.pathname.slice(prefix.length);
        if (req.method === 'POST' || req.method === 'PUT') {
            const chunks = []; let bytes = 0;
            for await (const chunk of req) { bytes += chunk.length; if (bytes > 16000) throw new Error('limit'); chunks.push(chunk); }
            const raw = Buffer.concat(chunks).toString(), data = JSON.parse(raw);
            if (scenario === 'readonly') return json(res, 403, {}, '조회 전용입니다.');
            const key = req.headers['idempotency-key'] || raw;
            if (requests.has(key)) return json(res, 200, requests.get(key));
            if (scenario === 'conflict' && !handled) { handled = true; version++; return json(res, 409, {}, '첨부 버전이 변경됐습니다. 최신 기준을 다시 조회하세요.'); }
            if (data.version.expectedAttachmentVersion !== version) return json(res, 409, {}, '현재 버전과 다릅니다.');
            let result;
            if (['/attachment-jobs/collection', '/attachment-jobs', '/attachment-roles'].includes(suffix)) {
                if (job || link) return json(res, 409, {}, '기존 작업 또는 연결된 공고를 먼저 확인하세요.');
                version++; jobs++; confirmation = null;
                job = {sourceId, jobId: id(50), setId: id(3), operationCode: suffix === '/attachment-roles' ? 'ROLE_CHANGE' : suffix === '/attachment-jobs' ? 'RETRY_FILES' : 'COLLECT',
                    jobStatusCode: 'PENDING', sourceVersionAtReservation: 4, attachmentVersionAtReservation: version};
                result = job;
            } else if (suffix === '/attachment-classification/confirmations') {
                version++; confirms++;
                result = {sourceId, confirmationId: id(30), evaluationId: id(2), setHash: hash, sourceVersion: 4, attachmentVersion: version,
                    reviewMethodCode: data.reviewMethodCode, isCurrent: true, confirmedAt: now};
                confirmation = {confirmation: result, targetCategoryCodes: data.targetCategoryCodes, supportTypeCodes: data.supportTypeCodes};
            } else if (suffix === '/attachment-classification/announcements' && confirmation) {
                version++; drafts++; result = {sourceId, announcementId: id(40), announcementCode: 'ANN-SYNTHETIC-DRAFT'};
                link = {announcementId: result.announcementId, announcementCode: result.announcementCode};
            } else return json(res, 400, {}, '확인이 필요합니다.');
            requests.set(key, result);
            if (scenario === 'unknown-result' && !handled) { handled = true; return json(res, 503, {}, '합성 응답 유실'); }
            return json(res, job && result === job ? 202 : 200, result);
        }
        let data;
        if (!suffix) data = {source: summary(), content: {sourceUrl: 'https://example.com/', bodyText: '합성 본문입니다. 소상공인 자금 지원. <script>실행되지 않아야 함</script>'}};
        else if (suffix === '/attachment-recovery-jobs') data = pageOf([], url); // 기존 모형은 일반 원복/실제 DB 검증을 대신하지 않는다.
        else if (suffix === '/attachment-collection-context') {
            if (scenario === 'policy-off' || job || link) return json(res, 409, {}, '새 수집 정책 OFF 또는 기존 작업·공고 연결로 외부 수집할 수 없습니다.');
            const {expectedSetHash, ...v} = currentVersion();
            data = {version: v, policyId: id(4), policyHash: hash, executionHash: hash, modeCode: scenario === 'preview' ? 'COLLECT_ONLY' : 'ENFORCE',
                maximumDownloadBytes: 1048576, maximumFileCount: 10, maximumAttempts: 3, maximumHttpRequests: 132,
                isAttachmentReviewRequired: scenario !== 'preview', effectCode: scenario === 'preview' ? 'COLLECT_PREVIEW_ONLY' : 'PRESERVE_ENFORCE_AND_STALE_CONFIRMATION'};
        } else if (suffix === `/attachment-jobs/${id(50)}` && job) data = job;
        else if (suffix === '/attachment-classification/review-context') {
            if (job) return json(res, 409, {}, '진행 중인 첨부 작업의 결과 확인 후 재검수하세요.');
            if (scenario === 'preview') return json(res, 409, {}, '첨부 검수 정책이 적용되지 않은 미리보기는 확정할 수 없습니다.');
            data = {sourceId, version: currentVersion(), decisionStatusCode: classification().semanticStatusCode, reasonCode: classification().reasonCode,
                manualSourceCheckRequired: scenario === 'partial', requiredAcknowledgementCodes: scenario === 'partial' ? ['OCR_REQUIRED'] : [],
                confirmedClassification: link ? null : confirmation, linkedAnnouncement: link};
        } else if (suffix === '/attachment-sets') data = pageOf([{setId: id(3), createdAt: now, setStatusCode: 'SEALED', discoveryStatusCode: 'FOUND',
            discoveryComplete: true, processedCount: 2, discoveredCount: 2, warningCodes: [], manifestHash: hash}], url);
        else if (suffix === `/attachment-sets/${id(3)}/files`) data = pageOf([file(1), file(2)], url);
        else if (/\/attachment-extractions\/[^/]+\/blocks$/.test(suffix)) data = pageOf([{blockIndex: 0, startOffset: 0, endOffset: 14, evidenceScopeId: 'qa', scopeReliable: true,
            locator: '합성 문단 1', text: '😀지원금 <b>원문</b>', textStartOffset: 0, textEndOffset: 14, hasMoreText: false}], url);
        else if (suffix === '/attachment-classification/history') data = pageOf([{...classification(), evaluationId: id(2), usageCode: scenario === 'preview' ? 'CURRENT_PREVIEW' : 'CURRENT_EFFECTIVE',
            policyId: id(4), ruleReleaseId: id(5), inputHash: hash, evaluatedAt: now, warningCodes: []}], url);
        else if (suffix === `/attachment-classification/${id(2)}/matches`) data = pageOf([{fileId: id(11), extractionId: id(21), termText: '지원금', blockIndex: 0, startOffset: 1, endOffset: 4,
            groupCode: 'SUPPORT', ruleCode: 'QA-1', appliedActionCode: 'TAG'}], url);
        else return json(res, 404, {}, '합성 QA 미지원 경로입니다.');
        return json(res, 200, data);
    } catch { if (!res.headersSent) json(res, 500, {}, '합성 QA 처리 실패'); else res.end(); }
});
server.listen(0, '127.0.0.1', () => process.stdout.write(`QA_FIXTURE_URL=http://127.0.0.1:${server.address().port}/ PID=${process.pid}\n`));
process.on('SIGINT', () => server.close(() => process.exit(0)));
process.on('SIGTERM', () => server.close(() => process.exit(0)));
