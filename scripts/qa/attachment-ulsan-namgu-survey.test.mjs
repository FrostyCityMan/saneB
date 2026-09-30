import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {spawnSync} from 'node:child_process';
import {endpoints, limits, summarizePage, selectSessionCookie, fixedListBody, collectSurvey, fetchFixedPage} from './attachment-ulsan-namgu-survey.mjs';

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
    env: {...process.env, SANEB_ULSAN_NAMGU_SURVEY: 'false'}, encoding: 'utf8', timeout: 10000});
  assert.equal(r.status, 1); assert.equal(r.stdout, '');
});

test('전송은 TLS 확인·총 시간·응답 크기를 제한하고 redirect를 따라가지 않는다', () => {
  const source = readFileSync('scripts/qa/attachment-ulsan-namgu-survey.mjs', 'utf8');
  assert(source.includes('rejectUnauthorized: true')); assert(source.includes('limits.requestMs'));
  assert(source.includes('size > limits.responseBytes')); assert(source.includes("'Accept-Encoding': 'identity'"));
  assert(!source.includes('headers.location')); assert(!source.includes('NODE_TLS_REJECT_UNAUTHORIZED'));
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
