# 첨부 3단계 처리 운영 기준선 및 QA 패키지 배포

## 최신 확인 — 2026-09-15 10시대

전체 출시 판정은 **Not ready**다. 아래03시대 V72 관측은 과거 기준선이며 현재 설치 버전이 아니다.

| 경계 | 현재 직접 확인 |
|---|---|
| AWS 인증 | 사용자 승인 root, 실제 계정=저장소 배포 대상, ap-northeast-2. ARN/인증값은 기록하지 않음 |
| 배포 | [Actions34916976535](https://github.com/FrostyCityMan/saneB/actions/runs/34916976535) success, `d-CTAGI7DTK` Succeeded |
| revision | `476c8f722e30464ff7c903b5519d86a4be1ad4c8` |
| 실제 JAR SHA256 | `16a1bb74a13e7d88c180fe6f3eb98e97c105c8362592f9c96de572890523f437` |
| schema / 가동 | JAR V83, 실제 DB V83/실패0, systemd active, localhost health UP |
| worker / QA 설치 | 새 worker class 있음. JAR 지문별 QA release/내장 추출기 설치, 실행 환경의 QA root 일치 |
| 활성화 | 첨부 worker·정책 QA·Provider QA 변수 미설정/코드 기본false. 운영 첨부 정책0, ACTIVE0 |
| 첨부 데이터 | set/file/extraction/job/active job/batch 모두0건. 일괄 재처리·ENFORCE 미실행 |
| 기존 원문 / 대상 | LOCAL_GOV_NOTICE2,945건, 활성 지자체223/목록 parser41종. 재처리 적격 건수 아님 |
| API key | 기업마당·정부24 실행 환경에서 모두 없음 |
| 공개 파일 QA | 단일 파일4건 예상 동작 통과/원본 정리4. PDF OCR/부분, HWP/HWPX 완전 텍스트. 공고 전체·worker 저장 성공 아님 |
| 운영 서버 독립 DB QA | SSM `d10887c4-488e-47e0-a6d7-e3413a9e3d23` Success. 합성 worker/DB 계약221건 통과·실패/생략/미실행/container 실패0, 정리 성공·운영 DB 미사용·설치 JAR 불변 |
| 공식 worker/DB/API | SSM `31cf27d7-cf0c-49f7-badd-10ffc1eeefcb` Success. 고정 양평3건 통과, BODY2/실제HWPX·PDF→worker→임시DB→API. 지원2파일은 PARTIAL_TEXT로 예외/검수 상태, JPG미지원·제목 제외 요청0. 운영 DB 미사용/정리 성공 |
| 브라우저 | 사용자 HTTP 진행 승인 후 관리자 로그인·대기열 조회 확인. 최종 검증 준비0건/전체2,945건·1/148페이지, 표시20건 모두3단계 미적용. 검수·DRAFT·역할별 E2E는 미완료 |

고정 읽기 전용 SSM 근거: 배포 전 runtime `b27e1306-0838-495d-8ca3-373814830ab4`, DB `a04fbb67-015a-408c-ae88-584129b2a10c`; 배포 후 runtime `cb6687b0-1361-4169-85b0-157bc2cb0a88`, DB `c8b4deb5-005e-4230-a345-81bd393fe8c9`, QA metadata `344622fa-f4ae-48d2-b262-6c0be53c9389`, ingress `2aac8108-3f76-4b4d-a81c-139de1926efa`는 Success다. DB 조회는 READ ONLY/statement8초·lock2초/ROLLBACK이며 원문·자격증명을 반환하지 않았다. 최초 runtime 진단1회는 Python3.10에서 지원하지 않는 로컬 진단용 hash 함수 사용으로 실패했고 이식 가능한 streaming hash로 수정 후 성공했다. 서비스 코드 오류로 계산하지 않는다.

V73~V83은 새 additive migration11개이며 배포 SHA와 이전 revision 사이 V1~V72 변경0을 확인했다. 동일 SHA Linux migration 순차 시험17건·원래 Flyway3건 통과 후 승인된 기존 환경 배포를 수행했다. 배포 후 스키마 downgrade/DB 복원/이전 JAR 실제 복구 시험을 수행한 것은 아니다. 자동 CodeDeploy rollback은 DEPLOYMENT_FAILURE에 설정되어 있으나 이번 성공 배포에서 실행되지 않았다.

최초 격리 DB 요청은 Windows 한글 포함 JSON 파일 전송 단계에서 실패했지만, ASCII escape 적용 후 실제 SSM 요청이 접수되고221건이 통과했다. 기존 미실행 기록을 현재 상태로 사용하지 않는다. 이 성공은 합성 계약 시험이며 공식 공고 전체 파일→worker 성공이나 정책 게시 승인 증거가 아니다.

설치 완료와 기능 활성화는 별개다. 공식 전체 파일/역할 기대값, 정책 QA·게시 승인, 전체 기존 데이터의 정확한 승인 범위·적용·복구 증거와 관리자 업무 E2E는 남아 있다. 비밀정보·운영 원문은 문서/로그에 추가하지 않았고 IAM/보안그룹/포트/정책 설정은 변경하지 않았다.

## 판정

2026-09-15 03:30~03:50 KST 읽기 전용 관측. **Not ready**다. 서비스 가동과 새 첨부 파이프라인 가동을 구분한다. 제목 1차 → 정제 본문 2차 → 실제 PDF/HWP/HWPX 텍스트 3차 → 관리자 최종 검증 순서, 실패/UNKNOWN 분리, 자동 ACTIVE 금지는 유지한다.

| 경계 | 확인 결과 | 의미 |
|---|---|---|
| Git QA 기준 | `25e03d2bc65f61a5b063d370744b58e999c38b73` | 아래 운영 JAR와 다른 코드다 |
| 서울 CodeDeploy | `saneb / saneb-dev`, 마지막 성공 `d-VHMRNZRPK` | 2026-09-09 배포 기록 |
| 배포 revision | `69b7278a92b2de4e71c55ac35db0069f1d4fbecc` | S3 revision 경로의 SHA이며 실행 JAR 지문과 구분 |
| 실제 설치 JAR SHA256 | `0c916a3856e78481bc97fab3267a2ad099e7a7310d5f29ef177a5d8d45989702` | SSM으로 현재 파일을 읽어 계산 |
| systemd / localhost health | active / UP | 외부 공개 health·브라우저 E2E 증거가 아님 |
| JAR migration / DB 적용 migration | V72 / V72 | JAR 목록과 실제 `flyway_schema_history`를 각각 조회 |
| 새 상시 첨부 worker class | 설치 JAR에 없음 | QA 브랜치 구현이 운영에 반영되지 않음 |
| 추출기 / 정책 DB QA 패키지 | 추출기 존재 / QA 패키지 없음 | 추출기 설치만으로 정책 QA를 실행할 수 없음 |
| 운영 API key | 실행 중 서비스의 기업마당·정부24 key 모두 없음 | 로컬 코드 구현 여부와 별개의 외부 API 운영 blocker |
| 운영 플래그 | 지자체 schedule·본문·분류 V2 true | 새 첨부 worker·정책 QA·Provider QA 플래그는 없음 |
| 실제 활성 지자체 대상 | 223개 / 목록 파서 41종 | 첨부 엔진6종·등록 프로필12개와 다른 분모 |
| 저장된 전체 원문 | LOCAL_GOV_NOTICE 2,945건 | 노출·적격·배치 적용 대상 수를 뜻하지 않음 |

실제 정책/job 테이블은 존재한다. 새 worker class가 없다는 이유로 해당 테이블도 없다고 추정하지 않는다. 동일 서비스 UID의 전체 thread는 한 시점에45개였으며, 새 QA 실행의 `nproc=128` 충족·성공을 보장하지 않는다.

후속으로 같은 완료 SSM snapshot의 활성 대상과 현재 코드의 지자체 SourceBinding11개를 대조했다. 강북010·부산027·남구034·중구045·달성052·서구074·태백121·횡성125·영월126·화천130·함안233(LGS- 접두어/6자리 코드)은 각각1행이며 목록 parser도 모두 일치한다. 남은212개 활성 대상에는 현재 첨부 모델 결합이 없다. 11개 결합의 일치는 실제 첨부 텍스트 처리·운영 성공을 증명하지 않는다. 새 SQL을 실행한 것이 아니라03:30~03:50 snapshot을 현재 코드와 대조한 결과다.

## 조회 방법과 변경 경계

2026-09-15 04시대 후속 조회는 AWS 세션 만료로 실패했다. 운영 정상 여부·신규 원주 SourceBinding 일치를 현재 시각 기준으로 재확인하지 못했다. 이 문서의 운영 사실은 위03:30~03:50 snapshot이며 최신 코드/운영 동기화를 의미하지 않는다. 이번 후속에서 운영 명령·DB 쓰기·배포는 실행되지 않았다.

- GitHub repository variables → CodeDeploy deployment group/tag → 해당 EC2/SSM Online 순서로 대상을 확정했다.
- SSM `4da5c4eb-d9d6-44f8-b444-8ff5b860d2bb`: JAR/서비스/localhost health/허용된 플래그 존재만 조회, Success.
- SSM `2ab2fbef-01b1-402e-9493-eb0fc762e587`: 실행 중 서비스 환경에서 접속값을 원격 메모리로만 사용했다. `default_transaction_read_only=on`, statement timeout8초/lock timeout2초 아래 고정 SELECT로 migration/대상/건수를 조회, Success.
- 인증값·프로세스 환경 원문·DB 접속값은 응답에 내보내지 않았다. 운영 데이터 쓰기·migration·정책·서비스 restart·배포는0이다.
- Windows의 현재 신뢰 저장소에 존재하며 유효한 기존 인증서를 프로세스 범위의 `AWS_CA_BUNDLE`로 지정해 TLS 검증을 유지했다. 전역 설정·신뢰 저장소를 수정하거나 TLS 검증을 끄지 않았다. 로그인/인증값은 기록하지 않는다.
- 외부 공개 health, 현재 운영 역할별 로그인/브라우저, 새 SHA 운영 배포는 이번 관측에서 검증하지 않았다.

## QA 실행 패키지 설치 설계와 구현

기존 배포 workflow가 별도 `installAttachmentContractQa` 산출물을 포함하지 않았고, 실제 운영에도 해당 패키지가 없었다. 다음 증분은 **배포 경로** 보완이며, 실제 설치/정책 QA 실행 성공이 아니다.

1. 배포 build에 `installAttachmentContractQa`·패키징 시험을 포함한다. QA JAR가 `build/libs`에 추가되므로 웹 JAR 선택에서 plain/QA JAR를 제외하고 정확히1개를 요구한다.
2. CodeDeploy bundle에 웹 JAR와 별도 `attachment-contract-qa` 전체 distribution을 포함한다. QA/JUnit/임시 PostgreSQL 라이브러리를 웹 JAR classpath에 추가하지 않는다.
3. AfterInstall에서 현재 설치 JAR와 같은 bundle JAR의 SHA256을 대조한다. `/opt/saneb/attachment-contract-qa-releases/<JAR SHA256>`의 새 임시 디렉터리에 완전 복사 후 rename한다. 심볼릭 링크/특수 파일·필수 파일 누락·기존 release 내용 불일치는 실패다.
4. 기존 release를 덮어쓰거나 의존성을 overlay하지 않는다. 이전 release와 관련 없는 경로는 삭제하지 않는다. 실패 때 이번 호출이 만든 `.install-*` 디렉터리만 경계를 검증하고 정리한다.
5. launcher는 실제 시작하는 JAR 지문으로 QA 경로를 선택한다. 이전 JAR를 복구하면 해당 이전 release가 선택된다. `app.env`의 명시적인 QA 경로는 보존하므로, 사용자 지정 경로가 있다면 정책 QA 코드 지문 검증과 별도 운영 확인이 필요하다.
6. 설치/launch가 정책을 게시하거나 worker/ENFORCE를 켜거나 원장을 재실행하지 않는다. 기존 데이터 처리는 정확한 적격 범위·전체 요청/byte 상한·효과·원복 승인을 별도로 요구한다.

파일: `scripts/attachment-contract-release.sh`, `scripts/install-attachment-contract-qa.sh`, `scripts/start.sh`, `.github/workflows/deploy.yml`, `appspec.yml`.

## 검증과 잔여

- 로컬 Node/Git Bash 임시 파일 시험:12건 중10통과/2 Linux 전용 생략, 실패0. 완전 설치·동일 패키지 재실행·다른 JAR·누락·동일 release 변조/남은 dependency 거부·두 JAR의 복구 경로·복사 중 실패의 staging만 정리를 실제 파일시스템에서 검증했다. 사용한 Node/Bash와 임시 디렉터리는 종료·정리했다.
- Gradle workflow 계약8건과 별도 패키징20건:46초 성공. bootJar/install은 UP-TO-DATE이며 새 operating build/실행으로 세지 않는다.
- 최종 `test :attachment-extractor:test bootJar attachmentContractQaTest installAttachmentContractQa --no-daemon --max-workers=1`은3분8초 성공이다. root2417건=2169통과/248조건부 생략/실패0이다. extractor·패키징·JAR/설치는 UP-TO-DATE이므로 이 호출에서 다시 실행한 것으로 합산하지 않는다. Windows-ROOT 신뢰 설정을 명령 범위로 사용했고 TLS 검증을 끄지 않았다. 새 Node 구문 검사와 `git diff --check`도 통과했다.
- Linux CI에 새 설치 시험을 연결했다. Linux 링크/권한·실제 CodeDeploy 설치·실제 systemd 복구는 별도 검증 대상이다.
- 후속 [Linux 실행21](https://github.com/FrostyCityMan/saneB/actions/runs/34883813483), `74cc12553c3ca4ac86eb2fd0c063fbac48abc2f5`는 전체 성공이다. 설치 시험12/12·생략0, 실제 DB/독립220/부모2도 통과했다. Linux 링크/권한 검증은 완료했지만 운영 CodeDeploy 설치·systemd 복구와 동일 SHA 운영 브라우저는 미실행이다. 상세 건수는 [Linux 검증 기록](../backend/announcement-attachment-linux-contract-qa-2026-09-11.md)을 따른다.
- 새 hook 설치 실패 시 CodeDeploy 전체 복구, 기존 추출기 경로의 복구, additive migration 이후 이전 JAR 호환성까지 전부 검증했다고 주장하지 않는다. 이 증분은 QA 패키지와 JAR의 경로 결합을 다룬다. 기존 hook의 JAR 복구만으로 DB/추출기를 완전 원복할 수는 없다.
- 불변 release는 자동 삭제하지 않는다. 보관 용량·불필요한 과거 release 정리는 복구 대상이 확정된 후 승인된 별도 작업으로 수행한다.

다음 순서는 새 설치 시험의 Linux 결과 확인 → 운영223개에 대한 첨부 프로필/본문 영역의 실제 적용성·남은 대상 확정 → 공식 파일 텍스트/역할 기대값과 전체 Provider QA → 동일 SHA 운영 설치·스모크 → 정확한 범위 승인 후 활성화/기존 데이터 처리·역할별 운영 브라우저 E2E다. 전체 Gate0~8과 ATT001~062를 축소하지 않는다.
