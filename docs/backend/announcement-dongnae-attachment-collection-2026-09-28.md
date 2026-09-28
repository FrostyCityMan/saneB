# 동래 첨부 수집 연결 — 2026-09-28

## 범위

사용자의 전 지역 첨부 수집 우선 지시에 따른 작업이다. HWP 추출 보완은 중지하고 제목 1차 정책 → 상세/첨부 발견 → 전체 binary 다운로드 → signature/hash → 원본 정리까지만 검증한다. 운영 정책·DB·worker 설정·기존 데이터는 변경하지 않는다.

## 구조 확인과 구현

- V61에 등록된 동래 공식 고시공고 메뉴의 iframe에서 `eminwon.dongnae.go.kr` 새올 연결을 확인했다. 운영 읽기 전용 15:56 영수증의 `LGS-000033 / SAFE_SAEOL_EMINWON / enabled=true`와 결합한다.
- 공식 목록에서 43807(청년 마이홈 중개수수료), 43719(청년 성장+면접준비금), 43686(청년 자격증 응시료)을 고정했다. 공고마다 첨부1개, 각각 PDF/HWP/HWP다. HWPX 미관측을 미지원이나 성공으로 해석하지 않는다.
- 실제 상세는 `form2[method=post]`와 `table.tb_t1`, 파일 영역은 `td` 라벨과 `goDownLoad` 3인자다. 공식 JavaScript의 FileDown.jsp GET 조합을 확인했으며 JavaScript를 실행하지 않았다.
- 최초 저장 HTML 검증은 기존 `form1` 가정으로1실패(전체23검사)했다. 다른 지역의 공통 엔진을 바꾸지 않고 `DongnaeSaeolAttachmentDiscoveryProfile`이 단일 form2·고정 table을 확인한 임시 DOM만 form1으로 연결해 기존 새올 파서에 위임한다. 기존 지역의 프로필 hash는 유지한다.
- 동래 hash는 어댑터 실행 코드와 위임 엔진의 hash를 모두 포함한다. 상세/파일 redirect는 최초 요청 URI와 정확히 같아야 하며 타기관·다른 공고·다른 파일로 교체할 수 없다.
- 카탈로그 참조3건은 `expectation:null`이다. 수집 성공으로 정상 기대값·정책 QA·자동 활성화 권한을 만들지 않는다.

## 예산과 조사 실패

- 동래 사전 조사8 GET: 공식 메뉴1·iframe1·목록검색/목록3·상세3. 요청당15초·응답1MiB 상한·HTTPS 검증 유지. 목록 검색 소상공인0건 후 청년 검색으로 표본을 확보했다. 무응답 재시도나 본문 추출 실행이 아니다.
- 실행기 고정3공고, 공고당 최대6요청 예약·23MiB, 파일당20MiB. 전체18요청 예약·69MiB 상한. 사전 조사 포함 이번 동래 캠페인 최대26요청 예약이다.
- 별도 산청 조사2 GET는 저장소의 공식 주소에 검색 조건을 붙인 요청과 원래 주소 모두 HTTP400이었다. 반복 실행을 중단했다. 원본0byte이며 프로필/수집 성공으로 계수하지 않는다. 원인은 아직 확정하지 않았다.
- 저장한 동래 공개 HTML은 이 작업 전용 `build/temporary-collection-dongnae-b37d6082df014a85a3fc427621cd9108` 아래5개 파일뿐이다. 실제 파일 QA 원본과 함께 검증 후 정리하며, 대장에는 원문·개인정보·파일명 대신 비식별 metadata만 보존한다.

## 검증 명령

```powershell
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=DONGNAE -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*DongnaeAttachmentCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 HTML 검사는 `SANEB_DONGNAE_SAVED_DETAILS`, 현재 등록 대조는 `SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT` 및 기존 읽기 전용 영수증 경로를 해당 프로세스에만 주입한다. Windows-ROOT 선택은 로컬 TLS 신뢰 저장소 선택이며 인증서 검증 우회가 아니다.

## 결과

- 실파일3검사 실패/생략0. 43807 PDF81,177byte, 43719 HWP120,832byte, 43686 HWP81,408byte, 총283,417byte. 모두 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`이며 원본 정리 확인.
- 실행기12요청 예약·6,600,796byte 예약. 본문 최대2요청/2MiB를 포함하므로 실전송 계수와 같다고 주장하지 않는다. 동래 조사 포함20요청 예약이며 별도 산청 실패2요청은 성공으로 합산하지 않는다. 조사 응답 decoded UTF-8 합계212,635byte다.
- 동래 프로필 hash=`41f5176ed7c601f00f39e40de274a124961b84432461a6588c3dbe6c40a5593b`. 수집 JUnit hash=`e375e481c393167dc3b644c3e98a44025b76ad4e84edea44ba52925a85bed359`. 원시 공고별 보고서는 `build/reports/attachment-regional-collection/DONGNAE-*.json`, hash·producer class hash는 [색인](attachment-collection-receipt-index-2026-09-28.json)에 기록한다.
- 최종 회귀88검사(새올17·카탈로그64·동래4·등록대조3), 실패/생략0. 원본 HTML 검사는 삭제 전에3상세를 읽어 실행했다. Node19검사 통과. bootJar 실제 생성 성공. 변경 없는 전체 프로젝트 테스트/운영/Linux E2E는 이번 회차에 재실행하지 않았다.
- 현재9실행 구현 클래스(기존8+동래 어댑터1),21프로필(지자체20·기업마당1). 카탈로그47참조/17지역 프로필·보관 기대값1·정상0. 운영15:56 읽기 전용 영수증과 새 로컬 등록을 대조했으며 운영을 다시 조회/변경한 것은 아니다.
- [수집 대장](attachment-collection-regional-ledger-2026-09-28.json): 활성223지역 중7충족/216잔여, 등록20/미등록203. 이전6지역 증거의 profile hash 일치를 유지한다. 등록됐지만 미완료13지역과 미등록203지역을 계속 처리한다.
- 작업 전용 임시 HTML5개를 검증 후 삭제했다. 재검증하려면 공개 사이트에서 다시 받아야 하며 원문은 저장소에 보관하지 않는다. 기존 사용자 파일/프로세스는 유지한다.

운영 상시 수집/추출/관리자 E2E는 이 수집 전용 검증과 별도다. 현재 요청에 브라우저 실행 지시가 없어 브라우저 검증은 정책상 미실행이다.
