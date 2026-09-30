/** GitHub 격리 runner의 수집 영수증. 로컬 실행 및 운영 worker 증거와 구분한다. */
import assert from 'node:assert/strict';
import {importLocalCollectionReport} from './attachment-collection-receipts.mjs';

export function validateGithubCollectionEntry(entry) {
  const p = entry.github;
  assert.equal(entry.kind, 'GITHUB_COLLECTION_ONLY');
  assert.equal(p?.repository, 'FrostyCityMan/saneB');
  for (const name of ['runId', 'jobId', 'artifactId']) assert.ok(Number.isSafeInteger(p[name]) && p[name] > 0);
  assert.match(p.headSha, /^[a-f0-9]{40}$/);
  assert.equal(p.runAttempt, 1);
  assert.match(entry.path, new RegExp(`^build/qa-github-runs/${p.runId}/reports/attachment-regional-collection/[A-Z0-9_-]+\\.json$`));
  return {repository: p.repository, runId: p.runId, runAttempt: p.runAttempt, jobId: p.jobId,
    artifactId: p.artifactId, headSha: p.headSha};
}

export function importGithubCollectionReport(bytes, inventory, entry) {
  const github = validateGithubCollectionEntry(entry);
  // 기존 서명·제목·정리·운영 쓰기 금지 검증을 재사용하고 실행 출처만 별도로 보존한다.
  const imported = importLocalCollectionReport(bytes, inventory, entry.producerClassHash);
  return {...imported, status: 'IMPORTED_GITHUB_COLLECTION_ONLY', samples: imported.samples.map(sample =>
    ({...sample, evidenceScope: 'GITHUB_COLLECTION_ONLY_REPORT', github}))};
}
