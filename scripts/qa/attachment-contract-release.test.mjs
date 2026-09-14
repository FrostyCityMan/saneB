import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, readdirSync, rmSync, symlinkSync, statSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';

const helper = resolve('scripts/attachment-contract-release.sh');
const bash = process.platform === 'win32' ? 'C:/Program Files/Git/bin/bash.exe' : '/bin/bash';
const digest = value => createHash('sha256').update(value).digest('hex');
const shellPath = value => value.replaceAll('\\', '/');
const bashAvailable = existsSync(bash);

function fixture(t) {
  const work = mkdtempSync(join(tmpdir(), 'saneb-contract-release-test-'));
  t.after(() => rmSync(work, { recursive: true, force: true }));
  const root = join(work, 'releases'), payload = join(work, 'payload');
  const app = join(work, 'app.jar'), bundle = join(work, 'bundle.jar');
  mkdirSync(root); mkdirSync(payload);
  for (const directory of ['lib', 'bin', 'config', 'extractor/bin', 'extractor/lib']) mkdirSync(join(payload, directory), { recursive: true });
  for (const path of ['lib/saneb-attachment-contract-qa-test.jar', 'lib/dependency.jar',
    'bin/run-attachment-contract-qa.sh', 'config/hosts', 'config/logback-qa.xml', 'config/logging.properties',
    'extractor/bin/attachment-extractor', 'extractor/lib/parser.jar']) writeFileSync(join(payload, path), `fixture:${path}\n`);
  writeFileSync(app, 'application-one'); writeFileSync(bundle, 'application-one');
  const run = (expression, ...args) => spawnSync(bash, ['-c', 'set -euo pipefail; source "$1"; shift; ' + expression,
    'release-test', shellPath(helper), ...args.map(shellPath)], { encoding: 'utf8', timeout: 20000 });
  return { work, root, payload, app, bundle, run, release: join(root, digest('application-one')),
    install: () => run('install_attachment_contract_release "$@"', root, payload, app, bundle) };
}

test('배포 bundle은 웹 JAR와 별도 QA 패키지를 정확히 포함한다', () => {
  const workflow = readFileSync('.github/workflows/deploy.yml', 'utf8');
  assert.match(workflow, /installAttachmentContractQa attachmentContractQaTest/);
  assert.match(workflow, /! -name 'saneb-attachment-contract-qa-\*\.jar'/);
  assert.match(workflow, /"\$\{#boot_jars\[@\]\}" -ne 1/);
  assert.match(workflow, /cp -r build\/install\/attachment-contract-qa/);
  assert.match(workflow, /zip -r .* app\.jar appspec\.yml scripts attachment-extractor attachment-contract-qa/);
  const spec = readFileSync('appspec.yml', 'utf8');
  assert(spec.indexOf('location: scripts/install-attachment-contract-qa.sh') < spec.indexOf('location: scripts/start.sh'));
  assert(!spec.includes('destination: /opt/saneb/attachment-contract-qa\n'));
});

test('설치는 운영 접속·QA 실행·플래그 활성화를 하지 않고 launch에서 현재 JAR 경로만 고른다', () => {
  const installer = readFileSync('scripts/install-attachment-contract-qa.sh', 'utf8');
  assert.match(installer, /release_root=\/opt\/saneb\/attachment-contract-qa-releases/);
  assert(!/app\.env|DB_URL|systemctl|run-attachment-contract-qa\.sh|ENABLED=true/.test(installer));
  const start = readFileSync('scripts/start.sh', 'utf8');
  const launch = start.slice(start.indexOf("<<'LAUNCH'"), start.indexOf('\nLAUNCH\n'));
  assert.match(launch, /if \[ -z "\$\{SANEB_ANNOUNCEMENT_ATTACHMENT_CONTRACT_QA_ROOT:-\}" \]/);
  assert.match(launch, /select_attachment_contract_release \\\n    \/opt\/saneb\/attachment-contract-qa-releases \/home\/ubuntu\/app\/app\.jar/);
  assert(!launch.includes('set_env_default'));
  assert(readFileSync('.github/workflows/attachment-contract-qa.yml', 'utf8').includes('node --test scripts/qa/attachment-contract-release.test.mjs'));
});

test('실제 bash hook 및 helper 구문 검사', { skip: !bashAvailable }, () => {
  for (const file of ['scripts/attachment-contract-release.sh', 'scripts/install-attachment-contract-qa.sh', 'scripts/start.sh']) {
    const result = spawnSync(bash, ['-n', shellPath(resolve(file))], { encoding: 'utf8', timeout: 10000 });
    assert.equal(result.status, 0, result.stderr);
  }
});

test('새 패키지를 완전 설치하고 동일 패키지 재설치에서는 파일을 덮지 않는다', { skip: !bashAvailable }, t => {
  const f = fixture(t);
  const first = f.install(); assert.equal(first.status, 0, first.stderr);
  assert.match(first.stdout, /INSTALL=INSTALLED/);
  const file = join(f.release, 'lib/dependency.jar'), before = statSync(file).mtimeMs;
  const second = f.install(); assert.equal(second.status, 0, second.stderr);
  assert.match(second.stdout, /INSTALL=ALREADY_INSTALLED/);
  assert.equal(statSync(file).mtimeMs, before);
  assert.deepEqual(readdirSync(f.root), [digest('application-one')]);
  if (process.platform === 'linux') {
    assert.equal(statSync(file).mode & 0o777, 0o644);
    assert.equal(statSync(join(f.release, 'bin/run-attachment-contract-qa.sh')).mode & 0o777, 0o755);
  }
});

test('같은 JAR 지문의 패키지가 달라지면 기존 release를 보존하고 실패한다', { skip: !bashAvailable }, t => {
  const f = fixture(t); assert.equal(f.install().status, 0);
  writeFileSync(join(f.payload, 'lib/dependency.jar'), 'unexpected revision');
  const result = f.install(); assert.notEqual(result.status, 0);
  assert.match(result.stderr, /RELEASE_CONTENT_MISMATCH/);
  assert.equal(readFileSync(join(f.release, 'lib/dependency.jar'), 'utf8'), 'fixture:lib/dependency.jar\n');
});

test('bundle JAR와 실제 설치 JAR가 다르면 release를 만들지 않는다', { skip: !bashAvailable }, t => {
  const f = fixture(t); writeFileSync(f.app, 'different application');
  const result = f.install(); assert.notEqual(result.status, 0);
  assert.match(result.stderr, /APP_JAR_MISMATCH/);
  assert.deepEqual(readdirSync(f.root), []);
});

test('필수 구성 파일 누락은 설치 전에 실패한다', { skip: !bashAvailable }, t => {
  const f = fixture(t); rmSync(join(f.payload, 'config/hosts'));
  const result = f.install(); assert.notEqual(result.status, 0);
  assert.match(result.stderr, /PAYLOAD_INCOMPLETE/);
  assert.deepEqual(readdirSync(f.root), []);
});

test('추가 dependency가 남은 기존 release를 정상 설치로 판단하지 않는다', { skip: !bashAvailable }, t => {
  const f = fixture(t); assert.equal(f.install().status, 0);
  writeFileSync(join(f.release, 'lib/stale.jar'), 'stale');
  const result = f.install(); assert.notEqual(result.status, 0);
  assert.match(result.stderr, /RELEASE_CONTENT_MISMATCH/);
  assert(existsSync(join(f.release, 'lib/stale.jar')));
});

test('새 JAR 설치 및 이전 JAR 복구에서 각각의 불변 QA release를 선택한다', { skip: !bashAvailable }, t => {
  const f = fixture(t); assert.equal(f.install().status, 0);
  writeFileSync(f.app, 'application-two'); writeFileSync(f.bundle, 'application-two');
  assert.equal(f.install().status, 0);
  const select = () => f.run('select_attachment_contract_release "$@"', f.root, f.app);
  assert(select().stdout.endsWith('/' + digest('application-two')));
  writeFileSync(f.app, 'application-one');
  assert(select().stdout.endsWith('/' + digest('application-one')));
  assert.equal(readdirSync(f.root).length, 2);
});

test('복사 도중 실패하면 이번 staging만 정리하고 기존 경로를 보존한다', { skip: !bashAvailable }, t => {
  const f = fixture(t); writeFileSync(join(f.root, 'preserve'), 'user-owned');
  const result = f.run('cp() { command cp "$@"; return 1; }; install_attachment_contract_release "$@"',
    f.root, f.payload, f.app, f.bundle);
  assert.notEqual(result.status, 0);
  assert.deepEqual(readdirSync(f.root), ['preserve']);
  assert.equal(readFileSync(join(f.root, 'preserve'), 'utf8'), 'user-owned');
});

test('Linux payload 심볼릭 링크를 거부하며 외부 파일은 보존한다', { skip: process.platform !== 'linux' }, t => {
  const f = fixture(t), outside = join(f.work, 'outside'); writeFileSync(outside, 'preserve');
  symlinkSync(outside, join(f.payload, 'lib/escape.jar'));
  const result = f.install(); assert.notEqual(result.status, 0);
  assert.match(result.stderr, /PAYLOAD_TYPE_INVALID/);
  assert.equal(readFileSync(outside, 'utf8'), 'preserve');
  assert.deepEqual(readdirSync(f.root), []);
});

test('Linux 기존 release 심볼릭 링크를 따라가지 않는다', { skip: process.platform !== 'linux' }, t => {
  const f = fixture(t), outside = join(f.work, 'outside'); mkdirSync(outside);
  symlinkSync(outside, f.release);
  const result = f.install(); assert.notEqual(result.status, 0);
  assert.match(result.stderr, /RELEASE_INVALID/);
  assert.deepEqual(readdirSync(outside), []);
});
