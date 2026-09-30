import {request} from 'node:https';
import {createHash} from 'node:crypto';
import {mkdirSync, writeFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

// V61의 공식 고시공고 경로와 2026-10-01 공식 iframe 조사에 한정한다.
export const endpoints = Object.freeze({
  menu: 'https://www.ulsannamgu.go.kr/cop/bbs/selectSaeolGosiList.do',
  form: 'https://eminwon.ulsannamgu.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04&list_gubun=Y',
  list: 'https://eminwon.ulsannamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do'
});
export const limits = Object.freeze({requests: 3, responseBytes: 2 * 1024 * 1024, requestMs: 15000});
const output = 'build/reports/attachment-ulsan-namgu-survey/result.json';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const knownErrors = new Set(['ECONNRESET', 'ECONNREFUSED', 'ENOTFOUND', 'ETIMEDOUT',
  'CERT_HAS_EXPIRED', 'UNABLE_TO_VERIFY_LEAF_SIGNATURE', 'UNABLE_TO_GET_ISSUER_CERT_LOCALLY',
  'ERR_TLS_CERT_ALTNAME_INVALID', 'RESPONSE_TOO_LARGE', 'REQUEST_TIMEOUT']);

export function summarizePage(response) {
  const bytes = response.bytes;
  if (!Buffer.isBuffer(bytes) || bytes.length > limits.responseBytes)
    throw Object.assign(new Error('RESPONSE_TOO_LARGE'), {code: 'RESPONSE_TOO_LARGE'});
  const base = {httpStatus: response.status, bytes: bytes.length, sha256: hash(bytes)};
  if (response.status !== 200) return {...base, status: 'HTTP_NOT_OK'};
  const type = String(response.headers['content-type'] ?? '').toLowerCase();
  if (type.split(';')[0].trim() !== 'text/html') return {...base, status: 'NOT_HTML'};
  const head = bytes.subarray(0, 4096).toString('latin1');
  const charset = (type.match(/charset\s*=\s*["']?([\w-]+)/i)
    ?? head.match(/charset\s*=\s*["']?([\w-]+)/i))?.[1]?.toLowerCase();
  if (!['utf-8', 'utf8', 'euc-kr', 'ks_c_5601-1987', 'windows-949'].includes(charset))
    return {...base, status: 'CHARSET_UNCONFIRMED'};
  let html;
  try { html = new TextDecoder(['utf-8', 'utf8'].includes(charset) ? 'utf-8' : 'euc-kr', {fatal: true}).decode(bytes); }
  catch { return {...base, status: 'DECODE_FAILED'}; }
  // 200이나 함수 선언만으로 실제 목록 성공을 주장하지 않는다. 실제 호출의 숫자 ID만 남긴다.
  const ids = [...new Set([...html.matchAll(/(?:searchDetail|goDetail|viewDetail)\s*\(\s*['"]([0-9]{1,12})['"]/g)]
    .map(match => match[1]))].slice(0, 20);
  return {...base, status: 'HTML_OBSERVED', charset, detailIds: ids,
    hasOfficialFrame: html.includes('eminwon.ulsannamgu.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp'),
    hasListForm: /<form\b/i.test(html) && html.includes('selectListOfrNotAncmtHomepage')
      && html.includes('OfrAction.do') && html.includes('not_ancmt_se_code')};
}

export function selectSessionCookie(headers) {
  // 공개 조회의 동일 호스트 세션만 메모리에서 유지한다. 다른 쿠키·원시 헤더는 보고하지 않는다.
  const values = headers['set-cookie'] ?? [];
  if (!Array.isArray(values)) return '';
  const value = values.find(v => typeof v === 'string' && /^JSESSIONID=[A-Za-z0-9._-]{1,256}(?:;|$)/.test(v));
  return value ? value.split(';')[0] : '';
}

export function fixedListBody() {
  // 공개 종료 공고 첫 10행 조회. 비 ASCII 표시용 제목·검색어는 전송하지 않는다.
  return new URLSearchParams({pageIndex: '1', jndinm: 'OfrNotAncmtEJB', context: 'NTIS',
    method: 'selectListOfrNotAncmt', methodnm: 'selectListOfrNotAncmtHomepage',
    not_ancmt_mgt_no: '', homepage_pbs_yn: 'Y', subCheck: 'Y', ofr_pageSize: '10',
    not_ancmt_se_code: '01,04', title: '', cha_dep_code_nm: '', initValue: '', countYn: 'Y',
    list_gubun: 'Y', not_ancmt_sj: '', not_ancmt_cn: '', dept_nm: '', yyyy: '', not_ancmt_reg_no: ''}).toString();
}

export function fetchFixedPage(key, options = {}) {
  if (!Object.hasOwn(endpoints, key)) throw new Error('SURVEY_TARGET_INVALID');
  return new Promise((resolveRequest, reject) => {
    const headers = {'User-Agent': 'saneB-notice-collector/1.0', 'Accept': 'text/html', 'Accept-Encoding': 'identity'};
    if (key === 'list') {
      headers.Referer = endpoints.form;
      headers['Content-Type'] = 'application/x-www-form-urlencoded';
      if (options.cookie) headers.Cookie = options.cookie;
    }
    const req = request(endpoints[key], {method: key === 'list' ? 'POST' : 'GET', headers,
      rejectUnauthorized: true, agent: false}, res => {
      const parts = []; let size = 0;
      res.on('data', chunk => {
        size += chunk.length;
        if (size > limits.responseBytes) {
          const error = Object.assign(new Error('RESPONSE_TOO_LARGE'), {code: 'RESPONSE_TOO_LARGE'});
          req.destroy(error); res.destroy(error);
        } else parts.push(chunk);
      });
      res.once('error', reject);
      res.once('end', () => resolveRequest({status: res.statusCode, headers: res.headers, bytes: Buffer.concat(parts)}));
    });
    const timer = setTimeout(() => req.destroy(Object.assign(new Error('REQUEST_TIMEOUT'), {code: 'REQUEST_TIMEOUT'})), limits.requestMs);
    req.once('close', () => clearTimeout(timer));
    req.once('error', reject);
    req.end(key === 'list' ? fixedListBody() : undefined);
  });
}

export async function collectSurvey({fetchPage = fetchFixedPage, now = () => new Date().toISOString()} = {}) {
  const pages = []; let count = 0; let cookie = '';
  const observe = async key => {
    count++;
    try {
      const response = await fetchPage(key, key === 'list' ? {cookie} : {});
      const summary = summarizePage(response);
      if (key === 'form' && summary.hasListForm) cookie = selectSessionCookie(response.headers);
      pages.push({stage: key, ...summary}); return summary;
    } catch (error) {
      const summary = {stage: key, status: 'TRANSPORT_OR_RESPONSE_FAILED',
        errorCode: knownErrors.has(error?.code) ? error.code : 'REQUEST_FAILED'};
      pages.push(summary); return summary;
    }
  };
  await observe('menu');
  const form = await observe('form');
  if (form.hasListForm) await observe('list');
  cookie = '';
  return {scope: 'ULSAN_NAMGU_FIXED_PUBLIC_LIST_SURVEY', observedAt: now(),
    status: 'DIAGNOSTIC_ONLY_NOT_COLLECTION_QA', requestCount: count,
    maximumRequests: limits.requests, maximumResponseBytes: limits.requests * limits.responseBytes,
    pages, downloadedFileCount: 0, productionWriteCount: 0, rawFilesWritten: false,
    isAttachmentDiscoveryVerified: false, isOperatingE2eVerified: false};
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  if (process.platform !== 'linux' || process.argv.length !== 2 || process.env.SANEB_ULSAN_NAMGU_SURVEY !== 'true') {
    console.error('울산 남구 목록 진단은 승인된 Linux 격리 실행에서만 가능합니다.'); process.exitCode = 1;
  } else {
    try {
      const result = await collectSurvey();
      mkdirSync(dirname(output), {recursive: true});
      writeFileSync(output, JSON.stringify(result, null, 2) + '\n', {flag: 'wx', mode: 0o600});
      console.log(JSON.stringify(result));
    } catch { console.error('울산 남구 진단 결과를 보존하지 못했습니다.'); process.exitCode = 1; }
  }
}
