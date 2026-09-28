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
test('공식 그룹은 양평 기본값과 고정 태백·충주·제천·보은·함안 표본만 허용한다', () => {
  assert(source.includes('case_group="${5:-YANGPYEONG}"'));
  assert(source.includes('[[ "$case_group" == YANGPYEONG || "$case_group" == TAEBAEK || "$case_group" == TAEBAEK_HWP || "$case_group" == CHUNGJU || "$case_group" == JECHEON || "$case_group" == BOEUN || "$case_group" == BOEUN_SEGMENT || "$case_group" == BOEUN_STRUCTURAL || "$case_group" == BOEUN_LONG_FORM || "$case_group" == HAMAN_SEGMENT || "$case_group" == HAMAN_LAYOUT_SEGMENT || "$case_group" == JUNGGU_SEGMENT ]] || exit 1'));
  assert(source.indexOf('"$case_group" == YANGPYEONG') < source.indexOf('mktemp'));
  assert(source.includes('AnnouncementAttachmentOfficialWorkerProbe "$4" "$case_group"'));
});

test('구간 엔진 실제 시험은 명시 task와 별도 보고서를 사용하며 CI에서 외부 호출하지 않는다', () => {
  const gradle = readFileSync('build.gradle', 'utf8');
  const start = gradle.indexOf("tasks.register('attachmentBoeunSegmentWorkerIntegrationTest'");
  assert(start >= 0);
  const task = gradle.slice(start, gradle.indexOf("tasks.register('attachmentOfficialWorkerProbeJar'", start));
  assert(task.includes("'BOEUN_SEGMENT'"));
  assert(task.includes('reports/attachment-boeun-segment-worker'));
  assert(task.includes('maxParallelForks = 1'));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml', 'utf8').includes('attachmentBoeunSegmentWorkerIntegrationTest'));
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
test('구조 보완 저장 검증은 구버전·CI 자동 외부 호출과 분리한다',()=>{
  const gradle=readFileSync('build.gradle','utf8');
  const task=gradle.slice(gradle.indexOf("tasks.register('attachmentBoeunStructuralWorkerIntegrationTest'"),gradle.indexOf("tasks.register('attachmentOfficialWorkerProbeJar'"));
  for(const value of ["'BOEUN_STRUCTURAL'",'reports/attachment-boeun-structural-worker','test-results/attachmentBoeunStructuralWorkerIntegrationTest','maxParallelForks = 1'])assert(task.includes(value));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml','utf8').includes('attachmentBoeunStructuralWorkerIntegrationTest'));
});
test('긴 서명란 단일 모드는 기존 실행 한도를 확대하거나 CI 외부 호출을 추가하지 않는다',()=>{
  assert(source.includes('"$case_group" == BOEUN_LONG_FORM'));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml','utf8').includes('BOEUN_LONG_FORM'));
  const java=readFileSync('src/test/java/com/saneb/db/AnnouncementAttachmentOfficialWorkerProbe.java','utf8');
  assert(java.includes('case "BOEUN_LONG_FORM" -> List.of("BOEUN-221497")'));
  assert(java.includes('selectSegmentMode(group) ? 5 : 44'));
  assert(java.includes('selectSegmentMode(group) ? 24L : 80L'));
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

test('함안 구간 worker는 고정1개·별도 보고서·명시 실행이며 CI 외부 호출을 추가하지 않는다',()=>{
  const gradle=readFileSync('build.gradle','utf8');
  const task=gradle.slice(gradle.indexOf("tasks.register('attachmentHamanSegmentWorkerIntegrationTest'"),gradle.indexOf("tasks.register('attachmentOfficialWorkerProbeJar'"));
  for(const value of ["'HAMAN_SEGMENT'",'reports/attachment-haman-segment-worker','test-results/attachmentHamanSegmentWorkerIntegrationTest','maxParallelForks = 1'])assert(task.includes(value));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml','utf8').includes('attachmentHamanSegmentWorkerIntegrationTest'));
  const java=readFileSync('src/test/java/com/saneb/db/AnnouncementAttachmentOfficialWorkerProbe.java','utf8');
  assert(java.includes('case "HAMAN_SEGMENT" -> List.of("HAMAN-41306")'));
  assert(source.includes('"$case_group" == HAMAN_SEGMENT'));
});

test('함안 최신 추출기 worker는 과거 실행과 분리하고 외부 요청 전에 버전을 고정한다',()=>{
  const gradle=readFileSync('build.gradle','utf8');
  const start=gradle.indexOf("tasks.register('attachmentHamanLayoutWorkerIntegrationTest'");
  assert(start>=0);
  const task=gradle.slice(start,gradle.indexOf("tasks.register('attachmentJungguSegmentWorkerIntegrationTest'",start));
  for(const value of ["'HAMAN_LAYOUT_SEGMENT'",'reports/attachment-haman-layout-worker','test-results/attachmentHamanLayoutWorkerIntegrationTest','maxParallelForks = 1'])assert(task.includes(value));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml','utf8').includes('HAMAN_LAYOUT_SEGMENT'));
  assert(source.includes('"$case_group" == HAMAN_LAYOUT_SEGMENT'));
  const java=readFileSync('src/test/java/com/saneb/db/AnnouncementAttachmentOfficialWorkerIntegrationTest.java','utf8');
  assert(java.indexOf('validatePinnedExtractor(')<java.indexOf('startDatabase();'));
});

test('화천 지원사업 사전 확인은 명시 task만 사용하고 자동 CI 외부 호출은 없다',()=>{
  const gradle=readFileSync('build.gradle','utf8');
  const start=gradle.indexOf("tasks.register('hwacheonSupportReferencePreflight'");assert(start>=0);
  const task=gradle.slice(start,gradle.indexOf("tasks.register('attachmentQaRuleSnapshot'",start));
  for(const text of ['HwacheonSupportReferencePreflightTest.class','SANEB_HWACHEON_SUPPORT_PREFLIGHT','maxParallelForks = 1','test-results/hwacheonSupportReferencePreflight'])assert(task.includes(text));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml','utf8').includes('hwacheonSupportReferencePreflight'));
  const java=readFileSync('src/test/java/com/saneb/domain/announcementattachment/qa/HwacheonSupportReferencePreflightTest.java','utf8');
  assert(java.includes('StandardOpenOption.CREATE_NEW'));assert(java.includes('requests>=2'));
  assert(java.indexOf('TITLE_NOT_ELIGIBLE_NOT_FETCHED')<java.indexOf('new AttachmentPinnedDownloadClient()'));
});

test('중구 두 파일 구간 worker는 명시 실행과 별도 보고서이며 자동 외부 호출하지 않는다',()=>{
  const gradle=readFileSync('build.gradle','utf8');
  const start=gradle.indexOf("tasks.register('attachmentJungguSegmentWorkerIntegrationTest'");
  assert(start>=0);
  const task=gradle.slice(start,gradle.indexOf("tasks.register('attachmentOfficialWorkerProbeJar'",start));
  for(const value of ["'JUNGGU_SEGMENT'",'reports/attachment-junggu-segment-worker','test-results/attachmentJungguSegmentWorkerIntegrationTest','maxParallelForks = 1'])assert(task.includes(value));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml','utf8').includes('attachmentJungguSegmentWorkerIntegrationTest'));
  assert(source.includes('"$case_group" == JUNGGU_SEGMENT'));
});

test('충주 worker 검증은 기본 양평과 별도 task·보고서이며 자동 외부 호출하지 않는다', () => {
  const gradle = readFileSync('build.gradle', 'utf8');
  const task = gradle.slice(gradle.indexOf("tasks.register('attachmentChungjuWorkerIntegrationTest'"), gradle.indexOf("tasks.register('attachmentOfficialWorkerProbeJar'"));
  assert(task.includes("systemProperty 'saneb.attachment-official-worker.group', 'CHUNGJU'"));
  assert(task.includes("reports/attachment-chungju-worker"));
  assert(task.includes("test-results/attachmentChungjuWorkerIntegrationTest"));
  assert(task.includes('maxParallelForks = 1'));
  assert(!readFileSync('.github/workflows/attachment-contract-qa.yml', 'utf8').includes('attachmentChungjuWorkerIntegrationTest'));
  const definition = readFileSync('src/test/java/com/saneb/domain/announcementattachment/qa/AnnouncementAttachmentBbsOfficialObservationTest.java', 'utf8');
  assert(!definition.includes('ChungjuEminwonProfileLiveQaTest'));
});
