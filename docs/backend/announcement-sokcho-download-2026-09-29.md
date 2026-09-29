# 속초 대표 홈페이지 첨부 연결과 전송 실패 분리

## 현재 단계 / Gate

전 지역 첨부 연결 확대 단계이며 전체 Gate는 미완료다. 성공 파일 보존·개별 오류 분리 원칙, 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 자동 활성화 금지, HWP 추출기 1.0.16 개선 보류를 유지한다.

- [x] 속초 공식 대표 홈페이지의 제목 검색·본문·첨부 영역 실측.
- [x] 속초 포털 상세 프로필 추가, 새올 파일 URL 검증기 재사용.
- [x] 저장된 실제 HTML에서 공고문·신청서식 HWP2개 발견 검증.
- [x] Java212건 통과·조건부1건 생략, Node23건·bootJar 통과.
- [!] 실제 Java 관측: 상세 `DETAIL_DISCOVERY / TLS_FAILED`, 본문 `NETWORK_ERROR`. 파일 다운로드0건.
- [x] 기존223영수증/179공고 보존, 총224영수증/180공고 재현.
- [x] 조사 HTML·헤더7개 2,467,212byte 길이·hash 대조 후 삭제.
- [ ] 미등록87지역 연결, 등록 후 다운로드 미확인28지역 후속.
- [ ] 속초 목록 자동 유입 연계 및 Java TLS 오류 원인 확인.
- [ ] 전국 상시 worker·추출·DB/API/UI·정책 승인·운영 E2E.

## 집계와 근거

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷이다. 이번에는 운영 설정을 재조회하거나 변경하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소1개 다운로드 확인 | 108 | **108/223 (48.4%)** |
| 다운로드 미확인 | 115 | **115** |
| 프로필 등록 지역 | 135 | **136** |
| 미등록 지역 | 88 | **87** |
| 등록 후 다운로드 미확인 | 27 | **28** |
| 첨부 오류 관측 지역 | 32 | **33** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역136+기업마당1=137프로필. 카탈로그192공고/135대상이며 신규 기대값은 null, 기존 승인 기대값1개는 보존한다. 오류 지역은 성공 파일이 있는 지역과 겹칠 수 있어 잔여 수에 더하지 않는다. 프로필 등록을 실제 다운로드 성공·상시 수집 완료로 표현하지 않는다.

## 실제 조사·관측

| 대상 | 근거 | 판정 |
|---|---|---|
| 영동 LGS-000141 | V61 공식 목록 HTTP404,146byte | 조사 오류, 같은 주소 재요청 없음 |
| 속초 LGS-000122 대표 홈페이지 | 목록·제목검색·고정 상세 HTTP200 | HTML 구조 조사 성공 |
| 속초 기존 새올 목록 | HTTPS 요청15초 시간 초과,0byte | 목록 수집 미확인, 재요청 없음 |
| 속초32983 저장 HTML | 제목 일치·본문 셀·HWP2개·대응 미리보기 확인 | 파서 fixture 통과, 다운로드 증거 아님 |
| 속초32983 Java 경로 | 본문 NETWORK_ERROR, 상세 TLS_FAILED | 외부 관측 실패, 파일 목록 도달 전 중단 |

표본 제목은 `2026년 속초시 소상공인 특례보증 수수료 지원 사업 공고`다. 제목 조합은 통과했으나 본문/첨부 실수집은 검증되지 않았다. curl 조사와 Java pinned 전송의 차이만 확인했으며 인증서 체인·런타임 신뢰 저장소·서버 협상 중 어느 것이 원인인지 단정하지 않는다. TLS 검증을 끄거나 HTTP 다운로드로 낮추지 않았다.

- 속초 프로필 hash: `6ce11fbcaccea38d64f53ea20b318d7b6cadce5ec45b823ff7138a437cdaeae8`.
- 관측 producer class hash: `77d3cac1095e56f12c5e45f41a849baffcb28e69841363dd1f456be1410ea862`.
- 관측 상태 `INCOMPLETE`, 다운로드0개, 첨부 텍스트 추출 미실행, 운영 쓰기0.

## 구현 계약과 남은 연결

`SokchoNoticePage`는 공식 `#content-bx` 내부 skinTb의 제목 라벨·본문 단독 셀·첨부파일 라벨을 구분한다. 메뉴·담당부서·파일명을 본문 근거에서 제외한다. `SokchoAttachmentDiscoveryProfile`은 고정 지역·목록 parser·원문 identity·HTTPS 포털 상세 path·단일 공고번호 query를 검증한다. 첨부 셀의 `div.attachFile` 안에 있는 실제 HTTPS 링크만 다운로드 대상으로 사용한다.

파일 URL의 정확한3필드와 경로·파일명은 기존 `SaeolGetAttachmentDiscoveryProfile` 검증을 재사용한다. 공통 엔진·지문 코드는 변경하지 않았다. 포털의 HTTP 미리보기 URL은 다운로드 링크와 같은 파일인지 비교할 때만 정규화하며 요청하지 않는다. 미리보기 불일치·미확인 링크는 정상 descriptor와 별도로 오류를 남긴다. PDF/HWP/HWPX, 미지원 JPG, 부분 발견 실패, 중복·한도10·외부 host·HTTP 요청 차단·본문 구조 변경 회귀 테스트를 추가했다.

이번 프로필은 **대표 홈페이지의 `notAncmtMgtNo` 상세 URL**을 지원한다. 기존 새올 상세 URL을 같은 공고로 자동 치환하지 않았으며, 운영 수집 endpoint/parser도 변경하지 않았다. 따라서 기존 새올 목록의 자동 유입을 이 포털 경로로 연계하는 작업이 남아 있다. 시스템 binding 일치는 이 URL 전환까지 증명하지 않는다.

## 검증 명령·실패 이력

```powershell
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT='true'
$env:SANEB_ATTACHMENT_TARGET_INVENTORY_RECEIPT='C:\PersonalProject\saneB\build\qa-results\target-inventory-20260928-receipt.txt'
$env:SANEB_SOKCHO_SURVEY_FIXTURE='true'
.\gradlew.bat :test --tests '*SokchoDownloadContractTest' --tests '*HwasunDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SOKCHO' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

첫 실행1분29초: 예상 첨부 개수를1개로 잘못 기록하여 실제 HTML의2개와 달라 fixture1건 실패했다. 실제 두 첨부를 다시 확인하고 예상 개수를2로 수정했다. 엔진이 발견한 파일을 줄이거나 테스트 조건을 완화하지 않았다. 외부 관측 태스크는 이때 실행되지 않았다.

두 번째 실행1분33초: Java213건 중212통과·이전 화순 원본 fixture1건 조건부 생략, bootJar 확인. 실제 관측1건은 TLS_FAILED로 실패하여 복합 명령 exit1이다. 전체 명령 성공으로 보고하지 않는다. 환경변수는 finally에서 복원했다. 조사 원본 삭제 후 신규 fixture 환경변수는 켜지 않는다.

조사6요청: 영동1회, 속초5회(대표 목록·새올 목록·검색2회·상세). 첫 검색은 작업자 입력이 실제 option과 다른 `searchCondition=1`이어서 필터 확인으로 인정하지 않았고, 관측한 `TITLE` 값으로 정정 후 적격 표본을 선택했다. 원인 불명 사이트 오류로 기록하지 않는다. 요청당15초/2MiB, TLS 검증·자동 redirect0. Java 관측은6요청/23MiB 상한 중 본문 포함예약3회·2,097,152byte, 조사 포함예약 상한9회다. 예약 수는 실제 HTTP 호출 수와 다르다.

운영 배포·DB/API/UI·상시 worker·정책 활성화·재분류·첨부 추출은 검증하지 않았다. 브라우저는 현재 명시적 실행 지시가 없어 사용자 정책상 생략한다. 커밋·푸시는 `[skip deploy]` 범위이며 운영 데이터·migration·설정은 변경하지 않는다.
