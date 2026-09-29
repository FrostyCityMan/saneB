# 홍천 본문·첨부 수집 연결

## 현재 단계 / Gate

전 지역의 첨부 발견·다운로드를 먼저 확대한다. 수집·파싱 오류는 별도로 남기고 정상 파일을 보존한다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지는 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 전체 장기 goal은 미완료다.

- [x] 홍천 LGS-000124 / SPRING_BBS 공식 본문·첨부 연결.
- [x] 실제 본문206자·HWP1개131,584byte 다운로드 및 기본 형식 검증.
- [x] Java244통과/0실패/조건부1생략, Node23/23, bootJar.
- [x] 256영수증/211공고 재현, 기존222카탈로그·다른 지역 상태·과거 요청 원장 보존 확인.
- [x] 조사 원본3개527,097byte 정리, 운영 쓰기0.
- [ ] 남은92지역: 미등록56 + 등록 후 다운로드 미확인36.
- [ ] 첨부 텍스트 추출·구간 분석·운영 상시 worker·DB/API/UI·DRAFT·기존 데이터·운영 E2E.

## 구현 범위

공식 목록 `https://www.hongcheon.go.kr/www/selectEminwonList.do?key=278`의 제목 검색으로 `HONGCHEON-53249`를 확인했다. 상세는 `https://www.hongcheon.go.kr/www/selectEminwonView.do?key=278&not_ancmt_mgt_no=53249`, 제목은 `홍천군 소상공인 특례보증(이차보전) 지원사업 공고`다.

`HongcheonNoticePage`는 고정 HTTPS host/path·key278·숫자 공고 ID를 검증한다. 공식 목록의 페이지/검색 인자는 저장 source identity를 검증한 후 상세 재요청에서 제거한다. 본문은 공식 `div.p-wrap.bbs.bbs__view > table.p-table.block`의 유일한 `td[colspan=4]`만 사용한다. 제목·담당부서·첨부·메뉴·푸터는 본문으로 합치지 않는다. 중복 영역과 구조 변경은 본문 오류다.

`HongcheonAttachmentDiscoveryProfile`은 공식 `첨부파일` 행의 `ul.p-attach > li.p-attach__item > a.p-attach__link`를 읽는다. 페이지 전체 링크를 수집하지 않는다. 실제 파일은 `https://eminwon.hongcheon.go.kr/emwp/jsp/ofr/FileDown.jsp`의 직접 GET이며 기존 새올 검증기로 세 인자·업로드 디렉터리·파일명·HTTPS443을 검사한다. 다른 host/path·임의 query·POST·다른 목적지 redirect는 승인하지 않는다.

PDF/HWP/HWPX만 다운로드 대상으로 삼고 다른 형식은 미지원으로 기록한다. 역할은 UNKNOWN이며 파일명만으로 공고문이라고 확정하지 않는다. 중복·상충·최대10파일·첨부 없음/발견 실패를 분리하고, 일부 링크 실패가 정상 descriptor를 지우지 않는다. 기존 공통 검증기·다른 지역 프로필 지문은 수정하지 않았다. DB migration/API/UI/분류 정책/추출기/운영 설정 변경은 없다.

## 실제 관측

2026-09-30 03:57 KST, 수집 전용 Java 경로 1회:

- 제목 COMBINATION_MATCHED.
- 본문 AVAILABLE206자, ACCEPTED. 후보 근거이며 관리자 최종 확정은 아니다.
- 첨부 FOUND·complete=true, HWP1개131,584byte / DOWNLOADED.
- 바이너리 SHA256 `8b6bbe09b4dc0b44fa7494a35e0ce16b2861c454ded6679917089b32806b5f61`.
- 프로필 `LOCAL_HONGCHEON_BOARD_V1`, 지문 `2fa5d20310abcb2809a277f5a6a9fe135cf821b14f831b6118a468b2790eee15`.
- `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 원본 정리true, 운영 쓰기0.

실파일 다운로드와 기본 형식 검증을 확인한 것이며 HWP 내부 구조·텍스트 추출·구간 분석·운영 상시 수집 성공을 의미하지 않는다. 신규 QA 카탈로그 expectation은 null이다.

## 지역 집계와 요청 원장

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 이번 운영 재조회는 없다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 최소1개 파일 다운로드 확인 | 130 | 131/223 (58.7%) |
| 다운로드 미확인 | 93 | 92 |
| 미등록 지역 | 57 | 56 |
| 등록 후 다운로드 미확인 | 36 | 36 |
| 첨부 오류가 남은 지역 | 43 | 43 |
| 기존3표본·전체 첨부 Gate 충족/잔여 | 16/207 | 16/207 |

지역167+기업마당1=168프로필이며 카탈로그223공고/166대상이다. 기존255영수증·210공고·홍천 외 지역 상태·모든 이전 요청 원장을 보존했다. 원장 상단의 오래된 importedReceiptCount252와 inputInventorySha256도 실제256영수증 및 현재 로컬 등록 목록 해시로 동기화했다. 재현 스크립트에 두 값의 일치 검사를 추가하여 요약 헤더가 뒤처지면 실패하도록 했다.

과거 `gangwonSecondDownloadRun`의 홍천2요청(목록200·검색 연결 시간 초과)은 보존한다. 이후 공식 목록·제목 검색·상세3요청으로 구조를 확인했고, 이번 실제 관측의 본문 포함 예약4회/2,408,960byte를 합쳐 누적 예약 상한9회다. 관측1회 상한은6요청/23MiB, 상세1MiB다. 조사3회는 각15초/2MiB였다. 예약 상한을 실제 wire 요청 수로 단정하지 않는다.

조사 HTML3개527,097byte는 원장의 SHA256·길이·정확한 디렉터리를 확인한 후 삭제했다. 실제 다운로드 임시 원본은 실행기가 정리했다. 원문은 공식 사이트에서 다시 조회해야 확보할 수 있으며 저장소에는 원문/연락처/헤더를 기록하지 않았다.

## 검증 명령 / 결과 / 미검증

```powershell
# 실제 홍천 fixture 및 보관 운영 대상 스냅샷 환경변수 활성화 후
.\gradlew.bat :test --tests '*HongcheonDownloadContractTest' --tests '*SeoulFifthDownloadContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=HONGCHEON -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 홍천/본문 표적 실행 성공(36초), 확대 실행 성공(1분54초). 확대 XML 집계는 총245건 중244통과·0실패·1생략이다. 생략1건은 기존 서울5차 실제 HTML fixture 조건부 검증이며 홍천 실제 fixture는 실행했다. 원본 정리 이후 홍천 fixture 환경변수를 켜지 않으면 해당 조건부 검증은 생략된다. Node23/23, bootJar·실파일 관측·256영수증/211공고 재현 통과다.

전체 프로젝트 테스트·AWS·운영 DB·실제 운영 worker/DB/API/UI·추출·운영 E2E는 이번 회차 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 사용자 정책에 따라 생략했다. 운영 배포·정책 게시·ENFORCE·기존 데이터 적용은 수행하지 않았다.
