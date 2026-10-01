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

export function decodePage(response) {
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
  return {...base, status: 'HTML_OBSERVED', charset, html};
}

export function summarizePage(response) {
  const {html, ...base} = decodePage(response);
  if (html === undefined) return base;
  // 200이나 함수 선언만으로 실제 목록 성공을 주장하지 않는다. 실제 호출의 숫자 ID만 남긴다.
  const ids = [...new Set([...html.matchAll(/(?:searchDetail|goDetail|viewDetail)\s*\(\s*['"]([0-9]{1,12})['"]/g)]
    .map(match => match[1]))].slice(0, 20);
  return {...base, detailIds: ids,
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

export function fixedListBody(mode = 'LIST') {
  if (!['LIST', 'SUPPORT'].includes(mode)) throw new Error('SURVEY_MODE_INVALID');
  // 공개 종료 공고 첫 10행 조회. SUPPORT는 직전 관측으로 확인한 UTF-8 폼에서만 호출한다.
  return new URLSearchParams({pageIndex: '1', jndinm: 'OfrNotAncmtEJB', context: 'NTIS',
    method: 'selectListOfrNotAncmt', methodnm: 'selectListOfrNotAncmtHomepage',
    not_ancmt_mgt_no: '', homepage_pbs_yn: 'Y', subCheck: 'Y', ofr_pageSize: '10',
    not_ancmt_se_code: '01,04', title: '', cha_dep_code_nm: '', initValue: '', countYn: 'Y',
    list_gubun: 'Y', not_ancmt_sj: mode === 'SUPPORT' ? '소상공인' : '', not_ancmt_cn: '', dept_nm: '', yyyy: '', not_ancmt_reg_no: ''}).toString();
}

export function selectSupportCandidates(html) {
  // 진단용 후보 선택이며 운영 분류기가 아니다. 제목 원문은 반환·기록하지 않는다.
  const candidates = [];
  for (const row of html.matchAll(/<tr\b[^>]*>([\s\S]*?)<\/tr\s*>/gi)) {
    for (const anchor of row[1].matchAll(/<a\b([^>]*)>([\s\S]*?)<\/a\s*>/gi)) {
      const id = anchor[1].match(/(?:searchDetail|goDetail|viewDetail)\s*\(\s*['"]([0-9]{1,12})['"]/);
      const text = anchor[2].replace(/<[^>]*>/g, ' ').replace(/&nbsp;|&#160;/gi, ' ').replace(/\s+/g, ' ').trim();
      if (!id || !text.includes('소상공인') || !/(지원|자금|융자|보증|이차보전)/.test(text)) continue;
      candidates.push({id: id[1], titleHash: hash(text), titleLength: text.length});
    }
  }
  return candidates.slice(0, 10);
}

export function summarizeDetailStructure(html) {
  // HTML 원문·본문·제목·불투명 다운로드 인자를 보존하지 않고 구조만 관측한다.
  const safeName = value => /^[A-Za-z_][A-Za-z0-9_.-]{0,79}$/.test(value ?? '') ? value : null;
  const attr = (tag, name) => tag.match(new RegExp('(?:^|\\s)' + name + '\\s*=\\s*["\']([^"\']*)["\']', 'i'))?.[1];
  const forms = [...html.matchAll(/<form\b([^>]*)>([\s\S]*?)<\/form\s*>/gi)].slice(0, 10).map(match => {
    const action = attr(match[1], 'action');
    return {name: safeName(attr(match[1], 'name')), method: /^(get|post)$/i.test(attr(match[1], 'method') ?? '') ? attr(match[1], 'method').toUpperCase() : null,
      actionPath: /^\/emwp\/[A-Za-z0-9_\/-]+\.(jsp|do)$/.test(action ?? '') ? action : null,
      inputs: [...match[2].matchAll(/<input\b([^>]*)>/gi)].slice(0, 50).map(input => safeName(attr(input[1], 'name'))).filter(Boolean)};
  });
  const labels = [...html.matchAll(/<(td|th)\b[^>]*>\s*(제목|공고명|첨부파일|첨부 파일|내용|공고내용)\s*<\/\1\s*>/gi)].map(match => match[2]);
  const functions = [...new Set([...html.matchAll(/(?:function\s+|javascript:|onclick=["']\s*)([A-Za-z_][A-Za-z0-9_]{0,79})\s*\(/g)]
    .map(match => match[1]).filter(name => /detail|down|file/i.test(name)))].slice(0, 30);
  const tableClasses = [...new Set([...html.matchAll(/<table\b[^>]*\bclass=["']([A-Za-z0-9_ -]{1,80})["']/gi)].map(match => match[1]))].slice(0, 20);
  return {forms, labels, functions, tableClasses,
    extensionMentions: Object.fromEntries(['pdf', 'hwp', 'hwpx'].map(ext => [ext.toUpperCase(), (html.match(new RegExp('\\.' + ext + '(?![a-z])', 'gi')) ?? []).length]))};
}

export function summarizeAttachmentPlacement(html) {
  // 파서 등록용 구조 진단이다. script 안의 안내 문구를 실제 첨부 라벨로 오인하지 않는다.
  const page = html.replace(/<!--[\s\S]*?-->/g, '').replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1\s*>/gi, '');
  const labels = [...page.matchAll(/첨부\s*파일/g)].slice(0, 4);
  return labels.map(label => {
    const before = page.slice(0, label.index);
    const fragment = page.slice(Math.max(0, label.index - 600), label.index + 1800);
    const tokens = [...fragment.matchAll(/<\/?([A-Za-z][A-Za-z0-9]*)\b[^>]*>/g)].slice(0, 60).map(match => {
      const tag = match[1].toLowerCase();
      const attributes = Object.fromEntries([...match[0].matchAll(/\b(class|name|id|method)\s*=\s*["']([A-Za-z0-9_ -]{1,80})["']/gi)]
        .map(value => [value[1].toLowerCase(), value[2]]));
      return {tag, closing: match[0].startsWith('</'), attributes,
        ...(tag === 'a' ? {knownDownloadCall: /\bgoDownLoad\s*\(/.test(match[0])} : {})};
    });
    return {formOpenCountBeforeLabel: (before.match(/<form\b/gi) ?? []).length,
      formCloseCountBeforeLabel: (before.match(/<\/form\s*>/gi) ?? []).length, tokens};
  });
}

export async function collectKnownDetailSurvey({fetchPage = fetchFixedPage, now = () => new Date().toISOString()} = {}) {
  // run36782051228의 공식 목록에서 확보한 ID. 번호 탐색·검색 POST·다른 게시판 대체는 하지 않는다.
  const noticeId = '54578';
  let page;
  try {
    const response = await fetchPage('detail', {id: noticeId});
    const {html, ...metadata} = decodePage(response);
    page = {...metadata, ...(html === undefined ? {} : {
      structure: summarizeDetailStructure(html), attachmentPlacement: summarizeAttachmentPlacement(html)})};
  } catch (error) {
    page = {status: 'TRANSPORT_OR_RESPONSE_FAILED', errorCode: knownErrors.has(error?.code) ? error.code : 'REQUEST_FAILED'};
  }
  return {scope: 'ULSAN_NAMGU_KNOWN_LIST_ID_DETAIL_SURVEY', observedAt: now(), noticeId,
    sourceListRunId: 36782051228, status: 'DIAGNOSTIC_ONLY_NOT_COLLECTION_QA',
    requestCount: 1, maximumRequests: 1, maximumResponseBytes: limits.responseBytes,
    page, downloadedFileCount: 0, productionWriteCount: 0, rawFilesWritten: false,
    isAttachmentDiscoveryVerified: false, isOperatingE2eVerified: false};
}

export function fetchFixedPage(key, options = {}) {
  if (!Object.hasOwn(endpoints, key) && key !== 'detail') throw new Error('SURVEY_TARGET_INVALID');
  if (key === 'detail' && !/^[0-9]{1,12}$/.test(options.id ?? '')) throw new Error('SURVEY_DETAIL_ID_INVALID');
  const detailQuery = new URLSearchParams({context: 'NTIS', homepage_pbs_yn: 'Y', jndinm: 'OfrNotAncmtEJB',
    method: 'selectOfrNotAncmt', methodnm: 'selectOfrNotAncmtRegst', subCheck: 'Y', not_ancmt_mgt_no: options.id ?? ''});
  const destination = key === 'detail' ? endpoints.list + '?' + detailQuery : endpoints[key];
  const body = key === 'list' ? fixedListBody(options.mode) : undefined;
  return new Promise((resolveRequest, reject) => {
    const headers = {'User-Agent': 'saneB-notice-collector/1.0', 'Accept': 'text/html', 'Accept-Encoding': 'identity'};
    if (key === 'list' || key === 'detail') {
      headers.Referer = endpoints.form;
      headers['Content-Type'] = 'application/x-www-form-urlencoded';
      if (options.cookie) headers.Cookie = options.cookie;
    }
    const req = request(destination, {method: key === 'list' ? 'POST' : 'GET', headers,
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
    req.end(body);
  });
}

export async function collectSupportSurvey({fetchPage = fetchFixedPage, now = () => new Date().toISOString()} = {}) {
  const pages = []; let cookie = ''; let count = 0;
  const fetch = async (key, options = {}) => {
    count++;
    try {
      const response = await fetchPage(key, {...options, cookie});
      const decoded = decodePage(response);
      pages.push({stage: key, ...summarizePage(response)});
      const renewed = selectSessionCookie(response.headers);
      if (renewed) cookie = renewed;
      return decoded.html;
    } catch (error) {
      pages.push({stage: key, status: 'TRANSPORT_OR_RESPONSE_FAILED', errorCode: knownErrors.has(error?.code) ? error.code : 'REQUEST_FAILED'});
    }
  };
  const formHtml = await fetch('form');
  let candidates = [], detail = null;
  if (formHtml && pages[0].hasListForm && pages[0].charset === 'utf-8') {
    const listHtml = await fetch('list', {mode: 'SUPPORT'});
    if (listHtml) {
      candidates = selectSupportCandidates(listHtml);
      // 실제 목록 함수에 상세 method가 확인된 경우만 같은 공식 endpoint로 GET한다.
      if (candidates.length && /['"]selectOfrNotAncmtRegst['"]/.test(listHtml) && /['"]selectOfrNotAncmt['"]/.test(listHtml)) {
        const html = await fetch('detail', {id: candidates[0].id});
        if (html) detail = {noticeId: candidates[0].id, ...summarizeDetailStructure(html)};
      }
    }
  }
  cookie = '';
  return {scope: 'ULSAN_NAMGU_SUPPORT_STRUCTURE_SURVEY', observedAt: now(), status: 'DIAGNOSTIC_ONLY_NOT_COLLECTION_QA',
    requestCount: count, maximumRequests: 3, maximumResponseBytes: 3 * limits.responseBytes,
    pages, candidates, detail, downloadedFileCount: 0, productionWriteCount: 0, rawFilesWritten: false,
    isAttachmentDiscoveryVerified: false, isOperatingE2eVerified: false};
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
  const supportMode = process.env.SANEB_ULSAN_NAMGU_SUPPORT_SURVEY === 'true';
  const listMode = process.env.SANEB_ULSAN_NAMGU_SURVEY === 'true';
  const detailMode = process.env.SANEB_ULSAN_NAMGU_KNOWN_DETAIL_SURVEY === 'true';
  if (process.platform !== 'linux' || process.argv.length !== 2 || [supportMode, listMode, detailMode].filter(Boolean).length !== 1) {
    console.error('울산 남구 목록 진단은 승인된 Linux 격리 실행에서만 가능합니다.'); process.exitCode = 1;
  } else {
    try {
      const result = await (detailMode ? collectKnownDetailSurvey() : supportMode ? collectSupportSurvey() : collectSurvey());
      const destination = detailMode ? output.replace('result.json', 'known-detail.json') : supportMode ? output.replace('result.json', 'support-structure.json') : output;
      mkdirSync(dirname(destination), {recursive: true});
      writeFileSync(destination, JSON.stringify(result, null, 2) + '\n', {flag: 'wx', mode: 0o600});
      console.log(JSON.stringify(result));
    } catch { console.error('울산 남구 진단 결과를 보존하지 못했습니다.'); process.exitCode = 1; }
  }
}
