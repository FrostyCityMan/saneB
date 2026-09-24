import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const script = 'scripts/qa/run-attachment-bbs-observation-probe.sh';
const source = readFileSync(script, 'utf8');
const bash = process.platform === 'win32' ? 'C:/Program Files/Git/bin/bash.exe' : '/bin/bash';
test('Bash 구문과 인자 없는 실행 차단', () => {
  const syntax = spawnSync(bash, ['-n', resolve(script).replaceAll('\\', '/')], { encoding: 'utf8', timeout: 10000 });
  assert.equal(syntax.status, 0, syntax.stderr);
  const denied = spawnSync(bash, [resolve(script).replaceAll('\\', '/')], { encoding: 'utf8', timeout: 10000 });
  assert.equal(denied.status, 1); assert.equal(denied.stdout, '');
});
test('비root·식별자·PID 격리·정리·10분 실행 상한 유지', () => {
  for (const text of ['"$(id -u)" != 0', 'sha256sum -- "$probe_jar"', '--unshare-pid', '--clearenv',
    '--die-with-parent', '--cap-drop ALL', '--kill-after=5 600', '--cpu=540', '-Xmx256m',
    'BBS_OBSERVATION_PROBE_CLEANUP=SUCCEEDED', '--bind "$work/tmp" /tmp']) assert(source.includes(text), text);
  for (const text of ['--ro-bind / /', 'app.env', '--bind /home', 'crypto.policy=']) assert(!source.includes(text), text);
});
test('고정 관측과 남구 단일 구조 진단 모드만 호출한다', () => {
  assert(source.includes('"$5" == FIXED || "$5" == OKCHEON'));
  assert(source.includes('probe_args=("$4")'));
  assert(source.includes('AnnouncementAttachmentBbsObservationProbe "${probe_args[@]}"'));
  assert(!source.includes('OfficialWorkerProbe'));
  const java = readFileSync('src/test/java/com/saneb/domain/announcementattachment/qa/AnnouncementAttachmentBbsObservationProbe.java', 'utf8');
  assert(source.includes('"$5" == BOEUN_OBSERVATION'));
  assert(source.includes('"$5" == BOEUN_DIAGNOSTIC'));
  assert(source.includes('"$5" == OKCHEON_DIAGNOSTIC'));
  assert(java.includes('"saneb.attachment-observation.diagnostic-budget", Boolean.toString(diagnostic)'));
  assert(java.includes('System.setProperty("saneb.attachment-observation.group", group)'));
  assert(source.includes('"$5" == NAMGU_OBSERVATION'));
  assert(java.includes('String group = dalseong?"DALSEONG":structure?"NAMGU_STRUCTURE":namgu ? "NAMGU" : boeun ? "BOEUN" : okcheon ? "OKCHEON" : "TAEBAEK"'));
  assert(source.includes('"$5" == DALSEONG_OBSERVATION'));
  assert(source.includes('"$5" == NAMGU_STRUCTURE'));
  assert(java.includes('selectClass(fixed?AnnouncementAttachmentBbsFixedCaseQaTest.class:AnnouncementAttachmentBbsOfficialObservationTest.class)'));
  assert(java.includes('"saneb.attachment-fixed.maximum-requests","39"'));
  assert(java.includes('"saneb.attachment-fixed.maximum-bytes","81508141"'));
  assert(java.includes('new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash()'));
  assert(!java.includes('getMessage()'));
  for (const mode of ['OTHER', 'JECHEON', 'TAEBAEK_HWP', 'OKCHEON; echo unsafe']) {
    const denied = spawnSync(bash, [resolve(script).replaceAll('\\', '/'), '/missing', '/missing', 'a'.repeat(64), 'b'.repeat(64), mode], { encoding: 'utf8', timeout: 10000 });
    assert.equal(denied.status, 1); assert.equal(denied.stdout, '');
  }
});
test('새 JAR는 고정 시험 클래스만 포함하고 운영 classpath·리소스를 복사하지 않는다', () => {
  const build = readFileSync('build.gradle', 'utf8');
  const block = build.slice(build.indexOf("tasks.register('attachmentBbsObservationProbeJar'"), build.indexOf("tasks.register('attachmentBbsFixedCaseQa'"));
  assert(block.includes('sourceSets.test.output.classesDirs'));
  for (const text of ['sourceSets.main.output', 'testRuntimeClasspath', 'resources', '${name}*.class']) assert(!block.includes(text), text);
  assert(block.includes('preserveFileTimestamps = false'));
});
