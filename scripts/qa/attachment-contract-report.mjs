import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Gradle 또는 고정 CI JUnit 실행기가 생성한 suite 헤더만 판정한다. 외부 문서·로그 원문은 출력하지 않는다.
export function validateSuite(xml, expectedName) {
  const header = xml.match(/^\s*(?:<\?xml[^>]*>\s*)?<testsuite\b([^>]*)>/);
  if (!header) throw new Error('JUnit suite 헤더가 없거나 형식이 다릅니다.');
  const attributes = [...header[1].matchAll(/\b([A-Za-z][\w.-]*)="([^"]*)"/g)];
  const values = Object.fromEntries(attributes.map(match => [match[1], match[2]]));
  if (values.name !== expectedName) throw new Error('필수 테스트 클래스의 보고서가 아닙니다.');
  const counts = {};
  for (const key of ['tests', 'failures', 'errors', 'skipped']) {
    if (attributes.filter(match => match[1] === key).length !== 1 || !/^\d+$/.test(values[key] ?? '')) {
      throw new Error('JUnit 테스트 집계 필드가 없거나 올바르지 않습니다.');
    }
    counts[key] = Number(values[key]);
    if (!Number.isSafeInteger(counts[key])) throw new Error('JUnit 테스트 집계 범위를 초과했습니다.');
  }
  if (counts.tests < 1) throw new Error('필수 테스트가 실행되지 않았습니다.');
  if (counts.failures || counts.errors || counts.skipped) throw new Error('필수 테스트에 실패·오류·생략이 있습니다.');
  return { suite: expectedName, ...counts };
}

export function validateReportTime(modifiedAtMs, startedAtMs) {
  if (!Number.isSafeInteger(startedAtMs) || startedAtMs <= 0 || startedAtMs > Date.now()) {
    throw new Error('이번 검증 실행의 시작 시각이 없습니다. 오래된 보고서는 인정하지 않습니다.');
  }
  if (!Number.isFinite(modifiedAtMs) || modifiedAtMs < startedAtMs) {
    throw new Error('보고서가 이번 검증 실행보다 오래됐습니다. 필수 테스트를 다시 실행하세요.');
  }
}

export function checkRequiredReports(root, startedAtMs) {
  validateReportTime(startedAtMs, startedAtMs);
  const required = [
    ['attachmentJobIntegrationTest', 'com.saneb.db.AnnouncementAttachmentJobIntegrationTest'],
    ['attachmentMigrationTest', 'com.saneb.db.AnnouncementAttachmentMigrationTest'],
    ['attachmentMigrationTest', 'com.saneb.db.AnnouncementAttachmentBackfillIntegrationTest'],
    ['attachmentRuntimeIntegrationTest', 'com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeGateIntegrationTest', 5],
    ['attachmentWorkerIntegrationTest', 'com.saneb.db.AnnouncementAttachmentWorkerIntegrationTest'],
    ['attachmentPolicyDbQaIntegrationTest', 'com.saneb.domain.announcementattachment.service.impl.AttachmentWorkerDbQaLinuxIntegrationTest'],
    ['flywayIntegrationTest', 'com.saneb.db.FlywayMigrationIntegrationTest', 3],
  ];
  return required.map(([task, name, minimumTests = 1]) => {
    let xml, modifiedAtMs;
    try {
      const path = resolve(root, 'build', 'test-results', task, `TEST-${name}.xml`);
      xml = readFileSync(path, 'utf8');
      modifiedAtMs = statSync(path).mtimeMs;
    }
    catch { throw new Error(`${task}: 필수 보고서가 없습니다.`); }
    validateReportTime(modifiedAtMs, startedAtMs);
    const result = validateSuite(xml, name);
    if (result.tests < minimumTests) throw new Error(`${task}: 필수 테스트 일부가 실행되지 않았습니다.`);
    return result;
  });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    console.log(JSON.stringify({ requiredContracts: checkRequiredReports(process.cwd(), Number(process.env.ATTACHMENT_QA_STARTED_AT)) }));
    console.log('DB 계약·고정 합성 파일 격리·합성 HTTP worker 통합만 판정했습니다. 실제 사이트·운영 worker·정책 적용·브라우저 검증은 별도입니다.');
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
