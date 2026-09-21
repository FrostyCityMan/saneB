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
test('그룹 인자를 받지 않고 고정 관측만 호출한다', () => {
  assert(source.includes('[[ $# -eq 4'));
  assert(source.includes('AnnouncementAttachmentBbsObservationProbe "$4"'));
  assert(!source.includes('OfficialWorkerProbe'));
  const java = readFileSync('src/test/java/com/saneb/domain/announcementattachment/qa/AnnouncementAttachmentBbsObservationProbe.java', 'utf8');
  assert(java.includes('System.setProperty("saneb.attachment-observation.group", "TAEBAEK")'));
  assert(java.includes('selectClass(AnnouncementAttachmentBbsOfficialObservationTest.class)'));
  assert(java.includes('new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash()'));
  assert(!java.includes('getMessage()'));
});
test('새 JAR는 고정 시험 클래스만 포함하고 운영 classpath·리소스를 복사하지 않는다', () => {
  const build = readFileSync('build.gradle', 'utf8');
  const block = build.slice(build.indexOf("tasks.register('attachmentBbsObservationProbeJar'"), build.indexOf("tasks.register('attachmentBbsFixedCaseQa'"));
  assert(block.includes('sourceSets.test.output.classesDirs'));
  for (const text of ['sourceSets.main.output', 'testRuntimeClasspath', 'resources', '${name}*.class']) assert(!block.includes(text), text);
  assert(block.includes('preserveFileTimestamps = false'));
});
