# 괴산 공식 목록·본문·HWP 첨부 연결

## 현재 단계 / Gate

- [x] 공식 목록 공개 POST 검색으로 지원 공고 식별
- [x] 괴산 새올 GET 첨부 프로필 등록 및 본문 영역 분리
- [x] 공고 29655 본문 287자·HWP 1개 144,896byte 수집
- [x] 원본 정리, 운영 쓰기 0회, 기존 관측 이력 보존
- [~] 활성 223수집원 중 **194개(87.0%) 다운로드 확인, 29개 잔여**
- [ ] 미연결 3개: 울산 남구(079), 천안(148), 서천(158)
- [ ] 연결 후 정상 다운로드 미확인 26개: 이전 강동 기록의 대상 유지
- [ ] 전체 표본·추출·worker/DB/API·운영 브라우저 E2E

집계 분모는 2026-09-28T15:56:12.116778+09:00 운영 인벤토리의 활성 수집원이다. 현재 운영 목록을 다시 조회한 수치나 고유 지자체 수가 아니다. 194개는 한 파일 이상 실제 다운로드를 확인한 수집원 수이며 전체 프로젝트 진행률이 아니다. 엄격한 3표본·전체 집합 Gate는 16/223을 유지한다. 오류 수집원 46개는 다운로드 확인 수집원과 중복될 수 있다.

## 공식 경로와 변경 범위

괴산 공식 포털이 연결하는 `https://eminwon.goesan.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,02,03,04&list_gubun=A`의 검색·상세 함수와 기존 V61 공개 폼 계약을 확인했다. 목록 GET 후 검색은 `OfrAction.do`에 POST하는 구조다. `not_ancmt_sj=소상공인`, `Key=B_Subject`, `temp=소상공인`을 포함한 공개 검색으로 HTTP200을 확인했다. 과거 GET 후속 조회400과 구분한다.

- 공고: `GOESAN-29655`, `LGS-000144`
- 제목: 2026년도 괴산형 소상공인 육성자금 지원계획 공고
- 상세: `https://eminwon.goesan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=29655&subCheck=Y`
- 바인딩: `LOCAL_GOESAN_GET_V1` / `SAFE_SAEOL_EMINWON_CELL`

기존 `SaeolGetAttachmentDiscoveryProfile` 엔진을 재사용하고 시스템 bean만 추가했다. 공식 첨부 셀의 `goDownLoad` 3개 문자열 인자를 읽어 실제 함수와 같은 `FileDown.jsp` GET을 구성한다. 첨부 셀 밖 링크는 사용하지 않는다. 기존 host·query·파일명·경로·형식·10파일 제한은 유지하며 파일 역할은 `UNKNOWN`이다. 미해석 링크가 있으면 정상 파일 descriptor는 보존하고 별도 발견 오류를 반환한다.

본문은 `form1 > table.table_view`의 제목과 `td.con[colspan=4]` 영역을 검증해 선택한다. 담당부서·첨부명·메뉴·스크립트를 본문에 섞지 않는다. 중복 폼·잘못된 구조·중복 query·빈 본문을 구분해 테스트했다.

목록 수집 엔진·운영 endpoint·기존 Flyway는 변경하지 않았다. 공개 POST 검색의 현재 응답과 고정 상세·파일 경로를 확인한 것이며, 운영 scheduler가 해당 공고를 상시 유입시켰다는 증거는 아니다.

## 실파일 관측

관측 시각 **2026-09-30 22:01:46 KST**. 제목 조합 통과, 본문 `AVAILABLE / ACCEPTED / TARGET_SUPPORT_CONFIRMED`, 첨부 `FOUND / discoveryComplete=true`, HWP 1개 다운로드 성공, 실패 0. 이 고정 표본의 `collectionStageComplete=true`다. 추출·정책 승인·운영 E2E는 미검증이다.

| 항목 | SHA-256 |
| --- | --- |
| HWP 144,896byte | `327ee751f085ed1c33c2762492eb98a384644af4178eba7e56b4c2af8bdb76e1` |
| 본문 287자 | `e3a23b62487932b497fb0d421895442aba0c54a1e9737119cfe76709bb1da7a0` |
| 프로필 | `cda1d129f1d89339dabe8ec3672233ce0e6eb48f6b9ac642ec645d6b36ae2fdb` |
| 관측 보고서 | `e86fe8ffcc2c77129f568103b323a220319842a1f7acffbe34562f59f535452a` |
| 관측 실행 클래스 | `dc6582001884eebffe6a7f97e9e03b69481753d6d80d7b86ec6adb2b3cd178b2` |
| 인벤토리 산출물 | `f18b8083e16df414a5fa7fdb1535d0f0950fe30874931848dab1f81dee199545` |

보고서: `build/reports/attachment-regional-collection/GOESAN-29655.json`. 최대 6요청·23MiB, 본문 포함 요청 예약 4회·예약 바이트 2,258,432byte. 임시 원본 정리 확인, 운영 쓰기 0회.

별도 구조 조사 6요청(GET4/POST2), 요청별 최대 15초·2MiB, TLS 검증 유지·collector UA·자동 redirect 비활성. 괴산 목록 GET1·검색 POST1·상세 GET2가 HTTP200이었다. 울산 남구 목록 GET은200, 검색 POST는 `MethodInvocationException`으로 미완료였다. 해당 예외의 내부 원인은 이번 출력만으로 확정하지 않는다. 조사 원문 파일은 저장하지 않았다.

## 실행 명령 / 결과

1. `.\gradlew.bat --no-daemon :test --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*GoesanDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AnnouncementAttachmentIntakeServiceTest' :bootJar`
   - 346개 중344통과·1실패·1조건부 생략. bootJar 생성 성공.
   - 새 본문 테스트의 잘못된 입력 필드가 원인: `registeredSourceUrl`만 바꾸고 실제 `officialDetailUrl`은 정상으로 둬 중복 query 검증을 실행하지 못했다. 실제 조회 URL에 변형을 넣도록 테스트를 수정했다. 검증 기준은 완화하지 않았다.
2. 같은 선택 회귀에서 카탈로그를 `AttachmentProviderQaCatalogTest.packagedRevalidatedExpectationKeepsWholeSetAndDoesNotCompleteCoverage`로 한정하여 재실행: **241개 중240통과·1조건부 생략**, 실패0, bootJar 통과. 생략은 인벤토리 입력이 없는 export 테스트였다.
3. 인벤토리 환경변수를 프로세스 범위에만 설정 후 `.\gradlew.bat --no-daemon :test --tests '*GoesanDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GOESAN -PsanebCollectionWindowsTrust=true`: **5개 전부 통과**, 입력 인벤토리 재생성 및 실제 HWP 관측 완료. 환경변수는 복원했다.
4. `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`: 23개 통과. `node scripts/qa/verify-collection-receipt-index.mjs`: 350기록/275공고 재현. `node scripts/qa/report-collection-availability.mjs`: 194/223·잔여29·오류46·엄격16 재현. `git diff --check` 통과. 전체 프로젝트 테스트 스위트 통과로 표현하지 않는다.

기존 349개 실행 기록·274개 최신 공고를 보존하고 1기록·1공고를 추가해 **350기록 / 275공고**다. 카탈로그는 기존283사례를 보존한 **284사례·220대상**, reference-only283+기존 기대값1이다. 새 기대값 승인은 없고 총 프로필은221개(지역220+기업마당1)다. 기존 지역의 프로필 실행 지문은 바꾸지 않았다.

## 미실행과 후속

운영 DB·설정·worker·정책 게시·ENFORCE·재분류·배포 변경 없음. Flyway V85와 HWP 추출기1.0.16 고도화 보류 유지. AWS 및 운영 E2E 미실행, 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 이번 원본 정리는 확인했으나 과거 연제·구례 임시 폴더 정리 차단까지 해결한 것은 아니다.

다음은 미연결 3개와 연결 후 미확인26개다. 성공 파일은 수집하고 HTTP·TLS·발견·다운로드·파싱 오류는 별도 유지한다. 커밋·푸시는 작업 브랜치 `[skip deploy]` 범위이며 전체 goal은 미완료다.
