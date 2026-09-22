# 공고 첨부 수집 E2E 장기 구현 진행 기록

## 목표와 승인 범위

2026-09-09 사용자 실행 요청 기준. 본문·PDF/HWP/HWPX 첨부 상시 수집부터 DB/API/관리자 검수·DRAFT 전환·기존 데이터 처리·운영 브라우저 QA까지 완료한다. CLI 표본 성공을 전체 완료로 보지 않는다.

- 코드·additive migration·문서·테스트·커밋·푸시·기존 환경 배포·브라우저 QA는 이번 범위다.
- 정책 게시·ENFORCE·기존 데이터 적용은 구체적인 대상/건수/요청량/효과/복구 방법을 제시하고 승인 후 실행한다.
- KMS/IAM/보안그룹/공개 포트/DB 복원/신규 유료 서비스는 별도 승인 대상이다.
- 원본과 개인정보는 로그·감사 metadata에 남기지 않는다. 임시 원본은 정리한다.

## 기준선

### 2026-09-22 ATT-025/027 실제 Linux 자원 실패 검증 추가

- 이전 회차는 옥천 실행기와23a7139 Linux 계약 검증을 완료한 진척이다. 이후 승인 경계와 ATT62의 남은 항목을 대조했고, 운영/외부 HTTP 없이 검증 가능한 실제 추출기 자원 실패 경로를 보완했다. 서버 QA·취소된 배포 재개는 승인 대기를 유지하며 main/migration/규칙/운영 설정은 변경하지 않았다.
- 전용 runtime task에2시험을 추가했다. 실제 production launcher의128MiB 자식 heap에서256MiB 단일 할당 실패를 관측하고, 비정상 종료가 FAILED로 반환된 뒤 부모 시험 JVM의 다음 추출 성공·소유 원본 정리를 검증한다. 다른 시험은 격리 내부에서 시작한 고유 표식 자식 JVM을 호스트에서 관측한 후 실제30초 TIMEOUT·그 자식 종료·임시 작업 폴더 정리·다음 추출 재개를 요구한다. 실패 시에도 자기 표식 자식과 호출 스레드만 정리한다.
- 테스트용 무의존 JAR로 launcher의 OS 경계를 검증하는 것이며 실제 HWP record bomb/모든 악성 parser 입력·운영 JVM 생존 증거는 아니다. 기존12형식 fixture·망/파일/환경 canary도 보존했다. 보고서 검사기는 runtime 최소4건과 실패/생략0을 요구하도록 강화했다.
- 로컬 표적23건=22통과/Windows에서 Linux 전용1생략,23초 성공이다. 새 합성 JAR 컴파일과 기존 workflow 계약을 확인했으며 실제 Linux 신규2시험을 실행한 결과는 아니다. Node 보고서 판정10건은 실패/생략0이다. 새 SHA Linux 실행 결과를 별도 확인한다. 사용한 로컬 Java/Node 작업은 종료됐고 사용자 output은 보존했다.

### 2026-09-22 옥천 고정3건 임시 QA 실행기 로컬 준비

- 후속 [Linux35677829436](https://github.com/FrostyCityMan/saneB/actions/runs/35677829436), SHA `23a71394acac9c111d7b17b67e059e8e9d34456e`는 최종 success다. root2765=2498통과/267조건부 생략/실패·오류0, 신규 probe16건 통과다. 추출기88·패키징20·job192·migration17·runtime2·worker12·Flyway3·정책 부모2는 모두 실패/생략0이다. 독립PG221/221·실패/생략/미실행/container 실패0 및 독립/정책 환경 정리를 확인했다. root와 특수 suite의 중복을 고유 시험 수로 합산하지 않는다.
- Linux 업무 코드 지문 `d49cb22d272b66bd5d10078d33f6f698ddb89bef53b48faa72a60cafa6096780`은 기존1a19e63/f496d2e와 같으며 원문 없는 XML은 `build/qa-results/run-35677829436-contracts/`에 있다. 공식 사이트 opt-in·서울 서버 실행·운영 설치/정책 변경·브라우저 검증은 하지 않았다. 아래 새 SHA Linux 대기는 해소됐지만 실제 옥천 접속 및 전체 Gate 차단은 남는다.
- 직전 회차는 진행률 상태 보고로 새 구현 진척이 없었다. 이번에는 서버 실행 승인과 분리해 기존 태백 한정 임시 실행기에 옥천 고정3건 `OKCHEON` 모드만 추가했다. main 코드/리소스·migration·규칙·catalog 기대값 변경은 없다.
- manifest의 모드/고정 공고 목록을 일치시켜 태백 패키지 재사용·기관 확장·제목 중단 표본 누락을 거부한다. JUnit3건 모두 성공, 현재 실행 결과3건·본문·전체 파일·예산·원본 정리를 대조하며 부분 추출과 정책/기대값 승인은 구분한다. 태백 기존 동작과600초 probe·CPU1·768MiB·임시1GiB 제한은 유지한다.
- Java 표적52건 실패/생략0·1분25초/probe JAR 성공, Node29·Python13 실패/생략0, PowerShell 구문과 로컬 패키지138개 해시 검증을 완료했다. 번들 Python 사용 전 기본 별칭 실행은 실패했으며 성공으로 계산하지 않는다. 전체 로컬 회귀·새 SHA Linux·서버 실제 격리는 이번 결과로 대체하지 않는다.
- [옥천 실행 준비](announcement-okcheon-bbs-profile-2026-09-15.md)에 패키지/상한/미실행 경계를 기록했다. 로컬 plan849a8592…는 uploaded=false/commandId 없음이며 서버·공식 사이트·DB·정책·worker·기존 데이터·브라우저 변경은 없다. 로컬 Java/Node 작업 종료·사용자 output 보존을 확인했다. 전체 ATT62/Gate8진행·1차단을 유지하고 취소된 재배포 재개 및 옥천 서버 추가 QA 승인은 계속 대기한다.

### 2026-09-22 현재 AWS 인증 사용 가능·운영 기준선 재확인

- 로그인 명령 timeout과 현재 인증 상태를 분리해 다시 조회했다. STS/default root·서울·저장소 계정 일치와 대상1대/SSM Online이 성공했으므로 현재 인증 차단은 해소됐다. 로그인 CLI의 실패 기록을 성공으로 바꾸지 않으며 새 로그인 창을 반복 실행하지 않았다.
- Runtime SSM521e3255-17e7-41ef-a9c6-5c339bbdb57b/DB SSMc7d7d18b-dcf1-4b4e-b1fb-e20aa0c885f7 모두Success다. 실제 운영787c594/JARd8696e85…/추출기1.0.1/V83·실패0/healthUP, 첨부 정책·file·job 등 전부0/worker·정책QA·ProviderQA OFF, 기존 BODY·수집true, 지자체223/목록parser41/원문2945·외부key2종 없음이다. DB READ ONLY/ROLLBACK/쓰기0·소유 임시 파일 정리를 확인했다. [운영 상세](../deployment/attachment-runtime-baseline-2026-09-15.md)를 따른다.
- 코드 재배포 재개 승인은 여전히 별도 대기다. 옥천3공고의 서버 임시 QA(132요청/240MiB/20분/CPU1·768MiB·임시1GiB)는 기존 태백 승인과 다른 범위이므로 추가 승인을 요청했다. 승인 전 제출·설치·운영 데이터 변경을 하지 않는다. 전체 Gate8진행/1차단·ATT62와 사용자output을 유지한다.

### 2026-09-22 옥천 접속 실패 확정·격리 경계 시험 대기

- `0bfa752` [Linux35630969706](https://github.com/FrostyCityMan/saneB/actions/runs/35630969706)은 옥천 실제 관측에서 failure다. 고정3건 중 제목 미충족1건 요청0 통과, 양성2건 BODY 2시도 TIMEOUT/상세 TRANSPORT_TIMEOUT이며 파일 발견·다운로드·추출 미도달이다. 원본 정리true·운영쓰기0, 예약 합계6요청/4,194,304bytes다. 로컬 단일GET도403이므로 접속 원인 미확정을 유지한다. [근거와 다음 경계](announcement-okcheon-bbs-profile-2026-09-15.md)를 따른다.
- 같은 실행의 코드 계약은root2488통과/266생략/실패0, 특수suite·독립PG221/221·정책 부모2 및 정리 성공이다. 전체 job 실패를 코드 성공으로 덮어쓰지 않는다. 기존 태백 관측 성공도 옥천 접속·추출 성공을 대신하지 않는다.
- `f496d2e`는 ATT-059의 실제 launcher 환경/호스트 파일/loopback 경계 합성 시험을 추가했다. 로컬22건=21통과/Windows의 Linux1생략·23초이며 실제 Linux [35631537040](https://github.com/FrostyCityMan/saneB/actions/runs/35631537040)은 최종 success다. runtime XML2건은 신규canary1.391초와 기존12fixture5.024초 각각 통과/실패·생략0이다. root2756=2489통과/267조건부 생략/실패·오류0, 독립PG221/221·정책 부모2 및 정리 성공을 확인했다. production main/migration/catalog 기대값은 변경하지 않았고 전체 ATT62·9Gate를 유지한다.
- Linux 업무 코드 지문은 이전1a19e63과 동일한 `d49cb22d272b66bd5d10078d33f6f698ddb89bef53b48faa72a60cafa6096780`이다. 추출기88·패키징20·job192·migration17·worker12·Flyway3도 실패/생략0이며 중복 시험 수를 합산하지 않는다. 결과 XML은 `build/qa-results/run-35631537040-contracts/`에 있다. 운영 설치·전체 악성 파일/자원 고갈·전체 Provider의 완료 증거로 확대하지 않는다.
- 사용자 요청의 AWS 로그인 세션은 `AWS_LOGIN_TIMED_OUT`으로 종료됐고 임시 CA를 정리했다. 인증 갱신 성공·새 운영 조회로 기록하지 않으며 재로그인은 사용자 준비 후 재개한다. 운영 재배포 재개 확인도 별도 미확인이다. 운영 설치/정책/worker/기존 데이터 변경·브라우저 업무 E2E는 이번에 실행하지 않았다. 사용자 `output/` 보존.

### 2026-09-22 FIXED 모드 후속 SHA Linux 검증 확정

- [Linux35629360526](https://github.com/FrostyCityMan/saneB/actions/runs/35629360526), SHA `1a19e63a8c5ec8fe8682afd18b432cc6dc6accb2`는 같은 실행을 계속 확인한 결과 최종 success다. XML root2754=2488통과/266조건부 생략/실패·오류0이다. 추출기88·패키징20·job192·migration17·runtime1·worker12·Flyway3·정책 부모2는 실패·오류·생략0이며 중복 suite를 고유 시험 수로 합산하지 않는다.
- 독립 PostgreSQL `SYNTHETIC_WORKER_DB_CONTRACTS_V2` 221/221·실패/생략/미실행/container 실패0, 독립 실행과 정책 부모 정리 성공을 로그에서 확인했다. Linux 업무 코드 지문은 `d49cb22d272b66bd5d10078d33f6f698ddb89bef53b48faa72a60cafa6096780`으로 선행71a05ae Linux와 같다. 결과 XML은 `build/qa-results/run-35629360526-contracts/`에 회수했다.
- 이 CI에서는 공식 사이트 opt-in을 실행하지 않았다. 실제 태백 관측/고정 비교는 아래 서울 SSM의 별도 근거이며, 새 CI 성공으로 전체 Provider/정상 표본/형식 검증을 대체하지 않는다. 현재 Gate6의 코드 회귀 대기는 해소됐으나 전체 실파일 QA 차단은 남는다.
- AWS 갱신은 사용자 요청으로 같은 로그인 세션에서 대기 중이다. 인증 완료·새 Runtime/DB 조회를 성공으로 기록하지 않는다. 취소된 재배포의 재개 확인도 별도 대기이며 운영 설치·정책/worker 활성화·기존 데이터 처리·운영 업무 브라우저 검증은 이번 회차에 수행하지 않았다. 사용자 `output/`은 보존했다.

### 2026-09-22 태백 새 기대값 production 재비교 성공

- 직전 회차71a05ae는 관측/기대값 검토/전체 로컬/커밋·푸시의 progress다. 같은 Linux35627379350을 계속 확인했으며 별도 재시작하지 않았다. 이 회차에서는 새 기대값을 production 단건 실행기로 실제 재검증했다.
- 앞선 서울 관측5요청/2,377,939bytes의 SSM 영수증과 정리를 대조하고, FIXED 모드를 잔여39요청/81,508,141bytes로 제한했다. JUnit 시험의 상한은 축소만 허용하고 관측/비교·기관을 명시적으로 분리한다. Java30·Node4·Python10건 실패/생략0이며 production 코드/API/schema/운영 설정 변경은 없다.
- SSM66eb8090-64c9-4947-b343-0e103bf98894 Success, 별도 JUnit1/1·실패/생략0,25.419초. 전체HWPX2파일의 사전 binary/text/blocks/문구/역할 지문이 일치했다. FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED/UNKNOWN+FORM·정상0·coverage false·정책 QA false를 유지한다.
- 비교3요청/280,787bytes, 관측+비교 합계8요청/2,658,726bytes. 원본·임시 파일·프로세스·자기 S3 객체 정리와 운영 JAR불변/health UP/운영 DB쓰기0을 확인했다. [고정 비교 근거](announcement-taebaek-fixed-qa-expectation-2026-09-15.md)를 따른다. 전체 정상 후보/기관/파일 형식, 동일 SHA 배포·승인된 활성화/기존 데이터·운영 브라우저 E2E는 남는다.
- [Linux35627379350](https://github.com/FrostyCityMan/saneB/actions/runs/35627379350), SHA71a05ae는 최종 success다. artifact XML의 root2751=2485통과/266조건부 생략/실패·오류0, 추출기88·패키징20·job192·migration17·runtime1·worker12·Flyway3·정책 부모2는 실패·생략0이다. 각 suite는 중복 실행을 포함하므로 고유 시험 수로 합산하지 않는다.
- 독립 PostgreSQL 산출물221/221·실패/생략/미실행/container 실패0, 원본/소유 환경 정리 성공 및 부모 연결·취소 정리 성공을 로그와 XML로 확인했다. Linux application code hash는 `d49cb22d272b66bd5d10078d33f6f698ddb89bef53b48faa72a60cafa6096780`이다. 로컬 패키지의6ff295d8…와 바이너리/리소스 지문이 같다고 주장하지 않는다. 원격 XML은 `build/qa-results/run-35627379350-contracts/`에 있다.
- 후속 FIXED 모드 변경은 시험 코드/launcher뿐이며71a05ae 대비 production 코드/리소스 변경0이다. 결과의 scope/caseId 교차 차단 추가 후 최종 Java30건도18초/실패·생략0으로 재검증했다. 이 회차 전체 로컬 회귀 재실행은 하지 않았으며 새 커밋의 Linux 결과와 위71a05ae를 구분한다.
- 취소된 운영 재배포의 재개 승인은 이번 서울 임시 QA에 포함되지 않았다. 코드 설치 뒤 기존 활성 본문 정기 수집에 BBS 보안/충주 모델이 적용될 수 있으므로 정확한 배포 SHA와 영향을 제시한 후 재개 확인이 필요하다. 첨부 worker/게시/ENFORCE/기존 데이터는 별도 범위다.

### 2026-09-22 서울 실제 3단계 관측 성공·태백 기대값 재검토

- 임시 실행기의 AWS 경로 및 CLI1/v2 옵션 호환성 결함을 수정했다. 서버 설치/권한 변경 없이 진행했고, PACKAGE_DOWNLOAD 단계에서 멈춘 재시도는 sourceWorkStarted=false였다. Python9건·Node24통과/Windows 조건부3생략·Java41건 통과다.
- 최종 SSM10df4968-07d1-49ce-b2c3-d768003c9f9c Success/관측 JUnit1/1, 실제 처리27.983초. TITLE 조합 통과→BODY431자/AVAILABLE→HWPX2개 COMPLETE_TEXT/2041·1994자→전체 텍스트 분석true다. 혼합 UNKNOWN+FORM, REVIEW_REQUIRED/ATTACHMENT_CONTEXT_REVIEW·관리자 검증은 유지한다.
- 5요청 예약/2,377,939bytes(본문 상한 포함), CPU1/메모리768MiB/임시1GiB 실측, 운영 DB쓰기0·원본/임시 파일/프로세스 정리true·운영 JAR불변/healthUp=true다. 이번 두 재시도의 임시 S3 객체도 삭제/부재 확인했다. 전체3실행 cleaned=true이며 사용자 output은 보존했다.
- [사전 기대값 재검토](announcement-taebaek-fixed-qa-expectation-2026-09-15.md): 전체2파일의 모든 내용·역할 지문 일치와 BBS redirect3행 변경을 확인하고 metadata3필드만 갱신했다. 이전 profile627d3f… 거부 회귀를 추가하고 정상0/참조30/전체 coverage false를 유지한다. 관측이 새 catalog의 production 고정 비교 성공은 아니다.
- 현재는 Gate6 실제 파일 QA 차단의 일부를 해소한 진척이다. 전체 Gate8진행/1차단은 나머지 Provider·형식·정상 후보 검증 때문에 유지한다. 새 catalog 전체 회귀/CI/고정 재비교·동일 SHA 배포·승인된 정책/기존 데이터·업무 브라우저 E2E는 남는다. [실행 상세](announcement-seoul-temporary-bbs-qa-2026-09-21.md)를 따른다.
- 최종 로컬 전체 회귀는4분24초 성공이다. root2751=2486통과/265조건부 생략/실패·오류0이며 기존 profile 불일치 실패는 해소됐다. 패키징20·추출기88은 각각 앞선 유효 실행 결과를 재사용했다. 표적159/패키징20 실행에서는 새 bootJar를 생성했으며 최종 SHA256은 `15d5e97dd7e440cebae1853d155f030b92ba56ff0f51611233837454edc485cf`다. 운영 설치 JAR와 혼동하지 않는다. 로컬 Java/Node 작업은 종료됐다.

### 2026-09-22 AWS 인증 갱신·서울 임시 QA 실패 회수·전송 패키지 정리

- 사용자 승인으로 default/root 서울 로그인을 갱신하고 저장소 대상 계정 일치를 확인했다. SSL 검증은 유지했다. 같은 SSM35926df3-e555-4c7a-bf95-444a3568d9c9만 조회했으며 재실행하지 않았다.
- 기존 실행은 Failed/responseCode1, 격리 실행기 INCOMPLETE/FileNotFoundError/RUNNER_FAILED다. 실패 경로·단계와 본문/첨부 관측 보고서는 없어 정확한 누락 대상과 실제 요청량은 미확인이다. 제목→본문→첨부 성공으로 계산하지 않는다.
- 해당 실행의 사후 결과에 unitInactive/installedJarUnchanged/healthUp=true가 확인됐다. 과거 실행 종료 시점의 증거이며 현재 health 새 조회와 구분한다. 파일별 정리 성공 metadata는 반환되지 않았다.
- terminal 상태와 소유 metadata/크기를 확인한 임시 S3 객체만 삭제하고 부재/plan.cleaned=true를 확인했다. 운영 객체 삭제0, 로컬 패키지·결과·사용자 output은 보존했다. 임시 CA와 로컬 단발 Node/로그인/조회 프로세스는 정리됐다.
- [서울 QA 기록](announcement-seoul-temporary-bbs-qa-2026-09-21.md)에 결과를 반영했다. 인증/전송 패키지 정리 차단은 해소됐으나 실행기 실패 진단·실제 최신 파일 QA가 남는다. 운영 배포·DB·정책·worker·기존 데이터 변경과 업무 브라우저 검증은 없으며 전체 Gate8진행/1차단을 유지한다.

### 2026-09-21 서울 임시 QA 승인·제출 후 인증 만료

- 사용자가 서울 임시 격리 QA를 승인하여 태백1공고/44요청·80MiB/20분/CPU1·임시1GiB 범위로 재개했다. 이전 승인 대기 상태는 해소됐다. 운영 서비스·DB·정책·기존 데이터 변경 승인은 아니다.
- 현재 운영 baseline과 서버 자원을 확인하고 기존 관측 JUnit용 별도 probe/제한 launcher를 준비했다. Java41건·새 Node4건 통과, 기존 launcher 포함 Node27건은24통과/Windows 조건부3생략이며 후속 workflow18건도 통과했다.
- 자기 임시 패키지188,072,770bytes를 비공개 S3에 전송하고 SSM35926df3-e555-4c7a-bf95-444a3568d9c9로 한 번 제출했다. 첫 결과 조회에서 AWS 인증이 만료돼 실행 결과/종료/사후 운영 불변·S3 삭제는 미확인이다. 같은 실행을 재시작하지 않는다.
- [실행·인증 후 재개 기록](announcement-seoul-temporary-bbs-qa-2026-09-21.md)의 plan/command ID로 조회와 정리를 이어간다. 승인 범위를 다시 요청하지 않고 인증 갱신만 필요하다. 제출 성공을 QA 성공으로 계산하지 않는다. 전체 Gate8진행/1차단·ATT62와 사용자 output을 유지한다.

### 2026-09-21 최신 수정본 Linux 결과 확정 — 1d06b4e

- [Linux35608794560](https://github.com/FrostyCityMan/saneB/actions/runs/35608794560)은7분45초 후 terminal failure다. 직전 회차의 CI 시작 이후 같은 실행을 확인했으며 중복 재실행하지 않았다. QA 브랜치1d06b4e는 원격과 일치하고 사용자 output은 보존했다.
- metadata/XML artifact를 `build/qa-results/run-35608794560-contracts/`에 회수했다. root2745=2478통과/266조건부 생략/1실패/오류0이며 실패는 `AttachmentProviderQaCatalogTest.packagedRevalidatedExpectationKeepsWholeSetAndDoesNotCompleteCoverage` 한 건뿐이다. 실제 profile 지문8cf428f1…과 구 기대값627d3f60…의 불일치로, 추가 코드 실패는 관측하지 않았다. 이 결과를 전체 통과로 표시하지 않는다.
- 별도 suite는 추출기88·QA패키지20·작업DB192·migration17·runtime1·worker12·Flyway3 모두 실패/오류/생략0이다. root와 별도 suite는 중복 실행을 포함할 수 있으므로 고유 시험 수로 합산하지 않는다. 로그에서 bootJar/추출기 installDist/QA 설치 task 실행도 확인했다.
- 독립 산출물 PostgreSQL 실행과 정책 부모 연결·취소·정리는 계약 실패 때문에 SKIPPED다. 필수 보고서 검사도 실패를 유지했고 artifact 보관은 성공했다. 공식 사이트 관측은 요청하지 않아 실행되지 않았다. 태백 기대값/관측 시각/정책을 변경하지 않는다.
- 서울 서버 임시 격리 QA 승인 대기가 다음 실제 파일 검증의 차단이다. 기존 서버 도구는 운영787c594 설치물을 사용하므로 최신 코드 QA로 재사용하지 않는다. 로컬 패키지 후보136파일/187,978,430bytes와 probe73,350bytes를 확인했지만 업로드/설치/실행은 하지 않았다. 전체9Gate는8진행/1차단, 정상 후보 coverage0이며 goal ACTIVE다.
- 이번은 terminal CI와 Linux DB·추출기 근거를 확정한 progress다. 새 코드·migration·운영 설정·데이터·브라우저 변경 없이 이 증거만 기록하며, 문서만의 후속 커밋에서는 중복 CI·배포를 시작하지 않는다. gh 관측 프로세스는 종료됐다.

### 2026-09-21 태백 관측 실패 확정·옥천 검사기 및 CI 실행 분리

- 직전 회차는 Git/CI/작업 기록의 현황 보고로 새 구현 진척이 없었다. 이번에는 미커밋 옥천 검사기와 workflow 보완을 검토·검증하고 기록하는 단계다. 전체 ATT001~062, Gate0~8 및 제목→정제 본문→실제 첨부 텍스트→최종 관리자 검증 목표를 유지한다.
- bbc8639의 [Linux35606668928](https://github.com/FrostyCityMan/saneB/actions/runs/35606668928)은 terminal failure다. 태백 관측 시각은2026-09-21T13:38:06.774952649Z이며 제목 조합 통과 후 BODY 2회 TIMEOUT/0자, DETAIL_DISCOVERY의 TRANSPORT_TIMEOUT으로 종료했다. 파일 목록은 비어 있고 첨부 다운로드·추출은 실행되지 않았다. JUnit1건/실패1/생략0, 운영 쓰기0·원본 정리true다. 리다이렉트 보안 차단으로 오인하지 않는다.
- 해당 실행의 코드·DB 계약은 앞선 외부 관측 실패 때문에 SKIPPED였다. 이후 workflow는 취소되지 않은 경우 계약 검증을 실행하되 외부 관측 실패와 전체 job 실패를 보존한다. 계약이 성공해야 독립 산출물/정책 부모 검증으로 진행한다. 테스트 제외, continue-on-error, 기대값 지문 교체는 하지 않는다.
- 옥천은 고정3공고(제목 통과2/중단1)의 현재 실행 metadata·JUnit·전체 파일·품질/역할·예산·원본 정리를 검사한다. 명시적 커밋 표식과 계약 성공을 모두 요구하며 일반 push는 공식 공고를 요청하지 않는다. 옥천·제천·충주 Node 검사기74건 통과/실패·생략0을 재확인했다. 실제 옥천 Linux 추출 성공이나 정책 승인 근거는 아직 없다.
- workflow 계약18건도 `:test --tests '*AttachmentContractWorkflowTest' --rerun-tasks --no-daemon --console=plain --max-workers=1`로 새 실행해46초 성공/실패·생략0이다. 앞선12초 UP-TO-DATE 실행과 구분한다. 전체 회귀·bootJar는 이번 증분에서 재실행하지 않았으며 원격 CI에서 확인한다. 기존 구 태백 지문 불일치1건은 아직 미해결이다.
- 로컬 Docker는 한 차례 기동했으나 소켓 관련 오류로 엔진 연결에 실패했다. 초기화·파일/볼륨 삭제 없이 이번 시작분을 종료했다. 서울 서버 임시 격리 QA는 별도 범위 승인 대기이며 실행/업로드하지 않았다. 기존 운영787c594/worker 비활성은 이전09-21 조회 근거이고 이번 운영 재조회·배포·정책 변경·브라우저 조작은 없다.
- catalog30/보관 기대값1/현행 실행 가능0/정상0과 Gate8진행·1차단을 유지한다. 다음은 실제 최신 코드 관측→내용/역할 대조→기대값 검토→전체 CI→동일 SHA 배포→승인된 활성화/기존 데이터 처리→운영 업무 E2E다. 승인 전 서버 QA로 우회하지 않는다.

### 2026-09-21 최신 Linux 성공 확정·옥천 후속 관측 준비

- 직전 현황 회차는 새 Linux terminal 결과와 산출물 회수로 다음 행동을 바꾼 **progress**다. `7a5ef22`의 [Linux35602665310](https://github.com/FrostyCityMan/saneB/actions/runs/35602665310)은22:13 KST success이며 root2731=2465통과/266조건부 생략/실패·오류0이다. 별도 추출기88·패키징20·job192·migration17·runtime1·worker12·정책 부모2·Flyway3은 모두 실패·생략0, 독립 PostgreSQL/부모 연결·취소·정리도 통과했다. 각 suite를 분리 집계하며 동일 시험의 중복 실행을 단일 고유 시험 수로 합산하지 않는다.
- 같은 실행의 태백 고정 비교 JUnit1/1·전체HWPX2파일 PASS/COMPLETE_TEXT,3예약/280,787bytes·원본 정리true·운영 쓰기0이다. 결과는 `FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED`이며 정상 공고false·전체 coverage false·정책 QA false다. 이번 고정 비교는 새 BODY/worker/API 관측을 실행한 것이 아니며, 관측 기한09-23T05:06:32Z도 연장하지 않는다. 산출물은 Git 제외 `build/qa-results/run-35602665310-contracts/`와 `run-35602665310-fixed/`에 있다.
- 태백 혼합 문서는 확정 설계의 NOTICE/GUIDE/FORM 혼재 예외이므로 기대값이나 역할 판정 기준을 완화하지 않는다. 등록된 옥천3표본의 공식 제목을 현재 HTTP200·단일 제목 표식으로 확인했고, 기존 공통 관측기의 명시적 `OKCHEON` 그룹에 연결한다. 제목 중단도 전체 표본 분모에 남기며 본문/첨부를 강제로 수집하지 않는다. [옥천 검증 경계](announcement-okcheon-bbs-profile-2026-09-15.md)를 따른다.
- 전체 ATT62/9Gate·엔진7/첨부19/본문18/형식3·catalog30/기대값1/정상0을 유지한다. 운영은 마지막09-21 SSM의787c594/추출기1.0.1/V83/worker OFF이며 최신 코드 재배포·정책/기존 데이터 승인·전체 수집처 QA·브라우저 업무 E2E는 남는다. 이번 후속은 운영 쓰기·재배포·브라우저 조작이 없고 사용자 output은 보존한다.
- 후속 옥천 표적에서 BBS의 다른 공고/첨부로 향하는 리다이렉트가 허용되는 결함을 재현했다. [최초 요청 식별자 고정](announcement-bbs-redirect-identity-2026-09-21.md)을 보완하며 DB/API/migration/역할/키워드는 유지한다. 이 변경은 새 프로필 지문을 만들기 때문에 위7a5ef22 성공과 구분하고, 보관 기대값1건의 현행 실행 가능 여부는 재관측 전0으로 둔다. 기존7a5ef22 재배포 요청보다 수정본 검증이 먼저이며 운영 배포를 임의 재개하지 않는다.
- 수정 후 표적179건/실패·생략0·1분15초 성공, 전체 회귀4분56초 XML2744=2478통과/265생략/구 태백 지문1실패다. 패키징20·bootJar/QA/probe 생성 성공, 추출기88은 기존 결과 재사용이다. 현재 코드는 새 태백 관측→기존 내용/역할 대조→별도 기대값 검토 순서로 검증하며 전체 Gate 완료로 보지 않는다. 사용한 Gradle/임시 PG/단발 Node는 종료됐다.

### 2026-09-21 중단 회귀 결과 복구·고정 비교 재개

- 직전 회차는 DNS 수정본9d5eaae 푸시·실제 태백 관측·기대값 검토/갱신까지 진행한 **progress**다. 재개 시 HEAD/origin9d5eaae와 staged6파일·사용자 output을 확인했다. 옛 실행48266은 핸들이 없고 Java/PG도 없으므로 같은 실행이 계속 살아 있다고 가정하지 않았다.
- 09-16 14:23의 최종 root XML2731=2466통과/265조건부 생략/실패·오류0을 회수했다. 패키징20(09-16)·추출기88(09-15)의 보관 결과도 실패/생략0이다. 현재 동일 Gradle 명령은31초 성공/23task 모두 UP-TO-DATE로 종료했다. 이 결과는 변경 없는 산출물 재사용이며09-21에 모든 시험을 새로 실행한 것이 아니다. 웹 JAR SHA256843b7a4b…는 이전 생성본과 일치한다.
- catalog 변경은 검토된 version/profileHash/observedAt3필드뿐이며 전체30참조·2파일 내용 기대값은 그대로다. 관측 유효기한09-23T05:06:32Z를 연장하지 않는다. 다음은 새 커밋에서 전체 Linux 계약과 태백 고정 기대값1건을 실제 실행하는 것이다. 전체 ATT62/9Gate·정상 공고0·정책 QA false를 유지하며 운영·브라우저는 이번에 조회/변경하지 않았다.
- 후속 회차는 **progress**다. 활성 GitHub 계정의 push=false를 확인한 뒤 보안 저장소의 기존 작업 계정 인증을 명령 범위에만 적용해 push=true를 검증했다. 전역 계정/credential 설정을 바꾸지 않고 `7a5ef22` 푸시·원격 일치, [Linux35602665310](https://github.com/FrostyCityMan/saneB/actions/runs/35602665310) 실행을 확인했다. 과거 채팅 토큰은 재사용하지 않았다.
- AWS도 현재 신뢰된 공개 CA로 TLS 검증을 유지하면서 서울/root·저장소 계정·인스턴스1대 일치를 확인했다. Runtime SSM954cd94c…/DB SSM42b133fe… Success, 운영787c594/추출기1.0.1/V83/health UP·worker OFF·첨부 count0·원문2945를 현재 재확인했다. DB READ ONLY/ROLLBACK/쓰기0이며 [현재 운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)을 따른다. 코드만 재배포하는 정확한 범위 승인을 요청했고 정책/기존 데이터/브라우저는 미실행이다.

### 2026-09-16 DNS 수정본 후속 검증·고정 공고 재관측 준비

- 직전 회차는 사용자 현황 요청에 따른 읽기 전용 재확인이며 구현 진척은 없었다. 이번은 DNS 실패 분리와 고정 태백 재관측 경로를 검토하여 QA 브랜치에 반영하는 단계다. 전체 ATT62/9Gate·제목→본문→첨부→최종 관리자 검증을 유지한다.
- 보관 XML을 다시 집계한 후속 표적99건·패키징20건은 실패/생략0이다. 해당 실행은1분35초 성공이며 웹 JAR/probe/추출기 설치는 동일 소스의 앞선 산출물을 재사용했다. 전체 회귀는 여전히2729건 중2463통과/265생략/태백 지문 불일치1실패다. catalog 원본과 실패 assertion을 완화하지 않았다.
- 새 코드의 태백184816/HWPX2파일 재관측은 고정1공고·최대44요청/80MiB의 명시적 CI 단계로만 실행한다. 실제 전체 파일·텍스트·역할/위치 지문을 보관 기대값과 대조하기 전 기대값 승인이나 정책 QA 성공으로 처리하지 않는다. 운영 배포·설정·정책·기존 데이터·브라우저 변경은 없다. 사용자 output은 보존한다.
- `9d5eaae` 커밋/QA 브랜치 푸시와 원격 일치를 확인했다. [Linux35058110396](https://github.com/FrostyCityMan/saneB/actions/runs/35058110396)의14:06 KST 실제 태백 관측은 JUnit1건/실패·생략0, BODY431자·전체HWPX2파일 완전 추출이다.5요청 예약/2,377,939bytes·원본 정리true·운영 쓰기0이며 UNKNOWN+FORM/관리자 검수는 유지된다. 전체 CI terminal 결과와 구분한다.
- 기존 전체 파일·텍스트·블록·역할/위치 지문이 일치하므로 catalog metadata3필드만 검토 후 갱신했다. 자동 기대값 생성/내용 변경/시험 완화는 없고 구지문2종의 실행 차단 회귀를 유지한다. 후속 표적133건·패키지20건 실패/생략0,2분48초 성공이다. catalog30/실행 가능1/정상0·정책 QA false, 엔진7/첨부19/본문18/형식3은 유지한다. 전체 회귀 및 새 catalog 실제 고정 비교를 진행한다.
- 관측 실행35058110396의 전체 CI는 구catalog와 새 profile 지문 불일치1건으로 failure 종료했다. 보관 XML root2730=2463통과/266생략/실패1, 별도 추출기88·패키징20·job192·migration17·runtime1·worker12·Flyway3 실패/생략0이다. 독립 산출물/정책 부모 단계는 생략, 필수 보고서 판정도 failure다. 같은 실패 실행을 재시작하지 않고 검토된 새 catalog의 전체 회귀·별도 고정 비교로 진행한다.

### 2026-09-16 DNS 진단 코드의 Linux 계약 통과·조회 실패 분류 수정

- 직전 회차는 DNS 비교기 구현/실행·커밋/푸시 및 보은 재검증 시작의 **progress**다. 이번에는 같은 실행35055559668의 terminal success와 실제 XML을 확인하고, DNS 조회 실패가 보안 차단으로 합쳐지는 별도 결함을 회귀로 재현·수정한 **progress**다. 전체 ATT62/9Gate·제목→본문→첨부→관리자 검증을 유지한다.
- `8589016` [Linux35055559668](https://github.com/FrostyCityMan/saneB/actions/runs/35055559668)는13:37 KST success다. root2722=2456통과/266조건부 생략/실패0, 별도 추출기88·패키징20·job192·migration17·runtime1·worker12·정책 부모2·Flyway3 모두 실패/생략0이다. 독립 namespace/PG·부모 연결/취소/정리도 success이며 공식 파일 검증은 이 실행에서 생략했다. XML은 Git 제외 `build/qa-results/run-35055559668-contracts/`에 있다.
- 같은 수정 전 SHA의 보은35055669009는 선행 실행 종료 후 실제 시작했다. 중복 요청/재시작은 하지 않았고 terminal 결과는 후속 확인한다. DNS 해석 성공을 파일 수집 성공으로 바꾸지 않는다.
- 첨부 전송기가 DNS 조회 불능을 URL 차단으로 바꾸어 기존 재시도에서 제외하는 문제를 합성3건 실패로 확인했다. 알려진 DNS 조회 실패만 고정 코드로 분리해 기존 NETWORK_UNAVAILABLE/최대3회 경로를 사용하도록 수정했다. 사설 주소/미승인 URL/예상 밖 오류는 차단하며 본문 DNS 계약·DB/API/migration/키워드/운영은 변경하지 않는다. [수정·검증 범위](announcement-attachment-dns-failure-retry-2026-09-16.md)를 따른다. 수정 후 표적54건/생략0·43초 성공, 전체 회귀/패키징은 진행 중이다.
- 초기09-08 구현 기록에는 현재 진행/ATT/계약 근거 안내만 추가하고 당시 실패/미완료 이력을 보존했다. 엔진7/첨부19/본문18/형식3·catalog30/기대값1/정상0·Gate8진행/1차단·Not ready를 유지한다. 운영 인증·취소 배포 재개·정확한 게시/기존 데이터 승인·브라우저 업무 E2E와 전체 Provider 근거는 여전히 남는다.
- 후속 보은35055669009는13:49 KST 전체 failure로 완료됐다. 계약 root2722=2456통과/266생략/실패0과 별개로 실제3건은 BODY TIMEOUT/2회·NETWORK_TIMEOUT/RETRY_WAIT·발견/처리 수 미확정·파일[]·추출 미실행이다. 원본/lease 정리·운영 쓰기0과9예약/6,291,456bytes를 확인했다. DNS 해석과 실제 본문 접근 성공은 다르며 같은 원격 관측을 다시 실행하지 않는다.
- DNS 분류 수정본의 첫 전체 회귀는6분12초 실패: XML2729=2463통과/265조건부 생략/실패1이다. 공유 전송 코드 변경으로 태백 기대값의 프로필 지문이 불일치한 것이며 원본 catalog·assertion은 유지했다. 보관 기대값1/현행 실행 가능0·정상0을 명시한다. 고정 태백1건 관측을 먼저 수집한 뒤 기존 기대값과 대조하는 명시적 QA 경로를 추가하며, 전체 테스트와 실패 차단을 제외하거나 완화하지 않는다. Node 전체393통과/Windows의 Linux 전용3생략/실패0이다.

### 2026-09-16 보은 DNS 비교 진단 구현

- 직전 회차는 최신 Git/CI/보관 결과를 읽은 현황 재확인으로 새 구현 진척이 없었다. 이번에는 실패의 다음 행동을 구분하기 위해 고정 보은·제천 OS/JVM/Node DNS 비교기를 추가한 **progress**다. 전체 ATT62/9Gate·제목→본문→첨부→관리자 검증을 유지한다.
- 진단은 QA 브랜치의 명시 표식만 실행하며8논리 조회·최대80초와 기동 여유/단계3분으로 제한한다. HTTP/파일 다운로드/DB 쓰기0, DNS/IP/TLS/SSRF 설정 변경0, 주소/예외/환경 원문 비보관이다. 결과는 수집 성공이 아닌 OBSERVED_NOT_COLLECTION_QA다. [보은 진단 계약](announcement-boeun-worker-db-api-qa-2026-09-16.md)을 따른다.
- Node68건/생략0 통과, Java helper의 임의 대상 거부를 확인했다. 새 workflow 테스트의 첫 컴파일 타입 오류를 수정한 후 표적Java21건(워크플로16·URL 검증5)이21초 성공/실패·생략0이다. production Java·DB/API/UI·migration과 엔진7/첨부19/본문18/형식3·catalog30/기대값1/정상0은 불변이다. 실제 새 Linux 진단은 후속 실행에서 판정한다.
- Git 기본 TLS backend 조회가 인증서 체인 오류로 실패했으나 명령 범위의 Windows schannel backend로 같은 원격 SHA를 확인했다. 인증서 검증을 끄거나 전역 Git 설정을 변경하지 않았다. 운영 재인증·취소 배포 재개 확인·정확한 게시/ENFORCE/기존 데이터 범위 승인·운영 업무 E2E는 여전히 남는다. 사용자 output을 보존한다.
- `8589016`을 QA 브랜치에 커밋/푸시하고 원격 SHA 일치를 확인했다. [Linux35055559668](https://github.com/FrostyCityMan/saneB/actions/runs/35055559668)의04:26:15Z~04:26:27Z DNS 단계에서 보은·제천 모두 OS/JVM/Node A 동일 IPv4 지문, AAAA ENODATA를 실제 metadata로 확인했다. 새 환경에서 실패가 재현되지 않았으나 이전 근본 원인은 미확정이다. HTTP/파일/운영 쓰기0이다.
- 상태 변화 근거에 따라 같은 SHA의 [보은 고정3건 재검증35055669009](https://github.com/FrostyCityMan/saneB/actions/runs/35055669009)를 한 차례 명시 실행했다. 기존 계약 실행과 직렬 concurrency를 유지하며 둘의 전체 terminal 결과는 후속 확인한다. DNS 관측 성공을 파일/정책 QA 성공으로 계산하지 않는다. Gate8진행/1차단·Not ready·goal ACTIVE다.

### 2026-09-16 보은 Linux 결과 — 계약 성공·공식 수집 DNS 차단

- 직전 회차는36cd7fd의 보은 실제 worker 경로·로컬 검증·커밋/푸시와 Linux 실행을 시작한 **progress**다. 이번은 동일35053587664를 terminal까지 확인하고 보관 XML/metadata 및 로컬·공용 DNS를 대조한 **progress**다. 전체 ATT62/9Gate와 제목→본문→첨부→최종 관리자 순서를 유지한다.
- [Linux35053587664](https://github.com/FrostyCityMan/saneB/actions/runs/35053587664)는 전체 failure다. 계약 단계와 독립 PG·정책 부모 연결/취소/정리는 success, XML root2721=2455통과/266조건부 생략/실패0이며 별도 추출기88·패키징20·job192·migration17·runtime1·worker12·부모2·Flyway3 실패/생략0이다. 이 성공을 공식 보은 파일 성공으로 합치지 않는다.
- 보은3공고는 제목 통과 후 BODY DNS_LOOKUP_FAILED/시도0, worker EVALUATED이지만 job PARTIAL_FAILED·발견 불완전·파일0/추출 미실행이었다. 전체3건 JUnit 실패, 예약9요청/6,291,456bytes·원본 정리true·lease0·운영 쓰기0이다. 로컬 Windows와 공용 DNS의 A 조회는 정상이라 Linux/사이트의 근본 원인을 확정하지 않는다. [실패와 다음 진단 경계](announcement-boeun-worker-db-api-qa-2026-09-16.md)를 따른다.
- catalog 문서의 오래된24/15개 참조·검증기 미연결 설명을 현재30/기대값1/정상0·구현된 검증 연결에 맞췄다. 실제 Provider QA·정책 승인으로 승격하지 않는다. 코드/migration/키워드/정책/운영 데이터는 이번에 변경하지 않았다. 동일 실행과 파일 요청을 재시작하지 않았으며 gh 감시 프로세스는 terminal exit1로 종료했다.
- 엔진7/첨부 모델19/전용 BODY18/형식3·Gate8진행/1차단·Not ready·goal ACTIVE다. 보은 Linux DNS, 충주/철원 접속·전체 정상/형식 표본, AWS 재인증·취소 배포 재개 확인·정확한 정책/기존 데이터 범위 승인·운영 업무 E2E가 남는다. 사용자 output은 보존한다.

### 2026-09-16 후속 — 계약 기록 확정·보은 HWPX/PDF worker 검증 연결

- 직전 회차는 현황 재확인으로 새 구현 진척이 없었다. 이번에는 계약·ATT 기록의 실제 검증 상태를035a356으로 커밋·푸시하고 보은 실제 worker 경로를 추가한 **progress**다. 전체 ATT62/9Gate와 제목→본문→첨부→관리자 최종 검증을 유지한다.
- 미커밋 문서6개의 상대 링크61개·diff·제한 자격증명 패턴 검사를 통과했다. 운영 적용/활성화 상태를 과거09-15 snapshot과 구분하며 오래된 기록을 최신 성공으로 계산하지 않는다. 사용자 output은 보존했다.
- 보은의 고정3공고 제목을 공식 HTML HTTP200·표식 각1개로 다시 확인했다. 현재 임시 DRAFT에서3건 모두 제목 통과이며 실제 worker/임시 DB/API에 HWPX2·PDF1을 연결했다. 전용 수동 CI 입력만 외부 요청하며 최대132요청/240MiB·비root 추출 격리·UNKNOWN/부분 검수·원본/lease 정리를 유지한다. [보은 검증 계약](announcement-boeun-worker-db-api-qa-2026-09-16.md)을 따른다.
- Java 표적53건(실제 임시 DB 준비4 포함)2분 성공·Node59건·패키징20건 모두 실패/생략0이다. 후속 패키징23초 성공의 추출기/bootJar/probe는 유효한 선행 결과 재사용이다. 웹 JARc2680a76… 불변, production Java/DB/API/UI/migration/운영 정책 변경0이다. 전체 로컬 root는 반복하지 않으며 새 SHA Linux 전체 계약과 실제 보은 결과는 후속 확인한다.
- 엔진7/첨부 모델19/전용 BODY18/형식3·catalog30참조/기대값1/정상0, Gate8진행/1차단·Not ready·goal ACTIVE다. AWS 재인증·취소 배포 재개 확인·정확한 정책/기존 데이터 범위 승인·운영 업무 E2E는 여전히 남는다.

### 2026-09-16 12:34 이후 — 제천 실파일 worker 통과·DB/API 문서 불일치 정정

- 직전 회차는9fe892c의 제천 실제 worker 경로·로컬 회귀·커밋·푸시·명시적 Linux 실행을 완료한 **progress**다. 이번에는 같은 실행35051444985의 terminal success와 보관 XML/metadata를 회수하고 실제 다음 상태를 확정한 **progress**다. 전체 ATT62/9Gate·제목→본문→첨부→관리자 순서를 유지한다.
- `9fe892c` [Linux35051444985](https://github.com/FrostyCityMan/saneB/actions/runs/35051444985) root2717=2451통과/266조건부 생략/실패0, 별도 추출기88·패키지20·job192·migration17·부모2·runtime1·worker12·Flyway3 실패·생략0이다. 제천 별도JUnit3/3도 통과했고 로컬 판정기로 현재 실행/전체3공고·3파일/완전성·검수·정리 경계를 재확인했다.
- 제목 음성403587은 후속 요청0, 양성403530/403490은 BODY AVAILABLE→전체HWPX3개→실제 worker EVALUATED·job SUCCEEDED→임시 DB/API 일치를 확인했다. 파일11645/4692/4792자 모두 COMPLETE_TEXT지만 혼합/초기 표제 부족으로 UNKNOWN·FINAL_REVIEW_EXCEPTION/REVIEW_REQUIRED다. 전체9요청/5,069,800bytes·원본정리true·lease0·운영쓰기0이며 정상 후보/정책 QA 성공으로 승격하지 않는다. [제천 실행 기록](announcement-jecheon-worker-db-api-qa-2026-09-16.md)을 따른다.
- DB 계약의 V72/운영 미반영·V73~V83 PG 미실행, API의 게시/배치 미구현·Provider 검증 미연결·게시 잠금18개 설명을 실제 코드·migration·JUnit에 맞췄다. ATT037/053/058의 DB 실행 차단 표기를 부분 완료로 갱신하고 운영 잔여를 유지했다. 인용 testcase16개 모두 실제 통과 XML에 존재하며 변경 문서의 상대 링크도 검증했다. [계약 근거](announcement-attachment-contract-evidence-2026-09-16.md)는 과거 운영09-15 V83과 현재 재조회 미실행을 분리한다.
- 이번은 문서 정정이며 production/test code·migration·workflow·DB/API shape 변경0이다. 추가 빌드·외부 수집을 재실행하지 않았고 단발Node 종료·사용자 output 보존이다. 전체 엔진7/첨부 모델19/전용BODY18/형식3·catalog30/기대값1/정상0, Gate8진행/1차단·Not ready·goal ACTIVE다. AWS 재인증·취소 배포 재개 확인·정확한 정책/기존 데이터 범위 승인·운영 업무 E2E는 남는다.

### 2026-09-16 12시대 — 선행 Linux 성공·제천 실제 worker 검증 연결

- 직전 현황 보고는 live 실행35050057694를 확인한 **검증된 대기**다. 이번에는 동일 실행의 terminal success/보관 XML을 회수하고 제천의 본문·실파일 관측을 실제 worker→임시 DB→API로 확장한다. 원래 사용자 goal·QA 계획의 전체 ATT62/9Gate·정상 표본/전체 대상 요구와 제목→본문→첨부→관리자 순서를 다시 대조했다.
- `85c7f65`의 [Linux35050057694](https://github.com/FrostyCityMan/saneB/actions/runs/35050057694)는12:13 KST success다. root2713=2447통과/266조건부 생략/실패0, 추출기88·패키징20·job192·migration17·부모2·runtime1·worker12·Flyway3은 모두 실패·생략0이다. 충주 접속 실패를 재시도하거나 공식 파일 QA 성공으로 바꾸지 않는다.
- 제천 고정3공고 중 제목 음성1건은 저장/후속 요청0, 양성2건은 전체HWPX3개를 대상으로 한다. 검증 코드의 기존 '태백 외 추출1회' 가정을 고정 공고별 기대 호출 수로 교체하고 원문 정리·lease·DB 텍스트/역할/블록·API 대조를 연결했다. 수동 CI 입력만 실제 외부 실행하며 새 엔진/모델·운영 코드/DB/API/UI/정책 변경은 없다. [실행 계약](announcement-jecheon-worker-db-api-qa-2026-09-16.md)을 따른다.
- 표적Java49건·실제 임시DB 준비3건 포함1분45초 성공/실패·생략0, Node 보고서/launcher32건 통과다. 수정본 전체 회귀4분43초 성공: root2717=2452통과/265조건부 생략/실패0·패키징20/20·Node91/91. 추출기88·bootJar·probe는 유효한 선행 결과 재사용이다. Java/임시PG0·단발Node 종료·diff/구문/제한 자격증명 패턴 검사 통과. 웹JARc2680a76…불변·probe ca5e01db…이며 새 SHA의 실제 Linux 제천 실행 결과는 후속 확인한다.
- 엔진7/첨부 모델19/전용 BODY18/형식3, catalog30참조/기대값1/정상0을 유지한다. 역할 혼합UNKNOWN/부분 추출을 정상 후보로 승격하지 않는다. 운영 AWS 재인증·취소 배포 재개 확인·정책/기존 데이터 범위 승인·관리자 업무 E2E가 남아 있고 전체 Not ready/goal ACTIVE다. 사용자 output/Word와 기존 migration을 보존한다.

### 2026-09-16 11:48 이후 — 충주 실제 worker 경로 연결·로컬 Linux 기동 확인

- 직전 회차는 충주 실패 근거/제목 회귀를0b5d7ec로 커밋·푸시한 **progress**다. 이번에는 Docker의 기존 설치 기동 가능성을 확인하고, 차단 중에도 준비할 수 있는 충주 실제 worker→임시 DB→API 검증 경로를 연결했다. 전체 ATT62/9Gate와 제목→본문→첨부→관리자 순서를 유지한다.
- Docker 정지 확인 후45초 옵션으로1회 기동 요청했으나 파일 접근·경로 형식 오류가 재현됐고 정상 engine에 연결되지 않았다. 초기화·재설치·파일 삭제 없이 CLI 취소/stop 후 소유6프로세스0·서비스Stopped를 확인했다. AWS 재인증·취소 배포 재개·정확한 운영 활성화 범위 승인은 여전히 대기다.
- `CHUNGJU` 고정3건을 기존 비root/설치 지문 고정 probe와 별도 Gradle task에 연결했다. 음성2건의 정확한 중단 사유·요청/원문0, 양성1건의 본문→전체 HWP→worker/DB/API 검증을 요구한다. 양성이 예상치 않게 제목 제외되면 JUnit도 실패하며, 독립 probe에 없는 live QA 클래스 역의존을 제거했다. 운영 코드·API·migration·키워드·정책은 변경하지 않았다. [계약/경계](announcement-chungju-worker-db-api-qa-2026-09-16.md)를 따른다.
- 표적 Java45건·Node/Bash6건 실패/생략0, 실제 임시 DB 준비2건과 독립 classloader 로딩을 포함하여1분32초 성공이다. 첫 전체 회귀 handle12978은 소실되고 JVM0/새 전체 XML 없음이 확인돼 성공으로 세지 않았다. 새 실행의 전체 회귀4분42초 성공: root2713=2448통과/265조건부 생략/실패0·패키지20/20·Node65/65다. 추출기88/bootJar/probe는 유효한 선행 결과 재사용이다. 새 공식 공고 요청·동일 CI 관측 재시도·운영/브라우저 실행은 없다.
- 웹 JARc2680a76… 불변, probe JAR1f971725…와 시험 class 전용 구성을 확인했다. 최신 코드의 Linux 계약 실행은 후속 SHA에서 별도로 확인하고 공식 충주 관측의 이전 실패를 성공으로 바꾸지 않는다.
- 엔진7/모델19/전용 BODY18/형식3·catalog30참조/실행 기대값1/정상0을 유지한다. 새 경로 구현은 공식 파일 검증 성공이 아니며 전체 Gate Not ready/goal ACTIVE다. 사용자 output/Word와 과거 migration은 보존한다.

### 2026-09-16 11:33 이후 — 충주 Linux 접속 실패 확정·제목 미충족 원인 검증

- 직전 현황 응답은 실행35047528916의 live 상태를 직접 확인한 **검증된 대기**다. 이번은 같은 실행의 terminal failure와 artifact를 회수해 다음 행동을 확정하고 제목 근거 회귀를 추가한 **progress**다. 전체 ATT62/Gate0~8, 제목→본문→첨부→관리자 최종 검증을 유지한다.
- `174230a` [Linux35047528916](https://github.com/FrostyCityMan/saneB/actions/runs/35047528916)는 전체 failure다. 주 계약/독립 격리/정책 부모 연결·취소·정리/필수보고서 단계는 성공했다. 보관 JUnit root2710=2444통과/266조건부 생략/실패0; 별도 추출기88·패키지20·job192·migration17·worker12·runtime1·부모2·Flyway3 실패/생략0이다. 실제 충주 관측과 이 성공을 합치지 않는다.
- 충주3건 중 음성2건은 제목 미충족/후속 요청0. 양성70852는 BODY2회 TIMEOUT 뒤 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT이며 파일0·추출 미실행이다. 예약3요청/2,097,152bytes·원본 정리true·운영 쓰기0이다. 앞선 Windows 다운로드 성공이 Linux 접속 성공을 증명하지 않는다. 같은 원격 요청을 재시작하지 않았다.
- 초기 DRAFT의 미충족 원인은72625 대상 근거 없음(`교통약자` 미등록),72039 보조 대상`기업`+보조 지원형태`지원`이다. A/B 적중은0이다. [충주 상세 진단](announcement-chungju-eminwon-profile-2026-09-16.md)에 근거와 별도 규칙 검토 절차를 기록했다. 초기 seed 문서의 첨부 제외·제외 원문 저장은 역사적 설명임을 명시했다. 키워드/강도/운영 ACTIVE를 자동 변경하지 않았다.
- 실제 DRAFT TAG·강도·대상/유형·A/B 부재 회귀를 보강한 표적47건이1분5초 성공/실패·생략0이다. production Java/DB/API/UI와 로컬 웹 JARc2680a76…는 불변이다. 전체 로컬2710건의 선행 결과와 이번 표적47건을 구분한다. Java/임시PG0·사용자 output 보존이다.
- Docker Linux pipe 부재/WSL docker-desktop만 존재를 새로 확인했다. AWS는 앞선 인증 만료 이후 재인증 대기, 취소 배포 재개와 정확한 정책/기존 데이터 범위 승인도 대기다. 운영·브라우저를 다시 조회하거나 데이터/정책을 변경하지 않았다. 엔진7/첨부 모델19/전용 BODY18/형식3·catalog30참조/실행 기대값1/정상0·전체 Not ready를 유지한다.

### 2026-09-16 11시대 — 제천 결과 판독·충주시 전용 모델 연결

- 직전 상태 보고는 `f5d06e4` [Linux35044622501](https://github.com/FrostyCityMan/saneB/actions/runs/35044622501)의 성공·실제 산출물을 확인한 **progress**다. root2680=2415통과/265조건부 생략/실패0, 별도 DB/worker/추출기 등335건 실패/생략0이다. 제천3건/3통과, 음성1요청0·양성2의HWPX3개 COMPLETE_TEXT·원본 정리true·운영 쓰기0을 확인했다. UNKNOWN 원인은 혼합 역할2개·초기 표제 부족1개이며 최종 검수/정상 공고0을 유지한다.
- 이번은 전체 수집처 적용성을 넓히는 **progress**다. V61/V62 `LGS-000137/SAEOL_GOSI`의 충주시 공식 목록·상세를 실측하고 전용 BODY·첨부 모델을 추가했다. 공통 첨부 구현7/등록 모델19/전용 BODY18/형식3, catalog30참조/기대값1/정상0이다. [경계·실측·검증](announcement-chungju-eminwon-profile-2026-09-16.md)을 따른다.
- 첫 표적166건의1실패는 잘못 생성한 빈 POST 시험 입력이었다. 기존 request 거부를 보존하고 고친 후166건 및 공식3건은54초 성공이다. 격리 DRAFT 제목에서2건은 미충족/요청0, 결혼·출산가정 공고1건은 BODY523자·HWP170,496bytes signature 통과, 원본 정리/운영 쓰기0이다. 제목 제외를 우회해 파일을 더 다운로드하지 않았다.
- 같은 충주 고정3건을 제목→본문→Linux 격리 추출→역할/종합 판정 관측과 CI에 연결했다. 최종 Node59/59, 후속 표적·패키징1분38초 및 전체 회귀4분36초 성공이다. root2710=2445통과/265조건부 생략/실패0. 패키지20·추출기88·bootJar는 선행 유효 결과 재사용이며 probe는 재생성했다. 로컬 JAR `c2680a76…`는 운영 지문이 아니다. 새 SHA Linux 관측은 후속 결과로 판정하며 정상 공고·정책 QA로 선승격하지 않는다.
- DB migration·API shape·UI·운영 정책/데이터는 변경하지 않는다. 읽기 전용 Runtime 재확인은 AWS_AUTH_REFRESH_REQUIRED로 SSM 전송 전에 중단됐고 임시 CA를 정리했다. 재로그인을 요청했으며 마지막09-15의787c594/추출기1.0.1/V83/worker 비활성은 과거 직접 근거다. 취소된 배포는 재개하지 않았고 관리자 업무 브라우저 E2E도 남는다. Java/임시PG·단발 Node 종료·사용자 output/Word 보존이다.

### 2026-09-16 — 최신 Linux 성공 확인·제천 세 단계 관측 연결

- 직전 현황 보고에서 `f7b7396` [Linux34944248951](https://github.com/FrostyCityMan/saneB/actions/runs/34944248951)의 완료·success와 실제 보관 산출물을 새로 확인했으므로 **progress**다. root2677=2412통과/265조건부 생략/실패0, 별도 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3 실패/생략0이다. 태백 고정 HWPX2개 재비교 PASSED/완전성true 및 HWP9734자/495블록 PARTIAL_TEXT·검수 상태를 확인했다. 각각 별도 JUnit1/1, 운영 쓰기0·원본 정리true다. 기본 추출기 불변 release 연결의 Linux 단계도 성공했지만 운영 원복 E2E는 아니다.
- 현재 증분은 기존 제천 참조3건의 실제 제목 Gate와 Linux 본문·전체 첨부 관측 경로를 연결하는 **progress**다. 조사원 모집1건은 조합 미충족/후속 요청0의 음성 표본, 나머지2건은 전체 HWPX3개를 관측한다. 음성1건과 알려진 파일1개를 catalog에서 삭제하거나 정상으로 만들지 않는다. [설계·한계](announcement-jecheon-three-stage-observation-2026-09-16.md)를 따른다.
- 첫 로컬 표적13건은 Windows initdb4551로1실패였지만 보안 설정 변경 없이 후속 표적43통과/Linux1생략, 최종 전체5분4초 성공을 확인했다. root2680=2416통과/264조건부 생략/실패0·패키지20통과, 추출기88/bootJar는 선행 결과 재사용이다. Node63통과/생략0·probe 재생성·JAR917d743e…불변·Java/임시PG/단발 Node 종료·diff/자격증명 패턴 검사 통과다. 새 SHA Linux/제천 실제 관측은 후속 결과로 판정한다.
- 전체 엔진6/첨부 모델18/전용 BODY17/형식3·catalog27참조/기대값1/정상0, ATT62/9Gate 범위를 유지한다. production Java/DB/API/UI·운영 정책·데이터 변경은 없다. 이번 운영 서버/DB·브라우저는 재조회하지 않았으며 09-15의787c594/추출기1.0.1/worker 비활성은 마지막 직접 확인 기록이다. 사용자 output을 보존한다.

### 2026-09-15 16시대 후속 — 실제 HWPX 재검토와 기본 추출기 버전 연결

- 직전 회차는 실제 HWP1.0.3 검증/부분 원인 관측의 **progress**였고 이번도 **progress**다. `cc79d59` [Linux34941434590](https://github.com/FrostyCityMan/saneB/actions/runs/34941434590)와 공식 태백184816 관측1건이 성공했다. 전체 HWPX2파일의 binary/locator/quality/텍스트/블록/역할 지문이 구기대값과 모두 일치했고 BODY 완전성true다. 정상 후보0·혼합UNKNOWN/FORM·관리자 검증을 유지한다.5요청/2377939bytes·원본 정리·운영 쓰기0. [재검토 근거](announcement-taebaek-fixed-qa-expectation-2026-09-15.md)를 따른다.
- 공통 코드 변경을 검토한 후 catalog의 버전/태백 profileHash/관측 시각3필드만 갱신했다. 파일 기대값·문구·역할·전체27참조·상한은 보존했다. 구지문 거부 시험은 역사적 fixture로 유지하고 새 packaged catalog의 실행 가능1/정상0/전체 coverage false를 검증한다. 새 CaseExecutor의 실제 고정 비교는 후속 원격 검증이 필요하다.
- 릴리스 점검에서 JAR 복구와 공용 추출기 버전이 어긋날 수 있는 경로를 확인했다. 기본 worker 추출기도 실제 웹 JAR의 불변 release에서 선택하도록 launch/helper를 보완했다. 명시 경로는 각각 보존하고 공용 디렉터리·app.env·플래그를 변경하지 않는다. 로컬 Node17건 중14통과/실패0·Linux3생략, 실제 Git Bash 선택·복구·경계·구문 검증을 완료했다. [계약/한계](../deployment/attachment-extractor-release-binding-2026-09-15.md)를 따른다.
- Runtime SSM `d291efb8-5107-485f-9cae-6f66def58921`, Database SSM `7547e95d-562f-4bef-baa8-15659ed0c8c6` 모두 Success. 운영787c594/추출기1.0.1·health UP·실제DB V83/실패0·첨부 모든 count0·worker/QA 비활성·외부키2종 없음/지자체223/목록parser41/원문2945를 새로 확인했다. READ ONLY/ROLLBACK/쓰기0·임시 CA 정리. 기존 배포 취소를 재개하거나 브라우저를 조작하지 않았다.
- 표적 Java 회귀·패키지/bootJar는1분23초 성공했고 전체 회귀4분36초도 성공했다. root2677=2413통과/264조건부 생략/실패0, 패키지20·추출기88은 앞선 유효 결과 재사용(UP-TO-DATE)이며 실패/생략0이다. 새 SHA의 Linux/고정 HWPX 비교/HWP worker는 후속 결과로 판정한다. 새 웹 JAR 지문은917d743e…다. Java0·단발 Node 종료·diff 검사 통과, 사용자 output 보존이다. 현재 엔진6/첨부18/본문17/형식3과 ATT62/9Gate 범위를 유지한다. 이전cc79d59 대상 배포 확인에 이번 미배포 변경이 자동 포함됐다고 가정하지 않는다.

### 2026-09-15 16:19~16:23 KST — 최신 HWP 실제 연결 성공·부분 원인 축소

- 직전 상태 보고 회차는 실행 중 CI를 확인한 **검증된 대기**였고 이번은 **progress**다. `cc79d59`의 [Linux34939913277](https://github.com/FrostyCityMan/saneB/actions/runs/34939913277) 전체 성공/보관 XML·실제 HWP metadata를 회수·대조했다. root2676=2411통과/265조건부 생략/실패0, 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 실패/생략0. 실제 태백 HWP는별도1건/실패·생략0이며 Node 보고서 판정 통과다.
- 동일 HWP125952bytes를1.0.3에서 실제 BODY→발견→추출→worker→임시 DB→API로 검증했다. 9734자/495블록·PARTIAL_TEXT/TECHNICAL_EXCEPTION/REVIEW_REQUIRED, 전체 완전성false다. 개체/그림 레코드3개씩이 부분 판정 조건임을 명세·코드와 대조했지만 이미지 내용·다른 누락 구조는 미확인이다. 정상 후보/전체 Provider QA로 계산하지 않는다.4요청/2354176bytes·원본/lease정리·운영 쓰기0. [실행 상세](announcement-taebaek-hwp-ci-qa-2026-09-15.md)를 따른다.
- 철원은 별도 네이티브 GET1회에서도3초 TCP 연결 시간 초과/수신0이었다. Java 선택자 검사 이전 연결 실패로 범위를 좁혔고 동일 요청·파일 요청·보안 완화는 중단했다. [연결 진단](announcement-cheorwon-bbs-profile-2026-09-15.md)에 관측/추론 경계를 남겼다.
- 같은cc79d59에서 기존 태백 HWPX2개를 재관측하는 [Linux34941434590](https://github.com/FrostyCityMan/saneB/actions/runs/34941434590)를 명시적 opt-in으로1회 시작했다. 기존 기대값/지문 자동 갱신·정책 승인·운영 데이터 변경은 없다. 이 실행의 완료는 후속 조회로 판정한다.
- production 코드/migration/API/UI 변경 없이 증거 문서만 갱신했다. 엔진6/첨부18/본문17/형식3·catalog27/보관기대값1/현행 실행0·전체ATT62/9Gate Not ready 유지. 취소된 배포 재개 질문은 최신cc79d59(1.0.3)/사전 운영 점검 후 코드·추출기만 설치하는 범위로 정정했다. 첨부worker 활성화/게시/ENFORCE/기존 데이터는 제외한다. AWS/브라우저는 이번에 조작하지 않았으며 마지막 운영787c594/1.0.1/worker 비활성은 과거 직접 근거다.

### 2026-09-15 16시대 — 철원 TIMEOUT 실증·배포 없는 태백 HWP QA 연결

- 직전 회차는7a414e8의 철원 모델/회귀/커밋·푸시를 완료한 **progress**다. 이번은 같은 목표의 실제 외부 검증과 미배포 최신 추출기 검증 경로를 연결한 **progress**다. 전체 ATT62/9Gate와 제목→본문→첨부→관리자 순서를 유지한다.
- 철원 HEAD는 TLS 검증0/HTTP200이었지만 실제 본문 수집3건은 모두TIMEOUT(6.474/6.022/6.024초)이다. 처음 일반 test의 외부 비활성 설정으로 실행0을 확인한 뒤 올바른 전용 task로 실행했다. 같은 조건 재시도·파일 요청 확장을 중단했고 BODY/첨부 성공으로 계산하지 않는다. [철원 후속 기록](announcement-cheorwon-bbs-profile-2026-09-15.md)을 따른다.
- 배포 재개 승인과 별개로 최신 HWP1.0.3의 실제 파일 검증이 가능하도록 기존 Linux workflow에 고정 태백 HWP1건/44요청·80MiB/420초 opt-in을 추가했다. 같은 SHA의 전체 계약 성공 후 실제 worker·임시 PG·API를 실행한다. 기존 양평3건 분모와 기본 외부 비활성은 보존한다. 원본 아닌 고정 metadata/JUnit만 업로드하고 이번 실행시각·정확한1건·실패/생략0·완전성 분리·원본/lease정리·운영쓰기0을 판정한다. [실행 계약](announcement-taebaek-hwp-ci-qa-2026-09-15.md)을 따른다.
- 로컬 Java 표적35통과/외부 opt-in1생략·Node31통과/생략0·패키지20통과/생략0, 빌드22초 성공. production Java/DB/API/UI/엔진·모델 수는 그대로이며 웹 JAR는직전7a414e8과 같은00c66025…다. 로컬 전체 일반 회귀·공식 HWP 실행 성공으로 확대하지 않는다. 소유 Node/Gradle 종료·Java0·사용자 output 보존.
- 새 commit은 중복 자동 CI만 `[skip ci]`로 생략하고 같은 SHA의 전체 계약+HWP를 명시적 수동 workflow에서 실행한다. 새 원격 실행은 실제 실행 ID/상태를 확인해야 한다. 운영 배포·정책 게시·ENFORCE·기존 데이터·관리자 인증 업무 E2E는 미실행이며 goal ACTIVE다.

### 2026-09-15 15:45 KST — 철원군 모델·공고 소속 검증과 HWP Linux 증거

- 직전 회차는 상태 보고 중 ee72b97 Linux 성공을 새로 확인한 검증 진척이다. 이번은 **progress**로 철원군 LGS-000129/게시판25·메뉴1226의 전용 본문·첨부 모델을 추가했다. 공통 엔진6/첨부 연결18/전용 본문 연결17이나 철원 실사이트 성공 수량은 아니다. 직접 상세 HTML1회 뒤 후속3회 시간 초과로 동일 조건 재시도를 중단했으며, 정확한 본문 marker/파일 MIME·signature·추출은 미확인이다. [기관 계약·한계](announcement-cheorwon-bbs-profile-2026-09-15.md)를 따른다.
- 파일/게시판/현재 공고번호3개 query를 필수로 하고 redirect에서도 최초 공고·파일 소속을 유지한다. 다른 기관의 MIME/헤더·HTTP·미리보기 예외를 차용하지 않는다. 불명확한 발견/미지원/부분 파일을 정상 후보로 승격하지 않는다. DB/API/UI/V1~V83/운영 정책·기존 데이터는 변경하지 않았다.
- catalog는27참조/보관 기대값1이다. 기존 태백 기대값은 내용과 구profileHash를 보존하므로 새 코드에서는 PROFILE_CHANGED/현재 실행0이다. 참조 추가와 합성 fixture를 공식 정확도나 정책 QA 통과로 계산하지 않는다. 첫 전체 회귀51실패의 mock 기본 overload 연결과 구지문 무효화 시험을 보완했고, 표적202/생략0을 통과했다. 최종15:49 전체5분11초 성공: root2675=2411통과/264조건부 생략·실패0, 패키지20통과/생략0. 추출기88·웹 JAR/probe는 UP-TO-DATE다. Node15통과/Linux2생략·diff/자격증명 패턴 검사 통과·Java0·단발 Node 종료·사용자 output 보존이다.
- ee72b97 [Linux34936106328](https://github.com/FrostyCityMan/saneB/actions/runs/34936106328) success 보관 XML 확인: root2653=2390통과/263조건부 생략, 추출기88·패키지20·job192·migration17·worker12·runtime1·정책부모2·Flyway3 실패/생략0. 현재 철원 변경 SHA의 검증은 아니며 공식 사이트 opt-in/운영 배포도 미실행이다.
- 마지막 직접 운영 근거는787c594/추출기1.0.1/DB V83/첨부 worker 비활성이다. 취소된 배포의 재개 확인, API key 직접 설정 및 관리자 재로그인은 대기다. 현재 회차는 AWS/브라우저를 다시 조작하지 않았다. 제목→본문→실제 첨부→최종 관리자 검증, ATT62/9Gate 전체 범위를 유지하고 goal ACTIVE다.

### 2026-09-15 15:14 KST — HWP 표·셀 순서 처리1.0.3 로컬 구현

- 직전 회차는 현황 보고로 구현 진척이 없었다. 이번은 **progress**다. 공개 작성/읽기 형식의 실제 셀 헤더8바이트 기본 영역과 레코드 깊이를 대조하여 표 앞 문장→셀별 문단/중첩 표→표 뒤 문장의 순서를 구현했다. 행·열/span·전체 격자·문단 수/종료 플래그/문자 수를 검증하고 각 셀/문단/연속 구간의 scope를 분리한다. 단순 TABLE tag 차단 해제가 아니다.
- 추출기/서버 기대 버전1.0.3. 불일치·앵커 누락·미지원 구조는 부분 추출로 남고 노드/깊이/격자 예산을 추가했다. DB/API/UI/V1~V83·정책·운영 데이터는 변경하지 않았다. 엔진6/첨부 profile17/전용 BODY16/형식3은 동일하다. [계약과 한계](announcement-hwp-table-text-v1.0.3-2026-09-15.md)를 따른다.
- 최종35초 로컬 성공: root2653=2391통과/262조건부 생략(앞선 전체 실행 결과 재사용), 추출기88(신규30)·패키지20 실패/생략0, bootJar/probe UP-TO-DATE. 검증 중 source/class 컴파일 시점 불일치로 새 테스트1건이 실패했으나 최종 소스 재컴파일 후 전부 통과했다. Node15통과/Linux2생략·JAR/class2종 hash 일치·Java0·사용자 output 보존이다.
- 새 SHA Linux/실제 HWP/HWPX/운영 설치/브라우저 검증은 아직 없다. 취소된 배포의 재실행 확인과 관리자 재로그인/API key 등록은 대기다. 기존 실제 HWP9743자의 부분 추출 해소를 주장하지 않는다. 전체 기관 대응·공식 기대값·승인된 게시/ENFORCE/기존 데이터·업무 E2E까지 ATT62/9Gate는 계속 Not ready, goal ACTIVE다.

### 2026-09-15 14:45 KST — HWP 보완 커밋·Linux 성공, 배포 취소 확인

- `a860bbdf820f13bb6ed151020089f616447fb08f`를 한글 커밋·푸시했고 HEAD/origin 일치를 확인했다. Linux [34929791026](https://github.com/FrostyCityMan/saneB/actions/runs/34929791026) success의 보관 XML을 내려받아 검토했다: root2653=2390통과/263조건부 생략, 추출기58·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3은 실패/생략0이다. 로컬 watch는 네트워크 오류로 종료됐으나 원격 작업은 재조회로 성공을 확인했으며 중복 시작하지 않았다.
- 새 배포 [34933705970](https://github.com/FrostyCityMan/saneB/actions/runs/34933705970)는 14:42 테스트·빌드 도중 cancelled다. 번들·AWS 인증·S3·CodeDeploy 모두 skipped이며 새 운영 설치는 없다. 제공 annotation만으로 취소 주체/원인을 특정하지 못해 자동 재실행하지 않고 사용자에게 재배포 여부를 확인한다. [HWP 상세 기록](announcement-hwp-structure-diagnostic-2026-09-15.md)을 따른다.
- 14:44 AWS inventory는 기존 `d-MWENRAGTK`/`787c594` Succeeded·서울/default root 대상 일치·SSM Online을 확인했다. 추출기1.0.2의 실제 HWP 구조 관측과 HWPX 사전 기대값 재비교는 미실행이다. 정책 게시·ENFORCE·기존 데이터·새 운영 DB 변경도 없다.
- 승인된 운영 브라우저 탭은 로그인 화면임을 metadata로 확인하고 handoff로 유지했다. 비밀번호/입력값·쿠키를 읽지 않았으며 로그인·업무 E2E·새 스크린샷/반응형 성공으로 계산하지 않는다. 기업마당·정부24 키의 운영 환경 직접 등록 가능 여부를 한 번 요청했다. Java0·소유 Node/gh/임시 CA 종료·사용자 output 보존. 전체 ATT62/9Gate Not ready·goal ACTIVE다.

### 2026-09-15 13:40 KST — HWP 구조 진단·미해석 레코드 품질 보완

- 직전 회차는 상태 보고와 실행 중 표적 시험 결과 회수(103통과/실패0/Linux1생략)였다. 이번은 추출기1.0.2의 전체 회귀·문서화 **progress**다. 전체 ATT62/9Gate Not ready와 제목→정제 본문→실제 첨부→관리자 최종 검증 범위를 유지한다.
- HWP의 기존76~88 밖 미해석 레코드가 COMPLETE_TEXT가 될 수 있는 공백을 보완했다. 원문 없는 section/record/level/tag 숫자 진단과 부모 IPC의 엄격한 계약 검증을 추가했다. 표를 완전히 지원한 것이 아니며 HWP 실제 누락 구조는 새 설치 후 확인한다. DB/API/UI/V1~V83은 변경하지 않았다. [상세 계약](announcement-hwp-structure-diagnostic-2026-09-15.md)에 범위·호환성·남은 검증을 기록했다.
- 전체 로컬 시험5분4초 성공: root2653=2391통과/262조건부 생략·실패0, 패키지20통과/생략0, 추출기58건은 앞선 표적 실행의 결과를 재사용(UP-TO-DATE). Node 배포/공식 probe15통과·Linux2생략, bootJar/probe 생성과 diff 검증 완료. 새 SHA Linux·실제 설치·공식 HWP/HWPX 재검증은 아직 미실행이다.
- 배포 전 Runtime SSM `3b1a2852-a8be-42f3-90d9-5aeafbd7818b` Success: 운영787c594/추출기1.0.1/JAR V83/health UP/추출기-불변 QA 일치·첨부 worker/QA 비활성·외부 API key2종 없음 재확인. 정책 게시·ENFORCE·기존 데이터는 실행하지 않았다. Java/Node·임시 CA 정리, 사용자 output 보존. 관리자 업무 브라우저 E2E는 여전히 남는다.

### 2026-09-15 13:16 KST — 새 catalog 배포·첫 고정 기대값 실증

- 직전 회차는 특정 CI34925971820/34926361587의 **검증된 대기**였고 이번은 배포·실제 기대값 비교를 완료한 **progress**다. 전체9Gate/ATT62 Not ready·goal ACTIVE이며 원래 범위와 제목→본문→실제 첨부→관리자 검증 순서를 유지한다.
- `787c594` Linux34926361587 success/artifact 확인: root2648=2385통과/263조건부 생략, extractor36·패키지20·job192·migration17·worker12·runtime1·정책부모2·Flyway3 실패/생략0. 배포34927650269/CodeDeploy `d-MWENRAGTK` success, 실제 JAR `d8696e85…`/DB V83·실패0·health UP·catalog24/기대값1·추출기/불변 QA 일치를 확인했다. [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)에 지문·SSM 근거를 기록했다.
- 태백184816 SSM `fe2aac98-5851-4bd8-9171-4abe962cf7b7`1/1통과·생략0. BODY/HWPX2파일/worker/임시 DB/API 후 production CaseExecutor의 **사전 기대값 실제 재비교2/2 PASSED**다. 전체8요청/2658726bytes, 원본/lease/임시PG/전송 정리·운영 DB 쓰기0·설치 JAR 불변. 혼합UNKNOWN/FORM·FINAL_REVIEW_EXCEPTION 유지, 정상 공고0/전체 기대 coverage false/정책 QA false다. [고정 비교 기록](announcement-taebaek-fixed-qa-expectation-2026-09-15.md)을 따른다.
- 태백HWP SSM `b7289058-6eba-4d99-aed6-f33c4c780574`1/1통과·생략0. 새 설치 runtime에서9743자/480블록 PARTIAL_TEXT·TECHNICAL_EXCEPTION 재확인, 문자 대체 표시0으로 구조 레코드 경로를 다음 조사 대상으로 좁혔다. 개별 구조·누락 내용은 미확인이다.4요청/2354176bytes·원본/임시자원 정리·운영 쓰기0. 원문 비노출 진단 로컬14건/생략0·Node5건 통과, 운영 추출기 판정은 바꾸지 않았다.
- 첨부 정책/작업/파일/배치는 모두0·worker/정책QA/ProviderQA 비활성, 기존 지자체223/목록parser41/원문2945·외부 API key2종 없음이다. 관리자 탭은 로그인 화면이며 업무 E2E는 재로그인 대기다. 게시·ENFORCE·기존 데이터 적용과 전체 수집처 검증은 계속 남는다. 사용자 output을 보존한다.

### 2026-09-15 12:45 KST — 실제 HWP 전체 worker 경로 관측

- P3 **progress**, 전체9Gate/ATT62 Not ready다. 기존 태백 모델에 고정 공고176153의 독립 시험 그룹만 추가했다. 엔진6/profile17/본문16/추출형식3과 production code/catalog1기대값은 변하지 않았다.
- SSM `f066eadd-753f-498b-b0e4-b863c1efec24` Success/1통과·실패/생략0, BODY AVAILABLE와 HWP125,952bytes/9,743자/480블록의 실제 다운로드·격리 추출→worker→임시 DB→API를 확인했다. PARTIAL_TEXT/UNKNOWN·PARTIAL_FAILED/TECHNICAL_EXCEPTION·REVIEW_REQUIRED를 유지하며 완전 추출로 계산하지 않는다.
- 총4요청/2,354,176bytes·원본/lease/임시 PG/전송 정리 성공·운영 DB 쓰기0·설치2bde216 JAR 불변이다. 로컬23건/생략0·Node5건 통과. [공식 worker 기록](announcement-official-worker-db-api-qa-2026-09-15.md)에 지문·경계를 남겼다. 새 catalog7e1c2de의 Linux34925971820는 현재 진행 중이며 새 SHA 배포/고정 기대값 재검증은 아직 성공이 아니다.

### 2026-09-15 12:28 KST 이후 — 첫 공식 전체 파일 기대값 연결

- 직전 회차는 상태 확인이며 구현 진척은 없었다. 이번은 공식 기대값0의 공백을 줄인 **progress**다. 전체 ATT62/9Gate는 Not ready, goal ACTIVE다. 제목→정제 본문→전체 첨부 텍스트→관리자 최종 검증을 유지한다.
- SSM `97f6ed09-0bf2-42c5-8b13-97aa408efa1b` Success/태백1건 통과·실패/생략0. BODY AVAILABLE·HWPX2개 완전 추출/DB/API와 혼합UNKNOWN·양식FORM 사유를 재확인했다. 원본/lease/임시PG/전송 정리·설치 JAR 불변·운영 DB 쓰기0,5요청/2,377,939bytes다.
- 실제 파일/본문과 독립적으로 확인한 고정 업무 문구·구조를 검토하여 catalog v2에 전체2파일의 역할/품질/locator/binary/text/block/assessment 기대값을 고정했다. 총24참조/실행 기대값1/정상 공고0이다. snapshot 시각 처리 문제를12건 실패로 재현하고 catalog 소유 JavaTimeModule로 보완했다. 최종 표적137건/생략0·Node launcher5건 통과다. 전체 회귀·새 SHA Linux/배포/고정 기대값 재실행은 후속 결과를 따른다.
- 태백 probe에 production 단건 실행기의 사전 기대값 재비교를 연결했고 기존44요청/80MiB·420초 합산 상한을 보존했다. 기대값을 실행 결과로 자동 덮어쓰거나 UNKNOWN을 정상 처리하지 않는다. 상세는 [태백 기대값](announcement-taebaek-fixed-qa-expectation-2026-09-15.md)이다.
- 이전 커밋 b7eaf33의 Linux34924308859는 success이며 artifact XML을 확인했다. root2639=2376통과/263조건부 생략/실패0, extractor36·패키지20·job192·migration17·worker12·runtime1·부모2·Flyway3 실패/생략0이다. 새 catalog 변경의 동일 SHA 검증은 아니다.
- 운영은 직전2bde216/DB V83/worker·정책QA 비활성이고 이번 정책/설정/기존 데이터 변경·브라우저 검증은 없다. 전체 기관/정상 다중첨부/형식 기대값·HWP 전체 worker·외부 API key·승인된 활성화/기존 데이터·관리자 업무 E2E는 계속 남는다. 사용자 output을 보존한다.
- 최종 전체 회귀4분39초 성공: root2647=2385통과/262조건부 생략/실패0, 패키지20/20·Node25통과/2 Linux 전용 생략이다. 추출기/bootJar/probe는 UP-TO-DATE이며 앞선 웹 JAR 생성 지문은2dd20b2b…다. 사용한 단발 Node/Gradle과 소유 임시PG/전송 자원을 정리했다. 새 SHA의 CI·운영 배포/고정 기대값 비교는 별도 확인한다.

### 2026-09-15 12:10 KST — 실제 혼합 역할 사유·태백 양식 인식 확인

- P3의 실제 파일 근거를 추가한 **progress**다. 전체9Gate/ATT62 **Not ready**, goal ACTIVE를 유지한다. 직전 배포2bde216의 운영 JAR/추출기1.0.1/역할1.0.2/DB/API/정책은 변경하지 않았다.
- probe metadata에 저장된 역할 판정의 고정 사유/일치 규칙/개수만 추가했다. 양평 재검증 SSM `48b5d034-a632-4c49-bf02-7cd5ab0ee8df` Success/3통과·생략0, HWPX11398자의 UNKNOWN은 NOTICE/FORM 제목 근거11개/11블록이 공존한 MIXED_DOCUMENT_ROLES로 확정했다. 명세에 따른 예외이며 원문 미확인 상태에서 규칙을 완화하지 않는다.
- 기존 양평3건 기본 동작과 필수 분모를 보존한 채 이미 관측한 태백184816 고정1건의 worker/DB/API 그룹을 추가했다. 임의 URL/ALL/임의 입력은 거부한다.44요청/80MiB 한도, 실제5요청/2377939bytes다. 태백 SSM `d6fe5871-496b-4412-a073-bccbdddae699` Success/1통과·생략0, BODY AVAILABLE/1시도와 HWPX2개 완전 추출·worker job SUCCEEDED/텍스트·역할·v2 API 일치를 확인했다.
- 태백 첫 파일2041자는 혼합 역할 UNKNOWN, 둘째1994자는 FORM/ROLE_TEXT_STRUCTURE_MATCHED/3근거로 자동 식별됐다. 앞선1.0.1 역할 관측의 구조 부족이 실제 파일에서 개선된 증거다. 전체 텍스트 완전성true지만 혼합 파일 때문에 FINAL_REVIEW_EXCEPTION·관리자 검수는 남는다. 정상 공고3개/정책 QA/전체 기관 성공으로 합산하지 않는다.
- 두 관측 모두 운영 DB 쓰기0·자동 confirmation/link0·lease0·원본/임시PG/전송 정리 성공·설치 JAR 불변이다. 사용자 관리자 재로그인은 아직 대기 중이며 브라우저를 다시 조작하지 않았다. 상세는 [공식 worker 기록](announcement-official-worker-db-api-qa-2026-09-15.md)과 [양식 역할 보완](announcement-form-role-markers-2026-09-15.md)이다. 로컬 표적60건/생략0과 Node5건은 통과했고 전체 회귀 결과는 후속 검증 기록을 따른다.
- 최종 전체 회귀4분28초 성공: root2639=2377통과/262조건부 생략/실패0, 패키지20/20. 추출기/bootJar/probe는 UP-TO-DATE이며 웹 JAR 지문은 직전과 동일하다. Node25통과/2 Linux 전용 생략·diff 검사 통과, 사용한 단발 Node/JVM 종료·소유 임시PG/전송 정리·사용자 output 보존이다. 새 검증 코드 CI와 운영 코드 재배포를 혼용하지 않는다. 브라우저 탭 metadata는 로그인 화면으로 확인했고 입력 필드는 읽지 않았다.

### 2026-09-15 11:58 KST — 추출기1.0.1 운영 배포·실파일 worker 개선 확인

- 이번 회차는 동일 SHA CI·운영 배포·실파일 연결 검증을 완료한 **progress**다. 전체9Gate/ATT62는 **Not ready**, goal ACTIVE다. 제목→정제 본문→실제 첨부 텍스트→관리자 최종 검증 목표를 유지한다.
- `2bde2161328c9f876eaaddaba374889c9dedf825`의 [Linux34922098905](https://github.com/FrostyCityMan/saneB/actions/runs/34922098905) success, root2636=2373통과/263조건부 생략, extractor36·패키지20·job192·migration17·worker12·runtime1·부모2·Flyway3은 실패/생략0이다. 공식 사이트 opt-in은 CI에서 미실행이며 서버 실증과 구분한다.
- 같은 SHA의 [배포34922807546](https://github.com/FrostyCityMan/saneB/actions/runs/34922807546)/CodeDeploy `d-Q1K7M8ETK` success. 실제 JAR `b31a27aa1267310e9e71401c66c2666c0fc9f82dbe95a6eacbd284244e575ece`, DB/JAR V83·migration 실패0·health UP·추출기1.0.1만 설치·운영/불변 QA 라이브러리 일치를 확인했다. API/migration/정책·worker 활성화/기존 데이터 적용은 변경하지 않았다.
- 고정 파일4건 진단 통과 후 공식 양평 worker SSM `adee6e78-7976-4046-94e3-c06c6d2941e6`가3/3통과했다. HWPX11398자는 PARTIAL_TEXT→COMPLETE_TEXT/job SUCCEEDED, 분할 블록1·불확실 블록0이며 BODY/전체 첨부 텍스트 완전성 true다. 다만 역할 UNKNOWN/FINAL_REVIEW_EXCEPTION·관리자 검수 필요가 남는다. PDF4195자 부분/JPG미지원은 TECHNICAL_EXCEPTION 유지, 제목 제외는 원문/작업 저장·후속 요청0이다. 운영 DB 쓰기0·원본/lease/임시PG/전송 정리 성공·설치 JAR 불변이다.
- 운영 첨부 정책/ACTIVE/set/file/extraction/job/batch 모두0, 첨부 worker/정책QA/ProviderQA 비활성이다. 지자체223·목록parser41·원문2945, 외부 API key2종 없음이다. 새 배포 후 관리자 세션 만료→인증 차단 안내→로그인 화면 이동을 실제 브라우저로 확인하고 사용자 재로그인을 요청했다. 새 SHA 관리자 업무 E2E는 미완료다.
- 다음은 UNKNOWN 문서 역할의 구체적 사유·근거 확인, 전체 기관 모델/공식 기대값·정책 QA, 승인 범위 활성화/기존 데이터, 운영 검수·DRAFT·복구 브라우저 검증이다. 엔진6/profile17/본문16기관/형식3/catalog참조24·기대값0을 전체 지원 완료로 보지 않는다. 상세는 [추출기 기록](announcement-extractor-text-order-v1.0.1-2026-09-15.md)과 [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)이다.

### 2026-09-15 11:39 KST — 추출기1.0.1 텍스트 순서·품질 보완

- P3 진행, 전체9Gate/ATT62 **Not ready**다. PDF 미사용 XObject의 과도한 PARTIAL_TEXT, 인라인 이미지/누락된 그리기 대상의 COMPLETE_TEXT 오판, HWPX 중첩 부모/자식 텍스트의 순서 역전을 재현하고 수정했다. HWPX는 실제 연속 구간을 각각 독립 근거로 보존하므로 중첩 자체를 누락으로 보지 않는다. 이미지/OLE/수식·미지원·UNKNOWN을 일괄 정상 처리하지 않는다. [상세 변경·검증](announcement-extractor-text-order-v1.0.1-2026-09-15.md)을 따른다.
- 추출기/JAR/IPC/서버 실행 버전1.0.1, 새 코드·런타임 지문을 요구한다. 과거 추출/QA 근거·정책은 재사용하지 않는다. 기존 v1/v2 필드·V1~V83/DB/정책·운영 데이터는 변경하지 않았다. 새 엔진·기관 모델 추가는 아니며6엔진/17프로필/전용본문16기관/추출3형식, catalog참조24/기대값0이다.
- 버전 고정 fixture95실패를 수정한 뒤 root2636=2374통과/262조건부 생략/실패0이다. 중간 HWPX source/class 시각 불일치2건은 최종 재컴파일로 해소했다. 최종42초 build 성공: extractor36/36·패키징20/20, root/bootJar/probe UP-TO-DATE. Node24통과/2 Linux 생략, 사용한 단발 Node/JVM 종료·사용자 output 보존이다.
- 직전0b8aff1 Linux34920332435는 success이며 새 변경의 동일 SHA 증거가 아니다. 이번 운영 배포/실파일 재검증/브라우저/활성화/기존 데이터 적용은 미실행이다. 양평의 PARTIAL_TEXT가 전부 해소됐다고 주장하지 않는다. 다음은 새 SHA Linux→운영 설치 확인→공식 worker/DB/API 효과 대조다. 전체 수집처·정책 기대값·승인된 활성화/기존 데이터/업무 E2E도 계속 남아 있다.

### 2026-09-15 11시대 — 서버 공식 worker 경로와 운영 읽기 전용 화면

- 최종 SSM `31cf27d7-cf0c-49f7-badd-10ffc1eeefcb` Success, 양평 고정3건/3통과·생략0이다. BODY2건 AVAILABLE, HWPX11,398자/PDF4,195자 실제 추출→worker→임시 DB 텍스트→API 전체 필드/블록/종합 상태를 대조했다. JPG미지원 분모 보존·TITLE 제외 요청0·자동 confirmation/link0·lease/원본/DB/전송 자원 정리·운영 DB 쓰기0·설치 JAR 불변이다. 두 문서는 PARTIAL_TEXT/UNKNOWN, TECHNICAL_EXCEPTION·REVIEW_REQUIRED이므로 정상 분석 완료로 승격하지 않는다. 전체 Provider/정책 기대값/활성화/기존 데이터/업무 E2E는 남아 있다.
- 고정 공식3건 JUnit을 별도 probe JAR로 실행하는 경로를 추가했다. 설치 웹 JAR와 app 전체 코드 지문을 대조하고 기존 runtime/JUnit lifecycle/timeout/실제 추출기를 재사용한다. 웹/불변 합성 QA JAR/DB/migration/API는 변경하지 않는다. 상세는 [공식 worker 검증](announcement-official-worker-db-api-qa-2026-09-15.md)이다.
- 운영 관리자 실제 브라우저: 전체2945건/페이지20건에서 다음 페이지 후 browser back으로 전체 상태·page1이 복원됐다. 보은 원문1건 첨부 상세는 기록 없음≠첨부 없음 안내, worker 비활성 사유, 수집 예약/검수 확인/DRAFT 버튼 차단을 표시했다. 해당 경로 콘솔 오류·경고0, 확인한360px 구간 clientWidth=scrollWidth345로 가로 넘침0, 렌더링 캡처 후 viewport를 복원했다. 실제 수집/확인/DRAFT write는 실행하지 않았다. 전체 디바이스·역할/업무 E2E 완료가 아니다.
- 로컬 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1`은4분38초 성공. root2633=2371통과/262조건부 생략/실패0, 독립 실행기·패키지20건 통과. extractor/bootJar는 UP-TO-DATE이며 신규 실제 실행 성공으로 합산하지 않는다. 후속 예외 metadata 진단 표적 검증은 별도다.

### 2026-09-15 10시대 — root 인증 복구 / V83 실제 배포 / 운영 파일 QA

- 전체 Gate0~8/ATT-001~062는 **Not ready**, goal ACTIVE다. 이번 회차는 운영 반영의 progress다. 제목→정제 본문→실제 첨부 텍스트→관리자 최종 검증을 유지하며, 배포 성공을 상시 수집·전체 파일·정책·기존 데이터 완료로 바꾸지 않는다.
- 사용자 요청에 따라 서울 리전의 default root 세션을 재인증했고 실제 STS 계정과 저장소 배포 대상의 일치를 확인했다. Windows의 현재 유효 신뢰 루트를 프로세스 범위에만 전달하여 TLS 검증을 유지했다. IAM/보안그룹/공개 포트는 변경하지 않았다. GitHub OIDC 진단34915789676의 SSM SendCommand 권한 거부와 root 진단 성공을 구분한다.
- 동일 SHA `476c8f722e30464ff7c903b5519d86a4be1ad4c8`의 [Linux 실행37](https://github.com/FrostyCityMan/saneB/actions/runs/34915271732)은 success다. root2631=2368통과/263조건부 생략, 별도 extractor25·패키지20·job192·migration17·worker12·runtime1·부모2·원래 Flyway3은 실패/생략0이다. 독립221/221·Node241/설치12도 통과했다. 공식 파일 관측과 공식 worker 시험은 opt-in 생략이며 성공으로 계산하지 않는다.
- [운영 배포](https://github.com/FrostyCityMan/saneB/actions/runs/34916976535), CodeDeploy `d-CTAGI7DTK`는 success다. 실제 설치 JAR SHA256 `16a1bb74a13e7d88c180fe6f3eb98e97c105c8362592f9c96de572890523f437`, JAR/DB V83, migration 실패0, 새 worker class·같은 JAR 지문의 QA 패키지/추출기·실행 경로 일치, systemd active/localhost health UP을 SSM으로 확인했다. Windows 로컬 JAR 지문과 Linux 배포 JAR 지문은 서로 다르므로 혼용하지 않는다.
- 공개 파일 진단4건은 예상 동작을 통과했다. PDF는 OCR_REQUIRED/0자와 PARTIAL_TEXT/31,498자, HWP는 COMPLETE_TEXT/565자, HWPX는 COMPLETE_TEXT/2,413자다. 앞의 PDF2건은 자동 분석 완료가 아니다. BUILD_MACHINE_DRAFT_SNAPSHOT·단일 선택 파일 진단이고 공고 전체 첨부/본문/자동 역할/worker 영속 저장 검증이 아니다. 원본 정리4/4·해당 QA의 운영 DB 쓰기0이다.
- 배포 전후 첨부 정책/ACTIVE 정책/set/file/extraction/job/batch는 모두0건이다. 첨부 worker·정책 QA·Provider QA 환경변수는 미설정(코드 기본false)이며 게시/ENFORCE/기존 데이터 재처리를 실행하지 않았다. 기존 지자체 활성 대상223/목록 parser41종/저장 원문2,945건을 확인했고 API key2종은 여전히 실행 환경에 없다. 원문 전체 건수는 재처리 승인 범위가 아니다.
- 운영 서버 독립 임시 DB 계약은 Windows JSON ASCII escape 수정 후 SSM `d10887c4-488e-47e0-a6d7-e3413a9e3d23` Success로 완료했다. 합성221/221·실패/생략/미실행/container 실패0, 정리 성공, 운영 DB 미사용·설치 JAR 불변이다. 최초 요청 단계 실패와 구분하며 운영 DB나 격리 한도를 변경하지 않았다.
- HTTP 진행 승인 후 관리자 로그인·첨부 대기열 실제 조회를 확인했다. 준비0건/전체2,945건·1/148페이지, 표시20건 모두3단계 미적용이고 콘솔 경고/오류0건이었다. 비로그인 진입 차단도 확인했다. 실제 검수·DRAFT·역할별 E2E/반응형은 미완료다. 서비스 ingress에는 Nginx/ALB가 없고8080만 열려 있으며 네트워크를 변경하지 않았다.
- 상세 운영 근거는 [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)에 추가했다. 브랜치/원격은476c8f7이며 output Word2개를 보존했다. 공식 기대값·나머지 모델/형식·정책 QA·승인된 전체 기존 데이터·운영 관리자 E2E를 계속 진행해야 한다.

### 2026-09-15 09:45 KST — 공식 파일 worker·DB·API 검증 경로 / CI 외부 접근 차단

- P3 진행, 전체 Gate0~8/ATT-001~062는 **Not ready**, goal ACTIVE다. `제목 1차 → 정제 본문 2차 → 실제 첨부 텍스트 3차 → 최종 관리자 검증`을 유지한다. 이번 회차는 검증 연결 구현의 progress이며 운영 완료가 아니다.
- [Linux 실행36](https://github.com/FrostyCityMan/saneB/actions/runs/34912804386), `0f34f9eeaf805ac604ca4218ee89c68a6caee159`는 공식 관측 failure다. root2628=2366통과/262조건부 생략, 별도 extractor25·패키지20·job192·migration17·worker12·runtime1·부모2·원래 Flyway3은 실패/생략0이다. 독립221/221, Node241/설치12, 부모 정리도 통과했지만 공식 파일 성공으로 계산하지 않는다.
- 양평312241/311846은 각각 BODY2시도 TIMEOUT 뒤 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT이다. 각각 요청 예약3/원본 정리true, 다운로드/추출 미실행이다. 제목 제외311507은 TITLE_EXCLUDED_NOT_FETCHED/후속 요청0이다. 태백 실행30/31 뒤 양평에서도 CI 접근 실패가 반복되어 동일 조건 즉시 재시도는 중단했다. 네트워크/리전/원격 차단 중 실제 원인은 미확정이다. WSL에는 docker-desktop만 있고 별도 개발 Linux 배포판은 없다.
- 새 공식 worker 시험은 실제 BODY·분류 persistence→현재 버전 job 예약→실제 worker/profile/download gateway/Linux 격리 추출/역할1.0.2→임시 PG→API/근거 블록을 연결한다. TITLE 제외 원문/요청0, BODY A/B/실패 후 첨부 계속, 미지원 분모/자동 검수·공고 link 금지, 원본/lease 정리를 확인한다. 공개 텍스트/파일명/URL/원격 예외는 로그·artifact에 남기지 않는다. 운영 규칙/ENFORCE가 아닌 소유 임시 DB fixture임을 구분한다. [설계·실행 기록](announcement-official-worker-db-api-qa-2026-09-15.md)을 따른다.
- 최초 컴파일은 추출기 패키지 import 누락에 따른 오류3건으로 실패했고 수정 후 준비 계약·기존 관측 계약·workflow 표적 검증은52초 성공했다. 합성 BODY의 실제 분류 저장→현재 버전 job 예약이 검증되었다. 공식 파일 worker 자체 실행은 외부 접근 차단으로 미실행이며 새 CI 단계는 수동 opt-in/defaultfalse다. 일반 push에서 같은 공식 사이트를 재요청하지 않는다.
- 전체 로컬 회귀·job192·원래 Flyway3·bootJar/패키지 검증은16분3초에 성공했다. root2631=2369통과/262조건부 생략/실패0, 별도 job192/192·패키지20/20·Flyway3/3이다. 새 DB 준비1건과 workflow10건도 root 실행에서 통과했다. Node20통과/2 Linux 전용 생략/실패0이며 사용한 Node/JVM/임시 PG는 종료했다. production Java/API/UI/migration은 바꾸지 않았고 bootJar/추출기 시험은 UP-TO-DATE다. JAR hash `19b1d2fb08bde9e29a9523e41e2a8352a3ad8e68bbf5e98b0a55826867f90d40`, V83/엔진6/profile17/전용 BODY16기관/형식3/catalog 참조24·기대값0을 유지한다. 현재 변경의 Linux 실행은 다음 SHA에서 별도 확인한다.
- AWS 재인증 응답, 공식 파일·정책 기대값·미연결 기관, 현 SHA 운영 배포·승인된 기존 데이터 전체 적용·운영 브라우저가 남았다. 이번 운영 접속/쓰기/배포/브라우저 검증은 미실행이며 사용자 Word2개를 보존했다.

### 2026-09-15 09:20 KST — 양평 실파일 세 단계 관측 연결 / 제목 제외 표본 보존

- P3에서 기존 태백1건에 고정된 관측기를 재사용해 명시적인 양평3건 그룹을 연결했다. 실제 공식 제목3개를 확인하고 임시 DB DRAFT로 사전 판정했다.312241·311846은 COMBINATION_MATCHED,311507은 TITLE_COMBINATION_NOT_MATCHED다. 마지막 표본은 본문/첨부 예산 예약 전에 TITLE_EXCLUDED_NOT_FETCHED로 끝내며 외부 요청0이다. 과거 BODY/파일 발견 성공3건을 실제 필터 통과3건으로 세지 않는다.
- 기존 profile·strict TLS/요청 허용·격리 추출기를 유지하며 새 엔진/seed/DDL/API/UI/운영 정책을 추가하지 않는다. 선택3건 상한132요청/240MiB, 실제 요청량은 metadata에서 별도 대조한다. 제목 입력은 고정 공식 표본, 규칙은 임시 DB DRAFT임을 명시한다. 본문 A/B·부족 후에도 첨부 분석을 계속하고, JPG·부분/OCR·실패·미실행은 전체 텍스트 완전성 false로 남긴다. UNKNOWN 역할이나 관측 성공을 정책 QA로 승격하지 않는다.
- 표적20건42초 통과 후 전체 회귀3분59초 성공: root2628=2367통과/261조건부 생략/실패0, 별도 패키지20/20·원래 임시 Flyway3/3이다. 이후 JUnit source factory의 오버로드 혼동을 없앤 최종 표적20건이39초에 다시 통과했다. Node20통과/2 Linux 전용 생략, production JAR/추출기/독립 패키지 UP-TO-DATE를 분리한다. 최종 웹 JAR SHA256 `19b1d2fb08bde9e29a9523e41e2a8352a3ad8e68bbf5e98b0a55826867f90d40`는 직전 커밋과 동일하다. 직접 사용한 프로세스는 종료했고 사용자 output Word2개를 보존했다.
- 직전 정부24 SHA `cda8c7ae56a54b545990f9fe891b716997bd27f1`의 [Linux 실행35](https://github.com/FrostyCityMan/saneB/actions/runs/34911583782)는 success다. root2622=2360통과/262조건부 생략, 별도 extractor25·패키지20·job192·migration17·worker12·runtime1·부모2·원래 Flyway3은 실패/생략0이다. 독립221/221, Node241/설치12와 정책 부모 정리를 확인했다. 해당 artifact는 `build/qa-results/run-34911583782`에 있다. 양평 실제 파일의 새 관측/운영 배포 성공은 아니다.
- 현재 Docker Linux pipe 없음이 재확인됐으며 로컬 비격리 추출은 하지 않았다. [새 양평 관측 설계·명령·대상](announcement-yangpyeong-three-stage-observation-2026-09-15.md)에 현재 범위와 한계를 기록했다. 다음은 명시한 Linux 실행의 실제 BODY/HWPX/PDF/미지원/제목 제외 결과 확인이다. 운영 AWS 재인증·정부24 API·나머지 기관·전체 기대값·운영 적용/기존 데이터/브라우저는 계속 미완료다. 첨부6엔진/17profile/전용BODY16기관/3형식·catalog24참조/기대값0, V83를 유지한다. 전체 Gate/ATT **Not ready**, goal ACTIVE다.

### 2026-09-15 09:03 KST — 정부24 공식 목록 요청 계약 보완 / 첨부 미지원 유지

- P3에서 정부24 공식 목록 API의 페이지/검색 규격과 기존 코드 불일치를 확인하고 보완했다. 공식 호스트의 고정 목록 경로, JSON 기본 응답, page/perPage/서비스명 LIKE를 사용한다. 고정 검색 계획의 대상·지원형태를 별도 전달하되, 어댑터는 대상 검색 후보를 조합 조건으로 미리 버리지 않는다. TITLE A 예외를 보존하고 조합·A/B 판정은 기존 공통 분류기가 원문 저장 전에 처리한다. 기존 제공자의 조합 문자열·라운드로빈 계약은 유지한다. [설계·검증·외부 근거](announcement-gov24-official-list-contract-2026-09-15.md)에 요청 상한과 첫 페이지 밖 누락 가능성을 명시했다.
- 공식 API에서 동일 의미가 없는 지역·카테고리·신청기간은 오류로 설명한다. 새 정부24 자동 배치의 임의 신청기간은 null로 만들고, 이미 승인된 요청·기존 중계 API/기업마당 동작은 변경하지 않는다. 오류 객체/잘못된 페이지·건수/ID·제목을 성공 0건으로 처리하지 않는다. 전화문의 별칭만 추가하며 기존 BODY 선택과 첨부 metadata 제외/hash를 유지한다. TITLE→BODY→첨부→최종 관리자 검증 및 자동 ACTIVE 금지에 변경 없다.
- 첫 표적65건 중1건은 공백 정제 기대값 차이로 실패했고 원인을 확인한 뒤65/65 통과했다. 이후 커밋 전 TITLE A 후보 보존 검토/보완과 회귀1건을 추가하고 전체를 다시 실행했다. 최종4분3초 성공: root2622=2361통과/261조건부 생략/실패0, 정부24 어댑터40/40, 공통 TITLE gate2/2, 패키지20/20, 별도 임시 PG Flyway3/3이다. Node20통과/2 Linux 전용 생략, extractor UP-TO-DATE를 분리한다. 최종 JAR SHA256 `19b1d2fb08bde9e29a9523e41e2a8352a3ad8e68bbf5e98b0a55826867f90d40`, 기존 migration 변경0/V83 유지다. 앞선4분13초/root2621은 최종 코드 증거가 아니다. 사용한 Node/JVM/임시 PG가 종료됐고 output Word2개를 보존했다.
- 직전 양평 SHA `3b88312d7cdbb609208f51adc11bbeb53946505e`의 [Linux 실행34](https://github.com/FrostyCityMan/saneB/actions/runs/34909529592)는 success다. root2580=2318통과/262조건부 생략, 별도 extractor25·패키지20·job192·migration17·worker12·runtime1·부모2·원래 Flyway3은 생략/실패0이다. 독립221/221, 정리2종, Node241/설치12 통과다. 이 결과를 정부24 변경 이후 동일 SHA나 공식 파일 재관측·운영 배포 증거로 쓰지 않는다. 증거는 `build/qa-results/run-34909529592`에 있다.
- 로컬 정부24 URL/key 환경변수는 없음으로 확인했고 실제 인증 API 호출은 하지 않았다. 정부24 첨부 profile은 여전히 미등록/PROFILE_REQUIRED이며 목록의 첨부 필드 부재를 NO_FILES로 만들지 않는다. 첨부 엔진6/profile17/전용 BODY16기관/추출3형식, catalog 참조24/기대값0은 변함없다. 실제 API·공식 파일 기대값·나머지 기관·현 SHA 운영 배포·승인된 전체 데이터·운영 브라우저가 남아 있다. AWS 재인증 응답 대기이며 이번 운영 조회/DB/설정/데이터/브라우저 쓰기·검증은 미실행이다. 전체 Gate/ATT **Not ready**, goal ACTIVE, 이번 회차는 구현·회귀 검증의 **progress**다.

### 2026-09-15 — 양평군 본문·첨부 연결 / 미지원 이미지 분모 보존

- 직전 옥천 증분은 실제 구현·공개 검증·커밋/푸시로 진전이었다. [Linux 실행33](https://github.com/FrostyCityMan/saneB/actions/runs/34908203781), `5b24bb8958039c79fa96e0d907b76adef9403bef`는 success로 끝났다. root2568=2308통과/260조건부 생략/실패0, 별도 extractor25·패키지20·job192·migration17·worker12·runtime1·부모2·원래 Flyway3은 실패/생략0이다. 독립221/221·정리2종·Node241/설치12도 통과했다. 공식 외부 관측/운영 배포는 실행하지 않았으며 이번 양평 변경의 같은 SHA 증거가 아니다.
- 양평 V61/V62의 고정 기관/게시판/parser에 BODY·첨부를 연결했다. 자체 `내용` 표제 옆 셀과 UUID/게시판/확장자 고정 미리보기 구조만 확인하고 미리보기는 요청하지 않는다. 제목→정제 본문→실제 첨부 텍스트→최종 관리자 검증을 유지한다. 첨부 엔진6/profile17(지자체16)/전용 BODY16기관/추출 형식3이며 미연결 기관도 분모에 남긴다.
- 표적199건은43초에 전부 통과했다. 공식6시험은23초/생략·실패0이다. BODY3건 AVAILABLE/시도1/redirect0, 전체 발견5개 중 HWPX1/PDF2의 다운로드/signature/Content-Disposition을 확인하고 JPG/PNG2개는 다운로드하지 않은 미지원 상태로 보존했다. 혼합2공고는 DISCOVERY_WITH_UNSUPPORTED_FILES/allFilesDownloaded=false다. 첨부6요청/예약1,486,696bytes/원본 정리3/3/DB 쓰기0, BODY 별도3요청이다. 이미지 또는 실패를 분모에서 제거하지 않으며 실제 텍스트/역할·정상 다중첨부·worker/운영 성공으로 계산하지 않는다.
- catalog schema2 참조24/기대값0/중복0을 확인했고 정책 QA 성공은 만들지 않았다. 기존 MIME/UTF-8 계약을 통과했으며 새 MIME 예외를 추가하지 않았다. 이전 BBS profile hash를 현재 실행에 재사용하지 않는다. 전체 회귀·임시 Flyway 검증은4분27초 성공: root2580=2319통과/261조건부 생략/실패0, 패키지20/20·Flyway3/3이다. extractor/bootJar는 UP-TO-DATE이며 production JAR SHA256은 `9a447b6adce7ce0372d318118af7717848991c8162822610d159946cff47c902`다. 사용한 Node/Gradle/임시 PostgreSQL은 종료했다. [양평 모델 기록](announcement-yangpyeong-bbs-profile-2026-09-15.md)에 설계·명령·표본·한계를 기록했다.
- AWS 재인증 응답 대기, 공식 파일 기대값/미연결 기관·현 SHA 운영 배포·승인 범위 기존 데이터 적용·운영 브라우저가 남아 있다. 이번 운영 조회/쓰기/브라우저는 미실행이고 사용자 Word2개를 보존한다. 전체 Gate/ATT **Not ready**, goal ACTIVE이며 이번 회차는 실제 기관 모델 구현의 **progress**다.

### 2026-09-15 — 옥천군 본문·첨부 연결 / 원래 Flyway3건 Linux 통과

- 직전 검증 연결 회차는 progress다. [Linux 실행32](https://github.com/FrostyCityMan/saneB/actions/runs/34906440965), `d18216ed5381f97cc1a8ebeeac209381e4c85ffb`는 success로 끝났다. root2556=2298통과/258조건부 생략/실패0, extractor25·패키지20·job192·migration17·worker12·runtime1·부모2 및 원래 `flywayIntegrationTest`3건은 각각 생략/실패0이다. 독립221/221·정리2종·Node241/설치12도 통과했다. 공식 외부 파일 관측/운영 배포는 실행하지 않았으며 이번 옥천 변경의 같은 SHA 증거가 아니다.
- 제목→정제 본문→실제 첨부 텍스트→최종 관리자 검증을 유지하며 옥천군 V61/V62의 기관/게시판/parser 고정 모델을 추가했다. 기존 BBS 엔진을 재사용하여 첨부 엔진6/등록 profile16(지자체15)/전용 BODY15기관/추출 형식3이다. 전체 대상 분모와 미결합 기관을 유지한다.
- 옥천 공식 지원 공고3건의 BODY는 AVAILABLE/시도1/redirect0이다. 전체 HWPX3파일 발견·다운로드·signature/Content-Disposition 검증, 임시 원본 정리3/3에 성공했다. 첨부 상세3+파일3=6요청/예약2,016,015bytes, BODY 별도3요청이며 DB 쓰기0이다. Linux 텍스트 추출·역할·worker·운영 후보 성공이 아니다. 세 공고 모두 단일 첨부여서 정상 다중첨부 증거도 남아 있다.
- 기관 고정 헤더 설정과 중첩 제목 경계를 보완했다. 초기 표적177건의2실패 중 횡성 세션 예외가 옥천으로 전파되는 경계는 횡성 host에만 한정했고 기존 profile 건수도 갱신했다. 표적177/공식6건은 생략·실패0/47초 통과다. 기존 기관 MIME/UTF-8 설정은 보존하되 BBS 공유 코드 hash는 바뀌므로 이전 QA를 재사용하지 않는다. catalog는 참조21/기대값0이며 과거 schema1과 자동 활성화 금지를 유지한다.
- 첫 전체 회귀3분29초는 오래된 snapshot 참조 건수18 assertion1건에서 실패했다. 기대값0·QA false 보호는 보존하고 실제21건만 반영한 재실행은4분23초 성공했다. root2568=2309통과/259조건부 생략/실패0, 패키지20/20, 원래 임시 Flyway3/3이다. extractor/bootJar는 UP-TO-DATE이며 JAR SHA256은 `e2acf6573847a4af2e183831b9b4f1c666e29bc56426faabeed77954e0956cdf`다. 로컬 생략을 실제 Linux/운영 통과로 세지 않는다. 단발 Node/Gradle/임시 PostgreSQL을 종료했다. 상세 근거·명령은 [옥천 모델 기록](announcement-okcheon-bbs-profile-2026-09-15.md)이다.
- 운영 AWS 재인증 응답 대기, 공식 전체 파일 기대값/미연결 기관·현 SHA 운영 배포·승인 범위 기존 데이터 적용·운영 브라우저가 남아 있다. 운영 조회/쓰기/브라우저는 이번 회차 미실행이고 사용자 Word2개를 보존한다. 전체 Gate/ATT **Not ready**, goal ACTIVE이며 이번 회차는 실제 기관 모델 구현의 **progress**다.

### 2026-09-15 — 필수 원래 Flyway3시험 실행·Linux 공식 접근 실패 구분

- 직전 회차는 진단 구현·검증·커밋의 progress다. [Linux 실행31](https://github.com/FrostyCityMan/saneB/actions/runs/34904550080), `0625b13fb25ddf7001f5e5848cb46087ae01edfc`는 전체 failure로 끝났다. root2556=2298통과/258생략, 별도 extractor25·패키지20·job192·migration17·worker12·runtime1·부모2·독립221/221·정리2종·Node241/설치12는 통과했다. 공식 관측은 BODY 2회 TIMEOUT 뒤 DETAIL_DISCOVERY도 TRANSPORT_TIMEOUT이었다. 원문 정리true/운영 쓰기0이고 파일은 미실행이다.
- Windows의 기존 BBS 본문3사례는 기본 Java 신뢰 설정에서 NETWORK_ERROR였으나 저장소의 Windows-ROOT 인자를 명시하면3/3 AVAILABLE·시도1·redirect0을 확인했다. 전체18초/HTTP시험2.088초다. 로컬 BODY를 Linux 전체 파일 성공으로 바꾸지 않는다. 같은 CI 공식 표본 즉시 반복을 중단하고 AWS 재인증 또는 외부 접근 상태 변화 뒤 재확인한다. catalog 참조18/기대값0을 유지한다.
- 원래 goal Gate6의 `flywayIntegrationTest`3건이 첨부 migration17건과 별도임을 확인했다. 기존 테스트에 명시적 임시 PostgreSQL 모드를 추가하여 운영 DB 자격증명을 사용하지 않고 Spring Boot Flyway/Mapper/golden gate 및 V70 제외 원문 정리를 실행했다. 초기 DataSource 선택 오류와 빈 DB/legacy fixture 차이를 해결하고 실제 V62→5개 숨김 초안→현재 migration, primary5/지원형태 추정0/checksum·pending0을 검증했다. 이전 migration과 원래 assertion은 보존했다.
- 전용3시험은41초/생략·실패0, Node 필수 보고서 검증10/10 통과다. 전체 회귀+Flyway 전용 task4분26초 성공: root2556=2299통과/257생략/실패0, 패키지20/20, Flyway3/3이다. 이후 부모 DB 환경 차단·fallback loopback·소유 port 확인을 포함한 최종 전용3시험도44초에 통과했다. extractor/bootJar/설치는 UP-TO-DATE며 production JAR hash `868e0aaaf9fc656985c5e9facd12435a7ec201ebb11d28f68c241ed16e4e769c`는 같다. Linux workflow에도 명령과3건 누락/생략/일부 실행 차단을 추가했으며 새 SHA Linux 검증은 남아 있다. 상세는 [원래 Flyway 임시 검증](announcement-full-flyway-ephemeral-qa-2026-09-15.md)이다. 변경은 시험/검증 설정/문서이며 production Java·DB/API/UI·V1~V83·운영 정책·데이터는 바꾸지 않았다.
- AWS 재인증 응답 대기, 공식 파일 기대값/나머지 기관 모델·실제 worker 운영 배포·승인된 기존 데이터 전체 적용·운영 브라우저는 잔여다. 이번 운영 조회·변경·브라우저는 미실행이고 사용자 Word2개를 보존한다. P3 진행·전체 Gate **Not ready**, goal ACTIVE이며 이번 회차는 필수 검증 공백 해소의 **progress**다.

### 2026-09-15 — 실제 BODY 실패 분리·고정 전체 파일 실행기 연결 준비

- 직전 역할1.0.2 증분의 [Linux 실행30](https://github.com/FrostyCityMan/saneB/actions/runs/34902614936), `72a78c89149267cdacffe2d0ce73c9a5035294b4`는 **전체 실패**다. root2550=2293통과/257조건부 생략, 별도 extractor25·패키지20·job192·migration17·worker12·runtime1·부모2, 독립221/221와 정리2종, Node241/241·설치12/12는 통과했다. 새 FORM 표식의 실제 Linux worker→DB→v2 API 경로는 이 SHA에서 확인됐다.
- 공식 태백 관측은 제목 조합 통과 후 BODY가 2회 TIMEOUT/FETCH_FAILED로 중단됐다. 첨부 발견/다운로드/추출은 미실행이며 원본 정리true/운영 쓰기0이다. 이전 역할/프로필 관측을 현재 값으로 추정하여 catalog에 넣지 않았다. 참조18/실행 기대값0, 정상 공고/전체 coverage 미완료를 그대로 둔다.
- 새 증분은 관측 시험의 BODY 실패를 보존하면서 남은 예산으로 첨부 진단을 수집하고 마지막 BODY 완전성에서 실패하는 구조다. TITLE 제외 후 요청 금지, BODY A/B가 첨부를 끊지 않음, UNKNOWN/혼합 검수·자동 활성화 금지를 유지한다. 운영 수집기/재시도/분류 규칙의 변경은 없다.
- 별도 opt-in 고정 공고 시험에서 production `AttachmentProviderQaCaseExecutor`, 현재 runtime, catalog, 임시 PostgreSQL의 미삭제 seed 전체+전국2채널을 연결했다. 요청·용량·시간·단일 자원·정리를 제한하고 전체 정책 QA false를 고정한다. BODY 관측과 단건 첨부 계약의 범위를 분리하며, 기대값0인 현재 실제 고정 계약 실행은 켜지 않는다. 상세는 [고정 기대값 연결](announcement-taebaek-fixed-qa-expectation-2026-09-15.md)이다.
- 표적116건/생략0/37초 통과, 전체 회귀3분27초 성공이다. root2556=2299통과/257조건부 생략/실패0, QA 패키지20/20이며 extractor/bootJar/설치 task는 UP-TO-DATE다. production JAR SHA256 `868e0aaaf9fc656985c5e9facd12435a7ec201ebb11d28f68c241ed16e4e769c`는 그대로다. Node 검사로 catalog18참조/기대값0 보존을 확인했다. V1~V83·v1/API shape·UI·production Java·운영 DB·정책·기존 데이터를 변경하지 않았다. AWS 재인증 응답은 대기 상태이며 운영 조회/배포/브라우저는 이번 회차 미실행이다. 사용자 Word2개를 보존한다. P3 진행·전체 Gate **Not ready**, goal ACTIVE이며 이번 회차는 실패 분리와 검증 연결 구현의 **progress**다.

### 2026-09-15 07:07 KST — 실제 관측 기반 양식 역할 표식 보완

- 직전 보은 증분은 구현·실파일 signature·커밋/푸시로 진전이었다. 이번에는 태백 실제 관측의 둘째 파일에서 확인한 양식 제목·독립 입력 표제·서명 표식과 기존 정규식의 차이를 분석했다. 첫 파일의 혼합 문서 UNKNOWN은 정당한 검수 사유로 보존한다.
- `document-role-1.0.2`에 콜론 없는 신청인/사업자등록번호/성명 표제와 수평 공백, 괄호형 `(서명 또는 인)` 종료 표식을 추가했다. 초기 제목·입력·서명 전부와 같은 파일/유효 block/완전 추출을 요구한다. 설명 문장·부분 일치·줄/block 분리·누락·혼합·불완전/OCR은 자동 역할 확정으로 만들지 않는다. 제목→정제 BODY→실제 첨부 텍스트→최종 관리자 검증과 A/B 정책은 그대로다.
- 이전1.0.1 정책/실행/근거를 재해석하지 않으며 null legacy·MANUAL/PROFILE 보존을 검증했다. V82의 기존 필드·규칙 코드/3개 FORM 근거를 재사용해 새 migration·v1/v2 shape·UI 변경은 없다. 새 정책 초안 수정·QA·게시/ENFORCE/기존 데이터 적용은 별도 승인 범위다.
- 표적122건/생략0/49초 통과, 첫 전체 회귀는 옛 관측 시험의 UNKNOWN 기대값1건으로 실패했다. 새 명세의 FORM/버전 검증으로 고치고 원문/canary/locator 비노출과 위치 검증을 유지했다. 최종 전체4분6초 성공: root2550=2294통과/256조건부 생략/실패0, QA 패키지20/20. bootJar SHA256 `868e0aaaf9fc656985c5e9facd12435a7ec201ebb11d28f68c241ed16e4e769c`다. 마지막 bootJar/extractor시험은 UP-TO-DATE이며 이번 실제 재실행으로 합산하지 않는다.
- 새 Linux worker 통합 시험은 합성 HWPX→실제 격리 parser→FORM 근거 DB→v2 API 경로를 추가했다. 파일명 공고/양식 내용 구분, 본문 없는 FORM 키워드의 주된 근거 승격 금지, base 보존/link0/정리를 검증한다. 로컬에서는 Linux 조건으로 생략됐으며 현재 같은 SHA 실제 실행 통과를 주장하지 않는다.
- [Linux 실행29](https://github.com/FrostyCityMan/saneB/actions/runs/34901307963), `69cf469e45ecb6e9339140fd080283fe1edf32e4`는 root2518=2262통과/256생략, 별도 extractor25·패키지20·실제 job192·migration17·worker11·runtime1·부모2·독립220/220 및 정리 성공, Node241/241·설치12/12다. 공식 관측은 생략됐고 새1.0.2 규칙의 근거가 아니다.
- 다음은 새 SHA의 Linux DB/worker 및 태백 같은1공고 전체2파일 재관측이다. FORM 개선 여부·첫 파일 혼합 검수 유지·원문 정리를 확인한 뒤 실제 기대값을 검토한다. 현재 profile15/전용 BODY14/엔진6/추출 형식3, catalog18참조/실행 기대값0이다. [설계·시험 상세](announcement-form-role-markers-2026-09-15.md)를 따른다. 운영/AWS 재인증/전체 Provider·형식 기대값/승인된 배치/운영 브라우저는 미완료이며 전체 Gate/ATT **Not ready**, goal ACTIVE, 이번 회차는 **progress**다.

### 2026-09-15 06:52 KST — 보은군 본문·첨부 모델 및 태백 Linux 세 단계 실증

- 보은군 고정 `LGS-000139/HEURISTIC_NOTICE`에 전용 BODY 정제와 `LOCAL_BOEUN_BBS_V1`을 추가했다. 제목→본문→실제 첨부 텍스트→관리자 최종 검증 순서, 제목 제외·BODY A/B 후 첨부 진행·UNKNOWN·자동 ACTIVE 금지를 유지한다. 현재 코드 엔진6/등록 profile15(지자체14)/전용 BODY14기관/추출 형식3이다. 새 DB migration·v1 API·UI·운영 규칙 변경은 없다.
- 실제 보은3공고의 BODY3건은 AVAILABLE/시도1/redirect0, 전체 HWPX2/PDF1은 발견·다운로드·signature를 통과했다.6시험/생략0/실패0,28초다. 첨부 검증6요청/예약1,100,217bytes/원본 정리3/3, BODY는 별도3요청이다. 초기 MIME 오류와 실측 후 기관 한정 보완은 [보은 모델 기록](announcement-boeun-bbs-profile-2026-09-15.md)에 있다. 단일 첨부3표본이므로 정상 다중첨부 Gate 성공이 아니며 실제 텍스트/역할 검증도 아직 남아 있다.
- schema2 catalog는 보은 REFERENCE_ONLY3건을 추가해18참조/실행 기대값0이다. 이전 schema1/V1~V83은 보존했다. 실행 기대값 없이 QA PASSED를 만들지 않는다. 기존 운영223개/결합11·미결합212 snapshot을 새 코드 등록 수로 갱신하지 않는다.
- 초기 표적167건/30초 성공, MIME/catalog 이후 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1`은3분32초 성공이다. root2518=2263통과/255조건부 생략/실패0, QA 패키지20/20이다. extractor시험은 UP-TO-DATE로 이번 실제 재실행 성공에 합산하지 않는다. bootJar SHA256 `4319896d5f07a5be5dfa0b860fbf404de99e016d8e6d776ca4f25a5fdbfd777e`는 운영 JAR가 아니다.
- [Linux 실행28](https://github.com/FrostyCityMan/saneB/actions/runs/34899577744), `5c6ff8d47e46d16bec2a63ecbe7a53eafe412e0a`는 root2504=2250통과/254생략, 별도 extractor25·패키지20·실제 job192·migration17·worker11·runtime1·부모2·독립220/220 및 정리 성공, Node241/241·설치12/12다. 태백184816 실제 관측1건도 성공했다: 제목 조합→BODY431자→HWPX2개 COMPLETE_TEXT(2041/1994자)→종합 REVIEW_REQUIRED다. 역할 UNKNOWN은 혼합 역할/구조 부족 사유이며 정상 후보로 바꾸지 않았다. 원본 정리true/운영 쓰기0/원문 필드0이다. 이전 SHA 관측을 새 BBS 지문의 검증 근거로 재사용하지 않는다.
- 다음 우선순위는 태백 실측의 역할 미확정 원인을 검토하고, 실제 텍스트 구조와 음성 사례로 검증 가능한 역할/내용 기대값을 마련하는 일이다. 모델 수 증가만으로 검수 감소를 입증하지 않는다. 전체 기관 모델/형식 QA·AWS 재인증·동일 SHA 배포·승인된 기존 데이터 배치·운영 브라우저가 남아 있다. 이번 브라우저/운영 조회·쓰기는 미실행이며 사용자 Word2개는 보존했다. 전체 Gate/ATT **Not ready**, goal ACTIVE, 이번 회차는 **progress**다.

### 2026-09-15 06:33 KST — 태백 고정 표본의 세 단계 실제 관측 경로

- 직전 회차는 QA 관리 화면 구현·커밋·푸시로 진전이었다. 현재 로컬 Docker Linux engine pipe 연결 실패를 확인했다. 격리 추출을 Windows 비격리 실행으로 우회하지 않으며 이전 기업마당 관측 timeout3건을 해소한 것으로 보지 않는다.
- 태백184816 공식 페이지의 제목·본문·첨부2개를 다시 확인하고, 기존 production 제목 분류→BODY client/정제·분류→공식 첨부 발견→전체 파일 Linux 격리 추출/역할 판정→종합 분류를 사용하는 고정 관측을 연결했다. 제목 제외 시 요청0, BODY A/B/정보 부족은 첨부로 진행, 파일 UNKNOWN/부분/실패는 전체 분모에 유지한다. 관리자 최종 검증·DB/API/UI/DRAFT 전환을 수행한 것으로 표시하지 않는다.
- 전체44요청/80MiB/420초를 유지하고 BODY 최대2시도/redirect0/1MiB 응답의2요청/2MiB 상한을 먼저 예약했다. 파일20MiB·최대10개·기존 TLS/SSRF/pinned host/격리 요구는 그대로다. 원본은 finally 정리하며 artifact에는 비식별 metadata만 남긴다. 명시 표식/기본false 입력에서만 고정1공고를 실행하고 운영 쓰기·catalog 자동 승인은 없다.
- 표적14건/44초, 전체 root2504=2251통과/253조건부 생략/실패0·QA 패키지20/20·3분16초 성공이다. 새 실제 관측은 로컬 Windows에서 미실행이며 일반 시험에서 생략했다. extractor/bootJar/설치 task는 UP-TO-DATE다. production JAR는 기존 `fd57b761ceb2dcd7cab381ae3e3db9cb2fb46aced6c927f59ed3a01b0b2402e1`로 유지되며 production Java/DB/API/UI/프로필/migration 변경은 없다.
- [Linux 실행27](https://github.com/FrostyCityMan/saneB/actions/runs/34898209145), `ad0e1307dd0ed1c8cbadb33468b2297ff5ece326`은 전체 성공이다. root2498=2245통과/253생략, 별도 extractor25·패키지20·실제 job192·migration17·worker11·runtime1·부모2·독립220/220 및 정리 통과, Node241/241·설치12/12다. 이 근거는 직전 QA UI SHA이며 이번 태백 관측의 실행 근거가 아니다.
- 상세는 `announcement-bbs-three-stage-observation-2026-09-15.md`다. 새 SHA의 실제 Linux 관측 결과를 받은 뒤 파일 목록/품질/역할·내용 기대값을 검토한다. catalog 참조15/실행 기대값0, 전체 대상 모델·공식 파일·운영 재인증/배포·승인된 전체 배치·운영 브라우저 Gate는 남아 있다. 브라우저/운영 조회·쓰기는 이번 회차에 실행하지 않았고 사용자 Word2개를 보존했다. 전체 Gate/ATT **Not ready**, goal ACTIVE다.

### 2026-09-15 06:18 KST — 수집원 QA 예약·이력·취소 화면 연결

- 제목→정제 BODY→실제 PDF/HWP/HWPX 텍스트→관리자 최종 검증 순서를 유지했다. 새 관리자 업무 화면은 고정 QA 분할의 전체 공고 코드·요청/바이트/시간 예산·전체 기대값 미완료 여부를 확인하고 별도 동의 후 예약한다. 읽기 전용 coverage 화면과 분리하고 기존 Controller/Service/API/CSRF 계약을 재사용했다. 새 DDL·기존 migration·v1 계약·분류 규칙·운영 데이터 변경은 없다.
- ADMIN만 예약/취소, OPERATOR/APPROVER는 조회한다. 최초 유실 이후401/403/409가 와도 예약 키·본문을 바꾸지 않는다. 취소 유실은 최신 실행 GET과 별도 인지 후 현재 상태를 새 검토 기준으로 채택할 수 있지만 원래 요청 성공을 추정하지 않는다. 계획409/OFF와 기존 이력/취소를 분리하고 COMPLETED/PASSED를 전체 QA·정상 공고로 표시하지 않는다.
- Node 신규41/41 및 전체 UI/판정기241/241·실패/생략0, 별도 설치12건은10통과/2 Linux 전용 생략이다. Java 표적79건/bootJar40초 성공, 전체 회귀3분37초 성공이다. root2498=2246통과/252조건부 생략/실패0, QA 패키지20/20. extractor25건·마지막 bootJar는 UP-TO-DATE이며 새 실행으로 세지 않는다. 현재 JAR SHA256 `fd57b761ceb2dcd7cab381ae3e3db9cb2fb46aced6c927f59ed3a01b0b2402e1`는 운영 JAR가 아니다.
- 실제 SSR의 합성 Chrome에서 예약1회→전체 항목→취소1회, 예약 응답503→403→정상 재확인3회/실제 예약1건, 취소 유실→최신 조회→별도 인지를 확인했다. 운영 쓰기/외부 요청/CSRF 거부0, 정상 console 오류/경고0이다. 조회 전용·OFF·계획409·빈 목록 및320~1440px 가로 넘침0을 확인했다. native200/400% 확대는 도구 context 종료로 미확인이며 전체 키보드·보조기술 검증도 남아 있다. 합성 결과는 운영·전체 Provider 성공이 아니다.
- 직전 `1d4be1b5c4e4fbe86bb3b520f93d9f2f2c9ee328` [Linux 실행26](https://github.com/FrostyCityMan/saneB/actions/runs/34895142129)은 전체 성공이다. root2491=2238통과/253생략, 별도 extractor25·패키징20·실제 job192·migration17·worker11·runtime1·부모2·독립220/220 및 정리2종 통과, Node200/200·설치12/12다. 현재 새 예약 화면은 이후 증분이므로 같은 SHA CI 검증이 별도로 필요하다.
- 합성 Chrome/전용 Node 서버 종료 확인, CLI 기록은 ignored build로 이동, 사용자 output/Word2개 보존. 서울 AWS 인증 만료는 새로 확인·해소되지 않았으며 이번 운영 조회/배포/설정/정책/데이터 변경은 없다. 공식 참조15/실행 기대값0, 전체 대상 모델·공식 파일/역할 기대값·운영 배포·승인된 전체 데이터 배치·운영 브라우저는 여전히 필수 잔여다. 전체 Gate/ATT **Not ready**, goal ACTIVE다. 상세는 `announcement-provider-qa-ui-2026-09-15.md`다.

### 2026-09-15 05:47 KST — 수집원별 검증 범위 UI 연결·합성 브라우저 검증

- 제목→정제 BODY→실제 첨부 텍스트→관리자 최종 검증 순서를 유지한 채 전체 target coverage API를 읽기 전용 관리자 화면에 연결했다. 정책 상세에서 선택한 정책으로 진입하며 기관별 연결 상태·참조/실행 기대값·정상3/다중 첨부1·관측/미관측/부족 형식을 구분한다. 계획 준비 완료도 실제 QA 통과가 아니며, 페이지 간 정책·표본·범위/계획 지문 변경은 결과를 섞지 않고 차단한다. 새 DDL·기존 migration·v1 API·분류 규칙·운영 데이터 변경은 없다.
- Node 새46+기존 정책36=82건 표적 통과, SSR/권한/Provider API42건·bootJar37초 통과. 전체 Java2491=2239통과/252조건부 생략/실패0, QA 패키지20/20,3분19초 성공. extractor25건 및 마지막 bootJar는 UP-TO-DATE이며 새 실행으로 세지 않는다. 최종 Node 전체200건/생략0 통과다. 현재 bootJar SHA256 `c3fc4d4f1b7b2b5c95c066fefcf6dc96d174468704efd199365052fb9256406e`는 운영 JAR가 아니다.
- 실제 SSR을 합성 fixture로 연 Chrome에서 320/360/375/768/1024/1440px 가로 넘침 없음, 페이지 이동·키보드 초점·계획 변경 차단·401/403/409/계약 오류/연결 종료 후 결과 제거·재조회 유지를 검증했다. 정상 콘솔 오류/경고0, fixture 변경 요청0/외부 요청0/운영 쓰기0. CSS 확대2/4배 모의에서는 넘침을 관측했으며 브라우저 기본200/400% 확대·보조공학 검증은 미실행으로 남긴다. 합성 화면 통과를 운영 브라우저 Gate로 계산하지 않는다.
- 기존 Thymeleaf/layout/공통 CSS/정책 client를 재사용했다. 원문·URL/파일명·서버 임의 오류를 표시하지 않는다. 브라우저 세션과 Node 서버를 종료했고 생성 snapshot/로그는 ignored build 경로에 보존했다. 사용자 output의 Word2개119943/330388bytes는 보존했다. 상세 설계·한계는 [조회 화면 기록](announcement-provider-coverage-ui-2026-09-15.md)에 있다.
- 직전 SHA `8db0c1072e9c1bb3a68cb9d6656ad13d6ad282f7`의 [Linux 실행25](https://github.com/FrostyCityMan/saneB/actions/runs/34892681344)는 성공했다. root2484=2231통과/253생략, extractor25·패키지20·실제 job192·migration17·worker11·runtime1·부모2·독립220/220와 정리, Node154/설치12 통과다. 이번 UI 소스의 같은 SHA 검증은 아니며 공식 파일 관측은 생략됐다.
- 현재 남은 핵심은 전체 공식 기대값(참조15/실행 기대값0)과 미구현 기관 모델·실파일/역할 검증, 수집원 QA 예약·취소 화면, 현재 SHA의 운영 배포/브라우저, 승인된 전체 기존 데이터 적용이다. AWS 재인증 응답은 대기 중이며 운영 조회를 반복하거나 활성화하지 않았다. 정책 게시·ENFORCE·기존 데이터 APPLY는 정확한 범위/예산/영향 승인이 필요하다. 전체 Gate/ATT **Not ready**, goal ACTIVE이며 이번 회차 분류는 **progress**다.

### 2026-09-15 05:22 KST — 형식 적용성 V2와 전체 대상 coverage API

- 제천 증분은 QA 브랜치 `be45ddfa4d883c56527861f8e2c469237682f3f3`로 커밋/푸시하고 원격 SHA 일치를 확인했다. [Linux 실행24](https://github.com/FrostyCityMan/saneB/actions/runs/34890992681)는 전체 성공이다. root2455=2202통과/253생략, extractor25·패키징20·실제 job192·migration17·worker11·runtime1·부모2·독립220/220 및 정리 성공, Node154/설치12 통과다. 외부 파일 재추출/운영 배포는 하지 않았다.
- 이어 [형식 적용성 V2](announcement-provider-format-applicability-v2-2026-09-15.md)를 구현했다. 모든 기관에 세 형식의 존재를 요구하는 schema1은 보존하고, 별도 schema2에서 각 기관의 전체 기대 파일에 등장한 지원 형식·미관측과 정상 다중 첨부 수를 구분한다. 전체3형식/기관별 정상3공고·정상 다중 첨부1건·전체 기관/파일 분모를 유지한다. 실패 PDF를 다른 기관의 성공으로 상쇄하거나 미관측을 미지원/N/A로 만들지 않는다.
- 새 v2 GET `/provider-qa-runs/execution-plan/targets`는 ADMIN/OPERATOR/APPROVER에만 페이지/전체 지문/형식 기대 metadata를 반환한다. 기존 API 응답과 v1은 보존하며 조회의 DB 쓰기/HTTP/예약/정책 변경은0이다. 실제 metadata 화면 연결·브라우저는 후속 작업이다.
- catalog·근거 재검증·Service·MockMvc·snapshot 표적156건은53초에 전부 통과했다. 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1`은3분18초 성공: root2484=2232통과/252조건부 생략/실패0, QA 패키징20/20이다. 추출기 시험25는 UP-TO-DATE이며 새 실행 통과로 세지 않는다. 로컬 생략은 실제 DB/Linux 성공이 아니다.
- 최종 bootJar SHA256은 `b1bdf80619b037c9678594204cffd371912db3ce8ea2644e7815a7dbc764c41a`다. Node 일회성 검사로 보존된 schema1과 새 schema2의15참조가 같고 실행 기대값0임을 확인했다. 과거 리소스·V1~V83/DDL/운영 행을 변경하지 않았다. 앞선 실행24는 이 형식 적용성 증분의 같은 SHA 증거가 아니다.
- 현재 엔진6/프로필14(지자체13)/본문13기관이며 실제 전체223기관·공식 역할/내용 기대값·운영 배포와 승인 범위 배치·브라우저가 잔여다. AWS 재로그인 응답 대기, 운영 상태는 마지막 V72 snapshot 이후 미확인이다. 사용한 Node/Gradle은 종료했고 사용자 output/Word2개는 보존했다. P3 진행, 전체 Gate/ATT **Not ready**, goal ACTIVE다.

### 2026-09-15 05:04 KST — 제천 본문·첨부 모델, 실제 HWPX4파일 및 공개 참조15건

- 제목→정제 본문→실제 첨부 텍스트→관리자 최종 검증 순서를 유지하면서 제천 고정 BBS 모델을 추가했다. 전용 본문13기관/첨부 엔진6/등록 프로필14(지자체13)/추출 형식3이다. 전체 활성 지자체223개를 지원 완료로 계산하거나 분모에서 제외하지 않는다.
- 제천 공식3공고의 BODY는 AVAILABLE/시도1/redirect0, 전체 HWPX4첨부는 발견·다운로드·signature 검사 통과다. 최종 실제 시험6건/생략0, 첨부 검증7요청/예약1,261,135bytes/원본 정리3/3이며 BODY 별도3요청이다. 텍스트 추출·역할/내용 판정·DB worker·최종 정상 후보의 성공 근거는 아니다.
- 제천 고유 첨부 표제·공식 SVG 구조·빈 id를 포함하는 실제 저장 URL 경계를 반영했다. 실제4파일의 MIME 관측에 따라 제천에만 기존 application/x-msdownload 예외를 지정했고 signature/확장자/Content-Disposition 의무는 유지한다. 다른 기관·원주의 형식 불일치를 허용하지 않는다. 자세한 구조/실패 원인은 [제천 증분](announcement-jecheon-bbs-profile-2026-09-15.md)에 기록했다.
- 공개 catalog는 원주/제천 각3건을 더해 참조15/실행 기대값0이다. 형식 불일치 표본을 숨기지 않는다. 참조 추가는 정책 QA 성공·활성화·HTTP 실행이 아니다. 기관별 실제 제공 형식의 적용성 계약은 여전히 잔여이며 현재 validator의 대상별3형식 요구를 해결했다고 보지 않는다.
- 표적138건과 공식6건은 통과했다. 첫 전체 회귀는 catalog 기대값9→15의 오래된 단위시험1건에서 실패했고, 실행 기대값0/QA 미통과 보호를 유지한 채 건수만 수정했다. 재실행 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1`은3분11초 성공이다. root2455=2203통과/252조건부 생략/실패0, QA 패키징20/20이다. 추출기25건 결과는 UP-TO-DATE로 이번 새 실행 성공으로 세지 않는다.
- 최종 bootJar SHA256은 `b81644a55a9880c33b93a435356d725cbc22299ef2986007f3e7e9b7f33760d6`이다. 실제 DB/Linux 전용 시험의 로컬 생략을 통과로 계산하지 않는다. Docker Linux engine은 현재 접근 불가이며 설정을 변경하지 않았다.
- 직전 SHA `569553417c371f34ba911de452807c59aa1d3d6e`의 [Linux 실행23](https://github.com/FrostyCityMan/saneB/actions/runs/34888428248)는 root2440=2189통과/251생략, extractor25·패키징20·실제 job192·migration17·worker11·runtime1·부모2·독립220/220 및 정리 성공, Node154/설치12 통과다. 공식 파일 재관측은 하지 않았고 이번 제천 증분의 같은 SHA 증거는 아니다.
- AWS 재인증을 요청했으며 응답 대기 중이다. 마지막 운영 V72/등록 결합11·미결합212와 새 코드의 원주/제천을 혼동하지 않는다. 이번 회차 운영 DB/배포/규칙/플래그/기존 데이터 쓰기와 운영 브라우저 검증은 미실행이다. 신규 DDL/API shape/UI 변경은 없고 사용자 output/Word2개를 보존한다. 전체 Gate/ATT **Not ready**, goal ACTIVE다.

### 2026-09-15 04:40 KST — 원주 기관 모델·본문 연결 및 실제 형식 예외 확인

- P3 구현·검증을 이어 원주 BBS의 고정 모델과 정제 BODY를 추가했다. 제목→본문→실제 첨부 텍스트→최종 관리자 검증 순서와 기존 제목 제외·A/B·UNKNOWN·자동 ACTIVE 금지 정책은 변경하지 않았다. 첨부 엔진6/등록 프로필13(지자체12)/전용 본문12기관으로 확장했으며 전체 대상223개를 축소하지 않는다.
- 원주 공식 지원 관련3공고의 BODY는 AVAILABLE/시도1/redirect0이다. 첨부6개 전부를 발견·다운로드했고5개 HWPX signature 통과,1개 표시 HWPX/실제 OLE-HWP 불일치는 `ATTACHMENT_FORMAT_MISMATCH`로 거부했다.6시험 통과 중1시험은 명시적인 음성 검증이다. 정상 후보3개·텍스트 추출 성공으로 계산하지 않는다. 마지막 첨부 검증9요청/예약1,434,064bytes/원본 정리3/3, 본문 별도3요청이다.
- 초기 파일 시험3실패를 실제 헤더·signature 관측으로 분리했다. 원주에만 기존 UTF-8 header octet 엄격 복원을 연결했고, legacy MIME 예외·형식 불일치 허용은 추가하지 않았다. BBS 공유 코드 지문이 바뀌므로 이전3기관의 정책/QA 근거를 새 코드에 재사용할 수 있다고 보지 않는다.
- 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1`은3분32초 성공, root2439=2189통과/250조건부 생략/실패0, 패키징20/20이다. 추출기 시험은 UP-TO-DATE이며 이번 새 실행 성공으로 세지 않는다. 이후 테스트 경계1건 추가·불필요한 진단 필드 제거에 대한 마지막 표적 재검증은 아래 후속 결과를 따른다. Node22건=20통과/2 Linux 전용 생략/실패0이다.
- 마지막 `:test`의 BBS 프로필·BODY·Provider 계획3개 시험군94건은22초에 전부 통과/생략0이다. 최종 production bootJar SHA256은 `880d1e9e1db6ee5cc8ac49ea72bdf881a434a7e65d046913438855f454d28a76`이며 운영 JAR와 구분한다. 사용한 Node/Java 및 임시 조사 파일은 정리했고 다른 작업의 프로세스는 종료하지 않았다.
- 직전 `fb81eaa85e2ed0a8f0a9aa441b7866b02904992a`의 [Linux 실행22](https://github.com/FrostyCityMan/saneB/actions/runs/34885874260)는 전체 성공이다. root2426=2177통과/249생략, 별도 extractor25·패키징20·job192·migration17·worker11·runtime1·부모2·독립220/220 및 정리 통과다. 원주 증분 이후의 CI 또는 공식 파일 재추출 근거는 아니다.
- 서울 AWS 세션이 만료되어 현재 운영 조회는 차단됐다. 원주 목록 parser는 저장소 V62 기준 HEURISTIC_NOTICE이며 운영 일치는 미확인이다. 따라서 직전 운영 결합11/미결합212를 현재211로 바꾸지 않는다. 운영 배포/정책/플래그/데이터/브라우저 변경·검증은 이번 회차에 실행하지 않았다.
- 상세 근거는 [원주 모델 증분](announcement-wonju-bbs-profile-2026-09-15.md)에 기록했다. 제천은 구조 차이만 확인했으며 미구현 상태로 남긴다. DB/API/UI/migration 변경은 없고 임시 조사 원문·소스는 삭제, 사용자 output/Word2개는 보존했다. 공식 역할 기대값·전체 기관 모델/형식 QA·같은 SHA 운영 적용·승인된 전체 데이터 배치·운영 브라우저가 남아 있으며 전체 Gate/ATT **Not ready**, goal ACTIVE다.

### 2026-09-15 04:12 KST — 등록 지자체의 본문 정제 연결 완료, 전체 대상 검증과 구분

- 직전 회차의 QA 배포 패키지 보완은 [Linux 실행21](https://github.com/FrostyCityMan/saneB/actions/runs/34883813483), `74cc12553c3ca4ac86eb2fd0c063fbac48abc2f5`에서 전체 성공했다. 설치12/12, 실제 job192·migration17·worker11·격리 runtime1·부모2·독립220/220 및 정리 통과다. 공식 관측을 재실행하지 않아 이전 timeout은 미해결이다.
- 부산시/강북구/화천군 각2공고의 구조를 실측하고 전용 BODY 정제를 추가했다. 강북은 요청/공식 hidden 공고번호를 대조하며, 세 기관 모두 본문 밖 담당자·첨부명·메뉴를 제거한다. 실제 본문 A/B 문구·내부 표·신청 링크는 보존한다. 구조 누락/변경/빈 본문을 주변 페이지로 대체하지 않는다.
- 본문49개 회귀·전체 root2426건=2178통과/248조건부 생략/실패0, 패키징20/20 통과다. 전체3분40초 후 마지막 소스로 실제 상세11사례를24초에 재검증하여 AVAILABLE/시도1/redirect0을 확인했다. Node20통과/2 Linux 전용 생략·diff 검사도 통과했다. 실제 상세 시험은 본문 HTTP smoke이며 첨부 파일/DB/최종 후보 성공이 아니다.
- 같은 완료 운영 SSM snapshot과 코드 SourceBinding을 대조하여 지자체11개는 기관·목록 parser 일치, 남은212개 활성 지자체는 결합 없음으로 확인했다. 전용 본문11기관/첨부 엔진6·등록 profile12·추출 형식3을 운영 전체223개와 구분한다. 수집 대상을 줄이거나 미구현 기관을 전체 QA에서 빼지 않았다.
- migration/API/UI/분류 정책·운영 DB/플래그는 변경하지 않았다. 실제 운영 배포/브라우저는 선행 검증이 남아 미실행이다. 상세 구조·명령·지문은 [3단계 설계 증분](announcement-three-stage-filtering-workflow-2026-09-14.md), 최신 운영/CI 사실은 각각 연결 문서를 따른다. 전체 Gate0~8/ATT001~062와 **Not ready**를 유지한다.

### 2026-09-15 03:50 KST — 운영 기준선 실측·누락된 QA 배포 경로 보완

- 서울 AWS 인증의 TLS 검증을 유지한 읽기 전용 조회로 CodeDeploy/실행 JAR/DB를 대조했다. 운영 active·localhost health UP, 실제 JAR/DB V72, 새 상시 첨부 worker class와 정책 DB QA 패키지는 미설치다. 현재 QA 코드가 운영에서도 실행된다고 보지 않는다.
- 활성 지자체223개/목록 파서41종, 저장 원문 LOCAL_GOV_NOTICE 2,945건을 확인했다. 첨부 엔진6종/등록 프로필12개와 다른 분모다. 2,945건은 적격 기존 데이터 배치의 승인 범위가 아니다. 실행 중 서비스의 기업마당/정부24 API key는 없었다. 원문/인증값·DB 접속값은 출력하지 않았고 데이터 쓰기0이다.
- 배포에 빠져 있던 별도 QA distribution을 포함하고, 실제 JAR 지문별 불변 설치·launch/이전 JAR 복구 시 경로 선택을 구현했다. 웹 JAR 선택에서 새 QA JAR 혼입을 방지했다. migration/API/분류 정책/상시 플래그는 변경하지 않았다.
- 로컬 Node/Git Bash10통과/2 Linux 전용 생략·Gradle workflow8/패키징20 통과다. 전체 root2417건=2169통과/248조건부 생략/실패0,3분8초 성공이다. Linux/실제 CodeDeploy 설치는 후속 검증이며 기존 DB·추출기의 전체 rollback 성공을 주장하지 않는다.
- 공식 관측 실행20은 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT3건으로 전체 실패다. 같은 SHA의 실제 DB job192/migration17/worker11·runtime1·부모2·독립220/220 및 정리는 통과했다. role 구조 관측은 여전히 미실행이며 이전7파일 관측으로 대체하지 않는다.
- 자세한 운영 사실·명령 ID·설치/복구 경계는 [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md), 실행19/20 XML 건수는 [Linux 검증 기록](announcement-attachment-linux-contract-qa-2026-09-11.md)에 남긴다. 전체 Gate0~8/ATT001~062, 제목→본문→첨부→관리자 순서를 유지한다. 현재 **Not ready**다.

### 2026-09-15 02:15 KST — 실제 실패 특정·공식 템플릿 보완·V83 비교식

- 고정 전체 공고 관측은 `c647628cff72839b85e540feb6ed97a401661e0a`로 QA 브랜치에 커밋/푸시했고 원격 SHA 일치를 확인했다. master는 `ae893b87348a9bd1cb0893763f6cad5047093a24` 그대로다.
- [Linux34870322920](https://github.com/FrostyCityMan/saneB/actions/runs/34870322920),456ab4c의 주 검증/부모 연결·취소2건/정리는 통과했으나 독립은219중218통과/1실패/생략0이었다. 실패 지문851f56bf…는 `full1001InventoryReservesBothSegmentsWithoutDuplicateOrNewArrivalAndAccountsAllDimensions`와 정확히 일치한다. 같은 SHA 주 시험에서는82.087초 통과했다. 실패 예외 종류는 기존 보고서에 없어 확정하지 않는다.
- [Linux34872159512](https://github.com/FrostyCityMan/saneB/actions/runs/34872159512),c647628은 주 검증·독립219/219/생략0·부모2건·정리가 통과했다. 같은1,001건 주 시험은63.932초다. 간헐 실패가 영구 해결됐다는 뜻은 아니다. 공식3공고 관측이 제목 확인 단계에서 실패해 전체 workflow는 실패다.
- 공식 페이지3건의 실제 HTML에 비어 있지 않은 og:title과 빈 템플릿 og:title이 함께 존재함을 재확인했다. 첫 관측은 상세3회/첨부0회, 제목 Gate는 모두 COMBINATION_MATCHED, 원본 정리3/3이었다. 빈 제목만 제외하고 단일 실제 제목/고정 제목 일치 의무를 유지하도록 보완했고 빈 값만 존재·서로 다른 실제 제목 중복·제목 변경 음성 시험을 추가했다. 파일 추출/역할 성공은 아직0이다.
- V83은 V75의 각 job→전체 분할 양방향 상관 탐색을 정렬된 source/content/base/release/provider5값의 실제 JSONB 배열 대조로 교체한다. 해시나 DISTINCT로 누락·중복을 감추지 않는다. 기존 지연 trigger3개/삭제 전후 분모/전체 job count/필수 receipt/23514·잠금·시간 한도는 보존한다. 기존 V1~V82와 운영 데이터는 수정하지 않는다. 성능·실패 해결은 실제 Linux 측정 전 미확정이다.
- 새 검증은 실제 migration 비교식을 추출한17개 합성 tuple 동등성(정상/역순/누락/추가/중복/빈값/5필드 변경·NULL), V82→V83/빈DB/지연 trigger 유지, 기존1,001건 전수·경합/삭제 시험이다. 표적 MigrationContract93건·bootJar는23초 성공했다. 후속 전체 `test bootJar attachmentContractQaTest`는3분14초 성공(root2396=2149통과/247조건부 생략, QA20통과). 마지막 빈-title 보완/비교식 경계 정리 전의 전체 결과이며 해당 최종 수정은 표적 재검증한다. 추출기 시험/설치는 UP-TO-DATE다.
- 최종 `:test --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' --tests '*MigrationContractTest' bootJar --no-daemon --max-workers=1`은20초 성공,93+4+8=105건/생략0이다. 사용한 로컬 Java/Node 프로세스는 종료했다. V83 실제 DB와 공식 파일 텍스트 관측은 다음 같은 SHA의 원격 실행에서 확인한다.
- Windows에서 해당1,001건만 `attachmentMigrationTest --tests '*AnnouncementAttachmentBackfillIntegrationTest.full1001InventoryReservesBothSegmentsWithoutDuplicateOrNewArrivalAndAccountsAllDimensions'`로 안전한 대체 검증을 시도했으나17초에 임시 PostgreSQL initdb 기동 실패(시험 본문 미실행)였다. 운영 DB를 사용하거나 Docker/보안 설정을 변경하지 않았다. 이 실패를 Linux 계약 실패와 같은 원인으로 단정하지 않는다.
- Release **Not ready**, goal ACTIVE. V83 실제 DB/시간 검증, 공식 전체 파일 관측·역할 기대값·전체 대상/형식 적용성, 같은 SHA 운영 배포와 승인 범위 데이터 처리, 운영 브라우저가 남아 있다. 사용자 output/Word2개는 보존한다.

### 2026-09-15 01:57 KST — 고정 기업마당 전체 첨부 관측 경로

- 시작 HEAD/origin은 QA 브랜치 `456ab4cef7d6f20f278daa5416451044590ffd5b`다. 사용자 output/Word2개는 보존한다. 기존 goal과 제목→본문→실제 첨부 텍스트→최종 관리자 검증 순서를 유지한다.
- 기존 실제 파일 시험은 선택한4파일·수동 QA 역할을 사용하므로 전체 공고/자동 역할 검증으로 확대할 수 없다. 새 `attachmentOfficialFileObservation`은 기존 공개 기업마당3공고(SEMAS/안양/서대문)의 공식 전체 descriptor를 사용한다. 고정 제목 선행 Gate·공식 상세 제목 일치→전체 발견→기존 pinned transport/signature→실제 Linux 격리 추출→텍스트 역할/위치 지문 관측이며 파일명으로 역할을 추정하지 않는다.
- 관측은 승인된 기대값과 별개다. `OBSERVED_NOT_VALIDATED`, `isExpectationApproved=false`, `isPolicyQaPassed=false`를 유지한다. 본문/API 수집·DB worker·운영 연결을 실행하지 않으므로 `isBodyPipelineVerified=false`다. 관측 데이터를 catalog에 자동 등록하거나 UNKNOWN을 NOTICE로 바꾸지 않는다. 원본은 정리하고 코드·형식·품질·건수·지문·역할 근거 위치만 보관한다. 파일별 실패/미지원/미실행과 전체 분모를 보존한다.
- 고정3공고 각각 파일최대10개·파일20MiB·전체예약80MiB·요청44회·420초 이내다. 운영 key/DB/계정/정책을 사용하지 않고 같은 migration을 적용한 임시 loopback DB의 DRAFT 규칙만 읽는다. 일반 test와 일반 QA push는 외부 호출 OFF이며 명시 수동 입력 또는 QA 커밋의 `[official-file-observation]` 표식에서만 실행한다. 기존 필수 DB 검사와 실패 차단을 유지한다.
- 표적 `:test --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' bootJar --no-daemon --max-workers=1`은21초 성공,3+8=11건/생략0이다. bootJar는 UP-TO-DATE이며 새 코드는 test/QA workflow 범위다. Node 보고서 판정기10건 통과. 전체 회귀와 실제 Linux 관측은 진행/대기 중이며 성공으로 세지 않는다.
- 후속 전체 `test bootJar attachmentContractQaTest --no-daemon --max-workers=1`은3분10초 성공, root2394=2148통과/246조건부 생략/실패0 및 독립 실행기/패키지20건 통과다. 실제 공개 파일 관측은 일반 test에서 생략됐다. 추출기 시험/설치·bootJar는 UP-TO-DATE이며 이번 새 실행 통과로 세지 않는다.
- [Linux34870322920](https://github.com/FrostyCityMan/saneB/actions/runs/34870322920),456ab4c는 주 검증이 통과했지만 독립 DB 실행이 실패했다. 부모 연결 검증은 진행 중이다. 이번에 추가한 고정 suite/case 진단 및 전체 XML로 원인을 특정해야 하며 지난219건 통과를 이번 실행 결과로 대체하지 않는다.
- Release **Not ready**, goal ACTIVE. 이번 증분에 migration/API/정책/운영 데이터 변경은 없다. 실제 전체 Provider/형식 적용성/역할 기대값·실패 원인 해결·동일 SHA 운영 적용·승인된 데이터 처리·운영 브라우저 Gate가 남아 있다.

### 2026-09-15 01:36 KST — P4 역할 기대값 결합·공식 9공고 재확인

- 역할 기대값 증분은 `397e59791d99bc161c0b6362759326c3781c14e9`로 로컬 커밋했다. 후속 부모 진단은 비정상 종료의 계약 JSON에서 고정4시험군의 일관된 건수와 최대32개 실패/생략/미실행 시험 지문만 선택한다. 원문 예외/SQL/URL/임의 클래스명은 전달하지 않으며 목록을 제한해도 전체 실패 수와 잘림 여부를 보존한다. 비정상 종료는 계속 실패이고 성공 보고서/위조 scope·지문·자료형·건수·중복 JSON을 진단으로 채택하지 않는다. 진단 보완은 자식 시험 실패의 해결 증거가 아니다.
- 진단 후속 표적 `.\gradlew.bat :test --tests '*AttachmentWorkerDbQaProcessTest' --tests '*AttachmentWorkerDbQaGateTest' --tests '*AttachmentContractWorkflowTest' attachmentContractQaTest bootJar --no-daemon --max-workers=1`은43초 성공했다. 부모process20/gate10/workflow7 및 독립 실행기/패키지20건, 총57건 통과·생략0이다. bootJar 재생성 완료. 사용한 로컬 Java/Node 프로세스는 종료했다. 후속 원격에서 같은 SHA의 실제 실패 지점을 확인해야 한다.
- 기준 QA HEAD/origin은 `657fd31abc5a0b89042a5e30721d09cab7484061`, origin/master는 `ae893b87348a9bd1cb0893763f6cad5047093a24`다. 새 증분은 catalog 고정 역할/사유/텍스트/위치/assessment 지문→실제 추출 역할 판정→V79 안전 metadata→원장 재검증이다. 기존 v1/v2 HTTP shape, 과거 누락 필드 hash, V1~V82 및 운영 데이터는 보존한다.
- COMPLETE_TEXT인데 역할 기대값 없는 새 catalog 항목은 실행 준비 불가다. UNKNOWN 음성 결과나 양식/참고자료만 있는 공고는 정상3공고를 채우지 않는다. 실제 파일 관측·검토 없이 catalog 기대값을 자동 생성하거나 QA 통과로 바꾸지 않았다. 공식 참조9/실행 기대값0과 전체 형식 적용성·Provider UI는 잔여다.
- 표적 Provider265건/생략0 및 bootJar 통과(55초). 첫 실행의 전체 근거 시험26건은 quality-only 합성 입력이 새 catalog 계약에서 거부되어 실패했으며, 검증 항목을 유지하고 역할 근거를 합성 fixture에 연결한 뒤 모두 통과했다.
- 전체 `.\gradlew.bat test bootJar attachmentContractQaTest --no-daemon --max-workers=1`은3분37초 성공. root2385=2140통과/245조건부 생략/실패0, 독립 실행기/패키지20건 통과다. bootJar는 직전 표적 명령에서 생성했고 전체 실행에서는 UP-TO-DATE다. Node 보고서 계약10건 통과·프로세스 종료. 로컬 Docker는 Linux engine pipe가 없어 사용할 수 없었으며 격리 추출 성공으로 표시하지 않았다.
- `attachmentProfileDiscoveryQa --tests '*StandardBbsAttachmentProfileLiveQaTest'`를 Windows-ROOT trust store로 실행해 공식 태백/횡성/영월9공고14첨부를21초에 재확인했다.9건 생략 없이 통과, PDF2/HWPX12, 요청23회·예약2,662,537bytes·임시 원본 정리9/9다. TLS 검증을 해제하지 않았다. 이 결과는 상세/전체 첨부 발견·다운로드·signature만 증명하며 실제 텍스트 추출/역할 정확도/전체 사이트 지원·HWP 미제공을 뜻하지 않는다.
- d56a7a8 [실행34866219185](https://github.com/FrostyCityMan/saneB/actions/runs/34866219185)의 V82 migration3·실제 worker11건은 통과했다. 부모 취소는 통과했지만 정상 연결은 QA_CHILD_PROCESS_LIMIT로 실패했다.
- 657fd31 [실행34867265889](https://github.com/FrostyCityMan/saneB/actions/runs/34867265889)은 주 검증 및 독립219/219(생략0)가 통과했으나 전체 workflow는 실패다. 직접 부모 실행에서 같은UID14/부모JVM6으로 낮아졌고 자식 검증까지 진행했지만 QA_CHILD_FAILED가 반환됐다. 정리 POLICY_DB_QA_CLEANUP=SUCCEEDED다. 기존 기동 자원 문제와 다른 실패이며 원인은 아직 미확정이다. 원문 로그를 공개하지 않고 실패 suite/case 지문·건수만 안전하게 확인할 진단 연결이 다음 조치다. 제한 완화·실패 무시·맹목 재실행은 하지 않는다.
- Release **Not ready**, goal ACTIVE. 운영 배포/게시/ENFORCE/기존 데이터 적용/운영 브라우저는 선행 Gate 미충족으로 미실행이다. 사용자 output/Word2개는 보존한다.

### 2026-09-15 P3 증분 — 역할 근거의 실제 처리 경로 연결

- 후속 확인: 역할 연결은 `d56a7a8210aad6f9d5c7ead689b3e9ebea38553b`로 QA 브랜치에 커밋/푸시했고 원격 SHA 일치를 확인했다. [Linux 실행34866219185](https://github.com/FrostyCityMan/saneB/actions/runs/34866219185)의 V82·worker·artifact 주 검증 단계는01:11 KST 통과, 독립 namespace/부모 단계는 진행 중이다. XML 최종 집계와 전체 workflow 성공은 아직 확인 전이다. origin/master는 기존 `ae893b87348a9bd1cb0893763f6cad5047093a24` 그대로다.
- 부모 실행 자원 개선: 전용 UID의 준비 Gradle을 종료한 뒤 `AttachmentPolicyDbQaCiMain`이 동일 JUnit 부모2사례를 직접 실행하도록 변경했다. 기존 Ledger를 재사용해 실패/생략/미실행/container 실패를 차단하고 고정 클래스·case 지문만 XML로 기록한다. 임의 suite/파일/URL 인자는 받지 않으며 운영 JAR/독립 QA JAR에 포함하지 않는다. 준비용 classpath는 ignored build 아래에만 생성한다.
- 기존128 thread/2GiB/namespace·비root·깨끗한 환경·소유 UID 정리/한도를 유지한다. CI 임시 계정에서 남은 준비 Java 종료를 최대10초 확인한 뒤 실제 시험을 시작하며 임의 프로세스를 종료하지 않는다. 이번 변경은 부모 자원 오류 해결 시도이지 해결 증거가 아니다.
- 로컬 후속 `.\gradlew.bat :test --tests '*AttachmentContractWorkflowTest' attachmentContractQaTest prepareAttachmentPolicyDbQaCi bootJar --no-daemon --max-workers=1`은37초 성공. workflow7/독립 실행기·새 직접 실행기20건 전부 통과, Bash `-n` 통과다. bootJar는 UP-TO-DATE이며 후속 변경은 CI 전용이다. 실제 Linux 부모 결과 확인 전까지 Release Not ready를 유지한다.

- 기준 HEAD는 QA 브랜치 `66d499c33fb76d46fdd2fb6592a9b465bc5ddab6`다. V82를 additive로 작성하고 새 정책의 역할 규칙 버전/지문 → worker → checkpoint/재시도 → 불변 file/extraction 근거 → v2 파일 조회 → 한글 관리자 근거 화면을 연결했다. 기존 정책/snapshot과 V1~V81은 보존하며 운영 정책/데이터를 갱신하지 않았다.
- `document-role-1.0.1`은 UNKNOWN/UNKNOWN·완전 추출에서만 동작한다. 기존 MANUAL/PROFILE을 덮어쓰지 않으며 내용 부족/혼합/문맥 불확실은 UNKNOWN과 이유를 남긴다. 자동 역할 확정은 공고 최종 승인·자동 활성화가 아니다. 제목→본문→첨부→최종 검증 순서와 기존 A/B 의무를 변경하지 않았다.
- V82는 정확한 extraction/file/set/source 복합 FK와 정책 버전·텍스트/문단 지문·실제 코드포인트 좌표를 검증한다. 관리자 수정/비선택 재사용은 원래 근거를 보존하며 새로운 다운로드 결과에 과거 assessment를 복사하지 않는다. DTO는 고정 metadata만 반환하고 원문은 기존 추출 조회에서만 읽는다.
- 로컬 `test bootJar :attachment-extractor:installDist --no-daemon --max-workers=1`은 3분10초 성공, root2355=2112통과/243조건부 생략/실패0이었다. 추출기 시험/설치는 UP-TO-DATE로 이번 재실행 통과가 아니다. 이후 추가된 DB migration 음성 사례와 선택 재시도 사례는 별도로 컴파일·Linux 실행해야 하며 이 전체 결과에 포함됐다고 하지 않는다.
- 후속 표적 검증·`attachmentContractQaTest installAttachmentContractQa bootJar`는43초 성공했다. 역할 연결6/형식 검증2/MigrationContract92 및 독립 패키지16건 통과, 실제 PostgreSQL migration3/worker10건은 Windows 환경에서 조건부 생략이다. 최종 선택 재시도 사례로 worker 전용 시험은11건이 된다.
- Node 관리자 화면/배치/보고서 계약154건 통과·생략0. 역할 근거의 같은 추출 ID/지문/좌표, 관리자 수정과 자동 제안의 구분, 미연결 정책 표시를 추가했다. 기존 UI와 native controls를 재사용했고 새 의존성/모션은 없다. 실제 브라우저 검증은 선행 Linux/운영 Gate 미충족으로 아직 실행하지 않았다.
- 새 실제 통합 검증은 HWPX 바이트→격리 parser→worker→PG→v2→관리자 역할 복제, 재시작 checkpoint, 부분 파일 선택 재시도와 성공 파일 재사용, V82 Unicode/변조/불변/cascade 계약이다. 합성 파일이며 공식 Provider 전체 정확도 QA를 대체하지 않는다.
- 이전 [Linux 실행34862376250](https://github.com/FrostyCityMan/saneB/actions/runs/34862376250)은 주 검증·독립215건·부모 취소·정리가 통과했지만 정상 부모 연결은 `QA_CHILD_PROCESS_LIMIT` 실패(같은UID97/부모JVM9)다. 이번 증분으로 그 실패가 해결됐다고 주장하지 않는다. 자원 제한128/2GiB·격리를 완화하지 않았다.
- Release **Not ready**, goal ACTIVE. V82/새 역할 경로의 Linux 검증, 부모 실행 자원 문제, 공식 profile/문서 역할 기대값·Provider 화면, 동일 SHA 운영 배포·정확한 범위 승인·기존 데이터 처리·운영 브라우저 E2E가 남아 있다. 사용자 `output/` Word2개는 그대로 보존한다. 전체 Gate0~8의 완료 기준과 분모는 축소하지 않는다.

### 최초 시작 시점 기준선

- 작업 경로: `C:\PersonalProject\saneB`, 루트 AGENTS.md 완독.
- 시작 HEAD: `ae893b87348a9bd1cb0893763f6cad5047093a24`, master, 작업 트리 clean.
- origin URL: `https://github.com/FrostyCityMan/saneB.git`. Windows 신뢰 저장소를 명령 단위로 지정한 원격 실조회에서도 master가 시작 HEAD와 일치했다.
- 실제 최신 migration: V72. 첨부 11개 테이블과 별도 CLI/순수 분류기만 존재한다. 상시 worker/DAO/API/UI는 시작 시 없음.
- 이전 서버 QA 문서에는 배포 69b7278 / CodeDeploy d-VHMRNZRPK 성공과 localhost health UP이 있다. 이번 작업의 현재 운영 확인을 대신하지 않는다.
- 지원 범위: 일반 텍스트 PDF, HWP 5.x, HWPX. OCR 실행/암호 우회/매크로/재귀 수집은 비범위이며 실패·검수 상태는 구현 범위다.

## Gate

| Gate | 상태 | 실제 결과 / 다음 작업 |
|---|---|---|
| 0 맥락·범위·검증 목록 | [~] | 09-15 10시대 운영 snapshot: 지자체223개/목록 parser41·JAR/DB V83. 전체 ATT62·9Gate 유지. 최신 전체 대상과 첨부 프로필의 적용성 대조가 남음 |
| 1 DB·API 계약 | [~] | 9fe892c Linux migration17·worker12·job192·Flyway3 실패/생략0. 실제 제천3공고 중 제목 중단1건과 HWPX3파일의 worker/임시 DB/API 일치도 확인. 태백·양평의 선행 근거 및 마지막 운영09-15 V83과 구분. 전체 catalog/Provider 근거는 미완료. v1/과거 migration 보존 |
| 2 상시 worker·Provider | [~] | 로컬 엔진7·첨부 profile19(충주 추가, 철원 실제 접속 QA 미완료). 마지막 운영 설치는 profile17/worker 비활성. worker/scheduler·예약/ENFORCE binding·24시간 재확인/checkpoint 구현. 전체223기관 적용성과 운영 상시 수집은 미완료 |
| 3 분류·정책 | [~] | 로컬 전용 BODY18. 09-22 서울 최신 코드 실제 관측·내용/역할 전체 지문 대조 후 catalog metadata3필드만 갱신. 참조30/현행 실행 가능1/정상0·coverage false. UNKNOWN+FORM과 관리자 검증을 유지. 전체 Provider/형식/정상 기대값·정책 게시 미완료 |
| 4 관리자 API·화면 | [~] | 처리 흐름 상세/최종 검증 대기열 구현·실제 PG/Java/HTTP/Node 검증. 운영 관리자 로그인·준비0/전체2945 조회 확인. 검수·DRAFT·운영 역할별 E2E는 미완료 |
| 5 기존 데이터 | [~] | 전체 후보 고정·불변 분할/배치/수집/적용/원복 구현과 실제 PG 전수/경합 시험 통과. 1,001건 전수 분할도 독립 환경에서 통과. 승인 범위 운영 실행/최종 대조·운영 응답 성능은 미완료 |
| 6 자동·실파일 QA | [!] | 09-22 서울 태백 관측/고정 비교 성공·정상0/검수 유지. f496d2e Linux35631537040 성공(root2489통과/267조건부 생략, runtime2·독립PG221/221·정책 부모2). ATT-059 실제 격리 canary 통과. 옥천 관측은 제목 중단1건 통과/양성2건 본문·상세 timeout이며 전체 Provider 정상/형식 기대값·부분 추출·접근 실패는 미해소 |
| 7 운영 배포·활성화 | [~] | 09-22 최신 STS/Runtime/DB SSM 성공: 인증사용가능·787c594·V83·추출기1.0.1·catalog24/기대값1·healthUP·첨부count0/worker비활성/외부key2종없음. 새1a19e63/추출기1.0.3 코드 재배포 재개 및 게시/ENFORCE/기존 데이터 정확한 범위 승인은 별도로 필요 |
| 8 운영 브라우저 E2E | [~] | 이전476c8f7 관리자 로그인·읽기 전용 목록, 2bde216 배포 후 인증 만료→로그인 확인. 최신787c594의 인증 업무 E2E는 재로그인 대기. 실제 검수/DRAFT/복구·역할/반응형 업무 E2E는 미완료 |

## 최신 실행 기록

### 2026-09-15 00:30 KST — 전용 계정 실증 및 텍스트 문서 역할 제안기 증분

- QA 브랜치 HEAD/origin `66d499c33fb76d46fdd2fb6592a9b465bc5ddab6`까지 한글 커밋·푸시했다. `8cb55d1` 전용 UID 격리, `5bdf715` 역할 제안기, `66d499c` 준비/시험 Gradle 수명 분리와 비식별 응답 오류 구분이다. master는 `ae893b87348a9bd1cb0893763f6cad5047093a24`이며 운영 배포/정책/기존 데이터/기존 migration은 변경하지 않았다.
- [Linux 아홉 번째34860558789](https://github.com/FrostyCityMan/saneB/actions/runs/34860558789),8cb55d1은 전체 실패다. root2078통과/242조건부 생략·추출기25·패키지16·job192·migration/분할15·runtime12합성파일/1시험·worker8·Node152·bootJar 및 독립215/215는 통과했다. 부모 취소1건은 통과, 정상 연결1건은 inventory 처리 중 `QA_PROCESS_FAILED`다. 같은UID108/부모JVM9 관측과 전용 계정/임시 경로 정리 성공을 확인했다. 이 오류를 이전 자식 기동 오류와 동일한 원인으로 단정하지 않는다.
- 역할 제안기는 COMPLETE_TEXT와 단일 파일의 텍스트 제목/항목/위치만 평가한다. 파일명/URL/다른 파일 키워드는 입력받지 않고 혼합·불완전·불명확은 UNKNOWN이다. 규칙/텍스트/위치 지문과 원문 없는 근거를 반환한다. 22개 합성 회귀와 기존 분류19건 통과. **DB/worker/API 연결 및 공식 파일 정확도는 미완료**이며 실제 역할을 변경하거나 정책을 활성화하지 않는다. 엔진6/등록profile12/형식추출3의 수를 늘린 사이트 모델이 아니다.
- 역할 제안기 포함 로컬 전체 시험·bootJar는3분18초 성공(root2101통과/241조건부 생략, 패키지16). 추출기 시험/설치는 UP-TO-DATE였다. 이후 부모 오류/수명 보완의 표적 workflow7·process16·역할22=45건과 bootJar도28초 통과했다. 주 로컬/Node 명령의 자식 프로세스는 종료했고 사용자 Word2파일은 보존했다.
- [열 번째 Linux34862376250](https://github.com/FrostyCityMan/saneB/actions/runs/34862376250),66d499c는 현재 실행 중이다. 원격 성공/부모 문제 해소/전체 Gate 완료로 보고하지 않는다. 원격 완료 후 suite·누락·생략·취소/정리와 동일 SHA를 확인해야 한다. AWS/운영 health/브라우저는 이번에 조회하지 않았고 선행 Gate 미충족이다.
- 다음 실제 구현은 역할 assessment의 additive DB 제약과 checkpoint/재시도/수동 역할 보존 연결이다. 기존 제목→본문→실제 첨부→최종 관리자 검증을 유지하고, pure 판정기의 합성 통과만으로 역할 자동화나 전체 목표 완료를 선언하지 않는다.

### 2026-09-14 후속 최종 확인 — 본문3모델 반영 및 CI 공유 UID 자원 충돌 확정

- 최신 QA branch/HEAD/origin은 `d6d9bb0bfd431497adcbf1f83deda6fda7503a42`다. 이번 재개에서 `7267a30`(안전한 기동 오류 구분), `2c37345`(공식 BBS 본문3모델), `d6d9bb0`(상위 JVM 병렬성/heap 축소와 비식별 자원 관측)을 한글 커밋으로 푸시했다. master/운영 배포·정책·데이터는 변경하지 않았다.
- [최종 Linux 34834798383](https://github.com/FrostyCityMan/saneB/actions/runs/34834798383)는 **실패 / Not ready**다. root2077통과/242조건부 생략, 패키지16, PG job192·migration2/분할13·worker8, Linux 합성12파일, bootJar/설치 산출물과 독립215/215·정리 성공을 확인했다. 독립215는 앞선 전용 DB/worker215와 같은 계약의 반복이며430개 기능으로 세지 않는다.
- 부모2건은0.485초에 `QA_CHILD_PROCESS_LIMIT`로 실패했다. 부모 JVM은9개지만 같은 real UID 전체는158개여서 자식128개 제한을 이미 초과했다. `ActiveProcessorCount=1`/SerialGC 적용만으로 공유 계정 문제를 해결하지 못했다. 실제 부모 실패는 이전 실행5·7·8의3회이며 이번 재개 원격 실행도3회다. 유한 반복 원칙에 따라 이 지점에서 진단 반복을 종료하고, 임시 CI 전용 비관리자 계정/명시 환경/한정 소스/결과 XML 회수·소유 자원 정리 경로를 다음 구현 대상으로 둔다. 제한 증가·root 실행·공용 runner 프로세스 종료로 우회하지 않는다.
- P3는 본문3모델과 공식 본문3표본 검증까지 전진했다. 문서 역할 자동화는 V72 유래 제약·worker checkpoint 비교·재시도 수동 역할 보존·봉인 manifest·v2 근거 표시의 연결 설계를 추가했으며 코드/migration 구현 완료가 아니다. 전체 Provider 기대값, ATT/FLOW 전수, 운영 반영과 브라우저도 미완료다.
- 최종 기록3문서는 로컬 갱신이며 추가 CI를 유발하는 문서 전용 푸시는 하지 않았다. 미추적 Word 산출물 `output/`은 보존했다. 장기 goal은 완료 처리하지 않는다. 운영/AWS/브라우저 확인을 성공으로 표현하지 않는다.

### 2026-09-14 19:37 KST — 본문 BBS3모델 구현·공식 표본 확인 및 Linux 후속 진단

- QA 브랜치 HEAD/origin `2c3734506ff8a49cdef434979bef801be303abd3`, master/origin `ae893b87348a9bd1cb0893763f6cad5047093a24`를 재조회했다. Git 기본 인증서 경로의 조회 실패는 명령 단위 `http.sslBackend=schannel`로 Windows 신뢰 저장소를 사용하여 확인했으며 TLS 검증을 끄거나 전역 설정을 바꾸지 않았다. 운영 배포/정책/기존 데이터 적용은 하지 않았다.
- `7267a30`은 부모 자식 시작 오류를 stdout1MiB/stderr64KiB 제한 내에서 고정 코드로 구분하고 원문은 노출하지 않는다. 관련13건 통과. [Linux 34832431117](https://github.com/FrostyCityMan/saneB/actions/runs/34832431117)의 주 검증은6분3초 통과했으나 독립1,001건 분할이 다시 실패하여214/215통과·정리 성공이고 부모는 미실행됐다. 해당 독립 실패의 예외 종류는 아직 미확정이다.
- `2c37345`는 태백25·횡성65·영월17의 정확한 게시판 표/제목/내용 셀을 2차 본문 모델로 연결했다. 구조 누락/중복/식별자 모호성은 실패 처리하고 일반 페이지·파일명을 대신 본문으로 쓰지 않는다. 본문 안의 실제 A/B 문장과 신청 링크는 보존하며, 기존 제목→본문→첨부→최종 검증 흐름은 유지한다.
- 표적80건(본문27·수집21·첨부 분류19·프로세스13), 공식 공개 본문3건과 Node152건 통과. 전체 로컬 회귀4분2초 성공: root2318=2077통과/241조건부 생략, 독립 패키지16, bootJar/설치 산출물 성공. 추출기 task는 변경 없어 UP-TO-DATE다. 이후 게시판 식별자/redirect 검증을 추가한 최종80건·bootJar와 workflow6건·공식 본문3건을 다시 통과했다. 실제 공개 파일의 Linux 추출/분류나 운영 E2E 성공으로 확대하지 않는다.
- [Linux 34833700369](https://github.com/FrostyCityMan/saneB/actions/runs/34833700369)는 동일 `2c37345`의 주 검증 및 독립215/215·정리 성공 뒤 부모2건이 `QA_CHILD_PROCESS_LIMIT`로 실패했다. 산출물 생성 성공·미취소 조건에서 독립 시험 실패와 별개로 부모 진단도 수행하도록 했으며, 이번에 프로세스/스레드 생성 자원 부족 계열로 구분됐다. 자식128개 한도는 유지하고 검증 부모/Gradle JVM의 병렬성을 줄여 재검증한다. 모든 실패 종료·보고서 Gate는 그대로며 전체 완료/배포 판정은 **Not ready**다.
- 다음 구현은 나머지 본문 모델과 텍스트 근거 기반 문서 역할 식별이다. 현재 UNKNOWN은 보조 근거이고 기존 V72의 role_origin은 UNKNOWN/PROFILE/MANUAL뿐이므로, 파일명 기반 NOTICE 승격 대신 additive DB/API 근거 계약부터 연결해야 한다. 전체 Provider 기대값/ATT/FLOW 전수·동일 SHA 운영 배포·승인된 데이터 적용·브라우저 Gate도 남아 있다.
- 사용자 Word2파일이 있는 미추적 `output/`을 보존했다. 로컬 검증 Java와 사용한 Node 명령은 종료됐고 브라우저는 선행 Gate 미충족으로 실행하지 않았다. 이 진행 기록은 로컬 체크포인트이며 아직 원격 실행 결과 확정본이 아니다.

### 2026-09-14 18:49 KST — 독립 계약215건 실제 통과·정책 부모 연결 차단

- [Actions 34828914963](https://github.com/FrostyCityMan/saneB/actions/runs/34828914963), QA 브랜치 SHA `97bf0369d296ebf49661f38edd7518ffd448607c`의 전체 결과는 **실패 / Not ready**다. 주 Gradle6분50초 성공: root2308=2067통과/241조건부 생략, extractor25·독립 패키지16·Node152, job192·migration2·분할13·worker8, Linux 격리 합성12파일(1시험)과 bootJar/설치 산출물 생성 통과다.
- 독립 namespace는215발견/215통과, 실패·생략·미실행·container실패0, 3분25초이며 임시 원본/DB 정리 성공이다. 업무 코드와 추출기 지문 전후 대조도 통과했다. 앞선 환경 경로 불일치와 대량 시험의 기본60초 충돌을 보정한 결과다. 독립215건은 주 전용 DB/worker215건과 같은 계약의 재실행이며 서로 다른430건으로 합산하지 않는다.
- 정책 부모 연결 `AttachmentWorkerDbQaLinuxIntegrationTest`는2건 모두 실패(0.646초/생략0)했다. `selectIdentity`의 inventory 자식이 비정상 종료했고, 취소 시험도 `EXECUTION_STOPPED` 전에 `QA_CHILD_FAILED`로 종료됐다. 구체적인 JVM/namespace 시작 원인은 현재 미확정이다. 정상 실행·취소·정리의 해당 검증 성공을 주장하지 않는다.
- 장기 goal 운영 스킬의 유한 반복 원칙에 따라 이번5회 원격 실행을 종료한다. 다음은 shell과 부모 실행기의 환경/경로/mount/자원 한도를 대조하고 원문·secret 비노출 시작 실패 정보를 확보하는 진단이다. 제한을 추측으로 늘리거나 stderr 원문 출력·격리 해제·skip으로 통과시키지 않는다.
- 제목→본문→첨부 텍스트→최종 관리자 검증과 관련 회귀를 유지했다. 중첩 메뉴 정제까지 구현했으나 사이트별 본문 영역·문서 역할 자동 식별(P3), 전체 profile/공식 기대값/Provider QA 화면(P4), 운영 배포·승인된 데이터 적용·브라우저(P6~P8)는 남아 있다. 운영 자동 활성화는 없다.
- 원격 master는 `ae893b87348a9bd1cb0893763f6cad5047093a24` 그대로다. QA 브랜치만 커밋·푸시했으며 과거 migration 수정 없이V73~V81 additive 변경을 보존했다. Word2파일과 사용자 프로세스를 보존했다. 이 최종 결과 기록은 로컬 문서 갱신이며 원격 실행 대상 코드는 변경하지 않는다.

### 2026-09-14 — 2차 본문에 중첩 메뉴 키워드가 섞이는 결함 수정

- `main` 내부 또는 `body` 대체 경로의 `nav/[role=navigation]`를 정제 대상에 포함했다. 의미를 나타내는 명시적 HTML 영역만 제거하며, 일반 문장·실제 본문의 제외 조건·기관명·온라인 신청 링크는 보존한다. 파일명은 기존대로 본문 근거에서 분리하고 상세/첨부 요청 수를 늘리지 않는다.
- 사이트별 공고 본문 전용 영역·문서 역할 자동 식별은 아직 구현 잔여다. 이 공통 정제 수정만으로 P3 전체나 모든 기관 지원을 완료 처리하지 않는다. 기존 DB/API 상태·과거 migration·수집 승인 정책과 자동 활성화 금지는 변경하지 않았다.
- `:test --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementSourceServiceImplTest' --tests '*AnnouncementAttachmentClassificationEngineTest' bootJar --no-daemon --max-workers=1`(명령 한정 Windows-ROOT)은29초 성공했다. 본문21·수집 Service21·첨부 분류19=61건 통과/실패·생략0이다. 현재 진행 중인 Linux `bd92dff` 실행은 이 본문 변경 전 SHA이므로 이 수정의 Linux 실증으로 사용하지 않는다.

### 2026-09-14 — 변경된 처리 흐름 유지·Linux 최초 실증과 실패 수정

- 네 번째 [Actions 34827786609](https://github.com/FrostyCityMan/saneB/actions/runs/34827786609), SHA `bd92dff3068363b8a4c8a2795ce2609cfc3d523d`: 주 Gradle 전체 통과 뒤 독립 실행215건 모두 실행/214통과·1실패/생략·미실행·container실패0, 정리 성공. 실패는1,001건 분할 예약 전수 시험이다. 같은 코드 일반 실행71.123초(직전74.558초)와 독립 기본60초의 충돌을 확인했다. 이 대량 사례만120초로 명시하여 재검증하며 전체600초/DB30초/전수 assertion은 유지한다. 부모 연결 단계는 미실행이고 workflow 전체는 실패다.
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
