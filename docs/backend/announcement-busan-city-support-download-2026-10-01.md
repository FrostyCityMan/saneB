# 부산시 지원 공고 실파일 수집과 상시 URL 계약 후속

## 현재 단계 / Gate

- [x] 기존 부산시 본문·첨부 처리기를 제목 적격 수집 관측에 연결
- [x] 공식 목록·상세의79622 확인, 본문146자·HWPX1개692,641byte 다운로드
- [x] Java138·인벤토리3·실제 관측1·bootJar·Node23 통과,415영수증/281공고 재현
- [~] 과거 제목 적격 다운로드 관측202/223(90.6%), 미확인21개
- [~] 최신 코드 다운로드36/223, 신규 확인·재검증187개
- [!] 부산 목록 검색 인자와 상세 허용 계약의 차이: 상시 유입 후속 수정 필요
- [!] AWS 로그인 갱신 대기·기존 임시 공개 CA/HTML 정리 차단 유지
- [ ] 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate

분모223은2026-09-28T15:56:12.116778+09:00 활성 지역 수집원 스냅샷이다. 현재 운영 조회 또는 고유 지자체 수가 아니다.90.6%는 전체 개발 완료율이 아니며 엄격3표본 전체 첨부 집합 Gate는 현재 코드0/223으로 별도 유지한다. HWP 추출기1.0.16 고도화 보류와 정상 파일 보존·개별 오류 분리를 유지한다.

## 조사와 구현

기준 HEAD `bbd34f8ec61c99f200a5e492ab8c85cc8ca727d9`. 부산시의 `LOCAL_BUSAN_LEGAL_GET_V1`은 이미 구현돼 있었으나 현재 제목 적격 수집 원장에 표본이 없었다.2026-09-12의 실제 파일 관측은 제목 필터를 포함하지 않았으므로 이번 분모·성공 근거로 소급 편입하지 않는다.

[공식 고시공고 검색](https://www.busan.go.kr/nbgosi/list)에서 GET 검색 폼의 제목 검색 항목과 `소상공인` 결과를 확인했다. 선택한 [79622 공식 상세](https://www.busan.go.kr/nbgosi/view?sno=79622&gosiGbn=A&curPage=1)의 제목은 ‘2026년도 부산광역시 중소기업·소상공인 자금지원계획 10차 변경 공고’다. 단일 `div.boardView` 안의 `h4.form-data-subject`와 공식 첨부 영역의 HWPX1개를 확인했다. 이름 링크와 다운로드 보조 링크는 같은 `fileId`·`seq=0`이었다.

- `BusanCitySupportDownloadCases`와 `BUSAN_CITY_SUPPORT` 실행 그룹을 추가했다. 기존 처리기·본문 정제·헤더 복원·분류 규칙을 재사용한다.
- 제목은 기존 DRAFT 정책 통과 후 실제 상세 제목과 대조한다. 제목 중복·불일치·공식 영역 누락은 실패다.
- 관측 예산은6요청·23MiB다. 역할 UNKNOWN과 파일 중복 제거를 계약 테스트로 확인했다.
- catalog에 expectation=null 참조1개만 추가했다. 기존287개 참조 내용·기존 승인 기대값은 그대로다. 전체288공고,287참조 전용+기존 기대값1개이며 catalog 시험의 대상은223개다. 이는 운영 활성 수집원223개와 의미가 다르다.
- 응용 Java·DB/API·Flyway V85·프로필은 변경하지 않았다. 모든 기존 프로필 지문 불변을 인벤토리 대조로 확인했다.

## 실제 관측

2026-10-01 00:47:41 KST의 `BUSAN_CITY-79622-BUSAN-CITY-20261001.json`:

| 단계 | 결과 |
| --- | --- |
| 제목 | COMBINATION_MATCHED |
| 본문 | AVAILABLE146자, TARGET_SUPPORT_CONFIRMED |
| 첨부 발견 | FOUND, complete=true,1개 |
| 파일 | HWPX,692641byte,DOWNLOADED |
| 수집 판정 | COLLECTION_ONLY_OBSERVED_NOT_APPROVED |
| 추출·기대값·정책·운영 | 모두 미검증/미승인, 운영 쓰기0 |
| 원본 | originalFilesRemoved=true |

파일 SHA-256: `775f683ea8802d4b90a7fde915b587c0579626cbda984b0c471e768cc1b9b567`.
본문 포함 요청 예약4회·예약량2,896,289byte이며 실제 wire 전송량과 구분한다. 추출기를 실행하지 않았으므로 HWPX 내부 텍스트 필터링 완료로 표시하지 않는다. 관리자 최종 검증·자동 활성화 금지를 유지한다.

로컬 공식 HTTP 조사는5회(울산 남구 메뉴1,부산 목록/검색3,부산 상세1)였으며 각12초·2MiB 상한, TLS 검증 유지·수집기 UA·쿠키 미사용·자동 redirect 금지를 적용했다. 조사 HTML은 파일로 저장하지 않았다. 별도 웹 검색6질의와 부산 목록 열람1회는 경로 탐색이며 수집 성공 증거가 아니다. 울산 남구 메뉴200은 여전히 기존 새올 iframe을 연결하므로, 반복 시간 초과했던 iframe/POST는 다시 요청하지 않았다. WSL 배포판 목록에는 docker-desktop만 있었으며 시작·설정 변경을 하지 않았다.

## 발견한 상시 유입 계약 차이

공식 목록의 상세 링크에는 `conIfmStdt`, `conIfmEnddt`, `conGosiGbn`, `schKeyType`, `srchText` 같은 검색 문맥 인자가 붙는다. 기본 페이지에서도 `conGosiGbn`이 붙은 링크를 관측했다. 기존 부산 상세 허용 검사는 `sno`, `gosiGbn`, `curPage`만 허용한다. 공통 목록 수집기의 URL 정규화는 검색 인자를 삭제하지 않는다.

이번 표본은 동일 공고의 세 가지 상세 인자 URL을 별도로 HTTP 확인하고 사용했다. 따라서 이번 성공이 실제 목록 URL 그대로의 상시 유입 성공을 증명하지 않는다. 후속은 공식 검색 문맥 인자의 한정적 검증·본문/첨부 일치·동일 공고 식별 보존·허용되지 않은 인자 차단을 함께 설계하고 회귀 검증하는 것이다. 임의 query 전체 허용이나 다른 공고로의 전환은 해법으로 사용하지 않는다. 운영 데이터를 일괄 정규화하거나 기존 providerNoticeId를 변경한 적은 없다.

## 실행 명령 / 결과

```powershell
.\gradlew.bat --no-daemon :test --tests '*BusanCitySupportCollectionContractTest' --tests '*LegalBoardAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar
# 기존 읽기 전용 운영 인벤토리 영수증 환경변수를 일시 적용하고 finally에서 복원
.\gradlew.bat --no-daemon :test --tests '*AttachmentProviderInventoryAuditTest' :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BUSAN_CITY_SUPPORT -PsanebCollectionReportLabel=BUSAN-CITY-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

첫 Gradle3분11초:138개 실패/생략0,bootJar 통과. 후속37초:인벤토리3개와 실제 관측1개 통과. Node23통과. 전체 Java suite·Linux 추출·상시 목록 유입·AWS·운영 검증은 실행하지 않았다. 인벤토리 재생성은2026-09-28 보관 영수증과 현재 로컬 등록 대조이며 운영 재조회가 아니다.

- inventory SHA-256: `2e2f0a9dca6e8f81b03303a41eef24870c2999bbfd2456c8c2fd4603d32b5cdf`
- 신규 producer SHA-256: `c50727a1f22a1a8260cc8ab59050b178a6cf58f49b8a8762f7baafad5b32e087`
- 이전 inventory의 LF 정규화 보관본: `build/reports/attachment-target-inventory/before-busan-city-20261001.json`, SHA-256 `a59a935067c4a332505b0f9b761c2c507effd0b7991b7df94ce1fb8ae63f1766`. 이전 원본 바이트 해시7f6d…와 JSON 내용은 같지만 바이트 해시는 다르다.

414개 기존 영수증·280개 기존 최신 공고를 유지하고415/281로 확장했다. 과거 모든 영수증을 현재 활성 수집원 식별에 대조해 제목 적격·상세 소속·원본 정리·파일 signature가 확인된 수집원의 합집합202개를 계산했다. 과거 producer/profile 지문을 최신으로 덮지 않았다. 현재 오류 포함 수집원9개는 성공 집계와 중복될 수 있다.

## 남은 수집원 / 운영 경계

다운로드 미확인21개: 은평,서대문,송파,연제,검단,울산 남구,성남,평택,포천,이천,동두천,강릉,속초,평창,철원,충북도,영동,천안,공주,의성,성주. 울산 남구는 프로필 미연결1개이며 나머지20개는 등록 후 적격 다운로드 미확인이다. 모든 기관의 현재 장애를 이번에 다시 관측한 목록은 아니다.

AWS 인증·기존 임시 공개 CA2개/연제·구례 HTML 정리 차단은 유지한다. 우회 삭제·브라우저 실행·운영 DB/설정/worker/정책/ENFORCE/배포 변경은 없다. 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 기존 미추적 output·Python 캐시를 보존한다. `[skip deploy]` 작업 브랜치 커밋·푸시 범위이며 전체 장기 goal은 미완료다.
