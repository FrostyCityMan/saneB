# 공식 파일 → worker → 임시 DB → API 검증 연결

## 현재 단계 / Gate

### 2026-09-15 13:14~13:16 KST — 새 설치 코드의 고정 비교·HWP 재검증

배포 `787c594`/CodeDeploy `d-MWENRAGTK`, 설치 웹 JAR `d8696e85c2d7c5415b8a39ea9ccb33f596f264feb5a7add966a62610b3751d74`, 업무 코드 hash `d6e98d08cd6f6f23c059bd15fd7a0f0a7513b13fbc07a7b7efbf235b3ecf9f3c`, 추출기1.0.1/runtime `c5e4e1efe9cdad5e2443e045024cad0c4dc78d007eae6e70c60066638e94ccd6`를 확인했다. 별도 probe hash는 `cc6fbe1b592b83f522c4772054e7f2b0e86165cfc4b70bd8aecf750150ac6032`다. 설치 불변 QA 패키지를 덮어쓰지 않았다.

| 검증 | 실제 결과 | 잔여 경계 |
|---|---|---|
| 태백184816 | SSM `fe2aac98-5851-4bd8-9171-4abe962cf7b7` Success/1통과·생략0. BODY→HWPX2개 COMPLETE_TEXT→worker→임시 DB/API 후 production CaseExecutor로2파일 재다운로드·재추출, PASSED/FIXED_NOTICE_EXPECTATIONS_MATCHED | UNKNOWN 혼합/FORM 유지, FINAL_REVIEW_EXCEPTION·REVIEW_REQUIRED. 정상 공고/정책 QA 성공이 아님 |
| 태백176153 HWP | SSM `b7289058-6eba-4d99-aed6-f33c4c780574` Success/1통과·생략0. BODY AVAILABLE→실제HWP1개/125952bytes/9743자/480블록→worker→DB/API, binary/text hash는12:45 기록과 동일 | PARTIAL_TEXT/UNKNOWN·PARTIAL_FAILED·TECHNICAL_EXCEPTION/ATTACHMENT_INCOMPLETE 유지. 사전 HWP 기대값 비교는 아님 |

태백184816의 비교 입력은 실행 전에 고정한 전체2파일이다. 입력 hash `7233c7f2c36ea291a6a7105beeef9720a59ba695b0318432c36e89691de7aeec`, catalog hash `eb8aa9795d7e3ae83986b05defd16f1d5c042450fb92bb8a70acef3915eeef65`이며 locator/binary/text/block/업무 문구/역할 Assessment를 모두 대조했다. 시험 scope의 전체 seed+전국채널246대상·실행 가능1·정상 공고0·전체 coverage false를 보존했다. 운영 활성 기관 수로 사용하지 않는다. [고정 기대값 기록](announcement-taebaek-fixed-qa-expectation-2026-09-15.md)을 따른다.

합산 요청/bytes는 태백HWPX8회/2658726bytes(고정 재비교 자체3회/280787bytes), HWP4회/2354176bytes다. 각44회/80MiB/420초 이내이며 두 실행 모두 원본/lease/임시PG/전송 정리 성공·운영 DB 쓰기0·설치 JAR 불변이다. 자동 confirmation/link0과 관리자 최종 검증 요구를 유지했다.

시험 보고서에 원문 비노출 `replacementCharacterCount`를 추가했다. HWPX2파일/HWP1파일 모두0이다. HWP 코드의 PARTIAL 경로는 U+FFFD 또는 tag76~88이며, 이번 HWP는 문자 대체 표시가 아닌 후자의 경로로 조사 범위를 좁혔다. 개별 tag와 실제 누락 내용은 아직 미확인이고0건을 완전 추출로 바꾸지 않는다. [한컴 HWP5 명세 표57·74~79](https://cdn.hancom.com/link/docs/%ED%95%9C%EA%B8%80%EB%AC%B8%EC%84%9C%ED%8C%8C%EC%9D%BC%ED%98%95%EC%8B%9D_5.0_revision1.3.pdf)와 대조하면 이 범위에 표와 도형·그림·OLE·수식이 포함되므로 단순 조건 제거가 아닌 구조별 검증이 다음 작업이다. 문자 집계가 null을0으로 바꾸지 않고 원문을 직렬화하지 않는 로컬14건/생략0·Node launcher5건이 통과했다. production 추출기/IPC/DB/API 판정은 이번 진단으로 변경하지 않았다.

전체9Gate/ATT62는 **Not ready**다. 실제 비교1건은 진전이지만 전체 Provider/정상 다중 첨부·형식 QA, 승인된 정책 게시·상시 수집·기존 데이터, 관리자 업무 브라우저 E2E는 남는다. 관리자 탭의 로그인 위치만 확인했고 인증 업무 검증은 실행하지 못했다.

### 2026-09-15 12:45 KST — 실제 HWP 전체 공고 경로

별도 고정 그룹 `TAEBAEK_HWP`에서 [태백176153 공식 공고](https://www.taebaek.go.kr/www/selectBbsNttView.do?bbsNo=25&key=352&nttNo=176153)를 기존 태백 BODY/profile/worker로 처리했다. 양평3건과 태백184816의 분모·기대값 재비교는 그대로다. 임의 URL/ALL을 허용하거나 새 수집 모델을 추가하지 않았다.

- SSM `f066eadd-753f-498b-b0e4-b863c1efec24` Success, JUnit1/1통과·실패/생략/중단/container 실패0이다. 설치 코드는2bde216/추출기1.0.1이며 새 catalog7e1c2de의 배포/정책 QA 성공이 아니다.
- BODY AVAILABLE/1시도, TITLE COMBINATION_MATCHED→BODY ACCEPTED→실제 전체 HWP1개 발견/다운로드125,952bytes/9,743자/480블록→worker→임시 DB→v2 API를 대조했다. HWP signature를 확인했으며 확장자 표시만으로 성공 처리하지 않았다.
- 품질 PARTIAL_TEXT, 역할 UNKNOWN/자동 역할 근거 없음, job PARTIAL_FAILED, processing TECHNICAL_EXCEPTION, effective REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE다. 전체 텍스트 완전성false·관리자 최종 검수 필요를 유지했다. 완전 HWP/정상 후보/최종 승인 성공으로 계산하지 않는다.
- binary hash `a424fffd308588d07a985076d379c4aedfe25c8b4576821065dbffac2c16c1d9`, text hash `730bd283fd4dc93d4f9a271883c35f037182b840b51168a1d190297cafa6f497`, QA locator hash `7a513eb3b13920e81932465c4f23f250b51c9d613e306dab508b9bf17a2ffde9`다. 정책 실행 기대값은 아직 등록하지 않았다.
- 4요청/2,354,176bytes, 상한44요청/80MiB/420초다. 운영 DB 쓰기0·자동 confirmation/link0·lease0·원본/소유 임시 PG/전송 정리 성공·설치 JAR 불변이다. probe hash `d7eebb0d2c9375d5fe049371da5ac5b53ce7f4502982152667e326877dcc95f9`다.
- 새 그룹/정확한1건 분모/기존 profile/임의 입력 거부와 관측 계약의 로컬23건은43초 통과·생략0, Node launcher5건 통과다. production Java/DB/API/catalog/엔진 수는 이 HWP 증분에서 바꾸지 않았다. 직전7e1c2de 전체 회귀2385통과/262조건부 생략과 구분한다.

이 결과로 **실제 HWP의 전체 worker·DB·API 경로와 부분 추출의 검수 전환**을 관측했다. 완전 텍스트 HWP의 같은 경로, 전체 Provider/형식의 사전 기대값, 새 catalog 배포/고정 비교, 승인된 상시 수집·기존 데이터·운영 브라우저 E2E는 남아 있다. 관측한 공식 페이지는 공개 지원계획이며 담당자·전화·원문을 보고서에 복사하지 않았다.

P3 진행, 전체 Gate0~8 / ATT-001~062는 **Not ready**다. 변경된 `제목 1차 → 정제 본문 2차 → 실제 첨부 텍스트 3차 → 관리자 최종 검증` 순서를 보존한다. 이번 변경은 시험·실행 설정·기록이며 production Java/API/UI/schema를 변경하지 않는다. 기존 공식 파일 관측과 별도로 실제 worker 저장·조회 경로를 검증하도록 확장했다.

## 2026-09-15 12:06~12:10 KST — 혼합 역할 사유 확정·태백 양식 자동 식별

운영 웹 JAR/업무 코드/추출기/역할 규칙은 아래11:58 기록과 동일한2bde216·추출기1.0.1·document-role-1.0.2다. 별도 probe에만 고정 역할 사유/일치 규칙 코드/근거 개수를 추가했다. 허용한 코드 외 값은 원문을 반사하지 않고 거부한다. 저장된 assessment를 읽으며 실제 텍스트·파일명·URL·자격증명을 출력하지 않는다. 임시 probe 변경을 운영 애플리케이션 재배포로 표현하지 않는다.

- 양평 SSM `48b5d034-a632-4c49-bf02-7cd5ab0ee8df` Success/3통과·생략0. probe SHA256 `4ce3f68d0c5f3b4603671f042c4fedc935af36f754146d01418150ebb18099d4`다.312241 HWPX의 UNKNOWN은 `MIXED_DOCUMENT_ROLES`이며 NOTICE_HEADING/FORM_HEADING 근거가 서로 다른11블록에11개 저장돼 있다. 현재 명세에 따른 혼합 역할 예외다. PDF 부분/미지원 JPG에는 역할 assessment가 없고, 제목 제외 요청0은 유지한다. 단순 추출 실패나 미확인 버그로 보고하지 않는다.
- 기존 양평3건 기본값은 유지하고 `TAEBAEK` 명시 그룹은 이미 관측한184816 공고1건/HWPX2파일만 실행한다. Java/Bash 양쪽의 고정 그룹 허용, 그룹별 필수 건수·제목 제외 기대 결과·보고서 검증을 적용했다. ALL/임의 URL/새 입력 파일을 받지 않는다. 양평의1건 통과를3건 성공으로 바꿀 수 없다. 원래4인자 Bash/1인자 Java 호출의 양평 기본 동작은 유지한다.
- 태백 한도는44요청/80MiB, 실제 예약 사용은5요청/2377939bytes였다. SSM **`d6fe5871-496b-4412-a073-bccbdddae699` Success**,1/1·실패/생략/중단/container 실패0이다. probe SHA256 `dba3c806a87a857945cb200ce6223bdfdd3c6f4459524c6a2a845d2ff33e7b51`다.

| 태백184816 파일 | 실제 추출·역할·저장 | 의미 |
|---|---|---|
| HWPX1 | 82695bytes/2041자, COMPLETE_TEXT, UNKNOWN/MIXED_DOCUMENT_ROLES, NOTICE/GUIDE/FORM 제목 근거4개·4블록 | 공고/안내/양식 역할이 섞인 문서로 검수 사유 유지 |
| HWPX2 | 67020bytes/1994자, COMPLETE_TEXT, FORM/ROLE_TEXT_STRUCTURE_MATCHED, FORM_HEADING/APPLICANT_FIELD/SIGNATURE_FIELD 근거3개·3블록 | 콜론 없는 입력·괄호형 서명 보완의 실제 파일→자동 역할→DB→API 효과 확인 |

태백 BODY AVAILABLE/1시도, 전체 발견2/처리2, extractor2회, worker EVALUATED/job SUCCEEDED, 본문·전체 첨부 텍스트 완전성true다. 모든 block 신뢰/텍스트 저장·v2 조회·역할 근거 hash 연결을 대조했다. 혼합 파일이 있으므로 FINAL_REVIEW_EXCEPTION/REVIEW_REQUIRED·ATTACHMENT_CONTEXT_REVIEW는 남는다. FORM 파일을 공고 본체로 간주하거나 정상 공고3개 충족으로 합산하지 않는다.

두 실행 모두 운영 DB 미사용/쓰기0·자동 confirmation/link0·lease0·원본/소유 임시 PostgreSQL namespace/전송 패키지 정리 성공·설치 JAR 불변이다. 이 두 관측으로 catalog 기대값을 자동 작성하거나 정책 QA를 통과시키지 않았다. HWP 전체 공고 worker, 나머지 기관·형식·사전 기대값, 관리자 재로그인/업무 E2E, 승인된 활성화·기존 데이터 적용은 남아 있다.

태백 그룹 재현은 기존 launcher에 마지막 고정 인자만 추가한다. 사용자 환경변수·운영 접속값을 상속하지 않는다.

```bash
bash scripts/qa/run-attachment-official-worker-probe.sh "$QA_DISTRIBUTION" "$PROBE_JAR" "$PROBE_SHA256" "$APPLICATION_CODE_HASH" TAEBAEK
```

로컬 표적60건/생략0은36초에 통과했다. 전체 `:test :attachment-extractor:test attachmentContractQaTest bootJar attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1`은4분28초 성공, root2639=2377통과/262조건부 생략/실패0·패키징20/20이다. 추출기/bootJar/probe는 UP-TO-DATE로 새 실행 성공에 합산하지 않는다. Node25통과/2 Linux 전용 생략/실패0, `git diff --check` 통과다. 단발 Node/JVM/소유 임시PG/전송 자원은 종료·정리했고 사용자 output은 보존했다. 로컬 웹 JAR SHA256 `25a83f2dfb5534b3ca05ae5d137a49ee04e578df402794d376d90b5106d0ffa7`는 직전과 같으며 Linux 운영 JAR 지문과 구분한다. 이번 검증 코드의 CI는 별도이며 운영 웹 재배포·정책 활성화·기존 데이터·브라우저 업무 검증은 실행하지 않았다.

## 2026-09-15 11:58 KST — 추출기1.0.1 실제 worker 재검증

SSM **`adee6e78-7976-4046-94e3-c06c6d2941e6` Success**, 고정 양평3건 **3통과/실패·생략·중단·container 실패0**이다. 배포 revision `2bde2161328c9f876eaaddaba374889c9dedf825`, 설치 웹 JAR SHA256 `b31a27aa1267310e9e71401c66c2666c0fc9f82dbe95a6eacbd284244e575ece`, probe SHA256 `40874d4d810564f3930969af94e8d03d5798f05519ca150beeadeaf1286e4242`, 업무 코드 catalog hash `4d7dab7848cb8001d7c18d84b1676e6a8c0d33b9f4422cb4e3682034169eba1f`, 실제 추출기1.0.1/runtime hash `c0b21f3dd591d80acfa2bf7b314cded2178579b413a0e79f4e01a17c51323ba6`다.

| 고정 공고 | 1.0.1에서 직접 확인 | 이전1.0.0 대비 / 남은 쟁점 |
|---|---|---|
| YANGPYEONG-312241 | BODY AVAILABLE/1시도, 전체 HWPX1파일/116740bytes/11398자. COMPLETE_TEXT, 분할 문단 블록1·불확실 블록0, DB/API 일치, job SUCCEEDED | PARTIAL_TEXT→COMPLETE_TEXT, TECHNICAL_EXCEPTION→FINAL_REVIEW_EXCEPTION. 본문·전체 첨부 텍스트 완전성은 true지만 TEXT_RULE 역할 UNKNOWN/ATTACHMENT_CONTEXT_REVIEW로 관리자 검수 필요. 역할 실패의 구체적 원인은 아직 수집하지 않았으므로 추측하지 않음 |
| YANGPYEONG-311846 | BODY AVAILABLE/1시도, PDF237822bytes/4195자 PARTIAL_TEXT·불확실 블록5, JPG BLOCKED/UNSUPPORTED_FORMAT·다운로드0bytes. 전체2파일 분모/DB/API 일치 | job PARTIAL_FAILED, TECHNICAL_EXCEPTION/ATTACHMENT_INCOMPLETE 유지. PDF 이미지·Form 등 어떤 개별 요소가 원인인지 이 보고서만으로 확정하지 않음 |
| YANGPYEONG-311507 | TITLE_COMBINATION_NOT_MATCHED, TITLE_EXCLUDED_NOT_FETCHED, 원문/작업 저장 및 BODY·첨부 요청0 | 제목 제외 정책 유지. 파일 추출 성공으로 합산하지 않음 |

지원2파일의 binary/text hash와 글자 수는 직전1.0.0 관측과 동일하다. HWPX는 연속 문단 구간의 품질·scope 보완 효과가 실제 파일에 반영됐으며, 이 표본에서 텍스트 순서 변경이 발생했다고 주장하지 않는다. 전체 텍스트 완전성, 자동 문서 역할, 최종 관리자 검증은 각각 다른 상태다. 부분/미지원/UNKNOWN을 정상 후보나 정책 QA 통과로 승격하지 않았다.

요청 예약/bytes 상한 사용은 HWPX4회/2533380bytes, PDF 공고4회/2654462bytes, 제목 제외0이다. 사전 한도는 전체132요청/240MiB다. 운영 DB 미사용/쓰기0, 자동 confirmation/link0, lease0, 원본·소유 임시 PostgreSQL namespace·전송 패키지 정리 성공, 설치 웹 JAR 불변을 확인했다. 사용자 관리자 세션은 재배포 후 만료되어 재로그인 대기이며 이 시험은 인증/브라우저 E2E 증거가 아니다.

다음은 UNKNOWN 역할의 구체적인 텍스트 규칙 사유·근거 확인, 실제 HWP 공고 전체 worker 경로와 전체 기관/형식의 사전 기대값이다. 운영 게시·ENFORCE·기존 데이터 적용 승인과 업무 브라우저 검증은 계속 남아 있다. 아래1.0.0 표의 HWPX 부분 추출을 최신 상태로 사용하지 않는다.

## 2026-09-15 이전 — 추출기1.0.0 설치 runtime의 고정 공식 시험

최종 SSM **`31cf27d7-cf0c-49f7-badd-10ffc1eeefcb` Success**, 고정3건 **3통과/실패·생략·중단·container 실패0**이다. 웹 JAR SHA256 `16a1bb74a13e7d88c180fe6f3eb98e97c105c8362592f9c96de572890523f437`, 별도 probe SHA256 `d41a1de17eac91dd9e8aae04fd17bc61ce6358d5800db44ec83bf7cd59e9e01f`, 전체 업무 코드 지문 `ad55e33ab0a04fae4cf76e607ce5bb0db39b1406e3452ba0579ae4b3be35b2bb`다.

| 고정 공고 | 실제 처리 | 최종 의미 |
|---|---|---|
| YANGPYEONG-312241 | BODY AVAILABLE/1시도, HWPX1개 발견·처리/116,740bytes/11,398자, 추출 텍스트 DB·API 일치 | PARTIAL_TEXT/UNKNOWN, TECHNICAL_EXCEPTION·REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE. 정상 완료 후보 아님 |
| YANGPYEONG-311846 | BODY AVAILABLE/1시도, 전체2파일 처리: PDF237,822bytes/4,195자 + JPG미지원 BLOCKED, DB·API 일치 | PARTIAL_TEXT/UNKNOWN 및 미지원 포함. TECHNICAL_EXCEPTION·REVIEW_REQUIRED 유지 |
| YANGPYEONG-311507 | TITLE_COMBINATION_NOT_MATCHED, 원문/작업 저장 전 종료, BODY·첨부 요청0 | 제외 표본이며 파일 추출 성공으로 계산하지 않음 |

지원 파일2건의 실제 추출 결과↔DB 텍스트↔API 전체 필드·블록·종합 상태, 잘못된 원문404/no-store, 자동 confirmation/link0을 확인했다. 두 양성 경로의 요청 예약 상한 사용은 각각4회/2,533,380bytes·4회/2,654,462bytes다. 운영 DB 쓰기0, lease0, 원본/소유 임시 PostgreSQL namespace/전송 패키지 정리 성공, 설치 JAR 불변이다. **정책 QA 기대값 승인·완전 텍스트·전체 Provider·관리자 인증 E2E 통과가 아니다.**

- 운영 V83/SHA476c8f7 배포와 합성221건 서버 격리 시험은 성공했다. 공식 사이트 접속과 전체 파일 worker 처리는 별도 확인한다.
- `attachmentOfficialWorkerProbeJar`는 기존 공식 worker 시험·고정 표본 helper·JUnit launcher만 별도 JAR에 담는다. 웹 JAR와 이미 설치된 불변 합성 QA 패키지를 변경하거나 시험용 라이브러리를 운영 classpath에 추가하지 않는다.
- `run-attachment-official-worker-probe.sh`는 비root, private PID/IPC/UTS namespace, 깨끗한 환경, 소유 임시 작업 경로를 사용한다. 공개 사이트 접속 때문에 이 전용 probe의 네트워크만 공유하며 기존 합성 QA의 `--unshare-all`과 실제 파일 추출기의 별도 네트워크 격리는 그대로다. 운영 home/env/socket은 mount하지 않는다. 시스템의 공개 TLS trust store만 읽기 전용으로 사용한다.
- 기존 JUnit `BeforeAll/BeforeEach/TempDir/AfterAll` 및 사례별420초 timeout을 그대로 실행한다. 전체3건·생략0·실패0·container 실패0 및 고정3개 metadata 보고서가 모두 있어야 성공이다. TITLE 제외1건을 실제 첨부 추출 성공으로 세지 않는다. 공개 텍스트/URL/예외 메시지는 반환하지 않는다.
- 첫 서버 실행 `fe281ad4-0649-4aff-90a9-758521b8880f`는3건 중 제목 제외1건 통과/본문 진입2건 Java 초기화 실패다. 성공으로 표시하지 않으며 원인 예외 자료형/호출 위치로 조사한다. 임시 원본·lease·전송 산출물 정리 및 설치 JAR 불변을 확인했다. 운영 DB 쓰기/정책 게시/ENFORCE는0이다.
- 두 번째 `57ec7a23-2af5-4e9f-9371-16c8557dcbf6`는 원인을 `javax.crypto.JceSecurity.setupJurisdictionPolicies`의 SecurityException으로 특정했다. 디렉터리만 읽기 전용 연결한 세 번째 `3dc2e7de-85a8-4753-97b1-338156fc4b68`도 같은 실패라 반복 실행을 중단했다.
- 고정 runtime 읽기 전용 SSM `44bf41c5-77a8-4947-bd1e-7275f9d0252a`에서 JDK 정책 디렉터리는 실제 디렉터리이고5개 `.policy` 파일이 각각 `/etc/java-21-openjdk/security/policy/...`로 향하는 symlink임을 확인했다. 이5개 설치 파일의 실제 대상을 읽기 전용 mount하고 DB 생성 전 JCE/TLS 초기화를 검사하도록 수정했다. TLS 검증/crypto.policy/운영 JDK 파일/기존 합성 QA 격리를 변경하지 않았다.
- 네 번째 `06d34e92-8986-4863-b452-4d02abd2e999`에서는 JCE 문제가 해결되고 BODY2건 AVAILABLE/각1시도, HWPX116,740bytes/11,398자 및 PDF237,822bytes/4,195자 실제 추출·worker EVALUATED/DB 저장까지 진행했다. 두 파일은 PARTIAL_TEXT이며 API 비교에서 실패해 성공으로 표시하지 않았다. 원본/lease/전송 자원 정리·설치 JAR 불변이다.
- 후속 로컬 회귀로 DTO의 LongNode와 HTTP JSON의 IntNode 직접 비교가 같은 값에도 false인 것을 재현했다. 기대값도 wire serialization 후 비교하도록 수정하고 standalone MVC의 날짜 직렬화를 동일 ISO 설정으로 맞췄다. 필드 누락/숫자 변경은 여전히 실패이며 모든 필드·배열 순서를 유지한다. 표적5건은38초 통과했다. 이 수정은 production JSON/API 변경이 아니다.
- 로컬 최초 컴파일은 JUnit launcher compile classpath 누락으로 실패했고 기존 runtime launcher를 testCompileOnly로 연결한 뒤 표적3건과 JAR 생성이59초 성공했다. Node/Bash14건 통과·기존 Linux 전용2건 생략, 실패0이다. 전체 회귀와 후속 실파일 결과는 완료 후 기록한다.

재현은 `./gradlew attachmentOfficialWorkerProbeJar`로 별도 시험 JAR를 만든 다음, 승인된 비root Linux 환경에서 다음 launcher를 실행한다. 설치 웹 JAR 내부 `attachment-qa-code/catalog.json` SHA256을 코드 지문으로 사용한다. 임의 업무 코드/DB 접속값/URL을 주입하지 않는다.

```bash
bash scripts/qa/run-attachment-official-worker-probe.sh "$QA_DISTRIBUTION" "$PROBE_JAR" "$PROBE_SHA256" "$APPLICATION_CODE_HASH"
```

## 과거 CI 외부 차단 근거

- [Linux 실행36](https://github.com/FrostyCityMan/saneB/actions/runs/34912804386), SHA `0f34f9eeaf805ac604ca4218ee89c68a6caee159`는 공식 관측 단계 실패다. 기본 계약·독립 DB·부모 연결 시험은 성공했지만 전체 workflow는 failure다.
- 양평312241/311846: BODY 각각2시도 TIMEOUT, 이어서 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT. 파일 다운로드·텍스트 추출은 미실행이다. 각각 요청 예약3회, 임시 원본 정리true다.
- 양평311507: TITLE_COMBINATION_NOT_MATCHED, TITLE_EXCLUDED_NOT_FETCHED, BODY/첨부 요청 예약0회다. 제외 표본을 성공 표본으로 바꾸지 않았다.
- 앞선 태백 실행30/31의 시간 초과와 합쳐 CI에서 공식 게시판 접근 실패가 반복됐다. 서버 차단·리전·네트워크 원인은 확정하지 않았다. 같은 접근 조건에서 다른 실행기를 재시도해도 성공 근거가 되지 않으므로 자동 재실행하지 않는다.
- Windows에서 과거 공식 BODY/다운로드가 성공한 사실은 해당 시점·클라이언트의 증거다. Linux 추출 또는 실제 worker 성공으로 대체하지 않는다. 현재 WSL에는 docker-desktop만 있으며 별도 개발 Linux 배포판은 없다. Docker engine pipe는 앞선 확인에서 없었다. 시스템 재설치·보안 완화·운영 환경 변경은 하지 않았다.
- 원시 파일/본문을 GitHub artifact로 옮겨 실패를 우회하지 않는다. 운영 AWS 재인증 응답과 승인된 실행 환경의 접근 상태 확인이 남아 있다.

## 구현한 검증 경로

1. `AnnouncementAttachmentOfficialWorkerIntegrationTest`는 기존 BBS 관측의 고정 양평3표본을 재사용한다. 임의 URL/기관 또는 ALL 입력을 제공하지 않는다. TITLE 제외 표본도 분모에 남기고 DB 원문·작업 예약 전 종료한다.
2. 최신 Flyway 전체를 적용한 **직접 소유한 loopback 임시 PostgreSQL**만 사용한다. 외부 DB 접속 정보를 받지 않는다. 실행용 ACTIVE 규칙·ENFORCE 정책 fixture는 이 임시 DB에만 생성하고 정책 QA/운영 게시 성공으로 보고하지 않는다.
3. TITLE은 현재 seed 규칙, BODY는 실제 `LocalGovernmentNoticeProviderContentClient`와 공통 분류기를 사용한다. BODY A/B·부족·실패 결과로 첨부를 중단하지 않는다. 실제 BODY/evaluation은 `AnnouncementSourceClassificationPersistenceServiceImpl`의 transaction/DAO/Mapper를 통해 저장한다.
4. 저장 후 현재 baseEvaluation/sourceVersion/attachmentVersion을 다시 읽어 job을 예약한다. 실제 `AnnouncementAttachmentWorkerServiceImpl`, download gateway·lease/예산, 기관 profile, signature 검사, Linux 격리 추출기, `document-role-1.0.2`, 평가 저장 경로를 사용한다. HTTP/추출 응답을 합성 파일로 교체하지 않는다.
5. 모든 첨부의 DB file/quality/role/extraction 연결과 추출 텍스트 일치, 역할 block hash, 실제 Controller/Service/DB의 JSON·블록 조회·다른 원문 접근404·no-store를 검사한다. standalone MockMvc이므로 인증/CSRF/브라우저 E2E는 아니다.
6. JPG 미지원 파일은 `BLOCKED/UNSUPPORTED_FORMAT`으로 전체 목록에 남는다. 임의 다운로드 차단을 미지원 성공으로 처리하지 않는다. 지원 파일의 추출 누락, BODY 실패, 전체 발견 수 변경은 실패다. 부분/OCR/미지원/UNKNOWN을 정상 후보로 승격하지 않는다.
7. 최종 검수/공고 link가 자동 생성되지 않았는지 검사한다. 관리자 최종 확인 필요를 유지하며 실제 DRAFT 생성·운영 사용자 동작을 이 시험으로 대체하지 않는다.
8. 공고당44요청/80MiB, 3건 전체132요청/240MiB 상한이다. BODY2요청/2MiB 상한을 먼저 예약한다. worker 자체 예산·DB transaction 밖 HTTP/추출·자원 lease를 우회하지 않는다.
9. 실제 텍스트/파일명/URL/HTTP 헤더/예외 원문을 로그·JUnit/artifact로 내보내지 않는다. opt-in JVM은 기존 logback OFF 설정을 사용하고 실패는 단계와 고정 코드로 보고한다. DB/API 텍스트 비교는 boolean assertion으로 확인한다. metadata만 남기고 임시 원본·lease·소유 DB·HTTP 자원을 정리한다.

## 검증 종류와 완료 기준

| 검증 | 의미 | 현재 상태 |
|---|---|---|
| 준비 계약 | 합성 BODY의 실제 분류 저장 → 현재 버전 job 예약 | 통과, 공개 파일 성공 아님 |
| 기존 worker 통합 | 합성 HTTP/PDF/HWP/HWPX → 실제 격리 추출/DB/API/검수/DRAFT | 기존 Linux12건 통과, 공개 파일 성공 아님 |
| 신규 공식 worker | 실제 BODY/파일 → worker/DB/API | 고정 양평3건 서버 통과. 지원 파일2건은 부분 텍스트·미완료 상태 보존 |
| 정책 QA 기대값 | 전체 대상/profile/형식/다중 파일의 사전 승인 기대값 비교 | 미완료, catalog 기대값0 |
| 운영 적용/E2E | 동일 SHA 배포·승인 범위 데이터·관리자 브라우저 | V83 설치·읽기 전용 관리자 화면 확인. 활성화/배치 적용·업무 E2E 미완료 |

`WORKER_DB_API_OBSERVED_NOT_APPROVED`는 고정 표본의 실행·저장·조회 연결을 관측했다는 의미다. `isWholeTextAnalysisComplete`는 BODY/모든 첨부 텍스트의 완전성만 나타낸다. 정책 QA/기대값 승인/전체 정상 후보/최종 검수 성공은 별도다. `TITLE_EXCLUDED_NOT_FETCHED`를 첨부 추출 성공으로 계산하지 않는다.

## 실행

2026-09-15 최종 로컬 검증은16분3초 성공이다. root2631=2369통과/262조건부 생략/실패0, 별도 실제 job192·패키지20·원래 Flyway3은 실패/생략0이다. 신규 준비1건·workflow10건도 root에서 통과했다. Node20통과/2 Linux 전용 생략이다. bootJar/추출기는 UP-TO-DATE이며 production JAR SHA256은 `19b1d2fb08bde9e29a9523e41e2a8352a3ad8e68bbf5e98b0a55826867f90d40`다. 사용한 단발 Node/JVM/임시 PG가 종료되었다. 공식 worker 자체 실행과 현재 SHA Linux/운영 검증의 성공 근거가 아니다.

일반 `test`는 공식 worker 환경값을 false로 고정한다. 합성 BODY 준비 시험은 실제 임시 PostgreSQL을 사용하고 외부 HTTP/파일 추출을 하지 않는다.

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentOfficialWorkerPreparationTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest attachmentJobIntegrationTest flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar installAttachmentContractQa --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-contract-release.test.mjs
```

공식 사이트 접근 상태가 바뀌거나 승인된 Linux 실행 경로가 확보된 뒤에만 아래를 실행한다. CI 입력 `verify-official-worker`는 기본false/수동 dispatch만 허용하며 push 표식은 제공하지 않는다. artifact에는 고정3 metadata JSON과 해당 JUnit만7일 보관한다.

```bash
bash ./gradlew attachmentOfficialWorkerIntegrationTest --no-daemon --console=plain --max-workers=1
```

기존 migration V1~V83, v1 API, 운영 정책·데이터·배포를 변경하지 않았다. 엔진6/profile17/전용 BODY16기관/형식3, catalog 참조24/기대값0과 미연결 기관 분모를 유지한다. `output/` 사용자 Word2개를 보존한다.
