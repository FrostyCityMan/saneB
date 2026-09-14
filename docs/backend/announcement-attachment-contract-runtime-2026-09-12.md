# 독립 첨부 worker·DB 계약 QA 산출물

## 범위와 현재 판정

- 목표: 소스 checkout이나 운영 DB 접속값 없이, 현재 코드와 Flyway를 별도 Linux JVM·임시 PostgreSQL에서 검증한다.
- 현재 결과 범위는 `SYNTHETIC_WORKER_DB_CONTRACTS_V2`다. 고정 합성 HTTP 입력에 실제 worker·Linux 추출·DB·조회/검수 서비스를 연결한다. 전체 Provider 실파일, 상시 scheduler, 실제 권한 필터, 정책 게시, 운영 브라우저를 증명하지 않는다.
- 별도 산출물·실행기·CI 연결을 구현했다. Windows에서 패키징과 실행기 자체 테스트를 확인하며, 실제 Linux namespace/DB 실행은 아직 미확인이다.
- 정책 QA에 성공 JSON을 가져오는 endpoint를 추가하지 않았다. WORKER_DB_RECOVERY는 부모 소유 namespace/임시 DB와 실제 실행·정리 검증기에 연결했다. PROVIDER_PROFILES 연결과 실제 Linux 통합 성공은 필수 미완료다. `announcement-attachment-policy-qa-bridge-2026-09-12.md`를 따른다.

## 산출물과 실행 경로

```text
build/install/attachment-contract-qa/
  bin/run-attachment-contract-qa.sh
  config/logback-qa.xml
  config/logging.properties
  config/hosts
  lib/saneb-attachment-contract-qa-0.0.1-SNAPSHOT.jar
  lib/<기존 testRuntimeClasspath 의존성>
  extractor/<현재 attachment-extractor installDist 전체>
```

`installAttachmentContractQa`는 파일을 빌드 디렉터리에 준비할 뿐 운영에 복사하거나 실행하지 않는다. 별도 QA JAR에는 현재 main class/resource와 다음 네 테스트 클래스·그 내부 클래스만 포함한다. JUnit/embedded PostgreSQL 의존성은 QA 산출물에만 있으며 웹 `bootJar`의 런타임 의존성에는 추가하지 않았다. 새 라이브러리 버전도 도입하지 않았다. 추출기와 합성 입력의 패키징 바이트가 현재 빌드와 같은지 별도 테스트로 검사한다.

| 고정 suite | 09-12 컴파일 산출물에서 발견한 테스트 수 |
|---|---:|
| AnnouncementAttachmentJobIntegrationTest | 168 |
| AnnouncementAttachmentMigrationTest | 2 |
| AnnouncementAttachmentBackfillIntegrationTest | 13 |
| AnnouncementAttachmentWorkerIntegrationTest | 8 |
| 합계 | 191 |

이 수는 **발견 수**다. 이번 Linux DB 실행 통과 수는 0이며, 문서의 과거 159개 job 테스트 수를 현재 전체 수로 사용하지 않는다. 기존 세 suite 내부의 Service/MyBatis/DB 계약과 대역 사용 범위를 그대로 유지한다. 새 worker suite의 HTTP만 합성 대역이며, 실제 네트워크 다운로드·운영 E2E를 주장하지 않는다.

빌드 및 실행기 자체 검증:

```powershell
.\gradlew.bat attachmentContractQaTest installAttachmentContractQa --no-daemon --max-workers=1
java -cp 'build/install/attachment-contract-qa/lib/*' com.saneb.qa.AttachmentContractQaMain --inventory
```

`--inventory`는 어느 OS에서나 고정 suite를 발견하지만 실행하지 않는다. 결과는 `INVENTORY_ONLY`, passed=0, notRun=191이다. 정부24 실제 출처 코드의 범위·전체 목록 고정·미지원 profile 차단 사례를 추가했고 migration suite는 V78까지 확인하도록 확장했다. 일반 실행은 Linux·정해진 작업 경로·빈 환경 조건을 먼저 검사하고 Windows에서는 `LINUX_REQUIRED`/종료 1로 거부한다. URL, DB 접속값, 클래스 선택, 성공 JSON, 원본 경로를 명령 인자로 받지 않는다.

승인된 Linux 환경에서 비root 사용자로 실행하는 명령:

```bash
bash build/install/attachment-contract-qa/bin/run-attachment-contract-qa.sh
```

배포된 별도 산출물이라면 실제 설치 디렉터리의 같은 스크립트를 사용한다. 이 문서는 운영 업로드·실행 또는 인프라 변경을 이미 수행했다는 뜻이 아니다.

## 합성 HTTP·실제 worker 통합 범위

`AnnouncementAttachmentWorkerIntegrationTest`는 실제 `AnnouncementAttachmentWorkerServiceImpl` → 다운로드 gateway → 격리 추출기 → transaction service/DAO/Mapper/Flyway PostgreSQL → 조회·검수·역할 변경·DRAFT 경로를 사용한다. HTTP client는 신뢰한 고정 HTML/바이너리만 반환한다. gateway의 목적지 승인·heartbeat·자원 lease·byte 예약 callback은 실행하며, HTTP와 추출 중 DB transaction 부재도 검사한다.

| 사례 | 직접 검사하도록 작성한 조건 |
|---|---|
| 세 형식·비지원 | PDF/HWP/HWPX 품질·한글 block, 비지원 파일 미다운로드, 전체 4개 저장, 실제 조회 서비스의 HTTP 직렬화와 source 불일치 404 |
| 역할·재검수·DRAFT | 역할 수정 후 네트워크/추출 재실행 0, 기존 확인 무효화, 새 검수, DRAFT 생성·중복 연결 방지 |
| 부분 실패 | OCR 필요·부분 텍스트·암호화·404 다운로드 실패를 모두 보존하고 수동 검수 요구 |
| 재시작 | 503 후 RETRY_WAIT, 새 worker가 성공 checkpoint를 재사용하며 실패 파일만 다시 다운로드 |
| 삭제 경합 | 실제 추출 도중 원문 삭제 후 늦은 결과 저장·원문 재생성 금지 |
| 본문 실패 | `FETCH_FAILED`인 기본 판정을 직접 seed하고 정상 첨부 처리 후에도 본문 실패 근거 보존 |
| 제외 경합 | 예약 후 원문을 EXCLUDED로 변경하면 실제 worker의 HTTP/추출 0·job CONFLICT·조회 차단 |
| COLLECT_ONLY | 첨부 근거를 저장하되 기본 판정·검수 binding·운영 공고/연결을 변경하지 않음 |

8개 모두 Linux/PG **미실행**이다. 정책과 actor/원문/기본 판정은 테스트가 소유한 임시 DB에 직접 seed하며 실제 정책 게시·사용자 인증·본문 수집기 성공을 대신하지 않는다. 역할 변경/검수는 실제 서비스 호출이고, MockMvc는 standalone이므로 Spring Security 필터·CSRF/운영 역할 E2E가 아니다. 외부 서버/DNS/redirect는 이 suite의 검증 대상이 아니며 별도 실사이트 QA가 필요하다.

전용 Gradle task `attachmentWorkerIntegrationTest`는 worker 조건만 true로 설정하고 현재 추출기 설치 경로를 전달한다. 일반 `:test`는 조건을 false로 고정하여 이 8건을 생략한다. 생략을 통과로 세지 않는다. 테스트 간에는 해당 클래스가 직접 만든 임시 DB의 원문/연결만 초기화하여 앞 사례의 대기 작업이 뒤 사례에 섞이지 않게 한다. 운영 DB 선택 경로는 없다.

## 실행·격리·정리 계약

1. 사용자 인자 0개, Linux, 비root, 설치된 Java/bubblewrap/prlimit/timeout을 요구한다. Java는 현재 선택된 설치의 실제 경로를 해석해 읽기 전용 `/jre`로 제공한다.
2. `env -i`와 `bwrap --clearenv`로 상속 환경을 비운다. 고정 PATH/LANG/HOME/TMPDIR 및 job/migration/worker 세 테스트 조건만 전달한다. 운영 자격증명·Spring DB/config 설정·JUnit 조건 해제·외부 추출기 경로 입력은 거부한다. 검사 후 실행기가 읽기 전용 `/qa/extractor`를 내부 지정한다.
3. 별도 network/PID/IPC/사용자 namespace, capability 제거, 부모 종료 시 정리를 요청한다. `/home`, `/opt`, 운영 environment file/socket은 공유하지 않는다. 시스템 실행 파일·라이브러리와 필요한 비기밀 계정/Java 설정 파일만 읽기 전용으로 제공한다. 고정 `/etc/hosts`에는 `127.0.0.1 localhost`만 제공하며 운영 hosts/resolver 설정을 복사하지 않는다. 네트워크는 private namespace의 임시 PostgreSQL loopback용이며 Provider를 호출하지 않는다.
4. 작업 디렉터리는 이 실행이 만든 `/tmp/saneb-contract-qa.*` 하나다. 테스트들은 기존 embedded PostgreSQL 코드로 자체 DB를 만들고 종료한다. 운영 DB URL을 선택하는 분기는 없다.
5. 전체 600초+강제 종료 여유5초, 프로세스별 주소 공간2 GiB/CPU480초/open files256/process128/file128 MiB, Java heap384 MiB/단일 CPU를 제한한다. 이 값은 합성 DB QA용이며 기존 문서 추출기의 512 MiB/30초 제한을 변경하지 않는다. 파일당 한도는 전체 임시 디렉터리 용량 제한과 다르며 실제 Linux 최대 사용량은 아직 미측정이다.
6. 실패/timeout도 namespace 종료 후 검증한 소유 임시 경로만 정리한다. 정리가 끝난 뒤 결과를 출력한다. 원본·DB 디렉터리·SQL/예외 원문을 artifact로 업로드하지 않는다.
7. 실제 namespace 생성, native PostgreSQL 로딩, 메모리·공간·시간 상한 적합성, timeout 자손 종료·정리는 Linux 실행으로 검증해야 한다. 스크립트 문법/소스 검사만으로 이를 통과 처리하지 않는다.

## 결과 판정

09-12 후속: 보고서 `reportSchemaVersion=2`에 자식 JVM이 검증한 executionCodeHash와 실제 Linux 실행의 extractorRuntimeHash를 추가했다. 둘 다 실행 전후 대조한다. inventory의 extractorRuntimeHash는 null이며 업무 코드 확인만으로 실행 성공을 표시하지 않는다. 정책 부모 run/lease와 schema4의 artifact/전체 사례·지문 고정, 취소/정리·저장 증거 검증을 연결했다. 실제 Linux 연결 검증2사례는 전용 `attachmentPolicyDbQaIntegrationTest`로 실행해야 하며 이 PC에서는 아직 미실행이다. `announcement-attachment-policy-qa-bridge-2026-09-12.md`를 따른다.

JUnit Launcher는 정확한 네 suite만 선택하고 병렬 실행/자동 확장을 끈다. 발견 목록의 모든 테스트가 끝나야 `PASSED`다. 실패·중단·생략·미실행·container 초기화 실패·예상 밖 동적 등록·중복 완료 이벤트는 실패다. suite 누락/0건도 실패다. 의도적인 실패 fixture는 실행기 자체 테스트에서만 사용하며 고정 DB QA 산출물에는 넣지 않는다.

출력은 scope, 실행 UUID/시각, 전체 QA lib와 스크립트/config 및 추출기 산출물의 전후 SHA-256, suite별 집계와 case ID hash/상태다. 테스트 display name·예외 원문·DB 주소·원문 문서를 출력하지 않는다. 코드/의존성/실행 스크립트가 바뀌면 artifact hash가 바뀌며 이전 결과를 현재 실행으로 사용할 수 없다. hash만으로 실제 실행 성공이나 정책 QA 승인을 주장하지 않는다.

기존 [JUnit Launcher API](https://junit.org/junit5/docs/5.11.0/user-guide/index.html#launcher-api)의 발견·실행·listener 계약을 사용한다. 문서의 Launcher 기능 설명은 검증 경로 선택 근거이며 이번 Linux 실행 증거가 아니다.

## CI 및 잔여 작업

`.github/workflows/attachment-contract-qa.yml`의 수동 Ubuntu 작업에 독립 산출물 자체 테스트·빌드·실제 스크립트 실행을 연결했다. 누락됐던 정책 화면 Node 테스트도 포함했다. 기존 읽기 전용 권한, 운영 secret 부재, 30분/worker1 제한, XML 보고서의 생략·실패 차단을 유지한다. 현재 GitHub 쓰기 권한이 없어 이 변경을 원격에 반영하거나 실행하지 못했다.

- [ ] 이 산출물을 실제 Linux에서 실행하여 현재 191개 계약과 native 프로세스·정리·중첩 namespace를 검증한다.
- [~] 실제 worker → 격리 추출 → DB 저장/실패 복구 8사례와 실행 경로를 작성했다. 실제 실행·경합/오류 결과와 전체 요구 범위 대조는 남는다.
- [ ] 전체 대상 profile의 실제 파일 QA를 완성한다. 일부 등록 profile 성공으로 전체를 대체하지 않는다.
- [ ] 정책 snapshot/run/코드/설치 지문·취소·예산·현재성에 결합한 두 추가 QA 단계의 실제 실행과 검증을 연결한다. 현재8분 lease에 별도600초 runner를 그대로 끼워 넣지 않는다.
- [ ] 동일 SHA 배포·정확한 범위 승인 후 운영 적용·기존 데이터 전체 처리·역할별 운영 브라우저 E2E를 완료한다.

이번 산출물은 CI 권한이 없을 때도 기존 승인 Linux 환경에 검증 코드를 전달할 수 있는 경로다. 새로운 유료 환경이나 운영 업무 DB에서의 테스트를 요구하지 않는다.

## 09-12 06:48 검증 기록 — 현재 V2 범위

- 전체 강제 회귀/bootJar/추출기 installDist/QA 패키징3분8초 성공: root1658통과/215생략·extractor25·QA12. Node129와 Bash 문법 검사도 통과했다. 마지막 조건부 테스트 정리 assertion 및 패키징 byte 검사 보강 후 재컴파일/QA12재검증은25초 성공했다.
- worker8은 작성·컴파일만 확인했고 Linux/PG에서는 실행하지 않았다. fixture 자체2건·workflow5건·QA 실행기9/패키징3건과 구분한다.
- 최종 목록190건 발견/passed0/notRun190. artifact hash `03bfd22a7b7510e6365e0b7d4f666279a3050e57819adb9318276a9accea94f7`.
- WSL 설치 목록은 docker-desktop만 확인했다. 같은 Docker 기동/AWS 인증 실패는 재시도하지 않았고 운영 변경/원격 CI/배포/브라우저는 미실행이다. 다음 단계는 실제 Linux 검증이지 목록 발견의 성공 처리나 합성 사례 수 증가가 아니다.

## 09-12 06:20 검증 기록 — 이전 V1 범위

- 실행기 판정9건·실제 패키징2건=11통과/실패·생략0. 고정 DB 목록 발견182/실행0과 별도다.
- workflow 구조5건, Node7파일129건 통과. Bash 문법 검사 성공, 새 스크립트 LF 및 배포 산출물 CR 부재 확인.
- 전체 강제 회귀/bootJar/installDist는3분7초 성공: root1656통과/207생략, 추출기25통과. 최종 QA 코드/패키징 재빌드·11건 재검증도 성공했다.
- 최종 QA 전체 artifact hash: `3bef217b7d15f83a6b01ddffc224d35cc0e593326af9d2c715bf4b4b84449ac8`.
- 초기 내부 fixture 중복 탐색, root task 접두 누락, JAR CopySpec 필터 범위 문제를 수정하고 재검증했다. 초기 실패 명령을 성공으로 재기록하지 않는다.
- Docker 정상 기동 시도는 엔진 응답 부재/파일 접근 오류로 실패했고 이번에 시작한 자원만 정리했다. AWS 재인증 필요/GitHub push=false가 확인돼 Linux·원격 CI·운영 실행은 미검증이다. 정책 게시·ENFORCE·기존 데이터는 변경하지 않았다.
