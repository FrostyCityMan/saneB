# 서천 본문·첨부 연결과 HTTP 오류 분리

## 현재 단계 / Gate

- [x] 공식 새올 공개 목록 POST 검색 및 고정 상세 확인
- [x] 서천 시스템 프로필·본문 분리·첨부 GET 연결 구현
- [x] 실제 본문 283자 및 HWP 첨부 1개 발견
- [!] 파일 다운로드 HTTP400. **실파일 성공으로 집계하지 않음**
- [~] **194/223수집원(87.0%) 다운로드 확인, 잔여29** 유지
- [ ] 미연결2: 울산 남구(079), 천안(148)
- [ ] 연결됐으나 정상 다운로드 미확인27: 기존26개+서천
- [ ] 전체 표본·추출·DB/API/worker·운영 E2E

분모는 2026-09-28T15:56:12.116778+09:00 운영 인벤토리의 활성 수집원이다. 현재 운영 조회 또는 고유 지자체 수가 아니며 전체 프로젝트 진행률도 아니다. 엄격한 전체 집합 Gate는16/223 유지, 오류 수집원은47개이며 성공 집계와 중복될 수 있다.

## 확인한 공식 경로

- 대표 홈페이지 일반공고: `https://www.seocheon.go.kr/prog/saeolGosi/03/kor/sub04_06_03/list.do` → HTTP502.
- V61에 등록된 공식 새올 목록 `https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do` → 공개 검색 POST200.
- 검색은 기존 공개 폼에 `not_ancmt_sj=소상공인`, `Key=B_Subject`, `temp=소상공인`을 넣었다. 결과에서 `27490`을 확인했다.
- 상세: `https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=27490&subCheck=Y`
- 제목: **2021년 서천군 소상공인 특례보증 지원사업 공고**. 보관 공고의 경로 검증이며 현재 신청 가능 여부·최신 공고 유입을 입증하지 않는다.

구조 조사8요청(GET6/POST2), GET은12초·POST는20초, 요청별2MiB 제한, TLS 검증 유지, collector UA, 자동 redirect 비활성으로 수행했다. 서천7요청(대표 목록1, 검색POST1, 상세구조GET3, Accept-Encoding 비교GET2), 울산 남구 검색POST1이다. 원문 파일은 저장하지 않았다.

울산 남구는 `MethodInvocationException > TaskCanceledException > TimeoutException > TaskCanceledException > IOException > SocketException`으로20초 제한에 도달했다. 이전 조회 예외의 내부 원인을 새 결과로 소급하지 않는다. 이번 울산 남구 반복 요청은 중단했다.

## 구현 범위

`SeocheonNoticePage`에서 `form[name=form][method=post]`, 폭98%의 공식 표, 제목, `word-break:break-all` 본문 셀을 검증한다. 중첩 첨부 표·담당자·주변 메뉴는 본문에서 제외한다. 정확한 HTTPS host·경로·7개 상세 query를 제한하고 중복 query는 거부한다.

`SeocheonAttachmentDiscoveryProfile`은 검증된 구형 form 이름만 메모리 내에서 변환한 뒤 기존 `SaeolGetAttachmentDiscoveryProfile`에 전체 첨부 칸을 전달한다. 파일명·경로·형식·10파일 상한·오류 분리 정책은 기존 엔진을 재사용한다. 역할은UNKNOWN이고 원문 URL 식별자를 바꾸지 않는다. 공통 엔진은 수정하지 않아 다른 지역의 실행 지문을 보존했다.

바인딩 `LOCAL_SEOCHEON_GET_V1 / LGS-000158 / SAFE_SAEOL_EMINWON_CELL`. 카탈로그 reference-only 표본1개를 추가했으며 기대값 승인·자동 활성화는 없다.

## 실제 관측: 두 실행을 모두 보존

| 시각(KST) | 결과 | 보고서 |
| --- | --- | --- |
| 22:15:42 | 본문283자 성공, 첨부 발견용 상세 재조회 HTTP400, 관측 태스크실패 | `build/reports/attachment-regional-collection/SEOCHEON-27490.json` |
| 22:17:44 | 본문283자·첨부FOUND/완전발견, HWP1개 다운로드HTTP400, 부분 관측 | `build/reports/attachment-regional-collection/SEOCHEON-27490-RECHECK-20260930.json` |

두 번째 실행은 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, `downloadedFileCount=0`, `failedFileCount=1`, `collectionStageComplete=false`다. 태스크종료0은 파일 성공이나 운영 완료가 아니다. 본문은 `AVAILABLE / ACCEPTED / TARGET_SUPPORT_CONFIRMED`다.

같은 공식 상세를 별도 HttpClient로 `Accept-Encoding: identity`와`gzip` 각각 조회했을 때 모두200이었다. 따라서 헤더 차이를400 원인으로 확정하지 않았다. 동일 수집 코드의 재확인에서 실패 단계가 상세조회에서파일로 바뀌었으므로 일시적 응답 차이 가능성은 있으나 **원인 미확정**이다. TLS 해제·UA 위장·인증 우회는 하지 않았다.

실행별 최대6요청·23MiB, 두 실행의 본문 포함 요청 예약7회·예약 바이트4,200,663byte. 두 실행 모두 원본 정리 확인, 운영쓰기0. 파일 원본을 확보하지 못했으므로 파일 해시·추출 성공 근거는 없다.

| 항목 | SHA-256 |
| --- | --- |
| 본문283자 | `e107ae5ce729a2fc953f7acb495ebb0a0f439a512432e276091454cd7e7c57b0` |
| 프로필 | `01a9e7078262e8b87eb7ad7bdf054f9f11173d5431bd7b88d2e2f43a7066b773` |
| 최초 보고서 | `516ed96f4633c2fb3c38ab2b55d43a27885e1e4984af17b2f03e01e03045b7e0` |
| 재확인 보고서 | `6eb7f0bd22f66e3788a32229a71620c8a895d76d17f2519ab3c7eb552b077268` |
| 두 실행의 동일 producer 클래스 | `c07a99fbf6860e24060e8e381065c0622ebef2efc88ba4e0544619bdc7ca4f97` |
| 인벤토리 산출물 | `0192c274451d544440ec998be93999dfd1cda98026433fe67692bebfd6691b60` |

## 실행 명령 / 검증

- `.\gradlew.bat --no-daemon :test --tests '*SeocheonDownloadContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest.packagedRevalidatedExpectationKeepsWholeSetAndDoesNotCompleteCoverage' --tests '*AttachmentProviderQaCatalogTest.reviewedExceptionFixtureKeepsAllReferencesAndCannotFillNormalCoverage' --tests '*AnnouncementAttachmentWorkerServiceTest' --tests '*AnnouncementAttachmentIntakeServiceTest' :bootJar`
  - 최초246개 중244통과·1실패·1조건부생략. 새 테스트가 빈 POST를 생성하다가 기존 Request 생성자 검증에서 먼저 거부됐다.
  - 빈 POST의 생성자 거부와 유효 POST 형식의 GET 프로필 거부를 각각 테스트하도록 수정. 동일 범위 재실행 **245통과·1조건부생략·실패0**, bootJar 통과. 조건부생략은 인벤토리 export 입력 미설정이었다.
- 인벤토리 환경변수를 프로세스 범위에서만 설정해 `:test --tests '*SeocheonDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SEOCHEON -PsanebCollectionWindowsTrust=true` 실행: 로컬8테스트 전부통과, 최초 외부 관측은 실패.
- 같은 실행에 `-PsanebCollectionReportLabel=RECHECK-20260930`을 추가해 1회 재확인: 외부 관측은 부분결과로 종료0, 파일은 여전히 실패. 기존 보고서를 덮어쓰지 않았다. 환경변수는 복원했다.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`:23개 통과. `node scripts/qa/verify-collection-receipt-index.mjs`:352기록/276공고 재현. `node scripts/qa/report-collection-availability.mjs`:194/223·잔여29·오류47·엄격16 재현. `git diff --check` 통과. 전체 테스트 스위트·운영 E2E를 검증한 것은 아니다.

기존350기록·275공고·284카탈로그사례를 보존하고 **352기록 / 276공고**, **285사례·221대상(reference-only284+기존기대값1)**으로 확장했다. 시스템 프로필222개(지역221+기업마당1), 미연결2개다.

## 범위와 다음 단계

서천은 연결 완료·파일 오류로 남기고 다른 미연결2개 및 다운로드 미확인27개 작업을 계속한다. 정상 파일이 있는 공고는 수집하고 오류는 별도 기록한다. 이번 상세·파일400 원인을 정상으로 숨기거나 임의 우회하지 않는다.

운영 DB·endpoint·worker·정책·ENFORCE·재분류·배포, Flyway V85, HWP 추출기1.0.16은 변경하지 않았다. AWS·운영 E2E 미실행, 브라우저는 현재 명시 지시가 없어 정책상 미실행. 이번 임시 원본 정리는 확인했으나 과거 연제·구례 폴더 정리 차단은 해결하지 않았다. 작업 브랜치 `[skip deploy]` 커밋·푸시 범위이며 전체 goal은 미완료다.
