# 첨부 수집 구현·출시 진행 기록

기준: `61223aef8db0118cf32d01e4fac16d41b0041868`, V71. 승인 범위는 첨부 설계 구현·검증·운영 반영이다. 기존 설계 문서 변경 5개는 직전 설계 산출물로 보존한다.

## Gate

- [x] 루트 AGENTS 및 첨부 설계·QA 계획 확인
- [~] A: V72 DB 확장·실제 PostgreSQL 검증 완료. DTO·Mapper 연결 대기
- [~] B: IP 고정 다운로드 기반 추가. 프로필 발견·영속 worker·전역 예산 연결 대기
- [~] C: 별도 추출 CLI·합성 fixture 및 공개 실파일 4개 Docker QA 완료. 운영 Linux bwrap/prlimit 검증 대기
- [~] D: 순수 종합 분류기·DB 봉인 보호 검증 완료. 저장·검수 동시성 연결 대기
- [ ] E: v2 API·기존 경로 보호·관리자 화면
- [ ] F: 범위 고정 배치·미리보기·적용·조건부 원복
- [ ] G: 출처별 QA·운영 COLLECT_ONLY canary
- [ ] H: 운영 ENFORCE 및 확인된 범위 적용

전체 설계 구현은 아직 완료되지 않았다. 새 기능은 기존 수집 흐름에 연결하지 않았으며 worker, 정책 게시, ENFORCE를 활성화하지 않았다. 이 상태를 운영 첨부 수집 지원으로 표현해서는 안 된다.

## 필수 검증

기존 migration checksum 보존, 빈 DB/V71 upgrade, 기존 Golden, 첨부 QA ATT-001~062, 기본 test/bootJar, 추출 산출물과 격리 테스트를 분리한다. 미실행을 통과로 표시하지 않는다. 브라우저 검증은 현 요청에 명시되지 않아 사용자 정책상 수행하지 않는다.

## 운영 안전 경계

제목 제외 원문을 복구하지 않는다. 첨부 텍스트는 기존 body_text에 합치지 않는다. 신규 근거만으로 운영 공고를 활성화하지 않는다. OFF는 신규 요청만 멈추며 이미 적용한 첨부 검수 요구를 해제하지 않는다. 보안 격리·DB 무결성 또는 배포 접근 권한이 확인되지 않으면 운영 활성화를 중단하고 실제 차단 원인을 기록한다.

## 2026-09-09 로컬 구현 결과

- `V72__add_announcement_attachment_evidence.sql`: 정책·배치·작업·집합·파일·추출·평가·입력·일치·확정·태그 11개 테이블과 source additive 필드. 복합 FK, current pointer 지연 검사, 봉인·평가 불변성, 목적 분리, cascade 인덱스. 정책 seed/활성화 없음.
- `attachment-extractor`: 웹 bootJar와 분리한 Java 21 CLI. PDFBox 3.0.8, Apache POI 5.5.1, Jackson 2.18.3을 고정했다. HWP는 OLE의 HWP 5 BodyText 레코드, HWPX는 ZIP/XML 제한 reader를 사용한다. hwplib/hwpxlib 후보를 채택한 구현은 아니며 실제 파일 호환성 검증 전의 제한적 reader다.
- `IsolatedAttachmentExtractor`: Linux `bwrap`/`prlimit`, 환경변수 초기화, 제한된 파일시스템, PID/네트워크 namespace, 30초·출력 byte 제한을 구성한다. Windows에서는 검증되지 않은 host 실행으로 우회하지 않고 `ISOLATION_UNAVAILABLE`을 반환한다.
- `AttachmentPinnedDownloadClient`: HTTPS·QA 승인 host 입력·검증 IP 고정·redirect 제한·stream byte 제한. 기존 파일을 덮어쓰거나 오류 정리로 삭제하지 않는다.
- `AnnouncementAttachmentClassificationEngine`: 기존 TITLE/BODY 결과와 별도 판정. 문서·문단 간 AND 금지, FORM/REFERENCE 참고 전용, 불완전·역할 불명·범위 불명·부정 문맥 검수. 첨부 B는 자동 제외가 아니라 검수다. 분석량·일치 건수 상한 초과도 검수로 남긴다.
- 기존 분류 엔진에는 첨부 단일 scope용 진입점만 추가했다. 기존 입력 record와 v1 결과 enum, content hash, Controller, 화면, 운영 데이터는 변경하지 않았다.

## 실행 명령과 결과

PowerShell에서 Java system property 인자는 따옴표로 감싸야 한다. 초기 미인용 실행은 task 이름으로 해석되어 실패했고, 아래 실행으로 정정했다. TLS 검증을 해제하지 않았다.

```powershell
.\gradlew.bat test :attachment-extractor:test attachmentMigrationTest bootJar :attachment-extractor:installDist --no-daemon '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'
```

- 최종 실행: `BUILD SUCCESSFUL`, 2분 46초.
- 기본 test: 120 suites, 580 tests, 실패 0, 오류 0, skip 20. 560건이 실행 통과했다. 조건부 통합 테스트의 skip을 성공으로 계산하지 않는다.
- `attachmentMigrationTest`: 2 tests, 실패/오류/skip 0. PostgreSQL 16.15의 loopback 임시 인스턴스에서 V71→V72, 빈 DB 전체 migration, 기존 checksum 보존, 다른 source 파일 연결 거부, 다른 policy의 set 연결 거부, 미완료 집합 봉인 거부, 봉인 수정 거부, source 목적 변경 거부, current pointer 불일치 거부, source 삭제 cascade를 확인했다.
- 추출 CLI test: 합성 fixture 12 tests, 실패/오류/skip 0. 일반/빈/암호 PDF, HTML 위장, 크기 제한, 한국어 HWP/HWPX 문단, 표 셀 분리, DTD·경로 이탈·압축비 한도·손상 레코드를 확인했다. 실제 기관 원본과 동등한 호환성 증거는 아니다.
- `bootJar`, `:attachment-extractor:installDist` 생성 통과. 배포 묶음·CodeDeploy workflow에는 CLI를 아직 연결하지 않았다.
- Node 일회성 검사: V71 이하 실제 migration 파일 68개가 Git HEAD blob과 동일. 변경 파일 25개에서 secret 고위험 패턴 검출 0. 이 검사는 비밀정보 부재의 절대 보증이 아니라 보조 검사다.
- `git diff --check` 통과. LF→CRLF 안내 경고만 있으며 기존 migration을 수정하지 않았다.

## 외부 상태와 자원 정리

- GitHub API의 현재 master SHA와 로컬 HEAD는 모두 `61223aef8db0118cf32d01e4fac16d41b0041868`이다. 저장된 배포용 계정의 push 권한을 읽기 전용 확인했다. 전역 활성 계정은 변경하지 않았다.
- [!] AWS: 서울 `ap-northeast-2`의 STS 조회가 세션 만료로 실패했다. 사용자에게 재로그인을 요청했다. 운영 서버 환경·첨부 처리·health·migration 상태를 새로 확인하지 못했다.
- Docker 시작/상태 조회는 내부 `dockerInference` 소켓 오류로 실패했다. factory reset, 기존 데이터·소켓 삭제는 하지 않았다. 이 작업에서 시작한 Docker 프로세스만 종료했다.
- Docker 실패를 DB 성공으로 숨기지 않았다. 별도 test-only embedded PostgreSQL 16.15로 migration 검증을 수행했고, try-with-resources로 임시 DB를 종료했다.
- Node 검사와 Gradle은 일회성 실행이며 `--no-daemon`을 사용했다. 사용자 브라우저·다른 프로젝트 프로세스는 제어하지 않았다.
- 커밋·푸시·배포·운영 데이터 갱신은 수행하지 않았다.

## 다음 구현 순서와 남은 출시 Gate

1. 출처별 실제 HTML/파일 QA를 근거로 시스템 profile을 등록한다. 현재 등록/배포된 첨부 profile은 없다. URL·파일명은 분류 입력에 넣지 않는다.
2. DAO/Mapper와 짧은 transaction의 job 예약·lease·heartbeat·재시도·취소를 구현한다. source당/전역 동시성, 재시도 누적 bytes, 총 임시파일 1 GiB·24시간 청소를 DB/파일 수명주기에 연결한다. 현재 다운로드 helper만으로 이 전역 정책이 충족되지는 않는다.
3. Linux 격리 실증: 네트워크·환경변수·다른 파일 차단, OOM/timeout 자식 프로세스 종료, JVM의 512 MiB 제한 내 시작, process-tree 전체 자원 상한을 검증한다. 현재는 command 구성/Windows fail-closed 단위 검증뿐이다. DNS 지연을 포함한 논리 다운로드 전체 deadline과 redirect 누적 시간도 추가 검증·보강해야 한다.
4. 실제 PDF/HWP/HWPX 표본으로 한국어·표·부분 이미지·암호·손상·내장 개체를 확인한다. 현재 PDF의 페이지 text는 추출하되 `scopeReliable=false`이므로 첨부만의 자동 후보 근거로 사용하지 않고 검수로 남긴다. HWP/HWPX reader의 표준 문서 호환성과 ZIP 특수 entry 방어도 추가 QA가 필요하다.
5. 불변 set/evaluation 저장, optimistic version·idempotency, 최신 base/rule/policy 일치, 원문 삭제와 worker 완료 경합을 검증한다.
6. 기존 확정·전환·V69 재분류 경로에 ENFORCE 우회 방지 guard를 연결한 뒤 v2 조회·역할·확정·DRAFT 전환과 관리자 UI를 구현한다. 이 guard가 없는 현재 상태에서 정책 활성화 금지.
7. 정책 DRAFT/검증/게시, 기존 데이터 scope 고정·수집·preview·apply·pause·rollback을 구현한다. 연결된 운영 공고를 자동 변경하지 않는다.
8. CLI·OS 설정·rollback 보호를 배포 묶음에 연결하고 전체 QA Gate 후 제한된 COLLECT_ONLY canary부터 검증한다. 이후 신규 ENFORCE와 승인 범위의 기존 데이터 적용을 진행한다.

브라우저 검증은 사용자 정책에 따라 미실행이다. 최종 릴리스 판정은 `Not ready`다. AWS 인증뿐 아니라 위 미완료 구현/검증이 모두 남아 있어 운영 반영을 진행하지 않는다.

## 기술 근거

2026-09-09 재부팅 후 추가 결과는 [실파일 QA 진행 기록](announcement-attachment-real-file-qa-2026-09-09.md)에 분리했다. Docker는 소켓 경로를 보존·재생성한 임시 기동 상태이며 지속 복구가 아니다. 실제 PDF 2개/HWP/HWPX 각 1개의 다운로드·격리 추출·단일 파일 판정을 확인했지만 운영 worker·저장·API·화면 연결 Gate는 변경되지 않았다. 위의 과거 Git/AWS 상태는 당시 확인 기록이며 현재 운영 상태로 재검증한 값이 아니다.

- [PDFBox 공식 다운로드](https://pdfbox.apache.org/download.html), [보안 공지](https://pdfbox.apache.org/security.html): 3.0.8 선택 근거. 이 프로젝트의 안전한 운영을 증명하는 자료는 아니다.
- [Apache POI 공식 배포](https://poi.apache.org/download.cgi): 5.5.1 OLE container reader와 Apache 2.0 라이선스 확인.
- [embedded PostgreSQL 공식 프로젝트](https://github.com/zonkyio/embedded-postgres): 테스트용 독립 PostgreSQL 사용 근거. 운영 stack에 추가된 서비스가 아니다.
