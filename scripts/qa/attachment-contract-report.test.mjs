import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname, basename } from 'node:path';
import { validateSuite, validateReportTime, checkRequiredReports } from './attachment-contract-report.mjs';

const name = 'com.saneb.db.AnnouncementAttachmentJobIntegrationTest';
const xml = (extra = {}) => '<?xml version="1.0"?>\n<testsuite ' + Object.entries({
  name, tests: '71', failures: '0', errors: '0', skipped: '0', ...extra,
}).map(([key, value]) => `${key}="${value}"`).join(' ') + '></testsuite>';

test('실행된 정확한 suite의 생략 없는 결과만 허용', () => {
  assert.deepEqual(validateSuite(xml(), name), { suite: name, tests: 71, failures: 0, errors: 0, skipped: 0 });
});
for (const field of ['skipped', 'failures', 'errors']) {
  test(`${field}가 있으면 거부`, () => assert.throws(() => validateSuite(xml({ [field]: '1' }), name)));
}
test('0건·잘못된 숫자·범위 초과 거부', () => {
  for (const tests of ['0', '-1', 'NaN', '1.1', '9007199254740992']) assert.throws(() => validateSuite(xml({ tests }), name));
});
test('다른 클래스 또는 필수 필드 누락 거부', () => {
  assert.throws(() => validateSuite(xml({ name: 'unrelated' }), name));
  assert.throws(() => validateSuite(xml().replace('skipped="0"', ''), name));
  assert.throws(() => validateSuite(xml().replace('skipped="0"', 'skipped="0" skipped="1"'), name));
});
test('DOCTYPE·빈 파일·다른 XML root 거부', () => {
  for (const input of ['', '<!DOCTYPE testsuite>' + xml(), '<testsuites/>']) assert.throws(() => validateSuite(input, name));
});
test('필수 보고서 부재를 통과로 처리하지 않음', () => {
  assert.throws(() => checkRequiredReports(new URL('./fixtures/not-created/', import.meta.url).pathname, Date.now() - 1000), /필수 보고서가 없습니다/);
});
test('이번 실행보다 오래된 보고서와 누락·미래 시작 시각 거부', () => {
  const start = Date.now() - 1000;
  assert.doesNotThrow(() => validateReportTime(start + 100, start));
  assert.throws(() => validateReportTime(start - 1, start), /오래됐습니다/);
  for (const invalid of [undefined, NaN, 0, -1, Date.now() + 60000]) assert.throws(() => validateReportTime(start, invalid));
});

test('DB 성공만으로 설치 격리 실행의 누락·생략을 통과 처리하지 않음', () => {
  const root = mkdtempSync(join(tmpdir(), 'saneb-contract-report-'));
  const start = Date.now() - 1000;
  const reports = [
    ['attachmentJobIntegrationTest', name],
    ['attachmentMigrationTest', 'com.saneb.db.AnnouncementAttachmentMigrationTest'],
    ['attachmentMigrationTest', 'com.saneb.db.AnnouncementAttachmentBackfillIntegrationTest'],
    ['attachmentRuntimeIntegrationTest', 'com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeGateIntegrationTest'],
    ['attachmentWorkerIntegrationTest', 'com.saneb.db.AnnouncementAttachmentWorkerIntegrationTest'],
    ['attachmentPolicyDbQaIntegrationTest', 'com.saneb.domain.announcementattachment.service.impl.AttachmentWorkerDbQaLinuxIntegrationTest'],
    ['flywayIntegrationTest', 'com.saneb.db.FlywayMigrationIntegrationTest'],
  ];
  const save = ([task, className], extra = {}) => {
    const directory = join(root, 'build', 'test-results', task);
    mkdirSync(directory, { recursive: true });
    writeFileSync(join(directory, `TEST-${className}.xml`), xml({ name: className, tests: '1', ...extra }));
  };
  try {
    save(reports[0]); save(reports[1]);
    assert.throws(() => checkRequiredReports(root, start), /attachmentMigrationTest/);
    save(reports[2], { skipped: '1' });
    assert.throws(() => checkRequiredReports(root, start), /생략/);
    save(reports[2]);
    assert.throws(() => checkRequiredReports(root, start), /attachmentRuntimeIntegrationTest/);
    save(reports[3], { skipped: '1' });
    assert.throws(() => checkRequiredReports(root, start), /생략/);
    for (const tests of ['1', '2', '3']) {
      save(reports[3], { tests });
      assert.throws(() => checkRequiredReports(root, start), /attachmentRuntimeIntegrationTest: 필수 테스트 일부가 실행되지/);
    }
    save(reports[3], { tests: '4' });
    assert.throws(() => checkRequiredReports(root, start), /attachmentWorkerIntegrationTest/);
    save(reports[4], { skipped: '1' });
    assert.throws(() => checkRequiredReports(root, start), /생략/);
    save(reports[4]);
    assert.throws(() => checkRequiredReports(root, start), /attachmentPolicyDbQaIntegrationTest/);
    save(reports[5], { skipped: '1' });
    assert.throws(() => checkRequiredReports(root, start), /생략/);
    save(reports[5]);
    assert.throws(() => checkRequiredReports(root, start), /flywayIntegrationTest/);
    save(reports[6], { tests: '3', skipped: '1' });
    assert.throws(() => checkRequiredReports(root, start), /생략/);
    save(reports[6], { tests: '2' });
    assert.throws(() => checkRequiredReports(root, start), /일부가 실행되지/);
    save(reports[6], { tests: '3' });
    assert.equal(checkRequiredReports(root, start).length, 7);
  } finally {
    // mkdtemp가 이 테스트만을 위해 반환한 경로만 정리한다.
    const target = resolve(root);
    assert.equal(dirname(target), resolve(tmpdir()));
    assert.ok(basename(target).startsWith('saneb-contract-report-'));
    rmSync(target, { recursive: true });
  }
});
