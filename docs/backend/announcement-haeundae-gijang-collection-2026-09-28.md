# 해운대구·기장군 첨부 수집 연결 — 2026-09-28

## 범위와 근거

HWP 추출 개선은 재개하지 않는다. 제목 정책 → 공식 상세 식별 → 첨부 전체 발견 → PDF/HWP/HWPX 실제 다운로드/signature/hash → 원본 정리만 이번 범위다. 운영 DB·정책·worker·기존 데이터는 변경하지 않는다.

09-28 15:56:12 운영 읽기 전용 영수증의 해운대 `LGS-000036 / SAFE_SAEOL_EMINWON_COMPACT`, 기장 `LGS-000043 / SAFE_SAEOL_EMINWON`은 활성/미연결이었다. 운영 상태는 이번에 다시 조회하지 않았다.

- 해운대 대표 메뉴 `DOM_000000104001002000`는 HTTP403이었다. 인증/차단을 우회하지 않고 저장소 V61에 이미 등록된 공개 `eminwon.haeundae.go.kr` 목록을 별도로 조회해 HTTP200을 확인했다. 대표 메뉴 성공으로 표현하지 않는다. 공식 공개 상세의 form1·article.news_view·h2.newsTitle 제목(등록일 p.small 분리)과 table.tstyle_view·th 첨부파일/td를 확인했다.
- 해운대 목록의 기본 게시기간 조건에서 제외된 과거 지원공고는 같은 공개 목록의 `list_gubun=A` 검색으로 확인했다. 표본53828·53342·53170은 서로 다른 공고이며, 현재 신청 가능 여부를 확정한 것이 아니다.
- 기장 대표 메뉴 `DOM_000000101001002000` iframe에서 `eminwon.gijang.go.kr` 공개 목록 연결을 직접 확인했다. 상세는 name=form POST·table.tb_board_read·thead th 제목·td.attach_list 전체 첨부 칸이다. 첨부 칸 앞의 단일 `<` 표시 오류가3공고 모두에 있었다.
- 두 기관 모두 goDownLoad3인자와 FileDown.jsp GET을 사용한다. JavaScript를 실행하지 않는다. 해운대는 기존 새올 엔진에 고정 bean만 추가한다. 기장은 유일한 form/table/첨부 칸을 검사하고 첨부 링크 앞의 단일 `<` 텍스트만 제거하는 어댑터를 추가한다. 다른 문구/태그/미지원 링크는 지우지 않고 실패한다. 기존 엔진·지역 프로필은 변경하지 않는다.

## 고정 표본과 한도

| 지역 | 공고 | 공식 첨부 |
|---|---|---|
|해운대|53828 청년 구직활동비3차|HWPX1|
|해운대|53342 청년 구직활동비|HWPX1|
|해운대|53170 청년채움공간 가상오피스3차|HWPX1|
|기장|51576 청년농업인 영농정착2차|HWPX1|
|기장|50406 청년 면접수당|HWP4|
|기장|50405 청년 자격시험 응시료|HWP3|

- 사전 조사 해운대7·기장7 GET, 총14회. 요청당 연결7초·전체15초·1MiB, 자동 redirect·TLS 우회 없음. 저장 HTML14개217,008byte(403응답 포함)이며 실제 성공 문서14개라는 뜻은 아니다.
- 이번 기관별 전체 상한30요청. 해운대 수집은 공고당6요청/23MiB, 기장은 최대4파일 때문에 공고당7요청/23MiB로 고정한다. 파일당20MiB. 조사 포함 최대 해운대25·기장28요청 예약이다. 기존 지역 예산은 변경하지 않는다.
- catalog6참조는 모두 `expectation:null`이다. 정책 정상 기대값, 운영 ACTIVE 규칙 또는 사용자 최종 후보 승인이 아니다.
- 조사 원본은 `build/temporary-collection-haeundae-gijang-99115240b4b1426dbb72961c7970ca94` 작업 전용 폴더에 한정하고 검증 종료 후 정리한다. 원문·파일명·개인정보를 대장에 복사하지 않는다.

## 검증 명령

```powershell
.\gradlew.bat :test --tests '*BusanEastAttachmentCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=HAEUNDAE -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GIJANG -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
```

저장 HTML 검사는 테스트 프로세스에 `SANEB_BUSAN_EAST_SAVED_DETAILS`를 주입하고 기존 비식별 운영 대상 영수증과 실제 Spring 등록 대조도 opt-in으로 실행한다. Windows-ROOT는 로컬 인증서 저장소 선택이며 TLS 검증 우회가 아니다. 전체 프로젝트/Linux/운영/브라우저 완료를 이 결과로 주장하지 않는다.

## 실제 결과와 원인 분리

- 로컬101검사(새 계약8·새올26·카탈로그64·운영 영수증 등록 대조3) 실패/생략0, Node19검사·bootJar 실제 생성 통과. 전체 프로젝트/Linux/운영 검증을 재실행한 것은 아니다.
- 기장 실제3검사 실패/생략0. 전체8파일(HWP7·HWPX1),864,526byte 다운로드·형식/signature 확인·원본 정리 완료.17요청/7,177,057byte 예약. 지역 수집 Gate 충족이다.
- 해운대 실제3검사 중1통과·2실패, 생략0.53170 HWPX1개2,899,470byte 검증 성공.53828·53342는 각각103,424/101,888byte를 전송받았지만 `FILE_SIGNATURE` 검증에서 실패했다.12요청/9,420,814byte 예약, 원본 정리 확인. 전체 수집 완료로 계수하지 않는다.
- 두 실패 파일의 추가 단일GET 진단2회(205,312byte)에서 파일명/Content-Disposition은 `.hwpx`, 실제8byte signature는 `d0cf11e0a1b11ae1`(HWP/OLE)임을 확인했다. 두 파일 hash는 최초 실패 보고서의 binaryHash와 각각 일치했다. 원인은 전송 실패가 아닌 명시 형식과 실제 형식 불일치다. 초기에 검토한 특수문자/UTF-8 헤더 문제로 확정하지 않는다. 원시 실행기의 범용 실패 코드 `TRANSPORT_FAILED`가 이 원인을 정확히 표현하지 못하므로 실제 단계/진단 근거를 함께 기록한다.
- 진단53828 hash=`2ec8e2098b19c68642a52351d7339dd15067d3e5ab991a2947141b79e382d08d`,53342=`750b7863afde0d8eded2c1e5d0890b580140c42c1fba7aef907d846ace4f77ad`. 진단 파일·응답 헤더 원본은 정리했고 비식별 hash/형식만 [색인](attachment-collection-receipt-index-2026-09-28.json)에 남겼다. HWP 텍스트 추출은 실행하지 않았다.
- 검증 성공 파일은 두 지역 합계9개3,763,996byte다. 형식 불일치2개는 다운로드 바이트가 있어도 성공 수에 넣지 않는다. 실행기29요청/16,597,871byte 예약, 조사14회·진단2회 포함45회(해운대21/30·기장24/30)다. 예약값은 본문 최대치를 포함하므로 실제 전송량과 다르다.
- 프로필 hash: 해운대=`63ebbebac5d4b3eebd46a44fc28b586b8b9f4854eb52de88ad5a250c11b701de`, 기장=`5b9abb208ad799bbae500446cb93ed9dae92e065e6ed53f145cea0b8a93f47e1`. 이전9충족 지역은 프로필/근거가 그대로 유지됐다.
- JUnit hash: 해운대=`569006759e61347b003b8ced87641008ec1f562a9087b71ca65cbcc369e89679`, 기장=`7bcf5c8fada0714c230167eece4adcb746b361ea9de8cce3cdb8cdf691156b20`. 실행 직후 각각 기록했고 공통 JUnit 파일은 후속 실행으로 교체된다. 개별 원시 보고서는 `build/reports/attachment-regional-collection/HAEUNDAE-*.json`, `GIJANG-*.json`에 보관한다.
- 조사 HTML14개217,008byte·진단 binary/헤더4개·임시 스크립트1개를 종료 전에 삭제했다. 재확인에는 공개 사이트 재조회가 필요하다. 기존 사용자 파일은 보존했다.
- 실제 Spring 등록27프로필(지역26·기업마당1), 실행 구현 클래스11개. catalog65참조/23지역 프로필·보관 기대값1·정상 승인0이다. 정책225대상은 연결27/미등록198(지역197+정부24)이다.
- [최신 수집 대장](attachment-collection-regional-ledger-2026-09-28.json)은 활성223지역 중 **10충족/213잔여 = 등록 미완료16 + 미등록197**이다. 보관 영수증73개/최신50공고 재현 통과. 해운대2건 형식 불일치와 기존 사상 JPEG 미지원은 계속 미완료다. 비활성21지역은 자동 활성화하지 않는다.

해운대의 실제 형식을 자동으로 재해석해 정상 처리하는 정책 변경은 하지 않았다. 이번 수집 성공은 운영 상시 worker·본문/첨부 텍스트 분류·정책 QA·전체 장기 Goal 완료가 아니다. 정책 게시·ENFORCE·기존 데이터 적용·운영 배포는 하지 않았고 브라우저는 현재 요청의 명시 지시가 없어 정책상 미실행이다.
