# 부산 북구·강서구 첨부 수집 연결 — 2026-09-28

## 범위

HWP 추출 개선은 보류한다. 제목 정책 → 공식 상세 식별 → 첨부 전체 발견 → PDF/HWP/HWPX 실제 다운로드/signature/hash → 원본 정리만 검증한다. 운영 DB·정책·worker·기존 데이터·브라우저는 이번 범위가 아니다.

09-28 15:56:12 운영 읽기 전용 영수증의 활성 북구 `LGS-000035 / SAFE_SAEOL_EMINWON`, 강서구 `LGS-000039 / SAFE_SAEOL_EMINWON_COMPACT`를 현재 코드와 대조한다. 운영을 다시 조회한 결과가 아니다.

## 공식 구조와 구현

- 북구 대표 메뉴 `DOM_000000105001005000`의 공식 iframe은 `eminwon.bsbukgu.go.kr`이다. 상세는 form1 POST·table.tbl.taC·제목 th/td·첨부파일 th/td이며, 별도 nnn 폼은 FileDownNew.jsp에 user_file_nm/sys_file_nm/file_path 3개 불투명 인자를 POST한다. JavaScript 실행·인자 해독·GET 대체를 하지 않는다. 기존 화천 프로필은 변경하지 않고 북구 고정 프로필을 추가한다.
- 강서구 대표 메뉴 `0501020000`의 공식 스크립트 문자열에서 `eminwon.bsgangseo.go.kr` 연결을 확인했다. 스크립트 실행은 하지 않았다. form1 POST·div.board > div.b_view·view_head h3 제목·직계 ul.view_file 전체 첨부 목록을 고정한다. 별도 docfm 문서 뷰어 폼은 실행하지 않는다. 기존 GET 엔진으로 공식 goDownLoad 3인자만 전달한다.
- 다른 기관·parser·공고 hash·경로·변경된 redirect는 거절한다. 미지 링크·미지원 파일·중복 충돌을 지워서 성공시키지 않는다. 기존 지역 프로필/근거를 유지한다.
- catalog6건은 모두 `expectation:null`이다. 정상 기대값 승인·운영 정책 활성화·자동 공고 활성화가 아니다.

## 고정 표본과 요청 한도

|지역|공고|공식 첨부|
|---|---|---|
|북구|37638 청년 자격시험 응시료 2026|HWP1|
|북구|35578 청년 자격시험 응시료 2025|HWP1|
|북구|35116 청년 자격시험 응시료 변경 2024|HWP1|
|강서구|40497 청년농업인 영농정착 2차|PDF1·HWPX1|
|강서구|39570 청년 자격시험 응시료|HWP1|
|강서구|39045 청년어촌정착지원|HWP2|

과거 표본은 수집 구조 검증용이며 현재 신청 가능 여부의 근거가 아니다. 사전 조사는 북구8회·강서7회, HTML15개722,322byte다. 연결7초/전체15초/요청당1MiB, 자동 redirect/TLS 우회 없음. 기관별 전체30요청 상한 안에서 공고당6요청/23MiB, 파일당20MiB로 실행한다. 조사 포함 최대 북구26회·강서25회다. 기존 지역 누적 한도는 변경하지 않는다.

## 검증

```powershell
.\gradlew.bat :test --tests '*BusanNorthWestAttachmentCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BSBUKGU -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=BSGANGSEO -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 HTML 검증은 `SANEB_BUSAN_NORTH_WEST_SAVED_DETAILS`, 등록 대조는 기존 비식별 운영 영수증 환경변수를 명시적으로 주입한다. Windows-ROOT는 로컬 인증서 저장소 선택이며 TLS 우회가 아니다. 전체 프로젝트/Linux/운영/브라우저 검증을 뜻하지 않는다.

최초 회귀의 등록 수 실패1건은 테스트용 Spring context에 북구 component 등록이 누락된 것이며 이를 수정했다. 아래는 수정 후 실제 검증 결과다. 검증 기준이나 기존 프로필은 낮추지 않았다.

## 실제 결과

- 최종 로컬100검사(신규 계약7·새올26·catalog64·등록 대조3), 실패/생략0. Node19검사 및 bootJar 확인 통과. 전체 테스트 스위트 재실행은 아니다.
- 강서 실제3검사 실패/생략0. 전체5파일(PDF1·HWPX1·HWP3),730,506byte 다운로드/signature/hash 검증 및 원본 정리 완료.14요청/7,046,538byte 예약. 지역 수집 Gate 충족이다.
- 북구 실제3검사 중2통과·1실패.37638·35578 HWP2개244,736byte 검증 성공.35116은148,988byte 전송됐지만 `FILE_SIGNATURE`에서 실패했다.12요청/6,712,204byte 예약. 북구 전체 완료로 계수하지 않는다.
- 북구35116 추가 단일 POST1회에서 최초8byte가 `5343445341303034`(ASCII `SCDSA004`)이며 현재 지원하는 HWP signature가 아님을 확인했다. hash=`2d4df65a8b94a657417167160a4b36f26fc752e7cccb54b23e886b47c517830d`는 최초 실패 보고서와 일치했다. 파일의 보호/암호화 제품이나 내부 원인을 확정하지 않았으며 해독·HWP 추출을 시도하지 않았다. 범용 `TRANSPORT_FAILED`만 보고 네트워크 장애로 단정하지 않는다.
- 두 기관 합계 검증 성공7파일975,242byte, 실행26요청/13,758,742byte 예약. 조사15회·진단1회를 포함42회(북구21/30·강서21/30)다. 예약량은 본문 최대치를 포함하며 실제 전송량과 다르다.
- 북구 프로필 hash=`61237b7f7898d2a7c798fb10e8cb5dd42f297dd4c99de60ec4ae00b008aa8425`, 강서=`29022422cf48c576818523163faaba9d6551f61e63c173dd7d85690c5c99e3d2`.
- JUnit hash 북구=`15f710a65c99c5890fb4be168fab63d5a6480017220977ff4da635b8c4b63899`, 강서=`3382c69565c9b243ffb3e090a9fe523455a7decdbbb4d69d8ba6c68f9eb09436`. 그룹 실행 직후 기록했고 공통 JUnit 파일은 후속 실행으로 교체된다. 개별 비식별 원시 보고서는 `build/reports/attachment-regional-collection/BSBUKGU-*.json`, `BSGANGSEO-*.json`에 보관한다.
- HTML15개와 진단 binary1개,871,310byte를 삭제했다. 임시 helper2개와 소유 작업 폴더도 삭제 확인했다. 원본 재확인에는 공개 사이트 재조회가 필요하다. 기존 사용자 파일은 보존했다.
- 실제 등록29프로필(지역28·기업마당1), catalog71참조/25지역 프로필·보관 기대값1/정상 승인0. 활성223지역은 등록28/미등록195이고, 정책225대상은 연결29/미등록196(지역195+정부24)이다. 같은15:56 운영 영수증을 기준으로 비교했으며 운영 배포를 의미하지 않는다.
- [지역 대장](attachment-collection-regional-ledger-2026-09-28.json)은 **11충족/212잔여 = 등록 미완료17 + 미등록195**다. 보관 영수증79개/최신56공고 재현 통과. 이전10충족 지역의 프로필·근거가 유지되는지도 확인했다. 비활성21지역은 활성화하지 않는다.

북구35116 형식 검증 실패, 해운대2건 형식 불일치, 사상 JPEG 미지원 등은 계속 미완료다. 성공한 파일만 뽑아 지역 완료를 선언하지 않는다. HWP 추출 개선·운영 정책 게시·ENFORCE·기존 데이터 적용·배포는 하지 않았다. 브라우저 검증은 현재 요청의 명시 지시가 없어 정책상 미실행이다.
