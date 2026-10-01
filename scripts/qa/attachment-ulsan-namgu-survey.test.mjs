import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {spawnSync} from 'node:child_process';
import {endpoints, limits, summarizePage, selectSessionCookie, fixedListBody, collectSurvey, fetchFixedPage,
  selectSupportCandidates, summarizeDetailStructure, collectSupportSurvey, summarizeAttachmentPlacement,
  collectKnownDetailSurvey} from './attachment-ulsan-namgu-survey.mjs';

const response = (html, status = 200) => ({status, headers: {'content-type': 'text/html; charset=utf-8'}, bytes: Buffer.from(html)});
const form = '<form action="OfrAction.do"><input name="not_ancmt_se_code"><script>selectListOfrNotAncmtHomepage</script></form>';

test('고정 공식 경로·조회 POST만 사용하며 외부 입력 URL은 허용하지 않는다', () => {
  assert.equal(Object.keys(endpoints).length, 3);
  assert.equal(new URL(endpoints.list).host, 'eminwon.ulsannamgu.go.kr');
  assert.throws(() => fetchFixedPage('https://example.com/'));
  const body = new URLSearchParams(fixedListBody());
  assert.equal(body.get('methodnm'), 'selectListOfrNotAncmtHomepage');
  assert.equal(body.get('pageIndex'), '1'); assert.equal(body.get('ofr_pageSize'), '10');
  assert.equal(body.get('list_gubun'), 'Y'); assert.equal(body.get('not_ancmt_se_code'), '01,04');
  assert.equal(limits.requests, 3); assert.equal(limits.responseBytes, 2097152);
  assert.throws(() => fixedListBody('OTHER'));
  assert.throws(() => fetchFixedPage('list', {mode: 'OTHER'}));
  assert.throws(() => fetchFixedPage('detail', {id: '../123'}));
});

test('제목·본문·쿠키 원문 없이 호출 숫자 ID와 구조만 기록한다', () => {
  const r = summarizePage(response(form + '<a onclick="searchDetail(\'12345\')">민감한 제목</a><script>function searchDetail(id) {}</script>'));
  assert.equal(r.hasListForm, true); assert.deepEqual(r.detailIds, ['12345']);
  assert(!JSON.stringify(r).includes('민감한 제목'));
  assert.equal(summarizePage(response('<html>접근 거부</html>')).hasListForm, false);
  assert.equal(summarizePage(response(form, 302)).status, 'HTTP_NOT_OK');
  assert.equal(summarizePage({...response(form), headers: {'content-type': 'application/pdf'}}).status, 'NOT_HTML');
  assert.equal(summarizePage({...response(form), headers: {'content-type': 'text/html'}}).status, 'CHARSET_UNCONFIRMED');
  assert.throws(() => summarizePage({...response(''), bytes: Buffer.alloc(limits.responseBytes + 1)}));
  assert.equal(summarizePage({...response(form), bytes: Buffer.from([0xff])}).status, 'DECODE_FAILED');
  assert.equal(summarizePage({...response(form), headers: {'content-type': 'text/html-invalid; charset=utf-8'}}).status, 'NOT_HTML');
  assert.equal(summarizePage({...response(form), headers: {'content-type': 'text/html; charset=windows-949'}}).hasListForm, true);
});

test('공개 세션은 정해진 형식만 전달하고 보고서에서 배제한다', async () => {
  assert.equal(selectSessionCookie({'set-cookie': ['OTHER=hidden', 'JSESSIONID=test-session; Path=/emwp; HttpOnly']}), 'JSESSIONID=test-session');
  assert.equal(selectSessionCookie({'set-cookie': ['JSESSIONID=bad\r\nInjected=1']}), '');
  const calls = [];
  const result = await collectSurvey({fetchPage: async (key, options) => {
    calls.push({key, options});
    return {...response(key === 'form' ? form : '<html></html>'), headers: {
      'content-type': 'text/html; charset=utf-8', 'set-cookie': ['JSESSIONID=test-session; Path=/emwp']}};
  }});
  assert.equal(result.requestCount, 3); assert.equal(result.maximumResponseBytes, 6291456);
  assert.deepEqual(calls.map(c => c.key), ['menu', 'form', 'list']);
  assert.equal(calls[2].options.cookie, 'JSESSIONID=test-session');
  assert(!JSON.stringify(result).includes('test-session'));
  assert.equal(result.downloadedFileCount, 0); assert.equal(result.productionWriteCount, 0);
  assert.equal(result.isAttachmentDiscoveryVerified, false);
});

test('폼 확인 실패는 POST를 생략하고 예외 원문을 기록하지 않는다', async () => {
  const result = await collectSurvey({fetchPage: async key => {
    if (key === 'menu') return response('<html>not a form</html>');
    throw Object.assign(new Error('private error'), {code: 'ECONNRESET'});
  }});
  assert.equal(result.requestCount, 2); assert.equal(result.pages[1].errorCode, 'ECONNRESET');
  assert(!JSON.stringify(result).includes('private error'));
  assert.equal(result.rawFilesWritten, false);
});

test('명시 opt-in 없는 CLI는 외부 요청과 파일 쓰기를 수행하지 않는다', () => {
  const r = spawnSync(process.execPath, ['scripts/qa/attachment-ulsan-namgu-survey.mjs'], {
    env: {...process.env, SANEB_ULSAN_NAMGU_SURVEY: 'false', SANEB_ULSAN_NAMGU_SUPPORT_SURVEY: 'false', SANEB_ULSAN_NAMGU_KNOWN_DETAIL_SURVEY: 'false'}, encoding: 'utf8', timeout: 10000});
  assert.equal(r.status, 1); assert.equal(r.stdout, '');
});

const supportList = '<script>function searchDetail(id) { a="selectOfrNotAncmtRegst"; b="selectOfrNotAncmt"; }</script>'
  + '<table><tr><td><a href="javascript:searchDetail(\'12345\')">2026년 소상공인 경영안정자금 지원 공고</a></td></tr></table>';

test('후속 목록은 고정 소상공인 검색이며 후보 제목은 해시만 반환한다', () => {
  const body = new URLSearchParams(fixedListBody('SUPPORT'));
  assert.equal(body.get('not_ancmt_sj'), '소상공인');
  assert.equal(body.get('ofr_pageSize'), '10');
  const candidates = selectSupportCandidates(supportList);
  assert.equal(candidates.length, 1); assert.equal(candidates[0].id, '12345');
  assert.match(candidates[0].titleHash, /^[a-f0-9]{64}$/);
  assert(!JSON.stringify(candidates).includes('소상공인'));
  assert.deepEqual(selectSupportCandidates('<a href="javascript:searchDetail(\'12345\')">소상공인 지원</a>'), []);
});

test('후속 진단은 후보 1건의 상세 구조만 읽고 파일이나 다른 상세는 호출하지 않는다', async () => {
  const calls = [];
  const result = await collectSupportSurvey({fetchPage: async (key, options) => {
    calls.push({key, options});
    return response(key === 'form' ? form : key === 'list' ? supportList : '<form name="form1" method="post"><table class="public_view"><tr><th>제목</th><td>private title</td></tr></table></form>');
  }});
  assert.deepEqual(calls.map(c => c.key), ['form', 'list', 'detail']);
  assert.equal(calls[1].options.mode, 'SUPPORT'); assert.equal(calls[2].options.id, '12345');
  assert.equal(result.requestCount, 3); assert.equal(result.detail.noticeId, '12345');
  assert.deepEqual(result.detail.tableClasses, ['public_view']);
  assert(!JSON.stringify(result).includes('private title'));
  assert.equal(result.downloadedFileCount, 0); assert.equal(result.isAttachmentDiscoveryVerified, false);
});

test('후속 진단은 상세 method 미확인·후보 없음·문자셋 변경 때 후속 호출하지 않는다', async () => {
  for (const list of ['<html>nothing</html>', supportList.replaceAll('selectOfrNotAncmt', 'other')]) {
    const result = await collectSupportSurvey({fetchPage: async key => response(key === 'form' ? form : list)});
    assert.equal(result.requestCount, 2); assert.equal(result.detail, null);
  }
  const result = await collectSupportSurvey({fetchPage: async () => ({...response(form), headers: {'content-type': 'text/html; charset=euc-kr'}})});
  assert.equal(result.requestCount, 1);
});

test('상세 구조 metadata에는 입력값·파일명·외부 URL·쿠키를 포함하지 않는다', () => {
  const result = summarizeDetailStructure('<form name="nnn" method="post" action="/emwp/jsp/ofr/FileDownNew.jsp">'
    + '<input name="user_file_nm" value="private-file.hwp"><input name="file_path" value="secret-path"></form>'
    + '<form name="x" method="post" action="https://example.com/secret"><input name="ok" value="secret"></form>'
    + '<table class="public_view"><tr><td>첨부파일</td><td><a href="javascript:goDownLoad(\'opaque\')">private-file.hwp</a></td></tr></table>');
  assert.equal(result.forms[0].actionPath, '/emwp/jsp/ofr/FileDownNew.jsp');
  assert.equal(result.forms[1].actionPath, null); assert.deepEqual(result.functions, ['goDownLoad']);
  assert.deepEqual(result.labels, ['첨부파일']);
  assert(!/private-file|secret-path|example\.com|opaque/.test(JSON.stringify(result)));
});

test('전송은 TLS 확인·총 시간·응답 크기를 제한하고 redirect를 따라가지 않는다', () => {
  const source = readFileSync('scripts/qa/attachment-ulsan-namgu-survey.mjs', 'utf8');
  assert(source.includes('rejectUnauthorized: true')); assert(source.includes('limits.requestMs'));
  assert(source.includes('size > limits.responseBytes')); assert(source.includes("'Accept-Encoding': 'identity'"));
  assert(!source.includes('headers.location')); assert(!source.includes('NODE_TLS_REJECT_UNAUTHORIZED'));
});

test('기존 목록 ID 상세 1건만 조회하며 검색이나 첨부를 재요청하지 않는다', async () => {
  const calls=[];
  const result=await collectKnownDetailSurvey({fetchPage: async (key, options) => {
    calls.push({key, options});return response('<form name="form1" method="post"></form><table><tr><th>첨부파일</th><td><a href="javascript:goDownLoad(\'secret.hwp\')">private title</a></td></tr></table>');
  }});
  assert.deepEqual(calls,[{key:'detail',options:{id:'54578'}}]);
  assert.equal(result.maximumRequests,1);assert.equal(result.maximumResponseBytes,2097152);
  assert.equal(result.page.attachmentPlacement[0].formOpenCountBeforeLabel,1);
  assert.equal(result.page.attachmentPlacement[0].formCloseCountBeforeLabel,1);
  assert(result.page.attachmentPlacement[0].tokens.some(t=>t.knownDownloadCall));
  assert(!/secret|private title/.test(JSON.stringify(result)));
  assert.equal(result.downloadedFileCount,0);assert.equal(result.isAttachmentDiscoveryVerified,false);
});

test('상세 실패는 한 번의 오류로 보존하며 재시도하지 않는다', async () => {
  let calls=0;
  const result=await collectKnownDetailSurvey({fetchPage: async () => {calls++;throw Object.assign(new Error('private'),{code:'REQUEST_TIMEOUT'});}});
  assert.equal(calls,1);assert.equal(result.page.errorCode,'REQUEST_TIMEOUT');
  assert(!JSON.stringify(result).includes('private'));assert.equal(result.productionWriteCount,0);
});

test('고정 상세 workflow는 기존 두 검색 모드와 상호 배타적이고 최초 push만 실행한다', () => {
  const yaml=readFileSync('.github/workflows/attachment-contract-qa.yml','utf8');
  const block=yaml.split('      - name: 울산 남구 기존 목록 ID 상세 구조')[1]?.split('      - name: 제천 worker')[0];
  assert(block);assert(block.includes("github.event_name == 'push' && github.run_attempt == 1"));
  for(const name of ['list-survey','support-survey'])assert(block.includes("!contains(github.event.head_commit.message, '[ulsan-namgu-"+name+"-01]')"));
  assert(block.includes('timeout-minutes: 1'));assert(block.includes('SANEB_ULSAN_NAMGU_KNOWN_DETAIL_SURVEY'));
  assert(block.includes('path: build/reports/attachment-ulsan-namgu-survey/known-detail.json'));
  assert(!/secrets\.|continue-on-error|aws |deploy\.yml/.test(block));
});

test('첨부 구조는 script 문구·개인정보·원시 속성을 배제하고 크기를 제한한다', () => {
  const page='<script>const label="첨부파일";</script><!-- 첨부파일 -->'
    + '<form name="form1" method="post"><table><tr><td><font>첨부파일</font></td><td>'
    + '<a href="javascript:goDownLoad(\'secret\')" onclick="secret" data-token="secret">secret</a></td></tr></table></form>';
  const result=summarizeAttachmentPlacement(page);
  assert.equal(result.length,1);assert.equal(result[0].formCloseCountBeforeLabel,0);
  assert(result[0].tokens.some(t=>t.tag==='font'));
  assert(!JSON.stringify(result).includes('secret'));
  assert.equal(summarizeAttachmentPlacement(page.repeat(10)).length,4);
  assert(result.every(r=>r.tokens.length<=60));
});

test('workflow는 별도 최초 push 표식으로만 진단하며 metadata만 보관한다', () => {
  const yaml = readFileSync('.github/workflows/attachment-contract-qa.yml', 'utf8');
  const block = yaml.split('      - name: 울산 남구 고정 목록 진단기 검증')[1]?.split('      - name: 제천 worker')[0];
  assert(block);
  assert(block.includes("github.event_name == 'push' && github.run_attempt == 1"));
  assert(block.includes('[ulsan-namgu-list-survey-01]'));
  assert(block.includes('timeout-minutes: 2'));
  assert(block.includes('path: build/reports/attachment-ulsan-namgu-survey/result.json'));
  assert(!/secrets\.|continue-on-error|aws |deploy\.yml/.test(block));
});
