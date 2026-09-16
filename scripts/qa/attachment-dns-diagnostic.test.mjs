import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { summarizeAddresses, summarizeProcess, summarizeDnsError, selectComparison, collectDiagnostic, targets } from './attachment-dns-diagnostic.mjs';

test('주소 원문은 남기지 않고 중복 제거한 동일 집합 지문을 비교한다', () => {
  const a = summarizeAddresses(['203.0.113.2', '2001:db8::1', '203.0.113.2']);
  const b = summarizeAddresses(['2001:db8::1', '203.0.113.2']);
  assert.deepEqual(a, b);
  assert.equal(a.addressCount, 2); assert.equal(a.ipv4Count, 1); assert.equal(a.ipv6Count, 1);
  assert.equal(selectComparison(a, b), 'RESOLVED_SAME_SET');
  assert(!JSON.stringify(a).includes('203.0.113'));
});
for (const value of [null, ['not-an-address'], Array(65).fill('203.0.113.2')]) {
  test('잘못된 주소 응답을 성공으로 만들지 않는다 ' + JSON.stringify(value)?.length, () => {
    assert.deepEqual(summarizeAddresses(value), { status: 'INVALID_RESPONSE' });
  });
}
test('OS와 JVM 응답을 각 형식으로 해석하고 예외 원문은 버린다', () => {
  const os = summarizeProcess('OS_GETENT', { status: 0, stdout: '203.0.113.2 STREAM name\n203.0.113.2 DGRAM name\n' });
  const java = summarizeProcess('JVM_INETADDRESS', { status: 0, stdout: 'RESOLVED\t203.0.113.2\n' });
  assert.deepEqual(os, java);
  assert.deepEqual(summarizeProcess('JVM_INETADDRESS', { status: 0, stdout: 'UNKNOWN_HOST' }), { status: 'UNKNOWN_HOST' });
  assert.deepEqual(summarizeProcess('OS_GETENT', { status: 2, stderr: 'private-data' }), { status: 'NOT_FOUND' });
  assert.deepEqual(summarizeProcess('OS_GETENT', { error: { code: 'ETIMEDOUT', message: 'private-data' } }), { status: 'PROCESS_TIMEOUT' });
  assert.deepEqual(summarizeProcess('JVM_INETADDRESS', { status: 1, stderr: 'private-data' }), { status: 'PROCESS_FAILED' });
  assert.deepEqual(summarizeProcess('JVM_INETADDRESS', { status: 0, stdout: 'private-data' }), { status: 'INVALID_RESPONSE' });
  assert.throws(() => summarizeProcess('OTHER', {}));
});
test('DNS 오류 코드와 부분 성공을 원인 단정 없이 구분한다', () => {
  assert.deepEqual(summarizeDnsError({ code: 'ESERVFAIL', message: 'private-data' }), { status: 'ESERVFAIL' });
  assert.deepEqual(summarizeDnsError({ code: 'private-data' }), { status: 'QUERY_FAILED' });
  const ok = summarizeAddresses(['203.0.113.2']), no = { status: 'UNKNOWN_HOST' };
  assert.equal(selectComparison(ok, no), 'ONLY_OS_RESOLVED');
  assert.equal(selectComparison(no, ok), 'ONLY_JVM_RESOLVED');
  assert.equal(selectComparison(no, no), 'NEITHER_RESOLVED');
  assert.equal(selectComparison(ok, summarizeAddresses(['203.0.113.3'])), 'RESOLVED_DIFFERENT_SET');
  assert.deepEqual(summarizeAddresses([]), { status: 'NO_ADDRESS' });
});
test('고정 두 호스트·8조회·상한과 무HTTP/DB 경계를 합성 실행으로 확인한다', async () => {
  const calls = [], queries = [];
  const result = await collectDiagnostic({ run: (command, args, options) => {
    calls.push({ command, args, options }); return { status: command === 'java' ? 0 : 2, stdout: 'UNKNOWN_HOST' };
  }, resolveRecords: async (host, type) => { queries.push([host, type]); return { status: 'ESERVFAIL' }; }, now: () => '2026-09-16T00:00:00Z' });
  assert.equal(calls.length, 4); assert.equal(queries.length, 4);
  assert.deepEqual(queries, targets.flatMap(t => [[t.host, 'A'], [t.host, 'AAAA']]));
  for (const call of calls) {
    assert.equal(call.options.timeout, 15000); assert.equal(call.options.killSignal, 'SIGKILL');
    assert.equal(call.options.maxBuffer, 8192); assert.deepEqual(Object.keys(call.options.env).sort(), ['LANG', 'PATH']);
    assert(!call.args.some(arg => arg.includes('preferIPv') || arg.includes('hosts.file')));
  }
  assert.equal(result.status, 'OBSERVED_NOT_COLLECTION_QA');
  for (const key of ['httpRequestCount', 'downloadedFileCount', 'productionWriteCount']) assert.equal(result[key], 0);
  for (const key of ['isCollectionQaPassed', 'isPolicyQaPassed', 'isOperatingStateVerified']) assert.equal(result[key], false);
});
test('CLI 명시 opt-in 없이는 조회하지 않는다', () => {
  const result = spawnSync(process.execPath, ['scripts/qa/attachment-dns-diagnostic.mjs'], {
    env: { ...process.env, SANEB_ATTACHMENT_DNS_DIAGNOSTIC: 'false' }, encoding: 'utf8', timeout: 10000
  });
  assert.equal(result.status, 1); assert.equal(result.stdout, '');
});
test('Java 도우미는 임의 host·환경 출력·HTTP·설정 변경을 포함하지 않는다', () => {
  const source = readFileSync('scripts/qa/AttachmentDnsDiagnostic.java', 'utf8');
  for (const value of ['case "BOEUN"', 'case "JECHEON"', 'args.length != 1', 'InetAddress.getAllByName(host)']) assert(source.includes(value));
  for (const value of ['System.getenv', 'setProperty', 'getMessage()', 'printStackTrace', 'HttpClient', 'URLConnection']) assert(!source.includes(value));
});
