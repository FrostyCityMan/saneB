import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { Resolver } from 'node:dns/promises';
import { mkdirSync, writeFileSync } from 'node:fs';
import { isIP } from 'node:net';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const targets = Object.freeze([
  Object.freeze({ code: 'BOEUN', host: 'www.boeun.go.kr' }),
  Object.freeze({ code: 'JECHEON', host: 'www.jecheon.go.kr' })
]);
const outputPath = 'build/reports/attachment-dns-diagnostic/result.json';
const javaSource = resolve(dirname(fileURLToPath(import.meta.url)), 'AttachmentDnsDiagnostic.java');
const knownErrors = new Set(['ENOTFOUND', 'ENODATA', 'ETIMEOUT', 'ESERVFAIL', 'ECONNREFUSED',
  'ECANCELLED', 'EFORMERR', 'EREFUSED', 'ENOTIMP', 'EBADRESP', 'ECONNRESET']);

// 주소 원문·resolver 설정·환경값은 artifact에 넣지 않는다. 동일 응답 집합은 지문으로 대조한다.
export function summarizeAddresses(addresses) {
  if (!Array.isArray(addresses) || addresses.length > 64 || addresses.some(value => !isIP(value)))
    return { status: 'INVALID_RESPONSE' };
  const unique = [...new Set(addresses)].sort();
  if (!unique.length) return { status: 'NO_ADDRESS' };
  return { status: 'RESOLVED', addressCount: unique.length,
    ipv4Count: unique.filter(value => isIP(value) === 4).length,
    ipv6Count: unique.filter(value => isIP(value) === 6).length,
    addressSetHash: createHash('sha256').update(unique.join('\n')).digest('hex') };
}

export function summarizeProcess(kind, result) {
  if (!['OS_GETENT', 'JVM_INETADDRESS'].includes(kind)) throw new Error('DNS_DIAGNOSTIC_KIND_INVALID');
  if (result.error?.code === 'ETIMEDOUT') return { status: 'PROCESS_TIMEOUT' };
  if (result.error || result.signal || !Number.isInteger(result.status)) return { status: 'PROCESS_FAILED' };
  if (result.status !== 0) return { status: kind === 'OS_GETENT' && result.status === 2 ? 'NOT_FOUND' : 'PROCESS_FAILED' };
  const text = typeof result.stdout === 'string' ? result.stdout.trim() : '';
  if (text.length > 8192) return { status: 'INVALID_RESPONSE' };
  if (kind === 'JVM_INETADDRESS') {
    if (['UNKNOWN_HOST', 'NO_ADDRESS', 'RESOLVER_FAILED'].includes(text)) return { status: text };
    if (!text.startsWith('RESOLVED\t')) return { status: 'INVALID_RESPONSE' };
    return summarizeAddresses(text.substring('RESOLVED\t'.length).split(','));
  }
  if (!text) return { status: 'NO_ADDRESS' };
  return summarizeAddresses(text.split(/\r?\n/).map(line => line.trim().split(/\s+/)[0]));
}

export function summarizeDnsError(error) {
  return { status: knownErrors.has(error?.code) ? error.code : 'QUERY_FAILED' };
}

export function selectComparison(os, jvm) {
  if (os.status === 'RESOLVED' && jvm.status === 'RESOLVED')
    return os.addressSetHash === jvm.addressSetHash ? 'RESOLVED_SAME_SET' : 'RESOLVED_DIFFERENT_SET';
  if (os.status === 'RESOLVED') return 'ONLY_OS_RESOLVED';
  if (jvm.status === 'RESOLVED') return 'ONLY_JVM_RESOLVED';
  return 'NEITHER_RESOLVED';
}

export async function collectDiagnostic({ run = spawnSync, resolveRecords = queryRecords, now = () => new Date().toISOString() } = {}) {
  const cases = [];
  // 각 getent/JVM 15초, Node A/AAAA 각각5초: 전체8개 논리 조회, 최대80초+기동 여유.
  // 논리 조회 수는 OS resolver가 전송하는 실제 DNS 패킷 수가 아니다.
  for (const target of targets) {
    const startedAt = now();
    const options = { encoding: 'utf8', timeout: 15000, killSignal: 'SIGKILL', maxBuffer: 8192,
      env: { PATH: '/usr/bin:/bin', LANG: 'C.UTF-8' } };
    const os = summarizeProcess('OS_GETENT', run('/usr/bin/getent', ['ahosts', target.host], options));
    const jvm = summarizeProcess('JVM_INETADDRESS', run('java',
      ['-Xms16m', '-Xmx64m', '-XX:ActiveProcessorCount=1', '-XX:+UseSerialGC', javaSource, target.code],
      { ...options, env: { ...options.env, PATH: process.env.PATH ?? '/usr/bin:/bin' } }));
    const a = await resolveRecords(target.host, 'A');
    const aaaa = await resolveRecords(target.host, 'AAAA');
    cases.push({ code: target.code, host: target.host, startedAt, finishedAt: now(), os, jvm,
      nodeA: a, nodeAaaa: aaaa, comparison: selectComparison(os, jvm) });
  }
  return { scope: 'FIXED_PUBLIC_HOST_DNS_DIAGNOSTIC_V1', observedAt: now(),
    status: 'OBSERVED_NOT_COLLECTION_QA', maximumLogicalLookups: 8, cases,
    httpRequestCount: 0, downloadedFileCount: 0, productionWriteCount: 0,
    isCollectionQaPassed: false, isPolicyQaPassed: false, isOperatingStateVerified: false };
}

async function queryRecords(host, type) {
  const resolver = new Resolver({ timeout: 2000, tries: 1 });
  let timer;
  try {
    return await Promise.race([
      (type === 'A' ? resolver.resolve4(host) : resolver.resolve6(host)).then(summarizeAddresses, summarizeDnsError),
      new Promise(resolveTimeout => { timer = setTimeout(() => {
        resolveTimeout({ status: 'QUERY_TIMEOUT' }); resolver.cancel();
      }, 5000); })
    ]);
  } finally { clearTimeout(timer); resolver.cancel(); }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  if (process.platform !== 'linux' || process.argv.length !== 2 || process.env.SANEB_ATTACHMENT_DNS_DIAGNOSTIC !== 'true') {
    console.error('DNS 진단은 Linux에서 명시적으로 승인한 고정 두 호스트만 실행할 수 있습니다.');
    process.exitCode = 1;
  } else {
    try {
      const result = await collectDiagnostic();
      mkdirSync(dirname(outputPath), { recursive: true });
      writeFileSync(outputPath, JSON.stringify(result, null, 2) + '\n', { mode: 0o600 });
      console.log(JSON.stringify(result));
    } catch {
      console.error('고정 DNS 진단 결과를 생성하지 못했습니다. 수집 성공으로 처리하지 않습니다.');
      process.exitCode = 1;
    }
  }
}
