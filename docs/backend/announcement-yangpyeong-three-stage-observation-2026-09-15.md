# 양평 고정 표본의 제목·본문·첨부 격리 관측

## 목표 / Gate

P3 실제 파일 검증을 위한 실행 경로다. 기존 태백1건 관측 코드를 재사용하여 양평3건을 명시적으로 선택한다. 새 사이트 엔진·운영 worker·정책을 추가하지 않는다. 전체 Gate/ATT는 **Not ready**다.

`고정 공식 제목 입력 → DRAFT TITLE 판정 → 정제된 실제 BODY → 공식 첨부 발견 → 지원 파일의 실제 격리 추출 → 종합 판정 → 관리자 검증 필요 표시` 순서다. 실 운영 ACTIVE 규칙, 새 목록 수집 run, worker→DB/API/UI 성공과는 구분한다.

## 현재 확인한 입력과 사전 판정

2026-09-15 공식 상세3건에서 각각 HTTP200과 단일 자체 제목을 확인했다. .NET HTTP 클라이언트는 redirect0/15초/응답1MiB 상한을 사용했고 응답은 메모리에서 제목만 확인한 뒤 해제했다. 본문·첨부 원본·자격증명을 저장하지 않았다.

| 고정 공개 표본 | 이전 발견 목록 | 최신 임시 DB DRAFT 제목 사전 판정 | 이번 후속 요청 |
|---|---|---|---|
| [312241](https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo=312241) | HWPX1 | COMBINATION_MATCHED | 본문·전체 첨부로 계속 |
| [311846](https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo=311846) | PDF1/JPG1 | COMBINATION_MATCHED | 본문·전체 첨부 확인, JPG는 미지원 분모 유지 |
| [311507](https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo=311507) | PNG1/PDF1 | COMBINATION_NOT_MATCHED | TITLE_EXCLUDED_NOT_FETCHED, 본문·첨부 요청0 |

이전 목록은 [기관 모델 관측](announcement-yangpyeong-bbs-profile-2026-09-15.md)의 참고값이다. 제목 제외 후 실제 파일 목록을 다시 확인한 것으로 해석하지 않는다. 제목3건을 현재 운영 규칙으로 판정했다고도 보고하지 않는다. 테스트를 통과시키기 위해 제외 표본을 다른 공고로 바꾸거나 seed/정책을 수정하지 않는다.

## 실행 경계

1. `sanebBbsObservationGroup`은 TAEBAEK 또는 YANGPYEONG만 허용한다. 기본 태백1건의 기존 assert와 공식 URL/profile을 보존한다. ALL·임의 URL·임의 profile 입력은 받지 않는다. 기존 태백 타임아웃을 재실행하지 않는다.
2. 양평은 기존 `LOCAL_YANGPYEONG_BBS_V1`, LGS-000110, HEURISTIC_NOTICE의 고정3원문 identity를 사용한다. 제목은 검증한 `div.p-wrap.bbs.bbs__view > table.p-table.block`의 자체 span 하나와 비교하며 다른 제목/중복/중첩 구조 혼동을 거부한다.
3. 제목 제외 시 BODY 예산 예약 전 종료한다. BODY A/B·부족·실패는 가능한 첨부 관측까지 계속하지만 BODY 실패를 전체 관측 성공으로 바꾸지 않는다.
4. 공고당 최대44요청/80MiB, 선택 그룹3건 전체 상한132요청/240MiB다. BODY 최대2시도/2MiB를 먼저 예약하고 첨부 상세·파일에 남은 예산을 사용한다. 다른 기관·preview 요청을 차단한다. 실제 사용량은 결과 metadata로 별도 확인한다.
5. Linux `/usr/bin/bwrap`·`prlimit`와 기존 격리 추출기를 요구한다. PDF/HWP/HWPX 파일은 기존 signature/MIME/크기/UTF-8 헤더 검증을 거친다. JPG/PNG는 다운로드하지 않고 UNSUPPORTED_NOT_DOWNLOADED로 보존한다. 실패/부분 추출/OCR/UNKNOWN을 삭제하거나 정상으로 승격하지 않는다.
6. `OBSERVED_NOT_VALIDATED`는 관측이 끝났다는 뜻일 뿐 정책 QA 성공이 아니다. `isWholeTextAnalysisComplete`는 BODY와 모든 실제 파일의 COMPLETE_TEXT 여부만 구분하며 역할·검수 완료가 아니다. 미지원/실패/부분/OCR/미실행이 하나라도 있으면 false다. 정책 QA/기대값 승인 값은 항상 false다.
7. titleInputSource는 FIXED_OFFICIAL_SAMPLE이다. 역할/본문/추출 text·block hash와 품질/판정 사유·근거 위치만 보고하며 원문 텍스트/파일명/URL/헤더/원격 오류 문자열을 JUnit/artifact에 복사하지 않는다. 임시 원본을 finally에서 삭제한다.
8. CI는 별도 opt-in 입력 또는 `[yangpyeong-observation]` 표식일 때만 실행한다. 일반 push는 공개 사이트를 요청하지 않는다. artifact는 고정3 JSON과 해당 JUnit XML뿐이며7일 보관한다. AWS/운영 credential·배포·정책 활성화·기존 데이터 적용은 없다.

## 검증

- [x] 표적20건은42초에 전부 통과했다. 그룹 allowlist/프로필 identity/제목 소속·중복/미지원·부분 파일 완전성/기관·preview 예산 거부/기존 태백 계약/CI 명시 실행과 metadata 전용 보관을 확인했다.
- [x] 별도 임시 PostgreSQL의 현재 DRAFT seed로 위 제목 판정을 확인했다. 운영 DB에는 접속하지 않았다.
- [x] 전체 로컬 회귀3분59초 성공: root2628=2367통과/261조건부 생략/실패0, 별도 패키지20/20·임시 Flyway3/3이다. 이후 JUnit factory 이름의 오버로드 가능성을 제거하고 factory 유일성 검사를 추가한 최종 표적20건이39초에 전부 통과했다. production JAR/추출기/독립 패키지는 UP-TO-DATE이며 새 바이너리 실행 성공으로 세지 않는다. JAR SHA256은 `19b1d2fb08bde9e29a9523e41e2a8352a3ad8e68bbf5e98b0a55826867f90d40`로 정부24 커밋과 같다. Node20통과/2 Linux 전용 생략, 사용한 프로세스는 종료했다.
- [!] 실제 Linux 관측 실행36은 양평2건의 BODY2회 TIMEOUT 뒤 DETAIL_DISCOVERY/TRANSPORT_TIMEOUT으로 실패했다. 해당 파일 다운로드/추출은 미실행이다. 제목 제외1건은 후속 요청0/원본 정리true를 확인했다. [공식 worker 후속 기록](announcement-official-worker-db-api-qa-2026-09-15.md)에 차단과 검증 경로를 구분했다. 같은 접근 조건에서 즉시 반복하지 않는다.
- [!] 정책 QA 기대값 승인·실 운영 ACTIVE 필터링·실제 worker/DB/API/UI·운영 배포·브라우저는 미완료다. 승인 없는 정책 게시/ENFORCE·기존 데이터 적용은 하지 않는다.

09-15 재확인한 로컬 Docker Linux engine pipe는 없어서 연결에 실패했다. 외부 파일을 Windows에서 비격리 추출하지 않는다. 직전 정부24 SHA `cda8c7ae56a54b545990f9fe891b716997bd27f1`의 [Linux 실행35](https://github.com/FrostyCityMan/saneB/actions/runs/34911583782)는 성공했지만 이번 양평 관측 코드는 포함하지 않는다. 이번 그룹의 현재 SHA/실파일 결과는 별도 실행으로 확인한다.

명령:

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentContractWorkflowTest' --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar installAttachmentContractQa --no-daemon --console=plain --max-workers=1
node --test scripts/qa/attachment-contract-report.test.mjs scripts/qa/attachment-contract-release.test.mjs
```

명시된 Linux QA 실행:

```bash
bash ./gradlew attachmentBbsOfficialFileObservation -PsanebBbsObservationGroup=YANGPYEONG --no-daemon --console=plain --max-workers=1
```

본문·첨부 profile/migration/API/화면/정책 seed 변경은 없다. 첨부 엔진6/등록profile17/전용 BODY16기관/형식3, catalog 참조24/실행 기대값0을 유지한다. 사용자가 만든 Word2개는 수정·커밋하지 않는다.
