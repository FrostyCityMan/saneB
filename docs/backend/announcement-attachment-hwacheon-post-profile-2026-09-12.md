# 화천 POST 첨부 프로필·실패 근거 연결

## 단계와 적용 범위

Gate 2·6 부분 진척이며 전체 **Not ready**다. 기존 기업마당/대전 서구/새올 GET4기관에 화천군 전용 프로필을 추가하여 시스템 등록은 7개다. 전체 수집원 지원, 격리 텍스트 추출, 운영 DB 저장·정책 게시·ENFORCE 성공을 뜻하지 않는다.

근거: `announcement-attachment-collection-design-2026-09-08.md` 4.3·5.3·6.1과 ATT-004/011~014/020~024/028. 관련 코드·테스트는 `discovery/HwacheonPostAttachmentDiscoveryProfile`, `AttachmentDownloadInvocation`, `AnnouncementAttachmentDiscoveryEvidenceTest`다.

## 고정 시스템 계약

| 항목 | 허용 범위 |
|---|---|
| provider / source | LOCAL_GOV_NOTICE / LGS-000130 |
| 기존 목록 profile | SAFE_SAEOL_EMINWON_LEGACY |
| 새 첨부 profile | LOCAL_HWACHEON_POST_V1 |
| host / transport | eminwon.ihc.go.kr / HTTPS 443만 |
| 상세 GET | `/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do` |
| 상세 query | context=NTIS, homepage_pbs_yn=Y, jndinm=OfrNotAncmtEJB, method=selectOfrNotAncmt, methodnm=selectOfrNotAncmtRegst, subCheck=N, 1~15자리 숫자 not_ancmt_mgt_no. 정확히 7개 |
| 첨부 POST | 같은 host의 `/emwp/jsp/ofr/FileDownNew.jsp`, query 없음 |
| 폼 | nnn의 method=post·고정 action·정확히 4개 hidden 필드. user_file_nm/sys_file_nm/file_path 초기값 없음, isHome=Y |
| 첨부 영역 | form1[method=post] 내 `첨부파일` td의 마지막 인접 td |

- source의 provider·source code·목록 profile·정규화 URL hash를 모두 확인한다. HTTP 전환, 다른 기관 fallback, 쿠키·로그인·임의 JavaScript 실행은 없다. POST redirect는 공통 transport에서 전부 차단한다.
- 공식 `goDownLoad`의 세 불투명 인자를 해독하거나 의미를 추정하지 않는다. 문자열 자체를 메모리에서 네 필드 POST에 넣고 UTF-8 form encoding을 한 번만 적용한다. 인코딩 본문은 기존 8 KiB 상한이다.
- 불투명 사용자/시스템 값은 각각 최대 2,048 UTF-16 단위이며 제어문자·경로 이탈·금지 구분자를 거부한다. 현재 공개 형식의 base64 유사 접미부만 검증하며, 암호 알고리즘이나 영구 안정성을 단정하지 않는다. file_path도 확인한 `/ntisho` 형식만 허용한다.
- locator에는 고정 profile/path, 공고 ID와 불투명 경로+시스템 값의 SHA-256만 남긴다. 요청 원문/폼 값/인증정보는 DB·문서·감사·오류에 저장하지 않는다. 값이 교체되면 locator hash가 바뀌어 기존 checkpoint나 고정 재시도 범위를 임의 재사용할 수 없다.
- anchor 표시명으로 형식 후보를 확인하지만 문서 역할은 UNKNOWN이다. 기존 관리자 MANUAL 역할은 재시도 계약으로 보존한다. 공고 자동 활성화는 없다.
- 정상 폼과 첨부 영역이 확인되고 파일이 0개일 때만 NO_FILES다. 누락/중복 폼, 바뀐 필드, 미해석 링크/잔여 내용은 실패다. 발견한 일부 파일은 유지하며 10개 초과는 LIMIT_EXCEEDED다. 비지원 형식은 발견 목록에 남기고 다운로드하지 않는다.

## 공통 호출 해석과 실제 실패 수정

1. 긴 문자열을 기존 반복 정규식으로 처리한 단위 회귀에서 StackOverflowError가 발생했다. `AttachmentDownloadInvocation`으로 대전 서구·새올 GET·화천의 고정 호출을 선형 순회로 해석하도록 변경했다. 전체 호출 8,192 단위, 인자 3개/각 2,048 단위 상한이며 재귀 regex·JS 실행·추가 명령·표현식·임의 escape는 허용하지 않는다. 서구만 기존 bare call/return false 형태를 허용한다.
2. 화천 다중 파일 실측 33895에서 짧은 불투명 사용자 값 때문에 최초 QA가 실패했다. 공식 응답의 폼과 세 값 형식을 다시 확인하고 관측한 짧은 접미부를 허용하는 회귀를 추가했다. 파일 수 기대값을 줄이거나 실패한 표본을 제거하지 않았다.
3. 새 `ATTACHMENT_DOWNLOAD_FORM_CHANGED`가 저장 서비스 허용 목록에서 빠져 있었다. 해당 고정 코드만 추가했고 DAO 대역 저장·worker 전달·불완전 발견·임의 코드 거부를 검사한다. PG 불변 manifest/조회 테스트에도 새 코드를 추가했으나 실제 PG 실행은 별도 미완료다.

공통 해석 class도 `AttachmentProfileFingerprint`에 포함한다. **7개 프로필의 지문을 현재 산출물 기준으로 재고정하고 정책 초안/전체 QA를 다시 진행해야 한다.** 이전 고정 hash를 조용히 갱신하지 않으며 운영 정책 변경은 실행하지 않았다.

## 실패 표시와 호환성

기존 attachment-sets의 `warningCodes` 배열/불변 manifest 구조를 유지한다. URL·본문·폼 원문 대신 고정 오류 코드를 저장한다. 새 폼 오류와 기존 상세/selector/링크/파일 한도 오류에 각각 관리자 한글 설명을 추가했다. 폼 변경은 “다운로드 폼 변경 · 시스템 수집 방식 재검증 필요”이며 “첨부 없음”과 구분한다. 관리자에게 파서 편집 권한을 제공하는 기능은 아니다.

V1 응답 구조·V1~V72 migration·DB schema 변경은 없다. 로컬 additive V73~V75 역시 이번 증분에서 수정하지 않았다. 발견 실패는 원문 직접 확인 사유이며 추출 텍스트만으로 자동 해제하지 않는다.

## 실제 공개 파일 검증

2026-09-12 03:31 KST 전용 task가 공식 상세 6건·첨부 10개를 기존 pinned downloader와 signature 식별기로 처리했다. 조회한 비식별 보고서의 결과는 다음과 같다.

| 기관 | 공고 ID | 파일 수 | signature | 파일 byte 합계 |
|---|---:|---:|---|---:|
| 부산 남구 | 46034 | 4 | HWPX 4 | 383,297 |
| 대구 달성군 | 53932 | 1 | HWPX 1 | 89,664 |
| 대구 중구 | 34295 | 1 | PDF 1 | 47,368 |
| 함안군 | 43065 | 1 | HWPX 1 | 44,879 |
| 화천군 | 33897 | 1 | HWPX 1 | 11,649 |
| 화천군 | 33895 | 2 | HWPX 1 + PDF 1 | 63,689 |

합계 640,546 byte, PDF 2/HWPX 8. 상세 6회+첨부 10회. 모두 임시 HTML/binary 정리 확인. 화천은 두 공고·세 파일 75,338 byte이며 모두 실제 POST 다운로드였다. 공식 공개 파일의 수집 구조 표본이며 제목 분류 통과 후보를 뜻하지 않는다.

실제 텍스트 추출·HWP 실파일·DB 쓰기·운영 활성화는 이 task의 범위가 아니다. 확장자/signature만으로 문서 전체 파싱 성공을 주장하지 않는다. 각 요청 30초/파일 20 MiB/공고 80 MiB/사례 180초, 단일 JVM이다. 현재 metadata 파일은 ignored `build/reports/attachment-profile-discovery-qa/{profileCode}-{noticeId}.json`이며 이전 profile-only 파일과 섞어 최신 실행 건수를 계산하면 안 된다.

## 검증 명령과 남은 Gate

- 표적 `:test --tests 'com.saneb.domain.announcementattachment.service.AnnouncementAttachmentDiscoveryEvidenceTest' --tests 'com.saneb.domain.announcementattachment.worker.AnnouncementAttachmentWorkerServiceTest' --tests 'com.saneb.domain.announcementattachment.discovery.*'`: 03:38 KST 22초 성공. 실제 네트워크/DB 대역을 사용한 worker 회귀는 운영 E2E가 아니다.
- `node --test scripts/qa/attachment-{review,recovery,policy,operations,batch,backfill}-ui.test.mjs scripts/qa/attachment-contract-report.test.mjs`: 파일 7개를 명시적으로 열거하여 실행, 117통과/실패·생략0. 브라우저 렌더링 검증은 아니다.
- 전체 `:test :attachment-extractor:test bootJar :attachment-extractor:installDist attachmentProfileDiscoveryQa --rerun-tasks`: 03:41 KST **2분56초 성공/17 task 전부 재실행**. root192 suite/1740건 중 **1544통과·196조건부 생략**, 추출기25통과, 별도 실사이트6사례 통과, 실패/오류0. 최신6사례도 위 표와 동일한10파일/640,546byte·임시 원본 정리를 확인했다.
- 새 실패 근거 저장6건, worker26건, 고정 호출4건이 통과했다. job PG159 표기·전체 분할PG12·migration2는 전부 생략이다. 조건부 비활성인 parameterized PG 메서드는 개별 입력으로 펼쳐지지 않을 수 있으므로 XML 생략 건수를 전체 실제 실행 사례 수로 해석하지 않는다. 새 폼 코드의 DB 불변/조회 검증은 실행하지 못했다.
- JAR SHA-256: `a0ed8c38a038fa2e0b5742dc16a9b19fe188218808579eb45c6cc95a767a843c`. 기존 MockBean 제거 예정/unchecked/Log4j provider 경고는 남아 있다.
- 03:44 변경 class6개/한글 표시 JS1개의 JAR 내부 bytes와 로컬 빌드/원본 일치, 기본 `git diff --check` 종료0. Java/이번 QA Node 프로세스0 및 실행 handle 종료를 확인했다. 브라우저는 이번 하위 작업에서 실행하지 않았으며 운영 브라우저 Gate는 남아 있다.
- Gradle 공통 인자: `--no-daemon --max-workers=1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL'`. TLS 검증 해제/전역 설정 변경은 없다.

잔여 필수 범위: 나머지 Provider/profile, 최신 Linux 격리 worker/실파일 텍스트/PG·migration·동시성, 정책 전체 QA/게시 UI·정확한 범위 승인, 전체 기존 데이터 실행/대조, 커밋·푸시·동일 SHA 운영 배포·운영 역할 브라우저 E2E. 7개 등록 또는 6개 다운로드 표본을 전체 완료로 치환하지 않는다.

03:41 GitHub 실조회는 pull=true/push=false, 원격 master는 로컬과 같은 `ae893b87348a9bd1cb0893763f6cad5047093a24`다. Docker Linux 엔진 named pipe 부재, WSL 배포판은 docker-desktop만 재확인했다. AWS/운영 health·현재 SHA·관리자 세션은 이번 미조회다. 운영에 이번 코드를 반영했다고 보고하지 않는다.
