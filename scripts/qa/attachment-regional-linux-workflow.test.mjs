import {readFileSync} from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const workflow = readFileSync(new URL('../../.github/workflows/attachment-contract-qa.yml', import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const job = workflow.split('  regional-transport-observation:\n')[1]?.split('\n  contracts:')[0];

test('파일 전송 QA는 계약 종료 후 명시한 최초 push에서만 실행한다', () => {
  assert.ok(job);
  assert.match(job, /needs: contracts/);
  assert.match(job, /!cancelled\(\) && github.event_name == 'push' && github.run_attempt == 1/);
  assert.match(job, /contains\(github.event.head_commit.message, '\[regional-transport-observation-01\]'\)/);
  assert.match(job, /timeout-minutes: 20/);
  assert.doesNotMatch(job, /continue-on-error|secrets\.|id-token|aws |deploy\.yml|sanebCollectionWindowsTrust/);
});

test('고정 지역 또는 충북 단일 공고만 수집 전용 task로 순차 실행한다', () => {
  assert.match(job, /--tests '\*RegionalTransportLinuxContractTest'/);
  assert.match(job, /:attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=POCHEON,GANGNEUNG,CHUNGBUK,GONGJU,PYEONGTAEK,SEONGNAM /);
  assert.match(job, /-PsanebCollectionReportLabel=LINUX-TRANSPORT-01 --no-daemon --console=plain --max-workers=1/);
  assert.match(job, /persist-credentials: false/);
  assert.doesNotMatch(job, /attachmentOfficialWorkerIntegrationTest|installDist|bootJar/);
  assert.match(job, /-PsanebBbsObservationGroup=CHUNGBUK -PsanebCollectionReportLabel=CHUNGBUK-FILE-01/);
  assert.match(job, /contains\(github.event.head_commit.message, '\[chungbuk-file-recheck-01\]'\) && !contains\(github.event.head_commit.message, '\[regional-transport-observation-01\]'\)/);
  assert.match(job, /contains\(github.event.head_commit.message, '\[regional-transport-observation-01\]'\) && !contains\(github.event.head_commit.message, '\[chungbuk-file-recheck-01\]'\)/);
});

test('실패 여부와 무관하게 지문과 JSON·JUnit metadata만 보존한다', () => {
  assert.equal((job.match(/if: always\(\)/g) ?? []).length, 2);
  assert.match(job, /sha256sum build\/classes\/java\/test\/com\/saneb\/domain\/announcementattachment\/qa\/AnnouncementAttachmentBbsOfficialObservationTest.class/);
  assert.match(job, /build\/reports\/attachment-regional-collection\/\*-LINUX-TRANSPORT-01.json/);
  assert.match(job, /if-no-files-found: error/);
  assert.match(job, /retention-days: 7/);
  assert.doesNotMatch(job, /path:.*(?:\.bin|\.pdf|\.hwp)/i);
});

test('잔여12개는 두 순차 묶음 모두 관측하고 어떤 실패도 성공으로 바꾸지 않는다', () => {
  const block = job.split('      - name: 잔여 12수집원 파일 관측')[1]?.split('      - name: 보고서 생산')[0];
  assert(block);
  assert.match(block, /\[pending-regions-linux-01\]/);
  assert.match(block, /!contains\(github.event.head_commit.message, '\[regional-transport-observation-01\]'\)/);
  assert.match(block, /!contains\(github.event.head_commit.message, '\[chungbuk-file-recheck-01\]'\)/);
  assert.match(block, /EUNPYEONG_SUPPORT,SEODAEMUN,GEOMDAN,ICHEON,DONGDUCHEON_YOUTH,SOKCHO/);
  assert.match(block, /YEONGDONG,ASAN_SUPPORT,UISEONG,SEONGJU,BONGHWA,NAMHAE/);
  assert.equal((block.match(/if ! bash /g) ?? []).length, 2);
  assert.equal((block.match(/result=1/g) ?? []).length, 4);
  assert.match(block, /exit "\$result"/);
  assert.doesNotMatch(block, /\|\| true|continue-on-error| &\s*$/m);
  assert.match(job, /\*-PENDING-LINUX-A-01.json/);
  assert.match(job, /\*-PENDING-LINUX-B-01.json/);
});
