# 대전 통합 목록의 서구 첨부 연결 및 연제구 조사 오류

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 먼저 연결하고 개별 실패는 분리한다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 자동 활성화 금지를 보존한다. HWP 추출기 1.0.16 개선은 보류 상태다.

- [x] LGS-000071 / DAEJEON_EMINWON_AGGREGATOR의 서구 상세 경로 연결.
- [x] 공식 공고 본문149자, HWP1개107,008byte 실제 다운로드·기본 형식 검증.
- [x] Java241통과·실패0·생략0, bootJar, Node23/23, 영수증267개/최신221공고 재현.
- [x] 조사 원본11개817,679byte와 관측 다운로드 원본 정리.
- [!] 연제구 LGS-000040 공식 목록200이나 검색 POST403. 동일 요청 반복 없이 분리.
- [ ] 대전 통합 목록의 동구·중구·유성구·대덕구 상세 경로는 이 프로필에서 미지원·미검증.
- [ ] 텍스트 추출·구간 분석·상시 worker·운영 DB/API/UI·DRAFT·기존 데이터·운영 E2E 전체 Gate.

## 조사와 구현 범위

연제구 등록 주소 `https://www.yeonje.go.kr/portal/contents.do?mId=0206030000`는 같은 호스트의 `/portal/saeol/gosi/list.do`로302를 반환했다. 최초 요청에 헤더를 보관하지 않아 같은 GET1회로 Location을 확인했다. 공식 이동 목록200, 화면의 `searchType=tit`, `searchTxt=소상공인`, `page=1` POST는403이었다. 총4요청 후 중단했으며 다른 경로나 인증을 사용해 차단을 우회하지 않았다.

대전 등록 주소 `https://www.daejeon.go.kr/drh/MediaList.do?notiType=NOTI_06&menuSeq=2564`는5개 구청 새올 링크를 모으는 목록이다. 목록200, 대전시 기관 필터 검색은 별도 시청 게시판으로302, 전체 기관 필터 검색은200이었다. 기존 목록 파서가 지원하는 `popupCenterNew('seogu','49944')`와 공식 JavaScript의 고정 상세 경로를 대조했다. 시청 자체 공고와 구청 집계 공고를 혼동하지 않는다.

표본 `DAEJEON_AGGREGATOR-49944`:

- 제목: 2026년 대전 서구 소상공인 경영환경개선 지원사업 공고.
- 상세: `https://eminwon.seogu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?subCheck=Y&jndinm=OfrNotAncmtEJB&context=NTIS&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=49944`.
- 공식 상세200. `form[name=form1][method=post] > table.tbl_board`의 제목·본문·첨부 셀을 구분한다. 담당자·연락처·담당부서·파일명은 본문 분류에서 제외한다.
- 실제 첨부의 `goDownLoad`는 고정된3문자열 인자로 `/emwp/jsp/ofr/FileDown.jsp`를 호출한다. JavaScript나 iframe은 실행하지 않고 기존 새올 GET 엔진으로 직접 요청한다.

`DaejeonAggregatorAttachmentDiscoveryProfile`은 source071/parser를 고정하고 서구 공식 호스트만 지원한다. 기존 원문 URL과 hash를 유지하며 정확한6개 상세 query, HTTPS, 포트443, 동일 요청만 허용한다. 기존7-key 새올 파싱 계약에 맞추는 합성 URL은 HTML 파싱 내부에만 사용한다. 합성 URL을 실제 fetch·DB 저장·원문 식별에 사용하지 않으며 외부 요청 승인에서도 거절한다. 첨부당 실패 분리·최대10개·중복·미지원 형식·UNKNOWN 역할 정책은 기존 엔진을 재사용한다.

본문 client의 교차 호스트 연결은 위 대전 등록 목록의 정확한2개 query와 서구의 정확한 상세6개 query에 한정한다. 등록 호스트와 상세 호스트 양쪽의 공개 IP 검증을 수행하고 상세 리다이렉트는 거절한다. 다른 출처·메뉴·구청·HTTP·임의 query를 허용하지 않는다. 기존 공통 URL 검증기, 파일 검증기, DB/migration, API/UI, 운영 source·정책은 변경하지 않았다.

## 실제 근거와 집계 해석

2026-09-30 06:28 KST(UTC `2026-09-29T21:28:06.881920300Z`):

- 프로필 `LOCAL_DAEJEON_AGGREGATOR_V1`, 지문 `4b4464183655f7d4fdf4eaf66ce06c2c5abe95dac59e45e65bea56adc0f0bf47`.
- 제목 COMBINATION_MATCHED, 본문 AVAILABLE149자 / ACCEPTED / TARGET_SUPPORT_CONFIRMED.
- 첨부 FOUND·complete=true, HWP1개 DOWNLOADED107,008byte.
- 파일 SHA256 `aec927743b661eb27f96606f84d9f61e27df1d98e5f25663aef8b54ae5ab3d1a`.
- 결과 COLLECTION_ONLY_OBSERVED_NOT_APPROVED, 원본 정리true, 운영 쓰기0.

2026-09-28 15:56:12 KST 운영 스냅샷의 활성223지역 수집원 기준으로 최소1파일 다운로드 확인140(62.8%), 잔여83=미등록46+등록 미확인37이다. **이번 증가1은 대전시 통합 수집원에서 서구 표본을 확인했다는 의미이며, 신규 구청 전체나 대전5개 구청 전체의 수집 완료를 뜻하지 않는다.** 기존 서구 자체 수집원과 대전 통합 수집원은 서로 다른 source다. source별 최소1표본 집계를 고유 행정구역·모든 하위 경로 완성도와 동일시하지 않는다.

177지역+기업마당1=178프로필, 카탈로그233공고/176대상(232reference-only),267영수증/221최신공고다. 기존3표본·전체 파일 Gate는16충족/207잔여이며 전체 운영 완료를 의미하지 않는다. 첨부 오류44지역은 성공 지역과 중복될 수 있다.

## 실행 명령 / 결과 / 미검증

```powershell
# 실제 HTML fixture와 기존 읽기 전용 inventory 환경변수 활성화 후
.\gradlew.bat :test --tests '*DaejeonAggregatorDownloadContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=DAEJEON_AGGREGATOR -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

최초 표적113건 중1건이 비어 있는 POST 폼을 테스트에서 생성하여 Request 생성자 검증에 실패했다(36초). 비어 있지 않은 POST 폼을 사용해 프로필의 거절을 검증하도록 수정했다. 이후 확대241건 모두 통과, bootJar·실제 수집 성공(2분1초). Node23건과 기존 영수증·다른 지역 보존 검증이 통과했다. 삭제된 실제 HTML fixture는 이후 환경변수 없이 실행 시 조건부 생략된다.

조사8요청(연제4·대전4), 각15초/2MiB로 제한했다. 관측1회 최대6요청/23MiB·상세1MiB, 실제 본문 포함 예약4회/2,215,752byte, 조사 포함 누적 예약 상한12회다. 예약 수를 실제 wire 요청 수로 단정하지 않는다. 원본11개는 허용된 절대 경로·크기·SHA256 대조 후 삭제했다. 복구에는 공식 사이트 재조회가 필요하며 보고서·해시는 보존한다.

전체 프로젝트 테스트·HWP 텍스트 추출·다른4구청 경로·AWS·운영 DB·상시 유입·운영 E2E는 미실행이다. 브라우저 검증은 현재 요청에 명시되지 않아 정책상 생략했다. 커밋은 작업 브랜치의 `[skip deploy]` 범위이며 운영 배포·정책 게시·ENFORCE·기존 데이터 처리는 수행하지 않는다.
