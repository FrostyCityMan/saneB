# 공고 첨부 수집 E2E 장기 구현 진행 기록

## 목표와 승인 범위

2026-09-09 사용자 실행 요청 기준. 본문·PDF/HWP/HWPX 첨부 상시 수집부터 DB/API/관리자 검수·DRAFT 전환·기존 데이터 처리·운영 브라우저 QA까지 완료한다. CLI 표본 성공을 전체 완료로 보지 않는다.

- 코드·additive migration·문서·테스트·커밋·푸시·기존 환경 배포·브라우저 QA는 이번 범위다.
- 정책 게시·ENFORCE·기존 데이터 적용은 구체적인 대상/건수/요청량/효과/복구 방법을 제시하고 승인 후 실행한다.
- KMS/IAM/보안그룹/공개 포트/DB 복원/신규 유료 서비스는 별도 승인 대상이다.
- 원본과 개인정보는 로그·감사 metadata에 남기지 않는다. 임시 원본은 정리한다.

## 기준선

- 작업 경로: `C:\PersonalProject\saneB`, 루트 AGENTS.md 완독.
- 시작 HEAD: `ae893b87348a9bd1cb0893763f6cad5047093a24`, master, 작업 트리 clean.
- origin URL: `https://github.com/FrostyCityMan/saneB.git`. Windows 신뢰 저장소를 명령 단위로 지정한 원격 실조회에서도 master가 시작 HEAD와 일치했다.
- 실제 최신 migration: V72. 첨부 11개 테이블과 별도 CLI/순수 분류기만 존재한다. 상시 worker/DAO/API/UI는 시작 시 없음.
- 이전 서버 QA 문서에는 배포 69b7278 / CodeDeploy d-VHMRNZRPK 성공과 localhost health UP이 있다. 이번 작업의 현재 운영 확인을 대신하지 않는다.
- 지원 범위: 일반 텍스트 PDF, HWP 5.x, HWPX. OCR 실행/암호 우회/매크로/재귀 수집은 비범위이며 실패·검수 상태는 구현 범위다.

## Gate

| Gate | 상태 | 실제 결과 / 다음 작업 |
|---|---|---|
| 0 맥락·범위·검증 목록 | [~] | 09-10 운영 조회 snapshot: 지자체 223개 활성 수집원/41종 목록 profile·V72·첨부 정책/작업 0건. 현재값 재확인·첨부 profile 매핑/정식 URL·브라우저 인증은 남음 |
| 1 DB·API 계약 | [~] | 09-14 V81 게시 경합 잠금/Provider QA 정책 연결 및 3단계 처리 projection·v2 대기열 필터 구현. 기존 v1·과거 migration 보존. 실제 PG·전체 실제 catalog 검증은 미완료 |
| 2 상시 worker·Provider | [~] | worker/scheduler·총12개 첨부 profile(기존9+태백/횡성/영월)·고정 예약/ENFORCE binding·24시간 재확인/checkpoint 구현. 새 BBS9사례14파일 발견/다운로드/signature·세션 비전달·기관 한정 헤더 호환 확인. 전체 profile/최신 PG/실제 Linux worker QA는 남음 |
| 3 분류·정책 | [~] | 09-14 전체 Provider 실행 근거 verifier→정책 QA→게시 재검증 연결 구현. 최신 실패/대기 우선·4단계 PASSED 때만 VERIFIED. 공식 참조9/실행 기대값0이므로 실제 정상 아님. 본문 전용 영역·문서 역할 자동 규칙·전체 기대값/Linux/Provider 잔여 |
| 4 관리자 API·화면 | [~] | 게시·배치·원복 UI에 이어 09-14 처리 흐름 상세와 읽기 전용 최종 검증 대기열 구현. SQL 전체 count/페이지·9상태 분리·URL 보존·늦은 응답 거부. 표적 Java/HTTP 및 Node 회귀 통과, 실제 PG/운영 역할·브라우저 E2E는 미완료 |
| 5 기존 데이터 | [~] | 전체 후보 고정·불변 분할/배치/수집/적용/원복 UI 구현. 정부24 필터 코드 차이에 의한 후보 누락·고정 분할 충돌 수정 및 미지원 profile 안내 보존. 실제 PG 전체/경합·승인 범위 운영 실행/최종 대조는 필수 미완료 |
| 6 자동·실파일 QA | [!] | 09-14 Linux 세 번째 실행: PG job192/192, 첨부 migration2/2, 분할13/13, Linux 합성12파일 격리 추출, worker8/8통과. root2064통과/241생략·extractor25·Node152. 독립 namespace 환경 계약 실패를 수정 후 재검증 중이며 부모 연결/전체 Provider는 미완료 |
| 7 운영 배포·활성화 | [!] | 09-14 GitHub 소유 계정 pull/push/admin=true 확인 및 QA 전용 브랜치 푸시 완료, master/배포 불변. 서울 AWS STS의 최근 TLS 실패는 미해결. 최신 구현 배포/정책/데이터 적용 미실행. TLS·IAM 완화 없음 |
| 8 운영 브라우저 E2E | [ ] | 사용자 명시 승인 있음. 합성 API 브라우저 결과는 별도 로컬 증거이며 운영 역할/업무/오류/반응형 검증을 대신하지 않음 |

## 최신 실행 기록

### 2026-09-14 — 변경된 처리 흐름 유지·Linux 최초 실증과 실패 수정

- 세 번째 [Actions 34826506160](https://github.com/FrostyCityMan/saneB/actions/runs/34826506160), SHA `6dc12e12073e6e46a9ab7566380949eb9cd48c42`: job192·migration2·분할13·worker8 및 Linux 합성12파일(1시험)이 모두 통과했다. root2305=2064통과/241조건부 생략·extractor25·독립 QA 패키지15·Node152와 bootJar/설치 패키지 생성도 성공했다. 독립 namespace가 `CLEAN_ENVIRONMENT_REQUIRED`로 차단되어 전체 실행은 실패다. 정리는 성공했으나 정책 부모 연결은 미실행이다.
- bubblewrap이 생성하는 고정 `PWD=/work`와 실행기 환경 검증 계약의 불일치를 수정한다. 다른 PWD/누락/운영 환경변수/조건 우회 거부 회귀를 유지하며 격리·테스트 조건을 해제하지 않는다. 수정본의 전체 원격 재검증 전 성공으로 집계하지 않는다.

- 두 번째 [Actions 34825668605](https://github.com/FrostyCityMan/saneB/actions/runs/34825668605), SHA `17243d54a132598d37e13753482eaef8fe6a9959`도 전체 실패다. PG job192건 중191통과/1실패, 첨부 migration2통과, 분할13건 중1통과/12실패, 설치된 Linux 격리 추출12합성 사례(1시험) 통과, worker8건 중7통과/1실패를 확인했다. 전용 task는 모두 생략0이고 root2304=2064통과/240생략·extractor25·독립 QA14·Node152는 통과다.
- 남은 원인: 분할 SELECT/GROUP BY의 같은 값이 다른 JDBC parameter로 바인딩되어 PostgreSQL이 같은 식으로 인정하지 않은 실제 Mapper 오류, commit 시 지연 제약 예외 wrapper에 대한 시험 기대값, 미지원 파일을 포함한 worker 전체 상태를 SUCCEEDED로 기대한 시험 오류다. 분할은 한 번 계산한 segment_no alias로 group하고, 제약 거부는 PostgreSQL SQLState23514와 실제 rollback을 검사한다. worker는 실패 파일을 숨기지 않는 PARTIAL_FAILED를 기대한다. 계약·검수 조건·migration을 약화하지 않는다.
- 독립 실행기의 고정 목록이 parameterized DB2건을 사전 집계하지 못하는 문제도 확인했다. 두 입력을 이름이 고정된 @Test2건으로 분리하고 동적 시험 누락을 패키징 Gate에서 차단했다. 현재 inventory215=job192/migration2/backfill13/worker8, 실행0/INVENTORY_ONLY이며 실제215건 통과가 아니다. 로컬 패키지 시험15건 통과. 이후 원격 재실행이 필요하다.

- 사용자 재강조에 따라 제목 1차 → 정제 본문 2차 → 공식 첨부 발견·실제 텍스트 3차 → 관리자 최종 검증을 기준으로 재개했다. 제목 제외 건 상세/첨부 요청 금지, 중간 A/B·본문 부족의 첨부 분석 지속, 기술 예외와 정상 후보 분리를 유지한다.
- 기존 장기 goal 범위의 코드·계약·테스트467파일을 QA 전용 `codex/attachment-three-stage-linux-qa`에 `bd148024611284b6c8ca2d6997e5ed2e87309721`로 커밋·푸시했다. Word2파일은 제외·보존했다. 원격 master는 `ae893b87348a9bd1cb0893763f6cad5047093a24` 그대로이며 배포 workflow는 실행하지 않았다.
- [Actions 34824820039](https://github.com/FrostyCityMan/saneB/actions/runs/34824820039), 위 정확한 SHA의 Ubuntu22.04/Java21에서 실제 loopback PostgreSQL 초기화와 V81까지 migration 이후 job192건을 실행했다.147통과/45실패/생략0이다. root2058통과/240조건부 생략, extractor25통과, 별도 QA 실행기 테스트14통과, Node152통과다. 첫 DB 실패로 이후 전용 task와 독립 namespace 실행은 진행되지 않았으며 성공으로 계산하지 않는다.
- 45실패: 실제 ReviewMapper가 없는 `a.announcement_code`를 읽은39건, 테스트의 동일 provider_notice_id 중복3건, 대기열 current 포인터/플래그를 별개 transaction으로 바꾼1건, 테스트 context에 Bean으로 등록되지 않은 IntakeDao 조회1건, NULL 문자열 결합으로 출처가 바뀌지 않은 fixture1건이다. 실제 컬럼 `public_code`에 기존 응답 alias를 유지하고, fixture/조회/transaction을 수정했다. 과거 migration·제약·assertion은 약화하지 않았다.
- 새 분류 회귀5건으로 중간 A/B 상태에서도 첨부 근거가 생성되는지, 충분한 본문이어도 첨부 B를 검출하는지, 발견 실패가 NO_FILES로 둔갑하지 않는지 확인했다. 이 클래스19건 로컬 통과. 최초 추가시험2건의 제목=본문 fixture는 기존 BODY_UNAVAILABLE 정책에 맞춰 정상 본문으로 교정했으며 업무 코드의 규칙은 변경하지 않았다.
- QA workflow에 정확한 QA 브랜치 push만 허용하고 대기열 Node18건을 포함했다. Gradle `--continue`는 독립 task의 추가 실패 수집용이며, 한 건의 실패도 전체 실패로 반환한다. 필수 보고서 누락/생략 차단과 운영 자격증명 미사용은 유지한다. 현재 기록은 재실행 전이며 Linux 전체 성공·전체 Gate 완료가 아니다.

### 2026-09-14 17:32 KST — 장기 goal 재개·3단계 최종 검증 대기열

- 문서 시각화 작업 후 사용자 재개 요청에 따라 실제 구현을 진행했다. AGENTS/장기 goal 운영 스킬·UI/UX 운영/구현 스킬과 기존 v2 조회·Mapper·화면·검증 장치를 확인했다. Word 산출물과 사용자 프로세스/기존 변경을 보존했다. 전체 ATT001~062/Gate0~8은 유지하며 최종 통과0·진행6·차단2·대기1, Not ready다.
- 직전 P1 증분의 처리 흐름 projection/상세 UI에 이어 P2를 구현했다. 9개 `processingFlowStatusCode`를 v2 목록의 additive query로 검증하고 list/count의 공통 SQL CASE·SearchWhere에 바인딩한다. 기존10인자 조건 생성자·v1·base/effective/preview·쓰기에 필요한 lock/CAS·제목 제외/QA 비노출 의미를 보존한다. DDL 없음, 최신 migration V81 및 추적 migration 변경0.
- `/app/admin/announcement-attachment-queue`는 ADMIN/OPERATOR/APPROVER 읽기 전용이며 기존 제목·본문 목록과 별도 연결한다. 기본 최종 검증 대기, 전체/9상태·수집처·검색어·페이지 URL 보존, 현재 근거/자동 완전성 분리, 상태별 다음 행동, 원문 textContent 렌더링, 늦은 응답/오류 거부를 구현했다. 0건과 실패·미확인 구분 및 재조회, native label/상태 알림/320px 대응 스타일을 추가했다. 목록에서 외부 수집·분류·검수·초안·공개 쓰기는 실행하지 않는다.
- 표적 `:test`는43초 성공: ProcessingFlow/CurrentService/CurrentController/QueueView/ReviewView/Mapper 회귀다. 전체 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`은3분50초·22task 중7실행/15up-to-date로 성공했다. root XML2298=2059통과+239조건부 생략·실패/error0. 독립 QA 패키지14는 현재 산출물로 통과했고 추출기25는09-12 기존 결과 재사용이다.
- `node --test` 기존7파일+`attachment-queue-ui.test.mjs` 합계152통과/실패·생략0. 새18건에는 순수 필터/HTTP/페이지 계약뿐 아니라 실제 화면 이벤트 코드의 DOM/HTTP 대역 실행4건(페이지 이동, 필터1페이지 초기화, popstate, 늦은 응답, 실패·미지 URL 복구)을 포함한다. 이는 실제 브라우저/접근성/운영 DB 성공 증거가 아니다.
- 실제 임시 PostgreSQL9상태/29행·5개 페이지 크기·SQL/Java projection/전체 count 대조 시험을 작성하고 전용 `attachmentJobIntegrationTest --tests 'com.saneb.db.AnnouncementAttachmentJobIntegrationTest.processingQueueMatchesJavaProjectionForAllNineStatesAndFullPageCounts'`를 같은 Gradle 옵션으로 실행했다.18초에 initializationError1로 실패했다.17:27:54 Code Integrity 이벤트3077/3033, 상태0xc0e90002·VerifiedAndReputableDesktop 정책이 embedded-pg 실행 파일의 서명/정책 차단을 기록했다. initdb 초기화 전 실패이므로 SQL assertion은 실행되지 않았다. 보안 완화·파일 차단 해제·운영 DB 대체는 하지 않았다.
- 설치한 독립 QA의 `--inventory`는213건(job190/migration2/backfill13/worker8) 발견,통과0·미실행213·INVENTORY_ONLY다. extractorRuntimeHash=null이며 Linux 성공이 아니다. QA artifactHash `012b68c497cc76ada8ceac13d90c1bfe87a83d6c535f6960f54b168bdd2c1767`, 업무 코드 지문 `671c614f0838f91e644eccf9cff8cf6531a02131b9b62a8bec79eb7735462190`이다.
- 외부 읽기 재확인: `gh api repos/FrostyCityMan/saneB --jq '.permissions | {pull,push,admin}'`는pull=true/push=false/admin=false, Docker info는Linux named pipe 없음, 서울 리전 AWS STS는TLS 인증서 검증 실패다. AWS 계정/secret은 출력하지 않았다. `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`는HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`와 일치한다. 운영health/현재 배포 SHA/운영 로그인은 이번 미확인이다.
- DB11.30/API24.33 및3단계 설계P2·Gate를 갱신했다. 본문 전용 영역/문서 역할 자동 판별(P3), 전체 profile·공식 기대값·Provider QA 화면(P4), 실제Linux/PG/운영배포·승인된 데이터 적용·브라우저(P5~P8)는 잔여다. 공식 catalog는여전히참조9/expectation0이다. 검증 대기열 구현을 전체 수집 완료나 인력 절감 실측으로 확대하지 않는다.
- 커밋·푸시·운영 배포·정책/ENFORCE·기존 데이터 적용·브라우저·서브에이전트 실행 없음. 장기 goal 스킬에 따라 필수 실행 경로의 차단을 기록하고 보안/권한을 우회하지 않는다. 이번 소유 Gradle/Node/inventory 프로세스는 종료됐으며 사용자의 기존 프로세스는 유지했다.
- 최종 재확인: master/HEAD 동일, 추적 수정35·미추적434·staged0, 추적 migration 변경0, diff check 지적0, ATT62행 유지. 새 JS2개 syntax 검사와 제한 secret 패턴 검사 지적0이다. Java/PostgreSQL 프로세스0·17시 이후 Node0이며 사용자 기존 Node/Word는 유지했다. 테스트 소유 임시 DB 폴더 정리는 도구 정책에 거부되어 남겨두었고 다른 삭제 경로로 재시도하지 않았다. 테스트 초기화 실패와 실제 운영 미검증을 성공으로 표시하지 않았다.

### 2026-09-12 11:15 KST — V80 Provider QA 관리 API·승인 원장·스케줄러 연결

- 직전 회차는 Gate 개수의 상태 보고로 진척 없음이었다. AGENTS/장기 목표 운영 스킬·Git·작성 중인 V80/DTO/설계·기존 원장/정책/검증 장치를 재확인하고 다음 구현을 진행했다. 전체 ATT001~062/Gate0~8은 유지하며 최종 통과0·진행6/차단2/대기1, Not ready다.
- V80 승인 계획은 run의1:1 불변 metadata다. 같은 생성 transaction에서 전체 case·정확한 요청/byte·시간 합계와 READY를 봉인해야 하며 기존 V1~V79는 편집하지 않았다. 이전 run은 LEFT JOIN으로 보존하며 V80 binding 없는 과거 활성 이력은 새 실행에 사용하지 않는다.
- `/api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-runs`의 계획/원장/항목 페이지·ADMIN 예약/취소를 Controller→Service→ServiceImpl→DAO→Mapper로 연결했다. 정확한 version/snapshot/catalog/plan·분할/건수/요청/byte/시간·명시 확인을 검증한다. 같은 키/actor/정책/입력 재전송은 OFF·60초 간격 검사보다 먼저 반환한다. 원문/lease/멱등 키/requestHash는 응답하지 않는다.
- 기본 OFF 스케줄러가 승인된 전체 분할과 원장 순서/입력/파일 수/상한을 현재 catalog에서 재구성해 대조한 후 한 공고씩 기존 ExecutionService에 전달한다. 설치/HTTP/추출은 쓰기 transaction 밖이며 실행 중 소유자는 재claim하지 않는다. 변경은 미실행 항목만 실패로 보존하고 설치 확인 불가는 성공/입력 변경으로 추정하지 않는다. 취소·만료·종료 정리를 먼저 수행하며 소유 executor 종료를 구현/시험했다.
- 최초 compileJava는13초 성공. 첫 표적은 테스트의 잘못된 페이지 메서드 totalElements 때문에21초 컴파일 실패해 기존 totalCount로 정정했다. 다음 표적363건 중6실패/187생략은 JSON long/정수 node 타입 차이를 계획 변경으로 오인한 실제 서비스 결함이었다. 동일 JSON 표현으로 정규화해 비교하도록 수정한 뒤34초 성공했다. 스케줄러 포함 확장 표적도40초 성공했고 실제 값/입력 변경 거부 조건은 유지했다.
- 최종 명령 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`:3분24초/22task 중9실행·13up-to-date, BUILD SUCCESSFUL. XML root2153=1919통과+234조건부 생략/실패·error0. 관리 Service34·HTTP23·스케줄러2·기존 실행 Service35/실행기42/catalog27 통과. 추출기25는 변경 없는 이전 결과 재사용, 독립 QA 패키지14는 현재 산출물로 통과했다.
- 실제 PG3사례(승인 계획 누락/시간 불일치 원자 거부, 불변 이력/페이지 전체 분모, 미실행 실패와 live owner 보존)와 V79→V80/빈DB 시험을 작성/컴파일했으나 실행하지 않았다. 독립 `--inventory`는208건 발견/통과0/미실행208·INVENTORY_ONLY, extractorRuntimeHash=null이다. 초기 조회는 nested result를 최상위로 잘못 투영해null이었으며 실제 result에서208/0을 다시 확인했다. 실제 Linux/DB 성공으로 대체하지 않는다.
- Node7파일 `node --test`는130통과·실패/생략0이며 종료됐다. 실행 지문 `2106730eff83a453d0622bcc13b3a306b6e515d1bfec66669ecbf2063a7975dd`; web JAR SHA256 `28aea066f05d73befee4f8d1e68b096811194b05fad273d9e173c341936f47c8`, QA JAR `e8c41fe258b910a1429e4fc4202ed4d7979feab50d6937c66894af42bfca2569`. 산출물/독립 목록 검증이지 운영 설치 증거가 아니다.
- Docker Linux engine named pipe 부재와 docker-desktop/Stopped/WSL2, GitHub pull=true/push=false/admin=false를 새로 확인했다. AWS/Actions/현재 운영 SHA·health·인증은 미조회다. TLS/격리 완화·Docker 초기화·운영 변경을 하지 않았다. 이전에 요청한 기존 Linux/CI 실행 환경에 대한 사용자 답변은 아직 없다.
- Git master/HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`, 전체 변경/미추적449경로(`--untracked-files=all`), staged0·추적 migration 편집0·diff check 지적0. 일반 status의 디렉터리 축약126행과 실제 파일449개를 구분한다. 커밋/푸시/배포·정책 게시/ENFORCE·기존 데이터 적용·서브에이전트·브라우저는 미실행이다. 운영 브라우저는 승인 범위이나 Linux/DB·배포 선행 Gate 미충족이다.
- 남은 필수 작업: 전체 공식 표본 기대값/catalog 채움, Provider QA 관리자 화면, 전체 분할 증거를 정책 verifier에 연결, 실제 Linux/PG/전체 Provider QA, exact-SHA 운영 배포/승인 범위 데이터 처리·역할별 운영 브라우저 E2E. 이번 연결 기능으로 전체 완료 기준을 축소하지 않는다. DB11.26/API24.30과 `announcement-attachment-provider-qa-management-2026-09-12.md`에 현재 계약을 기록했다.
- 11:17 최종 재확인: HEAD/449경로/staged0·추적 migration 변경0·diff check 지적0·ATT62행. Java0, 오늘 시작 Node0, 사용자 기존 Node7이며 모든 소유 검증 handle은 종료됐다. 새 관리/스케줄러/DTO/V80/테스트 범위의 제한 credential 패턴0이다. 전체 보안 감사나 실제 운영 정상 판정은 아니다.

### 2026-09-12 10:40 KST — 시스템 Provider QA catalog·전체 분할 계획과 snapshot6

- 직전 회차는 V79 실행 원장 연결·단위/DB 계약 시험 작성·로컬 전체 회귀를 완료한 진척이었다. 이번에는 AGENTS/장기 작업 스킬·Git·catalog/실행기/정책 snapshot/기존 공식 표본과 최신 환경을 읽고 다음 필수 구현을 진행했다. 전체 ATT001~062/Gate0~8과 최종 통과0·진행6/차단2/대기1·Not ready를 유지한다.
- Docker `version --format '{{.Server.Version}}'`은 Linux engine named pipe 부재로 실패했다. `wsl --list --verbose`는 docker-desktop/Stopped/WSL2다. `gh api repos/FrostyCityMan/saneB --jq '{permissions: .permissions}'`는 pull=true/push=false/admin=false였다. Docker 재시작/초기화/재설치·TLS/격리 완화 없이 확인만 했으며 사용할 수 있는 기존 Linux/CI 환경을 사용자에게 비차단 질문으로 요청했다. AWS/Actions/현재 운영 health·SHA·로그인은 이번 미조회다.
- `AttachmentProviderQaCatalog`와 고정 classpath JSON을 추가했다. unknown/중복 JSON·schema/version/개수·중복 source/case·URL/기관/parser 입력을 검증한다. 표본이 현재 대상 밖이거나 결합/profile/시각/기대값이 달라지면 상태를 남기고 실행 입력을 만들지 않는다. 모든 요구 target과 모든 catalog case를 유지하며 정상3공고·PDF/HWP/HWPX 요구를 줄일 수 없다.
- 앞선 공식 BBS 다운로드 표본9건은 식별자/정식 source URL만 참조 이관했다. expectation=null/REFERENCE_ONLY, 실행 입력0·예상 형식 coverage0이며 실제 최신 파일/추출 성공을 추정하지 않았다. production normalizer/profile과 정확한 source hash·host/게시판/기관/parser 결합을 오프라인으로 확인했다. 원본/첨부/목록을 이번에 다시 요청하지 않았다.
- `AttachmentProviderQaCaseContract`로 실행기의 기존 syntax/파일·품질/문구/420초·44회·80MiB/10파일 조건을 catalog와 공유했다. 현재 규칙에 따른 제목 제외 기대값 불일치, 동일 상세 URI의 중복 표본도 준비 불가다. 미확인 파일 수는null이며 한도 초과 선언은 실제 개수를 유지한다. OCR/부분/비지원 기대 동작을 정상3공고/완전 형식 coverage로 산입하지 않는다.
- 실행 가능 case는 정렬된 순서/전체 분모를 보존하여 항목 시간+60초 정리/DB 여유 합계가 분할당23시간 이내가 되도록 계획한다. 이는 V79의24시간 run/8분 case lease를 늘리거나 실제 성공을 보증하는 것이 아니다. 350합성 case의 분할은3개이며 중복/누락 없이 전체 case를 대조했다. 실제 운영 규모 성능은 미검증이다.
- 기존 `AttachmentPolicyValidationSnapshotFactory` schema6에 catalogVersion/catalogHash/scopeHash·전체 target/case metadata·분할별 case code/상한을 연결했다. URL/제목/기대 문구/Prepared.inputs는 snapshot/audit에 저장하지 않는다. 이전 schema1~5 이력은 수정하지 않으며 catalog/입력 변화 시 현재 snapshot과 달라진다. 전체 Provider 실제 verifier/예약 API/scheduler는 미완료여서 PROVIDER_PROFILES MISSING은 유지한다.
- 최초 표적 명령은20초에 compileTestJava 실패: 새 테스트가 기존 Target의5필드 생성자에 인자6개를 전달한 원인이다. 실제 계약에 맞춰 수정 후32초 성공, 중복 URI/축소 scope/안전 URL 보강 후30초 성공했다. 파일 수 미확인/한도 분모 보존까지 포함한 최종 catalog27건과 기존 실행기42·원장 Service35·snapshot/verifier 회귀가 통과했다.
- 전체 명령 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`은3분22초,22task 중10실행/12up-to-date 성공이다. XML root2089=1858통과+231조건부 생략/실패·error0, extractor25통과(동일 코드 기존 실행 up-to-date), 독립 QA 패키지14통과다. 이번에도 실제 PG/Linux 시험 생략을 통과로 표현하지 않는다.
- Node 동일7파일 전체 `node --test`는130통과/실패·생략0,프로세스 종료다. 독립 QA `--inventory`는205건 발견/통과0/미실행205,INVENTORY_ONLY이고 extractorRuntimeHash=null이다. 현재 업무 코드 지문은 `ff08ab30240a2953680127cf81274d3f32956d04583cc857cfae1830139a8cce`다.
- web JAR SHA256 `ba3ca5aacd426908d8ca1d0308beb10fabce97975014d2f6e13bd84fa2dcef47`, QA JAR SHA256 `c321050c170483720eb856ff24483d4c236b3dce55fa58f2d8181f3c72e37083`. 설치 패키지의 전체 업무 코드/리소스 지문과 inventory 포함을 검증했으며 운영에 설치하지 않았다. 상세는DB11.25/API24.29 및 `announcement-attachment-provider-qa-catalog-2026-09-12.md`다.
- master/HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`, 이번 시작430→435변경/미추적 경로,staged0·추적 migration 변경0·diff 지적0·ATT62행 유지다. 커밋/푸시/배포·운영 DB/정책/ENFORCE/기존 데이터 적용·브라우저·서브에이전트는 실행하지 않았다. 브라우저는 승인 범위지만 운영 선행 Gate 미충족으로 미실행이다. 실제 기대값/catalog 전체 채움·관리자 예약/조회·scheduler/정책 검증 연결 및 실제 Linux/PG/운영 전체 검증을 계속해야 한다.
- 10:41 최종 자원은 Java0·오늘 시작 Node0·사용자 기존 Node7이다. 이 회차의 Gradle/Node/inventory 명령은 모두 종료됐다. 사용자의 기존 프로세스는 종료하지 않았다. secret/실제 계정 비밀번호를 코드·문서·최종 응답에 추가하지 않았다.

### 2026-09-12 10:15 KST — Provider QA 원장·실행 Service 연결과 로컬 회귀

- 직전 회차는 전체 Gate 개수를 답한 상태 보고로 구현 진척 없음이었다. 현재 AGENTS·장기 목표 운영 스킬·원장 설계/코드·실행기·Mapper/테스트를 확인하고 작성 중인 V79 연결·검증을 진행했다. 전체 ATT001~062/Gate0~8, 최종 통과0·진행6/차단2/대기1·Not ready를 유지한다.
- V79는 현재 DRAFT 정책/규칙·전체 요구 범위·catalog/코드/runtime/입력 지문을 고정한 run/case 원장이다. 같은 transaction 안에서 전체 항목과 정확한 합산 예산을 봉인해야 한다. 이력/입력 불변·전역 단일 실행·8분 lease/24시간 run·소유권/취소/만료·완료 분모와 예산 대조를 구현했다. 기존 V1~V78은 이번에 수정하지 않았다.
- 내부 ExecutionService가 실제 CaseExecutor를 DB 원장 callback에 연결한다. HTTP/추출은 transaction 밖에서 수행하고 소유권/공유 다운로드2·호스트1·추출1/누적 예산·결과 저장만 짧게 잠근다. 기존 job/정책 QA도 같은 슬롯을 사용하며 만료 슬롯 인수 시 Provider 소유자 두 컬럼을 비운다. 다른 항목이 RUNNING이면 claim0으로 종료하며 unique constraint를 유지한다.
- Service는 기본 OFF, Controller/scheduler 미연결이다. 전체 공개 고정 표본 catalog·예약/취소/조회 API·정책 전체 QA verifier는 잔여다. run COMPLETED는 예약한 case의 기대 동작 일치일 뿐 전체 Provider/정책 PASSED나 운영 수집 시작이 아니다. PROVIDER_PROFILES MISSING을 유지하며 운영 정책/원문/공고를 변경하지 않는다.
- 최초 `compileJava --no-daemon --max-workers=1`은13초 성공. 첫 표적202건 중6건은 Mockito 재설정 단계에서 transaction assertion이 먼저 실행돼 실패했다. doReturn/doAnswer로 fixture만 수정하고 runtime transaction assertion은 유지했다. 수정 후 표적202통과/PG185조건부 생략,32초 성공. 내부 Service35건·실행기42건·Mapper35건·migration 정적90건이다.
- 실제 PostgreSQL14사례를 추가했다. 원자적 준비/예산 합계·불변 이력/전체 분모·독점 claim/실제 경합·case/run 예산·일반 worker/정책 QA 공유 슬롯·다른 owner 반환 차단·취소·DB 시각 만료·입력 변경·전체 완료/정책 분리를 검증하도록 작성했다. V78→V79/빈DB와 checksum 검증도 확장했지만 PG 실행은 생략됐으며 성공으로 보고하지 않는다.
- 전체 명령 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`은3분13초,22task 중6실행/16up-to-date로 성공했다. 최종 XML root2062=1831통과+231조건부 생략/실패·error0, extractor25통과(변경 없어 기존 실행 결과 up-to-date), 독립 QA 패키지14통과다.
- Node7파일 `node --test scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-recovery-ui.test.mjs scripts/qa/attachment-policy-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-batch-ui.test.mjs scripts/qa/attachment-backfill-ui.test.mjs`은130통과/실패·생략0,프로세스 종료다.
- 독립 QA `--inventory`는205건 발견/통과0/미실행205,INVENTORY_ONLY다. extractorRuntimeHash=null이며 실제 Linux/격리/DB 성공이 아니다. 전체 업무 코드 지문은 `5322a70aa457c9b4cc50b724d75c34eae78a559910272ed421f1f4e2c0182b57`이다.
- web JAR SHA256 `d8c1461ed220ccf845a04fc115217d2ba92063debecb57f59a101ef45adff30e`, QA JAR SHA256 `f7580a0919c8ccdefa9c3f7ce643095390482623bf277b4c4ee3b1110f78aff6`. 패키징 시험은 현재 모든 main class/기존·새 migration과 설치 bytes를 검증했다. 구체적 DB/API 계약은DB11.24/API24.28 및 `announcement-attachment-provider-qa-ledger-2026-09-12.md`다.
- 현재 master/HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`, 변경/미추적430경로,staged0. diff 검사 지적0,추적된 migration 편집0,ATT62행 유지. 이번 회차 GitHub/AWS/Actions/운영health/SHA/인증을 재조회하지 않았다. 이전 push 권한 없음/AWS 재인증 필요를 현재 성공으로 바꾸지 않는다.
- 커밋/푸시/배포/운영 DB·정책 게시/ENFORCE·기존 데이터 적용은 미실행이다. 운영 브라우저는 승인 범위이지만 배포·운영 연결 선행 Gate 미충족으로 이번 미실행이다. 서브에이전트 미사용. 10:18 최종 자원은 Java0·오늘 시작 Node0·사용자 기존 Node11이며 실행한 Gradle/Node/inventory는 모두 종료됐다. 기존 사용자 프로세스는 보존했다.

### 2026-09-12 09:40 KST — 고정 공고 전체 파일 QA 실행 단위

- 직전 회차는 Gate 개수의 상태 보고로 진척 없음이었다. 장기 목표 스킬·현재 Git·Provider 요구 계약·ATT62 QA 계획·production worker/transport/flow/temporary/runtime·기존 실사이트 시험을 확인하고 실행 단위를 구현했다. Gate0~8/ATT001~062 전체 범위와 최종 통과0·Not ready를 유지한다.
- `AttachmentProviderQaCase`와 `AttachmentProviderQaCaseExecutor`는 서버 고정 제목/규칙·출처/profile/runtime·전체 locator/binary 지문/형식/추출 기대값을 입력으로 받는다. 제목 제외는 상세/파일/runtime/임시 생성0이다. 목록 추가/누락/중복/형식 변경은 첫 파일 전에 중단하고 binary 교체는 해당 파일 추출 전에 거부한다. 일부 실패 후 다른 고정 파일 결과와 취소/제한 뒤 미실행 분모를 보존한다.
- 기존 pinned transport·4단계 flow·signature validator·Linux extractor·소유 임시 저장을 재사용한다. 호출자 `ExecutionControl`에 실제 소유 lease/공유 host·추출 permit/요청·byte 예약을 요구하며 기본 무제한 구현은 없다. 420초/44예약/80MiB/파일10개·상세1MiB/파일20MiB 상한과 작업 전35초 여유, 원본 정리·취소/현재 runtime 재확인을 적용한다. 원문/URL/파일명/예외 원문은 결과에 반환하지 않는다.
- 이 결과의 PASSED는 고정 표본 기대 동작 일치이지 전체 Provider/완전 추출/정책 게시 성공이 아니다. OCR/부분/비지원/빈 영역과 `allTextComplete`를 분리하고 `isPolicyQaPassed=false`를 유지한다. HTTP endpoint/scheduler/DB 분할 원장 연결·전체 공식 catalog·실제 Linux/실파일 실행은 아직 미구현 또는 미검증이다. 기존 정책 PROVIDER_PROFILES는 MISSING이며 정책 VERIFIED를 만들지 않는다. DB/API/migration/운영 설정 변경은 없다.
- 초기33건 중 직렬화 대역 설정2건, 다음42건 중 Mockito 중첩 stubbing1건이 실패했다. 테스트 설정을 수정한 표적42건은25초 성공했다. 추가 형식/원장 실패 보존을 포함한 전체 강제 회귀는3분39초/22task 성공, root2009=1792통과/217생략·실패/오류0, extractor25·독립QA14·Node130통과다. 이후 정리 중 취소/예상하지 않은 OCR 상태 보존2건을 추가한 최종 결과는 다음 항목과 같다.
- 최종 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --no-daemon --max-workers=1`(명령 한정 Windows-ROOT): **3분14초/22task 성공**, 변경 의존9task 실행·변경 없는 추출기 등13task up-to-date. 최종 root2011=**1794통과/217조건부 생략·실패/오류0**, 새 실행 단위42건 전부 통과, 추출기25·독립QA14·Node130통과다. Node와 변경 없는 추출기 결과는 직전 같은 코드 실행 결과이며 Linux/실파일로 바꾸지 않는다. web JAR SHA256 `874140d0f9946912e01f013f7a84f280a8ec4928e4490100c2012243907f88a0`, 독립 QA JAR SHA256 `5e2407ba0c3675d28a9dfeba155815c113dfdbca16db57d077379a7c9a34a656`이다.
- GitHub 실제 재조회는 pull=true/push=false/admin=false다. AWS는 이번 미조회로 기존 재인증 필요 기록을 성공으로 바꾸지 않는다. 시작419→422변경 경로이며 새 코드2/테스트1을 추가했다. staged0·추적 migration 변경0·ATT62행을 보존했고 커밋/푸시/운영 변경·브라우저·서브에이전트는 실행하지 않았다. 다음 필수 작업은 전체 Provider 고정 표본 catalog/분할 실행 원장과 실제 Linux/PG 검증이다.
- 최종 HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24` 유지·diff check0이다. 새3파일의 제한 credential 패턴0이며 전체 보안 감사를 뜻하지 않는다. 모든 Gradle/Node 실행 handle이 종료됐고 Java0, 새 Node0이다. 기존 사용자 Node11개(가장 최근 시작09-11 23:28)는 보존했다. 브라우저 승인은 있으나 실제 DB/운영 배포 선행 조건 미충족으로 이번 실행하지 않았다.

### 2026-09-12 09:13 KST — 전체 Provider QA 요구 범위·미지원 사유 조회

- 직전 회차는 WORKER_DB_RECOVERY 부모 실행/취소/정리와 게시 verifier 연결·최종 전체 회귀를 완료한 진척이다. 이번에는 AGENTS/장기 목표 스킬·실제 QA 계획/정책8분 lease·전체 프로필/Service/Mapper/API/테스트를 확인하고 Provider 요구 목록/조회 경로를 구현했다. 전체 ATT62/Gate0~8 최종 통과0·Not ready를 유지한다.
- 시스템12프로필은 provider·기관 코드·목록 parser 결합을 선언한다. `AttachmentProviderQaPlan`은 기업마당/정부24와 현재 활성 미삭제 지자체 전체를 대조하며 같은 parser의 다른 기관까지 지원한다고 추정하지 않는다. 미구현/파서 불일치/중복/단순 결합·최소 공고 요구량을 구분하고 공식 URL/표본/형식 실증은 별도 미완료로 유지한다.
- `GET .../{policyId}/provider-qa-plan`은 활성 ADMIN/OPERATOR/APPROVER 전용·no-store·ApiResponse/PageResponse·size1~100이다. runtime/QA worker ON 없이 현재 DB 범위를 읽고 외부 요청/QA 예약/정책 쓰기0이다. sourceId는 지자체 수집원 ID이며 원문 URL/설정 JSON/비밀값은 반환하지 않는다. 내부 snapshot schema5에 전체 요구 목록을 고정하고 PROVIDER_PROFILES MISSING 증거에 같은 목록 hash/요약을 저장한다. 기존 migration과 기존 API shape는 변경하지 않았다.
- 전체 Provider 실파일을 기존 단일8분 lease 안에서 모두 성공시킬 수 있다고 보장하지 않는다. 분모 축소/상한 자동 증가 대신 서버 소유 고정 표본·분할 실행·근거 원장이 후속 필수이며 이번에 구현되지 않았다. 실제 표본/요청/byte 계획을 만든 뒤 초과 운영 요청은 정확한 범위 승인을 받는다. 상세 계약·미구현 경계는 `announcement-attachment-provider-qa-scope-2026-09-12.md`다.
- 표적145건·QA14건·bootJar는1분4초 성공. 제한된 실제 Spring 프로필 등록 컨텍스트에서12프로필/11개 지자체+기업마당 결합과 정부24 미구현을 확인했다. 이는 현재 운영 전체 수집원 수/동작의 증거가 아니다. HTTP 테스트는 실제 보안 필터/Controller와 Service 대역이며 운영 API 호출은 아니다.
- 최종 전체 강제 회귀 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --max-workers=1`(명령 한정 Windows-ROOT): **3분41초/22task 성공**. root1969건=1752통과/217생략·실패/오류0, extractor25·QA14·Node7파일130통과다. 기존 MockBean/unchecked/Log4j provider 경고는 유지된다.
- 독립 inventory191건 발견/실행0/runtimeHash=null. artifactHash `b500fbeb9e55dad018966e60adaf69ecc6640a9bcf6ba6a332f23be84f1a2b5b`, 업무 코드 지문 `c7c300c3412d88362c47cba18b16c865282b96cfd9d256cc334cd4beabb7ea7e`, web JAR SHA256 `18633168acc720ba8b1db40fb8822f0f6d947ef3d059033cc13809e5fb3fc0a0`다. 공통 profile interface/구현에 결합 선언을 추가했으므로 과거 다운로드 결과를 새 profile hash의 운영 성공 근거로 재사용하지 않는다.
- 시작414경로에서 새 코드/DTO/Controller/테스트/문서5개를 추가해419경로다. HEAD ae893b87348a9bd1cb0893763f6cad5047093a24, staged0·추적 migration 변경0·ATT62행 유지다. 이번 회차 GitHub/AWS 재조회·실사이트 호출·Linux/PG·커밋/푸시/배포·정책/ENFORCE/기존 데이터 변경·브라우저는 미실행이다. 직전 GitHub CLI/Git dry-run 쓰기 권한 실패 및 AWS 재인증 필요 기록을 성공으로 바꾸지 않는다. 다음은 전체 Provider 분할 실행 원장과 실제 Linux 검증이다.

### 2026-09-12 08:51 KST — 정책 DB QA 부모 실행·취소·게시 검증 연결

- 직전 회차는 Gate 상태 보고이며 새 구현 진척은 없었다. 이전 빌드 종료와 root1717통과/217생략·extractor25·QA14 결과만 회수했다. 이번에는 AGENTS/장기 목표 스킬과 실제 snapshot/Service/Mapper/독립 실행기/테스트/CI를 읽고 WORKER_DB_RECOVERY의 하드코딩 MISSING을 실제 부모 실행 경로로 교체했다. 전체 ATT62/Gate0~8 목표와 최종 통과0·Not ready는 유지한다.
- 부모가 prlimit/bwrap와 임시 작업 폴더를 직접 소유한다. 외부 환경/운영 DB/네트워크/임의 클래스 입력 없이 고정 네 suite를 실행하고, stdout1MiB·전용 실행1개·취소/시간 제한·namespace 종료·임시 DB 정리를 검증한다. 기존8분 lease에서60초를 완료 재검증/정리에 남기며 원래 자원 상한을 늘리지 않는다. Windows fallback/보안 해제는 없다.
- snapshot 내부 schema4는 publicCode 외에 QA artifact·전체 suite/case·업무 코드/추출기 identity를 고정한다. 동일 웹 코드와 QA 설치가 없으면 예약을 거부한다. WORKER_DB_RECOVERY 근거는 실제 자식의 전체 사례 통과·정리·현재 지문이 맞을 때만 저장하고 게시 verifier도 같은 조건을 사용한다. JSONB 필드 순서 정규화와 evidence 생성/저장 시각 경계를 추가했다. 기존 migration/HTTP shape/운영 데이터는 변경하지 않았다.
- PROVIDER_PROFILES는 여전히 MISSING이며 전체 VERIFIED·정책 게시 성공을 만들지 않는다. 실제 Linux 연결2사례(전체 PG/worker 실행·게시 검증 및 취소/DB 정리)를 전용 `attachmentPolicyDbQaIntegrationTest`와 CI의 필수 XML 판정에 연결했지만 현재 PC에서는 실행하지 못했다. 독립191건은 발견 목록이며 실제 통과0 경계를 유지한다.
- 표적72건·독립 QA14건·bootJar는48초 성공했다. 첫 문법 오류와 Mockito 재설정 오류2건은 수정 후 재검증했다. JSONB/저장 시각 후속 수정 전 전체 회귀는3분16초/22task, root1955건=1738통과/217생략·extractor25·QA14·Node130통과다. 후속 수정의 최종 결과는 아래에 별도로 기록한다.
- GitHub 실조회는 pull=true/push=false/admin=false다. HEAD와 원격 master는 ae893b87348a9bd1cb0893763f6cad5047093a24 그대로이며 시작409경로에서 새 실행기/테스트5파일 추가로414변경 경로다. staged0·추적 migration 변경0을 확인했다. AWS는 이번 회차 미조회이며 직전 AUTH_REFRESH_REQUIRED 기록을 현재 인증 성공으로 대체하지 않는다.
- Git 경로도 프롬프트/상호작용을 비활성화한 `git -c http.sslBackend=schannel push --dry-run origin HEAD:master`로 확인했으며 인증/쓰기 권한 필요로 실패했다. 실제 push나 ref 변경은 없고 토큰/오류 원문은 기록하지 않았다.
- Node 전체 재검증에서 기존 정책 화면 테스트1건이 실패했다. 잘못된 만료 시각 fixture가 고정 `2026-09-12T00:00:00Z`를 사용하여 현재 시각이 접근하면서15분 유효 범위에 들어온 것이 원인이다. 만료가 생성보다 먼저임을 확실히 만드는 상대 시각으로 테스트만 수정한 뒤7파일130건 통과했다. 화면 코드는 변경하지 않았다.
- 최종 전체 회귀 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --max-workers=1`(명령 한정 Windows-ROOT): **3분13초/22task 성공**. root1957건=1740통과/217생략·실패/오류0, extractor25·QA14통과다. 저장 시각 인자 추가 후 테스트 대역2건의 실패를 수정하고 전체를 재실행한 결과다. 기존 MockBean/unchecked/Log4j provider 경고는 유지된다.
- 최종 독립 inventory는191건 발견·passed0/notRun191·runtimeHash=null, artifactHash `d9a8b5072e2fcffccc26a443c9257933bc95e605d60189a65020996989380513`다. 업무 코드 지문 `c015b48940aa2402ec40173ee75062ea5c8f536ee81d1323db3e3fe707695816`, web JAR SHA256 `c3f4af154a3ebb4e8d5e176ec35cfb13b2e706c50b3e58750f5ffc93af4c56e2`다. 패키징 테스트는 실제 web/QA class/resource bytes를 대조했다. Linux 실행/운영 배포 증거가 아니다.
- 커밋/푸시/배포/정책 게시·ENFORCE/기존 데이터 적용은 실행하지 않았다. 브라우저는 승인 범위지만 운영 선행 Gate 미충족으로 이번 미실행이다. 서브에이전트도 사용하지 않았다. 세부 계약은 `announcement-attachment-policy-qa-bridge-2026-09-12.md`다.

### 2026-09-12 08:12 KST — 태백·횡성·영월 BBS 첨부 프로필 구현·실제 다운로드

- 직전 회차는 Gate 개수 상태 보고로 진척 없음이었다. 이번에는 V61/V62 및 세 기관 공식 목록/상세를 대조하고 표준 BBS 프로필3개와 worker의 기관 한정 헤더 검증 경로를 구현했다. 등록12개이며 다른 SPRING_BBS 기관을 자동 지원하지 않는다. 전체 목표/ATT62/Gate0~8 최종 통과0·Not ready를 유지한다.
- 기존 URL identity를 확인한 뒤 태백 HTTP는 같은 HTTPS 경로로만 승격하고 횡성 익명 세션 경로를 요청/locator에서 제외한다. 정확한 파일 영역·제목/본문 구조·게시판/기관·파일/미리보기 경로를 검증한다. 파일명으로 역할을 지정하지 않고 UNKNOWN을 유지하며 미지원·누락·상한을 숨기지 않는다. DB/API/화면·마이그레이션 변경은 없다.
- 실제 태백 x-msdownload 및 횡성/영월 octer-stream MIME 오기와 UTF-8 header octet 문제를 확인했다. 최초29초9실패/MIME, 다음37초3통과6실패/파일명 헤더를 보존한다. 해당 시스템 profile에서만 명시적 형식/signature/attachment filename 일치를 요구하는 호환 경로를 적용했고, HTML/실행 파일·미승인 MIME·경로·제어문자·확장자 오류 거부를 시험했다.
- 표적 profile/validator/worker62건과 새 실사이트9건은32초 통과했다. 전체 첫 강제 실행3분36초/23task는 root1712통과217생략·추출기25·독립QA13·실사이트22통과였다. 이후 확장자 없는 형식명을 승인하지 않는3개 회귀를 추가한 최종 실행은 아래 결과로 구분한다. 이전 산출물을 최종 코드 증거로 대체하지 않는다.
- 새9건의 전체 첨부14개(PDF2/HWPX12)1,499,273byte/23HTTP를 확인했다. 공개 지원사업 공고/양식으로 고정했고 합격자/주소 명단을 내려받지 않았다. 원본 정리9/9, Windows 원문 추출/실제 DB/운영 활성화는 하지 않았다. HWP·빈 첨부/비지원 혼합 실사이트·전체 기관 범위는 여전히 미확인이다.
- 이번 회차 GitHub pull=true/push=false/admin=false, 공개 CA/TLS를 유지한 AWS AUTH_REFRESH_REQUIRED를 재확인했다. 기존 반복 Docker/Windows PG 실패는 설정 변화 없이 재시도하지 않았다. 실제 Linux/PG·Provider/worker QA 실행/근거 연결·정확한 승인 범위 적용·최신 배포·운영 브라우저는 남는다. 커밋·푸시·배포·정책 게시/ENFORCE·기존 데이터 적용·브라우저·서브에이전트는 미실행이다.
- 상세 계약·실측 URL·표본·검증 경계는 `announcement-attachment-standard-bbs-profiles-2026-09-12.md`다. 장기 작업 운영 스킬에 따라 이 증분을 전체 Gate 완료로 표시하지 않는다.
- 최종 전체 강제 실행 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa attachmentProfileDiscoveryQa --rerun-tasks --no-daemon --max-workers=1` 및 명령 한정 Windows-ROOT 설정: **3분35초/23task 실행, 실사이트1건으로 명령 실패**다. root205suite/1932건=1715통과217생략·실패/오류0, extractor25·독립QA13·Node7파일130통과. bootJar/installDist/독립 설치는 실행 성공했다. 기존 MockBean/unchecked/Log4j provider 경고는 남는다.
- 실사이트22건 중21통과, 강북184760의 두 번째 파일 BRIDGE_GET에서 HTTP400/요청5회·원본 정리true였다. 코드상 최초 GET query는 고정 검증·재인코딩하며 이 HTTP400의 원인은 확정하지 못했다. 동일 코드/표본으로 법정 게시판7건을 한 번 재검증하여24초·7통과를 확인했다. 앞선 실패를 성공으로 바꾸지 않고 간헐 실패 미해결을 유지한다. 새 BBS9건은 최종 코드로 전부 통과했다.
- 코드 목록1442파일 SHA256 `8cf1557c39b33f5f0b28e92ce7b7b1266df2307efac7ff8476ecb0d48ea28d19`, 웹 JAR SHA256 `01a9c95c764cd0c6bc47ac99942c3d7ebf92f90858c9910f5917e92cbe277152`. 변경 주요5클래스의 JAR/디스크 바이트가 일치하고 전체 QA 패키징 시험이 통과했다. 독립 QA artifact hash `5075e4afe4219c02359d9038f8d373c5320bb3b8ed07023e6f66144d4eab8b42`, job168/migration2/backfill13/worker8=191건은 발견만 했으며 passed0이다.
- 최종 Git: master/HEAD와 원격 master `ae893b87348a9bd1cb0893763f6cad5047093a24` 일치, 기존403→408개 변경/미추적·staged0·추적 migration diff0. 이번 신규파일5개 이외 기존 사용자 변경을 되돌리거나 삭제하지 않았다. `git diff --check`와 ATT62행/고유ID62 확인을 수행했다. 자원/제한된 비밀정보 패턴 최종 확인은 아래 보충을 따른다.
- 최종 보충: 이번13개 변경 파일의 한정 credential 패턴 지적0, profile27/validator8/worker30건의 XML 확인, diff 검사0, Java0·이번 회차 신규 Node0을 확인했다. 실행한 모든 Gradle/Node/독립 inventory handle은 종료됐다. 기존 사용자/앱 Node 프로세스는 종료하지 않았다. 이는 제한된 패턴 검사이지 전체 보안 감사가 아니다.

### 2026-09-12 07:36 KST — 정부24 출처 코드 누락·저장 제약·UI 계약 수정

- 직전 회차는 Gate 개수만 확인한 상태 보고로 **진척 없음**이다. 이번에는 실제 Provider·원문·배치/전체 목록·DB 제약·정책 registry·QA snapshot을 대조하고 계약 불일치를 수정했다. 원래 목표/ATT62/Gate0~8은 유지하고 최종 통과0·Not ready다.
- 기존 실제 코드는 `GOV24_PUBLIC_SERVICE`인데 배치/전체 목록 필터가 `GOV24`를 원문 컬럼과 직접 비교했고 V73/V74 첨부 제약도 실제 코드를 거부했다. V78에서 두 CHECK를 확장하고 Mapper 필터 바인딩·집계 API 경계를 맞췄다. 후보/작업/고정 항목·profile은 실제 코드를 유지하며 과거 필터·정규화·멱등 요청과 고정 이력은 변경하지 않는다. 정부24 profile 미구현을 0건으로 숨기지 않고 PROFILE_REQUIRED로 반환한다.
- 정책 registry/QA 대상의 실제 코드와 화면 응답 검증·한글 표기도 수정했다. UI/UX 운영 원칙에 따라 내부 코드 차이를 사용자에게 전가하지 않으며 기존 역할·동의·오류/입력 복구·레이아웃을 유지했다. 가짜 정부24 profile은 테스트 내부에서만 사용한다. 정부24 실제 첨부 제공 방식을 확인하거나 지원 profile을 구현한 것은 아니다.
- 첫 표적 실행30초·213건 중 새 SQL 검사1건 실패는 주석 LIMIT 오탐이었다. 주석을 제외한 실행 SQL을 검사하도록 수정 후 같은 표적25초·213건 전부 통과했다. 전체 강제 실행 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --max-workers=1`과 Windows-ROOT 명령 한정 설정은 **3분11초·22작업 실행 성공**이다. root1900건=1684통과/216조건부 생략/실패0·extractor25·QA13·Node7파일130통과다. 기존 MockBean/unchecked/Log4j provider 경고는 유지한다.
- 독립 목록191건(job168/migration2/backfill13/worker8)은 INVENTORY_ONLY/passed0, artifact hash `60c830252c391cfc80b70dd13a037d8cf863f2f6b81366ab8e830186fa529e23`다. V77→V78·fresh DB·정부24 materialize/미지원 profile 예약 차단 시험은 작성/패키징했으나 실제 PG 성공은 아니다.
- 코드 목록1440파일/8,670,543byte, catalog SHA256 `02777cac238ce196fdbef81304a15cb3decbf1766bd4bfbc6211285774f8e465`, 웹 JAR SHA256 `adbcf938672b2be359486f7b01430417c01d6587bd8f11bc7d49844e41b1f925`다. 웹/독립 QA의 목록·바이트 검사가 통과했지만 동일 SHA 운영 증거는 없다.
- V1~V77 기존74개 파일의 작업 전후 묶음 해시가 같다. master/HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`, 변경/미추적400→403개, staged0·추적 migration diff0이다. Git status의 기본 디렉터리 축약109행은 파일 수로 사용하지 않았다. 새 V78만 추가했고 기존 사용자 파일·개인 메모리를 삭제/되돌리지 않았다.
- GitHub 쓰기 권한 없음·AWS 재인증 필요를 다시 확인하고 사용자에게 안전한 로컬 재로그인을 요청했다. 같은 Docker 기동 실패는 재시도하지 않았다. Linux/PG·전체 profile/실파일·PROVIDER_PROFILES/WORKER_DB_RECOVERY 실제 실행/검증 연결·정확한 승인 범위 적용·최신 배포/운영 브라우저는 계속 미완료다. 커밋·푸시·운영 변경·브라우저·서브에이전트는 미실행이며 Gradle handle 종료·Java0·작업 QA Node0을 확인했다.
- 상세 계약·검증 경계: `announcement-attachment-gov24-provider-contract-2026-09-12.md`, DB11.23/API24.27. 이번 수정만으로 전체 목표 또는 정부24 지원을 완료 처리하지 않는다.
- 최종 확인: 이번 변경27파일의 고위험 자격증명 패턴0, ATT62행/고유ID62, `git diff --check` 종료0이다. 한정한 패턴 검사이며 전체 보안 감사가 아니다.

### 2026-09-12 07:08 KST — 전체 코드 지문·게시 검증 transaction 경계 수정

- 직전 회차는 worker8사례/독립 QA 패키징·로컬 회귀를 보강한 **진척**이다. 이번에는 root AGENTS·장기 목표 스킬·현재 정책 QA/게시/지문 및 접근 상태를 확인했다. GitHub는 pull=true/push=false/admin=false, AWS는 공개 CA/TLS 검증을 유지한 상태에서 재인증 필요였다. 같은 Docker 실패를 재시도하지 않았다. 전체 목표/ATT62/Gate0~8은 유지하며 최종 통과0·Not ready다.
- 수동 코드 지문 목록이 worker·저장 서비스·다른 Mapper 변경을 놓칠 수 있는 결함을 확인했다. `generateAttachmentQaCodeCatalog`와 `AttachmentApplicationCodeFingerprint`를 추가하여 현재 main class/resource 전부를 열거하고 설치 classpath 바이트의 크기/SHA를 대조한다. QA 내부 입력 schemaVersion2·executionCodeHash를 transaction 밖에서 계산해 고정한다. 상세와 제한은 `announcement-attachment-code-fingerprint-2026-09-12.md`다.
- 기존 게시의 마지막 파일/fixture 확인이 쓰기 transaction 안에 있던 경로를 분리했다. 짧은 읽기 준비 → transaction 밖 QA/설치 재검증 → NOWAIT 잠금 뒤 정책/scope/최신 QA/단계/전체 snapshot 동등성 재확인 → 기존 원자 게시 순서다. 선행 동일 요청이 준비 중 성공한 경합은 원래 영수증으로 복구한다. 역할/CSRF/승인/멱등성과 기존 source/job 비변경 정책은 유지한다. 실제 PG 경합 검증을 완료한 것은 아니다.
- 초기 표적 명령은 새 테스트의 IntStream `map` 형식 오류로17초 실패했다. `mapToObj`로 수정한 뒤 지문/QA snapshot/게시/검증기 표적 명령25초 성공을 확인했다. 실패를 성공으로 재기록하지 않는다.
- 최종 전체 강제 실행 `.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는 **3분11초·22작업 전부 실행 성공**이다. root203suite/1893건=1678통과/215조건부 생략·실패/오류0, extractor25·QA13·Node7파일129통과다. 새 코드 지문14·snapshot2·게시4 및 전체 패키징1 검증을 추가했다. 기존 MockBean/unchecked/Log4j provider 경고는 유지한다.
- 생성 목록1438파일/8,667,188바이트를 실제 classpath에서 검증했고, 웹 JAR와 독립 QA JAR 각각의 전체 목록 및 각 바이트/SHA가 같은 것을 검사했다. code catalog SHA256 `c38b0eaaed20e0e4105775e020c695516b0888b0e1912a4eb6a36ac917e2e9b4`, 웹 JAR SHA256 `b5811f084e84aa505ada02727c1310718d01abbf00ff926f890e46dd1be0a2e9`다. 외부 framework JAR 전체의 실행 검증이나 동일 SHA 운영 증거는 아니다.
- 독립 QA artifact hash `1e3328139713872a87a8b5d2c78ba5e190d3590beee31570604df22994b79591`, 목록190건은 INVENTORY_ONLY/passed0이다. Provider/worker DB 두 추가 검증기의 실제 실행·검증 연결, Linux/PG·전체 profile/실파일, 정확한 승인 범위 적용, 동일 SHA 배포·운영 역할 브라우저 E2E는 남는다. 임의 JSON 또는 코드 지문만으로 VERIFIED/게시 성공을 만들지 않았다.
- 이번에 migration·운영 설정/데이터·개인 메모리·사용자 기존 프로세스를 변경하지 않았고 커밋·푸시·배포·브라우저·서브에이전트는 미실행이다. 시작 HEAD/master `ae893b87348a9bd1cb0893763f6cad5047093a24`, 시작397→현재400개 변경/미추적이다. 최종 Git·자원·민감정보 검사는 보충 기록을 따른다.
- 최종 보충: Mapper만 바뀐 경우도 이전 목록 거부·새 지문 변경을 직접 assertion하도록 보강했다. 이후 `:test --no-daemon --max-workers=1` 전체 재실행은2분33초 성공(1893건=1678통과/215생략)이며 업무 코드·웹/QA 산출물 지문은 위 최종 빌드와 같다. 선택17파일의 고위험 자격증명 패턴0·ATT62행/고유ID62·diff 검사0을 확인했다. 범위를 한정한 검사이며 전체 보안 감사가 아니다.
- 자원/최종 산출물 확인: 모든 Gradle handle 종료·Java0·작업 QA Node0, 웹 JAR의 독립 QA 실행기/제한한 테스트·parser 라이브러리 혼입0이다. HEAD 유지·staged0·추적 migration diff0이며 사용자 소유 프로세스는 종료하지 않았다.

### 2026-09-12 06:48 KST — 실제 worker·격리 추출·DB 통합 사례와 패키징

- 직전 회차는 Gate 개수만 답한 **진척 없는 상태 보고**다. 이번에는 root AGENTS·장기 목표 스킬·실제 worker/transaction/Mapper/추출/QA 경로를 확인하고 남아 있던 테스트 실행의 종료·결과를 회수했다. 이어 8개 worker 통합 사례·실행 경로와 패키징 검증을 보강했다. 전체 ATT001~062/Gate0~8 범위를 유지하며 최종 통과0·Not ready다.
- `AnnouncementAttachmentWorkerIntegrationTest`는 HTTP만 신뢰한 합성 HTML/바이너리로 대체한다. 실제 worker/gateway/추출기/임시 PostgreSQL/Flyway/MyBatis/조회·검수·역할 서비스를 연결한다. 정상 3형식+비지원, OCR/부분/암호/404, checkpoint 재시작, 추출 중 원문 삭제, 역할 변경·검수 무효화·DRAFT 멱등성, 본문 FETCH_FAILED 보존, 예약 후 제외의 요청0, COLLECT_ONLY의 기본 판정/운영 공고 비변경을 작성했다. 실제 원문 본문 수집기나 운영 인증/정책 게시를 실행한 것은 아니다.
- 테스트 전용 DB의 사례 간 대기 작업 오염을 제거했다. 제외 후 작업 미시작은 임시 표식도 생성하지 않으므로 정리 assertion을 별도로 수정했다. 나머지 처리 경로의 표식2개·임시 원본0·자원 lease0 조건은 유지한다. 이 8건은 Linux/PG **미실행**이며 일반 root task에서 생략한다.
- 고정 네 suite·추출기 installDist·생성 입력을 독립 산출물에 포함하고 scope를 `SYNTHETIC_WORKER_DB_CONTRACTS_V2`로 구분했다. 실행기 내부 `/qa/extractor` 지정·상속 경로 거부·전체 artifact hash, 현재 테스트 class/추출기/fixture 바이트 비교를 추가했다. CI의 전용 worker task/XML을 필수 판정 대상에 포함했다. 실제 원격 workflow를 실행한 것은 아니다.
- 표적 빌드27초 성공. 전체 강제 실행 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --max-workers=1`는 **3분8초·21작업 실행 성공**이다. root202suite/1873건=1658통과/215조건부 생략, extractor25, QA12, Node7파일129건 통과/실패0이다. 이후 조건부 테스트의 임시 정리 assertion과 패키징 바이트 검사를 보강하여 `compileTestJava attachmentContractQaTest installAttachmentContractQa`25초 성공·QA12재통과를 확인했다. 최종 조건부 테스트를 Linux에서 실행한 것으로 확대하지 않는다. 기존 MockBean/unchecked/Log4j provider 경고는 유지한다.
- 최종 독립 목록190건(job168/migration2/backfill12/worker8)은 `INVENTORY_ONLY`·passed0·notRun190이다. QA artifact hash `03bfd22a7b7510e6365e0b7d4f666279a3050e57819adb9318276a9accea94f7`, 웹 JAR SHA256 `588fd7b5bd31419eb4b87802725418403c3f8eede70a8c6cb282b9aaae5c91c9`. 실제 운영 설치 지문이 아니다. Bash 문법 검사도 종료0이다.
- WSL 목록은 docker-desktop만 확인했다. 이전에 실패한 Docker 기동/AWS 인증을 같은 조건으로 재시도하지 않았다. Linux/PG·전체 profile·실파일·실제 정책 QA verifier/게시·승인 범위 기존 데이터 실행·정확한 SHA 배포·운영 역할별 브라우저 E2E는 남는다. 이번에는 main 업무 코드/migration/API/운영 데이터 변경, 커밋·푸시·배포·브라우저·서브에이전트를 실행하지 않았다.
- 현재 master/HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`, 변경·미추적397파일/staged0/추적 migration diff0이다. 이번 시작397파일을 보존했다. 세 Gradle handle 종료와 최종 Java0을 확인했으며 Node 검사도 종료됐다. 최종 diff/민감정보/산출물 확인은 아래 보충 기록을 따른다.
- 다음 필수 작업은 **실제 Linux/PG에서 이 검증을 실행하고 실패를 수정하는 것**, 전체 수집원 profile 및 운영 정책 QA의 실제 실행·검증 연결이다. 합성 테스트 수를 늘리는 것으로 이 실행 Gate를 대신하지 않는다. GitHub 쓰기/Actions 실행 또는 승인된 Linux 접속 상태의 변경이 필요하며, 기존 운영 정책/ENFORCE·데이터 적용은 여전히 정확한 범위 승인 후에만 수행한다.
- 최종 보충: ATT62행/고유ID62·diff 검사0, 선택15파일의 고위험 자격증명 패턴0건. 웹 JAR의 독립 QA 실행기/JUnit/embedded PostgreSQL/PDFBox/POI/hwplib 혼입0을 확인했다. 전체 보안 감사는 아니다. Java0·작업 QA Node0이며 사용자 기존 프로세스는 종료하지 않았다.

### 2026-09-12 06:20 KST — 독립 DB QA 산출물·실행 판정·CI 연결

- 직전 회차는 Gate 개수만 확인한 **진척 없는 상태 보고**다. 이번에는 root AGENTS·장기 목표 스킬·실제 QA 계획/정책 validation/테스트/CI와 실행 환경을 확인하고 Gate6의 별도 검증 산출물을 구현했다. 전체 ATT001~062/Gate0~8 범위를 유지하며 최종 통과0·Not ready다.
- `installAttachmentContractQa`는 현재 main code/resource와 고정 DB suite3개·기존 test 의존성을 별도 JAR/lib에 묶는다. 웹 bootJar의 QA runner/JUnit/embedded PostgreSQL 포함0을 확인했다. 소스 checkout·운영 접속값·임의 클래스/URL 없이 Linux 비root/private namespace에서 실행하도록 구성했다. 상세는 `announcement-attachment-contract-runtime-2026-09-12.md`다.
- 실제 Launcher가 발견한 현행 테스트는 job168·migration2·전체 분할12=182다. **INVENTORY_ONLY, passed0/notRun182**이며 실제 PostgreSQL 통과가 아니다. 일반 실행의 Windows 거부 LINUX_REQUIRED/exit1도 확인했다. 실패·생략·중단·초기화 실패·누락·중복 완료·원문 출력 방지와 현재 main class/migration 패키징 동일성 등 자체11건 통과다.
- CI 수동 QA에서 누락됐던 정책 화면 Node 테스트를 추가했고, 독립 QA 자체 검증/설치/실제 Linux 스크립트 실행을 연결했다. contents:read·운영 secret/배포 부재·worker1과 기존 필수 보고서 판정을 유지한다. workflow는 원격 미반영·미실행이며 static test5건 성공을 Actions 성공으로 표현하지 않는다.
- 중간 실패: 의도적 실패 fixture의 독립 탐색2실패는 상위 테스트가 직접 실행·검증하는 내부 fixture로만 제한해 수정했다. 잘못 지정한 무접두 `test --tests`가 extractor 하위 task에도 필터를 적용한 명령 실패는 root `:test`로 정정했다. 최초 QA JAR CopySpec 범위 오류로 main/runner가 누락된 문제를 수정하고 실제 패키지·전체 class/migration 동일성 회귀2건을 추가했다. assertion 삭제나 DB 생략의 성공 처리는 없다.
- 전체 강제 실행 `:test :attachment-extractor:test attachmentContractQaTest bootJar :attachment-extractor:installDist installAttachmentContractQa --rerun-tasks --no-daemon --max-workers=1`는 **3분7초·21작업 실행 성공**이다. root1863건=1656통과/207조건부 생략, extractor25, 별도 QA11, Node7파일129건 통과/실패0. 후속 QA 최종 코드 강제 재빌드42초·LF 계약 최종 자체검증15초도 성공했다. 기존 MockBean/unchecked/Log4j provider 경고는 유지한다.
- 최종 QA 전체 artifact hash `3bef217b7d15f83a6b01ddffc224d35cc0e593326af9d2c715bf4b4b84449ac8`, 웹 JAR SHA256 `a65cc800d3dbea1c9581d37e2756ddc744417b620f84a0693f26fb046c32a29d`. Bash 문법 검사 및 새 스크립트의 LF 지정/산출물 CR 부재를 확인했다. 운영 설치 지문은 아니다.
- 환경 재확인: Docker service/배포판 중지·Linux engine pipe 부재에서 정상 기동을 한 번 시도했으나 응답이 없고 파일 접근 오류가 기록됐다. 이번에 생성한 PID/시각이 일치하는 Docker 자원만 정리했고 배포판 Stopped를 확인했다. 보안 완화/초기화/재설치는 하지 않았다. 공개 CA bundle을 명령 범위에서 사용한 AWS 조회는 AUTH_REFRESH_REQUIRED, 현재 GitHub 권한은 pull=true/push=false다. 같은 initdb/재배포를 반복하지 않았다.
- 최종 master/HEAD/원격 master `ae893b87348a9bd1cb0893763f6cad5047093a24`, 시작386→395개 변경·미추적/staged0/추적 migration 변경0이다. 이번에는 migration/API/운영 설정/데이터 변경, 커밋·푸시·배포·실파일 재요청·브라우저·서브에이전트를 실행하지 않았다. 운영 브라우저는 승인돼 있으나 선행 배포/접근 미충족으로 미검증이다.
- 다음 필수 작업: 독립 산출물의 실제 Linux DB/정리 검증, 실제 worker/추출/DB 복구 사례, 전체 Provider profile·실파일과 두 추가 정책 QA 실행/검증 연결이다. **현재8분 정책 QA lease에10분 runner를 그대로 연결하거나 DB 계약 CLI 결과를 전체 WORKER_DB_RECOVERY 통과로 수입하지 않았다.** 정확한 SHA 배포·범위 승인 후 기존 데이터 전체 처리·운영 역할 브라우저까지 원래 목표를 유지한다.
- 06:24 최종 점검: ATT62행/고유ID62, diff 검사0, 선택한15파일의 고위험 자격증명 패턴0건. 전체 보안 감사로 확대하지 않는다. Java0·이번 작업 QA Node0·Docker0이며 실행한 모든 Gradle/Docker handle의 종료를 확인했다. 사용자 기존 Node 프로세스는 종료하지 않았다.

### 2026-09-12 05:54 KST — 부산·강북 프로필·고정 다운로드 flow·실파일 검증

- 직전 회차는 Gate 수만 보고한 진척 없는 상태 조회였다. 이번에는 AGENTS/장기 목표 스킬·실제 프로필/worker/QA 계획을 확인하고 Gate 2 구현을 진행했다. 전체 QA 실행기 미연결과 전체 수집원 대비 프로필 부족을 구분했으며, 작은 표본 성공으로 전체 목표를 대체하지 않는다. Gate0~8 최종 통과0·Not ready 유지다.
- 실제 부산/강북 목록·상세를 조사하고 source/host/path/query/DOM을 고정한 두 프로필을 추가했다. 등록9개이며 기존 목록 SPRING_BBS 전체를 승인한 것이 아니다. 명시된 첨부 영역 전체 순회·중복 제거·부분/누락/한도·UNKNOWN 역할·비지원 미다운로드를 유지한다. 상세는 `announcement-attachment-legal-board-profiles-2026-09-12.md`다.
- 처음 실측에서 부산 한글 Content-Disposition 오류와 강북 HTML 중간 페이지를 확인했다. 부산에만 최대2회 엄격 UTF-8 octet 복원을 적용했고 기본 header 검증·경로/제어문자/형식 차단은 유지했다. 강북은 GET중간→게재기간 POST→파일 POST의 고정3단계이며 기간 확인을 우회하지 않는다. 폼10필드·원래 파일 ID·endpoint·만료를 검증하고 원문을 단계마다 정리한다. opaque 요청은 저장하지 않는다.
- 공통 flow/gateway는 리다이렉트 포함4HTTP 상한을 단계 전체에 공유한다. 기존132회 승인 상한을 유지하고 각 요청에 DB heartbeat/허용·host lease/누적 bytes 예약을 다시 적용한다. 초기의 기존 출력 파일 삭제 위험을 사전 존재 거부로 막고 회귀를 추가했다. 내부 form 필드 한도8→10 외 크기/timeout/TLS/격리 정책은 확대하지 않았다. common flow/gateway/type validator가 profile 지문에 포함되어 기존 정책은 새 manifest·전체 QA가 필요하다. 기존 migration/DB 행/API shape/화면은 변경하지 않았다.
- 신규 프로필14·고정flow9·worker연결2·header2사례 및 내부10/11필드 경계 검증을 추가했다. 최초 worker 테스트2실패는 잘못 적은 UNSUPPORTED 기대값을 실제 BLOCKED/UNSUPPORTED_FORMAT 계약으로 수정해 해소했다. header fixture 첫 실패는 C1 octet이 없는 표본 선택 문제였으며 실제 한글 지원/중복 인코딩·잘린/과잉/경로 이탈 표본으로 검증했다. 조건을 제거하거나 네트워크 실패를 통과로 바꾸지 않았다.
- `:test :attachment-extractor:test bootJar :attachment-extractor:installDist attachmentProfileDiscoveryQa --rerun-tasks --no-daemon --max-workers=1`(Windows-ROOT 설정)은3분11초/17task 실행했다. root200suite/1862건=1655통과/207생략/실패·오류0, 추출기25통과, bootJar/installDist 수행 성공이다. 하지만 실사이트 마지막 단계에서 강북184744 HTTP400 1건이 발생해 **전체 명령 종료는 실패**였다. 당시 첫 PDF는 정상, 요청5번째에서 거부, 임시 원본 정리 true였다. 전체 빌드를 무조건 성공이라고 표시하지 않는다.
- 안전한 파일 순번/BRIDGE_GET·PERIOD_POST·FINAL_POST 단계 metadata만 보강한 후 `attachmentProfileDiscoveryQa`를 같은 표본으로 재실행했다.29초/13사례/실패·생략0으로 통과했다. 새7사례는19개 발견·18파일(PDF2/HWPX16)1,698,868byte·43요청·비지원1개 미다운로드, 기존6사례까지28파일·2,339,414byte·59요청이다. 전수 원본 정리를 확인했다. 중간 HTTP400 원인은 미확정이며 외부 서버의 간헐 응답이라는 추정 이상으로 단정하지 않는다. 최종13사례는7개 첨부 profile의 다운로드 경계 증거로, 등록9개/전체41종 목록 parser/223개 활성 수집원의 추출 성공이 아니다.
- Node7파일129통과/실패·생략0. JAR SHA256 `57a2d1e422d1f3169b6e4988ccc67900d6ab047ef464974fca11d66c1d26cab0`; 수정한 주요class7개와 JAR 내부 bytes가 일치했다. 기존 MockBean/unchecked/Log4j provider 경고는 유지된다. 실제 HWP 파일·Linux 내부 추출·최신 PostgreSQL은 이번 미실행이며 직전 initdb 실패 Gate를 숨기지 않는다.
- 05:49 GitHub pull=true/push=false/admin=false, origin/master와 HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24` 일치. 변경·미추적386개/staged0/추적 migration 변경0, git diff --check 종료0이다. 커밋·푸시·운영 배포·정책 게시/ENFORCE·기존 데이터 실행·AWS/운영 health/SHA/로그인 조회·브라우저·서브에이전트는 이번에 하지 않았다. 브라우저는 UI 변경 없는 이번 backend/실파일 증분에서 재실행하지 않았다. 모든 Gradle/Node 명령 handle이 종료됐고 Java 프로세스0을 확인했다.
- 남은 핵심 작업은 나머지 provider/profile과 전체 coverage, 실제 PROVIDER_PROFILES/WORKER_DB_RECOVERY 실행기·근거 검증 연결, Linux/PG 전체 QA, 승인 범위의 기존 데이터 적용·복구, 쓰기 권한 복구 후 동일SHA 배포와 실제 운영 역할 브라우저 E2E다. 목표를 축소하거나 완료로 표시하지 않는다.

### 2026-09-12 05:11 KST — V77 게시 화면·실제 로컬 브라우저·검증 경계 재확인

- 직전 회차는 Gate 수 상태 보고 중심으로 코드 진척이 없었다. 이번에는 AGENTS·현재 V77/API/Service/Mapper/템플릿/기존 테스트를 확인하고 실제 게시 UI 연결을 구현했다. 장기 목표 스킬의 전체 범위 유지와 UI/UX 스킬의 위험 확인·미확정 회복·네이티브 입력 기준을 적용했다. 전체 Gate0~8 최종 통과0·Not ready를 유지한다.
- V77 서버 경로는 이번 시작 전에 존재했다. 전체 QA 근거 재검증·18개 테이블 EXCLUSIVE NOWAIT/15초·이전 ACTIVE 퇴역/신규 ACTIVE/영수증/감사 원자 저장과 QA installed.runtimeHash 고정을 확인했다. 실제 전체 Provider/worker DB 실행기/근거 verifier는 미연결이며 이 상태를 단위 테스트 성공으로 우회하지 않는다. 새 migration 또는 운영 DB 변경은 이번 회차 없다.
- 기존 정책 화면과 스크립트 2개에 V76 준비 목록/상세/전체 항목 페이지, 현재성·만료·혼합 건수, 세 개별 확인, V77 게시 요청·불변 영수증 조회를 연결했다. 준비/게시 키를 분리하고 응답 유실은 같은 body/key로 재확인한다. 실제 전체 QA 미완료/범위 불일치/기한 만료/미저장 입력/일부 조회 실패는 게시 불가다. 고정 정책·QA·범위/버전/hash/mode와 응답을 대조한다. 구현 상세와 파일은 `announcement-attachment-policy-publication-ui-2026-09-12.md`를 따른다.
- `attachmentMigrationTest attachmentJobIntegrationTest --rerun-tasks --no-daemon --max-workers=1`(Windows-ROOT 설정)은36초 실패했다. Backfill 초기화1건과 migration2건 모두 임시 PostgreSQL initdb 시작 실패이며 SQL assertion에 도달하지 않았다. 뒤의 job task는 미실행이다. 과거 Code Integrity libpq 거부와 구분하여 이번에는 initdb 실패까지만 확정한다. 보안 정책·TLS·격리 제한을 해제하지 않았다.
- SSR 표적 `:test --tests com.saneb.domain.announcementattachment.controller.AnnouncementAttachmentPolicyViewControllerSmokeTest`는25초 성공(8건). 합성 화면 export 환경변수는 해당 명령 범위에서만 설정·복원했다. 실제 Spring Security/Thymeleaf 역할·disabled/미체크 동의·한글 설명을 확인했으며 실제 DB 인증/게시 E2E는 아니다.
- 전체 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`은2분54초/16task 성공. root197suite/1834건=1628통과·206생략·실패/오류0, extractor25통과다. 일반 root의 PG 조건부 생략과 위 전용 PG 초기화 실패를 구분한다. Node 정책34건 통과(기존22+신규12), 기존 전체7파일은 신규10건 시점127통과했고 마지막 추가2건까지 전체 재실행 결과는 아래 최종 확인에 기록한다.
- 명시 승인된 브라우저 QA 범위에서 실제 SSR export+loopback 합성 API를 Playwright CLI 전용 세션으로 검증했다. 준비 후 게시 버튼 활성, 미체크3항 제출 차단/첫 항목 포커스, Tab/Space/Enter로 세 항목 확인·게시, 불변 영수증 표시를 확인했다. lost-publication은 최초 처리를503으로 숨긴 후 같은 key/body 재확인2회에도 합성 쓰기 수2(준비1/게시1)가 유지됐다. expired-scope는 준비1/게시0, readonly 및 INCOMPLETE는 게시 차단이다. 운영 DB·인증·정책 쓰기는0이다.
- 320/375/768/1024/1440px에서 scrollWidth=viewportWidth이고 가로 넘침0이었다. reduced-motion 설정에서 확인했고 desktop/mobile 개별 확인란 스크린샷을 직접 확인했다. 출력 위치는 `build/attachment-policy-ui-qa/.playwright-cli/`이며 desktop `element-2026-09-11T20-07-28-546Z.png`, mobile `element-2026-09-11T20-07-30-676Z.png`다. 콘솔은 미게시 영수증 GET의 예상404와 유실 시나리오의 의도된503이며 JS 예외는 관측되지 않았다. CLI eval 최초1회는 PowerShell 인용 오류였고 인용 수정 후 실제 검증했다. 운영 역할/실제 API·DB, 스크린리더·실제 브라우저 확대 검증은 남는다.
- JAR SHA256 `50b1b732d4dbacc0ef21a178f867a1dc899cda6be09c50c3629623891c4537b1`. JAR 내부 JS2·정책 템플릿·V77·Publication Mapper bytes가 현재 파일과 일치했다. 로컬 HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`, 변경/미추적378개, staged0, 추적 migration 변경0, 기본 git diff --check 종료0. 새 사용자 변경을 임의 커밋·삭제하지 않았다.
- GitHub 실조회는 pull=true/push=false/admin=false이며 권한 있는 계정의 브라우저 재인증을 비차단 질문으로 요청했다. AWS/현재 운영 health·SHA·로그인/Actions는 이번 미조회다. 커밋·푸시·운영 배포·정책 게시/ENFORCE·기존 데이터 적용·서브에이전트는 실행하지 않았다. 합성 브라우저와 소유 Node 서버를 종료했고 최종 자원 검사는 아래에 기록한다.
- 다음 필수 작업: 실제 전체 Provider/profile QA 실행·근거 검증기, 격리 worker DB QA 및 Linux/PG 실행, 전체 기존 데이터 배치/복구 대조, 승인 범위 정책/데이터 반영, 동일 SHA 배포와 운영 역할 브라우저 E2E. 이번 UI 흐름 통과를 전체 목표 완료로 대체하지 않는다.
- 최종 확인: Node7파일129통과·실패/생략0(정책34 포함), 원격 master와 로컬 HEAD ae893b87348a9bd1cb0893763f6cad5047093a24 일치. 변경/미추적378개·staged0·git diff --check 종료0. 선택7파일 내용의 제한적 credential 패턴 일치0이며 전체 보안 감사는 아니다. Java 프로세스0·소유 QA Node0·loopback56518 listen0, Playwright 전용 브라우저와 fixture 서버/모든 실행 handle 종료를 확인했다. 사용자 소유 프로세스·기존 데이터는 정리하지 않았다.

### 2026-09-12 04:17 KST — 게시 준비 범위 원장·API와 로컬 회귀

- 직전 회차는 Gate 수를 확인한 상태 보고로 진척이 없었다. AGENTS/현재 Git·V76 작성본·정책/QA/게시 영향·실제 Service/Mapper/검증 장치를 다시 확인하고 DB/API 기반을 완성·검증했다. 장기 목표 운영 스킬의 구현/검증/운영 분리 원칙을 적용했다. 전체 Gate0~8 최종 통과0·Not ready는 유지한다.
- V76은 현재 DRAFT 정책/규칙 버전, 선택적 QA 연결, 전체 POLICY/SOURCE/JOB/COLLECTION_PLAN/COLLECTOR의 ID·상태 지문을 같은 생성 transaction에서 고정·봉인한다. 양방향 EXCEPT, count/hash, 마지막 정책/규칙 버전 재검사와 이후 수정/삭제 금지를 추가했다. 규칙 snapshot hash도 상태 지문에 포함하고 FK 조회 인덱스를 보강했다. V1~V75는 이 증분에서 수정하지 않았다. 실제 DB migration은 미실행이다.
- `publication-scopes`의 ADMIN POST(준비)와 READ3역할 GET 목록/상세/항목 API를 기존 계층에 연결했다. 10분 유효기간·원래 요청 키 재전송·만료 이력·동시 INSERT 패자 재조회·권한/CSRF/unknown 입력/원문 비노출을 검증했다. 실제 게시·승인 영수증·실행 UI는 아직 없다. isApproval=false/requiresPublicationRevalidation=true/0HTTP를 유지하며 현재성 조회를 QA 통과로 표현하지 않는다.
- 첫 표적 실행159건 중2건은 새 Mockito 응답 재설정 중 null fixture가 실행된 문제였다. doAnswer/doThrow로 설정 방식을 수정했고 동일 표적159건이29초에 통과했다. 서비스19·HTTP21와 Mapper/MigrationContract를 포함한다. 실제 PostgreSQL6사례(1000 초과/원장 재전송, 열린/부분 commit, 같은 수 다른 ID/hash, 같은 transaction 정책 변경, 이력 불변/변경 후 현재성, 동시 키)를 추가했다. 지연 COMMIT 제약은 wrapper가 아니라 PostgreSQL SQLSTATE23514를 검증한다. 실제 PG 실행 성공은 주장하지 않는다.
- 전체 재실행 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`은2분49초/16task 성공이다. root194suite/1788건=1586통과·202생략·실패/오류0, extractor25통과. Node7파일117통과. 기존 MockBean/unchecked/Log4j provider 경고는 남는다. 추가 PG assertion helper의 마지막 컴파일/회귀는 아래 최종 확인에 기록한다.
- 04:11 원격 master/로컬 HEAD는 `ae893b87348a9bd1cb0893763f6cad5047093a24`, GitHub pull=true/push=false다. 04:10 Docker Linux named pipe 부재, WSL docker-desktop만 확인했다. 04:16 AWS 서울 STS는 TLS 인증서 검증 실패 exit255이며 계정 권한 부족/세션 만료로 단정하지 않는다. 운영 health/SHA/정책·실제 DB·운영 브라우저는 이번 미조회다.
- API24.25/DB11.21 및 `announcement-attachment-policy-publication-scope-2026-09-12.md`에 현재 계약·정확한 집합·멱등/만료/최종 QA 경계를 기록했다. UI는 변경하지 않아 기존의 실제 게시 미제공 안내를 보존한다. 새11파일 내용의 제한적 credential 패턴 검사0은 전체 보안 감사를 뜻하지 않는다. 정책 게시/ENFORCE/기존 데이터 실행, 커밋·푸시·배포·브라우저·서브에이전트는 실행하지 않았다.
- 다음 필수 작업은 최종 QA 증거·설치·전체 profile 검증을 실제 게시 transaction/승인 영수증/UI에 연결하고, 전체 Provider/profile·Linux/PG·기존 데이터 전체 처리·동일SHA 운영 배포·역할별 브라우저 E2E를 완료하는 것이다. 준비 API 성공을 이 전체 목표의 완료로 대체하지 않는다.
- 04:20 최종 확인: PG 제약 assertion helper 보완 뒤 `.\gradlew.bat :test bootJar --no-daemon --max-workers=1`(동일 Windows-ROOT 설정)2분28초 성공, test/compileTestJava 재실행·bootJar UP-TO-DATE다. root194suite/1788건=1586통과/202생략/실패·오류0을 재확인했다. 최종 JAR SHA256 `5b09f6b00ef39a15ea5218d2783bdeef151255a23c284fca75d6c81c73d354f8`, JAR 내부 V76/Scope Mapper bytes와 현재 원본이 일치했다. Git HEAD는ae893b8 그대로, 변경·미추적363파일/staged0/추적 migration 변경0이다. 기본 git diff --check 종료0(기존 CRLF 변환 안내만 존재), 신규11파일 내용의 제한적 credential 패턴 일치0이다. 모든 Gradle/Node QA handle은 종료했으며 사용자 소유 Node 프로세스는 유지했다.

### 2026-09-12 03:41 KST — 화천 POST 마무리·실패 근거·전체 회귀

- 직전 Gate 수 보고는 구현 진척 없는 상태 보고였다. 현재 AGENTS·작업 트리·설계·테스트를 다시 읽고 미완성 화천 POST 실패 코드 연결을 수정했다. 장기 목표 운영 스킬의 구현/검증/운영 증거 분리 원칙을 적용했다. 전체 Gate0~8 최종 통과0, Not ready 유지다.
- 화천 `LOCAL_HWACHEON_POST_V1`은 LGS-000130/exact host/detail7query(subCheck=N)/고정POST4필드만 사용한다. 불투명 인자를 해독/영구 저장하지 않으며 locator는 경로+시스템 값 hash다. 수동 역할은 보존하고 일반 발견 역할은 UNKNOWN이다. 초기 긴 문자열 regex StackOverflowError는 공통 비재귀 `AttachmentDownloadInvocation`으로 수정했고 서구/새올GET도 같은 해석기를 사용한다. 화천 짧은 불투명 값 표본 실패는 실제 형식 재확인·회귀 추가로 해소했다.
- 발견 코드 `ATTACHMENT_DOWNLOAD_FORM_CHANGED`가 서비스 허용 목록에 없던 누락을 수정했다. 임의 코드 허용은 확대하지 않았다. 새 저장 대역6건·worker 폼 변경 사례·PG 불변/조회 입력을 추가하고 다섯 발견 오류의 관리자 한글 표시를 연결했다. worker 또는 DAO 대역 성공을 실제 DB 성공으로 보고하지 않는다. PG 새 입력은 미실행이다.
- 표적 회귀22초 성공, Node7파일117통과. 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist attachmentProfileDiscoveryQa --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **2분56초 성공/17task 재실행**. root192suite/1740건=**1544통과·196생략·실패/오류0**, extractor25통과, 별도 실사이트6사례 통과/생략0. job PG159·전체 분할PG12·migration2 생략이며 비활성 parameterized PG 입력은 개별 실행으로 펼쳐지지 않았음을 구분한다.
- 최신 전용 QA는 상세6회+첨부10회, PDF2/HWPX8·640,546byte, 모든 임시 원본 정리였다. 화천33897/33895 두 공고 세 파일75,338byte의 실제 POST를 포함한다. 텍스트 추출/DB쓰기/운영활성화는0. metadata에는 hash/byte/상태만 남겼다. 공통 해석 class 변경을 profile 지문에 포함했으므로7개 프로필의 정책 초안/전체 QA 재고정이 필요하며 기존 정책은 변경하지 않았다.
- JAR SHA256 `a0ed8c38a038fa2e0b5742dc16a9b19fe188218808579eb45c6cc95a767a843c`. 기존 MockBean/unchecked/Log4j provider 경고 유지. 상세 구현/파일/검증 한계는 `announcement-attachment-hwacheon-post-profile-2026-09-12.md`를 따른다.
- 03:44 산출물 재검사: 변경된 프로필/공통 호출/지문/근거 서비스 class6개와 한글 표시 JS1개의 JAR 내부 bytes가 로컬 빌드/원본과 일치했다. 변경·미추적352개/staged0/추적 migration 변경0. 기본 `git diff --check` 종료0(기존 CRLF 변환 안내만 있음). 앞서 명령 단위 core.autocrlf=false 검사로 CRLF가 대량 공백 오류로 표시됐으나 저장소 설정을 유지한 정상 검사로 구분했으며 전체 줄바꿈을 수정하지 않았다. 제한된5파일 credential 패턴 검사0이며 전체 보안 감사는 아니다. Java/이번 QA Node 프로세스0, 모든 실행 handle 종료, 사용자 자원은 종료하지 않았다.
- 03:41 GitHub pull=true/push=false, 원격 master/로컬 HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24`. Docker Linux 엔진 named pipe 부재, WSL docker-desktop만 재확인했다. AWS/운영health·SHA·로그인 및 운영 역할 브라우저는 미조회다. 코드·기존 migration·배포·운영 정책 상태를 원격에서 변경하지 않았다.
- 남은 작업: 나머지 전체 Provider/profile, 실제 Linux/PG·격리 worker·실파일 텍스트, 정책 전체 QA/게시와 정확한 범위 승인, 기존 데이터 전체 처리/대조, 커밋·푸시·동일SHA운영 배포·역할별 브라우저E2E. 표본 다운로드 성공으로 전체 목표를 축소하지 않는다.

### 2026-09-12 03:13 KST — 새올 GET4기관 구현·실제 첨부 다운로드 검증

- 직전 회차는 Gate 개수만 확인한 **진척 없는 상태 보고**였다. 이번에는 AGENTS·설계·실제 seed/프로필/worker/테스트를 확인하고 공통 새올 GET 프로필과 4개 시스템 bean을 구현했다. 장기 목표 운영 스킬로 합성/실파일/운영 증거를 분리했다. 전체 Gate0~8 최종 통과0, **Not ready**는 유지한다.
- 부산 남구·대구 달성군·대구 중구·함안군의 exact host/path·source code·목록 profile·첨부 영역을 고정했다. HTTP로 저장된 남구 source는 identity를 보존한 채 검증한 HTTPS 상세만 요청한다. 기존 2개에 새4개가 추가되어 등록 프로필은6개다. 화천군은 실측 결과 암호화 인자/별도 POST라 공통 GET으로 등록하지 않았다. 전체 지역 범위를 축소한 것이 아니며 전용 구현이 남는다.
- 설계6.1과 달리 기존 기업마당/대전 서구가 파일명으로 NOTICE/GUIDE 등을 확정하던 차이를 수정했다. 새 프로필을 포함해 일반 첨부 영역의 역할은 UNKNOWN이며 기존 MANUAL 역할은 재시도에 보존한다. 두 기존 프로필 hash도 바뀌므로 과거 정책 QA/고정 hash 재사용은 불가하다. DB/API shape·migration·운영 데이터는 이번에 변경하지 않았다.
- 새 단위17사례와 실제 worker 연결 대역1사례를 추가했다. 파일 전체 순회·빈 영역/변경/부분/한도·URL/인코딩/다른 기관·역할·등록을 검증했다. 첫 표적 회귀의 새 Mockito 재설정 NPE1건은 doAnswer 방식으로 수정했고 이후 표적 회귀는18초 성공했다. 테스트 조건을 제거하거나 실패를 생략하지 않았다.
- `attachmentProfileDiscoveryQa` 전용 명령을 추가하고03:04/03:11 두 번 실제 실행했다. 각 실행은 공식 상세4회+첨부7회, PDF1/HWPX6·파일565,208byte를 기존 pinned transport/파일 식별기로 확인했다. 모든 임시 HTML/binary 정리, 보고서에는 비식별 hash/byte/상태만 저장한다. 추출기·DB·운영 활성화를 실행한 결과가 아니다. 상세 기준/공고 ID/명령은 `announcement-attachment-saeol-get-profiles-2026-09-12.md`에 있다.
- 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist attachmentProfileDiscoveryQa --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **2분52초 성공/17task 재실행**. root189suite/1715건=**1519통과·196생략·실패/오류0**, extractor25통과, 별도 실사이트4사례 모두 통과/생략0. 일반 root의 새 실사이트 test container 생략1건과 전용4사례 실행을 구분한다. 실제 job PG159/전체 분할PG12/migration2는 여전히 생략이다.
- Node7개 QA 파일 **116통과·실패/생략0**. JAR SHA256 `fe5ff9c7c9ad3719ea0bb1727d9a82f03df12b355d7ff7a4106b9d07431f6982`, 수정한 프로필/등록 class4개와 JAR 내용 일치. 기존 MockBean/unchecked/Log4j provider 경고는 남는다. 제한된 신규5파일 credential 패턴 지적0이며 전체 보안 감사는 아니다.
- Docker Linux 엔진 연결 실패, WSL은 docker-desktop만 확인했다. OS 보안/TLS/격리를 해제하거나 호스트에서 실제 외부 문서를 파싱하지 않았다. 03:13 GitHub pull=true/push=false, origin/master와 로컬 HEAD `ae893b87348a9bd1cb0893763f6cad5047093a24` 일치. AWS/운영 health·SHA·역할 인증은 미조회다.
- 시작341→현재346개 변경·미추적 파일, staged0/추적 migration 변경0, `git diff --check` 지적0. 기존 사용자 변경은 보존했다. Java/해당 Node QA 프로세스0, 모든 실행 handle 종료. 브라우저/서브에이전트·커밋/푸시/Actions/배포·정책 게시/ENFORCE·기존 데이터 적용은 이번 회차 실행하지 않았다.
- 다음 핵심 작업은 나머지 Provider/profile(화천 POST 포함), 전체 실파일·격리 worker/DB QA와 정책 최종 검증/게시다. 현재4기관 다운로드 성공을 전체 ATT/운영 E2E 성공으로 대체하지 않는다.

## 발견한 계약 차이

1. 설계의 job COMPLETED 대신 V72는 SUCCEEDED, 다운로드 DOWNLOADED 대신 SUCCEEDED를 사용한다. 기존 migration을 수정하지 않고 실제 DB 상태에 맞춘다.
2. V72에는 job 실행의 profile/엔진/추출기 고정값, 재시도 누적 bytes와 전역 자원 lease가 없다. additive 계약과 테스트가 필요하다.
3. DB/API/seed 과거 문서의 첨부 제외 및 제목 제외 원문 보존 문구는 현재 목표/V70/첨부 설계와 구분해 정정해야 한다.
4. V72 migration 테스트의 V71 이후 1건 단정은 다음 additive migration을 고려한 V72 고정 검증과 최신 upgrade 검증으로 분리해야 한다. 기존 검증을 약화하지 않는다.

## 확인 명령과 외부 상태

- `Get-Location`, `Get-Content AGENTS.md`, `git status --short --branch`, `git log -5 --oneline --decorate`, `git remote -v`, `git rev-parse HEAD`: 위 로컬 기준선 확인.
- `node -e ...`: 필수 문서 길이/경로 일회성 확인, 종료 코드 0. 상시 Node 서버를 시작하지 않음.
- `aws sts get-caller-identity --region ap-northeast-2`: TLS 인증서 신뢰 실패. 계정/인증정보 원문은 기록하지 않음.
- `git ls-remote origin refs/heads/master`: 기본 CA 신뢰 실패 후 `git -c safe.directory=C:/PersonalProject/saneB -c http.sslBackend=schannel ls-remote origin refs/heads/master` 성공. TLS 검증/전역 Git 설정 변경 없음.
- 운영 브라우저용 실제 URL을 사용자에게 비차단 질문으로 요청함. localhost만으로 외부 정상 판정하지 않음.

## 검증 추적

상세 요구 ID와 기대값은 `announcement-attachment-qa-plan-2026-09-08.md` ATT-001~ATT-062를 유지한다. 구현·테스트를 추가할 때 각 ID별 현재 실행 증거를 아래에 연결한다. 이전 실행은 회귀 표본일 뿐 이번 E2E 통과 건수로 계산하지 않는다.

전체 ATT 완료 판정: 미완료. 아래 자동 검증은 표시된 계층의 증거이며 API·브라우저·운영 증거를 대신하지 않는다.

### 2026-09-10 로컬 하위 검증

- `compileJava`: 통과.
- `attachmentJobIntegrationTest attachmentMigrationTest`: 실제 격리 PostgreSQL에서 근거/동시성 20건·migration 2건, 실패/오류/생략 0. 빈 DB와 V71→V72→V73 upgrade, 기존 checksum 유지 확인. 후속 조회 테스트까지 job task 21건 통과.
- root 관련 회귀 `:test --tests '*AnnouncementSource*' --tests '*MigrationContractTest' bootJar`: 202건, 실패/오류/생략 0, build 성공.
- `test bootJar`: root 총 613건 중 580건 실행 통과, 33건 조건부 생략, 실패/오류 0. extractor 하위 task는 UP-TO-DATE였으므로 이번 재실행으로 세지 않는다.
- 위 Gradle 명령에는 `--no-daemon --max-workers=1`과 Windows JVM 신뢰 저장소 인자를 사용했다. 종료된 single-use daemon 외 상시 Java/Node 서버를 시작하지 않았다.
- 짧은 `--tests '*Attachment*'` 패턴은 Windows batch 인자 처리에서 폴더명으로 해석되어 task 선택 단계가 실패했다. 전체 test 또는 fully qualified class 패턴으로 실행했다. 테스트 자체 실패나 통과로 세지 않는다.
- source guard PG 경합·근거 봉인·같은 입력 동시 저장·source 삭제 cascade·base 변경 stale·code point 부분 조회 테스트 통과. 관리자 HTTP 조회/403/404/no-store/CSRF namespace 테스트와 기존 controller 회귀도 통과했다.
- 신규 기업마당 발견 profile의 합성 DOM 8건은 통과했다. profile 구현을 실제 worker가 사용한 결과는 아직 없다.
- 후속 `test bootJar --rerun-tasks`: root 647건 중 603건 통과·44건 환경 조건 생략, 추출기 13건 재실행 통과, 실패/오류 0. 이 실행 이후 추가한 종합 평가 저장은 별도 검증한다.
- 종합 평가 저장을 포함한 `attachmentJobIntegrationTest`: 28건 통과, 실패/오류/생략 0. 실제 seed에서 게시된 규칙 조회, 키워드/다중 태그 저장, 같은 lease 멱등·동시 확정, 미봉인·만료 fence, 부분 실패 전체 입력 저장, 새 세대 이력 분리, ENFORCE 검수 guard 유지 검증.
- 임시 저장소 8건 통과: 정상/예외 정리·21 MiB 단위 용량 예약·24시간 orphan 정리·살아 있는 lock 보존·예상하지 못한 파일 보존·quota 경합 시 핸들 해제. 실제 OS crash를 재현한 시험은 아니며 orphan fixture를 사용했다.
- 후속 `test bootJar` 전체 회귀: root 662건 중 611건 통과·51건 조건부 생략, 실패/오류 0. extractor는 UP-TO-DATE이며 앞선 13건 강제 재실행 증거와 구분한다. single-use Gradle daemon은 종료됐다.

| 요구 | 현재 증거 | 아직 남은 검증 |
|---|---|---|
| ATT-029/030 | JobIntegrationTest의 멱등·동일 key 동시 요청·다른 hash 409 | 실제 API wrapper/사용자 요청 |
| ATT-031/034/035 | claim/만료 token/원문 변경·삭제·늦은 근거 저장 차단 및 종합 evaluation 최종 CAS 테스트 | 실제 worker |
| ATT-012/013/014/038 | NO_FILES/발견 실패 구분, 성공·실패 파일 동시 봉인, 원문 삭제 cascade | 실제 네트워크/worker |
| ATT-036/045/052 일부 | source 일치 404, READ 3역할/외부역할 403, no-store 및 bounded Unicode 구간 조회, CSRF namespace 보호 | 변경 API/실제 운영 권한·브라우저 DOM |
| ATT-024/060 | 누적 예산·공유 2/1/1 슬롯 PG 테스트, downloader 읽기 전 예산 예약 | 전체 worker 연결·운영 계측 |
| ATT-041/042 | V1/V2 전환·확정·기본 재분류/rollback guard 및 기존 전환 회귀 | API 409·브라우저 새 경로, 실제 ENFORCE |
| ATT-053 | 빈 DB 및 V71/V72→V73 migration 테스트 | 운영 migration 이후 별도 확인 |
| ATT-061 | 기본 규칙 release 불일치 예약 거부 | 정책/기존 데이터 적용 경로 |
| ATT-022/023/024/025 일부 | redirect path 재검증, DNS timeout, stream 한도/실패/encoding 단위 검증 | 실제 HTTP·파일 format 검증·운영 profile QA |

### 구현 경계와 알려진 한계

- V1~V72는 수정하지 않았다. V73은 로컬 additive 파일이며 운영 적용 전이다.
- 기본 제목·본문 판정 및 기존 source 링크는 보존한다. 첨부 필수 source의 기존 변경 경로는 409이며, 기존 link가 있는 전환 재요청은 쓰기 없이 같은 link를 반환한다.
- downloader의 재시도 예산은 읽기 전 예약 방식이다. 마지막 미사용 예약량(최대 8 KiB)은 환급하지 않으며 실제 수신량과 별도 집계한다. 크기 미상 응답이 예산을 정확히 소진하면 추가 1바이트를 읽지 않고 제한 실패로 처리한다.
- DNS는 전체 deadline 내에서 최대 3초만 기다리며, 지연된 OS DNS task가 HTTP를 실행하지 못한다. 풀은 대기열 없이 최대 2개이고 close 시 interrupt한다. OS resolver 자체의 강제 종료를 입증한 것은 아니다.
- request 종료 시 연결을 취소하여 redirect/오류 body가 연결 재사용 정리 과정에서 무제한 읽히지 않도록 했다. 정밀 HTTP 전송 QA는 남아 있다.
- 상시 worker·정책 ACTIVE/ENFORCE·기존 데이터 적용·커밋·푸시·배포·운영 브라우저 QA는 아직 실행하지 않았다.
- 종합 평가 저장은 DB의 SEALED set만 읽으며 다운로드를 하지 않는다. 입력 snapshot과 CPU 분류, source→job 잠금 아래 확정을 분리했다. 파일 실패도 evaluation input에 포함하고 판정 근거에는 원문 대신 rule/file/extraction ID와 code point 위치를 저장한다.
- 같은 작업의 재저장은 안정적인 set/file/extraction ID를 사용한다. 새 작업 세대는 새 ID로 과거 선택 근거와 구분하고 manifest에 schema version·선택 set/file/extraction ID를 포함한다. 따라서 첨부 없음의 새 확인도 별도 이력이며 임시 URL·lease·처리 시각은 hash에 포함하지 않는다.
- 일반 작업은 별도 첨부 current pointer만 갱신한다. COLLECT_ONLY의 effective=base를 바꾸거나 검수 요구 flag·기존 정책·운영 공고를 변경하지 않는다. 배치 작업은 preview만 저장하도록 분리했으며, 배치 실행 API 자체는 아직 없다.

### 2026-09-10 운영 읽기 전용 재확인

- Windows 기본 TLS 검증에 성공한 공개 root CA만 임시 bundle로 사용했다. AWS `--ca-bundle`만 지정하면 로그인 token 갱신 경로에서 신뢰 오류가 남았으나, 해당 명령의 `AWS_CA_BUNDLE`을 지정하자 서울 리전 STS 인증이 성공했다. TLS 검증 해제·영구 환경변수 변경은 하지 않았다.
- `deploy get-deployment`: 기존 `d-VHMRNZRPK`는 Succeeded, 완료 시각 2026-09-09 23:16:59 KST. application/group `saneb`/`saneb-dev`, 배포 태그 `SanebDeployTarget=true` 확인.
- 승인 범위의 SSM 읽기 전용 명령 `8425b27f-7f01-471f-ba4a-bd727a16d254`: saneb ActiveState=active/SubState=running, localhost health HTTP 200, 서비스는 8080에서 listen. 조회한 saneB nginx 설정 경로에는 별도 출력이 없었다. 설정 파일 부재만으로 전체 라우팅 부재를 단정하지 않는다.
- 조회된 배포 대상의 공인 주소 `http://15.165.36.6:8080/actuator/health`도 현재 PC의 직접 요청에서 HTTP 200이었다. 과거 timeout과 구분한다. 운영 관리자 로그인·권한별 브라우저 조작은 아직 미실행이며 정식 접속 URL 확인은 별도다.
- 이 확인은 기존 서비스에 대한 진단이다. 이번 로컬 수정 코드/V73을 배포한 결과가 아니며 운영 정책·수집 데이터는 변경하지 않았다.
- 같은 서버의 실제 서비스 연결을 사용한 읽기 전용 DB 집계에서 V72, 운영 원문 2,945건(전부 LOCAL_GOV_NOTICE), link 9건, ACTIVE 규칙 ASCR-000001, 첨부 정책/작업 0건을 확인했다. 세부 파서별 건수와 설정 확인 한계는 `announcement-attachment-runtime-inventory-2026-09-10.md`를 따른다.

### 기업마당 프로필 조사 (읽기 전용)

- 2026-09-10 공식 상세 3건(`PBLN_000000000124628`, `PBLN_000000000120120`, `PBLN_000000000117918`)의 HTML에서 `.attached_file_list > ul`과 `.file_name`, `/cmm/fms/fileDown.do` 직접 링크 구조를 확인했다. 각각 5/1/1개의 다운로드 링크였다.
- 개발용 DOM 조사이며 파일 binary 다운로드·운영 job·DB 수집을 실행한 것이 아니다. 목록에서 열린 제목이나 링크만으로 실제 첨부 처리 성공을 주장하지 않는다.
- `BIZINFO_DETAIL_V1`은 exact host/path/query만 허용한다. profile hash에는 계약 descriptor와 배포된 parser class를 포함한다. 구현 또는 compiler 산출물이 바뀌면 재검증/정책 갱신이 필요하다.
- 확장자 없는 링크도 발견하며 PDF/HWP/HWPX를 후보로 처리한다. 명시적 비지원 형식도 발견 목록에서 숨기지 않고 다운로드 차단 대상으로 남긴다. 파일명은 역할 힌트일 뿐 분류 키워드 입력이 아니다.
- 정부24 및 지자체 전체 첨부 프로필 구현/QA는 남아 있다. 목록 파서 성공을 첨부 프로필 성공으로 간주하지 않는다.

### 상시 worker 연결 하위 검증 (2026-09-10)

- `:test --tests 'com.saneb.domain.announcementattachment.worker.*' --tests 'com.saneb.domain.announcementattachment.discovery.*' attachmentJobIntegrationTest attachmentMigrationTest bootJar`: 성공. root 선택 테스트 32건(작업자 13·파일 형식 검증 4·발견 profile 15), 실제 임시 PostgreSQL 작업/무결성 32건, migration 2건. 실패·오류·생략 0.
- 작업자 테스트의 HTTP와 추출기 응답은 mock이며 실제 임시 디렉터리 정리·전체 파일 순회·DB 저장 호출 순서를 검증한다. 실제 Linux/공개 binary/운영 DB·API·브라우저 성공 증거는 아니다.
- `LOCAL_DAEJEON_SEOGU_V1`은 LGS-000074 및 정해진 list parser에만 적용한다. 서구 공식 상세 HTML에서 `fileForm`, `.bbs--view--file`, 고정 POST `/emwp/jsp/ofr/FileDown.jsp` 구조를 읽기 전용으로 확인했다. 223개 기관에 일반화하지 않았다. 실제 POST binary QA는 남아 있다.
- 직접 POST는 최대 8개 허용 이름·8 KiB 인코딩 본문만 전송하고 모든 POST redirect를 차단한다. 파일명/서버 디렉터리는 요청 메모리에만 두며 locator에는 hash와 공고 ID만 남긴다. parser/shared 요청 검증 class 변경은 profile hash에 반영된다.
- Gate 2 worker는 별도 단일 executor로 실행하여 기존 scheduler를 장시간 막지 않는다. 기본 flag `SANEB_ANNOUNCEMENT_ATTACHMENT_WORKER_ENABLED=false`. 설치된 격리 실행 hash 불일치는 요청 전 중지한다. worker 활성화나 정책 게시를 이번 하위 검증에서 수행하지 않았다.
- 게시 정책의 퇴역만으로 진행 중 작업의 고정 입력을 바꾸지 않는다. 현재 ACTIVE 기본 규칙의 OFF 정책은 새 요청·추가 byte·자원 예약을 차단하며 검수 binding을 해제하지 않는다. 이미 봉인된 set은 HTTP 없이 판정을 재개한다.
- 발견 실패 코드 배열을 V73 additive 컬럼/manifest/조회 DTO에 연결했다. 원문 URL·예외 메시지는 저장하지 않는다. V1~V72는 변경하지 않았다.
- 실행 중단 전에 네트워크를 시작하지 않은 자원 경합은 시도를 환급한다. 이미 요청한 시도와 누적 다운로드 예산은 환급하지 않는다. 이 검증 당시 재시도는 같은 job의 전체 발견/파일 순회를 반복했고 성공 파일 checkpoint는 없었다. 이후 보강은 아래 12시 역할 변경/중간 저장 기록을 따른다. 최종 시도의 실패 근거는 검수 대상으로 봉인하며 실제 Linux worker QA는 여전히 필요하다.
- 자동 예약·ENFORCE 신규 binding/원복 이력, 전체 출처 매핑, 관리자 변경 API/UI, 승인된 기존 데이터 배치, 운영 배포/활성화 및 브라우저 QA는 미완료다. 전체 Gate 통과로 판정하지 않는다.

### 2026-09-10 10시 이후 수집 예약·현재 조회 후속 작업

- 자동 예약과 신규 ENFORCE binding은 이후 구현했다. 02시대 PostgreSQL 테스트 40건 통과 기록은 당시 코드 증거이며 아래 조회 변경의 실행 증거가 아니다.
- 10:23 전체 root 회귀 XML: 703건 중 639건 통과/1건 실패/63건 생략. 실패는 신규 수집 caller 테스트의 Mockito strict stubbing으로 BIZ-NEW 조회를 준비하지 않은 fixture 문제였고 신규/중복 조회를 명시적으로 설정한 후 해당 클래스 20건이 통과했다. assertion을 제거하지 않았다.
- 일반 재수집 예약에서 이전 첨부 current pointer/current flag/confirmation을 같은 source transaction에서 해제하도록 수정했다. 새 pending 응답에는 과거 판정 ID·태그·set hash를 반환하지 않는다.
- 신규 CurrentController/Service/DAO/Mapper와 DTO는 목록·count·상세의 판정 기준을 공유한다. COLLECT_ONLY base/preview 분리, ENFORCE pending/현재 종합 판정, OFF 뒤 검수 유지, confirmed 태그 기준, 제목 제외 원문 비노출을 계약에 기록했다.
- 선택 검증 `:test --tests '...AnnouncementAttachmentCurrentServiceTest' --tests '...AnnouncementAttachmentCurrentControllerSmokeTest' --tests '...AnnouncementSourceServiceImplTest' bootJar` 성공: 39건, 실패/오류/생략 0. HTTP/서비스 검증이며 PostgreSQL 실행 증거가 아니다.
- [!] 10:31 `attachmentJobIntegrationTest`: 테스트 DB 초기화 전 initdb 실행 실패(테스트 assertion 미실행). Code Integrity 이벤트 3033/3077이 libpq.dll 서명 수준/정책 거부를 명시했다. `initdb --version`도 같은 정책으로 종료되어 데이터 디렉터리나 SQL 실패와 구분했다. 보안 정책/서명 검증을 변경하지 않았다.
- [!] Docker 엔진 조회 실패 후 `docker desktop start --timeout 30` 1회 시도는 context deadline exceeded로 종료됐다. CLI/Desktop/backend 프로세스가 남지 않은 것을 확인했다. Docker 및 Windows PostgreSQL의 같은 시작을 무한 재시도하지 않고 승인된 Linux 격리 QA 환경으로 검증 경로를 전환해야 한다.
- 새 PG 테스트 4건을 추가했다: COLLECT_ONLY list/count와 preview 분리, ENFORCE 재예약 즉시 pending 및 OFF 유지, confirmed 태그 필터, 제목 제외 단건/목록 차단. 현재 실행 환경 차단 때문에 통과로 세지 않는다.
- 미완료 필수 경계: keyword release 교체 후 일치 정책 부재 시 신규 ENFORCE 처리, 동일 본문 재확인 최소 24시간, 배치 preview의 current 무변경, 전체 운영 profile 실파일 worker QA, 관리자 변경/UI/운영 적용/브라우저. 현재 목표는 계속 진행 중이며 완료 판정이 아니다.

### 2026-09-10 10:53 최종 로컬 검증과 재개 조건

- 24시간 자동 재확인 간격을 후속 구현했다. 진행 중 job은 먼저 재사용하고, 최근 일반 job이 있는 source는 RECHECK_NOT_DUE로 남기며 current/confirmation/version을 바꾸지 않는다. 실제 DB 테스트는 같은 시각의 두 번째 run이 작업을 생성하지 않는지 확인한 뒤 임시 fixture의 25시간 경과만 재현하도록 강화했다.
- Current 조회의 최신 일반 job 정렬을 `expected_attachment_version DESC`로 고정해 transaction 시작 시각과 완료 시각의 순서가 달라도 이전 job을 최신으로 보여주지 않도록 했다. source별 최근 시각/버전 partial index를 V73에 추가했다.
- 후속 선택 테스트: CurrentService/IntakeService/MigrationContract 78건 통과, 실패/오류/생략 0, bootJar 성공.
- 최종 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- 결과: BUILD SUCCESSFUL(2분 25초), 13 task 모두 재실행. root 135 suite/729건 중 662통과/67환경 조건 생략/실패0/오류0; 추출기 13건 전부 재실행 통과. root XML 10:53:27 KST, extractor XML 10:53:32 KST. 생략된 PG/실파일/Linux 테스트를 통과로 합산하지 않는다.
- artifact `build/libs/saneB-0.0.1-SNAPSHOT.jar` SHA-256: `0a79003af997672325a3e88ca4e00cb8be13fb9d7ac8f872aa3687f87858badd`.
- `git diff --check` 통과. 기존 V1~V72 migration 변경 없음. HEAD와 원격 master 모두 `ae893b87348a9bd1cb0893763f6cad5047093a24`; 로컬 미커밋 변경/V73는 보존했다. 커밋·push·배포·운영 DB/정책/worker 설정 변경 없음.
- 새 `announcement-attachment-qa-trace-2026-09-10.md`에 ATT-001~062 정확히 62행을 연결했다. 모든 필수 요구의 전체 계층 검증이 끝난 것은 아니며, 개별 미구현/부분/차단 상태를 유지한다.
- AWS STS는 공개 CA bundle을 사용해도 인증 갱신 필요 상태다. 원문 오류/인증값을 출력하지 않았다. GitHub repository 조회는 가능하나 현재 계정의 응답은 `pull=true`, `push=false`다. 기존 토큰을 재사용하거나 다른 계정 권한을 추측하지 않는다.
- `release-readiness-gate` 판정: **Not ready**. Linux PG/실파일 검증, 미완료 정책/변경/UI/배치 구현, 운영·브라우저 Gate를 충족해야 한다. AWS 서울 리전 재인증, GitHub 저장소 쓰기 권한 계정 인증, 실제 운영 URL/역할별 브라우저 로그인 준비가 필요하다.
- 마지막 조회에서 Java/PostgreSQL 및 이번 시도의 Docker CLI/Desktop/backend 프로세스는 없었다. 이 단계에서 브라우저 자동화·앱 서버·컨테이너를 띄우지 않았다. 이전 진단용 공개 CA/읽기 전용 요청 파일은 ignored `build/diagnostics`에 재개 증거로 보존하며 운영 secret을 담지 않는다.

### 2026-09-10 11:28 관리자 확인·DRAFT 연결 후속 검증

- `long-goal-operating-protocol`에 따라 현재 판정/버전/첨부 집합과 사람의 확인을 연결하고, 자동 판정·원문·메모·감사 metadata의 경계를 유지했다.
- 새 `AnnouncementAttachmentReviewController/Service/ServiceImpl/DAO/Mapper`와 Request/Response/VO, 순수 `AttachmentReviewAssessment`를 구현했다. GET review-context, POST confirmations, POST announcements가 실제 등록돼 있다. V1/V2의 기존 첨부 guard는 유지했다.
- 확인은 source→관련 행 순서 잠금, base/evaluation/source version/attachment version/set hash 확인, SEALED·현재 일반 작업 완료 조건을 요구한다. 실패·OCR·불확실성이 있으면 전체 수동 원문 확인과 조회한 필수 코드의 정확한 확인 집합을 요구한다. A/B 자동 판정·실패를 ACCEPTED/성공으로 덮어쓰지 않는다.
- 확인 시 첨부 버전 +1과 기존 확인 STALE를 원자적으로 저장하고, 카탈로그를 공유 잠금 아래 검증한 다중 CONFIRMED 태그를 저장한다. 같은 actor/source/정규화 요청의 UUID Idempotency-Key만 최초 응답을 재사용한다.
- DRAFT 전환은 현재 확인의 버전과 DB 확정 태그를 검증하고 기존 AnnouncementDao를 사용한다. 생성 직후 DRAFT를 검사하고 승인 요청·활성화는 하지 않는다. 같은 전환은 기존 link를 반환하고 다른/legacy 요청은 충돌로 보호한다. 연결된 source의 새 확인으로 기존 공고 태그를 덮어쓰지 않는다.
- V73(로컬 미적용)에 confirmation 버전 2컬럼, 확인 불변 trigger, source link의 confirmation/hash composite FK·불변 trigger를 추가했다. 기존 버전 없는 확인은 추측해 채우지 않으며 CURRENT projection에서 제외한다. 동일 확인의 DRAFT link가 증가시킨 1버전만 CURRENT로 유지한다. V1~V72는 변경하지 않았다.
- 대상 검증: `:test --tests '*AttachmentReviewAssessmentTest' --tests '*AnnouncementAttachmentReviewServiceTest' --tests '*AnnouncementAttachment*ControllerSmokeTest' --tests '*MigrationContractTest'`는 137건 통과/실패0/오류0/생략0. 이후 service 멱등 key 보강 3건까지 아래 전체 검증에서 실행했다.
- 신규 자동 테스트 52건(assessment 17, service 22, HTTP 13)이 실행 통과했다. READ 3역할, WRITE 2역할, APPROVER/외부역할의 변경 금지, 익명·CSRF·UUID/중첩 버전 검증, cross-source 404, stale 409, 다중 첨부 실패·수동 확인, 멱등·DRAFT만 생성 조건을 검증했다. 브라우저 실행 증거는 아니다.
- PG 테스트 6건을 추가했다: worker 근거→확인→DRAFT/동일 요청, 동시 확인, 동시 DRAFT, 새 예약 후 STALE, 실패 근거의 수동 확인, source 간 같은 idempotency key 경합과 loser transaction rollback. mapper 등록·fixture의 버전도 갱신했다. 현재 job PG 클래스는 50건이며 Windows 차단으로 **미실행**이다. migration upgrade 테스트에도 새 컬럼/trigger 검증을 추가했지만 실행 통과로 세지 않는다.
- 최종 전체 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- 결과: BUILD SUCCESSFUL(2분 37초), 13 task 모두 재실행. root 138 suite/787건 중 **714통과/73환경 조건 생략/실패0/오류0**; 추출기 **13통과/생략0/실패0/오류0**. XML 시각 root 11:28:00 KST, extractor 11:28:04 KST. 기존 MockBean deprecated/unchecked 경고는 남아 있으나 실패가 아니다.
- artifact `build/libs/saneB-0.0.1-SNAPSHOT.jar` SHA-256: `10f385f14b512a2836cf5a06519b92e6031ff02a6ec59fabe61a18dfc82a169f`.
- `git diff --check` 종료 0. 변경 텍스트 111개 고위험 자격증명 패턴 검사 0건(모든 보안 문제 부재의 증거로 확대하지 않는다). ATT 추적표 62개 고유 요구 행 유지. DB/API 계약과 추적표를 실제 추가 코드에 맞춰 갱신했다.
- 11:28:34 KST 원격 master 재조회와 로컬 HEAD는 `ae893b87348a9bd1cb0893763f6cad5047093a24`다. 로컬 수정/V73는 미커밋 상태로 보존했다. 이 단계는 커밋·push·운영 migration·worker 활성화·정책·기존 데이터·배포·브라우저를 실행하지 않았다.
- `release-readiness-gate`: **Not ready**. 최신 PG/Flyway·Linux 실파일 경로, 아직 없는 재시도/역할/정책/배치/UI, 전체 profile·운영·브라우저 필수 Gate가 남아 있다. 앞선 AWS 인증 갱신 필요/GitHub push=false/정식 URL·역할 로그인 미확인 상태를 새 정상 증거로 대체하지 않았다.
- 일회성 Node 감사 프로세스는 종료됐다. 최종 조회에 Java/PostgreSQL 프로세스 0개였다. 이번 단계는 브라우저·컨테이너·상시 서버를 시작하지 않았다.
- 다음 코드 작업은 재시도·파일 역할 변경을 새 set/판정/STALE로 연결하는 변경 API다. 정책/배치/API 완료 후 한국어 관리자 UI와 실제 운영 E2E를 이어간다. 필수 범위는 축소하지 않는다.

### 2026-09-10 12시 역할 변경·자동 재시도 중간 저장

- Gate 1~4는 부분 완료를 유지한다. 역할 변경 PUT 202와 작업 GET을 추가하고, 새 SEALED set/파일/extraction·새 generation, 이전 확인 STALE, actor-bound 멱등성과 404/409/CSRF를 연결했다. 기존 원본 역할/텍스트/실패/추출 시각을 수정하지 않는다. ROLE_CHANGE는 외부 HTTP/추출 자원 예약이 불가능하며 임시 저장소 장애나 OFF에서도 봉인된 근거의 CPU 재평가는 진행할 수 있다.
- CurrentMapper가 V72에 없는 `last_set_id`를 참조하던 실제 SQL 계약 오류를 `set_id`로 수정했다. 앞선 HTTP/unit 통과가 DB 컬럼 일치를 증명하지 않았음을 기록한다. 새 오프라인 MyBatis XML/namespace/parameter 검증 3건을 추가했으며 PostgreSQL 실행 증거와 구분한다.
- 같은 job의 일시 네트워크 재시도가 성공 파일까지 다시 내려받던 문제에 `announcement_attachment_file_checkpoints`를 추가했다. COMPLETE_TEXT만 내부 중간 저장하고 원본 binary는 저장 전에 삭제한다. 다음 lease/worker는 같은 job·locator의 성공 근거와 최초 추출 시각을 재사용한다. 새 job은 동일 URL도 다시 다운로드한다. 최종 set 봉인 전 checkpoint는 사용자 API에 나타나지 않는다.
- checkpoint는 파일당 JSON 16 MiB, 10개 파일×3시도 중 목록 변경까지 최대 30개로 제한한다. source/job composite FK, 유효 RUNNING COLLECT 조건, UPDATE 금지, 봉인/종료/충돌/취소/삭제 시 정리 계약을 V73에 추가했다. 일시 RETRY_WAIT에는 유지하며 누적 다운로드 bytes는 환급하지 않는다.
- 역할 service 16/HTTP 11건과 worker 16/MigrationContract 67건의 선택 검증 110건이 먼저 통과했다. checkpoint service 15·worker 19·MigrationContract 68건의 후속 선택 검증 102건도 통과했다. mapper binding 3건 추가 후 합계 105건 후속 선택 검증이 통과했다. HTTP/다운로드 mock 검증은 운영 실행으로 계산하지 않는다.
- 검증 중 테스트 fixture의 존재하지 않는 helper 호출 1건을 실제 `selectRequest()`로 수정했다. mapper binding 첫 실행은 OffsetDateTime이 Object로 해석되는 assertion 1건에서 실패했고 JDBC Java type을 명시한 후 통과했다. 실패를 삭제하거나 assertion을 약화하지 않았다.
- 신규 PG 역할 변경/동시 요청/추출 재사용 4건, checkpoint lease 복구·source 격리/종료 정리·불완전 추출/건수 상한 3건을 추가했다. 현재 `AnnouncementAttachmentJobIntegrationTest`는 **57건**이며 컴파일만 확인했고 실제 실행은 아직 차단이다. 같은 Windows initdb/Docker 시작을 반복하지 않았다. WSL 읽기 전용 목록은 docker-desktop뿐이므로 관리 distro를 임의 테스트 서버로 사용하지 않았다.
- 관리자 수동 실패 파일 재시도(`POST /attachment-jobs`), 정책/배치/이력 API, 전체 profile, UI는 아직 남아 있다. 이번 자동 재시도 checkpoint를 봉인된 부분 실패 세트의 수동 재시도까지 완료한 것으로 표현하지 않는다.
- API 24.3/24.4, DB 11.2, ATT 추적표를 갱신했다. 모든 ATT 62개 고유 행을 유지하며 ATT-039는 코드/단위 검증 부분 완료로 표시했다. 작업 범위의 고위험 자격증명 패턴 검사 124개 텍스트/0건 일치; 금지 MyBatis 패턴 검사도 일치 없음. 검사 범위 밖까지 보안 문제 부재로 확대하지 않는다.
- 12시 원격 master 실조회와 로컬 HEAD는 `ae893b87348a9bd1cb0893763f6cad5047093a24`로 일치했다. V1~V72 수정 없음. 이번 단계도 커밋·push·운영 DB/정책/worker 설정·배포·브라우저를 실행하지 않았다. 앞선 운영 인증/권한/URL 차단을 해소한 새 증거는 없다.

#### 12:10 최종 로컬 회귀

- 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- BUILD SUCCESSFUL(2분 16초), 13 task 모두 재실행. root **142 suite/847건: 767통과·80조건부 생략·실패0·오류0**. extractor **13통과·생략0·실패0·오류0**. root XML 2026-09-10 12:10:33 KST, extractor XML 12:10:36 KST.
- 80개 생략에는 첨부 job PG 57, 첨부 migration 2, Flyway integration 3, Linux 격리·실파일·규칙 snapshot 각 1, 그 밖의 환경 조건 통합 검증 15개가 포함된다. 생략 항목은 성공으로 합산하지 않았다. 최신 `attachmentJobIntegrationTest`/`attachmentMigrationTest`/`flywayIntegrationTest`의 실제 Linux 실행은 남아 있다.
- `build/libs/saneB-0.0.1-SNAPSHOT.jar` SHA-256: `e21e616b55f960623172629d7219d8aa8e050f782d696b3c69e9f0a8a1b2e857`.
- 최종 `git diff --check` 통과. 기존 V1~V72 수정 없음. V73과 작업 변경은 미커밋 보존. 직접 실행한 일회성 Node와 single-use Gradle daemon은 종료됐다. 최종 조회에서 Java/PostgreSQL 프로세스는 없었고, 기존 사용자 Node 프로세스는 종료하지 않았다.
- 중간 진단에서 명령 한 번에만 `core.autocrlf=false`를 지정한 검사는 기존 CRLF 전체를 trailing whitespace로 보고했다. 파일/전역 설정을 변경하지 않고 저장소의 기존 `core.autocrlf=true` 기준으로 재실행해 exit 0을 확인했다. 해당 출력은 기능/테스트 실패와 구분한다.
- `release-readiness-gate`: **Not ready**. 필수 DB/Linux·전체 profile·수동 재시도/정책/배치/UI·운영/브라우저 Gate를 면제하지 않는다. 브라우저는 사용자 미승인 때문이 아니라 화면/운영 접근과 선행 구현이 남아 미실행이다.
- 다음 구현은 봉인된 부분 실패 세트의 성공 근거를 보존하는 관리자 수동 재시도 예약이다. 이후 정책/배치/이력 API·한국어 화면·운영 검증으로 이어간다. 목표는 active이며 완료로 표시하지 않는다.

### 2026-09-10 12:44 선택 실패 파일 수동 재시도 후속 검증

- POST attachment-jobs의 실패 파일 선택 재시도 202와 `RETRY_FILES` 작업을 구현했다. 버전/현재 SEALED set/ACTIVE 정책·설치 profile/선택 1~10개 파일/누적 80 MiB 이하 예산을 검증한다. 초기 source별 60초·24시간 3회 한도, actor-bound 멱등성, 잘못된 파일 404/변경 409/한도 429를 추가했다. 임의 URL·profile을 요청으로 받지 않는다.
- V73 미적용 migration에 불변 선택 파일 범위와 composite FK·PENDING/실패 파일/최소 1개 deferred 검증을 추가했다. 선택하지 않은 전체 성공/실패 근거와 MANUAL 역할·최초 추출 시각을 보존하고 선택 파일만 새 generation에서 다시 처리한다. source 삭제 cascade를 막지 않도록 부모 생존 여부를 함께 확인한다.
- 재발견한 전체 locator 집합이 바뀌면 범위를 확장하지 않고 binary 요청 없이 발견 실패/미완료 근거를 남긴다. 여러 선택 파일의 일시 재시도는 이번 job에서 성공한 파일 checkpoint를 재사용한다. 원래 실패/부분/OCR 상태를 정상 성공으로 승격하지 않는다. 일반 COLLECT 전체 저장으로 선택 범위를 우회할 수 없다.
- 새 service 16, evidence 5, HTTP 9, worker 4건을 추가했다. 마지막 worker 검증은 두 선택 실패 중 한 파일이 먼저 성공한 뒤 다음 lease에서 그 성공을 재사용하는 흐름이다. PG 3건(정확한 선택 근거·추출 시각/삭제 정리, 동일 key 동시 예약, 범위 불변/성공 파일 추가 금지)을 추가했으며 현재 PG 전체 60건은 컴파일만 확인했다.
- 첫 전체 회귀(12:39)는 886건 중 1건 실패/83생략이었다. 기존 CSRF smoke 테스트가 새 등록된 POST에 여전히 404를 기대한 것이 원인이며 필수 입력 검증 400 기대값으로 수정했다. CSRF 누락의 403 assertion은 그대로 유지했다.
- 재실행 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- BUILD SUCCESSFUL(2분 48초), 13 task 재실행. root 145 suite/886건 중 **803통과·83생략·실패0·오류0**; extractor **13통과·생략0·실패0·오류0**. XML 시각 root 12:44:15 KST, extractor 12:44:19 KST. 조건부 생략 83건에는 최신 job PG 60건이 포함되며 성공으로 합산하지 않는다.
- artifact SHA-256: `e1a1fd26fb8e204433e1901df806096eb0252a370ec940535bbadc9304b72acd`. 이 결과는 다음 코드 변경 이전의 로컬 증거이며 최신 운영 배포 증거가 아니다.
- API 24.5/DB 11.3/ATT 추적표를 실제 구현에 맞춰 갱신했다. 초기/전체 수동 수집, 정책/배치/이력 API·관리자 UI·전체 profile·실파일 Linux·운영·브라우저는 남아 있다. Gate는 부분 완료/Not ready이며 이 단계에서 커밋·push·배포·운영 설정·데이터를 변경하지 않았다.

### 2026-09-10 13시 판정 이력·근거 조회와 노출 차단

- `AnnouncementAttachmentHistoryController/Service/ServiceImpl/Dao/Mapper`, 조회 DTO/VO를 추가했다. GET history/detail/inputs/matches 4경로는 READ 3역할·pagination·no-store·다른 source/evaluation/file 404를 적용한다. 기존 `/api/v1`과 현재 종합 판정 응답은 유지했다.
- 목록/상세는 같은 REPEATABLE_READ snapshot의 현재 projection으로 CURRENT_EFFECTIVE/CURRENT_PREVIEW/NOT_CURRENT를 구분한다. 현재 flag만 보고 preview를 effective로 승격하지 않는다. 당시 base/set/policy/release와 AUTO 태그를 보존하고 현재 CONFIRMED 태그와 분리한다.
- 입력 목록은 당시 exact extraction ID를 사용하며 추출 없는 다운로드 실패도 count/목록에서 제외하지 않는다. matches는 frozen term/action/file/extraction/block/code-point 좌표만 반환한다. 전체 추출문/URL/lease/검수 메모를 읽지 않고 제한된 block API로 연결한다. V73에 source/evaluated_at/id 이력 정렬 index를 추가했다.
- 기존 ReadService가 source 존재 여부만 확인하던 조회 경계를 보강했다. 실제 SQL은 제외된 잔존 source도 반환할 수 있으므로 제목 제외·QA·기본/제목 판정 미완료 source는 set/file/block 읽기 전에 차단한다. 이 발견은 로컬 코드상 누락이며 실제 운영 유출을 확인했다는 뜻은 아니다. 집합/파일에도 명시적 no-store를 적용했다.
- History service 15건/HTTP 10건, ReadService 11건, mapper binding 1건, migration index 계약 1건을 추가했다. 기존 READ HTTP 14/mapper 전체 5/MigrationContract 전체 70을 합한 선택 회귀는 **125건 통과·실패0·오류0·생략0**, bootJar 성공이다.
- 선택 회귀 첫 실행의 1건 실패는 존재하지 않는 extraction의 SQL 결과 null 대신 Mockito가 Long 기본값 0을 반환한 fixture 문제였다. null을 명시해 다른 source/extraction 404 assertion을 유지했고 전체 선택 회귀가 통과했다.
- PG 3건을 추가했다: 실패 입력/고정 키워드 좌표/조회 무변경, 새 generation 뒤 과거 입력 보존·교차 source/판정 파일 거부, COLLECT_ONLY preview 및 제목 제외 source의 모든 근거 조회 거부. 현재 PG 63건은 컴파일 검증이며 실제 Linux 실행이 필요하다.
- 13:00 원격 master와 HEAD는 `ae893b87348a9bd1cb0893763f6cad5047093a24`로 일치했다. 일회성 Node 점검: 변경 텍스트 144개/고위험 자격증명 패턴 0/금지 Mapper 패턴 0/ATT 62개 고유 항목/git diff --check 0/V1~V72 변경 0. 제한된 패턴 검사이므로 전체 보안 보증으로 확대하지 않는다. Node는 종료됐다.
- API 24.6/DB 11.4/ATT 연결표를 갱신했다. 이번 단계는 커밋·push·운영 migration·정책/기존 데이터 적용·배포·브라우저를 실행하지 않았다. 다음 구현은 초기/전체 수동 수집과 정책/배치 관리 계약이며 관리자 UI·전체 profile·Linux 실파일/운영/브라우저 Gate도 그대로 남는다.

#### 13:04 최종 로컬 회귀·자원 확인

- 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- BUILD SUCCESSFUL(2분 52초), 13 task 재실행. root **148 suite/927건: 841통과·86조건부 생략·실패0·오류0**, extractor **13통과·생략0·실패0·오류0**. root XML 13:03:54 KST, extractor XML 13:03:59 KST.
- 생략 86건: job PG 63, 첨부 migration 2, Flyway integration 3, Linux 격리/실파일/규칙 snapshot 각 1, 기타 환경 조건 통합 15. 이들 테스트는 최신 코드에서 실제 실행해야 하며 기존 통과나 XML parsing으로 대체하지 않는다.
- 최종 artifact `build/libs/saneB-0.0.1-SNAPSHOT.jar` SHA-256: `29cca0b74eadfab842d982ef4e26885bb11735d5f6155fe40c242ea36fe7523d`.
- 최종 HEAD/원격 master 실조회는 `ae893b87348a9bd1cb0893763f6cad5047093a24`, branch master, 변경 경로 144개/스테이징 0개다. 로컬 변경과 V73를 보존했고 커밋·push·운영 반영은 하지 않았다. `git diff --check` 종료 0; 경고는 기존 autocrlf 정책의 LF/CRLF 안내이며 실패가 아니다.
- 마지막 조회의 Java/PostgreSQL 프로세스는 0개다. 일회성 Node 진단은 종료됐으며 기존 사용자 Node 프로세스는 건드리지 않았다. 이번 단계에서 브라우저·컨테이너·상시 앱 서버를 시작하지 않았다.
- `long-goal-operating-protocol`에 따라 단위/HTTP/XML/실제 DB/운영 증거를 분리했고, `release-readiness-gate` 최종 판정은 **Not ready**다. 전체 profile·초기/전체 수동 수집·정책/배치/UI·최신 Linux DB/실파일·운영/브라우저 필수 Gate가 남아 있으므로 장기 목표는 active로 유지한다.

### 2026-09-10 13:30 최초 수집·전체 재수집 API

- GET `attachment-collection-context`와 POST `attachment-jobs/collection`을 추가했다. 기존 POST `attachment-jobs`의 선택 실패 파일 재시도 계약은 보존한다. 최초 source의 attachment decision은 null을 허용하고, source/base/attachment 버전·정책/실행 hash·예산을 확인한 뒤 새 COLLECT 작업을 예약한다.
- GET은 REPEATABLE_READ 읽기 전용 transaction이며 정책 `FOR SHARE`를 사용하지 않는다. POST는 source 잠금 이후 ACTIVE 정책/규칙 공유 잠금을 사용한다. profile은 시스템 registry와 source locator로 유일하게 결정하고 요청에 URL·파서·명령을 받지 않는다. 조회 결과는 Linux 추출기 가동 성공의 증거가 아니다.
- 수동 최초/전체 수집과 선택 실패 파일 재시도는 source별 60초 간격·최근 24시간 3회 한도를 공유한다. 10개 파일·3시도·redirect 상한 기준 최대 132 HTTP 요청과 정책 이하 최대 80 MiB 예산을 context로 제공한다. 같은 actor/key/정규화 요청은 기존 작업을 반환해 한도를 다시 소모하지 않는다.
- 전체 재수집은 모든 파일을 다시 발견하고 새 profile 역할을 부여한다. 기존 MANUAL 역할은 과거 set에 보존하며 새 set으로 자동 복사하지 않는다. 선택 실패 파일 재시도는 선택하지 않은 파일의 근거/역할을 보존하므로 두 행동을 구분한다.
- 기존 ENFORCE 검수 의무만 유지하고 새 검수 binding이나 운영 활성화를 만들지 않는다. 구버전/변경 정책·profile·전역 OFF·연결 공고·진행 중 작업은 거부한다. 예약 성공 시 attachment version 증가와 과거 확인 STALE·QUEUED를 같은 transaction에서 저장한다.
- 신규 Collection service/HTTP 테스트와 공유 rate SQL/부분 index 계약, 초기 null 제목 방어를 추가했다. 첫 선택 회귀는 Mockito 재설정 과정에서 null 인자를 처리하던 fixture 1건으로 실패했다. `doReturn`으로 fixture를 바로잡고 assertion을 유지한 재실행은 통과했다.
- PG 4건을 추가했다: read-only 최초 context/미적용 ENFORCE preview, 전체 수집·선택 재시도 공유 한도/guard, 동시 같은 key 단일 job/버전, 조건 변경 후 쓰기 없음. 최신 job PG **67건**은 컴파일 검증이며 실제 실행 성공으로 계산하지 않는다.
- 전체 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- Gradle daemon 로그의 **BUILD SUCCESSFUL in 3m 54s**와 새 XML을 확인했다. root **150 suite/971건: 881통과·90조건부 생략·실패0·오류0**; extractor **13통과·생략0·실패0·오류0**. root XML 13:29:52 KST, extractor XML 13:29:56 KST. build 완료 뒤 daemon contention handler 종료 경고가 있었으며 Java/PostgreSQL 프로세스는 종료됐다.
- 생략 90건: job PG 67, 첨부 migration 2, Flyway integration 3, Linux 격리/실파일/규칙 snapshot 각 1, 기타 조건 통합 15. 기존 Windows 차단을 반복 시도하거나 격리를 해제하지 않았다.
- artifact `build/libs/saneB-0.0.1-SNAPSHOT.jar` SHA-256: `049fbb3eb0d59fb34b9154913f40bdbbf390462e1e1a34ba784f14ef349d2db5`.
- API 24.7/DB 11.5와 ATT 추적표를 갱신했다. V1~V72는 보존하고 미적용 V73에 공유 수동 요청 partial index만 추가했다. 이번 단계에서 커밋·push·운영 DB/정책·기존 데이터·배포·브라우저는 실행하지 않았다. 전체 Gate는 **Not ready**이며 정책/배치/UI·전체 profile·Linux/운영/브라우저 구현과 검증을 계속한다.

### 2026-09-11 10:47 중단 작업 재개: 규칙·정책 불일치와 Linux QA 경로

- 시작 시 루트 AGENTS.md와 `long-goal-operating-protocol`을 다시 읽고 기존 미커밋 변경을 보존했다. branch/master HEAD와 원격 master는 `ae893b87348a9bd1cb0893763f6cad5047093a24`로 유지됐다. 기존 별도 Java 프로세스는 종료하지 않았다.
- 이전 중단 직전 추가한 ENFORCE 불일치 방어를 검증했다. 같은 ACTIVE keyword release의 정책이 없고 다른/퇴역 keyword release의 ACTIVE ENFORCE 정책이 있으면 Provider 목록 요청 전에 중지한다. 수집 context 조회 뒤 같은 규칙이 퇴역하는 경합도 NOT EXISTS 조건으로 포함했다.
- 기존 FROZEN/OFF 계획과 초기 NO_POLICY, 명시적인 일치 COLLECT_ONLY/OFF, worker 연동 비활성 동작은 보존한다. 구버전 정책을 새 source에 자동 적용하거나 source 검수 binding을 바꾸지 않는다. 기존 NO_POLICY 재개도 미일치 ENFORCE가 나타나면 원래 계획을 덮어쓰지 않고 차단한다.
- source service는 고정 ErrorCode만 구분하여 기존 run 결과를 `FAILED/totalCount=0/failedCount=1`로 저장한다. 외부 목록/본문/첨부 요청과 source/job 쓰기가 없음을 Mockito 회귀로 확인했다. 오류 안내는 현재 ACTIVE 규칙의 정책 검증·게시 후 새 수집을 요구하며 원문 exception 메시지를 출력하지 않는다.
- Intake/Mapper/Source service 선택 검증 43건 통과 후, workflow 구조 3건을 포함한 46건도 통과했다. PG 4건을 추가해 최신 job DB 클래스는 71건이다. 전체 compile 첫 시도의 AssertJ/TransactionTemplate 제네릭 타입 추론 오류를 UUID 지역 변수로 수정했다. assertion 삭제·완화는 하지 않았다.
- 새 `.github/workflows/attachment-contract-qa.yml`은 `workflow_dispatch` 전용, contents read, Ubuntu 22.04/Java 21, 단일 Gradle worker/30분 제한이다. AWS/운영 DB/정책/배포를 호출하지 않고 테스트 소유 loopback PostgreSQL의 job/migration 검증과 root/extractor 회귀를 실행한다. 실제 원격 workflow는 아직 실행하지 않았다.
- `scripts/qa/attachment-contract-report.mjs` 및 테스트는 필수 DB 보고서 누락·다른 suite·0건·실패·오류·skip·이번 실행보다 오래된 보고서를 거부한다. Node 자체 테스트 9건 통과. 시작 시각 없이 CLI를 실행한 음성 검증은 예상대로 exit 1이었다. 이것은 DB 테스트 통과가 아니다.
- 최종 Java 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- 결과: **BUILD SUCCESSFUL in 2m 9s**, 13 task 재실행. root **151 suite/992건: 898통과·94조건부 생략·실패0·오류0**, extractor **13통과·생략0·실패0·오류0**. root XML 10:47:02 KST, extractor XML 10:47:05 KST. MockBean deprecated/unchecked 경고는 남아 있다.
- 94개 생략은 job PG 71, migration 2, 별도 Spring Flyway integration 3, Linux/실파일/규칙 snapshot 각 1, 기타 조건부 통합 15다. 최신 PG/Flyway/Linux 실파일을 실제 실행한 것으로 계산하지 않는다.
- artifact `build/libs/saneB-0.0.1-SNAPSHOT.jar` SHA-256: `815e2bf738016e903fe690093d5c74de7369217a84ee37ddd4812fb488314902`.
- 오늘 읽기 전용 진단에서도 Docker 엔진은 불가, WSL은 docker-desktop뿐이며 별도 설치 PostgreSQL 명령을 찾지 못했다. 관리 distro 사용·Docker 반복 시작·Code Integrity 해제는 하지 않았다. GitHub permission은 pull=true/push=false였고, 공개 CA를 명령 범위에서 사용한 서울 리전 STS는 실패했다. 이번 STS 실패의 세부 원인은 확정하지 않는다. 임시 환경변수는 복원했고 응답 원문/자격증명은 남기지 않았다.
- API 24.8/DB 11.6/ATT 추적표와 `announcement-attachment-linux-contract-qa-2026-09-11.md`에 재개 경로를 기록했다. `release-readiness-gate`는 **Not ready**다. 정책/배치/UI·전체 profile·최신 실제 DB/실파일·운영·브라우저가 필수 미완료이며 범위를 축소하지 않는다. 커밋·push·원격 workflow dispatch·운영 데이터/설정·배포·브라우저는 실행하지 않았다.
- 최종 읽기 전용 점검: 변경 텍스트/경로 158개, staged 0개, 기존 V1~V72 변경 0개, `git diff --check` exit 0, ATT 행 62개/고유 ID 62개, 한정된 고위험 자격증명 패턴 및 금지 Mapper 패턴 각각 0건. 이는 전체 보안 감사 통과를 뜻하지 않는다. 직접 실행한 Gradle/Node QA는 종료됐고 시작 전 존재하던 Java PID 20900 등 사용자 자원은 보존했다.

### 2026-09-11 11:31 정책 초안·개정 API와 로컬 회귀

- Gate 4 진행, 전체 **Not ready**. 상세 설계의 정책 수명주기 중 초안 조회/생성/수정/개정만 구현했다. 검증·게시를 성공처럼 반환하는 임시 endpoint는 만들지 않았다. 실제 QA 증거와 설치 런타임에 결합된 validation/publication, 관리자 UI·배치·전체 profile·운영/브라우저는 여전히 필수 작업이다.
- `AnnouncementAttachmentPolicyController → Service → ServiceImpl → DAO → AnnouncementAttachmentPolicyMapper.xml`과 요청/응답 DTO를 추가했다. READ는 ADMIN/OPERATOR/APPROVER, 모든 변경은 ADMIN만 허용하고 직접 service 호출에서도 활성 계정·비밀번호 변경 완료를 확인한다. 읽기 전용 역할의 isEditable은 false다.
- POST 생성/개정은 UUID Idempotency-Key와 actor/정규화 입력/operation을 결합한다. 이미 편집된 동일 요청은 같은 정책의 현재 상태를 돌려준다. PUT은 DRAFT·조회 rowVersion CAS만 허용한다. 게시/퇴역 원본은 직접 수정하지 않고 같은 family의 새 versionNo로 복사한다. 원본 설정은 복사하되 policyHash/게시 시각/QA 성공은 이어받지 않는다.
- 미적용 V73에 최초 요청 식별자/개정 parent, self FK/인덱스/완성된 생성 metadata CHECK, 동일 family·후속 version 검증과 identity 불변/정확한 rowVersion+1 trigger를 추가했다. 기존 V1~V72 파일은 변경하지 않았다. 요청 key와 family별 transaction advisory lock·행 잠금으로 중복 생성/개정 번호 경합을 분리한다.
- 관리자 입력은 규칙·모드·공고당 다운로드 한도(1~80 MiB)·조회 버전·사유다. URL/parser/profile/settings JSON/임의 실행값/게시·검증 성공값 등 정의하지 않은 필드는 HTTP 400으로 거부한다. 생성/수정은 서버 registry와 엔진/추출기 버전을 사용하며 설치 런타임 hash는 실제 검증 전 null이다. 설정/사유 원문은 감사 metadata에 복사하지 않는다.
- 정책 service **26건**, 정책 HTTP **25건**과 추가 Mapper/MigrationContract 검증을 실행했다. PostgreSQL fixture **7건**을 추가하여 현재 job DB 테스트는 **78건**이다. 이 78건은 컴파일만 확인했으며 Linux에서 실행해야 한다. Windows initdb/Docker 시작이나 OS 보안 해제를 반복하지 않았다.
- 중간 실패: HTTP fixture의 int/Long 불일치, 통합 fixture의 잘못된 예외 getter를 실제 타입/API로 수정했다. 첫 전체 회귀는 새 정책 route에도 404를 기대하던 기존 CSRF smoke 1건에서 실패했다. 등록된 handler의 필수 입력 누락 400으로 갱신했으며 CSRF 누락의 403 assertion과 신규 권한/금지 필드 assertion은 유지했다.
- 최종 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- **BUILD SUCCESSFUL in 2m 56s**, 13 task 재실행. 최종 XML 집계 root **153 suite/1053건: 952통과·101조건부 생략·실패0·오류0**, extractor **13통과·생략0·실패0·오류0**. XML 시각 root 11:31:06 KST, extractor 11:31:09 KST. 기존 MockBean deprecated/unchecked 경고는 남아 있다.
- 생략 101건: job PG 78, attachment migration 2, Spring Flyway integration 3, Linux 격리/실파일/규칙 snapshot 각 1, 기타 조건부 통합 15. 이 조건부 생략이나 과거 DB/서버 CLI 기록을 현재 SQL·worker·운영 성공으로 합산하지 않는다.
- artifact `build/libs/saneB-0.0.1-SNAPSHOT.jar` SHA-256: `aa310695d7cb1d4124ad980f0411b7e823595aa2967d95531e741f8d0be84bb6`. 이것은 로컬 artifact이며 운영 설치된 SHA가 아니다.
- Node 보고서 판정기 자체 테스트 **9통과**, 수정 문서 5개 code fence 균형 검사 통과. API 24.9/DB 11.7/ATT 추적표/Linux 재개 문서를 갱신했다. ATT 62행/고유 ID 62개를 유지한다.
- 최종 로컬 HEAD와 11시 원격 master 실조회는 `ae893b87348a9bd1cb0893763f6cad5047093a24`로 일치했다. 변경 경로 168개/staged 0, `git diff --check` exit 0, 기존 migration 변경 0, 한정된 텍스트 자격증명 패턴 168개 파일/0건 일치, 첨부 Mapper 금지 패턴 0건이다. 전체 보안 감사 통과를 의미하지 않는다.
- 이번 GitHub 읽기 전용 권한 조회도 pull=true/push=false다. AWS STS는 이번 구간 재시도하지 않았으며 앞선 실패를 해결한 새 증거가 없다. 커밋·push·원격 workflow·배포·운영 정책/데이터 변경·브라우저를 실행하지 않았다. 브라우저는 사용자 승인이 없는 것이 아니라 선행 화면·운영 접근/세션 준비가 남았다.
- 직접 실행한 Node QA와 single-use Gradle daemon/worker가 종료됐으며 owned QA process 0개를 확인했다. 이전부터 실행 중인 사용자 Java PID 20900과 사용자 Node는 보존했다. 다음 구현은 초안을 실제 서버 QA 증거에 결합하는 검증·게시 계약이며, 운영 게시/ENFORCE/일괄 적용은 구체적 범위 승인 이후 수행한다.

### 2026-09-11 11:59 정책 분류 정답 세트 실행·이력

- Gate 4 진행, 전체 **Not ready**. 직전 구간은 초안 API/DB 계약과 검증 기록이 반영된 progress로 분류했다. 이번에는 분류 검증을 실제 서버 실행과 버전 고정 이력까지 연결했다. 전체 validation/publication·실파일·worker QA·UI·배치를 분류 성공으로 대체하지 않는다.
- `AnnouncementAttachmentPolicyGoldenGate`는 현재 RuleSet으로 AG-001~030을 실행한다. 제목 제외·미충족 우회, 첨부 A/B 근거·검수, 역할/부분/OCR/발견 실패, 파일·문단 간 AND, Provider 독립성, code point 위치를 검사한다. 입력/출력 원문은 반환하지 않고 suite/엔진/규칙 내용·결과 hash와 사례 ID만 기록한다. 이 AG 사례는 ATT-001~062의 전체 파일/DB/운영 요구가 아니다.
- `AnnouncementSourceRuleReleaseService.selectRuleValidationDetails`를 추가해 규칙 행의 일관된 조회·계산 snapshot hash와 저장된 게시 hash를 구분한다. 새 정책 분류 check는 DRAFT 정책과 DRAFT/ACTIVE 규칙만 허용하고 ACTIVE 저장 지문 불일치를 거부한다. 기존 v1 API는 바꾸지 않았다.
- `POST/GET /api/v2/admin/announcement-attachment-policies/{policyId}/classification-checks`와 Controller/Service/ServiceImpl/DAO/Mapper/DTO를 구현했다. ADMIN 실행·READ 3역할·직접 service 권한·CSRF·UUID 멱등 키·금지 입력 400·구버전 409를 검증했다. 요청이 passed/사례 수/규칙/실행 hash/URL을 지정할 수 없다.
- 읽기 snapshot transaction 종료 후 30개 분류를 실행하고, 최종 짧은 transaction에서 key → 규칙 → 정책 잠금 및 정확한 입력 대조를 수행한다. 같은 key는 한 이력/감사만 저장한다. 정책이나 규칙이 바뀌면 과거 성공 이력은 보존하고 isCurrent=false로 반환한다. isCurrent는 입력 버전 일치일 뿐 설치 runtime/전체 QA 상태가 아니다.
- V73의 `announcement_attachment_policy_checks`는 CLASSIFICATION_GOLDEN만 저장한다. 정책/규칙/actor FK·멱등 키 UNIQUE·hash/사례 JSON CHECK·관련 인덱스·정확한 DRAFT/규칙 버전 INSERT trigger·이력 UPDATE 금지를 추가했다. 검증 이력이 있는 정책 삭제는 FK로 보호한다. 정책 hash·설치 runtime hash·상태·rowVersion·source/job·검수 binding은 이 동작으로 바뀌지 않는다.
- GoldenGate 단위 **10건**, CheckService 단위 **18건**, CheckController HTTP **18건**, 추가 Mapper/MigrationContract 선택 검증을 통과했다. PG fixture **3건**(실제 DB seed 규칙/미게시 유지, 동시 key 저장, STALE/불변/잘못된 버전)을 추가했다. 최신 PG 총 **81건은 컴파일·조건부 생략 상태**이며 실제 Linux 실행이 남아 있다.
- 최초 GoldenGate 선택 실행은 본문 표본이 제목과 동일해서 SETUP 5건이 실패했다. 기존 엔진의 제목=본문 미확보 정책을 변경하지 않고 본문을 실제 설명 문구로 고쳐 10건 통과했다. 성공 판정을 만들기 위해 assertion이나 기존 엔진 정책을 약화하지 않았다.
- 최종 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- **BUILD SUCCESSFUL in 3m 7s**, 13 task 재실행. root **156 suite/1104건: 1000통과·104조건부 생략·실패0·오류0**, extractor **13통과·생략0·실패0·오류0**. XML 시각 root 11:59:33 KST, extractor 11:59:36 KST. 기존 deprecated/unchecked 경고는 남아 있다.
- 생략 104건: job PG 81, attachment migration 2, Spring Flyway integration 3, Linux 격리/실파일/규칙 snapshot 각 1, 기타 조건부 통합 15다. 실제 실행하지 않은 항목을 성공으로 합산하지 않았다. Node 보고서 판정기 9건도 통과했다.
- 로컬 JAR SHA-256: `3515506215b8586f282ec18db0090dcf6beda10cb1fcbdb01ec6706334f901f8`. 로컬/원격 master는 12:01 실조회에서도 `ae893b87348a9bd1cb0893763f6cad5047093a24`다. 현재 배포 SHA나 운영 설치 artifact로 표현하지 않는다.
- 수정/미추적 텍스트 184개, staged 0, 기존 V1~V72 변경 0, diff --check exit 0, 제한된 자격증명 패턴/첨부 Mapper 금지 패턴 각각 0건 일치다. 전체 보안 감사나 migration 실행 성공을 의미하지 않는다. 직접 실행한 Node/Gradle QA는 종료됐으며 기존 사용자 Java PID 20900/Node는 보존했다.
- 12:01 GitHub 권한은 pull=true/push=false다. AWS/DB/운영 health·브라우저는 이번 구간 재조회하지 않았고 이전 차단을 해결한 새 증거가 없다. 커밋·push·원격 workflow·배포·운영 정책/데이터 변경은 실행하지 않았다. 브라우저는 승인되어 있지만 필수 UI·운영 URL/세션 준비와 배포가 남았다.
- API 24.10/DB 11.8, ATT 추적표, Linux 재개 문서와 `announcement-attachment-policy-validation-2026-09-11.md`를 갱신했다. 다음은 실제 설치 runtime/실파일/worker QA 증거를 동일 정책 입력에 결합하는 전체 validation과 영향 범위 확인/publication이다. 분류 check만으로 게시하거나 남은 목표를 생략하지 않는다.

### 2026-09-11 12:40 단건 첨부 검수 UI와 로컬 브라우저

- Gate 4 진행, 전체 **Not ready**. `long-goal-operating-protocol`, `ui-ux-operating-principles`, `frontend-ui-engineering`, `browser-qa`, `release-readiness-gate` 기준으로 근거/입력 보존/명시적 영향 확인을 우선했다. 신규 프론트엔드 라이브러리를 도입하지 않았다.
- `/app/admin/collected-announcements/{sourceId}/attachments` 화면과 기존 검수 상세 진입 링크를 추가했다. 현재 기본/적용/미리보기, 집합/파일/문단, 분류 이력·일치 좌표, 관리자 다중 분류 확인, 별도 비활성 초안 생성 폼을 연결했다. 정책 편집·전체 validation/게시·수집/재시도/역할 변경·배치 UI는 아직 필수 미완료다.
- review-context의 `confirmedClassification`은 정확한 source/evaluation/set/version에 결합된 확인과 저장된 태그만 반환한다. 이미 연결된 원문은 `linkedAnnouncement`를 반환하고 재확인/중복 생성 UI를 잠근다. API 24.2에 명시했다. 기존 migration/SQL 계약은 변경하지 않았다.
- `saneb-attachment-review-core.js`에 현재 버전/확인 판단, no-store JSON 통신, 결과 미확정 재시도 상태, Unicode 코드포인트 강조를 분리했다. 본문은 textContent/text node로만 표시하며 원문·메모를 저장소나 로그에 쓰지 않는다. 입력 변경/새 기준 조회 시 영향 확인을 다시 받는다.
- 첫 표본은 63건 중 익명 요청에 HTML Accept 없이 302를 예상한 테스트 1건이 실패했다. 기존 EntryPoint를 확인하고 JSON 401/HTML 로그인 안내 이동을 각각 검증하도록 테스트만 수정했다. 이후 **64건 통과**. 인증 구현/권한/기존 assertion을 약화하지 않았다.
- 12:33 전체 `--rerun-tasks` 실행 **BUILD SUCCESSFUL in 2m 26s**, 13 task 재실행. 이후 마지막 CSS/표시 수정까지 포함하여 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`을 재실행했다. **BUILD SUCCESSFUL in 2m 7s**, 5 task 실행/8 up-to-date.
- 최종 root XML 시각 **12:40:59 KST**, **157 suite/1123건: 1019통과·104생략·실패0·오류0**. 추출기는 코드 변경 없이 12:33:47에 **13통과·생략0·실패0·오류0**, 최종 재실행에서는 up-to-date다. 생략은 최신 job PG 81, 첨부 migration 2, Spring Flyway 3, Linux 격리/실파일/규칙 snapshot 각 1, 기타 조건부 통합 15다. 새 확인 재조회 PG assertion은 컴파일됐지만 실제 DB에서 실행하지 못했다.
- Node `--check` 및 `node --test scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-contract-report.test.mjs`: **20통과·실패0·생략0**(새 화면 계약 11, 기존 보고서 판정 9). QA fixture 서버 구문 검사도 통과했다.
- 실제 인앱 브라우저는 **합성 API/SSR 로컬 환경**에서 근거 조회→확인→초안 응답, 새로고침 분류 복원, 409 입력 보존/재확인, 503 동일 요청 복구, OCR 수동 확인, preview/readonly/404 차단, XSS 비실행, 이모지 좌표 강조를 검증했다. 320/360/375/768/1024/1440px 가로 넘침 없음, 키보드 건너뛰기/본문 펼치기·포커스 표시를 확인했다. 초기 hidden 버튼 노출/320px 공통 최소 너비 문제를 발견해 수정하고 재확인했다. 자세한 실제 수행/미수행 항목은 `announcement-attachment-review-ui-2026-09-11.md`에 기록했다. **운영 DB/API E2E·실제 역할 세션 검증은 아니다.**
- 최종 로컬 JAR SHA-256: `4febfd17b96dc388316367227dcc53687b521b035934e03b3f4a336f5c27bcf5`. 12:39 근처 재조회에서 로컬/원격 master=`ae893b87348a9bd1cb0893763f6cad5047093a24`, GitHub pull=true/push=false. 수정/미추적 197경로, staged 0, 기존 migration 변경 0, diff check exit0, 제한된 자격증명 패턴 0건. 전체 보안 감사 통과를 뜻하지 않는다.
- 운영 SHA/health/DB/AWS는 이 구간 재조회하지 않았다. 커밋·push·원격 Actions·배포·정책 게시/ENFORCE·기존 데이터 적용은 실행하지 않았다. 기존 외부 차단을 해소한 새 증거가 없다. Linux 실행 환경/저장소 쓰기 권한과 운영 URL·세션이 필요하다.
- 직접 실행한 QA Node 두 프로세스를 종료했고 최종 대상 프로세스 0개를 확인했다. 브라우저 임시 탭을 닫고 viewport를 복원했다. 합성 SSR 임시 HTML을 정리했으며 재현 script/테스트와 이 문서를 보존했다. 기존 사용자 Java PID 20900(09-10 19:26 시작)과 사용자 Node는 그대로 두었다.

### 2026-09-11 13:07 단건 수집·실패 복구·역할 변경 UI

- Gate 4 진행, 전체 **Not ready**. 장기 목표/UI 운영/프론트엔드/브라우저/출시 Gate 스킬을 사용했다. 기존 계약을 연결하는 UI 증분이며 새 DB/API/의존성·운영 설정 변경은 없다. 미구현 정책 전체 validation/publication·정책 UI·배치·전체 profile·실파일/운영 Gate를 후속 개선으로 축소하지 않는다.
- 전용 검수 화면에 단건 전체 수집, 현재 실패 파일만 재시도, 전체 문서 역할 변경, 작업 상태 조회를 추가했다. 과거 집합 탐색과 변경 대상은 별개이며 고정된 현재 source/base/attachment/set/version·정책/실행 지문을 제출한다. 최초 수집 null 판정과 미리보기/ENFORCE 기존 binding을 구분한다.
- R2 영향 확인: 공고 1건, 전체 HTTP 최대132회/실패 파일 `12×(1+n)`회/역할 변경0회, 정책 이내 다운로드 상한, 합산 60초 간격·24시간3회, 이전 확인 STALE·근거 보존·재검수 필요·자동 활성화 없음. 실제 정책/요청률/고정 근거 무결성은 서버가 최종 검증한다.
- 화면 예약 응답은 처리 성공으로 표시하지 않는다. 사유 입력 보존, 결과 유실 시 같은 키/payload만 재시도, 실행 중/미확정/충돌 후 공통 쓰기 잠금, 최신 기준 재조회 후 선택/영향 재확인을 연결했다. 브라우저에서 발견한 409 후 검수 버튼 잔존과 공통 CSS 포커스 우선순위 결함을 수정·재검증했다.
- 표본 `:test --tests '*AnnouncementAttachmentViewControllerSmokeTest' --tests '*AnnouncementAttachmentCollection*Test' --tests '*AnnouncementAttachmentRetry*Test' --tests '*AnnouncementAttachmentRole*Test'`: **8 suite/104통과·생략0·실패0**(42초). 명령 범위의 `SANEB_ATTACHMENT_UI_FIXTURE=true`로 실제 Thymeleaf SSR 합성 QA 산출물을 만들었다.
- 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **BUILD SUCCESSFUL in 2m 27s**, 4 task 실행/9 up-to-date. root XML **13:07:16 KST**, **157 suite/1123건: 1019통과·104생략·실패0·오류0**. extractor 13건은 12:33:47 기존 결과를 재사용했고 이번 재실행은 아니었다.
- Node **29통과·생략0·실패0**(보고서9/검수11/복구9), 2개 화면 JS 구문 검사와 diff --check exit0. 최신 PG81건, migration/Flyway/Linux격리/실파일 등 기존 조건부104건은 실제로 실행하지 못했다.
- 실제 인앱 브라우저의 **loopback 합성 API**에서 전체 수집/실패1개 선택/0HTTP 역할 변경/작업 상태, readonly,409입력보존/공유잠금,503동일작업복구(합성 jobs=1),6개 폭 가로 넘침 없음,Tab/Enter/포커스 표시/콘솔오류0을 확인했다. 최종 CSS/JS 스크린샷은 작업 대화에 남겼다. 운영 DB/실제 계정/Provider/worker/HAR/전체접근성 검증은 아니다. 세부 증거는 화면 계약 문서에 기록했다.
- 최종 JAR SHA-256: `1e47e7281f1a2bb9169f921d6c8aad08214932c16766319890df48f76d59469c`. JAR 내 화면·CSS·2개 JS와 현재 소스 내용 일치를 확인했다. 로컬/원격 master HEAD=`ae893b87348a9bd1cb0893763f6cad5047093a24`; 원격/권한 조회 12:57 push=false/pull=true. 수정·미추적199경로/staged0/기존 migration 변경0/제한된 credential 패턴0. 전체 보안 감사 통과를 의미하지 않는다.
- 운영 SHA/health/AWS/DB는 이번 구간 미조회. 커밋·푸시·Actions실행·배포·정책게시·ENFORCE·기존데이터 적용은 미실행. 외부 권한 차단은 유지되며 정식 운영 URL/역할별 세션과 승인된 Linux QA 실행 경로가 필요하다.
- 직접 실행한 Node PID29936, 임시 브라우저 탭/viewport를 정리했다. 생성한 합성 HTML만 삭제했으며 테스트로 재생성할 수 있다. 빌드 종료 후 Java는 기존 사용자 PID20900만 확인했다. 다음 구현은 전체 정책 validation/publication과 관리자 정책 UI, 배치 계약이며 운영 적용 전 정확한 범위 승인을 별도로 받아야 한다.

### 2026-09-11 13:30 설치 런타임 고정 표본 QA 실행기

- Gate 4/6 진행, 전체 **Not ready**. 직전 회차는 단건 복구 UI 구현/브라우저 결함 수정으로 진행한 회차였고, 이번에는 `long-goal-operating-protocol`에 따라 분류 정답 세트와 실제 파일 실행 검증의 경계를 분리했다. `release-readiness-gate`상 필수 Linux/PG/운영 항목은 미검증이며 게시 가능으로 판정하지 않는다.
- `AttachmentRuntimeGate`를 추가했다. 외부 경로·URL·사용자 성공 JSON 없이 artifact 내부 12개 합성 binary를 기존 Linux 격리 추출기에 전달한다. 품질/형식/정확한 text/code point/locator/scope/page/error를 대조하고 정상·실패 모두 소유 원본 폴더 정리를 확인한다. 변조된 입력, 실행 중 runtime/suite 변경, 취소, 같은 실행기 동시 실행, 임시 정리 실패는 성공 결과를 반환하지 않는다.
- AR-001~012: PDF text/빈 PDF, HWP 한글·이모지/암호/손상, HWPX 한글·서로 다른 표 셀/부분 추출/DTD·entity/ZIP 경로 이탈/압축 상한, HTML 응답/손상 PDF다. 합성 text PDF는 영문이며 실제 한글 PDF·스캔 이미지·공개 실사이트 표본을 대체하지 않는다. 원문은 결과/로그에 넣지 않고 입력/text/runtime/suite/result hash와 case/시각만 남긴다.
- extractor 별도 `qaFixtures` source set에서 입력을 생성하며 root `processResources`로 복사한다. 최종 JAR에서 입력 12개·RuntimeGate 포함을 확인했고, PDFBox/POI/extractor parser JAR 및 생성기 class가 Spring 서버 JAR에 없는 것을 확인했다. 재생성 전후 12개 파일의 SHA-256 변경은 0건이다. 운영 parser host fallback은 추가하지 않았다.
- 내부 실행기는 아직 정책/규칙/profile의 frozen 입력 및 DB 검증 이력에 연결되지 않았다. 관리자 endpoint나 상시 worker에서 자동 호출하지 않는다. 다중 서버 QA lease/예약/실패 이력/중지·복구/전체 validation·publication/정책 UI는 필수 미완료다. 이 실행기 결과 하나만으로 게시하는 경로를 만들지 않았다.
- 전용 `attachmentRuntimeIntegrationTest`와 기존 수동 Linux workflow 연결을 추가했다. 임시 Ubuntu runner의 bubblewrap/prlimit을 요구하며 OS 보안 완화는 없다. 보고서 판정기는 DB 2종과 runtime 1종의 누락/다른 suite/0건/실패/오류/skip/오래된 실행을 거부한다. 이 workflow는 원격에서 실행하지 않았다.
- 선택 root 4 suite/29건 중 **27통과·2생략·실패0** 후 전체 회귀를 실행했다. 마지막 코드 변경을 포함한 최종 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`.
- **BUILD SUCCESSFUL in 2m 48s**, 16 task 모두 실행. root XML **13:30:27 KST**, **159 suite/1147건: 1042통과·105조건부 생략·실패0·오류0**. extractor XML **13:30:30 KST**, **2 suite/25통과·생략0·실패0·오류0**. 기존 deprecated/unchecked 및 합성 파일 생성기의 Log4j provider 경고가 남아 있다.
- 생략 105건은 최신 job PG81, attachment migration2, Spring Flyway3, 기존 Linux 격리/공개 실파일/규칙 snapshot 각1, 새 설치 runtime integration1, 기타 조건부15다. Windows에서 mock RuntimeGate 검사와 실제 fixture parser/CLI 단위 테스트가 통과한 것이며 Linux 격리 프로세스 12개가 통과한 것이 아니다. 차단된 initdb/Docker/AWS 호출을 반복하지 않았다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs`: **30통과·실패0·생략0**. 새 보고서 검증 10건은 DB 성공만 있고 runtime 보고서가 없거나 생략돼도 차단하는 조건을 포함한다. Node syntax 검사/diff --check exit0.
- 최종 로컬 JAR SHA-256: `edf43c68334320cf23f4e8bed96a0ad38877a559a10d8731d7918abce4ec7d48`. 로컬 master/원격 master 조회는 `ae893b87348a9bd1cb0893763f6cad5047093a24`로 일치했다. 변경·미추적205경로/staged0/기존 migration 변경0/ATT62행 유지/한정된 credential 패턴205파일 중0건이다. 전체 보안 감사 통과를 뜻하지 않는다.
- 운영 SHA/health/AWS/DB/역할별 브라우저는 이번 구간 미조회·미실행이다. 커밋·push·Actions dispatch·배포·정책/ENFORCE/기존 데이터 적용도 미실행이다. 기존 GitHub 쓰기 권한 차단을 해소한 새 증거는 없다. 브라우저는 승인돼 있지만 이번 내부 QA 증분에서는 실행하지 않았으며 이전 합성 UI 결과를 운영 또는 이번 최종 artifact E2E 통과로 재사용하지 않는다.
- 직접 실행한 Node QA/fixture Java/Gradle single-use daemon/worker는 종료됐다. 13:30:58 Java 프로세스는 기존 사용자 PID20900만 확인했다. JUnit 소유 임시 입력은 테스트 종료로 정리되고 재현 가능한 build fixture/JUnit XML/JAR은 증거로 남긴다. 다음은 이 실행기를 포함한 서버 소유 전체 QA 예약·정책/규칙/profile 버전 결합·불변 이력·게시 영향 계약과 배치/전체 provider·운영 검증이며 전체 목표를 축소하지 않는다.

### 2026-09-11 14:08 정책 QA 비동기 실행·버전 결합·불변 이력

- Gate 4/6 진행, 전체 **Not ready**. AGENTS.md와 long-goal-operating-protocol을 다시 확인하고 이전 내부 실행기를 정책 QA 예약·조회·취소 API 및 scheduler/DB 이력에 연결했다. release-readiness-gate상 전체 Linux/PG/Provider/운영 검증은 미완료다.
- `validation-runs`는 ADMIN 예약/취소, ADMIN·OPERATOR·APPROVER 조회만 허용한다. CSRF·정책/run CAS·동일 actor/정책/입력 멱등 키·금지 필드·no-store를 검증한다. DRAFT 정책/규칙/등록 profile/활성 수집원/설치 runtime·suite/실행 코드·mapper·V73 지문을 고정한다. 최근 실패 수집원도 범위에서 누락하지 않고 URL/설정 원문은 hash로 분리한다.
- V73에 별도 QA run/step 테이블과 전역 단일 활성 예약/불변 입력·종료/단계 증거 trigger를 추가했다. 수집 worker와 EXTRACTION/GLOBAL/1 슬롯을 공유하고 획득 실패 시 claim을 rollback한다. 최초 lease 최대 8분·매 파일 잔여 40초·만료 실패 회수·취소 우선·다른 token 완료/해제 금지 계약이다. 삭제·취소 중 소유권 교체·슬롯 없는 근거 저장도 차단한다.
- 기본 `SANEB_ANNOUNCEMENT_ATTACHMENT_POLICY_QA_ENABLED=false`, 같은 정책 60초 간격/24시간 3회·전역 대기/실행 1개다. 현재 실행은 고정 합성 표본만 사용하여 외부 공고 HTTP 0회다. 분류 AG30 + runtime AR12 성공도 실제 전체 profile 및 worker DB 단계가 MISSING이므로 **INCOMPLETE**다. 현재 worker에는 VERIFIED/게시/ENFORCE/기존 source 적용 경로가 없다.
- 전체 정책 QA가 미완료라는 사실을 상태에 기록한 것이며 목표를 줄인 것이 아니다. 실제 모든 profile 표본·격리 worker DB 복구 실행, 전체 증거 결합/게시 영향과 승인/정책 UI·배치/운영 E2E는 필수 후속 작업이다. policy/runtime 단위 mock 성공이나 DB trigger fixture의 네 PASSED 행을 실제 전체 QA 성공으로 해석하지 않는다.
- 중간 검증에서 runtime overload 추가 후 테스트 method reference 모호성 1건, Mockito 재설정 중 null 인자로 이전 answer가 호출된 1건을 수정했다. 명시적 lambda와 doReturn stubbing으로 정정했고 기대 assertion을 제거/약화하지 않았다. 후속 선택 테스트는 통과했다.
- 최종 실행 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`. **BUILD SUCCESSFUL in 2m 52s**, 16 task 모두 실행.
- root XML **14:08:09 KST**, **162 suite/1207건: 1090통과·117조건부 생략·실패0·오류0**. extractor XML **14:08:11 KST**, **2 suite/25통과·생략0·실패0·오류0**. 117 생략은 job PostgreSQL93 + migration2 + Spring Flyway3 + runtime/격리/공개파일/규칙 snapshot 각1 + 기타15다. 새 PG 12건은 컴파일됐지만 실제 실행하지 못했다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs`: **30통과·실패0·생략0**. 화면 JS 2개 syntax와 `git diff --check` exit0. 기존 MockBean/unchecked 경고와 합성 fixture 생성기의 Log4j provider 경고는 남아 있다.
- 로컬 JAR SHA-256: `e6f74c826f0d7699a990c40612cedf305f367de29ed24f170079d8d13cd62771`. 내부 고정 fixture12개/QA 관련 class17개를 확인하고 V73와 QA mapper가 현재 소스와 일치함을 확인했다. Spring JAR의 PDFBox/POI/extractor parser JAR·fixture 생성기 class는 0개다.
- 로컬/실조회 원격 master는 `ae893b87348a9bd1cb0893763f6cad5047093a24`로 일치한다. 변경·미추적217경로/staged0/기존 migration 변경0/ATT62행 유지/제한된 credential 패턴217파일 중0건. 이는 전체 보안 감사 통과가 아니다. 코드·V73·문서는 아직 커밋/운영 반영하지 않았다.
- 이번 구간 AWS/운영 DB·SHA·health/Actions 실행·운영 브라우저는 재조회·실행하지 않았다. 기존 GitHub 쓰기 권한·Linux PG/격리 환경·정식 운영 URL/역할별 세션 차단에 새 해소 증거는 없다. 과거 숫자/합성 UI를 현재 운영 정상 증거로 사용하지 않는다. 반복 실패한 initdb/Docker/STS를 재시도하거나 OS/TLS 보안을 완화하지 않았다.
- 직접 시작한 Gradle single-use daemon/Java fixture·테스트와 Node QA는 종료됐다. 14:08 확인 Java는 기존 사용자 PID20900만 남았다. 기존 Node/사용자 자원은 보존했다. 테스트 소유 원본은 정리됐고 재현 가능한 fixture/JUnit XML/JAR은 build 증거로 남긴다.

### 2026-09-11 14:35 기존 데이터 배치 범위 고정·취소

- 직전 회차는 QA API/DB 계약과 직접 검증 결과를 추가한 진행 회차다. 이번에는 Gate 5의 실제 미구현인 배치 범위를 구현했다. long-goal-operating-protocol에 따라 기존 설계 12장·V72 batch/job·worker claim/검수 경계를 확인했으며 전체 목표는 그대로다. release-readiness-gate 판정은 **Not ready**다.
- `AnnouncementAttachmentBatchController/Service/ServiceImpl/DAO/Mapper`, 요청/응답/행 DTO를 추가했다. 순수 DB scope-preview, BACKFILL/SCOPE_READY jobs 고정, 목록/상세/항목 pagination, 수집 전 취소를 제공한다. READ 3역할/ADMIN 변경·CSRF·UUID 멱등성·no-store·엄격한 입력과 한국어 오류 계약이다.
- 조회는 정책·provider·수집 시각/선택 마감일·명시적 최대 건수를 받는다. 전체 제외/후보 집계와 선택/잔여 건수, 미지원 profile/규칙 불일치/활성 job을 구분한다. 준비 불가 대상을 조용히 빼거나 다음 원문으로 채우지 않는다. 현재 HTTP는 0이며 파일 수/판정 변경 수를 만들지 않는다. 상한은 source별 정책 bytes·132 HTTP를 합산한다.
- scopeHash는 선택 source/content/base/현재 확인·binding/버전, 정책/profile/설치 설정과 저장된 출처 연결 지문, 전체 집계/필터를 묶는다. POST는 source UUID 순서 잠금과 정책 공유 잠금 후 다시 대조하고 같은 키/입력은 기존 batch를 반환한다. 불일치 시 일부 저장 없이 실패한다. URL/사유/원문과 source ID 목록을 batch/audit metadata에 복사하지 않는다.
- V73에 관리 배치 scope_item_count/policy_snapshot_json, batch-policy composite FK, frozen provider, 불변/수집 전 취소/deferred 전체 건수 계약을 추가했다. jobs는 SCOPE_READY라 worker가 claim하지 않고 current/source 버전·검수 binding을 변경하지 않는다. 기존 서버 검수는 batch job을 일반 진행 job으로 차단하지 않는다. 다른 첨부 수집은 활성 예약 제약과 충돌할 수 있으며 scope-cancellation으로 예약을 해제한다.
- 원문/content/base cascade로 jobs가 삭제되면 deleted_item_count만 증가시켜 고정 건수=남은 jobs+삭제 건수를 유지한다. 새 원문을 기존 batch에 넣거나 삭제 원문 ID/URL을 tombstone에 복원하지 않는다. 관리 batch 이력 DELETE와 CANCELLED 되돌림은 거부한다.
- 새 단위 **14건**, Spring HTTP **17건**, Mapper 2건/MigrationContract 1건이 최종 실행에서 통과했다. 테스트 소유 PostgreSQL 5건(고정/claim 금지·검수 유지, 취소, 삭제 건수, 동시 같은 키, 버전/미완료 상태 차단)을 추가했으나 실제 실행은 차단되어 있다. 임시 DB fixture의 TRUNCATE는 이 로컬 회차에서 실행하지 않았다.
- 중간 선택 검증 123건 중 정적 검사 1건이 SQL 주석의 영문 title을 조회 컬럼으로 오인했다. 실제 조회에 제목 컬럼이 없음을 확인하고 주석을 한글화했으며 assertion은 유지했다. 이후 최종 전체 회귀는 실패 0이다.
- 최종 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`. **BUILD SUCCESSFUL in 3m 10s**, 16 task 전부 실행.
- root XML **14:35:42 KST**, **164 suite/1246건: 1124통과·122조건부 생략·실패0·오류0**. extractor XML **14:35:45 KST**, **25통과·실패0·생략0**. 생략에는 최신 job PG98, migration2, Flyway3, Linux runtime/격리/공개 파일/규칙 snapshot4, 기타15가 포함된다. 기존 Log4j provider/MockBean deprecated/unchecked 경고는 남아 있다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs`: **30통과·실패0·생략0**. 기존 화면 JS 2개 구문 검사와 `git diff --check` exit0. 이번에는 화면 변경/브라우저 실행이 없었으며 승인된 운영 E2E는 여전히 필수 미완료다.
- 최종 로컬 JAR SHA-256: `f1305dc800e62b41f20283ddc6c079fc1bed34617da2247a59099cea0b1e24ce`. batch 관련 class22개와 현재 V73/BatchMapper 일치를 확인했다. Spring JAR의 PDFBox/POI/extractor parser JAR·fixture 생성기 class는 0개다.
- 로컬/실조회 원격 master 모두 `ae893b87348a9bd1cb0893763f6cad5047093a24`. 변경·미추적228경로/staged0/기존 migration 변경0/ATT62행 유지. 제한된 credential 패턴은 228파일 중0건이며 전체 보안 감사 판정은 아니다.
- AWS/운영 DB·health·SHA/Actions·운영 브라우저는 이번 구간 재조회·실행하지 않았다. 기존 권한/Linux 차단을 반복 호출하거나 보안 완화로 우회하지 않았다. 커밋·푸시·배포·정책 게시·ENFORCE·기존 데이터 운영 적용은 미실행이다.
- 직접 실행한 Gradle/Java fixture·테스트/Node 명령은 종료 코드 0으로 종료됐다. 14:36 Java는 기존 사용자 PID20900만 확인했다. 이후 관찰된 Node 중 본 작업 QA 파일을 실행 중인 프로세스는 없었고 도구용/소유권 미확인 프로세스를 임의 종료하지 않았다. build/JUnit/JAR 증거는 보존한다.
- 다음 필수 작업은 승인 영향에 묶인 batch collection 시작·고정 입력 재검증·봉인 결과 preview·apply/pause/resume/rollback·전체 분할 집계와 UI다. 정책 전체 QA/게시·전체 Provider·Linux/DB/운영 E2E도 그대로 남아 있다. 이번의 범위 고정 API를 기존 데이터 전체 처리 완료로 표현하지 않는다.

### 2026-09-11 15:09 배치 수집 시작·중지·재개와 결과 집계

- 이번은 Gate 5의 수집 실행 연결을 구현한 **진행 회차**다. root AGENTS와 long-goal-operating-protocol을 읽고 기존 배치 설계 12장/worker/DB 계약을 대조했다. release-readiness-gate 판정은 **Not ready**이며 전체 목표를 축소하거나 완료 처리하지 않았다.
- ADMIN·CSRF·고정 버전/범위/대상·삭제 수/최초 bytes·HTTP 상한 확인으로 수집 시작/중지/재개 PUT API를 추가했다. 시작은 source UUID → 정책/규칙 SHARE → batch 잠금과 실행 지문 재검증 후 같은 transaction에서 SCOPE_READY batch/jobs만 실행 대기로 전환한다. 최초 필터를 다시 실행하거나 일부 source로 조용히 줄이지 않는다. 응답 손실 시 GET으로 상태를 확인하며 이전 버전 재요청은 409이고 추가 작업을 생성하지 않는다.
- V73에 최초 실행 승인 시각/hash·승인자 필수/불변 계약, frozen locator hash, 이전 검수 binding 불변 검증을 추가했다. URL을 job에 복사하지 않는다. 현재 출처/검수/연결 공고가 바뀌면 매 HTTP guard가 차단하고 다음 claim에서 CONFLICT/FROZEN_INPUT_CHANGED로 종료하여 무한 대기를 막는다. 일반 수집의 기존 version 충돌 사유는 유지했다.
- 중지는 새 claim/HTTP를 막되 전송 중 요청의 즉시 종료를 보장하지 않는다. 완료·checkpoint·시도·누적 예산은 보존하고 재개는 미완료 고정 입력만 재검증한다. terminal job을 재시도하거나 예산을 환급하지 않는다. 삭제 대상은 재개에서 명시적 현재 건수 확인이 필요하며 모든 항목 삭제를 COLLECTED로 표시하지 않는다.
- worker enabled일 때 DB 전용 scheduler가 15초 간격·변경 후보 최대 100개/SKIP LOCKED로 실제 전체 jobs·삭제를 집계한다. 처음 100개의 단순 대기가 뒤쪽 완료 집계를 막지 않는다. 전체 SUCCEEDED·SEALED set·preview evaluation/hash·삭제 0일 때만 COLLECTED이며 나머지는 COLLECTION_PARTIAL_FAILED다. 중지를 자동 해제하거나 preview 승인/apply로 넘어가지 않는다. 기존 source current/검수/정책 binding/운영 공고는 이 경로에서 수정하지 않는다.
- BatchService **26건**, BatchController **24건**, BatchClaim/Scheduler **5건** 및 Mapper/MigrationContract 확장 검증이 최종 실행에서 통과했다. PG 6건(수집/봉인 평가 분리, 중지·예산·승인 보존, 변경 지문/우회 차단, claim 충돌, 중지 중 삭제 집계, 동시 시작 1회)을 추가했으나 조건부 생략되어 실제 PostgreSQL 증거가 아니다. 최신 job PG 사례는 총104건이다.
- 1차 표적 검증145건 중 권한 테스트5건이 403 대신400으로 실패했다. 원인은 중지 endpoint에 수집 DTO 필드를 보낸 테스트 fixture였다. 올바른 중지 요청으로 수정하고 403 및 service 미호출 assertion은 유지했다. 후속 표적/전체 실행은 실패0이다. 정적 SQL 검토에서 집계의 set 테이블명을 실제 Flyway와 일치시켰다.
- 최종 실행: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`. **BUILD SUCCESSFUL in 3m 7s**,16 task 전부 실행. 새 입력 오류의 한글 메시지 보완까지 포함한다.
- root XML **15:09:21 KST**, **165 suite/1279건:1151통과·128조건부 생략·실패0·오류0**. extractor XML **15:09:24 KST**, **25통과·실패0·생략0**. 생략에는 job PG104/migration2/Flyway3/Linux runtime·격리·공개 파일·규칙 snapshot4/기타15가 포함된다. Log4j provider·MockBean deprecated·unchecked 경고는 남아 있다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs`: **30통과/실패0/생략0**. 화면 JS2개 구문 검사, 문서 code fence/ATT62행 검사, `git diff --check` 통과. 이번 증분은 backend/계약 변경이며 브라우저는 실행하지 않았다. 승인된 실제 운영 E2E는 필수 미완료다.
- 최종 JAR SHA-256 **f31d5acd1c62acfe1051a18de33c0b41b4bf579ee6106077113da64e5799dc91**. 배치 class25개, V73/BatchMapper/JobMapper source 일치와 Spring JAR의 PDFBox/POI/extractor parser·fixture 생성기 혼입0개를 검사한다. JAR 생성은 운영 배포 증거가 아니다.
- Git master/HEAD 및 이번 회차 원격 실조회는 **ae893b87348a9bd1cb0893763f6cad5047093a24**. `status --porcelain --untracked-files=all` 기준 변경·미추적230경로, staged0, 기존 V1~V72 변경0. 기본 status는 미추적 디렉터리를 묶어74줄이므로 이를 변경 감소로 해석하지 않는다. 제한된 credential 패턴은 해당 확장자226파일 중0건이며 전체 보안 감사는 아니다.
- 커밋·푸시·배포·운영 DB·정책 게시·ENFORCE·운영 배치 실행은 미실행이다. GitHub 쓰기/Actions·AWS 접근과 Linux/PG 차단은 이번에 반복 조회/재시도하지 않았다. 과거 인증/운영 기록으로 현재 운영을 정상 처리하지 않는다.
- 직접 사용한 Gradle single-use daemon/테스트/fixture·Node는 종료됐다. 최종 Java는 기존 사용자 PID20900만 남아 보존했다. 소유권 불명인 Node/사용자 자원은 종료하지 않았으며 사용한 QA script의 실행 중 Node는 없다. build/JUnit/JAR 증거는 보존한다.
- 다음은 봉인된 고정 결과의 batch preview·성공 항목 명시적 선택·preview hash/CAS apply·적용 pause/resume·조건부 rollback·전체 분할 ledger/UI다. 정책 전체 QA/게시·전체 Provider·최신 PG/Linux·운영 배포/역할별 브라우저 E2E도 그대로 남아 있다.

### 2026-09-11 15:51 봉인 결과 미리보기·명시적 선택·불변 이력

- 이번은 Gate 5의 배치 분류 미리보기와 적용 대상 선택을 구현·검증한 **진행 회차**다. AGENTS와 long-goal-operating-protocol을 준수하며 기존 설계 12장, V72/V73, 수집·worker·평가 계약을 대조했다. release-readiness-gate 판정은 **Not ready**다. 실제 적용·복구와 운영 E2E가 남아 전체 목표를 완료 처리하지 않는다.
- `AnnouncementAttachmentBatchPreviewController/Service/ServiceImpl/DAO/Mapper`와 요청·응답·행 DTO, service/HTTP 테스트를 추가했다. ADMIN·CSRF·UUID 멱등성 키로 분류 미리보기 POST 및 명시적 선택 PUT을 실행하고, READ 3역할로 현재/지정 이력·페이지 항목을 조회한다. 기존 `/api/v1`은 보존하고 `/api/v2/admin/announcement-attachment-batches/{batchId}/classification-preview` 하위에 wrapper/no-store/엄격한 입력/한국어 오류 계약을 추가했다.
- 고정된 배치 전체를 유지하며 SUCCEEDED·완료된 SEALED 근거·현재 frozen 입력·미연결·다른 활성 job 없음 조건을 모두 만족하는 항목만 선택 가능하다. 실패/충돌/삭제를 분모에서 숨기거나 다른 공고로 채우지 않는다. 제안 판정 REVIEW_REQUIRED는 검수 완료나 최종 진행 확정을 뜻하지 않는다. 최초 선택은 0건, 빈 선택은 명시적 해제다.
- source UUID → 정책/규칙 공유 → batch 잠금 후 현재 정책 snapshot, scopeHash, batchVersion, 고정 입력·이전 검수 binding을 검증한다. 선택 변경은 새 preview ID/hash/version과 전체 항목 이력을 생성한다. 같은 멱등성 키 재요청은 당시 이력만 반환하며 이후 현재 선택을 되돌리지 않는다. 입력 변경 시 선택은 409이며 새로운 미리보기를 명시적으로 생성해야 한다.
- 분류 근거는 정확한 SEALED set/evaluation/evaluation-input의 extraction ID와 연결한다. 정적 검토 후 OPEN set이나 다른 evaluation의 metadata를 재사용하지 않도록 SEALED join 및 exact set 조건을 보강했다. 파일 metadata와 선언된 discovered/processed 수를 구분하고 최대 10개 초과는 준비 실패로 처리한다. 제목·본문·추출 원문·URL·파일명은 미리보기/audit metadata에 복사하지 않는다. 출처 연결은 hash로만 입력 지문에 포함한다.
- AUTO 태그와 이전 관리자 CONFIRMED 태그의 차이를 별도 표시한다. 선택이나 조회로 확정 태그·현재 source/evaluation/policy/confirmation binding·운영 공고를 수정하지 않는다. 모든 미리보기 API의 현재 외부 HTTP는 0이며 worker 다운로드/추출을 실행하지 않는다.
- V73에 불변 preview/항목 이력, 관리 배치 현재 preview pointer, composite FK·deferred 전체/선택 수 검증, 선택 변경 경계 trigger를 추가했다. 기존 V72 `preview_hash` 값이 있어도 새 이력이 존재한다고 가정하지 않도록 nullable `current_preview_id`를 사용한다. 원문 삭제 cascade는 항목만 제거하며 당시 전체/삭제 수와 현재 남은/삭제 수를 분리한다. 삭제 ID를 이력에 재구성하지 않고 inputsCurrent=false로 표시한다. V73은 여전히 로컬 미추적·미적용이며 V1~V72 변경은 0개다.
- 새 Service **16건**, Spring HTTP **18건**, Mapper 2건, MigrationContract 1건이 최종 실행에서 통과했다. 실제 PostgreSQL 6건(불변 이력/선택, 변경 입력 충돌, cascade/삭제 집계, 우회 변경 차단, 동시 멱등 요청, 실패 항목 보존)을 추가했으나 조건부 생략이다. 최신 job PG **110건 모두 미실행**이며 SQL/trigger 동작 성공을 주장하지 않는다.
- 최종 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`. **BUILD SUCCESSFUL in 3m**, 16 task 전부 실행. root XML **15:46:28 KST**, **167 suite/1322건 중 1188통과·134조건부 생략·실패0·오류0**. extractor XML **15:46:32 KST**, **25통과·실패0·생략0**. 기존 Log4j provider/MockBean deprecated/unchecked 경고는 남아 있다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs` **30통과·실패0·생략0**. 화면 JS 2개 `--check`, `git diff --check` exit0. 브라우저 실행은 이번 회차에 없었으며 전체 목표에서 승인된 운영 역할별 E2E는 여전히 필수 미완료다.
- 최종 로컬 JAR SHA-256 **30b2e2f7161ee4b6f0f7cdf5b1ce60b4a5f5f8119c22c898a4bb9e5ac2dea541**. 배치 관련 class43개, 현재 V73/BatchPreviewMapper source와 JAR entry의 SHA-256 일치를 확인했다. Spring JAR의 PDFBox/POI/extractor parser JAR 및 fixture 생성기 class 혼입은 0개다. 이 JAR을 운영 배포하지 않았다.
- Git master/HEAD 및 이번 회차 원격 실조회 기준은 **ae893b87348a9bd1cb0893763f6cad5047093a24**. 변경·미추적240경로/staged0/기존 migration 변경0/ATT62행 유지. 제한된 credential 패턴은 해당 확장자240파일 중0건이며 전체 보안 감사 결과는 아니다. 기존 사용자 변경사항을 정리·삭제·staging하지 않았다.
- 최종 기본 `git ls-remote`는 로컬 issuer 인증서 오류로 실패했다. TLS 검증을 유지한 명령 범위의 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`로 Windows 인증서 저장소를 사용한 조회는 exit0이며 위 SHA와 일치했다. 전역 Git 설정이나 TLS 검증을 변경하지 않았다. 원격 읽기 성공은 쓰기/Actions 권한 확인이 아니다.
- 이번 회차 커밋·푸시·배포·운영 DB·정책 게시·ENFORCE·기존 데이터 운영 적용은 미실행이다. GitHub 쓰기/Actions·AWS 및 Linux/PG blocker를 반복 호출하거나 TLS/OS 보호 완화로 우회하지 않았다. 운영 DB·health·SHA·Actions·운영 브라우저를 이번에 재조회하지 않았으며 과거 기록을 현재 정상 증거로 사용하지 않는다.
- 직접 실행한 Gradle/테스트/fixture·Node는 종료됐다. 15:49 조회에서 Java는 기존 사용자 PID20900만 남았고, 본 QA script를 실행 중인 Node는 0개였다. 이후 15:53 별도로 시작된 Java 프로세스는 이번 완료된 빌드 소유로 간주하지 않고 보존했다. 사용자·도구 소유 프로세스는 종료하지 않는다. build/JUnit/JAR 검증 산출물은 보존한다.
- 다음 필수 작업은 선택 이력/hash·현재 입력 CAS에 묶인 **배치 apply/pause/resume와 조건부 rollback**, 전체 분할 ledger와 관리자 UI다. 정책 전체 QA/게시·전체 Provider/profile·실제 Linux/PG·운영 배포/브라우저 E2E도 남아 있다. 미리보기·선택 완료를 실제 데이터 적용 완료로 표현하지 않는다.

### 2026-09-11 16:25 배치 적용 승인·중지·재개·항목 CAS

- 직전은 미리보기·선택 코드/검증을 추가한 진행 회차이며 이번에는 실제 적용 backend를 구현했다. AGENTS를 가장 먼저 완독하고 long-goal-operating-protocol에 따라 원래 설계12장·V72/V73·현재 평가/검수/배치 계약을 대조했다. release-readiness-gate 판정은 **Not ready**다. 조건부 복구·전체 Provider/정책 QA/UI·운영 E2E 목표를 축소하거나 완료 처리하지 않는다.
- `AnnouncementAttachmentBatchApplicationController/Service/ServiceImpl/DAO/Mapper`, DTO/행, background Scheduler, 공통 `AttachmentBatchFingerprint`를 추가했다. 기존 미리보기 직렬화 지문을 보존하며 apply에 재사용한다. 정확한 단건 LiveItem 조회로 매 항목마다 전체 batch를 다시 읽지 않는다.
- ADMIN·CSRF·UUID 멱등 키·현재 버전/preview ID/hash·전체/선택/삭제 수·재검수 확인·사유로 START/PAUSE/RESUME를 접수한다. 202는 명령 접수이지 적용 완료가 아니다. READ 3역할의 영수증/페이지 항목 조회에서 최초 승인 건수와 현재 남은/삭제/성공/충돌/실패를 분리한다. 수집 SUCCEEDED와 적용 CONFLICT/FAILED, 적용 실패/backoff도 별도 필드다. 제목·본문·주소·검수 원문을 응답/metadata에 추가하지 않는다.
- START는 source UUID→규칙/정책 SHARE→batch 잠금과 전체 미리보기 지문 검증 후 정확히 선택된 적격 항목만 PENDING으로 만든다. 현재 ACTIVE ENFORCE와 고정 정책·규칙이 일치해야 하며 COLLECT_ONLY/변경 정책을 자동 승격하지 않는다. 동일 키 재호출은 최초 영수증과 현재 상태만 반환하며 선택/완료/중지를 초기화하지 않는다.
- 기존 worker flag enabled일 때 기본1초마다 승인된 APPLYING 항목1건을 source→정책→batch 잠금으로 재조회한다. source/base/set/evaluation/extraction/검수/보호 link/활성 job/정책·itemInputHash가 바뀌면 terminal CONFLICT로 남기며 새 preview·다른 원문으로 교체하지 않는다. 최종 source CAS0도 재시도 오류가 아닌 APPLICATION_SOURCE_CAS_CONFLICT다. 중지는 다음 항목부터 적용되고 이미 transaction을 시작한 항목은 먼저 완료될 수 있다.
- 성공 항목은 같은 transaction에서 첨부 current 평가·정책·검수 요구와 attachment_row_version+1을 반영하고 이전 current evaluation/confirmation을 STALE로 만든다. 과거 CONFIRMED 태그/메모는 보존한다. base/classification_row_version·운영 공고/지원 진행은 변경하지 않는다. 새 확인 없이 이전 확인으로 DRAFT 전환할 수 없다. applied evaluation/source·첨부 version/inputHash를 조건부 복구 근거로 남긴다. HTTP/다운로드/추출/재분류/DRAFT 생성은 이 경로에 없다.
- 예기치 않은 transaction 실패는 전체 항목 변경 취소 후 별도 transaction에서 최대3회·30초 backoff를 기록한다. 완료/충돌/실패는 재개로 다시 대기시키지 않는다. 삭제/비선택/실패가 있으면 최초 전체 범위 APPLIED로 표시하지 않고 APPLY_PARTIAL_FAILED다. 원문 삭제로 남은 대기가 없어도 집계하며 중지를 자동 해제하지 않는다.
- V73에 불변 application_actions·명령별 상태/건수 검증·deferred 접수+전체 대기 완료 검증, 최초 application_approval_id, 선택 preview FK, 적용 후 version/hash·실패/backoff 계약을 추가했다. 승인 없는 적용·승인 preview 변경·terminal 결과 초기화·적용 근거 변조를 거부한다. V1~V72는 변경하지 않았으며 V73은 로컬 미추적·미적용이다.
- 새 Service **23건**, Spring HTTP **18건**, Scheduler **2건**, Mapper2/MigrationContract1이 최종 실행에서 통과했다. PG8건(대기/정확한 적용·중복 멱등성, 변경 입력 충돌, 중지/재개, 동시 처리1회, 삭제 분모 유지, 이전 확인 만료/태그 보존, 승인 후 DRAFT 보호, DB 우회 거부)을 추가했다. 최신 job PG **118건 전부 조건부 생략**이며 실제 SQL/trigger 성공 증거가 아니다.
- 1차 추가 테스트 컴파일에서 ApiException fixture의 누락된 message 인자1건을 수정했다. 표적 검증은 성공했다. 첫 전체 회귀1370건 중1건은 등록 전 application 경로의404 기대값이 남은 기존 테스트였다. CSRF403·보안 메시지 검증은 유지하고 현재 등록 handler의 필수 입력 누락400을 검증하도록 수정했다. 이후 최종 회귀 실패/오류는0이다.
- 최종 명령: `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`. **BUILD SUCCESSFUL in 3m 21s**,16 task 전부 실행. root XML **16:22:58 KST**, **170 suite/1376건: 1234통과·142조건부 생략·실패0·오류0**. extractor XML **16:23:02 KST**, **25통과·생략0·실패0**. 기존 Log4j provider/MockBean deprecated/unchecked 경고는 남아 있다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs` **30통과·실패0·생략0**. 화면 JS2개 `--check` 및 `git diff --check` exit0. 이번에는 브라우저를 실행하지 않았으며 전체 목표에 포함된 운영 역할별 E2E는 여전히 필수 미완료다.
- 최종 JAR SHA-256 **1647a7780ae5fdb004d9157a6057a2698b1957e16b098c89d162ec0a8393320f**. 배치 적용 관련 class12개와 최신 V73/BatchApplicationMapper/BatchPreviewMapper의 source/JAR SHA-256 일치를 확인했다. Spring JAR의 PDFBox/POI/extractor parser JAR·fixture 생성기 class 혼입은0개다. 이 산출물은 운영 배포하지 않았다.
- 로컬 master/HEAD 및 TLS 검증을 유지한 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master` 결과는 **ae893b87348a9bd1cb0893763f6cad5047093a24**로 일치한다. 변경·미추적253경로/staged0/기존 migration 변경0/ATT62행 유지. 제한 credential 패턴은253파일 중0건이며 전체 보안 감사 판정은 아니다. 기존 사용자 변경사항을 임의 staging·삭제·정리하지 않았다.
- 커밋·푸시·배포·운영 DB·정책 게시·ENFORCE·기존 데이터 운영 적용은 미실행이다. AWS/Actions/현재 운영 health·SHA·브라우저를 이번 회차에 조회하지 않았으며 이전 차단/운영 기록을 현재 정상으로 대체하지 않는다. 기존 Linux/PG·권한 blocker에 보안 완화나 반복 인증 시도를 하지 않았다.
- 직접 실행한 Gradle single-use daemon/fixture·테스트/Node는 종료됐다. 최종 Java는 기존 사용자 PID20900만 남았고 본 QA script의 실행 중 Node는0개다. 사용자의 다른 프로세스를 종료하지 않았으며 build/JUnit/JAR 증거를 보존한다.
- 다음 필수 작업은 **조건부 rollback과 이전 confirmation의 복원 유효성 계약**이다. 적용 후 current/hash/version 및 후속 검수/역할/본문/전환 부재를 확인하고 버전은 증가시켜야 한다. 단순 is_current 복원은 기존 confirmed version과 맞지 않으므로 완료 조건이 아니다. 전체 분할 ledger·배치 UI·정책 전체 QA/게시·전체 Provider/profile·실제 Linux/PG·운영 배포와 브라우저 E2E도 남아 있다.

## 2026-09-11 16:54 KST — Gate 5 이전 확인 복구 유효성 기반

- 이번 회차는 **진행(progress)**이다. 전체 목표와 ATT-001~062를 축소하지 않았으며 전체 Gate/출시 판정은 **Not ready**다. 직전 회차의 적용 승인/worker 위에 원복 후 기존 검수 확인의 유효성 계약을 추가했다. 전체 원복 승인 API/worker를 완료했다고 보고하지 않는다.
- 루트 AGENTS.md를 먼저 재확인하고 long-goal-operating-protocol, UI/UX 운영 원칙 및 frontend-ui-engineering을 적용했다. 기존 Thymeleaf·Bootstrap·입력 보존·native control을 유지하며 R2 위험인 잘못된 DRAFT 허용을 우선 검증했다. 운영/실제 DB 미확인을 성공으로 대체하지 않는 release-readiness-gate 판정을 유지했다.
- V73(여전히 로컬 미추적·미적용)에 불변 confirmation_restorations와 복구 완료 조회 함수를 추가했다. 원래 검수 ID·시각·버전·태그를 보존하고 증가한 유효 첨부 버전만 별도 저장한다. 이전 확인이 적용 당시 무효/무버전이면 복구로 유효하게 만들 수 없다. source/job/confirmation composite FK·단조 버전·중복 방지·단독 이력 삭제 금지·원문 cascade 및 deferred 원자적 완료 검증을 추가했다. 단순 is_current=true 재활성화는 차단한다.
- CurrentMapper·ReviewMapper·ReviewService가 완료된 동일 복구 근거를 사용한다. 확인 응답의 원래 버전은 바꾸지 않고 confirmedClassification.binding을 additive로 제공한다. 화면은 원래 검수와 복구된 유효 버전을 구분하고 새 검수로 표현하지 않는다. DRAFT는 현재 유효 버전·정확한 확인/판정/set hash·기존 권한/활성 카탈로그가 맞을 때만 허용하며 자동 활성화는 없다.
- **작성 경로 한계**: 현재 복구 근거 생성은 DB 계약 테스트 fixture뿐이다. 승인/실행 API·전체 영향 미리보기·후속 역할/입력 전체 appliedInputHash 비교·base 경로 재개·확인 없는 경우·일반 ENFORCE 원복·배치 ROLLING_BACK 집계는 아직 구현해야 한다. 이력 테이블은 운영 승인 근거를 대체하지 않는다.
- 표적 명령 `.\gradlew.bat :test --tests '*AnnouncementAttachmentReviewServiceTest' --tests '*AnnouncementAttachmentMapperBindingTest' --tests '*MigrationContractTest' --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'` 성공(31초). 최종 ReviewService41/MapperBinding23건 모두 통과했다. PG6건(원본 보존/최신 DRAFT, 단순 재활성화/미완료 원복 거부, 이전 무효 확인 거부, 새 검수 보호, 불변/cascade, 후속 base 변경)을 추가했지만 실행되지 않았다.
- 최종 명령 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는 **BUILD SUCCESSFUL in 3m 44s**,16 task 전부 실행이다. root XML16:52:01 KST **170 suite/1394건 =1246통과·148조건부 생략·실패0·오류0**. job PG124건은 전부 생략이다. extractor XML16:52:04 KST **25통과·생략0·실패0**. 기존 MockBean removal11/Log4j provider/unchecked 경고는 유지된다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs` **32통과·실패0·생략0**. 화면 JS2개 `--check`와 `git diff --check`도 exit0. 실제 브라우저/스크린리더/반응형 및 PostgreSQL/Linux/운영 E2E는 여전히 필수 미완료다.
- 최종 JAR SHA256 **b66e4e66e24bdaf4b77e058d641a7dcab0a4589405447586bee437dc4271b3e9**. 최신 V73·CurrentMapper·ReviewMapper·화면 JS2개의 source/JAR SHA 일치와 ConfirmationBinding DTO 포함을 확인했다. Spring JAR의 PDFBox/POI/hwplib/extractor parser·fixture 생성기 혼입은0개다. 해당 JAR을 운영에 배포하지 않았다.
- HEAD/master와 TLS 검증을 유지한 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`는 **ae893b87348a9bd1cb0893763f6cad5047093a24**로 일치한다. 전체 변경·미추적253경로/staged0/V1~V72 변경0/ATT62행을 유지했다. 제한된 credential 패턴 검사252개 텍스트 파일 중 matching0이며 전체 보안 감사는 아니다.
- 커밋·푸시·배포·운영 DB·정책 게시·ENFORCE·운영 데이터 적용은 미실행이다. 이번 회차에 AWS/Actions/health·운영SHA/운영 브라우저를 조회하지 않았다. 기존 Linux/PG·GitHub write/AWS/운영 세션 blocker에 보안 완화·재인증 반복을 하지 않았다.
- 직접 실행한 Node/Gradle single-use daemon/테스트는 종료했다. Java는 기존 사용자 PID20900만 남았고 해당 Node QA 실행은0개다. 산출물/테스트 증거·기존 변경/미추적 파일은 보존했다.
- 다음 구현 우선순위: **원복 영향 미리보기→정확한 범위/건수 승인→항목별 전체 지문 CAS/이전 binding 복구→실패/삭제/미적용 분리 집계와 API/worker**. 이후 전체 배치 UI/분할 ledger·정책 QA 게시·전체 Provider/profile·실제 Linux/DB·운영 exact-SHA 배포·역할별 브라우저 E2E까지 진행해야 한다.

## 2026-09-11 17:25 KST — Gate 5 배치 원복 승인·항목 실행

- 이번 회차는 **진행(progress)**이다. 직전 확인 복구 근거 위에 배치 원복 영향 조회·ADMIN 승인·고정 전체 대상·항목별 복구 worker·현재 결과 조회를 연결했다. 전체 목표/ATT62 범위를 유지하며 출시 판정은 **Not ready**다. 실제 DB·일반 자동 ENFORCE 원복·전체 batch UI/분할 ledger·정책 QA/게시·모든 Provider/profile·운영 배포/브라우저 E2E는 미완료다.
- cwd/루트 AGENTS.md를 가장 먼저 확인하고 long-goal-operating-protocol과 release-readiness-gate를 적용했다. 기존 V1~V72/계층/DB-first·SSR/원문·운영 공고 보존 정책을 유지했다. 별도 에이전트를 실행하지 않았다.
- `/api/v2/admin/announcement-attachment-batches/{batchId}/rollback`에 GET preview/items/actions/{id}, POST 승인 API를 추가했다. ADMIN 쓰기·ADMIN/OPERATOR/APPROVER 조회, Service 계정 검증·CSRF·UUID 멱등 키·정확한 버전/hash/scope/대상/삭제/base 재개/확인 복구/적용 취소 수 및 한국어 검증 메시지를 적용했다. 202 접수와 복구 성공을 구분하고 강제 덮어쓰기·임의 source 목록·정책 변경 필드는 받지 않는다.
- source UUID 순서→batch 잠금 아래 원복 미리보기와 전체 범위를 다시 계산한다. APPLIED/APPLY_PARTIAL_FAILED/APPLY_PAUSED의 적용 완료분 전부를 대상으로 고정하며 초기 충돌도 숨기지 않는다. 적용 중지의 남은 PENDING은 명시된 수만 APPLICATION_CANCELLED_BY_ROLLBACK으로 끝내고 다시 적용하지 않는다. 승인/대상/상태/대기 취소는 deferred DB 계약으로 함께 완료한다.
- 항목 worker는 source→batch 잠금, 승인 지문·적용 후 전체 live hash/current/버전·검수/link/활성 작업 재검증 후 pointer/정책/review binding과 이전 evaluation을 복원한다. 첨부 버전은 applied+1이며 base 버전은 유지한다. 이전에 유효했던 확인만 새 근거를 기록하여 복구하고, 이미 무효인 확인은 STALE로 남긴다. 확인이 없던 batch source는 base 경로로 복구 가능하다. 원문·첨부 이력·운영 공고·지원 진행을 삭제하거나 자동 활성화하지 않는다.
- 다운로드와 분리된 `SANEB_ANNOUNCEMENT_ATTACHMENT_ROLLBACK_ENABLED` 기본 false를 추가했다. true여도 승인된 ROLLING_BACK만 DB에서 처리한다. 오류는 항목 transaction 전체를 취소한 후 별도 실패 횟수/30초 backoff를 기록하고 최대3회 뒤 FAILED로 종료한다. 최초 전체 scope에 취소/미적용/실패/삭제가 있으면 ROLLBACK_PARTIAL_FAILED이며 일부 성공을 전체 성공으로 표시하지 않는다.
- V73에 rollback_actions/items·batch/job 승인 FK·복구 결과 버전·확인 FK·실패/시도/대기 컬럼·불변 승인/대상/terminal 검증을 추가했다. 정적 재검토에서 직전 회차의 **중복 uq_att_job_source ADD 선언1건**을 발견해 제거하고 한 번만 선언되는지 회귀를 추가했다. 이전 로컬 빌드가 실제 migration 성공을 증명하지 못했던 결함이며 최신 PostgreSQL 실행은 여전히 필요하다.
- 표적 `.\gradlew.bat :test --tests '*BatchRollback*' --tests '*AnnouncementAttachmentMapperBindingTest' --tests '*MigrationContractTest' --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는 성공(40초). 신규 Service20/HTTP16/Scheduler2 및 Mapper1/MigrationContract1 검증을 추가했다. DB 전용 테스트8건(기본 경로/멱등성, 승인 뒤 새 검수, 동시 worker1회, 삭제 분모, 이전 무효 확인 유지, DB 우회/이력 불변, 적용 중지 대기 취소, 후속 DRAFT 보존)을 추가했고 기존 확인 복구 테스트도 실제 승인/worker에 연결했다. **job PG132건은 모두 생략**이다.
- 첫 전체 실행 handle87895는 계속 살아 있었으나 테스트 JVM heap512 MiB 중 약501 MiB 사용과 Spring 컨텍스트 생성 스택을 확인했다. 이 실행을 성공으로 처리하지 않고 소유 테스트 worker PID25124를 command line/생성 시각 확인 후 종료했다(Gradle exit1,6분11초). 사용자의 기존 PID20900은 보존했다. `build.gradle` test에 Spring 컨텍스트 cache.maxSize=4만 추가했으며 테스트/조건/판정/병렬 JVM 수를 축소·증가하지 않았다. 재실행에서 실제 설정 적용과 관측 heap 약233 MiB를 확인했다.
- 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는 **BUILD SUCCESSFUL in 3m 32s**,16 task 전부 실행이다. root XML17:24:03 KST **173 suite/1442건 =1286통과·156조건부 생략·실패0·오류0**. extractor XML17:24:07 **25통과·생략0·실패0**. 기존 MockBean removal11/Log4j provider/unchecked 경고는 남는다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs` **32통과·실패0·생략0**. `git diff --check` exit0. 실제 PostgreSQL/Linux/운영 브라우저·접근성/반응형 E2E는 이번 실행에서 검증하지 않았다.
- 최종 JAR SHA256 **515b0fe46c2e8cabe327d2046236052982a949439ce426b06d91871f3e85beb7**. 원복 관련 class18개, 최신 V73·RollbackMapper·application.yml의 source/JAR SHA 일치를 확인했다. Spring JAR에 PDFBox/POI/hwplib/extractor parser·fixture 생성기 혼입0개다. JAR 배포/운영 migration은 실행하지 않았다.
- HEAD/master 및 TLS 검증을 유지한 원격 조회 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`는 **ae893b87348a9bd1cb0893763f6cad5047093a24**로 일치한다. 변경·미추적265경로/staged0/V1~V72 변경0. 제한 credential 패턴은264텍스트 파일 중 matching0이며 전체 보안 감사가 아니다. 원문·기존 사용자 변경·미추적 파일을 삭제·임의 staging하지 않았다.
- 커밋·푸시·배포·운영 정책/ENFORCE/원복 flag·운영 DB/기존 데이터 적용은 미실행이다. AWS/Actions/운영 health/SHA/브라우저를 이번 회차에 조회하지 않았다. 기존 Linux/DB·GitHub write/AWS/운영 세션 blocker를 보안 완화나 반복 재인증으로 우회하지 않았다.
- 최종 Java는 기존 사용자 PID20900만 남았으며 소유 Gradle/테스트 및 해당 Node QA 프로세스는 종료됐다. 최신 build/JUnit/JAR 증거와 작업 트리는 보존했다.
- 다음 필수 구현: **일반 자동 ENFORCE job의 적용 전/후 근거와 조건부 복구 계약**, 이어서 전체 배치 UI·분할 ledger·정책 전체 QA/게시·모든 Provider/profile. 기존 Linux 환경/권한이 확보되면 최신 migration과132개 job DB 테스트부터 실행하고 exact-SHA 배포·운영 역할별 브라우저 E2E까지 증명해야 한다.

## 2026-09-11 17:47 KST — Gate 5 일반 작업 예약 전·적용 후 근거

- 이번 회차는 **진행(progress)**이며 전체 목표 판정은 **Not ready**다. 일반 작업이 예약 단계에서 이전 확인/current를 STALE로 만들고 첨부 버전을 증가시키는 순서를 실제 Job/Collection/Retry/Role/Evaluation Service와 Mapper에서 확인했다. 배치 원복의 expectedAttachmentVersion을 일반 작업에 그대로 적용하면 예약 전 유효 확인의 버전을 잘못 판정하므로 근거 저장부터 보완했다. 원복 실행 완료를 주장하지 않는다.
- cwd/루트 AGENTS.md를 확인하고 long-goal-operating-protocol의 최소 변경·대상 검증→전체 회귀 순서를 적용했다. 이전 진행/격리 경계 기록은 참고만 했고 현재 Git·파일·테스트 결과를 별도로 확인했다. 서브에이전트나 브라우저를 실행하지 않았다.
- V73(미추적·미적용)에 reservation_source_version/reservation_attachment_version/reservation_locator_hash 및 is_reservation_previous_evaluation_current/is_reservation_confirmation_valid를 추가했다. BEFORE INSERT가 source 잠금 아래 이전 binding과 요청을 대조하며, 확인의 원래 또는 완료된 복구 버전·동일 base/content/rule/policy/SEALED hash를 확인한다. 유효성 flag는 binding 근거이며 원복 승인이나 전체 검수의 대체물이 아니다. 원래 confirmation은 수정하지 않는다.
- 일반 작업 버전은 예약 전 S/A → 예약 S/A+1 → 판정 저장 S/A+2로 명시했다. EvaluationMapper의 동일 lease-fenced 완료 transaction에서 applied_source_version과 applied_input_hash를 함께 저장한다. 지문은 작업/정책/규칙/근거 hash와 current/버전/출처/기관 metadata·후속 확인/link/다른 활성 job을 묶으며 원문·URL을 이력에 복사하지 않는다. 정책 OFF/퇴역이나 시도 횟수는 포함하지 않는다.
- DB guard는 예약 근거·이전 연결·APPLIED 근거의 변경, 다른 source locator로의 최종 적용, COLLECT_ONLY preview의 APPLIED 승격과 일반 job의 원복 상태 위조를 거부한다. V72 과거 행을 복구 가능하게 소급 보정하지 않는다. PARTIAL_FAILED의 검수 의무 적용을 파일 성공으로 표현하지 않는다. 신규 예약에는 첨부 버전 두 단계의 정수 여유를 요구한다.
- 새 공개 API/UI·운영 flag는 추가하지 않았다. **일반 원복 영향/승인 API·원자적 복구·미완료/실패 예약 해소는 여전히 미구현**이다. 기존 batch 원복도 실제 PostgreSQL/운영 검증은 남아 있다. DB 문서11.16과 ATT049/050 및 Linux 실행 대상140건을 갱신했다.
- 표적 명령 `.\gradlew.bat :test --tests com.saneb.db.MigrationContractTest --tests com.saneb.db.AnnouncementAttachmentMapperBindingTest --tests com.saneb.db.AnnouncementAttachmentJobIntegrationTest --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는26초에 성공했다. Mapper25/MigrationContract83은 통과, job PG140은 생략이었다. 이후 최종 SQL/컬럼 규칙 정리까지 전체 회귀를 다시 실행했다.
- 새 PG8건은 예약 이력 불변, 적용 이력/원복 위조 차단, 후속 확인 뒤 지문 불일치, COLLECT_ONLY 분리, 유효 확인 capture, 이미 무효인 확인 보호, locator 변경 시 transaction 취소, 부분 실패의 검수 보호를 다룬다. 기존 자동 신규 ENFORCE 테스트에도 예약/적용 버전과 지문 비교 assertion을 추가했다. **실제 PostgreSQL 실행은 없으며 정적 검사로 대체하지 않는다.**
- 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는 **BUILD SUCCESSFUL in3m21s**,16 task 전부 실행이다. root XML17:46:19 KST **173 suite/1453건=1289통과·164조건부 생략·실패0·오류0**. extractor XML17:46:23 **25통과·생략0·실패0**. 1차 전체 성공(3m31s)은 최종 SQL 보강 이전이므로 최종 증거와 구분한다. 기존 MockBean removal11/Log4j provider/unchecked 경고는 남는다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs` **32통과·실패0·생략0**, `git diff --check` exit0. JAR SHA256 **b8848ddc7b628e4d78747e39325100b7402087ac7cae0b37790362ccf31352b7**, 최종 V73/EvaluationMapper와 JAR 내 resource SHA 일치, 금지 parser/fixture 혼입0을 확인했다. 배포 증거가 아니다.
- HEAD/master와 TLS 검증을 유지한 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`는 **ae893b87348a9bd1cb0893763f6cad5047093a24**로 일치했다. 전체 변경/미추적265경로, staged0, V1~V72 변경0이다. 제한 credential 패턴265개 텍스트 파일에서 matching0이며 전체 보안 감사는 아니다. 이전 사용자 변경과 미추적 파일을 보존했다.
- 커밋·푸시·배포·운영 DB·정책 게시/ENFORCE/기존 데이터/원복은 미실행이다. AWS/Actions/운영 health/SHA/현재 인증 상태를 이번 회차에 재조회하지 않았다. 기존 Linux/PG·GitHub write/AWS·운영 세션 차단 기록을 현재 정상으로 대체하거나 보안 완화로 우회하지 않았다. 운영 브라우저 E2E는 명시적으로 승인된 필수 Gate이나 배포·운영 연결 조건이 미충족되어 이번에도 미검증이다.
- 소유 Gradle single-use daemon/테스트 및 Node QA는 종료됐다. 최종 Java 조회는 기존 사용자 PID20900만 확인했다. 검증 산출물과 작업 트리는 보존했다. 다음 필수 구현은 **이 근거를 사용하는 일반 원복 승인·실행과 실패 예약 해소**, 이어서 전체 배치 UI·분할 ledger·정책 전체 QA/게시·모든 Provider/profile·실제 Linux/PG·운영 배포/역할별 브라우저 E2E다.

## 2026-09-11 18:16 KST — Gate 5 일반 원복 승인·실패 예약 복구

- 이번 회차는 **진행(progress)**이며 전체 Gate/출시 판정은 **Not ready**다. 일반 작업의 예약 전·적용 후 근거를 사용하는 원복 영향 조회·ADMIN 승인·동기 복구·결과 영수증을 구현했다. 전체 목표와 ATT-001~062를 유지하며 로컬 성공을 실제 PostgreSQL/운영/브라우저 성공으로 대체하지 않는다.
- cwd와 루트 AGENTS.md를 먼저 확인하고 long-goal-operating-protocol 및 release-readiness-gate를 적용했다. 기존 V1~V72·기술스택·계층·사용자 변경사항을 보존했다. 서브에이전트와 브라우저는 실행하지 않았다.
- `/api/v2/admin/announcement-sources/{sourceId}/attachment-jobs/{jobId}/rollback`에 GET preview, POST 승인/복구, GET actions/{actionId}를 추가했다. ADMIN만 쓰고 ADMIN/OPERATOR/APPROVER가 조회한다. Service 계정 상태·권한, CSRF, UUID Idempotency-Key, 정확한 버전/previewHash/base 재개·확인 복구 영향 동의, 한국어 입력 검증, no-store를 적용했다. 영수증의 recordedAt은 기록 시각이며 현재 source 상태나 정확한 commit 시각을 뜻하지 않는다.
- `APPLIED`와 `FAILED_RESERVATION`을 분리했다. 후자는 FAILED/CONFLICT/CANCELLED + application PENDING + 현재 pointer 없음 + 예약 버전/문맥 일치만 허용한다. 실패 예약 복구가 원래 job 실패를 성공 또는 APPLIED로 바꾸지 않는다. 진행 중·다른 활성 작업·후속 검수/DRAFT/link·변경된 입력·근거 없는 과거 행은 강제 복구하지 않는다.
- source→job 잠금 아래 최신 중앙 상태와 승인 지문을 재검증하고 불변 승인 생성→source CAS→이전 판정 current→이전에 유효했던 확인만 복구→job 원복 결과→metadata 감사 기록을 한 transaction으로 수행한다. 일부 실패는 전체 취소되며 같은 키/actor/payload 재요청은 원래 영수증만 반환한다. HTTP/다운로드/worker 실행이나 새 운영 flag는 없다. 첨부 버전은 증가시키고 원본 확인·실패·적용 이력을 보존한다.
- V73(로컬 미추적·미적용)에 reservation_context_hash, normal_rollback_actions, job 복구 승인 FK와 불변/원자적 완료 제약을 추가했다. 기관/출처/base 문맥 변경도 잡도록 예약 지문을 고정한다. 중앙 recovery_state는 원문을 반환하지 않고 버전·영향·readiness·지문만 계산한다. 단독 승인 저장/부분 완료/후속 재시작/원복 결과 위조는 DB 제약으로 거부하도록 작성했다. **실제 SQL/trigger 실행은 아직 검증하지 않았다.**
- 기존 확인 유효 버전 조회 함수는 완료된 batch/normal 복구를 함께 처리한다. 일반 원복을 가짜 batch 이력으로 저장하지 않는다. 원래 확인이 이미 무효였다면 STALE로 남기고, 새 유효 버전과 원래 검수 기록을 구분한다. Current/Review/DRAFT 경로의 공통 binding 사용을 재확인했으며 자동 활성화/원문 삭제/운영 공고 변경은 없다.
- 첫 표적 실행은 잘못된 AttachmentBatchFingerprint import로 compileJava가 실패했다(16초). 실제 동일 service.impl 패키지의 helper를 사용하도록 import를 제거한 뒤 표적 실행이42초에 성공했다. 테스트 assertion이나 보안 조건을 약화하지 않았다. 이후 영수증 recordedAt 명칭과 malformed-state 차단 보강까지 포함해 전체 회귀를 다시 실행했다.
- 최종 명령 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는 **BUILD SUCCESSFUL in3m23s**,16 task 전부 실행이다. root XML18:09:14 KST **175 suite/1503건=1327통과·176조건부 생략·실패0·오류0**. NormalRollbackService21/HTTP15/MapperBinding26/MigrationContract84는 통과했다. extractor XML18:09:18 **25통과·생략0·실패0**. 기존 MockBean removal11/Log4j provider/unchecked 경고는 남는다.
- 일반 복구 PG12건을 추가했다: 정상/실패 예약 확인 복구, 신규 ENFORCE 정상/실패의 base 복구, 승인 후 새 검수/DRAFT/기관 변경, 동시 멱등 요청, 미완료 transaction 거부, 결과 불변/cascade, 무효 확인 보호, 후속 예약의 복구 확인 인식. **최신 job PG152건은 전부 생략**이다. 컴파일·Mapper 오프라인 검증은 이 테스트의 실제 PostgreSQL 통과를 뜻하지 않는다. Linux 실행 문서와 ATT049/050의 현재 구현/잔여 증거를 갱신했다.
- Node `--test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs` **32통과·실패0·생략0**. `git diff --check` exit0. JAR SHA256 **38b38e946430072b75db2de8d2c92c0861b2f598cd69cac202710cb334a27e8b**, 최신 V73/NormalRollbackMapper와 JAR resource 내용 일치, 복구 관련 class11개, 금지 parser/fixture 혼입0을 확인했다. 배포 증거가 아니다.
- 최종 master/HEAD는 **ae893b87348a9bd1cb0893763f6cad5047093a24**다. 이번 회차 TLS 검증을 유지한 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`도 같은 SHA였다. `git status --short --branch --untracked-files=all` 기준 변경·미추적275파일/staged0/추적 migration 변경0/ATT62행이다. 기본 short 출력은 미추적 디렉터리를 묶으므로74행이며 파일 수와 구분한다. 제한 credential 패턴275텍스트 파일에서 matching0이며 전체 보안 감사가 아니다.
- 커밋·푸시·배포·운영 DB·정책 게시/ENFORCE·기존 데이터 적용/원복은 미실행이다. AWS/Actions/운영 health/SHA/운영 브라우저를 이번 회차에 조회하지 않았다. 기존 Linux/PG·GitHub write/AWS·운영 세션 차단을 보안 완화나 반복 재인증으로 우회하지 않았다. 승인된 운영 브라우저 E2E는 배포·연결 조건 미충족으로 필수 미완료다.
- 소유 Gradle/테스트 및 Node QA는 종료됐다. 최종 관련 프로세스는 기존 사용자 Java PID20900만 확인해 보존했다. 작업 트리와 검증 산출물은 보존했다. 다음 구현은 **관리자 일반/배치 복구 UI와 전체 분할 처리 ledger**, 이어서 정책 전체 QA/게시·모든 Provider/profile·실제 Linux/PG·exact-SHA 운영 배포 및 역할별 브라우저 E2E다.

## 2026-09-11 18:40 KST — Gate 5 일반 작업 원복 관리자 화면

- 직전 회차는 일반 원복 API/원자적 복구를 추가한 **진행(progress)**이며, 이번 회차도 해당 계약을 관리자 화면에 연결한 진척이다. 전체 목표·ATT62·운영 브라우저 요구를 유지한다. 출시 판정은 **Not ready**다.
- cwd/루트 AGENTS.md를 먼저 확인했다. long-goal-operating-protocol, UI/UX 운영 원칙과 필수·폼/상태·업무도구·한국어·접근성·DoD reference, frontend-ui-engineering, release-readiness-gate를 적용했다. Design Read는 일반 작업 탐색→현재 영향→ADMIN 승인→불변 영수증/현재 유효 상태 구분이며 R2 위험으로 처리했다. 새 라이브러리·기술스택·모션·서브에이전트는 없다.
- 기존 단건 검수 화면에 별도 일반 원복 영역을 추가했다. ADMIN만 승인하고 OPERATOR/APPROVER는 목록·영향·영수증을 조회한다. 원문/작업·버전·기본 경로 재개·이전 확인 복구·원래 실패 보존·공고1건/HTTP0을 표시하고 사유·직접 동의를 받는다. source/preview/영수증 불일치는 성공으로 표시하지 않으며 원문 삭제·자동 활성화·외부 수집을 하지 않는다.
- GET `attachment-recovery-jobs`를 Controller→Service→DAO→Mapper/JobSummary로 추가했다. 현재 원문에 속한 일반 작업 전체를 최신순 페이지/건수 조회하며 실패·미적용·기복구 이력도 포함한다. PRODUCTION/비제외/batch_id NULL을 list/count에 공통 적용한다. source 부재/QA/제외404와 일반 작업0건을 구분한다. actionId로 새로고침 뒤 영수증을 찾으며 사유·actor·멱등 키·실행 snapshot·원문은 목록에 없다. 이번 회차는 migration을 추가하거나 수정하지 않았다.
- `saneb-attachment-recovery.js`는 순수 계약 검사와 mount를 분리했다. 읽기 응답을 세대에 묶고 실패·빈 상태를 구분한다. 비민감 페이지/job 선택만 URL에 보존하며 사유·key/payload는 탭 메모리 밖에 기록하지 않는다. 작업/입력/기준 변경 후 동의를 해제한다. native details/button/fieldset/textarea와 기존 반응형·focus-visible CSS를 재사용한다. SSR 권한 속성/라벨/오류 연결/초기 disabled를 검증했다.
- 응답 유실·5xx·불일치 영수증 뒤에는 원래 key/body를 보존하고 수집/검수/DRAFT/다른 원복을 잠근다. 재확인401/403/409도 최초 실행이 실패했다는 증거가 아니므로 새 키로 바꾸지 않는다. 명확한 최초409는 최신 기준 조회 후 다시 검토한다. 성공은 요청한 mode/원문/job/버전+1/영향/영수증이 맞을 때만 표시하고 새 유효 상태는 별도 refresh한다. 기존 CSRF wrapper의 동일 출처 전달과 서버 ADMIN 검증을 유지했다.
- 최초 Node 실행은 기존 테스트의 `operations?.stale || !canManage` 인접 문자열 assertion1건이 새 recovery 잠금 삽입으로 실패했다(47/48). 같은 submit guard에 기존/신규 조건 모두 존재하는지 검사하도록 바꿔 약화 없이 해소했다. 최초 서버 표적 실행은 초기 화면에 원복 영수증 제목이 없어 SSR3건이 실패했다(84건 중3실패,47초). 제목을 실제 추가한 뒤 표적84건이44초에 통과했다.
- 최종 명령 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`는 **BUILD SUCCESSFUL in3m56s**,16 task 전부 실행이다. root XML18:37:50 KST **175 suite/1517건=1340통과·177조건부 생략·실패0·오류0**. extractor XML18:37:54 **25통과·생략0·실패0**. 기존 MockBean removal11/Log4j provider/unchecked 경고는 남는다.
- 일반 작업 이력 PostgreSQL1건(원문 범위·실패 상태·페이지·영수증·QA 비노출)을 추가했고 소스 검토에서 데이터 목적 enum을 V67의 PRODUCTION/QA로 맞췄다. **최신 job PG153건은 전부 생략**이다. 실제 DB transaction/migration/SQL 통과는 아니며 이전 통과 기록으로 대체하지 않는다. Service26/HTTP22/Mapper27/SSR9는 로컬 통과했다.
- Node `--test scripts/qa/attachment-recovery-ui.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-contract-report.test.mjs` **48통과·실패0·생략0**. 새16건은 실제 mount/state와 최소 DOM/event 포트를 이용하며 실제 브라우저 렌더링/E2E가 아니다. 새 recovery/main/기존 fixture server의 Node `--check`와 `git diff --check`도 exit0이다. 기존 합성 fixture의 새 JS asset/조회 전용 속성/빈 이력 응답을 연결했지만 서버나 브라우저를 실행하지 않았다.
- JAR SHA256 **039674955d330d1726d1e78c230efe251022fd122df8e0f8588564ae23c82b65**. 최신 template/recovery JS/main JS/NormalRollbackMapper/V73의 source/JAR 내용 일치와 이력 Controller 포함을 확인했다. 웹 JAR에 금지 parser/fixture 혼입0이다. 운영 배포 증거가 아니다.
- master/HEAD 및 TLS 검증을 유지한 원격 master 조회는 **ae893b87348a9bd1cb0893763f6cad5047093a24**로 일치했다. 변경·미추적279파일/staged0/추적 migration 변경0이며 사용자 변경·미추적 파일을 보존했다. 제한 credential 패턴279텍스트 파일 중 matching0이며 전체 보안 감사는 아니다.
- API24.17과 새 `announcement-attachment-recovery-ui-2026-09-11.md`, ATT049/050, Linux 실행 대상153건을 갱신했다. 입력 영구 저장 대신 탭 메모리·이탈 경고·서버 영수증 조회를 쓰는 SHOULD 예외와 실제 키보드/반응형/스크린리더·운영 브라우저 미검증을 명시했다.
- 커밋·푸시·배포·운영 DB/정책/ENFORCE·기존 데이터 적용/원복은 미실행이다. AWS/Actions/health/운영SHA/운영 브라우저는 이번 회차에 조회하지 않았다. 기존 외부 권한/접속·Linux/PG 차단을 보안 완화나 반복 시도로 우회하지 않았다. 실행한 Gradle single-use daemon/테스트/Node는 종료됐고 기존 사용자 Java PID20900만 남겨 보존했다.
- 남은 필수 작업: **전체 배치 작업 화면과 분할 처리 ledger**, 정책 전체 QA/게시·모든 Provider/profile·실제 Linux/PG, exact-SHA 운영 배포·정확한 대상 승인 후 운영 적용 및 역할별 실제 브라우저 E2E. 일반 화면 구현만으로 전체 UI·ATT62·장기 목표 완료를 선언하지 않는다.

## 2026-09-11 23:57 KST — Gate 4·5 배치 관리자 작업 흐름

- 직전 회차는 Gate 개수의 상태 보고로 구현 진척이 없었다. 현재 파일·AGENTS·Git·기존 DTO/Service/Mapper/SSR을 확인한 뒤 다음 미구현인 배치 화면을 진행했다. 전체 Gate0~8은 모두 최종 통과 전이며 출시 판정은 **Not ready**다. 과거의 “Gate5 작업” 제목을 Gate0~4 완료로 해석하지 않는다.
- long-goal-operating-protocol·UI/UX 운영 원칙/필수 reference·frontend-ui-engineering으로 R2 영향/명시적 동의/오류 복구를 설계하고 release-readiness-gate로 실행 증거를 분리했다. 서브에이전트·라이브러리·기술스택 변경 없음.
- `/app/admin/announcement-attachment-batches` SSR 경로와 수집 공고 검수의 진입 링크를 추가했다. ADMIN만 변경하고 OPERATOR/APPROVER는 조회한다. no-store·초기 disabled·한국어 설명/라벨/상태·네이티브 폼·공통 CSRF wrapper를 유지한다. API/DB는 기존24.12~24.16 계약을 소비하며 이번 migration 수정은 없다.
- 게시 정책 페이지·서울 시각 범위·출처·상한 입력, 외부HTTP0 범위 미리보기, 전체/이번 대상/잔여/보호 제외와 준비 사유, 별도 예약/수집/중지/재개/취소, 봉인 결과/선택, 적용/중지/재개, 원복 영향/승인/항목 결과를 연결했다. 배치 정부24 입력은 실제 서버 계약 `GOV24`를 사용한다. URL/파서/profile을 관리자가 선택하지 않는다.
- 미리보기 항목은 전체 페이지를 읽고 총수·중복·최종 preview ID/hash/버전/입력을 대조한다. 화면은10개씩 보여주되 전체 선택을 보존한다. 미저장 선택은 저장/명시적 복원 전 다른 배치 이동·새 예약을 막는다. 기본/이전 첨부/이전 관리자 확인/제안 태그와 차이, 파일·추출 근거를 구분하며 원문 상세는 기존 검수 화면으로 연결한다.
- 명시적 영향/사유/동의 뒤만 요청한다. 변경된 입력/대상은 동의를 해제한다. 수집 CAS 응답 유실은 최신 상태를 읽고 별도 새 기준 동의로 해제하며 최초 요청 성공을 추정하지 않는다. 멱등 요청의 유실/5xx/불일치 영수증 뒤401/403/409도 원래 key/body를 유지한다. 비동기202 접수를 실제 성공으로 표시하지 않는다. 접수 뒤 목록 갱신만 실패해도 새 batch/action URL과 확인한 응답을 보존한다.
- 초기 Node19건 통과 뒤 모의 전체 흐름과 접수 후 조회 실패를 추가했다. 선택 보존 수정 중 닫는 괄호 누락으로 Node 구문 오류1회가 있었고 해당 괄호를 수정해 전부 재실행했다. assertion/권한/동의 조건을 약화하지 않았다. 최종 새 Node21건, 기존48건 포함 **69통과·실패0·생략0**다. 실제 브라우저가 아닌 최소 DOM/event·모의 API 포트 테스트다.
- 표적 Gradle `:test --tests '*AnnouncementAttachmentBatchViewControllerSmokeTest' --tests '*AnnouncementAttachmentViewControllerSmokeTest' --tests '*AnnouncementAttachmentBatch*Test'`에 공통 no-daemon/max-workers1/Windows-ROOT 옵션을 붙여 **BUILD SUCCESSFUL in1m3s**,13suite/187통과·생략0을 확인했다.
- 전체 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **BUILD SUCCESSFUL in3m2s**,16task 전부 실행. root176suite/1525건=**1348통과·177생략·실패0·오류0**, 추출기2suite/25통과·생략0. **job PG153건 전부 생략**이므로 실제 SQL/migration/transaction 통과는 아니다. 기존 MockBean removal11/unchecked/Log4j provider 경고가 남는다.
- 새 JS2개 `node --check`, `git diff --check` 통과. JAR SHA256 **eae0140cdcfc42a409290f7fa994c38845f5d73164df48d7f78b152198134bbd**. JS2개/template/공통CSS/V73가 현재 source와 JAR에서 동일했고 금지 parser/fixture class 패턴0이다. 신규6텍스트 파일의 제한 credential 패턴 matching0이며 전체 보안 감사가 아니다.
- master/HEAD와 TLS 검증을 유지한 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`는 **ae893b87348a9bd1cb0893763f6cad5047093a24**로 동일했다. 변경/미추적285파일·staged0·추적 migration 변경0. 사용자 변경·기존 미추적 파일을 보존했다. Node QA/검사와 Gradle/test는 종료됐으며 현재 남은 Node는 이번 QA 명령이 아닌 별도 프로세스로 확인하여 임의 종료하지 않았다. 현재 java 프로세스는 없었다.
- 커밋·푸시·배포·운영 DB·정책 게시/ENFORCE·운영 배치 수집/적용/원복은 실행하지 않았다. AWS/Actions/health/운영SHA·운영 브라우저의 이전 차단은 이번에 재검증하지 않았다. 원격SHA 읽기 성공이 push 권한 또는 배포 성공을 뜻하지 않는다.
- 남은 작업은 배치 전용 화면의 실제 DB/브라우저 검증·과거 승인 전체 이력 탐색, **1000건 초과 전체 분할 ledger**, 정책 전체 QA/게시/화면·모든 Provider/profile·실제 Linux/PG, exact-SHA 운영 배포와 정확한 승인 범위 적용·운영 역할별 E2E다. 새 화면 구현을 전체 Gate/ATT62 통과로 보고하지 않는다.

### 2026-09-12 00:35 KST — 전체 후보 고정·분할 소속 V74/API

- 직전 Gate 수 확인 회차는 상태 보고로 구현 진척 없음이었다. 이번 회차는 실제 migration/API/검증 계약을 추가한 진척이다. 전체9 Gate는 여전히 최종 통과0이며, 부분 완료6·차단2·대기1이다. Gate1·5는 부분 완료, 배포 `Not ready`를 유지한다.
- `AGENTS.md`와 장기 작업 운영 스킬을 완독하고 목표·명세·검증을 먼저 정리했다. `announcement-attachment-backfill-ledger-2026-09-12.md`가 상세 계약이며 전체 기존 데이터 처리 목표를 목록 고정으로 축소하지 않았다. V1~V73에는 이번 회차의 편집이 없고 V74를 추가했다.
- V74의 run/segment/item은 전체 후보를 collected_at/source UUID 순서로 고정한다. 전체 SQL에 LIMIT가 없고 각 source는 한 분할에만 속한다. 기관·출처·기본/첨부 버전·검수 binding 지문으로 입력 변화를 표시한다. 원문/base cascade 삭제 후 최초 후보/분할 분모를 보존하고 삭제 수만 증가시킨다. 최초 불완전 저장·후속 추가·이동·직접 삭제·관리 이력 변경은 DB guard 대상이다.
- `/api/v2/admin/announcement-attachment-backfills` scope-preview/전체 목록 고정/목록·상세/분할·항목 페이지 API를 추가했다. POST의 CSRF와 Service ADMIN 쓰기/읽기3역할, no-store wrapper, 임의 URL/profile/sourceIds/maximumCount 거부를 검증했다. `INVENTORIED`는 목록 고정이며 job/HTTP/source 쓰기/수집·적용 승인이 아니다. scope/hash/전체 후보 수/actor/key/사유를 고정하고 진행 중 동일 키는409·같은 입력 재확인을 안내한다.
- 수정/추가한 주요 경로: `AttachmentBackfillRequests/Responses/Rows`, `AnnouncementAttachmentBackfillController/Service/ServiceImpl/Dao/Mapper`, `V74__add_attachment_backfill_inventory.sql`, `SecurityConfig`, Service·HTTP·Mapper·migration 정적 시험, 실제 PG `AnnouncementAttachmentBackfillIntegrationTest`, API24.19·DB11.18·QA trace. 기존 `attachmentMigrationTest`와 Linux 필수 보고서 판정기에 새 PG suite를 포함했다. 필수 보고서 누락/생략은 실패다.
- 최초 대상 실행: `:test --tests '*AnnouncementAttachmentBackfill*Test' --tests '*AnnouncementAttachmentMapperBindingTest' --tests '*MigrationContractTest' --tests '*AnnouncementAttachmentBatchControllerSmokeTest'` **BUILD SUCCESSFUL 37s**,6suite/183건=178통과·5조건부생략·실패/오류0. 이후 기관 지문·PG 경합2건·transaction annotation 검증을 보완했다.
- 최종 코드로 ` .\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'` 재실행: **BUILD SUCCESSFUL 2m47s**,16task 전부 실행. root179suite/1576건=**1392통과·184생략·실패0·오류0**. 추출기2suite/25통과·생략0. 새 Service23·HTTP19 통과. job PG153·분할 PG7·migration2는 전부 생략이며 최신 SQL/무결성/원자성 실행 증거가 아니다. 기존 MockBean removal11/unchecked/Log4j provider 경고는 남는다.
- `node --test scripts/qa/attachment-batch-ui.test.mjs scripts/qa/attachment-recovery-ui.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-contract-report.test.mjs` **69통과·실패/생략0**. 모의 API와 보고서 판정기 검증이며 실제 브라우저가 아니다.
- `docker version --format '{{.Server.Version}}'`: dockerDesktopLinuxEngine named pipe가 없어 연결 실패. docker/WSL CLI는 설치돼 있으나 실행 가능한 Docker DB 환경을 확보하지 못했다. `wsl --status`의 기본 배포판 표시는 실제 worker/DB 성공 근거가 아니다. Windows 격리 우회·호스트 추출 fallback·TLS 비활성화는 하지 않았다. 실제 PG 전용 task/Linux 격리/실파일/운영 브라우저는 이번 회차 미실행이다.
- 최종 JAR SHA256 **1d9b997842bed3436e67d277bb16afd21c67f4414052e957cd10c002a5f061dd**. JAR의 V74/새 Mapper/Controller·ServiceImpl class가 현재 source/build 출력과 동일했다. 제한한 web parser library 패턴0, 신규13파일의 제한 credential 패턴0, 기본 설정 `git diff --check` 종료0·지적0이다. 이는 전체 보안 감사가 아니다.
- Git master/HEAD **ae893b87348a9bd1cb0893763f6cad5047093a24**, 00:33 TLS 검증을 유지한 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`도 동일. 변경·미추적298파일/staged0/추적 migration 변경0. 기존 사용자 변경·미추적 파일을 보존했다. Node QA와 Gradle/test handle은 종료됐고 java 프로세스0을 확인했다. 개인 메모리는 수정하지 않았다.
- 커밋/푸시/배포/운영 DB/정책 게시·ENFORCE/기존 데이터 실행은 하지 않았다. GitHub write·Actions/AWS 권한·운영 URL/역할 인증·운영 SHA/health는 이번에 재확인하지 않았다. 원격SHA 읽기는 push·배포 증거가 아니다.
- **다음 필수 작업:** 고정 segment→기존 batch 예약의 정확한 소속·일대일 연결/삭제 전후 대조, 수집·적용·원복 전체 상태 집계, 관리자 분할 UI. 이후 정책 전체 QA/게시/UI, 모든 Provider/profile, 최신 실제 PG/Linux·실파일 QA, exact-SHA 운영 배포·정확한 승인 범위 실행·역할별 운영 브라우저 E2E까지 계속한다. 어떤 항목도 단순 개선 사항으로 이관하거나 완료로 계산하지 않는다.

### 2026-09-12 01:28 KST — V75 검증 회수·전체 분할 관리자 UI 연결

- 직전 회차는 Gate 수 상태 보고로 구현 진척 없음이었다. 이번 회차는 중단 없이 남은 Gradle handle40223의 종료를 확인하고 전체 목록→분할→기존 배치 UI를 구현한 **진척**이다. 전체 Gate0~8 최종 통과0·배포 판정 **Not ready**를 유지한다.
- cwd/AGENTS/Git/현재 DTO·Service·Mapper·화면·테스트를 재확인했다. long-goal-operating-protocol, UI/UX 운영 원칙과 필수 reference, frontend-ui-engineering, release-readiness-gate를 적용했다. R2 영향·별도 동의·오류 복구와 실제 검증 경계를 설계했다. 서브에이전트·새 의존성·스택 변경 없음.
- 이전 V75 회차의 전체 실행은 **BUILD SUCCESSFUL 2m57s**,16task 실행이었다. 01:05 XML의 root181suite/1621건=1432통과·189생략, 추출기25통과를 확인했다. V75 분할 Service15·Controller20 통과이며 실제 PG 전체 분할12·job153·migration2는 모두 생략이다. 실행 관측이 늦었다는 이유로 이전 handle을 중복 재시작하지 않았다.
- V75는 고정 소속만 사용하는 내부 batch 예약·분할당 불변 일대일 연결·원자적 소속/삭제 분모 계약·수집 시작/claim/매 HTTP 입력 변경 guard와 전체 세 차원 집계를 추가한 로컬 구현이다. V1~V74는 이번 UI 회차에 편집하지 않았다. 실제 SQL/trigger/1001건 경합의 실행 증거는 아직 없다.
- 새 `/app/admin/announcement-attachment-backfills`에 전체 목록 탐색·게시 정책/필터 입력·전체 후보/보호 제외·고정 승인·분할/고정 항목 페이지·준비/삭제/입력 변화·분할 예약·영수증·전체 수집/적용/원복 집계를 연결했다. 원래 전체 분모와 페이지 일부를 구분한다. ADMIN만 쓰고 OPERATOR/APPROVER는 조회하며 외부/익명은 SSR에서 차단한다. no-store·초기 disabled·미체크 동의·한국어·공통 CSS/네이티브 폼·textContent를 유지한다.
- 예약 뒤 기존 batch 화면에서 수집/중지/재개/취소/선택/적용/원복을 별도 승인한다. batch의 검증한 backfillRunId/segmentNo가 있을 때만 원래 전체 분할 복귀 링크를 표시한다. 임의 URL/profile/sourceIds·자동 실행·정책 게시·운영 공고 활성화는 없다. 실패/취소한 연결을 교체하지 않고 잔여의 새 범위 검토를 안내한다.
- 범위/사유 수정·재조회는 동의를 해제한다. 응답 유실 이후401/403/409도 동일 요청 키/본문을 보존한다. 다른 조회 성공이 초기 실패를 감추지 않고, 후속 목록 조회만 실패해도 확인한 영수증·배치 링크는 유지한다. 원문/사유·요청 키의 영구 자동 저장은 하지 않는 SHOULD 예외를 문서화했다. 비민감 URL·이탈 경고·다른 탭 로그인·서버 이력으로 보완하며 실제 브라우저 복구 검증은 남는다.
- 표적 `.\gradlew.bat :test --tests '*AnnouncementAttachmentBackfillViewControllerSmokeTest' --tests '*AnnouncementAttachmentBatchViewControllerSmokeTest' --tests '*AnnouncementSourceViewControllerSmokeTest'` +공통 no-daemon/max-workers1/Windows-ROOT 인자: **32초 성공**,3suite/26통과·생략/실패0. 신규 SSR8건을 포함한다.
- 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **BUILD SUCCESSFUL 3m19s**,16task 모두 실행. root182suite/1629건=**1440통과·189생략·실패/오류0**, 추출기2suite/25통과·생략0. job PG153·전체 분할 PG12·migration2 전부 조건부 생략이며 실제 DB 성공이 아니다. 기존 MockBean removal11/unchecked/Log4j provider 경고는 남는다.
- `node --test scripts/qa/attachment-backfill-ui.test.mjs scripts/qa/attachment-batch-ui.test.mjs scripts/qa/attachment-recovery-ui.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-contract-report.test.mjs`: **87통과·실패/생략0**. 신규 분할17·기존 배치 복귀 링크1건 포함이다. 1001건 전체 표시/2분할·준비 불가·권한·순번·삭제/세 단계 합계·응답 유실·접수 후 조회 실패를 모의 DOM/API에서 시험했다. 실제 렌더링·네트워크·DB 시험이 아니다. 새 JS2개 `--check`도 exit0. 수동 Linux workflow에도6파일 Node 검증을 연결했으나 원격 실행하지 않았다.
- JAR SHA256 **43322317b421f43579ffeeedca10e4302a88f2150dd96ad9aa73f90134215dbe**. V75/SegmentMapper/SegmentController와 새 template/JS2개·변경 batch JS2개·ViewController의9개 항목이 현재 source/build 출력과 JAR에서 일치했다. 제한 web parser library 패턴0. 신규6텍스트 파일의 제한 credential 패턴0이며 전체 보안 감사는 아니다.
- 01:24 `docker version --format '{{.Server.Version}}'`: dockerDesktopLinuxEngine named pipe 부재로 연결 실패. 실제 PG/Linux/실파일을 실행할 환경은 확보되지 않았다. TLS/Windows 보안·격리를 완화하거나 호스트 추출로 우회하지 않았다. GitHub push 권한/AWS/Actions/health/운영SHA/역할 브라우저 접속은 이번 회차 재확인하지 않았다. 과거 차단을 현재 성공/실패 원인으로 단정하지 않는다.
- 01:28 master/HEAD **ae893b87348a9bd1cb0893763f6cad5047093a24**. 01:24 TLS 검증을 유지한 `git -c http.sslBackend=schannel ls-remote origin refs/heads/master`도 같은 SHA였다. 변경·미추적315파일/staged0/추적 migration 변경0, `git diff --check` exit0·지적0. 기존 사용자 파일은 보존했다. Node 시험/구문 검사·Gradle/test handle은 종료됐고 해당 Node QA 프로세스0·Java0을 확인했다. 별도 사용자 Node 프로세스는 종료하지 않았다.
- API24.21·문서 상단의 오래된 “첨부 전체 미구현” 안내·ledger/배치 UI/ATT046·048을 현행 로컬 계약으로 정정했다. 새 `announcement-attachment-backfill-ui-2026-09-12.md`가 화면 계약/실행 증거/남은 브라우저 검증을 설명한다. 커밋·푸시·배포·운영 DB·정책 게시/ENFORCE·기존 데이터 실행/원복은 이번 회차 미실행이다.
- **다음 필수 작업:** 과거 배치 승인 전체 이력 탐색, 정책 전체 QA/게시·관리자 UI, 모든 Provider/profile, 최신 실제 PG/Linux·실파일, exact-SHA 배포·정확한 영향 승인 후 전체 범위 실행·운영 역할별 브라우저 E2E. 화면이 연결됐다는 이유로 기존 데이터 처리를 완료하거나 어떤 Gate/ATT 요구도 축소하지 않는다.

### 2026-09-12 01:55 KST — 배치 승인 전체 이력과 영수증 탐색 검증

- 직전 회차는 Gate 개수만 답한 **진척 없는 상태 보고**였다. 이번 회차는 작업 중이던 승인 이력 구현을 현재 파일에서 재검토하고 회귀 수정·전체 검증·문서화를 수행한 진척이다. 전체 Gate0~8 최종 통과0, 출시 판정 **Not ready**를 유지한다.
- cwd/AGENTS/Git/스킬을 다시 확인했다. long-goal-operating-protocol·UI/UX 운영 원칙/필수 reference·frontend-ui-engineering으로 읽기와 고위험 후속 판단을 분리했고 release-readiness-gate로 실제 DB/운영 미검증을 명시했다. 사용자 수정/미추적 파일을 보존하며 서브에이전트·새 의존성·기술스택 변경은 없다.
- GET `/{batchId}/action-history`를 Controller→Service→ServiceImpl→DAO→Mapper XML/전용 DTO로 추가했다. 활성 ADMIN/OPERATOR/APPROVER·no-store·readOnly REPEATABLE READ·15초 timeout이다. V73의 불변 APPLICATION START/PAUSE/RESUME와 ROLLBACK START만 합치고 일반 감사 로그 전체라고 표시하지 않는다. 이번 migration 수정은 없다.
- 최초 현재 배치 버전을 조회 상한으로 고정하고 승인 전 버전이 그보다 작은 기록만 페이지 조회한다. 같은 상한에서 새 승인이 끼어들지 않으며 원문/job 삭제로 당시 범위·선택·원복 영향을 바꾸지 않는다. actor/사유/key/원문/URL은 목록에 없다. 페이지 총수/소속/범위/종류별 필드 불일치는409로 중단한다.
- 화면에 승인 전체 목록/현재 버전/고정 기준/페이지와 정확한 개별 영수증 연결을 추가했다. 당시 범위와 현재 성공·실패·삭제를 분리한다. URL에 비민감 기준·페이지를 복원하고 입력 사유·선택은 탭에서 보존한다. 목록 오류는 이전 행과 재조회 경로를 유지하며 미확정 변경 중에는 추가 조회도 잠근다.
- 재검토에서 다른 배치 이동 시 이전 영수증 DOM이 남을 수 있는 부분을 수정했다. 목적지 조회 성공/실패 모두 이전 이력/영수증을 해제하는 Node 회귀를 추가했다. SSR에는 이력 초기 disabled/제목/영수증 focus 목표/일반 감사 로그와의 구분을, Service에는 다른 배치 소속 거부를 추가 검증했다.
- 표적 `.\gradlew.bat :test --tests '*AnnouncementAttachmentBatchHistory*Test' --tests '*AnnouncementAttachmentMapperBindingTest' --tests '*AnnouncementAttachmentBatchViewControllerSmokeTest' --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **31초 성공**,4suite/63통과·실패/생략0(HistoryService13/HistoryHTTP12/Mapper30/SSR8).
- 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **BUILD SUCCESSFUL 2m48s**,16task 모두 실행. 01:52:38 root184suite/1658건=**1466통과·192조건부 생략·실패/오류0**. 추출기2suite/25통과·생략/실패0. 기존 MockBean removal11/unchecked/Log4j provider 경고는 남는다.
- 새 실제 PostgreSQL 사례3건은25개 승인 페이지 탐색 중2개 추가/원문 삭제 뒤 불변 범위/적용·원복 혼합과 완료 영수증을 대조한다. **job PG156·전체 분할 PG12·첨부 migration2 모두 생략**되어 SQL/trigger/동시성 성공 증거가 아니다. Windows 격리 우회/호스트 추출 fallback을 추가하지 않았다. 01:24 Docker 엔진 부재 기록을 이번 실행 성공으로 바꾸거나 반복 재시도하지 않았다.
- `node --test scripts/qa/attachment-backfill-ui.test.mjs scripts/qa/attachment-batch-ui.test.mjs scripts/qa/attachment-recovery-ui.test.mjs scripts/qa/attachment-review-ui.test.mjs scripts/qa/attachment-operations-ui.test.mjs scripts/qa/attachment-contract-report.test.mjs`: **94통과·실패/생략0**. 배치29건 중 이력7건이며 실제 mount/event/모의 API 검증이다. JS2개 구문 검사와 `git diff --check` exit0·지적0. 실제 브라우저 렌더링/키보드/스크린리더/확대/반응형/운영 E2E는 실행하지 않았다. 승인은 있지만 실제 DB·배포·역할 접속 선행 조건이 남는다.
- JAR SHA256 **fe40d614a49ec3996ee6cae92c696a7affbe743f7436006e67a7991499a70e04**. History Controller/ServiceImpl/Mapper·batch JS2개/template·V73의7개 항목 source 또는 class와 JAR 내용 일치, 제한 web parser library 패턴0. 새 History9파일+설계 문서의 제한 credential 패턴10파일 중0이며 전체 보안 감사는 아니다.
- 01:52 `gh api repos/FrostyCityMan/saneB --jq '{permissions: .permissions}'` 실제 조회: pull=true/push=false. 01:53 TLS 검증을 유지한 원격 master 조회는 로컬 master/HEAD **ae893b87348a9bd1cb0893763f6cad5047093a24**와 일치한다. 시작324→현재325개 변경·미추적 파일/staged0/추적 migration 변경0. 원격 읽기는 쓰기/배포 증거가 아니다. 개인 메모리는 수정하지 않았다.
- API24.22·승인 이력 상세 문서·배치 UI/ATT049·050/Linux156시험 대상·상단 Gate를 현재 코드/결과와 맞췄다. 커밋/푸시/Actions 실행/배포/운영 DB·정책 게시/ENFORCE/기존 데이터 적용·원복은 미실행이다. AWS/운영health/SHA/역할별 인증은 이번 회차 미조회다. Node QA 프로세스0·Java0을 확인했고 실행한 모든 Gradle/Node handle은 종료됐다. 사용자 별도 프로세스는 종료하지 않았다.
- **다음 필수 작업:** 정책 전체 QA 결과 판정·게시/교체·관리자 UI, 모든 Provider/profile, 실제 PostgreSQL/Linux·공개 실파일 및 상시 worker QA. 이후 exact-SHA 커밋/푸시/배포와 정해진 영향 범위 승인 후 기존 데이터 전체 처리·역할별 운영 브라우저 E2E까지 진행한다. 로컬 이력 구현을 전체 목표 완료로 선언하지 않는다.

### 2026-09-12 02:12 KST — 정책 게시 전 영향 관측 API

- 직전 회차는 배치 승인 이력 구현·회귀 수정·전체 검증의 **진척**이었다. 이번 회차도 AGENTS/현재 DB·API·정책 QA/worker·테스트를 확인한 뒤 게시 전 영향 관측을 구현한 진척이다. 장기 목표와 ATT001~062/Gate0~8은 유지하고 출시 판정은 **Not ready**다.
- long-goal-operating-protocol로 실제 선행 조건을 대조했다. 정책 QA는 현재 분류/합성 설치 runtime만 실행하고 전체 profile·worker DB 근거가 없어 INCOMPLETE로 끝나는 구조다. 이를 VERIFIED로 바꾸거나 임시 publication 성공 endpoint를 만들지 않았다. 대신 설계의 필수 게시 전 관측 API를 추가했다. 실제 최종 QA/게시 transaction·UI는 남는다.
- 새 GET `/api/v2/admin/announcement-attachment-policies/{policyId}/publication-impact`는 Controller→Service→ServiceImpl→DAO→Mapper XML·전용 DTO이며 활성 읽기3역할·no-store·readOnly REPEATABLE READ·15초 제한을 적용한다. V1/기존 v2 응답·V1~V75 migration은 수정하지 않았다.
- 현재 ACTIVE 규칙의 OFF 조건은 다른 규칙의 고정 작업에도 새 외부 요청을 막으므로 해당 규칙/전체 규칙을 따로 관측한다. 원문 binding/검수 필요/current pointer/운영 link, 고정 수집 job/RUNNING/적용 대기/원복 대기, 누적 FROZEN 계획을 별도 집계한다. PRODUCTION source/request만 포함하며 세 테이블 차원의 join 증식·전체 count LIMIT·QA 혼입을 피한다.
- 최신 QA의 단계 상태·증거 지문을 표시하고 누락·실패·오래된 DB 버전을 차단 사유로 반환한다. 4개 PASSED metadata도 코드/설치/전체 profile 현재성·게시 동의를 대신하지 않아 PUBLICATION_REVALIDATION_REQUIRED를 항상 남긴다. 임의 success/canPublish/게시 토큰은 없다. GET의 observedImpactHash는 집계 관측 지문이지 source ID/버전 전체의 scope/CAS가 아님을 명시했다. 같은 건수의 대상 교체를 반드시 감지한다고 주장하지 않는다.
- 표적 `.\gradlew.bat :test --tests '*AnnouncementAttachmentPolicyPublicationImpact*Test' --tests '*AnnouncementAttachmentMapperBindingTest' --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **29초 성공**,3suite/57건=Service16/HTTP10/Mapper31·실패/생략0. 역할/직접 service 방어, OFF 영향·집계/소속 검증, QA 누락·STALE·모든PASSED도 재검증, 지문 변화·POST405를 확인했다.
- 새 실제 PostgreSQL3건을 작성했다. 빈 초안 영향/audit·정책 불변, PRODUCTION/QA 원문·job 분리 및 원문/job 불변, 퇴역 정책 FROZEN 이력 보존/QA request 제외·ACTIVE 변경 후 지문 대조다. 테스트 harness에 실제 Mapper·Service bean을 연결했으나 환경 조건 때문에 실행하지 않았다.
- 최종 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`: **BUILD SUCCESSFUL 2m44s**,16task 실행. 02:09:19 root186suite/1688건=**1493통과·195조건부 생략·실패/오류0**, 추출기25통과·생략/실패0. job PG159/전체 분할 PG12/첨부 migration2는 모두 생략으로 실제 SQL/trigger/격리 성공이 아니다. 기존 MockBean removal11/unchecked/Log4j provider 경고가 남는다.
- 기존 Node QA6파일 **94통과·생략/실패0**, 프로세스 종료를 확인했다. 이번 회차는 backend 증분으로 UI/브라우저를 실행하지 않았다. 실제 운영 브라우저 승인은 있지만 실제 DB/배포/운영 역할 접속 검증을 대체할 환경은 확보하지 못했다. 운영 규모 count SQL 실행계획/15초 한도도 실제 PG에서 검증해야 한다.
- JAR SHA256 **4b79be81e2d87240c24da3d0b0358105a4bd912210aca397820220d4f2cf3fa1**. 새 Controller/ServiceImpl/Mapper3항목의 class/source와 JAR 내용 일치, 제한 web parser library 패턴0이다. 새8코드/테스트+상세문서9파일의 제한 credential 패턴0이며 전체 보안 감사가 아니다.
- API24.23/DB11.20·정책 QA/상세 영향 계약·ATT044/061·Linux159시험 대상·상단 Gate를 갱신했다. master/HEAD **ae893b87348a9bd1cb0893763f6cad5047093a24**, 시작325→현재334개 변경·미추적/staged0/추적 migration 변경0, diff 검사 지적0. 사용자 변경과 개인 메모리를 수정하지 않았다.
- 커밋·푸시·Actions 실행·운영 배포·DB/정책 게시/ENFORCE/기존 데이터 적용은 미실행이다. GitHub는01:52 push=false 조회 기록, AWS/운영 SHA/health는 이번 회차 미조회다. 최근 외부 실패를 재시도하거나 TLS/OS/격리 보안을 완화하지 않았다. 모든 실행 Gradle/Node handle 종료·해당 Node QA0/Java0을 확인했고 사용자 별도 프로세스는 보존했다.
- **다음 필수 작업:** 전체 profile·실파일/격리 worker DB QA의 실제 실행 증거 연결, 최종 검증과 정확한 승인 영향에 결합한 게시·교체 transaction, 관리자 정책 UI. Linux/PG·정확한 SHA 배포·승인한 전체 기존 데이터 처리/대조·역할별 운영 브라우저까지 원래 목표를 유지한다. 영향 관측 기능만으로 정책 관리나 전체 Gate를 완료 처리하지 않는다.

### 2026-09-12 02:48 KST — 정책 초안·QA 관리 화면과 로컬 실제 브라우저 검증

- 직전 회차는 Gate 개수를 확인한 **진척 없는 상태 보고**였다. 이번에는 현재 AGENTS·정책 CRUD/QA/영향 API·Thymeleaf·검증 장치를 다시 읽고 정책 관리 화면을 구현·검증했다. 전체 ATT001~062/Gate0~8과 운영 브라우저까지의 원래 목표는 유지한다. 최종 통과 Gate는 여전히0이며 **Not ready**다.
- 새 `/app/admin/announcement-attachment-policies`를 내부3역할·no-store로 연결하고 공고·수집 내비게이션에 추가했다. 초안 생성·수정·개정, 키워드 규칙 선택/페이지·퇴역 선택 해제, QA 예약·취소·실행/단계 이력, 게시 전 해당/전체 규칙 영향 관측을 기존 API에 연결했다. Controller의 화면 매핑 외 backend 비즈니스/DAO/Mapper/DB migration·설정은 변경하지 않았다.
- 명시적 사유·영향 확인·미선택 동의, 읽기 역할 쓰기 차단, 입력/키 탭 메모리 보존, 응답 유실 뒤401/403/409에도 동일 키/본문 재확인, 버전 기반 수정/취소의 현재 상태 별도 동의를 구현했다. 최초 요청 성공을 추정하지 않으며 받은 응답은 후속 조회 실패에도 유지한다. 이전 QA/영향 해제, 부분 조회 실패·필수 증거 없음/단계 실패 구분, 확인 닫기/저장 응답/복구 후 포커스를 검증했다.
- UI/UX 운영 원칙·frontend-ui-engineering으로 초안·QA·운영 게시를 분리했다. 정책 게시 실행은 아직 없음을 화면에 명시하며, ENFORCE 초안 저장을 활성화로 표현하지 않는다. long-goal-operating-protocol·release-readiness-gate에 따라 실제 검증 경계를 유지했다. 새 의존성/모션/차트·서브에이전트는 없다.
- 표적 Gradle(정책 화면/기존 수집 화면/정책·QA API) **33초 성공**. 명시적 SSR 내보내기 화면 테스트 **25초 성공/8건**. Node 정책 계약/실제 이벤트 대역 **22건**, 기존6파일 포함 **116건 전부 통과**, 실패/생략0이다.
- 최종 `SANEB_ATTACHMENT_POLICY_VIEW_EXPORT=true`를 해당 shell에만 지정하고 `.\gradlew.bat :test :attachment-extractor:test bootJar :attachment-extractor:installDist --rerun-tasks --no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'` 실행: **BUILD SUCCESSFUL 2m46s /16task 재실행**. root187suite/1696건=**1501통과·195생략·실패/오류0**, 추출기2suite/**25통과·생략/실패0**. XML 최신 시각02:45:23/02:45:26KST. 실제 PG159/전체 분할12/첨부 migration2는 모두 조건부 생략이며 성공으로 계산하지 않는다. 기존 MockBean removal11/unchecked/Log4j provider 경고가 남는다.
- browser-qa·playwright-visual-qa·playwright로 테스트가 생성한 비밀정보 제거 SSR과 loopback 합성 Node API를 실제 Chrome에서 검증했다. 초안 저장(키보드 Tab/Space/Enter), QA 예약→취소, 응답 유실503→같은 요청409→현재 상태 별도 확인, 읽기 역할 변경0, 영향503의 부분 실패 표시를 확인했다. 유실 시나리오 실제 합성 변경은1회였다. 최종 코드 재검증에서 확인 닫기→호출 버튼/저장→응답 포커스 모두true다.
- 320/375/768/1024/1440px에서 페이지 전체 가로 넘침0. 375px 편집 영역과 데스크톱 QA 화면 스크린샷을 직접 확인했다. 축소 모션 설정을 사용했고 정상 최종 페이지 console 오류/경고0. 오류 시나리오의 의도한503/409는 정상 결과로 숨기지 않았다. 실제 스크린리더·200/400% 브라우저 확대·운영 권한/CSRF/DB·실파일 worker 성공은 이 합성 검증으로 대체하지 않는다.
- QA 산출물: ignored `build/attachment-policy-ui-qa/index.html`, `policy-desktop.png`, `policy-mobile-editor.png`, `.playwright-cli` snapshot. 합성 서버는 실제 DB/인증/외부 공고와 연결하지 않았다. 각 명명 브라우저 세션과 소유 Node fixture를 종료했고 Gradle/Node 테스트 handle도 종료됐다. 기존 사용자 프로세스는 보존했다.
- JAR SHA256 **18e5d029d3e87d9ba3b5628d7f2ede74143f0f81be8013aa21117dfc51f6895e**. 화면 Controller class·layout·정책 template·JS2개의 실제 source/class와 JAR5항목 일치. 새7파일의 제한 credential 패턴0이며 전체 보안 감사는 아니다.
- 02:37 GitHub `pull=true/push=false`, Docker Linux 엔진 named pipe 부재를 새로 확인했다. 02:43 원격 master는 로컬 HEAD **ae893b87348a9bd1cb0893763f6cad5047093a24**와 같다. AWS/운영health/현재 배포 SHA/역할별 운영 인증은 이번 회차 미조회다. 시작334→현재341개 변경·미추적을 보존했으며 staging/커밋/푸시/Actions 실행·배포·운영 정책/데이터 변경은 하지 않았다.
- API24.24·정책 검증 문서·UI 상세 계약·ATT045/061·상단 Gate를 갱신했다. **다음 필수 작업은 전체 Provider/profile와 실제 실파일·격리 worker DB QA 증거 연결, 최종 QA 판정 및 정확한 승인 범위에 결합한 게시·교체 transaction/게시 UI다.** 실제 Linux/PG·정확한 SHA 배포·승인한 기존 데이터 전체 처리/대조·운영 역할 브라우저까지 계속하며, 로컬 정책 화면 추가로 목표를 축소하지 않는다.
