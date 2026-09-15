import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const script = 'scripts/qa/run-attachment-official-worker-probe.sh';
const source = readFileSync(script, 'utf8');
const bash = process.platform === 'win32' ? 'C:/Program Files/Git/bin/bash.exe' : '/bin/bash';

test('공식 QA launcher의 Bash 구문이 유효하다', () => {
  assert(existsSync(bash));
  const result = spawnSync(bash, ['-n', resolve(script).replaceAll('\\', '/')], { encoding: 'utf8', timeout: 10000 });
  assert.equal(result.status, 0, result.stderr);
});
test('인자 없는 실행은 DB나 파일 작업 전에 거부한다', () => {
  const result = spawnSync(bash, [resolve(script).replaceAll('\\', '/')], { encoding: 'utf8', timeout: 10000 });
  assert.equal(result.status, 1);
  assert.equal(result.stdout, '');
});
test('공개 요청 QA도 비root·깨끗한 환경·PID 격리와 원본 정리를 유지한다', () => {
  for (const value of ['"$(id -u)" != 0', '--unshare-pid', '--unshare-ipc', '--clearenv',
    '--die-with-parent', '--cap-drop ALL', '--nproc=128', '--as=2147483648',
    '-cp \'/probe.jar:/qa/lib/*\'', 'OFFICIAL_WORKER_PROBE_CLEANUP=SUCCEEDED']) assert(source.includes(value), value);
  assert(!source.includes('--ro-bind / /'));
  assert(!source.includes('--bind /home'));
  assert(!source.includes('app.env'));
  assert(source.includes('sha256sum -- "$probe_jar"'));
  assert(source.includes('--ro-bind "$java_crypto_policy" "$java_crypto_policy"'));
  assert(source.includes('--ro-bind "$java_policy_file" "$java_policy_file"'));
  assert(source.includes('unlimited/default_local.policy'));
  assert(!source.includes('-Dcrypto.policy='));
  assert(readFileSync('scripts/qa/run-attachment-contract-qa.sh', 'utf8').includes('--unshare-all'));
});
test('별도 probe JAR는 운영 코드·설정·JUnit 의존성을 포함하지 않는다', () => {
  const gradle = readFileSync('build.gradle', 'utf8');
  const task = gradle.slice(gradle.indexOf("tasks.register('attachmentOfficialWorkerProbeJar'"), gradle.indexOf("tasks.register('attachmentBbsFixedCaseQa'"));
  assert(task.includes("layout.buildDirectory.dir('official-worker-probe')"));
  assert(task.includes('sourceSets.test.output.classesDirs'));
  assert(!task.includes('sourceSets.main.output'));
  assert(!task.includes('testRuntimeClasspath'));
  assert(!task.includes('resources'));
});
