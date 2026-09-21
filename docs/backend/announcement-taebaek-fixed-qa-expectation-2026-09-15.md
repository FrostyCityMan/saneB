# 태백 전체 첨부 고정 기대값 연결

## 새 기대값 실제 재비교 — 2026-09-22

서울 임시 SSM `66eb8090-64c9-4947-b343-0e103bf98894`가 Success/관측과 별도 JUnit1/1·실패/생략0이다. 새 catalog의 production `AttachmentProviderQaCaseExecutor`가 실제 전체HWPX2파일을 사전 기대값과 비교해 `PASSED/FIXED_NOTICE_EXPECTATIONS_MATCHED`를 반환했다. locator/binary/text/blocks/필수 문구/역할 지문 일치와 원본 정리를 확인했다. 상태는 `FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED`이며 UNKNOWN+FORM·정상0·coverage false·정책 QA false를 보존했다.

앞선 관측 사용량을 공제한39요청/81,508,141bytes 상한으로 실행했고 실제3요청/280,787bytes다. 관측+비교 합계8요청/2,658,726bytes이며 원 승인44요청/80MiB 이내다.26초 이내 종료·CPU1/메모리768MiB/임시1GiB·운영 JAR불변·health UP·원본/프로세스/전송 및 S3 정리를 확인했다. 운영 DB쓰기0이며 운영 설치·정책·worker·기존 데이터는 변경하지 않았다. [정확한 코드 지문과 실행 근거](announcement-seoul-temporary-bbs-qa-2026-09-21.md)를 따른다. 전체 수집처/형식·운영 E2E 완료는 아니다.

## 서울 임시 최신 코드 재관측 — 2026-09-22

- [서울 격리 QA](announcement-seoul-temporary-bbs-qa-2026-09-21.md)의 SSM `10df4968-07d1-49ce-b2c3-d768003c9f9c`가 Success/관측 JUnit1통과·실패/생략0이다. 시각 `2026-09-21T16:32:11.928279006Z`, application code catalog hash b53a0384…이며 운영 설치물은 교체하지 않았다.
- 제목 조합 충족 → BODY431자/AVAILABLE → HWPX2개 COMPLETE_TEXT/전체 텍스트 분석true다. UNKNOWN/MIXED_DOCUMENT_ROLES(2041자/62블록)와 FORM(1994자/117블록), REVIEW_REQUIRED/ATTACHMENT_CONTEXT_REVIEW는 기존과 같다.5요청 예약/2,377,939bytes(본문 상한 포함), 운영 DB 쓰기0·원본/임시 전송/프로세스 정리true다.
- 사전 expectation의 전체2파일 locator/downloadAllowed/format/binary/quality/문자·블록/역할 규칙/text/blocks/assessment hash를 각각 대조해 일치했다. 필수 문구는 변경하지 않았으며 이전 검토와 동일한 전체 텍스트 hash를 확인했다. 원문을 새 문서/로그로 복사하지 않았다.
- 이전 관측 코드9d5eaae..b5a125a의 production diff는 StandardBbsAttachmentDiscoveryProfile의 BBS redirect 최초 경로·전체 식별 파라미터 고정3행이다. 추출기/역할 규칙/태백 본문·첨부 선택자는 변경되지 않았다. 성공한 새 관측의 profileHash8cf428f1…와 현행 코드 일치를 확인했다.
- catalogVersion을 `2026-09-22-taebaek-seoul-revalidated-v2`, profileHash를 `8cf428f1ca718f67960130dc0398aa354679179d3cc2638cd6d47f51ffc17e88`, observedAt을 위 시각으로 갱신했다. metadata3필드 외 차이0을 구조 대조하고 구627d3f60…의 자동 재연결 거부 회귀를 추가했다. 구지문9aea97d…/8a93cf4…도 유지한다.
- 현재 저장된 기대값1/전체 참조30/정상 후보0·coverage false·정책 QA false다. catalog 갱신 후 새 production CaseExecutor 고정 비교와 전체 CI는 별도 필요하다. 운영 정책 게시·worker·기존 데이터·업무 브라우저 E2E 승인/완료를 뜻하지 않는다.
- 갱신 후 표적159건/패키징20건 실패·생략0,1분34초 성공과 새 bootJar를 확인했다. 전체 로컬 회귀도4분24초 성공(root2751=2486통과/265조건부 생략/실패·오류0)이다. 추출기88·패키징20 결과는 전체 호출에서 재사용했으며 새 시험으로 중복 합산하지 않는다.

## 중단된 전체 로컬 회귀 결과 확인 — 2026-09-21

09-16 14:23 최종 XML의 root2731=2466통과/265조건부 생략/실패·오류0을 회수했다. 옛 실행 핸들과 Java/PG 프로세스는 없으며, 동일 소스의 전체 Gradle 명령은31초/23task 모두 UP-TO-DATE로 성공했다. 패키징20·추출기88도 기존 결과 재사용이며 현재 새 실행으로 합산하지 않는다. 고정 기대값의 관측 시각과09-23T05:06:32Z 만료는 연장하지 않았다. 새 커밋의 Linux 계약·실제 고정 비교는 별도 확인한다.

## DNS 수정 후 재관측 — 2026-09-16 14:06 KST

- `9d5eaae67f70660e55e2d00633396fe388ce143b`의 [Linux35058110396](https://github.com/FrostyCityMan/saneB/actions/runs/35058110396)에서 고정1공고 재관측 단계와 metadata 보관이 성공했다. 관측 JUnit1건/실패·오류·생략0이다. 전체 계약 단계의 terminal 결과와는 구분한다.
- `2026-09-16T05:06:32.672171396Z` 관측은 TITLE 조합 충족→BODY AVAILABLE/431자·1시도→전체 HWPX2파일/COMPLETE_TEXT였다. 파일1은2041자/62블록·UNKNOWN/MIXED_DOCUMENT_ROLES, 파일2는1994자/117블록·FORM/ROLE_TEXT_STRUCTURE_MATCHED다. 전체 텍스트 완전성true이나 REVIEW_REQUIRED/ATTACHMENT_CONTEXT_REVIEW·최종 관리자 검증을 유지한다.5요청 예약/2,377,939bytes(본문 상한 포함)·원본 정리true·운영 쓰기0이다.
- 기존 expectation의 전체2파일 locator/downloadAllowed/format/binary/quality/문자·블록 수, 역할 규칙/텍스트/전체 블록/assessment 지문을 대조해 전부 일치를 확인했다. requiredPhrases는 변경하지 않았고 동일한 전체 텍스트 hash가 이전 문구 검토의 유지 근거다. 원문은 로그·문서로 복사하지 않았다.
- 이전 관측 코드 `cc79d59..9d5eaae`를 검토했다. production 변경은 충주 전용 발견·본문 연결과 공통 전송기의 알려진 DNS 실패 분리이며, 태백 선택자·성공 다운로드 경로·HWPX 추출·역할 규칙 변경은 없다. 실제 관측의 profileHash는 로컬 수정본과 같은 `627d3f602e4f1255556906409999a7b38dd72bef98e2fe7da46792d8c524e572`다.
- 검토 후 catalogVersion/profileHash/observedAt3필드만 갱신했다. 전체30참조·파일 내용 기대값·역할·문구·예산은 불변이다. 고정 literal/현행 코드 hash 동시 대조를 유지하고 구지문9aea97d…와8a93cf4… 모두 PROFILE_CHANGED/실행0이 되는 회귀를 보존·확장했다. 새 catalog의 표적·전체 회귀와 production CaseExecutor 실제 재비교는 별도 결과로 판정한다.
- 갱신 후 표적133건·패키징20건/실패·오류·생략0,2분48초 성공이다. 웹 JAR는 새 catalog를 포함해 생성했으며 SHA256 `843b7a4b208226b0685eabd7983bd93fe9dc5b79d839e45e972943775d336dbf`다. 전체 로컬 회귀는 후속 실행 중이다.
- 관측을 실행한35058110396 전체 CI는 예상했던 구catalog/현행 profile 지문 불일치1건으로 failure 종료했다. XML root2730=2463통과/266생략/실패1, 추출기88·패키징20·job192·migration17·runtime1·worker12·Flyway3 실패/생략0이다. 독립 산출물/정책 부모 단계는 앞선 실패로 생략됐고 필수 보고서 판정도 실패다. 실제 관측1건 성공을 전체 CI 성공으로 바꾸지 않는다. `build/qa-results/run-35058110396-contracts/`에 보관했다.

관측 metadata/JUnit은 Git 제외 `build/qa-results/run-35058110396-taebaek-observation/`에 있다. 전체 정상 공고0·coverage false·정책 QA false이며 운영 배포·worker 활성화·정책 게시·기존 데이터 실행은 없다.

## 후속 고정 비교 확인 — 2026-09-16

`f7b7396`의 [Linux34944248951](https://github.com/FrostyCityMan/saneB/actions/runs/34944248951)는 success이며 보관 JUnit의 고정 비교1건/실패·오류·생략0을 확인했다. production CaseExecutor 결과는 `PASSED/FIXED_NOTICE_EXPECTATIONS_MATCHED`, 전체 HWPX2개 모두 COMPLETE_TEXT/기대값 일치다. 전체3요청 예약/280787bytes·원본 정리true·운영 쓰기0이다. catalog27참조/실행 가능1/정상0·전체 coverage false·정책 QA false와 관리자 검수 상태를 유지한다. 아래16:33의 “새 SHA 재비교 필요”는 이 실행으로 해당1공고에 한해 해소됐다. 전체 Provider/운영 배포·업무 E2E를 완료한 것은 아니다.

## 최신 재관측 — 2026-09-15 16:33 KST / 구기대값 내용 보존

- `cc79d597ed34c3549f380e90162383f86b570093`의 [Linux34941434590](https://github.com/FrostyCityMan/saneB/actions/runs/34941434590)는 성공이다. 보관 XML은 root2676=2411통과/265조건부 생략/실패0, 추출기88·패키지20·job192·migration17·worker12·runtime1·정책 부모2·Flyway3 실패/생략0이다. 공식 태백 관측은 별도1건/실패·오류·생략0이다.
- 관측 시각 `2026-09-15T07:33:03.129802080Z`, BODY AVAILABLE/완전성true, 전체 HWPX2개/COMPLETE_TEXT를 확인했다. 첫 파일2041자/62블록·UNKNOWN/MIXED_DOCUMENT_ROLES, 둘째1994자/117블록·FORM/ROLE_TEXT_STRUCTURE_MATCHED다. 최종 REVIEW_REQUIRED/ATTACHMENT_CONTEXT_REVIEW로 관리자 검증을 유지한다.5요청/2377939bytes·원본 정리true·운영 DB 쓰기0이다.
- 구catalog의 전체2파일과 locator/binary/quality/최소 문자·블록/역할 규칙/텍스트/전체 블록/assessment hash를 대조해 변경0을 확인했다. requiredPhrases도 기대값에서 그대로 보존했다. 같은 전체 텍스트 hash가 일치한다는 근거이지 원문을 새 로그·문서에 저장한 것이 아니다.
- `787c594..cc79d59`의 소스를 직접 검토했다. 공통 최초 요청 승인 overload와 철원 전용 모드가 추가돼 지문은 바뀌었지만 태백의 기존 모드·헤더/다운로드 규칙과 HWPX 추출·역할 분류 코드는 변경되지 않았다. 최신 실제 태백 HWP worker/DB/API 검증과 별도로 이번 전체 HWPX 관측을 수행했다.
- 검토 후 catalogVersion을 `2026-09-15-taebaek-revalidated-v2`로 갱신하고 태백 expectation의 profileHash를 `8a93cf41a20e0b3ae3a93c441d4db196762fbe972a71fb15e87136fe69d90044`, observedAt을 위 시각으로 변경했다. **파일 기대값/역할/문구/전체 참조/상한은 한 항목도 바꾸지 않았다.** Node 구조 대조에서 허용한 metadata3필드 외 변경0을 확인했다.
- 구9aea97d… 지문의 거부 회귀는 역사적 fixture로 유지했다. 새 packaged catalog는 전체27참조/실행 가능1/정상 공고0·전체 coverage false·정책 QA false를 검증한다. 관측 성공을 새 catalog의 production CaseExecutor 통과로 대체하지 않는다. 새 SHA에서 `verify-bbs-fixed-case=true`를 별도 실행해 같은 고정 기대값을 다시 비교해야 한다.

자료는 `build/qa-results/run-34941434590-contracts/`와 `build/qa-results/run-34941434590-bbs/`의 metadata/JUnit이다. 운영 설치는 별도이며 정책 게시·ENFORCE·worker 활성화·기존 데이터 적용은 없다. 아래13:14의 운영 고정 비교는 과거1.0.1 코드의 증거다.

## 최신 검증 — 2026-09-15 13:14 KST / 첫 사전 기대값 실제 비교 통과

- [x] 동일 SHA `787c594` Linux34926361587·배포34927650269/CodeDeploy `d-MWENRAGTK` success. 설치 JAR/case catalog와 DB V83 확인은 [운영 기준선](../deployment/attachment-runtime-baseline-2026-09-15.md)을 따른다.
- [x] SSM `fe2aac98-5851-4bd8-9171-4abe962cf7b7` Success, 태백184816의 BODY→전체 HWPX2파일→worker→임시 DB/API를 검증하고 production `AttachmentProviderQaCaseExecutor`로 전체2파일을 별도 재다운로드·재추출했다. JUnit1/1통과·실패/생략/중단/container 실패0이다.
- [x] 고정 비교 `PASSED/FIXED_NOTICE_EXPECTATIONS_MATCHED`, 파일2/2 PASSED. 사전 locator/binary/text/block/문구/역할 Assessment hash가 모두 일치한다. 첫 파일 UNKNOWN/MIXED_DOCUMENT_ROLES, 둘째 FORM을 유지한다. 실행 결과로 catalog를 덮어쓰지 않았다.
- [x] 고정 비교 자체3요청/280,787bytes, BODY+worker+비교 합산8요청/2,658,726bytes. 상한44요청/80MiB/420초 이내, 원본/lease/소유 임시 PG/전송 정리 성공·운영 DB 쓰기0·설치 JAR 불변이다.
- [~] 전체 seed+전국채널의 시험 계획246대상은 보존됐다. 이는 운영 활성 기관 수가 아니다. catalog 실행 가능1·정상 공고0·전체 기대 coverage false·정책 QA false다. 공고 처리 상태도 FINAL_REVIEW_EXCEPTION/REVIEW_REQUIRED로 관리자 검증이 필요하다.

관측 `2026-09-15T04:14:28.998937685Z`, 비교 종료 `2026-09-15T04:14:42.544450020Z`다. probe SHA256 `cc6fbe1b592b83f522c4772054e7f2b0e86165cfc4b70bd8aecf750150ac6032`, catalog hash `eb8aa9795d7e3ae83986b05defd16f1d5c042450fb92bb8a70acef3915eeef65`, 현재 runtime hash `c5e4e1efe9cdad5e2443e045024cad0c4dc78d007eae6e70c60066638e94ccd6`, 고정 입력 hash `7233c7f2c36ea291a6a7105beeef9720a59ba695b0318432c36e89691de7aeec`다.

이제 이1건의 사전 기대값 실제 재비교는 완료다. 전체 Provider/정상 공고/형식 QA·정책 게시·상시 활성화·기존 데이터·관리자 업무 E2E는 여전히 미완료이며 전체9Gate/ATT62는 **Not ready**다. 아래12:28의 “새 배포/재비교 미실행”은 당시 기록이다.

## 최신 증분 — 2026-09-15 12:28 KST 관측과 사전 기대값 등록

- [x] 서울 서버의 설치 코드 `2bde216`/추출기1.0.1에서 태백1건을 다시 관측했다. SSM `97f6ed09-0bf2-42c5-8b13-97aa408efa1b` Success, JUnit1통과/실패·생략0이다. probe SHA256은 `94694704b2f1e04e35388d7383439fd85335050d0b94586992c5a70ef0cba72d`다.
- [x] BODY AVAILABLE/1시도, 전체 HWPX2개/완전 텍스트/worker SUCCEEDED/DB·API 일치, 전체 텍스트 완전성true다. 운영 DB 쓰기0·원본/lease/임시 PG/전송 정리 성공·설치 JAR 불변이다. 총5요청/2,377,939bytes이며44요청/80MiB 상한을 유지했다.
- [x] 실제 텍스트·구조 근거와 고정 업무 문구의 존재 여부를 검토했다. 첫 파일은 NOTICE/GUIDE/FORM 제목4근거가 공존하므로 UNKNOWN/MIXED_DOCUMENT_ROLES, 둘째는 신청서 제목·신청인·서명3근거의 FORM이다. 첫 파일의 서명 문구 없음과 둘째의 있음도 보존한다. 정상 공고로 승격하지 않는다.
- [x] catalog v2에 이1공고의 **전체2파일**을 별도 고정했다. 총24항목 중 참조전용23/기대값1이며 정상 공고 수0·전체 coverage false·정책 QA false다. v1 catalog와 V1~V83은 변경하지 않았다.
- [x] 최초 실제 expectation의 `Instant`를 일반 ObjectMapper가 읽지 못해 snapshot 시험12건이 실패했다. catalog 소유 복사본에 JavaTimeModule을 명시하여 해결했다. 호출자 설정과 중복 키/미지원 필드/후행 JSON 거부는 유지한다. 보완 후 표적135건, 실행자 자원 한도 포함 최종137건은 실패/생략0이다.
- [x] 정적 계획에서 같은2파일/품질/역할/문구/현재 지문, 전체 대상 보존, 정상 수0, 미래·7일 만료·제목 제외 규칙 변경 시 준비/예약 불가를 확인했다. 실제 seed/실사이트 재실행과 구분한다.
- [~] 태백 worker probe의 마지막에 production `AttachmentProviderQaCaseExecutor` 재다운로드·재추출 비교를 연결했다. 실행 중 기대값 생성/수정은 없고 BODY+worker+재검증 합계44요청/80MiB 및 전체420초를 유지한다. 새 catalog 설치 후 실제 결과로 완료한다.
- [ ] 새 SHA Linux/배포 및 고정 기대값 실제 재검증. 현재 관측 성공은 그 뒤에 고정한 기대값의 PASS가 아니다.

| 첨부 | 최소 문자/블록 | 역할 기대값 | 직접 확인한 업무 문구 |
|---|---|---|---|
| 첫 HWPX | 2,041 / 62 | UNKNOWN / MIXED_DOCUMENT_ROLES | 청년농업인, 취업농, 신청서 |
| 둘째 HWPX | 1,994 / 117 | FORM / ROLE_TEXT_STRUCTURE_MATCHED | 청년농업인, 취업농, 신청서, 서명 |

관측 시각 `2026-09-15T03:28:40.008199613Z`, profile hash `9aea97d1281dd778ba6d6f331fd7dd147fef2a05f6de7132ccd90c28b28a58e5`, 역할 `document-role-1.0.2` 및 전체 binary/text/blocks/assessment 지문을 catalog에 고정했다. `locatorHash`는 QA 실행기의 canonical JSON 해시이며 worker 저장용 직렬화 해시와 혼동하지 않는다. 실행기와 canonical hash 일치 회귀를 추가했다. 원문·담당자·연락처·원본 파일은 문서/로그에 저장하지 않는다.

이 증분은 정책 게시·ENFORCE·worker 활성화·기존 데이터 적용 승인이 아니다. HWP 공고 전체 경로의 부분 추출 관측은 아래12:45 후속 기록을 따른다. 완전 텍스트 HWP, 나머지 기관·정상 다중 첨부/형식 기대값과 운영 업무 E2E는 남아 있다. 아래 Run30/31의 기대값0은 당시 상태다.

최종 로컬 전체 회귀는4분39초 성공이다. root2647=2385통과/262조건부 생략/실패0, 패키지20/20, Node25통과/2 Linux 전용 생략이다. 추출기/bootJar/probe는 UP-TO-DATE이며 이 호출에서 새로 실행한 시험으로 합산하지 않는다. 별도 앞선 bootJar 생성은 성공했고 로컬 웹 JAR 지문은 `2dd20b2b3004f7716bf864123c3bed421b0fe05d70d786b277b0c6f4aad56474`다. 새 Linux·실제 배포 지문은 아직 확인 전이다.

12:45 KST 후속: [태백176153 공식 공고](https://www.taebaek.go.kr/www/selectBbsNttView.do?bbsNo=25&key=352&nttNo=176153)는 기존 태백 BODY/profile로 실제 HWP signature·전체1파일·worker·임시 DB·API를 확인했다. 125,952bytes/9,743자/480블록이며 PARTIAL_TEXT·UNKNOWN·TECHNICAL_EXCEPTION·REVIEW_REQUIRED다. SSM `f066eadd-753f-498b-b0e4-b863c1efec24`는1건 통과/생략0, 운영 DB 쓰기0이다. 완전 추출이나 사전 기대값 비교 통과는 아니며 HWP 기대값도 아직 등록하지 않았다. [공식 worker 기록](announcement-official-worker-db-api-qa-2026-09-15.md)의 원본·임시 자원 정리와 실행 경계를 따른다.

## 범위와 합격 기준

- P3 실제 파일 QA 증분이다. 제목 → 정제 본문 → 전체 첨부 텍스트 → 최종 관리자 검증을 유지한다.
- `TAEBAEK-184816`의 사전 관측을 검토한 뒤 catalog에 파일 2개 모두의 locator/binary/text/blocks/role-assessment hash와 역할 규칙 버전을 고정한다. 실행 중 관측한 값으로 기대값을 자동 덮어쓰지 않는다.
- 혼합 문서의 UNKNOWN은 검수 사유다. 양식 FORM 판정 성공 또는 고정 기대값 일치를 정상 후보·정상 다중 첨부 공고·전체 정책 QA 통과로 계산하지 않는다.
- 기존 실제 세 단계 관측과 별도로 production `AttachmentProviderQaCaseExecutor`의 고정 공고 계약을 재실행한다. 후자는 BODY 분류·worker DB/API·관리자 검수 E2E를 대체하지 않는다.
- 새 검증의 계획 범위는 임시 PostgreSQL의 저장소 seed 전체(비활성 포함, 미삭제)와 두 전국 채널이다. 실제 운영 대상/활성 상태를 나타내지 않는다. 미구현 기관과 기대값 없는 참조를 분모에서 보존한다.
- 외부 실행은 관측 `attachmentBbsOfficialFileObservation`과 고정 계약 `attachmentBbsFixedCaseQa`를 별도 opt-in task로 분리한다. 각각 최대 420초·44요청·80MiB다. 같은 CI에서 둘 다 명시 실행하면 순차 합산 상한은 88요청·160MiB다. 기본값은 둘 다 OFF이며 일반 단위시험은 외부 호출하지 않는다. 원본은 종료 시 정리하고 metadata만 남긴다.
- 운영 DB·정책·수집 플래그·기존 데이터 변경, 자동 ACTIVE, schema/v1 계약 변경은 없다. V1~V83은 보존한다.

## 검증 체크리스트

- [x] Run30의 같은 SHA Linux 결과와 원본 정리를 검토했다. 전체 실행은 실패다.
- [!] 현재 profile/역할 지문 및 파일 2개 전체 기대값 연결은 새 실제 추출 관측 부재로 보류한다. catalog 참조18/기대값0을 보존했다.
- [x] production 실행기에 고정 catalog·전체 seed 범위·임시 저장·현재 runtime을 연결하는 시험을 구현하고 컴파일했다. 실제 파일 실행 통과는 아니다.
- [ ] 정상 수 0·전체 coverage 미완료·7일 만료 및 지문 변경 차단을 검증한다.
- [ ] production 실행기로 실제 전체 파일을 다시 비교하고 결과/예산/정리 metadata를 확인한다.
- [x] 표적116건/생략0/37초 통과. 전체 회귀3분27초 성공: root2556=2299통과/257조건부 생략/실패0, QA 패키지20/20. extractor/bootJar/설치 task는 UP-TO-DATE이며 새 실행으로 합산하지 않는다.
- [ ] 새 SHA의 Linux 관측 결과·원격 SHA·고정 기대값 연결 후 실제 단건 재검증을 확인한다.

검증 명령은 `:test --tests '*AnnouncementAttachmentBbsFixedCaseQaContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest'`와 `:test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa`이며 모두 `gradlew.bat --no-daemon --console=plain --max-workers=1`로 실행했다. production JAR SHA256은 변경 전과 같은 `868e0aaaf9fc656985c5e9facd12435a7ec201ebb11d28f68c241ed16e4e769c`다. Node 일회성 검사로 catalog18참조/기대값0 보존을 확인하고 종료했다. 운영/브라우저 검증은 이번 회차 미실행이다.

## 관측 실패와 후속 조치

[Run30](https://github.com/FrostyCityMan/saneB/actions/runs/34902614936), SHA `72a78c89149267cdacffe2d0ce73c9a5035294b4`의 관측 시각은 `2026-09-14T22:21:10.553004673Z`다. TITLE은 COMBINATION_MATCHED였으나 BODY가 2회 모두 TIMEOUT/FETCH_FAILED였다. 파일 발견/다운로드/추출은 실행되지 않았다. 원본 정리true·운영 쓰기0·본문 상한 예약2요청/2MiB다. 현재 태백 profile hash는 `23c1c1af20689d40c47684f1bf3a36090656181572ff305dc0405d04d5f6a28b`지만 지문 관측만으로 파일 기대값을 만들지 않는다.

본문 실패를 먼저 기록하고 남은 예산 안에서 첨부 발견·추출을 진단하도록 관측 시험을 보완했다. 마지막 BODY 완전성 검증은 유지하므로 파일이 성공해도 본문 실패를 전체 성공으로 승격하지 않는다. 실패/빈 본문/비활성에 대한 음성 시험을 추가했다. 운영 수집기나 timeout/재시도 설정은 바꾸지 않았다.

사전 기대값은 여전히0이므로 `[fixed-bbs-qa]` 또는 해당 수동 입력을 켜지 않는다. 후속 실행은 `[official-bbs-observation]`으로 위 진단 개선만 검증한다. 새 실파일·텍스트·역할 관측을 검토한 뒤 기대값을 별도 변경하고 production 단건 실행기 시험을 켠다. 기대값이 없거나 만료/변경됐다면 실행기는 HTTP 전에 준비 실패를 보고해야 한다.

## 실패 및 잔여 경계

외부 원문/파일/역할 지문 변경, 누락, 미실행, 만료, 다운로드/추출/정리 실패는 실패 또는 미완료다. 기대값을 즉석에서 바꿔 성공으로 만들지 않는다. 고정 계약 PASS는 전체 기관·형식·worker·운영 배포·승인된 기존 데이터 처리·운영 브라우저 Gate 완료가 아니다.

## 2026-09-15 후속 접근 결과

[Run31](https://github.com/FrostyCityMan/saneB/actions/runs/34904550080), SHA `0625b13fb25ddf7001f5e5848cb46087ae01edfc`도 전체 실패다. `2026-09-14T22:45:25.818698755Z`의 TITLE 조합 통과 → BODY 2회 TIMEOUT/FETCH_FAILED → base REVIEW_REQUIRED/BODY_FETCH_FAILED 보존 → DETAIL_DISCOVERY의 TRANSPORT_TIMEOUT을 확인했다. 첨부 발견/다운로드/추출은 미실행이다. 상한 예약3요청/2MiB, 원본 정리true/운영 쓰기0, profile hash는 Run30과 같다.

같은 회차 Windows의 기존 `attachmentProfileDiscoveryQa --tests '*StandardBbsBodyContentLiveQaTest.readsMeasuredOfficialBodyWithoutRequestingFiles'`는 기본 Java 신뢰 설정에서3건 NETWORK_ERROR였다. 저장소의 기존 Windows-ROOT/NUL 인자를 명시한 뒤에는 태백184816·횡성424078·영월157016 모두 AVAILABLE·시도1·redirect0으로3/3 통과(전체18초/HTTP 시험2.088초)했다. TLS 검증을 끄지 않았다. 이것은 로컬 BODY 접근 근거이며 Linux 접근·첨부·운영 성공 근거가 아니다.

두 Linux 실행에서 본문과 별도 pinned 상세 요청이 모두 시간 초과했으므로 단순 parser 문제로 단정하지 않는다. 실행 환경별 네트워크/사이트 접근 차이는 추정이며 세부 원인은 아직 미확인이다. 같은 CI 공식 표본을 즉시 반복하지 않는다. 기존 서울 AWS 검증 환경의 재인증 또는 외부 접근 상태 변경 후 다시 확인한다. 원래 범위의 다른 구현·필수 테스트는 계속 진행하고, 현재 파일 기대값0과 전체 미완료 상태는 보존한다.
