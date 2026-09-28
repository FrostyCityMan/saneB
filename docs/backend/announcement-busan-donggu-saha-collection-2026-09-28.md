# 부산 동구·사하구 첨부 수집 연결 — 2026-09-28

## 범위와 공식 근거

HWP1.0.16 추출 개선은 보류한다. 제목 정책 → 공식 상세 식별 → 전체 첨부 발견 → PDF/HWP/HWPX binary 다운로드/signature/hash → 원본 정리만 진행한다. 운영 DB·정책·worker·기존 데이터는 변경하지 않는다.

기존09-28 15:56:12 읽기 전용 운영 영수증의 활성/미연결 동구 `LGS-000030`, 사하 `LGS-000037`은 모두 `SAFE_SAEOL_EMINWON`이다. 이 영수증을 최신 코드와 비교하며 운영을 새로 조회한 것이 아니다.

- 동구 대표 메뉴 `DOM_000000103001002000`의 공식 iframe에서 `eminwon.bsdonggu.go.kr`을 확인했다. 상세 form1 POST·바깥 table(width100%/border0/cellspacing1/cellpadding0) 제목 td·내부 첨부파일 td/인접 td다. 기존 새올 GET 엔진에 기관 고정 bean만 추가한다.
- 사하 대표 메뉴 `0301030000` HTTPS 응답의 공식 iframe에서 `https://eminwon.saha.go.kr`을 확인했다. V61의 기존 HTTP 주소와 구분한다. 프로필은 기존 HTTP identity가 있어도 검증된 동일 host/path만 HTTPS로 승격하며 HTTP 다운로드는 허용하지 않는다. QA 표본 자체는 공식 HTTPS 주소를 사용한다.
- 사하 form1 POST·table.board_read·thead 제목 th/td>b·첨부파일 th(scope=col)/td(colspan3)를 확인했다. 첨부 anchor는 href=#, onclick/onkeypress의 동일 goDownLoad 호출이다. 선형 인자 파서로 전체 호출을 검사한 뒤 기존 GET 엔진에 전달한다. JavaScript 실행·임의 event·추가 statement·변경된 handler/redirect는 금지한다. 첨부 칸 전체를 보존해 미지원 파일·미지 링크·남은 텍스트를 숨기지 않는다.
- 사하43993의 공식 첨부 칸은 비어 있었다. 링크를 못 찾았다는 사실만으로 첨부 없음 처리하지 않고, 고정 구조와 실제 빈 칸을 함께 검증한다.
- catalog6건은 모두 `expectation:null`이다. 수집 확인은 정상 기대값·정책 게시·운영 자동 활성화 승인이 아니다. 기존 엔진과 이전 지역 프로필은 변경하지 않는다.

## 고정 표본과 예산

|지역|공고|공식 첨부|
|---|---|---|
|동구|32674 청년 자격증 응시료 2026|HWP1|
|동구|30868 청년 자격증 응시료 2025|HWP1|
|동구|30095 청년 자격증 응시료 2024 수정|HWP1|
|사하|43993 청년어촌정착지원|없음|
|사하|46096 여성어업인 건강검진 2차|HWPX1|
|사하|45847 다문화가족사업 지방보조금|HWPX2|

과거 공고는 수집 구조 표본이고 현재 신청 가능 여부를 확정하지 않는다. 사전 조사 동구7·사하9회, HTML16개553,540byte. 연결7초·전체15초·요청당1MiB, redirect 자동 추적/TLS 우회 없음. 기관별 전체30요청 안에서 공고당6요청/23MiB·파일당20MiB로 실행한다. 조사 포함 최대 동구25·사하27회다. 기존 지역의 누적 예산은 변경하지 않는다.

## 검증 명령

```powershell
.\gradlew.bat :test --tests '*BusanDongguSahaCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BSDONGGU -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SAHA -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 HTML 검사는 `SANEB_BUSAN_DONGGU_SAHA_SAVED_DETAILS`, 등록 대조는 기존 비식별 운영 영수증 환경변수를 주입한다. Windows-ROOT는 로컬 인증서 저장소 선택이며 TLS 우회가 아니다. 전체 프로젝트/Linux/운영/브라우저 검증을 대신하지 않는다.

## 실제 검증 결과

- 로컬100검사(새 계약7·새올26·catalog64·등록 대조3), 실패/생략0. Node19검사·bootJar 실제 생성 통과. 전체 프로젝트 테스트를 재실행한 것은 아니다.
- 동구 실제3검사 실패/생략0. HWP3개274,944byte 전체 다운로드/signature/hash 검증·원본 정리 완료.12요청/6,589,298byte 예약. 조사 포함19/30요청이다.
- 사하 실제3검사 실패/생략0.43993은 `NO_FILES`를 확인했고,46096·45847은 HWPX3개204,718byte 전체 다운로드/signature/hash 검증·원본 정리 완료.12요청/6,517,642byte 예약, 조사 포함21/30요청이다. 첨부 없음1건을 다운로드 성공 파일로 계수하지 않는다.
- 합계6공고·실파일6개479,662byte. 실행24요청/13,106,940byte 예약, 조사16회 포함40요청이다. 예약값은 본문 응답 상한을 포함하므로 실제 전송량과 다르다. 추가 재시도·진단 요청은 없었다.
- 동구 프로필 hash=`bbbe0080e77f1aed2c46fb8805ad8a166327056d507870e75894b8d9224f5938`, 사하=`5e9b26ad4644a186984e1d7665b3d34a5d14391f9b0492c21ec445555aaaf4eb`.
- JUnit hash 동구=`d9a0cdd9b93c266203a0cfb9e0b3d5cb51e87f153a3ef43149edaa1a8fa8423d`, 사하=`dc8e6da45b55fe307d84f767164d6e8024d4d04010c251076bb66792c883ad40`. 실행 직후 각각 기록했으며 공통 JUnit 파일은 후속 실행으로 교체된다. 개별 원시 보고서는 `build/reports/attachment-regional-collection/BSDONGGU-*.json`, `SAHA-*.json`이다.
- 조사 HTML16개553,540byte를 삭제했다. 임시 대장 갱신 helper·소유 작업 폴더도 삭제 확인했다. 원본 재검증에는 외부 재조회가 필요하며 기존 사용자 파일은 보존했다.
- 실제 등록31프로필(지역30·기업마당1), catalog77참조/27지역 프로필·보관 기대값1/정상 승인0이다. 활성223지역은 등록30/미등록193, 정책225대상은 연결31/미등록194(지역193+정부24)다. 같은15:56 운영 영수증 기준의 로컬 코드 비교이며 운영 배포가 아니다.
- [최신 수집 대장](attachment-collection-regional-ledger-2026-09-28.json)은 **13충족/210잔여 = 등록 미완료17 + 미등록193**이다. 보관85영수증/최신62공고 재현 검증 통과. 이전11충족 지역의 프로필·근거가 변경되지 않았음을 검증했다. 비활성21지역은 자동 활성화하지 않는다.

북구 signature 실패·해운대 형식 불일치·사상 JPEG 미지원 등 이전 미완료 항목은 그대로다. HWP 추출 개선·운영 정책 게시·ENFORCE·기존 데이터 적용·운영 배포는 하지 않았다. 브라우저는 현재 요청의 명시 지시가 없어 정책상 미실행이다. 이번 성공을 상시 운영 worker·전체 텍스트 분석·정책 QA·장기 Goal 완료로 표현하지 않는다.
